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
 *
 * @since 3.31
 */
Ext.define('NX.onboarding.step.ConfigureAnalyticsCollectionStep', {
  extend: 'NX.wizard.Step',
  requires: [
    'NX.onboarding.view.ConfigureAnalyticsCollectionScreen',
    'NX.State'
  ],

  config: {
    screen: 'NX.onboarding.view.ConfigureAnalyticsCollectionScreen',
    enabled: true
  },

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.control({
      'button[action=next]': {
        click: me.configureAnalytics
      },
      'button[action=back]': {
        click: me.moveBack
      }
    });
  },

  configureAnalytics: function(button) {
    var me = this;
    var enabled = button.up('form').down('#analyticsEnabled').getValue();

    NX.direct.capability_Capability.create({enabled: enabled, typeId: 'analytics-configuration'},
        function(response) {
          if (response && response.success) {
            me.moveNext();
          }
        }
    );
  }
});
