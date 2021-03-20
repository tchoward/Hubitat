import groovy.json.*;
import java.text.DecimalFormat;
import java.math.*; 


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

definition (
    name: "Hubigraph Long Term Storage",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Long Term Storage",
    category: "",
    parent: "tchoward:Hubigraphs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
    section ("test"){
       page(name: "mainPage", install: true, uninstall: true)
       page(name: "deviceSelectionPage", nextPage: "optionsPage")
       page(name: "optionsPage", nextPage: "mainPage")
    }
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
def mainPage() {
    
    unschedule();
    dynamicPage(name: "mainPage") { 
        parent.hubiForm_section(this, "Graph Options", 1, "tune"){
                    container = [];
                    container << parent.hubiForm_page_button(this, "Select Device/Data", "deviceSelectionPage", "100%", "vibration");
                    parent.hubiForm_container(this, container, 1); 
        }
        if (sensors){
            parent.hubiForm_section(this,"Current Attribute Storage", 1) {
                container = [];
                subcontainer = [];

                subcontainer << parent.hubiForm_text(this, "<b>Sensor</b>");
                subcontainer << parent.hubiForm_text(this, "<b>Attribute</b>");
                subcontainer << parent.hubiForm_text(this, "<b>Number of Events</b>");
                subcontainer << parent.hubiForm_text(this, "<b>First Event Time</b>");
                subcontainer << parent.hubiForm_text(this, "<b>Last Event Time</b>");
                subcontainer << parent.hubiForm_text(this, "<b>File Size</b>");


                container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.2, 0.2, 0.2, 0.2, 0.2, 0.2]);

                sensors.each { sensor->
                    if (settings["${sensor.id}_attributes"]){

                        settings["${sensor.id}_attributes"].each{attribute->

                            sensor_name = sensor.label != null ? sensor.label : sensor.name;

                            subcontainer = [];

                            //appendFile(sensor, attribute);

                            storage = getCurrentDailyStorage(sensor, attribute);

                            filename_ = getFileName(sensor, attribute);

                            uri_ = "http://${location.hub.localIP}:8080/local/${filename_}";

                            subcontainer << parent.hubiForm_text(this, sensor_name, uri_);
                            subcontainer << parent.hubiForm_text(this, attribute, uri_);
                            subcontainer << parent.hubiForm_text(this, storage.num_events);
                            subcontainer << parent.hubiForm_text(this, storage.first);
                            subcontainer << parent.hubiForm_text(this, storage.last);
                            subcontainer << parent.hubiForm_text(this, convertStorageSize(storage.size));

                            container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.2, 0.2, 0.2, 0.2, 0.2, 0.2]);
                        
                            getCronString(sensor, attribute);  

                            data = [id: sensor.id, attribute: attribute];
                            updateData(data);                      
                        }   
                    }
                }
                parent.hubiForm_container(this, container, 1);   
            }  
            

        }
    }  
}

def isStorage(id, attribute){
    
    sensor = sensors.find{it.id == id};

    if (sensor != null){
        if (settings["${id}_attributes"].find{it == attribute} != null) return true;
        else return false;
    } else {
        return false;
    }
}

def updateData(data){
    sensor = sensors.find{it.id == data.id};
    appendFile(sensor, data.attribute);
}

def getCronString(sensor, attribute){

    def dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    
    attr = attribute.replaceAll(" ", "_");
    date = Date.parse(dateFormat, settings["${sensor.id}_${attr}_time"]);
    repeat = settings["${sensor.id}_${attr}_time_every"];

    schedule("0 ${date.getMinutes()} ${date.getHours()}/${repeat} ? * * *", updateData, [overwrite: false, data: [id: sensor.id, attribute: attribute]]);

}

def averageFrequency(events){
    sum=0;
    for (i=1; i<events.size(); i++){
        sum += events[i].date.getTime() - events[i-1].date.getTime();
    }
    return sum/events.size();
}

def sum(events, decimals, round, granularity){
    sum = new Float(0);
    events.each{event->
        sum += Float.valueOf(event.value);
    }

    tdate = [date : events[events.size()-1].date, boundary: round, granularity: granularity];
    return [date: roundDate(tdate), value: sum.round(decimals)];
}

def average(events, decimals, round, granularity){
    sum = new Float(0);
    events.each{event->
        sum += Float.valueOf(event.value);
    }
    sum /= events.size();
        
    tdate = [date : events[events.size()-1].date, boundary: round, granularity: granularity];
    return [date: roundDate(tdate), value: sum.round(decimals)];
}

