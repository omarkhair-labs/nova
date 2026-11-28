import 'package:flutter/material.dart';
import 'gateway.dart';
import 'edition.dart';

String amount(Data data,String key)=>field(data,key,'—');
String productImage(Data product)=>product['has_image']==true
  ?'plantupia/commerce/catalog/${field(product,'product_id')}/image/':'';
class ShopEdition extends StatefulWidget {
  final Gateway gateway;
  final bool active;
  const ShopEdition({super.key,required this.gateway,required this.active});
  @override State<ShopEdition> createState()=>_ShopEditionState();
}
class _ShopEditionState extends State<ShopEdition>{
  Future<Data>? catalog;
  String kind='',query='';
  void load(){catalog=widget.gateway.get('plantupia/commerce/catalog/');}
  @override void initState(){super.initState();if(widget.active)load();}
  @override void didUpdateWidget(covariant ShopEdition old){
    super.didUpdateWidget(old);if(widget.active&&catalog==null)load();
  }
  @override Widget build(BuildContext context)=>ListView(padding:const EdgeInsets.fromLTRB(22,23,22,35),children:[
    Row(children:[
      const Expanded(child:Eyebrow('03 / The edited collection')),
      IconButton(tooltip:'Open cart',onPressed:()=>openEdition<void>(context,CartEdition(gateway:widget.gateway)),
        icon:const Icon(Icons.shopping_bag_outlined,size:25)),
    ]),
    const SizedBox(height:15),const TitleText('Objects\nwith life.',size:46),
    const SizedBox(height:13),const Text('Living material, thoughtful vessels and the things that care for them.',style:TextStyle(color:muted)),
    const SizedBox(height:23),const Rule(),ActionLine('My orders',onTap:()=>openEdition<void>(context,OrdersEdition(gateway:widget.gateway)),
      icon:Icons.receipt_long_outlined),const Rule(),const SizedBox(height:15),
    Wrap(spacing:7,runSpacing:5,children:[
      for(final pair in const [['','All'],['plant','Plants'],['planter','Pots'],['care','Care']])
        ChoiceChip(label:Text(pair[1]),selected:kind==pair[0],onSelected:(_)=>setState(()=>kind=pair[0]))
    ]),
    const SizedBox(height:12),TextField(onChanged:(v)=>setState(()=>query=v),
      decoration:const InputDecoration(hintText:'Find an object',prefixIcon:Icon(Icons.search))),
    const SizedBox(height:19),
    if(catalog!=null)FutureBuilder<Data>(future:catalog,builder:(context,s){
      if(s.connectionState!=ConnectionState.done)return const Loading();
      if(s.hasError)return Notice('Collection unavailable',explain(s.error),retry:()=>setState(load));
      final items=rows(s.data?['results']).where((p)=>(kind.isEmpty||field(p,'kind')==kind)
        &&('${field(p,'name')} ${field(p,'variant_label')}').toLowerCase().contains(query.trim().toLowerCase())).toList();
      if(items.isEmpty)return const Notice('An empty shelf','There are no published products in this view.');
      return LayoutBuilder(builder:(context,box){
        final columns=box.maxWidth>=300&&MediaQuery.textScalerOf(context).scale(14)<20?2:1;
        return Wrap(spacing:13,runSpacing:25,children:[
          for(final p in items)SizedBox(width:(box.maxWidth-13*(columns-1))/columns,
            child:InkWell(onTap:()=>openEdition<void>(context,ProductEdition(gateway:widget.gateway,product:p)),
              child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
                AspectRatio(aspectRatio:.83,child:PrivatePhoto(gateway:widget.gateway,
                  reference:productImage(p),caption:'No product image',catalog:true)),
                const SizedBox(height:11),TitleText(field(p,'name','Product'),size:22),
                if(field(p,'variant_label').isNotEmpty)Text(field(p,'variant_label'),style:const TextStyle(fontSize:12,color:muted)),
                const SizedBox(height:6),Text(amount(p,'price')+' '+field(p,'currency','EGP')),
                if(p['in_stock']!=true)const Eyebrow('Currently unavailable'),
              ])))
        ]);
      });
    }),
  ]);
}
class ProductEdition extends StatefulWidget {
  final Gateway gateway;
  final Data product;
  const ProductEdition({super.key,required this.gateway,required this.product});
  @override State<ProductEdition> createState()=>_ProductEditionState();
}
class _ProductEditionState extends State<ProductEdition>{
  int quantity=1;
  bool busy=false;
  String message='';
  String image='';
  @override void initState(){super.initState();image=productImage(widget.product);}
  int get maximum {
    final n=widget.product['available_units'];
    return n is int?n.clamp(0,10).toInt():0;
  }
  Future<void> save()async{
    if(busy||quantity>maximum||maximum==0)return;
    setState((){busy=true;message='';});
    try{
      final result=await widget.gateway.post('plantupia/commerce/cart/',{
        'sku_id':field(widget.product,'id'),'quantity':quantity
      });
      final found=rows(result['results']).any((r)=>object(r['product'])['id']==widget.product['id']&&r['quantity']==quantity);
      if(!found)throw const ApiProblem(0,'Cart change not confirmed. Open your saved cart to check.');
      if(mounted)setState(()=>message='Saved to your cart.');
    }catch(e){if(mounted)setState(()=>message=explain(e));}
    finally{if(mounted)setState(()=>busy=false);}
  }
  @override Widget build(BuildContext context){
    final p=widget.product;
    return EditionPage(label:'The collection',children:[
      Eyebrow(field(p,'kind','Object')),const SizedBox(height:14),TitleText(field(p,'name'),size:42),
      const SizedBox(height:16),AspectRatio(aspectRatio:.93,child:PrivatePhoto(gateway:widget.gateway,
        reference:image,catalog:true,caption:'No product image')),
      if(rows(p['photos']).isNotEmpty)Padding(padding:const EdgeInsets.only(top:10),child:Wrap(spacing:5,children:[
        TextButton(onPressed:()=>setState(()=>image=productImage(p)),child:const Text('Cover')),
        for(final photo in rows(p['photos']))TextButton(onPressed:()=>setState(()=>image=
          'plantupia/commerce/catalog/${field(p,'product_id')}/image/?photo_id=${Uri.encodeQueryComponent(field(photo,'id'))}'),
          child:Text('View ${rows(p['photos']).indexOf(photo)+1}')),
      ])),
      const SizedBox(height:19),TitleText(amount(p,'price')+' '+field(p,'currency','EGP'),size:32),
      if(field(p,'variant_label').isNotEmpty)Text(field(p,'variant_label'),style:const TextStyle(color:muted)),
      const SizedBox(height:17),Text(field(p,'description')),
      const SizedBox(height:22),const Rule(),
      if(maximum>0&&p['in_stock']==true)...[
        Row(children:[
          const Expanded(child:Text('Quantity in cart')),
          IconButton(tooltip:'Reduce quantity',onPressed:busy||quantity<=1?null:()=>setState(()=>quantity--),icon:const Icon(Icons.remove)),
          Text('$quantity'),IconButton(tooltip:'Increase quantity',onPressed:busy||quantity>=maximum?null:()=>setState(()=>quantity++),
            icon:const Icon(Icons.add)),
        ]),
        FilledButton(onPressed:busy?null:save,child:Text(busy?'Saving…':'Set cart quantity')),
      ]else const Padding(padding:EdgeInsets.symmetric(vertical:18),child:Eyebrow('Currently unavailable')),
      if(message.isNotEmpty)Padding(padding:const EdgeInsets.symmetric(vertical:12),child:Text(message)),
      ActionLine('Open saved cart',onTap:()=>openEdition<void>(context,CartEdition(gateway:widget.gateway))),
    ]);
  }
}
class CartEdition extends StatefulWidget{
  final Gateway gateway;
  const CartEdition({super.key,required this.gateway});
  @override State<CartEdition> createState()=>_CartEditionState();
}
class _CartEditionState extends State<CartEdition>{
  late Future<Data> cart;
  bool busy=false;
  String message='';
  @override void initState(){super.initState();cart=widget.gateway.get('plantupia/commerce/cart/');}
  Future<void> quantity(Data product,int q)async{
    if(busy||q<0||q>10)return;
    setState((){busy=true;message='';});
    try{
      final result=await widget.gateway.post('plantupia/commerce/cart/',{'sku_id':field(product,'id'),'quantity':q});
      if(mounted)setState(()=>cart=Future.value(result));
    }catch(e){if(mounted)setState(()=>message=explain(e));}
    finally{if(mounted)setState(()=>busy=false);}
  }
  @override Widget build(BuildContext context)=>EditionPage(label:'Your cart',children:[
    const Eyebrow('A considered collection'),const SizedBox(height:14),const TitleText('Make it yours.',size:42),const SizedBox(height:22),
    FutureBuilder<Data>(future:cart,builder:(context,s){
      if(s.connectionState!=ConnectionState.done)return const Loading();
      if(s.hasError)return Notice('Cart unavailable',explain(s.error),retry:()=>setState(()=>cart=widget.gateway.get('plantupia/commerce/cart/')));
      final result=s.data!,lines=rows(result['results']);
      return Column(crossAxisAlignment:CrossAxisAlignment.stretch,children:[
        if(lines.isEmpty)const Notice('Nothing selected','Your saved collection is empty.'),
        for(final row in lines)...[
          const Rule(),const SizedBox(height:15),
          Row(crossAxisAlignment:CrossAxisAlignment.start,children:[
            SizedBox(width:86,height:106,child:PrivatePhoto(gateway:widget.gateway,reference:productImage(object(row['product'])),
              catalog:true,caption:'No image')),
            const SizedBox(width:13),Expanded(child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
              TitleText(field(object(row['product']),'name'),size:24),
              Text(field(object(row['product']),'variant_label'),style:const TextStyle(color:muted,fontSize:12)),
              Text(amount(row,'line_total')+' EGP'),
              Wrap(crossAxisAlignment:WrapCrossAlignment.center,children:[
                IconButton(tooltip:'Remove one',onPressed:busy?null:()=>quantity(object(row['product']),((row['quantity'] as int?)??1)-1),icon:const Icon(Icons.remove)),
                Text('${row['quantity']}'),
                IconButton(tooltip:'Add one',onPressed:busy||((row['quantity'] as int?)??1)>=10?null:()=>quantity(object(row['product']),((row['quantity'] as int?)??1)+1),icon:const Icon(Icons.add)),
              ]),
              if(row['available']!=true)Text(field(row,'notice','Availability has changed.'),style:const TextStyle(color:muted,fontSize:12)),
            ])),
          ]),const SizedBox(height:16),
        ],
        const Rule(),const SizedBox(height:17),
        const Eyebrow('Saved cart subtotal'),const SizedBox(height:8),
        TitleText(amount(result,'subtotal')+' '+field(result,'currency','EGP'),size:34),
        if(message.isNotEmpty)Padding(padding:const EdgeInsets.only(top:12),child:Text(message)),
        const SizedBox(height:16),
        const Text('This review edition saves the same cart. Checkout and delivery confirmation remain in the existing Android app.',
          style:TextStyle(color:muted,fontSize:13)),
      ]);
    }),
  ]);
}
class OrdersEdition extends StatefulWidget{
  final Gateway gateway;
  const OrdersEdition({super.key,required this.gateway});
  @override State<OrdersEdition> createState()=>_OrdersEditionState();
}
class _OrdersEditionState extends State<OrdersEdition>{
  late Future<Data> orders;
  @override void initState(){super.initState();orders=widget.gateway.get('plantupia/commerce/orders/');}
  @override Widget build(BuildContext context)=>EditionPage(label:'Orders',children:[
    const Eyebrow('Recorded purchases'),const SizedBox(height:14),const TitleText('On their way.',size:42),
    const SizedBox(height:24),
    FutureBuilder<Data>(future:orders,builder:(context,s){
      if(s.connectionState!=ConnectionState.done)return const Loading();
      if(s.hasError)return Notice('Orders unavailable',explain(s.error),retry:()=>setState(()=>orders=widget.gateway.get('plantupia/commerce/orders/')));
      final items=rows(s.data?['results']);
      return Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        if(items.isEmpty)const Notice('No orders yet','Your actual saved orders will appear here.'),
        for(final o in items)...[
          const Rule(),ActionLine(rows(o['lines']).isNotEmpty?field(rows(o['lines']).first,'name','Order'):'Order',
            detail:dateOnly(field(o,'created_at'))+' · '+field(o,'status').replaceAll('_',' '),
            onTap:()=>openEdition<void>(context,OrderDetail(gateway:widget.gateway,id:field(o,'id')))),
        ],const Rule(),
      ]);
    }),
  ]);
}
class OrderDetail extends StatefulWidget{
  final Gateway gateway;
  final String id;
  const OrderDetail({super.key,required this.gateway,required this.id});
  @override State<OrderDetail> createState()=>_OrderDetailState();
}
class _OrderDetailState extends State<OrderDetail>{
  late Future<Data> order;
  @override void initState(){super.initState();order=widget.gateway.get('plantupia/commerce/orders/${widget.id}/');}
  @override Widget build(BuildContext context)=>EditionPage(label:'Order record',children:[
    FutureBuilder<Data>(future:order,builder:(context,s){
      if(s.connectionState!=ConnectionState.done)return const Loading();
      if(s.hasError)return Notice('Order unavailable',explain(s.error),retry:()=>setState(()=>order=widget.gateway.get('plantupia/commerce/orders/${widget.id}/')));
      final o=s.data!;
      return Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
        Eyebrow(dateOnly(field(o,'created_at'))),const SizedBox(height:14),
        TitleText(field(o,'status','Order').replaceAll('_',' '),size:39),
        const SizedBox(height:9),SelectableText('Reference '+field(o,'id'),style:const TextStyle(fontSize:11,color:muted)),
        const SizedBox(height:18),Text(field(o,'notice')),
        const SizedBox(height:20),const Eyebrow('Items'),
        for(final p in rows(o['lines']))...[
          const Rule(),Padding(padding:const EdgeInsets.symmetric(vertical:13),child:Text('${p['quantity']} × ${field(p,'name')} · ${field(p,'subtotal')} EGP')),
        ],const Rule(),const SizedBox(height:15),
        Text('Total: '+(field(o,'final_total').isEmpty?'Delivery price pending':field(o,'final_total')+' EGP')),
        Text('Payment: '+field(o,'payment_status','Not reported').replaceAll('_',' ')),
        const SizedBox(height:22),const Eyebrow('Recorded progress'),
        for(final e in rows(o['timeline']))...[
          const Rule(),Padding(padding:const EdgeInsets.symmetric(vertical:12),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
            Eyebrow(dateOnly(field(e,'at'))),Text(field(e,'label',field(e,'kind'))),
          ])),
        ],
      ]);
    })
  ]);
}
