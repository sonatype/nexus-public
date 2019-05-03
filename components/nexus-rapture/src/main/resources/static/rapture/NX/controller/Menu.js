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
 * Menu controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Menu', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Bookmarks',
    'NX.controller.User',
    'NX.controller.Features',
    'NX.Permissions',
    'NX.Security',
    'NX.State',
    'NX.view.header.Mode',
    'NX.I18n',
    'Ext.state.Manager'
  ],

  views: [
    'feature.Menu',
    'feature.NotFound',
    'feature.NotVisible',
    'UnsavedChanges'
  ],

  models: [
    'Feature'
  ],
  stores: [
    'Feature',
    'FeatureMenu',
    'FeatureGroup'
  ],

  refs: [
    {
      ref: 'featureMenu',
      selector: 'nx-feature-menu'
    },
    {
      ref: 'featureContent',
      selector: 'nx-feature-content'
    },
    {
      ref: 'headerPanel',
      selector: 'nx-header-panel'
    }
  ],

  /**
   * Currently selected mode name.
   *
   * @private
   * @type {String}
   */
  mode: undefined,

  /**
   * All available {@link NX.view.header.Mode modes}.
   *
   * @private
   * @type {Ext.util.MixedCollection}
   */
  availableModes: undefined,

  /**
   * @private
   * @type {Boolean}
   */
  bookmarkingEnabled: true,

  /**
   * Current selected path.
   *
   * @private {String}
   */
  currentSelectedPath: undefined,

  /**
   * True if menu should auto navigate to first available feature.
   *
   * @private {Boolean}
   */
  navigateToFirstFeature: false,

  /**
   * @override
   */
  init: function () {
    var me = this;

    // initialize privates
    me.availableModes = Ext.create('Ext.util.MixedCollection');

    me.getApplication().getIconController().addIcons({
      'feature-notfound': {
        file: 'exclamation.png',
        variants: ['x16', 'x32']
      }
    });

    me.listen({
      controller: {
        '#Permissions': {
          changed: me.refreshMenu
        },
        '#State': {
          changed: me.onStateChange
        },
        '#Bookmarking': {
          navigate: me.navigateTo
        },
        '#User': {
          beforesignout: me.warnBeforeSignOut,
          signout: me.onSignOut
        },
        '#Refresh': {
          beforerefresh: me.warnBeforeRefresh
        }
      },
      component: {
        'nx-feature-menu': {
          itemclick: me.onItemClick,
          afterrender: me.onAfterRender,
          beforecellclick: me.warnBeforeMenuSelect,
          beforeselect: me.onBeforeSelect
        },
        'nx-main #quicksearch': {
          beforesearch: me.warnBeforeSearch
        },
        '#breadcrumb button': {
          click: me.warnBeforeButtonClick
        },
        'nx-actions button[handler]': {
          click: me.warnBeforeButtonClick
        },
        'nx-actions menuitem[handler]': {
          click: me.warnBeforeButtonClick
        },
        'nx-header-mode': {
          afterrender: me.registerMode,
          destroy: me.unregisterMode,
          selected: me.warnBeforeModeSelect
        }
      },
      store: {
        '#Feature': {
          update: me.refreshMenu
        }
      }
    });

    // Warn people about refreshing or closing their browser when there are unsaved changes
    me.warnBeforeUnload();
  },

  /**
   * Unregister Application listener.
   *
   * @override
   */
  destroy: function() {
    var me = this;

    me.getApplication().un('controllerschanged', me.refreshMenu, me);

    me.callParent(arguments);
  },

  /**
   * Register as Application listener and rebuild menu.
   */
  onAfterRender: function () {
    var me = this;

    me.getApplication().on('controllerschanged', me.refreshMenu, me);
    me.refreshMenu();
  },

  /**
   * @public
   * @returns {NX.Bookmark} a bookmark for current selected feature (if any)
   */
  getBookmark: function () {
    var me = this,
        selection = me.getFeatureMenu().getSelectionModel().getSelection();

    if (!selection.length) {
      me.getFeatureMenu().setSelection(me.getFeatureMenu().getStore().first());
      selection = me.getFeatureMenu().getSelectionModel().getSelection();
    }

    return NX.Bookmarks.fromToken(selection.length ? selection[0].get('bookmark') : me.mode);
  },

  /**
   * Select a feature when the associated menu item is clicked
   *
   * @param panel - the panel that was clicked
   * @param featureMenuModel - the model of the record that was clicked
   * @param forceReselectOrHtmlElement - if generic event fired by ext, this will simply be the htmlElement
   *        otherwise if fired by navigateTo function of this class (in case of browser url-tweaking or
   *        back/forward buttons) will be a boolean stating to not force a reselect
   *
   * @private
   */
  onItemClick: function (panel, featureMenuModel, forceReselectOrHtmlElement) {
    var me = this,
        path = featureMenuModel.get('path'),
        forceReselect = forceReselectOrHtmlElement,
        pathIsChanging = path !== me.currentSelectedPath,
        isGroup = featureMenuModel.get('group'),
        externalLink = featureMenuModel.get('hrefTarget') === '_blank',
        separator = featureMenuModel.get('separator');

    if (externalLink || separator) {
      return;
    }
    else if (forceReselect || pathIsChanging || isGroup) {
      me.currentSelectedPath = path;

      //<if debug>
      me.logInfo('Selected feature:', path);
      //</if>

      if (me.bookmarkingEnabled) {
        me.bookmark(featureMenuModel);
      }
      me.selectFeature(me.getStore('Feature').getById(featureMenuModel.get('path')));
      me.populateFeatureGroupStore(featureMenuModel);
    }
  },

  onBeforeSelect: function(panel, featureMenuModel) {
    var externalLink = featureMenuModel.get('hrefTarget') === '_blank',
        separator = featureMenuModel.get('separator');

    if (externalLink || separator) {
      return false;
    }
  },

  /**
   * @private
   */
  selectFeature: function (featureModel) {
    var path;

    if (featureModel) {
      path = featureModel.get('path');
      if (path && path.length > 0) {
        this.fireEvent('featureselected', featureModel);
      }
    }
  },

  /**
   * Updates the {@link NX.store.FeatureGroup} store with children of selected feature.
   *
   * @private
   * @param {NX.model.FeatureMenu} record
   */
  populateFeatureGroupStore: function (record) {
    var me = this,
        features = [],
        featureStore = me.getStore('Feature');

    // add all children of the record to the group store, but do not include the node for the current record
    record.eachChild(function (node) {
      node.cascadeBy(function (child) {
        features.push(featureStore.getById(child.get('path')));
      });
    });

    me.getStore('FeatureGroup').loadData(features);
  },

  /**
   * @private
   */
  navigateTo: function (bookmark) {
    var me = this,
        node, mode, feature, menuBookmark, queryIndex;

    if (bookmark) {
      // Get the path (minus an optional filter string)
      if (bookmark.getSegments().length) {
        queryIndex = bookmark.getSegment(0).indexOf('=');
        if (queryIndex !== -1) {
          menuBookmark = bookmark.getSegment(0).slice(0, bookmark.getSegment(0).indexOf('='));
        }
        else {
          menuBookmark = bookmark.getSegment(0);
        }
      }

      //<if debug>
      me.logInfo('Navigate to:', menuBookmark);
      //</if>

      mode = me.getMode(bookmark);
      // if we are navigating to a new mode, sync it

      if (me.mode !== mode) {
        me.mode = mode;
        me.refreshModes();
      }
      if (menuBookmark) {
        node = me.getStore('FeatureMenu').getRootNode().findChild('bookmark', menuBookmark, true);
      }
      // in case that we do not have a bookmark to navigate to or we have to navigate to first feature,
      // find the first feature
      if (!node && (!Ext.isDefined(menuBookmark) || me.navigateToFirstFeature)) {
        if (!me.mode) {
          me.selectFirstAvailableMode();
          me.refreshModes();
        }
        node = me.getStore('FeatureMenu').getRootNode().firstChild;

        //<if debug>
        me.logDebug('Automatically selected:', node.get('bookmark'));
        //</if>
      }
      // select the bookmarked feature in menu, if available
      if (node) {
        me.bookmarkingEnabled = me.navigateToFirstFeature;
        me.navigateToFirstFeature = false;

        me.getFeatureMenu().selectPath(node.getPath('text'), 'text', '/', function () {
          me.bookmarkingEnabled = true;
        });
        me.getFeatureMenu().fireEvent('itemclick', me.getFeatureMenu(), node, false);
      }
      else {
        delete me.currentSelectedPath;
        // if the feature to navigate to is not available in menu check out if is hidden (probably no permissions)
        if (menuBookmark) {
          feature = me.getStore('Feature').findRecord('bookmark', menuBookmark, 0, false, false, true);
        }
        me.getFeatureMenu().getSelectionModel().deselectAll();
        if (feature) {
          if (feature.get('authenticationRequired') && NX.Permissions.available()) {
            //<if debug>
            me.logDebug('Asking user to authenticate as feature exists but is not visible');
            //</if>

            NX.Security.askToAuthenticate();
          }
          me.selectFeature(me.createNotAvailableFeature(feature));
        }
        else {
          // as feature does not exist at all, show teh 403 like content
          me.selectFeature(me.createNotFoundFeature(menuBookmark));
        }
      }
    }
  },

  /**
   * @private
   */
  onSignOut: function () {
    this.navigateToFirstFeature = true;
  },

  /**
   * On a state change check features visibility and trigger a menu refresh if necessary.
   *
   * @private
   */
  onStateChange: function () {
    var me = this,
        shouldRefresh = false;

    me.getStore('Feature').each(function (feature) {
      var visible, previousVisible;
      if (feature.get('mode') === me.mode) {
        visible = feature.get('visible')();
        previousVisible = me.getStore('FeatureMenu').getRootNode().findChild('path', feature.get('path'), true) !== null;
        shouldRefresh = (visible !== previousVisible);
      }
      return !shouldRefresh;
    });

    if (shouldRefresh) {
      me.refreshMenu();
    }
  },

  /**
   * @private
   */
  bookmark: function (node) {
    var me = this,
        bookmark = node.get('bookmark');

    if (NX.Bookmarks.getBookmark().getToken() !== bookmark) {
      NX.Bookmarks.bookmark(NX.Bookmarks.fromToken(bookmark), me);
    }
  },

  /**
   * Refresh modes and feature menu.
   *
   * @public
   */
  refreshMenu: function () {
    var me = this;

    //<if debug>
    me.logDebug('Refreshing menu; mode:', me.mode);
    //</if>

    me.refreshVisibleModes();
    me.refreshTree();
    me.navigateTo(NX.Bookmarks.getBookmark());
  },

  /**
   * Find mode switcher widget for given name.
   *
   * @private
   * @param {String} name
   * @returns {NX.view.header.Mode|undefined}
   */
  findModeSwitcher: function(name) {
    return this.availableModes.findBy(function(item) {
      return item.name === name;
    });
  },

  /**
   * Refreshes modes buttons based on the fact that there are features visible for that mode or not.
   * In case that current mode is no longer visible, auto selects a new one.
   *
   * @private
   */
  refreshVisibleModes: function () {
    var me = this,
        visibleModes = [],
        feature;

    me.getStore('Feature').each(function (rec) {
      feature = rec.getData();
      if (feature.visible() && !feature.group && visibleModes.indexOf(feature.mode) === -1) {
        visibleModes.push(feature.mode);
      }
    });

    //<if debug>
    me.logDebug('Visible modes:', visibleModes);
    //</if>

    me.availableModes.each(function (mode) {
      mode.toggle(false, true);
      if (mode.autoHide) {
        if (visibleModes.indexOf(mode.name) > -1) {
          mode.show();
        }
        else {
          mode.hide();
        }
      }
    });

    me.refreshModeButtons();
  },

  /**
   * @private
   */
  refreshModeButtons: function () {
    var me = this,
        mode;

    me.availableModes.each(function (mode) {
      mode.toggle(false, true);
    });

    if (me.mode) {
      mode = me.findModeSwitcher(me.mode);
      if (!mode || mode.isHidden()) {
        delete me.mode;
      }
    }
    if (me.mode) {
      mode = me.findModeSwitcher(me.mode);
      mode.toggle(true, true);
    }
  },

  // NOTE: refreshTree() is only used externally by coreui.controller.Search

  /**
   * @public
   */
  refreshTree: function () {
    var me = this,
        menuTitle = me.mode,
        groupsToRemove = [],
        nodeExpandMap = Ext.state.Manager.get("MenuExpandMap") || {},
        feature, segments, parent, child, mode;

    //<if debug>
    me.logDebug('Refreshing tree; mode:', me.mode);
    //</if>

    Ext.suspendLayouts();

    mode = me.findModeSwitcher(me.mode);
    if (mode && mode.title) {
      menuTitle = mode.title;
    }
    me.getFeatureMenu().setTitle(menuTitle);

    me.getStore('FeatureMenu').getRootNode().removeAll();

    // create leafs and all parent groups of those leafs
    me.getStore('Feature').each(function (rec) {
      feature = rec.getData();
      // iterate only visible features
      if ((me.mode === feature.mode) && feature.visible()) {
        segments = feature.path.split('/');
        parent = me.getStore('FeatureMenu').getRootNode();
        for (var i = 2; i < segments.length; i++) {
          child = parent.findChild('path', segments.slice(0, i + 1).join('/'), false);
          if (child) {
            if (i < segments.length - 1) {
              child.data = Ext.apply(child.data, {
                leaf: false
              });
            }
          }
          else {
            if (i < segments.length - 1) {
              // create the group
              child = parent.appendChild({
                text: segments[i],
                leaf: false,
                // expand the menu by default
                expanded: true
              });
            }
            else {
              // create the leaf
              child = parent.appendChild({
                leaf: true,
                iconCls: feature.iconCls || NX.Icons.cls(feature.iconName, 'x16'),
                qtip: feature.description,
                authenticationRequired: feature.authenticationRequired,
                bookmark: feature.bookmark,
                expanded: nodeExpandMap[feature.path] === undefined ? feature.expanded : nodeExpandMap[feature.path],
                helpKeyword: feature.helpKeyword,
                iconName: feature.iconName,
                mode: feature.mode,
                path: feature.path,
                text: feature.text,
                view: feature.view,
                weight: feature.weight,
                grouped: feature.group
              });

              me.addExpandCollapseHandlers(child, feature.path);
            }
          }
          parent = child;
        }
      }
    });

    // remove all groups without children
    me.getStore('FeatureMenu').getRootNode().cascadeBy(function (node) {
      if (node.get('grouped') && !node.hasChildNodes()) {
        groupsToRemove.push(node);
      }
    });
    Ext.Array.each(groupsToRemove, function (node) {
      node.parentNode.removeChild(node, true);
    });

    me.getStore('FeatureMenu').sort([
      { property: 'weight', direction: 'ASC' },
      { property: 'text', direction: 'ASC' }
    ]);

    me.addExternalLinks();

    Ext.resumeLayouts(true);
  },

  /**
   * @private
   */
  addExpandCollapseHandlers: function (node, path) {
    node.on('expand', function(path) {
      var nodeExpandMap = Ext.state.Manager.get("MenuExpandMap") || {};
      nodeExpandMap[path] = true;
      Ext.state.Manager.set("MenuExpandMap", nodeExpandMap);
    }.bind(this, path));

    node.on('collapse', function(path) {
      var nodeExpandMap = Ext.state.Manager.get("MenuExpandMap") || {};
      nodeExpandMap[path] = false;
      Ext.state.Manager.set("MenuExpandMap", nodeExpandMap);
    }.bind(this, path));
  },

  /**
   * @private
   */
  createNotAvailableFeature: function (feature) {
    return this.getFeatureModel().create({
      text: feature.get('text'),
      path: feature.get('path'),
      description: feature.get('description'),
      iconName: feature.get('iconName'),
      view: {
        xtype: 'nx-feature-notvisible',
        // FIXME: i18n
        text: feature.get('text') + ' feature is not available as '
            + (NX.State.getValue('user') ? ' you do not have the required permissions' : ' you are not logged in')
      },
      visible: NX.controller.Features.alwaysVisible
    });
  },

  /**
   * @private
   */
  createNotFoundFeature: function (bookmark) {
    return this.getFeatureModel().create({
      text: 'Not found',
      path: '/Not Found',
      description: bookmark,
      iconName: 'feature-notfound',
      view: {
        xtype: 'nx-feature-notfound',
        path: bookmark
      },
      visible: NX.controller.Features.alwaysVisible
    });
  },

  /**
   * @private
   */
  getMode: function (bookmark) {
    if (bookmark && bookmark.getSegment(0)) {
      return bookmark.getSegment(0).split('/')[0];
    }
    return undefined;
  },

  /**
   * Change mode.
   *
   * @public
   * @param {String} mode to change to
   */
  changeMode: function (mode) {
    var me = this;

    //<if debug>
    me.logDebug('Mode changed:', mode);
    //</if>

    me.mode = mode;
    me.refreshTree();
    me.navigateTo(NX.Bookmarks.fromToken(me.getStore('FeatureMenu').getRootNode().firstChild.get('bookmark')));
    NX.Bookmarks.bookmark(me.getBookmark());
  },

  /**
   * Register a mode button.
   *
   * @private
   * @param {NX.view.header.Mode} mode
   */
  registerMode: function (mode) {
    this.availableModes.add(mode);
  },

  /**
   * Unregister a mode button.
   *
   * @private
   * @param {NX.view.header.Mode} mode
   */
  unregisterMode: function (mode) {
    this.availableModes.remove(mode);
  },

  /**
   * @private
   */
  selectFirstAvailableMode: function () {
    var me = this;
    me.availableModes.each(function (mode) {
      if (!mode.isHidden()) {
        me.mode = mode.name;
        return false;
      }
      return true;
    });

    //<if debug>
    me.logDebug('Auto selecting mode:', me.mode);
    //</if>
  },

  /**
   * @private
   */
  refreshModes: function () {
    this.refreshModeButtons();
    this.refreshTree();
  },

  /**
   * Check for unsaved changes before opening a menu item.
   *
   * @private
   */
  warnBeforeMenuSelect: function(tree, td, cellIndex, record) {
    var me = this;

    return me.warnBeforeNavigate(
      function () {
        me.getFeatureMenu().getSelectionModel().select(record);
        me.getFeatureMenu().fireEvent('itemclick', me.getFeatureMenu(), record);
      }
    );
  },

  /**
   * Check for unsaved changes before switching modes.
   *
   * @private
   * @param {NX.view.header.Mode} mode
   */
  warnBeforeModeSelect: function(mode) {
    var me = this;

    var cb = function() {
      mode.toggle(true);
      me.changeMode(mode.name);
    };

    if (me.warnBeforeNavigate(cb)) {
      me.changeMode(mode.name);
    }
    else {
      mode.toggle(true);
    }
  },

  /**
   * Check for unsaved changes before doing a search.
   *
   * @private
   */
  warnBeforeSearch: function() {
    var me = this,
      quickSearch = me.getHeaderPanel().down('nx-header-quicksearch');

    return me.warnBeforeNavigate(
      function() {
        quickSearch.triggerSearch();
      }
    );
  },

  /**
   * Check for unsaved changes before clicking a button.
   *
   * @private
   */
  warnBeforeButtonClick: function(button, e) {
    return this.warnBeforeNavigate(
      function() {
        button.handler.call(button.scope, button, e);
      }
    );
  },

  /**
   * Check for unsaved changes before refreshing the view.
   *
   * @private
   */
  warnBeforeRefresh: function() {
    var me = this,
      button = me.getHeaderPanel().down('nx-header-refresh');

    return me.warnBeforeNavigate(
      function() {
        button.fireEvent('click');
      }
    );
  },

  /**
   * Check for unsaved changes before signing out.
   *
   * @private
   */
  warnBeforeSignOut: function() {
    return this.warnBeforeNavigate(
      function() {
        NX.getApplication().getController('User').signOut();
      }
    );
  },

  /**
   * Check for unsaved changes. Warn the user, and stop or continue navigation.
   *
   * @private
   * @param {Function} callback
   */
  warnBeforeNavigate: function(callback) {
    var me = this,
      dirty = me.hasDirt(),
      content = me.getFeatureContent();

    // If true, we’ve already warned the user about the unsaved changes. Don’t warn again.
    if (content.discardUnsavedChanges) {
      // Reset the flag and continue with navigation
      content.resetUnsavedChangesFlag();
      return true;
    }

    // Load the content, but warn first if there are unsaved changes
    if (dirty) {
      // Show modal and stop navigation
      me.showUnsavedChangesModal(callback);
      return false;

    } else {
      // Continue with navigation
      return true;
    }
  },

  /**
   * Show warning modal about unsaved changes, and take action.
   *
   * @private
   * @param {Function} callback
   */
  showUnsavedChangesModal: function(callback) {
    var content = this.getFeatureContent();

    Ext.create('NX.view.UnsavedChanges', {
      content: content,
      callback: function() {
        // Run the callback
        callback();

        // Reset the unsaved changes flag
        content.resetUnsavedChangesFlag();
      }
    });
  },

  /**
   * Are any forms dirty?
   *
   * @private
   */
  hasDirt: function() {
    var dirty = false,
      forms = Ext.ComponentQuery.query('form[settingsForm=true]');

    // Check for dirty content
    if (forms.length !== 0) {
      Ext.Array.each(forms, function (form) {
        if (form.isDirty()) {
          dirty = true;
          return false; // break
        }
      });
    }

    return dirty;
  },

  /**
   * Warn people about refreshing or closing their browser when there are unsaved changes.
   *
   * @private
   */
  warnBeforeUnload: function() {
    var me = this;

    window.onbeforeunload = function() {
      if (me.hasDirt()) {
        return NX.I18n.get('Menu_Browser_Title');
      }
    };
  },

  addExternalLinks: function() {
    var rootNode = this.getStore('FeatureMenu').getRootNode(),
        clmState = NX.State.getValue('clm'),
        showDashboardUrl = clmState && clmState.enabled && clmState.url,
        shouldShowDashboardLink = showDashboardUrl && clmState.showLink;

    if (this.mode === 'browse' && shouldShowDashboardLink) {
      rootNode.appendChild({
        leaf: true,
        separator: true,
        cls: 'separator',
        iconCls: ' ',
        text: ' '
      });
      rootNode.appendChild({
        leaf: true,
        qtip: NX.I18n.get('Clm_Dashboard_Description'),
        authenticationRequired: false,
        mode: 'browse',
        text: NX.I18n.get('Clm_Dashboard_Link_Text'),
        href: showDashboardUrl,
        hrefTarget: '_blank',
        cls: 'iq-dashboard-link'
      });
    }
  }
});
