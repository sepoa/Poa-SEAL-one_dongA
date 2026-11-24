SepoaHeaderToolbarObject=function(c){
	dhtmlx.extend(this, dhtmlx.Settings);
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.config=$.extend(true, {
		style: 'default'
	}, c);
	this.renderBase(); 
}
SepoaHeaderToolbarObject.prototype.renderBase=function(){
	var c = this.cont = this.config.parent.appendChild(document.createElement('div'));
	c.classList.add('sepoa-header-base');
	c.classList.add('sepoa-header-' + this.config.style);
	
	this._area = {
		left:c.appendChild($('<div class="sepoa-header-box sepoa-header-box-left"></div>')[0]),
		center:c.appendChild($('<div class="sepoa-header-box sepoa-header-box-center"></div>')[0]),
		right:c.appendChild($('<div class="sepoa-header-box sepoa-header-box-right"></div>')[0])
	}
	
	for(var area in this._area){
		if(this.config[area]){
			for(var i in this.config[area]){
				this.addItem(area, this.config[area][i]);
			}
		} else {
			this.addItem(area, {type: 'dummy'});
		}
	}
}
SepoaHeaderToolbarObject.prototype.getLogoTag=function(){
	return $('.sepoa-header-logo img')[0];
}
SepoaHeaderToolbarObject.prototype.addItem=function(pos, itemInfo){
	var item = this._area[pos].appendChild(document.createElement('div'));
	item.classList.add('sepoa-header-item');
	item.classList.add('sepoa-header-item-'+itemInfo.type);
	
	if("text" == itemInfo.type){
		item.innerHTML = '<div>'+itemInfo.text+'</div>';
	} else if("img" == itemInfo.type){
		var img = item.appendChild(document.createElement('img'))
		img.id=itemInfo.id
		img.onerror=function(){this.style.display="none"};
		img.src=itemInfo.img
	} else if("icon" == itemInfo.type){
		item.innerHTML = "<div class='"+itemInfo.img+"'></div>"
	} else if("logo" == itemInfo.type) {
		var div = $('<div class="sepoa-header-logo"><img></img></div>')[0];
		item.appendChild(div);
		
		var img = this.getLogoTag();
		img.src = itemInfo.img;
	} else if("dummy" == itemInfo.type) {
		item.innerHTML = "";
	}
	if(itemInfo.cursor){
		item.style.cursor=itemInfo.cursor
	}
	if(item.firstChild) {
		item._info = itemInfo;
		item.onclick=dhtmlx.bind(function(e){
			this.callEvent('onClick', [e.currentTarget._info.id])
		},this)
	}
	if(itemInfo.tooltip) {
		item.title = itemInfo.tooltip;
	}
}