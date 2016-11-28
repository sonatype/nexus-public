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
 * Nodes controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Nodes', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.view.info.Panel',
    'NX.view.info.Entry',
    'NX.util.Url',
    'NX.Permissions',
    'NX.I18n'
  ],
  masters: [
    'nx-coreui-system-nodelist',
    'nx-coreui-system-nodes-disabled'
  ],
  stores: [
    'Node'
  ],
  views: [
    'system.Nodes',
    'system.NodeList',
    'system.NodesDisabledMessage'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-system-nodes'},
    {ref: 'list', selector: 'nx-coreui-system-nodelist'},
    {ref: 'info', selector: 'nx-coreui-system-nodes nx-info-panel'},
    {ref: 'content', selector: 'nx-feature-content'},
    {ref: 'toggleFreezeButton', selector: 'button[action=freeze]'}
  ],

  features: {
    mode: 'admin',
    path: '/System/Nodes',
    text: 'Nodes',
    description: 'View nodes',
    view: 'NX.coreui.view.system.Nodes',
    iconConfig: {
      file: 'servers_network.png',
      variants: ['x16', 'x32']
    },
    visible: function () {
      return NX.Permissions.check('nexus:nodes:read');
    }
  },

  icons: {
    'node-default': {
      file: 'server.png',
      variants: ['x16', 'x32']
    }
  },

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        }
      },
      store: {
        '#Node': {
          load: me.reselect
        }
      },
      component: {
        'button[action=freeze]': {
          click: me.toggleFreeze,
          beforerender: me.load
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function (model) {
    return model.get('uuid');
  },

  dbFrozen: false,

  load: function() {
    var me = this;
    me.updateFreezeStatus(NX.State.getValue('db', {})['dbFrozen']);
  },

  updateFreezeStatus: function(status) {
    var me = this;
    me.dbFrozen = status;

    me.getToggleFreezeButton().setText(
        me.dbFrozen ? NX.I18n.get('Nodes_Disable_read_only_mode') : NX.I18n.get('Nodes_Enable_read_only_mode'));

    NX.State.setValue('db', { dbFrozen: me.dbFrozen });
  },

  toggleFreeze: function() {
    var me = this;

    var dialogTitle = me.dbFrozen ? NX.I18n.get('Nodes_Disable_read_only_mode_dialog') :
        NX.I18n.get('Nodes_Enable_read_only_mode_dialog');
    var dialogDescription = me.dbFrozen ? NX.I18n.get('Nodes_disable_read_only_mode_dialog_description') :
        NX.I18n.get('Nodes_enable_read_only_mode_dialog_description');

    NX.Dialogs.askConfirmation(dialogTitle, dialogDescription, function() {
      var settings = {frozen: !me.dbFrozen};

      me.getContent().getEl().mask(NX.I18n.get('Nodes_Toggling_read_only_mode'));
      NX.direct.coreui_DatabaseFreeze.update(settings, function(response) {
        me.getContent().getEl().unmask();
        if (Ext.isObject(response) && response.success) {
          me.updateFreezeStatus(response.data.frozen);
        }
      });
    }, {scope: me});

  },

  onSelection: function (list, model) {
    var me = this,
        info,
        attributes;

    if (Ext.isDefined(model)) {
      info = {};
      info['UUID'] = model.get('uuid');
      info['Local'] = model.get('local');
      info['Socket Address'] = model.get('socketAddress');

      attributes = model.get('attributes');
      if (attributes) {
        Ext.iterate(attributes, function (key, value) {
          info[key] = value;
        });
      }

      me.getInfo().showInfo(info);
    }
  }
});
