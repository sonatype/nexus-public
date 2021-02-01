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
/*global NX, Ext, Sonatype, Nexus*/

/**
 * Window presented to user to create a new capability.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CreateCapabilityWindow', {
  extend: 'Ext.Window',

  mixins: [
    'Nexus.LogAwareMixin',
    'Nexus.capabilities.CapabilitiesMediatorMixin'
  ],

  requires: [
    'Nexus.capabilities.Icons',
    'Nexus.capabilities.CapabilitySettingsFieldSet'
  ],

  /**
   * @constructor
   */
  constructor: function (callbackOnSuccess) {
    var self = this;

    self.callbackOnSuccess = callbackOnSuccess;

    self.constructor.superclass.constructor.call(self);
  },

  /**
   * @override
   */
  initComponent: function () {
    var self = this;

    self.aboutPanel = NX.create('Ext.Panel', {
      border: false,
      frame: false,
      html: ''
    });

    self.settings = NX.create('Nexus.capabilities.CapabilitySettingsFieldSet', {
      title: 'Settings'
    });

    /**
     * @private
     */
    self.formPanel = NX.create('Ext.form.FormPanel', {
      border: false,
      monitorValid: true,
      labelWidth: 175,
      items: [
        {
          xtype: 'fieldset',
          autoHeight: true,
          collapsed: false,
          border: false,
          items: {
            xtype: 'combo',
            fieldLabel: 'Type',
            itemCls: 'required-field',
            helpText: "Type of configured capability",
            name: 'typeId',
            store: self.mediator().capabilityTypeStore,
            displayField: 'name',
            valueField: 'id',
            forceSelection: true,
            editable: false,
            mode: 'local',
            triggerAction: 'all',
            emptyText: 'Select...',
            selectOnFocus: true,
            allowBlank: false,
            anchor: '96%',
            listeners: {
              select: self.handleCapabilityTypeSelected.createDelegate(self)
            }
          }
        },
        {
          xtype: 'fieldset',
          title: 'About',
          autoHeight: false,
          autoScroll: true,
          collapsible: true,
          collapsed: false,
          items: self.aboutPanel,
          listeners: {
            collapse: function () {
              // HACK: Fix window mask when about panel collapses
              self.fixMask();
            }
          }
        },
        self.settings
      ],

      buttons: [
        {
          text: 'Add',
          formBind: true,
          scope: self,
          handler: self.handleAdd
        },
        {
          xtype: 'link-button',
          text: 'Cancel',
          formBind: false,
          scope: self,
          handler: self.handleCancel
        }
      ],

      keys: [
        {
          // Close dialog on ESC
          key: Ext.EventObject.ESC,
          scope: self,
          fn: self.handleCancel
        }
      ]
    });

    Ext.apply(self, {
      cls: 'nx-capabilities-CreateCapabilityWindow',
      title: 'Create new capability',

      // FIXME: icon isn't properly aligned ATM, disable it for now its distracting
      //iconCls: icons.get('warning').cls,

      width: 640,
      autoHeight: true,
      border: false,

      modal: true,
      constrain: true,
      closable: false,
      resizable: true,

      items: [
        self.formPanel
      ],

      listeners: {
        show: function () {
          // auto-focus comment/description if we are showing it
          //self.formPanel.find('name', 'typeId')[0].focus(false, 100);
        }
      }
    });

    self.constructor.superclass.initComponent.apply(self, arguments);
  },

  importCapability: function (capability) {
    var self = this,
        form = self.formPanel.getForm();

    self.settings.importCapability(form, capability);
    self.renderAbout(capability.typeId);
    self.doLayout();
  },

  /**
   * @private
   */
  callbackOnSuccess: undefined,

  /**
   * @private
   */
  aboutPanel: undefined,

  /**
   * @private
   */
  settings: undefined,

  /**
   * HACK: To fix the modal dialog mask.  Unsure how better to do this, needs to be done when the dialog resizes.
   *
   * @private
   */
  fixMask: function () {
    var self = this;
    self.setPosition(self.getPosition());
  },

  /**
   * @private
   */
  handleCapabilityTypeSelected: function (combo) {
    var self = this;

    self.renderAbout(combo.getValue());
    self.settings.setCapabilityType(combo.getValue());
    self.doLayout();

    // HACK: Fix window mask when capability type changes
    self.fixMask();
  },

  /**
   * @private
   */
  renderAbout: function (capabilityTypeId) {
    var self = this,
        about = '',
        capabilityType = self.mediator().capabilityTypeStore.getTypeById(capabilityTypeId);

    if (capabilityType) {
      about = capabilityType.about;
    }
    self.aboutPanel.html = about;
    if (self.aboutPanel.body) {
      self.aboutPanel.body.update(about);
    }
  },

  /**
   * @private
   */
  handleAdd: function () {
    var self = this,
        form = self.formPanel.getForm(),
        mask = NX.create('Ext.LoadMask', self.body, {
          msg: 'Please wait...'
        }),
        capability;

    mask.show();

    capability = self.settings.exportCapability(form);

    self.mediator().addCapability(capability,
        function (response) {
          self.mediator().showMessage(
              'Capability added', self.mediator().describeCapability({typeName: self.settings.capabilityType.name})
          );
          mask.hide();
          self.close();
          if (self.callbackOnSuccess) {
            var responseObj = Ext.decode(response.responseText);
            if (responseObj && responseObj.capability) {
              self.callbackOnSuccess(responseObj.capability.id);
            }
          }
        },
        function (response, options) {
          self.mediator().handleError(response, options, 'Capability could not be created', self.formPanel.getForm());
          mask.hide();
        }
    );
  },

  /**
   * @private
   */
  handleCancel: function () {
    this.close();
  }

});
