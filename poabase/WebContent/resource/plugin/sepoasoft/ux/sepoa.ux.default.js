/* default */
window.CONTEXT_NAME = window.CONTEXT_NAME || "";

/* ajax custom */
if($.ajax){
	(function(){
		var t = $.ajax;
		$.ajax = function(){
			if(arguments.length > 0){
				arguments[0].originBeforeSend = arguments[0].beforeSend;
				arguments[0].beforeSend = function(e, r){
					if(!arguments[1].url.toLowerCase().startsWith('http')) {
						if(typeof(sTokenString) != "undefined" && sTokenString){
							e.setRequestHeader("s-token", sTokenString);
						}	
					}
					if(r.originBeforeSend) r.originBeforeSend.apply(this, [e, r]); 
		        }
			}
			return t.apply(this, [].slice.call(arguments));
		}
	})()
}

/* adapter */
/* string util */
window.LTrim = sUtils.string.trimStart;
window.RTrim = sUtils.string.trimEnd;
window.LRTrim = function(str){
	return str.trim();
}
window.lpad = sUtils.string.lpad;
window.rpad = sUtils.string.rpad;
window.getTextWidth=sUtils.string.getWidth;
window.htmlDecode = sUtils.string.decodeHTML;
window.numberWithCommas=function(number){
	return sUtils.number.format(number, "0,000");
}

/* url */
window.encodeUrl = encodeURIComponent;
window.decodeUrl = decodeURIComponent;
window.checkContextUrl = sUtils.url.checkContext;
window.O2SP=sUtils.url.toSepoaParams;
window.makeUrlParameter=function(url, params){
	return url+'?'+sUtils.url.toQueryString(params);
}

/* validator */
window.checkValidationIrsNo=function(value){
	return sUtils.validator.valiate("IRS_NO", value);
}

/* functions */
window.loadScript = sUtils.loader.script;
window.loadCss = sUtils.loader.css;
window.checkBusiness=function(s, m){
	return sUtils.workflow.check(m);
}
window.SepoaFormater = function(data){
	$.extend(this, sUtils.SepoaFormater.get(data));
}
window.getDocumentNumber=sUtils.ux.getDocumentNumber;
window.sepoaExcelProcessor=function(type, process_id, method, callback, template_file, params, flag){
	params = params || {};
	params.PROCESS_ID = process_id;
	params.METHOD = method;
	if(type == "up"){
		sUtils.excel.upload(params, callback);
	}else if(type == "down"){
		params.template_file = template_file;
		sUtils.excel.download(params);
	}
}
window.doServiceAjaxFile=function( nickName, conType, methodName, params){
	var url = window.CONTEXT_NAME+"/SepoaAjaxFile.sux?nickName=" + nickName + "&conType=" + conType + "&methodName=" + methodName;
	if ((params == undefined || params == null) && this.dataOutput) {
		params = this.dataOutput.getParams();
	}else if(typeof(params) != 'string'){
		params = "&"+Sepoa.common.KEY_PARAMS+"="+encodeUrl(sUtils.url.toSepoaParams(params, false));
	}
	url += params + "&requestMode=doServiceAjaxFile";
	return sUtils.multipart.download({url:url});
}
window.doServiceAjax=function( nickName, conType, methodName, params, callback , async, useProgress){
	var config={
		progress:(useProgress === false)?false:true,
		nickName:nickName,
		conType:conType,
		methodName:methodName,
		requestMode:"doServiceAjax",
		callback:callback,
		async:(async === false)?false:true
	}
	
	var data = {};
	if (params == undefined || params == null) {
		data[Sepoa.common.KEY_PARAMS] = this.dataOutput.getParams(false);
	}else if(typeof(params) != 'string'){
		data[Sepoa.common.KEY_PARAMS] = encodeURIComponent(sUtils.url.toSepoaParams(params,false));
	}
	
	if(this.dataOutput && this.dataOutput.hasGridData()) {
		data = $.extend(true, data, this.dataOutput.getGridData());
 	}
	return sUtils.connector.service(config, data);
}

/* popup */
window.PopupOpenPost=function(url, params, callback, width, height, title, code){
	var config = {
		params:$.extend(true, {}, params||{})
	};
	if(url){
		config.url = url;
	}
	if(width){
		config.width = width;
	}
	if(height){
		config.height = height;
	}
	if(title){
		config.title = title;
	}
	if(callback){
		config.params.callback = callback;
	}
	if(code){
		config.code = code;
	}
	return sUtils.popup.modal(config);
}
window.PopupCommonPost=function( strCode, callback, arrValue, arrDesc, width, height, url, title, param){
	var config = {
		width:"500",
		height:"540",
		title:sUtils.cache.text.get("BASE_LANG.SEARCH"),
		code:"default",
		url:window.CONTEXT_NAME+"/UxDefault/Common/Popup.sux",
		params:{
			popupType:"dhtmlxWindow",
			code:strCode
		}
	}
	
	if(url){
		config.url = url;
	}
	if(width){
		config.width = width;
	}
	if(height){
		config.height = height;
	}
	if(title){
		config.title = title;
	}
	if (arrDesc && arrDesc.join) {
		config.params.desc = arrDesc.join(Sepoa.common.FIELD);
	}
	if (arrValue && arrValue.join) {
		config.params.values = arrValue.join(Sepoa.common.FIELD);
	}
	if(callback){
		config.params.callback=callback;
	}
	if (param) {
		config.params.param = param;
	}
	
	return sUtils.popup.modal(config);
}
window.vaultPopup=function(data){
	var config={
		width:"400",
		height:"350",
		title:sUtils.cache.text.get("BASE_LANG.VAULT"),
		code:"vault",
		url:window.CONTEXT_NAME+"/UxDefault/Common/Vault.sux",
		params:data
	}
	return sUtils.popup.modal(config);
}
window.PopupOpenPostReal=function(url, width, height, title, aparam, flag, tabFlag){
	var config={
		url:url,
		isTab:tabFlag?true:false
	}
	if(width){
		config.width = width;
	}
	if(height){
		config.height = height;
	}
	if(title){
		config.title = title;
	}
	if(!aparam){
		aparam = {};
	}else if(typeof(aparam) == "string"){
		tmpParam = {};
		var arr = ((aparam||"").split('&') || []);
		for(var i=0;i<arr.length;i++){
			if (arr[i] != "") {
				pa = arr[i].split('=');
				tmpParam[pa[0]] = pa[1];
			}
	    }
		aparam = tmpParam;
	}
	config.params = aparam;
	
	if(url.indexOf('?') >= 0){
		var arr = url.split('?')[1].split('&');
		for(var i=0;i<arr.length;i++){
			if (arr[i] != "") {
				pa = arr[i].split('=');
				config.params[pa[0]] = pa[1];
			}
	    }
	}
	return sUtils.popup.real(config);
}
window.linkTo=function(_id, _url, _param, _menuName){
	if(typeof _id != "string"){
		const data =_id;
		_id = data.id;
		_param = data.param;
		_menuName = data.label;
		
		if(!sUtils.string.isBlank(data.mobileUrl) && sUtils.browser.isMobile()){
			_url = data.mobileUrl;
		}else{
			_url = data.url;
		}
	}
	
	if(_param == 'undefined') {
		_param = void 0
	}
	callFunction('MAINFRAME','openContainerUrl', [_id, _url, _param, _menuName]);
}