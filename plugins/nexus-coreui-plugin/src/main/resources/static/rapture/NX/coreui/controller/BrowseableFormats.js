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
/*global Ext, NX*/

/**
 * BrowseableFormats management controller.
 *
 * @since 3.2.1
 */
Ext.define('NX.coreui.controller.BrowseableFormats', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.State',
    'NX.coreui.util.BrowseableFormats'
  ],

  stores: [
    'BrowseableFormat'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.primeInitialFormats();

    me.listen({
      controller: {
        '#State': {
          userchanged: me.fetchFormats,
          browseableformatschanged: me.setFormats
        }
      },
      store: {
        '#BrowseableFormat': {
          load: me.fireFormatsChanged,
          update: me.onUpdate,
          remove: me.fireFormatsChanged
        }
      }
    });
  },

  /**
   * @private
   */
  primeInitialFormats: function () {
    var me = this,
        rawData = NX.State.getValue('browseableformats');

    //<if debug>
    me.logTrace('Initial visible formats:', rawData);
    //</if>

    me.getStore('BrowseableFormat').loadRawData(rawData, false);
    NX.coreui.util.BrowseableFormats.setFormats(me.getFormats());

    //<if debug>
    me.logInfo('VisiblePermissions primed');
    //</if>
  },

  /**
   * @private
   */
  onUpdate: function (store, record, operation) {
    if (operation === Ext.data.Model.COMMIT) {
      this.fireFormatsChanged();
    }
  },

  /**
   * @private
   */
  fetchFormats: function () {
    var me = this;

    NX.coreui.util.BrowseableFormats.resetFormats();
    //<if debug>
    me.logDebug('Fetching formats...');
    //</if>
    me.getStore('BrowseableFormat').load();
    NX.coreui.util.BrowseableFormats.setFormats(me.getFormats());
  },

  /**
   * @private
   */
  setFormats: function (formats) {
    var me = this;

    //<if debug>
    me.logDebug('Loading visible formats...');
    //</if>

    me.getStore('BrowseableFormat').loadRawData(formats, false);
    me.fireFormatsChanged();
  },

  /**
   * @private
   */
  fireFormatsChanged: function () {
    var me = this;

    NX.coreui.util.BrowseableFormats.setFormats(me.getFormats());

    //<if debug>
    me.logDebug('BrowseableFormats changed; Firing event');
    //</if>

    me.fireEvent('changed', NX.coreui.util.BrowseableFormats);
  },

  /**
   * @private
   * @return {Object} formats
   */
  getFormats: function () {
    var store = this.getStore('BrowseableFormat'),
        formats = [];

    store.each(function (rec) {
      formats.push(rec.get('id'));
    });

    return formats;
  }

});
