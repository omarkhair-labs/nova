import 'package:flutter/material.dart';
import 'gateway.dart';
import 'edition.dart';
import 'main.dart' show plantName, plantContext;

class PlantEdition extends StatefulWidget {
  final Gateway gateway;
  final Data plant;
  final String spaceName;
  const PlantEdition({super.key,required this.gateway,required this.plant,required this.spaceName});
  @override State<PlantEdition> createState()=>_PlantEditionState();
}
class _PlantEditionState extends State<PlantEdition>{
  int tab=0;
  bool changed=false;
  late Future<Data> guide;
  late Future<Data> history;
  String get id=>field(widget.plant,'id');
  bool get writable=>widget.plant['is_active']==true&&widget.plant['can_record']==true;
  @override void initState(){super.initState();_load();}
  void _load(){
    guide=widget.gateway.get('plantupia/plants/$id/care-guide/');
    history=widget.gateway.get('plantupia/plants/$id/events/');
    // Attach handlers immediately, including when a chapter is not visible.
    guide.then<void>((_) {},onError:(Object _,StackTrace __){});
    history.then<void>((_) {},onError:(Object _,StackTrace __){});
  }
  Future<void> record({Data? followUp})async{
    if(!writable)return;
    final saved=await openEdition<bool>(context,RecordEdition(gateway:widget.gateway,
      plant:widget.plant,followUp:followUp));
    if(mounted&&saved==true)setState((){changed=true;_load();tab=2;});
  }
  @override Widget build(BuildContext context){
    final p=widget.plant;
    final identity=field(p,'identity_state');
    return PopScope<bool>(canPop:false,onPopInvokedWithResult:(didPop,result){
      if(!didPop)Navigator.of(context).pop(changed);
    },child:Scaffold(
      appBar:AppBar(leading:IconButton(icon:const Icon(Icons.arrow_back),tooltip:'Return to collection',
        onPressed:()=>Navigator.of(context).pop(changed)),title:Eyebrow(widget.spaceName)),
      body:SafeArea(top:false,child:ListView(padding:const EdgeInsets.fromLTRB(22,6,22,36),children:[
        const Eyebrow('A living record / specimen'),
        const SizedBox(height:14),TitleText(plantName(p),size:43),
        if(plantContext(p).isNotEmpty)Padding(padding:const EdgeInsets.only(top:8),
          child:Text(plantContext(p),style:const TextStyle(color:muted,fontStyle:FontStyle.italic))),
        const SizedBox(height:14),
        Eyebrow(identity=='staff_verified'?'Identity verified by the team':
          identity=='user_selected'?'Reference type chosen by you':'Botanical identity not verified'),
        const SizedBox(height:20),
        AspectRatio(aspectRatio:.94,child:PrivatePhoto(gateway:widget.gateway,
          reference:field(p,'cover_photo'))),
        const SizedBox(height:16),if(writable)FilledButton(onPressed:()=>record(),child:
          const Row(children:[Expanded(child:Text('Record a moment')),Icon(Icons.add,size:21)]))
          else const Text('Read-only record.',style:TextStyle(color:muted)),
        const SizedBox(height:22),const Rule(),
        Row(children:List.generate(3,(i)=>Expanded(child:InkWell(onTap:()=>setState(()=>tab=i),
          child:Column(children:[
            Padding(padding:const EdgeInsets.symmetric(vertical:13,horizontal:2),child:Text(
              const ['Overview','Care guide','History'][i],textAlign:TextAlign.center,
              style:TextStyle(fontWeight:tab==i?FontWeight.w600:FontWeight.w400,
                color:tab==i?forest:muted,fontSize:13))),
            AnimatedContainer(duration:const Duration(milliseconds:170),height:2,
              color:tab==i?forest:Colors.transparent),
          ]))))),const Rule(),const SizedBox(height:20),
        if(tab==0)...[
          const TitleText('A place in your life.',size:30),const SizedBox(height:10),
          Text(field(p,'placement_note','No placement note yet.'),style:const TextStyle(color:muted)),
          if(field(p,'next_follow_up_on').isNotEmpty)...[
            const SizedBox(height:17),const Rule(),
            ActionLine('Next recorded check-in',detail:dateOnly(field(p,'next_follow_up_on')),
              onTap:()=>setState(()=>tab=2))],
          const Rule(),ActionLine('Read the care guide',onTap:()=>setState(()=>tab=1)),
          const Rule(),ActionLine('Open recorded history',onTap:()=>setState(()=>tab=2)),
        ]else if(tab==1) FutureBuilder<Data>(future:guide,builder:(context,s){
          if(s.connectionState!=ConnectionState.done)return const Loading();
          if(s.hasError)return Notice('Guide unavailable',explain(s.error),retry:()=>setState(()=>
            guide=widget.gateway.get('plantupia/plants/$id/care-guide/')));
          final g=object(s.data?['guide']);
          if(g.isEmpty)return const Notice('No reviewed guide yet',
            'A botanical guide will appear when the company has a published reference for this plant.');
          final sections=rows(g['sections']);
          return Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
            Eyebrow('Reviewed botanical library / version ${g['version']??''}'),
            const SizedBox(height:11),TitleText(field(g,'species','Care guide'),size:32),
            if(s.data?['identity_basis']!='staff_verified')const Padding(padding:EdgeInsets.symmetric(vertical:12),
              child:Text('General guidance for the selected reference type, not a diagnosis of your plant.')),
            if(sections.isEmpty)Text(field(g,'instructions')),
            for(final section in sections)...[
              const SizedBox(height:18),const Rule(),const SizedBox(height:14),
              TitleText(field(section,'title','Care'),size:25),const SizedBox(height:8),
              Text(field(section,'instructions')),
              if(field(section,'context_notes').isNotEmpty)Padding(padding:const EdgeInsets.only(top:7),
                child:Text(field(section,'context_notes'),style:const TextStyle(color:muted))),
              if(field(section,'source_reference').isNotEmpty)Padding(padding:const EdgeInsets.only(top:8),
                child:Text(field(section,'source_reference'),style:const TextStyle(fontSize:12,color:muted))),
              if(field(section,'source_url').isNotEmpty)SelectableText(field(section,'source_url'),
                style:const TextStyle(fontSize:11,color:muted)),
            ],
            if(field(g,'context_notes').isNotEmpty)Padding(padding:const EdgeInsets.only(top:18),
              child:Text(field(g,'context_notes'))),
            const SizedBox(height:20),const Rule(),const SizedBox(height:12),
            Text('Reviewed by '+field(g,'reviewer','the Plantupia team')+' · '+dateOnly(field(g,'reviewed_at')),
              style:const TextStyle(color:muted,fontSize:12)),
            if(field(g,'source_url').isNotEmpty)SelectableText(field(g,'source_url'),
              style:const TextStyle(fontSize:11,color:muted)),
          ]);
        })else HistoryEdition(key:ObjectKey(history),gateway:widget.gateway,plantId:id,first:history,
          onOutcome:writable?(e)=>record(followUp:e):null),
      ]))));
  }
}
class HistoryEdition extends StatefulWidget{
  final Gateway gateway;
  final String plantId;
  final Future<Data> first;
  final ValueChanged<Data>? onOutcome;
  const HistoryEdition({super.key,required this.gateway,required this.plantId,required this.first,this.onOutcome});
  @override State<HistoryEdition> createState()=>_HistoryEditionState();
}
class _HistoryEditionState extends State<HistoryEdition>{
  final List<Data> events=[];
  int? next;
  bool loading=true;
  String error='';
  @override void initState(){super.initState();load(widget.first,reset:true);}
  Future<void> load(Future<Data> request,{bool reset=false})async{
    if(mounted)setState((){loading=true;error='';});
    try{
      final result=await request;
      if(!mounted)return;
      setState((){
        if(reset)events.clear();
        for(final e in rows(result['results'])){if(!events.any((v)=>v['id']==e['id']))events.add(e);}
        next=result['next_offset'] is int?result['next_offset'] as int:null;
      });
    }catch(e){if(mounted)setState(()=>error=explain(e));}
    finally{if(mounted)setState(()=>loading=false);}
  }
  @override Widget build(BuildContext context)=>Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
    const Eyebrow('Private journal'),const SizedBox(height:12),const TitleText('Things noticed.',size:32),
    const SizedBox(height:13),
    if(!loading&&events.isEmpty&&error.isEmpty)const Notice('The first page','Your own care and observation moments will stay here.'),
    for(final e in events)...[
      const Rule(),const SizedBox(height:15),
      Eyebrow(dateOnly(field(e,'observed_at'))+' / '+field(e,'kind','Record')),
      const SizedBox(height:8),
      if(field(e,'note').isNotEmpty)Text(field(e,'note'),style:const TextStyle(fontSize:17,height:1.45)),
      if(field(e,'photo').isNotEmpty)Padding(padding:const EdgeInsets.symmetric(vertical:12),
        child:AspectRatio(aspectRatio:1.25,child:PrivatePhoto(gateway:widget.gateway,
          reference:field(e,'photo'),fit:BoxFit.contain))),
      if(field(e,'follow_up_on').isNotEmpty)...[
        const SizedBox(height:9),Text((field(e,'follow_up_completed_at').isEmpty?'Check-in: ':'Completed check-in: ')
          +dateOnly(field(e,'follow_up_on')),style:const TextStyle(color:muted,fontSize:13)),
        if(field(e,'follow_up_completed_at').isEmpty&&widget.onOutcome!=null)
          TextButton(onPressed:()=>widget.onOutcome!(e),child:const Text('Record the outcome'))
      ],const SizedBox(height:16),
    ],
    if(error.isNotEmpty)Notice('Could not load records',error,retry:()=>load(widget.gateway.get(
      'plantupia/plants/${widget.plantId}/events/?offset=${next??0}'),reset:events.isEmpty)),
    if(loading)const Loading()
    else if(next!=null&&error.isEmpty)OutlinedButton(onPressed:()=>load(widget.gateway.get(
      'plantupia/plants/${widget.plantId}/events/?offset=$next')),child:const Text('Earlier records')),
  ]);
}

