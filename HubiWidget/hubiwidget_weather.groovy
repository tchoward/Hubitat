import groovy.json.*;
/**
 *  HubiWidget Solar Widget Child App
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

// HubiWidget Solar Widget
// *****ALPHA BUILD

def file_key(){
    return "";
}

def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                                'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v1.0" }

definition(
    name: "HubiWidget Weather Widget",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "HubiWidget Weather Widget",
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
        
        path("/getOWData/") {
            action: [
                GET: "getOWData"
            ]
        }
        
        path("/getSensorData/") {
            action: [
                GET: "getSensorData"
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

def getSensorName(s){

    parse = s.tokenize('.');
    return [sensors.find{it.id == parse[0]}.label, parse[1]];
}


private getUnits(unit){
    if (unit == null)  return null;

    try{
        switch (unit.toLowerCase()){
            case "f":
            case "°f":
            case "fahrenheit":
                return [name: "fahrenheit", title: "Fahrenheit (°F)", type: "Temperature", units: atomicState.unitTemp]; break;
            case "c":
            case "°c":
            case "celcius":
                return [name: "celcius",  title: "Celsius (°C)", type: "temperature", units: atomicState.unitTemp]; break;
            case "mph": 
            case "miles_per_hour":
                return [name: "miles_per_hour", title: "Miles per Hour (mph)", type: "speed", units: atomicState.unitSpeed]; break;
            case "m/s":
            case "meters_per_second":
                return [name: "meters_per_second", title: "Meters per Second (m/s)", type: "speed", units: atomicState.unitSpeed]; break;
            case "in":
            case '"':
            case "inches":
                return [name: "inches", title: 'Inches (")', type: "depth", units: atomicState.unitDepth]; break;
            case "mm":
            case '"':
            case "millimeters":
                return [name: "millimeters", title: 'Millimeters (mm)', type: "depth", units: atomicState.unitDepth]; break;
            case "°":
            case "deg":
            case "degrees":
                return [name: "degrees", title: "Degrees (°)", type: "direction", units: atomicState.unitDirection]; break;
            case "rad":
            case "radians":
                return [name: "radians", title: "Radians (°)", type: "direction", units: atomicState.unitDirection]; break;
            case "inhg":
            case "inches_mercury":
                return [name: "inches_mercury", title: "Inches of Mercury (inHg)", type: "pressure", units: atomicState.unitPressure]; break;
            case "mmhg":
            case "millimeters_mercury":
                return [name: "millimeters_mercury", title: "Millimeters of Mecury mmHg)", type: "pressure", units: atomicState.unitPressure]; break;
            case "mbar":
            case "millibars":
                return [name: "millibars", title: "Millibars (mbar)", type: "pressure", units: atomicState.unitPressure]; break;
            case "km/h":
            case "kilometers":
                return [name: "kilometers", title: "Kilometers per hour (km/h)", type: "velocity", units: atomicState.unitSpeed]; break;
            case "hPa":
            case "hectopascal":
                return [name: "hectopascal", title: "Hectopascal (hPa)", type:"pressure", units: atomicState.unitPressure]; break;
            case "%":
            case "percent":
            case "percent_numeric":
            case "percent_decimal":
                return [name: "percent", title: "Percent (0 to 100)", type:"percent", units: atomicState.unitPercent]; 
            case "miles":
                return [name: "miles", title: "Miles", type: "distance", units: atomicState.unitDistance];
            break;
            case "time_seconds":
                return [name: "time", title: "Time (Seconds)", type:"seconds", units: atomicState.unitTime];
            default: 
                return null;  break;  
        } 
    } catch (error) {
            log.debug("Unable to find units: "+error);    
    } 
}

def outputUnitsSelection(var){
    
}

def setupUnits(){
   
    atomicState.unitTemp =       [["fahrenheit": "Fahrenheit (°F)"], ["celsius" : "Celsius (°C)"], ["kelvin" : "Kelvin (K)"]];
    atomicState.unitSpeed =      [["meters_per_second": "Meters per Second (m/s)"], ["miles_per_hour": "Miles per Hour (mph)"], ["knots": "Knots (kn)"], ["kilometers_per_hour": "Kilometers per Hour (km/h)"]];
    atomicState.unitDepth =      [["millimeters": "Millimeters (mm)"], ["inches": """Inches (") """]];
    atomicState.unitPressure =   [["millibars": "Millibars (mbar)"], ["millimeters_mercury": "Millimeters of Mercury (mmHg)"], ["inches_mercury": "Inches of Mercury (inHg)"], ["hectopascal" : "Hectopascal (hPa)"]];
    atomicState.unitDirection =  [["degrees": "Degrees (°)"], ["radians" : "Radians (°)"], ["cardinal": "Cardinal (N, NE, E, SE, etc)"]];
    atomicState.unitTrend =      [["trend_numeric": "Numeric (° < 0, ° = 0, ° > 0)"], ["trend_text": "Text (° rising, ° steady, ° falling)"]];
    atomicState.unitPercent =    [["percent_numeric": "Numeric (0 to 100)"], ["percent_decimal": "Decimal (0.0 to 1.0)"]];
    atomicState.unitTime =      [ ["time_seconds" : "Seconds since 1970"], ["time_milliseconds" : "Milliseconds since 1970"], 
                                  ["time_twelve_long" : "12 Hour Long (2:30 PM)"], ["time_military_long" : "24 Hour Long (14:30)"],
                                  ["time_twelve_med" : "12 Hour (2:30)"],
                                  ["time_twelve_short": "12 Hour Short (2)"], ["time_military_short":"24 Hour Short (14)"],
                                  ["time_day_long" : "Day of Week (Monday)"], ["time_day_short" : "Day of Week (Mon)"], 
                                  ["time_day_initial" : "Day of Week (M)"]
                                ];
    atomicState.unitUVI=         [["uvi" : "UV Index"]];
    atomicState.unitDistance=    [["miles": "Miles"]];
    atomicState.unitBlank=       [["none": "None"]];
    atomicState.unitDayofWeek=   [["short": "Short (Thu)"], ["long": "Long (Thursday)"]];
    atomicState.unitText=        [["plain": "Unformatted"], ["title": "Title Format"], ["lowercase": "Lowercase"], ["uppercase" : "Uppercase"]];
    atomicState.unitIcon=        [["icon": "Default Icon"]];

    atomicState.unitsSelection = [["unitTemp": "Temperature"], ["unitSpeed": "Wind Speed"], ["unitDepth": "Precipitation"],
                                  ["unitPressure": "Barometric Pressure"], ["unitDirection", "Wind Direction"], ["unitPercent", "Percent"],
                                  ["unitTime": "Time"], ["unitText": "Text"], ["icon": "Icon"], ["none": "Do Not Convert Value"]];
}

def openWeatherSelection(var, content){
    
    ow = getJsonData("OpenWeatherDefinition.json");

    if (content.open_weather && content.open_weather.time){

        app.updateSetting("${var}_ow_timeframe", [type:"enum", value: "${content.open_weather.time}"]);
        settings["${var}_ow_timeframe"] = "${content.open_weather.time}";
    }

    input( type: "enum", name: "${var}_ow_timeframe", title: "Open Weather Timeframe ", required: true, multiple: false, 
            options: [["current": "Current"], ["daily" : "Daily"], ["hourly" : "Hourly"]], defaultValue: "", submitOnChange: true);
                
    if (settings["${var}_ow_timeframe"]){

        timeframe = settings["${var}_ow_timeframe"];

        attrs = [];
        ow.each{ key, opts ->

            if (opts."${timeframe}" == "yes"){
                attrs << [ "${key}" : "${opts.name}" ];
            }
        }
            
        if (content.open_weather && content.open_weather.attribute){
                app.updateSetting("${var}_ow_device", [type:"enum", value: "${content.open_weather.attribute}"]);
                settings["${var}_ow_device"] = "${content.open_weather.attribute}";
        }
        
        input( type: "enum", name: "${var}_ow_device", title: "Weather Data Attribute ", required: true, multiple: false, 
                options: attrs, defaultValue: settings["${var}_ow_device"], submitOnChange: true);
        
        if (content.open_weather && content.open_weather.range){
            app.updateSetting("${var}_ow_range", [type:"string", value: "${content.open_weather.range.value}"]);
            settings["${var}_ow_range"] = "${content.open_weather.range.value}";  

            paragraph("Range of ${content.open_weather.range.value} selected from JSON file")
            
        } else {
            if (content.open_weather && content.open_weather.offset){
                app.updateSetting("${var}_owtime_offset", [type:"enum", value: "${content.open_weather.offset}"]);
                settings["${var}_ow_timeoffset"] = "${content.open_weather.offset}";
            }
            
            time_offset = 0;
            if (timeframe == "daily") {
                time_offset = 0..7;
                time_string = "Days";
            }
            if (timeframe == "hourly") {
                time_offset = 0..48;
                time_string = "Hours";
            }

            if (time_offset != 0){
                input( type: "enum", name: "${var}_ow_timeoffset", title: "Weather Data ${time_string} from current", required: true, multiple: false, 
                options: time_offset, defaultValue: settings["${var}_ow_timeoffset"], submitOnChange: false);
            }
        }
        
        if (settings["${var}_ow_device"]){
            dev = settings["${var}_ow_device"]
            opts = ow."${dev}"
            if (opts.type != "icon"){

                if (content.open_weather && content.open_weather.output_units){
                        app.updateSetting("${var}_output_units", [type:"enum", value: "${content.open_weather.output_units}"]);
                        settings["${var}_output_units"] = "${content.open_weather.output_units}";
                }

                units = getUnits(opts.in_units);

                paragraph("Open Weather natively uses <b>${units.title}</b> for this device")


                input( type: "enum", name: "${var}_output_units", title: "Select Display Units", 
                        required: true, multiple: false, options: units.units, submitOnChange: false);

                app.updateSetting("${var}_input_units", [type:"text", value: "${opts.in_units}"])
                app.updateSetting("${var}_ow_string", [type:"text", value: "${opts.ow}"]);
            
            } else {
            
                app.updateSetting("${var}_input_units", [type:"text", value: "text"]);
                app.updateSetting("${var}_output_units", [type:"text", value: "icon"]);
                app.updateSetting("${var}_ow_string", [type:"text", value: "${opts.ow}"]);
            
            }
        }
        
    }
}

def sensorSelection(var, content){

    sensor = settings["${var}_sensor"];

    try {
        attributes_ = sensor.getSupportedAttributes();
        final_attrs = [];

        attributes_.each{ attribute_->
            name = attribute_.getName();
            if (sensor.currentState(name)){
                final_attrs << ["$name" : "$name ::: [${sensor.currentState(name).getValue()}]"];
            }
        }
        final_attrs = final_attrs.unique(false);
    } catch (e){
        final_attrs = [1: "ERROR"];
        log.debug(e);
    }

    input( type: "enum", name: "${var}_attribute", title: "Attribute to show", 
           required: true, multiple: false, options: final_attrs, defaultValue: "1", submitOnChange: true);

    if (settings["${var}_attribute"]){
        attribute = settings["${var}_attribute"];
        units = getUnits(sensor.currentState(attribute).getUnit());

        if (units != null){
            paragraph("Detected units <b>${units.title}</b>")
            app.updateSetting("${var}_input_units" , units.name);
            if (content.conversion) 
                units.units << ["icon" : "Icons (Based on Conversion Value)"]
            
            input( type: "enum", name: "${var}_output_units", title: "Output Units", 
                required: true, multiple: false, options: units.units, submitOnChange: false);
        
        } else{

            paragraph("No Detected Units for this Sensor<br><small>To facilitate auto units conversion, please select below")
            input( type: "enum", name: "${var}_sensor_type", title: "Sensor Type", 
                   required: true, multiple: false, options: atomicState.unitsSelection, submitOnChange: true);
            
            if (settings["${var}_sensor_type"] && 
                settings["${var}_sensor_type"]!="none" &&
                settings["${var}_sensor_type"]!="icon"){

                sensorType = settings["${var}_sensor_type"];
                input( type: "enum", name: "${var}_input_units", title: "Sensor Input Units", 
                    required: true, multiple: false, options: atomicState[sensorType], submitOnChange: true);
                input( type: "enum", name: "${var}_output_units", title: "Sensor Output Units", 
                    required: true, multiple: false, options: atomicState[sensorType], submitOnChange: false);
                
            } else if (settings["${var}_sensor_type"]=="icon") {
                app.updateSetting("${var}_input_units" , [type:"enum", value: "none"]);
                app.updateSetting("${var}_output_units" , [type:"enum", value: "icon"]);
            } else {
                app.updateSetting("${var}_input_units" , [type:"enum", value: "none"]);
                app.updateSetting("${var}_output_units" , [type: "enum", value: "none"]);
            }
        }
    }
}

def deviceSelectionPage() {

    dynamicPage(name: "deviceSelectionPage") {
        
        setupUnits();

        parent.hubiForm_section(this,"Load JSON HubiWidget Weather Description File", 1){
            container = [];

            container << parent.hubiForm_text_input(this,   "Input File Name Located at /local/",
                                                                "filename",
                                                                "",
                                                                true);

            parent.hubiForm_container(this, container, 1); 
        }
        openweather = parent.getOpenWeatherChild();

        if (filename){
            atomicState.json = getJsonData(filename);

            if (atomicState.json){

                atomicState.json.widgets.each{widget->
                    widget.content.each{content->
                        if (content.type == 'sensor'){
                            var = widget.name.replaceAll(' ', '_');
                            parent.hubiForm_section(this,"Choose Sensor for ${widget.name} Widget", 1){
                
                                if (content.open_weather){
                                    app.updateSetting("${var}_sensor", [type:"enum", value: openweather]);
                                    settings["${var}_sensor"] = "${openweather}";
                                    paragraph("<b>${openweather.label}</b> selected in JSON config");

                                } else {
                                    input "${var}_sensor", "capability.*", title: "$widget.name Sensor", 
                                    multiple: false, required: true, submitOnChange: false
                                }
                                
                                if (settings["${var}_sensor"]){
                                    if (openweather && "${openweather}" == "${settings["${var}_sensor"]}"){
                                        openWeatherSelection(var, content);
                                        app.updateSetting ("${var}_ow", "true");
                                    } else {
                                        sensorSelection(var, content);
                                        app.updateSetting ("${var}_ow", "false");
                                        
                                    }
                                }
                            }
                        }
                    }
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

def getJsonData(filename_){

    uri = "http://${location.hub.localIP}:8080/local/${filename_}"

    def params = [
        uri: uri,
        textParser: true,
    ]

    try {
        httpGet(params) { resp ->
            if(resp!= null) {
                data = resp.getData();
                def jsonSlurper = new JsonSlurper();
                parse = jsonSlurper.parseText("${data}");
                
                return parse;
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Connection Exception: ${exception.message}"
        return null;
    }
}

def mainPage() {

    dynamicPage(name: "mainPage") {        
       
            def container = [];
            
            if (filename)
                atomicState.json = getJsonData(filename);

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
                
                
                if (atomicState.json){
                     parent.hubiForm_section(this, "Preview", 10, "show_chart"){                         
                         container = [];
                         container << parent.hubiForm_graph_preview(this)
                         
                         parent.hubiForm_container(this, container, 1); 
                     } //graph_timespan
            
                    parent.hubiForm_section(this, "HubiWidget Tile Installation", 2, "apps"){
                        container = [];
                             
                        container << parent.hubiForm_switch(this, title: "Install HubiWidget Tile Device?", name: "install_device", default: false, submit_on_change: true);
                        if (install_device==true){ 
                             container << parent.hubiForm_text_input(this, "Name for HubiWidget Tile Device", "device_name", "HubiWidget Tile", "false");
                        }
                        parent.hubiForm_container(this, container, 1); 
                    }
                } 
             
            
               if (state.endpoint){
                   parent.hubiForm_section(this, "HubiWidget Application", 1, "settings"){
                        container = [];
                        container << parent.hubiForm_sub_section(this, "Application Name");
                        container << parent.hubiForm_text_input(this, "Rename the Application?", "app_name", "HubiWidget Time Graph", "false");
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


    resp = atomicState.json;


    resp.widgets.each{widget->
        widget.content.each{content->
            var = widget.name.replaceAll(' ', '_');

            if (content.type == "sensor"){
                if (settings["${var}_ow"]=="true"){
                    timeframe = settings["${var}_ow_timeframe"];
                    endString = settings["${var}_ow_string"];

                    if (settings["${var}_ow_range"]) timespan = "%";
                    else timespan = settings["${var}_ow_timeoffset"] ? settings["${var}_ow_timeoffset"] : null;
                    
                    ow_string = timespan ? "${timeframe}.${timespan}.${endString}" :"${timeframe}.${endString}"; 

                    if (!content.open_weather) content.open_weather = [string: ow_string];
                    else content.open_weather.string = ow_string;
                
                } else {
                            
                    content << [device_id: settings["${var}_sensor"].id];
                    content << [attribute: settings["${var}_attribute"]];

                }
                content << [input_units: settings["${var}_input_units"]];
                content << [output_units: settings["${var}_output_units"]];

            }
        }
    }

    icons = getJsonData("HubiWeatherIcons.json");
    resp << [icons: icons];

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
            <script src="/local/${file_key()}HubiWidgetWeather.js"></script>

            <script type="text/javascript">

            function getOWData() {
                 console.log("Recieved Open Weather Data")
                 return jQuery.get("${state.localEndpointURL}getOWData/?access_token=${state.endpointSecret}", (data) => {
                    ow_data = new Map(Object.entries(data));
                });
            }

            function getSensorData() {
                return jQuery.get("${state.localEndpointURL}getSensorData/?access_token=${state.endpointSecret}", (data) => {
                    console.log("Recieved Sensor Data");
                    sensor_data = new Map(Object.entries(data));
                });
            }

            function getGraphData() {
                return jQuery.get("${state.localEndpointURL}getData/?access_token=${state.endpointSecret}", (data) => {
                    widget_data = new Map(Object.entries(data));

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

def getOWData() {
    data = parent.getOpenWeatherData();
    //data = parseJson(data);
    return render(contentType: "text/json", data: data);
}

def getSensorData() {
    json = atomicState.json;

    resp = [];

    json.widgets.each{widget->
        widget.content.each{content->
            var = widget.name.replaceAll(' ', '_');

            if (content.type == "sensor"){
                if (settings["${var}_ow"]!="true"){
                    sensor = settings["${var}_sensor"];
                    attribute = settings["${var}_attribute"];

                    resp << [
                                deviceId: sensor.id,
                                name: attribute,
                                value: sensor.currentValue(attribute)
                            ];

                }
            }
        }
    }
    return resp;
}

