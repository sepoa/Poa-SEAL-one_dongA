function float2fraction(t){
	var a = 1;
	while(true){
		var v = 1 / a;
		if(v == t || v < t){
			return a;
		}else{
			a++;
		}
		if(a > 100) return null;
	}
}

SepoaUXMockup=function(p, v, m){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.mode = m;
	this._objPool={};
	this.vp = p;
	$(this.vp).disableSelection();
	this.vp.className = "sepoaux_mockup_base";
	this.baseTime = (m)?"%REPLACEID%":this.convert52digits(new Date().getTime());
	this.baseInfo = v.info;
	this.renderBase(v);
	this.attachEvent('onUpdateItem', function(text){
		this.callEvent('onAddHistory', [this.rootItem.getOptions().component.VAR + " / " + text]);
	})
}
SepoaUXMockup.prototype.renderBase=function(v){
	var o = {};
	if(v.objects){
		this._objPool = v.objects;
		o = v.objects[v.tree.id]
	}else{
		o = {component:{VAR:v.info.PREFIX+"_"+v.info.CODE}}
	}
	
	this.rootItem = new suxItem_Body(this, o, v.tree);
	this._objPool[this.rootItem.id] = this.rootItem;
	this.vp.appendChild(this.rootItem.base);
}
SepoaUXMockup.prototype.updateInfo=function(t, o, n){
	for(var j in this._objPool){
		var obj = this._objPool[j];
		if(obj.type == "screen"){
			if(obj.getValue() == o){
				obj.setValue(n);
			}
		}else if(t == "s" && obj.type == "body" && obj.options.component.VAR == o){
			obj.options.component.VAR = n
			obj.changeTitle();
		}
	}
}
SepoaUXMockup.prototype.newId = function(){
	return ('_'+[1e6]+1e3+4e3+8e3+1e9).replace(/[_01]/g, c => {
		if(c == '_'){
			return (10 + (crypto.getRandomValues(new Uint8Array(1))[0] & 5)).toString(16)
		}else{
			return (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
		}
	}) 
}
SepoaUXMockup.prototype.convert52digits = function(i){
	var x52 = function(n){
		if(n > 25){
			n += 6;
		}
		return String.fromCharCode(65+n);
	}
	
	var a = i % 52
	var b = (i - a) / 52
	if(b > 51){
		return x52(a) + this.convert52digits(b);
	}else{
		return x52(a);
	}
}
SepoaUXMockup.prototype._TextData = function(t){
	var a = t.split(".");
	return Sepoa.cache.text.get("SCREEN_BUILDER."+a);
}
SepoaUXMockup.prototype.exportData=function(){
	var objects = {};
	for(var i in this._objPool){
		var t = this._objPool[i]
		if(t.getChildTree)
			objects[t.id] = t.getOptions();
	}
	return {
		objects:objects,
		tree:this.rootItem.getChildTree()
	}
}
SepoaUXMockup.prototype.getFormOptions=function(t, i){
	var get={
		SELECT_YN:function(){
			return [
				{text: "", value: ""},
			    {text: "Y", value: "true"},
		        {text: "N", value: "false"}
		    ]
		},
	    HEADER_CSS_TYPE:function(){
	    	return [
	    		{text: ""        , value: ""},
		    	{text: "List"    , value: "list"},
		        {text: "Contents", value: "contents"}
		    ]
	    },
	    ATTACH_TYPE:function(){
	    	return [
	    		{text: ""         , value: ""},
	    		{text: "ScreenID" , value: "S"},
	    		{text: "XML"      , value: "U"},
	    		{text: "iFrame"   , value: "F"},
		    ]
	    },
	    LAYOUT_PATTERN:function(){
	    	var r = [];
	    	for(var i in dhtmlXLayoutObject.prototype.tplData){
	    		r.push({text:i, value:i})
	    	}
	    	return r;
	    },
	    ACTIVE_INACTIVE:function(){
	    	return [
	    		{text: "Active"  , value: "true"},
		        {text: "inActive", value: "false"}
	    	]
	    },
	    LAYOUT_COLLAPSED:function(){
	    	return [
	    		{text: "Collapse"  , value: "true"},
		        {text: "Expand", value: "false"}
	    	]
	    },
	    FILTERBAR_FILTER_ITEM:function(){
	    	return [
	    		{text:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ADD_ITEM"), value:""},
	    		{text:"text", value:"text"},
	    		{text:"select", value:"select"},
	    		{text:"calendar", value:"calendar"},
				{text:"popup", value:"popup"},
				{text:"checkbox", value:"checkbox"},
				{text:"flag", value:"flag"},
	    	]
	    },
	    NUMBER:function(r){
	    	var o = [];
	    	for(var i=r[0];i<=r[1];i++){
	    		o.push({text: i, value:i});
	    	}
	    	return o;
	    },
	    ALIGN_HORIZONTAL:function(){
	    	return [
	    		{text:"", value:""},
	    		{text:"left", value:"left"},
	    		{text:"right", value:"right"},
	    		{text:"center", value:"center"}
	    	]
	    },
	    ALIGN_VERTICAL:function(){
	    	return [
	    		{text:"", value:""},
	    		{text:"top", value:"top"},
	    		{text:"middle", value:"middle"},
	    		{text:"bottom", value:"bottom"}
	    	]
	    },
	    CURSOR:function(){
	    	return [
	    		{text:"", value:""},
	    		{text:"pointer", value:"pointer"}
	    	]
	    },
	    SCRIPT_LIST:function(){
	    	var r = [{text:"", value:""}, {text:"--New Function--", value:"--NEW_FUNCTION--"}];
	    	for(var i in this.baseInfo.SCRIPT){
	    		var a = this.baseInfo.SCRIPT[i];
	    		if(a.type == "function"){
	    			r.push({text:a.NAME + "("+a.ARGS+")", value:a.NAME});
	    		}
	    	}
	    	return r;
	    },
	    VAULT_MODE:function(){
	    	return [
	    		{text:"" , value:""},
	    		{text:"download only" , value:"VI"}
	    	]
	    },
	    VAULT_METHOD_TYPE:function(){
	    	return [
	    		{text:"", value:""},
	    		{text:"SELECT", value:"SELECT"},
	    		{text:"UPLOAD", value:"UPLOAD"},
	    		{text:"DELETE", value:"DELETE"}
	    	]
	    },
	    BUTTON_POSITION:function(){
	    	return [
	    		{text:"", value:""},
	    		{text:"icon-left", value:"left"},
	    		{text:"icon-right", value:"right"}
	    	]
	    },
	    RENDER_TYPE:function(){
	    	return [
	    		{text: "", value:""},
	    		{text: "lazy", value:"lazy"}
	    	]
	    }
	}
	if(get[t]){
		return get[t].apply(this,[i]);
	}
}
SepoaUXMockup.prototype.getFormTemplate=function(t,p){
	var get={
		layout:function(){
			return {
				title:"layout",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true},
						{type: "select", value: "", name: "PATTERN",label:this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_PATTERN"), options:this.getFormOptions("LAYOUT_PATTERN"), className: "layout_pattern_select"},
						{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
						{type: "image", value: "1", name: "PATTERN_SAMPLE", label:this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_PATTERN_SAMPLE"), imageWidth:"645", imageHeight:"350", url:"/resource/plugin/sepoasoft/screenbuilder/icon/layout_patterns.png", className: "layoutImage"}
					]}
				],
				layout:this.getDesignOptions('LAYOUT', ['auto_height', 'size', 'margin', 'align', 'custom']),
				event:this.getEventForm({})
			}
		},
		layoutCell:function(){
			return {
				title:"layoutCell "+p,
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto",list: [
						{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_FIELD_BASIC"), value: "", className: "none_label_top screenBuilderLabel"},
						{type: "newcolumn"},
						{type: "input", value: "", label:this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_WIDTH"), name: "BASE_WIDTH"},
						{type: "newcolumn"},
						{type: "input", value: "", label:this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_HEIGHT"), name: "BASE_HEIGHT"}
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "block", width: "auto", list: [
							{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_FIELD_HEADER"), value: "", className: "none_label_top screenBuilderLabel"},
							{type: "newcolumn"},
							{type: "select", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_HEADER_VIEW"), name: "HEADER_VIEW", options:this.getFormOptions("SELECT_YN")},
							{type: "newcolumn"},
							{type: "select", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_HEADER_TYPE"), name: "HEADER_TYPE", options:this.getFormOptions("HEADER_CSS_TYPE")},
						]},
						{type: "block", width: "auto", list: [
							{type: "label", label: "", value: "", className: "none_label_top screenBuilderLabel"},
							{type: "newcolumn"},
							{type: "select", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_MENU"), name: "BASE_MENU", options:this.getFormOptions("SELECT_YN")},
							{type: "newcolumn"},
							{type: "select", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_COLLAPSE_COLLAPSED"), name: "COLLAPSE_COLLAPSED", options:this.getFormOptions("LAYOUT_COLLAPSED")}
						]},
						{type: "block", width: "auto", list: [
							{type: "label", label: "", value: "", className: "none_label_top screenBuilderLabel"},
							{type: "newcolumn"},
							{type: "input", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_HEADER_TEXT"), name: "HEADER_TEXT"}
						]},
						{type: "block", width: "auto", list: [
							{type: "label", label: "", value: "", className: "none_label_top screenBuilderLabel"},
							{type: "newcolumn"},
							{type: "select", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_COLLAPSE_ICON"), name: "COLLAPSE_ICON", options:this.getFormOptions("ACTIVE_INACTIVE")},
							{type: "newcolumn"},
							{type: "input", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_COLLAPSE_TEXT"), name: "COLLAPSE_TEXT"}
						]}
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_LAYOUT_CELL_FIELD_CONTENTS"), value: "", className: "none_label_top screenBuilderLabel"},
						{type: "newcolumn"},
						{type: "select", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_TYPE"), name: "ATTACH_TYPE", options:this.getFormOptions("ATTACH_TYPE")},
						{type: "newcolumn"},
						{type: "input", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_TARGET"), name: "ATTACH_TARGET", className: "areaDivision_content"},
						{type: "newcolumn"},
						{type: "input", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_ALIAS"), name: "ATTACH_ALIAS", className: "areaDivision_content"}
					]}
				]
			}
		},
		filterbar:function(){
			return {
				title:"FilterBar",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
					{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_KEYWORD_ACTIVE"), value: "", className: "screenBuilderLabel"},
						{type: "newcolumn"},
						{type: "select", value: "", name: "KEYWORD_ACTIVE", label: this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_SELECT_ACTIVE"), options:this.getFormOptions("ACTIVE_INACTIVE")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "KEYWORD_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_KEYWORD_ID"), required: true},
						{type: "newcolumn"},
						{type: "input", value: "", name: "KEYWORD_COMBO", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_KEYWORD_COMBO")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "KEYWORD_PLACEHOLDER", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_KEYWORD_PLACEHOLDER")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "KEYWORD_MODULE", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_KEYWORD_MODULE"), readonly: true}
					]},
					{type: "block", width: "auto", list: [
						{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_STATUS_ACTIVE"), value: "", className: "screenBuilderLabel"},
						{type: "newcolumn"},
						{type: "select", value: "", name: "STATUS_ACTIVE", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_SELECT_ACTIVE"), icon: "icon-select", options:this.getFormOptions("ACTIVE_INACTIVE")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "STATUS_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_STATUS_ID"), icon: "icon-input", required: true, required: true},
						{type: "newcolumn"},
						{type: "input", value: "", name: "STATUS_COMBO", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_STATUS_COMBO"), required: true, required: true},
						{type: "newcolumn"},
						{type: "input", value: "", name: "STATUS_LABEL", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_STATUS_LABEL"), icon: "icon-input", required: true},
						{type: "newcolumn"},
						{type: "input", value: "", name: "STATUS_DEFAULT", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_STATUS_DEFAULT"), required: true},
						{type: "newcolumn"},
						{type: "input", value: "", name: "STATUS_MODULE", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_STATUS_MODULE"), readonly: true}
					]},
					{type: "block", width: "auto", list: [
						{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_CALENDAR_ACTIVE"), value: "", className: "screenBuilderLabel"},
						{type: "newcolumn"},
						{type: "select", value: "", name: "CALENDAR_ACTIVE", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_SELECT_ACTIVE"), icon: "icon-select", options:this.getFormOptions("ACTIVE_INACTIVE")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "CALENDAR_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_CALENDAR_ID"), icon: "icon-input", required: true},
						{type: "newcolumn"},
						{type: "input", value: "", name: "CALENDAR_COMBO", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_CALENDAR_COMBO")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "CALENDAR_BUTTON", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_CALENDAR_BUTTON")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "CALENDAR_DEFAULT", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_CALENDAR_DEFAULT")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "CALENDAR_MODULE", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_CALENDAR_MODULE"), readonly: true}
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", name:"FILTER_OPTION", list: [
						{type: "block", width: "auto", list: [
							{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ACTIVE"), value: "", className: "screenBuilderLabel"},
							{type: "newcolumn"},
							{type: "select", value: "", name: "FILTER_ACTIVE", label:this._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_SELECT_ACTIVE"), icon: "icon-select", options:this.getFormOptions("ACTIVE_INACTIVE")},
							{type: "newcolumn"},
							{type: "select", label: this._TextData("SCREEN_BUILDER_MOCKUP_FILTERBAR_FILTER_ITEM_LABEL"), name: "FILTER_ADD_ITEM", hidden: true, options:this.getFormOptions("FILTERBAR_FILTER_ITEM")},
							{type: "newcolumn"},
							{type: "button", value: this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_CLEAR_ITEM"), name: "FILTER_CLEAR_ITEM", hidden: true}
						]},
						{type: "block", width: "auto", name: "FILTER_ITEMS", hidden: true, list:[]}
					]},
				],
				layout:this.getDesignOptions('FILTERBAR', ['auto_height', 'size', 'margin', 'align', 'custom']),
				event:this.getEventForm({
					onSearch:{NAME:"onSearch", ARGS:"flag", VALUE:"if(flag == \"filter\"){}else if(flag == \"search\"){}"}
				})
			}
		},
		list:function(){
			return {
				title:"list",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true},
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "select", value: "", name: "DRAG", label:this._TextData("SCREEN_BUILDER_MOCKUP_LIST_DRAG"), options:this.getFormOptions("SELECT_YN")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "DYNAMIC", label:this._TextData("SCREEN_BUILDER_MOCKUP_LIST_DYNAMIC")},
					]},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", inputWidth:"316",name: "TEMPLATE",label:this._TextData("SCREEN_BUILDER_MOCKUP_LIST_TEMPLATE"), rows:10}
					]}
				],
				layout:this.getDesignOptions('LIST', ['auto_height', 'size', 'margin', 'align', 'custom']),
				event:this.getEventForm({
					onItemClick:{NAME:"onItemClick" , ARGS:"id, ev, html", VALUE:"var item = this.get(id);"},
					onXLE:{NAME:"onXLE", ARGS:"", VALUE:"var rowCount = this.dataCount();"}
				})
			} 
		},
		dataview:function(){
			return {
				title:"dataview",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true}
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "block", width: "auto", list: [
							{type: "input", value: "", name: "WIDTH", label:this._TextData("SCREEN_BUILDER_MOCKUP_DATAVIEW_WIDTH"), options:this.getFormOptions("SELECT_YN")} ,
							{type: "newcolumn"},
							{type: "input", value: "", name: "HEIGHT", label:this._TextData("SCREEN_BUILDER_MOCKUP_DATAVIEW_HEIGHT")},
						]},
						{type: "input", value: "", name: "TEMPLATE",label:this._TextData("SCREEN_BUILDER_MOCKUP_DATAVIEW_TEMPLATE"), rows:10}
					]}
				],
				layout:this.getDesignOptions('DATAVIEW', ['auto_height', 'size', 'margin', 'align', 'custom']),
				event:this.getEventForm({
					onItemClick:{NAME:"onItemClick" , ARGS:"id, ev, html", VALUE:"var item = this.get(id);"},
					onXLE:{NAME:"onXLE", ARGS:"", VALUE:"var rowCount = this.dataCount();"}
				})
			} 
		},
		form:function(){
			return {
				title:"form",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true},
						{type: "input", value: "", inputWidth:"450" , name: "FORM_ID",label:this._TextData("SCREEN_BUILDER_MOCKUP_FORM_FORM_ID"), rows:10}
					]}
				],
				layout:this.getDesignOptions('LIST', ['auto_height', 'size', 'margin', 'align','custom']),
				event:this.getEventForm({
					onButtonClick:{NAME:"onButtonClick", ARGS:"name", VALUE:"switch(name) { case \"\":\n break; default:\n break;}"},
					onChange:{NAME:"onChange" , ARGS:"name, value, state", VALUE:"//state is checkboxes, radio, btn2state controls only\n switch(name) { case \"\":\n break; default:\n break; }"},
					onBeforeChange:{NAME:"onBeforeChange", ARGS:"name, oldValue, newValue", VALUE:"//if return false then not inserted new values\n switch(name) { case \"\":\n break; default:\n break; } return true;"},
					onXLE:{NAME:"onXLE", ARGS:"", VALUE:""}
				})
			} 
		},
		grid:function(){
			return {
				title:"grid",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true},
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "GRID_ID",label:this._TextData("SCREEN_BUILDER_MOCKUP_GRID_GRID_ID")},
						{type: "input", value: "", name: "APPEND_HEADER",label:this._TextData("SCREEN_BUILDER_MOCKUP_GRID_APPEND_HEADER")},
						{type: "select", name: "MERGE",label:this._TextData("SCREEN_BUILDER_MOCKUP_GRID_MERGE"), options:this.getFormOptions("SELECT_YN")},
						{type: "select", name: "DRAG",label:this._TextData("SCREEN_BUILDER_MOCKUP_GRID_DRAG"), options:this.getFormOptions("SELECT_YN")},
						{type: "select", name: "DYNAMIC",label:this._TextData("SCREEN_BUILDER_MOCKUP_GRID_DYNAMIC"), options:this.getFormOptions("SELECT_YN")},
						{type: "select", name: "EXCELDOWNLOAD", label:this._TextData("SCREEN_BUILDER_MOCKUP_GRID_EXCELDOWNLOAD"), options:this.getFormOptions("SELECT_YN")}
					]}
				],
				layout:this.getDesignOptions('GRID', ['auto_height', 'size', 'margin', 'align', 'custom']),
				event:this.getEventForm({
					onRowSelect:{NAME:"onRowSelect", ARGS:"id, ind", VALUE:"var header = this.getColumId();"},
					onEditCell:{NAME:"onEditCell" , ARGS:"stage, rId, cInd, nValue, oValue", VALUE:"var header = this.getColumId(); if(stage == 0){ return true; } else if(stage == 1){ return true; } else if(stage == 2 ){return true;} return false;"},
					onXLE:{NAME:"onXLE", ARGS:"obj, count", VALUE:"var msg = obj.getUserData(\"\", \"message\"); var status = obj.getUserData(\"\", \"status\"); if(status != \"true\"){ Alert(msg); }"},
					onTXE:{NAME:"onTXE", ARGS:"obj", VALUE:"var mode = obj.mode; var status = obj.status; var message = obj.message; if(status==\"true\"){ }else{ Alert(message); }"}
				})
			} 
		},
		container:function(){
			return {
				title:"container",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list:[
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true}
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list:[
						{type: "select", value: "", name: "COLUMN",label:this._TextData("SCREEN_BUILDER_MOCKUP_CONTAINER_COLUMN"), options:this.getFormOptions("NUMBER", [1, 6])},
						{type: "block", width: "auto", name: "COLUMN_ITEMS", hidden: true, list:[
							{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_CONTAINER_COLUMN_ITEMS")}
						]}
					]}
				],
				layout:this.getDesignOptions('CONTAINER', ['auto_height', 'size', 'margin', 'align', 'custom'])
			} 
		},
		column:function(){
			return {
				title:"containerCell",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list:[
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true}
					]}
				],
				layout:this.getDesignOptions('COLUMN', ['size_v', 'custom'])
			}
		},
		button:function(){
			return {
				title:"button",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
					{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
					{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "button", position:"label-left", value: this._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ADD_ITEM"), name: "ADD_ITEM", className: "ADD_ITEM"},
					]},
					{type: "block", width:"auto", name:"BUTTON_OPTION", list: [
						{type: "block", width: "auto", list: [
							{type: "block", width: "auto", name: "BUTTON_ITEMS", list:[]}
						]},
					]}
				]}],
				layout:this.getDesignOptions('CONTAINER', ['align_h', 'custom'])
			}
		},
		carousel:function(){
			return {
				title:"carousel",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true},
						{type: "block", width: "auto", name:"BUTTON_OPTION", list: [
							{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
							{type: "block", width: "auto", list: [
								{type: "button", value: this._TextData("SCREEN_BUILDER_MOCKUP_CAROUSEL_ADD_ITEM"), name: "ADD_ITEM"}
							]},
							{type: "block", width: "auto",name: "CAROUSEL_ITEMS", list:[]}
						]},
					]},
				],
				layout:this.getDesignOptions('LAYOUT', ['auto_height', 'size', 'margin', 'align', 'custom']),
				event:this.getEventForm({
					onSelect:{NAME:"onSelect", ARGS:"id", VALUE:"switch(id) { case: \"\":\n\tbreak; default:\n\tbreak; }"}
				})
			}
		},
		tabbar:function(){
			return {
				title:"tabbar",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true},
						{type: "block", width: "auto", name:"BUTTON_OPTION", className: "btnArea", list: [
							{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
							{type: "block", width: "auto", list: [
								{type: "button", value: this._TextData("SCREEN_BUILDER_MOCKUP_TABBAR_ADD_ITEM"), name: "ADD_ITEM"}
							]},
							{type: "block", width: "auto", name: "TABBAR_ITEMS", list:[]}
						]},
					]}
				],
				layout:this.getDesignOptions('LAYOUT', ['auto_height', 'size', 'margin', 'align', 'custom']),
				event:this.getEventForm({
					onTabChanged:{NAME:"onTabChanged", ARGS:"id", VALUE:"//callFunction('', '', []);\n switch(id) { case: \"\":\n\tbreak; default:\n\tbreak; }"},
					onSelect:{NAME:"onSelect", ARGS:"id", VALUE:"//callFunction('', '', []);\n switch(id) { case: \"\":\n\tbreak; default:\n\tbreak; }"}
				})
			}
		},
		vault:function(){
			return {
				title:"vault",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true},
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "TYPE",label:this._TextData("SCREEN_BUILDER_MOCKUP_VAULT_TYPE"), required:true},
						{type: "select", value: "", name: "MODE",label:this._TextData("SCREEN_BUILDER_MOCKUP_VAULT_MODE"), options:this.getFormOptions("VAULT_MODE")},
						{type: "input", value: "", name: "KEY",label:this._TextData("SCREEN_BUILDER_MOCKUP_VAULT_KEY")},
						{type: "select", value: "", name: "DND",label:this._TextData("SCREEN_BUILDER_MOCKUP_VAULT_DND"), options:this.getFormOptions("SELECT_YN")},
						{type: "input", value: "", name: "RESTRICTION",label:this._TextData("SCREEN_BUILDER_MOCKUP_VAULT_RESTRICTION")},
						{type: "block", className:"vault_add_method_bock", width: "auto", list: [
							{type: "input", value: "", name: "SERVICE",label:this._TextData("SCREEN_BUILDER_MOCKUP_VAULT_SERVICE")},
							{type: "button", value: this._TextData("SCREEN_BUILDER_MOCKUP_VAULT_ADD_METHOD"), name: "ADD_METHOD", width:"100"}
						]}
					]}
				],
				layout:this.getDesignOptions('LAYOUT', ['auto_height', 'size', 'margin', 'align_h', 'custom']),
				event:this.getEventForm({
					onUploadFile:{NAME:"onUploadFile", ARGS:"d, e, p", VALUE:"var attachKey = d.attach_key;\nvar fileCount = d.fileCount;"}
				})
			}
		},
		body:function(){
			return {
				title:"body",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), disabled:true},
						{type: "checkbox", label: this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_PAGE_OUT_WARNING"), name: "PAGE_CLOSE_WARN", className: "designHeight_chk"}
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "select", name: "FRAME_PRESET", label:this._TextData("SCREEN_BUILDER_MOCKUP_FRAME_PRESET_TYPE"), options:[
							{text:this._TextData("SCREEN_BUILDER_MOCKUP_FRAME_PRESET_FULL"), value:"FULL"},
							{text:this._TextData("SCREEN_BUILDER_MOCKUP_FRAME_PRESET_DEFAULT"), value:"DEFAULT"},
							{text:this._TextData("SCREEN_BUILDER_MOCKUP_FRAME_PRESET_LIST"), value:"LIST"}
						]}
					]}
				],
				layout:this.getDesignOptions('LAYOUT', ['scroll', 'content_min_width', 'auto_height', 'custom'])
			}
		},
		tree:function() {
			return {
				title: "Tree",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list: [
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true}
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list: [
						{type: "select", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_TREE_CHECKBOX"), name: "CHECKBOX", options:this.getFormOptions("SELECT_YN")},
					]}
				],
				layout:this.getDesignOptions('TREE', ['auto_height', 'size', 'margin', 'custom']),
				event:this.getEventForm({
					onSelect:{NAME:"onSelect", ARGS:"nodeId, mode", VALUE:"//if mode == false then node ID selected just before\nvar text, id, parentID; if(mode) { text = this.getUserData(nodeId, 'TEXT');\nid = this.getUserData(nodeId, 'ID');\nparentID = this.getUserData(nodeId, 'PARENTID'); }"},
					onCheck:{NAME:"onCheck", ARGS:"id, state", VALUE:"var chkNode, isChk;  chkNode = id; isChk = state;"},
					onXLE:{NAME:"onXLE", ARGS:"obj", VALUE:"var status = obj.status;\nvar msg = obj.message;\nif(status === 'false'){ Alert(msg); }\n"}
				})
			}
		},
		accordion:function() {
			return {
				title: "Accordion",
				component:[
					{type: "settings", position: "label-top", labelAlign: "left"},
					{type: "block", width: "auto", list:[
						{type: "input", value: "", name: "OBJECT_ID", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_OBJECT_ID"), disabled: true},
						{type: "input", value: "", name: "VAR", label:this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_VAR"), required:true}
					]},
					{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"},
					{type: "block", width: "auto", list:[
						{type: "input", value: "", name: "FLEXIBLE", label:this._TextData("SCREEN_BUILDER_MOCKUP_ACCORDION_FLEXIBLE")},
						{type: "select", value: "", name: "AUTOCLOSE", label:this._TextData("SCREEN_BUILDER_MOCKUP_ACCORDION_AUTOCLOSE"), options:this.getFormOptions("SELECT_YN")},
						{type: "button", value: this._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ADD_ITEM"), name: "ADD_ITEM"},
						{type: "block", width: "auto", name: "ACCORDION_ITEMS", list:[]}
					]}
				],
				layout:this.getDesignOptions('ACCORDION', ['auto_height', 'size', 'margin', 'align', 'custom']),
				event:this.getEventForm({
					onActive:{NAME:"onActive", ARGS:"id, state", VALUE:"if(state) {  }"},
				})
			}
		}
	}
	return get[t].apply(this,[]);
};
SepoaUXMockup.prototype.getEventForm=function(template){
	var select = [{text:"", value:""}, {text:"etc", value:"etc"}];
	for(var i in template){
		select.push({text:i, value:i});
	}
	
	return {
		form:[
			{type: "block", width: "auto", list: [
				{type: "select", name: "TEMPLATE", label:this._TextData("SCREEN_BUILDER_MOCKUP_EVENT_TEMPLATE"), options:select},
				{type: "block", width: "auto", list: [
					{type: "input", name: "NAME", label:this._TextData("SCREEN_BUILDER_MOCKUP_EVENT_NAME")},
					{type: "newcolumn"},
					{type: "select", name: "CALLBACK_TYPE", label:this._TextData("SCREEN_BUILDER_MOCKUP_EVENT_CALLBACK_TYPE"), options:[
						{text:"name", value:"name"},
						{text:"anonymous", value:"anonymous"}
					]},
					{type: "newcolumn"},
					{type: "input", hidden:false, name: "CALLBACK", label:this._TextData("SCREEN_BUILDER_MOCKUP_EVENT_CALLBACK")}
				]},
				{type: "input", name: "ARGS", label:this._TextData("SCREEN_BUILDER_MOCKUP_EVENT_ARGS")},
				{type: "container", name: "EDITOR_VALUE", label:this._TextData("SCREEN_BUILDER_MOCKUP_EVENT_CONTENTS")},
				{type: "hidden", name: "VALUE"},
				{type: "hidden", name: "id"},
			]}
		],
		template:template,
		baseInfo:this.baseInfo
	}
}
SepoaUXMockup.prototype.getDesignOptions=function(a, b){
	var get={
		margin:function(){
			return {type: "block", width: "auto", className: "designMargin", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_MARGIN"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "input", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_MARGIN_TOP"), name: "MARGIN_TOP"},
				{type: "newcolumn"},
				{type: "input", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_MARGIN_BOTTOM"), name: "MARGIN_BOTTOM"},
				{type: "newcolumn"},
				{type: "input", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_MARGIN_LEFT"), name: "MARGIN_LEFT"},
				{type: "newcolumn"},
				{type: "input", value: "", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_MARGIN_RIGHT"), name: "MARGIN_RIGHT"},
				{type: "newcolumn"},
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_"+a+"_DESCRIPTION_MARGIN"), value: ""}
			]}
		},
		size:function(){
			return {type: "block", width: "auto", className: "designSize", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_SIZE"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "input", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_SIZE_WIDTH"), value: "", name: "SIZE_WIDTH"},
				{type: "newcolumn"},
				{type: "input", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_SIZE_HEIGHT"), value: "", name: "SIZE_HEIGHT"},
				{type: "newcolumn"},
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_"+a+"_DESCRIPTION_SIZE"), value: ""}
			]}
		},
		size_v:function(){
			return {type: "block", width: "auto", className: "designSize_v", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_SIZE"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "input", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_SIZE_HEIGHT"), value: "", name: "SIZE_HEIGHT"},
				{type: "newcolumn"},
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_"+a+"_DESCRIPTION_SIZE"), value: ""}
			]}
		},
		size_h:function(){
			return {type: "block", width: "auto", className: "designSize_h", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_SIZE"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "input", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_SIZE_WIDTH"), value: "", name: "SIZE_WIDTH"},
				{type: "newcolumn"},
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_"+a+"_DESCRIPTION_SIZE"), value: ""}
			]}
		},
		auto_height:function(){
			return {type: "block", width: "auto", className: "designHeight", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_SIZE_AUTOHEIGHT"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "checkbox", value: "", name: "SIZE_AUTOHEIGHT", className: "label-hidden designHeight_chk"}
			]}
		},
		align_h:function(){
			return {type: "block", width: "auto", className: "designAlign_h", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_ALIGN"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "select", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_ALIGN_HORIZONTAL"), value: "", name: "ALIGN_HORIZONTAL", options:this.getFormOptions("ALIGN_HORIZONTAL")},
				{type: "newcolumn"},
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_"+a+"_DESCRIPTION_ALIGN"), value: ""}
			]}
		},
		align_v:function(){
			return {type: "block", width: "auto", className: "designAlign_v", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_ALIGN"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "select", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_ALIGN_VERTICAL"), value: "", name: "ALIGN_VERTICAL", options:this.getFormOptions("ALIGN_VERTICAL")},
				{type: "newcolumn"},
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_"+a+"_DESCRIPTION_ALIGN"), value: ""}
			]}
		},
		align:function(){
			return {type: "block", width: "auto", className: "designAlign", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_ALIGN"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "select", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_ALIGN_HORIZONTAL"), value: "", name: "ALIGN_HORIZONTAL", options:this.getFormOptions("ALIGN_HORIZONTAL")},
				{type: "newcolumn"},
				{type: "select", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_ALIGN_VERTICAL"), value: "", name: "ALIGN_VERTICAL", options:this.getFormOptions("ALIGN_VERTICAL")},
				{type: "newcolumn"},
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_"+a+"_DESCRIPTION_ALIGN"), value: ""}
			]}
		},
		custom:function(){
			return {type: "block", width: "auto", className: "designCustom", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_CUSTOM"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "input", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_CLASS"), value: "", name: "CLASS"},
			]}
		},
		scroll:function(){
			return {type: "block", width: "auto", className: "designScroll", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_SCREEN_SCROLL"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "checkbox", label: "", value: "", name: "SCREEN_SCROLL"}
			]}
		},
		content_min_width:function(){
			return {type: "block", width: "auto", className: "designContent_min_width", list: [
				{type: "label", label: this._TextData("SCREEN_BUILDER_MOCKUP_DESIGN_MIN_WIDTH"), value: "", className: "screenBuilderLabel"},
				{type: "newcolumn"},
				{type: "input", label: "", value: "", name: "CONTENT_MIN_WIDTH"},
			]}
		}
	}
	var result = [];
	var joiner = JSON.parse('{"type": "container", "width": "auto", "className": "AREA_DIVISION", "blockOffset": 0, "name": "AREA_DIVISION"}');
	for(var i in b){
		result.push(get[b[i]].apply(this, []));
		
		if((b.length - 1) != i) {
			result.push(joiner);
		}
	}
	return result;
}
// Base Type
suxItem_base=function(m, o, t){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	
	this.attachEvent('onPopupCallback', function(oid, r, so){
		if(r.event){
			var v = [];
			var c = [];
			var list = r.event.list;
			for(var i in list){
				var t = list[i];
				var w = {
					NAME:t.NAME,
					type:'event'
				}
				if(t.CALLBACK_TYPE == 'name'){
					w.CALLBACK = t.CALLBACK;
					c.push(t.CALLBACK);
				}else{
					w.CALLBACK = {
						ARGS:t.ARGS,
						VALUE:t.VALUE
					}
				}
				v.push(w);
			}
			
			this.options.events = v;
			
			if(c.length > 0){
				var p = r.event.user;
				var u = [];
				for(var i in c){
					var a = p[c[i]];
					if(a){
						a.NAME = c[i];
						a.type = 'function'
						u.push(a);
						delete p[c[i]];
					}
				}
				this.mockupObj.callEvent('onUpdateUserScript', [this.mockupObj.rootItem.options.component.VAR, u]);
			}
			delete r.event;
		}
	})
	
	this.drawBase=function(header){
		this.type = this.options.type;
		
		this.base = document.createElement('div');
		this.base.id = this.id;
		this.base.itemID = this.id;
		this.base.className="sepoaux_mockup_item_base sepoaux_mockup_item_base_"+this.type;
		
		if(header){
			var h = document.createElement('div');
			h.className="sepoaux_mockup_item_header";
			this.base.appendChild(h);
			h.innerHTML = "<div class='sepoaux_mockup_item_header_title'></div>";
			h.changeActiveScreenTabbarByHeader=function(){
				var b = getOAObject("SCREEN_BUILDER").builder
				var d = b.data;
				var path = b.component.infoForm.getValue("PATH");
				for(var i in d){
					for(var k in b.mockup[i]._objPool){
						ok = b.mockup[i]._objPool[k]
						if( ok.hasOwnProperty("options") ){
							if(ok["options"]["value"]==path){
								b.component.screenTabbar._setTabActive(i,true);
							}
						}
					}
				}
			}
			
			var screenPath = this.getPathByScreenId(this.options.component.VAR);
			if(screenPath) {
				var titleHeader = $(h).children('.sepoaux_mockup_item_header_title')[0];
				if(titleHeader) {
					function copyToClipboard(str) {
						var temp = document.createElement('textarea');
						temp.value = str;
						temp.style.top = '0';
						temp.style.left = '0';
						temp.style.position = 'fixed';
						
						h.appendChild(temp);
						
						temp.focus();
						temp.select();
						
						try {
							var flag = document.execCommand('copy');
							if(flag) {
								Message('Copy !', 500);
							}
						} catch(e) {
							console.log(e);
						} finally {
							h.removeChild(temp);
						}
					}
					
					titleHeader.addEventListener('dblclick', function(e) {
						copyToClipboard(screenPath);
					});
					
					var bc = document.createElement('div');
					bc.className = 'sepoaux_mockup_item_header_button sepoaux_mockup_item_header_button_title xi-document';
					bc.item = this;
					bc.onclick = function(){
						copyToClipboard(screenPath);
					}
					h.appendChild(bc);
				}
			}
			
			if(header.option){
				var bo = document.createElement('div');
				bo.className = 'sepoaux_mockup_item_header_button sepoaux_mockup_item_header_button_option xi-pen';
				bo.item = this;
				bo.onclick = function(){
					this.item.openOptionPopup();
				}
				h.appendChild(bo);
			}
			
			if(header.delete){
				var bd = document.createElement('div');
				bd.className = 'sepoaux_mockup_item_header_button sepoaux_mockup_item_header_button_delete xi-trash-o';
				bd.item = this;
				bd.onclick = function(){
					var parent = this;
					Confirm(Sepoa.cache.text.get('MESSAGE.1015'), function(flag) {
						if(flag) {
							// this.item.callEvent('onDelete', [this.item]);
							parent.item.callEvent('onDelete', [parent.item]);
							parent.item.mockupObj.callEvent('onUpdateItem', [parent.item.getOptions().type + " / "+ parent.item.getOptions().component.VAR + " / "+ parent.item.mockupObj._TextData("HISTORY_DELETE")]);
						}
					});

				}
				h.appendChild(bd);
			}
			
			if(header.parent){
				var bt = document.createElement('div');
				bt.className = 'sepoaux_mockup_item_header_button sepoaux_mockup_item_header_button_title xi-caret-up-circle-o';
				bt.item = this;
				bt.onclick = function(){
					this.parentElement.changeActiveScreenTabbarByHeader();
				}
				h.appendChild(bt);
			}
			
			this.changeTitle(header.title);
		}
		
		this.container = document.createElement('div');
		this.container.className="sepoaux_mockup_item_box sepoaux_mockup_item_"+this.type;
		this.container.item = this;
		
		if(header && header.drag){
			$(this.container).sortable({
				revert: 0,
				delay:100,
				connectWith:header.drag.connectWith || ".sepoaux_mockup_item_column, .sepoaux_mockup_item_body",
				placeholder: "sepoaux_mockup_item_base sepoaux_mockup_item_placeholder",
				items:"> div:not(.sepoaux_mockup_item_base_new)",
				start:function(event, ui){
					ui.placeholder.height(ui.item.outerHeight());
					ui.placeholder.css("marginTop", 15);
					ui.item[0].classList.add("sepoaux_mockup_item_draggable");
				},
				sort:function(event, ui){
					ui.helper.offset({top: event.clientY - 15, left:event.clientX - 100});
				},
				change:function(event, ui){
					var destListElement = $(ui.placeholder[0].parentElement)
					var _blank = destListElement.children(".sepoaux_mockup_item_base_new");
					if(_blank.length > 0 && _blank[0] != destListElement.children().last()[0]){
						_blank.appendTo(destListElement);
					}
				},
				stop:function(event, ui){
					ui.item[0].classList.remove("sepoaux_mockup_item_draggable");
					var parent = ui.item.parent()[0];
					if(this.item){
						if(this.item._beforeCellTransfer){
							var op = this.item._beforeCellTransfer(ui.item[0].itemID);
							if(parent.item && parent.item._afterCellTransfer){
								parent.item._afterCellTransfer(ui.item[0], op);
							}
						}
						var o = this.item.mockupObj._objPool[ui.item[0].itemID].getOptions();
						this.item.mockupObj.callEvent('onUpdateItem', [o.type + " / "+o.component.VAR + " / "+ this.item.mockupObj._TextData("HISTORY_MOVE")]);
					}
				}
			});
		}
		
		this.base.appendChild(this.container);
		this.base.onmouseover=function(e){
			this.classList.add("sepoaux_mockup_item_base_hover");
			e.stopPropagation();
		}
		this.base.onmouseout=function(){
			this.classList.remove("sepoaux_mockup_item_base_hover");
		}
		this.container.onclick = function(){
			this.chanageActiveScreenTabbarByItem();
			
		}
		this.container.chanageActiveScreenTabbarByItem=function(){
			if(this.classList.contains('sepoaux_mockup_item_box')){
				if(this.classList.contains('sepoaux_mockup_item_screen')){
					if(this.classList.contains('sepoaux_mockup_item_dummy')){
						
						var sId =  this.item.getScreenIdByPath(this.innerText.replaceAll("\r\n",""));
						getOAObject("SCREEN_BUILDER").builder.component.screenTabbar._setTabActive(sId,true)
					}
				}
			}
		}
	}
	this.changeTitle=function(t){
		t = t || this.type;
		var d = this.base.getElementsByClassName("sepoaux_mockup_item_header_title");
		this.title = t;
		if(d.length > 0 ){
			if(this.options.component.VAR && this.options.id != this.options.component.VAR){
				t = t+" ( "+this.options.component.VAR + " )";
				if(this.type == "body"){
					t = t+" "+this.getPathByScreenId(this.options.component.VAR);
				}
			}else if(t == "screen"){
				t = t+" ( "+this.getScreenIdByPath(this.options.value) + " )";
			}
			d[0].innerText = t;
		}
	}
	this.addChild=function(obj, c){
		this._childObj.push(obj.id);
		this.mockupObj._objPool[obj.id] = obj;
		(c || this.container).appendChild(obj.base);
		return obj.id;
	}
	this.removeChild=function(id, alive){
		var idx = this._childObj.indexOf(id);
		if(idx > -1){
			this.detachChildEvent(id)
			this._childObj.splice(idx, 1);
			this.mockupObj._objPool[id].base.parentElement.removeChild(this.mockupObj._objPool[id].base);
			
			if(!alive){
				this.mockupObj._objPool[id].clearAllChild();
				delete this.mockupObj._objPool[id];
			}
		}
	}
	this.clearAllChild=function(alive){
		var a = $.extend(true, [], this._childObj);
		for(var i in a){
			this.removeChild(a[i], alive)
		}
		this.container.innerHTML = "";
	}
	this.setOptions=function(o){
		this.options = $.extend(true, this.options, o);
		this.id = this.options.id
		
		if(this.options.component)
			for(var i in this.options.component){
				if(this.options.component[i] == "___DELETE_FLAG___"){
					delete this.options.component[i]
				}
			}
		if(this.options.layout)
			for(var i in this.options.layout){
				if(this.options.layout[i] == "___DELETE_FLAG___"){
					delete this.options.layout[i]
				}
			}
	}
	this.attachChildEvent=function(id, type, handler){
		if(!this._eventPool[id]){
			this._eventPool[id] = {};
		}
		var en = this.mockupObj._objPool[id].attachEvent(type, handler);
		this._eventPool[id][en]=type
	}
	this.detachChildEvent=function(id, _type){
		var events = this._eventPool[id];
		if(!events) return;
		for(var i in events){
			var type = events[i];
			if((!_type) || (_type && type == _type)){
				this.mockupObj._objPool[id].detachEvent(i);
			}
		}
	}
	this.openGuidePopup=function(params){
		var url = window.CONTEXT_NAME+"/Tools/ScreenBuilder/Popup/ComponentGuide.sux";
		var width = (SUX.getModuleTop().outerWidth * 0.4);
		var height = (SUX.getModuleTop().outerHeight * 0.8);
		var title = params.title;
		var code = params.code;
		
		title = title.substring(0,1).toUpperCase() + title.substring(1, title.length);
		
		if(Sepoa.window[code] && Sepoa.window[code].unload)
			Sepoa.window[code].unload();
		
		var mainContainer = SUX.getModuleTop().document.getElementById('container');
		var popup = Sepoa.window[code] = new SepoaDhtmlXWindowsObject();
		popup.setSkin('material');
		popup.createPopup(code || 'guidePopup',title,undefined,width,height);
		popup.setViewport(0,0,0,0,mainContainer);
		popup.attachURL(url, params);
		popup.setShow();
		return popup;
	};
	this.openOptionPopup=function(c){
		var url=window.CONTEXT_NAME+"/Tools/ScreenBuilder/Popup/ComponentOption.sux"
		var width = (SUX.getModuleTop().outerWidth * 0.8);
		var height = (SUX.getModuleTop().outerHeight * 0.8);
		var title = this.title || this.type;
		
		// 임시방편
		title = title.substring(0,1).toUpperCase() + title.substring(1, title.length);
		
		var params = {
			type:this.type,
			form:this.getFormTemplate(),
			options:this.getOptions(),
			callObj:this
		};
		
		this.callEvent('onBeforePopup', [params]);
		
		var code = this.id;
		if(Sepoa.window[code] && Sepoa.window[code].unload)
			Sepoa.window[code].unload();
		
		var mainContainer = SUX.getModuleTop().document.getElementsByClassName('container')[0];
		var popup = Sepoa.window[code] = new SepoaDhtmlXWindowsObject(null, 100);
		popup.setSkin('material');
		popup.createPopup(code || 'commonPopup',title,undefined,width,height);
		popup.setViewport(0,0,0,0,mainContainer);
		popup.attachURL(url, params);
		popup.setShow();
		return popup;
	}
	this.getFormTemplate=function(){
		return this.mockupObj.getFormTemplate(this.type);
	}
	this.getOptions=function(){
		return this.options
	}
	this.getChildTree=function(){
		var childTree = [];
		for(var i in this._childObj){
			var tc = this.mockupObj._objPool[this._childObj[i]];
			if(tc.getChildTree){
				childTree.push(tc.getChildTree());
			}
		}
		var a = {};
		a.id = this.id
		a.type = this.type
		if(childTree.length > 0)
			a.cells = childTree
		return a;
	}
	this.checkNewItem=function(){
		for(var i in this._childObj){
			var tc = this.mockupObj._objPool[this._childObj[i]];
			if(tc.type == 'new'){
				return true;
			}
		}
		return false;
	}
	this.getSortedItemArray=function(){
		var cdata = [];
		var tcdata = {};
		for(var i in this.options.component){
			var m = i.match(/^ITEM_(\w{0,})_(\w{0,})/);
			if(m){
				var ii = m[1];
				if(!tcdata[ii]){
					tcdata[ii] = {};
				}
				tcdata[ii][m[2]] = this.options.component[i];
				if(m[2] == "ID"){
					tcdata[ii].itemID = ii;
					cdata.push(tcdata[ii]);
				}
			}
		}
		tcdata = null;
		cdata.sort(function(a, b){
			var aS = parseInt(a.SORT)
			var bS = parseInt(b.SORT)
			return (aS<bS)?-1:(aS>bS)?1:0
		})
		return cdata;
	}
	this.mockupObj = m;
	this._childObj=[];
	this._eventPool={};
	this._typeOption=$.extend(true,{
		maxChildCount:0
	}, o);
	
	this.options= {
		layout:{},
		component:{},
		id:this.mockupObj.newId()
	}
	this.id = this.options.id;
	
	
	this.getScreenIdByPath=function(p){
		var s ="";
		var b = getOAObject("SCREEN_BUILDER").builder;
		var d = (b.mockup.hasOwnProperty("allData")? b.mockup.allData : b.data );
		if(d==undefined){
			d = getOAObject("SCREEN_BUILDER").builder.mockup.data;
		}
		for(var i in  d){
			if(d[i]["info"]==undefined){
				if(d[i]["PATH"]==p){
					s = i;
					break;
				}
			}else{
				if(d[i]["info"]["PATH"]==p){
					s = d[i]["info"]["PREFIX"]+"_"+d[i]["info"]["CODE"];
					break;
				}
			}
		}
		return s;
	}
	this.getPathByScreenId=function(s){
		var p ="";
		var b = getOAObject("SCREEN_BUILDER").builder;
		var d = (b.mockup.hasOwnProperty("allData")? b.mockup.allData : b.data);
		for(var i in  d){
			if(d[i]["info"]==undefined){
				if(i==s){
					p = d[i]["PATH"];
				}
			}else{
				if(d[i]["info"]["PREFIX"]+"_"+d[i]["info"]["CODE"]==s){
					p = d[i]["info"]["PATH"];
				}
			}
		}
		return p;
	}
}
// Body Type
suxItem_Body=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this._beforeCellTransfer=function(id){
		var y = this._childObj.indexOf(id);
		if(y >= 0){
			this._childObj.splice(y, 1);
		}
	}
	this._afterCellTransfer=function(){
		var childList = [];
		var newElem = null;
		for(var i=0;i<this.container.children.length;i++){
			var d = this.container.children[i].id;
			var k = this.mockupObj._objPool[d];
			if(k.type != "new"){
				childList.push(this.container.children[i].id);
			}else{
				newElem = k;
			}
		}
		if(newElem){
			childList.push(newElem.id);
			newElem.base.parentElement.insertBefore(newElem.base, null)
		}
		this._childObj = childList;
	}
	this.setOptions($.extend( true, {
		component:{
			COLUMN:1,
			VAR:this.id
		},
		layout:{
			SCROLL: ""
		},
		type:"body"
	},o || {}))
	
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		so.status = true;
	})
	
	this.drawBase({option:1, drag:true, parent:1});
	
	if(t && t.cells){
		for(i in t.cells){
			var co = t.cells[i];
			var a = this.addChild(new window["suxItem_"+co.type](this.mockupObj, this.mockupObj._objPool[co.id], co));
			var parent = this;
			this.attachChildEvent(a, "onDelete", function(){
				parent.callEvent("onBeforeChildDelete", [this])
				parent.removeChild(this.id);
				parent.draw();
			});
		}
	}
	this.draw();
}
suxItem_Body.prototype.draw=function(){
	if((!this.checkNewItem()) && ((!this.options.maxChildCount) || this.options.maxChildCount > this._childObj.length)){
		this.container.classList.add("sepoaux_mockup_base_has_blank")
		var cid = this.addChild(new suxItem_new(this.mockupObj, null));
		var parent = this;
		this.attachChildEvent(cid, "onDrop", function(id, o){
			if(o.type == "screen" && parent.type == "container"){
				Alert(this.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_MESSAGE_0001"));
				return;
			}
			if(!window["suxItem_"+o.type]){
				Alert("not supported object");
				return;
			}
			parent.removeChild(id);
			
			var i = parent.addChild(new window["suxItem_"+o.type](this.mockupObj, o));
			parent.draw();
			
			parent.attachChildEvent(i, "onDelete", function(){
				parent.callEvent("onBeforeChildDelete", [this])
				parent.removeChild(this.id);
				parent.draw();
			});
			parent.callEvent("onDropChildObject", [o.value?"S":"C" , o.value || i]);
		});
		
		this.attachChildEvent(cid, "onDelete", function(id, o){
			parent.callEvent("onBeforeChildDelete", [this])
			parent.removeChild(this.id);
			parent.draw();
		});
	}else{
		this.container.classList.remove("sepoaux_mockup_base_has_blank")
	}
}
// Cell Type
suxItem_cell=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.setOptions($.extend(true, {}, o))
}
suxItem_cell.prototype = suxItem_Body.prototype;
// Container Type
suxItem_container=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.setOptions($.extend( true, {
		component:{
			COLUMN:1,
			VAR:this.id
		},
		type:"container"
	},o || {}))
	
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		formObj_C.attachEvent("onChange", function(name, value){
			if(name == "COLUMN"){
				var t = [];
				var that = this;
				this.forEachItem(function(n){
					var itemType = that.getItemType(n);
					if(itemType == 'block' && n.indexOf("COLUMN_ITEM_INNER") == 0){
						t.push(n);
					}
				})
				for(var i in t){
					this.removeItem(t[i]);
				}
				var childList = [];
				for(var i=0;i<value;i++){
					if(i != 0){
						childList.push({type: "newcolumn"})
					}
					childList.push({type: "input", value: "1", name: "COLUMN_ITEM_"+i, label:(i+1)+"", inputWidth:80})
				}
				this.addItem('COLUMN_ITEMS', {type: "block", width: "auto", blockOffset: "0", name:"COLUMN_ITEM_INNER", list:childList})
				if(value > 1){
					this.showItem("COLUMN_ITEMS");
				}else{
					this.hideItem("COLUMN_ITEMS");
				}
				sObj.setSize();
			}
		})
		
		formObj_C.attachEvent("onXLE", function(){
			this.setItemValue('PATTERN_SAMPLE', "1")
		})
		
		if(this.options.component.COLUMN){
			formObj_C.callEvent("onChange", ["COLUMN", this.options.component.COLUMN])
		}
	});
	
	this.attachEvent('onPopupCallback', function(id, data, so){
		var reDrawFlag = this.options.component.COLUMN != data.component.COLUMN
		
		for(var i in this.options.component){
			if(i.indexOf("COLUMN_ITEM_") == 0){
				delete this.options.component[i]
			}
		}
		
		this.setOptions(data);
		if(reDrawFlag){
			var data = {}
			for(var i in this._childObj){
				var cObj = this.mockupObj._objPool[this._childObj[i]];
				data[cObj.options.name] = cObj
			}
			this.childCellInfo = {};
			this.clearAllChild(true);
			this.draw(data);
		}
		this.changeTitle();
		
		so.status=true
	})
	this.drawBase({option:1, delete:1});
	
	if(t){
		this.childCellInfo = {}
		for(var i in t.cells){
			var d = t.cells[i];
			var options = this.mockupObj._objPool[d.id];
			
			this.childCellInfo[options.name] = {
				obj:options, tree:d
			}
		}
	}
	
	this.draw();
}
suxItem_container.prototype.draw=function(cData){
	var column_wrapper = document.createElement('div');
	column_wrapper.className = "sepoaux_mockup_column_wrapper";
	this.container.appendChild(column_wrapper);
	
	var column_ratio = this.options.component.COLUMN;
	
	for(var i=0;i<column_ratio;i++){
		var name = String.fromCharCode(97 + i)
		var r = {name:name}
		var t = null;
		
		var c = document.createElement('div');
		column_wrapper.appendChild(c);
		c.className = "sepoaux_mockup_column sepoaux_mockup_column_1_"+column_ratio;
		
		if(cData && cData[name]){
			this.addChild(cData[name], c);
			delete cData[name]
			continue;
		}else if(this.childCellInfo && this.childCellInfo[name]){
			r = this.childCellInfo[name]
			t = r.tree
			r = r.obj || {name:name}
		}
		this.addChild(new suxItem_column(this.mockupObj, r, t), c);
	}
	delete this.childCellInfo;
	if(cData){
		for(var i in cData){
			var id = cData[i].id
			this.mockupObj._objPool[id].clearAllChild();
			delete this.mockupObj._objPool[id];
		}
	}
}

