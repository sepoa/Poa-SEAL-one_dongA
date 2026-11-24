SepoaFlowView = function(vp, config, item){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.config = $.extend(true, {
		template:{
			base:'<div class="sepoa-flow-view noselect"><div class="sepoa-flow-view-button-box"><div class="sepoa-flow-view-button sepoa-flow-view-button-prev"></div></div><div class="sepoa-flow-view-item-box"><div class="sepoa-flow-view-item-wrapper"></div><div class="sepoa-flow-view-item-toggle"></div></div><div class="sepoa-flow-view-button-box"><div class="sepoa-flow-view-button sepoa-flow-view-button-next"></div></div></div>',
			fix:'<div class="sepoa-flow-view noselect"><div class="sepoa-flow-view-item-box"><div class="sepoa-flow-view-item-wrapper"></div><div class="sepoa-flow-view-item-toggle"></div></div></div>'
		}
	}, config);
	this._items={};
	this._active=null;
	this._drawBase(vp);
	for(var i in item){
		this.addItem(item[i])
	}
	if(this.config.view == 'step'){
		this.setActive(this.getId(0));
	}
	this._setFixView();
	this.callEvent('onContentSizeChange', [this]);
}
SepoaFlowView.prototype.getItemWrapper=function(){
	return this._itemWrapper;
}
SepoaFlowView.prototype._drawBase=function(parent){
	this._vp = parent.appendChild($(this.config.template.base)[0]);
	this._itemWrapper = this._vp.children[1].children[0];
	this._buttonWrapper = {
		left:this._vp.children[0],
		right:this._vp.children[2]
	}
	this._vp.addEventListener('click', function(e){
		var buttons = $(e.srcElement).parents('.sepoa-flow-view-button')[0];
		if(!buttons && e.srcElement.classList.contains('sepoa-flow-view-button')){
			buttons = e.srcElement;
		}
		if(buttons && !buttons.classList.contains('disabled')){
			if(buttons.classList.contains('sepoa-flow-view-button-prev')){
				this.setActive(this._getNextId(-1), false, 'prev');
			}else{
				this.setActive(this._getNextId(1), false, 'next');
			}
			return;
		}
	}.bind(this));
}
SepoaFlowView.prototype.addItem=function(data, index){
	var item = new SepoaFlowViewItem(this.config, data);
	item.parentObj = this;
	
	this._itemWrapper.appendChild(item._getView());
	this._items[item.id] = item;
	item.attachEvent('onItemClick', function(id, itemObj){
		if(itemObj._vp.classList.contains("selectable")){
			this.setActive(id);
		}
	}.bind(this));
}
SepoaFlowView.prototype.setActive=function(id, force, btn){
	var next = this._items[id];
	if(next){
		if(force || this.callEvent('onBeforeActive', [id, this._active, btn]) !== false){
			if(this._active){
				this._active.setActive(false);
			}
			next.setActive();
			this._active = next;
			this._updateSelectableItem();
			this._updateButtonStatus();
			this.callEvent('onActive', [next.id]);
		}
	}
}
SepoaFlowView.prototype.setStop=function(id){
	var activeItem = this._items[id];
	if(activeItem){
		this._active = activeItem;
		this._active._vp.classList.add('fix-reject');
		
		var preItemVp = this._active._vp.previousElementSibling;
		if(preItemVp){
			var preItem = preItemVp._item;
			while(preItem){
				preItem._vp.classList.add('fix-complete'); 
				preItem = this._returnPreStep(preItem);
			}
		}
		
		var nextItemVp = this._active._vp.nextElementSibling;
		if(nextItemVp){
			var nextItem = nextItemVp._item;
			while(nextItem){
				nextItem._vp.classList.add('fix-wait'); 
				nextItem = this._returnNextStep(nextItem);
			}
		}
		
	}
}

SepoaFlowView.prototype.setReject=function(id){
	var activeItem = this._items[id];
	if(activeItem){
		this._active = activeItem;
		this._active._vp.classList.add('fix-reject');
		
		var preItemVp = this._active._vp.previousElementSibling;
		if(preItemVp){
			var preItem = preItemVp._item;
			while(preItem){
				preItem._vp.classList.add('fix-complete'); 
				preItem = this._returnPreStep(preItem);
			}
		}
		
		var nextItemVp = this._active._vp.nextElementSibling;
		if(nextItemVp){
			var nextItem = nextItemVp._item;
			while(nextItem){
				nextItem._vp.classList.add('fix-wait'); 
				nextItem = this._returnNextStep(nextItem);
			}
		}
	}
}

