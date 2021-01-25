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
/*jslint plusplus: true*/

/**
 * @since 3.0
 */
Ext.define('NX.util.condition.Condition', {
  mixins: {
    observable: 'Ext.mixin.Observable',
    logAware: 'NX.LogAware'
  },

  statics: {
    counter: 1
  },

  /**
   * Generated id used by event bus.
   *
   * @property {String}
   */
  id: undefined,

  /**
   * Number of listeners listening to this condition.
   *
   * @private
   * @property {Number}
   */
  listenerCounter: 0,

  /**
   * True when this condition is bounded.
   *
   * @protected
   * @property {Boolean}
   */
  bounded: false,

  /**
   * True when this condition is satisfied.
   *
   * @private
   * @property {Boolean}
   */
  satisfied: false,

  /**
   * @constructor
   * @param {Object} config
   */
  constructor: function (config) {
    var me = this;

    me.id = me.self.getName() + '-' + NX.util.condition.Condition.counter++;

    me.mixins.observable.constructor.call(me, config);
  },

  // HACK: comment the following lines to let debug messages flow
  /**
   * @override
   */
  logDebug: function () {
    // empty
  },

  /**
   * Sets {@link #bounded} = true.
   *
   * @protected
   * @chainable
   */
  bind: function () {
    var me = this;

    if (!me.bounded) {
      me.setBounded(true);
    }

    return me;
  },

  /**
   * Clears all listeners of this condition and sets {@link #bounded} = false.
   *
   * @protected
   * @chainable
   */
  unbind: function () {
    var me = this;

    if (me.bounded) {
      me.clearListeners();
      Ext.app.EventBus.unlisten(me.id);
      me.setBounded(false);
    }

    return me;
  },

  /**
   * Sets {@link #bounded} = false and makes condition unsatisfied.
   *
   * @protected
   * @param bounded
   */
  setBounded: function (bounded) {
    var me = this;
    if (Ext.isDefined(me.bounded)) {
      if (bounded !== me.bounded) {
        if (!bounded) {
          me.setSatisfied(false);
        }

        //<if debug>
        me.logDebug((bounded ? 'Bounded:' : 'Unbounded:'), me);
        //</if>

        me.bounded = bounded;
        Ext.defer(function () {
          NX.getApplication().getStateController().fireEvent('conditionboundedchanged', me);
        }, 1);
      }
    }
    else {
      me.bounded = bounded;
    }
  },

  /**
   * @public
   * @returns {boolean} true, if condition is satisfied
   */
  isSatisfied: function () {
    return this.satisfied;
  },

  /**
   * Sets {@link #satisfied} = true and fires 'satisfied' / 'unsatisfied' if satisfied changed.
   *
   * @protected
   * @param {boolean} satisfied if condition is satisfied
   */
  setSatisfied: function (satisfied) {
    var me = this;
    if (Ext.isDefined(me.satisfied)) {
      if (satisfied !== me.satisfied) {
        //<if debug>
        me.logDebug((satisfied ? 'Satisfied:' : 'Unsatisfied:'), me);
        //</if>

        me.satisfied = satisfied;
        me.fireEvent(satisfied ? 'satisfied' : 'unsatisfied', me);
        Ext.defer(function () {
          NX.getApplication().getStateController().fireEvent('conditionstatechanged', me);
        }, 1);
      }
    }
    else {
      me.satisfied = satisfied;
    }
  },

  /**
   * Additionally, {@link #bind}s when first listener added.
   *
   * @override
   */
  doAddListener: function (ename, fn, scope, options, order, caller, manager) {
    var me = this;
    me.mixins.observable.doAddListener.call(me, ename, fn, scope, options, order, caller, manager);
    me.listenerCounter++;
    if (me.listenerCounter === 1) {
      me.bind();
    }
    // re-fire event so new listener has the chance to do its job
    me.fireEvent(me.satisfied ? 'satisfied' : 'unsatisfied', me);
  },

  /**
   * Additionally, {@link #unbind}s when no more listeners.
   *
   * @override
   */
  doRemoveListener: function (ename, fn, scope) {
    var me = this;
    me.mixins.observable.doRemoveListener.call(me, ename, fn, scope);
    me.listenerCounter--;
    if (me.listenerCounter === 0) {
      me.unbind();
    }
  }
});
