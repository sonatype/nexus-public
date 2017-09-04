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
  models: [
    'Node'
  ],
  stores: [
    'Node'
  ],
  views: [
    'system.Nodes',
    'system.NodeList',
    'system.NodesDisabledMessage',
    'system.NodeSettings',
    'system.NodeSettingsForm'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-system-nodes'},
    {ref: 'list', selector: 'nx-coreui-system-nodelist'},
    {ref: 'info', selector: 'nx-coreui-system-nodes nx-coreui-system-node-settings'},
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
        },
        'nx-coreui-system-node-settings-form': {
          submitted: me.loadStores
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function (model) {
    return model.get('friendlyName') || model.get('nodeIdentity');
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

    if (NX.State.getValue('db', {})['dbFrozen'] !== me.dbFrozen) {
      NX.State.setValue('db', { dbFrozen: me.dbFrozen });
    }
  },

  toggleFreeze: function() {
    var me = this, dialogTitle, dialogDescription, yesButtonText;

    var systemInitiated = NX.State.getValue('db', {})['system'];

    if (systemInitiated && me.dbFrozen) {
      dialogTitle = NX.I18n.get('Nodes_force_release_dialog');
      dialogDescription = NX.I18n.get('Nodes_force_release_warning')
          + ' ' + NX.I18n.get('Nodes_force_release_confirmation');
      yesButtonText = NX.I18n.get('Nodes_force_release');
    } else {
      dialogTitle = me.dbFrozen ? NX.I18n.get('Nodes_Disable_read_only_mode_dialog') :
          NX.I18n.get('Nodes_Enable_read_only_mode_dialog');
      dialogDescription = me.dbFrozen ? NX.I18n.get('Nodes_disable_read_only_mode_dialog_description') :
          NX.I18n.get('Nodes_enable_read_only_mode_dialog_description');
      yesButtonText = me.dbFrozen ? NX.I18n.get('Nodes_Disable_read_only_mode') :
          NX.I18n.get('Nodes_Enable_read_only_mode');
    }

    NX.Dialogs.askConfirmation(dialogTitle, dialogDescription, function() {
      var settings = {frozen: !me.dbFrozen};

      me.getContent().getEl().mask(NX.I18n.get('Nodes_Toggling_read_only_mode'));
      if (systemInitiated && me.dbFrozen) {
        NX.direct.coreui_DatabaseFreeze.forceRelease(function(response) {
          me.getContent().getEl().unmask();
          if (Ext.isObject(response) && response.success) {
            me.updateFreezeStatus(response.data.frozen);
          }
        });
      } else {
        NX.direct.coreui_DatabaseFreeze.update(settings, function(response) {
          me.getContent().getEl().unmask();
          if (Ext.isObject(response) && response.success) {
            me.updateFreezeStatus(response.data.frozen);
          }
        });
      }

    }, {
      scope: me,
      buttonText: {
        yes: yesButtonText,
        no: 'Cancel'
      }
    });
  },

  onSelection: function(list, model) {
    if (Ext.isDefined(model)) {
      this.getInfo().loadRecord(model);
    }
  }
});
