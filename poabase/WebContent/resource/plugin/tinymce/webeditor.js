function initTinyMCE(id, callback, l) {
	var language = 'ko_KR'
	switch(l){
	case 'KO':
		language = 'ko_KR';
		break;
	case 'VI':
		langague = 'vi_VN';
		break;
	}
	
	var options = {
        theme: "modern",
        plugins: "advlist autolink lists link image charmap print preview hr anchor pagebreak searchreplace wordcount visualblocks visualchars code fullscreen insertdatetime media nonbreaking save table contextmenu directionality emoticons template textcolor colorpicker textpattern paste",
        toolbar1: "styleselect formatselect fontselect fontsizeselect | bold italic underline strikethrough | alignleft aligncenter alignright alignjustify | cut copy paste | searchreplace | bullist numlist | outdent indent blockquote | undo redo | link unlink anchor code | insertdatetime preview | forecolor backcolor | table | hr removeformat | subscript superscript | charmap emoticons | print fullscreen | ltr rtl | spellchecker | visualchars visualblocks nonbreaking template pagebreak",
        //toolbar2: "undo redo | link unlink anchor image media code | insertdatetime preview | forecolor backcolor | table | hr removeformat | subscript superscript | charmap emoticons | print fullscreen | ltr rtl | spellchecker | visualchars visualblocks nonbreaking template pagebreak",
        paste_data_images:true,
        image_advtab: true,
        language: language,
        skin: 'lightgray',
        forced_root_block: false,
        force_br_newlines : true,
        paste_auto_cleanup_on_paste: false,                  
        paste_retain_style_properties : "all",
        paste_strip_class_attributes : "none",
        extended_valid_elements:"a[name|href|target|title|onclick],img[class|src|border=0|alt|title|hspace|vspace|width|height|align|onmouseover|onmouseout|name],table[style|class|border=1|width|cellspacing|cellpadding|bgcolor],colgroup,col[style|width],tbody,tr[style|class],td[style|class|colspan|rowspan|width|height],hr[class|width|size|noshade],font[face|size|color|style],span[class|align|style]",
        menubar: "false",
        statusbar: false,
        file_browser_callback: function(field_name, url, type, win) {
            tinyImageFileFieldName = field_name;
            if (type == 'image') $('#tinyFielForm input').click();
        },
        content_css: [],
        font_formats: 'Andale Mono=andale mono,times;' + 
        			'Arial=arial,helvetica,sans-serif;' + 
        			'Arial Black=arial black,avant garde;' + 
        			'Book Antiqua=book antiqua,palatino;' + 
        			'Comic Sans MS=comic sans ms,sans-serif;' + 
        			'Courier New=courier new,courier;' + 
        			'Georgia=georgia,palatino;' + 
        			'Helvetica=helvetica;' + 
        			'Impact=impact,chicago;' + 
        			'Symbol=symbol;' + 
        			'Tahoma=tahoma,arial,helvetica,sans-serif;' + 
        			'Terminal=terminal,monaco;' + 
        			'Times New Roman=times new roman,times;' + 
        			'Trebuchet MS=trebuchet ms,geneva;' + 
        			'Verdana=verdana,geneva;' + 
        			'Webdings=webdings;' + 
        			'Wingdings=wingdings,zapf dingbats;' + 
        			'본고딕=Noto Sans KR;',
		setup : function(ed) {
			ed.on("keydown",function(evt) {
				if (evt.keyCode == 9){
					ed.execCommand('mceInsertContent', false, '<b>&emsp;</b>');
					evt.preventDefault();
					evt.stopPropagation();
					return false;
				}
			});
		},
        init_instance_callback: function(obj){
        	//obj.contentDocument.body.focus=function(){}
        	obj._focus = obj.focus;
        	obj.focus = function(){};
        	obj.setMode('code')
        	obj.focus = obj._focus;
        	delete obj._focus;
        	$(obj.contentDocument).on({
        		keydown:function(event){
        			if([33,34].indexOf(event.keyCode)+1){
        				event.preventDefault();
        				return false;
    				}
        		}
        	})
        	if(callback) callback(obj);
        }
    }
	
	if(typeof(id) == "string"){
		options.selector = id;
	}else{
		options.target = id;
	}
    tinymce.init(options);
}

/**
 * 에디터가 여러개일 경우 아래 펑션을 이용하여 GET SET... 을 하자 
 */

function setTinyMode(id, mode){
	getTinyEditor(id).setMode(mode);
}

function getTinyContent(id){
	return getTinyEditor(id).getContent();
}

function setTinyContent(id, content){
	getTinyEditor(id).setContent(content);
}

function getTinyEditor(id){
	for(var i = 0 ; i < tinymce.get().length ; i++){
		if(tinymce.get(i).id == id){
			return tinymce.get(i);
		}
	}
	return null;
}