/**
 * 1.dhtmlxGrid
 */
// dhtmlxgrid.js의 dhtmlXGridObject 생성자를 상속합니다.
// 상속후, 변경하고자 하는 함수나 변수를 오버라이딩 합니다.
// 즉, dhtmlxgrid.js원본을 유지한채, 본 sepoa_dhtmlxgrid.js를 변경하는 방식으로 파일을 관리합니다.
// as of 131031 ysh

function SepoaDhtmlXGridObject(id, gridobj_id, gridAttribute){
	var autoCreateFlag = false;
	var gridCode = ""
	if(typeof(gridAttribute) == "string"){
		autoCreateFlag = true;
		gridCode = gridAttribute;
		gridAttribute = Sepoa.cache.grid.get(gridAttribute);
	}
	this.returnObj = {};
	this.SCREEN_ID = "";
	
	this.POASRM_CONTEXT_NAME = window.CONTEXT_NAME;
	this.COL_DEL = Sepoa.common.FIELD;
	this.ROW_DEL = Sepoa.common.LINE;
	this.page_count	= 1000;
	this.gridbox_id = id;
	this.grid_id = gridobj_id;
	
	this._gridAttribute = gridAttribute;
	
	this.col_format = {
		KRW_AMT_TYPE:'0,000',
		USD_AMT_TYPE:'0,000.00',
		QTY_TYPE:'0,000',
		PRICE_TYPE:'0,000.00',
		SMALL_AMT_TYPE:'0,000.0',
		SCORE_TYPE:'0.00',
		ONE_POINT_TYPE:'0,000.0',
		TWO_POINT_TYPE:'0,000.00',
		THREE_POINT_TYPE:'0,000.000',
		FOUR_POINT_TYPE:'0,000.0000',
		FIVE_POINT_TYPE:'0,000.00000'
	};
	this.number_sep = {
		decimal:'.',
		group:','
	}
	
	this.grid_init_widths 	= [];
	this.grid_col_align 	= [];
	this.grid_col_id 		= [];
	this.grid_header 		= [];
	this.grid_combo_data 	= [];
	this.grid_num_format	= {};
	this.grid_col_color 	= [];
	this.grid_col_sort 		= [];
	this.grid_col_visible	= [];
	this.grid_col_type		= [];
	this.grid_footer_msg 	= [];
	this.grid_required		= [];
	this.grid_template		= {};
	this.grid_icon			= {};
	this.grid_autocomplete  = {};
	this._ajaxStatusW		= {};
	
	this.grid_date_format 	= "";
	this.grid_selected 		= "";

	this._isSelectScreen 	= false;
	this._isRowsMergeable 	= false;
	
	this._dhxwins = null;
	this._prg_win = null;
	
	this._excelHiddenColumn=false;
	this._allCellReadOnly=false;
	this._otherGridData = {};
	this._enableExcelDownload=true;

    this.grid_col_text_style = [];
	
    let gridAttributeArray = [];
    if(gridAttribute && Object.keys(gridAttribute).length > 0){
	    Object.keys(gridAttribute).forEach((k)=>{
	    	gridAttributeArray.push(gridAttribute[k]);
	    })
	    gridAttributeArray.sort((a, b)=>{
	    	if(parseFloat(a.COL_SEQ) > parseFloat(b.COL_SEQ)) {
				return 1;
			}
			if(parseFloat(a.COL_SEQ) < parseFloat(b.COL_SEQ)) {
				return -1;
			}
			
			return 0;
	    })
    }
    
    gridAttribute = gridAttributeArray;
    
	for(var _idx in gridAttribute){
		var grid_column_type   = gridAttribute[_idx].COL_TYPE;
		
		this.SCREEN_ID = gridAttribute[_idx].SCREEN_ID;
		this.grid_init_widths.push(	gridAttribute[_idx].COL_WIDTH);
		this.grid_col_align.push(	gridAttribute[_idx].COL_ALIGN);
		this.grid_header.push(		gridAttribute[_idx].CONTENTS);
		this.grid_col_id.push(		gridAttribute[_idx].CODE);
		this.grid_col_type.push(grid_column_type);
		
		if(["coro","coro2","combo","comboro","comboro2"].indexOf(grid_column_type) >= 0 && gridAttribute[_idx].COL_COMBO.length > 0) {
			var comboInfo = gridAttribute[_idx].COL_COMBO;
			var combo_map = null; 
			
			if(typeof(comboInfo) == "string"){
				var combo = comboInfo.split(',');
				combo_map = {
					CODE   : combo[0], 
					PARAMS : window.dhx4.comboParam(combo[1]),
					COLUMN : gridAttribute[_idx].CODE,
					DEFAULT : (combo[2] == undefined) ? '':combo[2]
				}
			}else{
				combo_map = {
					COLUMN : gridAttribute[_idx].CODE,
					DATA:comboInfo
				}
			}
			
			this.grid_combo_data.push(combo_map);
		}
		
		if(["autocomplete"].indexOf(grid_column_type) >= 0 && gridAttribute[_idx].COL_COMBO.length > 0) {
			var combo = gridAttribute[_idx].COL_COMBO.split(',');
			var combo_map = {
					CODE   : combo[0], 
					PARAMS : window.dhx4.comboParam(combo[1]),
					DEFAULT : (combo[2] == undefined) ? '':combo[2]
			}
			this.grid_autocomplete[gridAttribute[_idx].CODE] = combo_map;
		}
		
		if((grid_column_type == "edn" || grid_column_type == "ron" || grid_column_type == "dynedn" || grid_column_type == "dynron") && gridAttribute[_idx].COL_FORMAT.length > 0) {
			this.grid_num_format[gridAttribute[_idx].CODE] = this.col_format[gridAttribute[_idx].COL_FORMAT];
		}
		
		if(gridAttribute[_idx].COL_COLOR != "undefined"){
			this.grid_col_color.push(gridAttribute[_idx].COL_COLOR);
		}else{
			this.grid_col_color.push("");
		}
		if(gridAttribute[_idx].TEXT_STYLE != "undefined"){
			this.grid_col_text_style.push(gridAttribute[_idx].TEXT_STYLE);
		}else{
			this.grid_col_text_style.push("");
		}
		
		if(grid_column_type == "edn" || grid_column_type == "ron"){
			this.grid_col_sort.push("int");
		}else{
			if(grid_column_type == "ch"){
				this.grid_col_sort.push("na");
			}else if(gridAttribute[_idx].COL_SORT.trim().length > 0){
				this.grid_col_sort.push(gridAttribute[_idx].COL_SORT);
			}else{
				this.grid_col_sort.push("str");
			}
		}
		
		if(["dhxCalendar","dhxCalendarA","dhxCalendarR"].indexOf(grid_column_type) >= 0 && gridAttribute[_idx].COL_FORMAT.length > 0 && this.grid_date_format.length== 0){
			var f = Sepoa.info.DATE_FORMAT;
			this.grid_date_format = (!f)?gridAttribute[_idx].COL_FORMAT:f;
		}
		
		if(grid_column_type == "template"){
			this.setTemplate(gridAttribute[_idx].CODE, gridAttribute[_idx].TEMPLATE);
		}else if(grid_column_type == "icon"){
			this.grid_icon[gridAttribute[_idx].CODE] = gridAttribute[_idx].TEMPLATE;
		}
		
		if(parseInt(gridAttribute[_idx].COL_WIDTH || 0) == 0 || (gridAttribute[_idx].COL_VISIBLE.length > 0 && gridAttribute[_idx].COL_VISIBLE == "false")){
			this.grid_col_visible.push(gridAttribute[_idx].CODE);
		}

		if(gridAttribute[_idx].SELECTED_YN == "Y" && this.grid_selected.length== 0){
			this.grid_selected = gridAttribute[_idx].CODE;
		}
		
		this.grid_required.push((gridAttribute[_idx].NOT_NULL == "1")?"NotEmpty":"");
		
		if(this.grid_footer_msg.length == 0){
			this.grid_footer_msg.push("MSG");
		}else if(this.grid_footer_msg.length == 1){
			this.grid_footer_msg.push("<div id='message'>&nbsp;</div>");
		}else{
			this.grid_footer_msg.push("#cspan");
		}
		
	}
	var base = dhtmlXGridObject.call(this, id);
	this.setColumnSizes = function(gridWidth) {
        var summ = 0;
        var fcols = [];
        var fix = 0;
        for (var i = 0; i < this._cCount; i++) {
            if ((this.initCellWidth[i] == "*") && !this._hrrar[i]) {
                this._awdth = false;
                fcols.push(i);
                continue
            }
            if (this.cellWidthType == "%") {
                if (typeof this.cellWidthPC[i] == "undefined") {
                    this.cellWidthPC[i] = this.initCellWidth[i]
                }
                var cwidth = (gridWidth * this.cellWidthPC[i] / 100) || 0;
                if (fix > 0.5) {
                    cwidth++;
                    fix--
                }
                var rwidth = this.cellWidthPX[i] = Math.floor(cwidth);
                var fix = fix + cwidth - rwidth
            } else {
                if (typeof this.cellWidthPX[i] == "undefined") {
                    this.cellWidthPX[i] = this.initCellWidth[i]
                }
            }
            if (!this._hrrar[i]) {
                summ += this.cellWidthPX[i] * 1
            }
        }
        if (fcols.length) {
            var ms = Math.floor((gridWidth - summ) / fcols.length);
            if (ms < 0) {
                ms = 1
            }
            for (var i = 0; i < fcols.length; i++) {
                var next = Math.max((this._drsclmW ? (this._drsclmW[fcols[i]] || 0) : 0), ms);
                this.cellWidthPX[fcols[i]] = next;
                summ += next
            }
            if (gridWidth > summ) {
                var last = fcols[fcols.length - 1];
                this.cellWidthPX[last] = this.cellWidthPX[last] + (gridWidth - summ);
                summ = gridWidth
            }
            this._setAutoResize()
        }
        this.hdr.style.width = summ + "px";
        var hdrStyle = window.getComputedStyle(this.hdr);
        this.obj.style.width = Math.ceil(parseFloat(hdrStyle.width) - parseFloat(hdrStyle.paddingRight) - parseFloat(hdrStyle.paddingLeft)) + "px";
        if (this.ftr) {
            this.ftr.style.width = summ + "px"
        }
        this.chngCellWidth();
        return summ
    };
    this.chngCellWidth = function() {
        if ((_isOpera) && (this.ftr)) {
            this.ftr.width = this.objBox.scrollWidth + "px"
        }
        var l = this._cCount;
        for (var i = 0; i < l; i++) {
        	this.hdr.rows[0].cells[i].style.width = this.cellWidthPX[i] + "px";
            this.obj.rows[0].childNodes[i].style.width = this.cellWidthPX[i] + "px";
            if (this.ftr) {
                this.ftr.rows[0].cells[i].style.width = this.cellWidthPX[i] + "px"
            }
        }
    };
    
    //bug
    this.hdr.style.paddingRight="";
    this.hdr.style.marginRight="";
    
    var thdr = this.hdrBox.appendChild(document.createElement('div'));
    thdr.appendChild(this.hdr);
    thdr.style.display="inline-block";
    thdr.style.paddingRight="20px";
    
	this.setSizes = function() {
        if ((!this.hdr.rows[0])) {
            return
        }
        var quirks = this.quirks = (_isIE && document.compatMode == "BackCompat");
        var outerBorder = parseInt((this.entBox.offsetWidth - this.entBox.clientWidth) / 2);
        if (!this.dontSetSizes) {
            if (this.globalBox) {
                if (!this.globalBox.clientWidth) {
                    return
                }
                var ow = this.globalBox.clientWidth;
                var splitOuterBorder = (this.globalBox.offsetWidth - ow) / 2;
                if (this._delta_x && !this._realfake) {
                    this.globalBox.style.width = this._delta_x;
                    var owu = this.globalBox.clientWidth;
                    this.entBox.style.width = Math.max(0, (owu + (quirks ? splitOuterBorder * 2 : 0)) - this._fake.entBox.clientWidth) + "px";
                    if (owu != this._lastTimeSplitWidth) {
                    	if(this._fake){
                    		this._fake._correctSplit();
                    	}
                        this._lastTimeSplitWidth = owu
                    }
                }
                if (this._delta_y && !this._realfake) {
                    this.globalBox.style.height = this._delta_y;
                    this.entBox.style.overflow = this._fake.entBox.style.overflow = "hidden";
                    this.entBox.style.height = this._fake.entBox.style.height = this.globalBox.clientHeight + (quirks ? splitOuterBorder * 2 : 0) + "px"
                }
            } else {
                if (this._delta_x) {
                    if (this.entBox.parentNode && this.entBox.parentNode.tagName == "TD") {
                        this.entBox.style.width = "1px";
                        this.entBox.style.width = parseInt(this._delta_x) * this.entBox.parentNode.clientWidth / 100 - outerBorder * 2 + "px"
                    } else {
                        this.entBox.style.width = this._delta_x
                    }
                }
                if (this._delta_y) {
                    this.entBox.style.height = this._delta_y
                }
            }
        }
        window.clearTimeout(this._sizeTime);
        if (!this.entBox.offsetWidth && (!this.globalBox || !this.globalBox.offsetWidth)) {
            this._sizeTime = window.setTimeout(function() {
                if (self.setSizes) {
                    self.setSizes()
                }
            }, 250);
            return
        }
        var border_x = ((!this._wthB) && ((this.entBox.cmp || this._delta_x) && (this.skin_name || "").indexOf("dhx") == 0 && !quirks) ? 2 : 0);
        var border_y = ((!this._wthB) && ((this.entBox.cmp || this._delta_y) && (this.skin_name || "").indexOf("dhx") == 0 && !quirks) ? 2 : 0);
        if (this._sizeFix) {
            border_x -= this._sizeFix;
            border_y -= this._sizeFix
        }
        var isVScroll = this.parentGrid ? false : (this.objBox.scrollHeight > this.objBox.offsetHeight);
        var scrfix = dhtmlx.$customScroll ? 0 : 18;
        var gridWidth = this.entBox.clientWidth - (this.skin_h_correction || 0) * (quirks ? 0 : 1) - border_x;
        var gridWidthActive = this.entBox.clientWidth - (this.skin_h_correction || 0) - border_x;
        var gridHeight = (this.globalBox || this.entBox).clientHeight - border_y;
        var summ = this.setColumnSizes(gridWidthActive - (isVScroll ? scrfix : 0) - (this._correction_x || 0));
        var isHScroll = this.parentGrid ? false : ((this.objBox.scrollWidth > this.objBox.offsetWidth) || (this.objBox.style.overflowX == "scroll"));
        var headerHeight = this.hdr.clientHeight;
        var footerHeight = this.ftr ? this.ftr.clientHeight : 0;
        var newWidth = gridWidth;
        var newHeight = gridHeight - headerHeight - footerHeight;
        if (this._awdth && this._awdth[0] && this._awdth[1] == 99999) {
            isHScroll = 0
        }
        if (this._ahgr) {
            if (this._ahgrMA) {
                newHeight = this.entBox.parentNode.clientHeight - headerHeight - footerHeight
            } else {
                newHeight = this.obj.offsetHeight + (isHScroll ? scrfix : 0) + (this._correction_y || 0)
            }
            if (this._ahgrM) {
                if (this._ahgrF) {
                    newHeight = Math.min(this._ahgrM, newHeight + headerHeight + footerHeight) - headerHeight - footerHeight
                } else {
                    newHeight = Math.min(this._ahgrM, newHeight)
                }
            }
            if (isVScroll && newHeight >= this.obj.scrollHeight + (isHScroll ? scrfix : 0)) {
                isVScroll = false;
                this.setColumnSizes(gridWidthActive - (this._correction_x || 0))
            }
        }
        if ((this._awdth) && (this._awdth[0])) {
            if (this.cellWidthType == "%") {
                this.cellWidthType = "px"
            }
            if (this._fake) {
                summ += this._fake.entBox.clientWidth
            }
            var newWidth = Math.min(Math.max(summ + (isVScroll ? scrfix : 0), this._awdth[2]), this._awdth[1]) + (this._correction_x || 0);
            this.objBox.style.overflowX = (!isVScroll && this.objBox.scrollWidth <= newWidth) ? "hidden" : "auto";
            if (this._fake) {
                newWidth -= this._fake.entBox.clientWidth
            }
        }
        newHeight = Math.max(0, newHeight);
        this._ff_size_delta = (this._ff_size_delta == 0.1) ? 0.2 : 0.1;
        if (!_isFF) {
            this._ff_size_delta = 0
        }
        if (!this.dontSetSizes) {
            this.entBox.style.width = Math.max(0, newWidth + (quirks ? 2 : 0) * outerBorder + this._ff_size_delta) + "px";
            this.entBox.style.height = newHeight + (quirks ? 2 : 0) * outerBorder + headerHeight + footerHeight + "px"
        }
        this.objBox.style.height = newHeight + ((quirks && !isVScroll) ? 2 : 0) * outerBorder + "px";
        this.hdrBox.style.height = headerHeight + "px";
        if (newHeight != gridHeight) {
            this.doOnScroll(0, !this._srnd)
        }
        var ext = this["setSizes_" + this.skin_name];
        if (ext) {
            ext.call(this)
        }
        this.setSortImgPos();
        if (headerHeight != this.hdr.clientHeight && this._ahgr) {
            this.setSizes()
        }
        this.callEvent("onSetSizes", [])
    };
	
	
	this.getColumnLabel = function(cin, ind, hdr) {
	    var z = (hdr || this.hdr).rows[(ind || 0) + 1];
	    for (var i = 0; i < z.cells.length; i++) {
	        if (z.cells[i]._cellIndexS == cin) {
	        	var d = (z.cells[i].innerText ? z.cells[i].innerText : z.cells[i].textContent);
	        	if(z.cells[i].innerText){
	        		d = d.split("\n");
	        		if(d[d.length-1] == ""){
		        		d.splice(d.length-1, 1);	
	        		}
	        		d = d.join("\n");
	        	}
	            return d;
	        }
	    }
	    return ""
	};
	
	//크롬 스크롤링 버그 수정
	this.hdrBox.onscroll = function() {
        if (this._try_header_sync || this.grid._skip_header_event) {
            return
        }
        this._try_header_sync = true;
        if (Math.abs(this.grid.objBox.scrollLeft - this.scrollLeft) > 1) {
            this.grid.objBox.scrollLeft = this.scrollLeft
        }
        this._try_header_sync = false
    };
    this.objBox.onscroll = function () {
    	if(this.grid._skip_header_event){
    		clearTimeout(this.grid._skip_header_event)
    	}
    	this.grid._skip_header_event = setTimeout(function(obj){
    		obj.grid._skip_header_event = null;
    	},100, this)
    	this.grid._doOnScroll();
    };
    this.setSelectScreen = function (b) {
        if(b) {
            this.detachAllEvents("onEditCell");
        }
    };
  // 서블릿 갔다오면 행삭제 안되는 버그(?)로 인한 펑션 생성
    this.deleteRow2 = function(row_id, node){
        if (!node)
            node=this.getRowById(row_id)
    
        if (!node)
            return;
        
        var pid=0;
        if (this.cellType._dhx_find("tree") != -1 && !this._realfake){
            pid=this._h2.get[row_id].parent.id;
            this._removeTrGrRow(node);
        }
        else {
            if (node.parentNode)
                node.parentNode.removeChild(node);
    
            var ind = this.rowsCol._dhx_find(node);
    
            if (ind != -1)
                this.rowsCol._dhx_removeAt(ind);
    
            for (var i = 0; i < this.rowsBuffer.length; i++)
                if (this.rowsBuffer[i]&&this.rowsBuffer[i].idd == row_id){
                    this.rowsBuffer._dhx_removeAt(i);
                    ind=i;
                    break;
                }
        }
        this.rowsAr[row_id]=null;
    
        for (var i = 0; i < this.selectedRows.length; i++)
            if (this.selectedRows[i].idd == row_id)
                this.selectedRows._dhx_removeAt(i);
    
        if (this._srnd){
            for (var i = 0; i < this._fillers.length; i++){
                var f = this._fillers[i]
                if (!f) continue; // can be null
                
                this._update_fillers(i, (f[1] >= ind ? -1 : 0), (f[0] >= ind ? -1 : 0));
            };
    
            this._update_srnd_view();
        }
    
        if (this.pagingOn)
            this.changePage();
        if (!this._realfake)  this.callEvent("onAfterRowDeleted", [row_id,pid]);
        this.callEvent("onGridReconstructed", []);
        if (this._ahgr) this.setSizes();
        return true;
    };
	this.dyn_mask=[
		2,
		3,
		"",
		"",
		this.i18n.decimal_separator,
		this.i18n.group_separator
	];

	/**
	 * @desc: convert formated value to original
	 * @type: private
	 */
    this._aplNFb=function(data, ind, isDyn){
		var a = this._maskArr[ind];
		if(isDyn) {
			a = isDyn;
		}

		if (!a)
			return data;
	
		var dotChar = a[4] || "."
		var ndata = parseFloat(data.toString().replace(new RegExp(`[^0-9${dotChar}]`, 'g'), ""));
		if (data.toString().substr(0, 1) == "-")
			ndata=ndata*-1;

		//if (a[0] > 0) {
		//	ndata=ndata / Math.pow(10, a[0]);
		//}
		return isNaN(ndata)?'':ndata;
	};

	/**
	 * @desc: format data with mask
	 * @type: private
	 */
    this._aplNF=function(l, h) {
        var e = this._maskArr[h];
        if (!e) {
            return l
        }
        var m = (parseFloat(l) < 0 ? "-" : "") + e[2];
        l = Math.abs(Math.round(parseFloat(l) * Math.pow(10, e[0] > 0 ? e[0] : 0))).toString();
        l = (l.length < e[0] ? Math.pow(10, e[0] + 1 - l.length).toString().substr(1, e[0] + 1) + l.toString() : l).split("").reverse();
        l[e[0]] = (l[e[0]] || "0") + e[4];
        if (e[1] > 0) {
            for (var g = (e[0] > 0 ? 0 : 1) + e[0] + e[1]; g < l.length; g += e[1]) {
                l[g] += e[5]
            }
        }
        return m + l.reverse().join("") + e[3]
    };
	
	this.noDataScroll = function(){
		// 그리드데이터없을시 스크롤바생성
        if(this.getRowsNum()<1){
        	this.setSizes();
    	}
	};
	
	if(typeof(document.formstyle) == "undefined"){
		var form = document.createElement("form");
	    form.setAttribute("id"  , "formstyle");
	    form.setAttribute("name", "formstyle");
		
	    var parameters = ["headText", "headWidth", "screen_id", "user_id", "mode", "csvBuffer", "gridColids"];
	    // Input Box 생성
		for (var _idx in parameters){
			var input = document.createElement("input");
			input.setAttribute("type" , "hidden");
			input.setAttribute("id"   , parameters[_idx]);
			input.setAttribute("name" , parameters[_idx]);
			form.appendChild(input);	
		}
	    document.body.appendChild(form);
	}
	this._pasteEvent = false;
	
	this.editStop = function(mode) {
        if (_isOpera) {
            if (this._Opera_stop) {
                if ((this._Opera_stop * 1 + 50) > (new Date).valueOf()) {
                    return
                }
                this._Opera_stop = null
            }
        }
        if (this.editor && this.editor != null) {
            this.editor.cell.className = this.editor.cell.className.replace("editable", "");
            if (this.editor.detach()) {
                this.cell.wasChanged = true
            }
            var g = this.editor;
            if (g == null) {
                return
            }
            this.editor = null;
            var z = this.callEvent("onEditCell", [2, this.row.idd, this.cell._cellIndex, g.getValue(), g.val]);
            if ((typeof(z) == "string") || (typeof(z) == "number")) {
                g[g.setImage ? "setLabel" : "setValue"](z)
            } else {
                if (!z) {
                    g[g.setImage ? "setLabel" : "setValue"](g.val)
                }
            }
            if (this._ahgr && this.multiLine) {
                this.setSizes()
            }
        }
    };
    this.getColWidth = function(ind) {
        return parseInt((this.cellWidthPX[ind]=="")?0:this.cellWidthPX[ind]);
    };
    this.setColumnTextStyle = function(clr) {
    	this.columnTextStyle = clr.split(this.delim)
    };
  // 방향키 위, 아래 입력시 cell edit 상태로 변경하도록 수정
    this._key_events.k40_0_0 = function() {
        var c = this._realfake ? this._fake : this;
        if (this.editor && this.editor.combo) {
            this.editor.shiftNext()
        } else {
            if (!this.row.idd) {
                return
            }
            var a = Math.max((c._r_select || 0), this.getRowIndex(this.row.idd));
            var e = this._nextRow(a, 1);
            if (e) {
                c._r_select = null;
                this.selectCell(e, this.cell._cellIndex, true, undefined, true);
                if (c.pagingOn) {
                    c.showRow(e.idd)
                }
            } else {
                if (!this.callEvent("onLastRow", [])) {
                    return false
                }
                this._key_events.k34_0_0.apply(this, []);
                if (this.pagingOn && this.rowsCol[a + 1]) {
                    this.selectCell(a + 1, 0, true)
                }
            }
        }
        this._still_active = true
    };
    this._key_events.k38_0_0 = function() {
        var c = this._realfake ? this._fake : this;
        if (this.editor && this.editor.combo) {
            this.editor.shiftPrev()
        } else {
            if (!this.row.idd) {
                return
            }
            var a = this.getRowIndex(this.row.idd) + 1;
            if (a != -1 && (!this.pagingOn || (a != 1))) {
                var e = this._nextRow(a - 1, -1);
                this.selectCell(e, this.cell._cellIndex, true, undefined, true);
                if (c.pagingOn && e) {
                    c.showRow(e.idd)
                }
            } else {
                this._key_events.k33_0_0.apply(this, [])
            }
        }
        this._still_active = true
    };
    
    // 틀고정시 combobox 에서 tab 기능 안먹는현상 수정
    this._org_splitAt = this.splitAt;
    this.splitAt = function(m){
    	this._org_splitAt(m);
    	function l(K, J, D, M) {
            var E = M.shiftKey ? -1 : 1;
            var F = M.shiftKey ? -1 : D._cCount;
            var I = false;
            for (var H = K + E; H != F; H += E) {
                if (D.smartTabOrder) {
                    I = D.cells2(J, H).isDisabled() ? false : H
                } else {
                    I = H
                }
                if (I !== false) {
                    var L = !D._key_events.k_other;
                    D.selectCell(J, I, false, false, L, true);
                    M.cancelBubble = true;
                    if (M.preventDefault) {
                        M.preventDefault()
                    }
                    return true
                }
            }
        }
    	this._fake.doKey = this.doKey = function(I) {
            if (!I) {
                return true
            }
            if (this._htkebl) {
                return true
            }
            if ((I.target || I.srcElement).value !== window.undefined) {
                var E = (I.target || I.srcElement);
                var ct = E.className.indexOf('dhxcombo_input') != -1;
                if(!ct){
                	if((!E.parentNode) || (E.parentNode.className.indexOf("editable") == -1)){
                		return true;
                	}
                }else{
                	if((!E.parentNode) || (E.parentNode.parentNode.parentNode.className.indexOf("editable") == -1)){
                		return true;
                	}
                }
            }
            switch (I.keyCode) {
                case 9:
                    var H = this._realfake ? this._fake : this;
                    if (!H.callEvent("onTab", [true])) {
                        return true
                    }
                    if (this.cell) {
                        var F = this.cell._cellIndex;
                        var D = H.getRowIndex(this.cell.parentNode.idd);
                        while (!l(F, D, H, I)) {
                            D += (I.shiftKey ? -1 : 1);
                            if (D < 0 || D >= H.rowsBuffer.length) {
                                return
                            }
                            F = I.shiftKey ? H._cCount : -1
                        }
                        return
                    }
                    break
            }
            return this.doKeyA(I)
        };
    }
    
    this._key_events.k9_0_0=function() {
        this.editStop();
        if (!this.callEvent("onTab", [true])) {
            return true
        }
        var a = this._getNextCell(null, 1);
        if (a) {
            this.selectCell(a.parentNode, a._cellIndex, (this.row != a.parentNode), false, true,void 0, true);
            this._still_active = true
        }
    }
    this._key_events.k9_0_1=function() {
        this.editStop();
        if (!this.callEvent("onTab", [false])) {
            return false
        }
        var a = this._getNextCell(null, -1);
        if (a) {
            this.selectCell(a.parentNode, a._cellIndex, (this.row != a.parentNode), false, true,void 0, true);
            this._still_active = true
        }
    }
    this.selectCell = function(r, cInd, fl, preserve, edit, show, isTab) {
        if (!fl) {
            fl = false
        }
        if (typeof(r) != "object") {
            r = this.render_row(r)
        }
        if (!r || r == -1) {
            return null
        }
        if (r._childIndexes) {
            var c = r.childNodes[r._childIndexes[cInd]]
        } else {
            var c = r.childNodes[cInd]
        }
        if (!c) {
            c = r.childNodes[0]
        }
        if (!this.markedCells) {
            if (preserve) {
                this.doClick(c, fl, 3, show, isTab)
            } else {
                this.doClick(c, fl, 0, show, isTab)
            }
        } else {
            this.doMark(c, preserve ? 2 : 0)
        }
        if (edit) {
            this.editCell()
        }
    };
    this.doClick = function(el, fl, selMethod, show, isTab) {
    	if(this._beforeClickTime && ((Date.now() - this._beforeClickTime) < 300)){
    		return false;
    	}else{
    		this._beforeClickTime = Date.now();
    	}
        if (!this.selMultiRows) {
            selMethod = 0
        }
        var psid = this.row ? this.row.idd : 0;
        this.setActive(true);
        if (!selMethod) {
            selMethod = 0
        }
        if (this.cell != null) {
            this.cell.className = this.cell.className.replace(/cellselected/g, "")
        }
        if (el.tagName == "TD") {
            if (this.checkEvent("onSelectStateChanged")) {
                var initial = this.getSelectedId()
            }
            var prow = this.row;
            if (selMethod == 1) {
                var elRowIndex = this.rowsCol._dhx_find(el.parentNode);
                var lcRowIndex = this.rowsCol._dhx_find(this.lastClicked);
                if (elRowIndex > lcRowIndex) {
                    var strt = lcRowIndex;
                    var end = elRowIndex
                } else {
                    var strt = elRowIndex;
                    var end = lcRowIndex
                }
                for (var i = 0; i < this.rowsCol.length; i++) {
                    if ((i >= strt && i <= end)) {
                        if (this.rowsCol[i] && (!this.rowsCol[i]._sRow)) {
                            if (!this.rowsCol[i].idd) {
                                continue
                            }
                            if (this.rowsCol[i].className.indexOf("rowselected") == -1 && (this.callEvent("onBeforeSelect", [this.rowsCol[i].idd, psid, el._cellIndex]))) {
                                this.rowsCol[i].className += " rowselected";
                                this.selectedRows[this.selectedRows.length] = this.rowsCol[i]
                            }
                        } else {
                            this.clearSelection();
                            return this.doClick(el, fl, 0, show)
                        }
                    }
                }
            } else {
                if (selMethod == 2) {
                    if (el.parentNode.className.indexOf("rowselected") != -1) {
                        el.parentNode.className = el.parentNode.className.replace(/rowselected/g, "");
                        this.selectedRows._dhx_removeAt(this.selectedRows._dhx_find(el.parentNode));
                        var skipRowSelection = true;
                        show = false
                    }
                }
            }
            this.editStop();
            if (typeof(el.parentNode.idd) == "undefined") {
                return true
            }
            if ((!skipRowSelection) && (!el.parentNode._sRow)) {
                if (this.callEvent("onBeforeSelect", [el.parentNode.idd, psid, el._cellIndex])) {
                    if (this.getSelectedRowId() != el.parentNode.idd) {
                        if (selMethod == 0) {
                            this.clearSelection()
                        }
                        this.cell = el;
                        if ((prow == el.parentNode) && (this._chRRS)) {
                            fl = false
                        }
                        this.row = el.parentNode;
                        this.row.className += " rowselected";
                        if (this.selectedRows._dhx_find(this.row) == -1) {
                            this.selectedRows[this.selectedRows.length] = this.row
                        }
                    } else {
                        this.cell = el;
                        this.row = el.parentNode
                    }
                } else {
                    fl = false
                }
            }
            if (this.cell && this.cell.parentNode.className.indexOf("rowselected") != -1) {
                this.cell.className = this.cell.className.replace(/cellselected/g, "") + " cellselected"
            }
            if (selMethod != 1) {
                if (!this.row) {
                    return
                }
            }
            this.lastClicked = el.parentNode;
            var rid = this.row.idd;
            var cid = this.cell;
            if (fl && typeof(rid) != "undefined" && cid && !skipRowSelection && !isTab) {
            	base.callEvent("onRowSelectSync", [rid]);
                base.onRowSelectTime = setTimeout(function() {
                    if (base.callEvent) {
                        base.callEvent("onRowSelect", [rid, cid._cellIndex])
                    }
                }, 100)
            } else {
                this.callEvent("onRowSelectRSOnly", [rid])
            }
            if (this.checkEvent("onSelectStateChanged")) {
                var afinal = this.getSelectedId();
                if (initial != afinal) {
                    this.callEvent("onSelectStateChanged", [afinal, initial])
                }
            }
            if (skipRowSelection) {
                return false
            }
        }
        this.isActive = true;
        if (show !== false && this.cell && this.cell.parentNode.idd) {
            this.moveToVisible(this.cell)
        }
    };
    
    this.originClearAll = this.clearAll
    this.clearAll = function(flag){
    	this.originClearAll(flag)
    	$(this.objBox).find('.dhx_sub_row').remove()
    }
    
    // tree readonly
    this._edtc = true;
    
    if(autoCreateFlag && Sepoa.common.DEV_FLAG){
    	this.createModifyButton(gridCode);
    }
    this.attachEvent("onRowAdded", function(a){
    	for(var i in this.columnIds){
    		if(["icon", "ch"].indexOf(this.getColTypeById(this.columnIds[i])) +1){
    			this.cells(a, this.getColIndexById(this.columnIds[i])).setValue();
    		}
    	}
    });
    this.attachEvent("onColumnHidden", function(e,c) {
		for (var i = 0; i < this.selectedRows.length; i++) {
            var r = this.rowsAr[this.selectedRows[i].idd];
            if (r) {
            	$(r).find('td').removeClass('lastCell');
            	$(r).find('td:visible:last').addClass('lastCell');
            }
        }
    });
    var customClearSelection = function(){
    	if(this._fake){
    		$(this._fake.objBox).find('.preselected').removeClass('preselected')
    		this._fake.clearSelection();
    	}
    	$(this.objBox).find('.preselected').removeClass('preselected');
    	this.clearSelection();
    	return true;
    }
    this.attachEvent("onEmptyClick",customClearSelection);
    this.attachEvent("onXLE", customClearSelection);
    this.attachEvent("onXLE", ()=>{
    	if(this._headerFilter){
    		this.filterByAll();
    	}
    });
    this.attachEvent("onBeforeSorting", customClearSelection)
	this.attachEvent("onBeforeSelect", function(id){
		if(this._fake){
			$(this._fake.objBox).find('.preselected').removeClass('preselected')
			var pre = this._fake.getRowById(id).previousSibling
			if(pre){
				pre.classList.add('preselected')
			}
		}
		$(this.objBox).find('.preselected').removeClass('preselected');
		$(this.getRowById(id)).find('td:visible:last').addClass('lastCell');
		var pre = this.getRowById(id).previousSibling
		if(pre){
			pre.classList.add('preselected')
		}
		return true;
	})
	this.entBox.dhtmlxObject = this;
	this.entBox.classList.add('sepoaux_grid');
	this._setColumnHidden = this.setColumnHidden;
	this.setColumnHidden = function(){
		this._setColumnHidden.apply(this, [].slice.call(arguments));
		this.setSizes();
	}
	this.__parse = this.parse;
	this.parse = function(){
		if(!this._checkAjaxStatus()){
			this._parseDelayEvent={
				args:[].slice.call(arguments)
			}
			
			this._parseDelayEvent.event = this.attachEvent('alreadyCallConnection',function(){
				this.__parse.apply(this, this._parseDelayEvent.args);
				this.detachEvent(this._parseDelayEvent.event);
				delete this._parseDelayEvent;
			}.bind(this));
		}else{
			this.__parse.apply(this, [].slice.call(arguments));
		}
	}
	
	// XSS vulnerability fix
	this.setColumnLabel = function(c, label, ind, hdr) {
		var z = (hdr || this.hdr).rows[ind || 1];
		var col = (z._childIndexes ? z._childIndexes[c] : c);
		if (!z.cells[col]) {
		    return
		}
		if (!this.useImagesInHeader) {
		    var hdrHTML = "<div class='hdrcell'>";
		    if (label.indexOf("img:[") != -1) {
		        var imUrl = label.replace(/.*\[([^>]+)\].*/, "$1");
		        label = label.substr(label.indexOf("]") + 1, label.length);
		        hdrHTML += "<img width='18px' height='18px' align='absmiddle' src='" + imUrl + "' hspace='2'>"
		        hdrHTML += label;
		    } else {
		        const temp = document.createElement('div');
		        temp.appendChild(document.createTextNode(label));
		        const regex = /&lt;(br\s*\/?|\/br)&gt;/g;
		        const labelText = temp.innerHTML.replace(regex, '<br>');
		        hdrHTML += labelText;
		    }
		    hdrHTML += "</div>";
		    z.cells[col].innerHTML = hdrHTML;
		    if (this._hstyles[c]) {
		        z.cells[col].style.cssText = this._hstyles[c]
		    }
		} else {
		    z.cells[col].style.textAlign = "left";
		    z.cells[col].innerHTML = "<img src='" + label + "'>";
		    var a = new Image();
		    a.src = "" + label.replace(/(\.[a-z]+)/, ".des$1");
		    this.preloadImagesAr[this.preloadImagesAr.length] = a;
		    var b = new Image();
		    b.src = "" + label.replace(/(\.[a-z]+)/, ".asc$1");
		    this.preloadImagesAr[this.preloadImagesAr.length] = b
		}
		if ((label || "").indexOf("#") != -1) {
		    var t = label.match(/(^|{)#([^}]+)(}|$)/);
		    if (t) {
		        var tn = "_in_header_" + t[2];
		        if (this[tn]) {
		            this[tn]((this.forceDivInHeader ? z.cells[col].firstChild : z.cells[col]), col, label.split(t[0]))
		        }
		    }
		}
	}
	
	dhtmlXGridCellObject.prototype.setValue = function(a) {
	    if ((typeof(a) != "number") && (!a || a.toString()._dhx_trim() == "")) {
	        a = "&nbsp;";
	        this.cell._clearCell = true
	    } else {
	        this.cell._clearCell = false
	    }
	    this.setCTxtValue(a);
	};
	
	dhtmlXGridCellObject.prototype.setCTxtValue = function(a) {
	    this.cell.innerHTML = "";
	    this.cell.appendChild(document.createTextNode(htmlDecode(a)));
	    this.grid.callEvent("onCellChanged", [this.cell.parentNode.idd, this.cell._cellIndex, a])
	};
	
	// support for tree grid
	this.getRowId = function(ind) {
		const target = this.isTreeGrid() && this._h2 ? this._h2.get[Object.keys(this._h2.get)[ind + 1]]?.buff : (this._f_rowsBuffer||this.rowsBuffer)[ind];
		return target ? target.idd : this.undefined
	};
	
	this.getRowsNum = function() {
		return (this.isTreeGrid() && this._h2) ? Object.keys(this._h2.get).length - 1 : (this._f_rowsBuffer||this.rowsBuffer).length;
	}
	
	this.getRowById = function(e) {
        var c = this.rowsAr[e];
        if (c) {
            if (c.tagName != "TR") {
            	let hasFilterBuffer = (this._f_rowsBuffer)?true:false;
            	let orgBuffer = null;
            	if(hasFilterBuffer){
            		orgBuffer = this.rowsBuffer;
            		this.rowsBuffer = this._f_rowsBuffer;
            	}
            	let row = null;
            	for (var a = 0; a < this.rowsBuffer.length; a++) {
                    if (this.rowsBuffer[a] && this.rowsBuffer[a].idd == e) {
                        row = this.render_row(a)
                    }
                }
                if (!row && this._h2) {
                    row = this.render_row(null, c.idd)
                }
                if(hasFilterBuffer){
                	this._f_rowsBuffer = this.rowsBuffer;
                	this.rowsBuffer = orgBuffer;
                	
                	for (var a = 0; a < this.rowsBuffer.length; a++) {
                        if (this.rowsBuffer[a] && this.rowsBuffer[a].idd == e) {
                        	this.rowsBuffer[a] = row;
                        }
                    }
            	}
                if(row){
                	return row;
                }
                
            }
            return c
        }
        return null
    }
	
	this.attachHeaderFns = [this.attachHeaderA, function() {
		this.attachHeaderFns[0].apply(this, arguments);
	    if (this._realfake) {
	        return true
	    }
	    this.formAutoSubmit();
	    if (typeof(this.FormSubmitOnlyChanged) == "undefined") {
	        this.submitOnlyChanged(true)
	    }
	    if (typeof(this._submitAR) == "undefined") {
	        this.submitAddedRows(true)
	    }
	    var a = this;
	    this._added_rows = [];
	    this._deleted_rows = [];
	    this.attachEvent("onRowAdded", function(c) {
	        a._added_rows.push(c);
	        a.forEachCell(c, function(e) {
	            e.cell.wasChanged = true
	        });
	        return true
	    });
	    this.attachEvent("onBeforeRowDeleted", function(c) {
	        a._deleted_rows.push(c);
	        return true
	    });
	    this.attachHeaderFns.splice(1);
	}.bind(this)];
	
	this.attachHeader = function(){
		let filterHdr = this.getHeaderFilterIdx();
		if(filterHdr >= 0){
			this.disableHeaderFilter();
		}
		this.attachHeaderFns[this.attachHeaderFns.length - 1].apply(this, [].slice.call(arguments));
		if(filterHdr >= 0){
			this.enableHeaderFilter();
		}
	}
	delete this.attachHeaderA;
	
	this.render_dataset=(g, o)=>{
        if (this._srnd) {
            if (this._fillers) {
                return this._update_srnd_view()
            }
            o = Math.min((this._get_view_size() + (this._srnd_pr || 0)), this.rowsBuffer.length)
        }
        if (this.pagingOn) {
            g = Math.max((g || 0), (this.currentPage - 1) * this.rowsBufferOutSize);
            o = Math.min(this.currentPage * this.rowsBufferOutSize, this.rowsBuffer.length)
        } else {
            g = g || 0;
            o = o || this.rowsBuffer.length
        }
        for (var h = g; h < o; h++) {
            var a = this.render_row(h);
            if (a == -1) {
                if (this.xmlFileUrl) {
                    if (this.callEvent("onDynXLS", [h, (this._dpref ? this._dpref : (o - h))])) {
                        this.load(this.xmlFileUrl + dhtmlx.url(this.xmlFileUrl) + "posStart=" + h + "&count=" + (this._dpref ? this._dpref : (o - h)), this._data_type)
                    }
                }
                o = h;
                break
            }
            if (!a.parentNode || !a.parentNode.tagName) {
                this._insertRowAt(a, h);
                if (a._attrs.selected || a._attrs.select) {
                    this.selectRow(a, a._attrs.call ? true : false, true);
                    a._attrs.selected = a._attrs.select = null
                }
            }
            if (this._ads_count && h - g == this._ads_count) {
                var n = this;
                this._context_parsing = this._context_parsing || this._parsing;
                return this._contextCallTimer = window.setTimeout(function() {
                    n._contextCallTimer = null;
                    n.render_dataset(h, o);
                    if (!n._contextCallTimer) {
                        if (n._context_parsing) {
                            n.callEvent("onXLE", [])
                        } else {
                            n._fixAlterCss()
                        }
                        n._context_parsing = false
                    }
                }, this._ads_time)
            }
        }
        if (this._ads_count && h == o) {
            this.callEvent("onDistributedEnd", [])
        }
        if (this._srnd && !this._fillers) {
        	this._get_view_size();
            var c = this.rowsBuffer.length - o;
            this._fillers = [];
            if (this._fake && !this._realfake) {
                this._fake._fillers = []
            }
            var e = Math.round(990000 / this._srdh);
            while (c > 0) {
                var l = (_isIE || window._FFrv) ? Math.min(c, e) : c;
                var m = this._add_filler(o, l);
                if (m) {
                    this._fillers.push(m)
                }
                c -= l;
                o += l
            }
        }
        this.setSizes()
    }
}
SepoaDhtmlXGridObject.prototype = Object.create(dhtmlXGridObject.prototype);
SepoaDhtmlXGridObject.prototype.constructor = SepoaDhtmlXGridObject;
SepoaDhtmlXGridObject.prototype.activeCopy=function(){
	this._initPasteEvent();
	this._pasteEvent = true;
}
SepoaDhtmlXGridObject.prototype.createModifyButton = function(id){
	var btn = document.createElement('div');
	btn.className="sepoa_modify_button grid_modify_button";
	// this.gridbox_id.insertBefore(btn, this.gridbox_id.firstChild);
	this.gridbox_id.appendChild(btn);
	btn.onclick=function(){
		var houseCode = Sepoa.info.HOUSE_CODE;
		var url = void 0;
		
		if(houseCode == '000') {
			url = window.CONTEXT_NAME+'/Admin/GridManagement/AdminGridManagementPopupLayout.sux';
		} else {
			url = window.CONTEXT_NAME+'/Admin/GridFilterManagement/AdminGridFilterManagementPopupLayout.sux';
		}
		
		PopupOpenPostReal(url
				, window.screen.width
				, window.screen.height
				, this.parentElement.id
				, {GRID_ID:id}
				, true);
	}
	btn.style.display="none";
}
SepoaDhtmlXGridObject.prototype.setReadOnly = function(f){
	this._allCellReadOnly = f;
}
SepoaDhtmlXGridObject.prototype.showButton = function (type){
	var gridElment = this.entBox
	if(typeof(type)=='boolean'){
		if(!type && gridElment.getElementsByClassName('movingArea').length > 0){
			gridElment.removeChild(gridElment.childNodes[2]);
		}
		return;
	}
	if(gridElment.getElementsByClassName('movingArea').length > 0){
		gridElment.removeChild(gridElment.childNodes[2]);
	}
	var grid = this;
	type = type.split(",");
	var div = document.createElement('div');
	div.className='movingArea';
	for(var _idx in type){
		var input = document.createElement('div');
		input.buttonType = type[_idx];
		input.className='imgButton';
		input.style.width='24px';
		input.style.height='24px';
		var img = document.createElement('img');
		img.src = window.CONTEXT_NAME+'/resource/design/imgs/grid/'+type[_idx].toUpperCase()+'.png';
		$(input).append(img);
		$(div).append(input);
		$(input).mousedown(function(){
			$(div)[0].flag = 0;
		});
		$(input).mouseup(function(){
			if($(div)[0].flag == 0 ){
				var id = this.buttonType;
				grid.callEvent('mbtClick',[id,grid]);
			}
		});
	}
	$(gridElment).append(div);
	div.style.position='absolute';
	div.style.display='flex';
	div.style.flexDirection='column';
	div.style.left = (this.entBox.offsetWidth -(div.offsetWidth))+'px';
	div.style.top  = (this.entBox.offsetHeight-(div.offsetHeight+10))+'px';
	$('.movingArea').draggable({ containment: "parent", start: function() {this.flag = 1;}, drag: function() {this.flag = 1;}, stop: function() {this.flag = 1;} });
	
	if(this._beforeShowButtonEventId) this.detachEvent(this._beforeShowButtonEventId);
	this._beforeShowButtonEventId = this.attachEvent('onSetSizes', function(){
		div.style.left = (this.entBox.offsetWidth -(div.offsetWidth))+'px';
		div.style.top  = (this.entBox.offsetHeight-(div.offsetHeight+10))+'px';
	});
}
SepoaDhtmlXGridObject.prototype.validate = function(selected, s){
	var o = this._validators;
	if (!o) {
		Alert("validator load fail!");
        return false;
    }
	var rowsNum = this.getRowsNum();
	var data = this._validators.data;
	var returnFlag = true;
	s = (s)?gridObj.getColIndexById(s):false;
	if(!selected){
		for(var ridx=0;ridx<rowsNum;ridx++){
			var r = this.getRowId(ridx);
			for(cidx in data){
				if((data[cidx] != "") || (s && s == cidx)){
					returnFlag = this.validateCell(r, cidx, ((s)?"NotEmpty":data[cidx])+((this.getColType(cidx)=="edn")?"_edn":"")) && returnFlag;
				}
			}
		}
	}else{
		var rdata = this._getSelectedData(selected);
		if(rdata == null) return false;
		var selectedRow = Object.keys(rdata);
		for(var rIdx in selectedRow){
			var r = selectedRow[rIdx];
			for(cidx in data){
				if((data[cidx] != "") || (s && s == cidx)){
					returnFlag = this.validateCell(r, cidx, ((s)?"NotEmpty":data[cidx])+((this.getColType(cidx)=="edn")?"_edn":"")) && returnFlag;
				}
			}
		}
	}
	return returnFlag;
}
SepoaDhtmlXGridObject.prototype.validationEvent = function(l, c, g, e, n) {
    var o = this._validators;
    if (!o) {
        return true
    }
    var m = (o.data[g] || this.cells(c, g).getAttribute("validate")) || "";
    m = (this.getColType(g) == "edn" && m.indexOf("NotEmpty")+1)?m.replaceAll("NotEmpty", "NotEmpty_edn"):m;
    if (l == 1 && m && (this.getColType(g) != "dhxCalendar") && (this.getColType(g) != "dhxCalendarYM")) {
        var h = this.editor || (this._fake || {}).editor;
        if (!h) {
            return true
        }
        h.cell.className = h.cell.className.replace(/[ ]*dhtmlx_validation_error/g, "");
        if (o._live) {
            var a = this;
            dhtmlxValidation.trackInput(h.getInput(), m, function(r, s, u) {
                return a.callEvent("onLiveValidationError", [c, g, s, r, u])
            }, function(r, s, u) {
                return a.callEvent("onLiveValidationCorrect", [c, g, s, r, u])
            })
        }
    }
    if (l == 2) {
        this.validateCell(c, g, m, e)
    }
    return true
};
SepoaDhtmlXGridObject.prototype.setTemplate = function(c, t){
	if(!this.grid_template[c]) this.grid_template[c]={};
	this.grid_template[c].template = t;
	this.grid_template[c].columns = window.dhx4.templateColumns(t);
}
SepoaDhtmlXGridObject.prototype.pasteEvent = function(flag){
	if(!flag){
		return;
	}
	this._pasteEvent = flag;
}
SepoaDhtmlXGridObject.prototype.addRowFromClipboard = function(a){
    var e = this.csvParser.unblock(a, "\t", "\n");
    for (var c = 0; c < e.length; c++) {
        if (e[c]) {
            a = e[c];
            if (!a.length) {
                continue
            }
            if (this._csvAID) {
                this.addRow(this.getRowsNum() + 2, a)
            } else {
                if (this.rowsAr[a[0]]) {
                    a[0] = this.uid()
                }
                this.addRow(a[0], a.slice(1))
            }
        }
    }
};
SepoaDhtmlXGridObject.prototype._initPasteEvent = function(){
	var thisObj = this;
	if((navigator.userAgent.indexOf("MSIE") >= 0) || (navigator.userAgent.indexOf("Trident") >= 0)){
		var t = this.gridbox_id
		if(typeof(this.gridbox_id) == 'string'){
			t = $('#'+t)
		}else{
			t = $(t);
		}
		t.keydown(function(e){
			if(e.ctrlKey && e.which == 86 && thisObj._pasteEvent){
				thisObj.setCSVDelimiter("\t");
				console.log(thisObj.fromClipBoard());
				thisObj.addRowFromClipboard(thisObj.fromClipBoard());
			}
		});
	}else if((navigator.userAgent.indexOf("Firefox") >= 0) || (navigator.userAgent.indexOf("Chrome") >= 0)){
		var tmpTarget = null;
		var copy = false;
		SUX.attachEvent('onKeyDown',function(obj){
			if(!thisObj._pasteEvent){
				return;
			}
			obj.originalTarget = (obj.originalTarget || obj.srcElement);
			if(obj.ctrlKey == true && obj.keyCode == 86 && obj.originalTarget.tagName.toLowerCase() == 'body' && !copy){
				copy = !copy;
				if(tmpTarget == null){
					tmpTarget = document.createElement('textarea');
					tmpTarget.style['z-index'] = -1000;
					tmpTarget.style['position'] = 'absolute';
					tmpTarget.style['left'] = $(obj.originalTarget).offset().left+'px';
					tmpTarget.style['top'] = $(obj.originalTarget).offset().top+'px';
					tmpTarget.style['height'] = 0+'px';
					tmpTarget.style['width'] = 0+'px';
					document.body.appendChild(tmpTarget);
				}
				tmpTarget.focus();
			}
		});
		SUX.attachEvent('onKeyUp',function(obj){
			if(!thisObj._pasteEvent){
				return;
			}
			if(copy){
				var data = tmpTarget.value;
				tmpTarget.remove();
				tmpTarget = null;
				thisObj.addRowFromClipboard(data);
				copy = !copy;
			}
		});
	}
}
SepoaDhtmlXGridObject.prototype.setGridDraw = function(){
	this.setImagePath(this.POASRM_CONTEXT_NAME+"/resource/plugin/dhtmlx/dhtmlxSuite_v501_pro/skins/web/imgs/");
	this.setHeader(this.grid_header);
	this.setInitWidths(this.grid_init_widths.join(","));
	this.setColAlign(this.grid_col_align.join(","));
	this.setColumnIds(this.grid_col_id.join(","));
	//this.setColumnColor(this.grid_col_color.join(","));
	this.setColSorting(this.grid_col_sort.join(","));
	this.setColTypes(this.grid_col_type.join(","));
	
	this._setCombo();
	
	this.setColumnMinWidth(50,0);
	this.enableMultiselect(true);
	this.enableEditEvents(true,false,false);
	
	const menuObj = new dhtmlXMenuObject(null,"material");
	menuObj.renderAsContextMenu();
	menuObj.setOpenMode("material");
	
	let menuIDs = [];
	//old
	const personalization = Sepoa.config.get("sepoa.grid.personalization", "old");
	if(personalization == "new"){
		if(this.SCREEN_ID !== undefined) {
			menuIDs.push("GRID_CONFIG");
		}
	}else{
		menuIDs.push("SAVE_SETTING", "CLEAR_SETTING");
	}
	//common
	menuIDs.push("SELECTED_CHECK_ALL", "SELECTED_UNCHECK", "CELL_DATA_COPY", "EXCEL_DOWN", "FREEZE", "UNFREEZE");
	
	
	
	for(let i=0;i<menuIDs.length;i++){
		menuObj.addNewChild(null, i, menuIDs[i], sUtils.cache.text.get(`BASE_GRID.MENU_${menuIDs[i]}`));
	}
	//MenuObj.loadStruct(this.POASRM_CONTEXT_NAME+"/resource/plugin/sepoasoft/dhtmlx_custom/contextmenu/dyn_context_"+Sepoa.info.LANGUAGE+".xml");
	menuObj.grid_obj = this;
	
	this.enableContextMenu(menuObj);
	
	const oGridObj = this;
	
	menuObj.attachEvent("onClick", (menuId)=>{
		const rcInfo = this.contextID.split("_");
		const rId  = rcInfo[0];
		const cInd = rcInfo[1];
		
	    switch(menuId) {
	    case "SAVE_SETTING":
	    	this._gridOrderWidthSave();
	    	break;
	    case "CLEAR_SETTING":
	    	this._gridOrderWidthClear();
	    	break;
	    case "GRID_CONFIG":
	    	this.openPersonalization(oGridObj);
	    	break;
	    case "SELECTED_CHECK_ALL":
	    	this.allRowsChecked(this.grid_selected || this.getColumnId(cInd), true);
	    	break;
	    case "SELECTED_UNCHECK":
	    	this.allRowsChecked(this.grid_selected || this.getColumnId(cInd), false);
	    	break;
	    case "CELL_DATA_COPY":
	    	let value = null;
        	if(["dhxCalendar","dhxCalendarA","dhxCalendarR","dhxCalendarYM","dhxCalendarYMA","dhxCalendarYMR","coro","coro2","templet","combo","comboro","comboro2"].indexOf(this.getColType(cInd)) >= 0) {
        		value = this.cells(rId, cInd).getTitle();
        	} else {
        		value = this.cells(rId, cInd).getValue().toString();
        	}
        	
        	var t = document.body.appendChild($('<textarea style="width:0px;height:0px;"></textarea>')[0]);
        	t.value = value;
        	t.select();
        	document.execCommand('copy');
        	document.body.removeChild(t);
	    	break;
	    case "EXCEL_DOWN":
	    	Confirm(sUtils.cache.text.get("BASE_GRID.MESSAGE_00001"), (state)=>{
	    		if(state === true || state === false){
		    		let rows = state?null:this._getSelectedData("SELECTED");
		    		let rowsCount = state?this.getRowsNum():(rows?.length || 0);
		    		let limitCount = Sepoa.config.get(`sepoa.grid.excel.download.${state?"all":"selected"}`) || 3000
		    		
		    		if(rowsCount == 0){
		    			Alert(sUtils.string.dataReplacer(sUtils.cache.text.get('BASE_GRID.MESSAGE_00006'), {ROW_COUNT: limitCount}));
		        		return;
		    		}else if(rowsCount > limitCount){
		    			Alert(sUtils.string.dataReplacer(sUtils.cache.text.get(`BASE_GRID.MESSAGE_0000${state?"4":"5"}`), {ROW_COUNT: limitCount}));
		    			return;
		    		}
		    		this._gridExport(rows);
	    		}
	    	}, null, sUtils.cache.text.get("BASE_GRID.MESSAGE_00002"), sUtils.cache.text.get("BASE_GRID.MESSAGE_00003"), null, [sUtils.cache.text.get('BASE_BUTTON.CANCEL')]);
	    	break;
	    case "FREEZE":
	    	if(cInd){
	    		this.splitAt(parseInt(cInd) + 1);
	    	}
	    	break;
	    case "UNFREEZE":
	    	this.unSplitAt();
	    	break;
	    }
	});
	
	this.attachEvent("onBeforeContextMenu", (rowId,celInd,grid)=>{
		if(this._header_menu){
			this._header_menu.hide()
		}
		if(this._ctmndx){
			// _enableExcelDownload의 값에 따라 hide,show
			if(!this._enableExcelDownload) {
				this._ctmndx.hideItem("EXCEL_DOWN");
			} else {
				this._ctmndx.showItem("EXCEL_DOWN");
			}
			this._ctmndx.hide();
		}
		
		if(celInd !== undefined){
			this._ctmndx.showItem("CELL_DATA_COPY");
			this._ctmndx.showItem("EXCEL_DOWN");
			this._ctmndx.showItem("SELECTED_UNCHECK");
			this._ctmndx.showItem("SELECTED_CHECK_ALL");
			this._ctmndx.showItem("FREEZE");
			if(this._fake){
				this._ctmndx.showItem("UNFREEZE");
			}else{
				this._ctmndx.hideItem("UNFREEZE");
			}
		}else{
			this._ctmndx.hideItem("CELL_DATA_COPY");
			this._ctmndx.hideItem("EXCEL_DOWN");
			this._ctmndx.hideItem("SELECTED_UNCHECK");
			this._ctmndx.hideItem("SELECTED_CHECK_ALL");
			this._ctmndx.hideItem("FREEZE");
			this._ctmndx.hideItem("UNFREEZE");
		}
		return true;
	});
	
	this.attachEvent("onBeforePageChanged", this._doOnBeforePageChanged);
	this.attachEvent("onXLS",this._doQueryDuring);
	this.attachEvent("onXLE",this._doQueryModalEnd);
	this.attachEvent("onXLE",this._doQueryEnd);
	this.attachEvent("onEmptyClick",this.editStop);
	this.attachEvent("onEmptyClick",(ev)=>{
		var isRightMB;
		if ("which" in ev){
	        isRightMB = ev.which == 3; 
		}else if ("button" in e){ 
	        isRightMB = ev.button == 2; 
	    }
		if(isRightMB && !ev.target.closest('.hdr')){
			if (this._ctmndx) {
				if (!(this.callEvent("onBeforeContextMenu", [undefined, undefined, this]))) {
					return true
	            }
                if (_isIE) {
                    ev.srcElement.oncontextmenu = function() {
                        event.cancelBubble = true;
                        return false
                    }
                }
                if (this._ctmndx.showContextMenu) {
                    var dEl0 = window.document.documentElement;
                    var dEl1 = window.document.body;
                    var corrector = new Array((dEl0.scrollLeft || dEl1.scrollLeft), (dEl0.scrollTop || dEl1.scrollTop));
                    if (_isIE) {
                        var x = ev.clientX + corrector[0];
                        var y = ev.clientY + corrector[1]
                    } else {
                        var x = ev.pageX;
                        var y = ev.pageY
                    }
                    this._ctmndx.showContextMenu(x - 1, y - 1);
                    this.contextID = this._ctmndx.contextMenuZoneId = x+"_"+y;
                    this._ctmndx._skip_hide = true
                } 
                ev.cancelBubble = true;
                return false
            }
		}else{
			if (this._ctmndx.hideContextMenu) {
				this._ctmndx.hideContextMenu()
            } else {
            	this._ctmndx._contextEnd()
            }
		}
		
		if(this._header_menu){
			this._header_menu.hide()
		}
	});
	
	this.setSkin("dhx_web");
	this.entBox.classList.add('sepoaux_grid');
	if(!this._isSelectScreen) {
	    this.attachEvent("onEditCell",this._doOnCellEdit);
	}
	this.attachEvent("onRowSelect", this._doOnRowSelect);
	
	this.attachEvent("onCellChanged",this._templateChange);
	this.attachEvent("onEditCell",this._templateChange_edit);
    this.attachEvent("onEditCell",this._editBoxTextBlock);

    this.attachEvent("onRowCreated",this._doOnRowCreated);
    
    this.attachEvent("onAfterCMove", function() {
    	if(this.getHeaderFilterIdx() >= 0) {
    		this.disableHeaderFilter();
        	this.enableHeaderFilter();
    	}
    });
    
    this.enableUndoRedo();
    this.enableBlockSelection(false);
	this.enableColumnMove(true);
	this.setDateFormat(this.grid_date_format);
	
	// this.attachFooter(this.grid_footer_msg.join(","));

	for(var _idx in this.grid_num_format){
		this.setNumberFormat(this.grid_num_format[_idx], this.getColIndexById(_idx), this.number_sep.decimal, this.number_sep.group);
	}
	
	for(var _idx in this.grid_col_visible){
		this.setColumnHidden(this.getColIndexById(this.grid_col_visible[_idx]), true);
	}
	
	if(this._isRowsMergeable) {
		this.enableRowspan(true);
	}else{
		this.enableSmartRendering(true);
	}

	this.init();
	
	this.setAwaitedRowHeight(39);


	this.enableCellIds(true);
	
	for(var _idx in this.grid_col_visible){
		this.setColumnHidden(this.grid_col_visible[_idx]);
	}

	// 새끼 그리드 추가 부분
	if(this.grid_col_type.indexOf("sub_row_grid") >= 0) {
    	// this.attachEvent("onSubGridCreated", sub_grid_callback);
	}

	if(typeof document.form_footer != "undefined" && typeof document.form_footer.setGridDrawEnd != "undefined"){
		document.form_footer.setGridDrawEnd.value = 'true';	// 그리드 초기화가 완료되면, 값을
															// true로 갱신한다.
	}
	
	this.noDataScroll();
	this.enableValidation(true, true);
	this.setColValidators(this.grid_required);

	if(this._onDnDFlag){
		this.enableDragAndDrop(true);
	}else{
		this.enableBlockSelection();
	}
	this.attachEvent("onKeyPress",this._onKeyPressed);
	
	if(personalization == "old"){
		var headerMenu = [];
		if(Sepoa.config.get("sepoa.grid.headerMenu.flag") != 'false'){
			for(i in this.grid_init_widths){
				headerMenu.push((this.grid_init_widths[i] || 0) > 0);
			}
			this.enableHeaderMenu(headerMenu.join(','));
			this._header_menu.attachEvent("onBeforeContextMenu", (zone, ev)=>{
				return !ev.target.closest('.filter')
			});
		}
	}
	
	if(personalization == "new") {	
		this.renderGridPersonalization(false);
	}
}

SepoaDhtmlXGridObject.prototype.renderGridPersonalization = function(flag) {
	if(this.SCREEN_ID === undefined) {
		return;
	}
	var personalData = Sepoa.cache.personal.get('GRID.' + this.SCREEN_ID);
	if(personalData !== null) {
		var headers = personalData.state.headers;
		var freeze = personalData.state.freeze;
		
		if(headers.length > 0) {
			if(this._fake !== undefined) {
				this.unSplitAt();
			}
			
			var cspanCnt = 0;
			for(var i = 0; i < headers.length; i++) {
				var colIndex = this.getColIndexById(headers[i].id);
				if(colIndex != undefined) {
					var hidden = headers[i].hidden;
					var width = hidden ? 0 : headers[i].width;
					
					if(width == '') {
						width = this.initCellWidth[colIndex];
					}
					
					this.initCellWidth[colIndex] = parseInt(width);
					this.setColWidth(colIndex, parseInt(width));
					
					this.setColumnHidden(colIndex, hidden);
					
					var nextItem = headers[i + 1];
					if(nextItem !== undefined && nextItem.label == '#cspan') {
						cspanCnt++;
						continue;
					} else {
						if(headers[i].label == '#cspan') {
							this.moveColumn(colIndex, i - cspanCnt);
							cspanCnt = 0;
						} else {
							this.moveColumn(colIndex, i);
						}
					}
				}
			}
		}
		
		if(freeze !== undefined) {
			this.splitAt(this.getColIndexById(freeze) + 1);
		}
		
		if(!flag) {
			this.clearAll(false);
		}
		
	} else {
		if(flag) {
			if(this._fake !== undefined) {
				this.unSplitAt();
			}
			
			var oGridData = Sepoa.cache.grid.get(this.SCREEN_ID);
			if(oGridData === null) {
				return;
			}
			
			let gridAttributeArray = [];
		    if(oGridData && Object.keys(oGridData).length > 0){
			    Object.keys(oGridData).forEach((k)=>{
			    	gridAttributeArray.push(oGridData[k]);
			    })
			    gridAttributeArray.sort((a, b)=>{
			    	if(parseFloat(a.COL_SEQ) > parseFloat(b.COL_SEQ)) {
						return 1;
					}
					if(parseFloat(a.COL_SEQ) < parseFloat(b.COL_SEQ)) {
						return -1;
					}
					
					return 0;
			    })
		    }
		    
		    var colIds = this.grid_col_id;
			var nowColIds = this.getColIdsArray();
		    
			if(gridAttributeArray.length > 0) {
				for(var i = 0; i < gridAttributeArray.length; i++) {
					var oGridData = gridAttributeArray[i];
					var oColId = oGridData.CODE;
					var nowIndex = this.getColIndexById(oColId);
					var isVisible = oGridData.COL_VISIBLE === 'true' ? true : false;
					
					this.initCellWidth[nowIndex] = parseInt(oGridData.COL_WIDTH);
					this.setColWidth(nowIndex, parseInt(oGridData.COL_WIDTH));
					
					this.setColumnHidden(nowIndex, !isVisible);
					
					if(nowIndex !== i) {
						this.moveColumn(nowIndex, i);
					}
				}
			}
		}
	}
	
	this.setSizes();
}

SepoaDhtmlXGridObject.prototype.openPersonalization = function(data) {
	PopupOpenPost('/Tools/GridPersonalization/GridPersonalizationLayout.sux'
        , {gridObj : data}
        , (flag)=>{
        	this.renderGridPersonalization(flag);
          }
        , 1600
        , 800
        , sUtils.cache.text.get('BASE_GRID.GRID_PERSONALIZATION')
    );
}

SepoaDhtmlXGridObject.prototype._onKeyPressed = function(code,ctrl,shift){
	if(code==67&&ctrl){
		// if (!this._selectionArea) return alert("You need to select a block
		// area in grid first");
		this.setCSVDelimiter("\t");
		this.copyBlockToClipboard()
	}
	// else if(code==86&&ctrl){
	// this.setCSVDelimiter("\t");
	// this.pasteBlockFromClipboard()
	// }
	return true;
}

SepoaDhtmlXGridObject.prototype._templateChange = function(rowId,cellInd){
	var cId = this.getColumnId(cellInd);
	for(var tId in this.grid_template){
		if(this.grid_template[tId].columns.indexOf(cId) >= 0){
			this.cells(rowId, this.getColIndexById(tId)).setValue();
		}
	}	
}
SepoaDhtmlXGridObject.prototype._templateChange_edit = function(stage, rowId,cellInd){
	if(stage == 2){
		this._templateChange(rowId, cellInd);
	}
	return true;
}
SepoaDhtmlXGridObject.prototype._doQueryDuring = function(){
	sUtils.progress.on();
	return true;
}
SepoaDhtmlXGridObject.prototype._doQueryModalEnd = function(){
	sUtils.progress.off();
	return true;
}
SepoaDhtmlXGridObject.prototype._setCombo = function (){
	if(!this._ajaxStatusW.combo) this._ajaxStatusW.combo = 0;
	this._col_combos = [];
	for(var _idx in this.grid_combo_data){
		var comboInfo = this.grid_combo_data[_idx];
		
		var cellIndex = this.getColIndexById(comboInfo.COLUMN);
		this._col_combos[cellIndex] = window["eXcell_"+this.cellType[cellIndex]].prototype.initCombo.call({
            grid: this
        }, cellIndex) 
		
		if(comboInfo.DATA){
			var combobox = this.getCombo(comboInfo.COLUMN);
			var dhtmlxcombo = this._col_combos[cellIndex];
			
			combobox.clear();
			dhtmlxcombo.clearAll();
			
			combobox.put("", "");
			dhtmlxcombo.addOption("","");
			
			var data = comboInfo.DATA
			
			for(var idx in data) {
				combobox.put(data[idx].value, data[idx].text);
				dhtmlxcombo.addOption(data[idx].value, data[idx].text);
			}
		}else{
			this._loadCombo(comboInfo.CODE,comboInfo.PARAMS,comboInfo.COLUMN,comboInfo.DEFAULT);
		}
	}
}
SepoaDhtmlXGridObject.prototype.setSelectScreen = function(isSelectScreen){
	this._isSelectScreen = isSelectScreen;
}
SepoaDhtmlXGridObject.prototype.setRowsMergeable = function(isRowsMergeable){
	this._isRowsMergeable = isRowsMergeable;
}

// 엑셀 다운로드 on/off 기능 추가
SepoaDhtmlXGridObject.prototype.setExcelDownload = function(flag){ 
	this._enableExcelDownload = flag;
}

SepoaDhtmlXGridObject.prototype.allRowsChecked = function(cId, flag){
	var cInd = this.getColIndexById(cId);
	if(this.getColType(cInd) == "ch" && this._isSelectScreen == false) {
		if(this.getRowsNum() > 1000){
			Alert("데이터 조회 건수가 많습니다. 1000 건 까지만 처리 가능합니다.");
			return;
		} else {
			for(var row = 0; row < this.getRowsNum(); row++){
				this.cells(this.getRowId(row), cInd).setValue((flag)?"1":"0");
				this.cells(this.getRowId(row), cInd).cell.wasChanged = flag;
		    }
		}
	}
}
SepoaDhtmlXGridObject.prototype._doOnPageChanged = function(cur_page, first_page, last_page){
	if(this._dhxwins != null && this._prg_win != null && this._dhxwins.window("this._prg_win").isModal()) {
	}

	var page_cnt = parseInt(this.page_count);
	dhtmlx_start_row_id   = (cur_page - 1) * page_cnt;
	dhtmlx_end_row_id = cur_page * page_cnt;
	dhtmlx_cur_pagenumber = cur_page;

	if(GridObj.getRowsNum() <= page_cnt) {
		dhtmlx_end_row_id = GridObj.getRowsNum();
		dhtmlx_cur_page_last_idx = GridObj.getRowsNum();
	} else {
		dhtmlx_cur_page_last_idx = cur_page * page_cnt;
	}

	if(dhtmlx_end_row_id > GridObj.getRowsNum()) {
		dhtmlx_end_row_id = GridObj.getRowsNum();
	}

	return true;
}
SepoaDhtmlXGridObject.prototype._doOnBeforePageChanged = function(cur_page, page_cnt){
	if(this._dhxwins != null && this._prg_win != null && !this._dhxwins.window("this._prg_win").isModal()) {
		// this._dhxwins.window("this._prg_win").setModal(true);
		// this._dhxwins.window("this._prg_win").show();
	}

	return true;
}
SepoaDhtmlXGridObject.prototype._gridOrderWidthSave =function()
{
    var html="";
    var headText="";
    
    var numCols = this.getColumnsNum();
    var data = [];
    data.push(["ID","WIDTH","HIDDEN"].join(this.COL_DEL)+this.COL_DEL);
    for(i=0; i < numCols; i++){
    	data.push([this.getColumnId(i), (this.isColumnHidden(i)?this.initCellWidth[i]:this.getColWidth(i)), !this.isColumnHidden(i)].join(this.COL_DEL)+this.COL_DEL);
    }

    $.ajax({
		url: this.POASRM_CONTEXT_NAME+"/SepoaGridUserCustom.sux?timeStamp=" + new Date().getTime(),
		type:"post",
		data: {
			headText:data.join(this.ROW_DEL)+this.ROW_DEL,
			screen_id:this.SCREEN_ID,
			user_id:Sepoa.info.ID,
			mode:"save"
		},
		success: function(result){
			if(result.trim() == 'S'){
				Alert(Sepoa.cache.text.get('MESSAGE.0001'));
			}else{
				Alert(Sepoa.cache.text.get('MESSAGE.1002'));
			}
		},
		error:function(){
			Alert(Sepoa.cache.text.get('MESSAGE.1002'));
		}
	});
    return;
}
SepoaDhtmlXGridObject.prototype._gridExport = function(rows){
	var gridObj = this;
	var params = {
			SCREEN_ID : getScreenId(this.gridbox_id),
			MODE : "CHECK"
	}
	// 이미 진행중인 엑셀 다운로드인지 확인
	var ajaxParams = {
			url: window.CONTEXT_NAME+"/DhtmlxExcelGenerator.sux",
			dataType:"json",
			async:true,
			success: function(data){
				if(data.flag == "true"){
					if(data.mode == "stop"){
						Alert(Sepoa.cache.text.get("BASE_GRID.DUPLICATE_EXCEL_DOWNLOAD")); // 동일 아이디/화면에서 다운로드가 진행중입니다.
					} else if(data.mode == "download"){
						gridObj.toExcel(window.CONTEXT_NAME+'/DhtmlxExcelGenerator.sux?SCREEN_ID=' + getScreenId(gridObj.gridbox_id), 'color', undefined, undefined, rows);
						var msg = Sepoa.cache.text.get("BASE_GRID.EXCEL_DOWNLOAD_START"); // 엑셀 다운로드가 시작되었습니다.
						var timeOut = 5000;
						if(typeof(Message) === 'function') {
							Message(msg, timeOut);
						} else {
							SUX.root.Message(msg, timeOut);
						}
					} else {
						Alert(Sepoa.cache.text.get("BASE_GRID.EXCEL_MODE_CHECK")); // Mode를 찾을 수 없습니다.
					}
				} else {
					Alert(Sepoa.cache.text.get('BASE_MESSAGE.0003'));
				}
			},
			error:function(request, status, error){
				Alert(Sepoa.cache.text.get("BASE_CONNECTOR.ERROR_UNKNOWN")); // 서버와의 통신중 에러가 발생하였습니다.
			}
		};
	
	sUtils.loader.common(params, ajaxParams);
}
SepoaDhtmlXGridObject.prototype._gridOrderWidthClear = function(){
	$.ajax({
		url: this.POASRM_CONTEXT_NAME+"/SepoaGridUserCustom.sux?timeStamp=" + new Date().getTime(),
		type:"post",
		data: {
			screen_id:this.SCREEN_ID,
			user_id:Sepoa.info.ID,
			mode:"clear"
		},
		success: function(result){
			if(result.trim() == 'S'){
				Alert(Sepoa.cache.text.get('MESSAGE.0001'));
			}else{
				Alert(Sepoa.cache.text.get('MESSAGE.1002'));
			}
		},
		error:function(){
			Alert(Sepoa.cache.text.get('MESSAGE.1002'));
		}
	});
	return;
}
SepoaDhtmlXGridObject.prototype._PrintView = function(){
    var html="";
    var headText="";
    var gridColids="";
    var numCols = this.getColumnsNum();

    if(this.getRowsNum() < 1) return;

    for(i=0; i < numCols; i++)
    {
    	if(!this.isColumnHidden(i) && this.getColumnId(i) != this.grid_selected) {
    		headText   = headText + this.getColumnLabel(i);
    	    gridColids = gridColids + this.getColumnId(i);
    	}
    }

    document.formstyle.headText.value=headText;
    document.formstyle.gridColids.value=gridColids;
    document.formstyle.method='POST';
    document.formstyle.action=this.POASRM_CONTEXT_NAME+'/common/printView.jsp';
    document.formstyle.target='_blank';
    document.formstyle.submit();
    return;
}
SepoaDhtmlXGridObject.prototype._doFiltering = function(){
	var dim  = new Array(2);
	var width  = "550";
	var height = "120";
	var top = this.BwindowWidth()/2 - 180;
	var left  = this._BwindowHeight()/2 - 73;
	var headText="";
    var numCols = this.getColumnsNum();
    var gridColids = "";

    if(this.getRowsNum() < 1) return;

    for(i=0; i < numCols; i++)
    {
    	if(!this.isColumnHidden(i)) {
    		headText = headText + this.getColumnLabel(i)+ ",";
    		gridColids = gridColids + this.getColumnId(i)+ ",";
    	}
    }

    if(headText.length > 0) {
    	headText = headText.substring(0, headText.length - 1);
    }

    if(gridColids.length > 0) {
    	gridColids = gridColids.substring(0, gridColids.length - 1);
    }

	var url = this.POASRM_CONTEXT_NAME+"/common/do_filter.jsp?grid_col_id="+gridColids+"&grid_col_lable="+headText;
	CodeSearchCommon(url, 'FiterScreen', left, top, width, height);
}
SepoaDhtmlXGridObject.prototype.BwindowWidth = function() {
	if (window.innerWidth) {
		return window.innerWidth;
	} else if (document.body && document.body.offsetWidth) {
		return document.body.offsetWidth;
	} else {
		return 0;
	}
}
SepoaDhtmlXGridObject.prototype.BwindowHeight = function() {
	if (window.innerHeight) {
		return window.innerHeight;
	} else if (document.body && document.body.offsetHeight) {
		return document.body.offsetHeight;
	} else {
		return 0;
	}
}
SepoaDhtmlXGridObject.prototype._editBoxTextBlock = function(stage,rowId,cellInd){
	if(stage==0) {
		return true;
	} else if(stage==1) {
		try{
			if(this.cells(rowId,cellInd).getInput()){
				this.cells(rowId,cellInd).getInput().select();
			}else{
				this.editor.obj.select();
			}
		}catch(e){}
		return true;
	} else if(stage==2) {
		return true;
	}
	return false;
}

SepoaDhtmlXGridObject.prototype._doOnRowSelect = function(id, ind){
	if(this.getColType(ind) == "ch"){
		if(!this.callEvent('onEditCell',["0", id, ind])){
			return false;
		}
		
		var oValue = dhx.s2b(this.cells(id, ind).getValue());
		
		if(this.callEvent('onEditCell',["1", id, ind, !oValue, oValue])){
			this.cells(id, ind).setValue(!oValue);
		}
		
	}else if(this.getColType(ind).indexOf('sub_row')+1){
		var c = this.cells(id, ind);
		if(c.isOpen()){
			c.close();
		}else{
			c.open();
		}
	}
}
SepoaDhtmlXGridObject.prototype._doOnCellEdit = function(stage,rowId,cellInd,nValue, oValue){
	var max_value = this.cells(rowId, cellInd).getValue();
	var _gridAttribute = this._gridAttribute;
	
	// stage = 0 현재상태, 1 = 수정이전상태, 2 = 수정후상태, true 수정후값이 적용되며 false 는 수정이전값으로
	// 적용됩니다.
	if(stage==0) {
		if(this._allCellReadOnly){
			if(this.getColIndexById(this.grid_selected) != cellInd){
				return false;
			}
		}
		return true;
	} else if(stage==1) {
		if(this.getColType(cellInd) == "ch" && cellInd != this.getColIndexById(this.grid_selected)) {
			this.cells(rowId, this.getColIndexById(this.grid_selected)).setValue("1");
			this.cells(rowId, this.getColIndexById(this.grid_selected)).cell.wasChanged = true;
			return true;
		}
		return true;
	} else if(stage==2) {
		if(!this.isSelectScreen) {
			for(var _idx in _gridAttribute){
				var grid_column_type = _gridAttribute[_idx].COL_TYPE;
				if(grid_column_type == "edn"){
					var cells = this.cells(rowId, cellInd);
					var cellMask = this._maskArr[cellInd];
					if(cellMask){
					    var nValue = (cells.getValue()+"").replaceAll(cellMask[5], "");
					    if(nValue != "" && nValue != "-"){
					    	var nValue = this._aplNF(nValue, cellInd);
					    	cells.setValue(nValue.replaceAll(cellMask[5], ""));
					    }
					}
				}
				if(grid_column_type == "edn" && _gridAttribute[_idx].COL_MAX_LEN.length > 0 && _gridAttribute[_idx].COL_FORMAT.length > 0){
					if(cellInd == this.getColIndexById(_gridAttribute[_idx].CODE)){
						if(max_value == "0" || isNaN(max_value)) {
							if(this.grid_selected != null && this.grid_selected.length > 0){
								// this.cells(rowId,
								// this.getColIndexById(this.grid_selected)).setValue("0");
							}
							return true;
						}else if(this._getLength(max_value) > _gridAttribute[_idx].COL_MAX_LEN) {
							if(this.grid_selected != null && this.grid_selected.length > 0){
								alert("칼럼의 최대 입력 길이를 초과했습니다.\n입력가능 Bytes : "+_gridAttribute[_idx].COL_MAX_LEN+", 입력한 Bytes : " + this._getLength(max_value));
								this.cells(rowId, cellInd).setValue("0");
							}
							return false;
						} else {
							if(this.grid_selected != null && this.grid_selected.length > 0 && nValue != oValue) {
								this.cells(rowId, this.getColIndexById(this.grid_selected)).setValue("1");
								this.cells(rowId, this.getColIndexById(this.grid_selected)).cell.wasChanged = true;
							}
							return true;
						}
					}
				}else if(grid_column_type == "ed" || grid_column_type == "edtxt" || grid_column_type == "txt" || grid_column_type == "txttxt" && _gridAttribute[_idx].COL_MAX_LEN.length > 0 && _gridAttribute[_idx].COL_FORMAT == 0){
					if(cellInd == this.getColIndexById(_gridAttribute[_idx].CODE)){
						if(this._getLength(max_value) > _gridAttribute[_idx].COL_MAX_LEN){
							if(this.grid_selected != null && this.grid_selected.length > 0){
								alert("칼럼의 최대 입력 길이를 초과했습니다.\n입력가능 Bytes : "+_gridAttribute[_idx].COL_MAX_LEN+", 입력한 Bytes : " + this._getLength(max_value));
								this.cells(rowId, this.getColIndexById(this.grid_selected)).setValue("0");
							}
							return false;
						} else{
							if(this.grid_selected != null && this.grid_selected.length > 0 && nValue != oValue){
								this.cells(rowId, this.getColIndexById(this.grid_selected)).setValue("1");
								this.cells(rowId, this.getColIndexById(this.grid_selected)).cell.wasChanged = true;
							}
							return true;
						}
					}
				}
			}
		}
		if(this.grid_selected != null && this.grid_selected.length > 0 && nValue != oValue) {
			this.cells(rowId, this.getColIndexById(this.grid_selected)).setValue("1");
			this.cells(rowId, this.getColIndexById(this.grid_selected)).cell.wasChanged = true;
		}
		return true;
	}
	return false;
}
SepoaDhtmlXGridObject.prototype._getLength=function(chkstr){
    var j, lng = 0;
    for (j=0; j<chkstr.length; j++){
        if (chkstr.charCodeAt(j) > 255){
            ++lng;
        }
		++lng;
    }
    return lng;
}
SepoaDhtmlXGridObject.prototype._doQueryEnd = function(obj, RowCnt){
	
    var usrData = obj.UserData.gridglobaluserdata;
    if(usrData){
	    var usrDataKeys = usrData.getKeys();
	    var usrcnt = usrDataKeys.length;
	
		for(var i=0;i<usrcnt;i++){
			this.returnObj[usrDataKeys[i]] = usrData.get(usrDataKeys[i]);
		}
		if(this.returnObj.status == "SESSIONCLOSE"){
			sUtils.system.login();
			return;
		}
    }
	this.noDataScroll();
}
SepoaDhtmlXGridObject.prototype._doSaveEnd = function(obj){
	var return_obj = {};
	var attcnt = obj.attributes.length;
	for(var i=0;i<attcnt;i++){
		var node_name = obj.attributes[i].name;
		this.returnObj[node_name] = obj.attributes[i].nodeValue;
	}
	if(this.returnObj.status == "SESSIONCLOSE"){
		sUtils.system.login();
		return;
	}
	
	this.callEvent('onTXE',[this.returnObj]);
}
SepoaDhtmlXGridObject.prototype.getColIds = function(){
	return this.columnIds.join(',');
}
SepoaDhtmlXGridObject.prototype.getColIdsArray = function(){
	return this.columnIds;
}
SepoaDhtmlXGridObject.prototype.post = function(mode, url, user_param, selected){
	if(mode == "CONNECTION"){
		this.postConnection(url, user_param);
	}else if(mode == "TRANSACTION"){
		this.postTransaction(url, user_param, selected);
	}
}
SepoaDhtmlXGridObject.prototype.getSFSelectedData = function(selected){
	var a1 = this._getSelectedData(selected);
	return this._getSepoaFormatData(a1);
}
SepoaDhtmlXGridObject.prototype.addOtherGridData = function(obj,s){
	this._otherGridData[obj.grid_id] = obj.getSFSelectedData(s); 
}
SepoaDhtmlXGridObject.prototype.postTransaction = function(url, user_param, selected){
	checkContextUrl(url);
	selected = (selected||"SELECTED_ALL_ROWS");
	var a1 = this._getSelectedData(selected);
	var param = [];
	var objNames = [this.grid_id];
	param.push((this.dataOutput||dataOutput).getParams());
	param.push(this._getSepoaFormatData(a1));
	param.push("defaultGrid="+this.grid_id);
	for(var _id in this._otherGridData){
		param.push(this._otherGridData[_id]);
		objNames.push(_id);
	}
	this._otherGridData = {};
	param.push("gridObjNames="+objNames.join(","));
	if(!(!user_param)){
		for(var _idx in user_param){
			param.push(user_param[_idx]);
		}
	}
	param.push("requestMode=postTransaction");
	var post = param.join("&");
	this.callEvent("onXLS", [this]);
	var call;
	var type;
	if (arguments.length == 2 && typeof call != "function"){
		type=call;
		call=null;
	}
	type="xml";

	if (!this.xmlFileUrl)
		this.xmlFileUrl=url;
	this._data_type=type;

	var that = this;
	var xmlLoader = function(xml){
		var ids = [];
		if (a1)
			for (var key in a1)
				ids.push(key);

		return that.afterUpdate(that,xml,ids);
	};
	dhx4.ajax.post(url, (post||""), xmlLoader);
	this._CreateSelection(0,0)
}
SepoaDhtmlXGridObject.prototype._getSelectedData = function(selected){
	var out={};
	var has_one = false;

	var that = this;
	
	var getSubItem = function(rId){
		var sub = that.getSubItems(rId);
		if(sub != null && sub != ""){
			sub = sub.split(',');
			for(var idx in sub){
				out[sub[idx]]=that._getRowData(sub[idx]);
				getSubItem(sub[idx]);
			}
		}
	}
	
	if(selected != "SELECTED_ALL_ROWS"){
		var grid_array = this.getGridChangedRows(selected);
	
		for(var i=0;i<grid_array.length;i++){
			var id=grid_array[i];
			out[id]=this._getRowData(id);
			has_one = true;			
		}		
	}else{
		var hasTree = false;
		if(this.grid_col_type.indexOf('tree')>=0){
			hasTree = true;
		}
		for(var i=0;i<this.getRowsNum();i++){
			var id=this.getRowId(i);
			out[id]=this._getRowData(id);
			if(hasTree) getSubItem(id);
			has_one = true;
		}
	}
	return has_one?out:null;
}

SepoaDhtmlXGridObject.prototype.checkSelectedRow = function(selected){
	if(!selected) return 0; 
	var data = this._getSelectedData(selected);
	if(data == null){
		return 0;
	}else return Object.keys(data).length;
}
SepoaDhtmlXGridObject.prototype._getSepoaFormatData = function(grid_data){
	var col_ids = this.columnIds;
	
	var _rdata = [];
	_rdata.push(col_ids.join(this.COL_DEL));
	
	for(var _rkey in grid_data){
		var _cdata = [];
		for(var _ckey in col_ids){
			_cdata.push((grid_data[_rkey][col_ids[_ckey]] !== null)?grid_data[_rkey][col_ids[_ckey]]:"");
		}
		_rdata.push(_cdata.join(this.COL_DEL));
	}
	var encodeData = encodeUrl(_rdata.join(this.COL_DEL+this.ROW_DEL)+this.COL_DEL+this.ROW_DEL);
	return this.grid_id+"="+encodeData;
}
SepoaDhtmlXGridObject.prototype._getRowData=function(rowId){
	var data = [];
	
	data["gr_id"]=rowId;
	
	var r=this.getRowById(rowId);
	for (var i=0; i<this._cCount; i++){
		if (this._c_order)
	   		var i_c=this._c_order[i];
	   	else
		   	var i_c=i;
		
		var c=this.cells(r.idd,i);
		
		data[this.getColumnId(i)]=c.getValue();
		
	}
	return data;
}
SepoaDhtmlXGridObject.prototype.getRowDataObject=function(rowId){
	var data = {};
	
	data["gr_id"]=rowId;
	
	var r=this.getRowById(rowId);
	for (var i=0; i<this._cCount; i++){
		if (this._c_order)
	   		var i_c=this._c_order[i];
	   	else
		   	var i_c=i;
		
		var c=this.cells(r.idd,i);
		
		data[this.getColumnId(i)]=c.getValue();
		
	}
	return data;
}
SepoaDhtmlXGridObject.prototype.loadExcel = function(param){
	sUtils.excel.upload($.extend(true, {RETURN_DATA:"true"}, param || {}), function(so){
		if(so.status=="true"){
			this.clearAll(false);
			this.parse(JSON.parse(so.result[0]), "js");
		}else{
			Alert("BASE_MESSAGE.0003");
		}
	}.bind(this))
}
SepoaDhtmlXGridObject.prototype.postConnection = function(url, user_param){
	checkContextUrl(url);
	if(window.CONTEXT_NAME && url.indexOf(window.CONTEXT_NAME) != 0){
		url = (window.CONTEXT_NAME || '') + url;
	}
	
	var param = [];
	param.push((this.dataOutput||dataOutput).getParams());
    param.push("grid_col_id=" + (this.grid_col_id.length?this.grid_col_id:this.columnIds).join(','));
	if(!(!user_param)){
		for(var _idx in user_param){
			param.push(user_param[_idx]);
		}
	}
	param.push("requestMode=postConnection");
	var post = param.join("&") || "";
	var call;
	var type;
	if (arguments.length == 2 && typeof call != "function"){
		type=call;
		call=null;
	}
	type="xml";
	this._data_type=type;
	var that = this;
	var xmlLoader = function(xml){
		that._doQueryModalEnd();
		if (!that.callEvent) return;
		
		that.clearAll(false);
		that["_process_"+type](xml.xmlDoc);
		if (!that._contextCallTimer)
			that.callEvent("onXLE", [that,that.getRowsNum(),0,xml.xmlDoc]);
		if (call){
			call();
			call=null;
		}
		
		if (!that.xmlFileUrl){
			that.xmlFileUrl=url;
			that.xmlFileParam=param;
		}
	};
	
	if(!this._checkAjaxStatus()){
		this._ajaxDelayEvent = this.attachEvent('alreadyCallConnection',function(){
			this.callEvent("onXLS", [this]);
			dhx4.ajax.post(url, post, xmlLoader);
			that.detachEvent(this._ajaxDelayEvent);
		})
	}else{
		this.callEvent("onXLS", [this]);
		dhx4.ajax.post(url, post, xmlLoader);
	}
	this._CreateSelection(0,0)
}
SepoaDhtmlXGridObject.prototype.getGridChangedRows = function(selected){
	var res = [];
	var selectedIdx = this.getColIndexById(selected);

	for(var i=0;i<this.getRowsNum();i++){
		var id = this.getRowId(i);
		if(this.getCellValue(id, selected)){
        	res.push(id);
        }
	}
	return res;
}
SepoaDhtmlXGridObject.prototype.getSelectedRows = SepoaDhtmlXGridObject.prototype.getGridChangedRows; 
SepoaDhtmlXGridObject.prototype.gridGetCellValueId = function(row_id, column_name){
	if(this.getColType(this.getColIndexById(column_name)) == "ch") {
 	    if(this.cells(row_id, this.getColIndexById(column_name)).getValue() == 0) {
			return 'false';
	    } else {
		   	return 'true';
	    }
   	} else {
   		return this.cells(row_id, this.getColIndexById(column_name)).getValue();
   	}
}
SepoaDhtmlXGridObject.prototype.getCellValue = function(row_id, column_name){
	if(this.getColType(this.getColIndexById(column_name)) == "ch") {
 	    if(this.cells(row_id, this.getColIndexById(column_name)).getValue() == 0) {
			return false;
	    } else {
		   	return true;
	    }
   	} else {
   		return this.cells(row_id, this.getColIndexById(column_name)).getValue();
   	}
}

SepoaDhtmlXGridObject.prototype.setCellValue = function(row_id, column_name, value){
	if(this.getColType(this.getColIndexById(column_name)) == "ch") {
		if(value === true || value == '1'){
			this.cells(row_id, this.getColIndexById(column_name)).setValue('1')
		}else{
			this.cells(row_id, this.getColIndexById(column_name)).setValue('0')
		}
   	} else {
   		return this.cells(row_id, this.getColIndexById(column_name)).setValue(value);
   	}
}


/**
 * @desc: response from server
 * @param: xml - XMLLoader object with response XML
 * @type: private
 */
SepoaDhtmlXGridObject.prototype.afterUpdate=function(that,xml,id){
	// try to use json first
	if (window.JSON){
		try{
			var tag = JSON.parse(xml.xmlDoc.responseText);
			var action = tag.action || this.getState(id) || "updated";
			var sid = tag.sid || id[0];
			var tid = tag.tid || id[0];
			that.afterUpdateCallback(sid, tid, action, tag);
			return;
		} catch(e){
		}
	}
	// xml response
	this._doQueryModalEnd();
	var top = dhx4.ajax.xmltop("data", xml.xmlDoc);
	if (!top){
		top = dhx4.ajax.xmltop("rows", xml.xmlDoc);
		
		var atag = dhx4.ajax.xpath("/rows/userdata", top);
		var btag = {}
		for(var i=0;i<atag.length; i++){
			var a = atag[i];
			btag[a.getAttribute("name")] = (a.textContent || a.text || "").trim() 
		}
		
		if(this.returnObj.status == "SESSIONCLOSE"){
			sUtils.system.login();
			return;
		}
		that.callEvent("onTXE", [btag])
	}else if(top.tagName == "DIV"){
		return this.cleanUpdate(id);
	}else{
		var atag=dhx4.ajax.xpath("//data/action", top);
		if (!atag.length) return this.cleanUpdate(id);
	
		for (var i=0; i<atag.length; i++){
	    	var btag=atag[i];
			var action = btag.getAttribute("type");
			var sid = btag.getAttribute("sid");
			var tid = btag.getAttribute("tid");
			
			that.afterUpdateCallback(sid,tid,action,btag);
		}
	}
}
SepoaDhtmlXGridObject.prototype.cleanUpdate = function(id) {
	Alert("load XML ERROR");
}
/**
 * @desc: used in combination with setOnBeforeUpdateHandler to create custom
 *        client-server transport system
 * @param: sid - id of item before update
 * @param: tid - id of item after up0ate
 * @param: action - action name
 * @type: public
 * @topic: 0
 */
SepoaDhtmlXGridObject.prototype.afterUpdateCallback = function(sid, tid, action, btag) {
	var marker = sid;
	var correct=(action!="error" && action!="invalid");
	if (!correct) this.set_invalid(sid,action);
	this._doSaveEnd(btag); 
}
/* rowSapn 오버라이딩 - 기준 셀 데이터로 나머지 데이터 치환, 인덱스 변환 안함. */
SepoaDhtmlXGridObject.prototype.setRowspan=function(rowID,colInd,length){
	
   var c=this[this._bfs_cells?"_bfs_cells":"cells"](rowID,colInd).cell;    
   var r=this.rowsAr[rowID];

   if (c.rowSpan && c.rowSpan!=1){
		var ur=r.nextSibling;   
		for (var i=1; i<c.rowSpan; i++){
			var tc=ur.childNodes[ur._childIndexes[c._cellIndex+1]]
			var ti=document.createElement("TD"); 
			ti.innerHTML="&nbsp;"; 
			ti._cellIndex=c._cellIndex;
			ti._clearCell=true;
			if (tc)
				tc.parentNode.insertBefore(ti,tc);
			else
				ur.parentNode.appendChild(ti);
			this._shiftIndexes(ur,c._cellIndex,-1);
	    	ur=ur.nextSibling;
	    }
    }

    c.rowSpan=length;
    if (!this._h2)
		r=r.nextSibling||this.rowsCol[this.rowsCol._dhx_find(r)+1];
	else
		r=this.rowsAr[ this._h2.get[r.idd].parent.childs[this._h2.get[r.idd].index+1].id ];
		
	var kids=[];
	for (var i=1; i<length; i++){
	    var ct=null;
		if (this._fake && !this._realfake)
		    ct=this._bfs_cells3(r,colInd).cell;
		else
		    ct=this.cells3(r,colInd).cell;
		
		// this._shiftIndexes(r,c._cellIndex,1);
		if (ct){
			ct.style.display="none";
			ct.title = c.title;
		    ct.innerHTML = c.innerHTML;
		}
    	// ct.parentNode.removeChild(ct);
    	kids.push(r);
    	
    	if (!this._h2)
			r=r.nextSibling||this.rowsCol[this.rowsCol._dhx_find(r)+1];
		else { 
			var r=this._h2.get[r.idd].parent.childs[this._h2.get[r.idd].index+1];
			if (r) r=this.rowsAr[ r.id ];
		}
    }
    
    this.rowsAr[rowID]._rowSpan=this.rowsAr[rowID]._rowSpan||{};
    this.rowsAr[rowID]._rowSpan[colInd]=kids;
    if (this._fake && !this._realfake && colInd<this._fake._cCount) 
        this._fake.setRowspan(rowID,colInd,length)
}
/* colSapn 오버라이딩 - 기준 셀 데이터로 나머지 데이터 치환, 인덱스 변환 안함. */
SepoaDhtmlXGridObject.prototype.setColspan = function(e, x, g) {
    if (!this._ecspn) {
        return
    }
    var a = this.getRowById(e);
    if ((g) && (g > 1)) {
        if (a._childIndexes) {
            var o = a._childIndexes[x]
        } else {
            var o = x;
            a._childIndexes = new Array();
            for (var v = 0; v < a.childNodes.length; v++) {
                a._childIndexes[v] = v
            }
        }
        a.childNodes[o].colSpan = g;
        for (var v = 1; v < g; v++) {
            // a._childIndexes[a.childNodes[o + 1]._cellIndex] = o;
            // a.removeChild(a.childNodes[o + 1])
            this.cells3(a, x + v).setValue(this.cells3(a, x).getValue());
            a.childNodes[o + v].style.display="none";
        }
        var s = a.childNodes[a._childIndexes[x]]._cellIndex;
        for (var v = s * 1 + 1 * g; v < a._childIndexes.length; v++) {
            // a._childIndexes[v] -= (g - 1)
        }
    }
};

/* 엑셀다운로드 오버라이딩 */
SepoaDhtmlXGridObject.prototype.toPDF=function(url,mode,header,footer,rows,target){
	var noExportColumn = ["SELECTED","SELECT","SEL"];
	if(this._excelHiddenColumn){
		noExportColumn = [];
	}
	var save_sel = {
		row: (this.getSelectedRowId()||"").split(this.delim),
		col: this.getSelectedCellIndex()
	};
	if (save_sel.row === null || save_sel.col === -1)
		save_sel = false;
	else {
		if (save_sel.row){
			for (var i=0; i<save_sel.row.length; i++){
				if (save_sel.row[i]){
					var el = this.cells(save_sel.row[i], save_sel.col).cell;
					el.parentNode.className = el.parentNode.className.replace(' rowselected', '');
					el.className = el.className.replace(' cellselected', '');
					save_sel.row[i] = el;
				}
			}
		} else 
			save_sel = false;
	}
	mode = mode || "color";	
	var full_color = mode == "full_color";
	var grid = this;
	grid._asCDATA = true;
	if (typeof(target) === 'undefined')
		this.target = " target=\"_blank\"";
	else
		this.target = target;
		
	eXcell_ch.prototype.getContent = function(){
		return this.getValue();
	};
	eXcell_ra.prototype.getContent = function(){
		return this.getValue();
	};
	function xml_top(profile) {
		var spans = [];
		for (var i=1; i<grid.hdr.rows.length; i++){
			spans[i]=[];
			for (var j=0; j<grid._cCount; j++){
				var cell = grid.hdr.rows[i].childNodes[j];
				if (!spans[i][j])
					spans[i][j]=[0,0];
				if (cell)
					spans[i][cell._cellIndexS]=[cell.colSpan, cell.rowSpan];
			}
		}
		
	    var xml = "<rows profile='"+profile+"'";
	    if (header)
	    	xml+=" header='"+header+"'";
	    if (footer)
	    	xml+=" footer='"+footer+"'";
	    xml+="><head>"+grid._serialiseExportConfig(spans, noExportColumn).replace(/^<head/,"<columns").replace(/head>$/,"columns>");
	    for (var i=2; i < grid.hdr.rows.length; i++) {
                var empty_cols = 0;
                var row = grid.hdr.rows[i];
    	        var cxml="";
	    	for (var j=0; j < grid._cCount; j++) {
	    		if ((grid._srClmn && !grid._srClmn[j]) || (grid._hrrar[j] && ( !grid._fake || j >= grid._fake.hdrLabels.length)) || (j==0 && (noExportColumn.indexOf(grid.getColumnId(j))>=0))) {
	    			empty_cols++;
	    			continue;
    			}
	    		var s = spans[i][j];
	    		var rspan =  (( s[0] && s[0] > 1 ) ? ' colspan="'+s[0]+'" ' : "");
                        if (s[1] && s[1] > 1){
                             rspan+=' rowspan="'+s[1]+'" ';
                             empty_cols = -1;
                        }
                        
                
                var val = "";
                // split mode
                var frow = row;
                if (grid._fake && j < grid._fake._cCount)
                	frow = grid._fake.hdr.rows[i];

                for (var k=0; k<frow.cells.length; k++){
					if (frow.cells[k]._cellIndexS==j) {
						if (frow.cells[k].getElementsByTagName("SELECT").length)
							val="";
						else
							val = _isIE?frow.cells[k].innerText:frow.cells[k].textContent;
							val=val.replace(/[ \t\xA0]+/," ");
						break;
					}
				}
	    		if (!val || val==" ") empty_cols++;
	    		cxml+="<column"+rspan+"><![CDATA["+val+"]]></column>";
	    	};
	    	if (empty_cols != grid._cCount)
	    		xml+="\n<columns>"+cxml+"</columns>";
	    };
	    xml+="</head>\n";
	    // xml+=xml_footer();
	    return xml;
	};
	
	function xml_body() {
		var xml =[];
	    if (rows)
	    	for (var i=0; i<rows.length; i++)
	    		xml.push(xml_row(grid.getRowIndex(rows[i])));
	    else
	    	for (var i=0; i<grid.getRowsNum(); i++)
	    		xml.push(xml_row(i));
	    return xml.join("\n");
	}
	function xml_footer() {
		var xml =["<foot>"];
		if (!grid.ftr) return "";
		for (var i=1; i < grid.ftr.rows.length; i++) {
			xml.push("<columns>");
			var row = grid.ftr.rows[i];
			for (var j=0; j < grid._cCount; j++){
				if (grid._srClmn && !grid._srClmn[j]) continue;
				if (grid._hrrar[j] && ( !grid._fake || j >= grid._fake.hdrLabels.length)) continue;
				for (var k=0; k<row.cells.length; k++){
				 	var val = "";
				 	var span = "";
					if (row.cells[k]._cellIndexS==j) {
						val = _isIE?row.cells[k].innerText:row.cells[k].textContent;
						val=val.replace(/[ \n\r\t\xA0]+/," ");
						
						if (row.cells[k].colSpan && row.cells[k].colSpan!=1)
							span = " colspan='"+row.cells[k].colSpan+"' ";
						
						if (row.cells[k].rowSpan && row.cells[k].rowSpan!=1)
							span = " rowspan='"+row.cells[k].rowSpan+"' ";
						break;
					}
				}
				xml.push("<column"+span+"><![CDATA["+val+"]]></column>");
			}
			xml.push("</columns>");
		};
		xml.push("</foot>");
	    return xml.join("\n");
	};
	function get_style(node, style){
		return (window.getComputedStyle?(window.getComputedStyle(node, null)[style]):(node.currentStyle?node.currentStyle[style]:null))||"";
	};
	
	function xml_row(ind){
		if (!grid.rowsBuffer[ind]) return "";
		var r = grid.render_row(ind);
		if (r.style.display=="none") return "";
		var level = grid.isTreeGrid() ? ' level="' + grid.getLevel(r.idd) + '"' : '';
		var xml = "<row" + level + ">";
		for (var i=0; i < grid._cCount; i++) {
			if (((!grid._srClmn)||(grid._srClmn[i]))&&(!grid._hrrar[i] || ( grid._fake && i < grid._fake.hdrLabels.length))){
				if((i==0 && (noExportColumn.indexOf(grid.getColumnId(i)) >=0))) continue;
				var cell = grid.cells(r.idd, i);
				if (full_color){
					var text_color	= get_style(cell.cell,"color");
		        	var bg_color	= get_style(cell.cell,"backgroundColor");
					var bold		= get_style(cell.cell, "font-weight") || get_style(cell.cell, "fontWeight");
					var italic		= get_style(cell.cell, "font-style") || get_style(cell.cell, "fontStyle");
					var align		= get_style(cell.cell, "text-align") || get_style(cell.cell, "textAlign");
					var font	 = get_style(cell.cell, "font-family") || get_style(cell.cell, "fontFamily");
		        	if (bg_color == "transparent" || bg_color == "rgba(0, 0, 0, 0)") bg_color = "rgb(255,255,255)";
		        	xml+="<cell bgColor='"+bg_color+"' textColor='" + text_color + "' bold='" + bold + "' italic='" + italic + "' align='"+align+"' font='" + font + "' rowSpan='"+cell.cell.rowSpan+"' colSpan='"+cell.cell.colSpan+"'>";
				} else 
					xml+="<cell rowSpan='"+cell.cell.rowSpan+"' colSpan='"+cell.cell.colSpan+"'>";
				
				var cv = ((["co","coro","coro2","combo","comboro","comboro2","icon"].indexOf(grid.cellType[i])+1)?cell.getTitle():cell.getValue());
				if(cv == undefined){
					cv = '';
				}
				xml+="<![CDATA["+cv+"]]></cell>";
			}
		};
		return xml+"</row>";
	}
	function xml_end(){
	    var xml = "</rows>";
	    return xml;
	}


			
	var d=document.createElement("div");
	d.style.display="none";
	document.body.appendChild(d);
	var uid = "form_"+grid.uid();

	d.innerHTML = '<form id="'+uid+'" method="post" action="'+url+'" accept-charset="utf-8"  enctype="application/x-www-form-urlencoded"' + this.target + '><input type="hidden" name="grid_xml" id="grid_xml"/> </form>';
	// console.log(xml_top(mode).replace("–", "-") + xml_body() + xml_end());
	var t_xml = xml_top(mode).replace("–", "-") + xml_body() + xml_end();
	t_xml = t_xml.replaceAll("&nbsp;", " ");
	t_xml = t_xml.replace(/<[\w]?br[\w/]?>/ig,"\n");
	t_xml = t_xml.replace(/<[\w/]?span.*?>/ig,"");
	t_xml = t_xml.replace(/<[\w/]?span.*?>/ig,"");
	// t_xml = t_xml.replace(/[%]{1}/ig,"");
	window.test = t_xml;
	document.getElementById(uid).firstChild.value = encodeURIComponent(t_xml);
	document.getElementById(uid).submit();
	d.parentNode.removeChild(d);
	
	grid = null;
	
	if (save_sel && save_sel.row.length) {
		for (var i = 0; i < save_sel.row.length; i++) {
			save_sel.row[i].parentNode.className += ' rowselected';
			if (save_sel.row.length == 1)
				save_sel.row[i].className += ' cellselected';
		}
	};
	save_sel = null;
};
SepoaDhtmlXGridObject.prototype._serialiseExportConfig=function(spans, noExportColumn){
	function xmlentities(str) {
		if (typeof(str)!=='string') return str;
		str = str.replace(/&/g, "&amp;");
		str = str.replace(/"/g, "&quot;");
		str = str.replace(/'/g, "&apos;");
		str = str.replace(/</g, "&lt;");
		str = str.replace(/>/g, "&gt;");
		return str;
	}
	
	var out = "<head>";

	for (var i = 0; i < this.hdr.rows[0].cells.length; i++){
		if (this._srClmn && !this._srClmn[i]) continue;
		if (this._hrrar[i] && ( !this._fake || i >= this._fake.hdrLabels.length)) continue;
		if ((i==0 && (noExportColumn.indexOf(this.getColumnId(i))>=0))) continue;
		var sort = this.fldSort[i];
		if (sort == "cus"){
			sort = this._customSorts[i].toString();
			sort=sort.replace(/function[\ ]*/,"").replace(/\([^\f]*/,"");
		}
		var s = spans[1][i];
		var width = this.getColWidth(i);
		var rpans = (( s[1] && s[1] > 1 ) ? ' rowspan="'+s[1]+'" ' : "")+(( s[0] && s[0] > 1 ) ? ' colspan="'+s[0]+'" ' : "");
		out+="<column "+rpans+" width='"+(isNaN(width)?100:width)+"' align='"+this.cellAlign[i]+"' type='"+((["coro","coro2","co","combo","comboro","comboro2"].indexOf(this.cellType[i])+1)?"ro":this.cellType[i]) + "' hidden='" + ((this.isColumnHidden && this.isColumnHidden(i)) ? 'true' : 'false')
			+"' sort='"+(sort||"na")+"' color='"+(this.columnColor[i]||"")+"'"
			+(this.columnIds[i]
				? (" id='"+this.columnIds[i]+"'")
				: "")+">";
		if (this._asCDATA)
			out+="<![CDATA["+this.getColumnLabel(i)+"]]>";
		else
			out+=this.getColumnLabel(i);
		var z = false;
		if (z)
			for (var j = 0; j < z.keys.length; j++)out+="<option value='"+xmlentities(z.keys[j])+"'><![CDATA["+z.values[j]+"]]></option>";
		out+="</column>";
	}
	return out+="</head>";
};
SepoaDhtmlXGridObject.prototype._handlePaste = function(e){
	var clipboardData, text;
    
    e.stopPropagation();
    e.preventDefault();
    
    clipboardData = e.clipboardData || window.clipboardData;
    text = clipboardData.getData('Text');

    if(confirm("붙여넣기를 하시겠습니까?")){
    	return;
    }
    
	if(typeof(this.pasteCallBack) != 'undefined' && this.pateCallBack != null){
		this.pateCallBack(text);
	}
};
SepoaDhtmlXGridObject.prototype.setPaste = function(f){
	this.pasteCallBack = f;
}
SepoaDhtmlXGridObject.prototype.serializeToTextArea = function(){
	var noExportColumn = ["SELECTED","SELECT","SEL"];
	var grid = this;
	function row(ind){
		if (!grid.rowsBuffer[ind]) return "";
		var r = grid.render_row(ind);
		if (r.style.display=="none") return "";
		var level = grid.isTreeGrid() ? ' level="' + grid.getLevel(r.idd) + '"' : '';
		var xml = [];
		for (var i=0; i < grid._cCount; i++) {
			if (((!grid._srClmn)||(grid._srClmn[i]))&&(!grid._hrrar[i] || ( grid._fake && i < grid._fake.hdrLabels.length))){
				if((i==0 && (noExportColumn.indexOf(grid.getColumnId(i)) >=0))) continue;
				var cell = grid.cells(r.idd, i);
				xml.push(cell.getContent?cell.getContent():cell.getValue());
			}
		};
		return xml.join("\t");
	}
	var gridArray = [];
	for (var i=0; i<grid.getRowsNum(); i++){
		gridArray.push(row(i));
	}
	return gridArray.join("\n");
}
SepoaDhtmlXGridObject.prototype._expandMonolite = function(a, u, l) {
    var e = this.parentNode;
    var v = e.parentNode;
    var o = v.grid;
    if (a || window.event) {
        if (!l && !v._expanded) {
            o.editStop()
        }(a || event).cancelBubble = true
    }
    var s = o.getUserData(v.idd, "__sub_row");
    if (!o._sub_row_editor) {
        o._sub_row_editor = new eXcell_sub_row(e)
    }
// if (!s) {
// return
// }
    if (v._expanded && v._expanded.style.display != "none" && !u) {
        o._sub_row_editor._setState("plus.gif", e);
        e._previous_content = v._expanded;
        // o.objBox.removeChild(v._expanded);
        // v._expanded = false;
        v._expanded.style.display="none";
        v.style.height = (v.oldHeight || 20) + "px";
        e.style.height = (v.oldHeight || 20) + "px";
        if (o._fake) {
            o._fake.rowsAr[v.idd].style.height = (v.oldHeight || 20) + "px";
            o._fake.rowsAr[v.idd].firstChild.style.height = (v.oldHeight || 20) + "px"
        }
        for (var h = 0; h < v.cells.length; h++) {
            v.cells[h].style.verticalAlign = "middle"
        }
        delete o._flow[v.idd];
        o._correctMonolite();
        v._expanded.ctrl = null
    } else {
        if ((!v._expanded || v._expanded.style.display == "none") && !l) {
            o._sub_row_editor._setState("minus.gif", e);
            v.oldHeight = e.offsetHeight - 4;
            if (e._previous_content) {
                var r = e._previous_content;
                r.ctrl = e;
                // o.objBox.appendChild(r);
                v._expanded.style.display="block";
                o._detectHeight(r, e, parseInt(r.style.height))
            } else {
                var r = document.createElement("DIV");
                r.ctrl = e;
                if (e._sub_row_type) {
                    o._sub_row_render[e._sub_row_type](o, r, e, s)
                } else {
                    r.innerHTML = s
                }
                r.style.cssText = "position:absolute; left:0px; top:0px; overflow:auto; font-family:Tahoma; font-size:8pt; margin-top:2px; margin-left:4px;";
                r.className = "dhx_sub_row";
                o.objBox.appendChild(r);
                o._detectHeight(r, e)
            }
            if (!o._flow) {
                o.attachEvent("onGridReconstructed", function() {
                    if ((this.pagingOn && !this.parentGrid) || this._srnd) {
                        this._collapsMonolite()
                    } else {
                        this._correctMonolite()
                    }
                });
                o.attachEvent("onResizeEnd", function() {
                    this._correctMonolite(true)
                });
                o.attachEvent("onAfterCMove", function() {
                    this._correctMonolite(true)
                });
                o.attachEvent("onDrop", function() {
                    this._correctMonolite(true)
                });
                o.attachEvent("onBeforePageChanged", function() {
                    this._collapsMonolite();
                    return true
                });
                o.attachEvent("onGroupStateChanged", function() {
                    this._correctMonolite();
                    return true
                });
                o.attachEvent("onFilterEnd", function() {
                    this._collapsMonolite()
                });
                o.attachEvent("onUnGroup", function() {
                    this._collapsMonolite()
                });
                o.attachEvent("onPageChanged", function() {
                    this._collapsMonolite()
                });
                o.attachEvent("onXLE", function() {
                    this._collapsMonolite()
                });
                o.attachEvent("onClearAll", function() {
                    for (var c in this._flow) {
                        if (this._flow[c] && this._flow[c].parentNode) {
                            this._flow[c].parentNode.removeChild(this._flow[c])
                        }
                    }
                    this._flow = []
                });
                o.attachEvent("onEditCell", function(w, n, x) {
                    if ((w !== 2) && this._flow[n] && this.cellType[x] != "ch" && this.cellType[x] != "ra") {
                        this._expandMonolite.apply(this._flow[n].ctrl.firstChild, [0, false, true])
                    }
                    return true
                });
                o.attachEvent("onCellChanged", function(x, n) {
                    if (!this._flow[x]) {
                        return
                    }
                    var w = this.cells(x, n).cell;
                    w.style.verticalAlign = "top"
                });
                o._flow = []
            }
            o._flow[v.idd] = r;
            o._correctMonolite();
            var m = o._srdh > 30 ? 11 : 3;
            if (o.multiLine) {
                m = 0
            }
            for (var h = 0; h < v.cells.length; h++) {
                v.cells[h].style.verticalAlign = "top"
            }
            if (o._fake) {
                var g = o._fake.rowsAr[v.idd];
                for (var h = 0; h < g.cells.length; h++) {
                    g.cells[h].style.verticalAlign = "top"
                }
            }
            v._expanded = r
        }
    }
    if (o._ahgr) {
        o.setSizes()
    }
    if (o.parentGrid) {
        o.callEvent("onGridReconstructed", [])
    }
    o.callEvent("onSubRowOpen", [v.idd, (!!v._expanded && v._expanded.style.display != 'none')])
};
SepoaDhtmlXGridObject.prototype.reloadComboBox = function(id, params, default_value){
	this._loadCombo(params[0], window.dhx4.comboParam((typeof(params[1])=='undefined' || params[1]==null)?'':params[1]), id, default_value);
}
SepoaDhtmlXGridObject.prototype._loadCombo = function(code, param, selectbox_name, default_value, blank_line_flag, combobox) {
	this._ajaxStatusW.combo++;
	
	setTimeout(function(code, param, selectbox_name, default_value, blank_line_flag, combobox){
		var combobox = this.getCombo(selectbox_name);
		var dhtmlxcombo = this._col_combos[this.getColIndexById(selectbox_name)];
		
		combobox.clear();
		dhtmlxcombo.clearAll();
		
		combobox.put("", "");
		dhtmlxcombo.addOption("","");
		
		var data = Sepoa.cache.combo.get(code+","+param);
		
		for(var idx in data) {
			if(data[idx].text != ""){
				combobox.put(data[idx].value, data[idx].text);
				dhtmlxcombo.addOption(data[idx].value, data[idx].text);
			}
		}
		
		this.callEvent('onComboLoaded', ['CUSTOM']);
		
		if(this._checkAjaxStatus('combo')){
			this.callEvent('alreadyCallConnection',[]);
		}
	}.bind(this), 0, code, param, selectbox_name, default_value, blank_line_flag, combobox);
}
SepoaDhtmlXGridObject.prototype.excelHiddenColumn=function(mode){
	this._excelHiddenColumn = mode;
}
SepoaDhtmlXGridObject.prototype._checkAjaxStatus=function(type){
	if(type && this._ajaxStatusW[type]){
		this._ajaxStatusW[type] -= 1;
	}
	var flag = true;
	for(var _idx in this._ajaxStatusW){
		if(this._ajaxStatusW[_idx] != 0){
			flag = false;
			break;
		}
	}
	return flag;
}

SepoaDhtmlXGridObject.prototype.setPasteAllRow=function(cId, v){
	var cInd = this.getColIndexById(cId);
	this.editStop();
	this.forEachRow(function(id){
		this.cells(id, cInd).setValue(v);
	});
}

SepoaDhtmlXGridObject.prototype.setPasteBelowRow=function(rId, cId, v){
	var grid_array = this.getAllRowIds().split(',');
	var cInd = this.getColIndexById(cId);
	this.editStop();
	for(var i=0;i<grid_array.length;i++){
		if(i>=grid_array.indexOf(rId.toString())){
			this.cells(grid_array[i], cInd).setValue(v);
		}
	}
}

/**
 * 그리드 row span
 * 
 * @param obj
 *            그리드 오브젝트
 * @param keyId
 *            기준 컬럼 ID
 * @param colIds
 *            span처리될 컬럼 ID들"~~,~~"또는 array 형태 keyId == colIds 의경우 생략가능(null)
 * @param colorFlag
 *            격자형태의 색상
 */
SepoaDhtmlXGridObject.prototype.setRowMerge = function(keyId, colIds, colorName, otherCellFlag){
	if(this.getRowsNum() < 1){
		return;
	}
	
	colIds = colIds || [keyId];
	otherCellFlag = otherCellFlag || false;
	var patternFlag = typeof colorName !== 'undefined' ? true : false;
	
	if(!(colIds instanceof(Array))){
		colIds = colIds.split(",");
	}
	
	var startingPoint = 0;
	var beforeData = this.cells(this.getRowId(0), this.getColIndexById(keyId)).getValue();
	var colorCheck = true;
	
	for(var rowIndex=0;rowIndex<this.getRowsNum();rowIndex++){
		var keyData = this.cells(this.getRowId(rowIndex), this.getColIndexById(keyId)).getValue();
		
		if(keyData != beforeData || rowIndex == this.getRowsNum()-1){
			var margeCNT = (keyData == beforeData)? rowIndex - startingPoint + 1 : rowIndex - startingPoint;
			for(var colIdx in colIds){
				this.setRowspan(this.getRowId(startingPoint),this.getColIndexById(colIds[colIdx]),margeCNT);
				if(patternFlag){
					this.cells(this.getRowId(startingPoint),this.getColIndexById(colIds[colIdx])).setBgColor(colorName[(colorCheck)?0:1]);
				}
			}
			if(patternFlag && otherCellFlag){
				this.setOtherCellColor(this, startingPoint, margeCNT,this.getColIndexById(colIds[colIds.length-1]),colorName[(colorCheck)?0:1]);
			}
			colorCheck = !colorCheck;
			beforeData = keyData;
			startingPoint = rowIndex;
		}
	}
}

SepoaDhtmlXGridObject.prototype.setOtherCellColor = function(rowIdx, length,colIdx,color){
	for(var i = rowIdx;i <rowIdx+length; i++){
		var rId = this.getRowId(i);
		for(var j=colIdx;j<this.getColumnsNum();j++){
			this.cells(rId, j).setBgColor(color);
		}
	}
}


SepoaDhtmlXGridObject.prototype._parseHead = function(c, w, r) {
    this._parseHead_connector.apply(this, arguments);
    if (!this._colls_loaded) {
        var s = dhx4.ajax.xpath("./coll_options", arguments[0]);
        for (var m = 0; m < s.length; m++) {
            var o = s[m].getAttribute("for");
            var u = [];
            var g = null;
            if (this.cellType[o].indexOf("combo") == 0) {
                g = this.getColumnCombo(o)
            } else {
                if (this.cellType[o].indexOf("co") == 0) {
                    g = this.getCombo(o)
                }
            }
            var l = dhx4.ajax.xpath("./item", s[m]);
            var a = [];
            for (var h = 0; h < l.length; h++) {
                var e = l[h].getAttribute("value");
                if (g) {
                    var n = l[h].getAttribute("label") || e;
                    if (g.addOption) {
                        a.push([e, n])
                    } else {
                        g.put(e, n)
                    }
                    u[u.length] = n
                } else {
                    u[u.length] = e
                }
            }
            if (a.length) {
                if (g) {
                    g.addOption(a)
                }
            } else {
                if (u.length && !g) {
                    if (this.registerCList) {
                        this.registerCList(o * 1, u)
                    }
                }
            }
            if (this._con_f_used[o * 1]) {
                this._con_f_used[o * 1] = u
            }
        }
        this._colls_loaded = true
    }
}
// eXcell Type
function eXcell_dynedn(cell){
	this.base = eXcell_edn;
	this.base(cell)
	this.setValue = function(val){
		if(this.grid.ednNullEnable && (val == null || val == "")){
			this.setCValue("","");
			return;
		}
		
		if(!val || val.toString()._dhx_trim()=="")
			val="0"
		var flag = false;		

        var colIdx = this.grid.getColIndexById("UNIT_MEASURE");				
        if(colIdx) {
        	var rowId = this.cell.id.split("_")[1];
            flag = (GridObj.cells(rowId, colIdx).getValue() == "EA");
        }
       
        if(flag) {
// this._mask = this.grid.dyn_mask;
        	this.cell._mask = this.grid.dyn_mask;
		} else {
// this._mask = this.grid._maskArr[this.cell._cellIndex];
        	this.cell._mask = this.grid._maskArr[this.cell._cellIndex];
		}

		this.cell.innerHTML = this.grid._aplNF(val,this.cell._cellIndex,this.cell._mask);
	};

	this.getValue=function(){
		// this.grid.editStop();
		if ((this.cell.firstChild)&&(this.cell.firstChild.tagName == "TEXTAREA"))
			return this.cell.firstChild.value;

		if (this.cell._clearCell)
			return "";

		return this.cell._orig_value||this.grid._aplNFb(this.cell.innerHTML.toString()._dhx_trim(), this.cell._cellIndex, this.cell._mask);
	}
}
eXcell_dynedn.prototype = new eXcell_edn;
function eXcell_ro(a) {
    if (a) {
        this.cell = a;
        this.grid = this.cell.parentNode.grid
    }
    this.edit = function() {};
    this.isDisabled = function() {
        return true
    };
    this.getValue = function() {
        return this.cell._clearCell ? "" : htmlDecode(this.cell.innerHTML.toString()._dhx_trim());
    }
}
eXcell_ro.prototype = new eXcell;
eXcell_ron.prototype.setValue = function(a) {
	if(this.grid.ednNullEnable && (a == null || a == "")){
		this.setCValue("","");
		return;
	}
    if (!a || a.toString()._dhx_trim() == "") {
        this.cell._clearCell = false;
        this.cell._orig_value = "0";
        return this.setCValue(0, 0);
    } else {
        this.cell._clearCell = false;
        a = parseFloat(a.toString()._dhx_trim());
        a = (a+"" == "NaN")?"0":a;
        this.cell._orig_value = a;
    }
    this.setCValue(this.grid._aplNF(a, this.cell._cellIndex), parseFloat(a))
};

function eXcell_dynron(cell){
	this.base = eXcell_ron;
	this.base(cell)
	this.setValue = function(val){
		if(this.grid.ednNullEnable && (val == null || val == "")){
			this.setCValue("","");
			return;
		}
			if(!val || val.toString()._dhx_trim()=="")
				val="0"
			var flag = false;		

            var colIdx = this.grid.getColIndexById("UNIT_MEASURE");				
            if(colIdx) {
            	var rowId = this.cell.id.split("_")[1];
                flag = (GridObj.cells(rowId, colIdx).getValue() == "EA");
            }
           
            if(flag) {
// this._mask = this.grid.dyn_mask;
            	this.cell._mask = this.grid.dyn_mask;
			} else {
// this._mask = this.grid._maskArr[this.cell._cellIndex];
            	this.cell._mask = this.grid._maskArr[this.cell._cellIndex];
			}

			this.cell.innerHTML = this.grid._aplNF(val,this.cell._cellIndex,this.cell._mask);
	};

	this.getValue=function(){
		// this.grid.editStop();
		if ((this.cell.firstChild)&&(this.cell.firstChild.tagName == "TEXTAREA"))
			return this.cell.firstChild.value;

		if (this.cell._clearCell)
			return "";

		return this.cell._orig_value||this.grid._aplNFb(this.cell.innerHTML.toString()._dhx_trim(), this.cell._cellIndex, this.cell._mask);
	}
}
eXcell_dynron.prototype = new eXcell;
eXcell_dhxCalendar.prototype.edit = function() {
    var c = this.grid.getPosition(this.cell);
    //var p = Sepoa.request.isPopup && !Sepoa.request.isRealPopup;
    var abs = sUtils.element.getOffset(this.cell, "module.over");
    c[0] = abs.left + this.cell.offsetWidth;
    c[1] = abs.top;
    if(c[0] + 251 > SUX.root.innerWidth){
    	c[0] = c[0] - 251 - this.cell.offsetWidth;
    }
    if(c[1] + 296 > SUX.root.innerHeight){
    	c[1] = c[1] + (SUX.root.innerHeight - (c[1] + 296));
    }
    this.grid._grid_calendarA._show(false, false);
    this.grid._grid_calendarA.setPosition(c[0], c[1]);
    this.grid._grid_calendarA._last_operation_calendar = false;
    this.grid.callEvent("onCalendarShow", [this.grid._grid_calendarA, this.cell.parentNode.idd, this.cell._cellIndex]);
    this.cell._cediton = true;
    this.val = this.cell.val;
    this._val = this.cell.innerHTML;
    var a = this.grid._grid_calendarA.draw;
    this.grid._grid_calendarA.draw = function() {};
    this.grid._grid_calendarA.setDateFormat((this.grid._dtmask || Sepoa.info.DATE_FORMAT || "%Y/%m/%d"));
    this.grid._grid_calendarA.setDate(this.val || (new Date()));
    this.grid._grid_calendarA.draw = a
};
eXcell_dhxCalendar.prototype.detach = function() {
    if (!this.grid._grid_calendarA) {
        return
    }
    this.grid._grid_calendarA.hide();
    if (this.cell._cediton) {
        this.cell._cediton = false
    } else {
        return
    }
    if (this.grid._grid_calendarA._last_operation_calendar) {
        var e = this.grid._grid_calendarA.getFormatedDate((this.grid._dtmask || Sepoa.info.DATE_FORMAT || "%Y/%m/%d"));
        var c = this.grid._grid_calendarA.getDate();
        this.cell.val = new Date(c);
        this.setCValue(e, c);
        this.cell._clearCell = !e;
        var a = this.val;
        this.val = this._val;
        return (this.cell.val.valueOf() != (a || "").valueOf())
    }
    return false
};
eXcell_dhxCalendar.prototype.setValue = function(a) {
	if (a && typeof a == "object") {
        this.cell.val = a;
        this.cell._clearCell = false;
        this.setCValue(this.grid._grid_calendarA.getFormatedDate((this.grid._dtmask || Sepoa.info.DATE_FORMAT || "%Y/%m/%d"), a).toString(), this.cell.val);
        return
    }
    if (!a || a.toString()._dhx_trim() == "") {
        a = "&nbsp";
        this.cell._clearCell = true;
        this.cell.val = ""
        this.setCValue((a || "").toString(), this.cell.val);
    } else {
        this.cell._clearCell = false;
        var unFormatType = ["%Y%m%d", this.grid._dtmask_inc, this.grid._dtmask, Sepoa.info.DATE_FORMAT, "%Y/%m/%d"];
        this.cell.val = "";
        for(var idx in unFormatType){
        	var paramDate = new Date(this.grid._grid_calendarA.setFormatedDate((unFormatType[idx]), a.toString(), null, true));
        	if ((paramDate != "NaN") && (paramDate != "Invalid Date")) {
        		this.cell.val = paramDate;
        		break;
        	}
        }
        
        if ((this.cell.val == "NaN") || (this.cell.val == "Invalid Date") || (this.cell.val == "")) {
            this.cell._clearCell = true;
            this.cell.val = ""; 
            this.setCValue("&nbsp;", 0);
        } else {
        	this.cell._clearCell = false;
        	
        	a = this.grid._grid_calendarA.getFormatedDate((this.grid._dtmask_inc || this.grid._dtmask || Sepoa.info.DATE_FORMAT),this.cell.val);
            this.setCValue((a || "").toString(), this.cell.val)
        }
    }
};
eXcell_dhxCalendar.prototype.getValue = function() {
    if (this.cell._clearCell) {
        return ""
    }
    
    const calendar = this.grid._grid_calendarA || this.grid._grid_calendar;
    
    if (this.grid._dtmask_inc && this.cell.val) {
        return calendar.getFormatedDate(this.grid._dtmask_inc, this.cell.val).toString()
    }
    
    return calendar.getFormatedDate("%Y%m%d", this.getDate());
};
eXcell_dhxCalendarA.prototype.edit = function() {
    var c = this.grid.getPosition(this.cell);
    var abs = sUtils.element.getOffset(this.cell, "module.over");
    c[0] = abs.left + this.cell.offsetWidth;
    c[1] = abs.top;
    if(c[0] + 251 > SUX.root.innerWidth){
    	c[0] = c[0] - 251 - this.cell.offsetWidth;
    }
    if(c[1] + 296 > SUX.root.innerHeight){
    	c[1] = c[1] + (SUX.root.innerHeight - (c[1] + 296));
    }
    this.grid._grid_calendarA._show(false, false);
    this.grid._grid_calendarA.setPosition(c[0], c[1]);
    this.grid.callEvent("onCalendarShow", [this.grid._grid_calendarA, this.cell.parentNode.idd, this.cell._cellIndex]);
    this.grid._grid_calendarA._last_operation_calendar = false;
    this.cell._cediton = true;
    this.val = this.cell.val;
    this._val = this.cell.innerHTML;
    var a = this.grid._grid_calendarA.draw;
    this.grid._grid_calendarA.draw = function() {};
    this.grid._grid_calendarA.setDateFormat((this.grid._dtmask || "%Y/%m/%d"));
    this.grid._grid_calendarA.setDate(this.val || (new Date()));
    this.grid._grid_calendarA.draw = a;
    this.cell.atag = ((!this.grid.multiLine) && (_isKHTML || _isMacOS || _isFF)) ? "INPUT" : "TEXTAREA";
    this.obj = document.createElement(this.cell.atag);
    this.obj.style.height = (this.cell.offsetHeight - 10) + "px";
    this.obj.className = "dhx_combo_edit";
    this.obj.wrap = "soft";
    this.obj.style.textAlign = this.cell.align;
    this.obj.onclick = function(g) {
        (g || event).cancelBubble = true
    };
    this.obj.onmousedown = function(g) {
        (g || event).cancelBubble = true
    };
    this.obj.value = this.getValue();
    this.cell.innerHTML = "";
    this.cell.appendChild(this.obj);
    if (window.dhx4.isIE) {
        this.obj.style.overflow = "visible";
        if ((this.grid.multiLine) && (this.obj.offsetHeight >= 18) && (this.obj.offsetHeight < 40)) {
            this.obj.style.height = "36px";
            this.obj.style.overflow = "scroll"
        }
    }
    this.obj.onselectstart = function(g) {
        if (!g) {
            g = event
        }
        g.cancelBubble = true;
        return true
    };
    this.obj.focus();
    this.obj.focus()
};
eXcell_dhxCalendarA.prototype.setValue = function(a) {
    if (a && typeof a == "object") {
        this.cell.val = a;
        this.cell._clearCell = false;
        this.setCValue(this.grid._grid_calendarA.getFormatedDate((this.grid._dtmask || Sepoa.info.DATE_FORMAT || "%Y/%m/%d"), a).toString(), this.cell.val);
        return
    }
    if (!a || a.toString()._dhx_trim() == "") {
        a = "&nbsp";
        this.cell._clearCell = true;
        this.cell.val = ""
        this.setCValue((a || "").toString(), this.cell.val);
    } else {
        this.cell._clearCell = false;
        var unFormatType = ["%Y%m%d", this.grid._dtmask_inc, this.grid._dtmask, Sepoa.info.DATE_FORMAT, "%Y/%m/%d"];
        this.cell.val = "";
        for(var idx in unFormatType){
        	var paramDate = new Date(this.grid._grid_calendarA.setFormatedDate((unFormatType[idx]), a.toString(), null, true));
        	if ((paramDate != "NaN") && (paramDate != "Invalid Date")) {
        		this.cell.val = paramDate;
        		break;
        	}
        }
        
        if ((this.cell.val == "NaN") || (this.cell.val == "Invalid Date") || (this.cell.val == "")) {
            this.cell._clearCell = true;
            this.cell.val = ""; 
            this.setCValue("&nbsp;", 0);
        } else {
        	this.cell._clearCell = false;
        	
        	a = this.grid._grid_calendarA.getFormatedDate((this.grid._dtmask_inc || this.grid._dtmask || Sepoa.info.DATE_FORMAT),this.cell.val);
            this.setCValue((a || "").toString(), this.cell.val)
        }
    }
};
eXcell_dhxCalendarA.prototype.getValue = function() {
    if (this.cell._clearCell) {
        return ""
    }
    if (this.grid._dtmask_inc && this.cell.val) {
        return this.grid._grid_calendarA.getFormatedDate(this.grid._dtmask_inc, this.cell.val).toString()
    }
    
    return this.grid._grid_calendarA.getFormatedDate("%Y%m%d", this.getDate());
};
eXcell_dhxCalendarA.prototype.detach = function() {
    if (!this.grid._grid_calendarA) {
        return
    }
    this.grid._grid_calendarA.hide();
    if (this.cell._cediton) {
        this.cell._cediton = false
    } else {
        return
    }
    if (this.grid._grid_calendarA._last_operation_calendar) {
        this.grid._grid_calendarA._last_operation_calendar = false;
        var e = this.grid._grid_calendarA.getFormatedDate(this.grid._dtmask || Sepoa.info.DATE_FORMAT || "%Y/%m/%d");
        var c = this.grid._grid_calendarA.getDate();
        this.cell.val = new Date(c);
        this.setCValue(e, c);
        this.cell._clearCell = !e;
        var a = this.val;
        this.val = this._val;
        return (this.cell.val.valueOf() != (a || "").valueOf())
    }
    this.setValue(this.obj.value);
    var a = this.val;
    this.val = this._val;
    return (this.cell.val.valueOf() != (a || "").valueOf())
};
eXcell_dhxCalendarR = function(a){
	if (a) {
        this.cell = a;
        this.grid = this.cell.parentNode.grid;
        if (!this.grid._grid_calendarA) {
            var e = this.grid._grid_calendarA = new dhtmlxCalendarObject();
            this.grid.callEvent("onDhxCalendarCreated", [e]);
            var c = this.grid;
            e.attachEvent("onClick", function() {
                this._last_operation_calendar = true;
                window.setTimeout(function() {
                    c.editStop()
                }, 1);
                return true
            });
            var g = function(h) {
                (h || event).cancelBubble = true
            };
            dhtmlxEvent(e.base, "click", g)
        }
    }
}
eXcell_dhxCalendarR.prototype = new eXcell_dhxCalendar;
eXcell_dhxCalendarR.prototype.edit = function(){};
eXcell_dhxCalendarR.prototype.isDisabled = function() {
    return true
};
eXcell_dhxCalendarYM = function(a){
	if (a) {
        this.cell = a;
        this.grid = this.cell.parentNode.grid;
        if (!this.grid._grid_calendarYMA) {
            var e = this.grid._grid_calendarYMA = new SepoaDhtmlXCalendarYM();
            this.grid.callEvent("onDhxCalendarCreated", [e]);
            var c = this.grid;
            e.attachEvent("onChange", function() {
                this._last_operation_calendar = true;
                return true
            });
            e.attachEvent("onMonthSelect", function() {
                this._last_operation_calendar = true;
                c.editStop();
                return true
            });
            var g = function(h) {
                (h || event).cancelBubble = true
            };
            dhtmlxEvent(e.base, "click", g);
            e = null
        }
        if(this.grid._grid_calendarYMA.ACEventCode){
        	this.grid._grid_calendarYMA.detachEvent(this.grid._grid_calendarYMA.ACEventCode)
        	this.grid._grid_calendarYMA.detachEvent(this.grid._grid_calendarYMA.BCEventCode)
        }
        this.grid._grid_calendarYMA.ACEventCode = this.grid._grid_calendarYMA.attachEvent("onArrowClick", dhtmlx.bind(this.onChangeFunction, this))
        this.grid._grid_calendarYMA.BCEventCode = this.grid._grid_calendarYMA.attachEvent("onButtonClick", dhtmlx.bind(
    		function(){
    			this.onChangeFunction();
    			this.grid.editStop();
    		}, this)
    	)
        
    }
}
eXcell_dhxCalendarYM.prototype = new eXcell_dhxCalendar;
eXcell_dhxCalendarYM.prototype.onChangeFunction=function(){
	var e = this.grid._grid_calendarYMA.getFormatedDate("%Y/%m");
    var d = this.grid._grid_calendarYMA.getDate();
    this.cell.val = new Date(d);
    this.setCValue(e, d);
    this.cell._clearCell = !e;
    var a = this.val;
    this.val = this._val;
    return (this.cell.val.valueOf() != (a || "").valueOf())
}
eXcell_dhxCalendarYM.prototype.edit = function() {
    var c = this.grid.getPosition(this.cell);
    var abs = sUtils.element.getOffset(this.cell, "module.over");
    c[0] = abs.left + this.cell.offsetWidth;
    c[1] = abs.top;
    if(c[0] + 211 > SUX.root.innerWidth){
    	c[0] = c[0] - 211 - this.cell.offsetWidth;
    }
    if(c[1] + 31 > SUX.root.innerHeight){
    	c[1] = c[1] + (SUX.root.innerHeight - (c[1] + 31));
    }
    this.grid._grid_calendarYMA._show(false, false);
    this.grid._grid_calendarYMA.setPosition(c[0], c[1]);
    this.grid._grid_calendarYMA._last_operation_calendar = false;
    this.grid.callEvent("onCalendarShow", [this.grid._grid_calendarYMA, this.cell.parentNode.idd, this.cell._cellIndex]);
    this.cell._cediton = true;
    this.val = this.cell.val;
    this._val = this.cell.innerHTML;
    var a = this.grid._grid_calendarYMA.draw;
    this.grid._grid_calendarYMA.draw = function() {};
    this.grid._grid_calendarYMA.setDateFormat("%Y/%m");
    this.grid._grid_calendarYMA.setDate(this.val || (new Date()));
    this.grid._grid_calendarYMA.draw = a
};
eXcell_dhxCalendarYM.prototype.detach = function() {
    if (!this.grid._grid_calendarYMA) {
        return
    }
    this.grid._grid_calendarYMA.hide();
    if (this.cell._cediton) {
        this.cell._cediton = false
    } else {
        return
    }
    if (this.grid._grid_calendarYMA._last_operation_calendar) {
        var e = this.grid._grid_calendarYMA.getFormatedDate("%Y/%m");
        var c = this.grid._grid_calendarYMA.getDate();
        this.cell.val = new Date(c);
        this.setCValue(e, c);
        this.cell._clearCell = !e;
        var a = this.val;
        this.val = this._val;
        return (this.cell.val.valueOf() != (a || "").valueOf())
    }
    return false
};
eXcell_dhxCalendarYM.prototype.setValue = function(a) {
	if (a && typeof a == "object") {
        this.cell.val = a;
        this.cell._clearCell = false;
        this.setCValue(this.grid._grid_calendarYMA.getFormatedDate(("%Y/%m"), a).toString(), this.cell.val);
        return
    }
    if (!a || a.toString()._dhx_trim() == "") {
        a = "&nbsp";
        this.cell._clearCell = true;
        this.cell.val = ""
        this.setCValue((a || "").toString(), this.cell.val);
    } else {
        this.cell._clearCell = false;
        var unFormatType = ["%Y%m", "%Y/%m"];
        this.cell.val = "";
        for(var idx in unFormatType){
        	var paramDate = new Date(this.grid._grid_calendarYMA.setFormatedDate((unFormatType[idx]), a.toString(), null, true));
        	if ((paramDate != "NaN") && (paramDate != "Invalid Date")) {
        		this.cell.val = paramDate;
        		break;
        	}
        }
        
        if ((this.cell.val == "NaN") || (this.cell.val == "Invalid Date") || (this.cell.val == "")) {
            this.cell._clearCell = true;
            this.cell.val = ""; 
            this.setCValue("&nbsp;", 0);
        } else {
        	this.cell._clearCell = false;
        	
        	a = this.grid._grid_calendarYMA.getFormatedDate(("%Y/%m"),this.cell.val);
            this.setCValue((a || "").toString(), this.cell.val)
        }
    }
};
eXcell_dhxCalendarYM.prototype.getValue = function() {
    if (this.cell._clearCell) {
        return ""
    }
    if (this.grid._dtmask_inc && this.cell.val) {
        return this.grid._grid_calendarYMA.getFormatedDate(this.grid._dtmask_inc, this.cell.val).toString()
    }
    
    return this.grid._grid_calendarYMA.getFormatedDate("%Y%m", this.getDate());
};
eXcell_dhxCalendarYMR = function(a){
	if (a) {
        this.cell = a;
        this.grid = this.cell.parentNode.grid;
        if (!this.grid._grid_calendarYMA) {
            var e = this.grid._grid_calendarYMA = new SepoaDhtmlXCalendarYM();
            this.grid.callEvent("onDhxCalendarCreated", [e]);
            var c = this.grid;
            e.attachEvent("onChange", function(flag) {
                this._last_operation_calendar = true;
                return true
            });
            var g = function(h) {
                (h || event).cancelBubble = true
            };
            dhtmlxEvent(e.base, "click", g);
            e = null
        }
    }
}
eXcell_dhxCalendarYMR.prototype = new eXcell_dhxCalendarYM;
eXcell_dhxCalendarYMR.prototype.edit = function(){};
eXcell_dhxCalendarYMR.prototype.isDisabled = function() {
    return true
};
eXcell_template = function(cell){
	if(cell){                                 // default pattern, just copy it
        this.cell = cell;
        this.grid = this.cell.parentNode.grid;
    }
	this.editable = false;
    this.isDisabled = function() {
        return true
    };
    this.setValue=function(){
    	var cell_id = this.cell.id.split('_');
    	var cIdx = cell_id[2];
    	var cId = this.grid.getColumnId(cIdx);
    	var rIdx = cell_id[1];
    	var t = this.grid.grid_template[cId];
    	var data = {};
    	for(var idx in t.columns){
    		try{
    			var k = t.columns[idx];
    			var tc = this.grid.getColIndexById(k);
    			var forceValueFlag = false;
    			if(k.indexOf("-V-") == 0){
    				tc =  this.grid.getColIndexById(k.substring(3));
    				forceValueFlag = true;
    			}
    			var type = this.grid.grid_col_type[tc];
    			var value = "";
    			if(forceValueFlag){
    				value = this.grid.cells(rIdx, tc).getValue();
    			}else if(["dhxCalendar","dhxCalendarA","dhxCalendarR","edn","ron"].indexOf(type) >= 0){
    				value = this.grid.cells(rIdx, tc).cell.innerHTML;
    			}else if(["coro","coro2","combo","comboro","comboro2"].indexOf(type) >=0){
    				value = this.grid.cells(rIdx, tc).getTitle();
    			}else{
    				value = this.grid.cells(rIdx, tc).getValue();
    			}
    			data[t.columns[idx]] = (!value || value == "null")?"":value;
    		}catch(o){
    			if(t.columns[idx]=='ROWID'){
    				data[t.columns[idx]] = rIdx;
    			}else{
    				data[t.columns[idx]] = "";
    			}
    		}
    	}
    	this.setCValue(window.dhx4.template(t.template, data));
    }
    this.getValue=function(){
        return this.cell.innerHTML;
    }
    this.edit=function(){
        return; 
    }
    this.detach=function(){
    	return;
    }
}
eXcell_template.prototype = new eXcell;
function eXcell_icon(cell){
	if(cell){
        this.cell = cell;
        this.grid = this.cell.parentNode.grid;
        //this.setValue();
    }
	this.editable = false;
    this.isDisabled = function() {
        return true
    };
	this.setValue=function(type){
		var t = type;
		if(!t){
			if(this.cell._orig_value){
				t = this.cell._orig_value;
			}else{
				var grid = (this.grid instanceof SepoaDhtmlXGridObject)? this.grid : this.grid._fake;
				var cell_id = this.cell.id.split('_');
		    	var rIdx = cell_id[1];
		    	var cIdx = this.cell.cellIndex;
		    	var cId = grid.getColumnId(cIdx);
		    	t = grid.grid_icon[cId];
			}
		}else{
			this.cell._orig_value = t;
		}
		
		//this.cell.innerHTML = '<img class="sepoa_grid_icon_default sepoa_grid_icon_'+t+'" src="'+window.CONTEXT_NAME+'/resource/design/imgs/grid/'+t+'.png" />';;
		
		try {
			this.cell.innerHTML = '';

			const target = (t??'').trim();
			const img = document.createElement('img');
			img.classList.add('sepoa_grid_icon_default');
			img.classList.add('sepoa_grid_icon_' + target);
			img.src = window.CONTEXT_NAME + '/resource/design/imgs/grid/' + target + '.png';
			
			this.cell.appendChild(img);
		} catch (e) {
			console.error(e);
		}
		
		this.cell._orig_value = t;
		return;
	}
	this.setIcon=function(type){
		this.cell.changedIconType = type;
		this.setValue(type);
	}
    this.getValue=function(){
        return this.cell._orig_value;
    }
    this.edit=function(){
        return; 
    }
    this.detach=function(){
    	return;
    }
}
eXcell_icon.prototype = new eXcell;
function eXcell_ed(a) {
    if (a) {
        this.cell = a;
        this.grid = this.cell.parentNode.grid
    }
    this.edit = function() {
        this.cell.atag = (!this.grid.multiLine) ? "INPUT" : "TEXTAREA";
        this.val = this.getValue();
        this.obj = document.createElement(this.cell.atag);
        this.obj.setAttribute("autocomplete", "off");
        this.obj.style.height = (this.cell.offsetHeight - (this.grid.multiLine ? 9 : 10)) + "px";
        this.obj.className = "dhx_combo_edit";
        this.obj.wrap = "soft";
        this.obj.style.textAlign = this.cell.style.textAlign;
        this.obj.cellObj = this;
        this.obj.firstTab = true;
        this.obj.onkeyup = function(event){
        	var k = event.which || event.keyCode;
        	if(k == 9 && this.firstTab){
        		this.value = this.cellObj.val;
        		this.firstTab = false;
        	}
        }
        this.obj.onclick = function(c) {
            (c || event).cancelBubble = true
        };
        this.obj.onmousedown = function(c) {
            (c || event).cancelBubble = true
        };
        this.obj.value = this.val;
        this.cell.innerHTML = "";
        this.cell.appendChild(this.obj);
        this.obj.onselectstart = function(c) {
            if (!c) {
                c = event
            }
            c.cancelBubble = true;
            return true
        };
        if (_isIE) {
            this.obj.focus();
            this.obj.blur()
        }
        this.obj.focus()
    };
    this.getValue = function() {
        if ((this.cell.firstChild) && ((this.cell.atag) && (this.cell.firstChild.tagName == this.cell.atag))) {
            return this.cell.firstChild.value
        }
        if (this.cell._clearCell) {
            return ""
        }
        return htmlDecode(this.cell.innerHTML.toString()._dhx_trim())
    };
    this.detach = function() {
        this.setValue(this.obj.value);
        return this.val != this.getValue()
    }
}
eXcell_ed.prototype = new eXcell;
function eXcell_edn(a) {
    if (a) {
        this.cell = a;
        this.grid = this.cell.parentNode.grid
    }
    this.getValue = function() {
        if ((this.cell.firstChild) && (this.cell.firstChild.tagName == "TEXTAREA")) {
            return this.cell.firstChild.value
        }
        if (this.cell._clearCell) {
            // return this.grid._aplNF("0", this.cell._cellIndex);
        }
        return this.cell._orig_value || this.grid._aplNFb(this.cell.innerHTML.toString()._dhx_trim(), this.cell._cellIndex)
    };
    this.detach = function() {
        var c = this.obj.value;
        this.setValue(c);
        return this.val != this.getValue()
    }
}
eXcell_edn.prototype = new eXcell_ed;
eXcell_edn.prototype.setValue = function(a) {
	if(this.grid.ednNullEnable && (a == null || a == "")){
		this.cell._orig_value = "";
		this.setCValue("","");
		return;
	}
    if (!a || a.toString()._dhx_trim() == "") {
        this.cell._clearCell = false;
        this.cell._orig_value = "0";
        return this.setCValue(0, 0);
    } else {
        this.cell._clearCell = false;
        a = this.grid._aplNFb(a, this.cell._cellIndex);
        console.log(a);
        this.cell._orig_value = a;
    }
    this.setCValue(this.grid._aplNF(a, this.cell._cellIndex), parseFloat(a))
};
eXcell_edn.prototype.edit = function() {
    this.cell.atag = (!this.grid.multiLine) ? "INPUT" : "TEXTAREA";
    this.val = this.getValue();
    this.val = (this.val=="" || this.val==0)?"":this.val;
    this.obj = document.createElement(this.cell.atag);
    this.obj.setAttribute("autocomplete", "off");
    this.obj.style.height = (this.cell.offsetHeight - (this.grid.multiLine ? 9 : 10)) + "px";
    this.obj.className = "dhx_combo_edit";
    this.obj.wrap = "soft";
    this.obj.style.textAlign = this.cell.style.textAlign;
    this.obj.onclick = function(c) {
        (c || event).cancelBubble = true
    };
    this.obj.cellObj = this;
    this.obj.firstTab = true;
    //this.obj.onkeyup = function(event){
    //	var k = event.which || event.keyCode;
    //	if(k == 9 && this.firstTab){
    //		this.value = this.cellObj.val;
    //		this.firstTab = false;
    //	}
    //	var grid = (this.cellObj.grid instanceof SepoaDhtmlXGridObject)? this.cellObj.grid : this.cellObj.grid._fake;
    //	var format = grid.grid_num_format[grid.getColumnId(this.cellObj.cell._cellIndex)];
    //	if(!format) return;
    //	var cellMask = grid._maskArr[this.parentElement._cellIndex];
    //	var nValue = this.value.replaceAll(cellMask[5], "");
    //	if(nValue != "" && nValue != "-"){
    //		var nValue = grid._aplNF(nValue, this.parentElement._cellIndex);
    //		this.value = nValue.replaceAll(cellMask[5], "");
    //	}
    //}
    this.obj.onmousedown = function(c) {
        (c || event).cancelBubble = true
    };
    this.obj.value = this.val;
    this.cell.innerHTML = "";
    this.cell.appendChild(this.obj);
    this.obj.onselectstart = function(c) {
        if (!c) {
            c = event
        }
        c.cancelBubble = true;
        return true
    };
    if (_isIE) {
        this.obj.focus();
        this.obj.blur()
    }
    this.obj.focus()
};

function eXcell_txt(a) {
    if (a) {
        this.cell = a;
        this.grid = this.cell.parentNode.grid
    }
    this.edit = function() {
        this.val = this.getValue();
        this.obj = document.createElement("TEXTAREA");
        this.obj.className = "dhx_textarea";
        this.obj.onclick = function(g) {
            (g || event).cancelBubble = true
        };
        var c = this.grid.getPosition(this.cell);
        this.obj.value = this.val;
        this.obj.style.display = "";
        this.obj.style.textAlign = this.cell.style.textAlign;
        document.body.appendChild(this.obj);
        
        this.obj.cellObj = this;
        this.obj.firstTab = true;
        this.obj.onkeyup = function(event){
        	var k = event.which || event.keyCode;
        	if(k == 9 && this.firstTab){
        		this.value = this.cellObj.val;
        		this.firstTab = false;
        	}
        }
        
        if (_isOpera) {
            this.obj.onkeypress = function(g) {
                if (g.keyCode == 9 || g.keyCode == 27) {
                    return false
                }
            }
        }
        this.obj.onkeydown = function(h) {
            var g = (h || event);
            if (g.keyCode == 9 || g.keyCode == 27) {
                globalActiveDHTMLGridObject.entBox.focus();
                globalActiveDHTMLGridObject.doKey({
                    keyCode: g.keyCode,
                    shiftKey: g.shiftKey,
                    srcElement: "0"
                });
                return false
            }
        };
        this.obj.style.left = c[0] + "px";
        this.obj.style.top = c[1] + this.cell.offsetHeight + "px";
        if (this.cell.offsetWidth < 200) {
            var e = 200
        } else {
            var e = this.cell.offsetWidth
        }
        this.obj.style.width = e + 16 + "px";
        if (_isIE) {
            this.obj.select();
            this.obj.value = this.obj.value
        }
        this.obj.focus()
    };
    this.detach = function() {
        var c = "";
        c = this.obj.value;
        if (c == "") {
            this.cell._clearCell = true
        } else {
            this.cell._clearCell = false
        }
        this.setValue(c);
        document.body.removeChild(this.obj);
        this.obj = null;
        return this.val != this.getValue()
    };
    this.getValue = function() {
    	var r;
    	var decodeFlag = false;
        
    	if (this.obj) {
            r = this.obj.value
            decodeFlag = true;
        }else if (this.cell._clearCell) {
            r = ""
            decodeFlag = true
        }else if (typeof this.cell._brval != "undefined") {
            r = this.cell._brval
            decodeFlag = true;
        }else if ((!this.grid.multiLine)) {
            r = this.cell._brval || this.cell.innerHTML
        } else {
            r = this.cell._brval || this.cell.innerHTML.replace(/<br[^>]*>/gi, "\n")._dhx_trim()
        }
        
        return decodeFlag?htmlDecode(r):r;
    }
}
eXcell_txt.prototype = new eXcell;
function eXcell_txttxt(a) {
    if (a) {
        this.cell = a;
        this.grid = this.cell.parentNode.grid
    }
    this.getValue = function() {
        if ((this.cell.firstChild) && (this.cell.firstChild.tagName == "TEXTAREA")) {
            return this.cell.firstChild.value
        }
        if (this.cell._clearCell) {
            return ""
        }
        if ((!this.grid.multiLine) && this.cell._brval) {
            return this.cell._brval
        }
        return this.cell.textContent
    };
    this.setValue = function(c) {
        this.cell._brval = c;
        if (!c || c.toString()._dhx_trim() == "") {
            c = " ";
            this.cell._clearCell = true
        } else {
            this.cell._clearCell = false
        }
        this.setCTxtValue(c)
    }
}
eXcell_txttxt.prototype = new eXcell_txt;
eXcell_txt.prototype.setValue = function(a) {
    this.cell._brval = a;
    if (!a || a.toString()._dhx_trim() == "") {
        a = "&nbsp;";
        this.cell._clearCell = true
    } else {
        this.cell._clearCell = false
    }
    if ((!this.grid.multiLine) || this.cell._clearCell) {
        this.setCValue(a, this.cell._brval)
    } else {
        this.setCValue(a.replace(/\n/g, "<br/>"), a)
    }
};

eXcell_edtxt.prototype = new eXcell_ed;

dhx4.attachEvent("onGridCreated", function(c) {
    if (!window.dhx_globalImgPath) {
        window.dhx_globalImgPath = c.imgURL
    }
    c._col_combos = c._col_combos || [];
    for (var a = 0; a < c._cCount; a++) {
        if (c.cellType[a].indexOf("combo") == 0) {
        	
            c._col_combos[a] = c._col_combos[a] ||  (window["eXcell_"+c.cellType[a]].prototype.initCombo.call({
                grid: c
            }, a))
        }
    }
});

function eXcell_combo(a) {
    if (!a) {
        return
    }
    this.cell = a;
    this.grid = a.parentNode.grid;
    this._combo_pre = "";
    this.edit = function() {
        if (!window.dhx_globalImgPath) {
            window.dhx_globalImgPath = this.grid.imgURL
        }
        this.val = this.getValue();
        var c = this.getText();
        if (this.cell._clearCell) {
            c = ""
        }
        this.cell.innerHTML = "";
        if (!this.cell._brval) {
            this.combo = (this.grid._realfake ? this.grid._fake : this.grid)._col_combos[this.cell._cellIndex]
        } else {
            this.combo = this.cell._brval
        }
        this.cell.appendChild(this.combo.DOMParent);
        this.combo.DOMParent.style.margin = "0";
        this.combo.DOMelem_input.focus();
        this.combo.DOMelem_input.readOnly=true;
        this.combo.setSize(this.cell.offsetWidth - 2);
        if (!this.combo._xml) {
            if (this.combo.getIndexByValue(this.cell.combo_value) != -1) {
                this.combo.selectOption(this.combo.getIndexByValue(this.cell.combo_value))
            } else {
                if (this.combo.getOptionByLabel(c)) {
                    this.combo.selectOption(this.combo.getIndexByValue(this.combo.getOptionByLabel(c).value))
                } else {
                    this.combo.setComboText(c)
                }
            }
        } else {
            this.combo.setComboText(c)
        }
        this.combo.openSelect()
    };
    this.selectComboOption = function(e, c) {
        c.selectOption(c.getIndexByValue(c.getOptionByLabel(e).value))
    };
    this.getValue = function(c) {
        return this.cell.combo_value || ""
    };
    this.getText = function(e) {
        var g = this.cell;
        if (this._combo_pre == "" && g.childNodes[1]) {
            g = g.childNodes[1]
        } else {
            g.childNodes[0].childNodes[1]
        }
        return (_isIE ? g.innerText : g.textContent)
    };
    this.setValue = function(h) {
        if (typeof(h) == "object") {
            this.cell._brval = this.initCombo();
            var e = this.cell._cellIndex;
            var g = this.cell.parentNode.idd;
            if (!h.firstChild) {
                this.cell.combo_value = "&nbsp;";
                this.cell._clearCell = true
            } else {
                this.cell.combo_value = h.firstChild.data
            }
            this.setComboOptions(this.cell._brval, h, this.grid, e, g)
        } else {
            this.cell.combo_value = h;
            var c = null;
            if ((c = this.cell._brval) && (typeof(this.cell._brval) == "object")) {
                h = (c.getOption(h) || {}).text || h
            } else {
                if (c = this.grid._col_combos[this.cell._cellIndex] || ((this.grid._fake) && (c = this.grid._fake._col_combos[this.cell._cellIndex]))) {
                    h = (c.getOption(h) || {}).text || h
                }
            }
            if ((h || "").toString()._dhx_trim() == "") {
                h = null
            }
            if (h !== null) {
                this.setComboCValue(h)
            } else {
                this.setComboCValue("&nbsp;", "");
                this.cell._clearCell = true
            }
        }
    };
    this.detach = function() {
        var c = this.combo.getParent();
        if (c.parentNode == this.cell) {
            this.cell.removeChild(c)
        } else {
            return false
        }
        var e = this.cell.combo_value;
        this.combo._confirmSelect("blur");
        if (!this.combo.getComboText() || this.combo.getComboText().toString()._dhx_trim() == "") {
            this.setComboCValue("&nbsp;");
            this.cell._clearCell = true
        } else {
            this.setComboCValue(this.combo.getComboText().replace(/\&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"), this.combo.getActualValue());
            this.cell._clearCell = false
        }
        this.cell.combo_value = this.combo.getActualValue();
        this.combo.closeAll();
        this.grid._still_active = true;
        this.grid.setActive(1);
        return e != this.cell.combo_value
    }
}
eXcell_combo.prototype = new eXcell;
eXcell_combo_v = function(a) {
    var c = new eXcell_combo(a);
    c._combo_pre = "<img src='" + (window.dhx_globalImgPath ? window.dhx_globalImgPath : this.grid.imgURL) + "combo_select" + (dhtmlx.skin ? "_" + dhtmlx.skin : "") + ".gif' class='dhxgrid_combo_icon'/>";
    return c
};
eXcell_combo.prototype.initCombo = function(c, o) {
    var a = document.createElement("DIV");
    a.className = "dhxcombo_in_grid_parent";
    var g = o || (this.grid.defVal[arguments.length ? c : this.cell._cellIndex]);
    var h = new dhtmlXCombo(a, "combo", 0, g);
    this.grid.defVal[arguments.length ? c : this.cell._cellIndex] = "";
    var e = this.grid;
    h.DOMelem.onmousedown = h.DOMelem.onclick = function(l) {
        l = l || event;
        l.cancelBubble = true
    };
    h.DOMelem.onselectstart = function(l) {
        l = l || event;
        l.cancelBubble = true;
        return true
    };
    this.grid.attachEvent("onScroll", function() {
        if (h._isListVisible()) {
            h._hideList()
        }
    });
    h.attachEvent("onKeyPressed", function(l) {
        if (l == 13 || l == 27) {
            e.editStop();
            if (e._fake) {
                e._fake.editStop()
            }
        }
    });
    return h
};
eXcell_combo.prototype.fillColumnCombos = function(e, a) {
    if (!a) {
        return
    }
    var g = dhx4.ajax.xmltop("rows", a, -1);
    if (g && g.tagName !== "DIV") {
        e.combo_columns = e.combo_columns || [];
        columns = dhx4.ajax.xpath("//column", g);
        for (var c = 0; c < columns.length; c++) {
            if ((columns[c].getAttribute("type") || "").indexOf("combo") == 0) {
                e.combo_columns[e.combo_columns.length] = c;
                this.setComboOptions(e._col_combos[c], columns[c], e, c)
            }
        }
    }
};
eXcell_combo.prototype.setComboCValue = function(e, c) {
    if (this._combo_pre != "") {
        var a = (this.cell.offsetHeight ? this.cell.offsetHeight + "px" : 0);
        e = "<div style='width:100%;position:relative;height:100%;overflow:hidden;'>" + this._combo_pre + "<span>" + e + "</span></div>"
    }
    if (arguments.length > 1) {
        this.setCValue(e, c)
    } else {
        this.setCValue(e)
    }
};
eXcell_combo.prototype.setComboOptions = function(l, m, e, r, u) {
    if (window.dhx4.s2b(m.getAttribute("xmlcontent"))) {
        if (!m.getAttribute("source")) {
            options = m.childNodes;
            var a = [];
            for (var n = 0; n < options.length; n++) {
                if (options[n].tagName == "option") {
                    var g = options[n].firstChild ? options[n].firstChild.data : "";
                    a[a.length] = [options[n].getAttribute("value"), g, (options[n].getAttribute("css") || "")]
                }
            }
            l.addOption(a);
            if (arguments.length == 4) {
                e.forEachRowA(function(w) {
                    var v = e.cells(w, r);
                    if (!v.cell._brval && !v.cell._cellType && (v.cell._cellIndex == r)) {
                        if (v.cell.combo_value == "") {
                            v.setComboCValue("&nbsp;", "")
                        } else {
                            if (!l.getOption(v.cell.combo_value)) {
                                v.setComboCValue(v.cell.combo_value)
                            } else {
                                v.setComboCValue(l.getOption(v.cell.combo_value).text, v.cell.combo_value)
                            }
                        }
                    }
                })
            } else {
                var s = (this.cell) ? this : e.cells(u, r);
                if (m.getAttribute("text")) {
                    if (m.getAttribute("text")._dhx_trim() == "") {
                        s.setComboCValue("&nbsp;", "")
                    } else {
                        s.setComboCValue(m.getAttribute("text"))
                    }
                } else {
                    if ((!s.cell.combo_value) || (s.cell.combo_value._dhx_trim() == "")) {
                        s.setComboCValue("&nbsp;", "")
                    } else {
                        if (!l.getOption(s.cell.combo_value)) {
                            s.setComboCValue(s.cell.combo_value)
                        } else {
                            s.setComboCValue(l.getOption(s.cell.combo_value).text, s.cell.combo_value)
                        }
                    }
                }
            }
        }
    }
    if (m.getAttribute("source")) {
        if (m.getAttribute("auto") && window.dhx4.s2b(m.getAttribute("auto"))) {
            if (m.getAttribute("xmlcontent")) {
                var s = (this.cell) ? this : e.cells(u, r);
                if (m.getAttribute("text")) {
                    s.setComboCValue(m.getAttribute("text"))
                }
            } else {
                e.forEachRowA(function(y) {
                    var x = e.cells(y, r);
                    if (!x.cell._brval && !x.cell._cellType) {
                        var w = x.cell.combo_value.toString();
                        if (w.indexOf("^") != -1) {
                            var v = w.split("^");
                            x.cell.combo_value = v[0];
                            x.setComboCValue(v[1])
                        }
                    }
                })
            }
            l.enableFilteringMode(true, m.getAttribute("source"), window.dhx4.s2b(m.getAttribute("cache") || true), window.dhx4.s2b(m.getAttribute("sub") || false))
        } else {
            var o = this;
            var h = arguments.length;
            l.load(m.getAttribute("source"), function() {
                if (h == 4) {
                    e.forEachRow(function(x) {
                        var w = e.cells(x, r);
                        if (!w.cell._brval && !w.cell._cellType) {
                            if (l.getOption(w.cell.combo_value)) {
                                w.setComboCValue(l.getOption(w.cell.combo_value).text, w.cell.combo_value)
                            } else {
                                if ((w.cell.combo_value || "").toString()._dhx_trim() == "") {
                                    w.setComboCValue("&nbsp;", "");
                                    w.cell._clearCell = true
                                } else {
                                    w.setComboCValue(w.cell.combo_value)
                                }
                            }
                        }
                    })
                } else {
                    var v = e.cells(u, r);
                    if (l.getOption(v.cell.combo_value)) {
                        v.setComboCValue(l.getOption(v.cell.combo_value).text, v.cell.combo_value)
                    } else {
                        v.setComboCValue(v.cell.combo_value)
                    }
                }
            })
        }
    }
    // if (!m.getAttribute("auto") || !window.dhx4.s2b(m.getAttribute("auto")))
	// {
    // if (m.getAttribute("editable") &&
	// !window.dhx4.s2b(m.getAttribute("editable"))) {
            l.readonly(true)
    // }
    // if (m.getAttribute("filter") &&
	// window.dhx4.s2b(m.getAttribute("filter"))) {
    // l.enableFilteringMode(true)
    // }
    // }
};
eXcell_combo.prototype.setCellCombo = function(o) {
    this.cell._brval = this.initCombo(void 0, o);
    return this.cell._brval
};
eXcell_combo.prototype.getCellCombo = function() {
    if (!this.cell._brval) {
        this.setCellCombo();
    }
    return this.cell._brval
};
eXcell_combo.prototype.refreshCell = function() {
    this.setValue(this.getValue())
};
eXcell_combo.prototype.loadCombo=function(code, param, blank_flag){
	param = window.dhx4.comboParam(param);
	
	var queryString = "&code=" + code + "&param=" + param;
	this.grid._ajaxStatusW.combo++;
	$.ajax({
		url: window.CONTEXT_NAME+"/servlets/sepoa.svl.util.SepoaAjaxCombo?timeStamp=" + new Date().getTime(),
		type:"post",
		data: queryString,
		gridObj:this.grid,
		cellObj:this,
		selectBox:this.getCellCombo(),
		dataType: "text",
		success: function(data){
			
			var combo_data = new Array();
			var combobox = this.selectBox;
			combobox.clearAll();
			
			var result = new SepoaFormater(data);
			
			if(blank_flag)
				combobox.addOption("", "");
			
			for(var i=0; i<result.getRowCount() ; i++){
				combobox.addOption(result.getValue('value', i), result.getValue('text', i));
			}
			
			this.cellObj.setValue(this.cellObj.getValue())
			
			if(this.gridObj._checkAjaxStatus('combo')){
				this.gridObj.callEvent('alreadyCallConnection',[]);
			}
		},
		error:function(){
			if(this.gridObj._checkAjaxStatus('combo')){
				this.gridObj.callEvent('alreadyCallConnection',[]);
			}
		}
	});
}

function eXcell_comboro(a) {
    if (!a) {
        return
    }
    this.cell = a;
    this.grid = a.parentNode.grid;
    this._combo_pre = "";
    this.edit = function() {
        if (!window.dhx_globalImgPath) {
            window.dhx_globalImgPath = this.grid.imgURL
        }
        this.val = this.getValue();
        var c = this.getText();
        if (this.cell._clearCell) {
            c = ""
        }
        this.cell.innerHTML = "";
        if (!this.cell._brval) {
            this.combo = (this.grid._realfake ? this.grid._fake : this.grid)._col_combos[this.cell._cellIndex]
        } else {
            this.combo = this.cell._brval
        }
        this.cell.appendChild(this.combo.DOMParent);
        this.combo.DOMParent.style.margin = "0";
        this.combo.DOMelem_input.focus();
        this.combo.DOMelem_input.readOnly=true;
        this.combo.setSize(this.cell.offsetWidth - 2);
        if (!this.combo._xml) {
            if (this.combo.getIndexByValue(this.cell.combo_value) != -1) {
                this.combo.selectOption(this.combo.getIndexByValue(this.cell.combo_value))
            } else {
                if (this.combo.getOptionByLabel(c)) {
                    this.combo.selectOption(this.combo.getIndexByValue(this.combo.getOptionByLabel(c).value))
                } else {
                    this.combo.setComboText(c)
                }
            }
        } else {
            this.combo.setComboText(c)
        }
        this.combo.openSelect()
    };
    this.selectComboOption = function(e, c) {
        c.selectOption(c.getIndexByValue(c.getOptionByLabel(e).value))
    };
    this.getValue = function(c) {
        return this.cell.combo_value || ""
    };
    this.getText = function(e) {
        var g = this.cell;
        if (this._combo_pre == "" && g.childNodes[1]) {
            g = g.childNodes[1]
        } else {
            g.childNodes[0].childNodes[1]
        }
        return (_isIE ? g.innerText : g.textContent)
    };
    this.setValue = function(h) {
        if (typeof(h) == "object") {
            this.cell._brval = this.initCombo();
            var e = this.cell._cellIndex;
            var g = this.cell.parentNode.idd;
            if (!h.firstChild) {
                this.cell.combo_value = "&nbsp;";
                this.cell._clearCell = true
            } else {
                this.cell.combo_value = h.firstChild.data
            }
            this.setComboOptions(this.cell._brval, h, this.grid, e, g)
        } else {
            this.cell.combo_value = h;
            var c = null;
            if ((c = this.cell._brval) && (typeof(this.cell._brval) == "object")) {
                h = (c.getOption(h) || {}).text || h
            } else {
                if (c = this.grid._col_combos[this.cell._cellIndex]) {
                    var rh = (c.getOption(h) || {}).text;
                    if(!rh && (this.grid._fake) && (c = this.grid._fake._col_combos[this.cell._cellIndex])){
                    	rh = (c.getOption(h) || {}).text || h
                    }
                    h = rh;
                }
            }
            if ((h || "").toString()._dhx_trim() == "") {
                h = null
            }
            if (h !== null) {
                this.setComboCValue(h)
            } else {
                this.setComboCValue("&nbsp;", "");
                this.cell._clearCell = true
            }
        }
    };
    this.detach = function() {
        var c = this.combo.getParent();
        if (c.parentNode == this.cell) {
            this.cell.removeChild(c)
        } else {
            return false
        }
        var e = this.cell.combo_value;
        this.combo._confirmSelect("blur");
        if (!this.combo.getComboText() || this.combo.getComboText().toString()._dhx_trim() == "") {
            this.setComboCValue("&nbsp;");
            this.cell._clearCell = true
        } else {
            this.setComboCValue(this.combo.getComboText().replace(/\&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"), this.combo.getActualValue());
            this.cell._clearCell = false
        }
        this.cell.combo_value = this.combo.getActualValue();
        this.combo.closeAll();
        this.grid._still_active = true;
        this.grid.setActive(1);
        return e != this.cell.combo_value
    }
}
eXcell_comboro.prototype = new eXcell_combo;

function eXcell_comboro2(a){
	this.base = eXcell_comboro;
	this.base(a);
	this.isDisabled = function() {
        return true
    };
}
eXcell_comboro2.prototype = new eXcell_comboro;

function eXcell_coro2(a) {
    this.base = eXcell_co;
    this.base(a);
    this.editable = false;
    this.isDisabled = function() {
        return true
    };
}
eXcell_coro2.prototype = new eXcell_co;

function eXcell_autocomplete(a) {
    if (!a) {
        return
    }
    this.cell = a;
    this.grid = a.parentNode.grid;
    this._combo_pre = "";
    this.edit = function() {
        if (!window.dhx_globalImgPath) {
            window.dhx_globalImgPath = this.grid.imgURL
        }
        this.val = this.getValue();
        var c = this.getText();
        if (this.cell._clearCell) {
            c = ""
        }
        this.cell.innerHTML = "";
        if (!this.cell._brval) {
        	this.cell._brval = this.initCombo();
        }
        this.combo = this.cell._brval
        
        this.cell.appendChild(this.combo.DOMParent);
        this.combo.DOMParent.style.margin = "0";
        this.combo.DOMelem_input.focus();
        this.combo.setSize(this.cell.offsetWidth - 2);
        if (!this.combo._xml) {
            if (this.combo.getIndexByValue(this.cell.combo_value) != -1) {
                this.combo.selectOption(this.combo.getIndexByValue(this.cell.combo_value))
            } else {
                if (this.combo.getOptionByLabel(c)) {
                    this.combo.selectOption(this.combo.getIndexByValue(this.combo.getOptionByLabel(c).value))
                } else {
                    this.combo.setComboText(c)
                }
            }
        } else {
            this.combo.setComboText(c)
        }
        this.combo.openSelect()
    };
    this.selectComboOption = function(e, c) {
        c.selectOption(c.getIndexByValue(c.getOptionByLabel(e).value))
    };
    this.getValue = function(c) {
        return this.cell.combo_value || ""
    };
    this.getText = function(e) {
        var g = this.cell;
        if (this._combo_pre == "" && g.childNodes[1]) {
            g = g.childNodes[1]
        } else {
            g.childNodes[0].childNodes[1]
        }
        return (_isIE ? g.innerText : g.textContent)
    };
    this.setValue = function(h) {
    	if(!this.cell._brval){
    		this.cell._brval = this.initCombo();
    	}
    	if(h != null && h != ""){
	    	var c = this.cell._brval;
	    	if(c.getIndexByValue(h) >= 0){
	    		this.setComboCValue(c.getOption(h).text.TEXT);
	            this.cell.combo_value = h;
	    	}else{
	    		c.callEvent("onDynXLS",[null, null, h]);
	    	}
    	}else{
    		this.setComboCValue("&nbsp;", "");
    		//this.cell._clearCell = true
    	}
    };
    this.detach = function() {
    	if(this.combo._sConfig.f_server_tm){
    		window.clearTimeout(this.combo._sConfig.f_server_tm)
    	}
        var c = this.combo.getParent();
        if (c.parentNode == this.cell) {
            this.cell.removeChild(c)
        } else {
            return false
        }
        var e = this.cell.combo_value;
        this.combo._confirmSelect("blur");
        if (!this.combo.getComboText() || this.combo.getComboText().toString()._dhx_trim() == "") {
            this.setComboCValue("&nbsp;");
            this.cell._clearCell = true
        } else {
            this.setComboCValue(this.combo.getComboText().replace(/\&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"), this.combo.getActualValue());
            this.cell._clearCell = false
        }
        this.cell.combo_value = this.combo.getActualValue();
        this.combo.closeAll();
        this.grid._still_active = true;
        this.grid.setActive(1);
        return e != this.cell.combo_value
    }
    this.getSelectedUserData=function(){
    	if(this.cell._brval._sepoadata && this.cell._brval._sepoadata[this.getValue()]){
    		return this.cell._brval._sepoadata[this.getValue()];
    	}
    }
}
eXcell_autocomplete.prototype = new eXcell_combo;
eXcell_autocomplete.prototype.initCombo = function(c, o) {
    var a = document.createElement("DIV");
    a.className = "dhxcombo_in_grid_parent";
    var g = o || (this.grid.defVal[arguments.length ? c : this.cell._cellIndex]);
    var h = new dhtmlXCombo(a, "combo", 0, g);
    h._mcMakeTemplate = function(o) {
	    var l = "";
	    var g = "";
	    this.conf.col_w = 0;
	    for (var m = 0; m < o.length; m++) {
	        var a = Number(parseInt(o[m].width) || 50);
	        var e = (o[m].css || "");
	        var n = (m == 0 && window.dhx4.isIE6 == true ? "_first" : "");
	        g += "<div class='dhxcombo_cell" + n + " " + e + "' style='min-width:" + a + "px;'><div class='dhxcombo_cell_text'>" + (o[m].option || "&nbsp;") + "</div></div>";
	        l += "<div class='dhxcombo_hdrcell" + n + " " + e + "' style='min-width:" + a + "px;'><div class='dhxcombo_hdrcell_text'>" + (o[m].header || "&nbsp;") + "</div></div>";
	        this.conf.col_w += a + 1
	    }
	    var a = 500;
	    var c = document.createElement("DIV");
	    c.style.position = "absolute";
	    c.style.top = "10px";
	    c.style.left = -a * 2 + "px";
	    c.style.width = a + "px";
	    c.style.height = "50px";
	    c.style.overflowY = "scroll";
	    c.innerHTML = "<div>&nbsp;</div>";
	    document.body.appendChild(c);
	    this.conf.col_w += a - c.firstChild.offsetWidth + 10;
	    c.parentNode.removeChild(c);
	    c = null;
	    this.conf.template.option = g;
	    this._mcAttachHeader(l);
	    this.list.className += " dhxcombolist_multicolumn"
	};
	h._showList = function(a) {
	    if (this._getListVisibleCount() == 0) {
	        if (a && this._isListVisible()) {
	            this._hideList()
	        }
	        return
	    }
	    if (this._isListVisible()) {
	        this._checkListHeight();
	        return
	    }
	    this.list.style.zIndex = window.dhx4.zim.reserve(this.conf.list_zi_id);
	    if (this.hdr != null && this.conf.template.header == true) {
	        this.hdr.style.zIndex = Number(this.list.style.zIndex) + 1
	    }
	    this.list.style.visibility = "hidden";
	    this.list.style.display = "";
	    if (this.hdr != null && this.conf.template.header == true) {
	        this.hdr.style.visibility = this.list.style.visibility;
	        this.hdr.style.display = this.list.style.display
	    }
	    var c = (this.hdr != null && this.conf.template.header == true ? this.hdr.offsetHeight : 0);
	    this.list.style.minWidth = Math.max(this.conf.opts_width || this.conf.col_w || 0, this.conf.combo_width) + "px";
	    this.list.style.top = window.dhx4.absTop(this.base) + c + this.base.offsetHeight - 1 + "px";
	    this.list.style.left = window.dhx4.absLeft(this.base) + "px";
	    if (this.hdr != null && this.conf.template.header == true) {
	        this.hdr.style.width = this.list.style.width;
	        this.hdr.style.left = this.list.style.left;
	        this.hdr.style.top = parseInt(this.list.style.top) - c + "px"
	    }
	    this._checkListHeight();
	    this.list.style.visibility = "visible";
	    if (this.hdr != null && this.conf.template.header == true) {
	        this.hdr.style.visibility = "visible"
	    }
	    this.callEvent("onOpen", [])
	};
	h.setTemplate({
	    input: "#TEXT#",
	    header: false,
	    columns: [
	        {header: "", option: "#TEXT#"},
	        {header: "", option: "#DESCRIPTION#"}
	    ]
	});
	h.parents = this;
	h._sConfig = {
		dynParam:[]
	}
	var opt = this.grid.grid_autocomplete[this.grid.getColumnId(this.cell._cellIndex)]
	if(opt){
		h._sConfig.code = opt.CODE;
		h._sConfig.param = opt.PARAMS; 
	}
	h.enableFilteringMode(true, "custom")
	h.attachEvent("onDynXLS", function(text, ind, def){
		var load = function(obj){
			obj._sConfig.dynParam = [];
			obj.parents.grid.callEvent("onAutoComplete", [obj._sConfig.dynParam]);
			var param = obj._sConfig.param;
			if(obj._sConfig.dynParam.length > 0){
				param = param + "#" + obj._sConfig.dynParam.join('#'); 
			}
			var qs = "code="+obj._sConfig.code+"&param="+window.dhx4.comboParam(param || '')
			if(def){
				qs = qs+"&default="+def
			}else{
				qs = qs+"&mask="+text
			}
			$.ajax({
				url: window.CONTEXT_NAME+"/SepoaAutoComplete.sux",
				type:"post",
				data: qs,
				comboObj:obj,
				defaultOption:def,
				dataType: "json",
				cache: false,
				success: function(data){
					this.comboObj.clearAll();
					this.comboObj._sepoadata = {};
					var flag = true;
					for(var i in data){
						this.comboObj._sepoadata[data[i].VALUE] = {}
						for(var j in data[i]){
							this.comboObj._sepoadata[data[i].VALUE][j] = data[i][j];
						}
						flag = false;
						this.comboObj.addOption([{value:data[i].VALUE, text:this.comboObj._sepoadata[data[i].VALUE]}]);
					}
					if(flag){
						this.comboObj.callEvent("onChange", []);
					}
					if(this.defaultOption){
						this.comboObj._sConfig._disableEvent = true;
						this.comboObj.setComboValue(this.defaultOption);
						this.comboObj._sConfig._disableEvent = false;
			            this.comboObj._currentComboValue = this.comboObj.getActualValue();
			            this.comboObj.parents.setComboCValue(this.comboObj.getOption(this.defaultOption).text.TEXT);
			            this.comboObj.parents.cell.combo_value = this.defaultOption;
					}else{
						this.comboObj.callEvent("onChange", []);
						this.comboObj.openSelect();
					}
				}
			});
			obj._sConfig.f_server_tm = null;
		}
		if(this._sConfig.f_server_tm){
			 window.clearTimeout(this._sConfig.f_server_tm)
		}
		if(def){
			this._sConfig.f_server_tm = window.setTimeout(load, 0, this);
		}else if (text.trim().length == 0) {
			this.closeAll();
			this.clearAll();
			this.setComboText("");
		} else {
			if(this._sConfig.code){
				this._sConfig.f_server_tm = window.setTimeout(load , 500, this)
			}
		}
	});
    
    this.grid.defVal[arguments.length ? c : this.cell._cellIndex] = "";
    var e = this.grid;
    h.DOMelem.onmousedown = h.DOMelem.onclick = function(l) {
        l = l || event;
        l.cancelBubble = true
    };
    h.DOMelem.onselectstart = function(l) {
        l = l || event;
        l.cancelBubble = true;
        return true
    };
    this.grid.attachEvent("onScroll", function() {
        if (h._isListVisible()) {
            h._hideList()
        }
    });
    h.attachEvent("onKeyPressed", function(l) {
        if (l == 13 || l == 27) {
            e.editStop();
            if (e._fake) {
                e._fake.editStop()
            }
        }
    });
    return h
};

function eXcell_tree(a) {
    if (a) {
        this.cell = a;
        this.grid = this.cell.parentNode.grid
    }
    this.isDisabled = function() {
        return this.cell._disabled || this.grid._edtc
    	// return true;
    };
    this.edit = function() {
        if ((this.er) || (this.grid._edtc)) {
            return
        }
        this.er = this.cell.parentNode.valTag;
        this.val = this.getLabel();
        this.cell.atag = ((!this.grid.multiLine) && (_isKHTML || _isMacOS || _isFF)) ? "INPUT" : "TEXTAREA";
        this.er.innerHTML = "<" + this.cell.atag + " class='dhx_combo_edit' type='text' style='height:" + (this.cell.offsetHeight - 4) + "px;line-height:" + (this.cell.offsetHeight - 6) + "px; width:100%; border:0px; margin:0px; padding:0px; overflow:hidden;'></" + this.cell.atag + ">";
        this.er.childNodes[0].onmousedown = function(c) {
            (c || event).cancelBubble = true
        };
        this.er.childNodes[0].onselectstart = function(c) {
            if (!c) {
                c = event
            }
            c.cancelBubble = true;
            return true
        };
        this.er.className += " editable";
        this.er.firstChild.onclick = function(c) {
            (c || event).cancelBubble = true
        };
        this.er.firstChild.value = this.val;
        this.obj = this.er.firstChild;
        this.er.firstChild.style.width = Math.max(0, this.cell.offsetWidth - this.er.offsetLeft - 2) + "px";
        this.er.firstChild.focus();
        if (_isIE) {
            this.er.firstChild.focus()
        }
    };
    this.detach = function() {
        if (!this.er) {
            return
        }
        this.setLabel(this.er.firstChild.value);
        this.er.className = this.er.className.replace("editable", "");
        var c = (this.val != this.er.innerHTML);
        this.obj = this.er = null;
        return (c)
    };
    this.getValue = function() {
        return this.getLabel()
    };
    this.setImage = function(c) {
        this.cell.parentNode.imgTag.nextSibling.src = this.grid.iconURL + c;
        this.grid._h2.get[this.cell.parentNode.idd].image = c
    };
    this.setIcon = function(c) {
        this.grid._h2.get[this.cell.parentNode.idd].icon = c;
        this.cell.parentNode.imgTag.nextSibling.className = "dhx_treegrid_icon fa fa-" + c
    };
    this.getImage = function() {
        return this.grid._h2.get[this.cell.parentNode.idd].image
    };
    this.getIcon = function() {
        return this.grid._h2.get[this.cell.parentNode.idd].icon
    };
    this.setLabel = function(c) {
        this.setValueA(c)
    };
    this.getLabel = function(c) {
        return this.cell.parentNode.valTag.innerHTML
    }
}
eXcell_tree.prototype = new eXcell;
eXcell_tree.prototype.setValueA = function(a) {
    //this.cell.parentNode.valTag.innerHTML = a;
    this.cell.parentNode.valTag.innerHTML = '';
    this.cell.parentNode.valTag.appendChild(document.createTextNode(htmlDecode(a)));
    this.grid.callEvent("onCellChanged", [this.cell.parentNode.idd, this.cell._cellIndex, a])
};
eXcell_tree.prototype.setValue = function(a) {
    if (this.cell.parentNode.imgTag) {
        return this.setLabel(a)
    }
    if ((this.grid._tgc.iconTree == null) || (this.grid._tgc.iconTree != this.grid.iconTree)) {
        var h = {};
        h.spacer = "<img src='" + this.grid.iconTree + "blank.gif'  align='top' class='space'>";
        h.imst = "<img class='grid_collapse_icon' src='" + this.grid.iconTree;
        h.imsti = "<img style='padding-top:2px;'  src='" + (this.grid.iconURL || this.grid.iconTree);
        h.imact = "' align='top' onclick='this." + (_isKHTML ? "" : "parentNode.") + "parentNode.parentNode.parentNode.parentNode.grid.doExpand(this);event.cancelBubble=true;'>";
        h.plus = h.imst + "plus.gif" + h.imact;
        h.minus = h.imst + "minus.gif" + h.imact;
        h.blank = h.imst + "blank.gif" + h.imact;
        h.start = "<div class='treegrid_cell' style='overflow:hidden; white-space : nowrap; line-height:23px; height:" + (_isIE ? 21 : 23) + "px;'>";
        h.itemim = "' align='top' " + (this.grid._img_height ? (' height="' + this.grid._img_height + '"') : "") + (this.grid._img_width ? (' width="' + this.grid._img_width + '"') : "") + " >";
        h.itemne = "<span id='nodeval'>";
        h.close = "</span></div>";
        this.grid._tgc = h
    }
    var l = this.grid._h2;
    var h = this.grid._tgc;
    var g = this.cell.parentNode.idd;
    var m = this.grid._h2.get[g];
    if (this.grid.kidsXmlFile || this.grid._slowParse) {
        m.has_kids = (m.has_kids || (this.cell.parentNode._attrs.xmlkids && (m.state != "minus")));
        m._xml_await = !!m.has_kids
    }
    m.image = m.image || (this.cell._attrs.image || "leaf.gif");
    m.icon = m.icon || (this.cell._attrs.icon || (this.grid.iconset ? "file-o" : ""));
    //m.label = a;
    const temp = document.createElement('div');
    temp.appendChild(document.createTextNode(a));
    //m.label = temp.innerHTML;
    m.label = temp.innerText;
    
    var e = [h.start];
    for (var c = 0; c < m.level; c++) {
        e.push(h.spacer)
    }
    if (m.has_kids) {
        e.push(h.plus);
        m.state = "plus"
    } else {
        e.push(h.imst + m.state + ".gif" + h.imact)
    }
    if (!m.icon) {
        e.push(h.imsti);
        e.push(m.image);
        e.push(h.itemim)
    } else {
        e.push("<i class='dhx_treegrid_icon fa fa-" + m.icon + "'></i>")
    }
    e.push(h.itemne);
    e.push(m.label);
    e.push(h.close);
    this.cell.innerHTML = e.join("");
    this.cell._treeCell = true;
    this.cell.parentNode.imgTag = this.cell.childNodes[0].childNodes[m.level];
    this.cell.parentNode.valTag = this.cell.childNodes[0].childNodes[m.level + 2];
    if (_isKHTML) {
        this.cell.vAlign = "top"
    }
    if (m.parent.id != 0 && m.parent.state == "plus") {
        this.grid._updateTGRState(m.parent, false);
        this.cell.parentNode._skipInsert = true
    }
    this.grid.callEvent("onCellChanged", [g, this.cell._cellIndex, a])
};
eXcell_tree.prototype.editable = false;
function eXcell_sub_row_grid(a) {
    this.base = eXcell_sub_row;
    this.base(a);
    this.setValue = function(c) {
    	/*
		 * if (c === '') c = 'true' else if(c === 'false') c = ''
		 * 
		 * if (c) { this.grid.setUserData(this.cell.parentNode.idd, "__sub_row",
		 * c) }
		 */
        this.cell._sub_row_type = "grid";
        this._setState("plus.gif")
    };
    this.getSubGrid = function() {
        if (!a._sub_grid) {
            return null
        }
        return a._sub_grid
    }
    this.isOpen=function(){
    	if(!this.cell.parentNode._expanded){
    		return false
    	}else{
    		return this.cell.parentNode._expanded.style.display != "none"
    	}
    }
}
eXcell_sub_row_grid.prototype = new eXcell_sub_row;

// XSS vulnerability fix
eXcell_link.prototype.setValue = function(e) {
    if ((typeof(e) != "number") && (!e || e.toString()._dhx_trim() == "")) {
        this.setCValue("&nbsp;", c);
        return (this.cell._clearCell = true)
    }
    var c = e.split("^");
    if (c.length == 1) {
        c[1] = ""
    } else {
        if (c.length > 1) {
            c[1] = "href='" + c[1] + "'";
            if (c.length == 3) {
                c[1] += " target='" + c[2] + "'"
            } else {
                c[1] += " target='_blank'"
            }
        }
    }

    const temp = document.createElement('div');
    temp.appendChild(document.createTextNode(c[0]));
    const label = temp.innerHTML;

    this.setCValue("<a " + c[1] + " onclick='(_isIE?event:arguments[0]).cancelBubble = true;'>" + label + "</a>", c)
};



SepoaDhtmlXGridObject.prototype.enableBlockSelection = function(e) {
	var a = this;
    if (typeof this._bs_mode == "undefined") {
        this.obj.onmousedown = function(g) {
            if (a._bs_mode) {
                a._OnSelectionStart((g || event), this)
            }
            return true
        };
        this._CSVRowDelimiter = this.csv.row;
        this.attachEvent("onResize", function() {
            a._HideSelection();
            return true
        });
        this.attachEvent("onGridReconstructed", function() {
            a._HideSelection();
            return true
        });
        this.attachEvent("onFilterEnd", this._HideSelection)
        this.attachEvent('onKeyPress',function(code, c, s){
	    	if(code == '27' && !c && !s){
	    		a._HideSelection();
	    	}
	    	return true;
        })
    }
    if (e === false) {
        this._bs_mode = false;
        return this._HideSelection()
    } else {
        this._bs_mode = true
    }
    if (!window.dhx4.isIPad) {
        var c = this._clip_area = document.createElement("textarea");
        c.style.cssText = "position:absolute; width:1px; height:1px; overflow:hidden; color:transparent; background-color:transparent; bottom:1px; right:1px; border:none;";
        c.onkeydown = function(g) {
            g = g || event;
            if (g.keyCode == 86 && (g.ctrlKey || g.metaKey)) {
                a.pasteBlockFromClipboard()
            }
        };
        document.body.insertBefore(this._clip_area, document.body.firstChild);
        dhtmlxEvent(this.entBox, "click", function() {
            if (a && !a.editor && a._clip_area) {
                a._clip_area.select()
            }
        })
    }
};

SepoaDhtmlXGridObject.prototype.customCodeSearch = function(r,c,h,p){
	var v = this.cells(r,c).getValue();
	if(v.length > 0){
		var code = p[0];
		var param = window.dhx4.comboParam((typeof(p[1])=='undefined' || p[1]==null)?'':p[1]);	
		param += '#' + v;
		var so = doServiceAjax('CO_012','CONNECTION','getCustomCodeSearch',{code:code,param:param});
		if(so.status != 1){
			Alert(so.message);
			return;
		}
		
		var sf = new SepoaFormater(so.result[0]);
		
		if(sf.getRowCount() > 0){
			this.cells(r,this.getColIndexById(h)).setValue(sf.getValue('TEXT',0));
		}else{
			Alert(Sepoa.cache.text.get('MESSAGE.5300'));
			this.cells(r,c).setValue('');
			this.cells(r,this.getColIndexById(h)).setValue('');
		}
	}else{
		this.cells(r,this.getColIndexById(h)).setValue('');
	}
};
SepoaDhtmlXGridObject.prototype._doOnRowCreated = function(rId,rObj,rXml) {
	// this.setCellTextStyle(rId, ind, styleString);
	var r = this.getRowById(rId);
	if (!r) {
		return
	}
	for (var i = 0; i < r.childNodes.length; i++) {
		var colType = this.grid_col_color[this.grid_col_id.indexOf(this.getColumnId(i))];
		if(colType){
			r.childNodes[i].classList.add('sepoa_grid_column_type_'+colType.toLowerCase());
		}
	}
};
SepoaDhtmlXGridObject.prototype.addRowFromWindow=function(mode, select){
	select = select||"SELECTED";
	var grid_array = [];
	if(mode == "update"){
		grid_array = this.getGridChangedRows(select);
		if(grid_array.length == 0){
			return;
		}
	}
	var popup = PopupOpenPost("/common/addrowForm.sux", {gridObj:this, mode:(mode||'insert'), selected:select, rid:grid_array[0]}, undefined, "450", "500", "", 'addrow');
	popup[popup.name].button('minmax').hide();
	popup[popup.name].hideHeader();
};
SepoaDhtmlXGridObject.prototype.getSumData=function(column, select, value){
	var grid_array = [];
	var sum = 0;
	if(select && select == this.grid_selected){
		grid_array = this.getGridChangedRows(select);
		for(var i in grid_array){
			var rowData = this._getRowData(grid_array[i]);
			sum += parseInt(isNaN(rowData[column])?0:rowData[column]);
		}
	}else{
		var selectedFlag = select && (value != undefined)
		for(var i=0;i<this.getRowsNum();i++){
			var rowData = this._getRowData(this.getRowId(i));
			
			if(selectedFlag){
				if(rowData[select] == value){
					sum += parseInt(isNaN(rowData[column])?0:rowData[column]);
				}
			}else{
				sum += parseInt(isNaN(rowData[column])?0:rowData[column]);
			}
		}
	}
	return sum;
}
SepoaDhtmlXGridObject.prototype.checkItemExist = function(cId, value){
	var rowIds = this.getAllRowIds();
	if(rowIds == null || rowIds == ""){
		return false;
	}
	rowIds = rowIds.split(",");
	for(var i=0;i<rowIds.length ; i++){
		var rId = rowIds[i];
		var gridValue = this.cells(rId, this.getColIndexById(cId)).getValue();
		if(gridValue == value){
			return rId;
		}
	}
	return false;
}
SepoaDhtmlXGridObject.prototype.checkSubItemExist = function(rId, cId, value){
	var rowIds = this.getSubItems(rId);
	if(rowIds == null || rowIds == ""){
		return false;
	}
	rowIds = rowIds.split(",");
	for(var i=0;i<rowIds.length ; i++){
		var rId = rowIds[i];
		var gridValue = this.cells(rId, this.getColIndexById(cId)).getValue();
		if(gridValue == value){
			return rId;
		}
	}
	return false;
}
SepoaDhtmlXGridObject.prototype.getRootNode = function(rId){
	var pid = this.getParentId(rId);
	if(pid){
		return this.getRootNode(pid);
	}else{
		return rId;
	}
}
SepoaDhtmlXGridObject.prototype.ednNullEnabled = function(flag){
	this.ednNullEnable = flag
}

SepoaDhtmlXGridObject.prototype.setRowGroupMerge=function(col_num,col_name){
	var cnt = 0;
	var temp1 = "";
	var temp2 = "";
	var col_num_cnt = col_num.split(",");
	var col_name_cnt = col_name.split(",");
	for(var i = 1; i <= this.getRowsNum(); i++){
		cnt = 0;
		temp1 = "";
		for( var k = 0 ; k < col_name_cnt.length ; k++){
			temp1 += LRTrim(this.cells(i, this.getColIndexById(col_name_cnt[k])).getValue()+"");
		}
		// 해당 필드의 똑같은 데이타를 가지고 있는 로우의 갯수를 셈.
		for(var j = i; j <= this.getRowsNum(); j++){
			temp2 = "";
			for( var k = 0 ; k < col_name_cnt.length ; k++){
				temp2 += LRTrim(this.cells(j, this.getColIndexById(col_name_cnt[k])).getValue()+"");
			}
			if(temp1 == temp2){
				cnt = cnt + 1;
				if(temp1 == "" && temp2 == "") cnt = 1;
			}else{
				break;
			}
		}
		// 그 row수만 큼 span.
		for(var m = 0; m<col_num_cnt.length; m++){
			for(var n = Number(col_num_cnt[m].split("-")[0]); n <= Number(col_num_cnt[m].split("-")[1]); n++){
				this.setRowspan(i,n,cnt);// 줄 / 칸 / 갯수
			} 
		}
		i = i + cnt - 1;
		// alert(i + " " + n + " " + cnt);
	}
}
SepoaDhtmlXGridObject.prototype.insertColumn = function(e, n, r, a, m, o, u, c, h) {
    e = parseInt(e);
    if (e > this._cCount) {
        e = this._cCount
    }
    if (!this._cMod) {
        this._cMod = this._cCount
    }
    this._processAllArrays(this._cCount, e - 1, [(n || "&nbsp;"), (a || 100), (r || "ed"), (o || "left"), (u || ""), (m || "na"), (h || ""), "", this._cMod, (a || 100)]);
    this._processAllRows("_addColInRow", e);
    if (typeof(n) == "object") {
        for (var l = 1; l < this.hdr.rows.length; l++) {
            if (n[l - 1] == "#rspan") {
                var w = l - 1;
                var v = false;
                var s = null;
                while (!v) {
                    var s = this.hdr.rows[w];
                    for (var g = 0; g < s.cells.length; g++) {
                        if (s.cells[g]._cellIndex == e) {
                            v = g;
                            break
                        }
                    }
                    w--
                }
                this.hdr.rows[w + 1].cells[g].rowSpan = (this.hdr.rows[w].cells[g].rowSpan || 1) + 1
            } else if(n[l-1] == "#cspan"){
        		this.hdr.rows[l].cells[e].style.display = "none";
        		for(var i=1;i<=e;i++){
        			if(this.hdr.rows[l].cells[e-i].style.display != "none"){
        				this.hdr.rows[l].cells[e-i].colSpan=i+1;
        				break;
        			}
        		}
        	} else {
            	
                this.setHeaderCol(e, (n[l - 1] || "&nbsp;"), l)
            }
        }
    } else {
    	if(n == "#cspan"){
    		this.hdr.rows[1].cells[e].style.display = "none";
    		for(var i=1;i<=e;i++){
    			if(this.hdr.rows[1].cells[e-i].style.display != "none"){
    				this.hdr.rows[1].cells[e-i].colSpan=i+1;
    				break;
    			}
    		}
    	}
        this.setHeaderCol(e, (n || "&nbsp;"))
    }
    this.hdr.rows[0].cells[e];
    this._cCount++;
    this._cMod++;
    this._master_row = null;
    this.setSizes()
};
SepoaDhtmlXGridObject.prototype._sub_row_render = {
    ajax: function(that, d, td, c) {
        d.innerHTML = "Loading...";
        dhx4.ajax.get(c, function(xml) {
            d.innerHTML = xml.xmlDoc.responseText;
            var z = xml.xmlDoc.responseText.match(/<script[^>]*>([^\f]+?)<\/script>/g);
            if (z) {
                for (var i = 0; i < z.length; i++) {
                    eval(z[i].replace(/<([\/]{0,1})s[^>]*>/g, ""))
                }
            }
            that._detectHeight(d, td);
            that._correctMonolite();
            that.setUserData(td.parentNode.idd, "__sub_row", xml.xmlDoc.responseText);
            td._sub_row_type = null;
            if (that._ahgr) {
                that.setSizes()
            }
            that.callEvent("onSubAjaxLoad", [td.parentNode.idd, xml.xmlDoc.responseText])
        })
    },
    grid: function(a, e, h, g) {
    	var o = {};
    	a.callEvent('setSubGridOptions', [o, h.parentNode.idd, h._cellIndex, g]);
    	if(!o.SUB_GRID_ID){
    		console.log('not found sub_grid option');
    		return;
    	}
        h._sub_grid = new SepoaDhtmlXGridObject(e, h.id.replace('c', a.grid_id), Sepoa.cache.grid.get(o.SUB_GRID_ID));
        if (a.skin_name) {
            h._sub_grid.setSkin(a.skin_name)
        }
        h._sub_grid.parentGrid = a;
        h._sub_grid.parentRowId = h.parentNode.idd;
        h._sub_grid.imgURL = a.imgURL;
        h._sub_grid.iconURL = a.iconURL;
        h._sub_grid.enableAutoHeight(true);
        h._sub_grid._delta_x = h._sub_grid._delta_y = null;
        h._sub_grid.attachEvent("onGridReconstructed", function() {
            a._detectHeight(e, h, h._sub_grid.objBox.scrollHeight + h._sub_grid.hdr.offsetHeight + (this.ftr ? this.ftr.offsetHeight : 0));
            a._correctMonolite();
            this.setSizes();
            if (a.parentGrid) {
                a.callEvent("onGridReconstructed", [])
            }
        });
        
        h._sub_grid._onDnDFlag=false;
		h._sub_grid.setGridDraw();
		h._sub_grid.setSizes();
		h._sub_grid.setDataOutput(a.dataOutput)
		
		if (!a.callEvent("onSubGridCreated", [h._sub_grid, h.parentNode.idd, h._cellIndex, g])) {
            h._sub_grid.objBox.style.overflow = "hidden";
            h._sub_row_type = null
        } else {
			h._sub_grid.attachEvent('onXLE', function(){
				let subHeight = h._sub_grid.objBox.scrollHeight + h._sub_grid.hdr.offsetHeight + (h._sub_grid.ftr ? h._sub_grid.ftr.offsetHeight : 0);
				if(h._sub_grid.objBox.scrollWidth > h._sub_grid.objBox.offsetWidth){
					subHeight += 15;				
				}
				a._detectHeight(e, h, subHeight);
	            h._sub_grid.objBox.style.overflow = "auto";
	            h._sub_grid.objBox.style.height = "auto";
	            a._correctMonolite();
	            h._sub_row_type = null;
				if (!a.callEvent("onSubGridLoaded", [h._sub_grid, h.parentNode.idd, h._cellIndex, g])) {
		            return
		        }
				if (a._ahgr) {
		            a.setSizes()
		        }
				if (a.parentGrid) {                       
				    a.callEvent("onGridReconstructed", [])
				}                                         
			})
        }
    }
};
SepoaDhtmlXGridObject.prototype._prepareRow=function(a) {
    if (!this._master_row) {
        this._build_master_row()
    }
    var e = this._master_row.cloneNode(true);
    for (var c = 0; c < e.childNodes.length; c++) {
        e.childNodes[c]._cellIndex = c;
        if (this._enbCid) {
            e.childNodes[c].id = "c_" + a + "_" + c
        }
        if (this.dragAndDropOff) {
            this.dragger.addDraggableItem(e.childNodes[c], this)
        }
        if(["comboro", "comboro2"].indexOf(this.getColType(c)) + 1){
        	e.childNodes[c].classList.add("sepoa_grid_select");
        }
    }
    e.idd = a;
    e.grid = this;
    return e
}
SepoaDhtmlXGridObject.prototype.setDataOutput=function(obj){
	this.dataOutput = obj;
}
SepoaDhtmlXGridObject.prototype.forbidden=function(){
	// not used function
}
SepoaDhtmlXGridObject.prototype.saveSizeToCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.saveHiddenColumnsToCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.loadHiddenColumnsFromCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.saveSortingToCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.saveOrderToCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.loadOrderFromCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.loadSizeFromCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.clearConfigCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.clearSizeCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.setCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype.getCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype._getCookie=function(){
	this.forbidden();
}
SepoaDhtmlXGridObject.prototype._update_srnd_view = function() {
    var c = this._get_view_size();
    var l = Math.floor(this.objBox.scrollTop / this._srdh);
    c += l;
    
    if (this.multiLine) {
        var m = this.objBox.scrollTop;
        l = 0;
        while (m > 0) {
            m -= this.rowsCol[l] ? this.rowsCol[l].offsetHeight : this._srdh;
            l++
        }
        c = l + this._get_view_size();
        if (l > 0) {
            l--
        }
    }
    c += (this._srnd_pr || 0);
    if (c > this.rowsBuffer.length) {
        c = this.rowsBuffer.length
    }
    for (var g = l; g < c; g++) {
        if (!this.rowsCol[g]) {
            var h = this._add_from_buffer(g);
            if (h == -1) {
                if (this.xmlFileUrl && this.xmlFileParam) {
                    if (this._dpref && this.rowsBuffer[c - 1]) {
                        var e = this._dpref ? this._dpref : (c - g);
                        var a = Math.max(0, Math.min(g, c - this._dpref));
                        this._current_load = [a, c - a]
                    } else {
                        this._current_load = [g, (this._dpref ? this._dpref : (c - g))]
                    }
                    if (this.callEvent("onDynXLS", this._current_load)) {
                    	var that = this;
                    	var url = that.xmlFileUrl;
                    	var param = that.xmlFileParam;
                    	
                    	that.xmlFileUrl=null;
            			that.xmlFileParam=null;
                    	var type = "xml";
                    	var xmlLoader = function(xml){
                    		if (!that.callEvent) return;
                    		that["_process_"+type](xml.xmlDoc);
                    		if (!that._contextCallTimer)
                    			that.callEvent("onXLE", [that,0, 0,xml.xmlDoc]);
                    		if (!that.xmlFileUrl){
                    			that.xmlFileUrl=url;
                    			that.xmlFileParam=param;
                    		}
                    	};
                    	
                    	var post = $.extend(true, [], param);
                    	post.push("posStart="+this._current_load[0]);
                    	post.push("count="+this._current_load[1]);
                    	post = post.join('&');
                    	
                    	this.callEvent("onXLS", [this]);
                    	dhx4.ajax.post(url, post, xmlLoader);
                    }
                }
                return;
            } else {
                if (this._tgle) {
                    this._updateLine(this._h2.get[this.rowsBuffer[g].idd], this.rowsBuffer[g]);
                    this._updateParentLine(this._h2.get[this.rowsBuffer[g].idd], this.rowsBuffer[g])
                }
                if (g && g == (this._realfake ? this._fake : this)["_r_select"]) {
                    this.selectCell(g, this.cell ? this.cell._cellIndex : 0, true)
                }
            }
        }
    }
    if (this._fake && !this._realfake && this.multiLine) {
        this._fake.objBox.scrollTop = this.objBox.scrollTop
    }
};
SepoaDhtmlXGridObject.prototype._get_view_size = function() {
	if(this.rowsBuffer.length > 0 && this.rowsBuffer[0].offsetHeight){
		if(this.rowsBuffer.length > 1 && this.rowsBuffer[1].offsetHeight){
			this._srdh = this.rowsBuffer[1].offsetHeight
		}else{
			this._srdh = this.rowsBuffer[0].offsetHeight
		}
	}
	if(this._fake && !this._realfake){
		this._fake._srdh = this._srdh;
	}
    return Math.floor(parseInt(this.entBox.offsetHeight) / this._srdh) + 10
};
SepoaDhtmlXGridObject.prototype.setAppendHeader=function(headerIDS){
	if(!headerIDS) return;
	
	var spliter = headerIDS.split(';');
	
	for(var idx in spliter) {
		var gridID = spliter[idx];
		
		if(gridID) {
			var header = [];
			var gridData = Sepoa.cache.grid.get(gridID);
			var mainGridData = Sepoa.cache.grid.get(this.SCREEN_ID);

			if(this.columnIds.length != Object.keys(gridData).length) {
				console.warn("'" + gridID + "' Column count not matched by '" + this.SCREEN_ID + "'");
				continue;
			}
			
			for(var i in mainGridData) {
				for(var idx in gridData) {
					if(gridData[idx].CODE == mainGridData[i].CODE) {
						var obj = {
							CODE: gridData[idx].CODE,
							CONTENTS: gridData[idx].CONTENTS,
							SEQ: parseFloat(mainGridData[i].COL_SEQ)
						};
						header.push(obj);
						break;
					}
				}
			}
			
			header.sort(function(a, b) {
				if(a.SEQ > b.SEQ) {
					return 1;
				}
				if(a.SEQ < b.SEQ) {
					return -1;
				}
				
				return 0;
			});
			
			if(header.length > 0) {
				var sortedHeader = [];

				this.columnIds.forEach(function(element, idx, array) {
					for(var i in header) {
						if(header[i].CODE == element) {
							if(header[i].CONTENTS == '#NO_LABEL#') {
								sortedHeader.push('');
							} else {
								sortedHeader.push(header[i].CONTENTS);
							}
							break;
						}
					}
				});
				this.attachHeader(sortedHeader);
				
				for(var i in header) {
					if(header[i].CONTENTS == '#rspan') {
						if(this.hdr.rows[0].cells[i].rowSpan) {
							this.hdr.rows[0].cells[i].rowSpan = 1;
						}
					}
				}
			}
		}
	}
}
SepoaDhtmlXGridObject.prototype.getAllDataObject=function(){
	var result = [];
	this.forEachRow(function(id){
		result.push(this.getRowDataObject(id));
	})
	return result;
}
SepoaDhtmlXGridObject.prototype.moveColumn = function(src, dest) {
	this.editStop();
	
	src = parseInt(src);
    dest = parseInt(dest);
    
    const srcArray = [src];
    if(this.hdrLabels[src] == "#cspan"){
    	for(let i=src-1;i>0;i--){
    		srcArray.splice(0, 0, i);
	    	if(this.hdrLabels[i] != "#cspan"){
	    		break;
	    	}
	    }
    	
    	for(let i = src+1; i<this.hdrLabels.length; i++){
	    	if(this.hdrLabels[i] == "#cspan"){
	    		srcArray.push(i);
	    	} else {
	    		break;
	    	}
	    }
    }
    
    if(this.hdrLabels[dest] == "#cspan"){
    	for(let i = dest+1; i<this.hdrLabels.length; i++){
    		dest = i;
	    	if(this.hdrLabels[i] != "#cspan"){
	    		break;
	    	}
	    }
    }
    
    if(srcArray.includes(dest)){
    	return;
    }
    
    if (!this.callEvent("onBeforeCMove", [srcArray, dest])) {
        return false
    }
    
    for(var i=0;i<srcArray.length;i++){
    	const adjust = (srcArray[0] >= dest)?i:0;
    	if(i != 0){
    		this._moveColInRowHdrBlock = true;
    	}
    	
    	this._processAllRows("_moveColInRow", srcArray[0] + adjust, dest + adjust);
        this._processAllArrays(srcArray[0] + adjust, dest + adjust - 1);
    }
    
    if (this.fldSorted) {
        this.setSortImgPos(this.fldSorted._cellIndex)
    }
    this.callEvent("onAfterCMove", [srcArray, dest]);
    if (this.hdr.rows[1]._childIndexes) {
    	this.hdr.rows[1]._childIndexes = [];
    	
    	let childIndex = -1;
    	this.hdrLabels.forEach((label)=>{
    		if(label != "#cspan"){
    			childIndex++;
    		}
    		this.hdr.rows[1]._childIndexes.push(childIndex);
    	})
	}
    this._moveColInRowHdrBlock = false;
};
SepoaDhtmlXGridObject.prototype._moveColInRow = function(l, a, e, i, t) {
	if(t == "hdr"){
		if(this._moveColInRowHdrBlock){
			return;
		}
		for(var i=0;i<l.childNodes.length;i++){
			var node = l.childNodes[i];
			var colSpan = node.getAttribute('colspan');
			if(colSpan){
				let breakFlag = false;
				if(a > i){
					a -= (colSpan - 1);
				}else{
					breakFlag = true;
				}
				if(e > i){
					e -= (colSpan - 1);
				}else if(breakFlag){
					break;
				}
			}
		}
	}
	
	var m = l.childNodes[a];
    var h = l.childNodes[e];
    
    if (!m) {
        return
    }
    if (h) {
        l.insertBefore(m, h)
    } else {
        l.appendChild(m)
    }
    let spanAdjust = 0;
    for (var g = 0; g < l.childNodes.length; g++) {
    	let spanValue = l.childNodes[g].getAttribute("colspan");
    	if(spanValue && spanValue > 1){
    		spanAdjust += (spanValue - 1);
    	}
        l.childNodes[g]._cellIndex = l.childNodes[g]._cellIndexS = g+spanAdjust;
    }
};
SepoaDhtmlXGridObject.prototype._processAllRows = function(h, a, c) {
    this[h](this.obj.rows[0], a, c, 0, "th");
    this[h](this.hdr.rows[0], a, c, e, "th");
    var g = this.hdr.rows.length;
    for (var e = 1; e < g; e++) {
        this[h](this.hdr.rows[e], a, c, e, "hdr")
    }
    if (this.ftr) {
        var g = this.ftr.firstChild.rows.length;
        for (var e = 0; e < g; e++) {
            this[h](this.ftr.firstChild.rows[e], a, c, e, "ftr")
        }
    }
    this.forEachRow(function(l) {
        if (this.rowsAr[l] && this.rowsAr[l].tagName == "TR") {
            this[h](this.rowsAr[l], a, c, -1, "tr")
        }
    })
};
SepoaDhtmlXGridObject.prototype.unSplitAt = function() {
	const fakeBox = this._fake.entBox;
    this._fake.destructor();
    delete this._fake;
    this.globalBox.removeChild(fakeBox);
    
    while(this.entBox.children.length > 0){
    	this.globalBox.appendChild(this.entBox.children[0]);
    }
    this.entBox.parentElement.removeChild(this.entBox);
    
    this.entBox = this.globalBox;
    this.globalBox = null;
    
    (this._fakeInfo.events || []).forEach(ev=>this.detachEvent(ev));
    Object.keys(this).filter((fn)=>{
    	let type = null;
    	if(fn.startsWith('_bfs_')){
    		type = "B";
    	}else if(fn.endsWith('A')){
    		type = "A"
    	}else if(fn.endsWith('_s')){
    		type = "S"
    	}
    	if(type){
    		let reTxt = null;
    		switch(type){
    		case "A":
    			reTxt = "A$"; 
    			break;
    		case "B":
    			reTxt = "^_bfs_";
    			break;
    		case "S":
    			reTxt = "_s$";
    			break;
    		}
    		const orgFn = fn.replace(new RegExp(reTxt, "g"), '');
    		if(orgFn){
    			this[orgFn] = this[fn];
    			delete this[fn];
    		}
    	}
    });
    for(let i=0;i<this._fakeInfo.idx;i++){
    	this.setColumnHidden(i, false);
    }
    
    delete this._fakeInfo;
    
    if(this.getHeaderFilterIdx() >= 0){
    	this.disableHeaderFilter();
    	this.enableHeaderFilter();
    }
};
SepoaDhtmlXGridObject.prototype.splitAt = function(m) {
	if(this._fake){
		this.unSplitAt();
	}
	
    if (!this.obj.rows[0]) {
        return this._split_later = m
    }
    m = parseInt(m);
    var x = document.createElement("DIV");
    this.entBox.appendChild(x);
    var y = document.createElement("DIV");
    this.entBox.appendChild(y);
    for (var v = this.entBox.childNodes.length - 3; v >= 0; v--) {
        y.insertBefore(this.entBox.childNodes[v], y.firstChild)
    }
    this.entBox.style.position = "relative";
    this.globalBox = this.entBox;
    this.entBox = y;
    y.grid = this;
    x.style.cssText += "border:0px solid red !important;";
    y.style.cssText += "border:0px solid red !important;";
    y.style.top = "0px";
    y.style.position = "absolute";
    x.style.position = "absolute";
    x.style.top = "0px";
    x.style.left = "0px";
    x.style.zIndex = 11;
    y.style.height = x.style.height = this.globalBox.clientHeight;
    this._fake = new SepoaDhtmlXGridObject(x);
    this._fake.destructor=function() {
        this.editStop(true);
        if (this._sizeTime) {
            this._sizeTime = window.clearTimeout(this._sizeTime)
        }
        this.entBox.className = (this.entBox.className || "").replace(/gridbox.*/, "");
        if (this.formInputs) {
            for (var e = 0; e < this.formInputs.length; e++) {
                this.parentForm.removeChild(this.formInputs[e])
            }
        }
        var c;
        for (var e = 0; e < this.rowsCol.length; e++) {
            if (this.rowsCol[e]) {
                this.rowsCol[e].grid = null
            }
        }
        for (e in this.rowsAr) {
            if (this.rowsAr[e]) {
                this.rowsAr[e] = null
            }
        }
        this.rowsCol = new dhtmlxArray();
        this.rowsAr = {};
        this.entBox.innerHTML = "";
        var g = function() {};
        this.entBox.onclick = this.entBox.onmousedown = this.entBox.onbeforeactivate = this.entBox.onbeforedeactivate = this.entBox.onbeforedeactivate = this.entBox.onselectstart = g;
        this.setSizes = this._update_srnd_view = this.callEvent = g;
        this.entBox.grid = this.objBox.grid = this.hdrBox.grid = this.obj.grid = this.hdr.grid = null;
        for (c in this) {
            if ((this[c]) && (this[c].m_obj)) {
                this[c].m_obj = null
            }
            this[c] = null
        }
        if (this == globalActiveDHTMLGridObject) {
            globalActiveDHTMLGridObject = null
        }
        return null
    }
    this.globalBox = this._fake.globalBox = this.globalBox;
    this._fake._fake = this;
    this._fake._realfake = true;
    this._treeC = this.cellType._dhx_find("tree");
    this._fake.delim = this.delim;
    this._fake.customGroupFormat = this.customGroupFormat;
    this._fake.setImagesPath(this._imgURL);
    this._fake.iconURL = this.iconURL;
    this._fake._customSorts = this._customSorts;
    this._fake.noHeader = this.noHeader;
    this._fake._enbTts = this._enbTts;
    this._fake._drsclmW = this._drsclmW;
    this._fake._htkebl = this._htkebl;
    this._fake.clists = this.clists;
    this._fake.fldSort = new Array();
    this._fake.selMultiRows = this.selMultiRows;
    this._fake.multiLine = this.multiLine;
    this._fake._key_events = this._key_events;
    this._fake.smartTabOrder = this.smartTabOrder;
    this._fake._RaSeCol = this._RaSeCol;
    this._fakeInfo = {
    	events:[],
    	idx:m,
    }
    if (this.multiLine || this._erspan) {
    	this._fakeInfo.events.push(this.attachEvent("onCellChanged", this._correctRowHeight));
    	this._fakeInfo.events.push(this.attachEvent("onRowAdded", this._correctRowHeight));
        var c = function() {
            this.forEachRow(function(D) {
                this._correctRowHeight(D)
            })
        };
        this._fakeInfo.events.push(this.attachEvent("onPageChanged", c));
        this._fakeInfo.events.push(this.attachEvent("onXLE", c));
        this._fakeInfo.events.push(this.attachEvent("onResizeEnd", c));
        if (!this._ads_count) {
        	this._fakeInfo.events.push(this.attachEvent("onAfterSorting", c));
        }
        if (this._srnd) {
        	this._fakeInfo.events.push(this.attachEvent("onFilterEnd", c));
        }
        this._fakeInfo.events.push(this.attachEvent("onDistributedEnd", c));
    }
    this._fakeInfo.events.push(this.attachEvent("onGridReconstructed", function() {
        this._fake.objBox.scrollTop = this.objBox.scrollTop
    }));
    this._fake.loadedKidsHash = this.loadedKidsHash;
    if (this._h2) {
        this._fake._h2 = this._h2
    }
    this._fake._dInc = this._dInc;
    var n = [
        [],
        [],
        [],
        [],
        [],
        [],
        []
    ];
    var s = ["hdrLabels", "initCellWidth", "cellType", "cellAlign", "cellVAlign", "fldSort", "columnColor"];
    var h = ["setHeader", "setInitWidths", "setColTypes", "setColAlign", "setColVAlign", "setColSorting", "setColumnColor"];
    this._fake.callEvent = function() {
        var D = true;
        this._fake._split_event = true;
        var E = (arguments[0] == "onScroll");
        if (arguments[0] == "onGridReconstructed" || E) {
            this._fake.callEvent.apply(this, arguments)
        }
        if (!E) {
            D = this._fake.callEvent.apply(this._fake, arguments)
        }
        this._fake._split_event = false;
        return D
    };
    if (this._elmn) {
        this._fake.enableLightMouseNavigation(true)
    }
    if (this._cssEven || this._cssUnEven) {
    	this._fakeInfo.events.push(this._fake.attachEvent("onGridReconstructed", function() {
            this._fixAlterCss()
        }));
    }
    this._fake._cssSP = this._cssSP;
    this._fake.isEditable = this.isEditable;
    this._fake._edtc = this._edtc;
    if (this._sst) {
        this._fake.enableStableSorting(true)
    }
    this._fake._sclE = this._sclE;
    this._fake._dclE = this._dclE;
    this._fake._f2kE = this._f2kE;
    this._fake._maskArr = this._maskArr;
    this._fake._dtmask = this._dtmask;
    this._fake.combos = this.combos;
    var o = 0;
    var a = this.globalBox.offsetWidth;
    for (var v = 0; v < m; v++) {
        for (var u = 0; u < s.length; u++) {
            if (this[s[u]]) {
                n[u][v] = this[s[u]][v]
            }
            if (typeof n[u][v] == "string") {
                n[u][v] = n[u][v].replace(new RegExp("\\" + this.delim, "g"), "\\" + this.delim)
            }
        }
        if (_isFF) {
            n[1][v] = n[1][v] * 1
        }
        if (this.cellWidthType == "%") {
            n[1][v] = Math.round(parseInt(this[s[1]][v]) * a / 100);
            o += n[1][v]
        } else {
            o += parseInt(this[s[1]][v])
        }
        this.setColumnHidden(v, true)
    }
    for (var u = 0; u < s.length; u++) {
        var r = n[u].join(this.delim);
        if (h[u] != "setHeader") {
            if (r != "") {
                this._fake[h[u]](r)
            }
        } else {
            this._fake[h[u]](r, null, this._hstyles)
        }
    }
    this._fake._strangeParams = this._strangeParams;
    this._fake._drsclmn = this._drsclmn;
    o = Math.min(this.globalBox.offsetWidth, o);
    if(this.globalBox.offsetWidth - o < 300){
    	o = this.globalBox.offsetWidth - 300;
    }
    y.style.left = o + "px";
    x.style.width = o + "px";
    y.style.width = Math.max(this.globalBox.offsetWidth - o, 0);
    if (this._ecspn) {
        this._fake._ecspn = true
    }
    this._fake.init();
    if (this.dragAndDropOff) {
        this.dragger.addDragLanding(this._fake.entBox, this)
    }
    this._fake.objBox.style.overflow = "hidden";
    if (!dhtmlx.$customScroll) {
        this._fake.objBox.style.overflowX = "scroll"
    } else {
        this._fake.objBox._custom_scroll_mode = ""
    }
    this._fake._srdh = this._srdh || 20;
    this._fake._srnd = this._srnd;
    this._fake._cssEven = this._cssEven;
    this._fake._cssUnEven = this._cssUnEven;
    if (this.skin_name != this._fake.skin_name) {
        this._fake.setSkin(this.skin_name)
    }
    this._fake._srdh = this._srdh;
    var A = this;

    function C(F) {
        var D = A.objBox;
        if (D.scrollHeight - D.offsetHeight > 2) {
            var E = F.wheelDelta / -40;
            if (F.wheelDelta === window.undefined) {
                E = F.detail
            }
            D.scrollTop += E * 40;
            if (F.preventDefault) {
                F.preventDefault()
            }
        }
    }
    dhtmlxEvent(this._fake.objBox, "mousewheel", C);
    dhtmlxEvent(this._fake.objBox, "DOMMouseScroll", C);

    function g(E, D) {
        D.style.whiteSpace = "";
        var I = D.nextSibling;
        var F = D.parentNode;
        E.parentNode.insertBefore(D, E);
        if (!I) {
            F.appendChild(E)
        } else {
            F.insertBefore(E, I)
        }
        var H = E.style.display;
        E.style.display = D.style.display;
        D.style.display = H
    }

    function w(L, T, M, E) {
        var F = (new Array(m)).join(this.delim);
        var N = [];
        if (L == 2) {
            for (var J = 0; J < m; J++) {
                var D = T[L - 1].cells[T[L - 1]._childIndexes ? T[L - 1]._childIndexes[J] : J];
                if (D.rowSpan && D.rowSpan > 1) {
                    N[D._cellIndex] = D.rowSpan - 1;
                    E[L - 1].cells[E[L - 1]._childIndexes ? E[L - 1]._childIndexes[J] : J].rowSpan = D.rowSpan;
                    D.rowSpan = 1
                }
            }
        }
        for (L; L < T.length; L++) {
            this._fake.attachHeader(F, null, M);
            E = E || this._fake.ftr.childNodes[0].rows;
            var Q = m;
            var H = 0;
            for (var K = 0; K < Q; K++) {
                if (N[K]) {
                    N[K] = N[K] - 1;
                    if (_isIE || _isOpera) {
                        var I = document.createElement("TD");
                        if (_isFF) {
                            I.style.display = "none"
                        }
                        T[L].insertBefore(I, T[L].cells[0])
                    }
                    H++;
                    continue
                }
                var P = E[L].cells[K - H];
                var O = T[L].cells[K - (_isIE ? 0 : H)];
                var S = O.rowSpan;
                g(P, O);
                if (S > 1) {
                    N[K] = S - 1;
                    O.rowSpan = S
                }
                if (E[L].cells[K].colSpan > 1) {
                    T[L].cells[K].colSpan = E[L].cells[K].colSpan;
                    Q -= E[L].cells[K].colSpan - 1;
                    for (var J = 1; J < E[L].cells[K].colSpan; J++) {
                        E[L].removeChild(E[L].cells[K + 1])
                    }
                }
            }
        }
    }
    if (this.hdr.rows.length > 2) {
        w.call(this, 2, this.hdr.rows, "_aHead", this._fake.hdr.rows)
    }
    if (this.ftr) {
        w.call(this, 1, this.ftr.childNodes[0].rows, "_aFoot");
        this._fake.ftr.parentNode.style.bottom = (_isFF ? 2 : 1) + "px"
    }
    if (this.saveSizeToCookie) {
        this.saveSizeToCookie = function(E, D) {
            if (this._realfake) {
                return this._fake.saveSizeToCookie.apply(this._fake, arguments)
            }
            if (!E) {
                E = this.entBox.id
            }
            var H = new Array();
            var I = "cellWidthPX";
            for (var F = 0; F < this[I].length; F++) {
                if (F < m) {
                    H[F] = this._fake[I][F]
                } else {
                    H[F] = this[I][F]
                }
            }
            H = H.join(",");
            this.setCookie(E, D, 0, H);
            var H = (this.initCellWidth || (new Array)).join(",");
            this.setCookie(E, D, 1, H);
            return true
        };
        this.loadSizeFromCookie = function(D) {
            if (!D) {
                D = this.entBox.id
            }
            var I = this._getCookie(D, 1);
            if (!I) {
                return
            }
            this.initCellWidth = I.split(",");
            var I = this._getCookie(D, 0);
            var J = "cellWidthPX";
            this.cellWidthType = "px";
            var F = 0;
            if ((I) && (I.length)) {
                I = I.split(",");
                for (var E = 0; E < I.length; E++) {
                    if (E < m) {
                        this._fake[J][E] = I[E];
                        F += I[E] * 1
                    } else {
                        this[J][E] = I[E]
                    }
                }
            }
            this._fake.entBox.style.width = F + "px";
            this._fake.objBox.style.width = F + "px";
            var H = this.globalBox.childNodes[1];
            H.style.left = F - (_isFF ? 0 : 0) + "px";
            if (this.ftr) {
                this.ftr.style.left = F - (_isFF ? 0 : 0) + "px"
            }
            H.style.width = this.globalBox.offsetWidth - F + "px";
            this.setSizes();
            return true
        };
        this._fake.onRSE = this.onRSE
    }
    this.setCellTextStyleA = this.setCellTextStyle;
    this.setCellTextStyle = function(E, F, D) {
        if (F < m) {
            this._fake.setCellTextStyle(E, F, D)
        }
        this.setCellTextStyleA(E, F, D)
    };
    this.setRowTextBoldA = this.setRowTextBold;
    this.setRowTextBold = function(D) {
        this.setRowTextBoldA(D);
        this._fake.setRowTextBold(D)
    };
    this.setRowColorA = this.setRowColor;
    this.setRowColor = function(E, D) {
        this.setRowColorA(E, D);
        this._fake.setRowColor(E, D)
    };
    this.setRowHiddenA = this.setRowHidden;
    this.setRowHidden = function(E, D) {
        this.setRowHiddenA(E, D);
        this._fake.setRowHidden(E, D)
    };
    this.setRowTextNormalA = this.setRowTextNormal;
    this.setRowTextNormal = function(D) {
        this.setRowTextNormalA(D);
        this._fake.setRowTextNormal(D)
    };
    this.getChangedRowsA = this.getChangedRows; 
    this.getChangedRows = function(F) {
        var D = new Array();

        function E(I) {
            for (var H = 0; H < I.childNodes.length; H++) {
                if (I.childNodes[H].wasChanged) {
                    return D[D.length] = I.idd
                }
            }
        }
        this.forEachRow(function(J) {
            var I = this.rowsAr[J];
            var H = this._fake.rowsAr[J];
            if (I.tagName != "TR" || !H || H.tagName != "TR") {
                return
            }
            if (F && I._added) {
                D[D.length] = I.idd
            } else {
                if (!E(I)) {
                    E(H)
                }
            }
        });
        return D.join(this.delim)
    };
    this.setRowTextStyleA = this.setRowTextStyle;
    this.setRowTextStyle = function(E, D) {
        this.setRowTextStyleA(E, D);
        if (this._fake.rowsAr[E]) {
            this._fake.setRowTextStyle(E, D)
        }
    };
    this.lockRowA = this.lockRow;
    this.lockRow = function(E, D) {
        this.lockRowA(E, D);
        this._fake.lockRow(E, D)
    };
    this.getColWidthA = this.getColWidth;
    this.getColWidth = function(D) {
        if (D < m) {
            return parseInt(this._fake.cellWidthPX[D])
        } else {
            return parseInt(this.cellWidthPX[D])
        }
    };
    
    this.getColumnLabelA = this.getColumnLabel;
    this.getColumnLabel = function(D) {
        return this._fake.getColumnLabel.apply(((D < m) ? this._fake : this), arguments)
    };
    this.setColWidthA = this._fake.setColWidthA = this.setColWidth;
    this.setColWidth = function(D, E) {
        D = D * 1;
        if (D < m) {
            this._fake.setColWidthA(D, E)
        } else {
            this.setColWidthA(D, E)
        }
        if ((D + 1) <= m) {
            this._fake._correctSplit(Math.min(this._fake.objBox.offsetWidth, this._fake.obj.offsetWidth))
        }
    };
    this.setColumnLabelA = this.setColumnLabel;
    this.setColumnLabel = function(E, D, I, H) {
        var F = this;
        if (E < m) {
            F = this._fake
        }
        return this.setColumnLabelA.apply(F, [E, D, I, H])
    };
    this.adjustColumnSizeA = this.adjustColumnSize;
    this.adjustColumnSize = function(D, E) {
        if (D < m) {
            if (_isIE) {
                this._fake.obj.style.tableLayout = ""
            }
            this._fake.adjustColumnSize(D, E);
            if (_isIE) {
                this._fake.obj.style.tableLayout = "fixed"
            }
            this._fake._correctSplit()
        } else {
            return this.adjustColumnSizeA(D, E)
        }
    };
    var e = "cells";
    this._bfs_cells = this[e];
    this[e] = function() {
        if (arguments[1] < m) {
            return this._fake.cells.apply(this._fake, arguments)
        } else {
            return this._bfs_cells.apply(this, arguments)
        }
    };
    this._bfs_isColumnHidden = this.isColumnHidden;
    this.isColumnHidden = function() {
        if (parseInt(arguments[0]) < m) {
            return this._fake.isColumnHidden.apply(this._fake, arguments)
        } else {
            return this._bfs_isColumnHidden.apply(this, arguments)
        }
    };
    this._bfs_setColumnHidden = this.setColumnHidden;
    this.setColumnHidden = function() {
        if (parseInt(arguments[0]) < m) {
            this._fake.setColumnHidden.apply(this._fake, arguments);
            return this._fake._correctSplit()
        } else {
            return this._bfs_setColumnHidden.apply(this, arguments)
        }
    };
    var e = "cells2";
    this._bfs_cells2 = this[e];
    this[e] = function() {
        if (arguments[1] < m) {
            return this._fake.cells2.apply(this._fake, arguments)
        } else {
            return this._bfs_cells2.apply(this, arguments)
        }
    };
    var e = "cells3";
    this._bfs_cells3 = this[e];
    this[e] = function(E, D) {
        if (arguments[1] < m && this._fake.rowsAr[arguments[0].idd]) {
            if (this._fake.rowsAr[E.idd] && this._fake.rowsAr[E.idd].childNodes.length == 0) {
                return this._bfs_cells3.apply(this, arguments)
            }
            arguments[0] = arguments[0].idd;
            return this._fake.cells.apply(this._fake, arguments)
        } else {
            return this._bfs_cells3.apply(this, arguments)
        }
    };
    var e = "changeRowId";
    this._bfs_changeRowId = this[e];
    this[e] = function() {
        this._bfs_changeRowId.apply(this, arguments);
        if (this._fake.rowsAr[arguments[0]]) {
            this._fake.changeRowId.apply(this._fake, arguments)
        }
    };
    this._fake.getRowById = function(F) {
        var E = this.rowsAr[F];
        if (!E && this._fake.rowsAr[F]) {
            E = this._fake.getRowById(F)
        }
        if (E) {
            if (E.tagName != "TR") {
                for (var D = 0; D < this.rowsBuffer.length; D++) {
                    if (this.rowsBuffer[D] && this.rowsBuffer[D].idd == F) {
                        return this.render_row(D)
                    }
                }
                if (this._h2) {
                    return this.render_row(null, E.idd)
                }
            }
            return E
        }
        return null
    };
    if (this.collapseKids) {
        this._fake._bfs_collapseKids = this.collapseKids;
        this._fake.collapseKids = function() {
            return this._fake.collapseKids.apply(this._fake, [this._fake.rowsAr[arguments[0].idd]])
        };
        this["_bfs_collapseKids"] = this.collapseKids;
        this["collapseKids"] = function() {
            var D = this["_bfs_collapseKids"].apply(this, arguments);
            this._fake._h2syncModel();
            if (!this._cssSP) {
                this._fake._fixAlterCss()
            }
        };
        this._fake._bfs_expandKids = this.expandKids;
        this._fake.expandKids = function() {
            this._fake.expandKids.apply(this._fake, [this._fake.rowsAr[arguments[0].idd]]);
            if (!this._cssSP) {
                this._fake._fixAlterCss()
            }
        };
        this["_bfs_expandAll"] = this.expandAll;
        this["expandAll"] = function() {
            this._bfs_expandAll();
            this._fake._h2syncModel();
            if (!this._cssSP) {
                this._fake._fixAlterCss()
            }
        };
        this["_bfs_collapseAll"] = this.collapseAll;
        this["collapseAll"] = function() {
            this._bfs_collapseAll();
            this._fake._h2syncModel();
            if (!this._cssSP) {
                this._fake._fixAlterCss()
            }
        };
        this["_bfs_expandKids"] = this.expandKids;
        this["expandKids"] = function() {
            var D = this["_bfs_expandKids"].apply(this, arguments);
            this._fake._h2syncModel();
            if (!this._cssSP) {
                this._fake._fixAlterCss()
            }
        };
        this._fake._h2syncModel = function() {
            if (this._fake.pagingOn) {
                this._fake._renderSort()
            } else {
                this._renderSort()
            }
        };
        this._updateTGRState = function(D) {
            return this._fake._updateTGRState(D)
        }
    }
    if (this._elmnh) {
        this._setRowHoverA = this._fake._setRowHoverA = this._setRowHover;
        this._unsetRowHoverA = this._fake._unsetRowHoverA = this._unsetRowHover;
        this._setRowHover = this._fake._setRowHover = function() {
            var D = this.grid;
            D._setRowHoverA.apply(this, arguments);
            var E = (_isIE ? event.srcElement : arguments[0].target);
            E = D._fake.rowsAr[D.getFirstParentOfType(E, "TD").parentNode.idd];
            if (E) {
                D._fake._setRowHoverA.apply(D._fake.obj, [{
                    target: E.childNodes[0]
                }, arguments[1]])
            }
        };
        this._unsetRowHover = this._fake._unsetRowHover = function() {
            if (arguments[1]) {
                var D = this
            } else {
                var D = this.grid
            }
            D._unsetRowHoverA.apply(this, arguments);
            D._fake._unsetRowHoverA.apply(D._fake.obj, arguments)
        };
        this._fake.enableRowsHover(true, this._hvrCss);
        this.enableRowsHover(false);
        this.enableRowsHover(true, this._fake._hvrCss)
    }
    this._updateTGRState = function(D) {
        if (!D.update || D.id == 0) {
            return
        }
        if (this.rowsAr[D.id].imgTag) {
            this.rowsAr[D.id].imgTag.src = this.iconTree + D.state + ".gif"
        }
        if (this._fake.rowsAr[D.id] && this._fake.rowsAr[D.id].imgTag) {
            this._fake.rowsAr[D.id].imgTag.src = this.iconTree + D.state + ".gif"
        }
        D.update = false
    };
    this.copy_row = function(I) {
        var D = I.cloneNode(true);
        D._skipInsert = I._skipInsert;
        var J = m;
        D._attrs = {};
        D._css = I._css;
        if (this._ecspn) {
            J = 0;
            for (var H = 0;
                (J < D.childNodes.length && H < m); H += (D.childNodes[J].colSpan || 1)) {
                J++
            }
        }
        while (D.childNodes.length > J) {
            D.removeChild(D.childNodes[D.childNodes.length - 1])
        }
        var F = J;
        for (var H = 0; H < F; H++) {
            if (this.dragAndDropOff) {
                this.dragger.addDraggableItem(D.childNodes[H], this)
            }
            D.childNodes[H].style.display = (this._fake._hrrar ? (this._fake._hrrar[H] ? "none" : "") : "");
            D.childNodes[H]._cellIndex = H;
            D.childNodes[H].combo_value = arguments[0].childNodes[H].combo_value;
            D.childNodes[H]._clearCell = arguments[0].childNodes[H]._clearCell;
            D.childNodes[H]._cellType = arguments[0].childNodes[H]._cellType;
            D.childNodes[H]._brval = arguments[0].childNodes[H]._brval;
            D.childNodes[H].val = arguments[0].childNodes[H].val;
            D.childNodes[H]._combo = arguments[0].childNodes[H]._combo;
            D.childNodes[H]._attrs = arguments[0].childNodes[H]._attrs;
            D.childNodes[H].chstate = arguments[0].childNodes[H].chstate;
            if (I._attrs.style) {
                D.childNodes[H].style.cssText = I._attrs.style + ";" + D.childNodes[H].style.cssText
            }
            if (D.childNodes[H].colSpan > 1) {
                D._childIndexes = arguments[0]._childIndexes
            }
        }
        if (this._h2 && this._treeC < m) {
            var E = this._h2.get[arguments[0].idd];
            D.imgTag = D.childNodes[this._treeC].childNodes[0].childNodes[E.level];
            D.valTag = D.childNodes[this._treeC].childNodes[0].childNodes[E.level + 2]
        }
        D.idd = I.idd;
        D.grid = this._fake;
        return D
    };
    var e = "_insertRowAt";
    this._bfs__insertRowAt = this[e];
    this[e] = function() {
        var E = this["_bfs__insertRowAt"].apply(this, arguments);
        arguments[0] = this.copy_row(arguments[0]);
        var D = this._fake._insertRowAt.apply(this._fake, arguments);
        if (E._fhd) {
            D.parentNode.removeChild(D);
            this._fake.rowsCol._dhx_removeAt(this._fake.rowsCol._dhx_find(D));
            E._fhd = false
        }
        return E
    };
    this._bfs_setSizes = this.setSizes;
    this.setSizes = function() {
    	if (this._notresize) {
            return
        }
        this._bfs_setSizes(this, arguments);
        this.sync_headers();
        if (this.sync_scroll() && this._ahgr) {
            this.setSizes()
        }
        var D = this.dontSetSizes ? (this.entBox.offsetHeight + "px") : this.entBox.style.height;
        this._fake.entBox.style.height = D;
        this._fake.objBox.style.height = this.objBox.style.height;
        this._fake.hdrBox.style.height = this.hdrBox.style.height;
        this._fake.objBox.scrollTop = this.objBox.scrollTop;
        this._fake.setColumnSizes(this._fake.entBox.clientWidth);
        this.globalBox.style.width = parseInt(this.entBox.style.width) + parseInt(this._fake.entBox.style.width);
        if (!this.dontSetSizes) {
            this.globalBox.style.height = D
        }
    };
    this.sync_scroll = this._fake.sync_scroll = function(E) {
        var D = this.objBox.style.overflowX;
        if (this.obj.offsetWidth <= this.objBox.offsetWidth) {
            if (!E) {
                return this._fake.sync_scroll(true)
            }
            this.objBox.style.overflowX = "hidden";
            this._fake.objBox.style.overflowX = "hidden"
        } else {
            if (!dhtmlx.$customScroll) {
                this.objBox.style.overflowX = "scroll";
                this._fake.objBox.style.overflowX = "scroll"
            }
        }
        return D != this.objBox.style.overflowX
    };
    this.sync_headers = this._fake.sync_headers = function() {
    	if (this.noHeader || (this._fake.hdr.scrollHeight == this.hdr.offsetHeight) || this.noHeaderResize) {
            return
        }
        for (var E = 1; E < this.hdr.rows.length; E++) {
            var I = m;
            while (!this.hdr.rows[E].childNodes[I]) {
                I--
            }
            var D = Math.min(this.hdr.rows[E].childNodes[I].scrollHeight + 2, this.hdr.rows[E].scrollHeight);
            var H = this._fake.hdr.rows[E].scrollHeight;
            if (D != H) {
                this._fake.hdr.rows[E].style.height = this.hdr.rows[E].style.height = Math.max(D, H) + "px"
            }
            if (window._KHTMLrv) {
                var F = 0;
                while (this._fake._hrrar[F]) {
                    F++
                }
                this._fake.hdr.rows[E].childNodes[F].style.height = this.hdr.rows[E].childNodes[I].style.height = Math.max(D, H) + "px"
            }
        }
        this._fake.sync_headers
    };
    this._fake._bfs_setSizes = this._fake.setSizes;
    this._fake.setSizes = function() {
        if (this._fake._notresize) {
            return
        }
        this._fake.setSizes()
    };
    var e = "_doOnScroll";
    this._bfs__doOnScroll = this[e];
    this[e] = function() {
        this._bfs__doOnScroll.apply(this, arguments);
        this._fake.objBox.scrollTop = this.objBox.scrollTop;
        this._fake._doOnScroll.apply(this._fake, arguments)
    };
    var e = "selectAll";
    this._bfs_selectAll = this[e];
    this[e] = function() {
        this._bfs_selectAll.apply(this, arguments);
        this._bfs_selectAll.apply(this._fake, arguments)
    };
    var e = "doClick";
    this._bfs_doClick = this[e];
    this[e] = function() {
        this["_bfs_doClick"].apply(this, arguments);
        if (arguments[0].tagName == "TD") {
            var D = (arguments[0]._cellIndex >= m);
            if (!arguments[0].parentNode.idd) {
                return
            }
            if (!D) {
                arguments[0].className = arguments[0].className.replace(/cellselected/g, "")
            }
            if (!this._fake.rowsAr[arguments[0].parentNode.idd]) {
                this._fake.render_row(this.getRowIndex(arguments[0].parentNode.idd))
            }
            arguments[0] = this._fake.cells(arguments[0].parentNode.idd, (D ? 0 : arguments[0]._cellIndex)).cell;
            if (D) {
                this._fake.cell = null
            }
            this._fake._bfs_doClick.apply(this._fake, arguments);
            if (D) {
                this._fake.cell = this.cell
            } else {
                this.cell = this._fake.cell
            }
            if (this._fake.onRowSelectTime) {
                clearTimeout(this._fake.onRowSelectTime)
            }
            if (D) {
                arguments[0].className = arguments[0].className.replace(/cellselected/g, "");
                globalActiveDHTMLGridObject = this;
                this._fake.cell = this.cell
            } else {
                this.objBox.scrollTop = this._fake.objBox.scrollTop
            }
        }
    };
    this._fake._bfs_doClick = this._fake[e];
    this._fake[e] = function() {
        this["_bfs_doClick"].apply(this, arguments);
        if (arguments[0].tagName == "TD") {
            var D = (arguments[0]._cellIndex < m);
            if (!arguments[0].parentNode.idd) {
                return
            }
            arguments[0] = this._fake._bfs_cells(arguments[0].parentNode.idd, (D ? m : arguments[0]._cellIndex)).cell;
            this._fake.cell = null;
            this._fake._bfs_doClick.apply(this._fake, arguments);
            this._fake.cell = this.cell;
            if (this._fake.onRowSelectTime) {
                clearTimeout(this._fake.onRowSelectTime)
            }
            if (D) {
                arguments[0].className = arguments[0].className.replace(/cellselected/g, "");
                globalActiveDHTMLGridObject = this;
                this._fake.cell = this.cell;
                this._fake.objBox.scrollTop = this.objBox.scrollTop
            }
        }
    };
    this.clearSelectionA = this.clearSelection;
    this.clearSelection = function(D) {
        if (D) {
            this._fake.clearSelection()
        }
        this.clearSelectionA()
    };
    this.moveRowUpA = this.moveRowUp;
    this.moveRowUp = function(D) {
        if (!this._h2) {
            this._fake.moveRowUp(D)
        }
        this.moveRowUpA(D);
        if (this._h2) {
            this._fake._h2syncModel()
        }
    };
    this.moveRowDownA = this.moveRowDown;
    this.moveRowDown = function(D) {
        if (!this._h2) {
            this._fake.moveRowDown(D)
        }
        this.moveRowDownA(D);
        if (this._h2) {
            this._fake._h2syncModel()
        }
    };
    this._fake.getUserData = function() {
        return this._fake.getUserData.apply(this._fake, arguments)
    };
    this._fake.setUserData = function() {
        return this._fake.setUserData.apply(this._fake, arguments)
    };
    this.getSortingStateA = this.getSortingState;
    this.getSortingState = function() {
        var D = this.getSortingStateA();
        if (D.length != 0) {
            return D
        }
        return this._fake.getSortingState()
    };
    this.setSortImgStateA = this._fake.setSortImgStateA = this.setSortImgState;
    this.setSortImgState = function(E, D, H, F) {
        this.setSortImgStateA(E, D, H, F);
        if (D * 1 < m) {
            this._fake.setSortImgStateA(E, D, H, F);
            this.setSortImgStateA(false)
        } else {
            this._fake.setSortImgStateA(false)
        }
    };
    this._fake.doColResizeA = this._fake.doColResize;
    this._fake.doColResize = function(I, H, F, D, K) {
        var E = -1;
        var J = 0;
        if (arguments[1]._cellIndex == (m - 1)) {
            E = this._initalSplR + (I.clientX - D);
            if (!this._initalSplF) {
                this._initalSplF = arguments[3] + this.objBox.scrollWidth - this.objBox.offsetWidth
            }
            if (this.objBox.scrollWidth == this.objBox.offsetWidth && (this._fake.alter_split_resize || (I.clientX - D) > 0)) {
                arguments[3] = (this._initalSplF || arguments[3]);
                J = this.doColResizeA.apply(this, arguments)
            } else {
                J = this.doColResizeA.apply(this, arguments)
            }
        } else {
            if (this.obj.offsetWidth < this.entBox.offsetWidth) {
                E = this.obj.offsetWidth
            }
            J = this.doColResizeA.apply(this, arguments)
        }
        if (J !== false) {
            this._correctSplit(E);
            this.resized = this._fake.resized = 1
        }
        return J
    };
    this._fake.changeCursorState = function(F) {
        var E = F.target || F.srcElement;
        if (E.tagName != "TD") {
            E = this.getFirstParentOfType(E, "TD")
        }
        if ((E.tagName == "TD") && (this._drsclmn) && (!this._drsclmn[E._cellIndex])) {
            return
        }
        var D = (F.layerX || 0) + (((!_isIE) && (F.target.tagName == "DIV")) ? E.offsetLeft : 0);
        var H = parseInt(this.getPosition(E, this.hdrBox));
        if (((E.offsetWidth - (F.offsetX || (H - D) * -1)) < (_isOpera ? 20 : 10)) || ((this.entBox.offsetWidth - (F.offsetX ? (F.offsetX + E.offsetLeft) : D) + this.objBox.scrollLeft - 0) < (_isOpera ? 20 : 10))) {
            E.style.cursor = "E-resize"
        } else {
            E.style.cursor = "default"
        }
        if (_isOpera) {
            this.hdrBox.scrollLeft = this.objBox.scrollLeft
        }
    };
    this._fake.startColResizeA = this._fake.startColResize;
    this._fake.startColResize = function(E) {
        var F = this.startColResizeA(E);
        this._initalSplR = this.entBox.offsetWidth;
        this._initalSplF = null;
        if (this.entBox.onmousemove) {
            var D = this.entBox.parentNode;
            if (D._aggrid) {
                return F
            }
            D._aggrid = D.grid;
            D.grid = this;
            this.entBox.parentNode.onmousemove = this.entBox.onmousemove;
            this.entBox.onmousemove = null
        }
        return F
    };
    this._fake.stopColResizeA = this._fake.stopColResize;
    this._fake.stopColResize = function(E) {
        if (this.entBox.parentNode.onmousemove) {
            var D = this.entBox.parentNode;
            D.grid = D._aggrid;
            D._aggrid = null;
            this.entBox.onmousemove = this.entBox.parentNode.onmousemove;
            this.entBox.parentNode.onmousemove = null;
            if (this.obj.offsetWidth < this.entBox.offsetWidth) {
                this._correctSplit(this.obj.offsetWidth)
            }
        }
        return this.stopColResizeA(E)
    };
    this.doKeyA = this.doKey;
    this._fake.doKeyA = this._fake.doKey;

    function l(K, J, D, M) {
        var E = M.shiftKey ? -1 : 1;
        var F = M.shiftKey ? -1 : D._cCount;
        var I = false;
        for (var H = K + E; H != F; H += E) {
            if (D.smartTabOrder) {
                I = D.cells2(J, H).isDisabled() ? false : H
            } else {
                I = H
            }
            if (I !== false) {
                var L = !D._key_events.k_other;
                D.selectCell(J, I, false, false, L, true);
                M.cancelBubble = true;
                if (M.preventDefault) {
                    M.preventDefault()
                }
                return true
            }
        }
    }
    this._fake.doKey = this.doKey = function(I) {
        if (!I) {
            return true
        }
        if (this._htkebl) {
            return true
        }
        if ((I.target || I.srcElement).value !== window.undefined) {
            var E = (I.target || I.srcElement);
            if ((!E.parentNode) || (E.parentNode.className.indexOf("editable") == -1)) {
                return true
            }
        }
        switch (I.keyCode) {
            case 9:
                var H = this._realfake ? this._fake : this;
                if (!H.callEvent("onTab", [true])) {
                    return true
                }
                if (this.cell) {
                    var F = this.cell._cellIndex;
                    var D = H.getRowIndex(this.cell.parentNode.idd);
                    while (!l(F, D, H, I)) {
                        D += (I.shiftKey ? -1 : 1);
                        if (D < 0 || D >= H.rowsBuffer.length) {
                            return
                        }
                        F = I.shiftKey ? H._cCount : -1
                    }
                    return
                }
                break
        }
        return this.doKeyA(I)
    };
    this.editCellA = this.editCell;
    this.editCell = function() {
        if (this.cell && this.cell.parentNode.grid != this) {
            return this._fake.editCell()
        }
        return this.editCellA()
    };
    this.deleteRowA = this.deleteRow;
    this.deleteRow = function(D, E) {
        if (this.deleteRowA(D, E) === false) {
            return false
        }
        if (this._fake.rowsAr[D]) {
            this._fake.deleteRow(D)
        }
    };
    this.clearAllA = this.clearAll;
    this.clearAll = function() {
        this.clearAllA();
        this._fake.clearAll()
    };
    this.editStopA = this.editStop;
    this.editStop = function(D) {
        if (this._fake.editor) {
            this._fake.editStop(D)
        } else {
            this.editStopA(D)
        }
    };
    this._fakeInfo.events.push(this.attachEvent("onAfterSorting", function(E, D, F) {
        if (E >= m) {
            this._fake.setSortImgState(false)
        }
    }));
    this._fake.sortField = function(E, D, H) {
        this._fake.sortField.call(this._fake, E, D, this._fake.hdr.rows[0].cells[E]);
        if (this.fldSort[E] != "na" && this._fake.fldSorted) {
            var F = this._fake.getSortingState()[1];
            this._fake.setSortImgState(false);
            this.setSortImgState(true, arguments[0], F)
        }
    };
    this.sortTreeRowsA = this.sortTreeRows;
    this._fake.sortTreeRowsA = this._fake.sortTreeRows;
    this.sortTreeRows = this._fake.sortTreeRows = function(F, H, D, E) {
        if (this._realfake) {
            return this._fake.sortTreeRows(F, H, D, E)
        }
        this.sortTreeRowsA(F, H, D, E);
        this._fake._h2syncModel();
        this._fake.setSortImgStateA(false);
        this._fake.fldSorted = null
    };
    this._fake._fillers = [];
    this._fake.rowsBuffer = this.rowsBuffer;
    this._fakeInfo.events.push(this.attachEvent("onClearAll", function() {
        this._fake.rowsBuffer = this.rowsBuffer
    }));
    this._add_filler_s = this._add_filler;
    this._add_filler = function(E, D, I, F) {
        if (!this._fake._fillers) {
            this._fake._fillers = []
        }
        if (this._realfake || !F) {
            var H;
            if (I && I.idd) {
                H = this._fake.rowsAr[I.idd]
            } else {
                if (I && I.nextSibling) {
                    H = {};
                    H.nextSibling = this._fake.rowsAr[I.nextSibling.idd];
                    H.parentNode = H.nextSibling.parentNode
                } else {
                    if (this._fake._fillers.length) {
                        H = this._fake._fillers[this._fake._fillers.length - 1][2]
                    }
                }
            }
            this._fake._fillers.push(this._fake._add_filler(E, D, H))
        }
        return this._add_filler_s.apply(this, arguments)
    };
    this._add_from_buffer_s = this._add_from_buffer;
    this._add_from_buffer = function() {
        var D = this._add_from_buffer_s.apply(this, arguments);
        if (D != -1) {
            this._fake._add_from_buffer.apply(this._fake, arguments);
            if (this.multiLine) {
                this._correctRowHeight(this.rowsBuffer[arguments[0]].idd)
            }
        }
        return D
    };
    this._fake.render_row = function(D) {
        var E = this._fake.render_row(D);
        if (E == -1) {
            return -1
        }
        if (E) {
            return this.rowsAr[E.idd] = this.rowsAr[E.idd] || this._fake.copy_row(E)
        }
        return null
    };
    this._reset_view_s = this._reset_view;
    this._reset_view = function() {
        this._fake._reset_view(true);
        this._fake._fillers = [];
        this._reset_view_s()
    };
    this.moveColumn_s = this.moveColumn;
    this.moveColumn = function(E, D) {
        if (D >= m) {
            return this.moveColumn_s(E, D)
        }
    };
    this._fake.attachEvent("onFilterEnd", ()=>{
    	this.filterByAll();
    });
    this.refreshFilters_s = this.refreshFilters;
    this.refreshFilters = ()=>{
    	this.refreshFilters_s();
    	this._fake.refreshFilters();
    }
    this._fakeInfo.events.push(this.attachEvent("onCellChanged", function(I, F, H) {
        if (this._split_event && F < m && this.rowsAr[I]) {
            var D = this._fake.rowsAr[I];
            if (!D) {
                return
            }
            if (D._childIndexes) {
                D = D.childNodes[D._childIndexes[F]]
            } else {
                D = D.childNodes[F]
            }
            var E = this.rowsAr[I].childNodes[F];
            if (E._treeCell && E.firstChild.lastChild) {
                E.firstChild.lastChild.innerHTML = H
            } else {
                E.innerHTML = D.innerHTML
            }
            E._clearCell = false;
            E.combo_value = D.combo_value;
            E.chstate = D.chstate
        }
    }));
    this._fake.combos = this.combos;
    if (this.rowsBuffer[0]) {
        this._reset_view()
    }
    this._fakeInfo.events.push(this.attachEvent("onXLE", function() {
        this._fake._correctSplit()
    }));
    this._fake._correctSplit();
    this.setSizes();
};
SepoaDhtmlXGridObject.prototype._correctSplit = function(c) {
    c = c || (this.obj.scrollWidth - this.objBox.scrollLeft);
    c = Math.min(this.globalBox.offsetWidth, c);
    if(this.globalBox.offsetWidth - c < 300){
    	c = this.globalBox.offsetWidth - 300;
    }
    if (c > -1) {
        this.entBox.style.width = c + "px";
        this.objBox.style.width = c + "px";
        var g = (this.globalBox.offsetWidth - this.globalBox.clientWidth) / 2;
        this._fake.entBox.style.left = c + "px";
        this._fake.entBox.style.width = Math.max(0, this.globalBox.offsetWidth - c - (this.quirks ? 0 : 2) * g) + "px";
        if (this._fake.ftr) {
            this._fake.ftr.parentNode.style.width = this._fake.entBox.style.width
        }
    }
};
SepoaDhtmlXGridObject.prototype.getHeaderFilterIdx=function(){
	const target = this.hdr.querySelector('tr.header-filter-row');
	if(target){
		return [].slice.call(this.hdr.rows).indexOf(target) - 1; 
	}
	return -1;
}
SepoaDhtmlXGridObject.prototype._build_m_order = function() {
    if (this._c_order) {
        this._m_order = [];
        for (var a = 0; a < this._c_order.length; a++) {
            this._m_order[a] = a;
        }
    }
};
SepoaDhtmlXGridObject.prototype.disableHeaderFilter=function(){
	const filterIdx = this.getHeaderFilterIdx();
	if(filterIdx >= 0){
		this.detachHeader(filterIdx);
	}
	delete this.filters;
	
	if(this._fake && this._fake._realfake){
		this._fake.disableHeaderFilter();
	}
	
	this._headerFilter = false;
}
SepoaDhtmlXGridObject.prototype.enableHeaderFilter=function(){
	const filters = [];
	const cCnt = this.getColumnCount();
	let bCnt = 0;
	for(var i=0;i<cCnt;i++){
		switch(this.getColType(i)){
		case "autocomplete":
		case "comboro":
		case "comboro2":
			filters.push("#select_filter");
			break;
		case "edn":
		case "ron":
			filters.push("#numeric_filter");
			break;
		case "edtxt":
		case "txttxt":
		case "ro":
		case "rotxt":
		case "template":
			filters.push("#text_filter");
			break;
		case "dhxCalendar":
		case "dhxCalendarA":
		case "dhxCalendarR":
		case "dhxCalendarYM":
		case "dhxCalendarYMR":
			filters.push("#text_filter");
			break;
		case "ch":
		case "icon":
		case "img":
		case "link":
		default :
			filters.push("");
			bCnt++;
			break;
		}
	}
	if(cCnt - bCnt > 0){
		let split = this._fake?.getColumnCount();
		
		if(split){
			this.unSplitAt();
		}
		this.attachHeaderFns[0].bind(this)(filters.join(","));
		this.hdr.rows[this.hdr.rows.length - 1].classList.add("header-filter-row");
		this.hdr.rows[this.hdr.rows.length - 1].querySelectorAll('input').forEach((i)=>{
			i.setAttribute("autocomplete", "off");
		})
		if(split){
			this.splitAt(split);
		}
		this._headerFilter = true;
		this.setSizes();
		
	}
}
SepoaDhtmlXGridObject.prototype.collectTreeValues=function(e) {
    if (typeof this._tr_strfltr == "undefined") {
        this._tr_strfltr = -1
    }
    this.dma(true);
    this._build_m_order();
    e = this._m_order ? this._m_order[e] : e;
    var m = {};
    var h = [];
    var a = this._f_rowsBuffer || this._h2;
    a.forEachChild(0, function(c) {
        if (this._tr_strfltr == -2 || (this._tr_strfltr == -1 && !c.childs.length) || (this._tr_strfltr == c.level)) {
            var n = this._get_cell_value(c.buff, e);
            if (n) {
                m[n] = true
            }
        }
    }, this);
    this.dma(false);
    var o = (this.combos[e] || (this._col_combos ? this._col_combos[e] : ((this._sub_trees && this._sub_trees[e]) ? this._sub_trees[e][0] : false)));
    for (var l in m) {
        if (m[l] === true) {
            if (o) {
                if (o.get && o.get(l)) {
                    l = o.get(l)
                } else {
                    if (o.getOption && o.getOption(l)) {
                        l = o.getOption(l).text
                    } else {
                        if (o.getItemText) {
                            var u = o.getItemText(l);
                            var w = this._sub_trees[e][1] = this._sub_trees[e][1] || {};
                            w[u] = l;
                            l = u
                        }
                    }
                }
            }
            h.push(l)
        }
    }
    return h.sort()
};
SepoaDhtmlXGridObject.prototype._loadSelectOptins = function(h, n) {
    var a = this.collectValues(n);
    var e = h.value;
    h.innerHTML = "";
    h.options[0] = new Option("", "");
    var m = this._filter_tr ? this._filter_tr[n] : null;
    const combos = this.getColumnCombo(n);
    for (var g = 0; g < a.length; g++) {
    	let v = a[g];
    	if(combos){
    		const vFromCombo = combos?(combos.getOptionByLabel(a[g])?.value):undefined;
    		if(vFromCombo){
    			v = vFromCombo;
    		}
    	}
    	h.options[h.options.length] = new Option(m ? m(a[g]) : a[g], v)
    }
    h.value = e
};
SepoaDhtmlXGridObject.prototype._in_header_select_filter = function(e, c) {
	e.innerHTML = "<select></select>";
    e.onclick = function(g) {
        (g || event).cancelBubble = true;
        return false
    };
    this.makeFilter(e.firstChild, c);
    var a = this.combos;
    e.firstChild._filter = function() {
    	var g = e.firstChild.value;
        if (!g) {
            return ""
        }
        if (a[c]) {
            g = a[c].keys[a[c].values._dhx_find(g)]
        }
        g = g.toLowerCase();
        return function(h) {
            return (h.toString().toLowerCase() == g)
        }
    };
    this._filters_ready()
};
SepoaDhtmlXGridObject.prototype._filters_resize_combo = function() {
    if (!this.filters) {
        return true
    }
    for (var a = 0; a < this.filters.length; a++) {
        if (this.filters[a][0].combo != null) {
            this.filters[a][0].combo.setSize(Math.round(this.filters[a][0].offsetWidth * 90 / 100))
        }
    }
    return true
};
SepoaDhtmlXGridObject.prototype._add_filler = function(m, a, c, g) {
    if (!a) {
        return null
    }
    var l = "__filler__";
    var h = this._prepareRow(l);
    h.firstChild.style.width = "1px";
    h.firstChild.style.borderWidth = h.firstChild.style.padding = h.firstChild.style.margin = "0px";
    for (var e = 1; e < h.childNodes.length; e++) {
        h.childNodes[e].style.display = "none"
    }
    this._get_view_size();
    h.firstChild.style.height = a * this._srdh + "px";
    c = c || this.rowsCol[m];
    if (c && c.nextSibling) {
        c.parentNode.insertBefore(h, c.nextSibling)
    } else {
        if (_isKHTML) {
            this.obj.appendChild(h)
        } else {
            this.obj.rows[0].parentNode.appendChild(h)
        }
    }
    this.callEvent("onAddFiller", [m, a, h, c, g]);
    return [m, a, h]
};