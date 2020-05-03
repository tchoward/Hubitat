/**
 *  Hubigraph Timeline Child App
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
// V 0.1 Intial release
// V 0.2 Fixed startup code which needed all three device types, now one will work
 
import groovy.json.JsonOutput

def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v1.0" }

definition(
    name: "Hubigraph Time Line",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Time Line",
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
    }
    path("/getData/") {
        action: [
            GET: "getData"
        ]
    }
        
    path("/getConfig/") {
        action: [
            GET: "getConfig"
        ]
    }
}

def deviceSelectionPage() {
          
    dynamicPage(name: "deviceSelectionPage") {
        section() { 
            input (type: "capability.switch", name: "switches", title: "Choose Switches", multiple: true);
            input (type: "capability.motionSensor", name: "motions", title: "Choose Motion Sensors", multiple: true);
            input (type: "capability.contactSensor", name: "contacts", title: "Choose Contact Sensors", multiple: true);
        }
    }
}


def graphSetupPage(){
    def fontEnum = [["1":"1"], ["2":"2"], ["3":"3"], ["4":"4"], ["5":"5"], ["6":"6"], ["7":"7"], ["8":"8"], ["9":"9"], ["10":"10"], 
                    ["11":"11"], ["12":"12"], ["13":"13"], ["14":"14"], ["15":"15"], ["16":"16"], ["17":"17"], ["18":"18"], ["19":"19"], ["20":"20"]];  
    
    def colorEnum = ["Maroon", "Red", "Orange", "Yellow", "Olive", "Green", "Purple", "Fuchsia", "Lime", "Teal", "Aqua", "Blue", "Navy", "Black", "Gray", "Silver", "White", "Transparent"];
    def timeEnum = [["0":"Never"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1800000":"Half Hour"], 
                    ["3600000":"1 Hour"]]
            
    
    dynamicPage(name: "graphSetupPage") {
        section(){
            paragraph getTitle("General Options");
            input( type: "enum", name: "graph_update_rate", title: "Select graph update rate", multiple: false, required: false, options: timeEnum, defaultValue: "300000" ) 
            input( type: "enum", name: "graph_timespan", title: "Select Timespan to Graph", multiple: false, required: false, options: [["1":"1 hour"], ["2":"12 hours"], ["3":"1 day"], ["4":"3 days"], ["5":"1 Week"], ["6": "1 Minute"]], defaultValue: "3")     
            input( type: "enum", name: "graph_background_color", title: "Background Color", defaultValue: "White", options: colorEnum);
            
            //Size
            paragraph getTitle("Graph Size");
            input( type: "bool", name: "graph_static_size", title: "Set size of Graph? (False = Fill Window)", defaultValue: false, submitOnChange: true);
            if (graph_static_size==true){
                input( type: "number", name: "graph_h_size", title: "Horizontal dimension of the graph", defaultValue: "800", range: "100..3000");
                input( type: "number", name: "graph_v_size", title: "Vertical dimension of the graph", defaultValue: "600", range: "100..3000");
            }
            
            //Axis
            paragraph getTitle("Axes");
            input( type: "enum", name: "graph_axis_font", title: "Graph Axis Font Size", defaultValue: "9", options: fontEnum); 
            input( type: "enum", name: "graph_axis_color", title: "Graph Axis Text Color", defaultValue: "Black", options: colorEnum);
        }
    }
}

def disableAPIPage() {
    dynamicPage(name: "disableAPIPage") {
        section() {
            if (state.endpoint) {
                try {
                   revokeAccessToken();
                }
                catch (e) {
                    log.debug "Unable to revoke access token: $e"
                }
                state.endpoint = null
            }
            paragraph "It has been done. Your token has been REVOKED. Tap Done to continue."
        }
    }
}

def enableAPIPage() {
    dynamicPage(name: "enableAPIPage", title: "") {
        section() {
            if(!state.endpoint) initializeAppEndpoint();
            if (!state.endpoint){
                paragraph "Endpoint creation failed"
            } else {
                paragraph "It has been done. Your token has been CREATED. Tap Done to continue."
            }
        }
    }
}

def getTimeString(string_){
    //log.debug("Looking for $string_")
    switch (string_.toInteger()){
        case 0: return "Never"; 
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


def mainPage() {
    def timeEnum = [["0":"Never"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], 
                    ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]]
    
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
         
                
                    def paragraph_ = /<table width="100%" ID="Table2" style="margin: 0px;">/
                    paragraph_ +=  "${getTableRow3("<i><u><b>DEVICE INFORMATION</b></u></i>", "","")}"
                    paragraph_ +=  "${getTableRow3("<i><u>SWITCH</u></i>","<i><u>MOTION</u></i>","<i><u>CONTACTS</u></i>")}" 
                    num_switches = switches ?  switches.size() : 0;
                    num_motions = motions ?  motions.size() : 0;
                    num_contacts = contacts ?  contacts.size() : 0;
                    max_ = Math.max(num_switches, Math.max(num_motions, num_contacts));
                
                    log.debug ("Max = $max_, $num_switches, $num_motions, $num_contacts");
                    for (i=0; i<max_; i++){
                        switchText = i<num_switches?switches[i]:"";
                        motionText = i<num_motions?motions[i]:"";
                        contactText = i<num_contacts?contacts[i]:"";
                        paragraph_ += /${getTableRow3("$switchText", "$motionText", "$contactText")}/
                    }    
                    paragraph_ += "</table>"
                    paragraph paragraph_
                 
                if (graph_timespan){
                    
                
                    def timeString = "";
                    switch (graph_timespan){
                            case "1": timeString = "1 hour"; break;
                            case "2": timeString = "12 hours"; break;
                            case "3": timeString = "1 day"; break;
                            case "4": timeString = "3 days"; break;
                            case "5": timeString = "1 week"; break;
                    }
                    graph_update_rate = graph_update_rate ? graph_update_rate : 0
                    paragraph_ =  "<table>"
                    paragraph_ += "${getTableRow("<b><u>GRAPH SELECTIONS</b></u>", "<b><u>VALUE</b></u>", "<b><u>SIZE</b></u>", "<b><u>COLOR</b></u>")}"
                    paragraph_ += /${getTableRow("Timespan", timeString,"","")}/
                    paragraph_ += /${getTableRow("Update Rate", getTimeString(graph_update_rate), "", "")}/ 
                    if (graph_static_size==true){
                        paragraph_ += /${getTableRow("Graph Size", "$graph_h_size X $graph_v_size", "","")}/
                    } else {
                        paragraph_ += /${getTableRow("Graph Size", "DYNAMIC", "","")}/
                    }
                    paragraph_ += /${getTableRow("Axis", "", graph_axis_font, graph_axis_color)}/
                    paragraph_ += /${getTableRow("Background", "", "", graph_background_color)}/
                    paragraph_ += "</table>"
                    paragraph paragraph_
                } //graph_timespan
            }//else
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

def getTableRow3(col1, col2, col3){
     def html = "<tr><td width='30%'>$col1</td><td width='30%'>$col2</td><td width='40%'>$col3</td></tr>"  
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
    unsubscribe();
    app.updateLabel(app_name);
    state.dataName = attribute;
    if (switches) subscribe(switches, "switch",  eventHandler)
    if (contacts) subscribe(contacts, "contact", eventHandler)
    if (motions) subscribe(motions,  "motion",  eventHandler)  
    buildData();  
    
}

def getGraphDuration(){
    def mins;
    
    switch (graph_timespan){
               case "1": mins = 1 * 60; break;
               case "2": mins = 12 * 60; break;
               case "3": mins = 24 * 60; break;
               case "4": mins = 72 * 60; break;
               case "5": mins = 168 * 60; break;
               case "6": mins = 1; break;
    }
    
    return mins;
}

def getStartEventString(type) {
    switch (type){
         case "switch": return "on";
         case "motion": return "active";
         case "contact": return "open";
    }
}

def getEndEventString(type) {
    switch (type){
         case "switch": return "off";
         case "motion": return "inactive";
         case "contact": return "closed";
    }
}

def getCapType(evt) {
    switch (evt){
         case "on": return "switch";
         case "off": return "switch";
         case "active": return "motion";
         case "inactive": return "motion";
         case "open": return "contact";
         case "closed": return "contact";
    }
}

def clean() {
    def now = new Date();
    def min = new Date();
    
    def mins = getGraphDuration();
    
    use (groovy.time.TimeCategory) {
           min -= mins.minutes;
    }
    
    //clean our data
    state.eventList.eachWithIndex { it, index ->
        def newEvents = it.events_.findResults {
            //destroy events that actually go out of bounds
            if(it.end && Date.parse("yyyy-MM-dd HH:mm:ss.SSS", it.end) < min) return null;
            //trim events that start to go out of bounds
            else if(it.start && Date.parse("yyyy-MM-dd HH:mm:ss.SSS", it.start) < min) { return [start: min.format("yyyy-MM-dd HH:mm:ss.SSS"), end: it.end];}
            else return it;
        }
        
        state.eventList[index].events_ = newEvents;
    }
    
    
}

def eventHandler(evt){
    def device = evt.getDevice();
    def event = evt.getValue();
    def date = evt.date.format("yyyy-MM-dd HH:mm:ss.SSS");
    
    //log.debug("Got $device :: $event ");
    deviceEvents = state.eventList.find{ it.name_ == device.displayName }
    //log.debug("Pending Event is $deviceEvents.pending_event_");
    
    def type = getCapType(event);
    
    def start_event = getStartEventString(type);
    def end_event = getEndEventString(type);
    //reconstruct the corrosponding orphaned node if there is one
    def orphan_index = deviceEvents.events_.findIndexOf { !it.end }
    //if we have an orphan
    if(orphan_index != -1 && event.equals(end_event)) {
        deviceEvents.events_[orphan_index].end = date;
    }
    //else add an orphan
    else {
        if(event.equals(start_event)) deviceEvents.events_ << [ "start": date ];
        else deviceEvents.events_ << [ "end": date ];
    }
    
        //Waiting Ending Event
        /*if (deviceEvents.pending_event_){
            deviceEvents.events_.add([start: deviceEvents.pending_event_, end: getDateStringEvent(evt.date)]);
            deviceEvents.pending_event_ = null;
            dataCleanup();
        } else {
             deviceEvents.pending_event_ = getDateStringEvent(evt.date);
        }
    state.sensor_data << [name: evt.name, date: getDateStringEvent(evt.date), value: evt.value];
    //dataCleanup();*/
    clean();
}