def min(events, decimals, round, granularity){
    min = Float.valueOf(events[0].value);
    events.each{event->
        min = Float.valueOf(event.value) < min ? Float.valueOf(event.value) : min;
    }
    
    tdate = [date : events[events.size()-1].date, boundary: round, granularity: granularity];
    return [date: roundDate(tdate), value: min.round(decimals)];
}

def max(events, decimals, round, granularity){
    max = Float.valueOf(events[0].value);
    events.each{event->
        max = Float.valueOf(event.value) > max ? Float.valueOf(event.value) : max;
    }

    tdate = [date : events[events.size()-1].date, boundary: round, granularity: granularity];
    return [date: roundDate(tdate), value: max.round(decimals)];
}

def count(events, decimals, round, granularity){
    
    tdate = [date : events[events.size()-1].date, boundary: round, granularity: granularity];
    return [date: roundDate(tdate), value: events.size()];
}

def getTime(text){
    
    def dateFormat = "yyyy-MM-dd'T'HH:mm:ssX";
    return Date.parse(dateFormat, text).getTime();

}

def roundDate(Map map){

    t = [["0": "None"], ["5" : "5 Minutes"], ["10" : "10 Minutes"], ["20" : "20 Minutes"], ["30" : "30 Minutes"], 
                       ["60" : "1 Hour"], ["120" : "2 Hours"], ["180" : "3 Hours"], ["240" : "4 Hours"], ["360" : "6 Hours"],
                       ["480" : "8 Hours"], ["1440" : "24 Hours"]];

    date = map.date;
    boundary = map.boundary != null ? map.boundary : false;
    granularity = map.granularity as Integer;

    if (!boundary) return date;

    if (granularity > 60 && granularity < 1440)
        nearest = org.apache.commons.lang3.time.DateUtils.truncate(date, Calendar.HOUR);
    else if (granularity == 1440)
        nearest = org.apache.commons.lang3.time.DateUtils.truncate(date, Calendar.DAY);

    return nearest;    
}


def quantizeData(events, mins, funct, dec, boundary){
    
    minutes = mins as Integer;
    decimals = dec as Integer;
    
    microSeconds = minutes*1000*60;
    newEvents = [];
    if (microSeconds == 0) return events;
    
    stop = roundDate([date: events[0].date, granularity: minutes, boundary: boundary]).getTime() + microSeconds;

    tempEvents = [];
    idx = 0;

    while (idx < events.size()){
        
        currTime = roundDate([date: events[idx].date, granularity: minutes, boundary: boundary]).getTime();

        if (currTime > stop){
            if (tempEvents.size() == 1){
                newEntry = "${funct}"(tempEvents, decimals, boundary, minutes);
                newEvents.add(newEntry);
            } else if (tempEvents.size() != 0){
                newEntry = "${funct}"(tempEvents, decimals, boundary, minutes);
            }
            stop += microSeconds; 
            tempEvents = [];
        }
        tempEvents.add(events[idx]);
        idx++;
    }

    if (tempEvents.size() == 1){   
        newEntry = tempEvents[0];
    
    } else if (tempEvents.size() != 0){
       
        newEntry = "${funct}"(tempEvents, decimals, boundary, minutes);
        newEvents.add(newEntry);
    }

    return newEvents;
}

