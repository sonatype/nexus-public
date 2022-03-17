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
/*global NX, Ext, Nexus*/

/**
 * Master/detail view.
 *
 * @since 2.7
 */
NX.define('Nexus.masterdetail.MasterDetail', {
  extend: 'Ext.Panel',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  /**
   * @property
   */
  master: undefined,

  /**
   * @property
   */
  detail: undefined,

  /**
   * @property
   */
  emptySelection: undefined,

  /**
   * @constructor
   */
  constructor: function (master, detail, emptySelection) {
    var self = this;

    NX.assert(master, 'Missing master');
    NX.assert(detail, 'Missing detail');
    NX.assert(emptySelection, 'Missing emptySelection');

    self.master = master;
    self.detail = detail;
    self.emptySelection = emptySelection;

    self.constructor.superclass.constructor.apply(self, arguments);
  },

  /**
   * @override
   */
  initComponent: function () {
    var self = this;

    self.detailPanel = NX.create('Ext.Panel', {
      region: 'south',
      minHeight: 25, // height of panel title
      split: true,
      autoDestroy: false,

      layout: 'card',
      defaults: {
        border: false
      },
      items: [
        self.emptySelection,  // 0
        self.detail           // 1
      ],
      activeItem: 0,
      listeners: {
        // resize detail panel height to 75% of parent on first render
        afterrender: {
          single: true,
          fn: function () {
            self.detailPanel.setHeight(self.getHeight() * 0.6);
          }
        }
      }
    });

    Ext.apply(self, {
      cls: 'nx-masterdetail-MasterDetail',
      layout: 'border',
      border: false,

      items: [
        self.master,
        self.detailPanel
      ]
    });

    self.master.getSelectionModel().on('selectionchange', self.selectionChanged, self);

    self.constructor.superclass.initComponent.apply(self, arguments);
  },

  /**
   * Update detail panel when grid selection changes.
   * @param sm    grid selection model
   * @private
   */
  selectionChanged: function (sm) {
    var self = this,
        cardLayout = self.detailPanel.getLayout(),
        selections = sm.getSelections();

    if (selections.length === 0) {
      cardLayout.setActiveItem(0);
    }
    else {
      cardLayout.setActiveItem(1);
      self.detail.updateRecord(selections[0].data);
    }
  }

});