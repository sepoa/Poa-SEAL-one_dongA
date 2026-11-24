function SepoaClock(vp, template, interval){
	if(typeof(vp) == 'string'){
		vp = document.getElementById(vp);
	}
	if(!vp){
		return;
	}
	
	this.conf = {
		vp:vp,
		template:template||Sepoa.cache.text.get('COMMON.CURRENT_TIME'),
		interval:interval||500
	};
	
	this.btnObj = new SepoaButtonObject();
	this.btnObj.addButton({
		id:"CURRENT_TIME",
		div:this.conf.vp,
		color:"#ffffff",
		template:"<span style='color:red'>#TEXT#</span>"
	});

	var that = this;
	
	this.update = function(){
		that.lastUpdateDate = new Date();
		that.btnObj.btn('CURRENT_TIME').setText(dhx4.date2str(that.lastUpdateDate, that.conf.template));
	};
	
	this.setStart();
}
SepoaClock.prototype.getDateTime=function(){
	return this.lastUpdateDate;
}
SepoaClock.prototype.setTemplate=function(str){
	this.conf.template = str;
}
SepoaClock.prototype.setPause=function(){
	clearInterval(this.quartz);
	this.quartz = null;
}
SepoaClock.prototype.setStart=function(){
	if(this.quartz){
		return;
	}
	this.quartz=setInterval(this.update,this.conf.interval); 
}