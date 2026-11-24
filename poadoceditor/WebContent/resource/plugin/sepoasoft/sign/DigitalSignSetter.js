function SepoaDigitalSignSetter(vp, callback, flag, data){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.vp = vp;
	this.fontList = [];
	
	const fontCache = Sepoa.cache.combo.get(`SL0001,${Sepoa.info.HOUSE_CODE}#${Sepoa.info.LANGUAGE}#CM_010`);
	if(fontCache.length > 0) {
		for(let font of fontCache) {
			this.fontList.push(font.value);
		}
	}
	
	this.config = {
		vp : vp
		, skin : 'material'
		, preview_flag : false || flag
		, language : '/resource/plugin/sepoasoft/sign/lang/'+Sepoa.info.LANGUAGE+'.js'
	};
	this.base = {};
	this.component = {};
	this.callback = callback;
	this.data = data;
	this.init();
}

SepoaDigitalSignSetter.prototype.init = function() {
	Sepoa.cache.text.load(this.config.language, false);

	var font_list = this.fontList;
	var isFontLoaded = new Array(font_list.length);
	var tmpcanvas = document.createElement('canvas');
	var ctx = tmpcanvas.getContext('2d');
	var basicFontWidth = ctx.measureText('TEST').width;
	var initialFontWidth = new Array(font_list.length);
	var currentFontWidth = new Array(font_list.length);

	for(var i in font_list){
		ctx.fillText('', 1, 1);
		initialFontWidth[i] = ctx.measureText('TEST').width;
		isFontLoaded[i] = false;
	}

	function afterLoadedFonts(callback, attemptCount) {
		for(var i in font_list){
			ctx.font = '10px ' + font_list[i];	
			ctx.fillText('', 1, 1);
			currentFontWidth[i] = ctx.measureText('TEST').width;
		}
		for(var i in font_list) {
			if(basicFontWidth != initialFontWidth[i]) {
				isFontLoaded[i] = true;
			} else if (initialFontWidth[i] != currentFontWidth[i]) {
	        	isFontLoaded[i] = true;
	        }
		}		
	
		if(attemptCount === undefined) {
			attemptCount = 0;
		}else if (attemptCount >= 100) {
	          callback();	          
	          return;
		}

		if(isFontLoaded.every(function(e){return e == true})) {
			callback();
			return;
		} else {
			setTimeout(function () {afterLoadedFonts(callback, attemptCount + 1);}, 50);
		}
	};

	afterLoadedFonts(this.drawBase.bind(this));	
}

