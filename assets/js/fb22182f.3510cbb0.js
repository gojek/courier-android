"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[611],{3905:function(e,n,t){t.d(n,{Zo:function(){return u},kt:function(){return f}});var r=t(7294);function o(e,n,t){return n in e?Object.defineProperty(e,n,{value:t,enumerable:!0,configurable:!0,writable:!0}):e[n]=t,e}function i(e,n){var t=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);n&&(r=r.filter((function(n){return Object.getOwnPropertyDescriptor(e,n).enumerable}))),t.push.apply(t,r)}return t}function a(e){for(var n=1;n<arguments.length;n++){var t=null!=arguments[n]?arguments[n]:{};n%2?i(Object(t),!0).forEach((function(n){o(e,n,t[n])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(t)):i(Object(t)).forEach((function(n){Object.defineProperty(e,n,Object.getOwnPropertyDescriptor(t,n))}))}return e}function c(e,n){if(null==e)return{};var t,r,o=function(e,n){if(null==e)return{};var t,r,o={},i=Object.keys(e);for(r=0;r<i.length;r++)t=i[r],n.indexOf(t)>=0||(o[t]=e[t]);return o}(e,n);if(Object.getOwnPropertySymbols){var i=Object.getOwnPropertySymbols(e);for(r=0;r<i.length;r++)t=i[r],n.indexOf(t)>=0||Object.prototype.propertyIsEnumerable.call(e,t)&&(o[t]=e[t])}return o}var s=r.createContext({}),p=function(e){var n=r.useContext(s),t=n;return e&&(t="function"==typeof e?e(n):a(a({},n),e)),t},u=function(e){var n=p(e.components);return r.createElement(s.Provider,{value:n},e.children)},l={inlineCode:"code",wrapper:function(e){var n=e.children;return r.createElement(r.Fragment,{},n)}},d=r.forwardRef((function(e,n){var t=e.components,o=e.mdxType,i=e.originalType,s=e.parentName,u=c(e,["components","mdxType","originalType","parentName"]),d=p(t),f=o,m=d["".concat(s,".").concat(f)]||d[f]||l[f]||i;return t?r.createElement(m,a(a({ref:n},u),{},{components:t})):r.createElement(m,a({ref:n},u))}));function f(e,n){var t=arguments,o=n&&n.mdxType;if("string"==typeof e||o){var i=t.length,a=new Array(i);a[0]=d;var c={};for(var s in n)hasOwnProperty.call(n,s)&&(c[s]=n[s]);c.originalType=e,c.mdxType="string"==typeof e?e:o,a[1]=c;for(var p=2;p<i;p++)a[p]=t[p];return r.createElement.apply(null,a)}return r.createElement.apply(null,t)}d.displayName="MDXCreateElement"},7578:function(e,n,t){t.r(n),t.d(n,{assets:function(){return u},contentTitle:function(){return s},default:function(){return f},frontMatter:function(){return c},metadata:function(){return p},toc:function(){return l}});var r=t(7462),o=t(3366),i=(t(7294),t(3905)),a=["components"],c={},s="Non-standard Connection options",p={unversionedId:"NonStandardOptions",id:"NonStandardOptions",title:"Non-standard Connection options",description:"UserProperties in MqttConnectionOptions",source:"@site/docs/NonStandardOptions.md",sourceDirName:".",slug:"/NonStandardOptions",permalink:"/courier-android/docs/NonStandardOptions",draft:!1,editUrl:"https://github.com/gojek/courier-android/edit/main/docs/docs/NonStandardOptions.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Authenticator",permalink:"/courier-android/docs/Authenticator"},next:{title:"Adaptive KeepAlive",permalink:"/courier-android/docs/AdaptiveKeepAlive"}},u={},l=[{value:"UserProperties in MqttConnectionOptions",id:"userproperties-in-mqttconnectionoptions",level:3}],d={toc:l};function f(e){var n=e.components,t=(0,o.Z)(e,a);return(0,i.kt)("wrapper",(0,r.Z)({},d,t,{components:n,mdxType:"MDXLayout"}),(0,i.kt)("h1",{id:"non-standard-connection-options"},"Non-standard Connection options"),(0,i.kt)("h3",{id:"userproperties-in-mqttconnectionoptions"},"UserProperties in MqttConnectionOptions"),(0,i.kt)("p",null,"This option allows you to send user-properties in CONNECT packet for MQTT v3.1.1."),(0,i.kt)("pre",null,(0,i.kt)("code",{parentName:"pre",className:"language-kotlin"},'val connectOptions = MqttConnectOptions(\n    serverUris = listOf(ServerUri(SERVER_URI, SERVER_PORT)),\n    clientId = clientId,\n    ...\n    userPropertiesMap = mapOf(\n                "key1" to "value1",\n                "key2" to "value2"\n    )\n)\n\nmqttClient.connect(connectOptions)\n')),(0,i.kt)("p",null,"\u26a0\ufe0f **\nThis is a non-standard option. As far as the MQTT specification is concerned, user-properties support is added in MQTT v5. So to support this in MQTT v3.1.1, broker needs to have support for this as well."))}f.isMDXComponent=!0}}]);