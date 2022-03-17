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
 * Capabilities  self.mediator().
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CapabilitiesMediator', {

  singleton: true,

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  requires: [
    'Nexus.capabilities.Icons',
    'Nexus.capabilities.CapabilityStore',
    'Nexus.capabilities.CapabilityTypeStore',
    'Nexus.capabilities.factory.ComboFactory'
  ],

  /**
   * @property
   */
  capabilityStore: undefined,

  /**
   * @property
   */
  capabilityTypeStore: undefined,

  /**
   * @private
   */
  comboFactory: undefined,

  /**
   * @constructor
   */
  constructor: function () {
    var self = this;

    self.capabilityStore = NX.create('Nexus.capabilities.CapabilityStore');
    self.capabilityTypeStore = NX.create('Nexus.capabilities.CapabilityTypeStore');
    self.comboFactory = Nexus.capabilities.factory.ComboFactory;
  },

  /**
   * Creates a capability via REST.
   */
  addCapability: function (capability, successHandler, failureHandler) {
    var self = this;

    self.logDebug('Adding capability: ' + Ext.encode(capability));

    Ext.Ajax.request({
      url: self.capabilityStore.url,
      method: 'POST',
      scope: self,
      suppressStatus: true,
      jsonData: capability,
      success: successHandler,
      failure: failureHandler
    });
  },

  /**
   * Updates a capability via REST.
   */
  updateCapability: function (capability, successHandler, failureHandler) {
    var self = this;

    self.logDebug('Updating capability: ' + Ext.encode(capability));

    Ext.Ajax.request({
      url: self.capabilityStore.urlOf(capability.id),
      method: 'PUT',
      scope: self,
      suppressStatus: true,
      jsonData: capability,
      success: successHandler,
      failure: failureHandler
    });
  },

  /**
   * Enables a capability via REST.
   */
  enableCapability: function (capability, successHandler, failureHandler) {
    var self = this;

    self.logDebug('Enabling capability: ' + capability.id);

    Ext.Ajax.request({
      url: self.capabilityStore.urlOf(capability.id) + "/enable",
      method: 'PUT',
      scope: self,
      suppressStatus: true,
      success: successHandler,
      failure: failureHandler
    });
  },

  /**
   * Disables a capability via REST.
   */
  disableCapability: function (capability, successHandler, failureHandler) {
    var self = this;

    self.logDebug('Disabling capability: ' + capability.id);

    Ext.Ajax.request({
      url: self.capabilityStore.urlOf(capability.id) + "/disable",
      method: 'PUT',
      scope: self,
      suppressStatus: true,
      success: successHandler,
      failure: failureHandler
    });
  },

  /**
   * Deletes a capability via REST.
   */
  deleteCapability: function (capability, successHandler, failureHandler) {
    var self = this;

    self.logDebug('Deleting capability: ' + capability.id);

    Ext.Ajax.request({
      url: self.capabilityStore.urlOf(capability.id),
      method: 'DELETE',
      scope: self,
      suppressStatus: true,
      success: successHandler,
      failure: failureHandler
    });
  },

  /**
   * Returns a description of capability suitable to be displayed.
   */
  describeCapability: function (capability) {
    var description = capability.typeName;
    if (capability.description) {
      description += ' - ' + capability.description;
    }
    return description;
  },

  /**
   * Shows a message.
   */
  showMessage: function (title, message) {
    Nexus.messages.show(title, message);
  },

  /**
   * Refreshes data stores.
   */
  refresh: function () {
    var self = this;

    self.logDebug('Refreshing stores');

    self.capabilityStore.reload();
    self.capabilityTypeStore.reload();
    self.comboFactory.evictCache();
  },

  /**
   * Handles an REST response, eventually marking fields as invalid.
   * @param response REST response
   * @param [options] response options
   * @param [title] dialog error
   * @param [form] containing fields that should be marked in case of a validation error
   */
  handleError: function (response, options, title, form) {
    var handled = false,
        remainingMessages = [],
        message;

    if (response.siestaValidationError) {
      handled = true;
      Ext.each(response.siestaValidationError, function (error) {
        var marked = false,
            field;

        if (form) {
          field = form.findField('property.' + error.id);
          if (!field) {
            field = form.findField(error.id);
          }
          if (field) {
            marked = true;
            field.markInvalid(error.message);
          }
        }
        if (!marked) {
          remainingMessages.push(error.message);
        }
      });
    }
    if (response.siestaError) {
      handled = true;
      remainingMessages.push(response.siestaError.message);
    }
    if (!handled) {
      if (response.responseText) {
        message = Sonatype.utils.parseHTMLErrorMessage(response.responseText);
      }
      if (!message) {
        message = title + ' (' + response.statusText + ')';
        title = undefined;
      }
      remainingMessages.push(message);
    }
    if (remainingMessages.length > 0) {
      Ext.Msg.show({
        title: title || 'Operation failed',
        msg: remainingMessages.join('\n'),
        buttons: Ext.Msg.OK,
        icon: Ext.MessageBox.ERROR,
        closeable: false
      });
    }
  },

  /**
   * Calculates status label of a capability.
   */
  getStatusLabel: function (capability) {
    var enabled = capability.enabled,
        active = capability.active,
        error = capability.error;

    if (enabled && error) {
      return 'Error';
    }
    if (enabled && active) {
      return 'Active';
    }
    if (enabled && !active) {
      return 'Passive';
    }

    return 'Disabled';
  }

});