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
 * Ssl use Nexus Repository Truststore checkbox.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.ssl.SslUseTrustStore', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-sslusetruststore',
  requires: [
    'NX.I18n'
  ],

  initComponent: function () {
    var me = this;

    if (!me.fieldLabel && !me.boxLabel) {
      me.fieldLabel = NX.I18n.get('Ssl_SslUseTrustStore_BoxLabel');
    }
    if (me.fieldLabel === true) {
      me.fieldLabel = NX.I18n.get('Ssl_SslUseTrustStore_BoxLabel');
    }
    if (me.boxLabel === true) {
      me.boxLabel = NX.I18n.get('Ssl_SslUseTrustStore_BoxLabel');
    }

    me.items = {
      xtype: 'panel',
      layout: 'hbox',
      items: [
        {
          xtype: 'checkbox',
          name: me.name,
          value: me.value,
          inputValue: true,
          boxLabel: me.boxLabel,
          helpText: NX.I18n.get('Ssl_SslUseTrustStore_Certificate_HelpText')
        },
        {
          xtype: 'button',
          text: NX.I18n.get('Ssl_SslUseTrustStore_Certificate_Button'),
          ui: 'nx-plain',
          action: 'showcertificate',
          iconCls: 'x-fa fa-certificate',
          margin: '0 0 0 5'
        }
      ]
    };

    me.callParent();
  }
});
