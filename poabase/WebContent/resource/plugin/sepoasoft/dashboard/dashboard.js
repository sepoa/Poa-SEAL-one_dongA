const boardItems={};
const modules={};
export class SepoaDashboard{
	#config={};
	#items={};
	constructor(v){
		this.#config={
			base:v
		}
		v.classList.add('sepoa-dashboard');
		sUtils.cache.client.get('DESIGN.DASHBOARD');
		
		const styleData = sUtils.cache.client.data?.server?.DESIGN;
		if(styleData){
			const styleArray = ['.sepoa-dashboard * {', '}'];
			Object.keys(styleData).forEach((key)=>{
				if(key.startsWith('DASHBOARD_')){
					styleArray.splice(1, 0, `--dash-item-${key.replace('DASHBOARD_', '').toLowerCase()}:${styleData[key]};`); 
				}
			})
			
			if(styleArray.length > 2){
				this.styleElem = v.appendChild(document.createElement('style'));
				this.styleElem.innerText = styleArray.join('\n');
			}
		}
	}
	
	addItem(option){
		if(!boardItems[option.type]){
			console.error(`not support dashboard item type : ${option.type}`);
			return
		}
		const item = this.#items[option.id] = new boardItems[option.type]((option.parent || this.#config.base).appendChild(sUtils.element.create(`<div class="dash-item"></div>`)));
		item.draw(option);
		return item;
	}
	
	getItem(id){
		return this.#items[id];
	}
}

class SepoaDashboardItem{
	config={};
	elem={};
	constructor(base, type){
		this.config={
			base:base,
			type:type
		}
		this.#drawContainer();
	}
	
	#drawContainer(){
		this.config.base.classList.add(`dash-${this.config.type}`)
		this.config.base.innerHTML = `<div class="dash-label"></div><div class="dash-contents"></div>`;
		this.elem.label=this.config.base.querySelector('.dash-label');
		this.elem.contents=this.config.base.querySelector('.dash-contents');
	}
	
	setLabel(option){
		if(option.label){
			this.elem.label.innerHTML = `<span>${option.label || ''}</span>`
		}else{
			this.elem.label.classList.add('hidden');
		}
	}
	
	setHidden(flag){
		if(flag){
			this.config.base.classList.add("hidden");
		}else{
			this.config.base.classList.remove("hidden");
		}
		this.config.hidden = flag;
	}
	
	isHidden(){
		return this.config.hidden??false
	}
}

boardItems.groupbox = class extends SepoaDashboardItem{
	#items={}
	constructor(base){
		super(base, 'groupbox');
	}
	
	draw(option){
		this.setLabel(option);
		
		if(option.items && option.items.length > 0){
			option.items.forEach((itemOption)=>{
				this.addItem(itemOption);
			})
		}
	}
	
	addItem(option){
		if(!option.id){
			option.id = sUtils.string.randomKey();
		}
		
		const item = this.#items[option.id] = new boardItems.groupbox_group(this.elem.contents.appendChild(sUtils.element.create(`<div></div>`)));
		item.draw(option);
		return item;
	}
	
	getItem(id){
		return this.#items[id];
	}
	
	getItemCount(){
		return Object.keys(this.#items).length;
	}
}
boardItems.groupbox_group = class extends SepoaDashboardItem{
	#items={}
	constructor(base){
		super(base, 'groupbox-group');
	}
	
	draw(option){
		this.setLabel(option);
		this.setCss(option);
		if(option.items && option.items.length > 0){
			option.items.forEach((itemOption)=>{
				this.addItem(itemOption);
			})
		}
	}
	
	setCss(option){
		if(this.config.position){
			this.config.base.classList.remove(`position-${this.config.position}`);
		}
		
		if(!option.position){
			option.position = 'top'
		}
		
		this.config.base.classList.add(`position-${option.position}`);
		this.config.position = option.position;
		
		if(option.width){
			this.config.base.style.flexBasis = `${option.width}px`
		}else{
			this.config.base.style.flex = `1`
		}
	}
	
	addItem(option){
		if(!option.id){
			option.id = sUtils.string.randomKey();
		}
		
		const item = this.#items[option.id] = new boardItems[option.type](this.elem.contents.appendChild(sUtils.element.create(`<div></div>`)));
		item.draw(option);
		return item;
	}
}
boardItems.list = class extends SepoaDashboardItem{
	#items={}
	#template=`<div class="list-item-label">$.{label}</div><div class="list-item-value">$.{value}</div>`
	constructor(base){
		super(base, 'list');
	}
	
	draw(option){
		this.setLabel(option);
		this.setTheme(option);
		if(option.items && option.items.length > 0){
			option.items.forEach((itemOption)=>{
				this.addItem(itemOption);
			})
		}
	}
	
	setTheme(option){
		if(this.config.theme){
			this.config.base.classList.remove(`dash-list-theme-${this.config.theme}`);
		}
		if(!option.theme){
			option.theme = 'default';
		}
		this.config.theme = option.theme;
		this.config.base.classList.add(`dash-list-theme-${this.config.theme}`);
	}
	
	addItem(option){
		if(!option.id){
			option.id = sUtils.string.randomKey();
		}
		this.#drawItem(option);
	}
	
	setItemValue(data){
		Object.keys(data).forEach((key)=>{
			if(this.#items[key]){
				this.#drawItem({
					id:key,
					value:data[key]
				})
			}
		})
	}
	
	#drawItem(option){
		let item = this.#items[option.id]; 
		if(!item){
			item = this.#items[option.id] = this.elem.contents.appendChild(sUtils.element.create(`<div class="dash-list-item"></div>`));
			item.addEventListener('click', (e)=>{
				if(option.events?.click){
					option.events?.click();
				}
			});
		}
		option = item._option = sUtils.object.extend(true, item._option || {}, option);
		
		if(option.events?.click){
			item.classList.add('clickable');
		}else{
			item.classList.remove('clickable');
		}
		if(option.data){
			option.value = "...";
			const data = option.data;
			delete option.data;
			sUtils.connector.service({
				progress:false,
				conType:"CONNECTION",
				requestMode:"doServiceAjax",
				async:true,
				nickName:data.service,
				methodName:data.method,
				callback:(so)=>{
					if(so.status == "1"){
						option.value = so.result[0];
						this.#drawItem(option);
					}
				}
			}, {});
		}
		item.innerHTML = sUtils.string.dataReplacer(this.#template, option);
	}
}

