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
 * Migration content-options screen.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.ContentScreen', {
  extend: 'NX.wizard.FormScreen',
  alias: 'widget.nx-coreui-migration-content',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: NX.I18n.render(me, 'Title'),

      description: NX.I18n.render(me, 'Description'),

      fields: [
        {
          xtype: 'checkboxgroup',
          columns: 1,
          allowBlank: false,
          items: [
            {
              xtype: 'checkbox',
              name: 'repositories',
              boxLabel: NX.I18n.render(me, 'Repositories_FieldLabel'),
              checked: true
            },
            {
              xtype: 'checkbox',
              name: 'configuration',
              boxLabel: NX.I18n.render(me, 'Configuration_FieldLabel'),
              checked: true
            }
          ]
        }
      ],

      buttons: ['back', 'next', 'cancel']
    });

    me.callParent();
    me.down('form').settingsForm = true;
  },

  /**
   * Returns the state of the screen form
   *
   * @return {boolean}
   */
  isDirty: function() {
    return this.down('form').isDirty();
  }
});
