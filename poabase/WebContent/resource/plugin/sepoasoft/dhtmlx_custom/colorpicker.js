SepoaDhtmlXColorPicker = function(n, conf) {
	dhtmlXColorPicker.call(this, n);
	dhx4._eventable(this);
	
	this.obj = this;
	
	if(conf) {
		this.conf = $.extend(true, this.conf, conf);
	}
	
	this.base.innerHTML = "<div class='dhxcp_g_area'><div class='dhxcp_sub_area'><div class='dhxcp_g_color_area'><div class='dhxcp_color_selector'><div class='dhxcp_v_line'></div><div class='dhxcp_h_line'></div></div><div class='dhxcp_contrast_area'><div class='dhxcp_h_line'></div></div></div><div class='dhxcp_g_input_area'><div class='dhxcp_value_cont'><div class='dhxcp_value_color'></div><input type='text' class='dhxcp_value'/></div><table class='dhxcp_inputs_cont' cellpadding='0' cellspacing='0' border='0'><tr><td class='dhxcp_label_hsl'>" + this.i18n[this.conf.lang].labelHue + "</td><td class='dhxcp_input_hsl'><input type='text' class='dhxcp_input_hsl'/></td><td class='dhxcp_label_rgb'>" + this.i18n[this.conf.lang].labelRed + "</td><td class='dhxcp_input_rgb'><input type='text' class='dhxcp_input_rgb'/></td></tr><tr><td class='dhxcp_label_hsl'>" + this.i18n[this.conf.lang].labelSat + "</td><td class='dhxcp_input_hsl'><input type='text' class='dhxcp_input_hsl'/></td><td class='dhxcp_label_rgb'>" + this.i18n[this.conf.lang].labelGreen + "</td><td class='dhxcp_input_rgb'><input type='text' class='dhxcp_input_rgb'/></td></tr><tr><td class='dhxcp_label_hsl'>" + this.i18n[this.conf.lang].labelLum + "</td><td class='dhxcp_input_hsl'><input type='text' class='dhxcp_input_hsl'/></td><td class='dhxcp_label_rgb'>" + this.i18n[this.conf.lang].labelBlue + "</td><td class='dhxcp_input_rgb'><input type='text' class='dhxcp_input_rgb'/></td></tr></table></div><div class='dhxcp_g_memory_area'></div><div class='dhxcp_buttons_area'><button class='dhx_button_save'>" + this.i18n[this.conf.lang].btnSelect + "</button><button class='dhx_button_cancel'>" + this.i18n[this.conf.lang].btnCancel + "</button><button class='dhx_button_clear'>" + this.i18n[this.conf.lang].btnClear + "</button></div></div>";
    this._globalNode = this.base.firstChild;
    
    this._controllerNodes = {
        colorArea: this._globalNode.firstChild.firstChild.firstChild,
        v_line: this._globalNode.firstChild.firstChild.firstChild.childNodes[0],
        h_line: this._globalNode.firstChild.firstChild.firstChild.childNodes[1],
        contrastArea: this._globalNode.firstChild.firstChild.childNodes[1],
        contrast_line: this._globalNode.firstChild.firstChild.childNodes[1].firstChild,
        color: this._globalNode.firstChild.childNodes[1].childNodes[0].firstChild,
        hsv: this._globalNode.firstChild.childNodes[1].childNodes[0].childNodes[1],
        hue: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[0].childNodes[1].firstChild,
        sat: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[1].childNodes[1].firstChild,
        lum: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[2].childNodes[1].firstChild,
        red: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[0].childNodes[3].firstChild,
        green: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[1].childNodes[3].firstChild,
        blue: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[2].childNodes[3].firstChild,
        memory_block: this._globalNode.firstChild.childNodes[2],
        button_save: this._globalNode.firstChild.childNodes[3].firstChild,
        button_cancel: this._globalNode.firstChild.childNodes[3].childNodes[1],
        button_clear: this._globalNode.firstChild.childNodes[3].childNodes[2]
    };
    this._labelNodes = {
        labelHue: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[0].firstChild,
        labelSat: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[1].firstChild,
        labelLum: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[2].firstChild,
        labelRed: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[0].childNodes[2],
        labelGreen: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[1].childNodes[2],
        labelBlue: this._globalNode.firstChild.childNodes[1].childNodes[1].firstChild.childNodes[2].childNodes[2],
        btnAddColor: null,
        btnSelect: this._globalNode.firstChild.childNodes[3].firstChild,
        btnCancel: this._globalNode.firstChild.childNodes[3].childNodes[1],
        btnClear: this._globalNode.firstChild.childNodes[3].childNodes[2]
    };
    if (typeof(this._tempInitCC) == "function") {
        this._tempInitCC()
    }
    if (typeof(window.addEventListener) == "function") {
        this._controllerNodes.colorArea.addEventListener("mousedown", this._initMoveSelection, false);
        this._controllerNodes.colorArea.addEventListener("dblclick", this._doOnSelectColor, false);
        this._controllerNodes.contrastArea.addEventListener("mousedown", this._initMoveContrast, false);
        this._controllerNodes.button_save.addEventListener("click", this._doOnSelectColor, false);
        this._controllerNodes.button_cancel.addEventListener("click", this._doOnCancel, false);
        this._controllerNodes.button_clear.addEventListener("click", this._doOnClear.bind(this), false);
        this._controllerNodes.hue.addEventListener("change", this._doOnChangeHSL, false);
        this._controllerNodes.sat.addEventListener("change", this._doOnChangeHSL, false);
        this._controllerNodes.lum.addEventListener("change", this._doOnChangeHSL, false);
        this._controllerNodes.red.addEventListener("change", this._doOnChangeRGB, false);
        this._controllerNodes.green.addEventListener("change", this._doOnChangeRGB, false);
        this._controllerNodes.blue.addEventListener("change", this._doOnChangeRGB, false);
        this._controllerNodes.hsv.addEventListener("change", this._doOnChangeHSV, false)
    } else {
        this._controllerNodes.colorArea.attachEvent("onmousedown", this._initMoveSelection);
        this._controllerNodes.colorArea.attachEvent("ondblclick", this._doOnSelectColor);
        this._controllerNodes.contrastArea.attachEvent("onmousedown", this._initMoveContrast);
        this._controllerNodes.button_save.attachEvent("onclick", this._doOnSelectColor);
        this._controllerNodes.button_cancel.attachEvent("onclick", this._doOnCancel);
        this._controllerNodes.button_clear.attachEvent("onclick", this._doOnClear.bind(this));
        this._controllerNodes.hue.attachEvent("onchange", this._doOnChangeHSL);
        this._controllerNodes.sat.attachEvent("onchange", this._doOnChangeHSL);
        this._controllerNodes.lum.attachEvent("onchange", this._doOnChangeHSL);
        this._controllerNodes.red.attachEvent("onchange", this._doOnChangeRGB);
        this._controllerNodes.green.attachEvent("onchange", this._doOnChangeRGB);
        this._controllerNodes.blue.attachEvent("onchange", this._doOnChangeRGB);
        this._controllerNodes.hsv.attachEvent("onchange", this._doOnChangeHSV)
    }
    this.setColor(this.conf.selectedColor || "#ffffff");
    if (this._nodes.length) {
        for (var e = 0; e < this._nodes.length; e++) {
            this._attachEventsToNode(this._nodes[e])
        }
    }
    if (this.conf.hide) {
        this.hide()
    }
    if (typeof(this._cpInitFRM) == "function") {
        this._cpInitFRM()
    }
    
    this.attachEvent('onClear', function(n) {
    	this.setColor('#ffffff');
    	return true;
    });
}
SepoaDhtmlXColorPicker.prototype = Object.create(dhtmlXColorPicker.prototype);
SepoaDhtmlXColorPicker.prototype.constructor = SepoaDhtmlXColorPicker;

