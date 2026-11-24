SepoaContractApprovalManager = function(conf){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.test = [];
	this.conf = $.extend(true, {
		language:(window.CONTEXT_NAME ? window.CONTEXT_NAME : '') + '/resource/ux/langs/'+Sepoa.info.LANGUAGE+'/APPROVAL_LINE.js' //language file path
	}, conf);

	this.init();
}

SepoaContractApprovalManager.prototype.init = function() {
	if(!this.conf) {
		console.error('undefined conf params');
		return false;
	}
	
	if(!this.conf.base) {
		console.error('undefined base container');
		return false;
	}

	Sepoa.cache.text.load({
		url:this.conf.language,
		def:(window.CONTEXT_NAME ? window.CONTEXT_NAME : '')+'/resource/ux/langs/KO/APPROVAL_LINE.js'
	});
	
	//Sepoa.cache.text.load(this.conf.language, false);

	if(!this.conf.pattern || this.conf.pattern === '') {
		this.conf.pattern = 'UI1';
	}
	
	if(typeof this.conf.pattern === 'string') {
		if(this.conf.pattern === 'UI1') {
			this.conf.base.style.marginLeft = '10%';
			this.conf.base.style.marginRight = '10%';
			
			/* sample
patternObj = {
		direction : 'column',				// 전체 레이아웃의 적재 방향 (column, row)
		top : {
			direction : 'row',				// 컨테이너 내부에서 적재되는 방향 (column, row)
			container : [$('<div class="header" style="border:2px solid red;height:100%">TOP</div>')[0], 'header'],
			containerSetting : [			// 컨테이너 내부 내용의 설정 값 (width, height)
				{width : '20%'},
				{width : '80%'}
			],
			width : '10%',						// 컨테이너의 너비
			height : '10%',					// 컨테이너의 높이
			minWidth : '10%',					// 최소 너비
			minHeight : '10%',					// 최소 높이
		}
		middle : {
			  container : ['list']			// 컨테이너에 들어갈 내용 - HTMLElement값or 컴포넌트 이름(list, comment, header) 
			, componentSetting : [			// list 컴포넌트의 세팅 값 샘플
				{
					name : 'approval',		// 컴포넌트의 키값 이름
					header : '결재자',		// 해당 영역의 헤더 텍스트
					defaultData : 'sample',	// 초기 행의 값 - sample일 경우 본인의 정보
					button : {
						button1 : {location: 'top', text: '버튼gg'},					//top일 경우 click값이 없으면 팝업 버튼
						button2 : {location: 'bottom', customButton: HTMLElement}	//bottom일 경우 customButton값이 없으면  기본 값인 추가 버튼
					}
				}
			]
		},
		bottom : {
			  container : ['list', 'list']
		}
}

var approvalParamsObj = {
      base: setApprovalContainer				// vp
    , pattern : patternObj						// 기본값 : 'UI1' 또는 상단 패턴obj 샘플 코드 참조
    , componentConfig : {						// 컴포넌트 내부 데이터 세팅 옵션
    	sample1 : {								// 이름이 sample1인 컴포넌트의 옵션
    	      header : '결재자'					// 해당 컴포넌트 상단 레이아웃의 텍스트
			, dataOption : [
    		  	{text: '샘플컬럼'		, key: 'SAMPLE_1'		, width: '50%', hidden: false, blur: false, align: 'center', border: true},
				{text: '샘플컬럼 콤보'	, key: 'SAMPLE_2'		, width: '20%', hidden: false, blur: false, align: 'center', border: true, combo:'CT061'},
				{text: '샘플컬럼 이미지'	, key: 'SAMPLE_3'		, width: '30%', hidden: false, blur: false, align: 'center', image: '/theme/easysign/imgs/gridbutton/minus.png'}
			  ];
			  //dataOption 값 설명
//			  text	: 컬럼의 텍스트
//			  key	: 컬럼의 키값 - 데이터의 키와 매칭
//			  width	: 컬럼의 너비 비율
//			  hidden: 컬럼의 텍스트 노출 여부
//			  blur	: table-body-item-blur 클래스 관련 css 설정
//			  align : 컬럼과 데이터의 정렬 옵션
//			  border: 데이터의 테두리 여부
//			  combo	: 콤보박스의 공통코드 값 (SL0001모듈 기반)
//			  image : 데이터란에 들어갈 백그라운드 이미지 주소
    	}
    	, sample2 : {
    		  header : '결재자2'
    		, defaultData : {			// 기본값 : 'sample' (현재 세션의 정보 자동 세팅)
				  APP_LINE_SEQ : '0'
				, APP_METHOD : '01'
				, APP_USER_ID : 'SAMPLE_ID'
				, APP_USER_NAME : 'SAMPLE_NAME'
				, POSITION : Sepoa.info.POSITION
				, DEPARTMENT_NAME : Sepoa.info.DEPARTMENT_NAME_LOC
			}
		}
	}
};

new SepoaContractApprovalManager(approvalParamsObj);

			 */

			this.conf.patternObj = {
				direction : 'column',
				top : {
					container : 'comment'
				},
				approval : {
					  container : 'list'
					, componentSetting : [
						{
							name : 'approval',
							header : Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_HEADER'),
							defaultData : 'sample',
							button : {
								button1 : {location: 'top', text: Sepoa.cache.text.get('CONTRACT_APPROVAL.BUTTON_POPUP'),}
							}
						}
					]
				},
				referer : {
					container : 'list',
					componentSetting : [
						{
							name : 'referer',
							header : Sepoa.cache.text.get('CONTRACT_APPROVAL.REF_HEADER'),
						}
					]
				}
			}
		}
	} else {
		this.conf.patternObj = this.conf.pattern;
	}

	this.containerList = {};
	this.component = {};
	this.containers = new SepoaContractApprovalContainerGenerator(this.conf.patternObj);
	this.containers.parentObj = this;
	this.containers.importContainer();
	
	if(typeof this.conf.pattern === 'string' && this.conf.pattern === 'UI1') {
		this.component.approval.base.style.marginTop = '2%';
		this.component.approval.base.style.marginBottom = '5%';
		this.component.referer.base.style.marginBottom = '5%';
	}
}

SepoaContractApprovalManager.prototype.getApproverCountNameArray = function() {
	return this.approverCountNameArray;
}

//결재 정보 추출
SepoaContractApprovalManager.prototype.getAllApproverInfo = function() {
	var result = [];

	if(this.component) {
		var flag = false;
		for(var i in this.component) {
			if(this.component[i].objName == 'SepoaContractApprovalList') {
				flag = true;
				result = result.concat(this.component[i].getAllApprovalInfo());
			}
		}

		/*
		APP_LINE_SEQ와 APP_STAGE의 차이
		 - APP_LINE_SEQ는 전체 라인의 순서(결재자, 참조자 등등)
		 - APP_STAGE는 결재권자의 순서
		 - 상신자(기안자)는 해당 값 모두 0
		*/
		//APP_LINE_SEQ 재정렬
		result.map(function(e, i) {
			e.APP_LINE_SEQ = String(i);
			e.USER_CHECK_SEQ = String(i + 1);
		});
		
		if(!flag) {
			result = this.pattern.callback();
		}
	} else {
		result = this.pattern.callback();
	}

	return result; 
}

SepoaContractApprovalContainerGenerator = function(pattern) {
	this.conf = $.extend(true, {}, pattern);
}

