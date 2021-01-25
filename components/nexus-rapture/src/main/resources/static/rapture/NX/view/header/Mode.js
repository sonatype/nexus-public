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
 * Mode selector widget.
 *
 * @since 3.0
 */
Ext.define('NX.view.header.Mode', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-header-mode',

  config: {
    /**
     * Mode name.
     *
     * @cfg {String}
     */
    name: undefined,

    /**
     * Mode menu title.
     *
     * @cfg {String}
     */
    title: undefined,

    /**
     * Mode button text.
     *
     * @cfg {String}
     */
    text: undefined,

    /**
     * Mode button tooltip.
     *
     * @cfg {String}
     */
    tooltip: undefined,

    /**
     * Mode icon class
     *
     * @cfg {String}
     */
    iconCls: undefined,


    /**
     * If button should auto hide when no features are available for selected mode.
     *
     * @cfg {boolean}
     */
    autoHide: false,

    /**
     * If menu should be collapsed automatically when mode is selected.
     *
     * @cfg {boolean}
     */
    collapseMenu: false
  },

  publishes: {
    text: true,
    tooltip: true
  },

  /**
   * Absolute layout for caret positioning over button.
   */
  // layout: 'absolute',
  // TODO: Absolute layout breaks on the latest versions of ExtJS 6 so we'll need a different way to accomplish this

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.setViewModel({
      data: {
        text: me.getText(),
        tooltip: me.getTooltip()
      }
    });

    Ext.apply(me, {
      items: [
        {
          xtype: 'button',
          ui: 'nx-mode',
          cls: 'nx-modebutton',
          scale: 'medium',
          // min-width here as the user-mode extends past this with user-name
          minWidth: 49,
          toggleGroup: 'mode',
          allowDepress: false,
          handler: function(button) {
            me.fireEvent('selected', me);
          },
          iconCls: me.iconCls,
          // copied autoEl from Ext.button.Button
          autoEl: {
            tag: 'a',
            hidefocus: 'on',
            unselectable: 'on',
            // expose mode name on element for testability to target button by mode name
            'data-name': me.name
          },

          bind: {
            text: '{text:htmlEncode}',
            tooltip: '{tooltip:htmlEncode}'
          },

          ariaLabel: Ext.String.htmlEncode(me.text ? me.text : me.title)
        }
      ]
    });

    me.callParent();
  },

  /**
   * @public
   * @param {boolean} state
   * @param {boolean} suppressEvent
   */
  toggle: function(state, suppressEvent) {
    this.down('button').toggle(state, suppressEvent);
  }
});
