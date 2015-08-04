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
define('Nexus/navigation/section',['extjs', 'sonatype', 'Sonatype/strings'], function(Ext, Sonatype, Strings){
Ext.namespace('Sonatype.navigation');

Sonatype.navigation.Section = function(cfg) {
  var
        config = cfg || {},
        defaultConfig = {
          collapsible : true,
          titleCollapse : true,
          collapsed : false
        };

  if (config.items)
  {
    config.items = this.transformItem(config.items);
  }
  if (!config.items || config.items.length === 0)
  {
    config.hidden = true;
  }

  Ext.apply(this, config, defaultConfig);

  Sonatype.navigation.Section.superclass.constructor.call(this, {
        cls : 'st-server-sub-container',
        layout : 'fit',
        frame : true,
        autoHeight : true,
        hideMode : 'offsets',
        animCollapse : false,
        listeners : {
          collapse : this.collapseExpandHandler,
          expand : this.collapseExpandHandler,
          scope : this
        }
      });
};

Ext.extend(Sonatype.navigation.Section, Ext.Panel, {
      collapseExpandHandler : function() {
        if (this.layout && this.layout.layout)
        {
          this.ownerCt.doLayout();
        }
      },

      transformItem : function(c) {
        if (!c) {
          return null;
        }

        var i, item, c2;

        if (Ext.isArray(c))
        {
          c2 = [];
          for (i = 0; i < c.length; i=i+1)
          {
            item = this.transformItem(c[i]);
            if (item)
            {
              c2.push(item);
            }
          }
          return c2;
        }

        if (!c.xtype)
        {
          if (c.href)
          {
            // regular external link
            return {
              sortable_title : c.title,
              autoHeight : true,
              html : '<ul class="group-links"><li><a href="' + c.href + '" target="' + c.href + '"' +
                    (c.style ? ' style="' + c.style + '"' : '') +
                    '>' + c.title + '</a></li></ul>'
            };
          }
          else if (c.tabCode || c.handler)
          {

            if (Sonatype.view.supportedNexusTabs)
            {
              Sonatype.view.supportedNexusTabs[c.tabId] = true;
            }
            // panel open action
            return (c.enabled === false) ? null : {
              sortable_title : c.title,
              autoHeight : true,
              id : 'navigation-' + c.tabId,
              initialConfigNavigation : c,
              listeners : {
                render : {
                  fn : function(panel) {
                    panel.body.on('click', Ext.emptyFn, null, {
                          delegate : 'a',
                          preventDefault : true
                        });
                    panel.body.on('mousedown', function(e, target) {
                          e.stopEvent();
                          if (c.handler)
                          {
                            c.handler();
                          }
                          else
                          {
                            Sonatype.view.mainTabPanel.addOrShowTab(c.tabId, c.tabCode, {
                                  title : c.tabTitle || c.title
                                });
                          }
                        }, c.scope, {
                          delegate : 'a'
                        });
                  },
                  scope : this
                }
              },
              html : '<ul class="group-links"><li><a href="#">' + c.title + '</a></li></ul>'
            };
          }
        }
        return c;
      },

      add : function(c) {
        var
              i, arr = null, a = arguments;
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

        c = this.transformItem(c);
        if (!c) {
          return;
        }

        if (this.hidden)
        {
          this.show();
        }
        return Sonatype.navigation.Section.superclass.add.call(this, c);
      },

      sort : function(asOrder) {
        if(!this.items)
        {
            return;
        }
          
        var _fSorter = function(obj1, obj2) {
           var fieldName = "sortable_title";
           return Strings.sortFn(obj1[fieldName], obj2[fieldName]);
        };
        this.items.sort(asOrder || 'ASC', _fSorter);
      }
    });

});
