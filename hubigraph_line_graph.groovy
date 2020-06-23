import groovy.json.*;
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
// *****ALPHA BUILD
// v0.1 Initial release
// v0.2 My son added webpage efficiencies, reduced load on hubitat by 75%.
// v0.3 Loading Update; Removed ALL processing from Hub, uses websocket endpoint
// v0.5 Multiple line support
// v0.51 Select ANY device
// v0.60 Select AXIS to graph on
// v0.70 A lot more options
// v0.80 Added Horizontal Axis Formatting
// ****BETA BUILD
// v0.1 Added Hubigraph Tile support with Auto-add Dashboard Tile
// v0.2 Added Custom Device/Attribute Labels
// v0.3 Added waiting screen for initial graph loading & sped up load times
// v0.32 Bug Fixes
// V 1.0 Released (not Beta) Cleanup and Preview Enabled
// v 1.2 Complete UI Refactor
// V 1.5 Ordering, Color and Common API Update
    
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
       page(name: "mainPage", install: true, uninstall: true)
       page(name: "deviceSelectionPage", nextPage: "graphSetupPage")
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

def call(Closure code) {
    code.setResolveStrategy(Closure.DELEGATE_ONLY);
    code.setDelegate(this);
    code.call();
}

/********************************************************************************************************************************
*********************************************************************************************************************************
****************************************** PAGES ********************************************************************************
*********************************************************************************************************************************
*********************************************************************************************************************************/

