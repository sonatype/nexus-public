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
 * Manages health check column shown in repository grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.HealthCheckRepositoryColumn', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.grid.column.Column',
    'Ext.ToolTip',
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n',
    'NX.Icons',
    'NX.ext.grid.column.Renderers'
  ],

  models: [
    'HealthCheckRepositoryStatus'
  ],
  stores: [
    'HealthCheckRepositoryStatus'
  ],
  views: [
    'healthcheck.HealthCheckSummary',
    'healthcheck.HealthCheckEula'
  ],
  refs: [
    { ref: 'list', selector: 'nx-coreui-repository-list' },
    { ref: 'summary', selector: 'nx-coreui-healthcheck-summary' }
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
          changed: me.refreshHealthCheckColumn
        }
      },
      store: {
        '#Repository': {
          load: me.loadHealthCheckStatus
        },
        '#HealthCheckRepositoryStatus': {
          load: me.refreshHealthCheckColumn
        }
      },
      component: {
        'nx-coreui-repository-list': {
          afterrender: me.bindHealthCheckColumn
        }
      }
    });
  },

  /**
   * Load Health Check status store if repository grid is active.
   *
   * @private
   */
  loadHealthCheckStatus: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.getHealthCheckRepositoryStatusStore().load();
    }
  },

  /**
   * Add/Remove Health Check column based on nexus:healthcheck:read permission.
   *
   * @private
   * @param {NX.coreui.view.repository.RepositoryList} grid repository grid
   */
  bindHealthCheckColumn: function(grid) {
    var me = this;
    grid.mon(
        NX.Conditions.isPermitted("nexus:healthcheck:read"),
        {
          satisfied: Ext.pass(me.addHealthCheckColumn, grid),
          unsatisfied: Ext.pass(me.removeHealthCheckColumn, grid),
          scope: me
        }
    );
  },

  /**
   * Add Health Check column to repository grid.
   *
   * @private
   * @param {NX.coreui.view.repository.RepositoryList} grid repository grid
   */
  addHealthCheckColumn: function(grid) {
    var me = this,
        view = grid.getView(),
        column = grid.healthCheckColumn;

    if (!column) {
      column = grid.healthCheckColumn = Ext.create('Ext.grid.column.Column', {
        id: 'healthCheckColumn',
        header: NX.I18n.get('HealthCheckRepositoryColumn_Header'),
        hideable: false,
        sortable: false,
        menuDisabled: true,
        stateId: 'healthcheck',
        width: 120,
        align: 'center',
        renderer: Ext.bind(me.renderHealthCheckColumn, me),
        listeners: {
          click: Ext.bind(me.maybeAskToEnable, me)
        }
      });
      grid.headerCt.add(column);
      view.refresh();
      grid.healthCheckTooltip = Ext.create('Ext.ToolTip', {
        target: view.getEl(),
        delegate: view.getCellSelector(column),
        renderTo: NX.global.document.body,
        maxWidth: 550,
        // mouseOffset origin is [15, 18]. The following offset moves the tooltip to the left of the mouse
        //  while correcting for the [15, 18] origin
        mouseOffset: [-585, -18],
        showDelay: 400,
        hideDelay: 20000,
        dismissDelay: 20000,
        listeners: {
          beforeshow: Ext.bind(me.updateHealthCheckColumnTooltip, me)
        }
      });
    }
  },

  /**
   * Remove Health Check column from repository grid.
   *
   * @private
   * @param {NX.coreui.view.repository.RepositoryList} grid repository grid
   */
  removeHealthCheckColumn: function(grid) {
    var column = grid.healthCheckColumn;
    if (column) {
      grid.headerCt.remove(column);
      grid.getView().refresh();
      delete grid.healthCheckColumn;
      grid.healthCheckTooltip.destroy();
    }
  },

  /**
   * Render Health Check column based on corresponding {NX.coreui.model.HealthCheckRepositoryStatus}.
   *
   * @private
   * @param value (not used)
   * @param metadata Health Check column metadata
   * @param {NX.coreui.model.Repository} repositoryModel repository model
   * @returns {String} Health Check column content
   */
  renderHealthCheckColumn: function(value, metadata, repositoryModel) {
    var me = this,
        statusModel = me.getHealthCheckRepositoryStatusStore().getById(repositoryModel.getId()),
        classes, text, button;

    if (statusModel) {
      if (statusModel.get('enabled')) {
        if (statusModel.get('analyzing')) {
          return NX.I18n.get('HealthCheckRepositoryColumn_Analyzing');
        }
        return '<div><img src="' + NX.Icons.url('security-alert', 'x16') + '">&nbsp;'
            + statusModel.get('securityIssueCount') + '&nbsp;&nbsp;<img src="' + NX.Icons.url('license-alert', 'x16')
            + '" style="margin-left:10px">&nbsp;' + statusModel.get('licenseIssueCount') + '</div>';
      }
      else if (NX.Permissions.check('nexus:healthcheck', 'update')) {
        classes = "x-btn x-unselectable x-btn-nx-primary-small x-btn-nx-primary-toolbar-small-disabled";
        text = '<span class="x-btn-inner x-btn-inner-center" unselectable="on">' + NX.I18n.get('HealthCheckRepositoryColumn_Analyze') + '</span>';
        button = '<a class="' + classes + '" hidefocus="on" unselectable="on">' + text + '</a>';
        return button;
      }
    }
    else if (me.getHealthCheckRepositoryStatusStore().loaded) {
      return NX.ext.grid.column.Renderers.optionalData(null);
    }
    return NX.I18n.get('HealthCheckRepositoryColumn_Loading');
  },

  /**
   * Update Health Check column tooltip based on {NX.coreui.model.HealthCheckRepositoryStatus}.
   *
   * @private
   * @param {Ext.tip.Tooltip} tip
   * @returns {boolean} true if tooltip should be shown
   */
  updateHealthCheckColumnTooltip: function(tip) {
    var me = this,
        view = me.getList().getView(),
        repository, status, html, cell;

    if (tip.triggerElement) {
      repository = view.getRecord(tip.triggerElement.parentNode);
      if (repository) {
        status = me.getHealthCheckRepositoryStatusStore().getById(repository.getId());
        if (status) {
          if (status.get('enabled')) {
            if (status.get('analyzing')) {
              html = NX.I18n.get('HealthCheckRepositoryColumn_Analyzing_Tooltip');
            }
            else {
              if (NX.Permissions.check('nexus:healthchecksummary', 'read')) {
                cell = view.getCell(repository, me.getList().healthCheckColumn);
                Ext.defer(me.showSummary, 0, me, [status, cell.getX(), cell.getY()]);
                return false;
              }
              html = NX.I18n.get('HealthCheckRepositoryColumn_View_Permission_Error');
            }
          }
          else if (NX.Permissions.check('nexus:healthcheck', 'update')) {
            html = NX.I18n.get('HealthCheckRepositoryColumn_Analyze_Tooltip');
          }
          else {
            html = NX.I18n.get('HealthCheckRepositoryColumn_Analyze_Permission_Error');
          }
        }
        else if (me.getHealthCheckRepositoryStatusStore().loaded) {
          html = NX.I18n.get('HealthCheckRepositoryColumn_Unavailable_Tooltip');
        }
        tip.update(html);
        return true;
      }
    }
    return false;
  },

  /**
   * Refresh repository grid view (when Health Check status changes).
   *
   * @private
   */
  refreshHealthCheckColumn: function() {
    var list = this.getList();

    if (list) {
      list.getView().refresh();
    }
  },

  /**
   * Show Health Check summary window.
   *
   * @private
   * @param {NX.coreui.model.HealthCheckRepositoryStatus} statusModel Health Check status model
   * @param x where summary window should be shown
   * @param y where summary window should be shown
   */
  showSummary: function(statusModel, x, y) {
    var me = this,
        summary = me.getSummary(),
        docks;

    if (!summary) {
      summary = Ext.widget({
        xtype: 'nx-coreui-healthcheck-summary',
        x: x,
        y: y,
        height: statusModel.get('iframeHeight') + 8,
        width: statusModel.get('iframeWidth') + 8,
        statusModel: statusModel
      });
      docks = summary.getDockedItems('toolbar[dock="bottom"]');
      Ext.each(docks, function(dock) {
        summary.setHeight(summary.getHeight() + dock.getHeight());
      });
    }
  },

  /**
   * Ask user if Health Check should be enabled, if applicable and not already enabled.
   *
   * @private
   */
  maybeAskToEnable: function(gridView, cell, row, col, event, repositoryModel) {
    var me = this,
        list = me.getList(),
        status = me.getHealthCheckRepositoryStatusStore().getById(repositoryModel.getId());

    if (status && !status.get('enabled') && NX.Permissions.check('nexus:healthcheck', 'update')) {
      list.healthCheckTooltip.hide();
      Ext.Msg.show({
        title: NX.I18n.get('HealthCheckRepositoryColumn_Analyze_Dialog_Title'),
        msg: NX.I18n.format('HealthCheckRepositoryColumn_Analyze_Dialog_Msg', Ext.util.Format.htmlEncode(repositoryModel.get('name'))),
        buttons: 7, // OKYESNO
        buttonText: {
          ok: NX.I18n.get('HealthCheckRepositoryColumn_Analyze_Dialog_Ok_Text'),
          yes: NX.I18n.get('HealthCheckRepositoryColumn_Analyze_Dialog_Yes_Text')
        },
        icon: Ext.MessageBox.QUESTION,
        closeable: false,
        fn: function(buttonName) {
          if (buttonName === 'yes' || buttonName === 'ok') {
            if (status.get('eulaAccepted')) {
              me.enableAnalysis(buttonName === 'yes' ? status.getId() : undefined);
            }
            else {
              Ext.widget('nx-coreui-healthcheck-eula', {
                acceptFn: function() {
                  me.enableAnalysis(buttonName === 'yes' ? status.getId() : undefined);
                }
              });
            }
          }
        }
      });
    }
    return false;
  },

  /**
   * Enable Health Check for specified repository or all if repository not specified.
   *
   * @private
   * @param {String} repositoryName to enable Health Check for, or undefined if to enable for all repositories.
   */
  enableAnalysis: function(repositoryName) {
    var me = this;

    if (repositoryName) {
      NX.direct.healthcheck_Status.update(
          true /* eula accepted */, repositoryName, true /* enabled */,
          function(response) {
            if (Ext.isObject(response) && response.success) {
              me.getHealthCheckRepositoryStatusStore().load();
            }
          }
      );
    }
    else {
      NX.direct.healthcheck_Status.enableAll(
          true /* eula accepted */,
          function(response) {
            if (Ext.isObject(response) && response.success) {
              me.getHealthCheckRepositoryStatusStore().load();
            }
          }
      );
    }
  }

});
