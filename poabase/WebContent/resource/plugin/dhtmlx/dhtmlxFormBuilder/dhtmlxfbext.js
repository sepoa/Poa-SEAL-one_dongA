/*
Product Name: dhtmlxFormBuilder 
Version: 1.0 
Edition: Standard 
License: content of this file is covered by GPL. Usage outside GPL terms is prohibited. To obtain Commercial or Enterprise license contact sales@dhtmlx.com
Copyright UAB Dinamenta http://www.dhtmlx.com
*/
function dhtmlXAreaSelect(b, d) {
    var c = this;
    var a = {};
    if (typeof(b) == "object" && typeof(b.tagName) == "undefined") {
        a = b
    } else {
        a = {
            parent: b,
            skin: d,
            clear: true
        }
    }
    this.conf = {
        skin: (a.skin || window.dhx4.skin || (typeof(dhtmlx) != "undefined" ? dhtmlx.skin : null) || window.dhx4.skinDetect("dhxareaselect") || "dhx_skyblue"),
        parent: (typeof(a.parent) == "string" ? document.getElementById(a.parent) : a.parent),
        data: null,
        ofs: null,
        nodes: null
    };
    this.setNodes = function(f) {
        this.conf.nodes = {};
        var h = {};
        if (this.conf.data != null && this.conf.data.ofsX != null) {
            h = {
                x: this.conf.data.ofsX,
                y: this.conf.data.ofsY
            }
        }
        for (var e in f) {
            var g = {
                x1: window.dhx4.absLeft(f[e]) - (h.x || 0),
                y1: window.dhx4.absTop(f[e]) - (h.y || 0),
                ofs: (h.x != null)
            };
            g.x2 = g.x1 + f[e].offsetWidth;
            g.y2 = g.y1 + f[e].offsetHeight;
            this.conf.nodes[e] = g
        }
    };
    this._doOnMouseDown = function(g) {
        g = g || event;
        var f = (g.target || g.srcElement);
        if (c.callEvent("onBeforeSelectStart", [g]) == true) {
            c._selectPrepare(g)
        }
    };
    this._selectPrepare = function(f) {
        f.cancelBubble = true;
        c.conf.data = {
            x: f.clientX,
            y: f.clientY,
            selected: {}
        };
        c.conf.data.ofsX = window.dhx4.absLeft(c.conf.parent) - c.conf.parent.scrollLeft;
        c.conf.data.ofsY = window.dhx4.absTop(c.conf.parent) - c.conf.parent.scrollTop;
        c.conf.data.x -= c.conf.data.ofsX;
        c.conf.data.y -= c.conf.data.ofsY;
        c.callEvent("onSelectStart", [f]);
        c._selectAttachEvents()
    };
    this._selectUpdate = function(h) {
        h = h || event;
        if (c.conf.data.rect == null) {
            if (Math.abs(h.clientX - c.conf.data.x) < 5 && Math.abs(h.clientY - c.conf.data.y) < 5) {
                return
            }
            document.body.className += " dhxareaselect_actv";
            c.conf.data.rect = document.createElement("DIV");
            c.conf.data.rect.className = "dhxareaselect_rect_" + c.conf.skin;
            c.conf.parent.appendChild(c.conf.data.rect);
            if (c.conf.ofs == null) {
                c.conf.data.rect.style.cssText = "left: -100px; top: -100px; width: 50px; height: 50px;";
                c.conf.ofs = {
                    w: c.conf.data.rect.offsetWidth - 50,
                    h: c.conf.data.rect.offsetHeight - 50
                }
            }
            c.conf.data.performed = true
        }
        var g = {
            x1: Math.min(h.clientX - c.conf.data.ofsX, c.conf.data.x),
            y1: Math.min(h.clientY - c.conf.data.ofsY, c.conf.data.y),
            w: Math.abs(h.clientX - c.conf.data.ofsX - c.conf.data.x),
            h: Math.abs(h.clientY - c.conf.data.ofsY - c.conf.data.y)
        };
        g.x2 = g.x1 + g.w;
        g.y2 = g.y1 + g.h;
        c.conf.data.rect.style.cssText = window.dhx4.template("left: #x1#px; top: #y1#px; width: #w#px; height: #h#px;", g);
        for (var f in c.conf.nodes) {
            var i = c.conf.nodes[f];
            if (i.ofs == false) {
                i.x1 -= c.conf.data.ofsX;
                i.y1 -= c.conf.data.ofsY;
                i.x2 -= c.conf.data.ofsX;
                i.y2 -= c.conf.data.ofsY;
                i.ofs = true
            }
            if (c._selectCheckMatch(g, i) == true) {
                if (c.conf.data.selected[f] != true) {
                    c.callEvent("onNodeSelect", [f, true]);
                    c.conf.data.selected[f] = true
                }
            } else {
                if (c.conf.data.selected[f] == true) {
                    c.callEvent("onNodeSelect", [f, false]);
                    c.conf.data.selected[f] = false;
                    delete c.conf.data.selected[f]
                }
            }
        }
    };
    this._selectStop = function(f) {
        f = f || event;
        c._selectDetachEvents();
        if (c.conf.data.rect != null) {
            c.conf.data.rect.parentNode.removeChild(c.conf.data.rect);
            c.conf.data.rect = null
        }
        if (c.conf.data.performed == true) {
            c.callEvent("onSelectStop", [window.dhx4._copyObj(c.conf.data.selected)])
        } else {
            c.callEvent("onSelectCancel", [])
        }
        c.conf.data.selected = null;
        c.conf.data = null;
        if (document.body.className.match(/dhxareaselect_actv/gi) != null) {
            document.body.className = String(document.body.className).replace(/\s*dhxareaselect_actv/gi, "")
        }
    };
    this._selectAttachEvents = function() {
        if (typeof(window.addEventListener) == "function") {
            window.addEventListener("mousemove", this._selectUpdate, false);
            window.addEventListener("mouseup", this._selectStop, false);
            if (window.dhx4.isIE == true) {
                window.addEventListener("selectstart", this._preventSelect, false)
            }
        } else {
            document.body.attachEvent("onmousemove", this._selectUpdate);
            document.body.attachEvent("onmouseup", this._selectStop);
            if (window.dhx4.isIE == true) {
                document.body.attachEvent("onselectstart", this._preventSelect)
            }
        }
    };
    this._selectDetachEvents = function() {
        if (typeof(window.addEventListener) == "function") {
            window.removeEventListener("mousemove", this._selectUpdate, false);
            window.removeEventListener("mouseup", this._selectStop, false);
            if (window.dhx4.isIE == true) {
                window.removeEventListener("selectstart", this._preventSelect, false)
            }
        } else {
            document.body.detachEvent("onmousemove", this._selectUpdate);
            document.body.detachEvent("onmouseup", this._selectStop);
            if (window.dhx4.isIE == true) {
                document.body.detachEvent("onselectstart", this._preventSelect)
            }
        }
    };
    this._selectCheckMatch = function(g, e) {
        var f = false;
        if (g.x1 <= e.x1) {
            if (g.x2 >= e.x1) {
                f = "half"
            }
        } else {
            if (g.x1 <= e.x2) {
                f = "half"
            }
        }
        if (f == "half" && g.y1 <= e.y2 && g.y2 >= e.y1) {
            f = true
        }
        return f
    };
    this._preventSelect = function(f) {
        f = f || event;
        f.returnValue = false;
        if (f.preventDefault) {
            f.preventDefault()
        }
        return false
    };
    if (typeof(window.addEventListener) == "function") {
        this.conf.parent.addEventListener("mousedown", this._doOnMouseDown, false)
    } else {
        this.conf.parent.attachEvent("onmousedown", this._doOnMouseDown)
    }
    window.dhx4._eventable(this);
    this.unload = function() {
        if (typeof(window.addEventListener) == "function") {
            this.conf.parent.removeEventListener("mousedown", this._doOnMouseDown, false)
        } else {
            this.conf.parent.detachEvent("onmousedown", this._doOnMouseDown)
        }
        window.dhx4._eventable(this, "clear");
        c = null
    };
    if (a.clear == true) {
        a.parent = null
    }
    return this
}

