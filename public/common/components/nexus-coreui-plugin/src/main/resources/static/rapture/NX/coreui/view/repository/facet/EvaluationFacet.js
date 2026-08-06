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
/**
 * Configuration for repository evaluation facet.
 */
Ext.define('NX.coreui.view.repository.facet.EvaluationFacet', {
  extend: 'Ext.form.FieldContainer',
  alias: 'widget.nx-coreui-repository-evaluation-facet',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'fieldset',
        cls: 'nx-form-section',
        title: 'Evaluation',

        items: [
          {
            xtype: 'radiogroup',
            fieldLabel: 'Hosted Repository Evaluation',
            allowBlank: false,
            columns: 1,
            vertical: true,
            itemId: 'evaluationModeRadioGroup',
            items: [
              {
                boxLabel: 'Inherit from global evaluation settings',
                name: 'attributes.evaluation.mode',
                inputValue: 'INHERIT'
              },
              {
                boxLabel: 'Override',
                name: 'attributes.evaluation.mode',
                inputValue: 'OVERRIDE'
              },
              {
                boxLabel: 'Disable Evaluation',
                name: 'attributes.evaluation.mode',
                inputValue: 'DISABLE'
              }
            ],
            listeners: {
              afterrender: function(radiogroup) {
                // Store original values from form record
                var form = radiogroup.up('form');
                if (form && form.getRecord) {
                  var record = form.getRecord();
                  if (record && record.data && record.data.attributes && record.data.attributes.evaluation) {
                    var evaluation = record.data.attributes.evaluation;
                    radiogroup.originalEvaluationValues = {
                      activityTimeFrame: evaluation.activityTimeFrame || '30',
                      artifactLatestVersions: evaluation.artifactLatestVersions || '1',
                      policyEvaluationStage: evaluation.policyEvaluationStage || 'build'
                    };
                  } else {
                    radiogroup.originalEvaluationValues = {
                      activityTimeFrame: '30',
                      artifactLatestVersions: '1',
                      policyEvaluationStage: 'build'
                    };
                  }
                }

                // Set default mode to INHERIT if no mode is set
                var currentValue = radiogroup.getValue();
                if (!currentValue || !currentValue['attributes.evaluation.mode']) {
                  radiogroup.setValue({'attributes.evaluation.mode': 'INHERIT'});
                }

                // Fetch global evaluation settings from API for INHERIT mode display
                me._fetchAndApplyGlobalSettings(radiogroup);
              },
              change: function(radiogroup, newValue) {

                var form = me.up('form');
                var activityTimeFrame = form.down('#evaluationActivityTimeFrame');
                var artifactLatestVersions = form.down('#evaluationArtifactLatestVersions');
                var policyEvaluationStage = form.down('#evaluationPolicyEvaluationStage');

                var mode = newValue['attributes.evaluation.mode'];
                var oldMode = radiogroup.lastMode;
                radiogroup.lastMode = mode;
                var enableFields = (mode === 'OVERRIDE');

                // Save current OVERRIDE values before leaving OVERRIDE mode
                if (oldMode === 'OVERRIDE' && mode !== 'OVERRIDE') {
                  radiogroup.savedOverrideValues = {
                    activityTimeFrame: activityTimeFrame ? activityTimeFrame.getValue() : null,
                    artifactLatestVersions: artifactLatestVersions ? artifactLatestVersions.getValue() : null,
                    policyEvaluationStage: policyEvaluationStage ? policyEvaluationStage.getValue() : null
                  };
                }

                // Enable/disable fields based on mode
                if (activityTimeFrame) {
                  activityTimeFrame.setDisabled(!enableFields);
                  activityTimeFrame.allowBlank = !enableFields;
                }
                if (artifactLatestVersions) {
                  artifactLatestVersions.setDisabled(!enableFields);
                  artifactLatestVersions.allowBlank = !enableFields;
                }
                if (policyEvaluationStage) {
                  policyEvaluationStage.setDisabled(!enableFields);
                  policyEvaluationStage.allowBlank = !enableFields;
                }

                // Set values based on mode
                if (mode === 'OVERRIDE') {
                  // Restore saved OVERRIDE values if returning to OVERRIDE, otherwise use global settings
                  var valuesToUse;
                  if (radiogroup.savedOverrideValues && (radiogroup.savedOverrideValues.activityTimeFrame || radiogroup.savedOverrideValues.artifactLatestVersions || radiogroup.savedOverrideValues.policyEvaluationStage)) {
                    // Restore previous OVERRIDE values
                    valuesToUse = radiogroup.savedOverrideValues;
                  } else {
                    // Use global settings (INHERIT values) for first-time OVERRIDE or from DISABLE
                    valuesToUse = radiogroup.globalEvaluationSettings || {
                      activityTimeFrame: '30',
                      artifactLatestVersions: '1',
                      policyEvaluationStage: 'build'
                    };
                  }
                  me._setFieldValues(form, valuesToUse, true);
                } else if (mode === 'INHERIT') {
                  // Fetch and show global settings values (disabled)
                  if (radiogroup.globalEvaluationSettings) {
                    // Use cached global settings
                    me._setFieldValues(form, radiogroup.globalEvaluationSettings, true);
                  } else {
                    // Fetch global settings if not already cached
                    me._fetchAndApplyGlobalSettings(radiogroup);
                  }
                } else if (mode === 'DISABLE') {
                  // Clear values for DISABLE mode
                  me._setFieldValues(form, {
                    activityTimeFrame: null,
                    artifactLatestVersions: null,
                    policyEvaluationStage: null
                  }, true);
                }
              }
            }
          },
          {
            xtype: 'combo',
            name: 'attributes.evaluation.activityTimeFrame',
            itemId: 'evaluationActivityTimeFrame',
            fieldLabel: 'Activity Time Frame',
            helpText: 'Set the time frame for evaluating components based on recent repository activity.',
            disabled: true,
            allowBlank: true,
            editable: false,
            emptyText: 'Select Activity Time Frame',
            store: [
              ['30', 'Last 30 Days'],
              ['60', 'Last 60 Days'],
              ['90', 'Last 90 Days']
            ],
            queryMode: 'local',
            validator: function() {
              var form = this.up('form');
              var radioGroup = form.down('#evaluationModeRadioGroup');
              var mode = radioGroup ? radioGroup.getValue()['attributes.evaluation.mode'] : null;

              if (mode === 'OVERRIDE' && !this.getValue()) {
                return 'This field is required when Override mode is selected';
              }
              return true;
            }
          },
          {
            xtype: 'combo',
            name: 'attributes.evaluation.artifactLatestVersions',
            itemId: 'evaluationArtifactLatestVersions',
            fieldLabel: 'Latest Deployed Versions',
            helpText: 'Set the number of most recently deployed component versions to evaluate',
            disabled: true,
            allowBlank: true,
            editable: false,
            emptyText: 'Select Latest Deployed Versions',
            store: [
              ['1', '1'],
              ['2', '2'],
              ['3', '3'],
              ['4', '4'],
              ['5', '5']
            ],
            queryMode: 'local',
            validator: function() {
              var form = this.up('form');
              var radioGroup = form.down('#evaluationModeRadioGroup');
              var mode = radioGroup ? radioGroup.getValue()['attributes.evaluation.mode'] : null;

              if (mode === 'OVERRIDE' && !this.getValue()) {
                return 'This field is required when Override mode is selected';
              }
              return true;
            }
          },
          {
            xtype: 'combo',
            name: 'attributes.evaluation.policyEvaluationStage',
            itemId: 'evaluationPolicyEvaluationStage',
            fieldLabel: 'Policy Evaluation Stage',
            helpText: 'Select the policy evaluation stage used for the initial audit and ongoing monitoring.',
            disabled: true,
            allowBlank: true,
            editable: false,
            emptyText: 'Select Policy Evaluation Stage',
            store: [
              ['build', 'Build'],
              ['stage-release', 'Stage Release'],
              ['release', 'Release'],
              ['operate', 'Operate']
            ],
            queryMode: 'local',
            validator: function() {
              var form = this.up('form');
              var radioGroup = form.down('#evaluationModeRadioGroup');
              var mode = radioGroup ? radioGroup.getValue()['attributes.evaluation.mode'] : null;

              if (mode === 'OVERRIDE' && !this.getValue()) {
                return 'This field is required when Override mode is selected';
              }
              return true;
            }
          }
        ]
      }
    ];

    me.callParent();
  },

  /**
   * Helper method to set field values
   * @private
   */
  _setFieldValues: function(form, settings, clearInvalid) {
    var activityTimeFrame = form.down('#evaluationActivityTimeFrame');
    var artifactLatestVersions = form.down('#evaluationArtifactLatestVersions');
    var policyEvaluationStage = form.down('#evaluationPolicyEvaluationStage');

    if (activityTimeFrame) {
      activityTimeFrame.setValue(settings.activityTimeFrame);
      if (clearInvalid) {
        activityTimeFrame.clearInvalid();
      }
    }
    if (artifactLatestVersions) {
      artifactLatestVersions.setValue(settings.artifactLatestVersions);
      if (clearInvalid) {
        artifactLatestVersions.clearInvalid();
      }
    }
    if (policyEvaluationStage) {
      policyEvaluationStage.setValue(settings.policyEvaluationStage);
      if (clearInvalid) {
        policyEvaluationStage.clearInvalid();
      }
    }
  },

  /**
   * Helper method to fetch and apply global evaluation settings
   * @private
   */
  _fetchAndApplyGlobalSettings: function(radiogroup, callback) {
    var me = this;
    if (radiogroup.fetchingGlobalSettings) {
      return;
    }

    radiogroup.fetchingGlobalSettings = true;
    var form = radiogroup.up('form');

    Ext.Ajax.request({
      url: '/service/rest/v1/evaluation/settings',
      method: 'GET',
      success: function(response) {
        radiogroup.fetchingGlobalSettings = false;
        try {
          var globalSettings = Ext.decode(response.responseText);
          if (globalSettings) {
            var rawStage = globalSettings.policyEvaluationStage || 'build';
            var normalizedStage = rawStage.toLowerCase().replace(/_/g, '-');
            radiogroup.globalEvaluationSettings = {
              activityTimeFrame: String(globalSettings.activityTimeFrame != null ? globalSettings.activityTimeFrame : 30),
              artifactLatestVersions: String(globalSettings.artifactLatestVersions != null ? globalSettings.artifactLatestVersions : 1),
              policyEvaluationStage: normalizedStage
            };
          } else {
            radiogroup.globalEvaluationSettings = {
              activityTimeFrame: '30',
              artifactLatestVersions: '1',
              policyEvaluationStage: 'build'
            };
          }
        } catch (e) {
          // Use defaults if parsing fails
          radiogroup.globalEvaluationSettings = {
            activityTimeFrame: '30',
            artifactLatestVersions: '1',
            policyEvaluationStage: 'build'
          };
        }

        // Apply the settings to fields
        me._setFieldValues(form, radiogroup.globalEvaluationSettings, true);

        if (callback) {
          callback();
        }
      },
      failure: function() {
        radiogroup.fetchingGlobalSettings = false;
        // Use defaults if API call fails
        radiogroup.globalEvaluationSettings = {
          activityTimeFrame: '30',
          artifactLatestVersions: '1',
          policyEvaluationStage: 'build'
        };

        // Apply default settings to fields
        me._setFieldValues(form, radiogroup.globalEvaluationSettings, true);

        if (callback) {
          callback();
        }
      }
    });
  }
});
