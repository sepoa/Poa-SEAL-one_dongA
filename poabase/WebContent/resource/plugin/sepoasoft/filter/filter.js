SepoaFilterObject = function(_cont, _conf, sepoa){
	this.cont = (typeof(_cont)=="string")?document.getElementById(_cont):_cont;
	if(!this.cont) return;
	this.conf = {
		height:_conf.height || 28
	};
	this.area={
		sfilter_keyword:true,
		sfilter_button:false,
		sfilter_blank:true,
		sfilter_calendar:true,
		sfilter_filter:true,
		sfilter_status:true
	}
	this.filterPull={};
	this.btnPull={};
	window.dhtmlx.extend(this, dhtmlx.EventSystem);
	this._replaceInfoData(_conf);
	this.SepoaObject = sepoa;
	this.init(_conf);
};
SepoaFilterObject.prototype.getFilterData=function(){
	var result = {};
	for(var idx in this.filterPull){
		var d = this.filterPull[idx].getFilterData();
		for(var _i in d){
			result[_i] = d[_i];
		}
	}
	return result;
}
SepoaFilterObject.prototype.setShow=function(type, flag){
	var t = this.filterPull[type.toLowerCase()];
	flag = dhx.s2b(flag);
	if(t){
		switch(type.toLowerCase()){
			case 'keyword':
				t.setShow(flag);
				break;
			case 'calendar':
				t.setShow(flag);
			default:
				if(flag)
					this.btnPull[type].setShow()
				else 
					this.btnPull[type].setHide()
		}
	}
}
SepoaFilterObject.prototype.reloadCombo=function(id,combo){
	var t = this.filterPull.filter.items[id];
	if(!t){
		return;
	}
	t.combo.init=combo;
	t.combo.data=null;
	t.callreloadevent=true;
	this.filterPull.filter.clear();
}
SepoaFilterObject.prototype._drawbasic=function(){
	var t = this.cont
	t.innerHTML = "";
	var h = t.offsetHeight-2;
	t.style.lineHeight = h+'px';
	t.style.height = "auto";
	//this.conf.height = h;
	appendClassName(t, "sfilter_cont");
	
	for(idx in this.area){
		var a = document.createElement('div');
		appendClassName(a, idx);
		t.appendChild(a);
		a.offsetHeight = this.cont.height;
		if(!this.area[idx]){
			a.style.display="none";
		}
		this.area[idx] = a;
	}
};
SepoaFilterObject.prototype._replaceInfoData=function(c){
	 for(var idx in c){
		 if(['default','p_value','p_description'].indexOf(idx)+1){
			 c[idx] = c[idx].replace(/\$\{([.a-z0-9_-]{1,})(\|([^\}]*))?\}/gi, function() {
		    	if(Sepoa.cache.text.get(arguments[1])){
		    		return Sepoa.cache.text.get(arguments[1]);
		    	}
		    	return "";
			 });
			 c[idx] = c[idx].replace(/\P\{([.a-z0-9_-]{1,})(\|([^\}]*))?\}/gi, function() {
		    	if(Sepoa.request[arguments[1]]){
		    		return Sepoa.request[arguments[1]];
		    	}
		    	return "";
			 });
			 c[idx] = c[idx].replace(/\{([.a-z0-9_-]{1,})(\|([^\}]*))?\}/gi, function() {
		    	if(Sepoa.info[arguments[1]]){
		    		return Sepoa.info[arguments[1]];
		    	}
		    	return "";
			 });
		 }else if(typeof c[idx] == 'object'){
			 this._replaceInfoData(c[idx]);
		 }
	 }
}
SepoaFilterObject.prototype.init=function(c){
	this._drawbasic();
	var l={keyword:"search",filter:"filter",status:"status",calendar:false};
	for(type in l){
		if(c[type]){
			this.filterPull[type]=new this.items[type](this, c[type],this.area['sfilter_'+type]);
			if(l[type]){
				this.btnPull[l[type]]=new this.items.button(this, l[type], this.filterPull[type]);
			}
		}else{
			if(type == 'calendar'){
				this.area.sfilter_blank.style.display="none";
			}
			this.area['sfilter_'+type].style.display="none";
		}
	}
	var that = this;
	this.attachEvent("onClick",function(id, o){
		if(id == "modal"){
			that.hideModal(o);
		}
	});
};
SepoaFilterObject.prototype.showModal=function(obj){
	SUX.setOverFrameClickDummy(this, obj, true);
}
SepoaFilterObject.prototype.hideModal=function(t){
	if(t){
		SUX.releaseOverFrameClickDummy(t);
		t.setHide(true);
	}
}
SepoaFilterObject.prototype.items={};
SepoaFilterObject.prototype.items.keyword=function(p, c, v){
	this.cont=v;
	this.filterObj= p;
	this.conf=c;
	this.render();
	this.value="";
	this.isDisable=false;
};
SepoaFilterObject.prototype.items.keyword.prototype.setShow=function(flag){
	this.cont.style.display=flag?"":"none";
}
SepoaFilterObject.prototype.items.keyword.prototype.render=function(){
	this.input = document.createElement('input');
	appendClassName(this.input, "keyword");
	this.input.style.height = this.filterObj.conf.height+'px';
	this.input.style.width = (this.conf.width+"px") || 'auto';
	this.cont.appendChild(this.input);
	this.input.placeholder = this.conf.placeholder || this.filterObj.lang.KEYWORD_PLACEHOLDER ||"Search";
	this.setPlaceholder=function(a){
		this.input.placeholder = a;
	};
	this.input.keywordObj = this;
	$(this.input).on({
		keydown:function(e){
			if(e.which == 13){
				this.keywordObj.filterObj.callEvent('onSearch', ['search']);
			}
		}
	})
}
SepoaFilterObject.prototype.items.keyword.prototype.getFilterData=function(){
	var result = {};
	if(this.input.value.trim()){
		result[this.conf.id] = this.input.value;
	}
	return result;
}
SepoaFilterObject.prototype.items.keyword.prototype.setValue=function(a){
	if(this.input) this.input.value=a;
	else this.value = a;
}
SepoaFilterObject.prototype.items.keyword.prototype.getValue=function(a){
	return (this.input)?this.input.value:this.value;
}
SepoaFilterObject.prototype.items.keyword.prototype.setDisable=function(f){
	if(this.input) this.input.disable = f;
	this.isDisable(f);
}
SepoaFilterObject.prototype.items.button=function(p, c, o){
	this.filterObj= p;
	this.conf={id:c};
	this.render();
	if(o) this.linkedObj = o;
	this.isDisable=false;
};
SepoaFilterObject.prototype.items.button.prototype.render=function(){
	this.cont = document.createElement('div');
	appendClassName(this.cont, "sfilter_img");
	appendClassName(this.cont, this.conf.id);
	if(this.conf.id == 'search'){
		this.filterObj.area["sfilter_keyword"].appendChild(this.cont);
	}else{
		this.filterObj.area["sfilter_button"].style.display="";
		this.filterObj.area["sfilter_button"].appendChild(this.cont);
	}
	this.cont.style.height = this.filterObj.conf.height+"px";
	this.cont.innerHTML = '<div class="sfilter_img_text_area" style="height:28px"><span>'+this.filterObj.lang[this.conf.id.toUpperCase()]+'</span></div>'
	this.cont.filterBtnObj = this;
	var o = this.cont;
	$(o).on({
		click:function(){
			if(!o.isDisable){
				var l = this.filterBtnObj.linkedObj;
				if(l && l._btnTrigger) l._btnTrigger(this.filterBtnObj);
				if(this.filterBtnObj.conf.id == 'search'){
					this.filterBtnObj.filterObj.callEvent('onSearch', ['search']);
				}else{
					this.filterBtnObj.filterObj.callEvent('onButtonClick', [this.filterBtnObj.conf.id]);
				}
			}
		}
	})
}
SepoaFilterObject.prototype.items.button.prototype.setDisable=function(f){
	this.isDisable(f);
}
SepoaFilterObject.prototype.items.button.prototype.setHide=function(){
	appendClassName(this.cont, "sfilter_hidden");
}
SepoaFilterObject.prototype.items.button.prototype.setShow=function(){
	removeClassName(this.cont, "sfilter_hidden");
}
SepoaFilterObject.prototype.items.filter=function(p, c, v){
	this.filterObj= p;
	
	if(!this.filterObj.filterViewId){
		this.filterObj.filterViewId = 'filter_filterView'+SUX.getModuleTop().dhx.newId()
	}
	var v_id = this.filterObj.filterViewId;
	var b_data = SUX.getModuleTop().document.getElementById(v_id);
	if(b_data){
		$(b_data).remove()
	}
	b_data = null;
	
	var vp = SUX.getModuleTop().document.getElementById('sfilter_moduleTop_area');
	if(!vp){
		vp = document.createElement('div');
		vp.className = 'sfilter_cont';
		vp.style.position='inherit';
		SUX.getModuleTop().document.body.appendChild(vp);
	}
	vp.appendChild(v);
	v.id=v_id;
	
	var cont = this.cont = v;
	this.conf=c;
	this.items = {};
	this.render();
	this.isDisable=false;
	this._btnTrigger=this.toggleView;
	Sepoa.attachEvent("onWindowResize",function(){
		var hh = (window.document.body.offsetHeight/2);
		var sh = cont.scrollHeight;
		cont.style.height = hh+'px';
		if(sh < hh*1.1){
			cont.style.height='auto';
			cont.style.overflow='auto';
		}
	}, true)
}
SepoaFilterObject.prototype.items.filter.prototype._onHeaderButton=function(id){
	if(id == 'QUERY'){
		this.filterObj.callEvent('onSearch', ['filter']);
		this.setHide();
	}else if(id == 'CLEAR'){
		this.clear();
	}else if(id == 'CLOSE'){
		this.setHide();
	}
}
SepoaFilterObject.prototype.items.filter.prototype.render=function(){
	this.setHide();
	this.cont.innerHTML = "<table class='sfilter_filter_list'><tr><td class='sfilter_filter_header_title'>"+this.filterObj.lang.FILTER+"</td><td class='sfilter_filter_header_title_btn'><div class='sfilter_filter_btn_div'></td></tr><tr><td colspan='2' class='sfilter_filter_header_btn'><div class='sfilter_filter_btn_div'></div></td></tr></table>";
	
	var btnCont = this.cont.getElementsByClassName('sfilter_filter_btn_div')[1];
	
	var btn = this.btnObj=new SepoaButtonObject();
	btn.addButton({div:btnCont,id:"QUERY",text:"{BUTTON.search}",type:'filter_query', onclick:this._onHeaderButton, target:this});
	btn.addButton({div:btnCont,id:"CLEAR",text:"{BUTTON.initialization}",type:'filter_clear', onclick:this._onHeaderButton, target:this});
	
	btnCont = this.cont.getElementsByClassName('sfilter_filter_btn_div')[0];
	btn.addButton({div:btnCont,id:"CLOSE",type:'close', onclick:this._onHeaderButton, target:this});
	
	var table = this.cont.firstChild.lastChild;	
	for(var idx in this.conf){
		var t = this.conf[idx];
		this.items[t.id] = {
			filterObj:this,
			id:t.id,
			type:t.type,
			combo:{init:t.combo},
			popup:{
				ms         : (t.p_multiselect == "true"),
				code       : t.p_code,
				value      : t.p_value,   
				description: t.p_description,
				width      : t.p_width  || null,
				height     : t.p_height || null,
				url        : t.p_url    || null,
				title      : t.p_title  || null,
				param      : t.p_param  || null
			},
			default:t.default,
			notnull:t.notnull,
			data:{}
		};
		
		var tr = document.createElement('tr');
		appendClassName(tr, "sfilter_filter_tr");
		table.appendChild(tr);
		
		var label = document.createElement('td');
		appendClassName(label, "sfilter_filter_td");
		appendClassName(label, "sfilter_filter_td_label");
		tr.appendChild(label);
		label.innerHTML="<span class='sfilter_filter_list_label_text' filter_id='"+t.id+"'>"+Sepoa.cache.text.get((this.filterObj.SepoaObject || Sepoa).common.SCREEN_ID+'.'+t.id)+"</span>"
		
		var option = document.createElement('td');
		appendClassName(option, "sfilter_filter_td");
		appendClassName(option, "sfilter_filter_td_option");
		tr.appendChild(option);
		
		var optionUl = document.createElement('ul');
		appendClassName(optionUl, 'sfilter_filter_ul')
		option.appendChild(optionUl);
		
		this.items[t.id].ul = optionUl;
		
		var p = this.items[t.id];
		var d = (p.default)?p.default.split(','):[undefined];
		for(var _d in d){
			if( !(['select','popup'].indexOf(p.type)+1) || !(['select','popup'].indexOf(p.type)+1 && !d[_d])){
				this._addOption(p, d[_d]);
			}
		}
		if(['select','popup'].indexOf(p.type)+1){
			this._addOption(p);
		}
	}
}
SepoaFilterObject.prototype.items.filter.prototype.clear=function(){
	var targetList = $(this.cont).find('.sfilter_filter_li');
	targetList.remove();
	
	for(var idx in this.items){
		var p = this.items[idx];
		p.data = {};
		var d = (p.default)?p.default.split(','):[undefined];
		for(var _d in d){
			if( !(['select','popup'].indexOf(p.type)+1) || !(['select','popup'].indexOf(p.type)+1 && !d[_d])){
				this._addOption(p, d[_d]);
			}
		}
		if(['select','popup'].indexOf(p.type)+1){
			this._addOption(p);
		}
	}
}
SepoaFilterObject.prototype.items.filter.prototype._addOption=function(p, d){
	var optionLi = document.createElement('li');
	appendClassName(optionLi, 'sfilter_filter_li')
	
	if(p.ul.firstChild && p.type != 'range') p.ul.insertBefore(optionLi, p.ul.firstChild);
	else  p.ul.appendChild(optionLi);
	
	optionLi.id = new Date().getTime()+dhx4.newId();
	
	if(p.type == 'text'){
		var input = document.createElement('input');
		appendClassName(input, 'sfilter_filter_input')
		optionLi.appendChild(input);
		
		p.data[optionLi.id] = input;
		p.activeItem = input;
		
		var button = document.createElement('div');
		appendClassName(button, 'sfilter_filter_button');
		appendClassName(button, 'sfilter_filter_button_blank');
		optionLi.appendChild(button);
		
		button.targetProperties = p;
		
		p.activeItem.value = d||"";
	}else if(p.type == 'popup'){
		appendClassName(optionLi, 'sfilter_filter_popup');
		if(p.popup.ms){
			appendClassName(optionLi, 'sfilter_filter_popup_multi');
		}		
		
		var inputText = document.createElement('input');
		appendClassName(inputText, 'sfilter_filter_popup_text')
		optionLi.appendChild(inputText);
		inputText.disabled=true;
		
		var inputCode = document.createElement('input');
		appendClassName(inputCode, 'sfilter_filter_popup_code')
		optionLi.appendChild(inputCode);	

		p.data[optionLi.id] = {
			code:inputCode,
			text:inputText
		};
		p.activeItem = optionLi.id;
		
		var btn = document.createElement('div');
		appendClassName(btn, 'sfilter_filter_popup_btn')
		optionLi.appendChild(btn);
		btn.targetProperties = p;
		
		if(d){
			if('string' == typeof(d)){
				d = {
					CODE:d.split('#')[0]
				   ,TEXT:d.split('#')[1]
				}
			}
			
			inputText.value = d.TEXT;
			inputCode.value = d.CODE;
			
			appendClassName(optionLi, 'sfilter_filter_readonly');
			
			var span = document.createElement('span');
			span.className="sfilter_filter_select";
			span.innerText = inputText.value;
			optionLi.appendChild(span);
			
			var button = document.createElement('div');
			appendClassName(button, 'sfilter_filter_button');
			appendClassName(button, 'sfilter_filter_button_delete');
			optionLi.appendChild(button);
			button.targetProperties = p;
			
			$(button).on({
				click:this._buttonClick
			});
		}
		
		$(btn).on({
			click:function(){
				var tp = this.targetProperties;
				var tpp = tp.popup;
				
				if('string' == typeof(tpp.value)){
					tpp.value = tpp.value.split('#') 
				}
				if('string' == typeof(tp.description)){
					tpp.description = tpp.description.split('#');
				}
				window.filterPopupCallbackFunction=function(){
					if(tp.popup.ms){
						var data = arguments[0];
						var addCnt = 0;
						
						for(var i=0 ; i<data.length ; i++){
							var filterDataList = tp.data;
							
							var passFlag = false;
							for(var _idx in filterDataList){
								if(filterDataList[_idx].code.value == data[i][0]){
									passFlag = true;
									break;
								}
							}
							
							if(!passFlag){
								if(addCnt == 0){
									addCnt++;
									var li = tp.data[tp.activeItem].code.parentNode;
									appendClassName(li, 'sfilter_filter_readonly');
									
									tp.data[tp.activeItem].code.value = data[i][0];
									tp.data[tp.activeItem].text.value = data[i][1];
										
									var span = document.createElement('span');
									span.className="sfilter_filter_select";
									span.innerText = tp.data[tp.activeItem].text.value;
									li.appendChild(span);
									
									var button = document.createElement('div');
									appendClassName(button, 'sfilter_filter_button');
									appendClassName(button, 'sfilter_filter_button_delete');
									li.appendChild(button);
									
									button.targetProperties = p;
									
									$(button).on({
										click:tp.filterObj._buttonClick
									});
								}else{
									tp.filterObj._addOption(tp, {CODE:data[i][0], TEXT:data[i][1]});
								}
							}
						}
						if(addCnt){
							tp.filterObj._addOption(tp);
						}
					}else{
						tp.data[tp.activeItem].code.value = arguments[0];
						tp.data[tp.activeItem].text.value = arguments[1];
					}					
				}
				
				PopupCommonPost(tpp.code, filterPopupCallbackFunction,tpp.value,tpp.description,tpp.width,tpp.height,tpp.url,tpp.title,tpp.param)
			}
		});
	}else if(p.type == 'select'){
		var select = document.createElement('select');
		appendClassName(select, 'sfilter_filter_select');
		optionLi.appendChild(select);
		
		select.filterProperties = p;
		
		p.data[optionLi.id] = select;
		p.activeItem = select;
		
		var button = document.createElement('div');
		appendClassName(button, 'sfilter_filter_button');
		appendClassName(button, 'sfilter_filter_button_blank');
		
		optionLi.appendChild(button); 
		
		button.targetProperties = p;
		
		$(button).on({
			click:this._buttonClick
		});
		
		if(!p.combo.data){
			select.id = "sfilter_"+dhx.newId();
			select.ready = false;
			select.defaultValue = d;
			select.notnull = p.notnull;
			this.loadCombo(p, d, select);
		}else{
			select.ready = true;
			
			var beforeSelect = null;
			//if(!d && optionLi.nextSibling){
			//	var st = p.data[optionLi.nextSibling.id];
			//	beforeSelect = st.childNodes[st.selectedIndex].value;
			//}
			
			for(var idx in p.combo.data.data){
				var o = p.combo.data.data[idx];
				var option = document.createElement('option');
				option.value = o.value;
				option.innerText = o.text;
				if(d && option.value == d){
					 option.selected = true;
				}else if(!d && beforeSelect == option.value){
					option.selected = true;
				}
				p.activeItem.appendChild(option) ;
			}
		}
		
		$(select).change(function(){			
			var p = this.filterProperties;
			if(!this.childNodes[this.selectedIndex].value){
				return;
			}else{
				var t = p;
				var data = [];
				for(var idx in t.data){
					try{
						if(t.data[idx] == this){							
							continue;
						}
						if(t.data[idx].ready){
							var so = t.data[idx].options[t.data[idx].selectedIndex].value;
							if(so)
								data.push(so);
						}else{
							var so = t.data[idx].defaultValue;
							if(so)
								data.push(so);
						}
					}catch(e){}
				}
				
				if(data.indexOf(this.childNodes[this.selectedIndex].value)+1 != 0){
					p.activeItem.selectedIndex = 0;
					
					return;
				}
				
				var li = p.activeItem.parentNode;
				appendClassName(li, 'sfilter_filter_readonly');
				var btn = li.getElementsByClassName('sfilter_filter_button')[0];
				removeClassName(btn, 'sfilter_filter_button_blank');
				appendClassName(btn, 'sfilter_filter_button_delete');
				p.activeItem.disabled = true;
				
				var span = document.createElement('span');
				span.className="sfilter_filter_select";
				span.innerText = this.childNodes[this.selectedIndex].text;
				
				li.insertBefore(span, this);
				
				p.filterObj._addOption(p);
			}
		});
		
		if(d){
			var li = p.activeItem.parentNode;
			appendClassName(li, 'sfilter_filter_readonly');
			var btn = li.getElementsByClassName('sfilter_filter_button')[0];
			removeClassName(btn, 'sfilter_filter_button_blank');
			appendClassName(btn, 'sfilter_filter_button_delete');
			p.activeItem.disabled = true;
			
			var span = document.createElement('span');
			span.className="sfilter_filter_select";
			if(!select.ready){
				select.readyEvent=function(){
					span.innerText = select.childNodes[select.selectedIndex].text;
				}
			}else{
				span.innerText = select.childNodes[select.selectedIndex].text;
			}
			
			li.insertBefore(span, select);
		}
	}else if(p.type == 'calendar'){
		var calendar = document.createElement('div');
		optionLi.appendChild(calendar);
		var obj = new this.filterObj.items.calendar(this, {}, calendar);
		obj.mode = 'filterwindow'
		if(p.default){
			var now = new Date();
			
			var from, to;
			
			if(p.default.toLowerCase().indexOf('year') == 0){
				if(p.default.indexOf(':')){
					var adjust = p.default.split(':')[1];
					if(adjust > 0){
						from = new Date(now.getFullYear(), now.getMonth(), now.getDate());
						to = new Date(now.getFullYear()+parseInt(adjust), now.getMonth(), now.getDate());
					}else if(adjust < 0){
						from = new Date(now.getFullYear()+parseInt(adjust), now.getMonth(), now.getDate());
						to = new Date(now.getFullYear(), now.getMonth(), now.getDate());
					}else{
						from = new Date(now.getFullYear(), 0, 1);
						to = new Date(now.getFullYear()+1, 0, 0);
					}
				}else{
					from = new Date(now.getFullYear(), 0, 1);
					to = new Date(now.getFullYear()+1, 0, 0);
				}
			}else if(p.default.toLowerCase().indexOf('month') == 0){
				if(p.default.indexOf(':')){
					var adjust = p.default.split(':')[1];
					if(adjust > 0){
						from = new Date(now.getFullYear(), now.getMonth(), now.getDate());
						to = new Date(now.getFullYear(), now.getMonth()+parseInt(adjust), now.getDate());
					}else if(adjust < 0){
						from = new Date(now.getFullYear(), now.getMonth()+parseInt(adjust), now.getDate());
						to = new Date(now.getFullYear(), now.getMonth(), now.getDate());
					}else{
						from = new Date(now.getFullYear(), now.getMonth(), 1);
						to =  new Date(now.getFullYear(), now.getMonth()+1, 0);
					}
				}else{
					from = new Date(now.getFullYear(), now.getMonth(), 1);
					to =  new Date(now.getFullYear(), now.getMonth()+1, 0);
				}
			}else if(p.default.toLowerCase().indexOf('week') == 0){
				if(p.default.indexOf(':')){
					var adjust = p.default.split(':')[1];
					if(adjust > 0){
						from = new Date(now.getFullYear(), now.getMonth(), now.getDate());
						to = new Date(now.getFullYear(), now.getMonth(), now.getDate()+parseInt(adjust * 7));
					}else if(adjust < 0){
						from = new Date(now.getFullYear(), now.getMonth(), now.getDate()+parseInt(adjust * 7));
						to = new Date(now.getFullYear(), now.getMonth(), now.getDate());
					}else{
						from = new Date(now.getFullYear(), now.getMonth(), now.getDate()-7);
						to =  now;
					}
				}else{
					from = new Date(now.getFullYear(), now.getMonth(), now.getDate()-7);
					to =  now;
				}
			}else if(p.default.toLowerCase().indexOf('day') == 0){
				if(p.default.indexOf(':')){
					var adjust = p.default.split(':')[1];
					if(adjust > 0){
						from = new Date(now.getFullYear(), now.getMonth(), now.getDate());
						to = new Date(now.getFullYear(), now.getMonth(), now.getDate()+parseInt(adjust));
					}else if(adjust < 0){
						from = new Date(now.getFullYear(), now.getMonth(), now.getDate()+parseInt(adjust));
						to = new Date(now.getFullYear(), now.getMonth(), now.getDate());
					}else{
						from = now;
						to =  now;
					}
				}else{
					from = now;
					to =  now;
				}
			}
			
			if(from && to){
				obj.setDate('from', from);
				obj.setDate('to'  , to);
			}
		}
		p.data[optionLi.id] = obj;
	}else if(p.type == 'checkbox'){
	    var input = document.createElement('input');
	    appendClassName(input, 'sfilter_filter_input');
	    input.type = 'checkbox';
	    optionLi.appendChild(input);
	    
	    p.data[optionLi.id] = input;
	    p.activeItem = input;
	    
	    var button = document.createElement('div');
	    appendClassName(button, 'sfilter_filter_button');
	    appendClassName(button, 'sfilter_filter_button_blank');
	    optionLi.appendChild(button);
	    
	    button.targetProperties = p;
	    
	    $(button).on({
	        click:this._buttonClick
	    });
	    
	    $(input).change(function(){
	        if($(this).is(':checked')) {
	            $(this).attr('value', 'true');
	        } else {
	            $(this).attr('value', 'false');
	        }
	    });
	}else if(p.type == 'range'){
		var select = document.createElement('select');
		appendClassName(select, 'sfilter_filter_range_select');
		optionLi.appendChild(select);
		
		var operatorList = [
			{text:' ' , value:''},
			{text:'>' , value:'gt'},
			{text:'>=', value:'ge'},
			{text:'<' , value:'lt'},
			{text:'<=', value:'le'},
			{text:'==', value:'eq'},
			{text:'!=', value:'ne'}
		]
		for(var idx in operatorList){
			var op = operatorList[idx];
			var option = document.createElement('option');
			option.innerText = op.text;
			option.value = op.value;
			select.appendChild(option);
		}
		
		var input = document.createElement('input');
		appendClassName(input, 'sfilter_filter_range_input');
	    optionLi.appendChild(input);
	    
	    p.data[optionLi.id] = {
			operator:select,
			value:input
		};
		p.activeItem = optionLi.id;
	    
	    var buttonAdd = document.createElement('div');
		appendClassName(buttonAdd, 'sfilter_filter_button');
		appendClassName(buttonAdd, 'sfilter_filter_button_add');
		appendClassName(buttonAdd, 'sfilter_filter_button_range');
		optionLi.appendChild(buttonAdd);
		
		$(buttonAdd).on({
			click:function(){
				p.filterObj._addOption(p)
			}
		})
		
		var buttonDelete = document.createElement('div');
		appendClassName(buttonDelete, 'sfilter_filter_button');
		appendClassName(buttonDelete, 'sfilter_filter_button_delete');
		appendClassName(buttonDelete, 'sfilter_filter_button_range');
		optionLi.appendChild(buttonDelete);
	    
		buttonDelete.targetProperties = p;
		
		$(buttonDelete).on({
			click:this._buttonClick
		})
	}
}
SepoaFilterObject.prototype.items.filter.prototype.getFilterData=function(){
	var result = {};
	for(var idx in this.items){
		var t = this.items[idx];
		if(t.type == 'text'){
			var data = []; 
			for(var idx in t.data){
				var v = t.data[idx].value;
				if(v)
					data.push(v);
			}
			if(data.length > 0)
				result[t.id] = data.join(',');
		}else if(t.type == 'select'){
			var data = [];
			
			for(var idx in t.data){
				try{
					if(t.data[idx].ready){
						if(t.data[idx].disabled === true){
							var so = t.data[idx].options[t.data[idx].selectedIndex].value;
							if(so)
								data.push(so);
						}
					}else{
						var so = t.data[idx].defaultValue;
						if(so)
							data.push(so);
					}
				}catch(e){}
			}
			if(data.length > 0)
				result[t.id] = data.join(',');
		}else if(t.type == 'calendar'){
			var resultDate = {};
			
			for(var idx in t.data){
				var date = t.data[idx].getDate();
				for(var ft in date){
					if(date[ft] != "Invalid Date"){
						resultDate[ft] = dhx4.date2str(date[ft], '%Y%m%d'); 
					}else{
						resultDate[ft] = false;
					}
				}
			}
			
			if(resultDate.from)
				result[t.id+'_FROM'] = resultDate.from;
			if(resultDate.to)
				result[t.id+'_TO'] = resultDate.to;
			
		}else if(t.type == 'checkbox'){
			var data = []; 
			for(var idx in t.data){
				var v = t.data[idx].value;
				if(v)
					data.push(v);
			}
	        if(data.length > 0)
	        	  result[t.id] = data.join(',');
		}else if(t.type == 'popup'){
			var data = [];
			for(var idx in t.data){
				var so = t.data[idx].code.value;
				if(so)
					data.push(so);
			}
			if(data.length > 0)
				result[t.id] = data.join(',');
		}else if(t.type == 'range'){
			var data = [];
			for(var idx in t.data){
				var o = t.data[idx].operator.value;
				var v = t.data[idx].value.value
				
				if((o != null && o.trim() != '') && (v != null && v.trim() != '')){
					data.push(o + '^@^' + v);
				}
			}
			if(data.length > 0)
				result[t.id] = data.join('^@@^');
		}
	}
	return result;
}
SepoaFilterObject.prototype.items.filter.prototype._buttonClick=function(){
	var li = this.parentNode;
	if(this.className.split(' ').indexOf('sfilter_filter_button_add') >= 0){
		var active = this.targetProperties.activeItem;
		if(active.value.trim().length <= 0) return;
		appendClassName(li, 'sfilter_filter_readonly');
		removeClassName(this, 'sfilter_filter_button_blank');
		removeClassName(this, 'sfilter_filter_button_add');
		appendClassName(this, 'sfilter_filter_button_delete');
		if(active.tagName.toLowerCase() == 'input'){
			active.readOnly=true;
		}else if(active.tagName.toLowerCase() == 'select'){
			active.disabled=true;
		}
		
		this.targetProperties.filterObj._addOption(this.targetProperties);
	}else if(this.className.split(' ').indexOf('sfilter_filter_button_delete') >= 0){
		delete this.targetProperties.data[li.id];
		$(li).remove();
	}
}
SepoaFilterObject.prototype.items.filter.prototype.loadCombo=function(p, d, select){
	var params = p.combo.init;
	if(typeof(params)=='undefined' || params==null || params.length < 1){
		return;
	}
	params = params.split(',');
	
	var code = params[0];
	var param = window.dhx4.comboParam((typeof(params[1])=='undefined' || params[1]==null)?'':params[1]);
	var queryString = "&mode=form&code=" + code + "&param=" + param;
	$.ajax({
		url: window.CONTEXT_NAME+"/servlets/sepoa.svl.util.SepoaAjaxCombo?timeStamp=" + new Date().getTime(),
		type:"post",
		data: queryString,
		dataType: "json",
		selectBox: select,
		sepoaObject: this.filterObj.SepoaObject,
		success: function(data){
			if(typeof(data.data) == 'undefined' || data.data == null){
				return;
			}
			
			jqo = this.selectBox;
			
			if(!jqo.notnull || jqo.notnull != 'true'){
				data.data = [{text:"", value:""}].concat(data.data);
			}
			p.combo.data = data;
			
			jqo.ready = true;
			for(var idx in data.data){
				var o = data.data[idx];
				var option = document.createElement('option');
				option.value = o.value;
				option.innerText = o.text;
				if(jqo.defaultValue && option.value == jqo.defaultValue){
					 option.selected = true;
				}
				jqo.appendChild(option);
			}
			if(jqo.defaultValue){
				jqo.readyEvent();
			}
			if(p.callreloadevent){
				p.callreloadevent = false;
				p.filterObj.filterObj.callEvent('onReloadCombo', [p.id]);
			}
		}
	});
}

