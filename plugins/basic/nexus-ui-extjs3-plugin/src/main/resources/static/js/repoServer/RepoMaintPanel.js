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
/*
 * Repository Maintenance panel layout and controller
 */

define('repoServer/RepoMaintPanel',['Sonatype/all', 'Nexus/repository/AbstractRepoPanel', 'Sonatype/strings'], function(Sonatype, AbstractRepoPanel, Strings){
/*
 * config options: { id: the is of this panel instance [required] title: title
 * of this panel (shows in tab) }
 */

Sonatype.repoServer.RepositoryPanel = function(config) {
  var config = config || {};
  var defaultConfig = {
    title : 'Repositories'
  };
  Ext.apply(this, config, defaultConfig);

  var sp = Sonatype.lib.Permissions;

  this.repoStatusTask = {
    run : function() {
      if (sp.checkPermission('nexus:repostatus', sp.READ))
      {
        Ext.Ajax.request({
              url : Sonatype.config.repos.urls.repositoryStatuses + (this.forceStatuses ? '?forceCheck' : ''),
              callback : this.statusCallback,
              scope : this
            });
      }
      this.forceStatuses = false;
    },
    interval : 5000, // poll every 5 seconds
    scope : this
  };

  this.browseTypeButton = new Ext.Button({
        text : 'User Managed Repositories',
        icon : Sonatype.config.resourcePath + '/static/images/icons/page_white_stack.png',
        cls : 'x-btn-text-icon',
        value : 'user',
        tooltip : 'Click to browse other types of repositories.',
        scope : this,
        menu : {
          items : [{
                text : 'User Managed Repositories',
                value : 'user',
                checked : true,
                group : 'repo-view-selector',
                scope : this,
                handler : this.switchBrowseType
              }, {
                text : 'Nexus Managed Repositories',
                value : 'nexus',
                checked : false,
                group : 'repo-view-selector',
                scope : this,
                handler : this.switchBrowseType
              }]
        }
      });

  this.groupStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'resourceURI',
        fields : [{
              name : 'id'
            }, {
              name : 'name',
              sortType : Ext.data.SortTypes.asUCString
            }, {
              name : 'repoType',
              defaultValue : 'group'
            }, {
              name : 'exposed',
              type : 'boolean',
              defaultValue : true
            }, {
              name : 'userManaged',
              type : 'boolean',
              defaultValue : true
            }, {
              name : 'resourceURI'
            }, {
              name : 'format'
            }, {
              name : 'policy'
            }, {
              name : 'status'
            }, {
              name : 'repositories'
            }, {
              name : 'displayStatus',
              mapping : 'repositories',
              convert : function(v) {
                return Sonatype.utils.joinArrayObject(v, 'name');
              }
            }, {
              name : 'contentResourceURI'
            }],
        sortInfo : {
          field : 'name',
          direction : 'asc'
        },
        url : Sonatype.config.repos.urls.groups,
        listeners : {
          load : function(store, records, options) {
            switch (this.browseTypeButton.value)
            {
              case 'nexus' :
                store.filterBy(function(rec, id) {
                      return !rec.get("userManaged");
                    }, this);
                break;
              case 'user' :
                store.filterBy(function(rec, id) {
                      return rec.get("userManaged");
                    }, this);
                break;
            }
            this.dataStore.insert(0, store.data.items);
          },
          scope : this
        }
      });

  var toolbar = [];
  if (sp.checkPermission('nexus:wastebasket', sp.DELETE))
  {
    toolbar.push({
          id : 'repo-trash-btn',
          text : 'Trash...',
          icon : Sonatype.config.resourcePath + '/static/images/icons/user-trash.png',
          cls : 'x-btn-text-icon',
          tooltip : {
            title : 'Trash',
            text : 'Manage the Trash contents'
          },
          menu : {
            width : 125,
            items : [{
                  text : 'Empty Trash',
                  handler : this.deleteTrashHandler,
                  scope : this
                }]
          }
        });
  }
  toolbar.push(this.browseTypeButton);

  Sonatype.Events.addListener('nexusRepositoryStatus', this.statusStart, this);
  
  var columns = [{
      name : 'resourceURI'
  }, {
    name : 'remoteUri'
  }, {
    name : 'id'
  }, {
    name : 'exposed'
  }, {
    name : 'userManaged'
  }, {
    name : 'status'
  }, {
    name : 'name',
    sortType : Ext.data.SortTypes.asUCString,
    header : 'Repository',
    width : 175,
    sortable : true,
    renderer : function(value, metadata, record, rowIndex, colIndex, store) {
      return record.get('repoType') == 'group' ? ('<b>' + value + '</b>') : value;
    }
  }, {
    name : 'repoType',
    sortable : true,
    header : 'Type',
    width : 50
  }, {
    name : 'format',
    sortable : true,
    header : 'Format',
    width : 70
  }, {
    name : 'repoPolicy',
    sortable : true,
    header : 'Policy',
    width : 70,
    convert : Strings.upperFirstCharLowerRest
  }, {
    name : 'displayStatus',
    sortable : true,
    header : 'Repository Status',
    mapping : 'status',
    convert : Sonatype.repoServer.DefaultRepoHandler.statusConverter,
    width : 200
  }, {
    name : 'contentResourceURI',
    header : 'Repository Path',
    autoExpand : true,
    renderer : function(s) {
      if ( typeof s !== 'string' ) {
        return '';
      } else if (/^http(s)?:/.test(s)) {
        return '<a href="' + s + ((s != null && (s.charAt(s.length)) == '/') ? '' : '/') + '" target="_blank">' + s + '</a>';
      } else {
        return '' + s;
      }
    }
  }];
  
  //allow plugins to contribute to the columns and toolbar
  Sonatype.Events.fireEvent('repositoryGridInit', this, columns, toolbar);

  Sonatype.repoServer.RepositoryPanel.superclass.constructor.call(this, {
        emptyText: 'No repositories defined',
        emptyTextWhileFiltering: 'No repositories matched criteria: {criteria}',
        addMenuInitEvent : 'repositoryAddMenuInit',
        deleteButton : sp.checkPermission('nexus:repositories', sp.DELETE),
        rowClickEvent : 'repositoryViewInit',
        rowContextClickEvent : 'repositoryMenuInit',
        url : Sonatype.config.repos.urls.repositories,
        dataAutoLoad : false,
        tabbedChildren : true,
        tbar : toolbar,
        columns : columns
      });
  
  //allow plugins to access the properly built panel object
  Sonatype.Events.fireEvent('repositoryPanelInit', this);

  this.addListener('beforedestroy', function() {
        Ext.TaskMgr.stop(this.repoStatusTask);
        Sonatype.Events.removeListener('nexusRepositoryStatus', this.statusStart, this);
      }, this);
  this.dataStore.addListener('load', this.onRepoStoreLoad, this);
  this.dataStore.addListener('loadexception', this.onRepoStoreLoadException, this);
  this.dataStore.load();

  this.updatingBookmark = false;
  this.updating = new Ext.util.DelayedTask(function() {
        this.updatingBookmark = false;
      }, this, [this]);
  this.currentBookmark = [];
  this.cardPanel.on('afterlayout', function() {
        var tpanel = this.cardPanel.getLayout().activeItem.tabPanel;
        if (tpanel)
        {
          tpanel.on('tabchange', function(panel, tab) {
                if (!this.updatingBookmark)
                {
                  this.currentBookmark[1] = tab.name;
                  if (tab.name != 'browsestorage')
                  {
                    this.currentBookmark[2] = null;
                  }
                  Sonatype.utils.updateHistory(this);
                }
              }, this);
        }
      }, this);
  Sonatype.Events.addListener('repoBrowserStartSearch', function(text) {
        if (!this.updatingBookmark)
        {
          this.currentBookmark[2] = text;
          Sonatype.utils.updateHistory(this);
        }
      }, this);

  Sonatype.Events.addListener('repoBrowserStopSearch', function() {
        if (!this.updatingBookmark)
        {
          this.currentBookmark[2] = null;
          Sonatype.utils.updateHistory(this);
        }
      }, this);

};

