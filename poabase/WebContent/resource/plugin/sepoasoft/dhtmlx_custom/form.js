dhtmlXForm.prototype.properties={
	xbtn:false
}
dhtmlXForm.prototype.getFormData = function(v, l) {
    var c = {};
    var o = this;
    for (var u in this.itemPull) {
        var n = this.itemPull[u]._idd;
        var w = this.itemPull[u]._type;
        if (w == "ch") {
            c[n] = (this.isItemChecked(n) ? this.getItemValue(n) : 0)
        }else if (w == "ra" && !c[this.itemPull[u]._group]) {
            c[this.itemPull[u]._group] = this.getCheckedValue(this.itemPull[u]._group)
        }else if (this["getFormData_" + w]) {
            c[n] = this["getFormData_" + w](n)
        }else if (w == "up") {
            var h = this.getItemValue(n);
            for (var g in h) {
                c[g] = h[g]
            }
        }else if (this.itemPull[u]._list) {
            for (var e = 0; e < this.itemPull[u]._list.length; e++) {
                var m = this.itemPull[u]._list[e].getFormData(v, l);
                for (var s in m) {
                    c[s] = m[s]
                }
            }
        }else {
        	try{
        		c[n] = this.getItemValue(n, v)
        	}catch(o){
        	}
        }
        
        
    }
    if (!l && this.hId && this._userdata[this.hId]) {
        for (var u in this._userdata[this.hId]) {
            if (!c[u]) {
                c[u] = this._userdata[this.hId][u]
            }
        }
    }
    return c
};
dhtmlXForm.prototype._initObj = function(r, n) {
    if (typeof(r.data) != "undefined") {
        var s = null;
        if (typeof(n) != "undefined") {
            s = n.match(/(\?|\&)id\=([a-z0-9_\-]*)/i);
            if (s != null && s[0] != null) {
                s = s[0].split("=")[1]
            }
        }
        if (this.callEvent("onBeforeDataLoad", [s, window.dhx4._copyObj(r.data)]) === true) {
            this.formId = s;
            this._last_load_data = r;
            this.setFormData(r.data);
            this.resetDataProcessor("updated")
        }
        return
    }
    this._prepare();
    this.idef.position = Sepoa.config.get("sepoa.form.position") || "label-top";
    for (var o = 0; o < r.length; o++) {
        if (typeof(r[o]) != "undefined" && r[o].type == "settings") {
            for (var m in r[o]) {
                this.idef[m] = r[o][m]
            }
        }
    }
    for (var o = 0; o < r.length; o++) {
        this._prepareItem(r[o])
    }
    this._autoCheck()
};
dhtmlXForm.prototype._addItem = function(E, r, x, u, C, F, A) {
	var e = this;
    if (this.items[E]._index) {
        this.getForm()._indexId.push(r);
        this.getForm()._idIndex[r] = {
            ind: this.getForm()._indexId.length - 1
        }
    }
    if(x.label && (x.label.indexOf("#NO_LABEL#") >= 0)){
    	x.label = "";
    }
    if (!E) {
        E = this._type
    }
    if (E == "list" && C != null && this.itemPull[this.idPrefix + C] != null && typeof(this.itemPull[this.idPrefix + C]._addSubListNode) == "function") {
        var D = this.itemPull[this.idPrefix + C]._addSubListNode()
    } else {
        if (E == "newcolumn") {
            var D = {}
        } else {
            if (this.b_index == null) {
                this._prepare()
            }
            var s = this.base[this.b_index];
            var m = null;
            if (typeof(F) != "undefined" && !isNaN(F) && E != "list") {
                F = Math.max(parseInt(F), 0) + 1;
                for (var I = 0; I < this.cont.childNodes.length; I++) {
                    for (var n = 0; n < this.cont.childNodes[I].childNodes.length; n++) {
                        if (m == null && this.cont.childNodes[I].childNodes[n]._isNestedForm != true) {
                            F--;
                            if (F == 0) {
                                m = this.cont.childNodes[I].childNodes[n];
                                s = this.cont.childNodes[I]
                            }
                        }
                    }
                }
            } else {
                if (E == "list") {
                    for (var H in this.itemPull) {
                        if (H == this.idPrefix + r) {
                            m = this.itemPull[H].nextSibling;
                            if (this.itemPull[H]._listBase != null && this.itemPull[H]._listBase.length > 0) {
                                m = this.itemPull[H]._listBase[this.itemPull[H]._listBase.length - 1]
                            }
                        }
                    }
                }
            }
            var D = document.createElement("DIV");
            if (A == true && m != null) {
                m = m.nextSibling
            }
            if (m != null) {
                s.insertBefore(D, m)
            } else {
                s.appendChild(D)
            }
        }
    }
    D._idd = r;
    D._rId = this._rId;
    D.className = "";
    if (typeof(D.style) != "undefined") {
        if (typeof(x.offsetLeft) == "undefined" && this.idef.offsetLeft > 0) {
            x.offsetLeft = this.idef.offsetLeft
        }
        if (typeof(x.offsetTop) == "undefined" && this.idef.offsetTop > 0) {
            x.offsetTop = this.idef.offsetTop
        }
        var v = "";
        if (typeof(x.offsetLeft) != "undefined") {
            v += " padding-left:" + x.offsetLeft + "px!important;"
            D.className += " sepoa-form-padding-left-available";
        }
        if (typeof(x.offsetTop) != "undefined") {
            v += " padding-top:" + x.offsetTop + "px!important;"
            D.className += " sepoa-form-padding-top-available";
        }
        D.style.cssText += v
    }
    if (E == "block") {
        if (isNaN(x.blockOffset)) {
            x.blockOffset = this.idef.blockOffset
        }
    }
    if (E == "list") {
        if (typeof(D._ofsNested) == "undefined") {
            D._ofsNested = this.idef_const.offsetNested
        }
        if (u != null) {
            D._sId = u
        }
        var y = this.items[E].render(D, this.skin);
        if (!this.itemPull[this.idPrefix + r]._listObj) {
            this.itemPull[this.idPrefix + r]._listObj = []
        }
        if (!this.itemPull[this.idPrefix + r]._list) {
            this.itemPull[this.idPrefix + r]._list = []
        }
        if (!this.itemPull[this.idPrefix + r]._listBase) {
            this.itemPull[this.idPrefix + r]._listBase = []
        }(this.itemPull[this.idPrefix + r]._listObj).push(y[0]);
        (this.itemPull[this.idPrefix + r]._list).push(y[1]);
        (this.itemPull[this.idPrefix + r]._listBase).push(D);
        y[1].checkEvent = function(a) {
            return e.checkEvent(a)
        };
        y[1].callEvent = function(a, w) {
            return e.callEvent(a, w)
        };
        y[1].getForm = function() {
            return e.getForm()
        };
        y[1]._initObj(this._mergeSettings(x));
        if (D._inBlcok) {
            D.className += " in_block"
        }
        return y[1]
    }
    if (E == "newcolumn") {
        this._prepare(x.offset, F);
        return
    }
    if (E == "label" && this._ic++ == 0) {
        x._isTopmost = true
    }
    x.position = this.apos_css[(!x.position || !this.apos_css[x.position] ? this.idef.position : x.position)];
    D.className += " "+ (x.position + (typeof(x.className) == "string" ? " " + x.className : ""));
    D.className += " dhxform_item";

    if(x.design) {
    	D.className += " sepoa-dhxform-custom-item-design-" + x.design;    	
    }
    
    if(x.inputAlign) {
    	D.className += " sepoa-dhxform-custom-item-align-" + x.inputAlign;
    }
    
    if(x.sepoaStyle) {
    	D.className += " sepoa-dhxform-custom-item-style-" + x.sepoaStyle;
    }
    
    if (!x.labelWidth) {
        x.labelWidth = this.idef.labelWidth
    }
    if (!x.labelHeight) {
        x.labelHeight = this.idef.labelHeight
    }
    if (typeof(x.wrap) != "undefined") {
        x.wrap = window.dhx4.s2b(x.wrap)
    }
    x.labelAlign = (this.align_css[x.labelAlign] ? this.align_css[x.labelAlign] : this.align_css[this.idef.labelAlign]);
    x.inputWidth = (x.width ? x.width : (x.inputWidth ? x.inputWidth : this.idef.inputWidth));
    if (!x.inputHeight) {
        x.inputHeight = this.idef.inputHeight
    }
    if (typeof(x.note) != "undefined") {
        if (x.note.length != null && x.note[0] != null) {
            x.note = x.note[0]
        }
        if (typeof(x.note.width) == "undefined") {
            x.note.width = this.idef.noteWidth
        }
        if (x.note.width == "auto") {
            x.note.width = x.inputWidth
        }
    }
    D.checkEvent = function(a) {
        return e.checkEvent(a)
    };
    D.callEvent = function(a, w) {
        return e.callEvent(a, w)
    };
    D.getForm = function() {
        return e.getForm()
    };
    D._autoCheck = function(a) {
        e._autoCheck(a)
    };
    if (typeof(x.readonly) == "string") {
        x.readonly = window.dhx4.s2b(x.readonly)
    }
    if (typeof(x.autoStart) == "string") {
        x.autoStart = window.dhx4.s2b(x.autoStart)
    }
    if (typeof(x.autoRemove) == "string") {
        x.autoRemove = window.dhx4.s2b(x.autoRemove)
    }
    if (typeof(x.titleScreen) == "string") {
        x.titleScreen = window.dhx4.s2b(x.titleScreen)
    }
    if (typeof(x.info) == "string") {
        x.info = window.dhx4.s2b(x.info)
    }
    if (typeof(x.hidden) == "string") {
        x.hidden = window.dhx4.s2b(x.hidden)
    }
    if (typeof(x.checked) == "string") {
        x.checked = window.dhx4.s2b(x.checked)
    }
    if (typeof(x.userdata) != "undefined") {
        for (var H in x.userdata) {
            this.getForm().setUserData(r, H, x.userdata[H])
        }
    }
    if (x.validate) {
        if (typeof(x.validate != "undefined") && (typeof(x.validate) == "function" || typeof(window[x.validate]) == "function")) {
            D._validate = [x.validate]
        } else {
            D._validate = String(x.validate).split(this.separator)
        }
    }
    if (typeof(x.required) != "undefined") {
        if (typeof(x.required) == "string") {
            x.required = window.dhx4.s2b(x.required)
        }
        D._required = (x.required == true)
    }
    if (D._required) {
        if (!D._validate) {
            D._validate = []
        }
        var o = false;
        for (n = 0; n < D._validate.length; n++) {
            o = (o || (D._validate[n] == "NotEmpty"))
        }
        if (!o) {
            D._validate.push("NotEmpty")
        }
    }
    D.className = D.className +  (D._required?" dhxform_custom_required":"");
    D._ll = (x.position == this.apos_css["label-left"] || x.position == this.apos_css["label-top"]);
    this.objPull[this.idPrefix + r] = this.items[E].render(D, x, this);
    this.itemPull[this.idPrefix + r] = D;
};
dhtmlXForm.prototype.items.list.render= function(a, c) {
    a._type = "list";
    a._enabled = true;
    a._isNestedForm = true;
    a.style.paddingLeft = a._ofsNested + "px";
    a.className = "dhxform_base_nested" + (a._custom_css || "");
    var f = new dhtmlXForm(a, null, c);
    f.clear = function() {
        var m = {};
        this.formId = (new Date()).valueOf();
        this.resetDataProcessor("inserted");
        for (var n in this.itemPull) {
        	var o = this.itemPull[n]._idd;
        	if (this.itemPull[n]._type in {
                ta: 1,
                editor: 1,
                calendar: 1,
                pw: 1,
                hd: 1,
                vault: 1,
                colorpicker: 1
            }) {
	            this.setItemValue(o, "")
	        }else if (this.itemPull[n]._type == "ch") {
	            this.uncheckItem(o)
	        }else if (this.itemPull[n]._type == "combo") {
	            this.itemPull[n]._apiChange = true;
	            var v = this.getCombo(o);
	            v.selectOption(0);
	            v = null;
	            this.itemPull[n]._apiChange = false
	        }else if (this.itemPull[n]._type == "autocomplete") {
	            this.itemPull[n]._apiChange = true;
	            var v = this.getCombo(o);
	            v.clearAll();
	            v.setComboText("");
	            this.itemPull[n]._apiChange = false
	        }else if (this.itemPull[n]._type == "se") {
	            var s = this.getOptions(o);
	            if (s.length > 0) {
	                s[0].selected = true
	            }
	        }else if (this.itemPull[n]._type == "ra") {
	            var r = this.itemPull[n]._group;
	            if (!m[r]) {
	                this.checkItem(r, this.doWithItem(o, "_getFirstValue"));
	                m[r] = true
	            }
	        }else if (this.itemPull[n]._list) {
	            for (var u = 0; u < this.itemPull[n]._list.length; u++) {
	                this.itemPull[n]._list[u].clear()
	            }
	        }else if (this["setFormData_" + this.itemPull[n]._type]) {
	            this["setFormData_" + this.itemPull[n]._type](o, "")
	        }
        }
        m = null;
        if (this._parentForm) {
            this._autoCheck()
        }
        this.resetValidateCss()
    };
    return [this, f]
}
dhtmlXForm.prototype.items.checkbox = {
    render: function(a, c) {
        a._type = "ch";
        a._enabled = true;
        a._checked = false;
        a._value = (typeof(c.value) == "undefined" ? null : String(c.value));
        a._ro = (c.readonly == true);
        if (c._autoInputWidth !== false) {
            c.inputWidth = 14
        }
        this.doAddLabel(a, c);
        this.doAddInput(a, c, "INPUT", "TEXT", true, true, "dhxform_textarea");
        a.childNodes[a._ll ? 1 : 0].className += " dhxform_img_node";
        var e = document.createElement("DIV");
        e.className = "dhxform_img chbx0";
        a.appendChild(e);
        if (!isNaN(c.inputLeft)) {
            a.childNodes[a._ll ? 1 : 0].style.left = parseInt(c.inputLeft) + "px"
        }
        if (!isNaN(c.inputTop)) {
            a.childNodes[a._ll ? 1 : 0].style.top = parseInt(c.inputTop) + "px"
        }
        a.childNodes[a._ll ? 1 : 0].appendChild(e);
        a.childNodes[a._ll ? 1 : 0].firstChild.value = String(c.value);
        a._updateImgNode = function(h, l) {
            var g = h.childNodes[h._ll ? 1 : 0].lastChild;
            g.className = (l ? "dhxform_actv_c" : "dhxform_img") + " " + (h._checked ? "chbx1" : "chbx0");
            h = g = null
        };
        a._doOnFocus = function(g) {
            g.getForm().callEvent("onFocus", [g._idd])
        };
        a._doOnBlur = function(g) {
            g.getForm().callEvent("onBlur", [g._idd])
        };
        a._doOnKeyUpDown = function(h, g) {
            this.callEvent(h, [this.childNodes[this._ll ? 0 : 1].childNodes[0], g, this._idd])
        };
        if (c.checked == true) {
            this.check(a)
        }
        if (c.hidden == true) {
            this.hide(a)
        }
        if (c.disabled == true) {
            this.userDisable(a)
        }
        this.doAttachEvents(a);
        return this
    },
    destruct: function(a) {
        a._doOnFocus = a._doOnBlur = a._updateImgNode = null;
        this.doUnloadNestedLists(a);
        this.doDestruct(a)
    },
    doAddLabel: function(c, e) {
        var a = document.createElement("DIV");
        a.className = "dhxform_label " + e.labelAlign;
        if (e.wrap == true) {
            a.style.whiteSpace = "normal"
        }
        if (c._ll) {
            c.insertBefore(a, c.firstChild)
        } else {
            c.appendChild(a)
        }
        if (typeof(e.tooltip) != "undefined") {
            a.title = e.tooltip
        }
        a.innerHTML = "<div class='dhxform_label_nav_link' onfocus='if(this.parentNode.parentNode._updateImgNode)this.parentNode.parentNode._updateImgNode(this.parentNode.parentNode,true);this.parentNode.parentNode._doOnFocus(this.parentNode.parentNode);' onblur='if(this.parentNode.parentNode._updateImgNode)this.parentNode.parentNode._updateImgNode(this.parentNode.parentNode,false);this.parentNode.parentNode._doOnBlur(this.parentNode.parentNode);' onkeypress='var e=event||window.arguments[0];if(e.keyCode==32||e.charCode==32){e.cancelBubble=true;if(e.preventDefault)e.preventDefault();else e.returnValue=false;_dhxForm_doClick(this,\"mousedown\");return false;}' onkeyup='var e=event||window.arguments[0];this.parentNode.parentNode._doOnKeyUpDown(\"onKeyUp\",e);' onkeydown='var e=event||window.arguments[0];this.parentNode.parentNode._doOnKeyUpDown(\"onKeyDown\",e);' " + (window.dhx4.isIPad ? "ontouchstart='var e=event;e.preventDefault();_dhxForm_doClick(this,\"mousedown\");' " : "") + "role='link' tabindex='0'>" + (c._required ? "<span class='dhxform_item_required'>*</span>" : "") + e.label + (e.info ? "<span class='dhxform_info'>[?]</span>" : "") + "</div>";
        
        if(e.label == null || e.label == undefined || e.label == ""){
        	c.classList.add("dhxform_label_hideen");
        }else if(e.label == "#BLANK#"){
        	c.classList.add("dhxform_label_blank");
        }
        if(e.position == "dhxform_item_label_top"){
        	a.style.width = e.inputWidth + "px"
        }else if (!isNaN(e.labelWidth)) {
        	a.style.width = e.labelWidth + "px"
        }
        
        if (!isNaN(e.labelHeight)) {
            a.firstChild.style.height = parseInt(e.labelHeight) + "px"
        }
        if (!isNaN(e.labelLeft)) {
            a.style.left = parseInt(e.labelLeft) + "px"
        }
        if (!isNaN(e.labelTop)) {
            a.style.top = parseInt(e.labelTop) + "px"
        }
    },
    doAddInput: function(w, g, c, o, r, h, l) {
        var a = document.createElement("DIV");
        a.className = "dhxform_control";
        a.classList.add("xi-sepoa-dummy-after")
        if (w._ll) {
            w.appendChild(a)
        } else {
            w.insertBefore(a, w.firstChild)
        }
        var v = document.createElement(c);
        v.className = l;
        v.name = w._idd;
        v._idd = w._idd;
        v.id = g.uid;
        v.autocomplete="off"
        if (typeof(o) == "string") {
            v.type = o
        }
        if (c == "INPUT" || c == "TEXTAREA") {
            v.onkeyup = function(u) {
                u = u || event;
                w.callEvent("onKeyUp", [this, u, this._idd])
            };
            v.onkeydown = function(u) {
                u = u || event;
                w.callEvent("onKeyDown", [this, u, this._idd])
            }
        }
        if (c == "SELECT" && g.type == "select" && w.getForm().skin == "material") {
            if (window.dhx4.isOpera || window.dhx4.isChrome) {
                v.className += " dhxform_arrow_fix_webkit"
            } else {
                if (window.dhx4.isEdge) {
                    v.className += " dhxform_arrow_fix_edge"
                } else {
                    if (window.dhx4.isFF) {
                        v.className += " dhxform_fix_ff"
                    }
                }
            }
        }
        a.appendChild(v);
        if (r) {
            if (!isNaN(g.inputLeft)) {
                a.style.left = parseInt(g.inputLeft) + "px"
            }
            if (!isNaN(g.inputTop)) {
                a.style.top = parseInt(g.inputTop) + "px"
            }
        }
        var s = "";
        var m = false;
        if (h) {
        	if (!isNaN(g.inputWidth)) {
                s += "width:" + parseInt(g.inputWidth) + "px;";
                m = true
            }else if(g.inputWidth == "*"){
            	a.classList.add("dhxform_control_width_full");
            }
            if (!isNaN(g.inputHeight)) {
                s += "height:" + parseInt(g.inputHeight) + "px;"
            }else if(g.inputHeight == "*"){
            	a.classList.add("dhxform_control_height_full");
            }
        }
        if (typeof(g.style) == "string") {
            s += g.style
        }
        v.style.cssText = s;
        if (g.maxLength) {
            v.setAttribute("maxLength", g.maxLength)
        }
        if (g.connector) {
            v.setAttribute("connector", g.connector)
        }
        var e = (dhtmlXForm.prototype.items[this.t] != null ? dhtmlXForm.prototype.items[this.t]._dimFix == true : false);
        if (m && ({
                input: 1,
                password: 1,
                select: 1,
                multiselect: 1,
                calendar: 1,
                colorpicker: 1
            }[this.t] == 1 || e)) {
            if (dhtmlXForm.prototype.items[this.t]._dim == null) {
                dhtmlXForm.prototype.items[this.t]._dim = w.getForm()._checkDim(a, v)
            }
            v.style.width = parseInt(v.style.width) - dhtmlXForm.prototype.items[this.t]._dim + "px"
        }
        if (typeof(g.note) == "object") {
            var n = document.createElement("DIV");
            n.className = "dhxform_note";
            n.style.width = (isNaN(g.note.width) ? v.offsetWidth : parseInt(g.note.width)) + "px";
            n._w = g.note.width;
            n.innerHTML = g.note.text;
            a.appendChild(n);
            a.classList.add("dhxform_note_included")
            n = null
        }
        if (g.readonly) {
            this.setReadonly(w, true)
        }
        if (g.disabled == true) {
            this.userDisable(w)
        }
        if (g.hidden == true && this.t != "combo") {
            this.hide(w)
        }
    },
    doUnloadNestedLists: function(a) {
        if (!a._list) {
            return
        }
        for (var c = 0; c < a._list.length; c++) {
            a._list[c].unload();
            a._list[c] = null;
            a._listObj[c] = null;
            a._listBase[c].parentNode.removeChild(a._listBase[c]);
            a._listBase[c] = null
        }
        a._list = null;
        a._listObj = null;
        a._listBase = null
    },
    doDestruct: function(a) {
        a.callEvent = null;
        a.checkEvent = null;
        a.getForm = null;
        a._autoCheck = null;
        a._checked = null;
        a._enabled = null;
        a._idd = null;
        a._type = null;
        a._value = null;
        a._group = null;
        a.onselectstart = null;
        a.childNodes[a._ll ? 1 : 0].onmousedown = null;
        a.childNodes[a._ll ? 1 : 0].ontouchstart = null;
        a.childNodes[a._ll ? 0 : 1].onmousedown = null;
        a.childNodes[a._ll ? 0 : 1].ontouchstart = null;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].onfocus = null;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].onblur = null;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].onkeypress = null;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].onkeyup = null;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].onkeydown = null;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].onmousedown = null;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].ontouchstart = null;
        a.childNodes[a._ll ? 0 : 1].removeChild(a.childNodes[a._ll ? 0 : 1].childNodes[0]);
        while (a.childNodes.length > 0) {
            a.removeChild(a.childNodes[0])
        }
        a.parentNode.removeChild(a);
        a = null
    },
    doAttachEvents: function(c) {
        var a = this;
        c.childNodes[c._ll ? 1 : 0][window.dhx4.isIPad ? "ontouchstart" : "onmousedown"] = function(h) {
            h = h || event;
            if (h.preventDefault) {
                h.preventDefault()
            }
            var g = (h.target || h.srcElement);
            if (!this.parentNode._enabled || this.parentNode._ro || (typeof(g.className) != "undefined" && g.className == "dhxform_note")) {
                h.cancelBubble = true;
                if (h.preventDefault) {
                    h.preventDefault()
                } else {
                    h.returnValue = false
                }
                return false
            }
            a.doClick(this.parentNode)
        };
        c.childNodes[c._ll ? 0 : 1].childNodes[0][window.dhx4.isIPad ? "ontouchstart" : "onmousedown"] = function(h) {
            h = h || event;
            if (h.preventDefault) {
                h.preventDefault()
            }
            if (!this.parentNode.parentNode._enabled) {
                h.cancelBubble = true;
                if (h.preventDefault) {
                    h.preventDefault()
                } else {
                    h.returnValue = false
                }
                return false
            }
            var g = h.target || h.srcElement;
            if (typeof(g.className) != "undefined" && g.className == "dhxform_info") {
                this.parentNode.parentNode.callEvent("onInfo", [this.parentNode.parentNode._idd, h]);
                h.cancelBubble = true;
                if (h.preventDefault) {
                    h.preventDefault()
                } else {
                    h.returnValue = false
                }
                return false
            }
            a.doClick(this.parentNode.parentNode)
        }
    },
    doClick: function(a) {
        a.childNodes[a._ll ? 0 : 1].childNodes[0].focus();
        if (!a._enabled || a._ro) {
            return
        }
        if (a.checkEvent("onBeforeChange")) {
            if (a.callEvent("onBeforeChange", [a._idd, a._value, a._checked]) !== true) {
                return
            }
        }
        this.setChecked(a, !a._checked);
        a._autoCheck();
        a.callEvent("onChange", [a._idd, a._value, a._checked])
    },
    doCheckValue: function(a) {
        if (a._checked && a._enabled) {
            a.childNodes[a._ll ? 1 : 0].firstChild.name = String(a._idd);
            a.childNodes[a._ll ? 1 : 0].firstChild.value = this.getValue(a);
        } else {
            a.childNodes[a._ll ? 1 : 0].firstChild.name = String(a._idd);
            a.childNodes[a._ll ? 1 : 0].firstChild.value = this.getValue(a);
        }
    },
    setChecked: function(a, c) {
        a._checked = (c === true ? true : false);
        a.childNodes[a._ll ? 1 : 0].lastChild.className = a.childNodes[a._ll ? 1 : 0].lastChild.className.replace(/chbx[0-1]{1}/gi, "") + (a._checked ? " chbx1" : " chbx0");
        this.doCheckValue(a)
    },
    check: function(a) {
        this.setChecked(a, true)
    },
    unCheck: function(a) {
        this.setChecked(a, false)
    },
    isChecked: function(a) {
        return a._checked
    },
    enable: function(a) {
        if (String(a.className).search("disabled") >= 0) {
            a.className = String(a.className).replace(/disabled/gi, "")
        }
        a._enabled = true;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].tabIndex = 0;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].removeAttribute("disabled");
        this.doCheckValue(a)
    },
    disable: function(a) {
        if (String(a.className).search("disabled") < 0) {
            a.className += " disabled"
        }
        a._enabled = false;
        if (a._updateImgNode != null) {
            a._updateImgNode(a, false)
        }
        a.childNodes[a._ll ? 0 : 1].childNodes[0].tabIndex = -1;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].setAttribute("disabled", "true");
        this.doCheckValue(a)
    },
    isEnabled: function(a) {
        return a._enabled
    },
    setText: function(a, c) {
        //a.childNodes[a._ll ? 0 : 1].childNodes[0].innerHTML = (a._required ? "<span class='dhxform_item_required'>*</span>" : "") + c
        const temp = document.createElement('div');
        temp.appendChild(document.createTextNode(c));
        const label = temp.innerHTML;
        a.childNodes[a._ll ? 0 : 1].childNodes[0].innerHTML = (a._required ? "<span class='dhxform_item_required'>*</span>" : "") + label;
        var b = a.childNodes[a._ll ? 0 : 1].childNodes[0];
        if((b.parentElement.offsetWidth > 0) && b.parentElement.offsetWidth < b.offsetWidth){
        	b.parentElement.style.width=parseInt(b.offsetWidth)+'px'
        }
    },
    getText: function(a) {
        return a.childNodes[a._ll ? 0 : 1].childNodes[0].innerHTML.replace(/<span class=\"dhxform_item_required\">[^<]*<\/span>/g, "")
    },
    setValue: function(a, c) {
        this.setChecked(a, (c === true || parseInt(c) == 1 || c == "true" || a._value === c || c == "Y"))
    },
    getValue: function(a, c) {
        if (c == "realvalue") {
            return a._value
        }
        return ((typeof(a._value) == "undefined" || a._value == null || a._value == "") ? (a._checked ? 1 : 0) : a._value)
    },
    setReadonly: function(a, c) {
        a._ro = (c === true)
    },
    isReadonly: function(a) {
        return a._ro
    },
    setFocus: function(a) {
        a.childNodes[a._ll ? 0 : 1].childNodes[0].focus()
    }
};
dhtmlXForm.prototype.items.select.doAddLabel = function(c, e) {
    var a = document.createElement("DIV");
    a.className = "dhxform_label " + e.labelAlign;
    a.innerHTML = "<label for='" + e.uid + "'>" + (c._required ? "<span class='dhxform_item_required'>*</span>" : "") + e.label + (e.info ? "<span class='dhxform_info'>[?]</span>" : "") + "</label>";
    if (e.wrap == true) {
        a.style.whiteSpace = "normal"
    }
    if (typeof(e.tooltip) != "undefined") {
        a.title = e.tooltip
    }
    c.appendChild(a);
    if (typeof(e.label) == "undefined" || e.label == null || e.label.length == 0) {
    	c.classList.add("dhxform_label_hidden");
    }else if(e.label == "#BLANK#"){
    	c.classList.add("dhxform_label_blank");
    }
    if(e.position == "dhxform_item_label_top"){
    	a.style.width = e.inputWidth + "px"
    }else if (!isNaN(e.labelWidth)) {
    	a.style.width = e.labelWidth + "px"
    }
    if (!isNaN(e.labelHeight)) {
        a.style.height = parseInt(e.labelHeight) + "px"
    }
    if (!isNaN(e.labelLeft)) {
        a.style.left = parseInt(e.labelLeft) + "px"
    }
    if (!isNaN(e.labelTop)) {
        a.style.top = parseInt(e.labelTop) + "px"
    }
    if (e.info) {
        a.onclick = function(h) {
            h = h || event;
            var g = h.target || h.srcElement;
            if (typeof(g.className) != "undefined" && g.className == "dhxform_info") {
                this.parentNode.callEvent("onInfo", [this.parentNode._idd, h]);
                h.cancelBubble = true;
                if (h.preventDefault) {
                    h.preventDefault()
                } else {
                    h.returnValue = false
                }
                return false
            }
        }
    }
}
dhtmlXForm.prototype.items.select.setText= function(a, c) {
    if (!c) {
        c = ""
    }
    const temp = document.createElement('div');
    temp.appendChild(document.createTextNode(c));
    const label = temp.innerHTML;
    a.childNodes[a._ll ? 0 : 1].childNodes[0].innerHTML = (a._required ? "<span class='dhxform_item_required'>*</span>" : "") + label;
    a.childNodes[a._ll ? 0 : 1].style.display = (c.length == 0 || c == null ? "none" : "")
    var b = a.childNodes[a._ll ? 0 : 1].childNodes[0];
    if((b.parentElement.style.width > 0) && b.parentElement.offsetWidth < b.offsetWidth){
    	b.parentElement.style.width=parseInt(b.offsetWidth)+'px'
    }
}
dhtmlXForm.prototype.items.select.doAddInput= function(w, g, c, o, r, h, l) {
	var a = document.createElement("DIV");
    a.className = "dhxform_control";
    a.classList.add("xi-sepoa-dummy-after")
    if (w._ll) {
        w.appendChild(a)
    } else {
        w.insertBefore(a, w.firstChild)
    }
    var v = document.createElement(c);
    v.className = l;
    v.name = w._idd;
    v._idd = w._idd;
    v.id = g.uid;
    v.autocomplete="off"
    if (g.placeholder){
    	v.placeholder =  (g.placeholder.toUpperCase() == '{LABEL}')?g.label:g.placeholder
    }
    if (typeof(o) == "string") {
        v.type = o
    }
    if (c == "INPUT" || c == "TEXTAREA") {
        v.onkeyup = function(u) {
            u = u || event;
            w.callEvent("onKeyUp", [this, u, this._idd])
        };
        v.onkeydown = function(u) {
            u = u || event;
            w.callEvent("onKeyDown", [this, u, this._idd])
        }
        
        if(dhtmlXForm.prototype.properties.xbtn){
            var x = this.xbtn = document.createElement('div');
            x.className = 'dhxform_xbtn';
            a.parentElement.insertBefore(x, a.nextSibling);
            w.xbtn=x;
            x._idd=w._idd;
            x._formObj=w;
            x._itemInput=v;
            x.onclick = function(u){
            	u = u || event;
            	if(false !== this._formObj.callEvent('onXButtonClick',[this._idd])){
            		this._itemInput.value = '';
            	}
            }
        }
    }
    var back = null;
    if (c == "SELECT" && g.type == "select" && w.getForm().skin == "material") {
    	if (window.dhx4.isOpera || window.dhx4.isChrome) {
            v.className += " dhxform_arrow_fix_webkit"
        } else {
            if (window.dhx4.isEdge) {
                v.className += " dhxform_arrow_fix_edge"
            } else {
                if (window.dhx4.isFF) {
                    v.className += " dhxform_fix_ff"
                }
            }
        }
    	a.style.zIndex=1;
    }
    a.appendChild(v);
    
    if(c == "SELECT" && g.type == "select"){
    	a.classList.add("sepoa-bypass-event");
    	a.classList.add("xi-caret-down-min");
    }
    
    if (r) {
        if (!isNaN(g.inputLeft)) {
            a.style.left = parseInt(g.inputLeft) + "px"
            if(g.position != "dhxform_item_label_left" && back){
            	back.style.left = parseInt(g.inputLeft) + "px"
            }
        }
    }
    var s = "";
    var m = false;
    if (h) {
    	if (!isNaN(g.inputWidth)) {
    		a.style.width = parseInt(g.inputWidth) + "px"
    		
            if(g.position != "dhxform_item_label_left" && back){
            	back.style.width = parseInt(g.inputWidth) + "px"
            }
            m = true
        }else if(g.inputWidth == "*"){
        	a.classList.add("dhxform_control_width_full");
        }
        if (!isNaN(g.inputHeight)) {
        	a.style.height = parseInt(g.inputHeight) + "px"
        }else if(g.inputHeight == "*"){
        	a.classList.add("dhxform_control_height_full");
        }
    }
    if (typeof(g.style) == "string") {
        s += g.style
    }
    v.style.cssText = s;
    if (g.maxLength) {
        v.setAttribute("maxLength", g.maxLength)
    }
    if (g.connector) {
        v.setAttribute("connector", g.connector)
    }
    var e = (dhtmlXForm.prototype.items[this.t] != null ? dhtmlXForm.prototype.items[this.t]._dimFix == true : false);
    if (m && ({
            input: 1,
            password: 1,
            select: 1,
            multiselect: 1,
            calendar: 1,
            colorpicker: 1
        }[this.t] == 1 || e)) {
        dhtmlXForm.prototype.items[this.t]._dim = w.getForm()._checkDim(a, v)
        v.style.width = parseInt(v.style.width) - dhtmlXForm.prototype.items[this.t]._dim + "px"
    }
    if (typeof(g.note) == "object") {
        var n = document.createElement("DIV");
        n.className = "dhxform_note";
        n.style.width = (isNaN(g.note.width) ? v.offsetWidth : parseInt(g.note.width)) + "px";
        n._w = g.note.width;
        n.innerHTML = g.note.text;
        a.appendChild(n);
        a.classList.add("dhxform_note_included")
        n = null
    }
    if (g.readonly) {
    	if(this.setReadonly){
    		this.setReadonly(w, true)
    	}else if(this.userDisable){
    		this.userDisable(w)
    	}
    }
    if (g.disabled == true) {
        this.userDisable(w)
    }
    if (g.hidden == true && this.t != "combo") {
        this.hide(w)
    }
    if (g.value !== undefined && g.value.lenght > 0){
    	if(w.getForm().beforeSelectItem){
    		w.getForm().beforeSelectItem[g.name] = g.value;
    	}
    }
}
dhtmlXForm.prototype.items.select.getValue = function(e) {
    var a = '';
    var c = this.getOptions(e);
    for (var g = 0; g < c.length; g++) {
        if (c[g].selected) {
            a = c[g].value
        }
    }
    return a
}
dhtmlXForm.prototype.items.vault = {
    render: function(a, c) {
        a._type = "vault";
        a._enabled = true;
        this.doAddLabel(a, c);
        this.doAddInput(a, c, "DIV", null, true, true, "dhxform_container dhxform_vault");
        return this
    },
    getVault: function(a) {
        return a.childNodes[a._ll ? 1 : 0].childNodes[0].vault;
    },
    enable: function(a) {
        a._enabled = true;
        if (String(a.className).search("disabled") >= 0) {
            a.className = String(a.className).replace(/disabled/gi, "")
            var v = a.childNodes[a._ll ? 1 : 0].childNodes[0].vault
        	if(v){
        		v.changeMode('')
        	}
        }
        a.callEvent("onEnable", [a._idd])
    },
    disable: function(a) {
        a._enabled = false;
        if (String(a.className).search("disabled") < 0) {
            a.className += " disabled";
            var v = a.childNodes[a._ll ? 1 : 0].childNodes[0].vault
        	if(v){
        		v.changeMode('VI')
        	}
        }
        a.callEvent("onDisable", [a._idd])
    },
    setReadonly: function(a, c) {
        a._ro = (c === true);
        var v = a.childNodes[a._ll ? 1 : 0].childNodes[0].vault
    	if(v){
    		if (a._ro) {
    			v.changeMode('VI')
	        } else {
	        	v.changeMode('')
	        }
    		
    	}
    },
    doAttachEvents: function() {},
    setValue: function(a, v) {
    	a.childNodes[a._ll ? 1 : 0].childNodes[0].vault.setAttachKey(v);
    	a.childNodes[a._ll ? 1 : 0].childNodes[1].value=v;
    	a._value = v;
    },
    getValue: function(a) {
    	var v = a.childNodes[a._ll ? 1 : 0].childNodes[0].vault
    	if(v){
    		return v.getAttachKey();
    	}else{
    		return a.parentElement.parentElement._value;
    	}
    },
    doAddInput: function(w, g, c, o, r, h, l) {
        var a = document.createElement("DIV");
        a.className = "dhxform_control";
        a.classList.add("xi-sepoa-dummy-after")
        if (w._ll) {
            w.appendChild(a)
        } else {
            w.insertBefore(a, w.firstChild)
        }
        var v = document.createElement(c);
        v.className = l;
        v.name = w._idd;
        v._idd = w._idd;
        v.id = g.uid;
        v.autocomplete="off"
        if (typeof(o) == "string") {
            v.type = o
        }
        a.appendChild(v);
        
        var u = document.createElement('input');
        u.type="hidden";
        u.className="dhxform_input";
        u.name = w._idd;
        a.appendChild(u);
        
        if (r) {
            if (!isNaN(g.inputLeft)) {
                a.style.left = parseInt(g.inputLeft) + "px"
            }
            if (!isNaN(g.inputTop)) {
                a.style.top = parseInt(g.inputTop) + "px"
            }
        }
        var s = "";
        var m = false;
        if (h) {
            if (!isNaN(g.inputWidth)) {
                s += "width:" + parseInt(g.inputWidth) + "px;";
                m = true
            }else if(g.inputWidth == "*"){
            	a.classList.add("dhxform_control_width_full");
            }
            if (!isNaN(g.inputHeight)) {
                s += "height:" + parseInt(g.inputHeight) + "px;"
            }else if(g.inputHeight == "*"){
            	a.classList.add("dhxform_control_height_full");
            }
        }
        if (typeof(g.style) == "string") {
            s += g.style
        }
        v.style.cssText = s;
        if (g.maxLength) {
            v.setAttribute("maxLength", g.maxLength)
        }
        if (g.connector) {
            v.setAttribute("connector", g.connector)
        }
        if (typeof(g.note) == "object") {
            var n = document.createElement("DIV");
            n.className = "dhxform_note";
            n.style.width = (isNaN(g.note.width) ? v.offsetWidth : parseInt(g.note.width)) + "px";
            n._w = g.note.width;
            n.innerHTML = g.note.text;
            a.appendChild(n);
            a.classList.add("dhxform_note_included")
            n = null
        }
        var o = {
        	type:g.uploadType,
            container:v.appendChild($('<div style="width:100%;height:100%;"></div>')[0]),
            url:g.servlet||null,
            service : g.service || null,
            select : g.select || null,
            upload : g.upload|| null,
            'delete' : g['delete'] || null
        }
        if(window.SepoaDhtmlXVaultObject){
        	var l = v.vault = new SepoaDhtmlXVaultObject(o);
        	l.formInput = u;
        	l.attachEvent('onUploadFile', function(e, a, param){
        		this.formInput.value = e.attach_key;
        		this.formInput.parentElement.parentElement._value = e.attach_key;  
        		this.formInput.parentElement.parentElement.getForm().callEvent('onUploadFile', [this.formInput.parentElement.childNodes[0]._idd, e, a, param]);
        	})
        }
        
        if (g.readonly) {
            this.setReadonly(w, true)
        }
        if (g.disabled == true) {
            this.userDisable(w)
        }
        if (g.hidden == true && this.t != "combo") {
            this.hide(w)
        }
    }
};
dhtmlXForm.prototype.getVault = function(a) {
    return this.doWithItem(a, "getVault")
};
(function() {
    for (var c in dhtmlXForm.prototype.items.container) {
        if (!dhtmlXForm.prototype.items.vault[c]) {
            dhtmlXForm.prototype.items.vault[c] = dhtmlXForm.prototype.items.container[c]
        }
    }
})();
dhtmlXForm.prototype.items.input={
    render: function(l, m) {
        var h = (!isNaN(m.rows));
        l._type = "ta";
        l._enabled = true;
        
        this.doAddLabel(l, m);
        this.doAddInput(l, m, (h ? "TEXTAREA" : "INPUT"), (h ? null : "TEXT"), true, true, "dhxform_textarea");
        this.doAttachEvents(l);
        if (h) {
            l.childNodes[l._ll ? 1 : 0].childNodes[0].rows = Number(m.rows) + (window.dhx4.isIE6 ? 1 : 0)
        }
        if (typeof(m.numberFormat) != "undefined") {
            var g, e = null,
                n = null;
            if (typeof(m.numberFormat) != "string") {
                g = m.numberFormat[0];
                e = m.numberFormat[1] || null;
                n = m.numberFormat[2] || null
            } else {
                g = m.numberFormat;
                if (typeof(m.groupSep) == "string") {
                    e = m.groupSep
                }
                if (typeof(m.decSep) == "string") {
                    n = m.decSep
                }
            }
            this.setNumberFormat(l, g, e, n, false)
        }
        this.setValue(l, m.value);
        return this
    },
    doAddLabel:function(c, e) {
        var a = document.createElement("DIV");
        a.className = "dhxform_label " + e.labelAlign;
        a.innerHTML = "<label for='" + e.uid + "'>" + (c._required ? "<span class='dhxform_item_required'>*</span>" : "") + e.label + (e.info ? "<span class='dhxform_info'>[?]</span>" : "") + "</label>";
        if (e.wrap == true) {
            a.style.whiteSpace = "normal"
        }
        if (typeof(e.tooltip) != "undefined") {
            a.title = e.tooltip
        }
        c.appendChild(a);
        if (typeof(e.label) == "undefined" || e.label == null || e.label.length == 0 || (e.placeholder && e.placeholder.toUpperCase() == '{LABEL}')) {
        	c.classList.add("dhxform_label_hidden");
        }else if(e.label == "#BLANK#"){
        	c.classList.add("dhxform_label_blank");
        }
        if(e.position == "dhxform_item_label_top"){
        	a.style.width = e.inputWidth + "px"
        }else if (!isNaN(e.labelWidth)) {
        	a.style.width = e.labelWidth + "px"
        }
        if (!isNaN(e.labelHeight)) {
            a.style.height = parseInt(e.labelHeight) + "px"
        }
        if (!isNaN(e.labelLeft)) {
            a.style.left = parseInt(e.labelLeft) + "px"
        }
        if (!isNaN(e.labelTop)) {
            a.style.top = parseInt(e.labelTop) + "px"
        }
        if (e.info) {
            a.onclick = function(h) {
                h = h || event;
                var g = h.target || h.srcElement;
                if (typeof(g.className) != "undefined" && g.className == "dhxform_info") {
                    this.parentNode.callEvent("onInfo", [this.parentNode._idd, h]);
                    h.cancelBubble = true;
                    if (h.preventDefault) {
                        h.preventDefault()
                    } else {
                        h.returnValue = false
                    }
                    return false
                }
            }
        }
    },
    doAttachEvents: function(e) {
        var c = e.childNodes[e._ll ? 1 : 0].childNodes[0];
        if (typeof(c.tagName) != "undefined" && {
                input: 1,
                textarea: 1,
                select: 1
            }[c.tagName.toLowerCase()] == 1) {
            var a = this;
            c.onfocus = function() {
                var g = this.parentNode.parentNode;
                if(!a.isReadonly(g)){
	                if (g._df != null) {
	                    this.value = g._value || ""
	                    if(this.value.length != 0){
	                    	if (this.createTextRange) {
	                            var range = this.createTextRange();
	                            range.collapse(true);
	                            range.moveEnd('character', this.value.length);
	                            range.moveStart('character', this.value.length);
	                            range.select();
	                        } else if (this.setSelectionRange) {
	                            this.focus();
	                            this.setSelectionRange(this.value.length, this.value.length);
	                        }
	                    }
	                }
                }
                g.getForm()._ccActivate(g._idd, this, this.value);
                g.getForm().callEvent("onFocus", [g._idd]);
                g = null
            };
            c.onblur = function() {
                var g = this.parentNode.parentNode;
                g.getForm()._ccDeactivate(g._idd);
                a.updateValue(g, true);
                if (g.getForm().live_validate) {
                    a._validate(g)
                }
                g.getForm().callEvent("onBlur", [g._idd]);
                g = null
            }
        }
        c = null
    },
    updateValue: function(h, a) {
        var l = h.childNodes[h._ll ? 1 : 0].childNodes[0].value;
        var g = h.getForm();
        var c = (g._ccActive == true && g._formLS != null && g._formLS[h._idd] != null);
        g = null;
        if (!c && h._df != null && l == window.dhx4.template._getFmtValue(h._value, h._df)) {
            return
        }
        if (!a && h._df != null && h._value == l) {
            return
        }
        var e = this;
        if (h._value != l) {
        	if(h._df){
        		l = sUtils.number.unformat(l);
        	}
            if (h.checkEvent("onBeforeChange")) {
                if (h.callEvent("onBeforeChange", [h._idd, h._value, l]) !== true) {
                    if (h._df != null) {
                        e.setValue(h, h._value)
                    } else {
                        h.childNodes[h._ll ? 1 : 0].childNodes[0].value = h._value
                    }
                    return
                }
            }
            if (h._df != null && a) {
                e.setValue(h, l)
            } else {
                h._value = l
            }
            h.callEvent("onChange", [h._idd, l]);
            return
        }
        if (h._df != null && a) {
            this.setValue(h, h._value)
        }
    },
    setValue: function(e, g) {
        e._value = (typeof(g) != "undefined" && g != null ? g : "");
        var c = (String(e._value) || "");
        var a = e.childNodes[e._ll ? 1 : 0].childNodes[0];
        if (e._df != null) {
            c = window.dhx4.template._getFmtValue(c, e._df)
        }
        if (a.value != c) {
            a.value = c;
            e.getForm()._ccReload(e._idd, c)
        }
        a = null
    },
    getValue: function(a) {
        var c = a.getForm();
        if (c._formLS && c._formLS[a._idd] != null) {
            this.updateValue(a)
        }
        c = null;
        return (typeof(a._value) != "undefined" && a._value != null ? a._value : "")
    },
    setReadonly: function(a, c) {
        a._ro = (c === true);
        if (a._ro) {
        	a.classList.add("readonly");
        	a.childNodes[a._ll ? 1 : 0].childNodes[0].setAttribute("readOnly", "true");
        } else {
        	a.classList.remove("readonly");
        	a.childNodes[a._ll ? 1 : 0].childNodes[0].removeAttribute("readOnly");
        }
    },
    isReadonly: function(a) {
        if (!a._ro) {
            a._ro = false
        }
        return a._ro
    },
    getInput: function(a) {
        return a.childNodes[a._ll ? 1 : 0].childNodes[0]
    },
    setNumberFormat: function(g, h, e, l, c) {
        if (typeof(c) != "boolean") {
            c = true
        }
        if (h == "") {
            g._df = null;
            if (c) {
                this.setValue(g, g._value)
            }
            return true
        }
        if (typeof(h) != "string") {
            return
        }
        var a = window.dhx4.template._parseFmt(h, e, l);
        if (a == false) {
            return false
        } else {
            g._df = a
        }
        if (c) {
            this.setValue(g, g._value)
        }
        return true
    }
};
(function() {
    for (var c in {
    		destruct: 1,
    		doAddInput: 1,
            doUnloadNestedLists: 1,
            setText: 1,
            getText: 1,
            enable: 1,
            disable: 1,
            isEnabled: 1,
            setWidth: 1,
            setFocus: 1
        }) {
        dhtmlXForm.prototype.items.input[c] = dhtmlXForm.prototype.items.select[c]
    }
})();
(function() {
	for (var c in {
    	doAddInput: 1
    }){
		dhtmlXForm.prototype.items.container[c] = dhtmlXForm.prototype.items.input[c]
    }
})();
dhtmlXForm.prototype.items.autocomplete = {	
	render: function(c, e) {
		c._type = "autocomplete";
		c._enabled = true;
		c._value = null;
		c._newValue = null;
		var g = c.getForm().skin;
		if (typeof(e.inputWidth) != "undefined" && g == "material" && String(e.inputWidth).match(/^\d*$/) != null) {
		    e.inputWidth = parseInt(e.inputWidth);
		}
		this.doAddLabel(c, e);
		this.doAddInput(c, e, "SELECT", null, true, true, "dhxform_select");
		this.doAttachEvents(c);
		this.doLoadOpts(c, e);
		c.onselectstart = function(h) {
		    return true
		};
		c.childNodes[c._ll ? 1 : 0].childNodes[0].setAttribute("mode", e.comboType || "");
		if (e.comboImagePath) {
		    c.childNodes[c._ll ? 1 : 0].childNodes[0].setAttribute("imagePath", e.comboImagePath)
		}
		if (e.comboDefaultImage) {
		    c.childNodes[c._ll ? 1 : 0].childNodes[0].setAttribute("defaultImage", e.comboDefaultImage)
		}
		if (e.comboDefaultImageDis) {
		    c.childNodes[c._ll ? 1 : 0].childNodes[0].setAttribute("defaultImageDis", e.comboDefaultImageDis)
		}
		c._combo = new dhtmlXComboFromSelect(c.childNodes[c._ll ? 1 : 0].childNodes[0]);
		c._combo.setSkin(g);
		c._combo._currentComboValue = c._combo.getSelectedValue();
		c._combo.getInput().id = e.uid;
		
		if(e.placeholder){
			c._combo.getInput().placeholder = e.placeholder
		}
		c._combo.getInput().autocomplete="off"
		
		c._combo._mcMakeTemplate = function(o) {
		    var l = "";
		    var g = "";
		    this.conf.col_w = 0;
		    for (var m = 0; m < o.length; m++) {
		        var a = Number(parseInt(o[m].width) || 50);
		        var e = (o[m].css || "");
		        var n = (m == 0 && window.dhx4.isIE6 == true ? "_first" : "");
		        g += "<div class='dhxcombo_cell" + n + " " + e + "' style='min-width:" + a + "px;'><div class='dhxcombo_cell_text'>" + (o[m].option || "&nbsp;") + "</div></div>";
		        l += "<div class='dhxcombo_hdrcell" + n + " " + e + "' style='min-width:" + a + "px;'><div class='dhxcombo_hdrcell_text'>" + (o[m].header || "&nbsp;") + "</div></div>";
		        this.conf.col_w += a + 1
		    }
		    var a = 500;
		    var c = document.createElement("DIV");
		    c.style.position = "absolute";
		    c.style.top = "10px";
		    c.style.left = -a * 2 + "px";
		    c.style.width = a + "px";
		    c.style.height = "50px";
		    c.style.overflowY = "scroll";
		    c.innerHTML = "<div>&nbsp;</div>";
		    document.body.appendChild(c);
		    this.conf.col_w += a - c.firstChild.offsetWidth + 10;
		    c.parentNode.removeChild(c);
		    c = null;
		    this.conf.template.option = g;
		    this._mcAttachHeader(l);
		    this.list.className += " dhxcombolist_multicolumn"
		};
		c._combo._showList = function(a) {
		    if (this._getListVisibleCount() == 0) {
		        if (a && this._isListVisible()) {
		            this._hideList()
		        }
		        return
		    }
		    if (this._isListVisible()) {
		        this._checkListHeight();
		        return
		    }
		    this.list.style.zIndex = window.dhx4.zim.reserve(this.conf.list_zi_id);
		    if (this.hdr != null && this.conf.template.header == true) {
		        this.hdr.style.zIndex = Number(this.list.style.zIndex) + 1
		    }
		    this.list.style.visibility = "hidden";
		    this.list.style.display = "";
		    if (this.hdr != null && this.conf.template.header == true) {
		        this.hdr.style.visibility = this.list.style.visibility;
		        this.hdr.style.display = this.list.style.display
		    }
		    var c = (this.hdr != null && this.conf.template.header == true ? this.hdr.offsetHeight : 0);
		    this.list.style.minWidth = Math.max(this.conf.opts_width || this.conf.col_w || 0, this.conf.combo_width) + "px";
		    this.list.style.top = window.dhx4.absTop(this.base) + c + this.base.offsetHeight - 1 + "px";
		    this.list.style.left = window.dhx4.absLeft(this.base) + "px";
		    if (this.hdr != null && this.conf.template.header == true) {
		        this.hdr.style.width = this.list.style.width;
		        this.hdr.style.left = this.list.style.left;
		        this.hdr.style.top = parseInt(this.list.style.top) - c + "px"
		    }
		    this._checkListHeight();
		    this.list.style.visibility = "visible";
		    if (this.hdr != null && this.conf.template.header == true) {
		        this.hdr.style.visibility = "visible"
		    }
		    this.callEvent("onOpen", [])
		};
		c._combo.setTemplate({
		    input: "#TEXT#",
		    header: false,
		    columns: [
		        {header: "", option: "#TEXT#"},
		        {header: "", option: "#DESCRIPTION#"}
		    ]
		});
		
		if (g == "material") {
		    c._combo.list.className += " dhxform_obj_" + g
		}
		var a = this;
		c._combo.attachEvent("onChange", function() {
			a.doOnChange(this)
		});
		if (e.connector) {
		    this.doLoadOptsConnector(c, e.connector)
		}
		c._combo.parents = c.getForm();
		c._combo._sConfig = {
			dynParam:[]
		}
		
		if(e.userdata && e.userdata.combo){
			c._combo._sConfig.code = e.userdata.combo.split(',')[0];
			c._combo._sConfig.param = e.userdata.combo.split(',')[1] || ''; 
		}
		
		if(e.warningText){
			c._combo._warningText = e.warningText;
		}
		c._combo.enableFilteringMode(true, "custom")
		c._combo.attachEvent("onDynXLS", function(text, ind, def){
			var load = function(obj){
				text = text || '';
				obj._sConfig.dynParam = [];
				obj.parents.callEvent("onAutoComplete", [obj._sConfig.dynParam, obj._sConfig]);
				var param = obj._sConfig.param;
				if(obj._sConfig.dynParam.length > 0){
					param = param + "#" + obj._sConfig.dynParam.join('#'); 
				}
				var qs = "code="+obj._sConfig.code+"&param="+window.dhx4.comboParam(param || '')
				if(def){
					qs = qs+"&default="+def
				}else{
					qs = qs+"&mask="+text
				}
				if(obj._sConfig.PROCESS_ID){
					qs = qs+"&PROCESS_ID="+obj._sConfig.PROCESS_ID
				}
				if(obj._sConfig.METHOD){
					qs = qs+"&METHOD="+obj._sConfig.METHOD
				}
				$.ajax({
					url: window.CONTEXT_NAME+"/SepoaAutoComplete.sux",
					type:"post",
					data: qs,
					comboObj:obj,
					defaultOption:def,
					dataType: "json",
					cache: false,
					success: function(data){
						if(data.length ==0 && this.comboObj._warningText){
							Alert(this.comboObj._warningText);
						}
						this.comboObj.clearAll();
						this.comboObj._sepoadata = {};
						var flag = true;
						for(var i in data){
							this.comboObj._sepoadata[data[i].VALUE] = {}
							for(var j in data[i]){
								this.comboObj._sepoadata[data[i].VALUE][j] = data[i][j];
							}
							flag = false;
							this.comboObj.addOption([{value:data[i].VALUE, text:this.comboObj._sepoadata[data[i].VALUE]}]);
						}
						if(flag){
							this.comboObj.callEvent("onChange", []);
						}
						if(this.defaultOption){
							this.comboObj._sConfig._disableEvent = true;
							this.comboObj.setComboValue(this.defaultOption);
							this.comboObj._sConfig._disableEvent = false;
				            this.comboObj._currentComboValue = this.comboObj.getActualValue();
						}else{
							this.comboObj.callEvent("onChange", []);
							this.comboObj.openSelect();
						}
					}
				});
				obj._sConfig.f_server_tm = null;
			}
			if(this._sConfig.f_server_tm){
				 window.clearTimeout(this._sConfig.f_server_tm)
			}
			if(def){
				this._sConfig.f_server_tm = window.setTimeout(load, 0, this);
			}else if ((text == null || text.trim().length == 0) && !this.forceFlag) {
				this.closeAll();
				this.clearAll();
				this.setComboText("");
			} else {
				if(this._sConfig.code){
					this._sConfig.f_server_tm = window.setTimeout(load , 500, this)
				}
				this.forceFlag = false;
			}
		});
		
		if (e.readonly == true) {
		    this.setReadonly(c, true)
		}
		if (e.hidden == true) {
		    this.hide(c)
		}
		if (e.style) {
		    c._combo.DOMelem_input.style.cssText += e.style
		}
		c._combo.attachEvent("onFocus", function() {
		    var h = this.cont.parentNode.parentNode;
		    var l = h.getForm();
		    if ((l.skin == "dhx_terrace" || l.skin == "material") && this.cont.className.search(/combo_in_focus/) < 0) {
		        this.cont.className += " combo_in_focus"
		    }
		    l.callEvent("onFocus", [h._idd]);
		    l = h = null
		});
		c._combo.attachEvent("onBlur", function() {
		    var h = this.cont.parentNode.parentNode;
		    var l = h.getForm();
		    if ((l.skin == "dhx_terrace" || l.skin == "material") && this.cont.className.search(/combo_in_focus/) >= 0) {
		        this.cont.className = this.cont.className.replace(/\s{0,}combo_in_focus/gi, "")
		    }
		    l.callEvent("onBlur", [h._idd]);
		    l = h = null
		});
		return this
	},
	doOnChange: function(c) {
        var a = c.base.parentNode.parentNode.parentNode;
        if (a._apiChange) {
            return
        }
        c._newComboValue = c.getSelectedValue();
        if (c._newComboValue != c._currentComboValue) {
        	if (!c._sConfig._disableEvent && a.checkEvent("onBeforeChange")) {
                if (a.callEvent("onBeforeChange", [a._idd, c._currentComboValue, c._newComboValue, c._sepoadata[c._newComboValue]]) !== true) {
                    window.setTimeout(function() {
                        c.setComboValue(c._currentComboValue)
                    }, 1);
                    return false
                }
            }
            c._currentComboValue = c._newComboValue;
            if(!c._sConfig._disableEvent){
            	a.callEvent("onChange", [a._idd, c._currentComboValue, c._sepoadata[c._currentComboValue]])
            }
        }else{
        	a.callEvent("onChange", [a._idd, null])
        }
        a._autoCheck()
    },
    setValue: function(a, c) {
        if (a._connector_working) {
            a._connector_value = c;
            return
        }
        if(a._combo.getIndexByValue(c) >= 0){
        	a._apiChange = true;
        	a._combo.setComboValue(c);
            a._combo._currentComboValue = a._combo.getActualValue();
            a._apiChange = false
        }else{
        	a._combo.callEvent("onDynXLS",[null, null, c]);
        }
    },
    destruct: function(a) {
        a.childNodes[a._ll ? 1 : 0].childNodes[0].onchange = null;
        a._combo._currentComboValue = null;
        a._combo.unload();
        a._combo = null;
        a._apiChange = null;
        a = null
    }
};
(function() {
    for (var c in {
	    	doAttachEvents: 1,
	    	doValidate: 1,
	    	doLoadOptsConnector: 1,
	    	enable: 1,
	    	disable: 1,
	    	getCombo: 1,
	    	getValue: 1,
	    	setWidth: 1,
	    	setReadonly: 1,
	    	isReadonly: 1,
	    	setFocus: 1,
	    	_setCss: 1,
	    	doAddLabel: 1,
	        doAddInput: 1,
	        doLoadOpts: 1,
	        doUnloadNestedLists: 1,
	        setText: 1,
	        getText: 1,
	        isEnabled: 1,
	        _checkNoteWidth: 1
        }) {
        dhtmlXForm.prototype.items.autocomplete[c] = dhtmlXForm.prototype.items.combo[c]
    }
})();
dhtmlXForm.prototype.items.calendar = {
    render: function(e, h) {
        var c = this;
        e._type = "calendar";
        e._enabled = true;
        var l = navigator.userAgent;
        var a = (l.indexOf("MSIE 6.0") >= 0 || l.indexOf("MSIE 7.0") >= 0 || l.indexOf("MSIE 8.0") >= 0);
        this.doAddLabel(e, h);
        this.doAddInput(e, h, "INPUT", "TEXT", true, true, "dhxform_textarea calendar");
        this.doAttachChangeLS(e);
        if (a) {
            e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus2 = e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus;
            e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus = function() {
                if (this._skipOnFocus == true) {
                    this._skipOnFocus = false;
                    return
                }
                this.onfocus2.apply(this, arguments)
            }
        }
        e.childNodes[e._ll ? 1 : 0].onclick=function(evt){
        	if(!e._ro){
        		if(!e._doubleCalendar){
	        		e._c.show();
		        	e._c.setDate(e._value || (new Date()));
        		}
	        	evt.stopPropagation();
	        	evt.preventDefault();
        	}
        }
        e.childNodes[e._ll ? 1 : 0].classList.add("xi-calendar");
        e.childNodes[e._ll ? 1 : 0].childNodes[0]._idd = e._idd;
        e.childNodes[e._ll ? 1 : 0].childNodes[0].onblur = function() {
            var m = this.parentNode.parentNode;
            if (m._c.base._formMouseDown) {
                m._c.base._formMouseDown = false;
                this._skipOnFocus = true;
                this.focus();
                this.value = this.value;
                m = null;
                return true
            }
            var n = m.getForm();
            n._ccDeactivate(m._idd);
            c.checkEnteredValue2(this);
            if (n.live_validate) {
                c._validate(m)
            }
            n.callEvent("onBlur", [m._idd]);
            if (!m._c.isVisible()) {
                m._tempValue = null
            }
            n = m = null
        };
        e._f = (h.dateFormat || null);
        e._f0 = (h.serverDateFormat || e._f);
        var g = e.getForm();
        e._c = new dhtmlXCalendarObject(e.childNodes[e._ll ? 1 : 0].childNodes[0], h.skin || g.skin || "dhx_skyblue");
        e._c._nullInInput = true;
        e._c.enableListener(e.childNodes[e._ll ? 1 : 0].childNodes[0]);
        if (e._f != null) {
            e._c.setDateFormat(e._f)
        }
        if (!window.dhx4.s2b(h.enableTime)) {
            e._c.hideTime()
        }
        if (window.dhx4.s2b(h.enableTodayButton)) {
            e._c.showToday()
        }
        if (window.dhx4.s2b(h.showWeekNumbers)) {
            e._c.showWeekNumbers()
        }
        if (!isNaN(h.weekStart)) {
            e._c.setWeekStartDay(h.weekStart)
        }
        if (typeof(h.calendarPosition) != "undefined") {
            e._c.setPosition(h.calendarPosition)
        }
        if (h.minutesInterval != null) {
            e._c.setMinutesInterval(h.minutesInterval)
        }
        e._c._itemIdd = e._idd;
        e._c.attachEvent("onBeforeChange", function(m) {
            if (e._value != m) {
                if (e.checkEvent("onBeforeChange")) {
                    if (e.callEvent("onBeforeChange", [e._idd, e._value, m]) !== true) {
                        return false
                    }
                }
                e._tempValue = e._value = m;
                c.setValue(e, m, false);
                e.callEvent("onChange", [this._itemIdd, e._value])
            }
            return true
        });
        e._c.attachEvent("onClick", function() {
            e._tempValue = null
        });
        e._c.attachEvent("onHide", function() {
            e._tempValue = null
        });
        if (a) {
            e._c.base.onmousedown = function() {
                this._formMouseDown = true;
                return false
            }
        }
        this.setValue(e, h.value);
        g = null;
        return this
    },
    getCalendar: function(a) {
        return a._c
    },
    setSkin: function(a, c) {
        a._c.setSkin(c)
    },
    setValue: function(c, e, a) {
        if (!e || e == null || typeof(e) == "undefined" || e == "") {
            c._value = null;
            c.childNodes[c._ll ? 1 : 0].childNodes[0].value = ""
        } else {
        	var date = (e instanceof Date ? e : c._c._strToDate(e, c._f0 || "%Y%m%d"));
        	if(date == "Invalid Date") date = (e instanceof Date ? e : c._c._strToDate(e, c._f0 ||Sepoa.info.DATE_FORMAT||c._c._dateFormat));
            c._value = date;
            c.childNodes[c._ll ? 1 : 0].childNodes[0].value = c._c._dateToStr(c._value, c._f ||Sepoa.info.DATE_FORMAT|| c._c._dateFormat)
        }
        if (a !== false && c._value != "Invalid Date") {
            c._c.setDate(c._value)
        }else if(a !== false && c._value == "Invalid Date"){
        	c._c.setDate();
        }
    },
    getValue: function(c, a) {
        var e = c._tempValue || c._c.getDate();
        if (a === true && e == null) {
            return ""
        }
        return (a === true ? c._c._dateToStr(e, c._f0 || "%Y%m%d") : e)
    },
    setDateFormat: function(c, a, e) {
        c._f = a;
        c._f0 = (e || c._f);
        c._c.setDateFormat(c._f);
        this.setValue(c, this.getValue(c))
    },
    destruct: function(a) {
        a._c.disableListener(a.childNodes[a._ll ? 1 : 0].childNodes[0]);
        a._c.unload();
        a._c = null;
        try {
            delete a._c
        } catch (c) {}
        a._f = null;
        try {
            delete a._f
        } catch (c) {}
        a._f0 = null;
        try {
            delete a._f0
        } catch (c) {}
        a.childNodes[a._ll ? 1 : 0].childNodes[0]._idd = null;
        a.childNodes[a._ll ? 1 : 0].childNodes[0].onblur = null;
        this.d2(a);
        a = null
    },
    checkEnteredValue: function(a) {
    	var t = a.value;
    	var a = a.parentNode.parentNode;
        this.setValue(a, a._c.getDate() || t)
    },
    checkEnteredValue2: function(a) {
    	var t = a.value;
    	var a = a.parentNode.parentNode;
        this.setValue(a, t)
    }
};
(function() {
    for (var c in {
            doAddLabel: 1,
            doAddInput: 1,
            doUnloadNestedLists: 1,
            setText: 1,
            getText: 1,
            enable: 1,
            disable: 1,
            isEnabled: 1,
            setWidth: 1,
            setReadonly: 1,
            isReadonly: 1,
            setFocus: 1,
            getInput: 1
        }) {
        dhtmlXForm.prototype.items.calendar[c] = dhtmlXForm.prototype.items.input[c]
    }
})();
dhtmlXForm.prototype.items.calendar.doAttachChangeLS = dhtmlXForm.prototype.items.select.doAttachChangeLS;
dhtmlXForm.prototype.items.calendar.d2 = dhtmlXForm.prototype.items.input.destruct;
dhtmlXForm.prototype.items.btn2state = {
    setChecked: function(a, c) {
        a._checked = (c === true ? true : false);
        a._value = (a._checked ? 1 : null);
        a.childNodes[a._ll ? 1 : 0].lastChild.className = "dhxform_img " + a._cssName + "_" + (a._checked ? "1" : "0");
        this.doCheckValue(a)
    }
};
(function() {
    for (var c in dhtmlXForm.prototype.items.checkbox) {
        if (!dhtmlXForm.prototype.items.btn2state[c]) {
            dhtmlXForm.prototype.items.btn2state[c] = dhtmlXForm.prototype.items.checkbox[c]
        }
    }
})();
dhtmlXForm.prototype.items.btn2state.render2 = dhtmlXForm.prototype.items.btn2state.render;
dhtmlXForm.prototype.items.btn2state.render = function(a, c) {
    c._autoInputWidth = false;
    this.render2(a, c);
    a._type = "btn2state";
    a._value = null;
    a._cssName = (typeof(c.cssName) == "undefined" ? "btn2state" : c.cssName);
    a._updateImgNode = function() {};
    a._doOnFocus = function() {
        a.getForm().callEvent("onFocus", [a._idd])
    };
    a._doOnBlur = function() {
        a.getForm().callEvent("onBlur", [a._idd])
    };
    a._doOnKeyUpDown = function(g, e, h) {
        this.callEvent(g, [this.childNodes[this._ll ? 0 : 1].childNodes[0], e, this._idd])
    };
    this.setChecked(a, a._checked);
    return this
};
dhtmlXForm.prototype.setFormData_btn2state = function(a, c) {
    this[c == true || parseInt(c) == 1 || c == "true" || c === this.getItemValue(a) ? "checkItem" : "uncheckItem"](a)
};
dhtmlXForm.prototype.getFormData_btn2state = function(a) {
    return (this.isItemChecked(a) ? this.getItemValue(a) : 0)
};
dhtmlXForm.prototype.items.calendarYM = {
    render: function(e, h) {
        var c = this;
        e._type = "calendar";
        e._enabled = true;
        var l = navigator.userAgent;
        var a = (l.indexOf("MSIE 6.0") >= 0 || l.indexOf("MSIE 7.0") >= 0 || l.indexOf("MSIE 8.0") >= 0);
        this.doAddLabel(e, h);
        this.doAddInput(e, h, "INPUT", "TEXT", true, true, "dhxform_textarea calendar calendarYM");
        this.doAttachChangeLS(e);
        if (a) {
            e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus2 = e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus;
            e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus = function() {
                if (this._skipOnFocus == true) {
                    this._skipOnFocus = false;
                    return
                }
                this.onfocus2.apply(this, arguments)
            }
        }
        e.childNodes[e._ll ? 1 : 0].childNodes[0]._idd = e._idd;
        e.childNodes[e._ll ? 1 : 0].childNodes[0].onblur = function() {
            var m = this.parentNode.parentNode;
            if (m._c.base._formMouseDown) {
                m._c.base._formMouseDown = false;
                this._skipOnFocus = true;
                this.focus();
                this.value = this.value;
                m = null;
                return true
            }
            var n = m.getForm();
            n._ccDeactivate(m._idd);
            c.checkEnteredValue2(this);
            if (n.live_validate) {
                c._validate(m)
            }
            n.callEvent("onBlur", [m._idd]);
            if (!m._c.isVisible()) {
                m._tempValue = null
            }else{
            	m._c.hide();
            }
            n = m = null
        };
        e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus=function(){
        	if (!e._value) {
        		this.setValue(e, new Date());
        	}
        }.bind(this);
        e._f = (h.dateFormat || null);
        e._f0 = (h.serverDateFormat || e._f);
        var g = e.getForm();
        e._c = new SepoaDhtmlXCalendarYM(e.childNodes[e._ll ? 1 : 0].childNodes[0], h.skin || g.skin || "dhx_skyblue");
        e._c._nullInInput = true;
        e._c.enableListener(e.childNodes[e._ll ? 1 : 0].childNodes[0]);
        if (e._f != null) {
            e._c.setDateFormat(e._f)
        }
        if (!window.dhx4.s2b(h.enableTime)) {
            e._c.hideTime()
        }
        if (window.dhx4.s2b(h.enableTodayButton)) {
            e._c.showToday()
        }
        if (window.dhx4.s2b(h.showWeekNumbers)) {
            e._c.showWeekNumbers()
        }
        if (!isNaN(h.weekStart)) {
            e._c.setWeekStartDay(h.weekStart)
        }
        if (typeof(h.calendarPosition) != "undefined") {
            e._c.setPosition(h.calendarPosition)
        }
        if (h.minutesInterval != null) {
            e._c.setMinutesInterval(h.minutesInterval)
        }
        e._c._itemIdd = e._idd;
        e._c.attachEvent("onBeforeChange", function(m) {
            if (e._value != m) {
                if (e.checkEvent("onBeforeChange")) {
                    if (e.callEvent("onBeforeChange", [e._idd, e._value, m]) !== true) {
                        return false
                    }
                }
                e._tempValue = e._value = m;
                c.setValue(e, m, false);
                e.callEvent("onChange", [this._itemIdd, e._value])
            }
            return true
        });
        e._c.attachEvent("onClick", function() {
            e._tempValue = null
        });
        e._c.attachEvent("onHide", function() {
            e._tempValue = null
        });
        if (a) {
            e._c.base.onmousedown = function() {
                this._formMouseDown = true;
                return false
            }
        }
        this.setValue(e, h.value);
        g = null;
        return this
    },
    getCalendar: function(a) {
        return a._c
    },
    setSkin: function(a, c) {
        a._c.setSkin(c)
    },
    setValue: function(c, e, a) {
        if (!e || e == null || typeof(e) == "undefined" || e == "") {
            c._value = null;
            c.childNodes[c._ll ? 1 : 0].childNodes[0].value = ""
        } else {
        	var date = (e instanceof Date ? e : c._c._strToDate(e,"%Y/%m"));
        	if(date == "Invalid Date") date = (e instanceof Date ? e : c._c._strToDate(e, "%Y%m"));
            c._value = date;
            c.childNodes[c._ll ? 1 : 0].childNodes[0].value = c._c._dateToStr(c._value, "%Y/%m")
        }
        if (a !== false && c._value != "Invalid Date") {
            c._c.setDate(c._value)
        }else if(a !== false && c._value == "Invalid Date"){
        	c._c.setDate();
        }
    },
    getValue: function(c, a) {
        var e = c._tempValue || c._c.getDate();
        if (a === true && e == null) {
            return ""
        }
        return (a === true ? c._c._dateToStr(e,"%Y%m") : e)
    },
    setDateFormat: function(c, a, e) {
        c._f = a;
        c._f0 = (e || c._f);
        c._c.setDateFormat(c._f);
        this.setValue(c, this.getValue(c))
    },
    destruct: function(a) {
        a._c.disableListener(a.childNodes[a._ll ? 1 : 0].childNodes[0]);
        a._c.unload();
        a._c = null;
        try {
            delete a._c
        } catch (c) {}
        a._f = null;
        try {
            delete a._f
        } catch (c) {}
        a._f0 = null;
        try {
            delete a._f0
        } catch (c) {}
        a.childNodes[a._ll ? 1 : 0].childNodes[0]._idd = null;
        a.childNodes[a._ll ? 1 : 0].childNodes[0].onblur = null;
        this.d2(a);
        a = null
    },
    checkEnteredValue: function(a) {
    	var t = a.value;
    	var a = a.parentNode.parentNode;
        this.setValue(a, a._c.getDate() || t)
    },
    checkEnteredValue2: function(a) {
    	var t = a.value;
    	var a = a.parentNode.parentNode;
        this.setValue(a, t)
    }
};
(function() {
    for (var c in {
            doAddLabel: 1,
            doAddInput: 1,
            doUnloadNestedLists: 1,
            setText: 1,
            getText: 1,
            enable: 1,
            disable: 1,
            isEnabled: 1,
            setWidth: 1,
            setReadonly: 1,
            isReadonly: 1,
            setFocus: 1,
            getInput: 1
        }) {
        dhtmlXForm.prototype.items.calendarYM[c] = dhtmlXForm.prototype.items.input[c]
    }
})();
dhtmlXForm.prototype.items.calendarYM.doAttachChangeLS = dhtmlXForm.prototype.items.select.doAttachChangeLS;
dhtmlXForm.prototype.items.calendarYM.d2 = dhtmlXForm.prototype.items.input.destruct;

