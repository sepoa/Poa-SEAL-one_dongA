SepoaMainContainerObject=function(c){
	dhtmlx.extend(this, dhtmlx.Settings);
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.config = $.extend(true, {}, c);
	this._extModule();
	this.draw();
	this.extendO2W();
}
SepoaMainContainerObject.prototype._extModule=function(){
	var fList = this._frameType[this.config.type || "layout"];
	for(var i in fList){
		this[i] = fList[i].bind(this);
	}
}
SepoaMainContainerObject.prototype.progressOn=function(w){
	var target = this.getContentsCell(w); 
	if(target){
		target.progressCount = (target.progressCount||0) + 1;
		target.progressOn();
	}
}
SepoaMainContainerObject.prototype.progressOff=function(flag, w){
	var target = this.getContentsCell(w);
	if(target){
		target.progressCount = (target.progressCount||0) - 1;
		if(flag || target.progressCount <= 0){
			target.progressCount = 0;
			target.progressOff();
		}
	}
}
SepoaMainContainerObject.prototype.extendO2W=function(){
	var f = ['progressOn','progressOff','openContainerUrl', 'getContentsCell']
	for(var i in f){
		window[f[i]] = dhtmlx.bind(this[f[i]], this);
	}
}
SepoaMainContainerObject.prototype._frameType={
	tabbar:{
		draw:function(){
			this.config.tabbar.useContextMenu=true;
			
			this.containerObj = new SepoaDhtmlXTabBar($.extend(true, {parent:this.config.parent}, this.config.tabbar));
			if(this.config.tabbar.tabs){
				for(var i in this.config.tabbar.tabs){
					var t = this.config.tabbar.tabs[i];
					this.containerObj.tabs(t.id).attachURL(t.url);
				}
			}
			this.containerObj.attachEvent('onTabChanged', function(id){
				var t = this.tabs(id)
				if(t){
					var f = t.getFrame();
					if(f){
						if(f.contentWindow.SUX){
							f.contentWindow.SUX.callEvent('onActive');
						}
					}
				}
			})
			this.containerObj.attachEvent('onTabClose', function(id){
				if(id == 'SUMMARY'){
					return false;
				}
				return true;
			})
		},
		getContentsCell:function(w){
			var target = null;
			if(w){
				this.containerObj.forEachTab(function(tab){
					if($(tab.cell).find('iframe').get(0).contentWindow == w){
						target = tab;
					}
				});
			}else{
				target = this.containerObj.tabs(this.containerObj.getActiveTab());
			}
			return target;
		},
		openContainerUrl:function(id, url, param, menuName){
			try {
				this.callEvent('onBeforeChangeContainer', []);
				this.progressOff(true);
				
				var t = this.containerObj.tabs(id);
				if(!t){
					if(this.containerObj.getTabsCount() >= (this.config.tabbar.maxTabCount || 99)){
						if(this.containerObj.containerObj) {
							this.containerObj.tabs(this.containerObj.containerObj.getTabId(1)).close();	
						} else {
							this.containerObj.tabs(this.containerObj.getTabId(1)).close();
						}						
					}
					this.containerObj.addTab(id, menuName || Sepoa.cache.text.get('MENU.'+id));
					t = this.containerObj.tabs(id);
				}
				param = param || {}
				param.__SID__ = id;
				t.attachURL(window.CONTEXT_NAME+url,null,param);
				t.setActive(false);	
			} catch (e) {
				console.log(e);
			}
		}
	},
	layout:{
		draw:function(){
			this.containerObj = new SepoaDhtmlXLayoutObject($.extend(true, {parent:this.config.parent}, this.config.layout));
			if(this.config.layout.cells){
				for(var i in this.config.layout.cells){
					var c = this.config.layout.cells[i]
					this.containerObj.cells(c.id).attachURL(c.url);
					if(c.contents === true){
						this.getContentsCell=function(){
							this.containerObj.cells(c.id); 
						}
					}
				}
			}
		},
		getContentsCell:function(){
			this.containerObj.cells("a");
		},
		openContainerUrl:function(id, url, param){
			param = param || {}
			param.__SID__ = id;
			this.callEvent('onBeforeChangeContainer', []);
			this.progressOff(true);
			this.containerObj.cells("a").attachURL(url,null,param);
		}
	}
}

