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
				"name": "COMPANY_CODE",
				"label": "$LANG{AD_102_3.COMPANY_CODE}",
				"maxLength": "10",
				"inputWidth": "100",
				"required": true,
				"validate": ""
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "COMPANY_NAME_LOC",
				"label": "$LANG{AD_102_3.COMPANY_NAME_LOC}",
				"labelAlign": "right",
				"inputWidth": "100"
			}
		]
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "input",
				"value": "",
				"name": "CTRL_CODE",
				"label": "$LANG{AD_102_3.CTRL_CODE}",
				"validate": "",
				"required": true,
				"inputWidth": "100"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "button",
				"value": "$LANG{AD_102_3.DUPLICATE_CHK}",
				"name": "duplicate"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "CTRL_NAME_LOC",
				"label": "$LANG{AD_102_3.CTRL_NAME_LOC}",
				"inputWidth": "100"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "CTRL_NAME_ENG",
				"label": "$LANG{AD_102_3.CTRL_NAME_ENG}",
				"inputWidth": "100"
			}
		]
	}
]