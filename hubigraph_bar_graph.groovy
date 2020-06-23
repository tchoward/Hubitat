/**
 *  Hubigraph BarGraph Child App
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

// Hubigraph Bar Graph Changelog
// V 0.1 Intial release
// V 0.2 Ordering, Color and Common API Update

import groovy.json.JsonOutput

def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v0.22" }

definition(
    name: "Hubigraph Bar Graph",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Bar Graph",
    category: "",
    parent: "tchoward:Hubigraphs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
       page(name: "mainPage", install: true, uninstall: true)
       page(name: "deviceSelectionPage", nextPage: "attributeConfigurationPage")
       page(name: "attributeConfigurationPage", nextPage: "mainPage")
       page(name: "graphSetupPage", nextPage: "mainPage")
       page(name: "enableAPIPage")
       page(name: "disableAPIPage") 

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
        parent.hubiForm_section(this, "Device Selection", 1)  {
            input (type: "capability.*", name: "sensors", title: "Choose Sensors", multiple: true, submitOnChange: true)
            if (sensors) {
                def all = (1..sensors.size()).collect{ "" + it };
                sensors.eachWithIndex {sensor, idx ->
                    id = sensor.id;
                    sensor_attributes = sensor.getSupportedAttributes().collect { it.getName() }.unique();      
                    container = [];
                    container <<  parent.hubiForm_sub_section(this,  "${sensor.displayName}");
                    parent.hubiForm_container(this, container, 1);     
                    input( type: "enum", name: "attributes_${id}", title: "Attributes to graph", required: true, multiple: true, options: sensor_attributes, defaultValue: "1", submitOnChange: false )                                     
                }
            }
        }
    }

}

def attributeConfigurationPage() {
                           
    
    
    dynamicPage(name: "attributeConfigurationPage") {
         parent.hubiForm_section(this, "Directions", 1, "directions"){
            container = [];
            container << parent.hubiForm_text(this, "Choose Numeric Attributes Only");
            parent.hubiForm_container(this, container, 1); 

         }
        
         parent.hubiForm_section(this, "Graph Order", 1, "directions"){
             parent.hubiForm_list_reorder(this, "graph_order", "background");       
         }
         
        
         count = 0;
         sensors.each { sensor ->
             attributes = settings["attributes_${sensor.id}"];
             attributes.each { attribute ->
                 container = [];
                 parent.hubiForm_section(this, "${sensor.displayName} ${attribute}", 1, "directions"){
                            container << parent.hubiForm_text_input(this,   "Override Device Name<small></i><br>Use %deviceName% for DEVICE and %attributeName% for ATTRIBUTE</i></small>",
                                                                            "graph_name_override_${sensor.id}_${attribute}",
                                                                            "%deviceName%: %attributeName%", false);
                            container << parent.hubiForm_color      (this,  "Bar Background",               "attribute_${sensor.id}_${attribute}_background", "#3e4475", false, true);
                            container << parent.hubiForm_color      (this,  "Bar Border",                   "attribute_${sensor.id}_${attribute}_current_border", "#FFFFFF", false);
                            container << parent.hubiForm_line_size  (this,  "Bar Border",                   "attribute_${sensor.id}_${attribute}_current_border", 2, 1, 10);
                            container << parent.hubiForm_switch     (this,  "Show Current Value on Bar",    "attribute_${sensor.id}_${attribute}_show_value", false, true);
                            if (settings["attribute_${sensor.id}_${attribute}_show_value"]==true){
                                container<< parent.hubiForm_text_input(this, "Units", "attribute_${sensor.id}_${attribute}_annotation_units", "", false)
                            }
                            parent.hubiForm_container(this, container, 1); 
                       }
                       
             }
        }
    }
}

def graphSetupPage(){
    
    def rateEnum = [["-1":"Never"], ["0":"Real Time"], ["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], 
                    ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]];
    
    def timespanEnum = [[0:"Live"], [1:"Hourly"], [2:"Daily"], [3:"Every Three Days"], [4:"Weekly"]];
    
    dynamicPage(name: "graphSetupPage") {
        parent.hubiForm_section(this, "General Options", 1){
            container = [];
            input( type: "enum", name: "graph_type", title: "<b>Select graph type</b>", multiple: false, required: false, options: [["1": "Bar Chart"],["2": "Column Chart"]], defaultValue: "1");
            input( type: "enum", name: "graph_update_rate", title: "<b>Select graph update rate</b>", multiple: false, required: false, options: rateEnum, defaultValue: "0")
            
                 
            container << parent.hubiForm_color (this, "Graph Background", "graph_background", "#FFFFFF", false)
            container << parent.hubiForm_slider (this, title: "Graph Bar Width (1%-100%)", name: "graph_bar_percent", default_value: 90, min: 1, max: 100, units: "%");
            container << parent.hubiForm_text_input(this, "Graph Max", "graph_max", "", false);
            container << parent.hubiForm_text_input(this, "Graph Min", "graph_min", "", false);   

            parent.hubiForm_container(this, container, 1);   
        }

        parent.hubiForm_section(this, "Axes", 1){
            container = [];
            container << parent.hubiForm_color (this, "Axis", "haxis", "#000000", false);
            container << parent.hubiForm_font_size (this, "Axis", "haxis", 9, 2, 20);
            container << parent.hubiForm_slider (this, title: "Number of Pixels for Axis", name: "graph_h_buffer", default_value: 40, min: 10, max: 500, units: " pixels");
            parent.hubiForm_container(this, container, 1);  
        }
        parent.hubiForm_section(this, "Device Names", 1){
            container = [];
            container << parent.hubiForm_font_size (this, "Device Name","graph_axis",  9, 2, 20);
            container << parent.hubiForm_color (this, "Device Name","graph_axis",  "#000000", false);         
            container << parent.hubiForm_slider (this, title: "Number of Pixels for Device Name Area", name: "graph_v_buffer",  default_value: 100, min: 10, max: 500, " pixels");

            parent.hubiForm_container(this, container, 1); 
        }
        parent.hubiForm_section(this, "Graph Size", 1){
            container = [];
            input( type: "bool", name: "graph_static_size", title: "<b>Set size of Graph?</b><br><small>(False = Fill Window)</small>", defaultValue: false, submitOnChange: true);
            if (graph_static_size==true){      
                container << parent.hubiForm_slider (this, title: "Horizontal dimension of the graph", name: "graph_h_size",  default_value: 800, min: 100, max: 3000, units: " pixels");
                container << parent.hubiForm_slider (this, title: "Vertical dimension of the graph", name: "graph_v_size",  default_value: 600, min: 100, max: 3000, units: " pixels");   
            }

            parent.hubiForm_container(this, container, 1); 
        }
        parent.hubiForm_section(this, "Annotations", 1){
            container = [];
            container << parent.hubiForm_font_size (this, "Annotation", "annotation", 16, 2, 40);
            container << parent.hubiForm_switch    (this, "Show Annotation Outside (true) or Inside (false) of Bars", "annotation_inside", false, false);
            container << parent.hubiForm_color     (this, "Annotation", "annotation",  "#FFFFFF", false);
            container << parent.hubiForm_color     (this, "Annotation Aura", "annotation_aura", "#000000", false);
            container << parent.hubiForm_switch    (this, "Bold Annotation", "annotation_bold", false, false);
            container << parent.hubiForm_switch    (this, "Italic Annotation", "annotation_bold", false, false);

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
                
                if (graph_update_rate){
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
    log.debug "Installed with settings: ${settings}"
    updated();
}

def uninstalled() {
    if (state.endpoint) {
        try {
            log.debug "Revoking API access token"
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
    def resp = [:]
    def now = new Date();
    def then = new Date();
            
    if(sensors) {
      sensors.each {sensor ->
          def attributes = settings["attributes_${sensor.id}"];
          resp[sensor.id] = [:];
          attributes.each { attribute ->
              latest = sensor.latestState(attribute);
              resp[sensor.id][attribute] = [current: latest.getFloatValue(), date: latest.getDate()];
          }
      }
   }
   return resp
}

def getChartOptions(){
    
    colors = [];
    sensors.each {sensor->
        def attributes = settings["attributes_${sensor.id}"];
        attributes.each {attribute->
            attrib_string = "attribute_${sensor.id}_${attribute}_color"
            transparent_attrib_string = "attribute_${sensor.id}_${attribute}_color_transparent"
            colors << (settings[transparent_attrib_string] ? "transparent" : settings[attrib_string]);
           
        }
    }
    
    if (graph_type == "1"){
        axis1 = "hAxis";
        axis2 = "vAxis";
    } else {
        axis1 = "vAxis";
        axis2 = "hAxis";
    }
    
    def options = [
        "graphUpdateRate": Integer.parseInt(graph_update_rate),
        "graphType": Integer.parseInt(graph_type),
        "graphOptions": [
            "bar" : [ "groupWidth" : "${graph_bar_percent}%",
                    ],
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size: "90%",
            "timeline": [
                "rowLabelStyle": ["fontSize": graph_axis_font, "color": graph_axis_color_transparent ? "transparent" : graph_axis_color],
                "barLabelStyle": ["fontSize": graph_axis_font]
            ],
            "backgroundColor": graph_background_color_transparent ? "transparent" : graph_background_color,
            "isStacked": false,
            "chartArea": [ "left": graph_type == "1" ? graph_v_buffer : graph_h_buffer, 
                           "right" : 10, 
                           "top": 10, 
                           "bottom": graph_type == "1" ? graph_h_buffer : graph_v_buffer ],
            "legend" : [ "position" : "none" ],
            "${axis1}": [ "viewWindow" : ["max" : graph_max, 
                                        "min" : graph_min], 
                         "minValue" : graph_min, 
                         "maxValue" : graph_max,
                         "textStyle" : ["color": haxis_color_transparent ? "transparent" : haxis_color,
                                       "fontSize": haxis_font]
                      ],
            "${axis2}": [ "textStyle" : ["color": graph_axis_color_transparent ? "transparent" : graph_axis_color,
                                    "fontSize": graph_axis_font]
                       ],
            "annotations" : [    "alwaysOutside": annotation_inside,
                                 "textStyle": [
      					            "fontSize": annotation_font,
      					            "bold":     annotation_bold,
      					            "italic":   annotation_italic,
      	         					"color":    annotation_color_transparent ? "transparent" : annotation_color,
      					            "auraColor":annotation_aura_color_transparent ? "transparent" : annotation_aura_color,
				                 ],
                                 "stem": [ "color": "transparent" ],
                                 "highContrast": "false"
                             ],
              
        			 
         ],
        "graphLow": graph_min,
        "graphHigh": graph_max,
                
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
google.charts.load('current', {'packages':['corechart']});

let options = [];
let subscriptions = {};
let graphData = {};

//stack for accumulating points to average
let stack = {};

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
    if(subscriptions.ids.includes(deviceId) && subscriptions.attributes[deviceId].includes(event.name)) {
        let value = event.value;
        let attribute = event.name;
        
        console.log("Got Name: ", attribute, "Value: ", value);

        graphData[deviceId][attribute].current = value;
        graphData[deviceId][attribute].date = new Date();
        //update if we are realtime
        if(options.graphUpdateRate === 0) update();
    }
}

function update(callback) {
    drawChart(callback);
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
        drawChart();
    });
}

function onBeforeUnload() {
    if(websocket) websocket.close();
}

function drawChart(callback) {
    let now = new Date().getTime();
    let min = now - options.graphTimespan;
    const date_options = {
         weekday: "long",
         year: "numeric",
         month:"long",
         day:"numeric"
    };
    const time_options ={
         hour12 : true,
         hour:  "2-digit",
         minute: "2-digit",
         second: "2-digit"
    };


    const dataTable = new google.visualization.arrayToDataTable([[{ type: 'string', label: 'Device' }, { type: 'number', label: 'Value'},   { role: "style" }, { role: "tooltip" }, { role: "annotation" },]]);

    subscriptions.order.forEach(orderStr => {
      const splitStr = orderStr.split('_');
      const deviceId = splitStr[1];
      const attr = splitStr[2];
      const event = graphData[deviceId][attr];
        const cur_ = parseFloat(event.current);
        var cur_String = '';
        var units_ = ``;
        
        var t_date = new Date(event.date);
        var date_String = t_date.toLocaleDateString("en-US",date_options);
        var time_String = t_date.toLocaleTimeString("en-US",time_options);

        const name = subscriptions.labels[deviceId][attr].replace('%deviceName%', subscriptions.sensors[deviceId].displayName).replace('%attributeName%', attr);
        const colors = subscriptions.colors[deviceId][attr];
        if (colors.showAnnotation == true){
            cur_String = `\${cur_.toFixed(1)}\${colors.annotation_units} `;
            units_ = `\${colors.annotation_units}`;
        }

        var stats_ = `\${name}\nCurrent: \${event.current}\${units_}\nDate: \${date_String} \${time_String}`

        dataTable.addRow([name,  cur_, `{color:         \${colors.backgroundColor}; stroke-color:   \${colors.currentValueBorderColor}; 
                                        stroke-opacity: 1.0; 
                                        stroke-width:  \${colors.currentValueBorderLineSize};}`,      
                                 `\${stats_}`,     
                                 `\${cur_String} `]);

    });

    var chart;

    if (options.graphType == 1) {
        chart = new google.visualization.BarChart(document.getElementById("timeline"));
    } else {
        chart = new google.visualization.ColumnChart(document.getElementById("timeline"));
    }
    //if we have a callback
    if(callback) google.visualization.events.addListener(chart, 'ready', callback);

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
    
    ret = "#FFFFFF"
    switch (code){
        case 7:  ret = "#800000"; break;
        case 1:	    ret = "#FF0000"; break;
        case 6:	ret = "#FFA500"; break;	
        case 8:	ret = "#FFFF00"; break;	
        case 9:	ret = "#808000"; break;	
        case 2:	ret = "#008000"; break;	
        case 5:	ret = "#800080"; break;	
        case 4:	ret = "#FF00FF"; break;	
        case 10: ret = "#00FF00"; break;	
        case 11: ret = "#008080"; break;	
        case 12: ret = "#00FFFF"; break;	
        case 3:	ret = "#0000FF"; break;	
        case 13: ret = "#000080"; break;	
    }
    return ret;
}

//oauth endpoints
def getGraph() {
    return render(contentType: "text/html", data: getTimeLine());      
}

def getData() {
    def data = buildData();
         
    return render(contentType: "text/json", data: JsonOutput.toJson(data));
}

def getOptions() {
    return render(contentType: "text/json", data: JsonOutput.toJson(getChartOptions()));
}

def getSubscriptions() {
    def _ids = [];
    def _attributes = [:];
    def labels = [:];
    def colors = [:];
    sensors.each { sensor ->
        _ids << sensor.idAsLong;
        _attributes[sensor.id] = [];
        labels[sensor.id] = [:];
        colors[sensor.id] = [:];
        def attributes = settings["attributes_${sensor.id}"];
        attributes.each { attribute ->
            _attributes[sensor.id] << attribute;
            labels[sensor.id][attribute] = settings["graph_name_override_${sensor.id}_${attribute}"];
            colors[sensor.id][attribute] = ["backgroundColor":             settings["attribute_${sensor.id}_${attribute}_background_color"],
                                            "currentValueColor":           settings["attribute_${sensor.id}_${attribute}_current_color"],
                                            "currentValueBorderColor":     settings["attribute_${sensor.id}_${attribute}_current_border_color"],
                                            "currentValueBorderLineSize":  settings["attribute_${sensor.id}_${attribute}_current_border_line_size"],
                                            "showAnnotation":              settings["attribute_${sensor.id}_${attribute}_show_value"],
                                            "annotation_font":             settings["attribute_${sensor.id}_${attribute}_annotation_font"],
                                            "annotation_font_size":        settings["attribute_${sensor.id}_${attribute}_annotation_font_size"],
                                            "annotation_color":            settings["attribute_${sensor.id}_${attribute}_annotation_color"],
                                            "annotation_units":            settings["attribute_${sensor.id}_${attribute}_annotation_units"],
                                           ];
        }
                                               
    }
                                                                                    
    def sensors_fmt = [:];
    sensors.each { it ->
        sensors_fmt[it.id] = [ "id": it.id, "displayName": it.displayName, "currentStates": it.currentStates ];
    }
    
   
    
    def order = parseJson(graph_order);
   
    def subscriptions = [
        "sensors": sensors_fmt,
        "ids": _ids,
        "attributes": _attributes,
        "labels": labels,
        "colors": colors,
        "order": order
    ];
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
}
