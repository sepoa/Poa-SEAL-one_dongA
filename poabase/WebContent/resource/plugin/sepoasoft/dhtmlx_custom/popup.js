/**
 * dhtmlXPopup
 */

function dhtmlXPopup(e) {
    var g = this;
    this.conf = e || {};
    e = null;
    this.mode = (this.conf.mode || "bottom");
    this.conf.zi = window.dhx4.newId();
    this.conf.context = (this.conf.context == null ? true : window.dhx4.s2b(this.conf.context));
    this.conf.IE6_display_fix = (this.conf.IE6_display_fix == null ? false : window.dhx4.s2b(this.conf.IE6_display_fix));
    this.conf.last_p_click = this.conf.last_body_click = null;
    this.p = document.createElement("DIV");
    this.p.style.display = "none";
    this.p.innerHTML = "<div class='dhx_popup_area" + (window.dhx4.isIE ? " dhx_popup_area_ie" : "") + "'><table cellspacing='0' cellpadding='0' border='0' class='dhx_popup_table'><tbody></tbody></table></div><div class='dhx_popup_arrow dhx_popup_arrow_" + this.mode + "'></div>";
    document.body.appendChild(this.p);
    this.p.oncontextmenu = function(a) {
        if (g.conf.context == false) {
            a = a || event;
            a.returnValue = false;
            return false
        }
    };
    this.skinParams = {
        dhx_terrace: {
            t0: 19,
            t1: 9,
            t2: 19,
            t3: 9
        },
        dhx_skyblue: {
            t0: 12,
            t1: 9,
            t2: 12,
            t3: 9
        },
        dhx_web: {
            t0: 12,
            t1: 9,
            t2: 12,
            t3: 9
        },
        material: {
            t0: 19,
            t1: 9,
            t2: 19,
            t3: 9
        }
    };
    this.p.ontouchstart = this.p.onclick = function(l) {
        l = l || event;
        var h = (g.conf.last_p_click != null && g.conf.last_p_click != l.type);
        g.conf.last_p_click = l.type;
        if (h == true) {
            return
        }
        g._clearClick = true;
        if (g._nodeObj != null) {
            g.callEvent("onContentClick", []);
            return true
        }
        var a = (l.target || l.srcElement);
        var m = null;
        while (a != g.p && a != null) {
            if (typeof(a._idd) != "undefined" && !a._isSeparator) {
                m = a._idd;
                a = null
            } else {
                a = a.parentNode
            }
        }
        a = null;
        if (m != null) {
            g.callEvent("onClick", [m]);
            if (g != null && g.isVisible != null && g.isVisible() && g.callEvent("onBeforeHide", ["select", l, m]) === true) {
                l.cancelBubble = true;
                g.hide()
            }
        }
    };
    this.separator = "DHXSEP_" + window.dhx4.newId();
    this.tpl = [];
    this._setTemplate = function(a) {
        this.tpl = a.split(",")
    };
    this.show = function(h) {
        var a = null;
        if (arguments.length == 1) {
            if (!h) {
                h = this.conf.id[0]
            } else {
                if (!this._idExists(h)) {
                    return
                }
            }
            if (this.conf.toolbar) {
                a = this.conf.toolbar._getItemDim(h)
            }
            if (this.conf.ribbon) {
                a = this.conf.ribbon._getItemDim(h)
            }
            if (this.conf.form) {
                a = this.conf.form._getItemDim(h)
            }
        } else {
            if (arguments.length == 4) {
                this._clearClick = true;
                a = {
                    left: arguments[0],
                    top: arguments[1],
                    width: arguments[2],
                    height: arguments[3]
                };
                h = null
            }else if(arguments.length == 5){
        		this._clearClick = true;
                a = {
                    left: arguments[0],
                    top: arguments[1],
                    width: arguments[2],
                    height: arguments[3],
                	headerLine: arguments[4]
                };
                h = null
            }
        }
        if (!a) {
            return
        }
        this.p.style.visibility = "hidden";
        this.p.style.display = "";
        this._setPos(a);
        this.p.style.zIndex = window.dhx4.zim.reserve(this.conf.zi);
        this.p.style.visibility = "visible";
        this._lastId = h;
        this.callEvent("onShow", [h])
    };
    this._setPos = function(J, n) {
        var A = J.left;
        var u = J.top;
        var D = J.width;
        var M = J.height;
        var HL = J.headerLine;
        this._posData = {
            left: A,
            top: u,
            width: D,
            height: M
        };
        var O = window.dhx4.screenDim();
        var H = n || this.mode;
        if (typeof(n) == "undefined") {
            n = false
        }
        var Q = {
            top: (u - this.p.offsetHeight) - O.top,
            bottom: O.bottom - (u + M + this.p.offsetHeight),
            left: A - this.p.offsetWidth - O.left,
            right: O.right - (A + D + this.p.offsetWidth)
        };
        if (!n && Q[H] < 0) {
            var K = this._getAvailPos(H, Q);
            if (K !== false) {
                this._setPos(J, K);
                return
            }
        }
        if (H == "top" || H == "bottom") {
            var E = this.skinParams[this.conf.skin].t2;
            var C = this.skinParams[this.conf.skin].t3;
            var s = Math.round(this.p.offsetWidth / 2);
            var a = Math.round(this.p.lastChild.offsetWidth / 2);
            if (A < O.left) {
                var N = Math.min(A + D, O.left);
                D = A + D - N;
                A = N
            }
            if (A + D > O.right) {
                D = O.right - A
            }
            var v = Math.round(A + D / 2);
            var m = v - s;
            var L = v - E - a;
            var r = v + a + E - this.p.offsetWidth;
            if (m < O.left - C) {
                m = Math.min(O.left - C, L)
            } else {
                if (m + this.p.offsetWidth > O.right + C) {
                    m = Math.max(r, O.right + C - this.p.offsetWidth)
                }
            }
            this.p.style.left = m + "px";
            this.p.style.top = (H == "top" ? u - this.p.offsetHeight : u + M) + "px";
            v = v - m - a;
            this.p.lastChild.className = "dhx_popup_arrow dhx_popup_arrow_" + H;
            this.p.lastChild.style.top = (H == "top" ? this.p.offsetHeight - this.p.lastChild.offsetHeight : 0) + "px";
            this.p.lastChild.style.left = v + "px"
        }
        if (H == "left" || H == "right") {
            var E = this.skinParams[this.conf.skin].t0;
            var C = this.skinParams[this.conf.skin].t1;
            var o = Math.round(this.p.offsetHeight / 2);
            var T = Math.round(this.p.lastChild.offsetHeight / 2);
            if (u < O.top) {
                var l = Math.min(u + M, O.top);
                M = u + M - l;
                u = l
            }
            if (u + M > O.bottom) {
                M = O.bottom - u
            }
            var v = Math.round(u + M / 2);
            var I = v - o;
            var S = v - E - T;
            var P = v + T + E - this.p.offsetHeight;
            if (I < O.top - C) {
                I = Math.min(O.top - C, S)
            } else {
                if (I + this.p.offsetHeight > O.bottom + C) {
                    I = Math.max(P, O.bottom + C - this.p.offsetHeight)
                }
            }
            this.p.style.left = (H == "left" ? A - this.p.offsetWidth : A + D) + "px";
            if( I<HL ){
            	I+=HL;
            }
            this.p.style.top = I + "px";
        	v = v - I - T;
            
            this.p.lastChild.className = "dhx_popup_arrow dhx_popup_arrow_" + H;
            this.p.lastChild.style.left = (H == "left" ? this.p.offsetWidth - this.p.lastChild.offsetWidth : 0) + "px";
            this.p.lastChild.style.top = v + "px"
        }
        if (this._IEDisp && this._nodeId != null) {
            var F = document.getElementById(this._nodeId);
            if (this.conf.IE6_display_fix == true) {
                F.style.visibility = "hidden"
            }
            window.setTimeout(function() {
                F.style.visibility = "visible";
                F = null
            }, 1)
        }
    };
    this._getAvailPos = function(r, o) {
        var l = {
            top: ["bottom", "right", "left"],
            bottom: ["top", "right", "left"],
            left: ["right", "bottom", "top"],
            right: ["left", "bottom", "top"]
        };
        var m = null;
        for (var n = 0; n < l[r].length; n++) {
            if (m == null && o[l[r][n]] > 0) {
                m = l[r][n]
            }
        }
        if (m == null) {
            m = "bottom";
            for (var h in o) {
                if (o[h] > o[m]) {
                    m = h
                }
            }
        }
        if (m == r) {
            return false
        }
        return m
    };
    this._repaint = function() {
        if (this.isVisible()) {
            this._setPos(this._posData)
        }
    };
    this.clear = function() {
        if (this._nodeObj) {
            if (window.dhx4.isIE && typeof(window.dhtmlXLayoutObject) == "function" && this._nodeObj instanceof window.dhtmlXLayoutObject) {
                this.p.onmousedown = null
            }
            if (this._nodeObj.unload) {
                this._nodeObj.unload()
            } else {
                if (this._nodeObj.destruct) {
                    this._nodeObj.destruct()
                }
            }
            this._nodeObj = this._nodeId = null;
            if (this._nodeObjEv != null) {
                for (var h = 0; h < this._nodeObjEv.length; h++) {
                    this.detachEvent(this._nodeObjEv[h])
                }
                this._nodeObjEv = null
            }
        }
        if (this._IEHoverInited) {
            this._IEHoverClear()
        }
        var a = this.p.firstChild.firstChild.firstChild;
        while (a.childNodes.length > 0) {
            a.removeChild(a.lastChild)
        }
        a = null;
        this.itemData = {}
    };
    this.hide = function() {
        if (this.p.style.display != "none") {
            this.p.style.display = "none";
            window.dhx4.zim.clear(this.conf.zi);
            var a = this._lastId;
            this._lastId = null;
            this.callEvent("onHide", [a]);
            this.conf.last_p_click = this.conf.last_body_click = null
        }
    };
    this.isVisible = function() {
        return (this.p.style.display == "")
    };
    this.itemData = {};
    this.getItemData = function(a) {
        if (!a) {
            return this.itemData
        }
        if (this.itemData[a]) {
            return this.itemData[a]
        }
        return {}
    };
    this.setSkin = function(a) {
        this.conf.skin = a;
        this.p.className = "dhx_popup_" + this.conf.skin;
        if (this._nodeObj != null && typeof(this._nodeObj.setSkin) == "function") {
            this._nodeObj.setSkin(this.conf.skin)
        }
        this._repaint()
    };
    this.attachList = function(l, s) {
        this._setTemplate(l);
        this.clear();
        var m = this.p.firstChild.firstChild.firstChild;
        for (var o = 0; o < s.length; o++) {
            var n = document.createElement("TR");
            if (s[o] != this.separator) {
                if (typeof(s[o].id) == "undefined" || s[o].id == null) {
                    n._idd = window.dhx4.newId();
                    while (this.itemData[n._idd] != null) {
                        n._idd = window.dhx4.newId()
                    }
                } else {
                    n._idd = s[o].id
                }
                this.itemData[n._idd] = s[o]
            }
            m.appendChild(n);
            if (s[o] == this.separator) {
                n.className = "dhx_popup_sep";
                n._isSeparator = true;
                var u = document.createElement("TD");
                u.className = "dhx_popup_sep";
                u.colSpan = this.tpl.length;
                u.innerHTML = "<div class='dhx_popup_sep'>&nbsp;</div>";
                n.appendChild(u);
                u = null
            } else {
                for (var a = 0; a < this.tpl.length; a++) {
                    var h = "dhx_popup_td";
                    if (this._IEFirstLast && (this.tpl.length == 1 || a == 0 || a == this.tpl.length - 1)) {
                        if (this.tpl.length == 1) {
                            h += " dhx_popup_td_single"
                        } else {
                            h += (a == 0 ? " dhx_popup_td_first" : " dhx_popup_td_last")
                        }
                    }
                    var u = document.createElement("TD");
                    u.className = h;
                    u.innerHTML = s[o][this.tpl[a]] || "&nbsp;";
                    n.appendChild(u);
                    u = null
                }
                if (this._IEHover) {
                    n._IEHover = true;
                    if (!this._IEHoverInited) {
                        this._IEHoverInit()
                    }
                }
            }
            n = null
        }
        m = null;
        this._repaint()
    };
    this._attachNode = function(m, l) {
        this.clear();
        this._nodeId = "dhxpopup_node_" + window.dhx4.newId();
        var a = this.p.firstChild.firstChild.firstChild;
        var h = document.createElement("TR");
        h.className = "dhxnode";
        a.appendChild(h);
        var n = document.createElement("TD");
        n.className = "dhx_popup_td";
        n.innerHTML = "<div id='" + this._nodeId + "' style='position:relative;'></div>";
        if (l.width) {
            n.firstChild.style.width = l.width + "px"
        }
        if (l.height) {
            n.firstChild.style.height = l.height + "px"
        }
        h.appendChild(n);
        n = h = a = null;
        if (typeof(this["_attach_init_" + m]) == "function") {
            this["_attach_init_" + m](l);
            this._enableIEVFix()
        }
        this._repaint();
        return this._nodeObj
    };
    this.unload = function() {
        if (typeof(window.addEventListener) == "function") {
            window.removeEventListener("touchstart", this._doOnClick, false);
            window.removeEventListener("click", this._doOnClick, false);
            window.removeEventListener("keyup", this._doOnKeyUp, false);
            window.removeEventListener("unload", this._doOnUnload, false)
        } else {
            document.body.detachEvent("onclick", this._doOnClick, false);
            document.body.detachEvent("onkeyup", this._doOnKeyUp, false);
            document.body.detachEvent("onunload", this._doOnUnload, false)
        }
        window.dhx4.detachEvent(this.conf.ev_grid_click);
        this.clear();
        if (this.conf.toolbarEvent != null && this.conf.toolbar != null) {
            if (this.conf.toolbar.detachEvent != null) {
                this.conf.toolbar.detachEvent(this.conf.toolbarEvent)
            } else {
                this.conf.toolbar._getItemDim = null
            }
        }
        if (this.conf.ribbonEvent != null && this.conf.ribbon != null) {
            if (this.conf.ribbon.detachEvent != null) {
                this.conf.ribbon.detachEvent(this.conf.ribbonEvent)
            } else {
                this.conf.ribbon._getItemDim = null
            }
        }
        if (this.conf.slider != null) {
            for (var l = 0; l < this.conf.slider_events.length; l++) {
                this.conf.slider.detachEvent(this.conf.slider_events[l])
            }
            this.conf.slider_events = null;
            this._sliderShow = this._sliderHide = null;
            this.conf.slider = null
        }
        window.dhx4._eventable(this, "clear");
        this.p.onclick = this.p.ontouchstart = this.p.oncontextmenu = null;
        this.p.parentNode.removeChild(this.p);
        this.p = null;
        for (var h in this.conf) {
            this.conf[h] = null
        }
        for (var h in this) {
            this[h] = null
        }
        g = null
    };
    window.dhx4._eventable(this);
    this._doOnClick = function(s) {
        s = s || event;
        var a = (g.conf.last_body_click != null && g.conf.last_body_click != s.type);
        g.conf.last_body_click = s.type;
        if (a == true) {
            return
        }
        if (g._clearClick == true) {
            g._clearClick = false;
            return
        }
        if (g.conf.form != null) {
            var l;
            var u = (s.target || s.srcElement);
            if ((u.tagName || "").toLowerCase() == "option") {
                u = u.parentNode
            }
            if (u.className != null && u.className.search("dhxform") >= 0) {
                if (u.parentNode != null && u.parentNode.parentNode != null && u.parentNode.parentNode._idd != null) {
                    l = u.parentNode.parentNode._idd;
                    if (u.parentNode.parentNode._type == "ra") {
                        l = [u.parentNode.parentNode._group, u.parentNode.parentNode._value]
                    }
                }
            } else {
                var n = true;
                var o = false;
                while (n && !o) {
                    var m = (u.className || "").toLowerCase();
                    if (m.length > 0) {
                        o = (m == "dhxform_btn" || m.search(/dhxeditor_inside/gi) >= 0 || m == "dhxcombo_input" || m.search(/dhxcombolist/gi) >= 0)
                    }
                    u = u.parentNode;
                    n = (u != null)
                }
                if (o) {
                    return
                }
            }
            u = null;
            if (l != null && g._idExists(l)) {
                return
            }
        }
        if (typeof(window.dhtmlXForm) == "function" && g._nodeObj instanceof window.dhtmlXForm) {
            var v = {};
            var m = 0;
            var h = g._nodeObj;
            h.forEachItem(function(r) {
                if (h.getItemType(r) == "combo") {
                    v[h.getCombo(r).list._listId] = true;
                    m++
                }
            });
            h = null;
            if (m > 0) {
                var u = (s.target || s.srcElement);
                var n = true;
                var o = false;
                while (n == true && o != true) {
                    var m = (u.className || "").toLowerCase();
                    if (m.length > 0 && m.search(/^dhxcombolist/gi) >= 0 && u._listId != null && v[u._listId] == true) {
                        o = true;
                        u = null
                    } else {
                        u = u.parentNode;
                        n = (u != null)
                    }
                }
                if (o == true) {
                    return
                }
            }
        }
        if (g.isVisible() && g.callEvent("onBeforeHide", ["click", s]) === true) {
            g.hide()
        }
    };
    this._doOnKeyUp = function(a) {
        a = a || event;
        if (a.keyCode == 27) {
            if (g.isVisible() && g.callEvent("onBeforeHide", ["esc", a]) === true) {
                g.hide()
            }
        }
    };
    this._doOnUnload = function() {
        g.unload()
    };
    if (typeof(window.addEventListener) == "function") {
        window.addEventListener("touchstart", this._doOnClick, false);
        window.addEventListener("click", this._doOnClick, false);
        window.addEventListener("keyup", this._doOnKeyUp, false);
        window.addEventListener("unload", this._doOnUnload, false)
    } else {
        document.body.attachEvent("onclick", this._doOnClick, false);
        document.body.attachEvent("onkeyup", this._doOnKeyUp, false);
        document.body.attachEvent("onunload", this._doOnUnload, false)
    }
    this.conf.ev_grid_click = window.dhx4.attachEvent("_onGridClick", function(h, a) {
        if (g._nodeObj != null && (g._nodeObj == a || g._findGrid(g._nodeObj, a) == true)) {} else {
            g._clearClick = false;
            g._doOnClick(h)
        }
        a = null
    });
    this._findGrid = function(h, a) {
        var l = false;
        if (typeof(window.dhtmlXTabBar) == "function" && h instanceof window.dhtmlXTabBar) {
            h.forEachTab(function(m) {
                var n = m.getAttachedObject();
                if (l == false && n != null) {
                    l = l || (n == a) || this._findGrid(n);
                    n = null
                }
            })
        }
        if (typeof(window.dhtmlXForm) == "function" && h instanceof window.dhtmlXForm) {
            h.forEachItem(function(m) {
                if (l != true && h.getItemType(m) == "container" && a.entBox == h.getContainer(m)) {
                    l = true
                }
            })
        }
        h = null;
        return l
    };
    this._idExists = function(l) {
        var a = false;
        for (var h = 0; h < this.conf.id.length; h++) {
            if (this.conf.id[h] instanceof Array) {
                a = a || (this.conf.id[h][0] == l[0] && this.conf.id[h][1] == l[1])
            } else {
                a = a || this.conf.id[h] == l
            }
        }
        return a
    };
    this._IEDisp = (window.dhx4.isIE6 || document.compatMode != "CSS1Compat");
    this._IEHover = (window.dhx4.isIE6 == true);
    if (this._IEHover) {
        this._IEHoverInit = function() {
            this.p.onmouseover = function() {
                var a = event.srcElement;
                while (a != this && a._IEHover != true) {
                    a = a.parentNode
                }
                if (a._IEHover) {
                    if (g._IEHoverTM) {
                        window.clearTimeout(g._IEHoverTM)
                    }
                    if (g._lastIEHover == a) {
                        return
                    }
                    g._IEHoverRender(a);
                    a = null
                }
            };
            this.p.onmouseout = function() {
                if (g._IEHoverTM) {
                    window.clearTimeout(g._IEHoverTM)
                }
                g._IEHoverTM = window.setTimeout(function() {
                    g._IEHoverRender(null)
                }, 1)
            };
            this._IEHoverRender = function(a) {
                if (this._lastIEHover != null) {
                    if (this._lastIEHover.className.search(/tr_hover/gi) >= 0) {
                        this._lastIEHover.className = this._lastIEHover.className.replace(/\s{0,}tr_hover/gi, "");
                        this._lastIEHover = null
                    }
                }
                if (a != null && a.className.search(/tr_hover/gi) < 0) {
                    a.className += " tr_hover";
                    g._lastIEHover = a
                }
            };
            this._IEHoverInited = true
        };
        this._IEHoverClear = function() {
            this.p.onmouseover = null;
            this.p.onmouseout = null;
            this._IEHoverInited = false
        }
    }
    this._IEFirstLast = (window.dhx4.isIE6 || window.dhx4.isIE7 || window.dhx4.isIE8);
    this._enableIEVFix = function() {
        if (window.dhx4.isIE6 || window.dhx4.isIE7) {
            var h = this.attachEvent("onHide", function() {
                document.getElementById(this._nodeId).style.visibility = "hidden"
            });
            var a = this.attachEvent("onShow", function() {
                document.getElementById(this._nodeId).style.visibility = "visible"
            });
            if (this._nodeObjEv == null) {
                this._nodeObjEv = []
            }
            this._nodeObjEv.push(h, a)
        }
    };
    if (typeof(window.dhtmlXToolbarObject) == "function" && this.conf.toolbar != null && this.conf.toolbar instanceof window.dhtmlXToolbarObject && this.conf.id != null) {
        if (!(this.conf.id instanceof Array)) {
            this.conf.id = [this.conf.id]
        }
        this.skinParent = this.conf.toolbar.conf.skin;
        this._doOnToolbarClick = function(h) {
            for (var a = 0; a < g.conf.id.length; a++) {
                if (h == g.conf.id[a]) {
                    if (h != g._lastId) {
                        g.show(h);
                        g._clearClick = true
                    }
                }
            }
        };
        if (typeof(dhtmlXToolbarObject.prototype._getItemDim) == "undefined") {
            dhtmlXToolbarObject.prototype._getItemDim = function(l) {
                var a = this.objPull[this.idPrefix + l];
                var h = {
                    left: window.dhx4.absLeft(a.obj),
                    top: window.dhx4.absTop(a.obj),
                    width: a.obj.offsetWidth + (a.arw ? a.arw.offsetWidth : 0),
                    height: a.obj.offsetHeight
                };
                a = null;
                return h
            }
        }
        this.conf.toolbarEvent = this.conf.toolbar.attachEvent("onClick", this._doOnToolbarClick)
    }
    if (typeof(window.dhtmlXRibbon) == "function" && this.conf.ribbon != null && this.conf.ribbon instanceof window.dhtmlXRibbon && this.conf.id != null) {
        if (!(this.conf.id instanceof Array)) {
            this.conf.id = [this.conf.id]
        }
        this.skinParent = this.conf.ribbon.conf.skin;
        this._doOnRibbonClick = function(h) {
            for (var a = 0; a < g.conf.id.length; a++) {
                if (h == g.conf.id[a]) {
                    if (h != g._lastId) {
                        g.show(h);
                        g._clearClick = true
                    }
                }
            }
        };
        if (typeof(dhtmlXRibbon.prototype._getItemDim) == "undefined") {
            dhtmlXRibbon.prototype._getItemDim = function(l) {
                var a = this._items[l].base;
                var h = {
                    left: window.dhx4.absLeft(a),
                    top: window.dhx4.absTop(a),
                    width: a.offsetWidth,
                    height: a.offsetHeight
                };
                a = null;
                return h
            }
        }
        this.conf.ribbonEvent = this.conf.ribbon.attachEvent("_showPopup", this._doOnRibbonClick)
    }
    if (typeof(window.dhtmlXForm) == "function" && this.conf.form != null && this.conf.form instanceof window.dhtmlXForm && this.conf.id != null) {
        if (!(this.conf.id instanceof Array)) {
            this.conf.id = [this.conf.id]
        }
        if (!this.conf.mode) {
            this.mode = "right"
        }
        this.skinParent = this.conf.form.skin;
        if (typeof(dhtmlXForm.prototype._getItemDim) == "undefined") {
            dhtmlXForm.prototype._getItemDim = function(a, h) {
                return this.doWithItem(a, "_getDim")
            };
            for (var c in dhtmlXForm.prototype.items) {
                dhtmlXForm.prototype.items[c]._getDim = function(h) {
                    var a = h;
                    if ({
                            ta: true,
                            pw: true,
                            se: true,
                            tp: true,
                            fl: true,
                            calendar: true,
                            colorpicker: 1,
                            editor: true,
                            container: true
                        }[h._type]) {
                        a = h.childNodes[h._ll ? 1 : 0].childNodes[0]
                    }
                    if ({
                            ch: true,
                            ra: true,
                            btn2state: true
                        }[h._type]) {
                        a = h.childNodes[h._ll ? 1 : 0].childNodes[1]
                    }
                    if ({
                            bt: true,
                            lb: true,
                            fs: true
                        }[h._type]) {
                        a = h.firstChild
                    }
                    if ({
                            combo: true
                        }[h._type]) {
                        a = h._combo.DOMParent.firstChild
                    }
                    if ({
                            image: true
                        }[h._type]) {
                        a = h.childNodes[h._ll ? 1 : 0].lastChild
                    }
                    if ({
                            bl: true
                        }[h._type]) {
                        a = h.firstChild.firstChild.firstChild
                    }
                    var l = {
                        left: window.dhx4.absLeft(a),
                        top: window.dhx4.absTop(a),
                        width: a.offsetWidth,
                        height: a.offsetHeight
                    };
                    a = null;
                    return l
                }
            }
        }
    }
    if (typeof(window.dhtmlXSlider) == "function" && this.conf.slider != null && this.conf.slider instanceof window.dhtmlXSlider) {
        if (!this.conf.mode) {
            this.mode = "top"
        }
        this.conf.slider_tm = null;
        this._sliderShow = function() {
            if (g.conf.slider_tm) {
                window.clearTimeout(g.conf.slider_tm)
            }
            var a = g.conf.slider._r_actv;
            if (a != null) {
                var h = {
                    left: window.dhx4.absLeft(a),
                    top: window.dhx4.absTop(a),
                    width: a.offsetWidth,
                    height: a.offsetHeight
                };
                g.show(h.left, h.top, h.width, h.height);
                a = null
            }
        };
        this._sliderHide = function() {
            g.conf.slider_tm = window.setTimeout(function() {
                g.hide()
            }, 200)
        };
        this.conf.slider_events = [this.conf.slider.attachEvent("onMouseDown", this._sliderShow), this.conf.slider.attachEvent("onMouseUp", this._sliderHide), this.conf.slider.attachEvent("onChange", this._sliderShow)]
    }
    this.setSkin(this.skinParent || this.conf.skin || window.dhx4.skin || (typeof(dhtmlx) != "undefined" ? dhtmlx.skin : null) || window.dhx4.skinDetect("dhx_popup") || "material");
    return this
    }