def initialize() {
   updated();
}

private buildData(){
    state.eventList = []; 
    if (switches) buildDataCapability(switches, "switch");
    if (motions) buildDataCapability(motions, "motion");
    if (contacts) buildDataCapability(contacts, "contact");
}

private buildDataCapability(device_type, capability_) {
    def resp = []
    def now = new Date();
    def then = new Date();
    
    def mins = getGraphDuration();
    
    use (groovy.time.TimeCategory) {
           then -= mins.minutes;
    }
    
    def start_event = getStartEventString(capability_);
    def end_event = getEndEventString(capability_);
    
    log.debug("Initializing:: Capability = $capability_ from $then to $now");
    
    if(device_type) {
      device_type.each {it->  
          temp = it?.eventsBetween(then, now, [max: 500000])?.findAll{it.value == start_event || it.value == end_event}?.collect{[date: it.date.toString(), value: it.value]}         //resp.flatten(idx).sort{Date.parse("yyyy-MM-dd HH:mm:ss.SSS", it.date)};
          temp = temp.sort{Date.parse("yyyy-MM-dd HH:mm:ss.SSS", it.date)};
          
          //Final Parsing
          //firstTime = true;
          //temp_start = null;
          //place a short "start event to force the graph to the right range
          finalList = [];
          
          if(temp.size() > 0) {
              //if our first event is an end event, start at 1
              for(int i = temp[0].value == start_event ? 0 : 1; i < temp.size() - 1; i += 2) {
                  if(temp[i].date < temp[i + 1].date) finalList << [start: temp[i].date, end: temp[i + 1].date];
              }
          
              //add orphaned nodes
              if(temp[0].value != start_event) {
                  finalList.add(0, [end: temp[0].date]);
              } else if (temp[temp.size() - 1].value != end_event) {
                  finalList << [start: temp[temp.size() - 1].date];
              }
          }
          //if it's already on, add an event
          
          else if(it.currentState(capability_).value.equals(start_event)) {
              finalList << [start: then.format("yyyy-MM-dd HH:mm:ss.SSS")];
          }
          
          state.eventList += [name_:it.displayName, events_:finalList];
      }
      
   }
   
   
   resp
}

