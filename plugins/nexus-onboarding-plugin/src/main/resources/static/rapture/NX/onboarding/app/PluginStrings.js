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
 * @since 3.next
 */
Ext.define('NX.onboarding.app.PluginStrings', {
  '@aggregate_priority': 90,

  singleton: true,
  requires: [
    'NX.I18n'
  ],

  keys: {
    Onboarding_Text: 'Onboarding',
    Onboarding_Description: 'Configuration changes requiring attention'
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
      Description: '<p>Configure whether anonymous access is enabled for the server.</p>'
    }
  }
}, function(obj) {
  NX.I18n.register(obj);
});
