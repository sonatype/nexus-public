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
 * Wizard controller support.
 *
 * @since 3.0
 * @abstract
 */
Ext.define('NX.wizard.Controller', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Assert'
  ],

  /**
   * Registered steps.
   *
   * @private
   * @type {NX.wizard.Step[]}
   */
  steps: undefined,

  /**
   * Active step index.
   *
   * @private
   * @type {Number}
   */
  activeStepIndex: undefined,

  /**
   * Shared context.
   *
   * @private
   * @type {Ext.util.MixedCollection}
   */
  context: undefined,

  /**
   * @override
   */
  init: function () {
    var me = this;

    // initialize privates
    me.steps = [];
    me.activeStepIndex = 0;
    me.context = Ext.create('Ext.util.MixedCollection');

    // listen for events for logging consistency
    //<if debug>
    me.context.on({
      add: function(index, value, key) {
        me.logDebug('Set', key, '=', value);
      },
      remove: function(value, key) {
        me.logDebug('Unset', key);
      }
    });
    //</if>

    me.addRef([
      {
        ref: 'content',
        selector: 'nx-feature-content'
      },
      {
        ref: 'panel',
        selector: 'nx-wizard-panel'
      }
    ]);

    me.listen({
      controller: {
        '#Refresh': {
          // FIXME: handle-global refresh, this is not ideal as this calls all wizards even if not activated
          refresh: me.refresh
        }
      }
    });

    me.callParent();
  },

  /**
   * Reset when controller is destroyed.
   *
   * @override
   */
  onDestroy: function() {
    this.reset();
  },

  /**
   * Register a step.
   *
   * @protected
   * @param {String|Object} config
   */
  registerStep: function(config) {
    var step;

    //<if debug>
    this.logDebug('Register step:', config);
    //</if>

    if (Ext.isString(config)) {
      step = Ext.create(config);
    }
    else {
      step = Ext.widget(config);
    }

    step.attach(this);
    this.steps.push(step);
  },

  /**
   * Register steps.
   *
   * @param {String[]|Object[]} configs
   */
  registerSteps: function(configs) {
    var me = this;
    Ext.Array.each(configs, function(config) {
      me.registerStep(config);
    });
  },

  /**
   * Get a step by name.
   *
   * @param {String} name
   * @return {NX.wizard.Step} Step with name or null if not found.
   */
  getStep: function(name) {
    var s = this.steps,
        i;

    for (i = 0; i < s.length; i++) {
      if (name === s[i].getName()) {
        return s[i];
      }
    }

    this.logWarn('Missing step:', name);

    return null;
  },

  /**
   * Return the index of given step.
   *
   * @param {NX.wizard.Step} step
   * @return {number}
   */
  getStepIndex: function(step) {
    var s = this.steps,
        i;

    for (i = 0; i < s.length; i++) {
      if (step.getName() === s[i].getName()) {
        return i;
      }
    }

    return -1;
  },

  /**
   * Returns the active step.
   *
   * @return {NX.wizard.Step}
   */
  getActiveStep: function() {
    return this.steps[this.activeStepIndex];
  },

  /**
   * Load wizard.
   *
   * @protected
   */
  load: function() {
    var s = this.steps,
        i,
        panel;

    //<if debug>
    this.logDebug('Loading');
    //</if>

    panel = this.getPanel().getScreenContainer();
    for (i = 0; i < s.length; i++) {
      panel.add(s[i].createScreenCmp());
    }

    this.restore();
  },

  /**
   * @private
   */
  refresh: function() {
    //<if debug>
    this.logDebug('Refreshing');
    //</if>

    var step = this.getActiveStep();

    if (step){
      step.refresh();
    }
  },

  //
  // Context
  //

  /**
   * Returns shared context.
   *
   * @returns {Ext.util.MixedCollection}
   */
  getContext: function() {
    return this.context;
  },

  //
  // Navigation
  //

  // TODO: fire navigation events?

  /**
   * Move to specific step index.
   *
   * @param {number} index
   * @return {boolean} True if moved
   */
  moveTo: function(index) {
    if (index < 0 || index + 1 > this.steps.length) {
      this.logError('Index out of bounds:', index);
      return false;
    }

    //<if debug>
    this.logDebug('Moving to:', index);
    //</if>

    var panel = this.getPanel(),
        container,
        cards,
        screen;

    // panel may not exist if controller being destroyed
    if (!panel) {
      return false;
    }

    container = panel.getScreenContainer();
    cards = container.getLayout();

    // move and resolve screen component
    screen = cards.setActiveItem(index);
    if (screen === false) {
      screen = cards.getActiveItem();
    }

    this.activeStepIndex = index;

    this.updateScreenHeader(screen, index);

    return true;
  },

  /**
   * Update the title and progress for the current screen.
   *
   * @private
   * @param {NX.wizard.Screen} screen
   * @param {number} index
   */
  updateScreenHeader: function(screen, index) {
    var panel = this.getPanel(),
        s = this.steps,
        enabledSteps = 0,
        screenNumber = 1,
        i;

    // calculate number of enabled steps and displayed screen number
    for (i = 0; i < s.length; i++) {
      if (s[i].getEnabled()) {
        enabledSteps++;
        if (i < index) {
          screenNumber++;
        }
      }
    }

    panel.setTitle(screen.getTitle());
    panel.setProgress(screenNumber, enabledSteps);
  },

  /**
   * Move to step with given name.
   *
   * @param {String} name
   * @return {boolean} True if moved
   */
  moveToStepNamed: function(name) {
    var me = this,
        step;

    //<if debug>
    me.logDebug('Moving to step with name:', name);
    //</if>

    step = me.getStep(name);

    if (!step) {
      this.logError('Missing step with name:', name);
      return false;
    }

    return me.moveTo(me.getStepIndex(step));
  },

  /**
   * Move to the next enabled step.
   *
   * @return {boolean}  True if moved
   */
  moveNext: function() {
    var current = this.activeStepIndex,
        max = this.steps.length,
        i;

    if (current + 1 >= max) {
      this.logError('No next step');
      return false;
    }

    for (i = current + 1; i < max; i++) {
      if (this.steps[i].getEnabled()) {
        return this.moveTo(i);
      }
    }

    this.logError('No enabled next step');
    return false;
  },

  /**
   * Move to the previous step.
   *
   * @return {boolean}  True if moved
   */
  moveBack: function() {
    var current = this.activeStepIndex,
        i;
        
    if (current <= 0) {
      this.logError('No back step');
      return false;
    }

    for (i = current - 1; i >= 0; i--) {
      if (this.steps[i].getEnabled()) {
        return this.moveTo(i);
      }
    }

    this.logError('No enabled back step');
    return false;
  },

  /**
   * Reset state to initial.
   */
  reset: function() {
    var s = this.steps,
        i;

    //<if debug>
    this.logDebug('Resetting');
    //</if>

    // Reset all steps
    for (i = 0; i < s.length; i++) {
      s[i].reset();
    }

    // Reset context
    this.context.removeAll();

    // Move to starting position
    this.moveTo(0);

    //<if debug>
    this.logDebug('Reset');
    //</if>
  },

  /**
   * Cancel and reset.
   */
  cancel: function() {
    //<if debug>
    this.logDebug('Canceled');
    //</if>

    this.reset();
  },

  /**
   * Finish and reset.
   */
  finish: function() {
    //<if debug>
    this.logDebug('Finished');
    //</if>

    this.reset();
  },

  /**
   * Restore state to last known.
   */
  restore: function() {
    this.moveTo(this.activeStepIndex);

    //<if debug>
    this.logDebug('Restored');
    //</if>
  },

  //
  // Helpers
  //

  /**
   * Display screen mask.
   *
   * @param {string} message
   */
  mask: function(message) {
    this.getContent().getEl().mask(message);
  },

  /**
   * Remove screen mask.
   */
  unmask: function() {
    this.getContent().getEl().unmask();
  }
});
