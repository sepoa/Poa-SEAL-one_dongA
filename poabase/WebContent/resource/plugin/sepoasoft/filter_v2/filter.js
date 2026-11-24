"use strict";

if (window.NodeList && !NodeList.prototype.forEach) {
    NodeList.prototype.forEach = Array.prototype.forEach;
}

var SepoaFilterObject = function(_cont, _conf, sepoa) {
    var moduleConf = $.extend(true, moduleConf, _conf);
    var confKeys = Object.keys(moduleConf);

    for (var idx in confKeys) {
        var key = confKeys[idx];

        if (key == 'filter') continue;

        var item = moduleConf[key];

        if (item) {
            if (item.module) {
                if (!sUtils.workflow.check(item.module)) {
                    delete moduleConf[key]
                }
            }
        }
    }

    if (_conf.filter) {
        var array = _conf.filter;
        for (var idx in array) {
            var option = array[idx];

            if (option.module) {
                if (!sUtils.workflow.check(option.module)) {
                    var moduleIdx = 0;

                    for (var i in moduleConf.filter) {
                        if (moduleConf.filter[i].id == option.id) {
                            moduleIdx = i;
                            break;
                        }
                    }

                    moduleConf.filter.splice(moduleIdx, 1);

                    if (moduleConf.filter.length == 0) {
                        delete moduleConf.filter;
                    }
                }
            }
        }
    }

    _conf = moduleConf;

    Sepoa.cache.text.load('/resource/plugin/sepoasoft/filter_v2/lang/' + Sepoa.info.LANGUAGE + ".js", "/resource/plugin/sepoasoft/filter_v2/lang/KO.js");
    window.dhtmlx.extend(this, dhtmlx.EventSystem);
    this.cont = typeof _cont == "string" ? document.getElementById(_cont) : _cont;
    this.SepoaObject = sepoa;
    this.conf = _conf;
    this.filterObjs = [];
    this.filterObj = this;
    this.body = SUX.getModuleTop().document.body;
    this.conditionBarShowFlag = true;
    this.init();
    this._conditionUpdate();
};

SepoaFilterObject.prototype._conditionUpdate = function() {
    if (this.conditionCont && this.conditionBarShowFlag) {
        if (this.conditionCont.querySelector('.box_container').childElementCount > 0) {
            this.conditionCont.style.display = "";
        } else {
            this.conditionCont.style.display = "none";
        }

        setTimeout(function() {
            SUX.callEvent('onWindowResize', [])
        }, 400)
    }
}

SepoaFilterObject.prototype.getFilterData = function() {
    var result = {};

    this.filterObjs.forEach(function(obj) {
        var dataArr = obj._getFilterData();
        dataArr.forEach(
            function(d) {
                for (var id in d) {
                    if (d[id]) {
                        result[id] = d[id];
                    }
                }
            }.bind(this)
        );
    });
    return result;
};

SepoaFilterObject.prototype.setDefault = function(id, v) {
    this.getItemById(id).setDefault(v);
};

SepoaFilterObject.prototype.getDefault = function(id) {
    return this.getItemById(id).getDefault();
};

SepoaFilterObject.prototype.setValue = function(id, v) {
    this.getItemById(id).setValue(v);
};

SepoaFilterObject.prototype.getValue = function(id) {
    return this.getItemById(id).getValue();
};
SepoaFilterObject.prototype.setReadOnly = function(id, b) {
    this.getItemById(id).setReadOnly(b);
};
SepoaFilterObject.prototype.isReadOnly = function(id) {
    return this.getItemById(id).isReadOnly();
};

SepoaFilterObject.prototype.getItemById = function(id) {
    var target;
    this.filterObjs.forEach(
        function(obj) {
            if (obj.id.toLowerCase() == id.toLowerCase()) {
                target = obj;
                return;
            }
        }.bind(this)
    );
    return target;
};

SepoaFilterObject.prototype.reloadCombo = function(id, combo) {
    this.getItemById(id).reloadCombo(combo);
};

SepoaFilterObject.prototype.hideConditionBar = function() {
    var div = this.conditionCont;
    if (div) {
        div.style.display = "none";
        this.conditionBarShowFlag = false;
    }
};

SepoaFilterObject.prototype.resizeKeywordWidth = function(px) {
    var filterArr = this.filterObjs;

    var targetInputObj;
    filterArr.forEach(
        function(filter) {
            if (filter.type == "keyword") {
                targetInputObj = filter.objectDiv;
                return;
            }
        }.bind(this)
    );
    if (targetInputObj) {
        targetInputObj.style.maxWidth = px;
    }
};

SepoaFilterObject.prototype.setShow = function(id) {
    var target = this.getItemById(id);
    if (target) {
        target.objectDiv.style.display = "flex";
    }
};

SepoaFilterObject.prototype.setHide = function(id) {
    var target = this.getItemById(id);
    if (target) {
        target.objectDiv.style.display = "none";
    }
};


SepoaFilterObject.prototype.clearConditionById = function(filterId) {
    var p = this.obj.conditionCont;
    if (p) {
        var container = p.querySelector(".box_container");
        var boxes = container.querySelectorAll(".condition_box");
        var alreadyExist;
        if (boxes.length > 0) {
            boxes.forEach(function(box) {
                if (box.filterId == filterId) {
                    alreadyExist = box;
                }
            }.bind(this));
            if (alreadyExist) {
                alreadyExist.parentNode.removeChild(alreadyExist);
            }
        }
    }
    this.obj._conditionUpdate();
};

SepoaFilterObject.prototype.init = function() {
    this._drawbasic();
};

SepoaFilterObject.prototype._drawbasic = function() {
    var wrapper = document.createElement("div");
    this.cont.appendChild(wrapper);
    wrapper.setAttribute("class", "filter_wrap");

    var filterItemsCont = document.createElement("div");
    filterItemsCont.setAttribute("class", "ssfilter_cont");
    wrapper.appendChild(filterItemsCont);

    var filterCont = filterItemsCont;
    var filterConf = this.conf;
    var order = ["keyword", "filter", "status", "calendar"];

    if (filterConf.filter) {
        wrapper.classList.add("fix_height_70");
        var condition = document.createElement("div");
        wrapper.appendChild(condition);
        condition.setAttribute("class", "condition_container");
        this.conditionCont = condition;

        var title = document.createElement("div");
        title.setAttribute("class", "title");
        title.innerText = Sepoa.cache.text.get("FILTERBAR.CONDITION");
        condition.appendChild(title);

        var boxContainer = document.createElement("div");
        boxContainer.setAttribute("class", "box_container");
        condition.appendChild(boxContainer);

        this._drawClearBtn();
    }

    order.forEach(
        function(t) {
            for (var type in filterConf) {
                if (t == type) {
                    switch (type) {
                        case "keyword":
                            var keyword = new SepoaFilterBarKeyword(
                                filterConf[type],
                                filterCont,
                                this
                            );
                            this.filterObjs.push(keyword);
                            break;
                        case "filter":
                            var filter = new SepoaFilterBarFilter(
                                filterConf[type],
                                filterCont,
                                this
                            );
                            filter._drawbasic();
                            this.filterObjs.push(filter);
                            break;
                        case "status":
                            var status = new SepoaFilterBarStatus(
                                filterConf[type],
                                filterCont,
                                this
                            );
                            this.filterObjs.push(status);
                            break;
                        case "calendar":
                            var calendar = new SepoaFilterBarCalendar(
                                filterConf[type],
                                filterCont,
                                this
                            );
                            this.filterObjs.push(calendar);
                            break;
                    }
                }
            }
        }.bind(this)
    );
};

SepoaFilterObject.prototype._closeAllCombo = function() {
    var openedModals = this.body.querySelectorAll(".filter_modal_selected");
    openedModals.forEach(function(modal) {
        modal.classList.remove("filter_modal_selected");
        modal.classList.add("filter_hiding");
    });
};

SepoaFilterObject.prototype._makeFrame = function(parent) {
    var frame = document.createElement("div");
    frame.setAttribute("class", "frame_div");
    parent.appendChild(frame);

    const fn = (e) => {
        var combo = this.body.querySelector(".combo_icon_up");
        if (combo) {
            combo.classList.toggle("combo_icon_up");
        }
        this._closeAllModal();
        e.srcElement.removeEventListener('blur', fn);
        frame.removeEventListener('click', fn);

        frame.parentNode?.removeChild(frame);
    };

    frame.addEventListener("click", fn);
    if (window != SUX.getModuleTop()) {
        SUX.getModuleTop().focus();
        SUX.getModuleTop().addEventListener('blur', fn);
    } else {
        window.addEventListener('blur', fn);
    }
};

SepoaFilterObject.prototype._closeAllModal = function() {

    var openedModals = this.body.querySelectorAll(".modal_selected");
    openedModals.forEach(function(modal) {
        modal.classList.remove("modal_selected");
        modal.classList.add("hiding");
    });
};

SepoaFilterObject.prototype._closeAllFrame = function() {
    var frames = this.body.querySelectorAll(".frame_div");
    frames.forEach(function(frame) {
        frame.parentNode.removeChild(frame);
    });
};

SepoaFilterObject.prototype._closeAllFrameinModal = function() {
    var frames = this.body.querySelectorAll(".frame_div_in_modal");
    frames.forEach(function(frame) {
        frame.parentNode.removeChild(frame);
    });
};

SepoaFilterObject.prototype._toggleModal = function(div, top, left) {
    if (
        div.classList.contains("hiding") ||
        div.classList.contains("filter_hiding")
    ) {
        if (div.classList.contains("hiding")) {
            div.classList.remove("hiding");
            div.classList.add("modal_selected");
        } else if (div.classList.contains("filter_hiding")) {
            div.classList.remove("filter_hiding");
            div.classList.add("filter_modal_selected");
        }
        div.style.top = top + "px";
        div.style.left = left + "px";
    } else {
        if (div.classList.contains("modal_selected")) {
            div.classList.add("hiding");
            div.classList.remove("modal_selected");
        } else if (div.classList.contains("filter_modal_selected")) {
            div.classList.add("filter_hiding");
            div.classList.remove("filter_modal_selected");
        }
    }
};

SepoaFilterObject.prototype._addCondition = function(arr, filterId) {
    var p = this.obj.conditionCont;
    var container = p.querySelector(".box_container");
    var boxes = container.querySelectorAll(".condition_box");

    var newBox = document.createElement("div");
    newBox.setAttribute("class", "condition_box");
    newBox.filterId = this.id;

    var labelCont = document.createElement("div");
    labelCont.setAttribute("class", "label");
    labelCont.innerText = this.label.replace(/<(\/br|br)([^>]*)>/gi, "");

    if (
        this.type == "flag" ||
        this.type == "calendar" ||
        this.type == "text"
    ) {
        newBox.appendChild(labelCont);
    }

    arr.forEach(function(a) {
        newBox.appendChild(a);
    });

    var alreadyExist;
    if (boxes.length <= 0) {
        container.appendChild(newBox);
    } else {
        boxes.forEach(function(box) {
            if (box.filterId == filterId) {
                alreadyExist = box;
            }
        });
        if (arr.length == 0) {
            if (alreadyExist) {
                alreadyExist.parentNode.removeChild(alreadyExist);
            }
        }

        if (alreadyExist != undefined) {
            var alreadyCon = alreadyExist.querySelectorAll(".condition");
            alreadyCon.forEach(function(ac) {
                ac.parentNode.removeChild(ac);
            });
            arr.forEach(function(a) {
                alreadyExist.appendChild(a);
            });
        } else {
            container.appendChild(newBox);
        }
    }

    this.obj._conditionUpdate();
};

SepoaFilterObject.prototype._makeFrameinModal = function(parent) {
    var frame = document.createElement("div");
    frame.setAttribute("class", "frame_div_in_modal");

    if (parent.classList.contains("in_modal")) {
        $(parent).before(frame);
    } else {
        parent.appendChild(frame);
    }
    frame.addEventListener(
        "click",
        function(e) {
            var icon = this.body.querySelector(".combo_icon_up");
            if (icon) {
                icon.classList.toggle("combo_icon_up");
            }
            this._closeAllCombo();
            this._onModalClose();
            frame.parentNode.removeChild(frame);
        }.bind(this)
    );
};


SepoaFilterObject.prototype._clearConditionBar = function() {
    var p = this.conditionCont;
    var container = p.querySelector(".box_container");
    container.innerHTML = "";
    this._conditionUpdate();
};

SepoaFilterObject.prototype._resetAllFilterCondition = function() {
    this._clearConditionBar();
    this.filterObjs.forEach(function(obj) {
        if (obj instanceof SepoaFilterBarFilter) {
            if (obj.pools) {
                obj.pools.forEach(function(o) {
                    o.clear();
                    try {
                        o.restoreDefault();
                        o._setComboDefault();
                    } catch (error) {}
                }.bind(this))
            }
        }
    }.bind(this))
    this._conditionUpdate();
};

SepoaFilterObject.prototype._deleteCondition = function(_filterId, _targetId) {
    var container = this.obj.conditionCont;
    var boxes = container.querySelectorAll(".condition_box");
    var alreadyExist;
    boxes.forEach(function(box) {
        if (box.filterId == _filterId) {
            alreadyExist = box;
        }
    });

    if (alreadyExist) {
        var targetBoxs = alreadyExist.querySelectorAll(".condition");
        if (targetBoxs.length != 1 && _targetId) {
            var targetCondition;
            targetBoxs.forEach(function(box) {
                var t = box.getAttribute("target");
                if (t == _targetId) {
                    targetCondition = box;
                    return;
                }
            });
            targetCondition.parentNode.removeChild(targetCondition);
        } else {
            alreadyExist.parentNode.removeChild(alreadyExist);
        }
    }
};

