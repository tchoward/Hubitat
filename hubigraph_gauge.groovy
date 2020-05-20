/**
 *  Hubigraph Gauge Child App
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

// Hubigraph Gauge Change Log
// V 0.1 Intial release

import groovy.json.JsonOutput

def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v0.22" }

definition(
    name: "Hubigraph Gauge",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Gauge",
    category: "",
    parent: "tchoward:Hubigraphs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
    section ("test"){
       page(name: "mainPage", title: "Main Page", install: true, uninstall: true)
       page(name: "deviceSelectionPage", nextPage: "mainPage")
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

def deviceSelectionPage() {
    def supported_attrs;
        
    dynamicPage(name: "deviceSelectionPage") {
        section() { 
	    
            input "sensor_", "capability.*", title: "Sensor", multiple: false, required: true, submitOnChange: true
        
            if (sensor_){
                attributes_ = sensor_.getSupportedAttributes();
                final_attrs = [];
                attributes_.each{attribute_->
                    //Check to see if there is a valid number
                    if (attribute_.getDataType() == "NUMBER"){
                        currentState_ =  sensor_.currentState(attribute_.getName());
                        //Check to see if there is a valid event for this attribute
                        if (currentState_) {
                            final_attrs += attribute_.getName();
                        }
                    }
                }
                paragraph(sensor_.displayName);
                if (final_attrs == []){
                    paragraph "No supported Numerical Attributes, please select a different Sensor"
                } else {
                    input( type: "enum", name: "attribute_", title: "Attribute for Gauge", required: true, multiple: false, options: final_attrs, defaultValue: "1", submitOnChange: true)
                    if (attribute_){
                        state_ =  sensor_.currentState(attribute_);
                        if (state_) {
                            currentValue = state_.value;
                            paragraph getTitle("Please input the min/max and threshold values")
                            paragraph "Current Value = $currentValue"                          
                            input( type: "number", name: "minValue_", title: "Minimum Value for Gauge", required: true, multiple: false);
                            input( type: "number", name: "maxValue_", title: "Maximum Value for Gauge", required: true, multiple: false);
                        } else {
                             paragraph "<b>No recent valid events</b> Please select a different Attribute"   
                        }
                    }
                }
            }
        }
    }
}

def graphSetupPage(){
    def fontEnum = [["1":"1"], ["2":"2"], ["3":"3"], ["4":"4"], ["5":"5"], ["6":"6"], ["7":"7"], ["8":"8"], ["9":"9"], ["10":"10"], 
                    ["11":"11"], ["12":"12"], ["13":"13"], ["14":"14"], ["15":"15"], ["16":"16"], ["17":"17"], ["18":"18"], ["19":"19"], ["20":"20"]];  
    
    
    
    def colorEnum = [["#800000":"Maroon"], ["#FF0000":"Red"], ["#FFA500":"Orange"], ["#FFFF00":"Yellow"], ["#808000":"Olive"], ["#008000":"Green"],
                    ["#800080":"Purple"], ["#FF00FF":"Fuchsia"], ["#00FF00":"Lime"], ["#008080":"Teal"], ["#00FFFF":"Aqua"], ["#0000FF":"Blue"], ["#000080":"Navy"],
                    ["#000000":"Black"], ["#C0C0C0":"Gray"], ["#C0C0C0":"Silver"], ["#FFFFFF":"White"], ["rgba(255, 255, 255, 0)":"Transparent"]];
    
    def highlightEnum = [[0:"0"], [1:"1"], [2:"2"], [3:"3"]];
    
    dynamicPage(name: "graphSetupPage") {
        section(){
            paragraph getTitle("General Options");
            input( type: "string", name: "gauge_title", title: "Select Gauge Title", multiple: false, required: false, defaultValue: "Gauge Title")
            input( type: "string", name: "gauge_units", title: "Select Gauge Number Units", multiple: false, required: false, defaultValue: "")
            input( type: "string", name: "gauge_number_format", title: "Select Number Formatting (Example #.## = 1.23, #.# = 1.2, # = 1)", multiple: false, required: false, defaultValue: "#")
            paragraph getLine();
            if (!num_highlights){
                   num_highlights = 0;
            }
            input ( type: "enum", name: "num_highlights", title: "Select Number of Highlight Areas on Gauge (Max 3)", multiple: false, defaultValue: "0", options: highlightEnum, submitOnChange: true);
            int num_ = num_highlights.toInteger();
            if (num_highlights && num_ > 0){
                
                log.debug("Looping over $num_highlights");
                for (i=0; i<num_; i+=1){
                    log.debug("i: $i num_: $num_")   
                    input (type: "enum", name: "highlight_color${i}", title: "Select Highlight Color Region #${i}", multiple: false, options:colorEnum);
                    input (type: "number", name: "highlight_start${i}", title: "Select Highlight Start Region Value #${i}");
                }
                input (type: "number", name: "highlight_end", title: "Select Highlight End Region Value #${i-1}");
            }
            
            
            input( type: "enum", name: "gauge_minor_tics", title: "Number Minor Tics", multiple: false, required: false, options:fontEnum, defaultValue: "2")
            input( type: "bool", name: "default_major_ticks", title: "Use Custom Ticks/Labels?", defaultValue: "false", submitOnChange: true)
            if (default_major_ticks == true){
                if (!gauge_major_tics) {
                    gauge_major_tics = "5";
                }
                input( type: "enum", name: "gauge_major_tics", title: "Number Major Tics", multiple: false, required: false, options:fontEnum, defaultValue: "5", submitOnChange: true)
                for (tic = 0; tic<gauge_major_tics.toInteger(); tic++){
                    input( type: "string", name: "tic_title${tic}", title: "Input the Label for Tick #${tic+1}", multiple: false, required: false, defaultValue: "Tic Label ${tic+1}")
                }
                
            }
        }
    }
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

def mainPage() {
    def timeEnum = [["0":"Never"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], 
                    ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]]
    
    dynamicPage(name: "mainPage") {        
        section(){
            if (!state.endpoint) {
                paragraph "API has not been setup. Tap below to enable it."
                href name: "enableAPIPageLink", title: "Enable API", description: "", page: "enableAPIPage"    
            } else {
                href name: "deviceSelectionPage", title: "Select Device/Data", description: "", page: "deviceSelectionPage"
                href name: "graphSetupPage", title: "Configure Graph", description: "", page: "graphSetupPage"
                paragraph getLine();
                paragraph "<i><u><b>LOCAL GRAPH URL</b></u></i>\n${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"
                if (sensor_){
                    paragraph "Selected Device = ${sensor_.displayName} : $attribute_"
                    paragraph "Min: $minValue_"
                    paragraph "Max: $maxValue_"                
                }
               
                
            }//else
        }
        section(){
            input( type: "bool", name: "install_device", title: "Install Hubigraph Tile Device for Dashboard Display", defaultValue: false, submitOnChange: true);
            if (install_device==true){   
                 input( type: "text", name: "device_name", title: "<b>Name for HubiGraph Tile Device</b>", default: "Hubigraph Tile" ); 
            }
        }
        section(){
            if (state.endpoint){
                paragraph getLine();
                input( type: "text", name: "app_name", title: "<b>Rename the Application?</b>", default: "Hubigraph Line Graph", submitOnChange: true ) 
                href url: "${state.localEndpointURL}graph/?access_token=${state.endpointSecret}", title: "Graph -- Please Click <span style='font-weight: bold; font-size: 12px; padding: 10px; border-radius: 3px; box-shadow: 1px 1px 5px -2px black; margin: 5px;'>Done</span> to save settings before viewing the graph"
                href "disableAPIPage", title: "Disable API", description: ""
            }
        }    
        
    }
}

def createHubiGraphTile() {
	log.info "Creating HubiGraph Child Device"
    
    def childDevice = getChildDevice("HUBIGRAPH_${app.id}");     
    log.debug childDevice
   
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
    return sensor_.currentState(attribute_).doubleValue;
}

def getChartOptions(){
    
    if (default_major_ticks == true){
        tic_labels = [];
        for (tic=0; tic<gauge_major_tics.toInteger(); tic++){
            tic_labels += settings["tic_title${tic}"]
        }
    }
    
    highlightString = "";
    log.debug "Looking for ${num_highlights.toInteger()}"
    switch (num_highlights.toInteger()){
           
        case 3: 
        log.debug "Loading 3"
        redColor = highlight_color2;
        redFrom  = highlight_start2;
        redTo    = highlight_end;
        
        yellowColor  = highlight_color1;
        yellowFrom   = highlight_start1;
        yellowTo     = highlight_start2;
        
        greenColor  = highlight_color0;
        greenFrom   = highlight_start0;
        greenTo     = highlight_start1;
            
        break;
            
        case 2:
        log.debug "Loading 2"

        yellowColor  = highlight_color1;
        yellowFrom   = highlight_start1;
        yellowTo     = highlight_end;
        
        greenColor  = highlight_color0;
        greenFrom   = highlight_start0;
        greenTo     = highlight_start1;
        
        break;
        
        case 1: 
        log.debug "Loading 1"

        greenColor  = highlight_color0;
        greenFrom   = highlight_start0;
        greenTo     = highlight_end
        
        break;
    }
    log.debug "$greenColor $greenFrom $greenTo"
    def options = [
        "graphOptions": [
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size: "100%",
            "min": minValue_,
            "max": maxValue_,
            "greenFrom": greenFrom,
            "greenTo": greenTo,
            "greenColor": greenColor,
            "yellowFrom": yellowFrom,
            "yellowTo": yellowTo,
            "yellowColor": yellowColor,
            "redFrom": redFrom,
            "redTo": redTo,
            "redColor": redColor, 
            "backgroundColor": getColorCode(graph_background_color),
            "majorTicks" : default_major_ticks == true ? tic_labels : "",
            "minorTicks" : gauge_minor_tics
        ]
    ]
    
    return options;
}
        
void removeLastChar(str) {
    str.subSequence(0, str.length() - 1)
}

def getGauge() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    def html = """
    <html style="${fullSizeStyle}">
        <head>
            <script src="https://code.jquery.com/jquery-3.5.0.min.js" integrity="sha256-xNzN2a4ltkB44Mc/Jz3pT4iU1cmeR0FkXs4pru/JxaQ=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.0/moment.min.js" integrity="sha256-imB/oMaNA0YvIkDkF5mINRWpuFPEGVCEkHy6rm2lAzA=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/he/1.2.0/he.min.js" integrity="sha256-awnFgdmMV/qmPoT6Fya+g8h/E4m0Z+UFwEHZck/HRcc=" crossorigin="anonymous"></script>
            <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
            <script type="text/javascript">
google.charts.load('current', {'packages':['gauge']});

let options = [];
let subscriptions = {};
let graphData = {};

let websocket;


function getOptions() {
    return jQuery.get("${state.localEndpointURL}getOptions/?access_token=${state.endpointSecret}", (data) => {
        options = data;
        console.log("Got Options");
        console.log(options);
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
        graphData = data;
    });
}

function parseEvent(event) {
    let deviceId = event.deviceId;

    //only accept relevent events
    if(subscriptions.id == deviceId && subscriptions.attribute.includes(event.name)) {
        let value = event.value;

        graphData.value = parseInt(value);

        update();
    }
}

function update() {
    drawChart();   
}

async function onLoad() {
    //first load
    await getOptions();
    await getSubscriptions();
    await getGraphData();

    update();

    //start our update cycle
    //start websocket
    websocket = new WebSocket("ws://" + location.hostname + "/eventsocket");
    websocket.onopen = () => {
        console.log("WebSocket Opened!");
    }
    websocket.onmessage = (event) => {
        parseEvent(JSON.parse(event.data));
    }

    //attach resize listener
    window.addEventListener("resize", () => {
        drawChart();
    });
}

function onBeforeUnload() {
    if(websocket) websocket.close();
}

function drawChart() {
    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn('string', 'Label');
    dataTable.addColumn('number', 'Value');
    dataTable.addRow(["${gauge_title}", graphData.value]);

    var formatter = new google.visualization.NumberFormat(
{suffix: "${gauge_units}", pattern: "${gauge_number_format}"}
    );
    formatter.format(dataTable, 1);

    let chart = new google.visualization.Gauge(document.getElementById("timeline"));
    chart.draw(dataTable, options.graphOptions);
}

google.charts.setOnLoadCallback(onLoad);
window.onBeforeUnload = onBeforeUnload;
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
    return render(contentType: "text/html", data: getGauge());      
}

def getData() {
    def data = buildData();   
    return render(contentType: "text/json", data: JsonOutput.toJson([ "value": data ]));
}

def getOptions() {
    return render(contentType: "text/json", data: JsonOutput.toJson(getChartOptions()));
}

def getSubscriptions() {  
    def subscriptions = [
        "id": sensor_.idAsLong,
        "attribute": attribute_
    ];
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
}
