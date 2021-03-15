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
 * Proprietary Repositories settings form.
 *
 * @since 3.30
 */
Ext.define('NX.coreui.view.repository.ProprietaryRepositories', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-repository-proprietary-repositories',
  requires: [
    'NX.Conditions',
    'NX.ext.form.field.ItemSelector',
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.settingsForm = [
      // basic settings
      {
        xtype: 'nx-settingsform',
        settingsFormSuccessMessage: NX.I18n.get('ProprietaryRepositories_Update_Success'),
        api: {
          load: 'NX.direct.coreui_ProprietaryRepositories.read',
          submit: 'NX.direct.coreui_ProprietaryRepositories.update'
        },
        editableCondition: NX.Conditions.isPermitted('nexus:settings:update'),
        editableMarker: NX.I18n.get('ProprietaryRepositories_Update_Error'),

        items: [
          {
            xtype: 'nx-itemselector',
            name: 'enabledRepositories',
            fieldLabel: NX.I18n.get('ProprietaryRepositories_Field_Label'),
            buttons: ['addAll', 'add', 'remove', 'removeAll'],
            buttonsText: {
              add: 'Add to Selected',
              remove: 'Remove from Selected',
              addAll: 'Add All',
              removeAll: 'Remove All'
            },
            fromTitle: NX.I18n.get('ProprietaryRepositories_Available_FromTitle'),
            toTitle: NX.I18n.get('ProprietaryRepositories_Available_ToTitle'),
            store: 'ProprietaryRepositories',
            valueField: 'id',
            displayField: 'name',
            delimiter: null,
            allowBlank: true
          }
        ]
      }
    ];

    me.callParent();
  }
});
