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
 * Manages health check enabling/disabling on a per repository basis.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.HealthCheckRepositoryConfiguration', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n'
  ],
  stores: [
    'HealthCheckRepositoryStatus'
  ],
  views: [
    'healthcheck.HealthCheckEula'
  ],
  refs: [
    {ref: 'settings', selector: 'nx-coreui-repository-settings'},
    {ref: 'button', selector: 'nx-coreui-repository-feature button[action=toggleHealthCheck]'}
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      component: {
        'nx-coreui-repository-feature button[action=toggleHealthCheck]': {
          click: me.toggleHealthCheck,
          show: me.updateLabel
        },
        'nx-coreui-repository-settings-form': {
          recordloaded: me.updateHealthCheckButton
        }
      }
    });
  },

  /**
   * Update the button text based on whether or not the feature is enabled.
   * @private
   */
  updateLabel: function () {
    var me = this,
        button = me.getButton();

    if (!button.disabled) {
      var settings = me.getSettings();
      var form = settings.down('form');
      if (form) {
        var repository = form.getForm().getRecord();
        var repositoryStatus = me.getHealthCheckRepositoryStatusStore().findRecord('repositoryName',
            repository.get('name'));
        if (repositoryStatus && repositoryStatus.get('enabled') === true) {
          button.setText(NX.I18n.get('Repository_RepositoryFeature_HealthCheckDisable_Button'));
        }
        else {
          button.setText(NX.I18n.get('Repository_RepositoryFeature_HealthCheckEnable_Button'));
        }
      }
    }
  },

  /**
   * Show/hide Health Check button for proxy repositories based on permissions.
   *
   * @private
   */
  updateHealthCheckButton: function (e, model) {
    var me = this,
        button = me.getButton();

    if (model.get('type') !== 'proxy' || model.get('format') === 'maven2'
            && model.get('attributes').maven.versionPolicy !== 'RELEASE') {
      button.hide();
      return;
    }
    button.show();

    if (NX.Permissions.check('nexus:repository-admin:' + model.get('format') + ':' + model.get('name') + ':edit')) {
      button.enable();
    }
    else {
      button.disable();
    }
    me.updateLabel();
  },

  /**
   * Enable/disable Health Check for specified repository. Checks for eula acceptance and offers it if not
   * previously accepted.
   *
   * @private
   */
  toggleHealthCheck: function () {
    var me = this,
        repository = me.getSettings().down('form').getForm().getRecord(),
        repositoryName = repository.get('name'),
        status = me.getHealthCheckRepositoryStatusStore().findRecord('repositoryName', repositoryName);

    if (status.get('eulaAccepted')) {
      me.enableAnalysis(!status.get('enabled'), repositoryName);
    }
    else {
      Ext.widget('nx-coreui-healthcheck-eula', {
        acceptFn: function () {
          me.enableAnalysis(true, repositoryName);
        }
      });
    }
  },

  /**
   * Set the enabled status appropriately for a repository.  Assumes the eula has already been accepted.
   * @private
   */
  enableAnalysis: function (enabled, repositoryName) {
    var me = this;
    NX.direct.healthcheck_Status.update(
        true /* eula accepted */, repositoryName, enabled /* enabled */,
        function (response) {
          if (Ext.isObject(response) && response.success) {
            me.getHealthCheckRepositoryStatusStore().load(function (records, operation, success) {
              if (success) {
                me.updateLabel();
              }
            });
          }
        }
    );
  }

});
