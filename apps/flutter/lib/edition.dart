import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'gateway.dart';

const forest = Color(0xff123b2e);
const ink = Color(0xff16302a);
const paper = Color(0xfff3eede);
const chalk = Color(0xfffaf8f0);
const sage = Color(0xff9fb79a);
const line = Color(0xffc8cec0);
const muted = Color(0xff53685b);

ThemeData editionTheme() => ThemeData(
  useMaterial3: true,
  scaffoldBackgroundColor: paper,
  colorScheme: ColorScheme.fromSeed(seedColor: forest, surface: paper),
  visualDensity: VisualDensity.standard,
  textTheme: const TextTheme(bodyMedium: TextStyle(fontSize: 15, height: 1.45, color: ink),
      bodyLarge: TextStyle(fontSize: 17, height: 1.45, color: ink)),
  appBarTheme: const AppBarTheme(backgroundColor: paper, foregroundColor: ink, elevation: 0,
      scrolledUnderElevation: 0, centerTitle: false),
  dividerTheme: const DividerThemeData(color: line, thickness: 0.8, space: 1),
  inputDecorationTheme: const InputDecorationTheme(
    filled: false, contentPadding: EdgeInsets.symmetric(vertical: 14, horizontal: 12),
    border: OutlineInputBorder(borderRadius: BorderRadius.zero, borderSide: BorderSide(color: line)),
    enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.zero, borderSide: BorderSide(color: line)),
    focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.zero, borderSide: BorderSide(color: forest)),
  ),
  filledButtonTheme: FilledButtonThemeData(style: FilledButton.styleFrom(
    backgroundColor: forest, foregroundColor: chalk, minimumSize: const Size(48,54),
    shape: const RoundedRectangleBorder(borderRadius: BorderRadius.zero),
    padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14))),
);

class TitleText extends StatelessWidget {
  final String text;
  final double size;
  final Color color;
  final bool italic;
  const TitleText(this.text,{super.key,this.size=38,this.color=ink,this.italic=false});
  @override
  Widget build(BuildContext context) => Text(text, style: TextStyle(fontFamily:'serif',
    fontSize:size, fontWeight:FontWeight.w400, height:1.09, letterSpacing:-0.9,
    fontStyle:italic?FontStyle.italic:FontStyle.normal, color:color));
}
class Eyebrow extends StatelessWidget {
  final String text;
  final Color color;
  const Eyebrow(this.text,{super.key,this.color=muted});
  @override
  Widget build(BuildContext context) => Text(text.toUpperCase(),
    style:TextStyle(fontSize:11, letterSpacing:1.5, height:1.5,fontWeight:FontWeight.w500,color:color));
}
class Rule extends StatelessWidget {
  final Color color;
  const Rule({super.key,this.color=line});
  @override Widget build(BuildContext context) => Container(height:.8,color:color);
}
class ActionLine extends StatelessWidget {
  final String label;
  final String? detail;
  final VoidCallback? onTap;
  final IconData icon;
  const ActionLine(this.label,{super.key,this.detail,this.onTap,this.icon=Icons.arrow_outward});
  @override
  Widget build(BuildContext context) => InkWell(onTap:onTap,child:Padding(
    padding:const EdgeInsets.symmetric(vertical:16),
    child:Row(crossAxisAlignment:CrossAxisAlignment.center,children:[
      Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        Text(label,style:const TextStyle(fontSize:17,color:ink,height:1.25)),
        if(detail!=null) Padding(padding:const EdgeInsets.only(top:5),
          child:Text(detail!,style:const TextStyle(color:muted,fontSize:13,height:1.4)))
      ])), const SizedBox(width:12),Icon(icon,size:20,color:onTap==null?muted:forest),
    ])));
}
class Notice extends StatelessWidget {
  final String title, body;
  final VoidCallback? retry;
  const Notice(this.title,this.body,{super.key,this.retry});
  @override
  Widget build(BuildContext context) => Padding(padding:const EdgeInsets.symmetric(vertical:20),child:
    Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
      const Rule(), const SizedBox(height:16),TitleText(title,size:27),
      const SizedBox(height:9),Text(body,style:const TextStyle(color:muted,height:1.5)),
      if(retry!=null) TextButton(onPressed:retry,child:const Text('Try again')),
      const SizedBox(height:12),const Rule(),
    ]));
}
String explain(Object? error) => error is ApiProblem ? error.message :
    'The connection could not be completed. Please try again.';
class Loading extends StatelessWidget {
  const Loading({super.key});
  @override Widget build(BuildContext context)=> const Padding(padding:EdgeInsets.all(36),
    child:Center(child:CircularProgressIndicator(strokeWidth:1.5,color:forest)));
}

