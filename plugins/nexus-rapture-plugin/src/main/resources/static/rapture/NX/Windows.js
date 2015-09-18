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
 * Helpers to open browser windows.
 *
 * @since 3.0
 */
Ext.define('NX.Windows', {
  singleton: true,
  requires: [
    'NX.Messages',
    'NX.I18n'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * Open a new browser window.
   *
   * @public
   * @return Browser window object or {@code null} if unable to open.
   */
  open: function(url, name, specs, replace) {
    var win;

    // apply default window specs if omitted, helps keep windows user-controllable on most browsers
    if (specs === undefined) {
      specs = 'menubar=yes,location=yes,resizable=yes,scrollbars=yes,status=yes'
    }

    //<if debug>
    this.logDebug('Opening window: url=' + url + ', name=' + name + ', specs=' + specs + ', replace=' + replace);
    //</if>

    win = NX.global.open(url, name, specs, replace);
    if (win === null) {
      NX.Messages.add({text: NX.I18n.get('Windows_Popup_Message'), type: 'danger'});
    }
    return win;
  }
});
