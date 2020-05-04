import groovy.json.JsonOutput

/**
 *  Hubigraph Line Graph Child App
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
// v0.1 Initial release
// v0.2 My son added webpage efficiencies, reduced load on hubitat by 75%.
// V 0.3 Loading Update; Removed ALL processing from Hub, uses websocket endpoint
// Credit to Alden Howard for optimizing the code.
 
def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v1.0" }

definition(
    name: "Hubigraph Line Graph",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Line Graph",
    category: "",
    parent: "tchoward:Hubigraphs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
    section ("test"){
       page(name: "mainPage", title: "Main Page", install: true, uninstall: true)
       page(name: "deviceSelectionPage")
       page(name: "graphSetupPage")
       page(name: "enableAPIPage")
       page(name: "disableAPIPage")
}
   

    mappings {
        path("/graph/") {
            action: [
                GET: "getGraph"
            ]
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
}

def deviceSelectionPage() {
    def supported_attrs;
        
    dynamicPage(name: "deviceSelectionPage") {
    section() { 
	    
        input "sensor", "capability.sensor", title: "Sensors", multiple: false, required: false, submitOnChange: true
        
        if (sensor){
            sensor_events = sensor.events([max:250]).name;
            supported_attrs = sensor_events.unique(false);
           
            if (supported_attrs){
                //attrs = []
                //supported_attrs.each{atribute->
                //    attrs << /${supported_attrs[i]}/;
                //}   
            }else{
               supported_attrs  = ["NO SUPPORTED EVENTS"];   
            }
        input( type: "enum", name: "attribute", title: "Attribute to graph?", required: false, multiple: false, options: supported_attrs, defaultValue: "1", submitOnChange: true )
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
            input( type: "bool", name: "graph_smoothing", title: "Smooth Graph Points", defaultValue: true);
            input( type: "enum", name: "graph_type", title: "Graph Type", defaultValue: "Line Graph", options: ["Line Graph", "Area Graph"] )
            
            //Title
           
            paragraph getTitle("Title");
            input( type: "bool", name: "graph_show_title", title: "Show Title on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_title==true) {
                input( type: "text", name: "graph_title", title: "Input Graph Title", default: "Graph Title");
                input( type: "enum", name: "graph_title_font", title: "Graph Title Font", defaultValue: "9", options: fontEnum); 
                input( type: "enum", name: "graph_title_color", title: "Graph Title Color", defaultValue: "Black", options: colorEnum); 
            }
            
            //Size
            paragraph getTitle("Graph Size");
            input( type: "bool", name: "graph_static_size", title: "Set size of Graph? (False = Fill Window)", defaultValue: false, submitOnChange: true);
            if (graph_static_size==true){
                input( type: "number", name: "graph_h_size", title: "Horizontal dimension of the graph", defaultValue: "800", range: "100..3000");
                input( type: "number", name: "graph_v_size", title: "Vertical dimension of the graph", defaultValue: "600", range: "100..3000");
            }
            
            //Axis
            paragraph getTitle("Graph Axis");
            input( type: "enum", name: "graph_haxis_font", title: "Horizonal Axis Font Size", defaultValue: "9", options: fontEnum); 
            input( type: "enum", name: "graph_hh_color", title: "Horizonal Header Color", defaultValue: "Black", options: colorEnum);
            input( type: "enum", name: "graph_ha_color", title: "Horizonal Axis Color", defaultValue: "Gray", options: colorEnum);
            input( type: "enum", name: "graph_vaxis_font", title: "Vertical Font Size", defaultValue: "9", options: fontEnum); 
            input( type: "enum", name: "graph_vh_color", title: "Vertical Header Color", defaultValue: "Black", options: colorEnum);
            input( type: "enum", name: "graph_va_color", title: "Vertical Axis Color", defaultValue: "Gray", options: colorEnum);
            
            //Legend
            paragraph getTitle("Legend");
            input( type: "bool", name: "graph_show_legend", title: "Show Legend on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_legend==true){
                input( type: "enum", name: "graph_legend_font", title: "Legend Font Size", defaultValue: "9", options: fontEnum); 
                input( type: "enum", name: "graph_legend_color", title: "Legends Color", defaultValue: "Black", options: colorEnum); 
            }
            
            //Line
            paragraph getTitle("Line");
            input( type: "enum", name: "graph_line_color", title: "Line Color", defaultValue: "Blue", options: colorEnum); 
            input( type: "enum", name: "graph_line_thickness", title: "Line Thickness", defaultValue: "2", options: fontEnum); 
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
            paragraph "It has been done. Your token has been CREATED. Tap Done to continue."
        }
    }
}

def getTimeString(string_){
    //log.debug("Looking for $string_")
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
                if (sensor){
                    paragraph "<i><u><b>DEVICE INFORMATION</b></u></i>\n<b>Device</b>: \t\t$sensor\n<b>Attribute</b>: \t${attribute}"     
                }
                
                if (graph_update_rate){
                    def timeString = getTimsSpanString(graph_timespan);
                    def rateString = getTimeString(graph_update_rate);
                    
                    def paragraph_ =  "<table>"
                    
                    
                    paragraph_ += "${getTableRow("<b><u>GRAPH SELECTIONS</b></u>", "<b><u>VALUE</b></u>", "<b><u>SIZE</b></u>", "<b><u>COLOR</b></u>")}"
                    if (graph_show_title==true) {
                        paragraph_ += /${getTableRow("Title", graph_title, graph_title_font, graph_title_color)}/
                    } else {
                        paragraph_ += /${getTableRow("Title", "NOT SHOWN", "", "")}/
                    }
                    
                    paragraph_ += /${getTableRow("Horizontal Header", "", graph_haxis_font, graph_hh_color)}/
                    paragraph_ += /${getTableRow("Vertical Header", "", graph_vaxis_font, graph_vh_color)}/
                    paragraph_ += /${getTableRow("Horizontal Axis", "", "", graph_ha_color)}/
                    paragraph_ += /${getTableRow("Vertical Axis", "", "", graph_va_color)}/
                    
                    if (graph_show_legend==true){
                        paragraph_ += /${getTableRow("Legend", "", graph_legend_font, graph_legend_color)}/
                    } else {
                        paragraph_ += /${getTableRow("Legend", "NOT SHOWN", "", "")}/
                    }
                    
                    
                    paragraph_ += /${getTableRow("Line", "", graph_line_thickness, graph_line_color)}/  
                    paragraph_ += /${getTableRow("Background", "", "", graph_background_color)}/
                    if (graph_static_size==true){
                        paragraph_ += /${getTableRow("Graph Size", "$graph_h_size X $graph_v_size", "","")}/
                    } else {
                        paragraph_ += /${getTableRow("Graph Size", "DYNAMIC", "","")}/
                    }
                    paragraph_ += /${getTableRow("Smoothing", graph_smoothing, "", "")}/
                    paragraph_ += /${getTableRow("Timespan", timeString,"","")}/
                    paragraph_ += /${getTableRow("Graph Update Rate", rateString,"","")}/
                    paragraph_ += /${getTableRow("Graph Type", graph_type,"","")}/
                    
                    
                    paragraph_ += "</table>"
                    
                    paragraph paragraph_
                    
                }
                
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

def getLine(){	  
	def html = "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    html
}

def getTableRow(col1, col2, col3, col4){
     def html = "<tr><td width='40%'>$col1</td><td width='30%'>$col2</td><td width='20%'>$col3</td><td width='10%'>$col4</td></tr>"  
     html
}

def getTitle(myText=""){
    def html = "<div class='row-full' style='background-color:#1A77C9;color:white;font-weight: bold'>"
    html += "${myText}</div>"
    html
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
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
}

def updated() {
    app.updateLabel(app_name);
}

def initialize() {
   updated();
}

private buildData() {
    def resp = []
    def today = new Date();
    def then = new Date();
    
    use (groovy.time.TimeCategory) {
           then -= Integer.parseInt(graph_timespan).milliseconds;
    }
    
    log.debug("Initializing:: Device = $sensor  Attribute = $attribute from $then to $today");
    
    if(sensor) {
      sensor.each {
         log.debug("Checking:: $it.displayName $it $attribute");
         resp << it?.eventsBetween(then, today, [max: 50000])?.findAll{"$it.name" == attribute}?.collect{[name: it.name, date: it.date, value: it.value]}
      }
    }
    
   //Sort Data
    resp = resp.sort{ it.date };
    resp = resp.flatten();
    resp = resp.reverse();
    //convert the date to ms
    resp = resp.collect{ [name: it.name, date: it.date.getTime(), value: it.value] }
    
    return resp;
}

def getChartOptions(){
    def options = [
        "graphTimespan": Integer.parseInt(graph_timespan),
        "graphUpdateRate": Integer.parseInt(graph_update_rate),
        "graphOptions": [
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size: "100%",
            "chartArea": [ "width": graph_static_size ? graph_h_size : "80%", "height": graph_static_size ? graph_v_size: "80%"],
            "lineWidth": graph_line_thickness,
            "colors": [getColorCode(graph_line_color)],
            "hAxis": ["textStyle": ["fontSize": graph_haxis_font, "color": getColorCode(graph_hh_color)], "gridLines": ["color": getColorCode(graph_ha_color)]],
            "vAxis": ["textStyle": ["fontSize": graph_vaxis_font, "color": getColorCode(graph_vh_color)], "gridLines": ["color": getColorCode(graph_va_color)]],
            "legend": !graph_show_legend ? ["position": "none"] : ["position": "bottom",  "textStyle": ["fontSize": graph_legend_font, "color": getColorCode(graph_title_color)]],
            "backgroundColor": getColorCode(graph_background_color),
            "curveType": !graph_smoothing ? "" : "function",
            "title": !graph_show_title ? "" : graph_title,
            "titleTextStyle": !graph_show_title ? "" : ["fontSize": graph_title_font, "color": getColorCode(graph_title_color)]
        ]
    ]
    
    return options;
}
        
def getDrawType(){
    switch (graph_type){
        case "Line Graph": return "google.visualization.LineChart";
        case "Area Graph": return "google.visualization.AreaChart";
    }
}

void removeLastChar(str) {
    str.subSequence(0, str.length() - 1)
    str
}

def getLineGraph() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    def html = """
    <html style="${fullSizeStyle}">
    <head>
      <script src="https://code.jquery.com/jquery-3.5.0.min.js" integrity="sha256-xNzN2a4ltkB44Mc/Jz3pT4iU1cmeR0FkXs4pru/JxaQ=" crossorigin="anonymous"></script>
      <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.0/moment.min.js" integrity="sha256-imB/oMaNA0YvIkDkF5mINRWpuFPEGVCEkHy6rm2lAzA=" crossorigin="anonymous"></script>
      <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
      <script type="text/javascript">
google.charts.load('current', {'packages':['corechart']});
google.charts.setOnLoadCallback(onLoad);

let options = [];
let subscriptions = {};
let graphData = [];

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
        graphData = graphData.concat(data);
    });
}

function parseEvent(event) {
    const now = new Date().getTime();

    let deviceId = event.deviceId;

    //only accept relevent events
    let deviceIndex = -1;
    //subscriptions.forEach((val) => {
        //if we are subscribed to this certain type
        //if(val) {
            let index = subscriptions.sensor.idAsLong === deviceId && event.name === subscriptions.attributes[0] ? 0 : -1;
            if(index != -1) deviceIndex = index;
        //}
   // });

    if(deviceIndex != -1) {
        let value = event.value;
        let attribute = subscriptions.attributes[0];

        graphData.push({ name: attribute, date: now, value: event.value })

        //update if we are realtime
        if(options.graphUpdateRate === 0) update();
    }
}

function update() {
    //boot old data
    let min = new Date().getTime();
    min -= options.graphTimespan;

    graphData = graphData.filter((it) => {
        return it.date > min;
    });

    drawChart();   
}

async function onLoad() {
    //first load
    await getOptions();
    await getSubscriptions();
    await getGraphData();

    update();

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
        drawChart();
    });
}

function drawChart() {
    let chartFormatData = [[{label: 'Date', type: 'date'}, {label: "${attribute}", type: 'number'}], ...graphData.map((it) => [moment(it.date).toDate(), it.value])];
    let dataTable = google.visualization.arrayToDataTable(chartFormatData);
    let chart = new ${drawType}(document.getElementById("timeline"));

    chart.draw(dataTable, options.graphOptions);
}
        </script>
      </head>
      <body style="${fullSizeStyle}">
        <div id="timeline" style="${fullSizeStyle}"></div>
      </body>
       
    </html>
    """
    
    return html;
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
    render(contentType: "text/html", data: getLineGraph());      
}

def getDataMetrics() {
    def data;
    def then = new Date().getTime();
    data = getData();
    def now = new Date().getTime();
    log.debug("Command Time (ms): ${now - then}");
    return data;
}

def getData() {
    def timeline = buildData();
    return render(contentType: "text/json", data: JsonOutput.toJson(timeline));
}

def getOptions() {
    render(contentType: "text/json", data: JsonOutput.toJson(getChartOptions()));
}

def getSubscriptions() {
    def obj = [
        sensor: sensor,
        attributes: [ attribute ]
    ]
    
    def subscriptions = obj;
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
}
