dhtmlx.DataStore.prototype._parse=function(h) {
    this.callEvent("onParse", [this.driver, h]);
    if (this._filter_order) {
        this.filter()
    }
    var l = this.driver.getInfo(h);
    if (l._key) {
        dhtmlx.security_key = l._key
    }
    var g = this.driver.getRecords(h);
    var n = (l._from || 0) * 1;
    if (n === 0 && this.order[0]) {
        n = this.order.length
    }
    var c = 0;
    for (var e = 0; e < g.length; e++) {
        var a = this.driver.getDetails(g[e]);
        var m = this.id(a);
        a.randomseed=dhx.newId();
        if (!this.pull[m]) {
            this.order[c + n] = m;
            c++
        }
        this.pull[m] = a;
        if (this.extraParser) {
            this.extraParser(a)
        }
        if (this._scheme) {
            if (this._scheme.$init) {
                this._scheme.$update(a)
            } else {
                if (this._scheme.$update) {
                    this._scheme.$update(a)
                }
            }
        }
    }
    for (var e = 0; e < l._size; e++) {
        if (!this.order[e]) {
            var m = dhtmlx.uid();
            var a = {
                id: m,
                $template: "loading"
            };
            this.pull[m] = a;
            this.order[e] = m
        }
    }
    this.callEvent("onStoreLoad", [this.driver, h]);
    this.refresh()
}


SepoaDhtmlXDataView = function(a){
	this.COL_DEL = Sepoa.common.FIELD;
	this.ROW_DEL = Sepoa.common.LINE;
	this.id = "dataview"
	var base = dhtmlXDataView.call(this, a);
	
	this._fake_load = this.load;
	this.load = function(url){
		if(this.config.dynamic){
			this.config.dynamic.url = url;
		}
		checkContextUrl(url);
		this._fake_load(url);
	}
	
	this.$view.style.overflowY = "auto";
}
SepoaDhtmlXDataView.prototype = Object.create(dhtmlXDataView.prototype);
SepoaDhtmlXDataView.prototype.constructor = SepoaDhtmlXDataView;
SepoaDhtmlXDataView.prototype.postTransaction = function(url, user_param, selected){
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
	this.callEvent("onXLS", [this]);
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
SepoaDhtmlXDataView.prototype._getSelectedData = function(selected){
	var out={};
	var has_one = false;
	
	if(selected){
		var selected_array = this.getSelected();
		
		for(var i=0;i<selected_array.length;i++){
			var id=selected_array[i];
			out[id]=this.get(id);
			has_one = true;			
		}		
	}else{
		var p = this.data.pull;
		for(var id in p){
			out = p;
			has_ont = true;
			break;
		}
	}
	return has_one?out:null;
}
SepoaDhtmlXDataView.prototype.afterUpdate=function(that,xml,id){
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
	var top = dhx4.ajax.xmltop("data", xml.xmlDoc);
	if (!top){
		top = dhx4.ajax.xmltop("rows", xml.xmlDoc);
		
		var atag = dhx4.ajax.xpath("/rows/userdata", top);
		var btag = {}
		for(var i=0;i<atag.length; i++){
			var a = atag[i];
			btag[a.getAttribute("name")] = (a.textContent || a.text || "").trim() 
		}
		
		that.callEvent("onTXE", [btag])
	}else if(top.tagName == "DIV"){
		return this.cleanUpdate(id);
	}else{
		var atag=dhx4.ajax.xpath("//data/action", top);
		if (!atag.length) return this.cleanUpdate(id);
	
		for (var i=0; i<atag.length; i++){
	    	var btag=atag[i];
			var action = btag.getAttribute("type");
			var sid = btag.getAttribute("sid");
			var tid = btag.getAttribute("tid");
			
			that.afterUpdateCallback(sid,tid,action,btag);
		}
	}
}
SepoaDhtmlXDataView.prototype.cleanUpdate = function(id) {
	Alert("load XML ERROR");
}
SepoaDhtmlXDataView.prototype.afterUpdateCallback = function(sid, tid, action, btag) {
	var marker = sid;
	var correct=(action!="error" && action!="invalid");
	//if (!correct) this.set_invalid(sid,action);
	this._doSaveEnd(btag);
}

SepoaDhtmlXDataView.prototype._doSaveEnd = function(obj){
	var return_obj = {};
	var attcnt = obj.attributes.length;
	for(var i=0;i<attcnt;i++){
		var node_name = obj.attributes[i].name;
		return_obj[node_name] = obj.attributes[i].nodeValue;
	}
	if(return_obj.status == "SESSIONCLOSE"){
		sUtils.system.login();
		return;
	}
	
	this.callEvent('onTXE',[return_obj]);
}
SepoaDhtmlXDataView.prototype._getSepoaFormatData = function(grid_data){
	var _rdata = [];
	var col_ids = [];
	
	for(var _rkey in grid_data){
		if(col_ids.length == 0){
			for(var _ckey in grid_data[_rkey]){
				col_ids.push(_ckey);
			}
			_rdata.push(col_ids.join(this.COL_DEL));
		}
		var _cdata = [];
		for(var _ckey in col_ids){
			_cdata.push((grid_data[_rkey][col_ids[_ckey]] !== null)?grid_data[_rkey][col_ids[_ckey]]:"");
		}
		_rdata.push(_cdata.join(this.COL_DEL));
	}
	var encodeData = encodeUrl(_rdata.join(this.COL_DEL+this.ROW_DEL)+this.COL_DEL+this.ROW_DEL);
	return this.id+"="+encodeData;
}
SepoaDhtmlXDataView.prototype._doQueryModalEnd = function(){
	var t = (SUX.getModuleTop().Sepoa.request.isPopup)?SUX.getModuleTop().selfObj:SUX.root;
	var w = (SUX.getModuleTop().Sepoa.request.isPopup)?SUX.getModuleTop().parent.SUX.getModuleTop():SUX.getModuleTop();
	if(t.progressOff)
		t.progressOff(undefined,w);
	return true;
}
SepoaDhtmlXDataView.prototype._doQueryDuring = function(){
	var t = (SUX.getModuleTop().Sepoa.request.isPopup)?SUX.getModuleTop().selfObj:SUX.root;
	var w = (SUX.getModuleTop().Sepoa.request.isPopup)?SUX.getModuleTop().parent.SUX.getModuleTop():SUX.getModuleTop();
	if(t.progressOn)
		t.progressOn(w);
	return true;
}
SepoaDhtmlXDataView.prototype.setDataOutput = function(obj){
	this.dataOutput = obj;
}