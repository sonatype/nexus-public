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
/*global Ext*/

/**
 * Contains various buttons to execute actions for development/testing.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.Tests', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-dev-tests',

  title: 'Tests',

  layout: {
    type: 'vbox',
    padding: 4,
    defaultMargins: {top: 0, right: 0, bottom: 4, left: 0}
  },

  items: [
    { xtype: 'button', text: 'clear local state', action: 'clearLocalState' },
    { xtype: 'button', text: 'javascript error', action: 'testError' },
    { xtype: 'button', text: 'ext error', action: 'testExtError' },
    { xtype: 'button', text: 'message types', action: 'testMessages' },
    { xtype: 'button', text: 'toggle unsupported browser', action: 'toggleUnsupportedBrowser'},
    { xtype: 'button', text: 'show quorum warning', action: 'showQuorumWarning'}
  ]
});
