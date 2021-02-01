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
define('repoServer/RepositoryBrowserContainer',['Sonatype/repoServer/ArtifactContainer'], function(){
// This container will host both the repository browser and the artifact
// information panel
Sonatype.repoServer.RepositoryBrowserContainer = function(config) {
  var config = config || {};
  var defaultConfig = {
    artifactContainerInitEvent : 'fileContainerInit',
    artifactContainerUpdateEvent : 'fileContainerUpdate'
  };
  Ext.apply(this, config, defaultConfig);

  var items = [];

  this.repositoryBrowser = new Sonatype.repoServer.RepositoryBrowsePanel({
        name : 'repositoryBrowser',
        payload : this.payload,
        tabTitle : this.tabTitle,
        browseIndex : false,
        region : 'center',
        nodeClickEvent : 'fileNodeClickedEvent',
        nodeClickPassthru : {
          container : this
        }
      });

  this.artifactContainer = new Sonatype.repoServer.ArtifactContainer({
        collapsible : true,
        collapsed : true,
        region : 'east',
        split : true,
        width : '50%',
        initEventName : this.artifactContainerInitEvent,
        updateEventName : this.artifactContainerUpdateEvent
      });

  items.push(this.repositoryBrowser);
  items.push(this.artifactContainer);

  Sonatype.repoServer.RepositoryBrowserContainer.superclass.constructor.call(this, {
        layout : 'border',
        // this hideMode causes the tab to properly render when coming back from
        // hidden
        hideMode : 'offsets',
        items : items
      });
};

Ext.extend(Sonatype.repoServer.RepositoryBrowserContainer, Ext.Panel, {
      updatePayload : function(payload) {

        if (payload == null)
        {
          this.collapse();
          this.repositoryBrowser.updatePayload(null);
          this.artifactContainer.collapsePanel();
        }
        else
        {
          this.expand();
          this.repositoryBrowser.updatePayload(payload);
          this.artifactContainer.updateArtifact(payload);
        }

      }
    });

// Add the browse storage and browse index panels to the repo
Sonatype.Events.addListener('repositoryViewInit', function(cardPanel, rec) {
      if (rec.data.resourceURI
          && (['npm'].indexOf(rec.data.format) == -1))
      {
        cardPanel.add(new Sonatype.repoServer.RepositoryBrowserContainer({
              payload : rec,
              name : 'browsestorage',
              tabTitle : 'Browse Storage'
            }));
      }
    });

Sonatype.Events.addListener('fileNodeClickedEvent', function(node, passthru) {
      if (passthru && passthru.container && passthru.container.artifactContainer.items.getCount() > 0)
      {
        if (node && node.isLeaf())
        {
          var payload = passthru.container.payload;
          
          passthru.container.artifactContainer.updateArtifact({
                format : (payload && payload.data) ? payload.data.format : null,
                repoId : (payload && payload.data) ? payload.data.id : null,
                text : node.attributes.text,
                leaf : node.attributes.leaf,
                resourceURI : node.attributes.resourceURI
              });
        }
        else
        {
          passthru.container.artifactContainer.collapse();
        }
      }
    });

});

