/**
 * 4.dhtmlxWindow
 */
function SepoaDhtmlXWindowsObject(callScreen ,_zIndex){
	var base = dhtmlXWindows.call(this);
	
	_zIndex = typeof _zIndex !== 'undefined' ? _zIndex : 200;
	
	base.zIndexStep=_zIndex;
	
	this.viewport;
	this.callScreen = callScreen;	
	this.setViewport = function(c, l, g, a, h) {
	    e = document.createElement("DIV");
	    e.style.position = "absolute";
	    e.style.left = c + "px";
	    e.style.top = l + "px";
	    e.style.width = g + "px";
	    e.style.height = a + "px";
	    this.viewport = e;

	    if (typeof(h) == "undefined" || h == null) {
	        h = document.body
	    } else {
	        if (typeof(h) == "string") {
	            h = document.getElementById(h)
	        }
	    }
	    h.appendChild(e);
	    this.attachViewportTo(e);
	    this.conf.vp_custom = true;
	    h = e = null
	};
	
	
	this.createPopup = function(name,title,url,w,h) {
		var mainContainer = SUX.getModuleTop().document.getElementsByClassName('container')[0];
		
		this.setViewport(0,0,0,0,mainContainer);

		var x = ($(SUX.getModuleTop().window).width() - w)/2;
		var y = ($(SUX.getModuleTop().window).height() - h)/2;

		this.name = name;
		this[name] = this.createWindow(name,x,y,w,h);
		this[name].button('minmax').show();
		this[name].button('park').hide();
		this[name].setText(title);
		this[name].keepInViewport(true);
		
		var self = this;
		this[name].button('close').attachEvent('onClick', function(win, button){
			self.close();
			if(SUX.root.progressOff)
				SUX.root.progressOff(true);
		});

		if(url != null) {
			this.attachURL(url);
		}
		this[name].hide();
		
		this[name].sizeEvent = SUX.getModuleTop().Sepoa.attachEvent('onWindowResize', this.setSize);
	}
	this.parentWindow = window;
	this.close=function(){
		SUX.getModuleTop().Sepoa.detachEvent(this.eventNum);

		if(this[this.name].getFrame()){
			var s = this[this.name].getFrame().contentWindow;
			try{
				if(s.Sepoa){
					var sid = s.Sepoa.common.SCREEN_ID;
					if(s.Sepoa.request.alias){
						sid = [sid, s.Sepoa.request.alias].join('_'); 
					}
				}
			}catch(e){}
		}
		
		this.parentWindow.SUX.getModuleTop().Sepoa.detachEvent(this[this.name].sizeEvent);
		
		this.unload();
	}
	this.attachURL = function(url, params){
		this._url = url;
		
		var windowParameterId = new Date().valueOf()+''+dhx.newId();
		
		params = (!params)?{}:params;
		params.isPopup=true;
		params.parameterId = windowParameterId;
		
		if(!SUX.root.modalPopupInfo){
			SUX.root.modalPopupInfo = {};
		}
		var data = {};
		data.isPopup = true;
		data.selfObj = this;
		data.params  = params;
		
		SUX.root.modalPopupInfo[windowParameterId] = data;
		
		this[this.name].attachURL(url, null, params);
		var f = this[this.name].getFrame();
		var sf = this;
		
		f.onload = function(){
			try{
				if(f.contentWindow.init){
					f.contentWindow.init.apply(f.contentWindow)
				}
			}catch(e){
				console.log(e)
			}
		};
	}
	
	this.progressOn = function(){
		this[this.name].progressOn();
	}
	this.progressOff = function(){
		this[this.name].progressOff();
	}
	
	this.setShow = function(refresh, params) {
		if(!!refresh){
			this.attachURL(this._url, params);
		}
		this.viewport.style.width = '100%';
		this.viewport.style.height = '100%';
		this[this.name].center();
		this[this.name].setModal(true);
		this[this.name].show();
	}
	
	this.setHide = function() {
		this.viewport.style.width = '0px';
		this.viewport.style.height = '0px';
		this[this.name].setModal(false);
		this[this.name].hide();
	}
	
	var that = this;
	this.setSize = function(flag){
		if(!flag){
			if(!that[that.name].isHidden()){
				var h = that.viewport.offsetHeight;
				var w = that.viewport.offsetWidth;
				
				var wh = that[that.name].getDimension()[1];
				var ww = that[that.name].getDimension()[0];
				
				if(h < wh) wh = h;
				if(w < ww) ww = w;
				
				var ww = that[that.name].setDimension(ww, wh);
				that[that.name].center();
			}
		}
	}
	this.setDimension=function(w, h){
		this[this.name].setDimension(w, h);
		this[this.name].center();
	}.bind(this);
	
	this.getDimension=function(){
		return this[this.name].getDimension();
	}.bind(this);
}
SepoaDhtmlXWindowsObject.prototype = Object.create(dhtmlXWindows.prototype);
SepoaDhtmlXWindowsObject.prototype.constructor = SepoaDhtmlXWindowsObject;
SepoaDhtmlXWindowsObject.prototype.maximize = function(){
	this[this.name].maximize();
}
SepoaDhtmlXWindowsObject.prototype._dndInitModule = function() {
    var a = this;
    this.conf.dnd_enabled = true;
    this.conf.dnd_tm = null;
    this.conf.dnd_time = 0;
    this._dndOnMouseDown = function(h, l) {
        if (a.conf.dblclick_active) {
            return
        }
        if (h.preventDefault) {
            h.preventDefault()
        } else {
            h.returnValue = false
        }
        if (a._callMainEvent("onBeforeMoveStart", l) !== true) {
            return
        }
        a.conf.dnd = {
            id: l,
            x: a._dndPos(h, "X"),
            y: a._dndPos(h, "Y"),
            ready: true,
            css: false,
            css_touch: false,
            css_vp: false,
            tr: null,
            mode: "def",
            moved: false,
            prevent: false
        };
        if (a.w[l].conf.keep_in_vp) {
            a.conf.dnd.minX = 0;
            a.conf.dnd.maxX = a.vp.clientWidth - a.w[l].conf.w;
            a.conf.dnd.minY = 0;
            a.conf.dnd.maxY = a.vp.clientHeight - a.w[l].conf.h
        } else {
            a.conf.dnd.minX = -a.w[l].conf.w + a.conf.vp_pos_ofs;
            a.conf.dnd.maxX = a.vp.clientWidth - a.conf.vp_pos_ofs;
            a.conf.dnd.minY = 0;
            a.conf.dnd.maxY = a.vp.clientHeight - a.conf.vp_pos_ofs
        }
        var c = ["MozTransform", "WebkitTransform", "OTransform", "msTransform", "transform"];
        for (var g = 0; g < c.length; g++) {
            if (document.documentElement.style[c[g]] != null && a.conf.dnd.tr == null) {
                a.conf.dnd.tr = c[g];
                a.conf.dnd.mode = "tr"
            }
        }
        if (a.conf.dnd.mode == "tr") {
            a.w[l].win.style[a.conf.dnd.tr] = "translate(0px,0px)";
            if (a.w[l].fr_m_cover != null) {
                a.w[l].fr_m_cover.style[a.conf.dnd.tr] = a.w[l].win.style[a.conf.dnd.tr]
            }
        }
        if (window.dhx4.dnd._mTouch(h) == false && h.type == window.dhx4.dnd.evs.start) {
            if (a.conf.dnd.css_touch == false) {
                a.w[l].win.className += " dhxwin_dnd_touch";
                a.conf.dnd.css_touch = true
            }
            if (a.conf.dnd.css_vp == false) {
                a.vp.className += " dhxwins_vp_dnd";
                a.conf.dnd.css_vp = true
            }
        } else {
            a._dndInitEvents()
        }
    };
    this._dndOnMouseMove = function(l) {
        l = l || event;
        if(!a){
        	return;
        }
        var h = a.conf.dnd;
        var c = a._dndPos(l, "X") - h.x;
        var m = a._dndPos(l, "Y") - h.y;
        if (l.type == window.dhx4.dnd.evs.move) {
            if (h.moved != true && (Math.abs(c) > 20 || Math.abs(m) > 20)) {
                if (a.conf.dnd_tm != null) {
                    window.clearTimeout(a.conf.dnd_tm);
                    a.conf.dnd_tm = null
                }
                window.removeEventListener(window.dhx4.dnd.evs.start, a._dndOnMouseMove, false);
                return
            }
        }
        if (h.ready != true) {
            return
        }
        var g = a.w[h.id];
        if (l.preventDefault) {
            l.preventDefault()
        } else {
            l.returnValue = false
        }
        if (h.css != true) {
            if (h.css_touch == false) {
                g.win.className += " dhxwin_dnd"
            }
            g.fr_cover.className += " dhxwin_fr_cover_dnd";
            h.css = true
        }
        if (h.css_vp != true) {
            a.vp.className += " dhxwins_vp_dnd";
            h.css_vp = true
        }
        h.newX = g.conf.x + c;
        h.newY = g.conf.y + m;
        if (h.mode == "tr") {
            h.newX = Math.min(Math.max(h.newX, h.minX), h.maxX);
            c = h.newX - g.conf.x;
            h.newY = Math.min(Math.max(h.newY, h.minY), h.maxY);
            m = h.newY - g.conf.y;
            g.win.style[h.tr] = "translate(" + c + "px," + m + "px)";
            if (g.fr_m_cover != null) {
                g.fr_m_cover.style[h.tr] = g.win.style[h.tr]
            }
        } else {
            if (h.newX < h.minX || h.newX > h.maxX) {
                h.newX = Math.min(Math.max(h.newX, h.minX), h.maxX)
            } else {
                h.x = a._dndPos(l, "X")
            }
            if (h.newY < h.minY || h.newY > h.maxY) {
                h.newY = Math.min(Math.max(h.newY, h.minY), h.maxY)
            } else {
                h.y = a._dndPos(l, "Y")
            }
            a._winSetPosition(h.id, h.newX, h.newY)
        }
        h.moved = true;
        g = h = null
    };
    this._dndOnMouseUp = function(h) {
    	if(!a){
        	return;
        }
        h = h || event;
        a._dndUnloadEvents();
        if (a.conf.dnd != null && a.conf.dnd.id != null) {
            var g = a.conf.dnd;
            var c = a.w[g.id];
            if (g.newX != null) {
                if (g.mode == "tr") {
                    a._winSetPosition(g.id, g.newX, g.newY);
                    c.win.style[g.tr] = "translate(0px,0px)";
                    if (c.fr_m_cover != null) {
                        c.fr_m_cover.style[g.tr] = c.win.style[g.tr]
                    }
                }
            }
            if (g.css == true) {
                if (g.css_touch == false) {
                    c.win.className = String(c.win.className).replace(/\s{0,}dhxwin_dnd/gi, "")
                }
                c.fr_cover.className = String(c.fr_cover.className).replace(/\s{0,}dhxwin_fr_cover_dnd/gi, "")
            }
            if (g.css_touch == true) {
                c.win.className = String(c.win.className).replace(/\s{0,}dhxwin_dnd_touch/gi, "")
            }
            if (g.css_vp == true) {
                a.vp.className = String(a.vp.className).replace(/\s{0,}dhxwins_vp_dnd/gi, "")
            }
            if (g.moved == true) {
                a._callMainEvent("onMoveFinish", g.id)
            } else {
                a._callMainEvent("onMoveCancel", g.id)
            }
            c = g = a.conf.dnd = null
        }
        if (window.dhx4.dnd.p_en == true && h.type == window.dhx4.dnd.evs.end) {
            window.dhx4.dnd._touchOn();
            window.removeEventListener(window.dhx4.dnd.evs.end, a._dndOnMouseUp, false);
            window.removeEventListener(window.dhx4.dnd.evs.move, a._dndOnMouseMove, false);
            if (a.conf.dnd_tm != null) {
                window.clearTimeout(a.conf.dnd_tm)
            }
            a.conf.dnd_tm = null
        }
    };
    this._dndOnSelectStart = function(c) {
    	c = c || event;
        if (c.preventDefault) {
            c.preventDefault()
        } else {
            c.returnValue = false
        }
        return false
    };
    this._dndInitEvents = function() {
        if (typeof(window.addEventListener) == "function") {
            window.addEventListener("mousemove", this._dndOnMouseMove, false);
            window.addEventListener("mouseup", this._dndOnMouseUp, false);
            window.addEventListener("selectstart", this._dndOnSelectStart, false)
        } else {
            document.body.attachEvent("onmousemove", this._dndOnMouseMove);
            document.body.attachEvent("onmouseup", this._dndOnMouseUp);
            document.body.attachEvent("onselectstart", this._dndOnSelectStart)
        }
    };
    this._dndUnloadEvents = function() {
        if (typeof(window.addEventListener) == "function") {
            window.removeEventListener("mousemove", this._dndOnMouseMove, false);
            window.removeEventListener("mouseup", this._dndOnMouseUp, false);
            window.removeEventListener("selectstart", this._dndOnSelectStart, false)
        } else {
            document.body.detachEvent("onmousemove", this._dndOnMouseMove);
            document.body.detachEvent("onmouseup", this._dndOnMouseUp);
            document.body.detachEvent("onselectstart", this._dndOnSelectStart)
        }
    };
    this._dndUnloadModule = function() {
        this.detachEvent(this.conf.dnd_evid);
        this._dndUnloadEvents();
        this.conf.dnd_evid = null;
        this._dndOnMouseDown = null;
        this._dndOnMouseMove = null;
        this._dndOnMouseUp = null;
        this._dndOnSelectStart = null;
        this._dndInitEvents = null;
        this._dndUnloadEvents = null;
        this._dndInitModule = null;
        this._dndUnloadModule = null;
        a = null
    };
    this._dndPos = function(e, c) {
        var g = e[this.conf.dnd_ev_prefix + c];
        if ((g == null || g == 0) && e.touches != null) {
            g = e.touches[0][this.conf.dnd_ev_prefix + c]
        }
        return g
    };
    this.conf.dnd_evid = this.attachEvent("_winMouseDown", function(g, c) {
        if (this.w[c.id] == null || this.w[c.id].conf.allow_move != true) {
            return
        }
        if (typeof(g.button) != "undefined" && g.button >= 2) {
            return
        }
        if (g.type == window.dhx4.dnd.evs.start) {
            if (c.mode == "hdr") {
                if (this.w[c.id].conf.maxed && this.w[c.id].conf.max_w == null && this.w[c.id].conf.max_h == null) {
                    return
                }
                this.conf.dnd_ev_prefix = "page";
                this.conf.dnd = {
                    x: this._dndPos(g, "X"),
                    y: this._dndPos(g, "Y")
                };
                if (this.conf.dnd_time < 1) {
                    this._dndOnMouseDown(g, c.id)
                } else {
                    if (this.conf.dnd_tm != null) {
                        window.clearTimeout(this.conf.dnd_tm)
                    }
                    this.conf.dnd_tm = window.setTimeout(function() {
                        a._dndOnMouseDown(g, c.id)
                    }, this.conf.dnd_time)
                }
                if (window.dhx4.dnd.p_en == true) {
                    window.dhx4.dnd._touchOff();
                    window.addEventListener(window.dhx4.dnd.evs.end, this._dndOnMouseUp, false)
                }
                window.addEventListener(window.dhx4.dnd.evs.move, this._dndOnMouseMove, false)
            }
            return false
        }
        if (g.type == window.dhx4.dnd.evs.end) {
            if (this.conf.dnd_tm != null) {
                window.clearTimeout(this.conf.dnd_tm);
                this.conf.dnd_tm = null
            }
            this._dndOnMouseUp(g);
            window.removeEventListener(window.dhx4.dnd.evs.move, this._dndOnMouseMove, false);
            return false
        }
        this.conf.dnd_ev_prefix = "client";
        if (!(c.mode == "hdr" && g.type == "mousedown")) {
            return
        }
        if (this.w[c.id].conf.maxed && this.w[c.id].conf.max_w == null && this.w[c.id].conf.max_h == null) {
            return
        }
        if (g.preventDefault) {
            g.preventDefault()
        } else {
            g.returnValue = false
        }
        this._dndOnMouseDown(g, c.id);
        return false
    })
}
SepoaDhtmlXWindowsObject.prototype._winAdjustCell = function(c) {
	var r = this.w[c];
    if (this.conf.skin == "material") {
        var o = 0;
        var n = (r.conf.header ? r.hdr.offsetHeight : 0);
        var g = r.win.clientWidth;
        var s = r.win.clientHeight - n
    } else {
        var o = 1;
        var n = (r.conf.header ? r.hdr.offsetHeight : 1);
        var g = r.win.clientWidth - 2;
        var s = r.win.clientHeight - n - 1
    }
    r.brd.style.left = o + "px";
    r.brd.style.top = n + "px";
    if (r.conf.brd == null) {
        r.brd.style.width = g + "px";
        r.brd.style.height = s + "px";
        r.conf.brd = {
            w: g - r.brd.offsetWidth,
            h: s - r.brd.offsetHeight
        }
    }
    r.brd.style.width = g + r.conf.brd.w + "px";
    r.brd.style.height = s + r.conf.brd.h + "px";
    var e = 5;
    if (this.conf.skin == "material") {
        e = 0;
    }
    var a = 0 + e;
    var m = (r.conf.header ? n : n + e);
    var h = r.brd.clientWidth;
    var l = r.brd.clientHeight;
    r.cell._setSize(a, m, h, l);
    r.fr_cover.style.left = a + "px";
    r.fr_cover.style.top = m + "px";
    r.fr_cover.style.width = h + "px";
    r.fr_cover.style.height = l + "px";
    r = null
};