SepoaFilterObject.prototype.items.filter.prototype.setShow=function(p){
	removeClassName(this.cont, "sfilter_hidden");
	var screenX = this.cont.offsetWidth;
	if(p){
		this.btnObj = p;
		appendClassName(this.btnObj.cont, "btn_selected")
		
		var lt = sUtils.element.getOffset(this.cell, "module.top");
		
		lt.left -= window.scrollX?window.scrollX:0;
		lt.top -= window.scrollY?window.scrollY:0;
		
		if(lt.left + screenX > frame.document.body.offsetWidth){
			lt.left = frame.document.body.offsetWidth - screenX
		}
		
		this.cont.style.top =(lt.top+p.cont.offsetHeight)+'px';
		this.cont.style.left =lt.left+'px';
	}
	this.viewStatus=true;
	this.filterObj.showModal(this);
}
SepoaFilterObject.prototype.items.filter.prototype.setHide=function(f){
	if(this.btnObj){
		removeClassName(this.btnObj.cont, "btn_selected");
	}
	
	appendClassName(this.cont, "sfilter_hidden");
	this.cont.style.left = '0px';
	this.cont.style.top = '0px';
	this.viewStatus = false;
	if(!f)
		this.filterObj.hideModal(this);
}
SepoaFilterObject.prototype.items.filter.prototype.toggleView=function(p){
	if(this.viewStatus){
		this.setHide();
	}else{
		this.setShow(p);
	}
}
SepoaFilterObject.prototype.items.status=function(p, c, v){
	window.dhtmlx.extend(this, dhtmlx.EventSystem);
	
	var vp = SUX.getModuleTop().document.getElementById('sfilter_moduleTop_area');
	if(!vp){
		vp = document.createElement('div');
		vp.className = 'sfilter_cont';
		vp.style.position='inherit';
		SUX.getModuleTop().document.body.appendChild(vp);
	}
	vp.appendChild(v)
	var cont = this.cont = v;
	
	this.filterObj= p;
	this.conf=c;
	this.setHide();
	this.reloadComboBox(c.combo);
	this.isDisable=false;
	this._btnTrigger=this.toggleView;
	this.attachEvent('onClick', this.setSelect);
	Sepoa.attachEvent("onWindowResize",function(){
		var hh = (window.document.body.offsetHeight/2);
		var sh = cont.scrollHeight;
		cont.style.height = hh+'px';
		if(sh < hh*1.1){
			cont.style.height='auto';
			cont.style.overflow='auto';
		}
	}, true);
}
SepoaFilterObject.prototype.items.status.prototype.renderList=function(l){
	this.cont.innerHTML = "";
	
	var item = document.createElement('div');
	item.statusObj = this;
	item.comboValue = "";
	appendClassName(item, 'sfilter_status_item');
	item.innerHTML="<span class='sfilter_status_text'>"+Sepoa.cache.text.get('BUTTON.ALL')+"</span>"
	this.cont.appendChild(item);
	$(item).on({
		click:function(){
			this.statusObj.callEvent('onClick',[this.comboValue]);
		}
	});
	
	var domList = [];
	for(var idx in l){
		var t = l[idx];
		var item = document.createElement('div');
		item.statusObj = this;
		item.comboValue = t.value;
		item.comboText = t.text;
		appendClassName(item, 'sfilter_status_item');
		item.innerHTML="<span class='sfilter_status_text'>"+t.text+"</span>"		
		this.cont.appendChild(item);
		$(item).on({
			click:function(){
				this.statusObj.callEvent('onClick',[this.comboValue,undefined, this.comboText]);
			}
		});
		domList.push(item);
	}
	this.setSelect(this.conf.default||"", true);
	this.filterObj.callEvent('onLoadEnd', ['status', domList]);
}
SepoaFilterObject.prototype.items.status.prototype.toggleView=function(p){
	if(this.viewStatus){
		this.setHide();
	}else{
		this.setShow(p);
	}
}
SepoaFilterObject.prototype.items.status.prototype.setSelect=function(v, flag, vt){
	var a = $(this.cont).find('.sfilter_status_item');
	for(var idx in a){
		var t = a[idx];
		if(t.comboValue != v){
			removeClassName(t, "sfilter_status_selected");
		}else{
			appendClassName(t, "sfilter_status_selected");
		}
	}
	this.selectedValue=v;
	this.setHide();
	if(!flag)
		this.filterObj.callEvent('onSearch',['search','status',v,vt]);
}
SepoaFilterObject.prototype.items.status.prototype.getFilterData=function(){
	var result = {};
	if(!this.ready){
		result[this.conf.id] = this.conf.default;
	}else if(this.selectedValue){
		result[this.conf.id] = this.selectedValue;
	}
	return result;
}
SepoaFilterObject.prototype.items.status.prototype.setShow=function(p){
	removeClassName(this.cont, "sfilter_hidden");
	var screenX = this.cont.offsetWidth;
	if(p){
		this.btnObj = p;
		appendClassName(this.btnObj.cont, "btn_selected")
		
		var lt = sUtils.element.getOffset(this.cell, "module.top");
		lt.left -= window.scrollX?window.scrollX:0;
		lt.top -= window.scrollY?window.scrollY:0;
		
		if(lt.left + screenX > frame.document.body.offsetWidth){
			lt.left = frame.document.body.offsetWidth - screenX
		}
		
		this.cont.style.top =(lt.top+p.cont.offsetHeight)+'px';
		this.cont.style.left =lt.left+'px';
	}
	this.viewStatus=true;
	this.filterObj.showModal(this);
}
SepoaFilterObject.prototype.items.status.prototype.setHide=function(f){
	if(this.cont){
		appendClassName(this.cont, "sfilter_hidden");
		this.cont.style.left = '0px';
		this.cont.style.top = '0px';
	}
	if(this.btnObj){
		removeClassName(this.btnObj.cont, "btn_selected")
	}
	this.viewStatus=false;
	if(!f)
		this.filterObj.hideModal(this);
}
SepoaFilterObject.prototype.items.status.prototype.reloadComboBox = function(params){
	if(typeof(params)=='undefined' || params==null || params.length < 1){
		return;
	}
	this.ready = false;
	params = params.split(',');
	var that = this;
	var code = params[0];
	var param = window.dhx4.comboParam((typeof(params[1])=='undefined' || params[1]==null)?'':params[1]);
	var default_value = (params.length>=3)?params[2]:"";
	var queryString = "&mode=form&code=" + code + "&param=" + param+ "&selectbox_name=&default_value="+default_value;
	$.ajax({
		url: window.CONTEXT_NAME+"/servlets/sepoa.svl.util.SepoaAjaxCombo?timeStamp=" + new Date().getTime(),
		type:"post",
		data: queryString,
		dataType: "json",
		success: function(data){
			that.ready = true;
			if(typeof(data.data) == 'undefined' || data.data == null){
				return;
			}
			that.renderList(data.data);
		},
		error:function(data){
			that.ready = true;
		}
	});
}

