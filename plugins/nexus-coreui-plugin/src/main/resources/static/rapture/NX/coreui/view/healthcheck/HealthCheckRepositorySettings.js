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
 * Health Check repository settings form.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.healthcheck.HealthCheckRepositorySettings', {
  extend: 'NX.view.SettingsPanel',
  alias: 'widget.nx-coreui-healthcheck-repository-settings',
  requires: [
    'NX.Conditions'
  ],

  config: {
    active: false,
    repository: undefined
  },

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.settingsForm = [
      {
        xtype: 'nx-settingsform',
        paramOrder: ['repositoryId'],
        api: {
          load: 'NX.direct.healthcheck_Status.readForRepository',
          submit: 'NX.direct.healthcheck_Status.update'
        },
        settingsFormSuccessMessage: 'Health Check Repository Settings $action',
        editableCondition: NX.Conditions.isPermitted('nexus:healthcheck:update'),
        editableMarker: 'You do not have permission to update health check repository settings',

        items: [
          {
            xtype: 'hiddenfield',
            name: 'repositoryId'
          },
          {
            xtype: 'checkbox',
            name: 'eulaAccepted',
            hidden: true
          },
          {
            xtype: 'checkbox',
            name: 'enabled',
            fieldLabel: 'Enable',
            helpText: 'Enable analysis of this repository for security vulnerabilities and license issues.'
          }
        ]
      },
      {
        xtype: 'form',
        itemId: 'statusForm',
        title: 'Status',
        hidden: true,
        ui: 'nx-subsection',

        margin: 10,

        items: {
          xtype: 'displayfield',
          name: 'status'
        }
      }
    ];

    me.callParent(arguments);
    Ext.override(me.down('nx-settingsform'), {
      /**
       * @override
       * Block Ext.Direct load call if we do not have a repository id.
       */
      load: function() {
        var me = this;
        if (me.getForm().baseParams.repositoryId) {
          me.callParent(arguments);
        }
      },
      /**
       * @override
       * Block Ext.Direct submit call if EULA is not accepted & show EULA window.
       */
      submit: function() {
        var me = this;
        if (me.getForm().getFieldValues().eulaAccepted) {
          me.callParent(arguments);
        }
        else {
          Ext.widget('nx-coreui-healthcheck-eula', {
            acceptFn: function() {
              var saveButton = me.down('button[action=save]');

              me.getForm().setValues({ eulaAccepted: true });
              saveButton.fireEvent('click', saveButton);
            }
          });
        }
      }
    });

    Ext.override(me.down('nx-settingsform').getForm(), {
      /**
       * @override
       * Show status when settings form is updated.
       */
      setValues: function(values) {
        var statusForm = me.down('#statusForm');

        this.callParent(arguments);

        if (values && values['enabled'] && values['analyzing']) {
          statusForm.getForm().setValues(Ext.apply(values, { status: 'Analyzing...' }));
          statusForm.show();
        }
        else {
          statusForm.hide();
        }
      }
    });
  },

  /**
   * @private
   * Preset form base params to repository id.
   */
  applyRepository: function(repositoryModel) {
    var form = this.down('nx-settingsform');

    form.getForm().baseParams = {
      repositoryId: repositoryModel ? repositoryModel.getId() : undefined
    };

    return repositoryModel;
  }

});
