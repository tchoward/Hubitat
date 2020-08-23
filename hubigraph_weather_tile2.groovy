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
    name: "Hubigraph Weather Tile 2",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Weather Tile 2",
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
    def unitPrecip =     [["millimeters": "Millimeters (mm)"], ["inches": """Inches (") """]];
    def unitPressure =   [["millibars": "Millibars (mbar)"], ["millimeters_mercury": "Millimeters of Mercury (mmHg)"], ["inches_mercury": "Inches of Mercury (inHg)"], ["hectopascal" : "Hectopascal (hPa)"]];
    def unitDirection =  [["degrees": "Degrees (°)"], ["radians": "Radians (°)"], ["cardinal" : "Cardinal (N, NE, E, SE, etc)"]];
    def unitTrend =      [["trend_numeric": "Numeric (↑ < 0, → = 0, ↓ > 0)"], ["trend_text": "Text (↑ rising, → steady, ↓ falling)"]];
    def unitPercent =    [["percent_numeric": "Numeric (0 to 100)"], ["percent_decimal": "Decimal (0.0 to 1.0)"]];
    def unitTime =       [["time_seconds" : "Seconds since 1970"], ["time_milliseconds" : "Milliseconds since 1970"], ["time_twelve" : "12 Hour (2:30 PM)"], ["time_two_four" : "24 Hour (14:30)"]];
    def unitDate =       [["day_only": "Day Only (Thursday)"],["date_only":"Date Only (29)"],["day_date":"Day and Date (Thursday 29)"], ["month_day":"Month and Day (June 29)"]];  
    
    
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
            
            
            
                       
            container << parent.hubiForm_switch     (this, title: "Color Icons?", name: "color_icons", default: false);
                        
            parent.hubiForm_container(this, container, 1);
            def daysEnum = [[0: "Today"], [1: "Tomorrow"], [2: "2 Days from Now"], [3: "3 Days from Now"], [4: "4 Days from Now"], [5: "Five Days from Now"]];
            input( type: "enum", name: "day_num", title: "Day to Display", multiple: false, required: false, options: daysEnum, defaultValue: "1");        

        } 
        
        def decimalEnum =     [[0: "None (0)"], [1: "One (0.1)"], [2: "Two (0.12)"], [3: "Three (0.123)"], [4: "Four (0.1234)"]];
        atomicState.selections.each {measurement->
            parent.hubiForm_section(this, measurement.title, 1){
                container = [];
                container << parent.hubiForm_switch     (this, title: "Display "+measurement.title+"?", name: measurement.var+"_display", default: true, submit_on_change: true);

                if ((settings["${measurement.var}_display"]==null) || (settings["${measurement.var}_display"]==true)){
                                        
                    container << parent.hubiForm_slider   (this, title: "Text Weight (400 = normal, 700= bold)", 
                                                           name:  measurement.var+"_font_weight",  
                                                           default: measurement.font_weight, 
                                                           min: 100,
                                                           max: 900, 
                                                           units: "",
                                                           submit_on_change: false);
                
                    container << parent.hubiForm_color(this, "Font", measurement.var, "#FFFFFF", false);
                    parent.hubiForm_container(this, container, 1); 
                    
                    if (measurement.decimal == "yes"){
                        container = [];
                        container << parent.hubiForm_switch     (this, title: "Display Unit Values (mm, mph, mbar, °, etc)", name: measurement.var+"_display_units", default: true, submit_on_change: false);
                        parent.hubiForm_container(this, container, 1);
                        input( type: "enum", name: measurement.var+"_decimal", title: "Decimal Places", required: false, multiple: false, options: decimalEnum, defaultValue: 1, submitOnChange: false)
                    }
                    if (measurement.imperial != "none"){
                        input( type: "enum", name: measurement.var+"_units", title: "Displayed Units", required: false, multiple: false, options: measurement.unit, defaultValue: measurement.iu, submitOnChange: false)
                    }
                } else{
                     parent.hubiForm_container(this, container, 1); 
                }
            } //section
        }
           
            
    }//page
}//function

def parseAttribute(str){
     val = str.split('_');
     return val[1];
}