SepoaFilterObject.prototype._loadComboBox = function(combo) {
    var params = combo;
    if (typeof params == "undefined" || params == null || params.length < 1) {
        return;
    }
    params = params.split(",");
    var that = this;
    var code = params[0];
    var param = window.dhx4.comboParam(
        typeof params[1] == "undefined" || params[1] == null ? "" : params[1]
    );
    var default_value = params.length >= 3 ? params[2] : "";

    setTimeout(function(code, param, default_value) {
        var data = Sepoa.cache.combo.get(code + "," + param);
        this._drawComboList(data);

    }.bind(this), 0, code, param, default_value);
};

SepoaFilterObject.prototype._drawClearBtn = function() {
    var container = this.conditionCont;
    var type = "clearBtn";
    var objectDiv = document.createElement("div");
    objectDiv.setAttribute("class", "ssfilter_" + type);
    objectDiv.classList.add("xi-sepoa-dummy");
    objectDiv.classList.add("xi-x");
    objectDiv.title = Sepoa.cache.text.get("FILTERBAR.CONDITION_RESET");
    container.appendChild(objectDiv);

    objectDiv.addEventListener(
        "click",
        function() {
            this._resetAllFilterCondition();
            // this.callEvent("onSearch", ["clearButtonClick"]);
        }.bind(this)
    );
};

/* filter */
var SepoaFilterBarFilter = function(_conf, cont, filterObj) {
    this.conf = _conf;
    this.cont = cont;
    this.obj = filterObj;
    this.id = "F_Filter";
    this.comboList = {};
    this.filterTypes = {};
    this.body = SUX.getModuleTop().document.body;
    this.objectDiv;
    this.pools = [];
    this.type = "filter";
};

SepoaFilterBarFilter.prototype = Object.create(SepoaFilterObject.prototype);

SepoaFilterBarFilter.prototype._drawbasic = function() {
    var container = this.cont;
    var type = this.type;
    var objectDiv = document.createElement("div");
    objectDiv.setAttribute("class", "ssfilter_" + type);
    container.appendChild(objectDiv);
    this.objectDiv = objectDiv;

    var iconNspanWrapper = document.createElement("div");
    iconNspanWrapper.setAttribute("class", "filter_icon_span_wrapper");
    objectDiv.appendChild(iconNspanWrapper);

    var icon = document.createElement("div");
    icon.classList.add("filter_icon");
    icon.classList.add("xi-sepoa-dummy");

    var span = document.createElement("span");
    span.setAttribute("class", "filter_span");
    span.innerText = Sepoa.cache.text.get("FILTERBAR.FILTER");
    iconNspanWrapper.appendChild(icon);
    iconNspanWrapper.appendChild(span);

    objectDiv.addEventListener(
        "click",
        function(e) {
            var lt = sUtils.element.getOffset(e.currentTarget, "module.top");
            var top = lt.top + e.currentTarget.offsetHeight;
            var left = lt.left;
            var screenWidth = this.body.offsetWidth;
            this.test = this.test_wrapper.offsetHeight;
            var p = left + this.width;
            var d = screenWidth - p;
            if (d < 0) {
                left = left + d - 10;
            }

            this._toggleModal(this.modal, top, left);
            this._makeFrame(this.body);
        }.bind(this)
    );
    this._drawFilterModal();
};

SepoaFilterBarFilter.prototype._drawFilterModal = function() {
    var modalContainer = document.createElement("div");
    modalContainer.setAttribute("class", "filter_modal_container");
    modalContainer.classList.add("filter_modal");
    this.body.appendChild(modalContainer);
    this.modal = modalContainer;

    var modalWrapper = document.createElement("div");
    modalWrapper.setAttribute("class", "filter_modal_wrapper");
    modalContainer.appendChild(modalWrapper);

    var topBar = document.createElement("div");
    topBar.setAttribute("class", "topbar");
    modalWrapper.appendChild(topBar);

    var title = document.createElement("span");
    title.setAttribute("class", "title");
    topBar.appendChild(title);
    title.innerText = Sepoa.cache.text.get("FILTERBAR.ADVANCED_SEARCH");

    var closeBtn = document.createElement("div");
    closeBtn.setAttribute("class", "close_btn");
    closeBtn.classList.add("xi-sepoa-dummy");
    topBar.appendChild(closeBtn);

    closeBtn.addEventListener(
        "click",
        function() {
            this._toggleModal(this.modal);
            this._closeAllFrame();
        }.bind(this)
    );

    var contentWrapper = document.createElement("div");
    contentWrapper.setAttribute("class", "content_wrapper");
    modalWrapper.appendChild(contentWrapper);

    var wrapper = document.createElement("div");
    wrapper.setAttribute("class", "wrapper");
    contentWrapper.appendChild(wrapper);

    var btnsContainer = document.createElement("div");
    btnsContainer.setAttribute("class", "btns_cont");
    wrapper.appendChild(btnsContainer);

    var applyBtn = document.createElement("div");
    applyBtn.setAttribute("class", "apply_btn");
    btnsContainer.appendChild(applyBtn);
    applyBtn.innerText = Sepoa.cache.text.get("FILTERBAR.SEARCH_2");

    var clearBtn = document.createElement("div");
    clearBtn.setAttribute("class", "clear_btn");
    btnsContainer.appendChild(clearBtn);
    clearBtn.innerText = Sepoa.cache.text.get("FILTERBAR.RESET");

    var useFlagSpan = document.createElement("div");
    useFlagSpan.setAttribute("class", "use_flag_span");
    wrapper.appendChild(useFlagSpan);

    var span = document.createElement("span");
    useFlagSpan.appendChild(span);

    applyBtn.addEventListener(
        "click",
        function(e) {
            this.obj.callEvent("onSearch", ["filter"]);
            this._closeAllModal();
            this._closeAllFrame();
        }.bind(this)
    );

    clearBtn.addEventListener(
        "click",
        function(e) {
            this.obj._resetAllFilterCondition();
        }.bind(this)
    );

    this._drawFilterTypes(wrapper);

    this.test_wrapper = contentWrapper;
    this.width = this.test_wrapper.offsetWidth;
    this.height = this.test_wrapper.offsetHeight;
    modalContainer.classList.add("hiding");

    var beforeWindow = window.innerHeight;
    var beforeWidth = window.innerWidth;
    window.addEventListener(
        "resize",
        function(e) {
            beforeWindow = window.innerHeight;
            var windowHeight = window.innerHeight;
            var modalContHeight = modalContainer.offsetHeight;
            var modalSize = this.test;
            var n = 127 - (windowHeight - modalSize);

            if (windowHeight - modalContHeight < 127) {
                contentWrapper.style.maxHeight = this.test - n - 40 + "px";
            } else {
                contentWrapper.style.maxHeight =
                    contentWrapper.offsetHeight + Math.abs(n) - 40 + "px";
            }

            var beforeWidth = window.innerWidth;
            var r = this.test_wrapper.getBoundingClientRect().left;
            if (beforeWidth < r + this.width) {
                this.modal.style.left = beforeWidth - this.width + "px";
            } else {
                this.modal.style.left =
                    this.objectDiv.getBoundingClientRect().left - 10 + "px";
            }
        }.bind(this)
    );
};

SepoaFilterBarFilter.prototype._drawFilterTypes = function(wrapper) {
    var types = this.conf;
    var container = wrapper;

    types.forEach(
        function(t) {
            var ty = t.type.toLowerCase();
            var id = t.id;

            var typeContainer = document.createElement("div");
            typeContainer.setAttribute("class", "type_wrapper");
            container.appendChild(typeContainer);

            var labelArea = document.createElement("div");
            labelArea.setAttribute("class", "label_area");
            typeContainer.appendChild(labelArea);

            var labelcont = document.createElement("div");
            labelcont.setAttribute("class", "label");
            labelArea.appendChild(labelcont);

            var label = Sepoa.cache.text.get(
                (this.obj.SepoaObject || Sepoa).common.SCREEN_ID + "." + id
            );

            labelcont.innerHTML = label;

            var filterArea = document.createElement("div");
            filterArea.setAttribute("class", "filter_area");
            typeContainer.appendChild(filterArea);

            t.label = label;

            switch (ty) {
                case "select":
                    var select = new SepoaFilterBarSelect(t, typeContainer, this.obj);
                    this.obj.filterObjs.push(select);
                    this.pools.push(select);
                    break;
                case "flag":
                    var flag = new SepoaFilterBarFlag(t, typeContainer, this.obj);
                    this.obj.filterObjs.push(flag);
                    this.pools.push(flag);
                    break;
                case "calendar":
                    this._drawCalendar(t, typeContainer, this.obj);
                    break;
                case "checkbox":
                    var checkbox = new SepoaFilterBarCheckbox(t, typeContainer, this.obj);
                    this.obj.filterObjs.push(checkbox);
                    this.pools.push(checkbox);
                    break;
                case "popup":
                    var popup = new SepoaFilterBarPopup(t, typeContainer, this.obj);
                    this.obj.filterObjs.push(popup);
                    this.pools.push(popup);
                    break;
                case "text":
                    var text = new SepoaFilterBarText(t, typeContainer, this.obj);
                    this.obj.filterObjs.push(text);
                    this.pools.push(text);
                    break;
            }
        }.bind(this)
    );
};

SepoaFilterBarFilter.prototype.restoreDefault = function(filter) {};

SepoaFilterBarFilter.prototype._drawCalendar = function(t, typeContainer, obj) {
    var cont = typeContainer;
    var filterArea = cont.querySelector(".filter_area");
    var calendar = new SepoaFilterBarCalendar(t, filterArea, obj);
    obj.filterObjs.push(calendar);
    this.pools.push(calendar);
    var selectedDateCont = document.createElement("div");
    selectedDateCont.setAttribute("class", "selected_date_container");
    filterArea.appendChild(selectedDateCont);
    calendar.selectedDateCont = selectedDateCont;
    var c = calendar.modal;
    c.classList.remove("hiding");
    c.classList.add("filter_hiding");
};

SepoaFilterBarFilter.prototype._drawConditionBox = function(items, filterId) {
    var arr = [];
    items.forEach(
        function(item) {
            var condition = document.createElement("div");
            condition.setAttribute("class", "condition");
            var _id = item.getAttribute("data-id");
            condition.setAttribute("target", _id);
            condition.value = item.valueText;
            condition.label = item.filterLabel;
            condition.type = item.filterType;
            condition.filterId = filterId;

            var span = document.createElement("span");
            span.setAttribute("class", "condition_span");
            condition.appendChild(span);
            span.innerText =
                item.filterType == "flag" ?
                Sepoa.cache.text.get("FILTERBAR.CHECKED") :
                item.valueText;

            if (true) {
                var clearBtn = document.createElement("div");
                clearBtn.setAttribute("class", "clear_btn");
                clearBtn.classList.add('xi-sepoa-dummy');
                condition.appendChild(clearBtn);
            }
            arr.push(condition);

            if (true) {
                clearBtn.addEventListener(
                    "click",
                    function(e) {
                        var filterId = e.currentTarget.parentNode.filterId;
                        var item = this.obj.getItemById(filterId);
                        var condition = e.currentTarget.parentNode;
                        if (!item.readOnlyFlag) {
                            item._deleteConditionByTarget(condition);
                        }
                        this.obj._conditionUpdate();
                    }.bind(this)
                );
            }
        }.bind(this)
    );
    this._addCondition(arr, filterId);
};

SepoaFilterBarFilter.prototype.clear = function() {};

SepoaFilterBarFilter.prototype._toggleCombo = function(div) {
    if (div.classList.contains("filter_hiding")) {
        this._closeAllCombo();
        div.classList.remove("filter_hiding");
        div.classList.add("filter_modal_selected");
    } else {
        div.classList.add("filter_hiding");
        div.classList.remove("filter_modal_selected");
    }
};

SepoaFilterBarFilter.prototype._getFilterData = function() {
    var results = [];
    return results;
};

/* keyword */
var SepoaFilterBarKeyword = function(_conf, _cont, _obj) {
    this.id = _conf.id;
    this.placeholder = _conf.placeholder;
    this.cont = _cont;
    this.obj = _obj;
    this.combo = _conf.combo;
    this.comboBtn;
    this.comboList;
    this.selectedBtn;
    this.body = SUX.getModuleTop().document.body;
    this.objectDiv;
    this.type = "keyword";
    this.readOnlyFlag = false;
    this._drawbasic(this.obj);
    this.inputBar;
};
SepoaFilterBarKeyword.prototype = Object.create(SepoaFilterObject.prototype);
SepoaFilterBarKeyword.prototype.setCombo = function(c) {
    this.combo = c;
};
SepoaFilterBarKeyword.prototype.getCombo = function() {
    return this.combo;
};
SepoaFilterBarKeyword.prototype.setValue = function(v) {
    this.inputBar.value = v;
};
SepoaFilterBarKeyword.prototype.getValue = function() {
    return this.inputBar.value;
};
SepoaFilterBarKeyword.prototype.setReadOnly = function(b) {
    if (arguments.length == 0) {
        console.error(
            "Filterbar Error : setReadOnly() needs boolean as parameter."
        );
        return;
    }
    if (b == true) {
        this.inputBar.disabled = true;
        this.readOnlyFlag = true;
    } else if (b == false) {
        this.inputBar.disabled = false;
        this.readOnlyFlag = false;
    }
};
SepoaFilterBarKeyword.prototype.isReadOnly = function() {
    return this.readOnlyFlag;
};
SepoaFilterBarKeyword.prototype.reloadCombo = function(c) {
    this.combo = c;
    this._loadComboBox(c);
};
SepoaFilterBarKeyword.prototype.clear = function() {
    var comboList = this.comboListModal;
    if (comboList) {
        var selectedBtn = comboList.querySelector(".keyword_selected");
        selectedBtn.classList.remove("keyword_selected");
        selectedBtn.isSelected = false;

        var allBtn = comboList.querySelector(".all");
        this._setSeletedTarget(allBtn);
    }
    this.inputBar.value = "";
};
SepoaFilterBarKeyword.prototype._getTarget = function() {
    if (!this.combo) {
        return;
    }
    var target;
    this.comboList.forEach(function(item) {
        if (item.isSelected) {
            target = item.comboValue;
            return;
        }
    });
    return target;
};