function dhtmlXMockup(c, e) {
    var d = this;
    var b = {};
    if (typeof(c) == "object" && typeof(c.tagName) == "undefined") {
        b = c
    } else {
        b = {
            parent: c,
            skin: e,
            clear: true
        }
    }
    this.conf = {
        skin: (b.skin || window.dhx4.skin || (typeof(dhtmlx) != "undefined" ? dhtmlx.skin : null) || window.dhx4.skinDetect("dhxmockup") || "dhx_skyblue"),
        parent: (typeof(b.parent) == "string" ? document.getElementById(b.parent) : b.parent),
        selected: {},
        is_mac: (navigator.platform.match(/^mac/i) != null && typeof(window.addEventListener) == "function"),
        mac_cmd_key: false
    };
    this.conf.parent.className += " dhxmockup_" + this.conf.skin;
    this.items = {};
    this._addBase = function(i) {
        var h = window.dhx4.newId().toString();
        var f = document.createElement("TABLE");
        f.className = "dhxmockup_el_base";
        f._idd = h;
        f._type = "base";
        f.cellSpacing = 0;
        f.cellPadding = 0;
        f.border = 0;
        if (i == null) {
            this.conf.parent.appendChild(f)
        } else {
            this.items[i].node.appendChild(f)
        }
        f.appendChild(document.createElement("TBODY"));
        f.firstChild.appendChild(document.createElement("TR"));
        this.items[h] = {
            parentId: i,
            type: "base",
            node: f,
            settings: {}
        };
        if (i) {
            this.items[i].baseId = h
        }
        f = null;
        var g = this.addColumn(h, {
            type: "column"
        });
        return g
    };
    this.addColumn = function(k, i, g) {
        var g = g || {};
        var j = window.dhx4.newId().toString();
        var h = document.createElement("TD");
        h._idd = j;
        h._type = "column";
        h.className = "dhxmockup_el_column";
        h.innerHTML = "<div class='dhxmockup_btn_del dhxmockup_btn_del_column' title='Delete Column'>x</div><div><div class='dhxmockup_btn_add'>+</div><div class='dhxmockup_btn_cldel'>x</div></div>";
        var f = this.items[k].type === "base" ? k : this.items[k].baseId;
        this.items[f].node.firstChild.firstChild.appendChild(h);
        this.items[j] = {
            parentId: f,
            type: "column",
            node: h,
            formData: i
        };
        h = null;
        if (g.callEvent) {
            this.callEvent("onAdd", [f, j, this.items[j]])
        }
        return j
    };
    this.addBlock = function(k, i, f) {
        var f = f || {};
        var j = window.dhx4.newId().toString();
        var g = document.createElement("DIV");
        g._idd = j;
        g._type = "block";
        g.className = "dhxmockup_el_block";
        g.innerHTML = "<div class='dhxmockup_btn_del dhxmockup_btn_del_block' title='Delete Block'>x</div>";
        this.items[k].node.insertBefore(g, this.items[k].node.lastChild);
        if (i.width == null) {
            i.width = "auto"
        }
        if (i.blockOffset == null) {
            i.blockOffset = ""
        }
        this.items[j] = {
            parentId: k,
            type: "block",
            node: g,
            formData: i
        };
        g = null;
        var h = this._addBase(j);
        if (f.callEvent) {
            this.callEvent("onAdd", [k, j, this.items[j]])
        }
        return h
    };
    this.addItem = function(h, i, f) {
        var f = f || {};
        var j = window.dhx4.newId().toString();
        var g = document.createElement("DIV");
        g._idd = j;
        g._type = "item";
        g.className = "dhxmockup_el_item";
        g.innerHTML = (this._icons ? "<span class='icon'></span>" : "<span></span>") + "<span></span><div class='dhxmockup_btn_del dhxmockup_btn_del_item' title='Delete Item'>x</div>";
        this.items[h].node.insertBefore(g, this.items[h].node.lastChild);
        this.items[j] = {
            parentId: h,
            type: "item",
            node: g,
            formData: i
        };
        g = null;
        this._makeItemName(j, false);
        if (this._icons) {
            this._makeItemIcon(j)
        }
        if (f.callEvent) {
            this.callEvent("onAdd", [h, j, this.items[j]])
        }
        return j
    };
    this.copyItem = function(i, h, f) {
        var h = h || this.items[i].parentId,
            g = this.getItemData([i])[i];
        myMockup.addItem(h, g, f)
    };
    this.copyBlock = this.copyColumn = function(l, k, f) {
        var f = f || {},
            k = k || this.items[l].parentId,
            j = this.exportToForm(l),
            h = this.getItemData([l])[l],
            i = h.type == "column" ? this.addColumn : this.addBlock;
        if (i) {
            var g = i.call(this, k, h, f);
            this._initObj(j, g);
            if (f.callEvent) {
                this.callEvent("onAdd", [k, l, this.items[l]])
            }
        }
    };
    this.getBaseId = function(h) {
        var g = this.items[h];
        if (g && "base" == g.type) {
            return h
        }
        if (g && "block" == g.type) {
            return g.baseId
        }
        for (var f in this.items) {
            if (null == this.items[f].parentId) {
                return f
            }
        }
    };
    this.moveNode = function(f, j, o) {
        o = o || {};
        var n = this.items[f],
            l = this.items[j] || this.items[this.getBaseId()],
            m, k, i;
        if ("column" == n.type) {
            m = this.getBaseId(j);
            k = null;
            i = l.node.lastChild.firstChild.firstChild
        } else {
            m = j;
            k = l.node.lastChild;
            i = l.node
        }
        var g = this.items[f],
            l = this.items[m];
        g.parentId = m;
        if (null != o.childIndex) {
            var h = o.childIndex + ("column" == n.type ? 0 : 1);
            k = this._nthChild(i, h, [g.node])
        }
        i.insertBefore(g.node, k);
        if (o.callEvent) {
            this.callEvent("onNodeMove", [j, f])
        }
        k = i = null
    };
    this.deleteItem = function(j, f, i) {
        for (var g in this.items) {
            if (this.items[g].parentId == j) {
                this.deleteItem(g, true, i)
            }
        }
        if (f != true && this.items[j].type == "column" && this.items[j].node.parentNode.childNodes.length == 1) {
            console.log("last column");
            return
        }
        var h = this.items[j].type;
        this.items[j].node.parentNode.removeChild(this.items[j].node);
        this.items[j].node = null;
        this.items[j] = null;
        delete this.items[j];
        if (typeof(i) == "undefined" || i == true) {
            this.callEvent("onDelete", [j, h])
        } else {
            this._fixSelectionOnDelete(j)
        }
    };
    //Mockup item Name
    this._makeItemName = function(g, m) {
        var f = this.items[g];
        var i = "[" + String(f.formData.type).charAt(0) + "] " + (f.formData.label || f.formData.type);
        if(!m){
	        f.node.firstChild.nextSibling.innerHTML = i;
	        f = null
        }else{
        	var t = $(f.node.firstChild.nextSibling);
        	var b = t.find(".dhxmockup_el_base_label");
        	if(b.length>0 || t[0].tagName == 'SPAN')  (b[0] || t[0]).innerHTML = i;
        	else t.prepend($('<tr><td><span class="dhxmockup_el_base_label">'+i+'</span></td></tr>')) 
        }
    };
    this._makeItemIcon = function(i) {
        var g = this.items[i];
        var f = g.node.getElementsByClassName("icon");
        if (f.length > 0) {
            var h = String(f[0].className);
            h = h.replace(/icon-/ig, "");
            f[0].className = h + " icon-" + (g.formData && g.formData.type)
        }
        g = f = null
    };
    this.clearAll = function() {
        var h = null;
        for (var g = 0; g < this.conf.parent.childNodes.length; g++) {
            var f = this.conf.parent.childNodes[g];
            if (h == null && typeof(f.className) != "indefined" && String(f.className).match(/dhxmockup_el_base/) != null) {
                h = f._idd
            }
            f = null
        }
        if (h != null && this.items[h] != null) {
            this.deleteItem(h, true)
        }
    };
    this._initObj = function(l, k) {
        if (k == null) {
            this.clearAll();
            k = this._addBase()
        }
        for (var j = 0; j < l.length; j++) {
            var i = l[j];
            var m = window.dhx4._copyObj(i);
            if (m.list != null) {
                delete m.list
            }
            if (i.type == "block" || i.type == "fieldset") {
                var g = this.addBlock(k, m);
                if (i.list instanceof Array) {
                    this._initObj(i.list, g)
                }
            } else {
                if (i.type == "column" || i.type == "newcolumn") {
                    var h = this.items[k].parentId;
                    m.type = "column";
                    k = this.addColumn(h, m)
                } else {
                    if (i.type == "settings") {
                        var h = this.items[k].parentId;
                        if (i != null) {
                            for (var f in i) {
                                this.items[h].settings[f] = i[f]
                            }
                        }
                    } else {
                        this.addItem(k, m)
                    }
                }
            }
        }
    };
    this._xmlToObj = function() {};
    dhx4._enableDataLoading(this, "_initObj", "_xmlToObj", "data", {
        struct: true
    });
    this._doOnClick = function(g) {
        g = g || event;
        var f = g.target || g.srcElement;
        if (f.className.match(/dhxmockup_btn_add/) != null) {
            g.cancelBubble = true;
            d._showContextMenu(f);
            f = null;
            return
        }
        if (f.className.match(/dhxmockup_btn_cldel/) != null) {
            a(g, f, f.parentNode.parentNode._idd);
            return
        }
        if (f.className.match(/dhxmockup_btn_del/) != null) {
            a(g, f, f.parentNode._idd);
            return
        }
        d.callEvent("_onClick", [g])
    };

    function a(g, f, h) {
        g.cancelBubble = true;
        f = null;
        d.deleteItem(h)
    }
    if (typeof(window.addEventListener) == "function") {
        this.conf.parent.addEventListener("click", this._doOnClick, false)
    } else {
        this.conf.parent.attachEvent("onclick", this._doOnClick)
    }
    window.dhx4._eventable(this);
    this.unload = function() {
        this._unloadContextMenu();
        this._unloadSelection();
        window.dhx4._enableDataLoading(this, null, null, null, "clear");
        window.dhx4._eventable(this, "clear");
        d = null
    };
    this._initContextMenu();
    this._initSelection();
    this._initDragAndDrop();
    if (b.clear == true) {
        b.parent = null
    }
    return this
}
dhtmlXMockup.prototype.domWalker = {
    filter: function(e, a) {
        a = Array.prototype.slice.call(a, 0);
        var f = a.map(function(h) {
            for (var g = 0; h != e; ++g && (h = h.parentNode)) {}
            return g
        });
        var d = Math.min.apply(null, f),
            b = [];
        for (var c = 0; c < a.length; ++c) {
            if (f[c] == d) {
                b.push(a[c])
            }
        }
        return b
    },
    findColumns: function(a) {
        return dhtmlXMockup.prototype.domWalker.filter(a, a.querySelectorAll(".dhxmockup_el_column"))
    },
    findBlocksAndItems: function(a) {
        return dhtmlXMockup.prototype.domWalker.filter(a, a.querySelectorAll(".dhxmockup_el_block, .dhxmockup_el_item"))
    }
};
dhtmlXMockup.prototype.exportToTree = function(g) {
    var e = function(h) {
        var i = {
            id: h.id
        };
        var j = h.formData || {};
        if (h.type == "item") {
            i.userdata = {
                type: j.type,
                label: j.label
            };
            if ("button" == j.type) {
                i.userdata.label = j.value
            }
            if ("uploader" == j.type) {
                i.userdata.label = ""
            }
            i.text = "[" + j.type + "] " + (i.userdata.label || "");
            if (this._icons) {
                i.icons = {
                    file: this._icons[j.type]
                }
            }
        } else {
            i.text = j.type || h.type;
            i.userdata = {
                type: i.text
            }
        }
        i.text = "[" + h.id + "] " + i.text;
        return i
    }.bind(this);
    var d = null,
        c = [];
    if (!g) {
        c = this.domWalker.findColumns(this.conf.parent)
    } else {
        d = this.items[g];
        if (d.type == "block") {
            c = this.domWalker.findColumns(d.node)
        } else {
            if (d.type == "column") {
                c = this.domWalker.findBlocksAndItems(d.node)
            }
        }
    }
    var a = c.map(function(l) {
        var k = l._idd;
        var i = this.items[k];
        i.id = k;
        var h = e(i);
        var j = this.exportToTree(k);
        if (j.length > 0) {
            h.items = j;
            h.open = true
        }
        return h
    }.bind(this));
    if (!d || d.type == "block") {
        var f;
        if (d) {
            f = d.baseId
        } else {
            for (f in this.items) {
                if (null == this.items[f].parentId) {
                    break
                }
            }
        }
        var b = {
            id: f,
            text: "[" + f + "] @ settings",
            userdata: [{
                name: "type",
                value: "settings"
            }]
        };
        if (this._icons) {
            b.icons = {
                file: this._icons.settings
            }
        }
        a.unshift(b)
    }
    return a
};
dhtmlXMockup.prototype.exportToForm = function(h, n) {
    var j = function(o) {
        var p = {};
        if (!n.hideIds) {
            p._idd = o.id
        }
        var q = o.formData || {};
        for (var i in q) {
        	if(i == 'combo'){
        		if(!p['userdata']) p['userdata'] = {};
        		p.userdata[i] = q[i];
        	}else if((i == 'isNotEmpty' && q[i] == true) || i == 'validate'){
        		var m = (i=='isNotEmpty')?i:q[i];
        		if(!p['validate']){ 
        			p['validate'] = m;
        		}else{
        			var t = p['validate'].split(",");
            		t.push(m);
            		p['validate'] = t.join(",");
        		}
        	}else{
        		p[i] = q[i]
        	}
        }
        if ("block" == o.type) {
            p.type = q.type
        } else {
            if ("column" == o.type) {
                p.type = "newcolumn"
            }
        }
        if (n.fakeNames) {
            if ("radio" == p.type) {
                p.name = p.name || p._idd;
                p.userdata = {
                    _idd: p._idd
                }
            } else {
                p.name = p._idd
            }
        }
        return p
    };
    n = n || {};
    var l = null,
        d = [];
    if (!h) {
        d = this.domWalker.findColumns(this.conf.parent)
    } else {
        l = this.items[h];
        if (l.type == "block") {
            d = this.domWalker.findColumns(l.node)
        } else {
            if (l.type == "column") {
                d = this.domWalker.findBlocksAndItems(l.node)
            }
        }
    }
    var a = [];
    for (var g = 0; g < d.length; ++g) {
        var c = d[g];
        var b = c._idd;
        var m = this.items[b];
        m.id = b;
        var e = j(m);
        if (0 != a.length || "column" != m.type) {
            a.push(e)
        }
        var k = this.exportToForm(b, n);
        if (k.length > 0) {
            if ("column" == m.type) {
                a = a.concat(k)
            } else {
                e.list = k
            }
        }
    }
    if (!l || l.type == "block") {
        var b;
        if (l) {
            b = l.baseId
        } else {
            for (b in this.items) {
                if (null == this.items[b].parentId) {
                    break
                }
            }
        }
        var f = this._exportSettings(this.items[b].settings);
        a = f.concat(a || [])
    }
    return !l ? {
        data: a,
        selected: this.conf.selected
    } : window.dhx4._copyObj(a)
};
dhtmlXMockup.prototype.exportToFormSepoaMode = function(h, n, slang) {
	slang = (!slang)?[]:slang;
    var j = function(o) {
        var p = {};
        if (!n.hideIds) {
            //p._idd = o.id
        }
        var q = o.formData || {};
        for (var i in q) {
        	if(i == 'combo'){
        		if(!p['userdata']) p['userdata'] = {};
        		p.userdata[i] = q[i];
        	}else if((i == 'isNotEmpty' && q[i] == true) || i == 'validate'){
        		var m = (i=='isNotEmpty')?i:q[i];
        		if(!p['validate']){ 
        			p['validate'] = m;
        		}else{
        			var t = p['validate'].split(",");
            		t.push(m);
            		p['validate'] = t.join(",");
        		}
        	}else if(i == 'readonly'){
        		p.disabled=q[i];
        		p[i] = q[i];
        	}else if(i != '_idd'){
        		p[i] = q[i]
        	}
        }
        if ("block" == o.type) {
            p.type = q.type
        } else {
            if ("column" == o.type) {
                p.type = "newcolumn"
            }
        }
        if (n.fakeNames) {
            if ("radio" == p.type) {
                p.name = p.name || p._idd;
                p.userdata = {
                    _idd: p._idd
                }
            } else {
                p.name = p._idd
            }
        }
        if(!!p.name && p.name != ""){
        	if(!!p.label && p.label != "") {
        		var u = {};
            	if(p.type == 'radio'){
            		u['CODE'] = p.name+'{'+p.value+'}';
    	        	u['CONTENTS'] = p.label;
            	}else{
    	        	u['CODE'] = p.name
    	        	u['CONTENTS'] = p.label;
            	}
            	slang.push(u);
            	delete p.label;
        	} else {
        		if(o.type == "item") {
        			var u = {
        				CODE: p.name,
        				CONTENTS: "#NO_LABEL#"
        			};
        			slang.push(u);
        		}
        	}
        }
        return p
    };
    n = n || {};
    var l = null,
        d = [];
    if (!h) {
        d = this.domWalker.findColumns(this.conf.parent)
    } else {
        l = this.items[h];
        if (l.type == "block") {
            d = this.domWalker.findColumns(l.node)
        } else {
            if (l.type == "column") {
                d = this.domWalker.findBlocksAndItems(l.node)
            }
        }
    }
    var a = [];
    for (var g = 0; g < d.length; ++g) {
        var c = d[g];
        var b = c._idd;
        var m = this.items[b];
        m.id = b;
        var e = j(m);
        if (0 != a.length || "column" != m.type) {
            a.push(e)
        }
        var k = this.exportToFormSepoaMode(b, n, slang);
        if (k.length > 0) {
            if ("column" == m.type) {
                a = a.concat(k)
            } else {
                e.list = k
            }
        }
    }
    if (!l || l.type == "block") {
        var b;
        if (l) {
            b = l.baseId
        } else {
            for (b in this.items) {
                if (null == this.items[b].parentId) {
                    break
                }
            }
        }
        var f = this._exportSettings(this.items[b].settings);
        a = f.concat(a || [])
    }
    return !l ? {
        data: a,
        selected: this.conf.selected,
        slang: slang
    } : window.dhx4._copyObj(a)
};
dhtmlXMockup.prototype._exportSettings = function(e) {
    var d = {
        type: "settings"
    };
    var c = 0;
    for (var b in e) {
        if (b != "type") {
            d[b] = e[b];
            c++
        }
    }
    return (c > 0 ? [d] : [])
};
dhtmlXMockup.prototype.setItemData = function(f, a, d) {
    if (this.items[f] != null) {
        var c = (this.items[f].type == "base" ? "settings" : "formData");
        if (c == "settings") {
            var e = (+f + 1).toString();
            for (var b in this.items) {
                if (this.items.hasOwnProperty(b)) {
                    if (this.items[b].parentId == e && this.items[b]["formData"].type == "block") {
                        this.items[b]["formData"].blockOffset = d
                    }
                }
            }
        }
        if (d == "" || d == null) {
            if (typeof(this.items[f][c][a]) != "undefined") {
                delete this.items[f][c][a]
            }else if(a == "noteText"){
            	delete this.items[f][c]['note'];
            }else if(a == "noteWidth"){
            	delete this.items[f][c]['note'].width;
            }else if(a == "isNotEmpty" || a == "validate"){
            	var vItem = (a == "isNotEmpty")?"isNotEmpty":d;
            	var iNE = false;
            	var ar = [];
            	var newData = [];
            	if(!!this.items[f][c]["validate"]){
            		ar = this.items[f][c]["validate"].split(',');
            		iNE = (ar.indexOf("isNotEmpty") >= 0)?true:false;
            		if(iNE){
            			ar.splice([ar.indexOf('isNotEmpty')],1);
            		}
            	}
            	if(vItem == "isNotEmpty" && d){
            		newData.push("isNotEmpty");
            		newData.concat(ar);
            	}else if(vItem != "isNotEmpty"){
            		if(iNE) newData.push("isNotEmpty");
            		newData.push(vItem);
            	}else{
            		newData.concat(ar);
            	}
            	this.items[f][c]["validate"] = newData.join(',');
            } 
        } else {
            if (a == "minutesInterval") {
                this.items[f][c][a] = parseInt(d)
            } else {
                if (d == "uploader") {
                    this.items[f][c][a] = "upload"
                } else {
                    if (a == "noteText") {
                        if (typeof this.items[f][c]["note"] == "undefined") {
                            this.items[f][c]["note"] = {
                                text: d
                            }
                        } else {
                            this.items[f][c]["note"].text = d
                        }
                    } else {
                        if (a == "noteWidth") {
                            if (typeof this.items[f][c]["note"] == "undefined") {
                                this.items[f][c]["note"] = {
                                    width: d,
                                    text: ""
                                }
                            } else {
                                this.items[f][c]["note"].width = d
                            }
                        } else {
                            this.items[f][c][a] = d
                        }
                    }
                }
            }
        }
        if (d == "button") {
            this.items[f][c].value = this.items[f][c].label
        }
        if (d !== "fieldset" && d !== "block") {
            if (a == "type" || a == "label") {
                this._makeItemName(f, true)
            }
            if (a == "type" && this._icons) {
                this._makeItemIcon(f)
            }
        }
    }
};
dhtmlXMockup.prototype.getItemData = function(c) {
    var e = {};
    for (var d = 0; d < c.length; d++) {
        var f = c[d];
        if (this.items[f] != null) {
            if (this.items[f].type == "item") {
                e[f] = window.dhx4._copyObj(this.items[f].formData);
                if (this._icons) {
                    e[f].icon = this._icons[e[f].type]
                }
            } else {
                if (this.items[f].type == "column") {
                    e[f] = {
                        type: "column"
                    }
                } else {
                    if (this.items[f].type == "block") {
                        e[f] = {
                            type: this.items[f].formData.type
                        }
                    } else {
                        if (this.items[f].type == "base") {
                            e[f] = window.dhx4._copyObj(this.items[f].settings);
                            e[f].type = "settings";
                            if (this._icons) {
                                e[f].icon = this._icons.settings
                            }
                        }
                    }
                }
            }
            if (e[f] != null && this.items[f].formData != null) {
                for (var b in this.items[f].formData) {
                    if (typeof(e[f][b]) == "undefined") {
                        e[f][b] = this.items[f].formData[b]
                    }
                }
            }
        }
    }
    return e
};
dhtmlXMockup.prototype._addEventListener = function(b, a) {
    if (typeof(window.addEventListener) == "function") {
        window.addEventListener(b, a, false)
    } else {
        document.body.attachEvent("on" + b, a)
    }
};
dhtmlXMockup.prototype._removeEventListener = function(b, a) {
    if (typeof(window.addEventListener) == "function") {
        window.removeEventListener(b, a, false)
    } else {
        document.body.attachEvent("on" + b, a)
    }
};
dhtmlXMockup.prototype._nthChild = function(b, d, a) {
    a = a || [];
    var c = b.firstChild;
    while (c && (a.indexOf(c) !== -1 || d--)) {
        c = c.nextSibling
    }
    return c
};
dhtmlXMockup.prototype._children = function(e) {
    var b = [],
        d = this.items[e];
    if ("column" == d.type) {
        var a = d.node.childNodes;
        for (var c = 0; c < a.length; ++c) {
            if (String(a[c].className).match(/dhxmockup_el_(item|block)/)) {
                b.push(a[c])
            }
        }
    }
    return b
};
dhtmlXMockup.prototype._closest = function(b, a) {
    while (b && !a(b)) {
        b = b.parentNode
    }
    return b
};
if (typeof(window.dhtmlXCellObject) == "function") {
    dhtmlXCellObject.prototype.attachMockup = function() {
        var a = document.createElement("DIV");
        a.style.position = "relative";
        a.style.overflow = "auto";
        a.style.width = "100%";
        a.style.height = "100%";
        this._attachObject(a);
        this.dataType = "mockup";
        this.dataObj = new dhtmlXMockup(a, this.conf.skin);
        a = null;
        this.callEvent("_onContentAttach", []);
        return this.dataObj
    }
}
dhtmlXMockup.prototype.dndMap = {
    item: {
        move: function(c) {
            this.mockup._hideCapture();
            if (!this.el) {
                return
            }
            var b = this.getCatcher(c);
            if (!b) {
                return
            }
            var a = this.getChildIndex(c, b._idd, [this.target]);
            this.mockup.moveNode(this.target._idd, b._idd, {
                childIndex: a,
                callEvent: true
            });
            b = null
        },
        showCapture: function(d) {
            var c = this.getCatcher(d);
            if (!c) {
                this.mockup._hideCapture();
                return
            }
            var b = this.getChildIndex(d, c._idd, [this.target]);
            var a = this.mockup._children.call(this.mockup, c._idd).indexOf(this.target);
            if (a != b) {
                if (a !== -1 && b > a) {
                    ++b
                }
                this.mockup._showCapture(c._idd, b)
            } else {
                this.mockup._hideCapture()
            }
            c = null
        },
        getCatcher: function(c) {
            this.el.style.visibility = "hidden";
            var b = document.elementFromPoint(c.clientX, c.clientY);
            this.el.style.visibility = "visible";
            var a = this.mockup._closest(b, function(d) {
                return String(d.className).match(/dhxmockup_el_column/i)
            });
            b = null;
            return a
        },
        getChildIndex: function(g, j, f) {
            f = f || [];
            var a = this.mockup._children.call(this.mockup, j),
                b = 0,
                h = window.pageYOffset;
            for (var c = 0; c < a.length; ++c) {
                if (f.indexOf(a[c]) !== -1) {
                    continue
                }
                var d = a[c].getBoundingClientRect();
                if (h + d.top / 2 + d.bottom / 2 < g.pageY) {
                    ++b
                } else {
                    break
                }
            }
            a = null;
            return b
        }
    }
};
dhtmlXMockup.prototype._unloadDragAndDrop = function() {
    if (this.dnd.scrollInterval) {
        window.clearInterval(this.dnd.scrollInterval)
    }
    document.body.className = String(document.body.className).replace(/mockup_dnd/, "");
    this._onDragAndDropUnload()
};
dhtmlXMockup.prototype._initDragAndDrop = function() {
    if (this.dnd) {
        return
    }
    var a = this;
    this.conf.dnd = this.conf.dnd || {};
    this.conf.dnd.scrollSpeed = 5;
    this.conf.dnd.scrollDelay = 40;
    this.conf.dnd.scrollDelta = 40;
    this.conf.dnd.scrollMaxSpeed = 25;
    this.conf.parent.ondragstart = this.conf.parent.ondrop = function() {
        return false
    };
    this._onDragAndDropUnload = function() {
        a._removeEventListener("mousedown", a._onDragStart);
        a._removeEventListener("mousemove", a._onDrag);
        a._removeEventListener("mousemove", a._onDragScroll);
        a._removeEventListener("mouseup", a._onDragStop);
        for (var b in a) {
            if (a.hasOwnProperty(b) && String(b).match(/_onDrag/)) {
                delete a[b]
            }
        }
        delete a.dnd;
        a.conf.parent.ondragstart = a.conf.parent.ondrop = null;
        a = null
    };
    this._onDragStart = function(f) {
        f = f || event;
        if (f.which != 1) {
            return
        }
        var d = a._closest(f.target, function(e) {
            return String(e.className).match(/dhxmockup_el_item/i)
        });
        if (!d) {
            return
        }
        var c = a.items[d._idd].type;
        var b = a.dnd = Object.create(a.dndMap[c]);
        b.cache = {
            capture: null
        };
        b.target = d;
        b.mockup = a;
        b.downX = f.pageX;
        b.downY = f.pageY;
        b.scrollInterval = null;
        b.scrollSpeedY = b.scrollSpeedX = 0;
        a._addEventListener("mousemove", a._onDrag);
        a._addEventListener("mousemove", a._onDragScroll);
        a._addEventListener("mouseup", a._onDragStop);
        f.preventDefault && f.preventDefault();
        b = d = null
    };
    this._onDrag = function(d) {
        d = d || event;
        var b = a.dnd;
        if (!b.target) {
            return
        }
        if (!b.el) {
            if (Math.abs(b.downX - d.pageX) < 5 && Math.abs(b.downY - d.pageY) < 5) {
                return
            }
            b.el = document.createElement("div");
            b.el.style.width = "188px";
            b.el.style.height = "32px";
            b.el.className = "dhxmockup_dnd_item_clone";
            b.el.innerHTML = b.target.innerHTML;
            b.el.style.position = "absolute";
            b.el.style.zIndex = 10000;
            document.body.appendChild(b.el);
            document.body.className += " mockup_dnd";
            var c = b.target.getBoundingClientRect();
            b.offsetX = (b.downX - c.left - window.pageXOffset) / (c.right - c.left) * 188;
            b.offsetY = (b.downY - c.top - window.pageYOffset) / (c.bottom - c.top) * 32;
            b.cache.targetOpacity = b.target.style.opacity;
            b.target.style.opacity = 0.2
        }
        b.el.style.left = d.pageX - b.offsetX + "px";
        b.el.style.top = d.pageY - b.offsetY + "px";
        b.showCapture(d);
        d.preventDefault && d.preventDefault();
        b = null
    };
    this._onDragStop = function(d) {
        d = d || event;
        var c = a.dnd;
        if (!c.target) {
            return
        }
        if (c.el) {
            c.move(d);
            c.el.parentNode.removeChild(c.el);
            c.el = null
        }
        c.target.style.opacity = c.cache.targetOpacity;
        document.body.className = String(document.body.className).replace(/mockup_dnd/, "");
        window.clearInterval(c.scrollInterval);
        for (var b in a.dnd) {
            a.dnd[b] = null
        }
        a.dnd = {};
        a._removeEventListener("mousemove", a._onDrag);
        a._removeEventListener("mousemove", a._onDragScroll);
        a._removeEventListener("mouseup", a._onDragStop);
        c = null
    };
    this._onDragScroll = function(f) {
        f = f || event;
        var g = a.conf.dnd,
            i = a.conf.parent.getBoundingClientRect(),
            h = i.top + g.scrollDelta,
            b = h + i.bottom - i.top - 2 * g.scrollDelta,
            c = i.left + g.scrollDelta,
            l = c + i.right - i.left - 2 * g.scrollDelta,
            k, m, j, d;
        k = f.clientY < h ? -1 : (f.clientY > b ? 1 : 0);
        j = Math.abs(f.pageY - (k == 1 ? b : h));
        d = k == 1 ? (window.innerHeight - b) : h;
        a.dnd.scrollSpeedY = k * (g.scrollSpeed + (g.scrollMaxSpeed - g.scrollSpeed) * j / d);
        m = f.clientX < c ? -1 : (f.clientX > l ? 1 : 0);
        j = Math.abs(f.pageX - (m == 1 ? l : c));
        d = m == 1 ? (window.innerWidth - l) : c;
        a.dnd.scrollSpeedX = m * (g.scrollSpeed + (g.scrollMaxSpeed - g.scrollSpeed) * j / d);
        if (k == 0 && m == 0) {
            if (a.dnd.scrollInterval) {
                window.clearInterval(a.dnd.scrollInterval)
            }
            a.dnd.scrollInterval = null
        } else {
            if (!a.dnd.scrollInterval) {
                a.dnd.scrollInterval = window.setInterval(function() {
                    a.conf.parent.scrollTop += a.dnd.scrollSpeedY;
                    a.conf.parent.scrollLeft += a.dnd.scrollSpeedX
                }, g.scrollDelay)
            }
        }
        g = null
    };
    this._addEventListener("mousedown", this._onDragStart)
};
dhtmlXMockup.prototype._showCapture = function(e, b) {
    var c = e + "-" + b;
    if (this.dnd.cache.capture == c) {
        return
    }
    this._hideCapture();
    this.dnd.cache.capture = c;
    var d = this.items[e],
        a = window.document.createElement("div");
    a.className = "dhxmockup_capture";
    if ("column" == d.type) {
        d.node.insertBefore(a, this._nthChild(d.node, ++b))
    }
    d = a = null
};
dhtmlXMockup.prototype._hideCapture = function() {
    if (!this.dnd.cache.capture) {
        return
    }
    this.dnd.cache.capture = null;
    var a = Array.prototype.slice.call(this.conf.parent.getElementsByClassName("dhxmockup_capture"), 0);
    for (var b = 0; b < a.length; ++b) {
        a[b].parentNode.removeChild(a[b]);
        a[b] = null
    }
};
dhtmlXMockup.prototype._icons = {
    input: "icon-input",
    password: "icon-password",
    select: "icon-select",
    multiselect: "icon-multiselect",
    combo: "icon-combo",
    checkbox: "icon-checkbox",
    radio: "icon-radio",
    label: "icon-label",
    button: "icon-button",
    hidden: "icon-hidden",
    template: "icon-template",
    container: "icon-container",
    calendar: "icon-calendar",
    colorpicker: "icon-colorpicker",
    editor: "icon-editor",
    btn2state: "icon-btn2state",
    image: "icon-image",
    uploader: "icon-uploader",
    file: "icon-file",
    settings: "icon-settings"
};
dhtmlXMockup.prototype._initContextMenu = function() {
    if (typeof(window.dhtmlXMenuObject) != "function") {
        return
    }
    if (this.menu instanceof window.dhtmlXMenuObject) {
        return
    }
    this.menu = new dhtmlXMenuObject({
        context: true,
        items: [{
            id: "new_column",
            text: "Add New Column"
        }, {
            id: "new_block",
            text: "Add New Block"
        }, {
            id: "new_item",
            text: "Add New Item"
        }]
    });
    this.menu._mockup = this;
    this._contextMenuDoOnClick = function(c) {
        var b = this._mockup.conf.menu_column_id;
        if (c == "new_item") {
            this._mockup.addItem(b, {
                type: "input",
                label: "New Input",
                value: ""
            }, {
                callEvent: true
            })
        }
        if (c == "new_block") {
            this._mockup.addBlock(b, {
                type: "block"
            }, {
                callEvent: true
            })
        }
        if (c == "new_column") {
            var a = this._mockup.items[b].parentId;
            this._mockup.addColumn(a, {
                type: "column"
            }, {
                callEvent: true
            })
        }
    };
    this._contextMenuDoOnHide = function(a) {
        if (a == null) {
            this._mockup.conf.menu_column_id = null
        }
    };
    this.menu.attachEvent("onClick", this._contextMenuDoOnClick);
    this.menu.attachEvent("onHide", this._contextMenuDoOnHide)
};
dhtmlXMockup.prototype._showContextMenu = function(a) {
    if (this.menu != null && a.parentNode.parentNode._type == "column") {
        this.menu.showContextMenu(window.dhx4.absLeft(a), window.dhx4.absTop(a) + a.offsetHeight);
        this.conf.menu_column_id = a.parentNode.parentNode._idd
    }
    a = null
};
dhtmlXMockup.prototype._unloadContextMenu = function() {
    if (this.menu != null && typeof(this.menu.unload) == "function") {
        this.menu._mockup = null;
        this.menu.unload();
        this.menu = null
    }
};
dhtmlXMockup.prototype._initSelection = function() {
    this.conf.ev_sel = {
        on_click: this.attachEvent("_onClick", this._checkSelection),
        on_delete: this.attachEvent("onDelete", this._fixSelectionOnDelete)
    };
    this._initAreaSelect()
};
dhtmlXMockup.prototype._unloadSelection = function() {
    for (var b in this.conf.ev_sel) {
        this.detachEvent(this.conf.ev_sel[b])
    }
    for (var b in this.conf.selected) {
        delete this.conf.selected[b]
    }
    this.conf.selected = this.conf.ev_sel = null;
    if (this.areaSelect != null) {
        this._unloadAreaSelect()
    }
};
dhtmlXMockup.prototype._checkSelection = function(b) {
    var a = b.target || b.srcElement;
    while (a != null) {
        if (a == null || a == this.conf.parent) {
            this.clearSelection();
            a = null
        } else {
            if (a.className != null) {
                if (a.className.match(/dhxmockup_el_item/) != null) {
                    var c = a._idd;
                    if ((b.ctrlKey == true || this.conf.mac_cmd_key == true) && b.shiftKey == false && b.altKey == false) {
                        this._setSelected(c, !this._isSelected(c))
                    } else {
                        if (b.ctrlKey == false && b.shiftKey == false && b.altKey == false && this.conf.mac_cmd_key == false) {
                            if (!this.clearSelection(c)) {
                                this._setSelected(c, true)
                            }
                        }
                    }
                    a = null
                } else {
                    a = a.parentNode
                }
            }
        }
    }
};
dhtmlXMockup.prototype._setSelected = function(c, b, a) {
    if (typeof(a) == "undefined") {
        a = true
    } else {
        a = window.dhx4.s2b(a)
    }
    if (b == true) {
        if (this.conf.selected[c] != true) {
            this.items[c].node.className += " dhxmockup_el_selected";
            this.conf.selected[c] = true;
            if (a == true) {
                this.callEvent("onSelect", [c, true])
            }
        }
    } else {
        if (this.conf.selected[c] == true) {
            this.items[c].node.className = String(this.items[c].node.className).replace(/\s*dhxmockup_el_selected/gi, "");
            delete this.conf.selected[c];
            if (a == true) {
                this.callEvent("onSelect", [c, false])
            }
        }
    }
};
dhtmlXMockup.prototype._isSelected = function(a) {
    return (this.conf.selected[a] == true)
};
dhtmlXMockup.prototype.clearSelection = function(d) {
    var c = false;
    for (var b in this.conf.selected) {
        if (d != null && b == d) {
            c = true
        } else {
            this._setSelected(b, false)
        }
    }
    return c
};
dhtmlXMockup.prototype._fixSelectionOnDelete = function(b, a) {
    if (this.conf.selected[b] == true) {
        delete this.conf.selected[b]
    }
};
dhtmlXMockup.prototype.selectItem = function(c, b, a) {
    if (this.items[c] == null) {
        console.log("unexisting item selection", arguments);
        return
    }
    this._setSelected(c, b, window.dhx4.s2b(a))
};
dhtmlXMockup.prototype._initAreaSelect = function() {
    if (typeof(window.dhtmlXAreaSelect) != "function" || this.areaSelect != null) {
        return
    }
    var a = this;
    this._onAreaSelectStart = function() {
        a.clearSelection();
        if (this.conf.mockup_nodes == null) {
            this.conf.mockup_nodes = {}
        }
        for (var b in a.items) {
            if (a.items[b].type == "item") {
                this.conf.mockup_nodes[b] = a.items[b].node
            }
        }
        this.setNodes(this.conf.mockup_nodes)
    };
    this._onAreaSelectStop = function() {
        for (var b in this.conf.mockup_nodes) {
            this.conf.mockup_nodes[b] = null;
            delete this.conf.mockup_nodes[b]
        }
        var c = [];
        for (var b in a.conf.selected) {
            c.push(b)
        }
        a.callEvent("onSelect", [c, true])
    };
    this._onAreaSelectCancel = function() {};
    this._onAreaSelectNodeSelect = function(c, b) {
        a._setSelected(c, b, false)
    };
    this._onAreaSelectBeforeStart = function(c) {
        var b = c.target || c.srcElement;
        if (b != a.conf.parent) {
            return false
        }
        return true
    };
    this._onAreaSelectUnload = function() {
        if (this.conf.is_mac == true) {
            window.removeEventListener("keydown", this._macOnKey, false);
            window.removeEventListener("keyup", this._macOnKey, false)
        }
        a = null
    };
    this.areaSelect = new dhtmlXAreaSelect(this.conf.parent);
    this.areaSelect.attachEvent("onBeforeSelectStart", this._onAreaSelectBeforeStart);
    this.areaSelect.attachEvent("onSelectStart", this._onAreaSelectStart);
    this.areaSelect.attachEvent("onSelectStop", this._onAreaSelectStop);
    this.areaSelect.attachEvent("onSelectCancel", this._onAreaSelectCancel);
    this.areaSelect.attachEvent("onNodeSelect", this._onAreaSelectNodeSelect);
    if (this.conf.is_mac == true) {
        this._macOnKey = function(b) {
            if (((window.dhx4.isKHTML || window.dhx4.isChrome || window.dhx4.isOpera) && (b.keyCode == 91 || b.keyCode == 93)) || (window.dhx4.isFF && b.keyCode == 224)) {
                a.conf.mac_cmd_key = (b.type == "keydown")
            }
        };
        window.addEventListener("keydown", this._macOnKey, false);
        window.addEventListener("keyup", this._macOnKey, false)
    }
};
dhtmlXMockup.prototype._unloadAreaSelect = function() {
    this._onAreaSelectUnload();
    this.areaSelect.unload();
    this.areaSelect = null;
    for (var b in this) {
        if (String(b).match(/^_onAreaSelect/) != null) {
            delete this[b]
        }
    }
    that = null
};

