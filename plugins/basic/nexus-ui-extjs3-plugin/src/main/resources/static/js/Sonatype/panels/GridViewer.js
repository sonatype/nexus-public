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
/*global NX, Nexus, Sonatype, Ext*/
/*
 * A viewer panel offering a grid on top and a details pane at the bottom.
 * Config options:
 *
 * autoCreateNewRecord: if set, the "add" menu action handler
 * will automatically create a new blank record and insert it into the grid,
 * before invoking the subscriber supplied handler. The record will be assigned
 * the same "autoCreateNewRecord=true" flag, which is tracked by the editor
 * component, causing it to re-create the record from the server response when
 * the form is submitted.
 *
 * addMenuInitEvent: this parameter causes the panel to
 * create an "Add" button on the taskbar and fire the event so the subscribers
 * can append actions to the "add" menu.
 * columns: a column config, combining the
 * properties of Ext.data.Record and Ext.grid.ColumnModel. All items are used in
 * the Record constructor. Additionally, items with a 'header' property are used
 * in the ColumnsModel config, with the Record's 'name' becoming ColumnModel's
 * 'dataIndex'.
 *
 * dataAutoLoad: the 'autoLoad' property for the data store
 * (defaults to true). dataId: the id property in the record set (defaults to
 * 'resourceURI').
 * dataRoot: the root property of the response (defaults to
 * 'data').
 *
 * dataSortInfo: the 'sortInfo' property for the data store.
 *
 * deleteButton: creates a "Delete" button on the toolbar. The delete handler
 * will submit a DELETE request to requestURI of the selected record.
 *
 * rowClickEvent: event name to fire when a row is clicked.
 *
 * rowFocusChangedEvent: event name to fire when a row's focus changes. Called
 * every time a row is clicked.
 * rowClickHandler: a specific handler to be called
 * on rowClick
 *
 * rowContextClickEvent: event name to fire when a row is
 * right-clicked.
 *
 * rowContextClickHandler: a specific handler to be called when a
 * row is right-clicked
 *
 * mouseOverEvent: event name to fire when mouse over event
 * is fired
 *
 * mouseOverHandler: a specific handler to call when mouse over grid
 *
 * singleSelect: the 'singleSelect' property for the grid's selection model
 * (defaults to true).
 *
 * titleColumn: the name of the column to get a record title
 * from (defaults to 'name').
 *
 * url: the URl to load the data from.
 */
