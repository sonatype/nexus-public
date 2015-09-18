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
 * Helper to facilitate browser-based file downloads.
 *
 * @since 3.0
 */
Ext.define('NX.util.DownloadHelper', {
  singleton: true,
  requires: [
    'NX.Messages',
    'NX.Windows',
    'NX.I18n'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * ExtJS component identifier for nested iframe.
   *
   * @private
   */
  windowId: 'nx-download-frame',

  /**
   * Window names in IE are very picky, using '_' instead of '-' so that its its a valid javascript identifier.
   *
   * @private
   */
  windowName: 'nx_download_frame',

  /**
   * Get the hidden download frame.
   *
   * @private
   * @returns {Ext.Element}
   */
  getFrame: function() {
    var me = this, frame;

    // create the download frame if needed
    frame = Ext.get(me.windowId);
    if (!frame) {
      frame = Ext.getBody().createChild({
        tag: 'iframe',
        cls: 'x-hidden',
        id: me.windowId,
        name: me.windowName
      });

      //<if debug>
      me.logDebug('Created download-frame:', frame);
      //</if>
    }

    return frame;
  },

  /**
   * @public
   * @param {String} url URL to download
   */
  downloadUrl: function (url) {
    var me = this;

    //<if debug>
    me.logDebug('Downloading URL:', url);
    //</if>

    // resolve the download frame
    me.getFrame();

    // TODO: Consider changing this to a dynamic form or 'a' link and automatically submit/click
    // TODO: ... to make use of html5 download attribute and avoid needing to _open_ more windows
    // TODO: ... IE might not like this very much though?

    // TODO: Form method could be handy to GET/POST w/params vs link to just GET?

    // FIXME: This may produce js console warnings "Resource interpreted as Document but transferred with MIME type application/zip"

    // open new window in hidden download-from to initiate download
    if (NX.Windows.open(url, me.windowName) !== null) {
      NX.Messages.add({text: NX.I18n.get('Util_DownloadHelper_Download_Message'), type: 'success'});
    }
  }
});