SepoaDigitalSignSetter.prototype.drawBase = function(){
	var signSetterWrapper = document.createElement('div');
	signSetterWrapper.id = 'signsetter_wrapper';
	signSetterWrapper.className = 'signsetter-wrapper';
	this.vp.appendChild(signSetterWrapper);
	
	var filterWrapper = document.createElement('div');
	filterWrapper.className = 'signsetter-filter-wrapper';
	signSetterWrapper.appendChild(filterWrapper);
	
	var filter = this.base.filter = document.createElement('div');
	filter.className = 'signsetter-filter';
	filterWrapper.appendChild(filter);
	
	var filterInput = this.base.filter.input = document.createElement('input');
	var initValue = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.SAMPLE_NAME');
	
	if(this._keywordValidation(Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_SIGN_ID'), Sepoa.info.NAME_LOC)) {
		initValue = Sepoa.info.NAME_LOC;
	}
	
	filterInput.id = 'signsetter_filter_input';
	filterInput.className = 'signsetter-filter-input';
	filterInput.type = 'text';
	filterInput.value = this.input = initValue;
	filterInput.onkeydown = function(event) {
		if(event.keyCode == 13) {
			this.onButtonClickSignRefresh();
			event.currentTarget.blur();
		}
	}.bind(this);
	filter.appendChild(filterInput);
	
	var filterButton = this.base.filter.button = document.createElement('div');
	filterButton.id = 'signsetter_filter_button';
	filterButton.className = 'signsetter-filter-button noselect';
	filterButton.innerText = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.BUTTON_FILTER');
	filterButton.onclick = this.onButtonClickSignRefresh.bind(this);
	filter.appendChild(filterButton);
	
	var tabbarWrapper = document.createElement('div');
	tabbarWrapper.className = 'signsetter-tabbar-wrapper';
	signSetterWrapper.appendChild(tabbarWrapper);
	
	var tabbar = this.base.tabbar = document.createElement('div');
	tabbar.className = 'signsetter-tabbar';
	tabbarWrapper.appendChild(tabbar);
	
	this.component.tabbar = new SepoaDhtmlXTabBar({
		parent:tabbar
		, mode:'top'
		, align:'left'
		, tabs:[
			  { id : Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_SIGN_ID'), text : Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_SIGN'), width : 150, index : 0, active : true, close : false }
			//, { id : Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_STAMP_ID'), text : Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_STAMP'), width : 150, index : 1, active : false, close : false }
			//, { id : Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_DRAW_SIGN_ID'), text : Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_DRAW_SIGN'), width : 150, index : 2, active : false, close : false }
			, { id : Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_ATTACH_SIGN_ID'), text : Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_ATTACH_SIGN'), width : 150, index : 3, active : false, close : false }
		]
	});
	
	this.component.tabbar.parentObj = this;
	
	this.drawSelectDigitalSign(this.component.tabbar.getActiveTab());

	this.component.tabbar.attachEvent('onTabChanged', function(id){
		var filterKeyword = this.parentObj.base.filter.children.item(0).value;
		
		this.parentObj._clearDigitalsign();
		
		switch(id){
			case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_SIGN_ID'):
				this.parentObj.drawSelectDigitalSign(id);
				break;
			case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_STAMP_ID'):
				this.parentObj.drawSelectDigitalStamp(id);
				break;
			case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_DRAW_SIGN_ID'):				
				this.parentObj.drawDrawingDigitalSign(id);
				break;
			case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_ATTACH_SIGN_ID'):				
				this.parentObj.drawAttachDigitalSign(id);
				break;
			default:
				break;
		}
	})
	
	var buttonWrapper = this.base.button = document.createElement('div');
	buttonWrapper.className = 'signsetter-button-wrapper noselect';
	signSetterWrapper.appendChild(buttonWrapper);
	
	var buttonClear = this.base.button.clear = document.createElement('div');
	buttonClear.className = 'signsetter-button';
	buttonClear.innerText = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.BUTTON_CLEAR');
	buttonClear.style.display = 'none';

	buttonWrapper.appendChild(buttonClear);
	
	var buttonSave = this.base.button.save = document.createElement('div');
	buttonSave.className = 'signsetter-button';
	buttonSave.innerText = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.BUTTON_SAVE');
	buttonSave.onclick = this.onButtonClickSave.bind(this);
	buttonWrapper.appendChild(buttonSave);
	
	var previewSignWrapper = this.base.previewSign = document.createElement('div');
	previewSignWrapper.id = 'preview_digitalsign';
	previewSignWrapper.className = 'preview-digitalsign';
	
	if(!this.config.preview_flag) {
		this.vp.appendChild(previewSignWrapper);
	} else {
		filter.appendChild(previewSignWrapper);	
	}
		
	
}

SepoaDigitalSignSetter.prototype.drawSelectDigitalSign = function(id) {

	if(!this.input) {
		return;
	}
	
	var tmp_font = this.fontList.filter(function(e){return e != 'HJ_TYPE_B'});
	
	var digitalSignContainer = document.createElement('div');
	digitalSignContainer.className = 'noselect digitalsign-container';

	var sampleListWrapper = document.createElement('div');
	sampleListWrapper.className = 'sample-list-wrapper';
	
	digitalSignContainer.appendChild(sampleListWrapper);	

	for(var i = 0; i < tmp_font.length; i++) {
		var sampleUnitWrapper = document.createElement('div');
		sampleUnitWrapper.className = 'sample-unit-wrapper';
		sampleListWrapper.appendChild(sampleUnitWrapper);
		
		var sampleUnit = document.createElement('div');
		sampleUnit.className = 'sample-unit';
		sampleUnitWrapper.appendChild(sampleUnit);
		
		var sampleText = document.createElement('div');
		sampleText.className = 'sample-text';		
		sampleUnit.appendChild(sampleText);
		
		var sampleContents = document.createElement('div');
		sampleContents.className = 'sample-contents';
		sampleContents.config = {type:true, font : tmp_font[i]};
		sampleContents.onclick = this.onButtonClickContents.bind(this, sampleContents);
		this._drawContents(sampleContents, this.input, {type:true, font : tmp_font[i], fontSize : '30px'});
		sampleText.appendChild(sampleContents);
	}
	
	this.component.tabbar.tabs(id).attachObject(digitalSignContainer);
}

