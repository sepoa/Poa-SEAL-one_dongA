function SepoaDhtmlXCalendarYM(l, s) {
	var origin = dhtmlXCalendarObject.call(this, l, s);
	this.base.className +=" dhtmlxcalendarYM";
	this._updateInp = function() {
		if(this._nullDate){
			this.i[this._activeInp].input.value = '';
		}else if (this.i != null && this._activeInp != null && this.i[this._activeInp] != null && this.i[this._activeInp].input != null) {
            //this.i[this._activeInp].input.value = this.getFormatedDate()
        	this.i[this._activeInp].input.value = this.getFormatedDate(("%Y/%m"),this._activeDate);
        }
    };
    this.hideTime();
    this._oDrawMonth = this._drawMonth;
    this._drawMonth = function(F){
    	this._oDrawMonth(F);
    	//this._activeDate = F;
    	//this._updateInp();
    }
    
    this.attachEvent("onArrowClick", function(a, F){
    	this._activeDate = F;
    	this._updateInp();
    })
    
    this.attachEvent('onMonthSelect', function(){
    	this.hide();
    })
    
	var h = this;
	$(this.base).find('.dhtmlxcalendar_cell.dhtmlxcalendar_month_hdr')[0].onclick = function(w) {
        w = w || event;
        if (w.type == "touchstart" && w.preventDefault != null) {
            w.preventDefault()
        }
        var u = (w.target || w.srcElement);
        if (u.className && u.className.indexOf("dhtmlxcalendar_month_arrow") === 0) {
            h._hideSelector();
            var v = (u.parentNode.firstChild == u ? -1 : 1);
            var a = new Date(h._activeMonth);
            h._drawMonth(new Date(h._activeMonth.getFullYear(), h._activeMonth.getMonth() + v, 1, 0, 0, 0, 0));
            h._evOnArrowClick([a, new Date(h._activeMonth)]);
            return
        }
        if (u.className && u.className == "dhtmlxcalendar_month_label_month") {
            w.cancelBubble = true;
            h._showSelector("month", Math.round(u.offsetLeft + u.offsetWidth / 2), u.offsetTop + u.offsetHeight + 2, "selector_month", true);
            return
        }
        if (u.className && u.className == "dhtmlxcalendar_month_label_year") {
            w.cancelBubble = true;
            h._showSelector("year", Math.round(u.offsetLeft + u.offsetWidth / 2), u.offsetTop + u.offsetHeight + 2, "selector_year", true);
            return
        }
        h._hideSelector()
    };
    
    this.setDate = function(u) {
        window.dhx4.temp_calendar = {
            tz: null
        };
        this._nullDate = (typeof(u) == "undefined" || u === "" || !u);
        if (!(u instanceof Date)) {
            u = this._strToDate(String(u || ""), "%Y/%m");
            if (u == "Invalid Date") {
                u = new Date()
            } else {
                this.conf.tz = window.dhx4.temp_calendar.tz
            }
            window.dhx4.temp_calendar = null
        }
        if (this.conf.tz == null) {
            this.conf.tz = window.dhx4.date2str(u, "%P")
        }
        var a = u.getTime();
        if (this._isOutOfRange(a)) {
            return
        }
        this._activeDate = new Date(a);
        this._drawMonth(this._nullDate ? new Date() : this._activeDate);
        this._updateVisibleHours();
        this._updateVisibleMinutes()
    };
}
SepoaDhtmlXCalendarYM.prototype = Object.create(dhtmlXCalendarObject.prototype);
SepoaDhtmlXCalendarYM.prototype.constructor = dhtmlXCalendarObject;
