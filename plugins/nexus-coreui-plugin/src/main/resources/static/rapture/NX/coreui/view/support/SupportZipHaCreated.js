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
/*global Ext, NX*/

/**
 * Support ZIP created window for HA-C nodes.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.view.support.SupportZipHaCreated', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-support-supportziphacreated',
  requires: [
    'NX.Icons',
    'NX.I18n'
  ],

  truncatedWarning: {
    xtype: 'panel',
    layout: {
      type: 'hbox',
      align: 'middle'
    },
    style: {
      marginBottom: '10px'
    },
    items: [
      { xtype: 'component', html: NX.Icons.img('supportzip-truncated', 'x32') },
      { xtype: 'component', html: NX.I18n.get('Support_SupportZipCreated_Truncated_Text'),
        margin: '0 0 0 5'
      }
    ],
    hidden: true
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.setWidth(NX.view.ModalDialog.LARGE_MODAL);

    me.fileIcon = NX.Icons.img('supportzip-zip', 'x32');
    me.fileType = NX.I18n.get('Support_SupportZipCreated_FileType_Text');

    Ext.apply(me, {
      title: me.title || me.fileType + ' Created',
      items: [
        {
          xtype: 'tabpanel',
          bodyPadding: 10
        }
      ]
    });

    me.callParent();
  },

  /**
   * @public
   */
  addNode: function(nodeSupportZip) {
    var me = this,
        panel = Ext.create({
          xtype: 'panel',
          title: Ext.String.ellipsis(nodeSupportZip.nodeAlias, 15, false),
          items: []
        }),
        form = nodeSupportZip.file ? me.addFileForm(panel) : me.addErrorForm(panel, nodeSupportZip);

    me.addNodeIdField(form);

    if (nodeSupportZip.truncated) {
      form.insert(1, Ext.merge({}, me.truncatedWarning)).show();
    }

    form.getForm().setValues(nodeSupportZip);
    me.down('tabpanel').add(panel);
  },

  /**
   * @private
   */
  addNodeIdField: function (form) {
    form.insert(1, {
      xtype: 'textfield',
      name: 'nodeAlias',
      fieldLabel: NX.I18n.get('Support_SupportZipCreated_Node_FieldLabel'),
      readOnly: true
    });
  },

  /**
   * @private
   */
  addFileForm: function (panel) {
    var me = this;

    return panel.add({
      xtype: 'nx-coreui-support-filecreatedform',
      fileIcon: me.fileIcon,
      fileType: me.fileType
    });
  },

  /**
   * @private
   */
  addErrorForm: function(panel, supportZip) {
    var form = panel.add(Ext.widget('form', {
          items: [
            {
              xtype: 'panel',
              layout: 'hbox',
              style: {
                marginBottom: '10px'
              },
              items: [
                {
                  xtype: 'component',
                  html: NX.Icons.img('supportzip-error', 'x32')
                },
                {
                  xtype: 'component',
                  html: NX.I18n.get('Support_SupportZipCreated_Node_Failed_Message'),
                  margin: '0 0 0 5'
                }
              ]
            }
          ]
        }));

    if (supportZip.detail) {
      form.add({
        xtype: 'textfield',
        name: 'detail',
        fieldLabel: NX.I18n.get('Support_SupportZipCreated_Node_Details_FieldLabel'),
        helpText: NX.I18n.get('Support_SupportZipCreated_Node_Details_HelpText'),
        readOnly: true
      });
    }

    return form;
  }
});
