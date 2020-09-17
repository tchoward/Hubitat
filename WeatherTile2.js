//Global Variables
let options = [];
var focusTile;
var dialog = document.getElementById('tileOptions');
var newTileDialog = document.getElementById('addTileDialog');
var count = 12;
const num_columns = 26;
const num_rows = 26;

function getOptions() {
    return jQuery.get(localURL + "getOptions/?access_token=" + secretEndpoint, (data) => {
        options = data;

        console.log("Got Options");
        console.log(options);
    });
}

function getAbbrev(unit){
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

async function initializeWeather() {

    await getOptions();

    console.log(options);
    setInterval(() => {
        getWeatherData();
    }, options.openweather_refresh_rate);
    let minutes = (options.openweather_refresh_rate / 1000) / 60;
    console.log("Refreshing OpenWeather Forecast at " + minutes + " minutes");

    grid.engine.nodes.forEach(function (item) {
       setFont(item);
    });
}

function lookupData(data, value) {
    split_val = value.split('.');
    cur = data;
    try {
        split_val.forEach((val) => {
            cur = cur[val];
        })
    } catch (error) {
        return "";
    }
    return cur;
}



/******************************************************************************************************************************/
/****************************************GRIDSTACK IMPLEMENTATION *************************************************************/
/******************************************************************************************************************************/




var grid = GridStack.init({
    alwaysShowResizeHandle: /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
        navigator.userAgent
    ),

    doAnimate: 'true',
    removable: '#trash',
    removeTimeout: 100,
    acceptWidgets: '.newWidget',
    verticalMargin: 1,
    float: 'true',
    disableOneColumnMode: 'true',
    cellHeight: "4vh",
    maxRow: 45,
});

grid.on('added change', function (e, items) {
    var str = '';
    items.forEach(function (item) {
        if (item.id != undefined){
            obj = getTile(item.id);
            obj.baseline_row = item.y;
            obj.baseline_column = item.x;
            obj.h = item.height;
            obj.w = item.width;

            setFont(item);

        }
            
    });
    updateGroovy();
});

//HELPER FUNCTIONS
function hexToRgb(hex) {
    var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16)
    } : null;
}

function rgb2hex(rgb) {
    if (/^#[0-9A-F]{6}$/i.test(rgb)) return rgb;

    rgb = rgb.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);
    function hex(x) {
        return ("0" + parseInt(x).toString(16)).slice(-2);
    }
    return "#" + hex(rgb[1]) + hex(rgb[2]) + hex(rgb[3]);
}

//SET Functions
function setText(val, id) {

    let item = grid.engine.nodes.find(function (element) {
        return (element.id == id);
    });
    if (!item) {
        console.log("Returning (" + val + ", " + id + ")");
        return;
    }

    let wPixels = window.innerWidth;
    let hPixels = window.innerHeight;
    let column_size = wPixels / (num_columns - 2);
    let row_size = hPixels / num_rows;

    let textWidth = 0;
    let obj = getTile(id);

    let el = document.getElementById(id);
    if (el) {
        if (val != null) textWidth = val.length;
        else {
            textWidth = el.textContent.length;
        }
    }

    let text = document.getElementById(id + "_text");
    if (text) textWidth += text.textContent.length;


    let icon = document.getElementById(id + "_icon");
    if (icon) textWidth += icon.textContent.length;
    if (obj.icon != "none") textWidth++; 

    let units = document.getElementById(id + "_units");
    if (units) textWidth += units.textContent.length;

    let unit_space = document.getElementById(id + "_unit_space");
    if (unit_space) textWidth += unit_space.textContent.length;
    


    let fontSize = row_size * item.height;
    let lineHeight = fontSize;


    let pixelWidth = column_size * item.width;
    let fontWidth = fontSize * textWidth;


    if (pixelWidth < fontWidth) {
        fontSize = 1.25 * (pixelWidth / textWidth);
    }

    

    change = obj.font_adjustment;

    fontSize = fontSize + (fontSize * (change / 100.0));
    fontWeight = obj.font_weight;

    if (el) {
        if (val != null) 
            el.textContent = val;
        
             
        el.style.fontSize = fontSize + "px";
        el.style.lineHeight = lineHeight + "px";
        el.style.fontWeight = fontWeight;
    }
    if (text) {
        text.style.fontSize = fontSize + "px";
        text.style.lineHeight = lineHeight + "px";
        text.style.fontWeight = fontWeight;
    }
    if (icon) {
        icon.style.fontSize = fontSize + "px";
        icon.style.lineHeight = lineHeight + "px";
        icon.style.fontWeight = fontWeight;
    }
    if (units) {
        units.style.fontSize = (fontSize + "px");
        units.style.lineHeight = lineHeight + "px";
        units.style.fontWeight = fontWeight;
    }
    if (unit_space){
        unit_space.style.fontSize = (fontSize + "px");
        unit_space.style.lineHeight = lineHeight + "px";
        unit_space.style.fontWeight = fontWeight;
    }

}

