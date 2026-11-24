/**
 * dhtmlxList
 */
function SepoaDhtmlXList(a){
	var vp = dhtmlx.toNode(a.container);
	vp.dhtmlxObject = this;
	vp.classList.add('sepoaux_list');
	
	if(a.isDynamic < 1){
		if(!a.pager) a.pager = {};                                          
		var pageDiv = dhtmlx.toNode(a.pager.container || (vp.id+'_paging'));
		if(!pageDiv){                                                       
			pageDiv = document.createElement('div');                          
			pageDiv.id = vp.id+'_paging';                                     
			pageDiv.style.height = '22px';                                    
			pageDiv.style.width = '100%';                                     
			pageDiv.style.display="none";                                     
			vp.parentElement.insertBefore(pageDiv, vp);                       
		}                                                                   
		a.pager.container = pageDiv.id;                                     
		a.pager.size = a.pager.size || 10;                                  
		a.pager.group = a.pager.group || 15;                                
	}
	var base = dhtmlXList.call(this, a);
	
	if(a.isDynamic > 0){
		this.config.dynamic = {
			url:"",
			status:false,
			totalCount:0,
			size:a.isDynamic
		};
	}
	
	this._fake_load = this.load;
	this.load = function(url){
		if(this.config.dynamic){
			this.config.dynamic.url = url;
		}
		checkContextUrl(url);
		
		if(window.CONTEXT_NAME && url.indexOf(window.CONTEXT_NAME) != 0){
			url = (window.CONTEXT_NAME || '') + url;
		}
		
		this._fake_load(url);
	}
	
	this._onLoad = function(e, c, a) {
		this._doQueryModalEnd();
		
		if(Array.isArray(e) && !(e instanceof Array)){
			e = [].slice.call(e);
		}
		
        this.data._parse(this.data.driver.toObject(e, c));
        if(this.config.dynamic && this.config.dynamic.status){
        	this.config.dynamic.status = false;
        	this.checkNextPage(true);
        }else{
        	this.callEvent("onXLE", []);
        }
        
        if (this._readyHandler) {
            this.data.detachEvent(this._readyHandler);
            this._readyHandler = null
        }
    }
	
	var that = this;
	vp.onscroll = function(){
		that.callEvent("onScroll",[]);
	}
	this.attachEvent("onScroll",function(){
		this.checkNextPage();
	});
	this.attachEvent("onXLE", function(){
	    if(this.config.dynamic){
	    	if(!this.config.dynamic.status){
	    		this.config.dynamic.totalCount = (this.dataCount() > 0)?this.get(this.idByIndex(0)).__TOTAL_COUNT__:0;
	    	}
	    	this.config.dynamic.status = false;
	    }
	    this.checkNextPage(true);
	});
	
	this.attachEvent("onXLS", function(){
		if(this.config.pager){
			this.clearAll(false);
			this.config.pager.data.page=0;
		}else if(this.config.dynamic && !this.config.dynamic.status){
			this.clearAll(false);
			this.config.dynamic.totalCount=0;
		}
		this._doQueryDuring();
	});
	
	if(a.isDynamic < 1){
		var fake_refresh = this.refresh;
		this.refresh = function(){
			this.config.pager.select(0);
			fake_refresh();
			this.checkNextPage(true);
		}
	}
	
	this.attachEvent("onEditKeyPress", function(e, g, c) {
        if (e == 13 && !c) {
            this.stopEdit()
        } else {
            if (e == 27) {
                this.stopEdit(true)
            }
        }
    });
	
	this._org_get = this.get;
	this.get=function(a, upper){
		var data = this._org_get(a);
		if(upper){
			var tmp = {};
			for(var i in data){
				tmp[i.toUpperCase()] = data[i];
			}
			data = tmp;
		}
		return data;
	}
}
SepoaDhtmlXList.prototype = Object.create(dhtmlXList.prototype);
SepoaDhtmlXList.prototype.constructor = dhtmlXList;
SepoaDhtmlXList.prototype._doQueryModalEnd = function(){
	sUtils.progress.off();
	return true;
}
SepoaDhtmlXList.prototype._doQueryDuring = function(){
	sUtils.progress.on();
	return true;
}
SepoaDhtmlXList.prototype.checkNextPage = function(flag) {
	var vp = this.$view;
	
	if(this.config.pager){
		var data = this.config.pager.data;
		if(data.max != 0 && data.page < data.max ){
			var sh = vp.scrollHeight -  vp.offsetHeight;
			sh = (sh < 0)?0:sh;
			var tsh = sh * 0.99; 
			var ns = vp.scrollTop;
			if(tsh < ns || sh == 0){
				var before = this._dataobj.innerHTML;
				this.config.pager.select((data.page*1)+1);
				this._dataobj.innerHTML = before+this._dataobj.innerHTML;
				if(flag) this.checkNextPage(true);
			}
		}
	}else if(this.config.dynamic && !this.config.dynamic.status){
		var pos = this.dataCount();
		var need = this.config.dynamic.totalCount - pos;
		need = (need > this.config.dynamic.size)?this.config.dynamic.size : need;
		if(need > 0){
			var sh = vp.scrollHeight -  vp.offsetHeight;
			var tsh = sh * 0.99; 
			var ns = vp.scrollTop;
			if(tsh < ns || sh == 0){
				this.config.dynamic.status = true;
				var url = this.config.dynamic.url;
				url = url + ((url.indexOf("?") == -1) ? "?" : "&") + "posStart=" + pos + "&count=" + need;
				this._fake_load(url);
			}
		}
	}
}
SepoaDhtmlXList.prototype.setValue = function(r, c, value) {
	this.item(r)[c] = value
	this.refresh();
}
SepoaDhtmlXList.prototype.getValue = function(r, c) {
	return this.item(r)[c]
}
SepoaDhtmlXList.prototype.postConnection = function(url, user_param){
	var param = [];
	param.push((this.dataOutput||dataOutput).getParams());
    if(!(!user_param)){
		for(var _idx in user_param){
			param.push(user_param[_idx]);
		}
	}
	param.push("requestMode=postConnection");
	var post = param.join("&") || "";
	
	if(url.indexOf("?") < 0){
		url += "?"
	}
	
	this.load(url+post);
}
SepoaDhtmlXList.prototype.postTransaction = function(url, user_param, selected){
	selected = (selected||false);
	var a1 = this._getSelectedData(selected);
	var param = [];
	var objNames = [this.id];
	param.push((this.dataOutput || dataOutput).getParams());
	param.push(this._getSepoaFormatData(a1));
	param.push("defaultGrid="+this.id);
	this._otherGridData = {};
	param.push("gridObjNames="+objNames.join(","));
	if(!(!user_param)){
		for(var _idx in user_param){
			param.push(user_param[_idx]);
		}
	}
	param.push("requestMode=postTransaction");
	var post = param.join("&");
	this.callEvent("onTXS", [this]);
	var call;
	var type;
	if (arguments.length == 2 && typeof call != "function"){
		type=call;
		call=null;
	}
	type="xml";

	if (!this.xmlFileUrl)
		this.xmlFileUrl=url;
	this._data_type=type;

	var that = this;
	var xmlLoader = function(xml){
		var ids = [];
		if (a1)
			for (var key in a1)
				ids.push(key);
		return that.afterUpdate(that,xml,ids);
	};
	this._doQueryDuring();
	dhx4.ajax.post(url, (post||""), xmlLoader);
}
SepoaDhtmlXList.prototype._getSelectedData = function(selected){
	var out={};
	var has_one = false;
	
	if(selected){
		var selected_array = this.getSelectedArray(selected);
		for(var i=0;i<selected_array.length;i++){
			var id=selected_array[i];
			out[id]=this.item(id);
			has_one = true;			
		}		
	}else{
		var p = this.data.pull;
		for(var id in p){
			out = p;
			has_one = true;
			break;
		}
	}
	return has_one?out:null;
}
SepoaDhtmlXList.prototype.getSelectedArray=function(){
	var ar = this.getSelected();
	if(typeof(ar) == "string"){
		ar = [ar]
	}
	return ar;
}
SepoaDhtmlXList.prototype.afterUpdate=function(that,xml,id){
	// try to use json first
	if (window.JSON){
		try{
			var tag = JSON.parse(xml.xmlDoc.responseText);
			var action = tag.action || this.getState(id) || "updated";
			var sid = tag.sid || id[0];
			var tid = tag.tid || id[0];
			that.afterUpdateCallback(sid, tid, action, tag);
			return;
		} catch(e){
		}
	}
	// xml response
	this._doQueryModalEnd();
	var top = dhx4.ajax.xmltop("rows", xml.xmlDoc); // fix incorrect content
													// type in IE
	var atag= top.getElementsByTagName("userdata");
	var btag = {};
	for (var i=0; i<atag.length; i++){
    	btag[atag[i].getAttribute("name")]=atag[i].innerHTML;
	}
	that.afterUpdateCallback(btag);
	
}

