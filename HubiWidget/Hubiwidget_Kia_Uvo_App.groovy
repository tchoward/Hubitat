import groovy.json.*;
import java.text.DecimalFormat;
import java.math.*; 


/**
 *  Kia UVO App
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

// Kia UVO Changelog
// *****ALPHA BUILD

// Credit to Alden Howard for optimizing the code.

 
def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v1.0" }

definition (
    name: "Kia UVO",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Kia UVO App",
    parent: "tchoward:HubiWidgets",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
    section ("Main "){
       page(name: "mainPage", install: true, uninstall: true)
       page(name: "deviceSelectionPage", nextPage: "mainPage")
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
    unsubscribe();
    if (refreshTime) scheduleFunction(refreshTime, updateStatus);
    createChildrenDevices();

    dynamicPage(name: "mainPage") { 
        parent.hubiForm_section(this, "Graph Options", 1, "tune"){
                    container = [];
                    container << parent.hubiForm_page_button(this, "Setup", "deviceSelectionPage", "100%", "vibration");
                    parent.hubiForm_container(this, container, 1); 
        }

        if (cars){
            getCarData();
            cars.each{vin->
                if (settings["childDeviceSensor_${vin}"]){
                    subscribe(settings["childDeviceSensor_${vin}"], "power", powerUpdate, [data: vin]);
                }

                car = atomicState.vehicle_info.cars.find{ it.vin == vin };

                parent.hubiForm_section(this, "${car.name}", 1) {
                    container = [];
                    subcontainer = [];
                    subcontainer << parent.hubiForm_text(this, "<b>Model: </b> ${car.name}");
                    subcontainer << parent.hubiForm_text(this, "<b>Year: </b> ${car.year}");
                    subcontainer << parent.hubiForm_text(this, "<b>VIN: </b> ${car.vin}");
                    container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.3, 0.3, 0.3]);

                    subcontainer = [];
                    subcontainer << parent.hubiForm_text(this, "<b>Battery: </b> ${car.data.battery_level}%");
                    subcontainer << parent.hubiForm_text(this, "<b>Charging: </b> ${car.data.charge_status}");
                    subcontainer << parent.hubiForm_text(this, "<b>Range: </b> ${car.data.range} miles");
                    container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.3, 0.3, 0.3]);

                    subcontainer = [];
                    subcontainer << parent.hubiForm_text(this, "<b>Latitude:  </b> ${car.data.location.lat}");
                    subcontainer << parent.hubiForm_text(this, "<b>Longitude: </b> ${car.data.location.lon}");
                    subcontainer << parent.hubiForm_text(this, """<b>Update: </b>  ${car.data.last_sync}""");
                    container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.3, 0.3, 0.3]);

                    subcontainer = [];

                    subcontainer << parent.hubiForm_text(this, "<b>Mileage: </b> ${car.data.mileage} miles");
                    subcontainer << parent.hubiForm_text(this, "");
                    subcontainer << parent.hubiForm_text(this, "");
                    container << parent.hubiForm_subcontainer(this, objects: subcontainer, breakdown: [0.3, 0.3, 0.3]);

                    parent.hubiForm_container(this, container, 1);         
                }

            }
        }
    }  
}

def getNetworkId(idx){
    name = settings["childDeviceName_${idx}"];
    networkName = name.replaceAll("[^a-zA-Z0-9]", "");
    networkId = "${networkName}_${idx}_${app.id}";

    return networkId;
}

def getChargeLevel(vin, current_power){

    car = atomicState.vehicle_info.cars.find { it.vin == vin };

    max_charge = settings["maxCapacity_${vin}"] as Float;
    battery_level = car.data.battery_level as Float;
    total_charge = settings["chargeCapacity_${vin}"] as Float;
    sync_time = car.data.last_sync_date;
    current_time = new Date();
    current_time = current_time.getTime();

    time_diff_minutes = (current_time - sync_time)/(1000.0*60.0);
    charge_rate = ((current_power*0.9)/(total_charge*1000.0))*100.0;
    
    //Convert to minutes @ 90% efficiency
    current_charge = (battery_level+((time_diff_minutes/60.0) * charge_rate));

    time_to_charge = ((max_charge-current_charge) / charge_rate);

    return [percent: current_charge, time: time_to_charge*60];
}

def powerUpdate(evt){

    device = evt.getDevice();
   
    cars.each{vin->
        sensor = settings["childDeviceSensor_${vin}"];
        if (sensor.id == device.id){
          
            value = evt.getFloatValue() < 100 ? 0 : evt.getFloatValue();
              
            sendData(vin, "power", value, "W");

            if (value == 0){
                sendData(vin, "chargeState",   "Not Charging", "");
                sendData(vin, "timeToCharge", "0:00", "time");

                car = atomicState.vehicle_info.cars.find { it.vin == vin };

            } else { 
                
                charge = getChargeLevel(vin, value);

                hours = ((charge.time/60) as Integer);
                minutes = ((charge.time%60) as Integer);

                sendData(vin, "timeToCharge", "${hours}:${minutes<10 ? "0" : ""}${minutes}", "time");
                sendData(vin, "battery", Math.round(charge.percent), "%");

                if (charge.percent >= (settings["maxCapacity_${vin}"] as Float) + 0.1){
                    sendData(vin, "chargeState",   "Not Charging", "");
                    sendData(vin, "timeToCharge",  "0:00", "time");
                } else {
                    sendData(vin, "chargeState",   "Charging", "");
                }

            }

           
        }
    }
}

def createChildrenDevices(){

    if (!cars) return;

    cars.each{vin->
        name = settings["childDeviceName_${vin}"];
        networkId = "${vin}";

        if (getChildDevice(networkId)==null){
            addChildDevice("tchoward", "Kia UVO", networkId, [name: "Kia UVO Child Device", label: name]);
            log.debug("Child Device ${name} ${networkId} created");
        }
        else {
            log.debug("Child Device ${name} ${networkId} exists");
        }
    }
}

def updateCars(){
    log.debug("Updating Cars...");
    getCarData();
}

def getTime(text){
    
    def dateFormat = "yyyy-MM-dd'T'HH:mm:ssX";
    return Date.parse(dateFormat, text).getTime();

}

def scheduleFunction(minutes, function){
    switch (minutes){
        case "5" :   runEvery5Minutes(function); log.debug("Scheduling Every 5 Minutes"); break;
        case "10" :  runEvery10Minutes(function); log.debug("Scheduling Every 10 Minutes"); break;
        case "20" :  runEvery30Minutes(function); log.debug("Scheduling Every 20 Minutes"); break;
        case "30" :  runEvery30Minutes(function); log.debug("Scheduling Every 30 Minutes"); break;
        case "60" :  runEvery1Hour(function); log.debug("Scheduling Every 1 Hour"); break;
        case "180" : runEvery3Hours(function); log.debug("Scheduling Every 3 Hours"); break;
    }

}

def deviceSelectionPage(){

    def timeEnum = [["5" : "5 Minutes"], ["10" : "10 Minutes"], ["20" : "20 Minutes"], ["30" : "30 Minutes"], 
                    ["60" : "1 Hour"], ["180" : "3 Hours"]];
    
    if (password && username){
        log.debug("Username and Password Valid");
    }
    
    dynamicPage(name: "deviceSelectionPage"){

        parent.hubiForm_section(this,"Login Information", 1) {
            input "username", "string", title: "Kia UVO username/email", required: false, submitOnChange: true
            input "password", "password", title: "Kia UVO password", required: false, submitOnChange: true
            
            container = [];
            parent.hubiForm_container(this, container, 1);
        }

        parent.hubiForm_section(this,"Update Refresh", 1) {

            input( type: "enum", name: "refreshTime", title: "Car Refresh Time (Force Refresh of Data)", 
                                required: true, multiple: false, options: timeEnum, defaultValue: "1 Hour", submitOnChange: false);

            input( type: "enum", name: "updateTime", title: "Car Update Time (Update Events)", 
                                required: true, multiple: false, options: timeEnum, defaultValue: "30 Minutes", submitOnChange: false);
            
            container = [];
            parent.hubiForm_container(this, container, 1);         
        }

        if (settings["username"] && settings["password"]){
            
            parent.hubiForm_section(this,"Car Selection", 1) {
                container = [];
                
                if (getCookie()){
                    carEnum = [];
                    atomicState.vehicle_info.cars.each{ car->
                        carEnum << ["${car.vin}" : "${car.name} (${car.year} ${car.model} ${car.trim})"]
                    }
                    if (carEnum != []){
                        input( type: "enum", name: "cars", title: "Cars to Track", 
                                required: true, multiple: true, options: carEnum, submitOnChange: true);
                    } else {
                        container << parent.hubiForm_text(this, "<b>Error: No Cars in the Account</b>");
                    }
                } else {
                    container << parent.hubiForm_text(this, "<b>Error: ${atomicState.error}</b>");
                }
                
                parent.hubiForm_container(this, container, 1);         
            }
        }

        if (cars) {
            cars.each{vin->
                
                car = atomicState.vehicle_info.cars.find{ it.vin == vin };
                
                parent.hubiForm_section(this, "${car.name} ${car.year} ${car.model}", 1) {
                    container = [];
                    input "childDeviceName_${vin}", "string", title: "<b>Child Device Name</b>", defaultValue: "${car.name}", required: false, submitOnChange: true
                    
                    input "childDeviceSensor_${vin}", "capability.powerMeter", title: "<b>Power Sensor Device</b>", multiple: false, submitOnChange: false
                    
                    input "chargeCapacity_${vin}", "decimal", title: "<b>Charge Capacity</b> in KWh", defaultValue: 0, required:false

                    input "maxCapacity_${vin}", "number", title: "<b>Max Charge Percent</b> (0-100)", defaultValue: 0, required:false


                    parent.hubiForm_container(this, container, 1);         
                } 
            }
        }

    }
}
def readyToRun(){
    getCarData();
}

def statusReturn(resp, data){  
    //resp?.properties?.each { log.trace it }
    String output = new String(resp.data.decodeBase64());
    jsonData = new groovy.json.JsonSlurper().parseText("${output}");
    
    if (jsonData!=null){
        vin = jsonData.vehicleListOfUser.get(0).vin;

        temp = atomicState.vehicle_info;
        jsonData.vehicleListOfUser.each{car->
            vin = car.vin;
            current_data = temp.cars.find { it.vin == car.vin};
            if (current_data != null){
                if (current_data.data == null) current_data.data = [:];
                
                current_data.data.mileage =  car.data.mileage;
                sendData(vin, "mileage", car.data.mileage, "miles");
                
                current_data.data.location = car.data.gpslocation;
                sendData(vin, "latitude", car.data.gpslocation.lat, "degrees");
                sendData(vin, "longitude", car.data.gpslocation.lat, "degrees");

                current_data.data.battery_level = car.data.chargeLevel;
                sendData(vin, "battery", car.data.chargeLevel, "%");

                current_data.data.range = car.data.totalAvailableRange;
                sendData(vin, "range", car.data.totalAvailableRange, "%");

                current_data.data.charge_status = car.data.batteryCharge;
                if (car.data.batteryCharge == "true")
                    sendData(vin, "chargeState", "Charging");
                else 
                    sendData(vin, "chargeState", "Not Charging");


                current_data.data.lock_status = car.data.lockStatus;
                if (car.data.lockStatus == "true")
                    sendData(vin, "lockState", "Locked");
                else
                    sendData(vin, "lockState", "Unlocked");

                current_data.data.last_sync_raw = car.data.syncDateVal;
                current_data.data.last_sync_date = Date.parse("yyyyMMddHHmmss Z", car.data.syncDateVal+" GMT").getTime();
                sendData(vin, "syncTime", Date.parse("yyyyMMddHHmmss Z", car.data.syncDateVal+" GMT"));

            }
        }
        sendData(vin, "updateTime", new Date());
        sendData(vin, "status", "Ready");
        atomicState.vehicle_info = temp;
    }
}

def updateStatus() {
    log.debug("Kia UVO:: Update Status");
    if (username && password){
        getCookie();
        carData = [];
        count = 0;
        def request = [
            userId: "${username}",
            password: "${password}",
            userType: "0",
            vin: "",
            action: "authenticateUser"
        ];
        
        cars.each{vin->
            count++;
            car = atomicState.vehicle_info.cars.find{ it.vin == vin };
            key = car.key;
            log.debug(key);
            uri = "https://owners.kia.com/apps/services/owners/overviewvehicledata";
            queryString = "requestJson=%7B%22action%22:%22ACTION_GET_LAST_REFRESHED_STATUS_FULL_LOOP%22%7D";
            
    		try
	    	{
		        asynchttpGet(
                    "statusReturn",
			    	[
                        uri: uri,
                        queryString: queryString,
                        headers: 
                        [ 
                            "Host": "owners.kia.com",
                            "Origin": "https://owners.kia.com",
                            "Referer": "https://owners.kia.com/content/owners/en/kia-owner-portal.html/",
                            "Connection": "keep-alive",
                            "CSRF-Token": "undefined",
                            "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_3_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36",
                            
                            "Accept": "application/json, text/plain",
                            "Accept-Language": "en-US,en;q=0.9",
                            "Cookie" : "${atomicState.vehicle_info.cookie}",
                            "vinkey": "${key}"

                        ],
                        requestContentType: "application/x-www-form-urlencoded",
                        contentType: "UTF-8;charset=iso-8859-1",
                        timeout: 300
                    ]
			    )
                sendData(vin, "status", "Querying Server");
		    }
		    catch (e)
		    {
                log.error "Error sending Refresh Request: ${e.getCause()}"
                returnVal = false;
		    }
        }
        return returnVal;
    }
    else return false;
}

def getCookie() {
    
	if (username && password){
		def result = false
        cookie = null;
        
        def body = [
            userId: "${username}",
            password: "${password}",
            userType: "0",
            vin: "",
            action: "authenticateUser"
        ];
        
        def bodyJSON = JsonOutput.toJson(body);

		try
		{
		    httpPost(
				[
                    uri: "https://owners.kia.com/apps/services/owners/apiGateway",
                    headers: 
                    [ 
                        "Host": "owners.kia.com",
                        "Origin": "https://owners.kia.com",
                        "Referer": "https://owners.kia.com/content/owners/en/kia-owner-portal.html/",
                        "Connection": "keep-alive",
                        "CSRF-Token": "undefined",
                        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_3_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36",
                        
                        "Accept": "application/json, text/plain, */*",
                        "Accept-Encoding": "gzip, deflate, br",
                        "Accept-Language": "en-US,en;q=0.9",
                    ],
			        requestContentType: "application/x-www-form-urlencoded",
                    body: bodyJSON
				]
			)
			{ resp ->
				data = resp.getData();
                jsonData = new groovy.json.JsonSlurper().parseText("${data}");

                if (jsonData.status.statusCode == 0){
                    car_data = [:];
                    car_data.cookie = resp.headers.getAt("Set-Cookie").buffer-"Set-Cookie: "-";Path=/;Secure;HttpOnly";
                    car_data.cars = [];
                    jsonData.payload.vehicleSummary.each{car->
                        car_data.cars << [
                                vin:    car.vin,
                                name:   car.nickName,
                                model:  car.modelName,
                                year:   car.modelYear,
                                trim:   car.trim,
                                key:    car.vehicleKey
                        ];
                    }
                    atomicState.vehicle_info = car_data;
                } else {
                    atomicState.cars = car_data;
                } 
                returnVal = true;       
			}
		}
		catch (e)
		{
			log.error "Error logging in: ${e.getCause()}"
			returnVal = false;
		}
        return returnVal;
	}
	else return false;
}