SepoaFilterObject.prototype.items.calendar=function(p, c, vp){
	this.filterObj= p;
	this.conf=c;
	this.cont=vp;
	this.button={};
	this.dateInputStatus={to:false,from:false}
	window.dhtmlx.extend(this, dhtmlx.EventSystem);
	this.drawButton();
	this.isDisable=false;
	if(c.default){
		this.setSelected(c.default);
	}
};
SepoaFilterObject.prototype.items.calendar.prototype.drawButton=function(){
	var p = this.filterObj;
	if(this.conf.button){
		var b = document.createElement('div');		
		appendClassName(b, "sfilter_calendar_btn_cont");
		this.cont.appendChild(b);
		if(p.conf.height) b.style.height=(p.conf.height+2)+'px';
		var c = this.buttonCont = document.createElement('div');
		appendClassName(c, "sfilter_calendar_btn_cont_center");
		b.appendChild(c);
		
		var button = this.conf.button.split(',');
		for(var _idx in button){
			var btn = document.createElement('div');
			appendClassName(btn,"sfilter_calendar_btn");
			c.appendChild(btn);
			var s = document.createElement('span');
			appendClassName(s, "sfilter_calendar_btn_text");
			btn.appendChild(s);
			a = button[_idx];
			var t = a.split(':');
			this.button[a]={
				adjust:t[1],
				selected:false,
				type:t[0],
				id:a,
				base:btn,
				calendar:this
			}
			btn.buttonObj = this.button[a];
			$(btn).on({
				click:function(){
					this.buttonObj.calendar.setSelected(this.buttonObj.id);
					p.callEvent("onSearch",["calendar"]);
				}
			});
			this.setText(a, this.getAdjustDateString(t[0],t[1]));
		}
	
		var bl = document.createElement('div');		
		appendClassName(bl, "sfilter_calendar_blank");
		this.cont.appendChild(bl);
	
	}
	
	var b = document.createElement('div');		
	appendClassName(b, "sfilter_calendar_btn_cont");
	this.cont.appendChild(b);
	if(p.conf.height)b.style.height=(p.conf.height+2)+'px';
	var c = this.iconCont = document.createElement('div');
	appendClassName(c, "sfilter_calendar_btn_cont_center");
	b.appendChild(c);
	
	var btn = document.createElement('div');
	appendClassName(btn,"sfilter_calendar_btn");
	appendClassName(btn,"sfilter_calendar_img");
	c.appendChild(btn);
	
	btn.style.width = this.filterObj.conf.height+'px';
	
	a = "icon";
	this.button[a]={
		selected:false,
		type:this.conf.icon,
		id:a,
		base:btn,
		calendar:this
	}
	btn.buttonObj = this.button[a];
	$(btn).on({
		click:function(){
			this.buttonObj.calendar.setSelected(this.buttonObj.id, true);
			this.buttonObj.calendar.callEvent('onCalendarButtonClick',['icon']);
		}
	});

	var bl = document.createElement('div');		
	appendClassName(bl, "sfilter_calendar_blank");
	this.cont.appendChild(bl);
	
	this._attachCalendar();
	
	if(this.conf.default){
		this.setSelected(this.conf.default, true);
	}
}
SepoaFilterObject.prototype.items.calendar.prototype._attachCalendar=function(){
	var p = this.filterObj;
	var inp = document.createElement('div');		
	appendClassName(inp, "sfilter_calendar_btn_cont");
	appendClassName(inp, "sfilter_calendar_btn_cont_input");
	this.cont.appendChild(inp);
	if(p.conf.height) inp.style.height=p.conf.height+'px';
	var inputArea = this.inputArea = document.createElement('div');
	appendClassName(inputArea, "sfilter_calendar_btn_cont_center");
	inp.appendChild(inputArea);
	inputArea.innerHTML='<div class=sfilter_date_area><input type="text" class="sfilter_date_input date_from"/><span class="sfilter_date_span"> ~ </span><input type="text" class="sfilter_date_input date_to"/></div>';
	inputArea.filterCalendarObj = this;
	var doubleCA = this.doubleCA = document.createElement('div');
	appendClassName(doubleCA, 'sfilter_date_calendar_box');
	//inputArea.getElementsByClassName('sfilter_date_area')[0].appendChild(doubleCA);
	this.calendarBox = SUX.getModuleTop().calendarBox 
	if(!this.calendarBox){
		this.calendarBox = SUX.getModuleTop().calendarBox = document.createElement('div');
		appendClassName(this.calendarBox, 'sfilter_common_calendar_box')
		SUX.getModuleTop().document.body.appendChild(this.calendarBox);
	}
	this.calendarBox.appendChild(doubleCA);
	
	var ia = inputArea.getElementsByClassName('sfilter_date_area')[0];
	var dc = this._doubleCalendar = new dhtmlXDoubleCalendar(doubleCA);
	var that = this;
	dc.attachEvent('onClick',function(side,date){
		var s = (side=='right')?'to':'from';
		that.setDate(s, date);
	});
	$(inputArea).on({
		click:function(){
			this.filterCalendarObj.callEvent('onCalendarButtonClick',[]);
		}
	})
	$(this.inputArea.getElementsByClassName("sfilter_date_input")).on({
		blur:function(){
			var po = this.parentElement.parentElement.filterCalendarObj;
			var side = "to";
			if(this.classList.contains("date_from")){
				side = "from";
			}
			if(this.value)
				po.setDate(side, this.value);
		}
	});
	this.attachEvent('onCalendarButtonClick',function(mode){
		if(mode == 'icon' && that.viewStatus){
			that.setHide();
			return;
		}
		this.setShow();
	});
}
SepoaFilterObject.prototype.items.calendar.prototype.setDate=function(c, s){
	var t = this.inputArea.getElementsByClassName("date_"+c);
	
	var d = this.getFormattedDate(s);
	var ds = "";
	var us = "";
	if(d != 'Invalid Date'){
		ds = dhx.date2str(d,Sepoa.info.DATE_FORMAT || '%Y/%m/%d');
		us = dhx.date2str(d,'%Y%m%d');
	}
	
	
	if(t.length<0){
		t = this.inputArea.getElementsByClassName("sfilter_date_input");
		this._doubleCalendar.setDates(d,d);
		
		this.dateInputStatus.to=true;
		this.dateInputStatus.from=true;
		
		t[0].value = dhx.date2str(this._doubleCalendar.leftCalendar.getDate(), Sepoa.info.DATE_FORMAT || '%Y/%m/%d');
		t[1].value = dhx.date2str(this._doubleCalendar.rightCalendar.getDate(), Sepoa.info.DATE_FORMAT || '%Y/%m/%d');
	}else{
		if(c == 'from'){
			var rd = dhx.date2str(this._doubleCalendar.rightCalendar.getDate(), '%Y%m%d');
			if(rd<=us){
				this._doubleCalendar.setDates(null, d);
				var a = this.inputArea.getElementsByClassName("date_to");
				if(a[0])
					a[0].value = dhx.date2str(this._doubleCalendar.rightCalendar.getDate(), Sepoa.info.DATE_FORMAT || '%Y/%m/%d');
			}
			this._doubleCalendar.setDates(d, null);
			t[0].value = dhx.date2str(this._doubleCalendar.leftCalendar.getDate(), Sepoa.info.DATE_FORMAT || '%Y/%m/%d');
		}else{
			var ld = dhx.date2str(this._doubleCalendar.leftCalendar.getDate(), '%Y%m%d');
			if(ld>=us){
				this._doubleCalendar.setDates(d, null);
				var a = this.inputArea.getElementsByClassName("date_from");
				if(a[0])
					a[0].value = dhx.date2str(this._doubleCalendar.leftCalendar.getDate(), Sepoa.info.DATE_FORMAT || '%Y/%m/%d');
			}
			this._doubleCalendar.setDates(null, d);
			t[0].value = dhx.date2str(this._doubleCalendar.rightCalendar.getDate(), Sepoa.info.DATE_FORMAT || '%Y/%m/%d');
		}
		this.dateInputStatus[c]=true;
	}
	
	if(this.dateInputStatus.to && this.dateInputStatus.from){
		this.setHide();
		this.viewStatus = false;
	}
	(this.filterObj.filterObj || this.filterObj).callEvent('onChange',[this.conf.id,ds,c]);
}
SepoaFilterObject.prototype.items.calendar.prototype.getDate=function(c){
	if(c){
		var t = this.inputArea.getElementsByClassName("date_"+c);
		return dhx4.str2date((t[0].value || ""), Sepoa.info.DATE_FORMAT);
	}else{
		return {
			from:dhx4.str2date((this.inputArea.getElementsByClassName("date_from")[0].value || ""), Sepoa.info.DATE_FORMAT),
		    to:dhx4.str2date((this.inputArea.getElementsByClassName("date_to")[0].value || ""), Sepoa.info.DATE_FORMAT)
		}
	}
}
SepoaFilterObject.prototype.items.calendar.prototype.getFormattedDate=function(s){
	var r = s;
	if(!(s instanceof Date)){
		r = dhx.str2date(s, '%Y%m%d');
		if(r == 'Invalid Date'){
			r = dhx.str2date(s, Sepoa.info.DATE_FORMAT);
		}
	}
	return r;
}
SepoaFilterObject.prototype.items.calendar.prototype.setText=function(id, text){
	var b = this.button[id].base;
	b.firstChild.innerHTML=text;
};
SepoaFilterObject.prototype.items.calendar.prototype.setSelected =function(id, f){
	var selected = null;
	for(var idx in this.button){
		var t = this.button[idx];
		if(idx == id){
			t.selected=true;
			appendClassName(t.base,'btn_selected');
			selected = t;
			if(id != 'icon'){
				var ad = parseInt(t.adjust);
				var now = new Date();
				switch(t.type){
					case 'year':
						this.setDate('from', new Date(now.getFullYear()+ad, 0, 1, 0, 0, 0, 0));
						this.setDate('to', new Date(now.getFullYear()+ad+1, 0, 0, 0, 0, 0, 0));
						break;
					case 'month':
						this.setDate('from', new Date(now.getFullYear(), now.getMonth()+ad, 1, 0, 0, 0, 0));
						this.setDate('to', new Date(now.getFullYear(), now.getMonth()+ad+1, 0, 0, 0, 0, 0));
						break;
					case 'week':
						if(ad % 4 == 0){
							ad /= 4;
							if(ad == 0){
								this.setDate('from', new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0));
								this.setDate('to', new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0));
							}else if(ad > 0){
								this.setDate('from', new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0));
								this.setDate('to'  , new Date(now.getFullYear(), now.getMonth()+ad, now.getDate(), 0, 0, 0, 0));
							}else if(ad < 0){
								this.setDate('from', new Date(now.getFullYear(), now.getMonth()+ad, now.getDate(), 0, 0, 0, 0));
								this.setDate('to', new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0));
							}
							break;
						}else{
							ad *= 7;
						}
					case 'day':
						if(ad == 0){
							this.setDate('from', new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0));
							this.setDate('to', new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0));
						}else if(ad > 0){
							this.setDate('from', new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0));
							this.setDate('to', new Date(now.getFullYear(), now.getMonth(), now.getDate()+ad, 0, 0, 0, 0));
						}else if(ad < 0){
							this.setDate('from', new Date(now.getFullYear(), now.getMonth(), now.getDate()+ad, 0, 0, 0, 0));
							this.setDate('to', new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0, 0, 0));
						}
						break;
				}
			}
		}else{
			t.selected=false;
			removeClassName(t.base,'btn_selected');
		}
	}
};

