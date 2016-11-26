/*
 *  DSC Panel
 *
 *  Author: Kent Holloway <drizit@gmail.com>
 *  Date: 2014-03-20
 */

// for the UI
metadata {
  // Automatically generated. Make future change here.
  definition (name: "DSC Single Panel", author: "Kent Holloway <drizit@gmail.com>") {
    // Change or define capabilities here as needed
	capability "Alarm"
    capability "Polling"
    capability "Refresh"
	//capability "Button"

    // Add commands as needed
    command "partition"
    command "setPartitionState"
    
      attribute "alarmStatus", "string"
      attribute "alarmstate", "string"
  }

  simulator {
    // Nothing here, you could put some testing stuff here if you like
  }

  //Icons here - http://scripts.3dgo.net/smartthings/icons/
  tiles (scale: 1) {
    standardTile("dscpartition", "device.dscpartition", width: 3, height: 2, canChangeBackground: false, canChangeIcon: false) {
      state "armed",		label: 'Armed',      	backgroundColor: "#47BE47", icon:"st.Home.home3"
      state "exitdelay",	label: 'Exit Delay', 	backgroundColor: "#ED9C59", icon:"st.Home.home3"
      state "entrydelay",	label: 'Entry Delay',	backgroundColor: "#ED9C59", icon:"st.Home.home3"
      state "notready",  	label: 'Open',       	backgroundColor: "#FFD2AE", icon:"st.Home.home2"
      state "ready",     	label: 'Ready',      	backgroundColor: "#358E8E", icon:"st.Home.home2"
      state "disarmed",    	label: 'Ready',      	backgroundColor: "#358E8E", icon:"st.Home.home2"
      state "alarm",     	label: 'Alarm',      	backgroundColor: "#AB1818", icon:"st.Home.home3"
    }

    main "dscpartition"

    // These tiles will be displayed when clicked on the device, in the order listed here.
    details(["dscpartition"])
  }
}

// parse events into attributes
def parse(String description) {
  // log.debug "Parsing '${description}'"
  def myValues = description.tokenize()

  log.debug "Event Parse function: ${description}"
  sendEvent (name: "${myValues[0]}", value: "${myValues[1]}")
}

def partition(String state, String partition) {
    // state will be a valid state for the panel (ready, notready, armed, etc)
    // partition will be a partition number, for most users this will always be 1

	//Deprecated?
    sendEvent (name: "dscpartition", value: "${state}")
}

//[MS]
def setPartitionState( String partitionState ) {
    //sendEvent( name: "dscpartition", value: state )
    sendEvent( name: "dscpartition", value: "${partitionState}", descriptionText: partitionState, displayed: true, isStateChange: true )
}


def poll() {
  log.debug "Executing 'poll'"
  // TODO: handle 'poll' command
  // On poll what should we do? nothing for now..
}

def refresh() {
  log.debug "Executing 'refresh' which is actually poll()"
  poll()
  // TODO: handle 'refresh' command
}