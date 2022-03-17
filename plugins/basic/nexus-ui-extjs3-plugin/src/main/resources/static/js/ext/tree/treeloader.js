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
define('ext/tree/treeloader',['extjs'], function(Ext){
Ext.tree.TreeLoader.override({
  requestData : function(node, callback) {
    if (this.fireEvent("beforeload", this, node, callback) !== false)
    {
      this.transId = Ext.Ajax.request({
        method : this.requestMethod,
        url : this.dataUrl || this.url,
        success : this.handleResponse,
        failure : this.handleFailure,
        timeout : this.timeout || 1000,
        scope : this,
        argument : {
          callback : callback,
          node : node
        },
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
  }
});
});
