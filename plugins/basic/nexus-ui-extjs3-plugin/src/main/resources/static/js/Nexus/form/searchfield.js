/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global define*/
define('Nexus/form/searchfield',['extjs', 'nexus'], function(Ext, Nexus) {
Ext.namespace('Nexus.form');

Nexus.form.SearchField = Ext.extend(Ext.form.TwinTriggerField, {
      initComponent : function() {
        Ext.app.SearchField.superclass.initComponent.call(this);
        this.on('specialkey', function(f, e) {
              if (e.getKey() === e.ENTER)
              {
                this.onTrigger2Click();
              }
            }, this);
        if (this.searchPanel)
        {
          this.searchPanel.searchField = this;
        }
      },

      validationEvent : false,
      validateOnBlur : false,
      trigger1Class : 'x-form-clear-trigger',
      trigger2Class : 'x-form-search-trigger',
      hideTrigger1 : true,
      width : 180,
      paramName : 'q',

      onTrigger1Click : function() {
        if (this.getRawValue())
        {
          this.el.dom.value = '';
          this.triggers[0].hide();
          this.hasSearch = false;
        }
        if (this.searchPanel.stopSearch)
        {
          this.searchPanel.stopSearch(this.searchPanel);
        }
      },

      onTrigger2Click : function() {
        var v = this.getRawValue();
        if (v.length < 1)
        {
          this.onTrigger1Click();
          return;
        }
        // var o = {start: 0};
        this.searchPanel.startSearch(this.searchPanel, true);
      },

      /**
       * Override TwinTriggerField#afterRender, because position calculation for IE was always off by 1 (or even many)
       * pixels by default.
       */
      afterRender : function(){
        Ext.form.TriggerField.superclass.afterRender.call(this);
        var y;
        if (Ext.isIE && !this.hideTrigger) {
          if (Ext.isIE8||Ext.isIE6) { // IE6 is also discovered for IE9
            this.el.position();
            this.el.setY(this.el.getY()+1);
          } else if(Ext.isIE7 && this.el.getY() !== (y = this.trigger.getY())){
            this.el.position();
            this.el.setY(y);
          }
        }
      }
    });

Ext.reg('nexussearchfield', Nexus.form.SearchField);

// FIXME: legacy
Ext.app.SearchField = Nexus.form.SearchField;


});