class RecordEdition extends StatefulWidget{
  final Gateway gateway;
  final Data plant;
  final Data? followUp;
  const RecordEdition({super.key,required this.gateway,required this.plant,this.followUp});
  @override State<RecordEdition> createState()=>_RecordEditionState();
}
class _RecordEditionState extends State<RecordEdition>{
  final note=TextEditingController();
  String kind='observation',error='';
  DateTime? follow;
  Data? frozen;
  bool busy=false;
  @override void initState(){super.initState();if(widget.followUp!=null)kind='outcome';}
  @override void dispose(){note.dispose();super.dispose();}
  Future<void> chooseDate()async{
    final today=DateTime.now();
    final date=await showDatePicker(context:context,firstDate:DateTime(today.year,today.month,today.day),
      lastDate:DateTime(today.year+3,today.month,today.day),initialDate:follow??today);
    if(date!=null&&mounted)setState(()=>follow=date);
  }
  Future<void> save()async{
    if(busy)return;
    if(frozen==null){
      if(note.text.trim().isEmpty){setState(()=>error='Write what you actually did or noticed.');return;}
      frozen={
        'client_submission_id':uuid(),'kind':kind,'evidence_type':'observed',
        'note':note.text.trim(),'observed_at':DateTime.now().toUtc().toIso8601String(),
        if(follow!=null)'follow_up_on':dateOnly(follow!.toIso8601String()),
        if(widget.followUp!=null)'follow_up_for':field(widget.followUp!,'id'),
      };
    }
    final body=frozen!;
    setState((){busy=true;error='';});
    try{
      final result=await widget.gateway.post('plantupia/plants/${field(widget.plant,'id')}/event-submissions/',body);
      if(result['client_submission_id']!=body['client_submission_id']||field(result,'id').isEmpty){
        throw const ApiProblem(0,'The record has not been confirmed. Retry the same submission.');
      }
      if(mounted)Navigator.of(context).pop(true);
    }catch(e){
      if(mounted)setState((){
        error=explain(e);
        // An explicit validation rejection can be corrected with a new request.
        // Unknown network/response errors retain the exact frozen payload.
        if(e is ApiProblem&&e.status==400)frozen=null;
      });
    }finally{if(mounted)setState(()=>busy=false);}
  }
  Future<bool> mayLeave()async{
    if(busy)return false;
    if(frozen==null)return true;
    return await showDialog<bool>(context:context,builder:(context)=>AlertDialog(
      title:const Text('Confirmation is still uncertain'),
      content:const Text('The request might have reached the service. Retry the same submission, or leave and inspect the plant history before creating another record.'),
      actions:[TextButton(onPressed:()=>Navigator.pop(context,false),child:const Text('Keep editing')),
        TextButton(onPressed:()=>Navigator.pop(context,true),child:const Text('Leave and check history'))]))??false;
  }
  @override Widget build(BuildContext context)=>PopScope<bool>(canPop:!busy&&frozen==null,
    onPopInvokedWithResult:(didPop,result)async{
      if(didPop)return;
      if(await mayLeave()&&context.mounted)Navigator.of(context).pop(true);
    },child:EditionPage(label:'Private moment',children:[
      const Eyebrow('Only you and authorized members'),const SizedBox(height:16),
      TitleText('A moment with\n${plantName(widget.plant)}.',size:36),
      const SizedBox(height:18),const Text('Record a real observation or action. Nothing is published to the community.'),
      const SizedBox(height:23),
      if(widget.followUp==null)Wrap(spacing:8,runSpacing:8,children:[
        for(final k in ['observation','care','issue'])ChoiceChip(label:Text(k=='issue'?'Concern':k=='care'?'Care':'Observation'),
          selected:kind==k,onSelected:frozen!=null||busy?null:(_)=>setState(()=>kind=k))
      ])else const Eyebrow('Outcome of the selected check-in'),
      const SizedBox(height:17),TextField(controller:note,minLines:4,maxLines:7,maxLength:1500,
        enabled:!busy&&frozen==null,decoration:const InputDecoration(labelText:'What happened?',alignLabelWithHint:true)),
      if(widget.followUp==null)...[
        const Rule(),ActionLine(follow==null?'Choose a future check-in':dateOnly(follow!.toIso8601String()),
          detail:'Optional. A human-selected date, not a prediction.',onTap:busy||frozen!=null?null:chooseDate,
          icon:Icons.calendar_today_outlined),
        if(follow!=null&&frozen==null&&!busy)TextButton(onPressed:()=>setState(()=>follow=null),child:const Text('Remove date')),
      ],
      if(error.isNotEmpty)Notice('Not confirmed',error),
      if(frozen!=null&&!busy)const Text('Retry keeps the original note, date and submission identity.',
        style:TextStyle(color:muted,fontSize:12)),
      const SizedBox(height:18),FilledButton(onPressed:busy?null:save,child:Text(busy?'Saving…':frozen!=null?'Retry this record':'Save private record')),
    ]));
}
