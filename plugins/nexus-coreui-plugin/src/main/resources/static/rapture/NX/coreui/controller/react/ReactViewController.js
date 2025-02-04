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
 * Anonymous Security Settings controller.
 *
 * @since 3.21
 */
Ext.define('NX.coreui.controller.react.ReactViewController', {
  extend: 'NX.app.Controller',

  views: [
    'react.MainContainer'
  ],

  refs: [
    {
      ref: 'reactMainContainer',
      selector: 'nx-coreui-react-main-container'
    },
    {
      ref: 'breadcrumb',
      selector: 'nx-breadcrumb'
    },
  ],

  listen: {
    controller: {
      '#Refresh': {
        refresh: 'refresh'
      },
      '#Menu': {
        refresh: 'refresh'
      },
      '#State': {
        changed: function() {
          if (Ext.dom.Query.select('div.nxrm-usage-center')[0]) {
            this.refresh();
          }
        }
      },
    },
    component: {
      'nx-coreui-react-main-container': {
        render: function() {
          this.getBreadcrumb().hide();
        },
        destroy: function() {
          this.getBreadcrumb().show();
        }
      }
    }
  },

  refresh: function() {
    if (this.getReactMainContainer()) {
      this.getReactMainContainer().refresh();
    }
  }
});
