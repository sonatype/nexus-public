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
 * Wizard screen.
 *
 * @since 3.0
 * @abstract
 */
Ext.define('NX.wizard.Screen', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-wizard-screen',
  requires: [
    'NX.I18n'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  config: {
    /**
     * @cfg {String}
     */
    title: undefined,

    /**
     * @cfg {String}
     */
    description: undefined,

    /**
     * @cfg {String[]/Object[]}
     */
    buttons: undefined,

    /**
     * @cfg {Object[]}
     */
    fields: undefined
  },

  layout: 'fit',

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        items = [],
        buttons = [];

    // NOTE: title is handled by controller, rendered in NX.wizard.Panel, arguably should be part of step not screen

    // add optional description
    if (me.description) {
      items.push({
        xtype: 'container',
        itemId: 'description',
        html: me.description
      });
    }

    // add optional form fields
    if (me.fields) {
      Ext.Array.push(items, me.fields);
    }

    // add optional buttons
    if (me.buttons) {
      Ext.Array.each(me.buttons, function (button) {
        if (button === 'next') {
          buttons.push({
            text: NX.I18n.get('Wizard_Next'),
            action: 'next',
            ui: 'nx-primary',
            formBind: true
          });
        }
        else if (button === 'back') {
          buttons.push({
            text: NX.I18n.get('Wizard_Back'),
            action: 'back',
            ui: 'default'
          });
        }
        else if (button === 'cancel') {
          buttons.push({
            text: NX.I18n.get('Wizard_Cancel'),
            action: 'cancel',
            ui: 'default'
          })
        }
        else if (button === '->') {
          buttons.push(button);
        }
        else if (Ext.isObject(button)) {
          // custom button configuration
          buttons.push(button);
        }
        else {
          me.logWarn('Invalid button:', button);
        }
      });
    }

    Ext.apply(me, {
      items: {
        xtype: 'form',
        itemId: 'fields',
        items: items,
        buttonAlign: 'left',
        buttons: buttons
      }
    });

    me.callParent(arguments);
  },

  /**
   * @returns {Ext.container.Container}
   */
  getDescriptionContainer: function() {
    return this.down('#description');
  },

  /**
   * @return {Ext.toolbar.Toolbar}
   */
  getButtonsContainer: function() {
    return this.down('form').getDockedItems('toolbar[dock="bottom"]')[0];
  }
});
