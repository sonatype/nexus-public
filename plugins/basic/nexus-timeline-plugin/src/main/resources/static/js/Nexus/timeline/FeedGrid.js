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
// must pass in feedUrl that's local to our domain
// config: feedUrl required
/*global NX, Ext*/
/**
 * The main view for all feeds.
 *
 * @since 2.5
 */
NX.define('Nexus.timeline.FeedGrid', {
  extend : 'Ext.grid.GridPanel',

  constructor : function(cfg) {
    var self = this;

    Ext.apply(self, cfg);

    self.store = new Ext.data.Store({
      // note: IE requires text/xml or application/xml to parse via XmlReader
      // because it uses response.responseXML
      proxy : new Ext.data.HttpProxy({
        url : this.feedUrl,
        headers : {
          'accept' : 'text/xml'
        }
      }),
      reader : new Ext.data.XmlReader({
        record : 'channel/item'
      }, ['title', 'author', {
        name : 'pubDate',
        type : 'date',
        dateFormat : 'D, d M Y H:i:s T'
      }, 'link', 'description', 'content']),
      sortInfo : {
        field : 'pubDate',
        direction : 'DESC'
      },
      autoLoad : false
    });

    self.columns = [
      {
        id : 'title',
        header : "Title",
        dataIndex : 'title',
        sortable : true,
        width : 420,
        renderer : this.formatTitle
      },
      {
        id : 'last',
        header : "Date",
        dataIndex : 'pubDate',
        width : 150,
        renderer : this.formatDate,
        sortable : true
      }
    ];

    self.constructor.superclass.constructor.call(this, {
      title : 'Select a feed from the list',
      region : 'center',
      id : 'topic-grid',
      loadMask : {
        msg : 'Loading Feed...'
      },
      stripeRows : true,
      sm : new Ext.grid.RowSelectionModel({
        singleSelect : true
      }),
      viewConfig : {
        forceFit : true,
        enableRowBody : true,
        showPreview : true,
        getRowClass : this.applyRowClass,
        emptyText : 'No artifacts match this query'
      },
      tools : [
        {
          id : 'refresh',
          handler : function(e, toolEl, panel) {
            if (this.feedUrl) {
              this.store.reload();
            }
          },
          scope : this
        }
      ]
    });
  },

  // this.on('rowcontextmenu', this.onContextClick, this);
  setFeed : function(name, url) {
    this.setTitle(name);
    this.feedUrl = url;
    this.store.proxy = new Ext.data.HttpProxy({
      url : this.feedUrl,
      headers : {
        'accept' : 'text/xml'
      }
    });
    this.reloadFeed();
  },

  /**
   * Reload the feed store.
   */
  reloadFeed : function() {
    if (this.feedUrl) {
      this.store.reload();
    }
  },

  /**
   * @private
   */
  togglePreview : function(show) {
    this.view.showPreview = show;
    this.view.refresh();
  },

  // within this function "this" is actually the GridView
  applyRowClass : function(record, rowIndex, p, ds) {
    if (this.showPreview) {
      var xf = Ext.util.Format;
      p.body = '<p>' + Ext.util.Format.htmlEncode(xf.ellipsis(xf.stripTags(record.data.description)), 400) + '</p>';
      return 'x-grid3-row-expanded';
    }
    return 'x-grid3-row-collapsed';
  },

  /**
   * Date formatter that prefers "Today" or a day name over a real date.
   *
   * @private
   *
   * @param value The time to format.
   * @returns {String} The formatted date.
   */
  formatDate : function(value) {
    if (!value) {
      return '';
    }
    var now = new Date();
    var d = now.clearTime(true);

    // @todo: correct Ext's Date parsing of "GMT" timezone text, and text my
    // ISO8601 patches
    // @note: special handling for bad Ext.Date parsing
    value = value.add(Date.MINUTE, d.getTimezoneOffset() * (-1));

    var notime = value.clearTime(true).getTime();
    if (notime == d.getTime()) {
      return 'Today ' + value.dateFormat('g:i a');
    }
    d = d.add('d', -6);
    if (d.getTime() <= notime) {
      return value.dateFormat('D g:i a');
    }
    return value.dateFormat('n/j g:i a');
  },

  /**
   * Converts a record to HTML.
   *
   * @private
   *
   * @param value The title of the entry.
   * @param p
   * @param record The record of the entry. Used to add a link and author description.
   * @returns {String} HTML-formatted feed entry title.
   */
  formatTitle : function(value, p, record) {
    return String.format('<div class="topic"><b><a href="{1}" target="_blank">{0}</a></b><span class="author">{2}</span></div>',
          value, record.data.link, record.data.author);
  }
});
