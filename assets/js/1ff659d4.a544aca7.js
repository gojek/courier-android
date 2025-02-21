"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[500],{2560:(e,r,t)=>{t.r(r),t.d(r,{assets:()=>u,contentTitle:()=>i,default:()=>d,frontMatter:()=>o,metadata:()=>l,toc:()=>s});var n=t(8168),a=(t(6540),t(5680));const o={},i="Installation",l={unversionedId:"Installation",id:"Installation",title:"Installation",description:"Supported SDK versions",source:"@site/docs/Installation.md",sourceDirName:".",slug:"/Installation",permalink:"/courier-android/docs/Installation",draft:!1,editUrl:"https://github.com/gojek/courier-android/edit/main/docs/docs/Installation.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Introduction",permalink:"/courier-android/docs/Introduction"},next:{title:"Sample App",permalink:"/courier-android/docs/SampleApp"}},u={},s=[{value:"Supported SDK versions",id:"supported-sdk-versions",level:2},{value:"Download",id:"download",level:2},{value:"Modules",id:"modules",level:2},{value:"Core modules",id:"core-modules",level:3},{value:"Message &amp; Stream Adapters",id:"message--stream-adapters",level:3},{value:"Ping Sender",id:"ping-sender",level:3},{value:"Http Authenticator",id:"http-authenticator",level:3},{value:"MQTT Chuck",id:"mqtt-chuck",level:3}],p={toc:s},c="wrapper";function d(e){let{components:r,...t}=e;return(0,a.yg)(c,(0,n.A)({},p,t,{components:r,mdxType:"MDXLayout"}),(0,a.yg)("h1",{id:"installation"},"Installation"),(0,a.yg)("h2",{id:"supported-sdk-versions"},"Supported SDK versions"),(0,a.yg)("ul",null,(0,a.yg)("li",{parentName:"ul"},"minSdkVersion: 21"),(0,a.yg)("li",{parentName:"ul"},"targetSdkVersion: 34"),(0,a.yg)("li",{parentName:"ul"},"compileSdkVersion: 34")),(0,a.yg)("h2",{id:"download"},"Download"),(0,a.yg)("p",null,(0,a.yg)("a",{parentName:"p",href:"https://search.maven.org/search?q=g:%22com.gojek.courier%22%20AND%20a:%25courier%22"},(0,a.yg)("img",{parentName:"a",src:"https://img.shields.io/maven-central/v/com.gojek.courier/courier.svg?label=Maven%20Central",alt:"Maven Central"}))),(0,a.yg)("p",null,"All artifacts of Courier library are available via Maven Central."),(0,a.yg)("pre",null,(0,a.yg)("code",{parentName:"pre",className:"language-kotlin"},'repositories {\n    mavenCentral()\n}\n\ndependencies {\n    implementation "com.gojek.courier:courier:x.y.z"\n\n    implementation "com.gojek.courier:courier-message-adapter-gson:x.y.z"\n    implementation "com.gojek.courier:courier-stream-adapter-rxjava2:x.y.z"\n}\n')),(0,a.yg)("h2",{id:"modules"},"Modules"),(0,a.yg)("p",null,"Courier Android library provides multiple use case specific modules"),(0,a.yg)("h3",{id:"core-modules"},"Core modules"),(0,a.yg)("p",null,"These modules provide the core functionalities like Connect/Disconnect, Subscribe/Unsubscribe, Send/Receive"),(0,a.yg)("ul",null,(0,a.yg)("li",{parentName:"ul"},"courier"),(0,a.yg)("li",{parentName:"ul"},"mqtt-client")),(0,a.yg)("h3",{id:"message--stream-adapters"},"Message & Stream Adapters"),(0,a.yg)("p",null,"Library provided implementations of message and stream adapters. Read more about them ",(0,a.yg)("a",{parentName:"p",href:"MessageStreamAdapters"},"here"),"."),(0,a.yg)("ul",null,(0,a.yg)("li",{parentName:"ul"},"courier-message-adapter-gson"),(0,a.yg)("li",{parentName:"ul"},"courier-message-adapter-moshi"),(0,a.yg)("li",{parentName:"ul"},"courier-message-adapter-protobuf"),(0,a.yg)("li",{parentName:"ul"},"courier-stream-adapter-rxjava"),(0,a.yg)("li",{parentName:"ul"},"courier-stream-adapter-rxjava2"),(0,a.yg)("li",{parentName:"ul"},"courier-stream-adapter-coroutines")),(0,a.yg)("h3",{id:"ping-sender"},"Ping Sender"),(0,a.yg)("p",null,"Library provided implementations of Mqtt Ping Sender. Read more about them ",(0,a.yg)("a",{parentName:"p",href:"PingSender"},"here"),"."),(0,a.yg)("ul",null,(0,a.yg)("li",{parentName:"ul"},"timer-pingsender"),(0,a.yg)("li",{parentName:"ul"},"workmanager-pingsender"),(0,a.yg)("li",{parentName:"ul"},"workmanager-2.6.0-pingsender"),(0,a.yg)("li",{parentName:"ul"},"alarm-pingsender")),(0,a.yg)("h3",{id:"http-authenticator"},"Http Authenticator"),(0,a.yg)("p",null,"Library provided implementation of Authenticator. Read more about this ",(0,a.yg)("a",{parentName:"p",href:"Authenticator"},"here"),"."),(0,a.yg)("ul",null,(0,a.yg)("li",{parentName:"ul"},"courier-auth-http")),(0,a.yg)("h3",{id:"mqtt-chuck"},"MQTT Chuck"),(0,a.yg)("p",null,"HTTP Chuck inspired tool for debugging all MQTT packets. Read more about this ",(0,a.yg)("a",{parentName:"p",href:"MqttChuck"},"here"),"."),(0,a.yg)("ul",null,(0,a.yg)("li",{parentName:"ul"},"chuck-mqtt")))}d.isMDXComponent=!0},5680:(e,r,t)=>{t.d(r,{xA:()=>p,yg:()=>g});var n=t(6540);function a(e,r,t){return r in e?Object.defineProperty(e,r,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[r]=t,e}function o(e,r){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var n=Object.getOwnPropertySymbols(e);r&&(n=n.filter((function(r){return Object.getOwnPropertyDescriptor(e,r).enumerable}))),t.push.apply(t,n)}return t}function i(e){for(var r=1;r<arguments.length;r++){var t=null!=arguments[r]?arguments[r]:{};r%2?o(Object(t),!0).forEach((function(r){a(e,r,t[r])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):o(Object(t)).forEach((function(r){Object.defineProperty(e,r,Object.getOwnPropertyDescriptor(t,r))}))}return e}function l(e,r){if(null==e)return{};var t,n,a=function(e,r){if(null==e)return{};var t,n,a={},o=Object.keys(e);for(n=0;n<o.length;n++)t=o[n],r.indexOf(t)>=0||(a[t]=e[t]);return a}(e,r);if(Object.getOwnPropertySymbols){var o=Object.getOwnPropertySymbols(e);for(n=0;n<o.length;n++)t=o[n],r.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(a[t]=e[t])}return a}var u=n.createContext({}),s=function(e){var r=n.useContext(u),t=r;return e&&(t="function"==typeof e?e(r):i(i({},r),e)),t},p=function(e){var r=s(e.components);return n.createElement(u.Provider,{value:r},e.children)},c="mdxType",d={inlineCode:"code",wrapper:function(e){var r=e.children;return n.createElement(n.Fragment,{},r)}},m=n.forwardRef((function(e,r){var t=e.components,a=e.mdxType,o=e.originalType,u=e.parentName,p=l(e,["components","mdxType","originalType","parentName"]),c=s(t),m=a,g=c["".concat(u,".").concat(m)]||c[m]||d[m]||o;return t?n.createElement(g,i(i({ref:r},p),{},{components:t})):n.createElement(g,i({ref:r},p))}));function g(e,r){var t=arguments,a=r&&r.mdxType;if("string"==typeof e||a){var o=t.length,i=new Array(o);i[0]=m;var l={};for(var u in r)hasOwnProperty.call(r,u)&&(l[u]=r[u]);l.originalType=e,l[c]="string"==typeof e?e:a,i[1]=l;for(var s=2;s<o;s++)i[s]=t[s];return n.createElement.apply(null,i)}return n.createElement.apply(null,t)}m.displayName="MDXCreateElement"}}]);