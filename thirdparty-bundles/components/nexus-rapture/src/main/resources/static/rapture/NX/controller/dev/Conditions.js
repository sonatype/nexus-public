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
 * Conditions developer panel controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.dev.Conditions', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.util.Filter'
  ],

  stores: [
    'NX.store.dev.Condition'
  ],
  views: [
    'dev.Conditions'
  ],

  refs: [
    {
      ref: 'showSatisfied',
      selector: 'nx-dev-conditions #showSatisfied'
    },
    {
      ref: 'showUnsatisfied',
      selector: 'nx-dev-conditions #showUnsatisfied'
    },
    {
      ref: 'devPanelTabs',
      selector: 'nx-dev-panel tabpanel'
    },
    {
      ref: 'devConditionsPanel',
      selector: 'nx-dev-panel nx-dev-conditions'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.excludeSatisfiedFilter = Ext.create('Ext.util.Filter', {
      id: 'excludeSatisfiedFilter',
      filterFn: function (record) {
        return record.get('satisfied') !== true;
      }
    });
    me.excludeUnsatisfiedFilter = Ext.create('Ext.util.Filter', {
      id: 'excludeUnsatisfiedFilter',
      filterFn: function (record) {
        return record.get('satisfied') !== false;
      }
    });

    me.listen({
      controller: {
        '#State': {
          conditionboundedchanged: me.boundedChanged,
          conditionstatechanged: me.stateChanged
        }
      },
      component: {
        'nx-dev-conditions #showSatisfied': {
          change: me.syncSatisfiedFilter
        },
        'nx-dev-conditions #showUnsatisfied': {
          change: me.syncUnsatisfiedFilter
        }
      }
    });
  },

  /**
   * @override
   */
  onLaunch: function () {
    var devPanelTab = this.getDevPanelTabs();

    if (devPanelTab) {
      devPanelTab.add({ xtype: 'nx-dev-conditions' });
    }
  },

  /**
   * @override
   */
  onDestroy: function () {
    var me = this,
        devConditionsPanel = me.getDevConditionsPanel();

    if (devConditionsPanel) {
      me.getDevPanelTabs().remove(devConditionsPanel);
    }
  },

  syncSatisfiedFilter: function () {
    var me = this,
        value = me.getShowSatisfied().getValue(),
        store = me.getStore('NX.store.dev.Condition');

    if (value) {
      store.removeFilter(me.excludeSatisfiedFilter);
    }
    else {
      store.addFilter(me.excludeSatisfiedFilter);
    }
  },

  syncUnsatisfiedFilter: function () {
    var me = this,
        value = me.getShowUnsatisfied().getValue(),
        store = me.getStore('NX.store.dev.Condition');

    if (value) {
      store.removeFilter(me.excludeUnsatisfiedFilter);
    }
    else {
      store.addFilter(me.excludeUnsatisfiedFilter);
    }
  },

  boundedChanged: function (condition) {
    var store = this.getStore('NX.store.dev.Condition'),
        model;

    if (condition.bounded) {
      store.add({ id: condition.id, condition: condition });
      store.filter();
    }
    else {
      model = store.getById(condition.id);
      if (model) {
        store.remove(model);
      }
    }
  },

  stateChanged: function (condition) {
    var store = this.getStore('NX.store.dev.Condition'),
        model = store.getById(condition.id);

    if (model) {
      model.set('satisfied', condition.isSatisfied());
      model.commit();
      store.filter();
    }
  }

});
