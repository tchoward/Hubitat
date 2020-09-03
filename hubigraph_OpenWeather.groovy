/*
  Weather-Display With DarkSky.net Forecast Driver
   Import URL: https://raw.githubusercontent.com/HubitatCommunity/Weather-Display-With-DarkSky.net-Forecast-Driver/master/Weather-Display%20With%20DarkSky.net%20Forecast%20Driver.groovy
   Copyright 2020 @Matthew (Scottma61)
 
	Many people contributed to the creation of this driver.  Significant contributors include:
	- @Cobra who adapted it from @mattw01's work and I thank them for that!
	- @bangali for his original APIXU.COM base code that much of the early versions of this driver was
	adapted from. 
	- @bangali for his the Sunrise-Sunset.org code used to calculate illuminance/lux and the more
	recent adaptations of that code from @csteele in his continuation driver 'wx-ApiXU'.
	- @csteele (and prior versions from @bangali) for the attribute selection code.
	- @csteele for his examples on how to convert to asyncHttp calls to reduce Hub resource utilization.
	- @bangali also contributed the icon work from
	https://github.com/jebbett for new cooler 'Alternative' weather icons with icons courtesy
	of https://www.deviantart.com/vclouds/art/VClouds-Weather-Icons-179152045.
	- 'waynedgrant' for his json webservice that make the weather station data available to the driver.
	- @storageanarchy for his Dark Sky Icon mapping and some new icons to compliment the Vclouds set.
    - @nh.schottfam for lots of code clean up and optimizations.

	In addition to all the cloned code from the Hubitat community, I have heavily modified/created new
	code myself @Matthew (Scottma61) with lots of help from the Hubitat community.  If you believe you
	should have been acknowledged or received attribution for a code contribution, I will happily do so.
	While I compiled and orchestrated the driver, very little is actually original work of mine.

	This driver is free to use.  I do not accept donations. Please feel free to contribute to those
	mentioned here if you like this work, as it would not have been possible without them.

 *********************************************************************************************************
 *  REQUIREMENTS:  You MUST have a Personal Weather Station (PWS) and use Weather-Display software to    *
 *  capture that weather data from your network or a web server.  If you do not meet this requirement    *
 *  then this driver will not work for you.  This uses the Weather-Display data files from a webserver   *
 *  you specify in the driver preferences. I used waynedgrant's work to make those data files available  *
 *  in JSON format (https://github.com/waynedgrant/json-webservice-wdlive).                              *
 *********************************************************************************************************

	This driver is intended to pull data from data files on a web server created by Weather-Display software
	(http://www.weather-display.com).  It will also supplement forecast data from Dark Sky (DS)
	(http://darksky.net).  You will need your DarkSky API key to use the forecast from that sites,
	but the driver it will work without an external forecast source.

	The driver uses the Weather-Display data as the primary current weather dataset.  There are a few options you can select
	from like using your forecast source for illuminance/solar radiation/lux if you do not have those sensors.
	You can also select to use a base set of condition icons from the forecast source, or an 'alternative'
	(fancier) set.  The base 'Standard' icon set will be from WeatherUnderground.  You may choose the
	fancier 'Alternative' icon set if you use the Dark Sky.

	The driver exposes both metric and imperial measurements for you to select from.

	Licensed under the Apache License, Version 2.0 (the 'License'); you may not use this file except
	in compliance with the License. You may obtain a copy of the License at:

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
	on an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
	for the specific language governing permissions and limitations under the License.

	Last Update 04/22/2020
	{ Left room below to document version changes...}





    V4.5.1   Bug fixes                                                                         - 04/22/2020
	V4.5.0   Refactored the code, Improved log messaging, bug fixes                            - 04/21/2020
	V4.4.9   More checks of coordinates to prevent/warn of null values                         - 03/26/2020
	V4.4.8   Added some debugging helpers and code to remove any spaces in location coordinates- 03/22/2020
	V4.4.7   Allow location override. Corrected forecastHigh/Low to 'number' from 'string'     - 03/20/2020
	V4.4.6   Changed links for they Open in new tabs/windows.                                  - 03/01/2020
	V4.4.5   Enhancements to myTile and threedayfcstTile, NEW alertTile                        - 02/28/2020
	V4.4.4   Changed forecasts to use temperatureMax/Min instead of temperatureHigh/Low        - 02/27/2020
		from the Dark Sky API to match their website presentation.
	V4.4.3   Updated (reduced) logging and the behavior of 'refresh' (use 'pollData' instead   - 02/26/2020
		to force a polling of data.
	V4.4.2   Further bug squashing                                                             - 02/24/2020 8:20 PM EDT
	V4.4.1   Corrected bug from 1.3.0 that made some attrubutes strings instead of numbers     - 02/24/2020
	V4.4.0   Added ability to select displayed decimals                                        - 02/23/2020
	V4.3.9   Fixed pressured definition to avoid excess events                                 - 12/15/2019
	V4.3.8   Exposed 'feelsLike' so it gets updated                                            - 11/11/2019
	V4.3.7   Force three day forcast icons to be 'daytime' (instead of 'nighttime')            - 10/23/2019
	V4.3.6   Changed 'pressure' to a number from a string, added 'pressured' as a string.      - 10/22/2019
	V4.3.5   Added three day forecast tile                                                     - 10/22/2019
	V4.3.4   added meters per second ('m/s') for wind and hectopascals for pressure,           - 10/14/2019
		added ability to use DarkSky for wind instead of your weather station.
	V4.3.3   forecastIcon & weatherIcon fix.  Tuned Lux for 'fully nighttime'                  - 10/13/2019
	V4.3.2   Bug fix for is_day/is_light                                                       - 10/02/2019
	V4.3.1   Added ability to show 'knots' for wind/gust speeds                                - 10/01/2019
	V4.3.0   Eliminated 'Std' Icons.  Reworked condition_code/condition_text.                  - 09/30/2019
	V4.2.9   myTile format tweaking                                                            - 09/29/2019
	V4.2.8   Bug fixes, optimizations, Added 'wind' and lux jitter control                     - 09/28/2019
	V4.2.7   More myTile 'display:inline' corrections                                          - 09/28/2019
	V4.2.6   myTile 'display:inline' correction                                                - 09/27/2019
	V4.2.5   myTile enhancement for excessive length                                           - 09/27/2019
	V4.2.4   Prevent myTile from exceeding 1,024 characters                                    - 09/26/2019
	V4.2.3   Corrected myTile for 'alert' condition                                            - 09/26/2019
	V4.2.2 - Added 'wind_cardinal', more code optimization and cleanup                         - 09/25/2019
	V4.2.1 - Optimized lux updates and code optimizations re-organized preference order        - 09/24/2019
		and some goupings of 'optional' attributes.
	V4.2.0 - Randomized schedule start times, Added 'Powered by DarkSky' attribution           - 09/18/2019
	V4.1.9 - Default to 'TinyURL' for icon location, added log when changeing schedule         - 09/16/2019
	V4.1.8 - Changed icon location to prevent duplication - Please update icon file location   - 09/16/2019
	V4.1.7 - Moved driver to the HubitatCommunity github, added 'Nighttime' schedule option    - 09/16/2019
		added upDateCheck() to show if driver is current (thanks @csteele)
	V4.1.6   Another optional attribute bug fix.                                               - 09/15/2019
	V4.1.5   Tweaking and bug fixes.                                                           - 09/14/2019
	V4.1.4   Added 'weatherIcons' used for OWM icons/dashboard                                 - 09/14/2019
	V4.1.3   Added windSpeed and windDirection, required for some dashboards.                  - 09/14/2019
	V4.1.2   Attribute now dislplayed for dashboards ** Read caution below **                  - 09/14/2019
	V4.1.1 - bug fixes                                                                         - 09/13/2019
	V4.1.0 - Initial release of driver with ApiXU.com completely removed.                      - 09/12/2019
----------------------------------------------------------------------------------------------------------
	V4.0.0 - Complete re-write: asyncHttp calls, selectable attributes, many corrections       - 09/09/2019
	V3.0.4 - Further myTile character reduction.                                               - 03/24/2019
	V3.0.3 - Altered myTile to attempt to remain under to 1024 charater limit for Dashboard 2.0- 03/20/2019
	V3.0.2 - Instruction clarificatons and log improvements                                    - 03/19/2019
	V3.0.1 - Code tweaks and corrections.                                                      - 03/16/2019
	V3.0.0 - Major code optimization/reorganization - Removed WU option                        - 02/16/2019
	V2.1.6 - Format cleanup; improved Dark Sky condition mapping                               - 01/21/2019
	V2.1.5 - myTile redo - added icons, Pressure, Chance of rain, Precipitation                - 01/12/2019
	V2.1.4 - Bug fix for Dark Sky condition_code/text/icon values; Added alerts to myTile      - 01/06/2019
	V2.1.3 - Code cleanup/correction - no functionality changes                                - 01/05/2019
	V2.1.2 - Added DarkSky as an external forecast source                                      - 01/04/2019
	V2.1.1 - Added Apparent temp ('Feels Like') to myTile                                      - 12/31/2018
	V2.1.0 - Tweaked myTile attribute. Removed 'isStateChange:true' from sendEvents            - 12/30/2018
	V2.0.9 - Added a variation of @arnb's myTile attribute                                     - 12/9/2018
	V2.0.8 - Declared attributes: alert, twilight_begin, twilight_end, weatherSummary          - 11/3/2018
	V2.0.7 - Changed sunrise-sunset.org api from https: to http:                               - 9/21/2018
	V2.0.6 - More cleanup of table lookups/translations.                                       - 9/03/2018
	V2.0.5 - Consolidated table lookups (transform.field), cleaned up forecast translations    - 8/20/2016
	V2.0.4 - Translated forecastIcon for APIXU to WU equivalent                                - 8/16/2018
	V2.0.3 - Added forecastIcon attribute for SharpTools.io, various code cleanups.            - 8/16/2018
	V2.0.2 - Added hemisphere selectors for correct Lon/Lat on station.  Removed '?raw=true'   - 8/12/2018
		suffix from alternative icon file location if not on 'github.com'.
	V2.0.1 - Code cleanup; Added 'Observation' times; Changed 'Update' time to 'Poll' time     - 8/11/2018
		corrected display of some options variables (Illuminance/UV/FeelsLike) when no forecast source selected.
	V2.0.0 - New version completely rebuilt 08/10/2018

- Made changes to Attributes.  Attributes should now be available for dashboards.  *CAUTION - READ BELOW*

**ATTRIBUTES CAUTION**
The way the 'optional' attributes work:
	- Initially, only the optional attributes selected will show under 'Current States' and will be available in
	dashboards.
	- Once an attribute has been selected it too will show under 'Current States' and be available in dashboards.
	<*** HOWEVER ***> If you ever de-select the optional attribute, it will still show under 'Current States'
	and will still show as an attribute for dashboards **BUT IT'S DATA WILL NO LONGER BE REFRESHED WITH DATA
	POLLS**.  This means what is shown on the 'Current States' and dashboard tiles for de-selected attributes
	may not be current valid data.
	- To my knowledge, the only way to remove the de-selected attribute from 'Current States' and not show it as
	available in the dashboard is to delete the virtual device and create a new one AND DO NOT SELECT the
	attribute you do not want to show.
*/
public static String version()      {  return '4.5.1'  }
import groovy.transform.Field

metadata {
	definition (name: 'Weather-Display With DarkSky.net Forecast Driver',
		namespace: 'Matthew',
		author: 'Scottma61',
		importUrl: 'https://raw.githubusercontent.com/HubitatCommunity/Weather-Display-With-DarkSky.net-Forecast-Driver/master/Weather-Display%20With%20OpenWeatherMap%20Forecast%20Driver.groovy') {
        capability 'Sensor'
        capability 'Temperature Measurement'
        capability 'Illuminance Measurement'
        capability 'Relative Humidity Measurement'
        capability 'Pressure Measurement'
        capability 'Ultraviolet Index'

        capability 'Refresh'        

		attributesMap.each
		{
            k, v -> if (v.typeof)        attribute k, v.typeof
		}
//    The following attributes may be needed for dashboards that require these attributes,
//    so they are listed here and shown by default.
        attribute 'city', 'string'              //Hubitat  OpenWeather  SharpTool.io  SmartTiles
        attribute 'feelsLike', 'number'         //SharpTool.io  SmartTiles
        attribute 'forecastIcon', 'string'      //SharpTool.io
        attribute 'localSunrise', 'string'      //SharpTool.io  SmartTiles
        attribute 'localSunset', 'string'       //SharpTool.io  SmartTiles
        attribute 'percentPrecip', 'number'     //SharpTool.io  SmartTiles
        attribute 'pressured', 'string'         //UNSURE SharpTool.io  SmartTiles
        attribute 'weather', 'string'           //SharpTool.io  SmartTiles
        attribute 'weatherIcon', 'string'       //SharpTool.io  SmartTiles
        attribute 'weatherIcons', 'string'      //Hubitat  openWeather
        attribute 'wind', 'number'              //SharpTool.io
        attribute 'windDirection', 'number'     //Hubitat  OpenWeather
        attribute 'windSpeed', 'number'         //Hubitat  OpenWeather

//      The attributes below are sub-groups of optional attributes.  They need to be listed here to be available
//alert
        attribute 'alert', 'string'
        attribute 'alertTile', 'string'

//threedayTile
        attribute 'threedayfcstTile', 'string'

//DSAttribution
        attribute 'dsIcondarktext', 'string'
        attribute 'dsIconlighttext', 'string'
		
//fcstHighLow
        attribute 'forecastHigh', 'number'
        attribute 'forecastLow', 'number'

// controlled with localSunrise
        attribute 'tw_begin', 'string'
        attribute 'sunriseTime', 'string'
        attribute 'noonTime', 'string'
        attribute 'sunsetTime', 'string'
        attribute 'tw_end', 'string'

//obspoll
        attribute 'last_poll_Forecast', 'string'
        attribute 'last_observation_Forecast', 'string'

//precipExtended
        attribute 'rainDayAfterTomorrow', 'number'
        attribute 'rainTomorrow', 'number'

//nearestStorm
        attribute 'nearestStormBearing', 'string'
        attribute 'nearestStormCardinal', 'string'
        attribute 'nearestStormDirection', 'string'
        attribute 'nearestStormDistance', 'number'

        command 'pollData'
    }
    def settingDescr = settingEnable ? '<br><i>Hide many of the optional attributes to reduce the clutter, if needed, by turning OFF this toggle.</i><br>' : '<br><i>Many optional attributes are available to you, if needed, by turning ON this toggle.</i><br>'
    def logDescr = '<br><i>Extended logging will turn off automatically after 30 minutes.</i><br>'

    preferences() {
        section('Query Inputs'){
            input 'extSource', 'enum', title: 'Select Forecast Source', required:true, defaultValue: 1, options: [1:'Weather-Display', 2:'DarkSky']
            input 'apiKey', 'text', required: true, defaultValue: 'Type DarkSky.net API Key Here', title: 'API Key'
            input 'pollIntervalStation', 'enum', title: 'Station Poll Interval', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '1 Minute', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
            input 'pollLocationStation', 'text', required: true, title: 'Station Data File Location:', defaultValue: 'http://'
            input 'pollIntervalForecast', 'enum', title: 'External Source Poll Interval (daylight)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
            input 'pollIntervalForecastnight', 'enum', title: 'External Source Poll Interval (nighttime)', required: true, defaultValue: '3 Hours', options: ['Manual Poll Only', '2 Minutes', '5 Minutes', '10 Minutes', '15 Minutes', '30 Minutes', '1 Hour', '3 Hours']
            input 'logSet', 'bool', title: 'Enable Extended Logging', description: '<i>Extended logging will turn off automatically after 30 minutes.</i>', required: true, defaultValue: false
            input 'tempFormat', 'enum', required: true, defaultValue: 'Fahrenheit (°F)', title: 'Display Unit - Temperature: Fahrenheit (°F) or Celsius (°C)',  options: ['Fahrenheit (°F)', 'Celsius (°C)']
            input 'TWDDecimals', 'enum', required: true, defaultValue: '0', title: 'Display decimals for Temp, Wind & Distance', options: [0:'0', 1:'1', 2:'2', 3:'3', 4:'4']
            input 'PDecimals', 'enum', required: true, defaultValue: '0', title: 'Display decimals for Pressure', options: [0:'0', 1:'1', 2:'2', 3:'3', 4:'4']
            input 'RDecimals', 'enum', required: true, defaultValue: '0', title: 'Display decimals for Rain volume', options: [0:'0', 1:'1', 2:'2', 3:'3', 4:'4']            
            input 'datetimeFormat', 'enum', required: true, defaultValue: '1', title: 'Display Unit - Date-Time Format',  options: [1:'m/d/yyyy 12 hour (am|pm)', 2:'m/d/yyyy 24 hour', 3:'mm/dd/yyyy 12 hour (am|pm)', 4:'mm/dd/yyyy 24 hour', 5:'d/m/yyyy 12 hour (am|pm)', 6:'d/m/yyyy 24 hour', 7:'dd/mm/yyyy 12 hour (am|pm)', 8:'dd/mm/yyyy 24 hour', 9:'yyyy/mm/dd 24 hour']
            input 'distanceFormat', 'enum', required: true, defaultValue: 'Miles (mph)', title: 'Display Unit - Distance/Speed: Miles, Kilometers or knots',  options: ['Miles (mph)', 'Kilometers (kph)', 'knots', 'meters (m/s)']
            input 'pressureFormat', 'enum', required: true, defaultValue: 'Inches', title: 'Display Unit - Pressure: Inches or Millibar',  options: ['Inches', 'Millibar', 'Hectopascal']
            input 'rainFormat', 'enum', required: true, defaultValue: 'Inches', title: 'Display Unit - Precipitation: Inches or Millimeters',  options: ['Inches', 'Millimeters']
            input 'luxjitter', 'bool', title: 'Use lux jitter control (rounding)?', required: true, defaultValue: false
            input 'iconLocation', 'text', required: true, defaultValue: 'https://tinyurl.com/y6xrbhpf/', title: 'Alternative Icon Location:'
            input 'iconType', 'bool', title: 'Condition Icon: ON = Current - OFF = Forecast', required: true, defaultValue: false
            input 'dsIconbackgrounddark', 'bool', required: true, defaultValue: false, title: 'DarkSky logo text color: On = Dark  -  Off = Light'
            input 'sourcefeelsLike', 'bool', required: true, title: 'Feelslike from Weather-Display?', defaultValue: false
            input 'sourceIllumination', 'bool', required: true, title: 'Illuminance from Weather-Display?', defaultValue: true
            input 'sourceUV', 'bool', required: true, title: 'UV from Weather-Display?', defaultValue: true
            input 'sourceWind', 'bool', required: true, title: 'Wind from Weather-Display?', defaultValue: true
            input 'altCoord', 'bool', required: true, defaultValue: false, title: 'Override Hub\'s location coordinates'
            if (altCoord) {
                input 'altLat', 'string', title: 'Override location Latitude', required: true, defaultValue: location.latitude.toString(), description: '<br>Enter location Latitude<br>'
                input 'altLon', 'string', title: 'Override location Longitude', required: true, defaultValue: location.longitude.toString(), description: '<br>Enter location Longitude<br>'
            }
            input 'settingEnable', 'bool', title: '<b>Display All Optional Attributes</b>', description: settingDescr, defaultValue: true
// build a Selector for each mapped Attribute or group of attributes
	    	attributesMap.each
		    {
	    		keyname, attribute ->
                if (settingEnable) {
                    input keyname+'Publish', 'bool', title: attribute.title, required: true, defaultValue: attribute.default, description: '<br>'+attribute.descr+'<br>'                    
                    if(keyname == 'weatherSummary') input 'summaryType', 'bool', title: 'Full Weather Summary', description: '<br>Full: ON or short: OFF summary?<br>', required: true, defaultValue: false
                }
	    	}
            if (settingEnable) {
                input 'windPublish', 'bool', title: 'Wind Speed', required: true, defaultValue: 'false', description: '<br>Display wind speed<br>'
            }
        }
    }
}

