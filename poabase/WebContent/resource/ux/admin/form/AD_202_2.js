[ {
	"type" : "settings",
	"labelWidth" : "55",
	"blockOffset" : "0",
	"offsetLeft" : "0"
}, {
	"type" : "block",
	"width" : "auto",
	"blockOffset" : "",
	"list" : [ {
		"type" : "checkbox",
		"value" : "",
		"name" : "FLAG",
		"label" : "$LANG{AD_202_2.FLAG}",
		"labelWidth" : "110",
		"position" : "label-right",
		"labelAlign" : "left"
	} ]
}, {
	"type" : "input",
	"value" : "",
	"name" : "SUBJECT",
	"label" : "$LANG{AD_202_2.SUBJECT}",
	"inputWidth" : "925",
	"required" : true,
	"disabled" : true,
	"readonly" : true,
	"placeholder" : "{LABEL}"
}, {
	"type" : "block",
	"width" : "auto",
	"blockOffset" : "",
	"offsetTop" : "10",
	"list" : [ {
		"type" : "vault",
		"value" : "",
		"name" : "ATTACH_NO",
		"label" : "$LANG{AD_202_2.ATTACH_NO}",
		"inputHeight" : "150",
		"inputWidth" : "925",
		"uploadType": "NOT"
	}, {
		"type" : "container",
		"value" : "",
		"name" : "EDITOR_BOX",
		"label" : "$LANG{AD_202_2.EDITOR_BOX}",
		"inputWidth" : "925",
		"inputHeight" : "700"
	} ]
} ]