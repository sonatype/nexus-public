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
 * Search controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Search', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.view.info.Panel',
    'NX.view.info.Entry',
    'NX.Bookmarks',
    'NX.Conditions',
    'NX.Permissions',
    'NX.I18n',
    'NX.State',
    'NX.coreui.util.BrowseableFormats'
  ],
  masters: [
    'nx-coreui-searchfeature nx-coreui-search-result-list',
    'nx-coreui-searchfeature nx-coreui-component-asset-list'
  ],
  stores: [
    'ComponentAsset',
    'SearchFilter',
    'SearchCriteria',
    'SearchResult'
  ],
  models: [
    'Asset',
    'Component',
    'SearchFilter'
  ],

  views: [
    'search.SearchFeature',
    'search.SearchResultList',
    'search.TextSearchCriteria',
    'search.SaveSearchFilter'
  ],

  refs: [
    { ref: 'feature', selector: 'nx-coreui-searchfeature' },
    { ref: 'searchResult', selector: 'nx-coreui-searchfeature nx-coreui-search-result-list' },
    { ref: 'componentDetails', selector: 'nx-coreui-searchfeature nx-coreui-component-details' },
    { ref: 'assetList', selector: 'nx-coreui-searchfeature nx-coreui-component-asset-list' },
    { ref: 'quickSearch', selector: 'nx-header-panel #quicksearch' },
    { ref: 'info', selector: 'nx-coreui-searchfeature #info' },
    { ref: 'warning', selector: 'nx-coreui-searchfeature #warning' }
  ],

  timedOutMessage: null,
  limitMessage: null,

  /**
   * @override
   */
  init: function() {
    var me = this;
    me.callParent();
    me.getApplication().getIconController().addIcons({
      'search-default': {
        file: 'magnifier.png',
        variants: ['x16', 'x32']
      },
      'search-component': {
        file: 'box_front.png',
        variants: ['x16', 'x32']
      },
      'search-component-detail': {
        file: 'box_front_open.png',
        variants: ['x16', 'x32']
      },
      'search-folder': {
        file: 'folder_search.png',
        variants: ['x16', 'x32']
      },
      'search-saved': {
        file: 'magnifier.png',
        variants: ['x16', 'x32']
      },
      'message-primary': {
        file: 'information.png',
        variants: ['x16', 'x32'],
        preload: true
      },
      'message-danger': {
        file: 'exclamation.png',
        variants: ['x16', 'x32'],
        preload: true
      }
    });
    me.registerFilter([
      {
        id: 'keyword',
        name: 'Keyword',
        text: 'Keyword',
        description: 'Search for components by keyword',
        readOnly: true,
        criterias: [
          { id: 'keyword' }
        ]
      },
      {
        id: 'custom',
        name: 'Custom',
        text: NX.I18n.get('Search_Custom_Text'),
        description: NX.I18n.get('Search_Custom_Description'),
        readOnly: true
      }
    ], me);
    me.getStore('SearchFilter').each(function(model) {
      me.registerFeature(model, me);
    });
    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadStores
        },
        '#State': {
          userchanged: me.onUserChanged
        }
      },
      component: {
        'nx-coreui-searchfeature nx-coreui-search-result-list': {
          beforerender: me.onBeforeRender,
          render: me.showHideResponseMessage
        },
        'nx-coreui-searchfeature': {
          afterrender: me.initCriterias
        },
        'nx-coreui-searchfeature menuitem[action=add]': {
          click: me.addCriteria
        },
        'nx-coreui-searchfeature component[searchCriteria=true]': {
          search: me.onSearchCriteriaChange,
          searchcleared: me.onSearchCriteriaChange,
          criteriaremoved: me.removeCriteria
        },
        'nx-coreui-searchfeature button[action=save]': {
          click: me.showSaveSearchFilterWindow
        },
        'nx-coreui-search-save button[action=add]': {
          click: me.saveSearchFilter
        },
        'nx-main #quicksearch': {
          afterrender: me.bindQuickSearch,
          search: me.onQuickSearch,
          searchcleared: me.onQuickSearch
        }
      },
      store: {
        '#SearchResult': {
          load: me.setResponseMessage
        }
      }
    });
  },

  /**
   * Update search results after user changed.
   *
   * @private
   */
  onUserChanged: function (user, oldUser) {
    var me = this,
        searchResultStore = me.getSearchResultStore();
    if (!user) {
      if (searchResultStore !== null) {
        searchResultStore.clearFilter(true);
        searchResultStore.load(function() {});
      }
    }
  },

  /**
   * @public
   * Register a set of criterias.
   * @param {Array/Object} criterias to be registered
   * @param {Ext.util.Observable} [owner] to be watched to automatically unregister the criterias if owner is destroyed
   */
  registerCriteria: function(criterias, owner) {
    var me = this,
        models;

    models = me.getStore('SearchCriteria').add(criterias);
    if (owner) {
      owner.on('destroy', function() {
        me.getStore('SearchCriteria').remove(models);
      }, me);
    }
  },

  /**
   * @public
   * Register a set of filters.
   * @param {Array/Object} filters to be registered
   * @param {Ext.util.Observable} [owner] to be watched to automatically unregister the criterias if owner is destroyed
   */
  registerFilter: function(filters, owner) {
    var me = this;

    Ext.each(Ext.Array.from(filters), function(filter) {
      me.registerFeature(me.getSearchFilterModel().create(filter), owner);
    });
  },

  isBrowseableBasedOnFormatCriteria: function(model) {
    var formatCriteria = Ext.Array.from(model.get('criterias')).filter(function(item) {
      return item.id === 'format';
    });

    if (formatCriteria.length > 0) {
      return NX.coreui.util.BrowseableFormats.check(formatCriteria[0].value);
    }
    else {
      return true;
    }
  },

  /**
   * @private
   * Register feature for model.
   * @param {NX.coreui.model.SearchFilter} model to be registered
   * @param {Ext.util.Observable} [owner] to be watched to automatically unregister the criterias if owner is destroyed
   */
  registerFeature: function(model, owner) {
    var me = this;

    if (model.getId() === 'keyword') {
      me.getApplication().getFeaturesController().registerFeature({
        mode: 'browse',
        path: '/Search',
        text: NX.I18n.get('Search_Text'),
        description: NX.I18n.get('Search_Description'),
        group: true,
        view: { xtype: 'nx-coreui-searchfeature', searchFilter: model, bookmarkEnding: '' },
        iconCls: 'x-fa fa-search',
        weight: 20,
        expanded: false,
        visible: function() {
          return NX.Permissions.check('nexus:search:read');
        }
      }, owner);
    }
    else {
      me.getApplication().getFeaturesController().registerFeature({
        mode: 'browse',
        path: '/Search/' + (model.get('readOnly') ? '' : 'Saved/') + model.get('name'),
        view: { xtype: 'nx-coreui-searchfeature', searchFilter: model, bookmarkEnding: '/' + model.getId() },
        iconCls: 'x-fa fa-search',
        text: model.get('text'),
        description: model.get('description'),
        authenticationRequired: false,
        visible: function() {
          return NX.Permissions.check('nexus:search:read') && me.isBrowseableBasedOnFormatCriteria(model);
        }
      }, owner);
    }
  },

  /**
   * @private
   * Show quick search when user has 'nexus:search:read' permission.
   */
  bindQuickSearch: function(quickSearch) {
    quickSearch.up('panel').mon(
        NX.Conditions.isPermitted('nexus:search:read'),
        {
          satisfied: quickSearch.show,
          unsatisfied: quickSearch.hide,
          scope: quickSearch
        }
    );
  },

  /**
   * @private
   *
   * Store a message to be displayed along with the store results
   */
  setResponseMessage: function() {
    var rawData = this.getSearchResultStore().proxy.reader.rawData,
        searchRequestTimeoutInSeconds = NX.State.getValue('uiSettings', {})['searchRequestTimeout'] ||
            NX.State.getValue('uiSettings', {})['requestTimeout'] - 5,
        timedOut = rawData && rawData.timedOut,
        limited = rawData && rawData.limited,
        format = Ext.util.Format.numberRenderer('0,000'),
        learnMore = '<a target="_blank" rel="noopener" href="' + NX.controller.Help.getDocsUrl() + '/Searching+for+Components">' +
            NX.I18n.get('Search_Results_TimedOut_LearnMore') + ' <i class="fa fa-external-link" /></a>';

    if (timedOut) {
      this.timedOutMessage = NX.I18n.format('Search_Results_TimedOut_Message', searchRequestTimeoutInSeconds, learnMore);
    }
    else {
      this.timedOutMessage = null;
    }

    if (limited) {
      this.limitMessage = NX.I18n.format('Search_Results_Limit_Message', format(rawData.total), format(rawData.unlimitedTotal));
    }
    else {
      this.limitMessage = null;
    }

    this.showHideResponseMessage();
  },

  /**
   * @private
   *
   * Show/hide the response messages
   */
  showHideResponseMessage: function() {
    var warning = this.getWarning(),
        info = this.getInfo();

    if (!warning || !info) {
      return;
    }

    if (this.timedOutMessage) {
      warning.setTitle(this.timedOutMessage);
      warning.show();
    }
    else {
      warning.hide();
    }

    if (this.limitMessage) {
      info.setTitle(this.limitMessage);
      info.show();
    }
    else {
      info.hide();
    }
  },

  /**
   * @private
   * Initialize search criterias (filters) based on filter definition and bookmarked criterias.
   */
  initCriterias: function() {
    var me = this,
        searchPanel = me.getFeature(),
        searchFilter = searchPanel.searchFilter,
        searchCriteriaPanel = searchPanel.down('#criteria'),
        searchResultStore = me.getSearchResultStore(),
        searchCriteriaStore = me.getSearchCriteriaStore(),
        addCriteriaMenu = [],
        criterias = {}, criteriasPerGroup = {};

    searchCriteriaPanel.removeAll();

    if (searchFilter && searchFilter.get('criterias')) {
      Ext.Array.each(Ext.Array.from(searchFilter.get('criterias')), function(criteria) {
        criterias[criteria['id']] = { value: criteria['value'], hidden: criteria['hidden'] };
      });
    }
    searchResultStore.getFilters().items.forEach(function(filter) {
      var existingCriteria = criterias[filter.config.id];
      if (existingCriteria) {
        existingCriteria['value'] = filter.config.value;
      }
      else {
        criterias[filter.config.id] = { value: filter.config.value, removable: true };
      }
    });

    Ext.Object.each(criterias, function(id, criteria) {
      var criteriaModel = searchCriteriaStore.getById(id);

      if (criteriaModel) {
        var cmpClass = Ext.ClassManager.getByAlias('widget.nx-coreui-searchcriteria-' + criteriaModel.getId());
        if (!cmpClass) {
          cmpClass = Ext.ClassManager.getByAlias('widget.nx-coreui-searchcriteria-text');
        }
        searchCriteriaPanel.add(cmpClass.create(Ext.apply(Ext.clone(criteriaModel.get('config')), {
          criteriaId: criteriaModel.getId(),
          value: criteria['value'],
          hidden: criteria['hidden'],
          removable: criteria['removable']
        })));
      }
    });

    searchCriteriaStore.each(function(criteria) {
      // skip tags entry if not running PRO
      if (criteria.getId() === 'tags' && !me.isTaggingEnabled()) {
        return;
      }

      var addTo = addCriteriaMenu,
          group = criteria.get('group'),
          format = criteria.get('config').format;

      if (!format || NX.coreui.util.BrowseableFormats.check(format)) {
        if (group) {
          if (!criteriasPerGroup[group]) {
            criteriasPerGroup[group] = [];
          }
          addTo = criteriasPerGroup[group];
        }
        addTo.push({
          text: criteria.get('config').fieldLabel,
          criteria: criteria,
          criteriaId: criteria.getId(),
          action: 'add',
          disabled: Ext.isDefined(criterias[criteria.getId()])
        });
      }
    });
    Ext.Object.each(criteriasPerGroup, function(key, value) {
      addCriteriaMenu.push({
        text: key,
        menu: value
      });
    });

    searchCriteriaPanel.add({
      xtype: 'button',
      cls: 'more-criteria',
      itemId: 'addButton',
      text: NX.I18n.get('Search_More_Text'),
      iconCls: 'x-fa fa-plus-circle',
      menu: addCriteriaMenu
    });
  },

  /**
   * @private
   * Sets the store filters based on the Bookmark
   */
  loadBookmark: function() {
    var me = this,
        searchFilter = me.getFeature().searchFilter,
        bookmarkSegments = NX.Bookmarks.getBookmark().getSegments(),
        store = me.getStore('SearchResult'),
        bookmarkValues = {},
        criterias = {},
        filterSegments,
        queryIndex,
        pair;

    // Extract the filter object from the URI
    if (bookmarkSegments && bookmarkSegments.length) {
      queryIndex = bookmarkSegments[0].indexOf('=');
      if (queryIndex !== -1) {
        filterSegments = decodeURIComponent(bookmarkSegments[0].slice(queryIndex + 1)).split(' AND ');
        for (var i = 0; i < filterSegments.length; ++i) {
          pair = filterSegments[i].split('=');
          bookmarkValues[pair[0]] = pair[1];
        }
      }
    }

    // From the search type (e.g. maven, nuget, custom)
    if (searchFilter) {
      if (searchFilter.id !== 'keyword' && searchFilter.id !== 'custom') {
        store.proxy.setExtraParam('formatSearch', true);
      } else {
        store.proxy.setExtraParam('formatSearch', false);
      }
      if (searchFilter.get('criterias')) {
        searchFilter.get('criterias').forEach(function(criteria) {
          if (criteria.value) {
            criterias[criteria.id] = {value: criteria.value, hidden: criteria.hidden};
          }
        });
      }
    }

    Ext.Object.each(bookmarkValues, function(key, value) {
      var existingCriteria = criterias[key];
      if (existingCriteria) {
        existingCriteria['value'] = value;
      }
      else {
        criterias[key] = { value: value, removable: true };
      }
    });

    const filters = Object.entries(criterias).map(function(criteriaEntry) {
      return {
        id: criteriaEntry[0],
        property: criteriaEntry[0],
        value: criteriaEntry[1].value
      }
    }).filter(function(criteria) {
      return criteria.value;
    });

    me.applyFilters(filters, false);
  },

  /**
   * @private
   * @returns {boolean} whether tagging feature is available
   */
  isTaggingEnabled: function() {
    return NX.State.getUser() && NX.Permissions.check('nexus:tags:read') && ('PRO' === NX.State.getEdition());
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('name');
  },

  /**
   * @private
   * Add a criteria.
   * @param menuitem selected criteria menu item
   */
  addCriteria: function(menuitem) {
    var searchPanel = this.getFeature(),
        searchCriteriaPanel = searchPanel.down('#criteria'),
        addButton = searchCriteriaPanel.down('#addButton'),
        searchInfo = searchPanel.down('#searchInfo'),
        criteria = menuitem.criteria,
        cmpClass = Ext.ClassManager.getByAlias('widget.nx-coreui-searchcriteria-' + criteria.getId()),
        cmp;

    menuitem.disable();
    if (!cmpClass) {
      cmpClass = Ext.ClassManager.getByAlias('widget.nx-coreui-searchcriteria-text');
    }
    searchCriteriaPanel.remove(addButton, false);
    cmp = cmpClass.create(
        Ext.apply(Ext.clone(criteria.get('config')), { criteriaId: criteria.getId(), value: undefined, removable: true })
    );
    searchCriteriaPanel.add(cmp);
    cmp.focus();
    searchCriteriaPanel.add(addButton);

    if (criteria.getId() === 'keyword') {
      searchInfo.setTitle(NX.I18n.get('Search_KeywordSearchRestrictions'));
    }
  },

  /**
   * @private
   * Remove a criteria.
   * @param searchCriteria removed search criteria
   */
  removeCriteria: function(searchCriteria) {
    var searchCriteriaPanel = this.getFeature().down('#criteria');

    searchCriteriaPanel.remove(searchCriteria);
    searchCriteriaPanel.down('menuitem[criteriaId=' + searchCriteria.criteriaId + ']').enable();
    this.applyFilter({ criteriaId: searchCriteria.criteriaId }, true);
  },

  /**
   * @private
   * Start searching on criteria value changed.
   * @param searchCriteria changed criteria
   */
  onSearchCriteriaChange: function(searchCriteria) {
    this.applyFilter(searchCriteria, true);
  },

  /**
   * @override
   * Search on refresh.
   */
  loadStores: function() {
    var me = this;
    if (me.getFeature()) {
      if (me.currentIndex === 0) {
        me.getSearchResult().getStore().load();
      }
      if (me.currentIndex >= 1) {
        me.getAssetList().getStore().load(function () {
          me.reselect();
        });
      }
    }
  },

  /**
   * @private
   * Load stores based on the bookmarked URL
   */
  onBeforeRender: function() {
    var me = this,
        bookmark = NX.Bookmarks.getBookmark(),
        list_ids = bookmark.getSegments().slice(1),
        searchResultStore = me.getSearchResult().getStore(),
        componentModel;

    searchResultStore.clearFilter(true);
    me.loadBookmark();

    searchResultStore.load(function() {
      // If no search filter has been specified, don't load any stores
      if (!searchResultStore.getFilters().length) {
        return;
      }

      // Load the asset detail view
      if (list_ids[1]) {
        componentModel = searchResultStore.getById(decodeURIComponent(list_ids[0]));
        me.onModelChanged(0, componentModel);
        me.onSearchResultSelection(componentModel);
        me.getAssetList().getStore().load(function () {
          me.reselect();
        });
      }
      // Load the asset list view
      else if (list_ids[0]) {
        me.reselect();
      }
    });
  },

  /**
   * @private
   * Synchronize store filters with search criteria.
   * @param searchCriteria criteria to be synced
   * @param performSearchAfter if filter should be applied on store ( = remote call)
   */
  applyFilter: function(searchCriteria, performSearchAfter) {
    const allCriteria = this.getFeature().query('#criteria component[criteriaId]');
    const filters = allCriteria.filter(function(criteria) {
      return criteria.getValue();
    }).map(function (criteria) {
      return {
        id: criteria.criteriaId,
        property: criteria.criteriaId,
        value: criteria.getValue()
      };
    });

    this.applyFilters(filters, performSearchAfter)
  },

  /**
   * @private
   * Applies a set of filters to the SearchResult store.
   * @param filters {[{property:string, value:string}]}
   * @param performSearchAfter {boolean} if the filters should be applied to the store (performs a remote search)
   */
  applyFilters: function (filters, performSearchAfter) {
    const SUPPRESS_EVENTS = true;
    const searchResultStore = this.getStore('SearchResult');
    this.getSearchResult().getSelectionModel().deselectAll();

    searchResultStore.clearFilter(SUPPRESS_EVENTS);
    filters.forEach(function(filter) {
      // Setting all filters all at once causes bugs.
      // Fortunately, only one search is generated if we add all the filters and then apply them by loading the store.
      searchResultStore.addFilter(filter, SUPPRESS_EVENTS);
    });

    if (performSearchAfter) {
      searchResultStore.load();
      this.onSearchResultSelection(null);
      this.bookmarkFilters();
    }
  },

  /**
   * @override
   * When a list managed by this controller is clicked, route the event to the proper handler
   */
  onSelection: function(list, model) {
    var modelType = list.getStore().model;

    if (modelType === this.getComponentModel()) {
      this.onSearchResultSelection(model);
    }
    else if (modelType === this.getAssetModel()) {
      this.onAssetSelection(model);
    }
  },

  /**
   * @private
   * Show details and load assets of selected component.
   * @param {NX.coreui.model.Component} model selected component
   */
  onSearchResultSelection: function(model) {
    var me = this;

    me.getComponentDetails().setComponentModel(model);
    me.getAssetList().setComponentModel(model);
  },

  /**
   * Load asset container for selected asset
   *
   * @private
   * @param {NX.coreui.model.Asset} model selected asset
   */
  onAssetSelection: function(model) {
    this.getController('NX.coreui.controller.Assets').updateAssetContainer(null, null, null, model);
  },

  /**
   * @private
   * Show "Save Search Filter" window.
   */
  showSaveSearchFilterWindow: function() {
    Ext.widget('nx-coreui-search-save');
  },

  /**
   * @private
   * Save a search filter.
   * @param {Ext.button.Button} button 'Add' button from "Save Search Filter"
   */
  saveSearchFilter: function(button) {
    var me = this,
        win = button.up('window'),
        values = button.up('form').getValues(),
        criterias = [],
        model;

    Ext.Array.each(Ext.ComponentQuery.query('nx-coreui-searchfeature component[searchCriteria=true]'), function(cmp) {
      criterias.push({
        id: cmp.criteriaId,
        value: cmp.getValue(),
        hidden: cmp.hidden
      });
    });

    model = me.getSearchFilterModel().create(Ext.apply(values, {
      id: values.name,
      criterias: criterias,
      readOnly: false
    }));

    me.getStore('SearchFilter').add(model);

    me.getApplication().getFeaturesController().registerFeature({
      path: '/Search/' + (model.get('readOnly') ? '' : 'Saved/') + model.get('name'),
      mode: 'browse',
      view: { xtype: 'nx-coreui-searchfeature', searchFilter: model },
      iconName: 'search-saved',
      description: model.get('description'),
      authenticationRequired: false
    }, me);

    me.getController('Menu').refreshTree();
    NX.Bookmarks.navigateTo(NX.Bookmarks.fromToken('browse/search/saved/' + model.get('name')));

    win.close();
  },

  /**
   * @private
   * Bookmark search values.
   */
  bookmarkFilters: function() {
    var filterArray = [],
        firstSegment, segments;

    // Remove any pre-existing query string
    firstSegment = NX.Bookmarks.getBookmark().getSegment(0);
    if (firstSegment.indexOf('=') !== -1) {
      firstSegment = firstSegment.slice(0, firstSegment.indexOf('='));
    }

    // Add each criteria to the filter object
    Ext.Array.each(Ext.ComponentQuery.query('nx-coreui-searchfeature component[searchCriteria=true]'), function(cmp) {
      if (cmp.getValue() && !cmp.isHidden()) {
        filterArray.push(cmp.criteriaId + '=' + cmp.getValue());
      }
    });

    // Stringify and url encode the filter object, then bookmark it
    segments = [firstSegment + "=" + encodeURIComponent(filterArray.join(' AND '))];
    NX.Bookmarks.bookmark(NX.Bookmarks.fromSegments(segments), this);
  },

  /**
   * @private
   * @param {NX.ext.SearchBox} quickSearch search box
   * @param {String} searchValue search value
   */
  onQuickSearch: function(quickSearch, searchValue) {
    var me = this,
        searchFeature = me.getFeature();

    if (!searchFeature || (searchFeature.searchFilter.getId() !== 'keyword')) {
      if (searchValue) {
        NX.Bookmarks.navigateTo(
            NX.Bookmarks.fromToken('browse/search=' + encodeURIComponent('keyword=' + searchValue)),
            me
        );
      }
    }
    else {
      me.showChild(0, true);
      searchFeature.down('#criteria component[criteriaId=keyword]').setValue(searchValue);
      searchFeature.down('#criteria component[criteriaId=keyword]').search(searchValue);
    }
  }

});