SepoaFlowView.prototype._returnNextStep=function(item){
	if(item._vp.nextElementSibling){
		return item._vp.nextElementSibling._item;
	}
}
SepoaFlowView.prototype._returnPreStep=function(item){
	if(item._vp.previousElementSibling){
		return item._vp.previousElementSibling._item;
	}
};
SepoaFlowView.prototype._updateSelectableItem=function(){
	if(this.config.view == 'fix'){
		return;
	} 
	
	this.forEach("setSelectable", [false]);
	var active = this._active._vp;
	if(active.nextElementSibling){
		active.nextElementSibling._item.setSelectable(true, 'next');
	}
	if(active.previousElementSibling){
		active.previousElementSibling._item.setSelectable(true, 'prev');
	}
}
SepoaFlowView.prototype._updateButtonStatus=function(){
	var ind = this.getIndex(this._active.id);
	this._buttonWrapper.left.children[0].classList.remove('disabled');
	this._buttonWrapper.right.children[0].classList.remove('disabled');
	if(ind == 0){
		this._buttonWrapper.left.children[0].classList.add('disabled');
	}
	if(ind == (this.getCount() - 1)){
		this._buttonWrapper.right.children[0].classList.add('disabled');
	}
}
SepoaFlowView.prototype.forEach=function(f, args){
	for(var i in this._items){
		this._items[i][f].apply(this._items[i], args);
	}
}
SepoaFlowView.prototype.getId=function(ind){
	var t = this._itemWrapper.children[ind]
	return t?t._item.id:null;
}
SepoaFlowView.prototype._getNextId=function(adj){
	var ind = this.getIndex(this.getActiveId()) || 0;
	ind += parseInt(adj);
	return this.getId(ind);
}
SepoaFlowView.prototype.getActiveId=function(){
	if(this._active){
		return this._active.id;
	}else{
		return null;
	} 
}
SepoaFlowView.prototype.getIndex=function(id){
	var nodes = Array.prototype.slice.call(this._itemWrapper.children)
	return nodes.indexOf(this._items[id]._vp);
}

SepoaFlowView.prototype.getCount=function(){
	return this._itemWrapper.children.length;
}
SepoaFlowView.prototype._setFixView=function(){
	if(this.config.view != 'fix'){
		return;
	}
	//this._vp.classList.add('left-align');
	//config의 view가 fix일 때 양 옆 버튼 숨기기
	var btns = this._vp.querySelectorAll('.sepoa-flow-view-button-box');
	btns.forEach(function(btn){
		btn.classList.add('hidden');
	});
	//selectable 없애기
	for(var item in this._items){
		if(this._items[item]._vp.classList.contains('selectable')){
			this._items[item]._vp.classList.remove('selectable')
		}
	}
}
SepoaFlowView.prototype._setStepView=function(){
	if(this.config.view != 'step'){
		return;
	}
	var btns = this._vp.querySelectorAll('.sepoa-flow-view-button-box');
	btns.forEach(function(btn){
		btn.classList.remove('hidden');
	});
	this.setActive(this.getId(0));
}
SepoaFlowView.prototype.setViewMode = function(v) {
	this.config.view = v;
	
	if(v == 'fix'){
		this._setFixView();
	}else if(v == 'step'){
		this._setStepView();
	}
}