SepoaDigitalSignSetter.prototype.drawSelectDigitalStamp = function(id) {
	
	if(!this.input) {
		return;
	}
	
	var stampText = this.input
	
	var tmp_Arr = [];
	
	if(stampText.length <= 3) {
		tmp_Arr.push({shape : 'ellipse', font : '굴림체', fontStyle : 'bold', verticalFlag : true});
		tmp_Arr.push({shape : 'ellipse', font : '굴림체', fontStyle : 'bold'});
		tmp_Arr.push({shape : 'ellipse', font : '궁서체', fontStyle : 'bold', verticalFlag : true});
		tmp_Arr.push({shape : 'ellipse', font : '궁서체', fontStyle : 'bold'});
		tmp_Arr.push({shape : 'rect', font : 'HJ_TYPE_B', fontStyle : 'bold', verticalFlag : true});
	} else {
		tmp_Arr.push({shape : 'ellipse', font : '굴림체', fontStyle : 'bold'});
		tmp_Arr.push({shape : 'rect', font : '굴림체', fontStyle : 'bold'});
		tmp_Arr.push({shape : 'ellipse', font : '궁서체', fontStyle : 'bold'});
		tmp_Arr.push({shape : 'rect', font : '궁서체', fontStyle : 'bold'});
		tmp_Arr.push({shape : 'reverse_rect', font : 'HJ_TYPE_B', fontStyle : 'bold'});
	}

	tmp_Arr.push({shape : 'ellipse', font : 'HJ_TYPE_B', fontStyle : 'bold'});
	tmp_Arr.push({shape : 'rect', font : 'HJ_TYPE_B', fontStyle : 'bold'});
	tmp_Arr.push({shape : 'double_rect', font : 'HJ_TYPE_B', fontStyle : 'bold'});

	var digitalStampContainer = document.createElement('div');
	digitalStampContainer.className = 'digitalstamp-container';
	
	var sampleListWrapper = document.createElement('div');
	sampleListWrapper.className = 'sample-list-wrapper';
	
	digitalStampContainer.appendChild(sampleListWrapper);	

	for(var i = 0; i < tmp_Arr.length; i++) {
		var sampleUnitWrapper = document.createElement('div');
		sampleUnitWrapper.className = 'sample-unit-wrapper';
		sampleListWrapper.appendChild(sampleUnitWrapper);
		
		var sampleUnit = document.createElement('div');
		sampleUnit.className = 'sample-unit';
		sampleUnitWrapper.appendChild(sampleUnit);
		
		var sampleText = document.createElement('div');
		sampleText.className = 'sample-text';		
		sampleUnit.appendChild(sampleText);
		
		var sampleContents = document.createElement('div');
		sampleContents.className = 'sample-contents';
		sampleContents.onclick = this.onButtonClickContents.bind(this, sampleContents);
		sampleContents.config = tmp_Arr[i];
		this._drawContents(sampleContents, stampText, tmp_Arr[i]);		
		sampleText.appendChild(sampleContents);
	}

	this.component.tabbar.tabs(id).attachObject(digitalStampContainer);
	
}

