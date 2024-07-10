"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[325],{5680:(e,t,n)=>{n.d(t,{xA:()=>p,yg:()=>m});var r=n(6540);function i(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function o(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function a(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?o(Object(n),!0).forEach((function(t){i(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):o(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function l(e,t){if(null==e)return{};var n,r,i=function(e,t){if(null==e)return{};var n,r,i={},o=Object.keys(e);for(r=0;r<o.length;r++)n=o[r],t.indexOf(n)>=0||(i[n]=e[n]);return i}(e,t);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(r=0;r<o.length;r++)n=o[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(i[n]=e[n])}return i}var c=r.createContext({}),s=function(e){var t=r.useContext(c),n=t;return e&&(n="function"==typeof e?e(t):a(a({},t),e)),n},p=function(e){var t=s(e.components);return r.createElement(c.Provider,{value:t},e.children)},u="mdxType",g={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},f=r.forwardRef((function(e,t){var n=e.components,i=e.mdxType,o=e.originalType,c=e.parentName,p=l(e,["components","mdxType","originalType","parentName"]),u=s(n),f=i,m=u["".concat(c,".").concat(f)]||u[f]||g[f]||o;return n?r.createElement(m,a(a({ref:t},p),{},{components:n})):r.createElement(m,a({ref:t},p))}));function m(e,t){var n=arguments,i=t&&t.mdxType;if("string"==typeof e||i){var o=n.length,a=new Array(o);a[0]=f;var l={};for(var c in t)hasOwnProperty.call(t,c)&&(l[c]=t[c]);l.originalType=e,l[u]="string"==typeof e?e:i,a[1]=l;for(var s=2;s<o;s++)a[s]=n[s];return r.createElement.apply(null,a)}return r.createElement.apply(null,n)}f.displayName="MDXCreateElement"},7106:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>c,contentTitle:()=>a,default:()=>g,frontMatter:()=>o,metadata:()=>l,toc:()=>s});var r=n(8168),i=(n(6540),n(5680));const o={},a="MQTT Client Configuration",l={unversionedId:"MqttConfiguration",id:"MqttConfiguration",title:"MQTT Client Configuration",description:"As we have seen earlier, MqttClient requires an instance of MqttV3Configuration. MqttV3Configuration allows you to configure the following properties of MqttClient:",source:"@site/docs/MqttConfiguration.md",sourceDirName:".",slug:"/MqttConfiguration",permalink:"/courier-android/docs/MqttConfiguration",draft:!1,editUrl:"https://github.com/gojek/courier-android/edit/main/docs/docs/MqttConfiguration.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Connection Setup",permalink:"/courier-android/docs/ConnectionSetup"},next:{title:"Courier Service Interface",permalink:"/courier-android/docs/CourierService"}},c={},s=[{value:"Required Configs",id:"required-configs",level:2},{value:"Optional Configs",id:"optional-configs",level:2}],p={toc:s},u="wrapper";function g(e){let{components:t,...n}=e;return(0,i.yg)(u,(0,r.A)({},p,n,{components:t,mdxType:"MDXLayout"}),(0,i.yg)("h1",{id:"mqtt-client-configuration"},"MQTT Client Configuration"),(0,i.yg)("p",null,"As we have seen earlier, ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/client/MqttClient.kt"},"MqttClient")," requires an instance of ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/client/config/v3/MqttV3Configuration.kt"},"MqttV3Configuration"),". MqttV3Configuration allows you to configure the following properties of MqttClient:"),(0,i.yg)("h2",{id:"required-configs"},"Required Configs"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"MqttPingSender")," : It is an implementation of ",(0,i.yg)("a",{parentName:"p",href:"https://github.com/gojek/courier-android/blob/main/pingsender/mqtt-pingsender/src/main/java/com/gojek/mqtt/pingsender/MqttPingSender.kt"},"MqttPingSender")," interface, which defines the logic of sending ping requests over the MQTT connection. Read more ping sender ",(0,i.yg)("a",{parentName:"p",href:"PingSender"},"here"),".")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"Authenticator")," : MqttClient uses Authenticator to refresh the connect options when username or password are incorrect. Read more Authenticator ",(0,i.yg)("a",{parentName:"p",href:"Authenticator"},"here"),"."))),(0,i.yg)("h2",{id:"optional-configs"},"Optional Configs"),(0,i.yg)("ul",null,(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"Retry Policies")," : There are multiple retry policies used in Courier library - connect retry policy, connect timeout policy, subscription policy. You can either use the in-built policies or provide your own custom policies.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"Logger")," : An instance of ILogger can be passed to get the internal logs.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"Event Handler")," : EventHandler allows you to listen to all the library events like connect attempt/success/failure, message send/receive, subscribe/unsubscribe.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"Mqtt Interceptors")," : By passing mqtt interceptors, you can intercept all the MQTT packets sent over the courier connection. This is also used for enabling ",(0,i.yg)("a",{parentName:"p",href:"MqttChuck"},"MQTT Chuck"),".")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"Persistence Options")," : It allows you to configure the offline buffer present inside Paho. This buffer is used for storing all the messages while the client is offline.")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"Experimentation Configs")," : These are the experiment configs used inside Courier library which are explained in detail ",(0,i.yg)("a",{parentName:"p",href:"ExperimentConfigs"},"here"),".")),(0,i.yg)("li",{parentName:"ul"},(0,i.yg)("p",{parentName:"li"},(0,i.yg)("strong",{parentName:"p"},"WakeLock Timeout")," : When positive value of this timeout is passed, a wakelock is acquired while creating the MQTT connection. By default, it is 0. "))))}g.isMDXComponent=!0}}]);