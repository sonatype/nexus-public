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
 * Abstract Master/Detail controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Drilldown', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.Conditions',
    'NX.Dialogs',
    'NX.Bookmarks',
    'NX.view.drilldown.Drilldown',
    'NX.view.drilldown.Item',
    'NX.State'
  ],

  views: [
    'drilldown.Drilldown',
    'drilldown.Details'
  ],

  permission: undefined,

  /**
   * @protected
   * Get the human-readable name of a model
   */
  getDescription: Ext.emptyFn,

  /**
   * @override
   * An array of xtypes which represent the masters available to this drilldown
   */
  masters: null,

  /**
   * @override
   * The xtype of the detail panel used by this drilldown. Only needed when no masters exist.
   */
  detail: null,

  /**
   * @cfg {Function} optional function to be called on delete
   */
  deleteModel: undefined,

  currentIndex: 0,

  onClassExtended: function(cls, data, hooks) {
    var onBeforeClassCreated = hooks.onBeforeCreated;

    hooks.onBeforeCreated = function(cls, data) {
      //ext changes the stores list from short names to fully qualified names, so here we are just copying the list 
      //before ext changes it
      data.storesForLoad = data.stores ? data.stores.slice() : [];
      hooks.onBeforeCreated = onBeforeClassCreated;
      hooks.onBeforeCreated.apply(this, arguments);
    };
  },

  /**
   * @override
   */
  onLaunch: function () {
    this.getApplication().getIconController().addIcons({
      'drilldown-info': {
        file: 'information.png',
        variants: ['x16', 'x32']
      },
      'drilldown-warning': {
        file: 'warning.png',
        variants: ['x16', 'x32']
      }
    });
  },

  /**
   * @override
   */
  init: function () {
    var me = this,
        componentListener = {};

    // Normalize lists into an array
    if (!me.masters) {
      me.masters = [];
    }

    // Add event handlers to each list
    for (var i = 0; i < me.masters.length; ++i) {
      componentListener[me.masters[i]] = {
        selection: me.onSelection,
        cellclick: me.onCellClick
      };
    }

    // Drilldown
    componentListener[(me.masters[0] || me.detail) + ' ^ nx-drilldown'] = {
      activate: function() {
        me.currentIndex = 0;
        me.reselect();
      }
    };

    // New button
    componentListener[me.masters[0] + ' ^ nx-drilldown button[action=new]'] = {
      afterrender: me.bindNewButton
    };

    // Delete button
    componentListener[me.masters[0] + ' ^ nx-drilldown button[action=delete]'] = {
      afterrender: me.bindDeleteButton,
      click: me.onDelete
    };

    // Back button
    componentListener[(me.masters[0] || me.detail) + ' ^ nx-drilldown nx-addpanel button[action=back]'] = {
      click: function() {
        me.showChild(0);
      }
    };

    me.listen({
      component: componentListener,
      controller: {
        '#Bookmarking': {
          navigate: me.onNavigate
        }
      }
    });

    if (me.icons) {
      me.getApplication().getIconController().addIcons(me.icons);
    }
    if (me.features) {
      me.getApplication().getFeaturesController().registerFeature(me.features, me);
    }
  },

  /**
   * @private
   */
  getDrilldown: function() {
    return Ext.ComponentQuery.query('#nx-drilldown')[0];
  },

  /**
   * @private
   */
  getDrilldownItems: function() {
    return Ext.ComponentQuery.query('nx-drilldown-item').sort(this.compareGeneratedIds);
  },

  /**
   * @private
   */
  getDrilldownDetails: function() {
    return Ext.ComponentQuery.query('nx-drilldown-details')[0];
  },

  getDrilldownContainer: function() {
    return Ext.ComponentQuery.query('#drilldown-container')[0];
  },

  /**
   * @public
   * Load all of the stores associated with this controller
   */
  loadStores: function () {
    var me = this;
    if (this.getFeature()) {
      Ext.each(this.storesForLoad, function(store){
        //<if debug>
        me.logDebug('Loading Drilldown store: ', store);
        //</if>
        me.getStore(store).load();
      });
    }
  },

  /**
   * @public
   */
  reselect: function () {
    var lists = Ext.ComponentQuery.query('nx-drilldown-master');

    if (lists.length) {
      this.navigateTo(NX.Bookmarks.getBookmark());
    }
  },

  /**
   * @private
   * When a list item is clicked, display the new view and update the bookmark
   */
  onCellClick: function(view, td, cellIndex, model, tr, rowIndex, e) {
    var index = Ext.ComponentQuery.query('nx-drilldown-master').indexOf(view.up('grid'));

    //if the cell target is a link, let it do it's thing
    if(e && e.getTarget('a')) {
      return false;
    }
    this.loadView(index + 1, model);
  },

  /**
   * @public
   * A model changed, focus on the new row and update the name of the related drilldown
   */
  onModelChanged: function (index, model) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        view, firstCell, firstCellImgs;

    // If the list hasn’t loaded, don't do anything
    if (!lists[index]) {
      return;
    }

    view = lists[index].getView();
    firstCell = view.getCellByPosition({row:view.getRowId(model), column:0});
    firstCellImgs = firstCell ? firstCell.dom.getElementsByTagName('img') : null;

    lists[index].getSelectionModel().select([model], false, true);
    me.setItemName(index + 1, me.getDescription(model));
    if (firstCellImgs && firstCellImgs.length) {
      this.setItemClass(index + 1, firstCellImgs[0].className);
    }
  },

  /**
   * @public
   * Make the detail view appear
   *
   * @param index The zero-based view to load
   * @param model An optional record to select
   */
  loadView: function (index, model) {
    var me = this,
      lists = Ext.ComponentQuery.query('nx-drilldown-master');

    // Don’t load the view if the feature is not ready
    if (!me.getFeature()) {
      return;
    }

    // Model specified, select it in the previous list
    if (model && index > 0) {
      lists[index - 1].fireEvent('selection', lists[index - 1], model);
      me.onModelChanged(index - 1, model);
    }
    // Set all child bookmarks
    for (var i = 0; i <= index; ++i) {
      me.setItemBookmark(i, NX.Bookmarks.fromSegments(NX.Bookmarks.getBookmark().getSegments().slice(0, i + 1)), me);
    }

    // Show the next view in line
    me.showChild(index);
    me.bookmark(index, model);
  },

  /**
   * @public
   * Make the create wizard appear
   *
   * @param index The zero-based step in the create wizard
   * @param cmp An optional component to load
   */
  loadCreateWizard: function (index, cmp) {
    var me = this;

    // Reset all non-root bookmarks
    for (var i = 1; i <= index; ++i) {
      me.setItemBookmark(i, null);
    }

    // Show the specified step in the wizard
    me.showCreateWizard(index, cmp);
  },

  /**
   * @private
   * Bookmark specified model
   */
  bookmark: function (index, model) {
    var lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        bookmark = NX.Bookmarks.getBookmark().getSegments(),
        segments = [],
        i = 0;

    // Add the root element of the bookmark
    segments.push(bookmark.shift());

    // Find all parent models and add them to the bookmark array
    while (i < lists.length && i < index - 1) {
      segments.push(bookmark.shift());
      ++i;
    }

    // Add the currently selected model to the bookmark array
    if (model) {
      segments.push(encodeURIComponent(this.getModelId(model)));
    }

    // Set the bookmark
    NX.Bookmarks.bookmark(NX.Bookmarks.fromSegments(segments), this);
  },

  /**
   * Reselect on user navigation.
   *
   * @protected
   */
  onNavigate: function () {
    this.reselect();
  },

  /**
   * @public
   * @param {NX.Bookmark} bookmark to navigate to
   */
  navigateTo: function (bookmark) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        list_ids = bookmark.getSegments().slice(1),
        index, modelId, store;

    // Don’t navigate if the feature view hasn’t loaded
    if (!me.getFeature || !me.getFeature()) {
      return;
    }

    if (lists.length && list_ids.length) {
      //<if debug>
      me.logDebug('Navigate to: ' + bookmark.getSegments().join(':'));
      //</if>

      modelId = decodeURIComponent(list_ids.pop());
      index = list_ids.length;
      store = lists[index].getStore();

      if (store.isLoading() || !store.isLoaded()) {
        // The store hasn’t yet loaded, load it when ready
        me.mon(store, 'load', function() {
          me.selectModelById(index, modelId);
          me.mun(store, 'load');
        });
      } else {
        me.selectModelById(index, modelId);
      }
    } else {
      me.loadView(0);
    }
  },

  /**
   * @private
   * @param index of the list which owns the model
   * @param modelId to select
   */
  selectModelById: function (index, modelId) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        store, model;

    // If the list hasn’t loaded, don't do anything
    if (!lists[index]) {
      return;
    }

    // If the store doesn't have any records in it, do nothing
    store = lists[index].getStore();
    if (!store.getCount()) {
      return;
    }

    // getById() throws an error if a model ID is found, but not cached, check for content first
    model = me.getById(store, modelId);
    if (model === null) {
      // check for integer model id
      model = me.getById(store, parseInt(modelId));
    }
    if (model === null) {
      if (Ext.isFunction(me.findAndSelectModel)) {
        me.findAndSelectModel(index, modelId);
      }
      return;
    }

    me.selectModel(index, model);
  },

  /**
   * Selects a raw in specified list or loads the model in settings panel.
   *
   * @protected
   * @param index of the list which owns the model
   * @param model to select
   */
  selectModel: function (index, model) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master');

    if (index + 1 !== me.currentIndex) {
      me.loadView(index + 1, model);
    }
    else {
      lists[index].fireEvent('selection', lists[index], model);
      me.onModelChanged(index, model);
      me.refreshBreadcrumb();
    }
  },

  /**
   * Finds the model using other means that local store cache and selects the model if found.
   * Should be overridden by subclasses, usually to call into server to get the model by id.
   *
   * @protected
   * @param index of the list which owns the model
   * @param modelId to find and select
   */
  findAndSelectModel: function(index, modelId) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        store = lists[index].getStore(),
        modelType = store.model.modelName && store.model.modelName.replace(/^.*?model\./, '').replace(/\-.*$/, '');

    NX.Messages.add({
      text: modelType + " (" + modelId + ") not found",
      type: 'warning'
    });
  },

  /**
   * @private
   * Get a model from the specified store with the specified ID. Avoids exceptions
   * that arise from using Ext.data.Store.getById() with buffered stores.
   */
  getById: function (store, modelId) {
    var me = this,
        index = store.findBy(function(record) {
          return me.getModelId(record) === modelId;
        });

    if (index !== -1) {
      return store.getAt(index);
    }

    return null;
  },

  /**
   * @private
   * Get an ID from a model. Override if using a model with a synthetic ID
   */
  getModelId: function(model) {
    return model.getId();
  },

  /**
   * @private
   */
  onDelete: function () {
    var me = this,
        selection = me.getSelection(),
        description;

    if (Ext.isDefined(selection) && selection.length > 0) {
      description = me.getDescription(selection[0]);
      NX.Dialogs.askConfirmation('Confirm deletion?', Ext.htmlEncode(description), function () {
        me.deleteModel(selection[0]);

        // Reset the bookmark
        NX.Bookmarks.bookmark(NX.Bookmarks.fromToken(NX.Bookmarks.getBookmark().getSegment(0)));
      }, {scope: me});
    }
  },

  /**
   * @protected
   * Enable 'New' when user has 'create' permission.
   */
  bindNewButton: function (button) {
    button.mon(
        NX.Conditions.isPermitted(this.permission + ':create'),
        {
          satisfied: function() {
            button.enable();
          },
          unsatisfied: function() {
            button.disable();
          }
        }
    );
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission.
   */
  bindDeleteButton: function (button) {
    button.mon(
        NX.Conditions.isPermitted(this.permission + ':delete'),
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

  // Constants which represent card indexes
  BROWSE_INDEX: 0,
  CREATE_INDEX: 1,
  BLANK_INDEX: 2,

  /**
   * @public
   * Shift this panel to display the referenced step in the create wizard
   *
   * @param index The index of the create wizard to display
   * @param cmp An optional component to load into the panel
   */
  showCreateWizard: function (index, cmp) {
    var me = this,
      drilldown = me.getDrilldown(),
      items = me.padItems(index), // Pad the drilldown
      createContainer;

    // Add a component to the specified drilldown item (if specified)
    if (cmp) {
      createContainer = drilldown.down('#create' + index);
      createContainer.removeAll();
      createContainer.add(cmp);
    }

    // Show the proper card
    items[index].setCardIndex(me.CREATE_INDEX);

    me.slidePanels(index);
  },

  /**
   * @public
   * Shift this panel to display the referenced master or detail panel
   *
   * @param index The index of the master/detail panel to display
   */
  showChild: function (index) {
    var me = this,
      items = me.getDrilldownItems(),
      item = items[index],
      createContainer;

    // Show the proper card
    item.setCardIndex(me.BROWSE_INDEX);

    // Destroy any create wizard panels
    for (var i = 0; i < items.length; ++i) {
      createContainer = items[i].down('#create' + i);
      createContainer && createContainer.removeAll();
    }

    me.slidePanels(index);
  },

  /**
   * @private
   * Hide all except the specified panel. Focus on a default form field, if available.
   *
   * This is needed to restrict focus to the visible panel only.
   */
  hideAllExceptAndFocus: function (index) {
    var me = this,
      items = me.getDrilldownItems(),
      form;

    // Disable everything that’s not the specified panel
    Ext.each(items, function(item, i) {
      if (i != index) {
        item.disable();
      }
      else {
        item.enable();
      }
    });

    // Set focus on the default field (if available) or the panel itself
    form = items[index].down('nx-addpanel[defaultFocus]');
    if (form) {
      form.down('[name=' + form.defaultFocus + ']').focus();
    } else {
      me.getDrilldown().focus();
    }
  },

  /**
   * @private
   * Slide the drilldown to reveal the specified panel
   */
  slidePanels: function (index) {
    var drilldownContainer = this.getDrilldownContainer(),
        drilldownItems = this.getDrilldownItems(),
        item = drilldownItems[index],
        i, container, activeItem;

    if (item && item.el) {
      this.currentIndex = index;
      item.getLayout().setActiveItem(item.cardIndex);
    }

    activeItem = drilldownContainer.setActiveItem(index);
    if (activeItem) {
      activeItem.on({
        activate: function() {
          this.hideAllExceptAndFocus(this.currentIndex);
          this.refreshBreadcrumb();
        },
        single: true,
        scope: this
      });
    }

    // Destroy any create wizard panels after current
    for (i = index + 1; i < drilldownItems.length; ++i) {
      container = drilldownItems[i].down('#create' + i);
      container && container.removeAll();
    }
  },

  /**
   * @private
   * Pad the number of items in this drilldown to the specified index
   */
  padItems: function (index) {
    var me = this,
      drilldown = me.getDrilldown(),
      items = me.getDrilldownItems(),
      itemContainer;

    // Create new drilldown items (if needed)
    if (index > items.length - 1) {
      itemContainer = drilldown.down('container');

      // Create empty panels if index > items.length
      for (var i = items.length; i <= index; ++i) {
        itemContainer.add(drilldown.createDrilldownItem(i, undefined, undefined));
      }
    }

    return me.getDrilldownItems();
  },

  /**
   * @private
   * Update the breadcrumb based on the itemName and itemClass of drilldown items
   */
  refreshBreadcrumb: function() {
    var me = this,
      content = me.getDrilldown().up('#feature-content'),
      breadcrumb = content.down('#breadcrumb'),
      items = me.getDrilldownItems(),
      objs = [];

    if (me.currentIndex == 0) {
      // Feature's home page, no breadcrumb required
      content.showRoot();
    } else {
      // Make a breadcrumb (including icon and 'home' link)
      objs.push(
        {
          xtype: 'container',
          itemId: 'nx-feature-icon',
          width: 32,
          height: 32,
          cls: content.currentIcon,
          ariaRole: 'presentation'
        },
        {
          xtype: 'button',
          itemId: 'nx-feature-name',
          scale: 'large',
          ui: 'nx-drilldown',
          text: content.currentTitle,
          handler: function() {
            me.slidePanels(0);

            // Set the bookmark
            var bookmark = items[0].itemBookmark;
            if (bookmark) {
              NX.Bookmarks.bookmark(bookmark.obj, bookmark.scope);
            }
          }
        }
      );

      // Create the rest of the links
      for (var i = 1; i <= me.currentIndex && i < items.length; ++i) {
        // do no create breadcrumb for items that do not have a name
        if (!items[i].itemName) {
          return;
        }
        objs.push(
          // Separator
          {
            xtype: 'label',
            cls: 'nx-breadcrumb-separator',
            text: '/',
            ariaRole: 'presentation',
            tabIndex: -1
          },
          {
            xtype: 'container',
            height: 16,
            width: 16,
            cls: 'nx-breadcrumb-icon ' + items[i].itemClass,
            alt: items[i].itemClass.replace(/^nx-(.+)-x\d+$/, '$1').replace(/-/g, ' '),
            ariaRole: 'presentation'
          },

          // Create a closure within a closure to decouple 'i' from the current context
          (function(j) {
            return {
              xtype: 'button',
              scale: 'medium',
              ui: 'nx-drilldown',
              // Disabled if it’s the last item in the breadcrumb
              disabled: (i === me.currentIndex ? true : false),
              text: Ext.htmlEncode(items[j].itemName),
              handler: function() {
                var bookmark = items[j].itemBookmark;
                if (bookmark) {
                  NX.Bookmarks.bookmark(bookmark.obj, bookmark.scope);
                }
                me.slidePanels(j);
              }
            };
          })(i)
        );
      }

      breadcrumb.removeAll();
      breadcrumb.add(objs);
    }
  },

  /**
   * @public
   * Set the name of the referenced drilldown item
   */
  setItemName: function (index, text) {
    var me = this,
      items = me.padItems(index);

    items[index].setItemName(text);
  },

  /**
   * @public
   * Set the icon class of the referenced drilldown item
   */
  setItemClass: function (index, cls) {
    var me = this,
      items = me.padItems(index);

    items[index].setItemClass(cls);
  },

  /**
   * @public
   * Set the bookmark of the breadcrumb segment associated with the referenced drilldown item
   */
  setItemBookmark: function (index, bookmark, scope) {
    var me = this,
      items = me.padItems(index);

    items[index].setItemBookmark(bookmark, scope);
  },

  /**
   * @public
   */
  showInfo: function (message, tooltipText) {
    this.getDrilldownDetails().showInfo(message, tooltipText);
  },

  /**
   * @public
   */
  clearInfo: function () {
    this.getDrilldownDetails().clearInfo();
  },

  /**
   * @public
   */
  showWarning: function (message) {
    this.getDrilldownDetails().showWarning(message);
  },

  /**
   * @public
   */
  clearWarning: function () {
    this.getDrilldownDetails().clearWarning();
  },

  /**
   * Add a tab to the default detail panel
   *
   * Note: this will have no effect if a custom detail panel has been specified
   */
  addTab: function (tab) {
    var me = this;
    if (!me.detail) {
      me.getDrilldownDetails().addTab(tab);
    }
  },

  /**
   * Remove a panel from the default detail panel
   *
   * Note: this will have no effect if a custom detail panel has been specified
   */
  removeTab: function (tab) {
    var me = this;
    if (!me.detail) {
      me.getDrilldownDetails().removeTab(tab);
    }
  },

  /**
   * @private
   */
  compareGeneratedIds: function(a, b) {
    var idAIndex = parseInt(a.getId().replace('nx-drilldown-item', ''));
    var idBIndex = parseInt(b.getId().replace('nx-drilldown-item', ''));
    return idBIndex - idAIndex;
  },

  getModelIdFromBookmark: function() {
    var bookmarkSegments = NX.Bookmarks.getBookmark().segments,
        modelId = (bookmarkSegments.length > 1) && decodeURIComponent(bookmarkSegments[1]);

    return modelId;
  },

  getSelection: function() {
    return Ext.ComponentQuery.query('nx-drilldown-master')[0].getSelectionModel().getSelection();
  },

  getSelectedModel: function() {
    var selection = this.getSelection();
    return selection && selection[0];
  }
});