dhtmlXForm.prototype.items.doubleCalendar = {
	render: function(e, h) {
        var c = this;
        e._type = "calendar";
        e._enabled = true;
        var l = navigator.userAgent;
        var a = (l.indexOf("MSIE 6.0") >= 0 || l.indexOf("MSIE 7.0") >= 0 || l.indexOf("MSIE 8.0") >= 0);
        this.doAddLabel(e, h);
        this.doAddInput(e, h, "INPUT", "TEXT", true, true, "dhxform_textarea calendar doubleCalendar");
        this.doAttachChangeLS(e);
        if (a) {
            e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus2 = e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus;
            e.childNodes[e._ll ? 1 : 0].childNodes[0].onfocus = function() {
                if (this._skipOnFocus == true) {
                    this._skipOnFocus = false;
                    return
                }
                this.onfocus2.apply(this, arguments)
            }
        }
        
        $(e.childNodes[e._ll ? 1 : 0].childNodes[0]).on('blur', {targetNode:e}, function(e){
        	e.data.targetNode.doubleCA.classList.remove('show');
        });
        e.childNodes[e._ll ? 1 : 0].classList.add("xi-calendar");
        e.childNodes[e._ll ? 1 : 0].childNodes[0]._idd = e._idd;
        e.childNodes[e._ll ? 1 : 0].childNodes[0].onblur = function() {
            var m = this.parentNode.parentNode;
            if (m._c.base._formMouseDown) {
                m._c.base._formMouseDown = false;
                this._skipOnFocus = true;
                this.focus();
                this.value = this.value;
                m = null;
                return true
            }
            var n = m.getForm();
            n._ccDeactivate(m._idd);
            c.checkEnteredValue2(this);
            if (n.live_validate) {
                c._validate(m)
            }
            n.callEvent("onBlur", [m._idd]);
            if (!m._c.isVisible()) {
                m._tempValue = null
            }
            n = m = null
        };
        e._f = (h.dateFormat || null);
        e._f0 = (h.serverDateFormat || e._f);
        var g = e.getForm();
        
        var doubleCA = e.doubleCA = SUX.root.document.body.appendChild($('<div class="sepoa_form_dbcalendar_container"></div>')[0])
        var dc = e._doubleCalendar = new dhtmlXDoubleCalendar(doubleCA);
        var targetNode = e.childNodes[e._ll ? 1 : 0].childNodes[0];
        e.inputArea = targetNode;
        e.show = this.setShow.bind(this);
        
        $(targetNode).on('click', {baseObj:this, targetNode:e}, function(e){
        	e.data.baseObj.setShow(e.data.targetNode);
        });
        
        e._c = dc.leftCalendar;
        e._c._nullInInput = true;
        e._c.enableListener(e.childNodes[e._ll ? 1 : 0].childNodes[0]);
        e._r = dc.rightCalendar;
        
        if (e._f != null) {
            e._c.setDateFormat(e._f)
        }
        if (!window.dhx4.s2b(h.enableTime)) {
            e._c.hideTime()
        }
        if (window.dhx4.s2b(h.enableTodayButton)) {
            e._c.showToday()
        }
        if (window.dhx4.s2b(h.showWeekNumbers)) {
            e._c.showWeekNumbers()
        }
        if (!isNaN(h.weekStart)) {
            e._c.setWeekStartDay(h.weekStart)
        }
        if (typeof(h.calendarPosition) != "undefined") {
            e._c.setPosition(h.calendarPosition)
        }
        if (h.minutesInterval != null) {
            e._c.setMinutesInterval(h.minutesInterval)
        }
        
        e._c._itemIdd = e._idd;
        
        const fromDateUpdate = function(m) {
        	if (e._value != m) {
                if (e.checkEvent("onBeforeChange")) {
                    if (e.callEvent("onBeforeChange", [e._idd, e._value, m]) !== true) {
                        return false
                    }
                }
                e._tempValue = e._value = m;
                c.setValue(e, m, false);
                e.callEvent("onChange", [this._itemIdd, e._value]);
                e._doubleCalendar.setDates(m, e._r.getDate());
            }
            
            return true
        }
        
        e._c.attachEvent("onBeforeChange", fromDateUpdate);
        e._c.attachEvent("onButtonClick", fromDateUpdate);
        
        e._r._itemIdd = e._idd;
        e._r._targetToItem = h.calendarTo;
        
        const toDateUpdate = function(m) {
        	e.callEvent("onChange", [this._itemIdd, e._value]);
        	e.getForm().setItemValue(e._r._targetToItem, m);
        	e._r.setDate(m);
        	e._doubleCalendar.setDates(e._c.getDate(), m);
        }
        
        e._r.attachEvent("onBeforeChange", toDateUpdate);
        e._r.attachEvent("onButtonClick", toDateUpdate);
        
        e.getForm().attachEvent("onFocus", function(){
        	var focusTarget = event.currentTarget;
        	var targetName = focusTarget.name;
        	if(e._r._targetToItem == targetName){
        		var targetNode = e.getForm()._getItemNode(targetName);
        		targetNode.inputArea = targetNode.childNodes[e._ll?1:0].childNodes[0]
        		targetNode.doubleCA = e.doubleCA;
        		targetNode._doubleCalendar = e._doubleCalendar
        		targetNode._c._show = null;
        		
        		e.show(targetNode);
        		
        		e.toNode = focusTarget;
        		$(e.toNode).on('blur', {base:e}, function(e){
        			e.data.base.doubleCA.classList.remove('show');
        		});
        	}
        });
        
        e.getForm().attachEvent("onChange", function(n){
        	if(e._r._targetToItem == n){
        		var toDate = e.getForm().getItemValue(n);
        		e._r.setDate(toDate)
        		e._doubleCalendar.setDates(e._c.getDate(), toDate);
        	}
        });
        
        
        e.getForm().attachEvent("onXLE", function(m){
        	var keyArr = Object.keys(m.dataMap||m);
        	for(var k in keyArr){
        		if(e._r._targetToItem == keyArr[k]){
        			var toDate = e.getForm().getItemValue(keyArr[k]);
        			e._r.setDate(toDate);
        			e._doubleCalendar.setDates(e._c.getDate(), toDate);
        		}
        	}
        });
        
        e._c.attachEvent("onClick", function() {
            e._tempValue = null
        });
        e._c.attachEvent("onHide", function() {
            e._tempValue = null
        });
        if (a) {
            e._c.base.onmousedown = function() {
                this._formMouseDown = true;
                return false
            }
        }
        
        this.setValue(e, h.value);
        g = null;
        
        var node = e.childNodes[e._ll ? 1 : 0].childNodes[0];
    	node.setAttribute("readOnly", "true");
        return this
    },
    getCalendar: function(a) {
        return a._c
    },
    setSkin: function(a, c) {
        a._c.setSkin(c)
    },
    setValue: function(c, e, a) {
        if (!e || e == null || typeof(e) == "undefined" || e == "") {
            c._value = null;
            c.childNodes[c._ll ? 1 : 0].childNodes[0].value = ""
        } else {
        	var date = (e instanceof Date ? e : c._c._strToDate(e, c._f0 || "%Y%m%d"));
        	if(date == "Invalid Date") date = (e instanceof Date ? e : c._c._strToDate(e, c._f0 ||Sepoa.info.DATE_FORMAT||c._c._dateFormat));
            c._value = date;
            c.childNodes[c._ll ? 1 : 0].childNodes[0].value = c._c._dateToStr(c._value, c._f ||Sepoa.info.DATE_FORMAT|| c._c._dateFormat)
        }
        if (a !== false && c._value != "Invalid Date") {
            c._c.setDate(c._value)
        }else if(a !== false && c._value == "Invalid Date"){
        	c._c.setDate();
        }
        
        if(c._r){
        	c._r._p = c; 
        }
    },
    getValue: function(c, a) {
        var e = c._tempValue || c._c.getDate();
        if (a === true && e == null) {
            return ""
        }
        return (a === true ? c._c._dateToStr(e, c._f0 || "%Y%m%d") : e)
    },
    setDateFormat: function(c, a, e) {
        c._f = a;
        c._f0 = (e || c._f);
        c._c.setDateFormat(c._f);
        this.setValue(c, this.getValue(c))
    },
    destruct: function(a) {
        a._c.disableListener(a.childNodes[a._ll ? 1 : 0].childNodes[0]);
        a._c.unload();
        a._c = null;
        try {
            delete a._c
        } catch (c) {}
        a._f = null;
        try {
            delete a._f
        } catch (c) {}
        a._f0 = null;
        try {
            delete a._f0
        } catch (c) {}
        a.childNodes[a._ll ? 1 : 0].childNodes[0]._idd = null;
        a.childNodes[a._ll ? 1 : 0].childNodes[0].onblur = null;
        this.d2(a);
        a = null
    },
    checkEnteredValue: function(a) {
    	var t = a.value;
    	var a = a.parentNode.parentNode;
        this.setValue(a, a._c.getDate() || t)
    },
    checkEnteredValue2: function(a) {
    	var t = a.value;
    	var a = a.parentNode.parentNode;
        this.setValue(a, t)
    },
    setShow : function(e){
    	if(!this.isReadonly(e)){
	    	e.doubleCA.style.top = '0px';
	    	e.doubleCA.style.left= '0px';
	    	e.doubleCA.classList.add('show');
	    	e._doubleCalendar.show();
	    	
	    	var ia = e.inputArea;
	    	var lt = sUtils.element.getOffset(ia, "module.over");
	    	
	    	if((lt.left + e.doubleCA.offsetWidth) > SUX.root.document.body.offsetWidth){
	    		lt.left = SUX.root.document.body.offsetWidth - e.doubleCA.offsetWidth; 
	    	}
	    	
	    	if((lt.top + e.doubleCA.offsetHeight+ia.offsetHeight) > SUX.root.document.body.offsetHeight){
	    		lt.top = SUX.root.document.body.offsetHeight - e.doubleCA.offsetHeight - ia.offsetHeight; 
	    	}
	    	
	    	e.doubleCA.style.top =(lt.top+ia.offsetHeight)+'px';
	    	e.doubleCA.style.left =lt.left+'px';
    	}
    }
};
(function() {
    for (var c in {
            doAddLabel: 1,
            doAddInput: 1,
            doUnloadNestedLists: 1,
            setText: 1,
            getText: 1,
            enable: 1,
            disable: 1,
            isEnabled: 1,
            setWidth: 1,
            isReadonly: 1,
            setFocus: 1,
            getInput: 1,
            setReadonly: 1
        }) {
        dhtmlXForm.prototype.items.doubleCalendar[c] = dhtmlXForm.prototype.items.input[c]
    }
})();
dhtmlXForm.prototype.items.doubleCalendar.doAttachChangeLS = dhtmlXForm.prototype.items.select.doAttachChangeLS;
dhtmlXForm.prototype.items.doubleCalendar.d2 = dhtmlXForm.prototype.items.input.destruct;
dhtmlXForm.prototype.items.button.render=function(c, e) {
    c._type = "bt";
    c._enabled = true;
    c._name = e.name;
    //c.className = String(c.className).replace("item_label_top", "item_label_left").replace("item_label_right", "item_label_left");
    c._doOnKeyUpDown = function(h, g) {
        this.callEvent(h, [this.childNodes[0].childNodes[0], g, this._idd])
    };
    c.innerHTML = '<div class="dhxform_btn" role="link" tabindex="0" dir="ltr"><div class="dhxform_btn_txt">' + e.value + '</div><div class="dhxform_btn_filler" disabled="true"></div></div>';
    if (!isNaN(e.width)) {
        var a = Math.max(e.width, 10);
        if (dhtmlXForm.prototype.items[this.t]._dim == null) {
            c.firstChild.style.width = a + "px";
            dhtmlXForm.prototype.items[this.t]._dim = c.getForm()._checkDim(c, c.firstChild)
        }
        c.firstChild.style.width = a - dhtmlXForm.prototype.items[this.t]._dim + "px";
        c.firstChild.firstChild.className += " dhxform_btn_txt_autowidth"
    }
    if (!isNaN(e.inputLeft)) {
        c.childNodes[0].style.left = parseInt(e.inputLeft) + "px"
    }
    if (!isNaN(e.inputTop)) {
        c.childNodes[0].style.top = parseInt(e.inputTop) + "px"
    }
    if (e.hidden == true) {
        this.hide(c)
    }
    if (e.disabled == true) {
        this.userDisable(c)
    }
    if (typeof(e.tooltip) != "undefined") {
        c.firstChild.title = e.tooltip
    }
    c.onselectstart = function(g) {
        g = g || event;
        g.cancelBubble = true;
        if (g.preventDefault) {
            g.preventDefault()
        } else {
            g.returnValue = false
        }
        return false
    };
    c.firstChild.onselectstart = function(g) {
        g = g || event;
        g.cancelBubble = true;
        if (g.preventDefault) {
            g.preventDefault()
        } else {
            g.returnValue = false
        }
        return false
    };
    c.firstChild.onkeypress = function(g) {
        g = g || event;
        if ((g.keyCode == 32 || g.charCode == 32 || g.keyCode == 13 || g.charCode == 13) && !this.parentNode._busy) {
            this.parentNode._busy = true;
            g.cancelBubble = true;
            if (g.preventDefault) {
                g.preventDefault()
            } else {
                g.returnValue = false
            }
            _dhxForm_doClick(this.childNodes[0], ["mousedown", "mouseup"]);
            return false
        }
    };
    c.firstChild.onfocus = function() {
        this.parentNode._doOnFocus(this.parentNode)
    };
    c.firstChild.onblur = function() {
        _dhxForm_doClick(this.childNodes[0], "mouseout");
        this.parentNode._doOnBlur(this.parentNode)
    };
    c.firstChild.onkeyup = function(g) {
        this.parentNode._doOnKeyUpDown("onKeyUp", g || event)
    };
    c.firstChild.onkeydown = function(g) {
        this.parentNode._doOnKeyUpDown("onKeyDown", g || event)
    };
    c.firstChild.onmouseover = function() {
        var g = this.parentNode;
        if (!g._enabled) {
            return
        }
        this._isOver = true;
        this.className = "dhxform_btn dhxform_btn_over";
        g = null
    };
    c.firstChild.onmouseout = function() {
        var g = this.parentNode;
        if (!g._enabled) {
            return
        }
        this.className = "dhxform_btn";
        this._allowClick = false;
        this._pressed = false;
        this._isOver = false;
        g = null
    };
    c.firstChild.ontouchstart = c.firstChild.onmousedown = function(h) {
        h = h || event;
        if (h.type == "touchstart" && h.preventDefault) {
            h.preventDefault()
        }
        if (h.button >= 2) {
            return
        }
        if (this._pressed) {
            return
        }
        var g = this.parentNode;
        if (!g._enabled) {
            return
        }
        this.className = "dhxform_btn dhxform_btn_pressed";
        this._allowClick = true;
        this._pressed = true;
        g = null
    };
    c.firstChild.ontouchend = c.firstChild.onmouseup = function(h) {
        h = h || event;
        if (h.button >= 2) {
            return
        }
        if (!this._pressed) {
            return
        }
        var g = this.parentNode;
        if (!g._enabled) {
            return
        }
        g._busy = false;
        this.className = "dhxform_btn" + (this._isOver ? " dhxform_btn_over" : "");
        if (this._pressed && this._allowClick) {
            g.callEvent("_onButtonClick", [g._name, g._cmd])
        }
        this._allowClick = false;
        this._pressed = false;
        g = null
    };
    c._doOnFocus = function(g) {
        g.getForm().callEvent("onFocus", [g._idd])
    };
    c._doOnBlur = function(g) {
        g.getForm().callEvent("onBlur", [g._idd])
    };
    return this
}
dhtmlXForm.prototype.items.image = {
    _dimFix: true,
    render: function(l, m, o) {
        l._type = "image";
        l._enabled = true;
        l._fr_name = "dhxform_image_" + window.dhx4.newId();
        l._url = (typeof(m.url) == "undefined" || m.url == null ? "" : m.url);
        if (m.inputWidth == "auto") {
            m.inputWidth = 120
        }
        if (m.inputHeight == "auto") {
            m.inputHeight = m.inputWidth
        }
        this.doAddLabel(l, m);
        this.doAddInput(l, m, "DIV", null, true, true, "dhxform_image");
        var c = l.childNodes[l._ll ? 1 : 0].childNodes[0];
        c.classList.add("xi-sepoa-dummy");
        c.style.height = parseInt(c.style.height) - dhtmlXForm.prototype.items[this.t]._dim + "px";
        var a = (typeof(m.imageWidth) != "undefined" ? parseInt(m.imageWidth) : m.inputWidth);
        var e = (typeof(m.imageHeight) != "undefined" ? parseInt(m.imageHeight) : m.inputHeight);
        if (e == "auto") {
            e = a
        }
        l._dim = {
            mw: m.inputWidth - this._dim,
            mh: m.inputHeight - this._dim,
            w: a,
            h: e
        };
        c.innerHTML = "<img class='dhxform_image_img' border='0' style='visibility:hidden;'><iframe name='" + l._fr_name + "' style='position: absolute; width:0px; height:0px; top:-10px; left:-10px;' frameBorder='0' border='0'></iframe><div class='dhxform_image_wrap'><form action='" + l._url + "' method='POST' enctype='multipart/form-data' target='" + l._fr_name + "' class='dhxform_image_form'><input type='hidden' name='action' value='uploadImage'><input type='hidden' name='itemId' value='" + l._idd + "'><input type='text' name='" + l._idd + "' class='dhxform_image_input'></form></div>";
        this.adjustImage(l);
        c.childNodes[2].firstChild.lastChild.onchange = function() {
            l._is_uploading = true;
            this.parentNode.submit();
            this.parentNode.parentNode.className = "dhxform_image_wrap dhxform_image_in_progress";
            this.value = ""
        };
        c.childNodes[2].firstChild.lastChild.onclick = function(){
        	o.callEvent("onButtonClick", [m.name, o]);
        }
        let g = this;
        if (window.navigator.userAgent.indexOf("MSIE") >= 0) {
            c.childNodes[1].onreadystatechange = function() {
                if (this.readyState == "complete") {
                	if(g != null){
                		g.doOnUpload(l)
                	}
                }
            }
        } else {
            c.childNodes[1].onload = function() {
            	if(g != null){
            		g.doOnUpload(l)
            	}
            }
        }
        this._moreClear = function() {
            g = null
        };
        this.setValue(l, m.value || "");
        c = null;
        return this
    },
    destruct: function(c) {
        var a = c.childNodes[c._ll ? 1 : 0].childNodes[0];
        a.childNodes[2].firstChild.lastChild.onchange = null;
        a.childNodes[1].onreadystatechange = null;
        a.childNodes[1].onload = null;
        this._moreClear();
        this.d2(c);
        c = null
    },
    doAttachEvents: function() {},
    setValue: function(e, h) {
    	if(!e._url){
        	return;
        }
        e._value = (h == null ? "" : h);
        e.childNodes[e._ll ? 1 : 0].getElementsByClassName('dhxform_image_input')[0].value = e._value;
        var c = e._url + (e._url.indexOf("?") >= 0 ? "&" : "?") + "action=loadImage&itemId=" + encodeURIComponent(e._idd) + "&itemValue=" + encodeURIComponent(e._value);
        var g = e.childNodes[e._ll ? 1 : 0].childNodes[0].firstChild;
        // if (g.nextSibling.tagName.toLowerCase() == "img") {
        // g.nextSibling.src = c
        // } else {
        // var a = document.createElement("IMG");
        // a.className = "dhxform_image_img";
        // a.style.visibility = "hidden";
        // a.onload = function() {
        // this.style.visibility = "visible";
        // this.parentNode.removeChild(this.nextSibling);
        // this.onload = this.onerror = null
        // };
        // a.onerror = function() {
        // this.onload.apply(this, arguments);
        // this.style.visibility = "hidden"
        // };
        // g.parentNode.insertBefore(a, g);
        // a.src = c;
        // a = null;
        // this.adjustImage(e)
        // }
        g.src = c;
        g.style.visibility = (e._url.length > 0 && e._value.length > 0)?"visible":"hidden";
        this.adjustImage(e)
        // g = null
    },
    getValue: function(a) {
        return a._value
    },
    doOnUpload: function(e) {
        if (e._is_uploading == true) {
            var a = e.childNodes[e._ll ? 1 : 0].childNodes[0].lastChild.previousSibling;
            var c = dhx4.s2j(a.contentWindow.document.body.innerHTML);
            if (typeof(c) == "object" && c != null && c.state == true && c.itemId == e._idd) {
                this.setValue(e, c.itemValue, true);
                e.getForm().callEvent("onImageUploadSuccess", [c.itemId, c.itemVaule, c.extra])
            } else {
                e.getForm().callEvent("onImageUploadFail", [e._idd, (c ? c.extra : null)])
            }
            c = a = null;
            window.setTimeout(function() {
                e.childNodes[e._ll ? 1 : 0].childNodes[0].lastChild.className = "dhxform_image_wrap";
                e._is_uploading = false
            }, 50)
        }
    },
    adjustImage: function(g) {
        var c = g.childNodes[g._ll ? 1 : 0].childNodes[0].firstChild;
        var a = Math.min(g._dim.mw, g._dim.w);
        var e = Math.min(g._dim.mh, g._dim.h);
        c.style.width = a + "px";
        c.style.height = e + "px";
        c.style.marginLeft = Math.max(0, Math.round(g._dim.mw / 2 - a / 2)) + "px";
        c.style.marginTop = Math.max(0, Math.round(g._dim.mh / 2 - e / 2)) + "px";
        c = g = null
    }
};
(function() {
    for (var c in dhtmlXForm.prototype.items.input) {
        if (!dhtmlXForm.prototype.items.image[c]) {
            dhtmlXForm.prototype.items.image[c] = dhtmlXForm.prototype.items.input[c]
        }
    }
})();
dhtmlXForm.prototype.items.image.d2 = dhtmlXForm.prototype.items.input.destruct;
(function() {
    for (var c in dhtmlXForm.prototype.items) {
        if (!dhtmlXForm.prototype.items[c]._getItemNode) {
            dhtmlXForm.prototype.items[c]._getItemNode = function(a) {
                return a
            }
        }
    }
})();


