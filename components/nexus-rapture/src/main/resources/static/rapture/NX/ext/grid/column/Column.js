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
 * @override Ext.grid.column.Column
 *
 * Allow grid columns to toggle from ASC to DESC and back to no sort if allowClearSort is set on the gridpanel.
 *
 * @since 3.14
 */
Ext.define('NX.ext.grid.column.Column', {
  override: 'Ext.grid.column.Column',

  /**
   * @override
   */
  toggleSortState: function() {
    var view = this.up('grid');
    if (view && view.allowClearSort && this.isSortable() && this.sortState === 'DESC') {
      view.getStore().sorters.clear();
      view.getStore().load();
    }
    else {
      this.callParent();
    }
  }
});
