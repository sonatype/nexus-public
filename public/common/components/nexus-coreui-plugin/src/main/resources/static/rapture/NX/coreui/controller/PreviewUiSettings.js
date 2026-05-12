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
 * Preview UI Settings controller. Registers the feature in the admin menu.
 */
Ext.define('NX.coreui.controller.PreviewUiSettings', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.I18n',
    'NX.State'
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      // Preserve the legacy bookmark token `admin/system/previewui` for existing routes and deep links.
      // TODO: Remove this ExtJS feature registration once the Classic UI is fully retired.
      path: '/System/Preview UI',
      text: NX.I18n.get('PreviewUiSettings_Text'),
      description: NX.I18n.get('PreviewUiSettings_Description'),
      view: {xtype: 'nx-coreui-react-main-container'},
      iconCls: 'x-fa fa-eye',
      visible: function() {
        return NX.State.getValue('nexus.previewui.enabled', false);
      },
      weight: 10
    });
  }
});