boardItems.tabbar = class extends SepoaDashboardItem{
	#tabbar={
		tabs:{},
		tabIdx:[]
	}
	#active;
	constructor(base){
		super(base, 'tabbar');
	}
	draw(option){
		this.setLabel(option);
		this.elem.contents.innerHTML = `<ul class="dash-tabbar-tabs"></ul><div class="dash-tabbar-container"></div>`;
		this.elem.tabs=this.elem.contents.querySelector('.dash-tabbar-tabs');
		this.elem.cont=this.elem.contents.querySelector('.dash-tabbar-container');
		
		this.elem.tabs.addEventListener('click', (e)=>{
			if(e.srcElement.classList.contains('dash-tabbar-tab') && !e.srcElement.classList.contains('active')){
				this.setActive(e.srcElement.dataset.id);
			}
		})
		
		if(option.tabs && option.tabs.length > 0){
			option.tabs.forEach((tabOption)=>{
				this.addTab(tabOption);
			})
		}
	}
	addItem(option){
		if(!option.id){
			option.id = sUtils.string.randomKey()
		}
		let tab = this.#tabbar.tabs[option.id];
		if(!tab){
			tab = this.#tabbar.tabs[option.id]={
				tab:this.elem.tabs.appendChild(sUtils.element.create(`<li class="dash-tabbar-tab" data-id="${option.id}">${option.label || ""}</li>`)),
				cell:this.elem.cont.appendChild(sUtils.element.create(`<div class="dash-tabbar-cell"></div>`))
			}
			this.#tabbar.tabIdx.push(option.id);
			if(option.items && option.items.length > 0){
				tab.items = [];
				option.items.forEach((itemOption)=>{
					const item = new boardItems[itemOption.type](tab.cell.appendChild(sUtils.element.create(`<div></div>`)));
					item.draw(itemOption);
					tab.items.push(item);
				})
			}
			tab.cell.addEventListener("click", (e)=>{
				if(option.events?.cellClick){
					option.events.cellClick();
					e.stopImmediatePropagation();
					e.preventDefault();
				}
			})
		}
		if(option.events){
			if(option.events.cellClick){
				tab.cell.classList.add("clickable");
			}
		}
		if(option.active){
			this.setActive(option.id);
		}
	}
	setActive(id){
		if(this.#active){
			this.#tabbar.tabs[this.#active].tab.classList.remove('active');
			this.#tabbar.tabs[this.#active].cell.classList.remove('active');
			this.#active = null;
		}
		
		if(this.#tabbar.tabs[id]){
			this.#tabbar.tabs[id].tab.classList.add('active');
			this.#tabbar.tabs[id].cell.classList.add('active');
			this.#active = id;
		}
	}
	getTabID(idx){
		return this.#tabbar.tabIdx[idx];
	}
}
boardItems.grid = class extends SepoaDashboardItem{
	#grid={
		header:{},
		headerIdx:[],
		maxDataCount:5
	}
	#events={}
	#template={
		blank:`<div class="dash-grid-row dash-grid-blank">${sUtils.cache.text.get('SUMMARY.NO_DATA')}</div>`
	}
	constructor(base){
		super(base, 'grid');
	}
	
	draw(option){
		this.setLabel(option);
		this.#drawGrid(option);
		this.#loadData(option);
	}
	
	#drawGrid(option){
		this.elem.contents.innerHTML = `<div class="dash-grid-header"></div><div class="dash-grid-contents">${this.#template.blank}</div>`;
		this.elem.gHdr=this.elem.contents.querySelector('.dash-grid-header');
		this.elem.gCont=this.elem.contents.querySelector('.dash-grid-contents');
		
		if(option.maxDataCount){
			this.#grid.maxDataCount = option.maxDataCount;
		}
		
		if(option.grid && option.grid.length > 0){
			option.grid.forEach((column)=>{
				this.#drawColumn(column);
			})
		}
		
		if(option.events){
			Object.keys(option.events).forEach((k)=>{
				this.#events[k] = option.events[k];
			})
		}
		
		if(this.#events.click){
			this.elem.gCont.classList.add("clickable");
			this.elem.gCont.addEventListener("click", (e)=>{
				let target = e.srcElement;
				if(!target.classList.contains("dash-grid-cell")){
					target = target.closest(".dash-grid-cell"); 
				}
				if(target){
					this.#events.click({
						id:target.dataset.cell,
						data:target.parentElement._data
					})
				}
			})
		}
	}
	
	#drawColumn(option){
		let item = this.#grid.header[option.id];
		if(!item){
			item = this.#grid.header[option.id] = {
				id:option.id,
				align:option.align || "center",
				hidden:sUtils.string.toBoolean(option.hidden || "false"),
				width:option.width,
				format:option.format,
				mapping:option.mapping
			};
			item.hdr=this.elem.gHdr.appendChild(sUtils.element.create(`<div class="dash-grid-header-item ${item.hidden?"hidden":""}">${option.label}</div>`));
			if(option.width){
				item.hdr.style.flex = "none";
				item.hdr.style.width = item.width+"px";
			}else{
				item.hdr.style.flex = "1";
			}
			
			this.#grid.headerIdx.push(option.id);
		}
	}
	
	#loadData(option){
		const data = option.data;			
		sUtils.connector.service({
			progress:false,
			conType:"CONNECTION",
			requestMode:"doServiceAjax",
			async:true,
			nickName:data.service,
			methodName:data.method,
			callback:(so)=>{
				if(so.status == "1"){
					this.#drawRow(new SepoaFormater(so.result[0]).getArrayMap());
				}
			}
		}, data.param || {});
	}
	
	#drawRow(data){
		if(!data || data.length <= 0){
			this.elem.gCont.innerHTML = this.#template.blank;
			return;
		}
		if(data.length > this.#grid.maxDataCount){
			data.splice(this.#grid.maxDataCount);
		}
		this.elem.gCont.innerHTML = '';
		data.forEach((rowData)=>{
			this.#renderRow(this.elem.gCont.appendChild(sUtils.element.create(`<div class="dash-grid-row"></div>`)), rowData);
		})
	}
	
	#renderRow(row, rowData){
		row._data = rowData;
		this.#grid.headerIdx.forEach((id)=>{
			const cellOption = this.#grid.header[id];  
			
			let value = rowData[id];
			if(cellOption.mapping){
				value = sUtils.string.dataReplacer(cellOption.mapping, rowData);
			}
			const cell = row.appendChild(sUtils.element.create(`<div data-cell="${id}" class="dash-grid-cell ${cellOption.hidden?"hidden":""} dash-grid-cell-align-${cellOption.align}"><span>${value || ""}</span></div>`));
			if(cellOption.width){
				cell.style.flex = "none";
				cell.style.width = cellOption.width+"px";
			}else{
				cell.style.flex = "1";
			}
		})
	}
}

