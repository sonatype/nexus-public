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
 * @since 3.17
 */
Ext.define('NX.onboarding.app.PluginStrings', {
  '@aggregate_priority': 90,

  singleton: true,
  requires: [
    'NX.I18n'
  ],

  keys: {
    Onboarding_Text: 'Onboarding',
    Onboarding_Description: 'Configuration changes requiring attention',
    Onboarding_Authenticate: 'Your <b>admin</b> user password is located in <br><b>{0}</b> on the server.'
  },

  bundles: {
    'NX.onboarding.view.OnboardingStartScreen': {
      Title: 'Setup',
      Description: '<p>This wizard will help you complete required setup tasks.</p>',
    },
    'NX.onboarding.view.OnboardingCompleteScreen': {
      Title: 'Complete',
      Description: '<p>The setup tasks have been completed, enjoy using Nexus Repository Manager!</p>',
      Finish_Button: 'Finish'
    },
    'NX.onboarding.view.ChangeAdminPasswordScreen': {
      Title: 'Please choose a password for the admin user'
    },
    'NX.onboarding.view.ConfigureAnonymousAccessScreen': {
      Title: 'Configure Anonymous Access',
      Description: '<p>Enabling anonymous access will allow unauthenticated downloads, browsing, and ' +
      'searching of repository content by default. Permissions for unauthenticated users can be changed by ' +
      'editing the roles assigned to the <b>anonymous</b> user.<br><br>' +
      '<a href="https://links.sonatype.com/products/nexus/anonymous-access/docs" target="_blank">More information <span class="x-fa fa-external-link"></a></p>',
      Label: 'Enable anonymous access'
    }
  }
}, function(obj) {
  NX.I18n.register(obj);
});
