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
define('repoServer/UserPrivilegeBrowserPanel',['Sonatype/all'], function(){
Sonatype.repoServer.UserPrivilegeBrowsePanel = function(config) {
  var config = config || {};
  var defaultConfig = {};
  Ext.apply(this, config, defaultConfig);

  this.appliedPrivilegesDataStore = new Ext.data.JsonStore({
        root : 'data',
        id : 'id',
        url : Sonatype.config.servicePath + '/assigned_privileges/' + this.payload.get('userId'),
        sortInfo : {
          field : 'name',
          direction : 'ASC'
        },
        autoLoad : true,
        fields : [{
              name : 'id'
            }, {
              name : 'name'
            }, {
              name : 'parents'
            }],
        listeners : {
          load : {
            fn : function(store, records, options) {
              this.loadPrivilegeList();
            },
            scope : this
          }
        }
      });

  Sonatype.repoServer.UserPrivilegeBrowsePanel.superclass.constructor.call(this, {
        region : 'center',
        width : '100%',
        height : '100%',
        autoScroll : true,
        border : false,
        frame : false,
        collapsible : false,
        collapsed : false,
        tbar : [{
              text : 'Refresh',
              iconCls : 'st-icon-refresh',
              cls : 'x-btn-text-icon',
              scope : this,
              handler : this.refreshHandler
            }],
        items : [{
              xtype : 'panel',
              layout : 'table',
              layoutConfig : {
                columns : 2
              },
              frame : true,
              items : [{
                    xtype : 'panel',
                    style : 'padding: 10px; font-size: 11px;',
                    html : 'Select a privilege to view the role(s) in the user<br>that grant the privilege.'
                  }, {
                    xtype : 'panel',
                    style : 'padding: 10px; font-size: 11px;',
                    html : 'List of roles in the user that grant the selected privilege.<br>Expand the role to find nested role(s) that contain<br>the privilege.'
                  }, {
                    xtype : 'treepanel',
                    name : 'privilege-list',
                    title : 'Privileges',
                    border : true,
                    bodyBorder : true,
                    bodyStyle : 'background-color:#FFFFFF; border: 1px solid #B5B8C8',
                    style : 'padding: 0 20px 0 0',
                    width : 325,
                    height : 275,
                    animate : true,
                    lines : false,
                    autoScroll : true,
                    containerScroll : true,
                    rootVisible : false,
                    ddScroll : false,
                    enableDD : false,
                    root : new Ext.tree.TreeNode({
                          text : 'root',
                          draggable : false
                        })
                  }, {
                    xtype : 'treepanel',
                    name : 'role-tree',
                    title : 'Role Containment',
                    border : true,
                    bodyBorder : true,
                    bodyStyle : 'background-color:#FFFFFF; border: 1px solid #B5B8C8',
                    width : 325,
                    height : 275,
                    animate : true,
                    lines : false,
                    autoScroll : true,
                    containerScroll : true,
                    rootVisible : false,
                    ddScroll : false,
                    enableDD : false,
                    root : new Ext.tree.TreeNode({
                          text : 'root',
                          draggable : false
                        })
                  }]
            }]
      });
};

Ext.extend(Sonatype.repoServer.UserPrivilegeBrowsePanel, Ext.FormPanel, {
      refreshHandler : function(button, e) {
        // the load listener on these stores will reload the privileges
        this.appliedPrivilegesDataStore.reload();
        var tree = this.find('name', 'role-tree')[0];
        while (tree.root.lastChild)
        {
          tree.root.removeChild(tree.root.lastChild);
        }
      },
      clickHandler : function(node, event) {
        var tree = this.find('name', 'role-tree')[0];
        while (tree.root.lastChild)
        {
          tree.root.removeChild(tree.root.lastChild);
        }

        this.idCounter = 0;

        for (var i = 0; i < node.attributes.payload.data.parents.length; i++)
        {
          this.addRoleToTree(tree.root, node.attributes.payload.data.parents[i]);
        }
      },
      addRoleToTree : function(rootNode, childData) {
        var childNode = new Ext.tree.TreeNode({
              id : this.idCounter++,
              text : childData.name,
              payload : childData,
              allowChildren : (childData.parents && childData.parents.length > 0) ? true : false,
              draggable : false,
              leaf : (childData.parents && childData.parents.length > 0) ? false : true,
              icon : Sonatype.config.extPath + '/resources/images/default/tree/folder.gif'
            });

        rootNode.appendChild(childNode);

        if (childData.parents)
        {
          for (var i = 0; i < childData.parents.length; i++)
          {
            this.addRoleToTree(childNode, childData.parents[i]);
          }
        }
      },
      loadPrivilegeList : function() {
        var privilegeList = this.find('name', 'privilege-list')[0];
        if (!privilegeList) {
          // not displayed yet
          return;
        }
        while (privilegeList.root.lastChild)
        {
          privilegeList.root.removeChild(privilegeList.root.lastChild);
        }
        var privilegeRecs = this.appliedPrivilegesDataStore.getRange();
        for (var i = 0; i < privilegeRecs.length; i++)
        {
          privilegeList.root.appendChild(new Ext.tree.TreeNode({
                id : privilegeRecs[i].data.id,
                text : privilegeRecs[i].data.name,
                payload : privilegeRecs[i], // sonatype added
                allowChildren : false,
                draggable : false,
                leaf : true,
                listeners : {
                  click : {
                    fn : this.clickHandler,
                    scope : this
                  }
                }
              }));
        }
      }
    });

Sonatype.Events.addListener('userViewInit', function(cardPanel, rec, gridPanel) {
      if (rec.data.resourceURI)
      {
        cardPanel.add(new Sonatype.repoServer.UserPrivilegeBrowsePanel({
              payload : rec,
              tabTitle : 'Privilege Trace'
            }));
      }
    });
});

