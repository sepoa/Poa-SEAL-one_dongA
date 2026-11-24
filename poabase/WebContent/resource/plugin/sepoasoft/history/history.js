'use strict';
function HistoryManager(c){
	var store = {
		history:[],
		redo:[],
		itemMap:{},
		lastID:0,
		isEnabled:true,
		disableStack:[]
	}
	var winObjects = {}
	this.conf = $.extend(true, {
		stacksize:10,
		onChange:(function(a) {
		    return function() {
		        return (function(data, hasU, hasR){
		        	//console.log(data)
		        	//if(!this.conf.isHistory){
		        	//	console.log("hasUndo : " + hasU + ", hasRedo : "+ hasR);
		        	//}
		        }).apply(a, arguments)
		    }
		})(this),
		width:400,
		height:500,
		isHistory:false
	}, c || {});
	
	this.enable=function(f){
		if(f === undefined){
			return store.isEnabled;
		}else if(f){
			store.disableStack.pop();
			if(store.disableStack.length <= 0){
				store.isEnabled = true;
			}
		}else{
			store.disableStack.push(0);
			store.isEnabled = false;
		}
	}
	
	this.showHistory=function(flag){
		if(!winObjects.history){
			var w = winObjects.history = {}
			w.base = new dhtmlXWindows();
			w.window = w.base.createWindow({
			    id:"history",
			    left:20,
			    top:30,
			    width:this.conf.width,
			    height:this.conf.height,
			    center:true,
			    resize:false,
			    modal:true,
			    park:false,
			    move:false,
			    caption:"History"
			});
			w.window.button('park').hide();
			w.window.button('minmax').hide();
			w.window.button('close').attachEvent('onClick', function(win, button){
				win.hide();
			});
			w.window.attachEvent('onHide', function(){
				this.setModal(false);
			})
			w.window.attachEvent('onShow', function(){
				this.setModal(true);
			})
			w.listObj = w.window.attachList({
				type:{
					template:"#id# / #time# / #desc#"
				}
			});
			w.listObj.attachEvent('onItemClick', (function(a) {
			    return function() {
			        return (function(id, ev, html){
			        	var i = w.listObj.get(id);
			        	Confirm('Load History</br>'+i.id+" / "+i.desc, function() {
					        return (function(f){
					        	if(f){
				        			winObjects.history.window.hide();
				        			this.push($.extend(true, {}, i.data), "load history : "+i.id);
				        			this.enable(false);
				        			this.conf.onChange($.extend(true, {}, i.data));
				        			this.enable(true);
				        		}
					        	
					        }).apply(a, arguments)
					    })
			        }).apply(a, arguments)
			    }
			})(this))
		}
		winObjects.history.listObj.clearAll();
		winObjects.history.listObj.parse(store.history, "json");
		winObjects.history.window.show();
	}
	this.clear=function(){
		if(store.isEnabled){
			store.history.splice(0, store.history.length);
			store.redo.splice(0, store.redo.length);
			store.lastID = 0;
			store.itemMap={},
			this.conf.onChange(null, false, false);
		}
	}
	this.push=function(data, desc){
		if(store.isEnabled){
			var d = new Date();
			var item = {
				id:'#'+(++store.lastID),
				desc:desc,
				time:lpad(d.getHours(),2,'0')+":"+lpad(d.getMinutes(),2,'0')+":"+lpad(d.getSeconds(),2,'0'),
				data:$.extend(true, {}, data)
			}
			
			if(store.history.length == this.conf.stacksize){
				store.history.shift();
			}
			
			store.history.push(item);
			if(!this.conf.isHistory){
				store.redo.splice(0, store.redo.length);
				this.conf.onChange(null, true, false);
			}else{
				store.itemMap[item.id] = item;
			}
		}
	}
	this.moveIndex=function(val){
		if(store.isEnabled){
			if(!this.conf.isHistory){
				this.store.cursor += val;
			}else{
				this.push($.extend(true, {}, this.getItemById(val)), "move "+val);
			}
		}
	}
	this.getItemById=function(id){
		return store.itemMap[id];
	}
}