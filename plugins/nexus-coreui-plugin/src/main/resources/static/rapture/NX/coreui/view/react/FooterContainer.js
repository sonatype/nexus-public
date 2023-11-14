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
/*global Ext, NX, REACT_COMPONENTS, react, ReactDOM */

/**
 * React Footer Container
 *
 * @since 3.21
 */
Ext.define('NX.coreui.view.react.FooterContainer', {
  extend: 'Ext.Component',
  alias: 'widget.nx-coreui-react-footer-container',

  requires: [
    'NX.Permissions',
    'NX.State'
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
    const canViewMetrics = NX.Permissions.check('nexus:metrics:read');
    const isHa = NX.State.getValue('nexus.datastore.clustered.enabled');
    const isCircuitBreakerEnabled = NX.State.getValue('nexus.circuitb.enabled');
    const isOss = NX.State.getEdition() === 'OSS';
    const metrics = NX.State.getValue('contentUsageEvaluationResult');
    const hardLimitCards = metrics.filter(function(m) {
      return m.limitLevel === 'HARD_LIMIT';
    });
    const softLimitCards = metrics.filter(function(m) {
      return m.limitLevel === 'SOFT_LIMIT';
    });

    this.setVisible(canViewMetrics && !isHa && isCircuitBreakerEnabled && isOss && (hardLimitCards.length > 0 || softLimitCards.length > 0));
    this.updateLayout();
  },

  dismissAlert: function(){
    this.getEl().child('div.nx-alert--warning').setDisplayed(false);
    this.updateLayout();
  },
});