SepoaDhtmlXColorPicker.prototype.setColor = function(l) {
    var c = this.colorAIP.rgb2hex({
        r: this.value.red,
        g: this.value.green,
        b: this.value.blue
    });
    var e = this._parseColor(l);
    var h = (e instanceof Object);
    h = h && (0 <= e.r && e.r <= 255);
    h = h && (0 <= e.g && e.g <= 255);
    h = h && (0 <= e.b && e.b <= 255);
    if (!h) {
        return c
    }
    var g = this.colorAIP.rgb2hex({
        r: e.r,
        g: e.g,
        b: e.b
    });
    if (g == c) {
        return c
    }
    this.value.red = e.r;
    this.value.green = e.g;
    this.value.blue = e.b;
    var a = this.colorAIP.rgb2hsl(e.r / 255, e.g / 255, e.b / 255);
    this.value.hue = Math.round(a.h);
    this.value.sat = Math.round(a.s * 100);
    this.value.lum = Math.round(a.l * 100);
    this._refreshCoordinatesByHSL(this.value.hue, this.value.sat, this.value.lum);
    this._refreshContrast();
    this._refreshInputValues();
    this._refreshColorValue();
    return g
};

SepoaDhtmlXColorPicker.prototype.setSkin = function(h) {
    if (this._skinCollection[h] != true) {
        return
    }
    var g = "dhtmlxcp_" + (this.skin || "dummy");
    this.base.className = this.base.className.replace(new RegExp(g), "");
    this.base.className += " dhtmlxcp_" + h;
    this.skin = h;
    if (this.skin == "material") {
        var e = {
            labelHue: "H",
            labelSat: "S",
            labelLum: "L",
            labelRed: "R",
            labelGreen: "G",
            labelBlue: "B"
        };
        for (var c in e) {
        	this.i18n[this.conf.lang][c] = e[c];
        }
    }
    if (this.base.className.match(/dhxcp_shadow/) == null && !(dhx4.isIE6 || dhx4.isIE7 || dhx4.isIE8)) {
        this.base.className += " dhxcp_shadow"
    }
};

