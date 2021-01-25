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
 * Shows an icon display of features in the {@link NX.store.FeatureGroup} store.
 *
 * @since 3.0
 */
Ext.define('NX.view.feature.Group', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-feature-group',
  requires: [
    'NX.Icons'
  ],

  cls: [
    'nx-feature-group',
    'nx-inset'
  ],

  autoScroll: true,

  items: {
    xtype: 'container',
    frame: true,
    cls: 'nx-subsection',

    items: {
      xtype: 'dataview',

      store: 'FeatureGroup',
      tpl: [
        '<tpl for=".">',
        '<div class="item-wrap">',
        '<tpl if="iconCls">',
        '<i class="fa {[values.iconCls]} fa-2x fa-fw"></i>',
        '<span class="x-fa-icon-text">{text}</span>',
        '<tpl elseif="iconName">',
        '{[ NX.Icons.img(values.iconName, "x32") ]}',
        '<span>{text}</span>',
        '</tpl>',
        '</div>',
        '</tpl>'
      ],

      itemSelector: 'div.item-wrap',
      trackOver: true,
      overItemCls: 'x-item-over',
      selectedItemCls: 'x-item-selected'
    }
  }

});