// An original line drawing, deliberately abstract. Never passed off as a
// photograph of a customer's plant or a measured botanical illustration.
class BotanicalPlate extends StatelessWidget {
  final Color background, stroke;
  final String caption;
  const BotanicalPlate({super.key,this.background=const Color(0xffe6e9dc),this.stroke=const Color(0xff53685b),
    this.caption='No photograph saved'});
  @override Widget build(BuildContext context) => ColoredBox(color:background,child:Stack(
    fit:StackFit.expand,children:[
      CustomPaint(painter:_PlatePainter(stroke)),
      Align(alignment:Alignment.bottomCenter,child:Padding(padding:const EdgeInsets.all(12),
        child:Text(caption,textAlign:TextAlign.center,style:TextStyle(color:stroke,fontSize:11,letterSpacing:.3))))
    ]));
}
class _PlatePainter extends CustomPainter {
  final Color color;
  _PlatePainter(this.color);
  @override void paint(Canvas canvas, Size size) {
    if(size.width<1 || size.height<1)return;
    final p=Paint()..color=color.withValues(alpha:.55)..style=PaintingStyle.stroke..strokeWidth=0.85;
    final x=size.width*.48,y=size.height*.81, h=size.height*.63,w=size.width*.35;
    final stem=Path()..moveTo(x,y)..cubicTo(x-w*.2,y-h*.25,x+w*.4,y-h*.75,x+w*.1,y-h);
    canvas.drawPath(stem,p);
    for(var i=0;i<5;i++){
      final side=i.isEven?1.0:-1.0;
      final yy=y-h*(.18+i*.16);
      final xx=x+w*(.06+i*.025);
      final tip=Offset(xx+side*w*(.9-i*.08),yy-h*.19);
      final leaf=Path()..moveTo(xx,yy)..quadraticBezierTo(xx+side*w*.13,yy-h*.26,tip.dx,tip.dy)
        ..quadraticBezierTo(xx+side*w*.8,yy+h*.025,xx,yy);
      canvas.drawPath(leaf,p);
      canvas.drawLine(Offset(xx,yy),tip,p);
    }
    canvas.drawOval(Rect.fromCenter(center:Offset(x,y),width:size.width*.44,height:size.height*.045),p);
  }
  @override bool shouldRepaint(covariant _PlatePainter oldDelegate)=>oldDelegate.color!=color;
}
class PrivatePhoto extends StatefulWidget {
  final Gateway gateway;
  final String reference;
  final String caption;
  final bool catalog;
  final BoxFit fit;
  const PrivatePhoto({super.key,required this.gateway,required this.reference,
    this.caption='No photograph saved',this.catalog=false,this.fit=BoxFit.cover});
  @override State<PrivatePhoto> createState()=>_PrivatePhotoState();
}
class _PrivatePhotoState extends State<PrivatePhoto> {
  late Future<Uint8List?> _future;
  @override void initState(){super.initState();_load();}
  void _load(){_future=widget.reference.isEmpty?Future.value(null):
    widget.gateway.picture(widget.reference,catalog:widget.catalog);}
  @override void didUpdateWidget(covariant PrivatePhoto oldWidget){
    super.didUpdateWidget(oldWidget);
    if(oldWidget.reference!=widget.reference || oldWidget.gateway!=widget.gateway ||
      oldWidget.catalog!=widget.catalog) _load();
  }
  @override Widget build(BuildContext context)=>FutureBuilder<Uint8List?>(future:_future,
    builder:(context,snapshot){
      if(snapshot.connectionState!=ConnectionState.done) return const ColoredBox(color:Color(0xffe2e7da),
        child:Center(child:SizedBox(width:22,height:22,child:CircularProgressIndicator(strokeWidth:1.2))));
      final data=snapshot.data;
      if(data==null || data.isEmpty)return BotanicalPlate(caption:widget.reference.isEmpty?
        widget.caption:'Photograph unavailable');
      return Image.memory(data,fit:widget.fit,width:double.infinity,height:double.infinity,
        gaplessPlayback:false,errorBuilder:(_,__,___)=>const BotanicalPlate(caption:'Photograph unavailable'));
    });
}
Future<T?> openEdition<T>(BuildContext context,Widget page)=>Navigator.of(context).push<T>(
  PageRouteBuilder<T>(pageBuilder:(_,__,___)=>page,
    transitionDuration:const Duration(milliseconds:360),
    reverseTransitionDuration:const Duration(milliseconds:240),
    transitionsBuilder:(_,a,__,child)=>FadeTransition(opacity:CurvedAnimation(parent:a,curve:Curves.easeOut),
      child:SlideTransition(position:Tween<Offset>(begin:const Offset(.035,0),end:Offset.zero)
        .animate(CurvedAnimation(parent:a,curve:Curves.easeOutCubic)),child:child))));
class EditionPage extends StatelessWidget {
  final String label;
  final List<Widget> children;
  final Widget? action;
  final bool scroll;
  final Widget? footer;
  const EditionPage({super.key,required this.label,required this.children,this.action,this.scroll=true,this.footer});
  @override Widget build(BuildContext context)=>Scaffold(
    appBar:AppBar(title:Eyebrow(label),actions:action==null?null:[action!]),
    body:SafeArea(top:false,child:Column(children:[
      Expanded(child:scroll?ListView(padding:const EdgeInsets.fromLTRB(22,10,22,24),children:children):
        Column(children:children)),
      if(footer!=null) ...[const Rule(),Padding(padding:const EdgeInsets.fromLTRB(22,10,22,12),
        child:SizedBox(width:double.infinity,child:footer!))],
    ])));
}
class AccountTop extends StatelessWidget {
  final VoidCallback onMenu;
  const AccountTop({super.key,required this.onMenu});
  @override Widget build(BuildContext context)=>Padding(padding:const EdgeInsets.fromLTRB(22,3,14,4),child:
    LayoutBuilder(builder:(context,box)=>Row(children:[
      const Expanded(child:Text('Plantupia',style:TextStyle(fontFamily:'serif',fontSize:25,
        color:ink,letterSpacing:-.8),maxLines:1,overflow:TextOverflow.ellipsis)),
      if(box.maxWidth>=320 && MediaQuery.textScalerOf(context).scale(12)<=15)
        const Eyebrow('Living edition'),const SizedBox(width:8),
      IconButton(tooltip:'Your account and more',onPressed:onMenu,icon:const Icon(Icons.add,size:26))])));
}
void configureSystemBars()=>SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
  statusBarColor:paper,statusBarIconBrightness:Brightness.dark,
  systemNavigationBarColor:paper,systemNavigationBarIconBrightness:Brightness.dark));
