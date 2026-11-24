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
				"type": "settings"
			},
			{
				"type": "input",
				"name": "COMPANY_CODE",
				"label": "$LANG{AD_102_1.COMPANY_CODE}",
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
				"label": "$LANG{AD_102_1.COMPANY_NAME_LOC}",
				"labelAlign": "right",
				"inputWidth": "120"
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
				"label": "$LANG{AD_102_1.PLANT_CODE}",
				"validate": "",
				"required": true,
				"inputWidth": "120"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "button",
				"value": "$LANG{AD_102_1.DUPLICATE_CHK}",
				"name": "duplicate"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "PLANT_NAME_LOC",
				"label": "$LANG{AD_102_1.PLANT_NAME_LOC}",
				"inputWidth": "120"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "PLANT_NAME_ENG",
				"label": "$LANG{AD_102_1.PLANT_NAME_ENG}",
				"inputWidth": "105"
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
				"label": "$LANG{AD_102_1.ZIP_CODE}",
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
				"label": "$LANG{AD_102_1.PHONE_NO}",
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
				"name": "CEO_NAME",
				"label": "$LANG{AD_102_1.CEO_NAME}",
				"maxLength": "120",
				"required": true,
				"validate": "",
				"inputWidth": "120"
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
				"label": "$LANG{AD_102_1.ADDRESS_LOC}",
				"inputWidth": "390",
				"required": true,
				"validate": ""
			},
			{
				"type": "input",
				"name": "ADDRESS_ENG",
				"label": "$LANG{AD_102_1.ADDRESS_ENG}",
				"inputWidth": "390"
			},
			{
				"type": "input",
				"name": "HOMEPAGE",
				"label": "$LANG{AD_102_1.HOMEPAGE}",
				"inputWidth": "390"
			}
		]
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "input",
				"name": "BUSINESS_TYPE",
				"label": "$LANG{AD_102_1.BUSINESS_TYPE}",
				"inputWidth": "120",
				"maxLength": "90"
			},
			{
				"type": "input",
				"name": "INDUSTRY_TYPE",
				"label": "$LANG{AD_102_1.INDUSTRY_TYPE}",
				"maxLength": "90",
				"inputWidth": "500"
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
				"type": "container",
				"name": "JIKIN_ATTACH_NO",
				"label": "$LANG{AD_102_1.JIKIN_ATTACH_NO}",
				"maxLength": "90",
				"inputWidth": "500",
				"className": "JIKIN_ATTACH_NO",
				"inputHeight": "120"
			}
		]
	}
]