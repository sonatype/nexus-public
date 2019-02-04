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
 * Registers all feature groups for coreui.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.FeatureGroups', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  init: function () {
    this.getApplication().getFeaturesController().registerFeature([
      {
        mode: 'admin',
        path: '/Repository',
        text: NX.I18n.get('FeatureGroups_Repository_Text'),
        description: NX.I18n.get('FeatureGroups_Repository_Description'),
        group: true,
        weight: 50,
        iconConfig: {
          file: 'database.png',
          variants: ['x16', 'x32']
        }
      },
      {
        mode: 'admin',
        path: '/Security',
        text: NX.I18n.get('FeatureGroups_Security_Title'),
        description: NX.I18n.get('FeatureGroups_Security_Description'),
        group: true,
        weight: 90,
        iconConfig: {
          file: 'security.png',
          variants: ['x16', 'x32']
        }
      },
      {
        mode: 'admin',
        path: '/Support',
        text: NX.I18n.get('FeatureGroups_Support_Text'),
        description: NX.I18n.get('FeatureGroups_Support_Description'),
        group: true,
        expanded: false,
        iconConfig: {
          file: 'support.png',
          variants: ['x16', 'x32']
        }
      },
      {
        mode: 'admin',
        path: '/System',
        text: NX.I18n.get('FeatureGroups_System_Text'),
        description: NX.I18n.get('FeatureGroups_System_Description'),
        group: true,
        expanded: false,
        weight: 1000,
        iconConfig: {
          file: 'cog.png',
          variants: ['x16', 'x32']
        }
      },
      {
        mode: 'browse',
        path: '/Upload',
        text: NX.I18n.get('FeatureGroups_Upload_Text'),
        description: NX.I18n.get('FeatureGroups_Upload_Description'),
        group: true,
        iconConfig: {
          file: 'upload.png',
          variants: ['x16', 'x32']
        }
      }
    ]);
  }
});
