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

// Hubigraph Gauge Change Log
// V 0.1 Intial release
// V 1.0 Released (not Beta) Cleanup and Preview Enabled
// V 1.5 UI Redesign

import groovy.json.JsonOutput
import java.text.DecimalFormat;

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
       page(name: "mainPage", install: true, uninstall: true)
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

def call(Closure code) {
    code.setResolveStrategy(Closure.DELEGATE_ONLY);
    code.setDelegate(this);
    code.call();
}

def extractNumber( String input ) {
  val = input.findAll( /-?\d+\.\d*|-?\d*\.\d+|-?\d+/ )*.toDouble()
  val[0]
}

def deviceSelectionPage() {
    def supported_attrs;
        
    dynamicPage(name: "deviceSelectionPage") {
        
        parent.hubiForm_section(this,"Device Selection", 1){	    
            
            input "sensor_", "capability.*", title: "Sensor", multiple: false, required: true, submitOnChange: true
            
            container = [];
            if (sensor_) {
                attributes_ = sensor_.getSupportedAttributes();
                final_attrs = [];
                attributes_.each{attribute_->
                    final_attrs += attribute_.getName();
                }
                 
                container << parent.hubiForm_sub_section(this,"""${sensor_.getDisplayName()}""")
                    
                if (final_attrs == []){
                        container<< parent.hubiForm_text(this, "<b>No supported Numerical Attributes</b><br><small>Please select a different Sensor</small>");
                        parent.hubiForm_container(this, container, 1); 
                } else {
                        input( type: "enum", name: "attribute_", title: "Attribute for Gauge", required: true, multiple: false, options: final_attrs, defaultValue: "1", submitOnChange: true)
                }
            }
        }

        if (attribute_){
            state_ =  sensor_.currentState(attribute_);
            if (state_ != null) {
                currentValue = state_.value;
                parent.hubiForm_section(this, "Min Max Value", 1){
                    container = [];
                    container<< parent.hubiForm_text(this, "<b>Current Value = </b>$currentValue");
                    container << parent.hubiForm_text_input (this, "Minimum Value for Gauge", "minValue_", "0", false);  
                    container << parent.hubiForm_text_input (this, "Maximum Value for Gauge", "maxValue_", "100", false);
                    parent.hubiForm_container(this, container, 1); 
                }                      
            } else {
                container = [];
                paragraph "<b>No recent valid events</b><br><small>Please select a different Attribute</small>"  
                parent.hubiForm_container(this, container, 1); 
           }
        }
    }
}



