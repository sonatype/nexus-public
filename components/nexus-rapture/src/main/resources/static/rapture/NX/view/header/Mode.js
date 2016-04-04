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
     * Mode button tooltip.
     *
     * @cfg {String}
     */
    tooltip: undefined,

    /**
     * Mode button glyph.
     *
     * @cfg {String}
     */
    glyph: undefined,

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

  /**
   * Absolute layout for caret positioning over button.
   */
  layout: 'absolute',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.addEvents(
        /**
         * Fired when mode has been selected.
         *
         * @event selected
         * @param {NX.view.header.Mode} mode
         */
        'selected'
    );

    Ext.apply(me, {
      items: [
        {
          xtype: 'button',
          ui: 'nx-header',
          cls: 'nx-modebutton',
          scale: 'medium',
          height: 39,
          // min-width here as the user-mode extends past this with user-name
          minWidth: 39,
          toggleGroup: 'mode',
          allowDepress: false,
          tooltip: me.tooltip,
          glyph: me.glyph,
          handler: function(button) {
            me.fireEvent('selected', me);
          },
          // copied autoEl from Ext.button.Button
          autoEl: {
            tag: 'a',
            hidefocus: 'on',
            unselectable: 'on',
            // expose mode name on element for testability to target button by mode name
            'data-name': me.name
          }
        },
        {
          // css magic renders caret look
          xtype: 'container',
          cls: 'nx-caret',
          width: 0,
          height: 0,
          x: 14,
          y: 34
        }
      ]
    });

    me.callParent();
  },

  /**
   * @public
   * @param {String} text
   */
  setText: function(text) {
    this.down('button').setText(text);
  },

  /**
   * @public
   * @param {String} tip
   */
  setTooltip: function(tip) {
    this.down('button').setTooltip(tip);
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
