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
 * Support ZIP created window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.support.SupportZipCreated', {
  extend: 'NX.coreui.view.support.FileCreated',
  alias: 'widget.nx-coreui-support-supportzipcreated',
  requires: [
      'NX.Icons',
      'NX.I18n'
  ],

  fileType: 'Support ZIP',

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.fileIcon = NX.Icons.img('supportzip-zip', 'x32');

    me.callParent(arguments);

    me.truncatedWarning = Ext.widget({
      xtype: 'panel',
      layout: {
        type: 'hbox',
        align: 'middle'
      },
      style: {
        marginBottom: '10px'
      },
      // TODO Style
      items: [
        { xtype: 'component', html: NX.Icons.img('supportzip-truncated', 'x32') },
        { xtype: 'component', html: NX.I18n.get('Support_SupportZipCreated_Truncated_Text'),
          margin: '0 0 0 5'
        }
      ],
      hidden: true
    });

    me.items.get(0).items.insert(1, me.truncatedWarning);
  },

  /**
   * Set form values.
   *
   * @public
   */
  setValues: function (values) {
    var me = this;

    me.callParent(arguments);

    // if truncated show the warning
    if (values.truncated) {
      me.truncatedWarning.show();
    }
  }
});
