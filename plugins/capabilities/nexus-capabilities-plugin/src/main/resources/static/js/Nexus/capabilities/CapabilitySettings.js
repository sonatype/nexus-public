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
/*global NX, Ext, Nexus, Sonatype*/

/**
 * Capability Settings View.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CapabilitySettings', {
  extend: 'Ext.Panel',

  mixins: [
    'Nexus.LogAwareMixin',
    'Nexus.capabilities.CapabilitiesMediatorMixin'
  ],

  requires: [
    'Nexus.capabilities.CapabilitySettingsFieldSet'
  ],

  /**
   * Current selected capability.
   * @private
   */
  currentRecord: undefined,

  /**
   * Settings field set.
   * @private
   */
  settings: undefined,

  /**
   * @override
   */
  initComponent: function () {
    var self = this;

    self.settings = NX.create('Nexus.capabilities.CapabilitySettingsFieldSet', {
      border: false,
      parentPanel: self
    });

    self.formPanel = NX.create('Ext.FormPanel', {
      border: false,
      items: self.settings,
      buttonAlign: 'left',
      buttons: [
        {
          text: 'Save',
          formBind: true,
          scope: self,
          handler: function () {
            self.updateCapability();
          }
        },
        {
          xtype: 'link-button',
          text: 'Discard',
          formBind: false,
          scope: self,
          handler: function () {
            self.setCapability(self.currentRecord);
          }
        }
      ]
    });

    Ext.apply(self, {
      cls: 'nx-capabilities-CapabilitySettings',
      title: 'Settings',
      autoScroll: true,
      items: [
        self.formPanel
      ],
      listeners: {
        activate: {
          fn: function () {
            self.setCapability(self.currentRecord);
          },
          scope: self
        }
      }
    });

    self.constructor.superclass.initComponent.apply(self, arguments);
  },

  /**
   * Sets the current selected capability.
   * @param capability selected
   */
  setCapability: function (capability) {
    var self = this,
        sp = Sonatype.lib.Permissions,
        editable = sp.checkPermission('nexus:capabilities', sp.EDIT);

    self.currentRecord = capability;

    self.settings.importCapability(self.formPanel.getForm(), capability);

    self.doLayout();
    self.togglePermission(self.items, editable);
  },

  /**
   * Updates capability in Nexus.

   * @private
   */
  updateCapability: function () {
    var self = this,
        form = self.formPanel.getForm(),
        capability;

    if (!form.isValid()) {
      return;
    }

    capability = Ext.apply(self.settings.exportCapability(form), {
      id: self.currentRecord.id,
      notes: self.currentRecord.notes
    });

    self.mediator().updateCapability(capability,
        function () {
          form.items.each(function (item) {
            item.clearInvalid();
          });
          self.mediator().showMessage('Capability saved', self.mediator().describeCapability(self.currentRecord));
          self.mediator().refresh();
        },
        function (response, options) {
          self.mediator().handleError(response, options, 'Capability could not be saved', form);
        }
    );
  },

  /**
   * Enables/disables fields marked with "requiresPermission".
   * @private
   */
  togglePermission: function (items, enabled) {
    var self = this,
        iterable;

    if (items) {
      iterable = items.items;
      if (!iterable) {
        iterable = items;
      }
      Ext.each(iterable, function (item) {
        if (item) {
          if (item.requiresPermission) {
            if (enabled) {
              item.enable();
            }
            else {
              item.disable();
            }
          }
          self.togglePermission(item.items, enabled);
        }
      });
    }
  }

});