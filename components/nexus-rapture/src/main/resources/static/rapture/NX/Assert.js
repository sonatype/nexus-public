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
 * Assertion helper.
 *
 * @since 3.0
 */
Ext.define('NX.Assert', {
  singleton: true,
  requires: [
    'NX.Console'
  ],

  /**
   * Set to true to disable all assertions.
   *
   * @public
   * @property {Boolean}
   */
  disable: false,

  /**
   * @public
   * @param {Boolean} expression
   * @param {String...} message
   */
  assert: function() {
    //<if assert>
    if (this.disable) {
      return;
    }
    var args = Array.prototype.slice.call(arguments),
        expression = args.shift();
    if (!expression) {
      args.unshift('Assertion failure:');
      NX.Console.error.apply(NX.Console, args);
    }
    //</if>
  }
});
