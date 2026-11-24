SepoaButton=function(){
	sUtils.ux.extendModule(this, 'EventManager');
	this._objPulls={};
}
SepoaButton.prototype.addButton=function(vp, options){
	if(vp.div) {
		this._objPulls[vp.id] = new SepoaButtonItem(this, vp.div, vp);
	} else {
		this._objPulls[options.id] = new SepoaButtonItem(this, vp, options);
	}
}
SepoaButton.prototype.forEachBtn=function(f){
	for(var idx in this._objPulls){
		f.apply(this, [idx]);
	}
}
SepoaButton.prototype.btn=function(c){
	return this._objPulls[c]; 
}
SepoaButton.prototype.showButton=function(c, flag){
	if(!c){
		this.forEachBtn(function(id){
            this.btn(id)[flag===false?"setHide":((flag=="disable")?"setDisable":"setShow")]();
        });
		return;
	}
	c = c.split(',');
	for(var i in c){
		var t = this._objPulls[c[i]];
		if(t){
			t[flag===false?"setHide":((flag=="disable")?"setDisable":"setShow")]();
		}
	}
}

SepoaButtonItem=function(parent, vp, options){
	sUtils.ux.extendModule(this, 'EventManager');
	this._parent = parent;
	this._vp = vp;
	this._area={};
	this._config={};
	this._drawBase();
	this._parseOptions(options);
}
SepoaButtonItem.prototype._drawBase=function(){
	this._area.base = this._vp.appendChild($('<div class="sepoa-button-item"><div class="sepoa-button-item-area-left"></div><div class="sepoa-button-item-area-label"></div><div class="sepoa-button-item-area-right"></div></div>')[0]);
	this._area.left = this._area.base.children[0];
	this._area.label = this._area.base.children[1];
	this._area.right = this._area.base.children[2];
	
	this._area.base.addEventListener('click', function(){
		if(this._config.toggle){
			this.toggle();
		}
		if(this._config.fn){
			this._config.fn();
		}
	}.bind(this));
}
SepoaButtonItem.prototype._parseOptions=function(options){
	for(var o in this._fn){
		this._fn[o].apply(this, [options[o]]);
	}
}
SepoaButtonItem.prototype._fn={
	type:function(value){
		if(!value){
			value = "default"
		}
		this._area.base.classList.add('sepoa-button-item-type-'+value.toLowerCase());
	},
	size:function(value){
		if(!value){
			value = "m"
		}
		this._area.base.classList.add('sepoa-button-item-size-'+value.toLowerCase());
	},
	reverse:function(flag){
		if(flag === true){
			this._area.base.classList.add('sepoa-button-item-outline');
		}
	},
	toggle:function(flag){
		if(flag === true){
			this._area.base.classList.add('sepoa-button-item-toggle');
			this._config.toggle={
				status:false
			};
		}
	},
	template:function(value){
		if(value != null && value != ""){
			this.setTemplate(value);
		}
	},
	text:function(value){
		if(value != null && value != ""){
			this.setText(value);
		}
	},
	onclick:function(fn){
		if(fn){
			this._config.fn = fn;
		}
	},
	img:function(type){
		if(type){
			this.setIcon(type);
		}
	},
	position:function(pos){
		if(pos){
			this.setPosition(pos);
		}
	}
}
SepoaButtonItem.prototype.setShow=function(){
	this._area.base.classList.remove('sepoa-button-item-hide');
	this._parent.callEvent('ondisplaychange', []);
}
SepoaButtonItem.prototype.setHide=function(){
	this._area.base.classList.add('sepoa-button-item-hide');
	this._parent.callEvent('ondisplaychange', []);
}
SepoaButtonItem.prototype.setTemplate=function(value){
	value = value.replace(/^\{([a-z0-9_-]{1,}).([a-z0-9_-]{1,})\}$/gi, function() {
		return Sepoa.cache.text.get(arguments[1]+"."+arguments[2]);
    })
	this._config.template=value;
}
SepoaButtonItem.prototype.setText=function(value){
	if(value == undefined){
    	value = "";
    }
	switch(typeof(value)){
	case "number":
		value += "";
	case "string":
		value = {TEXT:value.replace(/\{([a-z0-9_-]{1,}).([a-z0-9_-]{1,})\}/gi, function() {
			return Sepoa.cache.text.get(arguments[1]+"."+arguments[2]);
	    })};
		break;
	case "object":
		break;
	default:
		value = {TEXT:""};
		break;
	}
	if(this._config.template){
		value = {TEXT:sUtils.string.dataReplacer(this._config.template, value)};
	}
	this._area.label.innerHTML = sUtils.string.dataReplacer('<div class="sepoa-button-item-label">$.{TEXT}</div>', value);
	this._area.label.classList.add("sepoa-button-item-area-show");
}
SepoaButtonItem.prototype.toggle=function(flag){
	this._config.toggle.status = flag;
	if(flag){
		this._area.base.classList.add("sepoa-button-item-toggle-on");
	}else{
		this._area.base.classList.remove("sepoa-button-item-toggle-on");
	}
}
SepoaButtonItem.prototype.setIcon=function(icon){
	if(icon){
		this._area.left.className = "sepoa-button-item-area-left xi-sepoa-dummy "+icon;
		this._area.right.className = "sepoa-button-item-area-right xi-sepoa-dummy "+icon;
		this.setSize('S');
	}
}
SepoaButtonItem.prototype.setPosition=function(pos){
	if(pos && this._area[pos]){
		this._area[pos].style.display="inherit";
	}
}
SepoaButtonItem.prototype.setSize=function(s){
	if(s){
		this._area.base.classList.remove("sepoa-button-item-size-s");
		this._area.base.classList.remove("sepoa-button-item-size-m");
		this._area.base.classList.remove("sepoa-button-item-size-l");
		
		this._area.base.classList.add("sepoa-button-item-size-"+s.toLowerCase());
	}
}
SepoaButtonItem.prototype.setBadge=function(s){
	if(!this._area.badge){
		this._area.badge = this._area.base.appendChild($('<div class="sepoa-button-item-area-badge"></div>')[0]);
	}
	var template = "";
	if(!s || s == "0"){
		this._area.badge.classList.remove("show");
	}else{
		if(!isNaN(parseInt(s))){
			this._area.badge.innerHTML = sUtils.string.dataReplacer('<div class="sepoa-button-badge-default"><span>$.{BADGE}</span></div>', {BADGE:s});
		}else if (typeof(s) == "string"){
			this._area.badge.innerHTML = sUtils.string.dataReplacer('<div class="sepoa-button-badge-icon xi-sepoa-dummy xi-$.{ICON}"></div>', {ICON:s.toLowerCase()});
		}else {
			return;
		}
		this._area.badge.classList.add("show");
	}
}

SepoaButtonItem.prototype.setReverse = SepoaButtonItem.prototype.toggle;
SepoaButtonItem.prototype.setColor=function(){}
SepoaButtonItem.prototype.setTextColor=function(){}
SepoaButtonItem.prototype.setBorderColor=function(){}