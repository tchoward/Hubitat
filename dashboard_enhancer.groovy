import groovy.json.*;

definition(
    name: "Dashboard Enhancer",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Switch dashboards with ease",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2X.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2X.png"
)

preferences {
    page(name: "mainPage")
    page(name: "enableOAuthPage")
    
    mappings {
        path("/main/") {
            action: [
                GET: "getMain"
            ]
        }
        
        path("/main/:/") {
            action: [
                GET: "getMain"
            ]
        }
        
        path("/getDashboards/") {
            action: [
                GET: "getDashboards"
            ]
        }
        
        path("/options/") {
            action: [
                GET: "getOptions",
                POST: "postOptions"
            ]
        }
    }
}

def mainPage() {
	dynamicPage(name: "mainPage", uninstall: true, install: true) {
		section() {
            if(state.endpoint) {
                paragraph("""<a href="${state.fullEndpoint}?access_token=${state.secret}">${state.fullEndpoint}?access_token=${state.secret}</a>""")
            }
            else paragraph("Click done to enable OAuth and return to the app to get the link.");
		}
	}
}


def initOAuth() {
    state.secret = createAccessToken();
    state.endpoint = fullLocalApiServerUrl("");
    state.fullEndpoint = fullLocalApiServerUrl("main/");
}

def installed() {
	log.debug("Installed")
    initOAuth()
}

def updated() {
	log.debug("Updated")
}

def uninstalled() {
	log.debug("Uninstalled")
}

def getMain() {
    return render(contentType: "text/html", data: """<!doctype html><html lang="en" class="full"><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/><meta name="theme-color" content="#000000"/><meta name="description" content="Dashboard Enhancer for Hubitat"/><title>Dashboard Enhancer</title><meta name="endpoint" content="${state.endpoint}"/><meta name="access_token" content="${state.secret}"/><style>.full{margin:0;padding:0;width:100%;height:100%}</style></head><body class="full"><noscript>You need to enable JavaScript to run this app.</noscript><div id="root" class="full"></div><script>!function(e){function r(r){for(var n,l,a=r[0],p=r[1],f=r[2],c=0,s=[];c<a.length;c++)l=a[c],Object.prototype.hasOwnProperty.call(o,l)&&o[l]&&s.push(o[l][0]),o[l]=0;for(n in p)Object.prototype.hasOwnProperty.call(p,n)&&(e[n]=p[n]);for(i&&i(r);s.length;)s.shift()();return u.push.apply(u,f||[]),t()}function t(){for(var e,r=0;r<u.length;r++){for(var t=u[r],n=!0,a=1;a<t.length;a++){var p=t[a];0!==o[p]&&(n=!1)}n&&(u.splice(r--,1),e=l(l.s=t[0]))}return e}var n={},o={1:0},u=[];function l(r){if(n[r])return n[r].exports;var t=n[r]={i:r,l:!1,exports:{}};return e[r].call(t.exports,t,t.exports,l),t.l=!0,t.exports}l.m=e,l.c=n,l.d=function(e,r,t){l.o(e,r)||Object.defineProperty(e,r,{enumerable:!0,get:t})},l.r=function(e){"undefined"!=typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(e,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(e,"__esModule",{value:!0})},l.t=function(e,r){if(1&r&&(e=l(e)),8&r)return e;if(4&r&&"object"==typeof e&&e&&e.__esModule)return e;var t=Object.create(null);if(l.r(t),Object.defineProperty(t,"default",{enumerable:!0,value:e}),2&r&&"string"!=typeof e)for(var n in e)l.d(t,n,function(r){return e[r]}.bind(null,n));return t},l.n=function(e){var r=e&&e.__esModule?function(){return e.default}:function(){return e};return l.d(r,"a",r),r},l.o=function(e,r){return Object.prototype.hasOwnProperty.call(e,r)},l.p="./";var a=this.webpackJsonppanelenhancer=this.webpackJsonppanelenhancer||[],p=a.push.bind(a);a.push=r,a=a.slice();for(var f=0;f<a.length;f++)r(a[f]);var i=p;t()}([])</script><script src="https://cdn.plumpynuggets.com/2.2f3b9aa6.chunk.js"></script><script src="https://cdn.plumpynuggets.com/main.0a8adaad.chunk.js"></script></body></html>""");
}

def getDashboards() {
    def resp;
    httpGet("http://127.0.0.1:8080/apps/api/1/menu?access_token=381a6260-cf15-4091-9b79-cffdfb66fb77") { it ->
        //                                          //head        //script
        def script = it.getData().getAt(0).children()[0].children()[13].text();
        
        def token = "";
        def dashboards = "";
        
        script.eachLine {
            def tokenIndex = it.indexOf("var access_token = \"");
            def dashboardsIndex = it.indexOf("var dashboardsJson = ");
            if(tokenIndex != -1) token = it.substring(tokenIndex + "var access_token = \"".length(), it.lastIndexOf("\""));
            else if(dashboardsIndex != -1) dashboards = it.substring(dashboardsIndex + "var dashboardsJson = ".length(), it.lastIndexOf("]") + 1);
        }
        
        resp = [
            "token": token,
            "dashboards": new JsonSlurper().parseText(dashboards)
        ]
    }
    return render(contentType: "text/json", data: JsonOutput.toJson(resp), headers: ["Access-Control-Allow-Origin": "*"]);
}

def getOptions() {
    log.debug(state.options);
    return render(contentType: "text/json", data: (state.options ? state.options : "{error: true}"), headers: ["Access-Control-Allow-Origin": "*"]); 
}

def postOptions() {
    state.options = request.body;
    return render(headers: ["Access-Control-Allow-Origin": "*"]);
}
