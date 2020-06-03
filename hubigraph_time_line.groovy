/**
 *  Hubigraph Timeline Child App
 *
 *  Copyright 2020, but let's behonest, you'll copy it
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

// Hubigraph Line Graph Changelog
// V 0.1 Intial release
// V 0.2 Fixed startup code which needed all three device types, now one will work
// V 0.22 Update to support tiles
// V 0.3 Loading Update; Removed ALL processing from Hub, uses websocket endpoint
// V 0.4 Uses any device
// V 0.5 Allows ordering of devices
// ****BETA BUILD
// v0.1 Added Hubigraph Tile support with Auto-add Dashboard Tile
// v0.2 Added Custom Device/Attribute Labels
// v0.3 Added waiting screen for initial graph loading & sped up load times
// V 1.0 Released (not Beta) Cleanup and Preview Enabled

 
import groovy.json.JsonOutput

def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v0.22" }

definition(
    name: "Hubigraph Time Line",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Time Line",
    category: "",
    parent: "tchoward:Hubigraphs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
    section ("test"){
       page(name: "mainPage", title: "Main Page", install: true, uninstall: true)
       page(name: "deviceSelectionPage", nextPage: "attributeConfigurationPage")
       page(name: "attributeConfigurationPage", nextPage: "mainPage")
       page(name: "graphSetupPage", nextPage: "mainPage")
       page(name: "enableAPIPage")
       page(name: "disableAPIPage")
    }
   

mappings {
    path("/graph/") {
            action: [
              GET: "getGraph"
            ]
        }
    }
    
    path("/getData/") {
        action: [
            GET: "getData"
        ]
    }
        
    path("/getOptions/") {
        action: [
            GET: "getOptions"
        ]
    }
    
    path("/getSubscriptions/") {
        action: [
            GET: "getSubscriptions"
        ]
    }
}

def getAttributeType(attrib, title){
    
    switch (attrib){
         case "motion":         return ["motion", "Motion (active/inactive)"];
         case "switch":         return ["switch", "Switch (on/off)"];
         case "contact":        return ["contact", "Contact (open/close)"];
         case "acceleration":   return ["acceleration", "Acceleration (active/inactive)"]
         case "audioVolume":
         case "number": return [title, "Number (Choose threshold)"];  
    }
}

def deviceSelectionPage() {                  
    dynamicPage(name: "deviceSelectionPage") {
        section() { 
            input (type: "capability.*", name: "sensors", title: "Choose Sensors", multiple: true, submitOnChange: true)
            if (sensors) {
                def all = (1..sensors.size()).collect{ "" + it };
                validateOrder(all);
                sensors.eachWithIndex {sensor, idx ->
                    id = sensor.id;
                    sensor_attributes = sensor.getSupportedAttributes().collect { it.getName() };      
                    paragraph getTitle(sensor.displayName);
                    
                    input( type: "enum", name: "attributes_${id}", title: "Attributes to graph", required: true, multiple: true, options: sensor_attributes, defaultValue: "1", submitOnChange: false )
                    input( type: "enum", name: "displayOrder_${id}", title: "Order to Display on Timeline", required: true, multiple:false, options: all, defaultValue: idx, submitOnChange: true);
                }
            }
        }
    }

}

def validateOrder(all) {
    def order = [];
    sensors.eachWithIndex {sensor, idx ->
        order << settings["displayOrder_${sensor.id}"];
    }
    
    //if we are initialized and need to check
    if(state.lastOrder && state.lastOrder[0]) {
        def remains = all.findAll { !order.contains(it) }
    
        def dupes = [];
        
        order.each {it ->
            if(order.count(it) > 1) dupes << it;
        }
        
        sensors.eachWithIndex {sensor, idx ->
            if(state.lastOrder[idx] == order[idx] && dupes.contains(settings["displayOrder_${sensor.id}"])) {
                settings["displayOrder_${sensor.id}"] = remains[0];
                app.updateSetting("displayOrder_${sensor.id}", [value: remains[0], type: "enum"]);
                remains.removeAt(0);
            }
        }
    }
    
    //reconstruct order
    order = [];
    sensors.eachWithIndex {sensor, idx ->
        order << settings["displayOrder_${sensor.id}"];
    }
    
    state.lastOrder = order;
}

def attributeConfigurationPage() {
    
    def supportedTypes = [
        "alarm":           ["start": "on",       
                            "end": "off"],
        "contact":         ["start": "open",      
                            "end": "closed"],
        "switch":          ["start": "on",        
                            "end": "off"],
        "motion":          ["start": "active", 
                            "end": "inactive"],
        "mute":            ["start": "muted", 
                            "end": "unmuted"],
        "presence":        ["start":"present",
                            "end":"not present"],
        "holdableButton":  ["start":"true",
                            "end":"false"],
        "carbonMonoxide":  ["start":"detected",
                            "end":"clear"],
        "playing":         ["start":"playing", 
                            "end":"stopped"],
        "door":            ["start": "open",      
                            "end": "closed"],
        "speed":           ["start": "on",        
                            "end": "off"],
        "lock":            ["start": "unlocked",        
                            "end": "locked"],
        "shock":           ["start": "detected",        
                            "end": "clear"],
        "sleepSensor":     ["start": "sleeping",        
                            "end": "not sleeping"],
        "smoke":           ["start":"detected",
                            "end":"clear"],
        "sound":           ["start":"detected",
                            "end":"not detected"],
        "tamper":          ["start":"detected",
                            "end":"clear"],
        "valve":           ["start": "open",      
                            "end": "closed"],
        "camera":          ["start": "on",        
                            "end": "off"],
        "water":           ["start": "wet",        
                            "end": "dry"],
        "windowShade":     ["start": "open",      
                            "end": "closed"],
        "acceleration":    ["start": "inactive", 
                            "end": "active"]        
         ];
                          
    state.count_ = 0;     
    dynamicPage(name: "attributeConfigurationPage") {
        section() {
            paragraph("Configure what counts as a 'start' or 'end' event for each attribute on the timeline. For example, Switches start when they are 'on' and end when they are 'off'.\n\nSome attributes will automatically populate. You can change them if you have a different configuration (chances are you won't).\n\nAdditionally, for devices with numeric values, you can define a range of values that count as 'start' or 'end'. For example, to select all the times a temperature is above 70.5 degrees farenheight, you would set the start to '> 70.5', and the end to '< 70.5'.\n\nSupported comparitors are: '<', '>', '<=', '>=', '==', '!='.\n\nBecause we are dealing with HTML, '<' is abbreviated to &amp;lt; after you save. That is completely normal. It will still work.");
            sensors.each { sensor ->
                def attributes = settings["attributes_${sensor.id}"];
                attributes.each { attribute ->
                    state.count_++;
                    paragraph getTitle("${sensor.displayName}: ${attribute}");
                    input( type: "string", name: "graph_name_override_${sensor.id}_${attribute}", title: "Override Device Name -- use %deviceName% for DEVICE and %attributeName% for ATTRIBUTE", defaultValue: "%deviceName%: %attributeName%");
                    input( type: "text", name: "attribute_${sensor.id}_${attribute}_start", title: "Start event value", defaultValue: supportedTypes[attribute] ? supportedTypes[attribute].start : null, required: true);
                    input( type: "text", name: "attribute_${sensor.id}_${attribute}_end", title: "End event value", defaultValue: supportedTypes[attribute] ? supportedTypes[attribute].end : null, required: true);
                }
            }
        }
    }

}


def graphSetupPage(){
    def fontEnum = [["1":"1"], ["2":"2"], ["3":"3"], ["4":"4"], ["5":"5"], ["6":"6"], ["7":"7"], ["8":"8"], ["9":"9"], ["10":"10"], 
                    ["11":"11"], ["12":"12"], ["13":"13"], ["14":"14"], ["15":"15"], ["16":"16"], ["17":"17"], ["18":"18"], ["19":"19"], ["20":"20"]];  
    
    def colorEnum = ["Maroon", "Red", "Orange", "Yellow", "Olive", "Green", "Purple", "Fuchsia", "Lime", "Teal", "Aqua", "Blue", "Navy", "Black", "Gray", "Silver", "White", "Transparent"];
    
    dynamicPage(name: "graphSetupPage") {
        section(){
            paragraph getTitle("General Options");
            input( type: "enum", name: "graph_update_rate", title: "Select graph update rate", multiple: false, required: false, options: [["-1":"Never"], ["0":"Real Time"], ["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]], defaultValue: "0")
            input( type: "enum", name: "graph_timespan", title: "Select Timespan to Graph", multiple: false, required: false, options: [["60000":"1 Minute"], ["3600000":"1 Hour"], ["43200000":"12 Hours"], ["86400000":"1 Day"], ["259200000":"3 Days"], ["604800000":"1 Week"]], defaultValue: "43200000")     
            input( type: "enum", name: "graph_background_color", title: "Background Color", defaultValue: "White", options: colorEnum);
            
            //Size
            paragraph getTitle("Graph Size");
            input( type: "bool", name: "graph_static_size", title: "Set size of Graph? (False = Fill Window)", defaultValue: false, submitOnChange: true);
            if (graph_static_size==true){
                input( type: "number", name: "graph_h_size", title: "Horizontal dimension of the graph", defaultValue: "800", range: "100..3000");
                input( type: "number", name: "graph_v_size", title: "Vertical dimension of the graph", defaultValue: "600", range: "100..3000");
            }
            
            //Axis
            paragraph getTitle("Axes");
            input( type: "enum", name: "graph_axis_font", title: "Graph Axis Font Size", defaultValue: "9", options: fontEnum); 
            input( type: "enum", name: "graph_axis_color", title: "Graph Axis Text Color", defaultValue: "Black", options: colorEnum);
        }
    }
}

def loadPreview(){
  if (!state.count_) state.count_ = 5;
  def html = ""
    
    html+= """
<iframe id="preview" style="width: 100%; height: 100%; background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAEq2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIKICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIKICAgIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIgogICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgZXhpZjpQaXhlbFhEaW1lbnNpb249IjIiCiAgIGV4aWY6UGl4ZWxZRGltZW5zaW9uPSIyIgogICBleGlmOkNvbG9yU3BhY2U9IjEiCiAgIHRpZmY6SW1hZ2VXaWR0aD0iMiIKICAgdGlmZjpJbWFnZUxlbmd0aD0iMiIKICAgdGlmZjpSZXNvbHV0aW9uVW5pdD0iMiIKICAgdGlmZjpYUmVzb2x1dGlvbj0iNzIuMCIKICAgdGlmZjpZUmVzb2x1dGlvbj0iNzIuMCIKICAgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIKICAgcGhvdG9zaG9wOklDQ1Byb2ZpbGU9InNSR0IgSUVDNjE5NjYtMi4xIgogICB4bXA6TW9kaWZ5RGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIKICAgeG1wOk1ldGFkYXRhRGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCI+CiAgIDx4bXBNTTpIaXN0b3J5PgogICAgPHJkZjpTZXE+CiAgICAgPHJkZjpsaQogICAgICBzdEV2dDphY3Rpb249InByb2R1Y2VkIgogICAgICBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZmZpbml0eSBQaG90byAxLjguMyIKICAgICAgc3RFdnQ6d2hlbj0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIvPgogICAgPC9yZGY6U2VxPgogICA8L3htcE1NOkhpc3Rvcnk+CiAgPC9yZGY6RGVzY3JpcHRpb24+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8+IC4TuwAAAYRpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZE7SwNBFEaPiRrxQQQFLSyCRiuVGEG0sUjwBWqRRPDVbDYvIYnLboIEW8E2oCDa+Cr0F2grWAuCoghiZWGtaKOy3k2EBIkzzL2Hb+ZeZr4BWyippoxqD6TSGT0w4XPNLyy6HM/UYqONfroU1dBmguMh/h0fd1RZ+abP6vX/uYqjIRI1VKiqEx5VNT0jPCk8vZbRLN4WblUTSkT4VLhXlwsK31p6uMgvFseL/GWxHgr4wdYs7IqXcbiM1YSeEpaX404ls+rvfayXNEbTc0HJnbI6MAgwgQ8XU4zhZ4gBRiQO0YdXHBoQ7yrXewr1s6xKrSpRI4fOCnESZOgVNSvdo5JjokdlJslZ/v/11YgNeovdG31Q82Sab93g2ILvvGl+Hprm9xHYH+EiXapfPYDhd9HzJc29D84NOLssaeEdON+E9gdN0ZWCZJdli8Xg9QSaFqDlGuqXip797nN8D6F1+aor2N2DHjnvXP4Bhcln9Ef7rWMAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAXSURBVAiZY7hw4cL///8Z////f/HiRQBMEQrfQiLDpgAAAABJRU5ErkJggg=='); background-size: 25px; background-repeat: repeat; image-rendering: pixelated;" src="${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"></iframe>
<script>
function resize() {
    const box = jQuery('#formApp')[0].getBoundingClientRect();
    const h = 35 * ${state.count_.floatValue()} + 30;
    jQuery('#preview').css('height', h);
}

resize();

jQuery(window).on('resize', () => {
    resize();
});
</script>
"""
}

def disableAPIPage() {
    dynamicPage(name: "disableAPIPage") {
        section() {
            if (state.endpoint) {
                try {
                   revokeAccessToken();
                }
                catch (e) {
                    log.debug "Unable to revoke access token: $e"
                }
                state.endpoint = null
            }
            paragraph "It has been done. Your token has been REVOKED. Tap Done to continue."
        }
    }
}

def enableAPIPage() {
    dynamicPage(name: "enableAPIPage", title: "") {
        section() {
            if(!state.endpoint) initializeAppEndpoint();
            if (!state.endpoint){
                paragraph "Endpoint creation failed"
            } else {
                paragraph "It has been done. Your token has been CREATED. Tap Done to continue."
            }
        }
    }
}

def getTimeString(string_){
    switch (string_.toInteger()){
        case -1: return "Never";
        case 0: return "Real Time"; 
        case 1000:return "1 Second"; 
        case 5000:return "5 Seconds"; 
        case 60000:return "1 Minute"; 
        case 300000:return "5 Minutes"; 
        case 600000:return "10 Minutes"; 
        case 1800000:return "Half Hour";
        case 3600000:return "1 Hour";
    }
    log.debug("NOT FOUND");
}

def getTimsSpanString(string_){
    switch (string_.toInteger()){
        case -1: return "Never";
        case 0: return "Real Time"; 
        case 1000:return "1 Second"; 
        case 5000:return "5 Seconds"; 
        case 60000:return "1 Minute"; 
        case 3600000:return "1 Hour";
        case 43200000: return "12 Hours";
        case 86400000: return "1 Day";
        case 259200000: return "3 Days";
        case 604800000: return "1 Week";
    }
    log.debug("NOT FOUND");
}


def mainPage() {
    def timeEnum = [["0":"Never"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], 
                    ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]]
    
    dynamicPage(name: "mainPage") {        
        section(){
            if (!state.endpoint) {
                paragraph getTitle("API has not been setup. Tap below to enable it.");
                href name: "enableAPIPageLink", title: "Enable API", description: "", page: "enableAPIPage"    
            } else {
                paragraph getTitle("Graph Options");
                href name: "deviceSelectionPage", title: "Select Device/Data", description: "", page: "deviceSelectionPage"
                href name: "graphSetupPage", title: "Configure Graph", description: "", page: "graphSetupPage"
                paragraph getTitle("Local URL for Graph");
                paragraph "<i><u><b>LOCAL GRAPH URL</b></u></i>\n${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"
          
                                   
                if (graph_timespan){
                    paragraph getTitle("Preview");
                    paragraph loadPreview();
                   
                } //graph_timespan
            }//else
        }
        section(){
            paragraph getTitle("Hubigraph Tile Installation");
            input( type: "bool", name: "install_device", title: "Install Hubigraph Tile Device for Dashboard Display", defaultValue: false, submitOnChange: true);
            if (install_device==true){   
                 input( type: "text", name: "device_name", title: "<b>Name for HubiGraph Tile Device</b>", default: "Hubigraph Tile" ); 
            }
        }
        section(){
            if (state.endpoint){
                paragraph getTitle("Hubigraph Application Name");
                input( type: "text", name: "app_name", title: "<b>Rename the Application?</b>", default: "Hubigraph Line Graph", submitOnChange: true ) 
                paragraph getTitle("Disable Oauth Authorization");
                href "disableAPIPage", title: "Disable API", description: ""
            }
        }    
        section(){
           paragraph getTitle("Hubigraph Debugging");
           input( type: "bool", name: "debug", title: "Enable Debug Logging?", defaultValue: false); 
        }    
    }
}

def createHubiGraphTile() {
	log.info "Creating HubiGraph Child Device"
    
    def childDevice = getChildDevice("HUBIGRAPH_${app.id}");     
       
    if (!childDevice) {
        if (!device_name) device_name="Dummy Device";
        log.debug("Creating Device $device_name");
    	childDevice = addChildDevice("tchoward", "Hubigraph Tile Device", "HUBIGRAPH_${app.id}", null,[completedSetup: true, label: device_name]) 
        log.info "Created HTTP Switch [${childDevice}]"
        
        //Send the html automatically
        childDevice.setGraph("${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
        log.info "Sent setGraph: ${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"
	}
    else {
    	
        childDevice.label = device_name;
        log.info "Label Updated to [${device_name}]"
        
        //Send the html automatically
        childDevice.setGraph("${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
        log.info "Sent setGraph: ${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"
	}

}


def getLine(){	  
	def html = "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    html
}

def getTableRow(col1, col2, col3, col4){
     def html = "<tr><td width='40%'>$col1</td><td width='30%'>$col2</td><td width='20%'>$col3</td><td width='10%'>$col4</td></tr>"  
     html
}

def getTableRow3(col1, col2, col3){
     def html = "<tr><td width='30%'>$col1</td><td width='30%'>$col2</td><td width='40%'>$col3</td></tr>"  
     html
}

def getTitle(myText=""){
    def html = "<div class='row-full' style='background-color:#1A77C9;color:white;font-weight: bold'>"
    html += "${myText}</div>"
    html
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    updated();
}

def uninstalled() {
    if (state.endpoint) {
        try {
            logDebug "Revoking API access token"
            revokeAccessToken()
        }
        catch (e) {
            log.warn "Unable to revoke API access token: $e"
        }
    }
    removeChildDevices(getChildDevices());
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}


def updated() {
    app.updateLabel(app_name);
    state.dataName = attribute;
    
     if (install_device == true){
        createHubiGraphTile();
    }
}

def buildData() {
    def resp = [:]
    def now = new Date();
    def then = new Date();
    
    use (groovy.time.TimeCategory) {
           then -= Integer.parseInt(graph_timespan).milliseconds;
    }
    
    if(sensors) {
      sensors.each {sensor ->
          def attributes = settings["attributes_${sensor.id}"];
          resp[sensor.id] = [:];
          attributes.each { attribute ->
              temp = sensor.statesSince(attribute, then, [max: 50000]).collect{[ date: it.date, value: it.value ]}
              //temp = sensor?.eventsBetween(then, now, [max: 500000])?.findAll{it.name == attribute}?.collect{[date: it.date, value: it.value]}
              temp = temp.sort{ it.date };
              temp = temp.collect{ [date: it.date.getTime(), value: it.value] }
              
              resp[sensor.id][attribute] = temp;
          }
      }
      
   }
   return resp
}

def getChartOptions(){
    def options = [
        "graphTimespan": Integer.parseInt(graph_timespan),
        "graphUpdateRate": Integer.parseInt(graph_update_rate),
        "graphOptions": [
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size: "100%",
            "timeline": [
                "rowLabelStyle": ["fontSize": graph_axis_font, "color": getColorCode(graph_axis_color)],
                "barLabelStyle": ["fontSize": graph_axis_font]
            ],
            "backgroundColor": getColorCode(graph_background_color)
        ]
    ]
    
    return options;
}
        
void removeLastChar(str) {
    str.subSequence(0, str.length() - 1)
}

def getTimeLine() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    def html = """
    <html style="${fullSizeStyle}">
        <head>
            <script src="https://code.jquery.com/jquery-3.5.0.min.js" integrity="sha256-xNzN2a4ltkB44Mc/Jz3pT4iU1cmeR0FkXs4pru/JxaQ=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.0/moment.min.js" integrity="sha256-imB/oMaNA0YvIkDkF5mINRWpuFPEGVCEkHy6rm2lAzA=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/he/1.2.0/he.min.js" integrity="sha256-awnFgdmMV/qmPoT6Fya+g8h/E4m0Z+UFwEHZck/HRcc=" crossorigin="anonymous"></script>
            <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
            <script type="text/javascript">
google.charts.load('current', {'packages':['timeline']});
google.charts.setOnLoadCallback(onLoad);
            
let options = [];
let subscriptions = {};
let graphData = {};
let unparsedData = {};

let websocket;

class Loader {
    constructor() {
        this.elem = jQuery(jQuery(document.body).prepend(`
            <div class="loaderContainer">
                <div class="dotsContainer">
                    <div class="dot"></div>
                    <div class="dot"></div>
                    <div class="dot"></div>
                </div>
                <div class="text"></div>
            </div>
        `).children()[0]);
    }

    setText(text) {
        this.elem.find('.text').text(text);
    }

    remove() {
        this.elem.remove();
    }
}

function getOptions() {
    return jQuery.get("${state.localEndpointURL}getOptions/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Options");
        console.log(data);                        
        options = data;
    });
}

function getSubscriptions() {
    return jQuery.get("${state.localEndpointURL}getSubscriptions/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Subscriptions");
        console.log(data);
        subscriptions = data;
        
    });
}

function getGraphData() {
    return jQuery.get("${state.localEndpointURL}getData/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Graph Data");
        console.log(data);
        unparsedData = data;
        
    });
}

function parseEvent(event) {
    const now = new Date().getTime();

    let deviceId = event.deviceId;
    let attribute = event.name;

    //only accept relevent events
    if(Object.keys(subscriptions.sensors).includes("" + deviceId) && Object.keys(subscriptions.definitions[deviceId]).includes(attribute)) {
        const pastEvents = graphData[deviceId][attribute];
        if(pastEvents.length > 0) {
            const start_event = subscriptions.definitions[deviceId][attribute].start;
            const end_event = subscriptions.definitions[deviceId][attribute].end;
            const is_start = evalTest(start_event, event.value);
            const is_end = evalTest(end_event, event.value);

            if(is_end && !pastEvents[pastEvents.length - 1].end) pastEvents[pastEvents.length - 1].end = now;
            else if(is_start && pastEvents[pastEvents.length - 1].end) pastEvents.push({ start: now });
        } else {
            pastEvents.push({ start: now });
        }

        //update if we are realtime
        if(options.graphUpdateRate === 0) update();
    }
}

function evalTest(evalStrPre, value) {
    const evalStr = he.decode(evalStrPre);
    const operatorMatch = evalStr.replace(' ', '').match(/(<=)|(>=)|<|>|(==)|(!=)/g);

    if(operatorMatch) {
        const operator = operatorMatch[0];
        const rest = parseFloat(evalStr.replace(operator, ''));
        const floatValue = parseFloat(value);

        switch (operator) {
            case '<':
                return floatValue < rest;
            case '>':
                return floatValue > rest;
            case '==':
                return floatValue == rest;
            case '!=':
                return floatValue != rest;
            case '<=':
                return floatValue <= rest;
            case '>=':
                return floatValue >= rest;
            default:
                
        }
    } else {
        return value == evalStr;
    }   
}

async function update(callback) {
    let now = new Date().getTime();
    let min = now;
    min -= options.graphTimespan;

    //parse data

    //boot old data
    Object.entries(graphData).forEach(([id, allEvents]) => {
        Object.entries(allEvents).forEach(([attribute, events]) => {
        //shift left points and mark for deletion if applicable
            let newArr = events.map(it => {
                let ret = { ...it }

                if(it.end && it.end < min) {
                    ret = {};
                }
                else if(it.start && it.start < min) ret.start = min;

                return ret;
            });

            //delete non-existant nodes
            newArr = newArr.filter(it => it.start || it.end);

            graphData[id][attribute] = newArr;
        });
    });

    drawChart(now, min, callback);
}

async function onLoad() {
    //append our css
    jQuery(document.head).append(`
        <style>
            .loaderContainer {
                position: fixed;
                z-index: 100;

                width: 100%;
                height: 100%;

                background-color: white;
                
                display: flex;
                flex-flow: column nowrap;
                justify-content: center;
                align-items: middle;
            }

            .dotsContainer {
                height: 60px;
                padding-bottom: 10px;

                display: flex;
                flex-flow: row nowrap;
                justify-content: center;
                align-items: flex-end;
            }

            @keyframes bounce {
                0% {
                    transform: translateY(0);
                }

                50% {
                    transform: translateY(-50px);
                }

                100% {
                    transform: translateY(0);
                }
            }

            .dot {
                box-sizing: border-box;

                margin: 0 25px;

                width: 10px;
                height: 10px;

                border: solid 5px black;
                border-radius: 5px;

                animation-name: bounce;
                animation-duration: 1s;
                animation-iteration-count: infinite;
            }

            .dot:nth-child(1) {
                animation-delay: 0ms;
            }

            .dot:nth-child(2) {
                animation-delay: 333ms;
            }

            .dot:nth-child(3) {
                animation-delay: 666ms;
            }

            .text {
                font-family: Arial;
                font-weight: 200;
                font-size: 2rem;
                text-align: center;
            }
        </style>
    `);

    let loader = new Loader();

    //first load
    loader.setText('Getting options (1/4)');
    await getOptions();
    loader.setText('Getting device data (2/4)');
    await getSubscriptions();
    loader.setText('Getting events (3/4)');
    await getGraphData();

    loader.setText('Drawing chart (4/4)');

    let now = new Date().getTime();
    let min = now;
    min -= options.graphTimespan;

    //parse data
    Object.entries(unparsedData).forEach(([id, allEvents]) => {
        graphData[id] = {};
        Object.entries(allEvents).forEach(([attribute, events]) => {
            graphData[id][attribute] = [];
            const start_event = subscriptions.definitions[id][attribute].start;
            const end_event = subscriptions.definitions[id][attribute].end;

            const  thisOut = graphData[id][attribute];
            if(events.length > 0) {
                //if our first event is an end event, start at 1
                thisOut.push(evalTest(start_event, events[0].value) ? { start: events[0].date } : { end: events[0].date });
                for(let i = 1; i < events.length; i++) {
                    const is_start = evalTest(start_event, events[i].value);
                    const is_end = evalTest(end_event, events[i].value);
                    
                    //always add the first event
                    if(is_end && !thisOut[thisOut.length - 1].end) thisOut[thisOut.length - 1].end = events[i].date;
                    else if(is_start && thisOut[thisOut.length - 1].end) thisOut.push({ start: events[i].date });
                }
            }
            //if it's already on, add an event
            else if(evalTest(start_event, subscriptions.sensors[id].currentStates.find((it) => it.name == attribute).value)) {
                thisOut.push({ start: min });
            }
        });
    });

    console.log("Parsed Data");
    console.log(Object.assign({}, graphData));

    //update data
    update(() => {
        //destroy loader when we are done with it
        loader.remove();
    });

    //start our update cycle
    if(options.graphUpdateRate !== -1) {
        //start websocket
        websocket = new WebSocket("ws://" + location.hostname + "/eventsocket");
        websocket.onopen = () => {
            console.log("WebSocket Opened!");
        }
        websocket.onmessage = (event) => {
            parseEvent(JSON.parse(event.data));
        }

        if(options.graphUpdateRate !== 0) {
            setInterval(() => {
                update();
            }, options.graphUpdateRate);
        }
    }

    //attach resize listener
    window.addEventListener("resize", () => {
        let now = new Date().getTime();
        let min = now;
        min -= options.graphTimespan;

        drawChart(now, min);
    });
}

function drawChart(now, min, callback) {
    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn({ type: 'string', id: 'Device' });
    dataTable.addColumn({ type: 'date', id: 'Start' });
    dataTable.addColumn({ type: 'date', id: 'End' });

    subscriptions.order.forEach(id => {
        Object.entries(graphData[id]).forEach(([attribute, events]) => {
            let newArr = [...events];

            //add endpoints for orphans
            newArr = newArr.map((it) => {
                if(!it.start) {
                    return {...it, start: min }
                }
                else if(!it.end) return {...it, end: now}
                return it;
            });

            //add endpoint buffers
            if(newArr.length == 0) {
                newArr.push({ start: min, end: min });
                newArr.push({ start: now, end: now });
            } else {
                if(newArr[0].start != min) newArr.push({ start: min, end: min });
                if(newArr[newArr.length - 1].end != now) newArr.push({ start: now, end: now });
            }

            let name = subscriptions.sensors[id].displayName;

            dataTable.addRows(newArr.map((parsed) => [subscriptions.labels[id][attribute].replace('%deviceName%', name).replace('%attributeName%', attribute), moment(parsed.start).toDate(), moment(parsed.end).toDate()]));
        });
    });

    let chart = new google.visualization.Timeline(document.getElementById("timeline"));

    //if we have a callback
    if(callback) google.visualization.events.addListener(chart, 'ready', callback);

    chart.draw(dataTable, options.graphOptions);
}
        </script>
      </head>
      <body style="${fullSizeStyle}">
          <div id="timeline" style="${fullSizeStyle}" align="center"></div>
      </body>
    </html>
    """
    
return html;
}

// Create a formatted date object string for Google Charts Timeline
def getDateString(date) {
    def dateObj = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", date.toString())
    //def dateObj = date
    def year = dateObj.getYear() + 1900
    def dateString = "new Date(${year}, ${dateObj.getMonth()}, ${dateObj.getDate()}, ${dateObj.getHours()}, ${dateObj.getMinutes()}, ${dateObj.getSeconds()})"
    dateString
}

// Events come in Date format
def getDateStringEvent(date) {
    def dateObj = date
    def yyyy = dateObj.getYear() + 1900
    def MM = String.format("%02d", dateObj.getMonth()+1);
    def dd = String.format("%02d", dateObj.getDate());
    def HH = String.format("%02d", dateObj.getHours());
    def mm = String.format("%02d", dateObj.getMinutes());
    def ss = String.format("%02d", dateObj.getSeconds());
    def dateString = /$yyyy-$MM-$dd $HH:$mm:$ss.000/;
    dateString
}
    
def initializeAppEndpoint() {
    if (!state.endpoint) {
        try {
            def accessToken = createAccessToken()
            if (accessToken) {
                state.endpoint = getApiServerUrl()
                state.localEndpointURL = fullLocalApiServerUrl("")  
                state.remoteEndpointURL = fullApiServerUrl("")
                state.endpointSecret = accessToken
            }
        }
        catch(e) {
            log.debug("Error: $e");
            state.endpoint = null
        }
    }
    return state.endpoint
}

def getColorCode(code){
    switch (code){
        case "Maroon":  ret = "#800000"; break;
        case "Red":	    ret = "#FF0000"; break;
        case "Orange":	ret = "#FFA500"; break;	
        case "Yellow":	ret = "#FFFF00"; break;	
        case "Olive":	ret = "#808000"; break;	
        case "Green":	ret = "#008000"; break;	
        case "Purple":	ret = "#800080"; break;	
        case "Fuchsia":	ret = "#FF00FF"; break;	
        case "Lime":	ret = "#00FF00"; break;	
        case "Teal":	ret = "#008080"; break;	
        case "Aqua":	ret = "#00FFFF"; break;	
        case "Blue":	ret = "#0000FF"; break;	
        case "Navy":	ret = "#000080"; break;	
        case "Black":	ret = "#000000"; break;	
        case "Gray":	ret = "#808080"; break;	
        case "Silver":	ret = "#C0C0C0"; break;	
        case "White":	ret = "#FFFFFF"; break;
        case "Transparent": ret = "transparent"; break;
    }
}

//oauth endpoints
def getGraph() {
    return render(contentType: "text/html", data: getTimeLine());      
}

def getData() {
    def timeline = buildData();
    
    /*def formatEvents = [:];
    
    timeline.each{device->
        formatEvents[device.id_] = [];
        device.events_.each{event->
            formatEvents[device.id_] << ["start": event.start, "end": event.end];
        }
    }*/
        
    return render(contentType: "text/json", data: JsonOutput.toJson(timeline));
}

def getOptions() {
    return render(contentType: "text/json", data: JsonOutput.toJson(getChartOptions()));
}

def getSubscriptions() {
    def definitions = [:];
    def labels = [:];
    sensors.each { sensor ->
        definitions[sensor.id] = [:];
        labels[sensor.id] = [:];
        def attributes = settings["attributes_${sensor.id}"];
        attributes.each { attribute ->
            definitions[sensor.id][attribute] = ["start": settings["attribute_${sensor.id}_${attribute}_start"], "end": settings["attribute_${sensor.id}_${attribute}_end"]];
            labels[sensor.id][attribute] = settings["graph_name_override_${sensor.id}_${attribute}"];
        }
    }
                                                                                    
    def sensors_fmt = [:];
    sensors.each { it ->
        sensors_fmt[it.id] = [ "id": it.id, "displayName": it.displayName, "currentStates": it.currentStates ];
    }
    
    def order = [sensors.size()];
    sensors.each {sensor ->
        order[Integer.parseInt(settings["displayOrder_${sensor.id}"]) - 1] = sensor.idAsLong;
    }
    
    def subscriptions = [
        "sensors": sensors_fmt,
        "definitions": definitions,
        "labels": labels,
        "order": order
    ];
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
}