SepoaFilterBarKeyword.prototype._setSeletedTarget = function(item) {
    this.selectedBtn = item;
    var span = this.comboBtn.querySelector(".option_span");
    span.innerText = this.selectedBtn.comboText;
    appendClassName(item, "keyword_selected");
    item.isSelected = true;
};
SepoaFilterBarKeyword.prototype._drawComboList = function(data) {
    this.optionContainer.innerHTML = "";

    var wrapper = document.createElement("div");
    wrapper.setAttribute("class", "option_container_wrapper");
    this.optionContainer.appendChild(wrapper);

    var span = document.createElement("div");
    span.setAttribute("class", "option_span");
    wrapper.appendChild(span);
    this.comboBtn = wrapper;

    span.innerText = Sepoa.cache.text.get("BUTTON.ALL");

    var showListBtn = document.createElement("div");
    showListBtn.setAttribute("class", "option_btn");
    showListBtn.classList.add("xi-sepoa-dummy");
    wrapper.appendChild(showListBtn);

    var comboList = document.createElement("div");
    comboList.setAttribute("class", "keyword_combo_list_wrapper");
    comboList.classList.add("hiding");
    comboList.classList.add("filter_modal");
    this.comboListModal = comboList;

    this.comboList = [];
    comboList.innerHTML = "";
    var item = document.createElement("div");
    item.setAttribute("class", "keyword_combo_list_item");
    appendClassName(item, "keyword_selected");
    appendClassName(item, "all");
    item.isSelected = true;
    item.comboText = Sepoa.cache.text.get("BUTTON.ALL");
    item.comboValue = "ALL";

    item.innerText = item.comboText;
    comboList.appendChild(item);
    this.comboList.push(item);

    this.comboBtn.addEventListener(
        "click",
        function(e) {
            var icon = this.comboBtn.querySelector(".option_btn");
            icon.classList.toggle("combo_icon_up");

            var lt = sUtils.element.getOffset(e.currentTarget, "module.top");
            var top = lt.top + e.currentTarget.offsetHeight;
            var left = lt.left;

            this.body.appendChild(this.comboListModal);
            this._makeFrame(this.body);
            this._toggleModal(this.comboListModal, top, left);
        }.bind(this)
    );

    for (var idx in data) {
        var t = data[idx];
        var item = document.createElement("div");
        item.comboText = t.text;
        item.comboValue = t.value;
        item.setAttribute("class", "keyword_combo_list_item");
        item.innerText = t.text;
        comboList.appendChild(item);
        item.isSelected = false;
        this.comboList.push(item);
    }

    this.comboList.forEach(
        function(item) {
            item.addEventListener(
                "click",
                function(e) {
                    if (!this.readOnlyFlag) {
                        var icon = this.comboBtn.querySelector(".option_btn");
                        icon.classList.toggle("combo_icon_up");

                        var selectedBtn = comboList.querySelector(".keyword_selected");
                        selectedBtn.classList.remove("keyword_selected");
                        selectedBtn.isSelected = false;
                        this._setSeletedTarget(item);

                        e.stopPropagation();
                        this._closeAllModal();
                        this._closeAllFrame();
                    }
                }.bind(this)
            );
        }.bind(this)
    );
};

SepoaFilterBarKeyword.prototype._drawbasic = function(obj) {
    var container = this.cont;
    var type = "keyword";
    var objectDiv = document.createElement("div");
    objectDiv.setAttribute("class", "ssfilter_" + type);
    container.appendChild(objectDiv);
    this.objectDiv = objectDiv;

    if (this.combo != undefined && this.combo != "") {
        var optionContainer = document.createElement("div");
        optionContainer.setAttribute("class", "option_container");
        objectDiv.appendChild(optionContainer);
        this.optionContainer = optionContainer;
        this._loadComboBox(this.combo);
    }

    var inputBar = document.createElement("input");
    inputBar.setAttribute("class", "search_input");
    inputBar.setAttribute("placeholder", this.placeholder || "Search");
    objectDiv.appendChild(inputBar);
    this.inputBar = inputBar;

    var searchBtn = document.createElement("div");
    searchBtn.classList.add("search_btn");
    searchBtn.classList.add("xi-sepoa-dummy");
    objectDiv.appendChild(searchBtn);

    searchBtn.addEventListener(
        "click",
        function() {
            obj.callEvent("onSearch", ["filter"]);
        }.bind(this)
    );

    inputBar.addEventListener(
        "keydown",
        function(e) {
            if (e.keyCode == 13) {
                obj.callEvent("onSearch", ["search"]);
            }
        }.bind(this)
    );
};

SepoaFilterBarKeyword.prototype._getFilterData = function() {
    var result = [];
    var target = {};
    var r = {};
    if (this.inputBar.value.trim()) {
        r[this.id] = this.inputBar.value;
        result.push(r);
    } else {
        return result;
    }
    if (this.combo) {
        var targetId = this.id + "_TARGET";
        target[targetId] = this._getTarget();
        result.push(target);
    }
    return result;
};

/* status */
var SepoaFilterBarStatus = function(conf, cont, filterObj) {
    this.id = conf.id;
    this.combo = conf.combo;
    this.label = conf.label;
    this.cont = cont;
    this.comboBtn;
    this.obj = filterObj;
    this.body = SUX.getModuleTop().document.body;
    this.default = conf.default;
    this.comboList;
    this.comboLoadFinished = false;
    this.readOnlyFlag = false;
    this.objectDiv;
    this.type = "status";
    this.tempValue;
    this._drawbasic();
};

SepoaFilterBarStatus.prototype = Object.create(SepoaFilterObject.prototype);

SepoaFilterBarStatus.prototype.setCombo = function(c) {
    this.combo = c;
};
SepoaFilterBarStatus.prototype.getCombo = function() {
    return this.combo;
};
SepoaFilterBarStatus.prototype.setDefault = function(d) {
    this.default = d;
};
SepoaFilterBarStatus.prototype.getDefault = function(d) {
    return this.default;
};

SepoaFilterBarStatus.prototype.setValue = function(v) {
    this.tempValue = v;

    if (this.comboList) {
        this._setValue(v);
    }
};

SepoaFilterBarStatus.prototype._setValue = function(v) {
    var target;
    this.comboList.forEach(function(item) {
        if (item.comboValue.toLowerCase() == v.toLowerCase()) {
            target = item;
            return;
        }
    });
    if (target == undefined) {
        console.error();
    } else {
        var beforeSelected = this.comboListModal.querySelector(".status_selected");
        beforeSelected.isSelected = false;
        beforeSelected.classList.remove("status_selected");

        appendClassName(target, "status_selected");
        target.isSelected = true;

        if (v.toUpperCase() == "ALL") {
            this.comboBtn.classList.remove("something_selected");
            this.comboBtn.querySelector('.status_span').innerText = this.label;
        } else {
            this.comboBtn.classList.add("something_selected");
            this.comboBtn.querySelector('.status_span').innerText = this.label + ' (' + target.comboText + ')';
        }
    }
};
SepoaFilterBarStatus.prototype.getValue = function() {
    var value;
    this.comboList.forEach(
        function(item) {
            if (item.isSelected && item.comboValue != "ALL") {
                value = item.comboValue;
            }
        }.bind(this)
    );
    return value;
};

SepoaFilterBarStatus.prototype.setReadOnly = function(b) {
    if (b == true) {
        this.readOnlyFlag = true;
    } else if (b == false) {
        this.readOnlyFlag = false;
    } else {
        console.log("Filterbar Error : setReadOnly() needs boolean as parameter.");
    }
};
SepoaFilterBarStatus.prototype.isReadOnly = function() {
    return this.readOnlyFlag;
};
SepoaFilterBarStatus.prototype.reloadCombo = function(c) {
    this.clear();
    this.combo = c;
    this.default = undefined;
    this.comboLoadFinished = false;
    this._loadComboBox(c);
};

SepoaFilterBarStatus.prototype.clear = function() {
    var comboList = this.comboListModal;
    if (comboList) {
        this._setValue('ALL');
    }
};

SepoaFilterBarStatus.prototype.restoreDefault = function() {
    this.clear();
    if (this.default) {
        var _default = this.default.toLowerCase();
        this._setValue(_default);
    }
};

SepoaFilterBarStatus.prototype._getFilterData = function() {
    var result = [];
    var r = {};
    if (this.comboLoadFinished) {
        r[this.id] = this.getValue();
    } else {
        r[this.id] = this.tempValue || this.default;
    }
    result.push(r);
    return result;
};

SepoaFilterBarStatus.prototype._drawbasic = function() {
    var container = this.cont;
    var objectDiv = document.createElement("div");
    objectDiv.setAttribute("class", "ssfilter_" + this.type);
    container.appendChild(objectDiv);
    this.objectDiv = objectDiv;
    this.comboBtn = objectDiv;

    var label = Sepoa.cache.text.get("FILTERBAR.STATUS");
    if (this.label) {
        label = Sepoa.cache.text.get((this.obj.SepoaObject || Sepoa).common.SCREEN_ID + "." + this.label);
    }

    this.label = label;

    objectDiv.appendChild($(sUtils.string.dataReplacer('<div class="status_icon_span_wrapper"><div class="status_icon xi-sepoa-dummy"></div><span class="status_span">$.{LABEL}</span></div>', {
        LABEL: label
    }))[0]);

    this.comboBtn.addEventListener("click", this._comboOpen.bind(this));
    this._loadComboBox(this.combo);
};

SepoaFilterBarStatus.prototype._comboOpen = function(e) {
    var lt = sUtils.element.getOffset(e.currentTarget, "module.top");
    var top = lt.top + e.currentTarget.offsetHeight;
    var left = lt.left;
    this._makeFrame(this.body);
    if (this.comboListModal) {
        this._toggleModal(this.comboListModal, top, left);
    } else {
        console.error(
            (this.obj.SepoaObject || Sepoa).common.SCREEN_ID +
            ": Filterbar-" +
            this.id +
            "-combo"
        );
    }
};


SepoaFilterBarStatus.prototype._drawComboList = function(data) {
    this.comboList = [];

    var comboList = document.createElement("div");
    comboList.setAttribute("class", "status_combo_list_wrapper");
    comboList.classList.add("hiding");
    comboList.classList.add("filter_modal");

    this.comboListModal = comboList;
    this.body.appendChild(this.comboListModal);

    comboList.innerHTML = "";
    var item = document.createElement("div");
    item.setAttribute("class", "status_combo_list_item");
    appendClassName(item, "status_selected");
    appendClassName(item, "all");
    item.comboValue = "ALL";
    item.comboText = Sepoa.cache.text.get("FILTERBAR.ALL");
    item.isSelected = true;
    item.innerText = Sepoa.cache.text.get("FILTERBAR.ALL");
    comboList.appendChild(item);
    this.comboList.push(item);

    for (var idx in data) {
        var t = data[idx];
        var item = document.createElement("div");
        item.setAttribute("class", "status_combo_list_item");
        item.comboText = t.text;
        item.comboValue = t.value;
        item.isSelected = false;
        item.innerText = t.text;
        comboList.appendChild(item);
        this.comboList.push(item);
    }
    var allitems = comboList.querySelectorAll(".status_combo_list_item");
    var filterObj = this.obj;
    allitems.forEach(
        function(item) {
            item.addEventListener(
                "click",
                function(e) {
                    if (!this.readOnlyFlag) {
                        this.obj.callEvent("onChange", [this.id, item.comboValue]);
                        var beforeSelected = comboList.querySelector(".status_selected");
                        beforeSelected.isSelected = false;
                        beforeSelected.classList.remove("status_selected");

                        appendClassName(e.currentTarget, "status_selected");
                        e.currentTarget.isSelected = true;
                        filterObj.callEvent("onSearch", ["filter"]);

                        this._toggleModal(e.currentTarget.parentNode);
                        this._closeAllFrame();
                        if (e.currentTarget.classList.contains("all")) {
                            this.comboBtn.querySelector('.status_span').innerText = this.label;
                            this.comboBtn.classList.remove("something_selected");
                        } else {
                            this.comboBtn.querySelector('.status_span').innerText = this.label + ' (' + e.currentTarget.comboText + ')';
                            this.comboBtn.classList.add("something_selected");
                        }
                    }
                }.bind(this)
            );
        }.bind(this)
    );
    this.comboLoadFinished = true;
    this.restoreDefault();

    if (this.tempValue) {
        this._setValue(this.tempValue);
    }
    this.obj.callEvent("onComboLoad", [this.id, this.comboList]);
};

/* calendar */
var SepoaFilterBarCalendar = function(_conf, cont, _obj) {
    SepoaFilterBarFilter.call(this, _conf, cont, _obj);
    this.conf = _conf;
    this.cont = cont;
    this.obj = _obj;
    this.id = _conf.id;
    this.combo = _conf.combo;
    this.comboBtn;
    this.comboList = [];
    this.fromId = _conf.id + "_FROM";
    this.toId = _conf.id + "_TO";
    this.defaultBtn = {};
    this.btns = [];
    this.fromInput;
    this.toInput;
    this.fromCalendar;
    this.toCalendar;
    this.fromObj;
    this.toObj;
    this.target;
    this.label = _conf.label;
    // this.body = SUX.getModuleTop().document.body;
    this.default = _conf.default;
    this.isForFilter = false;
    this.comboLoadFinished = false;
    this.readOnlyFlag = false;
    this.objectDiv;
    this.type = "calendar";
    this._drawbasic();
};

SepoaFilterBarCalendar.prototype = Object.create(
    SepoaFilterBarFilter.prototype
);