def optionsPage() {

    def quantizationEnum = [["0": "None"], ["5" : "5 Minutes"], ["10" : "10 Minutes"], ["20" : "20 Minutes"], ["30" : "30 Minutes"], 
                       ["60" : "1 Hour"], ["120" : "2 Hours"], ["180" : "3 Hours"], ["240" : "4 Hours"], ["360" : "6 Hours"],
                       ["480" : "8 Hours"], ["1440" : "24 Hours"]];

    def quantizationFunctionEnum = [["sum": "Sum Values"], ["average" : "Average Values"], ["count" : "Count Events"], 
                                    ["min" : "Minimum Value"], ["max" : "Maximum Value"]];

    def storageEnum = [["1" : "1 Day"], ["2" : "2 Days"], ["3" : "3 Days"], ["4" : "4 Days"], ["5" : "5 Days"], ["6" : "6 Days"], 
                       ["7" : "1 Week"], ["14" : "2 Weeks"], ["21" : "3 Weeks"], 
                       ["30" : "1 Month"], ["60" : "2 Months"], ["90" : "3 Months"], ["120" : "4 Months"], ["150" : "5 Months"], ["180" : "6 Months"],
                       ["210" : "7 Months"], ["240" : "8 Months"], ["270" : "9 Months"], ["300" : "10 Months"], ["330" : "11 Months"], 
                       ["365" : "1 Year"], ["730" : "2 Years"]];

    def hoursEnum = 0..23;

    def df = new DecimalFormat("#0.0");

    dynamicPage(name: "optionsPage") { 
        sensors.each { sensor->
            id = sensor.id;
            if (settings["${id}_attributes"]){
                settings["${id}_attributes"].each{ attribute->
                    attr = attribute.replaceAll(" ", "_");

                    sensor_name = sensor.label != null ? sensor.label : sensor.name;
                    parent.hubiForm_section(this, "${sensor_name} (${attribute})", 1) {

                        input( type: "enum", name: "${sensor.id}_${attr}_storage", title: "Amount of Storage to Maintain", 
                                required: false, multiple: false, options: storageEnum, submitOnChange: false, defaultValue: "7");

                        input( type: "bool", name: "${sensor.id}_${attr}_boundary", title: "Save Data on Hour/Day Boundary", 
                                required: false, multiple: false, submitOnChange: false, defaultValue: false);

                        input ( type: "enum", name: "${sensor.id}_${attr}_time_every", title: "Store Data Every X Hours", 
                                required: true, multiple: false, options: hoursEnum, submitOnChange: false, defaultValue: 24);
                        
                        input ( type: "time", name: "${sensor.id}_${attr}_time", title: "Time to Start Storing Data", 
                                required: false, multiple: false, submitOnChange: false, defaultValue: "00:00");

                        input( type: "enum", name: "${sensor.id}_${attr}_quantization", title: "Data Quantization", 
                                required: false, multiple: false, options: quantizationEnum, submitOnChange: true, defaultValue: "0");

                        input( type: "enum", name: "${sensor.id}_${attr}_quantization_function", title: "Quantization Function", 
                                required: false, multiple: false, options: quantizationFunctionEnum, submitOnChange: true, defaultValue: "average");

                        input( type: "enum", name: "${sensor.id}_${attr}_quantization_decimals", title: "Quantization Decimals to Maintain", 
                                required: false, multiple: false, options: [[0: "Zero"], [1: "One"], [2: "Two"], [3: "Three"], [4: "Four"]], 
                                submitOnChange: true, defaultValue: "1");

                        
                        container = [];

                        events = getEvents(sensor: sensor, attribute: attribute, days: 1);
                        num_events = events.size();
                        now = new Date();
                        if (num_events > 2){

                            span = (events[num_events-1].date.getTime()-events[0].date.getTime())/(1000*60*60*24);
                            since = (now.getTime() - events[0].date.getTime())/(1000*60*60);
                            quantization_minutes = settings["${sensor.id}_${attr}_quantization"] ? settings["${sensor.id}_${attr}_quantization"] : "0"; 
                            quantization_function = settings["${sensor.id}_${attr}_quantization_function"] ? 
                                                    settings["${sensor.id}_${attr}_quantization_function"] : "average"; 
                            quantization_decimals = settings["${sensor.id}_${attr}_quantization_decimals"] ? 
                                                    settings["${sensor.id}_${attr}_quantization_decimals"] : 1; 
                            quantization_boundary = settings["${sensor.id}_${attr}_boundary"] ? 
                                                    settings["${sensor.id}_${attr}_boundary"] : false;
                              
                            quantData = quantizeData(events, quantization_minutes, quantization_function, quantization_decimals, quantization_boundary);

                            frequency = averageFrequency(events);
                            container << parent.hubiForm_sub_section(this, "Estimated Storage Expense");
                            container << parent.hubiForm_text(this, "<b>Total Events:</b> ${quantData.size()} quantized (${num_events} raw data)");
                            container << parent.hubiForm_text(this, "<b>First Event:</b> ${events[0].date} (<b>${round(since)}</b> hours ago)");
                            container << parent.hubiForm_text(this, "<b>Frequency of raw data:</b> 1 event every ${round(frequency/(1000*60))} minutes");

                            subcontainer = [];
                            subcontainer << parent.hubiForm_text(this, "");
                            subcontainer << parent.hubiForm_text(this, "<b>Daily Storage</b>");
                            subcontainer << parent.hubiForm_text(this, "<b>Weekly Storage</b>");
                            subcontainer << parent.hubiForm_text(this, "<b>Monthly Storage</b>");
                            subcontainer << parent.hubiForm_text(this, "<b>Yearly Storage</b>");
                            container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.2, 0.2, 0.2, 0.2, 0.2]);

                            subcontainer = [];
                            daily = (num_events/span)*50;
                            subcontainer << parent.hubiForm_text(this, "Raw Data");
                            subcontainer << parent.hubiForm_text(this, "${convertStorageSize(daily)}");
                            subcontainer << parent.hubiForm_text(this, "${convertStorageSize(daily*7)}");
                            subcontainer << parent.hubiForm_text(this, "${convertStorageSize(daily*30)}");
                            subcontainer << parent.hubiForm_text(this, "${convertStorageSize(daily*365)}");
                            container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.2, 0.2, 0.2, 0.2, 0.2]);

                            subcontainer = [];
                            daily = (quantData.size()/span)*50;
                            subcontainer << parent.hubiForm_text(this, "Quantized Data");
                            subcontainer << parent.hubiForm_text(this, "${convertStorageSize(daily)}");
                            subcontainer << parent.hubiForm_text(this, "${convertStorageSize(daily*7)}");
                            subcontainer << parent.hubiForm_text(this, "${convertStorageSize(daily*30)}");
                            subcontainer << parent.hubiForm_text(this, "${convertStorageSize(daily*365)}");
                            container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.2, 0.2, 0.2, 0.2, 0.2]);
                               
                        
                        }

                        
                        parent.hubiForm_container(this, container, 1);    
                    }
                }
            }

        }
    }
}


