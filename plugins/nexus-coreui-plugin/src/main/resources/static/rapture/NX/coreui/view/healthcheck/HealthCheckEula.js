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
 * Health Check EULA window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.healthcheck.HealthCheckEula', {
  extend: 'Ext.window.Window',
  alias: 'widget.nx-coreui-healthcheck-eula',
  requires: [
    'NX.util.Url'
  ],

  title: 'CLM Terms of Use',

  layout: 'fit',
  autoShow: true,
  modal: true,
  constrain: true,
  width: 640,
  height: 500,

  acceptFn: undefined,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = {
      xtype: 'box',
      autoEl: {
        tag: 'iframe',
        src: NX.util.Url.urlOf('/static/healthcheck-tos.html')
      }
    };

    me.dockedItems = [
      {
        xtype: 'toolbar',
        dock: 'bottom',
        ui: 'footer',
        items: [
          { xtype: 'button', text: 'I Agree', action: 'agree', formBind: true, ui: 'nx-primary', handler: function () {
            var win = this.up('window');

            win.close();
            if (win.acceptFn) {
              win.acceptFn.call();
            }
          }},
          { xtype: 'button', text: 'I Do Not Agree', handler: function () {
            this.up('window').close();
          }},
          '->',
          { xtype: 'component', html: '<a href="' + NX.util.Url.urlOf('/static/healthcheck-tos.html') +
              '" target="_new">Download a copy of the license.</a>'
          }
        ]
      }
    ];

    me.callParent();
  }
});
