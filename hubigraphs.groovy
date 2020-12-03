definition(
    name: "Hubigraphs",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubitat Graph Creator Parent App",
    category: "My Apps",
    installOnOpen: true,
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

//V 1.0 Ordering, Color and Common API Update
//V 1.8 Smoother sliders, bug fixes
//V 2.0 Support for Time Graphs
//V 2.1 Support for Heatmaps
//V 3.3 Radar Tiles
//V 0.1 Beta - Weather Tiles 2

preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "Graph Creator", install: true, uninstall: true, submitOnChange: true)
    page(name: "setupOpenWeather", title: "Setup Open Weather", nextPage: "mainPage")
}

def mainPage(){
    
    if (latitude && longitude && apikey) {
        childDevice = getChildDevice("OPEN_WEATHER${app.id}");     
        log.debug(childDevice);
    
         if (!childDevice) {
            device_name="Open Weather Child Device";
            log.debug("Creating Device $device_name");
            childDevice = addChildDevice("tchoward", "OpenWeather Hubigraph Driver", "OPEN_WEATHER${app.id}", null,[completedSetup: true, label: device_name]) 
            log.info "Successfully Created Child"    
        }
    
        if (childDevice) {
           childDevice.setupDevice(latitude: latitude, longitude: longitude, apiKey: apikey, pollInterval: open_weather_refresh_rate);
           childDevice.pollData();
        }
    }
    
    
    dynamicPage(name: "mainPage"){
       section {
            app(name: "hubiGraphLine", appName: "Hubigraph Line Graph",    namespace: "tchoward", title: "Create New Line Graph (Deprecated)", multiple: true)
            app(name: "hubiBarGraph",  appName: "Hubigraph Bar Graph",     namespace: "tchoward", title: "Create New Bar Graph", multiple: true)
			app(name: "hubiRangeBar",  appName: "Hubigraph Range Bar",     namespace: "tchoward", title: "Create New Range Bar", multiple: true)
            app(name: "hubiGraphTime", appName: "Hubigraph Time Line",     namespace: "tchoward", title: "Create New Time Line", multiple: true)
            app(name: "hubiGauge",     appName: "Hubigraph Gauge",         namespace: "tchoward", title: "Create New Gauge", multiple: true)
            app(name: "hubiTimeGraph", appName: "Hubigraph Time Graph",    namespace: "tchoward", title: "Create New Time Graph", multiple: true)
            app(name: "hubiHeatMap",   appName: "Hubigraph Heat Map",      namespace: "tchoward", title: "Create New Heat Map", multiple: true)
            app(name: "hubiWeather",   appName: "Hubigraph Weather Tile",  namespace: "tchoward", title: "Create New Weather Tile", multiple: true)
            app(name: "hubiForecast",  appName: "Hubigraph Forecast Tile", namespace: "tchoward", title: "Create New Forecast Tile", multiple: true)
            app(name: "hubiWeather2",   appName: "Hubigraph Weather Tile 2",  namespace: "tchoward", title: "Create New Weather Tile 2", multiple: true)
            app(name: "hubiRadar",      appName: "Hubigraph Radar Tile",      namespace: "tchoward", title: "Create New Radar Tile", multiple: true)


        }
        section {
            href name: "setupOpenWeather", title: "Setup Up Open Weather for Weather Tile", description: "", page: "setupOpenWeather"    
        } 
    }
}
def getOpenWeatherData(){
    childDevice =  getChildDevice("OPEN_WEATHER${app.id}");
    if (!childDevice){
         log.debug("Error: No Child Found");
         return null;
    }
    return(childDevice.getWeatherData());
}

def setupOpenWeather(){
    
     def updateEnum = ['Manual Poll Only','1 Minute','5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours'];
    
    
    def location = getLocation();
    
    dynamicPage(name: "setupOpenWeather"){
        
        section{
            input( type: "enum", name: "open_weather_refresh_rate", title: "<b>Select OpenWeather Update Rate</b>", multiple: false, required: false, options: updateEnum, defaultValue: "5 Minutes");
            input( type: "text", name: "latitude", title:"<b>Latitude (Default = Hub Location)</b>", defaultValue: location.latitude);
            input( type: "text", name: "longitude", title:"<b>Longitude (Default = Hub Location)</b>", defaultValue: location.longitude);
            input( type: "text", name: "apikey", title: "<b>OpenWeather Key</b>", defaultValue:"", submitOnChange: true); 
        }
        
    }   
}

def makeCopy(child){

}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    
    
    
   
    
    unsubscribe()
    initialize()
}