SepoaFilterObject.prototype.items.calendar.prototype.setDisable=function(f){
	this.isDisable(f);
}
SepoaFilterObject.prototype.items.calendar.prototype.setHide=function(f){
	this._doubleCalendar.hide();
	removeClassName(this.doubleCA,"showBox");
	
	this.doubleCA.style.top ='0px';
	this.doubleCA.style.left ='0px';
	
	this.viewStatus = false;
	if(!f)
		(this.filterObj.filterObj || this.filterObj).hideModal(this);
}
SepoaFilterObject.prototype.items.calendar.prototype.setShow=function(){
	this.doubleCA.style.top = '0px';
	this.doubleCA.style.left= '0px';
	appendClassName(this.doubleCA,"showBox");
	this._doubleCalendar.show();
	this._doubleCalendar.setDates(this.getDate('from'), this.getDate('to'));
	
	var ia = this.inputArea.getElementsByClassName('sfilter_date_area')[0];
	var lt = sUtils.element.getOffset(this.cell, "module.top");
	
	if(lt.left + this.doubleCA.offsetWidth > SUX.getModuleTop().innerWidth){
		lt.left = SUX.getModuleTop().innerWidth - this.doubleCA.offsetWidth; 
	}
	
	if(lt.top + this.doubleCA.offsetHeight+ia.offsetHeight > SUX.getModuleTop().innerHeight){
		lt.top = SUX.getModuleTop().innerHeight - this.doubleCA.offsetHeight - ia.offsetHeight; 
	}
	
	this.doubleCA.style.top =(lt.top+ia.offsetHeight)+'px';
	this.doubleCA.style.left =lt.left+'px';
	
	
	this.dateInputStatus.to=false;
	this.dateInputStatus.from=false;
	this.viewStatus = true;
	if(this.filterObj.showModal) this.filterObj.showModal(this);
	else if(this.filterObj.filterObj.showModal) this.filterObj.filterObj.showModal(this);
}
SepoaFilterObject.prototype.items.calendar.prototype.getFilterData=function(){
	var result = {};
	var resultDate = {};
	
	var date = this.getDate();
	for(var ft in date){
		if(date[ft] != "Invalid Date"){
			resultDate[ft] = dhx4.date2str(date[ft], '%Y%m%d'); 
		}else{
			resultDate[ft] = false;
		}
	}
	
	if(resultDate.from)
		result[this.conf.id+'_FROM'] = resultDate.from;
	if(resultDate.to)
		result[this.conf.id+'_TO'] = resultDate.to;
	
	return result;
}
SepoaFilterObject.prototype.items.calendar.prototype.getAdjustDateString=function(t, a){
	var l = (Sepoa.info.LANGUAGE == 'KO')?'KO':'eng'
	var d = new Date();
	switch(t){
		case "year":
			var y = d.getFullYear();
			return parseInt(y)+parseInt(a);
		case "month":
			var m = d.getMonth();
			var s = parseInt(m)+parseInt(a);
			switch(l){
				case 'ko':
				case 'KO':
					var u = s+1;
					return (u<1)?12+u:u+'월';
				case 'eng':
				default :
					return ({
						0:{long:"January", short:"Jan"},
						1:{long:"February", short:"Feb"},
						2:{long:"March", short:"Mar"},
						3:{long:"April", short:"Apr"},
						4:{long:"May", short:"May"},
						5:{long:"June", short:"Jun"},
						6:{long:"July", short:"Jul"},
						7:{long:"August", short:"Aug"},
						8:{long:"September", short:"Sep"},
						9:{long:"October", short:"Oct"},
						10:{long:"November", short:"Nov"},
						11:{long:"December", short:"Dec"}
					})[(12+s) % 12]["short"];
			};
		case "week":
			if(a == 0) a = -1;
			
			if(a%4 == 0){
				a = a/4;
				return a + ' ' + ((l=='KO')?'개월':'Month') + ((!(l=='KO') && [-1,0,1].indexOf(a)+1)?'s':'');
			}else{
				return a + ' ' + ((l=='KO')?'주':'Week') + ((!(l=='KO') && [-1,0,1].indexOf(a)+1)?'s':'');
			}
		case "day":
			var d2str = { "-1":{KO:'어제', eng:'Yesterday'}
			 			  ,0:{KO:'오늘', eng:'Today'}
			 			  ,1:{KO:'내일', eng:'Tomorrow'}
			 			  ,d:{KO:'일', eng:'Day'}}
			if(d2str[a]){
				return d2str[a][l];
			}else{
				return a + ' ' + d2str[d][l || 'eng']+(!(l=='KO'))?'s':'';
			}
			
	}
};