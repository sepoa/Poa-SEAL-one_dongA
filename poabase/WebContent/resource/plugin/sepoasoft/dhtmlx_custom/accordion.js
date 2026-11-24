/**
 * dhtmlxAccordion
 */
function SepoaDhtmlXAccordion(h, m){
	var flexible = h.flexible || 0;
	var autoclose = h.autoclose || false;
	dhtmlXAccordion.call(this, h, m);
	this.base.dhtmlxObject = this;
	this.base.classList.add('sepoaux_accordion');
	
	this.conf.flexible = flexible;
	this.conf.autoclose = autoclose;
	this.conf.cellOpened = false;
	
	if(this.conf.flexible){
		this.cont.style.height = '';
		this.conf.def_height = this.conf.flexible;
		this.cont.addEventListener("transitionend", function(){
			this.callEvent("onSetSizes", []);
		}.bind(this))
	}
	
	if(this.conf.autoclose !== false) {
		SUX.getModuleTop().addEventListener('click', function(e) {
			this.callEvent('onCellAutoClose', [e, this]);
		}.bind(this));
	}
	
	this.attachEvent('onCellAutoClose', function(e, obj) {
		var cellID = obj.conf.on_active_id;
		var cellOpened = obj.conf.cellOpened;
		
		if(cellOpened) {
			if($(e.target).prop("tagName").toLowerCase() == "body") return;
			
			if(!$(e.target).parents('div.dhxacc_cont')[0]) {
				if(cellOpened) {
					obj.conf.cellOpened = false;
					obj.cells(cellID).close();
				}
			}
		}
	});
	
    var g = this;
    var e = window.dhx4.transDetect();
    this.addItem = function(x, w, r, o, v) {
        if (x == null) {
            x = "a" + window.dhx4.newId()
        }
        while (this.t[x] != null) {
            x = "a" + window.dhx4.newId()
        }
        var a = new SepoaDhtmlXAccordionCell(x, this);
        a.conf.skin = this.conf.skin;
        a.setText(w);
        if (v != null) {
            a.setIcon(v)
        }
        a.cell._accId = x;
        a.cell.childNodes[a.conf.idx.hdr].onselectstart = function(y) {
            y = y || event;
            if (y.preventDefault) {
                y.preventDefault()
            } else {
                y.returnValue = false
            }
            return false
        };
        a.cell.childNodes[a.conf.idx.hdr].onclick = function(A) {
            A = A || event;
            if (A.preventDefault) {
                A.preventDefault()
            } else {
                A.returnValue = false
            }
            if (g._dnd != null && g._dnd.ofs == true) {
                return
            }
            var y = (A.target || A.srcElement);
            var C = null;
            while (y._accId != this && C == null) {
                if (y._accId != null) {
                    C = y._accId
                } else {
                    y = y.parentNode
                }
            }
            if (C != null) {
                g._hdrClick(C)
            }
        };
        this.t[x] = {
            cell: a
        };
        this.conf.total_count++;
        if (this.conf.multi_mode) {
            if (typeof(r) == "undefined") {
                r = true
            }
            this.cont.appendChild(a.cell);
            a.conf.opened = (r == true);
            if (typeof(o) == "undefined" || o == null || o == "*") {
                if (o == "*") {
                    a.conf.h_auto = true
                }
                o = this.conf.def_height
            } else {
                o = Math.max(parseInt(o), 30)
            }
            a._setSize(this.ofs.m.left, 0, this.conf.cont_w || this._getAvailItemWidth(), a.conf.opened ? o : a._getHdrHeight());
            a.cell.style.marginTop = (a.cell == this.cont.firstChild ? this.ofs.m.first : this.ofs.m.between) + "px";
            if (r != true) {
                a.conf.size.h = o;
                a._adjustCell();
                a.cell.className += " dhx_cell_closed"
            }
            this._adjustOpened();
            if (this._dnd != null) {
                this._dndAttachEvent(x)
            }
        } else {
            r = (this.conf.last_opened == null ? true : window.dhx4.s2b(r));
            var u = this.conf.flexible || this._updateCellsHeight();
            if (this.conf.last_opened != null) {
                if (r) {
                    this._closeItem(this.conf.last_opened, false)
                } else {
                    var s = this.t[this.conf.last_opened].cell;
                    s._setSize(s.conf.size.x, s.conf.size.y, s.conf.size.w, u);
                    s = null
                }
            }
            this.cont.appendChild(a.cell);
            a.conf.opened = r;
            a.cell.style.marginTop = String(a.cell == this.cont.firstChild ? this.ofs.s.first : this.ofs.s.between) + "px";
            a._setSize(this.ofs.s.left, 0, this.conf.cont_w || this._getAvailItemWidth(), r ? u : a._getHdrHeight());
            if (r) {
                this.conf.last_opened = x
            } else {
                a.conf.size.h = u;
                a._adjustCell();
                a.cell.className += " dhx_cell_closed"
            }
        }
        a = null;
        return this.t[x].cell
    }
    this._adjustCont = function() {
        var n = this.conf.ofs.t;
        for (var m in this.conf.ofs_nodes.t) {
            n += (this.conf.ofs_nodes.t[m] == true ? this.dataNodes[m].offsetHeight : 0)
        }
        var e = this.conf.ofs.b;
        for (var m in this.conf.ofs_nodes.b) {
            e += (this.conf.ofs_nodes.b[m] == true ? this.dataNodes[m].offsetHeight : 0)
        }
        var f = this.conf.last_opened?this.cells(this.conf.last_opened).conf.opened:null;
        this.cont.style.left = this.conf.ofs.l + "px";
        this.cont.style.width = this.base.clientWidth - this.conf.ofs.l - this.conf.ofs.r + "px";
        this.cont.style.top = n + "px";
        this.cont.style.height = (f ? (this.conf.flexible || this.base.clientHeight) - n - e : "0") + "px";
    };
    this.setSizes = function() {
        this._adjustCont();
        if (this.conf.multi_mode == true) {
            this._adjustOpened()
        } else {
            this.conf.cont_w = null;
            var r = this.conf.flexible || this._updateCellsHeight();
            for (var o in this.t) {
                if (this.t[o].cell.conf.visible == true) {
                    this.t[o].cell._setSize(this.ofs.s.left, 0, this.conf.cont_w || this._getAvailItemWidth(), this.t[o].cell.conf.opened ? r : this.t[o].cell._getHdrHeight());
                    if (this.t[o].cell.conf.opened != true) {
                        this.t[o].cell.conf.size.h = r;
                        this.t[o].cell._adjustCell()
                    }
                }
            }
            this.conf.size_changed = true
        }
        this.callEvent("_onSetSizes", [])
    };
    this.__openItem = this._openItem;
    this._openItem=function(){
    	this.__openItem.apply(this, [].slice.call(arguments));
    	this.setSizes();
    }
}
SepoaDhtmlXAccordion.prototype = Object.create(dhtmlXAccordion.prototype);
SepoaDhtmlXAccordion.prototype.constructor = SepoaDhtmlXAccordion;
SepoaDhtmlXAccordion.prototype.getCellIndex = function(){
	var returnData = {};
	this.forEachItem(function(obj){
		returnData[obj._idd] = this.getIndexById(obj._idd)+'';		
	});
	return returnData;
}
SepoaDhtmlXAccordion.prototype.getIdByIndex = function(_idx){
	var i = this.cont.childNodes[_idx];
	var returnId = '';
	this.forEachItem(function(obj){
		if(obj.cell == i){
			returnId = obj._idd;
		}
	});
	return returnId;
}
SepoaDhtmlXAccordion.prototype.getIndexById = function(_id){
	var c = this.cells(_id).cell;
	for(var l = 0;l<this.cont.childNodes.length;l++){
		if(this.cont.childNodes[l] == c){
			return l;
		}
	}
}

