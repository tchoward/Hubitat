import groovy.json.*;
import java.text.SimpleDateFormat;


/**
 *  Hubigraph Line Graph Child App
 *
 *  Copyright 2020, but let's behonest, you'll copy it
 *iu:
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
        
        path("/updateSettings/") {
            action: [
                POST: "updateSettings"
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
            //container << parent.hubiForm_text_input (this, "<b>Open Weather Map Key</b>", "tile_key", "", "true");
            
            //container << parent.hubiForm_text_input (this, "<b>Latitude (Default = Hub location)</b>", "latitude", location.latitude, false);
            //container << parent.hubiForm_text_input (this, "<b>Longitude (Default = Hub location)</b>", "longitude", location.longitude, false);
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
        atomicState.unit_type.each {key, measurement->
            if (measurement.out != "none" ){
                parent.hubiForm_section(this, measurement.name, 1){
                    container = [];    
                    parent.hubiForm_container(this, container, 1);
                    input( type: "enum", name: key+"_units", title: "Displayed Units", required: false, multiple: false, options: measurement.enum, defaultValue: measurement.out, submitOnChange: false)
                 
                } //section
            }    
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
         parent.hubiForm_section(this,"Device Selection", 1){
             container = [];
             container << parent.hubiForm_switch(this, title: "Make Hubitat Devices Available?", name: "override_openweather", default: false, submit_on_change: true);
             parent.hubiForm_container(this, container, 1);
         }

         if (override_openweather == true){
             parent.hubiForm_section(this,"Sensor Selection", 1){ 
                 container = [];
                 parent.hubiForm_container(this, container, 1); 
                 input ("sensors", "capability.*", title: "Select Sensors", multiple: true, required: false, submitOnChange: true);   
             }
         }
        if (sensors){
            final_attrs = [];
            sensor_list = [:];
            sensors.each{sensor->
                attributes_ = sensor.getSupportedAttributes();    
                sensor_list."${sensor.id}" = [:];
                    attributes_.each{ attribute_->
                        name = attribute_.getName();
                        if (sensor.currentState(name)){
                            units = sensor.currentState(name).getUnit();
                            value = sensor.currentState(name).getValue();
                            sensor_list."${sensor.id}"."${name}" = [ sensor_name: "${sensor.displayName}", value: value, unit: units, supported_unit: getUnits(units, value)];
                            final_attrs << ["${sensor.id}.${name}" : "${sensor.displayName} (${name}) ::: [${sensor.currentState(name).getValue()} ${sensor.currentState(name).getUnit() ? sensor.currentState(name).getUnit() : ""} ]"]; 
                        }
                    }
            }
            final_attrs = final_attrs.unique(false);
            
            measurement_list = [:];
            atomicState.unit_type.each{key, type->
                measurement_list."${key}" = [:];
                if (type.out != "none"){
                    parent.hubiForm_section(this, type.name, 1){ 
                       container = [];
                       input( type: "enum", name: "${key}_devices", title: type.name, required: false, multiple: true, options: final_attrs, defaultValue: "", submitOnChange: true)
                       if (settings["${key}_devices"]){
                            settings["${key}_devices"].each{ attr->
                                sensor_id = "${attr}".tokenize('.')[0];
                                if (!measurement_list."${key}"."${sensor_id}")
                                    measurement_list."${key}"."${sensor_id}" = [:];
                                
                                attr = "${attr}".tokenize('.')[1];
                                sensor_name = sensor_list."${sensor_id}"."${attr}".sensor_name;
                                
                                if (atomicState.unit_type."${key}".enum == "none"){
                                    container << parent.hubiForm_text(this, "<b>"+sensor_name+" :: "+attr+"</b>");
                                    measurement_list."${key}"."${sensor_id}"."${attr}" = [sensor_name: sensor_list."${sensor_id}"."${attr}".sensor_name,
                                                                                          in_units: "none"
                                                                                         ];
                          
                                } else if (sensor_list."${sensor_id}"."${attr}".supported_unit.var == key) {
                                    units = sensor_list."${sensor_id}"."${attr}".supported_unit.name;
                                    container << parent.hubiForm_text(this, "<b>"+sensor_name+" :: "+attr+"</b></br>"+'&#9;'+" Units = "+units); 
                                    measurement_list."${key}"."${sensor_id}"."${attr}" = [sensor_name: sensor_list."${sensor_id}"."${attr}".sensor_name,
                                                                                          in_units: sensor_list."${sensor_id}"."${attr}".supported_unit.units
                                                                                         ];
                                } else { 
                                    parent.hubiForm_container(this, container, 1);
                                    unit = sensor_list."${sensor_id}"."${attr}".unit;
                                    list = atomicState.unit_type."${key}".enum;
                                    if (list != "none")
                                        input( type: "enum", name: "${key}.${sensor_id}.${attr}", 
                                                             title: "<b>"+sensor_name+" :: "+attr+"</b><br>Valid units not detected ("+unit+'); Expected <b>"'+key+'"</b> type<br><small>Please select measurement units below</small>',
                                                             required: false, multiple: false, 
                                                             options: list, 
                                                             defaultValue: "", submitOnChange: false);
                                    
                                        measurement_list."${key}"."${sensor_id}"."${attr}" = [sensor_name: sensor_list."${sensor_id}"."${attr}".sensor_name,
                                                                                              in_units: settings["${key}.${sensor_id}.${attr}"]
                                                                                             ];
                                    
                                    container = [];
                                }
                            }      
                        }
                       parent.hubiForm_container(this, container, 1);
                      
                    }
                }
                
            }
        }
        atomicState.device_list = measurement_list;
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
    def unitDepth =      [["millimeters": "Millimeters (mm)"], ["inches": """Inches (") """]];
    def unitPressure =   [["millibars": "Millibars (mbar)"], ["millimeters_mercury": "Millimeters of Mercury (mmHg)"], ["inches_mercury": "Inches of Mercury (inHg)"], ["hectopascal" : "Hectopascal (hPa)"]];
    def unitDirection =  [["degrees": "Degrees (°)"], ["radians" : "Radians (°)"], ["cardinal": "Cardinal (N, NE, E, SE, etc)"]];
    def unitTrend =      [["trend_numeric": "Numeric (↑ < 0, → = 0, ↓ > 0)"], ["trend_text": "Text (↑ rising, → steady, ↓ falling)"]];
    def unitPercent =    [["percent_numeric": "Numeric (0 to 100)"], ["percent_decimal": "Decimal (0.0 to 1.0)"]];
    def unitTime =       [["time_seconds" : "Seconds since 1970"], ["time_milliseconds" : "Milliseconds since 1970"], ["time_twelve" : "12 Hour (2:30 PM)"], ["time_two_four" : "24 Hour (14:30)"]];
    def unitUVI=         [["uvi" : "UV Index"]];
    def unitDistance=    [["miles": "Miles"]];
    def unitBlank=       [["none": "None"]];
    def unitDayofWeek=   [["short": "Short (Thu)"], ["long": "Long (Thursday)"]];
    def unitText=        [["plain": "Unformatted"], ["title": "Title Format"], ["lowercase": "Lowercase"], ["uppercase" : "Uppercase"]];
    def unitIcon=        [["icon": "Default Icon"]];
    
    atomicState.tile_dimensions = [rows: 14, columns: 26];
         
    if (!atomicState.tile_settings){
  
        atomicState.span_type = [ current: [title: "Current Measurements", num_time: 0, time_units:  ""],  
                                  daily:   [title: "Daily Forecast", num_time: 7, time_units:  "day"],
                                  hourly:  [title: "Hourly Forecast", num_time: 48, time_units: "hour"],
                                  blank:   [title: "Blank Tile", num_time: 0, time_units:  ""],
                                  sensor:  [title: "Device Measurement", num_time: 0, time_units: ""],
                                ];
        
        atomicState.unit_type = [ 
                                  temperature:          [name: "Temperature",         enum: unitTemp,      out:  "fahrenheit",      parse_func: "formatNumericData"],
                                  percent:              [name: "Percentage",          enum: unitPercent,   out:  "percent_numeric", parse_func: "formatNumericData"],
                                  icon:                 [name: "Weather Icons",       enum: unitIcon,      out:  "none",            parse_func: "translateCondition"],
                                  pressure:             [name: "Pressure",            enum: unitPressure,  out:  "inches_mercury",  parse_func: "formatNumericData"],
                                  velocity:             [name: "Velocity",            enum: unitWind,      out:  "miles_per_hour",  parse_func: "formatNumericData"], 
                                  time:                 [name: "Time",                enum: unitTime,      out:  "time_twelve",     parse_func: "formatNumericData"],
                                  depth:                [name: "Depth",               enum: unitDepth,     out:  "inches",          parse_func: "formatNumericData"],
                                  direction:            [name: "Direction",           enum: unitDirection, out:  "cardinal",        parse_func: "formatNumericData"],
                                  uvi:                  [name: "UV Index",            enum: unitUVI,       out:  "uvi",             parse_func: "formatNumericData"],
                                  visibility:           [name: "Visibility",          enum: unitDistance,  out:  "visibility",      parse_func: "formatNumericData"],
                                  blank:                [name: "Blank Tile",          enum: unitBlank,     out:  "none",            parse_func: "none"],
                                  day:                  [name: "Day of Week",         enum: unitDayofWeek, out:  "short",           parse_func: "formatNumericData"],
                                  text:                 [name: "Text Description",    enum: unitText,      out:  "plain",           parse_func: "formatTextData"],
                                 ];
                                 
                                  
                                 
        
        atomicState.tile_type = [
                                 weather_icon:          [name: "Weather Icon",          type: "icon",                 ow: "weather.0.description", in_units: "none",                 current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
                                 weather_description:   [name: "Weather Description",   type: "text",                 ow: "weather.0.description", in_units: "none",                 current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
                                 
                                 feels_like:            [name: "Feels Like",            type: "temperature",          ow: "feels_like",            in_units: "fahrenheit",           current: "yes", hourly: "yes", daily: "no" , sensor: "no"],
                                 feels_like_morning:    [name: "Morning Feels Like",    type: "temperature",          ow: "feels_like.morn",       in_units: "fahrenheit",           current: "no",  hourly: "no",  daily: "yes", sensor: "no"],
                                 feels_like_day:        [name: "Day Feels Like",        type: "temperature",          ow: "feels_like.day",        in_units: "fahrenheit",           current: "no",  hourly: "no",  daily: "yes", sensor: "no"],
                                 feels_like_evening:    [name: "Evening Feels Like",    type: "temperature",          ow: "feels_like.eve",        in_units: "fahrenheit",           current: "no",  hourly: "no",  daily: "yes", sensor: "no"],                          
                                 feels_like_night:      [name: "Night Feels Like",      type: "temperature",          ow: "feels_like.night",      in_units: "fahrenheit",           current: "no",  hourly: "no",  daily: "yes", sensor: "no"],
                                
                                 temperature:           [name: "Temperature",            type: "temperature",         ow: "temp",                 in_units: "fahrenheit",           current: "yes", hourly: "yes", daily: "no", sensor: "no"],
                                 temperature_max:       [name: "Maximum Temperature",    type: "temperature",         ow: "temp.max",             in_units: "fahrenheit",           current: "no",  hourly: "no",  daily: "yes", sensor: "no"],
                                 temperature_min:       [name: "Minimum Temperature",    type: "temperature",         ow: "temp.min",             in_units: "fahrenheit",           current: "no",  hourly: "no", daily: "yes", sensor: "no"],
                                 temperature_morning:   [name: "Morning Temperature",    type: "temperature",         ow: "temp.morn",            in_units: "fahrenheit",           current: "no",  hourly: "no", daily: "yes", sensor: "no"],
                                 temperature_day:       [name: "Day Temperature",        type: "temperature",         ow: "temp.day",             in_units: "fahrenheit",           current: "no",  hourly: "no", daily: "yes", sensor: "no"],
                                 temperature_evening:   [name: "Evening Temperature",    type: "temperature",         ow: "temp.eve",             in_units: "fahrenheit",           current: "no",  hourly: "no", daily: "yes", sensor: "no"],                          
                                 temperature_night:     [name: "Night Temperature",      type: "temperature",         ow: "temp.night",           in_units: "fahrenheit",           current: "no",  hourly: "no", daily: "yes", sensor: "no"],
                                 
                                 humidity:              [name: "Humidity",               type: "percent",             ow: "humidity",             in_units: "percent_numeric",      current: "yes", hourly: "yes", daily: "yes", sensor: "no"],

                                 dew_point:             [name: "Dew Point",              type: "temperature",         ow: "dew_point",            in_units: "fahrenheit",           current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
                                 
                                 pressure:              [name: "Pressure",               type: "pressure",            ow: "pressure",             in_units: "millibars",            current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
     
                                 uv_index:              [name: "UV Index",               type: "uvi",                 ow: "uvi",                  in_units: "uvi",                  current: "yes", hourly: "no", daily: "yes", sensor: "no"],
                                 cloud_coverage:        [name: "Cloud Coverage",         type: "percent",             ow: "clouds",               in_units: "percent_numeric",      current: "yes", hourly: "no", daily: "yes", sensor: "no"],
                                 visibility:            [name: "Visibility",             type: "distance",            ow: "visibility",           in_units: "miles",                current: "yes", hourly: "no", daily: "yes", sensor: "no"],
                                 
                                 wind_speed:            [name: "Wind Speed",             type: "velocity",            ow: "wind_speed",           in_units: "meters_per_second",    current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
                                 wind_gust:             [name: "Wind Gust",              type: "velocity",            ow: "wind_gust",            in_units: "meters_per_second",    current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
                                 wind_direction:        [name: "Wind Direction",         type: "direction",           ow: "wind_deg",             in_units: "degrees",              current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
                                 
                                 rain_past_hour:        [name: "Rain past Hour",         type: "depth",               ow: "rain.1h",              in_units: "millimeters",          current: "yes", hourly: "yes", daily: "no", sensor: "no"],
                                 snow_past_hour:        [name: "Snow past Hour",         type: "depth",               ow: "snow.1h",              in_units: "millimeters",          current: "yes", hourly: "yes", daily: "no", sensor: "no"],
                                 rain:                  [name: "Rain",                   type: "depth",               ow: "rain",                 in_units: "millimeters",          current: "no",  hourly: "no",  daily: "yes", sensor: "no"],
                                 snow:                  [name: "Snow",                   type: "depth",               ow: "snow",                 in_units: "millimeters",          current: "no",  hourly: "no",  daily: "yes", sensor: "no"],
                                 chance_precipitation:  [name: "Chance of Precipitation",type: "percent",             ow: "pop",                  in_units: "percent_decimal",      current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
                                 
                                 sunrise:               [name: "Sunrise",                type: "time",                ow: "sunrise",              in_units: "time_seconds",         current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
                                 sunset:                [name: "Sunset",                 type: "time",                ow: "sunset",               in_units: "time_seconds",         current: "yes", hourly: "yes", daily: "yes", sensor: "no"],
                    
                                 hour:                  [name: "Hour",                   type: "time",                ow: "dt",                   in_units: "time_seconds",         current: "no", hourly: "yes", daily: "no", sensor: "no"],
                                 day:                   [name: "Day",                    type: "day",                 ow: "dt",                   in_units: "time_seconds",         current: "no", hourly: "yes", daily: "yes", sensor: "no"],
      
                                 blank:                 [name: "Blank Tile",             type: "blank",               ow: "none",                 in_units: "none",                 current: "no", hourly: "no", daily: "no", sensor: "no"]
        ];
    
        atomicState.tile_settings = [[title: 'Forecast Weather Icon',          var: "weather_icon",  type: "weather_icon", period:"current", value: "",            
                                                                               icon: "alert-circle", icon_loc: "center",  icon_space: "",  
                                                                               h: 6,  w: 12, baseline_row: 1,  baseline_column:  13, 
                                                                               alignment: "center", text: "", decimals: 1, 
                                                                               lpad: 0, rpad: 0, 
                                                                               unit: "none",   decimal: "no", unit_space: "",
                                                                               font: 40, font_weight: "100", 
                                                                               font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                               font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                                      ],
                                      [title: 'Current Weather',            var: "description", type: "weather_description", period:"current", value: 0,                
                                                                        icon: "none", icon_loc: "none",  icon_space: "",  
                                                                        h: 4,  w: 12, baseline_row: 7,  baseline_column:  13, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: "none",   decimal: "no",  unit_space: "",
                                                                        font: 20, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                                      ],
                                      [title: 'Current Temperature',        var: "current_temperature", type: "temperature", period:"current",     
                                                                        icon: "none", icon_loc: "left",  icon_space: "",  
                                                                        h: 4,  w: 12, baseline_row: 1,  baseline_column:  1, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "yes", unit_space: "", 
                                                                        font: 20, font_weight: "900", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                                  ], 
                                  [title: 'Feels Like',                     var: "feels_like", type: "feels_like", period:"current", 
                                                                        icon: "home-thermometer-outline", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 12, baseline_row: 5,  baseline_column:  1, 
                                                                        alignment: "center", text: "Feels Like: ",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "yes", unit_space: "", 
                                                                        font: 7, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Forecast High',                  var: "forecast_high", type: "temperature_max",  period:"daily.0",           
                                                                        icon: "arrow-up-thick", icon_loc: "left",  icon_space: "",  
                                                                        h: 4,  w: 6, baseline_row: 7,  baseline_column:  7, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "yes",  unit_space: "",
                                                                        font: 7, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ], 
                              [title: 'Forecast Low',                 var: "forecast_low", type: "temperature_min",  period:"daily.0", 
                                                                        icon: "arrow-down-thick", icon_loc: "left",  icon_space: "",  
                                                                        h: 4,  w: 6, baseline_row: 7,  baseline_column:  1, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "yes", unit_space: "",  
                                                                        font: 6, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Precipitation Title',           var: "precipitation_title", type: "blank",  period:"none",             
                                                                        icon: "umbrella-outline", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 11,  baseline_column:  1, 
                                                                        alignment: "center", text: "Precipitation",
                                                                        lpad: 0, rpad: 0,  decimals: 1, 
                                                                        unit: unitMeasure,   decimal: "no",  unit_space: "",
                                                                        font: 6, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Forcast Precipitation',         var: "forecast_precipitation", type: "rain", period:"daily.0",                      
                                                                        icon: "ruler", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 15,  baseline_column:  1, 
                                                                        alignment: "center", text: "",
                                                                        lpad: 0, rpad: 0,  decimals: 1, 
                                                                        unit: unitMeasure,   decimal: "yes", unit_space: "", 
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Forecast Percent Precipitation', var: "forcast_percent_precipitation", type: "chance_precipitation", period:"daily.0",                    
                                                                        icon: "cloud-question", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 13,  baseline_column: 1, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitPercent,   decimal: "yes", unit_space: "", 
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Current Precipitation',          var: "current_precipitation", type: "rain_past_hour", period:"current",                      
                                                                        icon: "calendar-today", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 17,  baseline_column:  1, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitMeasure,   decimal: "yes",  unit_space: "",
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Wind Title',                     var: "wind_title", type: "blank",   period:"none",               
                                                                        icon: "weather-windy-variant", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 11,  baseline_column:  9, 
                                                                        alignment: "center", text: "Wind",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: "none",   decimal: "no", unit_space: "",
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Wind Speed',                     var: "wind_speed", type: "wind_speed",  period:"current",                         
                                                                        icon: "tailwind", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 13,  baseline_column:  9, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitWind,   decimal: "yes", unit_space: " ", 
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Wind Gust',                     var: "wind_gust", type: "wind_gust",  period:"current",                        
                                                                        icon: "weather-windy", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 15,  baseline_column:  9, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitWind,   decimal: "yes", unit_space: " ", 
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Wind Direction',                var: "wind_direction", type: "wind_direction",  period:"current",                           
                                                                        icon: "compass-outline", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 17,  baseline_column:  9, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitDirection,   decimal: "no", unit_space: "", 
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                                                  
                              [title: 'Pressure Title',               var: "pressure_title", type: "blank", period:"current",                   
                                                                        icon: "gauge", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 11,  baseline_column:  17, 
                                                                        alignment: "center", text: "Pressure", decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: "none",   decimal: "yes", unit_space: "", 
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ], 
                              [title: 'Current Pressure',             var: "current_pressure", type: "pressure", period:"current",                     
                                                                        icon: "thermostat", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 8, baseline_row: 13,  baseline_column:  17, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitPressure,   decimal: "yes",  unit_space: " ",
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                             
                              [title: 'Humidity',                      var: "current_humidity", type: "humidity", period:"current",                              
                                                                        icon: "water-percent", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 4, baseline_row: 20,  baseline_column:  1, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitPercent,   decimal: "yes", unit_space: "", 
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ], 
                               
                              [title: 'Current Dewpoint',             var: "current_dewpoint", type: "dew_point", period:"current",                          
                                                                        icon: "wave", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 4, baseline_row: 20,  baseline_column: 11, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTemp,   decimal: "yes", unit_space: "", 
                                                                        font: 4, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Sunrise',                        var: "sunrise", type: "sunrise",  period:"current",                
                                                                        icon: "weather-sunset-up", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 5, baseline_row: 20,  baseline_column:  15, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTime,   decimal: "no",  unit_space: "",
                                                                        font: 3, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                              ],
                              [title: 'Sunset',                        var: "sunset", type: "sunset",  period:"current",                          
                                                                        icon: "weather-sunset-down", icon_loc: "left",  icon_space: " ",  
                                                                        h: 2,  w: 5, baseline_row: 20,  baseline_column:  20, 
                                                                        alignment: "center", text: "",  decimals: 1, 
                                                                        lpad: 0, rpad: 0, 
                                                                        unit: unitTime,   decimal: "no",  unit_space: "",
                                                                        font: 3, font_weight: "400", 
                                                                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                                                                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true,
                              
                             ],
                              
        ];
        
        
               
    } else {

        
        //Update the Output Types
        temp = atomicState.unit_type;
        temp.each{key, item->
            if (settings["${key}_units"]){
                temp."${key}".out = settings["${key}_units"];
            }
        }
        atomicState.unit_type = temp;
        
        //reset to OpenWeather Data
        temp = atomicState.tile_type;
        atomicState.tile_type.each{key, item->
            if (item.sensor == "no"){
                temp << ["${key}": item];    
            }
        }
        atomicState.tile_type = temp;
    }
    
    temp = atomicState.tile_type;
    count = 0;
    atomicState.device_list.each{ type, var1->
        if (vars != [:]){
            var1.each {device, var2->
                var2.each{attr, var3->
                    temp."device_${device}_${attr}_${type}" = [name: "${var3.sensor_name} :: ${attr} (${type})", type: "${type}", ow: "device.${device}.${attr}", in_units: var3.in_units, current: "no", hourly: "no", daily: "no", sensor: "yes"];
                }
            }
        }
    }
    atomicState.tile_type = temp;
    
    typeList =  new TreeMap([:]);
        typeList.main_list = new TreeMap([:]);
        atomicState.span_type.each{span_key, span->
            typeList.main_list.put("${span_key}", [name: span_key.capitalize()]);
            typeList."${span_key}" =  new TreeMap([:]);
            typeList."${span_key}".measurement_list = new TreeMap([:]);
            atomicState.tile_type.each {key, item-> 
                if (item."${span_key}" == "yes") 
                    typeList."${span_key}".measurement_list << ["${key}": [name: item.name]];
            }
            if (span.num_time > 0) {
                typeList."${span_key}".time_list = new TreeMap([:]);
                typeList."${span_key}".title = "${span.time_units.capitalize()}s to Display";
                for (i=0; i<span.num_time; i++){
                    if (i==0) {
                        if (span.time_units == "day")
                            typeList."${span_key}".time_list << ["00" : [name: " Today"]];
                    } else if (span.time_units == "day" && i==1)
                        typeList."${span_key}".time_list << ["01" : [name: " Tomorrow"]];       
                    else if (i==1) 
                        typeList."${span_key}".time_list << ["01" : [name: " $i ${span.time_units} from now"]];
                    else if (i<10) 
                        typeList."${span_key}".time_list << ["0$i" : [name: " $i ${span.time_units}s from now"]];
                    else 
                        typeList."${span_key}".time_list << ["$i" : [name: " $i ${span.time_units}s from now"]];
                }
            } 
        }
        //atomicState.newTileDialog = "";
        atomicState.newTileDialog = typeList.sort();
    
    

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
                    parent.hubiForm_container(this, container, 0); 
                }
                
                parent.hubiForm_section(this, "Local Tile URL", 1, "link"){
                    container = [];
                    container << parent.hubiForm_text(this, "${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
                    
                    parent.hubiForm_container(this, container, 1); 
                }                  
                
                if (openweather_refresh_rate){
                     parent.hubiForm_section(this, "Configure Tile", 10, "settings"){                         
                         container = [];
                         container << getPreviewWindow("tile_settings_HTML", "mainPage");
                         parent.hubiForm_container(this, container, 0); 
                         
                     } 
            
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

def verifyDeviceCallback(response, data) {
}

def getPreviewWindow(var, page){
    
    def params = [
        uri: "${state.localEndpointURL}",
		path: "graph/?access_token=${state.endpointSecret}",
        requestContentType: "application/json",
    ]
    
    asynchttpGet(verifyDeviceCallback, params); 
    
    if (!settings["$var"]){
       app.updateSetting("${var}", [value: "", type: "string"]);
    } 

   html = """
                <style> 
                   .iframe-container {
                        overflow: hidden;
                        width: 55vmin;
                        height: 65vmin;
                        position: relative;
                   }
                   .iframe-container iframe {
                        border: 0;
                        left: 0;
                        position: absolute;
                        top: 0;
                   }
                </style>
                 
                """
                //<input type="text" id="settings${var}" name="settings[${var}]" value="${settings[var]}" style="display: none;" >
                //<div class="form-group" style="display:none;">
                //   <input type="hidden" name="${var}.type" value="text">
                //   <input type="hidden" name="${var}.multiple" value="false">
                //</div>
                //<div>
                html+="""
                <div class="iframe-container">
                        <iframe id="preview_frame" style="width: 100%; height: 100%; position: relative; z-index: 1; background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAEq2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIKICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIKICAgIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIgogICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgZXhpZjpQaXhlbFhEaW1lbnNpb249IjIiCiAgIGV4aWY6UGl4ZWxZRGltZW5zaW9uPSIyIgogICBleGlmOkNvbG9yU3BhY2U9IjEiCiAgIHRpZmY6SW1hZ2VXaWR0aD0iMiIKICAgdGlmZjpJbWFnZUxlbmd0aD0iMiIKICAgdGlmZjpSZXNvbHV0aW9uVW5pdD0iMiIKICAgdGlmZjpYUmVzb2x1dGlvbj0iNzIuMCIKICAgdGlmZjpZUmVzb2x1dGlvbj0iNzIuMCIKICAgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIKICAgcGhvdG9zaG9wOklDQ1Byb2ZpbGU9InNSR0IgSUVDNjE5NjYtMi4xIgogICB4bXA6TW9kaWZ5RGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIKICAgeG1wOk1ldGFkYXRhRGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCI+CiAgIDx4bXBNTTpIaXN0b3J5PgogICAgPHJkZjpTZXE+CiAgICAgPHJkZjpsaQogICAgICBzdEV2dDphY3Rpb249InByb2R1Y2VkIgogICAgICBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZmZpbml0eSBQaG90byAxLjguMyIKICAgICAgc3RFdnQ6d2hlbj0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIvPgogICAgPC9yZGY6U2VxPgogICA8L3htcE1NOkhpc3Rvcnk+CiAgPC9yZGY6RGVzY3JpcHRpb24+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8+IC4TuwAAAYRpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZE7SwNBFEaPiRrxQQQFLSyCRiuVGEG0sUjwBWqRRPDVbDYvIYnLboIEW8E2oCDa+Cr0F2grWAuCoghiZWGtaKOy3k2EBIkzzL2Hb+ZeZr4BWyippoxqD6TSGT0w4XPNLyy6HM/UYqONfroU1dBmguMh/h0fd1RZ+abP6vX/uYqjIRI1VKiqEx5VNT0jPCk8vZbRLN4WblUTSkT4VLhXlwsK31p6uMgvFseL/GWxHgr4wdYs7IqXcbiM1YSeEpaX404ls+rvfayXNEbTc0HJnbI6MAgwgQ8XU4zhZ4gBRiQO0YdXHBoQ7yrXewr1s6xKrSpRI4fOCnESZOgVNSvdo5JjokdlJslZ/v/11YgNeovdG31Q82Sab93g2ILvvGl+Hprm9xHYH+EiXapfPYDhd9HzJc29D84NOLssaeEdON+E9gdN0ZWCZJdli8Xg9QSaFqDlGuqXip797nN8D6F1+aor2N2DHjnvXP4Bhcln9Ef7rWMAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAXSURBVAiZY7hw4cL///8Z////f/HiRQBMEQrfQiLDpgAAAABJRU5ErkJggg=='); background-size: 25px; background-repeat: repeat; image-rendering: pixelated;" src="${state.localEndpointURL}graph/?access_token=${state.endpointSecret}" data-fullscreen="false" 
                            onload="(() => {
                         })()""></iframe>
                </div>
                
                </div>
                """
    return (html.replace('\t', '').replace('\n', '').replace('  ', ''));
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

def processCallBack(response, data) {
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
        case "none": return "";
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
    return "";
}

private getUnits(unit, value){
    if (unit == null)  return [name: "unknown", var: "tbd", units: "none"];
    
    try{
        switch (unit.toLowerCase()){
            case "f":
            case "°f":
                return [name: "Farenheit (°F)", var: "temperature", units: "fahrenheit"]; break;
            case "c":
            case "°c":
                return [name: "Celsius (°C)", var: "temperature", units: "celcius"]; break;
            case "mph": 
                return [name: "Miles per Hour (mph)", var: "velocity", units: "miles_per_hour"]; break;
            case "m/s":
                return [name: "Meters per Second (m/s)", var: "velocity", units: "meters_per_second"]; break;
            case "in":
            case '"':
                return [name: 'Inches (")', var: "depth", units: "inches"]; break;
            case "mm":
            case '"':
                return [name: 'Millimeters (mm)', var: "depth", units: "millimeters"]; break;
            case "°":
            case "deg":
                return [name: "Degrees (°)", var: "direction", units: "degrees"]; break;
            case "rad":
                return [name: "Radians (°)", var: "direction", units: "radians"]; break;
            case "inhg":
                return [name: "Inches of Mercury (inHg)", var: "pressure", units: "inches_mercury"]; break;
            case "mmhg":
                return [name: "Millimeters of Mecury mmHg)", var: "pressure", units: "millimeters_mercury"]; break;
            case "mbar":
                return [name: "Millibars (mbar)", var: "pressure", units: "millibars"]; break;
            case "km/h":
                return [name: "Kilometers per hour (km/h)", var: "velocity", units: "kilometers_per_hour"]; break;
            case "hPa":
                return [name: "Hectopascal (hPa)", var:"pressure", units: "hectopascal"]; break;
            case "%":
                value = Double.parseDouble(value);
                if (value > 1.0 && value < 100.0) {
                    return [name: "Percent (0 to 100)", var:"percent", units: "percent_numeric"];    
                } else if (value >=0.0 && value < 1.0) {
                    return [name: "Percent (0.1 to 1.0)", var: "percent", units: "percent_decimal"];  
                }
            break;
            default: 
                return [name: "unknown", var: "tbd", units: "none"];  break;  
        } 
    } catch (error) {
            log.debug("Unable to find units: "+error);    
    }
    
}

def getIconList(){
    
    return [
        [name: "None",                  icon: "alpha-x-circle-outline"],
        [name: "Cloudy",                icon: "weather-cloudy"],
        [name: "Cloudy Alert",          icon: "weather-cloudy-alert"],
        [name: "Cloudy Right Arrow",    icon: "weather-cloudy-arrow-right"],
        [name: "Fog",                   icon: "weather-fog"],
        [name: "Hail",                  icon: "weather-hail"],
        [name: "Hazy",                  icon: "weather-hazy"],
        [name: "Hurricane",             icon: "weather-hurricane"],
        [name: "Lightning",             icon: "weather-lightning"],
        [name: "Lightning Raining",     icon: "weather-lightning-rainy"],
        [name: "Night",                 icon: "weather-night"],
        [name: "Night Partly Cloudy",   icon: "weather-night-partly-cloudy"],
        [name: "Partly Cloudy",         icon: "weather-partly-cloudy"],
        [name: "Partly Lightning",      icon: "weather-partly-lightning"],
        [name: "Partly Raining",        icon: "weather-partly-rainy"],
        [name: "Partly Snowing",        icon: "weather-partly-snowy"],
        [name: "Partly Snowing Raining",icon: "weather-partly-snowy-rainy"],
        [name: "Pouring",               icon: "weather-pouring"],
        [name: "Raining",               icon: "weather-rainy"],
        [name: "Snowing",               icon: "weather-snowy"],
        [name: "Heavy Snow",            icon: "weather-snowy-heavy"],
        [name: "Snowing Raining",       icon: "weather-snowy-rainy"],
        [name: "Sunny",                 icon: "weather-sunny"],
        [name: "Sunny Alert",           icon: "weather-sunny-alert"],
        [name: "Sunny Off",             icon: "weather-sunny-off"],
        [name: "Sunset",                icon: "weather-sunset"],
        [name: "Sunset Down",           icon: "weather-sunset-down"],
        [name: "Sunset Up",             icon: "weather-sunset-up"],
        [name: "Tornado",               icon: "weather-tornado"],
        [name: "Windy",                 icon: "weather-windy"],
        [name: "Windy 2",               icon: "weather-windy-variant"],
        [name: "Home Thermometer",      icon: "home-thermometer-outline"],
        [name: "Arrow Up",              icon: "arrow-up-thick"],
        [name: "Arrow Down",            icon: "arrow-down-thick"],
        [name: "Umbrella",              icon: "umbrella-outline"],
        [name: "Ruler",                 icon: "ruler"],
        [name: "Cloud Question",        icon: "cloud-question"],
        [name: "Calendar",              icon: "calendar-today"],
        [name: "Tail Wind",             icon: "tailwind"],
        [name: "Compass",               icon: "compass-outline"],
        [name: "Gauge",                 icon: "gauge"],
        [name: "Thermostat",            icon: "thermostat"],
        [name: "Water Percent",         icon: "water-percent"],
        [name: "Wave",                  icon: "wave"]];
}

def getWeatherData(){  
    
    def options = [
        "tile_units": atomicState.unit_type,
        "openweather_refresh_rate": openweather_refresh_rate,
        "tiles" :     atomicState.tile_settings,
        "tile_type" : atomicState.tile_type,
        "new_tile_dialog" : atomicState.newTileDialog,
        "api_code" :  "${state.endpointSecret}",
        "url" :       "${state.localEndpointURL}",      
        ];
    
    options.out_units = [:];
    
    atomicState.unit_type.each {key, measurement->
        options.out_units << [ "${key}" : settings["${key}_units"]];
    }
    
     
    return options;
}
def getMapData(map, loc){
    splt = loc.tokenize('.');
    cur = map;
    splt.each{str->
        try{
            if (str.isNumber()){
                num = str.toInteger();
                cur = cur[num];
            } else {
                cur = cur[str];
            }
        } catch (e){
             log.debug(loc+": Cannot find data: "+e); 
             return -1;
        }
    }
    return cur;
}

def applyDecimals(tile, val){

    value = val.toString();
    if (value.isNumber()){
        num_decimals = tile.decimals
        value = sprintf("%.${num_decimals}f", value.toFloat());
        return value;
    }
    else return val;
}

def getWindDirection(direction) {
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

def applyConversion(tile, val){
    
    try {
        tile_type = atomicState.tile_type."${tile.type}";
        out_units = atomicState.unit_type."${tile_type.type}".out;
        in_units = tile_type.in_units;
    } catch (e){
        log.debug("Unable to find units for ${tile.title}:: Input units = "+in_units+"  Output units = "+out_units);
         log.debug(e);
    }
      
    if (in_units != out_units && out_units != "none")
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
                    case "kelvin":  val = ((val - 32) * (5 / 9)) + 273.15; break;
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
            
            //Precipitation
            case "millimeters":
                if (out_units == "inches") {
                    val = (val / 25.4);
                } else val = "UNSUPPORTED";
                break;
            case "inches":
                if (out_units == "millimeters") {
                    val = (val * 25.4);
                } else val = "UNSUPPORTED";
                break;
        
            //Velocity
            case "meters_per_second":
                switch (out_units) {
                    case "miles_per_hour": val = (val * 2.237); break;
                    case "knots": val = (val * 1.944); break;
                    case "kilometers_per_hour": val = (val * 3.6); break;
                    default: val = "UNSUPPORTED";
                } 
            break;
            case "miles_per_hour":
                switch (out_units) {
                    case "miles_per_hour": val = (val / 2.237); break;
                    case "knots": val = (val / 1.151); break; 
                    case "kilometers_per_hour": val = (val * 1.609); break;
                    default: val = "UNSUPPORTED";
                } 
            break;
            case "knots":
                switch (out_units) {
                    case "miles_per_hour": val = (val * 1.151); break;
                    case "meters_per_second": val = (val / 1.944); break;
                    case "kilometers_per_hour": val = (val * 1.852); break;
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
        
            //Pressure
            case "hectopascal":
            case "millibars":
                switch (out_units) {
                    case "inches_mercury": val = (val / 33.864); break;
                    case "millimeters_mercury": val = (val / 1.333); break;
                    case "hectopascal": break;
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
                    case "cardinal": val = getWindDirection(val); break;
                    case "radians": val = (val * 3.1415926535) * 180.0; break;
                    default: val = "UNSUPPORTED";
                } 
            break;
            case "radians":
                switch (out_units) {
                    case "cardinal": val = getWindDirection(((val * 3.1415926535) * 180.0)); break;
                    case "degrees": val = ((val * 180) / 3.1415926535); break;
                    default: val = "UNSUPPORTED";
                }  
            break;
            case "cardinal":
                switch (data.value) {
                    case "N": val = 0; break;
                    case "NNE": val = 22.5; break;
                    case "NE": val = 45; break;
                    case "ENE": val = 67.5; break;
                    case "E": val = 90; break;
                    case "ESE": val = 112.5; break;
                    case "SE": val = 135; break;
                    case "SSE": val = 157.5; break;
                    case "S": val = 180; break;
                    case "SSW": val = 202.5; break;
                    case "SW": val = 225; break;
                    case "WSW": val = 247.5; break;
                    case "W":val = 270; break;
                    case "WNW":  val = 292.5; break;
                    case "NW":  val = 315; break;
                    case "NNW":  val = 337.5; break;
                    default:   val = -1;
                }
                if (val != -1) {
                    switch (out_units) {
                        case "radians": val = ((val * 3.1415926535) * 180.0); break;
                        case "degrees": val = val; break;
                        default: val = "UNSUPPORTED";
                    }
                } else val = "UNSUPPORTED";
                break;
\
            //TEXT CONVERSIONS
            case "time_seconds":
                 v = val*1000L;
                 d = new Date(v);

                switch (out_units) {
                    case "time_twelve":
                        val = d.getTimeString();
                        break;
                    case "time_two_four":
                        SimpleDateFormat simpDate;
                        simpDate = new SimpleDateFormat("kk:mm:ss");
                        val = simpDate.format(d);
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "time_milliseconds":
                 d = new Date(val);

                switch (out_units) {
                    case "time_twelve":
                        val = d.getTimeString();
                        break;
                    case "time_two_four":
                        val = d.getTimeString();
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
           case "percent_numeric":
                if (out_units == "percent_decimal") val = val / 100.0;
                else val = "UNSUPPORTED";
                break;
            case "percent_decimal":
                if (out_units == "percent_numeric") val = val * 100.0;
                else val = "UNSUPPORTED";
                break;
        }
    
        return val;

    }
    
   
    



def translateCondition(tile, condition) {

    icon = "mdi-weather-sunny-off";
    
    def now = new Date().getTime() / 1000;
    
    pairings = [
        [name: "thunderstorm with light rain",      icon: "weather-lightning-rainy"],
        [name: "thunderstorm with rain",            icon: "weather-lightning-rainy"],     
        [name: "thunderstorm with heavy rain",      icon: "weather-lightning-rainy"], 
        [name: "light thunderstorm",                icon: "weather-lightning"],
        [name: "thunderstorm",                      icon: "weather-lightning"],
        [name: "heavy thunderstorm",                icon: "weather-lightning"],
        [name: "ragged thunderstorm",               icon: "weather-lightning"],
        [name: "thunderstorm with light drizzle",   icon: "weather-lightning-rainy"],
        [name: "thunderstorm with drizzle",         icon: "weather-lightning-rainy"],
        [name: "thunderstorm with heavy drizzle",   icon: "weather-lightning-rainy"],
        [name: "light intensity drizzle",           icon: "weather-partly-rainy"],
        [name: "drizzle",                           icon: "weather-partly-rainy"],
        [name: "heavy intensity drizzle",           icon: "weather-partly-rainy"],
        [name: "light intensity drizzle rain",      icon: "weather-partly-rainy"],
        [name: "drizzle rain",                      icon: "weather-partly-rainy"],
        [name: "heavy intensity drizzle rain",      icon: "weather-rainy"],
        [name: "shower rain and drizzle",           icon: "weather-rainy"],
        [name: "heavy shower rain and drizzle",     icon: "weather-pouring"],
        [name: "shower drizzle",                    icon: "weather-rainy"],
        [name: "light rain",                        icon: "weather-rainy"],
        [name: "moderate rain",                     icon: "weather-pouring"],
        [name: "heavy intensity rain",              icon: "weather-pouring"],
        [name: "very heavy rain",                   icon: "weather-pouring"],
        [name: "extreme rain",                      icon: "weather-pouring"],
        [name: "freezing rain",                     icon: "weather-snowy-rainy"],
        [name: "light intensity shower rain",       icon: "weather-rainy"],
        [name: "shower rain",                       icon: "weather-rainy"],
        [name: "heavy intensity shower rain",       icon: "weather-pouring"],
        [name: "ragged shower rain",                icon: "weather-partly-rainy"],
        [name: "light snow",                        icon: "weather-snowy"],
        [name: "snow",                              icon: "weather-snowy"],
        [name: "heavy snow",                        icon: "weather-snowy-heavy"],
        [name: "sleet",                             icon: "weather-hail"],
        [name: "light shower sleet",                icon: "weather-hail"],
        [name: "shower sleet",                      icon: "weather-hail"],
        [name: "light rain and snow",               icon: "weather-snowy-rainy"],
        [name: "rain and snow",                     icon: "weather-snowy-rainy"],
        [name: "light shower snow",                 icon: "weather-partly-snowy"],
        [name: "shower snow",                       icon: "weather-partly-snowy"],
        [name: "heavy shower snow",                 icon: "weather-partly-snowy"],
        [name: "mist",                              icon: "weather-fog"],
        [name: "smoke",                             icon: "weather-fog"],
        [name: "haze",                              icon: "weather-hazy"],
        [name: "sand dust whirls",                  icon: "weather-tornado"],
        [name: "fog",                               icon: "weather-fog"],
        [name: "sand",                              icon: "weather-fog"],
        [name: "dust",                              icon: "weather-fog"],
        [name: "volcanic ash",                      icon: "weather-fog"],
        [name: "squalls",                           icon: "weather-tornado"],
        [name: "tornado",                           icon: "weather-tornado"],
        [name: "clear sky night",                   icon: "weather-night"],
        [name: "clear sky",                         icon: "weather-sunny"],
        [name: "few clouds night",                  icon: "weather-night-partly-cloudy"],
        [name: "few clouds",                        icon: "weather-partly-cloudy"],
        [name: "scattered clouds night",            icon: "weather-night-partly-cloudy"],
        [name: "scattered clouds",                  icon: "weather-partly-cloudy"],
        [name: "broken clouds",                     icon: "weather-cloudy"],
        [name: "overcast clouds",                   icon: "weather-cloudy"]
    ];
   
    try {
        return_val = ["icon", pairings.find{el->  el.name == condition.toLowerCase()}.icon]; 
    } catch (e){
        log.debug (tile.name+": Unable to return "+condition+": "+e);
        return_val = ["icon", "alert-circle"];  
    }
    return return_val;
}

def formatNumericData(tile, val){
    if (val == null)
        val = 0;
    return ["value",  applyDecimals(tile, applyConversion(tile, val))];   
}

def formatHourData(tile, val){
     def location = getLocation();
    
     val_micro = val*1000L;
     Date date = new Date (val_micro);
    
    switch (settings["time_units"]){
        case "time_seconds" :        return ["value",  val];
        case "time_milliseconds" :   return ["value", val_micro];
        case "time_twelve" :         return ["value",  date.format('h:mm a', location.timeZone)];
        case "time_two_four" :       return ["value",  date.format('HH:mm', location.timeZone)];
    }
    return ["value",  "XXXX"]; 
}

def formatDayData(tile, val){
    def location = getLocation();
    val_micro = val*1000L;
    Date date = new Date (val_micro);
          
    if (settings["day_units"] == "short") day = date.format('E', location.timeZone);
    else day = date.format('EEEE', location.timeZone);
           
   
     
    return ["value",  day]; 
}

def formatTextData(tile, val){
    
    switch (settings["text_units"]){
        case "plain":      return ["value", val];
        case "lowercase":  return ["value", val.toLowerCase()];
        case "uppercase":  return ["value", val.toUpperCase()];
        case "title":      return ["value", val.split(" ").collect{it.capitalize()}.join(" ")];
    }
}



def formatConditionText(tile, val){
    return ["value", val.split(" ").collect{it.capitalize()}.join(" ")];
}

def formatTitle(tile, val){
    return["value", ""];   
}

def formatPressure(tile, val){
    return ["value", "Pressure Trend"];    
}

def formatDewPoint(tile, val) {
    def text = "";
    
    if (dewPoint < 50) text = "DRY"; 
    else if (dewPoint < 55) text =  "NORMAL"; 
    else if (dewPoint < 60) text =  "OPTIMAL"; 
    else if (dewPoint < 65) text =  "STICKY"; 
    else if (dewPoint < 70) text =  "MOIST"; 
    else if (dewPoint < 75) text =  "WET"; 
    else text "MISERABLE";
    
    return ["value", text];

}

def getSensorData(measurement){
    log.debug(measurement);
    device_id = measurement.tokenize('.')[1];
    attribute = measurement.tokenize('.')[2];
    
    sensor = sensors.find { it.id == device_id };
    return sensor.currentState(attribute).getValue();
}

def buildWeatherData(){
    
    def val; 
    def selections = settings["tile_settings"];                    
        
    data = parent.getOpenWeatherData();
    data = parseJson(data);
    
    temp = atomicState.tile_settings;
    temp.eachWithIndex{tile, index-> 
       try {
           period = tile.period;
           measurement = atomicState.tile_type."${tile.type}".ow;
           if (period == "sensor"){
               val = getSensorData(measurement);        
           } else if (period != "none" && measurement != "none") {
               val = getMapData(data, period+"."+measurement);
           }
       } catch (e){
             log.debug(tile.name+": Unable to get data: "+period+", "+measurement);    
       } 
        unit_type = atomicState.tile_type."${tile.type}".type;
        parse_func = atomicState.unit_type."${unit_type}".parse_func;
     
        try{
            if (parse_func!="none"){
                returnVal = "${parse_func}"(tile, val);
                tile."${returnVal[0]}" = returnVal[1];
            } else {
                tile.value = "";
            }
        } catch (error){
             log.debug(tile.name+": Unable to find parse function: $parse_func " + e);   
        }
    }
    atomicState.tile_settings = temp;
}

def getTileHTML(item){
    var = item.var;
    
    fontScale = 4.6;
    lineScale = 0.85;
    iconScale = 3.5;
    header = 0.1;
    
    
    height = item.h;
    html = "";    
    if (item.display==true){
            html += """ <div id="${var}_tile_main" class="grid-stack-item" data-gs-id = "${var}" data-gs-x="${item.baseline_column}" 
                                                  data-gs-y="${item.baseline_row}" data-gs-width="${item.w}" data-gs-height="${height}" data-gs-locked="false"
                                                  ondblclick="setOptions('${var}')">

                    <div id="${var}_title" style="display: none;">${item.title}</div>
                    <div id="${var}_font_adjustment" style="display: none;">${item.font_adjustment}</div>
                    <div class="mdl-tooltip" for="${var}_tile_main" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">${item.title}</div>


                    <div id="${var}_tile" class="grid-stack-item-content" style="font-size: ${fontScale*height}vh; 
                                                                          line-height: ${fontScale*lineScale*height}vh;
                                                                          text-align: ${item.justification};
                                                                          background-color: ${item.background_color};
                                                                          font-weight: ${item.font_weight};"> """;
        
        //Compute Icon and other spacing
        
        //Left Icon
        if (item.icon_loc != "right"){
            item.icon_space = item.icon_space ? item.icon_space : "";
            html+="""<span id="${var}_icon" class="mdi mdi-${item.icon}" style="font-size: ${iconScale*height}vh; color: ${item.font_color};">${item.icon_space}</span>""";
        }
        //Text
        if (item.text == "null" || item.text == null) item.text=""; 
        html+="""<span id="${var}_text" style="color: ${item.font_color};">${item.text}</span>""";
        
        //Main Content
        html += """<span id="${var}" style="color: ${item.font_color};">${item.value}</span>"""
        
        //Units
        try{
            tile_type = atomicState.tile_type."${item.type}".type;
            out_units = atomicState.unit_type."${tile_type}".out;
            units = getAbbrev(out_units);
        } catch (e){
            units = "";
        }
        
        if (units == "unknown") units="";
        
        //Unit Spacing
        html += """<span id="${var}_unit_space">${item.unit_space}</span>"""
                    
        html += """<span id="${var}_units" style="font-size: ${iconScale*height}vh; color: ${item.font_color};">${units}</span>""";  
        
        //Right Icon
        if (item.icon_loc == "right"){
            html+="""<span>${item.icon_space}</span>""";
            html+="""<span id="${var}_icon" class="mdi mdi-${item.icon}" style="color: ${item.font_color};"></span>""";
        }
        html += """</div></div>""";
    } //if display    
    
    return html;
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
    <link rel="stylesheet" href="//cdn.materialdesignicons.com/5.4.55/css/materialdesignicons.min.css">
    <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons">
    <link rel="stylesheet" href="https://code.getmdl.io/1.3.0/material.indigo-pink.min.css">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css">
    <link rel="stylesheet" href="/local/gridstack.css">
    

    <script>
        const localURL =         "${state.localEndpointURL}";
        const secretEndpoint =   "${state.endpointSecret}";
        const latitude =         "${latitude}";
        const longitude =        "${longitude}";
        const tile_key =         "${tile_key}";
    </script>

    <script defer src="https://code.getmdl.io/1.3.0/material.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.5.1.min.js" integrity="sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=" crossorigin="anonymous"></script>
    <script src="https://code.jquery.com/ui/1.12.1/jquery-ui.min.js" integrity="sha256-VazP97ZCwtekAsvgPBSUwPFKdrwD3unUfSGVYrahUqU=" crossorigin="anonymous"></script>
    <script type="text/javascript" src="https://unpkg.com/@fonticonpicker/fonticonpicker/dist/js/jquery.fonticonpicker.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/gridstack@1.1.2/dist/gridstack.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/gridstack@1.1.2/dist/gridstack.jQueryUI.js"></script>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jqueryui-touch-punch/0.2.3/jquery.ui.touch-punch.min.js" integrity="sha512-0bEtK0USNd96MnO4XhH8jhv3nyRF0eK87pJke6pkYf3cM0uDIhNJy9ltuzqgypoIFXw3JSuiy04tVk4AjpZdZw==" crossorigin="anonymous"></script>
    <script defer src="http://192.168.1.64:8080/a7af9806-4b0e-4032-a78e-a41e27e4d685-WeatherTile.js"></script>
    <script src="/local/iconPicker.js"></script>

    """
    return html;
}

def addColorPicker(Map map){
    var = map.var;
    title = map.title;
       
    html = """
    <div class = "border-container">

      <div id="text_box" class="flex-container">
          <div class="flex-item" style="flex-basis: 25%;">
            <span><label for="${var}_color">${title}</label></span>
          </div>
          <div class="flex-item" style="flex-basis: 75%;">
            <span><label for="${var}_color">Opacity</label></span>
          </div>
       </div>
    
      <div id="text_color_box" class="flex-container">
          <div class="flex-item" style="flex-basis: 25%;">
             <span><input type="color" id="${var}_color" name="${var}_color" value="#FFFFFF"></span>
          </div>
          <div class="flex-item" style="flex-basis: 60%;">
             <input id="${var}_slider" class="mdl-slider mdl-js-slider" type="range" min="0" max="100" value="100" tabindex="0"
              oninput="${var}_showMessage(this.value)" onchange="${var}_showMessage(this.value)">
         </div>
         <div class="flex-item" style="flex-basis: 15%;">
            <div class="item" id="${var}_message">100%</div>
        </div>
      </div>
    </div>
      <!-- JAVASCRIPT -->
      <script language="javascript">
        function ${var}_showMessage(value) {
          document.getElementById("${var}_message").innerHTML = value + "%";
        }	   
      </script>
"""
    return html;
}

def addButtonMenu(Map map){
    button_var = map.var_name;
    default_val = map.default_value;
    default_icon = map.default_icon;
    item_list = map.list;
    tooltip = map.tooltip ? map.tooltip : "";
    side = map.side ? map.side : "left";

    
    def html = """
        <div id = "${button_var}_value" style="display: none;">${default_val}</div>
        <div id = "${button_var}_icon" style="display: none;">${default_icon}</div>
        <button id="${button_var}_button"
            class="mdl-button mdl-js-button mdl-button--icon mdi mdi-${default_icon}">
        </button>
        
        <div class = "mdl-tooltip" for = "${button_var}_button">${tooltip}</div>
            <ul class="mdl-menu mdl-js-menu mdl-js-ripple-effect mdl-menu--bottom-${side}" for="${button_var}_button"> """
   
                item_list.each{item->
                    weight = item.font_weight ? item.font_weight : 400;
                    html += """ <li class="mdl-menu__item" onclick="${button_var}_itemSelected('${item.icon}',  '${item.name.toLowerCase()}')">
                                    <div id = "${item.name.toLowerCase()}_icon" style="display: none;">${item.icon}</div>
                                    <span id="${item.name.toLowerCase()}" class=" mdi mdi-${item.icon}" style="vertical-align: middle; font-weight: ${weight};"></span>
                                    <span>  ${item.text ? item.text : item.name}</span>
                                </li>"""
    }
    
    html += """</ul>
    """
    html += """
    <script>
            function ${button_var}_itemSelected(icon, val){
               
                replaceIcons("${button_var}_button", icon);
                document.getElementById("${button_var}_value").textContent = val;
                document.getElementById("${button_var}_icon").textContent = icon;

            } 
     </script>
    """
    return html;
}



def addMenu(Map map){
    
     button_var = map.var_name;
     default_val = map.default_value;
     default_icon = map.default_icon;
     item_list = map.list;
     tooltip = map.tooltip ? map.tooltip : "";
     title = map.title;
    
    def html = """
        <div>
        <div id = "${button_var}_value" style="display: none;">${default_val}</div>
        <div id = "${button_var}_icon" style="display: none;">mdi-${default_icon}</div>
        <span>
            <button id="${button_var}_button" class="mdl-button mdl-js-button mdl-js-ripple-effect" tabindex="-1">
                <i id="${button_var}_icon_display" class="mdi mdi-${default_icon}">
                    <label id="${button_var}_text_display"> ${title}</label>
                </i>
                
            </button>
            <div class = "mdl-tooltip" for = "${button_var}_button">${tooltip}</div>
        </span>
        
        
        <ul class="mdl-menu mdl-js-menu mdl-js-ripple-effect" for="${button_var}_button"  style="overflow-y: scroll; max-height: 50vh; line-height: 10px;"> """
            item_list.each{item->
                weight = item.font_weight ? item.font_weight : 400;
                html += """ <li id = "${item.var}_list_main" class="mdl-menu__item" onclick="${button_var}_itemSelected('${item.icon}',  '${item.var}')">
                            <div id = "${item.var}_list_item" style="display: none;">${item.icon}</div>
                            <span id="${item.var}_list_title" class=" mdi mdi-${item.icon}" style="vertical-align: middle; font-weight: ${weight};"></span>
                            <span id="${item.var}_list_name">${item.text ? item.text : item.name}</span>
                    </li>"""
    }
    
    html += """</ul></div>"""
    
    html += """
    <script>
            function ${button_var}_itemSelected(icon, val){
                let currentIcon = document.getElementById("${button_var}_icon").textContent;
                let iconDisplay = jQuery("#${button_var}_icon_display");
                console.log(iconDisplay.hasClass("mdi"));
                iconDisplay.removeClass(currentIcon);
                iconDisplay.addClass(icon);
                document.getElementById("${button_var}_text_display").textContent = document.getElementById(val+"_list_name").textContent;
                document.getElementById("${button_var}_value").textContent = val;
                document.getElementById("${button_var}_icon").textContent = icon;

            } 
        </script>
    """
    return html;    
}

def addIconMenu(Map map){
 
    button_var = map.var_name;
    default_val = map.default_value;
    default_icon = map.default_icon;
    item_list = map.list;
    tooltip = map.tooltip ? map.tooltip : "";
    description = map.description ? map.description : false;
    width = map.width;
    if (map.tooltip == "Use Icon Name") tooltip = "No Icon Selected";
    

    def html = """
        <div>
        <div id = "${button_var}_menu" class="flex-item" style="flex-grow:1;" tabindex="-1; ">
        <div id = "${button_var}_value" style="display: none;">${default_val}</div>
        <div id = "${button_var}_icon" style="display: none;">${default_icon}</div>
        <div>
        <button id="${button_var}_button"
            class="mdl-button mdl-js-button mdl-button--icon mdi mdi-${default_icon}">
        </button>
        """
    if (description) 
        html += """ <span> <b>Icon</b> </span><span id= "${button_var}_text">(None)</span>"""
    html += """ </div>
        
    <div id = "${button_var}_tooltip" class = "mdl-tooltip" for = "${button_var}_menu">${tooltip}</div>
    <ul class="mdl-menu mdl-js-menu mdl-js-ripple-effect" for="${button_var}_menu" style = "max-height: 40vh; overflow-y: scroll !important;"> """
    
    count = 0;
    item_list.each{item->
        if (count % width == 0) {
            html+="""<div class="flex-container">""";
        }
        weight = item.font_weight ? item.font_weight : 400;
        icon_var = item.icon.replaceAll("-","_");
        html += """ <div class="flex-item" style="flex-grow:1;">
                    <li class="mdl-menu__item" onclick="${button_var}_itemSelected('${item.icon}',  '${item.name.toLowerCase()}', '${item.name}')">
                        <div id = "${button_var}_${icon_var}_icon" style="display: none;">${item.icon}</div>
                        <span id="${button_var}_${icon_var}" class=" mdi mdi-${item.icon}" style="vertical-align: middle; font-size: 5vw;"></span>
                        <div id="${button_var}_${icon_var}_text" class = "mdl-tooltip" for = "${button_var}_${icon_var}">${item.name}</div>
                    </li>
                    </div>"""
        if (count % width == width-1) {
             html+= """</div>"""  
        }
        count++;
    }
    
           html += """</ul>
    </div>
    </div>
    """
    html += """
    <script>
            function ${button_var}_itemSelected(icon, val, name){           
                replaceIcons("${button_var}_button", icon);
                document.getElementById("${button_var}_value").textContent = val;
                document.getElementById("${button_var}_icon").textContent = icon;
    """
    if (description) 
        html += """ document.getElementById("${button_var}_text").textContent = "("+name+")";"""
    if (map.tooltip == "Use Icon Name")
        html += """ document.getElementById("${button_var}_tooltip").textContent = "Selected Icon: "+name;"""
    
    html += """
            } 
        </script>
   
    """
    return html;
}


def addSlider(Map map){
 
    var = map.var;
    title = map.title;
    min = map.min;
    max = map.max;
    value = map.value;
    
    html = """
    <div id="${var}_box" class="flex-container">
          <div class="flex-item" style="flex-basis: 35%;">
            <label for="${var}_slider">${title}</label>
          </div>
          <div class="flex-item" style="flex-grow: auto;">
            <input id="${var}_slider" class="mdl-slider mdl-js-slider" type="range" min="${min}" max="100" value="0"
                  tabindex="0" oninput="${var}_showMessage(this.value)" onchange="${var}_showMessage(this.value)">
          </div>
          <div class="flex-item" style="flex-basis: 15%;">
            <div id="${var}_message">0%</div>
          </div>
    </div>

    <script language="javascript">
        function ${var}_showMessage(value) {
            document.getElementById("${var}_message").innerHTML = value + "%";
        }	   
    </script>
    """
    return html;
}

def defineHTML_CSS(){
   
def html = """

<style>
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

.flex-container {
  display: flex;
  flex-wrap: nowrap;
  width: 100%;
  background-color: rgba(0,0,0,0);
}

.flex-container > div {
  background-color: rgba(0,0,0,0);
  width: auto;
  margin: 2px;
  text-align: center;
  line-height: 3vh;
  font-size: 3vh;
}

.border-container {
      border-style: solid none none none; 
      padding-bottom: 1vh;
      padding-top: 1vh;  
      width: 100%;
}

.mdl-textfield__label{  
   margin-bottom:0px !important;
   margin-top:0px !important;    
}


</style>

    """
    return html;
}

def defineSelectBox(Map map){
 
    title = map.title;
    var = map.var;
    list = map.list;
    visible = map.visible == false ? """style="display: none;" """ : "" ; 
    function = map.function;
    
    html = """
    
     <div id=${var}_main class="mdl-textfield mdl-js-textfield mdl-textfield--floating-label" ${visible}>
            <select class="mdl-textfield__input" id="${var}" name="${var}" style="line-height: 5vh !important" onchange="${function}(this.value)">
            <option value="blank"></option>
    """
    list.each{key, item->
        html+="""<option value="${key}">${item.name}</option>"""
    }
    html+= 
    """
            </select>
            <label class="mdl-textfield__label"  for="${var}">${title}</label>
            </div>
    """
    
    return html;
}

def defineNewTileDialog(){
    
    typeList = atomicState.newTileDialog;
    
    html = "";
    html += """<dialog id="addTileDialog" class="mdl-dialog mdl-shadow--12dp" tabindex="-1" style = "background-color: rgba(255, 255, 255, 0.90); border-radius: 2vh; height: 95vh">
                   <div class="mdl-dialog__content">
                      <div class="mdl-layout">
                          <div id="options_title" class="mdl-layout__title" style = "color: black; text-align: center;">
                            New Tile Options
                          </div>

                         <div class="mdl-grid" style="width: 100%">
                             <div class = "border-container">
                                 <div id="menu_items" class="flex-container">
                                     <div class="flex-item" style="max-width:18%; flex-basis: 18%" tabindex="-1">
                                         <button id="save_button" type="button" class="mdl-button mdi mdi-content-save" onclick="addNewTileClose()" style="color: darkgreen; font-size: 4vh !important;"></button>
                                         <div class="mdl-tooltip" for="save_button" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">Save/Close</div>
                                    </div>
                                    <div class="flex-item" style="max-width:18%; flex-basis: 18% padding-bottom: 0 !important;" tabindex="-1">
                                        <button id="close_button" type="button" class="mdl-button mdi mdi-close-circle" onclick="closeAddTileWindow()" style="color: darkred; font-size: 4vh !important;"></button>
                                        <div class="mdl-tooltip" for="close_button" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">Exit/Don't Save</div>
                                    </div>
                                </div>
                           </div>

                           <div class = "border-container">
                                 <div id="menu_items" class="flex-container">
                                     <div class="flex-item" style="max-width:50%; flex-basis: 50%" tabindex="-1">"""
                           list = new TreeMap(typeList.main_list);
                           html+= defineSelectBox(title: "Title Span", var: "new_tile_span", list: list, function: "selectTileSpan"); 
                                     
                           html += """
                                    </div>
                                </div>
                                  """
                            html += """
                                 <div id="menu_items" class="flex-container">
                                     <div class="flex-item" style="max-width:75%; flex-basis: 75%" tabindex="-1">"""
                           atomicState.span_type.each{span_key, span->
                                list = new TreeMap(typeList."${span_key}".measurement_list);
                                if (list!=[:])
                                    html+= defineSelectBox(title: span.title, var: span_key+"_measurement", list: list, visible: false, function: "selectTileType");
                           }
                           
                           html += """
                                    </div>
                                </div>
                            """
                           html += """
                                 <div id="menu_items" class="flex-container">
                                     <div class="flex-item" style="max-width:75%; flex-basis: 90%" tabindex="-1">"""
                           atomicState.span_type.each{span_key, span->
                               if (typeList."${span_key}".title){
                                    list = new TreeMap(typeList."${span_key}".time_list);
                                    html+= defineSelectBox(title: typeList."${span_key}".title, var: span_key+"_time", list: list, visible: false, function: "selectTileTime");
                               }
                           }
                           /*    
                           html+= defineSelectBox(title: "Days to Display", var: "daily_time", list: daily_list,   visible: false, function: "selectTileTime"); 
                           html+= defineSelectBox(title: "Hours to Display", var: "hourly_time", list: hourly_list,  visible: false, function: "selectTileTime"); 
                           */
                           html += """
                                    </div>
                                </div>
                            """
                                 
    html+= """</div> """
        
    html += """</div></div></dialog>"""
    
    return html;
}
        