Ext.extend(Sonatype.repoServer.RepositoryPanel, Sonatype.panels.GridViewer, {

	      deleteRecord : function(rec) {
        Ext.Ajax.request({
              callback : function(options, success, response) {
                if (success)
                {
                  this.dataStore.remove(rec);
                }
                else
                {
                	var options = {
                		hideErrorStatus : true
                	};
                  Sonatype.utils.connectionError(response, 'Delete request could not be completed!', false, options);
                }
              },
              scope : this,
              method : 'DELETE',
              url : rec.data.resourceURI
            });
      },

      applyBookmark : function(bookmark) {
        this.updatingBookmark = true;

        if (!this.groupStore.lastOptions)
        {
          this.groupStore.on('load', function(store, recs, options) {
                this.selectBookmarkedItem(bookmark);
              }, this, {
                single : true
              });

          this.groupStore.on('loadexception', function(store, recs, options) {
                this.selectBookmarkedItem(bookmark);
              }, this, {
                single : true
              });
        }
        else
        {
          this.selectBookmarkedItem(bookmark);
        }
      },

      selectBookmarkedItem : function(bookmark) {
        var parts = decodeURIComponent(bookmark).split('~');
        this.currentBookmark = parts;

        if (parts && parts.length > 0)
        {
          var recIndex = this.dataStore.findBy(function(rec, id) {
                return rec.data[this.dataBookmark] == parts[0];
              }, this);

          var selected = null;

          if (recIndex >= 0)
          {
            var rec = this.gridPanel.getSelectionModel().getSelected();
            var toSelect = this.dataStore.getAt(recIndex);
            if (rec == null || rec.id != toSelect.id)
            {
              selected = toSelect;
              this.gridPanel.getSelectionModel().selectRecords([toSelect]);
            }
          }

          if (parts.length == 1)
          {
            parts[1] = 'browsestorage';
            Sonatype.utils.replaceHistory(this);
          }

          if (parts.length > 1)
          {
            var tab, panel = this.cardPanel.getLayout().activeItem.tabPanel;

            if ( panel ) {
              tab = panel.find('name', parts[1])[0];

              if (tab)
              {
                panel.setActiveTab(tab);
              }
              else if (Sonatype.user.curr.isLoggedIn)
              {
                this.currentBookmark[1] = panel.getActiveTab().name;
                Sonatype.utils.replaceHistory(this);
              }
              else
              {
                var token = Ext.History.getToken();
                Sonatype.view.historyDisabled = false;
                Sonatype.view.afterLoginToken = token;
                Sonatype.repoServer.RepoServer.loginHandler();
                this.currentBookmark[1] = panel.getActiveTab().name;
                return;
              }
            }

            if (parts && parts.length > 2 && parts[1] == 'browsestorage' && toSelect != null)
            {
              var repoBrowser = panel.find('name', 'repositoryBrowser')[0];
              repoBrowser.selectPath('/' + toSelect.data.name + parts[2], 'text', function(success, node) {
                    if (success)
                    {
                      if (node.ownerTree.nodeClickEvent)
                      {
                        Sonatype.Events.fireEvent(node.ownerTree.nodeClickEvent, node, node.ownerTree.nodeClickPassthru);
                      }
                    }
                  });
            }
          }
        }
        this.updating.delay(200);
      },

      getBookmark : function() {
        var bookmark = Sonatype.repoServer.RepositoryPanel.superclass.getBookmark.call(this);
        if (bookmark == null)
        {
          return bookmark;
        }

        this.currentBookmark[0] = bookmark;

        bookmark = '';

        for (var i = 0; i < this.currentBookmark.length; i++)
        {
          if (!this.currentBookmark[i])
          {
            break;
          }

          if (i > 0)
          {
            bookmark += '~';
          }

          bookmark += this.currentBookmark[i];
        }

        return bookmark;
      },

      deleteTrashHandler : function(button, e) {
        Sonatype.utils.defaultToNo();

        Sonatype.MessageBox.show({
              animEl : this.gridPanel,
              title : 'Empty Trash',
              msg : 'Delete the entire contents of the Trash?<br><br>This operation cannot be undone!',
              buttons : Sonatype.MessageBox.YESNO,
              scope : this,
              icon : Sonatype.MessageBox.QUESTION,
              fn : function(btnName) {
                if (btnName == 'yes' || btnName == 'ok')
                {
                  Ext.Ajax.request({
                        callback : function(options, success, response) {
                          if (!success)
                          {
                            Sonatype.utils.connectionError(response, 'Error emptying the trash!');
                          }
                        },
                        scope : this,
                        method : 'DELETE',
                        url : Sonatype.config.repos.urls.trash
                      });
                }
              }
            });
      },

      onRepoStoreLoadException : function() {
        this.onRepoStoreLoad(null, null, null);
      },

      onRepoStoreLoad : function(store, records, options) {
        switch (this.browseTypeButton.value)
        {
          case 'nexus' :
            if (records == null)
            {
              this.dataStore.removeAll();
            }
            else
            {
              for (var i = 0; i < records.length; i++)
              {
                if (records[i].data.userManaged)
                {
                  store.remove(records[i]);
                }
              }
            }
            break;
          case 'user' :
            if (records == null)
            {
              this.dataStore.removeAll();
            }
            this.groupStore.reload();
            break;
        }
        this.statusStart();
      },

      refreshHandler : function(button, e) {
        if (button == this.refreshButton)
        {
          this.forceStatuses = true;
        }
        Sonatype.repoServer.RepositoryPanel.superclass.refreshHandler.call(this, button, e);
      },

      statusCallback : function(options, success, response) {
        if (response.status != 202)
        {
          Ext.TaskMgr.stop(this.repoStatusTask);
        }

        if (success)
        {
          var statusResp = Ext.decode(response.responseText);
          if (statusResp.data)
          {
            var data = statusResp.data;
            for (var i = data.length - 1; i >= 0; i--)
            {
              var item = data[i];
              var rec = this.dataStore.getById(item.resourceURI.replace(Sonatype.config.repos.urls.repositoryStatuses, Sonatype.config.repos.urls.repositories));
              if (rec)
              {
                rec.beginEdit();
                rec.set('status', item.status);
                rec.set('displayStatus', Sonatype.repoServer.DefaultRepoHandler.statusConverter(item.status, item));
                rec.commit(true);
                rec.endEdit();
              }
            }
          }
          else
          {
            // no content, clear display statuses
            this.dataStore.each(function(rec) {
              if (rec.get('repoType') !== 'group') {
                rec.beginEdit();
                rec.set('displayStatus', '');
                rec.commit(true);
                rec.endEdit();
              }
            });
          }
        }
        else
        {
          this.dataStore.each(function(rec) {
            if ( rec.get('repoType') !== 'group' ) {
              rec.beginEdit();
              rec.set('status', {localStatus : 'IN_SERVICE' , proxyMode : 'ALLOW', remoteStatus:'UNKNOWN'});
              rec.set('displayStatus', '<i>Status retrieval failed</i>');
              rec.commit(true);
              rec.endEdit();
            }
          });
        }

        this.gridPanel.getView().refresh();
      },

      statusStart : function() {
        Ext.TaskMgr.start(this.repoStatusTask);
      },

      switchBrowseType : function(button, e) {
        this.browseTypeButton.setText(button.text);
        this.browseTypeButton.value = button.value;

        switch (button.value)
        {
          case 'nexus' :
            if (this.toolbarAddButton)
              this.toolbarAddButton.disable();
            if (this.toolbarDeleteButton)
              this.toolbarDeleteButton.disable();
            this.dataStore.proxy.setApi(Ext.data.Api.actions.read, Sonatype.config.repos.urls.allRepositories);
            break;
          case 'user' :
            if (this.toolbarAddButton)
              this.toolbarAddButton.enable();
            if (this.toolbarDeleteButton)
              this.toolbarDeleteButton.enable();
            this.dataStore.proxy.setApi(Ext.data.Api.actions.read, Sonatype.config.repos.urls.repositories);
            break;
        }
        this.refreshHandler(button, e);
      }

    });