// <<<<<<<<<< Begin Sunrise-Sunset Poll Routines >>>>>>>>>>
void pollSunRiseSet() {
	currDate = new Date().format('yyyy-MM-dd', TimeZone.getDefault())
	LOGINFO('Polling Sunrise-Sunset.org')
	def requestParams = [ uri: 'https://api.sunrise-sunset.org/json?lat=' + altLat + '&lng=' + altLon + '&formatted=0' ]
	if (currDate) {requestParams = [ uri: 'https://api.sunrise-sunset.org/json?lat=' + altLat + '&lng=' + altLon + '&formatted=0&date=' + currDate ]}
	LOGINFO('Poll Sunrise-Sunset: ' + requestParams)
	asynchttpGet('sunRiseSetHandler', requestParams)
	return
}

void sunRiseSetHandler(resp, data) {
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		sunRiseSet = resp.getJson().results
		updateDataValue('sunRiseSet', resp.data)
		LOGINFO('Sunrise-Sunset Data: ' + sunRiseSet)
		setDateTimeFormats(datetimeFormat)
		updateDataValue('riseTime', new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.sunrise).format('HH:mm', TimeZone.getDefault()))
		updateDataValue('noonTime', new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.solar_noon).format('HH:mm', TimeZone.getDefault()))
		updateDataValue('setTime', new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.sunset).format('HH:mm', TimeZone.getDefault()))
		updateDataValue('tw_begin', new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.civil_twilight_begin).format('HH:mm', TimeZone.getDefault()))
		updateDataValue('tw_end', new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.civil_twilight_end).format('HH:mm', TimeZone.getDefault()))
		updateDataValue('localSunset',new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault()))
		updateDataValue('localSunrise', new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault()))
	} else {
		LOGWARN('Sunrise-Sunset API did not return data')
	}
	return
}
// >>>>>>>>>> End Sunrise-Sunset Poll Routines <<<<<<<<<<

// <<<<<<<<<< Begin Weather-Display Poll Routines >>>>>>>>>>
void pollWD() {
	def ParamsWD = [ uri: pollLocationStation+'everything.php' ]
	LOGINFO('Poll Weather-Display: ' + ParamsWD)
	asynchttpGet('pollWDHandler', ParamsWD)
	return
}

void pollWDHandler(resp, data) {
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		def wd = parseJson(resp.data)
		doPollWD(wd)		// parse the data returned by Weather-Display
	} else {
		LOGWARN('Weather-Display API did not return data')
	}
	return
}

