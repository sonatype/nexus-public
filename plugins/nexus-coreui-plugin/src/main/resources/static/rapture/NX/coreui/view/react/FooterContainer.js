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
  requires: [
    'NX.State',
    'NX.Permissions'
  ],

  alias: 'widget.nx-coreui-react-footer-container',

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

    const isCircuitBreakerEnabled = NX.State.getValue('nexus.circuitb.enabled');
    const contentUsageEvaluationResults = NX.State.getValue('contentUsageEvaluationResult');
    const isHa = NX.State.getValue('nexus.datastore.clustered.enabled');
    const isProEdition = NX.State.getEdition() === 'PRO';
    const canViewMetrics = NX.Permissions.check('nexus:metrics:read');
    const softLimitCards = contentUsageEvaluationResults.filter(function(m) {
      return m.limitLevel === 'SOFT_LIMIT';
    });

    this.setVisible(!isProEdition && isCircuitBreakerEnabled && !isHa && softLimitCards.length > 0 && canViewMetrics);
    this.updateLayout();
  },

  dismissAlert: function(){
    this.getEl().child('div.nx-alert--warning').setDisplayed(false);
    this.updateLayout();
  },
});
