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
/*global define*/

define('Nexus/profile/UserProfile',['extjs', 'Sonatype/view'], function(Ext, Sonatype) {
Ext.namespace('Nexus.profile');

var Nexus = window.Nexus;

/**
 * The user profile tab.
 *
 * @constructor
 */
Nexus.profile.UserProfile = function(cfg) {
  var
        _this = this,
        config = cfg || {},
        defaultConfig = {
          autoScroll : true,
          minWidth : 270,
          layout : 'absolute'
        },
        views = [];

  Ext.apply(this, config, defaultConfig);

  // this may happen on opening a bookmark
  if (!Sonatype.user.curr || !Sonatype.user.curr.isLoggedIn) {
    // initialize empty tab (we cannot remove it without this call)
    Nexus.profile.UserProfile.superclass.constructor.call(this, {
      title : 'Profile'
    });

    // suppress the next change TO this tab, because we don't have valid user data yet
    Sonatype.view.mainTabPanel.on("beforetabchange", function(tabPanel, newTab, oldTab) {
      if (newTab === _this) {
        Sonatype.view.mainTabPanel.remove(_this);
        return false;
      }
    }, _this, {
      single : true
    });

    if ( !Sonatype.view.justLoggedOut ) {
      Sonatype.view.afterLoginToken = window.location.hash.substring(1);
      Sonatype.repoServer.RepoServer.loginHandler();
    }

    return;
  }

  Sonatype.Events.fireEvent('userProfileInit', views);

  this.content = new Nexus.profile.UserProfile.Content({
    cls : 'user-profile-dynamic-content',
    border : false,
    x : 20,
    y : 20,
    anchor : '-20 -20'
  });

  this.selector = new Ext.form.ComboBox({
    id: 'user-profile-selector',
    x : 30,
    y : 11,
    editable : false,
    triggerAction : 'all',
    listeners : {
      'select' : {
        fn : function(combo, record, index) {
          this.bookmark = record.get(combo.displayField);
          this.content.display(record.get(combo.valueField), this);
          Sonatype.utils.updateHistory(this);
        },
        scope : this
      },
      'render' : {
        fn : function(combo) {
          if ( this.bookmark !== null ) {
            return;
          }
          var rec = combo.store.getAt(0);
          this.bookmark = rec.get(combo.displayField);

          combo.setValue(rec.get(combo.displayField));
          this.content.display(rec.get(combo.valueField), this);
        },
        scope : this
      }
    },
    store : (function() {
      // [ (v.item,v.name) for v in views]
      var viewArray = [];
      Ext.each(views, function(v) {
        var content = new v.item({username : Sonatype.user.curr.username, border : false, frame : false});
        if (!content.shouldShow || content.shouldShow()) {
          viewArray.push([content, v.name]);
        }
      });
      return viewArray;
    }())
  });

  this.refreshContent = function() {
    var tab = this.content.getActiveTab();
    if (tab.refreshContent) {
      tab.refreshContent();
      this.content.doLayout();
    }
  };

  this.refreshButton = new Ext.Button({
    tooltip : 'Refresh',
    style : 'position: absolute; right:25px; top:25px;',
    iconCls : 'st-icon-refresh',
    cls : 'x-btn-icon',
    scope : this,
    handler : this.refreshContent,
    noExtraClass : true
  });

  Nexus.profile.UserProfile.superclass.constructor.call(this, {
    title : 'Profile',
    items : [
      this.content,
      this.selector,
      this.refreshButton
    ]
  });

  this.bookmark = null;

  this.getBookmark = function() {
    return this.bookmark;
  };

  this.applyBookmark = function(token) {
    var
          rec, idx,
          combo = this.selector;

    token = decodeURIComponent(token);

    // FIXME should use combo.findRecord?
    idx = combo.store.find(combo.displayField, token);

    if (idx !== -1) {
      this.bookmark = token;
      rec = combo.store.getAt(idx);
      combo.setValue(rec.get(combo.valueField));
      this.content.display(rec.get(combo.valueField), this);
    }
  };

};

Ext.extend(Nexus.profile.UserProfile, Ext.Panel);

/**
 * The inner content panel of the user profile tab.
 * This is what changes on combo box selection.
 *
 * @constructor
 */
Nexus.profile.UserProfile.Content = function(config)
{
  Ext.apply(this, config || {}, {
    plain : true,
    autoScroll : true,
    border : true,
    layoutOnTabChange: true,
    listeners : {
      'tabchange' : function() {
        // hide tabStrip
        if (!this.headerAlreadyHidden) {
          this.header.hide();
          this.headerAlreadyHidden = true;
        }
      },
      scope : this
    }
  });

  Nexus.profile.UserProfile.Content.superclass.constructor.call(this);

  this.display = function(panel, profile) {
    this.add(panel);
    this.setActiveTab(panel);
    profile.refreshButton.setVisible(panel.refreshContent !== undefined);
  };
};
Ext.extend(Nexus.profile.UserProfile.Content, Ext.TabPanel);

/**
 * Weirdo hack to get bookmarking to work. navigation-* is usually the link in the left nav panel,
 * but the user profile menu does not have that.
 */
Nexus.profile.OpenAction = function(config) {
  config.initialConfigNavigation = {
    tabId : 'profile',
    tabCode : Nexus.profile.UserProfile,
    title : 'Profile'
  };
  Nexus.profile.OpenAction.superclass.constructor.call(this, config);
};
Ext.extend(Nexus.profile.OpenAction, Ext.Panel);

Nexus.profile.OpenAction.registered = new Nexus.profile.OpenAction({
  id : 'navigation-profile'
});

/**
 * @param {string} name The name displayed in the combo box selector.
 * @param {Object} panelCls The class definition of the panel to show as content. The constructor will be called with {username:$currentUsername} and may override frame and border settings.
 * @param {Array} views (optional) List of profile views to add the panel to. Currently 'user' and 'admin' are support. If omitted, the panel will be added to all views.
 *
 * @static
 * @member Nexus.profile
 */
Nexus.profile.register = function(name, panelCls, views) {
  if (views === undefined || views.indexOf('user') !== -1) {
    Sonatype.Events.addListener('userProfileInit', function(views) {
      views.push({
        name : name,
        item : panelCls
      });
    });
  }

  if (views === undefined || views.indexOf('admin') !== -1) {
    Sonatype.Events.addListener('userAdminViewInit', function(views) {
      views.push({
        name : name,
        item : panelCls
      });
    });
  }
};

  return Nexus.profile;
});
