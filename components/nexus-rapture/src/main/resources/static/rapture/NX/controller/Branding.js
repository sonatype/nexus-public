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
/*global Ext, NX*/

/**
 * Branding controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Branding', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.State'
  ],

  views: [
    'header.Branding'
  ],

  refs: [
    { ref: 'viewport', selector: 'viewport' },
    { ref: 'headerBranding', selector: 'nx-header-branding' },
    { ref: 'footerBranding', selector: 'nx-footer-branding' }
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.listen({
      controller: {
        '#State': {
          brandingchanged: me.onBrandingChanged
        }
      },
      component: {
        'nx-header-branding': {
          afterrender: me.renderHeaderBranding
        },
        'nx-footer-branding': {
          afterrender: me.renderFooterBranding
        }
      }
    });
  },

  /**
   * Render header/footer branding when branding configuration changes.
   *
   * @private
   */
  onBrandingChanged: function() {
    this.renderHeaderBranding();
    this.renderFooterBranding();
  },

  /**
   * Render header branding.
   *
   * @private
   */
  renderHeaderBranding: function() {
    var branding = NX.State.getValue('branding'),
        headerBranding = this.getHeaderBranding();

    if (headerBranding) {
      if (branding && branding['headerEnabled']) {
        headerBranding.update(branding['headerHtml']);
        headerBranding.show();
      }
      else {
        headerBranding.hide();
      }
    }
  },

  /**
   * Render footer branding.
   *
   * @private
   */
  renderFooterBranding: function() {
    var branding = NX.State.getValue('branding'),
        footerBranding = this.getFooterBranding();

    if (footerBranding) {
      if (branding && branding['footerEnabled']) {
        footerBranding.update(branding['footerHtml']);
        footerBranding.show();
      }
      else {
        footerBranding.hide();
      }
    }
  }

});
