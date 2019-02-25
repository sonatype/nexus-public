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
 * Migration agent connection step.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.AgentStep', {
  extend: 'NX.wizard.Step',
  requires: [
    'NX.coreui.migration.AgentScreen',
    'NX.I18n'
  ],

  config: {
    screen: 'NX.coreui.migration.AgentScreen',
    enabled: true
  },

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.control({
      'button[action=back]': {
        click: me.moveBack
      },
      'button[action=next]': {
        click: me.doNext
      },
      'button[action=cancel]': {
        click: me.cancel
      }
    });
  },

  /**
   * @override
   */
  reset: function() {
    var me = this,
        screen = me.getScreenCmp();

    if (screen) {
      screen.getForm().reset();
    }

    me.callParent();
  },

  /**
   * @private
   */
  doNext: function() {
    var me = this,
        input = me.getScreenCmp().getForm().getFieldValues();

    me.mask(NX.I18n.render(me, 'Connect_Mask'));

    NX.direct.migration_Assistant.connect(input.url, input.accessToken, input.fetchSize, input.useTrustStoreForUrl, function (response, event) {
      me.unmask();

      // FIXME: handle validation/errors

      if (event.status && response.success) {
        me.moveNext();

        NX.Messages.success(NX.I18n.render(me, 'Connect_Message'));
      }
    });
  }
});
