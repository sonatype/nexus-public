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
 * Migration preview screen.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.PreviewScreen', {
  extend: 'NX.wizard.GridScreen',
  alias: 'widget.nx-coreui-migration-preview',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: 'Preview',

      description: '<p>Here is a preview of the migration configuration.</p>',

      grid: {
        xtype: 'grid',

        viewConfig: {
          stripeRows: true
        },

        hideHeaders: true,

        columns: [
          {
            xtype: 'nx-iconcolumn',
            width: 36,
            iconVariant: 'x16',
            iconName: function (value, meta, record) {
              var state = record.get('state');
              switch (state) {
                case 'INITIALIZED':
                  return 'migration-step-pending';

                default:
                  return 'migration-step-error';
              }
            }
          },
          {
            header: 'Name',
            dataIndex: 'name',
            flex: 1
          },
          {
            header: 'State',
            dataIndex: 'state'
          }
        ],

        store: 'NX.coreui.migration.PreviewStore',

        features: [
          {
            ftype: 'grouping',
            collapsible: false,
            enableGroupingMenu: false
          }
        ]
      },

      buttons: [
        'back',
        {
          text: 'Begin',
          action: 'begin',
          ui: 'nx-primary',
          disabled: true
        },
        'cancel'
      ]
    });

    me.callParent();
  }
});
