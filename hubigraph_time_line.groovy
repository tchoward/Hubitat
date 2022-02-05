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
// V 1.5 Ordering, Color and Common API Update
// V 1.8 Smoother sliders, bug fixes


 
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
       page(name: "mainPage", install: true, uninstall: true)
       page(name: "deviceSelectionPage", nextPage: "attributeConfigurationPage")
       page(name: "attributeConfigurationPage", nextPage: "graphSetupPage")
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

def call(Closure code) {
    code.setResolveStrategy(Closure.DELEGATE_ONLY);
    code.setDelegate(this);
    code.call();
}

def deviceSelectionPage() {                  
    dynamicPage(name: "deviceSelectionPage") {
        parent.hubiForm_section(this, "Device Selection", 1)  {
            input (type: "capability.*", name: "sensors", title: "Choose Sensors", multiple: true, submitOnChange: true)
            if (sensors) {
                sensors.each{sensor ->
                    id = sensor.id;
                    sensor_attributes = sensor.getSupportedAttributes().collect { it.getName() }.unique().sort();      
                    def container = [];
                    container <<  parent.hubiForm_sub_section(this,  "${sensor.displayName}");
                    parent.hubiForm_container(this, container, 1);     
                    input( type: "enum", name: "attributes_${id}", title: "Attributes to graph", required: true, multiple: true, options: sensor_attributes, defaultValue: "1", submitOnChange: false )                                     
                }
                
            }
        }
    }

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
    	parent.hubiForm_section(this, "Directions", 1, "directions"){
            container = [];
            container << parent.hubiForm_text(this, """Configure what counts as a 'start' or 'end' event for each attribute on the timeline. 
            										   For example, Switches start when they are 'on' and end when they are 'off'.\n\nSome attributes will automatically populate. 
                                                       You can change them if you have a different configuration (chances are you won't).
                                                       Additionally, for devices with numeric values, you can define a range of values that count as 'start' or 'end'. 
                                                       For example, to select all the times a temperature is above 70.5 degrees farenheight, you would set the start to '> 70.5', and the end to '< 70.5'.
                                                       Supported comparitors are: '<', '>', '<=', '>=', '==', '!='.\n\nBecause we are dealing with HTML, '<' is abbreviated to &amp;lt; after you save. That is completely normal. It will still work.""" );
        
            parent.hubiForm_container(this, container, 1); 

         }
         parent.hubiForm_section(this, "Graph Order", 1, "directions"){
             parent.hubiForm_list_reorder(this, "graph_order", "line");       
         }
         
         cnt = 1;
         sensors.each { sensor ->
             def attributes = settings["attributes_${sensor.id}"];
             attributes.each { attribute ->
                 state.count_++;
                 parent.hubiForm_section(this, "${sensor.displayName} ${attribute}", 1, "directions", sensor.id){
                 		container = [];
                        container << parent.hubiForm_text_input(this,   "Override Device Name<small></i><br>Use %deviceName% for DEVICE and %attributeName% for ATTRIBUTE</i></small>",
                                                                        "graph_name_override_${sensor.id}_${attribute}",
                                                                        "%deviceName%: %attributeName%", false);
                        container << parent.hubiForm_color      (this,  "Line", 				"attribute_${sensor.id}_${attribute}_line",  "#3e4475", false, true);
						container << parent.hubiForm_text_input (this,  "Start event value", 	"attribute_${sensor.id}_${attribute}_start", supportedTypes[attribute] ? supportedTypes[attribute].start : "", false);
                        container << parent.hubiForm_text_input (this,  "End event value", 		"attribute_${sensor.id}_${attribute}_end",   supportedTypes[attribute] ? supportedTypes[attribute].end :   "", false);
                        parent.hubiForm_container(this, container, 1); 
                        }
                 cnt += 1;
                 }
         }
    }
}                 