SepoaDhtmlXColorPicker.prototype._doOnClear = function() {
	var h = this.obj;
    if (h.callEvent("onClear", [((h.activeNode) ? h.activeNode.node : null)]) == true && h.conf.closeable == true) {
        h.hide(true);
    }
};

SepoaDhtmlXColorPicker.prototype.hide = function(clearValue) {
    if (this.base._dhx_remove) {
        if (this.base.parentNode) {
            this.base.parentNode.removeChild(this.base);
            dhx4.zim.clear(this.conf.cp_id);
            if (typeof(window.addEventListener) == "function") {
                document.body.removeEventListener("mousedown", this._doOnClickByBody, false)
            } else {
                document.body.detachEvent("onmousedown", this._doOnClickByBody)
            }
        }
    } else {
        if (this.isVisible() == false) {
            return
        }
        this.base.removeChild(this._globalNode)
    }
    if (this._controllerNodes.fr_cover && this._controllerNodes.fr_cover.parentNode) {
        this._controllerNodes.fr_cover.parentNode.removeChild(this._controllerNodes.fr_cover)
    }
    if (this.callEvent != undefined) {
		this.callEvent("onHide", [((this.activeNode) ? this.activeNode.node : null), clearValue])
    }
};

SepoaDhtmlXColorPicker.prototype.i18n = {
	en: {
        labelHue: "H",
        labelSat: "S",
        labelLum: "L",
        labelRed: "R",
        labelGreen: "G",
        labelBlue: "B",
        btnAddColor: "Save the color",
        btnSelect: "Select",
        btnCancel: "Cancel",
        btnClear: "Clear"
    },
    ko: {
        labelHue: "H",
        labelSat: "S",
        labelLum: "L",
        labelRed: "R",
        labelGreen: "G",
        labelBlue: "B",
        btnAddColor: "색상 저장",
        btnSelect: "선택",
        btnCancel: "취소",
        btnClear: "초기화"
    }
};