SepoaFilterBarCalendar.prototype.setCombo = function(c) {
    this.combo = c;
};
SepoaFilterBarCalendar.prototype.getCombo = function() {
    return this.combo;
};
SepoaFilterBarCalendar.prototype.setDefault = function(d) {
    this.default = d;
};
SepoaFilterBarCalendar.prototype.getDefault = function() {
    return this.default;
};
SepoaFilterBarCalendar.prototype.setValue = function(date) {
    var valueArr = date.split(":");
    var dateObj = {};
    dateObj.option = valueArr[0];
    dateObj.number = valueArr[1];

    var result = this._adjustBtn(dateObj);

    if (result.to && result.from) {
        this._setInputDate("from", result.from);
        this._setInputDate("to", result.to);
        this.fromObj.setDate(result.from);
        this.toObj.setDate(result.to);

        if (this.isForFilter) {
            var fromValue = this.fromInput.value;
            var toValue = this.toInput.value;

            var selectedDate = document.createElement("div");
            selectedDate.setAttribute("class", "selected_date");
            selectedDate.id = this.id + "_calendar";
            selectedDate.valueText = fromValue + " ~ " + toValue;
            selectedDate.filterLabel = this.label;
            selectedDate.filterType = this.type;
            selectedDate.filterId = this.id;

            this._drawConditionBox([selectedDate], this.id);
        }
    }
};
SepoaFilterBarCalendar.prototype.getValue = function() {
    var r = {};
    this._getFilterData().forEach(
        function(data) {
            for (var a in data) {
                r[a] = data[a];
            }
        }.bind(this)
    );

    return r;
};
SepoaFilterBarCalendar.prototype.setReadOnly = function(b) {
    if (b == true) {
        this.readOnlyFlag = true;
        this.fromInput.disabled = true;
        this.toInput.disabled = true;
    } else if (b == false) {
        this.readOnlyFlag = false;
        this.fromInput.disabled = false;
        this.toInput.disabled = false;
    } else {
        console.log("Filterbar Error : setReadOnly() needs boolean as parameter.");
    }
};

SepoaFilterBarCalendar.prototype.isReadOnly = function() {
    return this.readOnlyFlag;
};

SepoaFilterBarCalendar.prototype.reloadCombo = function(c) {
    this.combo = c;
    this.comboLoadFinished = false;
    this._loadComboBox(c);
};


SepoaFilterBarCalendar.prototype.clear = function() {
    this.clearConditionById(this.id);
    var comboList = this.comboListModal;
    if (comboList) {
        var selectedBtn = comboList.querySelector(".calendar_selected");
        if (selectedBtn) {
            selectedBtn.classList.remove("calendar_selected");
            selectedBtn.isSelected = false;
        }

        var noneBtn = comboList.querySelector(".none");
        noneBtn.classList.add("calendar_selected");
        noneBtn.isSelected = true;

        var span = this.comboBtn.querySelector(".option_span");
        span.innerText = "";
    }
    this.fromInput.value = "";
    this.toInput.value = "";

    var now = new Date();
    this.toObj.setDate(now);
    this.fromObj.setDate(now);
};

SepoaFilterBarCalendar.prototype.restoreDefault = function() {
    this.clear();
    if (!this.default) {
        return;
    }
    var _default = this.default.toLowerCase();
    var dfBtn;

    if (_default.indexOf("#") != -1) {
        var arr = _default.split("#");
        var combo = arr[0];
        dfBtn = arr[1];
    } else {
        dfBtn = _default;
    }

    var dfBtnArr = dfBtn.split(":");
    var st = dfBtnArr[0];
    var num = dfBtnArr[1];
    var target;
    this.btns.forEach(
        function(btn) {
            for (var str in btn) {
                if (st.toLowerCase() == str.toLowerCase() && num == btn[str]) {
                    target = btn;
                    return;
                }
            }
        }.bind(this)
    );
    var d;
    if (target) {
        d = this._adjustBtn(target.div);
        target.div.classList.add("btn_selected");
    } else {
        var btn = {};
        btn.option = st.toLowerCase();
        btn.number = num.toLowerCase();
        d = this._adjustBtn(btn);
    }
    this._setInputDate("from", d.from);
    this._setInputDate("to", d.to);
    this.fromObj.setDate(d.from);
    this.toObj.setDate(d.to);

    if (this.isForFilter) {
        var fromValue = this.fromInput.value;
        var toValue = this.toInput.value;

        var selectedDate = document.createElement("div");
        selectedDate.setAttribute("class", "selected_date");
        selectedDate.id = this.id + "_calendar";
        selectedDate.valueText = fromValue + " ~ " + toValue;
        selectedDate.filterLabel = this.label;
        selectedDate.filterType = this.type;
        selectedDate.filterId = this.id;

        this._drawConditionBox([selectedDate], this.id);
    }
};

SepoaFilterBarCalendar.prototype._getFilterData = function() {
    var result = [];
    var toDate = {};
    var fromDate = {};
    var targetCode = {};

    var to = dhx.str2date(this.toInput.value, Sepoa.info.DATE_FORMAT);
    var from = dhx.str2date(this.fromInput.value, Sepoa.info.DATE_FORMAT);

    if (this.combo) {
        var target = this._getTarget();
        if (target == "NONE") {
            return result;
        }
        var targetId = this.id + "_TARGET";
        targetCode[targetId] = target;
        result.push(targetCode);
    }
    if (to != "Invalid Date") {
        toDate[this.toId] = dhx.date2str(to, "%Y%m%d");
        result.push(toDate);
    }
    if (from != "Invalid Date") {
        fromDate[this.fromId] = dhx.date2str(from, "%Y%m%d");
        result.push(fromDate);
    }
    return result;
};

SepoaFilterBarCalendar.prototype._setComboDefault = function(df) {
    df = df || this.default;
    if (df == undefined || df.indexOf("#") == -1) {
        return;
    }
    var _default = df.toLowerCase();
    var comboVal;
    var arr = _default.split("#");
    comboVal = arr[0];

    var targetCombo;
    if (this.comboList) {
        this.comboList.forEach(
            function(item) {
                if (item.comboValue.toLowerCase() == comboVal.toLowerCase()) {
                    targetCombo = item;
                    return;
                }
            }.bind(this)
        );
    }
    if (targetCombo) {
        var noneBtn = this.comboListModal.querySelector(".none");
        noneBtn.isSelected = false;
        noneBtn.classList.remove("calendar_selected");

        targetCombo.isSelected = true;
        targetCombo.classList.add("calendar_selected");

        var span = this.comboBtn.querySelector(".option_span");
        span.innerText = targetCombo.comboText;
    } else {
        console.error(
            (this.obj.SepoaObject || Sepoa).common.SCREEN_ID +
            "-Filterbar ERROR-" +
            this.id +
            "-default combo code"
        );
    }
};


SepoaFilterBarCalendar.prototype._setBtnInnerText = function(btnObj) {
    var lang = Sepoa.info.LANGUAGE;
    var option = btnObj.option;
    var number = parseInt(btnObj.number);
    var now = new Date();
    var adDate = new Date();
    var enMonth = {
        1: "January",
        2: "February",
        3: "March",
        4: "April",
        5: "May",
        6: "June",
        7: "July",
        8: "August",
        9: "September",
        10: "October",
        11: "November",
        12: "December",
    };

    switch (option) {
        case "year":
            if (number == 0) {
                btnObj.innerText =
                    lang == "KO" ?
                    now.getFullYear() + Sepoa.cache.text.get("FILTERBAR.YEAR") :
                    now.getFullYear();
            } else {
                btnObj.innerText =
                    lang == "KO" ?
                    now.getFullYear() +
                    number +
                    Sepoa.cache.text.get("FILTERBAR.YEAR") :
                    now.getFullYear() + number;
            }
            break;
        case "month":
            if (number == 0) {
                adDate = new Date(now.getFullYear(), now.getMonth(), now.getDay());
                btnObj.innerText =
                    lang == "KO" ?
                    adDate.getMonth() + 1 + Sepoa.cache.text.get("FILTERBAR.MONTH") :
                    enMonth[adDate.getMonth() + 1];
            } else {
                adDate = new Date(
                    now.getFullYear(),
                    now.getMonth() + number,
                    now.getDay()
                );
                btnObj.innerText =
                    lang == "KO" ?
                    adDate.getMonth() + 1 + Sepoa.cache.text.get("FILTERBAR.MONTH") :
                    enMonth[adDate.getMonth() + 1];
            }
            break;
        case "week":
            if (number % 4 == 0) {
                number /= 4;
                if (number == 0) {
                    btnObj.innerText = Sepoa.cache.text.get("FILTERBAR.THIS_WEEK");
                } else if (number < 0) {
                    btnObj.innerText =
                        Math.abs(number) + Sepoa.cache.text.get("FILTERBAR.MONTHS_AGO");
                } else {
                    btnObj.innerText =
                        number + Sepoa.cache.text.get("FILTERBAR.MONTHS_AFTER");
                }
            } else {
                if (number < 0) {
                    btnObj.innerText =
                        Math.abs(number) + Sepoa.cache.text.get("FILTERBAR.WEEKS_AGO");
                } else {
                    btnObj.innerText =
                        number + Sepoa.cache.text.get("FILTERBAR.WEEKS_AFTER");
                }
            }
            break;
        case "day":
            if (number == 0) {
                btnObj.innerText = Sepoa.cache.text.get("FILTERBAR.TODAY");
            } else if (number < 0) {
                btnObj.innerText = number + Sepoa.cache.text.get("FILTERBAR.DAYS_AGO");
            } else if (number > 0) {
                btnObj.innerText =
                    lang == "+" + number + Sepoa.cache.text.get("FILTERBAR.DAYS");
            }
            break;
    }
};

SepoaFilterBarCalendar.prototype._drawQuarterButtons = function(
    btnsContainer
) {
    var lang = Sepoa.info.LANGUAGE;
    var quarterExist = false;
    this.btns.forEach(function(btn) {
        for (var key in btn) {
            if (key == "quarter") {
                quarterExist = true;
                return;
            }
        }
    });
    if (!quarterExist) {
        return;
    }

    var quarters = {
        first: "1",
        second: "2",
        third: "3",
        fourth: "4"
    };
    var container = document.createElement("div");
    container.setAttribute("class", "quarter_btns_container");
    btnsContainer.appendChild(container);

    var quarterBtn = document.createElement("div");
    quarterBtn.setAttribute("class", "quarters_button");
    quarterBtn.classList.add("btn");
    container.appendChild(quarterBtn);

    var quarterSpan = document.createElement("div");
    quarterSpan.setAttribute("class", "span");
    quarterBtn.appendChild(quarterSpan);
    quarterSpan.innerText = Sepoa.cache.text.get("FILTERBAR.QUARTER");

    var quartericon = document.createElement("div");
    quartericon.setAttribute("class", "icon");
    quartericon.classList.add("xi-sepoa-dummy");
    quarterBtn.appendChild(quartericon);

    var quarterBtnCont = document.createElement("div");
    quarterBtnCont.setAttribute("class", "quarters_button_combo_list");
    quarterBtnCont.classList.add("hiding");
    container.appendChild(quarterBtnCont);

    quarterBtn.addEventListener(
        "click",
        function(e) {
            var frame = document.createElement("div");
            frame.setAttribute("class", "calendar_frame");
            this.modal.appendChild(frame);

            frame.addEventListener(
                "click",
                function(e) {
                    frame.parentNode.removeChild(frame);
                    quartericon.classList.toggle("combo_icon_up");
                    this._toggleModal(quarterBtnCont);
                }.bind(this)
            );
            quartericon.classList.toggle("combo_icon_up");
            // this._makeFrameinModal(quarterBtnCont.parentNode);
            this._toggleModal(quarterBtnCont);
        }.bind(this)
    );

    this.btns.forEach(
        function(b) {
            for (var option in b) {
                if (option == "quarter") {
                    for (var key in quarters) {
                        if (b[option] == quarters[key]) {
                            var btn = document.createElement("div");
                            btn.setAttribute("class", "quarter_btn");
                            btn.classList.add(key + "_quarter");
                            btn.classList.add("btn");
                            btn.innerText =
                                lang == "KO" ?
                                quarters[key] + Sepoa.cache.text.get("FILTERBAR.QUARTER") :
                                Sepoa.cache.text.get("FILTERBAR.QUARTER") + quarters[key];
                            btn.quarter = key;
                            btn.option = "quarter";
                            btn.number = quarters[key];
                            quarterBtnCont.appendChild(btn);

                            b.div = btn;
                            btn.addEventListener(
                                "click",
                                function(e) {
                                    var d = this._adjustQuarterBtn(e.currentTarget);
                                    this._setInputDate("from", d.from);
                                    this._setInputDate("to", d.to);
                                    this.fromObj.setDate(d.from);
                                    this.toObj.setDate(d.to);

                                    var selectedBtn = btnsContainer.querySelector(
                                        ".btn_selected"
                                    );
                                    if (selectedBtn) {
                                        selectedBtn.classList.remove("btn_selected");
                                    }
                                    appendClassName(e.currentTarget, "btn_selected");

                                    var frame = this.modal.querySelector(".calendar_frame");
                                    frame.parentNode.removeChild(frame);

                                    this._toggleModal(quarterBtnCont);

                                    quartericon.classList.toggle("combo_icon_up");
                                    quarterSpan.innerText =
                                        e.currentTarget.number +
                                        Sepoa.cache.text.get("FILTERBAR.QUARTER");
                                }.bind(this)
                            );
                        }
                    }
                }
            }
        }.bind(this)
    );
};

