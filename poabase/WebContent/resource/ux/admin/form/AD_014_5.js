[
	{
		"type": "settings",
		"position": "label-top"
	},
	{
		"type": "input",
		"name": "TEXT1",
		"label": "$LANG{AD_014_5.TEXT1}",
		"inputWidth": "474",
		"placeholder": "{LABEL}"
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "input",
				"name": "TYPE",
				"label": "$LANG{AD_014_5.TYPE}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"value": "",
				"name": "FLAG",
				"label": "$LANG{AD_014_5.FLAG}",
				"inputWidth": "110",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#AD016"
				}
			}
		]
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "select",
				"value": "",
				"name": "USE_FLAG",
				"label": "$LANG{AD_014_5.USE_FLAG}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#AD004"
				}
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "TEXT2",
				"label": "$LANG{AD_014_5.TEXT2}",
				"inputWidth": "270"
			}
		]
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "select",
				"value": "",
				"name": "TEXT5",
				"label": "$LANG{AD_014_5.TEXT5}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#AD004"
				}
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "TEXT3",
				"label": "$LANG{AD_014_5.TEXT3}",
				"inputWidth": "270"
			}
		]
	},
	{
		"type": "input",
		"value": "",
		"name": "TEXT4",
		"label": "$LANG{AD_014_5.TEXT4}",
		"rows": "10",
		"inputWidth": "474"
	}
]