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
 * Migration plan-step detail window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.PlanStepDetailWindow', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-migration-progressdetail',
  requires: [
    'Ext.XTemplate',
    'NX.I18n',
    'NX.util.DateFormat'
  ],

  config: {
    /**
     * @cfg {Object} Plan-step detail object.
     */
    detail: undefined
  },

  resizable: true,
  closable: true,
  layout: 'fit',
  height: 480,

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        store,
        timestampFmt = NX.util.DateFormat.forName('datetime')['short'];

    store = Ext.create('Ext.data.ArrayStore', {
      fields: [
        {name: 'timestamp', type: 'date', dateFormat: 'c'},
        {name: 'message', type: 'string'}
      ]
    });
    store.add(me.getDetail().entries);

    Ext.apply(me, {
      title: NX.I18n.render(me, 'Title', me.getDetail().name),
      width: NX.view.ModalDialog.LARGE_MODAL,
      items: {
        xtype: 'grid',
        viewConfig: {
          stripeRows: true,
          enableTextSelection: true,
          emptyText: NX.I18n.render(me, 'EmptyLog')
        },
        hideHeaders: true,
        columns: [
          {
            header: NX.I18n.render('Timestamp_Column'),
            dataIndex: 'timestamp',
            width: 150,
            renderer: function (value) {
              return Ext.util.Format.date(value, timestampFmt);
            }
          },
          {
            header: NX.I18n.render('Message_Column'),
            dataIndex: 'message',
            flex: 1
          }
        ],
        store: store,
        plugins: [
          {
            ptype: 'rowexpander',
            rowBodyTpl: Ext.create('Ext.XTemplate',
                '<div class="nx-rowexpander">',
                '<span class="x-selectable">{message}</span>',
                '</div>',
                {
                  compiled: true
                })
          }
        ]
      },

      buttonAlign: 'left',
      buttons: [
        {text: NX.I18n.get('Button_Close'), handler: me.close, scope: me}
      ]
    });

    me.callParent();
    me.center();
  }

});