dhtmlXForm.prototype.items.fieldset.render=function(c, g) {
    c._type = "fs";
    if (typeof(parseInt(g.inputWidth)) == "number") {}
    c._width = g.width;
    c._enabled = true;
    c._checked = true;
    c.className = "fs_" + g.position + (typeof(g.className) == "string" ? " " + g.className : "");
    var e = document.createElement("FIELDSET");
    e.className = "dhxform_fs";
    var h = String(g.labelAlign).replace("align_", "");
    e.innerHTML = "<legend class='fs_legend' align='" + h + "' style='text-align:" + h + ";display:"+((g.label == null || g.label=="" || g.label=="#NO_LABEL#")?"none":"inherit")+"'>" + g.label + "</legend>";
    c.appendChild(e);
    if (!isNaN(g.inputLeft)) {
        e.style.left = parseInt(g.inputLeft) + "px"
    }
    if (!isNaN(g.inputTop)) {
        e.style.top = parseInt(g.inputTop) + "px"
    }
    if (g.inputWidth != "auto") {
        if (!isNaN(g.inputWidth)) {
            e.style.width = parseInt(g.inputWidth) + "px";
            var a = parseInt(e.style.width);
            if (e.offsetWidth > a) {
                e.style.width = a + (a - e.offsetWidth) + "px"
            }
        }
    }
    c._addSubListNode = function() {
        var l = document.createElement("DIV");
        l._custom_css = " dhxform_fs_nested";
        this.childNodes[0].appendChild(l);
        return l
    };
    if (g.hidden == true) {
        this.hide(c)
    }
    if (g.disabled == true) {
        this.userDisable(c)
    }
    return this
}
dhtmlXForm.prototype.items.colorpicker.render=function(e, g){
	 var c = this;
     e._type = "colorpicker";
     e._enabled = true;
     this.doAddLabel(e, g);
     this.doAddInput(e, g, "INPUT", "TEXT", true, true, "dhxform_textarea");
     e._value = (g.value || "");
     e.childNodes[e._ll ? 1 : 0].childNodes[0].value = e._value;
     var a = {
         input: e.childNodes[e._ll ? 1 : 0].childNodes[0],
         custom_colors: (window.dhx4.s2b(g.enableCustomColors) == true),
         skin: e.getForm().skin
     };
     var conf = {
    	lang: Sepoa.info.LANGUAGE.toLowerCase()
     };
     this.colorpicker[e._idd] = new SepoaDhtmlXColorPicker(a, conf);
     this.colorpicker[e._idd]._nodes[0].valueColor = null;
     this.colorpicker[e._idd].base.className += " dhtmlxcp_in_form";
     if (typeof(g.customColors) != "undefined") {
         this.colorpicker[e._idd].setCustomColors(g.customColors)
     }
     if (typeof(g.cpPosition) == "string") {
         this.colorpicker[e._idd].setPosition(g.cpPosition)
     }
     this.colorpicker[e._idd].attachEvent("onSelect", function(h) {
         if (e._value != h) {
             if (e.checkEvent("onBeforeChange")) {
                 if (e.callEvent("onBeforeChange", [e._idd, e._value, h]) !== true) {
                     e.childNodes[e._ll ? 1 : 0].childNodes[0].value = e._value;
                     return
                 }
             }
             e._value = h;
             c.setValue(e, h);
             e.childNodes[e._ll ? 1 : 0].childNodes[0].value = h;
             e.callEvent("onChange", [e._idd, e._value])
         }
     });
     this.colorpicker[e._idd].attachEvent("onHide", function(h, f) {
         var l = e.childNodes[e._ll ? 1 : 0].childNodes[0];
         if ((l.value != e._value) || f !== true) {
             l.value = e._value
         } else {
        	 l.value = '';
        	 e._value = l.value;
        	 e.callEvent('onChange', [e._idd, e._value]);
         }
         l = null
     });
	 a.input.onchange = function(name, value) {
		 e.getForm().callEvent("onChange", [e._idd]);
	 }
     e.childNodes[e._ll ? 1 : 0].childNodes[0]._idd = e._idd;
     return this
}
dhtmlXForm.prototype.items.colorpicker.setValue= function(e, g) {
    e._value = (typeof(g) != "undefined" && g != null ? g : "");
    var c = (String(e._value) || "");
    var a = e.childNodes[e._ll ? 1 : 0].childNodes[0];
    if (e._df != null) {
        c = window.dhx4.template._getFmtValue(c, e._df)
    }
    this.colorpicker[e._idd].setColor(g);
    if (a.value != c) {
        a.value = c;
        e.getForm()._ccReload(e._idd, c)
    }
    a = null
}

