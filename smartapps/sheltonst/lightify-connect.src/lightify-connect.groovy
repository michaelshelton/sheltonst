/**
 *  Lightify Connect
 *
 *  Copyright 2016 Mike Shelton
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Lightify Connect",
    namespace: "SheltonST",
    author: "Mike Shelton",
    description: "Osram Lightify app to connect lights.",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    singleInstance: true
    )


preferences {
	section("Lightify Details") {
		// TODO: put inputs here
	//    page(name: "Credentials", title: "Lightify Authentication", content: "authPage", nextPage: "sampleLoggedInPage", install: false)

		input "username", "text", title: "Username", description: "Lightify Email", value: "mike@sheltonstudios.net", required: true
		input "password", "password", title: "Password", description: "Lightify Password", value: "yOp&GarTe#m3Cxcp", required: true
		input "serial", "text", title: "Serial", description: "Gateway Serial Number", value: "OSR027CAE67", required: true		
		input "fade", "number", title: "Transition Time", description: "1/10 Second (1-80)", range: "1..80", value: "30"
	}

}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Update Settings: ${settings}"
	unsubscribe()
    
    //Should this do something special on update?
	initialize()
    log.debug "updated"
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    log.debug "initialized"    
    log.debug "Initialize Settings = ${settings}"

	//MikeFunction()
    //return


    //Get AuthToken now?
    getSecurityToken()

	//here???
	getLightifyDevices()
}


//Test some things outside the normal functions
def MikeFunction(){
	//Crack at getting ChildDevices, which WORKS
	def AppId = app.id
   	log.debug app.id
	def deviceId = "lightify-11"
    log.debug(deviceId)
    def existing = getChildDevice(deviceId)
    
	//Wrap in try function
    try {
		if (!existing) {
                
	    	log.debug "none existing, so create"
	        def cDevice = addChildDevice("SheltonST", "LightifyBulb", "lightify-11", location.hubs[0].id, [
                	name: "Lightify Test",
                    label: "Lightify Test",
                    on: "on",
                    level: "100",
                    switch: "on",
                    color: "FF3000",
                    completedSetup: true
                ])

            log.debug "New = ${cDevice.displayName}"

            //Set device current states 


		} else {
	    	log.debug "yes existing, so remove"
		
	       def CurrentDevices = getChildDevices()
	        //log.debug CurrentDevices
	        CurrentDevices.each {
		        log.debug it.deviceNetworkId
	            deleteChildDevice(it.deviceNetworkId)
        	}
	    }
    } catch (e) {
        log.error "Error creating device: ${e}"
    }
	return
}


//Create initial page for authentication
def authPage() {

    log.debug "authPage"
	
    // Check to see if our SmartApp has it's own access token and create one if not.
    if(!state.accessToken) {
        // the createAccessToken() method will store the access token in state.accessToken
        getSecurityToken()
    }

    def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${getApiServerUrl()}"
    // Check to see if we already have an access token from the 3rd party service.
    if(!state.accessToken) {
        return dynamicPage(name: "auth", title: "Login", nextPage: "", uninstall: false) {
            section() {
                paragraph "tap below to log in to the 3rd party service and authorize SmartThings access"
                href url: redirectUrl, style: "embedded", required: true, title: "3rd Party product", description: "Click to enter credentials"
            }
        }
    } else {
        // We have the token, so we can just call the 3rd party service to list our devices and select one to install.
    }
}


//API login function to obtain/renew token
def getSecurityToken(){

    log.debug "getSecurityToken"

	def params = [
			method: 'POST',
			uri: "https://us.lightify-api.org",
			path: "/lightify/services/session",
			body: [
				'username': username,
                'password': password,
                'serialNumber': serial
            ],
			headers: [
            	'Content-Type': 'application/json',
                'Accept': 'application/json'
            ],
			synchronous: true
    ]

	try {
        httpPostJson(params) { resp ->
			if (resp.status == 403) {
                log.debug "Login API Call: 403 - API not responding"
            } else if ( resp.status == 200 ){
                log.debug "Login Response: ${resp.data}"

                //Store securityToken
                state.accessToken = resp.data.securityToken
            }

	        if (state.accessToken) {
	            // call some method that will render the successfully connected message
	            success()
	        } else {
	            // gracefully handle failures
	            fail()
	        }


		}
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}

// Example success method
def success() {
		log.debug "success - ${state.accessToken}"
        //def message = """
        //        <p>Your account is now connected to SmartThings!</p>
        //        <p>Click 'Done' to finish setup.</p>
        //"""
        //displayMessageAsHtml(message)
}

// Example fail method
def fail() {
	log.debug "fail"
    //def message = """
    //    <p>There was an error connecting your account with SmartThings</p>
    //    <p>Please try again.</p>
    //"""
    //displayMessageAsHtml(message)
}


def displayMessageAsHtml(message) {
    def html = """
        <!DOCTYPE html>
        <html>
            <head>
            </head>
            <body>
                <div>
                    ${message}
                </div>
            </body>
        </html>
    """
    render contentType: 'text/html', data: html
}




//Generic API Get Request
private def LightifyGetRequest (ApiPath, ApiQuery){
	
	log.debug "ApiQuery: ${ApiQuery}"
	def params = [
			method: 'GET',
			uri: "https://us.lightify-api.org",
            path: ApiPath,
            query: ApiQuery,
			headers: [
            	'Content-Type': 'application/json',
                'authorization': state.accessToken
            ],
			synchronous: true
    ]
    //log.debug params
	try {
        httpGet(params) { resp ->        
            return resp
        }
    } catch (e) {
    	//Lightify throws a 400 when "Invalid Security Token", so let's login
		log.debug "LightifyGetRequest = 400. ${e}"
        
        //Login
        getSecurityToken()
        
        //Retry original call
        LightifyGetRequest(ApiPath, ApiQuery)
    }
}



//Get Lightify Devices
def getLightifyDevices(){

	log.debug "getLightifyDevices"

    def ApiPath = "/lightify/services/devices"
    def ApiQuery = []
	def response = LightifyGetRequest (ApiPath, ApiQuery)    

	//log.debug location.hubs[0].id
    //log.debug "response data: ${response.data}"


	//Update device state.
	if (response.status == 200){

		//how to get the count? Still broken... tried -- count, length, size
		//log.debug "Total Items: " response.data.size()

		//Get Lightify Devices
    	response.data.each {device->
	       	//log.debug "${device.deviceId} - ${device.name}"
            def stDeviceId = "lightify-${device.deviceId}"
            def stDevice = state.devices.find{it.id==stDeviceId}
            
            //log.debug stDeviceId


            //Only add new devices
            def existing = getChildDevice(stDeviceId)
            if (existing) {
                //log.debug "does exist - ${stDeviceId}"
                return
            } else {
                //log.debug "does not exist, so create -  ${stDeviceId}"

					def cDevice = addChildDevice("SheltonST", "LightifyBulb", "lightify-${device.deviceId}", location.hubs[0].id, [
                        name: "Lightify ${device.name}",
                        label: device.name,
                        completedSetup: true
                    ])

                //log.debug "New = ${cDevice.displayName}"

                //Set device current states 
            }
		}
	}
    
    return
}







//Event Functions passed up from child
def on(childDevice, deviceFade){
    def deviceId = getDeviceId(childDevice)
    def fadeTime = getFadeDuration(deviceFade)

	//log.debug childDevice.device.displayName
	//log.debug childDevice.device.settings

    //Use generic API handler
    def ApiPath = "/lightify/services/device/set"
    def ApiQuery = [
			'idx': deviceId,
			'onoff': 1,
            "time": fadeTime
    ]

	def response = LightifyGetRequest (ApiPath, ApiQuery)

	if (response.status == 200){
		childDevice.sendEvent("name":"switch", "value": "on")
    }
    return
    
    


    //lights.each {
    //    // check to ensure the switch does have the setLevel command
    //    if (it.hasCommand('setLevel')) {
    //        log.debug("Not So Smart Lighting: $it.displayName setLevel($level)")
    //        it.setLevel(level as Integer)
    //    }
    //    it.on()
    //}

    //childDevice.device.each {
    //  //resp << [name: it.displayName, value: it.currentValue("switch")]
    //  log.debug it.name
    //  log.debug it.value
    //}
}


def off(childDevice, deviceFade){
	def deviceId = getDeviceId(childDevice)
    def fadeTime = getFadeDuration(deviceFade)


    //Use generic API handler
    def ApiPath = "/lightify/services/device/set"
    def ApiQuery = [
			'idx': deviceId,
			'onoff': 0,
            "time": fadeTime            
    ]

	def response = LightifyGetRequest (ApiPath, ApiQuery)

	if (response.status == 200){
		childDevice.sendEvent("name":"switch", "value": "off")
    }
    return
}


def refresh(childDevice){
    def deviceId = getDeviceId(childDevice)
    
    //Use generic API handler
    def ApiPath = "/lightify/services/devices/${deviceId}"
    def ApiQuery = []
	def response = LightifyGetRequest (ApiPath, ApiQuery)

	//Update device states.
	if (response.status == 200){
        log.debug "response data: ${response.data}"


        //Set some device values
		//childDevice.sendEvent(name: "switch", value: response.data.on.valueOf("1") ? "on" : "off")
        if (response.data.on == 1){
            childDevice.sendEvent("name":"switch", "value": "on")
	        childDevice.sendEvent("name":"level", "value": (response.data.brightnessLevel *100 ))
        } else {
			childDevice.sendEvent("name":"switch", "value": "off")
	        childDevice.sendEvent("name":"level", "value": 0)
        }
        childDevice.sendEvent("name": "color", "value": "#${response.data.color}")
		childDevice.sendEvent("name": "colorTemperature", "value": response.data.temperature)
		childDevice.sendEvent("name": "hue", "value": response.data.hue)
		childDevice.sendEvent("name": "saturation", "value": response.data.saturation)
    }
}



//setLevel(number, number)
def setLevel( childDevice, level, deviceFade ){
    def deviceId = getDeviceId(childDevice)
    def LightifyLevel = level/100
    def fadeTime = getFadeDuration(deviceFade)


    //Use generic API handler
    def ApiPath = "/lightify/services/device/set"
    def ApiQuery = [
			'idx': deviceId,
            'level': LightifyLevel,
            "time": fadeTime            
    ]

	def response = LightifyGetRequest (ApiPath, ApiQuery)

	if (response.status == 200){
		childDevice.sendEvent("name":"switch", "value": "on")
		childDevice.sendEvent("name":"level", "value": level)
    }
    return
}

def setColor(childDevice, colorValue, deviceFade){
    def deviceId = getDeviceId(childDevice)
    def fadeTime = getFadeDuration(deviceFade)

    //log.debug "New Color: ${colorValue}"

    //Use generic API handler
    def ApiPath = "/lightify/services/device/set"
	def ApiQuery

	if (colorValue.hex) {
    
        //Should be a better way to strip first character, but this works. Lightify doesn't want the # in the API call.
        def colorHexParts = colorValue.hex.split("#")
        def colorHex = colorHexParts[1]
        //log.debug colorHex

        ApiQuery = [
                'idx': deviceId,
                'color': colorHex,
                "time": fadeTime            
        ]
    	if (colorValue.hex) { childDevice.sendEvent(name: "color", value: colorValue.hex) }
    } else if (colorValue.hue) {
        ApiQuery = [
                'idx': deviceId,
                'hue': colorValue.hue,
                'saturation': colorValue.saturation/100,
                'time': fadeTime            
        ]
    	if (colorValue.hue) { childDevice.sendEvent(name: "hue", value: colorValue.hue) }
    	if (colorValue.saturation) { childDevice.sendEvent(name: "saturation", value: colorValue.saturation) }
    } else {
    	log.debug "No working color data sent"
    	return
    }

	def response = LightifyGetRequest (ApiPath, ApiQuery)

	if (response.status == 200){
	}
    return


	////From Child Awake App
	//def newValue = [hue: hueColor, saturation: hueSaturation, level: lightLevel as Integer ?: 100]
	//hues*.setColor(newValue)
}

def setColorTemperature (childDevice, tempValue, deviceFade){
    def deviceId = getDeviceId(childDevice)
    def fadeTime = getFadeDuration(deviceFade)

	//Use generic API handler
    def ApiPath = "/lightify/services/device/set"
    def ApiQuery = [
			"idx": deviceId,
            "time": fadeTime,
            "ctemp": tempValue
    ]

	def response = LightifyGetRequest (ApiPath, ApiQuery)

	if (response.status == 200){
		childDevice.sendEvent("name": "colorTemperature", "value": tempValue)
    }
    return
	
}



//SmartApp uninstall. Remove children.
def uninstalled (){
        def CurrentDevices = getChildDevices()
        CurrentDevices.each {
	        //log.debug it.deviceNetworkId
            deleteChildDevice(it.deviceNetworkId)
        }
}



//Generic Functions
//-------------------------------------------

//Get Osram light ID = split deviceNetworkId (lightify-X)
private getDeviceId (childDevice){
	def deviceIdParts = childDevice.device.deviceNetworkId.split("-")
    def deviceId = deviceIdParts[1]
    return deviceId
}

//Get fade time from device first, and then default to parent
private getFadeDuration (deviceFade){
	def fadeTime = 0
	if (deviceFade){
        fadeTime = deviceFade
    } else {
        fadeTime = fade
    }
    return fadeTime
}