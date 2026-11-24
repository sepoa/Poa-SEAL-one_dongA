function SepoaBigCalendar(conf){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.conf=$.extend(true, {
		ym_format:"%Y / %m",
		start_day:0,
		language:"KO",
		viewport:null,
		overflow:"ellipsis"
	}, conf);
	
	if(this.conf.language.toUpperCase() == "EN"){
		this.conf.ym_format = "%Y / %F"
	}
	
	if(!this.conf.viewport){
		throw new Error('not found viewport');
	}else if(typeof(this.conf.viewport) == "string"){
		this.conf.viewport = document.getElementById(this.conf.viewport);
	}
	
	this.conf.viewport.classList.add('sepoa-big-calendar-view');
	this.dateStrings = window.dhx4.dateStrings[this.conf.language.toLowerCase()] || window.dhx4.dateStrings["en"];
	
	this.obj = {};
	this.renderBase();
	this.attachEvent('onNavigatorClick', function(adjust){
		var nd = new Date(this.now)
		nd.setMonth(nd.getMonth() + adjust)
		this.setYearMonth(nd);
	})
	var base = this;
	if(SUX){
		SUX.attachEvent('onWindowResize', function(){
			base.setSize();
		})
	}
}
SepoaBigCalendar.prototype.date2str = function(val, format) {
	var format = format || this.conf.ym_format;
	var strings = this.dateStrings;
	
	if (val instanceof Date) {
		var z = function(t) {
			return (String(t).length==1?"0"+String(t):t);
		}
		var k = function(t) {
			switch(t) {
				case "%d": return z(val.getDate());
				case "%j": return val.getDate();
				case "%D": return strings.dayShortName[val.getDay()];
				case "%l": return strings.dayFullName[val.getDay()];
				case "%m": return z(val.getMonth()+1);
				case "%n": return val.getMonth()+1;
				case "%M": return strings.monthShortName[val.getMonth()];
				case "%F": return strings.monthFullName[val.getMonth()];
				case "%y": return z(val.getYear()%100);
				case "%Y": return val.getFullYear();
				case "%g": return (val.getHours()+11)%12+1;
				case "%h": return z((val.getHours()+11)%12+1);
				case "%G": return val.getHours();
				case "%H": return z(val.getHours());
				case "%i": return z(val.getMinutes());
				case "%s": return z(val.getSeconds());
				case "%a": return (val.getHours()>11?"pm":"am");
				case "%A": return (val.getHours()>11?"PM":"AM");
				case "%%": return "%";
				case "%u": return val.getMilliseconds();
				default: return t;
			}
		}
		var t = String(format||window.dhx4.dateFormat).replace(/%[a-zA-Z]/g, k);
	}
	return (t||String(val));
};
SepoaBigCalendar.prototype.str2date=function(val, format){
	var format = (format || this.conf.ym_format).replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\\:|]/g, "\\$&");
	var strings = this.dateStrings;
	var v = [];
	var f = [];
	format = format.replace(/%[a-z]/gi, function(t){
		switch (t) {
			case "%d": case "%m": case "%y": case "%h": case "%H": case "%i": case "%s": f.push(t); return "(\\d{2})";
			case "%D": case "%l": case "%M": case "%F": f.push(t); return "([a-zéûä\u0430-\u044F\u0451]{1,})";
			case "%j": case "%n": case "%g": case "%G": f.push(t); return "(\\d{1,2})";
			case "%Y": f.push(t); return "(\\d{4})";
			case "%a": f.push(t); return "([a|p]m)";
			case "%A": f.push(t); return "([A|P]M)";
			case "%u": f.push(t); return "(\\d{1,6})";
		}
		return t;
	});
	var re = new RegExp(format, "i");
	var e = val.match(re);
	if (e == null || e.length-1 != f.length) return "Invalid Date";
	for (var q=1; q<e.length; q++) v.push(e[q]);
	var p = {"%y":1,"%Y":1,"%n":2,"%m":2,"%M":2,"%F":2,"%d":3,"%j":3,"%a":4,"%A":4,"%H":5,"%G":5,"%h":5,"%g":5,"%i":6,"%s":7,"%u":7,"%P":7};
	var v2 = {};
	var f2 = {};
	for (var q=0; q<f.length; q++) {
		if (typeof(p[f[q]]) != "undefined") {
			var ind = p[f[q]];
			if (!v2[ind]){v2[ind]=[];f2[ind]=[];}
			v2[ind].push(v[q]);
			f2[ind].push(f[q]);
		}
	}
	v = [];
	f = [];
	for (var q=1; q<=7; q++) {
		if (v2[q] != null) {
			for (var w=0; w<v2[q].length; w++) {
				v.push(v2[q][w]);
				f.push(f2[q][w]);
			}
		}
	}
	var r = new Date();
	r.setDate(1);
	r.setHours(0);
	r.setMinutes(0);
	r.setSeconds(0);
	r.setMilliseconds(0);
	var getInd = function(val, ar) {
		for (var q=0; q<ar.length; q++) if (ar[q].toLowerCase() == val) return q;
		return -1;
	}
	for (var q=0; q<v.length; q++) {
		switch (f[q]) {
			case "%j": case "%n": case "%Y": case "%H": case "%G": case "%i": case "%s": case "%u":
				if (!isNaN(v[q])) r[{"%d":"setDate","%j":"setDate","%n":"setMonth","%m":"setMonth","%Y":"setFullYear","%H":"setHours","%G":"setHours","%i":"setMinutes","%s":"setSeconds","%u":"setMilliseconds"}[f[q]]](Number(v[q])+(f[q]=="%m"||f[q]=="%n"?-1:0));
				break;
			case "%m":
				if (!isNaN(v[q])){
					if(v[q]>11) v[q] = 11;
					else v[q] -= 1;
					r.setMonth(v[q]);
				}
				break;
			case "%d":
				if (!isNaN(v[q])){
					var br = new Date(r);
					r.setDate(v[q])
					if(br.getFullYear() != r.getFullYear() || br.getMonth() != r.getMonth()){
						r = br;
						r.setMonth(r.getMonth()+1);
						r.setDate(0);
					}
				}
				break;
			case "%M": case "%F":
				var k = getInd(v[q].toLowerCase(),strings[{"%M":"monthShortName","%F":"monthFullName"}[f[q]]]);
				if (k >= 0) r.setMonth(k);
				break;
			case "%y":
				if (!isNaN(v[q])) {
					var v0 = Number(v[q]);
					r.setFullYear(v0+(v0>50?1900:2000));
				}
				break;
			case "%g": case "%h":
				if (!isNaN(v[q])) {
					var v0 = Number(v[q]);
					if (v0 <= 12 && v0 >= 0) r.setHours(v0+(getInd("pm",v)>=0?(v0==12?0:12):(v0==12?-12:0)));
				}
				break;
		}
	}
	return r;
}
SepoaBigCalendar.prototype.clear=function(){
	this.items = {};
	this.setYearMonth(new Date());
}
SepoaBigCalendar.prototype.renderBase=function(){
	var calBox = this.obj.calBox = this.conf.viewport.appendChild(document.createElement('div'));
	calBox.classList.add('sepoa-big-calendar-base')
	
	var nav = this.obj.navBox = calBox.appendChild(document.createElement('div'));
	nav.classList.add('sepoa-big-calendar-nav');
	
	var prev = nav.appendChild(document.createElement('div'));
	prev.obj = this;
	prev.classList.add('sepoa-big-calendar-nav-button');
	prev.classList.add('sepoa-big-calendar-nav-button-prev');
	prev.innerHTML = '<svg height="15" width="15" viewBox="0 0 75 100" fill="rgba(0,0,0,0.5)"><polyline points="0,50 75,0 75,100"></polyline></svg>';
	prev.addEventListener('click', function() {
		this.obj.callEvent('onNavigatorClick', [-1]);
	});
	
	var ym = this.obj.ym = nav.appendChild(document.createElement('div'));
	ym.classList.add('sepoa-big-calendar-year-month');
	
	var next = nav.appendChild(document.createElement('div'));
	next.obj = this;
	next.classList.add('sepoa-big-calendar-nav-button');
	next.classList.add('sepoa-big-calendar-nav-button-next');
	next.innerHTML = '<svg height="15" width="15" viewBox="0 0 75 100" fill="rgba(0,0,0,0.5)"><polyline points="0,0 75,50 0,100"></polyline></svg>';
	next.addEventListener('click', function() {
		this.obj.callEvent('onNavigatorClick', [1]);
	});
	
	var monthBox = this.obj.monthBox = calBox.appendChild(document.createElement('div'));
	monthBox.classList.add('sepoa-big-calendar-month-box');
	
	this.setYearMonth();
}
SepoaBigCalendar.prototype.setYearMonth=function(newDate){
	this.now = newDate || new Date();
	this.now.setDate(1);
	
	var today = this.date2str(new Date(), "%Y%m%d");
	this.obj.monthBox.innerHTML = "";
	this.obj.ym.innerHTML = this.date2str(this.now)
	
	var weeks = this.obj.weeks = this.obj.monthBox.appendChild(document.createElement('ul'));
	weeks.classList.add('sepoa-big-calendar-weeks');
	
	var w = this.conf.start_day;
	for(var i=0;i<7;i++){
		if(w >= 7) w = 0
		var day = weeks.appendChild(document.createElement('li'));
		day.classList.add('sepoa-big-calendar-weeks-day');
		day.classList.add('sepoa-big-calendar-weeks-day-' + w);
		var label = day.appendChild(document.createElement('span'));
		label.classList.add('sepoa-big-calendar-label');
		label.innerHTML = this.dateStrings.dayShortName[w];
		w++;
	}
	
	var firstDate = new Date(this.now.getFullYear(), this.now.getMonth(), 1);
	var lastDate = new Date(this.now.getFullYear(), this.now.getMonth()+1, 0);
	
	var prevAdjust = firstDate.getDay() - this.conf.start_day;
	var nextAdjust = 6 - this.conf.start_day - lastDate.getDay();
	
	var startDate = new Date(this.now.getFullYear(), this.now.getMonth(), 1 - prevAdjust);
	var cnt = lastDate.getDate() + prevAdjust + nextAdjust;  
	
	var days = this.obj.monthBox.appendChild(document.createElement('ul'));
	days.classList.add('sepoa-big-calendar-date-box')
	for(var i=0;i < cnt ;i++){
		var dd = new Date(startDate.getFullYear(), startDate.getMonth(), startDate.getDate() + i);
		var dateString = this.date2str(dd, "%Y%m%d");
		var dateMonth = dd.getMonth();
		
		var monthFlag = "curr";
		if(dateMonth < this.now.getMonth()){
			monthFlag = "prev"
		}else if(dateMonth > this.now.getMonth()){
			monthFlag = "next"
		}
		
		var date = days.appendChild(document.createElement('li'));
		date.obj = this;
		date.date = dateString;
		date.month = dateMonth;
		date.classList.add('sepoa-big-calendar-date');
		date.classList.add('sepoa-big-calendar-month-'+monthFlag);
		date.classList.add('sepoa-big-calendar-weeks-day-'+dd.getDay());
		date.classList.add('sepoa-big-calendar-weeks-date-'+dateString);
		
		if(today == dateString){
			date.classList.add('sepoa-big-calendar-date-today')
		}
		date.addEventListener('mouseover', function(e){
			this.classList.add('sepoa-big-calendar-date-active')
		});
		date.addEventListener('mouseleave', function(e){
			this.classList.remove('sepoa-big-calendar-date-active')
		});
		
		date.addEventListener('click', function(e){
			if(this.classList.contains('sepoa-big-calendar-month-curr')){
				var list = [];
				
				for(var i=0;i<this.event.children.length;i++){
					list.push(this.event.children[i].item);
				}
				
				this.obj.callEvent('onDateClick', [this.date, list]);
			}else if(this.classList.contains('sepoa-big-calendar-month-next')){
				this.obj.callEvent('onNavigatorClick', [1]);
			}else{
				this.obj.callEvent('onNavigatorClick', [-1]);
			}
		});
		
		var dLabel = date.appendChild(document.createElement('div'));
		dLabel.classList.add('sepoa-big-calendar-label');
		dLabel.innerHTML = dd.getDate();
		
		var dEvents = date.appendChild(document.createElement('ul'));
		dEvents.classList.add('sepoa-big-calendar-event');
		
		date.event = dEvents;
		if(this.conf.overflow != "ellipsis"){
			dEvents.style.overflowY = (this.conf.overflow == "scroll")?"auto":this.conf.overflow
		}else{
			dEvents.style.overflowY = "hidden"
		}
	}
	this.callEvent('onCalendarChange', [this.now.getFullYear()+ "" + (this.now.getMonth()+1)])
}
SepoaBigCalendar.prototype.addEvent=function(list){
	if(!(list instanceof Array)){
		list = [list];
	}
	for(var i=0;i<list.length;i++){
		var event = list[i];
		var dateString = event.DATE;
		var date = this.str2date(dateString, "%Y%m%d")
		if(dateString == "Invalid Date"){
			console.log(event)
			throw new Error("Invalid Date")
		}
		if(date.getMonth() == this.now.getMonth()){
			var title = event.TITLE 		
			var events = $(this.obj.monthBox).find('.sepoa-big-calendar-weeks-date-'+dateString+' .sepoa-big-calendar-event')[0];
			
			var item = events.appendChild(document.createElement('li'));
			item.classList.add('sepoa-big-calendar-event-item');
			item.item = event;
			item.innerHTML = '<span>'+title+'</span>'
			this._eventAreaViewUpdate(events);
		}
	}
}
SepoaBigCalendar.prototype.removeEvent=function(date, id){
	if(date instanceof Date){
		date = this.date2str(date, "%Y%m%d");
	}
	var events = $(this.obj.monthBox).find('.sepoa-big-calendar-weeks-date-'+date+' .sepoa-big-calendar-event')[0];
	if(id === undefined){
		events.innerHTML="";
	}else{
		for(var i=0;i<events.children.length;i++){
			if(events.children[i].item.ID == id){
				events.removeChild(events.children[i]);
				break;
			}
		}
		this._eventAreaViewUpdate(events);
	}
}
SepoaBigCalendar.prototype._eventAreaViewUpdate=function(events){
	if(this.conf.overflow != "ellipsis"){
		var hiddenFlag = false;
		for(var i=0;i<events.children.length;i++){
			var item = events.children[i];
			item.classList.remove('sepoa-big-calendar-event-item-hidden');
			item.classList.remove('sepoa-big-calendar-event-item-ellipsis');
			
			if(hiddenFlag){
				item.classList.add('sepoa-big-calendar-event-item-hidden');
			}else if((item.offsetTop + item.offsetHeight) > item.offsetParent.offsetHeight){
				item.classList.add('sepoa-big-calendar-event-item-hidden');
				item.previousElementSibling.classList.add('sepoa-big-calendar-event-item-ellipsis');
				hiddenFlag = true;
			}
		}
	}
}
SepoaBigCalendar.prototype.setSize=function(){
	var events = $(this.obj.monthBox).find('.sepoa-big-calendar-date .sepoa-big-calendar-event');
	for(var i=0;i<events.length;i++){
		this._eventAreaViewUpdate(events[i]);
	}
}