def deviceSelectionPage(){
    
    if (password && username){
        log.debug("Username and Password Valid");
    }
    
    dynamicPage(name: "deviceSelectionPage"){

        parent.hubiForm_section(this,"Login Information", 1) {
                if (settings["hpmSecurity"]==null){
                    settings["hpmSecurity"] = true;
                    app.updateSetting("hpmSecurity", [type: "bool", value: "true"]); 
                }

                container = [];
                container << parent.hubiForm_switch (this, title: "<b>Use Hubitat Security?</b>", 
                                                           name: "hpmSecurity", default: true, submit_on_change: true);
                
                parent.hubiForm_container(this, container, 1);         


                if (settings["hpmSecurity"] == true){
                    input "username", "string", title: "Hub Security username", required: false, submitOnChange: true
                    input "password", "password", title: "Hub Security password", required: false, submitOnChange: true
                }
        }
        if (settings["hpmSecurity"] == true && !login()){
            parent.hubiForm_section(this,"Login Error", 1) {
                container = [];
                container << parent.hubiForm_text(this, """<b>CANNOT LOGIN</b><br>If you have Hub Security Enabled, please put in correct login credentials<br>
                                                                If not, please deselect <b>Use Hubitat Security</b>""");
                parent.hubiForm_container(this, container, 1);         
            }

        } else {
            parent.hubiForm_section(this,"Sensor and Attribute Selection", 1) {

                input "sensors", "capability.*", title: "<b>Sensor Selection for Long Term Storage</b>", multiple: true, submitOnChange: true

                if (sensors){   

                    final_attrs = [];
                    sensors.each { sensor->
                        try {
                            final_attrs = [];
                            attributes_ = sensor.getSupportedAttributes();
                            attributes_.each{ attribute_->
                                name = attribute_.getName();
                                if (sensor.currentState(name)){
                                    final_attrs << ["${name}" : "${name} ::: [${sensor.currentState(name).getValue()}]"];
                                }   
                            }
                            final_attrs = final_attrs.unique(false);   
                        } catch (e) {
                            final_attrs = [["1" : "ERROR"]];
                            log.debug("Error: ${e}");
                        }
                        sensor_name = sensor.label != null ? sensor.label : sensor.name;
                        input( type: "enum", name: "${sensor.id}_attributes", title: "${sensor_name} attribute(s) to Store", 
                                required: true, multiple: true, options: final_attrs, submitOnChange: false);
                    }
                }
            }
        }
    }
}

def getEvents(Map map){

    sensor = map.sensor;
    attribute = map.attribute;
    days = map.days;

    if (map.start_time){
        then = map.start_time;
    } else {
        now = new Date();
        then = now;
        use (groovy.time.TimeCategory) {
            then -= days.days;
        }
    }

    respEvents = sensor.statesSince(attribute, then, [max: 2000]).collect{[ date: it.date, value: it.value]}
    respEvents = respEvents.flatten();
    respEvents = respEvents.reverse();

    return respEvents;
}