def dataCleanup(){
    def today = new Date();
    def then = getThen();
    def deviceEvents = state.eventList;
    def cont = true;
    
    deviceEvents.each{device->
        cont = true;
        i = device.events_.size()-1;
        while (cont){
            log.debug("start:: ${device.events_[i].start.toString()} end:: ${device.events_[i].end.toString()}");
            start = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", device.events_[i].start.toString());
            end = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", device.events_.end[i].toString());
            if (end.before(then)){
                device.events_.pop();
                i--;
            } else if (start.before(then)){
                device.events_[0].start = getDateStringEvent(then);
                cont = false;
            } else {
                cont = false
            }
            
        }
    }
}

//Get the HTML for refreshing the screen
def getRefreshHTML(){
    
    def html = /<meta http-equiv="refresh" content=/
    
    switch (graph_update_rate){
        case "1": html = ""; break;
        case "2": html += /"60">/; break;
        case "3": html += /"300">/; break;
        case "4": html += /"600">/; break;
        case "5": html += /"3600">/; break;
     }
        
     html
}

def getDataLabel(){
    def html = /{label: "$state.dataName", type: 'number'}],/
    
    html
}

def getChartOptions(){
    def options = [
        "width": graph_static_size ? graph_h_size : "100%",
        "height": graph_static_size ? graph_v_size: "100%",
        "timeline": [
            "rowLabelStyle": ["fontSize": graph_axis_font, "color": getColorCode(graph_axis_color)],
            "barLabelStyle": ["fontSize": graph_axis_font]
        ],
        "backgroundColor": getColorCode(graph_background_color),
    ]
    
    return options;
}
        
