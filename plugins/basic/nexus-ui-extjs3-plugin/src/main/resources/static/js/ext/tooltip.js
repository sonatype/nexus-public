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
define('ext/tooltip',['extjs'], function(Ext){
// some special tooltip config to reuse same tooltip for whole grid
Ext.override(Ext.ToolTip, {
  onTargetOver : function(e) {
    if (this.disabled || e.within(this.target.dom, true))
    {
      return;
    }
    var t = e.getTarget(this.delegate);
    if (t)
    {
      this.triggerElement = t;
      this.clearTimer('hide');
      this.targetXY = e.getXY();
      this.delayShow();
    }
  },
  onMouseMove : function(e) {
    var t = e.getTarget(this.delegate);
    if (t)
    {
      this.targetXY = e.getXY();
      if (t === this.triggerElement)
      {
        if (!this.hidden && this.trackMouse)
        {
          this.setPagePosition(this.getTargetXY());
        }
      }
      else
      {
        this.hide();
        this.lastActive = new Date(0);
        this.onTargetOver(e);
      }
    }
    else if (!this.closable && this.isVisible())
    {
      this.hide();
    }
  },
  hide : function() {
    this.clearTimer('dismiss');
    this.lastActive = new Date();
    delete this.triggerElement;
    Ext.ToolTip.superclass.hide.call(this);
  }
});
});
