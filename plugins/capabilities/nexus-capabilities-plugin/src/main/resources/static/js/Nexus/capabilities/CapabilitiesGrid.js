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
/*global NX, Ext, Sonatype, Nexus*/

/**
 * Capabilities master grid.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CapabilitiesGrid', {
  extend: 'Ext.grid.GridPanel',

  mixins: [
    'Nexus.LogAwareMixin',
    'Nexus.capabilities.CapabilitiesMediatorMixin'
  ],

  requires: [
    'Nexus.capabilities.Icons',
    'Nexus.capabilities.CapabilitiesGridStore',
    'Nexus.capabilities.CreateCapabilityWindow',
    'Nexus.capabilities.CapabilitiesGridFilterBox'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var self = this,
        icons = Nexus.capabilities.Icons;

    self.buttonAdd = NX.create('Ext.Button', {
      text: 'New',
      iconCls: icons.get('capability_add').cls,
      tooltip: 'Add a new capability',
      handler: function () {
        self.addCapability();
      },
      disabled: true
    });

    self.buttonDuplicate = NX.create('Ext.Button', {
      text: 'Duplicate',
      iconCls: icons.get('capability_add').cls,
      tooltip: 'Duplicates selected capability',
      handler: function () {
        var selections = self.getSelectionModel().getSelections();
        if (selections.length > 0) {
          self.duplicateCapability(selections[0].data);
        }
      },
      disabled: true
    });

    self.buttonDelete = NX.create('Ext.Button', {
      text: 'Delete',
      iconCls: icons.get('capability_delete').cls,
      tooltip: 'Delete selected capability',
      handler: function (button) {
        var selections = self.getSelectionModel().getSelections();
        if (selections.length > 0) {
          self.deleteCapability(selections[0].data, button.btnEl);
        }
      },
      disabled: true
    });

    self.gridStore = NX.create('Nexus.capabilities.CapabilitiesGridStore', {
      capabilityStore: self.mediator().capabilityStore
    });

    self.filterBox = NX.create('Nexus.capabilities.CapabilitiesGridFilterBox', {
      filteredGrid: self,
      width: 200
    });

    Ext.apply(self, {
      cls: 'nx-capabilities-CapabilityGrid',
      ds: self.gridStore,
      stripeRows: true,
      border: false,

      loadMask: {
        msg: 'Loading...',
        msgCls: 'loading-indicator'
      },

      view: NX.create('Ext.grid.GroupingView', {
        emptyText: 'No capabilities defined',
        emptyTextWhileFiltering: 'No capabilities matched criteria: {criteria}',
        deferEmptyText: false,
        getRowClass: function (record) {
          var capability = record.data;
          if (capability.enabled && !capability.active) {
            return 'red-flag';
          }
        },
        forceFit: true,
        groupTextTpl: '{[values.text.replace("undefined","(none)").replace("null","(none)")]}'
      }),

      sm: NX.create('Ext.grid.RowSelectionModel', {
        singleSelect: true,
        listeners: {
          selectionchange: {
            fn: self.selectionChanged,
            scope: self
          }
        }
      }),

      colModel: self.gridStore.getColumnModel(),
      autoExpandColumn: 'notes',

      tbar: [
        {
          text: 'Refresh',
          tooltip: 'Refresh capabilities',
          iconCls: icons.get('refresh').cls,
          handler: function () {
            self.refresh();
          }
        },
        self.buttonAdd,
        self.buttonDuplicate,
        self.buttonDelete,
        '->',
        self.filterBox
      ],

      listeners: {
        destroy: {
          fn: function () {
            self.mediator().capabilityStore.removeListener('beforeload', self.rememberSelection, self);
            self.mediator().capabilityStore.removeListener('load', self.reconfigureGrid, self);
            self.mediator().capabilityTypeStore.removeListener('beforeload', self.disableAddButton, self);
            self.mediator().capabilityTypeStore.removeListener('load', self.maybeEnableAddButton, self);
          },
          scope: self
        },
        render: {
          fn: function () {
            self.mediator().capabilityStore.on('beforeload', self.rememberSelection, self);
            self.mediator().capabilityStore.on('load', self.reconfigureGrid, self);

            self.mediator().capabilityTypeStore.on('beforeload', self.disableAddButton, self);
            self.mediator().capabilityTypeStore.on('load', self.maybeEnableAddButton, self);
          },
          scope: self
        }
      }

    });

    self.constructor.superclass.initComponent.apply(self, arguments);

    self.refresh();

    self.on('rowcontextmenu', self.showMenu, self);
  },

  /**
   * @private
   */
  selectionChanged: function (sm) {
    var self = this,
        sp = Sonatype.lib.Permissions;

    self.buttonDuplicate.disable();
    self.buttonDelete.disable();
    if (sm.getCount() !== 0) {
      if (sp.checkPermission('nexus:capabilities', sp.CREATE)) {
        self.buttonDuplicate.enable();
      }
      if (sp.checkPermission('nexus:capabilities', sp.DELETE)) {
        self.buttonDelete.enable();
      }
    }
  },

  /**
   * @private
   */
  selectedRecords: undefined,

  /**
   * Remember what records are selected.
   * @private
   */
  rememberSelection: function () {
    this.selectedRecords = this.getSelectionModel().getSelections();
  },

  /**
   * Recall selection from previous remembered selection.
   * @private
   */
  recallSelection: function () {
    var self = this,
        toSelect = [];

    if (self.selectedRecords === undefined || self.selectedRecords.length === 0) {
      return;
    }

    Ext.each(self.selectedRecords, function (record) {
      record = self.getStore().getById(record.id);
      if (!Ext.isEmpty(record)) {
        toSelect.push(record);
      }
    });

    self.getSelectionModel().selectRecords(toSelect);
  },

  /**
   * Refreshes grid and selects a capability by id.
   * @param capabilityId to be selected
   * @private
   */
  refreshAndSelect: function (capabilityId) {
    var self = this;

    self.getSelectionModel().clearSelections();
    self.refresh();
    self.mediator().capabilityStore.on('load',
        function () {
          var record = self.getStore().getById(capabilityId);
          if (!Ext.isEmpty(record)) {
            self.getSelectionModel().selectRecords([record]);
          }
        },
        self, {single: true}
    );
  },

  /**
   * Reconfigures the grid based on information available is grid store.
   * @private
   */
  reconfigureGrid: function () {
    var self = this;

    if (!self.mediator().capabilityStore.sameTagKeysAs(self.gridStore.tagKeys)) {
      self.gridStore = NX.create('Nexus.capabilities.CapabilitiesGridStore', {
        capabilityStore: self.mediator().capabilityStore
      });
      self.gridStore.on('load', self.recallSelection, self);

      self.reconfigure(self.gridStore, self.gridStore.getColumnModel());
    }
    else {
      self.gridStore.loadCapabilities();
    }
  },

  /**
   * Refreshes data stores.
   * @private
   */
  refresh: function () {
    this.mediator().refresh();
  },

  /**
   * Deletes a capability.
   * @param capability to be deleted
   * @private
   */
  deleteCapability: function (capability, animEl) {
    var self = this;

    Ext.Msg.show({
      title: 'Confirm deletion?',
      msg: self.mediator().describeCapability(capability),
      buttons: Ext.Msg.YESNO,
      animEl: animEl,
      icon: Ext.MessageBox.QUESTION,
      closeable: false,
      scope: self,
      fn: function (buttonName) {
        if (buttonName === 'yes' || buttonName === 'ok') {
          self.mediator().deleteCapability(capability,
              function () {
                self.mediator().showMessage('Capability deleted', self.mediator().describeCapability(capability));
                self.refresh();
              },
              function (response, options) {
                self.mediator().handleError(response, options, 'Capability could not be deleted');
                if (response.status === 404) {
                  self.refresh();
                }
              }
          );
        }
      }
    });
  },

  /**
   * Enables a capability.
   * @param capability to be enabled
   * @private
   */
  enableCapability: function (capability) {
    var self = this;

    self.mediator().enableCapability(capability,
        function () {
          self.mediator().showMessage('Capability enabled', self.mediator().describeCapability(capability));
          self.refresh();
        },
        function (response, options) {
          self.mediator().handleError(response, options, 'Capability could not be enabled');
          if (response.status === 404) {
            self.refresh();
          }
        }
    );
  },

  /**
   * Disables a capability.
   * @param capability to be disabled
   * @private
   */
  disableCapability: function (capability) {
    var self = this;

    self.mediator().disableCapability(capability,
        function () {
          self.mediator().showMessage('Capability disabled', self.mediator().describeCapability(capability));
          self.refresh();
        },
        function (response, options) {
          self.mediator().handleError(response, options, 'Capability could not be disabled');
          if (response.status === 404) {
            self.refresh();
          }
        }
    );
  },

  /**
   * Opens create window.
   * @private
   */
  addCapability: function () {
    NX.create(
        'Nexus.capabilities.CreateCapabilityWindow', this.refreshAndSelect.createDelegate(this)
    ).show();
  },

  /**
   * Opens create window with pre-configured values copied from provided capability.
   * @param capability to copy values from
   * @private
   */
  duplicateCapability: function (capability) {
    if (capability) {
      NX.create(
          'Nexus.capabilities.CreateCapabilityWindow', this.refreshAndSelect.createDelegate(this)
      ).show().importCapability(capability);
    }
  },

  /**
   * @private
   * Grid row where context menu was last activated.
   */
  contextMenuRow: undefined,

  /**
   * Shows context menu.
   * @private
   */
  showMenu: function (grid, index, e) {
    var self = this,
        sp = Sonatype.lib.Permissions,
        icons = Nexus.capabilities.Icons,
        row = grid.view.getRow(index),
        capability = self.store.getAt(index).data,
        menu;

    self.hideMenu();

    self.contextMenuRow = row;

    Ext.fly(row).addClass('x-node-ctx');

    menu = new Ext.menu.Menu({
      items: [
        {
          text: 'Refresh',
          iconCls: icons.get('refresh').cls,
          scope: self,
          handler: self.refresh.createDelegate(self)
        }
      ]
    });

    if (sp.checkPermission('nexus:capabilities', sp.EDIT)) {
      menu.add('-');
      if (capability.enabled) {
        menu.add({
          text: 'Disable',
          iconCls: icons.get('disable').cls,
          scope: self,
          handler: self.disableCapability.createDelegate(self, [capability])
        });
      }
      else {
        menu.add({
          text: 'Enable',
          iconCls: icons.get('enable').cls,
          scope: self,
          handler: self.enableCapability.createDelegate(self, [capability])
        });
      }
    }

    if (sp.checkPermission('nexus:capabilities', sp.CREATE)) {
      menu.add('-');
      menu.add({
        text: 'Duplicate',
        iconCls: icons.get('capability_add').cls,
        scope: self,
        handler: self.duplicateCapability.createDelegate(self, [capability])
      });
    }

    if (sp.checkPermission('nexus:capabilities', sp.DELETE)) {
      if (!sp.checkPermission('nexus:capabilities', sp.CREATE)) {
        menu.add('-');
      }
      menu.add({
        text: 'Delete',
        iconCls: icons.get('capability_delete').cls,
        scope: self,
        handler: self.deleteCapability.createDelegate(self, [capability])
      });
    }

    e.stopEvent();

    self.getSelectionModel().selectRow(index, false);

    menu.on('hide', self.hideMenu, self);
    menu.showAt(e.getXY());
  },

  /**
   * Hides context menu.
   * @private
   */
  hideMenu: function () {
    var self = this;

    if (self.contextMenuRow) {
      Ext.fly(self.contextMenuRow).removeClass('x-node-ctx');
      this.contextMenuRow = null;
    }
  },

  /**
   * Disables add button.
   * @private
   */
  disableAddButton: function () {
    var self = this;

    self.buttonAdd.disable();
  },

  /**
   * Enables add button if user has create permission adn there is at least one capability type available.
   * @private
   */
  maybeEnableAddButton: function () {
    var self = this,
        sp = Sonatype.lib.Permissions,
        canCreate = sp.checkPermission('nexus:capabilities', sp.CREATE);

    if (canCreate && self.mediator().capabilityTypeStore.getCount() > 0) {
      self.buttonAdd.enable();
    }
  }

});
