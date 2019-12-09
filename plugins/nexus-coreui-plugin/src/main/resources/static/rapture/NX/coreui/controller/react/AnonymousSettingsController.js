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
 * Anonymous Security Settings controller.
 *
 * @since 3.next
 */
Ext.define('NX.coreui.controller.react.AnonymousSettingsController', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Permissions',
    'NX.I18n'
  ],

  views: [
    'react.MainContainer'
  ],

  refs: [
    {
      ref: 'mainContainer',
      selector: 'nx-coreui-react-main-container'
    }
  ],

  listen: {
    controller: {
      '#Refresh': {
        refresh: 'refresh'
      }
    }
  },

  /**
   * @override
   */
  init: function() {
    this.getApplication().getFeaturesController().registerFeature({
      mode: 'admin',
      path: '/Security/Anonymous',
      text: NX.I18n.get('AnonymousSettings_Text'),
      description: NX.I18n.get('AnonymousSettings_Description'),
      view: {
        xtype: 'nx-coreui-react-main-container',
        reactView: 'AnonymousSettings'
      },
      iconConfig: {
        file: 'user_silhouette.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        return NX.Permissions.check('nexus:settings:read');
      }
    }, this);

    this.callParent();
  },

  refresh: function() {
    this.getMainContainer().refresh();
  }

});