dhtmlXForm.prototype.items.editor.render = function(e, g) {
    var a = (!isNaN(g.rows));
    e._type = "editor";
    e._enabled = true;
    e._editor_id = e.getForm().idPrefix + e._idd;
    this.doAddLabel(e, g);
    this.doAddInput(e, g, "DIV", null, true, true, "dhxform_item_template");
    e._value = (g.value || "");
    var c = this;
    this.editor[e._editor_id] = new dhtmlXEditor({
        parent: e.childNodes[e._ll ? 1 : 0].childNodes[0],
        content: e._value,
        iconsPath: g.iconsPath,
        toolbar: g.toolbar,
        skin: e.getForm().skin
    });
    this.editor[e._editor_id].setContent=this.setContent = function(n) {
        n = n || "";
        if (this.edDoc.body) {
        	n = sUtils.string.getSecureString(n || "");
            var m = false;
            if (window.dhx4.isFF) {
                var l = navigator.userAgent.match(/Firefox\/(\d*)/);
                m = (l != null && l[1] < 28)
            }
            if (m) {
                if (typeof(this.conf.ffTest) == "undefined") {
                    this.editor.contentWindow.document.body.innerHTML = "";
                    this._runCommand("InsertHTML", "test");
                    this.conf.ffTest = (this.editor.contentWindow.document.body.innerHTML.length > 0)
                }
                if (this.conf.ffTest) {
                    this.editor.contentWindow.document.body.innerHTML = n
                } else {
                    this.editor.contentWindow.document.body.innerHTML = "";
                    if (n.length == 0) {
                        n = " "
                    }
                    this._runCommand("InsertHTML", n)
                }
            } else {
                this.editor.contentWindow.document.body.innerHTML = n
            }
            this.callEvent("onContentSet", [])
        } else {
            if (!this.conf.firstLoadEv) {
                this.conf.firstLoadEv = true;
                this.conf.firstLoadData = n;
                this._onFirstLoad = function() {
                    c.setContent(c.conf.firstLoadData);
                    if (typeof(window.addEventListener) == "function") {
                        c.edWin.removeEventListener("load", c._onFirstLoad, false)
                    } else {
                        c.edWin.detachEvent("onload", c._onFirstLoad)
                    }
                    c.conf.firstLoadData = null;
                    c.conf.firstLoadEv = false;
                    c._onFirstLoad = null
                };
                if (typeof(window.addEventListener) == "function") {
                    this.edWin.addEventListener("load", this._onFirstLoad, false)
                } else {
                    this.edWin.attachEvent("onload", this._onFirstLoad)
                }
            }
        }
    };
    this.editor[e._editor_id].attachEvent("onAccess", function(l, m) {
        e.callEvent("_onBeforeEditorAccess", []);
        _dhxForm_doClick(document.body, "click");
        if (l == "blur") {
            c.doOnBlur(e, this);
            e.callEvent("onBlur", [e._idd]);
            if ({
                    dhx_terrace: 1,
                    material: 1
                }[e.getForm().skin] == 1) {
                var h = e.childNodes[e._ll ? 1 : 0].className;
                if (h.indexOf("dhxeditor_focus") >= 0) {
                    e.childNodes[e._ll ? 1 : 0].className = (h).replace(/\s{0,}dhxeditor_focus/gi, "")
                }
            }
        } else {
            e.callEvent("onEditorAccess", [e._idd, l, m, this, e.getForm()]);
            e.callEvent("onFocus", [e._idd]);
            if ({
                    dhx_terrace: 1,
                    material: 1
                }[e.getForm().skin] == 1) {
                var h = e.childNodes[e._ll ? 1 : 0].className;
                if (h.indexOf("dhxeditor_focus") == -1) {
                    e.childNodes[e._ll ? 1 : 0].className += " dhxeditor_focus"
                }
            }
        }
    });
    this.editor[e._editor_id].attachEvent("onToolbarClick", function(h) {
        e.callEvent("onEditorToolbarClick", [e._idd, h, this, e.getForm()])
    });
    if (g.readonly) {
        this.setReadonly(e, true)
    }
    e.childNodes[e._ll ? 0 : 1].childNodes[0].removeAttribute("for");
    e.childNodes[e._ll ? 0 : 1].childNodes[0].onclick = function() {
        c.editor[e._editor_id]._focus()
    };
    return this
}

