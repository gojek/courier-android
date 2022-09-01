"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[185],{3905:(e,t,n)=>{n.d(t,{Zo:()=>s,kt:()=>f});var r=n(7294);function o(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function c(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function i(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?c(Object(n),!0).forEach((function(t){o(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):c(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function a(e,t){if(null==e)return{};var n,r,o=function(e,t){if(null==e)return{};var n,r,o={},c=Object.keys(e);for(r=0;r<c.length;r++)n=c[r],t.indexOf(n)>=0||(o[n]=e[n]);return o}(e,t);if(Object.getOwnPropertySymbols){var c=Object.getOwnPropertySymbols(e);for(r=0;r<c.length;r++)n=c[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(o[n]=e[n])}return o}var u=r.createContext({}),l=function(e){var t=r.useContext(u),n=t;return e&&(n="function"==typeof e?e(t):i(i({},t),e)),n},s=function(e){var t=l(e.components);return r.createElement(u.Provider,{value:t},e.children)},p={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},d=r.forwardRef((function(e,t){var n=e.components,o=e.mdxType,c=e.originalType,u=e.parentName,s=a(e,["components","mdxType","originalType","parentName"]),d=l(n),f=o,m=d["".concat(u,".").concat(f)]||d[f]||p[f]||c;return n?r.createElement(m,i(i({ref:t},s),{},{components:n})):r.createElement(m,i({ref:t},s))}));function f(e,t){var n=arguments,o=t&&t.mdxType;if("string"==typeof e||o){var c=n.length,i=new Array(c);i[0]=d;var a={};for(var u in t)hasOwnProperty.call(t,u)&&(a[u]=t[u]);a.originalType=e,a.mdxType="string"==typeof e?e:o,i[1]=a;for(var l=2;l<c;l++)i[l]=n[l];return r.createElement.apply(null,i)}return r.createElement.apply(null,n)}d.displayName="MDXCreateElement"},3540:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>u,contentTitle:()=>i,default:()=>p,frontMatter:()=>c,metadata:()=>a,toc:()=>l});var r=n(7462),o=(n(7294),n(3905));const c={},i="MQTT Chuck",a={unversionedId:"MqttChuck",id:"MqttChuck",title:"MQTT Chuck",description:"MQTT Chuck is used for inspecting all the outgoing or incoming MQTT packets for an underlying MQTT connection. MQTT Chuck is similar to HTTP Chuck, used for inspecting the HTTP calls on an android application.",source:"@site/docs/MqttChuck.md",sourceDirName:".",slug:"/MqttChuck",permalink:"/courier-android/docs/MqttChuck",draft:!1,editUrl:"https://github.com/gojek/courier-android/edit/main/docs/docs/MqttChuck.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Subscription Store",permalink:"/courier-android/docs/SubscriptionStore"},next:{title:"Quality of Service",permalink:"/courier-android/docs/QoS"}},u={},l=[{value:"Usage",id:"usage",level:2}],s={toc:l};function p(e){let{components:t,...c}=e;return(0,o.kt)("wrapper",(0,r.Z)({},s,c,{components:t,mdxType:"MDXLayout"}),(0,o.kt)("h1",{id:"mqtt-chuck"},"MQTT Chuck"),(0,o.kt)("p",null,"MQTT Chuck is used for inspecting all the outgoing or incoming MQTT packets for an underlying MQTT connection. MQTT Chuck is similar to ",(0,o.kt)("a",{parentName:"p",href:"https://github.com/jgilfelt/chuck"},"HTTP Chuck"),", used for inspecting the HTTP calls on an android application."),(0,o.kt)("p",null,"MQTT Chuck uses an interceptor to intercept all the MQTT packets, persisting them and providing a UI for accessing all the MQTT packets sent or received over the MQTT connection. It also provides multiple other features like search, share, and clear data."),(0,o.kt)("p",null,(0,o.kt)("img",{alt:"image chuck",src:n(6405).Z,width:"1340",height:"898"})),(0,o.kt)("h2",{id:"usage"},"Usage"),(0,o.kt)("p",null,"Add this dependency for using MQTT chuck"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-kotlin"},'dependencies {\n    implementation "com.gojek.courier:chuck-mqtt:x.y.z"\n}\n')),(0,o.kt)("p",null,"To enable MQTT chuck for your courier connection, just pass the ",(0,o.kt)("inlineCode",{parentName:"p"},"MqttChuckInterceptor")," inside ",(0,o.kt)("a",{parentName:"p",href:"MqttConfiguration"},"MqttConfiguration")),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-kotlin"},"mqttConfiguration = MqttV3Configuration(\n    mqttInterceptorList = listOf(MqttChuckInterceptor(context, mqttChuckConfig))\n)\n")))}p.isMDXComponent=!0},6405:(e,t,n)=>{n.d(t,{Z:()=>r});const r=n.p+"assets/images/mqtt-chuck-b6064afd3aa888427557125f19fdaf4c.png"}}]);