SepoaFilterBarCalendar.prototype._adjustQuarterBtn = function(btn) {
    var quarters = {
        1: "first",
        2: "second",
        3: "third",
        4: "fourth"
    };
    var quarter = btn.quarter || quarters[btn.number];
    var now = new Date();
    var fromD;
    var toD;

    switch (quarter) {
        case "first":
            fromD = new Date(now.getFullYear(), 0, 1);
            toD = new Date(now.getFullYear(), 3, 0);
            break;
        case "second":
            fromD = new Date(now.getFullYear(), 3, 1);
            toD = new Date(now.getFullYear(), 6, 0);
            break;
        case "third":
            fromD = new Date(now.getFullYear(), 6, 1);
            toD = new Date(now.getFullYear(), 9, 0);
            break;
        case "fourth":
            fromD = new Date(now.getFullYear(), 9, 1);
            toD = new Date(now.getFullYear(), 12, 0);
            break;
    }
    var result = {};
    result.to = toD;
    result.from = fromD;
    return result;
};

SepoaFilterBarCalendar.prototype._adjustHalfYearBtn = function(btn) {
    var half = btn.half;
    var now = new Date();
    var fromD;
    var toD;
    var result = {};

    switch (half) {
        case "first":
            fromD = new Date(now.getFullYear(), 0, 1);
            toD = new Date(now.getFullYear(), 6, 0);
            break;
        case "second":
            fromD = new Date(now.getFullYear(), 6, 1);
            toD = new Date(now.getFullYear(), 12, 0);
            break;
    }

    result.from = fromD;
    result.to = toD;
    return result;
};

SepoaFilterBarCalendar.prototype._drawHalfYearButtons = function(
    btnsContainer
) {
    var halfExist = false;
    this.btns.forEach(function(btn) {
        for (var key in btn) {
            if (key == "half") {
                halfExist = true;
            }
        }
    });

    if (!halfExist) {
        return;
    }
    var lang = Sepoa.info.LANGUAGE;
    var halfYears = {
        first: "상",
        second: "하"
    };

    var container = document.createElement("div");
    container.setAttribute("class", "halfYear_btns_container");
    btnsContainer.appendChild(container);

    var halfBtn = document.createElement("div");
    halfBtn.setAttribute("class", "halfs_button");
    halfBtn.classList.add("btn");
    container.appendChild(halfBtn);

    var halfSpan = document.createElement("div");
    halfSpan.setAttribute("class", "span");
    halfBtn.appendChild(halfSpan);
    halfSpan.innerText = Sepoa.cache.text.get("FILTERBAR.HALF");

    var halficon = document.createElement("div");
    halficon.setAttribute("class", "icon");
    halficon.classList.add("xi-sepoa-dummy");
    halfBtn.appendChild(halficon);

    var halfBtnCont = document.createElement("div");
    halfBtnCont.setAttribute("class", "halfs_button_combo_list");
    halfBtnCont.classList.add("hiding");
    container.appendChild(halfBtnCont);

    halfBtn.addEventListener(
        "click",
        function(e) {
            var frame = document.createElement("div");
            frame.setAttribute("class", "calendar_frame");
            this.modal.appendChild(frame);

            frame.addEventListener(
                "click",
                function(e) {
                    frame.parentNode.removeChild(frame);
                    halficon.classList.toggle("combo_icon_up");
                    this._toggleModal(halfBtnCont);
                }.bind(this)
            );
            halficon.classList.toggle("combo_icon_up");
            // this._makeFrameinModal(quarterBtnCont.parentNode);
            this._toggleModal(halfBtnCont);
        }.bind(this)
    );

    this.btns.forEach(
        function(b) {
            for (var option in b) {
                if (option == "half") {
                    for (var key in halfYears) {
                        if (b[option] == key) {
                            var btn = document.createElement("div");
                            btn.setAttribute("class", "half_year_btn");
                            btn.classList.add(key + "_year");
                            btn.classList.add("btn");
                            btn.innerText =
                                lang == "KO" ?
                                halfYears[key] + Sepoa.cache.text.get("FILTERBAR.HALF") :
                                key;
                            btn.half = key;
                            btn.option = "half";
                            btn.number = key;
                            halfBtnCont.appendChild(btn);
                            b.div = btn;

                            btn.addEventListener(
                                "click",
                                function(e) {
                                    var d = this._adjustHalfYearBtn(e.currentTarget);
                                    this._setInputDate("from", d.from);
                                    this._setInputDate("to", d.to);
                                    this.fromObj.setDate(d.from);
                                    this.toObj.setDate(d.to);

                                    var selectedBtn = btnsContainer.querySelector(
                                        ".btn_selected"
                                    );
                                    if (selectedBtn) {
                                        selectedBtn.classList.remove("btn_selected");
                                    }
                                    appendClassName(e.currentTarget, "btn_selected");

                                    var frame = this.modal.querySelector(".calendar_frame");
                                    frame.parentNode.removeChild(frame);

                                    this._toggleModal(halfBtnCont);

                                    halficon.classList.toggle("combo_icon_up");
                                    halfSpan.innerText =
                                        halfYears[e.currentTarget.half] +
                                        Sepoa.cache.text.get("FILTERBAR.HALF");
                                }.bind(this)
                            );
                        }
                    }
                }
            }
        }.bind(this)
    );
};

SepoaFilterBarCalendar.prototype._drawButtons = function(btnsContainer) {
    if (this.btns.length < 0) {
        return;
    }
    var btnsArea = document.createElement("div");
    btnsArea.setAttribute("class", "btns_area");
    btnsContainer.appendChild(btnsArea);

    var applyArea = document.createElement("div");
    applyArea.setAttribute("class", "apply_area");
    btnsContainer.appendChild(applyArea);

    if (!this.isForFilter) {
        var applyBtn = document.createElement("div");
        applyBtn.setAttribute("class", "apply_btn");
        applyArea.appendChild(applyBtn);
        applyBtn.innerText = Sepoa.cache.text.get("FILTERBAR.CONFIRM");

        applyBtn.addEventListener(
            "click",
            function() {
                this.obj.callEvent("onSearch", ["filter"]);
                this._closeAllModal();
                this._closeAllFrame();
            }.bind(this)
        );
    }

    this.btns.forEach(
        function(b) {
            for (var key in b) {
                if (key == "half" || key == "quarter" || key == "confirm") {
                    return;
                }

                var btn = document.createElement("div");
                btn.setAttribute("class", "btn");
                appendClassName(btn, "btn_" + key);
                btn.option = key;
                btn.number = b[key];
                b.div = btn;

                this._setBtnInnerText(btn);
                btnsArea.appendChild(btn);

                btn.addEventListener(
                    "click",
                    function(e) {
                        var selectedBtn = btnsArea.querySelector(".btn_selected");

                        if (selectedBtn) {
                            selectedBtn.classList.remove("btn_selected");
                        }
                        appendClassName(e.currentTarget, "btn_selected");
                        var d = this._adjustBtn(btn);
                        this._setInputDate("from", d.from);
                        this._setInputDate("to", d.to);
                        this.fromObj.setDate(d.from);
                        this.toObj.setDate(d.to);
                    }.bind(this)
                );
            }
        }.bind(this)
    );

    this._drawQuarterButtons(btnsArea);
    this._drawHalfYearButtons(btnsArea);
};

SepoaFilterBarCalendar.prototype._adjustBtn = function(btn) {
    var option = btn.option;
    var number = parseInt(btn.number);

    var now = new Date();
    var fromD;
    var toD;

    var result = {};

    switch (option) {
        case "year":
            fromD = new Date(now.getFullYear() + number, 0, 1);
            toD = new Date(now.getFullYear() + 1 + number, 0, 0);
            break;
        case "month":
            fromD = new Date(now.getFullYear(), now.getMonth() + number, 1);
            toD = new Date(now.getFullYear(), now.getMonth() + number + 1, 0);
            break;
        case "week":
            if (number % 4 == 0) {
                if (number == 0) {
                    var days = now.getDay() - 1;
                    now.setDate(now.getDate() - days);
                    fromD = new Date(now.getFullYear(), now.getMonth(), now.getDate());
                    toD = new Date();
                } else {
                    number /= 4;
                    if (number > 0) {
                        fromD = new Date();
                        toD = new Date(now.getFullYear(), now.getMonth() + number, now.getDate());
                    } else {
                        fromD = new Date(now.getFullYear(), now.getMonth() + number, now.getDate());
                        toD = new Date();
                    }
                }
            } else {
                var days = number * 7;
                now.setDate(now.getDate() + days);
                if (days < 0) {
                    fromD = new Date(now.getFullYear(), now.getMonth(), now.getDate());
                    toD = new Date();
                } else {
                    fromD = new Date();
                    toD = new Date(now.getFullYear(), now.getMonth(), now.getDate());
                }
            }
            break;
        case "day":
            if (number == 0) {
                fromD = now;
                toD = now;
            } else {
                now.setDate(now.getDate() + number);
                if (number > 0) {
                    toD = new Date(now.getFullYear(), now.getMonth(), now.getDate());
                    fromD = new Date();
                } else {
                    toD = new Date();
                    fromD = new Date(now.getFullYear(), now.getMonth(), now.getDate());
                }
            }
            break;
        case "quarter":
            result = this._adjustQuarterBtn(btn);
            break;
        case "half":
            result = this._adjustHalfYearBtn(btn);
            break;
    }

    if (
        option == "day" ||
        option == "month" ||
        option == "week" ||
        option == "year"
    ) {
        result.to = toD;
        result.from = fromD;
    }
    return result;
};

SepoaFilterBarCalendar.prototype._drawComboList = function(data) {
    var comboList = document.createElement("div");
    comboList.setAttribute("class", "calendar_combo_list_wrapper");
    comboList.classList.add("hiding");
    comboList.classList.add("filter_modal");
    this.comboListModal = comboList;
    this.body.appendChild(this.comboListModal);

    comboList.innerHTML = "";
    var item = document.createElement("div");
    item.setAttribute("class", "calendar_combo_list_item");
    appendClassName(item, "calendar_selected");
    appendClassName(item, "none");
    item.isSelected = true;
    item.comboText = "";
    item.comboValue = "NONE";

    comboList.appendChild(item);
    this.comboList.push(item);

    this.comboBtn.addEventListener("click", this._openCombo.bind(this));

    for (var idx in data) {
        var t = data[idx];
        var item = document.createElement("div");
        item.comboText = t.text;
        item.comboValue = t.value;
        item.setAttribute("class", "calendar_combo_list_item");
        item.innerText = t.text;
        comboList.appendChild(item);
        item.isSelected = false;

        this.comboList.push(item);
    }

    this.comboList.forEach(
        function(item) {
            item.addEventListener(
                "click",
                function(e) {
                    if (!this.readOnlyFlag) {
                        var icon = this.comboBtn.querySelector(".option_btn");
                        icon.classList.toggle("combo_icon_up");

                        var selectedBtn = comboList.querySelector(".calendar_selected");
                        if (selectedBtn) {
                            selectedBtn.classList.remove("calendar_selected");
                            selectedBtn.isSelected = false;
                        }
                        appendClassName(item, "calendar_selected");
                        this.selectedBtn = item;
                        item.isSelected = true;
                        var span = this.comboBtn.querySelector(".option_span");
                        span.innerText = this.selectedBtn.comboText;

                        e.stopPropagation();
                        this.obj.callEvent("onSearch", ["filter"]);

                        this._toggleModal(comboList);
                        this._closeAllFrame();
                    }
                }.bind(this)
            );
        }.bind(this)
    );
    this._setComboDefault();
    this.comboLoadFinished = true;
};

SepoaFilterBarCalendar.prototype._openCombo = function(e) {
    var icon = this.comboBtn.querySelector(".option_btn");
    icon.classList.toggle("combo_icon_up");

    var lt = sUtils.element.getOffset(e.currentTarget, "module.top");
    var top = lt.top + e.currentTarget.offsetHeight;
    var left = lt.left;
    this._makeFrame(this.body);
    this._toggleModal(this.comboListModal, top, left);
};

SepoaFilterBarCalendar.prototype._getTarget = function() {
    var target;
    if (this.comboLoadFinished) {
        this.comboList.forEach(function(item) {
            if (item.isSelected) {
                target = item.comboValue;
            }
        });
    } else {
        if (this.default) {
            if (this.default.indexOf("#") != -1) {
                var arr = this.default.split("#");
                target = arr[0];
            }
        }
    }
    return target;
};

