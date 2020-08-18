/**
 *  Hubigraph Radar Tile Child App
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
 *  Utilizes the Windy.com embedded map ability
 */

// Hubigraph Gauge Change Log
// V 0.1 Intial release
// V 1.0 Released (not Beta) Cleanup and Preview Enabled
// V 1.5 Ordering, Color and Common API Update
// V 1.8 Smoother sliders, bug fixes


import groovy.json.JsonOutput
import java.text.DecimalFormat;

def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v0.22" }

definition(
    name: "Hubigraph Radar Tile",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Radar Tile",
    category: "",
    parent: "tchoward:Hubigraphs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
    section ("test"){
       page(name: "mainPage", install: true, uninstall: true)
       page(name: "tileSetupPage", nextPage: "mainPage")
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
}

def call(Closure code) {
    code.setResolveStrategy(Closure.DELEGATE_ONLY);
    code.setDelegate(this);
    code.call();
}

def tileSetupPage() {
    def supported_attrs;
    
    zoomEnum =    [[3:"3"], [4: "4"], [5: "5"], [6: "6"], [7: "7"], [8: "8"], [9: "9"], [10: "10"]];
    refreshEnum = [[60000:"1 minute"], [300000: "5 minutes"], [600000: "10 minutes"], [1200000: "20 minutes"], [1800000: "30 minutes"], [3600000: "1 hour"]];
    
        
    weatherMapEnum = [["radar" :    "Current Radar"], 
                      ["temp" :     "Temperature"],
                      ["wind"  :    "Wind"],   
                      ["rain"  :    "Rain and Thunder"],
                      ["rainAccu" : "Rain Accumulation"],
                      ["snowAccu" : "Snow Accumulation"],
                      ["snowcover": "Snow Ground Cover"]];
    
    forecastModelEnum =[["ecmwf":    "European Centre for Medium-Range Weather Forecasts"],
                        ["gfs":      "Global Forecast System"]];
    
    hoursModelEnum = [["now" : "Current"],
                      ["12"  : "12 Hours"],
                      ["24"  : "24 Hours"]];  
    
    measureEnum =   [["in": "inches"],
                     ["mm": "millimeters"]];
    
    windEnum =  [["knot" : "Knots (k)"],
                 ["meters_per_second" : "Meters / Second (m/s)"],
                 ["kilometers_per_hour" : "Kilometers / Hour (km/h)"],
                 ["miles_per_hour" : "Miles per Hour (mph)"]];
    
    tempEnum =    [["fahrenheit": "Fahrenheit (°F)"], 
                   ["celsius" : "Celsius (°C)"]];

    
                         
                         
                         
                     

       
    dynamicPage(name: "tileSetupPage") {
        
        def location = getLocation();
        parent.hubiForm_section(this,"Tile Setup", 1){	    
            container = [];
            container << parent.hubiForm_text_input (this, "<b>Latitude (Default = Hub location)</b>", "latitude", location.latitude, false);
            container << parent.hubiForm_text_input (this, "<b>Longitude (Default = Hub location)</b>", "longitude", location.longitude, false);
            
            parent.hubiForm_container(this, container, 1);
            
            if (!overlay) overlay = "radar";
            
            input( type: "enum", name: "zoom", title: "<b>Zoom Amount</b>", required: false, multiple: false, options: zoomEnum, defaultValue: 3, submitOnChange: false)
            input( type: "enum", name: "refresh", title: "<b>Refresh Time</b>", required: false, multiple: false, options: refreshEnum, defaultValue: 600000, submitOnChange: false)
            input( type: "enum", name: "overlay", title: "<b>Map Type</b>", required: false, multiple: false, options: weatherMapEnum, defaultValue: "radar", submitOnChange: true)
            
            if (overlay != "radar") {
                container = [];
                container << parent.hubiForm_text(this, """<b>You have chosen a forecast map.</b> Please note:<br>
                                                              1. Forecast maps are update on the hour<br>
                                                              2. "Current" is the current condition (within the last hour)<br>
                                                              3. Refreshing these maps "more often" won't change anything""");
                parent.hubiForm_container(this, container, 1);     

                if (product == "radar") app.updateSetting("product", [type: "enum", value: "gfs"]);
                input( type: "enum", name: "product", title: "<b>Forecast Model</b>", required: false, multiple: false, options: forecastModelEnum, defaultValue: "gfs", submitOnChange: false);
                input( type: "enum", name: "calendar", title: "<b>Display Time</b>", required: false, multiple: false, options: hoursModelEnum, defaultValue: "now", submitOnChange: false);
            } else {
                app.updateSetting ("product", [type: "enum", value: "radar"]);   
                app.updateSetting ("calendar", [type: "enum", value: "now"]); 
            }
            
            input( type: "enum", name: "wind_units", title: "<b>Wind Speed Units</b>", required: false, multiple: false, options: windEnum, defaultValue: "miles_per_hour", submitOnChange: false);
            input( type: "enum", name: "temp_units", title: "<b>Temperature Units</b>", required: false, multiple: false, options: tempEnum, defaultValue: "farenheit", submitOnChange: false);
            container = [];
            container << parent.hubiForm_switch(this, title: "<b>Show Marker on Graph?</b>", 
                                                      name: "marker", 
                                                      default: false, 
                                                      submit_on_change: false);
             

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
            
           parent.hubiForm_container(this, container, 1);     


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
                    container << parent.hubiForm_page_button(this, "Setup Tile", "tileSetupPage", "100%", "vibration");                  
                    parent.hubiForm_container(this, container, 1); 
                }
                
                parent.hubiForm_section(this, "Local Graph URL", 1, "link"){
                    container = [];
                    container << parent.hubiForm_text(this, "${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
                    
                    parent.hubiForm_container(this, container, 1); 
                }
                
//                if (gauge_title){
                     parent.hubiForm_section(this, "Preview", 10, "show_chart"){                         
                         container = [];
                         container << parent.hubiForm_graph_preview(this)             
                         parent.hubiForm_container(this, container, 1); 
                     } //graph_timespan
            
                    parent.hubiForm_section(this, "Hubigraph Tile Installation", 2, "apps"){
                        container = [];
                             
                        container << parent.hubiForm_switch(this, title: "Install Hubigraph Tile Device?", name: "install_device", default: false, submit_on_change: true);
                        if (install_device==true){ 
                             container << parent.hubiForm_text_input(this, "Name for HubiGraph Tile Device", "device_name", "Hubigraph Radar Tile", "false");
                        }
                        parent.hubiForm_container(this, container, 1); 
                    }
//                } 
             
            
               if (state.endpoint){
                   parent.hubiForm_section(this, "Hubigraph Application", 1, "settings"){
                        container = [];
                        container << parent.hubiForm_sub_section(this, "Application Name");
                        container << parent.hubiForm_text_input(this, "Rename the Application?", "app_name", "Hubigraph Radar", "false");
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



def installed() {
    log.debug "Installed with settings: ${settings}"
    updated();
}

def uninstalled() {
    if (state.endpoint) {
        try {
            log.debug "Revoking API access token"
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
    state.dataName = attribute;
    
     if (install_device == true){
        parent.hubiTool_create_tile(this);    
    }
}

def buildData() {
    return extractNumber(sensor_.currentState(attribute_).getStringValue());
    
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

def getHTML(){
    
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    def wind = "kt";
    switch (wind_units){
         case "knot" : wind = "kt"; break; 
         case "meters_per_second" : wind = "m%2Fs"; break;
         case "kilometers_per_hour" :  wind = "km%2Fh"; break;
         case "miles_per_hour" :  wind = "mph"; break;   
    }
    
    def temp = "%C2%B0F";
    switch (temp_units){
        case "farenheit": temp = "%C2%B0F";
        case "celsius" : temp = "%C2%B0C"
    }
    def html = """

<style>
  .wrapper {
    display: flex;
    flex-flow: column;
    height: 100%;
    background-color: ${getRGBA(background_color, background_opacity)};
  }

</style>

<div class="wrapper" id="radar">
  <iframe id="windy2" style="position: absolute !important;z-index: 2;" src="" data-fs="false">
  </iframe>

  <iframe id="windy" style="position: absolute !important; z-index: 3;" src="" onload="(() => {         
       const NAME = 'once';
       var frameRefreshInterval;
       var count = 0;
       
       if (this.name !== NAME) {
         console.log('START')
         this.name = NAME
         frameRefreshInterval = setInterval(refreshFrame, ${refresh});
       }
       
       function refreshFrame() {  
          console.log('Refresh'+count);
          
          document.getElementById('windy2').style.visibility = 'visible';
          //this.style.visibilty = 'hidden'; 
          document.getElementById('windy').style.zIndex = 1; 
          document.getElementById('windy').src = document.getElementById('windy').src
          count++;
        }    
        
       setTimeout(() => { document.getElementById('windy').style.zIndex = 3; }, 1000);
       setTimeout(() => { document.getElementById('windy2').src = document.getElementById('windy2').src }, 2000);
       
     
     })()"></iframe>
</div>

<script>

var url = "https://embed.windy.com/embed2.html";
var width = document.getElementById('radar').offsetWidth-5;
var height = window.innerHeight-15;

var params = "?lat=${latitude}&lon=${longitude}&detailLat=${latitude}&detailLon=${longitude}&width="+width+"&height"+height+"&zoom=${zoom}&level=surface&overlay=${overlay}&product=${product}&menu=&message=true&marker=${marker==true ? 'true' : ''}&calendar=${calendar}&pressure=&type=map&location=coordinates&detail=&metricWind=${wind}&metricTemp=${temp}&radarRange=-1"

var iframe_url = url + params;

console.log(iframe_url);

document.getElementById("windy").src = iframe_url;
document.getElementById("windy2").src = iframe_url;
document.getElementById("windy").width = width+"px";
document.getElementById("windy2").width = width+"px";
document.getElementById("windy").height = height+"px";
document.getElementById("windy2").height = height+"px";


</script>

        """
    return html;
}

def getRadar() {
    
    def html = getHTML();
    
    return html;
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

//oauth endpoints
def getGraph() {
    return render(contentType: "text/html", data: getRadar());      
}

