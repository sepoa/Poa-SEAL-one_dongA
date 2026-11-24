dhtmlx.ui.Map.prototype = {
	addRect : function(id, points, userdata) {
		this._createMapArea(id, "RECT", points, userdata);
	},
	addPoly : function(id, points, userdata) {
		this._createMapArea(id, "POLY", points, userdata);
	},
	_createMapArea : function(id, shape, coords, userdata) {
		var extra_data = "";
		if (arguments.length == 4)
			extra_data = "userdata='" + userdata + "'";
		this._map
				.push("<area " + this._key + "='" + id + "' shape='" + shape
						+ "' coords='" + coords.join() + "' " + extra_data
						+ "></area>");
	},
	addSector : function(id, alpha0, alpha1, x, y, R, ky, userdata) {
		var points = [];
		points.push(x);
		points.push(Math.floor(y * ky));
		for (var i = alpha0; i < alpha1; i += Math.PI / 18) {
			points.push(Math.floor(x + R * Math.cos(i)));
			points.push(Math.floor((y + R * Math.sin(i)) * ky));
		}
		points.push(Math.floor(x + R * Math.cos(alpha1)));
		points.push(Math.floor((y + R * Math.sin(alpha1)) * ky));
		points.push(x);
		points.push(Math.floor(y * ky));

		return this.addPoly(id, points, userdata);
	},
	render : function(obj) {
		var d = dhtmlx.html.create("DIV");
		d.style.cssText = "position:absolute; width:100%; height:100%; top:0px; left:0px;";
		d.className = "dhx_map_div";
		obj.appendChild(d);
		var src = dhtmlx._isIE ? ""
				: "src='data:image/gif;base64,R0lGODlhEgASAIAAAP///////yH5BAUUAAEALAAAAAASABIAAAIPjI+py+0Po5y02ouz3pwXADs='";
		d.innerHTML = "<map id='" + this._id + "' name='" + this._id + "'>"
				+ this._map.join("\n") + "</map><img " + src
				+ " class='dhx_map_img' usemap='#" + this._id + "'>";

		obj._htmlmap = d; // for clearing routine

		this._map = [];
	}
}

