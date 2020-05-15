import groovy.json.*

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
// v0.3 Loading Update; Removed ALL processing from Hub, uses websocket endpoint
// v0.5 Multiple line support
// v0.51 Select ANY device
// v0.60 Select AXIS to graph on
// v0.70 A lot more options
// v0.80 Added Horizontal Axis Formatting
    
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
	    
            input "sensors", "capability.*", title: "Sensors", multiple: true, required: true, submitOnChange: true
        
            if (sensors){
                sensors.each {
                    sensor_events = it.events([max:250]).name;
                    supported_attrs = sensor_events.unique(false);           
                    paragraph(it.displayName);
                    input( type: "enum", name: "attributes_${it.id}", title: "Attributes to graph", required: true, multiple: true, options: supported_attrs, defaultValue: "1")
                }
            }
        }
    }
}

def graphSetupPage(){
    def fontEnum = [["1":"1"], ["2":"2"], ["3":"3"], ["4":"4"], ["5":"5"], ["6":"6"], ["7":"7"], ["8":"8"], ["9":"9"], ["10":"10"], 
                    ["11":"11"], ["12":"12"], ["13":"13"], ["14":"14"], ["15":"15"], ["16":"16"], ["17":"17"], ["18":"18"], ["19":"19"], ["20":"20"]];  
    
    def colorEnum = [["#800000":"Maroon"], ["#FF0000":"Red"], ["#FF0000":"Orange"], ["#FFFF00":"Yellow"], ["#808000":"Olive"], ["#008000":"Green"],
                    ["#800080":"Purple"], ["#FF00FF":"Fuchsia"], ["#00FF00":"Lime"], ["#008080":"Teal"], ["#00FFFF":"Aqua"], ["#0000FF":"Blue"], ["#000080":"Navy"],
                    ["#000000":"Black"], ["#C0C0C0":"Gray"], ["#C0C0C0":"Silver"], ["#FFFFFF":"White"], ["rgba(255, 255, 255, 0)":"Transparent"]];
    
    dynamicPage(name: "graphSetupPage") {
        section(){
            paragraph getTitle("General Options");
            input( type: "enum", name: "graph_update_rate", title: "Select graph update rate", multiple: false, required: true, options: [["-1":"Never"], ["0":"Real Time"], ["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]], defaultValue: "0")
            input( type: "enum", name: "graph_timespan", title: "Select Timespan to Graph", multiple: false, required: true, options: [["60000":"1 Minute"], ["3600000":"1 Hour"], ["43200000":"12 Hours"], ["86400000":"1 Day"], ["259200000":"3 Days"], ["604800000":"1 Week"]], defaultValue: "43200000")
            input( type: "enum", name: "graph_background_color", title: "Background Color", defaultValue: "White", options: colorEnum);
            input( type: "bool", name: "graph_smoothing", title: "Smooth Graph Points", defaultValue: true);
            input( type: "enum", name: "graph_type", title: "Graph Type", defaultValue: "Line Graph", options: ["Line Graph", "Area Graph"] )
            input( type: "bool", name: "graph_y_orientation", title: "Flip Graph to Vertical (Rotate 90 degrees)", defaultValue: false);
            input( type: "bool", name: "graph_z_orientation", title: "Reverse Data Order? (Flip Data left to Right)", defaultValue: false);            
            //Title
           
            paragraph getTitle("Title");
            input( type: "bool", name: "graph_show_title", title: "Show Title on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_title==true) {
                input( type: "text", name: "graph_title", title: "Input Graph Title", default: "Graph Title");
                input( type: "enum", name: "graph_title_font", title: "Graph Title Font", defaultValue: "9", options: fontEnum); 
                input( type: "enum", name: "graph_title_color", title: "Graph Title Color", defaultValue: "Black", options: colorEnum); 
                input( type: "bool", name: "graph_title_inside", title: "Put Title Inside Graph", defaultValue: false);
            }
            
            //Size
            paragraph getTitle("Graph Size");
            input( type: "bool", name: "graph_static_size", title: "Set size of Graph? (False = Fill Window)", defaultValue: false, submitOnChange: true);
            if (graph_static_size==true){
                input( type: "number", name: "graph_h_size", title: "Horizontal dimension of the graph", defaultValue: "800", range: "100..3000");
                input( type: "number", name: "graph_v_size", title: "Vertical dimension of the graph", defaultValue: "600", range: "100..3000");
            }
            
            //Axis
            paragraph getTitle("Horizontal Axis");
            input( type: "enum", name: "graph_haxis_font", title: "Horizonal Axis Font Size", defaultValue: "9", options: fontEnum); 
            input( type: "enum", name: "graph_hh_color", title: "Horizonal Header Color", defaultValue: "Black", options: colorEnum);
            input( type: "enum", name: "graph_ha_color", title: "Horizonal Axis Color", defaultValue: "Gray", options: colorEnum);
            input( type: "number", name: "graph_h_num_grid", title: "Num Horizontal Gridlines (blank for auto)", defaultValue: "", range: "0..100");
            
            input( type: "bool", name: "dummy", title: "Show Sring Formatting Help", defaultValue: false, submitOnChange: true);
            if (dummy == true){
                paragraph_ = "<table>"
                paragraph_ += getTableRow("<b>Name", "<b>Format" ,"<b>Result</b>", "");
                paragraph_ += getTableRow("Year", "Y", "2020", "");
                paragraph_ += getTableRow("Month Number", "M", "12", "");
                paragraph_ += getTableRow("Month Name ", "MMM", "Feb", "");
                paragraph_ += getTableRow("Month Full Name", "MMMM", "February", "");
                paragraph_ += getTableRow("Day of Month", "d", "February", "");
                paragraph_ += getTableRow("Day of Week", "EEE", "Mon", "");
                paragraph_ += getTableRow("Day of Week", "EEEE", "Monday", "");
                paragraph_ += getTableRow("Period", "a", "AM or PM", "");
                paragraph_ += getTableRow("Hour (12)", "h", "1..12", "");
                paragraph_ += getTableRow("Hour (12)", "hh", "01..12", "");
                paragraph_ += getTableRow("Hour (24)", "H", "1..23", "");
                paragraph_ += getTableRow("Hour (24)", "HH", "01..23", "");
                paragraph_ += getTableRow("Minute", "m", "1..59", "");
                paragraph_ += getTableRow("Minute", "mm", "01..59", "");
                paragraph_ += getTableRow("Seconds", "s", "1..59", "");
                paragraph_ += getTableRow("Seconds", "ss", "01..59", "");
                paragraph_ += "</table>"
                paragraph paragraph_
                paragraph_ = /<b>Example: "EEEE, MMM d, Y hh:mm:ss a" = "Monday, June 2, 2020 08:21:33 AM"/
                paragraph_ += "</b>"
                paragraph paragraph_
            }
            input( type: "string", name: "graph_h_format", title: "Horizontal Axis Format", defaultValue: "", submitOnChange: true);
            if (graph_h_format != ""){
                today = new Date();
                paragraph "Horizontal Axis Sample: ${today.format(graph_h_format)}"
            }
            
            paragraph getTitle("Vertical Axis");
            input( type: "enum", name: "graph_vaxis_font", title: "Vertical Font Size", defaultValue: "9", options: fontEnum); 
            input( type: "enum", name: "graph_vh_color", title: "Vertical Header Color", defaultValue: "Black", options: colorEnum);
            input( type: "enum", name: "graph_va_color", title: "Vertical Axis Color", defaultValue: "Gray", options: colorEnum);
            
            paragraph getTitle("Left Axis");
            input( type: "number", name: "graph_vaxis_1_min", title: "Minimum for left axis (blank for auto)", defaultValue: "", range: "");
            input( type: "number", name: "graph_vaxis_1_max", title: "Maximum for left axis (blank for auto)", defaultValue: "", range: "");
            input( type: "number", name: "graph_vaxis_1_num_lines", title: "Num gridlines (blank for auto)", defaultValue: "", range: "0..100");
            input( type: "bool", name: "graph_show_left_label", title: "Show Left Axis Label on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_left_label==true){
                input( type: "text", name: "graph_left_label", title: "Input Left Axis Label", default: "Left Axis Label");
                input( type: "enum", name: "graph_left_font", title: "Left Axis Font Size", defaultValue: "9", options: fontEnum); 
                input( type: "enum", name: "graph_left_color", title: "Left Axis Color", defaultValue: "White", options: colorEnum);
            }
            
            paragraph getTitle("Right Axis");
            input( type: "number", name: "graph_vaxis_2_min", title: "Minimum for right axis (blank for auto)", defaultValue: "", range: "");
            input( type: "number", name: "graph_vaxis_2_max", title: "Maximum for right axis (blank for auto)", defaultValue: "", range: "");
            input( type: "number", name: "graph_vaxis_2_num_lines", title: "Num gridlines (blank for auto) -- Must be greater than num tics to be effective", defaultValue: "", range: "0..100");
            input( type: "bool", name: "graph_show_right_label", title: "Show Right Axis Label on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_right_label==true){
                input( type: "text", name: "graph_right_label", title: "Input Right Axis Label", default: "Right Axis Label");
                input( type: "enum", name: "graph_right_font", title: "Right Axis Font Size", defaultValue: "9", options: fontEnum); 
                input( type: "enum", name: "graph_right_color", title: "Right Axis Color", defaultValue: "White", options: colorEnum);
             }
            
            //Legend
            def legendPosition = [["top": "Top"], ["bottom":"Bottom"], ["in": "Inside Top"]];
            def insidePosition = [["start": "Left"], ["center": "Center"], ["end": "Right"]];
            paragraph getTitle("Legend");
            input( type: "bool", name: "graph_show_legend", title: "Show Legend on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_legend==true){
                input( type: "enum", name: "graph_legend_font", title: "Legend Font Size", defaultValue: "9", options: fontEnum); 
                input( type: "enum", name: "graph_legend_color", title: "Legends Color", defaultValue: "Black", options: colorEnum);
                input( type: "enum", name: "graph_legend_position", title: "Legend Position", defaultValue: "Bottom", options: legendPosition);
                input( type: "enum", name: "graph_legend_inside_position", title: "Legend Justification", defaultValue: "center", options: insidePosition);
                
            }
            
            //Get the total number of devices
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
            sensors.each { sensor ->        
                settings["attributes_${sensor.id}"].each { attribute ->
                    paragraph getTitle("Line for ${sensor.displayName}: ${attribute}");
                    input( type: "enum", name: "graph_axis_number_${sensor.id}_${attribute}", title: "Graph Axis Number", defaultValue: "0", options: availableAxis);
                    input( type: "enum", name: "graph_line_color_${sensor.id}_${attribute}", title: "Line Color", defaultValue: "Blue", options: colorEnum); 
                    input( type: "enum", name: "graph_line_thickness_${sensor.id}_${attribute}", title: "Line Thickness", defaultValue: "2", options: fontEnum); 
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

def getColorString(string_){
    switch (string_){
        case "#800000": return "Maroon";
        case "#FF0000": return "Red";
        case "#FF0000": return "Orange";
        case "#FFFF00": return "Yellow";
        case "#808000": return "Olive";
        case "#008000": return "Green";
        case "#800080": return "Purple";
        case "#FF00FF": return "Fuchsia";
        case "#00FF00": return "Lime";
        case "#008080": return "Teal";
        case "#00FFFF": return "Aqua";
        case "#0000FF": return "Blue";
        case "#000080": return "Navy";
        case "#000000": return "Black";
        case "#C0C0C0": return "Gray";
        case "#C0C0C0": return "Silver";
        case "#FFFFFF": return "White";
        case "rgba(255, 255, 255, 0)": return "Transparent";   
        
    }
    
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
                if (sensors){
                    def paragraph_ = /${getLine()}/
                    paragraph_ +=  "<table>"
                    paragraph_ +=   "${getTableRow2("<b><u>DEVICE</b></u>", "<b><u>ATTRIBUTES</b></u>", "<b><u>LINE COLOR</b></u>", "<b><u>LINE WIDTH</b></u>", "<b><u>AXIS</b></u>")}"
                    sensors.each { sensor ->
                        settings["attributes_${sensor.id}"].each { attribute_ ->
                            text_color = settings["graph_line_color_${sensor.id}_${attribute_}"];
                            line_thickness = settings["graph_line_thickness_${sensor.id}_${attribute_}"];
                            switch (settings["graph_axis_number_${sensor.id}_${attribute_}"]){
                                case "0": axis_num = "LEFT"; break;
                                case "1": axis_num = "RIGHT"; break;
                                case "2": axis_num = "BOTH"; break; 
                            }
                            paragraph_ += /${getTableRow2("$sensor", "$attribute_", "${getColorString(text_color)}", "$line_thickness", "$axis_num")}/
                           
                        }
                      
                    }
                    paragraph_ += "</table>"
                    paragraph_ += /${getLine()}/
                    paragraph paragraph_   
                }
                
                
                if (graph_update_rate){
                    def timeString = getTimsSpanString(graph_timespan);
                    def rateString = getTimeString(graph_update_rate);
                    
                    paragraph_ =  "<table>"
                    
                    
                    paragraph_ += "${getTableRow("<b><u>GRAPH SELECTIONS</b></u>", "<b><u>VALUE</b></u>", "<b><u>SIZE</b></u>", "<b><u>COLOR</b></u>")}"
                    if (graph_show_title==true) {
                        paragraph_ += /${getTableRow("Title", graph_title, graph_title_font, getColorString(graph_title_color))}/
                    } else {
                        paragraph_ += /${getTableRow("Title", "NOT SHOWN", "", "")}/
                    }
                    
                    paragraph_ += /${getTableRow("Horizontal Header", "", graph_haxis_font, getColorString(graph_hh_color))}/
                    paragraph_ += /${getTableRow("Vertical Header", "", graph_vaxis_font, getColorString(graph_vh_color))}/
                    paragraph_ += /${getTableRow("Horizontal Axis", "", "", getColorString(graph_ha_color))}/
                    paragraph_ += /${getTableRow("Vertical Axis", "", "", getColorString(graph_va_color))}/
                    
                    if (graph_show_legend==true){
                        paragraph_ += /${getTableRow("Legend", "", graph_legend_font, getColorString(graph_legend_color))}/
                    } else {
                        paragraph_ += /${getTableRow("Legend", "NOT SHOWN", "", "")}/
                    }
    
                    paragraph_ += /${getTableRow("Background", "", "", getColorString(graph_background_color))}/
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

def getTableRow2(col1, col2, col3, col4, col5){
     def html = "<tr><td width='30%'>$col1</td><td width='30%'>$col2</td><td width='20%'>$col3</td><td width='20%'>$col4</td><td width='20%'>$col5</td></tr>"  
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
    def resp = [:]
    def today = new Date();
    def then = new Date();
    
    use (groovy.time.TimeCategory) {
           then -= Integer.parseInt(graph_timespan).milliseconds;
    }
    
    log.debug("Initializing:: Device = $sensor  Attribute = $attribute from $then to $today");
    
    if(sensors) {
        sensors.each { sensor ->
            resp[sensor.id] = [:];
            settings["attributes_${sensor.id}"].each { attribute ->
                def respEvents = [];    
                
                log.debug("Checking:: $sensor.displayName: $attribute id:$sensor.id");
                respEvents << sensor.eventsBetween(then, today, [max: 50000]).findAll{"${it.name}" == attribute}.collect{[ date: it.date, value: it.value ]}
                respEvents = respEvents.sort{ it.date };
                respEvents = respEvents.flatten();
                respEvents = respEvents.reverse();
                
                //convert the date to ms
                respEvents = respEvents.collect{ [date: it.date.getTime(), value: it.value] }
                
                resp[sensor.id][attribute] = respEvents;
            }
        }
    }
    //log.debug("Resp: $resp");  
    
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
            "hAxis": ["textStyle": ["fontSize": graph_haxis_font, "color": graph_hh_color], 
                      "gridlines": ["color": graph_ha_color, 
                                    "count": graph_h_num_grid != "" ? graph_h_num_grid : null
                                   ],
                      "format":     graph_h_format==""?"":graph_h_format                          
                     ],
            "vAxis": ["textStyle": ["fontSize": graph_vaxis_font, 
                                    "color": graph_vh_color], 
                      "gridLines": ["color": graph_va_color],
                     ],
            "vAxes": [
                0: ["title" : graph_show_left_label ? graph_left_label: null,  
                    "titleTextStyle": ["color": graph_left_color, "fontSize": graph_left_font],
                    "viewWindow": ["min": graph_vaxis_1_min != "" ?  graph_vaxis_1_min : null, 
                                   "max":  graph_vaxis_1_max != "" ?  graph_vaxis_1_max : null],
                    "gridlines": ["count" : graph_vaxis_1_num_tics != "" ? graph_vaxis_1_num_tics : null ],
                    "minorGridlines": ["count" : 0]
                   ],
                
                1: ["title": graph_show_right_label ? graph_right_label : null,
                    "titleTextStyle": ["color": graph_right_color, "fontSize": graph_right_font],
                    "viewWindow": ["min": graph_vaxis_2_min != "" ?  graph_vaxis_2_min : null, 
                                   "max":  graph_vaxis_2_max != "" ?  graph_vaxis_2_max : null],
                    "gridlines": ["count" : graph_vaxis_2_num_tics != "" ? graph_vaxis_2_num_tics : null ],
                    "minorGridlines": ["count" : 0]
                    ]
                
            ],
            "legend": !graph_show_legend ? ["position": "none"] : ["position": graph_legend_position,  
                                                                   "alignment": graph_legend_inside_position, 
                                                                   "textStyle": ["fontSize": graph_legend_font, 
                                                                                 "color": graph_legend_color]],
            "backgroundColor": graph_background_color,
            "curveType": !graph_smoothing ? "" : "function",
            "title": !graph_show_title ? "" : graph_title,
            "titleTextStyle": !graph_show_title ? "" : ["fontSize": graph_title_font, "color": graph_title_color],
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
            def text_color = settings["graph_line_color_${sensor.id}_${attribute}"];
            def line_thickness = settings["graph_line_thickness_${sensor.id}_${attribute}"];
            
            def annotations = [
                "targetAxisIndex": axis, 
                "color": text_color,
                "lineWidth": line_thickness
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
        case "Combo Graph": return "google.visualization.ComboChart";
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
    const now = new Date().getTime();
    let deviceId = event.deviceId;

    //only accept relevent events
    if(subscriptions.ids.includes(deviceId) && subscriptions.attributes[deviceId].includes(event.name)) {
        let value = event.value;
        let attribute = event.name;

        graphData[deviceId][attribute].push({ date: now, value })

        //update if we are realtime
        if(options.graphUpdateRate === 0) update();
    }
}

function update() {
    //boot old data
    let min = new Date().getTime();
    min -= options.graphTimespan;

    Object.entries(graphData).forEach(([deviceId, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {
            //filter old events
            graphData[deviceId][attribute] = events.filter(it => it.date > min);
        });
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

function onBeforeUnload() {
    if(websocket) websocket.close();
}

function drawChart() {
    let now = new Date().getTime();
    let min = now - options.graphTimespan;

    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn({ label: 'Date', type: 'datetime' });

    let colNums = {};

    let i = 0;
    subscriptions.ids.forEach((deviceId) => {
        colNums[deviceId] = {};
        subscriptions.attributes[deviceId].forEach((attr) => {
            dataTable.addColumn({ label: subscriptions.sensors[deviceId].displayName + ": " + attr, type: 'number' });
            colNums[deviceId][attr] = i++;
        });
    });

    const totalCols = i;

    let parsedGraphData = [];
    //map the graph data
    Object.entries(graphData).forEach(([deviceIndex, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {
            events.forEach((event) => {
                //try to find an already existing date
                //let index = parsedGraphData.findIndex((val) => val[0] === event.date);
                //if(index !== -1) parsedGraphData[index][colNums[deviceIndex][attribute] + 1] = event.value;
                //else {
                    //else make a new entry
                    let newEntry = Array.apply(null, new Array(totalCols + 1));
                    newEntry[0] = event.date;
                    newEntry[colNums[deviceIndex][attribute] + 1] = event.value;
                    parsedGraphData.push(newEntry);
                //}
            });
            
        });
    });

    parsedGraphData = parsedGraphData.map((it) => [ moment(it[0]).toDate(), ...it.slice(1).map((it) => parseFloat(it)) ]);

    parsedGraphData.forEach(it => {
        dataTable.addRow(it);
    });
    let chart = new ${drawType}(document.getElementById("timeline"));

    let graphOptions = Object.assign({}, options.graphOptions);

    graphOptions.hAxis = Object.assign(graphOptions.hAxis, { viewWindow: { min: moment(min).toDate(), max: moment(now).toDate() } });

    chart.draw(dataTable, graphOptions);
}

google.charts.setOnLoadCallback(onLoad);
window.onBeforeUnload = onBeforeUnload;
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
    def ids = [];
    def sensorsM = [:];
    def attributes = [:];
    sensors.each {
        ids << it.idAsLong;
        
        //only take what we need
        sensorsM[it.id] = [ id: it.id, idAsLong: it.idAsLong, displayName: it.displayName ];
        
        attributes[it.id] = settings["attributes_${it.id}"];
    }
    
    def obj = [
        ids: ids,
        sensors: sensorsM,
        attributes: attributes
    ]
    
    def subscriptions = obj;
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
}
