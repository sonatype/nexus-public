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
/*global Ext*/

/**
 * **{@link Ext.layout.container.Card}** override the setActiveItem method to allow for animation when switching items.
 * To accomplish this we needed to copy the full method from the ExtJS framework and then make changes.
 *
 * @since 3.14
 */
Ext.define('NX.ext.layout.container.Card', {
  override: 'Ext.layout.container.Card',

  /**
   * @override Ext.layout.container.Card#setActiveItem
   */
  setActiveItem: function (newCard) {
    if (!this.animate) {
      this.callParent(arguments);
    }

    var me = this,
        owner = me.owner,
        oldCard = me.activeItem,
        rendered = owner.rendered,
        newIndex, oldIndex, focusNewCard, region;
    newCard = me.parseActiveItem(newCard);
    newIndex = owner.items.indexOf(newCard);
    oldIndex = owner.items.indexOf(oldCard);
    // If the card is not a child of the owner, then add it.
    // Without doing a layout!
    if (newIndex === -1) {
      newIndex = owner.items.items.length;
      Ext.suspendLayouts();
      newCard = owner.add(newCard);
      Ext.resumeLayouts();
    }

    // Is this a valid, different card?
    if (newCard && oldCard !== newCard) {
      // Fire the beforeactivate and beforedeactivate events on the cards
      if (newCard.fireEvent('beforeactivate', newCard, oldCard) === false) {
        return false;
      }
      if (oldCard && oldCard.fireEvent('beforedeactivate', oldCard, newCard) === false) {
        return false;
      }
      if (rendered) {
        owner.findParentBy(function(parent) { return parent.getScrollable(); }).scrollTo(0, 0);

        Ext.suspendLayouts();

        region = owner.getRegion();

        // If the card has not been rendered yet, now is the time to do so.
        if (!newCard.rendered) {
          me.renderItem(newCard, me.getRenderTarget(), owner.items.length);
        }

        // Make sure the new card is shown
        if (newCard.hidden) {
          newCard.show();
        }

        if (oldCard && me.hideInactive) {
          focusNewCard = oldCard.el.contains(Ext.Element.getActiveElement());
          oldCard.animate({
            duration: NX.State.getValue('animateDuration', 200),
            from: {
              x: region.x,
              y: region.y,
              opacity: 1
            },
            to: {
              x: oldIndex < newIndex ? owner.el.getX() - owner.el.getWidth() : owner.el.getX() + owner.el.getWidth(),
              y: region.y,
              opacity: 0
            },
            callback: function() {
              if (oldCard === me.activeItem) {
                // just in case the user jumped back mid-animation
                return;
              }
              oldCard.el.hide();
              oldCard.hiddenByLayout = true;
              oldCard.fireEvent('deactivate');
            }
          });
        }

        var newCardAnimation = {
          duration: NX.State.getValue('animateDuration', 200),
          from: {
            x: oldIndex < newIndex ? region.x + region.width : region.x - region.width,
            y: region.y,
            opacity: 0
          },
          to: {
            x: region.x,
            y: region.y,
            opacity: 1
          },
          callback: function() {
            var parent = newCard.findParentBy(function(parent) { return parent.getScrollable(); });
            // Make sure the position is set correctly after animation, these styles are added by ExtJS during animation
            newCard.setStyle('top', '');
            newCard.setStyle('left', '');

            newCard.fireEvent('activate', newCard, oldCard);

            // Make sure the view remains scrolled to the top after activation
            parent.scrollTo(0, 0);

            // Force the x of the region to reset in case the animation gets off somewhere along the way.
            newCard.setX(parent.getX());
          }
        };
        newCard.animate(newCardAnimation);

        // Layout needs activeItem to be correct, so clear it if the show has been vetoed,
        // set it if the show has *not* been vetoed.
        if (newCard.hidden) {
          me.activeItem = newCard = null;
        } else {
          me.activeItem = newCard;
          // If the card being hidden contained focus, attempt to focus the new card
          // So as not to leave focus undefined.
          // The focus() call will focus the defaultFocus if it is a container
          // so ensure there is a defaultFocus.
          if (focusNewCard) {
            if (!newCard.defaultFocus) {
              newCard.defaultFocus = ':focusable';
            }
            newCard.focus();
          }
        }
        Ext.resumeLayouts(true);
      } else {
        me.activeItem = newCard;
      }
      return me.activeItem;
    }
    return false;
  }
});
