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
                input(type: "text", name: "dashboardToken", title: "Base Dashboard App Access Token - Can be obtained from any dashboard, try activating fullscreen and checking the url bar\nEx: (http://255.255.255.255/apps/api/1/menu?access_token=<span style='font-weight: bold;'>xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</span>))", required: true)
                input(type: "number", name: "dashboardAppId", title: "Base Dashboard App Id - Can be obtained from any dashboard, try activating fullscreen and checking the url bar\nEx: (http://255.255.255.255/apps/api/<span style='font-weight: bold;'>1</span>/menu?access_token=xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx))", required: true)
                paragraph("""<a href="${state.fullEndpoint}?access_token=${state.secret}&dashboardAppId=${dashboardAppId}&endpoint=${state.endpoint}&hubIp=${getHubIP()}">${state.fullEndpoint}?access_token=${state.secret}&dashboardAppId=${dashboardAppId}&endpoint=${state.endpoint}&hubIp=${getHubIP()}</a>""")
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
    def resp = "";
    httpGet([ "uri": "https://cdn.plumpynuggets.com", "path": "/index.html", contentType: "text/plain" ]) { it ->
        resp = it.data.text
    }
    
    resp = resp.replaceAll("/static/js/", "https://cdn.plumpynuggets.com/static/js/");
    
    return render(contentType: "text/html", data: resp, headers: ["Access-Control-Allow-Origin": "*"]);
}

def getDashboards() {
    def resp;
    httpGet("http://127.0.0.1:8080/apps/api/${dashboardAppId}/menu?access_token=${dashboardToken}") { it ->
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
    
    httpGet("http://127.0.0.1:8080/apps/api/${dashboardAppId}/dashboard/${dashboardId}/layout?access_token=${dashboardToken}") { it ->
        resp = it.getData();
    }
    
    return render(contentType: "text/json", data: JsonOutput.toJson(resp), headers: ["Access-Control-Allow-Origin": "*"]);
}

def getDashboardDevices() {
    def dashboardId = params.dashboardId;
    
    def resp;
    
    httpGet("http://127.0.0.1:8080/apps/api/${dashboardAppId}/dashboard/${dashboardId}/devices2?access_token=${dashboardToken}") { it ->
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