function setFont(grid) {

    let item = document.getElementById(grid.id);
    setText(null, item.id)
}

function setSlider(id, units, val) {
    document.getElementById(id + "_slider").value = val;
    document.getElementById(id + "_message").innerHTML = val + units;

    $("#" + id + "_slider")[0].MaterialSlider.change(val);

}

function setColor(color, opacity, id) {
    rgb = hexToRgb(color);

    colorString = "rgba(" + rgb.r + ", " + rgb.g + ", " + rgb.b + ", " + (opacity / 100) + ")"

    let el = document.getElementById(id);
    let icon = document.getElementById(id + "_icon");
    let unit = document.getElementById(id + "_units");
    let text = document.getElementById(id + "_text")
    if (el) el.style.color = colorString;
    if (icon) icon.style.color = colorString;
    if (unit) unit.style.color = colorString;
    if (text) text.style.color = colorString;
}

function setbkColor(color, opacity, id) {

    $('#' + id).css('background-color', color);
    $('#' + id).css('opacity', opacity / 100);
}

function setTileText(text, id) {
    let el = document.getElementById(id);
    if (el) el.textContent = text;
}

function setAlignment(alignment, id) {
    $('#' + id).css('text-align', alignment);
}

function setSpacingIcon(space, menu){

    let icon = "arrow-collapse-horizontal";
    document.getElementById(menu+"_value").textContent = "no space";
    if (space == " ") {
        icon = "keyboard-space";
        document.getElementById(menu+"_value").textContent = "single space";
    }
    if (space == "  "){
        icon = "arrow-expand-horizontal";
        document.getElementById(menu+"_value").textContent = "double space";
    }

    replaceIcons(menu+"_button", icon);
}

function setDecimalIcon(decimal, menu){

    let icon = "hexadecimal";
    document.getElementById(menu+"_value").textContent = "no decimal";
    if (decimal == 1) {
        icon = "surround-sound-2-0";
        document.getElementById(menu+"_value").textContent = "one decimal";
    }
    if (decimal == 2) {
        icon = "decimal";
        document.getElementById(menu+"_value").textContent = "two decimals";
    }

    replaceIcons(menu+"_button", icon);
}

function setSpacing(item_name, spacing){
    text = document.getElementById(item_name);
    switch (spacing.toLowerCase()){
        case "no space": text.textContent = ""; return "";
        case "single space": text.textContent = " "; return " ";
        case "double space": text.textContent = "  "; return "  ";
    }
    return "";

}

function setButton(t1, t2, t3) {
    let b1 = $('#' + t1 + "_justify_button");
    let b2 = $('#' + t2 + "_justify_button");
    let b3 = $('#' + t3 + "_justify_button");


    b1.addClass('mdl-button--colored');
    b2.removeClass('mdl-button--colored');
    b3.removeClass('mdl-button--colored');
}


//GET Functions
function getTile(name){
    
    tile = options.tiles.find(tile=> tile.var == name);
    if (tile==undefined){
        console.log("Could not find "+name);
        console.log(options.tiles);
    }
    return tile;
}

function getJustification() {
    if ($('#left_justify_button').hasClass("mdl-button--colored")) return "left";
    if ($('#mid_justify_button').hasClass("mdl-button--colored")) return "center";
    if ($('#right_justify_button').hasClass("mdl-button--colored")) return "right";
}

function getAutoFont() {
    let temp = $('#tileStaticFont').hasClass('checked');
    return temp;
}

function getFontWeight(val) {
    switch (val) {
        case "thin": return 100;
        case "normal": return 400;
        case "bold": return 700;
        case "thick": return 900;
    }
    return 400;
}

function getFontWeightName(val) {

    if (typeof val === 'string' || val instanceof String)
        intval = parseInt(val, 10);
    else
        intval = val;

    switch (intval) {
        case 100: return "thin"; break;
        case 400: return "normal"; break;
        case 700: return "bold"; break;
        case 900: return "thick"; break;
    }
    return "normal";
}

function getDecimals(text){
    switch (text.toLowerCase()){
        case "no decimal": return 0;
        case "one decimal": return 1;
        case "two decimals": return 2;
    }

}


