SepoaContextMenuManager = function(conf){
	sUtils.modules.extend(this, 'EventManager');
	this.conf = $.extend(true, {
		defaultClassName:'context-menu-selector'
	}, conf);
}

SepoaContextMenuManager.prototype._loadResource = function() {
	var loadEnd=function(url){
		delete this.conf.resource[i]
		if(Object.keys(this.conf.resource).length == 0){
			this.conf.isReady = true;
			if(_beforeData){
				this[_beforeData.func].apply(this, _beforeData.args);
			}
		}
	}.bind(this);
	for(var i in this.conf.resource){
		sUtils.loader[this.conf.resource[i]](i, function(){
			loadEnd(this.src || this.href)
		});
	}
}

SepoaContextMenuManager.prototype.setContextMenu = function(vp, option){
	if(this.conf.lastSelector){
		$(this.conf.lastSelector).removeClass(this.conf.lastSelector.substring(1));
		$.contextMenu('destroy');
	}
	
	if(typeof vp == 'string') {
		this.conf.lastSelector = vp;
	}else{
		vp.classList.add(this.conf.defaultClassName);
		this.conf.lastSelector = '.'+this.conf.defaultClassName;
	}
	$.contextMenu($.extend(true, {selector : this.conf.lastSelector, trigger: 'left', position : this._calcPosition, reposition: false}, option));
}

SepoaContextMenuManager.prototype.showContextMenu = function(vp, option){
	if(this.conf.lastSelector){
		$(this.conf.lastSelector).removeClass(this.conf.lastSelector.substring(1));
		$.contextMenu('destroy');
	}
	
	if(typeof vp == 'string') {
		this.conf.lastSelector = vp;
	}else{
		vp.classList.add(this.conf.defaultClassName);
		this.conf.lastSelector = '.'+this.conf.defaultClassName;
	}
	$.contextMenu($.extend(true, {selector : this.conf.lastSelector, trigger: 'left', position : this._calcPosition, reposition: false}, option));
	
	$(this.conf.lastSelector).contextMenu();
	vp.classList.remove(this.conf.lastSelector);
}

SepoaContextMenuManager.prototype._calcPosition = function(opt, x, y) {
	var pos = opt.$trigger[0].getBoundingClientRect();
	var positionTop = y ? y : pos.top;
	var positionLeft = x ? x : pos.left + pos.width;

	if(positionLeft + opt.$menu.width() > document.documentElement.offsetWidth) {
		positionLeft = positionLeft - opt.$menu.width();
	}
	
	if(positionTop + opt.$menu.height() > document.documentElement.offsetHeight) {
		positionTop = positionTop - opt.$menu.height();
	}
	
	opt.$menu.css({top:positionTop, left:positionLeft});
}