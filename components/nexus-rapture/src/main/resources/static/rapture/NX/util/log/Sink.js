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
 * Allows customizable processing of {@link NX.model.LogEvent}.
 *
 * @since 3.0
 */
Ext.define('NX.util.log.Sink', {
  mixins: {
    stateful: 'Ext.state.Stateful',
    logAware: 'NX.LogAware'
  },

  /**
   * Sink enabled.
   *
   * @property {Boolean}
   * @readonly
   */
  enabled: true,

  /**
   * @constructor
   */
  constructor: function () {
    // setup stateful configuration with class-name, these are not technically singletons but are used as such
    this.mixins.stateful.constructor.call(this, {
      stateful: true,
      stateId: this.self.getName()
    });

    this.callParent(arguments);
    this.initState();
  },

  /**
   * @override
   * @return {Object}
   */
  getState: function() {
    return {
      enabled: this.enabled
    };
  },

  /**
   * Toggle enabled.
   *
   * @public
   * @param {boolean} flag
   */
  setEnabled: function (flag) {
    this.enabled = flag;

    //<if debug>
    this.logInfo('Enabled:', flag);
    //</if>

    this.saveState();
  },

  /**
   * @public
   * @param {NX.model.LogEvent} event
   */
  receive: function(event) {
    throw 'abstract-method';
  }
});