void doPollWD(Map wd) {
// <<<<<<<<<< Begin Setup Global Variables >>>>>>>>>>
	setDateTimeFormats(datetimeFormat)
	setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
	setDisplayDecimals(TWDDecimals, PDecimals, RDecimals)
	updateDataValue('currDate', new Date().format('yyyy-MM-dd', TimeZone.getDefault()))
	updateDataValue('currTime', new Date().format('HH:mm', TimeZone.getDefault()))
	if(getDataValue('riseTime') <= getDataValue('currTime') && getDataValue('setTime') >= getDataValue('currTime')) {
		updateDataValue('is_day', 'true')
	} else {
		updateDataValue('is_day', 'false')
	}
	if(getDataValue('currTime') < getDataValue('tw_begin') || getDataValue('currTime') > getDataValue('tw_end')) {
		updateDataValue('is_light', 'false')
	} else {
		updateDataValue('is_light', 'true')
	}
	if(getDataValue('is_light') != getDataValue('is_lightOld')) {
		if(getDataValue('is_light')=='true') {
			log.info('Weather-Display Driver - INFO: Switching to Daytime schedule.')
		}else{
			log.info('Weather-Display Driver - INFO: Switching to Nighttime schedule.')
		}
		initialize_poll()
		updateDataValue('is_lightOld', getDataValue('is_light'))
	}
// >>>>>>>>>> End Setup Global Variables <<<<<<<<<<  

// <<<<<<<<<< Begin Setup Station Variables >>>>>>>>>>
	sotime = new Date().parse('HH:mm dd/MM/yyyy', wd.time.time_date, TimeZone.getDefault())
	updateDataValue('sotime', sotime.toString())
	sutime = new Date()
	updateDataValue('sutime', sutime.toString())
// >>>>>>>>>> End Setup Station Variables <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If No External Forcast Is Selected  >>>>>>>>>>
    if(extSource.toInteger() == 1){
		fotime = new Date().parse('HH:mm d/M/yyyy', wd.time.time_date, TimeZone.getDefault())
		futime = new Date()
		if(!wd.everything.weather.solar.percentage){
			updateDataValue('cloud', '1')
		} else {
			if(wd.everything.weather.solar.percentage.toInteger() == 100){
				updateDataValue('cloud', '1')
			} else {
				updateDataValue('cloud',(100 - wd.everything.weather.solar.percentage.toInteger()).toString())
			}
		}
		String c_code = (getDataValue('is_day')=='true' ? '' : 'nt_')
		switch(!wd.everything.forecast.icon.code ? 99 : wd.everything.forecast.icon.code.toInteger()) {
			case 0: c_code += 'sunny'; break;
			case 1: c_code += 'clear'; break;
			case 2: c_code += 'partlycloudy'; break;
			case 3: c_code += 'clear'; break;
			case 4: c_code += 'cloudy'; break;
			case 5: c_code += 'clear'; break;
			case 6: c_code += 'fog'; break;
			case 7: c_code += 'hazy'; break;
			case 8: c_code += 'rain'; break;
			case 9: c_code += 'clear'; break;
			case 10: c_code += 'hazy'; break;
			case 11: c_code += 'fog'; break;
			case 12: c_code += 'rain'; break;
			case 13: c_code += 'mostlycloudy'; break;
			case 14: c_code += 'rain'; break;
			case 15: c_code += 'rain'; break;
			case 16: c_code += 'snow'; break;
			case 17: c_code += 'tstorms'; break;
			case 18: c_code += 'mostlycloudy'; break;
			case 19: c_code += 'mostlycloudy'; break;
			case 20: c_code += 'rain'; break;
			case 21: c_code += 'rain'; break;
			case 22: c_code += 'rain'; break;
			case 23: c_code += 'sleet'; break;
			case 24: c_code += 'sleet'; break;
			case 25: c_code += 'snow'; break;
			case 26: c_code += 'snow'; break;
			case 27: c_code += 'snow'; break;
			case 28: c_code += 'sunny'; break;
			case 29: c_code += 'tstorms'; break;
			case 30: c_code += 'tstorms'; break;
			case 31: c_code += 'tstorms'; break;
			case 32: c_code += 'tstorms'; break;
			case 33: c_code += 'breezy'; break;
			case 34: c_code += 'partlycloudy'; break;
			case 35: c_code += 'rain'; break;
			default: c_code += 'unknown'; break;
		}
		updateDataValue('condition_code', c_code)
		updateDataValue('condition_text', wd.everything.forecast.icon.text)
		updateLux(false)
// <<<<<<<<<< Begin Icon processing >>>>>>>>>>
		String imgName = getImgName(getDataValue('condition_code'))
		sendEventPublisg(name: 'condition_icon', value: '<img src=' + imgName + '>')
		sendEventPublish(name: 'condition_iconWithText', value: '<img src=' + imgName + '><br>' + getDataValue('condition_text'))
		sendEventPublish(name: 'condition_icon_url', value: imgName)
		updateDataValue('condition_icon_url', imgName)
		sendEventPublish(name: 'condition_icon_only', value: imgName.split('/')[-1].replaceFirst('\\?raw=true',''))
// >>>>>>>>>> End Icon Processing <<<<<<<<<<        
		String Summary_forecastTemp = '. '
		String Summary_vis = ''
	}
// >>>>>>>>>> End Process Only If No External Forecast Is Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Standard Weather-Station Variables (Regardless of Forecast Selection)  >>>>>>>>>>
	updateDataValue('dewpoint', (tMetric=='°F' ? wd.everything.weather.dew_point.current.f.toBigDecimal() : wd.everything.weather.dew_point.current.c.toBigDecimal()).toString())
	updateDataValue('humidity', wd.everything.weather.humidity.current.toBigDecimal().toString())
	updateDataValue('precip_today', (rMetric!='Inches' ? wd.everything.weather.rainfall.daily.in.toBigDecimal() : wd.everything.weather.rainfall.daily.mm.toBigDecimal()).toString())
	updateDataValue('pressure', (pMetric!='inHg' ? wd.everything.weather.pressure.current.mb.toBigDecimal() : wd.everything.weather.pressure.current.inhg.toBigDecimal()).toString())
	updateDataValue('temperature', (tMetric=='°F' ? wd.everything.weather.temperature.current.f.toBigDecimal() : wd.everything.weather.temperature.current.c.toBigDecimal()).toString())

// <<<<<<<<<< Begin Process Only If Wind from WD Is Selected  >>>>>>>>>>
	if(sourceWind==true){
		updateDataValue('wind_bft_icon', 'wb' + wd.everything.weather.wind.avg_speed.bft.toInteger().toString() + '.png')
		String w_string_bft
		switch(wd.everything.weather.wind.avg_speed.bft.toInteger()){
			case 0: w_string_bft = 'Calm'; break;
			case 1: w_string_bft = 'Light air'; break;
			case 2: w_string_bft = 'Light breeze'; break;
			case 3: w_string_bft = 'Gentle breeze'; break;
			case 4: w_string_bft = 'Moderate breeze'; break;
			case 5: w_string_bft = 'Fresh breeze'; break;
			case 6: w_string_bft = 'Strong breeze'; break;
			case 7: w_string_bft = 'High wind, moderate gale, near gale'; break;
			case 8: w_string_bft = 'Gale, fresh gale'; break;
			case 9: w_string_bft = 'Strong/severe gale'; break;
			case 10: w_string_bft = 'Storm, whole gale'; break;
			case 11: w_string_bft = 'Violent storm'; break;
			case 12: w_string_bft = 'Hurricane force'; break;
			default: w_string_bft = 'Calm'; break;
		}
		BigDecimal t_wd
		BigDecimal t_wg
		if(dMetric == 'MPH') {
			t_wd = Math.round(wd.everything.weather.wind.avg_speed.mph.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
			t_wg = Math.round(wd.everything.weather.wind.gust_speed.mph.toBigDecimal() *  getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		} else if(dMetric == 'KPH') {
			t_wd = Math.round(wd.everything.weather.wind.avg_speed.kmh.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
			t_wg = Math.round(wd.everything.weather.wind.gust_speed.kmh.toBigDecimal() * 1.609344 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		} else if(dMetric == 'knots') {
			t_wd = Math.round(wd.everything.weather.wind.avg_speed.mph.toBigDecimal() * 0.868976 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
			t_wg = Math.round(wd.everything.weather.wind.gust_speed.mph.toBigDecimal() * 0.868976 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		} else {  //  this leave only m/s
			t_wd = Math.round(wd.everything.weather.wind.avg_speed.mph.toBigDecimal() *  0.44704 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
			t_wg = Math.round(wd.everything.weather.wind.gust_speed.mph.toBigDecimal()  * 0.44704 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		}
		updateDataValue('wind', t_wd.toString())
		updateDataValue('wind_gust', t_wg.toString())

		updateDataValue('wind_degree', wd.everything.weather.wind.direction.degrees.toInteger().toString())
		String w_direction
		switch(wd.everything.weather.wind.direction.cardinal.toUpperCase()){
			case 'N': w_direction = 'North'; break;
			case 'NNE': w_direction = 'North-Northeast'; break;
			case 'NE': w_direction = 'Northeast'; break;
			case 'ENE': w_direction = 'East-Northeast'; break;
			case 'E': w_direction = 'East'; break;
			case 'ESE': w_direction = 'East-Southeast'; break;
			case 'SE': w_direction = 'Southeast'; break;
			case 'SSE': w_direction = 'South-Southeast'; break;
			case 'S': w_direction = 'South'; break;
			case 'SSW': w_direction = 'South-Southwest'; break;
			case 'SW': w_direction = 'Southwest'; break;
			case 'WSW': w_direction = 'West-Southwest'; break;
			case 'W': w_direction = 'West'; break;
			case 'WNW': w_direction = 'West-Northwest'; break;
			case 'NW': w_direction = 'Northwest'; break;
			case 'NNW': w_direction = 'North-Northwest'; break;
			default: w_direction = 'Unknown'; break;
		}
		updateDataValue('wind_direction', w_direction)
		updateDataValue('wind_cardinal', wd.everything.weather.wind.direction.cardinal.toUpperCase())
		updateDataValue('wind_string', w_string_bft + ' from the ' + getDataValue('wind_direction') + (getDataValue('wind').toBigDecimal() < 1.0 ? '': ' at ' + String.format(ddisp_twd, getDataValue('wind').toBigDecimal()) + ' ' + dMetric))
	}
// >>>>>>>>>> End Process Only If Wind from WD Is Selected <<<<<<<<<<

	updateDataValue('city', wd.station.name.split(/ /)[0])
	updateDataValue('state', wd.station.name.split(/ /)[1])
	updateDataValue('country', wd.station.name.split(/ /)[2])

	updateDataValue('moonAge', wd.everything.astronomy.moon.moon_age.toBigDecimal().toString())
	String mPhase
	BigDecimal tma = wd.everything.astronomy.moon.moon_age.toBigDecimal()
	if (tma >= 0 && tma < 4) {mPhase = 'New Moon'}
	else if (tma >= 4 && tma < 7) {mPhase = 'Waxing Crescent'}
	else if (tma >= 7 && tma < 10) {mPhase = 'First Quarter'}
	else if (tma >= 10 && tma < 14) {mPhase = 'Waxing Gibbous'}
	else if (tma >= 14 && tma < 18) {mPhase = 'Full Moon'}
	else if (tma >= 18 && tma < 22) {mPhase = 'Waning Gibbous'}
	else if (tma >= 22 && tma < 26) {mPhase = 'Last Quarter'}
	else if (tma >= 26) {mPhase = 'Waxing Gibbous'}
	updateDataValue('moonPhase', mPhase)
	if(solarradiationPublish){
		if(!wd.everything.weather.solar.irradiance.wm2){
			updateDataValue('solarradiation', 'This station does not send Solar Radiation data.')
		} else {
			updateDataValue('solarradiation', wd.everything.weather.solar.irradiance.wm2.toInteger().toString())
		}
	}
// >>>>>>>>>> End Process Standard Weather-Station Variables (Regardless of Forecast Selection)  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If Illumination from WD Is Selected  >>>>>>>>>>
	if(sourceIllumination == true){
		if (!wd.everything.weather.solar.irradiance.wm2){
			updateDataValue('illuminance', 'This station does not send illuminance data.')
			updateDataValue('illuminated', 'This station does not send illuminance data.')
		} else {
			BigDecimal slux = Math.max(((wd.everything.weather.solar.irradiance.wm2.toBigDecimal() / 0.0079) / 12.8),5.000) //SolarRad to Lux conversion
			updateDataValue('illuminance', slux.toInteger().toString())
			updateDataValue('illuminated', String.format('%,4d', slux.toInteger()).toString())
		}
	}
// >>>>>>>>>> End Process Only If Illumination from WD Is Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If Ultraviolet Index from WD Is Selected  >>>>>>>>>>
	if(sourceUV==true){
		if(!wd.everything.weather.uv.uvi){
			updateDataValue('ultravioletIndex', 'This station does not send ultravoilet index data.')
		} else {
			updateDataValue('ultravioletIndex', wd.everything.weather.uv.uvi.toBigDecimal().toString())
		}
	}
// >>>>>>>>>> End Process Only If UV from WD Is Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If feelsLike from WD Is Selected  >>>>>>>>>>
	if(sourcefeelsLike==true){
		BigDecimal t_fl
		if(tMetric == '°F') {
			t_fl = Math.round(wd.everything.weather.apparent_temperature.current.f.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		} else {
			t_fl = Math.round(wd.everything.weather.apparent_temperature.current.c.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		}
		updateDataValue('feelsLike', t_fl.toString())
	}    
// >>>>>>>>>> End Process Only If feelsLike from WD Is Selected  <<<<<<<<<<

	if(getDataValue('forecastPoll') == 'false'){
		if(extSource.toInteger() == 2){ pollDS() }
	}else{
		PostPoll()
	}
}
// >>>>>>>>>> End Weather-Display routines <<<<<<<<<<

// <<<<<<<<<< Begin DarkSky Poll Routines >>>>>>>>>>
void pollDS() {
	if( apiKey == null ) {
		LOGWARN('DarkSky API Key not found.  Please configure in preferences.')
		return
	}
	def ParamsDS = [ uri: 'https://api.darksky.net/forecast/' + apiKey + '/' + altLat + ',' + altLon + '?units=us&exclude=minutely,hourly,flags' ]
	LOGINFO('Poll DarkSky: ' + ParamsDS)
	asynchttpGet('pollDSHandler', ParamsDS)
	return
}

void pollDSHandler(resp, data) {
	LOGINFO('Polling DarkSky.net')
	if(resp.getStatus() == 200 || resp.getStatus() == 207) {
		def ds = parseJson(resp.data)
		doPollDS(ds)		// parse the data returned by DarkSky
	} else {
		LOGWARN('Calling https://api.darksky.net/forecast/' + apiKey + '/' + altLat + ',' + altLon + '?units=us&exclude=minutely,hourly,flags')
		LOGWARN('DarkSky.net API did not return data')
	}
}

void doPollDS(Map ds) {
// <<<<<<<<<< Begin Setup Global Variables >>>>>>>>>>
	setDateTimeFormats(datetimeFormat)
	setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
	setDisplayDecimals(TWDDecimals, PDecimals, RDecimals)
	updateDataValue('currDate', new Date().format('yyyy-MM-dd', TimeZone.getDefault()))
	updateDataValue('currTime', new Date().format('HH:mm', TimeZone.getDefault()))

	if(getDataValue('riseTime') <= getDataValue('currTime') && getDataValue('setTime') >= getDataValue('currTime')) {
		updateDataValue('is_day', 'true')
	} else {
		updateDataValue('is_day', 'false')
	}
// >>>>>>>>>> End Setup Global Variables <<<<<<<<<<

// <<<<<<<<<< Begin Setup Forecast Variables >>>>>>>>>>
	fotime = new Date(ds.currently.time * 1000L)
	updateDataValue('fotime', fotime.toString())
	futime = new Date()
	updateDataValue('futime', futime.toString())
	Integer cloudCover = 1
	if (!ds.currently.cloudCover) {
		cloudCover = 1
	} else {
		cloudCover = (ds.currently.cloudCover.toBigDecimal() <= 0.01) ? 1 : ds.currently.cloudCover.toBigDecimal() * 100
	}
	updateDataValue('cloud', cloudCover.toString())
	updateDataValue('vis', (dMetric!='MPH' ? ds.currently.visibility.toBigDecimal() * 1.60934 : ds.currently.visibility.toBigDecimal()).toString())
	updateDataValue('percentPrecip', (ds.daily.data[0].precipProbability.toBigDecimal() * 100).toInteger().toString())

	String c_code = getdsIconCode(ds.currently.icon, ds.currently.summary, getDataValue('is_day'))
	updateDataValue('condition_code', c_code)
	updateDataValue('condition_text', getcondText(c_code))

	String f_code = getdsIconCode(ds.daily.data[0].icon, ds.daily.data[0].summary, getDataValue('is_day'))
	updateDataValue('forecast_code', f_code)
	updateDataValue('forecast_text', getcondText(f_code))

	if (!ds.alerts){
		updateDataValue('alert', 'No current weather alerts for this area')
		updateDataValue('alertTileLink', '<a href="https://darksky.net/forecast/' + altLat + ',' + altLon + '" target="_blank">No current weather alerts for this area.</a>')
		updateDataValue('alertLink', '<a>' + getDataValue('condition_text') + '</a>')
		updateDataValue('alertLink2', '<a>' + getDataValue('condition_text') + '</a>')
		updateDataValue('alertLink3', '<a>' + getDataValue('condition_text') + '</a>')
		updateDataValue('possAlert', 'false')
	} else {
		updateDataValue('alertTileLink', '<a style="font-style:italic;color:red;" href="' + ds.alerts[0].uri + '" target="_blank">' + ds.alerts.title.toString().replaceAll('[{}\\[\\]]', '').split(/,/)[0]+'</a>')
		updateDataValue('alertLink', '<a style="font-style:italic;color:red;" href="' + ds.alerts[0].uri + '" target="_blank">' + ds.alerts.title.toString().replaceAll('[{}\\[\\]]', '').split(/,/)[0]+'</a>')
		def String al2 = '<a style="font-style:italic;color:red;" href="https://darksky.net/forecast/' + altLat + ',' + altLon + '" target="_blank">'
		updateDataValue('alertLink2', al2+ds.alerts.title.toString().replaceAll('[{}\\[\\]]', '').split(/,/)[0]+'</a>')
		updateDataValue('alertLink3', '<a style="font-style:italic;color:red;" target="_blank">'+ds.alerts.title.toString().replaceAll('[{}\\[\\]]', '').split(/,/)[0]+'</a>')
		updateDataValue('alert', ds.alerts.title.toString().replaceAll('[{}\\[\\]]', '').split(/,/)[0])
		updateDataValue('possAlert', 'true')
/* code to test weather alerts
		updateDataValue('alertTileLink', '<a style="font-style:italic;color:red;" href="'+'https://alerts.weather.gov/cap/wwacapget.php?x=NJ125F3B5DE240.WindAdvisory.125F3B5E5130NJ.PHINPWPHI.4c81e473f52888dec2cb0723d0145f0b'+'">'+'Wind Advisory'+'</a>')
		updateDataValue('alertLink', '<a style="font-style:italic;color:red;" href="'+'https://alerts.weather.gov/cap/wwacapget.php?x=NJ125F3B5DE240.WindAdvisory.125F3B5E5130NJ.PHINPWPHI.4c81e473f52888dec2cb0723d0145f0b'+'">'+'Wind Advisory'+'</a>')
		updateDataValue('alertLink2', '<a style="font-style:italic;color:red;" href="https://darksky.net/forecast/' + altLat + ',' + altLon + '" target="_blank">' + '12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890>'+'Wind Advisory'+'</a>')
		updateDataValue('alertLink3', '<a style="font-style:italic;color:red;">'+'Wind Advisory'+'</a>')
		updateDataValue('alert', 'Wind Advisory')
		updateDataValue('possAlert', 'true')
*/    
	}

	if(threedayTilePublish) {
		updateDataValue('day1', new Date(ds.daily.data[1].time * 1000L).format('EEEE'))
		updateDataValue('day2', new Date(ds.daily.data[2].time * 1000L).format('EEEE'))
		updateDataValue('is_day1', 'true')
		updateDataValue('is_day2', 'true')
		String f_code1 = getdsIconCode(ds.daily.data[1].icon, ds.daily.data[1].summary, getDataValue('is_day1'))
		updateDataValue('forecast_code1', f_code1)
		updateDataValue('forecast_text1', getcondText(f_code1))

		String f_code2 = getdsIconCode(ds.daily.data[2].icon, ds.daily.data[2].summary, getDataValue('is_day2'))
		updateDataValue('forecast_code2', f_code2)
		updateDataValue('forecast_text2', getcondText(f_code2))

		updateDataValue('forecastHigh1', (tMetric=='°F' ? (Math.round(ds.daily.data[1].temperatureMax.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()) : (Math.round((ds.daily.data[1].temperatureMax.toBigDecimal() - 32) / 1.8 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger())).toString())
		updateDataValue('forecastHigh2', (tMetric=='°F' ? (Math.round(ds.daily.data[2].temperatureMax.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()) : (Math.round((ds.daily.data[2].temperatureMax.toBigDecimal() - 32) / 1.8 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger())).toString())    

		updateDataValue('forecastLow1', (tMetric=='°F' ? (Math.round(ds.daily.data[1].temperatureMin.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()) : (Math.round((ds.daily.data[1].temperatureMin.toBigDecimal() - 32) / 1.8 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger())).toString())
		updateDataValue('forecastLow2', (tMetric=='°F' ? (Math.round(ds.daily.data[2].temperatureMin.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()) : (Math.round((ds.daily.data[2].temperatureMin.toBigDecimal() - 32) / 1.8 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger())).toString())

		updateDataValue('imgName0', '<img class=\'centerImage\' src=' + getImgName(getDataValue('forecast_code')) + '>')
		updateDataValue('imgName1', '<img class=\'centerImage\' src=' + getImgName(getDataValue('forecast_code1')) + '>')
		updateDataValue('imgName2', '<img class=\'centerImage\' src=' + getImgName(getDataValue('forecast_code2')) + '>')

		updateDataValue('PoP', (!ds.daily.data[0].precipProbability ? 0 : (ds.daily.data[0].precipProbability.toBigDecimal() * 100).toInteger()).toString())
		updateDataValue('PoP1', (!ds.daily.data[1].precipProbability ? 0 : (ds.daily.data[1].precipProbability.toBigDecimal() * 100).toInteger()).toString())
		updateDataValue('PoP2', (!ds.daily.data[2].precipProbability ? 0 : (ds.daily.data[2].precipProbability.toBigDecimal() * 100).toInteger()).toString())
    }

	updateDataValue('forecastHigh', ((tMetric == '°F') ? (Math.round(ds.daily.data[0].temperatureMax.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()) : (Math.round((ds.daily.data[0].temperatureMax.toBigDecimal() - 32) / 1.8 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger())).toString())
	updateDataValue('forecastLow', ((tMetric == '°F') ? (Math.round(ds.daily.data[0].temperatureMin.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()) : (Math.round((ds.daily.data[0].temperatureMin.toBigDecimal() - 32) / 1.8 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger())).toString())
	if(precipExtendedPublish){
		updateDataValue('rainTomorrow', (ds.daily.data[1].precipProbability.toBigDecimal() * 100).toInteger().toString())
		updateDataValue('rainDayAfterTomorrow', (ds.daily.data[2].precipProbability.toBigDecimal() * 100).toInteger().toString())
	}

// <<<<<<<<<< Begin Process Only If Wind from WD Is NOT Selected  >>>>>>>>>>
	String w_string_bft
	String w_bft_icon
	BigDecimal t_ws = ds.currently.windSpeed.toBigDecimal()
	if(t_ws < 1.0) {
		w_string_bft = 'Calm'; w_bft_icon = 'wb0.png'
	}else if(t_ws < 4.0) {
		w_string_bft = 'Light air'; w_bft_icon = 'wb1.png'
	}else if(t_ws < 8.0) {
		w_string_bft = 'Light breeze'; w_bft_icon = 'wb2.png'
	}else if(t_ws < 13.0) {
		w_string_bft = 'Gentle breeze'; w_bft_icon = 'wb3.png'
	}else if(t_ws < 19.0) {
		w_string_bft = 'Moderate breeze'; w_bft_icon = 'wb4.png'
	}else if(t_ws < 25.0) {
		w_string_bft = 'Fresh breeze'; w_bft_icon = 'wb5.png'
	}else if(t_ws < 32.0) {
		w_string_bft = 'Strong breeze'; w_bft_icon = 'wb6.png'
	}else if(t_ws < 39.0) {
		w_string_bft = 'High wind, moderate gale, near gale'; w_bft_icon = 'wb7.png'
	}else if(t_ws < 47.0) {
		w_string_bft = 'Gale, fresh gale'; w_bft_icon = 'wb8.png'
	}else if(t_ws < 55.0) {
		w_string_bft = 'Strong/severe gale'; w_bft_icon = 'wb9.png'
	}else if(t_ws < 64.0) {
		w_string_bft = 'Storm, whole gale'; w_bft_icon = 'wb10.png'
	}else if(t_ws < 73.0) {
		w_string_bft = 'Violent storm'; w_bft_icon = 'wb11.png'
	}else if(t_ws >= 73.0) {
		w_string_bft = 'Hurricane force'; w_bft_icon = 'wb12.png'
	}
	updateDataValue('wind_string_bft', w_string_bft)
	updateDataValue('wind_bft_icon', w_bft_icon)

	BigDecimal t_wd
	BigDecimal t_wg
	if(dMetric == 'MPH') {
		t_wd = Math.round(ds.currently.windSpeed.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		t_wg = Math.round(ds.currently.windGust.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
	} else if(dMetric == 'KPH') {
		t_wd = Math.round(ds.currently.windSpeed.toBigDecimal() * 1.609344 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		t_wg = Math.round(ds.currently.windGust.toBigDecimal() * 1.609344 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
	} else if(dMetric == 'knots') {
		t_wd = Math.round(ds.currently.windSpeed.toBigDecimal() * 0.868976 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		t_wg = Math.round(ds.currently.windGust.toBigDecimal() * 0.868976 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
	} else {  //  this leave only m/s
		t_wd = Math.round(ds.currently.windSpeed.toBigDecimal() * 0.44704 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		t_wg = Math.round(ds.currently.windGust.toBigDecimal() * 0.44704 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
	}
	updateDataValue('wind', t_wd.toString())
	updateDataValue('wind_gust', t_wg.toString())

	updateDataValue('wind_degree', ds.currently.windBearing.toInteger().toString())
	String w_cardinal
	String w_direction
	BigDecimal twb = ds.currently.windBearing.toBigDecimal()
	if(twb < 11.25) {
		w_cardinal = 'N'; w_direction = 'North'
	}else if(twb < 33.75) {
		w_cardinal = 'NNE'; w_direction = 'North-Northeast'
	}else if(twb < 56.25) {
		w_cardinal = 'NE';  w_direction = 'Northeast'
	}else if(twb < 56.25) {
		w_cardinal = 'ENE'; w_direction = 'East-Northeast'
	}else if(twb < 101.25) {
		w_cardinal = 'E'; w_direction = 'East'
	}else if(twb < 123.75) {
		w_cardinal = 'ESE'; w_direction = 'East-Southeast'
	}else if(twb < 146.25) {
		w_cardinal = 'SE'; w_direction = 'Southeast'
	}else if(twb < 168.75) {
		w_cardinal = 'SSE'; w_direction = 'South-Southeast'
	}else if(twb < 191.25) {
		w_cardinal = 'S'; w_direction = 'South'
	}else if(twb < 213.75) {
		w_cardinal = 'SSW'; w_direction = 'South-Southwest'
		}else if(twb < 236.25) {
		w_cardinal = 'SW'; w_direction = 'Southwest'
	}else if(twb < 258.75) {
		w_cardinal = 'WSW'; w_direction = 'West-Southwest'
	}else if(twb < 281.25) {
		w_cardinal = 'W'; w_direction = 'West'
	}else if(twb < 303.75) {
		w_cardinal = 'WNW'; w_direction = 'West-Northwest'
	}else if(twb < 326.25) {
		w_cardinal = 'NW'; w_direction = 'Northwest'
	}else if(twb < 348.75) {
		w_cardinal = 'NNW'; w_direction = 'North-Northwest'
	}else if(twb >= 348.75) {
		w_cardinal = 'N'; w_direction = 'North'
	}
	updateDataValue('wind_direction', w_direction)
	updateDataValue('wind_cardinal', w_cardinal)	
	updateDataValue('wind_string', w_string_bft + ' from the ' + getDataValue('wind_direction') + (getDataValue('wind').toBigDecimal() < 1.0 ? '': ' at ' + getDataValue('wind') + ' ' + dMetric))

// >>>>>>>>>> End Process Only If Wind from WD Is NOT Selected <<<<<<<<<<

	String s_cardinal
	String s_direction
    if(nearestStormPublish) { // don't bother setting these values if it's not enabled
    	if(!ds.currently.nearestStormBearing){
    		updateDataValue('nearestStormBearing', '360')
    		s_cardinal = 'U'
    		s_direction = 'Unknown'        
    	}else{
    		updateDataValue('nearestStormBearing', (Math.round(ds.currently.nearestStormBearing.toBigDecimal() * 100) / 100).toString())
    		BigDecimal tnsb = ds.currently.nearestStormBearing.toBigDecimal()
    		if(tnsb < 11.25) {
    			s_cardinal = 'N'; s_direction = 'North'
    		}else if(tnsb < 33.75) {
    			s_cardinal = 'NNE'; s_direction = 'North-Northeast'
    		}else if(tnsb < 56.25) {
    			s_cardinal = 'NE';  s_direction = 'Northeast'
    		}else if(tnsb < 78.75) {
    			s_cardinal = 'ENE'; s_direction = 'East-Northeast'
    		}else if(tnsb < 101.25) {
    			s_cardinal = 'E'; s_direction = 'East'
    		}else if(tnsb < 123.75) {
    			s_cardinal = 'ESE'; s_direction = 'East-Southeast'
    		}else if(tnsb < 146.25) {
    			s_cardinal = 'SE'; s_direction = 'Southeast'
    		}else if(tnsb < 168.75) {
    			s_cardinal = 'SSE'; s_direction = 'South-Southeast'
    		}else if(tnsb < 191.25) {
    			s_cardinal = 'S'; s_direction = 'South'
    		}else if(tnsb < 213.75) {
    			s_cardinal = 'SSW'; s_direction = 'South-Southwest'
    		}else if(tnsb < 236.25) {
    			s_cardinal = 'SW'; s_direction = 'Southwest'
    		}else if(tnsb < 258.75) {
    			s_cardinal = 'WSW'; s_direction = 'West-Southwest'
    		}else if(tnsb < 281.25) {
    			s_cardinal = 'W'; s_direction = 'West'
    		}else if(tnsb < 303.75) {
    			s_cardinal = 'WNW'; s_direction = 'West-Northwest'
    		}else if(tnsb < 326.26) {
    			s_cardinal = 'NW'; s_direction = 'Northwest'
    		}else if(tnsb < 348.75) {
    			s_cardinal = 'NNW'; s_direction = 'North-Northwest'
    		}else if(tnsb >= 348.75) {
    			s_cardinal = 'N'; s_direction = 'North'
    		}
	    	updateDataValue('nearestStormCardinal', s_cardinal)
    		updateDataValue('nearestStormDirection', s_direction)
        }
		BigDecimal t_nsd
		if(!ds.currently.nearestStormDistance) {
			t_nsd = 9999.9
		} else if(dMetric == 'MPH') {
			t_nsd = Math.round(ds.currently.nearestStormDistance.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		} else {
			t_nsd = Math.round(ds.currently.nearestStormDistance.toBigDecimal() * 1.609344 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		}
		updateDataValue('nearestStormDistance', t_nsd.toInteger().toString())
	}
    
    if(ozonePublish) { // don't bother setting these values if it's not enabled
        if(!ds.currently.ozone) {
            updateDataValue('ozone','0')
        }else{
	        updateDataValue('ozone', (Math.round(ds.currently.ozone.toBigDecimal() * 10 ) / 10).toString())
        }
    }
// >>>>>>>>>> End Setup Forecast Variables <<<<<<<<<<
    updateLux(false)
// >>>>>>>>>> End Process Only If Illumination from WD Is NOT Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If Ultraviolet Index from WD Is NOT Selected  >>>>>>>>>>                    
	if(sourceUV==false){    
		updateDataValue('ultravioletIndex', ds.currently.uvIndex.toBigDecimal().toString())
	}
// >>>>>>>>>> End Process Only If Ultraviolet Index from WD Is NOT Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Process Only If feelsLike Index from WD Is NOT Selected  >>>>>>>>>>
	if(sourcefeelsLike==false){
		BigDecimal t_fl
		if(tMetric == '°F') {
			t_fl = Math.round(ds.currently.apparentTemperature.toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		} else {
			t_fl = Math.round((ds.currently.apparentTemperature.toBigDecimal() - 32) / 1.8 * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger()
		}
		updateDataValue('feelsLike', t_fl.toString())
	}    
// >>>>>>>>>> End Process Only If feelsLike from WD Is NOT Selected  <<<<<<<<<<

// <<<<<<<<<< Begin Icon Processing  >>>>>>>>>>
	String dimgName = (getDataValue('iconType')== 'true' ? getImgName(getDataValue('condition_code')) : getImgName(getDataValue('forecast_code')))
	sendEventPublish(name: 'condition_icon', value: '<img src=' + dimgName + '>')
	sendEventPublish(name: 'condition_iconWithText', value: '<img src=' + dimgName + '><br>' + (getDataValue('iconType')== 'true' ? getDataValue('condition_text') : getDataValue('forecast_text')))
	sendEventPublish(name: 'condition_icon_url', value: dimgName)
	updateDataValue('condition_icon_url', dimgName)
	sendEventPublish(name: 'condition_icon_only', value: dimgName.split('/')[-1].replaceFirst('\\?raw=true',''))
// >>>>>>>>>> End Icon Processing <<<<<<<<<<
	if(getDataValue('forecastPoll') == 'false'){
		updateDataValue('forecastPoll', 'true')
	}
	PostPoll()
}
// >>>>>>>>>> End DarkSky Poll Routines <<<<<<<<<<

// >>>>>>>>>> Begin Lux Processing <<<<<<<<<<
void updateLux(Boolean pollAgain=true) {
	LOGINFO('Calling updateLux(' + pollAgain + ')')
	if(pollAgain) {
		String curTime = new Date().format('HH:mm', TimeZone.getDefault())
		String newLight
		if(curTime < getDataValue('tw_begin') || curTime > getDataValue('tw_end')) {
			newLight =  'false'
		} else {
			newLight =  'true'
		}
		if(newLight != getDataValue('is_lightOld')) {
			pollDS()
			return
		}
	}
	if(extSource.toInteger()==1 || sourceIllumination == true){
		def (holdlux, bwn) = estimateLux(getDataValue('condition_code'), getDataValue('cloud').toInteger())
        LOGINFO('updateLux Results: holdlux: ' + holdlux + '; bwn: ' + bwn)
	} else {
		def (lux, bwn) = estimateLux(getDataValue('condition_code'), getDataValue('cloud').toInteger())
		updateDataValue('illuminance', !lux ? '0' : lux.toString())
		updateDataValue('illuminated', String.format('%,4d', !lux ? 0 : lux).toString())
        LOGINFO('updateLux Results: lux: ' + holdlux + '; bwn: ' + bwn)
	}
	if(pollAgain) PostPoll()
    return    
}
// >>>>>>>>>> End Lux Processing <<<<<<<<<<
// <<<<<<<<<< Begin Icon and condition_code, condition_text processing >>>>>>>>>>
String getdsIconCode(String icon='unknown', String dcs='unknown', String isDay='true') {
	switch(icon) {
		case 'rain':
		// rain=[Possible Light Rain, Light Rain, Rain, Heavy Rain, Drizzle, Light Rain and Breezy, Light Rain and Windy, 
		//       Rain and Breezy, Rain and Windy, Heavy Rain and Breezy, Rain and Dangerously Windy, Light Rain and Dangerously Windy],
			if (dcs == 'Drizzle') {
				icon = 'drizzle'
			} else if 	(dcs.startsWith('Light Rain')) { 
				icon = 'lightrain'
				if 		(dcs.contains('Breezy')) icon += 'breezy'
				else if (dcs.contains('Windy'))  icon += 'windy'
			} else if 	(dcs.startsWith('Heavy Rain')) {
				icon = 'heavyrain'
				if 		(dcs.contains('Breezy')) icon += 'breezy'
				else if (dcs.contains('Windy'))  icon += 'windy'
			} else if 	(dcs == 'Possible Light Rain') {
				icon = 'chancelightrain'
			} else if 	(dcs.startsWith('Possible')) {
				icon = 'chancerain'
			} else if 	(dcs.startsWith('Rain')) {
				if 		(dcs.contains('Breezy')) icon += 'breezy'
				else if (dcs.contains('Windy'))  icon += 'windy'
			}
			break;
		case 'snow':
			if      (dcs == 'Light Snow') icon = 'lightsnow'
			else if (dcs == 'Flurries') icon = 'flurries'
			else if (dcs == 'Possible Light Snow') icon = 'chancelightsnow'
			else if (dcs.startsWith('Possible Light Snow')) {
				if (dcs.contains('Breezy')) icon = 'chancelightsnowbreezy'
				else if (dcs.contains('Windy')) icon = 'chancelightsnowwindy'
			} else if (dcs.startsWith('Possible')) icon = 'chancesnow'
			break;
		case 'sleet':
			if (dcs.startsWith('Possible')) icon = 'chancesleet'
			else if (dcs.startsWith('Light')) icon = 'lightsleet'
			break;
		case 'thunderstorm':
			if (dcs.startsWith('Possible')) icon = 'chancetstorms'
			break;
		case 'partly-cloudy-night':
			if (dcs.contains('Mostly Cloudy')) icon = 'mostlycloudy'
			else icon = 'partlycloudy'
			break;
		case 'partly-cloudy-day':
			if (dcs.contains('Mostly Cloudy')) icon = 'mostlycloudy'
			else icon = 'partlycloudy'
			break;
		case 'cloudy-night':
			icon = 'cloudy'
			break;
		case 'cloudy':
		case 'cloudy-day':
			icon = 'cloudy'
			break;
		case 'clear-night':
			icon = 'clear'
			break;
		case 'clear':
		case 'clear-day':
			icon = 'clear'
			break;
		case 'fog':
		case 'wind':
			// wind=[Windy and Overcast, Windy and Mostly Cloudy, Windy and Partly Cloudy, Breezy and Mostly Cloudy, Breezy and Partly Cloudy, 
			// Breezy and Overcast, Breezy, Windy, Dangerously Windy and Overcast, Windy and Foggy, Dangerously Windy and Partly Cloudy, Breezy and Foggy]}
			if (dcs.contains('Windy')) {
				if (dcs.contains('Overcast')) icon = 'windovercast'
				else if (dcs.contains('Mostly Cloudy')) icon = 'windmostlycloudy'
				else if (dcs.contains('Partly Cloudy')) icon = 'windpartlycloudy'
				else if (dcs.contains('Foggy')) icon = 'windfoggy'
			} else if (dcs.contains('Breezy')) {
				icon = 'breezy'
				if 		(dcs.contains('Overcast')) icon = 'breezyovercast'
				else if (dcs.summary.contains('Mostly Cloudy')) icon = 'breezymostlycloudy'
				else if (dcs.contains('Partly Cloudy')) icon = 'breezypartlycloudy'
				else if (dcs.contains('Foggy')) icon = 'breezyfoggy'
			}
			break;
		case '':
			icon = 'unknown'
			break;
		default:
			icon = 'unknown'
	}
	if(isDay == 'false') icon = 'nt_' + icon
	return icon
}
// >>>>>>>>>> End Icon and condition_code, condition_text processing <<<<<<<<<<

// <<<<<<<<<< Begin Post-Poll Routines >>>>>>>>>>
void PostPoll() {
	def sunRiseSet = parseJson(getDataValue('sunRiseSet')).results
	setDateTimeFormats(datetimeFormat)
	setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
	setDisplayDecimals(TWDDecimals, PDecimals, RDecimals)
/*  SunriseSunset Data Eements */
	if(localSunrisePublish){  // don't bother setting these values if it's not enabled
		sendEvent(name: 'tw_begin', value: new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.civil_twilight_begin).format(timeFormat, TimeZone.getDefault()))
		sendEvent(name: 'sunriseTime', value: new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault()))
		sendEvent(name: 'noonTime', value: new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.solar_noon).format(timeFormat, TimeZone.getDefault()))
		sendEvent(name: 'sunsetTime', value: new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault()))
		sendEvent(name: 'tw_end', value: new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.civil_twilight_end).format(timeFormat, TimeZone.getDefault()))
	}
	if(dashSharpToolsPublish || dashSmartTilesPublish || localSunrisePublish) {
		sendEvent(name: 'localSunset', value: new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.sunset).format(timeFormat, TimeZone.getDefault())) // only needed for certain dashboards
		sendEvent(name: 'localSunrise', value: new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', sunRiseSet.sunrise).format(timeFormat, TimeZone.getDefault())) // only needed for certain dashboards
	}
/*  Weather-Display Data Elements */
	sendEvent(name: 'humidity', value: getDataValue('humidity').toBigDecimal(), unit: '%')
	sendEvent(name: 'illuminance', value: getDataValue('illuminance').toInteger(), unit: 'lx')
	sendEvent(name: 'pressure', value: getDataValue('pressure').toBigDecimal(), unit: pMetric)
	sendEvent(name: 'pressured', value: String.format(ddisp_p, getDataValue('pressure').toBigDecimal()), unit: pMetric)
	sendEvent(name: 'temperature', value: getDataValue('temperature').toBigDecimal(), unit: tMetric)
	sendEvent(name: 'ultravioletIndex', value: getDataValue('ultravioletIndex').toBigDecimal(), unit: 'uvi')
	sendEvent(name: 'feelsLike', value: getDataValue('feelsLike').toBigDecimal(), unit: tMetric)

/*  'Required for Dashboards' Data Elements */
	if(dashHubitatOWMPublish || dashSharpToolsPublish || dashSmartTilesPublish) { sendEvent(name: 'city', value: getDataValue('city')) }
	if(dashSharpToolsPublish) { sendEvent(name: 'forecastIcon', value: getstdImgName(getDataValue('condition_code'))) }
	if(dashSharpToolsPublish || dashSmartTilesPublish || percentPrecipPublish) { sendEvent(name: 'percentPrecip', value: getDataValue('percentPrecip')) }
	if(dashSharpToolsPublish || dashSmartTilesPublish) { sendEvent(name: 'weather', value: getDataValue('condition_text')) }
	if(dashSharpToolsPublish || dashSmartTilesPublish) { sendEvent(name: 'weatherIcon', value: getstdImgName(getDataValue('condition_code'))) }
	if(dashHubitatOWMPublish) { sendEvent(name: 'weatherIcons', value: getowmImgName(getDataValue('condition_code'))) }
	if(dashHubitatOWMPublish || dashSharpToolsPublish || windPublish) { sendEvent(name: 'wind', value: getDataValue('wind').toBigDecimal(), unit: dMetric) }
	if(dashHubitatOWMPublish) { sendEvent(name: 'windSpeed', value: getDataValue('wind').toBigDecimal(), unit: dMetric) }
	if(dashHubitatOWMPublish) { sendEvent(name: 'windDirection', value: getDataValue('wind_degree').toInteger(), unit: 'DEGREE') }

/*  Selected optional Data Elements */
	sendEventPublish(name: 'alert', value: getDataValue('alert'))
	sendEventPublish(name: 'betwixt', value: getDataValue('bwn'))
	sendEventPublish(name: 'cloud', value: getDataValue('cloud').toInteger(), unit: '%')
	sendEventPublish(name: 'condition_code', value: getDataValue('condition_code'))
	sendEventPublish(name: 'condition_text', value: getDataValue('condition_text'))
	sendEventPublish(name: 'country', value: getDataValue('country'))
	sendEventPublish(name: 'dewpoint', value: getDataValue('dewpoint').toBigDecimal(), unit: tMetric)
	if(dsAttributionPublish){
		sendEvent(name: 'dsIconlighttext', value: '<a href="https://darksky.net/poweredby/\' target="_blank"><img src=' + getDataValue('iconLocation') + 'dsL.png' + ' style="height:2em";></a>')
		sendEvent(name: 'dsIcondarktext', value: '<a href="https://darksky.net/poweredby/\' target="_blank"><img src=' + getDataValue('iconLocation') + 'dsD.png' + ' style="height:2em";></a>')
	}
	sendEventPublish(name: 'forecast_code', value: getDataValue('forecast_code'))
	sendEventPublish(name: 'forecast_text', value: getDataValue('forecast_text'))
	if(fcstHighLowPublish && extSource.toInteger() == 2){ // don't bother setting these values if it's not enabled
		sendEvent(name: 'forecastHigh', value: getDataValue('forecastHigh').toBigDecimal(), unit: tMetric)
		sendEvent(name: 'forecastLow', value: getDataValue('forecastLow').toBigDecimal(), unit: tMetric)
	}
	sendEventPublish(name: 'illuminated', value: getDataValue('illuminated') + ' lx')
	sendEventPublish(name: 'is_day', value: getDataValue('is_day'))
	sendEventPublish(name: 'moonPhase', value: getDataValue('moonPhase'))
	if(obspollPublish){  // don't bother setting these values if it's not enabled
		sendEvent(name: 'last_observation_Station', value: new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('sotime')).format(dateFormat, TimeZone.getDefault()) + ', ' + new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('sotime')).format(timeFormat, TimeZone.getDefault()))
		sendEvent(name: 'last_poll_Station', value: new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('sutime')).format(dateFormat, TimeZone.getDefault()) + ', ' + new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('sutime')).format(timeFormat, TimeZone.getDefault()))
		sendEvent(name: 'last_observation_Forecast', value: new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('fotime')).format(dateFormat, TimeZone.getDefault()) + ', ' + new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('fotime')).format(timeFormat, TimeZone.getDefault()))
        sendEvent(name: 'last_poll_Forecast', value: new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('futime')).format(dateFormat, TimeZone.getDefault()) + ', ' + new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('futime')).format(timeFormat, TimeZone.getDefault()))
	}
	sendEventPublish(name: 'ozone', value: Math.round(getDataValue('ozone').toBigDecimal() * 10) / 10)
	sendEventPublish(name: 'precip_today', value: getDataValue('precip_today').toBigDecimal(), unit: rMetric)
	if(precipExtendedPublish && extSource.toInteger() == 2){ // don't bother setting these values if it's not enabled
		sendEvent(name: 'rainDayAfterTomorrow', value: getDataValue('rainDayAfterTomorrow').toBigDecimal(), unit: '%')	
		sendEvent(name: 'rainTomorrow', value: getDataValue('rainTomorrow').toBigDecimal(), unit: '%')
	}
	sendEventPublish(name: 'solarradiation', value: getDataValue('solarradiation'))
	sendEventPublish(name: 'state', value: getDataValue('state'))
	if(extSource.toInteger()==1){
		sendEventPublish(name: 'vis', value: getDataValue('vis'))
	}else{
		sendEventPublish(name: 'vis', value: Math.round(getDataValue('vis').toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger(), unit: (dMetric=='MPH' ? 'miles' : 'kilometers'))
	}
	sendEventPublish(name: 'wind_degree', value: getDataValue('wind_degree').toInteger(), unit: 'DEGREE')
	sendEventPublish(name: 'wind_direction', value: getDataValue('wind_direction'))
	sendEventPublish(name: 'wind_cardinal', value: getDataValue('wind_cardinal'))
	sendEventPublish(name: 'wind_gust', value: Math.round(getDataValue('wind_gust').toBigDecimal() * getDataValue('mult_twd').toInteger()) / getDataValue('mult_twd').toInteger(), unit: dMetric)
	sendEventPublish(name: 'wind_string', value: getDataValue('wind_string'))
	if(nearestStormPublish) {
		sendEvent(name: 'nearestStormBearing', value: getDataValue('nearestStormBearing'), unit: 'DEGREE')
		sendEvent(name: 'nearestStormCardinal', value: getDataValue('nearestStormCardinal'))
		sendEvent(name: 'nearestStormDirection', value: getDataValue('nearestStormDirection'))
		sendEvent(name: 'nearestStormDistance', value: getDataValue('nearestStormDistance').toInteger(), unit: (dMetric=='MPH' ? 'miles' : 'kilometers'))
	}
//  <<<<<<<<<< Begin Built Weather Summary text >>>>>>>>>>
    Summary_last_poll_time = (getDataValue('sutime') > getDataValue('futime') ? new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('sutime')).format(timeFormat, TimeZone.getDefault()) : new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('futime')).format(timeFormat, TimeZone.getDefault()))
	Summary_last_poll_date = (getDataValue('sutime') > getDataValue('futime') ? new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('sutime')).format(dateFormat, TimeZone.getDefault()) : new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('futime')).format(dateFormat, TimeZone.getDefault()))
    if(weatherSummaryPublish){ // don't bother setting these values if it's not enabled
		String Summary_forecastTemp
		String Summary_precip
		String Summary_vis
		String mtprecip
		if(extSource.toInteger() == 2){
			Summary_forecastTemp = ' with a high of ' + String.format(ddisp_twd, getDataValue('forecastHigh').toBigDecimal()) + tMetric + ' and a low of ' + String.format(ddisp_twd, getDataValue('forecastLow').toBigDecimal()) + tMetric + '. '
			Summary_precip = 'There is a ' + getDataValue('percentPrecip') + '% chance of precipitation. '
			Summary_vis = 'Visibility is around ' + String.format(ddisp_twd, getDataValue('vis').toBigDecimal()) + (dMetric=='MPH' ? ' miles.' : ' kilometers.')
			mtprecip = getDataValue('percentPrecip') + '%'
		}else{
			Summary_forecastTemp = ''
			Summary_precip = ''
			Summary_vis = ''
			mtprecip = 'N/A'
		}
		SummaryMessage(summaryType, Summary_last_poll_date, Summary_last_poll_time, Summary_forecastTemp, Summary_precip, Summary_vis)
	}
//  >>>>>>>>>> End Built Weather Summary text <<<<<<<<<<
	String dsIcon = '<a href="https://darksky.net/poweredby/" target="_blank"><img src=' + getDataValue('iconLocation') + (dsIconbackgrounddark ? 'dsD.png' : 'dsL.png') + ' style="height:2em;"></a>'
	String dsText = '<a href="https://darksky.net/poweredby/" target="_blank">Powered by Dark Sky</a>'
    //  <<<<<<<<<< Begin Built 3dayfcstTile >>>>>>>>>>
	if(threedayTilePublish) {
		String my3day = '<style type="text/css">'
		my3day += '.centerImage'
		my3day += '{text-align:center;display:inline;height:50%;}'
		my3day += '</style>'
		my3day += '<table align="center" style="width:100%">'
		my3day += '<tr>'
		my3day += '<td></td>'
		my3day += '<td><a href="https://darksky.net/forecast/' + altLat + ',' + altLon + '" target="_blank">Today</a></td>'
		my3day += '<td>' + getDataValue('day1') + '</td>'
		my3day += '<td>' + getDataValue('day2') + '</td>'
		my3day += '</tr>'
		my3day += '<tr>'
		my3day += '<td></td>'
		my3day += '<td>' + getDataValue('imgName0') + '</td>'
		my3day += '<td>' + getDataValue('imgName1') + '</td>'
		my3day += '<td>' + getDataValue('imgName2') + '</td>'
		my3day += '</tr>'
		my3day += '<tr>'
		my3day += '<td style="text-align:right">Now:</td>'
		my3day += '<td>' + String.format(ddisp_twd, getDataValue('temperature').toBigDecimal()) + tMetric + '</td>'
		my3day += '<td>' + getDataValue('forecast_text1') + '</td>'
		my3day += '<td>' + getDataValue('forecast_text2') + '</td>'
		my3day += '</tr>'
		my3day += '<tr>'
		my3day += '<td style="text-align:right">Low:</td>'
		my3day += '<td>' + String.format(ddisp_twd, getDataValue('forecastLow').toBigDecimal()) + tMetric + '</td>'
		my3day += '<td>' + String.format(ddisp_twd, getDataValue('forecastLow1').toBigDecimal()) + tMetric + '</td>'
		my3day += '<td>' + String.format(ddisp_twd, getDataValue('forecastLow2').toBigDecimal()) + tMetric + '</td>'
		my3day += '</tr>'
		my3day += '<tr>'
		my3day += '<td style="text-align:right">High:</td>'
		my3day += '<td>' + String.format(ddisp_twd, getDataValue('forecastHigh').toBigDecimal()) + tMetric + '</td>'
		my3day += '<td>' + String.format(ddisp_twd, getDataValue('forecastHigh1').toBigDecimal()) + tMetric + '</td>'
		my3day += '<td>' + String.format(ddisp_twd, getDataValue('forecastHigh2').toBigDecimal()) + tMetric + '</td>'
		my3day += '</tr>'
		my3day += '<tr>'
		my3day += '<td style="text-align:right">PoP:</td>'
		my3day += '<td>' + getDataValue('PoP') + '%</td>'
		my3day += '<td>' + getDataValue('PoP1') + '%</td>'
		my3day += '<td>' + getDataValue('PoP2') + '%</td>'
		my3day += '</tr>'
		my3day += '</table>'
		if(my3day.length() + 11 > 1024) {
			my3day = 'Too much data to display.</br></br>Exceeds maximum tile length by ' + 1024 - my3day.length() - 11 + ' characters.'
		}else if((my3day.length() + dsIcon.length() + 11) < 1025) {
			my3day += dsIcon + '@ ' + Summary_last_poll_time
		}else if((my3day.length() + dsText.length() + 11) < 1025) {
			my3day += dsText + ' @ ' + Summary_last_poll_time
		}else{
			my3day += 'Powered by Dark Sky'
		}
		sendEvent(name: 'threedayfcstTile', value: my3day.take(1024))
	}
//  >>>>>>>>>> End Built 3dayfcstTile <<<<<<<<<<

//  <<<<<<<<<< Begin Built alertTile >>>>>>>>>>
	if(alertPublish){ // don't bother setting these values if it's not enabled
		String alertTime = new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('futime')).format(timeFormat, TimeZone.getDefault())
		String alertDate = new Date().parse('EEE MMM dd HH:mm:ss z yyyy', getDataValue('futime')).format(dateFormat, TimeZone.getDefault())
		String alertTile = 'Weather Alerts for <a href="https://darksky.net/forecast/' + altLat + ',' + altLon + '" target="_blank">' + getDataValue('city') + ', ' + getDataValue('state') + '</a><br>updated at ' + alertTime + ' on ' + alertDate + '.<br>'
		alertTile+= getDataValue('alertTileLink') + '<br>'
		alertTile+= dsIcon
		updateDataValue('alertTile', alertTile)
		sendEvent(name: 'alert', value: getDataValue('alert'))
		sendEvent(name: 'alertTile', value: getDataValue('alertTile'))
	}
//  >>>>>>>>>> End Built alertTile <<<<<<<<<<

//  <<<<<<<<<< Begin Built mytext >>>>>>>>>>
	if(myTilePublish){ // don't bother setting these values if it's not enabled
		Boolean gitclose = (getDataValue('iconLocation').toLowerCase().contains('://github.com/')) && (getDataValue('iconLocation').toLowerCase().contains('/blob/master/'))
		String iconClose = (gitclose ? '?raw=true' : '')
		String iconCloseStyled = iconClose + '>'
		Boolean noAlert = (!getDataValue('possAlert') || getDataValue('possAlert')=='' || getDataValue('possAlert')=='false')
		Boolean raintoday = (getDataValue('precip_today').toBigDecimal() > 0.0)
		String alertStyleOpen = (noAlert ? '' :  '<span>')
		String alertStyleClose = (noAlert ? '<br>' : '</span><br>')
		BigDecimal wgust
		if(getDataValue('wind_gust').toBigDecimal() < 1.0 ) {
			wgust = 0.0g
		} else {
			wgust = getDataValue('wind_gust').toBigDecimal()
		}
		String mytextb = '<span style="display:inline;"><a href="https://darksky.net/forecast/' + altLat + ',' + altLon + '" target="_blank">' + getDataValue('city') + ', ' + getDataValue('state')  + '</a><br>'
		String mytextm1 = getDataValue('condition_text') + (noAlert ? '' : ' | ') + alertStyleOpen + (noAlert ? '' : getDataValue('alertLink')) + alertStyleClose
		String mytextm2 = getDataValue('condition_text') + (noAlert ? '' : ' | ') + alertStyleOpen + (noAlert ? '' : getDataValue('alertLink2')) + alertStyleClose
		String mytextm3 = getDataValue('condition_text') + (noAlert ? '' : ' | ') + alertStyleOpen + (noAlert ? '' : getDataValue('alertLink3')) + alertStyleClose
		String mytexte = String.format(ddisp_twd, getDataValue('temperature').toBigDecimal()) + tMetric + '<img src=' + getDataValue('condition_icon_url') + iconClose + ' style="height:2.2em;display:inline;">'
		mytexte+= ' Feels like ' + String.format(ddisp_twd, getDataValue('feelsLike').toBigDecimal()) + tMetric + '<br></span>'
		mytexte+= '<span style="font-size:.9em;"><img src=' + getDataValue('iconLocation') + getDataValue('wind_bft_icon') + iconCloseStyled + getDataValue('wind_direction') + ' '
		mytexte+= getDataValue('wind').toBigDecimal() < 1.0 ? 'calm' : '@ ' + String.format(ddisp_twd, getDataValue('wind').toBigDecimal()) + ' ' + dMetric
		mytexte+= ', gusts ' + ((wgust < 1.0) ? 'calm' :  '@ ' + String.format(ddisp_twd, wgust) + ' ' + dMetric) + '<br>'
		mytexte+= '<img src=' + getDataValue('iconLocation') + 'wb.png' + iconCloseStyled + String.format(ddisp_p, getDataValue('pressure').toBigDecimal()) + ' ' + pMetric + '   <img src=' + getDataValue('iconLocation') + 'wh.png' + iconCloseStyled
		mytexte+= getDataValue('humidity') + '%   ' + '<img src=' + getDataValue('iconLocation') + 'wu.png' + iconCloseStyled + getDataValue('percentPrecip') + '%'
		mytexte+= (raintoday ? '   <img src=' + getDataValue('iconLocation') + 'wr.png' + iconCloseStyled + String.format(ddisp_r, getDataValue('precip_today').toBigDecimal()) + ' ' + rMetric : '') + '<br>'
		mytexte+= '<img src=' + getDataValue('iconLocation') + 'wsr.png' + iconCloseStyled + getDataValue('localSunrise') + '     <img src=' + getDataValue('iconLocation') + 'wss.png' + iconCloseStyled
		mytexte+= getDataValue('localSunset') + '     Updated: ' + Summary_last_poll_time
		String mytext = mytextb + mytextm1 + mytexte
		if((mytext.length() + dsIcon.length() + 10) < 1025) {
			mytext+= '<br>' + dsIcon + '</span>'
		}else{
			if((mytext.length() + dsText.length() + 10) < 1025) {
				mytext+= '<br>' + dsText + '</span>'
			}else{
				mytext = mytextb + mytextm2 + mytexte
				if((mytext.length() + dsIcon.length() + 10) < 1025) {
					mytext+= '<br>' + dsIcon + '</span>'
				}else if((mytext.length() + dsText.length() + 10) < 1025) {
					mytext+= '<br>' + dsText + '</span>'
				}else{
					mytext+= '<br>Powered by Dark Sky</span>'
				}
			}
		}
		if(mytext.length() > 1024) {
			Integer iconfilepath = ('<img src=' + getDataValue('iconLocation') + getDataValue('wind_bft_icon') + iconCloseStyled).length()
			Integer excess = (mytext.length() - 1024)
			Integer removeicons = 0
			Integer ics = iconfilepath + iconCloseStyled.length()
			if(raintoday) {
				if((excess - ics + 11) < 0) {
					removeicons = 1  //Remove sunset
				}else if((excess - (ics * 2) + 20) < 0) {
					removeicons = 2 //remove sunset and sunrise
				}else if((excess - (ics * 3) + 31) < 0) {
					removeicons = 3 //remove sunset, sunrise, Precip
				}else if((excess - (ics * 4) + 42) < 0) {
					removeicons = 4 //remove sunset, sunrise, Precip, PercentPrecip
				}else if((excess - (ics * 4) + 49) < 0) {
					removeicons = 5 //remove sunset, sunrise, Precip, PercentPrecip, Humidity
				}else if((excess - (ics * 5) + 53) < 0) {
					removeicons = 6 //remove sunset, sunrise, Precip, PercentPrecip, Humidity, Pressure
				}else if((excess - (ics * 6) + 53) < 0) {
					removeicons = 7 //remove sunset, sunrise, Precip, PercentPrecip, Humidity, Pressure, Wind
				}else if((excess - (ics * 7) + 53) < 0) {
					removeicons = 8 //remove sunset, sunrise, Precip, PercentPrecip, Humidity, Pressure, Wind, condition
				}else{
					removeicons = 9 // still need to remove html formatting
				}
			}else{
				if((excess - ics + 11) < 0) {
					removeicons = 1  //Remove sunset
				}else if((excess - (ics * 2) + 20) < 0) {
					removeicons = 2 //remove sunset and sunrise
				}else if((excess - (ics * 3) + 31) < 0) {
					removeicons = 3 //remove sunset, sunrise, PercentPrecip
				}else if((excess - (ics * 4) + 38) < 0) {
					removeicons = 4 //remove sunset, sunrise, PercentPrecip, Humidity
				}else if((excess - (ics * 5) + 42) < 0) {
					removeicons = 5 //remove sunset, sunrise, PercentPrecip, Humidity, Pressure
				}else if((excess - (ics * 6) + 42) < 0) {
					removeicons = 6 //remove sunset, sunrise, PercentPrecip, Humidity, Pressure, Wind
				}else if((excess - (ics * 7) + 42) < 0) {
					removeicons = 7 //remove sunset, sunrise, PercentPrecip, Humidity, Pressure, Wind, condition
				}else{
					removeicons = 8 // still need to remove html formatting
				}
			}

			if(removeicons < (raintoday ? 9 : 8)) {
				LOGINFO('myTile exceeds 1,024 characters (' + mytext.length() + ') ... removing last ' + (removeicons + 1).toString() + ' icons.')
				mytext = '<span>' + getDataValue('city') + '<br>'
				mytext+= getDataValue('condition_text') + (noAlert ? '' : ' | ') + alertStyleOpen + (noAlert ? '' : getDataValue('alert')) + alertStyleClose + '<br>'
				mytext+= String.format(ddisp_twd, getDataValue('temperature').toBigDecimal()) + tMetric + ' ' + (removeicons < (raintoday ? 8 : 7) ? '<img src=' + getDataValue('condition_icon_url') + iconClose + ' style="height:2.0em;display:inline;">' : '')
				mytext+= ' Feels like ' + String.format(ddisp_twd, getDataValue('feelsLike').toBigDecimal()) + tMetric + '<br></span>'
				mytext+= '<span style="font-size:.8em;">' + (removeicons < (raintoday ? 7 : 6) ? '<img src=' + getDataValue('iconLocation') + getDataValue('wind_bft_icon') + iconCloseStyled : '') + getDataValue('wind_direction') + ' '
				mytext+= getDataValue('wind').toBigDecimal() < 1.0 ? 'calm' : '@ ' + String.format(ddisp_twd, getDataValue('wind').toBigDecimal()) + ' ' + dMetric
				mytext+= ', gusts ' + ((wgust < 1.0) ? 'calm' :  '@ ' + String.format(ddisp_twd, wgust) + ' ' + dMetric) + '<br>'
				mytext+= (removeicons < (raintoday ? 6 : 5) ? '<img src=' + getDataValue('iconLocation') + 'wb.png' + iconCloseStyled : 'Bar: ') + String.format(ddisp_p, getDataValue('pressure').toBigDecimal()) + pMetric + '  '
				mytext+= (removeicons < (raintoday ? 5 : 4) ? '<img src=' + getDataValue('iconLocation') + 'wh.png' + iconCloseStyled : ' | Hum: ') + getDataValue('humidity') + '%  '
				mytext+= (removeicons < (raintoday ? 4 : 3) ? '<img src=' + getDataValue('iconLocation') + 'wu.png' + iconCloseStyled : ' | Precip%: ') + getDataValue('percentPrecip') + '%'
				mytext+= (raintoday ? (removeicons < 3 ? ('<img src=' + getDataValue('iconLocation') + 'wr.png' + iconCloseStyled) : (' | Precip: ')) + String.format(ddisp_twd, getDataValue('precip_today').toBigDecimal()) + ' ' + rMetric : '') + '<br>'
				mytext+= (removeicons < 2 ? ('<img src=' + getDataValue('iconLocation') + 'wsr.png' + iconCloseStyled) : ('Sunrise: ')) + getDataValue('localSunrise') + '  '
				mytext+= (removeicons < 1 ? ('<img src=' + getDataValue('iconLocation') + 'wss.png' + iconCloseStyled) : (' | Sunset: ')) + getDataValue('localSunset')
				mytext+= '     Updated ' + Summary_last_poll_time + '</span>'
			}else{
				LOGINFO('myTile still exceeds 1,024 characters (' + mytext.length() + ') ... removing all formatting.')
				mytext = getDataValue('city') + '<br>'
				mytext+= getDataValue('condition_text') + (noAlert ? '' : ' | ') + (noAlert ? '' : getDataValue('alert')) + '<br>'
				mytext+= String.format(ddisp_twd, getDataValue('temperature').toBigDecimal()) + tMetric + ' Feels like ' + String.format(ddisp_twd, getDataValue('feelsLike').toBigDecimal()) + tMetric + '<br>'
				mytext+= getDataValue('wind_direction') + ' '
				mytext+= getDataValue('wind').toBigDecimal() < 1.0 ? 'calm' : '@ ' + String.format(ddisp_twd, getDataValue('wind').toBigDecimal()) + ' ' + dMetric
				mytext+= ', gusts ' + ((wgust < 1.0) ? 'calm' :  '@ ' + String.format(ddisp_twd, wgust) + ' ' + dMetric) + '<br>'
				mytext+= 'Bar: ' + String.format(ddisp_p, getDataValue('pressure').toBigDecimal()) + pMetric
				mytext+= ' | Hum: ' + getDataValue('humidity') + '%  ' + ' | Precip%: ' + getDataValue('percentPrecip') + '%'
				mytext+= (raintoday ? ' | Precip: ' + String.format(ddisp_twd, getDataValue('precip_today').toBigDecimal()) + ' ' + rMetric : '') + '<br>'
				mytext+= 'Sunrise: ' + getDataValue('localSunrise') + ' | Sunset:' + getDataValue('localSunset') + ' |  Updated:' + Summary_last_poll_time
				if(mytext.length() > 1024) {
					LOGINFO('myTile even still exceeds 1,024 characters (' + mytext.length() + ') ... truncating.')
				}
			}
		}
		LOGINFO('mytext: ' + mytext)
		sendEvent(name: 'myTile', value: mytext.take(1024))
	}
//  >>>>>>>>>> End Built mytext <<<<<<<<<<
}
// >>>>>>>>>> End Post-Poll Routines <<<<<<<<<<

public void refresh() {
	updateLux(true)
	return
}

void installed() {
    updateDataValue('is_light', 'true')
    updateDataValue('is_lightOld', getDataValue('is_light')) //avoid startup oscilation
}

void updated()   {
	unschedule()
	updateCheck()
	initMe()
	runIn(5,finishSched)
}

void finishSched() {
    pollSunRiseSet()
    initialize_poll()
    runEvery5Minutes(updateLux, [Data: [true]])
	Random rand = new Random(now())
	Integer ssseconds = rand.nextInt(60)
    schedule("${ssseconds} 20 0/8 ? * * *", pollSunRiseSet)
    runIn(5, pollData)
	if(settingEnable) runIn(2100,settingsOff)// 'roll up' (hide) the condition selectors after 35 min
    if(settings.logSet) runIn(1800,logsOff)// turns off extended logging after 30 min
	Integer r_minutes = rand.nextInt(60)
    schedule("0 ${r_minutes} 8 ? * FRI *", updateCheck)
}

void initMe() {
    updateDataValue('forecastPoll', 'false')
	Boolean logSet = (settings.logSet ?: false)
	Boolean altCoord = (settings.altCoord ?: false)
	String valtLat = location.latitude.toString().replace(' ', '')
	String valtLon = location.longitude.toString().replace(' ', '')
	String altLat = settings.altLat ?: valtLat
	String altLon = settings.altLon ?: valtLon    
	if (altCoord) {
		if (altLat == null) {
			device.updateSetting('altLat', [value:valtLat,type:'text'])
		}
		if (altLon == null) {
			device.updateSetting('altLon', [value:valtLon,type:'text'])
		}
		if (altLat == null || altLon == null) {
			if ((valtLAt == null) || (ValtLat = '')) {
				LOGERR('The Override Coorinates feature is selected but both Hub & the Override Latitude are null.')
			} else {
				device.updateSetting('altLat', [value:valtLat,type:'text'])
			}
			if ((valtLon == null) || (valtLon = '')) {
				LOGERR('The Override Coorinates feature is selected but both Hub & the Override Longitude are null.')
			} else {
				device.updateSetting('altLon', [value:valtLon,type:'text'])
			}
		}
	} else {
		device.updateSetting('altLat', [value:valtLat,type:'text'])
		device.updateSetting('altLon', [value:valtLon,type:'text'])
		if (altLat == null || altLon == null) {
			if ((valtLat == null) || (valtLat = '')) {
				LOGERR('The Hub\'s latitude is not set. Please set it, or use the Override Coorinates feature.')
			} else {
				device.updateSetting('altLat', [value:valtLat,type:'text'])
			}
			if ((valtLon == null) || (valtLon = '')) {
				LOGERR('The Hub\'s longitude is not set. Please set it, or use the Override Coorinates feature.')
			} else {
				device.updateSetting('altLon', [value:valtLon,type:'text'])
			}
		}    
	}
	Boolean luxjitter = (settings.luxjitter ?: false)    
	Boolean iconType = (settings.iconType ?: false)
	updateDataValue('iconType', iconType ? 'true' : 'false')
	Boolean summaryType = (settings.summaryType ?: false)
	String iconLocation = (settings.iconLocation ?: 'https://tinyurl.com/y6xrbhpf/')
	updateDataValue('iconLocation', iconLocation)
	state.DarkSky = '<a href="https://darksky.net/poweredby/" target="_blank"><img src=' + getDataValue('iconLocation') + 'dsD.png style="height:2em;"></a>'
	setDateTimeFormats(datetimeFormat)
	String dMetric
	String pMetric
	String rMetric
	String tMetric
	String datetimeFormat = (settings.datetimeFormat ?: '1')
	String distanceFormat = (settings.distanceFormat ?: 'Miles (mph)')
	String pressureFormat = (settings.pressureFormat ?: 'Inches')
	String rainFormat = (settings.rainFormat ?: 'Inches')
	String tempFormat = (settings.tempFormat ?: 'Fahrenheit (°F)')
	setMeasurementMetrics(distanceFormat, pressureFormat, rainFormat, tempFormat)
	String ddisp_twd
	String ddisp_p
	String ddisp_r
	String mult_twd
	String mult_p
	String mult_r    
	String TWDDecimals = (settings.TWDDecimals ?: '0')
	String PDecimals = (settings.PDecimals ?: '0')
	String RDecimals = (settings.RDecimals ?: '0')
	setDisplayDecimals(TWDDecimals, PDecimals, RDecimals)
	Integer extSource = (settings.extSource.toInteger() ?: 2).toInteger()
	String pollIntervalStation = (settings.pollIntervalStation ?: '3 Hours')
	String pollLocationStation = (settings.pollLocationStation ?: 'http://')
	String pollIntervalForecast = (settings.pollIntervalForecast ?: '3 Hours')
	String pollIntervalForecastnight = (settings.pollIntervalForecastnight ?: '3 Hours')
	Boolean dsIconbackgrounddark = (settings.dsIconbackgrounddark ?: true)
	Boolean sourcefeelsLike = (settings.sourcefeelsLike ?: false)
	Boolean sourceIllumination = (settings.sourceIllumination ?: false)
	Boolean sourceUV = (settings.sourceUV ?: false)
	Boolean sourceWind = (settings.sourceWind ?: false)
}

void initialize_poll() {
	unschedule(pollWD)
	unschedule(pollDS)
	Random rand = new Random(now())
	Integer ssseconds = rand.nextInt(60)
	Integer minutes2 = rand.nextInt(2)
	Integer minutes5 = rand.nextInt(5)
	Integer minutes10 = rand.nextInt(10)
	Integer minutes15 = rand.nextInt(15)
	Integer minutes30 = rand.nextInt(30)
	Integer minutes60 = rand.nextInt(60)
	Integer hours3 = rand.nextInt(3)
	Integer dsseconds
	Integer wdseconds
	if(ssseconds < 52 ){
		wdseconds = ssseconds + 4
		dsseconds = wdseconds + 4
	}else if(ssseconds < 56 ){
		wdseconds = ssseconds + 4
		dsseconds = wdseconds - 60 + 4
	}else{
		wdseconds = ssseconds - 60 + 4
		dsseconds = wdseconds + 4
	}   
	String pollIntervalFcst = (settings.pollIntervalForecast ?: '3 Hours')
	String pollIntervalFcstnight = (settings.pollIntervalForecastnight ?: '3 Hours')
	String myFcstPoll = pollIntervalFcst
	if(getDataValue('is_light')=='true') {
		myFcstPoll = pollIntervalFcst
	} else {
		myFcstPoll = pollIntervalFcstnight
	}
	if(myFcstPoll == 'Manual Forcecast Poll Only'){
		LOGINFO('MANUAL FORECAST POLLING ONLY')
	} else {
        myFcstPoll = myFcstPoll.replace(' ','')
        String myFcstSched = "${dsseconds} ${minutes60} ${hours3/3} * * ? *"
        LOGINFO('myFcstPoll: ' + myFcstPoll)
        switch(myFcstPoll) {
            case '2Minutes':
                myFcstSched = "${dsseconds} ${minutes2}/2 * * * ? *"
                break
            case '5Minutes':
                myFcstSched = "${dsseconds} ${minutes5}/5 * * * ? *"
                break
            case '10 Minutes':
                myFcstSched = "${dsseconds} ${minutes10}/10 * * * ? *"
                break
            case '15Minutes':
                myFcstSched = "${dsseconds} ${minutes15}/15 * * * ? *"
                break
            case '30Minutes':
                myFcstSched = "${dsseconds} ${minutes30}/30 * * * ? *"
                break
            case '1Hour':
                myFcstSched = "${dsseconds} ${minutes60} * * * ? *"
                break
            case '3Hours':
                myFcstSched = "${dsseconds} ${minutes60} ${hours3}/3 * * ? *"
		}
        schedule(myFcstSched, pollDS)
	}
	String pollIntervalStation = (settings.pollIntervalStation ?: '3 Hours')
	String myStationPoll = pollIntervalStation
	if(myStationPoll == 'Manual Forcecast Poll Only'){
		LOGINFO('MANUAL STATION POLLING ONLY')
	} else {
        myStationPoll = myStationPoll.replace(' ','')
        String myStationSched = "${dsseconds} ${minutes60} ${hours3/3} * * ? *"
        LOGINFO('myStationPoll: ' + myStationPoll)
        switch(myStationPoll) {
            case '1Minute':
                myStationSched = "${dsseconds} * * * * ? *"
                break
            case '2Minutes':
                myStationSched = "${dsseconds} ${minutes2}/2 * * * ? *"
                break
            case '5Minutes':
                myStationSched = "${dsseconds} ${minutes5}/5 * * * ? *"
                break
            case '10 Minutes':
                myStationSched = "${dsseconds} ${minutes10}/10 * * * ? *"
                break
            case '15Minutes':
                myStationSched = "${dsseconds} ${minutes15}/15 * * * ? *"
                break
            case '30Minutes':
                myStationSched = "${dsseconds} ${minutes30}/30 * * * ? *"
                break
            case '1Hour':
                myStationSched = "${dsseconds} ${minutes60} * * * ? *"
                break
            case '3Hours':
                myStationSched = "${dsseconds} ${minutes60} ${hours3}/3 * * ? *"
		}
        schedule(myStationSched, pollWD)
	}
}

public void pollData() {
	pollWD()
	if (extSource.toInteger() == 2) { pollDS() }
	return
}
// ************************************************************************************************

public void setDateTimeFormats(String formatselector){
	switch(formatselector) {
		case '1': DTFormat = 'M/d/yyyy h:mm a';   dateFormat = 'M/d/yyyy';   timeFormat = 'h:mm a'; break;
		case '2': DTFormat = 'M/d/yyyy HH:mm';    dateFormat = 'M/d/yyyy';   timeFormat = 'HH:mm';  break;
		case '3': DTFormat = 'MM/dd/yyyy h:mm a'; dateFormat = 'MM/dd/yyyy'; timeFormat = 'h:mm a'; break;
		case '4': DTFormat = 'MM/dd/yyyy HH:mm';  dateFormat = 'MM/dd/yyyy'; timeFormat = 'HH:mm';  break;
		case '5': DTFormat = 'd/M/yyyy h:mm a';   dateFormat = 'd/M/yyyy';   timeFormat = 'h:mm a'; break;
		case '6': DTFormat = 'd/M/yyyy HH:mm';    dateFormat = 'd/M/yyyy';   timeFormat = 'HH:mm';  break;
		case '7': DTFormat = 'dd/MM/yyyy h:mm a'; dateFormat = 'dd/MM/yyyy'; timeFormat = 'h:mm a'; break;
		case '8': DTFormat = 'dd/MM/yyyy HH:mm';  dateFormat = 'dd/MM/yyyy'; timeFormat = 'HH:mm';  break;
		case '9': DTFormat = 'yyyy/MM/dd HH:mm';  dateFormat = 'yyyy/MM/dd'; timeFormat = 'HH:mm';  break;
		default: DTFormat = 'M/d/yyyy h:mm a';  dateFormat = 'M/d/yyyy';   timeFormat = 'h:mm a'; break;
	}
	return
}

public void setMeasurementMetrics(distFormat, pressFormat, precipFormat, temptFormat){
	if(distFormat == 'Miles (mph)') {
		dMetric = 'MPH'
	} else if(distFormat == 'knots') {
		dMetric = 'knots'
	} else if(distFormat == 'Kilometers (kph)') {
		dMetric = 'KPH'
	} else {
		dMetric = 'm/s'
	}
	if(pressFormat == 'Millibar') {
		pMetric = 'MBAR'
	} else if(pressFormat == 'Inches') {
		pMetric = 'inHg'
	} else {
		pMetric = 'hPa'
	}
	if(precipFormat == 'Millimeters') {
		rMetric = 'mm'
	} else {
		rMetric = 'inches'
	}
	if(temptFormat == 'Fahrenheit (°F)') {
		tMetric = '°F'
	} else {
		tMetric = '°C'
	}        
	return
}

public void setDisplayDecimals(TWDDisp, PressDisp, RainDisp) {
	switch(TWDDisp) {
		case '0': ddisp_twd = '%3.0f'; mult_twd = '1'; break;
		case '1': ddisp_twd = '%3.1f'; mult_twd = '10'; break;
		case '2': ddisp_twd = '%3.2f'; mult_twd = '100'; break;
		case '3': ddisp_twd = '%3.3f'; mult_twd = '1000'; break;
		case '4': ddisp_twd = '%3.4f'; mult_twd = '10000'; break;
		default: ddisp_twd = '%3.0f'; mult_twd = '1'; break;
	}
	updateDataValue('ddisp_twd', ddisp_twd)
	updateDataValue('mult_twd', mult_twd)
	switch(PressDisp) {
		case '0': ddisp_p = '%,4.0f'; mult_p = '1'; break;
		case '1': ddisp_p = '%,4.1f'; mult_p = '10'; break;
		case '2': ddisp_p = '%,4.2f'; mult_p = '100'; break;
		case '3': ddisp_p = '%,4.3f'; mult_p = '1000'; break;
		case '4': ddisp_p = '%,4.4f'; mult_p = '10000'; break;
		default: ddisp_p = '%,4.0f'; mult_p = '1'; break;
	}
	updateDataValue('ddisp_p', ddisp_p)
	updateDataValue('mult_p', mult_p)
	switch(RainDisp) {
		case '0': ddisp_r = '%3.0f'; mult_r = '1'; break;
		case '1': ddisp_r = '%3.1f'; mult_r = '10'; break;
		case '2': ddisp_r = '%3.2f'; mult_r = '100'; break;
		case '3': ddisp_r = '%3.3f'; mult_r = '1000'; break;
		case '4': ddisp_r = '%3.4f'; mult_r = '10000'; break;
	default: ddisp_r = '%3.0f'; mult_r = '1'; break;
	}
	updateDataValue('ddisp_r', ddisp_r)
	updateDataValue('mult_r', mult_r)
	return
}

def estimateLux(String condition_code, Integer cloud) {
	Long lux = 0l
	Boolean aFCC             = true
	Double l
	String sod
	def sunRiseSet           = parseJson(getDataValue('sunRiseSet')).results
	def tZ                   = TimeZone.getDefault() //TimeZone.getTimeZone(tz_id)
	String lT                = new Date().format('yyyy-MM-dd\'T\'HH:mm:ssXXX', tZ)
	Long localeMillis         = getEpoch(lT)
	Long twilight_beginMillis = getEpoch(sunRiseSet.civil_twilight_begin)
	Long sunriseTimeMillis    = getEpoch(sunRiseSet.sunrise)
	Long noonTimeMillis       = getEpoch(sunRiseSet.solar_noon)
	Long sunsetTimeMillis     = getEpoch(sunRiseSet.sunset)
	Long twilight_endMillis   = getEpoch(sunRiseSet.civil_twilight_end)
	Long twiStartNextMillis   = twilight_beginMillis + 86400000L // = 24*60*60*1000 --> one day in milliseconds
	Long sunriseNextMillis    = sunriseTimeMillis + 86400000L 
	Long noonTimeNextMillis   = noonTimeMillis + 86400000L 
	Long sunsetNextMillis     = sunsetTimeMillis + 86400000L
	Long twiEndNextMillis     = twilight_endMillis + 86400000L

	switch(localeMillis) {
		case { it < twilight_beginMillis}:
			sod = 'Fully Night Time'
			lux = 5l
			aFCC = false
			break
		case { it < sunriseTimeMillis}:
			sod = 'between twilight and sunrise'
			l = (((localeMillis - twilight_beginMillis) * 50f) / (sunriseTimeMillis - twilight_beginMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as Long)
			break
		case { it < noonTimeMillis}:
			sod = 'between sunrise and noon'
			l = (((localeMillis - sunriseTimeMillis) * 10000f) / (noonTimeMillis - sunriseTimeMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as Long)
			break
		case { it < sunsetTimeMillis}:
			sod = 'between noon and sunset'
			l = (((sunsetTimeMillis - localeMillis) * 10000f) / (sunsetTimeMillis - noonTimeMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as Long)
			break
		case { it < twilight_endMillis}:
			sod = 'between sunset and twilight'
			l = (((twilight_endMillis - localeMillis) * 50f) / (twilight_endMillis - sunsetTimeMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as Long)
			break
		case { it < twiStartNextMillis}:
			sod = 'Fully Night Time'
			lux = 5l
			aFCC = false        
			break
		case { it < sunriseNextMillis}:
			sod = 'between twilight and sunrise'
			l = (((localeMillis - twiStartNextMillis) * 50f) / (sunriseNextMillis - twiStartNextMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as Long)
			break
		case { it < noonTimeNextMillis}:
			sod = 'between sunrise and noon'
			l = (((localeMillis - sunriseNextMillis) * 10000f) / (noonTimeNextMillis - sunriseNextMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as Long)
			break
		case { it < sunsetNextMillis}:
			sod = 'between noon and sunset'
			l = (((sunsetNextMillis - localeMillis) * 10000f) / (sunsetNextMillis - noonTimeNextMillis))
			lux = (l < 50f ? 50l : l.trunc(0) as Long)
			break
		case { it < twiEndNextMillis}:
			sod = 'between sunset and twilight'
			l = (((twiEndNextMillis - localeMillis) * 50f) / (twiEndNextMillis - sunsetNextMillis))
			lux = (l < 10f ? 10l : l.trunc(0) as Long)
			break
		default:
			sod = 'Fully Night Time'
			lux = 5l
			aFCC = false
			break
	}
    updateDataValue('bwn', sod)
	String cC = condition_code
	String cCT = 'not set'
	Double cCF = (!cloud || cloud=='') ? 0.998d : (1 - (cloud/100 / 3d))

	if(aFCC){
		if(extSource.toInteger() == 1 && cloud){
			cCF = (1 - (cloud/100 / 3d))
			cCT = 'using cloud cover'
		} else if(extSource.toInteger() == 2 && cloud !='' && cloud != null){
			LUitem = LUTable.find{ it.ccode == condition_code }            
			if (LUitem && (condition_code != 'unknown'))    {
				cCF = (LUitem ? LUitem.luxpercent : 0)
				cCT = (LUitem ? LUitem.ctext : 'unknown') + ' using cloud cover.'
			} else    {
				cCF = 1.0
				cCT = 'cloud not available now.'
			}
		} else {
			cCF = 1.0
			cCT = 'cloud not available now.'
		}
	}
	lux = (lux * cCF) as Long
	if(luxjitter){
// reduce event variability  code from @nh.schottfam
		if(lux > 1100) {
			Long t0 = (lux/800)
			lux = t0 * 800
		} else if(lux <= 1100 && lux > 400) {
			Long t0 = (lux/400)
			lux = t0 * 400
		} else {
			lux = 5
		}
	}
	lux = Math.max(lux, 5)
	LOGINFO('estimateLux: condition: ' + cC + ' | condition factor: ' + cCF + ' | condition text: ' + cCT + ' | lux: ' + lux + ' | sod: ' + sod)
	return [lux, sod]
}

public Long getEpoch (String aTime) {
	def tZ = TimeZone.getDefault()
	def localeTime = new Date().parse('yyyy-MM-dd\'T\'HH:mm:ssXXX', aTime, tZ)
	Long localeMillis = localeTime.getTime()
	return (localeMillis)
}

void SummaryMessage(Boolean SType, String Slast_poll_date, String Slast_poll_time, String SforecastTemp, String Sprecip, String Svis){
	BigDecimal windgust
	if(getDataValue('wind_gust') == '' || getDataValue('wind_gust').toBigDecimal() < 1.0 || getDataValue('wind_gust')==null) {
		windgust = 0.00g
	} else {
		windgust = getDataValue('wind_gust').toBigDecimal()
	}
	String wSum = (String)null
	if(SType == true){
		wSum = 'Weather summary for ' + getDataValue('city') + ', ' + getDataValue('state') + ' updated at ' + Slast_poll_time + ' on ' + Slast_poll_date + '. '
		wSum+= getDataValue('condition_text')
		wSum+= (!SforecastTemp || SforecastTemp=='') ? '. ' : SforecastTemp
		wSum+= 'Humidity is ' + getDataValue('humidity') + '% and the temperature is ' + String.format(ddisp_twd, getDataValue('temperature').toBigDecimal()) +  tMetric + '. '
		wSum+= 'The temperature feels like it is ' + String.format(ddisp_twd, getDataValue('feelsLike').toBigDecimal()) +  tMetric + '. '
		wSum+= 'Wind: ' + getDataValue('wind_string') + ', gusts: ' + ((windgust < 1.00) ? 'calm. ' : 'up to ' + String.format(ddisp_twd, windgust) + ' ' + dMetric + '. ')
		wSum+= Sprecip
		wSum+= Svis
		wSum+= ((!getDataValue('alert') || getDataValue('alert')==null) ? '' : ' ' + getDataValue('alert') + '. ')
	} else {
		wSum = getDataValue('condition_text') + ' '
		wSum+= (!SforecastTemp || SforecastTemp=='') ? '. ' : SforecastTemp
		wSum+= ' Humidity: ' + getDataValue('humidity') + '%. Temperature: ' + String.format(ddisp_twd, getDataValue('temperature').toBigDecimal()) + tMetric + '. '
		wSum+= getDataValue('wind_string') + ', gusts: ' + ((windgust == 0.00) ? 'calm. ' : 'up to ' + String.format(ddisp_twd, windgust) + ' ' + dMetric + '. ')
	}
	wSum = wSum.take(1024)
	sendEvent(name: 'weatherSummary', value: wSum)
	return
}

String getImgName(String wCode){
	LUitem = LUTable.find{ it.ccode == wCode }
	LOGINFO('getImgName Input: ' + wCode + '; Result: ' + (LUitem ? LUitem.altIcon : 'na.png'))
	return (getDataValue('iconLocation') + (LUitem ? LUitem.altIcon : 'na.png') + (((getDataValue('iconLocation').toLowerCase().contains('://github.com/')) && (getDataValue('iconLocation').toLowerCase().contains('/blob/master/'))) ? '?raw=true' : ''))
}

String getowmImgName(String wCode){
	LUitem = LUTable.find{ it.ccode == wCode }
	LOGINFO('getowmImgName Input: ' + wCode + '; Result: ' + (LUitem ? LUitem.owmIcon : ''))
	return (LUitem ? LUitem.owmIcon : '')
}

String getstdImgName(String wCode){
	LUitem = LUTable.find{ it.ccode == wCode }
	LOGINFO('getstdImgName Input: ' + wCode + '; Result: ' + (LUitem ? LUitem.stdIcon : ''))
	return (LUitem ? LUitem.stdIcon : '')
}

String getcondText(String wCode){
	LUitem = LUTable.find{ it.ccode == wCode }
	LOGINFO('getcondText Input: ' + wCode + '; Result: ' + (LUitem ? LUitem.ctext : ''))
	return (LUitem ? LUitem.ctext : '')
}

void logCheck(){
	if(settings.logSet == true){
		log.info('Weather-Display Driver - INFO: All Logging Enabled')
	} else {
		log.info('Weather-Display Driver - INFO: Further Logging Disabled')
	}
	return
}

void LOGDEBUG(txt){
	if(settings.logSet == true){ log.debug('Weather-Display Driver - DEBUG:  ' + txt) }
	return
}

void LOGINFO(txt){
	if(settings?.logSet == true){log.info('Weather-Display Driver - INFO:  ' + txt) }
	return
}

void LOGWARN(txt){
	log.warn('Weather-Display Driver - WARNING:  ' + txt)
	return
}

void LOGERR(txt){
	log.error('Weather-Display Driver - ERROR:  ' + txt)
	return
}

void logsOff(){
	log.info 'Weather-Display Driver - INFO: extended logging disabled...'
	device.updateSetting('logSet',[value:'false',type:'bool'])
}

void settingsOff(){
	log.info 'Weather-Display Driver - INFO: Settings disabled...'
	device.updateSetting('settingEnable',[value:'false',type:'bool'])
}

void sendEventPublish(evt)	{
// 	Purpose: Attribute sent to DB if selected
	if (settings."${evt.name + 'Publish'}") {
		sendEvent(name: evt.name, value: evt.value, descriptionText: evt.descriptionText, unit: evt.unit, displayed: evt.displayed);
		LOGINFO('Will publish: ' + evt.name) //: evt.name, evt.value evt.unit
	}
}

@Field final List    LUTable =     [
[ ccode: 'breezy', altIcon: '23.png', ctext: 'Breezy', owmIcon: '50d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'breezyfoggy', altIcon: '48.png', ctext: 'Breezy and Foggy', owmIcon: '50d', stdIcon: 'fog', luxpercent: 0.2 ],
[ ccode: 'breezymostlycloudy', altIcon: '51.png', ctext: 'Breezy and Mostly Cloudy', owmIcon: '04d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'breezyovercast', altIcon: '49.png', ctext: 'Breezy and Overcast', owmIcon: '04d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'breezypartlycloudy', altIcon: '53.png', ctext: 'Breezy and Partly Cloudy', owmIcon: '03d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'chancelightrain', altIcon: '39.png', ctext: 'Chance of Light Rain', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'chancelightsnow', altIcon: '41.png', ctext: 'Possible Light Snow', owmIcon: '13d', stdIcon: 'snow', luxpercent: 0.3 ],
[ ccode: 'chancelightsnowbreezy', altIcon: '54.png', ctext: 'Possible Light Snow and Breezy', owmIcon: '13d', stdIcon: 'snow', luxpercent: 0.3 ],
[ ccode: 'chancerain', altIcon: '39.png', ctext: 'Chance of Rain', owmIcon: '10d', stdIcon: 'chancerain', luxpercent: 0.7 ],
[ ccode: 'chancesleet', altIcon: '41.png', ctext: 'Chance of Sleet', owmIcon: '13d', stdIcon: 'chancesleet', luxpercent: 0.7 ],
[ ccode: 'chancesnow', altIcon: '41.png', ctext: 'Chance of Snow', owmIcon: '13d', stdIcon: 'chancesnow', luxpercent: 0.3 ],
[ ccode: 'chancetstorms', altIcon: '38.png', ctext: 'Chance of Thunderstorms', owmIcon: '11d', stdIcon: 'chancetstorms', luxpercent: 0.2 ],
[ ccode: 'chancelightsnowwindy', altIcon: '54.png', ctext: 'Possible Light Snow and Windy', owmIcon: '13d', stdIcon: 'chancesnow', luxpercent: 0.3 ],
[ ccode: 'clear', altIcon: '32.png', ctext: 'Clear', owmIcon: '01d', stdIcon: 'clear', luxpercent: 1 ],
[ ccode: 'cloudy', altIcon: '26.png', ctext: 'Overcast', owmIcon: '04d', stdIcon: 'cloudy', luxpercent: 0.6 ],
[ ccode: 'drizzle', altIcon: '9.png', ctext: 'Drizzle', owmIcon: '09d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'flurries', altIcon: '13.png', ctext: 'Snow Flurries', owmIcon: '13d', stdIcon: 'flurries', luxpercent: 0.4 ],
[ ccode: 'fog', altIcon: '19.png', ctext: 'Foggy', owmIcon: '50d', stdIcon: 'fog', luxpercent: 0.2 ],
[ ccode: 'heavyrain', altIcon: '12.png', ctext: 'Heavy Rain', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'heavyrainbreezy', altIcon: '1.png', ctext: 'Heavy Rain and Breezy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'heavyrainwindy', altIcon: '1.png', ctext: 'Heavy Rain and Windy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'lightrain', altIcon: '11.png', ctext: 'Light Rain', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'lightrainbreezy', altIcon: '2.png', ctext: 'Light Rain and Breezy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'lightrainwindy', altIcon: '2.png', ctext: 'Light Rain and Windy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'lightsleet', altIcon: '8.png', ctext: 'Light Sleet', owmIcon: '13d', stdIcon: 'sleet', luxpercent: 0.5 ],
[ ccode: 'lightsnow', altIcon: '14.png', ctext: 'Light Snow', owmIcon: '13d', stdIcon: 'snow', luxpercent: 0.3 ],
[ ccode: 'mostlycloudy', altIcon: '28.png', ctext: 'Mostly Cloudy', owmIcon: '04d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'partlycloudy', altIcon: '30.png', ctext: 'Partly Cloudy', owmIcon: '03d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'rain', altIcon: '12.png', ctext: 'Rain', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'rainbreezy', altIcon: '1.png', ctext: 'Rain and Breezy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'rainwindy', altIcon: '1.png', ctext: 'Rain and Windy', owmIcon: '10d', stdIcon: 'rain', luxpercent: 0.5 ],
[ ccode: 'sleet', altIcon: '10.png', ctext: 'Sleet', owmIcon: '13d', stdIcon: 'sleet', luxpercent: 0.5 ],
[ ccode: 'snow', altIcon: '15.png', ctext: 'Snow', owmIcon: '13d', stdIcon: 'snow', luxpercent: 0.3 ],
[ ccode: 'sunny', altIcon: '36.png', ctext: 'Sunny', owmIcon: '01d', stdIcon: 'clear', luxpercent: 1 ],
[ ccode: 'thunderstorm', altIcon: '0.png', ctext: 'Thunderstorm', owmIcon: '11d', stdIcon: 'tstorms', luxpercent: 0.3 ],
[ ccode: 'wind', altIcon: '23.png', ctext: 'Windy', owmIcon: '50d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'windfoggy', altIcon: '23.png', ctext: 'Windy and Foggy', owmIcon: '50d', stdIcon: 'fog', luxpercent: 0.2 ],
[ ccode: 'windmostlycloudy', altIcon: '51.png', ctext: 'Windy and Mostly Cloudy', owmIcon: '50d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'windovercast', altIcon: '49.png', ctext: 'Windy and Overcast', owmIcon: '50d', stdIcon: 'mostlycloudy', luxpercent: 0.6 ],
[ ccode: 'windpartlycloudy', altIcon: '53.png', ctext: 'Windy and Partly Cloudy', owmIcon: '50d', stdIcon: 'partlycloudy', luxpercent: 0.8 ],
[ ccode: 'nt_breezy', altIcon: '23.png', ctext: 'Breezy', owmIcon: '50n', stdIcon: 'nt_partlycloudy', luxpercent: 0 ],
[ ccode: 'nt_breezyfoggy', altIcon: '48.png', ctext: 'Breezy and Foggy', owmIcon: '50n', stdIcon: 'nt_fog', luxpercent: 0 ],
[ ccode: 'nt_breezymostlycloudy', altIcon: '50.png', ctext: 'Breezy and Mostly Cloudy', owmIcon: '04n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
[ ccode: 'nt_breezyovercast', altIcon: '49.png', ctext: 'Breezy and Overcast', owmIcon: '04n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
[ ccode: 'nt_breezypartlycloudy', altIcon: '52.png', ctext: 'Breezy and Partly Cloudy', owmIcon: '03n', stdIcon: 'nt_partlycloudy', luxpercent: 0 ],
[ ccode: 'nt_chancelightrain', altIcon: '45.png', ctext: 'Chance of Light Rain', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_chancelightsnow', altIcon: '46.png', ctext: 'Possible Light Snow', owmIcon: '13n', stdIcon: 'nt_snow', luxpercent: 0 ],
[ ccode: 'nt_chancelightsnowbreezy', altIcon: '55.png', ctext: 'Possible Light Snow and Breezy', owmIcon: '13n', stdIcon: 'nt_snow', luxpercent: 0 ],
[ ccode: 'nt_chancerain', altIcon: '39.png', ctext: 'Chance of Rain', owmIcon: '09n', stdIcon: 'nt_chancerain', luxpercent: 0 ],
[ ccode: 'nt_chancesleet', altIcon: '46.png', ctext: 'Chance of Sleet', owmIcon: '13n', stdIcon: 'nt_chancesleet', luxpercent: 0 ],
[ ccode: 'nt_chancesnow', altIcon: '46.png', ctext: 'Chance of Snow', owmIcon: '13n', stdIcon: 'nt_chancesnow', luxpercent: 0 ],
[ ccode: 'nt_chancetstorms', altIcon: '47.png', ctext: 'Chance of Thunderstorms', owmIcon: '11n', stdIcon: 'nt_chancetstorms', luxpercent: 0 ],
[ ccode: 'nt_chancelightsnowwindy', altIcon: '55.png', ctext: 'Possible Light Snow and Windy', owmIcon: '13n', stdIcon: 'nt_chancesnow', luxpercent: 0 ],
[ ccode: 'nt_clear', altIcon: '31.png', ctext: 'Clear', owmIcon: '01n', stdIcon: 'nt_clear', luxpercent: 0 ],
[ ccode: 'nt_cloudy', altIcon: '26.png', ctext: 'Overcast', owmIcon: '04n', stdIcon: 'nt_cloudy', luxpercent: 0 ],
[ ccode: 'nt_drizzle', altIcon: '9.png', ctext: 'Drizzle', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_flurries', altIcon: '13.png', ctext: 'Flurries', owmIcon: '13n', stdIcon: 'nt_flurries', luxpercent: 0 ],
[ ccode: 'nt_fog', altIcon: '22.png', ctext: 'Foggy', owmIcon: '50n', stdIcon: 'nt_fog', luxpercent: 0 ],
[ ccode: 'nt_heavyrain', altIcon: '12.png', ctext: 'Heavy Rain', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_heavyrainbreezy', altIcon: '1.png', ctext: 'Heavy Rain and Breezy', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_heavyrainwindy', altIcon: '1.png', ctext: 'Heavy Rain and Windy', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_lightrain', altIcon: '11.png', ctext: 'Light Rain', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_lightrainbreezy', altIcon: '11.png', ctext: 'Light Rain and Breezy', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_lightrainwindy', altIcon: '11.png', ctext: 'Light Rain and Windy', owmIcon: '09n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_lightsleet', altIcon: '46.png', ctext: 'Sleet', owmIcon: '13n', stdIcon: 'nt_sleet', luxpercent: 0 ],
[ ccode: 'nt_lightsnow', altIcon: '14.png', ctext: 'Light Snow', owmIcon: '13n', stdIcon: 'nt_snow', luxpercent: 0 ],
[ ccode: 'nt_mostlycloudy', altIcon: '27.png', ctext: 'Mostly Cloudy', owmIcon: '04n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
[ ccode: 'nt_partlycloudy', altIcon: '29.png', ctext: 'Partly Cloudy', owmIcon: '03n', stdIcon: 'nt_partlycloudy', luxpercent: 0 ],
[ ccode: 'nt_rain', altIcon: '11.png', ctext: 'Rain', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_rainbreezy', altIcon: '2.png', ctext: 'Rain and Breezy', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_rainwindy', altIcon: '2.png', ctext: 'Rain and Windy', owmIcon: '10n', stdIcon: 'nt_rain', luxpercent: 0 ],
[ ccode: 'nt_sleet', altIcon: '46.png', ctext: 'Sleet', owmIcon: '13n', stdIcon: 'nt_sleet', luxpercent: 0 ],
[ ccode: 'nt_snow', altIcon: '46.png', ctext: 'Snow', owmIcon: '13n', stdIcon: 'nt_snow', luxpercent: 0 ],
[ ccode: 'nt_thunderstorm', altIcon: '0.png', ctext: 'Thunderstorm', owmIcon: '11n', stdIcon: 'nt_clear', luxpercent: 0 ],
[ ccode: 'nt_wind', altIcon: '23.png', ctext: 'Windy', owmIcon: '50n', stdIcon: 'nt_tstorms', luxpercent: 0 ],
[ ccode: 'nt_windfoggy', altIcon: '48.png', ctext: 'Windy and Foggy', owmIcon: '50n', stdIcon: 'nt_partlycloudy', luxpercent: 0 ],
[ ccode: 'nt_windmostlycloudy', altIcon: '50.png', ctext: 'Windy and Mostly Cloudy', owmIcon: '50n', stdIcon: 'nt_fog', luxpercent: 0 ],
[ ccode: 'nt_windovercast', altIcon: '49.png', ctext: 'Windy and Overcast', owmIcon: '50n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
[ ccode: 'nt_windpartlycloudy', altIcon: '52.png', ctext: 'Windy and Partly Cloudy', owmIcon: '50n', stdIcon: 'nt_mostlycloudy', luxpercent: 0 ],
]

@Field static attributesMap = [
	'threedayTile':				[title: 'Three Day Forecast Tile', descr: 'Display Three Day Forecast Tile?', typeof: false, default: 'false'],
	'alert':					[title: 'Weather Alert', descr: 'Display any weather alert?', typeof: false, default: 'false'],
	'betwixt':					[title: 'Slice of Day', descr: 'Display the \'slice-of-day\'?', typeof: 'string', default: 'false'],
	'cloud':			    	[title: 'Cloud', descr: 'Display cloud coverage %?', typeof: 'number', default: 'false'],
	'condition_code':			[title: 'Condition Code', descr: 'Display \'condition_code\'?', typeof: 'string', default: 'false'],
	'condition_icon_only':		[title: 'Condition Icon Only', descr: 'Display \'condition_code_only\'?', typeof: 'string', default: 'false'],
	'condition_icon_url':		[title: 'Condition Icon URL', descr: 'Display \'condition_code_url\'?', typeof: 'string', default: 'false'],
	'condition_icon':			[title: 'Condition Icon', descr: 'Display \'condition_icon\'?', typeof: 'string', default: 'false'],
	'condition_iconWithText':   [title: 'Condition Icon With Text', descr: 'Display \'condition_iconWithText\'?', typeof: 'string', default: 'false'],
	'condition_text':			[title: 'Condition Text', descr: 'Display \'condition_text\'?', typeof: 'string', default: 'false'],
	'country':					[title: 'Country', descr: 'Display \'country\'?', typeof: 'string', default: 'false'],
	'dashHubitatOWM':			[title: 'Dash - Hubitat and OpenWeatherMap', descr: 'Display attributes required by Hubitat and OpenWeatherMap dashboards?', typeof: false, default: 'false'],
	'dashSmartTiles':			[title: 'Dash - SmartTiles', descr: 'Display attributes required by SmartTiles dashboards?', typeof: false, default: 'false'],
	'dashSharpTools':			[title: 'Dash - SharpTools.io', descr: 'Display attributes required by SharpTools.io?', typeof: false, default: 'false'],
	'dewpoint':					[title: 'Dewpoint (in default unit)', descr: 'Display the dewpoint?', typeof: 'number', default: 'false'],
	'dsAttribution':			[title: 'Dark Sky Attribution', descr: 'Display the \'Dark Sky attribution\'?', typeof: false, default: 'false'],
	'fcstHighLow':				[title: 'Forecast High/Low Temperatures:', descr: 'Display forecast High/Low temperatures?', typeof: false, default: 'false'],
	'forecast_code':			[title: 'Forecast Code', descr: 'Display \'forecast_code\'?', typeof: 'string', default: 'false'],
	'forecast_text':			[title: 'Forecast Text', descr: 'Display \'forecast_text\'?', typeof: 'string', default: 'false'],
	'illuminated':				[title: 'Illuminated', descr: 'Display \'illuminated\' (with \'lux\' added for use on a Dashboard)?', typeof: 'string', default: 'false'],
	'is_day':					[title: 'Is daytime', descr: 'Display \'is_day\'?', typeof: 'number', default: 'false'],
	'localSunrise':				[title: 'Local SunRise and SunSet', descr: 'Display the Group of \'Time of Local Sunrise and Sunset\', with and without Dashboard text?', typeof: false, default: 'false'],
	'myTile':					[title: 'myTile for dashboard', descr: 'Display \'myTile\'?', typeof: 'string', default: 'false'],
	'moonPhase':				[title: 'Moon Phase', descr: 'Display \'moonPhase\'?', typeof: 'string', default: 'false'],
    'ozone':    				[title: 'Ozone', descr: 'Display \'ozone\'?', typeof: 'number', default: 'false'],
	'nearestStorm':				[title: 'Nearest Storm Info', descr: 'Display \'nearest storm\' data?', typeof: false, default: 'false'],
	'percentPrecip':			[title: 'Percent Precipitation', descr: 'Display the \'Chance of Rain\', in percent?', typeof: 'number', default: 'false'],
	'solarradiation':			[title: 'Solar Radiation', descr: 'Display \'solarradiation\'?', typeof: 'string', default: 'false'],
	'precipExtended':			[title: 'Precipitation Forecast', descr: 'Display precipitation forecast?', typeof: false, default: 'false'],
	'obspoll':					[title: 'Observation time', descr: 'Display Observation and Poll times?', typeof: false, default: 'false'], 
	'precip_today':				[title: 'Precipitation today (in default unit)', descr: 'Display precipitation today?', typeof: 'number', default: 'false'],
	'state':					[title: 'State', descr: 'Display \'state\'?', typeof: 'string', default: 'false'],
	'vis':						[title: 'Visibility (in default unit)', descr: 'Display visibility distance?', typeof: 'number', default: 'false'],
	'weatherSummary':			[title: 'Weather Summary Message', descr: 'Display the Weather Summary?', typeof: 'string', default: 'false'],
	'wind_cardinal':			[title: 'Wind Cardinal', descr: 'Display the Wind Direction (text initials)?', typeof: 'number', default: 'false'],
	'wind_degree':				[title: 'Wind Degree', descr: 'Display the Wind Direction (number)?', typeof: 'number', default: 'false'],
	'wind_direction':			[title: 'Wind direction', descr: 'Display the Wind Direction?', typeof: 'string', default: 'false'],
	'wind_gust':				[title: 'Wind gust (in default unit)', descr: 'Display the Wind Gust?', typeof: 'number', default: 'false'],
	'wind_string':				[title: 'Wind string', descr: 'Display the wind string?', typeof: 'string', default: 'false'],
]

// Check Version   ***** with great thanks and acknowledgment to Cobra (CobraVmax) for his original code ****
void updateCheck() {
	def paramsUD = [uri: 'https://raw.githubusercontent.com/Scottma61/Hubitat/master/docs/version2.json'] //https://hubitatcommunity.github.io/???/version2.json']
	asynchttpGet('updateCheckHandler', paramsUD)
}

def updateCheckHandler(resp, data) {
	state.InternalName = 'Weather-Display With DarkSky.net Forecast Driver'
	Boolean descTextEnable = settings.logSet ?: false
	if (resp.getStatus() == 200 || resp.getStatus() == 207) {
		def respUD = parseJson(resp.data)
		LOGDEBUG('Version Checking - Response Data: ' + respUD)   // Troubleshooting Debug Code - Uncommenting this line should show the JSON response from your webserver 
		state.Copyright = respUD.copyright
		// uses reformattted 'version2.json' 
		String newVer = padVer(respUD.driver.(state.InternalName).ver)
		String currentVer = padVer(version())               
		state.UpdateInfo = (respUD.driver.(state.InternalName).updated)
		LOGDEBUG('updateCheck: ' + respUD.driver.(state.InternalName).ver + ', ' + state.UpdateInfo + ', ' + respUD.author)
		switch(newVer) {
			case { it == 'NLS'}:
				state.Status = '<b>** This Driver is no longer supported by ' + respUD.author + '  **</b>'
				LOGWARN('** This Driver is no longer supported by ' + respUD.author + ' **')
				break
			case { it > currentVer}:
				state.Status = '<b>New Version Available (Version: ' + respUD.driver.(state.InternalName).ver + ')</b>'
				LOGWARN('** There is a newer version of this Driver available  (Version: ' + respUD.driver.(state.InternalName).ver + ') **')
				LOGWARN('** ' + state.UpdateInfo + ' **')
				break
			case { it < currentVer}:
				state.Status = '<b>You are using a Test version of this Driver (Expecting: ' + respUD.driver.(state.InternalName).ver + ')</b>'
				LOGWARN('You are using a Test version of this Driver (Expecting: ' + respUD.driver.(state.InternalName).ver + ')')
				break
			default:
				state.Status = 'Current Version: ' + respUD.driver.(state.InternalName).ver
				LOGINFO('You are using the current version of this driver')
				break
		}
	} else {
		LOGERR('Something went wrong checking the version. CHECK THE JSON FILE AND IT\'S URI')
	}
}

/*
	padVer
	Version progression of 1.4.9 to 1.4.10 would mis-compare unless each duple is padded first.
*/ 
String padVer(String ver) {
	String pad = ''
	ver.replaceAll( '[vV]', '' ).split( /\./ ).each { pad += it.padLeft( 2, '0' ) }
	return pad
}

String getThisCopyright(){'&copy; 2020 Matthew (scottma61) '}
