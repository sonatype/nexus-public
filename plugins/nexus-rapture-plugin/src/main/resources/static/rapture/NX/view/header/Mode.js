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
 * Container for mode buttons. Adds a caret to the bottom of the button.
 *
 * @since 3.0
 */
Ext.define('NX.view.header.Mode', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-header-mode',

  layout: 'absolute',

  /**
   * Add a caret to the mode button.
   *
   * @override
   */
  initComponent: function() {
    this.callParent(arguments);

    // Add caret
    this.add({
      xtype: 'container',
      cls: 'nx-caret',
      width: 0,
      height: 0,
      x: 14,
      y: 34
    });
  }
});