// New Type
suxItem_new=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.getChildTree = null;
	this.setOptions(o || {type:"new"})
	this.drawBase();
	
	var d = document.createElement('div');
	this.container.appendChild(d);
	$(d).css("width", "auto")
		.css("display", "flex");
	d.innerHTML = "<div style='margin-top: 4px; margin-left: 0px;' class='screen_add_button_item'></div><div class='sepoaux_mockup_textbox'>"+ SepoaUXMockup.prototype._TextData("SCREEN_BUILDER_MOCKUP_NEW_MOCKUP_TEXT")+"</div>"
	
	this.container.ondragover=function(e){
		e.preventDefault();
	}
	this.container.ondrop=function(e){
		if(e.dataTransfer){
			e.preventDefault();
			var type = e.dataTransfer.getData('type').toLowerCase();
			var value = e.dataTransfer.getData('value');
			var p = {type:type}
			if(value) p.value = value;
			this.item.callEvent('onDrop', [this.parentElement.id, p]);
			this.item.mockupObj.callEvent('onUpdateItem', [type + " / "+ this.item.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_HISTORY_NEW")]);
		}
	}
}
// Dummy Type
suxItem_dummy=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.setOptions($.extend(true, {type:"dummy"}, o));
	
	this.drawBase(o.header);
	
	if(o.type != "dummy"){
		this.container.className += " sepoaux_mockup_item_dummy"
	}
	
	this.getChildTree = null;
	
	var d = document.createElement('div');
	this.container.appendChild(d);
	d.innerHTML = "<div class='sepoaux_mockup_textbox'>"+ o.value +"</div>"
	
	this.changeText=function(t){
		d.children[0].innerText = t;
	}
}
// Screen Type
suxItem_screen=function(m, o, t){
	suxItem_dummy.call(this, m,$.extend(true, {type:"screen", header:{delete:1, title:"screen"}}, o || {}), t);
	this.setValue=function(v){
		this.options.value = v;
		this.changeText(v);
		this.callEvent("onChange", [v]);
	}
	this.getValue=function(){
		return this.options.value;
	}
}
// Carousel Type
suxItem_carousel=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this._beforeCellTransfer=function(id){
		var result = {};
		var r = new RegExp("^ITEM_"+id+"_(\\w{0,})");
		for(var i in this.options.component){
			var f = i.match(r);
			if(f){
				result[f[1]] = this.options.component[i];
				delete this.options.component[i];
			}
		}
		this.removeChild(id)
		return result;
	}
	this._afterCellTransfer=function(elem, op){
		var num = this.mockupObj.newId().toUpperCase();
		elem.itemID = num;
		for(var i in op){
			this.options.component["ITEM_"+num+"_"+i] = op[i];
		}
		var b = this.container.children;
		for(var i=0;i<b.length;i++){
			this.options.component["ITEM_"+b[i].itemID+"_SORT"] = i+1+"";
		}
		this.draw();
	}
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		var that = this;
		var getDynamicFormOption = function(num){
			return {type: "block", width: "auto", name:"ITEM_"+num, list: [
				{type: "block", width: "auto", list: [
					{type: "input", value: "", name: "ITEM_"+num+"_ID", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},
					{type: "newcolumn"},
					{type: "select", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_RENDER"), name: "ITEM_"+num+"_RENDER", options:that.mockupObj.getFormOptions('RENDER_TYPE')},
					{type: "newcolumn"},
					{type: "button", value: "", name: "ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA DELETE_AREA_CENTER"}
				]},
				{type: "block", width: "auto", list: [
					{type: "select", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_TYPE"), name: "ITEM_"+num+"_TYPE", options:that.mockupObj.getFormOptions("ATTACH_TYPE")},
					{type: "newcolumn"},
					{type: "input", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_TARGET"), name: "ITEM_"+num+"_TARGET"},
					{type: "newcolumn"},
					{type: "input", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_ALIAS"), name: "ITEM_"+num+"_ALIAS"},
					{type: "newcolumn"},
					{type: "input", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_MODULE"), name: "ITEM_"+num+"_MODULE", readonly: true},
				]},
				{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"}
			]}
		}
		
		var cdata = this.getSortedItemArray();
		for (var i in cdata){
			var options = getDynamicFormOption(cdata[i].itemID);
			formObj_C._apiConnector('addItem',['CAROUSEL_ITEMS', options])
		}
		formObj_C.attachEvent('onButtonClick', function(name){
			if(name == "CLEAR_ITEM"){
				var t = [];
				formObj_C.forEachItem(function(obj, n){
					var itemType = obj.getItemType(n);
					if(itemType == 'block' && n.indexOf("CAROUSEL_ITEM_") == 0){
						t.push(n);
					}
				})
				for(var i in t){
					this.removeItem(t[i]);
				}
			}else if(name == "ADD_ITEM"){
				var num = that.mockupObj.newId().toUpperCase();
				var options = getDynamicFormOption(num);
				formObj_C._apiConnector('addItem',['CAROUSEL_ITEMS', options])
			}else if(name.indexOf("_DELETE") + 1){
				name = name.replaceAll("_DELETE", "");
				for(var i in this._indexId){
					if(this._indexId[i].startsWith(name)){
						if(this.getFormData()[this._indexId[i]]){
							this.setItemValue(this._indexId[i], "___DELETE_FLAG___");
						}
					}
				}				
				that.options.component[name + "_SORT"] = "___DELETE_FLAG___";

				this.hideItem(name);
			}
			sObj.setSize();
		})
		formObj_C.attachEvent('onBeforeChange', function(name, oValue, nValue){
			if(name.match(/^ITEM_(\w{0,})_ID/) && nValue){
				var flag = false;
				formObj_C.forEachItem(function(obj, n){
					if(n != name && n.match(/^ITEM_(\w{0,})_ID/)){
						if(obj.getItemValue(n) == nValue){
							Alert(nValue+that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_MESSAGE_0002"));
							flag = true;
						}
					}
				})
				if(flag){
					return false;
				}
			}
			return true;
		});
		formObj_C.attachEvent("onFocus", function(name){
			var m = name.match(/^ITEM_(\w{0,})_(\w{0,})$/);
			if(m) {
				var id = m[1];
				if(m[2] == 'MODULE'){
					var moduleSelectCallback = function(name, value) {
						formObj_C.setValue(name, value);
					}
					PopupOpenPost(window.CONTEXT_NAME + '/Admin/ModuleConfiguration/AdminModuleConfigurationSelectPopup.sux', {target: m[0], selectedValue: formObj_C.getValue(m[0])}, moduleSelectCallback, 800, null, that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_MODULE_SELECT"), 'SELECT_MODULE');
				}
			}
		});
	});
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		this.draw();
		var b = this.container.children;
		for(var i=0;i<b.length;i++){
			this.options.component["ITEM_"+b[i].itemID+"_SORT"] = i+1+"";
		}
		so.status = true
	})	
	this.setOptions($.extend(true, {
		component:{
			VAR:this.id
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: "",
			SIZE_AUTOHEIGHT: "1"
		},
		type:"carousel"
	}, o || {}));
	this.drawBase({option:1, delete:1, drag:{connectWith:".sepoaux_mockup_item_carousel"}});
	this.draw();
}
suxItem_carousel.prototype.draw=function(){
	this.clearAllChild();
	this.container.innerHTML = "";
	
	var cdata = this.getSortedItemArray();
	
	for(var i in cdata){
		var d = document.createElement('div');
		this.container.appendChild(d);
		d.itemID = cdata[i].itemID;
		
		var t = null;
		if(cdata[i].TARGET && cdata[i].TYPE){
			t = {};
		}
		
		var a = this.addChild(new suxItem_carouselCell(this.mockupObj, {name:cdata[i].ID, id:cdata[i].itemID, component:cdata[i]}, t), d);
		var parent = this;
		this.attachChildEvent(a, "onChange", function(opt){
			var u = opt.component
			parent.options.component["ITEM_"+opt.id+"_ID"] = u.ID
			parent.options.component["ITEM_"+opt.id+"_TYPE"] = u.TYPE
			parent.options.component["ITEM_"+opt.id+"_TARGET"] = u.TARGET
			parent.options.component["ITEM_"+opt.id+"_ALIAS"] = u.ALIAS
		});		
	}
}
// Carousel dummy Type
suxItem_carouselCell=function(m, o, t){
	suxItem_cell.call(this, m, null);
	this._beforeCellTransfer=function(id){
		var op = this.options.component;
		var result = {
			TYPE:op.TYPE,
			TARGET:op.TARGET,
			ALIAS:op.ALIAS
		}
		this.callEvent('onBeforeChildDelete', [])
		this.removeChild(id);
		this.draw();
		return result;
	}
	this._afterCellTransfer=function(elem, op){
		this.clearAllChild();
		this.options.component.TARGET = op.TARGET;
		this.options.component.TYPE = op.TYPE;
		this.options.component.ALIAS = op.ALIAS;
		
		var target = this.options.component.TARGET;
		if(this.options.component.ALIAS){
			target += ", alias : "+this.options.component.ALIAS;
		}
		var i = this.addChild(new suxItem_screen(this.mockupObj, {value:target}, null));
		var parent = this;
		this.attachChildEvent(i, "onDelete", function(){
			parent.callEvent('onBeforeChildDelete', [])
			parent.removeChild(this.id);
			parent.draw();
		});
		this.attachChildEvent(i, "onChange", function(v){
			parent.options.component.TARGET=v;
			parent.callEvent('onChange', [parent.options]);
		});
		this.callEvent('onChange', [this.options])
	}
	this.attachEvent('onDropChildObject', function(a, t){
		this.setOptions({component:{TYPE:a, TARGET:t}})
		this.callEvent('onChange', [this.options]);
	})
	this.attachEvent('onBeforeChildDelete', function(){
		this.setOptions({component:{TYPE:"C", TARGET:""}})
		this.callEvent('onChange', [this.options]);
	})
	
	var defaultOption = {maxChildCount:1, type:"carouselCell", component:{}};
	
	this.setOptions($.extend(true, defaultOption, o))
	this.drawBase({drag:{connectWith:".sepoaux_mockup_item_carouselCell.sepoaux_mockup_base_has_blank"}});
	this.changeTitle(o.name + ((o.component.SELECTED == 'true')?(" - selected"):""));
	
	if(t){
		var target = this.options.component.TARGET;
		if(this.options.component.ALIAS){
			target += ", alias : "+this.options.component.ALIAS;
		}
		var i = this.addChild(new suxItem_screen(this.mockupObj, {value:target}, t));
		var parent = this;
		this.attachChildEvent(i, "onDelete", function(){
			parent.callEvent('onBeforeChildDelete', [])
			parent.removeChild(this.id);
			parent.draw();
		});
		this.attachChildEvent(i, "onChange", function(v){
			parent.options.component.TARGET=v;
			parent.callEvent('onChange', [parent.options]);
		});
	}
	this.draw();
}
suxItem_carouselCell.prototype = suxItem_cell.prototype;
// Tabbar Type
suxItem_tabbar=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this._beforeCellTransfer=function(id){
		var result = {};
		var r = new RegExp("^ITEM_"+id+"_(\\w{0,})");
		for(var i in this.options.component){
			var f = i.match(r);
			if(f){
				result[f[1]] = this.options.component[i];
				delete this.options.component[i];
			}
		}
		this.removeChild(id)
		return result;
	}
	this._afterCellTransfer=function(elem, op){
		var num = this.mockupObj.newId().toUpperCase();
		elem.itemID = num;
		for(var i in op){
			this.options.component["ITEM_"+num+"_"+i] = op[i];
		}
		var b = this.container.children;
		for(var i=0;i<b.length;i++){
			this.options.component["ITEM_"+b[i].itemID+"_SORT"] = i+1+"";
		}
		this.draw();
	}
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		var that = this;
		var getDynamicFormOption = function(num){
			return {type: "block", width: "auto", name:"ITEM_"+num, list: [
				{type: "block", width: "auto", list:[
					{type: "select", value: "", name: "ITEM_"+num+"_SELECTED", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_SELECTED"), options:that.mockupObj.getFormOptions("SELECT_YN")},
					{type: "newcolumn"},
					{type: "input", value: "", name: "ITEM_"+num+"_ID", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "ITEM_"+num+"_NAME", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_TABBAR_ITEM_NAME"), required:true},
					{type: "newcolumn"},
					{type: "select", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_TYPE"), name: "ITEM_"+num+"_TYPE", options:that.mockupObj.getFormOptions("ATTACH_TYPE")},
					{type: "newcolumn"},
					{type: "button", value: "", name: "ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA DELETE_AREA_CENTER"}
				]},
				{type: "block", width: "auto", list:[
					{type: "input", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_TARGET"), name: "ITEM_"+num+"_TARGET"},
					{type: "newcolumn"},
					{type: "input", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_ALIAS"), name: "ITEM_"+num+"_ALIAS"},
					{type: "newcolumn"},
					{type: "select", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_RENDER"), name: "ITEM_"+num+"_RENDER", options:that.mockupObj.getFormOptions('RENDER_TYPE')},
					{type: "newcolumn"},
					{type: "input", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_MODULE"), name: "ITEM_"+num+"_MODULE", readonly: true}
				]},
				{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"}
			]}
		}
		
		var cdata = this.getSortedItemArray();
		for (var i in cdata){
			var options = getDynamicFormOption(cdata[i].itemID);
			formObj_C._apiConnector('addItem',['TABBAR_ITEMS', options])
		}
		formObj_C.attachEvent('onButtonClick', function(name){
			if(name == "CLEAR_ITEM"){
				var t = [];
				formObj_C.forEachItem(function(obj, n){
					var itemType = obj.getItemType(n);
					if(itemType == 'block' && n.indexOf("TABBAR_ITEM_") == 0){
						t.push(n);
					}
				})
				for(var i in t){
					this.removeItem(t[i]);
				}
			}else if(name == "ADD_ITEM"){
				var num = that.mockupObj.newId().toUpperCase();
				var options = getDynamicFormOption(num);
				formObj_C._apiConnector('addItem',['TABBAR_ITEMS', options])
			}else if(name.indexOf("_DELETE") + 1){
				name = name.replaceAll("_DELETE", "")
				for(var i in this._indexId){
					if(this._indexId[i].startsWith(name)){
						if(this.getFormData()[this._indexId[i]]){
							this.setItemValue(this._indexId[i], "___DELETE_FLAG___");
						}
					}
				}
				that.options.component[name + "_SORT"] = "___DELETE_FLAG___";

				this.hideItem(name);
			}
			sObj.setSize();
		});
		formObj_C.attachEvent('onBeforeChange', function(name, oValue, nValue){
			if(name.match(/^ITEM_(\w{0,})_ID/) && nValue){
				var flag = false;
				formObj_C.forEachItem(function(obj, n){
					if(n != name && n.match(/^ITEM_(\w{0,})_ID/)){
						if(obj.getItemValue(n) == nValue){
							Alert(nValue+that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_MESSAGE_0002"));
							flag = true;
						}
					}
				})
				if(flag){
					return false;
				}
			}
			return true;
		});
		formObj_C.attachEvent("onFocus", function(name){
			var m = name.match(/^ITEM_(\w{0,})_(\w{0,})$/);
			if(m) {
				var id = m[1];
				if(m[2] == 'MODULE'){
					var moduleSelectCallback = function(name, value) {
						formObj_C.setValue(name, value);
					}
					PopupOpenPost(window.CONTEXT_NAME + '/Admin/ModuleConfiguration/AdminModuleConfigurationSelectPopup.sux', {target: m[0], selectedValue: formObj_C.getValue(m[0])}, moduleSelectCallback, 800, null, that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_MODULE_SELECT"), 'SELECT_MODULE');
				}
			}
		});
	});
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		this.draw();
		this.changeTitle();
		var b = this.container.children;
		for(var i=0;i<b.length;i++){
			this.options.component["ITEM_"+b[i].itemID+"_SORT"] = i+1+"";
		}
		so.status = true
	})	
	this.setOptions($.extend(true, {
		component:{
			VAR:this.id
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: "",
			SIZE_AUTOHEIGHT: "1"
		},
		type:"tabbar"
	}, o || {}));
	this.drawBase({option:1, delete:1, drag:{connectWith:".sepoaux_mockup_item_tabbar"}});
	this.draw();
}
suxItem_tabbar.prototype.draw=function(){
	this.clearAllChild();
	this.container.innerHTML = "";
	
	var cdata = this.getSortedItemArray();
	
	for(var i in cdata){
		var d = document.createElement('div');
		this.container.appendChild(d);
		d.itemID = cdata[i].itemID;
		
		var t = null;
		if(cdata[i].TARGET && cdata[i].TYPE){
			t = {};
		}
		
		var a = this.addChild(new suxItem_tabbarCell(this.mockupObj, {name:cdata[i].ID, id:cdata[i].itemID, component:cdata[i]}, t), d);
		var parent = this;
		this.attachChildEvent(a, "onChange", function(opt){
			var u = opt.component
			console.log(parent.options.component);
			parent.options.component["ITEM_"+opt.id+"_ID"] = u.ID
			parent.options.component["ITEM_"+opt.id+"_TYPE"] = u.TYPE
			parent.options.component["ITEM_"+opt.id+"_TARGET"] = u.TARGET
			parent.options.component["ITEM_"+opt.id+"_ALIAS"] = u.ALIAS
			parent.options.component["ITEM_"+opt.id+"_MODULE"] = u.MODULE
		});		
	}
}
// Tabbar dummy Type
suxItem_tabbarCell=function(m, o, t){
	suxItem_cell.call(this, m, null);
	this._beforeCellTransfer=function(id){
		var op = this.options.component;
		var result = {
			TYPE:op.TYPE,
			TARGET:op.TARGET,
			ALIAS:op.ALIAS,
			MODULE:op.MODULE
		}
		this.callEvent('onBeforeChildDelete', [])
		this.removeChild(id);
		this.draw();
		return result;
	}
	this._afterCellTransfer=function(elem, op){
		this.clearAllChild();
		this.options.component.TARGET = op.TARGET;
		this.options.component.TYPE = op.TYPE;
		this.options.component.ALIAS = op.ALIAS;
		this.options.component.MODULE = op.MODULE;
		
		var target = this.options.component.TARGET;
		if(this.options.component.ALIAS){
			target += ", alias : "+this.options.component.ALIAS;
		}
		var i = this.addChild(new suxItem_screen(this.mockupObj, {value:target}, null));
		var parent = this;
		this.attachChildEvent(i, "onDelete", function(){
			parent.callEvent('onBeforeChildDelete', [])
			parent.removeChild(this.id);
			parent.draw();
		});
		this.attachChildEvent(i, "onChange", function(v){
			parent.options.component.TARGET=v;
			parent.callEvent('onChange', [parent.options]);
		});
		this.callEvent('onChange', [this.options])
	}
	this.attachEvent('onDropChildObject', function(a, t){
		this.setOptions({component:{TYPE:a, TARGET:t}})
		this.callEvent('onChange', [this.options]);
	})
	this.attachEvent('onBeforeChildDelete', function(){
		this.setOptions({component:{TYPE:"C", TARGET:""}})
		this.callEvent('onChange', [this.options]);
	})
	
	var defaultOption = {maxChildCount:1, type:"tabbarCell", component:{}};
	
	this.setOptions($.extend(true, defaultOption, o))
	this.drawBase({drag:{connectWith:".sepoaux_mockup_item_tabbarCell.sepoaux_mockup_base_has_blank"}});
	this.changeTitle(o.name + (o.component.NAME?(" [ "+o.component.NAME+" ] "):"") + ((o.component.SELECTED == 'true')?(" - selected"):""));
	
	if(t){
		var target = this.options.component.TARGET;
		if(this.options.component.ALIAS){
			target += ", alias : "+this.options.component.ALIAS;
		}
		var i = this.addChild(new suxItem_screen(this.mockupObj, {value:target}, t));
		var parent = this;
		this.attachChildEvent(i, "onDelete", function(){
			parent.callEvent('onBeforeChildDelete', [])
			parent.removeChild(this.id);
			parent.draw();
		});
		this.attachChildEvent(i, "onChange", function(v){
			parent.options.component.TARGET=v;
			parent.callEvent('onChange', [parent.options]);
		});
	}
	this.draw();
}
suxItem_tabbarCell.prototype = suxItem_cell.prototype;
// Layout Type
suxItem_layout=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.setOptions($.extend( true, {
		component:{
			PATTERN:"1C",
			VAR:this.id
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: "",
			SIZE_AUTOHEIGHT: "1"
		},
		type:"layout"
	}, o || {}));
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		formObj_C.attachEvent("onXLE", function(){
			this.setItemValue('PATTERN_SAMPLE', "1")
		})
	});
	this.attachEvent('onPopupCallback', function(id, data, so){
		var reDrawFlag = this.options.component.PATTERN != data.component.PATTERN;
		this.setOptions(data);
		if(reDrawFlag){
			var data = {}
			for(var i in this._childObj){
				var cObj = this.mockupObj._objPool[this._childObj[i]];
				data[cObj.options.name] = cObj
			}
			this.clearAllChild(true);
			this.draw(data);
		}
		this.changeTitle();
		
		so.status = true
	})
	this.drawBase({option:1, delete:1});
	if(t && t.cells){
		this.childCellInfo = {}
		
		for(var i in t.cells){
			var d = t.cells[i];
			var options = this.mockupObj._objPool[d.id];
			
			this.childCellInfo[options.name] = {
				obj:options, tree:d
			}
		}
	}
	this.draw();
}
suxItem_layout.prototype.draw=function(t){
	var p = this.options.component.PATTERN
	var cellData = dhtmlXLayoutObject.prototype.tplData[p];
	this._drawCell(cellData, this.container)
}
suxItem_layout.prototype._drawCell=function(o, p){
	var mode = o.mode;
	var parent = this;
	
	var column_wrapper = null;
	var column_ratio = 0;
	for(var i in o.cells){
		var a = o.cells[i];
	
		if(a.name){
			var r = {name:a.name}
			var t = null;
			if(this.childCellInfo && this.childCellInfo[a.name]){
				r = this.childCellInfo[a.name]
				t = r.tree
				r = r.obj || {name:a.name}
			}
			
			if(mode == 'c' || mode == 'h'){
				if(column_wrapper == null){
					column_wrapper = document.createElement('div');
					column_wrapper.className = "sepoaux_mockup_column_wrapper";
					p.appendChild(column_wrapper);
				}
				
				var c = document.createElement('div');
				column_wrapper.appendChild(c);
				c.className = "sepoaux_mockup_column sepoaux_mockup_column_1_1";
				
				this.addChild(new suxItem_layoutCell(this.mockupObj, r, t), c);
			}else if(mode == 'v'){
				if(column_wrapper == null){
					column_wrapper = document.createElement('div');
					column_wrapper.className = "sepoaux_mockup_column_wrapper";
					p.appendChild(column_wrapper);
				}
				
				var column_ratio = float2fraction(a.width) || 2;
				
				var c = document.createElement('div');
				column_wrapper.appendChild(c);
				c.className = "sepoaux_mockup_column sepoaux_mockup_column_1_"+column_ratio;
				
				this.addChild(new suxItem_layoutCell(this.mockupObj, r, t), c);
			}
		}else{
			var c = document.createElement('div');
			p.appendChild(c);
			c.className = "sepoaux_mockup_cell_wrapper";
			if(mode == 'v'){
				if(!column_ratio) column_ratio = 2; 
				c.className += " sepoaux_mockup_column sepoaux_mockup_column_"+(column_ratio - 1)+"_"+column_ratio; 
			}
			this._drawCell(a.layout, c);
		}
	}
}
// Filter Type
suxItem_filterbar=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.setOptions($.extend( true, {
		component:{
			VAR:this.id,
			KEYWORD_ACTIVE:"true",
			KEYWORD_ID:"F_KEYWORD",
			STATUS_ACTIVE:"false",
			STATUS_ID:"F_STATUS",
			CALENDAR_ACTIVE:"false",
			CALENDAR_ID:"F_CALENDAR",
			FILTER_ACTIVE:"false"
		},
		type:"filterbar"
	}, o || {}));
	this.drawBase({option:1, delete:1});
	this.optionCell=['KEYWORD', 'STATUS', 'FILTER', 'CALENDAR'];
	
	var that = this;
	var getDynamicFormOption = function(num, type){
		var options = null;
		switch(type){
		case "text":
			options = 
				{type: "block", width: "auto", name:"FILTER_ITEM_"+num, list: [
					{type: "label", value: "", name: "FAKE_AREA", labelWidth: "135"},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_TYPE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_TYPE"), value:"text", disabled:true, readonly:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_ID", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_DEFAULT", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_DEFAULT")},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_MODULE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_MODULE"), readonly: true},
					{type: "newcolumn"},
					{type: "button", value: "", name: "FILTER_ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA"}
				]}
			break;
		case "select":
			options = 
				{type: "block", width: "auto", name:"FILTER_ITEM_"+num, list: [
					{type: "label", value: "", name: "FAKE_AREA", labelWidth: "135"},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_TYPE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_TYPE"), value:"select", disabled:true, readonly:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_ID", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_COMBO", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_COMBO"), required:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_DEFAULT", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_DEFAULT")},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_MODULE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_MODULE"), readonly: true},
					{type: "newcolumn"},
					{type: "button", value: "", name: "FILTER_ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA"}
				]}
			break;
		case "calendar":
			options = 
				{type: "block", width: "auto", name:"FILTER_ITEM_"+num, list: [
					{type: "label", value: "", name: "FAKE_AREA", labelWidth: "135"},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_TYPE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_TYPE"), value:"calendar", disabled:true, readonly:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_ID", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_BUTTON", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_CALENDAR_BUTTON")},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_DEFAULT", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_DEFAULT")},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_MODULE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_MODULE"), readonly: true},
					{type: "newcolumn"},
					{type: "button", value: "", name: "FILTER_ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA"}
				]}
			break;
		case "popup":
			options = 
			{type: "block", width: "auto", name:"FILTER_ITEM_"+num, list: [
				{type: "label", value: "", name: "FAKE_AREA", labelWidth: "135"},
				{type: "newcolumn"},
				{type: "input", value: "", name: "FILTER_ITEM_"+num+"_TYPE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_TYPE"), value:"popup", disabled:true, readonly:true},
				{type: "newcolumn"},
				{type: "block", width: "auto", list: [
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_ID", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},					
					// {type: "input", value: "", name:
					// "FILTER_ITEM_"+num+"_DESCRIPTION",
					// label:that.mockupObj._TextData("FILTER_BAR_FILTER_ITEM_DESCRIPTION"),
					// inputWidth: 150},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_POPUPCODE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_POPUPCODE"), required: true},
					{type: "newcolumn"},					
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_VALUE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_VALUE"), required: true},
					{type: "newcolumn"},					
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_MODULE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_MODULE"), readonly: true},
					// {type: "input", value: "", name:
					// "FILTER_ITEM_"+num+"_URL",
					// label:that.mockupObj._TextData("FILTER_BAR_FILTER_ITEM_URL"),
					// inputWidth: 150},
					// {type: "newcolumn"},
					// {type: "input", value: "", name:
					// "FILTER_ITEM_"+num+"_DEFAULT",
					// label:that.mockupObj._TextData("FILTER_BAR_FILTER_ITEM_DEFAULT"),
					// inputWidth: 150},
					// {type: "input", value: "", name:
					// "FILTER_ITEM_"+num+"_MULTISELECT",
					// label:that.mockupObj._TextData("FILTER_BAR_FILTER_ITEM_MULTISELECT"),
					// inputWidth: 150,
					// options:that.mockupObj.getFormOptions("SELECT_YN")},
					{type: "newcolumn"},
					{type: "button", value: "", name: "FILTER_ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA"}
					// {type: "button", value:
					// that.mockupObj._TextData("COMMON_DELETE"), name:
					// "FILTER_ITEM_"+num+"_DELETE"}
				]}
			]}
			break;
		case "flag":
			options = 
				{type: "block", width: "auto", name:"FILTER_ITEM_"+num, list: [
					{type: "label", value: "", name: "FAKE_AREA", labelWidth: "135"},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_TYPE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_TYPE"), value:"flag", disabled:true, readonly:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_ID", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_CHECKED", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_CHECKED")},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_UNCHECKED", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_UNCHECKED")},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_DEFAULT", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_DEFAULT")},
					{type: "newcolumn"},
					{type: "input", value: "", name: "FILTER_ITEM_"+num+"_MODULE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_MODULE"), readonly: true},
					{type: "newcolumn"},
					{type: "button", value: "", name: "FILTER_ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA"}
				]}
			break;
		case "checkbox":
			options = 
			{type: "block", width: "auto", name:"FILTER_ITEM_"+num, list: [
				{type: "label", value: "", name: "FAKE_AREA", labelWidth: "135"},
				{type: "newcolumn"},
				{type: "input", value: "", name: "FILTER_ITEM_"+num+"_TYPE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_TYPE"), value:"checkbox", disabled:true, readonly:true},
				{type: "newcolumn"},
				{type: "input", value: "", name: "FILTER_ITEM_"+num+"_ID", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},
				{type: "newcolumn"},
				{type: "input", value: "", name: "FILTER_ITEM_"+num+"_COMBO", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_COMBO"), required: true},
				{type: "newcolumn"},
				{type: "input", value: "", name: "FILTER_ITEM_"+num+"_DEFAULT", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_DEFAULT")},
				{type: "newcolumn"},
				{type: "input", value: "", name: "FILTER_ITEM_"+num+"_MODULE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_FILTER_BAR_FILTER_ITEM_MODULE"), readonly: true},
				{type: "newcolumn"},
				{type: "button", value: "", name: "FILTER_ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA"}
			]}
			break;
		}
		return options;
	}
	
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		var filterItem = [];
		var that = this;
		for(var i in this.options.component){
			var m = i.match(/^FILTER_ITEM_(\w{0,})_TYPE$/);
			if(m){
				filterItem.push({type:this.options.component[i], num:m[1]})
			}
		}
		filterItem.sort(function(a, b){
			var aS = parseInt(a.num)
			var bS = parseInt(b.num)
			return (aS<bS)?-1:(aS>bS)?1:0
		});
		for(var i in filterItem){
			var options = getDynamicFormOption(filterItem[i].num, filterItem[i].type);
			formObj_C._apiConnector('addItem',['FILTER_ITEMS', options])
		}
		
		formObj_C.attachEvent("onXLE", function(){
			if(dhx.s2b(this.getItemValue('FILTER_ACTIVE'))){
				this.showItem('FILTER_ITEMS');
				this.showItem('FILTER_ADD_ITEM');
				this.showItem('FILTER_CLEAR_ITEM');
				sObj.setSize();
			}
		});
		formObj_C.attachEvent("onChange", function(name, value){
			if(name == 'FILTER_ACTIVE'){
				if(dhx.s2b(value)){
					this.showItem('FILTER_ITEMS');
					this.showItem('FILTER_ADD_ITEM');
					this.showItem('FILTER_CLEAR_ITEM');
				}else{
					this.hideItem('FILTER_ITEMS');
					this.hideItem('FILTER_ADD_ITEM');
					this.hideItem('FILTER_CLEAR_ITEM');
				}
			}else if(name == "FILTER_ADD_ITEM"){
				if(!value) return;
				var num = that.mockupObj.newId().toUpperCase();
				var options = getDynamicFormOption(num, value);
				
				this.addItem('FILTER_ITEMS', options)
				this.setItemValue("FILTER_ADD_ITEM", "");
			}
			sObj.setSize();
		});
		formObj_C.attachEvent("onButtonClick", function(name){
			if(name == "FILTER_CLEAR_ITEM"){
				var t = [];
				var that = this;
				this.forEachItem(function(n){
					var itemType = that.getItemType(n);
					if(itemType == 'block' && n.indexOf("FILTER_ITEM_") == 0){
						t.push(n);
					}
				})
				for(var i in t){
					this.removeItem(t[i]);
				}
				sObj.setSize();
			}else if(name.indexOf("_DELETE") + 1){
				name = name.replaceAll("_DELETE", "")
				this.removeItem(name);
				sObj.setSize();
			}
		});
		formObj_C.attachEvent("onFocus", function(name){
			if(name.endsWith("_MODULE")) {
				var moduleSelectCallback = function(name, value) {
					formObj_C.setValue(name, value);
				}
				PopupOpenPost(window.CONTEXT_NAME + '/Admin/ModuleConfiguration/AdminModuleConfigurationSelectPopup.sux', {target: name, selectedValue: formObj_C.getValue(name)}, moduleSelectCallback, 800, null, that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_MODULE_SELECT"), 'SELECT_MODULE');
			}
		});
	});
	this.attachEvent('onPopupCallback', function(id, data, so){
		var reDrawFlag = false;
		for(var i in this.optionCell){
			var oName = this.optionCell[i]+"_ACTIVE";
			if(this.options.component[oName] != data.component[oName]){
				reDrawFlag = true;
			}
		}
		
		if(!dhx.s2b(data.component.FILTER_ACTIVE)){
			for(var i in data.component){
				if(i.indexOf('FILTER_ITEM_') == 0){
					delete data.component[i];
				}
			}
		}
		
		for(var i in this.options.component){
			if(i.indexOf('FILTER_ITEM_') == 0){
				delete this.options.component[i];
			}
		}
		
		this.setOptions(data);
		if(reDrawFlag){
			this.clearAllChild();
			this.draw();
		}
		
		this.changeTitle();
	});
	this.draw();
}
suxItem_filterbar.prototype.draw=function(){
	var column_wrapper = document.createElement('div');
	column_wrapper.className = "sepoaux_mockup_column_wrapper";
	this.container.appendChild(column_wrapper);
	
	for(var i in this.optionCell){
		if(dhx.s2b(this.options.component[this.optionCell[i]+"_ACTIVE"])){
			var c = document.createElement('div');
			column_wrapper.appendChild(c);
			c.className = "sepoaux_mockup_column sepoaux_mockup_column_1_4";
			this.addChild(new suxItem_dummy(this.mockupObj, {value:this.optionCell[i]}), c);
		}
	}
	
}
// List Type
suxItem_list=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.setOptions($.extend( true, {
		component:{
			VAR:this.id
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: "",
			SIZE_AUTOHEIGHT: "1",
		},
		type:"list"
	}, o || {}));
	this.drawBase({option:1, delete:1})
	
	if(t){
		this.draw();
	}
	
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		formObj_C.attachEvent("onKeydown", function(inp, ev, name){
			if(name == 'TEMPLATE'){
				if(ev.keyCode == '9'){
					var v=inp.value
					var s=inp.selectionStart;
					var e=inp.selectionEnd;
					inp.value=v.substring(0, s)+'\t'+v.substring(e)
					inp.selectionStart=inp.selectionEnd=s+1;
					ev.preventDefault();
				}
			}
		})
	})
	
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		this.clearAllChild();		
		this.draw();
		this.changeTitle();
		so.status = true;
	})
}
suxItem_list.prototype.draw=function(){
	this.container.innerHTML = "<div class='sepoaux_mockup_item_list_table_wrapper'>"+(this.options.component.TEMPLATE || "").replaceAll("\\\"", "\"") + "</div>";
}
// DataView Type
suxItem_dataview=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.setOptions($.extend( true, {
		component:{
			VAR:this.id
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: "",
			SIZE_AUTOHEIGHT: "1",
		},
		type:"dataview"
	}, o || {}));
	this.drawBase({option:1, delete:1})
	
	if(t){
		this.draw();
	}
	
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		formObj_C.attachEvent("onKeydown", function(inp, ev, name){
			if(name == 'TEMPLATE'){
				if(ev.keyCode == '9'){
					var v=inp.value
					var s=inp.selectionStart;
					var e=inp.selectionEnd;
					inp.value=v.substring(0, s)+'\t'+v.substring(e)
					inp.selectionStart=inp.selectionEnd=s+1;
					ev.preventDefault();
				}
			}
		})
	})
	
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		this.clearAllChild();		
		this.draw();
		this.changeTitle();
		so.status = true;
	})
}
suxItem_dataview.prototype.draw=function(){
	this.container.innerHTML = "<div class='sepoaux_mockup_item_list_table_wrapper'>"+(this.options.component.TEMPLATE || "").replaceAll("\\\"", "\"") + "</div>";
}
// Vault Type
suxItem_vault=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		var that = this;
		var getDynamicFormOption = function(num){
			return {type: "block", width: "auto", className: "vault_dynamic_form", name:"ITEM_"+num, list:[
				{type: "input", value: num, name: "ITEM_"+num+"_ID", hidden: true},
				{type: "newcolumn"},
				{type: "input", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_VAULT_METHOD_NAME"), name: "ITEM_"+num+"_NAME"},
				{type: "newcolumn"},
				{type: "select", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_VAULT_WORK"), name: "ITEM_"+num+"_TYPE", options:that.mockupObj.getFormOptions("VAULT_METHOD_TYPE")},
				{type: "newcolumn"},
				{type: "button", value: "", name: "ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA"}
			]}
		}
		
		var cdata = this.getSortedItemArray();
		for (var i in cdata){
			var options = getDynamicFormOption(cdata[i].itemID);
			formObj_C._apiConnector('addItem',['VAULT_ITEMS', options])
		}
		
		formObj_C.attachEvent('onButtonClick', function(name){
			if(name == "CLEAR_ITEM"){
				var t = [];
				formObj_C.forEachItem(function(obj, n){
					var itemType = obj.getItemType(n);
					if(itemType == 'block' && n.indexOf("VAULT_ITEM_") == 0){
						t.push(n);
					}
				})
				for(var i in t){
					this.removeItem(t[i]);
				}
			}else if(name == "ADD_METHOD"){
				var num = that.mockupObj.newId().toUpperCase();
				var options = getDynamicFormOption(num);
				formObj_C._apiConnector('addItem',['VAULT_ITEMS', options])
			}else if(name.indexOf("_DELETE") + 1){
				name = name.replaceAll("_DELETE", "")
				for(var i in this._indexId){
					if(this._indexId[i].startsWith(name)){
						if(this.getFormData()[this._indexId[i]]){
							this.setItemValue(this._indexId[i], "___DELETE_FLAG___");
						}
					}
				}
				that.options.component[name + "_SORT"] = "___DELETE_FLAG___";

				this.hideItem(name);
			}
			sObj.setSize();
		})
		formObj_C.attachEvent('onBeforeChange', function(name, oValue, nValue){
			if(name.match(/^ITEM_(\w{0,})_ID/) && nValue){
				var flag = false;
				formObj_C.forEachItem(function(obj, n){
					if(n != name && n.match(/^ITEM_(\w{0,})_ID/)){
						if(obj.getItemValue(n) == nValue){
							Alert(nValue+that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_MESSAGE_0002"));
							flag = true;
						}
					}
				})
				if(flag){
					return false;
				}
			}
			return true;
		});
	});
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		this.clearAllChild();		
		this.draw();
		this.changeTitle();
		so.status = true;
	})
	
	this.setOptions($.extend( true, {
		component:{
			VAR:this.id
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: ""
		},
		type:"vault"
	}, o || {}));
	
	this.drawBase({option:1, delete:1})
	if(t){
		this.draw();
	}
}
suxItem_vault.prototype.draw=function(){
	this.container.innerHTML = "<div class='sepoaux_mockup_textbox'>"+ (this.options.component.TYPE || '') +"</div>"
}
// Form Type
suxItem_form=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.setOptions($.extend( true, {
		component:{
			VAR:this.id
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: ""
		},
		type:"form"
	}, o || {}));
	this.drawBase({option:1, delete:1});
	
	if(t){
		this.draw();
	}
	
	this.attachEvent('onPopupCallback', function(id, data, so){
		var formArray = (data.component.FORM_ID || "").split(";");
		
		for(var i in formArray){
			var idx = formArray.indexOf("");
			if(idx >= 0){
				formArray.splice(idx, 1);
				data.component.FORM_ID = formArray.join(";");
			}
		}
		
		this.setOptions(data);
		this.clearAllChild();
		this.draw();
		this.changeTitle();
		so.status = true;
	})
	this.attachEvent('onBeforeChildDelete', function(c){
		var formArray = (this.options.component.FORM_ID || "").split(";");
		var idx = formArray.indexOf(c.options.value);
		if(idx >= 0){
			formArray.splice(idx, 1);
			this.options.component.FORM_ID = formArray.join(";");
		}
	})
}
suxItem_form.prototype.draw=function(){
	var formArray = (this.options.component.FORM_ID || "").split(";");
	if(formArray.length == 1){
		if(formArray[0]){
			var a = this.addChild(new suxItem_form_cell(this.mockupObj, {value:formArray[0]}));
		
			var parent = this;
			this.attachChildEvent(a, "onDelete", function(){
				parent.callEvent("onBeforeChildDelete", [this])
				parent.removeChild(this.id);
				parent.clearAllChild();
				parent.draw();
				
			});
		}
	}else{
		var column_wrapper = document.createElement('div');
		column_wrapper.className = "sepoaux_mockup_column_wrapper";
		this.container.appendChild(column_wrapper);
		
		for(var i in formArray){
			var value = formArray[i];
			if(value){
				var c = document.createElement('div');
				column_wrapper.appendChild(c);
				c.className = "sepoaux_mockup_column sepoaux_mockup_column_1_"+formArray.length;
				var a = this.addChild(new suxItem_form_cell(this.mockupObj, {value:value}), c);
				var parent = this;
				this.attachChildEvent(a, "onDelete", function(){
					parent.callEvent("onBeforeChildDelete", [this])
					parent.removeChild(this.id);
					parent.clearAllChild();
					parent.draw();
					
				});
			}
		}
	}	
}
// Form dummy Type
suxItem_form_cell=function(m, o, t){
	suxItem_dummy.call(this, m, $.extend(true, {type:"formCell", header:{delete:1, option:1, title:"formCell"}}, o || {}), t);
	this.openOptionPopup=function(c){
		PopupOpenPost(window.CONTEXT_NAME+'/Admin/FormManagement/AdminFormManagementPopupLayout.sux'
				, {FORM_ID:this.options.value}
				, null
				, window.innerWidth
				, window.innerHeight
				, this.options.value
				, 'modifyForm');
	}
}
// Grid Type
suxItem_grid=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this.setOptions($.extend( true, {
		component:{
			VAR:this.id,
			MERGE:"false",
			DRAG:"false",
			DYNAMIC:"false",
			EXCELDOWNLOAD:"true"
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: "",
			SIZE_AUTOHEIGHT: "1",
		},
		type:"grid"
	}, o || {}));
	this.drawBase({option:1, delete:1});
	
	if(t){
		this.draw(t);
	}
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		this.clearAllChild();
		this.draw();
		this.changeTitle();
		so.status=true
	})
	this.attachEvent("onBeforeChildDelete", function(){
		delete this.options.component.GRID_ID;
	})
}
suxItem_grid.prototype.draw=function(t){
	if(this.options.component.GRID_ID){
		var a = this.addChild(new suxItem_gridCell(this.mockupObj, {value:this.options.component.GRID_ID}));
		var parent = this;
		this.attachChildEvent(a, "onDelete", function(){
			parent.callEvent("onBeforeChildDelete", [this])
			parent.removeChild(this.id);
			parent.draw();
			
		});
	}
}
// Grid dummy Type
suxItem_gridCell=function(m, o, t){
	suxItem_dummy.call(this, m, $.extend(true, {type:"gridCell", header:{delete:1, option:1, title:"gridCell"}}, o || {}));
	this.openOptionPopup=function(c){
		PopupOpenPost(window.CONTEXT_NAME+'/Admin/GridManagement/AdminGridManagementPopupLayout.sux'
				, {GRID_ID:this.options.value}
				, null
				, window.innerWidth
				, window.innerHeight
				, this.options.value
				, 'modifyGrid');
	}
}

