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
 * File created window.
 *
 * @since 2.8
 */
NX.define('Nexus.wonderland.view.FileCreated', {
  extend: 'Ext.Window',
  xtype: 'nx-wonderland-view-file-created',
  mixins: [
    'Nexus.LogAwareMixin'
  ],
  requires: [
    'Nexus.wonderland.Icons',
    'Nexus.wonderland.AuthenticateButton'
  ],

  /**
   * @cfg Icon to show
   */
  fileIcon: undefined,

  /**
   * @cfg Type of file shown
   */
  fileType: undefined,

  /**
   * @cfg Download button id
   */
  downloadButtonId: undefined,

  cls: 'nx-wonderland-view-file-created',

  autoShow: true,
  constrain: true,
  resizable: false,
  width: 500,
  border: false,
  modal: true,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    Ext.apply(me, {
      title: me.title || me.fileType + ' Created',
      items: [
        {
          xtype: 'component',
          border: false,
          cls: 'nx-wonderland-view-file-created-description',
          html: me.fileIcon.img + '<div>' + me.fileType + ' has been created.' +
              '<br/><br/>You can reference this file on the filesystem or download the file from your browser.</div>'
        },
        {
          xtype: 'form',
          itemId: 'form',
          border: false,
          monitorValid: true,
          layoutConfig: {
            labelSeparator: '',
            labelWidth: 40
          },
          labelAlign: 'right',
          items: [
            {
              xtype: 'textfield',
              fieldLabel: 'Name',
              helpText: me.fileType + ' file name',
              name: 'name',
              readOnly: true,
              grow: true,
              style: {
                border: 0,
                background: 'none'
              }
            },
            {
              xtype: 'textfield',
              fieldLabel: 'Size',
              helpText: 'Size of ' + me.fileType + ' file in bytes',  // FIXME: Would like to render in bytes/kilobytes/megabytes
              name: 'size',
              readOnly: true,
              grow: true,
              style: {
                border: 0,
                background: 'none'
              }
            },
            {
              xtype: 'textfield',
              fieldLabel: 'Path',
              helpText: me.fileType + ' file location',
              name: 'file',
              readOnly: true,
              selectOnFocus: true,
              anchor: '96%'
            },
            {
              xtype: 'hidden',
              name: 'truncated'
            }
          ],

          buttonAlign: 'right',
          buttons: [
            { text: 'Close', xtype: 'link-button', handler: me.close, scope: me },
            { text: 'Download', xtype: 'nx-wonderland-button-authenticate', formBind: true, id: me.downloadButtonId }
          ]
        }
      ],

      keys: [
        {
          // Download on ENTER
          key: Ext.EventObject.ENTER,
          scope: me,
          fn: function () {
            var btn = Ext.getCmp(me.downloadButtonId);
            btn.fireEvent('click', btn);
          }
        },
        {
          // Close on ESC
          key: Ext.EventObject.ESC,
          scope: me,
          fn: me.close
        }
      ]
    });

    Nexus.wonderland.view.FileCreated.superclass.initComponent.apply(me, arguments);
  },

  /**
   * Set form values.
   *
   * @public
   */
  setValues: function (values) {
    this.down('form').getForm().setValues(values);
  },

  /**
   * Get form values.
   *
   * @public
   */
  getValues: function () {
    return this.down('form').getForm().getValues();
  }

});