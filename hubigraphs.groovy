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


preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "Graph Creator", install: true, uninstall: true,submitOnChange: true) {
        section {
            app(name: "hubiGraphLine", appName: "Hubigraph Line Graph", namespace: "tchoward", title: "Create New Line Graph", multiple: true)
            app(name: "hubiBarGraph", appName: "Hubigraph Bar Graph", namespace: "tchoward", title: "Create New Bar Graph", multiple: true)
			app(name: "hubiRangeBar", appName: "Hubigraph Range Bar", namespace: "tchoward", title: "Create New Range Bar", multiple: true)
            app(name: "hubiGraphTime", appName: "Hubigraph Time Line", namespace: "tchoward", title: "Create New Time Line", multiple: true)
            app(name: "hubiGauge", appName: "Hubigraph Gauge", namespace: "tchoward", title: "Create New Gauge", multiple: true)
        }
    }
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

//addContainer
def hubiForm_container(child, containers, numPerRow){
    
        child.call(){
                def html_ = 
                        """
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
}

//add_text
def hubiForm_text(child, text){
    child.call(){
        def html_ = "$text";
    
        return html_
    }
}

// specialPageButton
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



//specialSection
def hubiForm_section(child, title, pos, icon="", Closure code) {
        child.call(){
                def id = title.replace(' ', '_');

                def titleHTML = """
                        <div class="mdl-layout__header" style="display: block; background:#033673; margin: 0 -16px; width: calc(100% + 32px); position: relative; z-index: ${pos}; overflow: visible;">          
                        <div class="mdl-layout__header-row">
                                <span class="mdl-layout__title" style="margin-left: -32px; font-size: 20px; width: auto;">
                                        ${title}
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


//specialSwitch
def hubiForm_switch(child, title, var, defaultVal, submitOnChange){
         child.call(){
               
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
                
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
 }

//specialTextInput
def hubiForm_text_input(child, title, var, defaultVal, submitOnChange){
    
     child.call(){
         settings[var] = settings[var] != null ? settings[var] : defaultVal;
        
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
}

//fontSizeSelector
def hubiForm_font_size(child, title, varname, defaultSize, min, max){
    
    child.call(){
        def fontSize;
        def varFontSize = "${varname}_font"
        
        settings[varFontSize] = settings[varFontSize] ? settings[varFontSize] : defaultSize;
        
        def html_ = 
                """
                <table style="width:100%">
                <tr><td><label for="settings[${varFontSize}]" class="control-label"><b>${title} Font Size</b></td>
                        <td style="text-align:right; font-size:${settings[varFontSize]}px">Font Size: ${settings[varFontSize]}</td>
                        </label>
                </tr>
                </table>
                <input type="range" min = "$min" max = "$max" name="settings[${varFontSize}]" class="mdl-slider submitOnChange " value="${settings[varFontSize]}" id="settings[${varFontSize}]">
                <div class="form-group">
                        <input type="hidden" name="${varFontSize}.type" value="number">
                        <input type="hidden" name="${varFontSize}.multiple" value="false">
                </div>
                """
        
        return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
    }
}

//lineSizeSlector
def hubiForm_line_size(child, title, varname, defaultSize, min, max){
    
    child.call(){
        def fontSize;
        def varLineSize = "${varname}_line_size"
        
        settings[varLineSize] = settings[varLineSize] ? settings[varLineSize] : defaultSize;
        
        def html_ =
                """
                <table style="width:100%">
                <tr><td><label for="settings[${varLineSize}]" class="control-label"><b>${title} Line Size</b></td>
                        <td border=1 style="text-align:right;">Line Size: ${settings[varLineSize]}<hr style='background-color:#1A77C9; height:${settings[varLineSize]}px; border: 0;'></td>
                        </label>
                </tr>
                </table>
                <input type="range" min = "$min" max = "$max" name="settings[${varLineSize}]" class="mdl-slider submitOnChange "value="${settings[varLineSize]}" id="settings[${varLineSize}]">
                <div class="form-group">
                        <input type="hidden" name="${varLineSize}.type" value="number">
                        <input type="hidden" name="${varLineSize}.multiple" value="false">
                </div>
                """
        
        return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
    } 
    
}

//sliderSelector
def hubiForm_slider(child, title, varname, defaultSize, min, max, units=""){
    
    child.call(){
        def fontSize;
        def varSize = "${varname}"
        
        settings[varSize] = settings[varSize] ? settings[varSize] : defaultSize;
        
        def html_ =
                """
                <table style="width:100%">
                <tr><td><label for="settings[${varSize}]" class="control-label"><b>${title}</b></td>
                        <td border=1 style="text-align:right;">Value: ${settings[varSize]}${units}</td>
                        </label>
                </tr>
                </table>
                <input type="range" min = "$min" max = "$max" name="settings[${varSize}]" class="mdl-slider submitOnChange "value="${settings[varSize]}" id="settings[${varSize}]">
                <div class="form-group">
                        <input type="hidden" name="${varSize}.type" value="number">
                        <input type="hidden" name="${varSize}.multiple" value="false">
                </div>
                """
        
        return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
    }
}

//colorSelector
def hubiForm_color(child, title, varname, defaultColorValue, defaultTransparentValue){
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
                """;
                
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
}

//graphPreview
def hubiForm_graph_preview(child){
        child.call(){
                if (!state.count_) state.count_ = 5;
                
                def html_ =
                         """
                        <iframe id="preview" style="width: 100%; position: relative; z-index: 1; height: 100%; background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAEq2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIKICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIKICAgIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIgogICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgZXhpZjpQaXhlbFhEaW1lbnNpb249IjIiCiAgIGV4aWY6UGl4ZWxZRGltZW5zaW9uPSIyIgogICBleGlmOkNvbG9yU3BhY2U9IjEiCiAgIHRpZmY6SW1hZ2VXaWR0aD0iMiIKICAgdGlmZjpJbWFnZUxlbmd0aD0iMiIKICAgdGlmZjpSZXNvbHV0aW9uVW5pdD0iMiIKICAgdGlmZjpYUmVzb2x1dGlvbj0iNzIuMCIKICAgdGlmZjpZUmVzb2x1dGlvbj0iNzIuMCIKICAgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIKICAgcGhvdG9zaG9wOklDQ1Byb2ZpbGU9InNSR0IgSUVDNjE5NjYtMi4xIgogICB4bXA6TW9kaWZ5RGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIKICAgeG1wOk1ldGFkYXRhRGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCI+CiAgIDx4bXBNTTpIaXN0b3J5PgogICAgPHJkZjpTZXE+CiAgICAgPHJkZjpsaQogICAgICBzdEV2dDphY3Rpb249InByb2R1Y2VkIgogICAgICBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZmZpbml0eSBQaG90byAxLjguMyIKICAgICAgc3RFdnQ6d2hlbj0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIvPgogICAgPC9yZGY6U2VxPgogICA8L3htcE1NOkhpc3Rvcnk+CiAgPC9yZGY6RGVzY3JpcHRpb24+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8+IC4TuwAAAYRpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZE7SwNBFEaPiRrxQQQFLSyCRiuVGEG0sUjwBWqRRPDVbDYvIYnLboIEW8E2oCDa+Cr0F2grWAuCoghiZWGtaKOy3k2EBIkzzL2Hb+ZeZr4BWyippoxqD6TSGT0w4XPNLyy6HM/UYqONfroU1dBmguMh/h0fd1RZ+abP6vX/uYqjIRI1VKiqEx5VNT0jPCk8vZbRLN4WblUTSkT4VLhXlwsK31p6uMgvFseL/GWxHgr4wdYs7IqXcbiM1YSeEpaX404ls+rvfayXNEbTc0HJnbI6MAgwgQ8XU4zhZ4gBRiQO0YdXHBoQ7yrXewr1s6xKrSpRI4fOCnESZOgVNSvdo5JjokdlJslZ/v/11YgNeovdG31Q82Sab93g2ILvvGl+Hprm9xHYH+EiXapfPYDhd9HzJc29D84NOLssaeEdON+E9gdN0ZWCZJdli8Xg9QSaFqDlGuqXip797nN8D6F1+aor2N2DHjnvXP4Bhcln9Ef7rWMAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAXSURBVAiZY7hw4cL///8Z////f/HiRQBMEQrfQiLDpgAAAABJRU5ErkJggg=='); background-size: 25px; background-repeat: repeat; image-rendering: pixelated;" src="${state.localEndpointURL}graph/?access_token=${state.endpointSecret}" data-fullscreen="false" onload="(() => {
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
                                                const h = 100 * ${state.count_.floatValue()} + 30;
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
                return (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
        }
}

//getSubTitle
def hubiForm_sub_section(child, myText=""){
        child.call(){
                def newText = myText.replaceAll( /'/, '' )
                def html_ = 
                        """
                        <div class="mdl-layout__header" style="display: block; min-height: 0;">
                                <div class="mdl-layout__header-row" style="height: 48px;">
                                <span class="mdl-layout__title" style="margin-left: -32px; font-size: 9px; width: auto;">
                                        <h5 style="font-size: 16px;">${newText}</h5>
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
                        <div class = "mdl-grid" style="margin: 0; padding: 0;"> 
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


//createHubiGraphTile
def hubiTool_create_tile(child) {
	child.call(){

                log.info "Creating HubiGraph Child Device"
        
                def childDevice = getChildDevice("HUBIGRAPH_${app.id}");     
                log.debug(childDevice);
                
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
   
def hubiForm_list_reorder(child, var, list_data) {
        child.call(){
            def html_ = 
               """
                <script src="/local/DragDropTouch.js"></script>
                <div id = "moveable" class = "mdl-grid" style="margin: 0; padding: 0; text-color: white !important"> 
               """
               
                list_data.each{data->
                    color_ = settings["${data.var}_background_color"];
                    id_ = "${data.var}"
                    var_ = "${data.var}_data_order"
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
               <input type="hidden" name="${var}.type" value="text">
               <input type="hidden" name="${var}.multiple" value="">"""
            
               html_ = html_.replace('\t', '').replace('\n', '').replace('  ', '');
                  
               paragraph (html_);
        }
}
/********************************************************************************************************************************************
*********************************************************************************************************************************************
***************************************************  END HELPER FUNCTIONS  ******************************************************************
*********************************************************************************************************************************************
*********************************************************************************************************************************************/