function SepoaDhtmlXAccordionCell(g, e) {
    var base = dhtmlXAccordionCell.call(this, g, e);
    this.cell.children[1].style.height=this.acc.conf.flexible + "px";
    this.acc.attachEvent("onBeforeDrag", function(l) {
        if (this.t[l].cell.dataObj != null && this.t[l].cell.dataType == "editor") {
            if (this.conf.editor_data == null) {
                this.conf.editor_data = {}
            }
            this.conf.editor_data[l] = this.t[l].cell.dataObj.getContent();
        }
        for (var h in this.t) {
            //this.t[h].cell._showCellCover();
            this.t[h].cell._hideCellCover();
        }
        return true
    });
    this.open = function(a) {
    	if (window.dhx4.s2b(a) == true) {
            this.acc.conf.on_active_click = true
        }
    	this.acc.conf.cellOpened = false;
        this.acc._openItem(this._idd)
    };
    this.close = function(a) {
        if (window.dhx4.s2b(a) == true) {
            this.acc.conf.on_active_click = true
        }
        this.acc._closeItem(this._idd)
    };
}
SepoaDhtmlXAccordionCell.prototype = Object.create(dhtmlXAccordionCell.prototype);
SepoaDhtmlXAccordionCell.prototype.constructor = SepoaDhtmlXAccordionCell;
SepoaDhtmlXAccordionCell.prototype._open = function(c) {
    var h = {};
    if (this.acc.conf.multi_mode) {
        var g = {};
        g[this._idd] = true;
        h = this.acc._getDynData(g);
        this.acc._openId = this._idd;
        this.acc._openCache = {};
        this.acc._openMode = "open"
    } else {
        h[this._idd] = this.acc.conf.flexible || this.conf.size.h
    }
    for (var e in h) {
        if (this.acc._openCache != null) {
            this.acc._openCache[e] = true
        }
        var l = this.acc.t[e].cell;
        l.cell.className = String(l.cell.className).replace(/\s{1,}dhx_cell_closed/gi, "");
        l.conf.opened = true;
        l._trInitEv("open", c);
        l.conf.size.h = h[e];
        l.cell.style.height = l.conf.size.h + "px";
        l = null
    }
    if (this.conf.tr.prop == false) {
        this.acc._openId = this.acc._openCache = this.acc._openMode = null;
        this._onActiveCall(true)
    }
};
SepoaDhtmlXAccordionCell.prototype._close = function(c) {
    var g = {};
    if (this.acc.conf.multi_mode) {
        var h = {};
        h[this._idd] = true;
        g = this.acc._getDynData(null, h);
        this.acc._openId = this._idd;
        this.acc._openCache = {};
        this.acc._openMode = "close"
    } else {
        g[this._idd] = this._getHdrHeight()
    }
    for (var e in g) {
        var l = this.acc.t[e].cell;
        if (this.acc._openCache != null) {
            this.acc._openCache[e] = true
        }
        if (e == this._idd) {
            l.cell.className += " dhx_cell_closed";
            l.conf.opened = false;
            l._trInitEv("close", c)
        } else {
            l.conf.size.h = g[e];
            l._adjustCell();
            l._trInitEv("open", c)
        }
        l.cell.style.height = g[e] + "px";
        l = null
    }
	this.acc.setSizes();
    if (this.conf.tr.prop == false) {
        this.acc._openId = this.acc._openCache = this.acc._openMode = null;
        this._onActiveCall(false)
    }
};

SepoaDhtmlXAccordionCell.prototype._trOnEnd = function(a) {
    if (a.stopPropagation) {
        a.stopPropagation()
    }
    if (a.propertyName == "height" && this._accObj != null) {
    	
    	if(this._accObj.conf.autoclose) {
    		if(!this._accObj.conf.cellOpened) {
    			this._accObj.conf.cellOpened = !a.srcElement.classList.contains('dhx_cell_closed');
    		}
    	}
    	
        this.style[this._trProp] = "";
        if (this._accObj.conf.multi_mode) {
            if (this._accObj._openCache != null) {
                this._accObj._openCache[this._accId] = false
            }
            this._accObj._adjustOpened()
        }
        if (this._accObj.conf.on_active_click == true) {
            if (this._accObj.conf.multi_mode) {
                this._accObj._callMainEvent("onActive", [this._accId, this._accObj.t[this._accId].cell.conf.opened]);
                this._accObj.conf.on_active_click = false
            } else {
                if (this._accObj.conf.on_active_id != null) {
                    this._accObj._callMainEvent("onActive", [this._accObj.conf.on_active_id, true])
                }
                this._accObj.conf.on_active_id = null;
                this._accObj.conf.on_active_click = false
            }
        }
        this._accObj = null
    }
};