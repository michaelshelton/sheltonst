
metadata {
	// Automatically generated. Make future change here.
	definition (name: "Lightify", namespace: "SheltonST", author: "Mike Shelton", oauth: true) {
        capability "Color Temperature"
        capability "Actuator"
        capability "Switch"
		capability "Switch Level"
		capability "Configuration"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Color Control"

		//Custom tile actions must be defined here
		command "getLightifyStatus"

	}


	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"
	}


	preferences {
		input "username", "text", title: "Username", description: "Lightify Email", required: true
		input "password", "password", title: "Password", description: "Lightify Password", required: true
		input "serial", "text", title: "Serial", description: "Gateway Serial Number", required: true		
		input "uuid", "text", title: "Dropcam Device ID", description: "Legacy Dropcam Code"
		input "fade", "20", title: "Fade seconds", description: "Transition time in milliseconds."
	}
    
    tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label: 'on', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821"
			state "off", label: 'off', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff"
		}
        
		//Initiate API poll against Lightify
    	standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"getLightifyStatus", icon:"st.secondary.refresh"
		}
        
        controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false, range:"(0..100)") {
			state "level", action:"switch level.setLevel"
		}
		valueTile("on", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "switch", label: '${currentValue}'
		}
		valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
			state "level", label: 'Level ${currentValue}'
		}

		main(["switch"])

		//TODO: Add more from OSRAM device handler template
		details(["switch", "refresh", "levelSliderControl", "on", "level"])
	}
}



//[MS]
def getLightifyStatus(){    

    //Separate the calls out to make it easier
    // 1. Login
    MikeLogin3()
    ////log.debug "post MikeLogin3"
    ////log.debug "Token still available? -- ${state.auth}"
    
    // 2. Get light details
	getCurrentState('lightify-5')
    
}



//Something seems broken with these. Had to hard code the values for logging in to work.
private getUsername() {
	return getDevicePreferenceByName(device, "username")
}
private getPassword() {
	return getDevicePreferenceByName(device, "password")
}
private getSerial(){
	return getDevicePreferenceByName(device, "serial")	
}





//[MS] Lightify Login
private login(){
}


def MikeLogin3(){
	log.debug "MikeLogin3"
    
    
    log.debug getUsername()
    log.debug getPassword()
    log.debug getSerial()
    
    
	def params = [
			method: 'POST',
			uri: "https://us.lightify-api.org",
			path: "/lightify/services/session",
			body: [
				//'username': getUsername(),
                //'password': getPassword(),
                //'serialNumber': getSerial()
                'username': 'mike@sheltonstudios.net',
                'password': 'yOp&GarTe#m3Cxcp',
                'serialNumber': 'OSR027CAE67'
            ],
			headers: [
            	'Content-Type': 'application/json',
                'Accept': 'application/json'
            ]
			//synchronous: true
    ]

	try {
        httpPostJson(params) { resp ->

			//log.debug "in resp"

			if (resp.status == 403) {
                log.debug "Login API Call: 403 - API not responding"
            } else if ( resp.status == 200 ){
                //resp.headers.each {
                //    log.debug "${it.name} : ${it.value}"
                //}
                //log.debug "response contentType: ${resp.contentType}"
                log.debug "Refresh Response Data: ${resp.data}"

                //Store securityToken
                state.auth = resp.data.securityToken
            }
			
            return
            
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}



//[MS] Trying login different way with physicalgraph
private loginMike(){
}

private loginMike2(){
}

private mikeRespFunction(data){
	log.debug resp
}

private getCurrentState(deviceId){

	//Split deviceId (lightify-X)
	def OsramID = deviceId?.split("-")[1]
	log.debug "Getting Current State of $OsramID"

  	def params = [
			method: 'GET',
			uri: "https://us.lightify-api.org",
			path: "/lightify/services/devices/${OsramID}",
			headers: [
            	'Content-Type': 'application/json',
                'authorization': state.auth
            ]
    ]

	try {
        httpGet(params) { resp ->
			if (resp.status == 403) {
                log.debug "Get API Call: 403 - API not responding"
            } else if ( resp.status == 200 ){
                log.debug "response data: ${resp.data}"
                log.debug "Color? ${resp.data.color}"

				state.deviceStatus = resp.data

                //Set some device values
                if (resp.data.on == 1){
                    sendEvent("name":"switch", "value": "on")
                } else {
                    sendEvent("name":"switch", "value": "off")
                }
                sendEvent("name":"level", "value": (resp.data.brightnessLevel *100 ))
			}
			
            return
            
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}


//[MS] My parse
def parse (Map response) {
}


private setSwitch(value){
}


def on(){
	//Get Device
	def deviceIdParts = device.deviceNetworkId.split("-")
    def deviceId = deviceIdParts[1]
	//log.debug deviceId
    turnOnOff( deviceId, 1 )
}

def off(){
	//Get Device
	def deviceIdParts = device.deviceNetworkId.split("-")
    def deviceId = deviceIdParts[1]
	//log.debug deviceId
    turnOnOff( deviceId, 0 )
}

//Lightify API call to turn on/off
//Passing the level is overriden when passing onoff. It needs to be a separate call.
private turnOnOff( deviceId, onoff ){
	def params = [
			method: 'GET',
			uri: "https://us.lightify-api.org",
			path: "/lightify/services/device/set",
			query: [
            	'idx': deviceId,
                'onoff': onoff,
                'time': getDevicePreferenceByName(device, "fade")
            ],
			headers: [
            	'Content-Type': 'application/json',
                'authorization': state.auth
            ]
			//synchronous: true
    ]

	try {
        httpGet(params) { resp ->
			if (resp.status == 403) {
                log.debug "Login API Call: 403 - API not responding"
            } else if ( resp.status == 200 ){
				//Need to update device object state
                state.deviceStatus.on = onoff
            }
			
            
            return            
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }	
}


//setLevel(number, number)


def setLevel( level ){

	//log.debug level
    
    def LightifyLevel = level/100

	//Get Device
	def deviceIdParts = device.deviceNetworkId.split("-")
    def deviceId = deviceIdParts[1]

	def params = [
			method: 'GET',
			uri: "https://us.lightify-api.org",
			path: "/lightify/services/device/set",
			query: [
            	'idx': deviceId,
                'level': LightifyLevel,
                'time': getDevicePreferenceByName(device, "fade")
            ],
			headers: [
            	'Content-Type': 'application/json',
                'authorization': state.auth
            ]
			//synchronous: true
    ]

	try {
        httpGet(params) { resp ->
			if (resp.status == 403) {
                log.debug "Login API Call: 403 - API not responding"
            } else if ( resp.status == 200 ){
                //log.debug "response data: ${resp.data}"
            }
			
            //Need to update device object state
			state.deviceStatus.brightnessLevel = level
            
            //???? How to set it on the device property?
            //levelSliderControl.level = level
            
            return            
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }	
}