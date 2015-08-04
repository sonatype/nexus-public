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
define('ext/dd/droptarget', ['extjs'], function(Ext) {
  Ext.override(Ext.dd.DropTarget, {
    constructor : function(el, config) {
      this.el = Ext.get(el);

      Ext.apply(this, config);

      if (this.containerScroll) {
        Ext.dd.ScrollManager.register(this.el);
      }

      Ext.dd.DropTarget.superclass.constructor.call(this, this.el.dom, this.ddGroup || this.group,
        // Sonatype: allow config to pass thru to Ext.dd.DDTarget constructor so padding may
        // be passed from Ext.tree.TreePanel config when it need to have the whole drop panel
        // set as the drop zone because this is not done by the library
        config || {
          isTarget : true
        });
    }
  });
});
