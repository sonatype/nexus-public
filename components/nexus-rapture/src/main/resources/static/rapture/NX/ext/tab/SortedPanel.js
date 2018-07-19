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
 * A tab panel that sorts tabs based on weight and title and not show the tab bar if only one tab.
 *
 * @since 3.0
 */
Ext.define('NX.ext.tab.SortedPanel', {
  extend: 'Ext.tab.Panel',
  alias: 'widget.nx-sorted-tabpanel',

  listeners: {
    /**
     * @private
     * Reorders tabs sorting them by weight / title.
     * Show the tab bar in case of more then one tab.
     *
     * @param me this tab panel
     * @param component added tab
     */
    add: function (me, component) {
      var thisTitle = component.title || '',
          thisWeight = component.weight || 1000,
          position = 0;

      me.suspendEvents();
      me.remove(component, false);

      me.items.each(function (item) {
        var thatTitle = item.title || '',
            thatWeight = item.weight || 1000;

        if (thisWeight < thatWeight
            || (thisWeight === thatWeight && thisTitle < thatTitle)) {
          return false;
        }
        position++;
        return true;
      });

      me.insert(position, component);
      me.resumeEvents();
    }
  },

  // FIXME: This doesn't belong here, this is styling treatment for master/detail tabs only
  /**
   * @override
   */
  onAdd: function(item, index) {
    item.tabConfig = item.tabConfig || {};
    Ext.applyIf(item.tabConfig, {
      // HACK: force tabs to follow scss style for borders
      border: null
    });

    this.callParent([item, index]);
  }
});
