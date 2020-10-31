//Test 2

var sunrise;
var sunset;
let options = [];
let pws_data = [];
let currentTemperature;
var weekdays = new Array(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
);

var months = new Array(
    "Jan", "Feb", "Mar", "Apr", "May", "June", "July", "Aug", "Sep", "Oct", "Nov", "Dec"
);

function getOptions() {
    return jQuery.get(localURL+"getOptions/?access_token="+secretEndpoint, (data) => {
        options = data;

        console.log("Got Options");
        console.log(options);
    });
}


function getData() {
    return jQuery.get(localURL+"getData/?access_token="+secretEndpoint, (data) => {

        let out = options.display;

        pws_data = new Map();
        if (data.current_temperature)
            pws_data.set('current_temperature', { value: data.current_temperature.value, in_units: data.current_temperature.units });
        if (data.current_wind_speed)
            pws_data.set('current_wind_speed', { value: data.current_wind_speed.value, in_units: data.current_wind_speed.units });
        if (data.current_feels_like)
            pws_data.set('current_feels_like', { value: data.current_feels_like.value, in_units: data.current_feels_like.units });
        if (data.current_wind_gust)
            pws_data.set('current_wind_gust', { value: data.current_wind_gust.value, in_units: data.current_wind_gust.units });
        if (data.current_wind_direction)
            pws_data.set('current_wind_direction', { value: data.current_wind_direction.value, in_units: data.current_wind_direction.units });
        if (data.current_pressure)
            pws_data.set('current_pressure', { value: data.current_pressure.value, in_units: data.current_pressure.units });
        if (data.current_humidity)
            pws_data.set('current_humidity', { value: data.current_humidity.value, in_units: data.current_humidity.units });
        if (data.current_dewpoint) {
            pws_data.set('current_dewpoint', { value: data.current_dewpoint.value, in_units: data.current_dewpoint.units });
        }
        if (data.current_pressure_trend)
            pws_data.set('current_pressure_trend', { value: data.current_pressure_trend.value, in_units: data.current_pressure_trend.units });
        if (data.current_precipitation)
            pws_data.set('current_precipitation', { value: data.current_precipitation.value, in_units: data.current_precipitation.units });

    });
}


