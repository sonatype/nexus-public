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
/*global NX,Nexus,Ext,Sonatype*/
/**
 * Inner view on selecting one feed.
 *
 * @since 2.5
 */
NX.define('Nexus.timeline.FeedView', {
  extend : 'Ext.Panel',
  requires : ['Nexus.timeline.FeedGrid'],

  /*
   * config object: { feedUrl ; required title }
   */
  constructor : function(cfg) {
    var self = this;

    Ext.apply(self, cfg || {}, {
      feedUrl : '',
      title : 'Feed Viewer'
    });

    self.feedRecordConstructor = Ext.data.Record.create([
      {
        name : 'resourceURI'
      },
      {
        name : 'name',
        sortType : Ext.data.SortTypes.asUCString
      }
    ]);

    self.feedReader = new Ext.data.JsonReader({
      root : 'data',
      id : 'resourceURI'
    }, self.feedRecordConstructor);

    self.feedsDataStore = new Ext.data.Store({
      url : Sonatype.config.repos.urls.feeds,
      reader : self.feedReader,
      sortInfo : {
        field : 'name',
        direction : 'ASC'
      },
      autoLoad : true
    });

    self.feedsGridPanel = new Ext.grid.GridPanel({
      id : 'st-feeds-grid',
      region : 'north',
      layout : 'fit',
      collapsible : true,
      split : true,
      height : 160,
      minHeight : 120,
      maxHeight : 400,
      frame : false,
      autoScroll : true,
      selModel : new Ext.grid.RowSelectionModel({
        singleSelect : true
      }),

      tbar : [
        {
          text : 'Refresh',
          icon : Sonatype.config.resourcePath + '/static/images/icons/arrow_refresh.png',
          cls : 'x-btn-text-icon',
          handler : function() {
            self.feedsDataStore.reload();
            self.grid.reloadFeed();
          }
        },
        {
          text : 'Subscribe',
          icon : Sonatype.config.resourcePath + '/static/images/icons/feed.png',
          cls : 'x-btn-text-icon',
          handler : function() {
            if (self.feedsGridPanel.getSelectionModel().hasSelection()) {
              var rec = self.feedsGridPanel.getSelectionModel().getSelected();
              Sonatype.utils.openWindow(rec.get('resourceURI'));
            }
          }
        }
      ],

      // grid view options
      ds : self.feedsDataStore,
      sortInfo : {
        field : 'name',
        direction : "ASC"
      },
      loadMask : true,
      deferredRender : false,
      columns : [
        {
          header : 'Feed',
          dataIndex : 'name',
          width : 300
        },
        {
          header : 'URL',
          dataIndex : 'resourceURI',
          width : 300,
          id : 'feeds-url-col',
          renderer : function(s) {
            return '<a href="' + s + '" target="_blank">' + s + '</a>';
          },
          menuDisabled : true
        }
      ],
      autoExpandColumn : 'feeds-url-col',
      disableSelection : false
    });

    self.feedsGridPanel.getSelectionModel().on('rowselect', self.rowSelect, self);

    self.viewItemTemplate = new Ext.Template(
          '<div class="post-data">',
          '<span class="post-date">{pubDate:date("M j, Y, g:i a")}</span>',
          '<h3 class="post-title">{title}</h3>',
          '<h4 class="post-author">by {author:defaultValue("Unknown")}</h4>',
          '</div>',
          '<div class="post-body">',
          '{content:this.getBody}',
          '</div>');

    self.viewItemTemplate.compile();
    self.viewItemTemplate.getBody = function(v, all) {
      return Ext.util.Format.stripScripts(v || all.description);
    };

    self.grid = NX.create('Nexus.timeline.FeedGrid');

    self.constructor.superclass.constructor.call(self, {
      layout : 'border',
      title : self.title,
      hideMode : 'offsets',
      items : [self.feedsGridPanel, self.grid
      ]
    });

    self.gsm = self.grid.getSelectionModel();
    self.grid.store.on('load', self.gsm.selectFirstRow, self.gsm);
  },

  rowSelect : function(selectionModel, index, rec) {
    this.grid.setFeed(rec.get('name'), rec.get('resourceURI'));
  }

}, function() {
  Sonatype.config.repos.urls.feeds = Sonatype.config.servicePath + '/feeds';
  Sonatype.Events.addListener('nexusNavigationInit', function(panel) {
    var sp = Sonatype.lib.Permissions;
    panel.add({
      enabled : sp.checkPermission('nexus:feeds', sp.READ),
      sectionId : 'st-nexus-views',
      title : 'System Feeds',
      tabId : 'feed-view-system-changes',
      tabCode : Nexus.timeline.FeedView
    });
  });
});

