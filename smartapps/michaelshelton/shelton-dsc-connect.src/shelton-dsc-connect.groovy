/**
 *  DSC EyezOn Listener
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
    name: "Shelton DSC Connect",
    namespace: "michaelshelton",
    author: "Mike Shelton",
    description: "DSC alarm panel and zones integration. This allows updating of the partition and zone contacts with Envizalink.",
    category: "",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home3-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home3-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home3-icn@3x.png",
    oauth: [displayName: "DSC Alarm ", displayLink: "http://localhost:4567"])


preferences {

    page(name: "selectDevices", title: "Choose Devices", nextPage: "selectActions", uninstall: true) {
        section ("Allow external service to control these things...") {
            input "switches", "capability.switch", multiple: true, required: false
        }
        section("Alarm Panel:") {
            input "paneldevices", "capability.polling", title: "Alarm Panel", multiple: false, required: true
        }
        section("Zone Contacts:") {
        	//[MS] Should this be limited to capability.contactSensor instead of capability.polling
            input "zonedevices", "capability.contactSensor", title: "DSC Zone Devices", multiple: true, required: true
        }
    }

    page(name: "selectActions", nextPage: "notificationPrefs")

    page(name: "notificationPrefs", title: "Notifications", install: true, uninstall: true) {
        section("Notifications (optional):") {
            input "phone1", "phone", title: "Phone Number", required: false
            input "notifyAlarm", "enum", title: "Notify When Alarming?", required: false,
                metadata: [
                    values: ["Yes","No"]
                ]
            input "notifyArmed", "enum", title: "Notify When Armed?", required: false,
                metadata: [
                    values: ["Yes","No"]
                ]
            input "notifyDisarmed", "enum", title: "Notify When Disarmed?", required: false,
                metadata: [
                    values: ["Yes","No"]
                ]
        }
	}


}

def selectActions() {
    dynamicPage(name: "selectActions", title: "Routines") {
        //Get the available actions/routines
        def actions = location.helloHome?.getPhrases()*.label
        if (actions) {

            //Sort alphabetically
            actions.sort()

			section() {
                // use the actions as the options for an enum input
                input "armedRoutine", "enum", title: "When alarm armed:", options: actions, required: false
                input "alarmRoutine", "enum", title: "When alarm triggered:", options: actions, required: false
                input "disarmRoutine", "enum", title: "When alarm disarmed:", options: actions, required: false
            }
        }
    }
}

mappings {
  path("/switches") {
    action: [
      GET: "listSwitches"
    ]
  }
  path("/panels") {
    action: [
      GET: "listPanels"
    ]
  }
  path("/zones") {
    action: [
      GET: "listZones"
    ]
  }
  path("/switches/:command") {
    action: [
      PUT: "updateSwitches"
    ]
  }
  path("/panel/:eventcode/:zoneorpart") {
    action: [
      GET: "updateZoneOrPartition"
    ]
  }
}

// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {

    def status
    def contentType
    def data
    def headers = [:]

    def resp = []
    switches.each {
        resp << [name: it.displayName, value: it.currentValue("switch")]
	}

	resp << [Settings: settings]
	return resp
}

def listPanels(){
    def resp = []

    resp << [panels: paneldevices]
    resp << [name: paneldevices.displayName]

	resp << [dscpartition: paneldevices.currentValue("dscpartition") ]
	resp << [partition1: paneldevices.currentValue("partition1") ]
	resp << [alarmStatus: paneldevices.currentValue("alarmStatus") ]
	resp << [alarmState: paneldevices.currentValue("alarmState") ]

	resp << [Settings: settings]


	//[MS] Capabilities Test
    def mySwitchCaps = paneldevices.capabilities
    mySwitchCaps.each {cap ->
      resp << ["cap name": cap.name]
    }

    mySwitchCaps.commands.each {
      resp << ["command name": it.name ]
    }
    
    
    //[MS] test update partition state
	paneldevices.setPartitionState( "armed" )
  	resp << [dscpartitionNew: paneldevices.currentValue("dscpartition") ]

    return resp
}

def listZones(){
    def status
    def contentType
    def data
    def headers = [:]
    
    def resp = []

	zonedevices.each {
        resp << [name: it.displayName, value: it.currentValue("contact")]
    }

		//[MS] Capabilities Test
        def mySwitchCaps = zonedevices[0].capabilities
        mySwitchCaps.each {cap ->
            //log.debug "Capability name: ${cap.name}"
            resp << ["cap name": cap.name]
        }

        mySwitchCaps.commands.each {
            resp << ["command name": it.name ]
        }

	resp << [Settings: settings]
	return resp
}

void updateSwitches() {
    // use the built-in request object to get the command parameter
    def command = params.command

    if (command) {

        // check that the switch supports the specified command
        // If not, return an error using httpError, providing a HTTP status code.
        switches.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            } 
        }
        
        // all switches have the comand
        // execute the command on all switches
        // (note we can do this on the array - the command will be invoked on every element
        switches."$command"()
    }
}


def updateZoneOrPartition() {

    //Initialize Response Array
    def resp = []

    //Variables defined from URL
    def zoneorpartition = params.zoneorpart
    def eventCode = params.eventcode

    // Each event maps to a command in your "DSC Panel" device type
    def eventMap = [
      '601':"zone alarm",
      '602':"zone closed",
      '609':"zone open",
      '610':"zone closed",
      '631':"zone smoke",
      '632':"zone clear",
      '650':"partition ready",
      '651':"partition notready",
      '652':"partition armed",
      '654':"partition alarm",
      '655':"partition disarmed",
      '656':"partition exitdelay",
      '657':"partition entrydelay",
      '701':"partition armed",
      '702':"partition armed"
    ]

    //Figure out device from code
    def opts = eventMap."${eventCode}"?.tokenize()


	//[MS] Debugging
    resp << [zone: zoneorpartition, code: eventCode, engPart: opts[0], engCode: opts[1] ]

    if ( opts[0] == "zone") {
		//Update a zone
        resp << updateZoneDevices( zonedevices, zoneorpartition, "${opts[1]}" )
    }
    
    if ("${opts[0]}" == 'partition') {
		//Update the partition panel
        resp << updatePartitions(paneldevices, zoneorpartition, "${opts[1]}")
    }

	return resp
}


private updateZoneDevices( zonedevices, zonenum, zonestatus ) {
    //Initialize Response Array
    def response = []

	//[MS] Debug
    //response << [devices: zonedevices, num: zonenum, status: zonestatus]

	if (zonenum && zonestatus) {

		//Get ST ID from Device Network ID
		def zonedevice = zonedevices.find{ it.deviceNetworkId == "zone"+ zonenum }

		//Get Current Value
		//response << [ThisDevice: zonedevice.currentValue("contact")]

		//Change Zone to new value if different than current setting
        if ( zonedevice.currentValue("contact") != zonestatus ){

			//[MS] probably should update the device handler as this is confusing
			zonedevice.zone( zonestatus )
			response << [result: zonestatus]
	            
        } else {
			response << [result: "not changed" ]
        }
        
    }
    return response
}


private updatePartitions( paneldevices, partitionnum, partitionstatus ) {
    //Initialize Response Array
    def resp = []

	if (partitionnum && partitionstatus) {
        //log.debug "paneldevices: $paneldevices - ${partitionnum} is ${partitionstatus}"

		//Get ST ID from Device Network ID (zoneX)
		def paneldevice = paneldevices.find { it.deviceNetworkId == "partition"+ partitionnum }
        resp << [CurrentStatus: paneldevice.currentValue("dscpartition") ]

		//Get Current Value
        //log.debug "Currently: "+ partitionnum.currentValue("device.dscpartition")


		//Set Panel to new value
//        if (paneldevice) {
//        	paneldevice.partition("${partitionstatus}", "${partitionnum}")
//        }

        //[MS] using new command
        if ( paneldevice ){
        
			//Did it change?
            if (paneldevice.currentValue("dscpartition") != partitionstatus ){
				paneldevices.setPartitionState( partitionstatus )

                //Run Routines if configured
                if (partitionstatus == "alarm") {

                    //Trigger Routine
                    location.helloHome?.execute( settings.alarmRoutine )

                    //sendMessage("Alarm")
                    if ( notifyArmed =="Yes" ){
                        sendPush( "Alarm Triggered" )
                    }
                } else if (partitionstatus == "armed"){

                    //Trigger Routine
                    location.helloHome?.execute( settings.armedRoutine )

                    if ( notifyAlarm =="Yes" ){
                        sendPush( "Alarm Armed" )
                    }
                    //sendMessage("Armed")
                } else if (partitionstatus == "disarmed"){

                    //Trigger Routine
                    location.helloHome?.execute( settings.disarmRoutine )

                    if ( notifyDisarmed == "Yes" ){
                        sendPush( "Alarm Disarmed" )
                    }
                    //sendMessage("Disarmed")
                }



			}
        }


		resp << [Settings: settings]

		//resp << [Status: partitionstatus]

	}
	return resp
}


//Function to trigger notifications
private sendMessage(msg) {
    def newMsg = "Alarm Notification: $msg"
    if (phone1) {
        sendSms(phone1, newMsg)
    }
    if (sendNotification == "Yes") {
        sendPush(newMsg)
    }
}


def installed() {}

def updated() {}