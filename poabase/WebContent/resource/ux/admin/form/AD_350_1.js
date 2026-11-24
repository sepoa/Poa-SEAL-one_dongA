[
	{
		"type": "settings",
		"position": "label-top"
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "input",
				"name": "DEPLOY_ID",
				"label": "$LANG{AD_350_1.DEPLOY_ID}",
				"note": {
					"width": "200",
					"text": ""
				},
				"position": "label-left",
				"labelAlign": "left",
				"disabled": true,
				"readonly": true,
				"inputWidth": "300"
			},
			{
				"type": "input",
				"value": "",
				"name": "SOURCE_TEMP_DIR",
				"label": "$LANG{AD_350_1.SOURCE_TEMP_DIR}",
				"labelAlign": "left",
				"inputWidth": "300"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "SERVER_URL",
				"label": "$LANG{AD_350_1.SERVER_URL}",
				"position": "label-left",
				"labelAlign": "left",
				"note": {
					"width": "200",
					"text": ""
				},
				"inputWidth": "300"
			},
			{
				"type": "input",
				"value": "",
				"inputWidth": "300",
				"name": "TARGET_TEMP_DIR",
				"label": "$LANG{AD_350_1.TARGET_TEMP_DIR}",
				"labelAlign": "left"
			}
		]
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "input",
				"name": "DESCRIPTION",
				"label": "$LANG{AD_350_1.DESCRIPTION}",
				"note": {
					"width": "300",
					"text": ""
				},
				"labelAlign": "left",
				"required": true,
				"inputWidth": "605",
				"placeholder": "$LANG{AD_350_1.DESCRIPTION_PLACEHOLDER}",
				"label": ""
			}
		]
	}
]