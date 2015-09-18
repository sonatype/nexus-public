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
 * Add Ssl Certificate from Server window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ssl.SslCertificateAddFromServer', {
  extend: 'NX.view.AddPanel',
  alias: 'widget.nx-coreui-sslcertificate-add-from-server',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],

  defaultFocus: 'server',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.settingsForm = {
      xtype: 'nx-settingsform',
      settingsFormSubmitOnEnter: true,

      items: [
        {
          xtype: 'textfield',
          fieldLabel: NX.I18n.get('Ssl_SslCertificateAddFromServer_Load_FieldLabel'),
          name: 'server',
          itemId: 'server'
        }
      ],

      buttons: [
        { text: NX.I18n.get('Ssl_SslCertificateList_Load_Button'), action: 'load', formBind: true, bindToEnter: true, ui: 'nx-primary' },
        { text: NX.I18n.get('Ssl_SslCertificateAddFromServer_Cancel_Button'), action: 'back' }
      ]
    };

    me.callParent();
  }
});
