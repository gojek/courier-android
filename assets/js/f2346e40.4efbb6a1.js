"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[569],{5680:(e,r,n)=>{n.d(r,{xA:()=>p,yg:()=>m});var t=n(6540);function i(e,r,n){return r in e?Object.defineProperty(e,r,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[r]=n,e}function a(e,r){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var t=Object.getOwnPropertySymbols(e);r&&(t=t.filter((function(r){return Object.getOwnPropertyDescriptor(e,r).enumerable}))),n.push.apply(n,t)}return n}function o(e){for(var r=1;r<arguments.length;r++){var n=null!=arguments[r]?arguments[r]:{};r%2?a(Object(n),!0).forEach((function(r){i(e,r,n[r])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):a(Object(n)).forEach((function(r){Object.defineProperty(e,r,Object.getOwnPropertyDescriptor(n,r))}))}return e}function s(e,r){if(null==e)return{};var n,t,i=function(e,r){if(null==e)return{};var n,t,i={},a=Object.keys(e);for(t=0;t<a.length;t++)n=a[t],r.indexOf(n)>=0||(i[n]=e[n]);return i}(e,r);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(t=0;t<a.length;t++)n=a[t],r.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(i[n]=e[n])}return i}var c=t.createContext({}),u=function(e){var r=t.useContext(c),n=r;return e&&(n="function"==typeof e?e(r):o(o({},r),e)),n},p=function(e){var r=u(e.components);return t.createElement(c.Provider,{value:r},e.children)},l="mdxType",g={inlineCode:"code",wrapper:function(e){var r=e.children;return t.createElement(t.Fragment,{},r)}},d=t.forwardRef((function(e,r){var n=e.components,i=e.mdxType,a=e.originalType,c=e.parentName,p=s(e,["components","mdxType","originalType","parentName"]),l=u(n),d=i,m=l["".concat(c,".").concat(d)]||l[d]||g[d]||a;return n?t.createElement(m,o(o({ref:r},p),{},{components:n})):t.createElement(m,o({ref:r},p))}));function m(e,r){var n=arguments,i=r&&r.mdxType;if("string"==typeof e||i){var a=n.length,o=new Array(a);o[0]=d;var s={};for(var c in r)hasOwnProperty.call(r,c)&&(s[c]=r[c]);s.originalType=e,s[l]="string"==typeof e?e:i,o[1]=s;for(var u=2;u<a;u++)o[u]=n[u];return t.createElement.apply(null,o)}return t.createElement.apply(null,n)}d.displayName="MDXCreateElement"},3406:(e,r,n)=>{n.r(r),n.d(r,{assets:()=>c,contentTitle:()=>o,default:()=>g,frontMatter:()=>a,metadata:()=>s,toc:()=>u});var t=n(8168),i=(n(6540),n(5680));const a={},o="Courier Service Interface",s={unversionedId:"CourierService",id:"CourierService",title:"Courier Service Interface",description:"Courier provides the functionalities like Send, Receive, Subscribe, Unsubscribe through a service interface. This is similar to how we make HTTP calls using Retrofit.",source:"@site/docs/CourierService.md",sourceDirName:".",slug:"/CourierService",permalink:"/courier-android/docs/CourierService",draft:!1,editUrl:"https://github.com/gojek/courier-android/edit/main/docs/docs/CourierService.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"MQTT Client Configuration",permalink:"/courier-android/docs/MqttConfiguration"},next:{title:"Subscribe & Unsubscribe topics",permalink:"/courier-android/docs/SubscribeUnsubscribe"}},c={},u=[{value:"Usage",id:"usage",level:3}],p={toc:u},l="wrapper";function g(e){let{components:r,...n}=e;return(0,i.yg)(l,(0,t.A)({},p,n,{components:r,mdxType:"MDXLayout"}),(0,i.yg)("h1",{id:"courier-service-interface"},"Courier Service Interface"),(0,i.yg)("p",null,"Courier provides the functionalities like Send, Receive, Subscribe, Unsubscribe through a service interface. This is similar to how we make HTTP calls using Retrofit."),(0,i.yg)("h3",{id:"usage"},"Usage"),(0,i.yg)("p",null,"Declare a service interface for various actions like Send, Receive, Subscribe, SubscribeMultiple, Unsubscribe."),(0,i.yg)("pre",null,(0,i.yg)("code",{parentName:"pre",className:"language-kotlin"},'interface MessageService {\n    @Receive(topic = "topic/{id}/receive")\n    fun receive(@Path("id") identifier: String): Observable<Message>\n    \n    @Send(topic = "topic/{id}/send", qos = QoS.TWO)\n    fun send(@Path("id") identifier: String, @Data message: Message)\n    \n    @Subscribe(topic = "topic/{id}/receive", qos = QoS.ONE)\n    fun subscribe(@Path("id") identifier: String): Observable<Message>\n    \n    @SubscribeMultiple\n    fun subscribe(@TopicMap topicMap: Map<String, QoS>): Observable<Message>\n    \n    @Unsubscribe(topics = ["topic/{id}/receive"])\n    fun unsubscribe(@Path("id") identifier: String)\n}\n')),(0,i.yg)("p",null,"Use Courier to create an implementation of service interface."),(0,i.yg)("pre",null,(0,i.yg)("code",{parentName:"pre",className:"language-kotlin"},"val courierConfiguration = Courier.Configuration(\n    client = mqttClient,\n    streamAdapterFactories = listOf(RxJava2StreamAdapterFactory()),\n    messageAdapterFactories = listOf(GsonMessageAdapter.Factory())\n)\n\nval courier = Courier(courierConfiguration)\n\nval messageService = courier.create<MessageService>()\n")),(0,i.yg)("p",null,"Following annotations are supported for service interface."),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"@Send")," : A method annotation used for sending messages over the MQTT connection.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"@Receive")," : A method annotation used for receiving messages over the MQTT connection. Note: The topic needs to be subscribed for receiving messages.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"@Subscribe")," : A method annotation used for subscribing a single topic over the MQTT connection.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"@SubscribeMultiple")," : A method annotation used for subscribing multiple topics over the MQTT connection.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"@Unsubscribe")," : A method annotation used for unsubscribing topics over the MQTT connection.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"@Path")," : A parameter annotation used for specifying a path variable in an MQTT topic.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"@Data")," : A parameter annotation used for specifying the message object while sending a message over the MQTT connection.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"@TopicMap")," : A parameter annotation used for specifying a topic map. It is always used while subscribing multiple topics. "))),(0,i.yg)("p",null,(0,i.yg)("strong",{parentName:"p"},"Note")," : While subscribing topics using ",(0,i.yg)("inlineCode",{parentName:"p"},"@SubscribeMultiple")," along with a stream, make sure that messages received on all topics follow same format or a message adapter is added for handling different format."))}g.isMDXComponent=!0}}]);