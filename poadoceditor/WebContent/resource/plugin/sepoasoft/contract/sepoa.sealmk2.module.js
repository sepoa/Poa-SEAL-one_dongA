/**
 * PDF 방식으로 라이브러리 교체하여 구매의 계약시스템과 분리하기위해 파일 생성됨
 */
function SepoaContractObject(conf){
	sUtils.modules.extend(this, "EventManager")
	this.conf = $.extend(true, {}, conf);
	this.conf.header = conf.header;
	this.component = {};
}
SepoaContractObject.prototype.draw=function(){
	this._renderEditor();
	if(this.conf.module.items){
		this._renderItems();
	}
	this._renderPreview();
	if(this.conf.header){
		this._renderHeader();
	}
	
	SUX.callEvent('onWindowResize',[]);
}
SepoaContractObject.prototype.setWatermark=function(s){
	this.editor.setWatermark(s);
}
SepoaContractObject.prototype.setMode=function(mode){
	this.editor.setMode(mode);
}
SepoaContractObject.prototype.setSignFilter=function(signerNo){
	this._signerNo = signerNo;
	this.editor.setSignFilter(signerNo);
}
SepoaContractObject.prototype._renderHeader=function(){
	var _html={
        float:'<div class="contract-editor-float-wrapper"></div>',
        header:'<div class="contract-editor-header-wrapper"></div>'
    }
    
	this._header = {
		_area:{},
		list:[]
	}
	
	for(var i in this.conf.header){
        var options = this.conf.header[i];
        
        var position = options.position || 'header';
        if(!this._header._area[position]){
        	var p = (position == 'header')?this.editor.base:(this.getContainer('editor'));
            this._header._area[position] = p.insertBefore($(_html[position])[0], p.firstChild); 
        }
        
        var itemBox = this._header._area[position].appendChild($('<div class="contract-editor-header-item contract-editor-header-item-'+options.type+' sepoaux_component_join sepoaux_screen_preset_default"></div>')[0]);
        if(options.type == "form"){
            var formObj = new SepoaMultiDhtmlXFormObject(itemBox, 'formObj', [sUtils.cache.form.get(options.id)], true);
            formObj.enableLiveValidation(true);
            formObj.attachEvent('onChange', function(k, v) {
                var targetList = $(this.editor.editorWrapper.children[1]).find('.contract-item-box-code-' + k);
                for(var i = 0; i< targetList.length;i++){
                    var targetObj = targetList[i].obj;
                    targetObj.editor.setProperties({value: v}, true);
                }
            }.bind(this));
            this.attachEvent("onItemChange", function(){
                console.log(arguments);
            })
            options.obj = formObj;
            this._header.list.push(formObj);
            this.attachEvent('onDataLoaded', function(data){
            	if(data.data){
            		this.setData(data.data, "json");
            	}
            }.bind(formObj))
        }else if(options.type == "flowview"){
            var flowViewObj = Object.create(SepoaFlowView.prototype);
            SepoaFlowView.apply(flowViewObj, [itemBox].concat(options.options));
            flowViewObj._originObj = options.origin; 
            flowViewObj.setActive('MODULE');
            flowViewObj.attachEvent('onBeforeActive', function(id, before, btn){
            	this._originObj.setActive(id, false, btn);
                return false;
            });
            options.obj = flowViewObj;
            this._header.list.push(flowViewObj);
            
            if(options.events){
            	for(var evid in options.events){
            		flowViewObj.attachEvent(evid, options.events[evid]);
            	}
            }
        }else if(options.type == "button"){
            var btnObj = new SepoaButton();
            for(var j in options.list){
            	if(!options.list[j].onclick){
            		options.list[j].onclick = function(){
            			this.obj.callEvent('onHeaderButtonClick', [this.options.id]);
            		}.bind({obj:this, options:options.list[j]});
            	}
                btnObj.addButton(itemBox, options.list[j]);
            }
            options.obj = btnObj;
            this._header.list.push(btnObj);
        }
    }
}

SepoaContractObject.prototype.setImageServlet=function(svl){
	for(var i in this.preview){
		this.preview[i].setImageServlet(svl);
	}
	this.editor.setImageServlet(svl);
}
SepoaContractObject.prototype.load=function(array, type, watermark){
	this.clear();
	
	if(array && array.filter(e => e.DOC_SEQ).length == array.length) {
		array = array.sort((a, b) => a.DOC_SEQ - b.DOC_SEQ);
	}
	
	this.editor.load(array, type, watermark);
	if(this.conf.workType == 'T') {
		//this.editor.drawAddPageButton();
	}
	
	for(var i in this.preview){
		this.preview[i].load(array, type);
	}
}
SepoaContractObject.prototype.setData=function(data){
	this.editor.setData(data);
	if(this.items && this.items.sign){
		this.items.sign.setData(data.signer);
	}
	this.callEvent('onDataLoaded', [data])
}
SepoaContractObject.prototype.setDefaultSigner=function(cnt){
	this.items.sign.setDefaultCount(cnt);
}
SepoaContractObject.prototype.clear=function(){
	if(this.editor) {
		this.editor.clear();
	}

	if(this.items){
		if(this.items.sign){
			this.items.sign.clear();
		}
	}

	if(this.preview){
		for(var i in this.preview){
			this.preview[i].clear();
		}
	}
}
SepoaContractObject.prototype.getHtml=function(flag){
	return this.editor.getHtml(flag);
}
SepoaContractObject.prototype._renderItems=function(){
	var itemsCont = this._createLayoutDiv('items')
	if(this.conf.module.items){
		this.items = {};
		if(this.conf.module.items.item){
			this.items.item = new SepoaContractItemList(this.conf.module.items.item, itemsCont);
			this.items.item.parentObj = this;
			this.items.item.attachEvent('onItemClick', function(e, o){
				this.parentObj.showHelper(e, o);
			});
		}
		
		if(this.conf.module.items.extraItem){
			this.items.extraItem = new SepoaContractItemList(this.conf.module.items.extraItem, itemsCont);
			this.items.extraItem.parentObj = this;
			this.items.extraItem.attachEvent('onItemClick', function(e, o){
				this.parentObj.showHelper(e, o);
			})
		}
		
		if(this.conf.module.items.sign){
			this.items.sign = new SepoaContractSignList(this.conf.module.items.sign, itemsCont, this);
			this.items.sign.attachEvent('onItemClick', function(e, o){
				this.parentObj.showHelper(e, o);
			})
			this.items.sign.attachEvent('onSignerAdd', function(id, color){
				if(!this.parentObj._signerCss){
					this.parentObj._signerCss={};
				}
				this.parentObj._signerCss[id] = this.parentObj.getContainer('editor').appendChild($('<style></style>')[0]); 
				this.parentObj._signerCss[id].innerText='.contract-editor-area .contract-item-box-sign-id-'+id+'{background-color:hsla('+color+', 100%, 80%, 0.8);}\n.contract-item-box-sign-id-'+id+' .contract-item-se-resize{border-bottom-color:hsl('+(color-15)+', 100%, 80%);}';
			})
			this.items.sign.attachEvent('onSignerRemove', function(id){
				this.parentObj._signerCss[id].parentElement.removeChild(this.parentObj._signerCss[id]);
				delete this.parentObj._signerCss[id]
				
				this.data.list = this.data.list.filter(e => e.ID != id)
				this.data.nowCount = this.data.list.length;
				this.controlBox.children[0].children[1].innerHTML = this.data.nowCount;
				this.signItems[id].remove();
				delete this.signItems[id];
			})
			this.items.sign.drawDefault();
		}
	}
}
SepoaContractObject.prototype._renderPreview=function(){
	if(!this.preview){
		this.preview={};
	}
	for(var i=0; i<this.conf.module.preview.length; i++){
		var previewElement = this.conf.module.preview[i];
		this.preview[previewElement.layout] = new SepoaPreview($.extend(true, previewElement.config, {cell:previewElement.layout}), this._createLayoutDiv('preview', i));
		this.preview[previewElement.layout].parentObj = this;
		this.preview[previewElement.layout].attachEvent('onItemClick', function(e, id){
			this.parentObj.editor.showItem(id)
		})
		//coding events
	}
}
SepoaContractObject.prototype._renderEditor=function(){
	var cont = this._createLayoutDiv('editor');
	this.editor = new SepoaContractEditor(this.conf.module.editor.config, cont);
	this.callEvent("onCreateObject", ['editor', this.editor]);
	this.editor.parentObj = this;
	this.editor.attachEvent('onItemClick', function(e, id){
		if(this.parentObj.helper && this.parentObj.helper.status){
			var adj = {x:0, y:0};
			if(!e.srcElement.classList.contains('contract-editor-list-item')){
				adj.x = e.srcElement.parentElement.offsetLeft;
				adj.y = e.srcElement.parentElement.offsetTop;
			}
			
			// [CHECKBOX 기능 추가] 체크박스는 helper의 실제 위치를 사용하여 정확한 위치 계산
			var pos = {x:(e.offsetX+1+adj.x), y:(e.offsetY+1+adj.y)};
			if(this.parentObj.helper._data && (this.parentObj.helper._data.ITEM_TYPE == 'CHECKBOX' || this.parentObj.helper._data.ITEM_TYPE == 'MCHECKBOX' || this.parentObj.helper._data.ITEM_TYPE == 'NCHECKBOX')){
				// helper의 실제 위치를 가져와서 editor layer 기준으로 변환
				var helperRect = this.parentObj.helper.getBoundingClientRect();
				var layerRect = this._rowsAr[id].editorLayer.getBoundingClientRect();
				pos.x = (helperRect.left - layerRect.left) + (helperRect.width / 2) - 8; // 체크박스 크기의 절반(8px) 빼기
				pos.y = (helperRect.top - layerRect.top) + (helperRect.height / 2) - 8;
			}
			
			this.addContractItem(id, this.parentObj.helper._data, this.parentObj.helper, pos);
			this.parentObj.hideHelper();
		}
	});
	this.editor.attachEvent('onXLE', function(data){
		if(this.items && this.items.sign){
			if(data.SIGN_ID_LIST.length > 0){
				this.items.sign.removeAll();
				for(var i in data.SIGN_ID_LIST){
					this.items.sign.addBlankSigner(data.SIGN_ID_LIST[i]);
				}
			}
		}
	}.bind(this))
	//coding events
}
SepoaContractObject.prototype._getLayoutCell=function(type, index){
	var lc = this.conf.layout;
	if(typeof index === 'number'){
		if(this.conf.module[type][index]){
			return lc.cells(this.conf.module[type][index].layout)
		}
	} else {
		if(this.conf.module[type]){
			return lc.cells(this.conf.module[type].layout)
		}
	}
	return null;
}
SepoaContractObject.prototype.getContainer=function(type){
	var cells = this._getLayoutCell(type);
	if(cells != null){
		return $(cells.cell).find('.contract-'+type+'-base')[0];
	}
	return null;
}
SepoaContractObject.prototype._createLayoutDiv=function(type, index){
	var cells = this._getLayoutCell(type, index);
	var base = null;
	if(cells != null){
		base = cells.attachDiv().appendChild($('<div class="contract-noselect"></div')[0]);
	}else{
		base = $('<div class="contract-noselect"></div')[0];
	}
	return base.appendChild($('<div class="contract-'+type+'-base"></div>')[0]);
}
SepoaContractObject.prototype.showHelper=function(e, o){
	if(!this.helper){
		var that = this;
		
		var vp = this.conf.layout.cont;
		vp.classList.add('contract-helper-view');
		vp.classList.add('contract-helper-hidden');
		vp.onmouseleave=function(ev) {
			that.hideHelper(ev);
		}
		
		this.helper = vp.appendChild($('<textarea class="contract-helper-label"></textarea>')[0]);
	}
	this.helper.setAttribute('style', '');
	this.helper.status = true;
	this.helper.innerHTML = o.CONTENTS || o.ITEM_NAME;
	this.helper._data = sUtils.object.extend(true, {}, o, this.editor.beforeData);
	
	// [수정] 이전 클래스들을 먼저 모두 제거
	this.helper.classList.remove('contract-helper-sign');
	this.helper.classList.remove('contract-helper-seal');
	this.helper.classList.remove('contract-helper-image');
	this.helper.classList.remove('contract-helper-checkbox');
	this.helper.classList.remove('contract-helper-mcheckbox');
	this.helper.classList.remove('contract-helper-ncheckbox');
	
	// [수정] backgroundImage도 초기화
	this.helper.style.backgroundImage = '';
	this.helper.style.backgroundSize = '';
	this.helper.style.backgroundPosition = '';
	this.helper.style.backgroundRepeat = '';
	
	if(o.ITEM_TYPE.toLowerCase() == "sign"){
		this.helper.classList.add('contract-helper-sign')
	} else if(o.ITEM_TYPE.toLowerCase() == "seal"){
		var sUrl = window.CONTEXT_NAME+"/SepoaActionController.sux?module=seal.management&action=doSelectSealPreviewImage&id="+o.ITEM_NO;
		this.helper.style.backgroundImage = 'url(' + sUrl + ')'; 
		this.helper.classList.add('contract-helper-seal')
	} else if(o.ITEM_TYPE.toLowerCase() == "image"){
		var sUrl = window.CONTEXT_NAME + '/SepoaImage.sux?mode=CUSTOM&METHOD=doImage&PROCESS_ID=sepoa.svc.contract.SepoaContractItem';
		sUrl += '&ATTACH_TYPE=' + o.ATTACH_TYPE + '&DES_FILE_PATH=' + o.DES_FILE_PATH;
		this.helper.classList.add('contract-helper-image')
		this.helper.style.backgroundImage = 'url(' + sUrl + ')';
		this.helper.style.backgroundSize = 'contain';
		this.helper.style.backgroundPosition = 'center';
		this.helper.style.backgroundRepeat = 'no-repeat';
	} else if(o.ITEM_TYPE.toLowerCase() == "checkbox"){
		// [CHECKBOX 기능 추가] 체크박스 헬퍼 스타일 설정
		this.helper.classList.add('contract-helper-checkbox')
		this.helper.innerHTML = ''; // 텍스트 제거
	} else if(o.ITEM_TYPE.toLowerCase() == "mcheckbox"){
		this.helper.classList.add('contract-helper-mcheckbox')
		this.helper.innerHTML = ''; // 텍스트 제거
	} else if(o.ITEM_TYPE.toLowerCase() == "ncheckbox"){
		this.helper.classList.add('contract-helper-ncheckbox')
		this.helper.innerHTML = ''; // 텍스트 제거
	}
	
	// [CHECKBOX 기능 추가] 체크박스는 흰색 배경, 다른 아이템은 색상 배경
	if(o.ITEM_TYPE.toLowerCase() != "checkbox" && o.ITEM_TYPE.toLowerCase() != "mcheckbox" && o.ITEM_TYPE.toLowerCase() != "ncheckbox"){
		$(this.helper).css("background-color", 'hsl('+o.COLOR_LEVEL+', 100%, 80%)')
	} else {
		$(this.helper).css("background-color", '#fff')
	}
	$(this.helper).css("transform", "translate("+(e.clientX+5)+"px,"+(e.clientY+5)+"px) rotateY(0deg)")
	
	if(o.ITEM_TYPE.toLowerCase() == "seal"){
		$(this.helper).css("width", this.convertCmToPx(o.SEAL_WIDTH || 1.5, "w")+"px");
		$(this.helper).css("height", this.convertCmToPx(o.SEAL_HEIGHT || 1.5, "h")+"px");
		
		this.helper.SEAL_WIDTH = o.SEAL_WIDTH;
		this.helper.SEAL_HEIGHT = o.SEAL_HEIGHT;
	} else if(o.ITEM_TYPE.toLowerCase() == "checkbox" || o.ITEM_TYPE.toLowerCase() == "mcheckbox" || o.ITEM_TYPE.toLowerCase() == "ncheckbox"){
		// [CHECKBOX 기능 추가] 체크박스 헬퍼 크기 고정
		$(this.helper).css("width", "16px");
		$(this.helper).css("height", "16px");
	} 
	
	this.helper.parentElement.classList.remove('contract-helper-hidden')
	this.helper.parentElement.onmousemove = function(ev) {
		$(this).find('.contract-helper-label').css("transform", "translate("+(ev.clientX+5)+"px,"+(ev.clientY+5)+"px) rotateY(0deg)");
	}
}
SepoaContractObject.prototype.hideHelper=function(){
	this.helper.status = false;
	this.helper.innerHTML = "";
	this.helper._data = void 0;
	this.helper.parentElement.classList.add('contract-helper-hidden')
	this.helper.parentElement.onmousemove = null;
}
SepoaContractObject.prototype.useItemsMenu=function(flag){
	this._getLayoutCell('items')[flag?"expand":"collapse"]();
}
SepoaContractObject.prototype.hideResize=function(){
	$('.contract-item-se-resize').css("display", "none");
}
SepoaContractObject.prototype.convertCmToPx=function(cm, wh){
	//var contWidth = this.editor.editorWrapper.firstChild.firstChild.editorLayer.offsetWidth;
	//var contHeight = this.editor.editorWrapper.firstChild.firstChild.editorLayer.offsetHeight;
	var contWidth = this.editor.objBox.firstChild.editorLayer.offsetWidth;
	var contHeight = this.editor.objBox.firstChild.editorLayer.offsetHeight;
	var widthPercent = cm/21*100;
	var heightPercent = cm/29.7*100;
	widthPercent = Math.round(widthPercent*100)/100.0;
	heightPercent = Math.round(heightPercent*100)/100.0;
	
	if(wh == "w"){
		return contWidth * widthPercent / 100;
	} else if(wh == "h"){
		return contHeight * heightPercent / 100;
	}
	return null;
}
SepoaContractObject.prototype.getSignNumber=function(){
	var signer = [];
	for(var i in this.items.sign.data.list){
		signer.push(this.items.sign.data.list[i].ID);
	}
	return signer.join(',');
}
SepoaContractObject.prototype.checkSignField = function(signerNo) {
	if(this.editor.objBox.childElementCount <= 0) {
		return false;
	}
	
	/*
	const isRenderDone = Array.from(this.editor.objBox.children).find(e => e._options['_PDF_PAGE_VIEWPORT'] === undefined) === undefined;
	if(isRenderDone === false) {
		return false;
	}
	*/

	var unsignedField = this.editor.objBox.querySelector('.contract-item.contract-item-box.contract-item-box-code-SIGN_ITEM_SIGN_' + signerNo);
	if(unsignedField == null) {
		return true;
	} else {
		// scrollIntoView를 사용하여 요소를 스크롤 컨테이너 상단에 표시
		// behavior: 'smooth' - 부드러운 스크롤
		// block: 'start' - 요소를 컨테이너 상단에 정렬
		unsignedField.scrollIntoView({ 
			behavior: 'smooth', 
			block: 'start',
			inline: 'nearest'
		});

		return false;
	}
}
// [MCHECKBOX 검증 기능 추가] 필수 체크박스 검증 메서드
SepoaContractObject.prototype.checkMcheckboxField = function(signerNo) {
	if(this.editor.objBox.childElementCount <= 0) {
		return true; // 페이지가 없으면 검증 통과
	}
	
	if(!this.editor.sci || this.editor.sci.length === 0) {
		return true; // 아이템이 없으면 검증 통과
	}
	
	// 현재 서명자의 MCHECKBOX 아이템들을 찾기
	var mcheckboxItems = [];
	for(var i = 0; i < this.editor.sci.length; i++) {
		var sciItem = this.editor.sci[i];
		if(sciItem && sciItem.conf && sciItem.conf.ITEM_TYPE == 'MCHECKBOX') {
			// SIGN_ID가 현재 서명자와 일치하는지 확인
			var signerIdFromItemCode = null;
			if(sciItem.conf.ITEM_CODE && sciItem.conf.ITEM_CODE.indexOf('SIGN_ITEM_MCHECKBOX_') === 0){
				signerIdFromItemCode = sciItem.conf.ITEM_CODE.replace('SIGN_ITEM_MCHECKBOX_', '');
			}
			if(signerIdFromItemCode == signerNo || sciItem.conf.SIGN_ID == signerNo) {
				mcheckboxItems.push(sciItem);
			}
		}
	}
	
	if(mcheckboxItems.length === 0) {
		return true; // MCHECKBOX가 없으면 검증 통과
	}
	
	// 각 MCHECKBOX가 체크되어 있는지 확인
	for(var i = 0; i < mcheckboxItems.length; i++) {
		var sciItem = mcheckboxItems[i];
		// 체크되지 않은 MCHECKBOX가 있으면 false 반환
		if(!sciItem.conf.CHECKBOX_CHECKED) {
			// [검증 실패 시 스크롤 및 하이라이트] 첫 번째 검증 실패한 아이템으로 스크롤 및 노란색 테두리 표시
			if(sciItem.base) {
				this._scrollToItemAndHighlight(sciItem.base);
			}
			return false;
		}
	}
	
	return true; // 모든 MCHECKBOX가 체크되어 있으면 true 반환
}
// [NCHECKBOX 검증 기능 추가] 필수 해제 체크박스 검증 메서드
SepoaContractObject.prototype.checkNcheckboxField = function(signerNo) {
	if(this.editor.objBox.childElementCount <= 0) {
		return true; // 페이지가 없으면 검증 통과
	}
	
	if(!this.editor.sci || this.editor.sci.length === 0) {
		return true; // 아이템이 없으면 검증 통과
	}
	
	// 현재 서명자의 NCHECKBOX 아이템들을 찾기
	var ncheckboxItems = [];
	for(var i = 0; i < this.editor.sci.length; i++) {
		var sciItem = this.editor.sci[i];
		if(sciItem && sciItem.conf && sciItem.conf.ITEM_TYPE == 'NCHECKBOX') {
			// SIGN_ID가 현재 서명자와 일치하는지 확인
			var signerIdFromItemCode = null;
			if(sciItem.conf.ITEM_CODE && sciItem.conf.ITEM_CODE.indexOf('SIGN_ITEM_NCHECKBOX_') === 0){
				signerIdFromItemCode = sciItem.conf.ITEM_CODE.replace('SIGN_ITEM_NCHECKBOX_', '');
			}
			if(signerIdFromItemCode == signerNo || sciItem.conf.SIGN_ID == signerNo) {
				ncheckboxItems.push(sciItem);
			}
		}
	}
	
	if(ncheckboxItems.length === 0) {
		return true; // NCHECKBOX가 없으면 검증 통과
	}
	
	// 각 NCHECKBOX가 체크 해제되어 있는지 확인
	for(var i = 0; i < ncheckboxItems.length; i++) {
		var sciItem = ncheckboxItems[i];
		// 체크되어 있는 NCHECKBOX가 있으면 false 반환 (반드시 해제되어 있어야 함)
		if(sciItem.conf.CHECKBOX_CHECKED) {
			// [검증 실패 시 스크롤 및 하이라이트] 첫 번째 검증 실패한 아이템으로 스크롤 및 노란색 테두리 표시
			if(sciItem.base) {
				this._scrollToItemAndHighlight(sciItem.base);
			}
			return false;
		}
	}
	
	return true; // 모든 NCHECKBOX가 체크 해제되어 있으면 true 반환
}