def login() {
    
	if (settings["hpmSecurity"] && settings["hpmSecurity"]==true)
	{
		def result = false
		try
		{
			httpPost(
				[
					uri: "http://127.0.0.1:8080",
					path: "/login",
					query: 
					[
						loginRedirect: "/"
					],
					body:
					[
						username: username,
						password: password,
						submit: "Login"
					],
					textParser: true,
					ignoreSSLIssues: true
				]
			)
			{ resp ->
				if (resp.data?.text?.contains("The login information you supplied was incorrect."))
					result = false
				else {
					atomicState.cookie = resp?.headers?.'Set-Cookie'?.split(';')?.getAt(0)
					result = true
				}
			}
		}
		catch (e)
		{
			log.error "Error logging in: ${e}"
			result = false
		}
		return result
	}
	else
		return true
}

def fileExists(sensor, attribute){

    filename_ = getFileName(sensor, attribute);

    uri = "http://${location.hub.localIP}:8080/local/${filename_}";

     def params = [
        uri: uri,
        textParser: true,
    ]

    try {
        httpGet(params) { resp ->
            if (resp != null){
                return true;
            } else {
                return false;
            }
        }
    } catch (exception){
        if (exception.message == "Not Found"){
            log.debug("File DOES NOT Exists for ${sensor.name} (${attribute})");
        } else {
            log.error("Find file ${sensor.name} (${attribute}) :: Connection Exception: ${exception.message}");
        }
        return false;
    }

}