//items
SepoaFlowViewItem=function(config, data){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.config = $.extend(true, {
		template:{
			step:'<span class="sepoa-flow-view-item-step-label">#LABEL#</span>',
			content:'<p class="sepoa-flow-view-item-contents-default">#CONTENT#</p>'
		}
	}, config);
	this.status = data.status;
	this.defaultArrow = 'right-arrow';
	this._drawBase();
	this.id = data.id;
	this.data = data.data;
	this.setLabel(data.label);
	if(data.list){
		for(var i in data.list){
			this.addContent(data.list[i]);
		}
	}
}
SepoaFlowViewItem.prototype._itemWrapper='<div class="sepoa-flow-view-item"><div class="sepoa-flow-view-item-next-arrow"></div><div class="sepoa-flow-view-item-step"></div><div class="sepoa-flow-view-item-contents"></div></div>'
SepoaFlowViewItem.prototype._getView=function(){
	return this._vp;
}
SepoaFlowViewItem.prototype._drawBase=function(){
	this._vp = $(this._itemWrapper)[0];
	
	this._vp._item = this;
	this._step = this._vp.children[1];
	this._contents = this._vp.children[2];
	
	if(this.config.view == 'fix'){
		this._adjustFixStatus();
	}
	
	var arrowElement = this._vp.querySelector('.sepoa-flow-view-item-next-arrow');
	if(this.config.icon){
		arrowElement.classList.add(this.config.icon);
	}else{
		arrowElement.classList.add(this.defaultArrow);
	}
	
	this._vp.addEventListener('click', function(e){
		var item = $(e.srcElement).parents('.sepoa-flow-view-item-step')[0];
		if(!item && e.srcElement.classList.contains('sepoa-flow-view-item-step')){
			item = e.srcElement;
		}
		if(item){
			e.stopPropagation();
		    e.preventDefault();
			this.callEvent('onItemClick', [this.id, this]);
		}
	}.bind(this));
}
SepoaFlowViewItem.prototype._adjustFixStatus=function(){
	//complete, progress, wait, reject 
	if(this.status){
		var s = this.status.toLowerCase();
		var cssClass = 'fix-' + s;
		this._vp.classList.add(cssClass);
		
	}
}

SepoaFlowViewItem.prototype.setLabel=function(str){
	this._step.innerHTML = '';
	this._step.appendChild($(this.dataReplacer(this.config.template.step, {LABEL:str}))[0]);
}
SepoaFlowViewItem.prototype.addContent=function(contentObj){
	//this._contents.appendChild($(this.dataReplacer(this.config.template.content, {CONTENT:str.text, ICONTYPE:str.icon}))[0]);
	let item = null; 
	if(contentObj.icon){
		item = sUtils.element.create(this.dataReplacer(this.config.template.iconContent, sUtils.object.extend(true,{}, contentObj, {CONTENT:contentObj.text, ICONTYPE:contentObj.icon})));
		this._contents.appendChild(item);
		this.icon = this._contents.querySelector('.sepoa-flow-view-item-contents-icon-'+contentObj.icon);
	
		this.icon.addEventListener('click', function(){
			this.parentObj.callEvent('onIconClick', [this.id, this.data])
		}.bind(this))
		
		this.icon.addEventListener('mouseout', function(){
			this.parentObj.callEvent('onIconMouseOut', [this.id])
		}.bind(this))
		
	}else{
		item = sUtils.element.create(this.dataReplacer(this.config.template.content, sUtils.object.extend(true,{}, contentObj, {CONTENT:contentObj.text})))
		this._contents.appendChild(item);
	}
	item._data=contentObj;
}
SepoaFlowViewItem.prototype.dataReplacer=function(str, data){
	var m = str.match(/#([a-zA-Z0-9_.]{0,})#/)
	if(m != null){
		var nv = '';
		if(m[1].indexOf('COMMON.') >= 0){
			switch(m[1].substring(7)){
			case 'CONTEXT_NAME':
				nv=window.CONTEXT_NAME;
				break;
			case 'TOKEN':
				nv=window.sTokenString;
				break;
			case 'TIME':
				nv=Date.now();
				break;
			}
		}else{
			var v = data[m[1]];
			if(v !== undefined){
				nv = data[m[1]];
			}
		}
		return this.dataReplacer(str.replace(m[0], nv), data);
	}
	return str;
}
SepoaFlowViewItem.prototype.setActive=function(flag){
	if(flag === false){
		this._vp.classList.remove('active');
	}else{
		this._vp.classList.add('active');
	}
}
SepoaFlowViewItem.prototype.setSelectable=function(flag, type){
	if(flag){
		$(this._vp).addClass('selectable '+type);
	}else{
		$(this._vp).removeClass('selectable next prev');
	}
}
SepoaFlowViewItem.prototype.isSelectable=function(){
	return this._vp.classList.contains('selectable');
}


