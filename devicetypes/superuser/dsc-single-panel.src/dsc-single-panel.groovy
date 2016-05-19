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
	capability "Button"

    // Add commands as needed
    command "partition"
    command "setPartitionState"
    
      attribute "partition1", "string"
      attribute "alarmStatus", "string"
      attribute "alarmstate", "string"
  }

  simulator {
    // Nothing here, you could put some testing stuff here if you like
  }

  //Icons here - http://scripts.3dgo.net/smartthings/icons/
  tiles {
    standardTile("dscpartition", "device.dscpartition", width: 2, height: 2, canChangeBackground: false, canChangeIcon: true) {
      state "armed",     label: 'Armed',      backgroundColor: "#3BE00D", icon:"st.Home.home3"
      state "exitdelay", label: 'Exit Delay', backgroundColor: "#ff9900", icon:"st.Home.home3"
      state "entrydelay",label: 'Entry Delay', backgroundColor: "#ff9900", icon:"st.Home.home3"
      state "notready",  label: 'Open',       backgroundColor: "#ffcc00", icon:"st.Home.home2"
      state "ready",     label: 'Ready',      backgroundColor: "#0DA4E0", icon:"st.Home.home2"
      state "alarm",     label: 'Alarm',      backgroundColor: "#ff0000", icon:"st.Home.home3"
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

    log.debug "Partition: ${state} for partition: ${partition}"
    sendEvent (name: "dscpartition", value: "${state}")
}

//[MS]
def setPartitionState( String state ) {
    sendEvent ( name: "dscpartition", value: state )
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