def readFile(sensor, attribute){

    filename_ = getFileName(sensor, attribute);

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
                
                return [size: data.length, data: parse];
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

def getFileName(sensor, attribute){
    attr = attribute.replaceAll(" ", "_");
    return "HubiGraph_LTS_${sensor.id}_${attr}.json";
}

def pruneData(input_data, time){
    
    days = time as Integer;
    if (days == 0) return input_data;

    then = new Date();
    use (groovy.time.TimeCategory) {
        then -= days.days;
    }

    startDate = then.getTime();

    return_data = input_data;

    date = input_data[0].date.getTime();

    while (date < startDate){
        return_data.remove(0);    
        date = return_data[0].date.getTime();
    }

    return return_data;
          
}

def addData(main, append){

    return_data = main;

    append.each{data->
        return_data << data;
    }

    return return_data;

}

def convertToMap(json){

    def dateFormat = "yyyy-MM-dd'T'HH:mm:ssX";

    return_data = [];

    json.each{data->
        date = Date.parse(dateFormat, data.date);
        return_data << [date: date, value: data.value];
    }

    return return_data;

}

def getFileData(sensor, attribute){
    filename_ = getFileName(sensor, attribute);

    uri = "http://${location.hub.localIP}:8080/local/${filename_}"

    def params = [
        uri: uri,
        textParser: true,
    ]
    parse_data =[];

    try {
        httpGet(params) { resp ->
            file_data = resp.getData();
            jsonSlurper = new JsonSlurper();    
            parse_data = convertToMap(jsonSlurper.parseText("${file_data}"));
        }
    } catch (e) {
        log.debug("Cannot Get File data for ${sensor.name} (${attribute})")
    }
    return parse_data;
}

def appendFile(sensor, attribute){

    filename_ = getFileName(sensor, attribute);
    attr = attribute.replaceAll(" ", "_");

    quantization_minutes =  settings["${sensor.id}_${attr}_quantization"]; 
    quantization_function = settings["${sensor.id}_${attr}_quantization_function"]; 
    quantization_decimals = settings["${sensor.id}_${attr}_quantization_decimals"];
    quantization_boundary = settings["${sensor.id}_${attr}_boundary"] ? 
                            settings["${sensor.id}_${attr}_boundary"] : false;

    storage =               settings["${sensor.id}_${attr}_storage"] as Integer;

    uri = "http://${location.hub.localIP}:8080/local/${filename_}"

    def params = [
        uri: uri,
        textParser: true,
    ]

    try {
        httpGet(params) { resp ->
           //File exists and is good
            file_data = resp.getData();
            jsonSlurper = new JsonSlurper();
            
            parse_data = convertToMap(jsonSlurper.parseText("${file_data}"));
            
            parse_data = pruneData(parse_data, storage);
                        
            //Get the most Current Data
            then = parse_data[parse_data.size()-1].date;

            respEvents = getEvents(sensor: sensor, attribute: attribute, start_time: then);

            if (respEvents != []){

                write_data = addData(parse_data, respEvents);

            } else {

                write_data = parse_data;
            }
            
            write_data = quantizeData(write_data, quantization_minutes, quantization_function, quantization_decimals, quantization_boundary);
            
            writeFile(sensor, attribute, JsonOutput.toJson(write_data))

        }
    } catch (exception){

        if (exception.message == "Not Found"){

            then = new Date();
            use (groovy.time.TimeCategory) {
                then -= storage.days;
            }
            
            respEvents = getEvents(sensor: sensor, attribute: attribute, start_time: then);
            respEvents = quantizeData(respEvents, quantization_minutes, quantization_function, quantization_decimals, quantization_boundary);

            write_data = respEvents == null ? "" : JsonOutput.toJson(respEvents);

            writeFile(sensor, attribute, write_data)
        
        } else {
             log.error("Find file ${sensor.name} (${attribute}) :: Connection Exception: ${exception}");
        }
    }
}

def writeFile(sensor, attribute, contents) {
    
    filename_ = getFileName(sensor, attribute);

    if (!login()) return;
	try
	{
		def params = [
			uri: "http://127.0.0.1:8080",
			path: "/hub/fileManager/upload",
			query: [
				"folder": "/"
			],
			headers: [
				"Cookie": atomicState.cookie,
				"Content-Type": "multipart/form-data; boundary=----WebKitFormBoundaryDtoO2QfPwfhTjOuS"
			],
			body: """------WebKitFormBoundaryDtoO2QfPwfhTjOuS
Content-Disposition: form-data; name="uploadFile"; filename="${filename_}"
Content-Type: text/plain

${contents}

------WebKitFormBoundaryDtoO2QfPwfhTjOuS
Content-Disposition: form-data; name="folder"


------WebKitFormBoundaryDtoO2QfPwfhTjOuS--""",
			timeout: 300,
			ignoreSSLIssues: true
		]
		httpPost(params) { resp ->	
		}
		return true
	}
	catch (e) {
		log.error "Error installing file: ${e}"
	}
	return false
}


def getOpenWeatherData(){
    childDevice =  getChildDevice("OPEN_WEATHER${app.id}");
    if (!childDevice){
         log.debug("Error: No Child Found");
         return null;
    }
    return(childDevice.getWeatherData());
}

def getOpenWeatherChild(){
    return getChildDevice("OPEN_WEATHER${app.id}");
}

def convertToJSON(obj){

    def jsonSlurper = new JsonSlurper();
    return jsonSlurper.parseText(obj);

}

def getCurrentDailyStorage(sensor, attribute){

    if (fileExists(sensor, attribute)){

        json = readFile(sensor, attribute);
        data = json.data;
        size = json.size;

        def dateFormat = "yyyy-MM-dd'T'HH:mm:ssX";
        first = Date.parse(dateFormat, data[0].date);
        then = Date.parse(dateFormat, data[data.size()-1].date);

        respEvents = getEvents(sensor: sensor, attribute: attribute, start_time: then)
        
        file_string = respEvents == null ? "" : JsonOutput.toJson(respEvents);

        return [num_events: data.size(), first: first, last: then, size: size];

    } else {

        try{

            respEvents = getEvents(sensor: sensor, attribute: attribute, days: 30)
            
            file_string = respEvents == null ? "" : JsonOutput.toJson(respEvents);

            writeFile(sensor, attribute, file_string);
            
            return [num_events: respEvents.size(), first: respEvents[0].date, last: respEvents[respEvents.size()-1].date, size: respEvents.size()];

        } catch (e)  {
            log.debug("Error: ${e}")
            return -1;
        }

    }
}

def getSensor(str){

    split = str.tokenize('.');
    sensor = sensors.find{ it.id == split[0]};
    return [ sensor: sensor, attribute: split[1] ];

}

def convertStorageSize(num){
    def df = new DecimalFormat("#0.0")
    
    if (num < 1024){
        return "${df.format(num)} bytes";

    } else if (num < 1048576){
        return "${df.format(num/1024.0)} Kb";
    } else {
        return "${df.format(num/1048576.0)} Mb";
    }

}

def round(num){
    def df = new DecimalFormat("#0.0")
    return "${df.format(num)}"
}

