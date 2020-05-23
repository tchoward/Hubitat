import groovy.json.*;

definition(
    name: "HubiPanel",
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
        
        path("/getDashboardLayout/:dashboardId/") {
            action: [
                GET: "getDashboardLayout"
            ]
        }
        
        path("/getDashboardDevices/:dashboardId/") {
            action: [
                GET: "getDashboardDevices"
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
                input(type: "text", name: "dashboardToken", title: "Dashboard Access Token (Can be obtained from any dashboard, try activating fullscreen and checking the url bar)", required: true)
                paragraph("""<a href="${state.fullEndpoint}?access_token=${state.secret}">${state.fullEndpoint}?access_token=${state.secret}</a>""")
            }
            else paragraph("Click done to enable OAuth and return to the app to get the link.");
		}
        section(){
            input( type: "text", name: "app_name", title: "<b>Rename the Application?</b>", default: "HubiPanel", submitOnChange: true ) 
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
    app.updateLabel(app_name);
}

def updated() {
	log.debug("Updated")
    app.updateLabel(app_name);
}

def uninstalled() {
	log.debug("Uninstalled")
}

def getHubIP() {
    return "http://${location.hubs[0].getDataValue("localIP")}/";
}

def getMain() {
    return render(contentType: "text/html", data: """<!doctype html><html lang="en" class="full"><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1"/><meta name="theme-color" content="#000000"/><meta name="description" content="Dashboard Enhancer for Hubitat"/><title>HubiPanel</title><meta name="hubIp" content="${getHubIP()}"/><meta name="endpoint" content="${state.endpoint}"/><style>.full{margin:0;padding:0;width:100%;height:100%;overflow:hidden}</style></head><body class="full"><noscript>You need to enable JavaScript to run this app.</noscript><div id="root" class="full"></div><script>!function(e){function r(r){for(var n,a,l=r[0],f=r[1],i=r[2],p=0,s=[];p<l.length;p++)a=l[p],Object.prototype.hasOwnProperty.call(o,a)&&o[a]&&s.push(o[a][0]),o[a]=0;for(n in f)Object.prototype.hasOwnProperty.call(f,n)&&(e[n]=f[n]);for(c&&c(r);s.length;)s.shift()();return u.push.apply(u,i||[]),t()}function t(){for(var e,r=0;r<u.length;r++){for(var t=u[r],n=!0,l=1;l<t.length;l++){var f=t[l];0!==o[f]&&(n=!1)}n&&(u.splice(r--,1),e=a(a.s=t[0]))}return e}var n={},o={1:0},u=[];function a(r){if(n[r])return n[r].exports;var t=n[r]={i:r,l:!1,exports:{}};return e[r].call(t.exports,t,t.exports,a),t.l=!0,t.exports}a.m=e,a.c=n,a.d=function(e,r,t){a.o(e,r)||Object.defineProperty(e,r,{enumerable:!0,get:t})},a.r=function(e){"undefined"!=typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(e,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(e,"__esModule",{value:!0})},a.t=function(e,r){if(1&r&&(e=a(e)),8&r)return e;if(4&r&&"object"==typeof e&&e&&e.__esModule)return e;var t=Object.create(null);if(a.r(t),Object.defineProperty(t,"default",{enumerable:!0,value:e}),2&r&&"string"!=typeof e)for(var n in e)a.d(t,n,function(r){return e[r]}.bind(null,n));return t},a.n=function(e){var r=e&&e.__esModule?function(){return e.default}:function(){return e};return a.d(r,"a",r),r},a.o=function(e,r){return Object.prototype.hasOwnProperty.call(e,r)},a.p="./";var l=this.webpackJsonpdashboardenhancer=this.webpackJsonpdashboardenhancer||[],f=l.push.bind(l);l.push=r,l=l.slice();for(var i=0;i<l.length;i++)r(l[i]);var c=f;t()}([])</script><script src="https://cdn.plumpynuggets.com/2.bc047048.chunk.js"></script><script src="https://cdn.plumpynuggets.com/main.72502378.chunk.js"></script></body></html>""");
}

def getDashboards() {
    def resp;
    httpGet("http://127.0.0.1:8080/apps/api/1/menu?access_token=${dashboardToken}") { it ->
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

def getDashboardLayout() {
    def dashboardId = params.dashboardId;
    
    def resp;
    
    httpGet("http://127.0.0.1:8080/apps/api/1/dashboard/${dashboardId}/layout?access_token=${dashboardToken}") { it ->
        resp = it.getData();
    }
    
    return render(contentType: "text/json", data: JsonOutput.toJson(resp), headers: ["Access-Control-Allow-Origin": "*"]);
}

def getDashboardDevices() {
    def dashboardId = params.dashboardId;
    
    def resp;
    
    httpGet("http://127.0.0.1:8080/apps/api/1/dashboard/${dashboardId}/devices2?access_token=${dashboardToken}") { it ->
        resp = it.getData();
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
