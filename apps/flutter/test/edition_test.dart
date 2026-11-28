import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutterqa/gateway.dart';
import 'package:flutterqa/main.dart';

class Fixture implements Gateway {
  final calls=<({String path,Data body})>[];
  final events=<Data>[];
  final plant=<String,dynamic>{
    'id':'fixture-plant','space':'fixture-space','my_name':'Review fern',
    'species_label':'Reference fern','placement_note':'Review window',
    'is_active':true,'can_record':true,'identity_state':'user_selected',
    'next_follow_up_on':'2020-01-01',
  };
  @override Future<Data> login(String email,String password)async=>{'id':1,'name':'Review account'};
  @override Future<Data> get(String path)async{
    if(path=='plantupia/spaces/')return {'results':[{'id':'fixture-space','name':'Review studio'}]};
    if(path=='plantupia/plants/')return {'results':[plant]};
    if(path.contains('/care-guide/'))return {'identity_basis':'user_selected','guide':{
      'species':'Review reference','version':1,'sections':[
        {'title':'Observation','instructions':'Fictional fixture text. Not botanical advice.'}
      ]}};
    if(path.contains('/events/'))return {'results':events,'next_offset':null};
    if(path=='plantupia/commerce/catalog/')return {'results':[]};
    return {'results':[]};
  }
  @override Future<Data> post(String path,Data body)async{
    calls.add((path:path,body:Map.of(body)));
    if(path.endsWith('/event-submissions/')){
      final saved={...body,'id':'fixture-event'};
      events.insert(0,saved);return saved;
    }
    if(path.endsWith('/assistant/ask/'))return {'answer':'Fixture response for transport verification only.',
      'uncertainty':'Fictional test output'};
    throw const ApiProblem(400,'Not part of the fixture.');
  }
  @override Future<Uint8List?> picture(String reference,{bool catalog=false})async=>null;
  @override void clear(){}
}
void viewport(WidgetTester tester,{double width=390,double scale=1}){
  tester.view.physicalSize=Size(width,844);
  tester.view.devicePixelRatio=1;
  tester.platformDispatcher.textScaleFactorTestValue=scale;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
}
Future<void> showReview(WidgetTester tester,Fixture fixture)async{
  await tester.pumpWidget(RepaintBoundary(key:const ValueKey('review-frame'),
    child:PlantupiaApp(gateway:fixture,reviewUser:const {'id':1,'name':'Review account'})));
  await tester.pumpAndSettle();
}
void main(){
  test('submission identities have a valid v4 shape',(){
    final pattern=RegExp(r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$');
    final ids=List.generate(25,(_)=>uuid());
    expect(ids.every(pattern.hasMatch),isTrue);
    expect(ids.toSet().length,25);
  });
  testWidgets('daily attention opens the same private plant and records a confirmed event',(tester)async{
    viewport(tester);
    final fixture=Fixture();
    await showReview(tester,fixture);
    final due=find.text('One plant to check');
    expect(due,findsOneWidget);
    if(const bool.fromEnvironment('CAPTURE_REVIEW')){
      await expectLater(find.byKey(const ValueKey('review-frame')),matchesGoldenFile('goldens/today-fixture.png'));
    }
    await tester.ensureVisible(due);await tester.tap(due);await tester.pumpAndSettle();
    expect(find.text('Review fern'),findsOneWidget);
    final action=find.text('Record a moment');
    await tester.ensureVisible(action);await tester.tap(action);await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).last,'A private fixture observation.');
    final save=find.text('Save private record');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();
    await tester.ensureVisible(save);await tester.pumpAndSettle();
    expect(save.hitTestable(),findsOneWidget);
    await tester.tap(save);await tester.pumpAndSettle();
    expect(fixture.calls.length,1);
    expect(fixture.calls.single.path,'plantupia/plants/fixture-plant/event-submissions/');
    expect(fixture.calls.single.body['kind'],'observation');
    expect(fixture.calls.single.body['evidence_type'],'observed');
    expect(fixture.calls.single.body['note'],'A private fixture observation.');
    expect(fixture.events.length,1);
    expect(tester.takeException(),isNull);
  });
  testWidgets('a compact large-text edition retains the real daily entry',(tester)async{
    viewport(tester,width:320,scale:2);
    final fixture=Fixture();await showReview(tester,fixture);
    expect(find.text('One plant to check').hitTestable(),findsOneWidget);
    expect(tester.takeException(),isNull);
    if(const bool.fromEnvironment('CAPTURE_REVIEW')){
      await expectLater(find.byKey(const ValueKey('review-frame')),matchesGoldenFile('goldens/today-compact-fixture.png'));
    }
  });
  testWidgets('assistant requires confirmation and does not share context by default',(tester)async{
    viewport(tester);
    final fixture=Fixture();await showReview(tester,fixture);
    await tester.tap(find.text('Ask').last);await tester.pumpAndSettle();
    final input=find.widgetWithText(TextField,'Your question');
    await tester.ensureVisible(input);await tester.enterText(input,'A fictional test question');
    final ask=find.text('Ask Plantupia AI');
    await tester.ensureVisible(ask);await tester.tap(ask);await tester.pumpAndSettle();
    expect(fixture.calls,isEmpty);
    await tester.tap(find.text('Send question'));await tester.pumpAndSettle();
    expect(fixture.calls.single.body['include_my_plant_context'],false);
    expect(fixture.calls.single.body.containsKey('plant_id'),false);
    expect(find.text('Fixture response for transport verification only.'),findsOneWidget);
    expect(tester.takeException(),isNull);
  });
}