def graphSetupPage(){
    def fontEnum = [["1":"1"], ["2":"2"], ["3":"3"], ["4":"4"], ["5":"5"], ["6":"6"], ["7":"7"], ["8":"8"], ["9":"9"], ["10":"10"], 
                    ["11":"11"], ["12":"12"], ["13":"13"], ["14":"14"], ["15":"15"], ["16":"16"], ["17":"17"], ["18":"18"], ["19":"19"], ["20":"20"]]; 
    
    def updateEnum = [["-1":"Never"], ["0":"Real Time"], ["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"],
                      ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]];
    
    def timespanEnum = [["60000":"1 Minute"], ["3600000":"1 Hour"], ["43200000":"12 Hours"], ["86400000":"1 Day"], ["259200000":"3 Days"], ["604800000":"1 Week"]]
     
     def timespanEnum2 = [["60000":"1 Minute"], ["120000":"2 Minutes"], ["300000":"5 Minutes"], ["600000":"10 Minutes"],
                                                        ["2400000":"30 minutes"], ["3600000":"1 Hour"], ["43200000":"12 Hours"], ["86400000":"1 Day"], ["259200000":"3 Days"], ["604800000":"1 Week"]];
                                
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
    
    dynamicPage(name: "graphSetupPage") {
      
        log.debug("$allowSmoothing");
        parent.hubiForm_section(this,"General Options", 1)
        {      
            input( type: "enum", name: "graph_type", title: "<b>Graph Type</b>", defaultValue: "Line Graph", options: ["Line Graph", "Area Graph", "Scatter Plot"], submitOnChange: true)
            input( type: "enum", name: "graph_update_rate", title: "<b>Select graph update rate</b>", multiple: false, required: true, options: updateEnum, defaultValue: "0")
            input( type: "enum", name: "graph_timespan", title: "<b>Select Timespan to Graph</b>", multiple: false, required: true, options: timespanEnum, defaultValue: "43200000")
            container = [];
            container << parent.hubiForm_color (this, "Graph Background",    "graph_background", "#FFFFFF", false)
            container << parent.hubiForm_switch(this, "Smooth Graph Points", "graph_smoothing", true, false);
            container << parent.hubiForm_switch(this, "<b>Flip Graph to Vertical?</b><br><small>(Rotate 90 degrees)</small>", "graph_y_orientation", false, false);
            container << parent.hubiForm_switch(this, "<b>Reverse Data Order?</b><br><small> (Flip data left to Right)</small>", "graph_z_orientation", false, false)
            container << parent.hubiForm_slider (this, title: "Maximum number of Data Points?</b><br><small>(Zero for ALL)</small>", name: "graph_max_points",  default_value: 0, min: 0, max: 1000, units: " data points", submit_on_change: false);
           
            parent.hubiForm_container(this, container, 1); 
     
        }
             
        parent.hubiForm_section(this,"Graph Title", 1)
        {    
            container = [];
            container << parent.hubiForm_switch(this, "Show Title on Graph", "graph_show_title", false, true);
            if (graph_show_title==true) {
                container << parent.hubiForm_text_input (this, "Graph Title", "graph_title", "Graph Title", false);
                container << parent.hubiForm_font_size  (this, "Title", "graph_title", 9, 2, 20);
                container << parent.hubiForm_color      (this, "Title", "graph_title", "#000000", false);
                container << parent.hubiForm_switch     (this, "Graph Title Inside Graph?", "graph_title_inside", false, false);
            }
            parent.hubiForm_container(this, container, 1); 
        }
            
         parent.hubiForm_section(this, "Graph Size", 1){
            container = [];
            container << parent.hubiForm_switch     (this, "<b>Set size of Graph?</b><br><small>(False = Fill Window)</small>", "graph_static_size", false, true);
            if (graph_static_size==true){      
                container << parent.hubiForm_slider (this, title: "Horizontal dimension of the graph", name: "graph_h_size",  default_value: 800, min: 100, max: 3000, units: " pixels", submit_on_change: false);
                container << parent.hubiForm_slider (this, title: "Vertical dimension of the graph", name: "graph_v_size",  default_value: 600, min: 100, max: 3000, units: " pixels", submit_on_change: false);   
            }

            parent.hubiForm_container(this, container, 1); 
        }
        
          parent.hubiForm_section(this,"Horizontal Axis", 1)
         { 
            //Axis
            container = [];
            container << parent.hubiForm_font_size  (this, "Horizontal Axis", "graph_haxis", 9, 2, 20);
            container << parent.hubiForm_color      (this, "Horizonal Header", "graph_hh", "#C0C0C0", false);
            container << parent.hubiForm_color      (this, "Horizonal Axis", "graph_ha", "#C0C0C0", false);
            container << parent.hubiForm_text_input (this, "<b>Num Horizontal Gridlines</b><br><small>(Blank for auto)</small>", "graph_h_num_grid", "", false);
            
            container << parent.hubiForm_switch     (this, "Show String Formatting Help", "dummy", false, true);
            if (dummy == true){
                val = [];
                val <<"<b>Name"; val << "Format" ; val <<"Result</b>"; 
                val <<"<small>Year"; val << "Y"; val << "2020"; 
                val <<"Month Number"; val << "M"; val << "12"; 
                val <<"Month Name "; val << "MMM"; val << "Feb"; 
                val <<"Month Full Name"; val << "MMMM"; val << "February"; 
                val <<"Day of Month"; val << "d"; val << "February"; 
                val <<"Day of Week"; val << "EEE"; val << "Mon"; 
                val <<"Day of Week"; val << "EEEE"; val << "Monday"; 
                val <<"Period"; val << "a"; val << "AM/PM"; 
                val <<"Hour (12)"; val << "h"; val << "1..12"; 
                val <<"Hour (12)"; val << "hh"; val << "01..12"; 
                val <<"Hour (24)"; val << "H"; val << "1..23"; 
                val <<"Hour (24)"; val << "HH"; val << "01..23"; 
                val <<"Minute"; val << "m"; val << "1..59"; 
                val <<"Minute"; val << "mm"; val << "01..59"; 
                val <<"Seconds"; val << "s"; val << "1..59"; 
                val <<"Seconds"; val << "ss"; val << "01..59 </small>"
                container << parent.hubiForm_cell(this, val, 3); 
                container << parent.hubiForm_text(this, """<b><small>Example: "EEEE, MMM d, Y hh:mm:ss a" <br>= "Monday, June 2, 2020 08:21:33 AM</small></b>""")
            }
            container << parent.hubiForm_text_input (this, "Horizontal Axis Format", "graph_h_format", "", true);
            parent.hubiForm_container(this, container, 1); 
            if (graph_h_format){
                today = new Date();
                paragraph "<i><small><b>Horizontal Axis Sample:</b> ${today.format(graph_h_format)}</small></i>"
            }
           
         }
            
        //Vertical Axis
         parent.hubiForm_section(this,"Vertical Axis", 1)
         { 
            container = [];
            container << parent.hubiForm_font_size (this, "Vertical Axis", "graph_vaxis", 9, 2, 20);
            container << parent.hubiForm_color (this, "Vertical Header", "graph_vh", "#000000", false);
            container << parent.hubiForm_color (this, "Vertical Axis", "graph_va", "#C0C0C0", false);
            parent.hubiForm_container(this, container, 1); 
         }

        //Left Axis    
        parent.hubiForm_section(this,"Left Axis", 1, "arrow_back"){  
            container = [];
            container << parent.hubiForm_text_input(this,  "<b>Minimum for left axis</b><small>(Blank for auto)</small>", "graph_vaxis_1_min", "", false);
            container << parent.hubiForm_text_input(this,  "<b>Maximum for left axis</b><small>(Blank for auto)</small>", "graph_vaxis_1_max", "", false);   
            container << parent.hubiForm_text_input (this, "<b>Num Vertical Gridlines</b><br><small>(Blank for auto)</small>", "graph_vaxis_1_num_lines", "", false);
            container << parent.hubiForm_switch     (this, "<b>Show Left Axis Label on Graph</b>", "graph_show_left_label", false, true);
            if (graph_show_left_label==true){
                container << parent.hubiForm_text_input (this, "<b>Input Left Axis Label</b>", "graph_left_label", "Left Axis Label", false);
                container << parent.hubiForm_font_size  (this, "Left Axis", "graph_left", 9, 2, 20);
                container << parent.hubiForm_color      (this, "Left Axis", "graph_left", "#FFFFFF", false);
            }
            parent.hubiForm_container(this, container, 1); 
        }

        //Right Axis   
        parent.hubiForm_section(this,"Right Axis", 1, "arrow_forward"){  
            container = [];
            container << parent.hubiForm_text_input(this,  "<b>Minimum for right axis</b><small>(Blank for auto)</small>", "graph_vaxis_2_min", "", false);
            container << parent.hubiForm_text_input(this,  "<b>Maximum for right axis</b><small>(Blank for auto)</small>", "graph_vaxis_2_max", "", false);   
            container << parent.hubiForm_text_input (this, "<b>Num Vertical Gridlines</b><br><small>(Blank for auto)</small>", "graph_vaxis_2_num_lines", "", false);
            container << parent.hubiForm_switch     (this, "<b>Show Right Axis Label on Graph</b>", "graph_show_right_label", false, true);
            if (graph_show_right_label==true){
                container << parent.hubiForm_text_input (this, "<b>Input right Axis Label</b>", "graph_right_label", "Right Axis Label", false);
                container << parent.hubiForm_font_size  (this, "Right Axis", "graph_right", 9, 2, 20);
                container << parent.hubiForm_color      (this, "Right Axis", "graph_right", "#FFFFFF", false);
            }
            parent.hubiForm_container(this, container, 1); 
        }

        //Legend
        parent.hubiForm_section(this,"Legend", 1){
            container = [];
            def legendPosition = [["top": "Top"], ["bottom":"Bottom"], ["in": "Inside Top"]];
            def insidePosition = [["start": "Left"], ["center": "Center"], ["end": "Right"]];
            container << parent.hubiForm_switch(this, "Show Legend on Graph", "graph_show_legend", false, true);
            if (graph_show_legend==true){
                container << parent.hubiForm_font_size  (this, "Legend", "graph_legend", 9, 2, 20);
                container << parent.hubiForm_color      (ths, "Legend", "graph_legend", "#000000", false);
                parent.hubiForm_container(this, container, 1); 
                input( type: "enum", name: "graph_legend_position", title: "<b>Legend Position</b>", defaultValue: "Bottom", options: legendPosition);
                input( type: "enum", name: "graph_legend_in side_position", title: "<b>Legend Justification</b>", defaultValue: "center", options: insidePosition);
            } else {
                 parent.hubiForm_container(this, container, 1); 
            }
           

        }
        state.num_devices = 0;
        sensors.each { sensor ->
                settings["attributes_${sensor.id}"].each { attribute ->
                        state.num_devices++;
                }
        }
        def availableAxis = [["0" : "Left Axis"], ["1": "Right Axis"]];
        if (state.num_devices == 1) {
                availableAxis = [["0" : "Left Axis"], ["1": "Right Axis"], ["2": "Both Axes"]]; 
        }
                
        //Line
        cnt = 1;

        sensors.each { sensor ->        
                settings["attributes_${sensor.id}"].each { attribute ->
                    
                    parent.hubiForm_section(this,"${sensor.displayName} - ${attribute}", 1){
                               
                        container = [];
                        input( type: "enum", name:   "graph_axis_number_${sensor.id}_${attribute}", title: "<b>Graph Axis Side</b>", defaultValue: "0", options: availableAxis);
                        container << parent.hubiForm_color(this,        "Line", 
                                                                                "graph_line_${sensor.id}_${attribute}", 
                                                                                parent.hubiTools_rotating_colors(cnt), 
                                                                                false);
                        container << parent.hubiForm_line_size  (this,  "Line Thickness",                   
                                                                                "attribute_${sensor.id}_${attribute}_current_border", 
                                                                                2, 1, 20);
                        container << parent.hubiForm_text_input(this,   "<b>Override Device Name</b><small></i><br>Use %deviceName% for DEVICE and %attributeName% for ATTRIBUTE</i></small>",
                                                                                "graph_name_override_${sensor.id}_${attribute}",
                                                                                "%deviceName%: %attributeName%", false);
                        
                        startVal = supportedTypes[attribute] ? supportedTypes[attribute].start : "";
                        endVal = supportedTypes[attribute] ? supportedTypes[attribute].end : "";
                                
                        if (graph_type == "Area Graph"){
                            container << parent.hubiForm_slider (this, title: "Opacity of the area below the line", 
                                                                       name:  "attribute_${sensor.id}_${attribute}_opacity",  
                                                                       default_value: 30, 
                                                                       min: 0,
                                                                       max: 100, 
                                                                       units: "%",
                                                                       submit_on_change: false);
                        }
                        
                                if (startVal != ""){
                                    app.updateSetting ("attribute_${sensor.id}_${attribute}_non_number", true);
                                    app.updateSetting ("attribute_${sensor.id}_${attribute}_startString", startVal);
                                    app.updateSetting ("attribute_${sensor.id}_${attribute}_endString", endVal);
                                    container << parent.hubiForm_text(this, "<b><mark>This Attribute ($attribute) is non-numerical, please choose values for the states below</mark></b>");
                                    container << parent.hubiForm_text_input(this, "Value for <mark>$startVal</mark>",
                                                                                  "attribute_${sensor.id}_${attribute}_${startVal}",
                                                                                  "100", false);
                                    
                                    container << parent.hubiForm_text_input(this, "Value for <mark>$endVal</mark>",
                                                                                  "attribute_${sensor.id}_${attribute}_${endVal}",
                                                                                  "0", false);
                                    
                                    parent.hubiForm_container(this, container, 1); 
                                    
                                } else {
                                    container << parent.hubiForm_switch(this, "Display as a Drop Line", "attribute_${sensor.id}_${attribute}_drop_line", false, true);
                                                              
                                    if (settings["attribute_${sensor.id}_${attribute}_drop_line"]==true){
                                        container << parent.hubiForm_text_input(this,"Value to drop the Line",
                                                                                     "attribute_${sensor.id}_${attribute}_drop_value",
                                                                                     "0", false);
                                        parent.hubiForm_container(this, container, 1);
                                       
                                        input( type: "enum", name: "attribute_${sensor.id}_${attribute}_drop_time", title: "Drop Line Time", defaultValue: "5 Minutes", options: timespanEnum2 )

                                    } else {
                                        parent.hubiForm_container(this, container, 1); 
                                    }
                                }
                                
                               
                                cnt += 1;
                        }
                }           
        }
       
        
    }
}

