/**
 *  WeatherFlow Lite driver for Hubitat
 *
 *  Copyright (c) 2019 Justin Walker
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 *  v1.0.0 - initial version (2020-07-06)
 *  v1.0.1 - added strikeDistance (2020-07-08)
 *  v1.0.2 - initialize strike attrs (2020-07-09)
 *  v1.0.3 - added health check (2020-07-13)
 *  v1.0.4 - tchoward - Added Ability to update on a schedule
 */

import groovy.transform.Field

metadata {
	definition (name: 'WeatherFlow Lite', namespace: 'augoisms', author: 'Justin Walker', importUrl: 'https://raw.githubusercontent.com/augoisms/hubitat/master/weatherflow/weatherflow.driver.groovy') {
        capability 'IlluminanceMeasurement'
        capability 'Initialize'
        capability 'PressureMeasurement'
        capability 'RelativeHumidityMeasurement'	
        capability 'Sensor'
        capability 'TemperatureMeasurement'
        capability 'UltravioletIndex'

        attribute 'feelsLike', 'string'
        attribute 'heatIndex', 'number'
        attribute 'pressureTrend', 'enum', ['steady', 'falling', 'rising']
        attribute 'precipitationToday', 'number'
        attribute 'precipitationType', 'enum', ['none', 'rain', 'hail', 'mix']
        attribute 'solarRadiation', 'number'
        attribute 'strikeDetected', 'number'
        attribute 'strikeDistance', 'number'
        attribute 'windChill', 'number'
        attribute 'windDirection', 'string'
        attribute 'windSpeed', 'number'
	}

	preferences {
		input name: 'apiKey', type: 'string', title: '<b>WeatherFlow API Key</b>', description: '<div><i>Visit: <a href="https://weatherflow.github.io/SmartWeather/api/" target="_blank">WeatherFlow Smart Weather API</a></i></div><br>', required: true
        input name: 'stationId', type: 'string', title: '<b>Station ID</b>', description: '<div><i>ID of your station/hub.</i></div><br>', required: true
        
        if (connectionValidated()) {
            getStationDevices().each { k,v ->
                input name: "device_" + k, type: 'bool', title: "<b>Device: ${v}</b>", description: "<div><i>Subscribe to ${v} (${k}).</i></div><br>", required: true
            }
            input name: 'unitTemp', type: 'enum', title: '<b>Unit - Temp</b>', required: true, options: ['F': 'Fahrenheit (F)', 'C': 'Celsius (C)'], defaultValue: 1
            input name: 'unitWindDirection', type: 'enum', title: '<b>Unit - Wind Direction</b>', required: true, options: ['cardinal': 'Cardinal', 'degrees': 'Degrees'], defaultValue: 1
            input name: 'unitWindSpeed', type: 'enum', title: '<b>Unit - Wind Speed</b>', required: true, options: ['mph': 'Miles (mph)', 'kph': 'Kilometers (kph)', 'kn': 'Knots', 'm/s': 'Meters (m/s)'], defaultValue: 'mph'
            input name: 'unitPressure', type: 'enum', title: '<b>Unit - Pressure</b>', required: true, options: ['mb': 'Millibars (mb)', 'inHg': 'Inches of mercury (inHg)'], defaultValue: 'inHg'
            input name: 'unitRain', type: 'enum', title: '<b>Unit - Rain</b>', required: true, options: ['in': 'Inches (in)', 'mm': 'Millimeters (mm)'], defaultValue: 'in'
            input name: 'unitDistance', type: 'enum', title: '<b>Unit - Distance</b>', required: true, options: ['mi': 'Miles (mi)', 'km': 'Kilometers (km)'], defaultValue: 'mi'
            input name: 'refreshRate', type: 'enum', title: '<b>Refresh Rate for Events</b>', required: true, options: ['Real Time', '1 Minute', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours'], defaultValue: 'Real Time'
        }

		input name: 'logEnable', type: 'bool', title: 'Enable debug logging', defaultValue: false
	}
}

///
/// lifecycle events
///

void installed() {
    log.info 'installed()'
    updated()
}

void updated() {
    log.info 'updated()'

    removeDataValue('devices')
    removeDataValue('device_agl')
    removeDataValue('station_elevation')
    state.clear()

    //Unschedule any existing schedules
    unschedule()

    // perform health check every 5 minutes
    runEvery5Minutes('healthCheck') 
    
    log.debug("Running SendEvents every "+refreshRate);
    switch (refreshRate){
        case 'Real Time':                                     break;
        case '1 Minute':     runEvery1Minute(sendEvents);     break;
        case '5 Minutes':    runEvery5Minutes(sendEvents);    break;
        case '10 Minutes':   runEvery10Minutes(sendEvents);   break;
        case '15 Minutes':   runEvery15Minutes(sendEvents);   break;
        case '30 Minutes':   runEvery30Minutes(sendEvents);   break;
        case '1 Hour':       runEvery1Hour(sendEvents);       break;
        case '3 Hours':      runEvery3Hours(sendEvents);      break;  
    }

    // disable logs in 30 minutes
    if (settings.logEnable) runIn(1800, logsOff) 

    initialize()
}

                                         
void sendEvents(){
    def value;
    state.data.each{sensor->
              
        try {
             value = sensor.values.sum() / sensor.values.size();
        } catch (Exception e) {
             value = sensor.values[sensor.values.size()-1];   
        }
        
        sendEvent(name: sensor.name, value: value, unit: sensor.unit);
    }
    
    state.data = [];
}

void sendEventLocal(Map m){
    
    if (refreshRate == 'Real Time'){
         sendEvent(m);
         return;
    }
    
    if (!state.data){
        state.data = [];
    }
    
    test = state.data.findAll{ it.name == m.name };
    if (test == []) {
        state.data << [name: m.name, values: [m.value], unit: m.unit];
    } else {
        test.each{sensor->
             sensor.values << m.value;       
        }
    }
}




void initialize() {
    log.info 'initialize()'

    unschedule(initialize)

    // close websocket
    interfaces.webSocket.close()
    pauseExecution(1000)

    if (!connectionValidated()) {
        log.warn 'Connection not validated. Cannot initialized.'
        return
    }

    // generate a new ws id
    String ws_id = UUID.randomUUID().toString()
    updateDataValue('ws_id', ws_id)
    
    try {
        // connect webSocket to weatherflow
        interfaces.webSocket.connect("wss://ws.weatherflow.com/swd/data?api_key=${apiKey}")
    } 
    catch (e) {
        log.error "webSocket.connect failed: ${e.message}"
    }
}

def parse(String description) {
    logDebug "parsed: $description"
    
    try {
        def response = null;
        response = new groovy.json.JsonSlurper().parseText(description)
        if (response == null){
            log.warn 'String description not parsed'
            return
        }

        switch (response.type) {
            case 'obs_air':
            case 'obs_sky':
            case 'obs_st':
                parseObservation(response)
                break
            case 'evt_precip':
                sendEventLocal(name: 'precipitationType', value: 'rain')
                logDebug 'precipitationType: rain'
                break
            case 'evt_strike':
                parseStrikeEvent(response)
                break
            case 'ack':
            case 'connection_opened':
                logDebug "response type: ${response.type}"
                break
            default:
                log.warn "Unhandled event: ${response}"
                break
        }

        if (response.containsKey('summary')) parseSummary(response)
    }  
    catch(e) {
        log.error "Failed to parse json e = ${e}"
        log.debug description 
        return
    }
}

///
/// station metadata
///

Boolean connectionValidated() {   
    // api key and stationId are required
    if(!apiKey || !stationId) {
        log.warn 'apiKey and stationId are required'
        return false
    }

    // check for cached results
    if (getDataValue('devices')) return true 

    Boolean validated = false

    try {
        Map params = [
            uri: "https://swd.weatherflow.com/swd/rest/stations/${settings.stationId}?api_key=${settings.apiKey}",
            requestContentType: 'application/json',
            contentType: 'application/json'
        ]

        httpGet(params) { response -> 
            if (response.status != 200) {
                log.warn "Could not get station metadata. Error: ${response.status}"
            }
            else {                
                List devices = []
                response.data.stations[0].devices.each { it ->
                    // a device wihout a serial_number indicates that device is no longer active
                    // device_type of 'HB' is the hub
                    if (it.serial_number && it.device_type != 'HB') {
                        Map device = [
                            'device_id': it.device_id,
                            'device_type': it.device_type,
                            'serial_number': it.serial_number,
                            'agl': it.device_meta.agl,
                            'name': it.device_meta.name
                        ]
                        devices.add(device)
                    }

                    // save agl for Tempest (ST) or Air (AR)
                    if (it.device_type == 'ST' || it.device_type == 'AR') {
                        updateDataValue('device_agl', "${it.device_meta.agl}")
                    }
                }

                updateDataValue('devices', new groovy.json.JsonBuilder(devices).toString())

                // save station elevation
                updateDataValue('station_elevation', "${response.data.stations[0].station_meta.elevation}")
                
                validated = true
            }
        }
    }
    catch (e) {
        log.error e.message
    }

    return validated
}

Map getStationDevices() {
    String deviceData = getDataValue('devices')
    if (!deviceData) return [:]

    ArrayList devices = new groovy.json.JsonSlurper().parseText(deviceData)
    Map deviceConfig = [:]

    devices.each { it -> 
        deviceConfig << ["${it.device_id}":it.name]
    }

    return deviceConfig
}

///
/// websocket connection
///

void webSocketStatus(String status){
    logDebug "webSocketStatus: ${status}"

    if (status.startsWith('failure: ')) {
        log.warn "failure message from web socket ${status}"
        state.connection = 'disconnected'
        reconnectWebSocket()
    } 
    else if (status == 'status: open') {        
        log.info 'webSocket is open'
        state.connection = 'connected'

        requestData()

        // success! reset reconnect delay
        pauseExecution(1000)
        state.reconnectDelay = 1
    } 
    else if (status == 'status: closing'){
        log.warn 'webSocket connection closing.'
        state.connection = 'closing'
    } 
    else {
        log.warn "webSocket error: ${status}"
        state.connection = 'disconnected'
        reconnectWebSocket()
    }
}

void reconnectWebSocket() {
    // first delay is 2 seconds, doubles every time
    state.reconnectDelay = (state.reconnectDelay ?: 1) * 2

    // don't let delay get too crazy, max it out at 10 minutes
    if(state.reconnectDelay > 600) state.reconnectDelay = 600

    // if the station is offline, give it some time before trying to reconnect
    log.info "Reconnecting WebSocket in ${state.reconnectDelay} seconds."
    runIn(state.reconnectDelay, initialize, [overwrite: false])
}

void requestData() {
    getStationDevices().each { k, v ->
        if (settings["device_${k}"] == true) {
            Map listenStart = [
                'type': 'listen_start', // response types: ack, obs_air, obs_sky, obs_st, evt_strike, evt_precip
                'device_id': k,
                id: getDataValue('ws_id')
            ]
            String message = new groovy.json.JsonBuilder(listenStart).toString()
            interfaces.webSocket.sendMessage(message)
            log.info "Sent list_start for ${k}"
        }
    }
}

void healthCheck() {
    if (state.lastObservation != null) {
        // check if there have been any observations in the last 3 minutes
        if(state.lastObservation >= now() - (3 * 60 * 1000)) {
            // healthy
            logDebug 'healthCheck: healthy'
        }
        else {
            // not healthy
            log.warn 'healthCheck: not healthy'
            reconnectWebSocket()
        }
    }
    else {
        log.info 'No previous activity. Cannot determine health.'
    }
}

///
/// data parsing
///

@Field static Map EVT_STRIKE = [
    0: 'epoch',    // seconds utc,
    1: 'distance', // km
    2: 'energy'
]

@Field static Map OBS_AIR = [
    0:  'epoch',                             // seconds utc
    1:  'station_pressure',                  // mb
    2:  'air_temperature',                   // celcius
    3:  'relative_humidity',                 // %
    4:  'lightning_strike_count',
    5:  'lightning_strike_average_distance', // km
    6:  'battery',                           // volts
    7:  'report_interval'                    // minutes
]

@Field static Map OBS_SKY = [
    0:  'epoch',                             // seconds utc
    1:  'illuminance',                       // lux
    2:  'uv_index',
    3:  'rain_accumulation',                 // mm
    4:  'wind_lull',                         // m/s
    5:  'wind_avg',                          // m/s
    6:  'wind_gust',                         // m/s
    7:  'wind_direction',                    // degrees
    8:  'battery',                           // volts
    9:  'report_interval',                   // minutes
    10: 'solar_radiation',                   // W/m^2
    11: 'local_day_rain_accumulation',       // mm
    12: 'precipitation_type',                // 0 = none, 1 = rain, 2 = hail, 3 = rain/hail
    13: 'wind_sample_interval',              // seconds
    14: 'rain_accumulation_final',           // mm (rain check)
    15: 'local_day_rain_accumulation_final', // mm (rain check)
    16: 'precipitation_analysis_type'        // 0 = none, 1 = rain check with user display on, 2 = rain check with user display off
]

@Field static Map OBS_ST = [
    0:  'epoch',                             // seconds UTC
    1:  'wind_lull',                         // m/s
    2:  'wind_avg',                          // m/s
    3:  'wind_gust',                         // m/s
    4:  'wind_direction',                    // degrees
    5:  'wind_sample_interval',              // seconds
    6:  'station_pressure',                  // mb
    7:  'air_temperature',                   // celcius
    8:  'relative_humidity',                 // %
    9:  'illuminance',                       // lux
    10: 'uv_index',
    11: 'solar_radiation',                   // W/m^2
    12: 'rain_accumulation',                 // mm (for current interval)
    13: 'precipitation_type',                // 0 = none, 1 = rain, 2 = hail, 3 = rain/hail
    14: 'lightning_strike_average_distance', // km
    15: 'lightning_strike_count',
    16: 'battery',                           // volts
    17: 'report_interval',                   // minutes
    18: 'local_day_rain_accumulation',       // mm (for current day, midnight to midnight)
    19: 'rain_accumulation_final',           // mm (rain check)
    20: 'local_day_rain_accumulation_final', // mm (rain check)
    21: 'precipitation_analysis_type'        // 0 = none, 1 = rain check with user display on, 2 = rain check with user display off
]

void parseStrikeEvent(Map response) {
    Map strikeDistance = formatDistance(response.evt[1])
    sendEventLocal(name: 'strikeDetected', value: response.evt[0])
    sendEventLocal(name: 'strikeDistance', value: strikeDistance.value, unit: strikeDistance.unit)
    logDebug "strikeDetected: ${strikeDistance.value} ${strikeDistance.unit}"
}

void parseObservation(Map response) {

    Map obsMap
    switch (response.type) {
        case 'obs_air':
            obsMap = OBS_AIR
            break;
        case 'obs_sky':
            obsMap = OBS_SKY
            break;
        case 'obs_st':
            obsMap = OBS_ST
            break
    }

    response.obs[0].eachWithIndex { it, i -> 
        String field = obsMap[i]

        if (field == 'air_temperature') {
            Map temp = formatTemp(it)
            sendEventLocal(name: 'temperature', value: temp.value, unit: temp.unit)
            logDebug "${field}: ${temp.value} ${temp.unit}"
        }
        
        if (field == 'battery') {
            state["battery_${response.device_id}"] = "${it}V"
            logDebug "${field}: ${it}V"
        }

        if (field == 'illuminance') {
            sendEventLocal(name: 'illuminance', value: it, unit: 'lux')
            logDebug "${field}: ${it} lux"
        }
        
        if (field == 'local_day_rain_accumulation') {
            Map precipAmount = formatPrecipitationAmount(it)
            sendEventLocal(name: 'precipitationToday', value: precipAmount.value, unit: precipAmount.unit)
            logDebug "${field}: ${precipAmount.value} ${precipAmount.unit}"
        }

        if (field == 'precipitation_type') {
            Map precipType = formatPrecipitationType(it)
            sendEventLocal(name: 'precipitationType', value: precipType.value)
            logDebug "${field}: ${precipType.value}"
        }

        if (field == 'relative_humidity') {
            sendEventLocal(name: "humidity", value: it, unit: "%")
            logDebug "${field}: ${it}%"
        }

        if (field == 'solar_radiation') {
            sendEventLocal(name: 'solarRadiation', value: it, unit: 'W/m^2')
            logDebug "${field}: ${it} W/m^2"
        }

        if (field == 'station_pressure') {
            Map pressure = formatPressure(it)
            sendEventLocal(name: 'pressure', value: pressure.value, unit: pressure.unit)
            logDebug "${field}: ${pressure.value} ${pressure.unit}"
        }

        if (field == 'uv_index') {
            sendEventLocal(name: 'ultravioletIndex', value: it,)
            logDebug "${field}: ${it}"
        }

        if (field == 'wind_avg') {
            Map windSpeed = formatWindSpeed(it)
            sendEventLocal(name: 'windSpeed', value: windSpeed.value, unit: windSpeed.unit)
            logDebug "${field}: ${windSpeed.value} ${windSpeed.unit}"
        }

        if (field == 'wind_direction') {
            Map windDirection = formatWindDirection(it)
            sendEventLocal(name: 'windDirection', value: windDirection.value, unit: windDirection.unit)
            logDebug "${field}: ${windDirection.value} ${windDirection.unit}"
        }        
    }

    // init strike attributes so that they are available
    if (device.currentValue('strikeDetected') == null) { sendEventLocal(name: 'strikeDetected', value: 0) }
    if (device.currentValue('strikeDistance') == null) { sendEventLocal(name: 'strikeDistance', value: 0) }

    state.lastObservation = now()
}

// summary is undocumented and therefore not guaranteed, however WeatherFlow recognizes people are using these values
// https://community.weatherflow.com/t/heads-up-forthcoming-changes-to-api-rest-ws/3601
// if these were to go away, they could be calculated in the driver
// https://weatherflow.github.io/SmartWeather/api/derived-metric-formulas.htm
void parseSummary(Map response) {
    if (response.summary.containsKey('pressure_trend')) {
        sendEventLocal(name: 'pressureTrend', value: response.summary.pressure_trend)
    }    
    
    if (response.summary.containsKey('feels_like')) {
        Map feelsLike = formatTemp(response.summary.feels_like)
        sendEventLocal(name: 'feelsLike', value: feelsLike.value, unit: feelsLike.unit)
    }
    
    if (response.summary.containsKey('heat_index')) {
        Map heatIndex = formatTemp(response.summary.heat_index)
        sendEventLocal(name: 'heatIndex', value: heatIndex.value, unit: heatIndex.unit)
    }
    
    if (response.summary.containsKey('wind_chill')) {
        Map windChill = formatTemp(response.summary.wind_chill)
        sendEventLocal(name: 'windChill', value: windChill.value, unit: windChill.unit)
    }    
}

///
/// formatters
///

Map formatDistance(Integer value) {
    Boolean metric = (settings.unitDistance == 'km')
    return [
        value: metric ? value : value / 1.609344,
        unit: metric ? 'km' : 'mi'
    ]
}

Map formatPrecipitationAmount(BigDecimal value) {
    Boolean inches = (settings.unitRain == 'in')
    return [
        value: inches ? value / 25.4 : value,
        unit: inches ? 'in' : 'mm'
    ]
}

Map formatPrecipitationType(Integer value) {
    String type = 'none'
    switch (value) {
        case 0:
            type = 'none'
            break
        case 1:
            type = 'rain'
            break
        case 2:
            type = 'hail'
            break
        case 3:
            type = 'mix'
            break
    }

    return [
        value: type,
        unit: ''
    ]
}

Map formatPressure(BigDecimal value) {
    Boolean mb = (settings.unitPressure == 'mb')
    BigDecimal seaLevelPressure = calculateSeaLevelPressure(value)
    return [
        value: mb ? seaLevelPressure : round3(seaLevelPressure / 33.864),
        unit: mb ? 'mb' : 'inHg'
    ]
}

Map formatTemp(BigDecimal value) {
    Boolean fahrenheit = (settings.unitTemp == 'F')
    return [
        value: fahrenheit ? (value * 1.8) + 32 : value,
        unit: fahrenheit ? 'F' : 'C'
    ]
}

@Field static String[] CARDINAL_POINTS = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW']

Map formatWindDirection(Integer value) {
    Boolean cardinal = (settings.unitWindDirection == 'cardinal')
    String windDirection = "${value}"

    if (cardinal) {
        Double val = Math.floor((value / 22.5) + 0.5);
        windDirection = CARDINAL_POINTS[(val % 16).intValue()]
    }

    return [
        value: windDirection,
        unit: cardinal ? '' : '°'
    ]
}

Map formatWindSpeed(BigDecimal value) {
    String unit = settings.unitWindSpeed
    BigDecimal speed = value

    switch (unit) {
        case 'mph':
            speed = value * 2.23694
            break
        case 'kph':
            speed = value * 3.6
            break
        case 'kn':
            speed = value * 1.9438445
            break
    }

    return [
        value: round2(speed),
        unit: unit
    ]
}

///
/// helper methods
///

// https://weatherflow.github.io/SmartWeather/api/derived-metric-formulas.html#sea-level-pressure
@Field static BigDecimal STANDARD_SEA_LEVEL_PRESSURE = 1013.25   // mb
@Field static BigDecimal GAS_CONSTANT_DRY_AIR = 287.05           // J/(kg*K)
@Field static BigDecimal STANDARD_ATMOSPHERE_LAPSE_RATE = 0.0065 // K/m
@Field static BigDecimal GRAVITY = 9.80665                       // m/s^2
@Field static BigDecimal STANDARD_SEA_LEVEL_TEMPERATURE = 288.15 // K

BigDecimal calculateSeaLevelPressure(BigDecimal input) {
    BigDecimal stationPressure = input
    BigDecimal elevation = new BigDecimal(getDataValue('station_elevation')) + new BigDecimal(getDataValue('device_agl'))

    BigDecimal calc_a_exp = (GAS_CONSTANT_DRY_AIR * STANDARD_ATMOSPHERE_LAPSE_RATE) / GRAVITY
    BigDecimal calc_a = Math.pow((STANDARD_SEA_LEVEL_PRESSURE / stationPressure).doubleValue(), calc_a_exp.doubleValue())

    BigDecimal calc_b = (elevation * STANDARD_ATMOSPHERE_LAPSE_RATE) / STANDARD_SEA_LEVEL_TEMPERATURE

    BigDecimal calc_c_exp = GRAVITY / (GAS_CONSTANT_DRY_AIR * STANDARD_ATMOSPHERE_LAPSE_RATE)
    BigDecimal calc_c = Math.pow((1 + (calc_a * calc_b)).doubleValue(), calc_c_exp.doubleValue())

    BigDecimal seaLevelPressure = stationPressure * calc_c

    return seaLevelPressure
}

void logDebug(str) {
    if (settings.logEnable) {
        log.debug str
    }
}

void logsOff() {
    log.info 'Logging disabled.'
    device.updateSetting('logEnable',[value:'false',type:'bool'])
}

BigDecimal round3(BigDecimal value) {
    return Math.round(value * 1000) / 1000
}

BigDecimal round2(BigDecimal value) {
    return Math.round(value * 100) / 100
}
