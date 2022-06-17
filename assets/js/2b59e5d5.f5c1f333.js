"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[214],{3905:function(t,e,n){n.d(e,{Zo:function(){return p},kt:function(){return h}});var r=n(7294);function o(t,e,n){return e in t?Object.defineProperty(t,e,{value:n,enumerable:!0,configurable:!0,writable:!0}):t[e]=n,t}function a(t,e){var n=Object.keys(t);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(t);e&&(r=r.filter((function(e){return Object.getOwnPropertyDescriptor(t,e).enumerable}))),n.push.apply(n,r)}return n}function i(t){for(var e=1;e<arguments.length;e++){var n=null!=arguments[e]?arguments[e]:{};e%2?a(Object(n),!0).forEach((function(e){o(t,e,n[e])})):Object.getOwnPropertyDescriptors?Object.defineProperties(t,Object.getOwnPropertyDescriptors(n)):a(Object(n)).forEach((function(e){Object.defineProperty(t,e,Object.getOwnPropertyDescriptor(n,e))}))}return t}function c(t,e){if(null==t)return{};var n,r,o=function(t,e){if(null==t)return{};var n,r,o={},a=Object.keys(t);for(r=0;r<a.length;r++)n=a[r],e.indexOf(n)>=0||(o[n]=t[n]);return o}(t,e);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(t);for(r=0;r<a.length;r++)n=a[r],e.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(t,n)&&(o[n]=t[n])}return o}var u=r.createContext({}),s=function(t){var e=r.useContext(u),n=e;return t&&(n="function"==typeof t?t(e):i(i({},e),t)),n},p=function(t){var e=s(t.components);return r.createElement(u.Provider,{value:e},t.children)},l={inlineCode:"code",wrapper:function(t){var e=t.children;return r.createElement(r.Fragment,{},e)}},d=r.forwardRef((function(t,e){var n=t.components,o=t.mdxType,a=t.originalType,u=t.parentName,p=c(t,["components","mdxType","originalType","parentName"]),d=s(n),h=o,f=d["".concat(u,".").concat(h)]||d[h]||l[h]||a;return n?r.createElement(f,i(i({ref:e},p),{},{components:n})):r.createElement(f,i({ref:e},p))}));function h(t,e){var n=arguments,o=e&&e.mdxType;if("string"==typeof t||o){var a=n.length,i=new Array(a);i[0]=d;var c={};for(var u in e)hasOwnProperty.call(e,u)&&(c[u]=e[u]);c.originalType=t,c.mdxType="string"==typeof t?t:o,i[1]=c;for(var s=2;s<a;s++)i[s]=n[s];return r.createElement.apply(null,i)}return r.createElement.apply(null,n)}d.displayName="MDXCreateElement"},4247:function(t,e,n){n.r(e),n.d(e,{assets:function(){return p},contentTitle:function(){return u},default:function(){return h},frontMatter:function(){return c},metadata:function(){return s},toc:function(){return l}});var r=n(7462),o=n(3366),a=(n(7294),n(3905)),i=["components"],c={},u="Authenticator",s={unversionedId:"Authenticator",id:"Authenticator",title:"Authenticator",description:"When an MQTT client tries to make a connection with an MQTT broker, username and password are sent inside CONNECT packet, which the broker uses to authenticate the client. If username or password is incorrect, broker returns reason code 5.",source:"@site/docs/Authenticator.md",sourceDirName:".",slug:"/Authenticator",permalink:"/courier-android/docs/Authenticator",draft:!1,editUrl:"https://github.com/gojek/courier-android/edit/main/docs/docs/Authenticator.md",tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Experiment Configs",permalink:"/courier-android/docs/ExperimentConfigs"},next:{title:"Non-standard Connection options",permalink:"/courier-android/docs/NonStandardOptions"}},p={},l=[{value:"Http Authenticator",id:"http-authenticator",level:2},{value:"Usage",id:"usage",level:3}],d={toc:l};function h(t){var e=t.components,n=(0,o.Z)(t,i);return(0,a.kt)("wrapper",(0,r.Z)({},d,n,{components:e,mdxType:"MDXLayout"}),(0,a.kt)("h1",{id:"authenticator"},"Authenticator"),(0,a.kt)("p",null,"When an MQTT client tries to make a connection with an MQTT broker, username and password are sent inside CONNECT packet, which the broker uses to authenticate the client. If username or password is incorrect, broker returns reason code ",(0,a.kt)("inlineCode",{parentName:"p"},"5"),"."),(0,a.kt)("p",null,"Courier library uses the Authenticator to refresh the connect options, which contains the username and password, in order to reconnect with the broker successfully."),(0,a.kt)("p",null,"You can pass your own implementation of ",(0,a.kt)("a",{parentName:"p",href:"https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/auth/Authenticator.kt"},"Authenticator")," interface or uses the library provided ",(0,a.kt)("a",{parentName:"p",href:"https://github.com/gojek/courier-android/blob/main/courier-auth-http/src/main/java/com/gojek/courier/authhttp/HttpAuthenticator.kt"},"HttpAuthenticator")),(0,a.kt)("h2",{id:"http-authenticator"},"Http Authenticator"),(0,a.kt)("p",null,"Courier library provides an implementation of Authenticator, which allows you to fetch the latest connect options by making an HTTP call."),(0,a.kt)("h3",{id:"usage"},"Usage"),(0,a.kt)("p",null,"Add this dependency for using Http Authenticator"),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-kotlin"},'dependencies {\n    implementation "com.gojek.courier:courier-auth-http:x.y.z"\n}\n')),(0,a.kt)("p",null,"An instance of HttpAuthenticator can be created using the factory class."),(0,a.kt)("pre",null,(0,a.kt)("code",{parentName:"pre",className:"language-kotlin"},"httpAuthenticator = HttpAuthenticatorFactory.create(\n            retrofit = retrofit,\n            apiUrl = TOKEN_AUTH_API,\n            responseHandler = responseHandler,\n            eventHandler = eventHandler,\n            authRetryPolicy = authRetryPolicy\n        )\n")))}h.isMDXComponent=!0}}]);