dhtmlXForm.prototype.items.template.render = function(c, e) {
    var a = (!isNaN(e.rows));
    c._type = "tp";
    c._enabled = true;
    if (e.format != null) {
        if (typeof(e.format) == "function") {
            c.format = e.format
        } else {
            if (typeof(e.format) == "string" && typeof(window[e.format]) == "function") {
                c.format = window[e.format]
            }
        }
    }
    if (c.format == null) {
        c.format = function(g, h) {
            const temp = document.createElement('div');
            temp.appendChild(document.createTextNode(h));
            const label = temp.innerHTML;
            return label;
        }
    }
    this.doAddLabel(c, e);
    this.doAddInput(c, e, "DIV", null, true, true, "dhxform_item_template");
    this.setValue(c, e.value || "");
    return this
}

dhtmlXForm.prototype.items.password.doAddInput=function(w, g, c, o, r, h, l) {
	var a = document.createElement("DIV");
    a.className = "dhxform_control";
    if (w._ll) {
        w.appendChild(a)
    } else {
        w.insertBefore(a, w.firstChild)
    }
    var v = document.createElement(c);
    v.className = l;
    v.name = w._idd;
    v._idd = w._idd;
    v.id = g.uid;
    if (typeof(o) == "string") {
        v.type = o
    }
    if (c == "INPUT" || c == "TEXTAREA") {
        v.onkeyup = function(u) {
            u = u || event;
            w.callEvent("onKeyUp", [this, u, this._idd])
        };
        v.onkeydown = function(u) {
            u = u || event;
            w.callEvent("onKeyDown", [this, u, this._idd])
        }
    }
    if (c == "SELECT" && g.type == "select" && w.getForm().skin == "material") {
        if (window.dhx4.isOpera || window.dhx4.isChrome) {
            v.className += " dhxform_arrow_fix_webkit"
        } else {
            if (window.dhx4.isEdge) {
                v.className += " dhxform_arrow_fix_edge"
            } else {
                if (window.dhx4.isFF) {
                    v.className += " dhxform_fix_ff"
                }
            }
        }
    }
    a.appendChild(v);
    if (r) {
        if (!isNaN(g.inputLeft)) {
            a.style.left = parseInt(g.inputLeft) + "px"
        }
        if (!isNaN(g.inputTop)) {
            a.style.top = parseInt(g.inputTop) + "px"
        }
    }
    var s = "";
    var m = false;
    if (h) {
        if (!isNaN(g.inputWidth)) {
            s += "width:" + parseInt(g.inputWidth) + "px;";
            m = true
        }
        if (!isNaN(g.inputHeight)) {
            s += "height:" + parseInt(g.inputHeight) + "px;"
        }
    }
    if (typeof(g.style) == "string") {
        s += g.style
    }
    v.style.cssText = s;
    if (g.maxLength) {
        v.setAttribute("maxLength", g.maxLength)
    }
    if (g.connector) {
        v.setAttribute("connector", g.connector)
    }
    var e = (dhtmlXForm.prototype.items[this.t] != null ? dhtmlXForm.prototype.items[this.t]._dimFix == true : false);
    if (m && ({
            input: 1,
            password: 1,
            select: 1,
            multiselect: 1,
            calendar: 1,
            colorpicker: 1
        }[this.t] == 1 || e)) {
        if (dhtmlXForm.prototype.items[this.t]._dim == null) {
            dhtmlXForm.prototype.items[this.t]._dim = w.getForm()._checkDim(a, v)
        }
        v.style.width = parseInt(v.style.width) - dhtmlXForm.prototype.items[this.t]._dim + "px"
    }
    if (typeof(g.note) == "object") {
        var n = document.createElement("DIV");
        n.className = "dhxform_note";
        n.style.width = (isNaN(g.note.width) ? v.offsetWidth : parseInt(g.note.width)) + "px";
        n._w = g.note.width;
        n.innerHTML = g.note.text;
        a.appendChild(n);
        n = null
    }
    if (g.readonly) {
        this.setReadonly(w, true)
    }
    if (g.disabled == true) {
        this.userDisable(w)
    }
    if (g.hidden == true && this.t != "combo") {
        this.hide(w)
    }
    
    if(!document.body.querySelector('div.browser-idpw-autofill-off')){
    	const dummy = document.body.insertBefore(sUtils.element.create(`<div class="browser-idpw-autofill-off" style="display:none;"><input type="text"/><input type="password"/></div>`), document.body.firstChild);
    	dummy._removeFillFn = setInterval(()=>{
    		if(dummy.children[1].value.length > 0){
	    		dummy.children[0].value = "";
	    		dummy.children[1].value = "";
	    		clearInterval(dummy._removeFillFn);
    		}
    	}, 100);
    }
}

