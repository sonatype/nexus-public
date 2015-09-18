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
    'NX.State'
  ],

  views: [
    'feature.Menu',
    'feature.NotFound',
    'feature.NotVisible',
    'header.BrowseMode',
    'header.AdminMode',
    'header.Mode',
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
   * Current mode.
   *
   * @private
   */
  mode: undefined,

  /**
   * Modes discovered by searching all buttons with a mode property.
   *
   * @private
   */
  availableModes: [],

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
          select: me.onSelection,
          itemclick: me.onItemClick,
          afterrender: me.onAfterRender,
          beforecellclick: me.warnBeforeMenuSelect
        },
        'nx-header-panel button[mode]': {
          click: me.warnBeforeModeSelect
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
        'button[mode]': {
          afterrender: me.registerModeButton,
          destroy: me.unregisterModeButton
        }
      },
      store: {
        '#Feature': {
          update: me.refreshMenu
        }
      }
    });

    me.addEvents(
        /**
         * Fires when a feature is selected.
         *
         * @event featureselected
         * @param {NX.model.Feature} selected feature
         */
        'featureselected'
    );

    // Disable backspace as a means for navigating back
    me.disableBackspaceNav();

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

    return NX.Bookmarks.fromToken(selection.length ? selection[0].get('bookmark') : me.mode);
  },

  /**
   * Select a feature when the associated menu item is clicked
   *
   * @private
   */
  onItemClick: function (panel, featureMenuModel) {
    var me = this,
        path = featureMenuModel.get('path');

    me.currentSelectedPath = path;

    //<if debug>
    me.logInfo('Selected feature:', path);
    //</if>

    if (me.bookmarkingEnabled) {
      me.bookmark(featureMenuModel);
    }
    me.selectFeature(me.getStore('Feature').getById(featureMenuModel.get('path')));
    me.populateFeatureGroupStore(featureMenuModel);
  },

  /**
   * Select a feature when the associated menu item is selected. This differs
   * from onItemClick in that an already selected feature will not be reselected.
   *
   * @private
   */
  onSelection: function (panel, featureMenuModel) {
    var me = this,
      path = featureMenuModel.get('path');

    if ((path !== me.currentSelectedPath) || featureMenuModel.get('group')) {
      me.currentSelectedPath = path;
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

  /**
   * (Re)select a feature
   *
   * @private
   */
  selectMenuItem: function (featureMenuModel, reselect) {
    var me = this,
      path = featureMenuModel.get('path');

    if (reselect || path !== me.currentSelectedPath || featureMenuModel.get('group')) {
      me.currentSelectedPath = path;
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
        if (queryIndex != -1) {
          menuBookmark = bookmark.getSegment(0).slice(0, bookmark.getSegment(0).indexOf('='));
        } else {
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
        me.getFeatureMenu().selectPath(node.getPath('text'), 'text', undefined, function () {
          me.bookmarkingEnabled = true;
        });
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

    Ext.each(me.availableModes, function (button) {
      button.toggle(false, true);
      if (button.autoHide) {
        if (visibleModes.indexOf(button.mode) > -1) {
          button.up('nx-header-mode').show();
        }
        else {
          button.up('nx-header-mode').hide();
        }
      }
    });

    me.refreshModeButtons();
  },

  refreshModeButtons: function () {
    var me = this,
        headerPanel = me.getHeaderPanel(),
        modeButton;

    Ext.each(me.availableModes, function (button) {
      button.toggle(false, true);
    });

    if (me.mode) {
      modeButton = headerPanel.down('button[mode=' + me.mode + ']').up('nx-header-mode');
      if (!modeButton || modeButton.isHidden()) {
        delete me.mode;
      }
    }
    if (me.mode) {
      modeButton = headerPanel.down('button[mode=' + me.mode + ']');
      modeButton.toggle(true, true);
    }
  },

  refreshTree: function () {
    var me = this,
        menuTitle = me.mode,
        groupsToRemove = [],
        feature, segments, parent, child, modeButton;

    //<if debug>
    me.logDebug('Refreshing tree; mode:', me.mode);
    //</if>

    Ext.suspendLayouts();

    modeButton = me.getHeaderPanel().down('button[mode=' + me.mode + ']');
    if (modeButton && modeButton.title) {
      menuTitle = modeButton.title;
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
              child = parent.appendChild(Ext.apply(feature, {
                leaf: true,
                iconCls: NX.Icons.cls(feature.iconName, 'x16'),
                qtip: feature.description
              }));
            }
          }
          parent = child;
        }
      }
    });

    // remove all groups without children
    me.getStore('FeatureMenu').getRootNode().cascadeBy(function (node) {
      if (node.get('group') && !node.hasChildNodes()) {
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

    Ext.resumeLayouts(true);
  },

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

  getMode: function (bookmark) {
    if (bookmark && bookmark.getSegment(0)) {
      return bookmark.getSegment(0).split('/')[0]
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
   */
  registerModeButton: function (button) {
    this.availableModes.push(button);
  },

  /**
   * Unregister a mode button.
   *
   * @private
   */
  unregisterModeButton: function (button) {
    Ext.Array.remove(this.availableModes, button);
  },

  selectFirstAvailableMode: function () {
    var me = this;
    Ext.each(me.availableModes, function (button) {
      if (!button.isHidden()) {
        me.mode = button.mode;
        return false;
      }
      return true;
    });

    //<if debug>
    me.logDebug('Auto selecting mode:', me.mode);
    //</if>
  },

  refreshModes: function () {
    var me = this;
    me.refreshModeButtons();
    me.refreshTree();
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
    )
  },

  /**
   * Check for unsaved changes before switching modes.
   *
   * @private
   */
  warnBeforeModeSelect: function(button, e, eOpts) {
    var me = this;

    var cb = function() {
      me.getHeaderPanel().down('button[mode=' + button.mode + ']').toggle(true);
      me.changeMode(button.mode);
    };

    if (me.warnBeforeNavigate(cb)) {
      me.changeMode(button.mode);
    } else {
      me.getHeaderPanel().down('button[mode=' + me.mode + ']').toggle(true);
    }
  },

  /**
   * Check for unsaved changes before doing a search.
   *
   * @private
   */
  warnBeforeSearch: function() {
    var me = this,
      button = me.getHeaderPanel().down('nx-header-quicksearch');

    return me.warnBeforeNavigate(
      function() {
        button.fireEvent('search', button, button.getValue());
      }
    )
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
    )
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
    )
  },

  /**
   * Check for unsaved changes. Warn the user, and stop or continue navigation.
   *
   * @private
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

  // FIXME: jsdoc
  /**
   * Show warning modal about unsaved changes, and take action
   *
   * @private
   * @param record The menu item we’re trying to navigate to
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
    if (forms) {
      Ext.each(forms, function (form) {
        if (form.isDirty()) {
          dirty = true;
          return;
        }
      });
    }

    return dirty;
  },

  /**
   * Disable backspace as a means for navigating back.
   *
   * @private
   */
  disableBackspaceNav: function() {
    var parent = Ext.isIE ? document : window;
    Ext.EventManager.on(parent, 'keydown', function (e, focused) {
      if (e.getKey() == e.BACKSPACE && (!/^input$/i.test(focused.tagName) && !/^textarea$/i.test(focused.tagName)) || focused.disabled || focused.readOnly) {
        e.stopEvent();
      }
    });
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
  }
});