Sonatype.repoServer.RepositoryBrowsePanel = function(config) {
  var config = config || {};
  var defaultConfig = {
    titleColumn : 'name'
  };
  Ext.apply(this, config, defaultConfig);

  this.oldSearchText = '';
  this.searchTask = new Ext.util.DelayedTask(this.startSearch, this, [this]);
  this.nodeContextMenuEvent = 'repositoryContentMenuInit';

  this.searchField = new Ext.app.SearchField({
        xtype : 'nexussearchfield',
        searchPanel : this,
        width : 400,
        enableKeyEvents : true,
        listeners : {
          'keyup' : {
            fn : function(field, event) {
              var key = event.getKey();
              if (!event.isNavKeyPress())
              {
                this.searchTask.delay(200);
              }
            },
            scope : this
          },
          'render' : function(c) {
            Ext.QuickTips.register({
                  target : c.getEl(),
                  text : 'Enter a complete path to lookup, for example org/sonatype/nexus'
                });
          }
        }
      });

  Sonatype.repoServer.RepositoryBrowsePanel.superclass.constructor.call(this, {
        anchor : '0 -2',
        bodyStyle : 'background-color:#FFFFFF',
        animate : true,
        lines : false,
        autoScroll : true,
        containerScroll : true,
        rootVisible : true,
        enableDD : false,
        tbar : [{
              text : 'Refresh',
              iconCls : 'st-icon-refresh',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.refreshHandler
            }, ' ', 'Path Lookup:', this.searchField],
        loader : new Ext.tree.SonatypeTreeLoader({
              url : '',
              listeners : {
                loadexception : this.treeLoadExceptionHandler,
                scope : this
              }
            }),
        listeners : {
          click : this.nodeClickHandler,
          contextMenu : this.nodeContextMenuHandler,
          scope : this
        }
      });

  new Ext.tree.TreeSorter(this, {
        folderSort : true
      });

  this.refreshHandler();
};

