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
 * Analyze application window.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.view.component.AnalyzeApplicationWindow', {
  extend: 'NX.view.ModalDialog',
  alias: 'widget.nx-coreui-component-analyze-window',
  requires: [
    'NX.I18n'
  ],

  /**
   * Configuration
   */
  component: undefined,

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.setWidth(NX.view.ModalDialog.LARGE_MODAL);

    me.items = [
      {
        xtype: 'nx-coreui-react-main-container',
        reactView: window.ReactComponents.AnalyzeApplication,
        reactViewProps: {
          componentModel: me.component,
          rerender: function() {
            me.updateLayout();
            me.center();
          },
          onClose: function() {
            me.close();
          }
        }
      }
    ];

    me.callParent();
  },
});
