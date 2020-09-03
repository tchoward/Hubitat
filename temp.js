//google.load('visualization', '1.0', { 'packages': ['corechart'] });
//google.setOnLoadCallback(drawChart);

//const { TextPropTypes } = require("react-native");

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
    return jQuery.get(localURL + "getOptions/?access_token=" + secretEndpoint, (data) => {
        options = data;

        console.log("Got Options");
        console.log(options);
    });
}


function getData() {
    return jQuery.get(localURL + "getData/?access_token=" + secretEndpoint, (data) => {

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
    return { icon: icon, text1: text1, text2: text2, color: color };
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
                    case "fahrenheit":
                        val = (val * 9 / 5) + 32;
                        break;
                    case "kelvin":
                        val = val + 273.15;
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "fahrenheit":
                switch (out_units) {
                    case "celsius":
                        val = (val - 32.0) * (5 / 9);
                        break;
                    case "kelvin":
                        val = ((val - 32) * (5 / 9)) + 273.15;
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "kelvin":
                switch (out_units) {
                    case "fahrenheit":
                        val = ((val - 273.15) * (9 / 5)) + 32;
                        break;
                    case "celsius":
                        val = (val - 273.15);
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            //Length
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
            case "meters_per_second":
                switch (out_units) {
                    case "miles_per_hour":
                        val = (val * 2.237);
                        break;
                    case "knots":
                        val = (val * 1.944);
                        break;
                    case "kilometers_per_hour":
                        val = (val * 3.6);
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "miles_per_hour":
                switch (out_units) {
                    case "miles_per_hour":
                        val = (val / 2.237);
                        break;
                    case "knots":
                        val = (val / 1.151);
                        break;
                    case "kilometers_per_hour":
                        val = (val * 1.609);
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "knots":
                switch (out_units) {
                    case "miles_per_hour":
                        val = (val * 1.151);
                        break;
                    case "meters_per_second":
                        val = (val / 1.944);
                        break;
                    case "kilometers_per_hour":
                        val = (val * 1.852);
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "kilometers_per_hour":
                switch (out_units) {
                    case "miles_per_hour":
                        val = (val / 1.609);
                        break;
                    case "meters_per_second":
                        val = (val / 3.6);
                        break;
                    case "knots":
                        val = (val / 1.852);
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "hectopascal":
            case "millibars":
                switch (out_units) {
                    case "inches_mercury":
                        val = (val / 33.864);
                        break;
                    case "millimeters_mercury":
                        val = (val / 1.333);
                        break;
                    case "hectopascal":
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "inches_mercury":
                switch (out_units) {
                    case "hectopascal":
                    case "millibars":
                        val = (val * 33.864);
                        break;
                    case "inches_mercury":
                        val = (val / 25.4);
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "millimeters_mercury":
                switch (out_units) {
                    case "hectopascal":
                    case "millibars":
                        val = (val * 1.333);
                        break;
                    case "millimeters_mercury":
                        val = (val * 25.4);
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "degrees":
                switch (out_units) {
                    case "cardinal":
                        val = getWindDirection(val);
                        outputType = "text";
                        break;
                    case "radians":
                        val = (val * 3.1415926535) * 180.0;
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "radians":
                switch (out_units) {
                    case "cardinal":
                        val = getWindDirection(((val * 3.1415926535) * 180.0));
                        outputType = "text";
                        break;
                    case "degrees":
                        val = ((val * 180) / 3.1415926535);
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "cardinal":
                switch (data.value) {
                    case "N":
                        val = 0;
                    case "NNE":
                        val = 22.5;
                    case "NE":
                        val = 45;
                    case "ENE":
                        val = 67.5;
                    case "E":
                        val = 90;
                    case "ESE":
                        val = 112.5;
                    case "SE":
                        val = 135;
                    case "SSE":
                        val = 157.5;
                    case "S":
                        val = 180;
                    case "SSW":
                        val = 202.5;
                    case "SW":
                        val = 225;
                    case "WSW":
                        val = 247.5;
                    case "W":
                        val = 270;
                    case "WNW":
                        val = 292.5;
                    case "NW":
                        val = 315;
                    case "NNW":
                        val = 337.5;
                    default:
                        val = -1;
                }
                if (val != -1) {
                    switch (out_units) {
                        case "radians":
                            val = ((val * 3.1415926535) * 180.0);
                            break;
                        case "degrees":
                            val = val;
                            break;
                        default:
                            val = "UNSUPPORTED";
                    }
                } else val = "UNSUPPORTED";
                break;

            //TEXT CONVERSIONS
            case "time_seconds":
                let d = new Date(data.value * 1000);

                switch (out_units) {
                    case "time_twelve":
                        val = d.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
                        outputType = "text";
                        break;
                    case "time_two_four":
                        val = d.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' });
                        outputType = "text";
                        break;
                    case "day_only":
                        val = weekdays[d.getDay()];
                        outputType = "text";
                        break;
                    case "date_only":
                        val = d.getDate();
                        outputType = "text";
                        break;
                    case "day_date":
                        val = weekdays[d.getDay()] + " " + d.getDate();
                        outputType = "text";
                        break;
                    case "month_day":
                        val = months[d.getMonth()] + " " + d.getDate();
                        outputType = "text";
                        break;
                    default:
                        val = "UNSUPPORTED";
                }
                break;
            case "time_milliseconds":
                let e = new Date(data.value);

                switch (out_units) {
                    case "time_twelve":
                        val = e.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
                        outputType = "text";
                        break;
                    case "time_two_four":
                        val = e.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' });
                        outputType = "text";
                        break;
                    default:
                        val = "UNSUPPORTED";
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
                        case "falling":
                            val = -1;
                            break;
                        case "steady":
                            val = 0;
                            break;
                        case "rising":
                            val = 1;
                            break;
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
                        currentTemperature = (val * 9 / 5) + 32;
                        break;
                    case "fahrenheit":
                        currentTemperature = val;
                        break;
                    case "kelvin":
                        currentTemperature = (val - 273.15) * 9 / 5 + 32;
                        break;

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
                        dewPoint_f = (val * 9 / 5) + 32;
                        break;
                    case "fahrenheit":
                        dewPoint_f = val;
                        break;
                    case "kelvin":
                        dewPoint_f = (val - 273.15) * 9 / 5 + 32;
                        break;

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
    const url = `https://api.openweathermap.org/data/2.5/onecall?lat=` + latitude + `&lon=` + longitude + `&exclude=minutely&appid=` + tile_key + `&units=imperial`;

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

    const url2 = `https://api.openweathermap.org/data/2.5/onecall/timemachine?lat=` + latitude + `&lon=` + longitude + `&dt=` + secs + `&appid=` + tile_key;

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

async function initializeWeather() {

    await getOptions();

    console.log(options);
    setInterval(() => {
        //getCurrentWeather();
    }, options.openweather_refresh_rate);
    let minutes = (options.openweather_refresh_rate / 1000) / 60;
    console.log("Refreshing OpenWeather Forecast at " + minutes + " minutes");

    grid.engine.nodes.forEach(function (item) {

       setFont(item);

    });

    //getCurrentWeather();
}

function lookup(data, value) {
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

function setForecastTile(weather) {

    weather.forEach((value, key) => {

        let val;
        switch (key) {
            case 'description':
                val = translateCondition(value.value);

                let el = document.getElementById('weather_icon');
                el.className = 'mdi ' + val.icon;

                let text = val.text1 + " " + val.text2;
                el = document.getElementById('description');
                el.textContent = text;

                if (options.color_icons) {
                    jQuery(".weather_icon").css("cssText", "color: " + val.color + " !important");
                }
                break;
            case 'weather_icon':
                break;
            case 'dewpoint_desc':
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

    const url = `https://api.openweathermap.org/data/2.5/onecall?lat=` + latitude + `&lon=` + longitude + `&exclude=minutely&appid=` + tile_key + `&units=imperial`;

    let now = new Date();
    console.log("OpenWeather Data Refresh at " + now.toLocaleString());

    let weather = new Map();
    fetch(url)
        .then(response => response.json())
        .then(data => {
            let day = data.daily[options.display_day];
            console.log(day);
            options.measurements.forEach((measure) => {
                weather.set(measure.name, {
                    value: lookup(day, measure.openweather),
                    in_units: measure.in_unit,
                    out_units: measure.out_unit,
                    decimals: measure.decimals
                });
            });
            setForecastTile(weather);
        })

        .catch((error) => {
            console.log(error);
        });
}

function getCurrentWeather() {

    const currentWeatherUrl = `https://api.openweathermap.org/data/2.5/onecall?lat=` + latitude + `&lon=` + longitude + `&exclude=minutely&appid=` + tile_key + `&units=imperial`;

    const url = `https://api.openweathermap.org/data/2.5/onecall?lat=` + latitude + `&lon=` + longitude + `&exclude=minutely&appid=` + tile_key + `&units=imperial`;


    let now = new Date();
    console.log("OpenWeather Data Refresh at " + now.toLocaleString());

    let weather = new Map();
    fetch(currentWeatherUrl)
        .then(response => response.json())
        .then(data => {
            //console.log(data);



            options.measurements.forEach((measure) => {
                if (measure.openweather != "none") {
                    let value = lookup(data, measure.openweather);
                    if (value == undefined || value == "")
                        value = 0;

                    weather.set(measure.name, {
                                value: value,
                                in_units: measure.in_unit,
                                out_units: measure.out_unit,
                                decimals: measure.decimals
                    });
                }
            });
            //console.log(weather);
            setGridTile(weather);
        })

        .catch((error) => {
            //console.log(error);
        });
}

/******************************************************************************************************************************/
/****************************************GRIDSTACK IMPLEMENTATION *************************************************************/
/******************************************************************************************************************************/

var count = 12;
const num_columns = 26;
const num_rows = 26;


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
            console.log("x: "+item.x+" y: "+item.y);
            console.log("height: "+item.height+" width: "+item.width)
            obj.baseline_row = item.y;
            obj.baseline_column = item.x;
            obj.h = item.height;
            obj.w = item.width;

            setFont(item);

        }
            
    });
    updateGroovy();
});

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

function setGridTile(weather) {

    weather.forEach((value, key) => {

        let val;
        switch (key) {
            case 'description':
                tval = translateCondition(value.value);

                let el = document.getElementById('weather_icon_icon');
                el.className = 'mdi ' + tval.icon;
                
                val = tval.text1 + " " + tval.text2
                setText(val, key);

                if (options.color_icons) {
                    jQuery(".weather_icon").css("cssText", "color: " + val.color + " !important");
                }
                break;
            case 'weather_icon':
                break;
            case 'dewpoint_description':
                val = getDewPoint(value.value);
                setText(val, key);
                break;
            default:
                val = getString(value, value.out_units);
                val = isNaN(val) ? val : val.toFixed(value.decimals);
                setText(val, key);
        }

        let obj = getTile(key);
        obj.value = val;  
    });
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

var focusTile;
var dialog = document.getElementById('tileOptions');
var addTile = document.getElementById('addTile');

function getTile(name){
    return options.tiles.find(tile=> tile.var == name);
}


function getJustification() {
    if ($('#left_justify_button').hasClass("mdl-button--colored")) return "left";
    if ($('#mid_justify_button').hasClass("mdl-button--colored")) return "center";
    if ($('#right_justify_button').hasClass("mdl-button--colored")) return "right";
}

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
    $('#' + id).css('border-right', '5px solid red');
}
function setTileText(text, id) {
    let el = document.getElementById(id);
    if (el) el.textContent = text;
}

function setAlignment(alignment, id) {
    $('#' + id).css('text-align', alignment);
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
    
    //Set all the sliders and thier options
    setSlider("font_adjustment", "%", obj.font_adjustment);
    setSlider("text", "%", obj.font_opacity);
    setSlider("background", "%", obj.background_opacity);

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

    setText(null, focusTile);
    updateGroovy();
}

function setButton(t1, t2, t3) {
    let b1 = $('#' + t1 + "_justify_button");
    let b2 = $('#' + t2 + "_justify_button");
    let b3 = $('#' + t3 + "_justify_button");


    b1.addClass('mdl-button--colored');
    b2.removeClass('mdl-button--colored');
    b3.removeClass('mdl-button--colored');
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
    let obj = getTile(focusTile);
    
    //Set Display Field to False
    obj.display = false;

    //Remove the Physical Widget
    grid.removeWidget(item.el);
}

function addNewTile(var_name) {

    let obj = getTile(var_name);

    var $el = $(createNewTile(obj)); 
    grid.addWidget($el, 0, 0, obj.w, obj.h*2, true);

    obj.display = true;

    dialog.close();

    getCurrentWeather();

    //setText("", var_name);
}

function createNewTile(item){

    let fontScale = 4.6;
    let lineScale = 0.85;
    let iconScale = 3.5;
    let header = 0.1;
    let var_ = item.var;
    let height = item.h*2.0;
    let units = item.display_unit == "unknown" ? "" : item.display_unit;
     

    let html = "";
    html += `<div id="${var_}_tile_main" class="grid-stack-item" data-gs-id = "${var_}" data-gs-x="${item.baseline_column}" 
                    data-gs-y="${item.baseline_row*2-1}" data-gs-width="${item.w}" data-gs-height="${height}" data-gs-locked="false"
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
    html += `<span id="${var_}" style="color: ${item.font_color};"></span>`;
        
    //Units
    html+=`<span id="${var_}_units">${units}</span>`;  
        
    //Right Icon
    if (item.icon_loc == "right"){
        html+=`<span>${item.icon_space}</span>`;
        html+=`<span id="${var_}_icon" class="mdi mdi-${item.icon}" style="color: ${item.font_color};"></span>`;
    }
    html += `</div></div>`;
    
    return html;

}

function updateGroovy() {
    let data =  JSON.stringify(options.tiles);
    let el = jQuery("#settingsupdateDataLocation", parent.document);
    el.attr('value', data);
}
