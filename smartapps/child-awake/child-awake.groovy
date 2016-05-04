/**
 *  Landon Awake
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
    name: "Child Awake",
    namespace: "michaelshelton",
    author: "Mike Shelton",
    description: "Do something when a child is up from a nap or sleep.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "mainPage", title: "Adjust the color of your Hue lights on motion.", install: true, uninstall: true)
	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}
}

def mainPage() {
	dynamicPage(name: "mainPage") {
		def anythingSet = anythingSet()
		if (anythingSet) {
			section("Change lighting when..."){
				ifSet "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
				ifSet "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
				ifSet "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
				ifSet "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
				ifSet "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
			}
		}
		section(anythingSet ? "Select additional mood lighting triggers" : "Change lighting when...", hideable: anythingSet, hidden: true){
			ifUnset "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
			ifUnset "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
			ifUnset "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
			ifUnset "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
			ifUnset "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
		}
		section("Control these bulbs...") {
			input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
		}
		section("Choose light effects...") {
			input "color", "enum", title: "Hue Color?", required: false, multiple:false, options: [
					["Soft White":"Soft White - Default"],
					["White":"White - Concentrate"],
					["Daylight":"Daylight - Energize"],
					["Warm White":"Warm White - Relax"],
					"Red","Green","Blue","Yellow","Orange","Purple","Pink"]
			input "lightLevel", "enum", title: "Light Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
		}

		section("More options", hideable: true, hidden: true) {
			input "frequency", "decimal", title: "Minimum time between actions (defaults to every event)", description: "Minutes", required: false
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            input "activeSwitch", "capability.switch", title: "Only when switch on", required: false, defaultValue: false
            input "OpenDoorContact", "capability.contactSensor", title: "When contacts are open", required: false, defaultValue: false, multiple: true
            input "ClosedDoorContact", "capability.contactSensor", title: "When contacts are closed", required: false, defaultValue: false, multiple: true
			input "newValueTimeDuration", "number", title: "Duration of change in minutes", description: "Minutes", required: false
		}
		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}
private anythingSet() {
	for (name in ["motion","contact","contactClosed","mySwitch","mySwitchOff"]) {
		if (settings[name]) {
			return true
		}
	}
	return false
}

private ifUnset(Map options, String name, String capability) {
	if (!settings[name]) {
		input(options, name, capability)
	}
}

private ifSet(Map options, String name, String capability) {
	if (settings[name]) {
		input(options, name, capability)
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(app, appTouchHandler)
	subscribe(contact, "contact.open", eventHandler)
	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
	subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)

	if (triggerModes) {
		subscribe(location, modeChangeHandler)
	}
}

def eventHandler(evt=null) {
	log.trace "Executing Child Lighting"
	if (allOk) {
		log.trace "allOk"
		def lastTime = state[frequencyKey(evt)]
        
        //[MS] Check to see if chosen switch is on
        if (activeSwitch){
			log.trace "activeSwitch if statement"
        }
        
    	//[MS] check contact sensors
    	if (contactSensorsCheck() == false){
    		return
    	}

		takeAction(evt)
	}
}

def modeChangeHandler(evt) {
	log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
	if (evt.value in triggerModes) {
		eventHandler(evt)
	}
}



def appTouchHandler(evt) {
	takeAction(evt)
}

private takeAction(evt) {

	if (frequency || oncePerDay) {
		state[frequencyKey(evt)] = now()
	}
    


	def hueColor = 0
	def saturation = 100

	switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80 //83
			break;
		case "Blue":
			hueColor = 70
			break;
		case "Green":
			hueColor = 39
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 75
			break;
		case "Pink":
			hueColor = 83
			break;
		case "Red":
			hueColor = 100
			break;
	}

    //Get current state of light
	state.previous = [:]

	//[MS] try new each loop
	def int loopItem = 0
	hues.each {
		state.previous[loopItem] = [
			"id": it.id,
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation")
		]
        loopItem ++
	}
	//log.debug "current value of 0 = "+ $state.previous[0]




	def newValue = [hue: hueColor, saturation: saturation, level: lightLevel as Integer ?: 100]
	log.debug "new value = $newValue"

	hues*.setColor(newValue)
    
	//Change back?
    if ( newValueTimeDuration ){
        runIn(60 * newValueTimeDuration, returnPreviousState)
    }

	//Mike Testing ------------------------
	state.previous.each { id ->
//    	log.debug "light ID: "+ id
        //log.debug "hue="+ id['hue']
//        log.debug "prop: "+ state.previous[id]
	}
//    log.debug "full prop: "+ state.previous
//    for (int i = 0; i < state.previous.size(); i++) {
//    	log.debug "light ID: "+ i
        //log.debug "hue="+ id['hue']
//        log.debug "prop: "+ state.previous.i
//    }
	// end Mike Testing ------------------------


}


//[MS] Check to see if contacts are set per preferences
private contactSensorsCheck(){
	def result = true
    ClosedDoorContact.each {contactSensor ->
		def DoorStatus = contactSensor.currentState("contact").value
        if ( DoorStatus != "closed" ){
			result = false
            //log.debug "Closed: "+ DoorStatus
        }
	}

    OpenDoorContact.each {contactSensor ->
		def DoorStatus = contactSensor.currentState("contact").value
        if ( DoorStatus != "open" ){
			result = false
            //log.debug "Open: $DoorStatus"
        }
	}

   	if ( result ){
    	log.debug "proceed. doors right"
    } else {
    	log.debug "cancel. doors not right"
    }
    return result
}


private frequencyKey(evt) {
	"lastActionTimeStamp"
}

private dayString(Date date) {
	def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
	if (location.timeZone) {
		df.setTimeZone(location.timeZone)
	}
	else {
		df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
	}
	df.format(date)
}


private oncePerDayOk(Long lastTime) {
	def result = lastTime ? dayString(new Date()) != dayString(new Date(lastTime)) : true
	log.trace "oncePerDayOk = $result - $lastTime"
	result
}

// TODO - centralize somehow
private getAllOk() {
	modeOk && daysOk && timeOk
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() {
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting, location?.timeZone).time
		def stop = timeToday(ending, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private timeIntervalLabel()
{
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}


//[MS] Change lights to previous state
def returnPreviousState( ) {
	log.debug "return lights to previous state here. Old State = $state.previous"

	state.previous.each {
    	//log.debug "key-- "+ it.key
    	//log.debug "it-- "+ it.value
    	//log.debug "it sw-- "+ it.value.switch

//[MS] so close, but see error*
//		if (it.value.switch == "off"){
//        	 it.value.id.off()
//             log.debug "turn this switch back off"
//		}
		//it.value.id.setColor( [hue: it.value.hue, saturation: it.value.saturation, level: it.value.level as Integer ?: 100] )

//error*
//861fd9e0-3666-4373-bcf2-34120ca9f25f 11:35:43 PM: error groovy.lang.MissingMethodException: No signature of method: java.lang.String.setcolor() is applicable for argument types: (java.util.LinkedHashMap) values: [[hue:75, saturation:100, level:100]]
//861fd9e0-3666-4373-bcf2-34120ca9f25f 11:46:02 PM: error groovy.lang.MissingMethodException: No signature of method: java.lang.String.setColor() is applicable for argument types: (java.util.LinkedHashMap) values: [[hue:75, saturation:100, level:100]]

	}
    
	//Works, but sets them all. What is hues*
    //hues*.setColor([hue: 30, saturation: 100, level: 100 as Integer ?: 100])
    
}