NX.define('Sonatype.panels.GridViewer', {
  requires : ['Nexus.panels.AutoTabPanel','Nexus.ext.GridFilterBox'],
  requirejs : ['Sonatype/view', 'Nexus/config', 'Nexus/navigation', 'Sonatype/utils'],
  extend : 'Ext.Panel',

  statics : {
    removeDanglingSeparators : function(menu) {
      var item, seenSeparator;
      if (!menu.items) {
        return menu;
      }
      item = menu.items.first();
      while (item && !item.text) {
        menu.remove(item); // clean up if the first element is a separator
        item = menu.items.first();
      }
      item = menu.items.last();
      while (item && !item.text) {
        menu.remove(item); // clean up if the last element is a separator
        item = menu.items.last();
      }

      menu.items.each(function(item) {
        if (!item.text) {
          if (seenSeparator) {
            menu.remove(item);
          } else {
            seenSeparator = true;
          }
        } else {
          seenSeparator = false;
        }
      });
    }
  },

  constructor : function(cfg) {
    var
          fields = [], columns = [], i, store, toolbar, c,
          config = cfg || {},
          defaultConfig = {
            dataAutoLoad : true,
            dataId : 'resourceURI',
            dataBookmark : 'id',
            dataRoot : 'data',
            dataSortInfo : {
              field : 'name',
              direction : 'asc'
            },
            titleColumn : 'name',
            singleSelect : true,
            collapsibleDetails : false
          };
    Ext.apply(this, config, defaultConfig);

    if (config.columns) {
      for (i = 0; i < config.columns.length; i = i + 1) {
        c = config.columns[i];
        fields.push({
          name : c.name,
          mapping : c.mapping,
          type : c.type,
          sortType : c.sortType,
          sortDir : c.sortDir,
          convert : c.convert,
          dateFormat : c.dateFormat,
          defaultValue : c.defaultValue
        });

        if (c.header !== null && c.header !== undefined) {
          if (c.autoExpand) {
            if (!c.id) {
              c.id = Ext.id();
            }
            this.autoExpandColumn = c.id;
          }
          c.dataIndex = c.name;
          columns.push(c);
        }
      }
    }

    this.dataStore = new Ext.data.JsonStore({
      root : this.dataRoot,
      id : this.dataId,
      fields : fields,
      url : this.url,
      autoLoad : this.dataAutoLoad && (!this.dataStores || this.dataStores.length === 0),
      sortInfo : this.dataSortInfo,
      isLoaded : false,
      listeners : {
        add : this.recordAddHandler,
        remove : this.recordRemoveHandler,
        update : this.recordUpdateHandler,
        load : {
          fn : function() {
            this.dataStore.isLoaded = true;
          },
          single : true,
          scope : this
        },
        scope : this
      }
    });

    this.gridPanel = new Ext.grid.GridPanel({
      region : this.collapsibleDetails ? 'center' : 'north',
      collapsible : this.collapsible || false,
      split : true,
      height : this.collapsibleDetails ? null : Sonatype.view.mainTabPanel.getInnerHeight() / 3,
      minHeight : this.collapsibleDetails ? null : 100,
      maxHeight : this.collapsibleDetails ? null : 500,
      frame : false,
      autoScroll : true,
      selModel : new Ext.grid.RowSelectionModel({
        singleSelect : this.singleSelect,
        listeners : {
          // HACK rowselect events were always fired with ExtJS2, even on store refresh/rows added,
          // so we have to hack the same for ExtJS3 or rewrite how the refreshHandler/rowSelectHandler is working.
          beforerowselect : function(model) {
            model.silent = false;
          }
        }
      }),

      ds : this.dataStore,
      // sortInfo: { field: 'name', direction: "ASC"},
      loadMask : true,
      deferredRender : false,
      colModel : new Ext.grid.ColumnModel({
        defaultSortable : true,
        columns : columns
      }),
      autoExpandColumn : this.autoExpandColumn,
      disableSelection : false,

      viewConfig : {
        emptyText: this.emptyText,
        deferEmptyText: false,
        emptyTextWhileFiltering: this.emptyTextWhileFiltering
      },

      listeners : {
        rowcontextmenu : {
          fn : this.rowContextMenuHandler,
          scope : this
        },
        mouseover : {
          fn : this.mouseOverGridHandler,
          scope : this
        },
        cellclick : {
          fn : this.cellClickHandler,
          scope : this
        }
      }
    });

    this.gridPanel.getSelectionModel().on('selectionchange', this.rowSelectHandler, this);
    this.gridPanel.getView().on('rowsinserted', function() {
      var idx, previousSelection = this.gridPanel.getSelectionModel().previousSelection;
      if (previousSelection) {
        idx = this.gridPanel.store.findBy(function(rec) {
          return rec.id === previousSelection.id;
        });
        if (idx !== -1) {
          this.gridPanel.getSelectionModel().selectRecords([ this.gridPanel.store.getAt(idx) ]);
        }
      }
    }, this);
    this.gridPanel.getView().on('beforerefresh', function() {
      this.gridPanel.getSelectionModel().previousSelection = this.gridPanel.getSelectionModel().getSelected();
    }, this);

    this.refreshButton = new Ext.Button({
      text : 'Refresh',
      iconCls : 'st-icon-refresh',
      cls : 'x-btn-text-icon',
      scope : this,
      handler : this.refreshHandler
    });
    toolbar = this.tbar;
    this.tbar = [this.refreshButton];
    this.createAddMenu();
    this.createDeleteButton();
    if (toolbar) {
      this.tbar = this.tbar.concat(toolbar);
    }
    this.tbar.push(
        '->',
        NX.create('Nexus.ext.GridFilterBox', {
          filteredGrid: this.gridPanel
        })
    );

    this.cardPanel = new Ext.Panel({
      layout : 'card',
      region : this.collapsibleDetails ? 'south' : 'center',
      title : this.collapsibleDetails ? ' ' : null,
      split : true,
      height : this.collapsibleDetails ? Sonatype.view.mainTabPanel.getInnerHeight() / 4 : null,
      activeItem : 0,
      deferredRender : false,
      autoScroll : false,
      frame : false,
      collapsed : this.collapsibleDetails,
      collapsible : this.collapsibleDetails,
      items : [
        {
          xtype : 'panel',
          layout : 'fit',
          html : '<div class="little-padding">Select a record to view the details.</div>'
        }
      ]
    });

    Sonatype.panels.GridViewer.superclass.constructor.call(this, {
      layout : 'border',
      autoScroll : false,
      width : '100%',
      height : '100%',
      items : [this.gridPanel, this.cardPanel]
    });

    if (this.dataStores) {
      for (i = 0; i < this.dataStores.length; i = i + 1) {
        store = this.dataStores[i];
        store.on('load', this.dataStoreLoadHandler, this);
        if (store.autoLoad !== true) {
          store.load();
        }
      }
    }
  },

  addActionHandler : function(handler, item, e) {
    if (item.autoCreateNewRecord) {
      var rec = new this.dataStore.reader.recordType({
        name : 'New ' + item.text
      }, 'new_' + new Date().getTime());
      rec.autoCreateNewRecord = true;
      if (handler && (handler(rec, item, e) === false)) {
        return;
      }

      this.dataStore.insert(0, [rec]);
      this.gridPanel.getSelectionModel().selectRecords([rec], false);
    }
    else {
      handler(item, e);
    }
  },

  applyBookmark : function(bookmark) {
    if (!this.dataStore.lastOptions) {
      this.dataStore.on('load', function(store, recs, options) {
        this.selectBookmarkedItem(bookmark);
      }, this, {
        single : true
      });
    } else {
      this.selectBookmarkedItem(bookmark);
    }
  },

  cancelHandler : function(panel) {
    var rec = panel.payload;
    if (rec) {
      if (this.dataStore.getById(rec.id)) {
        this.dataStore.remove(rec);
      }
      else {
        this.recordRemoveHandler(this.dataStore, rec, -1);
      }
    }
  },

  checkStores : function() {
    var i, store;
    if (this.dataStores) {
      for (i = 0; i < this.dataStores.length; i = i + 1) {
        store = this.dataStores[i];
        if (!store.lastOptions) {
          return false;
        }
      }
    }
    return true;
  },

  clearAll : function() {
    this.clearCards();
    this.dataStore.removeAll();
  },

  clearCards : function() {
    this.cardPanel.items.each(function(item, i, len) {
      if (i > 0) {
        this.remove(item, true);
      }
    }, this.cardPanel);

    this.cardPanel.getLayout().setActiveItem(0);
  },

  convertDataValue : function(value, store, idProperty, nameProperty) {
    if (value) {
      var rec = store.getAt(store.data.indexOfKey(value));
      if (!rec) {
        rec = store.getAt(store.find(idProperty, value));
      }
      if (rec) {
        return rec.data[nameProperty];
      }
    }
    return '';
  },

  createAddMenu : function() {
    if (this.addMenuInitEvent) {
      var
            menu = new Sonatype.menu.Menu({
              payload : this,
              scope : this,
              items : []
            }), item;

      Sonatype.Events.fireEvent(this.addMenuInitEvent, menu);

      Sonatype.panels.GridViewer.removeDanglingSeparators(menu);

      if (!menu.items || menu.items.length === 0) {
        return; // quit if empty
      }

      menu.items.each(function(item, index, length) {
        if (item.handler) {
          item.setHandler(this.addActionHandler.createDelegate(this, [item.handler], 0));
        }
      }, this);

      this.toolbarAddButton = new Ext.Button({
        text : 'Add...',
        icon : Sonatype.config.resourcePath + '/static/images/icons/add.png',
        cls : 'x-btn-text-icon',
        menu : menu
      });
      this.tbar.push(this.toolbarAddButton);
    }
  },

  createChildPanel : function(rec, recreateIfExists) {
    var child, tab, id, panel;

    rec.data.showCtx = this.showRecordContextMenu(rec);
    if (this.collapsibleDetails) {
      this.cardPanel.expand();
    }

    id = this.id + rec.id;

    panel = this.cardPanel.findById(id);

    if (recreateIfExists) {
      if (panel) {
        this.cardPanel.remove(panel, true);
        panel = null;
      }
    }

    if (!panel) {
      panel = new Nexus.panels.AutoTabPanel({
        id : id,
        title : rec.data[this.titleColumn],
        activeTab : -1
      });

      if (this.rowClickHandler) {
        this.rowClickHandler(panel, rec);
      }

      if (this.rowClickEvent) {
        Sonatype.Events.fireEvent(this.rowClickEvent, panel, rec, this);
      }

      if (panel.items) {
        if (!panel.tabPanel) {
          // if the panel has a single child, and the child fires a cancel
          // event,
          // catch it to clean up automatically
          child = panel.getComponent(0);
          child.on('cancel', this.cancelHandler.createDelegate(this, [child]), this);
        }
        else {
          panel.tabPanel.items.sort('ASC', function(a, b) {
            if (a.tabTitle <= b.tabTitle) {
              return a.tabTitle < b.tabTitle ? -1 : 0;
            }
            return 1;
          });
          panel.tabPanel.on('beforetabchange', function(tabpanel, newtab, currenttab) {
            // don't want to set this unless user clicked
            if (currenttab) {
              this.selectedTabName = newtab.name;
            }
          }, this);

          if (this.selectedTabName) {
            tab = panel.find('name', this.selectedTabName)[0];

            if (tab) {
              panel.tabPanel.setActiveTab(tab.id);
            }
            else {
              panel.tabPanel.setActiveTab(0);
            }
          }
          else {
            panel.tabPanel.setActiveTab(0);
          }
        }

        this.cardPanel.add(panel);
      }
      else {
        panel.add({
          xtype : 'panel',
          layout : 'fit',
          html : '<div class="little-padding">No details available.</div>'
        });
        this.cardPanel.add(panel);
      }
    }
    else {
      if (this.selectedTabName) {
        tab = panel.find('name', this.selectedTabName)[0];

        if (tab && panel.tabPanel && tab.id !== panel.tabPanel.getActiveTab().id) {
          panel.tabPanel.setActiveTab(tab.id);
        }
      }
    }

    // row clicked (not just init)
    if (this.rowFocusChangedEvent) {
      Sonatype.Events.fireEvent(this.rowFocusChangedEvent, panel, rec, this);
    }

    this.cardPanel.getLayout().setActiveItem(panel);
    panel.doLayout();
  },

  createDeleteButton : function() {
    if (this.deleteButton) {
      this.toolbarDeleteButton = new Ext.Button({
        text : 'Delete',
        icon : Sonatype.config.resourcePath + '/static/images/icons/delete.png',
        cls : 'x-btn-text-icon',
        handler : this.deleteActionHandler,
        scope : this
      });
      this.tbar.push(this.toolbarDeleteButton);
    }
  },

  dataStoreLoadHandler : function(store, records, options) {
    if (this.checkStores() && this.dataAutoLoad) {
      this.dataAutoLoad = false;
      this.dataStore.reload();
    }
  },

  deleteRecord : function(rec) {
    Ext.Ajax.request({
      callback : function(options, success, response) {
        if (success) {
          this.dataStore.remove(rec);
        }
        else {
          Sonatype.utils.connectionError(response, 'Delete Failed!');
        }
      },
      scope : this,
      method : 'DELETE',
      url : rec.data.resourceURI
    });
  },

  deleteActionHandler : function(button, e) {
    if (this.gridPanel.getSelectionModel().hasSelection()) {
      var rec = this.gridPanel.getSelectionModel().getSelected();

      if (rec.id.substring(0, 4) === 'new_') {
        this.dataStore.remove(rec);
      }
      else {
        Sonatype.utils.defaultToNo();

        Sonatype.MessageBox.show({
          animEl : this.gridPanel.getEl(),
          title : 'Delete',
          msg : 'Delete ' + rec.data[this.titleColumn] + '?',
          buttons : Sonatype.MessageBox.YESNO,
          scope : this,
          icon : Sonatype.MessageBox.QUESTION,
          fn : function(btnName) {
            if (btnName === 'yes' || btnName === 'ok') {
              this.deleteRecord(rec);
            }
          }
        });
      }
    }
  },

  getBookmark : function() {
    var rec = this.gridPanel.getSelectionModel().getSelected();
    return rec ? rec.data[this.dataBookmark] : null;
  },

  recordAddHandler : function(store, recs, index) {
    if (recs.length === 1 && recs[0].autoCreateNewRecord && recs[0].id.substring(0, 4) !== 'new_') {
      this.createChildPanel(recs[0]);
    }
  },

  recordRemoveHandler : function(store, rec, index) {
    var
          resetActiveItem,
          id = this.id + rec.id,
          panel = this.cardPanel.findById(id);

    if (panel) {
      resetActiveItem = this.cardPanel.getLayout().activeItem === panel;
      this.cardPanel.remove(panel, true);

      if (resetActiveItem) {
        this.cardPanel.getLayout().setActiveItem(0);
      }
    }
  },

  recordUpdateHandler : function(store, rec, op) {
    if (op === Ext.data.Record.COMMIT) {
      this.createChildPanel(rec, true);
    }
  },

  refreshHandler : function(button, e) {
    this.clearCards();

    var i, store;

    if (this.dataStores) {
      this.dataAutoLoad = true;
      for (i = 0; i < this.dataStores.length; i = i + 1) {
        store = this.dataStores[i];
        store.lastOptions = null;
        store.reload();
      }
    }
    else {
      this.gridPanel.store.reload();
    }
  },

  mouseOverGridHandler : function(e, t) {
    if (this.mouseOverHandler) {
      this.mouseOverHandler(e, t);
    }

    if (this.mouseOverEvent) {
      Sonatype.Events.fireEvent(this.mouseOverEvent, e, t);
    }
  },

  cellClickHandler : function(grid, rowIndex, columnIndex, eventObject) {
    var internalColumnIndex = this.getDisplayedColumnByIndex(columnIndex);
    if (internalColumnIndex !== -1 && this.columns[internalColumnIndex].clickHandler) {
      this.columns[internalColumnIndex].clickHandler(grid, rowIndex, eventObject);
    }
  },

  rowSelectHandler : function(selectionModel) {
    var rec = selectionModel.getSelected();
    if (rec && (this.rowClickEvent || this.rowClickHandler)) {
      this.createChildPanel(rec);

      var bookmark = rec.data[this.dataBookmark];
      if (bookmark) {
        Sonatype.utils.updateHistory(this);
      }
    }
  },

  //our internal column index contains all data, even for 'columns' not displayed, that
  //may just be mapped to a store, this will weed those out and retrieve the needed index
  getDisplayedColumnByIndex : function(index) {
    var i, j;
    for (i = 0, j = 0; i < this.columns.length; i = i + 1) {
      if (this.columns[i].header) {
        if (index === j) {
          return i;
        }

        j = j + 1;
      }
    }
    return -1;
  },

  rowContextMenuHandler : function(grid, index, e) {
    if (e.target.nodeName === 'A') {
      return; // no menu on links
    }

    if (this.rowContextClickEvent || this.rowContextClickHandler) {
      var
            item,
            rec = grid.store.getAt(index),
            menu = new Sonatype.menu.Menu({
              payload : rec,
              scope : this,
              items : []
            });

      if (this.rowContextClickHandler) {
        this.rowContextClickHandler(menu, rec);
      }

      if (this.rowContextClickEvent) {
        Sonatype.Events.fireEvent(this.rowContextClickEvent, menu, rec);
      }

      Sonatype.panels.GridViewer.removeDanglingSeparators(menu);

      if (!menu.items || !menu.items.first()) {
        return;
      }

      e.stopEvent();
      menu.showAt(e.getXY());
    }
  },

  selectBookmarkedItem : function(bookmark) {
    var
          oldBookmark,
          recIndex = this.dataStore.findBy(function(rec, id) {
            return rec.data[this.dataBookmark] === bookmark;
          }, this);

    if (recIndex >= 0) {
      oldBookmark = this.getBookmark();
      if (bookmark !== oldBookmark) {
        this.gridPanel.getSelectionModel().selectRecords([this.dataStore.getAt(recIndex)]);
      }
    }
  },

  // Override if want to restrict the context menu
  // default: show context menu
  showRecordContextMenu : function(rec) {
    return true;
  }
});