function SepoaDhtmlXFormObject(h, g, pObj){
	this.beforeSelectItem = [];
	dhtmlXForm.call(this, h, g);
	
	let blockDirection = g?.[0]?.flexDirection;
	if(["row", "column"].includes(blockDirection)){
		this.setBlockDirection(blockDirection);
	}
	
	for(var c in this.items){
        this.items[c]._validate = function(n) {
            if (!n._validate) {
                return true
            }
            if (n._type == "ch" || n._type == "ra") {
                var u = (this.isChecked(n) ? this.getValue(n) : 0);
                if (n._type == "ra" && typeof(u) == "undefined") {
                    u = 0
                }
            } else if(!this.getValue){
            	return true
            } else {
            	var u = this.getValue(n)
            }
            var m = true;
            for (var s = 0; s < n._validate.length; s++) {
                var a = "is" + n._validate[s];
                if ((u == null || u.length == 0) && a != "isNotEmpty" && n._type != "container") {} else {
                    var o = dhtmlxValidation[a];
                    if (n._type == "container" && typeof(o) == "function") {
                        o = function() {
                            return true
                        }
                    }
                    if (typeof(o) != "function" && typeof(n._validate[s]) == "function") {
                        o = n._validate[s]
                    }
                    if (typeof(o) != "function" && typeof(window[n._validate[s]]) == "function") {
                        o = window[n._validate[s]]
                    }
                    m = ((typeof(o) == "function" ? o(u, n._idd) : new RegExp(n._validate[s]).test(u)) && m);
                    o = null
                }
            }
            if (!(n.callEvent("onValidate" + (m ? "Success" : "Error"), [n._idd, u, m]) === false)) {
                n.getForm().setValidateCss(n._idd, m)
            }
            return m
        }
	}
	this.cont.formObj = this;
	this.cont.parentObj = pObj;
	this.form_id = h;
	this._ajaxStatusW = {};
	this.setViewMode = function(s) {
        this.forEachItem(function(name,value){
        	try {
        		var type = this.getItemType(name);
        		if(type == 'select' || type == 'autocomplete'){
        			if(s){
        				this.disableItem(name);
        			}else{
        				this.enableItem(name);
        			}
        		}else if(type == 'radio'){
        			if(s){
        				this.disableItem(name,value);
        			}else{
        				this.enableItem(name,value);
        			}
        		}else if(type == 'btn2state'){
        			Boolean(s) ? this.disableItem(name) : this.enableItem(name);
        		}else{
        			this.setReadonly(name, s);
        		}
        	}catch(o){}
        }.bind(this));
    };
    this.setItemValue = function(m, a, flag) {
        this.resetValidateCss(m, a);
        
        if (this.getItemType(m) == "radio") {
            if (this._getRGroup(m, a) != null) {
                this.checkItem(m, a)
            } else {
                this.uncheckItem(m, this.getCheckedValue(m))
            }
            return null
        }else if(this.getItemType(m) == "label"){
        	this.setItemLabel(m, a);
        	return null
        }else if(this.getItemType(m) == "btn2state"){
        	(typeof(a)=='boolean'? a : ((a == 'true' || a == '1'|| a == 'Y') ? true : false)) ? this.checkItem(m) : this.uncheckItem(m);
        }
        // 셀렉트박스 로드전 입력시 저장용
        if(this.getItemType(m) == "select"){
        	this.beforeSelectItem[m] = a;
        }
        var t = this.doWithItem(m, "setValue", a);
        if(this.getItemType(m) == "calendar"){
        	a = this.getItemValue(m); 
        }
        if(!flag){
        	this.callEvent('onChange', [m, a]);
        }
        return t;
    };
    this.attachEvent('onChange', function(m, a){
    	if(this.getItemType(m) == "select"){
    		this.beforeSelectItem[m] = a;
    	}
    })
    // 콤보박스 로드
    this._loadComboBox();
    this.attachEvent('onBlur', this.onBlur);
    
    this.cont.onkeypress = function(m) {
        m = (m || event);
        if (m.keyCode == 13) {
            var a = (m.target || m.srcElement);
            if (typeof(a.tagName) != "undefined" && String(a.tagName).toLowerCase() == "textarea" && !m.ctrlKey) {
                return
            }
            if(a.name){
            	base.getItemValue(a.name);
            }
            base.callEvent("onEnter", []);
        }
    }
    
    this.clear = function() {
        var m = {};
        this.formId = (new Date()).valueOf();
        this.resetDataProcessor("inserted");
        for (var n in this.itemPull) {
            var o = this.itemPull[n]._idd;
            if (this.itemPull[n]._type in {
                    ta: 1,
                    editor: 1,
                    calendar: 1,
                    pw: 1,
                    hd: 1,
                    vault: 1,
                    colorpicker: 1
                }) {
                this.setItemValue(o, "")
            }else if (this.itemPull[n]._type == "ch") {
                this.uncheckItem(o)
            }else if (this.itemPull[n]._type == "combo") {
                this.itemPull[n]._apiChange = true;
                var v = this.getCombo(o);
                v.selectOption(0);
                v = null;
                this.itemPull[n]._apiChange = false
            }else if (this.itemPull[n]._type == "autocomplete") {
                this.itemPull[n]._apiChange = true;
                var v = this.getCombo(o);
                v.clearAll();
	            v.setComboText("");
                this.itemPull[n]._apiChange = false
            }else if (this.itemPull[n]._type == "se") {
                var s = this.getOptions(o);
                if (s.length > 0) {
                    s[0].selected = true
                }
            }else if (this.itemPull[n]._type == "ra") {
                var r = this.itemPull[n]._group;
                if (!m[r]) {
                    this.checkItem(r, this.doWithItem(o, "_getFirstValue"));
                    m[r] = true
                }
            }else if (this.itemPull[n]._list) {
                for (var u = 0; u < this.itemPull[n]._list.length; u++) {
                    this.itemPull[n]._list[u].clear()
                }
            }else if (this["setFormData_" + this.itemPull[n]._type]) {
                this["setFormData_" + this.itemPull[n]._type](o, "")
            }
        }
        m = null;
        if (this._parentForm) {
            this._autoCheck()
        }
        this.resetValidateCss()
    };
}
SepoaDhtmlXFormObject.prototype = Object.create(dhtmlXForm.prototype);
SepoaDhtmlXFormObject.prototype.constructor = dhtmlXForm;
SepoaDhtmlXFormObject.prototype.setBlockDirection=function(type){
	this.cont.classList.remove("block_flex_wrap");
	this.cont.classList.remove("flex_direction_row");
	this.cont.classList.remove("flex_direction_column");
	
	if(["row", "column"].includes(type)){
		this.cont.classList.add('block_flex_wrap');
		this.cont.classList.add(`flex_direction_${type}`);
	}
}
SepoaDhtmlXFormObject.prototype.onBlur = function(name){
	if(this.getItemType(name) == 'calendar'){
		this.getCalendar(name).hide();
	}
}
SepoaDhtmlXFormObject.prototype.setRequired = function(n, l, h) {
    if (typeof(h) == "undefined") {
        h = l
    } else {
        n = [n, l]
    }
    var e = this._getItemNode(n);
    if (!e) {
        return
    }
    h = window.dhx4.s2b(h);
    e._required = (h == true);
    if (e._required) {
        if (!e._validate) {
            e._validate = []
        }
        var c = false;
        for (var g = 0; g < e._validate.length; g++) {
            c = (e._validate[g] == "NotEmpty" || c)
        }
        if (!c) {
            e._validate.push("NotEmpty")
        }
        var m = e.childNodes[e._ll ? 0 : 1].childNodes[0];
        if (!(m.lastChild && m.lastChild.className && m.lastChild.className.search(/required/) >= 0)) {
            var a = document.createElement("SPAN");
            e.classList.add("dhxform_custom_required");
            
            if(m.childNodes.length > 0){
            	m.insertBefore(a, m.childNodes[0]);
            }else{
            	m.appendChild(a);
            }
            a = m = null
        }
    } else {
        if (e._validate != null) {
            var c = e._validate;
            e._validate = [];
            for (var g = 0; g < c.length; g++) {
                if (c[g] != "NotEmpty") {
                    e._validate.push(c[g])
                }
            }
            if (e._validate.length == 0) {
                e._validate = null
            }
        }
        e.classList.remove("dhxform_custom_required");
        var m = e.childNodes[e._ll ? 0 : 1].childNodes[0];
        if (m.lastChild && m.lastChild.className && m.lastChild.className.search(/required/) >= 0) {
            m.removeChild(m.lastChild);
            m = null
        }
    }
    this._resetValidateCss(e);
    e = null
};
SepoaDhtmlXFormObject.prototype._loadComboBox = function(){
	if(!this._ajaxStatusW.combo) this._ajaxStatusW.combo = 0;
	var obj = this;
	this.forEachItem(function(name){
		obj.reloadComboBox(name, obj.getUserData(name,'combo').split(','))
    });
};

