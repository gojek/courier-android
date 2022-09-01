"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[101],{3905:(e,t,n)=>{n.d(t,{Zo:()=>s,kt:()=>d});var r=n(7294);function o(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function i(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function a(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?i(Object(n),!0).forEach((function(t){o(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):i(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function c(e,t){if(null==e)return{};var n,r,o=function(e,t){if(null==e)return{};var n,r,o={},i=Object.keys(e);for(r=0;r<i.length;r++)n=i[r],t.indexOf(n)>=0||(o[n]=e[n]);return o}(e,t);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(r=0;r<i.length;r++)n=i[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(o[n]=e[n])}return o}var l=r.createContext({}),p=function(e){var t=r.useContext(l),n=t;return e&&(n="function"==typeof e?e(t):a(a({},t),e)),n},s=function(e){var t=p(e.components);return r.createElement(l.Provider,{value:t},e.children)},u={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},m=r.forwardRef((function(e,t){var n=e.components,o=e.mdxType,i=e.originalType,l=e.parentName,s=c(e,["components","mdxType","originalType","parentName"]),m=p(n),d=o,k=m["".concat(l,".").concat(d)]||m[d]||u[d]||i;return n?r.createElement(k,a(a({ref:t},s),{},{components:n})):r.createElement(k,a({ref:t},s))}));function d(e,t){var n=arguments,o=t&&t.mdxType;if("string"==typeof e||o){var i=n.length,a=new Array(i);a[0]=m;var c={};for(var l in t)hasOwnProperty.call(t,l)&&(c[l]=t[l]);c.originalType=e,c.mdxType="string"==typeof e?e:o,a[1]=c;for(var p=2;p<i;p++)a[p]=n[p];return r.createElement.apply(null,a)}return r.createElement.apply(null,n)}m.displayName="MDXCreateElement"},8698:(e,t,n)=>{n.r(t),n.d(t,{assets:()=>l,contentTitle:()=>a,default:()=>u,frontMatter:()=>i,metadata:()=>c,toc:()=>p});var r=n(7462),o=(n(7294),n(3905));const i={},a="Connection Setup",c={unversionedId:"ConnectionSetup",id:"ConnectionSetup",title:"Connection Setup",description:"MqttClient",source:"@site/docs/ConnectionSetup.md",sourceDirName:".",slug:"/ConnectionSetup",permalink:"/courier-android/docs/ConnectionSetup",draft:!1,editUrl:"https://github.com/gojek/courier-android/edit/main/docs/docs/ConnectionSetup.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Sample App",permalink:"/courier-android/docs/SampleApp"},next:{title:"MQTT Client Configuration",permalink:"/courier-android/docs/MqttConfiguration"}},l={},p=[{value:"MqttClient",id:"mqttclient",level:2},{value:"Connect using MqttClient",id:"connect-using-mqttclient",level:3},{value:"Disconnect using MqttClient",id:"disconnect-using-mqttclient",level:3},{value:"MqttConnectOptions",id:"mqttconnectoptions",level:3}],s={toc:p};function u(e){let{components:t,...n}=e;return(0,o.kt)("wrapper",(0,r.Z)({},s,n,{components:t,mdxType:"MDXLayout"}),(0,o.kt)("h1",{id:"connection-setup"},"Connection Setup"),(0,o.kt)("h2",{id:"mqttclient"},"MqttClient"),(0,o.kt)("p",null,"An instance of ",(0,o.kt)("a",{parentName:"p",href:"https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/client/MqttClient.kt"},"MqttClient")," needs to be created in order to establish a Courier connection."),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-kotlin"},"val mqttClient = MqttClientFactory.create(\n    context = context,\n    mqttConfiguration = mqttConfiguration\n)\n")),(0,o.kt)("h3",{id:"connect-using-mqttclient"},"Connect using MqttClient"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-kotlin"},"val connectOptions = MqttConnectOptions(\n    serverUris = listOf(ServerUri(SERVER_URI, SERVER_PORT)),\n    clientId = clientId,\n    username = username,\n    keepAlive = KeepAlive(\n        timeSeconds = keepAliveSeconds\n    ),\n    isCleanSession = cleanSessionFlag,\n    password = password\n)\n\nmqttClient.connect(connectOptions)\n")),(0,o.kt)("h3",{id:"disconnect-using-mqttclient"},"Disconnect using MqttClient"),(0,o.kt)("pre",null,(0,o.kt)("code",{parentName:"pre",className:"language-kotlin"},"mqttClient.disconnect()\n")),(0,o.kt)("h3",{id:"mqttconnectoptions"},"MqttConnectOptions"),(0,o.kt)("p",null,(0,o.kt)("a",{parentName:"p",href:"https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/model/MqttConnectOptions.kt"},"MqttConnectOptions")," represents the properties of the underlying MQTT connection in Courier."),(0,o.kt)("ul",null,(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("p",{parentName:"li"},(0,o.kt)("strong",{parentName:"p"},"Server URIs")," : List of ServerUri representing the host and port of an MQTT broker.")),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("p",{parentName:"li"},(0,o.kt)("strong",{parentName:"p"},"Client Id")," : Unique ID of the MQTT client.")),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("p",{parentName:"li"},(0,o.kt)("strong",{parentName:"p"},"Username")," : Username of the MQTT client.")),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("p",{parentName:"li"},(0,o.kt)("strong",{parentName:"p"},"Password")," : Password of the MQTT client.")),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("p",{parentName:"li"},(0,o.kt)("strong",{parentName:"p"},"KeepAlive Interval")," : Interval at which keep alive packets are sent for the MQTT connection.")),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("p",{parentName:"li"},(0,o.kt)("strong",{parentName:"p"},"Clean Session Flag")," : When clean session is false, a persistent connection is created. Otherwise, non-persistent connection is created and all persisted information is cleared from both client and broker.")),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("p",{parentName:"li"},(0,o.kt)("strong",{parentName:"p"},"Read Timeout")," : Read timeout of the SSL/TCP socket created for the MQTT connection.")),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("p",{parentName:"li"},(0,o.kt)("strong",{parentName:"p"},"MQTT protocol version")," : It can be either VERSION_3_1 or VERSION_3_1_1.")),(0,o.kt)("li",{parentName:"ul"},(0,o.kt)("p",{parentName:"li"},(0,o.kt)("strong",{parentName:"p"},"User properties")," : Custom user properties appended to the CONNECT packet."))))}u.isMDXComponent=!0}}]);