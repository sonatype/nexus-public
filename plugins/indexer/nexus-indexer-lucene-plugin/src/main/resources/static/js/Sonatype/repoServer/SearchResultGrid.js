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
// must pass in feedUrl that's local to our domain, 'cause we ain't got no proxy
// yet
// config: feedUrl required
define('Sonatype/repoServer/SearchResultGrid', function() {

  Sonatype.SearchStore = function(config) {
    var config = config || {};
    var defaultConfig = {
      searchUrl : Sonatype.config.servicePath + '/lucene/search'
    };
    Ext.apply(this, config, defaultConfig);

    Sonatype.SearchStore.superclass.constructor.call(this, {
          proxy : new Ext.data.HttpProxy({
                url : this.searchUrl,
                method : 'GET',
                suppressStatus : 400
              }),
          reader : new Ext.data.JsonReader({
                root : 'data',
                totalProperty : 'totalCount'
              }, Ext.data.Record.create([{
                    name : 'groupId'
                  }, {
                    name : 'artifactId'
                  }, {
                    name : 'version'
                  }, {
                    name : 'highlightedFragment'
                  }, {
                    name : 'artifactHits'
                  }, {
                    name : 'latestRelease'
                  }, {
                    name : 'latestReleaseRepositoryId'
                  }, {
                    name : 'latestSnapshot'
                  }, {
                    name : 'latestSnapshotRepositoryId'
                  }])),
          listeners : {
            'load' : {
              fn : function(store, records, options) {
                this.grid.updateRowTotals(this.grid);
              },
              scope : this
            },
            // FIXME loadexception event is deprecated, should use general DataProxy#exception event
            'loadexception' : {
              fn : function(obj, options, response) {
                try {
                  // The response is already HTML escaped as it's coming through the REST layer, and can be directly used as
                  // a warning
                  var errorResponse = Ext.decode(response.responseText);
                  if (errorResponse.errors && errorResponse.errors[0] && errorResponse.errors[0].id === "search") {
                    this.grid.setWarningLabel(errorResponse.errors[0].msg);
                  } else if (typeof response.responseText !== 'undefined') {
                    this.grid.setWarningLabel(response.responseText);
                  } else {
                    this.grid.setWarningLabel('Could not retrieve search results.');
                  }
                }
                catch (e) {
                  Sonatype.MessageBox.alert('Problem parsing error response:\n' + e.toString() + '\n' + response.responseText);
                }
              },
              scope : this
            }
          }
        });

    // FIXME it's stupid to do it this way, because getConnection usually returns Ext.Ajax and this ends up firing for all calls.
    this.proxy.getConnection().on('requestcomplete', function(conn, response, options) {
      if (response.responseText && options.url.indexOf(this.searchUrl) !== -1)
      {
        var statusResp = Ext.decode(response.responseText);
        if (statusResp)
        {
          this.grid.totalRecords = statusResp.totalCount;
          if (statusResp.tooManyResults)
          {
            this.grid.setWarningLabel('Too many results, please refine the search condition.');
          }
          else
          {
            this.grid.clearWarningLabel();
          }
        }
      }
    }, this);
  };

  Ext.extend(Sonatype.SearchStore, Ext.data.Store, {});

  Sonatype.repoServer.SearchResultGrid = function(config) {

    Ext.apply(this, config);

    this.sp = Sonatype.lib.Permissions;

    this.totalRecords = 0;

    this.defaultStore = new Sonatype.SearchStore({
          grid : this
        });

    this.store = this.defaultStore;

    this.columns =  [{
        header : 'Group',
        dataIndex : 'groupId',
        sortable : true,
        renderer: NX.htmlRenderer
      }, {
        header : 'Artifact',
        dataIndex : 'artifactId',
        sortable : true,
        renderer: NX.htmlRenderer
      }, {
        header : 'Version',
        dataIndex : 'version',
        sortable : true,
        renderer : this.formatVersionLink
      }, {
        id : 'search-result-download',
        header : 'Download',
        sortable : true,
        renderer : this.formatDownloadLinks
      }];

    this.clearButton = new Ext.Button({
          text : 'Clear Results',
          icon : Sonatype.config.resourcePath + '/static/images/icons/clear.gif',
          cls : 'x-btn-text-icon',
          handler : this.clearResults,
          disabled : true,
          scope : this
        });

    this.fetchMoreBar = new Ext.Toolbar({
          ctCls : 'search-all-tbar',
          items : ['Displaying 0 records', {
                xtype : 'tbspacer'
              }, this.clearButton]
        });
    this.subtitleBar = new Ext.Toolbar({
          ctCls : 'search-all-tbar',
          items : [{
                xtype : 'panel',
                html : '<img src="images/pom_obj.gif" />'
              }, {
                xtype : 'label',
                text : 'Pom file'
              }, {
                xtype : 'tbspacer'
              }, {
                xtype : 'panel',
                html : '<img src="images/jar_obj.gif" />'
              }, {
                xtype : 'label',
                text : 'Jar artifact'
              }, {
                xtype : 'tbspacer'
              }, {
                xtype : 'panel',
                html : '<img src="images/jar_sources_obj.gif" />'
              }, {
                xtype : 'label',
                text : 'Sources artifact'
              }, {
                xtype : 'tbspacer'
              }, {
                xtype : 'panel',
                html : '<img src="images/jar_javadoc_obj.gif" />'
              }, {
                xtype : 'label',
                text : 'Javadoc artifact'
              }]
        });

    Sonatype.Events.fireEvent('searchResultGridInit', this);

    //note we create the model object after the event, so plugins can contribute column data
    this.defaultColumnModel = new Ext.grid.ColumnModel({
          columns : this.columns
        });

    this.colModel = this.defaultColumnModel;

    Sonatype.repoServer.SearchResultGrid.superclass.constructor.call(this, {
          region : 'center',
          id : 'search-result-grid',
          loadMask : {
            msg : 'Loading Results...'
          },
          stripeRows : true,
          sm : new Ext.grid.RowSelectionModel({
                singleSelect : true
              }),

          viewConfig : {
            forceFit : true
          },

          bbar : [this.fetchMoreBar/* , '->', this.subtitleBar */],

          listeners : {
            render : {
              fn : function(grid) {

                var store = grid.getStore();
                var view = grid.getView();
                grid.tip = new Ext.ToolTip({
                      target : view.mainBody,
                      delegate : '.x-grid3-row',
                      maxWidth : 500,
                      trackMouse : true,
                      renderTo : document.body,
                      listeners : {
                        beforeshow : function(tip) {
                          var rowIndex = view.findRowIndex(tip.triggerElement);
                          var record = store.getAt(rowIndex);

                          if (!record)
                          {
                            return false;
                          }

                          var highlightedFragment = record.get('highlightedFragment');

                          if (Ext.isEmpty(highlightedFragment))
                          {
                            return false;
                          }

                          tip.body.dom.innerHTML = highlightedFragment;
                        }
                      }
                    });
              },
              scope : this
            }
          }
        });
  };

  Ext.extend(Sonatype.repoServer.SearchResultGrid, Ext.grid.GridPanel, {
        formatVersionLink : function(value, p, record, rowIndex, colIndex, store) {
          if (!store.reader.jsonData.collapsed)
          {
            return NX.htmlRenderer(record.get('version'));
          }

          var latest = record.get('latestRelease');

          if (!latest)
          {
            latest = record.get('latestSnapshot');
          }

          var linkMarkup = '<a href="#nexus-search;gav~' + encodeURIComponent(record.get('groupId')) + '~'
                  + encodeURIComponent(record.get('artifactId'))
                  + '~~~~kw,versionexpand" onmousedown="cancel_bubble(event)" onclick="cancel_bubble(event); return true;">';

          if (store.reader.jsonData.tooManyResults) {
            return linkMarkup + 'Show All Versions</a>';
          } else {
            return 'Latest: ' + NX.htmlRenderer(latest) + ' ' + linkMarkup + '(Show All Versions)</a>';
          }

        },
        formatDownloadLinks : function(value, p, record, rowIndex, colIndex, store) {
          var hitIndex = 0;
          if (store.reader.jsonData.collapsed)
          {
            var repoToUse = record.get('latestReleaseRepositoryId');

            if (!repoToUse)
            {
              repoToUse = record.get('latestSnapshotRepositoryId');
            }

            for (var i = 0; i < record.data.artifactHits.length; i++)
            {
              if (record.data.artifactHits[i].repositoryId == repoToUse)
              {
                hitIndex = i;
                break;
              }
            }
          }

          var icons = [];
          var links = [];

          for (var i = 0; i < record.data.artifactHits[hitIndex].artifactLinks.length; i++)
          {
            var cls = record.data.artifactHits[hitIndex].artifactLinks[i].classifier;
            var ext = record.data.artifactHits[hitIndex].artifactLinks[i].extension;
            var rep = record.data.artifactHits[hitIndex].repositoryId;
            var grp = record.data.groupId;
            var art = record.data.artifactId;
            var ver = record.data.version;

            if (store.reader.jsonData.collapsed)
            {
              ver = record.data.latestRelease;
              if (Ext.isEmpty(ver))
              {
                ver = record.data.latestSnapshot;
              }
            }
                    
            var link = Sonatype.config.repos.urls.redirect + '?r=' + encodeURIComponent(rep)
                        + '&g=' + encodeURIComponent(grp) + '&a=' + encodeURIComponent(art)
                        + '&v=' + encodeURIComponent(ver) + '&e=' + encodeURIComponent(ext);

            if (!Ext.isEmpty(cls))
            {
              link += '&c=' + encodeURIComponent(cls);
            }

            var icon = null;
            // if (ext == 'pom' && !cls)
            // {
            // icon = "images/pom_obj.gif";
            // }
            // else if ((ext == '' || ext == 'jar') && !cls)
            // {
            // icon = "images/jar_obj.gif";
            // }
            // else if ((ext == '' || ext == 'jar') && cls == 'sources')
            // {
            // icon = "images/jar_sources_obj.gif";
            // }
            // else if ((ext == '' || ext == 'jar') && cls == 'javadoc')
            // {
            // icon = "images/jar_javadoc_obj.gif";
            // }

            var desc = (cls ? (cls + '.' + ext) : ext);

            if (icon)
            {
              desc = '<img src="' + icon + '" title=' + NX.htmlRenderer(desc) + ' />';
            }

            desc = '<a href="' + link + '" onmousedown="cancel_bubble(event)" onclick="cancel_bubble(event); return true;" target="_blank">'
                //
                + desc +
                //
                '</a>'

            if (icon)
            {
              icons.push(desc);
            }
            else
            {
              links.push(desc);
            }

          }

          var value = '';

          for (var i = 0; i < icons.length; i++)
          {
            if (i > 0)
            {
              value += ', ';
            }
            value += icons[i];
          }

          if (icons.length > 0)
          {
            value += '<BR>';
          }

          for (var i = 0; i < links.length; i++)
          {
            if (i > 0)
            {
              value += ', ';
            }
            value += links[i];
          }

          return value;
        },

        switchStore : function(grid, store, columnModel) {
          if (store == null)
          {
            store = grid.defaultStore;
          }

          if (columnModel == null)
          {
            columnModel = grid.defaultColumnModel;
          }

          if (store)
          {
            this.clearResults();
          }

          grid.reconfigure(store, columnModel);
        },
        toggleExtraInfo : function(rowIndex) {
          var rowEl = new Ext.Element(this.getView().getRow(rowIndex));
          var input = rowEl.child('.copy-pom-dep', true);
          input.select(); // @todo: why won't this field highlight?!!!!
          rowEl.toggleClass('x-grid3-row-expanded');
        },

        updateRowTotals : function(p) {
          var count = p.store.getCount();

          p.clearButton.setDisabled(count == 0);

          if (count == 0 || count > p.totalRecords)
          {
            p.totalRecords = count;
          }

          p.fetchMoreBar.items.items[0].destroy();
          p.fetchMoreBar.items.removeAt(0);
          p.fetchMoreBar.insertButton(0, new Ext.Toolbar.TextItem('Displaying Top ' + count + ' records'));
          p.fetchMoreBar.doLayout();
        },

        setWarningLabel : function(s) {
          this.searchPanel.setWarningLabel(s);
        },

        clearWarningLabel : function() {
          this.searchPanel.clearWarningLabel();
        },

        clearResults : function() {
          this.store.baseParams = {};
          this.store.removeAll();
          delete this.store.baseParams['dir'];
          delete this.store.baseParams['sort'];
          delete this.store.sortInfo;
          delete this.view.sortState;
          this.store.sortToggle = {};
          this.view.mainHd.select('td').removeClass(this.sortClasses);
          this.fireEvent('sortchange', this.grid, null);
          this.updateRowTotals(this);
          this.clearWarningLabel();
        }
      });

  cancel_bubble = function(e) {
    if (!e)
      var e = window.event;
    e.cancelBubble = true;
    if (e.stopPropagation)
      e.stopPropagation();
  }
});
