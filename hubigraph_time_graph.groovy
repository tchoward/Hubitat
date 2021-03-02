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
// V 1.8 Smoother sliders, bug fixes
// V 2.0 New Version to Support Combo Graphs.  Support for Line Graphs is ended.    
// V 2.1 Long Term Storage Enabled
// V 4.6 Added finer control for timespan, resize graph sizes, bug fixes

// Credit to Alden Howard for optimizing the code.

 
def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v1.0" }

definition(
    name: "Hubigraph Time Graph",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Time Graph",
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
       page(name: "longTermStoragePage", nextPage: "mainPage");
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

def getEvents(sensor, attribute, num){
     def resp = [:]
     def today = new Date();
     def then = new Date();
    
     use (groovy.time.TimeCategory) {
           then -= 2.days;
     }
     
  
     def respEvents = [];                  
     respEvents << sensor.statesSince(attribute, then, [max: 200]){ it.value };
     respEvents = respEvents.flatten();
     respEvents = respEvents.unique();          
     return respEvents;  
}

def graphSetupPage(){
    def fontEnum = [["1":"1"], ["2":"2"], ["3":"3"], ["4":"4"], ["5":"5"], ["6":"6"], ["7":"7"], ["8":"8"], ["9":"9"], ["10":"10"], 
                    ["11":"11"], ["12":"12"], ["13":"13"], ["14":"14"], ["15":"15"], ["16":"16"], ["17":"17"], ["18":"18"], ["19":"19"], ["20":"20"]]; 
    
    def updateEnum = [["-1":"Never"], ["0":"Real Time"], ["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"],
                      ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1200000":"20 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"], ["6400000":"2 Hours"], ["19200000":"6 Hours"],
                      ["43200000":"12 Hours"], ["86400000":"1 Day"]];
    
    def timespanEnum = [["60000":"1 Minute"], 
                        ["3600000":"1 Hour"], 
                        ["43200000":"12 Hours"], 
                        ["86400000":"1 Day"], 
                        ["259200000":"3 Days"], 
                        ["604800000":"1 Week"], 
                        ["2419200000":"2 Weeks"], 
                        ["1814400000":"3 Weeks"], 
                        ["2419200000":"1 Month"], 
                        ["custom": "Custom Timespan"]];
     
     def timespanEnum2 = [["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["30000":"30 Seconds"], ["60000":"1 Minute"], ["120000":"2 Minutes"], ["300000":"5 Minutes"], ["600000":"10 Minutes"],
                          ["2400000":"30 minutes"], ["3600000":"1 Hour"], ["43200000":"12 Hours"], ["86400000":"1 Day"], ["259200000":"3 Days"], ["604800000":"1 Week"]];
    
    def updateRateEnum = [["-1":"Never"], ["0":"Real Time"], ["1000":"1 Second"], ["60000":"1 Minute"], ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1200000":"20 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]];
                 
    
    
    dynamicPage(name: "graphSetupPage") {
        
          
      
        parent.hubiForm_section(this,"General Options", 1)
        {      
            input( type: "enum", name: "graph_update_rate", title: "<b>Integration Time</b><br><small>(The amount of time each data point covers)</small>", 
                                 multiple: false, required: true, options: timespanEnum2, defaultValue: "300000", submitOnChange: true)

            input( type: "enum", name: "graph_refresh_rate", title: "<b>Graph Update Rate</b></br><small>(For panel viewing; the refresh rate of the graph)", 
                                 multiple: false, required: true, options: updateRateEnum, defaultValue: "300000")

            container = [];

            container <<  parent.hubiForm_sub_section(this, "Graph Time Span<br><small>Amount of time the graph covers</small>");

            if (graph_timespan_weeks == null){
                app.updateSetting("graph_timespan_weeks", 0);
                app.updateSetting("graph_timespan_days", 1);
                app.updateSetting("graph_timespan_hours", 0);
                app.updateSetting("graph_timespan_minutes", 0);
                settings["graph_timespan_weeks"] = 0;
                settings["graph_timespan_days"] = 1;
                settings["graph_timespan_hours"] = 0;
                settings["graph_timespan_minutes"] = 0;
            }

            container << parent.hubiForm_slider (this,  title: "<b>Weeks</b>", name: "graph_timespan_weeks",  
                                                        default: 0, min: 0, max: 104, units: " weeks", submit_on_change: true);

            container << parent.hubiForm_slider (this,  title: "<b>Days</b>", name: "graph_timespan_days",  
                                                        default: 0, min: 0, max: 30, units: " days", submit_on_change: true);

            container << parent.hubiForm_slider (this,  title: "<b>Hours</b>", name: "graph_timespan_hours",  
                                                        default: 0, min: 0, max: 24, units: " hours", submit_on_change: true);
            
            container << parent.hubiForm_slider (this,  title: "<b>Minutes</b>", name: "graph_timespan_minutes",  
                                                        default: 0, min: 0, max: 60, units: " seconds", submit_on_change: true);
            
            if (graph_timespan_weeks==null){
                secs = 86400000;
            } else {
                secs = (long)((double)(graph_timespan_weeks)*604800000+
                              (double)(graph_timespan_days)*86400000+
                              (double)(graph_timespan_hours)*3600000+
                              (double)(graph_timespan_minutes)*60000);
            }
            
            app.updateSetting("graph_timespan", secs);

            points = graph_update_rate ? (long)(secs/Double.parseDouble(graph_update_rate)) : 280;
            
            if (points > 2000) {
                container << parent.hubiForm_text (this, """<span style="color: red; font-weight: bold;">WARNING:</span> <b>${(points)} Points </b>will be generated per Attribute per Graph<br><small>Too many points will cause Hubigraphs to hang or take a long time to generate</small>""");
            } else {
                container << parent.hubiForm_text (this, "NOTE: <b>${(points)} Points </b>will be generated per Attribute per Graph");
            }

            container <<  parent.hubiForm_sub_section(this, "Other Options");

            container << parent.hubiForm_color (this, "Graph Background",    "graph_background", "#FFFFFF", false)
            container << parent.hubiForm_switch(this, title: "<b>Smooth Graph Points</b><br><small>(Enable Google Graph Smoothing)</small>", name: "graph_smoothing", default: false);
            container << parent.hubiForm_switch(this, title: "<b>Flip Graph to Vertical?</b><br><small>(Rotate 90 degrees)</small>", name: "graph_y_orientation", default: false);
            container << parent.hubiForm_switch(this, title: "<b>Reverse Data Order?</b><br><small> (Flip data left to Right)</small>", name: "graph_z_orientation", default: false)
              
            parent.hubiForm_container(this, container, 1); 
     
        }
             
        parent.hubiForm_section(this,"Graph Title", 1)
        {    
            container = [];
            container << parent.hubiForm_switch(this, title: "<b>Show Title on Graph</b>", name: "graph_show_title", default: false, submit_on_change: true);
            if (graph_show_title==true) {
                container << parent.hubiForm_text_input (this, "<b>Graph Title</b>", "graph_title", "Graph Title", false);
                container << parent.hubiForm_font_size  (this, title: "Title", name: "graph_title", default: 9, min: 2, max: 20);
                container << parent.hubiForm_color      (this, "Title", "graph_title", "#000000", false);
                container << parent.hubiForm_switch     (this, title: "Graph Title Inside Graph?", name: "graph_title_inside", default: false);
            }
            parent.hubiForm_container(this, container, 1); 
        }
            
         parent.hubiForm_section(this, "Graph Size", 1){
            container = [];

            container << parent.hubiForm_switch     (this,  title: "<b>Set Fill % of Graph?</b><br><small>(False = Default (80%) Fill)</small>", 
                                                            name: "graph_percent_fill", default: false, submit_on_change: true);
            if (graph_percent_fill==true){   

                container << parent.hubiForm_slider (this,  title: "Horizontal fill % of the graph", name: "graph_h_fill",  
                                                            default: 80, min: 1, max: 100, units: "%", submit_on_change: false);
                container << parent.hubiForm_slider (this,  title: "Vertical fill % of the graph", name: "graph_v_fill",  
                                                            default: 80, min: 1, max: 100, units: "%", submit_on_change: false); 
                                                       
            }
            container << parent.hubiForm_switch     (this,  title: "<b>Set size of Graph?</b><br><small>(False = Fill Window)</small>", 
                                                            name: "graph_static_size", default: false, submit_on_change: true);
            if (graph_static_size==true){      

                container << parent.hubiForm_slider (this,  title: "Horizontal dimension of the graph", name: "graph_h_size",  
                                                            default: 800, min: 100, max: 3000, units: " pixels", submit_on_change: false);
                container << parent.hubiForm_slider (this,  title: "Vertical dimension of the graph", name: "graph_v_size",  
                                                            default: 600, min: 100, max: 3000, units: " pixels", submit_on_change: false);   
            }

            parent.hubiForm_container(this, container, 1); 
        }
        
          parent.hubiForm_section(this,"Horizontal Axis", 1)
         { 
            //Axis
            container = [];
            container << parent.hubiForm_font_size  (this, title: "Horizontal Axis", name: "graph_haxis", default: 9, min: 2, max: 20);
            container << parent.hubiForm_color      (this, "Horizonal Header", "graph_hh", "#C0C0C0", false);
            container << parent.hubiForm_color      (this, "Horizonal Axis", "graph_ha", "#C0C0C0", false);
            container << parent.hubiForm_text_input (this, "<b>Num Horizontal Gridlines</b><small> (Blank for auto)</small>", "graph_h_num_grid", "", false);
            
            container << parent.hubiForm_text_input (this, "<b>Horizontal Axis Format<b>", "graph_h_format", "", "true");
            if (graph_h_format){
                today = new Date();
                container << parent.hubiForm_text(this, """<i><small><b>Horizontal Axis Sample:</b> ${today.format(graph_h_format)}</small></i>""");
            }
             
            container << parent.hubiForm_switch     (this, title: "Show String Formatting Help", name: "dummy", default: false, submit_on_change: true);           
            if (dummy == true){
                rows = [];
                header = ["<small>Name",           "Format", "Result"];
                rows <<  ["Year",           "Y",      "2020"]; 
                rows << ["Month Number",    "M",      "12"]; 
                rows << ["Month Name ",     "MMM",    "Feb"]; 
                rows << ["Month Full Name", "MMMM",   "February"]; 
                rows << ["Day of Month",     "d",     "February"]; 
                rows << ["Day of Week",      "EEE",   "Mon"]; 
                rows << ["Day of Week",      "EEEE",  "Monday"]; 
                rows << ["Period",           "a",     "AM/PM"]; 
                rows << ["Hour (12)",        "h",     "1..12"]; 
                rows << ["Hour (12)",        "hh",    "01..12"]; 
                rows << ["Hour (24)",        "H",     "1..23"]; 
                rows << ["Hour (24)",        "HH",    "01..23"]; 
                rows << ["Minute",           "m",     "1..59"]; 
                rows << ["Minute",           "mm",    "01..59"]; 
                rows << ["Seconds",          "s",     "1..59"]; 
                rows << ["Seconds",          "ss",     "01..59 <small>"];
                   
                 
               container << parent.hubiForm_table(this, header: header, rows: rows);
               container << parent.hubiForm_text(this, """<b><small>Example: "EEEE, MMM d, Y hh:mm:ss a" <br>= "Monday, June 2, 2020 08:21:33 AM</small></b>""")
            }
                 
            
            
            parent.hubiForm_container(this, container, 1); 
          
         }
            
        //Vertical Axis
         parent.hubiForm_section(this,"Vertical Axis", 1)
         { 
            container = [];
            container << parent.hubiForm_font_size (this, title: "Vertical Axis", name: "graph_vaxis", default: 9, min: 2, max: 20);
            container << parent.hubiForm_color (this, "Vertical Header", "graph_vh", "#000000", false);
            container << parent.hubiForm_color (this, "Vertical Axis", "graph_va", "#C0C0C0", false);
            parent.hubiForm_container(this, container, 1); 
         }

        //Left Axis 
        def formatEnum = [["":       "No Formatting  ::: 12345"], ["decimal":"Decimal ::: 12,345"], ["short": "Short ::: 12K"], ["scientific": "Scientific ::: 1e5"], ["percent": "Percent ::: 1234500%"], ["long": "Long ::: 12 Thousand"]];    
        
        parent.hubiForm_section(this,"Left Axis", 1, "arrow_back"){ 
            input( type: "enum", name: "graph_vaxis_1_format", title: "<b>Number Format</b>", multiple: false, required: true, options: formatEnum, defaultValue: "")
            container = [];
            container << parent.hubiForm_text_input(this,  "<b>Minimum for left axis</b><small> (Blank for auto)</small>", "graph_vaxis_1_min", "", false);
            container << parent.hubiForm_text_input(this,  "<b>Maximum for left axis</b><small> (Blank for auto)</small>", "graph_vaxis_1_max", "", false);   
            container << parent.hubiForm_text_input (this, "<b>Num Vertical Gridlines</b><small> (Blank for auto)</small>", "graph_vaxis_1_num_lines", "", false);
            container << parent.hubiForm_switch     (this, title: "<b>Show Left Axis Label on Graph</b>", name: "graph_show_left_label", default: false, submit_on_change: true);
            if (graph_show_left_label==true){
                container << parent.hubiForm_text_input (this, "<b>Input Left Axis Label</b>", "graph_left_label", "Left Axis Label", false);
                container << parent.hubiForm_font_size  (this, title: "Left Axis", name: "graph_left", default: 9, min: 2, max: 20);
                container << parent.hubiForm_color      (this, "Left Axis", "graph_left", "#FFFFFF", false);
            }
            parent.hubiForm_container(this, container, 1); 
        }

        //Right Axis   
        parent.hubiForm_section(this,"Right Axis", 1, "arrow_forward"){  
            input( type: "enum", name: "graph_vaxis_2_format", title: "<b>Number Format</b>", multiple: false, required: true, options: formatEnum, defaultValue: "")
            container = [];
            container << parent.hubiForm_text_input(this,  "<b>Minimum for right axis</b><small> (Blank for auto)</small>", "graph_vaxis_2_min", "", false);
            container << parent.hubiForm_text_input(this,  "<b>Maximum for right axis</b><small> (Blank for auto)</small>", "graph_vaxis_2_max", "", false);   
            container << parent.hubiForm_text_input (this, "<b>Num Vertical Gridlines</b><small> (Blank for auto)</small>", "graph_vaxis_2_num_lines", "", false);
            container << parent.hubiForm_switch     (this, title: "<b>Show Right Axis Label on Graph</b>", name: "graph_show_right_label", default: false, submit_on_change: true);
            if (graph_show_right_label==true){
                container << parent.hubiForm_text_input (this, "<b>Input right Axis Label</b>", "graph_right_label", "Right Axis Label", false);
                container << parent.hubiForm_font_size  (this, title: "Right Axis", name: "graph_right", default: 9, min: 2, max: 20);
                container << parent.hubiForm_color      (this, "Right Axis", "graph_right", "#FFFFFF", false);
            }
            parent.hubiForm_container(this, container, 1); 
        }

        //Legend
        parent.hubiForm_section(this,"Legend", 1){
            container = [];
            def legendPosition = [["top": "Top"], ["bottom":"Bottom"], ["in": "Inside Top"]];
            def insidePosition = [["start": "Left"], ["center": "Center"], ["end": "Right"]];
            container << parent.hubiForm_switch(this, title: "<b>Show Legend on Graph</b>", name: "graph_show_legend", default: false, submit_on_change: true);
            if (graph_show_legend==true){
                container << parent.hubiForm_font_size  (this, title: "Legend", name: "graph_legend", default: 9, min: 2, max: 20);
                container << parent.hubiForm_color      (this, "Legend", "graph_legend", "#000000", false);
                parent.hubiForm_container(this, container, 1); 
                input( type: "enum", name: "graph_legend_position", title: "<b>Legend Position</b>", defaultValue: "Bottom", options: legendPosition);
                input( type: "enum", name: "graph_legend_in side_position", title: "<b>Legend Justification</b>", defaultValue: "center", options: insidePosition);
            } else {
                 parent.hubiForm_container(this, container, 1); 
            }

        }
        
        parent.hubiForm_section(this, "Current Value Overlay", 1){
            def horizonalAlignmentEnum = ["Left", "Middle", "Right"];
            def veticalAlignmentEnum = ["Top", "Middle", "Bottom"];
            container = [];
            container << parent.hubiForm_switch     (this, title: "<b>Show Current Values on Graph?</b>", name: "show_overlay", default: false, submit_on_change: true);
            if (show_overlay == true){
                container << parent.hubiForm_color      (this, "Background", "overlay_background", "#000000", false);
                container << parent.hubiForm_slider     (this, title: "Background Opacity", 
                                                               name:  "overlay_background_opacity",  
                                                               default: 90, 
                                                               min: 0,
                                                               max: 100, 
                                                               units: "%",
                                                               submit_on_change: false);
                
               container << parent.hubiForm_font_size  (this, title: "Device", name: "overlay", default: 12, min: 2, max: 40);
               container << parent.hubiForm_color      (this, "Device Text", "overlay_text", "#FFFFFF", false);
                
               container << parent.hubiForm_enum (this, title:    "Horizontal Placement", 
                                                        name:     "overlay_horizontal_placement",
                                                        list:     horizonalAlignmentEnum,
                                                        default:  "Right");
                
                container << parent.hubiForm_enum (this, title:    "Vertical Placement", 
                                                         name:     "overlay_vertical_placement",
                                                         list:     veticalAlignmentEnum,
                                                         default:  "Top");

                container <<  parent.hubiForm_sub_section(this, "Display Order");
                parent.hubiForm_container(this, container, 1); 
                container = [];              
                parent.hubiForm_list_reorder(this, "overlay_order", "background");
            }
            parent.hubiForm_container(this, container, 1); 
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
        def bar_size_shown = false;
        
        //Deal with Global-Specific Settings (i.e bar spacing and plot-point size)
        def show_tile = false;
        def show_bar = false;
        def show_scatter = false;
        sensors.each { sensor ->        
                settings["attributes_${sensor.id}"].each { attribute ->              
                        switch (settings["graph_type_${sensor.id}_${attribute}"]){
                            case "Bar"      : show_title = true; show_bar = true; break;
                        }
                    }
                }
        if (show_title){
            parent.hubiForm_section(this,"Overall Settings for Graph Types", 1){
                container = [];
                
                if (show_bar) {
                    container << parent.hubiForm_slider (this, title: "Bar Graphs:: Relative Width for Bars", 
                                                               name:  "graph_bar_width",  
                                                               default: 90, 
                                                               min: 0,
                                                               max: 100, 
                                                               units: "%",
                                                               submit_on_change: false);
                }
                parent.hubiForm_container(this, container, 1);
            }
        }

        sensors.each { sensor ->        
                settings["attributes_${sensor.id}"].each { attribute ->
                    
                    parent.hubiForm_section(this,"${sensor.displayName} - ${attribute}", 1){
                               
                        container = [];
                        
                        container <<  parent.hubiForm_sub_section(this, "Plot Options");
                        
                        container << parent.hubiForm_enum (this, title:             "Plot Type", 
                                                                 name:              "graph_type_${sensor.id}_${attribute}",
                                                                 list:              ["Line", "Area", "Scatter", "Bar", "Stepped"],
                                                                 default:           "Line",
                                                                 submit_on_change:  true);
                        
                        container << parent.hubiForm_enum (this, title:             "Time Integration Function", 
                                                                 name:              "var_${sensor.id}_${attribute}_function",
                                                                 list:              ["Average", "Min", "Max", "Mid", "Sum"],
                                                                 default:           "Average");
                        
                        container << parent.hubiForm_enum (this, title:             "Axis Side", 
                                                                 name:              "graph_axis_number_${sensor.id}_${attribute}",
                                                                 list:              ["Left", "Right"],
                                                                 default:           "Left");
                        
                        def colorText = "";
                        def fillText = "";
                        def graphType = settings["graph_type_${sensor.id}_${attribute}"]; 
                        switch (graphType){
                             case "Line":
                                colorText = "Line";
                                break;
                             case "Area":
                                colorText = "Area Line";
                                fillText = "Fill";
                                break;
                             case "Bar":
                                colorText = "Bar Border";
                                fillText = "Fill";
                                break;
                             case "Scatter":
                                colorText = "Border";
                                fillText = "Fill";
                                break;
                            case "Stepped":
                                colorText = "Line";
                                fillText = "Fill";
                                break;
                        }
                        
                        container <<  parent.hubiForm_sub_section(this, colorText+" Options");
                        
                        container << parent.hubiForm_color(this, colorText, 
                                                                 "var_${sensor.id}_${attribute}_stroke", 
                                                                  parent.hubiTools_rotating_colors(cnt), 
                                                                  false);
                        
                        container << parent.hubiForm_slider (this, title: colorText+" Opacity", 
                                                                           name:  "var_${sensor.id}_${attribute}_stroke_opacity",  
                                                                           default: 90, 
                                                                           min: 0,
                                                                           max: 100, 
                                                                           units: "%",
                                                                           submit_on_change: false);
                        
                        container << parent.hubiForm_line_size  (this,  title: colorText,                   
                                                                            name: "var_${sensor.id}_${attribute}_stroke", 
                                                                            default: 2, min: 1, max: 20);
                        
                        
                        if (graphType == "Bar" || graphType == "Area" || graphType == "Stepped") {
                            
                            container <<  parent.hubiForm_sub_section(this, graphType+" "+fillText+" Options");
                            
                            container << parent.hubiForm_color(this, fillText, 
                                                                     "var_${sensor.id}_${attribute}_fill", 
                                                                      parent.hubiTools_rotating_colors(cnt), 
                                                                      false);
                            
                            container << parent.hubiForm_slider (this, title: fillText+" Opacity", 
                                                                       name:  "var_${sensor.id}_${attribute}_fill_opacity",  
                                                                       default: 90, 
                                                                       min: 0,
                                                                       max: 100, 
                                                                       units: "%",
                                                                       submit_on_change: false);    
                        }
                        if (graphType == "Scatter" || graphType == "Line" || graphType == "Area"){
                            
                            container <<  parent.hubiForm_sub_section(this, "Data Points");
                            
                            if (graphType == "Line" || graphType == "Area"){
                                container << parent.hubiForm_switch(this, title: "<b>Display Data Points on Line?</b>", name: "var_${sensor.id}_${attribute}_line_plot_points", default: false, submit_on_change: true);
                            }
                            
                            if (settings["var_${sensor.id}_${attribute}_line_plot_points"] || graphType == "Scatter"){
                                
                                 
                                container << parent.hubiForm_enum (this, 
                                                                   title: "Point Type", 
                                                                   name:  "var_${sensor.id}_${attribute}_point_type",
                                                                   list: [ "Circle", "Triangle", "Square", "Diamond", "Star", "Polygon"],
                                                                   default: "Circle");
                                                                   
                                
                                 container << parent.hubiForm_slider (this, title: "Point Size", 
                                                                            name:  "var_${sensor.id}_${attribute}_point_size",  
                                                                            default: 5, 
                                                                            min: 0,
                                                                            max: 60, 
                                                                            units: " points",
                                                                            submit_on_change: false);
                                if (graphType == "Area") {
                                    container << parent.hubiForm_text (this, "<b>*Note, Area Plots use the same fill setting for Points and Area (Above)");
                                } else {
                                    container << parent.hubiForm_color(this, "Point Fill", 
                                                                              "var_${sensor.id}_${attribute}_fill", 
                                                                              parent.hubiTools_rotating_colors(cnt), 
                                                                              false);
                            
                                     container << parent.hubiForm_slider (this, title: "Point Fill Opacity", 
                                                                                name:  "var_${sensor.id}_${attribute}_fill_opacity",  
                                                                                default: 90, 
                                                                                min: 0,
                                                                                max: 100, 
                                                                                units: "%",
                                                                                submit_on_change: false);
                                }
                            } else {
                                   app.updateSetting ("var_${sensor.id}_${attribute}_point_size", 0); 
                            }
                        }
                            
                        currentAttribute = null;
                        enumType = false;
                        sensor.getSupportedAttributes().each{attrib->
                            if (attrib.name == attribute){
                                currentAttribute = attrib;
                                if (attrib.dataType == "ENUM") enumType = true;
                            }
                        }
                        
                        if (enumType == true){
                            possible_values = currentAttribute.getValues();
                            def count_ = 0;
                            container <<  parent.hubiForm_sub_section(this, """Numerical values for "$attribute" states""");
                     
                            possible_values.each{value->
                                
                                container << parent.hubiForm_text_input(this, "Value for <mark>$value</mark>",
                                                                              "attribute_${sensor.id}_${attribute}_${value}",
                                                                              "100", 
                                                                               false);
                                count_++;
                            }
                            app.updateSetting ("attribute_${sensor.id}_${attribute}_states", possible_values);
                       
                        }  
                        
                        if (enumType == false){
                            container <<  parent.hubiForm_sub_section(this, """Custom State Values for "$attribute" """ );
                            if (settings["attribute_${sensor.id}_${attribute}_custom_states"] == null){
                                app.updateSetting("attribute_${sensor.id}_${attribute}_custom_states", [type: "bool", value: "false"]);
                            }
                            container << parent.hubiForm_switch(this, title: "<b>Set Custom State Values?</b><br><small>(For custom drivers w/ non-numeric values)</small>", 
                                                                      name: "attribute_${sensor.id}_${attribute}_custom_states", 
                                                                      default: false, 
                                                                      submit_on_change: true);
                            
                            if (settings["attribute_${sensor.id}_${attribute}_custom_states"] == true){
                                
                                    if (!settings["attribute_${sensor.id}_${attribute}_num_custom_states"]){
                                        
                                    }
                                    
                                    container << parent.hubiForm_text_input(this,"<b>Number of Custom States</b>",
                                                                                 "attribute_${sensor.id}_${attribute}_num_custom_states",
                                                                                 "2", "true");
                                
                                    def numStates = Integer.parseInt(settings["attribute_${sensor.id}_${attribute}_num_custom_states"]);
                                
                                    for (def i=0; i<numStates; i++){
                                        subcontainer = [];
                                                                              
                                        subcontainer << parent.hubiForm_text_input(this, "<b>State #"+(i)+"</b>",
                                                                                         "attribute_${sensor.id}_${attribute}_custom_state_${i}",
                                                                                          "",
                                                                                          "true");
                
                                        if (settings["attribute_${sensor.id}_${attribute}_custom_state_${i}"]){
                                            
                                                                           
                                            subcontainer << parent.hubiForm_text_input(this, '<b>Value for "<mark>'+settings["attribute_${sensor.id}_${attribute}_custom_state_${i}"]+'</mark></b>"',
                                                                                              "attribute_${sensor.id}_${attribute}_custom_state_${i}_value",
                                                                                              "0",
                                                                                              "true");
                                                
                                        }
                                        container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.5, 0.5]); 
                                        
                                    }  
                                
                                    //Update Settings
                                    
                                    possible_values = [];
                                    for (i=0; i<Integer.parseInt(settings["attribute_${sensor.id}_${attribute}_num_custom_states"]); i++){
                                        if (settings["attribute_${sensor.id}_${attribute}_custom_state_${i}"] &&
                                            settings["attribute_${sensor.id}_${attribute}_custom_state_${i}_value"]){
                                                val = settings["attribute_${sensor.id}_${attribute}_custom_state_${i}"].replaceAll("\\s","");
                                                possible_values << val;
                                                app.updateSetting ("attribute_${sensor.id}_${attribute}_${val}", settings["attribute_${sensor.id}_${attribute}_custom_state_${i}_value"]);
                                        }
                                    }
                                    if (possible_values != []) app.updateSetting ("attribute_${sensor.id}_${attribute}_states", possible_values);
                                
                                
                            } else {
                                if (settings["attribute_${sensor.id}_${attribute}_states"]){
                                    possible_values = settings["attribute_${sensor.id}_${attribute}_states"];
                                    possible_values.each{val->
                                        app.updateSetting ("attribute_${sensor.id}_${attribute}_${val}", 0);
                                    }
                                    app.updateSetting ("attribute_${sensor.id}_${attribute}_states", 0);
                                }
                            }

                        }
                        
                        //Line and Area Graphs can be "Drop-line"
                        if ((graphType == "Line" || graphType == "Area") && enumType==false && settings["attribute_${sensor.id}_${attribute}_custom_states"] == false) {

                                    container << parent.hubiForm_sub_section(this, "Handle Missing Values");
                         
                                    container << parent.hubiForm_switch(this, title: "<b>Display Missing Data as a Drop Line?</b>", name: "attribute_${sensor.id}_${attribute}_drop_line", default: false, submit_on_change: true);
                                                              
                                    if (settings["attribute_${sensor.id}_${attribute}_drop_line"]==true){
                                        
                                        container << parent.hubiForm_text_input(this,"<b>Value of Missing Data</b>",
                                                                                     "attribute_${sensor.id}_${attribute}_drop_value",
                                                                                     "0", false);                                    
                                    }

                                    container << parent.hubiForm_switch(this, title: "<b>Extend Left Value?</b><br><small>When values are unavailable, extend value to left</small>", 
                                                                              name: "attribute_${sensor.id}_${attribute}_extend_left", default: false, submit_on_change: false);

                                    container << parent.hubiForm_switch(this, title: "<b>Extend Right Value?</b><br><small>When values are unavailable, extend value to right</small>", 
                                                                              name: "attribute_${sensor.id}_${attribute}_extend_right", default: false, submit_on_change: false);
                       
                        }
                        
                        container << parent.hubiForm_sub_section(this, "Restrict Displayed Values");
                        
                        container << parent.hubiForm_switch(this, title: "<b>Restrict Displaying Bad Values?</b>", name: "attribute_${sensor.id}_${attribute}_bad_value", default: false, submit_on_change: true);

                        if (settings["attribute_${sensor.id}_${attribute}_bad_value"]==true){
                            
                            container << parent.hubiForm_text_input(this,"<b>Min Value to Exclude</b><br><small>If the recorded sensor value is <b>below</b> this value it will be dropped</small>",
                                                                         "attribute_${sensor.id}_${attribute}_min_value",
                                                                         "0", false);
                            
                            container << parent.hubiForm_text_input(this,"<b>Max Value to Exclude</b><br><small>If the recorded sensor value is <b>above</b> this value it will be dropped</small>",
                                                                         "attribute_${sensor.id}_${attribute}_max_value",
                                                                         "100", false);
                        }
                        
                        
                        
                        container <<  parent.hubiForm_sub_section(this, "Override Display Name on Graph");
                        
                        container << parent.hubiForm_text_input(this,   "<small></i>Use %deviceName% for DEVICE and %attributeName% for ATTRIBUTE</i></small>",
                                                                                "graph_name_override_${sensor.id}_${attribute}",
                                                                                "%deviceName%: %attributeName%", false);
                        
                        container << parent.hubiForm_text_input(this,   "<b>Units for Pretty Display</b>",
                                                                        "units_${sensor.id}_${attribute}",
                                                                        "", false);
                            
                        parent.hubiForm_container(this, container, 1);         
                        cnt += 1;
                
                    }//parent.hubiForm           
            }//attribute
        }//sensor
    }//page
}//function

