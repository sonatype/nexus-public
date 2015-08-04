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
define('ext/dd/dropzone',['extjs'], function(Ext) {
Ext.dd.DropZone = function(el, config) {
  Ext.dd.DropZone.superclass.constructor.call(this, el, config);
};

Ext.extend(Ext.dd.DropZone, Ext.dd.DropTarget, {
  getTargetFromEvent : function(e) {
    return Ext.dd.Registry.getTargetFromEvent(e);
  },
  onNodeEnter : function(n, dd, e, data) {},
  onNodeOver : function(n, dd, e, data) {
    return this.dropAllowed;
  },
  onNodeOut : function(n, dd, e, data) {},
  onNodeDrop : function(n, dd, e, data) {
    return false;
  },
  onContainerOver : function(dd, e, data) {
    return this.dropNotAllowed;
  },
  onContainerDrop : function(dd, e, data) {
    return false;
  },
  notifyEnter : function(dd, e, data) {
    return this.dropNotAllowed;
  },
  notifyOver : function(dd, e, data) {
    var n = this.getTargetFromEvent(e);
    if (!n)
    {
      if (this.lastOverNode)
      {
        this.onNodeOut(this.lastOverNode, dd, e, data);
        this.lastOverNode = null;
      }
      return this.onContainerOver(dd, e, data);
    }
    if (this.lastOverNode !== n)
    {
      if (this.lastOverNode)
      {
        this.onNodeOut(this.lastOverNode, dd, e, data);
      }
      this.onNodeEnter(n, dd, e, data);
      this.lastOverNode = n;
    }
    return this.onNodeOver(n, dd, e, data);
  },
  notifyOut : function(dd, e, data) {
    if (this.lastOverNode)
    {
      this.onNodeOut(this.lastOverNode, dd, e, data);
      this.lastOverNode = null;
    }
  },
  notifyDrop : function(dd, e, data) {
    if (this.lastOverNode)
    {
      this.onNodeOut(this.lastOverNode, dd, e, data);
      this.lastOverNode = null;
    }
    var n = this.getTargetFromEvent(e);
    return n ? this.onNodeDrop(n, dd, e, data) : this.onContainerDrop(dd, e, data);
  },
  triggerCacheRefresh : function() {
    Ext.dd.DDM.refreshCache(this.groups);
  }
});
});
