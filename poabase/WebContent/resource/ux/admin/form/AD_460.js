[
	{
        "type": "settings",
        "inputWidth": "200",
        "position": "label-top"
    },
    {
        "type": "block",
        "list": [
        	{
        		"type": "input",
            	"name": "CLIENT_NAME",
            	"readonly": "true",
            	"label": "$LANG{AD_460.CLIENT_NAME}"
        	},
        	{
        		"type": "newcolumn"
        	},
        	{
        		"type": "input",
            	"name": "TEMPLATE_CODE",
            	"required":"true",
            	"label": "$LANG{AD_460.TEMPLATE_CODE}"
        	},
        	{
        		"type": "newcolumn"
        	},
        	{
        		"type": "input",
        		"name": "DESCRIPTION",
        		"label": "$LANG{AD_460.DESCRIPTION}"
        	},
        	{
        		"type": "newcolumn"
        	},
        	{
        		"type": "input",
        		"name": "HOUSE_CODE",
        		"readonly": "true",
        		"hidden": "true",
        		"label": "$LANG{AD_460.HOUSE_CODE}"
        	}
        ]
    }
]