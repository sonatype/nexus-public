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
define('Sonatype/repoServer/searchLeftNav', function() {

  var SEARCH_FIELD_CONFIG = {
    xtype : 'trigger',
    triggerClass : 'x-form-search-trigger',
    listeners : {
      'specialkey' : {
        fn : function(f, e) {
          if (e.getKey() == e.ENTER)
          {
            this.onTriggerClick();
          }
        }
      }
    },
    onTriggerClick : function(a, b, c) {
      var v = this.getRawValue();
      if (v.length > 0)
      {
        var panel = Sonatype.view.mainTabPanel.addOrShowTab('nexus-search', Sonatype.repoServer.SearchPanel, {
              title : 'Search'
            });
        panel.startQuickSearch(v);
        // window.location = 'index.html#nexus-search;quick~' + v;
      }
    }
  };

  Sonatype.Events.addListener('nexusNavigationInit', function(nexusPanel) {
        if (Sonatype.lib.Permissions.checkPermission('nexus:index', Sonatype.lib.Permissions.READ))
        {
          nexusPanel.insert(0,{
                title : 'Artifact Search',
                id : 'st-nexus-search',
                items : [Ext.apply({
                          repoPanel : this,
                          id : 'quick-search--field',
                          width : 140
                        }, SEARCH_FIELD_CONFIG), {
                      title : 'Advanced Search',
                      tabCode : Sonatype.repoServer.SearchPanel,
                      tabId : 'nexus-search',
                      tabTitle : 'Search'
                    }]
              });
        }
      });

  Sonatype.Events.addListener('welcomePanelInit', function(repoServer, welcomePanelConfig) {
        if (Sonatype.lib.Permissions.checkPermission('nexus:index', Sonatype.lib.Permissions.READ))
        {
          welcomePanelConfig.items.push({
                layout : 'form',
                border : false,
                frame : false,
                labelWidth : 10,
                items : [{
                      border : false,
                      html : '<div class="little-padding">' + 'Type in the name of a project, class, or artifact into the text box ' + 'below, and click Search. Use "Advanced Search" on the left for more options.' + '</div>'
                    }, Ext.apply({
                          repoPanel : repoServer,
                          id : 'quick-search-welcome-field',
                          anchor : '-10',
                          labelSeparator : ''
                        }, SEARCH_FIELD_CONFIG)]
              });
        }
      });

  Sonatype.Events.addListener('welcomeTabRender', function() {
        var c = Ext.getCmp('quick-search-welcome-field');
        if (c)
        {
          c.focus(true, 100);
        }
      });
});