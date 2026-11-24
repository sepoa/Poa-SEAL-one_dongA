class SepoaMenuTileRenderer{
	constructor(){
		sUtils.modules.extend(this, "EventManager");
	}
	#itemInfo={
		items:{},
		index:[]
	}
	#config={
		theme:"summary",
		boxSize:46
	}
	#elem={}
	
	addItem(options){
		this.#itemInfo.items[options.id] = options;
		this.#itemInfo.index.push(options.id);
	}
	
	getItemCount(){
		return this.#itemInfo.index.length;
	}
	
	draw(option){
		this.#elem.base = option.base;
		this.#elem.base.innerHTML = `<h4><span>${option.label}</span></h4><div class="${this.#config.theme}-container ${this.#config.theme}-components-small"></div>`;
		this.#elem.itemContainer = this.#elem.base.querySelector(`div.${this.#config.theme}-container`); 
		this.#itemInfo.index.forEach((id)=>{
			this.#drawItem(this.#itemInfo.items[id]);
		})
	}
	
	#drawItem(option){
		const itemBase = this.#elem.itemContainer.appendChild(document.createElement("div"));
		itemBase.classList.add(`${this.#config.theme}-component-small`);
		itemBase.itemOption = option;
		itemBase.style.height=`${this.#config.boxSize + 72}px`;
		itemBase.innerHTML=
`<a class="${this.#config.theme}-component__item">
	<span class="${this.#config.theme}-component__summaryName">${option.label}</span>
	<span class="${this.#config.theme}-component__box"">
		<span class="${this.#config.theme}-component__content">
			<span class="${this.#config.theme}-component__icon"></span>
		</span>
	</span>
</a>`;
		itemBase.addEventListener('click', ()=>{
			this.callEvent('onItemClick', [option, itemBase]);
		});
		
		//icon
		const imageArea = itemBase.querySelector(`span.${this.#config.theme}-component__icon`);
		const image = (option.img || "").trim();
		if(image.indexOf("jpg") != -1 || image.indexOf("gif") != -1 || image.indexOf("png") != -1){
			imageArea.innerHTML = 
`<div class="${this.#config.theme}-component__icon-svg">
	<img id="${option.id}" src="${window.CONTEXT_NAME}/resource/design/imgs/menu/${image}" class="${this.#config.theme}-component__icon-use" />
</div>`
		}else if(image.indexOf("svg") != -1){
			$.ajax({
				url: window.CONTEXT_NAME+'/resource/design/imgs/menu/'+_img,
				type:"post",
				dataType: "xml",
				success: (data)=>{
					if(data){
						const svgElement = data.getElementsByTagName("svg")[0];
						svgElement.setAttribute("class", `${this.#config.theme}-component__icon-use`);
						imageArea.appendChild(svgElement);
					}
				}
			});
		}else{
			imageArea.innerHTML = 
`<svg style="display:none;">
	<symbol id="${option.id}" viewBox="0 0 ${this.#config.boxSize} ${this.#config.boxSize}">
		<path d="${image}"></path>
	</symbol>
</svg>
<svg class="${this.#config.theme}-component__icon-svg">
	<use class="${this.#config.theme}-component__icon-use" xlink:href="#${option.id}"></use>
</svg>
`
		}
		
		//summary
		if(option.summary == "Y"){
			var sumamryDiv = document.createElement("div");
			sumamryDiv.setAttribute("lang", option.id);
			sumamryDiv.setAttribute("class", `${this.#config.theme}-component__summary`);
			sumamryDiv.innerHTML= `<img src="${window.CONTEXT_NAME}/resource/design/imgs/summary/loading.gif" border="0" style="width:100%;height:100%;"/>`;
			itemBase.children[0].appendChild(sumamryDiv);
		}
		
		this.callEvent("onItemDraw", [option, itemBase]);
	}
}