def deviceSelectionPage() {
    def supported_attrs;
       
    dynamicPage(name: "deviceSelectionPage") {
         parent.hubiForm_section(this,"Device Selection", 1){
             input "sensors", "capability.*", title: "Sensors", multiple: true, required: true, submitOnChange: true
        
            if (sensors){
                sensors.each {
                    sensor_events = it.events([max:250]).name;
                    supported_attrs = sensor_events.unique(false);           
                    container = [];
                    container <<  parent.hubiForm_sub_section(this, it.displayName);
                    parent.hubiForm_container(this, container, 1);     
                    input( type: "enum", name: "attributes_${it.id}", title: "Attributes to graph", required: true, multiple: true, options: supported_attrs, defaultValue: "1")
                }
            }
        }
    }
}

def disableAPIPage() {
    dynamicPage(name: "disableAPIPage") {
        section() {
            if (state.endpoint) {
                revokeAccessToken();
                state.endpoint = null
            }
            paragraph "Token revoked. Click done to continue."
        }
    }
}

def enableAPIPage() {
    dynamicPage(name: "enableAPIPage", title: "") {
        section() {
            if(!state.endpoint) initializeAppEndpoint();
            paragraph "Token created. Click done to continue."
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

/********************************************************************************************************************************
*********************************************************************************************************************************
****************************************** END PAGES ********************************************************************************
*********************************************************************************************************************************
*********************************************************************************************************************************/

def installed() {
    initialize()
}

def uninstalled() {
    if (state.endpoint) {
        try {
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
    
    if (install_device == true){
        parent.hubiTool_create_tile(this);
    }
    
}

def initialize() {
   updated();
}

private getValue(id, attr, val){
    def reg = ~/[a-z,A-Z]+/;
    if (settings["attribute_${id}_${attr}_${val}"]!=null){
        ret = Double.parseDouble(settings["attribute_${id}_${attr}_${val}"]);
    } else {
        ret = Double.parseDouble(val - reg )
    }
    return ret;
}

private buildData() {
    def resp = [:]
    def today = new Date();
    def then = new Date();
    
    use (groovy.time.TimeCategory) {
           then -= Integer.parseInt(graph_timespan).milliseconds;
    }
    
    if(sensors) {
        sensors.each { sensor ->
            resp[sensor.id] = [:];
            settings["attributes_${sensor.id}"].each { attribute ->
                def respEvents = [];    
                
                respEvents << sensor.statesSince(attribute, then, [max: 50000]).collect{[ date: it.date.getTime(), value: getValue(sensor.id, attribute, it.value)  ]}
                respEvents = respEvents.flatten();
                respEvents = respEvents.reverse();
                
                //Add drop lines for non-numerical devices
                if (settings["attribute_${sensor.id}_${attribute}_non_number"] && respEvents.size()>1){
                    start = settings["attribute_${sensor.id}_${attribute}_startString"];
                    end = settings["attribute_${sensor.id}_${attribute}_endString"];
                    startVal = Float.parseFloat(settings["attribute_${sensor.id}_${attribute}_${start}"]);
                    endVal = Float.parseFloat(settings["attribute_${sensor.id}_${attribute}_${end}"]);
                    tEvents = [];
                    //Add Start Event
                    currDate = then;
                    if (respEvents[0].value == startVal){  
                          tEvents.push([date: currDate, value: endVal]);  
                    } else {
                          tEvents.push([date: currDate, value: startVal]);
                    }
                    for (i=0; i<respEvents.size(); i++){
                        currDate = respEvents[i].date;
                        if (respEvents[i].value == startVal){         
                            tEvents.push([date: currDate-1000, value: endVal]);
                            
                        } else {
                            tEvents.push([date: currDate-1000, value: startVal]);
                        }
                        tEvents.push(respEvents[i]);
                    }
                    respEvents = tEvents;
                }
                
                //graph_max_points
                if (graph_max_points > 0) {
                    reduction = (int) Math.ceil((float)respEvents.size() / graph_max_points);
                    respEvents = respEvents.collate(reduction).collect{ group -> 
                        group.inject([ date: 0, value: 0 ]){ col, it -> 
                            col.date += it.date / group.size();
                            col.value += it.value / group.size();
                            return col;
                        } 
                    };                             
                }                    
                
                //add drop line data
                tEvents = [];
                if (settings["attribute_${sensor.id}_${attribute}_drop_line"] && respEvents.size()>1){
                    def curr, prev, currDate, prevDate, drop_time, drop_value;
                    
                    drop_time = settings["attribute_${sensor.id}_${attribute}_drop_time"];
                    drop_value = settings["attribute_${sensor.id}_${attribute}_drop_value"];
                    tEvents.push(respEvents[0]);
                    for (i=1; i<respEvents.size(); i++){
                        curr = respEvents[i];
                        prev = respEvents[i-1];
                        currDate = curr.date;
                        prevDate = prev.date;
                        
                        if ((currDate - prevDate) > Integer.parseInt(drop_time)){
                              //add first zero
                              tEvents.push([date: prevDate-1000, value: Float.parseFloat(drop_value)]);
                              tEvents.push([date: currDate+1000, value: Float.parseFloat(drop_value)]);
                        }
                        tEvents.push(curr);
                     }
                     respEvents = tEvents;
                }         
                resp[sensor.id][attribute] = respEvents;
            }
        }
    }
    
    return resp;
}

def getChartOptions(){
    
    def options = [
        "graphReduction": graph_max_points,
        "graphTimespan": Integer.parseInt(graph_timespan),
        "graphUpdateRate": Integer.parseInt(graph_update_rate),
        "graphOptions": [
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size: "100%",
            "chartArea": [ "width": graph_static_size ? graph_h_size : "80%", "height": graph_static_size ? graph_v_size: "80%"],
            "hAxis": ["textStyle": ["fontSize": graph_haxis_font, 
                                    "color": graph_hh_color_transparent ? "transparent" : graph_hh_color ], 
                      "gridlines": ["color": graph_ha_color_transparent ? "transparent" : graph_ha_color, 
                                    "count": graph_h_num_grid != "" ? graph_h_num_grid : null
                                   ],
                      "format":     graph_h_format==""?"":graph_h_format                          
                     ],
            "vAxis": ["textStyle": ["fontSize": graph_vaxis_font, 
                                    "color": graph_vh_color_transparent ? "transparent" : graph_vh_color], 
                      "gridLines": ["color": graph_va_color_transparent ? "transparent" : graph_va_color],
                     ],
            "vAxes": [
                0: ["title" : graph_show_left_label ? graph_left_label: null,  
                    "titleTextStyle": ["color": graph_left_color_transparent ? "transparent" : graph_left_color, "fontSize": graph_left_font],
                    "viewWindow": ["min": graph_vaxis_1_min != "" ?  graph_vaxis_1_min : null, 
                                   "max":  graph_vaxis_1_max != "" ?  graph_vaxis_1_max : null],
                    "gridlines": ["count" : graph_vaxis_1_num_tics != "" ? graph_vaxis_1_num_tics : null ],
                    "minorGridlines": ["count" : 0]
                   ],
                
                1: ["title": graph_show_right_label ? graph_right_label : null,
                    "titleTextStyle": ["color": graph_right_color_transparent ? "transparent" : graph_right_color, "fontSize": graph_right_font],
                    "viewWindow": ["min": graph_vaxis_2_min != "" ?  graph_vaxis_2_min : null, 
                                   "max":  graph_vaxis_2_max != "" ?  graph_vaxis_2_max : null],
                    "gridlines": ["count" : graph_vaxis_2_num_tics != "" ? graph_vaxis_2_num_tics : null ],
                    "minorGridlines": ["count" : 0]
                    ]
                
            ],
            "legend": !graph_show_legend ? ["position": "none"] : ["position": graph_legend_position,  
                                                                   "alignment": graph_legend_inside_position, 
                                                                   "textStyle": ["fontSize": graph_legend_font, 
                                                                                 "color": graph_legend_color_transparent ? "transparent" : graph_legend_color]],
            "backgroundColor": graph_background_color_transparent ? "transparent" : graph_background_color,
            "curveType": !graph_smoothing ? "" : "function",
            "title": !graph_show_title ? "" : graph_title,
            "titleTextStyle": !graph_show_title ? "" : ["fontSize": graph_title_font, "color": graph_title_color_transparent ? "transparent" : graph_title_color],
            "titlePosition" :  graph_title_inside ? "in" : "out",
            "interpolateNulls": true, //for null vals on our chart
            "orientation" : graph_y_orientation == true ? "vertical" : "horizontal",
            "reverseCategories" : graph_x_orientation,
            "series": [],
            
        ]
    ];
    
    //add colors and thicknesses
    sensors.each { sensor ->
        settings["attributes_${sensor.id}"].each { attribute ->
            def axis = Integer.parseInt(settings["graph_axis_number_${sensor.id}_${attribute}"]);
            def text_color = settings["graph_line_${sensor.id}_${attribute}_color"];
            def text_color_transparent = settings["graph_line_${sensor.id}_${attribute}_color_transparent"];
            def line_thickness = settings["graph_line_thickness_${sensor.id}_${attribute}"];
            if (settings["attribute_${sensor.id}_${attribute}_opacity"]){
                   def opacity = settings["attribute_${sensor.id}_${attribute}_opacity"]/100.0;
            }
            
            log.debug("Opacity: $opacity");
            def annotations = [
                "targetAxisIndex": axis, 
                "color": text_color_transparent ? "transparent" : text_color,
                "stroke": text_color_transparent ? "transparent" : "red",
                "lineWidth": line_thickness,
                "areaOpacity" : opacity,
                
            ];
            
            options.graphOptions.series << annotations;  
        }
    }    
    
    return options;
}
        
def getDrawType(){
    switch (graph_type){
        case "Line Graph": return "google.visualization.LineChart";
        case "Area Graph": return "google.visualization.AreaChart";
        case "Scatter Plot": return "google.visualization.ScatterChart";
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
    <link rel='icon' href='https://www.shareicon.net/data/256x256/2015/09/07/97252_barometer_512x512.png' type='image/x-icon'/> 
    <link rel="apple-touch-icon" href="https://www.shareicon.net/data/256x256/2015/09/07/97252_barometer_512x512.png">
    <head>
      <script src="https://code.jquery.com/jquery-3.5.0.min.js" integrity="sha256-xNzN2a4ltkB44Mc/Jz3pT4iU1cmeR0FkXs4pru/JxaQ=" crossorigin="anonymous"></script>
      <script src="https://cdnjs.cloudflare.com/ajax/libs/svg.js/3.0.16/svg.min.js" integrity="sha256-MCvBrhCuX8GNt0gmv06kZ4jGIi1R2QNaSkadjRzinFs=" crossorigin="anonymous"></script>
      <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.0/moment.min.js" integrity="sha256-imB/oMaNA0YvIkDkF5mINRWpuFPEGVCEkHy6rm2lAzA=" crossorigin="anonymous"></script>
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
    const now = new Date().getTime();
    let deviceId = event.deviceId;

    //only accept relevent events
    if(subscriptions.ids.includes(deviceId) && subscriptions.attributes[deviceId].includes(event.name)) {
        let value = event.value;
        let attribute = event.name;

        non_num = subscriptions.non_num[deviceId][attribute];
         
        if (non_num.valid){
            if (value == non_num.start){
                graphData[deviceId][attribute].push({ date: now-1000, value: non_num.endVal});
                graphData[deviceId][attribute].push({ date: now, value: non_num.startVal});
            } else if (value == non_num.end){
                graphData[deviceId][attribute].push({ date: now-1000, value: non_num.startVal});
                graphData[deviceId][attribute].push({ date: now, value: non_num.endVal});
            } 
        } else {
            stack[deviceId][attribute].push({ date: now, value: value });
              
            //check the stack
            const graphEvents = graphData[deviceId][attribute];
            const stackEvents = stack[deviceId][attribute];
            const span = graphEvents[1].date - graphEvents[0].date;
        
            if(stackEvents[stackEvents.length - 1].date - graphEvents[graphEvents.length - 1].date >= span 
               || (stackEvents.length > 1 
               && stackEvents[stackEvents.length - 1].date - stackEvents[0].date >= span)) {
                
               //push the stack
               graphData[deviceId][attribute].push(stack[deviceId][attribute].reduce((accum, it) =>  accum = { date: accum.date + it.date / stackEvents.length, value: accum.value + it.value / stackEvents.length }, { date: 0, value: 0.0 }));
               stack[deviceId][attribute] = [];

                //check for drop
                const thisDrop = subscriptions.drop[deviceId][attribute];
                const thisEvents = graphData[deviceId][attribute];
                if(thisDrop.valid && thisEvents[thisEvents.length - 2].date - thisEvents[thisEvents.length - 1].date > thisDrop.time) {
                    graphData[deviceId][attribute].splice(thisEvents.length - 2, 0, { date: thisEvents[thisEvents.length - 2].date + 1000, value: thisDrop.value });
                    graphData[deviceId][attribute].splice(thisEvents.length - 2, 0, { date: thisEvents[thisEvents.length - 1].date - 1000, value: thisDrop.value });
                }
            }
        }

        //update if we are realtime
        if(options.graphUpdateRate === 0) update();
    }
}

function update(callback) {
    //boot old data
    let min = new Date().getTime();
    min -= options.graphTimespan;

    Object.entries(graphData).forEach(([deviceId, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {
            //filter old events
            graphData[deviceId][attribute] = events.filter(it => it.date > min);
        });
    });

    
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

    //create stack
    Object.entries(graphData).forEach(([deviceId, attrs]) => {
        stack[deviceId] = {};
        Object.keys(attrs).forEach(attr => {
            stack[deviceId][attr] = [];
        });
    })

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

    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn({ label: 'Date', type: 'datetime' });

    let colNums = {};

    let i = 0;
    subscriptions.ids.forEach((deviceId) => {
        colNums[deviceId] = {};
        subscriptions.attributes[deviceId].forEach((attr) => {
            
            dataTable.addColumn({ label: subscriptions.labels[deviceId][attr].replace('%deviceName%', subscriptions.sensors[deviceId].displayName).replace('%attributeName%', attr), type: 'number' });
            colNums[deviceId][attr] = i++;
        });
    });

    const totalCols = i;

    let parsedGraphData = [];
    //map the graph data
    Object.entries(graphData).forEach(([deviceIndex, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {
            non_num = subscriptions.non_num[deviceIndex][attribute];
            var length = events.length;
            events.forEach((event) => {
              
              //Make a new entry
              let newEntry = Array.apply(null, new Array(totalCols + 1));
              newEntry[0] = event.date;
              newEntry[colNums[deviceIndex][attribute] + 1] = event.value;
              parsedGraphData.push(newEntry);
              
            });
           
        });
    });

    //map the stack
    Object.entries(stack).forEach(([deviceIndex, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {
            if(events.length > 0) {
                const event = events.reduce((accum, it) =>  accum = { date: accum.date, value: accum.value + it.value / events.length }, { date: now, value: 0.0 });

                let newEntry = Array.apply(null, new Array(totalCols + 1));
                newEntry[0] = event.date;
                newEntry[colNums[deviceIndex][attribute] + 1] = event.value;
                parsedGraphData.push(newEntry);
            }
            
        });
    });

    parsedGraphData = parsedGraphData.map((it) => [ moment(it[0]).toDate(), ...it.slice(1).map((it) => parseFloat(it)) ]);

    parsedGraphData.forEach(it => {
        dataTable.addRow(it);
    });

    
    
    let graphOptions = Object.assign({}, options.graphOptions);

    graphOptions.hAxis = Object.assign(graphOptions.hAxis, { viewWindow: { min: moment(min).toDate(), max: moment(now).toDate() } });

    let chart = new ${drawType}(document.getElementById("timeline"));

    //if we have a callback
    if(callback) google.visualization.events.addListener(chart, 'ready', callback);

    chart.draw(dataTable, graphOptions);
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

//oauth endpoints
def getGraph() {
    render(contentType: "text/html", data: getLineGraph());      
}

def getDataMetrics() {
    def data;
    def then = new Date().getTime();
    data = getData();
    def now = new Date().getTime();
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
    def ids = [];
    def sensors_ = [:];
    def attributes = [:];
    def labels = [:];
    def drop_ = [:];
    def non_num_ = [:];
    sensors.each {sensor->
        ids << sensor.idAsLong;
        
        //only take what we need
        sensors_[sensor.id] = [ id: sensor.id, idAsLong: sensor.idAsLong, displayName: sensor.displayName ];        
        attributes[sensor.id] = settings["attributes_${sensor.id}"];
        
        labels[sensor.id] = [:];
        settings["attributes_${sensor.id}"].each { attr ->
            labels[sensor.id][attr] = settings["graph_name_override_${sensor.id}_${attr}"];
        }
        
        drop_[sensor.id] = [:];
        non_num_[sensor.id] = [:];
        settings["attributes_${sensor.id}"].each { attr ->
            if (settings["attribute_${sensor.id}_${attr}_non_number"]==true){
                startString = settings["attribute_${sensor.id}_${attr}_startString"];
                endString = settings["attribute_${sensor.id}_${attr}_endString"];
                non_num_[sensor.id][attr] = [ valid: true,
                                              start:       startString,
                                              startVal:    settings["attribute_${sensor.id}_${attr}_${startString}"],
                                              end:         endString,
                                              endVal:      settings["attribute_${sensor.id}_${attr}_${endString}"]
                                            ];  
            } else {
                non_num_[sensor.id][attr] = [ valid: false,
                                              start: "",
                                              end:   ""]; 
            }
                                   
            drop_[sensor.id][attr] = [valid: settings["attribute_${sensor.id}_${attr}_drop_line"],
                                      time:  settings["attribute_${sensor.id}_${attr}_drop_time"],
                                      value: settings["attribute_${sensor.id}_${attr}_drop_value"]];
        }
        
    }
    
    def obj = [
        ids: ids,
        sensors: sensors_,
        attributes: attributes, 
        labels : labels,
        drop : drop_,
        non_num: non_num_
    ]
    
    def subscriptions = obj;
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
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
