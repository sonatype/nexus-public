/*
	xBreadcrumbs (Extended Breadcrums) jQuery Plugin
	© 2009 ajaxBlender.com
	For any questions please visit www.ajaxblender.com 
	or email us at support@ajaxblender.com
*/

;(function($){
	/*  Variables  */
	$.fn.xBreadcrumbs = function(settings){
		var element = $(this);
		var 
		
		settings = $.extend({}, $.fn.xBreadcrumbs.defaults, settings);

		function _build(){
			if(settings.collapsible){
				var sz = element.children('LI').length;
				element.children('LI').children('A').css('white-space', 'nowrap').css('float', 'left');
				element.children('LI').children('A').each(function(i, el){
					if(i != sz - 1){
						$(this).css('overflow', 'hidden');
						$(this).attr('init-width', $(this).width());
						$(this).width(settings.collapsedWidth);
					}
				});
			}
			
            element.children('LI').mouseenter(function(){
                if($(this).hasClass('hover')){ return; }
                
            	_hideAllSubLevels();
            	if(!_subLevelExists($(this))){ return; }

            	// Show sub-level
            	var subLevel = $(this).children('UL');
            	_showHideSubLevel(subLevel, true);
            	
            	if(settings.collapsible && !$(this).hasClass('current')){
            		var initWidth = $(this).children('A').attr('init-width');
            		$(this).children('A').animate({width: initWidth}, 'normal');
            	}
            });
            
            element.children('LI').mouseleave(function(){
                var subLevel = $(this).children('UL');
                _showHideSubLevel(subLevel, false);
                
                if(settings.collapsible && !$(this).hasClass('current')){
                	$(this).children('A').animate({width: settings.collapsedWidth}, 'fast');
                }
            });
		};
		
		function _hideAllSubLevels(){
			element.children('LI').children('UL').each(function(){
                $(this).hide();
                $(this).parent().removeClass('hover');
			});
		};
		
		function _showHideSubLevel(subLevel, isShow){
			if(isShow){
                subLevel.parent().addClass('hover');
                if($.browser.msie){
                	var pos = subLevel.parent().position();
                	subLevel.css('left', parseInt(pos['left']));
                }
				if(settings.showSpeed != ''){ subLevel.fadeIn( settings.showSpeed ); } 
				else { subLevel.show(); }
			} else {
                subLevel.parent().removeClass('hover');
                if(settings.hideSpeed != ''){ subLevel.fadeOut( settings.hideSpeed ); } 
                else { subLevel.hide(); }
			}
		};
		
		function _subLevelExists(obj){
			return obj.children('UL').length > 0;
		};
		
		//    Entry point
		_build();
	};
	
	/*  Default Settings  */
	$.fn.xBreadcrumbs.defaults = {
		showSpeed:        'fast',
		hideSpeed:        '',
		collapsible:      false,
		collapsedWidth:   10
	};
})(jQuery);