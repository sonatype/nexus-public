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
/*global NX, Ext, Nexus*/

/**
 * Capability About View.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CapabilityAbout', {
  extend: 'Ext.Panel',

  mixins: [
    'Nexus.LogAwareMixin',
    'Nexus.capabilities.CapabilitiesMediatorMixin'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var self = this;

    Ext.apply(self, {
      cls: 'nx-capabilities-CapabilityAbout',
      title: 'About',
      autoScroll: true,
      html: ''
    });

    self.constructor.superclass.initComponent.apply(self, arguments);
  },

  /**
   * Sets the current selected capability.
   * @param capability selected
   */
  setCapability: function (capability) {
    var self = this,
        about = '',
        capabilityType = self.mediator().capabilityTypeStore.getTypeById(capability.typeId);

    if (capabilityType) {
      about = capabilityType.about;
    }
    self.html = about;
    if (self.body) {
      self.body.update(about);
    }
  }

});