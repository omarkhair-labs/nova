import 'package:flutter/material.dart';
import 'package:flutter/painting.dart';
import 'gateway.dart';
import 'edition.dart';
import 'plant_pages.dart';
import 'shop_pages.dart';
import 'people_pages.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  configureSystemBars();
  runApp(PlantupiaApp(gateway:OracleReviewGateway(
    const String.fromEnvironment('PLANTUPIA_QA_API'))));
}

class PlantupiaApp extends StatelessWidget {
  final Gateway gateway;
  final Data? reviewUser;
  const PlantupiaApp({super.key,required this.gateway,this.reviewUser});
  @override Widget build(BuildContext context)=>MaterialApp(
    title:'Plantupia',debugShowCheckedModeBanner:false,theme:editionTheme(),
    home:SessionPage(gateway:gateway,reviewUser:reviewUser));
}
class SessionPage extends StatefulWidget {
  final Gateway gateway;
  final Data? reviewUser;
  const SessionPage({super.key,required this.gateway,this.reviewUser});
  @override State<SessionPage> createState()=>_SessionPageState();
}
class _SessionPageState extends State<SessionPage> {
  Data? _user;
  int _generation=0;
  @override void initState(){super.initState();_user=widget.reviewUser;}
  void logout(){
    widget.gateway.clear();
    PaintingBinding.instance.imageCache.clear();
    PaintingBinding.instance.imageCache.clearLiveImages();
    Navigator.of(context).popUntil((route)=>route.isFirst);
    if(mounted)setState((){_user=null;_generation++;});
  }
  @override Widget build(BuildContext context)=>_user==null
    ? Arrival(gateway:widget.gateway,onLogin:(user){if(mounted)setState(()=>_user=user);})
    : LivingEdition(key:ValueKey(_generation),gateway:widget.gateway,user:_user!,onLogout:logout);
}

class Arrival extends StatefulWidget {
  final Gateway gateway;
  final ValueChanged<Data> onLogin;
  const Arrival({super.key,required this.gateway,required this.onLogin});
  @override State<Arrival> createState()=>_ArrivalState();
}
class _ArrivalState extends State<Arrival> {
  bool form=false,busy=false,showPassword=false;
  String error='';
  final email=TextEditingController(),password=TextEditingController();
  @override void dispose(){email.dispose();password.dispose();super.dispose();}
  Future<void> login()async{
    if(busy)return;
    if(email.text.trim().isEmpty||password.text.isEmpty){setState(()=>error='Enter your email and password.');return;}
    setState((){busy=true;error='';});
    try{
      final user=await widget.gateway.login(email.text,password.text);
      if(mounted){password.clear();widget.onLogin(user);}
    }catch(e){if(mounted)setState(()=>error=explain(e));}
    finally{if(mounted)setState(()=>busy=false);}
  }
  @override Widget build(BuildContext context){
    if(form)return Scaffold(
      appBar:AppBar(leading:IconButton(onPressed:busy?null:()=>setState(()=>form=false),
        icon:const Icon(Icons.arrow_back),tooltip:'Return to entrance')),
      body:SafeArea(top:false,child:ListView(padding:const EdgeInsets.fromLTRB(26,8,26,36),children:[
        const Eyebrow('Plantupia / 01 — Arrival'),const SizedBox(height:42),
        const TitleText('Come back\nto your plants.',size:43),
        const SizedBox(height:18),const Text('Your existing Plantupia account, your places and your records.'),
        const SizedBox(height:34),TextField(controller:email,keyboardType:TextInputType.emailAddress,
          autofillHints:const [AutofillHints.email],decoration:const InputDecoration(labelText:'Email address'),enabled:!busy),
        const SizedBox(height:16),TextField(controller:password,obscureText:!showPassword,
          autofillHints:const [AutofillHints.password],enabled:!busy,
          onSubmitted:(_)=>login(),decoration:InputDecoration(labelText:'Password',
          suffixIcon:IconButton(onPressed:()=>setState(()=>showPassword=!showPassword),
            icon:Icon(showPassword?Icons.visibility_off:Icons.visibility),tooltip:'Show or hide password'))),
        if(error.isNotEmpty) Padding(padding:const EdgeInsets.symmetric(vertical:12),
          child:Text(error,style:const TextStyle(color:Color(0xff9e3930)))),
        const SizedBox(height:24),FilledButton(onPressed:busy?null:login,child:Text(busy?'Signing in…':'Sign in')),
        const SizedBox(height:16),const Text('Independent review edition. Sign in again after restarting.',
          style:TextStyle(color:muted,fontSize:12)),
      ])));
    return Scaffold(backgroundColor:forest,body:SafeArea(child:LayoutBuilder(builder:(context,limits)=>
      SingleChildScrollView(child:ConstrainedBox(constraints:BoxConstraints(minHeight:limits.maxHeight),
        child:Column(crossAxisAlignment:CrossAxisAlignment.stretch,children:[
          const Padding(padding:EdgeInsets.fromLTRB(24,18,24,12),child:Row(children:[
            Expanded(child:TitleText('Plantupia',size:28,color:chalk)),
            Eyebrow('Edition 01',color:sage)])),
          SizedBox(height:(limits.maxHeight*.39).clamp(205.0,380.0).toDouble(),
            child:Stack(fit:StackFit.expand,children:[
              const BotanicalPlate(background:Color(0xff224739),stroke:sage,caption:'An original botanical study'),
              Image.network('https://images.pexels.com/photos/31814108/pexels-photo-31814108.jpeg?auto=compress&cs=tinysrgb&w=1200',
                fit:BoxFit.cover,errorBuilder:(_,__,___)=>const SizedBox.shrink()),
              const DecoratedBox(decoration:BoxDecoration(gradient:LinearGradient(
                begin:Alignment.topCenter,end:Alignment.bottomCenter,colors:[Colors.transparent,Color(0x990e231c)]))),
              const Positioned(left:20,right:20,bottom:16,child:Eyebrow(
                'Architecture & living material / reference',color:chalk)),
            ])),
          Padding(padding:const EdgeInsets.fromLTRB(24,24,24,25),child:
            Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
              const Eyebrow('A living relationship',color:sage),const SizedBox(height:12),
              const TitleText('A place.\nA living thing.\nA little care.',size:43,color:chalk),
              const SizedBox(height:16),const Text('A quieter way to live with the plants around you.',
                style:TextStyle(color:Color(0xffcfddce),fontSize:15,height:1.5)),
              const SizedBox(height:23),SizedBox(width:double.infinity,child:FilledButton(
                style:FilledButton.styleFrom(backgroundColor:paper,foregroundColor:forest),
                onPressed:()=>setState(()=>form=true),child:const Row(children:[
                  Expanded(child:Text('Enter your space')),Icon(Icons.arrow_forward,size:19)]))),
            ])),
        ])))));
  }
}

