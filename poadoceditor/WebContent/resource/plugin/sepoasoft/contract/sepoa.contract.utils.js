sUtils.contract={};
sUtils.contract.element={
	customSelect:function(dom){
		var x, i, j, selElmnt, a, b, c;
		x = $(dom).find('.selectContain');
		for (i = 0; i < x.length; i++) {
			selElmnt = x[i].getElementsByTagName("select")[0];
			
			a = document.createElement("DIV");
			a.setAttribute("class", "select-selected");
			a.innerHTML = selElmnt.options[selElmnt.selectedIndex] ? selElmnt.options[selElmnt.selectedIndex].innerHTML : '';
			x[i].appendChild(a);
			b = document.createElement("DIV");
			b.setAttribute("class", "select-items select-hide");
			for (j = 1; j < selElmnt.length; j++) {
				c = document.createElement("DIV");
				c.innerHTML = selElmnt.options[j].innerHTML;
				c.addEventListener("click", function(e) {
					var y, i, k, s, h;
					s = this.parentNode.parentNode
							.getElementsByTagName("select")[0];
					h = this.parentNode.previousSibling;
					for (i = 0; i < s.length; i++) {
						if (s.options[i].innerHTML == this.innerHTML) {
							s.selectedIndex = i;
							h.innerHTML = this.innerHTML;
							y = this.parentNode
									.getElementsByClassName("same-as-selected");
							for (k = 0; k < y.length; k++) {
								y[k].removeAttribute("class");
							}
							this.setAttribute("class","same-as-selected");
							$(s).trigger('change');
							break;
						}
					}
					h.click();
				});
				b.appendChild(c);
			}
			
			$(selElmnt).on('change',function(e){
				var t = this.nextElementSibling.innerHTML = this.options[this.selectedIndex].innerHTML 
				var s = this.nextElementSibling.nextElementSibling
				for(var i=0 ; i < s.childElementCount; i++){
					if(s.children[i].innerHTML == t){
						s.children[i].setAttribute("class","same-as-selected");
					}else{
						s.children[i].removeAttribute("class");
					}
				}
			});
			
			x[i].appendChild(b);
			a.addEventListener("click", function(e) {
				e.stopPropagation();
				sUtils.contract.element.closeAllSelect(this);
				if(e.target.getAttribute("disabled")!="true"){
					this.nextSibling.classList.toggle("select-hide");
					this.classList.toggle("select-arrow-active");
				}
			});
		}
	},
	closeAllSelect:function(elmnt) {
		var x, y, i, arrNo = [];
		x = document.getElementsByClassName("select-items");
		y = document.getElementsByClassName("select-selected");
		for (i = 0; i < y.length; i++) {
			if (elmnt == y[i]) {
				arrNo.push(i)
			} else {
				y[i].classList.remove("select-arrow-active");
			}
		}
		for (i = 0; i < x.length; i++) {
			if (arrNo.indexOf(i)) {
				x[i].classList.add("select-hide");
			}
		}
	}
}