SepoaDhtmlXList.prototype.afterUpdateCallback = function(btag) {
	
	this._doSaveEnd(btag);
}

SepoaDhtmlXList.prototype._doSaveEnd = function(obj){
	
	var attcnt = Object.keys(obj).length;
	if(obj.status == "SESSIONCLOSE"){
		sUtils.system.login();
		return;
	}
	
	this.callEvent('onTXE',[obj]);
}
SepoaDhtmlXList.prototype._getSepoaFormatData = function(grid_data){
	var _rdata = [];
	var col_ids = [];
	
	for(var _rkey in grid_data){
		if(col_ids.length == 0){
			for(var _ckey in grid_data[_rkey]){
				col_ids.push(_ckey);
			}
			_rdata.push(col_ids.join(Sepoa.common.FIELD).toUpperCase());
		}
		var _cdata = [];
		for(var _ckey in col_ids){
			_cdata.push((grid_data[_rkey][col_ids[_ckey]] !== null)?grid_data[_rkey][col_ids[_ckey]]:"");
		}
		_rdata.push(_cdata.join(Sepoa.common.FIELD));
	}
	var encodeData = encodeUrl(_rdata.join(Sepoa.common.FIELD+Sepoa.common.LINE)+Sepoa.common.FIELD+Sepoa.common.LINE);
	return this.id+"="+encodeData;
}
SepoaDhtmlXList.prototype.setDataOutput = function(obj){
	this.dataOutput = obj;
}