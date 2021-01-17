import groovy.json.*;
import org.codehaus.groovy.runtime.metaclass.*;

def NAME = "Denon AVR Master Device";
def NAME_SPACE = "Denon AVR";
def TYPE = "Main";


metadata {
    
    definition(name: NAME, namespace: NAME_SPACE, author: "Thomas Howard") {        
        capability "Refresh"
		capability "Actuator"
          
        deviceSetup(TYPE); 
        if (device) {
            updateDataValue("type", TYPE);
        }
    }
    
    preferences {
		input ("deviceIp", "text", title: "Denon AVR IP Address");
        getChildrenDevices(TYPE);
        getOverrides(TYPE, "Input");
    }    
}

def getOverrides(type, command_){
    deviceCommands = getDeviceCommands();
    
    deviceCommands."${type}"."${command_}".commands.each{name_, input_->
       var_name = "${command_}_${name_}_override";
       input (var_name, "text", title: "Override Display Name for ${command_}: <b>${input_.name}</b>", defaultValue: "${input_.name}");       
    }
}

def getChildrenDevices(type){
    deviceCommands = getDeviceCommands();                
    
    deviceCommands.each{ func, device_ -> 
        if (device_ instanceof Map){
            if (device_.child_device){
                input (device_.child_var, "bool", title: "Enable ${device_.child_type}", defaultValue: device_.child_var); 
            }
        }
    }
}

def deviceSetup(device_name){
        
    command "executeCommand",["command"]

    deviceCommands = getDeviceCommands();   
    
    log.debug("Device Setup");

    deviceCommands."$device_name".each{ func, choices -> 
        if (choices instanceof Map){
            def val = false;


            //if a capability is defined, add it
            if (choices.capability){
                capability "${choices.capability}"
            } else {

                def list = choices.commands.collect{ 
                    key, element ->    

                    if (element instanceof Map) {
                        if (element.val == true) val = true;
                  
                        return element.name;
                    }
                    else return null;        
                }.findAll { e ->
                    return (boolean) e;
                }
                
                log.debug(list);
                
                if (val){ 
                    params = [
                        [name: "${func}",
                         description: "Set the ${device} ${func}",
                         constraints: list,
                         type: "ENUM"
                        ],
                        [name: "value",
                         description: "Set the ${device} ${func}",
                         type: "NUMBER"   
                        ]
                    ];
                } else {
                    params = [
                        [name: "${func}",
                         method: "blah",
                         description: "Set the ${device} ${func}",
                         constraints: list,
                         type: "ENUM"
                        ]
                    ];
                }

                //Set the command
                command "Set${func}", params

                attribute "${func}", "string"

            }

        }
    }
}

