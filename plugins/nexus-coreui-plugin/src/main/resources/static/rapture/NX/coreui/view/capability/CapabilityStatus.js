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
 * Capability "Status" panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.capability.CapabilityStatus', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-capability-status',
  ui: 'nx-inset',
  requires: [
    'NX.I18n'
  ],

  html: '',

  /**
   * @public
   * Shows capability status text.
   * @param {String} text status text
   */
  showStatus: function (text) {
    this.html = text || NX.I18n.get('Capability_CapabilityStatus_EmptyText');
    if (this.body) {
      this.update(this.html);
    }
  }

});
