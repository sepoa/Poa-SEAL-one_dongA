[
	{
		"type": "settings",
		"posotion": "label-top",
		"inputWidth": "412"
	},
	{
		"type": "block",
		"width": "auto",
		"label": "$LANG{AD_051_1.USER_INFO}",
		"list": [
			{
				"type": "block",
				"width": "auto",
				"list": [
					{
						"type": "input",
						"value": "",
						"name": "EMAIL",
						"label": "$LANG{AD_051_1.EMAIL}"
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "input",
						"value": "",
						"name": "PHONE_NO1",
						"label": "$LANG{AD_051_1.PHONE_NO1}"
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
						"name": "DEPT",
						"label": "$LANG{AD_051_1.DEPT}",
						"readonly":"true",
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
						"name": "PHONE_NO2",
						"label": "$LANG{AD_051_1.PHONE_NO2}"
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
						"name": "POSITION",
						"label": "$LANG{AD_051_1.POSITION}",
						"readonly":"true",
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
						"label": "$LANG{AD_051_1.MANAGER_POSITION}",
						"readonly":"true",
						"userdata": {
							"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#UX050"
						}
					}
				]
			},
			{
				"type": "block",
				"name": "ATTACH_NO_SEAL_BLOCK",
				"width": "auto",
				"blockOffset": "", 
				"list": [
					{
						"type": "vault",
						"name": "ATTACH_NO_SEAL",
						"label": "$LANG{AD_051_1.ATTACH_NO_SEAL}",
						"inputHeight": "45",
						"uploadType": "seal"
					},
					{
						"type": "container", 
						"name": "ATTACH_NO_SEAL_VIEW",
						"inputHeight": "206",
						"className": "sepoa-user-info-seal-view"
					}
				]
			}
		]
	}
]