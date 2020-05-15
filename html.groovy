metadata{
	definition( name: "Hubigraph Tile Device", namespace: "tchoward", author: "Thomas Howard") {
		// Indicate what capabilities the device should be capable of
		attribute "Graph", "string"
        
        command "setGraph", ["String"]
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
    this.handle = setTimeout(() => {
      if(thisFrame.dataset.fullscreen == 'false') {
        thisFrame.style = 'position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 999;';
        thisFrame.dataset.fullscreen = 'true';
      } else {
        thisFrame.style = 'width: 100%; height: 100%;';
        thisFrame.dataset.fullscreen = 'false';
      }

      this.handle = -1;
    }, 1000);
  }

  const end = () => {
    if(this.handle !== -1) clearTimeout(this.handle);
  }

  body.addEventListener('mousedown', start);
  body.addEventListener('touchstart', start);
  body.addEventListener('mouseup', end);
  body.addEventListener('touchend', end);

})()"></iframe>""");
    sendEvent(event);
}
