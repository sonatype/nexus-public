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
/*global Ext, NX*/

/**
 * Helpers to interact with Message controller.
 *
 * @since 3.0
 */
Ext.define('NX.Messages', {
  singleton: true,

  requires: [
    'NX.State'
  ],

  info: function(message) {
    this.toast(message, 'info', 'fa-info');
  },

  success: function(message) {
    this.toast(message, 'success', 'fa-check-circle');
  },

  warning: function(message) {
    this.toast(message, 'warning', 'fa-exclamation-circle');
  },

  error: function(message) {
    this.toast(message, 'error', 'fa-exclamation-triangle');
  },

  /** @private */
  toast: function(message, type, iconCls) {
    // When the React shell is active, delegate to the React toast bridge so the
    // message is visible. The React shell suppresses .x-toast elements via CSS.
    if (typeof window !== 'undefined' && window.__nexusToast && typeof window.__nexusToast[type] === 'function') {
      window.__nexusToast[type](message);
      return;
    }
    const isFirst = !document.querySelector('.x-toast');
    const extraStyle = isFirst ? 'position: absolute; transform: translate(-24px, 70px);' : 'transform: translateY(70px)';
    setTimeout(function delay() {
      Ext.toast({
        baseCls: type,
        html:
            '<div role="presentation" class="icon x-fa ' + iconCls + '"></div>' +
            '<div class="text">' + Ext.htmlEncode(message) + '</div>' +
            '<div class="dismiss"><a aria-label="Dismiss" href="javascript:;" onclick="Ext.getCmp(this.closest(\'.x-toast\').id).close()"><i class="fa fa-times-circle"></i></a></div>',
        align: 'tr',
        style: extraStyle,
        stickOnClick: true,
        minWidth: 150,
        maxWidth: 400,
        autoCloseDelay: NX.State.getValue('messageDuration', 5000),
        slideInDuration: NX.State.getValue('animateDuration', 800),
        slideBackDuration: NX.State.getValue('animateDuration', 500),
        hideDuration: NX.State.getValue('animateDuration', 500),
        slideInAnimation: 'elasticIn',
        slideBackAnimation: 'elasticIn',
        ariaRole: 'alert'
      });
    }, 100);
  }
});
