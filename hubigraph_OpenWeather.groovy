/*
/* Initial Release
*/
public static String version()      {  return '4.5.1'  }
import groovy.transform.Field

metadata {
	definition (name: 'Open Weather Hubigraph Driver',
		        namespace: 'tchoward',
		        author: 'Thomas Howard',
		        importUrl: '') {
                    capability 'Sensor'
            
	    attribute 'current_weather', 'string'
	    
        command 'pollData'
    }
    
    preferences() {
        section('Query Inputs'){
            input 'apiKey', 'text', required: true, defaultValue: 'Type DarkSky.net API Key Here', title: 'API Key'
            input 'latitude', 'text', required: true, defaultValue: 'Type DarkSky.net API Key Here', title: 'API Key'
            input 'longitude', 'text', required: true, defaultValue: 'Type DarkSky.net API Key Here', title: 'API Key'
            input 'pollInterval', 'enum', title: 'Station Poll Interval', required: true, defaultValue: '10 Minutes', options: ['Manual Poll Only', '1 Minute', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
        }
    }
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
    ParamsOWM = [ uri: atomicState.ow_uri]
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
         sendEvent(name: 'current_weather', value: parseJson(resp.data));
    }
}