def getDeviceCommands(){
    
    command_refresh = [
        refresh:     [command: "",     name: "Refresh", val: false, delay: 1, capability: "Refresh", capability_func: "refresh", capability_value: "refresh"],
    ]
    
    command_level = [
        auto:     [command: "AUTO",     name: "Auto",     val: false, delay: 1],
        low:      [command: "LOW",      name: "Low",      val: false, delay: 1],
        mid:      [command: "MID",      name: "Mid",      val: false, delay: 1],
        high:     [command: "HIGH",     name: "High",     val: false, delay: 1],
        off:      [command: "OFF",      name: "Off",      val: false, delay: 1],
    ];
    
    command_on_standby = [ 
        on:     [command: "ON",      name: "On",      val: false, delay: 10, capability: "Switch", capability_func: "on", capability_value: "on"],
        off:    [command: "STANDBY", name: "Off",     val: false, delay: 1,  capability: "Switch", capability_func: "off", capability_value: "off"],
    ];

    command_on_off =     [
        on:     [command: "ON",      name: "On",       val: false, delay: 1, capability: "Switch", capability_func: "on", capability_value: "on"],
        off:    [command: "OFF",     name: "Off",      val: false, delay: 1, capability: "Switch", capability_func: "off", capability_value: "off"],   
    ];
    
    command_mute =     [
        on:     [command: "ON",      name: "On",       val: false, delay: 1, capability: "AudioVolume", capability_func: "mute", capability_value: "muted"],
        off:    [command: "OFF",     name: "Off",      val: false, delay: 1, capability: "AudioVolume", capability_func: "unmute", capability_value: "unmuted"],   
    ];
    
    command_volume =   [
        up:      [command: "UP",      name: "Up",       val: false, delay: 1, capability: "AudioVolume", capability_func: "volumeUp", capability_value: "volume"],
        down:    [command: "DOWN",    name: "Down",     val: false, delay: 1, capability: "AudioVolume", capability_func: "volumeDown", capability_value: "volume"],
        set:     [command: "",        name: "Set",      val: true,  delay: 1, capability: "AudioVolume", capability_func: "setVolume", capability_value: "volume", capability_vars: 1],
    ];

    command_up_down = [
        up:      [command: "UP",      name: "Up",       val: false, delay: 1],
        down:    [command: "DOWN",    name: "Down",     val: false, delay: 1],
        set:     [command: "",        name: "Set",      val: true,  delay: 1],
    ];

    command_input = [
        tuner:   [command: "TUNER",   name: "Tuner",           val: false, delay: 1],
        dvd:     [command: "DVD",     name: "DVD",             val: false, delay: 1], 
        blueray: [command: "BD",      name: "BlueRay",         val: false, delay: 1],
        tv:      [command: "TV",      name: "TV",              val: false, delay: 1],
        sat_cbl: [command: "SAT/CBL", name: "Satellite/Cable", val: false, delay: 1],
        mplay:   [command: "MPLAY",   name: "Media Player",    val: false, delay: 1],
        game:    [command: "GAME",    name: "Game",            val: false, delay: 1],
        aux:     [command: "AUX1",    name: "Auxilary",        val: false, delay: 1],
        network: [command: "NET",     name: "Network",         val: false, delay: 1],
    ];
    
    video_select = [
        off:     [command: "OFF",     name: "Off",             val: false, delay: 1],
        dvd:     [command: "DVD",     name: "DVD",             val: false, delay: 1], 
        blueray: [command: "BD",      name: "BlueRay",         val: false, delay: 1],
        tv:      [command: "TV",      name: "TV",              val: false, delay: 1],
        sat_cbl: [command: "SAT/CBL", name: "Satellite/Cable", val: false, delay: 1],
        mplay:   [command: "MPLAY",   name: "Media Player",    val: false, delay: 1],
        game:    [command: "GAME",    name: "Game",            val: false, delay: 1],
        vaux:    [command: "V.AUX",   name: "Auxilary",        val: false, delay: 1],
        network: [command: "NET",     name: "Network",         val: false, delay: 1],
        dock:    [command: "DOCK",    name: "Dock",            val: false, delay: 1],
        source:  [command: "SOURCE",  name: "Source",          val: false, delay: 1],

    ];

    command_mode = [
        movie:        [command: "MOVIE",         name: "Movie",                val: false, delay: 1],
        music:        [command: "MUSIC",         name: "Music",                val: false, delay: 1], 
        game:         [command: "GAME",          name: "Game",                 val: false, delay: 1],
        direct:       [command: "DIRECT",        name: "Direct",               val: false, delay: 1],
        pure_direct:  [command: "PURE DIRECT",   name: "Pure Direct",          val: false, delay: 1],
        stereo:       [command: "STEREO",        name: "Stereo",               val: false, delay: 1],
        standard:     [command: "STANDARD",      name: "Standard",             val: false, delay: 1],
        dolby_digital:[command: "DOLBY DIGITAL", name: "Dolby Digital",        val: false, delay: 1],
        dts_surround: [command: "DTS SUROUND",   name: "DTS Surround Sound",   val: false, delay: 1],
        mch_stero:    [command: "MCH STEREO",    name: "Multi-Channel Stereo", val: false, delay: 1],
    ]; 
    
    command_audyssey = [
        audyssey:     [command: "AUDYSSEY",         name: "On",                         val: false, delay: 1],   
        byp_lr:       [command: "BYP.LR",           name: "Bypass",                     val: false, delay: 1],
        flat:         [command: "FLAT",             name: "Flat",                       val: false, delay: 1],
        manual:       [command: "MANUAL",           name: "Manual",                     val: false, delay: 1],
        off:          [command: "OFF",              name: "Off",                        val: false, delay: 1],
    ];
    
    requirements_tone = [
        [var: "Tone",         val: "On"],
        [var: "AudioMode",    not: "Direct"],
        [var: "AudioMode",    not: "Pure Direct"]
    ];

    deviceCommands = [ 
        Main: 
        [ 
            name: "Main",
            Power:  [
                name: "Power", prefix: "PW", status: true,
                commands: command_on_standby,
                capability: "Switch", capability_var: "switch"
            ],

            Volume: [
                name: "Volume", prefix: "MV", status: true,
                commands: command_volume,
                capability: "AudioVolume", capability_var: "volume"
            ],
            
            Mute:  [   
                name: "Mute", prefix: "MU", status: true,
                commands: command_mute,
                capability: "AudioVolume", capability_var: "mute"
            ],

            Input:  [ 
                name: "Input", prefix: "SI", status: true,
                commands: command_input
            ],

            AudioMode: [ 
                name: "Audio Mode", prefix: "MS", status: true,
                commands: command_mode
            ],
            Select: [
                name: "Video Select", prefix: "SV", status: true,
                commands: video_select
            ],  
            Tone: [
                name: "Tone Control", prefix: "PSTONE CTRL ", status: true,
                commands: command_on_off
            ],
            Bass: [
                name: "Bass", prefix: "PSBAS ", status: true,
                commands: command_up_down,
                requirements: requirements_tone
            ],
            Treble: [
                name: "Treble", prefix: "PSTRE ", status: true,
                commands: command_up_down,
                requirements: requirements_tone
            ],
            LowFrequencyEffect: [
                name: "Low Frequency Effect", prefix: "PSLFE ", status: true,
                commands: command_up_down
            ],
            DirectChange: [
                name: "Direct Change", prefix: "PSDRC ", status: true,
                commands: command_level  
            ],
            AudysseyMode: [
                name: "Audyssey", prefix: "PSMULTEQ:", status: true,
                commands: command_audyssey
            ],
            MainZone: [
                name: "Main Zone", prefix: "ZM", status: true,
                commands: command_on_off
            ],
            DynamicEqualizer: [
               name: "Dynamic Equalizer", prefix: "PSDYNEQ ", status: true,
               commands: command_on_off,
            ],
        ],                                   
        Zone2: [ 
            name: "Zone 2", child_device: true, child_type: "Denon AVR Zone 2 Child Device", child_namespace: "Denon AVR", child_var: "zone2",
            Power:  [
                name: "Power", prefix: "Z2", status: true,
                commands: command_on_off,
                capability: "Switch", capability_var: "switch"

            ],
            Input:  [ 
                name: "Input", prefix: "Z2", status: false,
                commands: command_input
            ],
            Volume: [
                name: "Volume", prefix: "Z2", status: true,
                commands: command_volume,
                capability: "AudioVolume", capability_var: "volume"
            ],
            
            Mute:  [   
                name: "Mute", prefix: "Z2MU", status: true,
                commands: command_mute,
                capability: "AudioVolume", capability_var: "mute"
            ],
        ],
        Speakers: [
            name: "Speakers", child_device: true, child_type: "Denon AVR Speakers Child Device", child_namespace: "Denon AVR", child_var: "speakers",
            
            Front_Left:             [name: "Front Left Speaker",          prefix: "CVFL ",  status: false, commands: command_up_down],
            Front_Right:            [name: "Front Right Speaker",         prefix: "CVFR ",  status: false, commands: command_up_down],
            Center:                 [name: "Center Speaker",              prefix: "CVC ",   status: false, commands: command_up_down],
            Subwoofer:              [name: "Subwoofer",                   prefix: "CVSW ",  status: false, commands: command_up_down],
            Surround_Left:          [name: "Surround Left Speaker",       prefix: "CVSL ",  status: false, commands: command_up_down],
            Surround_Right:         [name: "Surround Right Speaker",      prefix: "CVSR ",  status: false, commands: command_up_down],
            Surround_Left_Back:     [name: "Surround Left Back Speaker",  prefix: "CVSBL ", status: false, commands: command_up_down],
            Surround_Right_Back:    [name: "Surround Right Back Speaker", prefix: "CVSBR ", status: false, commands: command_up_down],
        ],
    ]; 
    return deviceCommands;
}

