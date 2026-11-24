function SepoaScreenBuilder(c, lang){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.vp = c;
	this.config={
		vp:c,
		skin:"material"
	}

	this.orgData={};
	this.mockup={};
	this.component={}
	this.drawBaseLayout();
	this.initHistoryManager();
}
SepoaScreenBuilder.prototype.initHistoryManager=function(){
	this.history = new HistoryManager({
		stacksize:50,
		width:500,
		isHistory:true,
		onChange:dhtmlx.bind(function(data){
			if(data)
				this.initProject(data, this.mode);
		}, this)
	})
	this.history.addSnapShot=dhtmlx.bind(function(desc){
		if(this.history.enable()){
			var d = {};
			for(var i in this.mockup){
				d[i] = this.mockup[i].exportData()
				d[i].info = this.data[i]
			}
			this.history.push(d, desc);
		}
	}, this);
}
SepoaScreenBuilder.prototype.load=function(target){
	var that = this;
	var result = null;
	return doServiceAjax("sepoa.svc.tools.ScreenBuilder", "CONNECTION", "doLoad", {target:target}, function(so){
		result = that.doLoadAfter(so);
	}, false);
	return result;
}
SepoaScreenBuilder.prototype.doLoadAfter=function(so){
	if(so.status == "1"){
		var responseData = JSON.parse(so.result[0])
		var data = [];
		var message = [];
		for(var i in responseData){
			if(!responseData[i].FAIL){
				data.push(responseData[i]);
			}else{
				message.push(responseData[i].MESSAGE);
			}
		}
		if(message.length>0){
			Alert(message.join("</br>"));
		}
		if(data.length > 0){
			this.initProject(data);
		}else{
			return false;
		}
	}
	return true;
}
SepoaScreenBuilder.prototype.doSave=function(force){
	var d = {};
	var flag = true;
	for(var i in this.mockup){
		d[i] = this.mockup[i].exportData()
		d[i].info = this.data[i]
		flag = false;
	}
	if(flag){
		Alert(this._TextData('SCREEN_BUILDER_MESSAGE_0006'))
		return;
	}
	var params = {data:JSON.stringify(d)};
	if(this.mode == "TEMPLATE"){
		this.popupAction("SAVE_TEMPLATE", params);
	}else if(this.mode == "SPECIFICATION"){
		doServiceAjaxFile("sepoa.svc.tools.Outputs", "CONNECTION", "doSave", params);
	}else{
		var infoArr = [];
		var screenArr = Object.keys(d);
		
		for(var i in screenArr){
			infoArr.push(
				{
					SCREEN_ID : screenArr[i],
					PATH : d[screenArr[i]].info.PATH
				}
			);
		}
		this.popupAction("SAVE_PROJECT", infoArr);
	}
}
SepoaScreenBuilder.prototype.doSaveProject=function(data, force){
	var that = this;
	var d = {};
	var params = {};
	
	for(var i in this.mockup){
		for(var j in data){
			if(i == j){
				d[i] = this.mockup[i].exportData()
				d[i].info = this.data[i]
			}
		}
	}
	
	this.tempSaveData = d;
	params = {data:JSON.stringify(d)};
	
	if(force){
		params.forceFlag = true;
	}
	
	doServiceAjax("sepoa.svc.tools.ScreenBuilder", "CONNECTION", "doSave", params, function(so){
		that.doSaveAfter(so);
	});
}
SepoaScreenBuilder.prototype.doSaveAfter=function(so){
	switch(so.status){
	case "1":
		this.tempSaveData = {};
		sUtils.cache.text.clear();
		Alert(this._TextData('SCREEN_BUILDER_MESSAGE_0001'));
		break;
	case "2":
		var that = this;
		Confirm(this._TextData('SCREEN_BUILDER_MESSAGE_0002')+so.message, function(flag){
			if(flag){
				that.doSaveProject(that.tempSaveData, true);
			}
		})
		break;
	default:
		Alert(this._TextData('SCREEN_BUILDER_MESSAGE_0003')+so.message);
		break;
	}
}
SepoaScreenBuilder.prototype.drawBaseLayout=function(){
	var layout = this.component.layout = new SepoaDhtmlXLayoutObject({
		  cells:[{"button":"false","fix_size":["true","false"],"header":"false","id":"a"}]
		 ,pattern:"1C"
		 ,skin:this.config.skin
		 ,parent:this.vp
	});
	
	var menu = this.component.menu = layout.attachMenu({
		items:[
			{id:"PROJECT", text:this._TextData('SCREEN_BUILDER_MENU_PROJECT'), items:[
				{id:"NEW", text:this._TextData('SCREEN_BUILDER_MENU_NEW')},
				{id:"SAVE", text:this._TextData('SCREEN_BUILDER_MENU_SAVE')},
				{id:"LOAD", text:this._TextData('SCREEN_BUILDER_MENU_LOAD')}
			]},
			{id:"BUILDER", text:this._TextData('SCREEN_BUILDER_MENU_BUILDER'), items:[
				{id:"HISTORY", text:this._TextData('SCREEN_BUILDER_MENU_HISTORY')},
				{id:"PREVIEW", text:this._TextData('SCREEN_BUILDER_MENU_PREVIEW'), disabled:true},
				{id:"NOW_REFRESH", text:this._TextData('SCREEN_BUILDER_MENU_NOW_REFERESH'), disabled:true},
				{id:"ALL_REFRESH", text:this._TextData('SCREEN_BUILDER_MENU_ALL_REFERESH'), disabled:true},
				{id:"SWITCH_TEMPLATE", text:this._TextData('SCREEN_BUILDER_MENU_SWITCH_TEMPLATE'), disabled:true},
			]},
			{id:"ETC", text:this._TextData('SCREEN_BUILDER_MENU_ETC'), items:[
				{id:"PROGRAMLIST", text:this._TextData('SCREEN_BUILDER_MENU_PROGRAMLIST')},
				{id:"SPECIFICATION", text:"사양서저장"}
			]}
		],
		skin:"material"
	});
	menu.parentObj = this;
	menu.attachEvent('onClick',function(id, zone, cas){
		if("SAVE" == id){
			this.parentObj.doSave();
		}else if("PREVIEW" == id){
			this.parentObj.openPreview();
		}else if("SPECIFICATION" == id){
			this.parentObj.mode = "SPECIFICATION";
		}else if(id.lastIndexOf("REFRESH") > 0){
			this.parentObj.refresh(id);
		}else if("SWITCH_TEMPLATE" == id){
			this.parentObj.switchTemplate();
		}else if("HISTORY" == id){
			this.parentObj.history.showHistory();
		}else{
			this.parentObj.popupAction("MENU_"+id);
		}
	})
	
	var ribbon = this.component.ribbon = this.component.layout.attachRibbon({
		icons_path : window.CONTEXT_NAME+"/resource/plugin/sepoasoft/screenbuilder/icon/",
	    arrows_mode: "auto",
	    skin:this.config.skin,
	    items: [
        	{type:'block', text:'Layout', mode:'cols', list:[
            	{type:'button', isbig: true, text:'Layout'   , img: "icon_layout.png"},
            	{type:'button', isbig: true, text:'Tabbar'   , img: "icon_tabbar.png"},
            	{type:'button', isbig: true, text:'Carousel' , img: "icon_carousel.png"},
            	{type:'button', isbig: true, text:'Accordion' , img: "icon_container.png"}
            ]},
            {type:'block', text:'Html', mode:'cols', list:[
            	{type:'button', isbig: true, text:'Container', img: "icon_container.png"},
            	//{type:'button', isbig: true, text:'Column'   , img: "temp.gif"}
            ]},
            {type:'block', text:'Data', mode:'cols', list:[
            	{type:'button', isbig: true, text:'Grid'   , img: "icon_grid.png"},
            	{type:'button', isbig: true, text:'List'   , img: "icon_list.png"},
            	{type:'button', isbig: true, text:'DataView'   , img: "icon_data_view.png"},
            	{type:'button', isbig: true, text:'Form'   , img: "icon_form.png"},
            	{type:'button', isbig: true, text:'Vault'   , img: "icon_vault.png"},
            	{type:'button', isbig: true, text:'Tree'   , img: "icon_container.png"}
            ]},
            {type:'block', text:'ETC', mode:'cols', list:[
        		{type:'button', isbig: true, text:'FilterBar'   , img: "icon_toolbar.png"},
        		{type:'button', isbig: true, text:'Button'   , img: "icon_container.png"},
        	]}
    	]       
	})
	$(ribbon._base).find('.dhxrb_big_button').on('dragstart', function(e){
		e.originalEvent.dataTransfer.setData("type", this.lastElementChild.innerText);
	})
}
SepoaScreenBuilder.prototype.drawEditor=function(){
	if(this.config.isDraw){
		return;
	}
	var innerLayout = this.component.innerLayout = this.component.layout.cells('a').attachLayout({
		cells:[
			{button:"false",fix_size:["true","false"],header:"false",id:"a", width:"400"},
			{button:"false",fix_size:["true","false"],header:"false",id:"b"}
		],
		pattern:"2U",
		skin:this.config.skin,
		offsets: {
			top: -5
		}
	});
	
	var infoTabbar = this.component.infoTabbar = innerLayout.cells('a').attachTabbar({
		skin:this.config.skin,
		mode:"top",
		align:"left",
		content_zone:true,
		arros_mode:true,
		tabs:[
			{id:"INFO", text:"Info", close:false, enabled:false},
			{id:"SCRIPT", text:"Script", close:false, enabled:false},
			{id:"ONLOAD", text:"OnLoad", close:false, enabled:false},
			{id:"LINK", text:"Link", close:false, enabled:false},
			{id:"CSS", text:"CSS", close:false, enabled:false}
		]
	});
	infoTabbar.parentObj = this;
	infoTabbar.attachEvent("onTabChanged",function(id){
		if(["SCRIPT", "ONLOAD"].indexOf(id) + 1){
			this.parentObj.component[id.toLowerCase()+"List"].parse(this.parentObj.data[this.parentObj.activeID][id], "json")
		}
	})
	infoTabbar.attachEvent("onSelect", function(id, lastId){
		if(["LINK","CSS"].indexOf(id) + 1){
			this.parentObj.popupAction("LINK", {options:{script:{VALUE:this.parentObj.data[this.parentObj.activeID][id], type:id.toLowerCase()}}});
			return false;
		}
		return true;
	});
	
	var listObjOptions = {
		"isDynamic":0,
		"drag":false,
		type:{
			template:"<table width=100% class=list_template>"
					+"	<tr>"
					+"		<td style=width:40px><div class='deleteBtn xi-close'/></td>"
					+"		<td style=width:60px>#type#</td>"
					+"		<td>#NAME#</td>"
					+"	</tr>"
					+"</table>"
		}
	}
	
	var onItemClick = function(id, ev){
		if(ev.srcElement.type == "button" && ev.srcElement.className == 'deleteBtn'){
			this.remove(id);
			var sid = this.parentObj.activeID;
			var type = this.parentObj.component.infoTabbar.getActiveTab();
			var data = this.parentObj.data[sid][type]
			for(var i in data){
				if(data[i].id == id){
					this.parentObj.history.addSnapShot(sid+" / "+data[i].type+(data[i].NAME?(" / "+data[i].NAME):"") +" / "+ this.parentObj._TextData("SCREEN_BUILDER_HISTORY_"+type.toUpperCase()+"_DELETE"))
					data.splice(i, 1)
					break;
				}
			}
			return false;
		}else{
			var item = this.get(id);
			this.parentObj.popupAction('EDIT_FUNCTION', {options:{script:item}, type:this.parentObj.component.infoTabbar.getActiveTab()}, this);
		}
	}
	
	for(var i in {'SCRIPT':0, 'ONLOAD':0}){
		var cellDiv = infoTabbar.tabs(i).attachDiv(i.toLowerCase()+'TabCell');
		
		var btnDiv = document.createElement('div');
		cellDiv.appendChild(btnDiv);
		btnDiv.style.height="50px";
		btnDiv.style.borderBottom="1px solid #dfdfdf";
		btnDiv.style.display = "flex";
		btnDiv.style.alignItems ="center";
		btnDiv.style.justifyContent ="flex-end";
		
		
		var btn = document.createElement('div');
		btnDiv.appendChild(btn);
		btn.setAttribute("class", "xi-plus");
		btn.textContent=this._TextData("SCREEN_BUILDER_BUTTON_NEW_SCRIPT");
		btn.style.padding = "5px 5px 5px 30px";
		btn.setAttribute("style", "height: 20px; margin-right: 20px; text-align: end; padding: 0 10px; box-shadow: 1px 1px 2px 1px #dfdfdf; cursor: pointer; background-color: white; border: 1px solid #c8c8c8; margin: 0; padding-top: 6px; font-size: 13px;");
		btn.editorObj=this;
		btn.onclick=function(){
			this.editorObj.popupAction.apply(this.editorObj, ['EDIT_FUNCTION', {type:this.editorObj.component.infoTabbar.getActiveTab()}]);
		}
		
		var listDiv = document.createElement('div');
		cellDiv.appendChild(listDiv);
		var options = $.extend(true, {container:listDiv}, listObjOptions);
		var list = this.component[i.toLowerCase()+"List"] = new SepoaDhtmlXList(options);
		list.parentObj = this;
		list.attachEvent("onItemClick", onItemClick);
	}
	
	var infoForm = this.component.infoForm = infoTabbar.tabs("INFO").attachForm(null, "infoForm",[
	[
		{type: "settings", inputWidth:"300", position:"label-top", inputWidth:150},
		{type: "block", className: "infoForm", list: [
			{type: "block", list: [
				{type: "select", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PROJECT"), name: "PROJECT", disabled:true, options:formModuleOption},
			]},
			{type: "block", list: [
				{type: "input", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH_1"), name: "PATH_1"},
			]},
			{type: "block", list: [
				{type: "input", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH_2"), name: "PATH_2"},
				
			]},
			{type: "block", list: [
				{type: "input", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH_3"), name: "PATH_3", disabled:true, hidden: true},
			]},
			{type: "block", list: [
				{type: "input", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_NAME"), name: "NAME"}
			]},
			{type: "block", list: [
				{type: "input", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH"), name: "PATH", disabled:true}
			]},
			{type: "block", list: [
				{type: "input", width:"142", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PREFIX"), name: "PREFIX", placeholder:this._TextData("SCREEN_BUILDER_INFO_FORM_PREFIX_PLACEHOLDER")},
				{type: "newcolumn"},
				{type: "input", width:"142", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_CODE"), name: "CODE", placeholder:this._TextData("SCREEN_BUILDER_INFO_FORM_CODE_PLACEHOLDER")}
			]}
		]}
	]]);
	infoForm.parentObj = this;
	infoForm.attachEvent("onChange", function(name, value){
		infoForm.parentObj.history.enable(false);
		var NONSTD_PATH = this.getItemValue('NONSTD_PATH');
		if(["PATH_1","PATH_2"].indexOf(name)+1 && NONSTD_PATH != 'Y'){
			this.setItemValue("PATH_3", this.getItemValue("PATH_1") + this.getItemValue("PATH_2"), true);
		}
		if(name == "NONSTD_PATH"){
			if(value == 'Y'){
				this.enableItem('PATH_3');
				this.hideItem('PATH_2')
				this.setItemValue('PATH_2', '', true)
				this.setItemValue('PATH_3', '')
			}else{
				this.disableItem('PATH_3');
				this.showItem('PATH_2')
				this.setItemValue('PATH_1', '', true)
				this.setItemValue('PATH_2', '')
			}
		}else if(name.indexOf("PATH") + 1 || name == "NAME"){
			var path_1 = this.getItemValue("PATH_1");
			var path_2 = this.getItemValue("PATH_2");
			var path_3 = this.getItemValue("PATH_3");
			var n = this.getItemValue("NAME");
			
			var pathArray = [];
			if(path_1 != ""){
				pathArray.push(path_1)
			}
			if(NONSTD_PATH != "Y" && path_2 != ""){
				pathArray.push(path_2)
			}
			if(path_3+n != ""){
				pathArray.push(path_3 + n)
			}
			
			var path = "/" +pathArray.join("/") + ".xml"
			
			var oPath = this.getItemValue("PATH");
			
			this.setItemValue("PATH", path, true);
			
			infoForm.parentObj.data[infoForm.parentObj.activeID] = $.extend(true, infoForm.parentObj.data[infoForm.parentObj.activeID], {
				PATH_1 : path_1,
				PATH_2 : path_2,
				PATH_3 : path_3,
				NAME : n,
				PATH : path,
				NONSTD_PATH : NONSTD_PATH
			})
			
			infoForm.parentObj.updateInfo('p', oPath, path);
			
			if(name != "NAME"){
				Confirm(infoForm.parentObj._TextData("SCREEN_BUILDER_MESSAGE_0004"), function(flag){
					if(flag){
						for(var i in infoForm.parentObj.data){
							if(i != infoForm.parentObj.activeID){
								var oP = infoForm.parentObj.data[i].PATH;
								var pathArray = [];
								if(path_1 != ""){
									pathArray.push(path_1)
								}
								if(NONSTD_PATH != "Y" && path_2 != ""){
									pathArray.push(path_2)
								}
								if(path_3+n != ""){
									pathArray.push(path_3 + n)
								}
								var nP = "/" + pathArray.join("/") + ".xml"
								infoForm.parentObj.data[i] = $.extend(true, infoForm.parentObj.data[i], {
									PATH_1 : path_1,
									PATH_2 : path_2,
									PATH_3 : path_3,
									PATH : nP,
									NONSTD_PATH : NONSTD_PATH
								})
								infoForm.parentObj.updateInfo('p', oP, nP);
							}
						}
					}
				})
			}
		}else if(["PREFIX", "CODE"].indexOf(name) + 1){
			if(name == "PREFIX"){
				this.setItemValue("PREFIX", value.toUpperCase(), true);
			}
			var prefix = this.getItemValue("PREFIX");
			var code = this.getItemValue("CODE");
			
			var id = ((prefix)?prefix+"_":"") + code;
			var beforeID = infoForm.parentObj.activeID;
			
			infoForm.parentObj.data[id] = $.extend(true, infoForm.parentObj.data[beforeID] , {
				CODE:code,
				PREFIX:prefix,
				SCREEN_ID:id
			});
			delete infoForm.parentObj.data[beforeID];
			
			if(!infoForm.parentObj.data[id].orgId){
				infoForm.parentObj.data[id].orgId = beforeID;
			}
			
			infoForm.parentObj.mockup[id] = infoForm.parentObj.mockup[beforeID]
			delete infoForm.parentObj.mockup[beforeID];
			
			var index = infoForm.parentObj.component.screenTabbar.tabs(beforeID).getIndex();
			infoForm.parentObj.component.screenTabbar.addTab(id, id, null, index, true, true);
			infoForm.parentObj.component.screenTabbar.tabs(id).attachDiv().appendChild(infoForm.parentObj.mockup[id].vp)
			infoForm.parentObj.component.screenTabbar.tabs(beforeID).close();
			infoForm.parentObj.activeID = id;
			infoForm.parentObj.updateInfo('s', beforeID, id);
		}
		infoForm.parentObj.history.enable(true);
		infoForm.parentObj.history.addSnapShot(this._TextData("HISTORY_CHANGE_INFO_PANAL")+id)
	})
	infoForm.attachEvent("onBeforeChange", function(name, oValue, nValue){
		if(["PREFIX", "CODE"].indexOf(name) + 1){
			var prefix,code;
			if(name == "PREFIX"){
				prefix = nValue;
				code = this.getItemValue("CODE");
			}else{
				prefix = this.getItemValue("PREFIX");
				code = nValue;
			}
			
			var id = (((prefix)?prefix+"_":"") + code).toUpperCase();
			if(infoForm.parentObj.data[id]){
				Alert(infoForm.parentObj._TextData("SCREEN_BUILDER_MESSAGE_0005") + id);
				return false;
			}
		}
		return true;
	})
	
	var screenTabbar = this.component.screenTabbar = innerLayout.cells('b').attachTabbar({
		skin:this.config.skin,
		mode:"top",
		align:"left",
		content_zone:true,
		arros_mode:true,
		tabs:[
			{id:"ADD", text:"<div style='display: block;'>추가 <div class='screen_add_button_item xi-plus' /></div>", close:false}
		]
	});
	screenTabbar.addTab=function(a, w, o, u, s, x) {
        x = (typeof(x) == "undefined" ? (this.conf.closeButton == true) : window.dhx4.s2b(x));
        s = window.dhx4.s2b(s);
        var r = document.createElement("DIV");
        r.className = "dhxtabbar_tab";
        r.innerHTML = "<div draggable='true' class='dhxtabbar_tab_text" + (x ? " dhxtabbar_tab_text_close" : "") + "'>" + w + "</div>" + (x ? "<div class='dhxtabbar_tab_close'></div>" : "");
        r._tabId = a;
        if (x) {
            r.childNodes[1]._tabCloseId = a
        }
        var n = this.tabsArea.childNodes[1].firstChild;
        if (u != null && u + 1 > 0 && u + 1 < n.childNodes.length) {
            n.insertBefore(r, n.childNodes[u + 1])
        } else {
            n.appendChild(r)
        }
        var y = false;
        if (typeof(o) == "undefined" || o == null || o == "*") {
            o = this._getLabelWidth(w, x);
            y = true
        } else {
            o = parseInt(o)
        }
        r.style.width = o + "px";
        var v = new dhtmlXTabBarCell(a, this);
        this.tabsMode.appendChild(v.cell);
        this.t[a] = {
            tab: r,
            cell: v,
            conf: {
                text: w,
                visible: true,
                active: false,
                enabled: true,
                close: x,
                width: o,
                autowidth: y
            }
        };
        n = v = null;
        if (s) {
            this._setTabActive(a)
        } else {
            this._adjustCell(a)
        }
        if (this.conf.initSeq != true && this.conf.arwMode == "auto") {
            this.setSizes()
        }
        r.ondragstart=function(e){
        	e.dataTransfer.setData("type", "screen");
        	e.dataTransfer.setData("value", this._tabId);
        }
    };
	
	screenTabbar.parentObj = this;
	screenTabbar.attachEvent("onSelect", function(id, lastId){
		if("ADD" == id){
			var ar = this.getAllTabs();
			if(ar.length == 1){
				this.parentObj.popupAction("MENU_NEW");
			}else if(lastId){
				if(this.parentObj.mode == "TEMPLATE"){
					var max = 0;
					for(var i in this.t){
						var m = i.match(/^%PREFIX%_%CODE_([0-9]{0,})%$/);
						if(m && m[1] > max){
							max = (m[1] * 1);
						}
					}
					this.parentObj.addScreen({info:{
		            	PATH_1:"%PATH_1%",
		            	PATH_2:"%PATH_2%",
		            	PATH_3:"%PATH_1%%PATH_2%",
		            	NAME:"",
		            	PREFIX:"%PREFIX%",
		            	CODE:"%CODE_"+(max + 1)+"%"
            		}});
				}else{
					this.parentObj.popupAction("ADD_SCREEN")
				}
			}else{
				if(ar[ar.length - 1] != "ADD"){
					this.tabs(ar[ar.length - 1]).setActive();
				}
			}
			return false;
		}
		return true;
	});
	screenTabbar.attachEvent("onTabChanged", function(id){
		this.parentObj.history.enable(false);
		if(["ADD"].indexOf(id) < 0){
			this.parentObj.changeScreenInfoPanel(id);
		}
		this.parentObj.history.enable(true);
	})
	
	var that = this;
	screenTabbar.attachEvent("onTabClose", function(id){
		var targetTab = this;
		Confirm(that._TextData("SCREEN_BUILDER_MESSAGE_0009"),function(result){//삭제 하시겠습니까?
            if(result){
            	delete targetTab.parentObj.data[id]
        		delete targetTab.parentObj.mockup[id]
        		if(targetTab.getNumberOfTabs() <= 1){
        			targetTab.parentObj.component.infoForm.clear();
        		}
            	
            	//Confirm 함수와의 시점 충돌로인해 수동 close문 적용
            	targetTab.parentObj.component.screenTabbar.tabs(id).close();
            	
            	that.history.addSnapShot(id + " / "+that._TextData('SCREEN_BUILDER_HISTORY_SCREEN_CLOSE'));
            }
        });
	});
	this.config.isDraw = true;
}
SepoaScreenBuilder.prototype.popupAction=function(id, ext){
	var data = {
		LINK:{
			getArgs:function(){
				return [
					"/Tools/ScreenBuilder/Popup/ScriptPopup.sux",
					$.extend(true, {
						callback:function(data){
							var sid = this.component.screenTabbar.getActiveTab();
							this.data[sid][data.type.toUpperCase()] = data.VALUE;
							
							this.history.addSnapShot(sid+" / "+this._TextData("SCREEN_BUILDER_HISTORY_LINK_MODIFY"))
							return {status:1}
						},
						callObj:this,
						form:{
							script:[
								{type: "hidden", inputWidth: "80", name: "TYPE"},
								{type: "container", inputWidth: "800", inputHeight: "500", name: "EDITOR_VALUE", label:this._TextData("SCREEN_BUILDER_SCRIPT_POPUP_CONTENTS")}
							]
						}
					}, ext || {}),
					ext.options.script.type.toUpperCase()
				]		
			}
		},
		MENU_NEW:{
			getArgs:function(){
				return [
					"/Tools/ScreenBuilder/Popup/NewProject2.sux",
					{
						callback:function(data, mode){
							this.initProject($.extend(true, {}, data), mode)
							return {status:1}
						},
						formModuleOption:formModuleOption,
						callObj:this,
						ext:ext
					}
				]
			}
		},
		MENU_LOAD:{
			getArgs:function(){
				var that = this;
				return [
					"/Tools/ScreenBuilder/Popup/CommonList.sux",
					{
						text:this._TextData("SCREEN_BUILDER_LOAD_TEXT"),
						callback:function(data){
							return that.load(data.url);
						},
						SERVLET_URL:window.CONTEXT_NAME+"/SepoaList.sux",
						METHOD:"doQuery",
						PROCESS_ID:"sepoa.svc.tools.ProgramList",
						TEMPLATE:'<table class=list_template ><tr><td>[ #screen_id# ] <span class=listBold >#menu_name# > #screen_name#</span></td><td class=listRight ></td></tr><tr><td colspan=2 >#url#</td></tr></table>',
						callObj:this
					},
					this._TextData("SCREEN_BUILDER_LOAD_TITLE"),
					650,
					800
				]
			}
		},
		SAVE_TEMPLATE:{
			getArgs:function(){
				var that = this;
				return [
					"/Tools/ScreenBuilder/Popup/SaveTemplate.sux",
					ext,
					this._TextData("SAVE_TEMPLATE"),
					600,
					91
				]
			}
		},
		ADD_SCREEN:{
			getArgs:function(){
				var formData = this.component.infoForm.getFormData();
				formData.PATH = "";
				formData.NAME = "";
				formData.CODE = "";
				
				var formOption = null;
				if(formData.NONSTD_PATH != 'Y'){
					formOption = [
						{type: "fieldset", width: "340", label:this._TextData("SCREEN_BUILDER_INFO_FORM_CATEGORY"), list: [
							{type: "select", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PROJECT"), name: "PROJECT", disabled:true, options:formModuleOption},
							{type: "block", width: "306", list: [
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH_1"), name: "PATH_1", disabled:true},
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH_3"), name: "PATH_3", disabled:true},
								{type: "newcolumn"},
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH_2"), name: "PATH_2", disabled:true},
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_NAME"), name: "NAME", required:true}
							]},
							{type: "input", width:"285", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH"), name: "PATH", disabled: true}
						]},
						{type: "fieldset", width: "350", label: this._TextData("SCREEN_BUILDER_INFO_FORM_ID"), list: [
							{type: "block", width: "306", list: [
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PREFIX"), name: "PREFIX", required:true},
								{type: "newcolumn"},
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_CODE"), name: "CODE", required:true}
							]}
						]}
					]
				}else{
					formOption = [
						{type: "fieldset", width: "340", label:this._TextData("SCREEN_BUILDER_INFO_FORM_CATEGORY"), list: [
							{type: "block", width: "306", list: [
								{type: "select", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PROJECT"), name: "PROJECT", disabled:true, options:formModuleOption},
								{type: "newcolumn"},
								{type: "select", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_DIR_NONSTD"), name: "NONSTD_PATH", disabled:true, options:[
									{text:"", value:""},
									{text:"Y", value:"Y"}
								]}
							]},
							{type: "block", width: "306", list: [
								{type: "input", width:"280", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH_1"), name: "PATH_1", disabled:true},
								{type: "newcolumn"},
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH_2"), name: "PATH_2", disabled:true, hidden:true},
							]},
							{type: "block", width: "306", list: [
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH_3"), name: "PATH_3", disabled:true},
								{type: "newcolumn"},
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_NAME"), name: "NAME", required:true}
							]},
							{type: "input", width:"285", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PATH"), name: "PATH", disabled: true}
						]},
						{type: "fieldset", width: "350", label: this._TextData("SCREEN_BUILDER_INFO_FORM_ID"), list: [
							{type: "block", width: "306", list: [
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_PREFIX"), name: "PREFIX", required:true},
								{type: "newcolumn"},
								{type: "input", width:"140", value: "", label:this._TextData("SCREEN_BUILDER_INFO_FORM_CODE"), name: "CODE", required:true}
							]}
						]}
					]
				}
				
				return [
					"/Tools/ScreenBuilder/Popup/AddScreen.sux",
					{
						callback:function(data){
							return this.addScreen(data);
						},
						formOption:formOption,
						data:formData,
						callObj:this
					},
					"Add Screen"
				]
			}
		},
		EDIT_FUNCTION:{
			getArgs:function(){
				var typeOptions = [];
				var templateOptions = [];
				var scriptList = null;
				var buttonFunctionList = null;
				
				templateOptions.push({text: "", value: ""});
				templateOptions.push({text: "Grid", value: "//dataOutput.addFilter(filterObj)\n//dataOutput.setValue('', '');\n\nvar processID = '';\nvar methodID = '';\nvar grid = gridObj;\n\nvar params = [];\nparams.push('mode=doQuery');\nparams.push('PROCESS_ID=' + processID);\nparams.push('METHOD=' + methodID);\n\ngrid.postConnection(window.CONTEXT_NAME + '/SepoaGrid.sux', params);\ngrid.clearAll(false);"});
				templateOptions.push({text: "List", value: "//dataOutput.addFilter(filterObj)\n//dataOutput.setValue('', '');\n\nvar processID = '';\nvar methodID = '';\nvar list = listObj;\n\nvar params = '';\nparams += '?mode=doList';\nparams += '&PROCESS_ID=' + processID;\nparams += '&METHOD=' + methodID;\nparams += dataOutput.getParams();\n\nlist.load(window.CONTEXT_NAME + '/SepoaList.sux' + params);\nlist.clearAll(false);"});
				templateOptions.push({text: "Tree", value: "//dataOutput.addFilter(filterObj)\n//dataOutput.setValue('', '');\n\nvar processID = '';\nvar methodID = '';\nvar tree = treeObj;\n\nvar params = '';\nparams += '?mode=doTree';\nparams += '&PROCESS_ID=' + processID;\nparams += '&METHOD=' + methodID;\nparams += dataOutput.getParams();\n\ntree.loadStruct(window.CONTEXT_NAME + '/SepoaTree.sux' + params);\ntree.clearAll(false);"});
				templateOptions.push({text: "DataView", value: "//dataOutput.addFilter(filterObj)\n//dataOutput.setValue('', '')\n\nvar processID = '';\nvar methodID = '';\nvar dataView = dataviewObj;\n\nvar params = '';\nparams += '?mode=doList';\nparams += '&PROCESS_ID=' + processID;\nparams += '&METHOD=' + methodID;\nparams += dataOutput.getParams();\n\ndataView.load(window.CONTEXT_NAME + '/SepoaList.sux' + params);"});
				templateOptions.push({text: "doServiceAjax", value: "//dataOutput.setValue('', '');\n\n// if use dataOutput then useDataOutput = true and put the key, value in params\nvar useDataOutput = false;\nvar params = null;\n\nif(useDataOutput) {\n\tparams = {'key': 'value'};\n}\n\nvar processID = '';\nvar methodID = '';\nvar execMode = 'CONNECTION'\n\ndoServiceAjax(processID, execMode, methodID, params, function(so){\n\tif(so.status != 1) {\n\t\tAlert(so.message);\n\t\treturn;\n\t}\n\n\tvar sf = new SepoaFormater(so.result[0]);\n\n\tif(sf.getRowCount() > 0) {\n\t\t\n\t}\n});"})
				
				if(ext.type == 'ONLOAD'){
					typeOptions.push({text:"call", value:"call"})
					typeOptions.push({text:"text", value:"text"})
					typeOptions.push({text:"event", value:"event"})
					typeOptions.push({text:"var", value:"var"});
					scriptList = this.data[this.component.screenTabbar.getActiveTab()].ONLOAD;
				}else if(ext.type == 'SCRIPT'){
					typeOptions.push({text:"function", value:"function"})
					typeOptions.push({text:"var", value:"var"});
					scriptList = this.data[this.component.screenTabbar.getActiveTab()].SCRIPT;
				}else if(ext.type == 'F'){
					typeOptions.push({text:"function", value:"function"})
					scriptList = this.data[this.component.screenTabbar.getActiveTab()].SCRIPT;
				}
				return [
					"/Tools/ScreenBuilder/Popup/ScriptPopup.sux",
					$.extend(true, {
						callback:function(data){
							var listObj = this.component[this.component.infoTabbar.getActiveTab().toLowerCase()+"List"];
							var sid = this.component.screenTabbar.getActiveTab();
							var type = this.component.infoTabbar.getActiveTab();
							
							if(data.id){
								var row = listObj.get(data.id)
								var ka = [];
								for(var i in row){
									if(!(['id', '$selected', 'randomseed'].indexOf(i) + 1)){
										ka.push(i)
									}
								}
								for(var i in ka){
									delete row[ka[i]];
								}
								$.extend(true, row, data);
								this.history.addSnapShot(sid+" / "+data.type + (data.NAME?(" / "+data.NAME):"") +" / "+ this._TextData("SCREEN_BUILDER_HISTORY."+type.toUpperCase()+"_MODIFY"))
							}else{
								listObj.add(data);
								this.data[sid][type].push(data);
								this.history.addSnapShot(sid+" / "+data.type + (data.NAME?(" / "+data.NAME):"") +" / " + this._TextData("SCREEN_BUILDER_HISTORY."+type.toUpperCase()+"_ADD"))
							}
							listObj.refresh();
							return {status:1}
						},
						callObj:this,
						form:{
							script:[
								{type: "block", width: "auto", list: [
									{type: "select", name: "TYPE", label:this._TextData("SCREEN_BUILDER_SCRIPT_POPUP_TYPE"), options:typeOptions},
									{type: "newcolumn"},
									{type: "input", name: "OBJECT", label:this._TextData("SCREEN_BUILDER_SCRIPT_POPUP_OBJECT")},
									{type: "newcolumn"},
									{type: "input", name: "NAME", label:this._TextData("SCREEN_BUILDER_SCRIPT_POPUP_NAME"), required:true},
									{type: "newcolumn"},
									{type: "select", hidden:true, name: "CALLBACK_TYPE", label:this._TextData("SCREEN_BUILDER_SCRIPT_POPUP_CALLBACK_TYPE"), options:[
										{text:"name", value:"name"},
										{text:"function", value:"function"}
									]},
									{type: "newcolumn"},
									{type: "input", hidden:true, name: "CALLBACK", label:this._TextData("SCREEN_BUILDER_SCRIPT_POPUP_CALLBACK")},
									{type: "hidden", name: "VALUE"},
									{type: "newcolumn"},
									{type: "input", name: "ARGS", label:this._TextData("SCREEN_BUILDER_SCRIPT_POPUP_ARGS")},
									{type: "newcolumn"},
									{type: "select", name: "TEMPLATE_TYPE", label:this._TextData("SCREEN_BUILDER_SCRIPT_POPUP_TEMPLATE_TYPE"), options:templateOptions}
								]},
								{type: "container", name: "EDITOR_VALUE", label:this._TextData("SCREEN_BUILDER_SCRIPT_POPUP_CONTENTS")}
							]
						},
						script:scriptList
					}, ext || {}),
				]
			}
		},
		MENU_PROGRAMLIST:{
			getArgs:function(){
				return [
					"/Tools/ScreenBuilder/Popup/ProgramList.sux",
					{},
					this._TextData('SCREEN_BUILDER_POPUP_PROGRAMLIST_TITLE'),
					444
				]
			}
		},
		SAVE_PROJECT:{
			getArgs:function(){
				var that = this;
				return [
					"/Tools/ScreenBuilder/Popup/SaveProject.sux",
					{ 
						initSaveData : ext,
						callback:function(data){
							that.doSaveProject(data)
						}
					},
					this._TextData('SCREEN_BUILDER_SAVE_TITLE'),
					800,
					420,
				]
			}
		}
	}
	
	function callPopup(that, url, params, title, width, height){
		var code = id
		
		width = width||(SUX.getModuleTop().outerWidth * 0.8);
		height = height||(SUX.getModuleTop().outerHeight * 0.8);
		params = params || {};
		title = title||"";
		
		if(Sepoa.window[code] && Sepoa.window[code].unload)
			Sepoa.window[code].unload();
		
		var mainContainer = SUX.getModuleTop().document.getElementById('container');
		var popup = Sepoa.window[code] = new SepoaDhtmlXWindowsObject();
		popup.setSkin('material');
		popup.createPopup(code || 'commonPopup',title,undefined,width,height);
		popup.setViewport(0,0,0,0,mainContainer);
		popup.attachURL(window.CONTEXT_NAME+url, params);
		popup.setShow();
		return popup;
	}
	callPopup.apply(this, [this].concat(data[id].getArgs.apply(this, [])))
}
SepoaScreenBuilder.prototype.initProject=function(data, mode){
	this.history.clear();
	this.history.enable(false);
	this.drawEditor();
	this.clearAll();
	this.changeMode(mode);
	this.setAllData(data);
	if(data){
		for(var i in data){
			//this.ListTabSort(data[i],['SCRIPT']);
			this.addScreen(data[i]);
		}
	}
	this.clearAllData();
	this.component.menu.setItemEnabled("PREVIEW");
	this.component.menu.setItemEnabled("NOW_REFRESH");
	this.component.menu.setItemEnabled("ALL_REFRESH");
	this.history.enable(true);
	this.history.addSnapShot(this._TextData('HISTORY.INIT'));
}
SepoaScreenBuilder.prototype.setAllData=function(data){
	this.mockup.allData = data;
}
SepoaScreenBuilder.prototype.clearAllData=function(){
	delete this.mockup.allData;
}
SepoaScreenBuilder.prototype.ListTabSort=function(data, tabIds, sort){
	var as,bs,u = undefined;
	sort = (sort == null ? 'asc' : sort)
	if( typeof(tabIds) == 'string'){
		tabIds = [tabIds]
	}
	for(var i=0, item; tabId=tabIds[i]; i++) {
		if(data.info[tabId]!=u){
			data.info[tabId].sort(function(a, b) {
				as = a['NAME'];
				bs = b['NAME'];
				return ( sort == 'asc' ? ((u == as?"":as) < (u == bs?"":bs) ? -1 : (u == as?"":as) > (u == bs?"":bs) ? 1 : 0) :( (u == as?"":as) > (u == bs?"":bs) ? -1 : (u == as?"":as) < (u == bs?"":bs) ? 1 : 0 ));
			});
		}
	}
}
SepoaScreenBuilder.prototype.changeMode=function(m){
	this.mode = m;
	if(m == "TEMPLATE"){
		this.component.infoForm.hideItem('PROJECT');
		this.component.infoForm.hideItem('PATH_1');
		this.component.infoForm.hideItem('PATH_2');
//		this.component.infoForm.hideItem('PATH_3');
		this.component.infoForm.hideItem('NONSTD_PATH');
		this.component.infoForm.hideItem('PATH');
		this.component.infoForm.hideItem('SCREEN_FIELD');
		this.component.infoForm._apiConnector('enableItem', 'PROJECT')
		this.component.menu.setItemDisabled("SWITCH_TEMPLATE");
		
	}else{
		this.component.infoForm.showItem('PROJECT')
		this.component.infoForm.showItem('PATH_1');
		this.component.infoForm.showItem('PATH_2');
//		this.component.infoForm.showItem('PATH_3');
		this.component.infoForm.showItem('NONSTD_PATH');
		this.component.infoForm.showItem('PATH');
		this.component.infoForm.showItem('SCREEN_FIELD');
		this.component.infoForm._apiConnector('disableItem', 'PROJECT')
		this.component.menu.setItemEnabled("SWITCH_TEMPLATE");
	}
}
SepoaScreenBuilder.prototype.updateInfo=function(type, o, n){
	for(var i in this.mockup){
		this.mockup[i].updateInfo(type, o, n);
	}
}
SepoaScreenBuilder.prototype.addScreen=function(data){
	this.history.enable(false);
	var index;
	var info = data.info
	var sid = ((info.PREFIX)?info.PREFIX+"_":"")+info.CODE;
	
	this.orgData[sid] = $.extend(true, {}, data);
	
	if(this.mockup[sid]){
		return {status:0, message:this._TextData('SCREEN_BUILDER_MESSAGE_0007')};
	}
	
	info.SCREEN_ID = sid;
	
	if(this.mode != "TEMPLATE"){
		var pathArray = [];
		if(info.NONSTD_PATH != 'Y'){
			pathArray.push(info.PATH_1, info.PATH_2,info.PATH_3+info.NAME);
		}else{
			pathArray.push(info.PATH_1 ,info.PATH_3+info.NAME);
		}
		
		info.PATH = "/"+pathArray.join("/") + ".xml"
		for(var i in this.data){
			if(this.data[i].PATH == info.PATH){
				return  {status:0, message:this._TextData('SCREEN_BUILDER_MESSAGE_0008')};
			}
		}
	}
	
	if(!info.SCRIPT) info.SCRIPT = [];
	if(!info.ONLOAD) info.ONLOAD = [];
	
	this.data[sid]=info;
	this.changeScreenInfoPanel(sid);
	
	if(typeof(data.index) == "undefined") {
		index = this.component.screenTabbar.getAllTabs().length - 1;
	} else {
		index = parseInt(data.index);
	}
	
	this.component.screenTabbar.addTab(sid, sid, null, index, true, true);
	this.mockup[sid] = new SepoaUXMockup(this.component.screenTabbar.tabs(sid).attachDiv(), data, this.mode=="TEMPLATE"?true:undefined);
	this.mockup[sid].parentObj = this;
	
	this.mockup[sid].attachEvent('onUpdateUserScript', function(s, u){
		for(var i in u){
			var name = u[i].NAME;
			var flag = true;
			for(var j in this.parentObj.data[s].SCRIPT){
				if(this.parentObj.data[s].SCRIPT[j].NAME == name){
					$.extend(true, this.parentObj.data[s].SCRIPT[j], u[i])
					flag = false;
				}
			}
			if(flag){
				if(!this.parentObj.data[s].SCRIPT){
					this.parentObj.data[s].SCRIPT = [];
				}
				this.parentObj.data[s].SCRIPT.push(u[i]);
			}
		}
		if(this.parentObj.component.infoTabbar.getActiveTab() == 'SCRIPT'){
			this.parentObj.component.infoTabbar.callEvent('onTabChanged', ['SCRIPT']);
		}
	})
	this.mockup[sid].attachEvent('requestScriptEditor', function(ext){
		this.parentObj.popupAction.apply(this.parentObj, ['EDIT_FUNCTION', ext])
	})
	this.mockup[sid].attachEvent('onAddHistory', function(text){
		this.parentObj.history.addSnapShot(text)
	})
	this.component.infoTabbar.tabs('INFO').enable();
	this.component.infoTabbar.tabs('INFO').setActive();
	this.component.infoTabbar.tabs('SCRIPT').enable();
	this.component.infoTabbar.tabs('ONLOAD').enable();
	this.component.infoTabbar.tabs('LINK').enable();
	this.component.infoTabbar.tabs('CSS').enable();
	
	this.history.enable(true);
	this.history.addSnapShot(sid + " / "+this._TextData("SCREEN_BUILDER_HISTORY_SCREEN_ADD"))
	return {status:1};
}
SepoaScreenBuilder.prototype.clearAll=function(){
	this.data = {};
	this.mockup = {};
	this.orgData = {};
	
	var tabs = this.component.screenTabbar.getAllTabs();
	for(var i in tabs){
		if(["ADD"].indexOf(tabs[i]) < 0){
			this.component.screenTabbar._removeTab(tabs[i], false, true);
		}
	}
	
	this.component.menu.setItemDisabled("PREVIEW")
	this.component.menu.setItemDisabled("NOW_REFRESH");
	this.component.menu.setItemDisabled("ALL_REFRESH");
}
SepoaScreenBuilder.prototype.refresh=function(id){
	var activeTabId = this.activeID
	this.history.enable(false);
	switch(id){
		case "NOW_REFRESH":
			var idx = this.component.screenTabbar.tabs(activeTabId).getIndex();
			var orgId = ((this.data[activeTabId].orgId) ? this.data[activeTabId].orgId : activeTabId)
			
			delete this.data[activeTabId];
			delete this.mockup[activeTabId];
			
			this.component.infoForm.clear();
			this.component.screenTabbar._removeTab(activeTabId, false, true);
			this.orgData[orgId].index = idx;
			
			this.addScreen(this.orgData[orgId]);
			this.history.enable(true);
			this.history.addSnapShot(sid + " / " + this._TextData("SCREEN_BUILDER_HISTORY_REFRESH_NOW"))
			break;
		case "ALL_REFRESH":
			this.initProject(this.orgData);
			this.component.screenTabbar._setTabActive(activeTabId);
			this.history.enable(true);
			this.history.addSnapShot(this._TextData("SCREEN_BUILDER_HISTORY_REFRESH_ALL"))
			break;
	}
	
}
SepoaScreenBuilder.prototype.openPreview=function(){
	var d = {};
	var flag = true;
	var previewID = (function(){
		 return ([1e7]+[1e3]+[4e3]+[8e3]+[1e11]).replace(/[018]/g, function(c){
			   var cryptoFunc = ( window._isChrome ?window.crypto : window.msCrypto ); 
			   return (c ^ cryptoFunc.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
			  })
		})();
	
	var data = this.data
	var checkIncludePath = function(t){
		for(var i in data){
			if(data[i].PATH == t){
				return i;
			}
		}
		return false;
	}
	var renameTreeData = function(obj){
		if(obj.id) obj.id = obj.id.replaceAll("%", "_");
		if(obj.cells)
			for(var i in obj.cells){
				obj.cells[i] = renameTreeData(obj.cells[i])
			}
		return obj;
	}
	
	var childs = [];
	var sidList = [];
	
	for(var i in this.mockup){
		flag = false;
		sidList.push(i);
		d[i] = $.extend(true, {}, this.mockup[i].exportData());
		d[i].info = $.extend(true, {}, this.data[i])
		if(!d[i].info.PATH){
			d[i].info.PATH = i+".xml";
		}
		d[i].info.PATH = '/__Builder_Preview__/'+previewID + d[i].info.PATH.replaceAll("%", "_");
		d[i].info.PROJECT = 'FRWK';
		delete d[i].info.ONLOAD 
		
		var renameObjects = {};
		for(var j in d[i].objects){
			var obj = d[i].objects[j];
			if('layoutCell' == obj.type){
				if(obj.component.ATTACH_TYPE == 'U'){
					var s = checkIncludePath(obj.component.ATTACH_TARGET);
					if(s !== false){
						obj.component.ATTACH_TARGET = '/__Builder_Preview__/' + previewID + obj.component.ATTACH_TARGET.replaceAll("%", "_")
						childs.push(s);
					}
				}else if(obj.component.ATTACH_TYPE == 'S'){
					if(childs.indexOf(obj.component.ATTACH_TARGET) < 0){
						childs.push(obj.component.ATTACH_TARGET);
					}
				}
			}else if(['carousel', 'tabbar'].indexOf(obj.type)+1){
				for(var k in obj.component){
					var m = k.match(/^ITEM_(\w{0,})_TYPE$/);
					if(m){
						if(obj.component[k] == 'U'){
							var s = checkIncludePath(obj.component['ITEM_'+m[1]+'_TARGET']);
							if(s !== false){
								obj.component['ITEM_'+m[1]+'_TARGET'] = '/__Builder_Preview__/' + previewID + obj.component['ITEM_'+m[1]+'_TARGET'].replaceAll("%", "_")
								childs.push(s);
							}
						}else if(obj.component[k] == 'S'){
							if(childs.indexOf(obj.component['ITEM_'+m[1]+'_TARGET']) < 0){
								childs.push(obj.component['ITEM_'+m[1]+'_TARGET']);
							}
						}
						break;
					}
				}
			}
			if(obj.id)
				obj.id = obj.id.replaceAll("%", "_");
			
			renameObjects[j.replaceAll("%", "_")] = obj;
		}
		d[i].objects = renameObjects; 
		d[i].tree = renameTreeData(d[i].tree)
	}
	
	if(flag){
		Alert(this._TextData('SCREEN_BUILDER_MESSAGE.0006'))
		return;
	}
	
	for(var i in childs){
		var p = sidList.indexOf(childs[i]);
		if(p >= 0){
			sidList.splice(p, 1);
		}
	}
	
	var t = d[sidList[0]].info.PATH;
	t = t.substring(0, t.length-3) + "sux";
	
	var url = window.CONTEXT_NAME+"/servlets/sepoa.fw.ux.builder.Preview";
	var params = "data" + Sepoa.common.FIELD + JSON.stringify(d) + Sepoa.common.FIELD 
			   + "previewID" + Sepoa.common.FIELD + previewID + Sepoa.common.FIELD
			   + "previewURL" + Sepoa.common.FIELD + t + Sepoa.common.FIELD
	params = {params:params};
	var width = null;//window.innerWidth;
	var height = null;//window.innerHeight;
	var title = this._TextData("SCREEN_BUILDER_");
	var code = "preview";
	
	PopupOpenPost(url, params, null, width, height, title, code);
}
SepoaScreenBuilder.prototype.changeScreenInfoPanel=function(id){
	var data = this.data[id];
	this.activeID = id;
	this.component.infoForm.dataMapper(data, "JSON")
	if(data.NONSTD_PATH == 'Y'){
		this.component.infoForm._apiConnector('enableItem', ['PATH_3']);
		this.component.infoForm.hideItem('PATH_2')
	}else{
		this.component.infoForm._apiConnector('disableItem', ['PATH_3']);
		if(this.mode != "TEMPLATE"){
			this.component.infoForm.showItem('PATH_2')
		}
	}
	
	var s = this.component.infoTabbar.getActiveTab();
	if(["ONLOAD", "SCRIPT"].indexOf(s) + 1){
		this.component[s.toLowerCase()+"List"].parse(data[s], "json")
	}
}
SepoaScreenBuilder.prototype.switchTemplate=function(){
	this.history.enable(false);
	var cnt = 1;
	var transMap = {};
	var d = {};
	for(var i in this.mockup){
		var code = "%CODE_"+(cnt)+"%";
		
		var h = d["%PREFIX%_"+code] = this.mockup[i].exportData()
		h.info = $.extend(true, this.data[i],{
			PATH_1:"%PATH_1%",
        	PATH_2:"%PATH_2%",
        	PATH_3:"%PATH_1%%PATH_2%",
        	NAME:this.data[i].NAME,
        	PREFIX:"%PREFIX%",
        	CODE:code,
        	SCREEN_ID:"%PREFIX%_"+code,
		})
		
		transMap[i] = "%PREFIX%_"+code;
		cnt++;
		
		for(var j in h.objects){
			var item = h.objects[j];
			if(["layoutCell", "carouselCell", "tabbarCell"].indexOf(item.type) +1){
				if(h.objects[j].component.ATTACH_TARGET){
					if(h.objects[j].component.ATTACH_TYPE != "S"){
						var sId = null;
						for(var k in this.data){
							if(this.data[k].PATH == h.objects[j].component.ATTACH_TARGET){
								sId = k;
								break;
							} 
						}
						if(sId){
							h.objects[j].component.ATTACH_TYPE = "S";
							h.objects[j].component.ATTACH_TARGET = sId;
						}
					} 
				}
			}
			transMap[j] = "%REPLACEID%"+(this.mockup[i].convert52digits(new Date().getTime()) + this.mockup[i].convert52digits(Math.random() * 380204032 | 0))
		}
	}
	
	var str = JSON.stringify(d);
	
	var sortedList = [];
	for(var i in transMap){
		sortedList.push({BEFORE:i, AFTER:transMap[i]});
	}
	sortedList.sort(function(a, b){
		return (a.BEFORE.length < b.BEFORE.length)?1:(a.BEFORE.length > b.BEFORE.length)?-1:0;
	})
	
	for(var i in sortedList){
		str = str.replaceAll(sortedList[i].BEFORE, sortedList[i].AFTER);
	}
	var result = JSON.parse(str);
	
	this.initProject(result, "TEMPLATE")
	this.history.enable(true);
	this.history.addSnapShot(this._TextData("SCREEN_BUILDER_HISTORY_SWITCH_TEMPLATE"))
}
SepoaScreenBuilder.prototype._TextData = function(t){
	var a = t.replace(".", "_");
	return Sepoa.cache.text.get("SCREEN_BUILDER."+a);
}