function setupNewTileMenu(){

    options.tiles.forEach((tile) => {
        
        if (tile.display == false || tile.display == "false"){
            $('#' + tile.var + "_list_main").css("display", "flex");
        }
        else {
            $('#' + tile.var + "_list_main").css("display", "none");
        } 
    })
}

function setOptions(tile) {

    obj = getTile(tile);

    //Set the "Options" Title
    el = document.getElementById(tile + "_title");
    title = el.textContent;
    document.getElementById("options_title").textContent = title + " Options"

    //Set the text color
    document.getElementById("text_color").value = rgb2hex(obj.font_color);

    //Set the background color
    document.getElementById("background_color").value = rgb2hex(obj.background_color);

    //Set the static text; the floater requires special care
    text = obj.text;
    if (text) {
        document.getElementById("tileText").value = text;
        $('#text_floater').addClass('is-dirty');
    } else {
        document.getElementById("tileText").value = "";
        $('#text_floater').removeClass('is-dirty');
    }

    //Set the horizontal alignment and its icon
    horizontal_alignment_icon = document.getElementById(obj.alignment + "_icon").textContent;
    document.getElementById("horizontal_alignment_value").textContent = obj.alignment;
    replaceIcons("horizontal_alignment_button", horizontal_alignment_icon);

    //Set the font weight and its icon
    fname = getFontWeightName(obj.font_weight);
    font_weight_icon = document.getElementById(fname+"_icon").textContent;
    document.getElementById("font_weight_value").textContent = fname;
    replaceIcons("font_weight_button", font_weight_icon);

    //Set the main (or not) icon
    main_icon = obj.icon;
    if (main_icon == "none" || obj.icon_loc == "special"){
        main_icon = "alpha-x-circle-outline";
        document.getElementById("selected_icon_value").textContent = "none";
        document.getElementById("selected_icon_icon").textContent = "none";
        document.getElementById("selected_icon_tooltip").textContent = "No Icon Selected";
    } else {
        document.getElementById("selected_icon_value").textContent = main_icon;
        document.getElementById("selected_icon_icon").textContent = main_icon;
        let icon_name = main_icon.replace(/-/g, '_');
        let icon_text = document.getElementById("selected_icon_"+icon_name+"_text").textContent;
        document.getElementById("selected_icon_tooltip").textContent = "Selected Icon: "+icon_text;
        }
    replaceIcons("selected_icon_button", main_icon);
    
    setSpacingIcon(obj.icon_space, "icon_spacing");
    setSpacingIcon(obj.unit_space, "units_spacing");

    
    //Set all the sliders and thier options
    setSlider("font_adjustment", "%", obj.font_adjustment);
    setSlider("text", "%", obj.font_opacity);
    setSlider("background", "%", obj.background_opacity);

    //Set the decimal places 
    decimals = obj.decimals;
    setDecimalIcon(decimals, "decimal_places");

    //Set up the new tile menu by "not showing" the already available tiles
    setupNewTileMenu();

    //Show the dialog
    dialog.showModal();

    //Remember the tile
    focusTile = tile;

}

function closeWindow() {
    dialog.close();
}

function saveWindow() {

    dialog.close();
    let color = document.getElementById("text_color").value;
    let opacity = document.getElementById("text_slider").value;
    setColor(color, opacity, focusTile);

    let bkcolor = document.getElementById("background_color").value;
    let bkopacity = document.getElementById("background_slider").value;
    setbkColor(bkcolor, bkopacity, focusTile + "_tile");

    let text = document.getElementById("tileText").value;
    setTileText(text, focusTile + "_text");

    let horizontal_alignment = document.getElementById("horizontal_alignment_value").textContent;
    setAlignment(horizontal_alignment, focusTile + "_tile");

    let font_adjustment = document.getElementById('font_adjustment_slider').value;

    let font_weight = getFontWeight(document.getElementById("font_weight_value").textContent);

    let icon = document.getElementById("selected_icon_icon").textContent;
    if (icon == "alpha-x-circle-outline"){
        replaceIcons(focusTile+"_icon", "none");
    }
    else {
        replaceIcons(focusTile+"_icon", icon);
    }

    let icon_spacing = document.getElementById("icon_spacing_value").textContent;
    icon_spacing = setSpacing(focusTile+"_icon", icon_spacing);
    
    let unit_spacing = document.getElementById("units_spacing_value").textContent;
    unit_spacing = setSpacing(focusTile+"_unit_space", unit_spacing);

    let decimals = getDecimals(document.getElementById("decimal_places_value").textContent);

    obj = getTile(focusTile);
    if (obj.value==undefined) {
      obj.value="";
    }
    obj.font_color = color;
    obj.font_opacity = opacity;
    obj.background_color = bkcolor;
    obj.background_opacity = bkopacity;
    obj.text = text;
    obj.alignment = horizontal_alignment;
    obj.font_adjustment = font_adjustment;
    obj.font_weight = font_weight;
    obj.icon = icon;
    obj.icon_space = icon_spacing;
    obj.unit_space = unit_spacing;
    obj.decimals = decimals;

    setText(null, focusTile);
    updateGroovy();
    setTimeout(() => { getWeatherData() } , 1000);

}

