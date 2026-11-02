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

/**
 * Open VSX search contribution.
 *
 * Registers the format's saved search filter so cached extensions can be found alongside other
 * formats.
 */
Ext.define('NX.coreui.controller.SearchOpenVSX', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  init: function() {
    var me = this,
        search = me.getController('NX.coreui.controller.Search');

    search.registerCriteria([
      {
        id: 'attributes.openvsx.publisher',
        group: NX.I18n.get('SearchOpenVSX_Group'),
        config: {
          format: 'openvsx',
          fieldLabel: NX.I18n.get('SearchOpenVSX_Publisher_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.openvsx.targetPlatform',
        group: NX.I18n.get('SearchOpenVSX_Group'),
        config: {
          format: 'openvsx',
          fieldLabel: NX.I18n.get('SearchOpenVSX_TargetPlatform_FieldLabel'),
          width: 250
        }
      }
    ], me);

    search.registerFilter({
      id: 'openvsx',
      name: 'openvsx',
      text: NX.I18n.get('SearchOpenVSX_Text'),
      description: NX.I18n.get('SearchOpenVSX_Description'),
      readOnly: true,
      criterias: [
        {id: 'format', value: 'openvsx', hidden: true},
        {id: 'attributes.openvsx.publisher'},
        {id: 'attributes.openvsx.targetPlatform'},
        {id: 'name.raw'},
        {id: 'version'}
      ]
    }, me);
  }
});