def graphSetupPage(){
    def fontEnum = [["1":"1"], ["2":"2"], ["3":"3"], ["4":"4"], ["5":"5"], ["6":"6"], ["7":"7"], ["8":"8"], ["9":"9"], ["10":"10"], 
                    ["11":"11"], ["12":"12"], ["13":"13"], ["14":"14"], ["15":"15"], ["16":"16"], ["17":"17"], ["18":"18"], ["19":"19"], ["20":"20"]];  
    
    def highlightEnum = [[0:"0"], [1:"1"], [2:"2"], [3:"3"]];
    
    def num_;
    
    dynamicPage(name: "graphSetupPage") {
        parent.hubiForm_section(this,"General Options", 1){
            container = [];
            container << parent.hubiForm_text_input (this, "Gauge Title", "gauge_title", "Gauge Title", false);
            container << parent.hubiForm_text_input (this, "Gauge Units", "gauge_units", "Units", false);
            container << parent.hubiForm_text_input (this, "Gauge Number Formatting<br><small>Example</small>", "gauge_number_format", "##.#", false);

                    
            
            container << parent.hubiForm_slider (this, "Select Number of Highlight Areas on Gauge", "num_highlights",  3, 0, 3, " highlights");
                      
            parent.hubiForm_container(this, container, 1); 
        }
        
        num_ = num_highlights.toInteger();
        log.debug("$num_ $num_highlights");
        
        if (num_ > 0){
            parent.hubiForm_section(this,"HighLight Regions", 1){
                container = [];
                for (i=0; i<3; i+=1){
                    switch (i) {
                        case 0 : color_ = "#00FF00"; break;
                        case 1 : color_ = "#a9a67e"; break;
                        case 2 : color_ = "#FF0000"; break;
                    }
                    container << parent.hubiForm_color      (this, "Highlight $i", "highlight${i}", color_, false);
                    container << parent.hubiForm_text_input (this, "Select Highlight Start Region Value ${i}", "highlight${i}_start", "", false);
                }
                container << parent.hubiForm_text_input (this, "Select Highlight End Region Value ${i-1}", "highlight_end", "", false);
                parent.hubiForm_container(this, container, 1); 
           }
            
        }
        
        parent.hubiForm_section(this,"Major and Minor Tics", 1){
                container = [];
                container << parent.hubiForm_slider (this, "Number Minor Tics", "gauge_minor_tics",  3, 0, 10, " tics");
                
                container << parent.hubiForm_switch     (this, "Use Custom Tics/Labels", "default_major_ticks", false, true);
                if (default_major_ticks == true){
                    container << parent.hubiForm_slider (this, "Number Major Tics", "gauge_major_tics",  3, 0, 20, " tics");
                    for (tic = 0; tic<gauge_major_tics.toInteger(); tic++){
                        container << parent.hubiForm_text_input (this, "Input the Label for Tick ${tic+1}", "tic_title${tic}", "Label", false);
                    }
                } 
               parent.hubiForm_container(this, container, 1);   
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
    dynamicPage(name: "mainPage") {        
       
            def container = [];
            if (!state.endpoint) {
                parent.hubiForm_section(this, "Please set up OAuth API", 1, "report"){
                    href name: "enableAPIPageLink", title: "Enable API", description: "", page: "enableAPIPage"    
                 }    
            } else {
               parent.hubiForm_section(this, "Graph Options", 1, "tune"){
                    container = [];
                    container << parent.hubiForm_page_button(this, "Select Device/Data", "deviceSelectionPage", "100%", "vibration");
                    container << parent.hubiForm_page_button(this, "Configure Graph", "graphSetupPage", "100%", "poll");
                    
                    parent.hubiForm_container(this, container, 1); 
                }
                parent.hubiForm_section(this, "Local Graph URL", 1, "link"){
                    container = [];
                    container << parent.hubiForm_text(this, "${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
                    
                    parent.hubiForm_container(this, container, 1); 
                }
                
                if (graph_timespan){
                     parent.hubiForm_section(this, "Preview", 10, "show_chart"){                         
                         container = [];
                         container << parent.hubiForm_graph_preview(this)
                         
                         parent.hubiForm_container(this, container, 1); 
                     } //graph_timespan
            
                    parent.hubiForm_section(this, "Hubigraph Tile Installation", 2, "apps"){
                        container = [];
                             
                        container << parent.hubiForm_switch(this, "Install Hubigraph Tile Device?", "install_device", false, true);
                        if (install_device==true){ 
                             container << parent.hubiForm_text_input(this, "Name for HubiGraph Tile Device", "device_name", "Hubigraph Tile", "false");
                        }
                        parent.hubiForm_container(this, container, 1); 
                    }
                } 
             
            
               if (state.endpoint){
                   parent.hubiForm_section(this, "Hubigraph Application", 1, "settings"){
                        container = [];
                        container << parent.hubiForm_sub_section(this, "Application Name");
                        container << parent.hubiForm_text_input(this, "Rename the Application?", "app_name", "Hubigraph Bar Graph", "false");
                        container << parent.hubiForm_sub_section(this, "Debugging");
                        container << parent.hubiForm_switch(this, "Enable Debug Logging?", "debug", false, false);
                        container << parent.hubiForm_sub_section(this, "Disable Oauth Authorization");
                        container << parent.hubiForm_page_button(this, "Disable API", "disableAPIPage", "100%", "cancel");  
                       
                        parent.hubiForm_container(this, container, 1); 
                    }
               }
       
            } //else 
        
    } //dynamicPage
}



def installed() {
    logDebug "Installed with settings: ${settings}"
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
        parent.hubiTool_create_tile(this);    
    }
}

def buildData() {
    return extractNumber(sensor_.currentState(attribute_).getStringValue());
    
}

def getChartOptions(){
    
    if (default_major_ticks == true){
        tic_labels = [];
        for (tic=0; tic<gauge_major_tics.toInteger(); tic++){
            tic_labels += settings["tic_title${tic}"]
        }
    }
    
    highlightString = "";
    switch (num_highlights.toInteger()){
           
        case 3: 
        redColor = highlight2_color_transparent ? "transparent" : highlight2_color;
        redFrom  = highlight2_start;
        redTo    = highlight_end;
        
        yellowColor  = highlight1_color_transparent ? "transparent" : highlight1_color;
        yellowFrom   = highlight1_start;
        yellowTo     = highlight2_start;
        
        greenColor  = highlight0_color_transparent ? "transparent" : highlight0_color;
        greenFrom   = highlight0_start;
        greenTo     = highlight1_start;
            
        break;
            
        case 2:

        yellowColor  = highlight1_color_transparent ? "transparent" : highlight1_color;
        yellowFrom   = highlight1_start;
        yellowTo     = highlight_end;
        
        greenColor  = highlight0_color_transparent ? "transparent" : highlight0_color;
        greenFrom   = highlight0_start;
        greenTo     = highlight1_start;
        
        break;
        
        case 1: 

        greenColor  =  highlight0_color_transparent ? "transparent" : highlight0_color;
        greenFrom   = highlight0_start;
        greenTo     = highlight_end
        
        break;
    }
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
            "backgroundColor": graph_background_color_transparency ? "transparent": graph_background_color,
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

graphData.value = parseFloat(value.match(/[0-9.]+/g)[0]);

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
    dataTable.addRow(['${gauge_title}', graphData.value]);

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
            logDebug("Error: $e");
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
