/*
 *  DSC Zone Device
 *
 *  Author: Matt Martz <matt.martz@gmail.com>
 *  Date: 2014-04-28
 */

// for the UI
metadata {
  definition (name: "DSC Zone", author: "matt.martz@gmail.com") {
    // Change or define capabilities here as needed
    capability "Refresh"
    capability "Contact Sensor"
    capability "Sensor"
    capability "Polling"

    // Add commands as needed
    command "zone"
  }

  simulator {
    // Nothing here, you could put some testing stuff here if you like
  }

  tiles (scale: 1) {
    // Main Row
    standardTile("zone", "device.contact", width: 3, height: 2, canChangeBackground: true, canChangeIcon: true) {
      state "open",   label: '${name}', icon: "st.contact.contact.open",   backgroundColor: "#ED9C59"
      state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#47BE47"
      state "alarm",  label: '${name}', icon: "st.contact.contact.open",   backgroundColor: "#AB1818"
    }

    // This tile will be the tile that is displayed on the Hub page.
    main "zone"

    // These tiles will be displayed when clicked on the device, in the order listed here.
    details(["zone"])
  }
}

// handle commands
def zone(String contactState) {
	// state will be a valid state for a zone (open, closed)
	// zone will be a number for the zone
	log.debug "Zone: ${contactState}"
	sendEvent( name: "contact", value: "${contactState}", descriptionText: contactState, displayed: true, isStateChange: true )
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