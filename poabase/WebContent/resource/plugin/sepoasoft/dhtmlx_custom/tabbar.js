/**
 * 3.dhtmlxTabbar
 */
function SepoaDhtmlXTabBar(g, m){
	Sepoa.cache.text.load('/resource/plugin/sepoasoft/dhtmlx_custom/lang/'+Sepoa.info.LANGUAGE+'.js', false);
	dhtmlXTabBar.call(this, g, m);
	g.dhtmlxObject = this;
	this.conf.tabsWidth.material=[0, 32];
	
	$(this.tabsArea).find('.dhxtabbar_arrow_img').addClass("xi-sepoa-dummy");
	
	this.attachEvent('onTabClose', function(id) {
		var attachedObject = this.tabs(id).getAttachedObject();
		if(attachedObject) {
			var pools = Object.values(attachedObject.contentWindow.SUX.pools);
			
			if(pools && pools.length > 0) {
				const isPageCloseWarning = pools.reduce(function(prev, cur) {
					return prev || cur.pageCloseWarn
				}, false);
				
				if(isPageCloseWarning) {
					if(confirm(Sepoa.cache.text.get('TABBAR.WARNING_TAB_CLOSE'))) {
						return true;
					} else {
						return false;
					}
				}
			}
		}
		
		return true;
	})
	this.attachEvent('onTabChanged',function(){
    	SUX.callEvent('onWindowResize',[]);
    })
    SUX.attachEvent('onWindowResize',function(){
    	this.setSizes();
    }.bind(this));
    if(g.useContextMenu){
        this.contextMenuObj = new SepoaContextMenuManager();
        this.attachEvent('onContextMenuClick', function(tabName, tabId, cb){
        	switch(tabName){
    	    	case "target_tab_close":
    	    		if(this.callEvent('onTabClose', [tabId])){
    	    			this.tabs(tabId).close();
    	    		}
    	    		break;
    	    	case "all_tab_close":
    	    		for(var i in this.t){
    	    			if(this.callEvent('onTabClose', [i])){
    		    			this.tabs(i).close();
    		    		}
    	    		}
    	    		break;
    	    	case "left_tab_close":
    	    		var orgArr = $.extend(true, this.t, {});
    	    		var keysArr = Object.keys(orgArr);
    	    		var curIdx = keysArr.indexOf(tabId);
    	    		
    	    		for(var i=curIdx; i>0; i--){
    	    			if(tabId == keysArr[i]) continue;
    	    			if(this.callEvent('onTabClose', [keysArr[i]])){
    		    			this.tabs(keysArr[i]).close();
    		    		}
    	    		}
    	    		
    	    		break;
    	    	case "right_tab_close":
    	    		var orgArr = $.extend(true, this.t, {});
    	    		var keysArr = Object.keys(orgArr);
    	    		var curIdx = keysArr.indexOf(tabId);
    	    		
    	    		for(var i=curIdx; i<keysArr.length; i++){
    	    			if(tabId == keysArr[i]) continue;
    	    			if(this.callEvent('onTabClose', [keysArr[i]])){
    		    			this.tabs(keysArr[i]).close();
    		    		}
    	    		}
    	    		break;
    	    	case "other_tab_close":
    	    		for(var i in this.t){
    	    			if(tabId == i) continue;
    	    			if(this.callEvent('onTabClose', [i])){
    		    			this.tabs(i).close();
    		    		}
    	    		}
    	    		break;
    	    	default:
    	    		cb(tabName, tabId);
    	    		break;
        	}
        })
        this.tabsArea.addEventListener('mouseup', function(e) {
        	if ($(e.target).hasClass('dhxtabbar_tab_text') && ((e.button == 2) || (e.which == 3))) {
        		var basicOption = {
    				"target_tab_close" : {
    					"name": Sepoa.cache.text.get('TABBAR.TARGET_TAB_CLOSE'),
    					"callback" : function(){
    						this.callEvent('onContextMenuClick', ['target_tab_close', this.contextTab])
    					}.bind(this)
    				},
    				"all_tab_close" : {
    					"name": Sepoa.cache.text.get('TABBAR.ALL_TAB_CLOSE'),
    					"callback" : function(){
    						this.callEvent('onContextMenuClick', ['all_tab_close', this.contextTab])
    					}.bind(this)
    				},
    				"other_tab_close" : { 
    					"name": Sepoa.cache.text.get('TABBAR.OTHER_TAB_CLOSE'),
    					"callback" : function(){
    						this.callEvent('onContextMenuClick', ['other_tab_close', this.contextTab])
    					}.bind(this)
    				},
    				"left_tab_close" : { 
    					"name": Sepoa.cache.text.get('TABBAR.LEFT_TAB_CLOSE'),
    					"callback" : function(){
    						this.callEvent('onContextMenuClick', ['left_tab_close', this.contextTab])
    					}.bind(this)
    				},
    				"right_tab_close" : { 
    					"name": Sepoa.cache.text.get('TABBAR.RIGHT_TAB_CLOSE'),
    					"callback" : function(){
    						this.callEvent('onContextMenuClick', ['right_tab_close', this.contextTab])
    					}.bind(this)
    				}
    			}; 
        		
        		this.contextTab = e.target.parentElement._tabId
        		this.contextMenuObj.setContextMenu(e.target, {trigger : 'right', items : basicOption});
        		$(e.target).contextMenu();
        		e.target.leftClickFlag = false;
      		}
        }.bind(this));
        
        this.tabsArea.addEventListener('contextmenu', function(e) {
        	event.preventDefault();
        }.bind(this));
    }
	
	if (this.conf.autoload.json != null) {
        this.loadStruct(this.conf.autoload.json, this.conf.autoload.onload)
    } else {
        if (this.conf.autoload.xml != null) {
            this.loadStruct(this.conf.autoload.xml, this.conf.autoload.onload)
        } else {
            if (this.conf.autoload.tabs != null) {
                this.loadStruct({
                    tabs: this.conf.autoload.tabs
                }, this.conf.autoload.onload)
            }
        }
    }
    if (this.conf.autoload.arrows_mode != null) {
        this.setArrowsMode(this.conf.autoload.arrows_mode)
    }
    this._fixTabsOfs();
    this.setSizes();
}
SepoaDhtmlXTabBar.prototype = Object.create(dhtmlXTabBar.prototype);
SepoaDhtmlXTabBar.prototype.constructor = SepoaDhtmlXTabBar;
SepoaDhtmlXTabBar.prototype._setTabText = function(g, e, c) {
    if (!this.t[g]) {
        return
    }
    var a = false;
    if (typeof(c) == "undefined" || c == null) {
        c = this._getLabelWidth(e, this.t[g].conf.close);
        a = true
    }
    this.t[g].tab.style.width = c + "px";
    this.t[g].tab.childNodes[0].innerHTML = e;
    this.t[g].conf.text = e;
    this.t[g].conf.width = c;
    this.t[g].conf.autowidth = a
};
SepoaDhtmlXTabBar.prototype._setTabActive = function(c, a, f) {
    if (!this.t[c] || this.t[c].conf.active) {
        return
    }
    if (typeof(a) == "undefined") {
        a = true
    }
    if (a == true && this.callEvent("onSelect", [c, this.conf.lastActive]) !== true) {
        return
    }
    this.setTabInActive();
    this.t[c].conf.active = true;
    if (this.conf.contZone) {
        this.t[c].cell.cell.style.visibility = "visible";
        this.t[c].cell.cell.style.top = "0px";
        this.t[c].cell.cell.style.zIndex = 1
    }
    this.t[c].tab.className = this._tabCss(c);
    this.conf.lastActive = c;
    this.setSizes();
    if (this.conf.url_demand == true) {
        this._loadURLOnDemand(c)
    }
    this.setSizes();
    if(f !== false){
    	this.callEvent('onTabChanged',[c]);
    }
};
SepoaDhtmlXTabBar.prototype.getTabsCount=function(){
	return Object.keys(this.t).length
}
SepoaDhtmlXTabBar.prototype.showTabsArea=function(){
	this.cont.classList.remove("sepoa-custom-hide-tabs");
	this.tabsArea.style.display = "";
    this.setSizes();
}
SepoaDhtmlXTabBar.prototype.hideTabsArea=function(){
	this.cont.classList.add("sepoa-custom-hide-tabs");
	this.tabsArea.style.display = "none";
    this.setSizes();
}
SepoaDhtmlXTabBar.prototype.getTabId=function(i){
	var a = -1;
    var e = this.tabsArea.childNodes[1].firstChild;
    if(e.childNodes[i+1]){
    	return e.childNodes[i+1]._tabId;
    }
}