SepoaDhtmlXChart = function(_container, _view, _customOption) {

	this.dHtmlXChart = null;
	this.container = _container;
	this.view = _view;
	this.customViewName = _customOption;
	this.customAxisFlag = false;
	
	this.startIndex = 1;
	
	this.defaultData = [{id:this.startIndex, x:50, y:50, tooltip:"default"}];

	this.xAxisKey = "x";
	this.yAxisKey = "y";
	this.axisMoveFlag = false;
	
	this.matrixArea = null;
	this.matrixColor = new Array();
	this.defaultMatrixColor = "#ffffff";
	
	this.xAxisCount = null;
	this.yAxisCount = null;
	
	this.xRange = null;
	this.yRange = null;
	
	this.checkArea = null;
	this.nameList = null;
	this.areaNameArea = null;
	
	this.legendArea = null;
	this.legendLayout = "x";
	this.legendSize = 20;
	this.legendData = new Array();
	
	this.xAxisTitle = "__";
	this.yAxisTitle = "__";
	
	this.xAxis = {
		title : this.xAxisTitle,
		start : 0,
		step : 50,
		end : 100
	};
	this.yAxis = {
		title : this.yAxisTitle,
		start : 0,
		step : 50,
		end : 100
	};
	this.itemDesign = {
		radius : 5,
		borderColor : "#f38f00",
		borderWidth : 1,
		color : "#ff9600",
		type : "d",
		shadow : true
	};
	this.legend = {
		layout:"x",
		width: 75,
		margin:5,
		align:"center",
		valign:"top",
	}
	
	if($('#'+this.container).height() > $('#'+this.container).width()){
		$('#'+this.container).height($('#'+this.container).width());
	}else{
		$('#'+this.container).width($('#'+this.container).height());
	}
	
	dhtmlXChart.call(this, {
		view : _view,
		container : _container,
		value : "#" + this.yAxisKey + "#",
		xValue : "#" + this.xAxisKey + "#",
		xAxis : this.xAxis,
		yAxis : this.yAxis,
		item : this.itemDesign,
		legend : this.legend,
		tooltip: "#tooltip#",
	});
	if(this.customViewName == "matrix"){
		this.parse(this.defaultData, "json");
		this.clearAll();
		this.setMatrixArea(4, ["50"],["50"]);
	}
	
	this.deleteDefault();
	this.clearLegendArea();
};
SepoaDhtmlXChart.prototype = Object.create(dhtmlXChart.prototype);
SepoaDhtmlXChart.prototype.constructor = SepoaDhtmlXChart;
SepoaDhtmlXChart.prototype.deleteDefault = function() {
	this.setAxisTitle();
	var chartDiv = this.$view;
	var chartDivClassName = "#"+chartDiv.id.trim();
	$(chartDivClassName+' canvas[canvas_id="legend"]').remove();
	
};
SepoaDhtmlXChart.prototype.setMatrixArea = function(_countMatrix, _xRange, _yRange) {
	if(!this.matrixArea){
		this.matrixArea = this.getNewArea();
		this.matrixArea.show();
	}else{
		this.matrixArea.empty();
	}
	
	
	this.xRange = _xRange;
	this.yRange = _yRange;
	
	var colorAreaTable = $("<table></table>");
	colorAreaTable.css({
		 "border-collapse":"collapse"
	});
	colorAreaTable.attr("width", "100%");
	colorAreaTable.attr("height", "100%");
	
	this.xAxisCount = null;
	this.xAxisCount = null;
	if(!_countMatrix){
		if(_xRange && _yRange){
			this.xAxisCount = _xRange.length+1;
			this.yAxisCount = _yRange.length+1;
		}else{
			consol.log("[ERROR] SepoaDhtmlXChart.prototype.setMatrixArea : 파라메터 데이터 부족");
		}
	}else{
		if(!_xRange && !_yRange){
			this.xAxisCount = parseInt(Math.sqrt(_countMatrix));
			this.yAxisCount = _countMatrix / this.xAxisCount;
		}else if(!_yRange){
			this.xAxisCount = _xRange.length+1;
			this.yAxisCount = _countMatrix/(_xRange.length+1);
		}else if(!_xRange){
			this.xAxisCount = _yRange.length+1;
			this.yAxisCount = _countMatrix/(_yRange.length+1);
		}else{
			this.xAxisCount = _xRange.length+1;
			this.yAxisCount = _yRange.length+1;
		} 
	}
	
	this.matrixArea.append(colorAreaTable);
	
	var trHeightTot = 0;
	for(var i=this.yAxisCount-1;i>=0 ; i--){
		var colorAreaTr = $("<tr></tr>");
		colorAreaTr.attr("width", "100%");
		colorAreaTr.attr("height", (typeof(_yRange)=="undefined"||_yRange[i]==null||!this.axisMoveFlag)?(100/this.yAxisCount)+"%":_yRange[i]+"%");
		
		colorAreaTable.append(colorAreaTr);
		
		var tdwidthTot = 0;
		for(var j=0;j<this.xAxisCount ; j++){
			var colorAreaTd = $("<td></td>");
			
			if(j==this.xAxisCount-1){
				colorAreaTd.attr("width", (100-tdwidthTot)+"%");
			}else{
				colorAreaTd.attr("width", (typeof(_xRange)=="undefined"||_xRange[j]==null||!this.axisMoveFlag)?(100/this.xAxisCount)+"%":_xRange[j]+"%");
				tdwidthTot+= colorAreaTd.width();
			}
			colorAreaTd.css("border","1px solid #eeeeee");
			colorAreaTd.css("background-color", this.defaultMatrixColor);
			colorAreaTr.append(colorAreaTd);
		}
		if(i!=(this.yAxisCount-1)){
			trHeightTot+=(typeof(_yRange)=="undefined"||_yRange[i]==null||!this.axisMoveFlag)?(100/this.yAxisCount):_yRange[i];
		}
		if(i==0){
			$(colorAreaTable.find("tr")[i]).attr("height", (100-trHeightTot)+"%");
		}		
	}
	this.setMatrixColor();
};
SepoaDhtmlXChart.prototype.setData = function(_chartData, _AxisKey) {
	this.chartData = _chartData;

	if (_AxisKey != null) {
		this.xAxisKey = _AxisKey.x;
		this.yAxisKey = _AxisKey.y;
	}
};
SepoaDhtmlXChart.prototype.setYAxis = function(_xAxisInfo, _yAxisInfo) {
	if(_xAxisInfo instanceof(Array)){
		this.customAxisFlag = true;
		this.xAxis = _xAxisInfo;
		this.yAxis = _yAxisInfo;
	}else{
		this.xAxis = _xAxisInfo;
		this.yAxis = _yAxisInfo;
	}
};
SepoaDhtmlXChart.prototype.setMatrixRange = function(_xRange, _yRange){
	this.axisMoveFlag = true;
	this.xRange = _xRange;
	this.yRange = _yRange;
	
	var _x = null;
	var _y = null;
	if(!(_xRange instanceof(Array))){
		_xRange = [_xRange];
	}
	if(!(_yRange instanceof(Array))){
		_yRange = [_yRange];
	}	
	this.setMatrixArea(null,_xRange, _yRange);
}
SepoaDhtmlXChart.prototype.setMatrixColor = function(_color){
	_color = typeof _color !== 'undefined' ? _color : this.matrixColor;
	this.matrixColor = _color;
	
	if(this.matrixColor == null){
		return;
	}
	
	for(var y=this.yAxisCount-1;y>=0;y--){
		var tr = $(this.matrixArea.find("tr")[y]);
		for(var x=0;x<this.xAxisCount;x++){
			var colorIndex = this.getColorIndex(x,y);
			var td = $(tr.find("td")[x]);
			td.css("background-color", (this.matrixColor.length == 0 || typeof(this.matrixColor[colorIndex]) == "undefiend")?this.defaultMatrixColor:this.matrixColor[colorIndex]);
		}
	}
};
SepoaDhtmlXChart.prototype.getColorIndex = function(_xIndex, _yIndex){
	_yIndex++;
	this.customViewName == ""
	return ((this.yAxisCount-_yIndex) * this.xAxisCount) + _xIndex;
};

