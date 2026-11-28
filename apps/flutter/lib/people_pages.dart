import 'package:flutter/material.dart';
import 'gateway.dart';
import 'edition.dart';
import 'main.dart' show Library,plantName;
import 'shop_pages.dart';

class AskEdition extends StatefulWidget{
  final Gateway gateway;
  final bool active;
  final Future<Library> library;
  const AskEdition({super.key,required this.gateway,required this.active,required this.library});
  @override State<AskEdition> createState()=>_AskEditionState();
}
class _AskEditionState extends State<AskEdition>{
  final question=TextEditingController();
  final List<({String question,Data response})> letters=[];
  String? plantId;
  bool consent=false,busy=false;
  String error='';
  @override void dispose(){question.dispose();super.dispose();}
  Future<void> ask(List<Data> plants)async{
    final message=question.text.trim();
    if(busy||message.isEmpty)return;
    final selected=plants.any((p)=>p['id']==plantId)?plantId:null;
    final share=consent&&selected!=null;
    final confirmed=await showDialog<bool>(context:context,builder:(context)=>AlertDialog(
      title:const Text('Send your question?'),
      content:Text(share?'Your question and the selected plant’s recent private observations and placement will be sent to the configured AI service.':
        'Your question will be sent to the configured AI service. No saved plant context is included.'),
      actions:[TextButton(onPressed:()=>Navigator.pop(context,false),child:const Text('Cancel')),
        FilledButton(onPressed:()=>Navigator.pop(context,true),child:const Text('Send question'))]));
    if(confirmed!=true||!mounted||busy)return;
    setState((){busy=true;error='';consent=false;});
    try{
      final result=await widget.gateway.post('plantupia/assistant/ask/',{
        'message':message,
        if(share)'plant_id':selected,
        'include_my_plant_context':share,
        'include_recorded_evidence':false,
        'include_guide_and_layout':false,
        'conversation':[],
      });
      if(field(result,'answer').isEmpty)throw const ApiProblem(0,'No answer was returned.');
      if(mounted)setState((){
        letters.add((question:message,response:result));
        question.clear();
      });
    }catch(e){if(mounted)setState(()=>error=explain(e));}
    finally{if(mounted)setState(()=>busy=false);}
  }
  @override Widget build(BuildContext context)=>ListView(padding:const EdgeInsets.fromLTRB(22,24,22,32),children:[
    const Eyebrow('04 / A conversation'),const SizedBox(height:16),const TitleText('Something\nyou noticed?',size:45),
    const SizedBox(height:15),
    const Text('Ask Plantupia AI about care. Its answers are educational guidance, not a confirmed diagnosis.',style:TextStyle(color:muted)),
    const SizedBox(height:25),const Rule(),
    for(final letter in letters)...[
      const SizedBox(height:20),const Eyebrow('Your question'),const SizedBox(height:8),
      Text(letter.question,style:const TextStyle(fontSize:18)),
      const SizedBox(height:17),Container(padding:const EdgeInsets.all(17),color:chalk,
        child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
          const Eyebrow('Plantupia AI'),const SizedBox(height:12),SelectableText(field(letter.response,'answer'),
            style:const TextStyle(fontSize:16,height:1.55,color:ink)),
          const SizedBox(height:12),Text(field(letter.response,'uncertainty',
            'Educational guidance; not a confirmed diagnosis.'),style:const TextStyle(color:muted,fontSize:12)),
          for(final source in (letter.response['source_links'] is List?letter.response['source_links'] as List:const []))
            if(source is String)SelectableText(source,style:const TextStyle(fontSize:11,color:muted)),
        ])),
    ],
    const SizedBox(height:22),TextField(controller:question,enabled:!busy,minLines:3,maxLines:6,maxLength:1000,
      decoration:const InputDecoration(labelText:'Your question',alignLabelWithHint:true)),
    FutureBuilder<Library>(future:widget.library,builder:(context,s){
      final plants=s.data?.active??<Data>[];
      final selected=plants.any((p)=>p['id']==plantId)?plantId:'';
      return Column(crossAxisAlignment:CrossAxisAlignment.stretch,children:[
        if(plants.isNotEmpty)...[
          DropdownButtonFormField<String>(initialValue:selected,isExpanded:true,
            decoration:const InputDecoration(labelText:'Optional plant'),
            items:[const DropdownMenuItem(value:'',child:Text('General question')),
              for(final p in plants)DropdownMenuItem(value:field(p,'id'),child:Text(plantName(p),overflow:TextOverflow.ellipsis))],
            onChanged:busy?null:(value)=>setState((){plantId=value==''?null:value;consent=false;})),
          if(selected!='')CheckboxListTile(contentPadding:EdgeInsets.zero,
            title:const Text('Share this plant’s recorded context for this question'),
            value:consent,onChanged:busy?null:(v)=>setState(()=>consent=v==true)),
        ],
        const SizedBox(height:12),FilledButton(onPressed:busy?null:()=>ask(plants),child:Text(busy?'Waiting for a reply…':'Ask Plantupia AI')),
      ]);
    }),
    if(error.isNotEmpty)Notice('No reply yet',error),
    const SizedBox(height:13),const Text('Each question is sent separately. AI never changes your care history.',style:TextStyle(fontSize:12,color:muted)),
    const SizedBox(height:20),const Rule(),ActionLine('Prefer a person?',detail:'Read your private requests to the Plantupia team.',
      onTap:()=>openEdition<void>(context,TeamEdition(gateway:widget.gateway))),
  ]);
}
class AccountEdition extends StatelessWidget{
  final Gateway gateway;
  final Data user;
  final VoidCallback onLogout;
  const AccountEdition({super.key,required this.gateway,required this.user,required this.onLogout});
  @override Widget build(BuildContext context)=>EditionPage(label:'Your world',children:[
    const Eyebrow('Your account'),const SizedBox(height:13),
    TitleText(field(user,'name',field(user,'username','Your account')),size:42),
    const SizedBox(height:9),Text(field(user,'email'),style:const TextStyle(color:muted)),
    const SizedBox(height:29),const Rule(),
    ActionLine('Circle',detail:'Questions people chose to share',onTap:()=>openEdition<void>(context,CircleEdition(gateway:gateway))),
    const Rule(),ActionLine('Plantupia team',detail:'Private support and replies',onTap:()=>openEdition<void>(context,TeamEdition(gateway:gateway))),
    const Rule(),ActionLine('My orders',detail:'Recorded purchases and progress',onTap:()=>openEdition<void>(context,OrdersEdition(gateway:gateway))),
    const Rule(),const SizedBox(height:30),
    OutlinedButton(onPressed:()async{
      final confirmed=await showDialog<bool>(context:context,builder:(context)=>AlertDialog(
        title:const Text('Sign out of this edition?'),content:const Text('Your saved plants, cart and history stay in your account.'),
        actions:[TextButton(onPressed:()=>Navigator.pop(context,false),child:const Text('Cancel')),
          TextButton(onPressed:()=>Navigator.pop(context,true),child:const Text('Sign out'))]));
      if(confirmed==true&&context.mounted)onLogout();
    },child:const Text('Sign out')),
    const SizedBox(height:20),const Text('Flutter review edition · Android only',style:TextStyle(color:muted,fontSize:12)),
  ]);
}
class CircleEdition extends StatefulWidget{
  final Gateway gateway;
  const CircleEdition({super.key,required this.gateway});
  @override State<CircleEdition> createState()=>_CircleEditionState();
}
class _CircleEditionState extends State<CircleEdition>{
  List<Data> posts=[];
  String? cursor;
  String error='';
  bool busy=true;
  @override void initState(){super.initState();load();}
  Future<void> load({bool more=false})async{
    if(mounted)setState((){busy=true;error='';});
    try{
      final result=await widget.gateway.get('plantupia/circle/'+(more&&cursor!=null?'?cursor=${Uri.encodeQueryComponent(cursor!)}':''));
      if(!mounted)return;
      setState((){
        if(!more)posts=[];
        for(final p in rows(result['results'])){if(!posts.any((x)=>x['id']==p['id']))posts.add(p);}
        cursor=result['next_cursor']?.toString();
      });
    }catch(e){if(mounted)setState(()=>error=explain(e));}
    finally{if(mounted)setState(()=>busy=false);}
  }
  @override Widget build(BuildContext context)=>EditionPage(label:'The circle',children:[
    const Eyebrow('Human stories'),const SizedBox(height:16),const TitleText('Better\ntogether.',size:45),
    const SizedBox(height:15),const Text('Public plant questions people intentionally shared. Private records never appear here automatically.'),
    const SizedBox(height:20),
    if(posts.isEmpty&&!busy&&error.isEmpty)const Notice('Quiet today','There are no shared questions in this view.'),
    for(final item in posts)...[
      const Rule(),const SizedBox(height:17),
      Eyebrow(field(object(object(item['post'])['author']),'name',
        field(object(object(item['post'])['author']),'username','Community member'))),
      const SizedBox(height:9),TitleText(field(item,'topic','Plant question'),size:29),
      const SizedBox(height:10),Text(field(object(item['post']),'caption')),
      if(field(object(item['post']),'thumbnail_url',field(object(item['post']),'image_url')).isNotEmpty)
        Padding(padding:const EdgeInsets.only(top:14),child:AspectRatio(aspectRatio:1.15,child:PrivatePhoto(gateway:widget.gateway,
          reference:field(object(item['post']),'thumbnail_url',field(object(item['post']),'image_url'))))),
      const SizedBox(height:10),Text(item['resolved']==true?'Outcome recorded':'Open question',style:const TextStyle(color:muted,fontSize:12)),
      const SizedBox(height:20),
    ],
    if(error.isNotEmpty)Notice('Could not open the circle',error,retry:()=>load()),
    if(busy)const Loading()else if(cursor!=null&&error.isEmpty)
      OutlinedButton(onPressed:()=>load(more:true),child:const Text('More shared questions')),
    const Text('Reading view. Sharing and community replies remain in the existing Android app.',style:TextStyle(color:muted,fontSize:12)),
  ]);
}
class TeamEdition extends StatefulWidget{
  final Gateway gateway;
  const TeamEdition({super.key,required this.gateway});
  @override State<TeamEdition> createState()=>_TeamEditionState();
}
class _TeamEditionState extends State<TeamEdition>{
  late Future<Data> tickets;
  @override void initState(){super.initState();tickets=widget.gateway.get('plantupia/service-requests/');}
  @override Widget build(BuildContext context)=>EditionPage(label:'The team',children:[
    const Eyebrow('Private correspondence'),const SizedBox(height:14),
    const TitleText('Here for\nyour plants.',size:44),const SizedBox(height:16),
    const Text('Your existing requests and replies from the Plantupia team.'),
    const SizedBox(height:24),
    FutureBuilder<Data>(future:tickets,builder:(context,s){
      if(s.connectionState!=ConnectionState.done)return const Loading();
      if(s.hasError)return Notice('Requests unavailable',explain(s.error),retry:()=>setState(()=>tickets=widget.gateway.get('plantupia/service-requests/')));
      final items=rows(s.data?['results']);
      return Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        if(items.isEmpty)const Notice('No requests yet','Your private support conversations will appear here.'),
        for(final item in items)...[const Rule(),ActionLine(field(item,'subject','Support request'),
          detail:field(item,'plant_label')+' · '+field(item,'status').replaceAll('_',' '),
          onTap:()=>openEdition<void>(context,TicketEdition(gateway:widget.gateway,id:field(item,'id'))))],
        const Rule(),
      ]);
    }),
    const SizedBox(height:18),const Text('Reading view. New requests and replies remain in the existing Android app.',style:TextStyle(color:muted,fontSize:12)),
  ]);
}
class TicketEdition extends StatefulWidget{
  final Gateway gateway;
  final String id;
  const TicketEdition({super.key,required this.gateway,required this.id});
  @override State<TicketEdition> createState()=>_TicketEditionState();
}
class _TicketEditionState extends State<TicketEdition>{
  late Future<Data> ticket;
  @override void initState(){super.initState();ticket=widget.gateway.get('plantupia/service-requests/${widget.id}/');}
  @override Widget build(BuildContext context)=>EditionPage(label:'Private conversation',children:[
    FutureBuilder<Data>(future:ticket,builder:(context,s){
      if(s.connectionState!=ConnectionState.done)return const Loading();
      if(s.hasError)return Notice('Request unavailable',explain(s.error));
      final item=s.data!;
      return Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        TitleText(field(item,'subject','Request'),size:35),const SizedBox(height:10),
        Eyebrow(field(item,'status')),const SizedBox(height:23),
        for(final m in rows(item['messages']))...[
          const Rule(),const SizedBox(height:13),Eyebrow(m['from_staff']==true?'Plantupia team':'You'),
          const SizedBox(height:6),Text(field(m,'body')),const SizedBox(height:6),
          Text(dateOnly(field(m,'created_at')),style:const TextStyle(fontSize:12,color:muted)),
          const SizedBox(height:17),
        ],const SizedBox(height:10),Text(field(item,'notice'),style:const TextStyle(fontSize:12,color:muted)),
      ]);
    }),
  ]);
}
