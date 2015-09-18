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
/*global Ext*/

/**
 * Abstract settings panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.SettingsPanel', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-settingsPanel',
  autoScroll: true,

  dockedItems: [{
    xtype: 'toolbar',
    dock: 'top',
    cls: 'nx-actions'
  }],

  layout: {
    type: 'vbox',
    align: 'stretch'
  },

  // TODO maxWidth: 1024,

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = {
      xtype: 'panel',
      ui: 'nx-inset',

      items: me.settingsForm || []
    };

    me.callParent(arguments);
  },

  /**
   * @override
   * @param form The form to add to this settings panel
   */
  addSettingsForm: function(form) {
    this.down('panel').add(form);
  },

  /**
   * Remove all settings forms from this settings panel.
   *
   * @override
   */
  removeAllSettingsForms: function() {
    this.down('panel').removeAll();
  },

  /**
   * Loads an {@link Ext.data.Model} into this form
   * (internally just calls {@link NX.view.SettingsForm#loadRecord}).
   *
   * @public
   * @param model The model to load
   */
  loadRecord: function(model) {
    var settingsForm = this.down('nx-settingsform');
    if (settingsForm) {
      settingsForm.loadRecord(model);
    }
  }

});
