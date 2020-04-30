definition(
    name: "Hubigraphs",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubitat Graph Creator Parent App",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "Graph Creator", install: true, uninstall: true,submitOnChange: true) {
        section {
            app(name: "hubiGraphLine", appName: "Hubigraph Line Graph", namespace: "tchoward", title: "Create New Line Graph", multiple: true)
			app(name: "hubiGraphTime", appName: "Hubigraph Time Line", namespace: "tchoward", title: "Create New TimeLine", multiple: true)
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
    // nothing needed here, since the child apps will handle preferences/subscriptions
    // this just logs some messages for demo/information purposes
    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }
}