function translateCondition(condition) {

    let icon = "mdi-weather-sunny-off";
    let text1 = "UNKNOWN";
    let text2 = "";
    let now = new Date().getTime() / 1000;
    let color = "#ff0000";

    switch (condition) {
        case "thunderstorm with light rain":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS";
            text2 = "LIGHT RAIN";
            color = "#e6f7ff";
            break;
        case "thunderstorm with rain":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS";
            text2 = "RAIN";
            color = "#e6f7ff";
            break;
        case "thunderstorm with heavy rain":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS"
            text2 = "HEAVY RAIN";
            color = "#e6f7ff";
            break;
        case "light thunderstorm":
            icon = "mdi-weather-lightning";
            text1 = "LIGHT";
            text2 = "THUNDERSTORMS";
            color = "#cccc00";
            break;
        case "thunderstorm":
            icon = "mdi-weather-lightning";
            text1 = "THUNDERSTORMS";
            color = "#cccc00";
            break;
        case "heavy thunderstorm":
            icon = "mdi-weather-lightning";
            text1 = "HEAVY";
            text2 = "THUNDERSTORMS";
            color = "#cccc00";
            break;
        case "ragged thunderstorm":
            icon = "mdi-weather-lightning";
            text1 = "SCATTERED";
            text2 = "THUNDERSTORMS";
            color = "#cccc00";
            break;
        case "thunderstorm with light drizzle":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS";
            text2 = "LIGHT DRIZZLE";
            color = "#e6f7ff";
            break;
        case "thunderstorm with drizzle":
            icon = "mdi-weather-lightning-rainy";
            text1 = "THUNDERSTORMS";
            text2 = "DRIZZLE";
            color = "#e6f7ff";
            break;
        case "thunderstorm with heavy drizzle":
            icon = "mdi-weather-lightning-rainy"
            text1 = "THUNDERSTORMS"
            text2 = "HEAVY DRIZZLE";
            color = "#e6f7ff";
            break;
        case "light intensity drizzle":
            icon = "mdi-weather-partly-rainy";
            text1 = "LIGHT";
            text2 = "DRIZZLE";
            color = "#e6f7ff";
            break;
        case "drizzle":
            icon = "mdi-weather-partly-rainy";
            text1 = "DRIZZLE"
            color = "#e6f7ff";
            color = "#e6f7ff";
            break;
        case "heavy intensity drizzle":
            icon = "mdi-weather-partly-rainy";
            text1 = "HEAVY";
            text2 = "DRIZZLE";
            color = "#e6f7ff";
            break;
        case "light intensity drizzle rain":
            icon = "mdi-weather-partly-rainy";
            text1 = "LIGHT";
            text2 = "DRIZZLE";
            color = "#e6f7ff";
            break;
        case "drizzle rain":
            icon = "mdi-weather-partly-rainy";
            text1 = "DRIZZLE";
            break;
        case "heavy intensity drizzle rain":
            icon = "mdi-weather-rainy";
            text1 = "RAIN";
            color = "#00ace6";
            break;
        case "shower rain and drizzle":
            icon = "mdi-weather-rainy";
            text1 = "RAIN";
            color = "#00ace6";
            break;
        case "heavy shower rain and drizzle":
            icon = "mdi-weather-pouring";
            text1 = "SHOWERS";
            color = "#00ace6";
            break;
        case "shower drizzle":
            icon = "mdi-weather-rainy";
            text1 = "SHOWERS";
            color = "#00ace6";
            break;
        case "light rain":
            icon = "mdi-weather-rainy";
            text1 = "LIGHT";
            text2 = "RAIN";
            color = "#00ace6";
            break;
        case "moderate rain":
            icon = "mdi-weather-pouring";
            text1 = "MODERATE";
            text2 = "RAIN";
            color = "#00ace6";
            break;
        case "heavy intensity rain":
            icon = "mdi-weather-pouring";
            text1 = "HEAVY";
            text2 = "RAIN";
            color = "#00ace6";
            break;
        case "very heavy rain":
            icon = "mdi-weather-pouring";
            text1 = "VERY HEAVY";
            text2 = "RAIN";
            color = "#00ace6";
            break;
        case "extreme rain":
            icon = "mdi-weather-pouring";
            text1 = "INTENSE";
            text2 = "RAIN";
            color = "#4dd2ff";
            break;
        case "freezing rain":
            icon = "mdi-weather-snowy-rainy";
            text1 = "FREEZING";
            text2 = "RAIN";
            color = "#e6f9ff";
            break;
        case "light intensity shower rain":
            icon = "mdi-weather-rainy";
            text1 = "LIGHT";
            text2 = "SHOWERS";
            color = "#b3ecff"
            break;
        case "shower rain":
            icon = "mdi-weather-rainy";
            text1 = "SHOWERS";
            color = "#99e6ff"
            break;
        case "heavy intensity shower rain":
            icon = "mdi-weather-pouring";
            text1 = "HEAVY"
            text2 = "SHOWERS";
            color = "#4dd2ff";
            break;
        case "ragged shower rain":
            icon = "mdi-weather-partly-rainy";
            text1 = "SCATTERED";
            text2 = "SHOWERS";
            color = "#33ccff"
            break;
        case "light snow":
            icon = "mdi-weather-snowy";
            text1 = "LIGHT";
            text2 = "SNOW";
            color = "#FFFFFF"
            break;
        case "Snow":
            icon = "mdi-weather-snowy";
            text1 = "SNOW";
            color = "#FFFFFF"
            break;
        case "Heavy snow":
            icon = "mdi-weather-snowy-heavy";
            text1 = "HEAVY SNOW";
            color = "#FFFFFF"
            break;
        case "Sleet":
            icon = "mdi-weather-hail";
            text1 = "SLEET";
            color = "#e6f9ff"
            break;
        case "Light shower sleet":
            icon = "mdi-weather-hail";
            text1 = "LIGHT SLEET";
            color = "#e6f9ff"
            break;
        case "Shower sleet":
            icon = "mdi-weather-hail";
            text1 = "SLEET";
            text2 = "SHOWERS";
            color = "#e6f9ff"
            break;
        case "Light rain and snow":
            icon = "mdi-weather-snowy-rainy";
            text1 = "LIGHT"
            text2 = "RAIN & SNOW";
            color = "#e6f9ff"
            break;
        case "Rain and snow":
            icon = "mdi-weather-snowy-rainy";
            text1 = "RAIN & SNOW";
            color = "#e6f9ff"
            break;
        case "Light shower snow":
            icon = "mdi-weather-partly-snowy";
            text1 = "LIGHT RAIN"
            text2 = "SNOW SHOWERS";
            color = "#FFFFFF"
            break;
        case "Shower snow":
            icon = "mdi-weather-partly-snowy";
            text1 = "SNOW SHOWERS";
            color = "#ffffe6";
            break;
        case "Heavy shower snow":
            icon = "mdi-weather-partly-snowy";
            text1 = "HEAVY";
            text2 = "SNOW SHOWERS";
            color = "#ffffe6"
            break;
        case "mist":
            icon = "mdi-weather-fog";
            text1 = "MIST";
            color = "#f0f0f5"
            break;
        case "Smoke":
            icon = "mdi-weather-fog";
            text1 = "SMOKE";
            color = "#f0f0f5"
            break;
        case "Haze":
            icon = "mdi-weather-hazy";
            text1 = "HAZE";
            color = "#ebebe0"
            break;
        case "sand dust whirls":
            icon = "mdi-weather-tornado";
            text1 = "DUST WHIRLS";
            color = "#cc9966";
            break;
        case "fog":
            icon = "mdi-weather-fog";
            text1 = "FOG";
            color = "#a6a6a6"
            break;
        case "sand":
            icon = "mdi-weather-fog";
            text1 = "SAND";
            color = "#cc9966";
            break;
        case "dust":
            icon = "mdi-weather-fog";
            text1 = "DUST";
            color = "#cc9966";
            break;
        case "volcanic ash":
            icon = "mdi-weather-fog";
            text1 = "VOCANIC ASH";
            color = "#a6a6a6";
            break;
        case "squalls":
            icon = "mdi-weather-tornado";
            text1 = "SQUALLS";
            color = "#c68c53";
            break;
        case "tornado":
            icon = "mdi-weather-tornado";
            text1 = "TORNADO";
            color = "#c68c53";
            break;
        case "clear sky":
            if (now > sunset || now < sunrise) {
                icon = "mdi-weather-night";
                text1 = "CLEAR";
                color = "#4d79ff"
            } else {
                icon = "mdi-weather-sunny";
                text1 = "SUNNY";
                color = "yellow"
            }
            break;
        case "few clouds":
            if (now > sunset || now < sunrise) {
                icon = "mdi-weather-night-partly-cloudy"
            } else {
                icon = "mdi-weather-partly-cloudy";
            }
            text1 = "FEW CLOUDS";
            color = "#ffffcc";
            break;
        case "scattered clouds":
            if (now > sunset || now < sunrise) {
                icon = "mdi-weather-night-partly-cloudy"
            } else {
                icon = "mdi-weather-partly-cloudy";
            }
            text1 = "SCATTERED";
            text2 = "CLOUDS";
            color = "#ffffcc";
            break;
        case "broken clouds":
            icon = "mdi-weather-cloudy";
            text1 = "BROKEN"
            text2 = "CLOUDS";
            color = "#ffffcc";
            break;
        case "overcast clouds":
            icon = "mdi-weather-cloudy";
            text1 = "OVERCAST"
            text2 = "CLOUDS";
            color = "#ffffcc";
            break;
    }
    return {icon: icon, text1: text1, text2: text2, color: color};
}

