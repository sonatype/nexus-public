/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global define*/
define('ext/form/field',['extjs','sonatype'], function(Ext, Sonatype) {
Ext.override(Ext.form.Field, {
  /*
   * Override default form field rendering to include help text quick tip on
   * question mark rendered after field label.
   * Specifying 'helpText' will cause a help icon and text to be added as a QuickTip.
   * Specifying 'persistent' in conjunction with 'helpTest' will create a ToolTip that remains visible, useful for embedding links in the text.
   */
  afterRenderOrig : Ext.form.Field.prototype.afterRender,
  afterRender : function() {
    var helpClass = null, wrapDiv = null, helpMark = null, self = this;
    if ( this.helpMarker === true )
    {
      wrapDiv = this.getEl().up('div');
      helpClass = 'form-label-helpmark';
    }
    else if (this.getXType() === 'combo' || this.getXType() === 'uxgroupcombo' || this.getXType() === 'datefield' || this.getXType() === 'timefield')
    {
      wrapDiv = this.getEl().up('div.x-form-field-wrap');
      helpClass = 'form-label-helpmark-combo';
    }
    else if (this.getXType() === 'checkbox')
    {
      wrapDiv = this.getEl().up('div.x-form-check-wrap');
      helpClass = 'form-label-helpmark-check';
    }
    else if (this.getXType() === 'textarea')
    {
      wrapDiv = this.getEl().up('div.x-form-element');
      helpClass = 'form-label-helpmark-textarea';
    }
    else if ( this.getXType() === 'textentrylist' )
    {
      wrapDiv = this.getEl().up();
      helpClass = 'form-label-helpmark';
    }
    else
    {
      wrapDiv = this.getEl().up('div.x-form-element');
      helpClass = 'form-label-helpmark';
    }

    // @todo: afterText doesn't work with combo boxes!
    if (this.afterText)
    {
      if ( !wrapDiv ) {
        wrapDiv = this.getEl().wrap({cls:'x-form-element'});
      }

      wrapDiv.createChild({
        tag : 'span',
        cls : 'form-label-after-field',
        html : this.afterText
      });
    }

    if (this.helpText)
    {
      if ( !wrapDiv ) {
        wrapDiv = this.getEl().wrap({cls:'x-form-element'});
      }

      helpMark = wrapDiv.createChild({
        tag : 'img',
        src : Sonatype.config.resourcePath + '/static/icons/glyph_help.png',
        width : 16,
        height : 16,
        cls : helpClass
      });

      // Use a ToolTip instead of QuickTip if we want the tip to remain in place.
      // In this case the entire html fo the ToolTip will be set to the provided
      // 'helpText'.
      if(this.persistent)
      {
        var help = new Ext.ToolTip({
          html :  self.helpText,
          anchor: 'left',
          hideDelay: 10000
        });
        help.initTarget(helpMark);
      }
      else
      {
        Ext.QuickTips.register({
          target : helpMark,
          title : '',
          text : this.helpText,
          enabled : true
        });
      }
    }

    // original method
    this.afterRenderOrig.apply(this, arguments);
  }

});
});
