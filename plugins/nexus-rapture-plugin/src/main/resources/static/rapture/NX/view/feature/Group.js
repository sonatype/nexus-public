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
/*global Ext*/

/**
 * Shows an icon display of features in the {@link NX.store.FeatureGroup} store.
 *
 * @since 3.0
 */
Ext.define('NX.view.feature.Group', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-feature-group',
  cls: 'nx-feature-group',
  layout: 'fit',

  autoScroll: true,

  items: {
    xtype: 'panel',
    ui: 'nx-inset',

    items: {
      xtype: 'panel',
      ui: 'nx-subsection',
      frame: true,

      items: {
        xtype: 'dataview',

        store: 'FeatureGroup',
        tpl: [
          '<tpl for=".">',
          '<div class="item-wrap">',
          '{[ NX.Icons.img(values.iconName, "x32") ]}',
          '<span>{text}</span>',
          '</div>',
          '</tpl>'
        ],

        itemSelector: 'div.item-wrap',
        trackOver: true,
        overItemCls: 'x-item-over',
        selectedItemCls: 'x-item-selected'
      }
    }
  }

});
