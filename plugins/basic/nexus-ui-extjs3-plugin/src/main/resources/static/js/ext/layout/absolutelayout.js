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

define('ext/layout/absolutelayout', ['extjs'], function(Ext) {
  /**
   * Override the absolute layout to optionally omit the defined extraCls (x-abs-layout-item).
   * The class forces the 'left' CSS position property, which will disable positioning items
   * from the right side.
   */
  Ext.layout.AbsoluteLayout.override({
    renderItem : function(c, position, target) {
      Ext.layout.AbsoluteLayout.superclass.renderItem.call(this, c, position, target);
      if (c.noExtraClass) {
        c.removeClass(this.extraCls);
      }
    }
  });
});

