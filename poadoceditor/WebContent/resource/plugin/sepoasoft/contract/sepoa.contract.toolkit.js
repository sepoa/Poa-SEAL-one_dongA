function SepoaCertSign(conf){
	dhtmlx.extend(this, dhtmlx.EventSystem);
	this.conf = $.extend(true, {
		module:"certModuleNX",
		progress:{
			T:(SUX.getModuleTop().Sepoa.request.isPopup) ? SUX.getModuleTop().selfObj:SUX.root,
			W:(SUX.getModuleTop().Sepoa.request.isPopup) ? SUX.getModuleTop().parent.SUX.getModuleTop():SUX.getModuleTop()
		}
	}, conf || {});
	for(var i in SepoaCertSignModule[this.conf.module]){
		this[i] = SepoaCertSignModule[this.conf.module][i];	
	}
}
SepoaCertSign.prototype.progressOff=function(){
	if(this.progress.T.progressOff){
		this.progress.T.progressOff(undefined, this.progress.W)
	}
}
SepoaCertSign.prototype.progressOn=function(){
	if(this.progress.T.progressOn){
		this.progress.T.progressOn(this.progress.W)
	}
}
SepoaCertSignModule={
	certModuleNX:{
		init:function(){
			if(!this.ready){
				document.body.appendChild($('<div id="KICA_SECUKITNXDIV_ID"></div>')[0]).appendChild($(KICA_SECUKITNXDIV)[0]);
				
				window.SecuKitNX_Ready=function(a) {
					return function() {
						return function(flag){
							if(flag){
								this.ready = true;
								console.log('서명모듈이 로드되었습니다. SecuKitNX Ready');
								this.callEvent("onModuleReady", [Array.from(arguments)]);
							}
						}.apply(a, arguments)
					}
				}(this)
				
				window.SecuKitNXS_RESULT=function(a) {
					return function() {
						return function(cmd, res){
							var Err = 999;

							try {
								Err = res.ERROR_CODE;
							} catch (e) {
								console.log(e);
								Alert( "전자서명 이용을 위해 모듈을 로드합니다. 다시 접속해주세요.", function(result) {
									if(result){
										this.progressOff();
										window.location.reload(true);
									}
								});
							}
							
							if (Err === undefined) {
								this.activeProcessor.next(cmd, res);
							} else {
								$('.nx-cert-select').hide();
								$('#nx-pki-ui-wrapper').hide();
								KICA_Error.init();
								KICA_Error.setError(res.ERROR_CODE, res.ERROR_MESSAGE);
								var errorMsg = KICA_Error.getError();
								Message(errorMsg.replace(/\n/g,"<br>"));
								this.progressOff();
							}
						}.apply(a, arguments)
					}
				}(this)
				
				this.attachEvent("onSignError", this.__onError)
				
				secunx_Loading();
			}
		},
		doSign:function(data){
			var ap = this.activeProcessor=new SepoaNXSignProcessor(data, this);
			ap.addProcessor("setEnvironment", "env");
			ap.addProcessor("verifyVID", "ssn");
			ap.addProcessor("setEnvironment", "env");
			ap.addProcessor("generatePKCS7SignedData", "sign");
			ap.start();
		},
		__onError:function(cmd, data){
			Message(val.replace(/\n/g,"<br>"));
			this.activeProcessor = null;
			this.progressOff();
		}
	},
	certModuleOne : {
		init: function() {
			if(this.ready === true) {
				return;
			}
			
			$.getScript(window.CONTEXT_NAME + '/resource/plugin/SecuKitOne/secukit-one/config/config.js', () => {
				$.getScript(window.CONTEXT_NAME + '/resource/plugin/SecuKitOne/secukit-one/main/main.js', () => {
					if(this.conf.methodOrder) {
						if(secukit.setMethodOrder(this.conf.methodOrder) === undefined) {
							secukit.config.defaultMethod = '';
						}
					}
					
					secukit.init();
					window.test = secukit;
					this.ready = true;
					this.callEvent("onModuleReady", [secukit]);
					console.log('서명모듈이 로드되었습니다. SecuKitOne Ready');
				});
			});
		},
		doSign: function(data) {
			this.data = data;
			
			const message = data.SIGN_ORG_VALUE;
			const options = {
				title: '한국정보인증 간편인증 요청'
			};
			
			if(data.SERVER_CERT) {
				options.targetCert = data.SERVER_CERT;
				options.messageType = 'envelop';
			} else {
				options.messageType = 'pkcs1';
				options.digestAlg = 'sha256';
			}
			
			secukit.dialog.open();
			secukit.api.auth(message, options, (res, err) => {
				secukit.dialog.close();
				if (err !== undefined) {
					alert("code: " + err.code + "\n" + "message:" + err.message);
					return;
				}
				
				if (secukit.getMethod() === "cert") {
					const onAuthEnd = () => {
						secukit.api.getCertInfo((res) => {
							const result = $.extend(true, {}, this.data, {
								EXT_CERT_PEM : res.certPEM,
								EXT_CERT_METHOD : secukit.getMethod(),
								CERT_METHOD : secukit.getMethod()
							});
							
							this.callEvent("onSignPass", [result]);
						});
					};
					
					this.data.EXT_VID_RANDOM = res.vidRandom;
					
					if(options.messageType == 'envelop') {
						this.data.SIGN_VALUE = res.signedData;
						
						onAuthEnd();
					} else {
						secukit.api.genPKCS7SignedData(message, (res) => {
							this.data.SIGN_VALUE = res;
							onAuthEnd();
						});
					}
				} else {
					const result = {
						ORG_DATA : this.data.SIGN_ORG_VALUE,
						CERT_METHOD : secukit.getMethod(),
						SIGN_VALUE : res.signedData,
						EXT_CLIENT_TX_ID : res.clientTxId,
						EXT_SERVER_TX_ID : res.serverTxId,
						EXT_COMPLETE_STATUS_YN : res.completeStatusYn,
						EXT_RESULT_CD : res.resultCd,
						EXT_RESULT_MSG : res.resultMsg,
						EXT_USER_BIRTHDAY : res.userBirthday,
						EXT_USER_CI : res.userCi,
						EXT_USER_NAME : res.userName,
						EXT_USER_PHONE : res.userPhoneNum,
						EXT_PROVIDER : res.provider,
						EXT_CERT_METHOD : secukit.getMethod(),
					};
					
					this.callEvent("onSignPass", [result]);
					secukit.dialog.close();
				}
			});
		}
	}
};

