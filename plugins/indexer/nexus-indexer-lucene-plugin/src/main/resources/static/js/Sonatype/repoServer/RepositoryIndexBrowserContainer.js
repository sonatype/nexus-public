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
// This container will host both the repository browser and the artifact
// information panel
define('Sonatype/repoServer/RepositoryIndexBrowserContainer', function() {

  Sonatype.repoServer.RepositoryIndexBrowserContainer = function(config) {
    var config = config || {};
    var defaultConfig = {
      showRepositoryDropDown : false
    };
    Ext.apply(this, config, defaultConfig);

    this.repositoryBrowser = new Sonatype.repoServer.IndexBrowserPanel({
          payload : this.payload,
          tabTitle : this.tabTitle,
          region : 'center',
          url : this.initialUrl,
          root : this.initialRoot,
          parentContainer : this,
          nodeClickEvent : 'indexNodeClickedEvent',
          nodeClickPassthru : {
            container : this
          },
          showRepositoryDropDown : this.showRepositoryDropDown
        });

    this.artifactContainer = new Sonatype.repoServer.ArtifactContainer({
          collapsible : true,
          collapsed : true,
          region : 'east',
          split : true,
          width : '600'

        });
    Sonatype.repoServer.RepositoryIndexBrowserContainer.superclass.constructor.call(this, {
          layout : 'border',
          // this hideMode causes the tab to properly render when coming back from
          // hidden
          hideMode : 'offsets',
          items : [this.repositoryBrowser, this.artifactContainer]
        });
  };

  Ext.extend(Sonatype.repoServer.RepositoryIndexBrowserContainer, Ext.Panel, {
        updatePayload : function(payload) {
          if (payload == null)
          {
            this.repositoryBrowser.updatePayload(null);
            this.artifactContainer.collapsePanel();
          }
          else
          {
            if (payload.data.expandPath)
            {
              if (!this.loadMask)
              {
                this.loadMask = new Ext.LoadMask(this.getEl(), {
                      msg : 'Loading search result...'
                    });
              }
              this.loadMask.show();
            }
            this.repositoryBrowser.updatePayload(payload);
          }
        },
        loadComplete : function() {
          if (this.loadMask)
          {
            this.loadMask.hide();
          }
        }
      });

  // Add the browse storage and browse index panels to the repo
  Sonatype.Events.addListener('repositoryViewInit', function(cardPanel, rec) {
        if (rec.data.resourceURI && rec.data.repoType != 'virtual' && rec.data.format == 'maven2')
        {
          var panel = new Sonatype.repoServer.RepositoryIndexBrowserContainer({
                name : 'browseindex',
                tabTitle : 'Browse Index',
                autoExpand : false,
                payload : rec,
                initialUrl : rec.data.resourceURI + '/index_content',
                initialRoot : new Ext.tree.AsyncTreeNode({
                      text : rec.data['name'],
                      path : '/',
                      singleClickExpand : true,
                      expanded : false
                    })
              });

          if (cardPanel.items.getCount() > 0)
          {
            cardPanel.insert(1, panel);
          }
          else
          {
            cardPanel.add(panel);
          }
        }
      });

  Sonatype.Events.addListener('indexNodeClickedEvent', function(node, passthru) {
        if (passthru && passthru.container)
        {
          if (node && node.isLeaf())
          {
            if (!passthru.container.loadMask)
            {
              passthru.container.loadMask = new Ext.LoadMask(passthru.container.getEl(), {
                    msg : 'Loading search result...'
                  });
            }
            passthru.container.loadMask.show();

            Ext.Ajax.request({
                  scope : this,
                  method : 'GET',
                  options : {
                    dontForceLogout : true
                  },
                  cbPassThru : {
                    node : node,
                    container : passthru.container
                  },
                  callback : function(options, isSuccess, response) {
                    if (passthru.container.loadMask)
                    {
                      passthru.container.loadMask.hide();
                    }
                    if (isSuccess)
                    {
                      var json = Ext.decode(response.responseText);

                      var resourceURI = Sonatype.config.servicePath + '/repositories/' + options.cbPassThru.node.attributes.repositoryId + '/content' + json.data.repositoryPath;

                      var payload = (options.cbPassThru.container.payload) ?  options.cbPassThru.container.payload : options.cbPassThru.container.repositoryBrowser.payload;
                      
                      options.cbPassThru.container.artifactContainer.updateArtifact({
                            leaf : true,
                            resourceURI : resourceURI,
                            groupId : options.cbPassThru.node.attributes.groupId,
                            artifactId : options.cbPassThru.node.attributes.artifactId,
                            version : options.cbPassThru.node.attributes.version,
                            repoId : options.cbPassThru.node.attributes.repositoryId,
                            classifier : options.cbPassThru.node.attributes.classifier,
                            extension : options.cbPassThru.node.attributes.extension,
                            artifactLink : options.cbPassThru.node.attributes.artifactUri,
                            pomLink : options.cbPassThru.node.attributes.pomUri,
                            nodeName : options.cbPassThru.node.attributes.nodeName,
                            format : payload.data.format
                          });
                    }
                  },
                  url : Sonatype.config.servicePath + '/artifact/maven/resolve',
                  params : {
                    r : node.attributes.repositoryId,
                    g : node.attributes.groupId,
                    a : node.attributes.artifactId,
                    v : node.attributes.version,
                    c : node.attributes.classifier,
                    e : node.attributes.extension,
                    isLocal : 'true'
                  }
                });
            // var resourceURI = node.ownerTree.loader.url.substring(0,
            // node.ownerTree.loader.url.length - 'index_content'.length) +
            // 'content' + node.attributes.path;
          }
          else
          {
            passthru.container.artifactContainer.updateArtifact(null);
          }
        }
      });
});