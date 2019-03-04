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
 * File created window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.support.FileCreated', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-support-filecreated',

  // FIXME: remove use of nx-inset
  ui: 'nx-inset',

  // FIXME: convert to config{} block
  /**
   * @cfg Icon to show (img)
   */
  fileIcon: undefined,

  /**
   * @cfg Type of file shown
   */
  fileType: undefined,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.setWidth(NX.view.ModalDialog.LARGE_MODAL);

    Ext.apply(me, {
      title: me.title || me.fileType + ' Created',
      items: [
        {
          xtype: 'nx-coreui-support-filecreatedform',
          fileIcon: me.fileIcon,
          fileType: me.fileType
        }
      ]
    });

    me.callParent();
  },

  /**
   * Set form values.
   *
   * @public
   */
  setValues: function (values) {
    this.down('nx-coreui-support-filecreatedform').getForm().setValues(values);
  },

  /**
   * Get form values.
   *
   * @public
   */
  getValues: function () {
    return this.down('nx-coreui-support-filecreatedform').getForm().getValues();
  }
});
