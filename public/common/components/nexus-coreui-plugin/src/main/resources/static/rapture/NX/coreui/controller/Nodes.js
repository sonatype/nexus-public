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
    {ref: 'content', selector: 'nx-feature-content'}
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
      return NX.Permissions.check('nexus:nodes:read') &&
        !NX.State.getValue('nexus.datastore.clustered.enabled');
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

  onSelection: function(list, model) {
    if (Ext.isDefined(model)) {
      this.getInfo().loadRecord(model);
    }
  }
});