function saveAllWindow() {

    dialog.close();
    
    let color = document.getElementById("text_color").value;
    let opacity = document.getElementById("text_slider").value;
    let bkcolor = document.getElementById("background_color").value;
    let bkopacity = document.getElementById("background_slider").value;

    options.tiles.forEach((obj) => {
        if (!obj.display == false || !obj.display == "false"){
            id = obj.var;
            setColor(color, opacity, id);
            setbkColor(bkcolor, bkopacity, id + "_tile");

            obj.font_color = color;
            obj.font_opacity = opacity;
            obj.background_color = bkcolor;
            obj.background_opacity = bkopacity;
        }
    });
   
    updateGroovy();
}


function buttonClicked(justification) {

    switch (justification) {
        case 'left':
            setButton('left', 'right', 'center');
            break;

        case 'right':
            setButton('right', 'left', 'center');
            break;

        case 'center':
            setButton('center', 'right', 'left');
            break;
    }
}

function replaceIcons(id, icon) {

    $("#" + id).removeClass(function (index, className) {
        return (className.match(/(^|\s)mdi-\S+/g) || []).join(' ');
    });

    $("#" + id).addClass("mdi-" + icon);

}

jQuery(document).ready(function ($) {

    /**
     * Example 1
     * Load from SELECT field
     */
    $('#e1_element').fontIconPicker();

});

function deleteTile(){
    dialog.close();
    let id = focusTile;
    let item = grid.engine.nodes.find(function (element) {
        return (element.id == id);
    });

    options.tiles = options.tiles.filter(function(tile){ return tile.var != focusTile; });
    
    //Remove the Physical Widget
    grid.removeWidget(item.el);

    updateGroovy();
}

function addNewTile(var_name, tile_type, weather_code, span, time) {
    
    length = options.tiles.length;
    let type = options.tile_type[tile_type];
    console.log(var_name+" "+tile_type);
    console.log(type);

    title = type.name;
    if (type.type == "blank") title = "Blank Tile";
    else {
        switch (span){
            case "daily": title = "Daily "+type.name+" in "+time+" days";
            break;
            case "hourly": title = "Hourly "+type.name+" in "+time+" hours";
            break;
            case "current": title = "Current "+type.name;
            break;
        }
    }
     
    console.log("Adding Tile "+tile_type)
    let icon_var = "none";
    let value_var= "XXX"
    if (tile_type == "weather_icon"){
        icon_var = "help-circle-outline"
        value_var = "";
    }
    options.tiles.push( {title: title,  var: var_name, type: tile_type,  period: weather_code,  value: value_var,           
                        icon: icon_var, icon_loc: "left",  icon_space: "",  
                        h: 4,  w: 8, baseline_row: 1,  baseline_column:  1, 
                        alignment: "center", text: "",
                        lpad: 0, rpad: 0,  decimals: 1, 
                        unit: "none",   decimal: "no",  unit_space: "",
                        font: 6, font_weight: "400", 
                        font_color: "#2c3e50", font_opacity: "100", background_color: "#18bc9c", background_opacity: "100", 
                        font_auto_resize: "true", justification: "center", font_adjustment: 0, display: true, });
    
                        units = options.tile_units[type.type];
    
    obj = getTile(var_name);

    var $el = $(createNewTile(obj, units.out)); 

    grid.addWidget($el, 0, 0, obj.w, obj.h, true);

    setText( null, var_name );
}