def preformatData(){
     state.eventList.each{
        
        it.events_.eachWithIndex{event, idx->
            
            if (idx%2 == 0) { html+= /['$it.name_', ${getDateString($event.date)},/ }
            else            { html+= /${getDateString($event.date)}]/ }
            
        }
     }
}
void removeLastChar(str) {
    str.subSequence(0, str.length() - 1)
}

def getTimeLine3() {
    def fullSizeStyle = "width: 100%; height: 100%; overflow: hidden";
    
    def html = """
    <html style="${fullSizeStyle}">
        <head>
            <script src="https://code.jquery.com/jquery-3.5.0.min.js" integrity="sha256-xNzN2a4ltkB44Mc/Jz3pT4iU1cmeR0FkXs4pru/JxaQ=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.0/moment.min.js" integrity="sha256-imB/oMaNA0YvIkDkF5mINRWpuFPEGVCEkHy6rm2lAzA=" crossorigin="anonymous"></script>
            <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
            <script type="text/javascript">
                google.charts.load('current', {'packages':['timeline']});
                google.charts.setOnLoadCallback(onLoad);
                
                let duration = ${getGraphDuration()};

                let options = [];
                let graphData = {};

                function getOptions() {
                    return jQuery.get("${state.localEndpointURL}getConfig/?access_token=${state.endpointSecret}", (data) => {
                        console.log("Got Options");
                        console.log(options);                        
                        options = data;
                    });
                }

                function getGraphData() {
                    return jQuery.get("${state.localEndpointURL}getData/?access_token=${state.endpointSecret}", (data) => {
                        console.log("Got Graph Data");
                        console.log(Object.assign({}, data));
                        graphData = data;
                        
                    });
                }

                async function update() {
                    try {
                        await getGraphData();
                    } finally {
                        let now = new Date();
                        let min = new Date();
                        min.setMinutes(min.getMinutes() - duration);

                        //boot old data
                        Object.entries(graphData).forEach(([name, arr]) => {
                            let newArr = arr.map(it => {
                                let itStartDate = it.start ? moment(it.start).toDate() : undefined;
                                let itEndDate = it.end ? moment(it.end).toDate() : undefined;
                                let ret = { ...it }

                                if(itEndDate && itEndDate < min) {
                                    ret = {};
                                }
                                else if(itStartDate && itStartDate < min) ret.start = min.getTime();
                                

                                return ret;
                            });

                            //delete non-existant nodes
                            newArr = newArr.filter(it => it.start || it.end);

                            //add endpoints for orphans
                            newArr = newArr.map((it) => {
                                if(!it.start) {
                                    return {...it, start: min.getTime() }
                                }
                                else if(!it.end) return {...it, end: now.getTime()}
                                return it;
                            });

                            //add endpoint buffers
                            if(newArr.length == 0) {
                                newArr.push({ start: min.getTime(), end: min.getTime() });
                                newArr.push({ start: now.getTime(), end: now.getTime() });
                            } else {
                                if(newArr[0].start != min.getTime()) newArr.push({ start: min.getTime(), end: min.getTime() });
                                if(newArr[newArr.length - 1].end != now.getTime()) newArr.push({ start: now.getTime(), end: now.getTime() });
                            }


                            graphData[name] = newArr;
                        });

                        console.log(graphData);

                        drawChart();
            
                        let updateRate = parseInt(${graph_update_rate});
                        //reschedule update
                        if(updateRate) {
                            setTimeout(() => {
                                update();
                            }, updateRate);
                        }
                    }
                }

                async function onLoad() {
                    //first load
                    await getOptions();

                    update();

                    //attach resize listener
                    window.addEventListener("resize", () => {
                        drawChart();
                    });
                }
                
                function drawChart() {
                    let dataTable = new google.visualization.DataTable();
                    dataTable.addColumn({ type: 'string', id: 'Device' });
                    dataTable.addColumn({ type: 'date', id: 'Start' });
                    dataTable.addColumn({ type: 'date', id: 'End' });

                    Object.entries(graphData).forEach(([name, arr]) => {
                        dataTable.addRows(arr.map((parsed) => [name, moment(parsed.start).toDate(), moment(parsed.end).toDate()]));
                    });

                    

                    let chart = new google.visualization.Timeline(document.getElementById("timeline"));
                    chart.draw(dataTable, options);
                }
            </script>
      </head>
      <body style="${fullSizeStyle}">
          <div id="timeline" style="${fullSizeStyle}"></div>
      </body>
    </html>
    """
    
return html;
}

