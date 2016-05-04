/*
 *  DSC Alarm Panel integration via REST API callbacks
 *
 *  Author: Kent Holloway <drizit@gmail.com>
 *  Modified by: Matt Martz <matt.martz@gmail.com>
 */

definition(
    name: "DSC Integration",
    namespace: "DSC",
    author: "Kent Holloway <drizit@gmail.com>",
    description: "DSC Integration App",
    category: "My Apps",
    iconUrl: "https://dl.dropboxusercontent.com/u/2760581/dscpanel_small.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/2760581/dscpanel_large.png"
//	oauth: true
//	oauth: [displayName: "web services tutorial ", displayLink: "http://localhost"]    
)

//import groovy.json.JsonBuilder

preferences {
  section("Alarm Panel:") {
    input "paneldevices", "capability.polling", title: "Alarm Panel (required)", multiple: false, required: false
  }
  section("Zone Devices:") {
    input "zonedevices", "capability.polling", title: "DSC Zone Devices (required)", multiple: true, required: false
  }
}

mappings {
  path("/panel/:eventcode/:zoneorpart") {
    action: [
      GET: "update"
    ]
  }
}



def update() {
    //def zoneorpartition = params.zoneorpart
    
    log.debug "In Update"
    //log.debug "Zone Part: ${params.zoneorpart}"

    // get our passed in eventcode
    //def eventCode = params.eventcode
    
    return
}


def installed() {
  log.debug "Installed!"
}

def updated() {
  log.debug "Updated!"
}