function SepoaNXSignProcessor(data, signObj){
	this.data = data;
	this.signObj = signObj;
	this.step=-1;
	this.processor=[];
}
SepoaNXSignProcessor.prototype.start=function(){
	processLogic.init();
	processLogic.setProcessLogic('setCharSet');
	
	NX_ShowDialog();
}
SepoaNXSignProcessor.prototype.next=function(cmd, res){
	this.step++;
	
	if(this.step > 0){
		if(cmd == "sign"){
			this.data.SIGN_VALUE=res.generatePKCS7SignedData
		}else if(cmd == "ssn"){
			var result = res.verifyVID; 
			if(result != 'true'){
				this.signObj.callEvent("onSignError", [cmd, result.replace(/\n/g,"<br>")])
				return;
			}
		}
	}
	
	if(this.step >= this.processor.length){
		this.signObj.callEvent("onSignPass", [this.data]);
	}else{
		var nP = this.processor[this.step];
		secukitnxInterface.SecuKitNXS(nP.type+"."+nP.name, this.getParams(nP.type));
	}
}
SepoaNXSignProcessor.prototype.addProcessor=function(name, type){
	this.processor.push({name:name, type:type});
}
SepoaNXSignProcessor.prototype.getParams=function(type){
	if("env" == type){
		return {
			inCharset:"UTF-8",
			outCharset:"UTF-8",
			nxLocale:"KR"
		} 
	}else if("sign" == type){
		return {
			certType:"SignCert",
			certID:certListInfo.getCertID(),
			sourceString:this.data.SIGN_ORG_VALUE
		}
	}else if("ssn" == type){
		return {
			ssn:this.data.SIGN_KEY,
			certID:certListInfo.getCertID()
		}
	}
}