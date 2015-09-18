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
 * Permissions helper.
 *
 * @since 3.0
 */
Ext.define('NX.Permissions', {
  singleton: true,
  requires: [
    'NX.Assert',
    'NX.util.Array'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * Map between permission and permitted boolean value.
   *
   * @private
   */
  permissions: undefined,

  /**
   * Map of permission to implied calculated value.
   *
   * @private
   */
  impliedCache: undefined,

  /**
   * @public
   * @returns {boolean} True, if permissions had been set (loaded from server)
   */
  available: function() {
    return Ext.isDefined(this.permissions);
  },

  /**
   * Sets permissions.
   *
   * @public
   * @param {object} permissions
   */
  setPermissions: function(permissions) {
    var me = this;

    // defensive copy
    me.permissions = Ext.clone(permissions);

    // reset implied cache
    me.impliedCache = {};

    //<if debug>
    me.logDebug('Permissions installed');
    //</if>
  },

  /**
   * Resets all permissions.
   *
   * @public
   */
  resetPermissions: function() {
    var me = this;

    //<if debug>
    me.logDebug('Resetting permissions');
    //</if>

    delete me.permissions;
    delete me.impliedCache;
  },

  /**
   * Check if the current subject has the expected permission.
   *
   * @public
   * @param {String} expectedPermission
   * @returns {boolean} True if user is authorized for expected permission.
   */
  check: function(expectedPermission) {
    var me = this,
        hasPermission = false;

    //<if assert>
    NX.Assert.assert(expectedPermission.search('undefined') === -1, 'Invalid permission check:', expectedPermission);
    //</if>

    // short-circuit if permissions are not installed
    if (!me.available()) {
      return false;
    }

    // check for exact match first
    if (me.permissions[expectedPermission] !== undefined) {
      return me.permissions[expectedPermission];
    }

    // or use cached implied if we know it
    if (me.impliedCache[expectedPermission] !== undefined) {
      //<if debug>
      me.logTrace('Using cached implied permission:', expectedPermission, 'is:', me.impliedCache[expectedPermission]);
      //</if>
      hasPermission = me.impliedCache[expectedPermission];
    }
    else {
      // otherwise calculate if permission is implied or not
      Ext.Object.each(me.permissions, function (permission, permitted) {
        if (permitted && me.implies(permission, expectedPermission)) {
          hasPermission = true;
          return false; // break
        }
        return true; // continue
      });

      // cache calculated implied
      me.impliedCache[expectedPermission] = hasPermission;

      //<if debug>
      me.logTrace('Cached implied permission:', expectedPermission, 'is:', hasPermission);
      //</if>
    }

    return hasPermission;
  },

  /**
   * Returns true if permission1 implies permission2 using same logic as WildcardPermission.
   *
   * @private
   * @param {string} permission1    Granted permission
   * @param {string} permission2    Permission under-test
   * @return {boolean}
   */
  implies: function(permission1, permission2) {
    var parts1 = permission1.split(':'),
        parts2 = permission2.split(':'),
        part1, part2, i;

    //<if debug>
    this.logTrace('Checking if:', permission1, 'implies:', permission2);
    //</if>

    for (i = 0; i < parts2.length; i++) {
      // If this permission has less parts than the other permission, everything after the number of parts contained
      // in this permission is automatically implied, so return true
      if (parts1.length - 1 < i) {
        return true;
      }
      else {
        part1 = parts1[i].split(',');
        part2 = parts2[i].split(',');
        if (!Ext.Array.contains(part1, '*') && !NX.util.Array.containsAll(part1, part2)) {
          return false;
        }
      }
    }

    // If this permission has more parts than the other parts, only imply it if all of the other parts are wildcards
    for (; i < parts1.length; i++) {
      part1 = parts1[i].split(',');
      if (!Ext.Array.contains(part1, '*')) {
        return false;
      }
    }

    return true;
  },

  /**
   * Check if any permission exists with given prefix and is permitted.
   *
   * This does not perform any implied checking, use with caution.
   *
   * @public
   * @param {string} prefix
   */
  checkExistsWithPrefix: function(prefix) {
    var me = this,
        exists = false;

    // short-circuit if permissions are not installed
    if (!me.available()) {
      return false;
    }

    Ext.Object.each(me.permissions, function(permission, permitted) {
      if (Ext.String.startsWith(permission, prefix) && permitted === true) {
        exists = true;
        return false; // break
      }
      return true; // continue
    });

    return exists;
  }
});
