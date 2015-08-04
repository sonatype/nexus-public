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
define('Sonatype/repoServer/PluginInfoTab', function() {

  Sonatype.repoServer.PluginInfoTab = function(config) {
    var config = config || {};
    var defaultConfig = {
      labelClass : 'font: bold 12px tahoma, arial, helvetica, sans-serif;',
      textClass : 'font: normal 12px tahoma, arial, helvetica, sans-serif; padding: 0px 0px 0px 15px;'
    };
    Ext.apply(this, config, defaultConfig);

    Sonatype.repoServer.PluginInfoTab.superclass.constructor.call(this, {
          frame : true,
          items : [{
                xtype : 'panel',
                style : 'padding: 10px 0px 0px 10px;',
                layout : 'table',
                layoutConfig : {
                  columns : 2
                },
                items : [{
                      xtype : 'label',
                      html : 'Name',
                      style : this.labelClass,
                      width : 120
                    }, {
                      xtype : 'label',
                      name : 'name',
                      style : this.textClass,
                      width : 320
                    }, {
                      xtype : 'label',
                      html : 'Version',
                      style : this.labelClass,
                      width : 120
                    }, {
                      xtype : 'label',
                      name : 'version',
                      style : this.textClass,
                      width : 320
                    }, {
                      xtype : 'label',
                      html : 'Status',
                      style : this.labelClass,
                      width : 120
                    }, {
                      xtype : 'label',
                      name : 'status',
                      style : this.textClass,
                      width : 320
                    }, {
                      xtype : 'label',
                      html : 'Description',
                      style : this.labelClass,
                      width : 120
                    }, {
                      xtype : 'label',
                      name : 'description',
                      style : this.textClass,
                      width : 320
                    }, {
                      xtype : 'label',
                      html : 'SCM Version',
                      style : this.labelClass,
                      width : 120
                    }, {
                      xtype : 'label',
                      name : 'scmVersion',
                      style : this.textClass,
                      width : 320
                    }, {
                      xtype : 'label',
                      html : 'SCM Timestamp',
                      style : this.labelClass,
                      width : 120
                    }, {
                      xtype : 'label',
                      name : 'scmTimestamp',
                      style : this.textClass,
                      width : 320
                    }]
              }],
          listeners : {
            beforerender : {
              fn : this.beforerenderHandler,
              scope : this
            }
          }
        });
  };

  Ext.extend(Sonatype.repoServer.PluginInfoTab, Ext.Panel, {
        beforerenderHandler : function(panel) {
          this.find('name', 'name')[0].setText(this.payload.data.name);
          this.find('name', 'version')[0].setText(this.payload.data.version);
          this.find('name', 'description')[0].setText(this.payload.data.description);
          this.find('name', 'status')[0].setText(this.capitalizeHead(this.payload.data.status));
          this.find('name', 'scmVersion')[0].setText(this.payload.data.scmVersion);
          this.find('name', 'scmTimestamp')[0].setText(this.payload.data.scmTimestamp);

          var pluginPropertiesPanel = this.items.get(0);

          var site = this.payload.data.site;
          if (!Ext.isEmpty(site))
          {
            pluginPropertiesPanel.add({
                  xtype : 'label',
                  html : 'Site',
                  style : this.labelClass,
                  width : 120
                });
            pluginPropertiesPanel.add({
                  xtype : 'label',
                  name : 'site',
                  html : '<a href="' + site + '" target="_blank">' + site + '</a>',
                  style : this.textClass
                });
          }

          var documentation = this.payload.data.documentation;
          if (!Ext.isEmpty(documentation) && documentation.length != 0)
          {
            pluginPropertiesPanel.add({
                  xtype : 'label',
                  html : 'Documentation',
                  style : this.labelClass,
                  width : 120
                });

            var link = '';
            for (var i = 0; i < documentation.length; i++)
            {
              if (i != 0)
              {
                link += ', ';
              }
              link += '<a href="' + documentation[i].url + '" target="_blank">' + documentation[i].label + '</a>';
            }
            pluginPropertiesPanel.add({
                  xtype : 'label',
                  name : 'site',
                  html : link,
                  style : this.textClass
                });
          }

          Sonatype.Events.fireEvent('pluginInfoTabInit',this);

          var failureReason = this.payload.data.failureReason;
          if (failureReason)
          {
            var html = '<h4 style="color:red;">This plugin was not able to be activated</h4><br/>';
            html = html + '<pre> ' + failureReason + '</pre><br/>';
            this.add({
                  xtype : 'panel',
                  frame : true,
                  style : 'padding: 20px 0px 0px 10px;',
                  autoScroll : true,
                  html : html
                });
          }
        },

        capitalizeHead : function(str) {
          if (Ext.isEmpty(str))
          {
            return str;
          }
          return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
        }
      });

  Sonatype.Events.addListener('pluginInfoInit', function(cardPanel, rec, gridPanel) {
        cardPanel.add(new Sonatype.repoServer.PluginInfoTab({
              name : 'info',
              tabTitle : 'Information',
              payload : rec
            }));
      });
});
