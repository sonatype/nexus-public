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
/*global define*/
/**
 * DEPRECATED This does not seem to be working anymore, need to check whether plugins use this and then remove.
 */
define('Nexus/grid/checkcolumn', ['extjs', 'nexus'], function(Ext, Nexus) {
  Ext.namespace('Nexus.grid');

  Nexus.grid.CheckColumn = function(config) {
    Ext.apply(this, config);
    if (!this.id) {
      this.id = Ext.id();
    }
    this.renderer = this.renderer.createDelegate(this);
  };

  Nexus.grid.CheckColumn.prototype = {
    init : function(grid) {
      this.grid = grid;
      this.grid.on('render', function() {
        var view = this.grid.getView();
        view.mainBody.on('mousedown', this.onMouseDown, this);
      }, this);
    },

    onMouseDown : function(e, t) {
      if (t.className && t.className.indexOf('x-grid3-cc-' + this.id) !== -1) {
        e.stopEvent();
        var
              index = this.grid.getView().findRowIndex(t),
              record = this.grid.store.getAt(index);

        record.set(this.dataIndex, !record.data[this.dataIndex]);
      }
    },

    renderer : function(v, p, record) {
      p.css += ' x-grid3-check-col-td';
      return '<div class="x-grid3-check-col' + (v ? '-on' : '') + ' x-grid3-cc-' + this.id + '">&#160;</div>';
    }
  };

  // legacy: use Ext namespace as well
  Ext.grid.CheckColumn = Nexus.grid.CheckColumn;
});