// MTEXT 필수 입력 검증 (NTEXT는 선택 입력이므로 검증 제외, 복수 MTEXT 모두 검사)
SepoaContractObject.prototype.checkSignerInputField = function(signerNo) {
	if(!this.editor || !this.editor.sci || this.editor.sci.length === 0) {
		return true;
	}
	for(var i = 0; i < this.editor.sci.length; i++) {
		var sciItem = this.editor.sci[i];
		if(!sciItem || !sciItem.conf) continue;
		var itemType = String(sciItem.conf.ITEM_TYPE || '').toUpperCase();
		if(itemType != 'MTEXT') continue;
		if(!this._isCurrentSignerItem(sciItem, signerNo)) continue;
		
		var val = this._getSignerTextFieldValue(sciItem);
		if(String(val).trim().length === 0) {
			if(sciItem.base && this._scrollToItemAndHighlight) {
				this._scrollToItemAndHighlight(sciItem.base);
			}
			return false;
		}
	}
	return true;
}

SepoaContractObject.prototype._getSignerTextFieldValue = function(sciItem) {
	if(!sciItem) return '';
	if(sciItem.editor && sciItem.editor.conf && sciItem.editor.conf.value != null && String(sciItem.editor.conf.value).length > 0) {
		return String(sciItem.editor.conf.value);
	}
	if(sciItem.conf && sciItem.conf.value != null && String(sciItem.conf.value).length > 0) {
		return String(sciItem.conf.value);
	}
	if(sciItem.input) {
		return sciItem.input.value || sciItem.input.innerHTML || '';
	}
	return '';
}

SepoaContractObject.prototype._isCurrentSignerItem = function(sciItem, signerNo) {
	if(!sciItem || !sciItem.conf) return false;
	if(sciItem.conf.SIGN_ID == signerNo) return true;
	var code = sciItem.conf.ITEM_CODE || '';
	if(!code) return false;
	var prefix = 'SIGN_ITEM_';
	if(code.indexOf(prefix) !== 0) return false;
	var rest = code.substring(prefix.length);
	var lastUnderscore = rest.lastIndexOf('_');
	if(lastUnderscore < 0) return false;
	return rest.substring(lastUnderscore + 1) == signerNo;
}

// [체크박스 검증 실패 시 스크롤 및 하이라이트] 공통 메서드
SepoaContractObject.prototype._scrollToItemAndHighlight = function(itemElement) {
	if(!itemElement) {
		return;
	}
	
	// 이전 하이라이트 제거
	if(this._validationHighlightElement) {
		this._validationHighlightElement.classList.remove('contract-item-validation-failed');
		this._validationHighlightElement = null;
	}
	
	// 이전 스크롤 인터벌 정리
	if(this._validationScrollInterval) {
		clearInterval(this._validationScrollInterval);
		this._validationScrollInterval = null;
	}
	
	// 현재 아이템에 노란색 테두리 추가
	itemElement.classList.add('contract-item-validation-failed');
	this._validationHighlightElement = itemElement;
	
	// scrollIntoView를 사용하여 요소를 스크롤 컨테이너 상단에 표시
	// behavior: 'smooth' - 부드러운 스크롤
	// block: 'start' - 요소를 컨테이너 상단에 정렬
	itemElement.scrollIntoView({ 
		behavior: 'smooth', 
		block: 'start',
		inline: 'nearest'
	});
}
SepoaContractObject.prototype.setSignFixHtml = function(signerInfo) {
	this._signFixHtmlArr = [];

	if(signerInfo) {
		for(var info of signerInfo) {
			if(info.SIGN_FIX_HTML) {
				this._signFixHtmlArr.push(JSON.parse(info.SIGN_FIX_HTML));
			}
		}
	}
}