function setCondition(condition) {

    let val = translateCondition(condition);

    let el = document.getElementById('weather_icon');
    el.className = 'mdi ' + val.icon;

    el = document.getElementById('current_condition1');
    el.textContent = val.text1;

    el = document.getElementById('current_condition2');
    el.textContent = val.text2;

    if (options.color_icons) {
        console.log("Setting color: " + val.color);
        jQuery(".weather_icon").css("cssText", "color: " + val.color + " !important");


    }
}

function setValue(val, str) {
    let el = document.getElementById(str);
    el.textContent = val;
}


function getWindDirection(direction) {
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


function getString(data, out_units) {

    let val = parseFloat(data.value);
    let in_units = data.in_units;

    if (in_units != out_units) {
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
                    case "kelvin": val = ((val - 32) * (5 / 9)) + 273.15; break;
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
            //Length
            case "millimeters":
                if (out_units == "inches") {
                    val = (val / 25.4);
                }
                else val = "UNSUPPORTED";
                break;
            case "inches":
                if (out_units == "millimeters") {
                    val = (val * 25.4);
                }
                else val = "UNSUPPORTED";
                break;
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
                        outputType = "text";
                        break;
                    case "radians": val = (val * 3.1415926535) * 180.0; break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "radians":
                switch (out_units) {
                    case "cardinal": val = getWindDirection(((val * 3.1415926535) * 180.0)); outputType = "text"; break;
                    case "degrees": val = ((val * 180) / 3.1415926535); break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "cardinal":
                switch (data.value) {
                    case "N": val = 0;
                    case "NNE": val = 22.5;
                    case "NE": val = 45;
                    case "ENE": val = 67.5;
                    case "E": val = 90;
                    case "ESE": val = 112.5;
                    case "SE": val = 135;
                    case "SSE": val = 157.5;
                    case "S": val = 180;
                    case "SSW": val = 202.5;
                    case "SW": val = 225;
                    case "WSW": val = 247.5;
                    case "W": val = 270;
                    case "WNW": val = 292.5;
                    case "NW": val = 315;
                    case "NNW": val = 337.5;
                    default: val = -1;
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
                let d = new Date(data.value * 1000);

                switch (out_units) {
                    case "time_twelve": val = d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }); outputType = "text"; break;
                    case "time_two_four": val = d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }); outputType = "text"; break;
                    case "day_only": val = weekdays[d.getDay()]; outputType = "text"; break;
                    case "date_only": val = d.getDate(); outputType = "text"; break;
                    case "day_date": val = weekdays[d.getDay()]+" "+d.getDate(); outputType = "text"; break;
                    case "month_day": val = months[d.getMonth()]+" "+d.getDate(); outputType = "text"; break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "time_milliseconds":
                let e = new Date(data.value);

                switch (out_units) {
                    case "time_twelve": val = e.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }); outputType = "text"; break;
                    case "time_two_four": val = e.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }); outputType = "text"; break;
                    default: val = "UNSUPPORTED";
                }
                break;
            case "trend_numeric":
                if (out_units == "trend_text") {
                    val = parseFloat(data.value);
                    if (val < 0)
                        val = "Falling";
                    else if (val == 0)
                        val = "Steady";
                    else
                        val = "Rising";
                } else val = "UNSUPPORTED";
                outputType = "text";
                break;
            case "trend_text":
                if (out_units == "trend_numeric") {
                    let lcString = data.value.toLowerCase();
                    switch (lcString) {
                        case "falling": val = -1; break;
                        case "steady": val = 0; break;
                        case "rising": val = 1; break;
                    }
                }
                break;
            case "trend_numeric":
                val = parseFloat(data.value);
                if (val < 0)
                    val = "Falling";
                else if (val == 0)
                    val = "Steady";
                else
                    val = "Rising";
                break;
                outputType = "text";
            case "percent_numeric":
                if (out_units == "percent_decimal") val = val / 100.0;
                else val = "UNSUPPORTED";
                break;
            case "percent_decimal":
                if (out_units == "percent_numeric") val = val * 100.0;
                else val = "UNSUPPORTED";
                break;
        }

    }

    if (val == "UNSUPPORTED") {
        return val;
    }
    return val;
}


