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
Ext.define('NX.onboarding.controller.Onboarding', {
  extend: 'NX.wizard.Controller',
  requires: [
    'NX.Messages',
    'NX.I18n',
    'NX.onboarding.step.OnboardingStartStep',
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
    'OnboardingModal'
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
          changed: me.stateChanged
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
    if (NX.State.getValue('onboarding.required') && NX.State.getUser()) {
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

  itemsLoaded: function (store, records) {
    var me = this;
    me.registerStep('NX.onboarding.step.OnboardingStartStep');
    records.forEach(function(record) {
      me.registerStep('NX.onboarding.step.' + record.get('type') + 'Step');
    });
    me.registerStep('NX.onboarding.step.OnboardingCompleteStep');

    Ext.widget('nx-onboarding-modal');

    me.load();
  }
});
