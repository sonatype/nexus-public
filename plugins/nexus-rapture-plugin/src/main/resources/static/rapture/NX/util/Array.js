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
 * Array related utils.
 *
 * @since 3.0
 */
Ext.define('NX.util.Array', {
  singleton: true,
  requires: [
    'Ext.Array'
  ],

  /**
   * Check if one array contains all elements from another.
   *
   * @public
   * @param {Array} array1
   * @param {Array} array2
   * @return {boolean} true if array1 contains all elements from array2.
   */
  containsAll: function(array1, array2) {
    var i;
    for (i=0; i<array2.length; i++) {
      if (!Ext.Array.contains(array1, array2[i])) {
        return false;
      }
    }
    return true;
  }
});
