//MenuManagement
SepoaMenuManagement = function(b, c){
	this.base = b;
	dhtmlx.extend(this, dhtmlx.Settings);
	dhtmlx.extend(this, dhtmlx.EventSystem);
	dhtmlx.extend(this, dhtmlx.DataLoader);
	this.config=$.extend({},{
		servlet:window.CONTEXT_NAME + "/SepoaList.sux",
		baseObj:this
	}, c)
	this.data.attachEvent("onStoreUpdated", dhtmlx.bind(function(){
		var menuItems = {}
		var menuItemList = [];
		try{
			this.data.each(dhtmlx.bind(function(menuData){
				menuData.childs = [];
			    menuItems[menuData.menu_id] = menuData;
			    menuItemList.push(menuData);
			}));
			
		    var menuTree = [];
		    for(var i in menuItemList){
		    	var item = menuItemList[i];
		    	var parent = item.parent_menu_id;
		        if(parent == "0"){
		        	menuTree.push(item);
		        }else{
		        	menuItems[item.parent_menu_id].childs.push(item);
		        }
		    }
		    
			for(var i=0;i<menuTree.length;i++){
				this.addGroup(menuTree[i], i==0?true:false);
			}
		}catch(e){
			console.log(e)
		}finally{
			this.progress(false);
		}
	}, this));
	
	this.component = {};
	this.group = {};
	this.renderBase();
}
SepoaMenuManagement.prototype.renderBase = function(){
	this.base.classList.add("sepoa_menumanagement");
	
	var profileInfoArea = this.base.appendChild(document.createElement('div'));
	profileInfoArea.classList.add("sepoa_menumanagement_info");
	var pofileFormBox = profileInfoArea.appendChild(document.createElement('div'));
	var newButtonArea = profileInfoArea.appendChild(document.createElement('div'));
	newButtonArea.style.float="right";
	
	var btnCallback = (function(btnID){
		if(btnID == "DELETE"){
			Confirm(Sepoa.cache.text.get("MENU_MANAGEMENT.MESSAGE_A0004"), dhtmlx.bind(function(flag){
				if(flag){
					this.sendTransaction(this.component.profileForm.getFormData(), "doProfileDELETE");
				}
			}, this))
		}else{
			this.openPopup(btnID);
		}
	}).bind(this);
	
	var btnObj = new SepoaButton();
	btnObj.addButton(newButtonArea ,{id:"MODIFY",text:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_MODIFY"),type:"", onclick:(function(){btnCallback("MODIFY")}).bind(this)});
	btnObj.addButton(newButtonArea ,{id:"SAVEAS",text:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_SAVEAS"),type:"save", onclick:(function(){btnCallback("SAVEAS")}).bind(this)});
	btnObj.addButton(newButtonArea ,{id:"DELETE",text:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_DELETE"),type:"delete", onclick:(function(){btnCallback("DELETE")}).bind(this)});
	btnObj.addButton(newButtonArea ,{id:"NEW",text:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_NEW"),type:"new", onclick:(function(){btnCallback("NEW")}).bind(this)});
	
	var screenNameBlock = [];
	for(var i=0 ; i < this.config.language.length ; i++){
		var l = this.config.language[i];
		if(screenNameBlock.length > 0){
			screenNameBlock.push({type: "newcolumn"});
		}
		screenNameBlock.push({type: "input", value: "", name: "contents_"+l.toLowerCase(), placeholder:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_PROFILE_NAME")+"("+l+")"})
	}
	var formJson = [
		{type:"settings", width:"auto", inputWidth:"150"},
		{type: "block", list: [
			{type: "select", value: "", label:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_COMPANY_CODE"), name: "company_code", disabled:true, userdata:{
				combo: "SL0013,{HOUSE_CODE}"
			}},
			{type: "newcolumn"},
			{type: "input", value: "", label:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_PROFILE_CODE"), name: "menu_profile_code", placeholder:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_PROFILE_CODE"), disabled:true},
			{type: "newcolumn"},
			{type: "select", label:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_PROFILE_NAME"), name:"menu_profile_names"},
			{type: "newcolumn"},
			{type: "block", hidden:true, list: screenNameBlock}
			
		]}
	];
	var formObj = this.component.profileForm = new SepoaDhtmlXFormObject(pofileFormBox, formJson);
	
	var profileNodeArea = this.base.appendChild(document.createElement('div'));
	profileNodeArea.classList.add("sepoa_menumanagement_tree");
	profileNodeArea.classList.add("lowerDiv");
	var tabbarObj = this.component.groupTabbar = new SepoaDhtmlXTabBar({
		mode:"top",
		align:"left",
		content_zone:true,
		arros_mode:true,
		parent:profileNodeArea
	})
	var that = this;
	var dragBase = $(tabbarObj.cont).find(".dhxtabbar_tabs_cont_left");
	if(!dragBase.sortable("instance")){
		dragBase.sortable({
			revert: true,
			delay:100,
			placeholder:"",
			items:".dhxtabbar_tab:not(:last-child)",
			placeholder:"dhxtabbar_tab sepoa_menumanagement_tabs_placeholder",
			cancel:".dhxtabbar_tab_close",
			axis: "x",
			start:function(event, ui){
				ui.placeholder.height(ui.item.height());
				ui.placeholder.width(ui.item.width());
				ui.placeholder[0]._source = {
					index:Array.prototype.indexOf.call(ui.placeholder[0].parentNode.childNodes, ui.placeholder[0]) - 1
				}
			},
			stop:function(event, ui){
				var sInfo =  ui.placeholder[0]._source;
				var dPE = ui.item[0].parentElement;
				if(dPE.children[sInfo.index] != ui.item[0]){
					that.updateGroupNodeIndex();
				}
			}
		});
	}
	dragBase.disableSelection();
	
	tabbarObj.parentObj = this;
	tabbarObj.attachEvent("onTabClose", dhtmlx.bind(function(id){
		Confirm(Sepoa.cache.text.get('MESSAGE.1015'), function(flag){ // 삭제 하시겠습니까?
			if(flag){
				this.group[id]._onclick({
					srcElement:{
						eventId:"delete"
					}
				});
				// 메뉴 관리 프로파일 탭바 메뉴 삭제
				tabbarObj.tabs(id).close();
			}
		}.bind(this))
	}, this))
	tabbarObj.attachEvent("onSelect", function(id, lastId){
		if("__ADD_GROUP__" == id){
			var newFlag = false;
			if(lastId){
				newFlag = true;
			}else{
				var ar = this.getAllTabs();
				if(ar.length < 2){
					newFlag = true;
				}else{
					this.tabs(ar[ar.length - 2]).setActive();
				}
			}
			
			if(newFlag){
				if(!this.parentObj.component.profileForm.getItemValue("menu_profile_code")){
					Alert(tabbarObj.parentObj._TextData("MESSAGE.A0003"));
				}else{
					var options = {
						menu_profile_code:tabbarObj.parentObj.component.profileForm.getItemValue("menu_profile_code"),
						company_code:tabbarObj.parentObj.component.profileForm.getItemValue("company_code"),
						menu_id:sUtils.ux.getDocumentNumber("MUID"),
						item_seq:this.getAllTabs().length+"",
						parent_menu_id:"0"
					}
					options["contents_"+tabbarObj.parentObj.config.language[0].toLowerCase()] = "";
					this.parentObj.addGroup(options, true);
					this.parentObj.group[options.menu_id]._onclick({
						srcElement:{
							eventId:"save"
						}
					})
				}
				return false;
			}
		}else{
			this.parentObj.group[id].callEvent("onActive", []);
		}
		return true;
	});
	
	this.component.window = new dhtmlXWindows('material');
	this.component.window.setViewport(0,0,0,0,SUX.getModuleTop().document.getElementById('container'));
	this.component.window.attachEvent("onShow", function(win){
		this.vp.style.display="";
		this.vp.style.height="100%";
		this.vp.style.width="100%";
		win.center();
	});
	this.component.window.attachEvent("onHide", function(win){
		this.vp.style.height="0";
		this.vp.style.width="0";
		this.vp.style.display="none";
		win.formObj.clear();
	});
	this.component.window.attachEvent("onClose", function(win){
		win.hide();
	});
}
SepoaMenuManagement.prototype.openPopup = function(id){
	var popup = this.component.window.window(id);
	if(!popup){
		var winObj = this.component.window;
		winObj.vp.style.display="";
		winObj.vp.style.height="100%";
		winObj.vp.style.width="100%";
		
		popup = winObj.createWindow(id, undefined,undefined,200,400);
		popup.setModal(true);
		popup.button("park").hide();
		popup.button("minmax").hide();
		popup.button("close").hide();
		popup.setText(Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_"+id));
		var popupArea = popup.attachDiv();
		popupArea.style.height="";
		popupArea.innerHTML = '<div style="display:flex;" class="sepoaux_component"><div style="margin:auto;"><div></div></div></div>';
		popupArea.appendChild($('<div class="sepoaux_component_inner_wrapper"><div class="sepoaux_component_outer_wrapper"><div class="sepoaux_component sepoaux_button sepoaux_wrapper_style_halign_center"></div></div></div>')[0]);

		var btnObj = new SepoaButton();
		var buttonArea = popupArea.children[1].children[0].children[0];
		btnObj.addButton(buttonArea, {id: "CONFIRM", text: Sepoa.cache.text.get("MENU_MANAGEMENT.BUTTON_CONFIRM"), type: "save", onclick:(function(){popupBtnCallback("CONFIRM", id)}).bind(this)});
		btnObj.addButton(buttonArea, {id: "CANCEL", text: Sepoa.cache.text.get("MENU_MANAGEMENT.BUTTON_CANCEL"), type: "back", onclick:(function(){popupBtnCallback("CANCEL", id)}).bind(this)});
		
		var popupBtnCallback = (function(btnId, popupId){
			if(btnId == 'CONFIRM') {
				var params = this.component.profileForm.getFormData();
				var inputData = this.component.window.getTopmostWindow().formObj.getFormData();
				for(var i in inputData){
					if(i == "company_code"){
						if(inputData[i] == null || inputData[i] == ''){
							Alert(Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_EMPTY_COMPANY"))
							return;
						}
						params.new_company_code = inputData[i];
					}else{
						params[i] = inputData[i];
					}
				}
				if(!this.component.window.getTopmostWindow().formObj.validate()){
					Confirm(Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_EMPTY_VALUE"), dhtmlx.bind(function(f){
						if(f){
							this.sendTransaction(params, "doProfile"+id);
						}
					}, this));
				}else{
					this.sendTransaction(params, "doProfile"+id);
				}
			} else if(btnId == 'CANCEL') {
				this.component.window.window(popupId).hide();
			}
		}).bind(this);
		
		var formItem = [];
		if(id != "MODIFY"){
			formItem.push({type: "select", width:"140", value: "", name: "company_code", required:true, userdata:{combo: "SL0013,{HOUSE_CODE}"}, label:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_COMPANY_CODE")});
		}
		for(var i=0 ; i < this.config.language.length ; i++){
			var l = this.config.language[i];
			formItem.push({type: "input", width:"140", value: "", name: "contents_"+l.toLowerCase(), required:true, label:Sepoa.cache.text.get("MENU_MANAGEMENT.PROFILE_PROFILE_NAME")+"("+l+")"})
		}
		var formJson = [{type: "block", blockOffset: "0", list: formItem}]
		var formObj = popup.formObj = new SepoaDhtmlXFormObject(popupArea.children[0].children[0].children[0], formJson);
		
		popup.setDimension(popup.cell.parentElement.offsetWidth, popupArea.offsetHeight + 65);
	}
	popup.center();
	popup.show();
	
	if(id == "MODIFY"){
		popup.formObj.load({data:this.component.profileForm.getFormData()})
	}
}
SepoaMenuManagement.prototype.popupButtonCallback = function(id){
	console.log(arguments);
}
SepoaMenuManagement.prototype.showProfileInfo = function(data){
	if(data){
		this.base.classList.add("sepoa_menumanagement_profile_show");
		var select = this.component.profileForm.getSelect("menu_profile_names");
		select.innerHTML = "";
		for(var i in data){
			if(i.indexOf("contents_") == 0){
				var l = i.substring(9).toUpperCase();
				select.add(new Option("("+l+") "+data[i], l));
			}
		}
		this.menu_profile_code = data.menu_profile_code;
	}else{
		this.base.classList.remove("sepoa_menumanagement_profile_show");
		this.component.profileForm.getSelect("menu_profile_names").innerHTML = "";
	}
}
SepoaMenuManagement.prototype.progress=function(flag){
	var t = (SUX.getModuleTop().Sepoa.request.isPopup)?SUX.getModuleTop().selfObj:SUX.root;
	var w = (SUX.getModuleTop().Sepoa.request.isPopup)?SUX.getModuleTop().parent.SUX.getModuleTop():SUX.getModuleTop();
	if(flag === true && t.progressOn){
		t.progressOn(w);
	}else if(flag === false && t.progressOff){
		t.progressOff(undefined, w);
	}
}
SepoaMenuManagement.prototype.loadProfile=function(d){
	this.postConnection(d, "doQueryMenuMgtData")
	this.component.profileForm.setFormData(d)
	this.showProfileInfo(d);
}
SepoaMenuManagement.prototype.postConnection=function(d, method){
	this.progress(true);
	var url = this.config.servlet;
	var param = [];
	param.push("mode=doList")
	param.push("METHOD="+method)
	param.push("PROCESS_ID="+this.config.PROCESS_ID)
	param.push(O2SP(d));
    param.push("requestMode=postConnection");
	var post = param.join("&") || "";
	
	if(url.indexOf("?") < 0){
		url += "?"
	}
	
	this.load(url+post);
}
SepoaMenuManagement.prototype.sendTransaction=function(d, method, grid){
	this.progress(true);
	var url = this.config.servlet;
	var param = []
	param.push("mode=doSave")
	param.push("METHOD="+method)
	param.push("PROCESS_ID="+this.config.PROCESS_ID)
	param.push(O2SP(d));
	if(grid){
		param.push("defaultGrid=vListData");
		param.push("gridObjNames=vListData");
		param.push("vListData="+encodeUrl(grid));
	}
	param.push("requestMode=postTransaction");
	var post = param.join("&") || "";
	
	var xmlLoader = dhtmlx.bind(function(g){
		var top = dhx4.ajax.xmltop("rows", g.xmlDoc);
		var atag = dhx4.ajax.xpath("/rows/userdata", top);
		var btag = {}
		for(var i=0;i<atag.length; i++){
			var a = atag[i];
			btag[a.getAttribute("name")] = (a.textContent || a.text || "").trim() 
		}
		this.progress(false);
		if(btag.status == "false"){
			Alert(btag.message);
		}else{
			if(method == "doProfileMODIFY"){
				for(var i in d){
					if(i.indexOf("contents_") == 0){
						this.component.profileForm.setItemValue(i, d[i]);
					}
				}
				this.component.window.getTopmostWindow().hide();
				this.callEvent("onChangeProfile", []);
			}else if(method == "doProfileSAVEAS"){
				d.company_code = d.new_company_code
				d.menu_profile_code=btag.PROFILE_CODE
				this.clearAll();
                this.loadProfile(d);
                
                this.component.window.getTopmostWindow().hide();
				this.callEvent("onChangeProfile", []);
			}else if(method == "doProfileDELETE"){
				this.clearAll();
				this.callEvent("onChangeProfile", []);
			}else if(method == "doProfileNEW"){
				this.clearAll();
				d.menu_profile_code = btag.menu_profile_code
				d.company_code = d.new_company_code
				this.loadProfile(d);
				
				this.component.window.getTopmostWindow().hide();
				this.callEvent("onChangeProfile", []);
			}
			sUtils.cache.text.clear();
		}
	}, this);
	dhx4.ajax.post(url, post, xmlLoader);
}
SepoaMenuManagement.prototype.updateGroupNodeIndex=function(){
	var fstr = "company_code"+ Sepoa.common.FIELD + "menu_id"+ Sepoa.common.FIELD + "item_seq"+ Sepoa.common.FIELD+ Sepoa.common.LINE
	
	var tabs = this.component.groupTabbar.getAllTabs();
	for(var i in tabs){
		if(tabs[i] != "__ADD_GROUP__"){
			var g = this.group[tabs[i]];
			var gd = (g._beforeData?g._beforeData.data:g.data);
			fstr += gd.company_code + Sepoa.common.FIELD +gd.menu_id + Sepoa.common.FIELD + (this.component.groupTabbar.tabs(tabs[i]).getIndex()+1) + Sepoa.common.FIELD+ Sepoa.common.LINE
		}
	}
	
	var data={
		menu_profile_code:this.menu_profile_code,
		menu_id:"0"
	}
	this.sendTransaction(data, "doUpdateTreeIndex", fstr);
}
SepoaMenuManagement.prototype.addGroup=function(data, flag){
	var id = data.menu_id;
	var g = this.component.groupTabbar;
	g.addTab(id, data["contents_"+this.config.language[0].toLowerCase()], null, g.getAllTabs().length-1, false, true);
	var options = {
		config:this.config,
		childBase:g.tabs(id).attachDiv()
	}
	$(options.childBase).disableSelection();
	options.childBase.classList.add("sepoa_menutabcell_base");
	
	this.group[id] = new SepoaMenuTreeNode(options, 0, data);
	this.group[id].attachEvent("onUpdateNode", dhtmlx.bind(function(data){
		this.component.groupTabbar.tabs(data.menu_id).setText(data["contents_"+Sepoa.info.LANGUAGE.toLowerCase()]);
	}, this))
	if(flag !== false){
		g.tabs(id).setActive();
	}
}
SepoaMenuManagement.prototype.clearAll=function(){
	this.component.groupTabbar.clearAll();
	this.component.groupTabbar.addTab("__ADD_GROUP__", "+", null, null, false, false);
	this.data.clearAll();
	this.component.profileForm.clear();
	this.showProfileInfo();
	this.group = {};
}
//MenuTreeNode
SepoaMenuTreeNode = function(pObj, lv, data){
	dhtmlx.extend(this, dhtmlx.Settings);
	dhtmlx.extend(this, dhtmlx.EventSystem);
	dhtmlx.extend(this, dhtmlx.DataLoader);

	this.items={};
	this.menuData={};
	
	this.config = pObj.config;
	this.renderBase(pObj.childBase);
	
	this.editableNode = new Set();
	this.attachEvent("onBeforeDrag", dhtmlx.bind(function(){
		if(this.editableNode.size > 0){
			Alert(Sepoa.cache.text.get("MENU_MANAGEMENT.MESSAGE_A0002"));
			return false;
		}
	}, this));
	this.attachEvent("onBeforeEdit", dhtmlx.bind(function(node){
		if(!node._dummy()){
			this.editableNode.add(node);
		}
	}, this));
	this.attachEvent("onBeforeEditEnd", dhtmlx.bind(function(node){
		if(!node._dummy()){
			this.editableNode.delete(node);
		}
	}, this));
	this.attachEvent("onChangeChildIndex", dhtmlx.bind(function(){
		this._doSaveIndex();
	}, this));
	
	this._beforeData = {
		data:data,
		event:this.attachEvent("onActive", dhtmlx.bind(function(){
			var data = this._beforeData.data
			if(data.menu_id){
				this._dummy(false);
				this.setData(data);
				this.setReadOnly(true);
				if(data.childs && data.childs.length > 0){
					for(var i in data.childs){
						this.addChildNode(data.childs[i])
					}
				}
				this.addChildNode({
					menu_profile_code:this.menuData.menu_profile_code,
					company_code:this.menuData.company_code,
					parent_menu_id:this.menuData.menu_id
				})
			}else{
				this._dummy(true);
				this.setData(data);
			}
			
			if(data.parent_menu_id == "0"){
				this.showChildBox(true);
			}
			this.detachEvent(this._beforeData.event);
//			this._beforeData = null;
//			delete this._beforeData
		}, this))
	};
}
SepoaMenuTreeNode.prototype.renderBase=function(vp){
	this.base = vp.appendChild(document.createElement('div'));
	this.base.classList.add("sepoa_menutreenode_base");
	this.base.onclick=dhtmlx.bind(this._onclick, this);
	this.base.ondblclick=dhtmlx.bind(this._ondbclick, this);
	this.base.treeNode = this;
	
	var it = this.base.appendChild(document.createElement('div'));
	it.classList.add("sepoa_menutreenode_button");
	it.classList.add("sepoa_button_itemtype");
	it.eventId = "itemtype";
	
	var iconBox = this.items.iconBox = this.base.appendChild(document.createElement('div'));
	iconBox.innerHTML = "<div></div><div><textarea placeholder='"+Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_MENU_ICON")+"'></textarea></div>";
	iconBox.classList.add("sepoa_menutreenode_icon")
	
	var infoBox = this.base.appendChild(document.createElement('div'));
	infoBox.classList.add("sepoa_menutreenode_info");
	this.items.formBox = infoBox.appendChild(document.createElement('div'));
	this.items.formObj = this._createFormObject();
	
	var buttons = {
		delete:0,
		cancel:0,
		save:0,
		builder:0
	};	
	for(var i in buttons){
		var button = this.base.appendChild(document.createElement('div'));
		button.classList.add("sepoa_menutreenode_button");
		button.classList.add("sepoa_button_"+i);
		button.eventId = i;
	}
}
SepoaMenuTreeNode.prototype._createFormObject=function(formBox){
	var formBox = this.items.formBox
	var screenNameBlock = [];
	for(var i=0 ; i < this.config.language.length ; i++){
		var l = this.config.language[i];
		if(screenNameBlock.length > 0){
			screenNameBlock.push({type: "newcolumn"});
		}
		screenNameBlock.push({type: "input", width:"140", value: "", name: "contents_"+l.toLowerCase(), placeholder:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_SCREEN_NAME")+"("+l+")"})
	}
	var formJson = [
		{type: "block", blockOffset: "0", list: [
			{type:"settings", position:"label-left"},
			{type: "block", blockOffset: "0",  list: [
				{type: "input", offsetLeft:"0", width:"140", value: "", name: "menu_id", placeholder:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_MENU_ID"), disabled:true},
				{type: "newcolumn"},
				{type: "autocomplete", width:"140", value: "", name: "screen_id", placeholder:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_SCREEN_ID"), userdata:{combo:"SLAD0211"}},
				{type: "newcolumn"},
				{type: "block", width:"auto", blockOffset: "0", list: screenNameBlock},
				{type: "newcolumn"},
				{type: "input", offsetLeft:"15", width:"140", value: "", name: "module", placeholder:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_MODULE"), readonly: true},
			]},
			{type: "block", blockOffset: "0", list: [
				{type: "input", offsetLeft:"0", width:"575", value: "", name: "url", placeholder:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_URL")},
				{type: "input", offsetLeft:"0", width:"575", value: "", name: "mobile_url", placeholder:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_MOBILE_URL")},
				{type: "newcolumn"},
				{type: "select", width:"140", value: "", name: "summary_flag", options:[
					{text:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_SUMMARY_FLAG_N"), value:"N"},
					{text:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_SUMMARY_FLAG_Y"), value:"Y"}
				]},
				{type: "newcolumn"},
				{type: "select", width:"140", value: "", name: "hidden_flag", options:[
					{text:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_HIDDEN_FLAG_N"), value:"N"},
					{text:Sepoa.cache.text.get("MENU_MANAGEMENT.DETAIL_HIDDEN_FLAG_Y"), value:"Y"}
				]}
			]}
		]},
	];
	var formObj = new SepoaDhtmlXFormObject(formBox, formJson);
	formObj.attachEvent("onAutoComplete", function(param, obj){
		obj.PROCESS_ID = "sepoa.svc.admin.AD_021";
		obj.METHOD = "getScreenComboList";
	});
	formObj.attachEvent("onChange", dhtmlx.bind(function(name, value, cd){
		if(name == "screen_id" && cd){
			var params = {
				screen_id:"MENU_%",
				code:cd.SCREEN_ID
			}
			doServiceAjax(this.config.PROCESS_ID, "CONNECTION", "doQuerySlangFromMenuID",params, dhtmlx.bind(function(so){
				var sf = new SepoaFormater(so.result[0]);
				for(var i in sf.columns){
					if(sf.columns[i])
						this.setItemValue(sf.columns[i].toLowerCase(),  sf.getValue(sf.columns[i], 0));
				}
			}, this))
			this.setItemValue("menu_img" , cd.MENU_IMG);
			this.setItemValue("hidden_flag" , cd.HIDDEN_FLAG);
			this.setItemValue("summary_flag", cd.SUMMARY_FLAG);
			this.setItemValue("url", cd.URL);
		}
	}, this));
	formObj.attachEvent("onFocus", dhtmlx.bind(function(name) {
		if(name == 'module' && !this.base.classList.contains("sepoa_menutreenode_readonly")) {
			var moduleSelectCallback = dhtmlx.bind(function(name, value) {
				this.setItemValue(name, value);
			}, this);
			PopupOpenPost(window.CONTEXT_NAME + '/Admin/ModuleConfiguration/AdminModuleConfigurationSelectPopup.sux', {target: name, selectedValue: this.items.formObj.getItemValue(name)}, moduleSelectCallback, 800, null, Sepoa.cache.text.get('MENU_MANAGEMENT.DETAIL_MODULE_SELECT'), 'SELECT_MODULE');
		}
	}, this));
	return formObj;
}
SepoaMenuTreeNode.prototype._onclick=function(e){
	var id = e.srcElement.eventId;
	var tsmode = id;
	switch(id){
	case "disabled":
		this.setItemValue("hidden_flag", "Y")
		tsmode = "save";
	case "save":
		if(this._beforeData){
			this.callEvent("onActive", []);
		}
		this.callEvent("onBeforeEditEnd", [this]);
		var data = $.extend({}, this.menuData, this.items.formObj.getFormData());
		data.menu_img = this.items.iconBox.children[1].children[0].value;
		this.sendTransaction(data, "doUpdateTreeNode");
		break;
	case "delete":
		if(this._beforeData){
			this.callEvent("onActive", []);
		}
		// this._beforeData.data.parent_menu_id 가 0이라면 부모가 프로파일을 의미
		// M으로 시작하는 id값이 존재한다면 tabbar의 자식 노드
		if(this._beforeData.data.parent_menu_id == "0"){
			this.callEvent("onBeforeEditEnd", [this]);
			
			var ml = [this.menuData.menu_id];
			this.getChildNodes(this.menuData.childs, ml);
			
			var params = $.extend(true, {menu_id_list:ml.join("____")}, this.menuData);
			this.sendTransaction(params, "doDeleteTreeNode");
		} else {
			// Menu Tabbar의 자식 노드의 Delete를 실행했을 경우
			Confirm(Sepoa.cache.text.get('MESSAGE.1015'), function(flag){ // 삭제 하시곘습니까?
				if(flag){
					this.callEvent("onBeforeEditEnd", [this]);
					
					var ml = [this.menuData.menu_id];
					this.getChildNodes(this.menuData.childs, ml);
					
					var params = $.extend(true, {menu_id_list:ml.join("____")}, this.menuData);
					this.sendTransaction(params, "doDeleteTreeNode");
				}
			}.bind(this))
		}
		
		
		
		
		break;
	case "itemtype":
		if(this.childBase) {
			if(this.childBase.children.length < 1 || this.base.classList.contains("sepoa_menutreenode_show_child")){
				this.showChildBox(false);
			}else{
				this.showChildBox(true);
			}
		}
		break;
	case "cancel":
		this.callEvent("onBeforeEditEnd", [this]);
		this.setData(this.menuData);
		if(!this._dummy()){
			this.setReadOnly(true);
		}
		break;
	case "builder":
		var url = this.menuData.url;
		PopupOpenPostReal(window.CONTEXT_NAME+'/Tools/ScreenBuilder/ToolsScreenBuilder.sux', window.screen.width, window.screen.height, url, {
			page:url
		})
		break;
	}
	if(e.stopPropagation)
		e.stopPropagation();
}
SepoaMenuTreeNode.prototype._doSaveIndex = function(){
	var fstr = "company_code"+ Sepoa.common.FIELD + "menu_id"+ Sepoa.common.FIELD + "item_seq"+ Sepoa.common.FIELD+ Sepoa.common.LINE
	var children = this.getChildNode();
	for(var i in children){
		fstr += children[i].menuData.company_code + Sepoa.common.FIELD + children[i].menuData.menu_id + Sepoa.common.FIELD + (parseInt(i)+1) + Sepoa.common.FIELD+ Sepoa.common.LINE
	}
	
	var data = $.extend(true, {}, this.menuData);
	this.sendTransaction(data, "doUpdateTreeIndex", fstr);
}
SepoaMenuTreeNode.prototype._ondbclick=function(e){
	if(!e.srcElement.classList.contains("sepoa_menutreenode_button")){
		if(this.readonly == true){
			this.setReadOnly(false);
			if(this.childBase.children.length > 1){
				this.showChildBox(true);
			}
			this.items.formObj.setItemFocus('url');
			this.callEvent("onBeforeEdit", [this]);
		}
	}
	e.stopPropagation();
}
SepoaMenuTreeNode.prototype.getLevel=function(){
	if(this.menuData.parent_menu_id != "0"){
		return $(this.base).parents('.sepoa_menutreenode_base')[0].treeNode.getLevel() + 1;
	}
	return 0;
}
SepoaMenuTreeNode.prototype._dummy=function(flag){
	if(flag===true){
		this.base.classList.add("sepoa_menutreenode_dummy");
	}else if(flag === false){
		this.base.classList.remove("sepoa_menutreenode_dummy");
	}else{
		return this.isDummy;
	}
	this.isDummy = flag;
}
SepoaMenuTreeNode.prototype.addChildNode=function(data){
	if(!this.childs){
		this.childs = [];
	}
	if(!this.childBase){
		var childArea = this.base.appendChild(document.createElement('div'));
		childArea.classList.add("sepoa_menutreenode_child_area");
		
		this.childBase = childArea.appendChild(document.createElement('div'));
		this.childBase.classList.add("sepoa_menutreenode_child_base");
		
		var dragBase = $(this.childBase);
		if(!dragBase.sortable("instance")){
			dragBase.sortable({
				revert: true,
				delay:100,
				placeholder:"sepoa_menutreenode_base sepoa_menutreenode_placeholder",
				items:".sepoa_menutreenode_base:not(.sepoa_menutreenode_base.sepoa_menutreenode_dummy)",
				cancel:"input, select, textarea, .sepoa_menutreenode_button",
				connectWith:".sepoa_menutreenode_child_area > .sepoa_menutreenode_child_base, .sepoa_menutabcell_tree",
				start:function(event, ui){
					ui.placeholder.height(ui.item.height());
					ui.placeholder[0]._source = {
						parent:ui.placeholder[0].parentElement,
						index:Array.prototype.indexOf.call(ui.placeholder[0].parentNode.childNodes, ui.placeholder[0]) - 1
					}
				},
				stop:function(event, ui){
					var sInfo =  ui.placeholder[0]._source;
					var dPE = ui.item[0].parentElement;
					var mode = -1;
					if(dPE != sInfo.parent){
						mode = 1;
					}else if(dPE.children[sInfo.index] != ui.item[0]){
						mode = 2;
					}
					switch(mode){
						case 1:
							$(sInfo.parent).parents('.sepoa_menutreenode_base')[0].treeNode.callEvent("onChangeChildIndex", []);
						case 2:
							$(dPE).parents('.sepoa_menutreenode_base')[0].treeNode.callEvent("onChangeChildIndex", []);
						default:
							break;
					}
				},
				change:function(event, ui){
					var destListElement = $(ui.placeholder[0].parentElement)
					if(!destListElement.children().last().hasClass("sepoa_menutreenode_dummy")){
						destListElement.children(".sepoa_menutreenode_dummy").appendTo(destListElement);
					}
				}
			});
			var sortInstance = dragBase.sortable("instance");
			sortInstance._mouseStart_origin = sortInstance._mouseStart 
			sortInstance._mouseStart = function (event, overrideHandle, noActivation) {
				var treeNode = this._uiHash().item[0].treeNode;
				if(this._uiHash().item[0].treeNode.callEvent("onBeforeDrag", [])){
					this._mouseStart_origin.apply(this, [event, overrideHandle, noActivation]);
				}else{
					return false;
				}
			};
		}
		dragBase.disableSelection();
	}
	if(data){
		data.item_seq = this.childBase.children.length + 1
	}else{
		data = {
			item_seq:this.childBase.children.length + 1	
		};
	}
	var node = new SepoaMenuTreeNode(this, this.getLevel() + 1, data);
	
	node.attachEvent("onCreateEnd", dhtmlx.bind(function(){
		return this.addChildNode({
			menu_profile_code:this.menuData.menu_profile_code,
			company_code:this.menuData.company_code,
			parent_menu_id:this.menuData.menu_id
		});
	}, this));
	node.attachEvent("onBeforeEdit", dhtmlx.bind(function(node){
		return this.callEvent("onBeforeEdit", [node])
	}, this));
	node.attachEvent("onBeforeEditEnd", dhtmlx.bind(function(node){
		return this.callEvent("onBeforeEditEnd", [node])
	}, this));
	node.attachEvent("onBeforeDrag", dhtmlx.bind(function(){
		return this.callEvent("onBeforeDrag", [])
	}, this));
	node.attachEvent("onDeleteNode", dhtmlx.bind(function(node){
		this.childBase.removeChild(node.base);
	}, this));
	this.childs.push(node);
}
SepoaMenuTreeNode.prototype.sendTransaction=function(d, method, grid){
	this.config.baseObj.progress(true);
	var url = this.config.servlet;
	var param = []
	param.push("mode=doSave")
	param.push("METHOD="+method)
	param.push("PROCESS_ID="+this.config.PROCESS_ID)
	param.push(O2SP(d));
	if(grid){
		param.push("defaultGrid=vListData");
		param.push("gridObjNames=vListData");
		param.push("vListData="+encodeUrl(grid));
	}
	param.push("requestMode=postTransaction");
	var post = param.join("&") || "";
	
	var xmlLoader = dhtmlx.bind(function(g){
		try{
			var top = dhx4.ajax.xmltop("rows", g.xmlDoc);
			var atag = dhx4.ajax.xpath("/rows/userdata", top);
			var btag = {}
			for(var i=0;i<atag.length; i++){
				var a = atag[i];
				btag[a.getAttribute("name")] = (a.textContent || a.text || "").trim() 
			}
			if(method == "doUpdateTreeNode"){
				if(btag.status == "true"){
					this.setData(d);
					this.setReadOnly(true);
					if(this._dummy()){
						this._dummy(false);
						this.setItemValue("menu_id", btag.menu_id);
						this.addChildNode();
						this.callEvent("onCreateEnd", []);
					}else if(this.menuData.parent_menu_id == "0"){
						this.callEvent("onUpdateNode", [this.menuData]);
					}
				}else{
					Alert(btag.message);
				}
			}else if(method == "doDeleteTreeNode"){
				this.callEvent("onDeleteNode", [this]);
				this.callEvent("onStoreUpdated", []);
			}
		}catch(e){
			console.log(e)
		}finally{
			sUtils.cache.text.clear();
			this.config.baseObj.progress(false);
		}
	}, this);
	dhx4.ajax.post(url, post, xmlLoader);
}
SepoaMenuTreeNode.prototype.setReadOnly=function(flag){
	this.readonly=flag;
	if(flag){
		this.items.formObj.setViewMode(true)
		this.base.classList.add("sepoa_menutreenode_readonly");
	}else{
		this.items.formObj.setViewMode(false)
		this.base.classList.remove("sepoa_menutreenode_readonly");
	}
	this.items.formObj.disableItem("menu_id");
	this.items.formObj.setReadonly("module", true);
}
SepoaMenuTreeNode.prototype.setData = function(data){
	for(var i in data){
		this.setItemValue(i, data[i]);
	}
	if(data.url){
		this.base.classList.add("sepoa_menutreenode_builder_on");
	}else{
		this.base.classList.remove("sepoa_menutreenode_builder_on");
	}
}
SepoaMenuTreeNode.prototype.getChildNode=function(all){
	var c = [];
	var childElements = $(this.childBase)[all===true?"find":"children"](".sepoa_menutreenode_base");
	for(var i=0;i<childElements.length;i++){
		var t = childElements[i].treeNode
		if(!t._dummy()){
			c.push(t);
		}
	}
	return c;
}
SepoaMenuTreeNode.prototype.getChildNodes=function(nodes, arr){
	
	for(var i in nodes) {
		var node = nodes[i];
		
		arr.push(node.menu_id);
		if(node.childs && node.childs.length != 0) {
			this.getChildNodes(node.childs, arr);
		}
	}
	
	return arr;
}
SepoaMenuTreeNode.prototype.setItemValue=function(id, value){
	if(this.items.formObj.isItem(id)){
		this.items.formObj.setItemValue(id, value);
	}else if("menu_img" == id){
		this.items.iconBox.children[1].children[0].value=value
		this.items.iconBox.children[0].innerHTML = "";
		var extIdx = (value||"").lastIndexOf(".");
		if(extIdx + 1){
			var ext = value.substring(extIdx + 1).toLowerCase();
			if("svg" == ext){
				var d = null;
				$.ajax({
					url: window.CONTEXT_NAME+'/resource/design/imgs/menu/'+value,
					type:"post",
					dataType: "xml",
					success: function(data){
						d = data;
					},
					async: false
				});
				if(d){
					var svg = d.getElementsByTagName("svg")[0]
					if(!!svg.classList){
						svg.classList.add("sepoa_menutreenode_svg");
					}else{
						var classList = (svg.getAttribute("class") || "").split(" ")
						if(!(classList.indexOf("sepoa_menutreenode_svg") + 1)){
							classList.push("sepoa_menutreenode_svg");
						}
						svg.setAttribute("class", classList.join(" "));
					}
					this.items.iconBox.children[0].appendChild(svg);
				}
				return;
			}else if(["jpg", "gif", "png"].indexOf(ext)+1){
				this.items.iconBox.children[0].innerHTML = '<div class="sepoa_menutreenode_svg" style="background:url('+window.CONTEXT_NAME+'/resource/design/imgs/menu/'+value+' ) no-repeat center center / cover;"></div>'
				return;
			}
		}
		this.items.iconBox.children[0].innerHTML = '<svg class="sepoa_menutreenode_svg" viewBox="0 0 46 46">'
												 + '	<path d="'+value+'"/>'
												 + '</svg>';
	}
	this.menuData[id] = value;
}
SepoaMenuTreeNode.prototype.showChildBox=function(f){
	for(var i=0;i<(this.childs.length || 0);i++){
		this.childs[i].callEvent("onActive", []);
	}
	this.base.classList[(f)?"add":"remove"]("sepoa_menutreenode_show_child");
}

SepoaMenuTreeNode.prototype._TextData=function(){
	return this.config.baseObj._TextData.apply(this.config.baseObj, Array.prototype.slice.call(arguments));
}