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
 * Repositories controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Repositories', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Dialogs',
    'NX.Messages',
    'NX.Permissions',
    'NX.State',
    'NX.I18n'
  ],

  masters: [
    'nx-coreui-repository-list'
  ],
  models: [
    'Repository'
  ],
  stores: [
    'Blobstore',
    'Repository',
    'RepositoryRecipe',
    'RepositoryReference',
    'CleanupPolicy'
  ],
  views: [
    'repository.RepositoryAdd',
    'repository.RepositoryFeature',
    'repository.RepositoryList',
    'repository.RepositorySelectRecipe',
    'repository.RepositorySettings',
    'repository.RepositorySettingsForm',
    'repository.recipe.BowerGroup',
    'repository.recipe.BowerHosted',
    'repository.recipe.BowerProxy',
    'repository.recipe.Maven2Hosted',
    'repository.recipe.Maven2Proxy',
    'repository.recipe.Maven2Group',
    'repository.recipe.NpmHosted',
    'repository.recipe.NpmProxy',
    'repository.recipe.NpmGroup',
    'repository.recipe.NugetHosted',
    'repository.recipe.NugetProxy',
    'repository.recipe.NugetGroup',
    'repository.recipe.RubygemsHosted',
    'repository.recipe.RubygemsProxy',
    'repository.recipe.RubygemsGroup',
    'repository.recipe.RawHosted',
    'repository.recipe.RawProxy',
    'repository.recipe.RawGroup',
    'repository.recipe.DockerHosted',
    'repository.recipe.DockerGroup',
    'repository.recipe.DockerProxy',
    'repository.recipe.PyPiHosted',
    'repository.recipe.PyPiProxy',
    'repository.recipe.PyPiGroup',
    'repository.recipe.YumProxy',
    'repository.recipe.YumHosted',
    'repository.recipe.YumGroup',
    'repository.recipe.GitLfsHosted'
  ],
  refs: [
    {ref: 'feature', selector: 'nx-coreui-repository-feature'},
    {ref: 'content', selector: 'nx-feature-content'},
    {ref: 'list', selector: 'nx-coreui-repository-list'},
    {ref: 'settings', selector: 'nx-coreui-repository-feature nx-coreui-repository-settings'},
    {ref: 'proxyFacetContentMaxAge', selector: 'nx-coreui-repository-add numberfield[name=attributes.proxy.contentMaxAge]'}
  ],
  icons: {
    'repository-hosted': {
      file: 'database.png',
      variants: ['x16', 'x32']
    },
    'repository-proxy': {
      file: 'database_link.png',
      variants: ['x16', 'x32']
    },
    'repository-group': {
      file: 'folder_database.png',
      variants: ['x16', 'x32']
    }
  },

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.features = {
      mode: 'admin',
      path: '/Repository/Repositories',
      text: NX.I18n.get('Repositories_Text'),
      description: NX.I18n.get('Repositories_Description'),
      view: {xtype: 'nx-coreui-repository-feature'},
      iconConfig: {
        file: 'database.png',
        variants: ['x16', 'x32']
      },
      visible: function() {
        // Show feature if the current user is permitted any repository-admin permissions
        return NX.Permissions.checkExistsWithPrefix('nexus:repository-admin');
      }
    };

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        },
        '#State': {
          receivingchanged: me.onStateReceivingChanged
        }
      },
      store: {
        '#Repository': {
          load: me.reselect
        }
      },
      component: {
        'nx-coreui-repository-list': {
          beforerender: me.loadStores,
          afterrender: me.startStatusPolling,
          beforedestroy: me.stopStatusPolling
        },
        'nx-coreui-repository-list button[action=new]': {
          click: me.showSelectRecipePanel
        },
        'nx-coreui-repository-feature button[action=rebuildIndex]': {
          click: me.rebuildIndex,
          afterrender: me.bindIfProxyOrHostedAndEditable
        },
        'nx-coreui-repository-feature button[action=invalidateCache]': {
          click: me.invalidateCache,
          afterrender: me.bindIfProxyOrGroupAndEditable
        },
        'nx-coreui-repository-settings-form': {
          submitted: me.loadStores
        },
        'nx-coreui-repository-selectrecipe': {
          cellclick: me.showAddRepositoryPanel
        },
        'nx-coreui-repository-feature combo[name=attributes.maven.versionPolicy]' : {
          change: me.handleMaven2VersionPolicyChange
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function (model) {
    return model.get('name');
  },

  /**
   * @override
   */
  onSelection: function (list, model) {
    var me = this,
        settingsPanel = me.getSettings(),
        formCls = Ext.ClassManager.getByAlias('widget.nx-coreui-repository-' + model.get('recipe'));

    Ext.suspendLayouts();

    if (!formCls) {
      me.logWarn('Could not find settings form for: ' + model.getId());
    }
    else {
      if (Ext.isDefined(model)) {
        // Load the form
        settingsPanel.removeAllSettingsForms();
        settingsPanel.addSettingsForm({xtype: formCls.xtype, recipe: model});
        settingsPanel.loadRecord(model);
        me.loadCleanupPolicies(model.get('format'));

        // Set immutable fields to readonly
        Ext.Array.each(settingsPanel.query('field[readOnlyOnUpdate=true]'), function (field) {
          field.setReadOnly(true);
          field.addCls('nx-combo-disabled');
        });
      }
    }
    Ext.resumeLayouts();
  },

  /**
   * @private
   */
  showSelectRecipePanel: function () {
    var me = this;

    // Show the first panel in the create wizard, and set the breadcrumb
    me.setItemName(1, NX.I18n.get('Repositories_SelectRecipe_Title'));
    me.setItemClass(1, NX.Icons.cls('repository-hosted', 'x16'));

    // Show the panel
    me.loadCreateWizard(1, Ext.widget({
      xtype: 'panel',
      layout: {
        type: 'vbox',
        align: 'stretch',
        pack: 'start'
      },
      items: [
        {
          xtype: 'nx-coreui-repository-selectrecipe',
          flex: 1
        }
      ]
    }));
  },

  /**
   * @private
   */
  showAddRepositoryPanel: function (list, td, cellIndex, model) {
    var me = this,
        formCls = Ext.ClassManager.getByAlias('widget.nx-coreui-repository-' + model.getId());

    if (!formCls) {
      me.logWarn('Could not find settings form for: ' + model.getId());
    }
    else {
      // Show the second panel in the create wizard, and set the breadcrumb
      me.setItemName(2, NX.I18n.format('Repositories_Create_Title', model.get('name')));
      me.setItemClass(2, NX.Icons.cls('repository-hosted', 'x16'));
      me.loadCreateWizard(2, {xtype: 'nx-coreui-repository-add', recipe: model});
      me.loadCleanupPolicies(model.getId().split('-')[0]);
      if (model.getId() === 'maven2-proxy') {
        me.cleanUpdateProxyFacetContentMaxAge(-1);
      }
    }
  },

  /**
   * Updates the 'originalValue' of the proxyFacetContentMaxAge input and resets it so the field is not dirty.
   * @param newValue
   */
  cleanUpdateProxyFacetContentMaxAge: function(newValue) {
    var me = this,
        proxyFacetContentMaxAge = me.getProxyFacetContentMaxAge();

    proxyFacetContentMaxAge.originalValue = newValue;
    proxyFacetContentMaxAge.reset();
  },

  /**
   * @private
   * Update The maximum component age in the proxy facet based on the selected version policy, but do not update if the
   * user has entered a value.
   */
  handleMaven2VersionPolicyChange: function(element, newValue) {
    var me = this;

    var proxyFacetContentMaxAge = me.getProxyFacetContentMaxAge();

    if (proxyFacetContentMaxAge && !proxyFacetContentMaxAge.isDirty()) {
      switch (newValue) {
        case 'RELEASE':
          me.cleanUpdateProxyFacetContentMaxAge(-1);
          break;
        case 'SNAPSHOT':
          me.cleanUpdateProxyFacetContentMaxAge(1440);
          break;
        default:
          //no change
      }
    }
  },

  /**
   * @private
   */
  deleteModel: function (model) {
    var me = this,
        description = me.getDescription(model);

    me.getContent().getEl().mask(NX.I18n.get('Repositories_Delete_Mask'));
    NX.direct.coreui_Repository.remove(model.getId(), function (response) {
      me.getContent().getEl().unmask();
      me.getStore('Repository').load();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({text: 'Repository deleted: ' + description, type: 'success'});
      }
    });
  },

  /**
   * @private
   * Start polling for repository statuses.
   */
  startStatusPolling: function () {
    var me = this,
        uiSettings = NX.State.getValue('uiSettings'),
        statusInterval = 5;

    if (me.statusProvider) {
      me.statusProvider.disconnect();
    }

    if (uiSettings) {
      statusInterval = uiSettings.statusIntervalAnonymous || statusInterval;
      if (NX.State.getUser()) {
        statusInterval = uiSettings.statusIntervalAuthenticated || statusInterval;
      }
    }

    me.statusProvider = Ext.direct.Manager.addProvider({
      type: 'polling',
      url: NX.direct.api.POLLING_URLS.coreui_Repository_readStatus,
      interval: statusInterval * 1000,
      baseParams: {},
      listeners: {
        data: function (provider, event) {
          if (event.data && event.data.success && event.data.data) {
            me.updateRepositoryModels(event.data.data);
          }
        },
        scope: me
      }
    });

    //<if debug>
    me.logDebug('Repository status pooling started');
    //</if>
  },

  /**
   * @private
   * Stop polling for repository statuses.
   */
  stopStatusPolling: function () {
    var me = this;

    if (me.statusProvider) {
      me.statusProvider.disconnect();
    }

    //<if debug>
    me.logDebug('Repository status pooling stopped');
    //</if>
  },

  /**
   * @private
   * Updates Repository store records with values returned by status polling.
   * @param {Array} repositoryStatuses array of status objects
   */
  updateRepositoryModels: function (repositoryStatuses) {
    var me = this;

    Ext.Array.each(repositoryStatuses, function (repositoryStatus) {
      var repositoryModel = me.getStore('Repository').findRecord('name', repositoryStatus.repositoryName);
      if (repositoryModel) {
        if (!Ext.Object.equals(repositoryModel.get('status'), repositoryStatus)) {
          repositoryModel.set('status', repositoryStatus);
          repositoryModel.commit(true);
        }
      }
    });
  },

  /**
   * Start / Stop status pooling when server is disconnected/connected.
   * @param receiving if we are receiving or not status from server (server connected/disconnected)
   */
  onStateReceivingChanged: function (receiving) {
    var me = this;

    if (me.getList() && receiving) {
      me.startStatusPolling();
    }
    else {
      me.stopStatusPolling();
    }
  },

  /**
   * @private
   * Rebuild repository index for the selected Repository.
   */
  rebuildIndex: function () {
    var me = this,
        model = me.getList().getSelectionModel().getLastSelected();

    NX.direct.coreui_Repository.rebuildIndex(model.getId(), function (response) {
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({text: 'Repository index rebuilt: ' + me.getDescription(model), type: 'success'});
      }
    });
  },

  /**
   * @private
   * Invalidate caches for the selected proxy Repository.
   */
  invalidateCache: function () {
    var me = this,
        model = me.getList().getSelectionModel().getLastSelected();

    NX.direct.coreui_Repository.invalidateCache(model.getId(), function (response) {
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({text: 'Repository caches invalidated: ' + me.getDescription(model), type: 'success'});
      }
    });
  },

  /**
   * @private
   * Enables button if the select repository is a proxy or hosted repository.
   */
  bindIfProxyOrHostedAndEditable: function (button) {
    var me = this;

    //bind the enable/disable state to whether user has perms to edit a repo
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:repository-admin:*:*:edit'),
            NX.Conditions.watchEvents(me.getObservables(), me.watchEventsHandler({editRecord: true}))
        ),
        {
          satisfied: function () {
            button.enable();
          },
          unsatisfied: function () {
            button.disable();
          }
        }
    );

    //bind the show/hide state to whether the repo is proxy or hosted
    button.mon(
        NX.Conditions.watchEvents(me.getObservables(), me.watchEventsHandler({proxyOrHosted: true})),
        {
          satisfied: function () {
            button.show();
          },
          unsatisfied: function () {
            button.hide();
          }
        }
    );
  },

  /**
   * @private
   * Enables button if the select repository is a proxy or group repository.
   */
  bindIfProxyOrGroupAndEditable: function (button) {
    var me = this;

    //bind the enable/disable state to whether user has perms to edit a repo
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:repository-admin:*:*:edit'),
            NX.Conditions.watchEvents(me.getObservables(), me.watchEventsHandler({editRecord: true}))
        ),
        {
          satisfied: function () {
            button.enable();
          },
          unsatisfied: function () {
            button.disable();
          }
        }
    );

    //bind the show/hide state to whether the repo is proxy or group
    button.mon(
        NX.Conditions.watchEvents(me.getObservables(), me.watchEventsHandler({proxyOrGroup: true})),
        {
          satisfied: function () {
            button.show();
          },
          unsatisfied: function () {
            button.hide();
          }
        }
    );
  },

  /**
   * @override
   * @protected
   * Enable 'New' when user has 'add' permission.
   */
  bindNewButton: function (button) {
    button.mon(
        NX.Conditions.isPermitted('nexus:repository-admin:*:*:add'),
        {
          satisfied: function () {
            button.enable();
          },
          unsatisfied: function () {
            button.disable();
          }
        }
    );
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission for selected repository.
   */
  bindDeleteButton: function(button) {
    var me = this;

    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted('nexus:repository-admin:*:*:delete'),
            NX.Conditions.watchEvents(me.getObservables(), me.watchEventsHandler({deleteRecord: true}))
        ),
        {
          satisfied: function () {
            button.enable();
          },
          unsatisfied: function () {
            button.disable();
          }
        }
    );
  },

  /**
   * @private
   */
  getObservables: function () {
    var me = this;
    return [
      { observable: me.getStore('Repository'), events: ['load']},
      { observable: Ext.History, events: ['change']}
    ];
  },

  /**
   * @private
   */
  watchEventsHandler: function (options) {
    var me = this,
        store = me.getStore('Repository');

    return function() {
      var repositoryId = me.getModelIdFromBookmark(),
          model = repositoryId ? store.findRecord('name', repositoryId, 0, false, true, true) : undefined;

      if (model) {
        if (options.deleteRecord) {
          return NX.Permissions.check('nexus:repository-admin:' + model.get('format') + ':' + model.get('name') + ':delete');
        }
        else if (options.editRecord) {
          return NX.Permissions.check('nexus:repository-admin:' + model.get('format') + ':' + model.get('name') + ':edit');
        }
        else if (options.proxyOrGroup) {
          return model.get('type') === 'proxy' || model.get('type') === 'group';
        }
        else if (options.proxyOrHosted) {
          return model.get('type') === 'proxy' || model.get('type') === 'hosted';
        }
      }

      return false;
    };
  },

  loadCleanupPolicies: function(format) {
    this.getStore('CleanupPolicy').load({
      params: {
        filter: [
          {
            property: 'format',
            value: format
          }
        ]
      }
    });
  }

});
