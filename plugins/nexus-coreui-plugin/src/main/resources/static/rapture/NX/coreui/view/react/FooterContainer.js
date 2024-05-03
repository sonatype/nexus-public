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
 * React Footer Container
 *
 * @since 3.67
 */
Ext.define('NX.coreui.view.react.FooterContainer', {
  extend: 'Ext.Component',
  alias: 'widget.nx-coreui-react-footer-container',

  requires: [
    'NX.State',
    'NX.Permissions'
  ],

  reactView: undefined,

  reactViewProps: null,

  id: 'nxrm-react-footer-container',
  cls: 'nxrm-react-footer-container',

  listeners: {
    afterrender: 'initReactView',
    beforedestroy: 'destroyReactView'
  },

  initReactView: function() {
    const reactElement = react.createElement(this.reactView, {
      onClose: function() {
        this.dismissAlert()
      }.bind(this)
    }, null);
    ReactDOM.render(reactElement, this.getEl().dom);
  },

  destroyReactView: function() {
    if (this.reactView) {
      ReactDOM.unmountComponentAtNode(this.getEl().dom);
    }
  },

  refresh: function() {
    this.destroyReactView();
    this.initReactView();
    this.maybeSetVisible();
  },

  maybeSetVisible: function() {
    const user = NX.State.getUser();
    const isAdmin = user ? user.administrator : false;
    const isHa = NX.State.getValue('nexus.datastore.clustered.enabled');
    const metrics = NX.State.getValue('contentUsageEvaluationResult', []);

    this.setVisible(isAdmin && !isHa && (metrics.length > 0));
    this.updateLayout();
  },

  dismissAlert: function() {
    this.setVisible(false);
    this.updateLayout();
  }
});