SepoaDhtmlXChart.prototype.setCustomXAxis = function(nameList, align){
	var chartDiv = this.$view;
	var chartDivClassName = "#"+chartDiv.id.trim();
	$(chartDivClassName+" .dhx_axis_item_x").remove();
	
	
}

SepoaDhtmlXChart.prototype.redraw = function(){
	this.refresh();
	this.deleteDefault();
	this.setMatrixArea(this.xAxisCount * this.yAxisCount);
	this.setMatrixColor(this.matrixColor, this.xRange, this.yRange);
	this.setAxisTitle(this.xAxisTitle, this.yAxisTitle);
}

SepoaDhtmlXChart.prototype.addAreaName = function(_text, _x, _y){
	if(!this.areaNameArea){
		this.areaNameArea = this.getNewArea();
		this.areaNameArea.show();
	}
	
	if(this.nameList == null){
		this.nameList = [{text:_text, x:_x, y:_y}];
	}else{
		this.nameList[this.nameList.length] = {text:_text, x:_x, y:_y};
	}
		
	var tmpName = this.nameList[this.nameList.length -1];
	
	var areaName = $("<div></div>");
	this.areaNameArea.append(areaName);
	
	areaName.html(tmpName.text);
	var textOffset = areaName.offset();
	var parentOffset = this.areaNameArea.offset();
	
	var xMeasure = this.areaNameArea.width() / 100;
	var yMeasure = this.areaNameArea.height() / 100;
	
	areaName.css({
		 position:"absolute"
		,width:'100px'
		,"z-index":0
		,'text-align':'center'
		,'font-size':15
		,'white-space':'nowrap'
	});
	areaName.css({
		top:(((100-tmpName.y)*yMeasure)-(areaName.height()/2))+"px"
		,left:((tmpName.x*xMeasure)-50)+"px"
	});
	
}