//Item List
SepoaContractItemList=function(conf, _vp){
	sUtils.modules.extend(this, "EventManager")
	this.conf = conf;
	this.base = _vp.appendChild($('<div class="contract-itmes-area contract-itmes-area-item contract-items-area-hidden"><div class="contract-items-area-title"><div><p>'+((this.conf.title)?this.conf.title:Sepoa.cache.text.get('POA_CONTRACT_EDITOR.ITEMS_ITEM_TITLE'))+'</p></div></div><ul class="contract-items-list"></ul></div>')[0]);
	this.base.onclick=function(e) {
		this._onClickEvent(e);
	}.bind(this);

	this.objBox = this.base.children[1];
	if(this.conf.basicItem){
		this._basicItemLoad();
	}
	this._load();
}
SepoaContractItemList.prototype._load=function(){
	//아이템 리스트를 꾸려낼 서비스가 있다면 서비스 호출
	if(this.conf.service){
		doServiceAjax(this.conf.service.PROCESS_ID,'CONNECTION', this.conf.service.METHOD, this.conf.service.PARAMS, function(so) {
    		if(so.status == 1){
    			var data = [];
    			var sf = new SepoaFormater(so.result[0]).getArrayMap();
    			for(var i in sf) {
    				var target = sf[i];
    				if(target[(this.conf.service.PARAMS.VIEW_ITEMS == "EDIT") ?  'DISPLAY_EDIT' : 'DISPLAY_TEMPLATE'] === '1') {
    					data.push(target);
    				}
    			}
    			this._renderData(data);

    		}else{
    			//ERROR
    		}
		}.bind(this));
	}
	
	//파라미터로 넘어온 데이터가 있다면 데이터를 이용하여 아이템 리스트 그려내기
	if(this.conf.data){
		this._renderData(this.conf.data);
	}
}
//중복될 수 있으므로 기본아이템 분리처리
SepoaContractItemList.prototype._basicItemLoad=function(){
	var list = [];
	var dd = Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#CT020');
	var tmp = [];
	for(var i in dd){
		tmp.push({
			ITEM_NO:null,
			ITEM_CODE:null,
			ITEM_NAME:dd[i].text,
			ITEM_TYPE:dd[i].value
		})
	}
	list = tmp.concat(list);
	this._renderData(list);	
}
SepoaContractItemList.prototype._renderData=function(list){
	if(list == null) list = [];
	for(var i in list){
		this.base.classList.remove('contract-items-area-hidden');
		var item = list[i];
		
		var li = this.objBox.appendChild(document.createElement('li'));
		li._options=item;
		li.classList.add('contract-items-list-item');
		li.classList.add('contract-items-list-li');
		var icon = li.appendChild(document.createElement('div'));
		icon.classList.add('contract-items-list-item');
		icon.classList.add('contract-items-list-icon');
		icon.classList.add('contract-items-list-icon-'+item.ITEM_TYPE.toLowerCase());

		if(item.ITEM_TYPE == 'IMAGE') {
			var sUrl = window.CONTEXT_NAME + '/SepoaImage.sux?mode=CUSTOM&METHOD=doImage&PROCESS_ID=sepoa.svc.contract.SepoaContractItem';
			sUrl += '&ATTACH_TYPE=' + item.ATTACH_TYPE + '&DES_FILE_PATH=' + item.DES_FILE_PATH;
			icon.style.backgroundImage = 'url(' + sUrl + ')';
			icon.style.backgroundSize = 'contain';
			icon.style.backgroundPosition = 'center';
			icon.style.backgroundRepeat = 'no-repeat';
		}

		var text = li.appendChild(document.createElement('span'));
		text.classList.add('contract-items-list-item');
		text.classList.add('contract-items-list-text');
		text.innerHTML = item.ITEM_NAME;
	}
}
SepoaContractItemList.prototype._onClickEvent=function(e){
	var t = e.target;
	var cl = e.target.classList;
	if(cl.contains('contract-items-list-item')){
		if(t.tagName.toLowerCase() != "li"){
			t = $(t).closest('.contract-items-list-li')[0]
		}
		this.callEvent('onItemClick', [e, t._options]);
	}else{
		console.log(e)
	}
}
//Sign List
SepoaContractSignList=function(conf, _vp, parentObj){
	sUtils.modules.extend(this, "EventManager")
	this.conf = $.extend(true, {
		controlType:"default"
	}, conf);
	this.parentObj = parentObj;
	this.data = {
		nowCount:0,
		list:[]
	}
	this.base = _vp.appendChild($('<div class="contract-itmes-area contract-itmes-area-sign"><div class="contract-items-area-title"><div><p>'+Sepoa.cache.text.get('POA_CONTRACT_EDITOR.ITEMS_SIGN_TITLE')+'</p></div><div class="contract-items-sign-control"></div></div><div class="contract-items-sign-outer"></ul></div>')[0]);
	this.base.onclick=function(a) {
	    return function() {
	        return function(e){
	    		this._onClickEvent(e);
	    	}.apply(a, arguments)
	    }
	}(this)
	this.objBox = this.base.children[1];
	this.controlBox = this.base.children[0].children[1];
	this.initSignData();
	this.initControlBox();
	if(conf.control === false){
		this.hideControl();
	}
}
SepoaContractSignList.prototype.initSignData=function(){
	if(this.conf.service){
		var so = doServiceAjax(this.conf.service.PROCESS_ID,'CONNECTION', this.conf.service.METHOD, this.conf.service.PARAMS, null, false);
		if(so.status == 1){
			this._signData = [];
			var sf = new SepoaFormater(so.result[0]).getArrayMap();
			for(var i in sf) {
				var target = sf[i];
				if(target[(this.parentObj.conf.workType == "T") ? 'DISPLAY_TEMPLATE' : 'DISPLAY_EDIT'] === '1') {
					this._signData.push(target);
				}
			}
		}else{
			//ERROR
		}
	}
}
SepoaContractSignList.prototype.initControlBox=function(){
	var controlWrapper = this.controlBox.appendChild($('<div><div class="contract-items-sign-control-item contract-items-sign-control-button xi-sepoa-dummy contract-items-sign-control-minus"></div><div class="contract-items-sign-control-item  contract-items-sign-control-count"></div><div class="contract-items-sign-control-item contract-items-sign-control-button xi-sepoa-dummy contract-items-sign-control-plus"></div></div>')[0]);
	controlWrapper.classList.add("contract-items-sign-control-type-"+this.conf.controlType);

	//if(this.conf.controlType == 'popup'){
	//	controlWrapper.classList.add("contract-items-sign-addflag-"+(this.conf.addFlag||'true'));
	//}
	//
	for(var i in this._controlls[this.conf.controlType]){
		this[i] = this._controlls[this.conf.controlType][i];
	}
}
SepoaContractSignList.prototype._controlls={
	default:{
		_onClickEvent:function(e){
			var cl = e.target.classList;
			if(cl.contains('contract-items-sign-control-item')){
				if(cl.contains('contract-items-sign-control-plus')){
					if(this.data.nowCount < this.conf.min) {
						for(let i = this.data.nowCount; i < this.conf.min; i = this.data.nowCount) {
							this.add();
						}
					} else {
						this.add();
					}
				}else if(cl.contains('contract-items-sign-control-minus')){
					this.remove();
				}
			}else if(cl.contains('contract-items-list-item')){
				var t = e.target;
				if(t.tagName.toLowerCase() != "li"){
					t = $(t).closest('.contract-items-list-li')[0]
				}
				if(t._options.ITEM_TYPE.toUpperCase() != 'SIGNER')
					this.callEvent('onItemClick', [e, t._options]);
			}else{
				console.log(e)
			}
		},
		add:function(){
			if((this.data.nowCount + 1) > (this.conf.max || 9)){
				Alert(Sepoa.cache.text.get('POA_CONTRACT_EDITOR.MESSAGE_0001').replace('#NUMBER#', this.conf.max));
				return;
			}
			
			var id = getDocumentNumber("SIGNER_ID");
			this.addSignItem({
				ID:id,
				COLOR_LEVEL:(((this.data.nowCount - 1) * 60) + 230)
			});
			this.controlBox.children[0].children[1].innerHTML = this.data.nowCount;
		},
		remove:function(){
			if((this.data.nowCount - 1) < (this.conf.min || 1)){
				Alert(Sepoa.cache.text.get('POA_CONTRACT_EDITOR.MESSAGE_0002').replace('#NUMBER#', this.conf.min));
				return;
			}
			var lastId = this.data.list[this.data.list.length - 1].ID;
			
			$(this.parentObj.editor.base).find('.contract-item-box-sign-id-'+lastId).remove();
			
			this.callEvent('onSignerRemove',[lastId]);
		},
		addBlankSigner:function(id){
			var id = id || getDocumentNumber("SIGNER_ID");
			this.addSignItem({
				ID:id,
				COLOR_LEVEL:(((this.data.nowCount - 1) * 60) + 230)
			});
			this.controlBox.children[0].children[1].innerHTML = this.data.nowCount;
		}
	}
}
SepoaContractSignList.prototype.removeAll=function(){
	for(let signer of this.data.list) {
		this.callEvent('onSignerRemove',[signer.ID]);
	}
}
SepoaContractSignList.prototype.setData=function(signerList){
	if(signerList){
		this.removeAll();
		for(var i in signerList){
			this.addSignItem($.extend(true, {
				ID:signerList[i].SIGNER_NO,
				COLOR_LEVEL:(((this.data.nowCount - 1) * 60) + 230)
			}, signerList[i]));
			this.controlBox.children[0].children[1].innerHTML = this.data.nowCount;
		}
	}
}
SepoaContractSignList.prototype.addSignItem=function(info){
	if(info && !info.COLOR_LEVEL){
		info.COLOR_LEVEL = ((info.SIGNER_SEQ - 1) * 60) + 230;
	}
	if(info && !info.ID){
		if(info.SIGNER_NO){
			info.ID = info.SIGNER_NO;
		}else{
			info.ID = getDocumentNumber("SIGNER_ID");
		}
	}
	
	this.data.nowCount++;
	this.data.list.push(info);
	
	if(this.signItems === undefined) {
		this.signItems = {};
	}

	var ul = this.signItems[info.ID] = this.objBox.appendChild(document.createElement('ul'));
	ul.classList.add("contract-items-sign-box");

	for(var i in this._signData) {
		var itemType = this._signData[i].ITEM_TYPE.split('#');
		
		var li = ul.appendChild(document.createElement('li'));
		li.classList.add('contract-items-list-li')
		li.classList.add('contract-items-list-item')
		li._options={
			ITEM_NO:null,
			ITEM_CODE:"SIGN_ITEM_"+this._signData[i].ITEM_CODE+"_"+info.ID,
			ITEM_NAME:info[this._signData[i].ITEM_CODE] || this._signData[i].ITEM_NAME,
			ITEM_TYPE:itemType[0],
			SIGN_ID:info.ID,
			COLOR_LEVEL:info.COLOR_LEVEL
		}
		
		if(info[this._signData[i].ITEM_CODE]){
			li._options.ITEM_VALUE=info[this._signData[i].ITEM_CODE];
		}

		var icon = li.appendChild(document.createElement('div'));
		if(itemType[0] == "SIGNER"){
			if(this.parentObj.conf.contType == 'B' && info.SIGNER_COMPANY_NAME){
				li._options.ITEM_NAME=info.SIGNER_COMPANY_NAME;
			}else if(info.SIGNER_NAME_LOC){
				li._options.ITEM_NAME=info.SIGNER_NAME_LOC;
			}
		} else if(itemType.length >= 2){
			li._options.ITEM_FORMAT=itemType[1];
		}
		
		// [CHECKBOX 기능 추가] 체크박스 아이템 아이콘 처리
		if(itemType[0].toUpperCase() == 'CHECKBOX'){
			icon.style.backgroundColor='transparent';
			icon.classList.add('contract-items-list-item');
			icon.classList.add('contract-items-list-icon');
			icon.classList.add('contract-items-list-icon-checkbox');
		} else if(itemType[0].toUpperCase() == 'MCHECKBOX'){
			icon.style.backgroundColor='transparent';
			icon.classList.add('contract-items-list-item');
			icon.classList.add('contract-items-list-icon');
			icon.classList.add('contract-items-list-icon-mcheckbox');
		} else if(itemType[0].toUpperCase() == 'NCHECKBOX'){
			icon.style.backgroundColor='transparent';
			icon.classList.add('contract-items-list-item');
			icon.classList.add('contract-items-list-icon');
			icon.classList.add('contract-items-list-icon-ncheckbox');
		} else if(itemType[0].toUpperCase() == 'MTEXT' || itemType[0].toUpperCase() == 'NTEXT'){
			icon.style.backgroundColor='transparent';
			icon.classList.add('contract-items-list-item');
			icon.classList.add('contract-items-list-icon');
			icon.classList.add('contract-items-list-icon-'+itemType[0].toLowerCase());
		} else {
			icon.style.backgroundColor='hsl('+info.COLOR_LEVEL+', 100%, 80%)';
			icon.classList.add('contract-items-list-item');
			icon.classList.add('contract-items-list-icon');
			icon.classList.add('contract-items-list-icon-'+this._signData[i].ITEM_CODE.toLowerCase());
		}
		var text = li.appendChild(document.createElement('span'));
		text.classList.add('contract-items-list-item');
		text.classList.add('contract-items-list-text');
		text.innerHTML = li._options.ITEM_NAME;
	}

	this.callEvent('onSignerAdd',[info.ID, info.COLOR_LEVEL, info]);
}
SepoaContractSignList.prototype.showControl=function(){
	this.controlBox.classList.remove('contract-hide');
}
SepoaContractSignList.prototype.hideControl=function(){
	this.controlBox.classList.add('contract-hide');
}
SepoaContractSignList.prototype.setHidden=function(){
	this.base.style.display='none'
}
SepoaContractSignList.prototype.setShow=function(){
	this.base.style.display=''
}
SepoaContractSignList.prototype.drawDefault=function(){
	for(var i=0;i<this.conf.default ; i++){
		this.add();
	}
}
SepoaContractSignList.prototype.setDefaultCount=function(cnt){
	this.conf.default = cnt;
}
SepoaContractSignList.prototype.clear=function(){
	this.removeAll()
	this.drawDefault();
}
SepoaContractSignList.prototype.enabled=function(flag){
	if(flag == null || flag){
		this.objBox.parentElement.style.display = 'block';
	} else {
		this.objBox.parentElement.style.display = 'none'
	}
}
//Signer Modal
SepoaContractSignerDialog=function(conf){
	sUtils.modules.extend(this, "EventManager")
	this.conf = $.extend(true, {}, conf);
	this.base = conf.layout.base;
	this.listObj = [];
	this.drawBase();
}
SepoaContractSignerDialog.prototype.drawBase=function(){
	this._modalLayer = this.base.appendChild($('<div class="dhx_modal_cover" style="display:none;"></div>')[0])
	this._modalLayer.onclick=function(a) {
	    return function() {
	        return function(e){
	    		this.hide();
	    	}.apply(a, arguments)
	    }
	}(this);
	var dialogBase = this._dialogBase = this.base.appendChild($('<div class="set-signer-modal-container set-signer-modal-type-'+this.conf.contType+'" style="display:none;"></div>')[0]);
	$(dialogBase).draggable({handle: '.set-signer-header-btn-container', delay: 200});

	var closeButton = this._dialogBase.appendChild($('<div class="set-signer-header-btn-container"><div class="set-signer-header-label">'+Sepoa.cache.text.get('POA_CONTRACT_EDITOR.ITEMS_SIGN_TITLE')+'</div><div class="set-signer-modal-close-btn"></div></div>')[0]).children[1];
	closeButton.onclick = this.hide.bind(this);

	var topBtnConatiner = this._dialogBase.appendChild($('<div class="set-signer-top-btn-container sepoaux_button multi-btn-container"></div>')[0]);
	
	var topBtnLeftArea = document.createElement('div');
	topBtnLeftArea.className = 'set-signer-top-btn-left-area';
	topBtnConatiner.appendChild(topBtnLeftArea);
	
	if(this.conf.module.items.sign.signOrderCode && this.conf.module.items.sign.signOrderCode.length > 0) {
		var signOrderCode = this.conf.module.items.sign.signOrderCode;

		var signOrderWrapper = document.createElement('div');
		signOrderWrapper.className = 'sign-order-wrapper';
		topBtnLeftArea.appendChild(signOrderWrapper);	

		var signOrderArr = Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+"#"+Sepoa.info.LANGUAGE+"#"+signOrderCode).map(function(e){return e.value});
		for(var i in signOrderArr) {
			var signOrderValue = signOrderArr[i].toUpperCase();

			var signOrderRadioButtonWrapper = document.createElement('div');
			signOrderRadioButtonWrapper.className = 'sign-order-radio-button-wrapper';
			signOrderWrapper.appendChild(signOrderRadioButtonWrapper);

			var signOrderRadioButton = document.createElement('input');
			signOrderRadioButton.className = 'sign-order-radio-button';
			signOrderRadioButton.type = 'radio';
			signOrderRadioButton.name = 'sign_order';
			signOrderRadioButton.id = signOrderValue;
			signOrderRadioButton.value = signOrderValue;
			signOrderRadioButton.onchange = this._onChangeSignOrderRadio.bind(this);
			signOrderRadioButtonWrapper.appendChild(signOrderRadioButton);

			var signOrderLabel = document.createElement('label');
			signOrderLabel.className = 'sign-order-label';
			signOrderLabel.htmlFor = signOrderValue;
			signOrderRadioButtonWrapper.appendChild(signOrderLabel);

			var signOrderText = document.createElement('div');
			signOrderText.className = 'sign-order-text btn_inner_text';
			signOrderText.innerText = Sepoa.cache.text.get('POA_CONTRACT_EDITOR.SIGN_ORDER_' + signOrderValue);
			signOrderLabel.appendChild(signOrderText);
		}
	}

	var topBtnRightArea = document.createElement('div');
	topBtnRightArea.className = 'set-signer-top-btn-right-area';
	topBtnConatiner.appendChild(topBtnRightArea);

	var registerButton = topBtnRightArea.appendChild($('<div id="save" class="btn_default btn_theme_save btn_size_s btn_margin_right" style="float: left;"><div class="btn_inner_text">'+Sepoa.cache.text.get("POA_CONTRACT_EDITOR.SIGNER_SAVE")+'</div></div>')[0]);
	registerButton.onclick = this.register.bind(this);

	if(this.conf.module.items.sign.partnerAddFlag){
		var registerCompanyButton = topBtnRightArea.appendChild($('<div id="copyRow" class="btn_default btn_theme_default btn_size_s btn_margin_left" style="float: right;"><div class="btn_inner_text">'+Sepoa.cache.text.get("POA_CONTRACT_EDITOR.REGISTER_COMPANY")+'</div></div>')[0]);
		registerCompanyButton.onclick = this.registerCompany.bind(this);
	}

	var listContainer = this._dialogBase.appendChild($('<div class="set-signer-list-container"><div class="set-signer-list-contents"><div class="set-signer-list-contents-header"></div><ul class="set-signer-list-contents-body"></ul></div></div>')[0]);	
	var signerGridHeader = listContainer.children[0].children[0];
	this.signerGridRows = listContainer.children[0].children[1];
	this.initSignerGridRowSortable();
	
	var headerCode = "CT099";
	if(this.conf.module.items.sign.headerCode && this.conf.module.items.sign.headerCode.length > 0){
		headerCode = this.conf.module.items.sign.headerCode;
	}
	
	var headerTextArray = Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+"#"+Sepoa.info.LANGUAGE+"#"+headerCode);
	for(var i in headerTextArray){
		var item = headerTextArray[i];
		signerGridHeader.appendChild($('<div class="signer-list-header signer-list-header-'+item.value.toLowerCase()+'">'+item.text+'</div>')[0]);
	}

	if(this.conf.contType == 'B') {
		var addButton = this._dialogBase.appendChild($('<div class="set-signer-bottom-btn-container"><div class="set-signer-bottom-add-button"></div></div>')[0]).children[0];
		addButton.onclick = function() {
			if(this.listObj.length >= this.signListObj.conf.max) {
				Alert(Sepoa.cache.text.get('POA_CONTRACT_EDITOR.MESSAGE_0001').replace('#NUMBER#', this.signListObj.conf.max));
				return false;
			}

			this.addSignerRow();
		}.bind(this);
	}
}
SepoaContractSignerDialog.prototype.initSignerGridRowSortable=function(){
	if(this.signerGridRows != null && this.signerGridRows != undefined){
		this.signerGridRows.obj = this;
		$(this.signerGridRows).sortable({
			delay: 100,
			axis: "y",
			containment: "parent",
			stop: function(event, ui){
				this.obj._onUpdateRowSeq();
			}
		});
	}
}
SepoaContractSignerDialog.prototype.setPosition=function(pos){
	this._dialogBase.style.left = pos.x+'px';
	this._dialogBase.style.top = pos.y+'px';
}
SepoaContractSignerDialog.prototype.show=function(signerData){
	this._dialogBase.style.display = '';
	this._modalLayer.style.display = '';
}
SepoaContractSignerDialog.prototype.hide=function(){
	this._dialogBase.style.display = 'none';
	this._modalLayer.style.display = 'none';
	this.clear();
}
SepoaContractSignerDialog.prototype.clear=function(){
	this.signerGridRows.innerHTML = "";
	this.listObj=[];
}
SepoaContractSignerDialog.prototype.addSignerRow=function(data){
	var formOption = [
		{type:"block",width:"auto",blockOffset:"0",name:"ADD_SIGNER_BLOCK",offsetTop:"0",list:[
			{type: "fieldset",  width: "auto", blockOffset: 0, name: "SIGNER_SET_ITEM", className: "signer-setter-form", list: [
				{type: "label", name: "SIGNER_SEQ", labelWidth: "20", value: "", className: "signer-setter-form-signer-seq", label: "", offsetLeft: "17"},
				{type: "newcolumn", offset: "0"},
				{type: "image", name: "SIGNER_SEARCH", value: "", labelWidth: "40", className: "btn_search_black", label: "", inputWidth: "27", inputHeight: "27", offsetLeft: "45"},
				{type: "newcolumn", offset: "0"},
				{type: "input", value: "", name: "SIGNER_COMPANY", labelWidth: "0", inputWidth: "176", offsetLeft: "44", required: true, disabled: true, readonly: true, label: ""},
				{type: "newcolumn", offset: "0"},
				{type: "input", value: "", name: "SIGNER_NAME", labelWidth: "0", inputWidth: "180", offsetLeft: "23", required: true, disabled: true, readonly: true, label: ""},
				{type: "newcolumn"},
				{type: "input", value: "", name: "SIGNER_EMAIL", labelWidth: "0", inputWidth: "240", offsetLeft: "30", required: true, label: ""},
				{type: "newcolumn", offset: ""},
				{type: "input", value: "", name: "SIGNER_PHONE", labelWidth: "0", inputWidth: "180", offsetLeft: "30", required: true, label: ""},
				{type: "hidden", value: "", name: "OWNER", labelWidth: "0", inputWidth: "180", required: true},
				{type: "hidden", value: "", name: "ADDR", labelWidth: "0", inputWidth: "180", required: true},
				{type: "newcolumn", offset: "0"},
				{type: "container", name: "SIGNER_DELETE", value: "", labelWidth: "21", className: "signer-setter-form-signer-delete", label: ""}
			]}
		]}
	];

	if(this.conf.module.items.sign.formOption) {
		formOption[0].list[0].list = Sepoa.cache.form.get(this.conf.module.items.sign.formOption);
	}

	var signerRow = this.signerGridRows.appendChild($('<li class="set-signer-form-wrapper"><div class="set-signer-form-container"><div class="set-signer-form-area"></div></div></li>')[0]);
	signerRow.obj = this;
	signerRow.formObj = new SepoaMultiDhtmlXFormObject(signerRow.children[0].children[0], 'signerSetterForm', [formOption], true);
	if(data && data.SIGNER_ID == Sepoa.info.ID && this.conf.contType == 'B'){
		$(signerRow.children[0].children[0]).find(".signer-setter-form-signer-delete").addClass("dummy-area");
		$(signerRow.children[0].children[0]).find(".signer-setter-form-signer-delete").removeClass("signer-setter-form-signer-delete");
		
	}
	if(this.conf.contType != 'B') {
		$(signerRow.children[0].children[0]).find(".signer-setter-form-signer-delete").addClass("dummy-area");
		$(signerRow.children[0].children[0]).find(".signer-setter-form-signer-delete").removeClass("signer-setter-form-signer-delete");
	}
	
	signerRow.formObj.attachEvent('onFocus', this._onFocusSignerInfo);
	signerRow.formObj.attachEvent('onChange', function(signerRow, id, value) {
		this._onChangeSignerInfo.call(signerRow, id, value);
	}.bind(this, signerRow));

	signerRow.formObj._apiConnector("_getItemNode", ["SIGNER_DELETE"]).children[1].children[0].onclick=function(s) {
		this._onRowDelete(s);
	}.bind(this, signerRow);

	signerRow.formObj._apiConnector("_getItemNode", ["SIGNER_SEARCH"]).onclick=function(s){
		this._openSelectComapnyPopup(s);
	}.bind(this, signerRow);

	signerRow._data = $.extend(true, {}, data, {
		COLOR_LEVEL:((this.listObj.length * 60) + 230),
		SIGNER_SEQ:this.listObj.length+1+''
	})

	if(!signerRow._data.ID) signerRow._data.ID = getDocumentNumber("SIGNER_ID"); 
	this.listObj.push(signerRow);
	this._onUpdateRowData(signerRow);
	
	for(i in signerRow.formObj._formList){
		var form = signerRow.formObj._formList[i].obj;
		var base = form.cont;
		$(base).find('.dhxform_select').css('border-radius', '0px');
		$(base).find('.dhxform_select').css('border', '1px solid #AFAFAF');
		$(base).find('.dhxform_custom_required .dhxform_control >').css('background-color', 'transparent');
		$(base).find('.dhxform_custom_required .dhxform_control >').css('text-align', 'center');

		var signOrder = this.signListObj.conf.signOrder = $('.sign-order-wrapper').find('input[name="sign_order"]:checked').val();
		if(signOrder == 'PARALLEL') {
			$(base).find('.signer-setter-form-signer-seq').hide();
		}

		var target = $(form.base).find('.dhxform_base')[0];
		target.style.width = '100%';

		var parents = $(target).parents();
		for(var j = 0; j < parents.length; j++) {
			parents[j].style.width = '100%';
			if(parents[j].classList.contains('set-signer-form-container')) break;
		}
	}

}
SepoaContractSignerDialog.prototype._onChangeSignOrderRadio = function(e) {
	var formSeq = $('.signer-setter-form-signer-seq');

	if(e.target.id == 'SERIES') {
		this._dialogBase.classList.remove('set-signer-modal-type-B-PARALLEL');
		this._dialogBase.classList.add('set-signer-modal-type-B');
		formSeq.show();
	} else if(e.target.id == 'PARALLEL') {
		this._dialogBase.classList.add('set-signer-modal-type-B-PARALLEL');
		this._dialogBase.classList.remove('set-signer-modal-type-B');
		formSeq.hide();
	}
}
SepoaContractSignerDialog.prototype._onChangeSignerInfo=function(id, value){
	if(id == 'SIGNER_EMAIL'){
		if(value){
			var regexEmail = /^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*.[a-zA-Z]{2,3}$/;
			if(!regexEmail.test(value)){
				Alert(Sepoa.cache.text.get("POA_CONTRACT_EDITOR.SIGNER_EMAIL_INVALIDATE_WARNING"));
				this.formObj.setValue(id, "", true);
			}else{
				this._data[id] = value;
				this.obj._onUpdateRowData(this, true, id);
			}
		}
	}else if(id == 'SIGNER_PHONE'){
		if(value){
			var regexPhone = /(^02|^01\d{1}|^\d{3})-(\d{3,4})-(\d{4}$)|(^02|^01\d{1}|^\d{3})(\d{3,4})(\d{4}$)/;
			var regexReplace = /(^02|^01\d{1}|^\d{3})(\d{3,4})(\d{4}$)/;
			if(!regexPhone.test(value)){
				Alert(Sepoa.cache.text.get("POA_CONTRACT_EDITOR.SIGNER_PHONE_INVALIDATE_WARNING"));
				this.formObj.setValue(id, "", true);
			}else{
				this._data[id] = value.replace(regexReplace, '$1-$2-$3');
				this.obj._onUpdateRowData(this, true, id);
			}
		}
	}
}
SepoaContractSignerDialog.prototype._onFocusSignerInfo=function(id, value, ob){
	if(id != 'SIGNER_COMPANY'){
		var formData = this.getFormData();
		if(formData['SIGNER_COMPANY'].length == 0){
			var that = this;
			Alert(Sepoa.cache.text.get("POA_CONTRACT_EDITOR.SIGNER_COMPANY_NULL_WARNING"), function(flag){
				if(flag){
					that.setItemFocus('SIGNER_COMPANY');
				}
			});
			return false;
		}
		
		if(id == 'SIGNER_EMAIL' || id == 'SIGNER_PHONE'){
			if(formData['SIGNER_NAME'].length == 0){
				var that = this;
				Alert(Sepoa.cache.text.get("POA_CONTRACT_EDITOR.SIGNER_ID_NULL_WARNING"), function(flag){
					if(flag){
						that.setItemFocus('SIGNER_NAME');
					}
				});				
			}
			return false;
		}
	}
}
SepoaContractSignerDialog.prototype._onRowDelete=function(row){
	this.listObj.splice(this.listObj.indexOf(row), 1);
	row.parentElement.removeChild(row);
	this._onUpdateRowSeq();
}
SepoaContractSignerDialog.prototype._openSelectComapnyPopup=function(row){
	var popupCode = 'CT510';
	popupCode = this.conf.contType ? popupCode + '_' + this.conf.contType : popupCode;

	if(this.conf.contType && this.conf.contType == 'B') {
		PopupCommonPost(popupCode, this._companySelectCallback.bind(this, row), [Sepoa.info.HOUSE_CODE]);
	} else if(this.conf.contType && this.conf.contType == 'E') {
		PopupCommonPost(popupCode, this._userSelectCallback.bind(this, row), [Sepoa.info.HOUSE_CODE, Sepoa.info.COMPANY_CODE]);
	}
}
SepoaContractSignerDialog.prototype._companySelectCallback=function(row, companyName, irsNo, owner, signerName, addr, companyCode, email, signerId, phone, houseCode){
	var signerDataObj = {
		 COMPANY_CODE : companyCode
		,HOUSE_CODE : houseCode
		,SIGNER_COMPANY : companyName
		,SIGNER_COMPANY_NAME : companyName
		,SIGNER_EMAIL : email
		,SIGNER_ID : signerId
		,SIGNER_NAME_LOC : signerName
		,SIGNER_NAME : signerName
		,SIGNER_PHONE : phone
		,OWNER : owner
		,ADDR : addr
		,IRS_NO : irsNo
	}
	
	$.extend(true, row._data, signerDataObj);
	row.formObj.dataMapper(signerDataObj, "JSON");
}
SepoaContractSignerDialog.prototype._userSelectCallback=function(row, signerName, email, phone, houseCode, companyCode, signerId, companyName, dept, position, addr){
	var signerDataObj = {
		 COMPANY_CODE			: companyCode
		,HOUSE_CODE				: houseCode
		,SIGNER_COMPANY			: companyName ? companyName : ''
		,SIGNER_COMPANY_NAME	: companyName ? companyName : ''
		,SIGNER_EMAIL			: email ? email : ''
		,SIGNER_ID				: signerId ? signerId : ''
		,SIGNER_NAME_LOC		: signerName ? signerName : ''
		,SIGNER_NAME			: signerName ? signerName : ''
		,SIGNER_PHONE			: phone ? phone : ''
		,DEPT					: dept ? dept : ''
		,POSITION				: position ? position : ''
		,ADDR					: addr ? addr : ''
	}
	$.extend(true, row._data, signerDataObj);
	row.formObj.dataMapper(signerDataObj, "JSON");
}
SepoaContractSignerDialog.prototype._onUpdateRowSeq=function(){
	var dataList = this.signerGridRows.children;
	for(var i=0;i<dataList.length;i++){
		var data = dataList[i]._data;
		var ns = parseInt(data.SIGNER_SEQ);
		if(ns != (i+1)){
			data.SIGNER_SEQ = i+1+'';
			this._onUpdateRowData(dataList[i]);
		}
	}
}
SepoaContractSignerDialog.prototype._onUpdateRowData=function(r, f, id){
	if(f === false){
		for(var i in r._data){
			if(id != i){
				r.formObj.setValue(i, r._data[i])
			}
		}
	}else if(f === true){
		r.formObj.setValue(id, r._data[id], true)
	}else {
		for(var i in r._data){
			r.formObj.setValue(i, r._data[i])
		}
	}
	$(r).find('fieldset').css('border-left', '5px solid hsl(' + r._data.COLOR_LEVEL+', 100%, 80%)');
}
SepoaContractSignerDialog.prototype.load=function(list){
	$('.sign-order-wrapper').find('input#' + this.signListObj.conf.signOrder).click();

	for(var i in list){
		this.addSignerRow(list[i]);
	}
}
SepoaContractSignerDialog.prototype.registerCompany=function(){
	var width = 530
    var height = 600
    var params = {
			
	}
	var callback = null;
	PopupOpenPost('/Contract/Module/Popup/ModulePopupRegisterCompanyLayout.sux', params, callback, width, height, Sepoa.cache.text.get("POA_CONTRACT_EDITOR.COMPANY_REGISTER_POPUP_TITLE"));
}
SepoaContractSignerDialog.prototype.register=function(){
	var reqItem = ["COMPANY_CODE", "SIGNER_ID", "SIGNER_EMAIL", "SIGNER_PHONE"]

	var data = [];
	for(var i=0;i<this.signerGridRows.children.length;i++){
		var r = $.extend({}, this.signerGridRows.children[i]._data);
		for(var j in reqItem){
			if(r[reqItem[j]] === undefined || r[reqItem[j]].length == 0){
				Alert(Sepoa.cache.text.get("POA_CONTRACT_EDITOR.SIGNER_FORM_NULL_WARNING"));
				return;
			}
		}
		
		var duplicateCnt = data.filter(function(e) {return e.HOUSE_CODE === r.HOUSE_CODE && e.COMPANY_CODE === r.COMPANY_CODE && e.SIGNER_ID === r.SIGNER_ID}).length;
		if(duplicateCnt > 0) {
			Alert(Sepoa.cache.text.get("POA_CONTRACT_EDITOR.SIGNER_DUPLICATE_WARNING"));
			return;
		}
		
		data.push(r);
	}

	if(this.callEvent("onRegistSigner", [data])){
		this.signListObj.conf.signOrder = $('.sign-order-wrapper').find('input[name="sign_order"]:checked').val();
		this.hide();
	}
}

