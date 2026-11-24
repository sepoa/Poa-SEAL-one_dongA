function dhtmlXCalendarObject(l, s, e) {
	this.lang = (Sepoa.info?(Sepoa.info.LANGUAGE?Sepoa.info.LANGUAGE:"en"):"en").toLowerCase() ||  "en";
	if(!this.langData[this.lang]){
		this.lang = "en";
	}
	
	this.i = {};
    e = e || null;
    if (typeof(l) == "string") {
        var g = document.getElementById(l)
    } else {
        var g = l
    }
    if (g && typeof(g) == "object" && g.tagName && String(g.tagName).toLowerCase() != "input") {
        e = g
    }
    g = null;
    if (typeof(l) != "object" || !l.length) {
        l = [l]
    }
    for (var c = 0; c < l.length; c++) {
        if (typeof(l[c]) == "string") {
            l[c] = (document.getElementById(l[c]) || null)
        }
        if (l[c] != null && l[c].tagName && String(l[c].tagName).toLowerCase() == "input") {
            this.i[window.dhx4.newId()] = {
                input: l[c]
            }
        } else {
            if (!(l[c] instanceof Array) && l[c] instanceof Object && (l[c].input != null || l[c].button != null)) {
                if (l[c].input != null && typeof(l[c].input) == "string") {
                    l[c].input = document.getElementById(l[c].input)
                }
                if (l[c].button != null && typeof(l[c].button) == "string") {
                    l[c].button = document.getElementById(l[c].button)
                }
                this.i[window.dhx4.newId()] = l[c]
            }
        }
        l[c] = null
    }
    this.conf = {
        skin: (s || window.dhx4.skin || (typeof(dhtmlx) != "undefined" ? dhtmlx.skin : null) || window.dhx4.skinDetect("dhtmlxcalendar") || "material"),
        zi: window.dhx4.newId(),
        touch: !window.dhx4.isIE,
        time: true,
        today: false,
        ws_first: true
    };
    this.setSkin = function(u, a) {
        if (this.conf.skin == u && !a) {
            return
        }
        this.conf.skin = u;
        this.base.className = "dhtmlxcalendar_" + this.conf.skin;
        this._ifrSize()
    };
    this.base = document.createElement("DIV");
    this.base.style.display = "none";
    this.base.appendChild(document.createElement("DIV"));
    if (e != null) {
        this._hasParent = true;
        e.appendChild(this.base);
        e = null
    } else {
        SUX.root.document.body.appendChild(this.base);
    }
    this.setParent = function(a) {
        if (this._hasParent) {
            if (typeof(a) == "object") {
                a.appendChild(this.base)
            } else {
                if (typeof(a) == "string") {
                    document.getElementById(a).appendChild(this.base)
                }
            }
        }
    };
    this.setSkin(this.conf.skin, true);
    this.base.onclick = function(a) {
        a = a || event;
        if (a.preventDefault) {
            a.preventDefault()
        }
        a.cancelBubble = true
    };
    this.base.onmousedown = function() {
        return false
    };
    if (this.conf.touch) {
        this.base.ontouchstart = this.base.onclick
    }
    this.loadUserLanguage = function(x) {
        if (!this.langData[x]) {
            return
        }
        this.lang = x;
        this.setWeekStartDay(this.langData[this.lang].weekstart);
        this.setDateFormat(this.langData[this.lang].dateformat || "%Y-%m-%d");
        if (this.msCont) {
            var v = 0;
            for (var u = 0; u < this.msCont.childNodes.length; u++) {
                for (var a = 0; a < this.msCont.childNodes[u].childNodes.length; a++) {
                    this.msCont.childNodes[u].childNodes[a].innerHTML = this.langData[this.lang].monthesSNames[v++]
                }
            }
        }
        this.contTime.childNodes[0].childNodes[0].childNodes[5].innerHTML = this.langData[this.lang].today;
        this.contTime.childNodes[0].childNodes[0].childNodes[4].innerHTML = this.langData[this.lang].clear
    };
    this.contMonth = document.createElement("DIV");
    this.contMonth.className = "dhtmlxcalendar_month_cont";
    this.contMonth.onselectstart = function(a) {
        a = a || event;
        a.cancelBubble = true;
        if (a.preventDefault) {
            a.preventDefault()
        } else {
            a.returnValue = false
        }
        return false
    };
    this.base.firstChild.appendChild(this.contMonth);
    var m = document.createElement("UL");
    m.className = "dhtmlxcalendar_line";
    this.contMonth.appendChild(m);
    var r = document.createElement("LI");
    r.className = "dhtmlxcalendar_cell dhtmlxcalendar_month_hdr";
    r.innerHTML = "<div class='dhtmlxcalendar_month_arrow dhtmlxcalendar_month_arrow_left' onmouseover='this.className=\"dhtmlxcalendar_month_arrow dhtmlxcalendar_month_arrow_left_hover\";' onmouseout='this.className=\"dhtmlxcalendar_month_arrow dhtmlxcalendar_month_arrow_left\";'></div><span></span><div class='dhtmlxcalendar_month_arrow dhtmlxcalendar_month_arrow_right' onmouseover='this.className=\"dhtmlxcalendar_month_arrow dhtmlxcalendar_month_arrow_right_hover\";' onmouseout='this.className=\"dhtmlxcalendar_month_arrow dhtmlxcalendar_month_arrow_right\";'></div>";
    m.appendChild(r);
    var h = this;
    r.onclick = function(w) {
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
    if (this.conf.touch == true) {
        r.ontouchstart = r.onclick
    }
    this.contDays = document.createElement("DIV");
    this.contDays.className = "dhtmlxcalendar_days_cont";
    this.base.firstChild.appendChild(this.contDays);
    this.setWeekStartDay = function(a) {
        if (a == 0) {
            a = 7
        }
        this._wStart = Math.min(Math.max((isNaN(a) ? 1 : a), 1), 7);
        this._drawDaysOfWeek()
    };
    this._drawDaysOfWeek = function() {
        if (this.contDays.childNodes.length == 0) {
            var x = document.createElement("UL");
            x.className = "dhtmlxcalendar_line";
            this.contDays.appendChild(x)
        } else {
            var x = this.contDays.firstChild
        }
        var u = this._wStart;
        var v = this.langData[this.lang].daysSNames;
        v.push(String(this.langData[this.lang].daysSNames[0]).valueOf());
        for (var y = 0; y < 8; y++) {
            if (x.childNodes[y] == null) {
                var a = document.createElement("LI");
                x.appendChild(a)
            } else {
                var a = x.childNodes[y]
            }
            if (y == 0) {
                a.className = "dhtmlxcalendar_cell_wn";
                a.innerHTML = "<div class='dhtmlxcalendar_label'>" + (this.langData[this.lang].weekname || "w") + "</div>"
            } else {
                a.className = "dhtmlxcalendar_cell" + (u >= 6 ? " dhtmlxcalendar_day_weekday_cell" : "") + (y == 1 ? "_first" : "");
                a.innerHTML = v[u];
                if (++u > 7) {
                    u = 1
                }
            }
        }
        if (this._activeMonth != null) {
            this._drawMonth(this._activeMonth)
        }
    };
    this._wStart = this.langData[this.lang].weekstart;
    this.setWeekStartDay(this._wStart);
    this.contDates = document.createElement("DIV");
    this.contDates.className = "dhtmlxcalendar_dates_cont";
    this.base.firstChild.appendChild(this.contDates);
    this.contDates.onclick = function(y) {
        y = y || event;
        if (y.type == "touchstart" && y.preventDefault != null) {
            y.preventDefault()
        }
        var u = (y.target || y.srcElement);
        if (u.parentNode != null && u.parentNode._date != null) {
            u = u.parentNode
        }
        if (u._date != null && !u._css_dis) {
            var w = h._activeDate.getHours();
            var v = h._activeDate.getMinutes();
            var x = u._date;
            if (h.checkEvent("onBeforeChange")) {
                if (!h.callEvent("onBeforeChange", [new Date(u._date.getFullYear(), u._date.getMonth(), u._date.getDate(), w, v)])) {
                    return
                }
            }
            if (h._activeDateCell != null) {
                h._activeDateCell._css_date = false;
                h._updateCellStyle(h._activeDateCell._q, h._activeDateCell._w)
            }
            var a = (h._activeDate.getFullYear() + "_" + h._activeDate.getMonth() != x.getFullYear() + "_" + x.getMonth());
            h._nullDate = false;
            h._activeDate = new Date(x.getFullYear(), x.getMonth(), x.getDate(), w, v);
            h._activeDateCell = u;
            h._activeDateCell._css_date = true;
            h._activeDateCell._css_hover = false;
            h._updateCellStyle(h._activeDateCell._q, h._activeDateCell._w);
            if (a) {
                h._drawMonth(h._activeDate)
            }
            h._updateInp();
            h._evOnClick([new Date(h._activeDate.getTime())]);
            if (!h._hasParent) {
                if (y.type == "touchstart") {
                    window.setTimeout(function() {
                        h._hide()
                    }, 400)
                } else {
                    h._hide()
                }
            }
        }
    };
    if (this.conf.touch == true) {
        this.contDates.ontouchstart = this.contDates.onclick
    }
    this.contDates.onmouseover = function(u) {
        u = u || event;
        var a = (u.target || u.srcElement);
        if (a.parentNode != null && a.parentNode._date != null) {
            a = a.parentNode
        }
        if (a._date != null) {
            if (h._lastHover == a || a._css_hover) {
                return
            }
            a._css_hover = true;
            h._updateCellStyle(a._q, a._w);
            h._lastHover = a;
            h._evOnMouseOver([new Date(a._date.getFullYear(), a._date.getMonth(), a._date.getDate(), 0, 0, 0, 0), u]);
            a = null
        }
    };
    this.contDates.onmouseout = function(a) {
        h._clearDayHover(a || event)
    };
    this._lastHover = null;
    this._clearDayHover = function(a) {
        if (!this._lastHover) {
            return
        }
        this._lastHover._css_hover = false;
        this._updateCellStyle(this._lastHover._q, this._lastHover._w);
        if (a != null) {
            h._evOnMouseOut([new Date(this._lastHover._date.getFullYear(), this._lastHover._date.getMonth(), this._lastHover._date.getDate(), 0, 0, 0, 0), a])
        }
        this._lastHover = null
    };
    for (var c = 0; c < 6; c++) {
        var m = document.createElement("UL");
        m.className = "dhtmlxcalendar_line";
        this.contDates.appendChild(m);
        for (var o = 0; o <= 7; o++) {
            var r = document.createElement("LI");
            if (o == 0) {
                r.className = "dhtmlxcalendar_cell_wn"
            } else {
                r.className = "dhtmlxcalendar_cell"
            }
            m.appendChild(r)
        }
    }
    this.contTime = document.createElement("DIV");
    this.contTime.className = "dhtmlxcalendar_time_cont";
    this.contTime.style.display = "none";
    this.base.firstChild.appendChild(this.contTime);
    this.showTime = function() {
        if (this.conf.time != true) {
            this.conf.time = true;
            this._adjustTimeCont()
        }
    };
    this.hideTime = function() {
        if (this.conf.time == true) {
            this.conf.time = false;
            this._adjustTimeCont()
        }
    };
    this.showToday = function() {
        if (this.conf.today != true) {
            this.conf.today = true;
            this._adjustTimeCont()
        }
    };
    this.hideToday = function() {
        if (this.conf.today == true) {
            this.conf.today = false;
            this._adjustTimeCont()
        }
    };
    this._adjustTimeCont = function() {
        var a = "";
        if (this.conf.time == true) {
            a += "_time"
        }
        if (this.conf.today == true) {
            a += "_today"
        }
        if (a == "") {
            this.contTime.style.display = "none"
        } else {
            this.contTime.className = "dhtmlxcalendar_time_cont dhtmlxcalendar_mode" + a;
            this.contTime.style.display = ""
        }
        this._ifrSize()
    };
    this._adjustTimeCont();
    var m = document.createElement("UL");
    m.className = "dhtmlxcalendar_line";
    this.contTime.appendChild(m);
    var r = document.createElement("LI");
    r.className = "dhtmlxcalendar_cell dhtmlxcalendar_time_hdr";
    r.innerHTML = "<div class='dhtmlxcalendar_time_img'></div><span class='dhtmlxcalendar_label_hours'></span><span class='dhtmlxcalendar_label_colon'>:</span><span class='dhtmlxcalendar_label_minutes'></span><span class='dhtmlxcalendar_label_clear'>" + this.langData[this.lang].clear + "</span><span class='dhtmlxcalendar_label_today'>" + this.langData[this.lang].today + "</span>";
    m.appendChild(r);
    r.onclick = function(v) {
        v = v || event;
        if (v.type == "touchstart" && v.preventDefault != null) {
            v.preventDefault()
        }
        var a = (v.target || v.srcElement);
        if (a.tagName != null && a.tagName.toLowerCase() == "span" && a._par == true && a.parentNode != null) {
            a = a.parentNode
        }
        if (a.className && a.className == "dhtmlxcalendar_label_hours") {
            v.cancelBubble = true;
            var u = h.contMonth.offsetHeight + h.contDays.offsetHeight + h.contDates.offsetHeight + a.offsetTop;
            h._showSelector("hours", Math.round(a.offsetLeft + a.offsetWidth / 2), u - 2, "selector_hours", true);
            return
        }
        if (a.className && a.className == "dhtmlxcalendar_label_minutes") {
            v.cancelBubble = true;
            if (h._minutesInterval == 1) {
                var w = h.getFormatedDate("%i");
                a.innerHTML = "<span class='dhtmlxcalendar_selected_date'>" + w.charAt(0) + "</span>" + w.charAt(1);
                a.firstChild._par = true;
                h._selectorMode = 1
            }
            var u = h.contMonth.offsetHeight + h.contDays.offsetHeight + h.contDates.offsetHeight + a.offsetTop;
            h._showSelector("minutes", Math.round(a.offsetLeft + a.offsetWidth / 2), u - 2, "selector_minutes", true);
            return
        }
        h._hideSelector();
        if (a.className && a.className == "dhtmlxcalendar_label_today") {
            var w = new Date();
            w.setHours(h._activeDate.getHours());
            w.setMinutes(h._activeDate.getMinutes());
            w.setSeconds(h._activeDate.getSeconds());
            h.setDate(w);
            h._updateInp();
            h.callEvent("onButtonClick", [w])
        }
        if (a.className && a.className == "dhtmlxcalendar_label_clear") {
            h._nullDate = true;
            h._drawMonth(new Date());
            h._updateInp();
            h.callEvent("onButtonClick", [null])
        }
    };
    if (this.conf.touch == true) {
        r.ontouchstart = r.onclick
    }
    this._activeMonth = null;
    this._activeDate = new Date();
    this._activeDateCell = null;
    this.setDate = function(u) {
        window.dhx4.temp_calendar = {
            tz: null
        };
        this._nullDate = (typeof(u) == "undefined" || u === "" || !u);
        if (!(u instanceof Date)) {
            u = this._strToDate(String(u || ""));
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
    this.getDate = function(v) {
        if (this._nullDate) {
            return null
        }
        var a = new Date(this._activeDate.getTime());
        if (v) {
            window.dhx4.temp_calendar = {
                tz: this.conf.tz
            };
            var u = this._dateToStr(a);
            window.dhx4.temp_calendar = null;
            return u
        }
        return a
    };
    this._drawMonth = function(F) {
        if (!(F instanceof Date)) {
            return
        }
        if (isNaN(F.getFullYear())) {
            F = new Date(this._activeMonth.getFullYear(), this._activeMonth.getMonth(), 1, 0, 0, 0, 0)
        }
        this._activeMonth = new Date(F.getFullYear(), F.getMonth(), 1, 0, 0, 0, 0);
        this._activeDateCell = null;
        var D = new Date(this._activeMonth.getTime());
        var v = D.getDay();
        var J = v - this._wStart;
        if (J < 0) {
            J = J + 7
        }
        D.setDate(D.getDate() - J);
        var M = F.getMonth();
        var N = new Date(this._activeDate.getFullYear(), this._activeDate.getMonth(), this._activeDate.getDate(), 0, 0, 0, 0).getTime();
        var A = 0;
        for (var u = 0; u < 6; u++) {
            var E = this._wStart;
            for (var L = 0; L <= 7; L++) {
                if (L == 0) {
                    var K = this.getWeekNumber(new Date(D.getFullYear(), D.getMonth(), D.getDate() + A, 0, 0, 0, 0));
                    if (K >= 52 && this.conf.ws_first == true && this._activeMonth.getMonth() == 0) {
                        var y = this.getWeekNumber(new Date(D.getFullYear(), D.getMonth(), D.getDate() + A + 7, 0, 0, 0, 0));
                        if (y < K && y > 1) {
                            K = 1
                        }
                    }
                    this.contDates.childNodes[u].childNodes[L].innerHTML = "<div class='dhtmlxcalendar_label'>" + K + "</div>"
                } else {
                    var a = new Date(D.getFullYear(), D.getMonth(), D.getDate() + A, 0, 0, 0, 0);
                    if (a.getHours() != 0) {
                        var C = (a.getHours() > 12 ? 24 - a.getHours() : a.getHours());
                        a.setTime(a.getTime() + 60 * 60 * 1000 * C)
                    }
                    var I = a.getDay();
                    var x = a.getTime();
                    var H = "dhtmlxcalendar_label";
                    if (this._tipData[x] != null) {
                        if (this._tipData[x].usePopup && typeof(window.dhtmlXPopup) == "function") {
                            this.contDates.childNodes[u].childNodes[L].removeAttribute("title");
                            this._initTooltipPopup()
                        } else {
                            this.contDates.childNodes[u].childNodes[L].setAttribute("title", this._tipData[x].text)
                        }
                        if (this._tipData[x].showIcon) {
                            H += " dhtmlxcalendar_label_title"
                        }
                    } else {
                        this.contDates.childNodes[u].childNodes[L].removeAttribute("title")
                    }
                    this.contDates.childNodes[u].childNodes[L].innerHTML = "<div class='" + H + "'>" + a.getDate() + "</div>";
                    this.contDates.childNodes[u].childNodes[L]._date = new Date(x);
                    this.contDates.childNodes[u].childNodes[L]._q = u;
                    this.contDates.childNodes[u].childNodes[L]._w = L;
                    this.contDates.childNodes[u].childNodes[L]._css_month = (a.getMonth() == M);
                    this.contDates.childNodes[u].childNodes[L]._css_date = (!this._nullDate && x == N);
                    this.contDates.childNodes[u].childNodes[L]._css_weekend = (E >= 6);
                    this.contDates.childNodes[u].childNodes[L]._css_dis = this._isOutOfRange(x);
                    this.contDates.childNodes[u].childNodes[L]._css_holiday = (this._holidays[x] == true);
                    this._updateCellStyle(u, L);
                    if (x == N) {
                        this._activeDateCell = this.contDates.childNodes[u].childNodes[L]
                    }
                    if (++E > 7) {
                        E = 1
                    }
                    A++
                }
            }
        }
        this.contMonth.firstChild.firstChild.childNodes[1].innerHTML = this._buildMonthHdr(F)
        this.contMonth._dates = F;
    };
    this._updateCellStyle = function(x, a) {
        var v = this.contDates.childNodes[x].childNodes[a];
        var u = "dhtmlxcalendar_cell dhtmlxcalendar_cell";
        u += (v._css_month ? "_month" : "");
        u += (v._css_date ? "_date" : "");
        u += (v._css_weekend ? "_weekend" : "");
        u += (v._css_holiday ? "_holiday" : "");
        u += (v._css_dis ? "_dis" : "");
        u += (v._css_hover && !v._css_dis ? "_hover" : "");
        v.className = u;
        v = null
    };
    this._minutesInterval = 5;
    this._initSelector = function(D, x) {
        if (!this._selCover) {
            this._selCover = document.createElement("DIV");
            this._selCover.className = "dhtmlxcalendar_selector_cover";
            this.base.firstChild.appendChild(this._selCover)
        }
        if (!this._sel) {
            this._sel = document.createElement("DIV");
            this._sel.className = "dhtmlxcalendar_selector_obj";
            this.base.firstChild.appendChild(this._sel);
            this._sel.appendChild(document.createElement("TABLE"));
            this._sel.firstChild.className = "dhtmlxcalendar_selector_table";
            this._sel.firstChild.cellSpacing = 0;
            this._sel.firstChild.cellPadding = 0;
            this._sel.firstChild.border = 0;
            this._sel.firstChild.appendChild(document.createElement("TBODY"));
            this._sel.firstChild.firstChild.appendChild(document.createElement("TR"));
            this._sel.firstChild.firstChild.firstChild.appendChild(document.createElement("TD"));
            this._sel.firstChild.firstChild.firstChild.appendChild(document.createElement("TD"));
            this._sel.firstChild.firstChild.firstChild.appendChild(document.createElement("TD"));
            this._sel.firstChild.firstChild.firstChild.childNodes[0].className = "dhtmlxcalendar_selector_cell_left";
            this._sel.firstChild.firstChild.firstChild.childNodes[1].className = "dhtmlxcalendar_selector_cell_middle";
            this._sel.firstChild.firstChild.firstChild.childNodes[2].className = "dhtmlxcalendar_selector_cell_right";
            this._sel.firstChild.firstChild.firstChild.childNodes[0].innerHTML = "&nbsp;";
            this._sel.firstChild.firstChild.firstChild.childNodes[2].innerHTML = "&nbsp;";
            this._sel.firstChild.firstChild.firstChild.childNodes[0].onmouseover = function() {
                this.className = "dhtmlxcalendar_selector_cell_left dhtmlxcalendar_selector_cell_left_hover"
            };
            this._sel.firstChild.firstChild.firstChild.childNodes[0].onmouseout = function() {
                this.className = "dhtmlxcalendar_selector_cell_left"
            };
            this._sel.firstChild.firstChild.firstChild.childNodes[2].onmouseover = function() {
                this.className = "dhtmlxcalendar_selector_cell_right dhtmlxcalendar_selector_cell_right_hover"
            };
            this._sel.firstChild.firstChild.firstChild.childNodes[2].onmouseout = function() {
                this.className = "dhtmlxcalendar_selector_cell_right"
            };
            this._sel.onmouseover = function(H) {
                H = H || event;
                var w = (H.target || H.srcElement);
                if (w._cell === true) {
                    if (h._selHover != w) {
                        h._clearSelHover()
                    }
                    if (String(w.className).match(/^\s{0,}dhtmlxcalendar_selector_cell\s{0,}$/gi) != null) {
                        w.className += " dhtmlxcalendar_selector_cell_hover";
                        h._selHover = w
                    }
                }
            };
            this._sel.onmouseout = function() {
                h._clearSelHover()
            };
            this._sel.firstChild.firstChild.firstChild.childNodes[0].onclick = function(w) {
                w = w || event;
                if (w.type == "touchstart" && w.preventDefault != null) {
                    w.preventDefault()
                }
                w.cancelBubble = true;
                h._scrollYears(-5)
            };
            this._sel.firstChild.firstChild.firstChild.childNodes[2].onclick = function(w) {
                w = w || event;
                if (w.type == "touchstart" && w.preventDefault != null) {
                    w.preventDefault()
                }
                w.cancelBubble = true;
                h._scrollYears(8)
            };
            if (this.conf.touch == true) {
                this._sel.firstChild.firstChild.firstChild.childNodes[0].ontouchstart = this._sel.firstChild.firstChild.firstChild.childNodes[0].onclick;
                this._sel.firstChild.firstChild.firstChild.childNodes[2].ontouchstart = this._sel.firstChild.firstChild.firstChild.childNodes[2].onclick
            }
            this._sel._ta = {};
            this._selHover = null;
            this._sel.appendChild(document.createElement("DIV"));
            this._sel.lastChild.className = "dhtmlxcalendar_selector_obj_arrow"
        }
        if (this._sel._ta[D] == true) {
            return
        }
        if (D == "month") {
            this._msCells = {};
            this.msCont = document.createElement("DIV");
            this.msCont.className = "dhtmlxcalendar_area_" + x;
            this._sel.firstChild.firstChild.firstChild.childNodes[1].appendChild(this.msCont);
            var u = 0;
            for (var a = 0; a < 4; a++) {
                var C = document.createElement("UL");
                C.className = "dhtmlxcalendar_selector_line";
                this.msCont.appendChild(C);
                for (var E = 0; E < 3; E++) {
                    var F = document.createElement("LI");
                    F.innerHTML = this.langData[this.lang].monthesSNames[u];
                    F.className = "dhtmlxcalendar_selector_cell";
                    C.appendChild(F);
                    F._month = u;
                    F._cell = true;
                    this._msCells[u++] = F
                }
            }
            this.msCont.onclick = function(H) {
                H = H || event;
                if (H.type == "touchstart" && H.preventDefault != null) {
                    H.preventDefault()
                }
                H.cancelBubble = true;
                var w = (H.target || H.srcElement);
                if (w._month != null) {
                    h._hideSelector();
                    h._updateActiveMonth();
                    h._drawMonth(new Date(h._activeMonth.getFullYear(), w._month, 1, 0, 0, 0, 0));
                    h._doOnSelectorChange()
                    h.callEvent("onMonthSelect", [])
                }
            };
            if (this.conf.touch == true) {
                this.msCont.ontouchstart = this.msCont.onclick
            }
        }
        if (D == "year") {
            this._ysCells = {};
            this.ysCont = document.createElement("DIV");
            this.ysCont.className = "dhtmlxcalendar_area_" + x;
            this._sel.firstChild.firstChild.firstChild.childNodes[1].appendChild(this.ysCont);
            for (var a = 0; a < 4; a++) {
                var C = document.createElement("UL");
                C.className = "dhtmlxcalendar_selector_line";
                this.ysCont.appendChild(C);
                for (var E = 0; E < 3; E++) {
                    var F = document.createElement("LI");
                    F.className = "dhtmlxcalendar_selector_cell";
                    F._cell = true;
                    C.appendChild(F)
                }
            }
            this.ysCont.onclick = function(H) {
                H = H || event;
                if (H.type == "touchstart" && H.preventDefault != null) {
                    H.preventDefault()
                }
                H.cancelBubble = true;
                var w = (H.target || H.srcElement);
                if (w._year != null) {
                    h._hideSelector();
                    h._drawMonth(new Date(w._year, h._activeMonth.getMonth(), 1, 0, 0, 0, 0));
                    h._doOnSelectorChange()
                }
            };
            if (this.conf.touch == true) {
                this.ysCont.ontouchstart = this.ysCont.onclick
            }
        }
        if (D == "hours") {
            this._hsCells = {};
            this.hsCont = document.createElement("DIV");
            this.hsCont.className = "dhtmlxcalendar_area_" + x;
            this._sel.firstChild.firstChild.firstChild.childNodes[1].appendChild(this.hsCont);
            var u = 0;
            for (var a = 0; a < 4; a++) {
                var C = document.createElement("UL");
                C.className = "dhtmlxcalendar_selector_line";
                this.hsCont.appendChild(C);
                for (var E = 0; E < 6; E++) {
                    var F = document.createElement("LI");
                    F.innerHTML = this._fixLength(u, 2);
                    F.className = "dhtmlxcalendar_selector_cell";
                    C.appendChild(F);
                    F._hours = u;
                    F._cell = true;
                    this._hsCells[u++] = F
                }
            }
            this.hsCont.onclick = function(H) {
                H = H || event;
                if (H.type == "touchstart" && H.preventDefault != null) {
                    H.preventDefault()
                }
                H.cancelBubble = true;
                var w = (H.target || H.srcElement);
                if (w._hours != null) {
                    h._hideSelector();
                    h._activeDate.setHours(w._hours);
                    h._updateActiveHours();
                    h._updateVisibleHours();
                    h._doOnSelectorChange();
                    h.callEvent("onTimeChange", [new Date(h._activeDate.getTime())])
                }
            };
            if (this.conf.touch == true) {
                this.hsCont.ontouchstart = this.hsCont.onclick
            }
        }
        if (D == "minutes") {
            var A = 4;
            var v = 3;
            var y = 2;
            if (this._minutesInterval == 1) {
                if (this._selectorMode == 1) {
                    A = 2;
                    v = 3;
                    y = 1
                } else {
                    A = 2;
                    v = 5;
                    y = 1;
                    x += "5"
                }
            }
            if (this._minutesInterval == 10) {
                A = 2
            }
            if (this._minutesInterval == 15) {
                A = 1;
                v = 4;
                x += "4"
            }
            this._rsCells = {};
            this.rsCont = document.createElement("DIV");
            this.rsCont.className = "dhtmlxcalendar_area_" + x;
            this._sel.firstChild.firstChild.firstChild.childNodes[1].appendChild(this.rsCont);
            var u = 0;
            for (var a = 0; a < A; a++) {
                var C = document.createElement("UL");
                C.className = "dhtmlxcalendar_selector_line";
                this.rsCont.appendChild(C);
                for (var E = 0; E < v; E++) {
                    var F = document.createElement("LI");
                    F.innerHTML = (y > 1 ? this._fixLength(u, y) : u);
                    F.className = "dhtmlxcalendar_selector_cell";
                    C.appendChild(F);
                    F._minutes = u;
                    F._cell = true;
                    this._rsCells[u] = F;
                    u += this._minutesInterval
                }
            }
            this.rsCont.onclick = function(I) {
                I = I || event;
                if (I.type == "touchstart" && I.preventDefault != null) {
                    I.preventDefault()
                }
                I.cancelBubble = true;
                var H = (I.target || I.srcElement);
                if (H._minutes != null) {
                    if (h._minutesInterval == 1) {
                        var w = h.getFormatedDate("%i");
                        if (h._selectorMode == 1) {
                            w = H._minutes.toString() + w.charAt(1)
                        } else {
                            w = w.charAt(0) + H._minutes.toString()
                        }
                        h._activeDate.setMinutes(Number(w));
                        h.callEvent("onTimeChange", [new Date(h._activeDate.getTime())]);
                        h._hideSelector();
                        if (h._selectorMode == 1) {
                            h._updateVisibleMinutes(true);
                            h._selectorMode = 2;
                            h._showSelector("minutes", h._sel._x, h._sel._y, "selector_minutes", true);
                            h._updateActiveMinutes();
                            return
                        } else {
                            h._selectorMode = 1
                        }
                    } else {
                        h._hideSelector();
                        h._activeDate.setMinutes(H._minutes);
                        h._updateActiveMinutes();
                        h.callEvent("onTimeChange", [new Date(h._activeDate.getTime())])
                    }
                    h._updateVisibleMinutes();
                    h._doOnSelectorChange()
                }
            };
            if (this.conf.touch == true) {
                this.rsCont.ontouchstart = this.rsCont.onclick
            }
        }
        this._sel._ta[D] = true
    };
    this._showSelector = function(A, u, C, w, a) {
        if (a === true && this._sel != null && this._isSelectorVisible() && A == this._sel._t) {
            this._hideSelector();
            return
        }
        if (this.conf.skin == "dhx_terrace") {
            u += 12
        }
        if (!this._sel || !this._sel._ta[A]) {
            this._initSelector(A, w)
        }
        if (A != this._sel._t && this._sel._t == "minutes" && this._minutesInterval == 1) {
            this.contTime.firstChild.firstChild.childNodes[3].innerHTML = this.getFormatedDate("%i")
        }
        this._sel._x = u;
        this._sel._y = C;
        this._sel.style.visibility = "hidden";
        this._sel.style.display = "";
        this._selCover.style.width = this.base.offsetWidth - 2 + "px";
        this._selCover.style.top = this.contMonth.offsetHeight + "px";
        this._selCover.style.height = this.contDates.offsetHeight + this.contDays.offsetHeight - 1 + "px";
        this._selCover.style.display = "";
        this._sel._t = A;
        this._sel.className = "dhtmlxcalendar_selector_obj dhtmlxcalendar_" + w + (A == "hours" && this.conf.today == true ? "2" : "");
        this._sel.childNodes[0].firstChild.firstChild.childNodes[0].style.display = this._sel.childNodes[0].firstChild.firstChild.childNodes[2].style.display = (A == "year" ? "" : "none");
        var v = Math.max(0, u - Math.round(this._sel.offsetWidth / 2));
        if (v + this._sel.offsetWidth > this._sel.parentNode.offsetWidth) {
            v = this._sel.parentNode.offsetWidth - this._sel.offsetWidth
        }
        this._sel.style.left = v + "px";
        if (A == "hours" || A == "minutes") {
            this._sel.style.top = C - this._sel.offsetHeight + "px"
        } else {
            this._sel.style.top = C + "px"
        }
        this._sel.childNodes[1].style.width = this._sel.childNodes[0].offsetWidth + "px";
        this._sel.style.visibility = "visible";
        this._doOnSelectorShow(A)
    };
    this._doOnSelectorShow = function(a) {
        if (a == "month") {
            this._updateActiveMonth()
        }
        if (a == "year") {
            this._updateYearsList(this._activeMonth)
        }
        if (a == "hours") {
            this._updateActiveHours()
        }
        if (a == "minutes") {
            this._updateActiveMinutes()
        }
    };
    this._hideSelector = function(a) {
        if (!this._sel) {
            return
        }
        this._sel.style.display = "none";
        this._sel.style.visible = "hidden";
        this._selCover.style.display = "none";
        if (this._sel._t == "minutes" && this._minutesInterval == 1) {
            this.contTime.firstChild.firstChild.childNodes[3].innerHTML = this.getFormatedDate("%i");
            this._unloadSelector("minutes")
        }
    };
    this._isSelectorVisible = function() {
        if (!this._sel) {
            return false
        }
        return (this._sel.style.display != "none")
    };
    this._doOnSelectorChange = function(a) {
        this.callEvent("onChange", [new Date(this._activeMonth.getFullYear(), this._activeMonth.getMonth(), this._activeDate.getDate(), this._activeDate.getHours(), this._activeDate.getMinutes(), this._activeDate.getSeconds()), a === true])
    };
    this._clearSelHover = function() {
        if (!this._selHover) {
            return
        }
        this._selHover.className = String(this._selHover.className.replace(/dhtmlxcalendar_selector_cell_hover/gi, ""));
        this._selHover = null
    };
    this._unloadSelector = function(v) {
        if (!this._sel) {
            return
        }
        if (!this._sel._ta[v]) {
            return
        }
        if (v == "month") {
            this.msCont.onclick = this.msCont.ontouchstart = null;
            this._msActive = null;
            for (var u in this._msCells) {
                this._msCells[u]._cell = null;
                this._msCells[u]._month = null;
                this._msCells[u].parentNode.removeChild(this._msCells[u]);
                this._msCells[u] = null
            }
            this._msCells = null;
            while (this.msCont.childNodes.length > 0) {
                this.msCont.removeChild(this.msCont.lastChild)
            }
            this.msCont.parentNode.removeChild(this.msCont);
            this.msCont = null
        }
        if (v == "year") {
            this.ysCont.onclick = this.ysCont.ontouchstart = null;
            for (var u in this._ysCells) {
                this._ysCells[u]._cell = null;
                this._ysCells[u]._year = null;
                this._ysCells[u].parentNode.removeChild(this._ysCells[u]);
                this._ysCells[u] = null
            }
            this._ysCells = null;
            while (this.ysCont.childNodes.length > 0) {
                this.ysCont.removeChild(this.ysCont.lastChild)
            }
            this.ysCont.parentNode.removeChild(this.ysCont);
            this.ysCont = null
        }
        if (v == "hours") {
            this.hsCont.onclick = this.hsCont.ontouchstart = null;
            this._hsActive = null;
            for (var u in this._hsCells) {
                this._hsCells[u]._cell = null;
                this._hsCells[u]._hours = null;
                this._hsCells[u].parentNode.removeChild(this._hsCells[u]);
                this._hsCells[u] = null
            }
            this._hsCells = null;
            while (this.hsCont.childNodes.length > 0) {
                this.hsCont.removeChild(this.hsCont.lastChild)
            }
            this.hsCont.parentNode.removeChild(this.hsCont);
            this.hsCont = null
        }
        if (v == "minutes") {
            this.rsCont.onclick = this.rsCont.ontouchstart = null;
            this._rsActive = null;
            for (var u in this._rsCells) {
                this._rsCells[u]._cell = null;
                this._rsCells[u]._minutes = null;
                this._rsCells[u].parentNode.removeChild(this._rsCells[u]);
                this._rsCells[u] = null
            }
            this._rsCells = null;
            while (this.rsCont.childNodes.length > 0) {
                this.rsCont.removeChild(this.rsCont.lastChild)
            }
            this.rsCont.parentNode.removeChild(this.rsCont);
            this.rsCont = null
        }
        this._sel._ta[v] = null
    };
    this.setMinutesInterval = function(a) {
        if (!(a == 1 || a == 5 || a == 10 || a == 15)) {
            return
        }
        this._minutesInterval = a;
        this._unloadSelector("minutes")
    };
    this._updateActiveMonth = function() {
        if (typeof(this._msActive) != "undefined" && typeof(this._msCells[this._msActive]) != "undefined") {
            this._msCells[this._msActive].className = "dhtmlxcalendar_selector_cell"
        }
        this._msActive = this._activeMonth.getMonth();
        this._msCells[this._msActive].className = "dhtmlxcalendar_selector_cell dhtmlxcalendar_selector_cell_active"
    };
    this._updateActiveYear = function() {
        var a = this._activeMonth.getFullYear();
        if (this._ysCells[a]) {
            this._ysCells[a].className = "dhtmlxcalendar_selector_cell dhtmlxcalendar_selector_cell_active"
        }
    };
    this._updateYearsList = function(A) {
        for (var v in this._ysCells) {
            this._ysCells[v] = null;
            delete this._ysCells[v]
        }
        var x = A.getFullYear()-7;
        for (var y = 0; y < 4; y++) {
            for (var u = 0; u < 3; u++) {
                this.ysCont.childNodes[y].childNodes[u].innerHTML = x;
                this.ysCont.childNodes[y].childNodes[u]._year = x;
                this.ysCont.childNodes[y].childNodes[u].className = "dhtmlxcalendar_selector_cell";
                this._ysCells[x++] = this.ysCont.childNodes[y].childNodes[u]
            }
        }
        this._updateActiveYear()
    };
    this._scrollYears = function(a) {
        var v = (a < 0 ? this.ysCont.firstChild.firstChild._year : this.ysCont.lastChild.lastChild._year) + a;
        var u = new Date(v, this._activeMonth.getMonth(), 1, 0, 0, 0, 0);
        this._updateYearsList(u)
    };
    this._updateActiveHours = function() {
        if (typeof(this._hsActive) != "undefined" && typeof(this._hsCells[this._hsActive]) != "undefined") {
            this._hsCells[this._hsActive].className = "dhtmlxcalendar_selector_cell"
        }
        this._hsActive = this._activeDate.getHours();
        this._hsCells[this._hsActive].className = "dhtmlxcalendar_selector_cell dhtmlxcalendar_selector_cell_active"
    };
    this._updateVisibleHours = function() {
        this.contTime.firstChild.firstChild.childNodes[1].innerHTML = this._fixLength(this._activeDate.getHours(), 2)
    };
    this._updateActiveMinutes = function() {
        if (this._rsActive != null && typeof(this._rsActive) != "undefined" && typeof(this._rsCells[this._rsActive]) != "undefined") {
            this._rsCells[this._rsActive].className = "dhtmlxcalendar_selector_cell"
        }
        if (this._minutesInterval == 1) {
            this._rsActive = (this.getFormatedDate("%i").toString()).charAt(this._selectorMode == 1 ? 0 : 1)
        } else {
            this._rsActive = this._activeDate.getMinutes()
        }
        if (typeof(this._rsCells[this._rsActive]) != "undefined") {
            this._rsCells[this._rsActive].className = "dhtmlxcalendar_selector_cell dhtmlxcalendar_selector_cell_active"
        }
    };
    this._updateVisibleMinutes = function(u) {
        var a = this._fixLength(this._activeDate.getMinutes(), 2).toString();
        if (u == true) {
            a = a.charAt(0) + "<span class='dhtmlxcalendar_selected_date'>" + a.charAt(1) + "</span>"
        }
        this.contTime.firstChild.firstChild.childNodes[3].innerHTML = a;
        if (u == true) {
            this.contTime.firstChild.firstChild.childNodes[3].lastChild._par = true
        }
    };
    this._fixLength = function(a, u) {
        while (String(a).length < u) {
            a = "0" + String(a)
        }
        return a
    };
    this._dateFormat = "";
    this._dateFormatRE = null;
    this.setDateFormat = function(x) {
        var w = {};
        if (this._strToDate != null) {
            for (var u in this.i) {
                if (this.i[u].input != null && this.i[u].input.value.length > 0) {
                    var y = this._strToDate(this.i[u].input.value, this._dateFormat || this.langData[this.lang].dateformat || "%Y-%m-%d");
                    if (y instanceof Date) {
                        w[u] = y
                    }
                }
            }
        }
        this._dateFormat = x;
        var v = String(this._dateFormat).replace(/%[a-zA-Z]+/g, function(a) {
            var A = a.replace(/%/, "");
            switch (A) {
                case "n":
                case "h":
                case "j":
                case "g":
                case "G":
                    return "\\d{1,2}";
                case "m":
                case "d":
                case "H":
                case "i":
                case "s":
                case "y":
                    return "\\d{2}";
                case "Y":
                    return "\\d{4}";
                case "M":
                    return "(" + h.langData[h.lang].monthesSNames.join("|").toLowerCase() + "){1,}";
                case "F":
                    return "(" + h.langData[h.lang].monthesFNames.join("|").toLowerCase() + "){1,}";
                case "D":
                    return "[a-z]{2}";
                case "a":
                case "A":
                    return "AM|PM";
                case "u":
                    return "\\d{1,6}";
                case "P":
                    return "[\\+\\-]\\d{1,2}\\:\\d{1,2}"
            }
            return a
        });
        this._dateFormatRE = new RegExp(v, "i");
        for (var u in w) {
            this.i[u].input.value = this._dateToStr(w[u])
        }
        w = null
    };
    this.setDateFormat(this.langData[this.lang].dateformat || "%Y-%m-%d");
    this._updateDateStr = function(u) {
        if (!this._dateFormatRE || !u.match(this._dateFormatRE)) {
            return
        }
        if (u == this.getFormatedDate()) {
            return
        }
        var a = this._strToDate(u);
        if (!(a instanceof Date)) {
            return
        }
        if (this.checkEvent("onBeforeChange")) {
            if (!this.callEvent("onBeforeChange", [new Date(a.getFullYear(), a.getMonth(), a.getDate(), a.getHours(), a.getMinutes(), a.getSeconds())])) {
                this._updateInp();
                return
            }
        }
        this._nullDate = false;
        this._activeDate = a;
        this._drawMonth(this._nullDate ? new Date() : this._activeDate);
        this._updateVisibleMinutes();
        this._updateVisibleHours();
        if (this._sel && this._isSelectorVisible()) {
            this._doOnSelectorShow(this._sel._t)
        }
        this._doOnSelectorChange(true)
    };
    this.showMonth = function(a) {
        if (typeof(a) == "string") {
            a = this._strToDate(a)
        }
        if (!(a instanceof Date)) {
            return
        }
        this._drawMonth(a)
    };
    this.setFormatedDate = function(x, y, u, w) {
        var v = this._strToDate(y, x);
        if (w) {
            return v
        }
        this.setDate(v)
    };
    this.getFormatedDate = function(u, a) {
        if (!(a && a instanceof Date)) {
            if (this._nullDate) {
                return ""
            }
            a = new Date(this._activeDate)
        }
        return this._dateToStr(a, u)
    };
    this.getWeekNumber = function(y) {
        if (typeof(y) == "string") {
            y = this._strToDate(y)
        }
        if (!(y instanceof Date)) {
            return "Invalid Date"
        }
        if (typeof(this._ftDay) == "undefined") {
            this._ftDay = 4
        }
        var H = this._wStart;
        var x = H + 7;
        var F = 4;
        var v = new Date(y.getFullYear(), 0, 1, 0, 0, 0, 0);
        var w = v.getDay();
        if (w == 0) {
            w = 7
        }
        if (F < H) {
            F += 7;
            w += 7
        }
        var D = 0;
        if (w >= H && w <= F) {} else {
            D = 1
        }
        var C = w - H;
        var E = new Date(y.getFullYear(), 0, 1 - C + D * 7, 0, 0, 0, 0);
        var J = 604800000;
        var u = new Date(y.getFullYear(), y.getMonth(), y.getDate() + 1, 0, 0, 0, 0);
        var I = Math.ceil((u.getTime() - E.getTime()) / J);
        if (I == 0) {
            var A = new Date(y.getFullYear(), y.getMonth(), y.getDate(), 0, 0, 0, 0);
            var a = A.getDay() - this._wStart;
            if (a < 0) {
                a = a + 7
            }
            A.setDate(A.getDate() - a);
            I = this.getWeekNumber(A)
        }
        return I
    };
    this.showWeekNumbers = function() {
        this.base.firstChild.className = "dhtmlxcalendar_wn"
    };
    this.hideWeekNumbers = function() {
        this.base.firstChild.className = ""
    };
    this.show = function(w) {
        if (!w && this._hasParent) {
            this._show();
            return
        }
        if (typeof(w) == "string") {
            var v = document.getElementById(w);
            if (v != null && typeof(v._dhtmlxcalendar_uid) != "undefined" && this.i[v._dhtmlxcalendar_uid] != null) {
                this._show(v._dhtmlxcalendar_uid);
                return
            }
        }
        if (typeof(w) == "object" && typeof(w._dhtmlxcalendar_uid) != "undefined" && this.i[w._dhtmlxcalendar_uid] == w) {
            this._show(w._dhtmlxcalendar_uid);
            return
        }
        if (typeof(w) == "undefined") {
            for (var u in this.i) {
                if (!w) {
                    w = u
                }
            }
        }
        if (!w) {
            return
        }
        this._show(w)
    };
    this.hide = function() {
        if (this._isVisible()) {
            this._hide()
        }
    };
    this.isVisible = function() {
        return this._isVisible()
    };
    this._activeInp = null;
    this.pos = "bottom";
    this.setPosition = function(a, u) {
        this._px = null;
        this._py = null;
        if (a == "right" || a == "bottom") {
            this.pos = a
        } else {
            this.pos = "int";
            if (typeof(a) != "undefined" && !isNaN(a)) {
                this.base.style.left = a + "px";
                this._px = a
            }
            if (typeof(u) != "undefined" && !isNaN(u)) {
                this.base.style.top = u + "px";
                this._py = u
            }
            this._ifrSize()
        }
    };
    this._show = function(C, u) {
        if (u === true && this._activeInp == C && this._isVisible()) {
            this._hide();
            return
        }
        this.base.style.visibility = "hidden";
        this.base.style.display = "";
        if (!C) {
            if (this._px != null && this._py != null) {
                this.base.style.left = this._px + "px";
                this.base.style.top = this._py + "px"
            } else {
                this.base.style.left = "0px";
                this.base.style.top = "0px"
            }
        } else {
            if (this.base.className.indexOf("dhtmlxcalendar_in_input") == -1) {
                this.base.className += " dhtmlxcalendar_in_input"
            }
            var v = (this.i[C].input || this.i[C].button);
            var D = (Sepoa.request.isPopup)?SUX.getModuleTop().parent.SUX.getModuleTop().dhx4.screenDim():SUX.getModuleTop().dhx4.screenDim();
            var A = sUtils.element.getOffset(v, "module.over");
            A.left = A.left - $(window).scrollLeft();
            A.top = A.top - $(window).scrollTop();
            
            if (this.pos == "right") {
                this.base.style.left = A.left + v.offsetWidth + "px";
                this.base.style.top = Math.min(A.top, D.bottom - this.base.offsetHeight) + "px"
            } else {
                if (this.pos == "bottom") {
                    var y = A.top + v.offsetHeight + 1;
                    if (y + this.base.offsetHeight > D.bottom) {
                        var w = A.top - this.base.offsetHeight;
                        if (w >= -20) {
                            y = w
                        }
                    }
                    var a = A.left;
                    if (a + this.base.offsetWidth > D.right) {
                        a = Math.max(0, D.right - this.base.offsetWidth)
                    }
                    this.base.style.left = a + "px";
                    this.base.style.top = y + "px"
                } else {
                    this.base.style.left = (this._px || 0) + "px";
                    this.base.style.top = (this._py || 0) + "px"
                }
            }
            this._activeInp = C;
            v = null
        }
        this._hideSelector();
        this.base.style.visibility = "visible";
        this.base.style.zIndex = "999";
        this._ifrSize();
        if (this._ifr) {
            this._ifr.style.display = ""
        }
        this.callEvent("onShow", [])
    };
    this._hide = function() {
        if (this._lastHover != null) {
            this._clearDayHover()
        }
        this._hideSelector();
        this.base.style.display = "none";
        window.dhx4.zim.clear(this.conf.zi);
        if (this.base.className.indexOf("dhtmlxcalendar_in_input") >= 0) {
            this.base.className = this.base.className.replace(/\s{0,}dhtmlxcalendar_in_input/gi, "")
        }
        this._activeInp = null;
        if (this._ifr) {
            this._ifr.style.display = "none"
        }
        this.callEvent("onHide", [])
    };
    this._isVisible = function() {
        return (this.base.style.display != "none")
    };
    this._rangeActive = false;
    this._rangeFrom = null;
    this._rangeTo = null;
    this._rangeSet = {};
    this.setInsensitiveDays = function(v) {
        var a = this._extractDates(v);
        for (var u = 0; u < a.length; u++) {
            this._rangeSet[new Date(a[u].getFullYear(), a[u].getMonth(), a[u].getDate(), 0, 0, 0, 0).getTime()] = true
        }
        this._drawMonth(this._activeMonth)
    };
    this.clearInsensitiveDays = function() {
        this._clearRangeSet();
        this._drawMonth(this._activeMonth)
    };
    this._holidays = {};
    this.setHolidays = function(u) {
        if (u == null) {
            this._clearHolidays()
        } else {
            if (u != null) {
                var a = this._extractDates(u);
                for (var v = 0; v < a.length; v++) {
                    this._holidays[new Date(a[v].getFullYear(), a[v].getMonth(), a[v].getDate(), 0, 0, 0, 0).getTime()] = true
                }
            }
        }
        this._drawMonth(this._activeMonth)
    };
    this._extractDates = function(v) {
        if (typeof(v) == "string" || v instanceof Date) {
            v = [v]
        }
        var u = [];
        for (var x = 0; x < v.length; x++) {
            if (typeof(v[x]) == "string") {
                var y = v[x].split(",");
                for (var a = 0; a < y.length; a++) {
                    u.push(this._strToDate(y[a]))
                }
            } else {
                if (v[x] instanceof Date) {
                    u.push(v[x])
                }
            }
        }
        return u
    };
    this._clearRange = function() {
        this._rangeActive = false;
        this._rangeType = null;
        this._rangeFrom = null;
        this._rangeTo = null
    };
    this._clearRangeSet = function() {
        for (var u in this._rangeSet) {
            this._rangeSet[u] = null;
            delete this._rangeSet[u]
        }
    };
    this._clearHolidays = function() {
        for (var u in this._holidays) {
            this._holidays[u] = null;
            delete this._holidays[u]
        }
    };
    this._isOutOfRange = function(u) {
        if (this._rangeSet[u] == true) {
            return true
        }
        if (this._rangeActive) {
            if (this._rangeType == "in" && (u < this._rangeFrom || u > this._rangeTo)) {
                return true
            }
            if (this._rangeType == "out" && (u >= this._rangeFrom && u <= this._rangeTo)) {
                return true
            }
            if (this._rangeType == "from" && u < this._rangeFrom) {
                return true
            }
            if (this._rangeType == "to" && u > this._rangeTo) {
                return true
            }
        }
        var a = new Date(u);
        if (this._rangeWeek) {
            if (this._rangeWeekData[a.getDay()] === true) {
                return true
            }
        }
        if (this._rangeMonth) {
            if (this._rangeMonthData[a.getDate()] === true) {
                return true
            }
        }
        if (this._rangeYear) {
            if (this._rangeYearData[a.getMonth() + "_" + a.getDate()] === true) {
                return true
            }
        }
        return false
    };
    this.clearSensitiveRange = function() {
        this._clearRange();
        this._drawMonth(this._activeMonth)
    };
    this.setSensitiveRange = function(w, v, a) {
        var u = false;
        if (w != null && v != null) {
            if (!(w instanceof Date)) {
                w = this._strToDate(w, "%Y%m%d")
            }
            if (!(v instanceof Date)) {
                v = this._strToDate(v, "%Y%m%d")
            }
            if (w.getTime() > v.getTime()) {
                return
            }
            this._rangeFrom = new Date(w.getFullYear(), w.getMonth(), w.getDate(), 0, 0, 0, 0).getTime();
            this._rangeTo = new Date(v.getFullYear(), v.getMonth(), v.getDate(), 0, 0, 0, 0).getTime();
            this._rangeActive = true;
            this._rangeType = "in";
            u = true
        }
        if (!u && w != null && v == null) {
            if (!(w instanceof Date)) {
                w = this._strToDate(w)
            }
            this._rangeFrom = new Date(w.getFullYear(), w.getMonth(), w.getDate(), 0, 0, 0, 0).getTime();
            this._rangeTo = null;
            if (a === true) {
                this._rangeFrom++
            }
            this._rangeActive = true;
            this._rangeType = "from";
            u = true
        }
        if (!u && w == null && v != null) {
            if (!(v instanceof Date)) {
                v = this._strToDate(v)
            }
            this._rangeFrom = null;
            this._rangeTo = new Date(v.getFullYear(), v.getMonth(), v.getDate(), 0, 0, 0, 0).getTime();
            if (a === true) {
                this._rangeTo--
            }
            this._rangeActive = true;
            this._rangeType = "to";
            u = true
        }
        if (u) {
            this._drawMonth(this._activeMonth)
        }
    };
    this.setInsensitiveRange = function(u, a) {
        if (u != null && a != null) {
            if (!(u instanceof Date)) {
                u = this._strToDate(u)
            }
            if (!(a instanceof Date)) {
                a = this._strToDate(a)
            }
            if (u.getTime() > a.getTime()) {
                return
            }
            this._rangeFrom = new Date(u.getFullYear(), u.getMonth(), u.getDate(), 0, 0, 0, 0).getTime();
            this._rangeTo = new Date(a.getFullYear(), a.getMonth(), a.getDate(), 0, 0, 0, 0).getTime();
            this._rangeActive = true;
            this._rangeType = "out";
            this._drawMonth(this._activeMonth);
            return
        }
        if (u != null && a == null) {
            this.setSensitiveRange(null, u, true);
            return
        }
        if (u == null && a != null) {
            this.setSensitiveRange(a, null, true);
            return
        }
    };
    this.disableDays = function(y, x) {
        if (y == "week") {
            if (typeof(x) != "object" && typeof(x.length) == "undefined") {
                x = [x]
            }
            if (!this._rangeWeekData) {
                this._rangeWeekData = {}
            }
            for (var u in this._rangeWeekData) {
                this._rangeWeekData[u] = false;
                delete this._rangeWeekData[u]
            }
            for (var w = 0; w < x.length; w++) {
                this._rangeWeekData[x[w]] = true;
                if (x[w] == 7) {
                    this._rangeWeekData[0] = true
                }
            }
            this._rangeWeek = true
        }
        if (y == "month") {
            if (typeof(x) != "object" && typeof(x.length) == "undefined") {
                x = [x]
            }
            if (!this._rangeMonthData) {
                this._rangeMonthData = {}
            }
            for (var u in this._rangeMonthData) {
                this._rangeMonthData[u] = false;
                delete this._rangeMonthData[u]
            }
            for (var w = 0; w < x.length; w++) {
                this._rangeMonthData[x[w]] = true
            }
            this._rangeMonth = true
        }
        if (y == "year") {
            var v = this._extractDates(x);
            if (!this._rangeYearData) {
                this._rangeYearData = {}
            }
            for (var u in this._rangeYearData) {
                this._rangeYearData[u] = false;
                delete this._rangeYearData[u]
            }
            for (var w = 0; w < v.length; w++) {
                this._rangeYearData[v[w].getMonth() + "_" + v[w].getDate()] = true
            }
            this._rangeYear = true
        }
        this._drawMonth(this._activeMonth)
    };
    this.enableDays = function(a) {
        if (a == "week") {
            this._rangeWeek = false
        }
        if (a == "month") {
            this._rangeMonth = false
        }
        if (a == "year") {
            this._rangeYear = false
        }
        this._drawMonth(this._activeMonth)
    };
    this._tipData = {};
    this._tipTM = null;
    this._tipTMTime = 400;
    this._tipEvs = false;
    this._tipPopup = null;
    this._tipCellDate = null;
    this._tipCellDim = null;
    this.setTooltip = function(v, y, A, a) {
        var w = this._extractDates(v);
        for (var x = 0; x < w.length; x++) {
            var u = new Date(w[x].getFullYear(), w[x].getMonth(), w[x].getDate(), 0, 0, 0, 0).getTime();
            this._tipData[u] = {
                text: y,
                showIcon: A,
                usePopup: a
            }
        }
        this._drawMonth(this._activeMonth)
    };
    this.clearTooltip = function(u) {
        var v = this._extractDates(u);
        for (var w = 0; w < v.length; w++) {
            var a = new Date(v[w].getFullYear(), v[w].getMonth(), v[w].getDate(), 0, 0, 0, 0).getTime();
            this._tipData[a] = null;
            delete this._tipData[a]
        }
        this._drawMonth(this._activeMonth)
    };
    this._initTooltipPopup = function() {
        if (this._tipEvs) {
            return
        }
        this.attachEvent("onMouseOver", function(u) {
            var a = new Date(u.getFullYear(), u.getMonth(), u.getDate(), 0, 0, 0, 0).getTime();
            if (this._tipData[a] != null) {
                if (this._tipTM) {
                    window.clearTimeout(this._tipTM)
                }
                this._tipCellDate = u;
                this._tipCellDim = this.getCellDimension(u);
                this._tipText = this._tipData[a].text;
                this._tipTM = window.setTimeout(this._showTooltipPopup, this._tipTMTime)
            }
        });
        this.attachEvent("onMouseOut", this._hideTooltipPopup);
        this._tipEvs = true
    };
    this._showTooltipPopup = function(A, a, C, u, v) {
        if (!h._tipPopup) {
            h._tipPopup = new dhtmlXPopup({
                mode: "top"
            })
        }
        h._tipPopup.attachHTML(h._tipText);
        h._tipPopup.show(h._tipCellDim.x, h._tipCellDim.y, h._tipCellDim.w, h._tipCellDim.h);
        h.callEvent("onPopupShow", [h._tipCellDate])
    };
    this._hideTooltipPopup = function() {
        if (this._tipTM) {
            window.clearTimeout(this._tipTM)
        }
        if (this._tipPopup != null && this._tipPopup.isVisible()) {
            this._tipPopup.hide();
            this.callEvent("onPopupHide", [this._tipCellDate])
        }
    };
    this.getPopup = function() {
        return this._tipPopup
    };
    this.getCellDimension = function(v) {
        if (typeof(v) == "string") {
            v = this._strToDate(v)
        }
        if (!(v instanceof Date)) {
            return null
        }
        var x = new Date(v.getFullYear(), v.getMonth(), v.getDate(), 0, 0, 0, 0).getTime();
        var u = null;
        for (var y = 0; y < this.contDates.childNodes.length; y++) {
            for (var a = 0; a < this.contDates.childNodes[y].childNodes.length; a++) {
                var A = this.contDates.childNodes[y].childNodes[a];
                if (A._date != null && A._date.getTime() == x) {
                    u = {
                        x: window.dhx4.absLeft(A),
                        y: window.dhx4.absTop(A),
                        w: A.offsetWidth,
                        h: A.offsetHeight
                    }
                }
                A = null
            }
        }
        return u
    };
    this._updateFromInput = function(a) {
        if (this._nullInInput && ((a.value).replace(/\s/g, "")).length == 0) {
            if (this.checkEvent("onBeforeChange")) {
                if (!this.callEvent("onBeforeChange", [null])) {
                    this._updateInp();
                    return
                }
            }
            this.setDate(null)
        } else {
            this._updateDateStr(a.value)
        }
        a = null
    };
    this._doOnClick = function(u) {
        u = u || event;
        var a = (u.target || u.srcElement);
        if (a._dhtmlxcalendar_uid && a._dhtmlxcalendar_uid != h._activeInp && h._isVisible() && h._activeInp) {
            h._hide();
            return
        }
        if (!a._dhtmlxcalendar_uid || !h.i[a._dhtmlxcalendar_uid]) {
            if (h._isSelectorVisible()) {
                h._hideSelector()
            } else {
                if (!h._hasParent && h._isVisible()) {
                    h._hide()
                }
            }
        }
    };
    this._doOnKeyDown = function(a) {
        a = a || event;
        if (a.keyCode == 27 || a.keyCode == 13) {
            if (h._isSelectorVisible()) {
                h._hideSelector()
            } else {
                if (h._isVisible() && !h._hasParent) {
                    h._hide()
                }
            }
        }
    };
    this._doOnInpClick = function(u) {
        u = u || event;
        if(u.target.readOnly){ return; }
        if (u.type == "touchstart" && u.preventDefault != null) {
            u.preventDefault()
        }
        var a = (u.target || u.srcElement);
        if (!a._dhtmlxcalendar_uid) {
            return
        }
        if (!h._listenerEnabled) {
            h._updateFromInput(a)
        }
        
        if(h._show){
        	h._show(a._dhtmlxcalendar_uid, true)
        }
    };
    this._doOnInpKeyUp = function(u) {
        u = u || event;
        var a = (u.target || u.srcElement);
        if (u.keyCode == 13 || !a._dhtmlxcalendar_uid) {
            return
        }
        if (!h._listenerEnabled) {
            h._updateFromInput(a)
        }
    };
    this._doOnBtnClick = function(u) {
        u = u || event;
        if (u.type == "touchstart" && u.preventDefault != null) {
            u.preventDefault()
        }
        var a = (u.target || u.srcElement);
        if (!a._dhtmlxcalendar_uid) {
            return
        }
        if (h.i[a._dhtmlxcalendar_uid].input != null) {
            h._updateFromInput(h.i[a._dhtmlxcalendar_uid].input)
        }
        h._show(a._dhtmlxcalendar_uid, true)
    };
    this._doOnUnload = function() {
        if (h && h.unload) {
            h.unload()
        }
    };
    if (typeof(window.addEventListener) == "function") {
        document.body.addEventListener("click", h._doOnClick, false);
        window.addEventListener("keydown", h._doOnKeyDown, false);
        window.addEventListener("unload", h._doOnUnload, false);
        if (this.conf.touch == true) {
            document.body.addEventListener("touchstart", h._doOnClick, false)
        }
    } else {
        document.body.attachEvent("onclick", h._doOnClick);
        document.body.attachEvent("onkeydown", h._doOnKeyDown);
        window.attachEvent("onunload", h._doOnUnload)
    }
    this.attachObj = function(v) {
        var u = window.dhx4.newId();
        if (typeof(v) == "string") {
            this.i[u] = {
                input: document.getElementById(v)
            }
        } else {
            if (typeof(v.tagName) != "undefined") {
                this.i[u] = {
                    input: v
                }
            } else {
                if (typeof(v) == "object" && (v.input != null || v.button != null)) {
                    this.i[u] = {};
                    if (v.input != null) {
                        this.i[u].input = (typeof(v.input) == "string" ? document.getElementById(v.input) : v.input)
                    }
                    if (v.button != null) {
                        this.i[u].button = (typeof(v.button) == "string" ? document.getElementById(v.button) : v.button)
                    }
                }
            }
        }
        this._attachEventsToObject(u);
        return u
    };
    this.detachObj = function(w) {
        var v = null;
        if (this.i[w] != null) {
            v = w
        } else {
            if (typeof(w) == "string") {
                w = document.getElementById(w);
                v = w._dhtmlxcalendar_uid
            } else {
                if (typeof(w.tagName) != "undefined") {
                    v = w._dhtmlxcalendar_uid
                } else {
                    if (typeof(w) == "object" && (w.input != null || w.button != null)) {
                        if (v == null && w.input != null) {
                            v = (typeof(w.input) == "string" ? document.getElementById(w.input) : w.input)._dhtmlxcalendar_uid
                        }
                        if (v == null && w.button != null) {
                            v = (typeof(w.button) == "string" ? document.getElementById(w.button) : w.button)._dhtmlxcalendar_uid
                        }
                    }
                }
            }
        }
        if (v != null && this.i[v] != null) {
            this._detachEventsFromObject(v);
            for (var u in this.i[u]) {
                this.i[v][u]._dhtmlxcalendar_uid = null;
                this.i[v][u] = null;
                delete this.i[v][u]
            }
            this.i[v] = null;
            delete this.i[v];
            return true
        }
        return false
    };
    this._attachEventsToObject = function(u) {
        if (this.i[u].button != null) {
            this.i[u].button._dhtmlxcalendar_uid = u;
            if (typeof(window.addEventListener) == "function") {
                this.i[u].button.addEventListener("click", h._doOnBtnClick, false);
                if (this.conf.touch == true) {
                    this.i[u].button.addEventListener("touchstart", h._doOnBtnClick, false)
                }
            } else {
                this.i[u].button.attachEvent("onclick", h._doOnBtnClick)
            }
        } else {
            if (this.i[u].input != null) {
                this.i[u].input._dhtmlxcalendar_uid = u;
                if (typeof(window.addEventListener) == "function") {
                    this.i[u].input.addEventListener("click", h._doOnInpClick, false);
                    this.i[u].input.addEventListener("keyup", h._doOnInpKeyUp, false);
                    if (this.conf.touch == true) {
                        this.i[u].input.addEventListener("touchstart", h._doOnInpClick, false)
                    }
                } else {
                    this.i[u].input.attachEvent("onclick", h._doOnInpClick);
                    this.i[u].input.attachEvent("onkeyup", h._doOnInpKeyUp)
                }
            }
        }
    };
    this._detachEventsFromObject = function(u) {
        if (this.i[u].button != null) {
            if (typeof(window.addEventListener) == "function") {
                this.i[u].button.removeEventListener("click", h._doOnBtnClick, false);
                if (this.conf.touch == true) {
                    this.i[u].button.removeEventListener("touchstart", h._doOnBtnClick, false)
                }
            } else {
                this.i[u].button.detachEvent("onclick", h._doOnBtnClick)
            }
        } else {
            if (this.i[u].input != null) {
                if (typeof(window.addEventListener) == "function") {
                    this.i[u].input.removeEventListener("click", h._doOnInpClick, false);
                    this.i[u].input.removeEventListener("keyup", h._doOnInpKeyUp, false);
                    if (this.conf.touch == true) {
                        this.i[u].input.removeEventListener("touchstart", h._doOnInpClick, false)
                    }
                } else {
                    this.i[u].input.detachEvent("onclick", h._doOnInpClick);
                    this.i[u].input.detachEvent("onkeyup", h._doOnInpKeyUp)
                }
            }
        }
    };
    this._updateInp = function() {
        if (this.i != null && this._activeInp != null && this.i[this._activeInp] != null && this.i[this._activeInp].input != null) {
            //this.i[this._activeInp].input.value = this.getFormatedDate()
        	this.i[this._activeInp].input.value = this.getFormatedDate((Sepoa.info.DATE_FORMAT||"%Y%m%d"),this.getDate);
        }
    };
    this.enableListener = function(a) {
        if (!a) {
            return
        }
        if (typeof(window.addEventListener) == "function") {
            a.addEventListener("focus", h._listenerEvFocus, false);
            a.addEventListener("blur", h._listenerEvBlur, false)
        } else {
            a.attachEvent("onfocus", h._listenerEvFocus);
            a.attachEvent("onblur", h._listenerEvBlur)
        }
        a = null
    };
    this.disableListener = function(a) {
        if (!a) {
            return
        }
        a._f0 = false;
        if (this._tmListener) {
            window.clearTimeout(this._tmListener)
        }
        if (typeof(window.addEventListener) == "function") {
            a.removeEventListener("focus", h._listenerEvFocus, false);
            a.removeEventListener("blur", h._listenerEvBlur, false)
        } else {
            a.detachEvent("onfocus", h._listenerEvFocus);
            a.detachEvent("onblur", h._listenerEvBlur)
        }
        a = null
    };
    this._startListener = function(a) {
        if (this._tmListener) {
            window.clearTimeout(this._tmListener)
        }
        if (typeof(a._v1) == "undefined") {
            a._v1 = a.value
        }
        if (a._v1 != a.value) {
            this._updateFromInput(a);
            a._v1 = a.value
        }
        if (a._f0) {
            this._tmListener = window.setTimeout(function() {
                h._startListener(a)
            }, 100)
        }
    };
    this._listenerEvFocus = function(u) {
        u = u || event;
        var a = u.target || u.srcElement;
        a._f0 = true;
        h._startListener(a);
        a = null
    };
    this._listenerEvBlur = function(u) {
        u = u || event;
        var a = u.target || u.srcElement;
        a._f0 = false;
        a = null
    };
    for (var n in this.i) {
        this._attachEventsToObject(n)
    }
    window.dhx4._eventable(this);
    this._evOnArrowClick = function(a) {
        return this.callEvent("onArrowClick", a)
    };
    this._evOnClick = function(a) {
        return this.callEvent("onClick", a)
    };
    this._evOnMouseOut = function(a) {
        return this.callEvent("onMouseOut", a)
    };
    this._evOnMouseOver = function(a) {
        return this.callEvent("onMouseOver", a)
    };
    this.unload = function() {
    	window.removeEventListener('blur', this._onWindowBlur);
        this._activeDate = null;
        this._activeDateCell = null;
        this._activeInp = null;
        this._activeMonth = null;
        this._dateFormat = null;
        this._dateFormatRE = null;
        this._lastHover = null;
        if (this._tmListener) {
            window.clearTimeout(this._tmListener)
        }
        this._tmListener = null;
        if (typeof(window.addEventListener) == "function") {
            document.body.removeEventListener("click", h._doOnClick, false);
            window.removeEventListener("keydown", h._doOnKeyDown, false);
            window.removeEventListener("unload", h._doOnUnload, false);
            if (this.conf.touch == true) {
                document.body.removeEventListener("touchstart", h._doOnClick, false)
            }
        } else {
            document.body.detachEvent("onclick", h._doOnClick);
            document.body.detachEvent("onkeydown", h._doOnKeyDown);
            window.detachEvent("onunload", h._doOnKeyDown)
        }
        this._doOnClick = null;
        this._doOnKeyDown = null;
        this._doOnUnload = null;
        for (var u in this.i) {
            this.i[u]._dhtmlxcalendar_uid = null;
            this._detachEventsFromObject(u);
            this.disableListener(this.i[u].input);
            this.i[u] = null;
            delete this.i[u]
        }
        this.i = null;
        this._doOnInpClick = null;
        this._doOnInpKeyUp = null;
        window.dhx4._eventable(this, "clear");
        this.contMonth.onselectstart = null;
        this.contMonth.firstChild.firstChild.onclick = null;
        this.contMonth.firstChild.firstChild.ontouchstart = null;
        this.contMonth.firstChild.firstChild.firstChild.onmouseover = null;
        this.contMonth.firstChild.firstChild.firstChild.onmouseout = null;
        this.contMonth.firstChild.firstChild.lastChild.onmouseover = null;
        this.contMonth.firstChild.firstChild.lastChild.onmouseout = null;
        while (this.contMonth.firstChild.firstChild.childNodes.length > 0) {
            this.contMonth.firstChild.firstChild.removeChild(this.contMonth.firstChild.firstChild.lastChild)
        }
        this.contMonth.firstChild.removeChild(this.contMonth.firstChild.firstChild);
        this.contMonth.removeChild(this.contMonth.firstChild);
        this.contMonth.parentNode.removeChild(this.contMonth);
        this.contMonth = null;
        while (this.contDays.firstChild.childNodes.length > 0) {
            this.contDays.firstChild.removeChild(this.contDays.firstChild.lastChild)
        }
        this.contDays.removeChild(this.contDays.firstChild);
        this.contDays.parentNode.removeChild(this.contDays);
        this.contDays = null;
        this.contDates.onclick = null;
        this.contDates.ontouchstart = null;
        this.contDates.onmouseover = null;
        this.contDates.onmouseout = null;
        while (this.contDates.childNodes.length > 0) {
            while (this.contDates.lastChild.childNodes.length > 0) {
                this.contDates.lastChild.lastChild._css_date = null;
                this.contDates.lastChild.lastChild._css_month = null;
                this.contDates.lastChild.lastChild._css_weekend = null;
                this.contDates.lastChild.lastChild._css_hover = null;
                this.contDates.lastChild.lastChild._date = null;
                this.contDates.lastChild.lastChild._q = null;
                this.contDates.lastChild.lastChild._w = null;
                this.contDates.lastChild.removeChild(this.contDates.lastChild.lastChild)
            }
            this.contDates.removeChild(this.contDates.lastChild)
        }
        this.contDates.parentNode.removeChild(this.contDates);
        this.contDates = null;
        this.contTime.firstChild.firstChild.onclick = null;
        this.contTime.firstChild.firstChild.ontouchstart = null;
        while (this.contTime.firstChild.firstChild.childNodes.length > 0) {
            this.contTime.firstChild.firstChild.removeChild(this.contTime.firstChild.firstChild.lastChild)
        }
        this.contTime.firstChild.removeChild(this.contTime.firstChild.firstChild);
        this.contTime.removeChild(this.contTime.firstChild);
        this.contTime.parentNode.removeChild(this.contTime);
        this.contTime = null;
        this._lastHover = null;
        this._unloadSelector("month");
        this._unloadSelector("year");
        this._unloadSelector("hours");
        this._unloadSelector("minutes");
        if (this._selCover) {
            this._selCover.parentNode.removeChild(this._selCover);
            this._selCover = null
        }
        if (this._sel) {
            for (var u in this._sel._ta) {
                this._sel._ta[u] = null
            }
            this._sel._ta = null;
            this._sel._t = null;
            this._sel.onmouseover = null;
            this._sel.onmouseout = null;
            while (this._sel.firstChild.firstChild.firstChild.childNodes.length > 0) {
                this._sel.firstChild.firstChild.firstChild.lastChild.onclick = null;
                this._sel.firstChild.firstChild.firstChild.lastChild.onmouseover = null;
                this._sel.firstChild.firstChild.firstChild.lastChild.onmouseout = null;
                this._sel.firstChild.firstChild.firstChild.removeChild(this._sel.firstChild.firstChild.firstChild.lastChild)
            }
            this._sel.firstChild.firstChild.removeChild(this._sel.firstChild.firstChild.firstChild);
            this._sel.firstChild.removeChild(this._sel.firstChild.firstChild);
            while (this._sel.childNodes.length > 0) {
                this._sel.removeChild(this._sel.lastChild)
            }
            this._sel.parentNode.removeChild(this._sel);
            this._sel = null
        }
        this.base.onclick = null;
        this.base.onmousedown = null;
        this.base.ontouchstart = null;
        this.base.onmouseout = null;
        this.base.parentNode.removeChild(this.base);
        this.base = null;
        this._clearDayHover = null;
        this._clearSelHover = null;
        this._doOnSelectorChange = null;
        this._doOnSelectorShow = null;
        this._drawMonth = null;
        this._fixLength = null;
        this._ifrSize = null;
        this._hide = null;
        this._hideSelector = null;
        this._initSelector = null;
        this._isSelectorVisible = null;
        this._isVisible = null;
        this._scrollYears = null;
        this._show = null;
        this._showSelector = null;
        this._strToDate = null;
        this._updateActiveHours = null;
        this._updateActiveMinutes = null;
        this._updateActiveMonth = null;
        this._updateActiveYear = null;
        this._updateCellStyle = null;
        this._updateDateStr = null;
        this._updateVisibleHours = null;
        this._updateVisibleMinutes = null;
        this._updateYearsList = null;
        this.enableIframe = null;
        this.hide = null;
        this.hideTime = null;
        this.setDate = null;
        this.setDateFormat = null;
        this.setYearsRange = null;
        this.show = null;
        this.showTime = null;
        this.unload = null;
        if (this._tipPopup != null) {
            this._tipPopup.unload();
            this._tipPopup = null
        }
        for (var u in this) {
            delete this[u]
        }
        u = h = null;
    };
    this.setDate(this._activeDate);
    this.showToday();
    
    this._onWindowBlur = ()=>{
    	this.hide();
    }
    window.addEventListener('blur', this._onWindowBlur);
    
    return this
}
dhtmlXCalendarObject.prototype._buildMonthHdr = function(h) {
    var c = this;
    var g = function(l) {
        return (String(l).length == 1 ? "0" + String(l) : l)
    };
    var a = function(l, m) {
        return "<span class='dhtmlxcalendar_month_label_" + l + "'>" + m + "</span>"
    };
    var e = String(this.langData[this.lang].hdrformat || ("%F %Y")).replace(/%[a-z]/gi, function(l) {
        switch (l) {
            case "%m":
                return a("month", g(h.getMonth() + 1));
            case "%n":
                return a("month", h.getMonth() + 1);
            case "%M":
                return a("month", c.langData[c.lang].monthesSNames[h.getMonth()]);
            case "%F":
                return a("month", c.langData[c.lang].monthesFNames[h.getMonth()]);
            case "%y":
                return a("year", g(h.getYear() % 100));
            case "%Y":
                return a("year", h.getFullYear());
            case "%%":
                return "%";
            default:
                return l
        }
    });
    c = g = a = null;
    return e
};
dhtmlXCalendarObject.prototype.enableIframe = function(a) {
    if (a == true) {
        if (!this._ifr) {
            this._ifr = document.createElement("IFRAME");
            this._ifr.frameBorder = 0;
            this._ifr.border = 0;
            this._ifr.setAttribute("src", "javascript:false;");
            this._ifr.className = "dhtmlxcalendar_ifr";
            this._ifr.onload = function() {
                this.onload = null;
                this.contentWindow.document.open("text/html", "replace");
                this.contentWindow.document.write("<html><head><style>html,body{width:100%;height:100%;overflow:hidden;margin:0px;}</style></head><body</body></html>")
            };
            this.base.parentNode.insertBefore(this._ifr, this.base);
            this._ifrSize()
        }
    } else {
        if (this._ifr) {
            this._ifr.parentNode.removeChild(this._ifr);
            this._ifr = null
        }
    }
};
dhtmlXCalendarObject.prototype._ifrSize = function() {
    if (this._ifr) {
        this._ifr.style.left = this.base.style.left;
        this._ifr.style.top = this.base.style.top;
        this._ifr.style.width = this.base.offsetWidth + "px";
        this._ifr.style.height = this.base.offsetHeight + "px"
    }
};
dhtmlxCalendarObject = dhtmlXCalendarObject;
dhtmlXCalendarObject.prototype._dateStrings = function() {
    var a = this.langData[this.lang];
    return {
        monthFullName: a.monthesFNames,
        monthShortName: a.monthesSNames,
        dayFullName: a.daysFNames,
        dayShortName: a.daysSNames
    }
};
dhtmlXCalendarObject.prototype._strToDate = function(c, a) {
    return window.dhx4.str2date(c, a || this._dateFormat, this._dateStrings())
};
dhtmlXCalendarObject.prototype._dateToStr = function(c, a) {
    return window.dhx4.date2str(c, a || this._dateFormat, this._dateStrings())
};
window.dhtmlxDblCalendarObject = window.dhtmlXDoubleCalendarObject = window.dhtmlXDoubleCalendar = function(c) {
    var a = this;
    this.leftCalendar = new dhtmlXCalendarObject(c);
    this.leftCalendar.hideTime();
    this.rightCalendar = new dhtmlXCalendarObject(c);
    this.rightCalendar.hideTime();
    this.leftCalendar.attachEvent("onClick", function(e) {
        a._updateRange("rightCalendar", e, null);
        a._evOnClick(["left", e])
    });
    this.rightCalendar.attachEvent("onClick", function(e) {
        a._updateRange("leftCalendar", null, e);
        a._evOnClick(["right", e])
    });
    this.leftCalendar.attachEvent("onBeforeChange", function(e) {
        return a._evOnBeforeChange(["left", e])
    });
    this.rightCalendar.attachEvent("onBeforeChange", function(e) {
        return a._evOnBeforeChange(["right", e])
    });
    this.show = function() {
        this.leftCalendar.show();
        this.rightCalendar.base.style.marginLeft = this.leftCalendar.base.offsetWidth - 1 + "px";
        this.rightCalendar.show()
    };
    this.hide = function() {
        this.leftCalendar.hide();
        this.rightCalendar.hide()
    };
    this.setDateFormat = function(e) {
        this.leftCalendar.setDateFormat(e);
        this.rightCalendar.setDateFormat(e)
    };
    this.setDates = function(g, e) {
        if (g != null) {
            this.leftCalendar.setDate(g)
        }
        if (e != null) {
            this.rightCalendar.setDate(e)
        }
        this._updateRange()
    };
    this._updateRange = function(e, h, g) {
        if (arguments.length == 3) {
            (e == "leftCalendar" ? this.leftCalendar : this.rightCalendar).setSensitiveRange(h, g)
        } else {
        	if(this.rightCalendar.getDate() == null){
        		this.leftCalendar.clearSensitiveRange();
        	}else{
        		this.leftCalendar.setSensitiveRange(null, this.rightCalendar.getDate());
        	}
        	
        	if(this.leftCalendar.getDate() == null){
        		this.rightCalendar.clearSensitiveRange();
        	}else{
        		this.rightCalendar.setSensitiveRange(this.leftCalendar.getDate(), null)
        	}
        }
    };
    this.getFormatedDate = function() {
        return this.leftCalendar.getFormatedDate.apply(this.leftCalendar, arguments)
    };
    this.unload = function() {
        window.dhx4._eventable(this, "clear");
        this.leftCalendar.unload();
        this.rightCalendar.unload();
        this.leftCalendar = this.rightCalendar = null;
        this._updateRange = null;
        this._evOnClick = null;
        this._evOnBeforeChange = null;
        this.show = null;
        this.hide = null;
        this.setDateFormat = null;
        this.setDates = null;
        this.getFormatedDate = null;
        this.unload = null;
        a = null
    };
    this._evOnClick = function(e) {
        return this.callEvent("onClick", e)
    };
    this._evOnBeforeChange = function(e) {
        return this.callEvent("onBeforeChange", e)
    };
    window.dhx4._eventable(this);
    return this
};

SepoadhtmlXDoubleCalendar = function(c) {
    var a = this;
    this.leftCalendar = new dhtmlXCalendarObject(c[0]);
    this.leftCalendar.hideTime();
    this.rightCalendar = new dhtmlXCalendarObject(c[1]);
    this.rightCalendar.hideTime();
    
    this.leftCalendar.attachEvent("onClick", function(e) {
        a._updateRange("rightCalendar", e, null);
        a._evOnClick(["left", e])
    });
    this.rightCalendar.attachEvent("onClick", function(e) {
        a._updateRange("leftCalendar", null, e);
        a._evOnClick(["right", e])
    });
    this.leftCalendar.attachEvent("onBeforeChange", function(e) {
        return a._evOnBeforeChange(["left", e])
    });
    this.rightCalendar.attachEvent("onBeforeChange", function(e) {
        return a._evOnBeforeChange(["right", e])
    });
    this.show = function() {
        this.leftCalendar.show();
        this.rightCalendar.base.style.marginLeft = this.leftCalendar.base.offsetWidth - 1 + "px";
        this.rightCalendar.show()
    };
    this.hide = function() {
        this.leftCalendar.hide();
        this.rightCalendar.hide()
    };
    this.setDateFormat = function(e) {
        this.leftCalendar.setDateFormat(e);
        this.rightCalendar.setDateFormat(e)
    };
    this.setDates = function(g, e) {
        if (g != null) {
            this.leftCalendar.setDate(g)
        }
        if (e != null) {
            this.rightCalendar.setDate(e)
        }
        this._updateRange()
    };
    this._updateRange = function(e, h, g) {
        if (arguments.length == 3) {
            (e == "leftCalendar" ? this.leftCalendar : this.rightCalendar).setSensitiveRange(h, g)
        } else {
            this.leftCalendar.setSensitiveRange(null, this.rightCalendar.getDate());
            this.rightCalendar.setSensitiveRange(this.leftCalendar.getDate(), null)
        }
    };
    this.getFormatedDate = function() {
        return this.leftCalendar.getFormatedDate.apply(this.leftCalendar, arguments)
    };
    this.unload = function() {
        window.dhx4._eventable(this, "clear");
        this.leftCalendar.unload();
        this.rightCalendar.unload();
        this.leftCalendar = this.rightCalendar = null;
        this._updateRange = null;
        this._evOnClick = null;
        this._evOnBeforeChange = null;
        this.show = null;
        this.hide = null;
        this.setDateFormat = null;
        this.setDates = null;
        this.getFormatedDate = null;
        this.unload = null;
        a = null
    };
    this._evOnClick = function(e) {
        return this.callEvent("onClick", e)
    };
    this._evOnBeforeChange = function(e) {
        return this.callEvent("onBeforeChange", e)
    };
    window.dhx4._eventable(this);
    return this
};