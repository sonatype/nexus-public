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
/*global Ext, NX*/

/**
 * Wizard grid screen.
 *
 * @since 3.0
 * @abstract
 */
Ext.define('NX.wizard.GridScreen', {
  extend: 'NX.wizard.Screen',
  alias: 'widget.nx-wizard-gridscreen',
  requires: [
    'NX.Assert'
  ],

  config: {
    /**
     * @cfg {Object} {@link Ext.grid.Panel} configuration object.
     */
    grid: undefined
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    //<if assert>
    NX.Assert.assert(me.grid, 'Missing required config: grid');
    //</if>

    Ext.applyIf(me.grid, {
      xtype: 'grid'
    });

    me.fields = me.fields || [];
    me.fields.push(me.grid);

    me.callParent(arguments);

    var view = me.getGrid().getView();

    // when grid loads sync
    view.on('refresh', function (view) {
      me.syncSizeToScreen(view);
    });

    // when screen is activated sync
    me.on('activate', function () {
      me.syncSizeToScreen(view);
    });

    // when wizard panel resizes sync
    me.on('added', function () {
      var panel = me.up('nx-wizard-panel');
      panel.on('resize', function () {
        me.syncSizeToScreen(view);
      });
    });
  },

  /**
   * @return {Ext.grid.Panel}
   */
  getGrid: function () {
    return this.down('grid');
  },

  /**
   * @private
   * @param {Ext.view.Table} view
   */
  syncSizeToScreen: function (view) {
    //console.log('syncing size');

    Ext.suspendLayouts();
    try {
      var table,
          contentHeight,
          visibleHeight,
          maxHeight;

      // ref: Ext.view.Table.hasVerticalScroll()
      table = view.getEl().down('table');
      if (!table) {
        return;
      }

      contentHeight = table.getHeight();
      visibleHeight = view.getHeight();
      maxHeight = this.calculateMaxHeight(view);

      if (contentHeight > maxHeight) {
        // content is larger than can display, needs to be set to max visible to scroll
        view.setHeight(maxHeight);
        view.setAutoScroll(true);
      }
      else if (visibleHeight < contentHeight) {
        // content fits into visible height, use content height
        view.setHeight(contentHeight);
        view.setAutoScroll(false);
      }
      else if (contentHeight < visibleHeight) {
        // content is smaller than visible, use content height
        view.setHeight(contentHeight);
        view.setAutoScroll(false);
      }

      //console.log('content,visible,max,current', contentHeight, visibleHeight, maxHeight, view.getHeight());
    }
    finally {
      Ext.resumeLayouts(true);
    }
  },

  /**
   * @private
   * @param {Ext.view.Table} view
   * @returns {number}
   */
  calculateMaxHeight: function (view) {
    var me = this,
        panel = me.up('nx-wizard-panel'),
        screenContainer = panel.getScreenContainer(),
        screenHeader = panel.getScreenHeader(),
        buttonsContainer = me.getButtonsContainer(),
        max;

    //function logheight(name, comp) {
    //  var el = comp.getEl();
    //  console.log(
    //      name,
    //      //me.heightOf(comp),
    //      'hf', el.getHeight(false),
    //      'ht', el.getHeight(true),
    //      'hf-ht', el.getHeight(false) - el.getHeight(true),
    //      'p', el.getPadding('tb'),
    //      'b', el.getBorderWidth('tb'),
    //      'm', el.getMargin('tb')
    //  );
    //}

    //logheight('panel', panel);
    //logheight('view', view);
    //logheight('screen-container', screenContainer);
    //logheight('screen-header', screenHeader);
    //logheight('screen-form', me.down('form'));
    //logheight('grid-headers', view.getHeaderCt());
    //logheight('buttons-container', buttonsContainer);

    function h(comp) {
      var el = comp.getEl(),
          hf = el.getHeight(false),
          hc = el.getHeight(true),
          m = el.getMargin('tb');
      return hf + (hf - hc) + m;
    }

    //Object.is replacement, as not supported in all browsers, derived from
    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/is
    function is(x, y) {
      if (x === y) {
        return x !== 0 || 1 / x === 1 / y;
      } else {
        return x !== x && y !== y;
      }
    };

    // calculate the height of all fields (except the grid)
    var grid = me.getGrid();
    var fieldsHeight = 0;
    me.down('#fields').items.each(function(item) {
      if (!is(item, grid)) {
        //logheight('field', item);

        fieldsHeight += h(item);
      }
    });

    // FIXME: where do these come from?
    var mysteryPixels = 6;
    var screenContainerEl = screenContainer.getEl();

    max = panel.getHeight(true) -
        h(screenHeader) -
        (screenContainerEl.getHeight(false) - screenContainerEl.getHeight(true)) -
        fieldsHeight -
        h(view.getHeaderCt()) -
        h(buttonsContainer) -
        mysteryPixels;

    //console.log('max:', max);
    return max;
  }
});
