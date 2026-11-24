SepoaIndex = function(config){
	sUtils.modules.extend(this, "EventManager");
	sUtils.modules.extend(this, this[config.mode || 'login']);
	this.$var = {};
	this.config = $.extend(true, {}, config);
	
	if(config.isPopup){
		this.config.cont.classList.add('sepoa-index-type-popup');
	}
}
SepoaIndex.prototype.getElement=function(type){
	return $(this.config.cont).find('.sepoa-index-'+type)[0];
}
SepoaIndex.prototype.getImageUrl=function(v){
	if(v){
		if(v.indexOf('file://') == 0){
			v = v.replace('file://', '');
		}else{
			var sv = v.split(':');
			v = window.CONTEXT_NAME+"/SepoaImage.sux?attach_key="+sv[0]+"&attach_seq="+sv[1]+"&HOUSE_CODE="+Sepoa.info.HOUSE_CODE;
		}
	}
	return v;
}
SepoaIndex.prototype.find=function(){
	this._popup = {
		url:window.CONTEXT_NAME+'/010/form/default/findPW.jsp',
		width:520, 
		height:270,
		title:sUtils.cache.text.get('LOGIN.FIND_PASSWORD')
	};
	this.callPopup();
}
SepoaIndex.prototype.login={
	send:function(){
		var config = {
			progress:true,
			nickName:"sepoa.svc.system.login.SepoaLoginService",
			conType:"CONNECTION",
			methodName:$var.module.config.service,
			requestMode:"doServiceAjax",
			callback:function(so){
				var shareObj = {};
				var result = $var.module.callEvent('onLoginAfter', [so, shareObj])
				if(shareObj.message){
					this.setMessage(shareObj.message, 2000);
				}
				$var.module.callEvent('onLoginEnd', [result]);
			}.bind($var.module)
		}
		var data = {};
		data[Sepoa.common.KEY_PARAMS]= encodeUrl(sUtils.url.toSepoaParams($.extend($var.module.config.sendParams||{},{
			ID:$('#id').val(), 
			PASSWORD:$('#password').val()
		}),false));
		sUtils.connector.service(config, data);
	},
	setMessage:function(str, timeout){
		if(document.getElementById('login_message')){
			if (str.includes('LOGIN.STATUS_100001')) {
				str = '존재하지 않는 계정입니다.';
				document.getElementById('login_message').innerHTML = str;
				setTimeout(function(){document.getElementById('login_message').innerHTML = '';}, timeout);
			} else if (str.includes('LOGIN.STATUS_100003')) {
				str = '비밀번호가 맞지 않습니다.';
				document.getElementById('login_message').innerHTML = str;
				setTimeout(function(){document.getElementById('login_message').innerHTML = '';}, timeout);
			}
		}
	},
	showCapsLockWarning:function(flag){
		if(this.$var._capslock_warn_elem){
			if(flag){
				this.$var._capslock_warn_elem.classList.add("active");
			}else{
				this.$var._capslock_warn_elem.classList.remove("active");
			}
		}
	}
}
SepoaIndex.prototype.callPopup=function(){
	if(this._popup && this._popup.url){
		var host = document.location.protocol+"//"+document.location.hostname;
		if(this._popup.url.indexOf('/') == 0 || this._popup.url.indexOf(host) == 0){
			sUtils.popup.modal(this._popup);
		}else{
		sUtils.popup.real($.extend(true, {isPost:false}, this._popup));
		}
	}
}