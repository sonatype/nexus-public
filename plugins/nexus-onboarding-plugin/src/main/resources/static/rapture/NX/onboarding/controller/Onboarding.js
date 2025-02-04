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
Ext.define('NX.onboarding.controller.Onboarding', {
  extend: 'NX.wizard.Controller',
  requires: [
    'NX.Messages',
    'NX.I18n',
    'NX.onboarding.step.OnboardingStartStep',
    'NX.onboarding.step.CommunityDiscoverStep',
    'NX.onboarding.step.CommunityEulaStep',
    'NX.onboarding.step.ChangeAdminPasswordStep',
    'NX.onboarding.step.ConfigureAnonymousAccessStep',
    'NX.onboarding.step.OnboardingCompleteStep',
    'NX.State'
  ],
  views: [
    'OnboardingWizard',
    'OnboardingStartScreen',
    'ChangeAdminPasswordScreen',
    'ConfigureAnonymousAccessScreen',
    'OnboardingCompleteScreen',
    'OnboardingModal',
    'CommunityEulaScreen',
    'CommunityDiscoverScreen'
  ],
  stores: [
    'Onboarding'
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.callParent();

    me.listen({
      component: {
        'nx-onboarding-wizard': {
          closed: me.reset
        },
        'nx-signin': {
          beforeshow: me.beforeShowSignin
        }
      },
      controller: {
        '#State': {
          changed: me.stateChanged,
          userAuthenticated: me.stateChanged
        }
      },
      store: {
        '#Onboarding': {
          load: me.itemsLoaded
        }
      }
    });
  },

  beforeShowSignin: function(signin) {
    var doOnboarding = NX.State.getValue('onboarding.required'),
        passwordFile = NX.State.getValue("admin.password.file"),
        msg = NX.I18n.format('Onboarding_Authenticate', Ext.htmlEncode(passwordFile));

    if (doOnboarding && passwordFile) {
      signin.addMessage(msg);
    }
    else {
      signin.clearMessage();
    }
  },

  stateChanged: function() {
    var isOnboardingRequired = NX.State.getValue('onboarding.required'),
        user = NX.State.getUser();
    if (isOnboardingRequired && user && user['administrator']) {
      this.loadItems();
    }
  },

  /**
   * @override
   */
  finish: function() {
    NX.State.setValue('onboarding.required', false);
    var results = Ext.ComponentQuery.query('nx-onboarding-modal');
    if (results && results.length) {
      results[0].close();
    }
  },

  loadItems: function() {
    var me = this,
    store = me.getStore('Onboarding');

    if (!store.isLoaded() && !store.isLoading()) {
      store.load();
    }
  },

  itemsLoaded: function (store, records, successful) {
    var me = this;
    me.registerStep('NX.onboarding.step.OnboardingStartStep');
    if (successful && Array.isArray(records)) {
      records.forEach(function(record) {
        me.registerStep('NX.onboarding.step.' + record.get('type') + 'Step');
      });
    }
    else {
      NX.Messages.error(NX.I18n.get('Onboarding_LoadStepsError'));
    }
    me.registerStep('NX.onboarding.step.OnboardingCompleteStep');

    Ext.widget('nx-onboarding-modal');

    me.load();
  }
});
