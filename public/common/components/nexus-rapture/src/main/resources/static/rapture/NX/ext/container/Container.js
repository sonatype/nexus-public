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
/*global Ext*/

/**
 * Override ExtJS Container to protect against null items during component destruction.
 *
 */
Ext.define('NX.ext.container.Container', {
  override: 'Ext.container.Container',

  /**
   * @override
   * Protect against null items collection during destruction lifecycle.
   * This is called by query(), enable(), disable() and other traversal methods.
   *
   * Original ExtJS implementation:
   *   var me = this, items = me.items.items, len = items.length, i = 0, item, result = [];
   *   for (; i < len; i++) { item = items[i]; result[result.length] = item; ... }
   *   if (me.floatingItems) { items = me.floatingItems.items; ... }
   *   return result;
   */
  getRefItems: function(deep) {
    var me = this,
        items,
        len,
        i = 0,
        item,
        result = [];

    // Guard against component being destroyed or items collection being null
    if (me.isDestroying || me.destroying || !me.items) {
      return result;
    }

    // Safe access to items.items - we've already verified me.items exists
    items = me.items.items;
    if (items) {
      len = items.length;
      for (; i < len; i++) {
        item = items[i];
        result[result.length] = item;
        if (deep && item && item.getRefItems) {
          result.push.apply(result, item.getRefItems(true));
        }
      }
    }

    if (me.floatingItems && me.floatingItems.items) {
      items = me.floatingItems.items;
      len = items.length;
      for (i = 0; i < len; i++) {
        item = items[i];
        result[result.length] = item;
        if (deep && item && item.getRefItems) {
          result.push.apply(result, item.getRefItems(true));
        }
      }
    }

    return result;
  }
});