SepoaDigitalSignSetter.prototype.drawDrawingDigitalSign = function(id) {
	
	var drawingDigitalSignContainer = document.createElement('div');
	drawingDigitalSignContainer.className = 'drawing-digitalsign-container';
	
	var drawingToolWrapper = document.createElement('div');
	drawingToolWrapper.className = 'drawing-tool-wrapper';
	drawingDigitalSignContainer.appendChild(drawingToolWrapper);
	
	var drawingToolLineThickness = document.createElement('div');
	drawingToolLineThickness.className = 'drawing-tool-line-thickness';
	drawingToolWrapper.appendChild(drawingToolLineThickness);
	
	var sampleSVG = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
	sampleSVG.setAttributeNS(null, 'width', 30);
	sampleSVG.setAttributeNS(null, 'height', 30);
	drawingToolLineThickness.appendChild(sampleSVG);
	
	var sampleCircle = this.base.Circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
	sampleCircle.setAttributeNS(null, 'cx', 15);
	sampleCircle.setAttributeNS(null, 'cy', 15);
	sampleCircle.setAttributeNS(null, 'r', 1);
	sampleCircle.setAttributeNS(null, 'stroke', 'black');
	sampleCircle.setAttributeNS(null, 'strokeWidth', '1');
	sampleCircle.setAttributeNS(null, 'fill', 'black');
	sampleSVG.appendChild(sampleCircle);
	
	var drawingLineThick = this.base.canvasLine = document.createElement('input');
	drawingLineThick.type = 'range';
	drawingLineThick.value = 3;
	drawingLineThick.min = 1;
	drawingLineThick.max = 10;
	drawingLineThick.step = 0.1;
	drawingLineThick.oninput = function(){
		this.base.Circle.setAttributeNS(null, 'r', this.base.canvasLine.value);
	}.bind(this);
	drawingLineThick.onchange = function(){
		this.base.Circle.setAttributeNS(null, 'r', this.base.canvasLine.value);
	}.bind(this);
	drawingToolLineThickness.appendChild(drawingLineThick);		

	var drawingCanvasWrapper = document.createElement('div');
	drawingCanvasWrapper.className = 'drawing-canvas-wrapper';
	drawingDigitalSignContainer.appendChild(drawingCanvasWrapper);
	
	var drawingCanvas = this.base.canvas = document.createElement('div');
	drawingCanvas.className = 'drawing-canvas';
	this._drawDrawingCanvas(drawingCanvas);	
	drawingCanvasWrapper.appendChild(drawingCanvas);
	
	this.base.button.clear.onclick = function(){
		this._clearDigitalsign();
		this.drawDrawingDigitalSign(this.component.tabbar.getActiveTab());	
	}.bind(this);
	this.base.button.clear.style.display = '';
	
	this.component.tabbar.tabs(id).attachObject(drawingDigitalSignContainer);	
}

SepoaDigitalSignSetter.prototype.drawAttachDigitalSign = function(id) {
	
	var attachDigitalSignContainer = document.createElement('div');
	attachDigitalSignContainer.className = 'attach-digitalsign-container';
	
	var attachDigitalSignWrapper = document.createElement('div');
	attachDigitalSignWrapper.className = 'attach-digitalsign-wrapper';
	attachDigitalSignContainer.appendChild(attachDigitalSignWrapper);
	
	var attachDigitalSignImage = this.base.attach = document.createElement('div');
	attachDigitalSignImage.className = 'attach-digitalsign-image ';

	var imageUrl = '/resource/design/imgs/common/FILE_ADD_IMG.png';
	if(typeof(this.data) != 'undefined'){
		var so = doServiceAjax('sepoa.svc.contract.SepoaContractDigitalSign', 'CONNECTION', 'doQueryCorpSign', this.data, null, false);
		if(so.status == 1) {
			var result = new SepoaFormater(so.result[0]).getArrayMap()[0];
			if(result) {
				imageUrl = '/SepoaImage.sux?mode=CUSTOM&METHOD=doImage&PROCESS_ID=sepoa.svc.contract.SepoaContractDigitalSign&signId=YDH_1&ROOT_PATH_TYPE='
					+ result.ROOT_PATH_TYPE +'&DES_FILE_NAME=' + result.DES_FILE_NAME + '&SRC_FILE_NAME' + result.SRC_FILE_NAME + '&SIGN_SEQ=' + result.SIGN_SEQ;
			}
		}
	}

	attachDigitalSignImage.style.background = 'url(' + imageUrl + ') center center / 200px 200px no-repeat';
	attachDigitalSignWrapper.appendChild(attachDigitalSignImage);
	
	attachDigitalSignImage.onclick = function() {
		var tmpFileInput = document.createElement('input');
		tmpFileInput.type = 'file';
		tmpFileInput.id = 'upload_file';
		tmpFileInput.name = 'upload_file';
		tmpFileInput.accept = '.png, .jpg, .jpeg';
		tmpFileInput.required = 'required';
		
		tmpFileInput.onchange = function() {
			tmpFileInput.files[0].arrayBuffer().then(function(buf) {
				var data = new Uint8Array(buf);
				var dataStr = data.reduce(function(prev, cur) {
					return prev + String.fromCharCode(cur);
				}, '');
				var base64 = 'data:' + tmpFileInput.files[0].type + ';base64,' + btoa(dataStr);
				attachDigitalSignImage.style.backgroundImage = 'url(' + base64 + ')';
				this.outputData = base64;
			}.bind(this));
		}.bind(this);
		
		tmpFileInput.click();
	}.bind(this);
	
	//파일 드롭 컨테이너 드래그 이벤트 부여
	attachDigitalSignImage.ondragenter = function(e) {
		attachDigitalSignImage.style.backgroundColor = '#B2B2B2';
		e.stopPropagation();
		e.preventDefault();
	}
	
	attachDigitalSignImage.ondragover = function(e){
		e.stopPropagation();
		e.preventDefault();
	}
	
	attachDigitalSignImage.ondragleave = function(e){
		attachDigitalSignImage.style.backgroundColor = '#FFFFFF';
		e.stopPropagation();
		e.preventDefault();
	}
	
	attachDigitalSignImage.ondrop = function(e){		
		attachDigitalSignImage.style.backgroundColor = '#FFFFFF';
		fileUpload(e.dataTransfer.files);
		e.preventDefault();
	}
	
	attachDigitalSignImage.drop = null;

	this.base.button.clear.onclick = function(){
		this._clearDigitalsign();
		this.drawAttachDigitalSign(this.component.tabbar.getActiveTab());	
	}.bind(this);
	this.base.button.clear.style.display = '';
	this.component.tabbar.tabs(id).attachObject(attachDigitalSignContainer);	
}

