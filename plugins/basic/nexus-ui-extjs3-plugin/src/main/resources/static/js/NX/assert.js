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
/*global define,Ext,NX */

define('NX/assert', [], function() {

  Ext.ns('NX');

  /**
   * Exception thrown when assertion is triggered.
   *
   * @param message
   * @constructor
   *
   * @since 2.4
   */
  function AssertError(message) {
    this.name = 'AssertError';
    this.message = message || 'Assertion failure';
  }

  AssertError.prototype = new Error();
  AssertError.prototype.constructor = AssertError;

  /**
   * Assertion helpers.
   */
  Ext.apply(NX, {

    /**
     * Assert a condition.  When the condition expression is false an exception is thrown.
     *
     * @param expression    Condition expression
     * @param message       Exception message
     *
     * @since 2.4
     */
    assert : function(expression, message) {
      if (!expression) {
        throw new AssertError(message);
      }
    }

  });

});