function setWeatherTile(weather) {

    weather.forEach((value, key) => {

        let val;
        let out;

        switch (key) {
            case 'current_temperature':
                out = options.display.current_temp;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                switch (out.unit) {
                    case "celsius":
                        currentTemperature = (val * 9 / 5) + 32; break;
                    case "fahrenheit":
                        currentTemperature = val; break;
                    case "kelvin":
                        currentTemperature = (val - 273.15) * 9 / 5 + 32; break;

                }
                setValue(val, key);
                break;
            case 'forecast_high':
                out = options.display.forcast_high;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'forecast_low':
                out = options.display.forecast_low;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'current_feels_like':
                out = options.display.feels_like;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'current_description':
                setCondition(value.value);
                break;
            case 'current_wind_speed':
                out = options.display.wind_speed;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'current_wind_gust':
                out = options.display.wind_gust;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'current_wind_direction':
                out = options.display.wind_direction;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'current_pressure':
                out = options.display.current_pressure;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'current_pressure_trend':
                setPressure(value.value);
                break;
            case 'current_humidity':
                out = options.display.humidity;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'current_precipitation':
                out = options.display.actual_precipitation;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'current_dewpoint':
                out = options.display.dew_point;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);

                let dewPoint_f;
                switch (out.unit) {
                    case "celsius":
                        dewPoint_f = (val * 9 / 5) + 32; break;
                    case "fahrenheit":
                        dewPoint_f = val; break;
                    case "kelvin":
                        dewPoint_f = (val - 273.15) * 9 / 5 + 32; break;

                }
                if (options.show_dewpoint) {
                    let dewpoint_text = getDewPoint(dewPoint_f);
                    setValue(dewpoint_text, 'dewpoint_text');
                }
                break;
            case 'forecast_precipitation':
                out = options.display.forecast_precipitation;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'forecast_precipitation_chance':
                out = options.display.chance_precipitation;
                val = getString(value, out.unit);
                console.log(val + " Here");
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'sunrise':
                out = options.display.time_format;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;
            case 'sunset':
                out = options.display.time_format;
                val = getString(value, out.unit);
                val = isNaN(val) ? val : val.toFixed(out.decimal);
                setValue(val, key);
                break;

        }

    });

}
function toInches(val) {
    val = val / 25.4;
    return val.toFixed(2);
}

