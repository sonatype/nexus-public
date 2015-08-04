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
define('ext/element', ['extjs'], function(Ext) {
  Ext.override(Ext.Element, {
    setHeightOrig : Ext.Element.prototype.setHeight,
    setHeight : function(height)
    {
      // height is NaN for IE8 because syncHeight() will call this with
      // Math.max(0, "100%" - 27) == NaN
      if ( Ext.isIE8 && typeof height === 'number' && isNaN(height) ) {
        height = 0;
      }

      return this.setHeightOrig.call(this, height);
    }
  });
});