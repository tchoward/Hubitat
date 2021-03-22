//Global Variables
var draw;
var background;
var widget_group;

var grid;

var gridSpaceX;
var gridSpaceY;

var sensor_data;
var ow_data;
var orig_widget_data;
var widget_data;
var items;
var current_values = new Map();
var pendingUpdate = false;

var sunrise;
var sunset;

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

function convertUnits(val, in_units, out_units){
    
  if (in_units == out_units) return val;

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
            case "cardinal":
            val = getWindDirection(val); 
            break;
            case "radians": val = (val * 3.1415926535) * 180.0; break;
            default: val = "UNSUPPORTED";
        } 
    break;
    case "radians":
        switch (out_units) {
            case "cardinal":  
            val = getWindDirection(((val * 3.1415926535) * 180.0)); 
            break;
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

    //TEXT CONVERSIONS
    case "time_seconds":
      v = val*1000.0;
      d = new Date(v);

      switch (out_units) {

        case "time_twelve_long":
          val = customFormatTime( d, "h:mm AMPM");  
        break;
        case "time_military_long":
          val = customFormatTime( d, "hhh:mm ");  
        break;
        case "time_twelve_med":
          val = customFormatTime( d, "h:mm");  
        break;
        case "time_twelve_short":
          val = customFormatTime( d, "h");  
        break;
        case "time_military_short":
          val = customFormatTime( d, "hhh");  
        break;

        case "time_day_long":
          val = customFormatDate( d, "DDDD" );
        break;
        case "time_day_initial":
          val = customFormatDate( d, "d" );
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

function customFormatDate (date, formatString){

  var YYYY,YY,MMMM,MMM,MM,M,DDDD,DDD,DD,D,d,hhhh,hhh,hh,h,mm,m,ss,s,ampm,AMPM,dMod,th;
  
  YY = ((YYYY=date.getFullYear())+"").slice(-2);
  MM = (M = date.getMonth()+1)<10 ? ('0'+M) : M;
  MMM = (MMMM= ["January","February","March","April","May","June","July","August","September","October","November","December"] [M-1] ).substring(0,3);
  DD = ( D=date.getDate())<10 ? ('0'+D) : D;
  DDD = ( DDDD= ["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"][date.getDay()]).substring(0,3);
  th = ( D>=10 && D<=20 ) ? 'th' : ((dMod=D%10)==1) ? 'st' : (dMod==2) ? 'nd' : (dMod==3) ? 'rd' : 'th';
  d = ["S","M","T","W","Th","F","Sa"][date.getDay()];
  
  return formatString.replace("YYYY",YYYY)
                             .replace("YY",YY)
                             .replace("MMMM",MMMM)
                             .replace("MMM",MMM)
                             .replace("MM",MM)
                             .replace("M", M)
                             .replace("DDDD",DDDD)
                             .replace("DDD",DDD)
                             .replace("DD",DD)
                             .replace("D",D)
                             .replace("d", d)
                             .replace("th",th);
}

function customFormatTime (date, formatString){
  var YYYY,YY,MMMM,MMM,MM,M,DDDD,DDD,DD,D,d,hhhh,hhh,hh,h,mm,m,ss,s,ampm,AMPM,dMod,th;

  h=(hhh=date.getHours());

  if (h==0) h=24;
  if (h>12) h-=12;
  hh = h<10?('0'+h):h;
  hhhh = hhh<10?('0'+hhh):hhh;
  AMPM=(ampm=hhh<12?'am':'pm').toUpperCase();
  mm=(m=date.getMinutes())<10?('0'+m):m;
  ss=(s=date.getSeconds())<10?('0'+s):s;
  return formatString.replace("hhhh", hhhh).replace("hhh",hhh).replace("hh",hh).replace("h",h).replace("mm",mm).replace("m",m).replace("ss",ss).replace("s",s).replace("ampm",ampm).replace("AMPM",AMPM);

}

function getIcon(condition){

    icons = widget_data.get('icons');
    
    return icons.filter(icon=> (
       icon.conditions.includes(condition)
    ))
}

function getIconByName(name){
  icons = widget_data.get('icons');
    
  return icons.filter(icon=> (
    icon.title.includes(name)
  ))
}

function convertValue(content, value){

  var str = "";
  
  content.conversion.forEach((i)=>{
    str += "if ("+i.condition.replaceAll("%value%", "value")+") return '"+i.return+"';\n";
  });
  str += "return undefined;"
  
  var func = new Function("value",  str);

  return func(value);

}

function parseEvent(event) {

  if (event.deviceId  == 0) return;

  widgets = widget_data.get('widgets');

  var value, decimals;
  
  widgets.forEach((widget) => {
    widget.content.forEach((content) => {
      if (content.device_id != undefined &&
          content.type == "sensor" && 
          content.device_id == event.deviceId && 
          content.attribute == event.name){
            
            let value = 0;
            if (content.conversion != undefined){
              value = convertValue(content, event.value);  
            } else {
              value = event.value;
            }
            current_values.set(widget.name, value);
          }
    });
  });
  
  if (pendingUpdate == false) {
    
    pendingUpdate = true;
    setTimeout(drawWidget, 10000); 
  
  }
}

function update(callback) {
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
  loader.setText('Getting Options (1/4)');
  //await updateOptionData();
  loader.setText('Getting OpenWeather Data (2/4)');
  
  await updateSensorData();
  loader.setText('Drawing chart (4/4)');

  await updateOpenWeatherData();
  loader.setText('Getting Sensor Data (3/4)');

  loader.remove();
  
  update(() => {
    //destroy loader when we are done with it
    console.log("Done with Loader");
    loader.remove();
  });

  //start our update cycle
  if (1) {
    //start websocket
    websocket = new WebSocket("ws://" + location.hostname + "/eventsocket");
    websocket.onopen = () => {
      console.log("WebSocket Opened!");
    }
    websocket.onmessage = (event) => {
      parseEvent(JSON.parse(event.data));
    }
  }

  //attach resize listener
  window.addEventListener("resize", () => {
    drawWidget();
  });

  //Get OpenWeatherData on a schudle
  setInterval(() => {
    updateOpenWeatherData();
  }, 300000);
}

function onBeforeUnload() {
  if (websocket) websocket.close();
}



function getCircleLocation(deg, radius) {

  let rad = deg * Math.PI / 180.0;
  return { x: radius * Math.sin(rad), y: radius * Math.cos(rad) }

}

function formatValue(content, value){
  value = isNaN(value) ? value.replace(/ /g, '') : parseFloat((Math.round(value * 100) / 100));
  value = convertUnits(value, content.input_units, content.output_units)
  decimals = content.decimals == undefined ? 1 : content.decimals;
      
  value = isNaN(value) ? value : Math.abs(value).toFixed(decimals);

  return value;

}

function getLocation(parent_thing, content){

  let size_, location_;

  if (parent_thing.type == "circle"){
    
    size_ = parent_thing.radius * content.size;
    location_ = {
      x: parent_thing.x,
      y: parent_thing.y + parent_thing.radius * (content.location >= 0.5 ? content.location - 0.5 : -1.0 * (0.5 - content.location)),
      wide: false
    } 

  } else if (parent_thing.type == "rectangle"){
    
    size_ = parent_thing.width > parent_thing.height ? parent_thing.height*content.size : parent_thing.width*content.size;

    location_ = {
      x: parent_thing.x + parent_thing.width* (content.location.x >= 0.5 ? content.location.x - 0.5 : -1.0 * (0.5 - content.location.x)),
      y: parent_thing.y + parent_thing.height * (content.location.y >= 0.5 ? content.location.y - 0.5 : -1.0 * (0.5 - content.location.y)),
      wide: parent_thing.width > 1.3*parent_thing.height ? true : false
    }

  }

  return {size: size_, location: location_};

}

function drawText(content, parent_thing, name) {

  let value = current_values.get(name);

  if (value == undefined) value = "XX";
  else {
  
      value = formatValue(content, value);

      value = content.units == undefined ? value : value+content.units;

      val_length = content.units == "%" ? value.length+1 : value.length;

      resize = 1;
      switch (val_length){
          case 1:
          case 2:
              resize = 1;
              break;
          case 3: 
              resize = 0.6;
              break;
          case 4:
              resize = 0.5;
              break;
          default: 
              resize = 0.3;
              break;  
      }
  }

  let stats = getLocation(parent_thing, content)
  if (stats.location.wide){
    resize = 1;
  }
  

  return { obj: draw.text(value).attr({

      fill: content.color

  }).font({

      family: 'Helvetica',
      size: stats.size*resize,
      //size: size,
      style: 'center'

  }).center(stats.location.x, stats.location.y), location: stats.location, size: stats.size};
  
}

function drawIcon(thing, parent_thing, name) {


    let stats = getLocation(parent_thing, thing)
    let icon_name = "NOTHING";

    if (thing.icon != undefined){
      icon = getIconByName(thing.icon)[0];  
    } else {
      icon_name = current_values.get(name);
      icon = getIcon(current_values.get(name))[0];
    }
   
    if (icon == undefined) icon = getIconByName("None");
    
    return {obj: draw.path(icon.path).attr({

      fill: thing.color,
      stroke: {
        color: thing.color,
        linecap: 'round',
        linejoin: 'round'
      }

    }).size(stats.size).center(stats.location.x, stats.location.y).css({ 'fill-rule': 'evenodd', 
                                                                         'clip-rule': 'evenodd',
                                                                         'stroke-linejoin': 'round',
                                                                         'stroke-miterlimit' : 2
                                                                       }), 
      location: stats.location, size: stats.size} 
  
}

function getDistanceToCenter(thing, pixels=true){

  let scale = gridSpaceX < gridSpaceY ? gridSpaceX : gridSpaceY;

  switch (thing.type){
    case "circle":
      return pixels ? ((thing.circle.size.radius)*scale)*0.5 : ((thing.circle.size.radius))*0.5;
      break;
    case "rectangle":
      return pixels ? computeCenterDistance(thing.rectangle.size.x*gridSpaceX, 
                                           thing.rectangle.size.y*gridSpaceY, 
                                           thing.location.degrees) :

                      computeCenterDistance(thing.rectangle.size.x, 
                                            thing.rectangle.size.y, 
                                            thing.location.degrees)                     
      break; 

  }
}

function computeCenterDistance(w, h, angle){
  angle = angle < 0 ? 360+angle : angle;
  
  var h2 = h/2.0;
  var w2 = w/2.0;
  var wh = (Math.sqrt(h2*h2+w2*w2));
  
  if (angle <=22.5){
    return h2;
  } 
  else if (angle <= 67.5){
    return wh;
  }
  else if (angle <= 112.5){
    return w2;
  }
  else if (angle <= 157.5){
    return wh
  }
  else if (angle <= 202.5){
    return h2;
  }
  else if (angle <= 247.5){
    return wh;
  }
  else if (angle <= 292.5){
    return w2;
  }
  else if (angle <= 337.5){
    return wh;
  }
  else return h2;
}

function isUp(angle){
  if (angle <=45){
    return true;
  } 
  else if (angle <= 135){
    return false;
  }
  else if (angle <= 225){
    return true;
  }
  else if (angle <= 315){
    return false
  }
  else return true;
}


function getObjectLocation(child_, parent_){ 

  if (parent_ == null){
    switch (child_.type){
      case "circle":
        return {type: "circle", x: gridSpaceX * child_.location.x, y: gridSpaceY * child_.location.y};
        break;

      case "rectangle":
        return {type: "rectangle", x: gridSpaceX * child_.location.x, y: gridSpaceY * child_.location.y}
        break;
    }
  } else {
    let x,y,radius, loc, parent_radius;
    let  parent_size = parent_.obj.bbox();
    let child_radius = getDistanceToCenter(child_);

    let recessed = child_.location.recessed == undefined ? 0 : child_.location.recessed;

    switch (parent_.type){
      case "circle":
        parent_radius = parent_size.width/2;
        factor = 2;
      break;
      case "rectangle":
        parent_radius = computeCenterDistance(parent_size.width, parent_size.height, child_.location.degrees);
        factor = 2;
      break;
    }

    if (isUp(child_.location.degrees))
      length = parent_radius + child_radius + recessed * gridSpaceY;
    else 
      length = parent_radius + child_radius + recessed * gridSpaceX;

    child_location = getCircleLocation(child_.location.degrees, length);

    shift = ((parent_.location.shift != undefined) && 
             (child_.location.degrees) != 90) ?
                parent_.location.shift : 
                0;

                
    x = parent_size.cx + (child_location.x) - shift;
    y = parent_size.cy + (child_location.y);

    if (child_.type == "rectangle"){
      width = child_.rectangle.size.x*gridSpaceX/2.0;
      child_.location.shift = ((x - width) < parent_size.x) ? parent_size.x + (width-x): 0;
      x += child_.location.shift;
    }

    return {type: child_.type, x: x, y: y};
  }
}
  

function addContent(thing, stats, group){

  dynamic_text = [];

  thing.content.forEach(cont => {


    switch (cont.type) {
      case "text":
        cont.textObj = drawText(cont, stats, thing.name);
        break
      case "sensor":
        if (cont.output_units == "icon"){
          cont.textObj = drawIcon(cont, stats, thing.name);
        } else {
          cont.textObj = drawText(cont, stats, thing.name);
        }
        break;
      case "icon":
        cont.textObj = drawIcon(cont, stats, thing.name);
        break;
    }
    group.add(cont.textObj.obj);
  });

}

function drawRectangle(thing, parent_ = null) {
  
  var group = draw.group();

  stats = getObjectLocation(thing, parent_);
  

  let scale = gridSpaceX < gridSpaceY ? gridSpaceX : gridSpaceY;

  stats.width = thing.rectangle.size.x * gridSpaceX;
  stats.height = thing.rectangle.size.y * gridSpaceY;

  line_width = thing.rectangle.line_width

  rect = draw.rect(stats.width, stats.height).attr({
    fill: thing.backgroundColor,
    stroke: thing.rectangle.color,
    'stroke-width': line_width,
  }).center(stats.x, stats.y);

  if (thing.rectangle.radius != undefined)
    rect.radius(thing.rectangle.radius*scale);

  thing.location.x = stats.x/gridSpaceX;
  thing.location.y = stats.y/gridSpaceY;

  group.add(rect);

  dynamic_text = [];

  addContent(thing, stats, group);

  widget_group.add(group);

  return group;

}

function drawCircle(thing, parent_ = null) {

  var group = draw.group();

  stats = getObjectLocation(thing, parent_);

  let scale = gridSpaceX < gridSpaceY ? gridSpaceX : gridSpaceY;
  stats.radius = thing.circle.size.radius * scale;

  line_width = thing.circle.line_width

  circle = draw.circle(stats.radius).attr({
    cx: stats.x,
    cy: stats.y,
    fill: thing.backgroundColor,
    stroke: thing.circle.color,
    'stroke-width': line_width
  });

  thing.location.x = stats.x/gridSpaceX;
  thing.location.y = stats.y/gridSpaceY;

  group.add(circle);

  addContent(thing, stats, group);

  widget_group.add(group);

  return group;
}

function drawReplicant(thing, parent_ = null) {

}


function polarToCartesian(centerX, centerY, radius, angleInDegrees) {

  var angleInRadians = (angleInDegrees - 90) * Math.PI / 180.0;

  return {
    x: Math.floor(centerX + (radius * Math.cos(angleInRadians))),
    y: Math.floor(centerY + (radius * Math.sin(angleInRadians)))
  };
}

function getGradient(c1, c2) {

  return draw.gradient('linear', function (add) {
    add.stop(0, c1)
    add.stop(1, c2)
  })
}


function getOffset(t) {

  switch (t.output) {
    case 'right':
      return {
        x: t.x + t.size / 2, y: t.y
      };
    case 'left':
      return {
        x: t.x - t.size / 2, y: t.y
      };
    case 'top':
      return {
        x: t.x, y: t.y - t.size / 2
      };
    case 'bottom':
      return {
        x: t.x, y: t.y + t.size / 2
      };
  }
}

function drawLines(thing1, thing2) {

  let t1 = getOffset(thing1);
  let t2 = getOffset(thing2);
  let c1 = thing1.color;
  let c2 = thing2.color;

  let inx = 0;
  let iny = 0;
  let outx = 1;
  let outy = 1;
  if (thing1.output == 'bottom' && thing2.output == 'left') {
    outx = 0;
    outy = 0;
    inx = 1;
    iny = 0;
  }
  if (thing1.output == 'right' && thing2.output == 'left') {
    outx = 0;
    outy = 0;
    inx = 1;
    iny = 0;
  }
  if (thing1.output == 'bottom' && thing2.output == 'right') {
    outx = 1;
    outy = 0;
    inx = 0;
    iny = 1;
  }
  if (thing1.output == 'bottom' && thing2.output == 'top') {
    outx = 0;
    outy = 0;
    inx = 0;
    iny = 1;
  }
  if (thing1.output == 'top' && thing2.output == 'left') {
    outx = 0;
    outy = 0;
    inx = 1;
    iny = 0;
  }


  var gradient = draw.gradient('linear', function (add) {
    add.stop(0, c1)
    add.stop(0.75, c2)
  }).from(outx, outy).to(inx, iny)


  let pathString = 'M' + t1.x + ' ' + t1.y + ' ' +
    'C' + t1.x + ' ' + t1.y + ' ' +
    width / 2 + ' ' + height / 2 + ' ' +
    t2.x + ' ' + t2.y;

  path = draw.path(pathString).attr({
    stroke: gradient,
    'stroke-width': strokeWidth,
    'fill': backgroundColor
  })

  ball = animateBall(path);

  path.back();

  return {
    line: path,
    ball: ball
  };


}

function animateBall(path) {

  var len = path.length();
  let start = path.pointAt(0);

  var ball = draw.circle(maxDim * 0.02).attr({
    cx: start.x,
    cy: start.y,
    fill: "#FFFF00",
    stroke: "#FFFF00",
    'stroke-width': 1,
    opacity: 0.65
  });

  ball.animate(2000, '<>').during(
    function (pos, morph, eased) {
      var p = path.pointAt(pos * len)
      ball.center(p.x, p.y)
    }
  ).loop(true, false)

  return ball;

}

function drawBattery(battery) {

  x1 = battery.x;
  y1 = battery.y;

  size = {
    x: battery.size,
    y: battery.size / sizeRatio
  };

  loc = {
    x: x1,
    y: y1 + (size.x * 0.75)
  }

  frame = draw.rect(size.x, size.y).attr({
    fill: 'none',
    stroke: battery.color,
    'stroke-width': battery.strokeWidth
  }).center(loc.x, loc.y).radius(10);

  percent = 0.8;
  size = battery.size * percent

  if (percent <= 0.5)
    mid = 1.0 - (percent / 2.0);
  else
    mid = 1.0 - percent;

  var gradient = draw.gradient('linear', function (add) {
    add.stop(0, "#FF0000")
    add.stop(mid, "#FFFF00")
    add.stop(1, "#00FF00")
  })

  bar = draw.rect(size, battery.size / 4).attr({
    fill: gradient,
    stroke: battery.color,
    'stroke-width': 0
  }).move(x1 - (size * (0.5 / percent)), y1 + battery.size * 0.625).radius(10);

  value = draw.text(percent * 100 + "%").attr({
    fill: 'white'
  }).font({
    family: 'Helvetica',
    size: 0.05 * maxDim,
    style: 'center'

  }).center(x1 + battery.size - 0.15 * battery.size, y1 + battery.size * 0.75);

  frame.forward();
}

function getTime(str){

  t = str.split(".", 2).join(".")+".dt"

  return getOWvalue(t);
}

function getOWvalue(str, time = undefined){

  tokens = str.split('.');
  
  cnt = 0;
  cur = ow_data;

  if (tokens[tokens.length-1]=="precipitation" || tokens[tokens.length-2]=="precipitation"){
  
    return getOWvalue(str.replace("precipitation", "rain")) + (10.0 * getOWvalue(str.replace("precipitation", "snow")));
  
  } else {
    
    try {
      
      tokens.forEach(token => {
        if (isNaN(token)) 
          try {
            cur = cur.get(token);
          } catch (e){
            cur = cur[token];
          }
        else cur = cur[parseInt(token)];
      
      });

      if (isNaN(cur)){

        if ((time < sunrise && time > sunset) ||
            (sunrise < sunset && time < sunrise)) {

          let valid_str = ["few clouds", "scattered clouds" , "clear sky", "broken clouds"];
          
          if (valid_str.indexOf(cur)>-1) 
            cur += " night";  
        }
      }

      return cur == undefined ? 0 : cur;
    
    } catch (e) {
      return 0;
    }
  }
}

function combineObjects(parent_, child_){

  grid = widget_data.get('grid');

  window_height = window.innerHeight;
  window_width = window.innerWidth;

  gridSpaceX = window_width / grid.x;
  gridSpaceY = window_height / grid.y;

  child_radius = getDistanceToCenter(child_, false);
  parent_radius = getDistanceToCenter(parent_, false);

  recessed = child_.location.recessed;

  if (child_.circle != undefined){
    scale = gridSpaceX > gridSpaceY ? gridSpaceX/gridSpaceY : gridSpaceY/gridSpaceX;
    scale = 1/scale;
        
    parent_.type = "rectangle";
    
    parent_.rectangle = {
      "size": {
          "x" : 2*(parent_radius+child_radius)+recessed,
          "y" : child_.circle.size.radius*scale
      },
      "color":      child_.circle.color,
      "line_width": child_.circle.line_width,
      "radius" :    child_radius/2,  
    };

    parent_.content[0].location = {
      "x" : 0.5,
      "y" : 0.5,
    };

    parent_.circle = undefined;   
  
  } else if (child_.rectangle != undefined) {
  
    parent_.type = "rectangle";
    
    parent_.rectangle = {
      "size": {
          "x" : 2*(parent_radius+child_radius)+recessed,
          "y" : child_.rectangle.size.y
      },
      "color":      child_.rectangle.color,
      "line_width": child_.rectangle.line_width,
      "radius" :    child_.rectangle.radius 
    };
    parent_.content[0].location = {
      "x" : 0.5,
      "y" : 0.5,
    };
    
    parent_.circle = undefined;
  
  } 
}

function combineItems(items, parent_, child_){

  index = items.findIndex(it=> it.name == child_.name);

  needChange = items.filter(item=> item.parent_widget == child_.name);

  if (needChange.length > 0){
    needChange.forEach((item) => {
      item.parent_widget = parent_.name;
    })  
  }

  combineObjects(parent_, child_);
  
  child_.name = "XXX";

  removeItemAll(widgets, "XXX");
}

function condense(items, i){

  if (i < 0) return;
  
  child_ = items[i];

  if (child_.content.length!=0){
    sameName = child_.name.substring(0, child_.name.length-2);

    parents = items.filter(parent_=> parent_.name == child_.parent_widget && 
                                     parent_.name.includes(sameName) &&
                                     parent_.content[0].value == child_.content[0].value);


    //Should only have 1 "special" child
    if (parents.length != 0) {
      combineItems(items, parents[0], child_);
    }
  }
  condense(items, i-1);
  
}

function removeItemAll(arr, value) {
  var i = 0;
  while (i < arr.length) {
    if (arr[i].name === value) {
      arr.splice(i, 1);
    } else {
      ++i;
    }
  }
  return arr;
}

async function updateForRanges(){

  widgets = widget_data.get('widgets');

  var index=0;

  var newItems = [];

  widgets.forEach((widget) => {

    widget.content.forEach((content) => {  
       
      if (content.repeat != undefined) {
        parent_ = widget.parent_widget;
        location_ = widget.location;
        recessed_ = widget.content[0] != undefined ? widget.content[0].repeat.recessed : 1;
        angle_ = widget.content[0] != undefined ? widget.content[0].repeat.angle : 90;

        for (i=0; i<content.repeat.number; i++){
         
          newWidget = JSON.parse(JSON.stringify(widget));

          if (i!=0){
            newWidget.name = newWidget.name+i.toString();
          }

          newWidget.parent_widget = parent_;
          newWidget.location = location_;
          newWidget.content = [];

          index++;
          newWidget.index = index;
          newItems.push(newWidget);
        
          parent_ = newWidget.name;
          location_ = {"degrees": angle_, "recessed": recessed_};
        }

        widget.type = undefined;
        widget.name = "XXX";
      }   

      else if (content.open_weather != undefined && content.open_weather.range != undefined){
        
        split = content.open_weather.range.value.split("..");
        start = parseInt(split[0]);
        end = parseInt(split[1]);

        parent_ = widget.parent_widget;
        location_ = widget.location;
        recessed_ = widget.content[0] != undefined ? widget.content[0].open_weather.range.recessed : 1;
        angle_ = widget.content[0] != undefined ? widget.content[0].open_weather.range.angle : 90;

        var current_value = -999.99;
        for (i=start; i<end; i++){
        
          weatherString_ = content.open_weather.string.replaceAll('%', i.toString());
          new_value = formatValue(content, getOWvalue(weatherString_, getTime(weatherString_)));

          //newWidget = Object.assign({}, widget);
          newWidget = JSON.parse(JSON.stringify(widget));
          if (i!=start){
            newWidget.name = newWidget.name+i.toString();
          }

          newWidget.parent_widget = parent_;
          newWidget.location = location_;

          //newWidget.content = [];
          //newWidget.content.push(Object.assign({}, content));
          newWidget.content[0].open_weather.time = i;
          newWidget.content[0].open_weather.range = undefined;
          newWidget.content[0].open_weather.string = weatherString_;
          newWidget.content[0].value = new_value;

          index++;
          newWidget.index = index;
          newItems.push(newWidget);
        
          parent_ = newWidget.name;
          location_ = {"degrees": angle_, "recessed": recessed_};
          current_value = new_value;
        }
        widget.type = undefined;
        widget.name = "XXX"
      }
    });
    index++;
  });

  if (newItems!=[])
    newItems.forEach(item=>{
      widgets.splice(item.index, 0, item);
    })

  
  removeItemAll(widgets, "XXX");

  condense(widgets, widgets.length-1)  
}

async function updateOpenWeatherData(){
  
  await updateOptionData();
  await getOWData();

  updateForRanges();

  widgets = widget_data.get('widgets');

  let matchFound = false;
  var value, decimals;

  if (current_values == undefined){
    current_values = new Map();
  }

  current_time = getOWvalue("current.dt");
  sunrise = getOWvalue("daily.0.sunrise");
  if (sunrise < current_time) sunrise = getOWvalue("daily.1.sunrise");
  sunset =  getOWvalue("daily.0.sunset");
  if (sunset < current_time) sunset = getOWvalue("daily.1.sunset");

  widgets.forEach((widget) => {
    widget.content.forEach((content) => {
      if (content.open_weather != undefined){

        value = getOWvalue(content.open_weather.string, getTime(content.open_weather.string));   

        current_values.set(widget.name, value);
      
      }
    });
  });  

  if (pendingUpdate == false) {
    
    pendingUpdate = true;
    setTimeout(drawWidget, 10000); 
  
  }  
  return 0;
}

async function updateSensorData(getOptionData = true){
  if (getOptionData) await updateOptionData();

  await getSensorData();

  sensor_data.forEach(data=>{
    parseEvent(data);
  });
}

async function updateOptionData(){
  await getGraphData();

  return 0;
}


function drawWidget(callback) {

    grid = widget_data.get('grid');
    var widgets = widget_data.get('widgets');

    window_height = window.innerHeight;
    window_width = window.innerWidth;

    gridSpaceX = window_width / grid.x;
    gridSpaceY = window_height / grid.y;

    if (draw!=undefined) {
      draw.clear();
    }
    else {
      draw = SVG().addTo('#timeline').size(window_width, window_height);
      
    }
    widget_group = draw.group();

    background = draw.rect(window_width, window_height).attr({
      fill: "#000000",
      stroke: '#000000',
      'stroke-width': 1
    })

    widget_group.add(background);

    items = [];
    
    widgets.forEach(widget => {

      if (widget.type == 'circle') {
        if (widget.parent_widget == undefined)
          widget.obj = drawCircle(widget);
        else {
          parent_ = widgets.find(it => it.name == widget.parent_widget);
          widget.obj = drawCircle(widget, parent_);
        }
      }
      if (widget.type == 'rectangle') {
        if (widget.parent_widget == undefined)
          widget.obj = drawRectangle(widget);
        else {
          parent_ = widgets.find(it => it.name == widget.parent_widget);
          widget.obj = drawRectangle(widget, parent_);
        }
      }
    });

    background.back();
    
    pendingUpdate = false;

}
