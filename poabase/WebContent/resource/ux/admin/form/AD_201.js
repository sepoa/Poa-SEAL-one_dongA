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
				"type": "select",
				"value": "",
				"name": "GONGJI_GUBUN",
				"label": "$LANG{AD_201.GONGJI_GUBUN}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#AD011"
				},
				"required": true
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"value": "",
				"name": "POPUP_LOCATION",
				"label": "$LANG{AD_201.POPUP_LOCATION}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#AD012,N"
				},
				"disabled": true
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "calendar",
				"value": "",
				"minutesInterval": 15,
				"calendarPosition": "bottom",
				"name": "PUBLISH_FROM_DATE",
				"label": "$LANG{AD_201.PUBLISH_FROM_DATE}",
				"required": true
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "calendar",
				"value": "",
				"name": "PUBLISH_TO_DATE",
				"label": "$LANG{AD_201.PUBLISH_TO_DATE}",
				"calendarPosition": "bottom",
				"minutesInterval": 15,
				"required": true
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"value": "",
				"name": "PUBLISH_FLAG",
				"label": "$LANG{AD_201.PUBLISH_FLAG}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#AD013"
				}
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "NOTICE_NUMBER",
				"label": "$LANG{AD_201.NOTICE_NUMBER}",
				"disabled": true
			}
		]
	},
	{
		"type": "input",
		"value": "",
		"name": "SUBJECT",
		"label": "$LANG{AD_201.SUBJECT}",
		"inputWidth": "1140",
		"required": true,
		"placeholder": "{LABEL}"
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "vault",
				"value": "",
				"name": "ATTACH_NO",
				"label": "$LANG{AD_201.ATTACH_NO}",
				"inputHeight": "150",
				"inputWidth": "1140",
				"uploadType": "NOT"
			},
			{
				"type": "container",
				"value": "",
				"name": "EDITOR_BOX",
				"label": "$LANG{AD_201.EDITOR_BOX}",
				"inputWidth": "1138",
				"inputHeight": "700"
			}
		]
	}
]