def deviceSelectionPage() {
    def final_attrs;
       
    dynamicPage(name: "deviceSelectionPage") {
         parent.hubiForm_section(this,"Device Selection", 1){
             input "sensors", "capability.*", title: "Sensors", multiple: true, required: true, submitOnChange: true
        
            if (sensors){
                sensors.each {
                    attributes_ = it.getSupportedAttributes();
                    final_attrs = [];
                    
                    attributes_.each{ attribute_->
                        name = attribute_.getName();
                        if (it.currentState(name)){
                            final_attrs << ["$name" : "$name ::: [${it.currentState(name).getValue()}]"];
                        }
                    }
                    final_attrs = final_attrs.unique(false);
                    container = [];
                    container <<  parent.hubiForm_sub_section(this, it.displayName);
                    parent.hubiForm_container(this, container, 1);     
                    input( type: "enum", name: "attributes_${it.id}", title: "Attributes to graph", required: true, multiple: true, options: final_attrs, defaultValue: "1")
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
                    container << parent.hubiForm_page_button(this, "Long Term Storage", "longTermStoragePage", "100%", "storage");
                    
                    parent.hubiForm_container(this, container, 1); 
                }
                parent.hubiForm_section(this, "Local Graph URL", 1, "link"){
                    container = [];
                    container << parent.hubiForm_text(this, "${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
                    
                    parent.hubiForm_container(this, container, 1); 
                }
                
                
                if (lts){
                    parent.hubiForm_section(this, "Long Term Storage", 1, "calendar"){
                        container = [];
                        sensors.each { sensor ->
                            settings["attributes_${sensor.id}"].each {attribute ->
                                if (atomicState["history_${sensor.id}_${attribute}"]){
                                    data = atomicState["history_${sensor.id}_${attribute}"];
                                    container << parent.hubiForm_text(this, "<b>${sensor.displayName} - ${attribute}</b><br><small>${new Date(data[0].date)}<br>${new Date(data[data.size-1].date)}<br>${data.size} Events </small>");
                                                       
                                }
                            }
                        }
                        if (container==[]){
                            container << parent.hubiForm_text(this, "<b>Long Term Storage Enabled</b><br>Events will be stored when a graph is refreshed and nightly.<br>Details will populate over time.");       
                        }
                    
                        parent.hubiForm_container(this, container, 1);
                    }
                }
                
                
                if (graph_timespan){
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
                        container << parent.hubiForm_text_input(this, "Rename the Application?", "app_name", "Hubigraph Time Graph", "false");
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

def longTermStoragePage(){
    def resp = [:];
    def today = new Date();
    def then = new Date();
    def events = [];
    
    use (groovy.time.TimeCategory) {
           then -= 1.days;
    }
    
    dynamicPage(name: "longTermStoragePage", title: "") {
        
        def total_bytes = 0;
        def recommendedUpdateRate = 0;
        
        section() {
            if(!sensors) {
                paragraph "Please select Sensors and Graph Options Before setting up storage"
            } else {
                          
                sensors.each { sensor ->
                    resp[sensor.id] = [:];
                        settings["attributes_${sensor.id}"].each {attribute ->
                            def start = new Date();
                            events = sensor.statesSince(attribute, then, [max: 100]).collect{[ date: it.date.getTime(), value: getValue(sensor.id, attribute, it.value)]}
                            //events = sensor.events();
                            events = events.flatten();
                            bytes = (events.size*128.0)/1024.0
                            total_bytes += bytes;
                            
                            if (events != null && events.size > 1)
                                recommendedUpdateRate = Math.round(((events[0].date-events[events.size-1].date)/1000)/60);
                            else
                                recommendedUpdateRate = 4*60;
                    }
                    
                }
                    
                parent.hubiForm_section(this, "Storage Options", 1, "memory"){
                    def timeEnum = ["1 Day", "2 Days", "3 Days", "4 Days", "5 Days", "6 Days", "1 Week", "2 Weeks", "3 Weeks", "1 Month", "2 Months", "Indefinite"];
                    def updateEnum = ["5 Minutes", "15 Minutes", "30 Minutes", "1 Hour", "2 Hours", "3 Hours", "4 Hours", "5 Hours", "6 Hours"];
                    
                    container = [];
                    container << parent.hubiForm_switch(this, title: "<b>Utilize Long Term Storage for Sensors</b>", 
                                                              name: "lts", 
                                                              default: false, 
                                                              submit_on_change: true);
                            
                    if (lts == true){                         
                        container << parent.hubiForm_enum(this, title: "Time of Storage to Maintain",
                                                                name: "lts_time",
                                                                list: timeEnum,
                                                                default: "1 Week",
                                                                submit_on_change: true); 
                        
                        container << parent.hubiForm_enum(this, title: "Time to Refresh/Maintain Storage",
                                                                name: "lts_update",
                                                                list: updateEnum,
                                                                default: "1 Hour",
                                                                submit_on_change: false);
                        
                        if (lts_time == null) 
                            app.updateSetting("lts_time",   [type: "enum", value: "1 Week"]);
                            app.updateSetting("lts_update", [type: "enum", value: "1 Hour"]);
                        lts_time = "1 Week";
                        
                        def factor = getDays(lts_time);
                        
                        factor = (total_bytes*factor);
                        
                        if (factor < 1024){
                             factor = factor.setScale(1, BigDecimal.ROUND_DOWN);
                             factorString = factor+" Kb";
                        } else {
                             factor = factor/1024;
                             factor = factor.setScale(2, BigDecimal.ROUND_DOWN);
                             factorString = factor+" Mb";
                        }
                        
                        container << parent.hubiForm_text(this, "Estimated Storage Needed: "+factorString+"<br>Recommended Update Rate: "+Math.floor((recommendedUpdateRate/60))+" hours" );

               
                    } else {
                        sensors.each { sensor ->
                            settings["attributes_${sensor.id}"].each {attribute ->
                                atomicState["history_${sensor.id}_${attribute}"] = null;
                            }
                        }
                    }
                    parent.hubiForm_container(this, container, 1);
                }  
                


            } //else
        }
    }
}

/********************************************************************************************************************************
*********************************************************************************************************************************
****************************************** END PAGES ********************************************************************************
*********************************************************************************************************************************
*********************************************************************************************************************************/
def longTermStorageUpdate(){
    buildData();
}

def getDays(str){

    switch (str){
        case "1 Day":      return 1; break;
        case "2 Days":      return 2; break;
        case "3 Days":      return 3; break;
        case "4 Days":      return 4; break;
        case "5 Days":      return 5; break;
        case "6 Days":      return 6; break;
        case "1 Week":     return 7; break;
        case "2 Weeks":    return 14; break;
        case "3 Weeks":    return 21; break;
        case "1 Month":    return 30; break;
        case "2 Months":   return 60; break;
        case "Indefinite": return 0; break;
    }    
    
}

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
    
    now = new Date();
    minutes = now.getMinutes();
    rate = minutes % 15;
    
    
    if (lts){
        switch (lts_update){
            case "5 Minutes" :  schedule("${minutes} ${minutes % 5}/5 ) * * * ? *", longTermStorageUpdate); break;
            case "15 Minutes" : schedule("${minutes} ${minutes % 15}/15 * * * ? *", longTermStorageUpdate); break;
            case "30 Minutes" : schedule("${minutes} ${minutes % 30}/30 * * * ? *", longTermStorageUpdate); break;
            case "1 Hour" :     schedule("${minutes} ${minutes} 0/1 * * ? *", longTermStorageUpdate); break;
            case "2 Hours" :    schedule("${minutes} ${minutes} 0/2 * * ? *", longTermStorageUpdate); break;
            case "3 Hours" :    schedule("${minutes} ${minutes} 0/3 * * ? *", longTermStorageUpdate); break;
            case "4 Hours" :    schedule("${minutes} ${minutes} 0/4 * * ? *", longTermStorageUpdate); break;
            case "5 Hours" :    schedule("${minutes} ${minutes} 0/5 * * ? *", longTermStorageUpdate); break;
            case "6 Hours" :    schedule("${minutes} ${minutes} 0/6 * * ? *", longTermStorageUpdate); break;
        }
    }
    
}

def initialize() {
   updated();
}

private getValue(id, attr, val){
    def reg = ~/[a-z,A-Z]+/;
    
    orig = val;
    val = val.replaceAll("\\s","");
    if (settings["attribute_${id}_${attr}_${val}"]!=null){
        ret = Double.parseDouble(settings["attribute_${id}_${attr}_${val}"]);
    } else {
        try { 
            ret = Double.parseDouble(val - reg);
        } catch (e) {
            log.debug ("Bad value in Parse: "+orig);
            ret = null;   
        }
    }
    return ret;
}

private cleanupData(data){
    def then = new Date();
    use (groovy.time.TimeCategory) {
           then -= getDays(lts_time);
    }
    then_milliseconds = then.getTime();
    
    return data.findAll{ it.date >= then_milliseconds };
}

private buildData() {
    def resp = [:]
    
    def graph_time;
    def then = new Date();
    
    use (groovy.time.TimeCategory) {
           val =  Double.parseDouble("${graph_timespan}")/1000.0;
           then -= ((int)val).seconds;
           graph_time = then.getTime();
    }
    
    if(sensors) {
        sensors.each { sensor ->
            resp[sensor.id] = [:];
            settings["attributes_${sensor.id}"].each {attribute ->
                def newData = [];  
                //if this exists in storage
                if (atomicState["history_${sensor.id}_${attribute}"]) {
                    oldData = atomicState["history_${sensor.id}_${attribute}"];
                    then = new Date(oldData[oldData.size-1].date);
                } else {
                     oldData = [];   
                }
                
                            
                newData << sensor.statesSince(attribute, then, [max: 2000]).collect{[ date: it.date.getTime(), value: getValue(sensor.id, attribute, it.value)]}
                newData = newData.flatten();
                oldData += newData.reverse();
                
                         
                resp[sensor.id][attribute] = oldData.findAll{ it.date > graph_time}; 
                
                //Restrict "bad" values 
                if (settings["attribute_${sensor.id}_${attribute}_bad_value"]==true){
                    min = Float.valueOf(settings["attribute_${sensor.id}_${attribute}_min_value"]);
                    max = Float.valueOf(settings["attribute_${sensor.id}_${attribute}_max_value"]);
                    resp[sensor.id][attribute] = resp[sensor.id][attribute].findAll{ it.value > min && it.value < max}; 
                }
                
                if (lts){
                    atomicState["history_${sensor.id}_${attribute}"] = cleanupData(oldData);
                } else atomicState["history_${sensor.id}_${attribute}"] = null;
            }    
        }
    }
    return resp;
}

def getChartOptions(){
    
    /*Setup Series*/
    def series = ["series" : [:]];
        
    def options = [
        "graphReduction": graph_max_points,
        "graphTimespan": Long.parseLong("${graph_timespan}"),
        "graphUpdateRate": Integer.parseInt(graph_update_rate),
        "graphRefreshRate" : Integer.parseInt(graph_refresh_rate),
        "overlays": [   "display_overlays" : show_overlay,
                            "horizontal_alignment" : overlay_horizontal_placement,
                            "vertical_alignment" : overlay_vertical_placement,
                            "order" : overlay_order
                        ],
        "graphOptions": [
            "tooltip" : ["format" : "short"], 
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size : "100%",
            "chartArea": [  "width":    graph_percent_fill ? "${graph_h_fill}%" : "80%", 
                            "height":   graph_percent_fill ? "${graph_v_fill}%" : "80%"],
            "hAxis": ["textStyle": ["fontSize": graph_haxis_font, 
                                    "color": graph_hh_color_transparent ? "transparent" : graph_hh_color ], 
                      "gridlines": ["color": graph_ha_color_transparent ? "transparent" : graph_ha_color, 
                                    "count": graph_h_num_grid != "" ? graph_h_num_grid : null
                                   ],
                      "format":     graph_h_format==""?"":graph_h_format                          
                     ],
            "vAxis": ["textStyle": ["fontSize": graph_vaxis_font, 
                                    "color": graph_vh_color_transparent ? "transparent" : graph_vh_color, 
                                    ],
                      "gridlines": ["color": graph_va_color_transparent ? "transparent" : graph_va_color],
                     ],
            "vAxes": [
                0: ["title" : graph_show_left_label ? graph_left_label: null,  
                    "titleTextStyle": ["color": graph_left_color_transparent ? "transparent" : graph_left_color, "fontSize": graph_left_font],
                    "viewWindow": ["min": graph_vaxis_1_min != "" ?  graph_vaxis_1_min : null, 
                                   "max":  graph_vaxis_1_max != "" ?  graph_vaxis_1_max : null],
                    "gridlines": ["count" : graph_vaxis_1_num_lines != "" ? graph_vaxis_1_num_lines : null ],
                    "minorGridlines": ["count" : 0],
                    "format": graph_vaxis_1_format, 
                    
                   ],
                
                1: ["title": graph_show_right_label ? graph_right_label : null,
                    "titleTextStyle": ["color": graph_right_color_transparent ? "transparent" : graph_right_color, "fontSize": graph_right_font],
                    "viewWindow": ["min": graph_vaxis_2_min != "" ?  graph_vaxis_2_min : null, 
                                   "max":  graph_vaxis_2_max != "" ?  graph_vaxis_2_max : null],
                    "gridlines": ["count" : graph_vaxis_2_num_lines != "" ? graph_vaxis_2_num_lines : null ],
                    "minorGridlines": ["count" : 0],
                    "format": graph_vaxis_2_format, 
                    ]                
            ],
            "bar": [ "groupWidth" : graph_bar_width+"%", "fill-opacity" : 0.5],
            "pointSize": graph_scatter_size,
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
            "series": [:],
            
        ]
    ];
    
    count_ = 0;
    temp_sensors = sensors.sort{it.id.toInteger()};
    temp_sensors.each { sensor ->
        settings["attributes_${sensor.id}"].each { attribute ->
            def type_ = settings["graph_type_${sensor.id}_${attribute}"].toLowerCase();
            if (type_ == "stepped") type_ = "steppedArea";
            def axes_ = settings["graph_axis_number_${sensor.id}_${attribute}"] == "Left" ? 0 : 1;
            def stroke_color = settings["var_${sensor.id}_${attribute}_stroke_color"];
            def stroke_opacity = settings["var_${sensor.id}_${attribute}_stroke_opacity"];
            def stroke_line_size = settings["var_${sensor.id}_${attribute}_stroke_line_size"];
            def fill_color = settings["var_${sensor.id}_${attribute}_fill_color"];
            def fill_opacity = settings["var_${sensor.id}_${attribute}_fill_opacity"];
            def point_size = settings["var_${sensor.id}_${attribute}_point_size"];
            def point_type = settings["var_${sensor.id}_${attribute}_point_type"] != null ? settings["var_${sensor.id}_${attribute}_point_type"].toLowerCase() : "";
           
            type_ = type_=="bar" ? "bars" : type_;
               
               
            options.graphOptions.series << ["$count_" : [ "type"            : type_,
                                                          "targetAxisIndex" : axes_,
                                                          "pointSize"       : point_size,
                                                          "pointShape"      : point_type,
                                                          "color"           : stroke_color,
                                                          "opacity"         : stroke_opacity,
                                                          
                                                        ]
                                           ];
            count_ ++;
       }
    }
        
    //add colors and thicknesses
    sensors.each { sensor ->
        settings["attributes_${sensor.id}"].each { attribute ->
            def axis = settings["graph_axis_number_${sensor.id}_${attribute}"] == "Left" ? 0 : 1;
            def text_color = settings["graph_line_${sensor.id}_${attribute}_color"];
            def text_color_transparent = settings["graph_line_${sensor.id}_${attribute}_color_transparent"];
            
            
            
            
            def annotations = [
                "targetAxisIndex": axis, 
                "color": text_color_transparent ? "transparent" : text_color                
                
                
            ];
            
            options.graphOptions.series << annotations;  
        }
    }    
    
    return options;
}
        
def getDrawType(){
   return "google.visualization.LineChart" 
}

def removeLastChar(str) {
    str.subSequence(0, str.length() - 1)
    str
}

def getRGBA(hex, opacity){
    
    def c = hex-"#";
    c = c.toUpperCase();
    i = Integer.parseInt(c, 16);
    
    r = (i & 0xFF0000) >> 16;
    g = (i & 0xFF00) >> 8;
    b = (i & 0xFF);
    o = opacity/100.0;
    s = sprintf("rgba( %d, %d, %d, %.2f)", r, g, b, o); 
    return s;
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
      <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.1/moment.min.js" integrity="sha512-qTXRIMyZIFb8iQcfjXWCO8+M5Tbc38Qi5WzdPOYZHIlZpzBHG3L3by84BBBOiRGiEb7KKtAOAs5qYdUiZiQNNQ==" crossorigin="anonymous"></script>      
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
        subscriptions = data;
        
    });
}

function getGraphData() {
    return jQuery.get("${state.localEndpointURL}getData/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Graph Data");
        graphData = data;
    });
}

function parseEvent(event) {
    const now = new Date().getTime();
    let deviceId = event.deviceId;

    //only accept relevent events
    

    if(subscriptions.ids.includes(deviceId) && subscriptions.attributes[deviceId].includes(event.name)) {
        
        let value = isNaN(event.value) ? event.value.replace(/ /g,'') : parseFloat((Math.round(event.value * 100) / 100).toFixed(2));
        
        let attribute = event.name;

        let state = isNaN(value) ? subscriptions.states[deviceId][attribute][value] : undefined;
         
        if (state != undefined){
            value = parseFloat(state);
        }

        graphData[deviceId][attribute].push({ date: now, value: value });

        updateOverlay(deviceId, attribute, value);
              
        if(options.graphRefreshRate === 0) update();
    }
}

function update(callback) {
    //boot old data
    let min = new Date().getTime();
    min -= options.graphTimespan;

    //First Filter Events that are too old
    Object.entries(graphData).forEach(([deviceId, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {
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
            .overlay {
               box-sizing: border-box;
               padding: ${overlay_font ? (overlay_font.toInteger()/2): 12}px ${overlay_font}px;
               position: absolute;
               background-color: ${overlay_background_color ? getRGBA(overlay_background_color, overlay_background_opacity) : ""};
               top: 50px;   
               left: 100px;
               text-align: center;
               box-shadow: 0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24);
            }
            .overlay-title {
               font-size: ${overlay_font}px;
               text-align: left;
               color: ${overlay_text_color};
               font-family: Arial, Helvetica, sans-serif;
               
            }
            .overlay-number {
               font-size: ${overlay_font}px;
               font-weight: 900;
               text-align: right;
               padding: 0px 0px 0px ${overlay_font}px;
               color: ${overlay_text_color};
               font-family: Arial, Helvetica, sans-serif;
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
    if(options.graphRefreshRate !== -1) {
        //start websocket
        websocket = new WebSocket("ws://" + location.hostname + "/eventsocket");
        websocket.onopen = () => {
            console.log("WebSocket Opened!");
        }
        websocket.onmessage = (event) => {
            parseEvent(JSON.parse(event.data));
        }

        if(options.graphRefreshRate !== 0) {
            setInterval(() => {
                update();
            }, options.graphRefreshRate);
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

function averageEvents(minTime, maxTime, data, drop_val) {
    const matches = data.filter(it => it.date > minTime && it.date <= maxTime);
    return matches.reduce((sum, it) => {
        if (sum.value == drop_val) sum.value = 0;
        sum.value += it.value / matches.length;
        return sum;
    }, { date: minTime+((maxTime - minTime)/2), value: drop_val});
}

function sumEvents(minTime, maxTime, data, drop_val) {
    const matches = data.filter(it => it.date > minTime && it.date <= maxTime);
    return matches.reduce((sum, it) => {
        if (sum.value == drop_val) sum.value = parseFloat(0);
        sum.value += parseFloat(it.value);
        return sum;
    }, { date: minTime+((maxTime - minTime)/2), value: drop_val});
}


function maxEvents(minTime, maxTime, data, drop_val) {
    const matches = data.filter(it => it.date > minTime && it.date <= maxTime);
if (matches.length != 0) {
        return { date: minTime+((maxTime - minTime)/2), value: Math.max.apply(Math, matches.map(function(o) { return o.value; })) };
}    
else
        return { date: minTime+((maxTime - minTime)/2), value: drop_val }; 
}

function minEvents(minTime, maxTime, data, drop_val) {
    const matches = data.filter(it => it.date > minTime && it.date <= maxTime);
   if (matches.length != 0)
        return { date: minTime+((maxTime - minTime)/2), value: Math.min.apply(Math, matches.map(function(o) { return o.value; })) };
    else
        return { date: minTime+((maxTime - minTime)/2), value: drop_val }; 
}

function midEvents(minTime, maxTime, data, drop_val) {
    const matches = data.filter(it => it.date > minTime && it.date <= maxTime);
    if (matches.length != 0)
        return { date: minTime+((maxTime - minTime)/2), value: matches[Math.floor(matches.length/2)].value };
    else
        return { date: minTime+((maxTime - minTime)/2), value: drop_val }; 
}


function getStyle(deviceIndex, attribute){
    
        let style = subscriptions.var[deviceIndex][attribute]
        let stroke_color = style.stroke_color == null ? "" : style.stroke_color;
        let stroke_opacity = style.stroke_opacity == null ? "" : parseFloat(style.stroke_opacity)/100.0;
        let stroke_width = style.stroke_width == null ? "" : style.stroke_width;
        let fill_color = style.fill_color == null ? "" : style.fill_color;
        let fill_opacity = style.fill_opacity == null ? "" : parseFloat(style.fill_opacity)/100.0;
        
        let returnString = `{ stroke-color: \${stroke_color}; stroke-opacity: \${stroke_opacity}; stroke-width: \${stroke_width}; fill-opacity: \${fill_opacity}; fill-color: \${fill_color}; }`
        if (subscriptions.graph_type[deviceIndex][attribute] == "Stepped") returnString = `{ stroke-opacity: \${stroke_opacity}; stroke-width: \${stroke_width}; fill-opacity: \${fill_opacity}; fill-color: \${fill_color}; }`

        return returnString;
}

function drawChart(callback) {
    let now = new Date().getTime();
    let min = now - options.graphTimespan;

    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn({ label: 'Date', type: 'datetime', });

    let colNums = {};
    let i = 0;
    subscriptions.ids.forEach((deviceId) => {
        
        subscriptions.attributes[deviceId].forEach((attr) => {   
            console.log(deviceId+" "+attr);
            dataTable.addColumn({ label: subscriptions.labels[deviceId][attr].replace('%deviceName%', subscriptions.sensors[deviceId].displayName).replace('%attributeName%', attr), type: 'number' }); 
            dataTable.addColumn({ role: "style" });
        });
    });

    //**********************************************************************************************************************************************
    //***************************************** BUILD THE STYLES  **********************************************************************************
    //**********************************************************************************************************************************************
    

    //**********************************************************************************************************************************************
    //***************************************** COLLATE THE CURRENT DATA ***************************************************************************
    //**********************************************************************************************************************************************
    let accumData = {};
    let then = now - options.graphTimespan;
    let spacing = options.graphUpdateRate;
    let overlay = 10;
    var current;
    var drop_val;
    var newEntry;
    var next;

    
    //adjust for days 
    if (options.graphUpdateRate >= 86400000){
        let d = new Date(then);
        d.setHours(0, 0, 0, 0);
        then = d.getTime();
    }
    
    console.info(subscriptions);

    //map the graph data
    Object.entries(graphData).forEach(([deviceIndex, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {

            let func = subscriptions.var[deviceIndex][attribute].function;
            let num_events = events.length;

            extend_left = subscriptions.extend[deviceIndex][attribute].left;
            extend_right = subscriptions.extend[deviceIndex][attribute].right;
            let drop_line = subscriptions.drop[deviceIndex][attribute].valid;
            let drop_val = null;

            if (drop_line == "true"){
                drop_val = parseFloat(subscriptions.drop[deviceIndex][attribute].value);
            } else if (num_events>0 && extend_left) {
                drop_val = events[0].value;   
            } 

            current = then;

            while (current < now){
                //if (subscriptions.states[deviceIndex][attribute] != undefined && events.length > 0){
                //    if (drop_val == null){
                //        drop_val = events[0].value;
                //    } else {
                //        drop_val = newEntry.value;
                //    }
                //}
                if (subscriptions.graph_type[deviceIndex][attribute] == "Stepped"){
                    drop_val = newEntry == undefined ? events[0].value : newEntry.value;
                }
                next = current+spacing;

                switch (func){
                    case "Average": newEntry = averageEvents(current, next, events, drop_val); break;
                    case "Min":     newEntry = minEvents(current, next, events, drop_val);     break;
                    case "Max":     newEntry = maxEvents(current, next, events, drop_val);     break;
                    case "Mid":     newEntry = midEvents(current, next, events, drop_val);     break;
                    case "Sum":     newEntry = sumEvents(current, next, events, drop_val);     break;
                }

                if (drop_line != "true"){
                    if (num_events > 0 && next >= events[0].date && extend_left){
                        drop_val = null;
                    }
                    if (num_events > 0 && events[num_events-1].date <= next && extend_right){
                        drop_val = events[num_events-1].value;
                    }
                }
                    
                
                accumData[newEntry.date] = [ ...(accumData[newEntry.date] ? accumData[newEntry.date] : []), newEntry.value];
                accumData[newEntry.date] = [ ...(accumData[newEntry.date] ? accumData[newEntry.date] : []), getStyle(deviceIndex, attribute)];
                current += spacing;
                    
            }
        });
    });

    let parsedGraphData = Object.entries(accumData).map(([date, vals]) => [moment(parseInt(date)).toDate(), ...vals]);

    parsedGraphData.forEach(it => {
        dataTable.addRow(it);
    });
    
    //**********************************************************************************************************************************************
    //***************************************** DRAW THE GRAPH *************************************************************************************
    //**********************************************************************************************************************************************
    
    let graphOptions = Object.assign({}, options.graphOptions);

    graphOptions.hAxis = Object.assign(graphOptions.hAxis, { viewWindow: { min: moment(min).toDate(), max: moment(now).toDate() } });

    let chart = new ${drawType}(document.getElementById("timeline"));

    //if we have a callback
    if(callback) google.visualization.events.addListener(chart, 'ready', callback);

    if (options.overlays.display_overlays) google.visualization.events.addListener(chart, 'ready', placeMarker.bind(chart, dataTable));

    chart.draw(dataTable, graphOptions);
    
}

function updateOverlay(deviceId, attribute, value){
    console.log(deviceId+" "+attribute+" "+value);
    let searchString = "#overlay-"+deviceId+"_"+attribute+"-number";
    let val = parseFloat(value).toFixed(1)+" "+subscriptions.var[deviceId][attribute].units;
    console.log(searchString);
    jQuery(searchString).text(val);
}

function placeMarker(dataTable) {
        var cli = this.getChartLayoutInterface();
        var chartArea = cli.getChartAreaBoundingBox();
        let width = jQuery('#graph-overlay').outerWidth();
        let height = jQuery('#graph-overlay').outerHeight();
        let overlay = options.overlays;

        console.debug("Width =", width);
        console.debug(chartArea);
        console.debug(cli);
        

        switch (overlay.vertical_alignment){
            case "Top":     document.querySelector('.overlay').style.top = Math.floor(chartArea.top) + "px"; + "px"; break;
            case "Middle":   document.querySelector('.overlay').style.top = Math.floor(chartArea.height/2+chartArea.top-height/2) + "px"; + "px"; break;
            case "Bottom":    document.querySelector('.overlay').style.top = Math.floor(chartArea.height+chartArea.top-height) + "px"; + "px"; break;

        }
        switch (overlay.horizontal_alignment){
            case "Left":     document.querySelector('.overlay').style.left = Math.floor(chartArea.left) + "px"; break;
            case "Middle":   document.querySelector('.overlay').style.left = Math.floor(chartArea.width/2-(width/2)+chartArea.left) + "px"; break;
            case "Right":    document.querySelector('.overlay').style.left = Math.floor(chartArea.width+chartArea.left-width) + "px"; break;

        }
       
                

        //document.querySelector('.overlay').style.width = Math.floor(chartArea.width*0.25) + "px";
        //document.querySelector('.overlay').style.height = Math.floor(chartArea.height*0.25) + "px";
      };

        google.charts.setOnLoadCallback(onLoad);
        window.onBeforeUnload = onBeforeUnload;

      </script>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
      </head>
      <body style="${fullSizeStyle}">
      <div id="timeline" style="${fullSizeStyle}" align="center"></div>
    """
    if (show_overlay==true) html+= getOverlay();
    
    html+= """
        
      </body>
       
    </html>
    """
    
    return html;
}



def getOverlay(){
 
    def html = """<div id="graph-overlay" class="overlay"><table style="width:100%">"""
       
    val = new JsonSlurper().parseText(overlay_order)
    
    val.each{ str->
        splitStr = str.split('_');
        log.debug(splitStr);
        deviceId = splitStr[1];
        attribute = splitStr[2];
    
        sensor = sensors.find{ it.id == deviceId } ;
   
        val = getValue(sensor.id, attribute, sensor.currentState(attribute).getValue());
        units = settings["units_${sensor.id}_${attribute}"] ? settings["units_${sensor.id}_${attribute}"] : "";
        name = settings["graph_name_override_${sensor.id}_${attribute}"];
        name = name.replaceAll("%deviceName%", sensor.displayName).replaceAll("%attributeName%", attribute);
        str = sprintf("%.1f%s", val, units);
        html += """<tr><td class="overlay-title" id="overlay-${sensor.id}_${attribute}-name">${name}</td>
                           <td class="overlay-number" id="overlay-${sensor.id}_${attribute}-number">${str}</td></tr>"""
    }
    html += """</div>"""
    
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
                state.remoteEndpointURL = fullApiServerUrl("/graph")
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
    def extend_ = [:];
    def var_ = [:];
    def graph_type_ = [:];
    def states_ = [:];
    
    
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
        extend_[sensor.id] = [:];
        graph_type_[sensor.id] = [:];
        var_[sensor.id] = [:];
        states_[sensor.id] = [:];
        
        settings["attributes_${sensor.id}"].each { attr ->
            
            def stroke_color = settings["var_${sensor.id}_${attr}_stroke_color"];
            def stroke_opacity = settings["var_${sensor.id}_${attr}_stroke_opacity"];
            def stroke_line_size = settings["var_${sensor.id}_${attr}_stroke_line_size"];
            def fill_color = settings["var_${sensor.id}_${attr}_fill_color"];
            def fill_opacity = settings["var_${sensor.id}_${attr}_fill_opacity"];
            def function = settings["var_${sensor.id}_${attr}_function"];
            
            
            if (settings["attribute_${sensor.id}_${attr}_states"] && settings["attribute_${sensor.id}_${attr}_custom_states"] == true){
                states_[sensor.id][attr] = [:];  
                settings["attribute_${sensor.id}_${attr}_states"].each{states->
                    states_[sensor.id][attr][states] = settings["attribute_${sensor.id}_${attr}_${states}"];
                }
            }
            
            drop_valid = false;
            if (settings["attribute_${sensor.id}_${attr}_drop_line"] == true)
                drop_valid = true;    
            
            drop_[sensor.id][attr] = [    valid: drop_valid  ? "true" : "false",
                                          value: drop_valid ? settings["attribute_${sensor.id}_${attr}_drop_value"] : "null"
                                      ];

            extend_[sensor.id][attr] = [
                right: settings["attribute_${sensor.id}_${attr}_extend_right"],
                left:  settings["attribute_${sensor.id}_${attr}_extend_left"]
            ];

            
            graph_type_[sensor.id][attr] = settings["graph_type_${sensor.id}_${attr}"];
            
            var_[sensor.id][attr] = [ stroke_color :   stroke_color,
                                      stroke_opacity : stroke_opacity,
                                      stroke_width:    stroke_line_size,
                                      fill_color:      fill_color,
                                      fill_opacity:    fill_opacity,
                                      function:        function, 
                                      units:           settings["units_${sensor.id}_${attr}"] ? settings["units_${sensor.id}_${attr}"] : "",
                                    ];
        }//settings
            
    } //sensors
    
    def obj = [
        ids: ids.sort(),
        sensors: sensors_,
        attributes: attributes, 
        labels : labels,
        drop : drop_,
        extend: extend_,
        graph_type: graph_type_,
        var : var_,
        states: states_
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
