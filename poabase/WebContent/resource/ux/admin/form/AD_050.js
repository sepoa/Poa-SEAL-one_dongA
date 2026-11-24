[
	{
		"type": "settings",
		"position": "label-top"
	},
	{
		"type": "fieldset",
		"width": "auto",
		"label": "$LANG{AD_050.USER_INFO}",
		"list": [
			{
				"type": "settings"
			},
			{
				"type": "block",
				"width": "auto",
				"list": [
					{
						"type": "settings",
						"inputWidth": "150"
					},
					{
						"type": "image",
						"name": "PROFILE_ATTACHE_NO",
						"url": "/servlets/sepoa.svl.util.SepoaFormImageUrl?defaultPath=/theme/basic/imgs/icon/proflie.gif",
						"label": "",
						"inputWidth": "80",
						"inputHeight": "105",
						"className": "profileimg"
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "input",
						"value": "",
						"name": "USER_ID",
						"label": "$LANG{AD_050.USER_ID}",
						"disabled": true
					},
					{
						"type": "input",
						"value": "",
						"name": "USER_NAME_LOC",
						"label": "$LANG{AD_050.USER_NAME_LOC}"
					},
					{
						"type": "input",
						"value": "",
						"name": "USER_NAME_ENG",
						"label": "$LANG{AD_050.USER_NAME_ENG}"
					}
				]
			},
			{
				"type": "block",
				"width": "auto",
				"list": [
					{
						"type": "settings",
						"inputWidth": "150"
					},
					{
						"type": "select",
						"value": "",
						"name": "POSITION",
						"label": "$LANG{AD_050.POSITION}",
						"userdata": {
							"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#UX049"
						}
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "select",
						"value": "",
						"name": "MANAGER_POSITION",
						"label": "$LANG{AD_050.MANAGER_POSITION}",
						"userdata": {
							"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#UX050"
						}
					}
				]
			},
			{
				"type": "block",
				"width": "auto",
				"list": [
					{
						"type": "settings",
						"inputWidth": "150"
					},
					{
						"type": "select",
						"value": "",
						"name": "DEPT",
						"label": "$LANG{AD_050.DEPT}",
						"userdata": {
							"combo": "SL0015,{HOUSE_CODE}#{COMPANY_CODE}"
						}
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "input",
						"value": "",
						"name": "EMPLOYEE_NO",
						"label": "$LANG{AD_050.EMPLOYEE_NO}",
						"userdata": {
							"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#UX050"
						}
					}
				]
			},
			{
				"type": "block",
				"width": "auto",
				"list": [
					{
						"type": "settings",
						"inputWidth": "150"
					},
					{
						"type": "input",
						"value": "",
						"name": "EMAIL",
						"label": "$LANG{AD_050.EMAIL}"
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "input",
						"value": "",
						"name": "PHONE_NO1",
						"label": "$LANG{AD_050.PHONE_NO1}"
					}
				]
			},
			{
				"type": "block",
				"width": "auto",
				"list": [
					{
						"type": "settings",
						"inputWidth": "150"
					},
					{
						"type": "input",
						"value": "",
						"name": "PHONE_NO2",
						"label": "$LANG{AD_050.PHONE_NO2}"
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "input",
						"value": "easypro",
						"name": "FAX_NO",
						"label": "$LANG{AD_050.FAX_NO}"
					}
				]
			}
		]
	},
	{
		"type": "fieldset",
		"width": "auto",
		"label": "$LANG{AD_050.CHANGE_PASSWORD}",
		"list": [
			{
				"type": "settings"
			},
			{
				"type": "block",
				"width": "auto",
				"list": [
					{
						"type": "password",
						"value": "admin1234",
						"name": "PASS_WORD",
						"label": "$LANG{AD_050.PASS_WORD}",
						"inputWidth": "425",
						"required": true
					}
				]
			},
			{
				"type": "block",
				"width": "auto",
				"list": [
					{
						"type": "settings",
						"inputWidth": "150"
					},
					{
						"type": "password",
						"value": "",
						"name": "NEW_PASSWORD_1",
						"label": "$LANG{AD_050.NEW_PASSWORD_1}"
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "password",
						"value": "",
						"name": "NEW_PASSWORD_2",
						"label": "$LANG{AD_050.NEW_PASSWORD_2}"
					}
				]
			}
		]
	}
]