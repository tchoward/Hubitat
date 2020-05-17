metadata{
	definition( name: "Hubigraph Tile Device", namespace: "tchoward", author: "Thomas Howard") {
		// Indicate what capabilities the device should be capable of
		
        capability "Actuator"
        attribute  "Graph", "string"        
        command    "setGraph", ["String"]
    }
	preferences{
	}
}

def setGraph(str) {
    def event = createEvent(name: "Graph", value: """<iframe style="width: 100%; height: 100%;" src="${str}" data-fullscreen="false" onload="(() => {
  this.handel = -1;

  const thisFrame = this;
  const body = thisFrame.contentDocument.body;
  const start = () => {
      if(thisFrame.dataset.fullscreen == 'false') {
        thisFrame.style = 'position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 999;';
        thisFrame.dataset.fullscreen = 'true';
      } else {
        thisFrame.style = 'width: 100%; height: 100%;';
        thisFrame.dataset.fullscreen = 'false';
      }
  }

  body.addEventListener('dblclick', start);

})()"></iframe>""");
    sendEvent(event);
}
