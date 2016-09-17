
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
	//log.debug "in private function"
	getCurrentState('lightify-1')
}







def parse5(Map response) {
	def result = []
	if (response.status == 403) {
		// forbidden and either missing cookie or it's expired, get a new one
        log.debug "403"
		result << login()
	} else if (response.status == 400) {
        log.debug "400 - Logged out"
	} else if (response.status == 200) {
		if (response.headers.'Content-Type'.contains("image/jpeg")) {
			def imageBytes = response.data
			if (imageBytes) {
				storeImageInS3(getPictureName(), imageBytes)
			}
		} else if (response.headers.'Content-Type'.contains("application/json")) {
			result << response.body
		}
	}
	log.debug result
	result

}


private getUsername() {
	getDevicePreferenceByName(device, "username")
}
private getPassword() {
	getDevicePreferenceByName(device, "password")
}
private getSerial(){
	getDevicePreferenceByName(device, "serial")	
}





//[MS] Lightify Login
private login(){
	log.debug "Logging in"
	rest(
			method: 'POST',
			endpoint: "https://us.lightify-api.org",
			path: "/lightify/services/session",
			body: [username: getUsername(), password: getPassword(), serialNumber : getSerial()],
			headers: ['Content-Type': 'application/json'],
			//requestContentType: "application/x-www-form-urlencoded",
			synchronous: true
	)
}

//[MS] Trying login different way with physicalgraph
private loginMike(){
	def httpRequest = [
			method: 'POST',
			endpoint: "https://us.lightify-api.org",
			path: "/lightify/services/session",
			body: [username: getUsername(), password: getPassword(), serialNumber : getSerial()],
			headers: ['Content-Type': 'application/json'],
			//requestContentType: "application/x-www-form-urlencoded",
			synchronous: true
    ]
	def loginRequest = new physicalgraph.device.HubAction(httpRequest)
    log.debug loginRequest
	def loginResponse = sendHubCommand(hubAction)
    log.debug loginResponse
    return loginResponse
}

private loginMike2(){
    def params = [
        uri: "https://us.lightify-api.org",
        //endpoint: "https://us.lightify-api.org",
        path: "/lightify/services/session",
        body: [
        	'username': getUsername(),
            'password': getPassword(),
            'serialNumber' : getSerial()
        ],
        headers: [
        	'Content-Type': 'application/json'
		],
        
        contentType: 'application/json',
        requestContentType: 'application/json'
    ]

	log.debug params

	try {
        httpPostJson(params){ resp ->
            resp.headers.each {
	            log.debug "${it.name} : ${it.value}"
	        }
	        log.debug resp
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}
private mikeRespFunction(data){
	log.debug resp
}

private getCurrentState(deviceId){

	//Split deviceId (lightify-1)
	def OsramID = deviceId?.split("-")[1]
	log.debug "Getting Current State of $OsramID"

	log.debug state.auth

	//Custom variables are stored in state. If not defined, must log in.
	if (state.auth == ""){
		//state.auth = "113054-WZfAvktLpkeJhHPinrpa"
        log.debug "no state.auth"
        return login()
	}

    //Defaults to parse function after execution
	rest(
			method: 'GET',
			endpoint: "https://us.lightify-api.org",
			path: "/lightify/services/devices/$OsramID",
			headers: ['Content-Type': 'application/json', 'authorization': state.auth],
			requestContentType: "application/x-www-form-urlencoded",
			synchronous: true
	)
}


//[MS] My parse
def parse (Map response) {

	log.debug "in parse"

	log.debug response
    log.debug response.data.brightnessLevel

	def result = []

	if (response.status == 403) {
		//Server not responding
        log.debug "403 - API not responding"
    	log.debug (response.status)
    } else if ( response.status == 400 ) {
    	//Token invalid or expired
        log.debug "400 - Logged out"
		//result << login()
		result << loginMike2()
		log.debug "400 - after login()"
		log.debug result
        return
	} else if (response.status == 200) {

        log.debug "200"

    	//Avoid loops
        if ( response.data.securityToken ){
        	state.auth = response.data.securityToken
            log.debug "set state.auth"
            log.debug result
        	return
        } else {
	        result << response.data
            
            //Set some device values
            if (response.data.on == "1"){
				sendEvent("name":"switch", "value": "on")
            } else {
				sendEvent("name":"switch", "value": "off")
            }
			sendEvent("name":"level", "value": (response.data.brightnessLevel *100 ))
        }
    
	}
	log.debug result
    
    return result
	
/*
	def msg = parseLanMessage(response)

    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
    def xml = msg.xml                // => any XML included in response body, as a document tree structure
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)

	log.debug (data)
*/

}


private setSwitch(value){
}