// Create a formatted date object string for Google Charts Timeline
def getDateString(date) {
    def dateObj = Date.parse("yyyy-MM-dd HH:mm:ss.SSS", date.toString())
    //def dateObj = date
    def year = dateObj.getYear() + 1900
    def dateString = "new Date(${year}, ${dateObj.getMonth()}, ${dateObj.getDate()}, ${dateObj.getHours()}, ${dateObj.getMinutes()}, ${dateObj.getSeconds()})"
    dateString
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
            log.debug("Error: $e");
            state.endpoint = null
        }
    }
    return state.endpoint
}

def getColorCode(code){
    switch (code){
        case "Maroon":  ret = "#800000"; break;
        case "Red":	    ret = "#FF0000"; break;
        case "Orange":	ret = "#FFA500"; break;	
        case "Yellow":	ret = "#FFFF00"; break;	
        case "Olive":	ret = "#808000"; break;	
        case "Green":	ret = "#008000"; break;	
        case "Purple":	ret = "#800080"; break;	
        case "Fuchsia":	ret = "#FF00FF"; break;	
        case "Lime":	ret = "#00FF00"; break;	
        case "Teal":	ret = "#008080"; break;	
        case "Aqua":	ret = "#00FFFF"; break;	
        case "Blue":	ret = "#0000FF"; break;	
        case "Navy":	ret = "#000080"; break;	
        case "Black":	ret = "#000000"; break;	
        case "Gray":	ret = "#808080"; break;	
        case "Silver":	ret = "#C0C0C0"; break;	
        case "White":	ret = "#FFFFFF"; break;
        case "Transparent": ret = "transparent"; break;
    }
}

//oauth endpoints
def getGraph() {
    render(contentType: "text/html", data: getTimeLine3());      
}

def getData() {
    def timeline = state.sensor_data;
    
    def formatEvents = [:];
    
    state.eventList.each{device->
        def deviceName = device.name_.replaceAll("[^a-zA-Z0-9 ]", "");
        formatEvents[deviceName] = [];
        device.events_.each{event->
            formatEvents[deviceName] << ["start": event.start, "end": event.end];
        }
    }
        
    return render(contentType: "text/json", data: JsonOutput.toJson(formatEvents));
    
   
}

def getConfig() {
    render(contentType: "text/json", data: JsonOutput.toJson(getChartOptions()));
}
