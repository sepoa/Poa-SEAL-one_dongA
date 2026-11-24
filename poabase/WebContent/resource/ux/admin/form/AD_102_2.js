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
				"value": "",
				"name": "COMPANY_CODE",
				"label": "$LANG{AD_102_2.COMPANY_CODE}",
				"labelAlign": "right",
				"required": true,
				"inputWidth": "120",
				"maxLength": "10"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "COMPANY_NAME_LOC",
				"label": "$LANG{AD_102_2.COMPANY_NAME_LOC}",
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
				"type": "settings"
			},
			{
				"type": "input",
				"value": "",
				"name": "DEPT",
				"label": "$LANG{AD_102_2.DEPT}",
				"validate": "",
				"required": true,
				"inputWidth": "120"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "button",
				"value": "$LANG{AD_102_2.DUPLICATE_CHK}",
				"name": "duplicate"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "DEPT_NAME_LOC",
				"label": "$LANG{AD_102_2.DEPT_NAME_LOC}",
				"required": true,
				"inputWidth": "120"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "DEPT_NAME_ENG",
				"label": "$LANG{AD_102_2.DEPT_NAME_ENG}",
				"inputWidth": "120"
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
				"name": "MANAGER_NAME",
				"label": "$LANG{AD_102_2.MANAGER_NAME}",
				"maxLength": "120",
				"validate": "",
				"inputWidth": "120"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "MANAGER_POSITION",
				"label": "$LANG{AD_102_2.MANAGER_POSITION}",
				"inputWidth": "120"
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
				"name": "PHONE_NO",
				"label": "$LANG{AD_102_2.PHONE_NO}",
				"maxLength": "14",
				"inputWidth": "120",
				"validate": ""
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "FAX_NO",
				"label": "$LANG{AD_102_2.FAX_NO}",
				"maxLength": "14",
				"inputWidth": "120",
				"validate": "",
				"icon": "icon-input"
			}
		]
	}
]