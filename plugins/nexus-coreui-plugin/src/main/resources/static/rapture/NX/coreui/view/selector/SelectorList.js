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
 * Selector grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.selector.SelectorList', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-selector-list',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],

  stateful: true,
  stateId: 'nx-coreui-selector-list',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.store = 'Selector';

    me.columns = [
      {
        xtype: 'nx-iconcolumn',
        width: 36,
        iconVariant: 'x16',
        iconName: function() {
          return 'selector-default';
        }
      },
      {
        header: NX.I18n.get('Selector_SelectorList_Name_Header'),
        dataIndex: 'name',
        stateId: 'name',
        flex: 1
      },
      {
        header: NX.I18n.get('Selector_SelectorList_Description_Header'),
        dataIndex: 'description',
        stateId: 'description',
        flex: 1
      }
    ];

    me.viewConfig = {
      emptyText: NX.I18n.get('Selector_SelectorList_EmptyText'),
      deferEmptyText: false
    };

    me.plugins = [
      {
        ptype: 'gridfilterbox',
        emptyText: NX.I18n.get('Selector_SelectorList_Filter_EmptyText')
      }
    ];

    me.dockedItems = [
      {
        xtype: 'nx-actions',
        items: [
          {
            xtype: 'button',
            text: NX.I18n.get('Selector_SelectorList_New_Button'),
            glyph: 'xf055@FontAwesome' /* fa-plus-circle */,
            action: 'new',
            disabled: true
          }
        ]
      }
    ];

    me.callParent();
  }
});
