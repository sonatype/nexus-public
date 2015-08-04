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

define('Nexus/ext/TextEntryList',['extjs', 'nexus', 'Nexus/config'], function(Ext, Nexus, Config){
Ext.namespace('Nexus.ext');

/*
 * A widget that will allow custom strings to be entered (and removed) into a
 * listbox.
 */
Nexus.ext.TextEntryList = function(cfg) {
  var
        config = cfg || {},
        defaultConfig = {
          layout : 'form'
        };

  Ext.apply(this, config, defaultConfig);

  this.textEntryField = new Ext.form.Field({
        xtype : 'textfield',
        helpText : config.entryHelpText,
        name : 'entryName',
        width : 300,
        helpMarker : true
      });

  this.addEntryButton = new Ext.Button({
        xtype : 'button',
        text : 'Add',
        style : 'padding-left: 7px',
        minWidth : 100,
        id : 'button-add',
        handler : this.addNewEntry,
        scope : this
      });

  this.removeEntryButton = new Ext.Button({
        xtype : 'button',
        text : 'Remove',
        style : 'padding-left: 6px',
        minWidth : 100,
        id : 'button-remove',
        handler : this.removeEntry,
        scope : this
      });

  this.removeAllEntriesButton = new Ext.Button({
        xtype : 'button',
        text : 'Remove All',
        style : 'padding-left: 6px; margin-top: 5px',
        minWidth : 100,
        id : 'button-remove-all',
        handler : this.removeAllEntries,
        scope : this
      });

  this.entryList = new Ext.tree.TreePanel({
        xtype : 'treepanel',
        id : 'entry-list', // note: unique ID is assinged
        // before instantiation
        name : 'entry-list',
        title : config.listLabel,
        // cls: 'required-field',
        border : true, // note: this seem to have no effect w/in form panel
        bodyBorder : true, // note: this seem to have no effect w/in form
        // panel
        // note: this style matches the expected behavior
        bodyStyle : 'background-color:#FFFFFF; border: 1px solid #B5B8C8',
        style : 'padding: 0 20px 0 0',
        width : 320,
        height : 150,
        animate : true,
        lines : false,
        autoScroll : true,
        containerScroll : true,
        // @note: root node must be instantiated uniquely for each instance of
        // treepanel
        // @ext: can TreeNode be registerd as a component with an xtype so this
        // new root node
        // may be instantiated uniquely for each form panel that uses this
        // config?
        rootVisible : false,
        enableDD : false,
        root : new Ext.tree.TreeNode({
              text : 'root'
            })

      });

  Nexus.ext.TextEntryList.superclass.constructor.call(this, {
        autoScroll : true,
        border : false,
        collapsible : false,
        collapsed : false,
        labelWidth : 175,
        layoutConfig : {
          labelSeparator : ''
        },
        items : [{
              xtype : 'panel',
              fieldLabel : config.entryLabel,
              layout : 'column',
              items : [
                {
                    xtype : 'panel',
                    width : 320,
                    items : [this.textEntryField]
                  }, {
                    xtype : 'panel',
                    width : 120,
                    items : [this.addEntryButton]
                  }]
            }, {
              xtype : 'panel',
              fieldLabel : config.listLabel,
              layout : 'column',
              autoHeight : true,
              items : [this.entryList, {
                    xtype : 'panel',
                    width : 120,
                    items : [this.removeEntryButton, this.removeAllEntriesButton]
                  }]
            }]
      });

};

Ext.extend(Nexus.ext.TextEntryList, Ext.Panel, {

      addEntryNode : function(treePanel, entry) {
        var id = Ext.id();

        treePanel.root.appendChild(new Ext.tree.TreeNode({
                  id : id,
                  text : entry,
                  payload : entry,
                  allowChildren : false,
                  draggable : false,
                  leaf : true,
                  nodeType : 'entry',
                  icon : Config.extPath + '/resources/images/default/tree/leaf.gif'
                }));
      },

      addNewEntry : function() {
        var nodes, i, entry = this.textEntryField.getRawValue();


        if (entry)
        {
          nodes = this.entryList.root.childNodes;
          for (i = 0; i < nodes.length; i=i+1)
          {
            if (entry === nodes[i].attributes.payload)
            {
              this.textEntryField.markInvalid('This entry already exists');
              return;
            }
          }

          this.addEntryNode(this.entryList, entry);
          this.textEntryField.setRawValue('');
        }
      },

      removeEntry : function() {

        var selectedNode = this.entryList.getSelectionModel().getSelectedNode();
        if (selectedNode)
        {
          this.entryList.root.removeChild(selectedNode);
        }
      },

      removeAllEntries : function() {
        var treeRoot = this.entryList.root;

        while (treeRoot.lastChild)
        {
          treeRoot.removeChild(treeRoot.lastChild);
        }
      },

      setEntries : function(stringArray) {
        var entry, i;
        // first clear all
        this.removeAllEntries();

        for (i = 0; i < stringArray.length; i=i+1)
        {
          entry = stringArray[i];
          this.addEntryNode(this.entryList, entry);
        }

        return stringArray; // return stringArray, even if empty to comply with
                            // sonatypeLoad data modifier requirement
      },

      getEntries : function() {
        var
              i,
              outputArr = [],
              nodes = this.entryList.root.childNodes;

        for (i = 0; i < nodes.length; i=i+1)
        {
          outputArr[i] = nodes[i].attributes.payload;
        }

        return outputArr;
      }

    });

Ext.reg('textentrylist', Nexus.ext.TextEntryList);
});
