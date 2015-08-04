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
/*global NX, Ext, Nexus*/

/**
 * Capability view.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CapabilityView', {
  extend: 'Ext.Panel',

  mixins: [
    'Nexus.LogAwareMixin',
    'Nexus.capabilities.CapabilitiesMediatorMixin'
  ],

  requires: [
    'Nexus.capabilities.Icons',
    'Nexus.capabilities.CapabilitySummary',
    'Nexus.capabilities.CapabilitySettings',
    'Nexus.capabilities.CapabilityStatus',
    'Nexus.capabilities.CapabilityAbout'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var self = this,
        icons = Nexus.capabilities.Icons;

    self.messageTpl = NX.create('Ext.XTemplate',
        '<div class="nx-capabilities-CapabilitySummary-message">',
        '  <div>{icon}{html}</div>',
        '</div>',
        {
          compiled: true,

          message: function (capability) {
            var self = this,
                icons = Nexus.capabilities.Icons;

            if (capability.enabled && !capability.active) {
              return self.apply({
                icon: icons.get('warning').img,
                html: '<b>' + capability.stateDescription + '</b>.'
              });
            }
            return '';
          }
        });

    self.summaryView = NX.create('Nexus.capabilities.CapabilitySummary');
    self.settingsView = NX.create('Nexus.capabilities.CapabilitySettings');
    self.statusView = NX.create('Nexus.capabilities.CapabilityStatus');
    self.aboutView = NX.create('Nexus.capabilities.CapabilityAbout');

    Ext.apply(self, {
      cls: 'nx-capabilities-CapabilityView',
      header: true,
      border: false,
      layout: 'fit',
      items: {
        xtype: 'tabpanel',
        title: 'Capability',
        iconCls: icons.get('capability').cls,
        items: [
          self.summaryView,
          self.settingsView,
          self.statusView,
          self.aboutView
        ],
        activeTab: 0,
        layoutOnTabChange: true
      }
    });

    self.constructor.superclass.initComponent.apply(self, arguments);
  },

  /**
   * Update the capability record.
   * @param capability
   */
  updateRecord: function (capability) {
    var self = this,
        icons = Nexus.capabilities.Icons;

    self.setTitle(
        self.mediator().describeCapability(capability) + self.messageTpl.message(capability),
        icons.iconFor(capability).cls);

    self.summaryView.setCapability(capability);
    self.settingsView.setCapability(capability);
    self.statusView.setCapability(capability);
    self.aboutView.setCapability(capability);
  }

});