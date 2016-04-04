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
 * Color styles.
 *
 * @since 3.0
 */
Ext.define('NX.view.dev.styles.Colors', {
  extend: 'NX.view.dev.styles.StyleSection',
  requires: [
    'Ext.XTemplate'
  ],

  title: 'Colors',

  /**
   * @protected
   */
  initComponent: function () {
    var me = this;

    var rowTemplate = Ext.create('Ext.XTemplate',
        '<div>',
        '<tpl for=".">',
        '<div class="nx-hbox">{.}</div>',
        '</tpl>',
        '</div>'
    );

    var columnTemplate = Ext.create('Ext.XTemplate',
        '<div>',
        '<tpl for=".">',
        '<div class="nx-vbox">{.}</div>',
        '</tpl>',
        '</div>'
    );

    var labelTemplate = Ext.create('Ext.XTemplate',
        '<span class="{clz}">{text}</span>'
    );

    var paletteTemplate = Ext.create('Ext.XTemplate',
        '<div style="margins: 0 20px 20px 0">',
        '<tpl for="."><div style="float: left;">{.}</div></tpl>',
        '</div>'
    );

    var colorTemplate = Ext.create('Ext.XTemplate',
        '<div>',
        '<div height="40" width="80" class="{clz}"></div>',
        '<div>{name}</div>',
        '<div>{value}</div>',
        '</div>'
    );

    me.items = [
      {
        xtype: 'container',
        layout: {
          type: 'vbox',
          padding: 4
        },
        items: [
          me.html(columnTemplate.apply([
            labelTemplate.apply({text: 'Shell', clz: 'nx-section-header' }),
            paletteTemplate.apply([
              colorTemplate.apply({clz: 'nx-color black', name: 'Black', value: '#000000'}),
              colorTemplate.apply({clz: 'nx-color night-rider', name: 'Night Rider', value: '#333333'}),
              colorTemplate.apply({clz: 'nx-color charcoal', name: 'Charcoal', value: '#444444'}),
              colorTemplate.apply({clz: 'nx-color dark-gray', name: 'Dark Gray', value: '#777777'}),
              colorTemplate.apply({clz: 'nx-color gray', name: 'Gray', value: '#AAAAAA'}),
              colorTemplate.apply({clz: 'nx-color light-gray', name: 'Light Gray', value: '#CBCBCB'}),
              colorTemplate.apply({clz: 'nx-color gainsboro', name: 'Gainsboro', value: '#DDDDDD'}),
              colorTemplate.apply({clz: 'nx-color smoke', name: 'Smoke', value: '#EBEBEB'}),
              colorTemplate.apply({clz: 'nx-color light-smoke', name: 'Light Smoke', value: '#F4F4F4'}),
              colorTemplate.apply({clz: 'nx-color white', name: 'White', value: '#FFFFFF'})
            ])
          ])),

          me.html(rowTemplate.apply([
            columnTemplate.apply([
              labelTemplate.apply({text: 'Severity', clz: 'nx-section-header' }),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color cerise', name: 'Cerise', value: '#DB2852'}),
                colorTemplate.apply({clz: 'nx-color sun', name: 'Sun', value: '#F2862F'}),
                colorTemplate.apply({clz: 'nx-color energy-yellow', name: 'Energy Yellow', value: '#F5C649'}),
                colorTemplate.apply({clz: 'nx-color cobalt', name: 'Cobalt', value: '#0047B2'}),
                colorTemplate.apply({clz: 'nx-color cerulean-blue', name: 'Cerulean Blue', value: '#2476C3'})
              ])
            ]),
            columnTemplate.apply([
              labelTemplate.apply({text: 'Forms', clz: 'nx-section-header' }),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color citrus', name: 'Citrus', value: '#84C900'}),
                colorTemplate.apply({clz: 'nx-color free-speech-red', name: 'Free Speech Red', value: '#C70000'})
              ])
            ]),
            columnTemplate.apply([
              labelTemplate.apply({text: 'Tooltip', clz: 'nx-section-header' }),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color energy-yellow', name: 'Energy Yellow', value: '#F5C649'}),
                colorTemplate.apply({clz: 'nx-color floral-white', name: 'Floral White', value: '#FFFAEE'})
              ])
            ])
          ])),

          me.html(columnTemplate.apply([
            labelTemplate.apply({text: 'Dashboard', clz: 'nx-section-header' }),
            paletteTemplate.apply([
              colorTemplate.apply({clz: 'nx-color pigment-green', name: 'Pigment Green', value: '#0B9743'}),
              colorTemplate.apply({clz: 'nx-color madang', name: 'Madang', value: '#B6E9AB'}),
              colorTemplate.apply({clz: 'nx-color venetian-red', name: 'Venetian Red', value: '#BC0430'}),
              colorTemplate.apply({clz: 'nx-color beauty-bush', name: 'Beauty Bush', value: '#EDB2AF'}),
              colorTemplate.apply({clz: 'nx-color navy-blue', name: 'Navy Blue', value: '#006BBF'}),
              colorTemplate.apply({clz: 'nx-color cornflower', name: 'Cornflower', value: '#96CAEE'}),
              colorTemplate.apply({clz: 'nx-color east-side', name: 'East Side', value: '#B087B9'}),
              colorTemplate.apply({clz: 'nx-color blue-chalk', name: 'Blue Chalk', value: '#DAC5DF'})
            ])
          ])),

          me.html(rowTemplate.apply([
            columnTemplate.apply([
              labelTemplate.apply({text: 'Buttons', clz: 'nx-section-header' }),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color white', name: 'White', value: '#FFFFFF'}),
                colorTemplate.apply({clz: 'nx-color light-gainsboro', name: 'Light Gainsboro', value: '#E6E6E6'}),
                colorTemplate.apply({clz: 'nx-color light-gray', name: 'Light Gray', value: '#CBCBCB'}),
                colorTemplate.apply({clz: 'nx-color silver', name: 'Silver', value: '#B8B8B8'}),
                colorTemplate.apply({clz: 'nx-color suva-gray', name: 'Suva Gray', value: '#919191'}),
                colorTemplate.apply({clz: 'nx-color gray', name: 'Gray', value: '#808080'})
              ]),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color denim', name: 'Denim', value: '#197AC5'}),
                colorTemplate.apply({clz: 'nx-color light-cobalt', name: 'Light Cobalt', value: '#0161AD'}),
                colorTemplate.apply({clz: 'nx-color dark-denim', name: 'Dark Denim', value: '#14629E'}),
                colorTemplate.apply({clz: 'nx-color smalt', name: 'Smalt', value: '#014E8A'}),
                colorTemplate.apply({clz: 'nx-color dark-cerulean', name: 'Dark Cerulean', value: '#0F4976'}),
                colorTemplate.apply({clz: 'nx-color prussian-blue', name: 'Prussian Blue', value: '#013A68'})
              ]),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color light-cerise', name: 'Light Cerise', value: '#DE3D63'}),
                colorTemplate.apply({clz: 'nx-color brick-red', name: 'Brick Red', value: '#C6254B'}),
                colorTemplate.apply({clz: 'nx-color old-rose', name: 'Old Rose', value: '#B2314F'}),
                colorTemplate.apply({clz: 'nx-color fire-brick', name: 'Fire Brick', value: '#9E1E3C'}),
                colorTemplate.apply({clz: 'nx-color shiraz', name: 'Shiraz', value: '#85253B'}),
                colorTemplate.apply({clz: 'nx-color falu-red', name: 'Falu Red', value: '#77162D'})
              ]),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color sea-buckthorn', name: 'Sea Buckthorn', value: '#F39244'}),
                colorTemplate.apply({clz: 'nx-color tahiti-gold', name: 'Tahiti Gold', value: '#DA792B'}),
                colorTemplate.apply({clz: 'nx-color zest', name: 'Zest', value: '#C17536'}),
                colorTemplate.apply({clz: 'nx-color rich-gold', name: 'Rich Gold', value: '#AE6122'}),
                colorTemplate.apply({clz: 'nx-color afghan-tan', name: 'Afghan Tan', value: '#925829'}),
                colorTemplate.apply({clz: 'nx-color russet', name: 'Russet', value: '#83491A'})
              ]),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color elf-green', name: 'Elf Green', value: '#23A156'}),
                colorTemplate.apply({clz: 'nx-color dark-pigment-green', name: 'Dark Pigment Green', value: '#0B893D'}),
                colorTemplate.apply({clz: 'nx-color salem', name: 'Salem', value: '#1C8145'}),
                colorTemplate.apply({clz: 'nx-color jewel', name: 'Jewel', value: '#096E31'}),
                colorTemplate.apply({clz: 'nx-color fun-green', name: 'Fun Green', value: '#156134'}),
                colorTemplate.apply({clz: 'nx-color dark-jewel', name: 'Dark Jewel', value: '#0C4F26'})
              ])
            ]),
            columnTemplate.apply([
              labelTemplate.apply({text: 'Font Awesome Icons', clz: 'nx-section-header' }),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color navy-blue', name: 'Navy Blue', value: '#006BBF'}),
                colorTemplate.apply({clz: 'nx-color smalt', name: 'Smalt', value: '#014E8A'}),
                colorTemplate.apply({clz: 'nx-color prussian-blue', name: 'Prussian Blue', value: '#013A68'})
              ]),
              paletteTemplate.apply([
                colorTemplate.apply({clz: 'nx-color white', name: 'White', value: '#FFFFFF'}),
                colorTemplate.apply({clz: 'nx-color gainsboro', name: 'Gainsboro', value: '#DDDDDD'}),
                colorTemplate.apply({clz: 'nx-color gray', name: 'Gray', value: '#AAAAAA'})
              ])
            ])
          ]))
        ]
      }
    ];

    me.callParent();
  }
});
