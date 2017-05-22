/**
 *  Light Progression
 *
 *  Copyright 2017 Mike Shelton
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
    name: "Light Progression",
    namespace: "michaelshelton",
    author: "Mike Shelton",
    description: "This changes lighting as time progresses.",
    category: "Family",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name:"initPage", title:"Setup", nextPage:"sceneSettings", uninstall: true) {
        section("Select Lights") {
            input "colorLights", "capability.colorControl", title: "Lights", required:true, multiple:true
            input(name: "sceneCount", type:"number", title: "Number of Steps", required:true, submitOnChange: true)
			input "modes", "mode", title: "only when mode is", multiple: true, required: false
			input "timeOfDay", "time", title: "At a Scheduled Time", required: false
			input "oncePerDay", "bool", title: "Only once per day", required: false, defaultValue: false
        }
    }

	page(name: "sceneSettings", title: "Settings.", install: true, uninstall: true)
}

//Define Main Page
def sceneSettings() {
	dynamicPage(name: "sceneSettings", title:"Light Settings") {
    	if (sceneCount > 0) {

			//Iterate through each scene and set parameters
			(1..sceneCount).each { sceneKey ->
			        //paragraph "Scene interation - ${sceneKey}"
                    section("Light ${sceneKey}") {
                        input "color${sceneKey}", "enum", title: "Color", required: false, multiple:false, options: [
                            ["Soft White":"Soft White - Default"],
                            ["White":"White - Concentrate"],
                            ["Daylight":"Daylight - Energize"],
                            ["Warm White":"Warm White - Relax"],
                            "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
                        input "lightLevel${sceneKey}", "enum", title: "Level", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
                        input "timeDuration${sceneKey}", "number", title: "Duration of change in minutes", description: "Minutes", required: false
						paragraph " ------ "
                    }
				}
                        
        } else {
	        section("Scene") {
		        paragraph "Scene count is set to zero."
	        }
        }

	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.

	//Subscribe to events
	if (timeOfDay) {
		schedule(timeOfDay, scheduledTimeHandler)
	}
}




// TODO: implement event handlers
def scheduledTimeHandler() {
	log.trace "scheduledTimeHandler()"
	eventHandler()
}


//TODO: Verify everything in place first
private getAllOk() {
	//modeOk && daysOk && timeOk
    return true
}

private oncePerDayOk(Long lastTime) {
	def result = lastTime ? dayString(new Date()) != dayString(new Date(lastTime)) : true
	log.trace "oncePerDayOk = $result - $lastTime"
	result
}

def eventHandler(evt=null) {
	log.trace "Executing Lighting Change"
	if (allOk) {
		log.trace "allOk"
		//def lastTime = state[frequencyKey(evt)]
		//if (oncePerDayOk(lastTime)) {
		//	if (frequency) {
		//		if (lastTime == null || now() - lastTime >= frequency * 60000) {
		//			takeAction(evt)
		//		}
		//	} else {
				takeAction(evt)
		//	}
		//} else {
		//	log.debug "Not taking action because it was already taken today"
		//}
	}
}



//Execute Lighting Change
private takeAction(evt) {

	log.debug "takeAction( ${evt} )"

	def hueColor = 0
	def hueSaturation = 100

	//TODO: hard coding for now, but should be the variable
	def lightPercentage = lightLevel1 as Integer

	def hex = "#ffffff"

	//TODO: Which step are we on in the loop?
    def color = color1

	switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
            hex = "#ffffff"
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
            hex = "#E3FFFF"
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
            hex = "#FFF8C2"
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80 //83
            hex = "#FFEF78"
			break;
		case "Blue":
			hueColor = 70
            hex = "#0000FF"
			break;
		case "Green":
			hueColor = 39
            hex = "#008000"
			break;
		case "Yellow":
			hueColor = 25
            hex = "#FFFF00"
			break;
		case "Orange":
			hueColor = 10
            hex = "#FFA500"
			break;
		case "Purple":
			hueColor = 75
            hex = "#800080"
			break;
		case "Pink":
			hueColor = 83
            hex = "#FFC0CB"
			break;
		case "Red":
			hueColor = 100
            hex = "#FF0000"
			break;
	}
    

    //Turn on first via Level
	if (lightPercentage){
		colorLights*.setLevel(lightPercentage)
    }

 	colorLights*.setColor( [hue: hueColor, saturation: hueSaturation, hex: hex, transitionTime: 1] )
    log.debug "Changed"


	//TODO: This shouldn't go here.
	//if ( newValueTimeDuration ){
	//	//Set to 20 seconds, but for minute will be 60 seconds
	//	runIn(20 * newValueTimeDuration, returnPreviousState)
	//}
    
}