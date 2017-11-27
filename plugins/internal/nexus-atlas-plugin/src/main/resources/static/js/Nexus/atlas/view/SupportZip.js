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
/**
 * Support ZIP panel.
 *
 * @since 2.7
 */
NX.define('Nexus.atlas.view.SupportZip', {
  extend: 'Ext.Panel',

  mixins: [
    'Nexus.LogAwareMixin'
  ],

  requires: [
    'Nexus.atlas.Icons'
  ],

  xtype: 'nx-atlas-view-supportzip',
  title: 'Support ZIP',
  id: 'nx-atlas-view-supportzip',
  cls: 'nx-atlas-view-supportzip',

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
              cls: 'nx-atlas-view-supportzip-description',
              border: false,
              html: icons.get('zip').variant('x32').img +
                  '<div>Creates a ZIP file containing useful support information about your server. ' +
                  'No information will be sent to Sonatype when creating the support ZIP file.' +
                  '<br/><br/>Select the contents and options for support ZIP creation:</div>'
            },
            {
              xtype: 'form',
              itemId: 'form',
              cls: 'nx-atlas-view-supportzip-form',
              layoutConfig: {
                labelSeparator: '',
                labelWidth: 70
              },
              border: false,
              monitorValid: true,
              items: [
                {
                  xtype: 'checkboxgroup',
                  fieldLabel: 'Contents',
                  columns: 1,
                  allowBlank: false,
                  anchor: '96%',
                  items: [
                    {
                      xtype: 'checkbox',
                      name: 'systemInformation',
                      boxLabel: 'System Information',
                      helpText: 'Includes system information report',
                      checked: true
                    },
                    {
                      xtype: 'checkbox',
                      name: 'threadDump',
                      boxLabel: 'Thread Dump',
                      helpText: 'Include a JVM thread-dump',
                      checked: true
                    },
                    {
                      xtype: 'checkbox',
                      name: 'configuration',
                      boxLabel: 'Configuration',
                      helpText: 'Include configuration files',
                      checked: true
                    },
                    {
                      xtype: 'checkbox',
                      name: 'security',
                      boxLabel: 'Security Configuration',
                      helpText: 'Include security configuration files',
                      checked: true
                    },
                    {
                      xtype: 'checkbox',
                      name: 'log',
                      boxLabel: 'Log',
                      helpText: 'Include log files',
                      checked: true
                    },
                    {
                      xtype: 'checkbox',
                      name: 'metrics',
                      boxLabel: 'Metrics',
                      helpText: 'Includes system and component metrics',
                      checked: true
                    },
                    {
                      xtype: 'checkbox',
                      name: 'jmxinfo',
                      boxLabel: 'JMX information',
                      helpText: 'Includes JMX Bean information',
                      checked: true
                    }
                  ]
                },
                {
                  xtype: 'checkboxgroup',
                  fieldLabel: 'Options',
                  columns: 1,
                  anchor: '96%',
                  items: [
                    {
                      xtype: 'checkbox',
                      name: 'limitFileSizes',
                      boxLabel: 'Limit Included File Sizes',
                      helpText: 'Limit the size of files included in the support ZIP to no more than 30 MB each.',
                      checked: true
                    },
                    {
                      xtype: 'checkbox',
                      name: 'limitZipSize',
                      boxLabel: 'Limit Maximum ZIP File Size',
                      helpText: 'Limit the maximum size of the support ZIP file to no more than 20 MB.',
                      checked: true
                    }
                  ]}
              ],
              buttons: [
                { text: 'Create', id: 'nx-atlas-button-supportzip-create', formBind: true }
              ],
              buttonAlign: 'left'
            }
          ]
        }
      ]
    });

    me.constructor.superclass.initComponent.apply(me, arguments);
  },

  /**
   * Get form values.
   *
   * @public
   */
  getValues: function () {
    return this.down('form').getForm().getValues();
  }
});
