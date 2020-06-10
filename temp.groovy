google.charts.load('current', {packages: ['corechart', 'bar']});
google.charts.setOnLoadCallback(drawBasic);

function drawBasic() {

      var data = google.visualization.arrayToDataTable([
        ['Device', 'Min', { role: "style" }, 'Low', { role: "style" },'Value', { role: "style" }, {role: "annotation"}, 'High', { role: "style" }, 'Max', { role: "style" }],
        ['Bedroom Temp', 72, '#0000FF', 6, '#555500', 1, '00FF00', '72 degrees', 1, '#555500', 20, '0000FF'],
        ['Ouside', 56, '#0000FF', 16, '#555500', 1, '00FF00', '72 degrees', 14, '#555500', 13, '0000FF'],
      ]);

      var options = {
        title: 'Home Temperatures',
        chartArea: {width: '80%'},
        bar: {groupWidth: "80%"},
        isStacked: true,
        legend: { position: "none" },
        annotations: { 
        			textStyle: {
      					fontSize: 18,
      					bold: true,
      					italic: true,
      					// The color of the text.
      					color: '#000000',
      					// The color of the text outline.
      					auraColor: 'transparent',
				      // The transparency of the text.
      					opacity: 1.8
    					},
        			highContrast: false }
      };

      var chart = new google.visualization.BarChart(document.getElementById('chart_div'));

      chart.draw(data, options);
    }