def sendData(vin, attribute, value, units = ""){
    
    networkId = "${vin}";

    child = getChildDevice(networkId);
    if (child == null){
        log.debug("Cannot send event: Child Device ${attribute} on ${vin}");
        return;
    }
    child.sendEvent(name: attribute, value: value, unit: units);
}

def updateCarEvents(){

    cars.each{vin->

        car = atomicState.vehicles.find{ it.vin == vin };

        sendData(vin, "battery", car.data.batteryStatus, "%");
        sendData(vin, "range",   car.data.totalAvailableRange, "miles");

        if (car.data.batteryCharge == true){
            sendData(vin, "chargeState",   "Charging", "");
        } else {
            sendData(vin, "chargeState",   "Not Charging", "");
        }
        sendData(vin, "latitude",   car.data.gpslocation.lat, "degrees");
        sendData(vin, "longitude",  car.data.gpslocation.lon, "degrees");

        def dateFormat = "yyyy-MM-dd HH:mm:ss Z"
        def dateString = "${car.data.syncDate} GMT"
        def date = Date.parse(dateFormat, dateString);

        sendData(vin, "updateTime",   date, "");
        sendData(vin, "lastUpdate",   new Date(), "");
    }
}

def getCarData() {

    log.debug("Kia UVO:: Update Car Data");
	if (username && password){
        getCookie();
		        
        keys = [];
        carData = [];

        cars.each{vin->
            car = atomicState.vehicle_info.cars.find{ it.vin == vin };
            keys = ["${car.key}"];

            def body = [
                vehicleKeys: keys,
                action: "getVehicleInfo",
                sid : "sid"
            ];
        
            def bodyJSON = JsonOutput.toJson(body);
            
    		try
	    	{
		        httpPost(
			    	[
                        uri: "https://owners.kia.com/apps/services/owners/apiGateway",
                        headers: 
                        [ 
                            "Host": "owners.kia.com",
                            "Origin": "https://owners.kia.com",
                            "Referer": "https://owners.kia.com/content/owners/en/kia-owner-portal.html/",
                            "Connection": "keep-alive",
                            "CSRF-Token": "undefined",
                            "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_3_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.90 Safari/537.36",
                            
                            "Accept": "application/json, text/plain, */*",
                            "Accept-Encoding": "gzip, deflate, br",
                            "Accept-Language": "en-US,en;q=0.9",
                            "Cookie" : "${atomicState.vehicle_info.cookie}"
                        ],
                        requestContentType: "application/x-www-form-urlencoded",
                        body: bodyJSON
                    ]
			    )
			    { resp ->

				    data = resp.getData();
                    jsonData = new groovy.json.JsonSlurper().parseText("${data}"); 
                    log.debug(jsonData);

                    if (jsonData!=null){
                        temp = atomicState.vehicle_info;
                        jsonData.vehicleInfoList.each{input->
                            current_data = temp.cars.find { it.vin == vin};a
                            if (current_data != null){
                                if (current_data.data == null) current_data.data = [:];
                                current_data.data.mileage = input.data.mileage;
                                current_data.data.location = input.data.gpslocation;
                                current_data.data.battery_level = input.data.batteryStatus;
                                current_data.data.range = input.data.totalAvailableRange;
                                current_data.data.charge_status = input.data.batteryCharge;

                                dateFormat = "yyyy-MM-dd HH:mm:ss Z"
                                dateString = "${input.data.syncDate} GMT"
                                current_data.data.last_sync = Date.parse(dateFormat, dateString).format("MMM d 'at' h:mm a");
                            }
                        }
                        atomicState.vehicle_info = temp;
                    }
                }
		    }
		    catch (e)
		    {
                log.error "Error Retrieving Car Information in: ${e}"
                returnVal = false;
		    }
        }
        return returnVal;
    }
    else return false;
}

def convertToJSON(obj){

    def jsonSlurper = new JsonSlurper();
    return jsonSlurper.parseText(obj);

}
