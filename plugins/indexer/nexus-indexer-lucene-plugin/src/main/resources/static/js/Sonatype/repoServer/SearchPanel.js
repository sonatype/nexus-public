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
define('Sonatype/repoServer/SearchPanel', function() {

  Sonatype.repoServer.SearchPanel = function(config) {
    var config = config || {};
    var defaultConfig = {};
    Ext.apply(this, config, defaultConfig);

    this.grid = new Sonatype.repoServer.SearchResultGrid({
          searchPanel : this
        });

    this.searchTypes = [];

    // fire event for plugins to add their own search items
    Sonatype.Events.fireEvent('searchTypeInit', this.searchTypes, this);

    // no items, no page
    if (this.searchTypes.length < 1)
    {
      return;
    }

    var defaultSearchTypeIndex = 0;
    // find default
    for (var i = 0; i < this.searchTypes.length; i++)
    {
      if (this.searchTypes[i].defaultQuickSearch)
      {
        defaultSearchTypeIndex = i;
        break;
      }
    }

    this.searchTypeButton = new Ext.Button({
          text : this.searchTypes[defaultSearchTypeIndex].text,
          value : this.searchTypes[defaultSearchTypeIndex].value,
          tooltip : 'Click for more search options',
          handler : this.switchSearchType,
          scope : this,
          menu : {
            items : this.searchTypes
          }
        });

    this.searchToolbar = new Ext.Toolbar({
          ctCls : 'search-all-tbar',
          items : [this.searchTypeButton, this.convertToFieldObject(this.searchTypes[defaultSearchTypeIndex].panelItems[0])]
        });

    this.repoBrowserContainer = new Sonatype.repoServer.RepositoryIndexBrowserContainer({
          region : 'south',
          split : true,
          height : 375,
          showRepositoryDropDown : true
        });

    Sonatype.repoServer.SearchPanel.superclass.constructor.call(this, {
          layout : 'border',
          hideMode : 'offsets',
          tbar : this.searchToolbar,
          items : [this.grid, this.repoBrowserContainer]
        });

    // this.grid.getSelectionModel().on('rowselect',
    // this.displayArtifactInformation, this);
    this.grid.on('rowclick', this.rowClickHandler, this);
    this.grid.on('keypress', this.keypressHandler, this);
    this.grid.clearButton.on('click', this.clearArtifactInformation, this);
    this.repoBrowserContainer.repositoryBrowser.getSelectionModel().on('selectionchange', this.focusGrid, this);

    this.lastbookmark = '';
    this.searchTask = new Ext.util.DelayedTask(this.delayedDisplayArtifactInformation, this, []);
  };

  Ext.extend(Sonatype.repoServer.SearchPanel, Ext.Panel, {
        focusGrid : function() {
          // this.grid.doLayout();
          // this.grid.focus(false, 10);
        },

        clearArtifactInformation : function(button, e) {
          this.repoBrowserContainer.updatePayload(null);
        },

        rowClickHandler : function(grid, rowindex, evt) {
          this.displayArtifactInformation(grid.getSelectionModel(), rowindex, grid.getSelectionModel().getSelected());
        },

        keypressHandler : function(evt) {
          this.searchTask.cancel();
          if (evt.keyCode == 13)
          {
            this.delayedDisplayArtifactInformation();
          }
          else
          {
            this.searchTask.delay(2000);
          }
        },

        delayedDisplayArtifactInformation : function() {
          this.displayArtifactInformation(this.grid.getSelectionModel(), -1, this.grid.getSelectionModel().getSelected());
        },

        displayArtifactInformation : function(selectionModel, index, rec) {
          var searchType = this.getSearchType(this.searchTypeButton.value);
          if (typeof searchType.showArtifactContainer != 'function' || searchType.showArtifactContainer(rec))
          {
            var hitIndex = 0;
            if (rec.store.reader.jsonData.collapsed)
            {
              var repoToUse = rec.get('latestReleaseRepositoryId');

              if (!repoToUse)
              {
                repoToUse = rec.get('latestSnapshotRepositoryId');
              }

              for (var i = 0; i < rec.data.artifactHits.length; i++)
              {
                if (rec.data.artifactHits[i].repositoryId == repoToUse)
                {
                  hitIndex = i;
                  break;
                }
              }
            }

            var getRepoDetails = function(repoId, repoList) {
              for (var i = 0; i < repoList.length; i++)
              {
                if (repoList[i].repositoryId == repoId)
                {
                  return repoList[i];
                }
              }

              return null;
            };

            var repoDetails = getRepoDetails(rec.data.artifactHits[hitIndex].repositoryId, rec.store.reader.jsonData.repoDetails);

            var payload = {
              data : {
                showCtx : true,
                id : repoDetails.repositoryId,
                name : repoDetails.repositoryName,
                resourceURI : repoDetails.repositoryURL,
                format : repoDetails.repositoryContentClass,
                repoType : repoDetails.repositoryKind,
                hitIndex : hitIndex,
                useHints : true,
                expandPath : true,
                hits : rec.data.artifactHits,
                rec : rec,
                isSnapshot : repoDetails.repositoryPolicy == 'SNAPSHOT',
                repoList : rec.store.reader.jsonData.repoDetails,
                getRepoDetails : getRepoDetails
              }
            };

            this.repoBrowserContainer.updatePayload(payload);
          }
        },
        // search type switched on the drop down button
        switchSearchType : function(button, event) {
          // if event is null, this is called directly, and we
          // we will reset regardless if already selected, otherwise
          // no need to do anything if already set to same value
          if (event == null || this.searchTypeButton.value != button.value)
          {
            this.searchTypeButton.value = button.value;
            this.searchTypeButton.setText(this.getSearchType(button.value).text);
            this.clearWarningLabel();
            this.loadSearchPanel();
            this.switchStore();
          }
        },
        // load the dynamic panel
        loadSearchPanel : function() {
          // first remove current items
          while (this.searchToolbar.items.length > 1)
          {
            var item = this.searchToolbar.items.last();
            this.searchToolbar.items.remove(item);
            item.destroy();
          }

          // now add the other items
          var searchType = this.getSearchType(this.searchTypeButton.value);

          if (searchType != null)
          {
            for (var i = 0; i < searchType.panelItems.length; i++)
            {
              // can't simply add object config to toolbar, need to create
              // a real item
              this.searchToolbar.add(this.convertToFieldObject(searchType.panelItems[i]));
            }
            this.searchToolbar.doLayout();
          }
        },
        // toolbar only supports adding certain types of items, so we
        // need to do some special handling
        convertToFieldObject : function(config) {
          if (config.xtype == 'nexussearchfield')
          {
            return new Ext.app.SearchField(config);
          }
          else if (config.xtype == 'textfield')
          {
            return new Ext.form.TextField(config);
          }
          else
          {
            return config;
          }
        },
        // different search types may have different stores
        switchStore : function() {
          var searchType = this.getSearchType(this.searchTypeButton.value);
          this.grid.switchStore(this.grid, searchType.store, searchType.columnModel);
        },
        // retrieve the specified search type object
        getSearchType : function(value) {
          for (var i = 0; i < this.searchTypes.length; i++)
          {
            if (this.searchTypes[i].value == value)
            {
              return this.searchTypes[i];
            }
          }

          return null;
        },
        /**
         * Set the warning message in the search toolbar
         * @param s A string safe to display in the UI
         */
        setWarningLabel : function(s) {
          var warningHtml = '<span class="x-toolbar-warning">' + NX.htmlRenderer(s) + '</span>';

          this.clearWarningLabel();
          this.warningLabel = this.searchToolbar.addText(warningHtml);
          this.searchToolbar.doLayout();
        },
        // clear the warning in the toolbar
        clearWarningLabel : function() {
          if (this.warningLabel)
          {
            this.warningLabel.destroy();
            this.warningLabel = null;
            this.searchToolbar.doLayout();
          }
        },
        // start the search
        startSearch : function(panel, updateHistory) {
          if (updateHistory)
          {
            // update history in address bar of browser
            panel.extraData = null;
            Sonatype.utils.updateHistory(panel);
          }

          var searchType = this.getSearchType(this.searchTypeButton.value);

          if (panel.grid.store.sortInfo)
          {
            panel.grid.store.sortInfo = null;
            panel.grid.getView().updateHeaders();
          }

          searchType.searchHandler.call(this, panel);
        },
        // get the records from the server using grid
        fetchRecords : function(panel, reverse) {
          panel.repoBrowserContainer.updatePayload(null);
          panel.grid.totalRecords = 0;
          panel.grid.store.removeAll();
          panel.grid.store.load();

          panel.grid.doSort = reverse;
          panel.grid.store.on('load', this.sortResults, panel);
        },
        sortResults : function(store, records, options) {
          if (this.grid.doSort)
          {
            this.grid.doSort = null;
            store.sort('version', 'desc');
          }

          if (records.length > 0)
          {
            this.grid.getSelectionModel().selectFirstRow();
            this.displayArtifactInformation(this.grid.getSelectionModel(), 0, this.grid.getSelectionModel().getSelected());
          }
        },
        // start the quick search, we will look at all search types
        // and try to guess which type of search to use
        startQuickSearch : function(v) {
          var defaultSearchType = null;
          var searchType = null;
          for (var i = 0; i < this.searchTypes.length; i++)
          {
            // this default search will be used if no other searches match
            if (this.searchTypes[i].defaultQuickSearch == true)
            {
              defaultSearchType = this.searchTypes[i];
            }
            else if (this.searchTypes[i].quickSearchCheckHandler.call(this, this, v))
            {
              searchType = this.searchTypes[i];
              break;
            }
          }

          // apply the default search
          if (searchType == null && defaultSearchType != null)
          {
            searchType = defaultSearchType;
          }

          if (searchType != null)
          {
            this.switchSearchType({
                  value : searchType.value
                }, null);

            searchType.quickSearchHandler.call(this, this, v);
            this.startSearch(this, true);
          }
        },
        // apply the bookmark params to page
        applyBookmark : function(bookmark) {
          if (bookmark)
          {
            if (this.lastbookmark == bookmark)
            {
              return;
            }

            this.lastbookmark = bookmark;

            var parts = decodeURIComponent(bookmark).split('~');

            // if type not specified, simply do a quick search and guess
            if (parts.length == 1)
            {
              this.startQuickSearch(bookmark);
            }
            else if (parts.length > 1)
            {
              this.switchSearchType({
                    value : parts[0]
                  }, null);

              var searchType = this.getSearchType(parts[0]);

              searchType.applyBookmarkHandler.call(this, this, parts);
            }
          }
        },
        // get the params to build bookmark
        getBookmark : function() {
          var searchType = this.getSearchType(this.searchTypeButton.value);

          var bookmark = searchType.getBookmarkHandler.call(this, this);

          if (this.extraData)
          {
            var extras = this.extraData.split(',');

            if (extras.length > 0)
            {
              bookmark += '~';
              for (var i = 0; i < extras.length; i++)
              {
                if (i > 0)
                {
                  bookmark += ',';
                }
                bookmark += extras[i];
              }
            }
          }

          this.lastbookmark = bookmark;

          return bookmark;
        }
      });

  // Add the quick search
  Sonatype.Events.addListener('searchTypeInit', function(searchTypes, panel) {
        // keyword is the default, we always want first in list
        searchTypes.splice(0, 0, {
              value : 'quick',
              text : 'Keyword Search',
              scope : panel,
              handler : panel.switchSearchType,
              defaultQuickSearch : true,
              // use the default store
              store : null,
              quickSearchCheckHandler : function(panel, value) {
                return true;
              },
              quickSearchHandler : function(panel, value) {
                panel.getTopToolbar().items.itemAt(1).setRawValue(value);
              },
              searchHandler : function(panel) {
                var value = panel.getTopToolbar().items.itemAt(1).getRawValue();

                if (value)
                {
                  panel.grid.store.baseParams = {};
                  panel.grid.store.baseParams['q'] = value;
                  panel.grid.store.baseParams['collapseresults'] = true;
                  panel.fetchRecords(panel);
                }
              },
              applyBookmarkHandler : function(panel, data) {
                panel.extraData = null;
                panel.getTopToolbar().items.itemAt(1).setRawValue(data[1]);
                panel.startSearch(panel, false);
              },
              getBookmarkHandler : function(panel) {
                var result = panel.searchTypeButton.value;
                result += '~';
                result += panel.getTopToolbar().items.itemAt(1).getRawValue();
                return result;
              },
              panelItems : [{
                    xtype : 'nexussearchfield',
                    name : 'single-search-field',
                    searchPanel : panel,
                    width : 300
                  }]
            });
      });

  // Add the classname search
  Sonatype.Events.addListener('searchTypeInit', function(searchTypes, panel) {
        searchTypes.push({
              value : 'classname',
              text : 'Classname Search',
              scope : panel,
              // use the default store
              store : null,
              handler : panel.switchSearchType,
              quickSearchCheckHandler : function(panel, value) {
                return value.search(/^[a-z.]*[A-Z]/) == 0;
              },
              quickSearchHandler : function(panel, value) {
                panel.getTopToolbar().items.itemAt(1).setRawValue(value);
              },
              searchHandler : function(panel) {
                var value = panel.getTopToolbar().items.itemAt(1).getRawValue();

                if (value)
                {
                  panel.grid.store.baseParams = {};
                  panel.grid.store.baseParams['cn'] = value;
                  panel.fetchRecords(panel);
                }
              },
              applyBookmarkHandler : function(panel, data) {
                panel.getTopToolbar().items.itemAt(1).setRawValue(data[1]);
                panel.startSearch(panel, false);
              },
              getBookmarkHandler : function(panel) {
                var result = panel.searchTypeButton.value;
                result += '~';
                result += panel.getTopToolbar().items.itemAt(1).getRawValue();

                return result;
              },
              panelItems : [{
                    xtype : 'nexussearchfield',
                    name : 'single-search-field',
                    searchPanel : panel,
                    width : 300
                  }]
            });
      });

  // Add the gav search
  Sonatype.Events.addListener('searchTypeInit', function(searchTypes, panel) {
        var enterHandler = function(f, e) {
          if (e.getKey() == e.ENTER)
          {
            this.startSearch(this, true);
          }
        };

        var gavPopulator = function(panel, data) {
          panel.extraData = null;

          // groupId
          if (data.length > 1)
          {
            panel.getTopToolbar().items.itemAt(2).setRawValue(data[1]);
          }
          // artifactId
          if (data.length > 2)
          {
            panel.getTopToolbar().items.itemAt(5).setRawValue(data[2]);
          }
          // version
          if (data.length > 3)
          {
            panel.getTopToolbar().items.itemAt(8).setRawValue(data[3]);
          }
          // packaging
          if (data.length > 4)
          {
            panel.getTopToolbar().items.itemAt(11).setRawValue(data[4]);
          }
          // classifier
          if (data.length > 5)
          {
            panel.getTopToolbar().items.itemAt(14).setRawValue(data[5]);
          }
          // extra params, comma seperated list of params
          if (data.length > 6)
          {
            panel.extraData = data[6];
          }
        }

        searchTypes.push({
              value : 'gav',
              text : 'GAV Search',
              scope : panel,
              // use the default store
              store : null,
              handler : panel.switchSearchType,
              quickSearchCheckHandler : function(panel, value) {
                return value.indexOf(':') > -1;
              },
              quickSearchHandler : function(panel, value) {
                var parts = value.split(':');
                var data = ['gav'];
                for (var i = 0; i < parts.length; i++)
                {
                  data.push(parts[i]);
                }
                gavPopulator(panel, data);
              },
              searchHandler : function(panel) {
                this.grid.store.baseParams = {};

                // groupId
                var v = panel.getTopToolbar().items.itemAt(2).getRawValue();
                if (v)
                {
                  panel.grid.store.baseParams['g'] = v;
                }
                // artifactId
                v = panel.getTopToolbar().items.itemAt(5).getRawValue();
                if (v)
                {
                  panel.grid.store.baseParams['a'] = v;
                }
                // version
                v = panel.getTopToolbar().items.itemAt(8).getRawValue();
                if (v)
                {
                  panel.grid.store.baseParams['v'] = v;
                }
                // packaging
                v = panel.getTopToolbar().items.itemAt(11).getRawValue();
                if (v)
                {
                  panel.grid.store.baseParams['p'] = v;
                }
                // classifier
                v = panel.getTopToolbar().items.itemAt(14).getRawValue();
                if (v)
                {
                  panel.grid.store.baseParams['c'] = v;
                }

                panel.grid.store.baseParams['collapseresults'] = true;

                // go through the extras and process them.
                if (panel.extraData)
                {
                  var extras = panel.extraData.split(',');

                  for (var i = 0; i < extras.length; i++)
                  {
                    if (extras[i] == 'versionexpand')
                    {
                      panel.grid.store.baseParams['versionexpand'] = true;
                    }
                  }
                }

                if (panel.grid.store.baseParams['g'] == null && panel.grid.store.baseParams['a'] == null && panel.grid.store.baseParams['v'] == null)
                {
                  panel.setWarningLabel('A group, an artifact or a version is required to run a search.');
                  return;
                }

                panel.clearWarningLabel();

                panel.fetchRecords(panel, true);
              },
              applyBookmarkHandler : function(panel, data) {
                gavPopulator(panel, data);
                panel.startSearch(this, false);
              },
              getBookmarkHandler : function(panel) {
                var result = panel.searchTypeButton.value;
                // groupId
                result += '~';
                var v = panel.getTopToolbar().items.itemAt(2).getRawValue();
                if (v)
                {
                  result += v;
                }
                // artifactId
                result += '~';
                v = panel.getTopToolbar().items.itemAt(5).getRawValue();
                if (v)
                {
                  result += v;
                }
                // version
                result += '~';
                v = panel.getTopToolbar().items.itemAt(8).getRawValue();
                if (v)
                {
                  result += v;
                }
                // packaging
                result += '~';
                v = panel.getTopToolbar().items.itemAt(11).getRawValue();
                if (v)
                {
                  result += v;
                }
                // classifier
                result += '~';
                v = panel.getTopToolbar().items.itemAt(14).getRawValue();
                if (v)
                {
                  result += v;
                }

                return result;
              },
              panelItems : ['Group:', {
                    xtype : 'textfield',
                    id : 'gavsearch-group',
                    size : 80,
                    listeners : {
                      'specialkey' : {
                        fn : enterHandler,
                        scope : panel
                      }
                    }
                  }, {
                    xtype : 'tbspacer'
                  }, 'Artifact:', {
                    xtype : 'textfield',
                    id : 'gavsearch-artifact',
                    size : 80,
                    listeners : {
                      'specialkey' : {
                        fn : enterHandler,
                        scope : panel
                      }
                    }
                  }, {
                    xtype : 'tbspacer'
                  }, 'Version:', {
                    xtype : 'textfield',
                    id : 'gavsearch-version',
                    size : 80,
                    listeners : {
                      'specialkey' : {
                        fn : enterHandler,
                        scope : panel
                      }
                    }
                  }, {
                    xtype : 'tbspacer'
                  }, 'Packaging:', {
                    xtype : 'textfield',
                    id : 'gavsearch-packaging',
                    size : 80,
                    listeners : {
                      'specialkey' : {
                        fn : enterHandler,
                        scope : panel
                      }
                    }
                  }, {
                    xtype : 'tbspacer'
                  }, 'Classifier:', {
                    xtype : 'textfield',
                    id : 'gavsearch-classifier',
                    size : 80,
                    listeners : {
                      'specialkey' : {
                        fn : enterHandler,
                        scope : panel
                      }
                    }
                  }, {
                    xtype : 'tbspacer'
                  }, {
                    icon : Sonatype.config.resourcePath + '/static/images/icons/search.gif',
                    cls : 'x-btn-icon',
                    scope : panel,
                    handler : function() {
                      this.startSearch(this, true);
                    }
                  }]
            });
      });

  // Add the checksum search
  Sonatype.Events.addListener('searchTypeInit', function(searchTypes, panel) {
        if (Sonatype.lib.Permissions.checkPermission('nexus:identify', Sonatype.lib.Permissions.READ))
        {
          searchTypes.push({
                value : 'checksum',
                text : 'Checksum Search',
                scope : panel,
                // use the default store
                store : null,
                handler : panel.switchSearchType,
                quickSearchCheckHandler : function(panel, value) {
                  return value.search(/^[0-9a-f]{40}$/) == 0;
                },
                quickSearchHandler : function(panel, value) {
                  panel.getTopToolbar().items.itemAt(1).setRawValue(value);
                },
                searchHandler : function(panel) {
                  var value = panel.getTopToolbar().items.itemAt(1).getRawValue();

                  if (value)
                  {
                    panel.grid.store.baseParams = {};
                    panel.grid.store.baseParams['sha1'] = value;
                    panel.fetchRecords(panel);
                  }
                },
                applyBookmarkHandler : function(panel, data) {
                  panel.getTopToolbar().items.itemAt(1).setRawValue(data[1]);
                  panel.startSearch(panel, false);
                },
                getBookmarkHandler : function(panel) {
                  var result = panel.searchTypeButton.value;
                  result += '~';
                  result += panel.getTopToolbar().items.itemAt(1).getRawValue();

                  return result;
                },
                panelItems : [{
                      xtype : 'nexussearchfield',
                      name : 'single-search-field',
                      searchPanel : panel,
                      width : 300
                    }]
              });
        }
      });
});