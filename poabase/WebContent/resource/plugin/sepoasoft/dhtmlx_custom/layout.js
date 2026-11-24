/**
 * dhtmlxLayout
 */
function SepoaDhtmlXLayoutObject(e, o, w){
	var base = dhtmlXLayoutObject.call(this, e, o, w);
	this.base.dhtmlxObject = this;
	for(var _idx in e.cells){
		var cOpt = e.cells[_idx];
		var c = this.cells(cOpt.id);
		
		//hide collapse button
		var flag = (!e.cells[_idx].button)?true:eval(e.cells[_idx].button);
	 	if(!flag){
	 		this.cells(e.cells[_idx].id).hideButton();
	 	}
		
		flag = (!cOpt.border)?true:eval(cOpt.border);
	 	if(!flag){
	 		this.cells(cOpt.id).hideBorder();
	 	}
	 	
	 	var hType = cOpt["header-type"];
	 	if(hType){
	 		c.cell.classList.add("sepoa_layout_cell_type_"+hType);
	 	}
	 	
	 	c.cell.appendChild($('<div class="filterModal" style="display:none;"></div>')[0]);
	 	c.cell.classList.add("dhx_cell_layout_"+c._idd);
		var arrow = $(c.cell.children[c.conf.idx.hdr]).find('.dhxlayout_arrow');
		arrow.addClass('xi-sepoa-custom');
	}
	var that = this;
	this.showIcon=[];
	
	function _doOnCellResize(){
		SUX.callEvent('onWindowResize',[]);
	}
	
	this.attachEvent('onPanelResizeFinish', _doOnCellResize);
	this.attachEvent('onExpand', _doOnCellResize);
	this.attachEvent('onCollapse', _doOnCellResize);
}
SepoaDhtmlXLayoutObject.prototype = Object.create(dhtmlXLayoutObject.prototype);
SepoaDhtmlXLayoutObject.prototype.constructor = SepoaDhtmlXLayoutObject;
SepoaDhtmlXLayoutObject.prototype.callCAPI = function(cid, fn, params){
	var a = this.cells(cid);
	return a[fn].apply(a, params);
}
SepoaDhtmlXLayoutObject.prototype.getButtonObject = function(c, id){
	return this.cells(c).conf._buttonObj.btn('layout_'+id);
}

