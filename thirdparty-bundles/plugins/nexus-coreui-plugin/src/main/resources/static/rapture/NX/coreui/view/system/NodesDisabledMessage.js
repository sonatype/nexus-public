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
/**
 * Nodes disabled.
 *
 * @since 3.2
 */
Ext.define('NX.coreui.view.system.NodesDisabledMessage', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-system-nodes-disabled',
  requires: [
    'NX.I18n'
  ],

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
  items: [
    {
      xtype: 'panel',
      ui: 'nx-inset',
      items: {
        xtype: 'panel',
        ui: 'nx-subsection-framed',
        items: {
          xtype: 'text',
          text: NX.I18n.get('Nodes_OSS_Message')
        }
      }
    }
  ]
});
