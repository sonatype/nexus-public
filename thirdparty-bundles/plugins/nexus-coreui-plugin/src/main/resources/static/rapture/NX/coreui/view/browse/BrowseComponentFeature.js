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
 * Browse feature.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.browse.BrowseComponentFeature', {
  extend: 'NX.view.drilldown.Drilldown',
  alias: 'widget.nx-coreui-browsecomponentfeature',

  iconName: 'browse-component-default',

  masters: [
    {xtype: 'nx-coreui-browse-repository-list'},
    {xtype: 'nx-coreui-browse-component-list'},
    {
      xtype: 'panel',
      region: 'center',
      layout: {
        type: 'vbox',
        align: 'stretch',
        pack: 'start'
      },
      items: [
        {
          xtype: 'nx-coreui-component-details'
        },
        {
          xtype: 'nx-coreui-component-asset-list',
          flex: 1
        }
      ]
    }
  ],

  detail: {
    xtype: 'panel',

    layout: {
      type: 'vbox',
      align: 'stretch',
      pack: 'start'
    },

    items: {
      xtype: 'nx-coreui-component-assetcontainer',
      header: false,
      flex: 1
    }
  }

});
