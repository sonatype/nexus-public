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
 * Support for progress screens.
 *
 * @since 3.0
 * @abstract
 */
Ext.define('NX.coreui.migration.ProgressScreenSupport', {
  extend: 'NX.wizard.GridScreen',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.fields = [{
      xtype: 'progressbar'
    }];

    me.grid = {
      xtype: 'grid',

      viewConfig: {
        stripeRows: true,

        // disable load-mask this is too distracting when auto-refreshing via task
        loadMask: false
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
              case 'COMPLETED':
                return 'migration-step-done';

              case 'INITIALIZED':
                return 'migration-step-pending';

              case 'RUNNING':
                return 'migration-step-running';

              default:
                return 'migration-step-error';
            }
          }
        },
        {
          header: NX.I18n.render(me, 'Name_Column'),
          dataIndex: 'name',
          flex: 1
        },
        {
          header: NX.I18n.render(me, 'Status_Column'),
          dataIndex: 'status',
          flex: 2,
          renderer: function (value) {
            if (value === null) {
              return 'Pending';
            }
            return value;
          }
        },
        {
          header: NX.I18n.render(me, 'State_Column'),
          dataIndex: 'state'
        },
        {
          header: NX.I18n.render(me, 'Complete_Column'),
          dataIndex: 'complete',
          width: 80,
          renderer: function (value) {
            if (value >= 0) {
              return Math.floor(value * 100) + '%';
            }
            //else {
            //  return '&infin;';
            //}
          }
        }
      ],

      store: 'NX.coreui.migration.ProgressStore',
      invalidateScrollerOnRefresh: false
    };

    me.callParent();
  },

  /**
   * @returns {Ext.ProgressBar}
   */
  getProgressBar: function() {
    return this.down('progressbar');
  }
});
