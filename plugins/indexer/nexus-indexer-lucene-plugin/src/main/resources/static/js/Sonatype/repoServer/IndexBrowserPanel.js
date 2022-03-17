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
define('Sonatype/repoServer/IndexBrowserPanel', function() {

  Sonatype.repoServer.IndexBrowserPanel = function(config) {
    var config = config || {};
    var defaultConfig = {
      url : '',
      showRepositoryDropDown : false
    };

    Ext.apply(this, config, defaultConfig);

    if (!this.root)
    {
      this.root = new Ext.tree.TreeNode({
            text : '(Not Available)',
            id : '/',
            singleClickExpand : true,
            expanded : true
          });
    }

    if (this.showRepositoryDropDown)
    {
      this.toolbarInitEvent = 'indexBrowserToolbarInit';
    }

    Sonatype.repoServer.IndexBrowserPanel.superclass.constructor.call(this, {
          nodeIconClass : 'x-tree-node-nexus-icon',
          useNodeIconClassParam : 'locallyAvailable',
          appendAttributeToId : 'type'
        });
  };

  Sonatype.Events.addListener('indexBrowserToolbarInit', function(treepanel, toolbar) {
        if (treepanel.showRepositoryDropDown)
        {
          var store = new Ext.data.SimpleStore({
                fields : ['id', 'name']
              });
          treepanel.repocombo = new Ext.form.ComboBox({
                width : 200,
                store : store,
                valueField : 'id',
                displayField : 'name',
                editable : false,
                mode : 'local',
                triggerAction : 'all',
                listeners : {
                  select : {
                    fn : function(combo, record, index) {
                      for (var i = 0; i < treepanel.payload.data.hits.length; i++)
                      {
                        if (record.data.id == treepanel.payload.data.hits[i].repositoryId)
                        {
                          var repoDetails = treepanel.payload.data.getRepoDetails(record.data.id, treepanel.payload.data.repoList);
                          treepanel.updatePayload({
                                data : {
                                  showCtx : treepanel.payload.data.showCtx,
                                  id : repoDetails.repositoryId,
                                  name : repoDetails.repositoryName,
                                  resourceURI : repoDetails.repositoryURL,
                                  format : repoDetails.repositoryContentClass,
                                  repoType : repoDetails.repositoryKind,
                                  hitIndex : treepanel.payload.data.hitIndex,
                                  useHints : treepanel.payload.data.useHints,
                                  expandPath : treepanel.payload.data.expandPath,
                                  hits : treepanel.payload.data.hits,
                                  rec : treepanel.payload.data.rec,
                                  isSnapshot : repoDetails.repositoryPolicy == 'SNAPSHOT',
                                  repoList : treepanel.payload.data.repoList,
                                  getRepoDetails : treepanel.payload.data.getRepoDetails
                                }
                              }, true);
                        }
                      }
                    },
                    scope : treepanel
                  }
                }
              });

          toolbar.push(' ', '-', ' ', 'Viewing Repository:', ' ', treepanel.repocombo);
        }
      });

  Ext.extend(Sonatype.repoServer.IndexBrowserPanel, Sonatype.panels.TreePanel, {
        nodeExpandHandler : function() {
          var parentContainer = this.parentContainer;

          if (this.payload.data.expandPath)
          {
            this.selectPath(this.getDefaultPathFromPayload(), 'text', function(success, node) {
                  if (success)
                  {
                    if (node.ownerTree.nodeClickEvent)
                    {
                      Sonatype.Events.fireEvent(node.ownerTree.nodeClickEvent, node, node.ownerTree.nodeClickPassthru);
                    }
                  }
                  else if (parentContainer != null)
                  {
                    parentContainer.loadComplete();
                  }
                });
          }
        },
        getDefaultPathFromPayload : function() {
          var rec = this.payload.data.rec;
          var hitIndex = this.payload.data.hitIndex;

          var basePath = '/' + this.payload.data.name + '/' + rec.data.groupId.replace(/\./g, '/') + '/' + rec.data.artifactId + '/' + rec.data.version + '/' + rec.data.artifactId + '-' + rec.data.version;

          for (var i = 0; i < rec.data.artifactHits[hitIndex].artifactLinks.length; i++)
          {
            var link = rec.data.artifactHits[hitIndex].artifactLinks[i];

            if (Ext.isEmpty(link.classifier))
            {
              if (link.extension != 'pom')
              {
                return basePath + '.' + link.extension;
              }
            }
          }

          var link = rec.data.artifactHits[hitIndex].artifactLinks[0];
          return basePath + (link.classifier ? ('-' + link.classifier) : '') + '.' + link.extension;
        },
        refreshHandler : function(button, e) {
          Sonatype.Events.fireEvent(this.nodeClickEvent, null, this.nodeClickPassthru);
          if (this.payload)
          {
            this.loader.url = this.payload.data.resourceURI + '/index_content';

            if (this.payload.data.useHints)
            {
              this.loader.baseParams = {
                groupIdHint : this.payload.data.rec.data.groupId,
                artifactIdHint : this.payload.data.rec.data.artifactId
              }
            }
            else
            {
              this.loader.baseParams = null;
            }

            this.setRootNode(new Ext.tree.AsyncTreeNode({
                  text : this.payload.data[this.titleColumn],
                  leaf : false,
                  path : '/',
                  singleClickExpand : true,
                  expanded : true,
                  listeners : {
                    expand : {
                      fn : this.nodeExpandHandler,
                      scope : this
                    }
                  }
                }));
            Sonatype.repoServer.IndexBrowserPanel.superclass.refreshHandler.apply(this, arguments);
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
            this.afterRender();
          }
        },

        updatePayload : function(payload, onlyPayload) {
          this.oldPayload = this.payload;
          this.payload = payload;

          if (!onlyPayload && this.repocombo)
          {
            var store = this.repocombo.store;
            store.removeAll();
            if (this.payload)
            {
              for (var i = 0; i < this.payload.data.hits.length; i++)
              {
                var repoDetails = this.payload.data.getRepoDetails(this.payload.data.hits[i].repositoryId, this.payload.data.repoList);
                if ((this.payload.data.isSnapshot && repoDetails.repositoryPolicy == 'SNAPSHOT') || (!this.payload.data.isSnapshot && repoDetails.repositoryPolicy == 'RELEASE'))
                {
                  var record = new Ext.data.Record.create({
                        name : 'id'
                      }, {
                        name : 'name'
                      });

                  store.add(new record({
                        id : repoDetails.repositoryId,
                        name : repoDetails.repositoryName
                      }));
                }
              }

              this.repocombo.setValue(this.payload.data.id);
            }
          }

          this.refreshHandler();
        }
      });
});