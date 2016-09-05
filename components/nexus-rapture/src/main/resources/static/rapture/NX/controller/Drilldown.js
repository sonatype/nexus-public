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
      syncsize: me.syncSizeToOwner,
      activate: function() {
        me.currentIndex = 0;
        me.reselect();
        me.syncSizeToOwner();
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
        me.showChild(0, true);
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
    return Ext.ComponentQuery.query('nx-drilldown-item');
  },

  /**
   * @private
   */
  getDrilldownDetails: function() {
    return Ext.ComponentQuery.query('nx-drilldown-details')[0];
  },

  /**
   * @public
   * Load all of the stores associated with this controller
   */
  loadStores: function () {
    var me = this;
    if (this.getFeature()) {
      Ext.each(this.stores, function(store){
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
    this.loadView(index + 1, true, model);
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
   * @param animate Whether to animate the panel into view
   * @param model An optional record to select
   */
  loadView: function (index, animate, model) {
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
    me.showChild(index, animate);
    me.bookmark(index, model);
  },

  /**
   * @public
   * Make the create wizard appear
   *
   * @param index The zero-based step in the create wizard
   * @param animate Whether to animate the panel into view
   * @param cmp An optional component to load
   */
  loadCreateWizard: function (index, animate, cmp) {
    var me = this;

    // Reset all non-root bookmarks
    for (var i = 1; i <= index; ++i) {
      me.setItemBookmark(i, null);
    }

    // Show the specified step in the wizard
    me.showCreateWizard(index, animate, cmp);
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
      segments.push(encodeURIComponent(model.getId()));
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

      if (store.isLoading()) {
        // The store hasn’t yet loaded, load it when ready
        me.mon(store, 'load', function() {
          me.selectModelById(index, modelId);
          me.mun(store, 'load');
        });
      } else {
        me.selectModelById(index, modelId);
      }
    } else {
      me.loadView(0, false);
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
      me.loadView(index + 1, false, model);
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
        modelType = store.model.modelName.replace(/^.*?model\./, '').replace(/\-.*$/, '');

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
    var index = store.findBy(function(record) {
      return record.getId() === modelId;
    });

    if (index !== -1) {
      return store.getAt(index);
    }

    return null;
  },

  /**
   * @private
   */
  onDelete: function () {
    var me = this,
        selection = Ext.ComponentQuery.query('nx-drilldown-master')[0].getSelectionModel().getSelection(),
        description;

    if (Ext.isDefined(selection) && selection.length > 0) {
      description = me.getDescription(selection[0]);
      NX.Dialogs.askConfirmation('Confirm deletion?', description, function () {
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
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
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
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  // Constants which represent card indexes
  BROWSE_INDEX: 0,
  CREATE_INDEX: 1,
  BLANK_INDEX: 2,

  /**
   * @private
   * Given N drilldown items, this panel should have a width of N times the current screen width
   */
  syncSizeToOwner: function () {
    var me = this,
      drilldown = me.getDrilldown(),
      owner = drilldown.ownerCt.body.el,
      container = drilldown.down('container');

    container.setSize(owner.getWidth() * container.items.length, owner.getHeight());
    me.slidePanels(me.currentIndex, false);
  },

  /**
   * @public
   * Shift this panel to display the referenced step in the create wizard
   *
   * @param index The index of the create wizard to display
   * @param animate Set to “true” if the view should slide into place, “false” if it should just appear
   * @param cmp An optional component to load into the panel
   */
  showCreateWizard: function (index, animate, cmp) {
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

    me.slidePanels(index, animate);
  },

  /**
   * @public
   * Shift this panel to display the referenced master or detail panel
   *
   * @param index The index of the master/detail panel to display
   * @param animate Set to “true” if the view should slide into place, “false” if it should just appear
   */
  showChild: function (index, animate) {
    var me = this,
      items = me.getDrilldownItems(),
      item = items[index],
      createContainer;

    // Show the proper card
    item.setCardIndex(me.BROWSE_INDEX);

    // Destroy any create wizard panels
    for (var i = 0; i < items.length; ++i) {
      createContainer = items[i].down('#create' + i);
      createContainer.removeAll();
    }

    me.slidePanels(index, animate);
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

    // Hide everything that’s not the specified panel
    for (var i = 0; i < items.length; ++i) {
      if (i != index) {
        items[i].getLayout().setActiveItem(me.BLANK_INDEX);
      }
    }

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
  slidePanels: function (index, animate) {
    var me = this,
      drilldown = me.getDrilldown(),
      feature = drilldown.up('nx-feature-content'),
      items = me.getDrilldownItems(),
      item = items[index];

    if (item && item.el) {

      // Restore the current card
      me.currentIndex = index;
      item.getLayout().setActiveItem(item.cardIndex);

      var left = feature.el.getX() - (index * feature.el.getWidth());
      if (animate) {
        // Suspend layouts until the drilldown animation is complete
        Ext.suspendLayouts();

        drilldown.animate({
          easing: 'easeInOut',
          duration: NX.State.getValue('animateDuration', 200),
          to: {
            x: left
          },
          callback: function() {
            // Update the breadcrumb
            me.refreshBreadcrumb();

            // Put focus on the panel we’re navigating to
            me.hideAllExceptAndFocus(me.currentIndex);

            // Destroy any create wizard panels after current
            for (var i = index + 1; i < items.length; ++i) {
              items[i].down('#create' + i).removeAll();
            }

            // Resume layouts
            Ext.resumeLayouts(true);

            // Resize the breadcrumb to fit the window
            me.resizeBreadcrumb();
          }
        });
      } else {
        // Show the requested panel, without animation
        drilldown.setX(left, false);

        // Update the breadcrumb
        me.refreshBreadcrumb();
        me.resizeBreadcrumb();

        // Put focus on the panel we’re navigating to
        me.hideAllExceptAndFocus(index);

        // Destroy any create wizard panels after current
        for (var i = index + 1; i < items.length; ++i) {
          items[i].down('#create' + i).removeAll();
        }
      }
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

      // Resize the panel
      me.syncSizeToOwner();
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
      objs.push({
          xtype: 'button',
          scale: 'large',
          ui: 'nx-drilldown',
          text: content.currentTitle,
          handler: function() {
            me.slidePanels(0, true);

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
            text: '/'
          },
          {
            xtype: 'image',
            height: 16,
            width: 16,
            cls: 'nx-breadcrumb-icon ' + items[i].itemClass
          },

          // Create a closure within a closure to decouple 'i' from the current context
          (function(j) {
            return {
              xtype: 'button',
              scale: 'medium',
              ui: 'nx-drilldown',
              // Disabled if it’s the last item in the breadcrumb
              disabled: (i === me.currentIndex ? true : false),
              text: items[j].itemName,
              handler: function() {
                var bookmark = items[j].itemBookmark;
                if (bookmark) {
                  NX.Bookmarks.bookmark(bookmark.obj, bookmark.scope);
                }
                me.slidePanels(j, true);
              }
            };
          })(i)
        );
      }

      breadcrumb.removeAll();
      breadcrumb.add(objs);
    }
  },

  /*
   * @private
   * Resize the breadcrumb, truncate individual elements with ellipses as needed
   */
  resizeBreadcrumb: function() {
    var me = this,
      padding = 60, // Prevent truncation from happening too late
      parent = me.getDrilldown().ownerCt,
      breadcrumb = me.getDrilldown().up('#feature-content').down('#breadcrumb'),
      buttons, availableWidth, minimumWidth;

    // Is the breadcrumb clipped?
    if (parent && breadcrumb.getWidth() + padding > parent.getWidth()) {

      // Yes. Take measurements and get a list of buttons sorted by length (longest first)
      buttons = breadcrumb.query('button').splice(1);
      availableWidth = parent.getWidth();

      // What is the width of the breadcrumb, sans buttons?
      minimumWidth = breadcrumb.getWidth() + padding;
      for (var i = 0; i < buttons.length; ++i) {
        minimumWidth -= buttons[i].getWidth();
      }

      // Reduce the size of the longest button, until all buttons fit in the specified width
      me.reduceButtonWidth(buttons, availableWidth - minimumWidth);
    }
  },

  /*
   * @private
   * Reduce the width of a set of buttons, longest first, to a specified width
   *
   * @param buttons The list of buttons to resize
   * @param width The desired resize width (sum of all buttons)
   * @param minPerButton The minimum to resize each button (until all buttons are at this minimum)
   */
  reduceButtonWidth: function(buttons, width, minPerButton) {
    var me = this,
      currentWidth = 0,
      setToWidth;

    // Sort the buttons by width
    buttons = buttons.sort(function(a,b) {
      return b.getWidth() - a.getWidth();
    });

    // Calculate the current width of the buttons
    for (var i = 0; i < buttons.length; ++i) {
      currentWidth += buttons[i].getWidth();
    }

    // Find the next button to resize
    for (var i = 0; i < buttons.length; ++i) {

      // Shorten the longest button
      if (i < buttons.length - 1 && buttons[i].getWidth() > buttons[i+1].getWidth()) {

        // Will resizing this button make it fit?
        if (currentWidth - (buttons[i].getWidth() - buttons[i+1].getWidth()) <= width) {

          // Yes.
          setToWidth = width;
          for (var j = i + 1; j < buttons.length; ++j) {
            setToWidth -= buttons[j].getWidth();
          }
          buttons[i].setWidth(setToWidth);

          // Exit the algorithm
          break;
        }
        else {
          // No. Set the width of this button to that of the next button, and re-run the algorithm.
          buttons[i].setWidth(buttons[i+1].getWidth());
          me.reduceButtonWidth(buttons, width, minPerButton);
        }
      }
      else {
        // All buttons are the same length, shorten all by the same length
        setToWidth = Math.floor(width / buttons.length);
        for (var j = 0; j < buttons.length; ++j) {
          buttons[j].setWidth(setToWidth);
        }

        // Exit the algorithm
        break;
      }
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
  showInfo: function (message) {
    this.getDrilldownDetails().showInfo(message);
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
  }
});