SepoaContractApprovalContainerGenerator.prototype.importContainer = function() {
	if(!this.parentObj) {
		return false;
	}

	var base = this.parentObj.conf.base;
	base.style.display = 'flex';
	base.style.flexDirection = this.conf.direction || 'row';
	
	for(var i in this.conf) {
		if(i != 'direction') {
			var wrapper = document.createElement('div');
			wrapper.className = 'set-approval-base-cell-' + i;
			if(this.conf[i].width) wrapper.style.width = this.conf[i].width;
			if(this.conf[i].height) wrapper.style.height = this.conf[i].height;
			if(this.conf[i].minWidth) wrapper.style.minWidth = this.conf[i].minWidth;
			if(this.conf[i].minHeight) wrapper.style.minHeight = this.conf[i].minHeight;
			base.appendChild(wrapper);

			if(Array.isArray(this.conf[i].container)) {
				wrapper.style.display = 'flex';
				wrapper.style.flexDirection = this.conf[i].direction || 'row';
				this.parentObj.containerList[i] = [];

				if(!this.conf[i].containerSetting) {
					this.conf[i].containerSetting = [];
					for(var k in this.conf[i].container) {
						var styleKey = wrapper.style.flexDirection === 'row' ? 'width' : 'height';
						
						var tmp = {};
						tmp[styleKey] = 100 / this.conf[i].container.length + '%';
						this.conf[i].containerSetting.push(tmp);
					}
				}

				for(var j in this.conf[i].container) {
					var width = this.conf[i].containerSetting[j].width;
					var height = this.conf[i].containerSetting[j].height;
					var container = this._containerSwitch(i, wrapper, this.conf[i].container[j], j);
					if(container) {
						if(width) container.style.width = width;
						if(height) container.style.height = height;
					}
				}
			} else {
				this._containerSwitch(i, wrapper, this.conf[i].container);
			}
		}
	}
}

SepoaContractApprovalContainerGenerator.prototype._containerSwitch = function(name, vp, target, index) {
	if(!this.parentObj.containerList[name]) {
		this.parentObj.containerList[name] = [];
	}

	if(typeof target === 'object') {
		vp.appendChild(target);
		this.parentObj.containerList[name].push(target);

		return target;
	} else if(typeof target === 'string') {
		var item = null;
		var config;
		var componentName;
		if(this.parentObj.conf.patternObj[name].componentSetting) {
			index = index ? index : 0;
			config = this.parentObj.conf.patternObj[name].componentSetting[index];
			componentName = config.name ? config.name : undefined;
		} else {
			componentName = target;
		}

		if(this.parentObj.component) {
			var componentList = Object.keys(this.parentObj.component);
			var orgName = componentName ? componentName : 'undefined';
			for(var i = 1; i <= 100; i++) {
				if(componentList.indexOf(String(componentName)) >= 0) {
					componentName = orgName + "_" + i;
					config ? config.name = componentName : void 0;
				} else {
					break;
				}
			}
		}

		if(this.parentObj.conf.componentConfig && this.parentObj.conf.componentConfig[componentName]) {
			config = $.extend(true, {}, config, this.parentObj.conf.componentConfig[componentName])
		}

		switch(target) {
			case 'header':
				item = new SepoaContractApprovalHeader(config);
				break;
			case 'list':
				item = new SepoaContractApprovalList(config);
				break;
			case 'comment':
				item = new SepoaContractApprovalComment(config);
				break;
			default:
				console.error('undefined item type');
				break;
		}

		item.parentObj = this;
		item._drawBase();
		if(item.getBase()) {
			vp.appendChild(item.getBase());
			this.parentObj.containerList[name].push(item.getBase());
		}
		
		this.parentObj.component[componentName] = item;
		
		return item.getBase();
	}
}

SepoaContractApprovalHeader = function() {
}

SepoaContractApprovalHeader.prototype._drawBase = function() {
}

SepoaContractApprovalHeader.prototype.getBase = function() {
}

SepoaContractApprovalList = function(conf) {
	dhtmlx.extend(this, dhtmlx.EventSystem)
	this.conf = $.extend(true, {}, conf);
	this.objName = 'SepoaContractApprovalList';
	this.approvalList = [];

	this.init();
	this._initEvent();
}