// Button Type
suxItem_button=function(m, o, t){
	suxItem_base.call(this, m, null, t);
	this._beforeCellTransfer=function(id){
		var result = {};
		var r = new RegExp("^ITEM_"+id+"_(\\w{0,})$");
		for(var i in this.options.component){
			var l = i.match(r);
			if(l){
				result[l[1]] = this.options.component[i]
				delete this.options.component[i];
			}
		}
		return result;
	}
	this._afterCellTransfer=function(elem, op){
		var num = this.mockupObj.newId().toUpperCase();
		elem.itemID = num;
		for(var i in op){
			this.options.component["ITEM_"+num+"_"+i] = op[i];
		}
		var b = this.container.firstChild.children;
		for(var i=0;i<b.length;i++){
			this.options.component["ITEM_"+b[i].itemID+"_SORT"] = i+1+"";
		}
	}
	this._buttonTemplate={
		default:{value:"default", code:""},
		primary:{value:"primary", code:""},
		secondary:{value:"secondary", code:""},
		new:{value:"new", code:""},
		inquery:{value:"inquery", code:""},
		save:{value:"save", code:""},
		delete:{value:"delete", code:""},
		back:{value:"back", code:""},
		link:{value:"link", code:""},
		accept:{value:"accept", code:""},
		reject:{value:"reject", code:""},
		excelupload:{value:"excelupload", code:""},
		exceldownload:{value:"exceldownload", code:""},
		gridrow:{value:"gridrow", code:""},
		fileattach:{value:"fileattach", code:""},
		remark:{value:"remark", code:""},
		icon:{value:"icon", code:""},
		template:{value:"template", code:""}
	}
	this.getButtonCombo=function(){
		var r = [{text:"", value:""}];
		for(var i in this._buttonTemplate){
			r.push({text:i, value:this._buttonTemplate[i].value});
		}
		return r;
	}
	this.callScriptList=function(element) {
		var suxItem = element.data.obj;
		var component = element.data.builder;
		var activeID = element.data.activeID;
		var functionName = element.data.functionName;
		var formObj = element.data.formObj;
		var onClickStr = element.data.onClickStr;
		var paramStr = element.data.paramStr;
		
		suxItem.mockupObj.callEvent('requestScriptEditor', [{
			callback:function(item){
				
				if( item.NAME == '' ) {
					formObj.setItemValue(onClickStr, '');
					formObj.setItemValue(paramStr, '');
				} else {
					suxItem.mockupObj.callEvent('onUpdateUserScript', [suxItem.mockupObj.rootItem.options.component.VAR, [item]]);
					formObj.setItemValue(onClickStr, item.NAME);
				}
				
				var ext = {obj: suxItem, builder: component, activeID: activeID, functionName: item.NAME, formObj: formObj, onClickStr: onClickStr, paramStr: paramStr};
				
				$(formObj.getInput(onClickStr)).click(ext, suxItem.callScriptList);
			},
			type:"F",
			options: {
				type: "button_function",
				functionName: functionName,
				script: component.builder.data[activeID].SCRIPT
			}
		}])
	}
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		var that = this;
		var getDynamicFormOption = function(num){
			return {type: "block", width:"600", name:"ITEM_"+num, className: "form_subItem btnArea", list: [
				{type: "button", value: "", name: "ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA"},
				{type: "button", value: "", name: "ITEM_"+num+"_COLLAPSE", hidden:true, className: "COLLAPSE_AREA"},
				{type: "button", value: "", name: "ITEM_"+num+"_EXPAND", hidden:true, className: "EXPAND_AREA"},
				{type: "newcolumn"},
				{type: "block", width:"auto", list: [
					{type: "block", width:"auto" ,list: [
						{type: "input", value: "", name: "ITEM_"+num+"_ID" , label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},
						{type: "select", value: "", name: "ITEM_"+num+"_TYPE" , label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ITEM_TYPE"), options:that.getButtonCombo()},
						{type: "input", value: "", name: "ITEM_"+num+"_MODULE" , label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_MODULE"), required:false, readonly: true},
						{type: "newcolumn"},
						{type: "newcolumn"},
						{type: "select", value: "", name: "ITEM_"+num+"_ONCLICK", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ITEM_ONCLICK"), options:that.mockupObj.getFormOptions("SCRIPT_LIST")},
						{type: "input", value: "", name: "ITEM_"+num+"_SLCODE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ITEM_SLCODE")},
						{type: "input", value: "", name: "ITEM_"+num+"_ICON", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ITEM_ICON"), hidden:true},
						{type: "input", value: "", name: "ITEM_"+num+"_TSLCODE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ITEM_TSLCODE")},
						{type: "newcolumn"},
						{type: "input", value: "", name: "ITEM_"+num+"_PARAM", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ITEM_PARAM")},
						{type: "input", value: "", name: "ITEM_"+num+"_TEXT", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ITEM_TEXT")  , required:true},
						{type: "select", value: "", name: "ITEM_"+num+"_POSITION", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ITEM_POSITION"), hidden:true, options:that.mockupObj.getFormOptions("BUTTON_POSITION")},
						{type: "input", value: "", name: "ITEM_"+num+"_TEMPLATE", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_BUTTON_ITEM_TEMPLATE")}
					]}
				]},
				{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"}
			]}
		}
		
		
		var btns = [];
		for(var i in this.options.component){
			var m = i.match(/^ITEM_(\w{0,})_ID$/);
			if(m){
				btns.push({
					OBJECT_ID:m[1],
					ID:this.options.component[i],
					SORT:this.options.component["ITEM_"+m[1]+"_SORT"]
				})
			}
		}
		btns.sort(function(a, b){
			var aS = parseInt(a.SORT)
			var bS = parseInt(b.SORT)
			return (aS<bS)?-1:(aS>bS)?1:0
		})
		var p = getOAObject('SCREEN_BUILDER');
		var activeID = p.builder.activeID;
		for(var i in btns){
			var options = getDynamicFormOption(btns[i].OBJECT_ID);
			formObj_C._apiConnector('addItem',['BUTTON_ITEMS', options])
		}
		formObj_C.attachEvent("onChange", function(name, value){
			var m = name.match(/^ITEM_(\w{0,})_(\w{0,})$/);
			if(m){
				var id = m[1];
				if(m[2] == 'TYPE'){
					if(value==""){return;}
					var bt = that._buttonTemplate[value];
					
					var bI = this.getItemValue("ITEM_"+id+"_ID");
					if(bt.code){
						if(bI.toUpperCase() != value.toUpperCase() && value!="template"){
							this.setItemValue("ITEM_"+id+"_ID", value.toUpperCase());
						}
						this.setItemValue("ITEM_"+id+"_SLCODE", bt.code);
					}
					
					this.hideItem("ITEM_"+id+"_ICON");
					this.hideItem("ITEM_"+id+"_POSITION");
					
					if(value=="icon"){
						this.showItem("ITEM_"+id+"_ICON");
						this.showItem("ITEM_"+id+"_POSITION");
					}
					
				}else if(m[2] == 'SLCODE'){
					var txt = Sepoa.cache.text.get(value);
					if(value == "" || txt == 'Has not found text : '+value){
						this.setItemValue("ITEM_"+id+"_TEXT", "");
						this.setReadonly("ITEM_"+id+"_TEXT", false);
					}else{
						this.setItemValue("ITEM_"+id+"_TEXT", txt);
						this.setReadonly("ITEM_"+id+"_TEXT", true);
					}
				}else if(m[2] == 'TSLCODE'){
					var txt = Sepoa.cache.text.get(value);
					if(value == "" || txt == 'Has not found text : '+value){
						this.setItemValue("ITEM_"+id+"_TEMPLATE", "");
						this.setReadonly("ITEM_"+id+"_TEMPLATE", false);
					}else{
						this.setItemValue("ITEM_"+id+"_TEMPLATE", txt);
						this.setReadonly("ITEM_"+id+"_TEMPLATE", true);
					}
				}else if(m[2] == 'BORDER'){
					if(value == "true"){
						this.showItem("ITEM_"+id+"_BORDERCOLOR")
					}else{
						this.hideItem("ITEM_"+id+"_BORDERCOLOR")
					}
					sObj.setSize();
				}else if(m[2] == 'ONCLICK'){
					if(value == '--NEW_FUNCTION--'){
						that.mockupObj.callEvent('requestScriptEditor', [{
							callback:function(item){
								that.mockupObj.callEvent('onUpdateUserScript', [that.mockupObj.rootItem.options.component.VAR, [item]]);
								formObj_C.forEachItem(function(b,v){
									if(v.match(/^ITEM_(\w{0,})_ONCLICK$/)){
										var o = document.createElement('option');
										o.text = item.NAME+"("+item.ARGS+")";
										o.value = item.NAME;
										b.getSelect(v).appendChild(o);
										if(v == name){
											b.setItemValue(v, item.NAME)
										}
									}
								})
							},
							type:"F"
						}])
						this.setItemValue(name, "");
					}
				}else if(m[2] == 'COLOR'){
					if(!value) this.setItemValue(name, "", true);
				}else if(m[2] == 'TEXTCOLOR'){
					if(!value) this.setItemValue(name, "", true);
				}else if(m[2] == 'BORDERCOLOR'){
					if(!value) this.setItemValue(name, "", true);
				}
			}
		});
		formObj_C.attachEvent("onXLE", function(){
			formObj_C.forEachItem(function(f, name){
				var m = name.match(/^ITEM_(\w{0,})_(\w{0,})$/)
				if(m){
					if(m[2] == "BORDER"){
						if(f.getItemValue() == "true"){
							f.showItem("ITEM_"+m[1]+"_BORDERCOLOR")
						}
					}else if(m[2] == "PARAM"){
						f.setItemValue(name, f.getItemValue(name).replaceAll("&#39;", "'"), true);
					}else if(m[2] == "TYPE"){
						f.setItemValue(name, f.getItemValue(name).replaceAll("&#39;", "'"), true);
						
						f.hideItem("ITEM_"+m[1]+"_ICON");
						f.hideItem("ITEM_"+m[1]+"_POSITION");
						
						var type = f.getItemValue(name);
						if(type=="icon"){
							f.showItem("ITEM_"+m[1]+"_ICON");
							f.showItem("ITEM_"+m[1]+"_POSITION");
						}
					}
				}
			})
		})
		formObj_C.attachEvent("onButtonClick", function(name){
			if(name == 'ADD_ITEM'){
				var num = that.mockupObj.newId().toUpperCase(); 
				this.addItem('BUTTON_ITEMS', getDynamicFormOption(num))
				sObj.setSize();
			}else if(name == 'CLEAR_ITEM'){
				var t = [];
				var f = this;
				this.forEachItem(function(n){
					var itemType = f.getItemType(n);
					if(itemType == 'block' && n.match("^ITEM_")){
						t.push(n);
					}
				})
				for(var i in t){
					this.removeItem(t[i]);
				}
				sObj.setSize();
			}else if(name.match(/_DELETE$/)){
				name = name.replaceAll("_DELETE", "")
				this.removeItem(name);
				sObj.setSize();
			}else if(name.match(/_EXPAND$/)){
				this.hideItem(name)
				name = name.replaceAll("_EXPAND", "");
				this.showItem(name+"_COLLAPSE");
				this.showItem(name+"_OPTION");
				
				sObj.setSize();
			}else if(name.match(/_COLLAPSE$/)){
				this.hideItem(name)
				name = name.replaceAll("_COLLAPSE", "");
				this.showItem(name+"_EXPAND");
				this.hideItem(name+"_OPTION");
				
				sObj.setSize();
			}
		});
		formObj_C.attachEvent("onFocus", function(name){
			var m = name.match(/^ITEM_(\w{0,})_(\w{0,})$/);
			if(m) {
				var id = m[1];
				if(m[2] == 'MODULE'){
					var moduleSelectCallback = function(name, value) {
						formObj_C.setValue(name, value);
					}
					PopupOpenPost(window.CONTEXT_NAME + '/Admin/ModuleConfiguration/AdminModuleConfigurationSelectPopup.sux', {target: m[0], selectedValue: formObj_C.getValue(m[0])}, moduleSelectCallback, 800, null, that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_MODULE_SELECT"), 'SELECT_MODULE');
				}
			}
		});
	});
	this.attachEvent('onPopupCallback', function(id, data, so){
		for(var i in this.options.component){
			if(i.match(/^ITEM_/)){
				delete this.options.component[i]
			}
		}
		this.setOptions(data);
		this.draw();
		this.changeTitle();
		if(this.container.firstChild){
			var b = this.container.firstChild.children;
			for(var i=0;i<b.length;i++){
				this.options.component["ITEM_"+b[i].itemID+"_SORT"] = i+1+"";
			}
		}
		so.status = true
	})
	this.attachEvent('onRemoveChildItem', function(id){
		this.removeChild(id);
	});
	
	this.setOptions($.extend( true, {
		component:{
			VAR:this.id
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: ""
		},
		type:"button"
	}, o || {}));
	
	this.drawBase({option:1, delete:1});
	this.draw();
}
suxItem_button.prototype.draw=function(t){
	this.container.innerHTML = "";
	
	var btns = [];
	var sort = 0;
	for(var i in this.options.component){
		var m = i.match(/^ITEM_(\w{0,})_ID$/);
		if(m){
			var code = this.options.component["ITEM_"+m[1]+"_SLCODE"] || this.options.component["ITEM_"+m[1]+"_TSLCODE"];
			var text = Sepoa.cache.text.get(code);
			if(!text || text.length == 0 || text.startsWith('Has not found text')) {
				text = this.options.component[i];
			} else {
				text = sUtils.string.dataReplacer(text + '('+ this.options.component[i] +')');
			}
			btns.push({
				OBJECT_ID:m[1],
				ID:this.options.component[i],
				SORT:this.options.component["ITEM_"+m[1]+"_SORT"] || ++sort,
				TEXT: text
			})
		}
	}
	btns.sort(function(a, b){
		var aS = parseInt(a.SORT)
		var bS = parseInt(b.SORT)
		return (aS<bS)?-1:(aS>bS)?1:0
	})
	var w = document.createElement('div');
	w.className = "sepoaux_mockup_item_btn_wrapper";
	this.container.appendChild(w);
	
	var m = [0,0,0,0];
	
	switch(this.options.layout.ALIGN_HORIZONTAL){
	case "right":
		m[1] = 0;
		m[3] = "auto";
		break;
	case "center":
		m[1] = "auto";
		m[3] = "auto";
		break;
	case "left":
	default:
		m[1] = "auto";
		m[3] = 0;
		break;
	}
	
	w.obj = this;
	
	w.style.margin = m.join(" ");
	$(w).sortable({
		revert: 0,
		delay:100,
		connectWith:".sepoaux_mockup_item_btn_wrapper",
		placeholder: "sepoaux_mockup_item_btn sepoaux_mockup_item_placeholder",
		start:function(event, ui){
			ui.placeholder.width(ui.item.width());
			ui.placeholder.height(ui.item.height());
		},
		stop:function(event, ui){
			var parent = ui.item.parent()[0];
			var op = this.obj._beforeCellTransfer(ui.item[0].itemID);
			parent.obj._afterCellTransfer(ui.item[0], op);
			this.obj.mockupObj.callEvent('onUpdateItem', ["buttonCell / " + op.ID +" / "+ this.obj.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_HISTORY_MOVE")]);
		}
	});
	for(var i in btns){
		var d = document.createElement('div');
		d.itemID = btns[i].OBJECT_ID;
		w.appendChild(d);
		d.className = "sepoaux_mockup_item_btn";
		d.innerHTML = btns[i].TEXT;
	}
}
// Column Cell
suxItem_column=function(m, o, t){
	suxItem_cell.call(this, m, null);
	this._beforeCellTransfer=function(id){
		var y = this._childObj.indexOf(id);
		if(y >= 0){
			this._childObj.splice(y, 1);
		}
	}
	this._afterCellTransfer=function(){
		var childList = [];
		var newElem = null;
		for(var i=0;i<this.container.children.length;i++){
			var d = this.container.children[i].id;
			var k = this.mockupObj._objPool[d];
			if(k.type != "new"){
				childList.push(this.container.children[i].id);
			}else{
				newElem = k;
			}
		}
		if(newElem){
			childList.push(newElem.id);
			newElem.base.parentElement.insertBefore(newElem.base, null)
		}
		this._childObj = childList;
	}
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		this.changeTitle();
		so.status = true
	})
	
	var defaultOption = {type:"column", component:{VAR:this.id}};
	this.setOptions($.extend(true, defaultOption, o))
	this.drawBase({option:1, drag:true});
	
	if(t){
		for(var i in t.cells){
			var c = t.cells[i];
			
			var a = this.addChild(new window["suxItem_"+c.type](this.mockupObj, this.mockupObj._objPool[c.id], c));
			var parent = this;
			this.attachChildEvent(a, "onDelete", function(){
				parent.callEvent("onBeforeChildDelete", [this])
				parent.removeChild(this.id);
				parent.draw();
				
			});
		}
	}
	
	this.draw();
}
suxItem_column.prototype = suxItem_cell.prototype;
// Layout Cell
suxItem_layoutCell=function(m, o, t){
	suxItem_cell.call(this, m, null);
	this._beforeCellTransfer=function(id){
		var op = this.options.component;
		var result = {
			ATTACH_TYPE:op.ATTACH_TYPE,
			ATTACH_TARGET:op.ATTACH_TARGET,
			ATTACH_ALIAS:op.ATTACH_ALIAS
		}
		this.callEvent('onBeforeChildDelete', [])
		this.removeChild(id);
		this.draw();
		return result;
	}
	this._afterCellTransfer=function(elem, op){
		this.clearAllChild();
		this.options.component.ATTACH_TARGET = op.ATTACH_TARGET;
		this.options.component.ATTACH_TYPE = op.ATTACH_TYPE;
		this.options.component.ATTACH_ALIAS = op.ATTACH_ALIAS;
		var target = this.options.component.ATTACH_TARGET;
		if(this.options.component.ATTACH_ALIAS){
			target += this.options.component.ATTACH_TARGET + ", alias : "+ this.options.component.ATTACH_ALIAS
		}
		var i = this.addChild(new suxItem_screen(this.mockupObj, {value:target}, null));
		var parent = this;
		this.attachChildEvent(i, "onDelete", function(){
			parent.callEvent('onBeforeChildDelete', [this])
			parent.removeChild(this.id);
			parent.draw();
		});
		this.attachChildEvent(i, "onChange", function(v){
			parent.options.component.ATTACH_TARGET=v;
		});
	}
	this.attachEvent('onPopupCallback', function(id, data, so){
		var reDrawFlag = this.options.component.ATTACH_TYPE != data.component.ATTACH_TYPE;
		this.setOptions(data);
		if(reDrawFlag){
			this.clearAllChild();
			if(["U", "S"].indexOf(this.options.component.ATTACH_TYPE) + 1){
				var target = this.options.component.ATTACH_TARGET;
				if(this.options.component.ATTACH_ALIAS){
					target += this.options.component.ATTACH_TARGET + ", alias : "+ this.options.component.ATTACH_ALIAS
				}
				var i = this.addChild(new suxItem_screen(this.mockupObj, {value:target}, t));
				var parent = this;
				this.attachChildEvent(i, "onDelete", function(){
					parent.callEvent("onBeforeChildDelete", [this])
					parent.removeChild(this.id);
					parent.draw();
					
				});
				this.attachChildEvent(i, "onChange", function(v){
					parent.options.component.ATTACH_TARGET=v;
				})
			}else{
				this.draw();
			}
		}
		so.status=true
	})
	
	this.attachEvent('onDropChildObject', function(a, t){
		if(a !== 'S') {
			Alert(this.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_MESSAGE_0003"));
			this.removeChild(t);
			this.draw();
			return;
		}
		this.setOptions({component:{ATTACH_TYPE:a, ATTACH_TARGET:t}});
	})
	this.attachEvent('onBeforeChildDelete', function(){
		this.setOptions({component:{ATTACH_TYPE:"C", ATTACH_TARGET:""}});
	})
	
	var defaultOption = {maxChildCount:1, type:"layoutCell", component:{HEADER_VIEW:"", COLLAPSE_COLLAPSED:"false", COLLAPSE_ICON:"false", BASE_MENU:(o.name=='a')?"true": "false"}};
	this.setOptions($.extend(true, defaultOption, o))
	this.drawBase({option:1, drag:{connectWith:".sepoaux_mockup_item_layoutCell.sepoaux_mockup_base_has_blank"}});
	this.changeTitle(o.name.toUpperCase());
	
	if(t && this.options.component.ATTACH_TARGET){
		var i = this.addChild(new suxItem_screen(this.mockupObj, {value:this.options.component.ATTACH_TARGET}, t));
		var parent = this;
		this.attachChildEvent(i, "onDelete", function(){
			parent.callEvent("onBeforeChildDelete", [this])
			parent.removeChild(this.id);
			parent.draw();
			
		});
		this.attachChildEvent(i, "onChange", function(v){
			parent.options.component.ATTACH_TARGET=v;
		});
	}
	this.draw();
}

suxItem_layoutCell.prototype = suxItem_cell.prototype;

// Tree Type
suxItem_tree = function(m, o, t) {
	
	suxItem_base.call(this, m, null, t);
	
	this.setOptions($.extend( true, {
		component:{
			VAR:this.id,
			CHECKBOX:""
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: "",
			SIZE_AUTOHEIGHT: "1",
		},
		type:"tree"
	}, o || {}));
	
	this.drawBase({option: 1, delete: 1});
	
	if(t) {
		this.draw();
	}
	
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		this.clearAllChild();
		this.draw();
		this.changeTitle();
		so.status=true;
	});
}
suxItem_tree.prototype.draw=function() {
	this.container.innerHTML = "<div class='sepoaux_mockup_item_list_table_wrapper'>"+(this.options.component.TEMPLATE || "").replaceAll("\\\"", "\"") + "</div>";
}

// Accordion Type
suxItem_accordion = function(m, o, t) {
	suxItem_base.call(this, m, null, t);
	
	this._beforeCellTransfer=function(id){
		var result = {};
		var r = new RegExp("^ITEM_"+id+"_(\\w{0,})");
		for(var i in this.options.component){
			var f = i.match(r);
			if(f){
				result[f[1]] = this.options.component[i];
				delete this.options.component[i];
			}
		}
		this.removeChild(id)
		return result;
	}
	
	this._afterCellTransfer=function(elem, op){
		var num = this.mockupObj.newId().toUpperCase();
		elem.itemID = num;
		for(var i in op){
			this.options.component["ITEM_"+num+"_"+i] = op[i];
		}
		var b = this.container.children;
		for(var i=0;i<b.length;i++){
			this.options.component["ITEM_"+b[i].itemID+"_SORT"] = i+1+"";
		}
		this.draw();
	}
	
	this.attachEvent('onPopupDraw', function(sObj, formObj_C, formObj_L, formObj_S){
		var that = this;
		var getDynamicFormOption = function(num){
			return {type: "block", width: "auto", name:"ITEM_"+num, list: [
				{type: "block", width: "auto", list:[
					{type: "select", value: "", name: "ITEM_"+num+"_COLLAPSED", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ACCORDION_COLLAPSED"), options:that.mockupObj.getFormOptions("LAYOUT_COLLAPSED")},
					{type: "newcolumn"},
					{type: "input", value: "", name: "ITEM_"+num+"_ID", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_ITEM_ID"), required:true},
					{type: "newcolumn"},
					{type: "input", value: "", name: "ITEM_"+num+"_NAME", label:that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ACCORDION_ITEM_NAME"), required:true},
					{type: "newcolumn"},
					{type: "button", value: "", name: "ITEM_"+num+"_DELETE", className: "OBJECT_DELETE_AREA DELETE_AREA_CENTER"}
				]},
				{type: "block", width: "auto", list:[
					{type: "select", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_TYPE"), name: "ITEM_"+num+"_TYPE", options:that.mockupObj.getFormOptions("ATTACH_TYPE")},
					{type: "newcolumn"},
					{type: "input", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_TARGET"), name: "ITEM_"+num+"_TARGET"},
					{type: "newcolumn"},
					{type: "input", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ATTACH_ALIAS"), name: "ITEM_"+num+"_ALIAS"},
					{type: "newcolumn"},
					{type: "select", value: "", label: that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_ACCORDION_HEADER"), name: "ITEM_"+num+"_HEADER", options: that.mockupObj.getFormOptions("SELECT_YN")}
				]},
				{type: "container", width: "auto", className: "AREA_DIVISION", name: "AREA_DIVISION"}
			]}
		}
		
		var cdata = this.getSortedItemArray();
		
		for (var i in cdata){
			var options = getDynamicFormOption(cdata[i].itemID);
			formObj_C._apiConnector('addItem',['ACCORDION_ITEMS', options])
		}
		formObj_C.attachEvent('onButtonClick', function(name){
			if(name == "ADD_ITEM"){
				var num = that.mockupObj.newId().toUpperCase();
				var options = getDynamicFormOption(num);
				formObj_C._apiConnector('addItem',['ACCORDION_ITEMS', options]);
			}else if(name.indexOf("_DELETE") + 1){
				name = name.replaceAll("_DELETE", "")
				for(var i in this._indexId){
					if(this._indexId[i].startsWith(name)){
						if(this.getFormData()[this._indexId[i]]){
							this.setItemValue(this._indexId[i], "___DELETE_FLAG___");
						}
					}
				}				
				that.options.component[name + "_SORT"] = "___DELETE_FLAG___";
				
				this.hideItem(name);
			}
			sObj.setSize();
		})
		formObj_C.attachEvent('onBeforeChange', function(name, oValue, nValue){
			if(name.match(/^ITEM_(\w{0,})_ID/) && nValue){
				var flag = false;
				formObj_C.forEachItem(function(obj, n){
					if(n != name && n.match(/^ITEM_(\w{0,})_ID/)){
						if(obj.getItemValue(n) == nValue){
							Alert(nValue+that.mockupObj._TextData("SCREEN_BUILDER_MOCKUP_COMMON_MESSAGE_0002"));
							flag = true;
						}
					}
				})
				if(flag){
					return false;
				}
			}
			return true;
		});
	});
	
	this.attachEvent('onPopupCallback', function(id, data, so){
		this.setOptions(data);
		this.draw();
		this.changeTitle();
		var b = this.container.children;
		for(var i=0;i<b.length;i++){
			this.options.component["ITEM_"+b[i].itemID+"_SORT"] = i+1+"";
		}
		so.status = true
	});
	
	this.setOptions($.extend(true, {
		component:{
			VAR:this.id
		},
		layout:{
			MARGIN_BOTTOM: "",
			MARGIN_LEFT: "",
			MARGIN_RIGHT: "",
			MARGIN_TOP: "",
			SIZE_WIDTH: "",
			SIZE_HEIGHT: "",
			SIZE_AUTOHEIGHT: "1"
		},
		type:"accordion"
	}, o || {}));
	this.drawBase({option:1, delete:1, drag:{connectWith:".sepoaux_mockup_item_accordion"}});
	this.draw();
}
suxItem_accordion.prototype.draw=function() {
	this.clearAllChild();
	this.container.innerHTML = "";
	
	var cdata = this.getSortedItemArray();
	
	for(var i in cdata){
		var d = document.createElement('div');
		this.container.appendChild(d);
		d.itemID = cdata[i].itemID;
		
		var t = null;
		if(cdata[i].TARGET && cdata[i].TYPE){
			t = {};
		}
		
		var a = this.addChild(new suxItem_accordionCell(this.mockupObj, {name:cdata[i].ID, id:cdata[i].itemID, component:cdata[i]}, t), d);
		var parent = this;
		this.attachChildEvent(a, "onChange", function(opt){
			var u = opt.component
			parent.options.component["ITEM_" + opt.id + "_COLLAPSED"] = u.COLLAPSED;
			parent.options.component["ITEM_"+opt.id+"_ID"] = u.ID
			parent.options.component["ITEM_"+opt.id+"_TYPE"] = u.TYPE
			parent.options.component["ITEM_"+opt.id+"_TARGET"] = u.TARGET
			parent.options.component["ITEM_"+opt.id+"_NAME"] = u.NAME
		});
	}
}
// Accordion dummy Type
suxItem_accordionCell=function(m, o, t) {
	suxItem_cell.call(this, m, null);
	
	this._beforeCellTransfer=function(id){
		var op = this.options.component;
		var result = {
			COLLAPSED: op.COLLAPSED,
			TYPE: op.TYPE,
			TARGET: op.TARGET,
			NAME: op.NAME
		}
		this.callEvent('onBeforeChildDelete', [])
		this.removeChild(id);
		this.draw();
		return result;
	}
	
	this._afterCellTransfer=function(elem, op){
		this.clearAllChild();
		this.options.component.COLLAPSED = op.COLLAPSED;
		this.options.component.TARGET = op.TARGET;
		this.options.component.TYPE = op.TYPE;
		this.options.component.NAME = op.NAME
		
		var target = this.options.component.TARGET;
		var i = this.addChild(new suxItem_screen(this.mockupObj, {value:target}, null));
		var parent = this;
		this.attachChildEvent(i, "onDelete", function(){
			parent.callEvent('onBeforeChildDelete', [])
			parent.removeChild(this.id);
			parent.draw();
		});
		this.attachChildEvent(i, "onChange", function(v){
			parent.options.component.TARGET=v;
			parent.callEvent('onChange', [parent.options]);
		});
		this.callEvent('onChange', [this.options])
	}
	
	this.attachEvent('onDropChildObject', function(a, t){
		this.setOptions({component:{TYPE:a, TARGET:t}})
		this.callEvent('onChange', [this.options]);
	});
	
	this.attachEvent('onBeforeChildDelete', function(){
		this.setOptions({component:{TYPE:"C", TARGET:""}})
		this.callEvent('onChange', [this.options]);
	});
	
	var defaultOption = {maxChildCount:1, type:"accordionCell", component:{}};
	
	this.setOptions($.extend(true, defaultOption, o))
	this.drawBase({drag:{connectWith:".sepoaux_mockup_item_accordionCell.sepoaux_mockup_base_has_blank"}});
	this.changeTitle(o.name + (o.component.NAME ? (" [ "+o.component.NAME+" ] ") : ""));
	
	if(t){
		var target = this.options.component.TARGET;
		
		var i = this.addChild(new suxItem_screen(this.mockupObj, {value:target}, t));
		var parent = this;
		
		this.attachChildEvent(i, "onDelete", function(){
			parent.callEvent('onBeforeChildDelete', [])
			parent.removeChild(this.id);
			parent.draw();
		});
		
		this.attachChildEvent(i, "onChange", function(v){
			parent.options.component.TARGET=v;
			parent.callEvent('onChange', [parent.options]);
		});
	}
	this.draw();
}
suxItem_accordionCell.prototype = suxItem_cell.prototype;