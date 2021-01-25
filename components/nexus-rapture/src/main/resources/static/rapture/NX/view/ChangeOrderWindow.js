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
/*global Ext*/

/**
 * Abstract change order window.
 *
 * @since 3.0
 */
Ext.define('NX.view.ChangeOrderWindow', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-changeorderwindow',
  requires: [
    'NX.ext.form.field.ItemOrderer',
    'NX.I18n'
  ],
  ui: 'nx-inset',

  displayField: 'name',
  valueField: 'id',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.setWidth(NX.view.ModalDialog.MEDIUM_MODAL);

    me.items = {
      xtype: 'form',
      items: {
        xtype: 'nx-itemorderer',
        store: me.store,
        displayField: me.displayField,
        valueField: me.valueField,
        delimiter: null,
        height: 400,
        width: 400
      },
      buttonAlign: 'left',
      buttons: [
        { text: NX.I18n.get('ChangeOrderWindow_Submit_Button'), action: 'save', formBind: true, ui: 'nx-primary' },
        { text: NX.I18n.get('ChangeOrderWindow_Cancel_Button'), handler: function () {
          this.up('window').close();
        }}
      ]
    };

    me.callParent();
  }

});
