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
 * License Agreement window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.licensing.LicenseAgreement', {
  extend: 'Ext.window.Window',
  alias: 'widget.nx-coreui-licensing-agreement',
  requires: [
    'NX.util.Url',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.title = NX.I18n.get('Licensing_LicenseAgreement_Title');

    me.layout = 'fit';
    me.autoShow = true;
    me.modal = true;
    me.constrain = true;
    me.width = 640;
    me.height = 500;

    me.items = {
      xtype: 'box',
      autoEl: {
        tag: 'iframe',
        src: NX.util.Url.urlOf('/LICENSE.html')
      }
    };

    me.dockedItems = [
      {
        xtype: 'toolbar',
        dock: 'bottom',
        ui: 'footer',
        items: [
          { xtype: 'button', text: NX.I18n.get('Licensing_LicenseAgreement_Yes_Button'), action: 'agree', formBind: true, ui: 'nx-primary' },
          { xtype: 'button', text: NX.I18n.get('Licensing_LicenseAgreement_No_Button'), handler: function () {
            this.up('window').close();
          }},
          '->',
          { xtype: 'component', html: '<a href="' + NX.util.Url.urlOf('/LICENSE.html') +
              '" target="_new">' + NX.I18n.get('Licensing_LicenseAgreement_Download_Button') + '</a>'
          }
        ]
      }
    ];

    me.callParent();
  }

});
