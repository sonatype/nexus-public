define('ext/ux/browsebutton',['extjs'], function(Ext){
/*
 * Public Domain
 * http://www.sencha.com/forum/showthread.php?29032-Ext.ux.form.BrowseButton
 */

/*global Ext*/
Ext.namespace('Ext.ux.form');

// @author 4Him
//if(typeof Ext.isIE8 !== 'boolean') { // Ext 2.x (at least 2.1) doesn't know IE8. Let's tell him about it...
//    Ext.isIE8 = Ext.isIE && navigator.userAgent.toLowerCase().indexOf("msie 8")>-1;
//}

/**
 * @class Ext.ux.form.BrowseButton
 * @extends Ext.Button
 * Ext.Button that provides a customizable file browse button.
 * Clicking this button, pops up a file dialog box for a user to select the file to upload.
 * This is accomplished by having a transparent <input type="file"> box above the Ext.Button.
 * When a user thinks he or she is clicking the Ext.Button, they're actually clicking the hidden input "Browse..." box.
 * Note: this class can be instantiated explicitly or with xtypes anywhere a regular Ext.Button can be except in 2 scenarios:
 * - Panel.addButton method both as an instantiated object or as an xtype config object.
 * - Panel.buttons config object as an xtype config object.
 * These scenarios fail because Ext explicitly creates an Ext.Button in these cases.
 * Browser compatibility:
 * Internet Explorer 6:
 * - no issues
 * Internet Explorer 7:
 * - no issues
 * Internet Explorer 8:
 * - no issues
 * Firefox 3 - Windows:
 * - pointer cursor doesn't display when hovering over the button.
 * @author loeppky - based on the work done by MaximGB in Ext.ux.UploadDialog (http://extjs.com/forum/showthread.php?t=21558)
 * The follow the curosr float div idea also came from MaximGB. With patches for IE8 by 4Him.
 * @see http://extjs.com/forum/showthread.php?t=29032
 * @constructor
 * Create a new BrowseButton.
 * @param {Object} config Configuration options
 */

/*global Ext*/
Ext.ux.form.BrowseButton = Ext.extend(Ext.Button, {
    /*
     * Config options:
     */
    /**
     * @cfg {String} inputFileName
     * Name to use for the hidden input file DOM element.  Deaults to "file".
     */
    inputFileName: 'file',

    /**
     * @cfg {Boolean} debug
     * Toggle for turning on debug mode.
     * Debug mode doesn't make clipEl transparent so that one can see how effectively it covers the Ext.Button.
     * In addition, clipEl is given a green background and floatEl a red background to see how well they are positioned.
     */
    debug: window.location.search === '?debug',

    /*
     * Private constants:
     */
    /**
     * @property FLOAT_EL_WIDTH
     * @type Number
     * The width (in pixels) of floatEl.
     * It should be less than the width of the IE "Browse" button's width (65 pixels), since IE doesn't let you resize it.
     * We define this width so we can quickly center floatEl at the mouse cursor without having to make any function calls.
     * @private
     */
    FLOAT_EL_WIDTH: 60,
    
    /**
     * @property FLOAT_EL_HEIGHT
     * @type Number
     * The heigh (in pixels) of floatEl.
     * It should be less than the height of the "Browse" button's height.
     * We define this height so we can quickly center floatEl at the mouse cursor without having to make any function calls.
     * @private
     */
    FLOAT_EL_HEIGHT: 18,
    
    
    /*
     * Private properties:
     */
    /**
     * @property buttonCt
     * @type Ext.Element
     * Element that contains the actual Button DOM element.
     * We store a reference to it, so we can easily grab its size for sizing the clipEl.
     * @private
     */
    buttonCt: null,
    /**
     * @property clipEl
     * @type Ext.Element
     * Element that contains the floatEl.
     * This element is positioned to fill the area of Ext.Button and has overflow turned off.
     * This keeps floadEl tight to the Ext.Button, and prevents it from masking surrounding elements.
     * @private
     */
    clipEl: null,
    /**
     * @property floatEl
     * @type Ext.Element
     * Element that contains the inputFileEl.
     * This element is size to be less than or equal to the size of the input file "Browse" button.
     * It is then positioned wherever the user moves the cursor, so that their click always clicks the input file "Browse" button.
     * Overflow is turned off to preven inputFileEl from masking surrounding elements.
     * @private
     */
    floatEl: null,
    /**
     * @property inputFileEl
     * @type Ext.Element
     * Element for the hiden file input.
     * @private
     */
    inputFileEl: null,
    /**
     * @property originalHandler
     * @type Function
     * The handler originally defined for the Ext.Button during construction using the "handler" config option.
     * We need to null out the "handler" property so that it is only called when a file is selected.
     * @private
     */
    originalHandler: null,
    /**
     * @property originalScope
     * @type Object
     * The scope originally defined for the Ext.Button during construction using the "scope" config option.
     * While the "scope" property doesn't need to be nulled, to be consistent with originalHandler, we do.
     * @private
     */
    originalScope: null,
    /**
     * @property BROWSERS_OFFSET
     * @type Object
     * The browsers specific offsets used to position the clipping element for better overlay tightness. For
     * Ext 3, Ext 2 offsets are used unless there is an Ext 3 entry.
     * @private
     * @author 4Him
     */
    BROWSERS_OFFSETS: {
        Ext2: {
            IE10:  {left: -8, top: -16, width: 16,  height: 22},
            IE9:   {left: -8, top: -16, width: 16,  height: 22},
            IE8:   {left: -8, top: -16, width: 16,  height: 22},
            IE:    {left: -8, top: -3,  width: 16,  height: 6},
            Opera: {left: -8, top: -3,  width: -18, height: -1},
            Gecko: {left: -8, top: -6,  width: 16,  height: 10},
            Safari:{left: -4, top: -2,  width: 6,   height: 6},
            Chrome:{left: -4, top: -2,  width: 6,   height: 6}
        },
        Ext3: {
            IE10:  {left: -7,           width: 10},
            IE9:   {left: -7,           width: 10},
            IE8:   {left: -7,           width: 10},
            IE:    {left: -3,           width: 6},
            Gecko: {                    width: 11}
        }
    },
    /**
     * @property isExt2x
     * @type boolean
     * Whether we are currently using Ext 2.x
     * @private
     * @author 4Him
     */
    isExt2x: false, // HACK: jdillon we are only in 3 now fuck 2
    
    
    /*
     * Protected Ext.Button overrides
     */
    /**
     * @see Ext.Button.initComponent
     */
    initComponent: function(){
        Ext.ux.form.BrowseButton.superclass.initComponent.call(this);

        // FIXME: This should never have fucked with the handler, should have registered a new event

        // Store references to the original handler and scope before nulling them.
        // This is done so that this class can control when the handler is called.
        // There are some cases where the hidden file input browse button doesn't completely cover the Ext.Button.
        // The handler shouldn't be called in these cases.  It should only be called if a new file is selected on the file system.  
        this.originalHandler = this.handler;
        this.originalScope = this.scope;
        this.handler = null;
        this.scope = null;
    },
    
    /**
     * @see Ext.Button.onRender
     */
    onRender: function(ct, position){
        Ext.ux.form.BrowseButton.superclass.onRender.call(this, ct, position); // render the Ext.Button
        // Patch for compatibility with 3.x (@author 4Him, based on dario's 05-10-2009 post)
        if(this.isExt2x) {
            this.buttonCt = this.el.child('.x-btn-center em');
        } else {
            this.buttonCt = this.el.child('.x-btn-mc em');
        }

        this.buttonCt.position('relative'); // this is important!
        var styleCfg = {
            position: 'absolute',
            overflow: 'hidden',
            top: '0px', // default
            left: '0px' // default
        };

        // browser specifics for better overlay tightness - modified by 4Him
        for(var browser in this.BROWSERS_OFFSETS.Ext2) {
            if(Ext['is'+browser]) {
                Ext.apply(styleCfg, {
                    left: this.getBrowserOffset(browser, 'left')+'px',
                    top: this.getBrowserOffset(browser, 'top')+'px'
                });
                break;
            }
        }
        this.clipEl = this.buttonCt.createChild({
            tag: 'div',
            style: styleCfg
        });
        this.setClipSize();
        this.clipEl.on({
            'mousemove': this.onButtonMouseMove,
            'mouseover': this.onButtonMouseMove,
            scope: this
        });
        
        this.floatEl = this.clipEl.createChild({
            tag: 'div',
            style: {
                position: 'absolute',
                width: this.FLOAT_EL_WIDTH + 'px',
                height: this.FLOAT_EL_HEIGHT + 'px',
                overflow: 'hidden'
            }
        });
        
        
        if (this.debug) {
            this.clipEl.applyStyles({
                'background-color': 'green'
            });
            this.floatEl.applyStyles({
                'background-color': 'red'
            });
        } else {
            // We don't set the clipEl to be transparent, because IE 6/7 occassionaly looses mouse events for transparent elements.
            // We have listeners on the clipEl that can't be lost as they're needed for realligning the input file element.
            this.floatEl.setOpacity(0.0);
        }
        
        // Cover cases where someone tabs to the button:
        // Listen to focus of the button so we can translate the focus to the input file el.
        var buttonEl = this.el.child(this.buttonSelector);
        buttonEl.on('focus', this.onButtonFocus, this);
        // In IE, it's possible to tab to the text portion of the input file el.  
        // We want to listen to keyevents so that if a space is pressed, we "click" the input file el.
        if (Ext.isIE) {
            this.el.on('keydown', this.onButtonKeyDown, this);
        }
        
        this.createInputFile();
    },
    
    
    /*
     * Private helper methods:
     */
    /**
     * Returns an offset based on this.BROWSERS_OFFSET 
     * If currently using Ext 3.x, tries to find a value for 3.x and if there is none for 3.x, it
     * returns a value for 2.x
     * @param {string} which the desired offset. Can be one of the following: 'left', 'top', 'width', 'height'
     * @param {string} browser the browser for which to return the offset
     * @return {int} the desired offset
     * @author 4Him
     */
    getBrowserOffset: function(browser, which) {
        if(!this.isExt2x && this.BROWSERS_OFFSETS.Ext3[browser] && this.BROWSERS_OFFSETS.Ext3[browser][which]) {
            return this.BROWSERS_OFFSETS.Ext3[browser][which];
        } else {
            return this.BROWSERS_OFFSETS.Ext2[browser][which];
        }
    },

    /**
     * Sets the size of clipEl so that is covering as much of the button as possible.
     * @private
     */
    setClipSize: function(){
        if (this.clipEl) {
            var width = this.buttonCt.getWidth();
            var height = this.buttonCt.getHeight();

            // The button container can have a width and height of zero when it's rendered in a hidden panel.
            // This is most noticable when using a card layout, as the items are all rendered but hidden,
            // (unless deferredRender is set to true). 
            // In this case, the clip size can't be determined, so we attempt to set it later.
            // This check repeats until the button container has a size. 
            if (width === 0 || (height === 0 && (!Ext.isIE8 && !Ext.isIE9 && !Ext.isIE10)) ) {  // ugly hack (Ext.isIE8) by 4Him
                this.setClipSize.defer(100, this);
            } else {
                // Loop by 4Him
                for(var browser in this.BROWSERS_OFFSETS.Ext2) {
                    if(Ext['is'+browser]) {
                        width = width + this.getBrowserOffset(browser, 'width');
                        height = height + this.getBrowserOffset(browser, 'height');
                        break;
                    }
                }

                this.clipEl.setSize(width, height);
            }
        }
    },
    
    /**
     * Creates the input file element and adds it to inputFileCt.
     * The created input file elementis sized, positioned, and styled appropriately.
     * Event handlers for the element are set up, and a tooltip is applied if defined in the original config.
     * @private
     */
    createInputFile: function(){
        // When an input file gets detached and set as the child of a different DOM element,
        // straggling <em> elements get left behind.  
        // I don't know why this happens but we delete any <em> elements we can find under the floatEl to prevent a memory leak.
        this.floatEl.select('em').each(function(el){
            el.remove();
        });
        this.inputFileEl = this.floatEl.createChild({
            tag: 'input',
            type: 'file',
            size: 1, // must be > 0. It's value doesn't really matter due to our masking div (inputFileCt).  
            name: this.inputFileName || Ext.id(this.el),
            tabindex: this.tabIndex,
            // Use the same pointer as an Ext.Button would use.  This doesn't work in Firefox.
            // This positioning right-aligns the input file to ensure that the "Browse" button is visible.
            style: {
                position: 'absolute',
                cursor: 'pointer',
                right: '0px',
                top: '0px'
            }
        });
        this.inputFileEl = this.inputFileEl.child('input') || this.inputFileEl;
        // IE8 needs opacity on the 'file input' element - @author 4Him
        if(Ext.isIE8 || Ext.isIE9) {
            this.inputFileEl.setOpacity(0.0);
        }
        
        // setup events
        this.inputFileEl.on({
            'click': this.onInputFileClick,
            'change': this.onInputFileChange,
            'focus': this.onInputFileFocus,
            'select': this.onInputFileFocus,
            'blur': this.onInputFileBlur,
            scope: this
        });
        
        // add a tooltip
        if (this.tooltip) {
            if (typeof this.tooltip === 'object') {
                Ext.QuickTips.register(Ext.apply({
                    target: this.inputFileEl
                }, this.tooltip));
            } else {
                this.inputFileEl.dom[this.tooltipType] = this.tooltip;
            }
        }
    },
    
    /**
     * Redirecting focus to the input file element so the user can press space and select files.
     * @param {Event} e focus event.
     * @private
     */
    onButtonFocus: function(e){
        if (this.inputFileEl) {
            this.inputFileEl.focus();
            e.stopEvent();
        }
    },
    
    /**
     * Handler for the IE case where once can tab to the text box of an input file el.
     * If the key is a space, we simply "click" the inputFileEl.
     * @param {Event} e key event.
     * @private
     */
    onButtonKeyDown: function(e){
        if (this.inputFileEl && e.getKey() === Ext.EventObject.SPACE) {
            this.inputFileEl.dom.click();
            e.stopEvent();
        }
    },
    
    /**
     * Handler when the cursor moves over the clipEl.
     * The floatEl gets centered to the cursor location.
     * @param {Event} e mouse event.
     * @private
     */
    onButtonMouseMove: function(e){
        var xy = e.getXY();
        xy[0] -= this.FLOAT_EL_WIDTH / 2;
        xy[1] -= this.FLOAT_EL_HEIGHT / 2;
        this.floatEl.setXY(xy);
    },
    
    /**
     * Add the visual enhancement to the button when the input file recieves focus. 
     * This is the tip for the user that now he/she can press space to select the file.
     * @private
     */
    onInputFileFocus: function(e){
        if (!this.isDisabled) {
            this.el.addClass("x-btn-over");
        }
    },
    
    /**
     * Removes the visual enhancement from the button.
     * @private
     */
    onInputFileBlur: function(e){
        this.el.removeClass("x-btn-over");
    },
    
    /**
     * Handler when inputFileEl's "Browse..." button is clicked.
     * @param {Event} e click event.
     * @private
     */
    onInputFileClick: function(e){
        e.stopPropagation();
    },
    
    /**
     * Handler when inputFileEl changes value (i.e. a new file is selected).
     * @private
     */
    onInputFileChange: function(){
        if (this.originalHandler) {
            this.originalHandler.call(this.originalScope, this);
        }
    },
    
    
    /*
     * Public methods:
     */
    /**
     * Detaches the input file associated with this BrowseButton so that it can be used for other purposed (e.g. uplaoding).
     * The returned input file has all listeners and tooltips applied to it by this class removed.
     * @param {Boolean} noCreate whether to create a new input file element for this BrowseButton after detaching.
     * True will prevent creation.  Defaults to false.
     * @return {Ext.Element} the detached input file element.
     */
    detachInputFile: function(noCreate){
        var result = this.inputFileEl;
        
        if (typeof this.tooltip === 'object') {
            Ext.QuickTips.unregister(this.inputFileEl);
        } else {
            this.inputFileEl.dom[this.tooltipType] = null;
        }
        this.inputFileEl.removeAllListeners();
        this.inputFileEl = null;
        
        if (!noCreate) {
            this.createInputFile();
        }
        return result;
    },
    
    /**
     * @return {Ext.Element} the input file element attached to this BrowseButton.
     */
    getInputFile: function(){
        return this.inputFileEl;
    },
    
    /**
     * @see Ext.Button.disable
     */
    disable: function(){
        Ext.ux.form.BrowseButton.superclass.disable.call(this);
        this.inputFileEl.dom.disabled = true;
    },
    
    /**
     * @see Ext.Button.enable
     */
    enable: function(){
        Ext.ux.form.BrowseButton.superclass.enable.call(this);
        this.inputFileEl.dom.disabled = false;
    }
});

/**
 * HACK:
 * Some layouts (e.g. license detail page) expect a BoxComponent (setters for size and position).
 * The documentation states that Ext.Button is a BoxComponent, but the source actually extends only
 * Ext.Component! Instead of "fixing" ext js (and potentially breaking a button somewhere in the Nexus UI)
 * we will add the BoxComponent methods only to the browse button.
 */
// FIXME this is fixed in ExtJS3 AFAIK, no need to enhance the prototype here
Ext.applyIf(Ext.ux.form.BrowseButton.prototype, Ext.BoxComponent.prototype);

Ext.reg('browsebutton', Ext.ux.form.BrowseButton);

});
