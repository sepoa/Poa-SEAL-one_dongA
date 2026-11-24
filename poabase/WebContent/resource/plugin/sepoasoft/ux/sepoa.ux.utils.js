if(!window.sUtils){
	window.sUtils={
		isTop:false
	}
	try{
		parent.sUtils;
		if(window == parent){
			sUtils.isTop = true;
		}
	}catch(e){
		sUtils.isTop = true;
	}
	
	/* modules */
	var modules = {
		EventManager:{
			_init: function() {
		        this._events = {"_uid":(new Date()).valueOf()};
		        this._handlers = {};
		    },
		    blockEvent: function() {
		        this._events._block = true
		    },
		    unblockEvent: function() {
		        this._events._block = false
		    },
		    callEvent: function(e, h) {
		        if (this._events._block) {
		            return true
		        }
		        e = e.toLowerCase();
		        
		        var g = this._events[e.toLowerCase()];
		        var a = true;
		        if (g) {
		        	var tmp = $.extend(true, [], g);
		        	for (var c = 0; c < tmp.length; c++) {
		                if (tmp[c].apply(this, (h || [])) === false) {
		                    a = false
		                }
		            }
		        }
		        return a
		    },
		    attachEvent: function(c, a, g) {
		        c = c.toLowerCase();
		        g = g || (this._events._uid++);
		        var e = this._events[c] || [];
		        e.push(a);
		        this._events[c] = e;
		        this._handlers[g] = {
		            f: a,
		            t: c
		        };
		        return g
		    },
		    detachEvent: function(g) {
		        if (this._handlers[g]) {
		            var c = this._handlers[g].t;
		            var a = this._handlers[g].f;
		            var e = this._events[c];
		            e.splice(e.indexOf(a), 1);
		            delete this._handlers[g]
		        }
		    }
		},
		extend:function(org, ext){
			if(typeof(ext) == "string"){
				ext = modules[ext]; 
			}
			for(var i in ext){
				org[i] = ext[i].bind(org);
			}
			if(org._init){
				org._init();
				delete org._init;
			}
			return org;
		}
	};
	
	sUtils.modules=modules;
	
	/* domain */
	sUtils.domain={
		relax:function(){
			try{
				var reDomain;
				var newDoamin = document.domain;
				if (newDoamin.search(/^\d+\.\d+\.\d+\.\d+$/) >=0){
					reDomain = newDoamin;
				} else {
					var lnDotPos = newDoamin.indexOf( "." );
					reDomain = (lnDotPos >= 0) ? newDoamin.substr( lnDotPos + 1): newDoamin;
				}
				document.domain = reDomain;
			} catch(ex) {
				console.log(ex.message);
			}
		}
	}
	
	/* loader */
	sUtils.loader={
		css:function(url, callback , id, position){
			if(!position){
				position = $('head')[0];
			}
			var t = $('<link rel="stylesheet" type="text/css""></link>')[0];
			t.onreadystatechange = callback;
			t.onload = callback;
			t.href = url;
			position.appendChild(t);
		},
		script:function(url, callback, id, position){
			if(!position){
				position = $('head')[0];
			}
			var script = document.createElement('script');
			script.onload = callback;
			script.src = url;
			position.appendChild(script);
		},
		common:function(params, ajaxOptions){
			var result = null;
			ajaxOptions = $.extend({
				url: (window.CONTEXT_NAME||"")+"/SepoaJSCommon.sux",
				type:"post",
				data:params,
				dataType: "json",
				success: function(data){
					result = data;
				},
				error:function(){

				},
				async: false,
				cache:false
			}, ajaxOptions);
			
			if(window.CONTEXT_NAME && ajaxOptions.url.indexOf(window.CONTEXT_NAME) != 0) {
				ajaxOptions.url = (window.CONTEXT_NAME || '') + ajaxOptions.url;
			}
			
			$.ajax(ajaxOptions);
			return result;
		}
	}
	
	/* html element */
	sUtils.element={
		getOffset: function(elem, target) {
			if(target && (target.indexOf("module") == 0)){
				//module top
            	var r = this._elemOffset(elem);
            	var targetWindow = (target == "module.over")?window.top:SUX.getModuleTop();
            	if(window != window.top && !parent.isBridgeWindow && targetWindow != window){
            		var l = parent.$('iframe');
            		for(var w in l){
            			if(l[w].contentWindow == self){
            				var t = parent.sUtils.element.getOffset(l[w], (targetWindow != window)?target:null);
            				r.top += t.top;
            				r.left += t.left;
            				break;
            			}  
            		}
            		return r;
            	}else{
            		return r;
            	}
            }else{
            	//in page
            	return this._elemOffset(elem, $(elem).parents('body')[0]);
            }
        },
        _elemOffset: function(a, target) {
        	var e = 0,
		        c = 0;
        	if(!target && a.getBoundingClientRect){
        		var box = a.getBoundingClientRect();
    			var body = document.body;
    			var docElem = document.documentElement;
    			var scrollTop = window.pageYOffset || docElem.scrollTop || body.scrollTop;
    			var scrollLeft = window.pageXOffset || docElem.scrollLeft || body.scrollLeft;
    			var clientTop = docElem.clientTop || body.clientTop || 0;
    			var clientLeft = docElem.clientLeft || body.clientLeft || 0;
    			e  = box.top +  scrollTop - clientTop;
    			c = box.left + scrollLeft - clientLeft;
        	}else{
        	    while (a) {
			    	e = e + parseInt(a.offsetTop) - parseInt(a.scrollTop || 0);
			        c = c + parseInt(a.offsetLeft) - parseInt(a.scrollLeft || 0);
			        if(target){
			    		var parents = [].slice.call($(a.offsetParent).parents());
				    	if(parents.indexOf(target) < 0){
				    		break;
				    	}
			    	}
			        a = a.offsetParent;
			    }
        	}
		    return {
		        top: e,
		        left: c
		    }
		},
		create:(htmlString)=>{
			const div = document.createElement('div');
			div.innerHTML = htmlString.trim();
			return div.firstChild;
		}
	}
	
	/* progress */
	sUtils.progress={
		on:function(){
			var t = SUX.getModuleTop().selfObj || SUX.root;
			var w = (SUX.getModuleTop().selfObj)?SUX.getModuleTop().parent.SUX.getModuleTop():SUX.getModuleTop();
			if(t.progressOn)
				t.progressOn(w);
			return true;
		},
		off:function(){
			var t = SUX.getModuleTop().selfObj || SUX.root;
			var w = (SUX.getModuleTop().Sepoa.request.isPopup)?SUX.getModuleTop().parent.SUX.getModuleTop():SUX.getModuleTop();
			if(t.progressOff)
				t.progressOff(undefined,w);
		}
	}
	
	/* strings */
	sUtils.string={
		toBoolean: function(r) {
			if (typeof(r) == "string") r = r.toLowerCase();
			return (r == true || r == 1 || r == "true" || r == "1" || r == "yes" || r == "y" || r == "on");
		},
		dataReplacer:function(str, data){
			var reg = /\$([a-zA-Z_.]{0,})\{([\sa-zA-Z0-9#_.,\[\]]{0,})\}/g;
			var result = str.replace(reg, function() {
				var group = arguments[1];
				if(group == "CONFIG") {
					switch(arguments[2]){
					case 'CONTEXT_NAME':
						return window.CONTEXT_NAME;
						break;
					case 'TOKEN':
						return window.sTokenString;
						break;
					}
				}else if(group == "CURRENT") {
					switch(arguments[2]){
					case 'DATE':
						return sUtils.date.toString(new Date(), '%Y%m%d');
						break;
					case 'TIME':
						return sUtils.date.toString(new Date(), '%H%i%s');
						break;
					case 'TIME_STRING':
						return sUtils.date.getTimeString();
						break;
					}
				}else if(group == "NUMBER"){
					if(arguments[2]){
						return arguments[2].replace(/^\[([0-9,.]{0,}|PRICE)\][\s]{0,}([a-zA-Z0-9_.]{0,})/, function(){
							if(arguments[1] == "PRICE"){
								return sUtils.number.price(data[arguments[2]]);
							}else{
								return sUtils.number.format(data[arguments[2]], arguments[1]);
							}
						})
					}
				}else if(group == "DAY"){
					if(data[arguments[2]]){
						return sUtils.date.toString(sUtils.date.fromString(data[arguments[2]], '%Y%m%d'), (Sepoa.info.DATE_FORMAT || '%l'));
					}
				}if(group == "DATE"){
					if(data[arguments[2]]){
						return sUtils.date.toString(sUtils.date.fromString(data[arguments[2]], '%Y%m%d'), (Sepoa.info.DATE_FORMAT || '%Y/%m/%d'));
					}
				}else if(group == "TIME"){
					if(data[arguments[2]]){
						if(data[arguments[2]].length == 4){
							return sUtils.date.toString(sUtils.date.fromString(data[arguments[2]], '%H%i'), '%H:%i');
						}else if(data[arguments[2]].length == 6){
							return sUtils.date.toString(sUtils.date.fromString(data[arguments[2]], '%H%i%s'), '%H:%i:%s');
						}
					}
				}else if(group == "LANG"){
					return sUtils.cache.text.get(arguments[2]);
				}else if(group == "INFO"){
					return Sepoa.info[arguments[2]];
				}else if(group == "COMBO"){
					if(arguments[2]) {
						var m = arguments[2].match(/\[([a-zA-Z0-9,._#\{\}]{0,})\]([a-zA-Z0-9._]{0,})/);
						if(m){
							var list = sUtils.cache.combo.get(m[1]);
							var v = "";
							for(var idx in list) {
								if(list[idx].value == data[m[2]]) {
									v = list[idx].text;
									break;
								}
							}
							return v;
						}
					}
				}else if(group == "."){
					if(!data){
						return ""
					}else{
						var v = data[arguments[2]];
						if(v == null || v == undefined){
							return "";
						}else{
							return v;
						}
					}
				}else if(group == "DEC"){
					return new TextDecoder().decode(new Uint8Array(data[arguments[2]]));
				}
			});
			if(result.match(reg)){
				return sUtils.string.dataReplacer(result, data);
			}else{
				return result;
			}
		},
		trimStart:function(str) {
			if (!isNaN(str)) {
				str = str.toString();
			}
			return str.replace(/^\s+/, '');
		},
		trimEnd:function(str) {
			if (!isNaN(str)) {
				str = str.toString();
			}
			return str.replace(/\s+$/, '');
		},
		trim:function(str){
			return str.trim();
		},
		lpad:function(s, l, p){
			s = s+'';
		    while(s.length < l)
		        s = p +''+ s;
		    return s;
		},
		rpad:function(s, l, p){
			s = s+'';
		    while(s.length < l)
		        s = s +''+ p;
		    return s;
		},
		getWidth:function(text, size, font){
			let canvas = document.createElement('canvas');
			let context = canvas.getContext('2d');
			
			var textA = text.split("\n");
			
			var max = 0;
			for(var idx in textA){
				var line = textA[idx];
				context.font = size +" "+ font;
				var now = context.measureText(line).width;
				max = max > now ? max : now 
			}
			return max
		},
		getSecureString:function(text){
			const resultElement = document.createElement('div');
			const dummyElement = document.createElement('div');
			dummyElement.innerHTML = text;

			const childrens = dummyElement.childNodes;
			for(let children of childrens) {
				if(children instanceof HTMLElement) {
					if((children instanceof HTMLScriptElement) === false) {
						const element = document.createElement(children.tagName);
						
						const targetClassName = children.className;
						if(targetClassName) {
							element.className = targetClassName;
						}
						
						const targetStyle = children.getAttribute('style');
						if(targetStyle) {
							element.setAttribute('style', targetStyle);
						}
						
						const targetDataSet = Object.entries(children.dataset);
						if(targetDataSet && targetDataSet.length > 0) {
							for(let entry of targetDataSet) {
								const [key, value] = entry;
								element.dataset[key] = value;
							}
						}
						
						if(children.childElementCount > 0) {
							element.innerHTML = sUtils.string.getSecureString(children.innerHTML);
						} else {
							element.innerHTML = children.innerHTML;
						}

						resultElement.appendChild(element);
					}
				} else {
					resultElement.appendChild(children.cloneNode(true));
				}
			}

			return resultElement.innerHTML;
		},
		decodeHTML:function(str){
			var elem = document.createElement('textarea');
			elem.innerHTML = str;
			var result = elem.value;
			elem = null;
			return result;
		},
		isBlank:function(str){
			if(str == null || str == undefined || str.length == 0){
				return true;
			}
			return false;
		},
		randomUUID(){
			return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
				(c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
			);
		},
		randomKey(){
			return ('_'+[1e6]+1e3+4e3+8e3+1e9).replace(/[_01]/g, c => {
				if(c == '_'){
					return (10 + (crypto.getRandomValues(new Uint8Array(1))[0] & 5)).toString(16)
				}else{
					return (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
				}
			});
		},
		capitalize(str) {
			if(sUtils.string.isBlank(str)) {
				return "";
			}
			
			return `${str.charAt(0).toUpperCase()}${str.slice(1)}`;
		},
		getObjectType(obj){
			return Object.prototype.toString.call(obj);
		}
	}
	
	/* number */
	sUtils.number=function(){
		var createMask = function(n){
			var digitGroup = (Sepoa.info.DIGIT_GROUP || ",.").split("");
			var h = digitGroup[1];
			var m = digitGroup[0];
			var g = n.replace(/[^0\,\.]*/g, "");
		    var a = g.indexOf(".");
		    if (a > -1) {
		        a = g.length - a - 1
		    }
		    var c = g.indexOf(",");
		    if (c > -1) {
		        c = g.length - a - 2 - c
		    }
		    if (typeof h != "string") {
		        h = '.'
		    }
		    if (typeof m != "string") {
		        m = ','
		    }
		    var o = n.split(g)[0];
		    var l = n.split(g)[1];
		    return [a, c, o, l, h, m]
		}
		
		var i2k = ["", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구", "십"];
		var g2k = ["", "십", "백", "천"]
		var gk2k = ["", "만", "억", "조", "경", "해", "자", "양", "구", "간", "정", "재", "극"]
			
		
		return {
			unformat:function(data){
				var digitGroup = (Sepoa.info.DIGIT_GROUP || ",.").split("");
				return data.replace(new RegExp(digitGroup[0], "g"), "");
			},
			format:function(data, format){
				if (!format)
					return data;
				format = format.trim().toUpperCase();
				
				var a = createMask(format);

				var c = (parseFloat(data) < 0 ? "-" : "")+a[2];
				data=Math.abs(Math.round(parseFloat(data)*Math.pow(10, a[0] > 0 ? a[0] : 0))).toString();
				data=(data.length< a[0]? Math.pow(10, a[0]+1-data.length).toString().substr(1, a[0]+1)+data.toString(): data).split("").reverse();
				data[a[0]]=(data[a[0]]||"0")+a[4];

				if (a[1] > 0)
					for (var j = (a[0] > 0 ? 0 : 1)+a[0]+a[1]; j < data.length; j+=a[1])data[j]+=a[5];
				return c+data.reverse().join("")+a[3];
			},
			price:function(val){
				var result = "";
				if(isNaN(val)){
					return result;
				}else if(typeof(val) == 'number'){
					val = parseInt(val) + "";
				}
				var prefix = "";
				if(val.charAt(0) == "-"){
					prefix = "-";
					val = val.substring(1);
				}
				
				var manCount = 0;
				for(var i=0;i<val.length;i++){
					var strTextWord = i2k[val.charAt(i)];
					if(strTextWord != ""){
						manCount++;
						strTextWord += g2k[(val.length - (i+1)) % 4];
					}
					if(manCount != 0 && (val.length - (i+1)) % 4 == 0){
						manCount = 0;
						strTextWord = strTextWord + gk2k[(val.length - (i+1)) / 4];
					}
					result += strTextWord;
				}
				result = prefix+result;
				return result;
			}
		}
	}();
	
	/* date */
	sUtils.date={
		getToday:function(){
			return this.toString(new Date(), "%Y%m%d");
		},
		getYearMonth:function(){
			return this.toString(new Date(), "%Y%m");
		},
		getYear:function(){
			return this.toString(new Date(), "%Y");
		},
		getMonth:function(){
			return this.toString(new Date(), "%m");
		},
		getDay:function(){
			return this.toString(new Date(), "%d");
		},
		getTime:function(){
			return this.toString(new Date(), "%H%i%s");
		},
		getTimeString:function(){
			return (new Date()).getTime();
		},
		_lang:{
			en: {
				monthFullName:	["January","February","March","April","May","June","July","August","September","October","November","December"],
				monthShortName:	["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"],
				dayFullName:	["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"],
				dayShortName:	["Su","Mo","Tu","We","Th","Fr","Sa"]
			},
			ko:{
				monthFullName: ["1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"],
				monthShortName: ["1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월"],
				dayFullName: ["일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"],
				dayShortName: ["일", "월", "화", "수", "목", "금", "토"],
		    }
		},
		toString:function(val, format, language) {
			var strings = sUtils.date._lang[(language || "ko").toLowerCase()];
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
				var t = String(format||"%Y%m%d").replace(/%[a-zA-Z]/g, k);
			}
			
			return (t||String(val));
		},
		fromString:function(val, format, language) {
			var strings = sUtils.date._lang[(language || "ko").toLowerCase()];
			
			format = (format || "%Y%m%d").replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\\:|]/g, "\\$&");
			
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
	}
	
	/* cookie */
	sUtils.cookie=(sUtils.isTop)?{
		set:function(cName, cValue, cDay){
		    var expire = new Date();
		    expire.setDate(expire.getDate() + cDay);
		    cookies = cName + '=' + escape(cValue) + '; path=/ ';
		    if(typeof cDay != 'undefined') cookies += ';expires=' + expire.toGMTString() + ';';
		    document.cookie = cookies;
		},
		get:function(cName) {
		    cName = cName + '=';
		    var cookieData = document.cookie;
		    var start = cookieData.indexOf(cName);
		    var cValue = '';
		    if(start != -1){
		        start += cName.length;
		        var end = cookieData.indexOf(';', start);
		        if(end == -1)end = cookieData.length;
		        cValue = cookieData.substring(start, end);
		    }
		    return unescape(cValue);
		}
	}:parent.sUtils.cookie;
	
	/* cache */
	sUtils.cache=(sUtils.isTop)?(function(){
		var CacheManager = function(config){
			this.config = $.extend(true, {cache:false, dataType:"json"}, config)
			this.data = {manual:{}};
		}
		CacheManager.prototype.request=sUtils.loader.common;
		CacheManager.prototype.clear=function(flag){
			this.data = {manual:{}};
			if(flag !== false){
				this.request({mode:'cache', action:'clear', type:this.config.type});
			}
		}
		CacheManager.prototype.print=function(){
			this.data = {manual:{}};
			this.request({mode:'cache', action:'print', type:this.config.type});
		}
		CacheManager.prototype.get=function(code){
			if(this.config.type != 'module') {
				if(!code){
					return "";
				}
			}
			return this.request({mode:'cache', action:'get', type:this.config.type, code:code});
		}
		CacheManager.prototype.load=function(url, ajaxOptions){
			return JSON.parse(sUtils.string.dataReplacer(this.request({v:window._POABASE_VERSION_}, $.extend({cache:true, url:url, dataType:"text"}, ajaxOptions))));
		}
		var ExtCacheManager = function(config){
			CacheManager.call(this, $.extend(true, {}, config));
			this.urls = [];
		}
		ExtCacheManager.prototype = Object.create(CacheManager.prototype);
		ExtCacheManager.prototype.get = function(code){
			if(!code){
				return "";
			}
			var _code = code;
			if(this.config.defaultCode){
				if(!this.data.server){
					this.data.server = this.request({mode:'cache', action:'get', type:this.config.type, code:this.config.defaultCode});
				}else if(window._CACHE_TIME_ && this.data.server._CACHE_TIME_ != window._CACHE_TIME_){
					this.data.server = this.request({mode:'cache', action:'get', type:this.config.type, code:this.config.defaultCode});
					window._CACHE_TIME_ = this.data.server._CACHE_TIME_;
				}
			}else if(!this.data.server){
				this.data.server = {}
			}
			var result = null;
			if(code.indexOf('.') >= 0){
				code = code.split('.');
				if(this.data.manual[code[0]]){
					result = this.data.manual[code[0]][code[1]];
				}else{
					if(!this.data.server[code[0]]){
						var serverData = this.request({mode:'cache', action:'get', type:this.config.type, code:code[0]}) || {};
						this.data.server[code[0]] = serverData[code[0]];
					}
					result = (this.data.server[code[0]] || {})[code[1]];
				}
			}
			if(result){
				return result;
			}else{
				return sUtils.string.dataReplacer((this.config.errorMessage || ""), {CODE:_code});
			}
		}
		ExtCacheManager.prototype.load=function(urls){
			if(typeof(urls) == "string"){
				urls = {url : urls};
			}
			if(!Array.isArray(urls)){
				urls = [urls]
			}
			for(var i in urls){
				var item = urls[i];
				if(this.urls.indexOf(item.url) < 0 || item.origin){
					if(!item.origin)
						this.urls.push(item.url);
					this.request({v:window._POABASE_VERSION_}, {cache:true, url:item.url, async:false, manager:this, def:item.def,
						success:function(data){
							$.extend(this.manager.data.manual, data)
						},
						error:function(data){
							if(this.def){
								console.log('[sUtils cache] load backup : '+this.def);
								this.manager.load([{url:this.def, origin:this.url}]);
							}else{
								console.warn('[sUtils cache] fail to load : '+(this.url));
								this.manager.urls.splice(this.manager.urls.indexOf(this.url), 1);
							}
						}
					});
				}
			}
		}
		ExtCacheManager.prototype.clear=function(flag){
			this.urls = [];
			delete this.data.server
			if(flag !== false){
				this.request({mode:'cache', action:'clear', type:this.config.type});
			}
		}
		var cacheTypeList = ['grid', 'form', 'combo', 'text', 'module', 'client', 'personal'];
		var result = {
			clear:function(type){
				if(type == null){
					for(var i in cacheTypeList){
						this[cacheTypeList[i]].clear(false);
					}
					this.grid.request({mode:'cache', action:'clear'});
				}else{
					this[type].clear();
				}
			}
		};
		for(var i in cacheTypeList){
			var type = cacheTypeList[i]
			if(type == 'text'){
				result[type] = new ExtCacheManager({type:type, defaultCode:"MESSAGE;BUTTON", errorMessage:"Has not found text : $.{CODE}"});
			}else if(type == 'client'){
				result[type] = new ExtCacheManager({type:type});
			}else{
				result[type] = new CacheManager({type:type});
			}
		}
		
		return result
	}()):parent.sUtils.cache;
	
	/* file transper */
	sUtils.multipart=(function(){
		var $fileVar = {}
		$fileVar.send = function(conf, data, files){
			if(conf.progress !== false){
				conf.progress = true;
			}
			
			var xhr = new XMLHttpRequest();
			xhr.open('post', conf.url, (conf.async === false)? false : true);
			
			var formData = null;
			if(conf.contentType){
				switch(conf.contentType){
				case 'json':
					formData = JSON.stringify(data);
					//formData = encodeURIComponent(formData);
					xhr.setRequestHeader('Content-type', 'application/json;charset=UTF-8');
					break;
				}
			}else if(conf.multipart !== false){
				formData = new FormData();
				for(var i in data){
					formData.append(i, data[i]);
				}
				if(files){
					if(files instanceof FileList){
						for(var i=0;i<files.length;i++){
							var inputId = 'file_item_'+i;
							if(conf.input){
								inputId = conf.input;
							}
							formData.append(inputId, files[i], files[i].name); 
						}
					}else{
						formData.append('file_item_0', files, files.name);
					}
				}
			}else {
				formData = sUtils.url.toQueryString(data);
				xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
			}
			
			if(typeof(sTokenString) != "undefined" && sTokenString){
				xhr.setRequestHeader("s-token", sTokenString);
			}
			xhr.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
			if(conf.async && conf.isFileDownload){
				xhr.responseType = "arraybuffer"
			}else if(conf.async){
				xhr.responseType = "text"
			}
			xhr.onreadystatechange=function(){
				if(this.readyState == "4"){
					if(this.getResponseHeader('Sepoa-Status') == 'SESSIONCLOSE'){
						if(this._config.progress){
							sUtils.progress.off();
						}
						sUtils.system.login();
						return;
					}
					if (this.status === 200) {
						if(this._config.progress){
							sUtils.progress.off();
						}
						if(this._config.isFileDownload){
							var filename = "";
					        var disposition = this.getResponseHeader('Content-Disposition');
					        if (disposition && disposition.indexOf('attachment') !== -1) {
					            var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
					            var matches = filenameRegex.exec(disposition);
					            if (matches != null && matches[1]) filename = matches[1].replace(/['"]/g, '');
					            
					            filename = decodeURIComponent(filename);
						        
						        var type = this.getResponseHeader('Content-Type');
						        var blob = new Blob([this.response], { type: type });
					
						        if (typeof window.navigator.msSaveBlob !== 'undefined') {
						            window.navigator.msSaveBlob(blob, filename);
						        } else {
						            var URL = window.URL || window.webkitURL;
						            var downloadUrl = URL.createObjectURL(blob);
					
						            if (filename) {
						                var a = document.createElement("a");
						                if (typeof a.download === 'undefined') {
						                	window.open(downloadUrl);
						                } else {
						                    a.href = downloadUrl;
						                    a.download = filename;
						                    document.body.appendChild(a);
						                    a.click();
						                    document.body.removeChild(a);
						                }
						            } else {
						            	window.open(downloadUrl);
						            }
						            
						            setTimeout(function () { URL.revokeObjectURL(downloadUrl); }, 100);
						        }
					        }else{
					        	if(this._config.on && this._config.on.callback){
					        		var res = this.response;
					        		try{
					        			res = String.fromCharCode.apply(null, new Uint8Array(res));
					        			res = JSON.parse(res);
					        		}catch(e){}
					        		this._config.on.callback(res, this._config, this._data);
								}else{
									Alert(sUtils.cache.text.get('BASE_MESSAGE.0003'));
								}
					        }
						}else{
							if(this._config.on && this._config.on.callback){
								var res = this.response;
				        		try{
				        			res = JSON.parse(this.response);
				        		}catch(e){}
				        		this._config.on.callback(res, this._config, this._data);
							}
						}
					}else{
						if(this._config.progress){
							sUtils.progress.off();
						}
						Alert(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_NETWORK"));
					}
				}
			}
			
			xhr._config = conf;
			xhr._data = data;
			try{
				if(conf.progress){
					sUtils.progress.on();
				}
				xhr.send(formData);
				if(conf.async === false && xhr.status === 200 && !conf.isFileDownload){
					return JSON.parse(xhr.response);
				}
			}catch(e){
				Alert(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_NETWORK"));
			}
		}
		$fileVar.checkExt=function(config, params, files){
			if(config.ext){
				for(var i=0;i<files.length;i++){
					var name = files[i].name;
					var match = name.match(/\.([a-zA-Z]{0,})$/);
					if(!match || config.ext.indexOf(match[1].toLowerCase()) < 0 ){
						this.value = "";
						Alert(sUtils.string.dataReplacer(sUtils.cache.text.get("BASE_FILE.NOT_SUPPORT_EXT"), {EXT:(match?match[1]:" ")}));
						return false;
					}
				}
			}
			return true;
		}
		$fileVar.connector=function(config, params, files){
			if(config.foreach){
				for(var i=0;i<files.length;i++){
					var config = $.extend(true, {}, config);
					var params = $.extend(true, {}, params);
					
					if(config.on && config.on.beforeSend){
						config.on.beforeSend(config, params, files[i], i);
					}
					$fileVar.send(config, params, files[i]);
				}
			}else{
				if(config.on && config.on.beforeSend){
					config.on.beforeSend(config, params, files, 0);
				}
				return $fileVar.send(config, params, files);
			}
		}
		
		return{
			send:function(config, params){
				return $fileVar.connector(config, params);
			},
			upload:function(config, params){
				if(!params.files){
					if(!$fileVar.input){
						$fileVar.input = document.body.appendChild($('<input type="file" id="sutil_file_'+(new Date()).getTime()+'" style="display:none;" ></input>')[0])
						$fileVar.input.onchange=function(){
							if($fileVar.checkExt(this._config, this._params, this.files)){
								$fileVar.connector(this._config, this._params, this.files)
							}
						}
					}
					$fileVar.input.value = "";
					$fileVar.input._config = config;
					$fileVar.input._params = params;
					
					if(config.multiple){
						$fileVar.input.setAttribute("multiple", "");
					}else{
						$fileVar.input.removeAttribute("multiple");
					}
					
					$fileVar.input.click();
				}else{
					var files = params.files;
					params = $.extend(true, {}, params);
					delete params.files;
					
					if($fileVar.checkExt(config, params, files)){
						$fileVar.connector(config, params, files);
					}
				}
			},
			download:function(config, params){
				config = $.extend(true, {isFileDownload:true, async:true}, config);
				$fileVar.connector(config, params);
			}
		}
	})()
	
	/* SepoaExcelProcessor */
	sUtils.excel=(function(){
		return {
			upload:function(params, callback){
				config = {
					url:"/SepoaExcel.sux",
					async:true,
					ext:["xls", "xlsx"],
					multiple:false,
					input:"excelUploadInput",
					on:{}
				}
				params = $.extend(true, {
					excelType:'up'
				}, params)
				
				if(callback){
					config.on._org_callback = callback;
				}
				
				config.on.callback = function(data, config){
					if(config.progress){
						sUtils.progress.off();
					}
					
					if (window.ActiveXObject !== window.undefined) {
	                    var b = new window.ActiveXObject("Microsoft.XMLDOM");
	                    b.async = "false";
	                    data = b.loadXML(a)
	                }else{
	                	data = new DOMParser().parseFromString(data, "text/xml");
	                }
					
					var result = {
						status:"",
						message:"",
						result:[],
						param:{}
					};
					
					var status_nodes = data.getElementsByTagName("status");
					var message_nodes = data.getElementsByTagName("message");
					var result_nodes = data.getElementsByTagName("result");
					var param_nodes = data.getElementsByTagName("param");
					
					var status = "";
					if (status_nodes[0] && status_nodes[0].firstChild != null) {
						result.status = status_nodes[0].firstChild.nodeValue;
					}
					var message = "";
					if (message_nodes[0] && message_nodes[0].firstChild != null) {
						result.message=message_nodes[0].firstChild.nodeValue;
					}

					var result_size = result_nodes.length;
					for (var i = 0; i < result_size; i++) {
						if (result_nodes[i].firstChild != null) {
							result.result.push(result_nodes[i].firstChild.nodeValue);
						}
					}
					
					var param = Object();
					var param_size = param_nodes.length;
					for (var i = 0; i < param_size; i++) {
						if (param_nodes[i].firstChild != null) {
							var k = param_nodes[i].firstElementChild.firstChild.nodeValue;
							var v = "";
							var nv = param_nodes[i].lastElementChild.firstChild;
							if(nv){
								v = nv.nodeValue
							}
							result.param[k] = v;
						}
					}
					
					if(config.on._org_callback){
						config.on._org_callback(result);
					}else{
						if(return_obj.status == 'true' || return_obj.status == '1'){
							Alert(sUtils.cache.text.get('BASE_MESSAGE.0002'));
						}else{
							Alert(sUtils.cache.text.get('BASE_MESSAGE.0003'));
						}
					}
				}
				sUtils.multipart.upload(config, params);
			},
			download:function(params){
				config = {
					url:"/SepoaExcel.sux",
					async:true
				}
				params = $.extend(true, {
					excelType:'down'
				}, params)
				
				sUtils.multipart.download(config, params);
			}
		}
	})()
	
	/* SepoaFormater */
	sUtils.SepoaFormater=(function(){
		var SepoaFormater = function(data){
			this.dataMap = {};
			this.columns = [];
			this.str;
			
			if(typeof(data) == "string"){
				this._parseS2O(data);
			}else if(data instanceof Array){
				this._parseA2S(data);
			}
		};
		SepoaFormater.prototype = {
			_parseA2S:function(array){
				var sfArray = [];
				for(var i=0;i<array.length;i++){
					var row = array[i];
					if(i == 0){
						var cStr = "";
						for(var j in row){
							var cName = j.toUpperCase();
							this.columns.push(cName);
							this.dataMap[cName] = [];
							cStr += (cName + Sepoa.common.FIELD);
						}
						sfArray.push(cStr);
					}
					var rStr = "";
					for(var j in row){
						var cName = j.toUpperCase();
						var cData = row[j] || "";
						if(this.columns.indexOf(cName) + 1){
							this.dataMap[cName][i] = cData;
							rStr += (cData + Sepoa.common.FIELD);
						}else{
							rStr += Sepoa.common.FIELD;
						}
					}
					sfArray.push(rStr);
				}
				this.str=sfArray.join(Sepoa.common.LINE)+Sepoa.common.LINE;
			},
			_parseS2O:function(str){
				this.str = str;
				var rowArray = this.str.split(Sepoa.common.LINE);
				if(rowArray.length > 0) {
					this.columns = rowArray[0].toUpperCase().split(Sepoa.common.FIELD);
					for(var i = 0; i < this.columns.length; i++) {
						this.dataMap[this.columns[i]] = new Array();
					}
				}
				
				for(var i = 1; i < rowArray.length - 1; i++) {
					var colArray = rowArray[i].split(Sepoa.common.FIELD);
					for(var j = 0; j < this.columns.length; j++) {
						var dataArray = this.dataMap[this.columns[j]];
						dataArray.push(colArray[j]);
					}
				}
			},
			getValue : function(column, rowIdx){   
				rowIdx = (!rowIdx)?0:rowIdx;
				var dataArray = this.dataMap[column.toUpperCase()];
				if(!dataArray || !dataArray[rowIdx]){
					return
				}
				return dataArray[rowIdx];
			},
			getRowCount : function(){   
				return this.dataMap[this.columns[0]] ? this.dataMap[this.columns[0]].length : 0;
			},
			getArrayMap : function(){
				var r = [];
				for(var i=0;i<this.getRowCount();i++){
					var m={};
					for(var j=0;j<this.columns.length;j++){
						m[this.columns[j]] = (this.getValue(this.columns[j], i) || "");
					}
					r.push(m);
				}
				return r;
			}
		}
		
		return {
			get:function(data){
				return new SepoaFormater(data);
			}
		}
	})();
	
	/* url */
	sUtils.url={
		toSepoaParams:function(obj, flag){
			var result = [];
			for(var i in obj){
				result.push(i);
				result.push(obj[i]);
			}
			
			//result = encodeURIComponent(result.join(Sepoa.common.FIELD) + Sepoa.common.FIELD);
			result = result.join(Sepoa.common.FIELD) + Sepoa.common.FIELD;
			if(flag !== false){
				result = "&" + Sepoa.common.KEY_PARAMS + "=" + encodeURIComponent(result);
			}
			return result;
		},
		toQueryString:function(obj, encode){
			var s = $.param(obj);
			if(encode === false){
				s = decodeURIComponent(s)
			}
			return s;
		},
		checkContext:function(url){
			if(window.CONTEXT_NAME && url.indexOf(window.CONTEXT_NAME) != 0){
				console.warn("Context-path : "+window.CONTEXT_NAME +", url : "+url);
			}
			return url;
		}
	}
	
	/* image */
	sUtils.image={
		toBase64:function(url, callback, params){
			var image = new Image();
		    image.params = params;
		    image.crossOrigin="anonymous"
		    image.onload = function () {
		        var canvas = document.createElement('canvas');
		        canvas.width = this.naturalWidth;
		        canvas.height = this.naturalHeight;
		        canvas.getContext('2d').drawImage(this, 0, 0);
		        callback(canvas.toDataURL('image/png'), this.params);
		    };
		    image.src = url;
		}
	}
	
	/* form */
	sUtils.form=(function(){
		var formController = function(){
			this.data = {};
		}
		formController.prototype.submit=function(config){
			var form = document.body.appendChild($(sUtils.string.dataReplacer('<form method="POST" action="$.{url}" target="$.{title}" style="display:none;"></form>', config))[0]);
			for(var i in this.data){
				form.appendChild($(sUtils.string.dataReplacer('<input type="hidden" name="$.{name}" value="$.{value}"></input>', {name:i, value:this.data[i]}))[0]);
			}
			sUtils.url.checkContext(config.url);
			
			form.submit();	
			form.parentElement.removeChild(form);
			form = null;
		}
		formController.prototype.clean=function(){
			this.data = {};
		}
		formController.prototype.put=function(k, v){
			this.data[k] = v;
		}
		formController.prototype.putAll=function(obj){
			for(var i in obj){
				this.data[i] = obj[i];
			}
		}
		formController.prototype.get=function(k){
			this.data[k];
		}
		formController.prototype.getAll=function(k){
			return this.data;
		}
		return {
			get:function(){
				return new formController();
			}
		};
	})();
	
	/* popup */
	sUtils.popup=(function(){
		var dock = {};
		var createOptionString=function(config){
			var options = {
				toolbar:'no',menubar:'no',status:'yes',scrollbar:'yes',resizable:'yes',
				width:config.width,height:config.height,left:config.left,top:config.top
			}
			
			var optionArray = [];
			for(var i in options){
				optionArray.push(i+"="+options[i]);
			}
			return optionArray.join(",");
		}
		
		return {
			modal:function(config){
				config = $.extend(true, {
					width:(SUX.getModuleTop().outerWidth * 0.8),
					height:(SUX.getModuleTop().outerHeight * 0.8),
					title:"",
					params:{},
					code:"noname",
					viewport:{
						x:0,
						y:0,
						width:0,
						height:0,
						container:SUX.getModuleTop().document.getElementsByClassName('container')[0]
					}
				}, config||{})
				
				//if(!config.url){
				//	Alert(sUtils.cache.text.get("BASE_POPUP.NOT_FOUND_URL"))
				//	return;
				//}
				
				if(dock[config.code] && dock[config.code].unload)
					dock[config.code].unload();
				
				sUtils.url.checkContext(config.url);
				
				var popup = dock[config.code] = new SepoaDhtmlXWindowsObject();
				popup.setSkin('material');
				popup.createPopup(config.code,config.title,undefined,config.width,config.height);
				popup.setViewport.apply(popup, [config.viewport.x, config.viewport.y, config.viewport.width, config.viewport.height, config.viewport.container]);
				if(config.url){
					popup.attachURL(config.url, config.params);
				}
				popup.setShow();
				if(config.header === false){
					popup[popup.name].hideHeader();
				}
				return popup;
			},
			real:function(config){
				config = $.extend(true, {
					width:(SUX.getModuleTop().outerWidth * 0.8),
					height:(SUX.getModuleTop().outerHeight * 0.8),
					title:"windowPopup_"+(new Date().valueOf()),
					isPost:true,
					params:{
						parameterId:(new Date().valueOf()),
						isRealPopup:true,
						isPopup:true
					}
				}, config||{})
				
				config.left = (screen.width - config.width)/2;
				config.top = (screen.height - config.height)/2; 
				
				var optionString = config.isTab?'':createOptionString(config);
				var win = dock[config.title] = window.open((config.isPost?'':config.url), config.title , optionString);
				if(config.isPost){
					if(!SUX.root.modalPopupInfo){
						SUX.root.modalPopupInfo = {};
					}
					SUX.root.modalPopupInfo[config.params.parameterId] = {
						params:config.params,
						selfObj:win
					};
					var form = sUtils.form.get();
					form.putAll(config.params);
					form.submit(config);
				}
			},
			getWindowObject:function(code){
				return dock[code];
			}
		}
	})()
	
	/* ux */
	sUtils.ux=(function(){
		var events = {};
		modules.extend(events, 'EventManager');
		
		var $val = {};
		$(window).on({
			resize:function(){
				if($val._windowResizeTimeOut)
					clearTimeout($val._windowResizeTimeOut);
				$val._windowResizeTimeOut = setTimeout(function(){
					events.callEvent('onWindowResize', []);
					events.callEvent('onWindowResize', []);
				}, 100)
			},
			beforeunload:function(e){
				return $val._pageOutWarningStatus;
			}
		});
		
		$val.windowResizeHandler = function(ready){
			var conts = $('.sepoaux.screen_area > .container')
			for(var i=0;i<conts.length;i++){
				var cont = conts[i];
				if(!cont.classList.contains('sepoaux_screen_scroll')){
					var lowers = $(cont).find('.lowerDiv');
					for(var j=0;j<lowers.length;j++){
						var lower = lowers[j];
						if(cont == $(lower).parents('.container')[0]){
							lower.style.height="100%";
							var height = $(cont).innerHeight() - lower.offsetTop - ($(lower).outerHeight() - $(lower).innerHeight());
							for(var k in {'gridbox_dhx_web':0, 'chartbox':0}){
								if(lower.classList.contains(k)){
									height -= 20;
								}
							}
							lower.style.height = height + "px";
						}
					}
				}
				
				var dhxClasses = ['.sepoaux_list', '.sepoaux_grid', '.sepoaux_accordion', '.sepoaux_vault', '.sepoaux_carousel','.sepoaux_layout', '.sepoaux_tabbar'];
				var dhxElements = $(cont).find(dhxClasses.join(', '));
				for(var j=0;j<dhxElements.length;j++){
					var dhxElement = dhxElements[j];
					if(dhxElement.classList.contains('sepoaux_grid')){
						if(!dhxElement.dhtmlxObject._realfake){
							dhxElement.dhtmlxObject.entBox.style.width="100%";
							dhxElement.dhtmlxObject.setSizes();
						}
					}else{
						if(dhxElement && dhxElement.dhtmlxObject && dhxElement.dhtmlxObject.setSizes){
							dhxElement.dhtmlxObject.setSizes();
						}
					}
				}
			}
		}
		
		events.attachEvent('onWindowResize', $val.windowResizeHandler);
		events.callEvent('onWindowResize', []);
		
		return{
			setPageOutWarning:function(flag){
				$val._pageOutWarningStatus = flag;
			},
			extendModule:modules.extend,
			extend:$.extend,
			callEvent:events.callEvent.bind(events),
			attachEvent:events.attachEvent.bind(events),
			detachEvent:events.detachEvent.bind(events),
			setSizes:$val.windowResizeHandler,
			getDocumentNumber:function(type){
				return sUtils.loader.common({mode:"docnumber", type:type}, {dataType:'text'})
			},
			getCommonPackage:function(){
				return sUtils.loader.common({mode:"common"})
			},
			getRootScreenID:function(){
				return $(window.document).find('.sepoaux.screen_area')[0].id;
			}
		}
	})()
	
	/* validator */
	sUtils.validator=(function(){
		var policy={
			pass:{
				LENGTH_MAX:20,
				LENGTH_MIN:8,
				USE_CHAR:true,
				USE_NUMBER:true,
				USE_LOWER_UPPER:false,
				USE_SPECIAL:false
			}
		}
		var _fn ={
			IRS_NO:function(irsNum){
				var checkID = new Array(1, 3, 7, 1, 3, 7, 1, 3, 5, 1);
			    var tmpBizID, i, chkSum = 0,
			        c2, remander;
			    irsNum = irsNum.replace(/-/gi, '');
	
			    for (i = 0; i <= 7; i++) chkSum += checkID[i] * irsNum.charAt(i);
			    c2 = "0" + (checkID[8] * irsNum.charAt(8));
			    c2 = c2.substring(c2.length - 2, c2.length);
			    chkSum += Math.floor(c2.charAt(0)) + Math.floor(c2.charAt(1));
			    remander = (10 - (chkSum % 10)) % 10;
	
			    if (Math.floor(irsNum.charAt(9)) == remander) {
			        return true;
			    } else {
			        return false;
			    }
			},
			PASSWORD:function(pass){
				if(pass.length > policy.pass.LENGTH_MAX){
					return false;
				}
				if(pass.length < policy.pass.LENGTH_MIN){
					return false;
				}
				if(policy.pass.USE_CHAR){
					var ch = pass.replace(/[^a-zA-Z]/g, "");
					if(ch.length == 0){
						return false;
					}else if(USE_LOWER_UPPER){
						var lower = ch.replace(/[A-Z]/g, "");
						if(lower.length == 0 || lower.length == ch.length){
							return false;
						}
					}
				}
				if(policy.pass.USE_NUMBER){
					var ch = pass.replace(/[^0-9]/g, "");
					if(ch.length == 0){
						return false;
					}
				}
			}
		}
		return {
			valiate:function(type, value){
				if(type && _fn[type]){
					return _fn[type](value);
				}else{
					Alert(sUtils.string.dataReplacer(sUtils.cache.text.get('BASE_VALIDATOR.NOT_SUPPORT_TYPE'), {TYPE:type}));
					return false;
				}
			}
		}
	})()
	
	/* workflow */
	sUtils.workflow=(function(){
		var cache = null;
		var loadend = false;
		var __init=function(){
			if(!loadend){
				cache = sUtils.cache.module.get();
				loadend = true;
			}
		}		
		return {
			clear:function(){
				loadend = false;
			},
			check:function(module){
				__init();
				if(cache){
					module = module.split(',');
					for(var i in module){
						var m = module[i];
						if(cache[m] !== undefined){
							return true;
						}
					}
					return false;
				}
				return true;
			},
			getChildren:function(module){
				__init();
				if(cache && cache[module] && cache[module].CHILDREN.length > 0){
					return cache[module].CHILDREN
				}
				return [];
			},
			getAlias:function(module){
				__init();
				if(cache && cache[module] && cache[module].ALIAS){
					return cache[module].ALIAS;
				}
			},
			getOptions:function(module, isArray){
				__init();
				if(cache && cache[module] && cache[module].OPTIONS){
					if(isArray){
						return cache[module].OPTIONS.split(',')
					}else{
						return cache[module].OPTIONS
					}
				}
			},
			testOperator:function(module, test){
				var op = sUtils.workflow.getOptionString(module);
			}
		}
	})()
	
	/* system */
	sUtils.system=(function(){
		var $private = {
			login:{}
		}
		return {
			login:function(config){
				if(Sepoa.config.get('sepoa.server.development.flag') === 'false') {
					Alert(Sepoa.cache.text.get('LOGIN.SESSION_CLOSE'), function(){
						SUX.root.document.location.href = window.CONTEXT_NAME+'/UxDefault/Index/IndexLogin.sux?house_code='+Sepoa.info.HOUSE_CODE;
					});
					return;
				}
				
				if(Sepoa.config.get('sepoa.session.close.navigate') === 'index'){
					Alert(Sepoa.cache.text.get('LOGIN.SESSION_CLOSE'), function(){
						SUX.root.document.location.href = window.CONTEXT_NAME+'/';
					})
					return;
				}
				if($private.login.use){
					return;
				}
				$private.login.use = true;
				config = $.extend(true, {
					url:window.CONTEXT_NAME+'/UxDefault/Index/IndexLogin.sux?house_code='+Sepoa.info.HOUSE_CODE,
					title:sUtils.cache.text.get("LOGIN.SESSION_CLOSE"),
					message:{
						success:sUtils.cache.text.get("LOGIN.LOGIN_SUCCESS")
					},
					params:{}
				}, config);
				if(!config.callback){
					config.params.callback = function(){
						new SepoaCommonObject().callSepoaJSCommon(function(data) {
							if(Sepoa.config.get('sepoa.server.development.flag') === 'true') {
								if(data.info.ID === Sepoa.info.ID) {
									Alert(config.message.success);
									return true;
								} else {
									SUX.root.document.location.href = window.CONTEXT_NAME+'/Main.sux';
								}
							}
						});
					}
				}else{
					config.params.callback = config.callback;
				}
				var popup = sUtils.popup.modal(config);
				popup.attachEvent('onClose', function(){
					$private.login.use = false;
				})
			}
		}
	})()
	
	/* connector */
	sUtils.connector=(function(){
		const rd = {
			"c1":[234,170,145],"c2":[234,170,146],"r1":[61],"r2":[61,61],
			"header":[97, 112, 112, 108, 105, 99, 97, 116, 105, 111, 110, 47, 115, 101, 112, 111, 97, 115, 111, 102, 116]
		}
		
		return {
			api:function(type, data, files){
				const fd = new FormData();
				fd.append("data", new Blob([
					btoa(JSON.stringify(data)).replace(new RegExp().compile(sUtils.string.dataReplacer("$DEC{r2}$", rd), "g"), sUtils.string.dataReplacer("$DEC{c2}", rd))
					.replace(new RegExp().compile(sUtils.string.dataReplacer("$DEC{r1}$", rd), "g"), sUtils.string.dataReplacer("$DEC{c1}", rd))
				], {
					type:"application/sepoasoft",
					filename:"data.sux"
				}))
				if(files){
					if(files) {
						if(files instanceof FileList || files instanceof Array) {
							for(let i = 0; i < files.length; i++) {
								const fileId = files[i].id ?? 'file_item_' + i;
								fd.append(fileId, files[i], files[i].name);
							}
						} else if(files instanceof File) {
							const fileId = files.id ?? 'file_item_0';
							fd.append(fileId, files, files.name);
						}
						delete this.params.files;
					}
				}
				
				return new Promise((resolve, reject)=>{
					const config = {
						method:'POST',
						body:fd
					}
					if(!sUtils.string.isBlank(sTokenString)){
						config.headers = {
							"s-token":sTokenString
						}
					}
					fetch(`${CONTEXT_NAME}/SepoaAPI/${type}`, config).then(res => {
						return new Promise((resolve, reject)=>{
							res.text().then(txt=>{
									if(res.headers.get("content-type")?.startsWith(sUtils.string.dataReplacer("$DEC{header}", rd))){
										resolve(JSON.parse(atob(txt
											.replace(sUtils.string.dataReplacer("$DEC{c2}", rd), sUtils.string.dataReplacer("$DEC{r2}", rd))
											.replace(sUtils.string.dataReplacer("$DEC{c1}", rd), sUtils.string.dataReplacer("$DEC{r1}", rd)))));
									}else{
										resolve(JSON.parse(txt));
									}
								}
							);
						});
					}).then(json=>{
						resolve(json);
					})
				});
			},
			service:function(config, params){
				if(!config.nickName){
					Alert(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_NOTFOUND"));
					console.warn(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_NOTFOUND_SERVICE"));
					return;
				}
				if(!config.methodName){
					Alert(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_NOTFOUND"));
					console.warn(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_NOTFOUND_METHOD"));
					return;
				}
				if(!config.conType){
					Alert(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_NOTFOUND"));
					console.warn(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_NOTFOUND_CONNECTION"));
					return;
				}
				
				var data = $.extend(true, {
					nickName:config.nickName,
					methodName:config.methodName,
					conType:config.conType
				}, params)
				
				
				if(config.progress){
					sUtils.progress.on();
				}
				
				var result = {};
				sUtils.loader.common(data, $.extend(true, {
					url: window.CONTEXT_NAME+"/SepoaAjax.sux",
					dataType:"xml",
					async:true,
					_config:config,
					success:function(data){
						if(this._config.progress){
							sUtils.progress.off();
						}
						
						result = {
							status:"",
							message:"",
							result:[],
							param:{}
						};
						
						var status_nodes = data.getElementsByTagName("status");
						var message_nodes = data.getElementsByTagName("message");
						var result_nodes = data.getElementsByTagName("result");
						var param_nodes = data.getElementsByTagName("param");
						
						var status = "";
						if (status_nodes[0] && status_nodes[0].firstChild != null) {
							result.status = status_nodes[0].firstChild.nodeValue;
						}
						var message = "";
						if (message_nodes[0] && message_nodes[0].firstChild != null) {
							result.message=message_nodes[0].firstChild.nodeValue;
						}

						var result_size = result_nodes.length;
						for (var i = 0; i < result_size; i++) {
							if (result_nodes[i].firstChild != null) {
								result.result.push(result_nodes[i].firstChild.nodeValue);
							}
						}
						
						var param = Object();
						var param_size = param_nodes.length;
						for (var i = 0; i < param_size; i++) {
							if (param_nodes[i].firstChild != null) {
								var k = param_nodes[i].firstElementChild.firstChild.nodeValue;
								var v = "";
								var nv = param_nodes[i].lastElementChild.firstChild;
								if(nv){
									v = nv.nodeValue
								}
								result.param[k] = v;
							}
						}
						if(result.status == 'SESSIONCLOSE'){
							sUtils.system.login();
							return;
						}
						if(this._config.callback){
							this._config.callback(result);
						}
					},
					error:function(request, status, error){
						if(this._config.progress){
							sUtils.progress.off();
						}
						if (request.readyState == 4) {
							Alert(sUtils.string.dataReplacer("code:$.{status}\nmessage:$.{message}\nerror:$.{error}", {status:result.status, message:request.responseText, error:error}));
				        }else if (request.readyState == 0) {
				            Alert(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_NETWORK"));
				        }else {
				        	Alert(sUtils.cache.text.get("BASE_CONNECTOR.ERROR_UNKNOWN"));
				        }
					}
				}, config));
				if(result.status == 'SESSIONCLOSE'){
					sUtils.system.login();
					return;
				}
				return result;
			}
		}
	})()
	
	/* watch */
	sUtils.watch = (function(){
		return {
			countdown: function(config){
				var CountDown = function(config){
					this.config = Object.assign({ 
													TIME : 0, 
													FORMAT : (Sepoa.info.LANGUAGE == 'KO' ? '%d일 %h시간 %m분 %s초' : '%d days %h hours %m minutes %s seconds'),
													CALLBACK : undefined
												}, config)
					this.ms = this.config.TIME;
					this.format = this.config.FORMAT;
					this.callback = this.config.CALLBACK;
				}
				
				CountDown.prototype.start = function(){
					if(!this.callback){
						console.error('[sUtils.watch.countdown] Callback function can not be undefined.');
						return;
					}
					
					var intervalId = null;
					var interval = function(){
						this.ms -= 1000;
						
						if(this.ms < 0){
							this.end();
							return;
						}

						var obj = this._convert(this.ms);
						
						this.callback({
										endFlag : false, 
										formatString :  this._formatting(this.format, obj), 
										remainTime : this.ms,
										remainTimeObject : obj
						});
					}.bind(this)
					
					intervalId = setInterval(interval, 1000);
					this.intervalId = intervalId;
				}
				
				CountDown.prototype.end = function(){
					this.callback({endFlag : true});
					clearInterval(this.intervalId);
				}		
				
				CountDown.prototype._convert = function(millisecond){
					var day = String(Math.floor(millisecond / (1000 * 60 * 60 * 24)));
					var hour = String(Math.floor((millisecond / (1000 * 60 * 60)) % 24));
					var minute = String(Math.floor((millisecond / (1000 * 60)) % 60));
					var second = String(Math.floor((millisecond / 1000) % 60));
					var result = {
							DAY : day,
							HOUR : hour,
							MINUTE : minute,
							SECOND : second,
					};			
					
					return result; 
				}
				
				CountDown.prototype._formatting = function(format, obj){
					var result = format.replaceAll('%d', obj.DAY)
					      			   .replaceAll('%H', obj.HOUR.padStart(2, '0'))
					      			   .replaceAll('%h', obj.HOUR)
					      			   .replaceAll('%M', obj.MINUTE.padStart(2, '0'))
					      			   .replaceAll('%m', obj.MINUTE)
					      			   .replaceAll('%S', obj.SECOND.padStart(2, '0'))
					      			   .replaceAll('%s', obj.SECOND);
					return result;
				}				
				
				return new CountDown(config);
			}
		}
	})();
	
	sUtils.browser={
		isMobile:()=>{
			const navi = navigator.userAgent||navigator.vendor||window.opera;
			return (/(android|bb\d+|meego).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows ce|xda|xiino/i.test(navi)||/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(navi.substr(0,4)));
		}
	}
	
	sUtils.object=(()=>{

		const getIn = function(obj, field) {
			try {
				if (field === null || field === "" || typeof field === "undefined") {
					return null;
				}
				return _getInRecurs(obj, field.split("."));
			} catch (error) {
				return undefined;
			}
		}

		const _getInRecurs = function(obj, fieldArray){
			if(obj === undefined || obj === null){
				return undefined;
			}
			
			let field = fieldArray.shift();
			const m = field.match(/^([\S]{1,})\[\]$/);
		    if(m){
		    	field = m[1];
		    }
		    
		    if(fieldArray.length == 0){
		    	return obj[field];
		    }else{
		    	return _getInRecurs(obj[field], fieldArray);
		    }
		}

		const setIn = function(obj, field, value, clone){
			let result = obj;
			if(clone === true){
				result = structuredClone(obj);
			}
			try {
				_setInRecurs(obj, field.split("."), value);
				return true;
			} catch (error) {
				console.error(error);
			}
			return false;
		}

		const _setInRecurs = function(obj, fieldArray, value){
			let field = fieldArray.shift();
			let isArray = false;
			const m = field.match(/^([\S]{1,})\[\]$/);
			if(m){
				isArray = true;
		    	field = m[1];
		    }
		    
		    if(fieldArray.length == 0){
		    	obj[field] = value;
		    	return true;
		    }else{
		    	obj[field] ??= (isArray?[]:{});
		    	return _setInRecurs(obj[field], fieldArray, value);
		    }
		}
		
		return {
			extend:function(){
				const length = arguments.length;
				
				let deep = false;
				let i = 0;
				
				if (Object.prototype.toString.call(arguments[0]) === '[object Boolean]') {
					deep = arguments[0];
					i++;
				}
				
				const result = arguments[i];
				i++;
				
				const merge = (result, obj) => {
					for (let prop in obj) {
						if (Object.prototype.hasOwnProperty.call(obj, prop)) {
							const objType = Object.prototype.toString.call(obj[prop]);
							if (deep && (['[object Object]', '[object Array]'].indexOf(objType)+1)) {
								result[prop] ??= ((objType=='[object Object]')?{}:[]);
								sUtils.object.extend(true, result[prop], obj[prop]);
							}else{
								result[prop] = obj[prop];
							}
						}
					}
				};
				for (; i < length; i++) {
					merge(result, arguments[i]);
				}
				return result;
			},
			isBoolean:(obj)=>{
				return sUtils.string.getObjectType(obj) == "[object Boolean]"
			},
			isString:(obj)=>{
				return sUtils.string.getObjectType(obj) == "[object String]"
			},
			isNumber:(obj)=>{
				return sUtils.string.getObjectType(obj) == "[object Number]"
			},
			isArray:(obj)=>{
				return sUtils.string.getObjectType(obj) == "[object Array]"
			},
			isObject:(obj)=>{
				return sUtils.string.getObjectType(obj) == "[object Object]"
			},
			isFunction:(obj)=>{
				return sUtils.string.getObjectType(obj) == "[object Function]"
			},
			hasPath:(obj, field)=>{
				return getIn(obj, field) !== undefined
			},
			getIn,
			setIn,
		}
	})();
	
	sUtils.token=(()=>{
		return {
			get:(type, ext)=>{
				return new Promise((resolve, reject)=>{
					sUtils.connector.api('service', {
						__salt1:sUtils.string.randomUUID(),
						target:{name:"sepoa.svc.system.token.TokenService",action:"getToken"},
						__salt2:sUtils.string.randomUUID(),
						data:{disposable:true,autoRenewal:false,data:{type:type,ext:ext}},
						__salt3:sUtils.string.randomUUID()
					}).then(r=>{
						if(r.flag){
							resolve(r.result[0]);
						}else{
							reject(r.message);
						}
					})
				})
			}
		}
	})();
}
