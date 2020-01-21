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
/*global Ext, NX, REACT_COMPONENTS, react, ReactDOM */

/**
 * React Main Container
 *
 * @since 3.21
 */
Ext.define('NX.coreui.view.react.MainContainer', {
  extend: 'Ext.Component',
  alias: 'widget.nx-coreui-react-main-container',

  reactView: undefined,

  scrollable: true,

  listeners: {
    afterrender: 'initReactView',
    beforedestroy: 'destroyReactView'
  },

  initReactView: function() {
    var reactElement = react.createElement(this.reactView);
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
  }
});
