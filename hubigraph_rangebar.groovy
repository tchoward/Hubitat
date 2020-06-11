/**
 *  Hubigraph RangeBar Child App
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

// Hubigraph RangeBar Changelog
// V 0.1 Intial release

import groovy.json.JsonOutput

def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v0.22" }

definition(
    name: "Hubigraph Range Bar",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Range Bar",
    category: "",
    parent: "tchoward:Hubigraphs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
    section ("test"){
       page(name: "mainPage", install: true, uninstall: true)
       page(name: "deviceSelectionPage", nextPage: "attributeConfigurationPage")
       page(name: "attributeConfigurationPage", nextPage: "mainPage")
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

def getAttributeType(attrib, title){
    
    switch (attrib){
         case "motion":         return ["motion", "Motion (active/inactive)"];
         case "switch":         return ["switch", "Switch (on/off)"];
         case "contact":        return ["contact", "Contact (open/close)"];
         case "acceleration":   return ["acceleration", "Acceleration (active/inactive)"]
         case "audioVolume":
         case "number": return [title, "Number (Choose threshold)"];  
    }
}

def deviceSelectionPage() {                  
    dynamicPage(name: "deviceSelectionPage") {
        specialSection("Device Selection", 1){
            input (type: "capability.*", name: "sensors", title: "Choose Sensors", multiple: true, submitOnChange: true)
            if (sensors) {
                def all = (1..sensors.size()).collect{ "" + it };
                validateOrder(all);
                sensors.eachWithIndex {sensor, idx ->
                    id = sensor.id;
                    sensor_attributes = sensor.getSupportedAttributes().collect { it.getName() };      
                    paragraph getSubTitle(sensor.displayName);
                    input( type: "enum", name: "attributes_${id}", title: "Attributes to graph", required: true, multiple: true, options: sensor_attributes, defaultValue: "1", submitOnChange: false )
                    input( type: "enum", name: "displayOrder_${id}", title: "Order to Display on Timeline", required: true, multiple:false, options: all, defaultValue: idx, submitOnChange: true);
                }
            }
        }
    }

}

def validateOrder(all) {
    def order = [];
    sensors.eachWithIndex {sensor, idx ->
        order << settings["displayOrder_${sensor.id}"];
    }
    
    //if we are initialized and need to check
    if(state.lastOrder && state.lastOrder[0]) {
        def remains = all.findAll { !order.contains(it) }
    
        def dupes = [];
        
        order.each {it ->
            if(order.count(it) > 1) dupes << it;
        }
        
        sensors.eachWithIndex {sensor, idx ->
            if(state.lastOrder[idx] == order[idx] && dupes.contains(settings["displayOrder_${sensor.id}"])) {
                settings["displayOrder_${sensor.id}"] = remains[0];
                app.updateSetting("displayOrder_${sensor.id}", [value: remains[0], type: "enum"]);
                remains.removeAt(0);
            }
        }
    }
    
    //reconstruct order
    order = [];
    sensors.eachWithIndex {sensor, idx ->
        order << settings["displayOrder_${sensor.id}"];
    }
    
    state.lastOrder = order;
}

def attributeConfigurationPage() {
                           
    state.count_ = 0;     
    dynamicPage(name: "attributeConfigurationPage") {
         specialSection("Directions", 1, "directions"){
            paragraph """Choose Numeric Attributes Only."""
         }
         cnt = 1;
         sensors.each { sensor ->
             def attributes = settings["attributes_${sensor.id}"];
             attributes.each { attribute ->
                 state.count_++;
                 specialSection("${sensor.displayName} - ${attribute}", 1, "direction"){
                        input( type: "string", name: "graph_name_override_${sensor.id}_${attribute}", title: "<b>Override Device Name</b><small></i><br>Use %deviceName% for DEVICE and %attributeName% for ATTRIBUTE</i></small>", defaultValue: "%deviceName%: %attributeName%");
                        colorSelector("attribute_${sensor.id}_${attribute}_background", "Background Color", "#3e4475", false);
                        colorSelector("attribute_${sensor.id}_${attribute}_minmax", "Min/Max Color", "#607c91", false);
                        colorSelector("attribute_${sensor.id}_${attribute}_current", "Current Value Color", "#8eb6d4", false);
                        input( type: "bool", name: "attribute_${sensor.id}_${attribute}_showAnnotation", title: "Show Current Value on Bar?", defaultValue: false,  submitOnChange: true);
                        if (settings["attribute_${sensor.id}_${attribute}_showAnnotation"]==true){
                            fontSizeSelector("attribute_${sensor.id}_${attribute}_annotation", "Current Value", 16, 2, 30);
                            input( type: "string", name: "attribute_${sensor.id}_${attribute}_annotation_units", title: "Units", defaultValue: "");
                        }
                        
                 }
                 cnt += 1;
               }
            }
     }
}

def graphSetupPage(){
    
    def rateEnum = [["-1":"Never"], ["0":"Real Time"], ["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], 
                    ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]];
    
    def timespanEnum = [[0:"Live"], [1:"Hourly"], [2:"Daily"], [3:"Every Three Days"], [4:"Weekly"]];
    
    dynamicPage(name: "graphSetupPage") {
        specialSection("General Options", 1){
            input( type: "enum", name: "graph_update_rate", title: "<b>Select graph update rate</b>", multiple: false, required: false, options: rateEnum, defaultValue: "0")
            input( type: "enum", name: "graph_timespan", title: "<b>Select Timespan to Graph (i.e How Often to Reset Range)</b>", multiple: false, required: false, options: timespanEnum, defaultValue: "2", , submitOnChange: true)     
            colorSelector("graph_background", "Background", "White", false);
            
            input( type: "number", name: "graph_bar_percent", title: "<b>Graph Bar Width (1-100)</b>", defaultValue: "90", range: "1..100");
            input( type: "number", name: "graph_min", title: "<b>Graph Min</b>", defaultValue: "");
            input( type: "number", name: "graph_max", title: "<b>Graph Max</b>", defaultValue: "");
        }
        specialSection("Horizontal Axes", 1){
            colorSelector("haxis", "Horizontal Axis", "White", false);
            fontSizeSelector("haxis", "Horizontal Axis", 9, 2, 20);
        }
        specialSection("Device Axes", 1){
            fontSizeSelector("graph_axis", "Device Name", 9, 2, 20);
            colorSelector("graph_axis", "Device Name", "Black", false);
            input( type: "number", name: "graph_v_buffer", title: "<b>Number of Pixels for Device Names</b>", defaultValue: "100", range: "10..500");
        }
        specialSection("Graph Size", 1){
            //Size
            input( type: "bool", name: "graph_static_size", title: "<b>Set size of Graph?</b><br><small>(False = Fill Window)</small>", defaultValue: false, submitOnChange: true);
            if (graph_static_size==true){
                input( type: "number", name: "graph_h_size", title: "<b>Horizontal dimension of the graph</b>", defaultValue: "800", range: "100..3000");
                input( type: "number", name: "graph_v_size", title: "<b>Vertical dimension of the graph</b>", defaultValue: "600", range: "100..3000");
            }
        }
        specialSection("Annotations", 1){
            fontSizeSelector("annotation", "Annotation", 16, 2, 40);
            colorSelector("annotation", "Annotation", "#FFFFFF", false);
            colorSelector("annotation_stem", "Annotation Stem", "#FFFFFF", false);
            colorSelector("annotation_aura", "Annotation Aura", "#000000", false);
            input( type: "bool", name: "annotation_bold", title: "<b>Bold Annotation</b>", defaultValue: false);
            input( type: "bool", name: "annotation_italics", title: "<b><i>Italic</i> Annotation</b>", defaultValue: false);
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

def mainPage() {
   
    
    dynamicPage(name: "mainPage") {        
       
            if (!state.endpoint) {
                specialSection("Please set up OAuth API", 1, "report"){
                    href name: "enableAPIPageLink", title: "Enable API", description: "", page: "enableAPIPage"    
                 }    
            } else {
               specialSection("Graph Options", 1, "tune"){
                    objects = [];
                    objects << specialPageButton("Select Device/Data", "deviceSelectionPage", "100%", "vibration");
                    objects << specialPageButton("Configure Graph", "graphSetupPage", "100%", "poll");
                    addContainer(objects, 1);
                }
                specialSection("Local Graph URL", 1, "link"){
                    addContainer(["${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"], 1);
                }
                
                if (graph_timespan){
                     specialSection("Preview", 10, "show_chart"){
                      paragraph graphPreview()
                } //graph_timespan
            
                    specialSection("Hubigraph Tile Installation", 2, "apps"){
                        objects = [];
                        objects << specialSwitch("Install Hubigraph Tile Device?", "install_device", false, true);
                        if (install_device==true){ 
                             objects << specialTextInput("Name for HubiGraph Tile Device", "device_name", "false");
                        }
                        addContainer(objects, 1);
                    }
                } 
             
                
               if (state.endpoint){
                   specialSection("Hubigraph Application", 1, "settings"){
            
                        paragraph getSubTitle("Application Name");
                        addContainer([specialTextInput("Rename the Application?", "app_name", "false")], 1);
                        paragraph getSubTitle("Debugging");
                        addContainer([specialSwitch("Enable Debug Logging?", "debug", false, false)], 1);
                
                        paragraph getSubTitle("Disable Oauth Authorization");
                        addContainer([specialPageButton("Disable API", "disableAPIPage", "100%", "cancel")], 1);  
                    }
               }
       
            } //else 
        
    } //dynamicPage
}


/********************************************************************************************************************************
*********************************************************************************************************************************
****************************************** NEW FORM FUNCTIONS********************************************************************
*********************************************************************************************************************************
*********************************************************************************************************************************/