SepoaDhtmlXChart.prototype.clearAreaName = function(){
	if(this.areaNameArea ==null) return;
	this.areaNameArea.empty();
	this.nameList = [];
}

SepoaDhtmlXChart.prototype.getNewArea = function(){
	var tmp = $("<div></div>");
	
	var chartDiv = this.$view;
	var chartDivClassName = "#"+chartDiv.id.trim();
	var chartDivOffset = $(chartDivClassName).offset(); 
	var graphAreaOffset = $(chartDivClassName+" .dhx_map_div").offset();
	var graphAreaSize = new Object();
	var graphAreaOffset = $(chartDivClassName+" .dhx_map_div").offset();
	graphAreaSize.height = $(chartDivClassName+" .dhx_map_div").outerHeight();
	graphAreaSize.width = $(chartDivClassName+" .dhx_map_div").outerWidth();
	
	tmp.css({
		 position:"absolute"
		,width:graphAreaSize.width+"px"
		,height:graphAreaSize.height+"px"
		,top:(graphAreaOffset.top-chartDivOffset.top)+"px"
		,left:(graphAreaOffset.left-chartDivOffset.left)+"px"
		,"z-index":0
	});
	
	tmp.hide();
	
	$(chartDivClassName).append(tmp);
	return tmp;
}

SepoaDhtmlXChart.prototype.setCheckArea = function(_flag){
	
	if(!_flag){
		if(this.checkArea){
			this.checkArea.hide();
		}
		return;
	}
	
	if(!this.checkArea){
		this.checkArea = this.getNewArea();
		this.checkArea.show();
	}else{
		this.checkArea.empty();
	}
	this.checkArea.css({
		opacity:0.2
	});
	var colorAreaTable = $("<table></table>");
	colorAreaTable.css({
		 "border-collapse":"collapse"
	});
	colorAreaTable.attr("width", "100%");
	colorAreaTable.attr("height", "100%");
	
	for(var i=0;i<10 ; i++){
		var colorAreaTr = $("<tr></tr>");
		colorAreaTr.attr("width", "100%");
		colorAreaTr.attr("height","10%");
		
		var bg_color = '#ffffff';
		if(i%2 == 0){
			bg_color='#999999';
		}
		
		for(var j=0;j<10 ; j++){
			var colorAreaTd = $("<td></td>");
			
			colorAreaTd.attr("width", "10%");
			colorAreaTd.css("border","1px solid #000000");
			colorAreaTd.css("background-color", bg_color);
			
			colorAreaTr.append(colorAreaTd);
		}
		colorAreaTable.append(colorAreaTr);
	}
	this.checkArea.append(colorAreaTable);
}

SepoaDhtmlXChart.prototype.setAxisTitle = function(_xAxisTitle, _yAxisTitle){
	
	_xAxisTitle = typeof _xAxisTitle !== 'undefined' ? _xAxisTitle : "";
	_yAxisTitle = typeof _yAxisTitle !== 'undefined' ? _yAxisTitle : "";
	
	this.xAxisTitle = _xAxisTitle;
	this.yAxisTitle = _yAxisTitle;
	
	var chartDiv = this.$view;
	var chartDivClassName = "#"+chartDiv.id.trim();
	$(chartDivClassName+" .dhx_axis_title_x").html(_xAxisTitle);
	$(chartDivClassName+" .dhx_axis_title_y").html(_yAxisTitle);
	var dhx_axis_title_y = $(chartDivClassName+" .dhx_axis_title_y")
	var width = dhx_axis_title_y.width();
	var height = dhx_axis_title_y.height();
	dhx_axis_title_y.css({
		left:(height/2)-(width/2)
	});	
}