def getString(val){
 
    if (val<10) return "0"+val.toString();
    else return val.toString();
}



def getFunction (name, args, type) {
    
    if (!device) return null
            
    deviceCommands = getDeviceCommands(); 
    def prefix_ = null;
    def suffix_ = null;
    def value_ = null;
    //Set command
    if (name.matches("Set(.*)")){
        name = name-"Set";
        function = deviceCommands."${type}"."${name}";                
        if (function){
            try {
                command = function.commands.find{key, val->
                    args[0] == val.name
                }.value;
            } catch (e) {
                 return null;   
            }
            
            if (command){
                prefix_ = function.prefix;
                suffix_ = command.command;
                if (command.val){
                    value_= getString(args[1]);
                } else {
                    value_= "";    
                }
            } else return null;    
                
            
        } else return null; 
        
    //Now deal with capability functions
    } else {
        
        try{
            deviceCommands."${type}".each{ function, variables->
                if (variables instanceof Map) {
                    if (variables.capability){
                        variables.commands.each {command, vars->
                            if (vars.capability_func == name){
                                prefix_ = variables.prefix;
                                suffix_ = vars.command;
                                
                                //Is there an expected value? 
                                if (vars.val){
                                    value_= getString(args[0]);
                                } else {
                                    value_= "";    
                                }
                
                                throw new Exception("Found Answer");
                            }
                        }   
                    }
                }
            }
        } catch (e) {
            //Do nothing here
        }
        if (prefix_ == null) return null;
    }
    
    //Execute the command
    executeCommand("${prefix_}${suffix_}${value_}");
    
    //Return Nothing.
    return {};
    
}