function getTime(secs) {
    let date = new Date(secs * 1000);
    return date.toLocaleTimeString([], { timeStyle: 'short' });
}

function getDewPoint(dewPoint) {
    
        if (dewPoint < 50) "DRY"
        else if (dewPoint < 55) return "NORMAL";
        else if (dewPoint < 60) return "OPTIMAL";
        else if (dewPoint < 65) return "STICKY";
        else if (dewPoint < 70) return "MOIST";
        else if (dewPoint < 75) return "WET";
        else return "MISERABLE"
  
}

function setPressure(val) {
    let icon = '';
    let text = '';
    let valStr = '';

    if (isNaN(val)) valStr = val.toLowerCase();

    console.log(val + " " + valStr);

    if (val == 0 || valStr == 'steady') {
        text = 'Steady';
        icon = 'mdi-arrow-right-thick';
    } else if (val < 0 || valStr == 'falling') {
        text = 'Falling';
        icon = 'mdi-arrow-down-thick';
    } else if (val > 0 || valStr == 'rising') {
        text = 'Rising';
        icon = 'mdi-arrow-up-thick';
    }

    let el = document.getElementById('pressure_icon');
    el.className = 'current_pressure_trend mdi ' + icon;

    el = document.getElementById('current_pressure_trend');
    el.textContent = text;
}

