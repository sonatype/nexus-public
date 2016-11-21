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
 * Node list.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.system.NodeList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-system-nodelist',
  requires: [
    'NX.I18n'
  ],

  config: {
    stateful: true,
    stateId: 'nx-coreui-system-nodelist'
  },

  store: 'Node',

  columns: [
    {
      xtype: 'nx-iconcolumn',
      width: 36,
      iconVariant: 'x16',
      iconName: function () {
        return 'node-default';
      }
    },
    {
      header: 'UUID',
      dataIndex: 'uuid',
      stateId: 'uuid',
      flex: 1
    },
    {
      header: 'Local',
      dataIndex: 'local',
      stateId: 'local',
      width: 80
    },
    {
      header: 'Socket Address',
      dataIndex: 'socketAddress',
      stateId: 'socketAddress',
      flex: 1
    }
  ],

  // Add a white background behind the filter, to make it look like part of the header
  dockedItems: [
    {
      xtype: 'nx-actions',
      items: [
        {
          xtype: 'button',
          text: 'Enable read-only mode',
          glyph: 'xf1e5@FontAwesome' /* fa-binoculars */,
          action: 'freeze'
        }
      ]
    }
  ],

  plugins: [
    {ptype: 'gridfilterbox', emptyText: 'No nodes matched "$filter"'}
  ]
});
