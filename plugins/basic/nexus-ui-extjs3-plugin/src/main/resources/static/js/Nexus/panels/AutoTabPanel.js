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
/*global Ext, Sonatype, Nexus, NX*/

/*
 * A helper panel creating a tabbed container inside itself if more than one
 * component is added.
 */
NX.define('Nexus.panels.AutoTabPanel', {
  extend : 'Ext.Panel',

  requirejs : ['Sonatype/view'],

  constructor : function(cfg) {
    var
          config = cfg || {},
          defaultConfig = {
            layout : 'card',
            activeItem : 0,
            deferredRender : false,
            autoScroll : false,
            frame : false,
            border : false,
            activeTab : 0,
            hideMode : 'offsets',
            tools : [
              {
                id : 'refresh',
                qtip : 'Refresh data',
                hidden : true,
                handler : function(evt, toolEl, panel) {
                  var active;
                  if (panel.tabPanel) {
                    active = panel.tabPanel.getActiveTab();
                  } else {
                    active = panel.getComponent(0);
                  }
                  if (active && active.refreshContent) {
                    active.refreshContent();
                  }
                }
              }
            ]
          };
    Ext.apply(this, config, defaultConfig);
    Sonatype.panels.AutoTabPanel.superclass.constructor.call(this, {
      collapseMode : 'mini'
    });
  },

  add : function(c) {
    if (this.items && this.items.length > 0) {
      if (!this.tabPanel) {
        this.initTabPanel();
      }

      if (!c.title && c.tabTitle) {
        c.title = c.tabTitle;
      }
      return this.tabPanel.add(c);
    }
    else {
      return Sonatype.panels.AutoTabPanel.superclass.add.call(this, c);
    }
  },
  insert : function(index, c) {
    if (this.items && this.items.length > 0) {
      if (!this.tabPanel) {
        this.initTabPanel();
      }

      if (!c.title && c.tabTitle) {
        c.title = c.tabTitle;
      }
      return this.tabPanel.insert(index, c);
    }
    else {
      return Sonatype.panels.AutoTabPanel.superclass.insert.call(this, index, c);
    }
  },
  initTabPanel : function() {
    var first = this.getComponent(0);
    this.remove(first, false);
    first.setTitle(first.tabTitle);

    this.tabPanel = new Ext.TabPanel({
      activeItem : this.activeTab === -1 ? null : this.activeTab,
      deferredRender : false,
      enableTabScroll : true,
      autoScroll : false,
      frame : false,
      border : false,
      layoutOnTabChange : true,
      items : [first],
      hideMode : 'offsets',
      listeners : {
        tabchange : function(panel, tab) {
          var tool = this.tools && this.tools.refresh;
          if (tool) {
            if (tab.refreshContent) {
              tool.show();
            } else {
              tool.hide();
            }
          }
        },
        scope : this
      }
    });

    Sonatype.panels.AutoTabPanel.superclass.add.call(this, this.tabPanel);
    if (this.getLayout() && this.getLayout().setActiveItem) {
      this.getLayout().setActiveItem(this.tabPanel);
    }
  }
}, function() {
  Ext.ns('Sonatype.panels').AutoTabPanel = Nexus.panels.AutoTabPanel;
});
