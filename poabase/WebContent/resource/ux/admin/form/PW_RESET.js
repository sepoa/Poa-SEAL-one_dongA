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
				"type": "block", 
				"width": "auto", 
				"list": [
					{
						"type": "input", 
						"value": "", 
						"disabled": "true", 
						"readonly": "true", 
						"label": "$LANG{PW_RESET.USER_ID}",
						"name": "USER_ID", 
						"inputWidth": "200"
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
						"disabled": "true", 
						"readonly": "true", 
						"label": "$LANG{PW_RESET.USER_NAME}",
						"name": "USER_NAME", 
						"inputWidth": "200"
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
						"name": "NEW_PASSWORD", 
						"label": "$LANG{PW_RESET.NEW_PASSWORD}",
						"inputWidth": "200"
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "button", 
						"value": "$LANG{PW_RESET.GEN_AUTO_PW}",
						"name": "GEN_AUTO_PW"
					}
				]
			},
			{
				"type": "block", 
				"width": "auto", 
				"list": [
					{
						"type": "checkbox", 
						"value": "", 
						"name": "SEND_MAIL_FLAG", 
						"label": "$LANG{PW_RESET.SEND_MAIL_FLAG}",
						"hidden": "true"
					},
					{
						"type": "newcolumn"
					},
					{
						"type": "input", 
						"value": "", 
						"label": "$LANG{PW_RESET.EMAIL}",
						"name": "EMAIL", 
						"inputWidth": "265", 
						"hidden": "true"
					}
				]
			}
		]
	}
]