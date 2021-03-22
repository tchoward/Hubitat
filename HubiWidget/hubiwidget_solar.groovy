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

// Hubigraph Solar Widget
// *****ALPHA BUILD

def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v1.0" }

definition(
    name: "HubiWidget Solar Widget",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "HubiWidget Solar Widget",
    category: "",
    parent: "tchoward:HubiWidgets",
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
    
    
    dynamicPage(name: "graphSetupPage") {    
          
        parent.hubiForm_section(this,"General Options", 1)
        {      
            container = [];

            container << parent.hubiForm_color (this, "Graph Background",    "graph_background", "#FFFFFF", false)
            
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
    }//page
}//function

def deviceSelectionPage() {
    def final_attrs;

    types_ = [
        solar : [
            color:              "#ffd300",
            arcColor:           "#9b870c",
            line_output:        "bottom",
            text:               "Solar",
            location:           "top",
            type:               "circle",
            default_threshold:  11000,
            connections:        [ grid :   ["grid-negative"], 
                                  home :   ["solar-nonzero", "grid-notequal-home"],
                                  battery: ["battery-negative"]
                                ],
            output:              [ side: "right"]
        ],
        grid : [
            color:              "#AAAAAA",
            arcColor:           "#111111",
            line_output:        "right",
            text:               "Grid",
            location:           "left", 
            type:               "circle",
            default_threshold:  9000,
            connections:        [ home : [ "grid-positive"]],
            input:              [ side: "top"],
            output:             [ side: "bottom"]


        ],
        home : [
            color:              '#3895d3',
            arcColor:           '#24618a',
            line_output:        "left",
            text:               "Home",
            location:           "right",
            type:               "circle",
            default_threshold:  9000,
            input:              [ side: "top"]

        ],
        battery : [
            color:              "#4b8b3b",
            arcColor:           "#294d20",
            line_output:        "top",
            text:               "Battery",
            location:           "bottom",
            type:               "circle",
            default_threshold:  7000,
            connections:        [home : ["battery-positive"]],
            input:              [side: "left"],
            output:             [side: "right"],
        ],
        batterypercent: [
            text:               "Battery Percent",
            type:               "battery",
            parent:             "battery",

        ]
    ];

    atomicState.types = types_;

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
                            final_attrs << ["${it.id}.$name" : "$name ::: [${it.currentState(name).getValue()}]"];
                        }
                    }

                    final_attrs = final_attrs.unique(false);
                    container = [];
                    container <<  parent.hubiForm_sub_section(this, it.displayName);
                    parent.hubiForm_container(this, container, 1);   

                }

                atomicState.types.each { dev, opts-> 
                    container = [];
                    container <<  parent.hubiForm_sub_section(this, opts.text);
                    parent.hubiForm_container(this, container, 1);   

                    input( type: "enum", name: "${dev}_attr",   
                                     title: "${opts.text} Current Value", required: true, multiple: false, options: final_attrs, defaultValue: "1");
                
                    if (opts.input){
                        input( type: "enum", name: "${dev}_input",   
                                     title: "${opts.text} Total Daily Input", required: false, multiple: false, options: final_attrs, defaultValue: "1");
                    
                    }

                    if (opts.output){
                        input( type: "enum", name: "${dev}_output",   
                                     title: "${opts.text} Total Daily Output", required: false, multiple: false, options: final_attrs, defaultValue: "1");
                    
                    }
                    
                
                }



                container = [];
                container <<  parent.hubiForm_sub_section(this, "Thresholds");
                
                atomicState.types.each {dev, opts-> 

                    if (opts.type == "circle"){

                        max = Math.round(opts.default_threshold+opts.default_threshold*0.1);
                        container << parent.hubiForm_slider     (this,  title: "${opts.text} Threshold for 100%", 
                                                                        name:  "${dev}_threshold",  
                                                                        default: opts.default_threshold, 
                                                                        min: 0,
                                                                        max: max, 
                                                                        units: "wH",
                                                                        submit_on_change: false);
                    
                    }

                }

                parent.hubiForm_container(this, container, 1);     
                
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
                
                
                if (sensors){
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

public getSensor(str){

    if (str==null) return null;

    String[] splitStr = str.tokenize('.');
    id = splitStr[0];
    attr = splitStr[1];
    return [sensor: sensors.find{it.id == id}, attr: attr];

}

public buildData() {

    if (!sensors) return 0;

    resp = [:];

    atomicState.types.each {dev, opts->

        result = getSensor(settings["${dev}_attr"]);
        full_load = settings["${dev}_threshold"];
        
        //If there is an input

        if (opts.input){
            r = getSensor(settings["${dev}_input"]);
            input_ = [id:    r.sensor.id,
                     attr:  r.attr, 
                     value: r.sensor.currentValue(r.attr)];   
        } else input_ = null;

        //If there is an output
        if (opts.output){
            r = getSensor(settings["${dev}_output"]);
            output_ = [id:   r.sensor.id,
                      attr:  r.attr, 
                      value: r.sensor.currentValue(r.attr)];  
        } else output_ = null;

        resp << [ "$dev" : [    options:       opts,
                                value:         result.sensor.currentValue(result.attr),
                                subscription:  [id:    result.sensor.id,           
                                                attr:  result.attr
                                                ],
                                io:            [ input: input_,
                                                 output: output_
                                                ],
                                full_load:     full_load
                            ]
        ]

        

    }
    return resp;
}


public getSensors(){

    return resp;
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

def getWebPage() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    def html = """
    <html style="${fullSizeStyle}">
        <link rel='icon' href='https://www.shareicon.net/data/256x256/2015/09/07/97252_barometer_512x512.png' type='image/x-icon'/> 
        <link rel="apple-touch-icon" href="https://www.shareicon.net/data/256x256/2015/09/07/97252_barometer_512x512.png">
        <head>
            <meta charset=utf-8>
            <meta name=viewport content="width=device-width, initial-scale=1, maximum-scale=1">
            <meta name=apple-mobile-web-app-capable content=yes>
            <meta name=apple-mobile-web-app-status-bar-style content=black>
            <script src="https://code.jquery.com/jquery-3.5.0.min.js" integrity="sha256-xNzN2a4ltkB44Mc/Jz3pT4iU1cmeR0FkXs4pru/JxaQ=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/svg.js/3.0.16/svg.min.js" integrity="sha256-MCvBrhCuX8GNt0gmv06kZ4jGIi1R2QNaSkadjRzinFs=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.29.1/moment.min.js" integrity="sha512-qTXRIMyZIFb8iQcfjXWCO8+M5Tbc38Qi5WzdPOYZHIlZpzBHG3L3by84BBBOiRGiEb7KKtAOAs5qYdUiZiQNNQ==" crossorigin="anonymous"></script> 
            <script src="https://cdn.jsdelivr.net/npm/@svgdotjs/svg.js@3.0/dist/svg.min.js"></script>
            <script src="/local/HubigraphSolarWidget.js"></script>

            <script type="text/javascript">

            function getOptions() {
                return jQuery.get("${state.localEndpointURL}getOptions/?access_token=${state.endpointSecret}", (data) => {
                });
            }

            function getSubscriptions() {
                return jQuery.get("${state.localEndpointURL}getSubscriptions/?access_token=${state.endpointSecret}", (data) => {
                });
            }

            function getGraphData() {
                return jQuery.get("${state.localEndpointURL}getData/?access_token=${state.endpointSecret}", (data) => {
                    console.log("Got Data");
                    sensorData = new Map(Object.entries(data));
                    console.log(sensorData);

                });
            }

            </script>
        </head>
        
        <body style="${fullSizeStyle}" onload="onLoad()" >
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
    render(contentType: "text/html", data: getWebPage());      
}

def getDataMetrics() {
    def data;
    def then = new Date().getTime();
    data = getData();
    def now = new Date().getTime();
    return data;
}

def getData() {
    def data = buildData();
    return render(contentType: "text/json", data: JsonOutput.toJson(data));
}

def getOptions() {
}

def getSubscriptions() {
}

