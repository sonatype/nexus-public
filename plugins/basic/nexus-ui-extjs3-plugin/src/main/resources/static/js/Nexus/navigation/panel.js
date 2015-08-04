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
define('Nexus/navigation/panel',['extjs', 'sonatype'], function(Ext, Sonatype){
Ext.namespace('Sonatype.navigation');

Sonatype.navigation.NavigationPanel = function(cfg) {
  var
        config = cfg || {},
        defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  this.delayedItems = {};

  Sonatype.navigation.NavigationPanel.superclass.constructor.call(this, {
    cls : 'st-server-panel',
    autoScroll : true,
    border : false,
    items : []
  });
};

Ext.extend(Sonatype.navigation.NavigationPanel, Ext.Panel, {
  insert : function(sectionIndex, container) {
    if (!sectionIndex) {
      sectionIndex = 0;
    }
    if (!container) {
      return;
    }

    var panel;

    // check if this is an attempt to add a navigation item to an existing
    // section
    if (container.sectionId)
    {
      panel = this.findById(container.sectionId);
      if (panel)
      {
        return panel.insert(sectionIndex, container);
      }
      else
      {
        if (!this.delayedItems[container.sectionId])
        {
          this.delayedItems[container.sectionId] = [];
        }
        this.delayedItems[container.sectionId].push(container);
        return null;
      }
    }

    panel = new Sonatype.navigation.Section(container);
    panel = Sonatype.navigation.NavigationPanel.superclass.insert.call(this, sectionIndex, panel);
    if (panel.id && this.delayedItems[panel.id])
    {
      panel.add(this.delayedItems[panel.id]);
      this.delayedItems[panel.id] = null;
    }
    return panel;
  },
  add : function(c) {
    var i, arr = null, a = arguments, panel, newPanel;

    if (a.length > 1)
    {
      arr = a;
    }
    else if (Ext.isArray(c))
    {
      arr = c;
    }
    if (arr !== null)
    {
      for (i = 0; i < arr.length; i=i+1)
      {
        this.add(arr[i]);
      }
      return;
    }

    if (!c) {
      return;
    }

    // check if this is an attempt to add a navigation item to an existing
    // section
    if (c.sectionId)
    {
      panel = this.findById(c.sectionId);
      if (panel)
      {
        newPanel = panel.add(c);
        panel.sort();
        return newPanel;
      }
      else
      {
        if (!this.delayedItems[c.sectionId])
        {
          this.delayedItems[c.sectionId] = [];
        }
        this.delayedItems[c.sectionId].push(c);
        return null;
      }
    }

    panel = new Sonatype.navigation.Section(c);
    panel = Sonatype.navigation.NavigationPanel.superclass.add.call(this, panel);
    if (panel.id && this.delayedItems[panel.id])
    {
      panel.add(this.delayedItems[panel.id]);
      this.delayedItems[panel.id] = null;
      panel.sort();
    }
    return panel;
  }
});
});