function getWeather() {
    const url = `https://api.openweathermap.org/data/2.5/onecall?lat=`+latitude+`&lon=`+longitude+`&exclude=minutely&appid=`+tile_key+`&units=imperial`;

    let now = new Date();
    let tempUnits = "fahrenheit";
    let speedUnits = "meters_per_second";
    let precipUnits = "millimeters";
    let pressureUnits = "millibars";
    let timeUnits = "time_seconds"
    let dirUnits = "degrees"
    let percent = "percent_numeric";



    var currentPressure = 0;

    fetch(url)
        .then(response => response.json())
        .then(data => {
            let weather = new Map();
            console.log(data);
            let override = options.override;
            let out = options.display;

            if (override.current_temp == "openweather") weather.set('current_temperature', { value: data.current.temp, in_units: tempUnits });
            weather.set('forecast_high', { value: data.daily[0].temp.max, in_units: tempUnits });
            weather.set('forecast_low', { value: data.daily[0].temp.min, in_units: tempUnits });
            if (override.feels_like == "openweather") weather.set('current_feels_like', { value: data.current.feels_like, in_units: tempUnits });
            weather.set('current_description', { value: data.current.weather[0].description, in_units: "none" });
            if (override.wind_speed == "openweather") weather.set('current_wind_speed', { value: data.current.wind_speed, in_units: speedUnits });

            if (override.wind_gust == "openweather")
                if (data.current.wind_gust != undefined) weather.set('current_wind_gust', { value: data.current.wind_gust, in_units: speedUnits });
                else weather.set('current_wind_gust', { value: data.current.wind_speed, in_units: speedUnits });

            if (override.wind_direction == "openweather") weather.set('current_wind_direction', { value: data.current.wind_deg, in_units: dirUnits });

            if (data.daily[0].rain != undefined) weather.set('forecast_precipitation', { value: data.daily[0].rain, in_units: precipUnits });
            else weather.set('forecast_precipitation', { value: "0.00", in_units: precipUnits });

            weather.set('forecast_precipitation_chance', { value: data.daily[0].pop * 100, in_units: percent })

            if (override.current_pressure == "openweather") weather.set('current_pressure', { value: data.current.pressure, in_units: pressureUnits });
            currentPressure = data.current.pressure;

            if (override.humidity == "openweather") weather.set('current_humidity', { value: data.current.humidity, in_units: percent });
            weather.set('sunrise', { value: data.current.sunrise, in_units: timeUnits });
            weather.set('sunset', { value: data.current.sunset, in_units: timeUnits });
            if (override.dew_point == "openweather") weather.set('current_dewpoint', { value: data.current.dew_point, in_units: tempUnits });

            //Set global sunrise and sunset
            sunrise = data.current.sunrise;
            sunset = data.current.sunset;

            setWeatherTile(weather);
        })
        .catch((error) => {
            console.log(error);
        });


    now.setHours(0, 0, 0, 0);
    let secs = now.getTime() / 1000.0
    secs = secs.toFixed();

    const url2 = `https://api.openweathermap.org/data/2.5/onecall/timemachine?lat=`+latitude+`&lon=`+longitude+`&dt=`+secs+`&appid=`+tile_key;

    fetch(url2)
        .then(response => response.json())
        .then(data => {
            let override = options.override;
            let weather2 = new Map();
            let out = options.display;

            if (override.daily_precip == "openweather") {
                let total_rain = 0.0;
                data.hourly.forEach((val) => {
                    if (val.rain != undefined) {
                        total_rain += val.rain["1h"];
                    }
                })
                weather2.set('current_precipitation', { value: total_rain, in_units: "millimeters" });
            }

            if (override.pressure_trend == "openweather") {
                let num = data.hourly.length;
                let compare = num - 1;
                if (((now.getTime() / 1000.0) - data.hourly[num - 1].dt) < 1800) {
                    compare = compare + 1 == num ? compare : compare + 1;
                }

                let diff = data.hourly[compare].pressure - currentPressure;
                weather2.set('current_pressure_trend', { value: diff, in_units: "none" });
            }
            setWeatherTile(weather2);

        })
        .catch((error) => {
            console.log(error);
        });
}