//Preview
SepoaPreview=function(conf, _vp){
	sUtils.modules.extend(this, "EventManager")
	this.conf = $.extend(true, {
		mapping:{
			ID:"ID",
			IMAGE:"ID"
		},
		dynamic:true
	},conf);
	if(this.conf.language){
		Sepoa.cache.text.load(this.conf.language, false)
	}
	_vp.classList.add('sepoa-preview-base');
	this._rowsAr = {};
	this.base = _vp.appendChild($('<div class="sepoa-preview-area"></div>')[0]);
	if(conf.title) {
		this.base.appendChild($('<div class="preview-title">'+conf.title+'</div>')[0]);
	}
	this.objBox = this.base.appendChild($('<ul class="sepoa-preview-list"></div>')[0])
	
	this.base.onscroll=function(a){
		return function(){
			return function(){
				this._lazyLoader();
			}.apply(a, arguments)
		}
	}(this)
	this.base.onclick=function(a) {
	    return function() {
	        return function(e){
	    		this._onClickEvent(e);
	    	}.apply(a, arguments)
	    }
	}(this)
}
SepoaPreview.prototype._onClickEvent=function(e){
	var cl = e.srcElement.classList;
	if(cl.contains('sepoa-preview-list-item-img')){
		var data = e.srcElement.parentElement.parentElement._options;
		this.callEvent('onItemClick', [e, sUtils.string.dataReplacer(this.conf.mapping.ID, data), data])
	}
}
SepoaPreview.prototype.setMappingTable=function(k, v){
	if(typeof(k)=="string"){
		var t = {}
		t[k] = v;
		k = t;
	}
	for(var i in k){
		this.conf.mapping[i] = k[i];
	}
}
SepoaPreview.prototype.clear=function(){
	this.objBox.innerHTML='';
	this.objBox.render = false;
	this._rowsAr = {};
}
SepoaPreview.prototype.load=function(data, type){
	if(type !== "json"){
		data = data.getArrayMap();
	} 
	
	var pdfPromiseArray = [];
	for(let i in data){
		if(this.conf.type?.includes(data[i].FILE_TYPE)) {
			pdfPromiseArray.push(
				pdfjsLib.getDocument({
					url: sUtils.string.dataReplacer(this.conf.IMAGE_SERVLET, data[i]),
					cMapUrl : window.cMapUrl ? window.cMapUrl : window.CONTEXT_NAME + '/resource/plugin/pdfjs/cmaps/',
					_data:data[i]
				}).promise
			);
		}
	}
	
	Promise.all(pdfPromiseArray).then((pdfs) =>{
		for(let i in pdfs){
			this.add(pdfs[i])
		}
		this._lazyLoader();
		
		var layoutWidth = 0;
		if(Object.keys(this._rowsAr).length != 0){
			layoutWidth = 200
		}
		var layoutObj = this.parentObj.conf.layout;
		layoutObj.cells(this.conf.cell).setMinWidth(layoutWidth);
		layoutObj.cells(this.conf.cell).setWidth(layoutWidth);
	})
}
SepoaPreview.prototype.add=function(pdf){
	let numPages = pdf.numPages;
	
	const data = pdf._transport._params._data;
	const fixHtml = data.FIX_HTML ? JSON.parse(data.FIX_HTML) : null;
	if(fixHtml) {
		const fixHtmlMaxPage = Math.max(...Object.keys(fixHtml));
		numPages = numPages > fixHtmlMaxPage ? numPages : fixHtmlMaxPage;
	}
	
	for(let pageIdx=1;pageIdx <= numPages;pageIdx++){
		let data = $.extend(true, {}, pdf._transport._params._data);
		data._PDF_PAGE_IDX=pageIdx;
		data._PDF_DOC=pdf;
		data.PART_ID = sUtils.string.lpad(pageIdx, 3, '0')
		data.ID = sUtils.string.dataReplacer(this.conf.mapping.ID, data);
		let idx = this.objBox.children.length + 1;
		
		let sToken = '';
		if(window.sTokenString) {
			sToken = '&s-token=' + window.sTokenString;
		}
	
		var li = this._rowsAr[data.ID] = this.objBox.appendChild($('<li class="sepoa-preview-list-li"></li>')[0]);
		li._options = data; 
		var contents = li.appendChild($('<div class="sepoa-preview-list-item sepoa-preview-list-item-contents"><canvas class="sepoa-preview-list-item sepoa-preview-list-item-img sepoa-preview-lazy-loading")></canvas></div>')[0]);
		var pageLabel = li.appendChild($('<div class="sepoa-preview-list-item-page"><span>'+idx+' / </span></div>')[0]);
		if(this.conf.type){
			for(var i in this.conf.type){
				pageLabel.classList.add(this.conf.type[i]);
			}
		}
		this.updateTotalCount();
	}
}
SepoaPreview.prototype._lazyLoader=function(){
	var ul = this.objBox;
	if(!ul.render){
		ul.render = true;
		var _blankPageList = $(this.base).find('ul .sepoa-preview-lazy-loading');
		if(_blankPageList.length > 0){
			var pageCanvas = _blankPageList[0];
			var li = pageCanvas.parentElement.parentElement;
			
			var showBottom = ul.parentElement.offsetHeight + ul.parentElement.scrollTop;
			var itemPosition = li.parentElement.offsetTop
			
			if(itemPosition < (showBottom + 500)){
				pageCanvas.classList.remove('sepoa-preview-lazy-loading');
				li._options._PDF_DOC.getPage(li._options._PDF_PAGE_IDX)
					.then((pdfPage) => {
						li.style.display = '';
						
						const viewport = pdfPage.getViewport({ scale: 1 });
						const outputScale = pageCanvas.offsetWidth / viewport.width;
						
						pageCanvas.width = Math.floor(viewport.width * outputScale);
						pageCanvas.height = Math.floor(viewport.height * outputScale); 
						pageCanvas.style.width = Math.floor(viewport.width * outputScale) + 'px';
						pageCanvas.style.height = Math.floor(viewport.height * outputScale) + 'px';
						
						const renderContext = {
							canvasContext: pageCanvas.getContext('2d'),
							transform: (outputScale !== 1 ? [outputScale, 0, 0, outputScale, 0, 0] : null),
							viewport: viewport
						};
						
						pdfPage.render(renderContext).promise.then(() => {
							ul.render = false;
							this._lazyLoader();
						});
						
					}).catch((reason) => {
						li.style.display = '';
						
						const viewport = {width : 595.2756, height : 841.8898};
						const outputScale = pageCanvas.offsetWidth / viewport.width;
						
						pageCanvas.width = Math.floor(viewport.width * outputScale);
						pageCanvas.height = Math.floor(viewport.height * outputScale); 
						pageCanvas.style.width = Math.floor(viewport.width * outputScale) + 'px';
						pageCanvas.style.height = Math.floor(viewport.height * outputScale) + 'px';
						
						ul.render = false;
						this._lazyLoader();
					})
			}else{
				ul.render = false;
			}
		}else{
			ul.render = false;
		}
	}
}
SepoaPreview.prototype.updateTotalCount=function(){
	var cnt = this.objBox.children.length;
	if(!this.previewStyleElement){
		this.previewStyleElement = this.base.appendChild($('<style></style>')[0]);
	}
	
	var cssText = '';
	if(this.conf.type){
		for(var i in this.conf.type){
			cssText +='.sepoa-preview-list-item-page.'+this.conf.type[i]+' span::after{content:"'+cnt+'"}\n'
		}
	}else{
		cssText ='.sepoa-preview-list-item-page span::after{content:"'+cnt+'"}\n'
	}
	this.previewStyleElement.innerText = cssText;
}
SepoaPreview.prototype.setImageServlet=function(svl){
	this.conf.IMAGE_SERVLET=svl || "";
}
//Editor
SepoaContractEditor=function(conf, _vp){
	sUtils.modules.extend(this, "EventManager")
	this.conf = $.extend(true, {},conf);
	if(this.conf.language){
		Sepoa.cache.text.load(this.conf.language, false)
	}
	this._rowsAr = {};
	this.base = _vp.appendChild($('<div class="contract-editor-area lowerDiv"></div>')[0]);
	
	this.editorWrapper = document.createElement('div');
	this.editorWrapper.className = 'contract-editor-wrapper';

	this.base.appendChild(this.editorWrapper);
	this.objBox = this.editorWrapper.appendChild($('<ul class="contract-editor-list"></div>')[0]);
	
	this.base.addEventListener('mouseleave', this._onMouseEvent);
}
SepoaContractEditor.prototype.setMappingTable=function(k, v){
	if(typeof(k)=="string"){
		var t = {}
		t[k] = v;
		k = t;
	}
	for(var i in k){
		this.conf.mapping[i] = k[i];
	}
}
SepoaContractEditor.prototype.clear=function(){
	this.objBox.innerHTML='';
	this._rowsAr = {};
	this.objBox.render = false;
	this.beforeData = undefined;
}
SepoaContractEditor.prototype.load=function(data, type, watermark){
	if(type !== "json"){
		data = data.getArrayMap();
	}
	
	var pdfPromiseArray = [];
	for(let i in data){
		pdfPromiseArray.push(
			pdfjsLib.getDocument({
				url: sUtils.string.dataReplacer(this.conf.IMAGE_SERVLET, data[i]),
				cMapUrl : window.cMapUrl ? window.cMapUrl : window.CONTEXT_NAME + '/resource/plugin/pdfjs/cmaps/',
				_data:data[i]
			}).promise
		);
	}
	
	var result = {SIGN_ID_LIST:[]};
    
	if(this.parentObj.items) {
		if(this.parentObj.conf.workType.toUpperCase() == 'T' && !this.parentObj.conf.modify) {
		} else {
			if(this.parentObj.items.sign != null && this.parentObj.items.sign.length > 0){
				this.parentObj.items.seal.removeAll();
			}
		}
	}
    
	var signerItems = $(this.objBox).find('.contract-item-box-signer-data');
	for(var i=0;i<signerItems.length;i++){
		var item = signerItems[i];
		
		for(var j=0;j<item.classList.length;j++){
			var m = item.classList[j].match('contract-item-box-sign-id-([^-]{0,})')
			if(m && (result.SIGN_ID_LIST.indexOf(m[1]) < 0)){
				result.SIGN_ID_LIST.push(m[1]);
			}
		}
	}
    
	result.SIGN_ID_LIST.sort();
	
	Promise.all(pdfPromiseArray).then((pdfs) =>{
		for(let i in pdfs){
			this.add(pdfs[i], watermark)
		}
		this._lazyLoader();
	})
	this.callEvent("onXLE", [result]);
	this.conf.loaded = true;
}
SepoaContractEditor.prototype._lazyLoader=function(){
	var ul = this.objBox;
	if(!ul.render){
		ul.render = true;
		var _blankPageList = $(this.base).find('ul .sepoa-preview-lazy-loading');
		if(_blankPageList.length > 0){
			var pageCanvas = _blankPageList[0];
			var li = pageCanvas.parentElement.parentElement.parentElement;
			
			var showBottom = ul.parentElement.offsetHeight + ul.parentElement.scrollTop;
			var itemPosition = li.parentElement.offsetTop
			
			if(itemPosition < (showBottom + 500)){
				pageCanvas.classList.remove('sepoa-preview-lazy-loading');
				
				li._options._PDF_DOC.getPage(li._options._PDF_PAGE_IDX)
					.then((pdfPage) => {
						this._pdfPageResolve(li, pageCanvas, pdfPage);
					}).catch((reason) => {
						this._pdfPageResolve(li, pageCanvas);
					})
			}else{
				ul.render = false;
			}
		}else{
			ul.render = false;
		}
	}
	
	const isRenderDone = Array.from(this.objBox.querySelectorAll('li')).find(e => e._options['_PDF_PAGE_VIEWPORT'] === undefined) === undefined;
	if(isRenderDone && this.sci) {
		for(let item of this.sci) {
			if(item.conf.ITEM_SOURCE == 'org' && item.conf.ITEM_ITEMKEY){
				item.contractObj.editor.setAttrCloneItem(item);
			}
		}
	}
}
SepoaContractEditor.prototype._pdfPageResolve=function(element, canvas, pdfPage){
	element.style.display = '';
	
	const viewport = element._options._PDF_PAGE_VIEWPORT = pdfPage ? pdfPage.getViewport({ scale: 1 }) : {width : 595.2756, height : 841.8898};
	const outputScale = canvas.offsetWidth / viewport.width;
	canvas.width = Math.floor(viewport.width * outputScale);
	canvas.height = Math.floor(viewport.height * outputScale); 
	canvas.style.width = Math.floor(viewport.width * outputScale) + 'px';
	canvas.style.height = Math.floor(viewport.height * outputScale) + 'px';
	element.editorLayer.style.width = canvas.style.width;
	element.editorLayer.style.height = canvas.style.height;
	
	const renderContext = {
		canvasContext: canvas.getContext('2d'),
		transform: (outputScale !== 1 ? [outputScale, 0, 0, outputScale, 0, 0] : null),
		viewport: viewport
	};
	
	const renderResolve = () => {
		if(this.beforeData){
			this.setData(null, [element]);
		} else {
			this.setData({signer:this.parentObj.items.sign.data.list}, null, true);
		}
		
		const $wrapper =  $(element).find('.contract-editor-list-item-wrapper');
		$wrapper.width($wrapper.find('canvas').width()+2+'px');
		
		const docCmDimensions = {
			width : ((viewport.width * 2.54) / 72).toFixed(1),
			height : ((viewport.height * 2.54) / 72).toFixed(1)
		};
		
		for(let item of element.editorLayer.querySelectorAll('.contract-item-box')) {
			const prefix = 'contract-item-prop-seal_';
			const targetList = Array.from(item.querySelector('textarea').classList).filter(e => e.startsWith(prefix));
			for(let target of targetList) {
				const [prop, value] = target.replace(prefix, '').split('-');
				if(['width', 'height'].includes(prop)) {
					item.style[prop] = (value / docCmDimensions[prop] * 100).toFixed('2') + '%';
				}
			}
			
			item.obj.editor.callEvent('onPropUpdate', []);
		}
		this.objBox.render = false;
		this._lazyLoader();
	};
	
	if(pdfPage) {
		pdfPage.render(renderContext).promise.then(renderResolve);
	} else {
		renderResolve();
	}
}
SepoaContractEditor.prototype.add=function(pdf, watermark){
	let numPages = pdf.numPages;
	const data = pdf._transport._params._data;
	const fixHtml = data.FIX_HTML ? JSON.parse(data.FIX_HTML) : null;
	if(fixHtml) {
		const fixHtmlMaxPage = Math.max(...Object.keys(fixHtml));
		numPages = numPages > fixHtmlMaxPage ? numPages : fixHtmlMaxPage;
	}
	
	for(let pageIdx=1;pageIdx <= numPages;pageIdx++){
		let data = $.extend(true, {}, pdf._transport._params._data);
		
		data._PDF_PAGE_IDX=pageIdx;
		data._PDF_DOC=pdf;
		data.PART_ID = sUtils.string.lpad(pageIdx, 3, '0')
		data.ID = sUtils.string.dataReplacer(this.conf.mapping.ID, data);
		
		let fixHtml = data.FIX_HTML?JSON.parse(data.FIX_HTML):null;
		if(fixHtml && fixHtml[data.PART_ID]){
			data.FIX_HTML = fixHtml[data.PART_ID]
		}else{
			delete data.FIX_HTML;
		}
		
		let idx = this.objBox.children.length + 1;
		
		let sToken = '';
		if(window.sTokenString) {
			sToken = '&s-token=' + window.sTokenString;
		}

		const li = this.addBlankPage(idx);
		if(this._rowsAr[li._options.ID]) {
			delete this._rowsAr[li._options.ID];
		}
		
		this._rowsAr[data.ID] = li;
		li.style.display = 'none';
		li._options = data;
		
		const pdfPage = li.children[0].children[0].insertBefore(sUtils.element.create('<canvas class="contract-editor-list-item contract-editor-list-item-img sepoa-preview-lazy-loading"></canvas>'), li.editorLayer);
		pdfPage.obj = this;
		pdfPage.li = li;
		
		const itemLayer = li.editorLayer;
		itemLayer.parentElement.style.height = '';
		
		if (watermark) {
			this.base.appendChild(sUtils.element.create('<style>.watermark{background-image: url(' + window.CONTEXT_NAME + '/resource/plugin/sepoasoft/contract/icon/watermark.png);	background-size: 86px; background-repeat: repeat; background-position: center;}</style>'));
			itemLayer.classList.add('watermark');
		}
		
		if(data.FIX_HTML){
			var mList = data.FIX_HTML.match(/url\(\"([\S.]{0,})\"\)/g);
			if(mList){
				mList.forEach(function(e) {
					data.FIX_HTML = data.FIX_HTML.replace(e, e.replace(/\"/g, "'"));
				});
			}
			
			li.editorLayer.innerHTML = data.FIX_HTML;
			
			//adding sToken string to image servlet
			if(sToken.length > 0) {
				var servletImageList = $(li.editorLayer).find('img');
				for(var i = 0; i < servletImageList.length; i++) {
					if(servletImageList[i].src.indexOf('SepoaPathToImage') >= 0) {
						servletImageList[i].src = servletImageList[i].src + sToken + '&ran='+new Date().getTime();
					}
				}
			}
			var items = $(li.editorLayer).find('.contract-item.contract-item-box:not(.contract-item-box-source-symmetry)');
			for(var i=0;i<items.length;i++){
				let data = {};
				var item = items[i];
				for(var j=0;j<item.classList.length;j++){
					var m = item.classList[j].match('contract-item-box-([^-]{0,})-([^-]{0,})')
					if(m){
						data["ITEM_"+m[1].toUpperCase()] = m[2];
					}
					
					var f = item.classList[j].match('contract-item-type-([^-]{0,})');
					if(f){
						data["ITEM_TYPE"] = f[1].toUpperCase();
					}
				}
				if(!this.sci) this.sci = [];
				data.contractObj = this.parentObj;
				data = $.extend(true, data, this.conf);
				this.sci.push(new SepoaContractItem(data, item));
				this.setItemIndex(li.editorLayer);
			}
		}
	}
}

SepoaContractEditor.prototype.addBlankPage=function(idx, direction){
	const defaultData = {
		ID : sUtils.string.randomUUID(),
		FILE_ID : '__DUMMY_FILE_ID__',
		PART_ID : sUtils.string.lpad(idx, 3, '0'),
		LAYER : 'CONT_DUMMY'
	};
	
	const page = this._rowsAr[defaultData.ID] = this.objBox.appendChild(sUtils.element.create('<li class="contract-editor-list-li"></li>'));
	page.editor = this;
	page._options = defaultData;
	
	const contents = sUtils.element.create('<div class="contract-editor-list-item contract-editor-list-item-contents"></div>');
	page.appendChild(contents);
	
	const itemWrapper = sUtils.element.create('<div class="contract-editor-list-item contract-editor-list-item-wrapper"></div>');
	contents.appendChild(itemWrapper);
	
	let operator = '*';
	if(direction == 'landscape') {
		operator = '/';
	}
	itemWrapper.style.height = 'calc(' + itemWrapper.clientWidth + 'px' + operator + '1.414)';
	
	const itemLayer = sUtils.element.create('<div class="contract-editor-list-item contract-item-layer"></div>');
	itemWrapper.appendChild(itemLayer);
	page.editorLayer = itemLayer;
	page.editorLayer.contractObj = this.parentObj;
	page.appendChild(sUtils.element.create('<div class="contract-editor-list-item-page"><span>'+idx+' / </span></div>'));
	
	this.initEditorEvent(page.editorLayer);
	this.updateTotalCount();
	
	return page;
}

SepoaContractEditor.prototype.drawAddPageButton = function(direction) {
	//here
	const addPageButton = sUtils.element.create('<div style="display: flex;justify-content: center;"><div style="font-size: 19px;border: 1px solid;width: 80%;display: flex;flex-direction: column;margin: 15px 0;border-style: dashed;"><div style="margin: auto;">+</div><div style="margin: auto;">페이지 추가</div></div></div>');
	
	addPageButton.onclick = () => {
		const idx = this.objBox.children.length + 1;
		this.addBlankPage(idx, direction);
	};
	
	this.editorWrapper.appendChild(addPageButton);
}

SepoaContractEditor.prototype.moveOrgItem=function(item){
	for(var i in this._rowsAr){
	    var li = this._rowsAr[i];
	    var targetItem = $(li).find('.contract-item-box-source-org.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY);
	    if(targetItem.length > 0){
	    	$(this.objBox.parentElement.parentElement).animate({
	    		scrollTop:this._rowsAr[i].offsetTop
	    	}, 100, null)

	    	targetItem.click();
	        break;
	    }
	}
}
SepoaContractEditor.prototype.addSymmetryItem=function(item, type){
	var symConfig = null;
	for(var i in this._rowsAr){
		var li = this._rowsAr[i];
		var orgNode = $(li).find('.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY);
		
		if(orgNode.length > 0){
			//대칭노드 input옵션 파싱
			if(symConfig == null){
				symConfig = {};
				for(var i=0;i<item.input.classList.length;i++){
					var m = item.input.classList[i].match(/^contract-item-prop-([^-]{0,})-([^-]{0,})/)
					if(m){
						symConfig[m[1]] = m[2];
					}
				}
			}
			
			//대칭 노드복제
			var symmetryNode = $(orgNode[0].outerHTML);
			symmetryNode.input = symmetryNode[0].children[0];
			$(li).find('.contract-item-box-source-symmetry.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY).remove();
			symmetryNode.removeClass('contract-item-box-source-org').removeClass('contract-item-box-source-copy').addClass('contract-item-box-source-symmetry');
			li.editorLayer.appendChild(symmetryNode[0]);
			
			//대칭 css설정
			var position = this.conf["symmetry_cut_position"];
			var reverseFlag = ((position == 'LEFT')?-1:1)
			var percent = (parseInt(symConfig['symmetry_cut_percent']) * reverseFlag) + "%";
			
			if(!$(symmetryNode.input).attr('style')){
				symmetryNode.input.setAttribute('style', 'temp;');
			}
			
			var cssArr = $(symmetryNode.input).attr('style').split(';');
			var resArr = [];
			for(var i in cssArr){
				var target = cssArr[i].trim();
				if(target.indexOf("-webkit-transform") == 0 || target.indexOf("transform") == 0 || target.indexOf("temp") == 0 || target.trim().length == 0){
					continue;
				}
				
				resArr.push(target);
			}
			resArr.push("-webkit-transform: translateX("+percent+")");
			resArr.push("transform: translateX("+percent+")");
			$(symmetryNode.input).attr('style', resArr.join(';') + ";");
			//위치지정
			symmetryNode[0].style.left = ((100 - parseInt(symmetryNode[0].style.left)) - ((symmetryNode[0].offsetWidth/symmetryNode[0].parentElement.offsetWidth) * 100).toFixed(2)) + '%'
			
			//대칭 아이템객체 생성을 위한 클래스 파싱
			var data = {};
			for(var j=0;j<symmetryNode[0].classList.length;j++){
				var m = symmetryNode[0].classList[j].match('contract-item-box-([^-]{0,})-([^-]{0,})')
				if(m){
					data["ITEM_"+m[1].toUpperCase()] = m[2];
				}
			}
			
			//대칭객체 생성
			if(!this.sci) this.sci = [];
			data.contractObj = this.parentObj;
			data = $.extend(true, data, this.conf);
			this.sci.push(new SepoaContractItem(data, symmetryNode[0]));
			this.setItemIndex(li.editorLayer)
		} else {
			continue;
		}
	}
}
SepoaContractEditor.prototype.removeSymmetryItem=function(item, type){
	for(var i in this._rowsAr){
		var li = this._rowsAr[i];
		$(li).find('.contract-item-box-source-symmetry.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY).remove();
	}
}
SepoaContractEditor.prototype.addCloneItem=function(item){
	//현재페이지 빼고 다 복제
	for(var i in this._rowsAr){
		var li = this._rowsAr[i];
		if($(li).find('.contract-item-box-source-org.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY).length > 0) continue;
		$(li).find('.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY).remove();
		var cloneNode = $(item.base.outerHTML);
		cloneNode.removeClass('contract-item-box-source-org').addClass('contract-item-box-source-copy');
		li.editorLayer.appendChild(cloneNode[0]);
		
		var data = {};
		for(var j=0;j<cloneNode[0].classList.length;j++){
			var m = cloneNode[0].classList[j].match('contract-item-box-([^-]{0,})-([^-]{0,})')
			if(m){
				data["ITEM_"+m[1].toUpperCase()] = m[2];
			}
		}
		
		if(!this.sciArr) this.sciArr = [];
		data.contractObj = this.parentObj;
		data = $.extend(true, data, this.conf);
		var sci = new SepoaContractItem(data, cloneNode[0]);
		this.sciArr.push(sci);
		this.setItemIndex(li.editorLayer);
		
		if(sci.editor.conf.seal_width && sci.editor.conf.seal_height){
			var w = this.editorSizeInit(i, sci.editor.conf.seal_width, 'w')+"px";
			var h = this.editorSizeInit(i, sci.editor.conf.seal_height, 'h')+"px";
			var rectObj = {w:w, h:h};
			sci.setSize(rectObj);
			sci.setRectSaveType(false);
			sci.setRectSaveType(true);
		}
	}
}
SepoaContractEditor.prototype.removeCloneItem=function(item){
	for(var i in this._rowsAr){
		var li = this._rowsAr[i];
		$('.contract-item-box-source-symmetry.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY).remove();
		$('.contract-item-box-source-copy.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY).remove();
	}
}
SepoaContractEditor.prototype.setAttrCloneItem=function(item){
	for(var i in this._rowsAr){
		var li = this._rowsAr[i];
		var cItem = $(li).find('.contract-item-box-source-copy.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY);
		if(cItem.length > 0){
			cItem[0].style.width  	= ((item.editor.conf.seal_width) ? this.editorSizeInit(i, item.editor.conf.seal_width, 'w')+"px" :item.base.style.width);
			cItem[0].style.height 	= ((item.editor.conf.seal_height) ? this.editorSizeInit(i, item.editor.conf.seal_height, 'h')+"px" :item.base.style.height);
			cItem[0].style.top 		= item.base.style.top;
			cItem[0].style.left 	= item.base.style.left;
			cItem[0].obj.setRectSaveType(false);
			cItem[0].obj.setRectSaveType(true);
		}
		
		var sItem = $(li).find('.contract-item-box-source-symmetry.contract-item-box-itemkey-'+item.conf.ITEM_ITEMKEY);
		if(sItem.length > 0){
			sItem[0].style.width  	= ((item.editor.conf.seal_width) ? this.editorSizeInit(i, item.editor.conf.seal_width, 'w')+"px" :item.base.style.width);
			sItem[0].style.height 	= ((item.editor.conf.seal_height) ? this.editorSizeInit(i, item.editor.conf.seal_height, 'h')+"px" :item.base.style.height);
			sItem[0].style.top 		= item.base.style.top;
			sItem[0].style.left = ((100 - parseInt(item.base.style.left)) - ((sItem[0].offsetWidth/sItem[0].parentElement.offsetWidth) * 100).toFixed(2)) + '%'
			sItem[0].obj.setRectSaveType(false);
			sItem[0].obj.setRectSaveType(true);
		}
	}
}
SepoaContractEditor.prototype.setItemIndex=function(page){
	var itemArr = $(page).find(".contract-item-box");

	for(var i=0; i<itemArr.length; i++){
		var target = itemArr[i];
		target.calcArea = target.offsetWidth * target.offsetHeight;
	}
	
	itemArr.sort(function(a, b){
		return a.calcArea > b.calcArea ? -1 : a.calcArea < b.calcArea ? 1 : 0;
	});
	
	for(var i=0; i<itemArr.length; i++){
		var target = itemArr[i];
		target.style.zIndex = i+1;
	}
}
SepoaContractEditor.prototype.initEditorEvent=function(editorLayer){
	editorLayer.addEventListener('click'     , this._onMouseEvent);
	editorLayer.addEventListener('mouseleave', this._onMouseEvent);
	editorLayer.addEventListener('mousemove' , this._onMouseEvent);
}
SepoaContractEditor.prototype.updateTotalCount=function(){
	var cnt = this.objBox.children.length;
	if(!this.previewStyleElement){
		this.previewStyleElement = this.base.appendChild($('<style></style>')[0]);
	}
	this.previewStyleElement.innerText='.contract-editor-list-item-page span::after{content:"'+cnt+'"}'
}
SepoaContractEditor.prototype.showItem=function(id){
	//$(this.base).animate()로 대체가능
	$(this.objBox.parentElement.parentElement).animate({
		scrollTop:this._rowsAr[id].offsetTop
	}, 100, null)
	
}
SepoaContractEditor.prototype.editorSizeInit=function(id, cm, wh){
	const editorLayerSize = {
		w : this._rowsAr[id].editorLayer.offsetWidth,
		h : this._rowsAr[id].editorLayer.offsetHeight
	};
	
	const viewport = this._rowsAr[id]._options._PDF_PAGE_VIEWPORT ?? {width : 595.2756, height : 841.8898};
	const docCmDimensions = {
		w : ((viewport.width * 2.54) / 72).toFixed(2),
		h : ((viewport.height * 2.54) / 72).toFixed(2)
	};
	
	return editorLayerSize[wh] * (cm / docCmDimensions[wh]);
}
SepoaContractEditor.prototype.addContractItem=function(id, data, helper, pos){
	data = $.extend(true, {}, data);
	if(data.ITEM_NO === null){
		data.ITEM_NO = getDocumentNumber('CI');
	}
	var layer = this._rowsAr[id].editorLayer
	var item = layer.appendChild($('<div class="contract-item contract-item-box contract-item-box-no-'+data.ITEM_NO+(data.ITEM_CODE?(' contract-item-box-code-'+data.ITEM_CODE):'')+'"></div>')[0]);
	var rect={x:pos.x+"px", y:pos.y+"px"}
	
	if(data.ITEM_TYPE == 'SEAL' && data.SEAL_WIDTH && data.SEAL_HEIGHT){
		rect.w = this.editorSizeInit(id, data.SEAL_WIDTH, 'w')+"px";;
		rect.h = this.editorSizeInit(id, data.SEAL_HEIGHT, 'h')+"px";;
	} else if(data.ITEM_TYPE == 'CHECKBOX' || data.ITEM_TYPE == 'MCHECKBOX' || data.ITEM_TYPE == 'NCHECKBOX'){
		// [CHECKBOX 기능 추가] 체크박스 아이템 크기 고정
		rect.w = "16px";
		rect.h = "16px";
	} else {
		rect.w = ($(helper).width())+"px";
		rect.h = ($(helper).height())+"px";
	}
	
	if(data.SIGN_ID != undefined){
		item.classList.add('contract-item-box-signer-data');
		item.classList.add('contract-item-box-sign-id-'+data.SIGN_ID);
	}
	
	if(data.ITEM_TYPE == 'SEAL'){
		data.ITEM_ITEMKEY = this.conf.ITEM_ITEMKEY || new Date().getTime();
		data.ITEM_SOURCE = 'org';
		item.classList.add('contract-item-box-'+data.ITEM_TYPE.toLowerCase()+'-data');
		item.classList.add('contract-item-box-'+data.ITEM_TYPE.toLowerCase()+'-code-'+(data.MANAGE_NUMBER || data.ITEM_NO));
		item.classList.add('contract-item-box-itemkey-'+data.ITEM_ITEMKEY);
		item.classList.add('contract-item-box-source-org');
	}
	
	if(data.SYSTEM == "E"){
		if(data.ITEM_CODE == "SIGNER_COMPANY_NAME"){
			data.ITEM_VALUE = Sepoa.info.COMPANY_NAME_LOC;
		}else if(data.ITEM_CODE == "ADDR"){
			data.ITEM_VALUE = Sepoa.info.COMPANY_ADDR_LOC;
		}else if(data.ITEM_CODE == "OWNER"){
			data.ITEM_VALUE = Sepoa.info.CEO_NAME_LOC;
		}
	}	
	
	
	if(!this.sci) this.sci = [];
	data.contractObj = this.parentObj;
	data = $.extend(true, data, this.conf);
	var sci = new SepoaContractItem(data, item);
	this.setItemIndex(layer);
	this.sci.push(sci);

	if(sci.editor.conf.value) {
		delete rect.w;
		delete rect.h;
	}
	sci.setSize(rect);
	sci.setRectSaveType(false);
	sci.setRectSaveType(true);
	
	// [CHECKBOX/MTEXT] 체크박스·서명자 입력란은 자동 값(ITEM_NAME) 설정 제외
	if(this.parentObj.conf.workType != 'T' && data.ITEM_TYPE != "SEAL" && data.ITEM_TYPE != "CHECKBOX" && data.ITEM_TYPE != "MCHECKBOX" && data.ITEM_TYPE != "NCHECKBOX" && data.ITEM_TYPE != "MTEXT" && data.ITEM_TYPE != "NTEXT"){
		sci.editor.setProperties({value : sci.conf.ITEM_VALUE || sci.conf[sci.conf.ITEM_CODE] || sci.conf.ITEM_NAME});
	}

	if(data.ITEM_TYPE == 'SEAL'){
		sci.base.children[4].style.backgroundImage = $(helper).css('background-image');
		sci.editor.setProperties({seal_width:data.SEAL_WIDTH, seal_height:data.SEAL_HEIGHT});
	}

	if(data.ITEM_TYPE == 'IMAGE') {
		var itemCover = item.querySelector('.contract-item-cover');
		var itemImage = itemCover.appendChild($('<img class="contract-item-cover-image">')[0]);
		itemImage.src = window.CONTEXT_NAME + '/SepoaImage.sux?mode=CUSTOM&METHOD=doImage&PROCESS_ID=sepoa.svc.contract.SepoaContractItem';
		itemImage.src += '&ATTACH_TYPE=' + data.ATTACH_TYPE + '&DES_FILE_PATH=' + data.DES_FILE_PATH;
		itemImage.style.width = '100%'
		itemImage.style.height = '100%'
		itemImage.style.objectFit = 'contain';
		itemImage.ondragstart = function(e) {
			return false;
		}
	}

	// [CHECKBOX 기능 추가] 체크박스 아이템 초기화
	if(data.ITEM_TYPE == 'CHECKBOX' || data.ITEM_TYPE == 'MCHECKBOX' || data.ITEM_TYPE == 'NCHECKBOX') {
		// 체크박스는 텍스트 입력 없이 흰색 네모박스만 표시
		var itemInput = item.querySelector('.contract-item-input');
		if(itemInput) {
			itemInput.style.display = 'none';
		}
		// 체크박스 상태 저장을 위한 속성 추가
		data.CHECKBOX_CHECKED = false;
	}

	var resizeable = this.parentObj.conf.module.items.item.resizeable;
	for(var i in resizeable){
		if(data.ITEM_TYPE.toUpperCase() == i.toUpperCase() && !resizeable[i]){
			this.parentObj.hideResize();
		}
	}
}
SepoaContractEditor.prototype._onMouseEvent=function(e){
	var src = e.srcElement;
	var active = $(this).find('.contract-item-active')[0];
	if(active){
		active.obj['_on'+e.type.charAt(0).toUpperCase()+e.type.slice(1)](e)
	}else{
		if(e.type == "click"){
			var cl = src.classList;
			if(cl.contains('contract-editor-list-item') || (this.contractObj.helper && this.contractObj.helper.status)){
				var w = $(src.parentElement).closest('.contract-editor-list-item-wrapper');
				if(w){
					var li = $(src.parentElement).closest('.contract-editor-list-li')[0];
					li.editor.callEvent('onItemClick', [e, li._options.ID, li._options])
				}
			}
		}
	}
}
SepoaContractEditor.prototype.setImageServlet=function(svl){
	this.conf.IMAGE_SERVLET=svl || "";
}
SepoaContractEditor.prototype.setData=function(data, li, force){
	if(force || !this.beforeData){
		var p = $.extend(true, {}, data.data);
		for(var i in data.signer){
			var s = data.signer[i]
			for(var j in s){
				p["SIGN_ITEM_"+j.toUpperCase()+"_"+s.SIGNER_NO] = s[j];
			}
		}
		this.beforeData = p;
	}
	var p = this.beforeData;
	var target = li || this._rowsAr;
	for(var i in target){
		var li = target[i];
		var items = $(li.editorLayer).find('.contract-item.contract-item-box');
		for(var i=0;i<items.length;i++){
			var item = items[i];
			if(p[item.obj.conf.ITEM_CODE] != undefined){
				item.obj.editor.setProperties({value:p[item.obj.conf.ITEM_CODE]}, true)
			}else{
				//item.obj.input.placeholder=""
			}
		}
	}
}
SepoaContractEditor.prototype.setMode=function(mode){
	if(this._mode){
		this.base.classList.remove("contract-editor-mode-"+this._mode);
	}
	mode = mode.toLowerCase();
	this.base.classList.add("contract-editor-mode-"+mode);
	this._mode = mode;
}
SepoaContractEditor.prototype.setWatermark=function(text){
	if(this._watermark){
		this._watermark.parentElement.removeChild(this._watermark);
	}
	if(text){
		this.base.classList.add("contract-editor-watermark-show");
		this._watermark = this.base.appendChild($('<style>.contract-editor-area.contract-editor-watermark-show .contract-item-layer:after{content:"'+text+'"}</style>')[0]);
	}else{
		this.base.classList.remove("contract-editor-watermark-show");
	}
}
SepoaContractEditor.prototype.setSignFilter=function(signerNo){
	if(!this.signFilter){
		this.signFilter = this.base.appendChild($('<style></style>')[0]);
	}
	// 서명 모드: 해당 서명자 아이템(서명/체크박스/텍스트·개인정보 입력 등) 표시
	// SIGN: contract-item-box-code-SIGN_ITEM_SIGN_ + signerNo
	// 서명자 바인딩 전체: contract-item-box-sign-id- + signerNo
	this.signFilter.innerText='.contract-editor-area.contract-editor-mode-sign .contract-item-box.contract-item-box-code-SIGN_ITEM_SIGN_'+signerNo+'{ display:inline-flex }\n'+
		'.contract-editor-area.contract-editor-mode-sign .contract-item-box.contract-item-box-sign-id-'+signerNo+'{ display:inline-flex }\n'+
		'.contract-editor-area.contract-editor-mode-sign .contract-item-box.contract-item-box-sign-id-'+signerNo+' .contract-item-se-resize,\n'+
		'.contract-editor-area.contract-editor-mode-sign .contract-item-box.contract-item-box-sign-id-'+signerNo+' .contract-item-delete,\n'+
		'.contract-editor-area.contract-editor-mode-sign .contract-item-box.contract-item-box-code-SIGN_ITEM_SIGN_'+signerNo+' .contract-item-se-resize,\n'+
		'.contract-editor-area.contract-editor-mode-sign .contract-item-box.contract-item-box-code-SIGN_ITEM_SIGN_'+signerNo+' .contract-item-delete{ display:none !important; }'
}
SepoaContractEditor.prototype.getHtml=function(flag){
	var htmls = [];
	//IE
	for(var i in this._rowsAr){
		var li = this._rowsAr[i];
		
		var clone = $(li.editorLayer).clone();
		if(flag === true){
			clone.html('');

			var digitalSignItem = li.querySelectorAll('.contract-item-box-code-SIGN_ITEM_SIGN_' + this.parentObj._signerNo + '.digital-sign-selector-container');
			digitalSignItem.forEach(function(el){
				$(el.obj.extractDigitalSign()).clone().appendTo(clone);
			});
			
			// [CHECKBOX 기능 추가] 체크박스 아이템 처리: extractCheckbox 메서드 사용 (디지털 서명과 동일한 패턴)
			// 현재 서명자의 체크박스만 처리 (SIGN_ID 또는 ITEM_CODE에서 서명자 ID 추출)
			if(this.sci){
				for(var sciIdx = 0; sciIdx < this.sci.length; sciIdx++){
					var sciItem = this.sci[sciIdx];
					// 해당 페이지의 체크박스 아이템인지 확인
					if(sciItem.base && sciItem.base.parentElement == li.editorLayer){
						if(sciItem.conf && (sciItem.conf.ITEM_TYPE == 'CHECKBOX' || sciItem.conf.ITEM_TYPE == 'MCHECKBOX' || sciItem.conf.ITEM_TYPE == 'NCHECKBOX')){
							// 현재 서명자의 체크박스인지 확인
							var signerIdFromItemCode = null;
							if(sciItem.conf.ITEM_CODE && sciItem.conf.ITEM_CODE.indexOf('SIGN_ITEM_CHECKBOX_') === 0){
								signerIdFromItemCode = sciItem.conf.ITEM_CODE.replace('SIGN_ITEM_CHECKBOX_', '');
							} else if(sciItem.conf.ITEM_CODE && sciItem.conf.ITEM_CODE.indexOf('SIGN_ITEM_MCHECKBOX_') === 0){
								signerIdFromItemCode = sciItem.conf.ITEM_CODE.replace('SIGN_ITEM_MCHECKBOX_', '');
							} else if(sciItem.conf.ITEM_CODE && sciItem.conf.ITEM_CODE.indexOf('SIGN_ITEM_NCHECKBOX_') === 0){
								signerIdFromItemCode = sciItem.conf.ITEM_CODE.replace('SIGN_ITEM_NCHECKBOX_', '');
							}
							// 현재 서명자의 체크박스만 처리
							if(signerIdFromItemCode == this.parentObj._signerNo || sciItem.conf.SIGN_ID == this.parentObj._signerNo){
								// extractCheckbox 메서드로 체크 상태가 반영된 HTML 추출
								var checkboxElement = sciItem.extractCheckbox();
								if(checkboxElement){
									$(checkboxElement).clone().appendTo(clone);
								}
							}
						} else if(sciItem.conf.ITEM_TYPE && ['SIGN','SEAL','IMAGE'].indexOf(String(sciItem.conf.ITEM_TYPE).toUpperCase()) < 0){
							// 서명자 텍스트/개인정보 입력 필드 추출
							if(sciItem.conf.SIGN_ID == this.parentObj._signerNo || (sciItem.conf.ITEM_CODE && String(sciItem.conf.ITEM_CODE).endsWith('_' + this.parentObj._signerNo))){
								var textElement = sciItem.extractSignerTextField && sciItem.extractSignerTextField();
								if(textElement){
									$(textElement).appendTo(clone);
								}
							}
						}
					}
				}
			}
		}else{
			var items = clone.find('textarea');
			for(var i=0;i<items.length;i++){
				var placeholder = items[i].getAttribute('placeholder');
				var innerHTML = items[i].innerHTML;
				/*
				if(placeholder == innerHTML){
					items[i].innerHTML = "";
				}
				*/
			}
		}
		// [체크박스 검증 하이라이트 제거] HTML 추출 시 검증 실패 하이라이트 클래스 제거
		$(clone).find('.contract-item-validation-failed').removeClass('contract-item-validation-failed');
		
		var html = clone[0].innerHTML
		if(html){
			var obj = {FIX_HTML:html};
			for(var k in {FILE_ID:1, PART_ID:1, LAYER:1}){
				obj[k] = li._options[k];
			}
			htmls.push(obj);
		}
	}
	return htmls;
}
/* contract item */
SepoaContractItem=function(conf, base){
	dhtmlx.extend(this, dhtmlx.EventSystem)
	this.conf = $.extend(true, {}, conf)
	this.base = base;
	this.contractObj = conf.contractObj;
	this.base.obj = this;
	this.status={
		draggable:false,
		resizable:false
	}
	if(this.base.children.length <= 0){
		this.renderBase();
	}else{
		this.input = $(this.base).find('.contract-item.contract-item-input')[0];
		this.prefix = $(this.base).find('.contract-item.contract-item-input-prefix')[0];
		this.suffix = $(this.base).find('.contract-item.contract-item-input-suffix')[0];
		this.origin = $(this.base).find('.contract-item.contract-item-input-origin')[0];
		// [CHECKBOX 기능 추가] 기존 HTML에서 로드된 체크박스 아이템의 경우 checkboxMark 찾기
		if(this.conf.ITEM_TYPE == 'CHECKBOX'){
			this.checkboxMark = $(this.base).find('.contract-checkbox-mark')[0];
			// checkboxMark가 없으면 생성 (기존 HTML에 없을 수 있음)
			if(!this.checkboxMark){
				this.checkboxMark = this.base.appendChild($('<div class="contract-checkbox-mark" style="display:none;"></div>')[0]);
			}
			// 초기 체크 상태 설정
			this.conf.CHECKBOX_CHECKED = this.conf.CHECKBOX_CHECKED || false;
			this.updateCheckboxDisplay();
		} else if(this.conf.ITEM_TYPE == 'MCHECKBOX'){
			this.mcheckboxMark = $(this.base).find('.contract-mcheckbox-mark')[0];
			// mcheckboxMark가 없으면 생성 (기존 HTML에 없을 수 있음)
			if(!this.mcheckboxMark){
				this.mcheckboxMark = this.base.appendChild($('<div class="contract-mcheckbox-mark" style="display:none;"></div>')[0]);
			}
			// 초기 체크 상태 설정
			this.conf.CHECKBOX_CHECKED = this.conf.CHECKBOX_CHECKED || false;
			this.updateCheckboxDisplay();
		} else if(this.conf.ITEM_TYPE == 'NCHECKBOX'){
			this.ncheckboxMark = $(this.base).find('.contract-ncheckbox-mark')[0];
			// ncheckboxMark가 없으면 생성 (기존 HTML에 없을 수 있음)
			if(!this.ncheckboxMark){
				this.ncheckboxMark = this.base.appendChild($('<div class="contract-ncheckbox-mark" style="display:none;"></div>')[0]);
			}
			// 초기 체크 상태 설정
			this.conf.CHECKBOX_CHECKED = this.conf.CHECKBOX_CHECKED || false;
			this.updateCheckboxDisplay();
		}
	}
	this.initEvent();
	this.initEditor();
	this.setRectSaveType(true);
	
	this.callEvent('onInit', []);
}
SepoaContractItem.prototype.initEditor=function(){
	this.editor = new SepoaContractItemEditor(this);
	this.editor.attachEvent('onPropUpdate', function(){
		if(this.item.conf.ITEM_SOURCE != 'copy' && this.item.conf.ITEM_SOURCE != 'symmetry'){
			var rem = [];
			for(var i=0;i<this.item.input.classList.length;i++){
				if(this.item.input.classList[i].indexOf('contract-item-type-') == 0 || this.item.input.classList[i].indexOf('contract-item-prop-') == 0){
					rem.push(this.item.input.classList[i]);
				}
			}
			for (var i in rem){
				this.item.input.classList.remove(rem[i]); 
			}
			
			var extendObj = {}

			for(var i in this.conf){
				if(i == "font_size"){
					this.item.input.classList.add("contract-item-prop-"+i+"-"+this.conf.font_size);
					this.item.input.style.fontSize = (this.conf.font_size / 1000) + "em"
				} else if(["value", "prefix", "suffix"].indexOf(i) < 0){
					this.item.input.classList.add("contract-item-prop-"+i+"-"+this.conf[i]);
					if(i == "cut_percent"){
						position = this.conf["cut_position"];
						var reverseFlag = ((position == 'LEFT')?-1:1)
						var percent = (parseInt(this.conf[i]) * reverseFlag) + "%";
						
						if(!$(this.item.input).attr('style')){
							this.item.input.setAttribute('style', 'temp;');
						}
						
						var cssArr = $(this.item.input).attr('style').split(';');
						var resArr = [];
						for(var i in cssArr){
							var target = cssArr[i].trim();
							if(target.indexOf("-webkit-transform") == 0 || target.indexOf("transform") == 0 || target.indexOf("temp") == 0 || target.trim().length == 0){
								continue;
							}
							
							resArr.push(target);
						}
						resArr.push("-webkit-transform: translateX("+percent+")");
						resArr.push("transform: translateX("+percent+")");
						$(this.item.input).attr('style', resArr.join(';') + ";");
					}else if (i == 'seal_width' || i == 'seal_height'){
						$(this.base).find(".contract-item-editor-prop-line.contract-item-editor-prop-type-"+i+" input").val(this.conf[i])
					}else if(i == 'repeat'){
						extendObj['repeat'] = this.conf[i];
					}else if(i == 'symmetry'){
						extendObj['symmetry'] = this.conf[i];
					}else if(i == 'seal_template'){
						$(this.base).find(".contract-item-editor-prop-type-seal_template select").val(this.conf[i]);
					}
				}else if(i == "prefix" || i == "suffix"){
					var target = this.item[i];
					if(!target){
						target = $('<div class="contract-item contract-item-input-'+i+'" style="display:none;"></div>')[0]; 
						if(i == "prefix"){
							this.item.prefix = this.item.base.insertBefore(target, this.item.input)
						}else{
							this.item.suffix = this.item.base.insertBefore(target, this.item.input.nextSibling)
						}
					}
					target.innerHTML = this.conf[i];
					
				}
			}
			
			// [CHECKBOX 기능 추가] 체크박스는 자동 크기 조정 제외
			if(['SEAL', 'IMAGE', 'CHECKBOX'].indexOf(this.item.conf.ITEM_TYPE) < 0) {
				var prefix = (this.item.prefix)?this.item.prefix.innerHTML:"";
				var suffix = (this.item.suffix)?this.item.suffix.innerHTML:"";
				
				var holder = this.item.originPlaceHolder;
				if(!holder){
					holder = this.item.originPlaceHolder = this.item.input.getAttribute('placeholder'); 
				}
				this.item.input.setAttribute('placeholder', prefix + holder + suffix);
				
				if(!this.item.origin){
					this.item.origin = $('<div class="contract-item contract-item-input-origin" style="display:none;"></div>')[0]
				}
				
				var v = this.item.origin.innerHTML = (this.conf.value || "").trim();
				if(v){
					if(this.conf.number_format){
						if(this.conf.number_format == "PRICE"){
							v = sUtils.number.price(v);
						}else{
							v = sUtils.number.format(v, this.conf.number_format);
						}
					}else if(this.conf.date_format){
						v = sUtils.date.toString(sUtils.date.fromString(v, '%Y%m%d'), this.conf.date_format.replaceAll("_", " "));
					}
					v = prefix + v + suffix;
				}
				this.item.input.innerHTML = v;
				
				this.item.setSize({
					h:"1%",
					w:"1%"
				})
				this.item.setSize({
					h:(((this.item.input.scrollHeight + 4) / this.item.base.parentElement.offsetHeight ) * 100).toFixed(2) + "%",
					w:(((this.item.input.scrollWidth + 15)  / this.item.base.parentElement.offsetWidth) * 100).toFixed(2) + "%"
				})
				
			} else {
				for(var i in extendObj){
					if(this.item.contractObj.editor.conf.loaded){
						if(i == 'repeat'){
							if(extendObj[i] == 'ON'){
								this.item.contractObj.editor.addCloneItem(this.item);
							} else{
								this.item.contractObj.editor.removeCloneItem(this.item);
							}
						} else if(i == 'symmetry'){
							if(extendObj[i] == 'ON'){
								$(this.base).find('.contract-item-editor-prop-type-symmetry_page_copy select').removeAttr('disabled');
								this.item.contractObj.editor.addSymmetryItem(this.item);
							} else{
								this.conf['symmetry_page_copy'] = "OFF";
								this.item.input.classList.remove('contract-item-prop-symmetry_page_copy-ON');
								this.item.input.classList.add('contract-item-prop-symmetry_page_copy-OFF');
								$(this.base).find(".contract-item-editor-prop-type-symmetry_page_copy select").val("OFF")
								$(this.base).find('.contract-item-editor-prop-type-symmetry_page_copy select').attr('disabled', '');
								this.item.contractObj.editor.removeSymmetryItem(this.item);
							}
						}
					}
				}
			}
			
			// 속성 편집기 값을 아이템 conf에 동기화 (서명 검증/추출용)
			this.item.conf.value = this.conf.value;
			this.item.contractObj.callEvent('onItemChange', [this.item.conf.ITEM_NO, (this.conf.value || ""), this.conf]);
		}
	})
	this.editor.callEvent('onPropUpdate', []);
}
SepoaContractItem.prototype.setSize=function(r){
	if(r.x) this.base.style.left = r.x;
	if(r.y) this.base.style.top = r.y;
	if(r.w) this.base.style.width = r.w;
	if(r.h) this.base.style.height = r.h;
}
SepoaContractItem.prototype.renderBase=function(){
	this.base.classList.add('contract-item-type-'+this.conf.ITEM_TYPE.toLowerCase())
	
	// [CHECKBOX 기능 추가] 체크박스 아이템 전용 렌더링
	if(this.conf.ITEM_TYPE == 'CHECKBOX'){
		this.input = this.base.appendChild($('<textarea class="contract-item contract-item-input" style="display:none;"></textarea>')[0]);
		this.checkboxMark = this.base.appendChild($('<div class="contract-checkbox-mark" style="display:none;"></div>')[0]);
		this.conf.CHECKBOX_CHECKED = this.conf.CHECKBOX_CHECKED || false;
		this.origin = this.base.appendChild($('<div class="contract-item contract-item-input-origin" style="display:none;"></div>')[0]);
		this.suffix = this.base.appendChild($('<div class="contract-item contract-item-input-suffix" style="display:none;"></div>')[0]);
		this.prefix = this.base.appendChild($('<div class="contract-item contract-item-input-prefix" style="display:none;"></div>')[0]);
		this.base.appendChild($('<div class="contract-item contract-item-cover"></div>')[0]);
		this.base.appendChild($('<div class="contract-item-se-resize"></div>')[0]);
		this.base.appendChild($('<div class="contract-item-delete"></div>')[0]);
	} 
	else if(this.conf.ITEM_TYPE == 'MCHECKBOX'){
		this.input = this.base.appendChild($('<textarea class="contract-item contract-item-input" style="display:none;"></textarea>')[0]);
		this.mcheckboxMark = this.base.appendChild($('<div class="contract-mcheckbox-mark" style="display:none;"></div>')[0]);
		this.conf.CHECKBOX_CHECKED = this.conf.CHECKBOX_CHECKED || false;
		this.origin = this.base.appendChild($('<div class="contract-item contract-item-input-origin" style="display:none;"></div>')[0]);
		this.suffix = this.base.appendChild($('<div class="contract-item contract-item-input-suffix" style="display:none;"></div>')[0]);
		this.prefix = this.base.appendChild($('<div class="contract-item contract-item-input-prefix" style="display:none;"></div>')[0]);
		this.base.appendChild($('<div class="contract-item contract-item-cover"></div>')[0]);
		this.base.appendChild($('<div class="contract-item-se-resize"></div>')[0]);
		this.base.appendChild($('<div class="contract-item-delete"></div>')[0]);
	} else if(this.conf.ITEM_TYPE == 'NCHECKBOX'){
		this.input = this.base.appendChild($('<textarea class="contract-item contract-item-input" style="display:none;"></textarea>')[0]);
		this.ncheckboxMark = this.base.appendChild($('<div class="contract-ncheckbox-mark" style="display:none;"></div>')[0]);
		this.conf.CHECKBOX_CHECKED = this.conf.CHECKBOX_CHECKED || false;
		this.origin = this.base.appendChild($('<div class="contract-item contract-item-input-origin" style="display:none;"></div>')[0]);
		this.suffix = this.base.appendChild($('<div class="contract-item contract-item-input-suffix" style="display:none;"></div>')[0]);
		this.prefix = this.base.appendChild($('<div class="contract-item contract-item-input-prefix" style="display:none;"></div>')[0]);
		this.base.appendChild($('<div class="contract-item contract-item-cover"></div>')[0]);
		this.base.appendChild($('<div class="contract-item-se-resize"></div>')[0]);
		this.base.appendChild($('<div class="contract-item-delete"></div>')[0]);
	}else {
		this.input = this.base.appendChild($('<textarea class="contract-item contract-item-input" readonly wrap="off" placeholder="'+(this.conf.CONTENTS || this.conf.ITEM_NAME)+'">'+(this.conf.ITEM_VALUE || '')+'</textarea>')[0]);
		this.origin = this.base.appendChild($('<div class="contract-item contract-item-input-origin" style="display:none;"></div>')[0]);
		this.suffix = this.base.appendChild($('<div class="contract-item contract-item-input-suffix" style="display:none;"></div>')[0]);
		this.prefix = this.base.appendChild($('<div class="contract-item contract-item-input-prefix" style="display:none;"></div>')[0]);
		this.base.appendChild($('<div class="contract-item contract-item-cover"></div>')[0]);
		this.base.appendChild($('<div class="contract-item-se-resize"></div>')[0]);
		this.base.appendChild($('<div class="contract-item-delete"></div>')[0]);
	}
}
SepoaContractItem.prototype.setRectSaveType=function(flag){
	if((flag === true) && !this.base.classList.contains('contract-item-unit-save-ready')){
		this.setSize({
			x:((this.base.offsetLeft / this.base.parentElement.offsetWidth ) * 100).toFixed(2) + "%",
			y:((this.base.offsetTop  / this.base.parentElement.offsetHeight) * 100).toFixed(2) + "%",
			w:(($(this.base).width()  / this.base.parentElement.offsetWidth) * 100).toFixed(2) + "%",
			h:(($(this.base).height() / this.base.parentElement.offsetHeight ) * 100).toFixed(2) + "%"
		})
		this.base.classList.add('contract-item-unit-save-ready');
	}else if(flag === false && this.base.classList.contains('contract-item-unit-save-ready')){
		this.setSize({
			x:parseInt($(this.base).css('left').replace(/[^0-9\.]/g, ""))+"px",
			y:parseInt($(this.base).css('top').replace(/[^0-9\.]/g, ""))+"px",
			w:parseInt($(this.base).css('width').replace(/[^0-9\.]/g, ""))+"px",
			h:parseInt($(this.base).css('height').replace(/[^0-9\.]/g, ""))+"px"
		})
		this.base.classList.remove('contract-item-unit-save-ready');
	}
}
SepoaContractItem.prototype.initEvent=function(){
	if(this.contractObj.editor._mode == 'sign' && this.conf.ITEM_CODE == 'SIGN_ITEM_SIGN_' + this.contractObj._signerNo) {
		this.base.addEventListener('click' , this._onSignItemClick.bind(this));
	}
	
	if(this.conf.view) return false;
	this.base.addEventListener('mousedown' , function(a){ return function(){ return function(e){ this._onMousedown(e) }.apply(a, arguments) }}(this));
	this.base.addEventListener('mousemove' , function(a){ return function(){ return function(e){ this._onMousemove(e) }.apply(a, arguments) }}(this));
	this.base.addEventListener('click'     , function(a){ return function(){ return function(e){ this._onClick(e) }.apply(a, arguments) }}(this));
	this.base.addEventListener('mouseleave', function(a){ return function(){ return function(e){ this._onMouseleave2(e) }.apply(a, arguments) }}(this));
}
SepoaContractItem.prototype._onClick=function(e){
	if(this.contractObj.helper && this.contractObj.helper.status){
		return true;
	}
	// [CHECKBOX 기능 추가] 서명 모드에서 체크박스 클릭 처리
	// 서명 모드이고 체크박스 아이템이며, 현재 서명자의 체크박스인지 검증
	// ITEM_CODE 형식: "SIGN_ITEM_CHECKBOX_SI251231000023" -> "SI251231000023" 추출
	var signerIdFromItemCode = null;
	if(this.conf.ITEM_CODE && this.conf.ITEM_CODE.indexOf('SIGN_ITEM_CHECKBOX_') === 0){
		signerIdFromItemCode = this.conf.ITEM_CODE.replace('SIGN_ITEM_CHECKBOX_', '');
	} else if(this.conf.ITEM_CODE && this.conf.ITEM_CODE.indexOf('SIGN_ITEM_MCHECKBOX_') === 0){
		signerIdFromItemCode = this.conf.ITEM_CODE.replace('SIGN_ITEM_MCHECKBOX_', '');
	} else if(this.conf.ITEM_CODE && this.conf.ITEM_CODE.indexOf('SIGN_ITEM_NCHECKBOX_') === 0){
		signerIdFromItemCode = this.conf.ITEM_CODE.replace('SIGN_ITEM_NCHECKBOX_', '');
	}
	if(this.contractObj.editor._mode == 'sign' && (this.conf.ITEM_TYPE == 'CHECKBOX' || this.conf.ITEM_TYPE == 'MCHECKBOX' || this.conf.ITEM_TYPE == 'NCHECKBOX') && signerIdFromItemCode == this.contractObj._signerNo){
		if(e.target && e.target.classList.contains('contract-item-delete')){
			return;
		}
		
		// [CHECKBOX 기능 추가] 드래그 모드가 활성화되어 있으면 해제
		if(this.status.draggable){
			this.status.draggable = false;
			this.base.classList.remove('contract-item-active');
			$(this.base).closest('.contract-item-layer')[0].classList.remove('contract-item-draggable');
		}
		if(this._mouseDownEventDelay){
			clearTimeout(this._mouseDownEventDelay);
			this._mouseDownEventDelay = null;
		}
		
		this.conf.CHECKBOX_CHECKED = !this.conf.CHECKBOX_CHECKED;
		this.updateCheckboxDisplay();
		
		this.contractObj.callEvent('onItemChange', [
			this.conf.ITEM_NO, 
			(this.conf.CHECKBOX_CHECKED ? 'Y' : 'N'), 
			this.conf
		]);
		
		// [CHECKBOX 기능 추가] stopPropagation 제거: editorLayer의 _onMouseEvent가 호출되도록 함
		// e.stopPropagation();
		e.preventDefault();
		return;
	}
	if(e.srcElement.classList.contains('contract-item-delete')){
		if(this.conf.ITEM_SOURCE && this.conf.ITEM_ITEMKEY){
			if(this.conf.ITEM_SOURCE == 'org'){
				var that = this;
				Confirm(Sepoa.cache.text.get('POA_CONTRACT_EDITOR.CLONE_ITEM_DELETE_WARNING'), function(flag){
					if(flag){
						that.contractObj.editor.removeCloneItem(that);
						that.remove();
					} else {
						return false;
					}
				})
			} 
		} else {
			this.remove();
			this.editor.hide();
		}
	}else{
		var type = null
		if(this.status.draggable){
			type = "draggable"
		}else if(this.status.resizeable){
			type = "resizeable"
		}
		if(type != null){
			this.setRectSaveType(true);
			this.status[type]=false;
			this.base.classList.remove('contract-item-active')
			$(this.base).closest('.contract-item-layer')[0].classList.remove('contract-item-'+type)
			e.stopPropagation();
			e.preventDefault();
			e.stopImmediatePropagation();
		}else{
			if(this._mouseDownEventDelay){
				clearTimeout(this._mouseDownEventDelay)
			}
			if(this.editor.status){
				this.editor.hide();
				e.stopPropagation();
				e.preventDefault();
				e.stopImmediatePropagation();
			}else{
				this.editor.show();
			}
			
		}
	}
}
// 서명관리(라이브러리 재사용) 없이, 서명 시마다 생성하여 적용
SepoaContractItem.prototype._onSignItemClick=function(e){
	this.openSignSetter();
}
SepoaContractItem.prototype._buildDigitalSignImageUrl=function(row){
	var result = '/SepoaImage.sux?mode=CUSTOM&METHOD=doImage&PROCESS_ID=sepoa.svc.contract.SepoaContractDigitalSign';
	result += '&ROOT_PATH_TYPE=' + row.ROOT_PATH_TYPE + '&DES_FILE_NAME=' + row.DES_FILE_NAME + '&SRC_FILE_NAME=' + row.SRC_FILE_NAME;
	if(window.sTokenString) {
		result += '&s-token=' + window.sTokenString;
	}
	return result;
}
SepoaContractItem.prototype._applySavedDigitalSign=function(){
	doServiceAjax('sepoa.svc.contract.SepoaContractDigitalSign', 'CONNECTION', 'doQuery', '', function(so) {
		if(!so || !so.result || !so.result[0]) {
			return;
		}
		var sf = new SepoaFormater(so.result[0]);
		if(sf.getRowCount() <= 0) {
			return;
		}
		var rows = sf.getArrayMap();
		// 계약 서명 생성 시 SIGN_SEQ=1 슬롯을 덮어쓰므로, 적용도 1번을 우선 사용
		var target = rows.find(function(row) {
			return String(row.SIGN_SEQ) === '1';
		}) || rows[rows.length - 1];
		var imageUrl = this._buildDigitalSignImageUrl(target);

		// 이미 배치된 서명이면 이미지만 교체
		if(this.base && this.base.classList.contains('digital-sign-selector-container') && this.signImageArea) {
			this.signImageArea.setAttribute('src', imageUrl);
			return;
		}

		this.selectedSignImageIndex = 0;
		this.signImageArr = [imageUrl];

		var container = $('<div class="contract-item contract-item-box-code-SIGN_ITEM_SIGN_' + this.contractObj._signerNo + ' digital-sign-selector-container" style="position:absolute"></div>')[0];
		this.base.parentElement.appendChild(container);

		var selectorHeader = container.selectorHeader = container.appendChild($('<div class="digital-sign-selector-header"></div>')[0]);

		const createIcon = selectorHeader.appendChild($('<img class="digital-sign-selector-header-create-icon digital-sign-selector-header-icon"></img>')[0]);
		createIcon.setAttribute('src', '/resource/plugin/sepoasoft/contract/icon/sign.png');
		createIcon.onclick = (e) => {
			this.openSignSetter();
		};

		var selectorBody = container.selectorBody = container.appendChild($('<div class="digital-sign-selector-body" style="display:flex;box-sizing:border-box;"></div>')[0]);
		selectorBody.style.borderWidth = '0 2px 2px';
		selectorBody.style.borderStyle = 'solid';
		selectorBody.style.borderColor = '#a79f9f';
		selectorBody.style.width = this.base.offsetWidth + 'px';
		selectorBody.style.height = this.base.offsetHeight + 'px';

		var signImageArea = this.signImageArea = $('<img class="digital-sign-selector-body-image" style="margin:auto;"></img>')[0];
		signImageArea.setAttribute('src', imageUrl);
		signImageArea.addEventListener('load', function() {
			if(container.clientWidth > container.clientHeight) {
				signImageArea.style.height = 'inherit';
			} else {
				signImageArea.style.width = 'inherit';
			}
			selectorBody.appendChild(signImageArea);
		});

		container.style.left = this.base.style.left;
		container.style.top = (((this.base.offsetTop - selectorHeader.offsetHeight) / this.base.parentElement.offsetHeight) * 100).toFixed(2) + '%';
		container.style.width  = ((this.base.clientWidth  / this.base.parentElement.offsetWidth ) * 100).toFixed(2) + '%';
		container.style.height = (((this.base.offsetHeight + selectorHeader.offsetHeight) / this.base.parentElement.offsetHeight) * 100).toFixed(2) + '%';

		this.base.remove();
		this.base = container;
		this.base.obj = this;

		$(container).draggable({
			scroll: true,
			scrollSensitivity: 100,
			scrollSpeed: 5,
			containment: $(container.parentElement),
			snap: "parent",
			opacity: 0.7,
			stack: ".ui-draggable",
			cursor: "all-scroll",
			cancel:'.ui-resizable-handle , .range, .range-result, .digital-sign-resize',
			distance: 10,
			delay:150,
			start : function(e, ui){
				this.style.cursor = 'all-scroll';
			},
			stop : function(e, ui){
				this.style.cursor = 'unset';
				this.style.left = ((this.offsetLeft / this.parentElement.offsetWidth) *  100).toFixed(2) + '%';
				this.style.top = ((this.offsetTop / this.parentElement.offsetHeight) * 100).toFixed(2) + '%';
				this.style.width = (($(this).width() / this.parentElement.offsetWidth) * 100).toFixed(2) + '%';
				this.style.height = (($(this).height() / this.parentElement.offsetHeight) * 100).toFixed(2) + '%';
			}
		});

		var resizeArea = container.resizeArea = container.appendChild($('<div class="digital-sign-resize" style="position:absolute;"></div>')[0]);
		resizeArea.style.left = 'calc(100% - ' + resizeArea.offsetWidth + 'px)';
		resizeArea.style.top = 'calc(100% - ' + resizeArea.offsetHeight + 'px)';
		resizeArea.addEventListener('pointerdown', function(e) {
			var pos = {
				x : e.pageX,
				y : e.pageY
			};
			var currentSize = {
				width : container.offsetWidth,
				height : container.offsetHeight
			};

			var move = function(e) {
				if (e.cancelable) {
					e.preventDefault();
				}
				container.style.width = currentSize.width + (e.pageX - pos.x) + 'px';
				container.style.height = currentSize.height + (e.pageY - pos.y) + 'px';
				selectorBody.style.width = container.offsetWidth + 'px';
				selectorBody.style.height = (container.offsetHeight - selectorHeader.offsetHeight) + 'px';
				if(container.clientWidth > container.clientHeight) {
					signImageArea.style.width = '';
					signImageArea.style.height = 'inherit';
				} else {
					signImageArea.style.width = 'inherit';
					signImageArea.style.height = '';
				}
			}

			var up = function() {
				container.parentElement.removeEventListener('pointermove', move);
				container.parentElement.removeEventListener('touchmove', move);
				container.parentElement.removeEventListener('pointerup', up);
			};

			container.parentElement.addEventListener('pointermove', move);
			container.parentElement.addEventListener('touchmove', move);
			container.parentElement.addEventListener('pointerup', up);
		});

		var touchDraggable = function(container) {
			var pointerObj = {
				cache : []
				, basePointDiff : {x : -1, y : -1}
				, prevPointDiff : {x : -1, y : -1}
			}
			var cancleClass = [
				'digital-sign-selector-header-icon',
				'digital-sign-resize'
			];
			var baseX;
			var baseY;
			var dragStart = function(e) {
				pointerObj.cache.push(e);
				if(pointerObj.cache.length != 1) {
					return false;
				}
				for(let className of cancleClass) {
					if(e.target.classList.contains(className)) {
						return;
					}
				}
				if (e.cancelable) {
					e.preventDefault();
				}
				this.style.left = this.offsetLeft + 'px';
				this.style.top = this.offsetTop + 'px';
				baseX = e.touches[0].clientX;
				baseY = e.touches[0].clientY;
			}
			var dragMove = function(e) {
				if(pointerObj.cache.length != 1) {
					return false;
				}
				for(let className of cancleClass) {
					if(e.target.classList.contains(className)) {
						return;
					}
				}
				if (e.cancelable) {
					e.preventDefault();
				}
				var moveX = e.touches[0].clientX - baseX;
				var moveY = e.touches[0].clientY - baseY;
				var targetX = this.offsetLeft + moveX;
				var targetY = this.offsetTop + moveY;
				baseX = e.touches[0].clientX;
				baseY = e.touches[0].clientY;
				if(targetX < 0) {
					targetX = 0;
				}
				if(targetY < 0) {
					targetY = 0;
				}
				if(this.offsetLeft + this.offsetWidth > this.parentElement.offsetWidth) {
					targetX = this.parentElement.offsetWidth - this.offsetWidth;
				}
				if(this.offsetTop + this.offsetHeight > this.parentElement.offsetHeight) {
					targetY = this.parentElement.offsetHeight - this.offsetHeight;
				}
				this.style.left = targetX + 'px';
				this.style.top = targetY + 'px';
			}
			var dragEnd = function(e) {
				for(let className of cancleClass) {
					if(e.target.classList.contains(className)) {
						return;
					}
				}
				if (e.cancelable) {
					e.preventDefault();
				}
				var targetX = this.offsetLeft;
				var targetY = this.offsetTop;
				if(targetX < 0) {
					targetX = 0;
				}
				if(targetY < 0) {
					targetY = 0;
				}
				if(this.offsetLeft + this.offsetWidth > this.parentElement.offsetWidth) {
					targetX = this.parentElement.offsetWidth - this.offsetWidth;
				}
				if(this.offsetTop + this.offsetHeight > this.parentElement.offsetHeight) {
					targetY = this.parentElement.offsetHeight - this.offsetHeight;
				}
				this.style.left = ((targetX / this.parentElement.offsetWidth) * 100).toFixed(2) + '%';
				this.style.top = ((targetY / this.parentElement.offsetHeight) * 100).toFixed(2) + '%';
				pointerObj = {
					cache : []
					, basePointDiff : {x : -1, y : -1}
					, prevPointDiff : {x : -1, y : -1}
				}
			}
			container.addEventListener('touchstart', dragStart);
			container.addEventListener('touchmove', dragMove);
			container.addEventListener('touchend', dragEnd);
		}
		touchDraggable(container);
	}.bind(this));
}
// [CHECKBOX 기능 추가] 체크박스 표시 상태 업데이트
SepoaContractItem.prototype.updateCheckboxDisplay=function(){
	var mark = null;
	
	// ITEM_TYPE에 따라 적절한 mark 선택
	if(this.conf.ITEM_TYPE == 'CHECKBOX'){
		mark = this.checkboxMark;
		if(!mark){
			mark = this.checkboxMark = $(this.base).find('.contract-checkbox-mark')[0];
		}
	} else if(this.conf.ITEM_TYPE == 'MCHECKBOX'){
		mark = this.mcheckboxMark;
		if(!mark){
			mark = this.mcheckboxMark = $(this.base).find('.contract-mcheckbox-mark')[0];
		}
	} else if(this.conf.ITEM_TYPE == 'NCHECKBOX'){
		mark = this.ncheckboxMark;
		if(!mark){
			mark = this.ncheckboxMark = $(this.base).find('.contract-ncheckbox-mark')[0];
		}
	}
	
	// mark가 있으면 표시/숨김 처리
	if(mark){
		if(this.conf.CHECKBOX_CHECKED){
			mark.style.display = 'block';
			mark.style.visibility = 'visible';
		} else {
			mark.style.display = 'none';
			mark.style.visibility = 'hidden';
		}
	}
}
SepoaContractItem.prototype.openSignSetter = function() {
	var item = this;
	var onSaved = function() {
		if(item.contractObj && item.contractObj.signSetter) {
			$(item.contractObj.signSetter).remove();
			item.contractObj.signSetter = null;
		}
		item._applySavedDigitalSign();
	};
	// 서명관리 없이 단일 슬롯(SIGN_SEQ=1) 덮어쓰기
	var setterData = { SIGN_SEQ: '1' };

	if(this.contractObj.signSetter) {
		$(this.contractObj.signSetter).remove();
		this.contractObj.signSetter = null;
	}

	import('/resource/plugin/sepoasoft/sign/DigitalSignSetter.js').then(module => {
		if(this.contractObj) {
			if(!this.contractObj.editor.base.querySelector('link[href*="easySign.css"]')) {
				const style = document.createElement('link');
				style.href = '/resource/plugin/sepoasoft/sign/css/easySign.css';
				style.rel = 'stylesheet';
				this.contractObj.editor.base.appendChild(style);
			}
			
			const signSetter = this.contractObj.signSetter = sUtils.element.create('<div class="contract-sign-setter-container"></div>');
			const signSetterContent = sUtils.element.create('<div class="contract-sign-setter-content"></div>');
			signSetter.appendChild(signSetterContent);
			signSetter.onpointerup = (e) => {
				if(e.target === signSetter) {
					$(signSetter).remove();
					if(this.contractObj) {
						this.contractObj.signSetter = null;
					}
				}
			}
			
			this.contractObj.editor.base.appendChild(this.contractObj.signSetter);
			this.contractObj.signSetter.obj = new module.SepoaDigitalSignSetter(
				signSetterContent,
				onSaved,
				'',
				setterData
			);
		}
	});
}
SepoaContractItem.prototype.extractDigitalSign=function(){
	if(this.base.classList.contains('digital-sign-selector-container')) {
		this.base.style.left = ((this.base.offsetLeft / this.base.parentElement.offsetWidth) * 100).toFixed(2) + '%'
		this.base.style.top = (((this.base.offsetTop + this.base.selectorHeader.offsetHeight) / this.base.parentElement.offsetHeight) * 100).toFixed(2) + '%'
		this.base.style.width = ((this.base.offsetWidth / this.base.parentElement.offsetWidth) * 100).toFixed(2) + '%';
		this.base.style.height = (((this.base.offsetHeight - this.base.selectorHeader.offsetHeight) / this.base.parentElement.offsetHeight) * 100).toFixed(2) + '%';
		
		this.base.selectorBody.style.border = '';		
		this.base.selectorBody.style.width = '100%';
		this.base.selectorBody.style.height = '100%';

		this.base.selectorHeader.remove();
		this.base.resizeArea.remove();

		return this.base;
	} else {
		return;
	}
}
// 서명자 텍스트/개인정보 입력 필드 추출 (SIGN_FIX_HTML 병합용) — MTEXT/NTEXT/기타 텍스트형
SepoaContractItem.prototype.extractSignerTextField=function(){
	var itemType = String(this.conf.ITEM_TYPE || '').toUpperCase();
	if(['SIGN','CHECKBOX','MCHECKBOX','NCHECKBOX','SEAL','IMAGE'].indexOf(itemType) >= 0) {
		return null;
	}
	var val = '';
	if(this.contractObj && this.contractObj._getSignerTextFieldValue) {
		val = this.contractObj._getSignerTextFieldValue(this);
	} else if(this.conf.value != null && String(this.conf.value).length > 0) {
		val = String(this.conf.value);
	} else if(this.input) {
		val = this.input.value || this.input.innerHTML || '';
	}
	// NTEXT: 선택 입력 — 값이 없으면 완성본 HTML에 미포함
	if(itemType == 'NTEXT' && String(val).trim().length === 0) {
		return null;
	}
	var clone = this.base.cloneNode(true);
	clone.classList.remove('contract-item-active', 'contract-item-validation-failed');
	var dstTa = clone.querySelector('textarea.contract-item-input');
	if(dstTa) {
		dstTa.value = val;
		dstTa.textContent = val;
		dstTa.removeAttribute('readonly');
		dstTa.removeAttribute('placeholder');
	}
	$(clone).find('.contract-item-se-resize, .contract-item-delete, .contract-item-cover').remove();
	$(clone).find('.contract-item-editor, .contract-item-editor-show').remove();
	clone.style.display = 'inline-flex';
	return clone;
}
// [CHECKBOX 기능 추가] 체크박스 아이템 추출 메서드 (extractDigitalSign과 동일한 패턴)
SepoaContractItem.prototype.extractCheckbox=function(){
	if(this.conf.ITEM_TYPE == 'CHECKBOX' || this.conf.ITEM_TYPE == 'MCHECKBOX' || this.conf.ITEM_TYPE == 'NCHECKBOX') {
		// 체크 상태에 따라 mark 표시/숨김
		var mark = null;
		if(this.conf.ITEM_TYPE == 'CHECKBOX'){
			mark = this.checkboxMark;
		} else if(this.conf.ITEM_TYPE == 'MCHECKBOX'){
			mark = this.mcheckboxMark;
		} else if(this.conf.ITEM_TYPE == 'NCHECKBOX'){
			mark = this.ncheckboxMark;
		}
		
		if(mark){
			if(this.conf.CHECKBOX_CHECKED){
				mark.style.display = 'block';
				mark.style.visibility = 'visible';
				// [CHECKBOX 기능 추가] HTML → 이미지 변환 시 CSS ::after가 렌더링되지 않을 수 있으므로 실제 텍스트 노드 추가
				// 문자 V 사용 (ASCII, 폰트/인코딩 환경에 무관하게 표시 보장)
				if(!mark.textContent && !mark.innerHTML){
					mark.innerHTML = 'V';
				}
			} else {
				mark.style.display = 'none';
				mark.style.visibility = 'hidden';
				// [CHECKBOX 기능 추가] 체크 해제 시 텍스트 제거
				mark.textContent = '';
			}
		} else {
			// mark가 없으면 다시 찾기
			if(this.conf.ITEM_TYPE == 'CHECKBOX'){
				this.checkboxMark = $(this.base).find('.contract-checkbox-mark')[0];
				mark = this.checkboxMark;
			} else if(this.conf.ITEM_TYPE == 'MCHECKBOX'){
				this.mcheckboxMark = $(this.base).find('.contract-mcheckbox-mark')[0];
				mark = this.mcheckboxMark;
			} else if(this.conf.ITEM_TYPE == 'NCHECKBOX'){
				this.ncheckboxMark = $(this.base).find('.contract-ncheckbox-mark')[0];
				mark = this.ncheckboxMark;
			}
			
			if(mark){
				if(this.conf.CHECKBOX_CHECKED){
					mark.style.display = 'block';
					mark.style.visibility = 'visible';
					// [CHECKBOX 기능 추가] HTML → 이미지 변환 시 CSS ::after가 렌더링되지 않을 수 있으므로 실제 텍스트 노드 추가
					// 문자 V 사용 (ASCII, 폰트/인코딩 환경에 무관하게 표시 보장)
					if(!mark.textContent && !mark.innerHTML){
						mark.innerHTML = 'V';
					}
				} else {
					mark.style.display = 'none';
					mark.style.visibility = 'hidden';
					// [CHECKBOX 기능 추가] 체크 해제 시 텍스트 제거
					mark.textContent = '';
				}
			}
		}
		
		// 위치를 퍼센트로 변환 (extractDigitalSign과 동일한 패턴)
		this.base.style.left = ((this.base.offsetLeft / this.base.parentElement.offsetWidth) * 100).toFixed(2) + '%';
		this.base.style.top = ((this.base.offsetTop / this.base.parentElement.offsetHeight) * 100).toFixed(2) + '%';
		this.base.style.width = ((this.base.offsetWidth / this.base.parentElement.offsetWidth) * 100).toFixed(2) + '%';
		this.base.style.height = ((this.base.offsetHeight / this.base.parentElement.offsetHeight) * 100).toFixed(2) + '%';
		
		return this.base;
	} else {
		return;
	}
}
SepoaContractItem.prototype.remove=function(){
	this.base.parentElement.removeChild(this.base);
	if(this.source){
		this.source.signMarkArr.splice(this.source.signMarkArr.indexOf(this),1);
	}
}
SepoaContractItem.prototype._onMouseleave=SepoaContractItem.prototype._onClick
SepoaContractItem.prototype._onMouseleave2=function(e){
	var type = null
	if(this.status.draggable){
		type = "draggable"
	}else if(this.status.resizeable){
		type = "resizeable"
	}
	if(type == null || type == "draggable"){
		if(this._mouseDownEventDelay){
			clearTimeout(this._mouseDownEventDelay)
		}
		e.stopPropagation();
		e.preventDefault();
		e.stopImmediatePropagation();
		if(this.conf.ITEM_SOURCE == 'org' && this.conf.ITEM_ITEMKEY){
			this.contractObj.editor.setAttrCloneItem(this);
		}
	}
	
	this.contractObj.editor.setItemIndex(e.currentTarget.parentElement);
}
SepoaContractItem.prototype._onMousedown=function(e){
	if(this.contractObj.helper && this.contractObj.helper.status){
		return true;
	}else{
		// [CHECKBOX 기능 추가] 서명 모드에서 체크박스는 드래그 비활성화
		// [서명자 입력] 서명 모드에서는 위치 변경 비활성화 (입력만 허용)
		if(this.contractObj.editor._mode == 'sign'){
			return;
		}
		
		if(this.conf.ITEM_SOURCE == 'copy' ||this.conf.ITEM_SOURCE == 'symmetry' ){
			var that = this;
			Confirm(Sepoa.cache.text.get('POA_CONTRACT_EDITOR.NOT_EVENT_COPY_ITEM'), function(flag){
				that.contractObj.editor.moveOrgItem(that);
			})
			return false;
		}
		
		if(typeof(e.movementX) == "undefined"){
			this.lastMouseWhich={
				x:e.screenX,
				y:e.screenY
			}
		}
		
		if(e.srcElement.classList.contains('contract-item-se-resize')){
			this.status.resizeable = true;
			this.setRectSaveType(false);
			this.base.classList.add('contract-item-active')
			$(this.base).closest('.contract-item-layer')[0].classList.add('contract-item-resizeable')
		}else{
			this._mouseDownEventDelay = setTimeout(function(obj){
				clearTimeout(obj._mouseDownEventDelay)
				if(obj.base.parentElement){
					obj.status.draggable = true;
					obj.setRectSaveType(false);
					obj.base.classList.add('contract-item-active')
					$(obj.base).closest('.contract-item-layer')[0].classList.add('contract-item-draggable')
				}
			}, 200, this)
		}
	}
}
SepoaContractItem.prototype._onMousemove=function(e){
	if(this.status.draggable){
		if(typeof(e.movementX) == "undefined"){
			e.movementX = e.screenX - this.lastMouseWhich.x
			e.movementY = e.screenY - this.lastMouseWhich.y
			this.lastMouseWhich = {
				x:e.screenX,
				y:e.screenY
			}
		}
		
		var now = {
			x:parseInt(this.base.style.left.replace(/[^0-9\.]/g, "") || 0),
			y:parseInt(this.base.style.top.replace(/[^0-9\.]/g, "") || 0)
		}
		var pos = {
			x : e.movementX + now.x,
			y : e.movementY + now.y
		}
		if(pos.y < 0){
			pos.y = 0;
		}else if(((pos.y + this.base.offsetHeight) > this.base.parentElement.offsetHeight)){
			pos.y = this.base.parentElement.offsetHeight - this.base.offsetHeight 
		}
		if(pos.x < 0){
			pos.x = 0;
		}else if(((pos.x + this.base.offsetWidth) > this.base.parentElement.offsetWidth)){
			pos.x = this.base.parentElement.offsetWidth - this.base.offsetWidth
		}
		this.setSize({
			x:pos.x+'px',
			y:pos.y+'px'
		})
		this.editor.move();
		e.stopPropagation();
	}else if(this.status.resizeable){
		if(typeof(e.movementX) == "undefined"){
			e.movementX = e.screenX - this.lastMouseWhich.x
			e.movementY = e.screenY - this.lastMouseWhich.y
			this.lastMouseWhich = {
				x:e.screenX,
				y:e.screenY
			}
		}
		var setSizeParam = {
			w:parseInt($(this.base).css('width').replace(/[^0-9\.]/g, "")) + e.movementX +'px'
		}
		var itemType = this.conf.ITEM_TYPE; //클래스 or추가 필요
		if(itemType == 'SEAL' || itemType == 'IMAGE' || $(this.base.obj.base).hasClass('contract-item-box-seal-data') || $(this.base.obj.base).hasClass('contract-item-type-image')){
			setSizeParam['h'] = parseInt($(this.base).css('height').replace(/[^0-9\.]/g, "")) + e.movementY +'px'
		}
		//maxWidth 지정 필요.
		this.setSize(setSizeParam);
		this.editor.move();
		e.stopPropagation();
	}
}

/* contract item editor */
SepoaContractItemEditor=function(item){
	sUtils.modules.extend(this, "EventManager")
	this.status = false;
	this.item = item;
	this.parseOption();
	this.extendFunc();
	this.renderBase();
}
SepoaContractItemEditor.prototype._onEvent=function(e){
	if(e.type == "click"){
		if(!e.path){ // IE
			e.path = [];
			var target = e.target;
			while (target.parentNode !== null) {
				e.path.push(target);
				target = target.parentNode;
			}
		}
		if((e.path.indexOf(this.base) + 1)){
			if(this.cal){
				this.cal.hide();
			}
			e.stopPropagation();
			e.preventDefault();
			e.stopImmediatePropagation();
		}else if((e.path.indexOf(this.item.base) < 0)){
			this.hide();
		}
	}else if(e.type == "mouseleave"){
		if(this.status){
			this.hide();
		}
	}
	
}
SepoaContractItemEditor.prototype.renderBase=function(){
	this.vp = $(this.item.base).closest('.contract-editor-area')[0];
	this.base = this.vp.appendChild($('<div class="contract-item-editor-base"></div>')[0])
	this.base.obj = this;
	this.prop = this.base.appendChild($('<div class="contract-item-editor-prop-base"></div>')[0]).appendChild($('<table class="contract-item-editor-prop-table"></table>')[0])
	this.renderProp();
	
	this.vp.addEventListener('mouseleave'     , function(a){ return function(){ return function(e){ this._onEvent(e) }.apply(a, arguments) }}(this));
	this.vp.addEventListener('click'          , function(a){ return function(){ return function(e){ this._onEvent(e) }.apply(a, arguments) }}(this));
	this.base.addEventListener('click'          , function(a){ return function(){ return function(e){ this._onEvent(e) }.apply(a, arguments) }}(this));
}
SepoaContractItemEditor.prototype.parseOption=function(){
	this.conf={}
	
	for(var i=0;i<this.item.base.classList.length;i++){
		if(this.item.base.classList[i].indexOf('contract-item-type-') == 0){
			this.type = this.item.base.classList[i].substring(this.item.base.classList[i].lastIndexOf("-")+1).toLowerCase();
			break;
		}
	}
	
	// type이 설정되지 않은 경우 conf.ITEM_TYPE에서 추출 시도
	if(!this.type && this.item.conf && this.item.conf.ITEM_TYPE){
		this.type = this.item.conf.ITEM_TYPE.toLowerCase();
	}

	for(var i=0;i<this.item.input.classList.length;i++){
		var m = this.item.input.classList[i].match(/^contract-item-prop-([^-]{0,})-([^-]{0,})/)
		if(m){
			this.conf[m[1]] = m[2]
		}
	}
	
	var value = this.item.origin?this.item.origin.innerHTML:"";
	var placeHolder = this.item.input.getAttribute('placeholder');
	var prefix = this.item.prefix?this.item.prefix.innerHTML:null;
	if(prefix){
		this.conf.prefix = prefix;
		if(!value){
			placeHolder = this.item.originPlaceHolder = placeHolder.replace(new RegExp("^"+prefix), "");
		}
	}
	var suffix = this.item.suffix?this.item.suffix.innerHTML:null;
	if(suffix){
		this.conf.suffix = suffix;
		if(!value){
			this.item.originPlaceHolder = placeHolder.replace(new RegExp(suffix+"$"), "");
		}
	}
	if(value){
		this.conf.value = value;
	}
}
SepoaContractItemEditor.prototype.extendFunc=function(){
	if(this.type && this.modules[this.type]){
		for(var i in this.modules[this.type]){
			this[i] = this.modules[this.type][i]
		}
	}
	// renderProp이 할당되지 않은 경우를 대비한 기본값 설정
	if(!this.renderProp){
		this.renderProp = function(){};
	}
}
SepoaContractItemEditor.prototype.move=function(){
	var iO = $(this.item.base).offset();
	var vO = $(this.vp).offset();
	
	var item={
		top:(iO.top - vO.top) + this.vp.scrollTop,
		left:(iO.left - vO.left) + this.vp.scrollLeft
	}
	
	if(item.top + this.item.base.offsetHeight + this.base.offsetHeight > this.vp.scrollHeight){
		item.top -= this.base.offsetHeight;
		this.base.classList.add('contract-item-editor-arrow-bottom')
		this.base.classList.remove('contract-item-editor-arrow-top')
	}else{
		item.top += this.item.base.offsetHeight;
		this.base.classList.remove('contract-item-editor-arrow-bottom')
		this.base.classList.add('contract-item-editor-arrow-top')
	}
	
	if(item.left + this.base.scrollWidth > this.vp.offsetWidth){
		item.left = item.left - (this.base.scrollWidth - this.item.base.offsetWidth);
	}
	
	this.base.style.top = item.top+"px"
	this.base.style.left = item.left + "px"
}
SepoaContractItemEditor.prototype.show=function(){
	this.status = true;
	this.base.classList.add('contract-item-editor-show');
	this.move();
}

SepoaContractItemEditor.prototype.hide=function(){
	if(this.cal){
		this.cal.hide();
	}
	this.status = false;
	this.base.classList.remove('contract-item-editor-show');
}
SepoaContractItemEditor.prototype._getPropElement=function(type, visible){
	var tr = $('<tr class="contract-item-editor-prop-line contract-item-editor-prop-type-'+type+'"></tr>')[0]
	var dtd = $('<td class="contract-item-editor-prop-td"></td>')[0];
	
	tr.appendChild($('<td class="contract-item-editor-prop-label">'+Sepoa.cache.text.get("POA_CONTRACT_EDITOR.PROP_LABEL_"+type.toUpperCase())+'</td>')[0]);
	
	if(type == "font_size" || type == 'cut_percent' || type == 'symmetry_cut_percent' || type == 'seal_width' || type == 'seal_height'){
		var slider;
		var manual;
		
		this.conf[type] = this.conf[type] || Sepoa.cache.text.get("POA_CONTRACT_EDITOR.PROP_DEFAULT_"+type.toUpperCase());
		if(type == 'font_size'){
			slider = tr.appendChild(dtd.cloneNode()).appendChild($('<input class="contract-item-editor-prop-font_size contract-item-editor-prop-font_size-slider" min="1" max="5000" type="range" />')[0])
			manual = tr.appendChild(dtd.cloneNode()).appendChild($('<input class="contract-item-editor-prop-font_size contract-item-editor-prop-font_size-manual contract-item-editor-prop-input" min="1" max="5000" type="number" />')[0])
		} else if(type == 'cut_percent' || type == 'symmetry_cut_percent') {
			slider = tr.appendChild(dtd.cloneNode()).appendChild($('<input class="contract-item-editor-prop-font_size contract-item-editor-prop-font_size-slider" min="0" max="100" type="range" />')[0])
			manual = tr.appendChild(dtd.cloneNode()).appendChild($('<input class="contract-item-editor-prop-font_size contract-item-editor-prop-font_size-manual contract-item-editor-prop-input" min="0" max="100" type="number" />')[0])
		} else if(type == 'seal_width' || type == 'seal_height') {
			slider = tr.appendChild(dtd.cloneNode()).appendChild($('<input class="contract-item-editor-prop-font_size contract-item-editor-prop-font_size-slider" min="0" max="100" type="range" />')[0]);
			manual = tr.appendChild(dtd.cloneNode()).appendChild($('<input class="contract-item-editor-prop-font_size contract-item-editor-prop-font_size-manual contract-item-editor-prop-input" min="0" max="100" type="number" />')[0]);
		}

		manual.link = slider;
		slider.link = manual;
		manual.editor = slider.editor = this;
		manual.onkeyup=function(e){
			var k = e.which || e.keyCode
			if(k == 38){
				this.value = praseInt(this.value) + 1
				this.onchange();
			}else if(k == 40){
				this.value = this.value - 1
				this.onchange();
			}
		}
		
		var _onchange = function(){
			var min = parseInt(this.getAttribute('min'));
			var max = parseInt(this.getAttribute('max'));
			var now = parseInt(this.value);
			if(now < min){
				this.value = min;
			}else if(now > max){
				this.value = max;
			}
			
			this.link.value = this.value;
			var propObj = {}
			propObj[type] = this.value;
			this.editor.setProperties(propObj);
		}
		manual.onchange = _onchange
		slider.onchange = _onchange
		
		slider.value = this.conf[type];
		manual.value = this.conf[type];
	} else if(type == "value"){
		var td = tr.appendChild(dtd.cloneNode());
		td.setAttribute('colspan', 2);
		this.prop.value = td;
		// MTEXT(필수)/NTEXT(선택)도 TEXT와 동일하게 값 입력란 생성
		if(this.type == "text" || this.type == "mtext" || this.type == "ntext"){
			var input = td.appendChild($('<textarea class="contract-item-editor-prop-value-input contract-item-editor-prop-input" style="resize:none;"></textarea>')[0]);
			input.obj = this;
			input.addEventListener('change', function(e){
				this.obj.setProperties({value:this.value});	
			});
		}else if(this.type == "number"){
			var input = td.appendChild($('<input type="number" class="contract-item-editor-prop-value-input contract-item-editor-prop-input"></input>')[0]);
			input.obj = this;
			input.addEventListener('change', function(e){
				this.obj.setProperties({value:this.value});	
			});
			input.addEventListener('focus', function(e){
				this.value = this.value.replace(/[^0-9\.]/g,"");
			});
		}else if(this.type == "calendar"){
			var input = td.appendChild($('<input class="contract-item-editor-prop-value-input contract-item-editor-prop-input"></input>')[0]);
			input.obj = this;
			input.addEventListener('click', function(e){
				var iO = $(this).offset();
				var vO = $(this.obj.vp).offset();
				
				var item={
					top:(iO.top - vO.top) + this.obj.vp.scrollTop + this.offsetHeight,
					left:(iO.left - vO.left) + this.obj.vp.scrollLeft
				}
				
				this.obj.cal.setPosition(item.left, item.top);
				this.obj.cal._show(false, false);
				this.obj.cal.setDateFormat(Sepoa.info.DATE_FORMAT);
				this.obj.cal.setDate(this.value || (new Date()));
				
				e.stopPropagation();
				e.preventDefault();
				e.stopImmediatePropagation();
			})
			
			var c = this.cal = new dhtmlxCalendarObject(undefined, undefined, this.vp);
			c.obj = this;
			c.input = input;
			c.attachEvent("onClick", function(d) {
				this.input.value = this.getFormatedDate(Sepoa.info.DATE_FORMAT, d);
                this.obj.setProperties({value:d});	
				
                this.hide();
				return true;
            });
			c.attachEvent("onButtonClick", function(d){
				if(d){
					this.input.value = this.getFormatedDate(Sepoa.info.DATE_FORMAT, d);
				}
				this.obj.setProperties({value:(d || "")});
				this.hide();
			})
			
		} 
	} else if(type == "prefix"){
		var td = tr.appendChild(dtd.cloneNode());
		td.setAttribute('colspan', 2);
		
		var input = td.appendChild($('<input class="contract-item-editor-prop-prefix-input contract-item-editor-prop-input"></input>')[0]);
		input.obj = this;
		input.addEventListener('change', function(e){
			this.obj.setProperties({prefix:this.value});	
		});
		if(this.conf.prefix){
			input.value = this.conf.prefix;
		}
	} else if(type == "suffix"){
		var td = tr.appendChild(dtd.cloneNode());
		td.setAttribute('colspan', 2);
		
		var input = td.appendChild($('<input class="contract-item-editor-prop-suffix-input contract-item-editor-prop-input"></input>')[0]);
		input.obj = this;
		input.addEventListener('change', function(e){
			this.obj.setProperties({suffix:this.value});	
		});
		if(this.conf.suffix){
			input.value = this.conf.suffix;
		}
	} else {
		var td = tr.appendChild(dtd.cloneNode());
		td.setAttribute('colspan', 2);
		
		var select = td.appendChild($('<select class="contract-item-editor-prop-select contract-item-editor-prop-input"></select>')[0])
		select.editor = this;
		select._type = type;
		select.onchange=function(e){
			var type = this._type;
			var data = {};
			data[type] = this.value;
			this.editor.setProperties(data);
		}
		
		var def="";
		var ft = null;
		if(type == "font_align"){
			def = this.conf.font_align = this.conf.font_align||Sepoa.cache.text.get("POA_CONTRACT_EDITOR.PROP_DEFAULT_FONT_ALIGN");
			ft = Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#CT022')
		} else if(type == "font_type"){
			def = this.conf.font_type = this.conf.font_type||Sepoa.cache.text.get("POA_CONTRACT_EDITOR.PROP_DEFAULT_FONT_TYPE");
			ft = Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#CT021')
		} else if(type == "date_format"){
			def = this.conf.date_format = this.conf.date_format||Sepoa.cache.text.get("POA_CONTRACT_EDITOR.PROP_DEFAULT_DATE_FORMAT");
			ft = Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#CT023')
		} else if(type == "number_format"){
			def = this.conf.number_format = this.conf.number_format||"";
			ft = [{text:"", value:""}].concat(Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#CT024'));
		} else if(type == 'cut_position' || type == 'symmetry_cut_position'){
			def = this.conf[type] =  this.conf[type] ||  Sepoa.cache.text.get("POA_CONTRACT_EDITOR.PROP_DEFAULT_CUT_POSITION"); 
			ft = Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#CT030');
		} else if(type == 'repeat' || type == 'symmetry' || type == 'symmetry_page_copy'){
			def = this.conf[type] =  this.conf[type] || Sepoa.cache.text.get("POA_CONTRACT_EDITOR.PROP_DEFAULT_ON_OFF");
			ft = Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#CT031');
		} else if(type == 'seal_template'){
			def = this.conf[type] = this.conf[type] || Sepoa.cache.text.get("POA_CONTRACT_EDITOR.PROP_LABEL_SEAL_DEFAULT");
			ft = Sepoa.cache.combo.get('SL0001,'+Sepoa.info.HOUSE_CODE+'#'+Sepoa.info.LANGUAGE+'#CT032');
		}
		
		for(var i in ft){
			var option = select.appendChild(new Option(ft[i].text, ft[i].value))
			if(option.value == def){
				option.selected = true
			}
		}
	}
	if(visible == false){
		tr.style.display = "none";
	}
	
	return tr;
}
SepoaContractItemEditor.prototype.getProperties=function(){
	return this.conf;
}
SepoaContractItemEditor.prototype.modules={}
SepoaContractItemEditor.prototype.modules.text={
	renderProp:function(){
		this.prop.appendChild(this._getPropElement('value'));
		this.prop.appendChild(this._getPropElement('prefix'));
		this.prop.appendChild(this._getPropElement('suffix'));
		this.prop.appendChild(this._getPropElement('font_size'));
		this.prop.appendChild(this._getPropElement('font_align'));
		this.prop.appendChild(this._getPropElement('font_type'));
	},
	setProperties:function(data, flag){
		if(flag && data.value){
			$(this.prop.value).find('.contract-item-editor-prop-value-input').val(data.value);
		}
		this.conf = $.extend(true, this.conf, data)
		this.callEvent('onPropUpdate', []);
	}
}
// MTEXT(필수)/NTEXT(선택) — TEXT와 동일 UI
SepoaContractItemEditor.prototype.modules.mtext = SepoaContractItemEditor.prototype.modules.text;
SepoaContractItemEditor.prototype.modules.ntext = SepoaContractItemEditor.prototype.modules.text;
SepoaContractItemEditor.prototype.modules.number={
	renderProp:function(){
		this.prop.appendChild(this._getPropElement('value'));
		this.prop.appendChild(this._getPropElement('prefix'));
		this.prop.appendChild(this._getPropElement('suffix'));
		this.prop.appendChild(this._getPropElement('font_size'));
		this.prop.appendChild(this._getPropElement('font_align'));
		this.prop.appendChild(this._getPropElement('font_type'));
		this.prop.appendChild(this._getPropElement('number_format'));
	},
	setProperties:function(data, flag){
		if(flag && data.value){
			$(this.prop.value).find('.contract-item-editor-prop-value-input').val(data.value);
		}
		this.conf = $.extend(true, this.conf, data)
		if(typeof(this.conf.value) == "number"){
			this.conf.value += ""
		}
		
		this.callEvent('onPropUpdate', [])
	}
}
SepoaContractItemEditor.prototype.modules.calendar={
	renderProp:function(){
		this.prop.appendChild(this._getPropElement('value'));
		this.prop.appendChild(this._getPropElement('prefix'));
		this.prop.appendChild(this._getPropElement('suffix'));
		this.prop.appendChild(this._getPropElement('font_size'));
		this.prop.appendChild(this._getPropElement('font_align'));
		this.prop.appendChild(this._getPropElement('font_type'));
		this.prop.appendChild(this._getPropElement('date_format'));
	},
	setProperties:function(data, flag){
		if(data.value && typeof(data.value) != "string"){
			data.value = sUtils.date.toString(data.value, "%Y%m%d");
		}
		if(flag && data.value){
			$(this.prop.value).find('.contract-item-editor-prop-value-input').val(sUtils.date.toString(sUtils.date.fromString(data.value, "%Y%m%d"), Sepoa.info.DATE_FORMAT || "%Y/%m/%d"));
		}
		if(!data.value){
			$(this.prop.value).find('.contract-item-editor-prop-value-input').val("");
		}
		this.conf = $.extend(true, this.conf, data)
		
		this.callEvent('onPropUpdate', [])
	}
}
SepoaContractItemEditor.prototype.modules.image={
	renderProp:function(){
	},
	setProperties:function(data, flag){
		if(flag && data.value){
			$(this.prop.value).find('.contract-item-editor-prop-value-input').val(data.value);
		}
		this.callEvent('onPropUpdate', []);
	},
	show:function(){
		this.hide();
	}
}
SepoaContractItemEditor.prototype.modules.seal={
	renderProp:function(){
		this.prop.appendChild(this._getPropElement('seal_template')); 					//규격
		this.prop.appendChild(this._getPropElement('repeat')); 							//반복
		this.prop.appendChild(this._getPropElement('cut_position', false));				//자르기 활성화되면 어느방향으로 자를지
		this.prop.appendChild(this._getPropElement('cut_percent', false));				//자르기 활성화되면 얼마나 자를지
		this.prop.appendChild(this._getPropElement('symmetry', false));					//대칭 만들기
		this.prop.appendChild(this._getPropElement('symmetry_cut_position', false));	//대칭만들기 활성화되면 어느방향으로 대칭을 만들지
		this.prop.appendChild(this._getPropElement('symmetry_cut_percent', false));		//대칭되는 이미지의 퍼센트 조절
		this.prop.appendChild(this._getPropElement('symmetry_page_copy', false));		//대칭 페이지 복제
		this.prop.appendChild(this._getPropElement('seal_width', false));				//직인 길이(cm)
		this.prop.appendChild(this._getPropElement('seal_height', false));				//직인 높이(cm)
	},
	setProperties:function(data, flag){
		if(flag && data.value){
			this.prop.value.find('.contract-item-editor-prop-value-input').val(data.value);
		}

		this.conf = $.extend(true, this.conf, data)
		if(data.seal_template){
			var option="";
			switch(data.seal_template){
				case "CRS" : 
					option = {"cut_position":"LEFT","cut_percent":"50","symmetry":"ON","symmetry_cut_position":"RIGHT","symmetry_cut_percent":"50","symmetry_page_copy":"ON"};
					this.item.base.style.left=0;
					this.conf.repeat='ON'
					this.prop.querySelector('.contract-item-editor-prop-type-repeat .contract-item-editor-prop-select').value="ON"
					break;
				case "STD" :
					option = {"cut_position":"LEFT","cut_percent":"0","symmetry":"OFF","symmetry_cut_position":"LEFT","symmetry_cut_percent":"0","symmetry_page_copy":"OFF"};
					break;
				default :
					break;
			}
			
			$.extend(true, this.conf, option)
		}
		this.callEvent('onPropUpdate', [])
	}
}
SepoaContractItemEditor.prototype.modules.sign={
	renderProp:function(){
	},
	setProperties:function(data, flag){
		if(flag && data.value){
			this.prop.value.find('.contract-item-editor-prop-value-input').val(data.value);
		}
		this.callEvent('onPropUpdate', [])
	},
	show:function(){
		this.hide();
	}
}
// [CHECKBOX 기능 추가] 체크박스 아이템 편집기 모듈
SepoaContractItemEditor.prototype.modules.checkbox={
	renderProp:function(){
		// 체크박스는 편집할 속성이 없으므로 빈 함수
	},
	setProperties:function(data, flag){
		// 체크박스는 속성 설정 불필요
		this.callEvent('onPropUpdate', [])
	},
	show:function(){
		// 체크박스는 편집기를 표시하지 않음
		this.hide();
	}
}

SepoaContractItemEditor.prototype.modules.mcheckbox={
	renderProp:function(){
		// 체크박스는 편집할 속성이 없으므로 빈 함수
	},
	setProperties:function(data, flag){
		// 체크박스는 속성 설정 불필요
		this.callEvent('onPropUpdate', [])
	},
	show:function(){
		// 체크박스는 편집기를 표시하지 않음
		this.hide();
	}
}

SepoaContractItemEditor.prototype.modules.ncheckbox={
	renderProp:function(){
		// 체크박스는 편집할 속성이 없으므로 빈 함수
	},
	setProperties:function(data, flag){
		// 체크박스는 속성 설정 불필요
		this.callEvent('onPropUpdate', [])
	},
	show:function(){
		// 체크박스는 편집기를 표시하지 않음
		this.hide();
	}
}