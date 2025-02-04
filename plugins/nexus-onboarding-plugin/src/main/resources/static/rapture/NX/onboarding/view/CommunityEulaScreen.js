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
 * @since 3.77
 */
Ext.define('NX.onboarding.view.CommunityEulaScreen', {
  extend: 'NX.onboarding.view.OnboardingScreen',
  alias: 'widget.nx-onboarding-community-eula-screen',
  requires: [
    'NX.Conditions',
    'NX.I18n',
    'NX.view.SecondaryContainer',
    'NX.util.Url'
  ],

  /**
   * @override
   */
  initComponent: function () {
    const me = this;

    Ext.apply(me, {
      title: NX.I18n.render(me, 'Title'),

      buttons: ['back', '->', {
        text: NX.I18n.render(me, 'Agree_Button'),
        action: 'next',
        enabled: true,
        cls: 'ce-eula-agree-button',
      }],

      fields: [
        {
          xtype: 'uxiframe',
          src: NX.util.Url.licenseUrl(),
          cls: 'nx-onboarding-community-eula-license',
        },
        {
          xtype: 'nx-secondary-container',
          reactView: window.ReactComponents.CommunityEulaOnboarding
        },
      ],
    });
    me.callParent();
  },
});
