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
 * Component details panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.component.ComponentDetails', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-component-details',
  requires: [
    'NX.Icons'
  ],
  ui: 'nx-inset',

  /**
   * Currently shown component model.
   */
  componentModel: undefined,

  layout: {
    type: 'hbox',
    align: 'stretch'
  },

  style: {
    'background-color': '#FFFFFF'
  },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {xtype: 'nx-info', itemId: 'repositoryInfo'},
      {xtype: 'nx-info', itemId: 'componentInfo'},
      {xtype: 'nx-info', itemId: 'extraInfo'}
    ];

    me.callParent();
  },

  /**
   * @public
   *
   * Sets component.
   * @param {NX.coreui.model.Component} componentModel component
   */
  setComponentModel: function(componentModel) {
    var me = this;

    me.componentModel = componentModel;
    me.fireEvent('updated', me, me.componentModel);
  }

});
