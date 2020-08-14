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
            container << parent.hubiForm_text_input (this, "<b>Open Weather Map Key</b>", "tile_key", "", "true");
            
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
            
            container << parent.hubiForm_switch     (this, title: "Color Icons?", name: "color_icons", default: false);
            
            container << parent.hubiForm_switch     (this, title: "Show Dew Point Description?", name: "show_dewpoint", default: true);
                         
            parent.hubiForm_container(this, container, 1);     
        } 
        parent.hubiForm_section(this,"Font Sizes", 1)
        {
            container = [];
            container << parent.hubiForm_text         (this, "<p style='background-color: yellow;'><b>Adjust the component font sizes.<br>Default are populated upon installation (below)<br>*Note: The displayed sizes are not exact but <b>relative</b> to tile size.</b>,</p>");
            container << parent.hubiForm_fontvx_size  (this, title: "Icon", name: "icon", default: 10, min: 1, max: 20, icon: true);
            container << parent.hubiForm_fontvx_size  (this, title: "Current Temperature", name: "temperature", default: 20, min: 1, max: 30, weight: 900);
            container << parent.hubiForm_fontvx_size  (this, title: "Real Feel", name: "realfeel", default: 7, min: 1, max: 15, weight: 900);
            container << parent.hubiForm_fontvx_size  (this, title: "High/Low Forecast", name: "highlow", default: 7, min: 1, max: 15);
            container << parent.hubiForm_fontvx_size  (this, title: "Headings<br>(Rainfall/Wind/Pressure)", name: "heading", default: 5, min: 1, max: 10);
            container << parent.hubiForm_fontvx_size  (this, title: "Wind, Rail and Barometer Items", name: "column", default: 5, min: 1, max: 10);
            container << parent.hubiForm_fontvx_size  (this, title: "Sunrise/Sunset", name: "time", default: 3, min: 1, max: 10);
            container << parent.hubiForm_fontvx_size  (this, title: "Current Conditions", name: "conditions", default: 5, min: 1, max: 10);
            container << parent.hubiForm_fontvx_size  (this, title: "Humidity/Dew Point", name: "humidity", default: 4, min: 1, max: 10);
            container << parent.hubiForm_fontvx_size  (this, title: "Dew Point Conditions", name: "dewpoint", default: 4, min: 1, max: 10);

            
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
                }
                container = [];
                container <<  parent.hubiForm_sub_section(this, attribute.title+" Display");
                parent.hubiForm_container(this, container, 1);
                input( type: "enum", name: attribute.var, title: "Units", required: false, multiple: false, options: attribute.unit, defaultValue: attribute.imperial, submitOnChange: false)
                if (attribute.var != "display_time_format")
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
                        if (settings[tile.var] != "openweather" && tile.unit != null && settings[tile.var]!=null){
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
                        } else if (settings[tile.var]!=null && settings[tile.var] != "openweather"){
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
               parent.hubiForm_section(this, "Tile Options", 1, "tune"){
                    container = [];
                    container << parent.hubiForm_page_button(this, "Select Device/Data", "deviceSelectionPage", "100%", "vibration");
                    container << parent.hubiForm_page_button(this, "Configure Tile", "tileSetupPage", "100%", "poll");              
                    parent.hubiForm_container(this, container, 1); 
                }
                
                parent.hubiForm_section(this, "Local Tile URL", 1, "link"){
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
                             container << parent.hubiForm_text_input(this, "Name for Tile Device", "device_name", "Hubigraph Tile", "false");
                        }
                        parent.hubiForm_container(this, container, 1); 
                    }
                } 
             
            
               if (state.endpoint){
                   parent.hubiForm_section(this, "Hubigraph Application", 1, "settings"){
                        container = [];
                        container << parent.hubiForm_sub_section(this, "Application Name");
                        container << parent.hubiForm_text_input(this, "Rename the Application?", "app_name", "Hubigraph Weather Tile", "false");
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
        case "percent_numeric": return "%";
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
        "color_icons": color_icons,
        "show_dewpoint": show_dewpoint,
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

    <script>
        const localURL =         "${state.localEndpointURL}";
        const secretEndpoint =   "${state.endpointSecret}";
        const latitude =         "${latitude}";
        const longitude =        "${longitude}";
        const tile_key =         "${tile_key}";
    </script>
    
    <script src="https://code.getmdl.io/1.3.0/material.min.js"></script>
    <!--script defer src="http://192.168.1.64:8080/WeatherTile.js"></script> -->
    <script defer src="/local/a7af9806-4b0e-4032-a78e-a41e27e4d685-WeatherTile.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js"></script>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
    
    """
    return html;
}

def defineHTML_CSS(){
    def html = """
     .grid-container {
      display: grid;
      grid-template-columns: 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw 4vw;
      grid-template-rows: 30vmin 6vmin 8vmin 6vmin 5vmin 6vmin 7vmin 7vmin 7vmin 6vmin 6vmin 6vmin;
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
      font-size: ${icon_font}vmin;
      padding-top: 0vmin !important; 
      padding-left: 00vmin !important;
      text-align: center !important;
      color: ${text_color} !important;
    }

    .current_condition1 {
      grid-row-start:3;
      grid-row-end: 3;
      grid-column-start: 13;
      grid-column-end: 24;
      font-size: ${conditions_font}vmin;
      text-align: center !important;
      line-height: 1;
    }

    .current_condition2 {
      grid-row-start: 4;
      grid-row-end: 4;
      grid-column-start: 13;
      grid-column-end: 24;
      font-size: ${conditions_font}vmin;
      text-align: center !important;
      line-height: 1;
    }

    .current_temperature {
      font-weight: 900;
      grid-row-start: 1;
      grid-row-end: 1;
      grid-column-start: 2;
      grid-column-end: 15;
      font-size: ${temperature_font}vmin;
      text-align: center !important;
      padding-top: 10vmin !important; 
    }

    .current_feels_like{
      grid-row-start: 3;
      grid-row-end: 3;
      grid-column-start: 2;
      grid-column-end: 14;
      font-size: 4vmin;
      text-align: center !important;  
    }

    .feels_like_number{
       font-size: ${realfeel_font}vmin !important;
       font-weight: 900 !important;

    }

    .forecast_low{
      grid-row-start: 4;
      grid-row-end: 6;
      grid-column-start: 3;
      grid-column-end: 8;
      font-size: ${highlow_font}vmin;
      text-align: center !important;
    }

    .forecast_high{
      grid-row-start: 4;
      grid-row-end: 6;
      grid-column-start: 8;
      grid-column-end: 13;
      font-size: ${highlow_font}vmin;
      text-align: center !important;
    }

    .precipitation_group {
        grid-row-start: 6;
        grid-row-end: 9;
        grid-column-start: 2;
        grid-column-end: 9;
    }
    .precipitation_title{
      grid-row-start: 6;
      grid-row-end: 6;
      grid-column-start: 2;
      grid-column-end: 9;
      font-size: ${heading_font}vmin;
      text-align: left !important;
      line-height: 1; 
      border-bottom: 1px solid ${text_color};
    }

    .forecast_precipitation_chance{
      grid-row-start: 7;
      grid-row-end: 7;
      grid-column-start: 2;
      grid-column-end: 9;
      font-size: ${column_font}vmin;
      text-align: left !important;
    }

    .forecast_precipitation{
      grid-row-start: 8;
      grid-row-end: 8;
      grid-column-start: 2;
      grid-column-end: 9;
      font-size: ${column_font}vmin;
      text-align: left !important;
    }

    .current_precipitation{
      grid-row-start: 9;
      grid-row-end: 9;
      grid-column-start: 2;
      grid-column-end: 9;
      font-size: ${column_font}vmin;
      text-align: left !important;
    }

    .wind_title{
      grid-row-start: 6;
      grid-row-end: 6;
      grid-column-start: 10;
      grid-column-end: 17;
      font-size: ${heading_font}vmin;
      text-align: left !important;
      line-height: 1;
      border-bottom: 1px solid ${text_color}; 
    }

.current_wind_speed{
  grid-row-start:7;
  grid-row-end: 7;
  grid-column-start: 10;
  grid-column-end: 17;
  font-size: ${column_font}vmin;
  text-align: left !important;
}
.current_wind_gust{
  grid-row-start: 8;
  grid-row-end: 8;
  grid-column-start: 10;
  grid-column-end: 17;
  font-size: ${column_font}vmin;
  text-align: left !important;
}
.current_wind_direction{
  grid-row-start: 9;
  grid-row-end: 9;
  grid-column-start: 10;
  grid-column-end: 17;
  font-size: ${column_font}vmin;
  text-align: left !important;
}

.pressure_title{
  grid-row-start: 6;
  grid-row-end: 6;
  grid-column-start: 18;
  grid-column-end: 25;
  font-size: ${heading_font}vmin;
  text-align: left !important;
  line-height: 1;
  border-bottom: 1px solid ${text_color};
}

.current_pressure{
  grid-row-start: 7;
  grid-row-end: 7;
  grid-column-start: 18;
  grid-column-end: 26;
  font-size: ${column_font}vmin;
  text-align: left !important;
}

.current_pressure_trend{
  grid-row-start: 8;
  grid-row-end: 8;
  grid-column-start: 18;
  grid-column-end: 25;
  font-size: ${column_font}vmin;
  text-align: left !important;
}

.current_humidity{
  grid-row-start: 11;
  grid-row-end: 11;
  grid-column-start: 2;
  grid-column-end: 6;
  font-size: ${humidity_font}vmin;
  text-align: left !important;
  line-height: 1;
}

.sunset{
  grid-row-start: 11;
  grid-row-end: 11;
  grid-column-start: 21;
  grid-column-end: 26;
  font-size: ${time_font}vmin;
  text-align: left !important;
  line-height: 1;
}

.sunrise{
  grid-row-start: 11;
  grid-row-end: 11;
  grid-column-start: 17;
  grid-column-end: 21;
  font-size: ${time_font}vmin;
  text-align: left !important;
  line-height: 1;
}

.current_dewpoint{
  grid-row-start: 11;
  grid-row-end: 11;
  grid-column-start: 6;
  grid-column-end: 18;
  font-size: ${humidity_font}vmin;
  text-align: left !important;
  line-height: 1;
}

.dewpoint_text{
   grid-row-start: 11;
   grid-row-end: 11;
   grid-column-start: 11;
   grid-column-end: 20;
   font-size: ${dewpoint_font}vmin;
   text-align: left !important;
   line-height: 1;
}
}

.units{
  font-size: 3vmin;
}

    """
    return html
}

def defineHTML_GraphWindowCSS(){
    def html = """
 
.graphWindow {
  display: none; /* Hidden by default */
  position: fixed; /* Stay in place */
  z-index: 1; /* Sit on top */
  padding-top: 5vh; /* Location of the box */
  left: 0;
  top: 0;
  width: 100%; /* Full width */
  height: 100%; /* Full height */
  overflow: auto; /* Enable scroll if needed */
  background-color: rgb(0,0,0); /* Fallback color */
  background-color: rgba(0,0,0,0.4); /* Black w/ opacity */
}

.graphWindow-content {
  background-color: #fefefe;
  margin: auto;
  padding: 1vh;
  border: 1px solid #888;
  width: 80%;
}

/* The Close Button */
.close {
  color: #aaaaaa;
  float: right;
  font-size: 28px;
  font-weight: bold;
}

.close:hover,
.close:focus {
  color: #000;
  text-decoration: none;
  cursor: pointer;
}
"""
    return html;
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
        <span class="mdi mdi-home-thermometer-outline">Feels Like:</span>
        <span id="current_feels_like" class="feels_like_number">--</span><span>${getAbbrev(display_feels_like)}</span>  
    </div>
    
    
    <div class="precipitation_title"  onclick="showWeather()">          
        <span class="mdi mdi-umbrella-outline"> Rainfall</span>
    </div>
    <div class="forecast_precipitation" ><span class="mdi mdi-ruler"> </span> 
        <span id="forecast_precipitation">-.--</span><span class="units">${getAbbrev(display_forecast_precipitation)}</span> 
    </div>
    <div class="current_precipitation"><span  class="mdi mdi-calendar-today"> </span>
        <span id="current_precipitation"> -.--</span><span class="units">${getAbbrev(display_actual_precipitation)}</span> 
    </div>
    <div class="forecast_precipitation_chance"><span class="mdi mdi-cloud-question"> </span>
        <span id="forecast_precipitation_chance" >--</span><span class="units">%</span> 
    </div>
    

    <div class="pressure_title">
        <span class="mdi mdi-gauge"> Pressure</span>
    </div>
    <div class="current_pressure"><span class="mdi mdi-thermostat"> </span>
        <span id="current_pressure">----</span><span class="units"> ${getAbbrev(display_current_pressure)}</span> 
    </div>
    <div class="current_pressure_trend">
        <span id="pressure_icon" class="mdi mdi-arrow-up-thick"></span>
        <span id="current_pressure_trend"> -------</span>
    </div>

    <div class="current_humidity"><span class="mdi mdi-water-percent"> </span>
        <span id="current_humidity">--</span><span class="units">%</span> 
    </div>
    <div class="current_dewpoint"><span class="mdi mdi-waves"> </span>
        <span id="current_dewpoint"> --.-</span><span class="units">${getAbbrev(display_dew_point)}</span> 
    </div>
    """
    
    if (show_dewpoint) html += """
    <div class="dewpoint_text">
        <span id="dewpoint_text"> -------</span>
    </div>
    """
    
    html += """
    <div class="current_condition1"> 
        <span id="current_condition1">------- </span>
    </div>
    <div class="current_condition2"> 
        <span id="current_condition2">------- </span>
    </div>

    <div class="wind_title">
        <span class="mdi mdi-weather-windy-variant"> Wind</span>
    </div>
    <div class="current_wind_speed"><span class=" mdi mdi-tailwind"> </span>
        <span id="current_wind_speed"> -- </span><span class="units">${getAbbrev(display_wind_speed)}</span> 
    </div>
    <div class="current_wind_gust"><span class=" mdi mdi-weather-windy"> </span>
        <span id="current_wind_gust"> -- </span><span class="units">${getAbbrev(display_wind_gust)}</span> 
    </div>
    <div class="current_wind_direction"><span class="mdi mdi-compass-outline"> </span>
        <span id="current_wind_direction"> --</span><span class="units">${getAbbrev(display_wind_direction)}</span> 
    </div>

    <div class="sunrise">
        <span id="sunrise" class="mdi mdi-weather-sunset-up"> --:-- </span> 
    </div>
    <div class="sunset">
        <span id="sunset" class="mdi mdi-weather-sunset-down"> --:-- </span> 
    </div>
    </div>

    <!-- The Weather Graph -->
    <div id="precipitationGraph" class="graphWindow">

      <!-- Modal content -->
      <div class="graphWindow-content">
        <div class="daily-graph" id="daily_precipitation_graph" style="width: 90vw; height: 75vh">
      </div>

    </div>
"""
    return html;
   
}

def defineHTML_globalVariables(){
    def html = """
        google.load('visualization', '1.0', {'packages':['corechart']});
        google.setOnLoadCallback(drawChart);

        var sunrise;
        var sunset;
        let options = [];
        let pws_data = [];
        let currentTemperature;
    """
}


def getWeatherTile() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    html = defineHTML_Header();
    html += "<head><style>";
    //CSS
    html += defineHTML_CSS();
    html += defineHTML_GraphWindowCSS();
    html += """</style></head><body onload="initialize()">"""
    html += defineHTML_Tile();
    
    html+="</body></html>"
 
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

