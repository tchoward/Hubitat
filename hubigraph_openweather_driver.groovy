/*
/* Initial Release
*/
public static String version()      {  return '4.5.1'  }

metadata {
	definition (name: 'OpenWeather Hubigraph Driver',
		        namespace: 'tchoward',
		        author: 'Thomas Howard',
		        importUrl: '') {
                    capability 'Sensor'
            
	    attribute 'current_weather', 'string'
	    
        command 'pollData'
    }
    
    preferences() {
        def location = getLocation();
        
        section('Query Inputs'){
            input 'apiKey', 'text', required: true, defaultValue: 'Type OpenWeather API Key Here', title: 'API Key'
            input 'latitude', 'number', required: true, defaultValue: location.latitude, title: 'Latitude'
            input 'longitude', 'number', required: true, defaultValue: location.longitude, title: 'Longitude'
            input 'pollInterval', 'enum', title: 'Station Poll Interval', required: true, defaultValue: '10 Minutes', options: ['Manual Poll Only','1 Minute','5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
        }
    }
}

void updated(){
    switch (pollInterval) {
        case '1 Minute'     : runEvery1Minute(pollOpenWeather);   break;
        case '5 Minutes'    : runEvery5Minutes(pollOpenWeather);  break;
        case '10 Minutes'   : runEvery10Minutes(pollOpenWeather); break;
        case '15 Minutes'   : runEvery15Minutes(pollOpenWeather); break;
        case '30 Minutes'   : runEvery30Minutes(pollOpenWeather); break;
        case '1 Hour'       : runEvery1Hour(pollOpenWeather);     break;
        case '3 Hours'      : runEvery3Hours(pollOpenWeather);    break;
    }
}

void setupDevice(Map map){
    device.updateSetting("latitude",      [value: map.latitude,     type: "number"]);
    device.updateSetting("longitude",     [value: map.longitude,    type: "number"]);
    device.updateSetting("apiKey",        [value: map.apiKey,       type: "string"]);
    device.updateSetting("pollInterval",  [value: map.pollInterval, type: "string"]);
    
    updated();
}

void pollData(){
    pollOpenWeather();
}

// <<<<<<<<<< Begin Weather-Display Poll Routines >>>>>>>>>>
void pollOpenWeather() {
    if( apiKey== null ) {
        log.warn 'OpenWeatherMap.org Weather Driver - WARNING: OpenWeatherMap API Key not found.  Please configure in preferences.'
        return;
    }
    
    def ParamsOWM;
    
    state.ow_uri = 'https://api.openweathermap.org/data/2.5/onecall?lat=' + latitude + '&lon=' + longitude + '&exclude=minutely&mode=json&units=imperial&appid=' + apiKey;
    ParamsOWM = [ uri: state.ow_uri]
    //log.debug('Poll OpenWeatherMap.org: ' + ParamsOWM)
	asynchttpGet('openWeatherHandler', ParamsOWM)
    return;
}


void openWeatherHandler(resp, data) {
    log.debug('Polling OpenWeatherMap.org')
    
    if(resp.getStatus() != 200 && resp.getStatus() != 207) {
        log.warn 'Calling' + atomicState.ow_uri
        log.warn resp.getStatus() + ':' + resp.getErrorMessage()
	} else {
         sendEvent(name: 'current_weather', value: resp.data);
    }
}

