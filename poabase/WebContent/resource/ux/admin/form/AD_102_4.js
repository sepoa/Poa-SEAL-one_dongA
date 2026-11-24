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
				"label": "$LANG{AD_102_4.COMPANY_CODE}",
				"maxLength": "10",
				"inputWidth": "120",
				"required": true,
				"validate": ""
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "COMPANY_NAME_LOC",
				"label": "$LANG{AD_102_4.COMPANY_NAME_LOC}",
				"labelAlign": "right"
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
				"name": "PLANT_CODE",
				"label": "$LANG{AD_102_4.PLANT_CODE}",
				"validate": "",
				"required": true,
				"inputWidth": "120"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "PLANT_NAME_LOC",
				"label": "$LANG{AD_102_4.PLANT_NAME_LOC}"
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
				"name": "STORAGE_CODE",
				"label": "$LANG{AD_102_4.STORAGE_CODE}",
				"validate": "",
				"required": true,
				"inputWidth": "120"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "button",
				"value": "$LANG{AD_102_4.DUPLICATE_CHK}",
				"name": "duplicate"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "STORAGE_NAME_LOC",
				"label": "$LANG{AD_102_4.STORAGE_NAME_LOC}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "STORAGE_NAME_ENG",
				"label": "$LANG{AD_102_4.STORAGE_NAME_ENG}"
			}
		]
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "input",
				"name": "ZIP_CODE",
				"label": "$LANG{AD_102_4.ZIP_CODE}",
				"maxLength": "5",
				"inputWidth": "120",
				"required": true,
				"validate": ""
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "PHONE_NO",
				"label": "$LANG{AD_102_4.PHONE_NO}",
				"maxLength": "14",
				"inputWidth": "120",
				"required": true,
				"validate": ""
			}
		]
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "input",
				"name": "ADDRESS_LOC",
				"label": "$LANG{AD_102_4.ADDRESS_LOC}",
				"inputWidth": "700",
				"required": true,
				"validate": ""
			},
			{
				"type": "input",
				"name": "ADDRESS_ENG",
				"label": "$LANG{AD_102_4.ADDRESS_ENG}",
				"inputWidth": "700"
			},
			{
				"type": "input",
				"name": "HOMEPAGE",
				"label": "$LANG{AD_102_4.HOMEPAGE}",
				"inputWidth": "700"
			}
		]
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "settings"
			},
			{
				"type": "input",
				"name": "CEO_NAME",
				"label": "$LANG{AD_102_4.CEO_NAME}",
				"maxLength": "120",
				"required": true,
				"validate": "",
				"inputWidth": "120"
			}
		]
	}
]