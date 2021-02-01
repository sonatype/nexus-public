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
/*global NX, Ext, Nexus, Sonatype*/

/**
 * Support ZIP created window.
 *
 * @since 2.7
 */
NX.define('Nexus.atlas.view.SupportZipCreated', {
  extend: 'Nexus.wonderland.view.FileCreated',
  xtype: 'nx-atlas-view-supportzip-created',
  mixins: [
    'Nexus.LogAwareMixin'
  ],
  requires: [
    'Nexus.atlas.Icons'
  ],

  fileType: 'Support ZIP',
  downloadButtonId: 'nx-atlas-button-supportzip-download',

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        icons = Nexus.atlas.Icons;

    me.fileIcon = icons.get('zip').variant('x32');

    Nexus.atlas.view.SupportZipCreated.superclass.initComponent.apply(me, arguments);

    me.truncatedWarning = NX.create('Ext.Component', {
      cls: 'nx-atlas-view-supportzip-created-truncated-warning',
      html: '<span>' + icons.get('warning').img +
          'Contents have been truncated due to exceeded size limits.</span>',
      hidden: true
    });

    me.items.insert(1, me.truncatedWarning);
  },

  /**
   * Set form values.
   *
   * @public
   */
  setValues: function (values) {
    Nexus.atlas.view.SupportZipCreated.superclass.setValues.apply(this, arguments);

    // if truncated show the warning
    if (values.truncated) {
      this.truncatedWarning.show();
    }
  }

});