def addContainer(containers, numPerRow){
    
    def html_ = """
            <div class = "mdl-grid" style="margin: 0; padding: 0;"> 
    """
    containers.each{container->
        html_ += """<div class="mdl-cell mdl-cell--${12/numPerRow}-col-desktop mdl-cell--${8/numPerRow}-col-tablet mdl-cell--${4/numPerRow}-col-phone">"""
        html_ += container;
        html_ += """</div>"""
    }
    html_ += """</div>"""
         
    paragraph (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
    
}

def addText(text){
    
    def html_ = "$text";
    
    return html_
}

def specialPageButton(title, page, width, icon){
    def html_ = """
        <button type="button" name="_action_href_${page}|${page}|1" class="btn btn-default btn-lg btn-block hrefElem  mdl-button--raised mdl-shadow--2dp mdl-button__icon" style="text-align:left;width:${width}; margin: 0;">
            <span style="text-align:left;white-space:pre-wrap">
                ${title}
            </span>
            <ul class="nav nav-pills pull-right">
                <li><i class="material-icons">${icon}</i></li>
            </ul>
            <br>
            <span class="state-incomplete-text " style="text-align: left; white-space:pre-wrap"></span>
       </button>
    """.replace('\t', '').replace('\n', '').replace('  ', '');
    
    return html_;
}

def specialSection(String name, pos, icon="", Closure code) {
    def id = name.replace(' ', '_');
    //def icon = "vibration";
    
    def titleHTML = """
        <div class="mdl-layout__header" style="display: block; background:#033673; margin: 0 -16px; width: calc(100% + 32px); position: relative; z-index: ${pos}; overflow: visible;">          
            <div class="mdl-layout__header-row">
                <span class="mdl-layout__title" style="margin-left: -32px; font-size: 20px; width: auto;">
                        ${name}
                </span>
                <div class="mdl-layout-spacer"></div>
                <ul class="nav nav-pills pull-right">
                        <li> <i class="material-icons">${icon}</i></li>
                </ul>
             </div> 
        </div>
    """;
    
    def modContent = """
    <div id=${id} style="display: none;"></div>
    <script>
        var sectionElem = jQuery('#${id}').parent();
        
        /*hide default header*/
        sectionElem.css('display', 'none');
        sectionElem.css('z-index', ${pos});

        var elem = sectionElem.parent().parent();
        elem.addClass('mdl-card mdl-card-wide mdl-shadow--8dp');
        elem.css('width', '100%');
        elem.css('padding', '0 16px');
        elem.css('display', 'block');
        elem.css('min-height', 0);
        elem.css('position', 'relative');
        elem.css('z-index', ${pos});
        elem.css('overflow', 'visible');
        elem.prepend('${titleHTML}');
    </script>
    """;
    
    modContent = modContent.replace('\t', '').replace('\n', '').replace('  ', '');
    
    section(modContent) {
        code.call();
    }
}

def specialSwitch(title, var, defaultVal, submitOnChange){
   
    def actualVal = settings[var] != null ? settings[var] : defaultVal;
    
   def html_ = """      
                    <div class="form-group">
                        <input type="hidden" name="${var}.type" value="bool">
                        <input type="hidden" name="${var}.multiple" value="false">
                    </div>
                    <label for="settings[${var}]"
                        class="mdl-switch mdl-js-switch mdl-js-ripple-effect mdl-js-ripple-effect--ignore-events is-upgraded ${actualVal ? "is-checked" : ""}  
                            data-upgraded=",MaterialSwitch,MaterialRipple">
                            <input name="checkbox[${var}]" id="settings[${var}]" class="mdl-switch__input 
                                ${submitOnChange ? "submitOnChange" : ""} "
                                    type="checkbox" 
                                ${actualVal ? "checked" : ""}>                
                            <div class="mdl-switch__label">${title}</div>    
                            <div class="mdl-switch__track"></div>
                            <div class="mdl-switch__thumb">
                                <span class="mdl-switch__focus-helper">
                                </span>
                            </div>
                            <span class="mdl-switch__ripple-container mdl-js-ripple-effect mdl-ripple--center" data-upgraded=",MaterialRipple">
                                <span class="mdl-ripple">
                                </span>
                            </span>
                    </label>
                    <input name="settings[${var}]" type="hidden" value="${actualVal}">
    """
    return html_.replace('\t', '').replace('\n', '').replace('  ', '');;
 }

def specialTextInput(title, var, submitOnChange){
     def html_ = """
        <div class="form-group">
            <input type="hidden" name="${var}.type" value="text">
            <input type="hidden" name="${var}.multiple" value="false">
        </div>
        <label for="settings[${var}]" class="control-label">
            <b>${title}</b>
        </label>
            <input type="text" name="settings[${var}]" 
                   class="mdl-textfield__input ${submitOnChange == "true" ? "submitOnChange" : ""} " 
                   value="${settings[var]}" placeholder="Click to set" id="settings[${var}]">
        """
     return html_.replace('\t', '').replace('\n', '').replace('  ', '');
}

def fontSizeSelector(varname, label, defaultSize, min, max){
    
    def fontSize;
    def varFontSize = "${varname}_font"
    
    settings[varFontSize] = settings[varFontSize] ? settings[varFontSize] : defaultSize;
    
    def html = "";
    
    html += 
    """
    <table style="width:100%">
    <tr><td><label for="settings[${varFontSize}]" class="control-label"><b>${label} Font Size</b></td>
        <td style="text-align:right; font-size:${settings[varFontSize]}px">Font Size: ${settings[varFontSize]}</td>
        </label>
    </tr>
    </table>
    <input type="range" min = "$min" max = "$max" name="settings[${varFontSize}]" class="mdl-slider submitOnChange " value="${settings[varFontSize]}" id="settings[${varFontSize}]">
    <div class="form-group">
        <input type="hidden" name="${varFontSize}.type" value="number">
        <input type="hidden" name="${varFontSize}.multiple" value="false">
    </div>
    """.replace('\t', '').replace('\n', '').replace('  ', '');
    
    paragraph html
    
    //input (type: "range", name: varFontSize, title: "${label}:<p style='font-size:${settings["$varFontSize"]}px'>Font Size: ${settings["$varFontSize"]}</p>", min: "2", max: "20", submitOnChange: true);
    
}

def colorSelector(varname, label, defaultColorValue, defaultTransparentValue){
    def html = ""
    def varnameColor = "${varname}_color";
    def varnameTransparent = "${varname}_color_transparent"
    def colorTitle = "<b>${label} Color</b>"
    def notTransparentTitle = "Transparent";
    def transparentTitle = "${label}: Transparent"
    
    settings[varnameColor] = settings[varnameColor] ? settings[varnameColor]: defaultColorValue;
    settings[varnameTransparent] = settings[varnameTransparent] ? settings[varnameTransparent]: defaultTransparentValue;
    
    def isTransparent = settings[varnameTransparent];
    
    html += 
    """
    <div style="display: flex; flex-flow: row wrap;">
        <div style="display: flex; flex-flow: row nowrap; flex-basis: 100%;">
            ${!isTransparent ? """<label for="settings[${varnameColor}]" class="control-label" style="flex-grow: 1">${colorTitle}</label>""" : """"""}
            <label for="settings[${varnameTransparent}]" class="control-label" style="width: auto;">${isTransparent ? transparentTitle: notTransparentTitle}</label>
        </div>
        ${!isTransparent ? """
            <div style="flex-grow: 1; flex-basis: 1px; padding-right: 8px;">
                <input type="color" name="settings[${varnameColor}]" class="mdl-textfield__input" value="${settings[varnameColor] ? settings[varnameColor] : defaultColorValue}" placeholder="Click to set" id="settings[${varnameColor}]" list="presetColors">
                  <datalist id="presetColors">
                    <option>#800000</option>
                    <option>#FF0000</option>
                    <option>#FFA500</option>
                    <option>#FFFF00</option>

                    <option>#808000</option>
                    <option>#008000</option>
                    <option>#00FF00</option>
                    
                    <option>#800080</option>
                    <option>#FF00FF</option>
                    
                    <option>#000080</option>
                    <option>#0000FF</option>
                    <option>#00FFFF</option>

                    <option>#FFFFFF</option>
                    <option>#C0C0C0</option>
                    <option>#000000</option>
                  </datalist>
            </div>
        """ : ""}
        <div class="submitOnChange">
            <input name="checkbox[${varnameTransparent}]" id="settings[${varnameTransparent}]" style="width: 27.6px; height: 27.6px;" type="checkbox" onmousedown="((e) => { jQuery('#${varnameTransparent}').val('${!isTransparent}'); })()" ${isTransparent ? 'checked' : ''} />
            <input id="${varnameTransparent}" name="settings[${varnameTransparent}]" type="hidden" value="${isTransparent}" />
        </div>
        <div class="form-group">
            <input type="hidden" name="${varnameColor}.type" value="color">
            <input type="hidden" name="${varnameColor}.multiple" value="false">

            <input type="hidden" name="${varnameTransparent}.type" value="bool">
            <input type="hidden" name="${varnameTransparent}.multiple" value="false">
        </div>
    </div>
    """.replace('\t', '').replace('\n', '').replace('  ', '');
    
    paragraph html;
}

def graphPreview(){
  def html = ""
    if (!state.count_) state.count_ = 5;
    html+= """<iframe id="preview" style="width: 100%; position: relative; z-index: 1; height: 100%; background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAEq2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIKICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIKICAgIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIgogICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgZXhpZjpQaXhlbFhEaW1lbnNpb249IjIiCiAgIGV4aWY6UGl4ZWxZRGltZW5zaW9uPSIyIgogICBleGlmOkNvbG9yU3BhY2U9IjEiCiAgIHRpZmY6SW1hZ2VXaWR0aD0iMiIKICAgdGlmZjpJbWFnZUxlbmd0aD0iMiIKICAgdGlmZjpSZXNvbHV0aW9uVW5pdD0iMiIKICAgdGlmZjpYUmVzb2x1dGlvbj0iNzIuMCIKICAgdGlmZjpZUmVzb2x1dGlvbj0iNzIuMCIKICAgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIKICAgcGhvdG9zaG9wOklDQ1Byb2ZpbGU9InNSR0IgSUVDNjE5NjYtMi4xIgogICB4bXA6TW9kaWZ5RGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIKICAgeG1wOk1ldGFkYXRhRGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCI+CiAgIDx4bXBNTTpIaXN0b3J5PgogICAgPHJkZjpTZXE+CiAgICAgPHJkZjpsaQogICAgICBzdEV2dDphY3Rpb249InByb2R1Y2VkIgogICAgICBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZmZpbml0eSBQaG90byAxLjguMyIKICAgICAgc3RFdnQ6d2hlbj0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIvPgogICAgPC9yZGY6U2VxPgogICA8L3htcE1NOkhpc3Rvcnk+CiAgPC9yZGY6RGVzY3JpcHRpb24+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8+IC4TuwAAAYRpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZE7SwNBFEaPiRrxQQQFLSyCRiuVGEG0sUjwBWqRRPDVbDYvIYnLboIEW8E2oCDa+Cr0F2grWAuCoghiZWGtaKOy3k2EBIkzzL2Hb+ZeZr4BWyippoxqD6TSGT0w4XPNLyy6HM/UYqONfroU1dBmguMh/h0fd1RZ+abP6vX/uYqjIRI1VKiqEx5VNT0jPCk8vZbRLN4WblUTSkT4VLhXlwsK31p6uMgvFseL/GWxHgr4wdYs7IqXcbiM1YSeEpaX404ls+rvfayXNEbTc0HJnbI6MAgwgQ8XU4zhZ4gBRiQO0YdXHBoQ7yrXewr1s6xKrSpRI4fOCnESZOgVNSvdo5JjokdlJslZ/v/11YgNeovdG31Q82Sab93g2ILvvGl+Hprm9xHYH+EiXapfPYDhd9HzJc29D84NOLssaeEdON+E9gdN0ZWCZJdli8Xg9QSaFqDlGuqXip797nN8D6F1+aor2N2DHjnvXP4Bhcln9Ef7rWMAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAXSURBVAiZY7hw4cL///8Z////f/HiRQBMEQrfQiLDpgAAAABJRU5ErkJggg=='); background-size: 25px; background-repeat: repeat; image-rendering: pixelated;" src="${state.localEndpointURL}graph/?access_token=${state.endpointSecret}" data-fullscreen="false" onload="(() => {
          this.handel = -1;
          const thisFrame = this;
          const body = thisFrame.contentDocument.body;
          const start = () => {
              if(thisFrame.dataset.fullscreen == 'false') {
                thisFrame.style = 'position:fixed !important; z-index: 100; height: 100%; width: 100%; top: 60px; left: 0; overflow:visible;';
                thisFrame.dataset.fullscreen = 'true';
              } else {
                thisFrame.style = 'position:relative; top: 0; z-index: 1; left: 0; overflow:hidden; opacity: 1.0;';
                const box = jQuery('#preview').parent()[0].getBoundingClientRect();
                const h = 35 * ${state.count_.floatValue()} + 30;
                const w = box.width * 1.00;
                jQuery('#preview').css('height', h);
                jQuery('#preview').css('width', w);
                thisFrame.dataset.fullscreen = 'false';
              }
          }
          body.addEventListener('dblclick', start);
    })()""></iframe>
    <script>
    function resize() {
        const box = jQuery('#preview').parent()[0].getBoundingClientRect();
        const h = 100 * ${state.count_.floatValue()} + 30;
        const w = box.width * 1.00;
        jQuery('#preview').css('height', h);
        jQuery('#preview').css('width', w);
    }
    resize();
    jQuery(window).on('resize', () => {
        resize();
    });
    </script><small> *Double-click to Toggle Full-Screen </small>
"""
    
return html;
}

def getSubTitle(myText=""){
    def html = """
        <div class="mdl-layout__header" style="display: block; min-height: 0;">
        <div class="mdl-layout__header-row" style="height: 48px;">
        <span class="mdl-layout__title" style="margin-left: -32px; font-size: 9px; width: auto;">
                   <h5 style="font-size: 16px;">${myText}</h5>
        </span>
        </div>
        </div>
    """.replace('\t', '').replace('\n', '').replace('  ', '');
                
    return html
}

def logDebug(str){
    if (debug==true){
        log.debug(str);   
    }
}

def createHubiGraphTile() {
	log.info "Creating HubiGraph Child Device"
    
    def childDevice = getChildDevice("HUBIGRAPH_${app.id}");     
    logDebug(childDevice);
   
    if (!childDevice) {
        if (!device_name) device_name="Dummy Device";
        logDebug("Creating Device $device_name");
    	childDevice = addChildDevice("tchoward", "Hubigraph Tile Device", "HUBIGRAPH_${app.id}", null,[completedSetup: true, label: device_name]) 
        log.info "Created HTTP Switch [${childDevice}]"
        
        //Send the html automatically
        childDevice.setGraph("${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
        log.info "Sent setGraph: ${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"
	}
    else {
    	
        childDevice.label = device_name;
        log.info "Label Updated to [${device_name}]"
        
        //Send the html automatically
        childDevice.setGraph("${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
        log.info "Sent setGraph: ${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"
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

def getTitle(myText=""){
    def html = "<div class='row-full' style='background-color:#1A77C9;color:white;font-weight: bold; text-align: center; font-size: 20px '>"
    html += "${myText}</div>"
    html
}

/********************************************************************************************************************************************
*********************************************************************************************************************************************
***************************************************  END HELPER FUNCTIONS  ******************************************************************
*********************************************************************************************************************************************
*********************************************************************************************************************************************/

def getTableRow3(col1, col2, col3){
     def html = "<tr><td width='30%'>$col1</td><td width='30%'>$col2</td><td width='40%'>$col3</td></tr>"  
     html
}



def installed() {
    log.debug "Installed with settings: ${settings}"
    updated();
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
    removeChildDevices(getChildDevices());
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}


def updated() {
    app.updateLabel(app_name);
    state.dataName = attribute;
    
     if (install_device == true){
        createHubiGraphTile();
    }
}

def buildData() {
    def resp = [:]
    def now = new Date();
    def then = new Date();
    
    log.debug(graph_timespan);
    switch (graph_timespan){
        case "0": //"Live":
        break;
        case "1": //"Hourly":
            use (groovy.time.TimeCategory) {
              then -= 1.hours
            }
        break;
        case "2": //"Daily":
            then.setHours(0);
            then.setMinutes(0);
            then.setSeconds(0);
        break;
        case "3": //"Every Three Days":
            use (groovy.time.TimeCategory) {
                then -= 2.days;
            }
            then.setHours(0);
            then.setMinutes(0);
            then.setSeconds(0);
        break;
        case "4": //"Weekly":
            use (groovy.time.TimeCategory) {
                then -= 6.days;
            }
            then.setHours(0);
            then.setMinutes(0);
            then.setSeconds(0);
        break;
    }   
     
    if(sensors) {
      sensors.each {sensor ->
          def attributes = settings["attributes_${sensor.id}"];
          resp[sensor.id] = [:];
          attributes.each { attribute ->
              log.debug("$sensor $attribute");
              temp = sensor.statesSince(attribute, then, [max: 50000]).collect{ it.getFloatValue() }
              if (temp.size() == 0){
                 resp[sensor.id][attribute] = [current: sensor.currentState(attribute).getFloatValue(), min: sensor.currentState(attribute).getFloatValue(), max: sensor.currentState(attribute).getFloatValue()];
              } else {
                  resp[sensor.id][attribute] = [current: sensor.currentState(attribute).getFloatValue(), min: temp.min(), max: temp.max()];
              }
              log.debug(resp[sensor.id][attribute]);
          }
      }
      
   }
   return resp
}

def getChartOptions(){
    
    colors = [];
    sensors.each {sensor->
        def attributes = settings["attributes_${sensor.id}"];
        attributes.each {attribute->
            attrib_string = "attribute_${sensor.id}_${attribute}_color"
            transparent_attrib_string = "attribute_${sensor.id}_${attribute}_color_transparent"
            colors << (settings[transparent_attrib_string] ? "transparent" : settings[attrib_string]);
           
        }
    }
    
    def options = [
        "graphTimespan": Integer.parseInt(graph_timespan),
        "graphUpdateRate": Integer.parseInt(graph_update_rate),
        "graphOptions": [
            "bar" : [ "groupWidth" : "${graph_bar_percent}%" ],
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size: "90%",
            "timeline": [
                "rowLabelStyle": ["fontSize": graph_axis_font, "color": graph_axis_color_transparent ? "transparent" : graph_axis_color],
                "barLabelStyle": ["fontSize": graph_axis_font]
            ],
            "backgroundColor": graph_background_color_transparent ? "transparent" : graph_background_color,
            "isStacked": true,
            "chartArea": [ "left": graph_v_buffer, "right" : 10, "top": 10, "bottom": 40 ],
            "legend" : [ "position" : "none" ],
            "hAxis" : [ "viewWindow" : ["max" : graph_max, 
                                        "min" : graph_min], 
                         "minValue" : graph_min, 
                         "maxValue" : graph_max,
                        "textStyle" : ["color": haxis_color_transparent ? "transparent" : haxis_color,
                                       "fontSize": haxis_font]
                      ],
            "vAxis" : [ "textStyle" : ["color": graph_axis_color_transparent ? "transparent" : graph_axis_color,
                                       "fontSize": graph_axis_font]
                       ],
            "annotations" : [    "alwaysOutside": true,
                                 "textStyle": [
      					            "fontSize": annotation_font,
      					            "bold":     annotation_bold,
      					            "italic":   annotation_italic,
      	         					"color":    annotation_color_tansparent ? "transparent" : annotation_color,
      					            "auraColor":annotation_aura_color_transparent ? "transparent" : annotation_aura_color,
				                 ]
                             ],
              "stem": [ "color": annotation_stem_color_transparent ? "transparent" : annotation_stem_color],
        			 
         ],
        "graphLow": graph_min,
        "graphHigh": graph_max,
                
    ]
    
    return options;
}
        
void removeLastChar(str) {
    str.subSequence(0, str.length() - 1)
}

void sampleCode(){
    
def html = """
 <html>
  <head>
    <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
    <script type="text/javascript">
      google.charts.load("current", {packages:["corechart"]});
      google.charts.setOnLoadCallback(drawChart);


function drawChart(){
      var data = google.visualization.arrayToDataTable([
        ['Device',     'Min', 	{ role: "style" }, {role: "tooltip"}, {role: "annotation"}, 
        			   'Low', 	{ role: "style" }, {role: "tooltip"}, {role: "annotation"}, 
                       'Value', { role: "style" }, {role: "tooltip"}, {role: "annotation"}, 
                       'High', 	{ role: "style" }, {role: "tooltip"}, {role: "annotation"},  
                       'Max', 	{ role: "style" }, {role: "tooltip"}, {role: "annotation"}
        ],
        ['Bedroom Temp', 72, 'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475', '1',	'',
        				  6, 'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91', '2',	'',
                          1, 'stroke-color: #527535; stroke-opacity: 0.8; stroke-width: 2; color: #8eb6d4', '3',	'3', 
                          1, 'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91', '4',	'',
                          20,'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475', '5',	'',
        ],
        ['Ouside', 		68, 'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475', 	'6',	'',
        				12, 'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91',	'7',	'',
                        1, 	'stroke-color: #527535; stroke-opacity: 0.8; stroke-width: 2; color: #8eb6d4', 	'8',	'8', 
                        14, 'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91', 	'9',	'',
                        5,	'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475',  '10',	'',
        ],
        ['Freezer', 	28, 'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475',	'11',	'', 
        				2, 	'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91',	'12',	'',
                        1, 	'stroke-color: #527535; stroke-opacity: 0.8; stroke-width: 2; color: #8eb6d4',	'13', '13', 
                        3, 	'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91',	'14',	'',
                        66,	'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475', 	'15',	'',
        ]
      ]);

      var options = {
        title: 'Home Temperatures',
        chartArea: {width: '50%'},
        bar: {groupWidth: "95%"},
        isStacked: true,
        legend: { position: "none" },
        annotations: { 
        			alwaysOutside: true,
        			textStyle: {
      					fontSize: 18,
      					bold: true,
      					italic: true,
      					// The color of the text.
      					color: '#FFFFFF',
      					// The color of the text outline.
      					auraColor: 'transparent',
				      // The transparency of the text.
      					opacity: 1.8
    					},
              stem: {
              	color: 'transparent'
              },
        			highContrast: false }
      };

      var chart = new google.visualization.BarChart(document.getElementById('chart_div'));

      chart.draw(data, options);
      }
      
    </script>
  </head>
  <body>
    <div id="chart_div" style="width: 900px; height: 500px;"></div>
  </body>
</html>
"""
}

def getTimeLine() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    def html = """
    <html style="${fullSizeStyle}">
        <head>
            <script src="https://code.jquery.com/jquery-3.5.0.min.js" integrity="sha256-xNzN2a4ltkB44Mc/Jz3pT4iU1cmeR0FkXs4pru/JxaQ=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.0/moment.min.js" integrity="sha256-imB/oMaNA0YvIkDkF5mINRWpuFPEGVCEkHy6rm2lAzA=" crossorigin="anonymous"></script>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/he/1.2.0/he.min.js" integrity="sha256-awnFgdmMV/qmPoT6Fya+g8h/E4m0Z+UFwEHZck/HRcc=" crossorigin="anonymous"></script>
            <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
            <script type="text/javascript">
google.charts.load('current', {'packages':['corechart']});

let options = [];
let subscriptions = {};
let graphData = {};

//stack for accumulating points to average
let stack = {};

let websocket;

class Loader {
    constructor() {
        this.elem = jQuery(jQuery(document.body).prepend(`
            <div class="loaderContainer">
                <div class="dotsContainer">
                    <div class="dot"></div>
                    <div class="dot"></div>
                    <div class="dot"></div>
                </div>
                <div class="text"></div>
            </div>
        `).children()[0]);
    }

    setText(text) {
        this.elem.find('.text').text(text);
    }

    remove() {
        this.elem.remove();
    }
}

function getOptions() {
    return jQuery.get("${state.localEndpointURL}getOptions/?access_token=${state.endpointSecret}", (data) => {
        options = data;
        console.log("Got Options");
        console.log(options);
    });
}

function getSubscriptions() {
    return jQuery.get("${state.localEndpointURL}getSubscriptions/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Subscriptions");
        console.log(data);
        subscriptions = data;

    });
}

function getGraphData() {
    return jQuery.get("${state.localEndpointURL}getData/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Graph Data");
        console.log(data);
        graphData = data;
    });
}

function parseEvent(event) {
    const now = new Date().getTime();
    let deviceId = event.deviceId;

    //only accept relevent events
    if(subscriptions.ids.includes(deviceId) && subscriptions.attributes[deviceId].includes(event.name)) {
        let value = event.value;
        let attribute = event.name;

        graphData[deviceId][attribute].current = value;
        if (value > max) graphData[deviceId][attribute].max = value;
        else if (value < min) graphData[deviceId][attribute].min = value;
        //update if we are realtime
        if(options.graphUpdateRate === 0) update();
    }
}

function update(callback) {
    drawChart(callback);
}

async function onLoad() {
    //append our css
    jQuery(document.head).append(`
        <style>
            .loaderContainer {
                position: fixed;
                z-index: 100;

                width: 100%;
                height: 100%;

                background-color: white;

                display: flex;
                flex-flow: column nowrap;
                justify-content: center;
                align-items: middle;
            }

            .dotsContainer {
                height: 60px;
                padding-bottom: 10px;

                display: flex;
                flex-flow: row nowrap;
                justify-content: center;
                align-items: flex-end;
            }

            @keyframes bounce {
                0% {
                    transform: translateY(0);
                }

                50% {
                    transform: translateY(-50px);
                }

                100% {
                    transform: translateY(0);
                }
            }

            .dot {
                box-sizing: border-box;

                margin: 0 25px;

                width: 10px;
                height: 10px;

                border: solid 5px black;
                border-radius: 5px;

                animation-name: bounce;
                animation-duration: 1s;
                animation-iteration-count: infinite;
            }

            .dot:nth-child(1) {
                animation-delay: 0ms;
            }

            .dot:nth-child(2) {
                animation-delay: 333ms;
            }

            .dot:nth-child(3) {
                animation-delay: 666ms;
            }

            .text {
                font-family: Arial;
                font-weight: 200;
                font-size: 2rem;
                text-align: center;
            }
        </style>
    `);

    let loader = new Loader();

    //first load
    loader.setText('Getting options (1/4)');
    await getOptions();
    loader.setText('Getting device data (2/4)');
    await getSubscriptions();
    loader.setText('Getting events (3/4)');
    await getGraphData();

    loader.setText('Drawing chart (4/4)');

    update(() => {
        //destroy loader when we are done with it
        loader.remove();
    });

    //start our update cycle
    if(options.graphUpdateRate !== -1) {
        //start websocket
        websocket = new WebSocket("ws://" + location.hostname + "/eventsocket");
        websocket.onopen = () => {
            console.log("WebSocket Opened!");
        }
        websocket.onmessage = (event) => {
            parseEvent(JSON.parse(event.data));
        }

        if(options.graphUpdateRate !== 0) {
            setInterval(() => {
                update();
            }, options.graphUpdateRate);
        }
    }

    //attach resize listener
    window.addEventListener("resize", () => {
        drawChart();
    });
}

function onBeforeUnload() {
    if(websocket) websocket.close();
}

function drawChart(callback) {
    let now = new Date().getTime();
    let min = now - options.graphTimespan;

    const dataTable = new google.visualization.arrayToDataTable([[{ type: 'string', label: 'Device' }, { type: 'number', label: 'Min' }, 	{ role: "style" }, { role: "tooltip" }, { role: "annotation" },
                                                                        					           { type: 'number', label: 'Low'}, 	{ role: "style" }, { role: "tooltip" }, { role: "annotation" },
                                                                                                       { type: 'number', label: 'Value'},   { role: "style" }, { role: "tooltip" }, { role: "annotation" },
                                                                                                       { type: 'number', label: 'High'}, 	{ role: "style" }, { role: "tooltip" }, { role: "annotation" },
                                                                                                       { type: 'number', label: 'Max'}, 	{ role: "style" }, { role: "tooltip" }, { role: "annotation" }]]);

    subscriptions.order.forEach(deviceId => {
      Object.entries(graphData[deviceId]).forEach(([attr, event]) => {
        const max_ = event.max > options.graphHigh ? options.graphHigh : event.max;
        const min_ = event.min < options.graphLow ? options.graphLow : event.min;
        const cur_ = event.current;
        const b = cur_ - min_;
        const c = 1;
        const d = max_ - cur_ ;
        const e = options.graphHigh - max_;
        const a = (options.graphHigh - options.graphLow) - (b + c + d + e);
        var cur_String = '';

        const name = subscriptions.labels[deviceId][attr].replace('%deviceName%', subscriptions.sensors[deviceId].displayName).replace('%attributeName%', attr);
        const colors = subscriptions.colors[deviceId][attr];
        console.log("showAnnotation ", colors.showAnnotation, cur_, colors.annotation_units);
        if (colors.showAnnotation == true){
            cur_String = `\${cur_.toFixed(1)}\${colors.annotation_units}`;
            console.log("Current: ",cur_String) 
        }

        dataTable.addRow([name,  a,     `color: \${colors.backgroundColor}`,                                                                        `\${name}`,     '',
                                 b,     `color: \${colors.minMaxColor}`,                                                                            `\${name}`,     '',
                                 c,     `{color: \${colors.currentValueColor}; stroke-color: #FFFFFF; stroke-opacity: 0.8; stroke-width: 1;}`,      `\${name}`,     cur_String,
                                 d,     `color: \${colors.minMaxColor}`,                                                                            `\${name}`,     '',
                                 e,     `color: \${colors.backgroundColor}`,                                                                        `\${name}`,     ''
        ]);
        //console.log("Current: %f, Min: %f, Max: %f A: %f B: %f C: %f D: %f E: %f", event.current, event.min, event.max, a, b, c, d, e);
      });

    });

    let chart = new google.visualization.BarChart(document.getElementById("timeline"));

    //if we have a callback
    if(callback) google.visualization.events.addListener(chart, 'ready', callback);

    chart.draw(dataTable, options.graphOptions);
}

google.charts.setOnLoadCallback(onLoad);
window.onBeforeUnload = onBeforeUnload;
        </script>
      </head>
      <body style="${fullSizeStyle}">
          <div id="timeline" style="${fullSizeStyle}" align="center"></div>
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

//oauth endpoints
def getGraph() {
    return render(contentType: "text/html", data: getTimeLine());      
}

def getData() {
    def data = buildData();
         
    return render(contentType: "text/json", data: JsonOutput.toJson(data));
}

def getOptions() {
    return render(contentType: "text/json", data: JsonOutput.toJson(getChartOptions()));
}

def getSubscriptions() {
    def _ids = [];
    def _attributes = [:];
    def labels = [:];
    def colors = [:];
    sensors.each { sensor ->
        _ids << sensor.id;
        _attributes[sensor.id] = [];
        labels[sensor.id] = [:];
        colors[sensor.id] = [:];
        def attributes = settings["attributes_${sensor.id}"];
        attributes.each { attribute ->
            _attributes[sensor.id] << attribute;
            labels[sensor.id][attribute] = settings["graph_name_override_${sensor.id}_${attribute}"];
            colors[sensor.id][attribute] = ["backgroundColor":     settings["attribute_${sensor.id}_${attribute}_background_color"],
                                            "minMaxColor":         settings["attribute_${sensor.id}_${attribute}_minmax_color"],
                                            "currentValueColor":   settings["attribute_${sensor.id}_${attribute}_current_color"],
                                            "showAnnotation":      settings["attribute_${sensor.id}_${attribute}_showAnnotation"],
                                            "annotation_font":     settings["attribute_${sensor.id}_${attribute}_annotation_font"],
                                            "annotation_units":    settings["attribute_${sensor.id}_${attribute}_annotation_units"],
                                           ];
        }
                                               
    }
                                                                                    
    def sensors_fmt = [:];
    sensors.each { it ->
        sensors_fmt[it.id] = [ "id": it.id, "displayName": it.displayName, "currentStates": it.currentStates ];
    }
    
    def order = [sensors.size()];
    sensors.each {sensor ->
        order[Integer.parseInt(settings["displayOrder_${sensor.id}"]) - 1] = sensor.idAsLong;
    }
    
    def subscriptions = [
        "sensors": sensors_fmt,
        "ids": _ids,
        "attributes": _attributes,
        "labels": labels,
        "colors": colors,
        "order": order
    ];
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
}