def defineTileDialog(){
 
    list = [];
    
    atomicState.tile_settings.each{item->
        list << [name: item.title, icon: item.icon, var: item.var];   
    }
      
    def html = """
        <dialog id="tileOptions" class="mdl-dialog mdl-shadow--12dp" tabindex="-1" style = "background-color: rgba(255, 255, 255, 0.90); border-radius: 2vh; height: 95vh">
          <div class="mdl-dialog__content">

            <div class="mdl-layout">
              <div id="options_title" class="mdl-layout__title" style = "color: black; text-align: center;">
                Options
              </div>

           <div class="mdl-grid" style="width: 100%">
              <div class = "border-container">
              <div id="text_box" class="flex-container">
                  <div class="flex-item" style="max-width:18%; flex-basis: 18%;" tabindex="-1">
                    <button id="trash_button" type="button" class="mdl-button mdi mdi-trash-can-outline" onclick="deleteTile()" style="color: darkred; font-size: 4vh !important;"></button>
                    <div class="mdl-tooltip" for="trash_button" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">Delete this tile</div>
                  </div>
                               
                     <div class="flex-item" style="max-width:18%; flex-basis: 18%" tabindex="-1">
                        <button id="new_tile" type="button" class="mdl-button mdi mdi-shape-rectangle-plus"" onclick="newTile()" style="color: darkgreen; font-size: 4vh !important;"></button>
                        <div class="mdl-tooltip" for="new_tile" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">Place New Tile</div>
                    </div>

                    <div class="flex-item" style="max-width:10%; flex-basis: 10%;" tabindex="-1">
                    <span class="mdi mdi-drag-vertical-variant" style="color: gray; font-size: 6vh !important; line-height: 6vh !important;"></span>
                  </div>
                    
                    <div class="flex-item" style="max-width:18%; flex-basis: 18%" tabindex="-1">
                        <button id="save_button" type="button" class="mdl-button mdi mdi-content-save" onclick="saveWindow()" style="color: darkgreen; font-size: 4vh !important;"></button>
                        <div class="mdl-tooltip" for="save_button" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">Save/Close</div>
                    </div>
                    <div class="flex-item" style="max-width:18%; flex-basis: 18%" tabindex="-1">
                        <button id="save_all_button" type="button" class="mdl-button mdi mdi-content-save-all" onclick="saveAllWindow()" style="color: darkgreen; font-size: 4vh !important;"></button>
                        <div class="mdl-tooltip" for="save_all_button" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">Save Colors and Opacity to All Tiles</div>
                    </div>
                    <div class="flex-item" style="max-width:18%; flex-basis: 18% padding-bottom: 0 !important;" tabindex="-1">
                        <button id="close_button" type="button" class="mdl-button mdi mdi-close-circle" onclick="closeWindow()" style="color: darkred; font-size: 4vh !important;"></button>
                        <div class="mdl-tooltip" for="close_button" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">Exit/Don't Save</div>
                    </div>
              </div>
              </div>
"""
    
/********************ALIGNMENT****************************************/
html+= """
        <!-- ALIGNMENT -->
        <div class = "border-container">
        <div id="text_box" class="flex-container">
        <div class="flex-item" style="flex-grow:1;" tabindex="-1"> """
        
    html +=  addIconMenu(var_name: "selected_icon", title: "Select Tile Type", default_icon: "alpha-x-circle-outline", 
                             default_value: "center", tooltip: "Use Icon Name", list: getIconList(), width: 4);
    
    html += """
                </div>
                <div class="flex-item" style="flex-grow:1;" tabindex="-1">
                """
        html+= addButtonMenu(var_name: "horizontal_alignment", default_icon: "format-align-center", tooltip: "Horizontal Alignment", default_value: "center", side: "left",
                                                                                                    list:[[name: "Left",   icon: "format-align-left"], 
                                                                                                          [name: "Center", icon: "format-align-center"], 
                                                                                                          [name: "Right",  icon: "format-align-right"]]);
       
       html+=
       """
       </div>
       <div class="flex-item" style="flex-grow:1;" tabindex="-1">
       """
    
     
       html+= addButtonMenu(var_name: "icon_spacing", default_icon: "keyboard-space", tooltip: "Icon Spacing", default_value: "Single Space", side: "left",
                                                                                                    list:[[name: "No Space",     icon: "arrow-collapse-horizontal"], 
                                                                                                          [name: "Single Space", icon: "keyboard-space"], 
                                                                                                          [name: "Double Space", icon: "arrow-expand-horizontal"]]);

        
        html+=
        """
       </div>
       <div class="flex-item" style="flex-grow:1;" tabindex="-1">
       """
    
     
       html+= addButtonMenu(var_name: "decimal_places", default_icon: "decimal", tooltip: "Decimal Places", default_value: "One Decimal",  side: "left",
                                                                                                            list:[[name: "No Decimal",     icon: "hexadecimal"], 
                                                                                                                  [name: "One Decimal",    icon: "surround-sound-2-0"], 
                                                                                                                  [name: "Two Decimals",   icon: "decimal"]]);

        

        html += """
                </div>
                <div class="flex-item" style="flex-grow:1;" tabindex="-1">
                """
    
        html+= addButtonMenu(var_name: "font_weight", default_icon: "numeric-4-circle", default_value: "center", tooltip: "Font Weight", side: "right",
                                                                                        list:[[name: "Thin",   icon: "numeric-1-circle"], 
                                                                                              [name: "Normal", icon: "numeric-4-circle"], 
                                                                                              [name: "Bold",   icon: "numeric-7-circle"],
                                                                                              [name: "Thick",  icon: "numeric-9-circle"]]); 
 html += """
                </div>
                <div class="flex-item" style="flex-grow:1;" tabindex="-1">
                """
    
            html+= addButtonMenu(var_name: "units_spacing", default_icon: "keyboard-space", tooltip: "Units Spacing", default_value: "Single Space", side: "right",
                                                                                                    list:[[name: "No Space",     icon: "arrow-collapse-horizontal"], 
                                                                                                          [name: "Single Space", icon: "keyboard-space"], 
                                                                                                          [name: "Double Space", icon: "arrow-expand-horizontal"]]);
    
            
html +=  """
         </div>
         </div>
         </div>"""
           
/*****************TEXT COLOR***************************************/
        
    html+= addColorPicker(var: "text", title: "Text");
    
/****************BACKGROUND COLOR *********************************/
      
    html+= addColorPicker(var: "background", title: "Background");
    
/****************Font Adjustment  *********************************/
    html += """
        <div class = "border-container">
            <div id="text_box" class="flex-container">"""
    
                html+= addSlider(var: "font_adjustment", title: "Relative Size", min: -100, value: 0, max:100);
    
                html+="""
            </div>
        </div>
        <div class = "border-container">
            <!-- CUSTOM TEXT -->
            <div class="flex-item" style="flex-grow:auto;" tabindex="-1">
                <div class="mdl-textfield mdl-js-textfield mdl-textfield--floating-label">
                    <input class="mdl-textfield__input" type="text" id="tileText">
                    <label class="mdl-textfield__label" for="tileText">Static Text</label>
                </div>
            </div>
        </div>
        </div></div></div>
</dialog>
    """;
    return html;
}

