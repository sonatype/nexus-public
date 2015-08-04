/*
 * <sonatypeNote> this javascript was found here
 * http://extjs.com/forum/showthread.php?t=28115 </sonatypeNote>
 * MultiSelectTreePanel v 1.1 This work is derivative of Ext-JS 2.2. Much of the
 * code is modified versions of default code. Refer to Ext-JS 2.2 licencing for
 * more information. http://extjs.com/license Any and all original code is made
 * available as is for whatever purpose you see fit. Should be a largely drop in
 * replacement for ordinary TreePanel when you require multiselect with drag and
 * drop. Overrides most of the methods and events to pass a nodelist rather than
 * a single node. Note that the code is provided as-is and should be considered
 * experimental and likely to contain bugs, especially when combined with other
 * extensions or modifications to the default library. It has been tested
 * against Ext-JS 2.2 and 2.2.1 with: Firefox 3, Opera 9.5+, Safari 3.1, MSIE
 * 6,7,8rc1 (+5.5 seems to work too) Usage: Add the following CSS to make the
 * floating "drag" version of the tree indent prettily.. .x-dd-drag-ghost
 * .x-tree-node-indent,.x-dd-drag-ghost .x-tree-ec-icon {display: inline
 * !important;} If you are using Ext-JS 2.2.1 or earlier you need to add this
 * override! (reported as a bug) Ext.override(Ext.tree.TreeDropZone, {
 * completeDrop : function(de){ var ns = de.dropNode, p = de.point, t =
 * de.target; if(!Ext.isArray(ns)){ ns = [ns]; } var n, node, ins = false; if (p !=
 * 'append'){ ins = true; node = (p == 'above') ? t : t.nextSibling; } for(var i =
 * 0, len = ns.length; i < len; i++){ n = ns[i]; if (ins){
 * t.parentNode.insertBefore(n, node); }else{ t.appendChild(n); }
 * if(Ext.enableFx && this.tree.hlDrop){ n.ui.highlight(); } } ns[0].ui.focus();
 * t.ui.endDrop(); this.tree.fireEvent("nodedrop", de); } }); Instantiate like a
 * normal tree (except DD stuff is enabled by default) var tree = new
 * Ext.ux.MultiSelectTreePanel({ autoScroll:true, width:400, height:500,
 * animate:true, containerScroll: true, enableDD: true, root: new
 * Ext.tree.AsyncTreeNode({ text: 'A Book', draggable:false, id:'node0' }),
 * loader: new Ext.tree.TreeLoader({ dataUrl:'bookdata.json' }) });
 * tree.render("target"); When listening for DND events look for dragdata.nodes
 * instead of dragdata.node Use ctrl-click to select multiple nodes. Use
 * shift-click to select a range of nodes. Changelog v1.0 Initial Release v1.1 -
 * reinstated enableDD, enableDrag, enableDrop config params. *NEED TO INCLUDE
 * THIS NOW* - consolidated compareDocumentPosition code into compareNodeOrder
 * (only works with rendered nodes) - cleaned up select function by above and
 * creating selectNode function. - cleaned up DDGhost generation code to be less
 * hacky (still not ideal) - included onContainerOver and onContainerDrop code
 * (awaiting ExtJS fix) - fixed several lingering postdrag selection bugs -
 * fixed key events to respect shift/ctrl keys Enjoy
 */

