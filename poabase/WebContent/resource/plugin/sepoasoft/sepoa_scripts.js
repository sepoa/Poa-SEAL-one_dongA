if(typeof(SUX) == 'undefined' || SUX == null){
	SUX = {
		getChildList:function(){
			var r = [];
			for(var _idx in SUX){
				if((SUX[_idx] instanceof Window) && (_idx != "root")){
					r.push(SUX[_idx]);
				}
			}
			return r;
		},
		pools:{},
		getModuleTop:function(flag){
			if(SUX.root == parent || (Sepoa.request.isPopup && !flag)){
				return window;
			}else{
				try{
					return parent.SUX.getModuleTop();
				}catch(e){
					return window;
				}
			}
		}
	};
	SUX.callEvent = sUtils.ux.callEvent;
	SUX.attachEvent = sUtils.ux.attachEvent;
	SUX.detachEvent = sUtils.ux.detachEvent;
	
	if(parent == this){
		SUX.root = this;
	}else{
		try{
			SUX.root = parent.SUX.root;
			if((parent != SUX.root && !request.isPopup) || (parent == SUX.root && SUX.root.request.isPopup)){
				SUX.pools = parent.SUX.pools;
			}
		}catch(e){
			SUX.root = window;
		}
	}
}

/**
 * Sepoa 공통 객체
 */
function SepoaCommonObject(){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	if(SUX.root == window){
		this.callSepoaJSCommon((data) => {
			this.commonKey = [];
			for(var _idx in data){
				this[_idx] = data[_idx];
				this.commonKey.push(_idx);
			}
			if(this.config){
				this.config.get=function(s, d){
					return (this[s] === void 0)?(d || ""):this[s];
				}
			}
		});
	}else{
		var keys = SUX.root.Sepoa.commonKey;
		for(var i in keys){
			this[keys[i]] = $.extend({}, SUX.root.Sepoa[keys[i]]);
		}
		delete this.common.SCREEN_ID;
		if(this.config){
			this.config.get=function(s, d){
				return (this[s] === void 0)?(d || ""):this[s];
			}
		}
	}
	
	this.cache = sUtils.cache;
	this.button={};
	this.properties = {
		host:document.location.host
	};
	this.request={};
	this.window={};
	this.attachEvent('onLoad',function(){
		SUX.callEvent('onWindowResize',[]);
	});
};
SepoaCommonObject.prototype.callSepoaJSCommon=function(fn) {
	$.ajax({
		url: window.CONTEXT_NAME+"/SepoaJSCommon.sux",
		type:"post",
		data:"mode=common",
		dataType: "json",
		success: fn,
		error:function(){
			console.log(arguments);
		},
		async: false
	});
}
SepoaCommonObject.prototype.setScreenId=function(sid){
	this.common.SCREEN_ID = sid;
	this.attachEvent('onLoadEnd',function(id){
		SUX.getModuleTop().SUX.callEvent('alreadyFunctionCall',[id]);
	})
}
SepoaCommonObject.prototype.ChangeNomalString=sUtils.string.decodeHTML
SepoaCommonObject.prototype.getToday=sUtils.date.getToday

SepoaCommonCopyObject = function(s){
	var tmpBase = s.baseWindow;
	delete s.baseWindow;
	var copyObj = $.extend(true, {}, s);
	s.baseWindow = tmpBase;
	return copyObj;
}

if(typeof(Sepoa) == "undefined"){
	Sepoa = new SepoaCommonObject();
	Sepoa.attachEvent('onLoad',function(){
		SUX.callEvent('onWindowResize',[]);
	});
}

