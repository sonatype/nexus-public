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
 * Abstract add window.
 *
 * @since 3.0
 */
Ext.define('NX.view.AddPanel', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-addpanel',
  requires: [
    'NX.I18n'
  ],

  layout: {
    type: 'vbox',
    align: 'stretch'
  },

  dockedItems: [{
    xtype: 'toolbar',
    dock: 'top',
    cls: 'nx-actions'
  }],

  autoScroll: true,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    // Create default buttons if they do not exist
    if (Ext.isDefined(me.settingsForm) && !Ext.isArray(me.settingsForm)) {
      if (!me.settingsForm.buttons) {
        me.settingsForm.buttons = [
          { text: NX.I18n.get('Add_Submit_Button'), action: 'add', formBind: true, ui: 'nx-primary', bindToEnter:  me.items.settingsFormSubmitOnEnter },
          { text: NX.I18n.get('Add_Cancel_Button'), handler: function () {
            this.up('nx-drilldown').showChild(0, true);
          }}
        ];
      }
    }

    // Add settings form to the panel
    me.items = {
      xtype: 'panel',
      ui: 'nx-inset',

      items: me.settingsForm
    };

    me.callParent(arguments);
  }

});
