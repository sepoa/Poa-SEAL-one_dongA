[
	{
		"type": "settings",
		"position": "label-top",
		"inputWidth": "412"
	},
	{
		"type": "block",
		"width": "auto",
		"label": "$LANG{AD_051_2.CHANGE_PASSWORD}",
		"list": [
			{
				"type": "block",
				"width": "auto",
				"list": [
					{
						"type": "password",
						"name": "PASS_WORD",
						"label": "$LANG{AD_051_2.PASS_WORD}",
						"required": true,
						"offsetTop": "10",
						"maxLength": "20"
					},
					{
						"type": "password",
						"value": "",
						"name": "NEW_PASSWORD_1",
						"label": "$LANG{AD_051_2.NEW_PASSWORD_1}",
						"maxLength": "20"
					},
					{
						"type": "password",
						"value": "",
						"name": "NEW_PASSWORD_2",
						"label": "$LANG{AD_051_2.NEW_PASSWORD_2}",
						"maxLength": "20"
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "label",
						"value": "",
						"name": "NEW_PASSWORD_MESSAGE",
						"label": ""
					}
				]
			}
		]
	}
]