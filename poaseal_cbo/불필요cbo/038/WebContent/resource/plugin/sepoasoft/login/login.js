SepoaIndex = function(config){
	sUtils.modules.extend(this, "EventManager");
	sUtils.modules.extend(this, this[config.mode || 'login']);
	this.$var = {};
	this.config = $.extend(true, {}, config);
	
	if(config.isPopup){
		this.config.cont.classList.add('sepoa-index-type-popup');
	}
	this.init();
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
			v = (window.CONTEXT_NAME || '') + "/SepoaImage.sux?attach_key="+sv[0]+"&attach_seq="+sv[1]+"&HOUSE_CODE="+Sepoa.info.HOUSE_CODE;
		}
	}
	return v;
}
SepoaIndex.prototype.login={
	init:function(){
		this.loadHTML();
		this.createEvents();
		this.renderPage();
	},
	createEvents:function(){
		
	},
	loadHTML:function(){
		$.get({
			url:window.CONTEXT_NAME+'/resource/plugin/sepoasoft/login/form/'+this.config.form+'/login.html',
			dataType:'html',
			async:false,
			cache:false,
			success:function(data){
				$(this.config.cont).append($(sUtils.string.dataReplacer(data, this.config.style)));
			}.bind(this)
		})
	},
	renderPage:function(){
		var renderTarget = $('.sepoa-index-module-target');
		for(var i=0;i<renderTarget.length;i++){
			var elem = renderTarget[i];
			var renderInfo = {};
			for(var j=0;j<elem.classList.length;j++){
				var className = elem.classList[j];
				var m = className.match(/sepoa-index-module-([a-z_]{0,})-([a-zA-Z_0-9]{0,})/);
				if(m){
					renderInfo[m[1]] = m[2];
				}
			}
			
			if("image" == renderInfo.type){
				elem.onerror=function(){
					this.classList.add("sepoa-index-logo-error");
					var def = this.getAttribute('defsrc')
					if(def){
						this.onerror=undefined;
						this.src=window.CONTEXT_NAME+def;
					}
				}
				elem.src = this.getImageUrl(this.config.style[renderInfo['var']]);
			}else if("back_color" == renderInfo.type){
				elem.style.backgroundColor=this.config.style[renderInfo['var']];
			}else if(renderInfo.type.indexOf("remark") == 0){
				var text = this.config.style[renderInfo['var']];
				var color = this.config.style[renderInfo['var'] + "_TEXT_COLOR"];
				if(text){
					elem.innerHTML = '<span>'+text+'</span>'
					elem.style.color = color;
				}
			}else if("login_input" == renderInfo.type){
				if(elem.id == "id"){
					elem.onkeydown=function(e){
						if (e.keyCode == 13) {
							$('#password')[0].focus();
						}
					}.bind(this);
					elem.placeholder = sUtils.cache.text.get("LOGIN.ID");
				}else{
					elem.onkeydown=function(e){
						if (e.keyCode == 13) {
							this.send();
						}else{
							if(e.getModifierState("CapsLock")){
								this.showCapsLockWarning(true);
							}else{
								this.showCapsLockWarning(false);
							}
						}
					}.bind(this);
					elem.placeholder = sUtils.cache.text.get("LOGIN.PASSWORD");
				}
				
			}else if("login_capslock_warning" == renderInfo.type){
				elem.innerHTML = sUtils.string.dataReplacer('<span>$LANG{LOGIN.CAPSLOCK_WARNING}</span>');
				this.$var._capslock_warn_elem = elem;
			}else if("login_button" == renderInfo.type){
				elem.style.color=this.config.style[renderInfo['var']];
				elem.value = this.config.style.LOGIN_FORM_LABEL || sUtils.cache.text.get("LOGIN.LOGIN_BUTTON");
				elem.onclick=function(){
					this.send();
				}.bind(this);
			}else if("login_message" == renderInfo.type){
				this.$var._login_message = elem;
			}else if("login_submenu" == renderInfo.type){
				if(renderInfo['var'] == 'FIND_ID_PW'){
					elem._popup = {
						url:window.CONTEXT_NAME+'/common/FindLayout.sux',
						width:520, 
						height:270,
						title:sUtils.cache.text.get('LOGIN.FIND_ID_PW')
					};
					elem.innerHTML = sUtils.string.dataReplacer('<span>$LANG{LOGIN.FIND_ID_PW}</span>')
				}else{
					if(this.config.style.LOGIN_FORM_SUB_BUTTON_USE == "Y"){
						elem.innerHTML = '<span>'+this.config.style.LOGIN_FORM_SUB_BUTTON_LABEL+'</span>'
						elem._popup = {
							url:window.CONTEXT_NAME+this.config.style.LOGIN_FORM_SUB_BUTTON_LINK,
							width:1200, 
							height:900,
							title:this.config.style.LOGIN_FORM_SUB_BUTTON_LABEL
						};
						elem.classList.add('show');
					}
				}
				elem.onclick=this.callPopup;
			}else if("footer" == renderInfo.type){
				elem.innerHTML = this.config.style.COPYRIGHT_CONTENTS || "COPYRIGHT ⓒ"+sUtils.date.getYear()+" Sepoasoft ALL RIGHT RESERVED.";
			}else if(renderInfo.type.indexOf("contact") == 0){
				var _splited = renderInfo.type.split('_');
				var target = _splited[0].toUpperCase();
				var type = _splited[1];
				
				var contents = this.config.style[target+"_CONTENTS"];
				if(contents){
					if(type == "header"){
						elem.innerHTML = sUtils.string.dataReplacer('<span>$.{TEXT}</span>', {TEXT:this.config.style[target.toUpperCase()+"_HEADER"] || sUtils.cache.text.get("LOGIN."+target)});
					}else if(type == "contents"){
						elem.innerHTML = '<span>'+contents.replace(/\n/g, '</br>')+'</span>'
					}
					$(elem).parents('.sepoa-index-bottom-item.sepoa-index-bottom-item-contact')[0].classList.add('show');
				}
			}else if(renderInfo.type.indexOf("menu") == 0){
				var c = parseInt(this.config.style.GUIDE_ITEM_COUNT || 0);
				var w = ['FIRST', 'SECOND', 'THIRD'];
				var _idx = w.indexOf(renderInfo['var']);
				
				var menuHtml = '<div class="sepoa-index-module-var-$.{MENU_ID} sepoa-index-bottom-item-menu-item show">'
					 + '	<div class="sepoa-index-bottom-item-menu-item-icon-wrapper">'
					 + '		<img class="sepoa-index-bottom-item-menu-item-icon" onerror="this.style.display=\'none\'" src=$.{MENU_ICON}>'
					 + '	</div>'
					 + '	<div class="sepoa-index-bottom-item-menu-item-label $.{MENU_LABEL_USE}">'
					 + '		<span>$.{MENU_LABEL}</span>'
					 + '	</div>'
					 + '</div>'
				
				for(var _idx=0;_idx<c;_idx++){
					var menuID = w[_idx] || _idx;
					var menuItemElem = elem.appendChild($(sUtils.string.dataReplacer(menuHtml, {
						MENU_ID:menuID,
						MENU_ICON:this.getImageUrl(this.config.style['GUIDE_SUB_ITEM_'+menuID]),
						MENU_LABEL:this.config.style['GUIDE_SUB_ITEM_'+menuID+'_LABEL'] || '',
						MENU_LABEL_USE:(this.config.style['GUIDE_SUB_ITEM_'+menuID+'_LABEL'])?'show':''
					}))[0]);
					
					menuItemElem._popup = {
						url:this.config.style['GUIDE_SUB_ITEM_'+menuID+'_LINK'],
						title:this.config.style['GUIDE_SUB_ITEM_'+menuID+'_LABEL']
					}
					menuItemElem.onclick=this.callPopup;
				}
			}else if("custom_iframe" == renderInfo.type){
				if(this.config.style.RIGHT_CUSTOM_PATH){
					var iframeElem = document.createElement('iFrame');
					iframeElem.src = this.config.style.RIGHT_CUSTOM_PATH;
					elem.appendChild(iframeElem);
				}
			}
		}
		if(this.config.isPopup && selfObj){
			var cont = $('.sepoa-index-container');
			if(cont.length > 0){
				selfObj.setDimension(cont[0].offsetWidth, cont[0].offsetHeight);
			}
		}
	},
	send:function(){
		var config = {
			progress:true,
			nickName:"sepoa.svc.system.login.SepoaLoginService",
			conType:"CONNECTION",
			methodName:this.config.service,
			requestMode:"doServiceAjax",
			callback:function(so){
				var shareObj = {};
				var result = this.callEvent('onLoginAfter', [so, shareObj])
				if(shareObj.message){
					this.setMessage(shareObj.message);
				}
				this.callEvent('onLoginEnd', [result]);
			}.bind(this)
		}
		var data = {};
		data[Sepoa.common.KEY_PARAMS]= encodeUrl(sUtils.url.toSepoaParams($.extend(this.config.sendParams||{},{
			ID:$('#id').val(), 
			PASSWORD:$('#password').val()
		}),false));
		sUtils.connector.service(config, data);
	},
	setMessage:function(str){
		if(this.$var._login_message){
			this.$var._login_message.innerHTML = str;
			if(this.$var._login_message_event){
				clearTimeout(this.$var._login_message_event)
			}
			this.$var._login_message_event = setTimeout(function(){
				this.$var._login_message.innerHTML = '';
			}.bind(this), 2000);
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