function dhtmlXPropertyView(a) {
    var b = this;
    if (typeof(a) == "string") {
        a = {
            parent: a,
            clear: true
        }
    } else {
        if (typeof(a) == "object" && typeof(a.tagName) != "undefined") {
            a = {
                parent: a,
                clear: true
            }
        }
    }
    if (typeof(a.parent) == "string") {
        a.parent = document.getElementById(a.parent)
    }
    this.conf = {
        skin: (a.skin || window.dhx4.skin || (typeof(dhtmlx) != "undefined" ? dhtmlx.skin : null) || window.dhx4.skinDetect("dhxpropview") || "dhx_skyblue"),
        labelWidth: a.labelWidth || 100
    };
    this.cont = a.parent;
    this.cont.className += " dhxpv_cont_" + this.conf.skin;
    this.t = {};
    this.addItem = function(c) {
        if (!(c instanceof Array)) {
            c = [c]
        }
        for (var d = 0; d < c.length; d++) {
            if (typeof(c[d]) != "undefined" && c[d].type != null && this.items[c[d].type] != null) {
                this._addItem(c[d])
            }
        }
    };
    this._addItem = function(d) {
        var e = (d.name != null ? d.name : window.dhx4.newId());
        while (this.t[e] != null) {
            e = window.dhx4.newId()
        }
        var c = document.createElement("DIV");
        c.className = "dhxpv_item dhxpv_type_" + d.type;
        this.cont.appendChild(c);
        c._idd = e;
        c._conf = window.dhx4._copyObj(d);
        if (c._conf.labelWidth == null) {
            c._conf.labelWidth = this.conf.labelWidth
        }
        c._conf.edit_start = this._editStart;
        c._conf.edit_stop = this._editStop;
        c._conf.call_event = this._callEvent;
        this.items[d.type].render(c);
        this.t[e] = c
    };
    this.setSizes = function() {
        for (var c in this.t) {
            if (typeof(this.items[this.t[c]._conf.type].setSizes) == "function") {
                this.items[this.t[c]._conf.type].setSizes(this.t[c])
            }
        }
    };
    this.setValues = function(e) {
        for (var c in e) {
            var d = this.t[c];
            if(d != null) {
            	if(c == 'validate'){
                	var v = e[c];
                	v = v.split(",");
                	if(v.indexOf('isNotEmpty')>=0){
                		this.items[this.t['isNotEmpty']._conf.type].setValue(this.t['isNotEmpty'], true);
                		v.splice([v.indexOf('isNotEmpty')],1);
                	}
                	if(v.length > 0){
                		this.items[d._conf.type].setValue(d, v[0]);
                	}
                }else{
                	if(c == 'label' && e[c] == ""){
                		this.items[d._conf.type].setValue(d, "#NO_LABEL#");
                	}else{
                		this.items[d._conf.type].setValue(d, e[c]);
                	}
                }
                d = null
            }else if(c == "userdata"){
            	for(var f in e[c]){
            		var g = this.t[f];
            		if(g != null){
            			this.items[g._conf.type].setValue(g, e[c][f]);
            		}
            	}
            }
        }
    };
    this.getValues = function() {
        var e = {};
        for (var c in this.t) {
            var d = this.t[c];
            if (typeof(this.items[d._conf.type].getValue) == "function") {
                e[c] = this.items[d._conf.type].getValue(d)
            }
            d = null
        }
        return e
    };
    this._editStart = function(d, c) {
        if (typeof(b.items[b.t[d]._conf.type].editStart) == "function") {
            if (b.items[b.t[d]._conf.type].editStart(b.t[d], c)) {
                b.conf.edit_mode = true;
                b.conf.edit_id = d
            }
        }
    };
    this._editStop = function(c, d) {
        if (b.conf.edit_id != null) {
            var f = b.t[b.conf.edit_id];
            b.items[f._conf.type].editStop(f, (c == true))
        }
        b.conf.edit_id = null;
        b.conf.edit_mode = false;
        if (d != null) {
            var e = null;
            while (f != null && e == null) {
                f = f[d];
                if (f != null && typeof(b.items[b.t[f._idd]._conf.type].editStart) == "function") {
                    e = f._idd
                }
            }
            if (e != null) {
                b._editStart(e, true)
            }
        }
        f = null
    };
    this._doOnClick = function(d) {
        d = d || event;
        var c = (d.target || d.srcElement);
        if (c.className != null) {
            if (c.className.match(/dhxpv_item_value/) != null) {
                return
            }
            if (c.className.match(/dhxpv_item_label/) != null) {
                var f = c.parentNode._idd;
                
                if(f == 'module') return;
                
                if (b.conf.edit_id != f) {
                    if (b.conf.edit_mode == true) {
                        b._editStop()
                    }
                    b._editStart(f, true)
                }
                return
            }
        }
        if (b.conf.edit_mode == true) {
            b._editStop(true)
        }
    };
    this._doOnMouseDown = function(d) {
        if (b.conf.edit_mode == true) {
            d = d || event;
            var c = (d.target || d.srcElement);
            if ((c.tagName || "").toLowerCase() == "select" && (c.className || "").match(/dhxpv_item_editor/gi) != null && c.parentNode.parentNode._idd == b.conf.edit_id) {} else {
                b._editStop(true)
            }
        }
    };
    if (typeof(window.addEventListener) == "function") {
        window.addEventListener("click", this._doOnClick, false);
        window.addEventListener("mousedown", this._doOnMouseDown, false)
    } else {
        document.body.attachEvent("onclick", this._doOnClick);
        document.body.attachEvent("onmousedown", this._doOnMouseDown)
    }
    this._initObj = function(c) {
        this.addItem(c);
        this.setSizes()
    };
    this._xmlToObject = function() {};
    window.dhx4._enableDataLoading(this, "_initObj", "_xmlToObject", "items", {
        struct: true,
        data: true
    });
    window.dhx4._eventable(this);
    this._callEvent = function(d, c) {
        return b.callEvent(d, c)
    };
    this.clearAll = function() {
        for (var d in this.t) {
            var e = this.t[d];
            if (typeof(this.items[e._conf.type].destruct) == "function") {
                this.items[e._conf.type].destruct(e)
            }
            if (this.conf.edit_mode == true && this.conf.edit_id == e._idd) {
                this.conf.edit_id = null;
                this.conf.edit_mode = false
            }
            for (var c in e._conf) {
                e._conf[c] = null
            }
            e._conf = null;
            e._idd = null;
            e.parentNode.removeChild(e);
            e = null;
            this.t[d] = null;
            delete this.t[d]
        }
    };
    this.unload = function() {
        if (typeof(window.addEventListener) == "function") {
            window.removeEventListener("click", this._doOnClick, false);
            window.removeEventListener("mousedown", this._doOnMouseDown, false)
        } else {
            document.body.detachEvent("onclick", this._doOnClick);
            document.body.detachEvent("onmousedown", this._doOnMouseDown)
        }
        this.clearAll();
        for (var c in this) {
            this[c] = null
        }
        b = null
    };
    if (a.items != null || a.json != null || a.xml != null) {
        this.loadStruct(a.items || a.json || a.xml)
    }
    if (a.clear == true) {
        a.parent = null
    }
    
    this.attachEvent('propertyChange', function(target, node, value) {
    	if(this.t[node._idd]) {
    		var el = this.t[node._idd];
    		el._conf.value = value;
    		el.lastChild.firstChild.innerHTML = value;
    		el._conf.edit_mode = false;
    		el.lastChild.firstChild.style.display = '';
    		el._conf.call_event('onChange', [node._idd, value]);
    	}
    });
    return this
}
dhtmlXPropertyView.prototype._getExtraCSS = function() {
    var d = null;
    var c = {
        ie6: "isIE6",
        ie7: "isIE7",
        ie8: "isIE8"
    };
    for (var b in c) {
        if (d == null && window.dhx4[c[b]] == true) {
            d = "dhxpv_" + b
        }
    }
    return (d || "")
};
dhtmlXPropertyView.prototype.items = {};
dhtmlXPropertyView.prototype.items.input = {
    tpl: "<div class='dhxpv_item_label' style='width: #labelWidth#px;'></div><div class='dhxpv_item_value' style='left: #valueLeft#px; width: #valueWidth#px;'><div class='dhxpv_item_inp' role='link' tabindex='0'></div></div>",
    dimFix: null,
    render: function(b, c) {
        if (b._conf.labelWidth == null) {
            b._conf.labelWidth = 100
        }
        var a = {
            labelWidth: b._conf.labelWidth,
            valueLeft: b._conf.labelWidth,
            valueWidth: b.clientWidth - b._conf.labelWidth
        };
        if (this.dimFix == null) {
            b.innerHTML = window.dhx4.template(this.tpl, a);
            this.dimFix = {
                label: b.firstChild.offsetWidth - a.labelWidth,
                value: b.lastChild.offsetWidth - a.valueWidth
            }
        }
        a.labelWidth -= this.dimFix.label;
        a.valueWidth -= this.dimFix.value;
        b.innerHTML = window.dhx4.template(this.tpl, a);
        this.setLabel(b, b._conf.label || b._idd);
        this.setValue(b, b._conf.value);
        this.initEvents(b);
        b = null
    },
    destruct: function(a) {
        if (a._conf.edit_mode == true) {
            this.editStop(a, false)
        }
        this.detachEvents(a);
        a = null
    },
    initEvents: function(b) {
        var a = this;
        b.lastChild.firstChild.onfocus = function() {
            b._conf.edit_stop(true);
            b._conf.edit_start(b._idd);
            dhtmlxApp.prototype.propertyFocus(a, b);
        }
    },
    detachEvents: function(a) {
        a.firstChild.onselectstart = null;
        a.lastChild.firstChild.onfocus = null;
        a = null
    },
    editStart: function(b) {
        if ((window.dhx4.isIE6 || window.dhx4.isIE7 || window.dhx4.isIE8) && dhtmlXPropertyView.prototype._lastCheckboxFocus != null) {
            dhtmlXPropertyView.prototype._lastCheckboxFocus = null
        }
        b.lastChild.firstChild.style.display = "none";
        var c = (typeof(b._conf.value) == "undefined" || b._conf.value == null ? "" : String(b._conf.value));
        var a = document.createElement("INPUT");
        a.className = "dhxpv_item_editor " + dhtmlXPropertyView.prototype._getExtraCSS();
        a.value = c;
        b.lastChild.appendChild(a);
        b._conf.edit_mode = true;
        a.onclick = function(d) {
            d = d || event;
            d.cancelBubble = true
        };
        a.onmousedown = function(d) {
            d = d || event;
            d.cancelBubble = true
        };
        a.onkeydown = function(d) {
            d = d || event;
            if (d.keyCode == 13 || d.keyCode == 13) {
                b._conf.edit_stop(true, "nextSibling")
            } else {
                if (d.keyCode == 27 || d.charCode == 27) {
                    b._conf.edit_stop(false)
                }
            }
        };
        window.setTimeout(function() {
            a.focus();
            a.value = "";
            a.value = c;
            a = null
        }, 1);
        return true
    },
    editStop: function(c, a) {
        if (c._conf.edit_mode == true) {
            var b = c.lastChild.lastChild;
            b.onclick = b.onmousedown = b.onkeydown = b.onchange = null;
            b.parentNode.removeChild(b);
            var d = (a == true && b.value != c._conf.value);
            this.setValue(c, (a == true ? b.value : c._conf.value));
            b = null;
            c._conf.edit_mode = false;
            c.lastChild.firstChild.style.display = "";
            if (window.dhx4.isIE6 || window.dhx4.isIE7 || window.dhx4.isIE8) {
                window.setTimeout(function() {
                    if (dhtmlXPropertyView.prototype._lastCheckboxFocus != null) {
                        dhtmlXPropertyView.prototype._lastCheckboxFocus.focus();
                        dhtmlXPropertyView.prototype._lastCheckboxFocus = null
                    }
                }, 135)
            }
            if (d == true) {
                c._conf.call_event("onChange", [c._idd, c._conf.value])
            }
        }
        c = null
    },
    setSizes: function(b) {
        var a = Math.max(b.clientWidth - b._conf.labelWidth - this.dimFix.value, 0);
        b.lastChild.style.width = a + "px";
        b = null
    },
    setLabel: function(b, a) {
        b.firstChild.innerHTML = a;
        this.setLabelSelect(b);
        b = null
    },
    setLabelSelect: function(a) {
        if ((window.dhx4.isIE6 || window.dhx4.isIE7 || window.dhx4.isIE8) && a.firstChild.onselectstart == null) {
            a.firstChild.onselectstart = function(b) {
                b = b || event;
                b.returnValue = false;
                return false
            }
        }
        a = null
    },
    setValue: function(a, b) {
        a._conf.value = b;
        a.lastChild.firstChild.innerHTML = (typeof(b) == "undefined" || b == null ? "" : String(b));
        a = null
    },
    getValue: function(a) {
        var b = a._conf.value;
        a = null;
        return b
    }
};
dhtmlXPropertyView.prototype.items.group = {
    tpl: "<div class='dhxpv_item_label'>#label#</div>",
    render: function(a) {
        a.innerHTML = window.dhx4.template(this.tpl, {
            label: a._conf.label
        });
        a = null
    }
};
dhtmlXPropertyView.prototype.items.checkbox = {
    tpl: "<div class='dhxpv_item_label' style='width: #labelWidth#px;'><label></label></div><div class='dhxpv_item_value' style='left: #valueLeft#px; width: #valueWidth#px;'><div class='dhxpv_chbx dhxpv_chbx_state_0' role='link' tabindex='0'></div></div>",
    initEvents: function(c) {
        var a = c.lastChild.firstChild;
        var b = this;
        a.onfocus = function() {
            if (window.dhx4.isIE6 || window.dhx4.isIE7 || window.dhx4.isIE8) {
                dhtmlXPropertyView.prototype._lastCheckboxFocus = this
            }
            if (this.parentNode.className.match(/actv/) == null) {
                this.parentNode.className += " dhxpv_item_actv"
            }
            var d = (window.dhx4.isIE6 || window.dhx4.isIE7 ? 120 : 1);
            var e = this;
            window.setTimeout(function() {
                c._conf && c._conf.edit_stop(true)
            }, 1);
            this._tmt = false
        };
        a.onblur = function() {
            this._tmt = true;
            var d = this;
            window.setTimeout(function() {
                if (d._tmt == true && d.parentNode.className.match(/actv/) != null) {
                    d.parentNode.className = d.parentNode.className.replace(/\s{0,}dhxpv_item_actv/gi, "")
                }
                d = null
            }, 20)
        };
        a.onkeydown = function(d) {
            d = d || event;
            if (d.keyCode == 13 || d.charCode == 13 || d.keyCode == 32 || d.charCode == 32) {
                b.setValue(c, !b.getValue(c), true)
            }
        };
        a.onclick = function() {
            b.setValue(c, !b.getValue(c), true)
        };
        a = null
    },
    detachEvents: function(b) {
        b.firstChild.firstChild.onmousedown = null;
        var a = b.lastChild.firstChild;
        a.onfocus = a.onblur = a.onkeypress = a.onclick = null;
        a = b = null
    },
    setLabel: function(c, a) {
        c.firstChild.firstChild.innerHTML = a;
        if (c.firstChild.firstChild.onmousedown == null) {
            var b = this;
            c.firstChild.firstChild.onmousedown = function(d) {
                d = d || event;
                d.cancelBubble = true;
                window.setTimeout(function() {
                    b.setValue(c, !c._conf.value);
                    c.lastChild.firstChild.focus()
                }, 1);
                return false
            }
        }
        this.setLabelSelect(c)
    },
    setValue: function(a, b, c) {
        if (c == true && b == a._conf.value) {
            c = false
        }
        a._conf.value = window.dhx4.s2b(b);
        a.lastChild.firstChild.className = a.lastChild.firstChild.className.replace(/dhxpv_chbx_state_\d/gi, "dhxpv_chbx_state_" + String(a._conf.value ? 1 : 0));
        if (c == true) {
            a._conf.call_event("onChange", [a._idd, a._conf.value])
        }
        a = null
    },
    editStart: function(b, a) {
        if (a == true) {
            b.lastChild.firstChild.focus()
        }
        return false
    },
    editStop: function() {}
};
(function() {
    for (var b in dhtmlXPropertyView.prototype.items.input) {
        if (dhtmlXPropertyView.prototype.items.checkbox[b] == null) {
            dhtmlXPropertyView.prototype.items.checkbox[b] = dhtmlXPropertyView.prototype.items.input[b]
        }
    }
})();
dhtmlXPropertyView.prototype._lastCheckboxFocus = null;
dhtmlXPropertyView.prototype.items.select = {
    setValue: function(a, c) {
        a._conf.value = c;
        var d = null;
        if (a._conf.options instanceof Array) {
            for (var b = 0; b < a._conf.options.length; b++) {
                if (typeof(a._conf.options[b]) == "string") {
                    a._conf.options[b] = {
                        text: a._conf.options[b],
                        value: a._conf.options[b]
                    }
                }
                if (a._conf.options[b].value == c) {
                    d = a._conf.options[b].text
                }
            }
        }
        a.lastChild.firstChild.innerHTML = (typeof(d) == "undefined" || d == null ? "" : String(d));
        a = null
    },
    editStart: function(d) {
        d.lastChild.firstChild.style.display = "none";
        var f = (typeof(d._conf.value) == "undefined" || d._conf.value == null ? "" : String(d._conf.value));
        var b = document.createElement("SELECT");
        b.className = "dhxpv_item_editor " + dhtmlXPropertyView.prototype._getExtraCSS();
        b.value = f;
        d.lastChild.appendChild(b);
        if (d._conf.options instanceof Array) {
            var c = -1;
            for (var e = 0; e < d._conf.options.length; e++) {
                if (typeof(d._conf.options[e]) == "string") {
                    d._conf.options[e] = {
                        text: d._conf.options[e],
                        value: d._conf.options[e]
                    }
                }
                b.add(new Option(d._conf.options[e].text, d._conf.options[e].value));
                if (d._conf.options[e].value == f) {
                    c = e
                }
            }
            if (c >= 0) {
                b.selectedIndex = c
            }
        }
        d._conf.edit_mode = true;
        b.onclick = function(g) {
            g = g || event;
            g.cancelBubble = true
        };
        b.onkeydown = function(g) {
            g = g || event;
            if (g.keyCode == 13 || g.charCode == 13) {
                d._conf.edit_stop(true, "nextSibling")
            } else {
                if (g.keyCode == 27 || g.charCode == 27) {
                    d._conf.edit_stop(false)
                }
            }
        };
        b.onchange = function() {
            d._conf.edit_stop(true)
        };
        var a = (window.dhx4.isIE6 || window.dhx4.isIE7 || window.dhx4.isIE8 ? 120 : 1);
        window.setTimeout(function() {
            b.focus();
            b = null
        }, a);
        return true
    }
};
(function() {
    for (var b in dhtmlXPropertyView.prototype.items.input) {
        if (dhtmlXPropertyView.prototype.items.select[b] == null) {
            dhtmlXPropertyView.prototype.items.select[b] = dhtmlXPropertyView.prototype.items.input[b]
        }
    }
})();
if (typeof(window.dhtmlXCellObject) != "undefined") {
    dhtmlXCellObject.prototype.attachPropertyView = function(a) {
        this.callEvent("_onBeforeContentAttach", ["propertyview"]);
        var b = document.createElement("DIV");
        b.style.cssText = "position: relative; width: 100%; height: 100%; overflow-x: hidden; overflow-y: auto;";
        this._attachObject(b);
        if (typeof(a) == "undefined" || a == null) {
            a = {}
        }
        if (a.skin == null) {
            a.skin = this.conf.skin
        }
        a.parent = b;
        this.dataType = "propertyview";
        this.dataObj = new dhtmlXPropertyView(a);
        a.parent = null;
        a = b = null;
        this.callEvent("_onContentAttach", []);
        return this.dataObj
    }
}
dhtmlXForm.prototype.emulateSelect = function(a, b, c) {
    if (arguments.length == 2) {
        c = b;
        b = null
    }
    if (b != null) {
        a = [a, b]
    }
    return this.doWithItem(a, "emuSel", c)
};
dhtmlXForm.prototype.items.input.emuSel = function(d, i) {
    var e = d.getForm();
    if (i == true) {
        if (e._emuSelCache[d._idd] == null) {
            var g = e._getItemDim(d);
            var a = g.x2 - g.x1;
            var c = g.y2 - g.y1;
            var b = document.createElement("DIV");
            b.className = "dhxform_emu_sel";
            b.style.left = g.x1 - 4 + "px";
            b.style.top = d.offsetTop + g.y1 - 2 + "px";
            b.style.width = a + 4 + "px";
            b.style.height = c + 2 + "px";
            d.parentNode.insertBefore(b, d);
            e._emuSelCache[d._idd] = b;
            b = null
        }
    } else {
        if (e._emuSelCache[d._idd] != null) {
            e._emuSelCache[d._idd].parentNode.removeChild(e._emuSelCache[d._idd]);
            e._emuSelCache[d._idd] = null;
            delete e._emuSelCache[d._idd]
        }
    }
    e = d = null
};
(function() {
    for (var b in dhtmlXForm.prototype.items) {
        if (typeof(dhtmlXForm.prototype.items[b].emuSel) == "undefined") {
            if ({
                    hidden: 1,
                    list: 1,
                    newcolumn: 1
                }[b] == 1) {
                dhtmlXForm.prototype.items[b].emuSel = function() {}
            } else {
                dhtmlXForm.prototype.items[b].emuSel = dhtmlXForm.prototype.items.input.emuSel
            }
        }
    }
})();
dhtmlXForm.prototype._emuSelCache = {};
dhtmlXForm.prototype._emuSelCacheClear = function() {
    for (var b in this._emuSelCache) {
        this._emuSelCache[b].parentNode.removeChild(this._emuSelCache[b]);
        this._emuSelCache[b] = null;
        delete this._emuSelCache[b]
    }
};
dhtmlXForm.prototype._getItemDim = function(e) {
    var h = {
        x1: +Infinity,
        x2: -Infinity,
        y1: +Infinity,
        y2: -Infinity
    };
    for (var f = 0; f < e.childNodes.length; f++) {
        var g = e.childNodes[f];
        if (g.offsetWidth == 0 && g.offsetHeight == 0) {
            continue
        }
        var b = g.offsetLeft;
        var a = b + g.offsetWidth;
        var d = g.offsetTop;
        var c = d + g.offsetHeight;
        h.x1 = Math.min(h.x1, b);
        h.x2 = Math.max(h.x2, a);
        h.y1 = Math.min(h.y1, d);
        h.y2 = Math.max(h.y2, c);
        g = null
    }
    e = null;
    return h
};