SepoaFilterBarCalendar.prototype._drawbasic = function() {
    var container = this.cont;
    var type = "calendar";
    var objectDiv = document.createElement("div");
    objectDiv.setAttribute("class", "ssfilter_" + type);
    container.appendChild(objectDiv);
    this.objectDiv = objectDiv;

    if (this.combo != undefined) {
        var optionContainer = document.createElement("div");
        optionContainer.setAttribute("class", "option_container");
        objectDiv.appendChild(optionContainer);
        this.comboBtn = optionContainer;

        var wrapper = document.createElement("div");
        wrapper.setAttribute("class", "option_container_wrapper");
        optionContainer.appendChild(wrapper);

        var span = document.createElement("div");
        span.setAttribute("class", "option_span");
        wrapper.appendChild(span);

        var showListBtn = document.createElement("div");
        showListBtn.setAttribute("class", "option_btn");
        showListBtn.classList.add("xi-sepoa-dummy");
        wrapper.appendChild(showListBtn);

        this._loadComboBox(this.combo);
    }

    if (this.conf.button) {
        var btns = this.conf.button.split(",");

        btns.forEach(
            function(btn) {
                var firstIndex = btns.indexOf(btn);
                var lastIndex = btns.lastIndexOf(btn);
                if (firstIndex != lastIndex) {
                    btns.splice(lastIndex, 1);
                }
                if (btns.indexOf(btn) > 4) {
                    return;
                }
                var d = btn.split(":");
                var btn = {};
                if (
                    d[0] == "year" ||
                    d[0] == "month" ||
                    d[0] == "day" ||
                    d[0] == "week" ||
                    d[0] == "confirm"
                ) {
                    btn[d[0]] = d[1];
                    this.btns.push(btn);
                } else if (d[0] == "quarter") {
                    for (var i = 1; i <= 4; i++) {
                        btn[d[0]] = i;
                        this.btns.push(btn);
                        btn = {};
                    }
                } else if (d[0] == "half") {
                    for (var i = 0; i < 2; i++) {
                        var arr = ["first", "second"];
                        btn[d[0]] = arr[i];
                        this.btns.push(btn);
                        btn = {};
                    }
                } else {
                    console.error("something is wrong in filterbar - buttons");
                }
            }.bind(this)
        );
    }

    var dateContainer = document.createElement("div");
    dateContainer.setAttribute("class", "date_container");
    objectDiv.appendChild(dateContainer);

    var fromDate = document.createElement("input");
    fromDate.setAttribute("class", "from_date");
    fromDate.classList.add("date");
    fromDate.classList.add("clickable");
    this.fromInput = fromDate;

    var toDate = document.createElement("input");
    toDate.setAttribute("class", "to_date");
    toDate.classList.add("date");
    toDate.classList.add("clickable");
    this.toInput = toDate;

    var toChar = document.createElement("div");
    toChar.setAttribute("class", "to_char");
    toChar.innerText = "~";

    dateContainer.appendChild(fromDate);
    dateContainer.appendChild(toChar);
    dateContainer.appendChild(toDate);

    var calendarBtn = document.createElement("div");
    calendarBtn.setAttribute("class", "calendar_icon");
    calendarBtn.classList.add("clickable");
    calendarBtn.classList.add("xi-sepoa-dummy");

    objectDiv.appendChild(calendarBtn);

    if (dateContainer.parentNode.parentNode.classList.contains("filter_area")) {
        this.isForFilter = true;
    }

    this._drawCalendar(dateContainer);

    fromDate.addEventListener(
        "blur",
        function() {
            var s = this.fromInput.value;
            var n = "";
            var d = dhx.str2date(s, "%Y%m%d");
            var now = new Date();

            if (s == "") {
                return;
            }

            if (d != "Invalid Date") {
                n = dhx.date2str(d, Sepoa.info.DATE_FORMAT);
            } else {
                d = dhx.str2date(s, Sepoa.info.DATE_FORMAT);
                if (d != "Invalid Date") {
                    n = dhx.date2str(d, Sepoa.info.DATE_FORMAT);
                } else {
                    n = dhx.date2str(now, Sepoa.info.DATE_FORMAT);
                    this.toInput.value = n;
                    this.toObj.setDate(d);
                }
            }

            if (d > this.toObj.getDate()) {
                this.toInput.value = n;
                this.toObj.setDate(d);
                this.fromObj.setSensitiveRange(null, d);
            }
            this.fromInput.value = n;
            this.fromObj.setDate(d);
            this.toObj.setSensitiveRange(d, null);
        }.bind(this)
    );

    toDate.addEventListener(
        "blur",
        function() {
            var s = this.toInput.value;
            if (s == "") {
                return;
            }
            var n = "";
            var d = dhx.str2date(s, "%Y%m%d");
            var now = new Date();
            if (d != "Invalid Date") {
                n = dhx.date2str(d, Sepoa.info.DATE_FORMAT);
            } else {
                d = dhx.str2date(s, Sepoa.info.DATE_FORMAT);
                if (d != "Invalid Date") {
                    n = dhx.date2str(d, Sepoa.info.DATE_FORMAT);
                } else {
                    n = dhx.date2str(now, Sepoa.info.DATE_FORMAT);
                    this.fromInput.value = n;
                    this.fromObj.setDate(d);
                }
            }

            if (d < this.fromObj.getDate()) {
                this.fromInput.value = n;
                this.fromObj.setDate(d);
                this.toObj.setSensitiveRange(d, null);
            }
            this.toInput.value = n;
            this.toObj.setDate(d);
            this.fromObj.setSensitiveRange(null, d);
        }.bind(this)
    );

    var clickableBtns = objectDiv.querySelectorAll(".clickable");
    clickableBtns.forEach(
        function(btn) {
            btn.addEventListener(
                "click",
                function(e) {
                    if (!this.readOnlyFlag) {
                        var lt = sUtils.element.getOffset(dateContainer, "module.top");
                        var top = lt.top + dateContainer.offsetHeight;
                        var left = lt.left;

                        var calendarContainer = this.modal;
                        if (this.body.offsetWidth < left + this.modalWidth) {
                            left = this.body.offsetWidth - this.modalWidth - 10;
                        }

                        if (calendarContainer.classList.contains("in_modal")) {
                            this._makeFrameinModal(calendarContainer);
                            this._toggleModal(calendarContainer, top, left);
                        } else {
                            this._makeFrame(this.body);
                            this._toggleModal(calendarContainer, top, left);
                        }
                        this.fromCalendar.style.display = "block";
                        this.toCalendar.style.display = "block";
                    }
                }.bind(this)
            );
        }.bind(this)
    );

    window.addEventListener(
        "resize",
        function(params) {
            if (
                this.body.offsetWidth <
                dateContainer.getBoundingClientRect().left + this.modalWidth
            ) {
                this.modal.style.left =
                    this.body.offsetWidth - this.modalWidth - 10 + "px";
            } else {
                this.modal.style.left =
                    dateContainer.getBoundingClientRect().left + "px";
            }
        }.bind(this)
    );
};

SepoaFilterBarCalendar.prototype._onModalClose = function() {

    var fromValue = this.fromInput.value;
    var toValue = this.toInput.value;

    if (fromValue == "" && toValue == "") {
        this._deleteCondition(this.id, this.id);
    } else {
        var selectedDate = document.createElement("div");
        selectedDate.setAttribute("class", "selected_date");
        selectedDate.id = this.id + "_calendar";
        selectedDate.valueText = fromValue + " ~ " + toValue;
        selectedDate.filterLabel = this.label;
        selectedDate.filterType = this.type;
        selectedDate.filterId = this.id;

        this._drawConditionBox([selectedDate], this.id);
    }
    this.obj._conditionUpdate();
};

SepoaFilterBarCalendar.prototype._drawCalendar = function(c) {
    var calendarContainer = document.createElement("div");
    calendarContainer.setAttribute("class", "calendar_container");
    calendarContainer.classList.add("filter_modal");

    var dcd = new dhtmlXDoubleCalendar(calendarContainer);
    var clearSpans = calendarContainer.querySelectorAll(
        ".dhtmlxcalendar_label_clear"
    );
    clearSpans[0].innerText = Sepoa.cache.text.get("FILTERBAR.RESET");
    clearSpans[1].innerText = Sepoa.cache.text.get("FILTERBAR.RESET");

    var btnsContainer = document.createElement("div");
    btnsContainer.setAttribute("class", "btns_container");
    calendarContainer.appendChild(btnsContainer);

    this._drawButtons(btnsContainer);

    var calendars = calendarContainer.querySelectorAll(
        ".dhtmlxcalendar_material"
    );
    this.fromCalendar = calendars[0];
    this.toCalendar = calendars[1];

    this.fromObj = dcd.leftCalendar;
    this.toObj = dcd.rightCalendar;

    this.body.appendChild(calendarContainer);
    this.modal = calendarContainer;
    this.modalWidth = this.modal.offsetWidth;

    if (c.parentNode.parentNode.classList.contains("filter_area")) {
        calendarContainer.classList.add("in_modal");
        calendarContainer.classList.add("filter_hiding");
    } else {
        calendarContainer.classList.add("hiding");
    }

    this.fromObj.attachEvent(
        "onClick",
        function(date) {
            this._setInputDate("from", date);
            this.obj.callEvent("onCalendarButtonClick", [
                this.fromId,
                this.getValue()[this.fromId],
            ]);
        }.bind(this)
    );

    this.toObj.attachEvent(
        "onClick",
        function(date) {
            this._setInputDate("to", date);
            this.obj.callEvent("onCalendarButtonClick", [
                this.toId,
                this.getValue()[this.toId],
            ]);
        }.bind(this)
    );

    this.fromObj.attachEvent(
        "onButtonClick",
        function(btn) {
            if (btn == null) {
                // clear btn
                this.fromInput.value = this.fromObj.getDate(true);
                this.toObj.clearSensitiveRange()
            } else {
                // today btn
                this.fromInput.value = dhx.date2str(
                    btn,
                    Sepoa.info.DATE_FORMAT || "%Y/%m/%d"
                );
                var date = this.fromObj.getDate();
                date.setHours(0, 0, 0);
                this.toObj.setSensitiveRange(date, null);
            }
        }.bind(this)
    );

    this.toObj.attachEvent(
        "onButtonClick",
        function(btn) {
            if (btn == null) {
                // clear btn
                this.toInput.value = this.toObj.getDate(true);
                this.fromObj.clearSensitiveRange()
            } else {
                // today btn
                this.toInput.value = dhx.date2str(
                    btn,
                    Sepoa.info.DATE_FORMAT || "%Y/%m/%d"
                );
                var date = this.toObj.getDate();
                date.setHours(24, 0, 0);
                this.fromObj.setSensitiveRange(null, date);
            }
        }.bind(this)
    );
    this.restoreDefault();
};

SepoaFilterBarCalendar.prototype._setInputDate = function(side, date) {
    if (side == "from") {
        this.fromInput.value = dhx.date2str(
            date,
            Sepoa.info.DATE_FORMAT || "%Y/%m/%d"
        );
    } else {
        this.toInput.value = dhx.date2str(
            date,
            Sepoa.info.DATE_FORMAT || "%Y/%m/%d"
        );
    }
};
SepoaFilterBarCalendar.prototype._deleteConditionByTarget = function() {
    this.clear();
};

/* text */
var SepoaFilterBarText = function(_conf, _itemCont, fo) {
    this.objectDiv = _itemCont;
    this.obj = fo;
    this.id = _conf.id;
    this.default = _conf.default;
    this.inputBar;
    this.label = _conf.label;
    this.type = "text";
    this.readOnlyFlag = false;
    this._drawbasic();
};
SepoaFilterBarText.prototype = Object.create(SepoaFilterBarFilter.prototype);

SepoaFilterBarText.prototype.setDefault = function(df) {
    this.default = df;
};

SepoaFilterBarText.prototype.getDefault = function() {
    return this.default;
};

SepoaFilterBarText.prototype.getValue = function() {
    return this.inputBar.value;
};

SepoaFilterBarText.prototype.setValue = function(v) {
    this.inputBar.value = v;
    this.inputBar.valueText = v;
    this._drawConditionBox([this.inputBar], this.id);
    if (this.inputBar.value.length == 0 || this.inputBar.value == "") {
        this._deleteCondition(this.id, this.id);
    }
};

SepoaFilterBarText.prototype.setReadOnly = function(b) {
    if (b == true) {
        this.inputBar.disabled = true;
        this.readOnlyFlag = true;
    } else if (b == false) {
        this.inputBar.disabled = false;
        this.readOnlyFlag = false;
    } else {
        console.log("Filterbar Error : setReadOnly() needs boolean as parameter.");
    }
};

SepoaFilterBarText.prototype.isReadOnly = function(v) {
    return this.readOnlyFlag;
};

SepoaFilterBarText.prototype._getFilterData = function(v) {
    var result = [];
    var r = {};
    r[this.id] = this.inputBar.value;
    result.push(r);
    return result;
};

SepoaFilterBarText.prototype._drawbasic = function() {
    var cont = this.objectDiv;
    var filterArea = cont.querySelector(".filter_area");
    var inputArea = document.createElement("div");
    inputArea.setAttribute("class", "input_area");
    filterArea.appendChild(inputArea);
    var inputBar = document.createElement("input");
    inputBar.setAttribute("class", "input_bar");
    inputArea.appendChild(inputBar);
    inputBar.dataset.id = this.id;
    inputBar.filterLabel = this.label;
    inputBar.filterType = this.type;
    inputBar.filterId = this.id;
    this.inputBar = inputBar;

    this.inputBar.addEventListener(
        "keyup",
        function(e) {
            this.inputBar.valueText = this.inputBar.value;
            if (this.inputBar.value.length == 0) {
                this._deleteCondition(this.id, this.id);
            } else {
                this._drawConditionBox([this.inputBar], this.id);
            }
            this.obj._conditionUpdate();
        }.bind(this)
    );

    this.restoreDefault();
};

SepoaFilterBarText.prototype.restoreDefault = function() {
    this.clear();
    if (this.default) {
        var ib = this.inputBar;
        ib.value = this.default;
        ib.valueText = this.default;
        this._drawConditionBox([ib], this.id);
    }
};

SepoaFilterBarText.prototype._deleteConditionByTarget = function() {
    this.clear();
};

SepoaFilterBarText.prototype.clear = function() {
    this.clearConditionById(this.id);
    this.setValue("");
};

/* select */
var SepoaFilterBarSelect = function(_conf, _itemCont, fo) {
    this.objectDiv = _itemCont;
    this.obj = fo;
    this.id = _conf.id;
    this.default;
    this.setDefault(_conf.default);
    this.combo = _conf.combo;
    this.label = _conf.label;
    this.comboLoadFinished = false;
    this.readOnlyFlag = false;
    this.body = SUX.getModuleTop().document.body;
    this.type = "select";
    this.tempValue;
    this._drawbasic();
};

SepoaFilterBarSelect.prototype = Object.create(SepoaFilterBarFilter.prototype);

SepoaFilterBarSelect.prototype.setCombo = function(c) {
    this.combo = c;
};
SepoaFilterBarSelect.prototype.getCombo = function(c) {
    return this.combo;
};
SepoaFilterBarSelect.prototype.setDefault = function(value) {
    if (value && value != "") {
        if (value.indexOf("{") != -1) {
            var code = value.substr(1, value.length - 2);
            this.default = Sepoa.info[code];
        } else {
            this.default = value;
        }
    }
};
SepoaFilterBarSelect.prototype.getDefault = function() {
    return this.default;
};

SepoaFilterBarSelect.prototype.setValue = function(v) {
    this.tempValue = v;
    if (this.inputs) {
        this._setValue(v);
    }
};