dhtmlXPopup.prototype.attachObject = function(a) {
    return this._attachNode("object", {
        obj: a
    })
};
dhtmlXPopup.prototype._attach_init_object = function(a) {
    this._nodeObj = (typeof(a.obj) == "string" ? document.getElementById(a.obj) : a.obj);
    a.obj = null;
    document.getElementById(this._nodeId).appendChild(this._nodeObj);
    this._nodeObj.style.display = "";
    this._nodeObj.style.visibility = "visible"
};
dhtmlXPopup.prototype.attachHTML = function(a) {
    return this._attachNode("html", {
        html: a
    })
};
dhtmlXPopup.prototype._attach_init_html = function(a) {
    document.getElementById(this._nodeId).innerHTML = a.html;
    this._nodeObj = {
        text: a.html
    }
};
dhtmlXPopup.prototype.attachForm = function(c, a) {
    return this._attachNode("form", {
        struct: c,
        width: a
    })
};
dhtmlXPopup.prototype._attach_init_form = function(c) {
    var a = this;
    this._nodeObj = new dhtmlXForm(this._nodeId, c.struct);
    this._nodeObj.setSkin(this.conf.skin);
    this._nodeObj.attachEvent("_onBeforeEditorAccess", function() {
        a._clearClick = true
    });
    if (this.conf.editorEv != null && this.checkEvent(this.conf.editorEv) == false) {
        this.conf.editorEv = null
    }
    if (dhtmlXForm.prototype.items.editor != null && this.conf.editorEv == null) {
        this.conf.editorEv = this.attachEvent("onShow", function() {
            var h = dhtmlXForm.prototype.items.editor.editor;
            for (var e in h) {
                var l = h[e].base;
                var g = false;
                while (l != null) {
                    if (l == this.p) {
                        g = true;
                        l = null
                    } else {
                        l = l.parentNode
                    }
                }
                l = null;
                if (g == true) {
                    h[e].cell.conf.cells_cont = null;
                    h[e].setSizes()
                }
            }
            h = null;
            this.detachEvent(this.conf.editorEv);
            this.conf.editorEv = null
        });
        if (this._nodeObjEv == null) {
            this._nodeObjEv = []
        }
        this._nodeObjEv.push(this.conf.editorEv)
    }
};
dhtmlXPopup.prototype.attachCalendar = function(a) {
    return this._attachNode("calendar", a || {})
};
dhtmlXPopup.prototype._attach_init_calendar = function(a) {
    if (a["double"] == true) {
        this._nodeObj = new dhtmlXDoubleCalendarObject(this._nodeId);
        this._nodeObj.leftCalendar.setSkin(this.conf.skin);
        this._nodeObj.rightCalendar.setSkin(this.conf.skin)
    } else {
        this._nodeObj = new dhtmlXCalendarObject(this._nodeId);
        this._nodeObj.setSkin(this.conf.skin)
    }
    this._nodeObj.show()
};
dhtmlXPopup.prototype.attachGrid = function(c, a) {
    return this._attachNode("grid", {
        width: c || 400,
        height: a || 200
    })
};
dhtmlXPopup.prototype._attach_init_grid = function() {
    this._nodeObj = new dhtmlXGridObject(this._nodeId);
    this._nodeObj.setSkin(this.conf.skin)
};
dhtmlXPopup.prototype.attachTree = function(e, c, a) {
    return this._attachNode("tree", {
        width: e || 400,
        height: c || 200,
        rootId: a || 0
    })
};
dhtmlXPopup.prototype._attach_init_tree = function(a) {
    this._nodeObj = new dhtmlXTreeObject(this._nodeId, "100%", "100%", (a.rootId))
};
dhtmlXPopup.prototype.attachLayout = function(c, a, e) {
    return this._attachNode("layout", {
        width: c || 400,
        height: a || 200,
        pattern: e || "3L"
    })
};
dhtmlXPopup.prototype._attach_init_layout = function(a) {
    this._nodeObj = new dhtmlXLayoutObject(this._nodeId, a.pattern, this.conf.skin)
};
dhtmlXPopup.prototype.attachAccordion = function(e, a, c) {
    return this._attachNode("accordion", {
        width: e || 400,
        height: a || 200,
        conf: c || {}
    })
};
dhtmlXPopup.prototype._attach_init_accordion = function(a) {
    a.conf.parent = this._nodeId;
    a.conf.skin = this.conf.skin;
    this._nodeObj = new dhtmlXAccordion(a.conf)
};
dhtmlXPopup.prototype.attachTabbar = function(e, a, c) {
    if (typeof(c) == "string") {
        c = {
            mode: c
        }
    } else {
        if (typeof(c) != "object" || c == null) {
            c = {}
        }
    }
    return this._attachNode("tabbar", {
        width: e || 400,
        height: a || 200,
        conf: c
    })
};
dhtmlXPopup.prototype._attach_init_tabbar = function(a) {
    a.conf.parent = this._nodeId;
    a.conf.skin = this.conf.skin;
    this._nodeObj = new dhtmlXTabBar(a.conf)
};
dhtmlXPopup.prototype.attachSidebar = function(e, a, c) {
    if (c == null) {
        c = {}
    }
    return this._attachNode("sidebar", {
        width: e || 400,
        height: a || 200,
        conf: c
    })
};
dhtmlXPopup.prototype._attach_init_sidebar = function(a) {
    a.conf.parent = this._nodeId;
    a.conf.skin = this.conf.skin;
    this._nodeObj = new dhtmlXSideBar(a.conf)
};
dhtmlXPopup.prototype.attachEditor = function(e, a, c) {
    return this._attachNode("editor", {
        width: e || 400,
        height: a || 200,
        conf: c || {}
    })
};
dhtmlXPopup.prototype._attach_init_editor = function(c) {
    document.getElementById(this._nodeId).className = "dhxeditor_" + this.conf.skin;
    c.conf.parent = this._nodeId;
    if (c.conf.skin == null) {
        c.conf.skin = this.conf.skin
    }
    this._nodeObj = new dhtmlXEditor(c.conf);
    var a = this.attachEvent("onShow", function() {
        if (this._nodeObj instanceof window.dhtmlXEditor) {
            this._nodeObj.setSizes()
        }
    });
    if (this._nodeObjEv == null) {
        this._nodeObjEv = []
    }
    this._nodeObjEv.push(a)
};
dhtmlXPopup.prototype.attachColorPicker = function(a) {
    if (typeof(a) != "object" || a == null) {
        a = {}
    }
    return this._attachNode("colorpicker", {
        conf: a
    })
};
dhtmlXPopup.prototype._attach_init_colorpicker = function(a) {
    a.conf.skin = this.conf.skin;
    a.conf.parent = this._nodeId;
    this._nodeObj = new SepoaDhtmlXColorPicker(a.conf, {lang: Sepoa.info.LANGUAGE.toLowerCase()})
};
dhtmlXPopup.prototype.attachCarousel = function(e, a, c) {
    if (c == null) {
        c = {}
    }
    return this._attachNode("carousel", {
        width: e || 400,
        height: a || 300,
        conf: c
    })
};
dhtmlXPopup.prototype._attach_init_carousel = function(a) {
    a.conf.parent = this._nodeId;
    a.conf.skin = this.conf.skin;
    this._nodeObj = new dhtmlXCarousel(a.conf)
};
dhtmlXPopup.prototype.attachTreeView = function(e, a, c) {
    if (c == null) {
        c = {}
    }
    return this._attachNode("treeview", {
        width: e || 400,
        height: a || 300,
        conf: c
    })
};
dhtmlXPopup.prototype._attach_init_treeview = function(a) {
    a.conf.parent = this._nodeId;
    a.conf.skin = this.conf.skin;
    this._nodeObj = new dhtmlXTreeView(a.conf);
    this._nodeObj.base.className += " dhxtreeview_with_border";
    var c = this.attachEvent("onShow", function() {
        if (this._nodeObj instanceof window.dhtmlXTreeView) {
            this._nodeObj.setSizes()
        }
    });
    if (this._nodeObjEv == null) {
        this._nodeObjEv = []
    }
    this._nodeObjEv.push(c)
};
dhtmlXPopup.prototype.setDimension = function(a, c) {
    if (this._nodeId == null || (a == null && c == null)) {
        return
    }
    var e = document.getElementById(this._nodeId);
    if (a != null) {
        e.style.width = a + "px"
    }
    if (c != null) {
        e.style.height = c + "px"
    }
    this._repaint();
    if (this._nodeObj != null && typeof(this._nodeObj.setSizes) == "function") {
        this._nodeObj.setSizes()
    }
    e = null
};


