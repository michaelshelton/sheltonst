metadata {
	// This is a device handler for Lightify Connect parent app.
	definition (name: "LightifyBulb", namespace: "SheltonST", author: "Mike Shelton", oauth: true) {
        capability "Actuator"
        capability "Switch"
		capability "Switch Level"
		capability "Configuration"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Color Control"
        capability "Color Temperature"

		//Custom tile actions must be defined here
        command "customTileCommand"

	}


	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"
	}


	preferences {
		input "fade", "number", title: "Transition Time", description: "1/10 Second (1-80)", range: "1..80"
	}
    
    tiles(scale: 1) {

		multiAttributeTile(name:"switchMulti", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"color control.setColor"
			}
		}
        
        
    	standardTile("refresh", "device.switch", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}


		//Color Specific
		controlTile("rgbSelector", "device.color", "color", height: 2, width: 2, inactiveLabel: false) {
			state "color", action: "color control.setColor"
		}
		valueTile("currentColor", "device.color", height: 2, width: 2) {
			state "color", label: 'Color: ${currentValue}', defaultState: true, backgroundColor: '${currentValue}'
		}


        controlTile("tempSliderControl", "device.colorTemperature", "slider", height: 2, width: 4, inactiveLabel: false, range:"(1500..6500)") {
			state "colorTemperature", action:"color temperature.setColorTemperature"
		}
	    valueTile("colorTemp", "device.colorTemperature", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
    	    state "colorTemperature", label: 'Color Temp'
    	}



        //Not in Use (added in details array)
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label: 'on', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#79b821"
			state "off", label: 'off', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#ffffff"
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
        




		main(["switchMulti"])

		//TODO: Add more from OSRAM device handler template
		details(["switchMulti", "refresh", "rgbSelector", "currentColor", "tempSliderControl", "colorTemp"])
	}
}




def refresh(){
	parent.refresh(this)
}

def on() {
	parent.on(this, fade)
}

def off() {
	parent.off(this, fade)
}

def setLevel( level ){
	parent.setLevel (this, level, fade)
}


def setColor( colorSetValue ){
	
    //[MS] Tried to force a quick change from other apps
    //def fadeTime = getTransitionTime(colorSetValue)
    //log.debug "Custom Fade: ${fadeTime}"
	
	log.debug "child setColor called: ${colorSetValue}"
	parent.setColor (this, colorSetValue, fade)
}

def setColorTemperature( tempSetValue ){
	parent.setColorTemperature (this, tempSetValue, fade)    
}

def customTileCommand(value) {
}


def installed() {
	log.debug "Lightify Device Install Function"
}



//General Functions
// ------------------------------------

private getTransitionTime(SetValue){

    def fadeTime
    if ( SetValue.transitionTime ){
    	fadeTime = SetValue.transitionTime    	
	} else {
    	fadeTime = fade    	
    }
    return fadeTime

}