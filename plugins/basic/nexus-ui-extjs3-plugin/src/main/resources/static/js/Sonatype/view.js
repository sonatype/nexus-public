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
/*global define*/
define('Sonatype/view',['../extjs', 'Sonatype/init', 'nexus', 'Nexus/log', 'Sonatype/utils', 'ext/ux/TabCloseMenu', 'Sonatype/strings'], function(Ext, Sonatype, Nexus, Log, Utils, TabCloseMenu, Strings) {
Sonatype.view = {
  FIELD_OFFSET_WITH_SCROLL : String((3 + 16 + 3 + 30) * (-1)), // (help icon margin) + (help icon) + margin + (scrollbar)
  FIELD_OFFSET : String((3 + 16) * (-1)), // extra padding on right of icon not needed
  FIELDSET_OFFSET : String((3 + 18) * (-1)), // (extra room between border and scrollbar) + (scrollbar)
  FIELDSET_OFFSET_WITH_SCROLL : String((3 + 18 + 3 + 30) * (-1)), // (extra room between border and scrollbar) + (scrollbar) + margin + (scrollbar)
  HISTORY_DELIMITER : ';',

  headLinks : new Sonatype.headLinks(),

  init : function() {
    var viewport, size, srv, availSvrs;

    Sonatype.Events.addListener('initHeadLinks', function() {
          Sonatype.view.headLinks.updateLinks();
        });

    Sonatype.Events.fireEvent('initHeadLinks');

    // Ext.get('login-link').on('click',
    // Sonatype.repoServer.RepoServer.loginHandler,
    // Sonatype.repoServer.RepoServer);
    // this.updateLoginLinkText();

    Ext.get('header').show();
    Ext.get('welcome-tab').show();

    Sonatype.view.welcomeTab = new Ext.Panel({
          contentEl : 'welcome-tab',
          title : 'Welcome',
          id : 'welcome'
        });

    Sonatype.view.headerPanel = new Ext.Panel({
          contentEl : 'header',
          region : 'north',
          margins : '0 0 0 0',
          border : false,
          autoHeight : true
        });

    Ext.Ajax.on({
          "requestexception" : {
            fn : function(conn, response, options) {
              var suppressed = options.suppressStatus;

              if (Sonatype.config.repos.urls.status === options.url)
              {
                // never show the connection error message for the status resource
                // (if anonymous is disabled, UI would show this all the time, until you're logged in)
                return;
              } else if ( suppressed === true ) {
                // 'true' means suppress all error handling
                return;
              }

              if (suppressed === undefined) {
                suppressed = [];
              } else if (!Ext.isArray(suppressed)) {
                suppressed = [suppressed];
              }

              // NEXUS-5228 some plugins use numbers in their options, some use strings (suppressStatus : '404')
              // and rely on type conversion. That should be fixed because
              // a) it was relying on broken js behavior (implicit conversion),
              // b) it did not work at all when multiple status codes should be suppressed
              //     (['400', '404'] would still show error messages)
              Ext.each(suppressed, function(item, idx, array) {
                if (Ext.type(item) !== 'number') {
                  Nexus.Log.warn('The "suppressStatus" config option is not an instance of Number. Trying to convert.' );
                  array[idx] = parseInt(item, 10);
                }
              });

              if (suppressed.indexOf(response.status) < 0)
              {
                Sonatype.utils.connectionError(response, options.defaultErrorMessage, null, options, true);
              }
            },
            scope : this
          }
        });

    viewport = new Ext.Viewport({
          layout : 'border',
          items : [Sonatype.view.headerPanel, {
                region : 'west',
                title : 'Sonatype&trade;',
                collapsible : true,
                // collapseMode: 'mini',
                // border: false,
                split : false,
                width : 185,
                minSize : 185,
                maxSize : 185,
                layout : 'fit',
                margins : '0 5 5 5',
                items : [{
                  xtype : 'tabpanel',
                  id : 'st-server-tab-panel',
                  border : false,
                  tabPosition : 'top',
                  layoutOnTabChange : true
                    // tabs added by servers
                  }]
              }, new Sonatype.view.MainTabPanel({
                    id : 'st-main-tab-panel',
                    region : 'center',
                    margins : '0 5 5 0',
                    deferredRender : false,
                    resizeTabs : true,
                    enableTabScroll : true,
                    minTabWidth : 140,
                    autoScroll : false, // default
                    defaults : {
                      closable : true
                    },
                    activeTab : 0,
                    layoutOnTabChange : true,
                    // border: false,
                    // bodyBorder: true,
                    items : [Sonatype.view.welcomeTab]
                  })]
        });

    // Export useful values to Sonatype.view namespace
    Sonatype.view.viewport = viewport;
    Sonatype.view.serverTabPanel = viewport.findById('st-server-tab-panel');
    Sonatype.view.mainTabPanel = viewport.findById('st-main-tab-panel');
    Sonatype.view.supportedNexusTabs = {};

    size = Sonatype.view.serverTabPanel.getSize();
    Sonatype.view.serverTabPanel.setHeight(size.height - 20);

    // allow each included sonatype server to setup its tab and events
    availSvrs = Sonatype.config.installedServers;
    for (srv in availSvrs)
    {
      if (availSvrs.hasOwnProperty(srv)) {
        if (availSvrs[srv] && typeof(Sonatype[srv]) !== 'undefined')
        {
          Sonatype[srv][Strings.capitalize(srv)].initServerTab();
        }
      }
    }

    Sonatype.view.serverTabPanel.setActiveTab('st-nexus-tab');

    Ext.History.addListener('change', Sonatype.utils.onHistoryChange);
    Sonatype.view.mainTabPanel.addListener('tabchange', function(panel, tab) {
          Sonatype.utils.updateHistory(tab);
        });
  }

  // updateLoginLinkText : function(){
  // var loginEl = Ext.get('login-link');
  // var usernameEl = Ext.get('username');
  //    
  // if (Sonatype.user.curr.isLoggedIn){
  // usernameEl.update(Sonatype.user.curr.username + ' | ').show();
  // loginEl.update('Log Out');
  // }
  // else {
  // loginEl.update('Log In');
  // usernameEl.update('');
  // }
  // }
};


Sonatype.view.MainTabPanel = Ext.extend(Ext.TabPanel, {
      plugins: new Ext.ux.TabCloseMenu(),

      addOrShowTab : function(id, PanelClass, panelClassParams) {
        var  tab = this.getComponent(id);

        if (tab)
        {
          this.setActiveTab(tab);
        }
        else
        {
          tab = new PanelClass(Ext.apply({
                id : id
              }, panelClassParams||{}));
          this.add(tab);
        }

        this.setActiveTab(tab);

        return tab;
      }
    });

Sonatype.Events.addListener('nexusNavigationInit', function(panel) {
      Sonatype.view.viewport.doLayout();
      Sonatype.utils.updateGlobalTimeout();
    });


  return Sonatype;
});