SepoaDigitalSignSetter.prototype.onButtonClickSignRefresh = function() {
	var filterKeyword = this.base.filter.children.item(0).value;
	var activeTabId = this.component.tabbar.getActiveTab();
	this.input = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.SAMPLE_NAME');
	
	switch(activeTabId) {
		case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_SIGN_ID'):
			if(!this._keywordValidation(activeTabId, filterKeyword)) {
				Alert(Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_SIGN_VALIDATION_WARNING'));
				return;
			}
			this.input = filterKeyword;
			this.drawSelectDigitalSign(activeTabId);
			break;
		case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_STAMP_ID'):
			if(!this._keywordValidation(activeTabId, filterKeyword)) {
				Alert(Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_STAMP_VALIDATION_WARNING'));
				return;
			}
			this.input = filterKeyword;
			this.drawSelectDigitalStamp(activeTabId);
			break;
		default:
			break;
	}

}

SepoaDigitalSignSetter.prototype.onButtonClickContents = function(obj){
	var selectedSign = obj.cloneNode();	
	
	obj.config.width = 200;
	obj.config.height = 200;
	obj.config.fontSize = obj.config.type ? '70px' : '40px';	
	this._drawContents(selectedSign, this.input, obj.config);
	this.outputData = selectedSign.children.item(0).toDataURL();
	
	var target = this.base.previewSign;
	
	if(target.childElementCount >= 1) {
		target.removeChild(target.children[0]);
	}
	
	target.appendChild(selectedSign);
	$('.' + obj.className).css('border', '');
	obj.style.border = '1px solid #fbcb72';
};

SepoaDigitalSignSetter.prototype.onButtonClickSave = function(){
	if(this.outputData == null){
		Alert(Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_SIGN_NONE_SELECTED_WARNING'));

		return;
	}
	
	var activeTabId = this.component.tabbar.getActiveTab();
	var pool = {};
	pool.params = {};
	pool.params.IMG_DATA = this.outputData;
	pool.method = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.METHOD_COMMON_SAVE');
	pool.failLimit = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_SAVE_FAIL_LIMIT');
	pool.fail = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_SAVE_FAIL') + Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_SAVE_FAIL_DESCRIPTION');
	
	switch(activeTabId) {
		case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_SIGN_ID'):
			pool.params.SIGN_TYPE = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TYPE_DIGITAL_SIGN');
			pool.confirmText = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_SIGN_ADD_CONFIRM');
			pool.success = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_SIGN_SAVE_SUCCESS');
			break;
		case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_STAMP_ID'):
			pool.params.SIGN_TYPE = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TYPE_STAMP');
			pool.confirmText = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_STAMP_ADD_CONFIRM');
			pool.success = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_STAMP_SAVE_SUCCESS');
			break;
		case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_DRAW_SIGN_ID'):
			pool.params.SIGN_TYPE = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TYPE_DRAW_SIGN');
			pool.confirmText = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_DRAW_ADD_CONFIRM');
			pool.success = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_SIGN_SAVE_SUCCESS');
			break;
		case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_ATTACH_SIGN_ID'):
			pool.method = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.METHOD_ATTACH_SIGN_SAVE');
			pool.params.SIGN_TYPE = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TYPE_ATTACH_SIGN');
			pool.confirmText = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_DRAW_ADD_CONFIRM');
			pool.success = Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_SIGN_SAVE_SUCCESS');
			break;
		default:
			break;
	}	
	
	Confirm(pool.confirmText, function(flag) {
		if(flag == true){
			var parameter = $.extend({}, this.data, pool.params);
			var so = doServiceAjax('sepoa.svc.contract.SepoaContractDigitalSign', 'TRANSACTION', pool.method, parameter, null, false);
			
			if(so.status == '1') {
				Alert(pool.success, function() {
					this.callback();
					selfObj.close();
				}.bind(this));
			} else if(so.status == '-1'){
				Alert(pool.failLimit, function(){
				});
			} else {
				Alert(pool.fail);
				return;
			}
		}
	}.bind(this));

}

SepoaDigitalSignSetter.prototype._setDigitalStampArray = function(target, flag) {	//flag = verticalFlag
	var arr;	

	if(target.length == 1) {
		arr = new Array(1);
	} else if(target.length / 3 == 3) {
		arr = new Array(3);
	} else if(target.length / 2 >= 1) {
		arr = new Array(2);
		
		if(target.length % 2 != 0 && !flag) {
			target += Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.STAMP_APPEND_STRING');
		}		
	} 
	
	var targetArr = target.split('');
	var rowlength = targetArr.length / arr.length;
	
	if(flag) {
		arr = new Array(target.length);
		rowlength = 1;
	}

	for(let i = 0; i < arr.length; i++) {
		arr[i] = new Array(rowlength);
	}
	
	for(let i = 0; i < targetArr.length; i++) {
		arr[parseInt(i / rowlength)][i % rowlength] = targetArr[i];		
	}
	
	return arr;
}

SepoaDigitalSignSetter.prototype._drawEllipse = function(ctx, cx, cy, width, height){
    var PI2 = Math.PI * 2;
    var ratio = height / width;
    var radius = Math.max(width, height) / 2;
    var increment = 1 / radius;

    ctx.beginPath();
    var x = cx + radius * Math.cos(0);
    var y = cy - ratio * radius * Math.sin(0);
    ctx.lineTo(x,y);

    for(var radians = increment; radians < PI2; radians += increment){ 
        var x = cx + radius * Math.cos(radians);
        var y = cy - ratio * radius * Math.sin(radians);
        ctx.lineTo(x,y);
     }

    ctx.closePath();
    ctx.stroke();
}

SepoaDigitalSignSetter.prototype._drawContents = function(vp, text, config) {

	var textArr = this._setDigitalStampArray(text, config.verticalFlag);
	var font = config.font || '궁서체';	
	var fontStyle = config.fontStyle || '';
	var fontSize = config.fontSize || '20px';
	var stampMargin = 5;
	
	var canvas = document.createElement('canvas');
	canvas.width = config.width || 100;
	canvas.height = config.height || 100;
	
	var ctx = canvas.getContext('2d');
	ctx.fillStyle = 'Red';
	ctx.strokeStyle = 'Red';
	ctx.font = fontStyle + ' ' + fontSize + ' ' + font;
	ctx.textAlign = 'center';
	ctx.textBaseline = 'middle';
	ctx.lineWidth = '3';
	
	if(config.type) {
		ctx.fillStyle = 'Black';

		while(ctx.measureText(text).width > canvas.width) {			
			fontSize = fontSize.replace(/[^\d]/g, '') - 1 + 'px';
			ctx.font = fontStyle + ' ' + fontSize + ' ' + font;
		}

		ctx.fillText(text, canvas.width/2, canvas.height/2);
		vp.appendChild(canvas);
		
		return;
	}
	
	var startWidth = 0;
	
	if(textArr[0].length % 2 == 0) {
		startWidth -= ctx.measureText(textArr[0][0]).width / 2;
	}
	
	for(var a = 1; a < textArr[0].length / 2; a++) {
		startWidth -= ctx.measureText(textArr[0][0]).width;
	}
	
	var startHeight = -ctx.measureText(textArr[0][0]).width / 2;

	if(textArr.length % 2 != 0) {
		startHeight = startHeight / 4;
	}
	
	for(var a = 1; a < textArr.length / 2; a++) {
		startHeight -= ctx.measureText(textArr[0][0]).width;		
	}
	
	if(config.shape.toUpperCase() == 'RECT') {	
		ctx.strokeRect(
				  canvas.width/2 + startWidth - ctx.measureText(textArr[0][0]).width / 2 - stampMargin
				, canvas.height/2 + startHeight - ctx.measureText(textArr[0][0]).width / 2 - stampMargin
				, ctx.measureText(textArr[0][0]).width * textArr[0].length + stampMargin * 2
				, ctx.measureText(textArr[0][0]).width * textArr.length + stampMargin * 2
		);		
	} else if(config.shape.toUpperCase() == 'DOUBLE_RECT') {
		ctx.lineWidth = '4';
		ctx.strokeRect(
				  canvas.width/2 + startWidth - ctx.measureText(textArr[0][0]).width / 2 - (stampMargin + 5)
				, canvas.height/2 + startHeight - ctx.measureText(textArr[0][0]).width / 2 - (stampMargin + 5)
				, ctx.measureText(textArr[0][0]).width * textArr[0].length + (stampMargin + 5) * 2
				, ctx.measureText(textArr[0][0]).width * textArr.length + (stampMargin + 5) * 2
		);
		ctx.lineWidth = '3';
		ctx.strokeRect(
				  canvas.width/2 + startWidth - ctx.measureText(textArr[0][0]).width / 2 - stampMargin + 0.1
				, canvas.height/2 + startHeight - ctx.measureText(textArr[0][0]).width / 2 - stampMargin + 0.1
				, ctx.measureText(textArr[0][0]).width * textArr[0].length + stampMargin * 2
				, ctx.measureText(textArr[0][0]).width * textArr.length + stampMargin * 2
		);		
	} else if(config.shape.toUpperCase() == 'REVERSE_RECT') {		
		ctx.fillRect(
				  canvas.width/2 + startWidth - ctx.measureText(textArr[0][0]).width / 2 - stampMargin
				, canvas.height/2 + startHeight - ctx.measureText(textArr[0][0]).width / 2 - stampMargin
				, ctx.measureText(textArr[0][0]).width * textArr[0].length + stampMargin * 2
				, ctx.measureText(textArr[0][0]).width * textArr.length + stampMargin * 2
		);
		ctx.fillStyle = 'rgb(255, 255, 255)';
	} else if(config.shape.toUpperCase() == 'ELLIPSE') {
		var marginWidth = 0;
		var marginHeight = 0;
		
		if(text.length == 1) {
			marginWidth = stampMargin * 2;
			marginHeight = stampMargin * 3;
		} else if(text.length == 2) {
			marginWidth = stampMargin * 2;
		} else if(text.length == 7 || text.length == 8) {
			marginWidth = stampMargin * 3;
			marginHeight = stampMargin * 5;
		} else {
			marginWidth = stampMargin * 5;
			marginHeight = stampMargin * 4;
		}
		
		if(config.verticalFlag) {
			this._drawEllipse(
					  ctx
					, canvas.width/2
					, canvas.height/2
					, ctx.measureText(textArr[0][0]).width
					, ctx.measureText(textArr[0][0]).width * 2
			)
		} else {
			this._drawEllipse(
					  ctx
					, canvas.width/2
					, canvas.height/2
					, ctx.measureText(textArr[0][0]).width * textArr[0].length + marginWidth
					, ctx.measureText(textArr[0][0]).width * textArr.length + marginHeight
			)
		}
		
	}
	
	var addWidth = 0;
	var addHeight = 0;
	for(var a = 0; a < textArr.length; a++) {			
		for(let b = 0; b < textArr[a].length; b++) {
			ctx.fillText(textArr[a][b], canvas.width/2 + startWidth + addWidth, canvas.height/2 + startHeight + addHeight);
			addWidth += ctx.measureText(textArr[0][0]).width;
		}
		addWidth = 0;
		addHeight += ctx.measureText(textArr[0][0]).width;		
	}
	
	vp.appendChild(canvas);
}

SepoaDigitalSignSetter.prototype._drawDrawingCanvas = function(vp) {
	
	var canvas = document.createElement('canvas');
	canvas.style.border = 'solid 1px';
	
	vp.appendChild(canvas);
	
	var ctx = canvas.getContext('2d');
	var pos = {
			  init : false
		    , drawable: false
		    , x: 0
		    , y: 0
		};

	canvas.width = 300;
	canvas.height = 200;
    ctx.lineWidth = this.base.canvasLine.value;
	
    var drawStart = function(event) {
    	if(!pos.init) {
    		pos.init = true;    		
    		removePlaceHolder(canvas, ctx);    		
    	}    	
    	if(!pos.drawable) {    		
    		pos.drawable = true;
    		setPosition(event);
    		ctx.lineWidth = this.base.canvasLine.value;
    	    ctx.beginPath();
    	    ctx.moveTo(pos.x, pos.y);
    	}
    }.bind(this);
    
    var draw = function(event) {
    	if(pos.drawable) {
    		setPosition(event);
    	    ctx.lineTo(pos.x, pos.y);
    	    ctx.stroke();
    	}
    }
    
    var drawEnd = function(event) {
		pos.drawable = false;
	    pos.X = 0;
	    pos.Y = 0;
    }
    
    var afterDraw = function(event) {
    	if(pos.init) {
    		this.outputData = canvas.toDataURL();	
    	}    	
    }.bind(this);
    
    var setPosition = function(event) {
    	pos.x = event.offsetX;
	    pos.y = event.offsetY;
	    
	    if(event.touches) {
	    	var rect = event.target.getBoundingClientRect();
	    	
	    	pos.x = event.touches[0].clientX - rect.left;
		    pos.y = event.touches[0].clientY - rect.top;
	    }
    }
    
	var createPlaceHolder = function(canvas, ctx) {
		ctx.font = '30px Comic Sans MS';
	    ctx.fillStyle = 'grey';
		ctx.textAlign = 'center';
	    ctx.fillText(Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.MESSAGE_CANVAS_PLACEHOLDER'), canvas.width/2, canvas.height/2);
	}
	
	var removePlaceHolder = function(canvas, ctx) {
		ctx.clearRect(0, 0, canvas.width, canvas.height);
	}
	
	createPlaceHolder(canvas, ctx);	
    canvas.addEventListener('mousedown', drawStart);
    canvas.addEventListener('mouseup', drawEnd);
    canvas.addEventListener('mousemove', draw);
    canvas.addEventListener('mouseout', drawEnd);
    canvas.addEventListener('mouseout', afterDraw);
	canvas.addEventListener('touchstart', drawStart);
    canvas.addEventListener('touchend', drawEnd);
    canvas.addEventListener('touchmove', draw);
    canvas.addEventListener('touchend', afterDraw);
    canvas.ondragstart = function(){return false};
}

SepoaDigitalSignSetter.prototype._clearDigitalsign = function() {
	var target = this.base.previewSign;
	
	if(target.childElementCount >= 1) {
		target.removeChild(target.children[0]);
	}	
	
	this.base.button.clear.style.display = 'none';
	delete this.outputData;
}

SepoaDigitalSignSetter.prototype._keywordValidation = function(activeTabId, filterKeyword) {
	var regex;
	
	switch(activeTabId) {
		case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_SIGN_ID'):
			regex = /^[가-힣A-Za-z ]{1,11}$/;
			break;
		case Sepoa.cache.text.get('DIGITAL_SIGN_SETTER.TABBAR_DIGITAL_STAMP_ID'):
			regex = /^[一-龥가-힣]{1,9}$/;
			break;
		default:
			break;
	}

	return regex.test(filterKeyword);
}

SepoaDigitalSignSetter.prototype._TextData = function(target) {
	const arg = target.split('.');
	
	return this.lang[arg[0]][arg[1]]
}

export { SepoaDigitalSignSetter }
