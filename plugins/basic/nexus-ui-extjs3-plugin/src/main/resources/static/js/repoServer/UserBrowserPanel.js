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
define('repoServer/UserBrowserPanel',['Sonatype/all'], function(){
Sonatype.repoServer.UserBrowsePanel = function(config) {
  var config = config || {};
  var defaultConfig = {
    titleColumn : 'name',
    isRole : false
  };
  Ext.apply(this, config, defaultConfig);

  this.roleTreeDataStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'id',
        baseParams : {
          isRole : this.isRole
        },
        proxy : new Ext.data.HttpProxy({
              url : Sonatype.config.servicePath + '/role_tree/' + this.parentId,
              method : 'GET'
            }),
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : true,
        fields : [{
              name : 'id'
            }, {
              name : 'type'
            }, {
              name : 'name'
            }, {
              name : 'children'
            }],
        listeners : {
          load : {
            fn : this.roleTreeLoadHandler,
            scope : this
          }
        }
      });

  Sonatype.repoServer.UserBrowsePanel.superclass.constructor.call(this, {
        anchor : '0 -2',
        bodyStyle : 'background-color:#FFFFFF',
        animate : true,
        lines : false,
        autoScroll : true,
        containerScroll : true,
        rootVisible : true,
        enableDD : false,
        root : new Ext.tree.TreeNode({
              text : this.parentName,
              draggable : false
            }),
        tbar : [{
              text : 'Refresh',
              iconCls : 'st-icon-refresh',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.refreshHandler
            }]
      });

  new Ext.tree.TreeSorter(this, {
        folderSort : true
      });
};

Ext.extend(Sonatype.repoServer.UserBrowsePanel, Ext.tree.TreePanel, {
      refreshHandler : function(button, e) {
        this.roleTreeDataStore.reload();
      },
      roleTreeLoadHandler : function(store, records, options) {
        if (!this.getRootNode()) {
          // not rendered yet
          return;
        }
        this.idCounter = 0;
        while (this.getRootNode().lastChild)
        {
          this.getRootNode().removeChild(this.getRootNode().lastChild);
        }
        for (var i = 0; i < records.length; i++)
        {
          this.loadItemIntoTree(records[i].data.name, records[i].data.type, this.getRootNode(), records[i].data.children);
        }
      },
      loadItemIntoTree : function(name, type, parentNode, children) {
        var childNode = new Ext.tree.TreeNode({
              id : this.idCounter++,
              text : name,
              allowChildren : (children && children.length > 0) ? true : false,
              draggable : false,
              leaf : (children && children.length > 0) ? false : true,
              icon : (type == 'role') ? (Sonatype.config.extPath + '/resources/images/default/tree/folder.gif') : (Sonatype.config.extPath + '/resources/images/default/tree/leaf.gif')
            });

        parentNode.appendChild(childNode);

        if (children)
        {
          for (var i = 0; i < children.length; i++)
          {
            this.loadItemIntoTree(children[i].name, children[i].type, childNode, children[i].children)
          }
        }
      }
    });

Sonatype.Events.addListener('userViewInit', function(cardPanel, rec, gridPanel) {
      if (rec.data.resourceURI)
      {
        var parentName = null;

        if (rec.data.firstName)
        {
          parentName = rec.data.firstName;
        }

        if (rec.data.lastName)
        {
          parentName += ' ' + rec.data.lastName;
        }

        if (parentName == null)
        {
          parentName = rec.data.userId;
        }
        cardPanel.add(new Sonatype.repoServer.UserBrowsePanel({
              payload : rec,
              tabTitle : 'Role Tree',
              parentId : rec.data.userId,
              parentName : parentName
            }));
      }
    });

Sonatype.Events.addListener('roleViewInit', function(cardPanel, rec, gridPanel) {
      if (rec.data.resourceURI)
      {
        cardPanel.add(new Sonatype.repoServer.UserBrowsePanel({
              payload : rec,
              tabTitle : 'Role Tree',
              parentId : rec.data.id,
              isRole : true,
              parentName : rec.data.name
            }));
      }
    });
});

