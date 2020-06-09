metadata{
	definition( name: "Hubigraph Tile Device", namespace: "tchoward", author: "Thomas Howard") {
		// Indicate what capabilities the device should be capable of
		
        capability "Actuator"
        attribute  "Graph", "string"
        attribute  "Graph_No_Title", "string"
        command    "setGraph", ["String"]
    }
	preferences{
	}
}

def setGraph(str) {
    name = device.getDisplayName();
    
    iframe_html = """
    <iframe style="width: 100%; height: 100%;" src="${str}" data-fs="false" onload="(() => {
      const body = this.contentDocument.body;
      const start = () => {
      if(this.dataset.fs == 'false') {
        this.style = 'position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 999;';
        this.dataset.fs = 'true';
      } else {
        this.style = 'width: 100%; height: 100%;';
        this.dataset.fs = 'false';
      }
  }
  body.addEventListener('dblclick', start);
})()"></iframe>
     
"""                          

   def event = createEvent(name: "Graph", value: iframe_html);
   sendEvent(event);
    
    iframe_html = """
    <iframe style="width: 100%; height: 100%;" src="${str}" data-fs="false" onload="(() => {
      const body = this.contentDocument.body;
      const start = () => {
      if(this.dataset.fs == 'false') {
        this.style = 'position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 999;';
        this.dataset.fs = 'true';
      } else {
        this.style = 'width: 100%; height: 100%;';
        this.dataset.fs = 'false';
      }
  }
  
  body.addEventListener('dblclick', start);
  this.parentElement.parentElement.parentElement.querySelector('.tile-title').style='display: none;';
})()"></iframe>
     
"""       
    event = createEvent(name: "Graph_No_Title", value: iframe_html);
    sendEvent(event);
    
}
