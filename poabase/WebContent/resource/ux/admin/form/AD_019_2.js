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
				"name": "USER_TYPE",
				"label": "$LANG{AD_019_2.USER_TYPE}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#AD011"
				}
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"name": "WORK_TYPE",
				"label": "$LANG{AD_019_2.WORK_TYPE}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#UX099"
				}
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"value": "",
				"name": "COMPANY_CODE",
				"label": "$LANG{AD_019_2.COMPANY_CODE}",
				"userdata": {
					"combo": "SL0013,{HOUSE_CODE}"
				}
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"name": "PLANT_CODE",
				"label": "$LANG{AD_019_2.PLANT_CODE}",
				"userdata": {
					"combo": "SL0014,{HOUSE_CODE}#{COMPANY_CODE}"
				}
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"name": "DEPT",
				"label": "$LANG{AD_019_2.DEPT}",
				"userdata": {
					"combo": "SL0015,{HOUSE_CODE}#{COMPANY_CODE}"
				}
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
				"name": "SELLER_CODE",
				"label": "$LANG{AD_019_2.SELLER_CODE}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "SELLER_NAME_LOC",
				"label": "$LANG{AD_019_2.SELLER_NAME_LOC}",
				"inputWidth": "295"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "image",
				"value": "",
				"name": "VENDOR_SEARCH",
				"label": "",
				"className":"btn_search"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"value": "",
				"name": "MENU_PROFILE_CODE",
				"label": "$LANG{AD_019_2.MENU_PROFILE_CODE}",
				"userdata": {
					"combo": "SL0035,{LANGUAGE}"
				},
				"inputWidth": "289"
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
				"name": "COUNTRY",
				"label": "$LANG{AD_019_2.COUNTRY}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#AD002"
				}
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"value": "",
				"name": "LANGUAGE",
				"label": "$LANG{AD_019_2.LANGUAGE}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#AD003"
				},
				"required": true
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "STATE",
				"label": "$LANG{AD_019_2.STATE}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"value": "%Y/%m/%d",
				"name": "DATE_FORMAT",
				"label": "$LANG{AD_019_2.DATE_FORMAT}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#UX102"
				}
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "select",
				"value": "",
				"name": "TIME_ZONE",
				"label": "$LANG{AD_019_2.TIME_ZONE}",
				"userdata": {
					"combo": "SL0001,{HOUSE_CODE}#{LANGUAGE}#UX101"
				}
			}
		]
	},
	{
		"type": "block",
		"width": "auto",
		"list": [
			{
				"type": "input",
				"name": "USER_ID",
				"label": "$LANG{AD_019_2.USER_ID}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "USER_NAME_LOC",
				"label": "$LANG{AD_019_2.USER_NAME_LOC}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "USER_NAME_ENG",
				"label": "$LANG{AD_019_2.USER_NAME_ENG}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "EMPLOYEE_NO",
				"label": "$LANG{AD_019_2.EMPLOYEE_NO}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"userdata": {
					"combo": "SL5001,M230"
				},
				"name": "POSITION",
				"label": "$LANG{AD_019_2.POSITION}"
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
				"name": "PHONE_NO1",
				"label": "$LANG{AD_019_2.PHONE_NO1}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"value": "",
				"name": "PHONE_NO2",
				"label": "$LANG{AD_019_2.PHONE_NO2}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "EMAIL",
				"label": "$LANG{AD_019_2.EMAIL}",
				"validate": "isValidEmail"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "password",
				"name": "PASSWORD_1",
				"label": "$LANG{AD_019_2.PASSWORD_1}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "password",
				"name": "PASSWORD_2",
				"label": "$LANG{AD_019_2.PASSWORD_2}"
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
				"name": "ZIP_CODE",
				"label": "$LANG{AD_019_2.ZIP_CODE}"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "image",
				"value": "",
				"name": "ADDR_SEARCH",
				"label": "",
				"className":"btn_search"
			},
			{
				"type": "newcolumn"
			},
			{
				"type": "input",
				"name": "ADDRESS_LOC",
				"label": "$LANG{AD_019_2.ADDRESS_LOC}",
				"inputWidth": "600"
			},
			{
				"type": "input",
				"value": "",
				"name": "ADDRESS_ENG",
				"label": "$LANG{AD_019_2.ADDRESS_ENG}",
				"inputWidth": "600"
			}
		]
	}
]