SepoaFilterBarSelect.prototype._setValue = function(code) {
    this.clear();
    if (code) {
        var codeArr = code.split(",");
        var targetArr = [];
        this.inputs.forEach(function(item) {
            var id = item.getAttribute("data-id");
            codeArr.forEach(function(code) {
                if (code.toLowerCase() == id.toLowerCase()) {
                    targetArr.push(item);
                }
            });
        });
        if (targetArr.length != 0) {
            targetArr.forEach(function(item) {
                item.checked = true;
            });
            this._setSelectedItem();
        }
    }
};
SepoaFilterBarSelect.prototype.getValue = function() {
    var values = [];
    var allInputs = this.inputs;
    if (allInputs) {
        allInputs.forEach(function(input) {
            var _id = input.getAttribute("data-id");
            if (input.checked) {
                values.push(_id);
            }
        });
        if (values.length > 0) {
            return values.join(",");
        }
    }
};

SepoaFilterBarSelect.prototype.setReadOnly = function(b) {
    if (b == true) {
        this.readOnlyFlag = true;
        this._setReadOnly(false);
    } else if (b == false) {
        this.readOnlyFlag = false;
        this._setReadOnly(false);
    } else {
        console.log("Filterbar Error : setReadOnly() needs boolean as parameter.");
    }
};

SepoaFilterBarSelect.prototype._setReadOnly = function(b) {
    if (this.inputs) {
        if (b) {
            this.inputs.forEach(
                function(input) {
                    input.disabled = true;
                }.bind(this)
            );
            this.selectAllBtn.disabled = true;
        } else {
            this.inputs.forEach(
                function(input) {
                    input.disabled = false;
                }.bind(this)
            );
            this.selectAllBtn.disabled = false;
        }
    }
};

SepoaFilterBarSelect.prototype.isReadOnly = function() {
    return this.readOnlyFlag;
};

SepoaFilterBarSelect.prototype.reloadCombo = function(combo) {
    this.clear();
    this.combo = combo;
    this.default = undefined;
    this.comboLoadFinished = false;
    this._loadComboBox(this.combo);
};

SepoaFilterBarSelect.prototype.clear = function() {
    this.clearConditionById(this.id);
    var inputs = this.inputs;
    if (inputs) {
        inputs.forEach(function(input) {
            input.checked = false;
        });
        var selectedItemCont = this.objectDiv.querySelector(
            ".selected_item_container"
        );
        while (selectedItemCont.hasChildNodes()) {
            selectedItemCont.removeChild(selectedItemCont.firstChild);
        }
    }
};

SepoaFilterBarSelect.prototype._getFilterData = function() {
    var result = [];
    var r = {};
    if (this.comboLoadFinished) {
        r[this.id] = this.getValue();
        result.push(r);
        return result;
    } else {
        r[this.id] = this.tempValue || this.default;
        result.push(r);
        return result;
    }
};

SepoaFilterBarSelect.prototype._drawbasic = function(t, filter) {
    var cont = this.objectDiv;
    var filterArea = cont.querySelector(".filter_area");

    var checkCombo = document.createElement("div");
    checkCombo.setAttribute("class", "check_combo");
    checkCombo.id = this.id;
    filterArea.appendChild(checkCombo);
    this.comboBtn = checkCombo;

    var spanNiconWrapper = document.createElement("div");
    spanNiconWrapper.setAttribute("class", "filter_modal_icon_span_wrapper");
    checkCombo.appendChild(spanNiconWrapper);
    var span = document.createElement("span");
    span.setAttribute("class", "span");
    var icon = document.createElement("div");
    icon.setAttribute("class", "icon");
    icon.classList.add("xi-sepoa-dummy");
    spanNiconWrapper.appendChild(span);
    spanNiconWrapper.appendChild(icon);
    this.comboListModal = spanNiconWrapper;

    this._loadComboBox(this.combo);

    var comboListWrapper = document.createElement("div");
    comboListWrapper.setAttribute("class", "select_combo_list_wrapper");

    var seletedItemContainer = document.createElement("div");
    seletedItemContainer.setAttribute("class", "selected_item_container");
    filterArea.appendChild(seletedItemContainer);

    spanNiconWrapper.addEventListener(
        "click",
        function(e) {
            var icon = this.comboListModal.querySelector(".icon");
            icon.classList.toggle("combo_icon_up");
            var comboList = this.comboList;

            var top = spanNiconWrapper.getBoundingClientRect().top;
            var left = spanNiconWrapper.getBoundingClientRect().left;

            if (comboList) {
                this._makeFrameinModal(comboList);
                this._toggleModal(comboList, top, left);
            } else {
                console.error(
                    (this.obj.SepoaObject || Sepoa).common.SCREEN_ID +
                    " : Filterbar-" +
                    this.id +
                    "-combo"
                );
            }
        }.bind(this)
    );
};

SepoaFilterBarSelect.prototype._drawComboList = function(data) {
    var l = this.label;

    var comboList = document.createElement("div");
    comboList.setAttribute("class", "filter_combo_list_wrapper");
    comboList.classList.add("filter_hiding");
    comboList.classList.add("in_modal");
    comboList.classList.add("filter_modal");

    this.body.appendChild(comboList);
    this.comboList = comboList;

    comboList.innerHTML = "";
    var item = document.createElement("div");
    item.setAttribute("class", "filter_combo_list_buttons");

    var input = document.createElement("input");
    input.setAttribute("placeholder", Sepoa.cache.text.get("FILTERBAR.SEARCH"));
    item.appendChild(input);
    this.input = input;


    var selectAllCont = document.createElement("div");
    selectAllCont.setAttribute("class", "select_all_cont");
    item.appendChild(selectAllCont);

    var selectAllBtn = document.createElement("input");
    selectAllBtn.setAttribute("type", "checkbox");
    selectAllCont.appendChild(selectAllBtn);
    this.selectAllBtn = selectAllBtn;

    var span = document.createElement("span");
    span.innerText = Sepoa.cache.text.get("FILTERBAR.SELECT_ALL");
    selectAllCont.appendChild(span);

    this.selectAllBtn.addEventListener(
        "click",
        function(e) {
            var combowrapper = e.currentTarget.parentNode.parentNode.parentNode;
            var itemWrapper = combowrapper.querySelector(".item_wrapper");
            var inputList = itemWrapper.querySelectorAll("input");

            if (!this.selectAllBtn.checked) {
                for (var i = 0; i < inputList.length; i++) {
                    inputList[i].checked = false;
                }
            } else if (this.selectAllBtn.checked) {
                for (var i = 0; i < inputList.length; i++) {
                    inputList[i].checked = true;
                }
            }
            this._setSelectedItem(this.id, this.comboBtn);
        }.bind(this)
    );

    comboList.appendChild(item);
    // input search기능 테스트
    var itemWrapper = document.createElement("div");
    itemWrapper.setAttribute("class", "item_wrapper");
    comboList.appendChild(itemWrapper);

    var resultArr = [];


    var timeout = null;
    this.input.addEventListener("keyup", function() {
        clearTimeout(timeout);
        timeout = setTimeout(function() {
            var keyword = input.value.toUpperCase();
            $(itemWrapper).find('.filter_combo_list_item').removeClass("filter_combo_list_item_hidden");
            if (keyword) {
                var r = $(itemWrapper).find('.filter_combo_list_item');
                for (var i = 0; i < r.length; i++) {
                    if (r[i].comboText.toUpperCase().indexOf(keyword) < 0) {
                        $(r[i]).addClass('filter_combo_list_item_hidden');
                    }
                }
            }
        }, 300);
    });


    var allInput = [];

    for (var idx in data) {
        var itemObj = {};
        var t = data[idx];
        var item = document.createElement("div");
        item.setAttribute("class", "filter_combo_list_item");
        var checkBox = document.createElement("input");
        checkBox.setAttribute("type", "checkbox");
        checkBox.dataset.id = t.value;
        checkBox.valueText = t.text;
        checkBox.filterType = "select";
        checkBox.filterLabel = l;

        var label = document.createElement("div");
        label.dataset.id = t.value;
        label.setAttribute("class", "label");
        label.innerText = t.text;

        item.appendChild(checkBox);
        item.appendChild(label);

        item.comboText = t.text;
        item.comboValue = t.value;

        itemWrapper.appendChild(item);

        allInput.push(checkBox);

        itemObj.value = t.value;
        itemObj.text = t.text;
        itemObj.div = item;
        resultArr.push(itemObj);

        checkBox.addEventListener(
            "click",
            function(e) {
                if (!this.readOnlyFlag) {
                    this._setSelectedItem();
                }
            }.bind(this)
        );
        label.addEventListener(
            "click",
            function(e) {
                if (!this.readOnlyFlag) {
                    var cb = e.currentTarget.parentNode.querySelector("input");
                    if (cb.checked) {
                        cb.checked = false;
                    } else {
                        cb.checked = true;
                    }
                    this._setSelectedItem();
                }
            }.bind(this)
        );
    }
    this.inputs = allInput;

    if (selectAllBtn.checked) {
        var inputList = itemWrapper.querySelectorAll("input");
        for (var i = 0; i < inputList.length; i++) {
            inputList[i].checked = false;
        }
    }
    this.clear();
    this.comboLoadFinished = true;
    this._setReadOnly(this.readOnlyFlag);
    this.restoreDefault();
    if (this.tempValue) {
        this._setValue(this.tempValue);
    }
    this.obj.callEvent("onComboLoad", [this.id, this.inputs]);
};

SepoaFilterBarSelect.prototype.restoreDefault = function() {
    this.clear();
    if (this.default) {
        this._setValue(this.default);
    }
};

SepoaFilterBarSelect.prototype._deleteConditionByTarget = function(condition) {
    var box = condition.parentNode;
    var conditionCount = box.childElementCount - 1;
    if (conditionCount == 0) {
        box.parentNode.removeChild(box);
    }
    condition.parentNode.removeChild(condition);

    var target = condition.getAttribute("target");
    var allInputs = this.inputs;
    allInputs.forEach(function(input) {
        var _id = input.getAttribute("data-id");
        if (_id == target) {
            input.checked = false;
            return;
        }
    });
    var selectedItemCont = this.objectDiv.querySelector(
        ".selected_item_container"
    );
    var items = selectedItemCont.querySelectorAll(".selected_item");
    items.forEach(function(item) {
        var t = item.getAttribute("target");
        if (target == t) {
            selectedItemCont.removeChild(item);
            return;
        }
    });
};


SepoaFilterBarSelect.prototype._setSelectedItem = function() {
    var allInputs = this.inputs;
    var selectedInputs = [];
    allInputs.forEach(function(input) {
        if (input.checked) {
            selectedInputs.push(input);
        }
    });

    var selectedItemsContainer = this.objectDiv.querySelector(
        ".selected_item_container"
    );
    selectedItemsContainer.innerHTML = "";

    this._drawConditionBox(selectedInputs, this.id);

    selectedInputs.forEach(
        function(input) {
            var selected = document.createElement("div");
            selected.setAttribute("class", "selected_item");
            var id = input.getAttribute("data-id");
            selected.setAttribute("target", id);

            var span = document.createElement("span");
            span.setAttribute("class", "span");
            span.innerText = input.valueText;
            selected.appendChild(span);

            var clearBtn = document.createElement("div");
            clearBtn.setAttribute("class", "clear_btn");
            clearBtn.classList.add("xi-sepoa-dummy");
            selected.appendChild(clearBtn);

            selectedItemsContainer.appendChild(selected);

            clearBtn.addEventListener(
                "click",
                function(e) {
                    if (!this.readOnlyFlag) {
                        var item = e.currentTarget.parentNode;
                        var targetId = item.getAttribute("target");
                        allInputs.forEach(function(input) {
                            var dataId = input.getAttribute("data-id");
                            if (dataId == targetId) {
                                input.checked = false;
                                return;
                            }
                        });
                        selectedItemsContainer.removeChild(item);
                        this._deleteCondition(this.id, targetId);
                        this.obj._conditionUpdate();
                    }
                }.bind(this)
            );
        }.bind(this)
    );
};

SepoaFilterBarSelect.prototype._onModalClose = function() {
    this.obj.callEvent("onChange", [this.id, this.getValue()]);
};

var SepoaFilterBarCheckbox = function(_conf, _itemCont, fo) {
    this.objectDiv = _itemCont;
    this.obj = fo;
    this.id = _conf.id;
    this.default = _conf.default;
    this.combo = _conf.combo;
    this.label = _conf.label;
    this.comboLoadFinished = false;
    this.readOnlyFlag = false;
    this.tempValue;
    this.body = SUX.getModuleTop().document.body;
    this.type = "checkbox";
    this._drawbasic();
};

SepoaFilterBarCheckbox.prototype = Object.create(
    SepoaFilterBarFilter.prototype
);
SepoaFilterBarCheckbox.prototype.setCombo = function(c) {
    this.combo = c;
};
SepoaFilterBarCheckbox.prototype.getCombo = function() {
    return this.combo;
};
SepoaFilterBarCheckbox.prototype.setDefault = function(d) {
    this.default = d.toUpperCase();
};
SepoaFilterBarCheckbox.prototype.getDefault = function(d) {
    return this.default;
};

SepoaFilterBarCheckbox.prototype.setValue = function(v) {
    this.tempValue = v;
    if (this.inputs) {
        this._setValue(v);
    }
};

SepoaFilterBarCheckbox.prototype._setValue = function(v) {
    this.clear();
    if (v) {
        var dfArr = v.split(",");
        var targetArr = [];
        this.inputs.forEach(function(item) {
            var id = item.getAttribute("data-id");
            dfArr.forEach(function(df) {
                if (df.toLowerCase() == id.toLowerCase()) {
                    targetArr.push(item);
                }
            });
        });

        if (targetArr.length != 0) {
            targetArr.forEach(function(item) {
                item.checked = true;
            });
            this._drawConditionBox(targetArr, this.id);
        }
    }
};
SepoaFilterBarCheckbox.prototype.getValue = function() {
    var values = [];
    var allInputs = this.inputs;
    if (allInputs) {
        allInputs.forEach(function(input) {
            if (input.checked) {
                values.push(input.value);
            }
        });
        if (values.length > 0) {
            return values.join(",");
        }
    }
};

