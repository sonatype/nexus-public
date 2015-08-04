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
define('Nexus/navigation/menu', ['extjs', 'sonatype', 'ext/menu'], function(Ext, Sonatype, Menu){

Ext.namespace('Sonatype.menu');

Sonatype.menu.Menu = Ext.extend(Menu, {
  add : function(c) {
    if (!c) {
      return null;
    }

    var i, item, param, arr = null, a = arguments;

    if (a.length > 1)
    {
      arr = a;
    }
    else if (Ext.isArray(c))
    {
      arr = c;
    }
    if (arr)
    {
      for (i = 0; i < arr.length; i=i+1)
      {
        this.add(arr[i]);
      }
      return;
    }

    item = Sonatype.menu.Menu.superclass.add.call(this, c);
    param = c.payload || this.payload;
    if (c.handler && param)
    {
      // create a delegate to pass the payload object to the handler
      item.setHandler(c.handler.createDelegate(c.scope ? c.scope : this.scope, [param], 0));
    }
    return item;
  }
});
});