async function getOpenWeatherData() {
    let now = new Date();
    console.log("OpenWeather Data Refresh at " + now.toLocaleString());
    getWeather();
}
async function getPWSData() {
    let now = new Date();
    console.log("PWS Data Refresh at " + now.toLocaleString());

    await getData();
    setWeatherTile(pws_data);
}
async function initialize() {
    await getOptions();

    setInterval(() => {
        getOpenWeatherData();
    }, options.openweather_refresh_rate);
    let minutes = (options.openweather_refresh_rate / 1000) / 60;
    console.log("Refreshing OpenWeather at " + minutes + " minutes");

    if (options.pws_refresh_rate != null) {

        setInterval(() => {
            getPWSData();
        }, options.pws_refresh_rate);
        let minutes = (options.pws_refresh_rate / 1000) / 60;
        console.log("Refreshing Personal Weather Station at " + minutes + " minutes");
    }

    await getData();
    getWeather();
    setWeatherTile(pws_data);

}

function showWeather() {
  
}

function hideWeather() {
    jQuery(".graphWindow").css("cssText", "display: none");
}

function drawChart() {
    google.charts.load('current', { 'packages': ['corechart'] });

    var data = google.visualization.arrayToDataTable([
        ['Year', 'Sales', 'Expenses'],
        ['2004', 1000, 400],
        ['2005', 1170, 460],
        ['2006', 660, 1120],
        ['2007', 1030, 540]
    ]);

    var options = {
        title: 'Company Performance',
        curveType: 'function',
        legend: { position: 'bottom' }
    };

    var chart = new google.visualization.LineChart(document.getElementById('daily_precipitation_graph'));

    chart.draw(data, options);
}
   
async function initializeForecast() {
    await getOptions();

    setInterval(() => {
        getWeeklyForecastWeather();
    }, options.openweather_refresh_rate);
    let minutes = (options.openweather_refresh_rate / 1000) / 60;
    console.log("Refreshing OpenWeather Forecast at " + minutes + " minutes");

    getWeeklyForecastWeather();

}
function lookup(data, value){
    split_val = value.split('.');
    cur = data;
    split_val.forEach((val)=>{
        cur = cur[val];   
    })
    return cur;
}

function setForecastTile(weather) {

    weather.forEach((value, key) => {
       
        let val;
        switch (key){
            case 'description' : 
                val = translateCondition(value.value);

                let el = document.getElementById('weather_icon');
                el.className = 'mdi ' + val.icon;
        
                let text = val.text1+" "+val.text2;
                el = document.getElementById('description');
                el.textContent = text;
        
                if (options.color_icons) {
                    console.log("Setting color: " + val.color);
                    jQuery(".weather_icon").css("cssText", "color: " + val.color + " !important");
                }
                break;
            case 'weather_icon' : break;
            case 'dewpoint_desc' :
                val = getDewPoint(value.value);
                setValue(val, key);
                break;
            default:
                val = getString(value, value.out_units); 
                val = isNaN(val) ? val : val.toFixed(value.decimals);
                setValue(val, key);
        }
    });
}

function getWeeklyForecastWeather() {
       
    const url = `https://api.openweathermap.org/data/2.5/onecall?lat=`+latitude+`&lon=`+longitude+`&exclude=minutely&appid=`+tile_key+`&units=imperial`;

    let now = new Date();
    console.log("OpenWeather Data Refresh at " + now.toLocaleString());

    let weather = new Map();
    fetch(url)
        .then(response => response.json())
        .then(data => {
            let day = data.daily[options.display_day];
            console.log(day);
            options.measurements.forEach((measure) => {
                weather.set(measure.name, { value: lookup(day, measure.openweather), 
                                            in_units: measure.in_unit, 
                                            out_units: measure.out_unit, 
                                            decimals: measure.decimals});
            });
            setForecastTile(weather);
        })
        
        .catch((error) => {
            console.log(error);
        });
}

