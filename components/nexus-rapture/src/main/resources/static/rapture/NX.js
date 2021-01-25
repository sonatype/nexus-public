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

// NOTE: This is not a real class, but exists for jsdoc purposes ONLY
// NOTE: Namespace is created via bootstrap.js and app.js

/**
 * NX singleton.
 *
 * @class NX
 * @singleton
 * @since 3.0
 */
(function () {
  /**
   * Global reference.
   *
   * @public
   * @property {Object} global
   * @readonly
   */

  /**
   * Application reference.
   *
   * @internal
   * @property {NX.app.Application} application
   * @readonly
   */

  /**
   * Container for bootstrap cruft.
   *
   * @internal
   * @property {Object} app
   * @property {Boolean} app.debug  Flag if debug is enabled.
   * @property {String} app.baseUrl Initial application base-url.
   * @property {Object} app.state   Initial application state.
   * @readonly
   */
});
