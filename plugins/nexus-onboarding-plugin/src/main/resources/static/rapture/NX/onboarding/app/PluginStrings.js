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
    Onboarding_Authenticate: 'Your <b>admin</b> user password is located in <br><b>{0}</b> on the server.',
    Onboarding_LoadStepsError: 'Failed to retrieve setup steps from server'
  },

  bundles: {
    'NX.onboarding.view.OnboardingStartScreen': {
      Title: 'Welcome to Sonatype Nexus Repository Manager',
      Description: '<p>This wizard will guide you through setup tasks to get started.</p>',
    },
    'NX.onboarding.view.OnboardingCompleteScreen': {
      Title: 'Setup Complete',
      Description: '<p>Your instance is now ready to use. Explore Sonatype Nexus Repository to unlock its full potential.</p>',
      Finish_Button: 'Finish'
    },
    'NX.onboarding.view.ChangeAdminPasswordScreen': {
      Title: 'Please choose a password for the admin user'
    },
    'NX.onboarding.view.ConfigureAnonymousAccessScreen': {
      Title: 'Configure Anonymous Access',
      Description: '<p><b>Enable anonymous access</b> means that by default, users can search, browse and download  ' +
      'components from repositories without credentials. Please <b>consider the security implications for your ' +
      ' organization.</b>' +
      '<br>' +
      '<p><b>Disable anonymous access</b> should be chosen with care, as it <b>will require credentials for all</b> ' +
      'users and/or build tools.'+
      '<br><br>' +
      '<a href="https://links.sonatype.com/products/nexus/anonymous-access/docs" target="_blank" rel="noopener">More information <span class="x-fa fa-external-link"></a></p>',
      Enable_Label: 'Enable anonymous access',
      Disable_Label: 'Disable anonymous access'
    },
    'NX.onboarding.view.ConfigureAnalyticsCollectionScreen': {
      Title: 'Help Us Improve Nexus Repository',
      Description: '<p>Please help us improve the Nexus Repository experience and shape future feature improvements by sharing ' +
          'anonymous statistical metrics and performance information with Sonatype. The collected information will not contain ' +
          'identifying or proprietary information (e.g. the names of hosts, servers, repositories, or users).</p>',
      Enable_Label: 'Yes, I agree to share anonymous data.',
      Disable_Label: 'No, not interested.'
    },
    'NX.onboarding.view.CommunityDiscoverScreen': {
      Title: 'Discover Community Edition'
    },
    'NX.onboarding.view.CommunityEulaScreen': {
      Title: 'Agree End User License Agreement',
      Agree_Button: 'Agree'
    }
  }
}, function(obj) {
  NX.I18n.register(obj);
});