def parseSensor(str){
     val = str.split('_');
     return sensors.find( { it.id == val[0]} );
}

def tilePlacementPage(){
    
}

def deviceSelectionPage() {
    def final_attrs;
    def unitTemp =       [["fahrenheit": "Fahrenheit (°F)"], ["celsius" : "Celsius (°C)"], ["kelvin" : "Kelvin (K)"]];
    def unitWind =       [["meters_per_second": "Meters per Second (m/s)"], ["miles_per_hour": "Miles per Hour (mph)"], ["knots": "Knots (kn)"], ["kilometers_per_hour": "Kilometers per Hour (km/h)"]];
    def unitLength =     [["millimeters": "Millimeters (mm)"], ["inches": """Inches (") """]];
    def unitPressure =   [["millibars": "Millibars (mbar)"], ["millimeters_mercury": "Millimeters of Mercury (mmHg)"], ["inches_mercury": "Inches of Mercury (inHg)"], ["hectopascal" : "Hectopascal (hPa)"]];
    def unitDirection =  [["degrees": "Degrees (°)"], ["radians", "Radians (°)"], ["cardinal", "Cardinal (N, NE, E, SE, etc)"]];
    def unitTrend =      [["trend_numeric": "Numeric (↑ < 0, → = 0, ↓ > 0)"], ["trend_text": "Text (↑ rising, → steady, ↓ falling)"]];
    def unitPercent =    [["percent_numeric": "Numeric (0 to 100)"], ["percent_decimal": "Decimal (0.0 to 1.0)"]];
    
    
    dynamicPage(name: "deviceSelectionPage") {
         parent.hubiForm_section(this,"Override OpenWeather", 1){
             container = [];
             container << parent.hubiForm_switch(this, title: "Override OpenWeatherMap values with PWS?", name: "override_openweather", default: false, submit_on_change: true);
             parent.hubiForm_container(this, container, 1);
         }

         if (override_openweather == true){
             parent.hubiForm_section(this,"Sensor Selection", 1){ 
                 container = [];

                 input ("sensors", "capability.*", title: "Select Sensors/PWS", multiple: true, required: false, submitOnChange: true);
                 parent.hubiForm_container(this, container, 1); 
             }
             
            if (sensors){
                final_attrs = [];
                final_attrs << ["openweather" : "Open Weather Map"];

                sensors.each{sensor->
                    attributes_ = sensor.getSupportedAttributes();                    
                    attributes_.each{ attribute_->
                        name = attribute_.getName();
                        if (sensor.currentState(name)){
                            final_attrs << ["${sensor.id}_${name}" : "${sensor.displayName} (${name}) ::: [${sensor.currentState(name).getValue()} ${sensor.currentState(name).getUnit() ? sensor.currentState(name).getUnit() : ""} ]"];
                        }
                    }
                    final_attrs = final_attrs.unique(false);
                }
                  
                parent.hubiForm_section(this,"Override Values - Select Attribute Units", 1){ 
                    atomicState.selections.each{feature->
                        container = [];
                        
                        if (feature.can_be_overriden == "yes"){
                            container <<  parent.hubiForm_sub_section(this, feature.title); 
                            parent.hubiForm_container(this, container, 1);
                            
                            container = [];
                            input( type: "enum", name: "${feature.var}_override", title: feature.title, required: false, multiple: false, options: final_attrs, defaultValue: "openweather", submitOnChange: true)
                              
                            if (settings["${feature.var}_override"]!=null && settings["${feature.var}_override"] != "openweather" && feature.unit != "none") {
                                attribute = parseAttribute(settings["${feature.var}_override"]);
                                sensor = parseSensor(settings["${feature.var}_override"]);
                                unit = sensor.currentState(attribute).getUnit();
                                value = sensor.currentState(attribute).getValue();    
                                if (unit==null) unit = "blank";
                                detectedUnits = getUnits(unit, value);
                            
                                validUnits = feature.unit.any{ detectedUnits.var in it };
                                    
                                if (detectedUnits.name != "unknown" && validUnits){
                                    container <<  parent.hubiForm_text(this, "Detected units = "+detectedUnits.name);
                                    app.updateSetting("${feature.var}_units", detectedUnits.var);
                                    parent.hubiForm_container(this, container, 1);
                                } else {
                                    if (detectedUnits.name == "unknown"){
                                        container <<  parent.hubiForm_text(this, "Unknown Units, Please update Below");
                                    } else {
                                        container <<  parent.hubiForm_text(this, "<span style='color:red'>Error: Units detected = ${detectedUnits.name}</span><br><small>Please choose a different device above, or override the units below</small>");
                                    }
                                    parent.hubiForm_container(this, container, 1);
                                    input( type: "enum", name: "${feature.var}_units", title: "Override "+feature.title+" Units", required: false, multiple: false, options: feature.unit, defaultValue: feature.unit, submitOnChange: false)
                                } 
                                
                            } else if (settings["${feature.var}_units"]!=null && settings["${feature.var}_override"] != "openweather"){
                                app.updateSetting("${feature.var}_units", null);
                            }
                        }
                    }
                }  //parent   
            }  //if sensor
            
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
    def unitTemp =       [["fahrenheit": "Fahrenheit (°F)"], ["celsius" : "Celsius (°C)"], ["kelvin" : "Kelvin (K)"]];
    def unitWind =       [["meters_per_second": "Meters per Second (m/s)"], ["miles_per_hour": "Miles per Hour (mph)"], ["knots": "Knots (kn)"], ["kilometers_per_hour": "Kilometers per Hour (km/h)"]];
    def unitMeasure =     [["millimeters": "Millimeters (mm)"], ["inches": """Inches (") """]];
    def unitPressure =   [["millibars": "Millibars (mbar)"], ["millimeters_mercury": "Millimeters of Mercury (mmHg)"], ["inches_mercury": "Inches of Mercury (inHg)"], ["hectopascal" : "Hectopascal (hPa)"]];
    def unitDirection =  [["degrees": "Degrees (°)"], ["radians" : "Radians (°)"], ["cardinal": "Cardinal (N, NE, E, SE, etc)"]];
    def unitTrend =      [["trend_numeric": "Numeric (↑ < 0, → = 0, ↓ > 0)"], ["trend_text": "Text (↑ rising, → steady, ↓ falling)"]];
    def unitPercent =    [["percent_numeric": "Numeric (0 to 100)"], ["percent_decimal": "Decimal (0.0 to 1.0)"]];
    def unitTime =       [["time_seconds" : "Seconds since 1970"], ["time_milliseconds" : "Milliseconds since 1970"], ["time_twelve" : "12 Hour (2:30 PM)"], ["time_two_four" : "24 Hour (14:30)"]];

    
   
    atomicState.tile_dimensions = [rows: 14, columns: 26];
    
    atomicState.selections = [[title: 'Forecast Weather Icon',          var: "weather_icon",                       
                                                                        ow:  "current.weather.0.description", can_be_overriden: "no",
                                                                        iu:  "none", icon: "mdi-alert-circle", icon_loc: "none",  icon_space: "",  
                                                                        h: 3,  w: 12, baseline_row: 1,  baseline_column:  13, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: "none",   decimal: "no",  
                                                                        font: 40, font_weight: "900", 
                                                                        imperial: "none",   metric: "none",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              ],
                              [title: 'Current Weather',                var: "description",                          
                                                                        ow: "current.weather.0.description", can_be_overriden: "no",
                                                                        iu: "none", icon: "none", icon_loc: "none",  icon_space: "",  
                                                                        h: 2,  w: 12, baseline_row: 4,  baseline_column:  13, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: "none",   decimal: "no",  
                                                                        font: 20, font_weight: "400", 
                                                                        imperial: "none",   metric: "none",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              ],
                              [title: 'Current Temperature',            var: "current_temperature",                      
                                                                        ow: "current.temp", can_be_overriden: "yes",
                                                                        iu: "fahrenheit", icon: "none", icon_loc: "left",  icon_space: "",  
                                                                        h: 2,  w: 12, baseline_row: 1,  baseline_column:  1, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "no",  
                                                                        font: 20, font_weight: "400", 
                                                                        imperial: "farenheit",   metric: "celsius",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              ], 
                              [title: 'Feels Like',                     var: "feels_like",                 
                                                                        ow: "current.feels_like", can_be_overriden: "yes",
                                                                        iu: "fahrenheit", icon: "mdi-home-thermometer-outline", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 12, baseline_row: 3,  baseline_column:  1, 
                                                                        alignment: "center", text: "Feels Like: ",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "no",  
                                                                        font: 7, font_weight: "400", 
                                                                        imperial: "farenheit",   metric: "celsius",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Forecast High',                  var: "forecast_high",                       
                                                                        ow: "daily.0.temp.max", can_be_overriden: "no",
                                                                        iu: "fahrenheit", icon: "mdi-arrow-up-thick", icon_loc: "left",  icon_space: "",  
                                                                        h: 2,  w: 6, baseline_row: 4,  baseline_column:  7, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "no",  
                                                                        font: 7, font_weight: "400", 
                                                                        imperial: "farenheit",   metric: "celsius",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ], 
                              [title: 'Forecast Low',                 var: "forecast_low",    
                                                                        ow: "daily.0.temp.min", can_be_overriden: "no",
                                                                        iu: "fahrenheit", icon: "mdi-arrow-down-thick", icon_loc: "left",  icon_space: "",  
                                                                        h: 2,  w: 6, baseline_row: 4,  baseline_column:  1, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "no",  
                                                                        font: 6, font_weight: "400", 
                                                                        imperial: "farenheit",   metric: "celsius",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Precipitation Title',           var: "precipitation_title",                      
                                                                        ow: "none", can_be_overriden: "no",
                                                                        iu: "none", icon: "mdi-umbrella-outline", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 6,  baseline_column:  1, 
                                                                        alignment: "center", text: "Precipitation",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitMeasure,   decimal: "no",  
                                                                        font: 6, font_weight: "400", 
                                                                        imperial: "none",   metric: "none",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Forcast Precipitation',         var: "forecast_precipitation",                          
                                                                        ow: "daily.0.rain", can_be_overriden: "no",
                                                                        iu: "millimeters", icon: "mdi-ruler", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 8,  baseline_column:  1, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitMeasure,   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "inches",   metric: "militmeters",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Forecast Percent Precipitation', var: "forcast_percent_precipitation",                       
                                                                        ow: "daily.0.pop", can_be_overriden: "no",
                                                                        iu: "percent_decimal", icon: "mdi-cloud-question", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 7,  baseline_column: 1, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitPercent,   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "percent_numerical",   metric: "percent_numerical",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Current Precipitation',          var: "current_precipitation",                          
                                                                        ow: "current.rain.1h", can_be_overriden: "yes",
                                                                        iu: "millimeters", icon: "mdi-calendar-today", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 9,  baseline_column:  1, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitMeasure,   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "inches",   metric: "millimeters",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Wind Title',                    var: "wind_title",                  
                                                                        ow: "none", can_be_overriden: "no",
                                                                        iu: "meters_per_second", icon: "mdi-weather-windy-variant", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 6,  baseline_column:  9, 
                                                                        alignment: "center", text: "Wind",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: "none",   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "none",   metric: "none",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Wind Speed',                     var: "wind_speed",                         
                                                                        ow: "current.wind_speed", can_be_overriden: "yes",
                                                                        iu: "meters_per_second", icon: "mdi-tailwind", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 7,  baseline_column:  9, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitWind,   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "miles_per_hour",   metric: "meters_per_second",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Wind Gust',                     var: "wind_gust",                       
                                                                        ow: "current.wind_gust", can_be_overriden: "yes",
                                                                        iu: "meters_per_second", icon: "mdi-weather-windy", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 8,  baseline_column:  9, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitWind,   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "miles_per_hour",   metric: "meters_per_second",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Wind Direction',                var: "wind_direction",                      
                                                                        ow: "current.wind_deg", can_be_overriden: "yes",
                                                                        iu: "degrees", icon: "mdi-compass-outline", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 9,  baseline_column:  9, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitDirection,   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "cardinal",   metric: "cardinal",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                                                  
                              [title: 'Pressure Title',               var: "pressure_title",                   
                                                                        ow: "none", can_be_overriden: "no",
                                                                        iu: "none", icon: "mdi-gauge", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 6,  baseline_column:  17, 
                                                                        alignment: "center", text: "Pressure",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: "none",   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "none",   metric: "none",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ], 
                              [title: 'Current Pressure',             var: "current_pressure",                     
                                                                        ow: "current.pressure", can_be_overriden: "yes",
                                                                        iu: "millibars", icon: "mdi-thermostat", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 8, baseline_row: 7,  baseline_column:  17, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitPressure,   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "inches_mercury",   metric: "millimeters_mercury",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Pressure Trend',                var: "pressure_trend",                        
                                                                        ow: "current.pressure", can_be_overriden: "yes",
                                                                        iu: "none", icon: "none", icon_loc: "none",  icon_space: "",  
                                                                        h: 1,  w: 8, baseline_row: 8,  baseline_column:  17, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: "none",   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "none",   metric: "none",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Humidity',                      var: "current_humidity",                         
                                                                        ow: "current.humidity", can_be_overriden: "yes",
                                                                        iu: "percent_numeric", icon: "mdi-water-percent", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 4, baseline_row: 11,  baseline_column:  1, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitPercent,   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "percent_numeric",  metric: "percent_numeric",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ], 
                               [title: 'Dewpoint Description',          var: "dewpoint_description",                   
                                                                        ow: "current.dew_point", can_be_overriden: "no",
                                                                        iu: "none", icon: "none", icon_loc: "none",  icon_space: " ",  
                                                                        h: 1,  w: 6, baseline_row: 11,  baseline_column:  5, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: "none",   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "none",  metric: "none",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ], 
                              [title: 'Current Dewpoint',             var: "current_dewpoint",                          
                                                                        ow: "current.dew_point", can_be_overriden: "yes",
                                                                        iu: "farenheit", icon: "mdi-wave", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 4, baseline_row: 11,  baseline_column: 11, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "no",  
                                                                        font: 4, font_weight: "400", 
                                                                        imperial: "farenheit",   metric: "celsius",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Sunrise',                        var: "sunrise",                  
                                                                        ow:  "current.sunrise", can_be_overriden: "no",
                                                                        iu:  "time_seconds", icon: "mdi-weather-sunset-up", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 5, baseline_row: 11,  baseline_column:  15, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTime,   decimal: "no",  
                                                                        font: 3, font_weight: "400", 
                                                                        imperial: "time_twelve",   metric: "time_two_four",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
                              [title: 'Sunset',                        var: "sunset",                      
                                                                        ow: "current.sunset", can_be_overriden: "no",
                                                                        iu: "time_seconds", icon: "mdi-weather-sunset-down", icon_loc: "left",  icon_space: " ",  
                                                                        h: 1,  w: 5, baseline_row: 11,  baseline_column:  20, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTime,   decimal: "no",  
                                                                        font: 3, font_weight: "400", 
                                                                        imperial: "time_twelve",   metric: "time_two_four",
                                                                        font_color: "#2c3e50", background_color: "#18bc9c",
                                                                        font_auto_resize: "true", justification: "center", font_static_size: "none"
                              
                              ],
    ];

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
                             container << parent.hubiForm_text_input(this, "Name for Tile Device", "device_name", "Forecast Tile", "false");
                        }
                        parent.hubiForm_container(this, container, 1); 
                    }
                } 
             
            
               if (state.endpoint){
                   parent.hubiForm_section(this, "Hubigraph Application", 1, "settings"){
                        container = [];
                        container << parent.hubiForm_sub_section(this, "Application Name");
                        container << parent.hubiForm_text_input(this, "Rename the Application?", "app_name", "Hubigraph Forecast Tile", "false");
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
        case "fahrenheit": return "&deg;";
        case "celsius": return "&deg;";
        case "kelvin": return "K";
        case "meters_per_second": return "m/s";
        case "miles_per_hour": return "mph";
        case "knots": return "kn";
        case "millimeters": return "mm";
        case "inches": return '"';
        case "degrees": return "&deg;";
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


def getTileOptions(){  
    
    def options = [
        "tile_units": tile_units,
        "display_day": day_num,
        "color_icons": color_icons,
        "openweather_refresh_rate": openweather_refresh_rate,
        "measurements": [],
        ];
    
        
    atomicState.selections.each{measurement->
        outUnits = settings["${measurement.var}_units"] ? settings["${measurement.var}_units"] : "none";
        decimals = measurement.decimal == "yes" ? settings["${measurement.var}_decimal"] : "none";
       
        options.measurements << [ "name": measurement.var, 
                                  "openweather": measurement.ow, 
                                  "in_unit" : measurement.iu, 
                                  "out_unit" : outUnits,
                                  "decimals" : decimals,
                                ];  
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

def defineHTML_Header(){
    def html = """
    <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
    <link rel="stylesheet" href="https://code.getmdl.io/1.3.0/material.indigo-pink.min.css">
    <link rel="stylesheet" href="//cdn.materialdesignicons.com/5.4.55/css/materialdesignicons.min.css">

    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css">
    <link rel="stylesheet" href="/local/gridstack.css">

    <script>
        const localURL =         "${state.localEndpointURL}";
        const secretEndpoint =   "${state.endpointSecret}";
        const latitude =         "${latitude}";
        const longitude =        "${longitude}";
        const tile_key =         "${tile_key}";
    </script>
    <script src="https://code.jquery.com/jquery-3.5.1.min.js" integrity="sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=" crossorigin="anonymous"></script>
    <script src="https://code.getmdl.io/1.3.0/material.min.js"></script>
    <script defer src="http://192.168.1.64:8080/a7af9806-4b0e-4032-a78e-a41e27e4d685-WeatherTile.js"></script>
    

    <script src="https://cdn.jsdelivr.net/npm/gridstack@1.1.2/dist/gridstack.js"></script>
    <script src="https://code.jquery.com/ui/1.12.1/jquery-ui.min.js" integrity="sha256-VazP97ZCwtekAsvgPBSUwPFKdrwD3unUfSGVYrahUqU=" crossorigin="anonymous"></script>
    <script src="https://cdn.jsdelivr.net/npm/gridstack@1.1.2/dist/gridstack.jQueryUI.js"></script>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jqueryui-touch-punch/0.2.3/jquery.ui.touch-punch.min.js" integrity="sha512-0bEtK0USNd96MnO4XhH8jhv3nyRF0eK87pJke6pkYf3cM0uDIhNJy9ltuzqgypoIFXw3JSuiy04tVk4AjpZdZw==" crossorigin="anonymous"></script>
    
    """
    return html;
}

def defineHTML_CSS(){
    
    def num_columns = atomicState.tile_dimensions.columns;
    def column_width = 100.0/num_columns;
    
    def num_rows =  atomicState.tile_dimensions.rows;
    def row_height = 100.0/num_rows;
   

    def html = """
    """

    def idx = 1;
    current_row = 2; //leave top row blank  
    atomicState.selections.each{item->
        var = item.var;
        if (settings["${var}_display"]){
            color = settings["${var}_color"];
            font = settings["${var}_font"];
            weight = settings["${var}_font_weight"];
            row_start = item.baseline_row;
            row_end = item.baseline_row + item.h;
            column_start = item.baseline_column;
            column_end = item.baseline_column + item.w;
            html += 
                """
                .${var}{
                     grid-row-start: ${row_start};
                     grid-row-end: ${row_end}; 
                     grid-column-start: ${column_start};
                     grid-column-end: ${column_end};
                     font-size: ${font}vh;
                     padding-top: 0vmin !important; 
                     padding-left:  ${item.lpad}vw !important;
                     padding-right: ${item.rpad}vw !important;
                     text-align: ${item.alignment} !important;
                     color: ${color} !important;
                     font-weight: ${weight};
                }
               """
        }
    }
    html += """
.grid-stack {
  background: #000000;
}

.grid-stack-item-content {
  color: #2c3e50;
  text-align: center;
  background-color: #18bc9c;
  left: 1px !important;
  right: 1px !important;
}

.grid-stack-item-content{overflow:hidden !important}

/* Optional styles for demos */
.btn-primary {
  color: #fff;
  background-color: #007bff;
}

.btn {
  display: inline-block;
  padding: .375rem .75rem;
  line-height: 1.5;
  border-radius: .25rem;
}

.font-test{
    line-height: 10vw;
    padding-top: 0px !important;
    font-size: 10vw;
    margin: 0 !important;
    text-align: center;
}

a {
  text-decoration: none;
}

h1 {
  font-size: 2.5rem;
  margin-bottom: .5rem;
}

.placeholder-content{
    left: 0;
    right: 0;
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
    <style type="text/css">
    .grid-stack-item-removing {
      opacity: 0.8;
      filter: blur(5px);
    }

    #trash {
      background: rgba(255, 0, 0, 0.4);
    }
    </style>
    <body>
  
    <div class="grid-stack grid-stack-26" data-gs-animate="yes" data-gs-verticalMargin="1" data-gs-column="26" id="main_grid">
    """
    fontScale = 4.6;
    lineScale = 0.85;
    iconScale = 3.5;
    
    atomicState.selections.each{item->
        var = item.var;
        height = item.h*2;
        html += """ <div  class="grid-stack-item" data-gs-id = "${var}" data-gs-x="${item.baseline_column}" 
                                                  data-gs-y="${item.baseline_row*2-1}" data-gs-width="${item.w}" data-gs-height="${height}" data-gs-locked="false"
                                                  ondblclick="setOptions('${var}')">

                    <div id="${var}_title" style="display: none;">${item.title}</div>
                    <div id="${var}_auto_font" style="display: none;">${item.font_auto_resize}</div>

                    <div id="${var}_tile" class="grid-stack-item-content" style="font-size: ${fontScale*height}vh; 
                                                                          line-height: ${fontScale*lineScale*height}vh;
                                                                          text-align: ${item.justification};
                                                                          background-color: ${item.background_color};"> """;
        
        //Left Icon
        if (item.icon != "none" && item.icon_loc == "left"){
            html+="""<span id="${var}_icon" class="mdi ${item.icon}" style="font-size: ${iconScale*height}vh; color: ${item.font_color};">${item.icon_space}</span>""";
        }
        //Text
        html+="""<span id="${var}_text" style="color: ${item.font_color};">${item.text}</span>""";
        
        //Main Content
        html += """<span id="${var}" style="color: ${item.font_color};"></span>"""
        
        //Units
        units = getAbbrev(settings["${var}_units"]);
        if (settings["${var}_units"] && item.imperial != "none" && units != "unknown") html+="""<span id="${var}_units">${units}</span>""";  
        
        //Right Icon
        if (item.icon != "none" && item.icon_loc == "right"){
            html+="""<span>${item.icon_space}</span>""";
            html+="""<span id="${var}_icon" class="mdi ${item.icon}" style="color: ${item.font_color};"></span>""";
        }
        html += """</div></div>""";
    }        
    html += """
    </div>
    </div>
    </div>
    """
    
    html+= """
    <style>
  .mdl-layout__title {
    padding-bottom: 20px;
    background: transparent;
  }

  .mdl-grid__hubitat {
    padding: 0px !important;
    margin: 5px !important;
  }

  .mdl-dialog__content {
    padding: 0px !important;
    margin: 5px !important;
  }

  .mdl-dialog {
    width: 75vw !important;
  }

  .is-checked {}
</style>
<dialog class="mdl-dialog mdl-shadow--12dp" tabindex="-1">
  <div class="mdl-dialog__content">

    <div class="mdl-layout">
      <div id="options_title" class="mdl-layout__title">
        Options
      </div>

      <div class="mdl-grid" style="width: 100%">
        <!-- ALIGNMENT -->
        <div id="alignment_grid" class="mdl-grid"  style="width: 100%; padding: 0px;">
        <div class="mdl-cell" style="width: auto; flex-grow: 1;" tabindex="-1">
          <span>Alignment</span>
        </div>
        <div class="mdl-cell"  style="width: auto">
            <button id = "left_justify_button" class="mdl-button mdl-js-button mdl-button--icon" onclick="buttonClicked('left')" tabindex="-1">
            <i class="material-icons" tabindex="-1">align_horizontal_left</i>
            </button>
        </div>
        <div class="mdl-cell" style="width: auto">
          <button id = "center_justify_button" class="mdl-button mdl-js-button mdl-button--icon" onclick="buttonClicked('center')">
            <i class="material-icons">align_horizontal_center</i>
          </button>
        </div>
        <div class="mdl-cell" style="width: auto">
          <button id = "right_justify_button" class="mdl-button mdl-js-button mdl-button--icon" onclick="buttonClicked('right')">
            <i class="material-icons">align_horizontal_right</i>
          </button>
        </div>
        <div class="mdl-layout-spacer"></div>
        </div>

       <div id="text_color_grid" class="mdl-grid"  style="width: 100%; padding: 0px;">
        <!-- TEXT COLOR -->
        <div class="mdl-cell" style="width: auto; flex-grow: 1;">
          <span><label for="Text Color">Choose TEXT color </label></span>
        </div>
        <div class="mdl-cell" style="width: auto">
          <span><input type="color" id="textColor" name="textColor" value="#FFFFFF"></span>
        </div>
        </div>
        
        <div id="tile_color_grid" class="mdl-grid"  style="width: 100%; padding: 0px;">
        <!-- TILE COLOR -->
        <div class="mdl-cell" style="width: auto; flex-grow: 1;">
          <label for="Background Color">Choose TILE color </label>
        </div>
        <div class="mdl-cell" style="width: auto">
          <input type="color" id="tileColor" name="bkcolor" value="#000000">
        </div>
        </div>

        <!-- ADJUST TEXT SIZE -->
        <div id="font_adjustment_grid" class="mdl-grid"  style="width: 100%; padding: 0px;">
            <div class="mdl-cell" style="width: auto; flex-grow: 1;">
                Automatic Font Size Adjustment
            </div>
            <div class="mdl-cell" style="width: auto">
                <label id="tileStaticFontSwitch" class="mdl-switch mdl-js-switch mdl-js-ripple-effect is-checked" for="tileStaticFont">
                    <input type="checkbox" id="tileStaticFont" 
                                           class="mdl-switch__input"
                                           onchange="enableFontSize(tileStaticFont.checked)" checked>
                </label>
            </div>
        </div>

        <!-- TEXT SIZE ADJUSTMENT -->
        <script language="javascript">
          function showMessage(value) {
            document.getElementById("message").innerHTML = value;
          }	   
        </script>
 
        <div id="fontSizeControl" class="mdl-grid"  style="width: 100%; padding: 0px;">
            <div class="mdl-cell" style="width: auto; flex-grow: 1;">
              <label for="tileFontSize">Adjust Font (Pixels)</label>
              <input id="tileFontSize" class="mdl-slider mdl-js-slider" type="range" min="-100" max="100" value="0" tabindex="0"
                oninput="showMessage(this.value)" onchange="showMessage(this.value)">
            </div>
            <div class="mdl-cell" style="width: auto">
              Value: <div id="message">0</div>
            </div>
        </div>

        <!-- CUSTOM TEXT -->
        <div class="mdl-cell mdl-cell--12-col">
          <div id="text_floater" class="mdl-textfield mdl-js-textfield mdl-textfield--floating-label">
            <input class="mdl-textfield__input" type="text" id="tileText">
            <label class="mdl-textfield__label" for="tileText">Static Text</label>
          </div>
        </div>
        
        <!-- ICON -->
        

      </div>

    </div>
  </div>
  <div class="mdl-dialog__actions">
    <button id="dialog_button" type="button" class="mdl-button close" onclick="closeWindow()"> Done </button>
  </div>
</dialog>
    """;
    
    return html;
   
}

def defineHTML_globalVariables(){
    def html = """
        var sunrise;
        var sunset;
        let options = [];
        let pws_data = [];
        let currentTemperature;
    """
}
def defineScript(){
 
    def html = """
<script type="text/javascript">
  	

  </script>
"""
    return html;
}

def getWeatherTile() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    html = defineHTML_Header();
    html += """<head><meta name="viewport" content="width=device-width, initial-scale=1.0"><style>""";
    //CSS
    html += defineHTML_CSS();
    html += """</style></head><body onload="initializeWeather()">"""
    html += defineHTML_Tile();
    html += defineScript();
    
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