function SepoaDataOutput(sId){
	if(sId){
		this.document = $(sId[0]);
		this.document = (this.document.length > 0)?this.document[0] : document;
	}else{
		this.document = document.body;
	}
	
	this.encoding = true;
	this.FIELD = Sepoa.common.FIELD;
	this.target=["input","select","textarea"];
	this.otherScreenData = [];
	this.KEY_PARAMS = Sepoa.common.KEY_PARAMS;
	this.filterObj = null;
	this.categoriObj = null;
	this.gridData = {};
}
SepoaDataOutput.prototype.getDocument=function(){
	if(this.document)
		return this.document;
	else 
		return document.body;
}
SepoaDataOutput.prototype.setEncoding = function(flag){
	this.encoding = flag;
}
SepoaDataOutput.prototype.createParamString = function(){
	var params = [];

	var target = this.getDocument();
	
	function process(obj1, obj2, obj3){
		for (var i = 0; i < obj1.length; i++) {
			if(this.document && this.document.id != getScreenId(obj1[i])){
				continue;
			}
			if(obj1[i].classList.contains("dhxcombo_input")){
				var o = obj1[i].nextElementSibling;
				params.push(o.name);
				params.push(o.value);
			}else if(obj1[i].id != "" && obj1[i].className.indexOf('dhxform_') < 0) {
				if(obj1[i].type == "radio") {
					if(obj1[i].checked) {
						params.push(obj1[i].name);
			    		params.push(obj1[i].value);
			    	}
			    } else if(obj1[i].type == "checkbox") {
			    	params.push(obj1[i].id);
		    		params.push(obj1[i].checked);
			    } else{
			    	params.push(obj1[i].id);
		    		params.push(obj1[i].value);
			    }
			}else if(obj1[i].className.indexOf('dhxform_')>= 0){
				var temp = obj1[i].name;
				var temp_type = obj1[i].type;
				if(obj1[i].type == "radio") {
					if(obj1[i].checked) {
						params.push(obj1[i].name);
			   			params.push(obj1[i].value);
			   		}
			   	} else if(obj1[i].type == "checkbox") {
			   		params.push(obj1[i].name);
		    		params.push(obj1[i].checked);
			   	} else{
			   		if(obj1[i].className.indexOf('calendarYM')>=0){
			   			var c = obj1[i].parentNode.parentNode;
			   			var e = c._tempValue || c._c.getDate();
			   			var value =""
			   	        if (e != null) {
			   	        	value = (c._c._dateToStr(e, c._f0 || "%Y%m"));
			   	        }
			   	        params.push(obj1[i].name);
			   			params.push(value);
			   		}else if(obj1[i].className.indexOf('calendar')>=0){
			   			var c = obj1[i].parentNode.parentNode;
			   			var e = c._tempValue || c._c.getDate();
			   			var value =""
			   	        if (e != null) {
			   	        	value = (c._c._dateToStr(e, c._f0 || "%Y%m%d"));
			   	        }
			   	        params.push(obj1[i].name);
			   			params.push(value);
			   		}else{
			    		params.push(obj1[i].name);
			    		var v = "";
		    			if(obj1[i].parentNode.className.indexOf('dhxform_img_node') >= 0 
		    					|| obj1[i].parentNode.className.indexOf('dhxform_image_form') >= 0){
			    			v = obj1[i].value;
			    		}else{
			    			v = obj1[i].parentNode.parentNode._value
			    		}
			   			params.push(v);
			   		}
			   	}
			}
		}
		for (var i = 0; i < obj2.length; i++) {
			if(this.document && this.document.id != getScreenId(obj2[i])){
				continue;
			}
			if(obj2[i].id != "" && obj2[i].className.indexOf('dhxform_') < 0) {
				params.push(obj2[i].id);
				params.push(obj2[i].value);
			}else if(obj2[i].className.indexOf('dhxform_')>= 0){
				params.push(obj2[i].name);
				params.push(obj2[i].value);
			}
		}
		for (var i = 0; i < obj3.length; i++) {
			if(this.document && this.document.id != getScreenId(obj3[i])){
				continue;
			}
			if(obj3[i].id != "" && obj3[i].className.indexOf('dhxform_') < 0) {
				params.push(obj3[i].id);
				params.push(obj3[i].value.replaceAll("\r\n","\n"));
			}else if(obj3[i].className.indexOf('dhxform_')>= 0){
				params.push(obj3[i].name);
				params.push(obj3[i].value.replaceAll("\r\n","\n"));
		 	}
		}
		    
		var radioGroup = {};
		var radioElementNonSelect = this.getDocument().getElementsByClassName('rdbt0');
		for(var i=0 ; i<radioElementNonSelect.length;i++ ){
			radioGroup[radioElementNonSelect[i].parentNode.parentNode._group] = '';
		}
		
		var radioElementSelect = this.getDocument().getElementsByClassName('rdbt1');
		for(var i=0 ; i<radioElementSelect.length;i++ ){
			radioGroup[radioElementSelect[i].parentNode.parentNode._group] = radioElementSelect[i].parentNode.parentNode._value;
		}
		
		for(var i in radioGroup){
			params.push(i);
			params.push(radioGroup[i]);
		}
		
		if(this.filterObj){
			var filterData = this.filterObj.getFilterData();
			for(var idx in filterData){
				params.push(idx);
				params.push(filterData[idx]);
			}
			this.filterObj = null;
		}
		if(this.categoriObj){
			var categori = this.categoriObj.getData();
			for(var idx in categori){
				params.push(idx);
				params.push(categori[idx]);
			}
			this.categoriObj = null;
		}
	}
	
	if(this.formObjectList && this.formObjectList.length){
		for(var _idx in this.formObjectList){
			target = this.formObjectList[_idx]._parent;
			var domList = [];
			domList.push(target.getElementsByTagName("input"));
			domList.push(target.getElementsByTagName("select"));
			domList.push(target.getElementsByTagName("textarea"));
			process.apply(this, domList)
		}
		
		this.formObjectList = [];
	}else{
		var domList = [];
		domList.push(target.getElementsByTagName("input"));
		domList.push(target.getElementsByTagName("select"));
		domList.push(target.getElementsByTagName("textarea"));
		process.apply(this, domList)
		
		if(this.disposableList){
			for(var i=0;i<this.disposableList.length;i++ ){
				this.remove(this.disposableList[i]);
			}
			this.disposableList = [];
		}
	}
	
	return params.join(this.FIELD);
}
SepoaDataOutput.prototype._getOtherScreenParams = function(sid){
	return SUX.pools[sid].dataOutput.createParamString();
}
SepoaDataOutput.prototype.addScreen = function(sid){
	sid = (sid instanceof Array)? sid : [sid];
	for(var _idx in sid){
		if("all" == _idx.toLowerCase()){
			var list = SUX.getChildList();
			for(var l in list){
				this.otherScreenData.push(this._getOtherScreenParams(list[l]));
			}
		}else{
			this.otherScreenData.push(this._getOtherScreenParams(sid[_idx]));
		}
	}
}
SepoaDataOutput.prototype.addFilter = function(obj){
	this.filterObj = obj;
}
SepoaDataOutput.prototype.addCategori = function(obj){
	this.categoriObj = obj;
}
SepoaDataOutput.prototype.getParams = function(flag){
	this.otherScreenData.push(this.createParamString());
	returnText = this.otherScreenData.join(this.FIELD)+this.FIELD
	
	if( this.encoding ) {
		returnText = encodeURIComponent(returnText);
    }
	
	this.otherScreenData = [];
	return ((flag === false)?"":("&"+this.KEY_PARAMS + "="))+returnText;
}
SepoaDataOutput.prototype.getInputDom = function(){
	var doc = this.getDocument();
	var s = $(doc).children('#input_data');
	if(s.length <= 0){
		s = document.createElement('div');
		s.id = 'input_data'
		s.style.display="none";
		doc.appendChild(s);
	}else{
		s = s[0];
	}
	return $(s);
}
SepoaDataOutput.prototype.setValue = function(id, data, disposable){
	var t = this.getInputDom();
	data = (data === undefined)?"":data;
	if(t.find('#'+id).length < 1){
		var _input = document.createElement('input');
		_input.type="hidden";
		_input.value=data;
		_input.id=id;
		_input.name=name;
		t.append(_input);
	}else{
		t.find('#'+id).val(data);
	}
	if(disposable){
		if(!this.disposableList) this.disposableList = [];
		this.disposableList.push(id);
	}
}
SepoaDataOutput.prototype.setObject = function(obj, disposable){
	for(var i in obj){
		this.setValue(i, obj[i], disposable);
	}
}
SepoaDataOutput.prototype.setTextAreaValue = function(id, data, disposable){
	var t = this.getInputDom();
	data = (data === undefined)?"":data;
	if(t.find('#'+id).length < 1){
		var _input = document.createElement('textarea');
		_input.type="hidden";
		_input.value=data;
		_input.id=id;
		_input.name=name;
		t.append(_input);
	}else{
		t.find('#'+id).val(data);
	}
	if(disposable){
		if(!this.disposableList) this.disposableList = [];
		this.disposableList.push(id);
	}
}
SepoaDataOutput.prototype.getValue = function(id){
	var data = this.getInputDom().find('#'+id).val();
	return (data==undefined)?"":data;
}
SepoaDataOutput.prototype.remove = function(id){
	this.getInputDom().find('#'+id).remove();
}
SepoaDataOutput.prototype.addForm = function(obj){
	if(!this.formObjectList){
		this.formObjectList = [];
	}
	this.formObjectList.push(obj);
}
SepoaDataOutput.prototype.clear = function(){
	var t = this.getInputDom();
	t.innerHTML = "";
}
SepoaDataOutput.prototype.addGridData=function(id, data, isDefault){
	var COL_DEL = Sepoa.common.FIELD;
	var ROW_DEL = Sepoa.common.LINE;

	function process(objData) {
		var _row = [];
		var col_ids = [];
		
		if(!objData) {
			objData = [];
		}
		
		if(!Array.isArray(objData)) {
			objData = [].slice.call(objData);
		}
		
		if(objData.length > 0) {
			col_ids = col_ids.slice.call(Object.keys(objData[0]));
			_row.push(col_ids.join(COL_DEL));
		}
		
		for(var _rkey in objData) {
			var _cdata = [];
			for(var _ckey in col_ids) {
				_cdata.push((objData[_rkey][col_ids[_ckey]] !== null) ? objData[_rkey][col_ids[_ckey]] : "");
			}
			_row.push(_cdata.join(COL_DEL));
		}
		
		var rv = _row.join(COL_DEL + ROW_DEL) + COL_DEL + ROW_DEL;
		return rv;
	}
	
	var gd = {};
	gd.defaultGrid = (isDefault !== false) ? id : undefined;
	gd.gridObjNames = (this.gridData.gridObjNames) ? [id].concat(this.gridData.gridObjNames).join(',') : id;
	gd[id] = process(data);
	
	this.gridData = $.extend(true, this.gridData, gd);
}
SepoaDataOutput.prototype.getGridData=function(){
	var rv = this.gridData;
	this.gridData = {};
	return rv;
}
SepoaDataOutput.prototype.hasGridData=function(){
	return (Object.keys(this.gridData).length > 0 && this.gridData.constructor === Object);
}

