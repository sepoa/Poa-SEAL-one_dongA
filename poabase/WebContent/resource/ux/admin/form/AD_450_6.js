[
	{
        "type": "settings",
        "inputWidth": "200",
        "position": "label-top"
    },
    {
        "type": "block",
        "inputWidth" : "auto",
        "list": [
        	{
        		"type": "select",
            	"name": "KAKAO_VENDOR",
            	"label": "$LANG{AD_450_6.KAKAO_VENDOR}",
            	"options": [
            		{
            			"text": "bizplus",
            			"value": "BIZPLUS"
            		}
            	]
        	},
        	{
        		"type": "newcolumn"
        	},
        	{
        		"type": "input",
            	"name": "KAKAO_TEMPLATE_CODE",
            	"label": "$LANG{AD_450_6.KAKAO_TEMPLATE_CODE}"
        	},
        	{
        		"type": "newcolumn"
        	},
        	{
        		"type": "button",
        		"name": "KAKAO_BIZPLUS_MOVE",
        		"value": "$LANG{AD_450_6.KAKAO_BIZPLUS_MOVE}"
        	},
        	{
        		"type": "newcolumn"
        	},
        	{
        		"type": "button",
        		"name": "KAKAO_TEST_SEND",
        		"value": "$LANG{AD_450_6.KAKAO_TEST_SEND}"
        	}
        ]
    }
]