SepoaDhtmlXFormObject.prototype.reloadComboBox = function(id,params){
	var form_id = this.form_id
	if(this.getItemType(id) == 'select'){
		if(typeof(params)=='undefined' || params==null || params.length < 1 || params[0].trim() == ""){
			return;
		}
		//this._combo Sepoa.cache.combo.get(code+","+param);
		
		this._ajaxStatusW.combo++;
		setTimeout(function(id, params){
			var code = params[0];
			var param = window.dhx4.comboParam((typeof(params[1])=='undefined' || params[1]==null)?'':params[1]);
			params[2] = window.dhx4.comboParam((typeof(params[2])=='undefined' || params[2]==null)?'':params[2]);
			var selectbox_name = id;
			var default_value = (!this.beforeSelectItem[id])?(params[2]+"" || ''):this.beforeSelectItem[id];
			
			var data = Sepoa.cache.combo.get(code+","+param);
			
			var s = this.getSelect(id);
			if(typeof(data) != 'undefined' && data != null){
				s.innerHTML = "";
		        s.add(new Option(" ", ""))
		        for(var _idx in data){
		            s.add(new Option(data[_idx].text, data[_idx].value))
		        }
		        this.setItemValue(id, default_value || "", true);
				this.callEvent('onComboLoaded', [id, this.getItemValue(id)]);
			
			}else{
				
				s.innerHTML = "";
		        s.add(new Option(" ", ""))
			}
			if(this._checkAjaxStatus('combo') && this.cont.parentObj){
				this.cont.parentObj._checkAjaxStatus('combo');
			}
		}.bind(this), 0, id, params);
	}
}
SepoaDhtmlXFormObject.prototype._checkAjaxStatus=function(type){
	if(type)
		this._ajaxStatusW[type] -= 1;
	var flag = true;
	for(var _idx in this._ajaxStatusW){
		if(this._ajaxStatusW[_idx] != 0){
			flag = false;
			break;
		}
	}
	return flag;
}

