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

// Hubigraph Weather Tile Changelog
// V0.01 - Proof of Concept (Minimal Functionality)

// Credit to Alden Howard for optimizing the code.

 
def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v1.0" }

definition(
    name: "Hubigraph Weather Tile",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Weather Tile",
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
       page(name: "tileSetupPage", nextPage: "mainPage")
       page(name: "enableAPIPage")
       page(name: "disableAPIPage")
}
   

    mappings {
        path("/graph/") {
            action: [
                GET: "getTile"
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

def tileSetupPage(){
      
    def updateEnum = [["60000":"1 Minute"],["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1200000":"20 Minutes"], ["1800000":"Half Hour"], 
                      ["3600000":"1 Hour"], ["6400000":"2 Hours"], ["19200000":"6 Hours"], ["43200000":"12 Hours"], ["86400000":"1 Day"]];
    
    def unitEnum =       [["imperial":"Imperial (°F, mph, in, inHg, 0:00 am)"], ["metric":"Metric (°C, m/sec, mm, mmHg, 00:00)"]]; 
    def unitTemp =       [["fahrenheit": "Fahrenheit (°F)"], ["celsius" : "Celsius (°C)"], ["kelvin" : "Kelvin (K)"]];
    def unitWind =       [["meters_per_second": "Meters per Second (m/s)"], ["miles_per_hour": "Miles per Hour (mph)"], ["knots": "Knots (kn)"], ["kilometers_per_hour": "Kilometers per Hour (km/h)"]];
    def unitLength =     [["millimeters": "Millimeters (mm)"], ["inches": """Inches (") """]];
    def unitPressure =   [["millibars": "Millibars (mbar)"], ["millimeters_mercury": "Millimeters of Mercury (mmHg)"], ["inches_mercury": "Inches of Mercury (inHg)"], ["hectopascal" : "Hectopascal (hPa)"]];
    def unitDirection =  [["degrees": "Degrees (°)"], ["radians", "Radians (°)"], ["cardinal": "Cardinal (N, NE, E, SE, etc)"]];
    def unitTrend =      [["trend_numeric": "Numeric (↑ < 0, → = 0, ↓ > 0)"], ["trend_text": "Text (↑ rising, → steady, ↓ falling)"]];
    def unitPercent =    [["percent_numeric": "Numeric (0 to 100)"], ["percent_decimal": "Decimal (0.0 to 1.0)"]];
    def unitTime =       [["time_milliseconds" : "Milliseconds since 1970"], ["time_twelve" : "12 Hour (2:30 PM)"], ["time_two_four" : "24 Hour (14:30)"]];
    
    def unit_selections = [[title: 'Current Temperature',     var: "display_current_temp",           unit: unitTemp,      imperial: "fahrenheit",      metric: "celsius"],
                           [title: 'Feels Like',              var: "display_feels_like",             unit: unitTemp,      imperial: "fahrenheit",      metric: "celsius"],
                           [title: 'Forecast High',           var: "display_forcast_high",           unit: unitTemp,      imperial: "fahrenheit",      metric: "celsius"],
                           [title: 'Forecast Low',            var: "display_forecast_low",           unit: unitTemp,      imperial: "fahrenheit",      metric: "celsius"],
                           [title: 'Forecast Daily Rainfall', var: "display_forecast_precipitation", unit: unitLength,    imperial: "inches",          metric: "millimeters"],
                           [title: 'Actual Daily Rainfall',   var: "display_actual_precipitation",   unit: unitLength,    imperial: "inches",          metric: "millimeters"],
                           [title: 'Forecast Rainfall Chance',var: "display_chance_precipitation",   unit: unitPercent,   imperial: "percent_numeric", metric: "percent_numeric"],
                           [title: 'Wind Speed',              var: "display_wind_speed",             unit: unitWind,      imperial: "miles_per_hour",  metric: "meters_per_second"],
                           [title: 'Wind Gust',               var: "display_wind_gust",              unit: unitWind,      imperial: "miles_per_hour",  metric: "meters_per_second"],
                           [title: 'Wind Direction',          var: "display_wind_direction",         unit: unitDirection, imperial: "cardinal",        metric: "cardinal"], 
                           [title: 'Current Pressure',        var: "display_current_pressure",       unit: unitPressure,  imperial: "inches_mercury",  metric: "millimeters_mercury"],
                           [title: 'Dew Point',               var: "display_dew_point",              unit: unitTemp,      imperial: "fahrenheit",      metric: "celsius"],
                           [title: 'Humidity',                var: "display_humidity",               unit: unitPercent,   imperial: "percent_numeric", metric: "percent_numeric"],
                           [title: 'Time Format',             var: "display_time_format",            unit: unitTime,      imperial: "time_twelve",     metric: "time_two_four"]

                          ];


    def location = getLocation();
    
    dynamicPage(name: "tileSetupPage") {      
      
        parent.hubiForm_section(this,"General Options", 1)
        {      
            input( type: "enum", name: "openweather_refresh_rate", title: "<b>Select OpenWeather Update Rate</b>", multiple: false, required: true, options: updateEnum, defaultValue: "300000");
            if (override_openweather){
                input( type: "enum", name: "pws_refresh_rate", title: "<b>Select PWS Update Rate</b>", multiple: false, required: true, options: updateEnum, defaultValue: "300000");        
            }
            container = [];
            container << parent.hubiForm_text_input (this, "<b>Open Weather Map Key</b>", "tile_key", "", false);
            
            container << parent.hubiForm_text_input (this, "<b>Latitude (Default = Hub location)</b>", "latitude", location.latitude, false);
            container << parent.hubiForm_text_input (this, "<b>Longitude (Default = Hub location)</b>", "longitude", location.longitude, false);
            container << parent.hubiForm_color(this, "Background", 
                                                     "background", 
                                                     "#000000", 
                                                     false);
            container << parent.hubiForm_slider     (this, title: "Background Opacity", 
                                                           name:  "background_opacity",  
                                                           default: 90, 
                                                           min: 0,
                                                           max: 100, 
                                                           units: "%",
                                                           submit_on_change: false);
            
            container << parent.hubiForm_color(this, "Text", 
                                                     "text", 
                                                     "#FFFFFF", 
                                                     false);
                         
            parent.hubiForm_container(this, container, 1);     
        } 
        
        parent.hubiForm_section(this,"Display Options", 1)
        {
            container = [];
            def globalEnum =       [["blank": "Choose to Fill"],["imperial":"Imperial (°F, mph, in, inHg, 0:00 am)"], ["metric":"Metric (°C, m/sec, mm, mmHg, 00:00)"]];
            def decimalEnum =     [[0: "None (0)"], [1: "One (0.1)"], [2: "Two (0.12)"], [3: "Three (0.123)"], [4: "Four (0.1234)"]];
                                     
            setUnits = null;
            if (prefill != "blank"){
                if (prefill){
                    setUnits = prefill;    
                } else {
                    setUnits = "imperial";
                }  
                app.updateSetting("prefill", [type: "enum", value: "blank"]);
            }
            input( type: "enum", name: "prefill", title: "<b>Prefill Units</b>", required: false, multiple: false, options: globalEnum, defaultValue: tile.default, submitOnChange: true)
            
            unit_selections.each{attribute->
                
                if (setUnits != null) {
                    setting = "${attribute[setUnits]}";
                    var = attribute.var;
                    app.updateSetting(var, [type: "enum", value: setting]);
                    log.debug("Var: $var Setting: $setting");              
                }
                container = [];
                container <<  parent.hubiForm_sub_section(this, attribute.title+" Display");
                parent.hubiForm_container(this, container, 1);
                input( type: "enum", name: attribute.var, title: "Units", required: false, multiple: false, options: attribute.unit, defaultValue: attribute.imperial, submitOnChange: false)
                input( type: "enum", name: attribute.var+"decimal_places", title: "Decimal Places", required: false, multiple: false, options: decimalEnum, defaultValue: 1, submitOnChange: false)
            }
        }
            
    }//page
}//function

def deviceSelectionPage() {
    def final_attrs;
    def unitTemp =       [["fahrenheit": "Fahrenheit (°F)"], ["celsius" : "Celsius (°C)"], ["kelvin" : "Kelvin (K)"]];
    def unitWind =       [["meters_per_second": "Meters per Second (m/s)"], ["miles_per_hour": "Miles per Hour (mph)"], ["knots": "Knots (kn)"], ["kilometers_per_hour": "Kilometers per Hour (km/h)"]];
    def unitLength =     [["millimeters": "Millimeters (mm)"], ["inches": """Inches (") """]];
    def unitPressure =   [["millibars": "Millibars (mbar)"], ["millimeters_mercury": "Millimeters of Mercury (mmHg)"], ["inches_mercury": "Inches of Mercury (inHg)"], ["hectopascal" : "Hectopascal (hPa)"]];
    def unitDirection =  [["degrees": "Degrees (°)"], ["radians", "Radians (°)"], ["cardinal", "Cardinal (N, NE, E, SE, etc)"]];
    def unitTrend =      [["trend_numeric": "Numeric (↑ < 0, → = 0, ↓ > 0)"], ["trend_text": "Text (↑ rising, → steady, ↓ falling)"]];
    def unitPercent =    [["percent_numeric": "Numeric (0 to 100)"], ["percent_decimal": "Decimal (0.0 to 1.0)"]];
    
    def tile_attributes = [[title: '"Current Temperature"', var: "override_current_temp",     unit: unitTemp,      default: "fahrenheit"],
                           [title: '"Feels Like"',          var: "override_feels_like",       unit: unitTemp,      default: "fahrenheit"],
                           [title: '"Precipitation Today"', var: "override_daily_precip",     unit: unitLength,    default: "inches"],
                           [title: '"Wind Speed"',          var: "override_wind_speed",       unit: unitWind,      default: "miles_per_hour"],
                           [title: '"Wind Gust"',           var: "override_wind_gust",        unit: unitWind,      default: "miles_per_hour"],
                           [title: '"Wind Direction"',      var: "override_wind_direction",   unit: unitDirection, default: "degrees"],
                           [title: '"Current Pressure"',    var: "override_current_pressure", unit: unitPressure,  default: "inches_mercury"],
                           [title: '"Pressure Trend"',      var: "override_pressure_trend",   unit: unitTrend,     default: "trend_text"],
                           [title: '"Humidity"',            var: "override_humidity",         unit: unitPercent,   defualt: "percent_numeric"], 
                           [title: '"Dew Point"',           var: "override_dew_point",        unit: unitTemp,      default: "fahrenheit"]
                          ];
       
    dynamicPage(name: "deviceSelectionPage") {
         parent.hubiForm_section(this,"Override OpenWeather", 1){
             container = [];
             container << parent.hubiForm_switch(this, title: "Override OpenWeatherMap values with PWS?", name: "override_openweather", default: false, submit_on_change: true);
             parent.hubiForm_container(this, container, 1);
         }

         if (override_openweather == true){
             parent.hubiForm_section(this,"Sensor Selection", 1){ 
                 container = [];

                 input ("sensor", "capability.*", title: "Select PWS", multiple: false, required: false, submitOnChange: true);
                 parent.hubiForm_container(this, container, 1); 
             }
             
             if (sensor){
                 attributes_ = sensor.getSupportedAttributes();
                 final_attrs = [];
                 final_attrs << ["openweather" : "Open Weather Map"];
                    
                 attributes_.each{ attribute_->
                    name = attribute_.getName();
                    if (sensor.currentState(name)){
                        final_attrs << ["$name" : "$name ::: [${sensor.currentState(name).getValue()} ${sensor.currentState(name).getUnit() ? sensor.currentState(name).getUnit() : ""} ]"];
                    }
                  }
                  final_attrs = final_attrs.unique(false);
                  
                  parent.hubiForm_section(this,"Override Values - Select Attribute Units", 1){ 
                   
                    tile_attributes.each{tile->
                        container = [];
                        container <<  parent.hubiForm_sub_section(this, tile.title);
                        parent.hubiForm_container(this, container, 1); 
                        input( type: "enum", name: tile.var, title: tile.title, required: false, multiple: false, options: final_attrs, defaultValue: "openweather", submitOnChange: true)
                        container = [];
                        if (settings[tile.var] != "openweather" && tile.unit != null){
                            unit = sensor.currentState(settings[tile.var]).getUnit();
                            value = sensor.currentState(settings[tile.var]).getValue();
                            
                            if (unit==null) unit = "blank";
                            
                            detectedUnits = getUnits(unit, value);                     
                            
                            validUnits = tile.unit.any{ detectedUnits.var in it };
                                
                            if (detectedUnits.name != "unknown" && validUnits){
                                container <<  parent.hubiForm_text(this, "Detected units = "+detectedUnits.name);
                                app.updateSetting("${tile.var}_units", detectedUnits.var);
                                parent.hubiForm_container(this, container, 1);
                            } else {
                                if (detectedUnits.name == "unknown"){
                                    container <<  parent.hubiForm_text(this, "Unknown Units, Please update Below");
                                } else {
                                    container <<  parent.hubiForm_text(this, "<span style='color:red'>Error: Units detected = ${detectedUnits.name}</span><br><small>Please choose a different device above, or override the units below</small>");
                                }
                                parent.hubiForm_container(this, container, 1);
                                input( type: "enum", name: tile.var+"_units", title: "Override "+tile.title+" Units", required: false, multiple: false, options: tile.unit, defaultValue: tile.default, submitOnChange: false)
                            } 
                        } else {
                            app.updateSetting("${tile.var}_units", null);
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
                    container << parent.hubiForm_page_button(this, "Configure Tile", "tileSetupPage", "100%", "poll");              
                    parent.hubiForm_container(this, container, 1); 
                }
                
                parent.hubiForm_section(this, "Local Graph URL", 1, "link"){
                    container = [];
                    container << parent.hubiForm_text(this, "${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
                    
                    parent.hubiForm_container(this, container, 1); 
                }
                
                  
                
                if (tile_key){
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
def getDays(str){

    switch (str){
        case "1 Day":      return 1; break;
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
    
    if (lts){
        switch (lts_update){
            case "1:05 am": schedule("0 05 01 * * ?", longTermStorageUpdate); break;
            case "2:30 am": schedule("0 30 02 * * ?", longTermStorageUpdate); break;
            case "3:45 am": schedule("0 45 04 * * ?", longTermStorageUpdate); break;
            case "4:55 am": schedule("0 55 04 * * ?", longTermStorageUpdate); break;    
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

private getAbbrev(unit){
    
    switch (unit){
        case "fahrenheit": return "°";
        case "celsius": return "°";
        case "kelvin": return "K";
        case "meters_per_second": return "m/s";
        case "miles_per_hour": return "mph";
        case "knots": return "kn";
        case "millimeters": return "mm";
        case "inches": return '"';
        case "degrees": return "°";
        case "radians": return "rad";
        case "cardinal": return "";
        case "trend_numeric": return "";
        case "trend_text": return "";
        case "percent_numeric": return "";
        case "millibars": return "mbar";
        case "millimeters_mercury": return "mmHg";
        case "inches_mercury": return "inHg";
        case "hectopascal": return "hPa";
        case "kilometers_per_hour" : return "km/h";
        

        
    }
    return "unknown";
}

private getUnits(unit, value){
    
    switch (unit.toLowerCase()){
        case "f":
        case "°f":
            return [name: "Farenheit (°F)", var: "fahrenheit"]; break;
         case "c":
        case "°c":
            return [name: "Celsius (°C)", var: "celsius"]; break;
        case "mph": 
            return [name: "Miles per Hour (mph)", var: "miles_per_hour"]; break;
        case "m/s":
            return [name: "Meters per Second (m/s)", var: "meters_per_second"]; break;
        case "in":
        case '"':
            return [name: 'Inches (")', var: "inches"]; break;
        case "°":
        case "deg":
            return [name: "Degrees (°)", var: "degrees"]; break;
        case "rad":
            return [name: "Radians (°)", var: "radians"]; break;
        case "inhg":
            return [name: "Inches of Mercury (inHg)", var: "inches_mercury"]; break;
        case "mmhg":
            return [name: "Millimeters of Mecury mmHg)", var: "millimeters_mercury"]; break;
        case "mbar":
            return [name: "Millibars (mbar)", var: "millibars"]; break;
        case "km/h":
            return [name: "Kilometers per hour (km/h)", var: "kilometers_per_hour"]; break;
        case "hPa":
            return [name: "Hectopascal (hPa)", var: "hectopascal"]; break;
        case "%":
            value = Double.parseDouble(value);
            if (value > 1.0 && value < 100.0) {
                return [name: "Percent (0 to 100)", var: "percent_numeric"]; break;    
            } else if (value >=0.0 && value < 1.0) {
                return [name: "Percent (0.1 to 1.0)", var: "percent_decimal"]; break;  
            } else {
                return [name: "unknown", var: "tbd"];  break;  
            }
    }
    
    switch (value.toLowerCase()){
       case "falling":
       case "rising" :
       case "steady" :
            return [name: "Text Status (Rising, Steady, Falling)", var: "trend_text"]; break;
       case "n": case "nne": case "ne": case "ene": case "e": case "ese":
       case "se": case "sse": case "s": case "ssw": case "sw":  case "wsw":
       case "w":  case "wnw":  case "nw": case  "nnw":
            return [name: "Cardinal Coordinates (N, S, E, W)", var: "cardinal"]; break;
    }
    return [name: "unknown", var: "tbd"];
}

private getPWSData() {
    def resp = [:]
    def units;
    
    if (override_openweather){
        if (override_current_temp != "openweather") {
            resp << ["current_temperature":         [ "value" : sensor.currentValue(override_current_temp),     "units": settings["override_current_temp_units"] ]];
        }
        if (override_wind_speed != "openweather") {
            resp << ["current_wind_speed":          [ "value" : sensor.currentValue(override_wind_speed),       "units": settings["override_wind_speed_units"]]];
        }
        if (override_feels_like != "openweather") {
            resp << ["current_feels_like":          [ "value" : sensor.currentValue(override_feels_like	),       "units": settings["override_feels_like_units"]]];
        }
        if (override_wind_gust != "openweather") {       
            resp << ["current_wind_gust":           [ "value" : sensor.currentValue(override_wind_gust),        "units": settings["override_wind_gust_units"]]];
        }
        if (override_wind_direction != "openweather") {       
            resp << ["current_wind_direction":      [ "value" : sensor.currentValue(override_wind_direction),   "units": settings["override_wind_direction_units"]]];
        }
        if (override_current_pressure != "openweather") {      
            resp << ["current_pressure":            [ "value" : sensor.currentValue(override_current_pressure), "units": settings["override_current_pressure_units"]]];
        }
        if (override_humidity != "openweather") {      
            resp << ["current_humidity":            [ "value" : sensor.currentValue(override_humidity),         "units": settings["override_humidity_units"]]];
        }
        if (override_dew_point != "openweather") {      
            resp << ["current_dewpoint":            [ "value" : sensor.currentValue(override_dew_point),        "units": settings["override_dew_point_units"]]];
        }
        if (override_daily_precip != "openweather") {      
            resp << ["current_precipitation":       [ "value" : sensor.currentValue(override_daily_precip),     "units": settings["override_daily_precip_units"]]];
        }
        if (override_pressure_trend != "openweather") {      
            resp << ["current_pressure_trend":  [ "value" : sensor.currentValue(override_pressure_trend),   "units": settings["override_pressure_trend_units"]]];
        }
    }
    
    return resp;
}

def getTileOptions(){  
    
    def options = [
        "tile_units": tile_units,
        "openweather_refresh_rate": openweather_refresh_rate,
        "pws_refresh_rate": override_openweather ? pws_refresh_rate : null,
        "override" : [  "sensor_id" :            override_openweather ? sensor.id : null,
                        "current_pressure" :     override_openweather ? override_current_pressure : "openweather",
                        "current_temp" :         override_openweather ? override_current_temp : "openweather",
                        "daily_precip" :         override_openweather ? override_daily_precip : "openweather",
                        "dew_point":             override_openweather ? override_dew_point : "openweather",
                        "feels_like":            override_openweather ? override_feels_like : "openweather",
                        "humidity":              override_openweather ? override_humidity : "openweather",
                        "pressure_trend":        override_openweather ? override_pressure_trend : "openweather",
                        "wind_direction":        override_openweather ? override_wind_direction : "openweather",
                        "wind_gust":             override_openweather ? override_wind_gust : "openweather",
                        "wind_speed":            override_openweather ? override_wind_speed : "openweather",
                      ],
        "display"  : [ "current_temp":             [ "unit" : display_current_temp,            "decimal" : display_current_tempdecimal_places],
                       "feels_like"  :             [ "unit" : display_feels_like,              "decimal" : display_feels_likedecimal_places],
                       "forcast_high":             [ "unit" : display_forcast_high,            "decimal" : display_forcast_highdecimal_places],  
                       "forecast_low":             [ "unit" : display_forecast_low,            "decimal" : display_forecast_lowdecimal_places],
                       "forecast_precipitation" :  [ "unit" : display_forecast_precipitation,  "decimal" : display_forecast_precipitationdecimal_places],  
                       "actual_precipitation" :    [ "unit" : display_actual_precipitation,    "decimal" : display_actual_precipitationdecimal_places],
                       "chance_precipitation" :    [ "unit" : display_chance_precipitation,    "decimal" : display_chance_precipitationdecimal_places],
                       "wind_speed" :              [ "unit" : display_wind_speed,              "decimal" : display_wind_speeddecimal_places],
                       "wind_gust" :               [ "unit" : display_wind_gust,               "decimal" : display_wind_gustdecimal_places],
                       "wind_direction"  :         [ "unit" : display_wind_direction,          "decimal" : display_wind_directiondecimal_places],
                       "current_pressure" :        [ "unit" : display_current_pressure,        "decimal" : display_current_pressuredecimal_places],
                       "dew_point"  :              [ "unit" : display_dew_point,               "decimal" : display_dew_pointdecimal_places],
                       "humidity"  :               [ "unit" : display_humidity,                "decimal" : display_humiditydecimal_places],
                       "time_format"  :            [ "unit" : display_time_format,             "decimal" : display_time_formatdecimal_places],
                       "pressure_trend" :      [ "unit" : "trend_text",                    "decimal" : "none"],
                      ]
    ];
        
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

def defineHTML_Header(){
    def html = """
    <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
    <link rel="stylesheet" href="https://code.getmdl.io/1.3.0/material.indigo-pink.min.css">
    <link rel="stylesheet" href="//cdn.materialdesignicons.com/5.4.55/css/materialdesignicons.min.css">
    
    <script defer src="https://code.getmdl.io/1.3.0/material.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js"></script>
    """
    return html;
}

def defineHTML_CSS(){
    def html = """
        .grid-container {
      display: grid;
      grid-template-columns: 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw;
      grid-template-rows: 30vmin 6vmin 6vmin 8vmin 3vmin 8vmin 7vmin 7vmin 7vmin 6vmin 6vmin 6vmin;
      grid-gap: 0px;
      align-items: center;
      background-color: ${getRGBA(background_color, background_opacity)};
    }

    .grid-container > div {
      text-align: center;
      color: ${text_color};
    }

    .weather_icon {
      grid-row-start: 1;
      grid-row-end: 3;
      grid-column-start: 13;
      grid-column-end: 24;
      font-size: 10vmin;
      padding-top: 0vmin !important; 
      padding-left: 00vmin !important;
      text-align: center !important;
    }

    .current_condition1 {
      grid-row-start:2;
      grid-row-end: 2;
      grid-column-start: 13;
      grid-column-end: 24;
      font-size: 5vmin;
      text-align: center !important;
      line-height: 1;
    }

    .current_condition2 {
      grid-row-start: 3;
      grid-row-end: 3;
      grid-column-start: 13;
      grid-column-end: 24;
      font-size: 5vmin;
      text-align: center !important;
      line-height: 1;
    }

    .current_temperature {
      font-weight: 900;
      grid-row-start: 1;
      grid-row-end: 1;
      grid-column-start: 2;
      grid-column-end: 15;
      font-size: 20vmin;
      text-align: center !important;
      padding-top: 10vmin !important; 
    }

    .current_feels_like{
      grid-row-start: 3;
      grid-row-end: 3;
      grid-column-start: 2;
      grid-column-end: 14;
      font-size: 5vmin;
      text-align: center !important;  
    }

    .forecast_low{
      grid-row-start: 4;
      grid-row-end: 4;
      grid-column-start: 3;
      grid-column-end: 8;
      font-size: 5vmin;
      text-align: center !important;
    }

    .forecast_high{
      grid-row-start: 4;
      grid-row-end: 4;
      grid-column-start: 8;
      grid-column-end: 13;
      font-size: 5vmin;
      text-align: center !important;
    }

    .precipitation_title{
      grid-row-start: 6;
      grid-row-end: 6;
      grid-column-start: 2;
      grid-column-end: 9;
      font-size: 4vmin;
      text-align: left !important;
      line-height: 1; 
      border-bottom: 1px solid ${text_color};
    }

    .forecast_precipitation_chance{
      grid-row-start: 7;
      grid-row-end: 7;
      grid-column-start: 2;
      grid-column-end: 9;
      font-size: 4vmin;
      text-align: left !important;
    }

    .forecast_precipitation{
      grid-row-start: 8;
      grid-row-end: 8;
      grid-column-start: 2;
      grid-column-end: 9;
      font-size: 4vmin;
      text-align: left !important;
    }

    .current_precipitation{
      grid-row-start: 9;
      grid-row-end: 9;
      grid-column-start: 2;
      grid-column-end: 9;
      font-size: 4vmin;
      text-align: left !important;
    }

    .wind_title{
      grid-row-start: 6;
      grid-row-end: 6;
      grid-column-start: 10;
      grid-column-end: 17;
      font-size: 4vmin;
      text-align: left !important;
      line-height: 1;
      border-bottom: 1px solid ${text_color}; 
    }

.current_wind_speed{
  grid-row-start:7;
  grid-row-end: 7;
  grid-column-start: 10;
  grid-column-end: 17;
  font-size: 4vmin;
  text-align: left !important;
}
.current_wind_gust{
  grid-row-start: 8;
  grid-row-end: 8;
  grid-column-start: 10;
  grid-column-end: 17;
  font-size: 4vmin;
  text-align: left !important;
}
.current_wind_direction{
  grid-row-start: 9;
  grid-row-end: 9;
  grid-column-start: 10;
  grid-column-end: 17;
  font-size: 4vmin;
  text-align: left !important;
}

.pressure_title{
  grid-row-start: 6;
  grid-row-end: 6;
  grid-column-start: 18;
  grid-column-end: 25;
  font-size: 4vmin;
  text-align: left !important;
  line-height: 1;
  border-bottom: 1px solid ${text_color};
}

.current_pressure{
  grid-row-start: 7;
  grid-row-end: 7;
  grid-column-start: 18;
  grid-column-end: 26;
  font-size: 4vmin;
  text-align: left !important;
}

.current_pressure_trend{
  grid-row-start: 8;
  grid-row-end: 8;
  grid-column-start: 18;
  grid-column-end: 25;
  font-size: 4vmin;
  text-align: left !important;
}

.current_humidity{
  grid-row-start: 11;
  grid-row-end: 11;
  grid-column-start: 3;
  grid-column-end: 9;
  font-size: 4vmin;
  text-align: left !important;
  line-height: 1;
}

.sunset{
  grid-row-start: 11;
  grid-row-end: 11;
  grid-column-start: 21;
  grid-column-end: 26;
  font-size: 3vmin;
  text-align: left !important;
  line-height: 1;
}

.sunrise{
  grid-row-start: 11;
  grid-row-end: 11;
  grid-column-start: 17;
  grid-column-end: 21;
  font-size: 3vmin;
  text-align: left !important;
  line-height: 1;
}

.current_dewpoint{
  grid-row-start: 11;
  grid-row-end: 11;
  grid-column-start: 9;
  grid-column-end: 18;
  font-size: 4vmin;
  text-align: left !important;
  line-height: 1;
}

.dewpoint_text{
   grid-row-start: 4;
      grid-row-end: 4;
      grid-column-start: 13;
      grid-column-end: 24;
      font-size: 4vmin;
      text-align: center !important;
      line-height: 1;
    }
}

.units{
  font-size: 3vmin;
}

    """
    return html
}

def defineHTML_Tile(){
    
    def temp_units = '°';
    def rain_units = '"';
    def m_time_units = ' am';
    def e_time_units = ' pm';
    def wind_units = ' mph';
    def pressure_units = 'inHg';
    
    if (tile_units == "metric"){
        rain_units = 'mm';
        m_time_units = '';
        e_time_units = '';
        wind_units = ' m/sec';
        pressure_units = 'mmHg';
    }
        
    def html = """
    
    <div class="grid-container">
    <div class="weather_icon">
        <span id="weather_icon" class="mdi mdi-alert-circle" style="font-size:2em">
        </span>
    </div>
    <div class="current_temperature"><span id="current_temperature">--</span><span><small>${getAbbrev(display_current_temp)}</small></span></div>

    <div class="forecast_low">
        <span id="forecast_low" class="mdi mdi-arrow-down-thick">--</span><span>${getAbbrev(display_forecast_low)}</span> 
    </div>
    <div class="forecast_high">
        <span id="forecast_high" class="mdi mdi-arrow-up-thick">--</span><span>${getAbbrev(display_forcast_high)}</span>  
    </div>

    <div class="current_feels_like">
        <span class="mdi mdi-home-thermometer-outline">Feels Like: </span>
        <span id="current_feels_like">--</span><span>${getAbbrev(display_feels_like)}</span>  
    </div>

    <div class="precipitation_title">
        <span class="mdi mdi-umbrella-outline"> Rainfall</span>
    </div>
    <div class="forecast_precipitation mdi mdi-ruler">
        <span id="forecast_precipitation"> -.--</span><span class="units">${getAbbrev(display_forecast_precipitation)}</span> 
    </div>
    <div class="current_precipitation">
        <span id="current_precipitation" class="mdi mdi-calendar-today"> -.--</span><span class="units">${getAbbrev(display_actual_precipitation)}</span> 
    </div>
    <div class="forecast_precipitation_chance">
        <span id="forecast_precipitation_chance" class="mdi mdi-cloud-question"> --</span><span class="units">%</span> 
    </div>

    <div class="pressure_title">
        <span class="mdi mdi-gauge"> Pressure</span>
    </div>
    <div class="current_pressure">
        <span id="current_pressure" class="mdi mdi-thermostat"> ----</span><span class="units"> ${getAbbrev(display_current_pressure)}</span> 
    </div>
    <div class="current_pressure_trend">
        <span id="pressure_icon" class="mdi mdi-arrow-up-thick"></span>
        <span id="current_pressure_trend"> -------</span>
    </div>

    <div class="current_humidity">
        <span id="current_humidity" class="mdi mdi-water-percent"> --</span><span class="units">%</span> 
    </div>
    <div class="current_dewpoint">
        <span id="current_dewpoint" class="mdi mdi-waves"> --.-</span><span class="units">${getAbbrev(display_dew_point)}</span> 
    </div>
    <div class="dewpoint_text">
        <span id="dewpoint_text"> -------</span>
    </div>
    <div class="current_condition1"> 
        <span id="current_condition1">------- </span>
    </div>
    <div class="current_condition2"> 
        <span id="current_condition2">------- </span>
    </div>

    <div class="wind_title">
        <span class="mdi mdi-weather-windy-variant"> Wind</span>
    </div>
    <div class="current_wind_speed mdi mdi-tailwind">
        <span id="current_wind_speed"> -- </span><span class="units">${getAbbrev(display_wind_speed)}</span> 
    </div>
    <div class="current_wind_gust mdi mdi-weather-windy">
        <span id="current_wind_gust"> -- </span><span class="units">${getAbbrev(display_wind_gust)}</span> 
    </div>
    <div class="current_wind_direction mdi mdi-compass-outline">
        <span id="current_wind_direction"> --</span><span class="units">${getAbbrev(display_wind_direction)}</span> 
    </div>

    <div class="sunrise">
        <span id="sunrise" class="mdi mdi-weather-sunset-up"> --:-- </span> 
    </div>
    <div class="sunset">
        <span id="sunset" class="mdi mdi-weather-sunset-down"> --:-- </span> 
    </div>
    </div>
"""
    return html;
   
}

def defineHTML_getOptions(){
     def html = """
        function getOptions() {
            return jQuery.get("${state.localEndpointURL}getOptions/?access_token=${state.endpointSecret}", (data) => {
            options = data;
            
            console.log("Got Options");
            console.log(options);
        });
    }
    """
    return html;
}

def defineHTML_getData(){
     def html = """
        function getData() {
            return jQuery.get("${state.localEndpointURL}getData/?access_token=${state.endpointSecret}", (data) => {
            
            let out = options.display;

            pws_data = new Map();
            if (data.current_temperature) 
                pws_data.set('current_temperature',         {value : data.current_temperature.value,            in_units: data.current_temperature.units,        out: out.current_temp});
            if (data.current_wind_speed)
                pws_data.set('current_wind_speed',          {value : data.current_wind_speed.value,             in_units: data.current_wind_speed.units,         out: out.wind_speed});
            if (data.current_feels_like) 
                pws_data.set('current_feels_like',          {value : data.current_feels_like.value,             in_units: data.current_feels_like.units,         out: out.feels_like});
            if (data.current_wind_gust)  
                pws_data.set('current_wind_gust',           {value : data.current_wind_gust.value,              in_units: data.current_wind_gust.units,          out: out.wind_gust});
            if (data.current_wind_direction) 
                pws_data.set('current_wind_direction',      {value : data.current_wind_direction.value,         in_units: data.current_wind_direction.units,     out: out.wind_direction});
            if (data.current_pressure) 
                pws_data.set('current_pressure',            {value : data.current_pressure.value,               in_units: data.current_pressure.units,           out: out.current_pressure});
            if (data.current_humidity) 
                pws_data.set('current_humidity',            {value : data.current_humidity.value,               in_units: data.current_humidity.units,           out: out.humidity});
            if (data.current_dewpoint) {
                pws_data.set('current_dewpoint',            {value : data.current_dewpoint.value,               in_units: data.current_dewpoint.units,           out: out.dew_point});
                pws_data.set('dewpoint_text',               {value : getDewPoint(data.current_dewpoint.value),  in_units: "none",                                out: "none"});
            }
            if (data.current_pressure_trend)
                pws_data.set('current_pressure_trend',      {value : data.current_pressure_trend.value,         in_units: data.current_pressure_trend.units,    out: out.pressure_trend});  
            if (data.current_precipitation) 
                pws_data.set('current_precipitation',       {value : data.current_precipitation.value,          in_units: data.current_precipitation.units,      out: out.actual_precipitation});
            
        });
    }
    """
    return html;
}

def defineHTML_globalVariables(){
    def html = """
        var sunrise;
        var sunset;
        let options = [];
        let pws_data = [];
    """
}

def defineHTML_setCondition(){
    def html = """
    
    function setCondition(condition) {

    let icon = "mdi-weather-sunny-off";
    let text1 = "UNKNOWN";
    let text2 = "";
    let now = new Date().getTime() / 1000;

    switch (condition) {
        case "thunderstorm with light rain":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS";
            text2 = "LIGHT RAIN";
            break;
        case "thunderstorm with rain":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS";
            text2 = "RAIN";
            break;
        case "thunderstorm with heavy rain":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS"
            text2 = "HEAVY RAIN";
            break;
        case "light thunderstorm":
            icon = "mdi-weather-lightning";
            text1 = "LIGHT";
            text2 = "THUNDERSTORMS";
            break;
        case "thunderstorm":
            icon = "mdi-weather-lightning";
            text1 = "THUNDERSTORMS";
            break;
        case "heavy thunderstorm":
            icon = "mdi-weather-lightning";
            text1 = "HEAVY";
            text2 = "THUNDERSTORMS";
            break;
        case "ragged thunderstorm":
            icon = "mdi-weather-lightning";
            text1 = "SCATTERED";
            text2 = "THUNDERSTORMS";
            break;
        case "thunderstorm with light drizzle":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS";
            text2 = "LIGHT DRIZZLE";
            break;
        case "thunderstorm with drizzle":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS";
            text2 = "DRIZZLE";
            break;
        case "thunderstorm with heavy drizzle":
            icon = "mdi-weather-lightning-rainy"
            text1 = "THUNDERSTORMS"
            text2 = "HEAVY DRIZZLE";
            break;
        case "light intensity drizzle":
            icon = "mdi-weather-partly-rainy";
            text1 = "LIGHT";
            text2 = "DRIZZLE";
            break;
        case "drizzle":
            icon = "mdi-weather-partly-rainy";
            text1 = "DRIZZLE";
            break;
        case "heavy intensity drizzle":
            icon = "mdi-weather-partly-rainy";
            text1 = "HEAVY";
            text2 = "DRIZZLE";
            break;
        case "light intensity drizzle rain":
            icon = "mdi-weather-partly-rainy";
            text1 = "LIGHT";
            text2 = "DRIZZLE";
            break;
        case "drizzle rain":
            icon = "mdi-weather-partly-rainy";
            text1 = "DRIZZLE";
            break;
        case "heavy intensity drizzle rain":
            icon = "mdi-weather-rainy";
            text1 = "RAIN";
            break;
        case "shower rain and drizzle":
            icon = "mdi-weather-rainy";
            text1 = "RAIN";
            break;
        case "heavy shower rain and drizzle":
            icon = "mdi-weather-pouring";
            text1 = "SHOWERS";
            break;
        case "shower drizzle":
            icon = "mdi-weather-rainy";
            text1 = "SHOWERS";
            break;
        case "light rain":
            icon = "mdi-weather-rainy";
            text1 = "LIGHT";
            text2 = "RAIN";
            break;
        case "moderate rain":
            icon = "mdi-weather-pouring";
            text1 = "MODERATE";
            text2 = "RAIN";
            break;
        case "heavy intensity rain":
            icon = "mdi-weather-pouring";
            text1 = "HEAVY";
            text2 = "RAIN";
            break;
        case "very heavy rain":
            icon = "mdi-weather-pouring";
            text1 = "VERY HEAVY";
            text2 = "RAIN";
            break;
        case "extreme rain":
            icon = "mdi-weather-pouring";
            text1 = "INTENSE";
            text2 = "RAIN";
            break;
        case "freezing rain":
            icon = "mdi-weather-snowy-rainy";
            text1 = "FREEZING";
            text2 = "RAIN";
            break;
        case "light intensity shower rain":
            icon = "mdi-weather-rainy";
            text1 = "LIGHT";
            text2 = "SHOWERS";
            break;
        case "shower rain":
            icon = "mdi-weather-rainy";
            text1 = "SHOWERS";
            break;
        case "heavy intensity shower rain":
            icon = "mdi-weather-pouring";
            text1 = "HEAVY"
            text2 = "SHOWERS";
            break;
        case "ragged shower rain":
            icon = "mdi-weather-partly-rainy";
            text1 = "SCATTERED";
            text2 = "SHOWERS";
            break;
        case "light snow":
            icon = "mdi-weather-snowy";
            text1 = "LIGHT";
            text2 = "SNOW";
            break;
        case "Snow":
            icon = "mdi-weather-snowy";
            text1 = "SNOW";
            break;
        case "Heavy snow":
            icon = "mdi-weather-snowy-heavy";
            text1 = "HEAVY SNOW";
            break;
        case "Sleet":
            icon = "mdi-weather-hail";
            text1 = "SLEET";
            break;
        case "Light shower sleet":
            icon = "mdi-weather-hail";
            text1 = "LIGHT SLEET";
            break;
        case "Shower sleet":
            icon = "mdi-weather-hail";
            text1 = "SLEET";
            text2 = "SHOWERS";
            break;
        case "Light rain and snow":
            icon = "mdi-weather-snowy-rainy";
            text1 = "LIGHT"
            text2 = "RAIN & SNOW";
            break;
        case "Rain and snow":
            icon = "mdi-weather-snowy-rainy";
            text1 = "RAIN & SNOW";
            break;
        case "Light shower snow":
            icon = "mdi-weather-partly-snowy";
            text1 = "LIGHT RAIN"
            text2 = "SNOW SHOWERS";
            break;
        case "Shower snow":
            icon = "mdi-weather-partly-snowy";
            text1 = "SNOW SHOWERS";
            break;
        case "Heavy shower snow":
            icon = "mdi-weather-partly-snowy";
            text1 = "HEAVY";
            text2 = "SNOW SHOWERS";
            break;
        case "mist":
            icon = "mdi-weather-fog";
            text1 = "MIST";
            break;
        case "Smoke":
            icon = "mdi-weather-fog";
            text1 = "SMOKE";
            break;
        case "Haze":
            icon = "mdi-weather-hazy";
            text1 = "HAZE";
            break;
        case "sand dust whirls":
            icon = "mdi-weather-tornado";
            text1 = "DUST WHIRLS";
            break;
        case "fog":
            icon = "mdi-weather-fog";
            text1 = "FOG";
            break;
        case "sand":
            icon = "mdi-weather-fog";
            text1 = "SAND";
            break;
        case "dust":
            icon = "mdi-weather-fog";
            text1 = "DUST";
            break;
        case "volcanic ash":
            icon = "mdi-weather-fog";
            text1 = "VOCANIC ASH";
            break;
        case "squalls":
            icon = "mdi-weather-tornado";
            text1 = "SQUALLS";
            break;
        case "tornado":
            icon = "mdi-weather-tornado";
            text1 = "TORNADO";
            break;
        case "clear sky":
            if (now > sunset || now < sunrise) {
                icon = "mdi-weather-night";
                text1 = "CLEAR";
            } else {
                icon = "mdi-weather-sunny";
                text1 = "SUNNY";
            }
            break;
        case "few clouds":
            if (now > sunset || now < sunrise) {
                icon = "mdi-weather-night-partly-cloudy"
            } else {
                icon = "mdi-weather-partly-cloudy";
            }
            text1 = "FEW CLOUDS";
            break;
        case "scattered clouds":
            if (now > sunset || now < sunrise) {
                icon = "mdi-weather-night-partly-cloudy"
            } else {
                icon = "mdi-weather-partly-cloudy";
            }
            text1 = "SCATTERED";
            text2 = "CLOUDS";
            break;
        case "broken clouds":
            icon = "mdi-weather-cloudy";
            text1 = "BROKEN"
            text2 = "CLOUDS";
            break;
        case "overcast clouds":
            icon = "mdi-weather-cloudy";
            text1 = "OVERCAST"
            text2 = "CLOUDS";
            break;
    }

    let el = document.getElementById('weather_icon');
    el.className = 'mdi ' + icon;

    el = document.getElementById('current_condition1');
    el.textContent = text1;

    el = document.getElementById('current_condition2');
    el.textContent = text2;
    }
    """
    return html;
}

def defineHTML_setValue(){
    def html = """
        function setValue(val, str) {
 		    let el = document.getElementById(str);
            el.textContent = val;
         }
    """
    return html;
}

def defineHTML_getWindDirection(){
   def html = """
        function getWindDirection(direction){
	        if (direction > 348.75 || direction < 11.25) return "N";
            if (direction >= 11.25 && direction < 33.75) return "NNE";
         	if (direction >= 33.75 && direction < 56.25) return "NE";
         	if (direction >= 56.25 && direction < 78.7) return "ENE";
         	if (direction >= 78.75 && direction < 101.25) return "E";
         	if (direction >= 101.25 && direction < 123.75) return "ESE";
 	        if (direction >= 123.75 && direction < 146.25) return "SE";
         	if (direction >= 146.25 && direction < 168.75) return "SSE";
 	        if (direction >= 168.75 && direction < 191.25) return "S";
 	        if (direction >= 191.25 && direction < 213.75) return "SSW";
 	        if (direction >= 213.75 && direction < 236.25) return "SW";
 	        if (direction >= 236.25 && direction < 258.75) return "WSW";
 	        if (direction >= 258.75 && direction < 281.25) return "W";
 	        if (direction >= 281.25 && direction < 303.75) return "WNW";
 	        if (direction >= 303.75 && direction < 326.25) return "NW";
 	        if (direction >= 326.25 && direction < 348.75) return "NNW";
        }
    """
    return html;
}

def defineHTML_getString(){
    
    def html = """
    
function getString(data) {

    let val = parseFloat(data.value);
    let in_units = data.in_units;
    let out_units = data.out.unit;
    let digits = data.out.decimal;
    let outputType = "numeric";

    if (in_units != out_units) {
        switch (in_units) {
            //Temperature
            case "celsius":
                switch (out_units) {
                    case "fahrenheit": val = (val * 9 / 5) + 32; break;
                    case "kelvin": val = val + 273.15; break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "fahrenheit":
                switch (out_units) {
                    case "celsius": val = (val - 32.0) * (5 / 9); break;
                    case "kelvin": val = ((val - 32) * (5 / 9)) + 273.15; break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "kelvin":
                switch (out_units) {
                    case "fahrenheit": val = ((val - 273.15) * (9 / 5)) + 32; break;
                    case "celsius": val = (val - 273.15); break;
                    default: val = "UNSUPPORTED";
                }
            break;
            //Length
            case "millimeters":
                if (out_units == "inches") {
                    val = (val / 25.4);
                }
                else val = "UNSUPPORTED";
            break;
            case "inches":
                if (out_units == "millimeters") {
                    val = (val * 25.4);
                }
                else val = "UNSUPPORTED";
            break;
            case "meters_per_second":
                switch (out_units) {
                    case "miles_per_hour": val = (val * 2.237); break;
                    case "knots": val = (val * 1.944); break;
                    case "kilometers_per_hour" : val = (val * 3.6); break;
                    default: val = "UNSUPPORTED";
                }
            break;
            case "miles_per_hour":
                switch (out_units) {
                    case "miles_per_hour": val = (val / 2.237); break;
                    case "knots": val = (val / 1.151); break;
                    case "kilometers_per_hour" : val = (val * 1.609); break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "knots":
                switch (out_units) {
                    case "miles_per_hour": val = (val * 1.151); break;
                    case "meters_per_second": val = (val / 1.944); break;
                    case "kilometers_per_hour" : val = (val * 1.852); break;
                    default: val = "UNSUPPORTED";
                }
                break;
             case "kilometers_per_hour":
                switch (out_units) {
                    case "miles_per_hour": val = (val / 1.609); break;
                    case "meters_per_second": val = (val / 3.6); break;
                    case "knots": val = (val / 1.852); break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "hectopascal":
            case "millibars":
                switch (out_units) {
                    case "inches_mercury": val = (val / 33.864); break;
                    case "millimeters_mercury": val = (val / 1.333); break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "inches_mercury":
                switch (out_units) {
                    case "hectopascal":
                    case "millibars": val = (val * 33.864); break;
                    case "inches_mercury": val = (val / 25.4); break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "millimeters_mercury":
                switch (out_units) {
                    case "hectopascal":
                    case "millibars": val = (val * 1.333); break;
                    case "millimeters_mercury": val = (val * 25.4); break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "degrees":
                switch (out_units) {
                    case "cardinal": 
                        val = getWindDirection(val); 
                        outputType = "text"; 
                        break;
                    case "radians": val = (val * 3.1415926535) * 180.0; break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "radians":
                switch (out_units) {
                    case "cardinal": val = getWindDirection(((val * 3.1415926535) * 180.0)); outputType = "text"; break;
                    case "degrees": val = ((val * 180) / 3.1415926535); break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "cardinal":
                switch (data.value) {
                    case "N": val = 0;
                    case "NNE": val = 22.5;
                    case "NE": val = 45;
                    case "ENE": val = 67.5;
                    case "E": val = 90;
                    case "ESE": val = 112.5;
                    case "SE": val = 135;
                    case "SSE": val = 157.5;
                    case "S": val = 180;
                    case "SSW": val = 202.5;
                    case "SW": val = 225;
                    case "WSW": val = 247.5;
                    case "W": val = 270;
                    case "WNW": val = 292.5;
                    case "NW": val = 315;
                    case "NNW": val = 337.5;
                    default: val = -1;
                }
                if (val != -1) {
                    switch (out_units) {
                        case "radians": val = ((val * 3.1415926535) * 180.0); break;
                        case "degrees": val = val; break;
                        default: val = "UNSUPPORTED";
                    }
                } else val = "UNSUPPORTED";
                break;

            //TEXT CONVERSIONS
            case "time_seconds":
                 let d = new Date(data.value*1000);

                switch (out_units) {
                    case "time_twelve": val = d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }); outputType = "text"; break;
                    case "time_two_four": val = d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }); outputType = "text"; break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "time_milliseconds":
                let e = new Date(data.value);

                switch (out_units) {
                    case "time_twelve": val = e.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }); outputType = "text"; break;
                    case "time_two_four": val = e.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }); outputType = "text"; break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "trend_numeric":
                if (out_units == "trend_text") {
                    val = parseFloat(data.value);
                    if (val < 0)
                        val = "Falling";
                    else if (val == 0)
                        val = "Steady";
                    else
                        val = "Rising";
                } else val = "UNSUPPORTED";
                outputType = "text";
                break;
            case "trend_text":
                if (out_units == "trend_numeric") {
                    let lcString = data.value.toLowerCase();
                    switch (lcString) {
                        case "falling": val = -1; break;
                        case "steady": val = 0; break;
                        case "rising": val = 1; break;
                    }
                }
                break;
            case "trend_numeric":
                val = parseFloat(data.value);
                if (val < 0)
                    val = "Falling";
                else if (val == 0)
                    val = "Steady";
                else
                    val = "Rising";
                break;
                outputType = "text";
            case "percent_numeric":
                if (out_units == "percent_decimal") val = val / 100.0;
                else val = "UNSUPPORTED";
                break;
            case "percent_decimal":
                if (out_units == "percent_numeric") val = val * 100.0;
                else val = "UNSUPPORTED";
                break;
        }

    }
    
    if (outputType == "text") return val;
    else {
        if (val == "UNSUPPORTED"){
            console.log("UNSUPPORTED:: "+data.value+": "+data.in_units+" "+data.out.unit+" "+data.out.decimal);
            return val;
        }
        return " "+val.toFixed(digits);
    } 
}
    """
    return html;
}

def defineHTML_setWeatherTile(){
     def html = """
 
function setWeatherTile(weather) {

    weather.forEach((value, key) => {
        
        let val = getString(value); 

        if (key == "current_description") {        
            setCondition(value.value);
        } else if (key == "current_pressure_trend") {
            setPressure(value.value);
        }
        else if (key == "dewpoint_text"){
            setValue(value.value, key);
        } else {
           setValue(val, key);
        }

    });

}
    """
    return html;
}

def defineHTML_toInches(){
   def html = """ 
         function toInches(val){
 	        val = val/25.4;
            return val.toFixed(2);
         }
    """
    return html;
}

def defineHTML_getTime(){
   def html = """
     function getTime(secs){
 	    let date = new Date(secs*1000);
        return date.toLocaleTimeString([], {timeStyle: 'short'});
     }
 """
    return html;
}

def defineHTML_getDewPoint(){
    
   def html = """
     function getDewPoint(temp){
 
 	    if (temp < 50) return "DRY";
        else if (temp < 55) return "PLEASANT";
        else if (temp < 60) return "COMFORTABLE";
        else if (temp < 65) return "STICKY";
        else if (temp < 70) return "UNCOMFORTABLE";
        else if (temp < 75) return "OPPRESSIVE";
        else return "MISERABLE"
    }
 
  """
    return html;
}

def defineHTML_setPressure(){
    
    def html = """

    function setPressure(val){
    let icon = '';
    let text = '';
    let valStr = '';
    
    if (isNaN(val)) valStr = val.toLowerCase();

    console.log(val+" "+valStr);
    
    if (val == 0 || valStr == 'steady') {
    	text = 'Steady';
        icon = 'mdi-arrow-right-thick';
    } else if (val < 0  || valStr == 'falling') {
    	text = 'Falling';
        icon = 'mdi-arrow-down-thick';
    } else if (val > 0 || valStr == 'rising') {
    	text = 'Rising';
        icon = 'mdi-arrow-up-thick';
    }
 
 	let el = document.getElementById('pressure_icon');
    el.className = 'current_pressure_trend mdi '+icon;
    
    el = document.getElementById('current_pressure_trend');
    el.textContent = text;
 }
"""
    return html;
}

def defineHTML_getWeather(){
    def html = """
    
function getWeather() {
    const url = `https://api.openweathermap.org/data/2.5/onecall?lat=${latitude}&lon=${longitude}&exclude=minutely,hourly&appid=${tile_key}&units=imperial`;

    let now = new Date();
    let tempUnits = "fahrenheit";
    let speedUnits = "meters_per_second";
    let precipUnits = "millimeters";
    let pressureUnits = "millibars";
    let timeUnits = "time_seconds"
    let dirUnits = "degrees"
    let percent = "percent_numeric";

    

    var currentPressure = 0;

    fetch(url)
        .then(response => response.json())
        .then(data => {
            let weather = new Map();
            let override = options.override;
            let out = options.display;

            if (override.current_temp == "openweather")      weather.set('current_temperature',           {value : data.current.temp,                   in_units: tempUnits,     out: out.current_temp});
                                                             weather.set('forecast_high',                 {value : data.daily[0].temp.max,              in_units: tempUnits,     out: out.forcast_high});
                                                             weather.set('forecast_low',                  {value : data.daily[0].temp.min,              in_units: tempUnits,     out: out.forecast_low});
            if (override.feels_like == "openweather")        weather.set('current_feels_like',            {value : data.current.feels_like,             in_units: tempUnits,     out: out.feels_like});
                                                             weather.set('current_description',           {value : data.current.weather[0].description, in_units: "none",        out: "none"});
            if (override.wind_speed == "openweather")        weather.set('current_wind_speed',            {value : data.current.wind_speed,             in_units: speedUnits,    out: out.wind_speed});
            
            if (override.wind_gust == "openweather")
                if (data.current.wind_gust != undefined)     weather.set('current_wind_gust',             {value : data.current.wind_gust,              in_units: speedUnits,    out: out.wind_gust});
                else                                         weather.set('current_wind_gust',             {value : data.current.wind_speed,             in_units: speedUnits,    out: out.wind_gust});

            if (override.wind_direction == "openweather")    weather.set('current_wind_direction',        {value : data.current.wind_deg,               in_units: dirUnits,      out: out.wind_direction});

            if (data.daily[0].rain != undefined)             weather.set('forecast_precipitation',        {value : data.daily[0].rain,                  in_units: precipUnits,   out: out.forecast_precipitation});
            else                                             weather.set('forecast_precipitation',        {value : "0.00",                              in_units: precipUnits,   out: out.forecast_precipitation});

                                                             weather.set('forecast_precipitation_chance', {value : data.daily[0].pop * 100,             in_units: percent,       out: out.chance_precipitation})

            if (override.current_pressure == "openweather")  weather.set('current_pressure',              {value : data.current.pressure,               in_units: pressureUnits, out: out.current_pressure});
            currentPressure = data.current.pressure;
            
            if (override.humidity == "openweather")          weather.set('current_humidity',              {value : data.current.humidity,               in_units: percent,       out: out.humidity});
                                                             weather.set('sunrise',                       {value : data.current.sunrise,                in_units: timeUnits,     out: out.time_format});    
                                                             weather.set('sunset',                        {value : data.current.sunset,                 in_units: timeUnits,     out: out.time_format});
            if (override.dew_point == "openweather")         weather.set('current_dewpoint',              {value : data.current.dew_point,              in_units: tempUnits,     out: out.dew_point});
            if (override.dew_point == "openweather")         weather.set('dewpoint_text',                 {value : getDewPoint(data.current.dew_point), in_units: "none",        out: "none"});
            
            //Set global sunrise and sunset
            sunrise = data.current.sunrise;
            sunset = data.current.sunset;

            setWeatherTile(weather);
        })
        .catch((error) => {
            console.log(error);
        });


    now.setHours(0, 0, 0, 0);
    let secs = now.getTime() / 1000.0
    secs = secs.toFixed();

    const url2 = `https://api.openweathermap.org/data/2.5/onecall/timemachine?lat=${latitude}&lon=${longitude}&dt=\${secs}&appid=${tile_key}`;

    fetch(url2)
        .then(response => response.json())
        .then(data => {
            let override = options.override;
            let weather2 = new Map();
            let out = options.display;

            if (override.daily_precip == "openweather"){
                let total_rain = 0.0;
                data.hourly.forEach((val) => {
                    if (val.rain != undefined) {
                        total_rain += val.rain["1h"];
                    }
                })            
                weather2.set('current_precipitation',              {value : total_rain,         in_units: "millimeters",        out: out.actual_precipitation});
            }

            if (override.pressure_trend == "openweather"){
                let num = data.hourly.length;
                let compare = num - 1;
                if (((now.getTime()/1000.0) - data.hourly[num - 1].dt) < 1800) {
                   compare = compare+1 == num ? compare : compare+1;  
                }
                
                let diff = data.hourly[compare].pressure - currentPressure;
                weather2.set('current_pressure_trend',        {value : diff,                in_units: "none",           out: "none"});
            }
            setWeatherTile(weather2);

        })
        .catch((error) => {
           console.log(error);
        });
    }

    """
    return html;
}

def defineHTML_getOpenWeatherData(){
    def html = """
    async function getOpenWeatherData(){
        let now = new Date();
        console.log("OpenWeather Data Refresh at "+now.toLocaleString());
        getWeather();
    }
    """
    return html;
}

def defineHTML_getPWSData(){
    def html = """
    async function getPWSData(){
        let now = new Date();
        console.log("PWS Data Refresh at "+now.toLocaleString());
        
        await getData();
        setWeatherTile(pws_data);
    }
    """
    return html;
}

def defineHTML_initialize(){
    
    def html = """
    async function initialize(){ 
        await getOptions();
        
        setInterval(() => {
                getOpenWeatherData();
        }, options.openweather_refresh_rate);
        let minutes = (options.openweather_refresh_rate/1000)/60;
        console.log("Refreshing OpenWeather at "+minutes+" minutes");
    
        if (options.pws_refresh_rate != null){
        
            setInterval(() => {
                getPWSData();
            }, options.pws_refresh_rate);
            let minutes = (options.pws_refresh_rate/1000)/60;
            console.log("Refreshing Personal Weather Station at "+minutes+" minutes");
        }
         
        await getData();
        getWeather();
        setWeatherTile(pws_data);
        
    }
    """
    return html;
}

def getWeatherTile() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    html = defineHTML_Header();
    html += "<head><style>";
    //CSS
    html += defineHTML_CSS();
    html += """</style></head><body onload="initialize()">"""
    html += defineHTML_Tile();
    
    html += "<script>"
    html += defineHTML_globalVariables();

    html += defineHTML_getOptions();
    html += defineHTML_getData();
    html += defineHTML_getOpenWeatherData();
    html += defineHTML_getPWSData();
    html += defineHTML_setCondition();
    html += defineHTML_setValue();
    html += defineHTML_getWindDirection();
    html += defineHTML_setWeatherTile();
    html += defineHTML_toInches();
    html += defineHTML_getTime();
    html += defineHTML_getDewPoint();
    html += defineHTML_setPressure();    
    html += defineHTML_getWeather();
    html += defineHTML_initialize();
    html += defineHTML_getString();
    //html += defineHTML_getTimerData();
     
    html +="</script></body></html>"
 
    return html;
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
def getTile() {
    render(contentType: "text/html", data: getWeatherTile());      
}

def getData() {
    def timeline = getPWSData();
    return render(contentType: "text/json", data: JsonOutput.toJson(timeline));
}

def getOptions() {
    render(contentType: "text/json", data: JsonOutput.toJson(getTileOptions()));
}

def getSubscriptions() {
    def ids = [];
    def sensors_ = [:];
    def attributes = [:];
    def labels = [:];
    def drop_ = [:];
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
        ids: ids,
        sensors: sensors_,
        attributes: attributes, 
        labels : labels,
        drop : drop_,
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