if(typeof(isAutoGeneratePage) == "undefined" || !isAutoGeneratePage){
	dataOutput = new SepoaDataOutput();
}

function callFunction(targetQ, f, params){
	targetQ = (typeof(targetQ) == 'string')?[targetQ]:targetQ;
	var SUX = this.SUX;
	if(typeof(f) == 'string'){
		f = [f];
	}
	
	function getParentScreenId(obj){
		if(!obj){
			return null;
		}else if(obj instanceof Element || obj instanceof Window || obj instanceof Document){
			obj = $(obj)
		}else if(obj.length && obj.length > 0){
			obj = $(obj[0]);
		}else{
			return null;
		}
		
		var parents = obj.parents('div.sepoaux.screen_area');
		if(parents.length > 0){
			return parents[0].relaxId;
		}else if(parent.window == SUX.root && !SUX.root.request.isPopup){
			return 'ROOT';
		}else{
			var pfs = $(parent.window.document).find('iframe');
			for(var i=0;i<pfs.length;i++){
				if(pfs[i].contentWindow == window){
					var ps = $(pfs[i]).parents('div.sepoaux.screen_area');
					if(ps.length > 0){
						var result = [];
						for(var i=ps.length-1;i>=0;i--){
							result.push(ps[i].id);
						}
						return result;
					}else{
						return pfs[i].contentWindow.Sepoa.common.SCREEN_ID;  
					}
				} 
			}
		}
	}
	
	var target = targetQ.shift();
	var pFlag = false
	if(target != null && (['P','PARENT'].indexOf(target.toUpperCase()) >= 0)){
		target = getParentScreenId(this._vp || document);
		if(Array.isArray(target)){
			targetQ = target.concat(targetQ);
			target = targetQ.shift();
		}
	}
	
	if(SUX.pools[target]){
		try{
			if(SUX.pools[target].readyStatus && SUX.pools[target].Sepoa.baseWindow && SUX.pools[target].Sepoa.baseWindow.closed){
				delete SUX.pools[target];
			}
		}catch(e){
			delete SUX.pools[target];
		}
	}
	
	if(['MAIN','ROOT','MAINFRAME'].indexOf(target.toUpperCase()) >= 0){
		var t = SUX.root
		var scope = null;
		var logObject = [];
		for(var i in f){
			scope = t;
			t = t[f[i]];
			logObject.push({name:f[i], object:t});
		}
		
		if(!t){
			t = SUX.root.SUX.pools['MAINFRAME'];
			var scope = null;
			logObject.push({name:"new", object:t});
			for(var i in f){
				scope = t;
				t = t[f[i]];
				logObject.push({name:f[i], object:t});
			}
		}
		
		try{
			return t.apply(scope, params);
		}catch(e){
			console.log(logObject);
			console.log(e);
			logObject = null;
		}
	}else if(SUX.pools[target] && SUX.pools[target].readyStatus){
		try{
			var t = SUX.pools[target];
			if(targetQ.length > 0){
				return t.callFunction.apply(t, [targetQ, f, params]);
			}else{
				var scope = null;
				var logObject = [];
				for(var i in f){
					scope = t;
					t = t[f[i]];
					logObject.push(f[i]);
				}
				try{
					return t.apply(scope, params);
				}catch(e){
					console.log(target + '.'+logObject.join('.')+' apply error')
					console.log(e);
					logObject = null;
				}
			}
		}catch(e){
			console.log(e);
			console.log(target + ' in '+ f +' is undefined')
		}
	}else if(SUX.parentPools && SUX.parentPools[target] && SUX.parentPools[target].readyStatus){
		try{
			var t = SUX.parentPools[target];
			if(targetQ.length > 0){
				return t.callFunction.apply(t, [targetQ, f, params]);
			}else{
				var scope = null;
				var logObject = [];
				for(var i in f){
					scope = t;
					t = t[f[i]];
					logObject.push({name:f[i], object:t});
				}
				try{
					return t.apply(scope, params);
				}catch(e){
					console.log(logObject);
					console.log(e);
					logObject = null;
				}
			}
		}catch(e){
			console.log(e);
			console.log(target + ' in '+ f +' is undefined')
		}
	}else if(SUX.root != this && (!SUX.pools[target] || !SUX.pools[target].readyStatus)){
		var tmpObj = {};
		tmpObj.eventNum = SUX.getModuleTop().SUX.attachEvent("alreadyFunctionCall",function(s){
			if(s == target){
				try{
					var t = SUX.pools[s];
					if(targetQ.length > 0){
						t.callFunction.apply(t, [targetQ, f, params]);
					}else{
						var scope = null;
						var logObject = [];
						for(var i in f){
							scope = t;
							t = t[f[i]];
							logObject.push({name:f[i], object:t});
						}
						try{
							return t.apply(scope, params);
						}catch(e){
							console.log(logObject);
							console.log(e);
							logObject = null;
						}
					}
				}catch(e){
					console.log(e);
					console.log(s + ' in '+ f +' is undefined')
				}finally{
					SUX.getModuleTop().SUX.detachEvent(this.eventNum);
				}
			}
		}.bind(tmpObj));
	}
}