def methodMissing(String name, Object args) {
    
    if (device)
        type = device.getDataValue("type");
    
    function = getFunction(name, args, type);
    if (function != null) {
        return function;
    } else {
        //super
        def method = this.class.metaClass.pickMethod("invokeMethod", [String, Object] as Class[]);
        return method.invoke(this, name, args);
    }
}

def setupChildDevices(){
    
    deviceCommands = getDeviceCommands();
    
    deviceCommands.each{ func, device_ -> 
        if (device_ instanceof Map){
            if (device_.child_device && settings["${device_.child_var}"]==true){
                def childNetworkID = "AVR_${func}_CHILD";
                
                child = getChildDevice(childNetworkID);
                
                if (!child){
                    child = addChildDevice(device_.child_namespace, device_.child_type, childNetworkID, [name: "${device.getLabel()} ${device_.name}", type: "${func}"])
                }

                
            }
        }
    }
   /* 
    def zone2DeviceId = "AVR_ZONE2_ID";
        
    //Setup Zone 2 Child Device
    if (zone2child == true){
        child = getChildDevice(zone2DeviceId);
        if (!child){
            child = addChildDevice("Denon AVR", "AVR Zone 2 Child Device", "AVR_ZONE2_ID", [name: "Denon Zone 2", type: "Zone2"])
        }
        child.updated();
    }
    */
}

def setupInputs(){
    
}
    
def updated(){
    log.debug("Initializing Device to IP = $deviceIp");
    init(deviceIp, "23");
    
    setupChildDevices();
    setupInputs();
}

def initialized(){
    log.debug("Initialized"); 
    updated();
}

def init(ip, port) {
	disconnect();
    log.debug("Connecting to ${ip}@${port}");
	connect(ip, port);
}

def close() {
	disconnect()
}

def connect(ip, port) {
	telnetConnect([termChars:[13]], ip, port.toInteger(), null, null)	
}


def disconnect() {
	telnetClose()
}

//commands
def executeCommand(command) {
    log.debug("Sending command: $command");
    command = command;
	sendHubCommand(new hubitat.device.HubAction(command, hubitat.device.Protocol.TELNET))
}

def refresh(){
    
	deviceCommands = getDeviceCommands();
    
    devName = device.getDataValue("type");
    
    //Setup the Class Methods
    deviceCommands."${devName}".each{ func, choices -> 
        if (choices instanceof Map){
            if (choices.status){
                executeCommand("${choices.prefix}?");
            }
        }
    }
	
}
def getNumbers(input){
    return input.findAll("[0-9][0-9]")*.toInteger()
}

def parseResponse(resp){

    data = device.getData();
    
    parseResponseDev(this, data.type, resp);
    
    deviceCommands = getDeviceCommands();                
    
    deviceCommands.each{ func, device_ -> 
        if (device_ instanceof Map){
            if (device_.child_device){
                def childNetworkID = "AVR_${func}_CHILD";
                
                child = getChildDevice(childNetworkID);
               
                if (child) parseResponseDev(child, func, resp);
            }    
        }
    }
}

def parseResponseDev(dev, name, resp){
    
    deviceCommands = getDeviceCommands();
     
    deviceCommands."$name".each{ func, choices -> 
        
        if (choices instanceof Map) {
            if (resp.matches("${choices.prefix}(.*)")){
                suffix = resp - choices.prefix;
                choices.commands.each{cmd, data->
                    num = getNumbers(suffix);   
                    if (num!=[] && data.val){
                        num = num[0];
                        if (num<10) suffix = suffix-("0"+num.toString());
                        else suffix = suffix-num.toString();

                        if (suffix.length() > 0 && suffix[suffix.length()-1]==" "){
                            suffix = suffix.substring(0, suffix.length()-1);    
                        }
                    } else {
                        num = null;    
                    }
                    if (suffix.matches("${data.command}")){
                        if (num != null) {
                            
                            if (choices.capability && data.capability && choices.capability && data.capability && choices.capability_var)
                                dev.sendEvent(name: choices.capability_var, value: num);
                            else
                                dev.sendEvent(name: "${func}", value: num, description: "${data.name} recieved from AVR");
                       
                        } else {
                            
                            if (choices.capability && data.capability && choices.capability && data.capability && choices.capability_var)
                                dev.sendEvent(name: choices.capability_var, value: data.capability_value);
                            else {
                                if (settings["${func}_${cmd}_override"] != null && settings["${func}_${cmd}_override"] != data.name)
                                    name = settings["${func}_${cmd}_override"]+" (${data.name})";
                                else 
                                    name = data.name;
                                
                                dev.sendEvent(name: "${func}", value: name, description: "${data.name}::${name} recieved from AVR");
               
                            }
                        }

                    }

                }
            }
        }
    }
}


def parse(command) {
	log.debug("Got Response: ${command}");
	commandReceived = true;
    parseResponse(command);
    
    return null;
}

def refreshTimeout(){
	log.debug("commandReceived = ${commandReceived}");
}

