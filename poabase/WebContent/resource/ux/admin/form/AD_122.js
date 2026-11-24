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
				"type": "select",
				"name": "INEQUALITY_TYPE",
				"required": "true",
				"inputWidth": "100",
				"label": "부등호",
				"options": [
					{"text": "<", "value": "lt"},
					{"text": "<=", "value": "le"},
    				{"text": ">", "value": "gt"},
    				{"text": ">=", "value": "ge"},
    				{"text": "==", "value": "eq"},
    				{"text": "!=", "value": "ne"}
				]
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "VALUE",
				"required": "true",
				"inputWidth": "100",
				"label": "값"
			}
		]
	}
]