SepoaFilterBarCheckbox.prototype.setReadOnly = function(b) {
    if (b == true) {
        this.readOnlyFlag = true;
        this._setReadOnly(true);
    } else if (b == false) {
        this.readOnlyFlag = false;
        this._setReadOnly(false);
    }
};

SepoaFilterBarCheckbox.prototype._setReadOnly = function(b) {
    if (this.inputs) {
        if (b == true) {
            this.inputs.forEach(
                function(input) {
                    input.disabled = true;
                }.bind(this)
            );
        } else if (b == false) {
            this.inputs.forEach(
                function(input) {
                    input.disabled = false;
                }.bind(this)
            );
        }
    }
};
SepoaFilterBarCheckbox.prototype.isReadOnly = function() {
    return this.readOnlyFlag;
};

SepoaFilterBarCheckbox.prototype.reloadCombo = function(c) {
    this.clear();
    this.combo = c;
    this.default = undefined;
    this.comboLoadFinished = false;
    this._loadComboBox(c);
};

SepoaFilterBarCheckbox.prototype.clear = function() {
    this.clearConditionById(this.id);
    var allInputs = this.checkArea.querySelectorAll("input");
    if (allInputs) {
        allInputs.forEach(function(input) {
            input.checked = false;
        });
    }
};

SepoaFilterBarCheckbox.prototype._getFilterData = function() {
    var result = [];
    var r = {};
    if (this.comboLoadFinished) {
        var values = [];
        var allInputs = this.inputs;
        if (allInputs) {
            allInputs.forEach(function(input) {
                if (input.checked) {
                    values.push(input.value);
                }
            });
            if (values.length > 0) {
                r[this.id] = values.join(",");
                result.push(r);
            }
        }
        return result;
    } else {
        r[this.id] = this.tempValue || this.default;
        result.push(r);
        return result;
    }
};

SepoaFilterBarCheckbox.prototype._drawbasic = function() {
    var combo = this.getCombo();
    var filterArea = this.objectDiv.querySelector(".filter_area");
    var checkArea = document.createElement("div");
    checkArea.setAttribute("class", "check_box_area");
    filterArea.appendChild(checkArea);
    this.checkArea = checkArea;

    this._loadComboBox(combo);
};

SepoaFilterBarCheckbox.prototype._drawComboList = function(data) {
    var checkArea = this.checkArea;
    checkArea.innerHTML = "";

    this.inputs = [];
    data.forEach(
        function(item) {
            var checkItemCont = document.createElement("div");
            checkItemCont.setAttribute("class", "checkbox_item");
            checkArea.appendChild(checkItemCont);

            var span = document.createElement("span");
            span.setAttribute("class", "span");
            checkItemCont.appendChild(span);
            span.innerText = item.text;

            var checkBox = document.createElement("input");
            checkBox.setAttribute("class", "check_box");
            checkBox.setAttribute("type", "checkbox");
            checkBox.dataset.id = item.value;
            checkBox.value = item.value;
            checkBox.valueText = item.text;
            checkBox.filterType = "checkbox";
            checkBox.filterLabel = this.label;
            checkItemCont.appendChild(checkBox);

            this.inputs.push(checkBox);

            checkBox.addEventListener(
                "click",
                function(e) {
                    this.obj.callEvent("onChange", [this.id, this.getValue()]);
                    var inputs = this.inputs;
                    var allcheckedInputs = [];
                    inputs.forEach(function(input) {
                        if (input.checked == true) {
                            allcheckedInputs.push(input);
                        }
                    });

                    this._drawConditionBox(allcheckedInputs, this.id);
                }.bind(this)
            );
        }.bind(this)
    );
    this.comboLoadFinished = true;
    this._setReadOnly(this.readOnlyFlag);
    this.restoreDefault();
    if (this.tempValue) {
        this._setValue(this.tempValue);
    }
    this.obj.callEvent("onComboLoad", [this.id, this.inputs]);
};

SepoaFilterBarCheckbox.prototype.restoreDefault = function() {
    this.clear();
    if (this.default) {
        this._setValue(this.default);
    }
};

SepoaFilterBarCheckbox.prototype._deleteConditionByTarget = function(
    condition
) {
    var conditionCount = condition.parentNode.childElementCount - 1;
    var box = condition.parentNode;
    if (conditionCount == 0) {
        box.parentNode.removeChild(box);
    }
    condition.parentNode.removeChild(condition);

    var target = condition.getAttribute("target");
    var checkboxs = this.objectDiv.querySelectorAll(
        ".check_box_area input[type='checkbox']:checked"
    );
    checkboxs.forEach(function(checkbox) {
        var _id = checkbox.getAttribute("data-id");
        if (_id == target) {
            checkbox.checked = false;
            return;
        }
    });
};


var SepoaFilterBarFlag = function(_conf, _itemCont, fo) {
    this.objectDiv = _itemCont;
    this.obj = fo;
    this.id = _conf.id;
    this.default = _conf.default;
    this.label = _conf.label;
    this.uncheckedValue = _conf.unchecked || "N";
    this.checkedValue = _conf.checkedValue || "Y";
    this.type = "flag";
    this.readOnlyFlag = false;
    this._drawbasic();
};
SepoaFilterBarFlag.prototype = Object.create(SepoaFilterBarFilter.prototype);
SepoaFilterBarFlag.prototype.setDefault = function(d) {
    this.default = d;
};
SepoaFilterBarFlag.prototype.getDefault = function() {
    return this.default;
};
SepoaFilterBarFlag.prototype.setValue = function(b) {
    if (b) {
        this.checkbox.checked = true;
        this._drawConditionBox([this.checkbox], this.id);
    } else {
        this.clear()
    }
};
SepoaFilterBarFlag.prototype.getValue = function() {
    if (this.checkbox.checked) {
        return this.checkedValue;
    } else {
        return this.uncheckedValue;
    }
};
SepoaFilterBarFlag.prototype.setReadOnly = function(b) {
    if (arguments.length == 0) {
        console.error(
            "Filterbar Error : setReadOnly() needs boolean as parameter."
        );
        return;
    }
    if (b) {
        this.checkbox.disabled = true;
        this.readOnlyFlag = true;
    } else {
        this.checkbox.disabled = false;
        this.readOnlyFlag = false;
    }
};
SepoaFilterBarFlag.prototype.isReadOnly = function() {
    return this.readOnlyFlag;
};

SepoaFilterBarFlag.prototype.clear = function() {
    this.clearConditionById(this.id);
    var cb = this.checkbox;
    cb.checked = false;
};

SepoaFilterBarFlag.prototype._getFilterData = function() {
    var checkBox = this.checkbox;
    var result = [];
    var r = {};
    if (checkBox.checked) {
        r[this.id] = this.checkedValue;
    } else {
        r[this.id] = this.uncheckedValue;
    }
    result.push(r);
    return result;
};
SepoaFilterBarFlag.prototype._drawbasic = function() {
    var cont = this.objectDiv;
    var filterArea = cont.querySelector(".filter_area");

    var checkArea = document.createElement("div");
    checkArea.setAttribute("class", "check_area");
    filterArea.appendChild(checkArea);

    var checkBox = document.createElement("input");
    checkBox.setAttribute("type", "checkbox");
    checkBox.dataset.id = this.id;
    checkBox.valueText = this.checkedValue;
    checkBox.filterType = "flag";
    checkBox.filterLabel = this.label;
    this.checkbox = checkBox;

    checkArea.appendChild(checkBox);

    checkBox.addEventListener(
        "click",
        function(e) {
            if (this.checkbox.checked) {
                this.obj.callEvent("onChange", [this.id, this.checkedValue]);
                this._drawConditionBox([checkBox], this.id);
            } else {
                this.obj.callEvent("onChange", [this.id, this.uncheckedValue]);
                this.clearConditionById(this.id);
            }
        }.bind(this)
    );
    this.restoreDefault();
};
SepoaFilterBarFlag.prototype.restoreDefault = function() {
    this.clear();
    if (this.default == "checked") {
        var cb = this.checkbox;
        cb.checked = true;
        this._drawConditionBox([cb], this.id);
    }
};
SepoaFilterBarFlag.prototype._deleteConditionByTarget = function() {
    this.clear();
};

/* popup */
var SepoaFilterBarPopup = function(_conf, _itemCont, fo) {
    this.objectDiv = _itemCont;
    this.obj = fo;
    this.id = _conf.id;
    this.default;
    this.label = _conf.label;
    this.value = _conf.p_value;
    this.code = _conf.p_code;
    this.type = "popup";
    this.readOnlyFlag = false;
    this._drawbasic();
};
SepoaFilterBarPopup.prototype = Object.create(SepoaFilterBarFilter.prototype);

SepoaFilterBarPopup.prototype.setValue = function(code, text) {
    this.filterPopupCallbackFunction(code, text);
};
SepoaFilterBarPopup.prototype.getValue = function() {
    var selectedItem = this.objectDiv.querySelector(".selected_item");
    if (selectedItem) {
        return selectedItem.value;
    }
};
SepoaFilterBarPopup.prototype.reloadPopup = function(code, value) {
    this.clear();
    this.code = code;
    this.value = value;
    this._loadPopup(this.code, this.value);
};
SepoaFilterBarPopup.prototype.setReadOnly = function(b) {
    if (arguments.length == 0) {
        console.error(
            "Filterbar Error : setReadOnly() needs boolean as parameter."
        );
        return;
    }
    if (b) {
        this.readOnlyFlag = true;
    } else {
        this.readOnlyFlag = false;
    }
};
SepoaFilterBarPopup.prototype.isReadOnly = function() {
    return this.readOnlyFlag;
};

SepoaFilterBarPopup.prototype.clear = function() {
    this.clearConditionById(this.id);
    var selectedItemCont = this.objectDiv.querySelector(".selected_item");
    if (selectedItemCont) {
        selectedItemCont.parentNode.removeChild(selectedItemCont);
    }
};

SepoaFilterBarPopup.prototype._getFilterData = function() {
    var result = [];
    var r = {};
    r[this.id] = this.getValue();
    result.push(r);
    return result;
};

SepoaFilterBarPopup.prototype._drawbasic = function() {
    var cont = this.objectDiv;
    var filterArea = cont.querySelector(".filter_area");
    var text = "";

    var btn = document.createElement("div");
    btn.setAttribute("class", "popup_btn");
    btn.setAttribute("id", this.id);
    filterArea.appendChild(btn);

    var spanNiconWrapper = document.createElement("div");
    spanNiconWrapper.setAttribute("class", "filter_modal_icon_span_wrapper");
    btn.appendChild(spanNiconWrapper);

    var div = document.createElement("div");
    div.setAttribute("class", "span");

    var icon = document.createElement("div");
    icon.setAttribute("class", "icon");
    icon.classList.add("xi-sepoa-dummy");
    spanNiconWrapper.appendChild(div);
    spanNiconWrapper.appendChild(icon);
    this.popupBtn = icon;

    this._loadPopup(this.code, this.value);

    this.filterPopupCallbackFunction = function() {
        this.obj.callEvent("onChange", [this.id, arguments[0]]);
        if (arguments[0] == undefined) {
            return;
        }

        div.innerHTML = "";
        text = arguments[1];

        var selected = document.createElement("div");
        selected.setAttribute("class", "selected_item");
        selected.id = this.id;
        selected.value = arguments[0];
        selected.valueText = text;
        selected.filterType = "popup";
        selected.filterLabel = this.label;

        var textDiv = document.createElement("span");
        textDiv.setAttribute("class", "text");
        textDiv.innerText = text;
        selected.appendChild(textDiv);

        var clearBtn = document.createElement("div");
        clearBtn.setAttribute("class", "clear_btn");
        clearBtn.classList.add("xi-sepoa-dummy");
        selected.appendChild(clearBtn);

        div.appendChild(selected);
        this._drawConditionBox([selected], this.id);

        clearBtn.addEventListener(
            "click",
            function(e) {
                if (!this.readOnlyFlag) {
                    this._deleteCondition(this.id, btn.id);
                    e.currentTarget.parentNode.parentNode.removeChild(
                        e.currentTarget.parentNode
                    );
                }
                this.obj._conditionUpdate();
            }.bind(this)
        );
    }.bind(this);
};

SepoaFilterBarPopup.prototype._loadPopup = function(code, _value) {
    let type;
    let params;
    try {
        params = JSON.parse(sUtils.string.dataReplacer(_value));
        type = 'v2';
    } catch (e) {
        const values = _value.split("#");
        params = values.map(value => {
            const match = value.match(/\{(.+?)\}/);
            if (match != null) {
                const key = match[1];
                return Sepoa.info[key];
            }

            return value;
        });
        type = 'v1';
    }

    this.popupBtn.addEventListener("click", (e) => {
        if (!this.readOnlyFlag) {
            if (type == 'v1') {
                PopupCommonPost(code, this.filterPopupCallbackFunction, params);
            } else if (type == 'v2') {
                const options = {
                    type: 'common',
                    title: sUtils.cache.text.get("BASE_LANG.SEARCH"),
                    params: {
                        option: {
                            code: code,
                            params: params,
                            callback: (resultList, params, indexList) => {
                                const data = resultList[0];

                                this.filterPopupCallbackFunction(
                                    data[indexList[0]],
                                    data[indexList[1]]
                                );
                            }
                        }
                    }
                };

                sUtils.popup.modal(options);
            }
        }
    });
};

SepoaFilterBarPopup.prototype._deleteConditionByTarget = function(condition) {
    this.clear();
};