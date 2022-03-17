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
/**
 * Support Request panel.
 *
 * @since 2.8.1
 */
NX.define('Nexus.atlas.view.SupportRequest', {
  extend: 'Ext.Panel',
  requires: [
    'Nexus.atlas.Icons'
  ],

  xtype: 'nx-atlas-view-supportrequest',
  title: 'Support Request',
  id: 'nx-atlas-view-supportrequest',
  cls: 'nx-atlas-view-supportrequest',

  border: false,
  layout: 'fit',

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
        icons = Nexus.atlas.Icons;

    Ext.apply(me, {
      items: [
        {
          xtype: 'container',
          items: [
            {
              cls: 'nx-atlas-view-supportrequest-description',
              border: false,
              html: icons.get('support').variant('x32').img +
                  '<div>' +
                  '<p>Submit a support request to Sonatype.</p>' +
                  '<br/>' +
                  '<p>Please include a complete description of your problem and steps to allow us to reproduce the problem (if available).</p>' +
                  '<br/>' +
                  '<p>Attaching a "Support ZIP" to your request will help our engineers give you a faster response.</p>' +
                  '</div>'
            },
            {
              xtype: 'form',
              itemId: 'form',
              cls: 'nx-atlas-view-supportrequest-form',
              border: false,
              buttons: [
                { text: 'Submit Request', id: 'nx-atlas-button-supportrequest-makerequest' }
              ],
              buttonAlign: 'left'
            }
          ]
        }
      ]
    });

    me.constructor.superclass.initComponent.apply(me, arguments);
  }
});