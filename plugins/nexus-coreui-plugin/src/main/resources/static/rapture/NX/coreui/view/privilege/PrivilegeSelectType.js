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
 * Select privilege type window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.privilege.PrivilegeSelectType', {
  extend: 'NX.view.drilldown.Master',
  alias: 'widget.nx-coreui-privilege-selecttype',
  requires: [
    'NX.I18n'
  ],

  cls: 'nx-hr',

  initComponent: function() {
    var me = this;

    Ext.apply(me, {

      store: 'PrivilegeType',

      columns: [
        {
          xtype: 'nx-iconcolumn',
          dataIndex: 'id',
          width: 36,
          iconVariant: 'x16',
          iconNamePrefix: 'privilege-'
        },
        {
          header: NX.I18n.get('Privilege_PrivilegeSelectType_Type_Header'),
          dataIndex: 'name',
          flex: 1
        }
      ]
    });

    me.callParent();
  }

});
