/**
 * IE하위버전(7,8)에서 Object.create 함수가 존재하지 않기에
 * 크롬브라우저에서 제공하는 Object.create 함수를 이용해서 사용한다.
 * dhtmlx grid,calendar를 상속받는 과정에서 Object.create함수를 사용한다.
 * dhtmlx 의 객체 생성자들은 new 하는 과정에서 html tag를 생성하기 때문에 상속하는 과정에서 new 명령어를 사용할 수 없다.
 * 그러기에 이러한 방법을 이용하였다.
 * */
if (!Object.create) {
    Object.create = (function(){
        function F(){}

        return function(o){
            if (arguments.length != 1) {
                throw new Error('Object.create implementation only accepts one parameter.');
            }
            F.prototype = o;
            return new F()
        }
    })()
}
if(!Object.keys) Object.keys = function(o){
    if (o !== Object(o))
         throw new TypeError('Object.keys called on non-object');
    var ret=[],p;
    for(p in o) if(Object.prototype.hasOwnProperty.call(o,p)) ret.push(p);
    return ret;
}

window.dhx4.ajax.method='post';

function SepoaDhtmlXVaultObject(data){
	//DhtmlxVault 초기데이터 생성
	var init_data = new Object();
	init_data.uploadUrl = data.url ||( window.CONTEXT_NAME + "/SepoaVault.sux");
	init_data.downloadUrl =  data.url ||( window.CONTEXT_NAME +"/SepoaVault.sux");
	init_data.container = (data.container || data.container != null)?data.container:null;
	init_data.skin = (data.skin || data.skin != null)?data.skin:'material';
	init_data.parent = (data.parent || data.parent != null)?data.parent:undefined;
	init_data.mode = "html4";
	init_data.autoRemove = true;
	
    var g = null;
    if (typeof(window.FormData) != "undefined" && typeof(window.XMLHttpRequest) != "undefined") {
        g = new XMLHttpRequest();
        if (typeof(g.upload) == "undefined") {
            g = null
        }
    }
    if (g != null) {
    	init_data.mode = "html5";
    	g = null;
    }
	
    var base = dhtmlXVaultObject.call(this, init_data);
    
    if(this.cont){
    	this.cont.dhtmlxObject = this;
    	this.cont.classList.add('sepoaux_vault');
    }
	
	this.conf.max_file_size = (data.maxsize == void 0) ? Sepoa.config.get('sepoa.attach.maxsize') * 1024 * 1024 : data.maxsize; 
	
	this._clearToolbar();
	this._initCustomToolbar();
	this.setSkin(this.conf.skin);
	
	//버튼정상작동을위한 엔진 재설정
	this._initEngine2();
	
	//모드에따른버튼변경
	this.changeMode(data.mode);
	
	//추가설정정보생성
	this._callbackid = "";
	this.setCallBack(data.callback);
	this.conf.type = data.type;
	this.conf.attach_key = (!data.attach_key)?'':data.attach_key;
	this.conf.attach_seq = 0;
	this.conf.updateUrl = checkContextUrl(data.url || (window.CONTEXT_NAME+"/SepoaVault.sux"));
	this.conf.param = (!data.param)?'':data.param;
	this.conf.restriction = data.restriction||"";
	
	this.conf.service = (data.service || data.service != null)?data.service:undefined;
	
	if(this.conf.service && this.conf.service != null){
		this.conf.select 	= (data.select || data.select != null)?data.select:undefined;
		this.conf.upload 		= (data.upload || data.upload != null)?data.upload:undefined;
		this.conf['delete'] 	= (data['delete'] || data['delete'] != null)?data['delete']:undefined;
	}
	
	//attach_key 가 존재할경우 초기데이터 로드
	if(this.conf.attach_key != ''){
		this.setAttachKey(this.conf.attach_key);
	}
	
	//업로드 실패시 Alert 발생
	this.attachEvent("onUploadFail", function(e, a){
    	Alert(a);
    });
	
	//업로드성공시
	this._onUploadSuccess = function(n, m, k, a, ak, obj) {
    	m = decodeURIComponent(m);
        if (typeof(k) != "undefined" && this.conf.engine == "flash") {
            var e = window.dhx4.s2j(k.data);
            if (e != null && e.state == true && e.name != null) {
                m = e.name;
                if (e.extra != null) {
                    a = e.extra
                }
            } else {
                this._onUploadFail(n, (e != null && e.extra != null ? e.extra : null));
                return
            }
        }
        
        if(this.conf.attach_seq*1 < m*1) this.conf.attach_seq = m;
        
        this.conf.attach_key = ak;
        this.conf.uploaded_count++;
        this.conf.uploaded_files[n] = {
            realName: this.file_data[n].name,
            serverName: m
        };
        
        this.file_data[n].state = "uploaded";
        this.conf.uploaded_state[n] = true;
        this._updateFileInList(n, "uploaded", 100);
        
        var callbackParam = {
        	attach_key: this.conf.attach_key,
            id: n,
            name: this.file_data[n].name,
            size: this.file_data[n].size,
            lastModifiedDate: this.file_data[n].file.lastModifiedDate || null,
            serverName: m,
            uploaded: true,
            error: false,
            fileCount: this.getData().length
        }
        
        if(obj != null && typeof(obj) == "object"){
        	callbackParam.serviceObj = obj;
        }
        
        this.callEvent("onUploadFile", [callbackParam, a,this.conf.param]);
        if (this.conf.uploading) {
            this._uploadStart()
        }
    };
    
    //업로드실패시
    this._onUploadFail = function(e, a) {
        this.file_data[e].state = "fail";
        this._updateFileInList(e, "fail", 0);
        this.conf.uploaded_state[e] = false;
        this.callEvent("onUploadFail", [{
            id: e,
            name: this.file_data[e].name,
            size: this.file_data[e].size,
            lastModifiedDate: this.file_data[e].file.lastModifiedDate || null,
            serverName: null,
            uploaded: false,
            error: true
        }, a,this.conf.param]);
        if (this.conf.auto_remove) {
            this._removeFileFromQueue(e)
        }
        if (this.conf.uploading) {
            this._uploadStart()
        }
    };
    
    //파일추가시 모드값을 넘겨주기위해 수정
    this._addFileToList = function(r, k, m, q, e) {
        var o = this.getFileExtension(k);
        var n = (o.length > 0 ? (this.conf.icons[o.toLowerCase()] || this.conf.icon_def) : this.conf.icon_def);
        var a = false;
        if (q == "added" && typeof(m) == "number" && m > 0 && this.conf.max_file_size > 0 && m > this.conf.max_file_size) {
            q = this.file_data[r].state = "size_exceeded";
            a = true
        }
        this.list.addFileItem(r, this.p_files);
        this.list.renderFileRecord(r, {
            name: k,
            icon: n,
            size: m,
            readableSize: this.readableSize(m || 0),
            state: q,
            progress: e,
            mode: this.conf.mode
        });
        if (q == "size_exceeded") {
            this.list.updateFileState(r, {
                state: q,
                str_size_exceeded: window.dhx4.template(this.strings.size_exceeded, {
                    size: this.readableSize(this.conf.max_file_size)
                })
            })
        }
        this.callEvent("onFileAdd", [{
            id: r,
            name: k,
            size: m,
            lastModifiedDate: this.file_data[r].file.lastModifiedDate || null,
            serverName: null,
            uploaded: false,
            error: a
        }])
    };
    
    var thisObj = this;
    if(typeof(Sepoa) != 'undefined'){
	    Sepoa.attachEvent('onWindowResize', function(){
	    	thisObj.setSizes();
	    });
    }
    
    this._switchButton = function(n) {
        if (n == true) {
            if (this.conf.buttons.upload == true) {
                this.buttons.upload.style.display = "none";
                this.buttons.cancel.style.display = ""
            }
        } else {
            var k = this.conf.uploaded_count;
            var m = [];
            for (var e in this.conf.uploaded_state) {
                m.push({
                    id: e,
                    name: (this.file_data[e] != null ? this.file_data[e].name : null),
                    size: (this.file_data[e] != null ? this.file_data[e].size : null),
                    lastModifiedDate: (this.file_data[e] != null ? (this.file_data[e].file.lastModifiedDate || null) : null),
                    serverName: (this.conf.uploaded_files[e] ? this.conf.uploaded_files[e].serverName : null),
                    uploaded: this.conf.uploaded_state[e],
                    error: !this.conf.uploaded_state[e]
                })
            }
            if (this.conf.buttons.upload == true) {
                this.buttons.upload.style.display = "";
                this.buttons.cancel.style.display = "none"
            }
            this.conf.uploaded_count = 0;
            this.conf.uploaded_state = {};
            if (k > 0) {
                this.callEvent("onUploadComplete", [m])
            }
        }
    };
    this.setProgressMode("eta");
}
SepoaDhtmlXVaultObject.prototype = Object.create(dhtmlXVaultObject.prototype);
SepoaDhtmlXVaultObject.prototype.constructor = SepoaDhtmlXVaultObject;
SepoaDhtmlXVaultObject.prototype.setSingleFileMode=function(f){
	if(f){
		this.f.removeAttribute("multiple")
	}else{
		this.f.setAttribute("multiple", "")
	}
}
SepoaDhtmlXVaultObject.prototype.setType = function(t) {
	this.conf.type = t;
	this.setAttachKey();
}
SepoaDhtmlXVaultObject.prototype.setSizes = function() {
    var a = $(this.base).outerWidth() - $(this.base).innerWidth();
    var b = $(this.base).outerHeight() - $(this.base).innerHeight();
    if (this.base._attach_mode != true) {
        this.base.style.width = Math.max(0, this.base.parentNode.clientWidth - a) + "px";
        this.base.style.height = Math.max(0, this.base.parentNode.clientHeight - b) + "px"
    }
    var c = this.conf.ofs[this.conf.skin];
    this.p_files.style.top = this.p_controls.offsetHeight + "px";
    this.p_files.style.left = c + "px";
    if (!this.conf.ofs_f) {
        this.p_files.style.width = "100px";
        this.p_files.style.height = "100px";
        this.conf.ofs_f = {
            w: this.p_files.offsetWidth - this.p_files.clientWidth,
            h: this.p_files.offsetHeight - this.p_files.clientHeight
        }
    }
    this.p_files.style.width = Math.max(this.base.clientWidth - c * 2 - this.conf.ofs_f.w, 0) + "px";
    this.p_files.style.height = Math.max(this.base.clientHeight - this.p_controls.offsetHeight - c - this.conf.ofs_f.h, 0) + "px";
    if (typeof(this.callEvent) == "function") {
        this.callEvent("_onSetSizes", [])
    }
};
//attach_key 반환
SepoaDhtmlXVaultObject.prototype.getAttachKey = function(){
	return this.conf.attach_key;
};
//모드변경 'VI' / 'NODEL' / '' 
SepoaDhtmlXVaultObject.prototype.changeMode = function(mode){
	if(mode == 'VI'){
		this.showButton('clear', false);
		this.showButton('upload', false);
		this.showButton('download', true);
		this.showButton('browse', false);
		$('.dhx_vault_file_delete').hide();
		this.conf.mode=mode;
	}else if(mode == 'NODEL'){
		// 업로드 가능, clear/삭제 불가
		this.showButton('clear', false);
		this.showButton('upload', false);
		this.showButton('download', true);
		this.showButton('browse', true);
		$('.dhx_vault_file_delete').hide();
		this.conf.mode=mode;
	}else{
		this.showButton('clear', true);
		this.showButton('upload', false);
		this.showButton('download', true);
		this.showButton('browse', true);
		$('.dhx_vault_file_delete').show();
		this.conf.mode="default"
	}
}
//레코드조회
SepoaDhtmlXVaultObject.prototype._getFileRecord = function(key){
	sUtils.token.get("vault", {
		action:"filelist",
		type:this.conf.type,
		attach_key:key
	}).then((t)=>{
		let param = "t="+t;

		if((this.conf.service && this.conf.service != null) && (this.conf.select && this.conf.select)){
			param += "&custom_class=" + this.conf.service 
					 +"&custom_method=" + this.conf.select;
		}
		
		this._lastExecuteGetFileRecode = (new Date).getTime()+"_"+dhx.newId();
	    param += "&_execute_="+this._lastExecuteGetFileRecode;
	    
	    window.dhx4.ajax.post(this.conf.url, param, (e)=>{
	    	const exData = JSON.parse('{"' + decodeURI(e.xmlDoc._reqData.data.replace(/&/g, "\",\"").replace(/=/g,"\":\"")) + '"}');
	    	if(exData._execute_ == this._lastExecuteGetFileRecode){
	    		const data = JSON.parse(e.xmlDoc.responseText);
	    		if(data.flag === false){
	    			this.conf.loading = false;
	    		}else{
	    			if(this.conf.waitingKey){
	    				const key = this.conf.waitingKey
	    				this.conf.waitingKey = null;
	    				this.conf.loading = false;
	    				this.setAttachKey(key);
	    			}else{
	    				this._setFileRecord(data);
	    				this.callEvent('onUploadFile',[{attach_key:this.conf.attach_key,fileCount: this.getData().length},void 0, this.conf.param]);
	    				this.conf.loading = false;
	    			}
	    		}
			}else{
				this.conf.loading = false;
			}
	    });
	}).catch((message)=>{
		AlertE(message);
	})
}
//레코드셋팅
SepoaDhtmlXVaultObject.prototype._setFileRecord = function(data){
	this.conf.attach_seq = data.seq;
	for(var _idx in data.file){
		this.addFileRecord(data.file[_idx],"uploaded");
		
	}
}
//콜백펑션셋팅
SepoaDhtmlXVaultObject.prototype.setCallBack = function(f){
	if(typeof(f) == 'string'){
		f = eval(f);
	}
	
	if(this._callbackid != ""){
		this.detachEvent(this._callbackid);
		this._callbackid = "";
	}
	if(f){
		this._callbackid = this.attachEvent("onUploadFile", f);
	}
}
//버튼커스텀
SepoaDhtmlXVaultObject.prototype._initCustomToolbar = function(){
	var h = this;
	this.b_opts = {
        browse: {
            str: "btnAdd",
            onclick: null
        },
        upload: {
            str: "btnUpload",
            onclick: function() {
                if (!h.conf.enabled) {
                    return
                }
                if (!h.conf.uploading) {
                    h._uploadStart()
                }
            }
        },
        download: {
            str: "btnDownload",
            onclick: function() {
                if (!h.conf.enabled) {
                    return
                }
                if (!h.conf.uploading) {
                    h._doDownloadFile();
                }
            }
        },
        cancel: {
            str: "btnCancel",
            onclick: function() {
                if (!h.conf.enabled) {
                    return
                }
                h._uploadStop();
                h._switchButton(false)
            }
        },
        clear: {
            str: "btnClean",
            onclick: function() {
                if (!h.conf.enabled) {
                    return
                }
                h.clear()
            },
            css: "float:right!important;"
        }
    };
    this.buttons = {};
    for (var e in this.b_opts) {
        var m = document.createElement("DIV");
        m.innerHTML = "<div class='dhxvault_button_icon dhx_vault_icon_" + e + "'></div><div class='dhxvault_button_text'>" + this.strings[this.b_opts[e].str] + "</div>";
        if (this.b_opts[e].css != null) {
            m.style.cssText += this.b_opts[e].css
        }
        m.className = "dhx_vault_button";
        m._css = m.className;
        m._onclick = this.b_opts[e].onclick;
        m.onmouseover = function() {
            if (h.conf.enabled != true) {
                return
            }
            if (this._hover == true) {
                return
            }
            this._hover = true;
            this.className = this._css + " dhx_vault_button" + this._css_p + "_hover"
        };
        m.onmouseout = function() {
            if (h.conf.enabled != true) {
                return
            }
            if (this._hover != true) {
                return
            }
            this._hover = false;
            this.className = this._css
        };
        m.onmousedown = function() {
            if (h.conf.enabled != true) {
                return
            }
            if (this._hover != true) {
                return
            }
            this._pressed = true;
            this.className = this._css + " dhx_vault_button" + this._css_p + "_pressed"
        };
        m.onmouseup = function(a) {
            if (h.conf.enabled != true) {
                return
            }
            if (this._pressed != true) {
                return
            }
            this._pressed = false;
            this.className = this._css + (this._hover ? " dhx_vault_button" + this._css_p + "_hover" : "");
            if (this._onclick != null) {
                this._onclick()
            }
        };
        if (this.b_opts[e].tooltip) {
            m.title = this.b_opts[e].tooltip
        }
        this.p_controls.appendChild(m);
        this.buttons[e] = m;
        m = null;
        if (e == "upload" || e == "clear" || e == "browse" || e == "download") {
            this.buttons[e].style.display = (this.conf.buttons[e] == true ? "" : "none")
        }
        this.b_opts[e].onclick = null;
        this.b_opts[e] = null;
        delete this.b_opts[e]
    }
    this.b_opts = null;
    delete this.b_opts;
    this.buttons.cancel.style.display = "none"
};
//상단버튼 제어
SepoaDhtmlXVaultObject.prototype.showButton = function(e, flag) {
	if (e == "upload" || e == "clear" || e == "browse" || e == "download") {
        this.buttons[e].style.display = (flag == true ? "" : "none")
    }
}
//버튼간격지정
SepoaDhtmlXVaultObject.prototype.setSkin = function(c) {
    if (c != this.conf.skin) {
        this.base.className = String(this.base.className).replace(new RegExp("s{0,}dhx_vault_" + this.conf.skin), " dhx_vault_" + c);
        this.conf.skin = c
    }
    this._updateBttonsSkin();
    var b = this.conf.ofs[this.conf.skin];
    this.buttons.browse.style.marginLeft = b + "px";
    this.buttons.upload.style.marginLeft = (c == "dhx_terrace" ? "-1px" : b + "px");
    this.buttons.cancel.style.marginLeft = this.buttons.upload.style.marginLeft;
    this.buttons.clear.style.marginRight = b + "px";
    var a = "";
    if (c == "dhx_terrace") {
        a = (this.conf.buttons.upload == true) ? "0px" : "3px"
    }
    this.buttons.browse.style.borderTopRightRadius = a;
    this.buttons.browse.style.borderBottomRightRadius = a;
    this.buttons.upload.style.borderTopLeftRadius = a;
    this.buttons.upload.style.borderBottomLeftRadius = a;
    this.buttons.cancel.style.borderTopLeftRadius = this.buttons.upload.style.borderTopLeftRadius;
    this.buttons.cancel.style.borderBottomLeftRadius = this.buttons.upload.style.borderBottomLeftRadius;
    this.setSizes()
    
    if(this.buttons.download){
	    this.buttons.download.style.borderTopLeftRadius = a;
	    this.buttons.download.style.borderBottomLeftRadius = a;
	    this.buttons.download.style.marginLeft = (c == "dhx_terrace" ? "-1px" : b + "px");
    }
};
//버튼삭제
SepoaDhtmlXVaultObject.prototype._clearToolbar = function(){
	$(this.p_controls).find('div').remove()
};
//문자열데이터
SepoaDhtmlXVaultObject.prototype.strings.btnDownload = "AllDownLoad";