SepoaScreenInfoObject=function(){
	this.status = false;
	this._renderBase();
	window.toggleScreenInfo = dhtmlx.bind(this.toggleScreenInfo, this);
	window.showScreenInfo = dhtmlx.bind(this.showScreenInfo, this);
}
SepoaScreenInfoObject.prototype._renderBase=function(){
	this.cont = document.createElement('div');
	this.cont.className='sepoaux screeninfo';
	this.cont.style.display='none';
	document.body.appendChild(this.cont);
}
SepoaScreenInfoObject.prototype.showScreenInfo=function(info, e){
	if(this.status){
		var cont = this.cont;
		if(this.lastID != info.id){
			cont.innerHTML = info.id+'<br/>'+info.path;
			this.lastID = info.id;
		}
		cont.style.display='inherit';
		
		var padding = 15;
		var x = (e.clientX+padding);
		var bodyW = document.body.offsetWidth;
		if(bodyW - padding - x - cont.offsetWidth < 5){
			x = x - (cont.offsetWidth + padding + padding);
		}
		
		var y = (e.clientY+padding)
		var bodyY = document.body.offsetHeight;
		if(bodyY - padding - y - cont.offsetHeight < 5){
			y = y - (cont.offsetHeight + padding + padding);
		}
		
		this.cont.style.left = x + 'px';
		this.cont.style.top  = y  + 'px';
	}
}
SepoaScreenInfoObject.prototype.toggleScreenInfo = function(){
	this.status = (!this.status);
	if(!this.status && this.cont){
		this.cont.style.display="none";
	}
}

function getMenuData(profileCode){
	var tree = [];
	doServiceAjax("sepoa.svc.admin.AD_421", "CONNECTION", "doQueryMenuFilter", {HOUSE_CODE: Sepoa.info.HOUSE_CODE, "CODE":Sepoa.info.MENU_PROFILE_CODE}, function(so){
        var sf = new SepoaFormater(so.result[0]);
        var menuItems = {}
        var menuItemsList = [];
        for(var i=0;i<sf.getRowCount();i++) {
            var menuData = {};
            for(var j=0;j<sf.columns.length;j++){
                var columnName = sf.columns;
                menuData[columnName[j]]= sf.getValue(columnName[j],i);
            }
            menuData.childs = [];
            menuItems[menuData.MENU_ID] = menuData;
            menuItemsList.push(menuData);
        }
        
        for(var i in menuItemsList){
            var item = menuItemsList[i];
            var parent = item.PARENT_MENU_ID;
            if(parent == "0"){
                tree.push(item);
            }else{
                menuItems[item.PARENT_MENU_ID].childs.push(item);
            }
        }
    }, false);
	return tree;
}

function checkNotice(){
	doServiceAjax('sepoa.svc.admin.AD_201','CONNECTION','selectNoticePopup', {POPUP_LOCATION:"M"}, function( so ){
		if(so.status == 1){
			var sf = new SepoaFormater(so.result[0]);
			var sfList = sf.getArrayMap();
			
			var l = [];
			for(var idx in sfList) {
				var item = sfList[idx];
				l.push(item.NOTICE_NUMBER);
			}
			
			var c = sUtils.cookie.get('sepoa.notice.ignore');
			if(c){
				c = c.split(',');  
				for(var i in c){
					if(l.indexOf(c[i])+1){
						l.splice(l.indexOf(c[i]), 1);
					}
				}
			}
			if(l.length > 0){
				PopupOpenPost(window.CONTEXT_NAME+'/Admin/NoticeView/AdminNoticeViewCarousel.sux', {NOTICELIST:l.join(',')}, '' , 1300, '', Sepoa.cache.text.get('MAIN.NOTICE'),'common');
			}
		}
	});
}