dhtmlXLayoutCell.prototype.hideButton = function(){
	var obj = $(this.cell).find('.dhxlayout_arrow');
	if(typeof(obj) != "undefined" && obj != null){
		obj.css("display", "none");
	}
	obj = $(this.cell).find('.dhx_cell_hdr_text')
	obj.css("margin-right","0");
}
dhtmlXLayoutCell.prototype.showButton = function(){
	var obj = $(this.cell).find('.dhxlayout_arrow');
	if(typeof(obj) != "undefined" && obj != null){
		obj.css("display", "block");
	}
	obj = $(this.cell).find('.dhx_cell_hdr_text')
	obj.css("margin-right","");
}
dhtmlXLayoutCell.prototype.hideBorder = function(){
	obj = $(this.cell);
	obj.css('box-shadow','none');
	obj.find('.dhx_cell_hdr').css('border','none');
	obj.find('.dhx_cell_cont_layout').css('border','none');
}
dhtmlXLayoutCell.prototype.showBorder = function(){
	obj = $(this.cell);
	obj.css('box-shadow','');
	obj.find('.dhx_cell_hdr').css('border','');
	obj.find('.dhx_cell_cont_layout').css('border','');
}
dhtmlXLayoutCell.prototype._hdrUpdText = function() {
    var a = (this.conf.collapsed == true && this.conf.hdr.text_collapsed != null ? this.conf.hdr.text_collapsed : this.conf.hdr.text);
    var obj = $(this.cell.childNodes[this.conf.idx.hdr].firstChild);
    if(obj.find('.dhx_cell_hdr_text_icon').length <= 0){
    	var div = document.createElement('div');
    	div.className = 'dhx_cell_hdr_text_icon';
    	//div.style.display='none';
    	obj.append(div);
    }
    if(obj.find('span').length <= 0){
    	obj.append(document.createElement('span'));
    }
//    obj.find('span').html(a);

    const temp = document.createElement('div');
    temp.appendChild(document.createTextNode(htmlDecode(a)));

    const text = temp.innerHTML;
    obj.find('span').html(text);
};
dhtmlXLayoutCell.prototype.setHeaderTitleIcon = function(url) {
    //var a = (this.conf.collapsed == true && this.conf.hdr.text_collapsed != null ? this.conf.hdr.text_collapsed : this.conf.hdr.text);
    //var obj = $(this.cell.childNodes[this.conf.idx.hdr].firstChild);
    //var div = obj.find('.dhx_cell_hdr_text_icon')
    //if(div.length <= 0){
    //	var div = document.createElement('div');
    //	div.className = 'dhx_cell_hdr_text_icon'
    //	obj.append(div);
    //}else{
    //	div = div[0];
    //}
    //div.style.display = "block";
    //div.style.background= 'url("/theme/'+Sepoa.common.THEME+'/imgs/icon/'+url+'") no-repeat scroll right center / auto 18px';
};
dhtmlXLayoutCell.prototype.showIcon = function(type){
	//if(!type){
	//	if(this.conf._headerButtonCustom){
	//		for(var i in this.conf.customBtnList){
	//			var t = this.conf._buttonObj.btn("layout_"+this.conf.customBtnList[i]);
	//			if(t)
	//				t.setShow();
	//		}
	//	}else{
	//		$(this.cell).find('.dhx_cell_hdr_text .dhx_cell_hdr_icon').not('.dhx_cell_hdr_icon_menu').addClass('dhx_cell_hdr_icon_show');
	//	}
	//	return;
	//}
	//type = type.split(",");
	//for(var _idx in type){
	//	if(this.conf._headerButtonCustom){
	//		var t = this.conf._buttonObj.btn("layout_"+type[_idx]);
	//		if(t)
	//			t.setShow();
	//	}else{
	//		obj = $(this.cell).find('.dhx_cell_hdr_text .dhx_cell_hdr_icon.dhx_cell_hdr_icon_'+type[_idx]);
	//		obj.addClass('dhx_cell_hdr_icon_show');
	//	}
	//}
}
dhtmlXLayoutCell.prototype.hideIcon = function(type){
	//if(!type){
	//	if(this.conf._headerButtonCustom){
	//		for(var i in this.conf.customBtnList){
	//			var t = this.conf._buttonObj.btn("layout_"+this.conf.customBtnList[i]);
	//			if(t)
	//				t.setHide();
	//		}
	//	}else{
	//		$(this.cell).find('.dhx_cell_hdr_text .dhx_cell_hdr_icon').not('.dhx_cell_hdr_icon_menu').removeClass('dhx_cell_hdr_icon_show');
	//	}
	//	return;
	//}
	//type = type.split(",");
	//for(var _idx in type){
	//	if(this.conf._headerButtonCustom){
	//		var t = this.conf._buttonObj.btn("layout_"+type[_idx]);
	//		if(t)
	//			t.setHide();
	//	}else{
	//		obj = $(this.cell).find('.dhx_cell_hdr_text .dhx_cell_hdr_icon.dhx_cell_hdr_icon_'+type[_idx]);
	//		obj.removeClass('dhx_cell_hdr_icon_show');
	//	}
	//}
}
dhtmlXLayoutCell.prototype.showMenuIcon = function(){
	//obj = $(this.cell).find('.dhx_cell_hdr_text .dhx_cell_hdr_icon_menu').css("display", "block");
}
dhtmlXLayoutCell.prototype.hideMenuIcon = function(){
	//obj = $(this.cell).find('.dhx_cell_hdr_text .dhx_cell_hdr_icon_menu').css("display", "none");
}
dhtmlXLayoutCell.prototype.initIcon = function(type, flag, sepoaObject){
	//this.conf._headerButtonCustom = !!flag;
	//
	//$(this.cell).find('.dhx_cell_hdr_icon').remove();
	//
	//if(flag){
	//	type = type;
	//	
	//}else{
	//	type = type.split(",");
	//}
	//
	//if(!this.conf._buttonObj){
	//	this.conf._buttonObj = new SepoaButtonObject();
	//}
	//var btnObj = this.conf._buttonObj;
	//
	//this.conf.customBtnList = [];
	//for(var _idx in type){
	//	var vp = $(this.cell).find('.dhx_cell_hdr_text');
	//	var id = (flag)?type[_idx].id:type[_idx];
	//	var t = this;
	//	
	//	if(id != 'menu' && flag){
	//		var div;
	//		this.conf.customBtnList.push(id);
	//		if(!this.conf._buttonDiv){
	//			div = this.conf._buttonDiv = document.createElement('div');
	//			if(vp.find('span').length > 0){
	//				vp.find('span').after(div);
	//			}else{
	//				vp.prepend(div);
	//			}
	//			div.className='dhx_cell_hdr_icon dhx_cell_hdr_icon_sepoabutton';
	//		}else{
	//			div = this.conf._buttonDiv;
	//		}
	//		
	//		type[_idx].div = div;
	//		type[_idx].id  = "layout_"+id;
	//		type[_idx].type = type[_idx].type || "layout_"+id;
	//		type[_idx].text = (type[_idx].text||undefined);
	//		type[_idx].target = this;
	//		type[_idx].onclick = function(id){
	//			var targetFrame = null;
	//			if(type == 'menu'){
	//				targetFrame = "MAINFRAIME";
	//			}else{
	//				
	//				var f = t.getFrame();
	//				if(f){
	//					targetFrame = f.contentWindow;
	//				}else{
	//					var area = $(t.cell).find('div.sepoaux.screen_area');
	//					if(area.length > 0){
	//						targetFrame = area[0].id
	//					}else{
	//						targetFrame = window;
	//					} 
	//				}
	//			}
	//			if(targetFrame instanceof Window){
	//				targetFrame = getChildScreenId(targetFrame.document.body)
	//			}
	//			
	//			var sId = getScreenId(this.cell);
	//			if(targetFrame && sId){
	//				SUX.pools[sId].callFunction(targetFrame, ["Sepoa", "callEvent"], ['onButtonClick', [id.replace("layout_","")]])
	//			}else{
	//				console.log('not found target object')
	//			}
	//			return;
	//		}
	//		
	//		btnObj.addButton(type[_idx]);
	//	}else{
	//		var div = document.createElement('div');
	//		if(id == 'menu'){
	//			vp.prepend(div);
	//		}else if(vp.find('span').length > 0){
	//			vp.find('span').after(div);
	//		}else{
	//			vp.prepend(div);
	//		}
	//		
	//		div.className='dhx_cell_hdr_icon dhx_cell_hdr_icon_'+id;
	//		div.onclick = function(e){
	//			var a = this.className.split(" ");
	//			if(a.indexOf("dhx_cell_hdr_icon") >= 0){
	//				var b = a[a.indexOf("dhx_cell_hdr_icon")+1].split("_");
	//				var type = [b[b.length - 1]];
	//				var target = null;
	//				if(type == 'menu'){
	//					target = SUX.root;
	//				}else{
	//					try{
	//						target = t.getFrame().contentWindow;
	//					}catch (e) {
	//						target = window;
	//					}
	//				}
	//				if(!target) return;
	//				target.Sepoa.callEvent('onButtonClick', type);
	//			}
	//		};
	//	}
	//}
	//this.hideIcon();
}
function setLayoutText(obj, c, text){
	this[obj].cells(c).setText(text);
}