boardItems.column = class extends SepoaDashboardItem{
	#items={}
	constructor(base){
		super(base, 'column');
	}
	
	draw(option){
		this.setLabel({});
		if(option.items && option.items.length > 0){
			option.items.forEach((itemOption)=>{
				this.addItem(itemOption);
			})
		}
	}
	
	addItem(option){
		if(!option.id){
			option.id = sUtils.string.randomKey();
		}
		this.#drawItem(option);
	}
	
	#drawItem(option){
		let item = this.#items[option.id]; 
		if(!item){
			item = this.#items[option.id] = new boardItems[option.type](this.elem.contents.appendChild(sUtils.element.create(`<div class="dash-column-item"></div>`)));
			item.draw(option);
			
			const itemKeyArray = Object.keys(this.#items);
			itemKeyArray.forEach((id)=>{
				this.#items[id].config.base.style.cssText = `width:calc( calc(100% - calc(var(--dash-item-gap) * ${itemKeyArray.length - 1})) / ${itemKeyArray.length})`;
			})
		}
	}
}

boardItems.chart = class extends SepoaDashboardItem{
	#chart
	constructor(base){
		super(base, 'chart');
	}
	
	draw(option){
		this.setLabel(option);
		this.#drawBase(option);
		this.#loadData(option);
	}
	
	#drawBase(option){
		this.elem.canvas = this.elem.contents.appendChild(sUtils.element.create( `<canvas class="dash-chart-canvas"></canvas>`));
		
		if(option.chart.options?.plugins?.datalabels){
			option.chart.plugins=[ChartDataLabels]
		}
		
		this.#renderChart(option.chart);
	}
	
	#loadData(option){
		const data = option.data;			
		sUtils.connector.service({
			progress:false,
			conType:"CONNECTION",
			requestMode:"doServiceAjax",
			async:true,
			nickName:data.service,
			methodName:data.method,
			callback:(so)=>{
				if(so.status == "1"){
					const orgData = JSON.parse(so.result[0]);
					const chartOption = sUtils.object.extend(true, {}, option.chart, (data.parser??(()=>{
						return {
                            options:{
                                title:{
                                    text:orgData.title
                                }
                            },
                            data:{
                                datasets: [{
                                    backgroundColor:orgData.color,
                                    data: orgData.data
                                }],
                                labels:orgData.label
                            }
                        }
					}))(orgData))
					this.#renderChart(chartOption);
				}
			}
		}, {});
	}
	
	#renderChart(option){
		option = sUtils.object.extend(true, {}, option)
		if(!this.#chart){
			this.#chart = new Chart(this.elem.canvas, option);
		}
		
		let hasUpdate = false;
		if(option?.options){
			this.#chart.options = option.options;
			hasUpdate = true;
		}
		if(option?.data){
			this.#chart.data = option.data;
			hasUpdate = true;
		}
		if(hasUpdate){
			this.#chart.update();
		}
	}
}
boardItems.button = class extends SepoaDashboardItem{
	#button = new SepoaButton();
	constructor(base){
		super(base, 'button');
	}
	
	draw(option){
		this.setLabel({});
		if(option.items && option.items.length > 0){
			option.items.forEach((itemOption)=>{
				this.addItem(itemOption);
			})
		}
	}
	
	addItem(option){
		if(!option.id){
			option.id = sUtils.string.randomKey();
		}
		this.#button.addButton(this.elem.contents, option);
	}
}
boardItems.openlayers = class extends SepoaDashboardItem{
	#olObject;
	constructor(base){
		super(base, 'openlayers');
	}
	
	draw(option){
		this.setLabel(option);
		this.#initOpenlayers(option);
		this.#loadData(option);
	}
	
	#initOpenlayers(option){
		if(!this.openlayers){
			this.#olObject = new ol.Map({
				target:this.elem.contents,
				layers:(option.map?.layers || [ 
					new ol.layer.Tile({
						source : new ol.source.OSM()
					})
				]),
				view:(option.map?.view || new ol.View({
					center:ol.proj.fromLonLat([ 128.0, 36.0 ]),
					zoom:0
				}))
			});
			this.#olObject.on('click', (e)=>{
				let feature = this.#olObject.forEachFeatureAtPixel(e.pixel, function(f) {
					return f;
				});
				if (feature && option.events?.click) {
					option.events?.click(feature);
				}
			});
			this.#olObject.on('pointermove', (e)=>{
				if (e.dragging) {
					return;
				}
				if (this.#olObject.hasFeatureAtPixel(this.#olObject.getEventPixel(e.originalEvent))) {
					this.elem.contents.classList.add('feature-hover');
				} else {
					this.elem.contents.classList.remove('feature-hover');
				}
			});
		}
	}
	#loadData(option){
		const data = option.data;			
		sUtils.connector.service({
			progress:false,
			conType:"CONNECTION",
			requestMode:"doServiceAjax",
			async:true,
			nickName:data.service,
			methodName:data.method,
			callback:(so)=>{
				if(so.status == "1"){
					const orgData = JSON.parse(so.result[0]);
					this.#drawFeatureLayer((data.parser??(()=>{
						const features = [];
						const radiusScale = 50000;
						orgData.forEach((country)=>{
							let radius;
							if(country.POPULATION.length == 1){
								radius = Math.sqrt((country.POPULATION + '00') / Math.PI) * radiusScale;
							}else {
								radius = Math.sqrt((country.POPULATION + '0') / Math.PI) * radiusScale;
							}
							const coords = ol.proj .fromLonLat([ country.LONGITUDE, country.LATITUDE ]);
							const circleFeature = new ol.Feature({
								geometry : new ol.geom.Circle(coords, radius),
								population: country.POPULATION,
								name : country.NAME
							});
							features.push(circleFeature);
						});
						return features;
					}))(orgData), option.map?.featureStyle);
				}
			}
		}, {});
	}
	#drawFeatureLayer(features, style){
		this.#olObject.addLayer(new ol.layer.Vector({
			source: new ol.source.Vector({
				features : features
			}),
			style: style || ((feature)=>{
			    return new ol.style.Style({
			    	text: new ol.style.Text({
			    		text: feature.get("population").toString(),
			    		font: "bold 12px Arial",
			    		fill: new ol.style.Fill({
			    			color: "black"
			    		}),
			    		stroke: new ol.style.Stroke({
			    			color: "white",
			    			width: 2
			    		})
			    	}),
			    	fill: new ol.style.Fill({
			    		color: "rgba(255, 0, 255, 0.1)"
			    	}),
			    	stroke: new ol.style.Stroke({
			    		color: "white",
			    		width: 2
			    	})
			    });
			})
		}));
	}
}
boardItems.flowview = class extends SepoaDashboardItem{
	#baseOption = {
		view:'fix',
		template:{
			content:`<div class="dash-list-item clickable" data-id="#id#"><div class="list-item-label">#text#</div><div class="list-item-value">...</div></div>`
		}
	}
	#flowObj
	constructor(base){
		super(base, 'flowview');
	}
	
	draw(option){
		this.setLabel(option);
		this.#init(option);
		if(option.data){
			this.#loadData(option);
		}else{
			this.#flowCustom(option);
		}
	}
	
	#init(option){
		this.#flowObj = new SepoaFlowView(this.elem.contents, sUtils.object.extend(true,{},this.#baseOption), option.items);
	}
	
	#loadData(option){
		sUtils.connector.api('service', {
			target:{
				name:option.data.layout.service,
				action:option.data.layout.method
			},
			data:{}
		}).then(res => {
			res.result?.[0]?.forEach((group)=>{
				this.#flowObj.addItem(group)
			})
			let countPromise = null;
			if(option.data?.count){
				countPromise = sUtils.connector.api('service', {
					target:{
						name:option.data.count.service,
						action:option.data.count.method
					},
					data:{}
				})
			}
			this.#flowCustom(option);
			return countPromise;
		})?.then(res => {
			const countData = res.result?.[0];
			if(countData){
				Object.keys(countData).forEach((k)=>{
					const itemNode = this.elem.contents.querySelector(`.dash-list-item[data-id=${k}]`);
					if(itemNode){
						itemNode.children[1].innerText = countData[k];
					}
				})
			}
		})
	}
	
	#flowCustom(option){
		this.elem.contents.querySelectorAll('.sepoa-flow-view-item-contents').forEach((node)=>{
			node.classList.add('dash-list');
			node.classList.add('dash-list-theme-summary');
		})
		
		this.elem.contents.addEventListener('click', (e)=>{
			let target = null;
			if(e.srcElement.classList.contains('dash-list-item')){
				target = e.srcElement 
			}else{
				target = e.srcElement.closest('.dash-list-item');
			}
			if(target != null){
				if(option.events?.click){
					option.events.click(target.dataset.id, target._data);
				}
			}
		})
	}
}