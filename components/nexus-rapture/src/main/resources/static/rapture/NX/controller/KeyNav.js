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
 * KeyNav controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.KeyNav', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.util.KeyNav'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      component: {
        'form button[bindToEnter=true]': {
          afterrender: me.installEnterKey
        }
      }
    });

    me.disableBackspaceNav();
  },

  /**
   * Install a key nav that will trigger click on any form buttons marked with "bindToEnter: true",
   * (usually submit button) on ENTER.
   *
   * @private
   */
  installEnterKey: function (button) {
    var form = button.up('form');

    button.keyNav = Ext.create('Ext.util.KeyNav', form.el, {
      enter: function () {
        if (!button.isDisabled()) {
          button.fireEvent('click', button);
        }
      }
    });
  },

  /**
   * Disable backspace as a means for navigating back. Allow backspace when an enabled
   * input field has focus.
   *
   * @private
   */
  disableBackspaceNav: function() {
    var parent = Ext.isIE ? document : window;
    Ext.EventManager.on(parent, 'keydown', function (e, focused) {
      // Check for least-likely to most-likely conditions in order to avoid costly evaluations - fail fast to increase performance
      var isBackspace = e.getKey() === e.BACKSPACE;
      if ( isBackspace && !isBackspaceAllowed() ) {
        e.stopEvent();
      }

      /**
       * Returns true if a backspace should be allowed.
       *
       * @inner
       * @private
       * @returns {boolean}
       */
      function isBackspaceAllowed() {
        // isEditable is false if focused.readOnly is undefined; this traps the case where no field has focus,
        //  and the short-circuit avoids costly field checking
        var isEditable = (focused.readOnly !== undefined && !focused.readOnly),
            isEnabled = !focused.disabled,
            isTypingAllowed = isEditable && isEnabled;

        return isTypingAllowed && isFieldAllowed();
      }

      /**
       * Returns true if the field should allow backspaces.
       *
       * ExtJS use the role attribute to map to multiple UI field types:
       *  textbox ==> normal text field (`input[type=text]`); multi-line text area (`textarea`)
       *  spinbutton ==> normal text field ('input[type=text]`) with accompanying up/down arrows for selecting numeric values
       *  combobox ==> text field that allows typing in addition to list selection (`input[type=text]`);
       *      text field that only allows list selection (`input[type=text,readOnly=readOnly]`)
       *
       * Field types disallowed by exclusion:
       *  checkbox ==> rendered by ExtJS as a button with accompanying label (`input[type=button]`)
       *
       * @inner
       * @private
       * @returns {boolean}
       */
      function isFieldAllowed() {
        var roleAttribute = focused.attributes["role"],
            role = roleAttribute && roleAttribute.value,
            rolesAllowed = ['textbox', 'spinbutton', 'combobox'],
            rePattern = '^' + rolesAllowed.join('|') + '$',
            re = new RegExp(rePattern, 'i');

        return re.test(role);
      }
    });
  }

});