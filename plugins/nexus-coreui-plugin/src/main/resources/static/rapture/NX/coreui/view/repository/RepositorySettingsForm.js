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
 * Repository "Settings" form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.repository.RepositorySettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-repository-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n',
    'NX.util.Validator'
  ],

  api: {
    submit: 'NX.direct.coreui_Repository.update'
  },

  initComponent: function() {
    var me = this,
        permittedCondition;

    me.addListener('remotevalidation', me.handleRemoteValidationError);

    me.settingsFormSuccessMessage = function(data) {
      return NX.I18n.get('Repository_RepositorySettingsForm_Update_Success') + data['name'];
    };

    me.editableMarker = NX.I18n.get('Repository_RepositorySettingsForm_Update_Error');

    if (!me.editableCondition) {
      me.editableCondition = NX.Conditions.and(
          permittedCondition = NX.Conditions.isPermitted('nexus:repository-admin:*:*:edit'),
          NX.Conditions.formHasRecord('nx-coreui-repository-settings-form', function(model) {
            permittedCondition.setPermission(
                'nexus:repository-admin:' + model.get('format') + ':' + model.get('name') + ':edit'
            );
            return true;
          })
      );
    }

    me.items = me.items || [];
    Ext.Array.insert(me.items, 0, [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section nx-no-title',

        items: [
          {
            xtype: 'textfield',
            cls: 'nx-no-border',
            name: 'name',
            itemId: 'name',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('Repository_RepositorySettingsForm_Name_FieldLabel'),
            helpText: NX.I18n.get('Repository_RepositorySettingsForm_Name_HelpText'),
            readOnly: true,
            allowBlank: false,
            vtype: 'nx-name'
          },
          {
            xtype: 'textfield',
            cls: 'nx-no-border',
            name: 'format',
            itemId: 'format',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('Repository_RepositorySettingsForm_Format_FieldLabel'),
            helpText: NX.I18n.get('Repository_RepositorySettingsForm_Format_HelpText'),
            readOnly: true
          },
          {
            xtype: 'textfield',
            cls: 'nx-no-border',
            name: 'type',
            itemId: 'type',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('Repository_RepositorySettingsForm_Type_FieldLabel'),
            helpText: NX.I18n.get('Repository_RepositorySettingsForm_Type_HelpText'),
            readOnly: true
          },

          {
            xtype: 'textfield',
            cls: 'nx-no-border',
            name: 'url',
            itemId: 'url',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('Repository_RepositorySettingsForm_URL_FieldLabel'),
            helpText: NX.I18n.get('Repository_RepositorySettingsForm_URL_HelpText'),
            readOnly: true
          },
          {
            xtype: 'checkbox',
            name: 'online',
            itemId: 'online',
            labelAlign: 'left',
            fieldLabel: NX.I18n.get('Repository_RepositorySettingsForm_Online_FieldLabel'),
            helpText: NX.I18n.get('Repository_RepositorySettingsForm_Online_HelpText'),
            value: true
          }
        ]
      }
    ]);

    me.callParent();

    //map repository attributes raw map structure to/from a flattened representation
    Ext.override(me.getForm(), {
      getValues: function() {
        return me.doGetValues(this.callParent(arguments));
        },
      setValues: function(values) { 
        me.doSetValues(values);
        this.callParent(arguments);
      }
    });
  },

  doGetValues: function(values) {
    var processed = { attributes: {} };

    Ext.Object.each(values, function(key, value) {
      var segments = key.split('.'),
          parent = processed;

      Ext.each(segments, function(segment, pos) {
        if (pos === segments.length - 1) {
          parent[segment] = value;
        }
        else {
          if (!parent[segment]) {
            parent[segment] = {};
          }
          parent = parent[segment];
        }
      });
    });

    return processed;
  },

  doSetValues: function(values) {
    var process = function(child, prefix) {
      Ext.Object.each(child, function(key, value) {
        var newPrefix = (prefix ? prefix + '.' : '') + key;
        if (Ext.isObject(value)) {
          process(value, newPrefix);
        }
        else {
          values[newPrefix] = value;
        }
      });
    };

    process(values);
  },

  handleRemoteValidationError: function(validationMap) {
    Object.keys(validationMap).forEach(function (key) {
      const errorMsg = validationMap[key];

      if (!key.startsWith("attributes.")) {
        key = "attributes." + key;
      }

      Ext.ComponentQuery.query('form[settingsForm=true] [name=' + key + ']')
        .forEach(function(component) {
          component.markInvalid(errorMsg);
        });
    });
  }
});
