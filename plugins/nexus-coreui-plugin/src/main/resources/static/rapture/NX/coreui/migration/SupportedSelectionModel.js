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
 * Supported-repository selection model.
 *
 * Assumes records are {@link NX.coreui.migration.RepositoryModel}.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.migration.SupportedSelectionModel', {
  extend: 'Ext.selection.CheckboxModel',

  mode: 'SIMPLE',

  // note this only prevents use of right-mouse click when there is already some selection :-(
  ignoreRightMouseSelection: true,

  constructor: function () {
    var me = this;
    me.callParent(arguments);

    me.on('beforeselect', function(sm, record, i, opts) {
      // only allow selection of supported records
      return record.get('supported');
    });
  },

  /**
   * @override
   */
  selectAll: function (suppressEvent) {
    var me = this,
        selections = me.store.getRange(),
        i = 0,
        len = selections.length,
        lenMinusUnsupported = len,
        start = me.getSelection().length;

    // Subtract the number of unsupported repositories from the length
    selections.forEach(function (e) {
      if (!e.get('supported')) {
        --lenMinusUnsupported;
      }
    });

    // If the corrected length is different from the starting length, select all
    if (start !== lenMinusUnsupported) {
      me.suspendChanges();
      for (i = 0; i < len; i++) {
        if (selections[i].get('supported')) {
          me.doSelect(selections[i], true, suppressEvent);
        }
      }
      me.resumeChanges();
      // fire selection change only if the number of selections differs
      if (!suppressEvent) {
        me.maybeFireSelectionChange(me.getSelection().length !== start);
      }
    }
    else {
      // Otherwise, deselect all
      me.deselectAll(suppressEvent);
    }
  }
});
