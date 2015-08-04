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
/*global NX, Ext, Sonatype, Nexus*/

NX.define('Sonatype.repoServer.ArtifactContainer', {
  extend : 'Nexus.panels.AutoTabPanel',

  constructor : function(cfg) {
    Ext.apply(this, cfg || {}, {
      initEventName : 'artifactContainerInit',
      updateEventName : 'artifactContainerUpdate'
    });

    Sonatype.repoServer.ArtifactContainer.superclass.constructor.call(this, {
      layoutOnTabChange : true
    });

    var items = [], i;

    Sonatype.Events.fireEvent(this.initEventName, items, null);

    items.sort(function(a, b) {
      if (a.preferredIndex === undefined && b.preferredIndex === undefined) {
        return 0;
      }

      if (a.preferredIndex === undefined) {
        return 1;
      }
      if (b.preferredIndex === undefined) {
        return -1;
      }
      if (a.preferredIndex < b.preferredIndex) {
        return -1;
      }
      if (a.preferredIndex > b.preferredIndex) {
        return 1;
      }
      return 0;
    });

    for (i = 0; i < items.length; i += 1) {
      this.add(items[i]);
    }
  },

  collapsePanel : function() {
    this.collapse();
    Sonatype.Events.fireEvent(this.updateEventName, this, null);
  },
  updateArtifact : function(data) {
    Sonatype.Events.fireEvent(this.updateEventName, this, data);
    if (data) {
      this.expand();
    }
  },
  hideTab : function(panel) {
    var i, nextPanel;

    panel.tabHidden = true;
    this.tabPanel.hideTabStripItem(panel);
    for (i = 0; i < this.tabPanel.items.getCount(); i += 1) {
      nextPanel = this.tabPanel.items.get(i);
      if (nextPanel.id !== panel.id && !nextPanel.tabHidden) {
        this.tabPanel.setActiveTab(nextPanel);
        return;
      }
    }

    // we haven't found anything, so collapse
    this.tabPanel.doLayout();
    this.collapse();
  },
  showTab : function(panel) {
    panel.tabHidden = false;
    this.tabPanel.unhideTabStripItem(panel);
  }

});

