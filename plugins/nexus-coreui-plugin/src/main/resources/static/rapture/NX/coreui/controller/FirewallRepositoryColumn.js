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
 * Manages firewall column shown in repository grid.
 *
 * @since 3.2
 */
Ext.define('NX.coreui.controller.FirewallRepositoryColumn', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.grid.column.Column',
    'Ext.XTemplate',
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n',
    'NX.Icons',
    'NX.ext.grid.column.Renderers',
    'NX.Windows'
  ],

  models: [
    'FirewallRepositoryStatus'
  ],
  stores: [
    'FirewallRepositoryStatus'
  ],
  refs: [
    { ref: 'list', selector: 'nx-coreui-repository-list-template' }
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.getApplication().getIconController().addIcons({
      'license-alert': {
        file: 'license-alert.png',
        variants: ['x16']
      },
      'security-alert': {
        file: 'security-alert.png',
        variants: ['x16']
      }
    });

    me.listen({
      controller: {
        '#Permissions': {
          changed: me.refreshFirewallColumn
        }
      },
      store: {
        '#RepositoryReference': {
          load: me.loadFirewallStatus
        },
        '#FirewallRepositoryStatus': {
          load: me.refreshFirewallColumn
        }
      },
      component: {
        'nx-coreui-repository-list-template': {
          afterrender: me.bindFirewallColumn
        }
      }
    });

    me.firewallColumnTemplate = Ext.create('Ext.XTemplate',
        '<div class="nx-firewall-container">',
        '<tpl if="criticalCount &gt; 0">',
        '<div class="nx-firewall-chiclet critical">{criticalCount}</div>',
        '</tpl>',
        '<tpl if="severeCount &gt; 0">',
        '<div class="nx-firewall-chiclet severe">{severeCount}</div>',
        '</tpl>',
        '<tpl if="moderateCount &gt; 0">',
        '<div class="nx-firewall-chiclet moderate">{moderateCount}</div>',
        '</tpl>',
        '<tpl if="!criticalCount && !severeCount && !moderateCount">',
        '<div style="float:left;">' + NX.I18n.get('FirewallRepositoryColumn_NoViolations') + '</div>',
        '</tpl>',
        '<tpl if="quarantinedCount &gt; 0">',
        '<div class="nx-firewall-quarantine-icon"></div><div class="nx-firewall-quarantine-count">{quarantinedCount}</div>',
        '</tpl>',
        '<div class="nx-external-link"></div>',
        '</div>',
        {
          compiled : true
        }
    );

    me.firewallColumnMessageTemplate = Ext.create('Ext.XTemplate',
        '<div class="nx-firewall-container">',
        '<div style="float:left;">{message}</div>',
        '</div>',
        {
          compiled : true
        }
    );

    me.firewallColumnErrorTemplate = Ext.create('Ext.XTemplate',
        '<div class="nx-firewall-container">',
        '<div class="nx-firewall-iq-error-icon"></div>',
        '<div class="nx-firewall-error">{errorMessage}</div>',
        '</div>',
        {
          compiled : true
        }
    );
  },

  /**
   * Load Firewall status store if repository grid is active.
   *
   * @private
   */
  loadFirewallStatus: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.getFirewallRepositoryStatusStore().load();
    }
  },

  /**
   * Add Firewall column.
   *
   * @private
   * @param {NX.coreui.view.repository.RepositoryList} grid repository grid
   */
  bindFirewallColumn: function(grid) {
    var me = this;
    grid.mon(
        NX.Conditions.and(
            NX.Conditions.watchState('user'),
            NX.Conditions.isPermitted("nexus:iq-violation-summary:read")
        ),
        {
          satisfied: Ext.pass(me.addFirewallColumn, grid),
          unsatisfied: Ext.pass(me.removeFirewallColumn, grid),
          scope: me
        }
    );
  },

  /**
   * Add Firewall column to repository grid.
   *
   * @private
   * @param {NX.coreui.view.repository.RepositoryList} grid repository grid
   */
  addFirewallColumn: function(grid) {
    var me = this,
        view = grid.getView();

    if (!grid.firewallColumn) {
      grid.firewallColumn = grid.pushColumn({
        id: 'firewallColumn',
        header: NX.I18n.get('FirewallRepositoryColumn_Header'),
        hideable: false,
        sortable: false,
        menuDisabled: true,
        stateId: 'firewall',
        width: 120,
        align: 'center',
        renderer: Ext.bind(me.renderFirewallColumn, me),
        listeners: {
          click: Ext.bind(me.viewReportHandler, me)
        }
      });

      view.refresh();
    }
  },

  /**
   * Remove Firewall column from repository grid
   *
   * @private
   * @param {NX.coreui.view.repository.RepositoryList}
   */
  removeFirewallColumn: function(grid) {
    var column = grid.firewallColumn;
    if (column) {
      grid.headerCt.remove(column);
      grid.getView().refresh();
      delete grid.firewallColumn;
    }
  },


  /**
   * Render Firewall column based on corresponding {NX.coreui.model.FirewallRepositoryStatus}.
   *
   * @private
   * @param value (not used)
   * @param metadata firewall column metadata
   * @param {NX.coreui.model.Repository} repositoryModel repository model
   * @returns {String} Firewall column content
   */
  renderFirewallColumn: function(value, metadata, repositoryModel) {
    var me = this,
        statusModel = me.getFirewallRepositoryStatusStore().getById(repositoryModel.getId());
    if (statusModel) {
      if (statusModel.get('errorMessage')) {
        var errorMessage = Ext.util.Format.htmlEncode(statusModel.get('errorMessage'));
        metadata.tdAttr = 'data-qtip="' + errorMessage + '"';
        return me.firewallColumnErrorTemplate.apply({
          errorMessage: errorMessage
        });
      }
      else if (statusModel.get('message')) {
        var message = Ext.util.Format.htmlEncode(statusModel.get('message'));
        metadata.tdAttr = 'data-qtip="' + message + '"';
        return me.firewallColumnMessageTemplate.apply({
          message: message
        });
      }
      return me.firewallColumnTemplate.apply({
        criticalCount: statusModel.get('criticalComponentCount'),
        severeCount: statusModel.get('severeComponentCount'),
        moderateCount: statusModel.get('moderateComponentCount'),
        quarantinedCount: statusModel.get('quarantinedComponentCount')
      });
    }
    else if (me.getFirewallRepositoryStatusStore().loaded) {
      return NX.ext.grid.column.Renderers.optionalData(null);
    }
    return NX.I18n.get('FirewallRepositoryColumn_Loading');
  },

  /**
   * Refresh repository grid view (when Firewall status changes).
   *
   * @private
   */
  refreshFirewallColumn: function() {
    var list = this.getList();

    if (list) {
      list.getView().refresh();
    }
  },

  /**
   * Handler for when the cell is clicked and opens a new window to go to the IQ report
   *
   * @private
   * @param gridView (not used)
   * @param cell (not used)
   * @param row (not used)
   * @param col (not used)
   * @param event (not used)
   * @param {NX.coreui.model.Repository} repositoryModel repository model
   */
  viewReportHandler: function(gridView, cell, row, col, event, repositoryModel) {
    var me = this;
    var statusModel = me.getFirewallRepositoryStatusStore().getById(repositoryModel.getId());
    if (statusModel && statusModel.get('reportUrl')) {
      NX.Windows.open(statusModel.get('reportUrl'), 'noopener,_blank');
    }
    return false;
  }

});
