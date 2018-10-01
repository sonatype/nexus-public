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
 * Cleanup Policy "Settings" form.
 *
 * @since 3.14
 */
var criteriaConfiguration = null;

Ext.define('NX.coreui.view.cleanuppolicy.CleanupPolicySettingsForm', {
  extend: 'NX.view.SettingsForm',
  alias: 'widget.nx-coreui-cleanuppolicy-settings-form',
  requires: [
    'NX.Conditions',
    'NX.I18n'
  ],

  api: {
    submit: 'NX.direct.cleanup_CleanupPolicy.update'
  },

  initComponent: function() {
    var me = this;

    me.settingsFormSuccessMessage = me.settingsFormSuccessMessage || function(data) {
      return NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_Update_Success') + Ext.htmlEncode(data['name']);
    };

    me.editableMarker = NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_Update_Error');

    me.editableCondition = me.editableCondition ||
        NX.Conditions.and(NX.Conditions.isPermitted('nexus:repository-admin:*:*:edit'));

    me.items = [
      {
        xtype: 'fieldset',
        itemId: 'cleanupPolicies',
        cls: 'nx-form-section',
        title: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_CleanupPolicy_Title'),
        items: [
          {
            xtype: 'textfield',
            name: 'name',
            itemId: 'name',
            fieldLabel: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_Name_FieldLabel'),
            helpText: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_Name_HelpText'),
            readOnly: true,
            allowBlank: false,
            maxLength: 255,
            vtype: 'nx-name'
          },
          {
            xtype: 'combo',
            name: 'format',
            itemId: 'format',
            fieldLabel: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_Format_FieldLabel'),
            helpText: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_Format_HelpText'),
            editable: false,
            allowBlank: false,
            store: 'RepositoryFormat',
            queryMode: 'local',
            displayField: 'value',
            valueField: 'value',
            readOnlyOnUpdate: true,
            listeners: {
              'change': {
                fn: me.onFormatChange,
                scope: me
              }
            }
          },
          {
            xtype: 'textfield',
            name: 'notes',
            itemId: 'notes',
            fieldLabel: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_Notes_FieldLabel'),
            allowBlank: true
          },
          {
            xtype: 'hiddenfield',
            name: 'mode',
            value: 'delete' // default
          }
        ]
      },
      {
        xtype: 'fieldset',
        itemId: 'criteria',
        cls: 'nx-form-section',
        title: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_Criteria_Title'),
        items: [
          {
            xtype: 'fieldcontainer',
            itemId: 'lastBlobUpdatedContainer',
            fieldLabel: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_LastBlobUpdated_FieldLabel'),
            helpText: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_LastBlobUpdated_HelpText'),
            layout: 'hbox',
            items: [
              me.createCheckbox('lastBlobUpdated'),
              me.createNumberField('lastBlobUpdated')
            ]
          },
          {
            xtype: 'fieldcontainer',
            itemId: 'lastDownloadedContainer',
            fieldLabel: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_LastDownloaded_FieldLabel'),
            helpText: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_LastDownloaded_HelpText'),
            layout: 'hbox',
            items: [
              me.createCheckbox('lastDownloaded'),
              me.createNumberField('lastDownloaded')
            ]
          },
          {
            xtype: 'fieldcontainer',
            itemId: 'isPrereleaseContainer',
            fieldLabel: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_IsPrerelease_FieldLabel'),
            helpText: NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_IsPrerelease_HelpText'),
            layout: 'hbox',
            items: [
              me.createCheckbox('releaseType'),
              me.createComboBox('releaseType', [
                ['RELEASES', NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_IsPrerelease_Releases_Item')],
                ['PRERELEASES', NX.I18n.get('CleanupPolicy_CleanupPolicySettingsForm_IsPrerelease_Prereleases_Item')]
              ])
            ],
            hidden: true
          }
        ]
      }
    ];

    me.callParent();

    Ext.override(me.getForm(), {
      getValues: function() {
        var values = this.callParent(arguments);
        values.criteria = {};
        values.criteria['lastBlobUpdated'] = values.lastBlobUpdated;
        values.criteria['lastDownloaded'] = values.lastDownloaded;
        values.criteria['releaseType'] = values.releaseType;
        return values;
      },
      setValues: function(values) {
        values.lastBlobUpdatedEnabled = values.criteria['lastBlobUpdated'] !== null;
        values.lastDownloadedEnabled = values.criteria['lastDownloaded'] !== null;
        values.releaseTypeEnabled = values.criteria['releaseType'] !== null;

        values.lastBlobUpdated = values.criteria['lastBlobUpdated'];
        values.lastDownloaded = values.criteria['lastDownloaded'];
        values.releaseType = values.criteria['releaseType'];
        this.callParent(arguments);
      }
    });
  },

  /**
   * @override
   *
   * reset the form to keep check boxes correct
   */
  loadRecord: function(model) {
    var me = this,
        checkboxes = me.down('#criteria').query('checkbox');

    Ext.Array.each(checkboxes, function(checkbox) {
      checkbox.resetOriginalValue();
    });

    me.form.reset();

    me.callParent(arguments);
  },

  createCheckbox: function(name) {
    return {
      xtype: 'checkbox',
      name: name + 'Enabled',
      itemId: name + 'Enabled',
      boxLabel: name + 'Enabled',
      listeners: {
        afterrender: function() {
          // HACK. Why? Boxlabel makes a label appear, on which we
          // use selection in tests but do not want to actually display it
          this.boxLabelEl.dom.style.width = '0';
          this.boxLabelEl.dom.style.overflow = 'hidden';
        },
        change: function() {
          var form = this.up('form'),
              field = form.down('#' + name);

          if (this.getValue()) {
            field.enable();
          }
          else {
            field.disable();
          }

          form.isValid();
        }
      }
    };
  },

  createNumberField: function(name) {
    return {
      xtype: 'numberfield',
      name: name,
      itemId: name,
      minValue: 1,
      // (2147483646 - 1 / 86400) java max - 1 / seconds in day
      maxValue: 24855,
      allowDecimals: false,
      allowExponential: false,
      allowBlank: false,
      disabled: true,
      width: 580,
      style: {
        marginLeft: '5px'
      },
      listeners: {
        change: function() {
          var form = this.up('form'),
              checkbox = form.down('#' + name + 'Enabled');
          if (this.getValue() && !checkbox.getValue()) {
            checkbox.setValue(true);
            checkbox.resetOriginalValue();
          }
        }
      }
    };
  },

  createComboBox: function(name, store) {
    return {
      xtype: 'combo',
      name: name,
      itemId: name,
      editable: false,
      store: store,
      readOnlyOnUpdate: false,
      disabled: true,
      allowBlank: false,
      width: 580,
      style: {
        marginLeft: '5px'
      },
      listeners: {
        change: function() {
          var form = this.up('form'),
              checkbox = form.down('#' + name + 'Enabled');
          if (this.getValue() && !checkbox.getValue()) {
            checkbox.setValue(true);
            checkbox.resetOriginalValue();
          }
        }
      }
    };
  },

  onFormatChange: function() {
    if (criteriaConfiguration) {
      this.updateFields();
    }
    else {
      this.initialiseCriteriaFields();
    }
  },

  updateFields: function() {
    var me = this;
    var items = me.down('#cleanupPolicies').items;
    var formatCombo = items.items[1];
    var format = formatCombo.getValue();
    var containers = this.getContainers();

    Ext.Object.each(containers, function(field, container, myself) {
      var formatCriteria = criteriaConfiguration[format];

      if (!formatCriteria) {
        formatCriteria = criteriaConfiguration['default'];
      }

      var visible = formatCriteria[field];

      container.setVisible(visible);

      if (!visible) {
        container.down('checkbox').setValue(false);
        container.down('combo').clearValue();
      }
    });
  },

  initialiseCriteriaFields: function() {
    var me = this;
    var containers = this.getContainers();

    NX.direct.cleanup_CleanupPolicy.getApplicableFields(Object.keys(containers), function(response) {
      if (Ext.isObject(response) && response.success) {

        criteriaConfiguration = response.data;
        me.updateFields();
      }
    });
  },

  getContainers: function() {
    var me = this;
    var elements = me.down('#criteria').items;

    var containers = {};

    elements.eachKey(function (k, container) {
      var containerId = container.getItemId();

      if (containerId.indexOf('Container') >= 0) {
        var key = containerId.substring(0, containerId.indexOf('Container'));
        containers[key] = container;
      }
    });

    return containers;
  }
});