SepoaDhtmlXChart.prototype.addCustomLegend = function(text, color, mode){
	
	if(typeof mode == 'undefined' || mode == "" || mode != "RE"){
		this.legendData.push({text:text, color:color});
	}
	
	this.legendArea.show();
	var legendTable  = null;
	if(this.legendArea.children("table").length == 0){
		legendTable = $("<table></table>");
		this.legendArea.append(legendTable);
	}else{
		legendTable = $(this.legendArea.children("table"));
	}
	
	var tr = null;
	
	if(this.legendLayout == "x"){
		if(legendTable.find("tr").length < 1){
			tr = $("<tr></tr>");
			legendTable.append(tr);
		}else{
			tr = $(legendTable.find("tr"));
		}
	}else{
		tr = $("<tr></tr>");
		legendTable.append(tr);
	}
	
	tr.css({
		height:(this.legendSize+5)+"px"
	});
	
	var colorTd = $("<td></td>");
	var colorDIV = $("<div></div>");
	var textTd = $("<td></td>");
	
	colorTd.css({
		align:"center",
		'vertical-align':"middle",
		width:(this.legendSize+5)+"px",
	});
	colorDIV.css({
		width:this.legendSize+"px",
		height:this.legendSize+"px",
		border:"1px solid #000000",
		'background-color':"#"+color
	});
	textTd.css({
		align:"center",
		'vertical-align':"middle"
	});
	textTd.html(text);
	
	colorTd.append(colorDIV);
	tr.append(colorTd);
	tr.append(textTd);
	
	this.setLegendAreaPOS();
}

SepoaDhtmlXChart.prototype.setLegendAreaPOS = function(){
	this.legendArea.css({left : $(this.$view).width()/2 - this.legendArea.width()/2});
}

SepoaDhtmlXChart.prototype.clearLegendArea = function(){
	if(!this.legendArea){
		var chartDiv = this.$view;
		var chartDivClassName = "#"+chartDiv.id.trim();
		this.legendArea = $(chartDivClassName+" .dhx_chart_legend");
	}
	this.legendArea.empty();
	this.legendArea.hide();	
}

SepoaDhtmlXChart.prototype.clearLegendData = function(){
	this.legendData = new Array();	
}

SepoaDhtmlXChart.prototype.parseGridData = function(obj, x_name, y_name, tooltip_name, gridArray){
	if(typeof gridArray == 'undefined'){
		gridArray = new Array();
		for(var i=0;i<obj.getRowsNum(); i++){
			gridArray.push(i);
		}
	}
	
	var tooltipFlag = (typeof tooltip_name != 'undefined')?true:false;
		
	if(gridArray.length < 1) return;
	
	var tmpArray = new Array();
	
	var cnt = this.startIndex;
	for(var i=0; i<gridArray.length ;i++){
		var x_data = obj.cells(obj.getRowId(gridArray[i]), obj.getColIndexById(x_name)).getValue();
		var y_data = obj.cells(obj.getRowId(gridArray[i]), obj.getColIndexById(y_name)).getValue();
		var tooltip = (tooltipFlag)?obj.cells(obj.getRowId(gridArray[i]), obj.getColIndexById(tooltip_name)).getValue():"";
		
		if(x_data == null || x_data == '' || x_data > this.xAxis.end || x_data < this.xAxis.start){
			continue;
		}
		if(y_data == null || y_data == '' || y_data > this.yAxis.end || y_data < this.yAxis.start){
			continue;
		}
		
		tmpArray.push({id:cnt++, x:x_data, y:y_data, tooltip:tooltip});
	}
	
	this.clearAll();
	this.parse(tmpArray, "json");
	this.setAxisTitle(this.xAxisTitle, this.yAxisTitle);
	
	var chartDiv = this.$view;
	var chartDivClassName = "#"+chartDiv.id.trim();
	this.legendArea = $(chartDivClassName+" .dhx_chart_legend");
	this.legendArea.empty();
	for(var i=0;i<this.legendData.length;i++){
		this.addCustomLegend(this.legendData[i].text, this.legendData[i].color, "RE");
	}
	
	$(chartDivClassName+' canvas[canvas_id*="axis"]').attr("style","z-index:-1;");
}