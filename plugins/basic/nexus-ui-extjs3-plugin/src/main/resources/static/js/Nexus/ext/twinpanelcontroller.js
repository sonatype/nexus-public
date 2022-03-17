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
/*
 * Target Edit/Create panel layout and controller
 */
/*global define*/
define('Nexus/ext/twinpanelcontroller', ['extjs', 'nexus', 'Nexus/config'], function(Ext, Nexus, Config) {

Ext.namespace('Nexus.ext');

Nexus.ext.TwinPanelController = function(cfg) {
  Ext.apply(this, cfg || {});

  this.addOneButton = new Ext.Button({
        xtype : 'button',
        handler : this.addOne,
        scope : this,
        tooltip : 'Add',
        icon : Config.extPath + '/resources/images/default/grid/page-prev.gif',
        cls : 'x-btn-icon'
      });

  this.addAllButton = new Ext.Button({
        xtype : 'button',
        handler : this.addAll,
        scope : this,
        tooltip : 'Add All',
        icon : Config.extPath + '/resources/images/default/grid/page-first.gif',
        cls : 'x-btn-icon'
      });

  this.removeOneButton = new Ext.Button({
        xtype : 'button',
        handler : this.removeOne,
        scope : this,
        tooltip : 'Remove',
        icon : Config.extPath + '/resources/images/default/grid/page-next.gif',
        cls : 'x-btn-icon'
      });

  this.removeAllButton = new Ext.Button({
        xtype : 'button',
        handler : this.removeAll,
        scope : this,
        tooltip : 'Remove All',
        icon : Config.extPath + '/resources/images/default/grid/page-last.gif',
        cls : 'x-btn-icon'
      });

  Nexus.ext.TwinPanelController.superclass.constructor.call(this, {
        layout : 'table',
        style : 'padding-top: ' + (this.halfSize ? 40 : 100) + 'px; padding-right: 10px; padding-left: 10px',
        width : 45,
        defaults : {
          style : 'margin-bottom: 3px'
        },
        layoutConfig : {
          columns : 1
        },
        items : [this.addOneButton, this.addAllButton, this.removeOneButton, this.removeAllButton]
      });

};

Ext.extend(Nexus.ext.TwinPanelController, Ext.Panel, {
      disable : function() {
        this.addOneButton.disable();
        this.addAllButton.disable();
        this.removeOneButton.disable();
        this.removeAllButton.disable();
      },
      enable : function() {
        this.addOneButton.enable();
        this.addAllButton.enable();
        this.removeOneButton.enable();
        this.removeAllButton.enable();
      },
      addOne : function() {
        this.moveItems(2, 0, false);
      },

      addAll : function() {
        this.moveItems(2, 0, true);
      },

      removeOne : function() {
        this.moveItems(0, 2, false);
      },

      removeAll : function() {
        this.moveItems(0, 2, true);
      },

      moveItems : function(fromIndex, toIndex, moveAll) {
        var
              i, node, selectedNodes,
              fromPanel = this.ownerCt.getComponent(fromIndex),
              toPanel = this.ownerCt.getComponent(toIndex),
              fromRoot = fromPanel.root,
              toRoot = toPanel.root,

              dragZone = fromPanel.dragZone,
              dropZone = toPanel.dropZone,
              fn = toPanel.dropConfig.onContainerOver.createDelegate(dropZone, [dragZone, null], 0),
              checkIfDragAllowed = function(node) {
                var dropTarget = fn({
                  node : node
                });
                return (!node.disabled) && dropTarget === dropZone.dropAllowed;
              };

        if (fromPanel && toPanel)
        {
          if (toPanel.sorter && toPanel.sorter.disableSort)
          {
            toPanel.sorter.disableSort(toPanel);
          }
          if (fromPanel.sorter && fromPanel.sorter.disableSort)
          {
            fromPanel.sorter.disableSort(fromPanel);
          }
          if (moveAll)
          {
            while (fromRoot.firstChild) {
              toRoot.appendChild(fromRoot.firstChild);
            }
          }
          else
          {
            selectedNodes = fromPanel.getSelectionModel().getSelectedNodes();
            if (selectedNodes)
            {
              for (i = 0; i < selectedNodes.length; i=i+1)
              {
                node = selectedNodes[i];
                if (checkIfDragAllowed(node))
                {
                  toRoot.appendChild(node);
                }
              }
            }
          }
          if (toPanel.sorter && toPanel.sorter.enableSort)
          {
            toPanel.sorter.enableSort(toPanel);
          }
          if (fromPanel.sorter && fromPanel.sorter.enableSort)
          {
            fromPanel.sorter.enableSort(fromPanel);
          }
        }
      }
    });

Ext.reg('twinpanelcontroller', Nexus.ext.TwinPanelController);

Nexus.ext.TwinPanelChooser = function(cfg) {
  var
        config = cfg || {},
        defaultConfig = {
          doubleWide : false,
          displayField : 'name',
          validateLeftItems : false,
          validateLeftItemsText : 'Invalid items selected.',
          nodeIcon : Config.extPath + '/resources/images/default/tree/leaf.gif'
        };
  Ext.apply(this, config, defaultConfig);

  Nexus.ext.TwinPanelChooser.superclass.constructor.call(this, {
        layout : 'column',
        autoHeight : true,
        style : 'padding: 10px 0 10px 0',
        listeners : {
          beforedestroy : {
            fn : function() {
              if (this.store)
              {
                this.loadStore();
                this.store.un('load', this.loadStore, this);
              }
            },
            scope : this
          }
        },

        items : [{
              xtype : 'multiselecttreepanel',
              name : 'targettree',
              // id: '_staging-profiles-target-groups-tree', //note: unique ID
              // is assinged before instantiation
              title : this.titleLeft,
              cls : this.required ? 'required-field' : null,
              border : true, // note: this seem to have no effect w/in form panel
              bodyBorder : true, // note: this seem to have no effect w/in form panel
              // note: this style matches the expected behavior
              bodyStyle : 'background-color:#FFFFFF; border: 1px solid #B5B8C8',
              width : this.doubleWide ? 400 : 225,
              height : this.halfSize ? 150 : 300,
              animate : true,
              lines : false,
              autoScroll : true,
              containerScroll : true,
              // @note: root node must be instantiated uniquely for each instance of treepanel
              // @ext: can TreeNode be registerd as a component with an xtype so this new root
              // node may be instantiated uniquely for each form panel that uses this config?
              rootVisible : false,
              root : new Ext.tree.TreeNode({
                    text : 'root'
                  }),
              enableDD : true,
              ddScroll : true,
              dropConfig : {
                allowContainerDrop : true,
                onContainerDrop : function(source, e, data) {
                  var i;

                  if (data.nodes)
                  {
                    for (i = 0; i < data.nodes.length; i=i+1)
                    {
                      this.tree.root.appendChild(data.nodes[i]);
                    }
                  }
                  return true;
                },
                onContainerOver : function(source, e, data) {
                  return this.dropAllowed;
                },
                // passign padding to make whole treePanel the drop zone. This is dependent
                // on a sonatype fix in the Ext.dd.DropTarget class. This is necessary
                // because treepanel.dropZone.setPadding is never available in
                // time to be useful.
                padding : [0, 0, (this.halfSize ? 124 : 274), 0]
              },
              // added Field values to simulate form field validation
              invalidText : 'Select one or more items',
              validate : function() {
                return (this.root.childNodes.length > 0);
              },
              invalid : false,
              listeners : {
                'append' : {
                  fn : function(tree, parentNode, insertedNode, i) {
                    this.clearInvalid();
                  },
                  scope : this
                },
                'remove' : {
                  fn : function(tree, parentNode, removedNode) {
                    if (tree.root.childNodes.length < 1 && this.required)
                    {
                      this.markTreeInvalid(tree, null);
                    }
                    else
                    {
                      this.clearInvalid();
                    }
                  },
                  scope : this
                }
              }
            }, {
              xtype : 'twinpanelcontroller',
              name : 'twinpanel',
              halfSize : this.halfSize
            }, {
              xtype : 'multiselecttreepanel',
              name : 'sourcetree',
              // id: id + '_staging-profiles-available-groups-tree', //note:
              // unique ID is assinged before instantiation
              title : this.titleRight,
              border : true, // note: this seem to have no effect w/in form
                              // panel
              bodyBorder : true, // note: this seem to have no effect w/in form
                                  // panel
              // note: this style matches the expected behavior
              bodyStyle : 'background-color:#FFFFFF; border: 1px solid #B5B8C8',
              width : this.doubleWide ? 400 : 225,
              height : (this.halfSize ? 150 : 300) + (Ext.isGecko ? 15 : 0),
              animate : true,
              lines : false,
              autoScroll : true,
              containerScroll : true,
              // @note: root node must be instantiated uniquely for each
              // instance of treepanel
              // @ext: can TreeNode be registerd as a component with an xtype so
              // this new root node
              // may be instantiated uniquely for each form panel that uses this
              // config?
              rootVisible : false,
              root : new Ext.tree.TreeNode({
                    text : 'root'
                  }),
              enableDD : true,
              ddScroll : true,
              dropConfig : {
                allowContainerDrop : true,
                onContainerDrop : function(source, e, data) {
                  var i;

                  if (data.nodes)
                  {
                    for (i = 0; i < data.nodes.length; i=i+1)
                    {
                      this.tree.root.appendChild(data.nodes[i]);
                    }
                  }
                  return true;
                },
                onContainerOver : function(source, e, data) {
                  return this.dropAllowed;
                },
                // passign padding to make whole treePanel the drop zone. This
                // is dependent
                // on a sonatype fix in the Ext.dd.DropTarget class. This is
                // necessary
                // because treepanel.dropZone.setPadding is never available in
                // time to be useful.
                padding : [0, 0, (this.halfSize ? 124 : 274), 0]
              }
            }]
      });

  if (this.store)
  {
    this.loadStore();
    this.store.on('load', this.loadStore, this);
  }
};

Ext.extend(Nexus.ext.TwinPanelChooser, Ext.Panel, {
      disable : function() {
        this.find('name', 'twinpanel')[0].disable();
        this.find('name', 'sourcetree')[0].dragZone.lock();
        this.find('name', 'targettree')[0].dragZone.lock();
      },
      enable : function() {
        this.find('name', 'twinpanel')[0].enable();
        this.find('name', 'sourcetree')[0].dragZone.unlock();
        this.find('name', 'targettree')[0].dragZone.unlock();
      },
      createNode : function(root, rec) {
        root.appendChild(new Ext.tree.TreeNode({
              id : rec.id,
              text : rec.data[this.displayField],
              payload : rec,
              allowChildren : false,
              draggable : true,
              leaf : true,
              disabled : rec.data.readOnly,
              icon : this.nodeIcon
            }));
      },

      loadStore : function() {
        if (this.store)
        {
          var root = this.getComponent(2).root;
          while (root.lastChild)
          {
            root.removeChild(root.lastChild);
          }
          this.store.each(function(rec) {
                this.createNode(root, rec);
              }, this);
        }
      },

      clearInvalid : function() {
        var tree = this.getComponent(0);
        if (tree.invalid)
        {
          // remove error messaging
          tree.getEl().child('.x-panel-body').setStyle({
                'background-color' : '#FFFFFF',
                border : '1px solid #B5B8C8'
              });
          Ext.form.Field.msgFx.normal.hide(tree.errorEl, tree);
        }
      },

      markTreeInvalid : function(tree, errortext) {
        if (!tree)
        {
          tree = this.getComponent(0);
        }
        var
              elp = tree.getEl(),
              oldErrorText = tree.invalidText;

        if (elp)
        {
          if (!tree.errorEl)
          {
            tree.errorEl = elp.createChild({
                  cls : 'x-form-invalid-msg'
                });
            tree.errorEl.setWidth(elp.getWidth(true)); // note removed -20 like on form fields
          }
          tree.invalid = true;
          if (errortext)
          {
            tree.invalidText = errortext;
          }
          tree.errorEl.update(tree.invalidText);
          tree.invalidText = oldErrorText;
          elp.child('.x-panel-body').setStyle({
                'background-color' : '#fee',
                border : '1px solid #dd7870'
              });
          Ext.form.Field.msgFx.normal.show(tree.errorEl, tree);
        }
      },

      validate : function() {
        var
              valid,
              leftTree = this.getComponent(0);

        if (this.containsInvalidNode())
        {
          this.markTreeInvalid(leftTree, this.validateLeftItemsText);
          return false;
        }

        if (!this.required) {
          return true;
        }

        valid = leftTree.validate.call(leftTree);
        if (!valid)
        {
          this.markTreeInvalid(leftTree, null);
        }
        return valid;
      },

      containsInvalidNode : function() {
        if (!this.validateLeftItems)
        {
          return false;
        }
        var
              i,
              root = this.getComponent(0).root,
              nodes = root.childNodes;

        for (i = 0; i < nodes.length; i=i+1)
        {
          if (nodes[i].attributes.payload.invalid === true)
          {
            return true;
          }
        }

        return false;
      },

      getValue : function() {
        var
              i,
              output = [],
              nodes = this.getComponent(0).root.childNodes;

        for (i = 0; i < nodes.length; i=i+1)
        {
          if (!nodes[i].disabled)
          {
            output.push(this.valueField ? nodes[i].attributes.payload.data[this.valueField] : nodes[i].attributes.payload.data);
          }
        }

        return output;
      },

      setValue : function(arr) {
        if (!Ext.isArray(arr))
        {
          arr = [arr];
        }

        var
              i, j,
              name, valueId, displayValue, found = false, readOnly, node, nodeValue, rec,
              leftRoot = this.getComponent(0).root,
              rightRoot = this.getComponent(2).root,
              nodes = rightRoot.childNodes;

        while (leftRoot.lastChild)
        {
          leftRoot.removeChild(leftRoot.lastChild);
        }

        this.loadStore();

        for (i = 0; i < arr.length; i=i+1)
        {
          valueId = arr[i];
          name = valueId;
          readOnly = false;
          if (typeof(valueId) !== 'string')
          {
            name = valueId[this.displayField];
            readOnly = valueId.readOnly;
            valueId = valueId[this.valueField];
          }
          for (j = 0; j < nodes.length; j=j+1)
          {
            node = nodes[j];
            nodeValue = this.valueField ? node.attributes.payload.data[this.valueField] : node.attributes.payload.id;
            if (nodeValue === valueId)
            {
              leftRoot.appendChild(node);
              if (readOnly)
              {
                node.disable();
              }
              found = true;
              break;
            }
          }
          if (!found)
          {
            rec = {
              id : valueId,
              data : {
                readOnly : readOnly
              }
            };
            rec.data[this.valueField] = valueId;
            displayValue = valueId;
            if (this.validateLeftItems)
            {
              displayValue += ' - (Invalid)';
            }
            rec.data[this.displayField] = displayValue;
            rec.invalid = true;
            this.createNode(leftRoot, rec);
          }
        }

        this.doLayout();
      }
    });

Ext.reg('twinpanelchooser', Nexus.ext.TwinPanelChooser);
return Nexus;
});