define('ext/ux/multiselecttree',['extjs'], function(Ext){

Ext.ux.FixedMultiSelectionModel = Ext.extend(Ext.tree.MultiSelectionModel, {
      // disabled tracking of mouse clicks because it doubles up drag
      // selection...
      onNodeClick : function(node, e) {
        if (e.shiftKey)
          e.preventDefault();
        // this.select(node);
      },

      // private
      // for comparing node order... (taken from quirksmode.org and googlecode)
      compareNodeOrder : document.compareDocumentPosition ? function(node1, node2) {
        // W3C DOM lvl 3 method (Gecko)
        return 3 - (node1.ui.elNode.compareDocumentPosition(node2.ui.elNode) & 6);
      } : (typeof document.documentElement.sourceIndex !== "undefined" ? function(node1, node2) {
        // IE source index method
        return node1.ui.elNode.sourceIndex - node2.ui.elNode.sourceIndex;
      } : function(node1, node2) {
        if (node1 == node2)
          return 0;
        // Safari doesn't support compareDocumentPosition or sourceIndex
        // from
        // http://code.google.com/p/doctype/wiki/ArticleNodeCompareDocumentOrder
        var range1 = document.createRange();
        range1.selectNode(a.ui.elNode);
        range1.collapse(true);

        var range2 = document.createRange();
        range2.selectNode(b.ui.elNode);
        range2.collapse(true);

        return range1.compareBoundaryPoints(Range.START_TO_END, range2);
      }),

      // private
      sortSelNodes : function() {
        if (this.selNodes.length > 1)
        {
          if (!this.selNodes[0].ui.elNode)
            return;
          this.selNodes.sort(this.compareNodeOrder);
        }
      },

      // private single point for selectNode
      selectNode : function(node, push) {
        if (!this.isSelected(node))
        {
          this.selNodes.push(node);
          this.selMap[node.id] = node;
          node.ui.onSelectedChange(true);
        }
      },

      // overwritten from MultiSelectionModel to fix unselecting...
      select : function(node, e, keepExisting) {
        // Add in setting an array as selected... (for multi-selecting D&D
        // nodes)
        if (node instanceof Array)
        {
          for (var c = 0; c < node.length; c++)
          {
            this.selectNode(node[c]);
          }
          this.sortSelNodes();
          this.fireEvent("selectionchange", this, this.selNodes, this.lastSelNode);
          return node;
        }
        // Shift Select to select a range
        // NOTE: Doesn't change lastSelNode
        // EEK has to be a prettier way to do this
        if (e && e.shiftKey && this.selNodes.length > 0)
        {
          this.lastSelNode = this.lastSelNode || this.selNodes[0];
          var before = this.compareNodeOrder(this.lastSelNode, node) > 0;
          // if (this.lastSelNode == node) {
          // check dom node ordering (from ppk of quirksmode.org)
          this.clearSelections(true);
          var cont = true;
          var inside = false;
          var parent = this.lastSelNode;
          // ummm... yeah don't read this bit...
          do
          {
            for (var next = parent; next != null; next = (before ? next.previousSibling : next.nextSibling))
            {
              // hack to make cascade work the way I want it to
              inside = inside || (before && (next == node || next.contains(node)));
              if (next.isExpanded())
              {
                next.cascade(function(n) {
                      if (cont != inside)
                      {
                        this.selectNode(n);
                      }
                      cont = (cont && n != node);
                      return true;
                    }, this);
              }
              else
              {
                this.selectNode(next);
                cont = (next != node);
              }
              if (!cont)
                break;
            }
            if (!cont)
              break;
            while ((parent = parent.parentNode) != null)
            {
              if (before)
              {
                this.selectNode(parent);
              }
              cont = (cont && parent != node);
              if (before && parent.previousSibling)
              {
                parent = parent.previousSibling;
                break;
              }
              if (!before && parent.nextSibling)
              {
                parent = parent.nextSibling;
                break;
              }
            }
            if (!cont)
              break;
          } while (parent != null);
          this.selectNode(node);
          // sort the list
          this.sortSelNodes();
          this.fireEvent("selectionchange", this, this.selNodes, node);
          e.preventDefault();
          return node;
        }
        else if (keepExisting !== true)
        {
          this.clearSelections(true);
        }
        if (this.isSelected(node))
        {
          // handle deselect of node...
          if (keepExisting === true)
          {
            this.unselect(node);
            if (this.lastSelNode === node)
            {
              this.lastSelNode = this.selNodes[0];
            }
            return node;
          }
          this.lastSelNode = node;
          return node;
        }
        // save a resort later on...
        this.selectNode(node);
        this.sortSelNodes();
        this.lastSelNode = node;
        this.fireEvent("selectionchange", this, this.selNodes, this.lastSelNode);
        return node;
      },
      // returns selected nodes precluding children of other selected nodes...
      // used for multi drag and drop...
      getUniqueSelectedNodes : function() {
        var ret = [];
        for (var c = 0; c < this.selNodes.length; c++)
        {
          var parent = this.selNodes[c];
          ret.push(parent);
          // nodes are sorted(?) so skip over subsequent nodes inside this one..
          while ((c + 1) < this.selNodes.length && parent.contains(this.selNodes[c + 1]))
            c++;
        }
        return ret;
      },

      // check for descendents when nodes are removed...
      unselect : function(node, subnodes) {
        if (subnodes)
        {
          for (var c = this.selNodes.length - 1; c >= 0; c--)
          {
            if (this.selNodes[c].isAncestor(node))
            {
              Ext.ux.FixedMultiSelectionModel.superclass.unselect.call(this, this.selNodes[c]);
            }
          }
        }
        return Ext.ux.FixedMultiSelectionModel.superclass.unselect.call(this, node);
      },

      /**
       * Selects the node above the selected node in the tree, intelligently
       * walking the nodes
       * 
       * @return TreeNode The new selection
       */
      selectPrevious : function(keepExisting) {
        var s = this.selNodes[0];
        if (!s)
        {
          return null;
        }
        var ps = s.previousSibling;
        if (ps)
        {
          if (!ps.isExpanded() || ps.childNodes.length < 1)
          {
            return this.select(ps, null, keepExisting);
          }
          else
          {
            var lc = ps.lastChild;
            while (lc && lc.isExpanded() && lc.childNodes.length > 0)
            {
              lc = lc.lastChild;
            }
            return this.select(lc, null, keepExisting);
          }
        }
        else if (s.parentNode && (this.tree.rootVisible || !s.parentNode.isRoot))
        {
          return this.select(s.parentNode, null, keepExisting);
        }
        return null;
      },

      /**
       * Selects the node above the selected node in the tree, intelligently
       * walking the nodes
       * 
       * @return TreeNode The new selection
       */
      selectNext : function(keepExisting) {
        var s = this.selNodes[this.selNodes.length - 1];
        if (!s)
        {
          return null;
        }
        if (s.firstChild && s.isExpanded())
        {
          return this.select(s.firstChild, null, keepExisting);
        }
        else if (s.nextSibling)
        {
          return this.select(s.nextSibling, null, keepExisting);
        }
        else if (s.parentNode)
        {
          var newS = null;
          s.parentNode.bubble(function() {
                if (this.nextSibling)
                {
                  newS = this.getOwnerTree().selModel.select(this.nextSibling, null, keepExisting);
                  return false;
                }
              });
          return newS;
        }
        return null;
      },

      onKeyDown : function(e) {
        var s = this.selNode || this.lastSelNode;
        // undesirable, but required
        var sm = this;
        if (!s)
        {
          return;
        }
        var k = e.getKey();
        switch (k)
        {
          case e.DOWN :
            e.stopEvent();
            this.selectNext(e.shiftKey || e.ctrlKey);
            break;
          case e.UP :
            e.stopEvent();
            this.selectPrevious(e.shiftKey || e.ctrlKey);
            break;
          case e.RIGHT :
            e.preventDefault();
            if (s.hasChildNodes())
            {
              if (!s.isExpanded())
              {
                s.expand();
              }
              else if (s.firstChild)
              {
                this.select(s.firstChild, e, e.shiftKey || e.ctrlKey);
              }
            }
            break;
          case e.LEFT :
            e.preventDefault();
            if (s.hasChildNodes() && s.isExpanded())
            {
              s.collapse();
            }
            else if (s.parentNode && (this.tree.rootVisible || s.parentNode != this.tree.getRootNode()))
            {
              this.select(s.parentNode, e, e.shiftKey || e.ctrlKey);
            }
            break;
        }
      }

    });
/*
 * Enhanced to support dragging multiple nodes... for extension refer to
 * data.nodes instead of data.node
 */
Ext.ux.MultiSelectTreeDragZone = Ext.extend(Ext.tree.TreeDragZone, {
      onBeforeDrag : function(data, e) {
        if (data.nodes && data.nodes.length > 0)
        {
          for (var c = 0; c < data.nodes.length; c++)
          {
            n = data.nodes[c];
            if (n.draggable === false || n.disabled)
              return false
          }
          return true;
        }
        else if (data.node)
        {
          if (data.node.draggable === false || data.node.disabled)
            return false
        }
        return false;

      },
      // v1.0
      // fixed to handle multiSelectionModel
      // Data now calls SelectionModel.select instead of waiting for the click
      // event
      // Creates Ghost inline rather than calling TreeNodeUI.
      //
      // v1.1
      // cleanup to have ghost generation slightly less hacky... still hacky
      // though...
      // fixes problems with using extra tag nesting in a custom TreeNodeUI.
      getDragData : function(e) {
        // use tree selection model..
        var selModel = this.tree.getSelectionModel();
        // get event target
        var target = Ext.dd.Registry.getHandleFromEvent(e);
        // if no target (die)
        if (target == null)
          return;
        if (target.node.isSelected() && e.ctrlKey)
        {
          selModel.unselect(target.node);
          return;
        }
        var selNodes = [];
        if (!selModel.getSelectedNodes)
        {
          // if not multiSelectionModel.. just use the target...
          selNodes = [target.node];
        }
        else
        {
          // if target not selected select it...
          if (!target.node.isSelected() || e.shiftKey)
          {
            selModel.select(target.node, e, e.ctrlKey);
          }
          // get selected nodes - nested nodes...
          selNodes = selModel.getUniqueSelectedNodes();
        }
        // if no nodes selected stop now...
        if (!selNodes || selNodes.length < 1)
          return;
        var dragData = {
          nodes : selNodes
        };
        // create a container for the proxy...
        var div = document.createElement('ul'); // create the multi element drag
                                                // "ghost"
        // add classes to keep is pretty...
        div.className = 'x-tree-node-ct x-tree-lines';
        // add actual dom nodes to div (instead of tree nodes)
        var height = 0;
        for (var i = 0, len = selNodes.length; i < len; i++)
        {
          // add entire node to proxy
          // normally this is done by TreeNodeUI.appendDDGhost(), but overriding
          // that class requires
          // also overriding TreeLoader etc. Ext.extend() is an option though...
          var clonenode = selNodes[i].ui.wrap.cloneNode(true);
          // fix extra indenting by removing extra spacers
          // should really modify UI rendering code to render a duplicate
          // subtree but this is simpler...
          // count current indent nodes from ui indentNode... (add 1 for elbow)
          var subtract = selNodes[i].ui.indentNode.childNodes.length + 1;
          // avoid indent alterations if possible..
          if (subtract > 0)
          {
            // relies on node ui using the same tag for all elems...
            var subNodes = Ext.query(selNodes[i].ui.indentNode.nodeName + ".x-tree-node-indent", clonenode);
            for (var c = 0, clen = subNodes.length; c < clen; c++)
            {
              var inode = subNodes[c];
              var current = inode.childNodes.length;
              if (current <= subtract)
              {
                inode.innerHTML = "";
                // remove elbow icon as well..
                if (current < subtract)
                  inode.parentNode.removeChild(subNodes[c].nextSibling);
              }
              else
              {
                for (var r = 0; r < subtract; r++)
                {
                  subNodes[c].removeChild(subNodes[c].firstChild);
                }
              }
            }
          }
          div.appendChild(clonenode);
          Ext.fly(clonenode).removeClass(['x-tree-selected', 'x-tree-node-over']);
        }
        dragData.ddel = div;
        return dragData;
      },
      // fix from TreeDragZone (references dragData.node instead of
      // dragData.nodes)
      onInitDrag : function(e) {
        var data = this.dragData;
        this.tree.eventModel.disable();
        this.proxy.update("");
        this.proxy.ghost.dom.appendChild(data.ddel);
        this.tree.fireEvent("startdrag", this.tree, data.nodes, e);
      },
      // Called from TreeDropZone (looks like hack for handling multiple tree
      // nodes)
      getTreeNode : function() {
        return this.dragData.nodes;
      },
      // fix from TreeDragZone (refers to data.node instead of data.nodes)
      // Don't know what this does, so leaving as first node.
      getRepairXY : function(e, data) {
        return data.nodes[0].ui.getDDRepairXY();
      },

      // fix from TreeDragZone (refers to data.node instead of data.nodes)
      onEndDrag : function(data, e) {
        this.tree.eventModel.enable.defer(100, this.tree.eventModel);
        this.tree.fireEvent("enddrag", this.tree, data.nodes || [data.node], e);
      },

      // fix from TreeDragZone (refers to dragData.node instead of
      // dragData.nodes)
      onValidDrop : function(dd, e, id) {
        this.tree.fireEvent("dragdrop", this.tree, this.dragData.nodes, dd, e);
        this.hideProxy();
      },

      // fix for invalid Drop
      beforeInvalidDrop : function(e, id) {
        // this scrolls the original position back into view
        var sm = this.tree.getSelectionModel();
        // sm.clearSelections();
        // sm.select(this.dragData.nodes, e, true);
      }

    });

/*
 * MultiSelectTreeDropZone Contains following fixups - modified functions to
 * handle multiple nodes in dd operation isValidDropPoint afterRepair - modified
 * getDropPoint such that isValidDropPoint can simulate leaf style below
 * inserting. Overriding isValidDropPoint affects getDropPoint affects
 * onNodeOver and onNodeDrop Refer to data.nodes instead of data.node for
 * events..
 */
Ext.ux.MultiSelectTreeDropZone = Ext.extend(Ext.tree.TreeDropZone, {

      // fix from TreeDropZone (referred to data.node instead of data.nodes)
      isValidDropPoint : function(n, pt, dd, e, data) {
        if (!n || !data)
        {
          return false;
        }
        var targetNode = n.node;
        var dropNodes = data.nodes ? data.nodes : [data.node];
        // default drop rules
        if (!(targetNode && targetNode.isTarget && pt))
        {
          return false;
        }
        if (pt == "append" && targetNode.allowChildren === false)
        {
          return false;
        }
        if ((pt == "above" || pt == "below") && (targetNode.parentNode && targetNode.parentNode.allowChildren === false))
        {
          return false;
        }
        // don't allow dropping a treenode inside itself...
        for (var c = 0; c < dropNodes.length; c++)
        {
          if (dropNodes[c] && (targetNode == dropNodes[c] || dropNodes[c].contains(targetNode)))
          {
            return false;
          }
        }
        // reuse the object
        var overEvent = this.dragOverData;
        overEvent.tree = this.tree;
        overEvent.target = targetNode;
        overEvent.data = data;
        overEvent.point = pt;
        overEvent.source = dd;
        overEvent.rawEvent = e;
        overEvent.dropNode = dropNodes;
        overEvent.cancel = false;
        var result = this.tree.fireEvent("nodedragover", overEvent);
        return overEvent.cancel === false && result !== false;
      },

      // override to allow insert "below" when leaf != true...
      getDropPoint : function(e, n, dd, data) {
        var tn = n.node;
        if (tn.isRoot)
        {
          return this.isValidDropPoint(n, "append", dd, e, data) ? "append" : false;
        }
        var dragEl = n.ddel;
        var t = Ext.lib.Dom.getY(dragEl), b = t + dragEl.offsetHeight;
        var y = Ext.lib.Event.getPageY(e);
        var noAppend = tn.allowChildren === false || tn.isLeaf() || !this.isValidDropPoint(n, "append", dd, e, data);
        if (!this.appendOnly && tn.parentNode.allowChildren !== false)
        {
          var noBelow = false;
          if (!this.allowParentInsert)
          {
            noBelow = tn.hasChildNodes() && tn.isExpanded();
          }
          var q = (b - t) / (noAppend ? 2 : 3);
          if (y >= t && y < (t + q) && this.isValidDropPoint(n, "above", dd, e, data))
          {
            return "above";
          }
          else if (!noBelow && (noAppend || y >= b - q && y <= b) && this.isValidDropPoint(n, "below", dd, e, data))
          {
            return "below";
          }
        }
        return noAppend ? false : "append";
      },

      // Override because it calls getDropPoint and isValidDropPoint
      onNodeOver : function(n, dd, e, data) {
        var pt = this.getDropPoint(e, n, dd, data);
        var node = n.node;

        if (!this.expandProcId && pt == "append" && node.hasChildNodes() && !n.node.isExpanded())
        {
          this.queueExpand(node);
        }
        else if (pt != "append")
        {
          this.cancelExpand();
        }

        var returnCls = this.dropNotAllowed;
        if (pt)
        {
          var el = n.ddel;
          var cls;
          if (pt == "above")
          {
            returnCls = n.node.isFirst() ? "x-tree-drop-ok-above" : "x-tree-drop-ok-between";
            cls = "x-tree-drag-insert-above";
          }
          else if (pt == "below")
          {
            returnCls = n.node.isLast() ? "x-tree-drop-ok-below" : "x-tree-drop-ok-between";
            cls = "x-tree-drag-insert-below";
          }
          else
          {
            returnCls = "x-tree-drop-ok-append";
            cls = "x-tree-drag-append";
          }
          if (this.lastInsertClass != cls)
          {
            Ext.fly(el).replaceClass(this.lastInsertClass, cls);
            this.lastInsertClass = cls;
          }
        }
        return returnCls;
      },

      // Override because it calls getDropPoint and isValidDropPoint
      onNodeDrop : function(n, dd, e, data) {
        var point = this.getDropPoint(e, n, dd, data);
        var targetNode = n.node;
        targetNode.ui.startDrop();
        if (point === false)
        {
          targetNode.ui.endDrop();
          return false;
        }

        var dropNode = data.node || (dd.getTreeNode ? dd.getTreeNode(data, targetNode, point, e) : null);
        var dropEvent = {
          tree : this.tree,
          target : targetNode,
          data : data,
          point : point,
          source : dd,
          rawEvent : e,
          dropNode : dropNode,
          cancel : !dropNode,
          dropStatus : false
        };
        var retval = this.tree.fireEvent("beforenodedrop", dropEvent);
        if (retval === false || dropEvent.cancel === true || !dropEvent.dropNode)
        {
          targetNode.ui.endDrop();
          return dropEvent.dropStatus;
        }

        targetNode = dropEvent.target;
        if (point == "append" && !targetNode.isExpanded())
        {
          targetNode.expand(false, null, function() {
                this.completeDrop(dropEvent);
              }.createDelegate(this));
        }
        else
        {
          this.completeDrop(dropEvent);
        }
        return true;
      },

      // fix from TreeDropZone (referred to data.node instead of data.nodes)
      afterRepair : function(data) {
        if (data && Ext.enableFx)
        {
          var nl = data.nodes ? data.nodes : [data.node];
          for (var c = 0, len = nl.length; c < len; c++)
          {
            nl[c].ui.highlight();
          }
        }
        this.hideProxy();
      },

      // handle allowContainerDrop (appends nodes to the root node)
      onContainerDrop : function(dd, e, data) {
        if (this.allowContainerDrop && this.isValidDropPoint({
              ddel : this.tree.getRootNode().ui.elNode,
              node : this.tree.getRootNode()
            }, "append", dd, e, data))
        {
          var targetNode = this.tree.getRootNode();
          targetNode.ui.startDrop();
          var dropNode = data.node || (dd.getTreeNode ? dd.getTreeNode(data, targetNode, "append", e) : null);
          var dropEvent = {
            tree : this.tree,
            target : targetNode,
            data : data,
            point : "append",
            source : dd,
            rawEvent : e,
            dropNode : dropNode,
            cancel : !dropNode,
            dropStatus : false
          };
          var retval = this.tree.fireEvent("beforenodedrop", dropEvent);
          if (retval === false || dropEvent.cancel === true || !dropEvent.dropNode)
          {
            targetNode.ui.endDrop();
            return dropEvent.dropStatus;
          }

          targetNode = dropEvent.target;
          if (!targetNode.isExpanded())
          {
            targetNode.expand(false, null, function() {
                  this.completeDrop(dropEvent);
                }.createDelegate(this));
          }
          else
          {
            this.completeDrop(dropEvent);
          }
          return true;
        }
        return false;
      },

      // handle allowContaineDrop (treat as a drop to the root node)
      onContainerOver : function(dd, e, data) {
        if (this.allowContainerDrop && this.isValidDropPoint({
              ddel : this.tree.getRootNode().ui.elNode,
              node : this.tree.getRootNode()
            }, "append", dd, e, data))
        {
          return this.dropAllowed;
        }
        return this.dropNotAllowed;
      }

    });

/*
 * MultiSelectTreePanel sets up using FixedMultiSelectionModel and initing with
 * extended DragZone and DropZone by default
 */

Ext.ux.MultiSelectTreePanel = Ext.extend(Ext.tree.TreePanel, {

      getSelectionModel : function() {
        if (!this.selModel)
        {
          this.selModel = new Ext.ux.FixedMultiSelectionModel();
        }
        return this.selModel;
      },

      initEvents : function() {
        if ((this.enableDD || this.enableDrop) && !this.dropZone)
        {
          this.dropZone = new Ext.ux.MultiSelectTreeDropZone(this, this.dropConfig || {
                ddGroup : this.ddGroup || "TreeDD",
                appendOnly : this.ddAppendOnly === true
              });
        }
        if ((this.enableDD || this.enableDrag) && !this.dragZone)
        {
          this.dragZone = new Ext.ux.MultiSelectTreeDragZone(this, {
                ddGroup : this.ddGroup || "TreeDD",
                scroll : this.ddScroll
              });
        }
        Ext.ux.MultiSelectTreePanel.superclass.initEvents.apply(this, arguments);

        // This is temporary. Should really Ext.extend on TreeNode.removeChild()
        // and call getOwnerTree().removeNode(node) or similar...

        this.on("remove", function(tree, parent, node) {
              tree.getSelectionModel().unselect(node, true);
            });
      }
    });

Ext.reg('multiselecttreepanel', Ext.ux.MultiSelectTreePanel);
});