def graphSetupPage(){
    
    
    dynamicPage(name: "graphSetupPage") {
    	parent.hubiForm_section(this, "General Options", 1, "directions"){	
            input( type: "enum", name: "graph_update_rate", title: "<b>Select graph update rate</b>", multiple: false, required: false, 
                  options: [["-1":"Never"], ["0":"Real Time"], ["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]], defaultValue: "0")
            input( type: "enum", name: "graph_timespan", title: "<b>Select Timespan to Graph</b>", multiple: false, required: false, options: [["60000":"1 Minute"], ["3600000":"1 Hour"], ["43200000":"12 Hours"], ["86400000":"1 Day"], ["259200000":"3 Days"], ["604800000":"1 Week"]], defaultValue: "43200000")     
            input( type: "enum", name: "graph_combine_rate", title: "<b>Combine events with events less than ? apart</b>", multiple: false, required: false, options: 
                  [["0":"Never"], ["10000":"10 Seconds"], ["30000":"30 seconds"], ["60000":"1 Minute"], ["120000":"2 Minutes"], ["180000":"3 Minutes"], ["240000":"4 Minutes"], ["300000":"5 Minutes"], ["600000":"10 Minutes"], 
                   ["1200000":"20 Minutes"], ["1800000":"30 Minutes"], ["3600000":"1 Hour"], ["6400000":"2 Hours"], ["9600000":"3 Hours"], ["13200000":"4 Hours"],  ["16800000":"5 Hours"], ["20400000":"6 Hours"]], defaultValue: "Never")     

            container = [];
            container << parent.hubiForm_color (this, "Background", "graph_background",  "#FFFFFF", false);
            
        }
        parent.hubiForm_section(this, "Graph Size", 1){
            container = [];
            input( type: "bool", name: "graph_static_size", title: "<b>Set size of Graph?</b><br><small>(False = Fill Window)</small>", defaultValue: false, submitOnChange: true);
            if (graph_static_size==true){      
                container << parent.hubiForm_slider (this, title: "Horizontal dimension of the graph", name: "graph_h_size",  default_value: 800, min: 100, max: 3000, units: " pixels", submit_on_change: false);
                container << parent.hubiForm_slider (this, title: "Vertical dimension of the graph", name: "graph_v_size",  default_value: 600, min: 100, max: 3000, units: " pixels", submit_on_change: false);   
            }

            parent.hubiForm_container(this, container, 1); 
        }
        parent.hubiForm_section(this, "Devices", 1){
            container = [];
            container << parent.hubiForm_color (this, 	  "Device Text", "graph_axis", "#FFFFFF", false);
            container << parent.hubiForm_font_size (this, title: "Device", name: "graph_axis", default: 9, min: 2, max: 20);
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
                    container << parent.hubiForm_text(this, "${state.localEndpointURL}graph/?access_token=${state.endpointSecret}",
													 "${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
                    
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
                             
                        container << parent.hubiForm_switch(this, title: "Install Hubigraph Tile Device?", name: "install_device", default: false, submit_on_change: true);
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
                        container << parent.hubiForm_switch(this, title: "Enable Debug Logging?", name: "debug", default: false);
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
    
    use (groovy.time.TimeCategory) {
           then -= Integer.parseInt(graph_timespan).milliseconds;
    }
    
    if(sensors) {
      sensors.each {sensor ->
          def attributes = settings["attributes_${sensor.id}"];
          resp[sensor.id] = [:];
          attributes.each { attribute ->
              temp = sensor.statesSince(attribute, then, [max: 50000]).collect{[ date: it.date, value: it.value ]}
              temp = temp.sort{ it.date };
              temp = temp.collect{ [date: it.date.getTime(), value: it.value] }             
              resp[sensor.id][attribute] = temp;
          }
      }
   }
   return resp
}

def getChartOptions(){
    
    def colors = [];
    order = parent.hubiTools_get_order(graph_order);
    order.each{ device->
        attrib_string = "attribute_${device.id}_${device.attribute}_line_color"
        transparent_attrib_string = "attribute_${device.id}_${device.attribute}_line_color_transparent"
        colors << (settings[transparent_attrib_string] ? "transparent" : settings[attrib_string]);         
    }
    
    /*
    sensors.each {sensor->
        def attributes = settings["attributes_${sensor.id}"];
        attributes.each {attribute->
            attrib_string = "attribute_${sensor.id}_${attribute}_line_color"
            transparent_attrib_string = "attribute_${sensor.id}_${attribute}_line_color_transparent"
            colors << (settings[transparent_attrib_string] ? "transparent" : settings[attrib_string]);         
        }
    }
    */

    def options = [
        "graphTimespan": Integer.parseInt(graph_timespan),
        "graphUpdateRate": Integer.parseInt(graph_update_rate),
        "graphCombine_msecs": Integer.parseInt(graph_combine_rate),
        "graphOptions": [
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size: "100%",
            "timeline": [
                "rowLabelStyle": ["fontSize": graph_axis_font, "color": graph_axis_color_transparent ? "transparent" : graph_axis_color],
                "barLabelStyle": ["fontSize": graph_axis_font],
            ],
            "haxis" : [ "text": ["fontSize": "24px"]],
            "backgroundColor": graph_background_color_transparent ? "transparent" : graph_background_color,
            "colors" : colors
        ],
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
            <script src="/local/HubiGraph.js"></script>
            <script type="text/javascript">
            
google.load("visualization", "1.1", {packages:["timeline"]});
//google.charts.load('current', {'packages':['timeline']});
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

            //merge events
            let mergedArr = [];

            newArr.forEach((event, index) => {
                if(index === 0) mergedArr.push(event);
                else {
                    if(event.start - mergedArr[mergedArr.length - 1].end <= options.graphCombine_msecs) {
                        mergedArr[mergedArr.length - 1].end = event.end;
                    } else mergedArr.push(event);
                }
            });

            graphData[id][attribute] = mergedArr;
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
            var date;
            var seconds = options.graphCombine_msecs;
            var skip_trigger;
            if(events.length > 0) {
                //if our first event is an end event, start at 1
                thisOut.push(evalTest(start_event, events[0].value) ? { start: events[0].date } : { end: events[0].date });
                for(let i = 1; i < events.length; i++) {
                    const is_start = evalTest(start_event, events[i].value);
                    const is_end = evalTest(end_event, events[i].value);
                    
                    //always add the first event
                    if (is_end && !thisOut[thisOut.length - 1].end){
                        thisOut[thisOut.length - 1].end = events[i].date;
                        
                    } else if(is_start && thisOut[thisOut.length - 1].end){
                        /*TCH - Look for more than 5 minutes between events*/
                        if (events[i].date - thisOut[thisOut.length - 1].end > seconds){
                            thisOut.push({ start: events[i].date });
                        } else {
                            skip_trigger = true;
                        } 
                    } else if (is_end && skip_trigger){
                        thisOut[thisOut.length - 1].end = events[i].date;
                        skip_trigger = false;
                    }
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

function getToolTip(name, start, end){
    var html =     "<div class='mdl-layout__header' style='display: block; background:#033673; width: 100%; padding-top:10px; padding-bottom:5px; overflow: hidden;'>";
    html +=        "<div class='mdl-layout__header-row'";
    html +=        "<span class='mdl-layout__title' style='font-size: 14px; color:#FFFFFF !important; width: auto; font-family:Roboto, Helvetica, Arial, sans-serif !important;'>";
    html +=        name;
    html +=        "</span>";
    html +=        "</div>"; 
    html +=        "</div>";

    html +=        "<div class = 'mdl-grid' style='padding: 5px; background:#FFFFFF; font-family:Roboto, Helvetica, Arial, sans-serif !important;'>" 
    html +=        "<div class='mdl-cell mdl-cell--12-col-desktop mdl-cell--8-col-tablet mdl-cell--4-col-phone' style='margin-bottom: 5px; padding: 5px;' >";
    html = html+   start.toDateString()+" at "+start.toLocaleTimeString('en-US');
    html +=        "</div>";    
    html +=        "<div class='mdl-cell mdl-cell--12-col-desktop mdl-cell--8-col-tablet mdl-cell--4-col-phone' style='margin-bottom: 5px; padding: 5px;'>";
    html = html+   end.toDateString()+" at "+end.toLocaleTimeString('en-US');
    html +=        "</div>";    

    
    
    //var html = "<p style = 'font-family:courier,arial,helvetica; font-size: 14px;'><b>"+name+"</b><br><hr><br>";
    //html +=    "Start: "+start.toDateString()+" at "+start.toLocaleTimeString('en-US')+"<br>";
    //html +=    "End: "+end.toDateString()+" at "+start.toLocaleTimeString('en-US')+"<br>";
    return html;
}

function drawChart(now, min, callback) {
    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn({ type: 'string', id: 'Device' });
    dataTable.addColumn({ type: 'date', id: 'Start' });
    dataTable.addColumn({ type: 'date', id: 'End' });
    dataTable.addColumn({'type': 'string', 'role': 'tooltip', 'p': {'html': true}});

    subscriptions.order.forEach(orderStr => {
      	const splitStr = orderStr.split('_');
      	const id = splitStr[1];
      	const attribute = splitStr[2];
      	const events = graphData[id][attribute];
        
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
            
            dataTable.addRows(newArr.map((parsed) => [subscriptions.labels[id][attribute].replace('%deviceName%', name).replace('%attributeName%', attribute), 
                                                      moment(parsed.start).toDate(), 
                                                      moment(parsed.end).toDate(), 
                                                      getToolTip(
                                                            subscriptions.labels[id][attribute].replace('%deviceName%', name).replace('%attributeName%', attribute), 
                                                            moment(parsed.start).toDate(), 
                                                            moment(parsed.end).toDate() )
                                                     ]));
       
    });

    let chart = new google.visualization.Timeline(document.getElementById("timeline"));

    //if we have a callback
    if(callback) google.visualization.events.addListener(chart, 'ready', callback);

    chart.draw(dataTable, options.graphOptions);
    
    google.visualization.events.addListener(chart, 'onmouseover', tooltipHandler);

    function tooltipHandler(e){
        if(e.row != null){
            jQuery(".google-visualization-tooltip").html(dataTable.getValue(e.row,3)).css({width:"auto",height:"auto"});
        }
    }

    

        
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
    def timeline = buildData();
       
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
    
    def order = parseJson(graph_order);
    
    def subscriptions = [
        "sensors": sensors_fmt,
        "definitions": definitions,
        "labels": labels,
        "order": order
    ];
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
}
