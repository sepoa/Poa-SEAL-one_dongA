dhtmlxApp = function(e) {
    function t() {
        for (var e = this._craft_handlers, t = e.length - 1; t >= 0; t--) console.log("detach " + e[t]), a.detachEvent(e[t]);
        this._craft_handlers = []
    }
    if (this === window) return new dhtmlxApp(e);
    var a = dhtmlxApp.activeApp = this;
    return a.config = e, a.locale = function(e) {
        return a.locale.current[e] || e
    }, a.start = function(e, t) {
        if (a.templates = a.templates || dhtmlxApp.templates.app, t)
            for (var n in t) this.config[n] = t[n];
        a.locale.current = a.locale[this.config.$lang || "en"], a.renderView(a.config.$start, e), a.container = e, a.callEvent("onAppReady", [])
    }, a.defineView = function(e, t, n) {
        a.attachEvent("render:" + e, function(o) {
            if (o && (o._craft_id !== e || n)) {
                var l = a._craft_cell;
                a._craft_cell = o, t.apply(a, arguments), a._craft_cell = l, o._craft_id = e
            }
        })
    }, a.renderView = function(e, n) {
        (n.tagName || "string" == typeof n) && (n = new dhtmlxApp.HTMLWrapper(n)), n && n.attachEvent && !n._craft_handlers && (n.attachEvent("_onCellUnload", t), n.attachEvent("_onBeforeContentDetach", t), n._craft_handlers = []);
        var o = [].concat.apply([], arguments).slice(1);
        a.callEvent("render:" + e, o)
    }, a.dhxevs = {
        data: {}
    }, a.attachEvent = function(e, t) {
        e = String(e).toLowerCase(), this.dhxevs.data[e] || (this.dhxevs.data[e] = []);
        var a = window.dhx4.newId();
        this.dhxevs.data[e].push([a, t]);
        var n = e + "@@@" + a;
        return this._craft_cell && !this._craft_cell.wrapper && (this._craft_cell._craft_handlers || (this._craft_cell._craft_handlers = []), this._craft_cell._craft_handlers.push(n)), n
    }, a.detachEvent = function(e) {
        var t = e.split("@@@"),
            a = this.dhxevs.data[t[0]];
        if (a)
            for (var n = 0; n < a.length; n++)
                if (a[n][0] == t[1]) return a.splice(n, 1)
    }, a.callEvent = function(e, t) {
        e = String(e).toLowerCase();
        var a = this.dhxevs.data[e];
        if (null == a) return !0;
        for (var n = !0, o = 0; o < a.length; o++) n = a[o][1].apply(this, t) && n;
        return n
    }, a.unload = function() {
        a.callEvent("onAppUnload", []), a.container && (a.container.attachHTMLString ? a.container.attachHTMLString("") : a.container.innerHTML = "");
        for (var e in a) a[e] = null;
        dhtmlxApp.activeApp = a = null
    }, this
}, dhtmlxApp.start = function(e, t, a) {
    var n = this.apps[e];
    n();
    var o = dhtmlxApp.activeApp;
    return o.templates = dhtmlxApp.templates[e], o.start(t, a), o
}, dhtmlxApp.template = function(e, t, a) {
    var n = dhtmlxApp.templates[e] = dhtmlxApp.templates[e] || {};
    n[t] = a
}, dhtmlxApp.apps = {}, dhtmlxApp.templates = {}, dhtmlxApp.HTMLWrapper = function(e) {
    this._node = e
}, dhtmlxApp.HTMLWrapper.prototype = {
    _isCell: !0,
    unload: function() {
        this._node = null
    },
    attachLayout: function(e) {
        return e.parent = this._node, new dhtmlXLayoutObject(e)
    },
    wrapper: !0
}, dhx4.ajax.query = function(e) {
    var t;
    return e.promise && dhx4.promise && (t = dhx4.promise.defer(), e.callback = function(e) {
        t.resolve(JSON.parse(e.xmlDoc.responseText))
    }), dhx4.ajax._call(e.method || "GET", e.url, e.data || "", e.async || !0, e.callback, null, e.headers), t
}, dhtmlxApp.apps.formbuilder = function() {
    var app = dhtmlxApp({
        $start: "Layout"
    });
    app.defineView("Layout", function(top) {
        function reenable() {
            var e = treeHistory.length - 1;
            myToolbar[historyIndex > 0 ? "enableItem" : "disableItem"]("undo"), myToolbar[e > historyIndex ? "enableItem" : "disableItem"]("redo")
        }
        myLayout = new dhtmlXLayoutObject(top, "3W");
        myLayout.cells("a").setWidth(320), myLayout.cells("c").setWidth(320), myLayout.cells("a").setText(""), myLayout.cells("c").setText("Item Config");
        var myToolbar = myLayout.attachToolbar({
                icons_path: window.CONTEXT_NAME+"/resource/plugin/dhtmlx/dhtmlxFormBuilder/etc/",
                items: [{
                    id: "new",
                    type: "button",
                    text: "New",
                    img: "new.png"
                }, {
                    id: "rename",
                    type: "button",
                    text: "Rename",
                    img: "rename.png"
                }, {
                    id: "save",
                    type: "button",
                    text: "Save",
                    img: "save.png"
                }, {
                    id: "delete",
                    type: "button",
                    text: "Delete",
                    img: "delete.png"
                }, {
                    id: "import",
                    type: "button",
                    text: "Import",
                    img: "import.png"
                }, {
                    id: "export",
                    type: "button",
                    text: "Export",
                    img: "export.png"
                }, {
                    id: "undo",
                    type: "button",
                    text: "Undo",
                    img: "undo.gif",
                    img_disabled: "undo_dis.gif"
                }, {
                    id: "redo",
                    type: "button",
                    text: "Redo",
                    img: "redo.gif",
                    img_disabled: "redo_dis.gif"
                }, {
                    id: "language",
                    type: "button",
                    text: "Language",
                    img: ""
                }]
            }),
            winExport, winImport, exportedForm = "";
        myToolbar.attachEvent("onClick", function(id) {
        	var scope = SUX.getModuleTop();
            if (null == window.dhxWins && (window.dhxWins = new dhtmlXWindows), "import" == id) {
                winImport = dhxWins.createWindow("w1", 1, 1, 800, 600), winImport.denyResize(), winImport.setModal(!0), winImport.center(), winImport.setText("Import"), winImport.attachEvent("onClose", function() {
                    return winImport = null, !0
                });
                var form = winImport.attachForm([{
                    type: "input",
                    name: "data",
                    rows: 1,
                    inputWidth: 740,
                    inputHeight: 480,
                    offsetLeft: 18,
                    offsetTop: 16,
                    className: "textarea_import_export"
                }, {
                    width: "auto",
                    blockOffset: 20,
                    type: "block",
                    list: [{
                        type: "button",
                        name: "import",
                        value: "Import",
                        offsetLeft: 221
                    }, {
                        type: "newcolumn"
                    }, {
                        type: "button",
                        name: "import-file",
                        value: "Import from File",
                        offsetLeft: 5
                    }, {
                        type: "newcolumn"
                    }, {
                        type: "button",
                        name: "cancel",
                        value: "Cancel",
                        offsetLeft: 5
                    }]
                }]);
                form.attachEvent("onButtonClick", function(name) {
                    if ("import" == name) {
                        var data = dhx4.temp = null;
                        try {
                            eval("dhx4.temp=" + this.getItemValue("data")), data = dhx4.temp
                        } catch (e) {}
                        delete dhx4.temp, null != data && (app.callEvent("onDataImport", [data]), winImport.close())
                    } else "import-file" == name ? app.loadFile(function(e, t) {
                        return e ? void console.error(e) : (app.callEvent("onDataImport", [t.data]), void winImport.close())
                    }) : "cancel" == name && winImport.close()
                }), form.setItemFocus("data"), dhx4.ajax.get("views/temp_import.json", function(e) {
                    form.setItemValue("data", e.xmlDoc.responseText)
                }), winImport.cell.firstChild.style.border = "1px solid #a4bed4", winImport.cell.firstChild.firstChild.style.backgroundColor = "#fafafa"
            } else if ("export" == id) {
                winExport = dhxWins.createWindow("w1", 1, 1, 800, 600), winExport.denyResize(), winExport.setModal(!0), winExport.center(), winExport.setText("Export"), winExport.attachEvent("onClose", function() {
                    return winExport = null, !0
                });
                var form = winExport.attachForm([{
                    type: "input",
                    name: "data",
                    rows: 1,
                    inputWidth: 740,
                    inputHeight: 480,
                    offsetLeft: 18,
                    offsetTop: 16,
                    readonly: !0,
                    className: "textarea_import_export"
                }, {
                    width: "auto",
                    blockOffset: 20,
                    type: "block",
                    list: [{
                        type: "button",
                        name: "close",
                        value: "Close",
                        offsetLeft: 312
                    }]
                }]);
                exportedForm = "";
                form.attachEvent("onButtonClick", function(e) {
                    "export-file" == e ? app.saveFile(exportedForm, "tree.json") : "close" == e && winExport.close()
                });
                form.setItemFocus("data");
                form = null;
                winExport.cell.firstChild.style.border = "1px solid #a4bed4";
                winExport.cell.firstChild.firstChild.style.backgroundColor = "#fafafa";
                app.callEvent("onPreviewDataRequest", ["export"]);
            } else if("save" == id){
            	scope.callFunction("AD_041_2", "doSave", []);
            } else if("new" == id){
            	scope.callFunction("AD_041_2", "clearApp", []);
            	scope.callFunction("AD_041_2", "renameForm", []);
            } else if("delete" == id){
            	scope.callFunction("AD_041_2", "deleteForm", []);
            } else if("rename" == id){
            	scope.callFunction("AD_041_2", "renameForm", []);
            } else if("language" == id) {
            	scope.callFunction("AD_041_2", "setMultiLanuage", []);
            } else "undo" == id ? app.callEvent("onUndo") : "redo" == id && app.callEvent("onRedo")
        });
        app.attachEvent("onPreviewDataProvide", function(e, t, m) {
            if ("export" == t && null != winExport) {
                "settings" != e.data[0].type || "" != e.data[0].offsetLeft && 0 != e.data[0].offsetLeft || delete e.data[0].offsetLeft, "settings" != e.data[0].type || "" != e.data[0].offsetTop && 0 != e.data[0].offsetTop || delete e.data[0].offsetTop;
                var a = winExport.getAttachedObject();
                var exportData = app.objToStr(e.data);
                if(!!m){
                	exportData = app.objToStr([{SFORM:e.data}, {SLANG:m}]);
                }
                a.setItemValue("data", exportData), exportedForm = JSON.stringify(e), a = null
            } else {
            	this.callEvent('onPreviewDataRequest', ['dataCheck']);
            }
        });
        app.attachEvent("onDuplicateDataCheck", function(item, e) {
        	if("dataCheck" == e) {
        		var names = [];
        		this.findItemNodes(item.data, names);
        		
        		var nameChecker = [];
        		for(var idx in names) {
        			if(nameChecker.indexOf(names[idx]) + 1) {
        				AlertE(Sepoa.cache.text.get("AD_041_2.DUPLICATE_ITEM_NAME").replace('#NAME#', names[idx]));
        				break;
        			} else {
        				nameChecker.push(names[idx]);
        			}
        		}
        	}
        })
        var myTabbar = window.myTabbar = myLayout.cells("b").attachTabbar({
            arrows_mode: "auto",
            tabs: [{
                id: "mockup",
                text: "Mockup"
            }, {
                id: "preview",
                text: "Preview"
            }]
        });
        myTabbar.attachEvent("onSelect", function(e) {
            var t = this.cells(e),
                a = {
                    mockup: "Mockup",
                    preview: "Preview"
                }[e];
            return window.setTimeout(function() {
                app.renderView(a, [t]), t = a = null
            }, 1), !0
        }), myTabbar.goToNextTab(), myTabbar.attachEvent("onSelect", function(e) {
            return app.callEvent("onTabbarSelect", [e]), !0
        }), app.renderView("Tree", [myLayout.cells("a")]), app.renderView("Property", [myLayout.cells("c")]), app.attachEvent("onAppUnload", function() {
            null != window.dhxWins && (dhxWins.unload(), dhxWins = null)
        });
        var treeHistory = [],
            historyIndex = -1;
        app.attachEvent("onUndo", function() {
            1 > historyIndex || (--historyIndex, app.callEvent("onDataImport", [treeHistory[historyIndex].data, {
                fromHistory: !0
            }]), reenable())
        }), app.attachEvent("onRedo", function() {
            historyIndex >= treeHistory.length - 1 || (++historyIndex, app.callEvent("onDataImport", [treeHistory[historyIndex].data, {
                fromHistory: !0
            }]), reenable())
        }), app.attachEvent("onHistoryRecordProvide", function(e) {
            treeHistory = treeHistory.slice(0, ++historyIndex), treeHistory[historyIndex] = e, reenable()
        })
    }), app.defineView("Mockup", function(e) {
        function t() {
            if (!n) {
                var e = o.exportToForm(null, {
                    fakeNames: !0
                });
                app.callEvent("onPreviewDataProvide", [e, "preview"])
            }
        }

        function a() {
            n || app.callEvent("onHistoryRecordProvide", [o.exportToForm()])
        }
        var n = !1,
            o = window.myMockup = e.attachMockup();
        o.attachEvent("onSelect", function(e, t) {
            app.callEvent("onItemSelect", ["mockup", e, t])
        }), o.attachEvent("onAdd", function(e, n, l) {
            app.callEvent("onTreeUpdateProvide", [o.exportToTree()]), o.clearSelection(), o.selectItem(n, !0, !0), t(), a()
        }), o.attachEvent("onNodeMove", function(e, n) {
            app.callEvent("onTreeUpdateProvide", [o.exportToTree()]), o.clearSelection(), o.selectItem(n, !0, !0), t(), a()
        }), o.attachEvent("onDelete", function(e, t) {
            app.callEvent("onItemDelete", ["mockup", e, t])
        }), o.attachEvent("onNodeMove", function(e, n) {
            t(), a()
        });
        
        // Defailt Json Data
        var l = (window.defaultJsonData || []);
        
        o.loadStruct(l, function() {
            app.callEvent("onHistoryRecordProvide", [o.exportToForm()]), app.callEvent("onTreeUpdateProvide", [o.exportToTree()])
        }), app.attachEvent("onItemSelect", function(e, t, a) {
            "mockup" != e && o.selectItem(t, a, !1)
        }), app.attachEvent("onItemDelete", function(e, n, l) {
            "mockup" != e && o.deleteItem(n, !1, !1), t(), a()
        }), app.attachEvent("onConfigRequest", function(e) {
            var t = o.getItemData(e);
            app.callEvent("onConfigProvide", [t])
        }), app.attachEvent("onConfigUpdate", function(e, n, l, i) {
            for (var p in e) o.setItemData(p, n, l);
            var r = o.getItemData(app.keys(e));
            app.callEvent("onConfigProvide", [r]), "preview" != i && t(), a()
        }), app.attachEvent("onPreviewDataRequest", function(e) {
            var t = {};
            var args = [];
            if("preview" == e){
            	t.fakeNames = !0
            	args.push(o.exportToForm(null, t));
            	args.push(e);
            }else if("export" == e){
            	t.hideIds = !0
            	var m = o.exportToFormSepoaMode(null, t);
            	args.push(m);
            	args.push(e);
            	if(m.slang.length > 0) args.push(m.slang);
            }else if("dataCheck" == e){
            	t.hideIds = !0
            	var m = o.exportToFormSepoaMode(null, t);
            	args.push(m);
            	args.push(e);
            	app.callEvent('onDuplicateDataCheck', args);
            	return;
            }
            app.callEvent("onPreviewDataProvide", args)
        }), app.attachEvent("onDataImport", function(e, l) {
            n = !0, l = l || {}, o.loadStruct(e, function() {
                app.callEvent("onTreeUpdateProvide", [o.exportToTree()]), n = !1, t(), l.fromHistory || a()
            })
        }), app.attachEvent("onTreeUpdateRequest", function() {
            app.callEvent("onTreeUpdateProvide", [o.exportToTree()])
        }), app.attachEvent("onItemCopyRequest", function(e, t) {
            var a = {
                item: "copyItem",
                column: "copyColumn",
                block: "copyBlock"
            }[e];
            a && o[a](t, null, {
                callEvent: !0
            })
        }), app.attachEvent("onItemAddRequest", function(e, t) {
            "item" === e ? o.addItem(t, {
                type: "input",
                label: "New Input",
                value: ""
            }, {
                callEvent: !0
            }) : "block" === e ? o.addBlock(t, {
                type: "block"
            }, {
                callEvent: !0
            }) : "column" === e && o.addColumn(t, {
                type: "column"
            }, {
                callEvent: !0
            })
        }), app.attachEvent("onItemMove", function(e, t, a, n) {
            n = n || {}, n.callEvent = !0, o.moveNode(t, a, n)
        })
    }), app.defineView("Preview", function(e) {
        var t = window.myForm = e.attachForm(),
            a = !0;
        t.fixRadioId = function(e) {
            if (null == t.getItemType(e))
                for (var a in dhtmlXForm.prototype.items.radio.r)
                    if (t.getUserData(a, "_idd") == e) {
                        e = a;
                        break
                    }
            return e
        }, t.getRadioItems = function(e) {
            var a = dhtmlXForm.prototype.items.radio.r,
                n = [];
            for (var o in a) {
                var l = t.getUserData(o, "_idd");
                l && n.push({
                    id: l,
                    value: a[o]._value,
                    checked: a[o]._checked
                })
            }
            return n
        }, t.attachEvent("onChange", function(e, a, n) {
            var o = t.getItemType(e),
                l = {};
            l[e] = 1;
            var i, p;
            switch (o) {
                case "radio":
                    var r = t.getRadioItems(e);
                    r.forEach(function(e) {
                        l = {}, l[e.id] = 1, app.callEvent("onConfigUpdate", [l, "checked", e.checked, "preview"])
                    });
                    break;
                case "checkbox":
                    app.callEvent("onConfigUpdate", [l, "checked", n, "preview"]);
                    break;
                case "colorpicker":
                    i = t.getColorPicker(e), p = i.getCustomColors(), app.callEvent("onConfigUpdate", [l, "value", a, "preview"]), p.length > 0 && app.callEvent("onConfigUpdate", [l, "customColors", p, "preview"]);
                    break;
                case "calendar":
                case "doubleCalendar":
                    app.callEvent("onConfigUpdate", [l, "value", window.dhx4.date2str(a, "%Y/%m/%d"), "preview"]);
                    break;
                default:
                    app.callEvent("onConfigUpdate", [l, "value", a, "preview"])
            }
        }), app.attachEvent("onPreviewDataProvide", function(e, n) {
            if ("preview" == n && a) {
                var o = t.cont,
                    l = o.scrollTop,
                    i = o.scrollLeft;
                "function" == typeof t._emuSelCacheClear && t._emuSelCacheClear();
                for (var p in t.objPull) t._removeItem(String(p).replace(t.idPrefix, ""));
                "settings" == e.data[0].type && "undefined" == typeof e.data[0].offsetLeft && (e.data[0].offsetLeft = ""), "settings" == e.data[0].type && "undefined" == typeof e.data[0].offsetTop && (e.data[0].offsetTop = ""), t.loadStruct(e.data, function() {
                    for (var a in e.selected) t.emulateSelect(t.fixRadioId(a), !0);
                    o.scrollLeft = i, o.scrollTop = l, o = null
                })
            }
        }), app.callEvent("onPreviewDataRequest", ["preview"]), app.attachEvent("onTabbarSelect", function(e) {
            return "preview" == e ? (a = !0, app.callEvent("onPreviewDataRequest", ["preview"])) : a = !1, !0
        }), app.attachEvent("onItemSelect", function(e, n, o) {
            if (1 == a) {
                n instanceof Array || (n = [n]);
                for (var l = 0; l < n.length; l++) t.emulateSelect(t.fixRadioId(n[l]), o)
            }
        }), app.attachEvent("onAppUnload", function() {
            t._emuSelCacheClear()
        })
    }), app.defineView("Property", function(e) {
        function t() {
            null != o && window.clearTimeout(o), o = window.setTimeout(a, l)
        }

        function a() {
            o = null;
            var e = [],
                t = [],
                a = n.cont,
                l = a.scrollTop,
                d = a.scrollLeft;
            for (var c in p) "upload" == r[c].type && (r[c].type = "uploader"), r[c].hasOwnProperty("note") && (r[c].noteText = r[c].note.text, r[c].noteWidth = r[c].note.width), t.indexOf(r[c].type) < 0 && (t.push(r[c].type), e.push(app.config.itemProps[r[c].type]));
            var s = t.sort().join("_");
            if (n.clearAll(), e.length > 0) {
                if (null == i[s]) {
                    e = 1 == e.length ? e[0] : app.getIntersect.apply(app, e);
                    var u = [];
                    for (var f in app.config.propGroups) {
                        var t = [{
                            type: "group",
                            label: f
                        }];
                        for (var m in app.config.propGroups[f]) e.indexOf(m) >= 0 && t.push(app.config.propGroups[f][m]);
                        t.length > 1 && (u = u.concat(t))
                    }
                    i[s] = window.dhx4._copyObj(u)
                }
                n.loadStruct(window.dhx4._copyObj(i[s]), function() {
                    var e = [];
                    for (var t in p) e.push(r[t]);
                    e = app.getIntersect.apply(app, e), n.setValues(e), a.scrollLeft = d, a.scrollTop = l, a = null
                })
            }
        }
        var n = window.myPropView = e.attachPropertyView();
        n.attachEvent("onChange", function(e, t) {
            for (var n in p) "" == t || null == t ? "undefined" != typeof r[n][e] && delete r[n][e] : r[n][e] = t;
            app.callEvent("onConfigUpdate", [window.dhx4._copyObj(p), e, t]), "type" == e && a()
        });
        var o = null,
            l = 1,
            i = {},
            p = {},
            r = {};
        app.attachEvent("onItemSelect", function(e, a, n) {
            a instanceof Array || (a = [a]);
            for (var o = 0; o < a.length; o++) 1 == n ? p[a[o]] = !0 : delete p[a[o]];
            var l = [];
            for (var i in p) null == r[i] && l.push(i);
            l.length > 0 ? app.callEvent("onConfigRequest", [l]) : t()
        }), app.attachEvent("onConfigProvide", function(e) {
            for (var a in e) r[a] = e[a];
            t()
        }), app.attachEvent("onItemDelete", function(e, a, n) {
            null != r[a] && delete r[a], 1 == p[a] && (delete p[a], t())
        })
    }), app.defineView("Tree", function(e) {
        function t(e) {
            return !!{
                block: 1,
                column: 1,
                item: 1
            }[e.type]
        }

        function a(e, t, a) {
            if ("block" == t.type && 0 == a) return !1;
            var n = {
                block: {
                    column: 1
                },
                column: {
                    item: 1,
                    block: 1
                }
            };
            return n[t.type] && n[t.type][e.type]
        }

        function n(e) {
            if (!e) return {
                type: "block"
            };
            var t = {
                    type: null
                },
                a = l.getUserData(e, "type") || "";
            return app.treeContextMenuItemTypes[a] ? t.type = "item" : a.match(/column$/gi) ? t.type = "column" : a.match(/(fieldset|block)$/gi) && (t.type = "block"), t
        }
        var o = window.myTreeMenu = new dhtmlXMenuObject({
            context: !0,
            items: [{
                id: "column_delete",
                text: "Delete Column"
            }, {
                id: "column_add_block",
                text: "Add Block"
            }, {
                id: "column_add_item",
                text: "Add Item"
            }, {
                id: "column_copy",
                text: "Copy Column"
            }, {
                id: "block_delete",
                text: "Delete Block"
            }, {
                id: "block_add_column",
                text: "Add Column"
            }, /*{
                id: "block_copy",
                text: "Copy Block"
            },*/ {
                id: "item_delete",
                text: "Delete Item"
            }, {
                id: "item_copy",
                text: "Copy Item"
            }]
        });
        o.filterItemsByText = function(e) {
            var t = this,
                a = new RegExp("^" + e);
            this.forEachItem(function(e) {
                t[null != e.match(a) ? "showItem" : "hideItem"](e)
            }), a = t = null
        }, o.attachEvent("onClick", function(e) {
            var t;
            null != e.match(/delete$/gi) && app.callEvent("onItemDelete", ["ctx", this.conf.ctx_id, e.replace(/_delete$/, "")]), t = /(column|block|item)_copy$/gi.exec(e), null != t && app.callEvent("onItemCopyRequest", [t[1], this.conf.ctx_id]), t = /add_(column|block|item)$/gi.exec(e), null != t && app.callEvent("onItemAddRequest", [t[1], this.conf.ctx_id])
        });
        var l = window.myTreeView = e.attachTreeView({
            dnd: !0,
            multiselect: !0,
            context_menu: !0
        });
        l.attachEvent("onBeforeDrag", function(e) {
            return t(n(e))
        }), l.attachEvent("onDragOver", function(e, t, o) {
            return a(n(e), n(t), o)
        }), l.attachEvent("onDrop", function(e, t, a) {
            var o = n(e).type;
            "column" == o && --a, app.callEvent("onItemMove", [o, e, t, {
                childIndex: a
            }])
        }), l.attachEvent("onSelect", function(e, t) {
            app.callEvent("onItemSelect", ["tree", e, t])
        }), l.attachEvent("onContextMenu", function(e, t, a) {
            o.conf.ctx_id = e;
            var n = l.getUserData(e, "type");
            if ("column" == n) o.filterItemsByText("column");
            else if ("block" == n || "fieldset" == n) o.filterItemsByText("block");
            else {
                if (!app.treeContextMenuItemTypes[n]) return !1;
                o.filterItemsByText("item")
            }
            return l.selectItem(e), o.showContextMenu(t, a), !1
        });
        app.attachEvent("onTreeUpdateProvide", function(e) {
            l.clearAll(), l.loadStruct(e)
        }), app.attachEvent("onItemSelect", function(e, t, a) {
            "tree" != e && (1 == a ? l.selectItem(t, !1, !0) : l.unselectItem(t))
        }), app.attachEvent("onItemAdd", function(e, t, a, n) {
            "tree" != e && (console.warn("myTreeView.addItem syntax changed, recheck!"), l.addItem(a, t, n))
        }), app.attachEvent("onItemDelete", function(e, t, a) {
            "tree" != e && (null == l.getParentId(t) ? window.setTimeout(function() {
                app.callEvent("onTreeUpdateRequest", [])
            }, 1) : l.deleteItem(t))
        }), app.attachEvent("onConfigProvide", function(e) {
            for (var t in e) {
                var a = e[t].type,
                    n = e[t].label;
                "upload" == a && (a = "uploader", n = ""), "button" == a && (n = e[t].value), {
                    column: 1,
                    fieldset: 1,
                    block: 1
                }[a] ? l.setItemText(t, "[" + t + "] " + a) : app.treeContextMenuItemTypes[a] && l.setItemText(t, "[" + t + "] [" + a + "] " + n), e[t].icon && l.setItemIcons(t, {
                    file: e[t].icon
                })
            }
        }), app.attachEvent("onConfigUpdate", function(e, t, a) {
            if ({
                    type: 1,
                    label: 1,
                    value: 1
                }[t])
                for (var n in e) l.setUserData(n, t, a)
        })
    }), app.treeContextMenuItemTypes = {
        input: !0,
        password: !0,
        select: !0,
        multiselect: !0,
        combo: !0,
        checkbox: !0,
        radio: !0,
        label: !0,
        button: !0,
        hidden: !0,
        template: !0,
        container: !0,
        calendar: !0,
        doubleCalendar: !0,
        calendarYM: !0,
        colorpicker: !0,
        editor: !0,
        btn2state: !0,
        image: !0,
        uploader: !0,
        file: !0
    }, app.formatFormToTree = function(e) {
        for (var t = [], a = 0; a < e.length; a++) {
            var n = null;
            "settings" == e[a].type || ("column" == e[a].type ? n = {
                id: dhx4.newId(),
                text: "[column]"
            } : (n = {
                id: e[a].name,
                text: "[" + String(e[a].type).charAt(0) + "] " + (e[a].label || e[a].name)
            }, null != e[a].list && (n.text = "[block]", n.item = app.formatFormToTree(e[a].list), n.open = !0))), null != n && t.push(n)
        }
        return t
    }, app._cmpIntersect = function(e, t) {
        if (e instanceof Array)
            for (var a = [], n = 0; n < e.length; n++) t.indexOf(e[n]) >= 0 && a.push(e[n]);
        else {
            var a = {};
            for (var o in e) e[o] == t[o] && (a[o] = t[o])
        }
        return a
    }, app.getIntersect = function() {
        for (var e = [], t = 0; t < arguments.length; t++) e.push(window.dhx4._copyObj(arguments[t]));
        for (var t = 1; t < e.length; t++) e[0] = app._cmpIntersect(e[0], e[t]);
        return e[0]
    }, app.objToStr = function(e, t) {
        "undefined" == typeof t && (t = 0);
        var a = "";
        if (e instanceof Array) {
            for (var n = [], o = 0; o < e.length; o++) n.push(app.objToStr(e[o], t + 1));
            a += n.length > 0 ? "[\n" + n.join(",\n") + "\n" + "	".repeat(t) + "]" : "[]"
        } else if ("object" == typeof e)
            if (null == e) a += "null";
            else {
                var n = [];
                for (var l in e) n.push(l + ": " + app.objToStr(e[l], t));
                a += n.length > 0 ? "	".repeat(t) + "{" + n.join(", ") + "}" : "{}"
            }
        else "string" == typeof e ? a += '"' + e.replace(/([\"\'])/g, "\\$1") + '"' : "number" == typeof e ? a += e : "boolean" == typeof e && (a += e ? "true" : "false");
        return a
    }, app.saveFile = function(e, t, a) {
        a = a || "text/plain";
        var n = document.createElement("textarea"),
            o = document.createElement("input"),
            l = document.createElement("input"),
            i = document.createElement("iframe"),
            p = document.createElement("form");
        i.style = "display:none", o.name = "filename", o.value = t, n.name = "content", n.value = e, l.value = a, l.name = "type", p.action = "assets/download.php", p.method = "post", p.appendChild(n), p.appendChild(o), p.appendChild(l), i.appendChild(p), document.body.appendChild(i), p.submit(), i.remove()
    }, app.loadFile = function(e) {
        var t = "dhtmlx-formbuilder-import-data-file-prompt",
            a = document.getElementById(t);
        if (!a) {
            var a = document.createElement("input");
            a.id = t, a.type = "file", a.style = "visibility:hidden", a.onchange = function(t) {
                var n = (t.target || window.event.srcElement).files || [];
                if (FileReader && n.length) {
                    var o = new FileReader;
                    o.onload = function() {;
                        e(null, {
                            data: window.dhx4.s2j(o.result)
                        })
                    }, o.onerror = function() {
                        e("Error while loading a file")
                    }, o.readAsText(n[0]), a.remove()
                } else e("Too old browser")
            }, document.body.appendChild(a)
        }
        a.click()
    }, app.keys = function(e) {
        var t = [];
        for (var a in e) e.hasOwnProperty(a) && t.push(a);
        return t
    }, app.config.itemProps = {
        select: ["name", "type", "position", "offsetLeft", "offsetTop", "label", "labelAlign", "labelWidth", "labelHeight",   "info", "tooltip", "combo", "inputWidth", "inputHeight", "inputLeft", "inputTop", "className", "module", "disabled", "hidden", "validate", "required", "noteText", "noteWidth", "design"],
        autocomplete: ["name", "type", "position", "offsetLeft", "offsetTop", "label", "labelAlign", "labelWidth", "labelHeight",   "info", "tooltip", "combo", "inputWidth", "inputHeight", "inputLeft", "inputTop", "className", "module", "disabled", "hidden", "validate", "required", "noteText", "noteWidth", "design"],
        input: ["name", "placeholder", "type", "position", "offsetLeft", "offsetTop", "label", "labelAlign", "labelWidth", "labelHeight",   "info", "tooltip", "inputWidth", "inputHeight", "inputLeft", "inputTop", "module", "rows", "maxLength", "numberFormat", "value", "className", "readonly", "disabled", "hidden", "validate", "required", "noteText", "noteWidth", "design", "inputAlign", "sepoaStyle"],
        label: ["name", "type", "offsetLeft", "offsetTop", "label", "labelWidth", "labelHeight", "module", "className", "disabled", "hidden", "design"],
        button: ["name", "type", "offsetLeft", "offsetTop", "label", "value", "width", "inputLeft", "inputTop", "tooltip", "className", "module", "disabled", "hidden", "design"],
        checkbox: ["name", "type", "position", "offsetLeft", "offsetTop", "label", "labelAlign", "labelWidth", "labelHeight", "module", "info", "tooltip", "inputLeft", "inputTop", "value", "checked", "className", "readonly", "disabled", "hidden", "required", "noteText", "noteWidth", "design"],
        block: ["name", "blockType", "width", "blockOffset", "offsetLeft", "offsetTop", "blockWidth", "module"],
        btn2state: ["name", "type", "offsetLeft", "offsetTop", "position", "info", "label", "labelAlign", "labelWidth", "labelHeight", "module", "tooltip", "inputWidth", "inputHeight", "inputLeft", "inputTop", "value", "className", "cssName", "disabled", "hidden", "required", "readonly", "noteText", "noteWidth", "design"],
        calendar: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight", "module", "tooltip", "inputWidth", "inputHeight", "inputLeft", "inputTop", "value", "dateFormat", "calendarPosition", "minutesInterval", "weekStart", "enableTime", "showWeekNumbers", "className", "disabled", "hidden", "readonly", "required", "validate", "noteText", "noteWidth", "design"],
        doubleCalendar: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight", "module", "tooltip", "calendarTo", "inputWidth", "inputHeight", "inputLeft", "inputTop", "value", "dateFormat", "calendarPosition", "minutesInterval", "weekStart", "enableTime", "showWeekNumbers", "className", "disabled", "hidden", "readonly", "required", "validate", "noteText", "noteWidth", "design"],
        calendarYM: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight", "module", "tooltip", "inputWidth", "inputHeight", "inputLeft", "inputTop", "value", "dateFormat", "calendarPosition", "className", "disabled", "hidden", "readonly", "required", "validate", "noteText", "noteWidth", "design"],
        colorpicker: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight", "module", "tooltip", "inputWidth", "inputHeight", "inputLeft", "inputTop", "value", "cpPosition", "enableCustomColors", "customColors", "className", "disabled", "hidden", "readonly", "required", "validate", "noteText", "noteWidth", "design"],
        combo: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight", "module", "tooltip", "inputWidth", "inputLeft", "inputTop", "value", "options", "comboType", "comboImagePath", "comboDefaultImage", "comboDefaultImageDis", "filtering", "serverFiltering", "filterCache", "filterSubLoad", "className", "disabled", "hidden", "readonly", "required", "validate", "noteText", "noteWidth", "design"],
        container: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight", "module", "tooltip", "inputWidth", "inputLeft", "inputTop", "inputHeight", "className", "hidden", "required", "noteText", "noteWidth"],
        editor: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight", "module", "tooltip", "inputWidth", "inputLeft", "inputTop", "inputHeight", "value", "className", "hidden", "disabled", "required", "validate", "noteText", "noteWidth", "design"],
        fieldset: ["name", "position", "blockType", "offsetLeft", "offsetTop", "label", "inputLeft", "inputTop", "fieldsetWidth", "className", "module", "disabled"],
        file: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight",   "tooltip", "inputLeft", "inputTop", "className", "module", "hidden", "disabled", "required", "noteText", "noteWidth"],
        hidden: ["name", "type", "value", "module"],
        image: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight",   "tooltip", "inputWidth", "inputHeight", "inputLeft", "inputTop", "module", "value", "imageUrl", "imageWidth", "imageHeight", "className", "hidden", "disabled", "style"],
        multiselect: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight",   "tooltip", "inputWidth", "inputHeight", "inputLeft", "inputTop", "module", "value", "options", "className", "hidden", "disabled", "required", "noteText", "noteWidth", "design"],
        column: ["columnOffset"],
        password: ["name", "type", "position", "offsetLeft", "offsetTop", "label", "labelAlign", "labelWidth", "labelHeight", "module", "info", "tooltip", "inputWidth", "inputHeight", "inputLeft", "inputTop", "maxLength", "value", "className", "readonly", "disabled", "hidden", "validate", "required", "noteText", "noteWidth", "design"],
        radio: ["name", "type", "position", "offsetLeft", "offsetTop", "group", "label", "labelAlign", "labelWidth", "labelHeight", "module", "info", "tooltip", "inputLeft", "inputTop", "value", "checked", "className", "readonly", "disabled", "hidden", "required", "noteText", "noteWidth", "design"],
        settings: ["position", "offsetLeft", "offsetTop", "labelAlign", "labelWidth", "labelHeight", "inputWidth", "inputHeight", "blockOffset", "noteWidth"],
        template: ["name", "type", "position", "offsetLeft", "offsetTop", "label", "labelAlign", "labelWidth", "labelHeight", "module", "info", "tooltip", "inputWidth", "inputHeight", "inputLeft", "inputTop", "value", "formatFunc", "className", "hidden", "required", "noteText", "noteWidth"],
        uploader: ["name", "type", "inputWidth", "inputHeight", "module", "uploaderMode", "titleScreen", "titleText", "autoStart", "autoRemove", "uploaderUrl", "swfPath", "swfUrl", "slXap", "slUrl", "className", "hidden", "disabled"],
        vault: ["name", "type", "position", "offsetLeft", "offsetTop", "info", "label", "labelAlign", "labelWidth", "labelHeight", "module", "tooltip", "inputWidth", "inputLeft", "inputTop", "inputHeight","uploadType", "servlet", "service", "select", "upload", "delete", "className", "hidden","readonly","required", "noteText", "noteWidth"]
    }, app.config.propGroups = {
        Common: {
            name: {
                name: "name",
                type: "input",
                v: "str"
            },
            placeholder: {
                name: "placeholder",
                type: "input",
                v: "str"
            },
            type: {
                name: "type",
                type: "select",
                v: "select",
                options: ["input", "password", "select", "autocomplete","multiselect", "checkbox", "radio", "label", "button", "template", "container", "calendar", "doubleCalendar", "calendarYM", "colorpicker", "editor", "btn2state", "image", "vault"]
            },
            blockType: {
                name: "type",
                type: "select",
                v: "select",
                options: ["block", "fieldset"]
            },
            position: {
                name: "position",
                type: "select",
                v: "select",
                options: ["", "label-left", "label-right", "label-top", "absolute"]
            },
            offsetLeft: {
                name: "offsetLeft",
                type: "input",
                v: "int"
            },
            offsetTop: {
                name: "offsetTop",
                type: "input",
                v: "int"
            },
            columnOffset: {
                name: "offset",
                type: "input",
                v: "int"
            },
            group: {
                name: "group",
                type: "input",
                v: "str"
            },
            design: {
            	name: "design",
            	type: "select",
            	v: "select",
            	options: ["", "primary"]
            },
            module: {
            	name: "module",
            	type: "input",
            	v: "str"
            },
            sepoaStyle: {
            	name: "sepoaStyle",
            	type: "select",
            	v: "select",
            	options: [
            		{text: "", value: ""},
            		{text: "강조1", value: "primary"},
            		{text: "강조2", value: "secondary"},
            		{text: "강조3", value: "tertiary"}
            	]
            }
        },
        "Select Conf":{
        	combo:{
            	name: "combo",
            	type: "input",
            	v: "str"
            }
        },
        "Block Conf": {
            blockWidth: {
                name: "width",
                type: "input",
                v: "int"
            },
            blockOffset: {
                name: "blockOffset",
                type: "input",
                v: "int"
            }
        },
        "Fieldset Conf": {
            fieldsetWidth: {
                name: "width",
                type: "input",
                v: "int"
            }
        },
        "Label Conf": {
            label: {
                name: "label",
                type: "input",
                v: "str"
            },
            labelAlign: {
                name: "labelAlign",
                type: "select",
                v: "select",
                def: "left",
                options: ["", "left", "right", "center"]
            },
            labelWidth: {
                name: "labelWidth",
                type: "input",
                v: "int",
                auto: !0,
                def: "auto"
            },
            labelHeight: {
                name: "labelHeight",
                type: "input",
                v: "int",
                auto: !0,
                def: "auto"
            },
            labelLeft: {
                name: "labelLeft",
                type: "input",
                v: "int"
            },
            labelTop: {
                name: "labelTop",
                type: "input",
                v: "int"
            },
            info: {
                name: "info",
                type: "checkbox",
                v: "bool",
                def: !1
            },
            tooltip: {
                name: "tooltip",
                type: "input",
                v: "str"
            }
        },
        "Input Conf": {
            inputWidth: {
                name: "inputWidth",
                type: "input",
                v: "int",
                auto: !0,
                def: "auto"
            },
            inputHeight: {
                name: "inputHeight",
                type: "input",
                v: "int",
                auto: !0,
                def: "auto"
            },
            inputLeft: {
                name: "inputLeft",
                type: "input",
                v: "int"
            },
            inputTop: {
                name: "inputTop",
                type: "input",
                v: "int"
            },
            inputAlign: {
                name: "inputAlign",
                type: "select",
                v: "select",
                def: "",
                options: ["", "left", "right"]
            },
            rows: {
                name: "rows",
                type: "input",
                v: "int"
            },
            maxLength: {
                name: "maxLength",
                type: "input",
                v: "int"
            },
            numberFormat: {
                name: "numberFormat",
                type: "input",
                v: "str"
            },
            value: {
                name: "value",
                type: "input",
                v: "str"
            },
            checked: {
                name: "checked",
                type: "checkbox",
                v: "bool"
            },
            connector: {
                name: "connector",
                type: "input",
                v: "str"
            },
            options: {
                name: "options",
                type: "input",
                v: "array"
            },
            buttonLabel: {
                name: "value",
                type: "input",
                v: "string"
            },
            buttonWidth: {
                name: "width",
                type: "input",
                v: "int",
                auto: !0,
                def: "auto"
            },
            formatFunc: {
                name: "format",
                type: "input",
                v: "str"
            },
            calendarTo: {
            	name: "calendarTo",
            	type: "input",
            	v: "string"
            }
        },
        "Combo Conf": {
            comboType: {
                name: "comboType",
                type: "select",
                v: "select",
                options: ["option", "checkbox", "image"]
            },
            comboImagePath: {
                name: "comboImagePath",
                type: "input",
                v: "str"
            },
            comboDefaultImage: {
                name: "comboDefaultImage",
                type: "input",
                v: "str"
            },
            comboDefaultImageDis: {
                name: "comboDefaultImageDis",
                type: "input",
                v: "str"
            },
            filtering: {
                name: "filtering",
                type: "checkbox",
                v: "bool",
                def: !1
            },
            serverFiltering: {
                name: "serverFiltering",
                type: "input",
                v: "str"
            },
            filterCache: {
                name: "filterCache",
                type: "checkbox",
                v: "bool",
                def: !1
            },
            filterSubLoad: {
                name: "filterSubLoad",
                type: "checkbox",
                v: "bool",
                def: !1
            }
        },
        "Calendar Conf": {
            dateFormat: {
                name: "dateFormat",
                type: "input",
                v: "str"
            },
            serverDateFormat: {
                name: "serverDateFormat",
                type: "input",
                v: "str"
            },
            calendarPosition: {
                name: "calendarPosition",
                type: "select",
                v: "select",
                options: ["bottom", "right"]
            },
            minutesInterval: {
                name: "minutesInterval",
                type: "select",
                v: "select",
                options: ["1", "5", "10", "15"]
            },
            weekStart: {
                name: "weekStart",
                type: "select",
                v: "select",
                options: ["1", "2", "3", "4", "5", "6", "7"]
            },
            enableTime: {
                name: "enableTime",
                type: "checkbox",
                v: "bool",
                def: !0
            },
            showWeekNumbers: {
                name: "showWeekNumbers",
                type: "checkbox",
                v: "bool",
                def: !0
            }
        },
        "ColorPicker Conf": {
            cpPosition: {
                name: "cpPosition",
                type: "select",
                v: "select",
                options: ["bottom", "right"]
            },
            enableCustomColors: {
                name: "enableCustomColors",
                type: "checkbox",
                v: "bool",
                def: !1
            },
            customColors: {
                name: "customColors",
                type: "input",
                v: "str"
            }
        },
        "Uploader Conf": {
            uploaderMode: {
                name: "mode",
                type: "select",
                v: "select",
                options: ["auto", "html5", "html4", "flash", "sl"]
            },
            titleScreen: {
                name: "titleScreen",
                type: "checkbox",
                v: "bool",
                def: !0
            },
            titleText: {
                name: "titleText",
                type: "input",
                v: "str",
                def: "Drag-n-Drop files here or click to select files for upload"
            },
            autoStart: {
                name: "autoStart",
                type: "checkbox",
                v: "bool",
                def: !1
            },
            autoRemove: {
                name: "autoRemove",
                type: "checkbox",
                v: "bool",
                def: !1
            },
            uploaderUrl: {
                name: "url",
                type: "input",
                v: "str"
            },
            swfPath: {
                name: "swfPath",
                type: "input",
                v: "str"
            },
            swfUrl: {
                name: "swfUrl",
                type: "input",
                v: "str"
            },
            slXap: {
                name: "slXap",
                type: "input",
                v: "str"
            },
            slUrl: {
                name: "slUrl",
                type: "input",
                v: "str"
            }
        },
        "Image Conf": {
            imageUrl: {
                name: "url",
                type: "input",
                v: "str"
            },
            imageWidth: {
                name: "imageWidth",
                type: "input",
                v: "int"
            },
            imageHeight: {
                name: "imageHeight",
                type: "input",
                v: "int"
            }
        },
        "Vault Conf":{
        	uploadType:{
        		name:"uploadType",
        		type:"input",
        		v:"str"
        	}, 
        	viewMode:{
        		name:"viewMode",
        		type:"select",
        		v:"select",
        		options:["", "View"]
        	}, 
        	servlet:{
        		name:"servlet",
        		type:"input",
        		v:"str"
        	},
        	service:{
        		name:"service",
        		type:"input",
        		v:"str"
        	},
        	select:{
        		name:"select",
        		type:"input",
        		v:"str"
        	},
        	upload:{
        		name:"upload",
        		type:"input",
        		v:"str"
        	},        	
        	'delete':{
        		name:"delete",
        		type:"input",
        		v:"str"
        	}
        },
        Misc: {
            style: {
                name: "style",
                type: "input",
                v: "str"
            },
            className: {
                name: "className",
                type: "input",
                v: "str"
            },
            cssName: {
                name: "cssName",
                type: "input",
                v: "str"
            },
            disabled: {
                name: "disabled",
                type: "checkbox",
                v: "bool"
            },
            hidden: {
                name: "hidden",
                type: "checkbox",
                v: "bool"
            },
            readonly: {
                name: "readonly",
                type: "checkbox",
                v: "bool"
            },
            required: {
                name: "required",
                type: "checkbox",
                v: "bool"
            },
            isNotEmpty: {
                name: "isNotEmpty",
                type: "checkbox",
                v: "bool"
            },
            validate: {
                name: "validate",
                type: "select",
                v: "select",
                options: ["", "ValidBoolean", "ValidEmail","ValidHP","ValidTel","ValidID", "ValidInteger", "ValidNumeric", "ValidAplhaNumeric", "ValidDatetime", "ValidDate", "ValidTime", "ValidIPv4", "ValidCurrency", "ValidSSN", "ValidSIN"]
            }
        },
        Note: {
            noteText: {
                name: "noteText",
                type: "input",
                v: "str",
                def: ""
            },
            noteWidth: {
                name: "noteWidth",
                type: "input",
                v: "int",
                auto: !0,
                def: "auto"
            }
        },
        UserData: {
        	userdataName: {
                name: "userdataName",
                type: "input",
                v: "str"
            },
            userdataValue: {
                name: "userdataValue",
                type: "input",
                v: "str"
            }
        }
    }
};
dhtmlxApp.prototype.findItemNodes=function(data, itemArray){
	var finder = ['block', 'fieldset'];
    
    for (var idx in data) {
        if (finder.indexOf(data[idx].type) + 1) {
            if (data[idx].list) {
            	this.findItemNodes(data[idx].list, itemArray);
            }
        } else {
        	if(data[idx].type == 'settings' || data[idx].type == 'radio') continue;
        	if(typeof(data[idx].name) == 'undefined') continue;
        	
        	itemArray.push(data[idx].name);
        }
    }
}
dhtmlxApp.prototype.propertyFocus=function(a, b) {
	if(b._conf) {
		if(b._conf.name == 'module') {
			var moduleSelectCallback = function(target, value, custom) {
				var node = custom.b;
				node.conf = node._conf = custom.conf;
				node.idd = node._idd = custom.idd;
				node.conf.call_event('propertyChange', [target, node, value]);
			}
			var customParam = {idd: b._idd, conf: b._conf, b: b};
			var selectedValue = a.getValue(b);
			PopupOpenPost(window.CONTEXT_NAME + '/Admin/ModuleConfiguration/AdminModuleConfigurationSelectPopup.sux', {target: a, selectedValue: selectedValue, customParam: customParam}, moduleSelectCallback, 800, null, Sepoa.cache.text.get('AD_041_2.MODULE_SELECT'), 'SELECT_MODULE');
		}
	}
}