def getTileListItem(Map map){
    function = map.function;
    var: map.name;
    menu = map.list;
    selections = map.selections.clone();
    selections << var;
    
    
    return "";
    if (!menu.list) onclick = """onclick="${map.function}('${selections}')" """
    
    html = """<span id=${var}_menu ${onclick}>"""
    if (menu.icon){
            html += """<button id="${var}_button" class="mdl-button mdl-js-button mdl-js-ripple-effect" tabindex="-1">
                       <i id="${var}_icon_display" class="mdi mdi-${default_icon}"  style="color: darkgreen; font-size: 6vh !important;"></i>
                       </button>
                    """
    }
    if (menu.text){
            html += """<span id=${var}_text>${text}</span> """  
    }   
    if (menu.tooltip){
            html += """<div class = "mdl-tooltip" for = "${button_var}_button">${tooltip}</div>"""
    }
    html += """</span>"""

    if (menu.list) {
            html += """<ul class="mdl-menu mdl-js-menu mdl-js-ripple-effect" for="${var}_menu" style="overflow-y: scroll; max-height: 50vh; line-height: 10px;"> """
            
            menu.list.each{item->
                html+= getTileListItem(name: var+"_"+item.name, list: item.list, selections: selections, function: function);
            }
        
            html += """</ul>"""
    } else {
        var = item.name.replaceAll(" ","");
        var = parent_+var;
        select = selections.clone();
        select << [item.name];
        
        func = """onclick="${function_name}('${select}')" """;
        if (item.list){
            html +=  getTileListItem(name: item.name, parent: var, function: function, list: item.list, selections: select);
        }
        else {
            html += """ <li id = "${var}_list_main" class="mdl-menu__item" ${func}>
                            <span id="${var}_list_name">${item.name}</span>
                        </li>"""
        }
        
    }
    
    html += """</ul>"""
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
      background: rgba(0, 0, 0, 0);
    }

    </style>
    <body style="background-color:black; overflow: visible;">
    
     <div class="flex-container" style="display: none;">
        
        <div id="trash" class="flex-item"  style="flex-grow:1;">
            
                <span id="trash" class="text-center mdi mdi-trash-can-outline" style="color: rgba(255, 50, 50, 0.75); background-color: rgba(0,0,0,100); font-size: 10vh; line-height: 15vh"></span>
        </div>
        <div class="mdl-tooltip" for="trash" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">
            <div>Drag a TILE to</div>
            <div class = "mdi mdi-trash-can-outline" style="font-size: 5vh"></div>
            <div>to REMOVE it</div>
        </div>


        <div style="flex-grow: 6;"></div>

           
        <div class="mdl-tooltip" for="add_tile" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);)">
             <div>CLICK to ADD a TILE</div>
        </div>
    </div>


    <div class="grid-stack grid-stack-26" data-gs-animate="yes" data-gs-verticalMargin="1" data-gs-column="26" id="main_grid">
    """
   
    //Main Tile Building Code
    atomicState.tile_settings.eachWithIndex{item, index->
       html += getTileHTML(item);
    } //each  
    
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

"""
    
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
//tile_settings_HTML
def defineUpdateDataHTML(var){
    
   if (!settings["$var"]){
       app.updateSetting("${var}", [value: "", type: "string"]);
   } 

   html = """
                <input type="text" id="settings${var}" name="settings[${var}]" value="${settings[var]}" style="display: none;" >
                <div class="form-group">
                   <input type="hidden" name="${var}.type" value="text" submitOnChange>
                   <input type="hidden" name="${var}.multiple" value="false">
                </div>
                
            """
            
   return html;
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
   
    buildWeatherData();
    
    html = defineHTML_Header();
    html += """<head>
               <meta name="viewport" content="width=device-width, initial-scale=1.0"><style>""";
    //CSS
    html += defineHTML_CSS();
    html += """</head>
               <body onload="initializeWeather()">"""
    html += defineHTML_Tile();
    html += defineTileDialog();
    html += defineNewTileDialog();
    
    
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
    buildWeatherData();
    return render(contentType: "text/json", data: JsonOutput.toJson(atomicState.tile_settings));
}

def getOptions() {
    render(contentType: "text/json", data: JsonOutput.toJson(getWeatherData()));
}

def updateSettings() {
    
    atomicState.tile_settings = request.JSON;
    //atomicState.temp_tile_settings = request.JSON;

   
    render(contentType: "application/json", data: """{"status":"success"}""");
}


