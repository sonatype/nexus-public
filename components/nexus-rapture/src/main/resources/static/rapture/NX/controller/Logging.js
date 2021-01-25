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
 * Logging controller.
 *
 * @since 3.0
 * @see NX.util.log.Sink
 */
Ext.define('NX.controller.Logging', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Log',
    'NX.util.log.StoreSink',
    'NX.util.log.ConsoleSink',
    'NX.util.log.RemoteSink'
  ],
  mixins: {
    stateful: 'Ext.state.Stateful'
  },

  stores: [
    'LogEvent'
  ],

  /**
   * Map of named sinks.
   *
   * @private
   * @property {Object}
   * @readonly
   */
  sinks: {},

  /**
   * Array of configured sinks.
   *
   * Mirrors {@link #sinks} values, but in array form for faster evaluation.
   *
   * @private
   * @property {NX.util.log.Sink[]}
   * @readonly
   */
  sinkRefs: undefined,

  /**
   * Logging threshold.
   *
   * @private
   * @property {String}
   */
  threshold: 'debug',

  /**
   * @constructor
   */
  constructor: function () {
    this.mixins.stateful.constructor.call(this, {
      stateful: true,
      stateId: this.self.getName()
    });

    this.callParent(arguments);
    this.initState();
  },

  /**
   * @override
   */
  init: function () {
    this.sinks = {
      store: Ext.create('NX.util.log.StoreSink', this.getStore('LogEvent')),
      console: Ext.create('NX.util.log.ConsoleSink'),
      remote: Ext.create('NX.util.log.RemoteSink')
    };
    // build array of all sink objects for faster evaluation
    this.sinkRefs = Ext.Object.getValues(this.sinks);
  },

  /**
   * Attach to {@link NX.Log} helper.
   *
   * @override
   */
  onLaunch: function () {
    NX.Log.attach(this);
    this.logInfo('Attached');
  },

  /**
   * @override
   * @return {Object}
   */
  getState: function() {
    return {
      threshold: this.threshold
    };
  },

  /**
   * Returns sink by name, or undefined.
   *
   * @public
   * @param {String} name
   */
  getSink: function(name) {
    return this.sinks[name];
  },

  /**
   * Get the logging threshold.
   *
   * @public
   * @returns {String}
   */
  getThreshold: function () {
    return this.threshold;
  },

  /**
   * Set the logging threshold.
   *
   * @public
   * @param {String} threshold
   */
  setThreshold: function (threshold) {
    this.threshold = threshold;
    this.saveState();
  },

  /**
   * Mapping of {@link NX.model.LogLevel} weights.
   *
   * @private
   * @property {Object}
   */
  levelWeights: {
    all: 1,
    trace: 2,
    debug: 3,
    info: 4,
    warn: 5,
    error: 6,
    off: 7
  },

  /**
   * Check if given level exceeds configured threshold.
   *
   * @private
   * @param {String} level
   * @return {Boolean}
   */
  exceedsThreshold: function (level) {
    return this.levelWeights[level] >= this.levelWeights[this.threshold];
  },

  /**
   * Record a log-event.
   *
   * @public
   * @param event
   */
  recordEvent: function (event) {
    // ignore events that do not exceed threshold
    if (!this.exceedsThreshold(event.level)) {
      return;
    }

    // pass events to all enabled sinks
    for (var i=0; i<this.sinkRefs.length; i++) {
      if (this.sinkRefs[i].enabled) {
        this.sinkRefs[i].receive(event);
      }
    }
  }
});