class Library {
  final List<Data> spaces,plants;
  const Library(this.spaces,this.plants);
  List<Data> get active=>plants.where((p)=>p['is_active']==true).toList();
  List<Data> get due=>active.where((p)=>field(p,'next_follow_up_on').isNotEmpty &&
    dateOnly(field(p,'next_follow_up_on')).compareTo(localDay())<=0).toList();
  String spaceName(Data plant) {
    for(final s in spaces){if(s['id']==plant['space'])return field(s,'name');}
    return 'Your collection';
  }
}
String plantName(Data p)=>field(p,'my_name',field(p,'label','Unnamed plant'));
String plantContext(Data p)=>[field(p,'species_label'),field(p,'placement_note')].where((s)=>s.isNotEmpty).join(' · ');
Future<Library> loadLibrary(Gateway g)async{
  final r=await Future.wait([g.get('plantupia/spaces/'),g.get('plantupia/plants/')]);
  return Library(rows(r[0]['results']),rows(r[1]['results']));
}

class LivingEdition extends StatefulWidget {
  final Gateway gateway;
  final Data user;
  final VoidCallback onLogout;
  const LivingEdition({super.key,required this.gateway,required this.user,required this.onLogout});
  @override State<LivingEdition> createState()=>_LivingEditionState();
}
class _LivingEditionState extends State<LivingEdition>{
  int selected=0;
  late Future<Library> library;
  @override void initState(){super.initState();library=loadLibrary(widget.gateway);}
  Future<void> refresh()async{
    final next=loadLibrary(widget.gateway);setState(()=>library=next);
    try{await next;}catch(_){}
  }
  Future<void> openPlant(Data p,Library l)async{
    final changed=await openEdition<bool>(context,PlantEdition(gateway:widget.gateway,
      plant:p,spaceName:l.spaceName(p)));
    if(mounted&&changed==true)await refresh();
  }
  Future<void> menu()async{
    await openEdition<void>(context,AccountEdition(gateway:widget.gateway,user:widget.user,
      onLogout:widget.onLogout));
    if(mounted)await refresh();
  }
  Widget libraryPage(bool today)=>FutureBuilder<Library>(future:library,builder:(context,s){
    if(s.connectionState!=ConnectionState.done)return const Loading();
    if(s.hasError)return ListView(padding:const EdgeInsets.all(22),children:[
      const TitleText('Your living world.'),Notice('Could not open your collection',explain(s.error),retry:refresh)]);
    final l=s.data!;
    return today?TodayEdition(library:l,gateway:widget.gateway,user:widget.user,
      onPlant:(p)=>openPlant(p,l),onCollection:()=>setState(()=>selected=1),onRefresh:refresh)
      :CollectionEdition(library:l,gateway:widget.gateway,onPlant:(p)=>openPlant(p,l),onRefresh:refresh);
  });
  @override Widget build(BuildContext context)=>Scaffold(body:SafeArea(child:Column(children:[
    AccountTop(onMenu:menu),const Rule(),
    Expanded(child:IndexedStack(index:selected,children:[
      libraryPage(true),libraryPage(false),
      ShopEdition(gateway:widget.gateway,active:selected==2),
      AskEdition(gateway:widget.gateway,active:selected==3,library:library),
    ])),
    const Rule(),_EditionNav(selected:selected,onSelect:(i)=>setState(()=>selected=i)),
  ])));
}
class _EditionNav extends StatelessWidget {
  final int selected;
  final ValueChanged<int> onSelect;
  const _EditionNav({required this.selected,required this.onSelect});
  @override Widget build(BuildContext context){
    const labels=['Today','Plants','Shop','Ask'];
    const symbols=[Icons.wb_sunny_outlined,Icons.spa_outlined,Icons.shopping_bag_outlined,Icons.auto_awesome_outlined];
    return Row(children:List.generate(labels.length,(i)=>Expanded(child:Semantics(
      button:true,selected:i==selected,child:InkWell(onTap:()=>onSelect(i),child:
        Padding(padding:const EdgeInsets.symmetric(vertical:9,horizontal:2),child:Column(mainAxisSize:MainAxisSize.min,children:[
          AnimatedContainer(duration:const Duration(milliseconds:180),height:2,width:i==selected?32:0,color:forest),
          const SizedBox(height:7),Icon(symbols[i],size:23,color:i==selected?forest:muted),
          const SizedBox(height:4),Text(labels[i],style:TextStyle(fontSize:12,fontWeight:i==selected?FontWeight.w600:FontWeight.w400,
            color:i==selected?forest:muted),textAlign:TextAlign.center),
        ]))))));
  }
}
class TodayEdition extends StatelessWidget {
  final Library library;
  final Gateway gateway;
  final Data user;
  final ValueChanged<Data> onPlant;
  final VoidCallback onCollection;
  final Future<void> Function() onRefresh;
  const TodayEdition({super.key,required this.library,required this.gateway,required this.user,
    required this.onPlant,required this.onCollection,required this.onRefresh});
  @override Widget build(BuildContext context){
    final plants=library.active;
    final due=library.due;
    final Data? feature=plants.isEmpty?null:plants.firstWhere((p)=>field(p,'cover_photo').isNotEmpty,
      orElse:()=>plants.first);
    const months=['January','February','March','April','May','June','July','August','September','October','November','December'];
    final d=DateTime.now();
    return RefreshIndicator(onRefresh:onRefresh,child:ListView(physics:const AlwaysScrollableScrollPhysics(),
      padding:const EdgeInsets.fromLTRB(22,22,22,30),children:[
        Row(children:[Expanded(child:Eyebrow('${d.day} ${months[d.month-1]} / Your day')),const Eyebrow('01')]),
        const SizedBox(height:18),const TitleText('Living with\nnature.',size:46),
        const SizedBox(height:16),Text('A little attention makes a place yours.',
          style:TextStyle(fontSize:15,color:muted)),
        const SizedBox(height:25),
        if(feature!=null) ...[
          InkWell(onTap:()=>onPlant(feature),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
            SizedBox(height:280,child:Stack(fit:StackFit.expand,children:[
              PrivatePhoto(gateway:gateway,reference:field(feature,'cover_photo')),
              const Positioned(top:12,left:12,child:ColoredBox(color:paper,child:Padding(
                padding:EdgeInsets.symmetric(horizontal:9,vertical:5),child:Eyebrow('From your collection')))),
            ])),
            Container(color:forest,padding:const EdgeInsets.fromLTRB(18,19,18,18),
              child:Row(children:[Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
                Eyebrow(library.spaceName(feature),color:sage),const SizedBox(height:9),
                TitleText(plantName(feature),size:31,color:chalk),
              ])),const Icon(Icons.arrow_outward,color:chalk,size:24)])),
          ])),
        ] else const Notice('A place to begin','Your own plant records will appear here when they are added or handed over.'),
        const SizedBox(height:28),const Rule(),const SizedBox(height:15),
        Row(crossAxisAlignment:CrossAxisAlignment.end,children:[
          const Expanded(child:TitleText('Your attention',size:29)),
          Text(due.length.toString().padLeft(2,'0'),style:const TextStyle(fontFamily:'serif',fontSize:34,color:forest))
        ]),
        const SizedBox(height:5),Text(due.isEmpty?'No recorded check-ins due today.':'Recorded check-ins due. Observe before acting.',
          style:const TextStyle(color:muted,fontSize:13)),
        for(final p in due.take(5)) ...[const Rule(),ActionLine(plantName(p),
          detail:'Check-in · '+dateOnly(field(p,'next_follow_up_on')),onTap:()=>onPlant(p))],
        if(due.length>5) ActionLine('See the collection',onTap:onCollection),
        const SizedBox(height:17),const Rule(),ActionLine('All your plants',detail:'${plants.length} active plants · ${library.spaces.length} spaces',
          onTap:onCollection,icon:Icons.arrow_forward),
      ]));
  }
}
class CollectionEdition extends StatefulWidget {
  final Library library;
  final Gateway gateway;
  final ValueChanged<Data> onPlant;
  final Future<void> Function() onRefresh;
  const CollectionEdition({super.key,required this.library,required this.gateway,required this.onPlant,required this.onRefresh});
  @override State<CollectionEdition> createState()=>_CollectionEditionState();
}
class _CollectionEditionState extends State<CollectionEdition>{
  String? space;
  String query='';
  bool archived=false;
  @override Widget build(BuildContext context){
    final l=widget.library;
    final items=l.plants.where((p)=>(archived?p['is_active']==false:p['is_active']==true)
      &&(space==null||p['space']==space)
      &&('${plantName(p)} ${plantContext(p)}').toLowerCase().contains(query.trim().toLowerCase())).toList();
    return RefreshIndicator(onRefresh:widget.onRefresh,child:ListView(physics:const AlwaysScrollableScrollPhysics(),
      padding:const EdgeInsets.fromLTRB(22,23,22,35),children:[
        const Eyebrow('02 / Personal collection'),const SizedBox(height:15),
        const TitleText('Things we\ngrow.',size:45),const SizedBox(height:16),
        Text('${l.active.length} living records across ${l.spaces.length} spaces.',style:const TextStyle(color:muted)),
        const SizedBox(height:22),const Rule(),const SizedBox(height:9),
        DropdownButtonFormField<String>(value:l.spaces.any((s)=>s['id']==space)?space:'',
          decoration:const InputDecoration(labelText:'Place'),isExpanded:true,
          items:[const DropdownMenuItem(value:'',child:Text('All spaces')),
            for(final s in l.spaces)DropdownMenuItem(value:field(s,'id'),child:Text(field(s,'name'),overflow:TextOverflow.ellipsis))],
          onChanged:(v)=>setState(()=>space=v==''?null:v)),
        const SizedBox(height:12),TextField(onChanged:(v)=>setState(()=>query=v),
          decoration:const InputDecoration(hintText:'Find a plant',prefixIcon:Icon(Icons.search))),
        SwitchListTile(contentPadding:EdgeInsets.zero,title:const Text('Archived plants'),value:archived,
          onChanged:(v)=>setState(()=>archived=v)),
        if(items.isEmpty)const Notice('Nothing here yet','No plants match this view.'),
        // A photographic contact sheet, not boxed dashboard cards.
        LayoutBuilder(builder:(context,box){
          final scale=MediaQuery.textScalerOf(context).scale(14)/14;
          final columns=box.maxWidth>=300&&scale<1.4?2:1;
          return Wrap(spacing:13,runSpacing:25,children:[
            for(final p in items) SizedBox(width:(box.maxWidth-(columns-1)*13)/columns,
              child:InkWell(onTap:()=>widget.onPlant(p),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
                AspectRatio(aspectRatio:0.88,child:PrivatePhoto(gateway:widget.gateway,
                  reference:field(p,'cover_photo'))),
                const SizedBox(height:10),TitleText(plantName(p),size:23),
                const SizedBox(height:4),Text(l.spaceName(p),style:const TextStyle(color:muted,fontSize:12)),
                if(field(p,'species_label').isNotEmpty)Text(field(p,'species_label'),
                  style:const TextStyle(color:muted,fontStyle:FontStyle.italic,fontSize:12)),
              ])))
          ]);
        }),
      ]));
  }
}