function getChildScreenId(obj, flag){
	var f = $(obj).find('div.sepoaux.screen_area');
	if(f.length > 0){
		return flag?f[0].id:f[0].relaxId;
	}else{
		return void 0;
	}
}

function getScreenId(obj, relax){
	var f = $(obj).parents('div.sepoaux.screen_area');
	if(f.length > 0){
		return relax?f[0].relaxId:f[0].id;
	}else{
		return void 0;
	}
}

function getOAObject(s_list, o){
	s_list = (typeof(s_list) == 'string')?[s_list]:s_list;
	var t = this;
	for(var _idx in s_list){
		var s = s_list[_idx];
		var aliasFlag = false;
		if(['P', 'PARENT'].indexOf(s.toUpperCase()) + 1){
			s = getScreenId(t._vp, true);
			aliasFlag = this.alias != this.Sepoa.common.SCREEN_ID;
		}
		t = t.SUX[aliasFlag?"parentPools":"pools"][s];
		if(!t){
			return void 0;
		}
	}
	
	if(!o) return t;
	o = o.split('.');
	
	for(var _idx in o){
		t = t[o[_idx]]
		if(t === void 0){
			break;
		}
	}
	return t;
}

function extendO2W(obj){
	var tmp={
		callFunction:window.callFunction
	}
	$.extend(window, obj);
	$.extend(window, tmp);
}

