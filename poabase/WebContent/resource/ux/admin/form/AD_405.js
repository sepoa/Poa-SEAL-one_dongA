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
            	"name": "CLIENT_CODE",
            	"required": true,
            	"readonly": true,
            	"label": "$LANG{AD_405.CLIENT_CODE}",
            	"maxLength": "10"
        	},
        	{
        		"type": "newcolumn"
        	},
        	{
        		"type": "button",
        		"name": "CLIENT_CODE_CHECK",
        		"value": "$LANG{AD_405.CLIENT_CODE_CHECK}"
        	},
        	{
        		"type": "newcolumn"
        	},
        	{
        		"type": "button",
        		"name": "CLIENT_COPY",
        		"value": "$LANG{AD_405.CLIENT_COPY}"
        	}
        ]
    },
    {
    	"type": "block",
    	"inputWidth": "450",
    	"list": [
    		{
    			"type": "input",
    			"label": "$LANG{AD_405.COMPANY_NAME}",
    			"name": "COMPANY_NAME",
    			"inputWidth": "450",
    			"required": true
    		}
    	]
    },
    {
    	"type": "block",
    	"list": [
    		{
    			"type": "input",
    			"label": "$LANG{AD_405.IRS_NO}",
    			"name": "IRS_NO",
    			"required": true,
    			"maxLength": "10"
    		}
    	]
    },
    {
    	"type": "block",
    	"inputWidth": "450",
    	"list": [
    		{
    			"type": "calendar",
    			"label": "$LANG{AD_405.SERVICE_FROM}",
    			"name": "SERVICE_FROM",
    			"required": true
    		},
    		{
    			"type": "newcolumn"
    		},
    		{
    			"type": "label",
    			"label": "~"
    		},
    		{
    			"type": "newcolumn"
    		},
    		{
    			"type": "calendar",
    			"label": " ",
    			"name": "SERVICE_TO",
    			"requried": true
    		}
    	]
    },
    {
    	"type": "block",
    	"list": [
    		{
    			"type": "input",
    			"label": "$LANG{AD_405.SERVICE_URL}",
    			"name": "SERVICE_URL",
    			"required": true,
    			"inputWidth": "450"
    		}
    	]
    },
    {
    	"type": "block",
    	"list": [
    		{
    			"type": "select",
    			"name": "APP_NAME",
    			"label": "$LANG{AD_405.APP_NAME}",
    			"required": true,
    			"options": [
    				{
    					"text": "TOMCAT",
    					"value": "TOMCAT"
    				}
    			]
    		}
    	]
    },
    {
    	"type": "block",
    	"list": [
    		{
    			"type": "select",
    			"name": "APP_DB_VENDOR",
    			"label": "$LANG{AD_405.DB_VENDOR}",
    			"required": true,
    			"options": [
    				{
    					"text": "ORACLE",
    					"value": "ORACLE"
    				},
    				{
    					"text": "MSSQL",
    					"value": "MSSQL"
    				},
    				{
    					"text": "MARIADB",
    					"value": "MARIADB"
    				}
    			]
    		}
    	]
    },
    {
    	"type": "block",
    	"list": [
    		{
    			"type": "input",
    			"name": "COMPANY_CODE",
    			"label": "",
    			"required": true,
    			"disabled": true,
    			"hidden": true
    		}
    	]
    }
]