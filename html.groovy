/*HTML Device for use with Hubigraphs*/

metadata{
	definition( name: "HTML Device", namespace: "tchoward", author: "Thomas Howard") {
		// Indicate what capabilities the device should be capable of
		attribute "html", "string"
        
        command "setHTML", ["String"]
    }
	preferences{
	}
}

def setHTML(str) {
    def event = createEvent(name: "html", value: """<iframe style="width: 100%; height: 100%;" src="${str}"></iframe>""");
    sendEvent(event);
}
