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
 * Abstract settings form.
 *
 * @since 3.0
 */
Ext.define('NX.view.SettingsForm', {
  extend: 'Ext.form.Panel',
  requires: [
    'NX.I18n'
  ],
  alias: 'widget.nx-settingsform',
  ui: 'nx-subsection',
  frame: true,

  /**
   * Set trackResetOnLoad by default.
   *
   * @private
   */
  constructor: function (config) {
    config = config || {};
    config.trackResetOnLoad = true;
    this.callParent([config]);
  },

  /**
   * @cfg {boolean} [settingsForm=true] Marker that we have a settings form
   * ({NX.controller.SettingsForm} controller kicks in)
   */
  settingsForm: true,

  /**
   * @cfg {boolean} [settingsFormSubmit=true] True if settings form should be submitted automatically when 'submit'
   * button is clicked. Set this to false if custom processing is needed.
   */
  settingsFormSubmit: true,

  /**
   * @cfg {boolean} [settingsFormSubmitOnEnter=false] True if form should be submitted on Enter.
   */
  settingsFormSubmitOnEnter: false,

  /**
   * @cfg {String/Function} Text to be used when displaying submit/load messages. If is a function it will be called
   * with submit/load response data as parameter and it should return a String.
   * If text contains "${action}", it will be replaced with performed action.
   */
  settingsFormSuccessMessage: undefined,

  /**
   * @cfg {String} [settingsFormLoadMessage] Text to be used as mask while loading data.
   */
  settingsFormLoadMessage: undefined,

  /**
   * @cfg {String} [settingsFormSubmitMessage] Text to be used as mask while submitting data.
   */
  settingsFormSubmitMessage: undefined,

  /**
   * @cfg {NX.util.condition.Condition} The condition to be satisfied in order for this form to be editable.
   */
  editableCondition: undefined,

  /**
   * @cfg {String} Optional text to be shown in case that form is not editable (condition is not satisfied).
   */
  editableMarker: undefined,

  waitMsgTarget: true,

  defaults: {
    xtype: 'textfield',
    allowBlank: false
  },

  buttonAlign: 'left',

  buttons: 'defaultButtons',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    if (me.buttons === 'defaultButtons') {
      me.buttons = [
        {
          text: NX.I18n.get('SettingsForm_Save_Button'),
          action: 'save',
          ui: 'nx-primary',
          bindToEnter: false
        },
        {
          text: NX.I18n.get('SettingsForm_Discard_Button'),
          action: 'discard'
        }
      ];
    }

    Ext.applyIf(me, {
      settingsFormLoadMessage: NX.I18n.get('SettingsForm_Load_Message'),
      settingsFormSubmitMessage: NX.I18n.get('SettingsForm_Submit_Message')
    });

    if (me.buttons && Ext.isArray(me.buttons) && me.buttons[0] && Ext.isDefined(me.buttons[0].bindToEnter)) {
      me.buttons[0].bindToEnter = me.settingsFormSubmitOnEnter;
    }

    me.callParent();
  },

  /**
   * Fires 'recordloaded' after record was loaded.
   *
   * @override
   */
  loadRecord: function (record) {
    var me = this;

    me.fireEvent('beforerecordloaded', me, record);
    me.callParent(arguments);
    me.fireEvent('recordloaded', me, record);
    me.isValid();
  },

  /**
   * Sets the read only state for all items passed in
   *
   * @public
   * @param {boolean} editable
   * @param itemsToModify
   */
  setItemsEditable: function (isEditable, itemsToModify) {
    if (isEditable) {
          Ext.Array.each(itemsToModify, function (item) {
            var enable = true,
                form;

            if (item.resetEditable) {
              if (Ext.isFunction(item.setReadOnly)) {
                item.setReadOnly(false);
              }
              else {
                if (Ext.isDefined(item.resetFormBind)) {
                  item.formBind = item.resetFormBind;
                }
                if (item.formBind) {
                  form = item.up('form');
                  if (form && !form.isValid()) {
                    enable = false;
                  }
                }
                if (enable) {
                  item.enable();
                }
              }
            }
            if (Ext.isDefined(item.resetEditable)) {
              delete item.resetEditable;
              delete item.resetFormBind;
            }
          });
        }
        else {
          Ext.Array.each(itemsToModify, function (item) {
            if (Ext.isFunction(item.setReadOnly)) {
              if (item.resetEditable !== false && !item.readOnly) {
                item.setReadOnly(true);
                item.resetEditable = true;
              }
            }
            else {
              if (item.resetEditable !== false) {
                item.disable();
                item.resetFormBind = item.formBind;
                delete item.formBind;
                item.resetEditable = true;
              }
            }
          });
        }
    },

  /**
   * Sets the read only state for all fields of this form.
   *
   * @public
   * @param {boolean} editable
   */
  setEditable: function (editable) {
    var me = this,
        itemsToDisable,
        bottomBar;

    if (me.isDestroying) {
      return;
    }

    itemsToDisable = me.getChildItemsToDisable().filter(function(item){
      return item.xtype !== 'nx-coreui-formfield-settingsfieldset';
    });

    me.setItemsEditable(editable, itemsToDisable);

    bottomBar = me.getDockedItems('toolbar[dock="bottom"]')[0];
    if (bottomBar) {
      if (bottomBar.editableMarker) {
        bottomBar.remove(bottomBar.editableMarker);
        bottomBar.editableMarker = undefined;
      }

      if (!editable && me.editableMarker) {
        bottomBar.editableMarker = Ext.widget({
          xtype: 'label',
          text: me.editableMarker,
          cls: 'nx-form-important-msg'
        });
        bottomBar.add(bottomBar.editableMarker);
      }
    }
  }

});