SepoaDhtmlXFormObject.prototype.createModifyButton=function(id){
	var btn = document.createElement('div');
	btn.className="sepoa_modify_button form_modify_button";
	this.cont.appendChild(btn);
	btn.onclick=function(){
		var houseCode = Sepoa.info.HOUSE_CODE;
		var url = void 0;
		
		if(houseCode == '000') {
			url = window.CONTEXT_NAME+'/Admin/FormManagement/AdminFormManagementPopupLayout.sux';
		} else {
			url = window.CONTEXT_NAME+'/Admin/FormFilterManagement/AdminFormFilterManagementPopupLayout.sux';
		}
		
		PopupOpenPostReal(url
				, window.screen.width
				, window.screen.height
				, id || this.parentElement.id
				, {form_id:id}
				, true);
	}
	btn.style.display="none";
}
SepoaDhtmlXFormObject.prototype.setNote = function(h, e, a) {
    if (typeof(a) == "undefined") {
        a = e
    } else {
        h = [h, e]
    }
    var c = this._getItemNode(h);
    if (!c) {
        return
    }
    var g = this._getNoteNode(c);
    if (!g) {
        if (!a.width) {
            a.width = c.childNodes[c._ll ? 1 : 0].childNodes[0].offsetWidth
        }
        g = document.createElement("DIV");
        g.className = "dhxform_note";
        if ({
                ch: 1,
                ra: 1
            }[c._type]) {
            c.childNodes[c._ll ? 1 : 0].insertBefore(g, c.childNodes[c._ll ? 1 : 0].lastChild)
        } else {
            c.childNodes[c._ll ? 1 : 0].appendChild(g)
        }
        c.childNodes[c._ll ? 1 : 0].classList.add("dhxform_note_included");
    }
    g.innerHTML = a.text;
    if (a.width != null) {
        g.style.width = a.width + "px";
        g._w = a.width
    }
    g = null
};
SepoaDhtmlXFormObject.prototype.clearNote = function(g, c) {
    if (typeof(c) != "undefined") {
        g = [g, c]
    }
    var a = this._getItemNode(g);
    if (!a) {
        return
    }
    var e = this._getNoteNode(a);
    if (e != null) {
        e.parentNode.removeChild(e);
        e.parentNode.classList.remove("dhxform_note_included");
        e = null
    }
};
// 멀티폼제어
 function SepoaMultiDhtmlXFormObject(h, p, g, f, fid){
	this._formList = {};
	this.form_id = fid;
	if(typeof h == "object"){
		this._parent = h;
	}else{
		this._parent = document.getElementById(h);
	}
	appendClassName(this._parent,'form_area');
	this._maxHeight = 0;
	this._event = {};
	this._variable = p;
	this._hfFlag = f;
	
	for(var _idx in g){
		var id = p+'_'+_idx;
		
		var option = g[_idx]; 
		if(Array.isArray(option)){
			this.attachForm(id, option, f);
		}else{
			var fid = option.id;
			option = option.option;
			var form = this.attachForm(id, option, f, fid);
			if(Sepoa.common.DEV_FLAG){
				form.createModifyButton(fid);
			}
		}
	}
	
}
SepoaMultiDhtmlXFormObject.prototype.reloadComboBox = function(id, params){
	var obj = this;
	this.forEachObject(function(formId){
		obj.getForm(formId).reloadComboBox(id, params);
		var i = obj.getForm(formId);
		if(i.isItem(id)){
			i.reloadComboBox(id,params);
		}
	});
};
SepoaMultiDhtmlXFormObject.prototype.attachForm = function(id, g, f, fid){
	var tmp = document.createElement('div');
	tmp.id = id;
	this._parent.appendChild(tmp);
	this._formList[id] = {};
	this._formList[id]["obj"] = new SepoaDhtmlXFormObject(tmp, g, this);
	this._formList[id]["event"] = {};
	tmp.className += " multi_form_box";
	
	if(fid){
		if(fid.endsWith("_BLK")){
			fid = fid.replace("_BLK", "");
		}
		
		var blockDirection = Sepoa.config.get(`sepoa.form.cbo.${fid}.direction`);
		if(["row", "column"].includes(blockDirection)){
			this._formList[id]["obj"].setBlockDirection(blockDirection);
		}
	}

	f = (typeof(f) == 'undefined')?true:f;
	if(f){
		var t = this;
		setTimeout(function(){
			//var height = $(t._parent).find('.dhxform_base').outerHeight();
			//t._maxHeight=(t._maxHeight < height)?height:t._maxHeight;
			//$(t._parent).find('div.multi_form_box').height(t._maxHeight);
			if(!(!Sepoa)) Sepoa.callEvent("onWindowResize",[]);
		}, 1500)
	}
	return this._formList[id]["obj"];
}
SepoaMultiDhtmlXFormObject.prototype.forEachObject = function(f){
	for(var _idx in this._formList){
		f.apply(this, [_idx]);
	}
}
SepoaMultiDhtmlXFormObject.prototype.forEachItem = function(f){
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		i.forEachItem(function(name){
			f.apply(this,[i,name]);
		});
	});
}
SepoaMultiDhtmlXFormObject.prototype.setdisable = function(id, flag){
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			try{
				if(flag){
    				i.disableItem(id);
    			}else{
    				i.enableItem(id);
    			}
				if(i.getItemType(id) == 'calendar'){
					i.setReadonly(id, flag)
        		}
			}catch(a){}
		}
	});
}
SepoaMultiDhtmlXFormObject.prototype.setReadonly = function(id, flag){
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			try{
				if(flag){
    				i.disableItem(id);
    			}else{
    				i.enableItem(id);
    			}
				if(i.getItemType(id) != 'select'){
					i.setReadonly(id, flag)
        		}
			}catch(a){}
		}
	});
}
SepoaMultiDhtmlXFormObject.prototype.setReadOnly = SepoaMultiDhtmlXFormObject.prototype.setReadonly;
SepoaMultiDhtmlXFormObject.prototype.getForm = function(formId){
	return this._formList[formId].obj;
}
SepoaMultiDhtmlXFormObject.prototype.setValue = function(id, value, f){
	var o = this;
	var flag = false;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			flag = i.setItemValue(id, value, f);
		}
	});
	return flag;
}
SepoaMultiDhtmlXFormObject.prototype.getValue = function(id, flag){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			returnData = i.getItemValue(id, (flag != false)?true:false);
			
			if(returnData === -1 && i.beforeSelectItem[id] != undefined) {
				returnData = i.beforeSelectItem[id];
			}
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.getInput = function(id){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			returnData = i.getInput(id);
		}
	});
	return returnData || "";
}
SepoaMultiDhtmlXFormObject.prototype.getSelect = function(id){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			if(i.getItemType(id) == 'select'){
				returnData = i.getSelect(id);
			}
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.getOptions = function(id){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			if(i.getItemType(id) == 'select'){
				returnData = i.getOptions(id);
			}
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.getContainer = function(id){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			if(i.getItemType(id) == 'container'){
				returnData = i.getContainer(id);
			}
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.getCheckedValue = function(id){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.getItemType(id) == 'radio'|| i.getItemType(id) == 'checkbox'){
			returnData = i.getCheckedValue(id);
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.checkItem = function(id,value){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.getItemType(id) == 'radio'|| i.getItemType(id) == 'checkbox'){
			i.checkItem(id,value);
		}else{
			return id+'is not radio or checkbox';
		}
	});
}
SepoaMultiDhtmlXFormObject.prototype.uncheckItem = function(id,value){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.getItemType(id) == 'radio'){
			i.forEachItem(function(ID,VAL){
				if(ID==id){
					i.uncheckItem(ID,VAL);
				}
			});
		}else if(i.getItemType(id) == 'checkbox'){
			i.uncheckItem(id);
		}else{
			return id+'is not radio or checkbox';
		}
	});
}
SepoaMultiDhtmlXFormObject.prototype.showItem = function(id){
	var o = this;
	var returnData = "";
	if(!(id instanceof Array)){
		if(typeof(id) != "string"){
			return;
		}else{
			id = id.split(',');
		}
	}
	this.forEachObject(function(formId){
		for(var idx in id){
			o.getForm(formId).showItem(id[idx]);
		}
	});
}
SepoaMultiDhtmlXFormObject.prototype.hideItem = function(id){
	var o = this;
	var returnData = "";
	if(!(id instanceof Array)){
		if(typeof(id) != "string"){
			return;
		}else{
			id = id.split(',');
		}
	}
	this.forEachObject(function(formId){
		for(var idx in id){
			o.getForm(formId).hideItem(id[idx]);
		}
	});
}
SepoaMultiDhtmlXFormObject.prototype.setItemLabel = function(id, value, label){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		o.getForm(formId).setItemLabel(id, value, label);
	});
}
SepoaMultiDhtmlXFormObject.prototype.getSelectedOption= function(id){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			if(i.getItemType(id) == 'select'){
				returnData = i.getOptions(id)[i.getOptions(id).selectedIndex].text;
			}
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype._checkAjaxStatus=function(){
	var flag = true;
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		flag = flag && i._checkAjaxStatus();
	});
	if(flag){
		this.callEvent('alreadyCallConnection',[]);
	}
	return flag;
}
 
SepoaMultiDhtmlXFormObject.prototype.dataMapper = function(sf, type){
	var that = this;
	if(!this._checkAjaxStatus()){
		var cnt = 0;
		this._ajaxDelayEvent = this.attachEvent('alreadyCallConnection', function(){
			that.clear();
			that.forEachItem(function(obj, id){
				var v = void 0;
				if(!type){
					v = sf.getValue(id);
				}else if(type.toUpperCase() == 'JSON'){
					var v = sf[id.toUpperCase()] || sf[id.toLowerCase()];
				}
				if(v){
					obj.setItemValue(id, v, true);
				}
			})
			// this.detachEvent('alreadyCallConnection',
			// that._event['alreadyCallConnection'][that._ajaxDelayEvent][this.form_id]);
			if(Object.keys(that._formList).length == ++cnt){
				that.callEvent("onXLE",[sf], false);
				that.detachEvent(that._ajaxDelayEvent);
			}
		})
	}else{
		this.clear();
		this.forEachItem(function(obj, id){
			var v = void 0;
			if(!type){
				v = sf.getValue(id);
			}else if(type.toUpperCase() == 'JSON'){
				var v = sf[id.toUpperCase()] || sf[id.toLowerCase()];
			}
			if(v){
				obj.setItemValue(id, v, true);
			}
		})
		this.callEvent("onXLE",[sf], false);
	}
}
SepoaMultiDhtmlXFormObject.prototype.setData = SepoaMultiDhtmlXFormObject.prototype.dataMapper;
SepoaMultiDhtmlXFormObject.prototype.setDataString = function(s){
	var sf = new SepoaFormater(s);
	if(sf.getRowCount() > 0){
		this.dataMapper(sf);
	}
}
SepoaMultiDhtmlXFormObject.prototype.setViewMode = function(flag){
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		i.setViewMode(flag);
	});
}
SepoaMultiDhtmlXFormObject.prototype.callEvent = function(type, param, bubble){
	var o = this;
	bubble = (bubble === false)?false:true;
	var ft = true;
	this.forEachObject(function(formId){
		if(bubble == false && ft == false){
			return;
		}
		
		ft = false;
		var i = o.getForm(formId);
		i.callEvent(type, param);
	});
}
SepoaMultiDhtmlXFormObject.prototype.attachEvent = function(type, f){
	if(!this._event[type]){
		this._event[type] = {};
	}
	var id = new Date().getTime();
	var ar = {};
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		ar[formId] = i.attachEvent(type, f);
	});
	this._event[type][id] = ar;
	return id;
}
SepoaMultiDhtmlXFormObject.prototype.detachEvent = function(id){
	var event = null;
	var type = null;
	
	for(var i in this._event){
		for(var j in this._event[i]){
			if(j == id){
				type = i;
				event = this._event[i][j] 
			}
		}
	}
	
	if(!event){
		return;
	}
	for(var _idx in event){
		this.getForm(_idx).detachEvent(event[_idx]);
	}
	delete this._event[type][id];
}
SepoaMultiDhtmlXFormObject.prototype.clear = function(){
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		i.clear();
	});
}
SepoaMultiDhtmlXFormObject.prototype.validate = function(){
	var flag = true;
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		flag = (i.validate() && flag);
	});
	return flag;
}
SepoaMultiDhtmlXFormObject.prototype.reload = function(){
	var formList = this._formList;
	var contain = this._parent.id;
	var hfFlag = this._hfFlag;
	var variable = this._variable;
	for(var _idx in formList){
		formList[_idx].obj.unload();
	}
	for (var param in arguments){
		var id = variable+'_'+param;
		this.attachForm(id,Sepoa.cache.form.get(arguments[param]),hfFlag);
	}
}

SepoaMultiDhtmlXFormObject.prototype.hideObject = function(formId){
	var o = this.getForm(formId);
	if(!o){ return ;}
	o.cont.hidden = true;
}
SepoaMultiDhtmlXFormObject.prototype.showObject = function(formId){
	var o = this.getForm(formId);
	if(!o){ return ;}
	o.cont.hidden = false;
}
SepoaMultiDhtmlXFormObject.prototype.enableLiveValidation = function(flag){
	this._apiConnector('enableLiveValidation', flag);
}
SepoaMultiDhtmlXFormObject.prototype._apiConnector = function(fn, params){
	var returnData;
	if(!Array.isArray(params)) params = [params];
	this.forEachObject(function(id){
		var obj = this.getForm(id);
		returnData = obj[fn].apply(obj,params);
	})
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.unload = function(){
	var o = this;
	this.forEachObject(function(formId){
		try {
			o.getForm(formId).unload();
		} catch (e) {}
	});
	this._parent.innerHTML = "";
}
SepoaMultiDhtmlXFormObject.prototype.validateItem = function(id){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			returnData = i.validateItem(id);
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.getItemLabel = function(id){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			returnData = i.getItemLabel(id);
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.isItemChecked = function(id){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			returnData = i.isItemChecked(id);
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.getFormData = function(f){
	var o = this;
	var returnObj = new Object;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		returnObj = $.extend({},returnObj,i.getFormData(f));
	});
	return returnObj;
}
SepoaMultiDhtmlXFormObject.prototype.setItemWidth = function(id,width){
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			i.setItemWidth(id,width);
		}
	});
}
SepoaMultiDhtmlXFormObject.prototype.setRequired = function(id,flag){
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			i.setRequired(id,flag);
		}else{
			console.log('has not found '+id);
		}
	});
}
SepoaMultiDhtmlXFormObject.prototype.getEditor = function(id){
	var o = this;
	var returnObj = new Object;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			returnObj = i.getEditor(id);
		}
	});
	return returnObj;
}
SepoaMultiDhtmlXFormObject.prototype.getItemType = function(name , value){
	var o = this;
	var returnData = "";
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i._indexId.indexOf(name) >= 0){
			returnData = i.getItemType(name,value);
		}
	});
	return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.setNote = function(id,value,note){
	var o = this;
	this.forEachObject(function(formId){
		var i = o.getForm(formId);
		if(i.isItem(id)){
			i.setNote(id,value,note);
		}
	});
}
SepoaMultiDhtmlXFormObject.prototype.isReadonly = function(id){
  var o = this;
  var returnData = "";
  this.forEachObject(function(formId){
    var i = o.getForm(formId);
    if(i.isItem(id)){
      returnData = i.isReadonly(id);
    }
  });
  return returnData;
}
SepoaMultiDhtmlXFormObject.prototype.getCombo = function(id){
  var o = this;
  var returnData = "";
  this.forEachObject(function(formId){
    var i = o.getForm(formId);
    if(i.isItem(id) && i.getItemType(id) == 'select'){
        returnData = i.getSelect(id);
    }
  });
  return returnData;
}

SepoaMultiDhtmlXFormObject.prototype.linkDataStore = function(store, path){
	let eventLocks = false;
	
	//사용자 입력시 데이터 적용
    this.attachEvent('onChange', (id, nValue)=>{
        if(!eventLocks){
            if("[object Date]" == sUtils.string.getObjectType(nValue)){
                nValue = sUtils.date.toString(nValue, '%Y%m%d');
            }
            
            store.dispatch(store.action.update({
                path:path,
                data:{...store.getState(path), [id]:nValue}
            }));
        }
    });
    
    //데이터 변경시 폼 적용
    store.subscribe((storeApi)=>{
        const changed = storeApi.getChanged(path);
        if(changed){
        	eventLocks = true;
        	this.dataMapper(storeApi.getState(path), 'json');
            eventLocks = false;
        }
    });
    
}
