(function(w){
	/* Object.values() [IE] */
	(function(o, p){
		if(p in o?o[p]?false:true:true){
			o[p]=function(obj) {
				var allowedTypes = ["[object String]", "[object Object]", "[object Array]", "[object Function]"];
				var objType = Object.prototype.toString.call(obj);
				if(obj === null || typeof obj === "undefined") {
					return null;
				} else if(!~allowedTypes.indexOf(objType)) {
					return [];
				} else {
					// if ES6 is supported
					if (Object.keys) {
						return Object.keys(obj).map(function (key) {
							return obj[key];
						});
					}
					
					var result = [];
					for (var prop in obj) {
						if (obj.hasOwnProperty(prop)) {
							result.push(obj[prop]);
						}
					}
					
					return result;
				}
			};
		}
	})(Object, 'values');
	
	/* String.trimStart() [IE, Edge(non-standard)] */
    (function(o,p){
        if(p in o?o[p]?false:true:true){
            var r=/^\s+/;
            o[p]=o.trimLeft||function(){
                return this.replace(r,'')
            }
        }
    })(String.prototype,'trimStart');

    /* String.trimEnd() [IE, Edge(non-standard)] */
    (function(o,p){
        if(p in o?o[p]?false:true:true){
            var r=/\s+$/;
            o[p]=o.trimRight||function(){
                return this.replace(r,'')
            }
        }
    })(String.prototype,'trimEnd');
    
    /* String.replaceAll(regexp, str) [IE] */
    (function(o,p){
        if(p in o?o[p]?false:true:true){
            o[p]=function(r, n){
            	if(typeof(r) == "string"){
            		return this.replace(new RegExp(r, 'g'), n);
            	}else{
            		return this.replace(r,n);
            	}
            }
        }
    })(String.prototype,'replaceAll');
    
    /* String.capitalize(str) [non-standard] */
    try{
	    Object.defineProperty(String.prototype, 'capitalize', {
	    	value: function() {
	    		return this.charAt(0).toUpperCase() + this.slice(1);
	    	},
	    	enumerable: false
	    });
    }catch(e){}
    
    /* Map.put [Sepoa Spec]*/
    (function(o,p){
        if(p in o?o[p]?false:true:true){
            o[p]=o.set
        }
    })(Map.prototype,'put');
})(window);