function createNewTile(item, units){

    let fontScale = 4.6;
    let lineScale = 0.85;
    let iconScale = 3.5;
    let header = 0.1;
    let var_ = item.var;
    let height = item.h;

    let html = "";
    html += `<div id="${var_}_tile_main" class="grid-stack-item" data-gs-id = "${var_}" data-gs-x="${item.baseline_column}" 
                    data-gs-y="${item.baseline_row}" data-gs-width="${item.w}" data-gs-height="${item.w}" data-gs-locked="false"
                                                  ondblclick="setOptions('${var_}')">

                    <div id="${var_}_title" style="display: none;">${item.title}</div>
                    <div id="${var_}_font_adjustment" style="display: none;">${item.font_adjustment}</div>
                    <div class="mdl-tooltip" for="${var_}_tile_main" style="background-color: rgba(255,255,255,0.75); color: rgba(0,0,0,100);">${item.title}</div>

                    <div id="${var_}_tile" class="grid-stack-item-content" style="font-size: ${fontScale*height}vh; 
                                                                           line-height: ${fontScale*lineScale*height}vh;
                                                                           text-align: ${item.justification};
                                                                           background-color: ${item.background_color};
                                                                           font-weight: ${item.font_weight};"> `;
        
    //Left Icon
    if (item.icon != "right"){
        html+=  `<span id="${var_}_icon" class="mdi mdi-${item.icon}" style="font-size: ${iconScale*height}vh; color: ${item.font_color};">${item.icon_space}</span>`;
    }
    //Text
    html+=`<span id="${var_}_text" style="color: ${item.font_color};">${item.text}</span>`;
        
    //Main Content
    html += `<span id="${var_}" style="color: ${item.font_color};">${item.value}</span>`;
    
    //Unit Spacing
    html += `<span id="${var_}_unit_space">${item.unit_space}</span>`;

    //Units
    let out_units = getAbbrev(units);
    html += `<span id="${var_}_units" style="color: ${item.font_color};">${out_units}</span>`;  

    

    //Right Icon
    if (item.icon_loc == "right"){
        html+=`<span>${item.icon_space}</span>`;
        html+=`<span id="${var_}_icon" class="mdi mdi-${item.icon}" style="color: ${item.font_color};"></span>`;
    }
    html += `</div></div>`;
    
    return html;

}

function addNewTileClose(){

    newTileDialog.close();

    let num_elements = options.tiles.length;
    let var_name = "";

    console.log(selected_new_tile_span+" "+selected_new_tile_type+" "+selected_new_tile_time);

    //Error Checking
    diag = options.new_tile_dialog;
    type = selected_new_tile_type;
    if ((selected_new_tile_span != "blank") &&
        ((selected_new_tile_span == undefined) || 
         (selected_new_tile_type == undefined) ||
         (selected_new_tile_span != "current" && selected_new_tile_time == undefined)) &&
         (selected_new_tile_span != "sensor" )){
        window.alert("Missing Required Fields, Exitting...")
        return;
    }

    if (selected_new_tile_span=="blank"){
        var_name = "blank"+num_elements;
        type = "blank";
        period = 0;
    } else {
        var_name = selected_new_tile_type+"_"+num_elements;
        type = selected_new_tile_type;
        period = selected_new_tile_time ? selected_new_tile_span+"."+selected_new_tile_time : selected_new_tile_span;
    } 
    
    addNewTile(var_name, type, period, selected_new_tile_span, selected_new_tile_time);

    updateGroovy();
    setTimeout(() => { getWeatherData() } , 1000);
}

function updateGroovy() {
    let data =  JSON.stringify(options.tiles);
    let el = jQuery("#settingstile_settings_HTML", parent.document);
    el.attr('value', data);

    $.ajax({
        type: 'POST',
        url: options.url+`/updateSettings/?access_token=`+options.api_code,
        data: data,
        contentType: 'application/json'
    });

}

function getWeatherData() {
    return jQuery.get(localURL + "getData/?access_token=" + secretEndpoint, (data) => {

        data.forEach(tile=>{
            obj = getTile(tile.var);
            if (obj!=undefined){
                obj.value = tile.value;
                obj.icon = tile.icon; 
                
                replaceIcons(tile.var+"_icon", obj.icon);
                setText(tile.value, tile.var);   
            }

        });
        console.log(options.tiles)
    });

}

function newTile(){
    dialog.close();
    newTileDialog.showModal();
}

//Just close the window with no action
function closeAddTileWindow(){
    newTileDialog.close();
}


var selected_new_tile_span;
var selected_new_tile_type;
var selected_new_tile_time;


function selectTileSpan(tileSpan){
    selected_new_tile_span = tileSpan;

    diag = options.new_tile_dialog;
    Object.entries(diag).forEach(([key, value]) => {
        css_text = "none"
        if (key == tileSpan){ 
            css_text = "flex";
            $("#"+key+"_measurement").val("blank");
            $("#"+key+"time").val("blank");
        }
        $("#"+key+"_measurement_main").css("display", css_text);
        $("#"+key+"_time_main").css("display", css_text);
    })
    selected_new_tile_type = undefined;
    selected_new_tile_time = undefined;

}

function selectTileType(tileType){
    selected_new_tile_type = tileType;
}

function selectTileTime(tileTime){
    selected_new_tile_time = tileTime;
}