SepoaContractApprovalList.prototype.init = function() {
	var approvalMethodList = Sepoa.cache.combo.get("SL0001,"+Sepoa.info.HOUSE_CODE+"#"+Sepoa.info.LANGUAGE+ "#CT061");
	this.conf.maxApproval = 9;
	this.approvalList = [];
	this.approvalCount = this.approvalList.length;
	this.basicFlag = this.conf && this.conf.dataOption ? false : true;
	
	if(this.basicFlag) {
		this.dataOption = [
			{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_LINE_SEQ')	, key: 'COUNT'				, width: '10%', hidden: false, blur: false, align: 'center', border: true},
			{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_METHOD')		, key: 'APP_METHOD'			, width: '15%', hidden: false, blur: false, align: 'center', border: true, code: 'CT061'},
			{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_USER_NAME')	, key: 'APP_USER_NAME'		, width: '15%', hidden: false, blur: false, align: 'center', border: true},
			{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.POSITION')		, key: 'POSITION'			, width: '15%', hidden: false, blur: false, align: 'center', border: true},
			{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.DEPARTMENT_NAME'), key: 'DEPARTMENT_NAME'	, width: '30%', hidden: false, blur: false, align: 'center', border: true},
			{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.BUTTON_DELETE')	, key: 'BUTTON'				, width: '15%', hidden: false, blur: false, align: 'center', image: (window.CONTEXT_NAME ? window.CONTEXT_NAME : '') + '/resource/design/imgs/approvalLine/minus.png'}
		];
	} else {
		this.dataOption = this.conf.dataOption;
	}
}

SepoaContractApprovalList.prototype._drawBase = function() {
	// form 메인 영역 지정
	this.base = document.createElement('div');
	this.base.className = 'set-approval-form-list-container noselect';
	this.base.style.display = 'none';
	this.base.style.flexDirection = 'column';

	//헤더 영역
	if(this.conf.header) {
		var headerWrapper = document.createElement('div');
		headerWrapper.className = 'set-approval-header-wrapper';
		this.base.appendChild(headerWrapper);
		
		var headerLabel = document.createElement('div');
		headerLabel.className = 'set-approval-header-label';
		headerWrapper.appendChild(headerLabel);

		var headerLabelText = document.createElement('div');
		headerLabelText.className = 'set-approval-header-text';
		headerLabelText.innerText = this.conf.header;
		headerLabel.appendChild(headerLabelText);
	}

	//상단 버튼 영역
	if(this.conf.header && this.conf.button) {
		for(var i in this.conf.button) {
			if(this.conf.button[i].location == 'top') {
				var headerButton = document.createElement('div');
				headerButton.className = 'set-approval-header-button button-style';
				headerWrapper.appendChild(headerButton);

				if(!this.conf.button[i].click) {
					headerButton.onclick = function() {
				        PopupOpenPost('/Contract/SetApproval/ContractSetApprovalPopup.sux'
			                , {data : this.parentObj.parentObj.getAllApproverInfo()}
			                , function(data){
			                	var component = this.parentObj.parentObj.component;
	
			                	//임시
			                	component['approval'].removeAll();
			                	component['referer'].removeAll();
	
			                	for(var i in data) {
			                		var target;
			                		if(['01', '02', '04'].indexOf(data[i].APP_METHOD) >= 0) {
			                			target = 'approval';
			                		} else if(['03'].indexOf(data[i].APP_METHOD) >= 0) {
			                			target = 'referer';
			                		}
			                		component[target].callEvent('addForm', [data[i]]);
			                		component[target].callEvent('sortApproval', []);
			                	}
			                }.bind(this)
			                , 1280
			                , 800
			                , Sepoa.cache.text.get("CONTRACT_APPROVAL.SET_APPROVAL_POPUP_TITLE")
				        );
					}.bind(this);			
				}

				var headerButtonText = document.createElement('div');
				headerButtonText.className = 'set-approval-header-text';
				headerButtonText.innerText = this.conf.button[i].text ? this.conf.button[i].text : Sepoa.cache.text.get('CONTRACT_APPROVAL.BUTTON_DEFAULT');
				headerButton.appendChild(headerButtonText);
			}
		}
	}	

	// 테이블 헤더 그리기
	this.base.appendChild(this._drawTableHeader());

	// 결재자 폼 영역
	var setApprovalFormContainer = document.createElement('div');
	setApprovalFormContainer.className = 'set-approval-container';
	setApprovalFormContainer.onclick = this._event.bind(this);
	this.base.appendChild(setApprovalFormContainer);

	// approver content 영역 지정
	var setApprovalContent = this.approvalContent = document.createElement('div');
	setApprovalContent.className = 'set-approval-content';
	setApprovalFormContainer.appendChild(setApprovalContent);

	// 하단 버튼 영역
	if(this.conf.button) {
		for(var i in this.conf.button) {
			if(this.conf.button[i].location == 'bottom') {
				if(!this.conf.button[i].customButton) {
					var setApprovalAddButtonContainer = document.createElement('div');
					setApprovalAddButtonContainer.className = 'set-approval-add-button-container';
					setApprovalFormContainer.appendChild(setApprovalAddButtonContainer);
					
					var setApprovalAddButton = document.createElement('div');
					setApprovalAddButton.className = 'set-approval-add-button';
					setApprovalAddButtonContainer.appendChild(setApprovalAddButton);	
				}
			}
		}
	}

	this.callEvent('onInit', []);
}

SepoaContractApprovalList.prototype._drawTableHeader = function() {
	var headerWrapper = document.createElement('div');
	headerWrapper.className = 'table-header-wrapper';
	
	for(var i in this.dataOption) {
		var blur = this.dataOption[i].blur;
		var clazz = 'table-header-item';
		clazz += this.dataOption[i].align ? ' table-header-item-' + this.dataOption[i].align : ' table-header-item-center';
		clazz += this.dataOption[i].hidden ? ' table-header-item-hidden' : '';

		var item = document.createElement('div');
		item.className = clazz;
		item.style.width = this.dataOption[i].width;
		item.innerText = this.dataOption[i].text;
		headerWrapper.appendChild(item);
	}

	return headerWrapper;
}

SepoaContractApprovalList.prototype.getBase = function() {
	return this.base;
}

SepoaContractApprovalList.prototype.getAllApprovalInfo = function() {
	var appStage = 1;
	return this.approvalList.map(function(e) {
		if(['04', '02'].indexOf(e.data.APP_METHOD) >= 0) {
			e.data.APP_STAGE = String(appStage++);
		} else {
			e.data.APP_STAGE = '0';
		}

		return e.data;
	});
}

SepoaContractApprovalList.prototype._initEvent = function() {
	this.attachEvent('onInit', this._onInitBase.bind(this));
	this.attachEvent('addForm', this._addForm.bind(this));
	this.attachEvent('removeForm', this._removeForm.bind(this));
	this.attachEvent('sortApproval', this._sortApproval.bind(this));
}

SepoaContractApprovalList.prototype._event = function(e) {
	var target = e.target.classList;

	if(target.contains('set-approval-add-button')) {
		if(this.approvalCount > this.conf.maxApproval) {
			Alert(Sepoa.cache.text.get("CONTRACT_APPROVAL.MAX_APP_WARNING"));
			return false;
		}
		this.callEvent('addForm', []);
	} else if(target.contains('set-approval-form-remove-area')) {
		this.callEvent('removeForm', [e]);
	}
}

SepoaContractApprovalList.prototype._onInitBase = function(e) {
	this._sortable();

	if(this.basicFlag && this.conf.defaultData === 'sample') {
		this.defaultData = {
				  APP_LINE_SEQ : String(this.approvalCount)
				, APP_METHOD : '01'
				, APP_USER_ID : Sepoa.info.ID
				, APP_USER_NAME : Sepoa.info.NAME_LOC
				, SELLER_CODE : Sepoa.info.COMPANY_CODE
				, POSITION : Sepoa.info.POSITION
				, DEPARTMENT_NAME : Sepoa.info.DEPARTMENT_NAME_LOC
				, EMPLOYEE_NO : Sepoa.info.EMPLOYEE_NO
		};
	} else if(typeof this.conf.defaultData === 'object') {
		if(!Array.isArray(this.conf.defaultData)) {
			this.conf.defaultData = [this.conf.defaultData];
		}

		this.defaultData = this.conf.defaultData

		so = doServiceAjax('sepoa.svc.ct.CT_060', 'CONNECTION', 'doQueryUser', {USER_ID : this.defaultData.map(function(e) {return e.APP_USER_ID;})}, null, false);
		if(so.status == '1') {
			var userList = new SepoaFormater(so.result[0]).getArrayMap();

			this.defaultData.map(function(e) {
				var userInfo = userList.filter(function(search) {
					return search.USER_ID.toUpperCase() == e.APP_USER_ID.toUpperCase();
				})

				if(userInfo.length > 0) {
					e.APP_USER_NAME		= userInfo[0].USER_NAME_LOC;
					e.SELLER_CODE		= userInfo[0].COMPANY_CODE;
					e.POSITION			= userInfo[0].POSITION;
					e.DEPARTMENT_NAME	= userInfo[0].DEPARTMENT_NAME;
					e.EMPLOYEE_NO		= userInfo[0].EMPLOYEE_NO;	
				}

				return e;
			});
			this.defaultData.map(function(e, i) {e.USER_CHECK_SEQ = String(i + 1)});
		}
	}

	if(this.defaultData) {
		if(Array.isArray(this.defaultData)) {
			for(var i in this.defaultData) {
				this.callEvent('addForm', [this.defaultData[i]]);
			}
		} else {
			this.callEvent('addForm', [this.defaultData]);
		}
	}
}

SepoaContractApprovalList.prototype._sortable = function(e) {
	if(this.approvalContent != null && this.approvalContent != undefined){
		var that = this;
		$(this.approvalContent).sortable({
			axis: 'y',
			containment: 'parent',
			items: '> *:not(.not-sortable)',
			stop: this._sortApproval.bind(this)
		});
	}
}

SepoaContractApprovalList.prototype._addForm = function(data) {
	this.base.style.display = 'flex';

	var approvalForm = new SepoaContractApproval();
	approvalForm.parentObj = this;
	approvalForm._drawBase(data);
	this.approvalList.push(approvalForm);
	this.approvalCount = this.approvalList.length;
	this.approvalContent.appendChild(approvalForm.getBase());
}

SepoaContractApprovalList.prototype._removeForm = function(e){
	var target = $(e.target).parents('.set-approval-cell');

	if(target != undefined && target != null) {
		var targetIndex = this.approvalList.indexOf(target[0].obj);
		this.approvalList.splice(targetIndex, 1);
		this.approvalCount = this.approvalList.length;
		target.remove();

		this.callEvent('sortApproval', []);
	}
}

SepoaContractApprovalList.prototype.removeAll = function(){
	this.base.style.display = 'none';
	this.approvalList = [];
	this.approvalCount = this.approvalList.length;
	$(this.approvalContent).find('.set-approval-cell').remove();
	this.callEvent('sortApproval', []);
}

SepoaContractApprovalList.prototype._sortApproval=function(){
	var tmp = $(this.approvalContent).find('.set-approval-cell');
	this.approvalList = [];

	var isReporterContain = tmp.filter(function(e) {return tmp[e].obj.data['APP_METHOD'] == '01';}).length > 0;
	for(var i = 0; i < tmp.length; i++) {
		this.approvalList.push(tmp[i].obj);
		tmp[i].obj.data['APP_LINE_SEQ'] = String(i);

		if(tmp[i].obj.dataElements['COUNT']) {
			if(tmp[i].obj.data['APP_METHOD'] == '01') {
				tmp[i].obj.dataElements['COUNT'].innerText = Sepoa.cache.text.get('CONTRACT_APPROVAL.REPORTER');
			} else {
				tmp[i].obj.dataElements['COUNT'].innerText = isReporterContain ? i : i + 1;
			}	
		}
	}
}

SepoaContractApproval = function() {
}

SepoaContractApproval.prototype._drawBase = function(data) {
	//create form elements area

	var cnt = this.parentObj.approvalCount;
	this.dataOption = this.parentObj.dataOption;
	this.dataElements = {};
	this.data = {};

	var setApprovalCell = this.base = document.createElement('div');
	setApprovalCell.className = 'set-approval-cell';
	setApprovalCell.obj = this;

	if(data.APP_METHOD == '01') {
		setApprovalCell.className += ' not-sortable';
		setApprovalCell.style.borderLeft = '5px solid #50B847';
	} else {
		setApprovalCell.style.borderLeft = '5px solid #00ADBD';
	}

	var itemWrapper = document.createElement('div');
	itemWrapper.className = 'table-item-wrapper';
	setApprovalCell.appendChild(itemWrapper);

	for(var i in this.dataOption) {
		var clazz = 'table-body-item';
		clazz += this.dataOption[i].blur ? ' table-body-item-blur' : '';

		var item = document.createElement('div');
		this.dataOption[i].width ? item.style.width = this.dataOption[i].width : void 0;
		this.dataOption[i].display ? item.style.display = this.dataOption[i].display : void 0;
		item.className = clazz;
		itemWrapper.appendChild(item);

		var itemOuter = document.createElement('div');
		itemOuter.className = 'table-body-item-outer';
		itemOuter.className += this.dataOption[i].align ? ' table-body-item-' + this.dataOption[i].align : ' table-body-item-center';
		item.appendChild(itemOuter);

		var itemInner = this.dataOption[i].combo ? document.createElement('select') : document.createElement('div');
		itemInner.className = 'table-body-item-inner';
		itemInner.className += this.dataOption[i].align ? ' table-body-item-' + this.dataOption[i].align : ' table-body-item-center';
		itemInner.className += this.dataOption[i].border ? ' table-body-item-border' : '';
		if(this.dataOption[i].key) {
			itemInner.setAttribute('data-key', this.dataOption[i].key)
			this.dataElements[this.dataOption[i].key] = itemInner;
		}

		if(this.dataOption[i].combo) {
			var combo = Sepoa.cache.combo.get('SL0001,' + Sepoa.info.HOUSE_CODE + '#' + Sepoa.info.LANGUAGE + '#' + this.dataOption[i].combo);
			for(var i in combo) {
				var option = $('<option value="' + combo[i].value + '">' + combo[i].text + '</option>')[0];
				itemInner.appendChild(option);
			}
			itemInner.cell = this;
			itemInner.onchange = function(e) {
				this.cell.data.APP_METHOD = this.value;
			}
		} else if(this.dataOption[i].image) {
			itemInner.className = itemInner.className + ' table-body-item-image';
			itemInner.style.backgroundImage = 'url(' + this.dataOption[i].image + ')';
			
			if(this.dataOption[i].key == 'BUTTON' && this.dataOption[i].text == Sepoa.cache.text.get('CONTRACT_APPROVAL.BUTTON_DELETE')) {
				itemInner.className += ' set-approval-form-remove-area';
			}

			if(data.APP_METHOD == '01') {
				itemInner.classList.toggle('set-approval-form-remove-area');
				itemInner.classList.toggle('table-body-item-image');
				itemInner.style.backgroundImage = '';
			}
		} else if(this.dataOption[i].key === 'COUNT') {
			if(data.APP_METHOD == '01') {
				itemInner.innerText = Sepoa.cache.text.get('CONTRACT_APPROVAL.REPORTER');
			} else {
				var isReporterContain = this.parentObj.approvalList.filter(function(e) {return e.base.classList.contains('not-sortable')}).length;
				itemInner.innerText = isReporterContain ? cnt : cnt + 1;
			}
		}

		itemOuter.appendChild(itemInner);
	}
	
	if(data) {
		this._setData(data);
	}
}

SepoaContractApproval.prototype._setData = function(data) {
	for(var i in data) {
		if(this.dataElements[i]) {
			var codeData = this.dataOption.filter(function(e) {return e.code && e.key == i});

			if(i === 'POSITION') {
				var positionName = Sepoa.cache.combo.get("SL0001," + Sepoa.info.HOUSE_CODE + '#' + Sepoa.info.LANGUAGE + "#" + 'UX049').filter(function(e) {
					return e.value === data[i];
				});
				this.dataElements[i].innerText = positionName.length > 0 ? positionName[0].text : '';
			} else if(this.dataElements[i].tagName == 'SELECT') {
				this.dataElements[i].value = data[i];
			} else if (codeData.length > 0) {
				var code = codeData.map(function(e) {return e.code})[0];
				this.dataElements[i].innerText = Sepoa.cache.combo.get('SL0001,' + Sepoa.info.HOUSE_CODE + '#' + Sepoa.info.LANGUAGE + '#' + code).filter(function(e) {return e.value == data[i];})[0].text
			} else {
				this.dataElements[i].innerText = data[i] ? data[i] : '';
			}
		}

		this.data[i] = data[i];
	}
	
	var reqItem = ['APP_METHOD', 'APP_LINE_SEQ', 'APP_USER_ID'];
	for(var i = 0; i < reqItem.length; i++) {
		if(Object.keys(this.data).indexOf(reqItem[i]) < 0) {
			Alert('Required [' + reqItem[i] + '] data');
			this.callEvent('data init event', []);
		}
	}
}

SepoaContractApproval.prototype.getBase = function() {
	return this.base;
}

SepoaContractApprovalComment = function(conf) {
	this.conf = $.extend(true, {}, conf);
}

SepoaContractApprovalComment.prototype._drawBase = function() {
	// 상신의견 영역 추가
	this.base = document.createElement('div');
	this.base.className = 'set-approval-comment-cotainer';
	
	var commentTitleWrapper = document.createElement('div');
	commentTitleWrapper.className = 'set-approval-comment-title-wrapper';
	this.base.appendChild(commentTitleWrapper);

	var commentTitle = document.createElement('div');
	commentTitle.className = 'set-approval-comment-title';
	commentTitle.innerText = Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_TABBAR_COMMENT');
	commentTitleWrapper.appendChild(commentTitle);
	
	var commentContentsWrapper = document.createElement('div');
	commentContentsWrapper.className = 'set-approval-comment-contents-wrapper';
	this.base.appendChild(commentContentsWrapper);

	var commentContents = document.createElement('textarea');
	commentContents.className = 'set-approval-comment-contents';
	commentContents.id = 'comment_edit_box';
	commentContents.placeholder = Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_COMMENT_PLACEHOLDER');
	if(this.conf.defaultData) {
		commentContents.value = this.conf.defaultData;
	}

	commentContentsWrapper.appendChild(commentContents);
}

SepoaContractApprovalComment.prototype.getBase = function() {
	return this.base;
}

//상신 의견 추출
SepoaContractApprovalComment.prototype.getAppComment = function(approverInfo) {
	return document.querySelector("#comment_edit_box").value;
}

SepoaContractApprovalPopup = function(vp, params) {
	this.base = vp;
	this.conf = $.extend(true, {
		data:[],
		comment : true,
		language:'/resource/ux/langs/'+Sepoa.info.LANGUAGE+'/APPROVAL_LINE.js' //language file path
	}, params);
	
	Sepoa.cache.text.load({
		url:this.conf.language,
		def:CONTEXT_NAME+'/resource/ux/langs/KO/APPROVAL_LINE.js'
	});
	
	this.init();
}

SepoaContractApprovalPopup.prototype.init = function() {
	this.tabbar = {};
	this._drawBase();
}

SepoaContractApprovalPopup.prototype._drawBase = function() {
	var wrapper = document.createElement('div');
	wrapper.className = 'set-approval-popup-wrapper';
	this.base.appendChild(wrapper);

	var tabbarContainer = document.createElement('div');
	tabbarContainer.className = 'set-approval-tabbar-container';
	tabbarContainer.style.height = this.conf.comment ? '65%' : '95%';
	wrapper.appendChild(tabbarContainer);

	var tabbar1 = this.tabbar['tabbar1'] = new SepoaContractApprovalTabbar(['chart', 'preset'], this.conf);
	tabbar1.parentObj = this;
	tabbar1.base.style.marginRight = '2.5%';
	tabbarContainer.appendChild(tabbar1.base);

	var tabbar2 = this.tabbar['tabbar2'] = new SepoaContractApprovalTabbar(['approval', 'referer'], this.conf, this.conf.data)
	tabbar2.parentObj = this;
	tabbar2.base.style.marginLeft = '2.5%';
	tabbarContainer.appendChild(tabbar2.base);

	if(this.conf.comment) {
		var commentContainer = document.createElement('div');
		commentContainer.className = 'set-approval-comment-container';
		commentContainer.style.height = '30%';
		wrapper.appendChild(commentContainer);
		
		var comment = this.comment = new SepoaContractApprovalComment();
		comment._drawBase();
		commentContainer.appendChild(comment.getBase());	
	}

	var buttonContainer = document.createElement('div');
	buttonContainer.className = 'set-approval-button-container';
	wrapper.appendChild(buttonContainer);
	
	var buttonWrapper = document.createElement('div');
	buttonWrapper.className = 'set-approval-button-wrapper';
	buttonContainer.appendChild(buttonWrapper);
	
	var buttonSavePreset = document.createElement('div');
	buttonSavePreset.className = 'set-approval-button-save-preset button-style';
	buttonSavePreset.innerText = Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_BUTTON_PRESET');
	buttonSavePreset.onclick = function(e) {
		dhtmlx.confirm(Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_MESSAGE_CONFIRM_PRESET'), function(result){
			if(result) {
				var params = {
						mode	: 'EDIT',	// VIEW, EDIT
						config	: {
							placeholder : Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_BUTTON_PRESET_NAME')
						}
				};

				var popupCallback = function(formObj) {
					for(var i in this.tabbar) {
						if(this.tabbar[i].preset) {
//							var targetPreset = this.tabbar[i].preset.filter(function(e) {return e.element.classList.contains('set-approval-preset-selected')});
//							var params = {
//								DATA : JSON.stringify(this.getAllApproverInfo())
//							}
//
//							if(targetPreset.length > 0) {
//								params.APP_LINE_CODE = targetPreset[0].APP_LINE_CODE;
//								params.APP_LINE_NAME = $(targetPreset[0].element).find('input').val();
//							} else {
//								//params.APP_LINE_NAME = prompt(Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_BUTTON_PRESET_NAME'));
//								params.APP_LINE_NAME = formObj.getValue('INPUT');
//								
//								if(params.APP_LINE_NAME === null) {
//									return false;
//								}
//							}
							var params = {
								DATA : JSON.stringify(this.getAllApproverInfo()),
								APP_LINE_NAME : formObj.getValue('INPUT')
							}

							if(params.APP_LINE_NAME.trim() == '') {
								Alert(Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_BUTTON_PRESET_NAME'));
								return false;
							}

							var so = doServiceAjax('sepoa.svc.ct.CT_060', 'TRANSACTION', 'doSaveMyApp', params, null, false);
							if(so.status == '1') {
								this.tabbar[i]._loadPreset();
							}
						}
					}
				}.bind(this);

				var popup = PopupOpenPost('/common/Popup/InputWindow.sux', 
						params,
						popupCallback,
						350,
						150,
						Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_BUTTON_PRESET')
				);
			}
		}.bind(this));

//		if(!confirm(Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_MESSAGE_CONFIRM_PRESET'))) {
//			return false;
//		}

	}.bind(this);
	buttonWrapper.appendChild(buttonSavePreset);

	var buttonSelectApprovalLine = document.createElement('div');
	buttonSelectApprovalLine.className = 'set-approval-button-approvalLine button-style';
	buttonSelectApprovalLine.innerText = Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_BUTTON_SAVE');
	buttonSelectApprovalLine.onclick = function() {
		var result = {};
		result.approver = this.conf.data = this.getAllApproverInfo();
		
		if(this.comment) {
			result.comment = this.comment.getAppComment();
		}
		
		this.conf.callback(result);
		selfObj.close();
	}.bind(this);
	buttonWrapper.appendChild(buttonSelectApprovalLine);

	SUX.callEvent("onWindowResize");
}

SepoaContractApprovalPopup.prototype.getAllApproverInfo = function() {
	for(var i in this.tabbar) {
		if(this.tabbar[i].component) {
			var component = this.tabbar[i].component;
			var result = this.conf.data[0] ? [this.conf.data[0]] : [];
			for(var i in component) {
				if(component[i].objName == 'SepoaContractApprovalList') {
					result = result.concat(component[i].getAllApprovalInfo());
				}
			}
			return result.map(function(e, i) {e.APP_LINE_SEQ = String(i); return e});
		}
	}
	
	return [this.conf.data[0]];
}

SepoaContractApprovalTabbar = function(drawList, conf, data) {
	this.tabs = [];
	this.conf = $.extend(true, {}, conf);
	this.data = $.extend(true, [], data);
	var itemList = ['chart', 'preset', 'approval', 'referer'];
	for(var i in drawList) {
		if(itemList.indexOf(drawList[i]) >= 0) {
			var list = {};
			list[drawList[i]] = {};
			this.tabs.push(list);
		}
	}

	this.base = this._drawBase();
}

SepoaContractApprovalTabbar.prototype._drawBase = function() {
	var tabbarWrapper = document.createElement('div');
	tabbarWrapper.className = 'set-approval-tabbar-wrapper container';
	
	var tabbar = document.createElement('div');
	tabbar.className = 'set-approval-tabbar';
	tabbarWrapper.appendChild(tabbar);
	
	var tabs = [];
	for(var i in this.tabs) {
		var active = i == 0 ? true : false;
		tabs.push({id : Object.keys(this.tabs[i])[0], text : Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_TABBAR_' + Object.keys(this.tabs[i])[0].toUpperCase()), width : 100, index : i, active : active, close : false });
	}
	
	this.tabbarObj = new SepoaDhtmlXTabBar({
		  parent:tabbar
		, mode:'top'
		, tabs: tabs
	});

	for(var i in tabs) {
		var item;
		switch(tabs[i].id) {
			case 'chart':
				this.treeView = new SepoaTreeView(this.conf);
				this.treeView.parentObj = this;
				item = this.treeView.base;
				break;
			case 'preset':
				item = this._drawPreset();
				break;
			case 'approval':
				item = this._drawApproval();
				break;
			case 'referer':
				item = this._drawReferer();
				break;
			default:
				console.error('not supported item');
				break;
		}

		item.style.display = 'flex';
		this.tabbarObj.cells(tabs[i].id).attachObject(item);
	}

	this.tabbarObj.getActiveTab();
	return tabbarWrapper;
}

SepoaContractApprovalTabbar.prototype._drawPreset = function() {
	var presetContainer = document.createElement('div');
	presetContainer.className = 'set-approval-preset-container';
	presetContainer.style.height = '100%';
	
	var presetWrapper = document.createElement('div');
	presetWrapper.className = 'set-approval-preset-wrapper';
	presetContainer.appendChild(presetWrapper);

	var presetFilterWrapper = document.createElement('div');
	presetFilterWrapper.className = 'set-approval-filter-wrapper';
	presetWrapper.appendChild(presetFilterWrapper);
	
	var presetSearch = function() {
		var result = this.preset.filter(function(e) {return e.APP_LINE_NAME.indexOf(presetFilterInput.value) >= 0});
		var targetIndex = 0;
		result.forEach(function(e, i) {
			if(e.element.classList.contains('set-approval-preset-selected')) {
				targetIndex = i + 1;
			}
		});

		targetIndex = targetIndex >= result.length ? 0 : targetIndex;
		selectPreset(result[targetIndex].element);
	}.bind(this);
	
	var presetFilterInput = document.createElement('input');
	presetFilterInput.className = 'set-approval-filter-input';
	presetFilterInput.onkeydown = function(e) {
		if(e.keyCode == 13) {
			presetSearch();
		}
	}
	presetFilterWrapper.appendChild(presetFilterInput);

	var presetFilterButton = document.createElement('div');
	presetFilterButton.className = 'set-approval-filter-button button-style';
	presetFilterButton.onclick = presetSearch;
	presetFilterWrapper.appendChild(presetFilterButton);

	var presetContentsWrapper = this.presetBase = document.createElement('div');
	presetContentsWrapper.className = 'set-approval-preset-wrapper';
	presetWrapper.appendChild(presetContentsWrapper);
	presetContentsWrapper.onclick = function(e) {
		var target;

		if(e.path) {
			target = e.path.filter(function(e) {
			    if(e.classList) {
			        return e.classList.contains('set-approval-preset');
			    }
			    return false;
			});
			
			target = target.length > 0 ? target[0] : undefined;
			selectPreset(target);
		}
	};

//	this._loadContextMenu();
	this._loadPreset();
	
	var selectPreset = function(target) {
		var renameElement = $('.set-approval-preset-text-rename');
		if(renameElement.length > 0) {
			renameElement[0].readOnly = true;
			renameElement[0].classList.remove('set-approval-preset-text-rename');
		}
		
		$('.set-approval-preset').removeClass('set-approval-preset-selected');

		if(target) {
			$(target).addClass('set-approval-preset-selected');	
		}
	}

	this.contextTemplate = {
//		'rename' : {
//			name: Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_CONTEXT_RENAME'),
//			callback: function(key, opt, e){
//				selectPreset(opt.$trigger[0]);
//				var target = opt.$trigger.find('input')[0];
//				target.classList.toggle('set-approval-preset-text-rename');
//				target.readOnly = false;
//			}
//		},
		'delete' : {
			name: Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_CONTEXT_DELETE'),
			callback: function(key, opt, e){
				selectPreset(opt.$trigger[0]);
				if(confirm(Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_MESSAGE_DELETE_PRESET'))) {
					var params = {APP_LINE_CODE : opt.$trigger[0].value};
					var so = doServiceAjax('sepoa.svc.ct.CT_060', 'TRANSACTION', 'doDeleteMyApp', params, null, false);
					if(so.status == '1') {
						this._loadPreset();
					} else {
						console.log(so.message);
					}
				}
			}.bind(this)
		}
	};

	//loadScript('/Contract/Module/js/ContextMenu.js', function(){
	loadScript((window.CONTEXT_NAME ? window.CONTEXT_NAME : '') + '/resource/plugin/sepoasoft/contextmenu/contextMenu.js', function(){
		this.contextMenu = new SepoaContextMenuManager();
		this.contextMenu.setContextMenu('.set-approval-preset', {trigger : 'right', items : this.contextTemplate});
	}.bind(this));


	return presetContainer;
}

SepoaContractApprovalTabbar.prototype._loadPreset = function() {
	var so = doServiceAjax('sepoa.svc.ct.CT_060', 'CONNECTION', 'doQuerySGWMG', [], null, false);
	if(so.status == '1') {
		if(this.presetBase.childElementCount > 0) {
			$(this.presetBase).empty();
		}

		var data = this.preset = new SepoaFormater(so.result[0]).getArrayMap();
		for(var i in data) {
			var presetContents = this.preset[i].element = document.createElement('div');
			presetContents.className = 'set-approval-preset noselect';
			presetContents.value = data[i].APP_LINE_CODE;
			presetContents.ondblclick = this._setPreset.bind(this);
			presetContents.obj = this;
			this.presetBase.appendChild(presetContents);

			var presetIcon = document.createElement('div');
			presetIcon.className = 'set-approval-preset-icon';
			presetContents.appendChild(presetIcon);

			var presetText = document.createElement('input');
			presetText.className = 'set-approval-preset-text noselect';
			presetText.readOnly = true;
			presetText.value = data[i].APP_LINE_NAME;
			presetText.onclick = function(e) {
				if(!e.target.readOnly) {
					e.stopImmediatePropagation();	
				}
			}
//			presetText.onblur = function(e) {
//				if(!e.target.readOnly) {
//					e.target.classList.toggle('set-approval-preset-text-rename');
//					e.target.readOnly = true;
//				}
//			}
			presetContents.appendChild(presetText);
		}
	}	
}

SepoaContractApprovalTabbar.prototype._loadContextMenu = function() {
	if(this.contextLibraryList && this.contextLibraryList.filter(function(e) {return e.loaded === false}).length <= 0) {
		return false;
	}
	
	var urlList = this.contextLibraryList = [
		    {url : '/Contract/Module/js/contextmenu/jquery.contextMenu.js',		loaded : false}
		  , {url : '/Contract/Module/js/contextmenu/jquery.ui.position.js',		loaded : false}
		  , {url : '/Contract/Module/css/contextmenu/jquery.contextMenu.css',	loaded : false}
	];
	
	var setLoaded = function(e) {
		var link = e.path[0].src ? e.path[0].src : e.path[0].href;
		for(var i in this.contextLibraryList) {
			if(link.indexOf(this.contextLibraryList[i].url) >= 0) {
				this.contextLibraryList[i].loaded = true;
			}
		}
		
		if(this.contextLibraryList.filter(function(e) {return e.loaded === false}).length <= 0) {
			this.isContextLoaded = true;
		}
	}.bind(this);

	for (var i in urlList) {
		var ext = urlList[i].url.split('.');
		if (ext[ext.length - 1] == 'js') {
			loadScript(urlList[i].url, setLoaded);
		} else if(ext[ext.length - 1] == 'css') {
			loadCss(urlList[i].url, setLoaded);
		}
	}
}

SepoaContractApprovalTabbar.prototype._setPreset = function(e) {
	if(!e.currentTarget.classList.contains('set-approval-preset')) {
		return false;
	}

	var params = {
		APP_LINE_CODE : e.currentTarget.value
	}
	var so = doServiceAjax('sepoa.svc.ct.CT_060', 'CONNECTION', 'doQuerySGWML', params, null, false);
	var result = new SepoaFormater(so.result[0]).getArrayMap();

	var tabbarList = this.parentObj.tabbar;
	for(var i in tabbarList) {
	    if(tabbarList[i].component) {
			var approvalData = result.filter(function(e) {return ['04', '02'].indexOf(e.APP_METHOD) >= 0}).sort(function(a,b) {
				if (Number(a.APP_LINE_SEQ) < Number(b.APP_LINE_SEQ)) {
					return -1;
				}
				if (Number(a.APP_LINE_SEQ) > Number(b.APP_LINE_SEQ)) {
					return 1;
				}

				return 0;
			});

			tabbarList[i].component.approval.removeAll();
			tabbarList[i].component.approval.base.style.display = 'flex';
			for(var j in approvalData) {
				tabbarList[i].component.approval.callEvent('addForm', [approvalData[j]]);
			}

			var refererData = result.filter(function(e) {return ['03'].indexOf(e.APP_METHOD) >= 0}).sort(function(a,b) {
				if (Number(a.APP_LINE_SEQ) < Number(b.APP_LINE_SEQ)) {
					return -1;
				}
				if (Number(a.APP_LINE_SEQ) > Number(b.APP_LINE_SEQ)) {
					return 1;
				}

				return 0;
			});
			tabbarList[i].component.referer.removeAll();
			tabbarList[i].component.referer.base.style.display = 'flex';
			for(var j in refererData) {
				tabbarList[i].component.referer.callEvent('addForm', [refererData[j]]);
			}
	    }
	}
}

SepoaContractApprovalTabbar.prototype._drawApproval = function() {
	var approvalOption = this.approvalOption = [
		{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_METHOD')		, key: 'APP_METHOD'			, width: '20%', hidden: false, blur: false, align: 'center', combo: 'CT061_1'},
		{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_USER_NAME')	, key: 'APP_USER_NAME'		, width: '20%', hidden: false, blur: false, align: 'center'},
		{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.DEPARTMENT_NAME'), key: 'DEPARTMENT_NAME'	, width: '30%', hidden: false, blur: false, align: 'center'},
		{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.POSITION')		, key: 'POSITION'			, width: '20%', hidden: false, blur: false, align: 'center'},
		{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.BUTTON_DELETE')	, key: 'BUTTON'				, width: '10%', hidden: true, blur: false, align: 'right', image: (window.CONTEXT_NAME ? window.CONTEXT_NAME : '') + '/resource/design/imgs/approvalLine/minus.png'}
	];
	
	if(!this.component) {
		this.component = {};
	}
	
	var approval = this.component.approval = new SepoaContractApprovalList({dataOption : approvalOption});
	approval.parentObj = this;
	approval.type = 'approval';
	approval._drawBase();
	approval.base.obj = approval;

	var approvalData = this.data.filter(function(e) {return ['04', '02'].indexOf(e.APP_METHOD) >= 0});
	for(var i in approvalData) {
		approval.callEvent('addForm', [approvalData[i]]);
	}

	return approval.getBase();
}

SepoaContractApprovalTabbar.prototype._drawReferer = function() {
	var refererOption = this.refererOption = [
		{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.APP_USER_NAME')	, key: 'APP_USER_NAME'		, width: '20%', hidden: false, blur: false, align: 'center'},
		{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.DEPARTMENT_NAME'), key: 'DEPARTMENT_NAME'	, width: '30%', hidden: false, blur: false, align: 'center'},
		{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.POSITION')		, key: 'POSITION'			, width: '20%', hidden: false, blur: false, align: 'center'},
		{text: Sepoa.cache.text.get('CONTRACT_APPROVAL.BUTTON_DELETE')	, key: 'BUTTON'				, width: '30%', hidden: true, blur: false, align: 'right', image: (window.CONTEXT_NAME ? window.CONTEXT_NAME : '') + '/resource/design/imgs/approvalLine/minus.png'}
	];
	
	if(!this.component) {
		this.component = {};
	}
	
	var referer = this.component.referer = new SepoaContractApprovalList({dataOption : refererOption});
	referer.parentObj = this;
	referer.type = 'referer';
	referer._drawBase();
	referer.base.obj = referer;

	var refererData = this.data.filter(function(e) {return ['03'].indexOf(e.APP_METHOD) >= 0});
	for(var i in refererData) {
		referer.callEvent('addForm', [refererData[i]]);
	}

	return referer.getBase();
}

SepoaTreeView = function(conf) {
	dhtmlx.extend(this, dhtmlx.EventSystem)
	this.conf = $.extend(true, {
		dynamicLoad : true
	}, conf);
	this.init(this.conf);
}

SepoaTreeView.prototype.init = function(conf) {
	this._drawBase(this.conf.base);
}

SepoaTreeView.prototype._drawBase = function(vp) {
	
	var treeContainer = this.base = document.createElement('div');
	treeContainer.className = 'sepoa-tree-container';
	treeContainer.style.height = '100%';
	if(vp) {
		vp.appendChild(this.base);	
	}

	var treeWrapper = document.createElement('div');
	treeWrapper.className = 'sepoa-tree-wrapper';
	treeContainer.appendChild(treeWrapper);
	
	var treeFilterWrapper = document.createElement('div');
	treeFilterWrapper.className = 'sepoa-tree-filter-wrapper set-approval-filter-wrapper';
	treeWrapper.appendChild(treeFilterWrapper);
	
	var treeSearch = function(){
		if(!this.tree) {
			return;
		}

		this.tree.search(treeFilterInput.value, function(result) {
			this.tree.focusItem(result);
		}.bind(this));
	}.bind(this);

	var treeFilterInput = document.createElement('input');
	treeFilterInput.className = 'sepoa-tree-filter-input set-approval-filter-input';
	treeFilterInput.onkeydown = function(e) {
		if(e.keyCode == 13) {
			treeSearch();
		}
	}
	treeFilterWrapper.appendChild(treeFilterInput);

	var treeFilterButton = document.createElement('div');
	treeFilterButton.className = 'sepoa-tree-filter-button set-approval-filter-button button-style';
	treeFilterButton.onclick = treeSearch;
	treeFilterWrapper.appendChild(treeFilterButton);
	
	var treeInnerWrapper = document.createElement('div');
	treeInnerWrapper.classList.add('sepoa-tree-inner-wrapper');
	treeWrapper.appendChild(treeInnerWrapper);
	
	var treeContents = document.createElement('div');
	treeContents.className = 'sepoa-tree';
	treeContents.style.width = '100%'
	treeContents.style.height = '100%'
	treeInnerWrapper.appendChild(treeContents);

	var conf = {
		parent: 		treeContents,
		skin: 			'material',
		multiselect:    true,
		dnd:            false,
	    context_menu:   false
	};
	var treeView = this.tree = new SepoaDhtmlXTreeView(conf);
	
	this._initTreeEvent();
	this._initCustomFunction();
	this.tree.callEvent('initData', []);
}

SepoaTreeView.prototype._initTreeEvent = function() {
	if(!this.tree) {
		return;
	}
	
	var that = this;

	//Data Event area
	this.tree.attachEvent('initData', function(){
		if(this.conf.dynamicLoad) {
			var so = doServiceAjax('sepoa.svc.ct.CT_060', 'CONNECTION', 'doQueryDept', {'DEPT_IS_NULL' : 'true'}, null, false);
			if(so.status == '1') {
				var deptData = new SepoaFormater(so.result[0]).getArrayMap();
				this.setTreeItem(deptData);
			}
		} else {
			this.setTreeData();
		}
		this.tree.callEvent('onXLE', []);
	}.bind(this));

	this.tree.attachEvent('onXLE', function() {
		SUX.callEvent("onWindowResize");
		this.tree.enableContextMenu(true);
		this.tree.setSizes();

		if(this.tree.base.clientWidth === 0 || this.tree.base.clientHeight === 0) {
			var loadTimer = setInterval(function(){
				if(this.tree.base.clientHeight > 0) {
					this.tree.setSizes();
					clearInterval(loadTimer);
				}
			}.bind(this), 1);
		}
	}.bind(this));

	//Mouse Event area
	if(this.conf.dynamicLoad) {
		document.body.addEventListener('click', function(e) {
			if($(e.target).parents('.dhxtreeview_item_DEPT').length <= 0) {
				return false;
			}
			
			var data = this.tree.items[this.tree.getSelectedId()].userdata.DATA;
			if(data.TYPE != 'DEPT') {
				return false;
			}

			if(this.tree.getSubItems(this.tree.getSelectedId()[0]).length <= 0) {
				this.setTreeData(data);
			}

		}.bind(this))
	}

	document.body.addEventListener('dblclick', function(e) {
		if($(e.target).parents('.dhxtreeview_item_EMPLOYEE').length <= 0) {
			return false;
		}

		var data = this.tree.items[this.tree.getSelectedId()].userdata.DATA;

		var target = $('.dhx_cell_tabbar').filter(function(e, i) {
			return i.style.visibility == 'visible' && $(i).find('.set-approval-form-list-container').length > 0
		}).find('.set-approval-form-list-container')[0];

		if(target) {
			target = target.obj;
			this._setTargetData(target, data);
		}
	}.bind(this))
	
	document.body.addEventListener('dragstart', function(e) {
		if(!e.target.classList.contains('dhxtreeview_item_EMPLOYEE')) {
			return false;
		}

		var data = this.tree.getSelectedId().filter(function(e) {
			return this.tree.items[e].userdata.DATA.TYPE != 'DEPT';
		}.bind(this));

		e.dataTransfer.setData("text", data[0]);
		
		if(navigator.userAgent.indexOf('Trident') == -1) {
			e.dataTransfer.setDragImage($('<div></div>')[0], 0, 0);	
		}

	}.bind(this))
	
	document.body.addEventListener("dragover", function(e) {
		//드롭을 허용하도록 prevetDefault() 호출
		e.preventDefault();	
	});
		
	document.body.addEventListener('drop', function(e) {
		if(e.target.length <= 0 || !e.target.classList.contains('set-approval-form-list-container')) {
			return false;
		}

		var list = e.dataTransfer.getData("text").split(',');
		for(var i in list) {
			var data = this.tree.items[list[i]].userdata.DATA;
			var target = e.target;
			target = target ? target.obj : e.target.children[0].obj;

			this._setTargetData(target, data);
		}
	}.bind(this));

	//ContextMenu Event area
	if(this.context) {
		this.tree.attachEvent('onContextMenu', function(id, x, y, ev){
		    // show context menu here
			this.context.setItemText('itemText', this.tree.getItemText(id));
			this.context.showContextMenu(x, y);

		    // optionally, you can select treeview item
			this.tree.selectItem(id);

		    // prevent default context menu
		    return false;
		}.bind(this));
	}
}

SepoaTreeView.prototype.getDeptData = function(target) {
	var params = {};
	if(target && target.ID) {
		params.DEPT = target.ID;
	}
	
	return doServiceAjax('sepoa.svc.ct.CT_060', 'CONNECTION', 'doQueryDept', params, null, false);
}

SepoaTreeView.prototype.getUserData = function(target) {
	var params = {};
	if(target && target.ID) {
		params.DEPT = target.ID;
	}

	return doServiceAjax('sepoa.svc.ct.CT_060', 'CONNECTION', 'doQueryUser', params, null, false);
}

SepoaTreeView.prototype.setTreeItem = function(data) {
	var deptData = data.filter(function(e){return e.TYPE == 'DEPT'});
	var userData = data.filter(function(e){return e.TYPE == 'EMPLOYEE'});
	
	if(deptData.length > 0) {
		deptData = this._customSort(deptData);

		for(var i in deptData) {
			if(this.tree.items[deptData[i].ID]) {
				continue;
			}

			if(deptData[i].ID === '1') {
				deptData[i].TYPE = 'CEO';
			}

			this.tree.addItem(deptData[i].ID, deptData[i].TEXT, deptData[i].PARENTID ? deptData[i].PARENTID : '');
			this.tree.setUserData(deptData[i].ID, 'DATA', deptData[i]);
			this.tree.items[deptData[i].ID].item.className += ' dhxtreeview_item_' + deptData[i].TYPE;
			$(this.tree.items[deptData[i].ID].item).find('.dhxtreeview_icon')[0].style.backgroundImage = 'url(/resource/design/imgs/approvalLine/icon_folder_closed.gif)';

			if(deptData[i].TYPE === 'CEO') {
				var parent = this.tree.items[deptData[i].ID].item.parentElement;
				parent.insertBefore(this.tree.items[deptData[i].ID].item, parent.firstChild);
			}
		}
	}

	if(userData.length > 0) {
		var positionNameList = Sepoa.cache.combo.get("SL0001," + Sepoa.info.HOUSE_CODE + '#' + Sepoa.info.LANGUAGE + "#" + 'UX049');

		for(var i in userData) {
			if(this.tree.items[userData[i].USER_ID]) {
				continue;
			}

			var positionName = positionNameList.filter(function(e){return e.value == userData[i].POSITION});
			positionName = positionName.length > 0 ? positionName[0].text : '';
			this.tree.addItem(userData[i].USER_ID, userData[i].USER_NAME_LOC + ' ' + positionName, userData[i].DEPT);
			this.tree.setUserData(userData[i].USER_ID, 'DATA', userData[i]);
			this.tree.items[userData[i].USER_ID].item.className += ' dhxtreeview_item_' + userData[i].TYPE;
			this.tree.items[userData[i].USER_ID].item.obj = this;

			if(!this.tree.items[userData[i].USER_ID].pId) {
				this.tree.deleteItem(userData[i].USER_ID);
				continue;
			}

			var parent = this.tree.items[userData[i].USER_ID].item.parentElement;
			parent.insertBefore(this.tree.items[userData[i].USER_ID].item, parent.firstChild);
			this.tree.items[userData[i].USER_ID].item.draggable = true;
			this.tree.items[userData[i].USER_ID].item.onmousedown = function(e) {
				this.obj.tree.selectItem(this._itemId);
			};
		}
	}
}

SepoaTreeView.prototype.setTreeData = function(target) {
	var so1 = this.getDeptData(target);
	if(so1.status == '1') {
		var deptData = new SepoaFormater(so1.result[0]).getArrayMap();
		this.setTreeItem(deptData);
	}
	
	var so2 = this.getUserData(target);
	if(so2.status == '1') {
		var userData = new SepoaFormater(so2.result[0]).getArrayMap();
		this.setTreeItem(userData);
	}
}

SepoaTreeView.prototype._customSort = function(data) {
	var result = [];
	var recursiveSort = function(target) {
		if(target.TYPE != 'DEPT') {
			return;
		}

		var targetParent = data.filter(function(e){return e.ID == target.PARENTID && e.ID != target.ID});
		if(targetParent.length > 0) {
			if(result.indexOf(targetParent[0]) < 0) {
				recursiveSort(targetParent[0]);
				result.push(target);
			}
		}

		if(result.indexOf(target) < 0) {
			result.push(target)
		}
	}

	for(var i in data) {
		recursiveSort(data[i]);
	}

	return result;
}

SepoaTreeView.prototype._setTargetData = function(target, data) {
	for(var j in target.approvalList) {
		if(data.USER_ID === target.approvalList[j].data.APP_USER_ID) {
			return false;
		}
	}

	var params = {
		APP_USER_ID : data.USER_ID,
		APP_USER_NAME : data.USER_NAME_LOC,
		SELLER_CODE : data.COMPANY_CODE,
		POSITION : data.POSITION,
		EMPLOYEE_NO : data.EMPLOYEE_NO,
		DEPT : data.DEPT,
		DEPARTMENT_NAME : this.tree.items[data.DEPT].text,
		APP_LINE_SEQ : String(target.approvalCount + 1)
	};

	if(target.type == 'approval') {
		params.APP_METHOD = '04';
	} else if(target.type == 'referer') {
		params.APP_METHOD = '03';
	}

	target.callEvent('addForm', [params]);
}

SepoaTreeView.prototype._initCustomFunction = function() {
	if(!this.tree) {
		return;
	}

	this.tree.search = function(keyword, callback) {
		var values = Object.keys(this.tree.items).map(function(e) {return this.tree.items[e];}.bind(this));	//obj -> arr
		var result = values.filter(function(e) {
			if(e.userdata && ['DEPT', 'COMPANY'].indexOf(e.userdata.DATA.TYPE) >= 0) {
				return e.text.indexOf(keyword) >= 0; 
			} else {
				return e.id.indexOf(keyword.toUpperCase()) >= 0 || e.text.indexOf(keyword) >= 0;	
			}
		});

		if(this.conf.dynamicLoad) {
			doServiceAjax('sepoa.svc.ct.CT_060', 'CONNECTION', 'doSearch', {'KEYWORD' : keyword}, function(so) {
				if(so.status == 1) {
					var data = JSON.parse(so.result[0]);

					if(result.length != data.length) {
						doServiceAjax('sepoa.svc.ct.CT_060', 'CONNECTION', 'doQuerySearchTreeData', {'TARGET' : data}, function(so2) {
							if(so2.status == 1) {
								var treeData = JSON.parse(so2.result[0]);
								this.setTreeItem(treeData);

								values = Object.keys(this.tree.items).map(function(e) {return this.tree.items[e];}.bind(this));	//obj -> arr
								result = values.filter(function(e) {
									if(e.userdata && ['DEPT', 'COMPANY'].indexOf(e.userdata.DATA.TYPE) >= 0) {
										return e.text.indexOf(keyword) >= 0; 
									} else {
										return e.id.indexOf(keyword.toUpperCase()) >= 0 || e.text.indexOf(keyword) >= 0;	
									}
								});

								callback(result);
							}
						}.bind(this));
					} else {
						callback(result);
					}
				}
			}.bind(this));
		} else {
			callback(result);
		}

		return result;
	}.bind(this);
	
	this.tree.focusItem = function(target) {
		if(typeof target === 'string') {
			target = [target];
		} else {
			if(Array.isArray(target) && target.length > 0 && target[0].id) {
				target = target.map(function(e) {return e.id});
			} else {
				return;
			}
		}

		function setItemList(id, list) {
			if(this.items[id].pId) {
				list.splice(0, 0, this.items[id].pId);
				return setItemList.call(this, this.items[id].pId, list);
			} else {
				return list;
			}
		};

		var targetIndex = target.length - 1;
		var selectedId = this.getSelectedId();
		if(selectedId.length > 0 && target.indexOf(selectedId[0]) >= 0) {
			targetIndex = target.indexOf(selectedId[0]) - 1;
			targetIndex = targetIndex < 0 ? target.length - 1 : targetIndex;
		}
		
		var id = target[targetIndex];
		var targetList = setItemList.call(this, id, []);
		for(var j in targetList) {
			this.openItem(targetList[j]);
		}

		this.selectItem(id);
		$(this.cont).animate({scrollTop: this.items[id].item.getBoundingClientRect().top}, 50);
	}.bind(this.tree);
	
	this.tree.closeAll = function(){
		for(var i in this.items) {
			this.closeItem(this.items[i].id);
		}
	}.bind(this.tree);
}

SepoaTreeView.prototype._initTreeData = function() {
	if(!this.tree) {
		return;
	}

//	this.tree.loadStruct('/servlets/sepoa.svl.util.SepoaAjaxTree?mode=doTree&PROCESS_ID=sepoa.svc.ct.CT_060&METHOD=doQueryDept');
}