Ext.extend(Sonatype.repoServer.RepositoryBrowsePanel, Ext.tree.TreePanel, {
      getBrowsePath : function(baseUrl) {
        return baseUrl + this.getBrowsePathSnippet() + '/';
      },

      getBrowsePathSnippet : function() {
        return Sonatype.config.browsePathSnippet;
      },

      nodeClickHandler : function(node, e) {
        if (e.target.nodeName == 'A')
          return; // no menu on links

        if (this.nodeClickEvent)
        {
          Sonatype.Events.fireEvent(this.nodeClickEvent, node, this.nodeClickPassthru);
        }
      },

      nodeContextMenuHandler : function(node, e) {
        if (e.target.nodeName == 'A')
          return; // no menu on links

        if (!this.payload.data.showCtx)
        {
          return;
        }

        if (this.nodeContextMenuEvent)
        {

          node.attributes.repoRecord = this.payload;
          node.data = node.attributes;

          var menu = new Sonatype.menu.Menu({
                payload : node,
                scope : this,
                items : []
              });

          Sonatype.Events.fireEvent(this.nodeContextMenuEvent, menu, this.payload, node);

          var item;
          while ((item = menu.items.first()) && !item.text)
          {
            menu.remove(item); // clean up if the first element is a separator
          }
          while ((item = menu.items.last()) && !item.text)
          {
            menu.remove(item); // clean up if the last element is a separator
          }
          if (!menu.items.first())
            return;

          e.stopEvent();
          menu.showAt(e.getXY());
        }
      },

      refreshHandler : function(button, e) {
        if (this.payload)
        {
          this.setRootNode(new Ext.tree.AsyncTreeNode({
                text : this.payload.data[this.titleColumn],
                id : this.getBrowsePath(this.payload.data.resourceURI),
                singleClickExpand : true,
                expanded : true,
                listeners : {
                  expand : {
                    fn : function() {
                      if (this.payload.data.expandPath)
                      {
                        this.selectPath(this.payload.data.expandPath, 'text', function(success, node) {
                              if (success)
                              {
                                if (node.ownerTree.nodeClickEvent)
                                {
                                  Sonatype.Events.fireEvent(node.ownerTree.nodeClickEvent, node, node.ownerTree.nodeClickPassthru);
                                }
                              }
                            });
                      }
                    },
                    scope : this
                  }
                }
              }));
        }
        else
        {
          this.setRootNode(new Ext.tree.TreeNode({
                text : '(Not Available)',
                id : '/',
                singleClickExpand : true,
                expanded : true
              }));
        }

        if (this.innerCt)
        {
          // need to check if root node is reloadable and not 'Not Available'
          // (may happen on closing staging repositories, which use this view)
          if ( this.root.reload ) {
            this.root.reload();
          }
          this.afterRender();
        }
      },

      updatePayload : function(payload) {
        this.payload = payload;
        this.refreshHandler();
      },

      startSearch : function(p) {
        var field = p.searchField;
        var searchText = field.getRawValue();

        Sonatype.Events.fireEvent('repoBrowserStartSearch', searchText);

        if (searchText.charAt(0) == '/')
        {
          searchText = searchText.slice(1, searchText.length);
        }

        var treePanel = p;
        if (searchText)
        {
          field.triggers[0].show();
          var justEdited = p.oldSearchText.length > searchText.length;

          var findMatchingNodes = function(root, textToMatch) {
            var n = textToMatch.indexOf('/');
            var remainder = '';
            if (n > -1)
            {
              remainder = textToMatch.substring(n + 1);
              textToMatch = textToMatch.substring(0, n);
            }

            var matchingNodes = [];
            var found = false;
            for (var i = 0; i < root.childNodes.length; i++)
            {
              var node = root.childNodes[i];

              var text = node.text;
              if (text == textToMatch)
              {
                node.enable();
                node.ensureVisible();
                node.expand();
                found = true;
                if (!node.isLeaf())
                {
                  var autoComplete = false;
                  if (!remainder && node.childNodes.length == 1)
                  {
                    remainder = node.firstChild.text;
                    autoComplete = true;
                  }
                  if (remainder)
                  {
                    if (node.expanded)
                    {
                      var s = findMatchingNodes(node, remainder);
                      if (autoComplete || (s && s != remainder))
                      {
                        return textToMatch + '/' + (s ? s : remainder);
                      }
                    }
                    else
                    {
                      node.on('expand', function(node) {
                            findMatchingNodes(node, remainder)
                          }, this);

                    }

                  }
                }
              }
              else if (text.substring(0, textToMatch.length) == textToMatch)
              {
                matchingNodes[matchingNodes.length] = node;
                node.enable();
                if (matchingNodes.length == 1)
                {
                  node.ensureVisible();
                }
              }
              else
              {
                node.disable();
                node.collapse(false, false);
              }
            }

            // if only one non-exact match found, suggest the name
            return !found && matchingNodes.length == 1 ? matchingNodes[0].text + '/' : null;
          };

          var s = findMatchingNodes(treePanel.root, searchText);

          p.oldSearchText = searchText;

          // if auto-complete is suggested, and the user hasn't just started
          // deleting
          // their own typing, try the suggestion
          if (s && !justEdited && s != searchText)
          {
            field.setRawValue(s);
            p.startSearch(p);
          }

        }
        else
        {
          p.stopSearch(p);
        }
      },

      stopSearch : function(p) {
        p.searchField.triggers[0].hide();
        p.oldSearchText = '';

        Sonatype.Events.fireEvent('repoBrowserStopSearch');

        var treePanel = p;

        var enableAll = function(root) {
          for (var i = 0; i < root.childNodes.length; i++)
          {
            var node = root.childNodes[i];
            node.enable();
            node.collapse(false, false);
            enableAll(node);
          }
        };
        enableAll(treePanel.root);
      },

      treeLoadExceptionHandler : function(treeLoader, node, response) {
        if (response.status == 503)
        {
          if (Sonatype.MessageBox.isVisible())
          {
            Sonatype.MessageBox.hide();
          }
          node.setText(node.text + ' (Out of Service)');
        }
        else if (response.status == 404 || response.status == 400)
        {
          if (Sonatype.MessageBox.isVisible())
          {
            Sonatype.MessageBox.hide();
          }
          node.setText(node.text + (node.isRoot ? ' (Not Available)' : ' (Not Found)'));
        }
        else if (response.status == 401 || response.status == 403)
        {
          if (Sonatype.MessageBox.isVisible())
          {
            Sonatype.MessageBox.hide();
          }
          node.setText(node.text + ' (Access Denied)');
        }
      }

    });
});