def initialize() {
    // nothing needed here, since the child apps will handle preferences/subscriptions
    // this just logs some messages for demo/information purposes
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }
}

/********************************************************************************************************************************
*********************************************************************************************************************************
****************************************** NEW FORM FUNCTIONS********************************************************************
*********************************************************************************************************************************
*********************************************************************************************************************************/

def hubiForm_container(child, containers, numPerRow=1){
    
    if (numPerRow == 0){
            style = """style="margin: 0 !important; padding: 0 !important;"""
            numPerRow = 1
    } else { 
            style = "";
    }
        child.call(){
                def html_ = 
                        """
                        
                        <div class = "mdl-grid" style="margin: 0 !important; padding: 0 !important;"> 
                        """
                containers.each{container->
                        html_ += """<div class="mdl-cell mdl-cell--${12/numPerRow}-col-desktop mdl-cell--${8/numPerRow}-col-tablet mdl-cell--${4/numPerRow}-col-phone" ${style}>"""
                        html_ += container;
                        html_ += """</div>"""
                }
                html_ += """</div>"""
                        
                paragraph (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
}

def hubiForm_subcontainer(Map map, child){
    
        child.call(){
                def containers = map.objects;
                def breakdown = map.breakdown;
                def html_ = 
                        """
                        
                        <div class = "mdl-grid" style="margin: 0; padding: 0; "> 
                        """
                count = 0;
                containers.each{container->
                    def sz_12 = 12*breakdown[count];
                    def sz_8 = 8*breakdown[count];
                    def sz_4 = 4*breakdown[count];
                    html_ += """<div class="mdl-cell mdl-cell--${sz_12.intValue()}-col-desktop mdl-cell--${sz_8.intValue()}-col-tablet mdl-cell--${sz_4.intValue()}-col-phone" style= "justify-content: center;" >"""
                    html_ += container;
                    html_ += """</div>"""
                    
                    count++;
                }
                html_ += """</div>"""
                        
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
}

def hubiForm_table(Map map, child){
    
    child.call(){
        def header = map.header;
        def rows = map.rows;
        def footer = map.footer;
        
        def html_ = """
            <table class="mdl-data-table  mdl-shadow--2dp dataTable" role="grid" data-upgraded=",MaterialDataTable">
              <thead><tr>
        """
        header.each{cell->
            html_ += """<th class="mdl-data-table__cell--non-numeric ">${cell}</th>"""
        }
        html+= """</tr></thead><tbody>"""
        count = 0;
        rows.each{row->
           
            html_ += """<tr role="row" class="odd">""";
            row.each{cell->
                html_ += """<td class="mdl-data-table__cell--non-numeric">${cell}</td>""";
            }
            html_ += """</tr>""";
        } //rows
        html_ += """<tr role="row" class="even">""";
        footer.each{cell->
            html_ += """<td class="mdl-data-table__cell--non-numeric">${cell}</td>""";    
        }
        html_ += """</tr>""";
        
        html_ += """</tbody></table>"""
        
        return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
            
    }
}

def hubiForm_text(child, text){
    child.call(){
        def html_ = """$text""";
    
        return html_
    }
}

def hubiForm_text_format(Map map, child){
    
    child.call(){
        def text = map.text;
        def halign = map.horizontal_align ? "text-align: ${map.horizontal_align};" : ""; 
        def valign = map.vertical_align ? "vertical-align: ${map.vertical_align}; " : ""; 
        def size = map.size ? "font-size: ${map.size}px;" : "";
        def html_ = """<p style="$halign padding-top:20px; $size">$text</p>""";
    
        return html_
    }
}


def hubiForm_page_button(child, title, page, width, icon=""){
        def html_;
    
        child.call(){
                 html_ = """
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
                        """.replace('\t', '').replace('\n', '').replace('  ', '')
        }
        return html_;
}



def hubiForm_section(child, title, pos, icon="", Closure code) {
	
        child.call(){
                def id = title.replace(' ', '_').replace('(', '').replace(')','');
                def title_ = title.replace("'", "’").replace("`", "’");

                def titleHTML = """
                        <div class="mdl-layout__header" style="display: block; background:#033673; margin: 0 -16px; width: calc(100% + 32px); position: relative; z-index: ${pos}; overflow: visible;">          
                        <div class="mdl-layout__header-row">
                                <span class="mdl-layout__title" style="margin-left: -32px; font-size: 18px; width: auto;">
                                        ${title_}
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
            
            section(modContent, code);
        }         
}

def hubiForm_enum(Map map, child){
         
        child.call(){
             	def title = map.title;
		        def var = map.name;
                def list = map.list;
		        def defaultVal = map.default;
		        def submit_on_change = map.submit_on_change;
		
		
                if (settings[var] == null){
                    app.updateSetting ("${var}", defaultVal);
                }
                def actualVal = settings[var] != null ? settings[var] : defaultVal;
                def submitOnChange = submit_on_change ? "submitOnChange" : "";
		 
                def html_ = """    
                    <div class="form-group">
                        <input type="hidden" name="${var}.type" value="enum">
                        <input type="hidden" name="${var}.multiple" value="false">
                    </div>

                    <div class="mdl-cell mdl-cell--12-col mdl-textfield mdl-js-textfield" style="" data-upgraded=",MaterialTextfield">
                    <label for="settings[${var}]" class="control-label">
                        <b>${title}</b>
                    </label>

                    
                        <select id="settings[${var}]" name="settings[${var}]"
                            class="selectpicker form-control mdl-switch__input ${submitOnChange} SumoUnder" placeholder="Click to set" data-default="${defaultVal}" tabindex="-1">
                                <option class="optiondefault" value="" style="display: block;">No selection</option>
                    """
                    list.each{ item ->
                        if (actualVal == item) 
                            selectedString = /selected="selected"/;
                        else 
                            selectedString = "";
                        
                        html_ += """<option value="${item}" ${selectedString}>${item}</option>"""
                    }       
                    html_ += """ 
                        </select>
                        
                        <div class="optWrapper">
                            <ul class="options">
                        """
                        list.each{ item ->
                            html+= """<li class="opt selected"><label>${item}</label></li>"""
                        }
                   html_ += """
                            </ul>
                        </div>
                    </div>
              
                """
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
 }

def hubiForm_switch(Map map, child){
         
        child.call(){
             	def title = map.title;
		        def var = map.name;
		        def defaultVal = map.default;
		        def submit_on_change = map.submit_on_change;
		
		
                def actualVal = settings[var] != null ? settings[var] : defaultVal;
                def submitOnChange = submit_on_change ? "submitOnChange" : "";
		 
                def html_ = """      
                                <div class="form-group">
                                        <input type="hidden" name="${var}.type" value="bool">
                                        <input type="hidden" name="${var}.multiple" value="false">
                                </div>
                                <label for="settings[${var}]"
                                        class="mdl-switch mdl-js-switch mdl-js-ripple-effect mdl-js-ripple-effect--ignore-events is-upgraded ${actualVal ? "is-checked" : ""}  
                                        data-upgraded=",MaterialSwitch,MaterialRipple">    
                                        <input name="checkbox[${var}]" id="settings[${var}]" class="mdl-switch__input 
                                                ${submitOnChange}"
                                                type="checkbox" 
                                                ${actualVal ? "checked" : ""}>                
                                        <div class="mdl-switch__label" >${title}</div>   
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
                
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
 }

def hubiForm_text_input(child, title, var, defaultVal, submitOnChange){
    
     child.call(){
         settings[var] = settings[var] != null ? settings[var] : defaultVal;
        
        def html_ = """
                <div class="form-group">
                <input type="hidden" name="${var}.type" value="text">
                <input type="hidden" name="${var}.multiple" value="false">
                </div>
                <label for="settings[${var}]" class="control-label">
                ${title}
                </label>
                <input type="text" name="settings[${var}]" 
                        class="mdl-textfield__input ${submitOnChange == "true" ? "submitOnChange" : ""} " 
                        value="${settings[var]}" placeholder="Click to set" id="settings[${var}]">
                """
        
        return html_.replace('\t', '').replace('\n', '').replace('  ', '');
     }
}

def hubiForm_font_size(Map map, child){
    
    child.call(){
        def title = map.title;
        def varname = map.name;
        def default_ = map.default;
        def min = map.min;
        def max = map.max;
        def submit_on_change = map.submit_on_change;
        def baseId = varname;
	
	    def varFontSize = "${varname}_font"    
        settings[varFontSize] = settings[varFontSize] ? settings[varFontSize] : default_;
        submitOnChange = submit_on_change ? "submitOnChange" : "";
	    
        def html_ = 
                """
                <table style="width:100%">
                <tr><td><label for="settings[${varFontSize}]" class="control-label"><b>${title} Font Size</b></td>
                        <td >
                            <span id="${baseId}_font_size_val" style="text-align:right; font-size:${settings[varFontSize]}px">Font Size: ${settings[varFontSize]}</span>
                        </td>
                        </label>
                </tr>
                </table>
                <input type="range" min = "$min" max = "$max" name="settings[${varFontSize}]" 
							      class="mdl-slider $submit_on_change " 
							      value="${settings[varFontSize]}" 
							      id="settings[${varFontSize}]"
							      onchange="${baseId}_updateFontSize(this.value);">
                <div class="form-group">
                        <input type="hidden" name="${varFontSize}.type" value="number">
                        <input type="hidden" name="${varFontSize}.multiple" value="false">
                </div>
		        <script>
                      function ${baseId}_updateFontSize(val) {
                            var text = "";
                            text += "Font Size: "+val;
                            jQuery('#${baseId}_font_size_val').css("font-size", val+"px");
                            jQuery('#${baseId}_font_size_val').text(text); 
                      }
                </script>
                """
        
        return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
    }
}

def hubiForm_fontvx_size(Map map, child){
    
    child.call(){
        def title = map.title;
        def varname = map.name;
        def default_ = map.default;
        def min = map.min;
        def max = map.max;
        def submit_on_change = map.submit_on_change;
        def baseId = varname;
        def weight = map.weight ? "font-weight: ${map.weight} !important;" : "";
        def icon = null;
        
        def varFontSize = "${varname}_font"  
        def icon_size = settings[varFontSize] ? 10*settings[varFontSize] : default_*10; 
       
        def jq = "";
        
        if (map.icon){
              icon = """
                    <style>
                        .material-icons.test { font-size: ${icon_size}px; }
                    </style>
                    <i id="${baseId}_icon" class="material-icons test">cloud</i>
                    """;
            
              jq = """jQuery('.test').css('font-size', 10*val+"px");"""   
        } else {
              jq = """                              
                        jQuery('#${baseId}_font_size_val').css("font-size", 0.5*val+"em");
                        jQuery('#${baseId}_font_size_val').text(text);
                    """    
        }
        
	
	    
        settings[varFontSize] = settings[varFontSize] ? settings[varFontSize] : default_;
        submitOnChange = submit_on_change ? "submitOnChange" : "";
	    
        def html_ = 
                """
                <label for="settings[${varFontSize}]" class="control-label" style= "vertical-align: bottom;">
                    <b>${title}</b>
                <span id="${baseId}_font_size_val" style="float:right; font-size: ${settings[varFontSize]*0.5}em; ${weight}">
                    ${icon == null ? settings[varFontSize] : icon}
                </span>
                </label>
                        
                <input type="range" min = "$min" max = "$max" name="settings[${varFontSize}]" 
							      class="mdl-slider $submit_on_change " 
							      value="${settings[varFontSize]}" 
							      id="settings[${varFontSize}]"
							      onchange="${baseId}_updateFontSize(this.value);">
                <div class="form-group">
                        <input type="hidden" name="${varFontSize}.type" value="number">
                        <input type="hidden" name="${varFontSize}.multiple" value="false">
                </div>
		        <script>
                      function ${baseId}_updateFontSize(val) {
                            var text = "";
                            text += val;"""
                        html_+= jq;
                        html_+="""
                            
                      }
                </script>
                """
        
        return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
    }
}


def hubiForm_line_size(Map map, child){
    
     child.call(){
        def title = map.title;
        def varname = map.name;
        def default_ = map.default;
        def min = map.min;
        def max = map.max;
        def submit_on_change = map.submit_on_change;
        def baseId = varname;
	   
	    def varLineSize = "${varname}_line_size"     
        settings[varLineSize] = settings[varLineSize] ? settings[varLineSize] : default_;
        submitOnChange = submit_on_change ? "submitOnChange" : "";
        
        def html_ =
                """
                <table style="width:100%">
                <tr><td><label for="settings[${varLineSize}]" class="control-label"><b>${title} Width</b></td>
                        <td border=1 style="text-align:right;">
			                <span id="${baseId}_line_size_text" name="testing" >
				                Width: ${settings[varLineSize]} <hr id='${baseId}_line_size_draw' style='background-color:#1A77C9; height:${settings[varLineSize]}px; border: 0;'>
			                </span>
			</td>
                        </label>
                </tr>
                </table>
                <input type="range" min = "$min" max = "$max" name="settings[${varLineSize}]" 
                                  class="mdl-slider ${submitOnChange}"
							      value="${settings[varLineSize]}" 
							      id="settings[${varLineSize}]"
							      onchange="${baseId}_updateLineInput(this.value);">
                <div class="form-group">
                        <input type="hidden" name="${varLineSize}.type" value="number">
                        <input type="hidden" name="${varLineSize}.multiple" value="false">
                </div>
		<script>
                      function ${baseId}_updateLineInput(val) {
                            var text = "";
                            text += "Width: "+val;
                            
                            jQuery('#${baseId}_line_size_text').text(text);
                            jQuery('#${baseId}_line_size_draw').remove();
                            jQuery('#${baseId}_line_size_text').after("<hr id='${baseId}_line_size_draw' style='background-color:#1A77C9; height:"+val+"px; border: 0;'>");
                        }
                </script>
                """
        
        return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
    } 
    
}

def hubiForm_slider(Map map, child){
      
    child.call(){
        def title = map.title;
        def varname = map.name;
        def default_ = map.default;
        def min = map.min;
        def max = map.max;
        def units = map.units;
        def submit_on_change = map.submit_on_change;
            
        def fontSize;
        def varSize = "${varname}"
        def baseId = "${varname}";
        
        settings[varSize] = settings[varSize] ? settings[varSize] : default_;
        submitOnChange = submit_on_change ? "submitOnChange" : "";
        
        def html_ =
                """
                <table style="width:100%">
                <tr><td><label for="settings[${varSize}]" class="control-label"><b>${title}</b></td>
                    <td border=1 style="text-align:right;"><span id="${baseId}_slider_val" name="testing" >${settings[varSize]}${units}</span></td>
                        </label>
                </tr>
                </table>
                <input type="range" min = "$min" max = "$max" name="settings[${varSize}]" 
                                                              class="mdl-slider $submitOnChange " 
                                                              value="${settings[varSize]}" 
                                                              id="settings[${varSize}]"
                                                              onchange="${baseId}_updateTextInput(this.value);">
                <div class="form-group">
                        <input type="hidden" name="${varSize}.type" value="number">
                        <input type="hidden" name="${varSize}.multiple" value="false">
                </div>
                <script>
		
		                function ${baseId}_updateTextInput(val) {
                            var text = "";
                            text += val+"${units}";
                            jQuery('#${baseId}_slider_val').text(text); 
                        }
                </script>
                """
        
        return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
    }
}

def hubiForm_color(child, title, varname, defaultColorValue, defaultTransparentValue, submit = false){
        child.call(){
                
                def varnameColor = "${varname}_color";
                def varnameTransparent = "${varname}_color_transparent"
                def colorTitle = "<b>${title} Color</b>"
                def notTransparentTitle = "Transparent";
                def transparentTitle = "${title}: Transparent"
                
                settings[varnameColor] = settings[varnameColor] ? settings[varnameColor]: defaultColorValue;
                settings[varnameTransparent] = settings[varnameTransparent] ? settings[varnameTransparent]: defaultTransparentValue;
                
                def isTransparent = settings[varnameTransparent];
                
                def html_ = 
                """
                <div style="display: flex; flex-flow: row wrap;">
                        <div style="display: flex; flex-flow: row nowrap; flex-basis: 100%;">
                        ${!isTransparent ? """<label for="settings[${varnameColor}]" class="control-label" style="flex-grow: 1">${colorTitle}</label>""" : """"""}
                        <label for="settings[${varnameTransparent}]" class="control-label" style="width: auto;">${isTransparent ? transparentTitle: notTransparentTitle}</label>
                        </div>
                        ${!isTransparent ? """
                        <div style="flex-grow: 1; flex-basis: 1px; padding-right: 8px;">
                                <input type="color" name="settings[${varnameColor}]" class="mdl-textfield__input ${submit ? "submitOnChange" : ""} " value="${settings[varnameColor] ? settings[varnameColor] : defaultColorValue}" placeholder="Click to set" id="settings[${varnameColor}]" list="presetColors">
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
                """;
                
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
}

def hubiForm_graph_preview(child){
        child.call(){
                if (!state.count_) state.count_ = 7;
                
                def html_ =
                         """
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
                        <div class="iframe-container">
                        <iframe id="preview_frame" style="width: 100%; height: 100%; position: relative; z-index: 1; background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAEq2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIKICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIKICAgIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIgogICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgZXhpZjpQaXhlbFhEaW1lbnNpb249IjIiCiAgIGV4aWY6UGl4ZWxZRGltZW5zaW9uPSIyIgogICBleGlmOkNvbG9yU3BhY2U9IjEiCiAgIHRpZmY6SW1hZ2VXaWR0aD0iMiIKICAgdGlmZjpJbWFnZUxlbmd0aD0iMiIKICAgdGlmZjpSZXNvbHV0aW9uVW5pdD0iMiIKICAgdGlmZjpYUmVzb2x1dGlvbj0iNzIuMCIKICAgdGlmZjpZUmVzb2x1dGlvbj0iNzIuMCIKICAgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIKICAgcGhvdG9zaG9wOklDQ1Byb2ZpbGU9InNSR0IgSUVDNjE5NjYtMi4xIgogICB4bXA6TW9kaWZ5RGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIKICAgeG1wOk1ldGFkYXRhRGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCI+CiAgIDx4bXBNTTpIaXN0b3J5PgogICAgPHJkZjpTZXE+CiAgICAgPHJkZjpsaQogICAgICBzdEV2dDphY3Rpb249InByb2R1Y2VkIgogICAgICBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZmZpbml0eSBQaG90byAxLjguMyIKICAgICAgc3RFdnQ6d2hlbj0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIvPgogICAgPC9yZGY6U2VxPgogICA8L3htcE1NOkhpc3Rvcnk+CiAgPC9yZGY6RGVzY3JpcHRpb24+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8+IC4TuwAAAYRpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZE7SwNBFEaPiRrxQQQFLSyCRiuVGEG0sUjwBWqRRPDVbDYvIYnLboIEW8E2oCDa+Cr0F2grWAuCoghiZWGtaKOy3k2EBIkzzL2Hb+ZeZr4BWyippoxqD6TSGT0w4XPNLyy6HM/UYqONfroU1dBmguMh/h0fd1RZ+abP6vX/uYqjIRI1VKiqEx5VNT0jPCk8vZbRLN4WblUTSkT4VLhXlwsK31p6uMgvFseL/GWxHgr4wdYs7IqXcbiM1YSeEpaX404ls+rvfayXNEbTc0HJnbI6MAgwgQ8XU4zhZ4gBRiQO0YdXHBoQ7yrXewr1s6xKrSpRI4fOCnESZOgVNSvdo5JjokdlJslZ/v/11YgNeovdG31Q82Sab93g2ILvvGl+Hprm9xHYH+EiXapfPYDhd9HzJc29D84NOLssaeEdON+E9gdN0ZWCZJdli8Xg9QSaFqDlGuqXip797nN8D6F1+aor2N2DHjnvXP4Bhcln9Ef7rWMAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAXSURBVAiZY7hw4cL///8Z////f/HiRQBMEQrfQiLDpgAAAABJRU5ErkJggg=='); background-size: 25px; background-repeat: repeat; image-rendering: pixelated;" src="${state.localEndpointURL}graph/?access_token=${state.endpointSecret}" data-fullscreen="false" 
                            onload="(() => {
                         })()""></iframe>
                        </div>
                """
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
}

def hubiForm_sub_section(child, myText=""){

		child.call(){
                def newText = myText.replaceAll( /'/, '’' ).replace("'", "’").replace("`", "’")
                def html_ = 
                        """
                       
                        <div class="mdl-layout__header" style="display: block; min-height: 0;">
                                <div class="mdl-layout__header-row" style="height: 48px;">
                                <span class="mdl-layout__title" style="margin-left: -32px; font-size: 9px; width: auto;">
                                    <h4 id="${myTest}" style="font-size: 16px;">${newText}</h5>
                                </span>
                                </div>
                        </div>
                        """
                        
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
}

def hubiForm_cell(child, containers, numPerRow){
    
        child.call(){
                def html_ = 
                        """
                        <div class = "mdl-grid mdl-grid--no-spacing mdl-shadow--4dp" style="margin-top: 0px !important; margin: 0px; padding: 0px 0px;"> 
                        """
                containers.each{container->
                        html_ += """<div class="mdl-cell mdl-cell--${12/numPerRow}-col-desktop mdl-cell--${8/numPerRow}-col-tablet mdl-cell--${4/numPerRow}-col-phone">"""
                        html_ += container;
                        html_ += """</div>"""
                }
                html_ += """</div>"""
                        
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
}

def hubiForm_list_reorder(child, var, var_color, solid_background="") {
        child.call(){
           
           def count_ = 0;
            
            if (settings["${var}"] != null){
                list_ = parent.hubiTools_get_order(settings["${var}"]);
                
                //Check List
                result_ = true;
                count_ = 0;
                //check for addition/changes
                sensors.each { sensor ->
                     id = sensor.id;
                     attributes = settings["attributes_${id}"];
                     attributes.each { attribute ->
                         count_ ++;
                         inner_result = false;
                         for (i=0; i<list_.size(); i++){
                             if (list_[i].id == id && list_[i].attribute == attribute){
                                  inner_result = true;   
                             }
                         }
                         result_ = result_ & inner_result;
                     }
                 }   
                 //check for smaller
                count_result = false;
                if (list_.size() == count_){
                    count_result = true;
                }
                result_ = result_ & count_result;    
            }
            
            if (result_ == false) {
                settings["${var}"] = null;        
            } 
            
            //build list order
            list_data = [];
            //Setup Original Ordering
            if (settings["${var}"] == null){
                settings["${var}"] = "[";
                sensors.each { sensor ->
                     attributes = settings["attributes_${sensor.id}"];
                     attributes.each { attribute ->
                         settings["${var}"] += /"attribute_${sensor.id}_${attribute}",/  
                         if (settings["attribute_${sensor.id}_${attribute}_${var_color}_color"] == null){
                             if (solid_background== ""){
                                 settings["attribute_${sensor.id}_${attribute}_${var_color}_color"] = parent.hubiTools_rotating_colors(count_);
                             } else {
                                 settings["attribute_${sensor.id}_${attribute}_${var_color}_color"] = solid_background;
                             }
                         }
                         count_++;
                     }
                 }
                settings["${var}"] = settings["${var}"].substring(0, settings["${var}"].length() - 1);
                settings["${var}"] += "]";
            }
   
            count_ = 0;
            order_ = parent.hubiTools_get_order(settings["${var}"]);
            order_.each { device_->
                deviceName_ = parent.hubiTools_get_name_from_id(device_.id, sensors);
                title_ = """<b>${deviceName_}</b><br><p style="float: right;">${device_.attribute}</p>""";
                title_.replace("'", "’").replace("`", "’");
                list_data << [title: title_, var: "attribute_${device_.id}_${device_.attribute}"];
            }
            
            /**********************************************/
            
            def var_val_ = settings["${var}"].replace('"', '&quot;');
            def html_ = 
               """
                <script>
                    function onOrderChange(order) {
                                        jQuery("#settings${var}").val(JSON.stringify(order));
                    }
                </script>
                <script src="/local/a930f16d-d5f4-4f37-b874-6b0dcfd47ace-HubiGraph.js"></script>
                <div id = "moveable" class = "mdl-grid" style="margin: 0; padding: 0; text-color: white !important"> 
               """
               
                list_data.each{data->
                    color_ = settings["${data.var}_${var_color}_color"];
                    id_ = "${data.var}"
                    html_ += """<div id="$id_" class="mdl-cell mdl-cell--12-col-desktop mdl-cell--8-col-tablet mdl-cell--4-col-phone mdl-shadow--4dp mdl-color-text--indigo-400" 
                                        draggable="true" ondragover="dragOver(event)" ondragstart="dragStart(event)" ondragend= "dragEnd(event)"
                                        style = "font-size: 16px !important; margin: 8px !important; padding: 14px !important;">
                                        <i class="mdl-icon-toggle__label material-icons" style="color: ${color_} !important;">fiber_manual_record</i>
                                        
                                    """
                        html_ += data.title;
                        html_ += """</div>
                        """
               }
               html_ += """</div>
                <input type="text" id="settings${var}" name="settings[${var}]" value="${var_val_}" style="display: none;" disabled />
                <div class="form-group">
                   <input type="hidden" name="${var}.type" value="text">
                   <input type="hidden" name="${var}.multiple" value="false">
                </div>"""
            
               html_ = html_.replace('\t', '').replace('\n', '').replace('  ', '');
                  
               paragraph (html_);
        }
}

/********************************************************************************************************************************
*********************************************************************************************************************************
****************************************** TOOLS ********************************************************************************
*********************************************************************************************************************************
*********************************************************************************************************************************/

def hubiTool_create_tile(child, location="graph") {
	child.call(){

                log.info "Creating HubiGraph Child Device"
        
                def childDevice = getChildDevice("HUBIGRAPH_${app.id}");     
                log.debug(childDevice);
                
                if (!childDevice) {
                        if (!device_name) device_name="Dummy Device";
                        log.debug("Creating Device $device_name");
                        childDevice = addChildDevice("tchoward", "Hubigraph Tile Device", "HUBIGRAPH_${app.id}", null,[completedSetup: true, label: device_name]) 
                        log.info "Created HTTP Switch [${childDevice}]"
                        
                        //Send the html automatically
			childDevice.setGraph("${state.localEndpointURL}${location}/?access_token=${state.endpointSecret}");
			log.info "Sent setGraph: ${state.localEndpointURL}${location}/?access_token=${state.endpointSecret}"
                        }
                else {
                        
                        childDevice.label = device_name;
                        log.info "Label Updated to [${device_name}]"
                        
                        //Send the html automatically
                    childDevice.setGraph("${state.localEndpointURL}${location}/?access_token=${state.endpointSecret}");
                    log.info "Sent setGraph: ${state.localEndpointURL}${location}/?access_token=${state.endpointSecret}"
                }
        }
}

def hubiTools_validate_order(child, all) {
    child.call(){
        def order = [];
        sensors.eachWithIndex {sensor, idx ->
            order << settings["displayOrder_${sensor.id}"];
        }
    
        //if we are initialized and need to check
        if(state.lastOrder && state.lastOrder[0]) {
            def remains = all.findAll { !order.contains(it) }
    
            def dupes = [];
        
            order.each {ord ->
                if(order.count(ord) > 1) dupes << ord;
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
}

def hubiTools_rotating_colors(c){
    
    ret = "#FFFFFF"
    color = c % 13;
    switch (color){
        case 0: return hubiTools_get_color_code("RED"); break;
        case 1: return hubiTools_get_color_code("GREEN"); break;
        case 2: return hubiTools_get_color_code("BLUE"); break;
        case 3: return hubiTools_get_color_code("MAROON"); break;
        case 4: return hubiTools_get_color_code("YELLOW"); break;
        case 5: return hubiTools_get_color_code("OLIVE"); break;
        case 6: return hubiTools_get_color_code("AQUA"); break;
        case 7: return hubiTools_get_color_code("LIME"); break;
        case 8: return hubiTools_get_color_code("NAVY"); break;
        case 9: return hubiTools_get_color_code("FUCHSIA"); break;
        case 10: return hubiTools_get_color_code("PURPLE"); break;
        case 11: return hubiTools_get_color_code("TEAL"); break;
        case 12: return hubiTools_get_color_code("ORANGE"); break;
    }
    return ret;
}

def hubiTools_get_color_code(input_color){
    
    new_color = input_color.toUpperCase();
    
    switch (new_color){
        
        case "WHITE" :	return "#FFFFFF"; break;
        case "SILVER" :	return "#C0C0C0"; break;
        case "GRAY" :	return "#808080"; break;
        case "BLACK" :	return "#000000"; break;
        case "RED" :	return "#FF0000"; break;
        case "MAROON" :	return "#800000"; break;
        case "YELLOW" :	return "#FFFF00"; break;
        case "OLIVE" :	return "#808000"; break;
        case "LIME" :	return "#00FF00"; break;
        case "GREEN" :	return "#008000"; break;
        case "AQUA" :	return "#00FFFF"; break;
        case "TEAL" :	return "#008080"; break;
        case "BLUE" :	return "#0000FF"; break;
        case "NAVY" :	return "#000080"; break;
        case "FUCHSIA" :return "#FF00FF"; break;
        case "PURPLE" :	return "#800080"; break;
    }
}
   
def hubiTools_get_name_from_id(id, sensors){
    
    def return_val = "Error"
    
    sensors.each { sensor ->
        if (id == sensor.id) {  
            return_val = sensor.displayName;
        }
    }
    return return_val;
    
}

def hubiTools_get_order(order){
    
    split_ = order.replace('"', '').replace('[', '').replace(']', '').replace("attribute_", "").split(',');
    list_ = [];
    split_.each{device->
        sub_ = device.split('_');
        list_ << [id: sub_[0], attribute:sub_[1]]; 
    }
    return list_;    
}

def hubiTools_check_list(child, sensors, list_){
    
    result = true;
    count_ = 0;
    //check for addition/changes
    sensors.each { sensor ->
                     id = sensor.id;
                     
                     attributes = settings["attributes_${sensor.id}"];
                     attributes.each { attribute ->
                         count_ ++;
                         inner_result = false;
                         for (i=0; i<list_.size(); i++){
                             if (list_[i].id == id && list_[i].attribute == attribute){
                                  inner_result = true;   
                             }
                         }
                         result = result & inner_result;
                     }
    }   
    //check for smaller
    count_result = false;
    if (list_.size() == count_){
        count_result = true;
    }
    return (result & count_result);  
    
}

/********************************************************************************************************************************************
*********************************************************************************************************************************************
***************************************************  END HELPER FUNCTIONS  ******************************************************************
*********************************************************************************************************************************************
*********************************************************************************************************************************************/
