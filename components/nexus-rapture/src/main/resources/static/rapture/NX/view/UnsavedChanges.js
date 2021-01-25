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
/*global Ext*/

/**
 * Unsaved changes window.
 *
 * @since 3.0
 */
Ext.define('NX.view.UnsavedChanges', {
  extend: 'NX.view.ModalDialog',
  requires: [
    'NX.I18n'
  ],
  alias: 'widget.nx-unsaved-changes',

  /**
   * Panel with content to be saved.
   *
   * @public
   */
  content: null,

  /**
   * Function to call if content is to be discarded.
   *
   * @public
   */
  callback: Ext.emptyFn,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.title = NX.I18n.get('UnsavedChanges_Title');

    me.setWidth(NX.view.ModalDialog.SMALL_MODAL);

    Ext.apply(me, {
      items: {
        xtype: 'panel',
        ui: 'nx-inset',
        html: NX.I18n.get('UnsavedChanges_Help_HTML'),
        buttonAlign: 'left',
        buttons: [
          {
            text: NX.I18n.get('UnsavedChanges_Discard_Button'),
            ui: 'nx-primary',
            itemId: 'nx-discard',
            handler: function () {
              // Discard changes and load new content
              if (me.content) {
                me.content.resetUnsavedChangesFlag(true);
              }
              me.callback();
              me.close();
            }
          },
          { text: NX.I18n.get('UnsavedChanges_Back_Button'), handler: me.close, scope: me }
        ]
      }
    });

    me.on({
      resize: function() {
        me.down('#nx-discard').focus();
      },
      single: true
    });

    me.callParent();
  }

});
