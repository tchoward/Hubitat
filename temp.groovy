google.charts.load('current', {packages: ['corechart', 'bar']});
google.charts.setOnLoadCallback(drawBasic);

function drawBasic() {

      var data = google.visualization.arrayToDataTable([
        ['Device', 'Min', 	{ role: "style" }, {role: "tooltip"}, {role: "annotation"}, 
        					 'Low', 	{ role: "style" }, {role: "tooltip"}, {role: "annotation"}, 
                   'Value', { role: "style" }, {role: "tooltip"}, {role: "annotation"}, 
                   'High', 	{ role: "style" }, {role: "tooltip"}, {role: "annotation"},  
                   'Max', 	{ role: "style" }, {role: "tooltip"}, {role: "annotation"}
        ],
        ['Bedroom Temp', 72, 'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475', '1',	'',
        									6, 'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91', '2',	'',
                          1, 'stroke-color: #527535; stroke-opacity: 0.8; stroke-width: 2; color: #8eb6d4', '3',	'3', 
                          1, 'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91', '4',	'',
                          20,'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475', '5',	'',
        ],
        ['Ouside', 				68, 'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475', 	'6',	'',
        									12, 'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91',	'7',	'',
                          1, 	'stroke-color: #527535; stroke-opacity: 0.8; stroke-width: 2; color: #8eb6d4', 	'8',	'8', 
                          14, 'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91', 	'9',	'',
                          5,	'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475',  '10',	'',
        ],
        ['Freezer', 			28, 'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475',	'11',	'', 
        									2, 	'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91',	'12',	'',
                          1, 	'stroke-color: #527535; stroke-opacity: 0.8; stroke-width: 2; color: #8eb6d4',	'13', '13', 
                          3, 	'stroke-color: #607c91; stroke-opacity: 0.8; stroke-width: 2; color: #607c91',	'14',	'',
                         66,	'stroke-color: #505896; stroke-opacity: 0.8; stroke-width: 2; color: #3e4475', 	'15',	'',
        ]
      ]);

      var options = {
        title: 'Home Temperatures',
        chartArea: {width: '80%'},
        bar: {groupWidth: "95%"},
        isStacked: true,
        legend: { position: "none" },
        annotations: { 
        			alwaysOutside: true,
        			textStyle: {
      					fontSize: 18,
      					bold: true,
      					italic: true,
      					// The color of the text.
      					color: '#FFFFFF',
      					// The color of the text outline.
      					auraColor: 'transparent',
				      // The transparency of the text.
      					opacity: 1.8
    					},
              stem: {
              	color: 'transparent'
              },
        			highContrast: false }
      };

      var chart = new google.visualization.BarChart(document.getElementById('chart_div'));

      chart.draw(data, options);
    }