function pageDefaultSettings(){
	if(Sepoa.request.isPopup && !Sepoa.request.isRealPopup){
		var info = SUX.root.modalPopupInfo[Sepoa.request.parameterId];
		Sepoa.params = info.params;
		window.selfObj = info.selfObj;
		SUX.root.modalPopupInfo[Sepoa.request.windowParameterId] = null;
		delete SUX.root.modalPopupInfo[Sepoa.request.windowParameterId];
	}else if(Sepoa.request.isRealPopup){
		var o = opener || parent;
		if(o){
			try{
				var info = o.SUX.root.modalPopupInfo[Sepoa.request.parameterId];
				Sepoa.params = info.params;
				window.selfObj = info.selfObj;
				o.SUX.root.modalPopupInfo[Sepoa.request.windowParameterId] = null;
				delete o.SUX.root.modalPopupInfo[Sepoa.request.windowParameterId];
			}catch(e){
				console.error(e);
			}
		}
	}
	
	$(document).on({
		keydown:function(event){
			if (event.keyCode === 8) {
				var doPrevent = true;
			    var d = $(event.srcElement || event.target);
			    var disabled = d.prop("readonly") || d.prop("disabled");
			    if (!disabled) {
			    	if (d[0].isContentEditable) {
			        	doPrevent = false;
			        } else if (d.is("input")) {
			            doPrevent = false;
			        } else if (d.is("textarea")) {
			            doPrevent = false;
			        }
			    }
			    if (doPrevent) {
			        event.preventDefault();
			        return false;
			    }
			}else if(event.keyCode === 122 && event.ctrlKey){
				if(!Sepoa.common.DEV_FLAG) return;
				
				if(SUX.modifyBtn){
					$('.dhx_carousel_controls').hide();
					$('.sepoa_modify_button').hide();
					$('.sepoa_modify_button_area').remove();
					delete SUX.modifyBtn;
				}else{
					$('.dhx_carousel_controls').show();
					$('.sepoa_modify_button').show();
					var eventChecker = $._data($('.sepoa_modify_button')[0], "events");
					
					if( !eventChecker ) {
						$('.sepoa_modify_button').on('mouseenter', function(e) {
							var contents;
							
							if( $(this).hasClass('page_modify_button') ) {
								contents = $(this).closest('.screen_area')[0] || $(this).closest('.sepoaux_component')[0];
							} else {
								contents = $(this).closest('.sepoaux_component')[0];
							}
							
							var divArea = document.createElement('div');
							$(divArea).addClass('sepoa_modify_button_area');
							$(divArea).css('width', $(contents).width())
									  .css('height', $(contents).height())
							$(contents).prepend(divArea);
							
							var descArea = document.createElement('div');
							$(descArea).addClass('sepoa_modify_button_area').addClass('sepoa_modify_button_description');
							$(descArea).text($(contents).attr('id'));
							$(descArea).css('margin-right', $(contents).width() - $(this).offset().left + $(this).width());
							$(divArea).prepend(descArea);
							
							var descAreaMargin = parseInt($(descArea).css('margin-right'));
							
							if( descAreaMargin < 50 ) {
								var rightValue = 50 - descAreaMargin;
								$(descArea).css('margin-right', descAreaMargin + rightValue);
							}
						});
						$('.sepoa_modify_button').on('mouseleave', function(e) {
							$('.sepoa_modify_button_area').remove();
						});
					}
					SUX.modifyBtn = true;
				}
			}else if(event.keyCode === 123 && event.ctrlKey){
				callFunction('MAINFRAME', 'toggleScreenInfo', []);
			}else if([33,34].indexOf(event.keyCode)+1){
				event.preventDefault();
		        return false;
			}else{
				SUX.callEvent('onKeyDown', [event.originalEvent]);
			}
		},
		keyup:function(obj){
			SUX.callEvent('onKeyUp', [obj.originalEvent]);
		},
		mousemove:function(e){
			var parents = $(e.toElement).parents('div.sepoaux.screen_area');
			if(parents.length > 0){
				var info = {};
				info.id   = parents[0].id;
				info.path = parents[0].getAttribute('path')
				e.bubbles= false;
				if(SUX.root.showScreenInfo)
					callFunction('MAINFRAME', 'showScreenInfo', [info, e]);
			}
		}
	});
	(function(){
		Sepoa.attachEvent('onLoadEnd', function(sid, sObject){
			if(Sepoa.common.DEV_FLAG){
				var d = sObject._vp[0];
				var carousel = d.querySelector("div.dhx_carousel_controls");
				var btn = document.createElement('div');
				btn.className="sepoa_modify_button page_modify_button";
				d.appendChild(btn);
				btn.onclick=function(){
					PopupOpenPostReal(window.CONTEXT_NAME+'/Tools/ScreenBuilder/ToolsScreenBuilder.sux', window.screen.width, window.screen.height, this.parentElement.id, {
						page:this.parentElement.getAttribute('path')+".xml"
					})
				}
				btn.style.transform="scaleX(-1)"
				//btn.innerHTML = '<img src="/theme/basic/imgs/common/builder.png">';
				btn.style.display = "none";
				btn.style.marginRight = ( carousel==null ? "0" : "27px" );
			}
		})
	})();	
}