//파일다운로드
SepoaDhtmlXVaultObject.prototype._doDownloadFile = function(b) {
	const tp = {
		action:"download",
		type:this.conf.type,
		attach_key:this.conf.attach_key
	};
	if(b){
		tp.attach_seq = (this.conf.uploaded_files != null && this.conf.uploaded_files[b] != null) ? this.conf.uploaded_files[b].serverName : null; 
	}else if(Object.keys(this.conf.uploaded_files).length == 0){
		Alert(sUtils.cache.text.get("BASE_FILE.NOT_FOUND_FILE_IN_LIST"));
		return;
	}
	
	sUtils.token.get("vault", tp).then((t)=>{
		const param = {t:t}
		if((this.conf.service && this.conf.service != null) && (this.conf.select && this.conf.select)){
			param.custom_class=this.conf.service;
			param.custom_method=this.conf.select;
		}
		
		sUtils.multipart.download({url:this.conf.download_url, multipart:false, callback:function(){
			Alert(sUtils.cache.text.get("BASE_FILE.FILE_DOWNLOAD_ERROR"));
		}}, param)
	}).catch((message)=>{
		AlertE(message);
	})
}
//attach_key 재설정
SepoaDhtmlXVaultObject.prototype.setAttachKey = function(a){
	if (this.conf.uploading) {
        this._uploadStop()
    }
	
	if(this.conf.loading){
		this.conf.waitingKey = a;
		return;
	}else{
		this.conf.loading = true;
	}
	
    this._switchButton(false);
	for (var e in this.file_data) {
    	this.file_data[e].file = null;
        this.file_data[e].name = null;
        this.file_data[e].size = null;
        this.file_data[e].state = null;
        this.file_data[e] = null;
        delete this.file_data[e];
        this._removeFileFromList(e);
    }
	this.conf.attach_key = (typeof(a) == 'undefined' || a == null)?'':a;
	this.conf.attach_seq = 0;
	if(this.conf.attach_key != ''){
		this._getFileRecord(this.conf.attach_key);
	}else{
		this.conf.loading = false;
	}
	this.callEvent('onUploadFile',[{attach_key:this.conf.attach_key, fileCount:(this.file_data.length || 0)}, undefined,this.conf.param]);
	this.callEvent('onChangeAttachKey', []);
}
//HTML5 모드
//파일업로드 서버호출 코드
SepoaDhtmlXVaultObject.prototype.html5.prototype._doUploadFile = function(c) {
    if (this.file_data[c].custom == true) {
        this._cfUploadStart(c);
        return
    }
    
    sUtils.token.get("vault", {
    	mode:"html5",
		type:this.conf.type,
		attach_key:this.conf.attach_key,
		restriction:this.conf.restriction,
		attach_seq:this.conf.attach_seq+""
	}).then((t)=>{
        var b = this;
	    if (!this.loader) {
	        this.loader = new XMLHttpRequest();
	        this.loader.upload.onprogress = function(d) {
	            if (b.file_data[this._idd].state == "uploading") {
	                b._updateFileInList(this._idd, "uploading", Math.round(d.loaded * 100 / d.total))
	            }
	        };
	        this.loader.onload = function(f) {
	            var d = window.dhx4.s2j(this.responseText);
	            if (d != null && typeof(d) == "object" && typeof(d.state) != "undefined" && d.state == true) {
	            	if(d.serviceDataObj != null && typeof(d.serviceDataObj) == "object"){
	            		b._onUploadSuccess(this.upload._idd, d.name, null, d.extra, d.attch_key, d.serviceDataObj);
	            	} else {
	            		b._onUploadSuccess(this.upload._idd, d.name, null, d.extra, d.attch_key)
	            	}
	                
	            } else {
	                b._onUploadFail(this.upload._idd, (d != null && d.extra != null ? d.extra : null))
	            }
	            d = null
	        };
	        this.loader.onerror = function(d) {
	            b._onUploadFail(this.upload._idd)
	        };
	        this.loader.onabort = function(d) {
	            b._onUploadAbort(this.upload._idd)
	        }
	    }
	    this.loader.upload._idd = c;
	    var a = new FormData();
	    a.append("t", t);
	    if (this.file_data[c].size == 0 && (navigator.userAgent.indexOf("MSIE") > 0 || navigator.userAgent.indexOf("Trident") > 0)) {
	        a.append("file_name", String(this.file_data[c].name));
	        a.append("zero_size", "1")
	    } else {
	        a.append(this.conf.param_name, this.file_data[c].file)
	    }
	    if (window.dhx4.ajax.cache != true) {
	        a.append("dhxr" + new Date().getTime(), "1")
	    }
	    
	    var url = this.conf.url;
	    if((this.conf.service && this.conf.service != null) && (this.conf.upload && this.conf.upload != null)){
	    	a.append("custom_class", this.conf.service);
	    	a.append("custom_method", this.conf.upload);
	    	//url = makeUrlParameter(url, {custom_class:this.conf.customClass, custom_method:this.conf.customUpMethod});
	    }
	    
	    this.loader.open("POST", url, true);
	    this.loader.setRequestHeader("X-Requested-With", "XMLHttpRequest");
	    if(typeof(sTokenString) != "undefined" && sTokenString){
	    	this.loader.setRequestHeader("s-token", sTokenString);
		}
	    this.loader.send(a)
	}).catch((message)=>{
		//AlertE(message);
		this._onUploadFail(c, message);
	})
}
//삭제이벤트 발생시
SepoaDhtmlXVaultObject.prototype.html5.prototype._removeFileFromQueue = function(e) {
	if (!this.file_data[e]) {
        return
    }
    var attach_key = this.conf.attach_key;
    var c = this.file_data[e].name;
    var d = (this.conf.uploaded_files != null && this.conf.uploaded_files[e] != null ? this.conf.uploaded_files[e].serverName : null);
    var b = {
        id: Number(e),
        name: c,
        size: this.file_data[e].size,
        serverName: d,
        uploaded: (this.file_data[e].state == "uploaded"),
        error: (this.file_data[e].state == "fail"),
        attach_key:attach_key
    };
    if (this.callEvent("onBeforeFileRemove", [b]) !== true) {
        return
    }
    var a = false;
    if (this.cf_loader_id != null || (this.conf.uploading && this.loader != null && e == this.loader.upload._idd && this.file_data[e].state == "uploading")) {
        this._uploadStop();
        a = true
    }
    
    var f = function(r){
    	console.log(r);
    }
    
    if(d != null){
    	sUtils.token.get("vault", {
    		action:"delete",
    		type:this.conf.type,
    		attach_key:this.conf.attach_key,
    		attach_seq:d
    	}).then((t)=>{
	    	let params = "&t="+t
			if((this.conf.service && this.conf.service != null) && (this.conf['delete'] && this.conf['delete'] != null) && (this.conf.select && this.conf.select != null)){
				params += "&custom_class=" + this.conf.service 
						 +"&custom_delete_method=" + this.conf['delete']
						 +"&custom_select_method=" + this.conf.select;
			}
	    	
	    	window.dhx4.ajax.post(this.conf.url, params, (r) => {
	        	r = JSON.parse(r.xmlDoc.responseText);
	        	if(!r.state){
	        		Alert('File delete fail');
	        	}else{
	        		this.file_data[e].file = null;
	        	    this.file_data[e].name = null;
	        	    this.file_data[e].size = null;
	        	    this.file_data[e].state = null;
	        	    this.file_data[e] = null;
	        	    delete this.file_data[e];
	        	    this._removeFileFromList(e);
	        	    this.conf.files_added = this.getData().length;
	        	    this.callEvent("onFileRemove", [b]);
	        	    this.callEvent('onUploadFile',[{attach_key:this.conf.attach_key, fileCount:(this.getData().length || 0)}, undefined,this.conf.param])
	        	    if (a) {
	        	        this._uploadStart();
	        	    }
	        	}
	        });
    	}).catch((message)=>{
    		AlertE(message);
    	})
    }else{
    	this._removeFileFromList(e);
    	delete this.file_data[e];
    }
}
//버그로인한수정
SepoaDhtmlXVaultObject.prototype.html5.prototype._unloadEngine = function() {
    this.buttons.browse.onclick = null;
    if (this.conf.dnd_enabled && this._unloadDND != null) {
        this._unloadDND()
    }
    this.f.onchange = null;
    //if문
    if(this.f.parentNode){
    	this.f.parentNode.removeChild(this.f);
    }
    this.f = null;
    if (this.loader) {
        this.loader.upload.onprogress = null;
        this.loader.onload = null;
        this.loader.onerror = null;
        this.loader.onabort = null;
        this.loader.upload._idd = null;
        this.loader = null
    }
    this._initEngine = null;
    this._doUploadFile = null;
    this._uploadStop = null;
    this._parseFilesInInput = null;
    this._addFileToQueue = null;
    this._removeFileFromQueue = null;
    this._unloadEngine = null
}
SepoaDhtmlXVaultObject.prototype.html5.prototype._initEngine= function() {
    var c = this;
    this.buttons.browse.onclick = function() {
        if (c.conf.enabled) {
            c.f.click()
        }
    };
    this.conf.progress_type = "percentage";
    this.conf.dnd_enabled = true;
    var a = window.navigator.userAgent;
    var b = true;
    if (a.match(/Windows/gi) != null && a.match(/AppleWebKit/gi) != null && a.match(/Safari/gi) != null) {
        if (a.match(/Version\/5\.1\.5/gi)) {
            this.conf.multiple_files = false
        }
        if (a.match(/Version\/5\.1[^\.\d{1,}]/gi)) {
            this.conf.dnd_enabled = false
        }
        if (a.match(/Version\/5\.1\.1/gi)) {
            this.conf.multiple_files = false;
            this.conf.dnd_enabled = false
        }
        if (a.match(/Version\/5\.1\.2/gi)) {
            this.conf.dnd_enabled = false
        }
        if (a.match(/Version\/5\.1\.7/gi)) {
            this.conf.multiple_files = false
        }
    }
    this._addFileInput();
}

