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
/*global Ext*/

/**
 * Content panel.
 *
 * @since 3.14
 */
Ext.define('NX.view.feature.BreadcrumbViewController', {
  extend: 'Ext.app.ViewController',
  alias: 'controller.breadcrumb',

  control: {
    '#': {
      afterlayout: 'handleClipping'
    }
  },

  handleClipping: function() {
    var breadcrumbs = this.getView(),
        constrainRegion = breadcrumbs.getConstrainRegion(),
        children = breadcrumbs.query('button:visible'),
        lastChild;

    if (children.length === 0) {
      return;
    }

    lastChild = children[children.length - 1];

    Ext.suspendLayouts();

    if (constrainRegion.right < lastChild.getRegion().right) {
      this.reduceButtonWidth(children, constrainRegion);
    }

    Ext.resumeLayouts(true);
  },

  /**
   * [icon] + [button] + [slash] + [icon] + [button] + ...
   *
   * We take the left edge of the each button, compute the space between it and the remaining buttons (the icons,
   * slashes, and spacing) then compute the average amount of space all remaining buttons would need to take up to fill
   * the space. If the current button's width is greater than that average then we set a max width on all remaining
   * buttons to that average.
   *
   * @param buttons - the list of buttons in the toolbar
   * @param constrainRegion - the region the buttons must fit within
   */

  reduceButtonWidth: function(buttons, constrainRegion) {
    var maxWidth;

    buttons.forEach(function(button, buttonIndex) {
      var reservedBetweenSpace = 0,
          nextButton = button,
          lastButton = button,
          spaceRemaining, averageBtnSize;

      if (maxWidth && button.getWidth() > maxWidth) {
        button.setMaxWidth(maxWidth);
        return;
      }

      while (nextButton = nextButton.next('button')) {
        reservedBetweenSpace += nextButton.getRegion().left - lastButton.getRegion().right;
        lastButton = nextButton;
      }
      spaceRemaining = constrainRegion.right - button.getRegion().left - reservedBetweenSpace;
      averageBtnSize = Math.floor(spaceRemaining / (buttons.length - buttonIndex));

      if (button.getWidth() > averageBtnSize) {
        maxWidth = averageBtnSize;
        button.setMaxWidth(maxWidth);
      }
    });
  }

});
