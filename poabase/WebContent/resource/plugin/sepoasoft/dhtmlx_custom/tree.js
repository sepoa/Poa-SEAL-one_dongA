/**
 * dhtmlxTree 
 */
function SepoaDhtmlXTreeView(o, l, c, a){
	var base = dhtmlXTreeView.call(this, o, l, c, a);
	this._loadData = function(v, w, x) {
        if (arguments.length == 2) {
            x = w;
            w = null
        }
        var u = null;
        if (arguments.length == 3) {
            x = arguments[2]
        }
        if (typeof(v) == "string") {
            var s = v.replace(/^\s{1,}/, "").replace(/\s{1,}$/, "");
            var C = new RegExp("^<" + this._dhxdataload.xmlRootTag);
            if (C.test(s.replace(/^<\?xml[^\?]*\?>\s*/, ""))) {
                u = dhx4.ajax.parse(v);
                if (u != null) {
                    u = this[this._dhxdataload.xmlToJson].apply(this, [u])
                }
            }
            if (u == null && (s.match(/^[\s\S]*{[.\s\S]*}[\s\S]*$/) != null || s.match(/^[\s\S]*\[[.\s\S]*\][\s\S]*$/) != null)) {
                u = dhx4.s2j(s)
            }
            if (u == null) {
                this.callEvent("onXLS", []);
                var r = [];
                if (typeof(this._dhxdataload.onBeforeXLS) == "function") {
                    var s = this._dhxdataload.onBeforeXLS.apply(this, [v]);
                    if (s != null && typeof(s) == "object") {
                        if (s.url != null) {
                            v = s.url
                        }
                        if (s.params != null) {
                            for (var y in s.params) {
                                r.push(y + "=" + encodeURIComponent(s.params[y]))
                            }
                        }
                    }
                }
                var A = this;
                var o = function(a) {
                	A.clearAll();
                    var D = null;
                    if ((a.xmlDoc.getResponseHeader("Content-Type") || "").search(/xml/gi) >= 0 || (a.xmlDoc.responseText.replace(/^\s{1,}/, "")).match(/^</) != null) {
                        D = A[A._dhxdataload.xmlToJson].apply(A, [a.xmlDoc.responseXML])
                    } else {
                        D = dhx4.s2j(a.xmlDoc.responseText)
                    }
                    if (D != null) {
                        A[A._dhxdataload.initObj].apply(A, [D, v])
                    }
                    var rt = a.xmlDoc.responseXML.getElementsByTagName('Userdata');
                    if(rt){
                    	rt = rt[0].attributes
                    	var tt = {}
                    	for(var _idx in rt){
                    		tt[rt[_idx].name] = rt[_idx].value
                    	}
                    	rt = tt;
                    }
                    A.callEvent("onXLE", [rt]);
                    if (x != null) {
                        if (typeof(x) == "function") {
                            x.apply(A, [])
                        } else {
                            if (typeof(window[x]) == "function") {
                                window[x].apply(A, [])
                            }
                        }
                    }
                    o = x = null;
                    D = a = A = null
                };
                r = r.join("&") + (typeof(w) == "string" ? "&" + w : "");
                if (dhx4.ajax.method == "post") {
                    dhx4.ajax.post(v, r, o)
                } else {
                    if (dhx4.ajax.method == "get") {
                        dhx4.ajax.get(v + (r.length > 0 ? (v.indexOf("?") > 0 ? "&" : "?") + r : ""), o)
                    }
                }
                return
            }
        } else {
            if (typeof(v.documentElement) == "object" || (typeof(v.tagName) != "undefined" && typeof(v.getElementsByTagName) != "undefined" && v.getElementsByTagName(this._dhxdataload.xmlRootTag).length > 0)) {
                u = this[this._dhxdataload.xmlToJson].apply(this, [v])
            } else {
                u = window.dhx4._copyObj(v)
            }
        }
        if (u != null) {
            this[this._dhxdataload.initObj].apply(this, [u])
        }
        if (x != null) {
            if (typeof(x) == "function") {
                x.apply(this, [])
            } else {
                if (typeof(window[x]) == "function") {
                    window[x].apply(this, [])
                }
            }
            x = null
        }
    };
        this.loadA = this.load
    this.load = function(url){
	    checkContextUrl(url);
    	this.loadA(url)
    }
    this.loadStructA = this.loadStruct
    this.loadStruct = function(url){
   	    checkContextUrl(url);
    	this.loadStructA(url)
    } 
}
SepoaDhtmlXTreeView.prototype = Object.create(dhtmlXTreeView.prototype);
SepoaDhtmlXTreeView.prototype.constructor = dhtmlXTreeView;
SepoaDhtmlXTreeView.prototype.getSubItems = function(g) {
    var e = [];
    if(g.toLowerCase() == "root" || g == "0") g = undefined;
    for (var c in this.items) {
        if (this.items[c].pId == g) {
            e.push(c)
        }
    }
    return e
};
SepoaDhtmlXTreeView.prototype.openItemLevel = function(g) {
    if(!g) g=1;
    for (var c in this.items) {
        if (this.items[c].level == g) {
        	this.openItem(c);
        }
    }
};
SepoaDhtmlXTreeView.prototype.closeItemLevel = function(g) {
    if(!g) g=1;
    for (var c in this.items) {
        if (this.items[c].level == g) {
        	this.closeItem(c);
        }
    }
};
SepoaDhtmlXTreeView.prototype.selectItemByUserData = function(UserDataId) {
    if(!UserDataId) return;
    for (var c in this.items) {
    	if(this.items[c].userdata.ID == UserDataId){
    		for(var i=1; i<=this.items[c].level;i++){
				this.openItemLevel(i);
			}
    		this.selectItem(this.items[c].id);
    	}
    }
};
SepoaDhtmlXTreeView.prototype.checkChilds= function(id) {
    var child = this.getSubItems(id);
	if(child.length < 1) return id;
	this.openItem(id);
    for (var i in child) {
    	this.checkItem(child[i]);
    }
};
SepoaDhtmlXTreeView.prototype.uncheckChilds= function(id) {
	var child = this.getSubItems(id);
	if(child.length < 1) return id;
	for (var i in child) {
		this.uncheckItem(child[i]);
	}
};
SepoaDhtmlXTreeView.prototype.getLastLevelItems= function(flag) {
	var itemArr = [];
	if(!flag){
		for (var c in this.items) {
			if(this.getSubItems(this.items[c].id).length < 1){
				itemArr.push(this.items[c].id);
			}
		}
	}else if(flag.toLowerCase() == 'checked'){
		var checkedItem = this.getAllChecked();
		for(var c in checkedItem){
			if(this.getSubItems(checkedItem[c]).length < 1){
				itemArr.push(checkedItem[c]);
			}
		}
	}
	return itemArr;
};
SepoaDhtmlXTreeView.prototype.showItem = function(id) {
	var top = 0;
	var sub = false;
	
	while (id){
		var node = this.items[id];
		if (node){
			top += node.item.offsetTop + (sub?node.item.firstChild.offsetHeight:0);
			id = node.pId;
			sub = true;
		} else
			break;
	}

	this.cont.scrollTop = top;
};
