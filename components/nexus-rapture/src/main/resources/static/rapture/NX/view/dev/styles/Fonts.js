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
 * Font styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Fonts', {
  extend: 'NX.view.dev.styles.StyleSection',
  requires: [
    'Ext.XTemplate'
  ],

  title: 'Fonts',
  layout: {
    type: 'vbox',
    defaultMargins: {top: 4, right: 0, bottom: 0, left: 0}
  },

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    var faceExampleTpl = Ext.create('Ext.XTemplate',
        '<div>',
        '<span class="nx-section-header">{text}</span>',
        '<p class="{clz}">',
        'Trusted applications at the speed of deployment<br/>',
        'abcdefghijklmnopqrstuvwxyz<br/>',
        'ABCDEFGHIJKLMNOPQRSTUVWXYZ<br/>',
        ',1234567890?¿¡;.:*@#£$%&/()=[]+',
        '</p>',
        '</div>'
    );

    function faceExample(name, clz) {
      return me.html(faceExampleTpl.apply({
          text: name,
          clz: clz
        })
      );
    }

    // Create a table
    var tableTemplate = Ext.create('Ext.XTemplate',
        '<table cellpadding="5">',
        '<thead>{thead}</thead>',
        '<tbody>{tbody}</tbody>',
        '</table>'
    );

    // Create a table head
    var theadTemplate = Ext.create('Ext.XTemplate',
        '<tpl for=".">',
        '<th>{.}</th>',
        '</tpl>'
    );

    // Create a table body
    var tbodyTemplate = Ext.create('Ext.XTemplate',
        '<tpl foreach=".">',
        '<tr>',
        '<td>{$}</td>',
        '<tpl for=".">',
        '<tpl if="clz">',
        '<td class="{clz}">{text}</td>',
        '<tpl else>',
        '<td>{.}</td>',
        '</tpl>',
        '</tpl>',
        '</tr>',
        '</tpl>'
    );

    me.items = [
      {
        xtype: 'panel',
        title: 'Faces',
        ui: 'nx-subsection',
        layout: {
          type: 'hbox',
          defaultMargins: {top: 0, right: 20, bottom: 0, left: 0}
        },
        items: [
          faceExample('Proxima Nova Regular', 'nx-proxima-nova-regular'),
          faceExample('Proxima Nova Bold', 'nx-proxima-nova-bold'),
          faceExample('Courier New', 'nx-courier-new-regular')
        ]
      },
      {
        xtype: 'panel',
        title: 'Styles',
        ui: 'nx-subsection',
        items: [
          me.html(tableTemplate.apply({
            thead: theadTemplate.apply(['Name', 'Description', 'Font & Weight', 'Use Cases', 'Pixels', 'Sample']),
            tbody: tbodyTemplate.apply({
              'h1': [
                'Header', 'Proxima Nova Light', 'Logo', '20', { text: 'Sonatype Nexus', clz: 'nx-sample-h1' }
              ],
              'h2': [
                'Header', 'Proxima Nova Bold', 'Page Title', '26', { text: 'Development', clz: 'nx-sample-h2' }
              ],
              'h3': [
                'Header', 'Proxima Nova Bold', 'Header', '22', { text: 'Development', clz: 'nx-sample-h3' }
              ],
              'h4': [
                'Header', 'Proxima Nova Bold', 'Sub-Header', '18', { text: 'Development', clz: 'nx-sample-h4' }
              ],
              'h5': [
                'Header', 'Proxima Nova Bold', 'Sub-Header', '13', { text: 'Development', clz: 'nx-sample-h5' }
              ],
              'p/ul/ol': [
                'Body', 'Proxima Nova Regular', 'Body text, lists, default size', '13', { text: 'Development', clz: 'nx-sample-body' }
              ],
              'code': [
                'Code', 'Courier New Regular', 'Code examples', '13', { text: 'Development', clz: 'nx-sample-code' }
              ],
              'utility': [
                'Small Text', 'Proxima Nova Regular', 'Labels, Side-Nav', '10', { text: 'Development', clz: 'nx-sample-utility' }
              ]
            })
          }))
        ]
      }
    ];

    me.callParent();
  }
});
