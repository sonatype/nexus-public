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
// allow eval statement because it's used for uiProvider attribute, not sure where that is set so we cannot drop it
// without potentially breaking things
/*jslint evil:true*/

define('ext/tree/sonatype',['extjs'], function(Ext){
Ext.tree.SonatypeTreeLoader = function(config) {
  config.requestMethod = "GET";

  Ext.tree.SonatypeTreeLoader.superclass.constructor.call(this, config);
};

Ext.extend(Ext.tree.SonatypeTreeLoader, Ext.tree.TreeLoader, {
      load : function(node, callback) {
        if (this.clearOnLoad)
        {
          while (node.firstChild)
          {
            node.removeChild(node.firstChild);
          }
        }
        if (this.doPreload(node))
        { // preloaded json children
          if (typeof callback === "function")
          {
            callback();
          }
        }
        else if (typeof(this.dataUrl) !== 'undefined' || typeof(this.url) !== 'undefined')
        { // diff
          this.requestData(node, callback);
        }
      },

      // override to request data according ot Sonatype's Nexus REST service
      requestData : function(node, callback) {
        if (this.fireEvent("beforeload", this, node, callback) !== false)
        {
          this.transId = Ext.Ajax.request({
                method : this.requestMethod,
                url : node.id + '?isLocal', // diff
                success : this.handleResponse,
                failure : this.handleFailure,
                options : {
                  dontForceLogout : true
                },
                scope : this,
                argument : {
                  callback : callback,
                  node : node
                },
                // disableCaching: false, //diff
                params : this.getParams(node)
              });
        }
        else
        {
          // if the load is cancelled, make sure we notify
          // the node that we are done
          if (typeof callback === "function")
          {
            callback();
          }
        }
      },

      getParams : function(node) {
        var buf = [], bp = this.baseParams, key;
        for (key in bp)
        {
          if (typeof bp[key] !== "function")
          {
            buf.push(encodeURIComponent(key), "=", encodeURIComponent(bp[key]), "&");
          }
        }
        // buf.push("node=", encodeURIComponent(node.id)); //diff
        return buf.join("");
      },

      processResponse : function(response, node, callback) {
        try
        {
          var i, len, data = Ext.decode(response.responseText).data;
          if (this.jsonRoot) {
            data = data[this.jsonRoot];
          }
          
          node.beginUpdate();
          for (i = 0, len = data.length; i < len; i=i+1)
          {
            if (this.createNode(data[i]))
            {
              node.appendChild(this.createNode(data[i]));
            }
          }
          node.endUpdate();
          if (typeof callback === "function")
          {
            callback(this, node);
          }
        }
        catch (e)
        {
          this.handleFailure(response);
        }
      },

      // FIXME this method is only copy/paste? need to compare
      createNode : function(attr) {
        // apply baseAttrs, nice idea Corey!
        if (this.baseAttrs)
        {
          Ext.applyIf(attr, this.baseAttrs);
        }
        if (this.nodeTextAttribute){
          attr.text = attr[this.nodeTextAttribute];
        }
        if (this.nodeForceLeaf){
          attr.leaf = true;
        }
        if (!attr.id)
        { // diff
          attr.id = attr.resourceURI; // diff
        } // diff
        if (this.applyLoader !== false)
        {
          attr.loader = this;
        }
        if (typeof attr.uiProvider === 'string')
        {
          attr.uiProvider = this.uiProviders[attr.uiProvider] || eval(attr.uiProvider);
        }

        attr.singleClickExpand = true; // diff

        return (attr.leaf ? new Ext.tree.TreeNode(attr) : new Ext.tree.AsyncTreeNode(attr));
      }
    });

// create constructor for new class
Ext.tree.SonatypeTreeSorter = function(el, config) {
  Ext.tree.SonatypeTreeSorter.superclass.constructor.call(this, el, config);
};

Ext.extend(Ext.tree.SonatypeTreeSorter, Ext.tree.TreeSorter, {
      disableSort : function(tree) {
        tree.un("beforechildrenrendered", this.doSort, this);
        tree.un("append", this.updateSort, this);
        tree.un("insert", this.updateSort, this);
        tree.un("textchange", this.updateSortParent, this);
      },
      enableSort : function(tree) {
        tree.on("beforechildrenrendered", this.doSort, this);
        tree.on("append", this.updateSort, this);
        tree.on("insert", this.updateSort, this);
        tree.on("textchange", this.updateSortParent, this);

        this.doSort(tree.root);
      }
    });
 });