//HTML4 모드
//form 생성
SepoaDhtmlXVaultObject.prototype.html4.prototype._addForm = function() {
    var a = this;
    if (!this.k) {
        this.k = document.createElement("DIV");
        this.k.className = "dhx_vault_file_form_cont";
        this.buttons.browse.appendChild(this.k);
        this.conf.fr_name = "dhx_vault_file_" + window.dhx4.newId();
        this.k.innerHTML = '<iframe name="' + this.conf.fr_name + '" style="height:0px;width:0px;" frameBorder="0"></iframe>';
        this.fr = this.k.firstChild;
//        IE9 에서 iframe.readyState 가 존재하지않아 삭제함.
//        if (window.navigator.userAgent.indexOf("MSIE") >= 0) {
//            this.fr.onreadystatechange = function() {
//                if (this.readyState == "complete") {
//                    a._onLoad()
//                }
//            }
//        } else {
            this.fr.onload = function() {
                a._onLoad()
            }
//        }
    }
    var b = document.createElement("DIV");
    b.innerHTML = "<form method='POST' enctype='multipart/form-data' target='" + this.conf.fr_name + "' class='dhx_vault_file_form' name='dhx_vault_file_form_" + window.dhx4.newId() + "'><input type='hidden' name='mode' value='html4'><input type='hidden' name='uid' value=''><input type='hidden' name='type' value=''><input type='hidden' name='attach_key' value=''><input type='hidden' name='attach_seq' value=''><input type='file' name='" + this.conf.param_name + "' class='dhx_vault_file_input'></form>";
    this.k.appendChild(b);
    b.firstChild.lastChild.onchange = function() {
        var d = this.value.match(/[^\/\\]*$/)[0];
        this.previousSibling.value = this._idd = window.dhx4.newId();
        var c = null;
        var e = null;
        if (this.files != null && this.files[0] != null) {
            c = this.files[0].lastModifiedDate || null;
            e = this.files[0].size || null
        }
        if (a._beforeAddFileToList(d, e, c) == true) {
            a._addFileToQueue(this);
            this.onchange = null;
            this.parentNode.parentNode.style.display = "none"
        }
        a._addForm()
    };
    b = null
}
//파일업로드
SepoaDhtmlXVaultObject.prototype.html4.prototype._doUploadFile = function(a) {
    if (this.file_data[a].custom == true) {
        this._cfUploadStart(a)
    } else {
        this.fr._idd = a;
        this.file_data[a].form.type.value = this.conf.type; 
        this.file_data[a].form.attach_key.value = this.conf.attach_key;
        this.file_data[a].form.attach_seq.value = this.conf.attach_seq;
        this.file_data[a].form.action = this.conf.url;
        this.file_data[a].form.restriction = this.conf.restriction;
        this.file_data[a].form.submit()
    }
}
//callback
SepoaDhtmlXVaultObject.prototype.html4.prototype._onLoad = function() {
    if (this.conf.uploading && this.fr._idd != null) {
        var a = window.dhx4.s2j(this.fr.contentWindow.document.body.innerHTML);
        if (a != null) {
            if (typeof(a.state) != "undefined") {
                if (a.state == "cancelled") {
                    this._onUploadAbort(this.fr._idd);
                    this.fr.contentWindow.document.body.innerHTML = "";
                    a = null;
                    return
                } else {
                    if (a.state == true) {
                        if (typeof(a.size) != "undefined" && !isNaN(a.size)) {
                            this.file_data[this.fr._idd].size = a.size
                        }
                        this._onUploadSuccess(this.fr._idd, a.name, null, a.extra, a.attch_key);
                        a = null;
                        return
                    }
                }
            }
        }
        this._onUploadFail(this.fr._idd, (a != null && a.extra != null ? a.extra : null))
    }
}
//파일삭제
SepoaDhtmlXVaultObject.prototype.html4.prototype._removeFileFromQueue = function(e) {
	if (!this.file_data[e]) {
        return
    }
    var attach_key = this.conf.attach_key;
    var c = this.file_data[e].name;
    var d = (this.conf.uploaded_files != null && this.conf.uploaded_files[e] != null ? this.conf.uploaded_files[e].serverName : null);
    var b = {
        id: Number(e),
        name: c,
        size: this.file_data[e].size,
        serverName: d,
        uploaded: (this.file_data[e].state == "uploaded"),
        error: (this.file_data[e].state == "fail"),
        attach_key:attach_key
    };
    if (this.callEvent("onBeforeFileRemove", [b]) !== true) {
        return
    }
    var a = false;
    if (this.cf_loader_id != null || (this.conf.uploading && this.loader != null && e == this.loader.upload._idd && this.file_data[e].state == "uploading")) {
        this._uploadStop();
        a = true
    }
    
    var that = this;
    var f = function(r){
    	r = JSON.parse(r.xmlDoc.responseText);
    	if(!r.state){
    		Alert('File delete fail');
    	}else{
    		that.file_data[e].file = null;
    	    that.file_data[e].name = null;
    	    that.file_data[e].size = null;
    	    that.file_data[e].state = null;
    	    that.file_data[e] = null;
    	    delete that.file_data[e];
    	    that._removeFileFromList(e);
    	    that.callEvent("onFileRemove", [b]);
    	    that.callEvent('onUploadFile',[{attach_key:that.conf.attach_key, fileCount:(that.getData().length || 0)}, undefined,that.conf.param])
    	    if (a) {
    	        that._uploadStart();
    	    }
    	}
    }
    
    //서버에 전송코드 추가
    if(d != null){
    	window.dhx4.ajax.post(this.conf.url, "mode=delete&type="+this.conf.type+"&attach_key="+this.conf.attach_key+"&attach_seq="+d+"&type="+this.conf.type, f);
    }
}
SepoaDhtmlXVaultObject.prototype._initEngine2 = function() {
	if(this.conf.engine=="html5"){
		var h = this;
		this.buttons.browse.onclick = function() {
            if (h.conf.enabled) {
                h.f.click()
            }
        };
        if (this.conf.dnd_enabled && this._initDND != null && this.conf.mode != "VI") {
            this._initDND()
        }
	}else if(this.conf.engine=="html4"){
		this.k = null;
		this._initEngine();
	}
}
//뷰모드에서 X버튼 삭제
SepoaDhtmlXVaultObject.prototype.list_default = function() {
    this.t = {};
    this.n = {};
    this.addFileItem = function(c, a) {
        var b = document.createElement("DIV");
        b._idd = c;
        a.appendChild(b);
        this.t[c] = b;
        b = a = null
    };
    this.isFileItemExist = function(a) {
        return (this.t[a] != null)
    };
    this.renderFileRecord = function(c, b) {
        var a = this.t[c];
        if (!a == null) {
            return
        }
        a.className = "dhx_vault_file dhx_vault_file_" + b.state;
        a.innerHTML = "<div class='dhx_vault_file_param dhx_vault_file_name'>&nbsp;</div><div class='dhx_vault_file_param dhx_vault_file_progress'>&nbsp;</div><div class='dhx_vault_file_param dhx_vault_file_delete'>&nbsp;</div><div class='dhx_vault_file_icon dhx_vault_" + b.icon + "'><div class='dhx_vault_all_icons'></div></div>";

        a.childNodes[2]._action = {
            id: c,
            data: "delete_file"
        };
        this.updateFileNameSize(c, b);
        if(b.mode == 'VI' || b.mode == 'NODEL'){
        	$(a).find('.dhx_vault_file_delete').hide();
        }
        a = null
    };
    this.removeFileRecord = function(b) {
        var a = this.t[b];
        if (a == null) {
            return
        }
        a._idd = null;
        a.childNodes[2]._action = null;
        a.parentNode.removeChild(a);
        a = null;
        this.n[b] = this.t[b] = null;
        delete this.t[b];
        delete this.n[b]
    };
    this.updateFileNameSize = function(f, d) {
        var c = this.t[f];
        if (c == null) {
            return
        }
        if (this.n[f] == null) {
            this.n[f] = {}
        }
        for (var b in {
                name: true,
                size: true,
                readableSize: true
            }) {
            if (d[b] != null) {
                this.n[f][b] = d[b]
            } else {
                d[b] = this.n[f][b]
            }
        }
        var e = d.name + (!isNaN(d.size) && d.size !== false ? " (" + d.readableSize + ")" : "&nbsp;");
        if (d.download == true) {
            e = "<a href='javascript:void(0);'>" + e + "</a>"
        }
        c.childNodes[0].innerHTML = "<div class='dhx_vault_file_name_text'>" + e + "</div>";
        c.childNodes[0].title = d.name + (!isNaN(d.size) && d.size !== false ? " (" + d.readableSize + ")" : "");
        if (d.download == true) {
            c.childNodes[0].childNodes[0].childNodes[0]._action = {
                id: f,
                data: "download_file"
            }
        }
        c = null
    };
    this.updateFileState = function(d, c) {
        var b = this.t[d];
        if (b == null) {
            return
        }
        var a = false;
        if (this.updateFileStateExtra != null) {
            a = this.updateFileStateExtra(d, c)
        }
        if (!a) {
            if (c.state == "added") {
                b.className = "dhx_vault_file dhx_vault_file_added";
                b.childNodes[1].className = "dhx_vault_file_param dhx_vault_file_progress";
                b.childNodes[1].innerHTML = "&nbsp;"
            }
            if (c.state == "fail") {
                b.className = "dhx_vault_file dhx_vault_file_fail";
                b.childNodes[1].className = "dhx_vault_file_param dhx_vault_file_progress";
                b.childNodes[1].innerHTML = c.str_error
            }
            if (c.state == "size_exceeded") {
                b.className = "dhx_vault_file dhx_vault_file_size_exceeded";
                b.childNodes[1].className = "dhx_vault_file_param dhx_vault_file_progress";
                b.childNodes[1].innerHTML = c.str_size_exceeded
            }
            if (c.state == "uploaded") {
                b.className = "dhx_vault_file dhx_vault_file_uploaded";
                b.childNodes[1].className = "dhx_vault_file_param dhx_vault_file_progress";
                b.childNodes[1].innerHTML = c.str_done
            }
            if (c.state == "uploading_html4" || c.state == "uploading") {
                b.className = "dhx_vault_file dhx_vault_file_uploading";
                b.childNodes[1].className = "dhx_vault_file_param dhx_vault_file_uploading";
                b.childNodes[1].innerHTML = "<div class='dhx_vault_progress'><div class='dhx_vault_progress_loader'>&nbsp;</div></div>"
            }
        }
        b = null
    };
    this.updateStrings = function() {};
    this.unload = function() {
        this.t = null
    }
};
//DND 상태에따른 알림변경
SepoaDhtmlXVaultObject.prototype._initDND = function() {
    var a = this;
    this.dnd = {};
    this._showDNDBox = function() {
        if (!this.conf.enabled) {
            return
        }
        if (!this.dnd.box) {
            this.dnd.box = document.createElement("DIV");
            this.dnd.box.className = "dhx_vault_dnd_box";
            this.base.appendChild(this.dnd.box);
            this.p_files.className = "dhx_vault_files dhx_vault_dnd_box_over";
            this.dnd.box.style.top = this.p_files.style.top;
            this.dnd.box.style.left = this.p_files.style.left;
            this.dnd.box.style.width = this.p_files.offsetWidth - (this.dnd.box.offsetWidth - this.dnd.box.clientWidth) + "px";
            this.dnd.box.style.height = this.p_files.offsetHeight - (this.dnd.box.offsetHeight - this.dnd.box.clientHeight) + "px";
            this.dnd.box.innerHTML = "<div class='dhx_vault_dnd_box_text' style='margin-top:" + Math.round(this.dnd.box.offsetHeight / 2 - 24) + "px;'>" + this.strings.dnd + "</div>";
            this.dnd.box.ondragenter = function(b) {
                a.dnd.last_node = b.target;
                if (!b.dataTransfer) {
                    return
                }
                try {
                    b.dataTransfer.effectAllowed = "copy";
                    b.dataTransfer.dropEffect = "copy"
                } catch (c) {}
                b.stopPropagation();
                b.preventDefault()
            };
            this.dnd.box.ondragover = function(b) {
                a.dnd.last_node = b.target;
                if (!b.dataTransfer) {
                    return
                }
                b.stopPropagation();
                b.preventDefault()
            };
            this.dnd.box.ondrop = function(b) {
                if (!b.dataTransfer) {
                    return
                }
                try {
                    b.dataTransfer.effectAllowed = "copy";
                    b.dataTransfer.dropEffect = "copy"
                } catch (c) {}
                b.stopPropagation();
                b.preventDefault();
                if (b.dataTransfer.files.length > 0) {
                    a._parseFilesInInput(b.dataTransfer.files)
                } else {
                    a.callEvent("_onNodeDrop", [b.dataTransfer])
                }
                a._hideDNDBox()
            }
        }
    };
    this._hideDNDBox = function() {
        if (this.dnd.box != null) {
            this.dnd.box.ondragenter = null;
            this.dnd.box.ondragover = null;
            this.dnd.box.ondrop = null;
            this.dnd.box.parentNode.removeChild(this.dnd.box);
            this.dnd.box = null;
            this.p_files.className = "dhx_vault_files";
            this.dnd.last_node = null
        }
    };
    this.dnd.last_node = null;
    this._doOnWinDragEnter = function(b) {
    	if(a.conf.dnd_enabled && a.conf.mode != "VI"){
	        a.dnd.last_node = b.target;
	        a._showDNDBox()
    	}
    };
    this._doOnWinDragLeave = function(b) {
        if (a.dnd.last_node == b.target) {
            window.setTimeout(function() {
                a._hideDNDBox()
            }, 1)
        }
    };
    window.addEventListener("dragenter", this._doOnWinDragEnter, false);
    window.addEventListener("dragleave", this._doOnWinDragLeave, false);
    this._doOnWinDragOver = function(b) {
        if (!b.dataTransfer) {
            return
        }
        try {
            b.dataTransfer.effectAllowed = "none";
            b.dataTransfer.dropEffect = "none"
        } catch (c) {}
        b.stopPropagation();
        b.preventDefault()
    };
    this._doOnWinDrop = function(b) {
        if (!b.dataTransfer) {
            return
        }
        b.stopPropagation();
        b.preventDefault();
        a._hideDNDBox()
    };
    window.addEventListener("dragover", this._doOnWinDragOver, false);
    window.addEventListener("drop", this._doOnWinDrop, false);
    this._unloadDND = function() {
        window.removeEventListener("dragenter", this._doOnWinDragEnter, false);
        window.removeEventListener("dragleave", this._doOnWinDragLeave, false);
        window.removeEventListener("dragover", this._doOnWinDragOver, false);
        window.removeEventListener("drop", this._doOnWinDrop, false);
        for (var b in this._dndNodesData) {
            if (this._dndNodesData[b].inst_id == this.conf.inst_id) {
                this.removeDraggableNode(this._dndNodesData[b].node)
            }
        }
        this._hideDNDBox();
        this._showDNDBox = null;
        this._hideDNDBox = null;
        this._doOnWinDragEnter = null;
        this._doOnWinDragLeave = null;
        this._doOnWinDragOver = null;
        this._doOnWinDrop = null;
        this._initDND = null;
        this._unloadDND = null;
        this.dnd = null;
        a = null
    }
};
//DND 상태변경
SepoaDhtmlXVaultObject.prototype.setDragNDrop = function(flag){
	this.conf.dnd_enabled = flag;
}
if (typeof(window.dhtmlXCellObject) == "function" && typeof(dhtmlXCellObject.prototype.attachVault) == "undefined") {
    dhtmlXCellObject.prototype.attachVault = function(a) {
        var b = document.createElement("DIV");
        b.style.position = "relative";
        b.style.width = "100%";
        b.style.height = "100%";
        b.style.overflow = "hidden";
        this._attachObject(b);
        b._attach_mode = true;
        b._no_border = true;
        if (typeof(window.dhtmlXWindowsCell) == "function" && this instanceof window.dhtmlXWindowsCell) {
            b._no_border = false
        }
        if (typeof(a) != "object" || a == null) {
            a = {}
        }
        a.parent = b;
        if (typeof(a.skin) == "undefined") {
            a.skin = this.conf.skin
        }
        this.dataType = "vault";
        this.dataObj = new dhtmlXVaultObject(a);
        if (typeof(window.dhtmlXLayoutCell) == "function" && this instanceof window.dhtmlXLayoutCell) {
            this.layout._getMainInst().attachEvent("onExpand", function(d) {
                for (var e = 0; e < d.length; e++) {
                    var c = this.cells(d[e]);
                    if (c.dataType == "vault" && c.dataObj != null) {
                        c.dataObj.setSizes()
                    }
                    c = null
                }
            })
        }
        a.parent = null;
        a = b = null;
        return this.dataObj
    }
}
//다른컴포넌트에 attach 동작 커스텀
if (typeof(window.dhtmlXCellObject) == "function") {
    dhtmlXCellObject.prototype.attachVault = function(data) {
    	var a = new Object();
        var b = document.createElement("DIV");
        b.style.position = "relative";
        b.style.width = "100%";
        b.style.height = "100%";
        b.style.overflow = "hidden";
        this._attachObject(b);
        b._attach_mode = true;
        b._no_border = true;
        if (typeof(window.dhtmlXWindowsCell) == "function" && this instanceof window.dhtmlXWindowsCell) {
            b._no_border = false
        }
        if (typeof(a) != "object" || a == null) {
            a = {}
        }
        a.parent = b;
        if (typeof(a.skin) == "undefined") {
            a.skin = this.conf.skin
        }
        this.dataType = "vault";
        
        var options = $.extend(true, a, data);
        options.container=null;
        
        this.dataObj = new SepoaDhtmlXVaultObject(options);
        if (typeof(window.dhtmlXLayoutCell) == "function" && this instanceof window.dhtmlXLayoutCell) {
            this.layout._getMainInst().attachEvent("onExpand", function(d) {
                for (var e = 0; e < d.length; e++) {
                    var c = this.cells(d[e]);
                    if (c.dataType == "vault" && c.dataObj != null) {
                        c.dataObj.setSizes()
                    }
                    c = null
                }
            })
        }
        a.parent = null;
        a = b = null;
        return this.dataObj
    }
}