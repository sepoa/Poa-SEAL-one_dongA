/**
 * 6.dhtmlXCarousel 
 */
function SepoaDhtmlXCarouselObject(g, l, n){
	if(g.touch_scroll !== true){
		g.touch_scroll = false
	}
	var base = dhtmlXCarousel.call(this, g, l, n);
	this.base.dhtmlxObject = this;
	this.setSizes = function() {
		if(this.conf.animating){
			if(this.conf.delayResize){
				this.detachEvent(this.conf.delayResize);
			}
			var that = this;
			this.conf.delayResize = this.attachEvent("_onAnimateEnd", function(){
				this.setSizes();
				this.detachEvent(this.conf.delayResize);
				this.conf.delayResize = null;
				sUtils.ux.setSizes();
			})
			return;
		}
	    this._adjustCont();
	    var v = {};
	    this.area.style.height = this.cont.offsetHeight - this.controls.offsetHeight + "px";
	    this.conf.width = (this.conf.autowidth ? this.cont.offsetWidth - this.conf.ofs_left * 2 : this.conf.item_width);
	    this.conf.height = (this.conf.autoheight ? this.area.offsetHeight - this.conf.ofs_top * 2 : this.conf.item_height);
	    this.conf.top = Math.max(0, (this.conf.autoheight ? this.conf.ofs_top : Math.floor((this.area.offsetHeight - this.conf.height) / 2)));
	    this.area.style.width = this[this.conf.anim_f.detect_aw]() * (this.conf.width + this.conf.ofs_item) + this.conf.ofs_item + "px";
	    this.area.style.left = Math.round(this.cont.offsetWidth / 2 - this.conf.width / 2 - this.conf.ofs_item) + "px";
	    for (var r in this.cdata) {
	        var u = {};
	        for (var o in this.cdata[r].cell.conf.size) {
	            u[o] = this.cdata[r].cell.conf.size[o]
	        }
	        if (this.conf.autowidth == true) {
	            u.w = this.conf.width;
	            u.x = this[this.conf.anim_f.detect_x](r)
	        }
	        if (this.conf.autoheight == true) {
	            u.h = this.conf.height
	        }
	        v[r] = u
	    }
	    if(['cards', 'flip'].indexOf(this.conf.anim_type) + 1){
	    	this.area.style.left = "0px";
	    }else{
	    	this.area.style.left = Math.round(this.cont.offsetWidth / 2 - this.conf.width / 2 - this.conf.ofs_item) - (this.conf.width + this.conf.ofs_item) * this.conf.selected + "px";
	    }
	    this._adjustControls();
	    if (this.conf.autoheight != true) {
	        this.conf.top = Math.max(0, Math.floor(this.area.offsetHeight - this.conf.height) / 2);
	        for (var r in v) {
	            v[r].y = this.conf.top
	        }
	    }
	    for (var r in v) {
	        this.cdata[r].cell._setSize(v[r].x, v[r].y, v[r].w, v[r].h)
	    }
	    this.callEvent("_onSetSizes", [])
	}
	this.enableHotKeys(false);
	this._addCell = this.addCell;
	var _keyEvent = {}
	this.addCell = function(u, s){
		this._addCell(u, s);
		$(this.cells(u).cell).on({
			keydown:function(e){
				_keyEvent.key=e.which
			},
			focusout:function(e){
				if(_keyEvent.key == 9){
					var rc = $(e.relatedTarget).parents('.dhx_cell_carousel')[0];
					if(rc != this){
						e.originalEvent.preventDefault();
						e.originalEvent.stopPropagation();
						$(e.originalEvent.target).focus();
					}
				}
				_keyEvent = {}
			}
		})
	}
	this._removeCell = function(v) {
		this.conf.animating = false;
        var s = this.cdata[v].index;
        this.cdata[v].cell._unload();
        this.cdata[v].index = this.cdata[v].cell = null;
        this.cdata[v] = null;
        delete this.cdata[v];
        delete this.ind[s];
        this.conf.items_count--;
        if (this.conf.unloading == true) {
            return
        }
        this.ind = {};
        var o = 0;
        for (var r in this.cdata) {
            if (this.cdata[r].index > s) {
                this.cdata[r].index--
            }
            this.ind[this.cdata[r].index] = r
        }
        var u = false;
        if (this.conf.selected > s) {
            this.conf.selected--
        } else {
            if (this.conf.selected == s) {
                this.conf.selected = Math.min(this.conf.selected, this.conf.items_count - 1);
                u = true
            } else {}
        }
        this._removeBar(false);
        this._setBarIndex(this.conf.selected);
        if (u == true) {
            if (this.conf.selected >= 0) {
                this[this.conf.anim_f.update_selected](this.ind[this.conf.selected])
            }
        }
        this.setSizes();
        this._checkControls()
    };
    this._animateEnd = function(a) {
        this.conf.selected = this.conf.selected + a;
        this._checkControls();
        this._setBarIndex(this.conf.selected);
        this.callEvent("onSelect", [this.ind[this.conf.selected]]);
        this.conf.animating = false;
        this.callEvent("_onAnimateEnd", [this.ind[this.conf.selected]]);
    };
}
SepoaDhtmlXCarouselObject.prototype = Object.create(dhtmlXCarousel.prototype);
SepoaDhtmlXCarouselObject.prototype.constructor = dhtmlXCarousel;
SepoaDhtmlXCarouselObject.prototype.callCAPI = function(cid, fn, params){
	var a = this.cells(cid);
	return a[fn].apply(a, params);
}
SepoaDhtmlXCarouselObject.prototype._cards_prepare = function(e, c) {
    if (e > 0) {
        var g = this.ind[this.conf.selected + e];
        var a = this.cdata[g].cell;
        this._cards_update_selected(g);
        if (a.conf.transEvInit != true) {
            a.cell.addEventListener(this.conf.transEv, this._animateTransEnd, false);
            a.conf.transEvInit = true
        }
        this.conf.current_id = g;
        this.conf.current_dir = e;
        a.cell.style[this.conf.transProp] = this.conf.anim_cards;
        a.cell.style.left = this._cards_detect_x(g, 0) + "px";
        a = null
    } else {
        this._cards_adjust_middle(this.conf.selected + e + 1, this.conf.selected - 1, 1);
        var g = this.ind[this.conf.selected];
        var a = this.cdata[g].cell;
        a._hideCover();
        if (a.conf.transEvInit != true) {
            a.cell.addEventListener(this.conf.transEv, this._animateTransEnd, false);
            a.conf.transEvInit = true
        }
        this.conf.current_id = g;
        this.conf.current_dir = e;
        this.cdata[this.ind[this.conf.selected + e]].cell._hideCover();
        a.cell.style[this.conf.transProp] = this.conf.anim_cards;
        a.cell.style.left = this._cards_detect_x(g, 1) + "px";
        a = null
    }
};
dhtmlXCarouselCell.prototype.setActive = function() {
    var a = this.getIndex() - this.carousel.conf.selected;
    if(this.carousel.callEvent('onBeforeChange', [this._idd]) != false){
    	if (a != 0) {
            this.carousel._animateStart(a)
        }
    }
};