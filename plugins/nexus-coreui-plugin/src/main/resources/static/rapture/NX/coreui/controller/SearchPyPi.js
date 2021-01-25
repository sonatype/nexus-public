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
 * PyPI repository search contribution.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.controller.SearchPyPi', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.I18n'
  ],

  /**
   * @override
   */
  init: function() {
    var me = this,
        search = me.getController('NX.coreui.controller.Search');

    search.registerCriteria([
      {
        id: 'assets.attributes.pypi.classifiers',
        group: NX.I18n.get('SearchPyPi_Group'),
        config: {
          format: 'pypi',
          fieldLabel: NX.I18n.get('SearchPyPi_Classifiers_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'assets.attributes.pypi.description',
        group: NX.I18n.get('SearchPyPi_Group'),
        config: {
          format: 'pypi',
          fieldLabel: NX.I18n.get('SearchPyPi_Description_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'assets.attributes.pypi.keywords',
        group: NX.I18n.get('SearchPyPi_Group'),
        config: {
          format: 'pypi',
          fieldLabel: NX.I18n.get('SearchPyPi_Keywords_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'assets.attributes.pypi.summary',
        group: NX.I18n.get('SearchPyPi_Group'),
        config: {
          format: 'pypi',
          fieldLabel: NX.I18n.get('SearchPyPi_Summary_FieldLabel'),
          width: 250
        }
      }
    ], me);

    search.registerFilter({
      id: 'pypi',
      name: 'pypi',
      text: NX.I18n.get('SearchPyPi_Text'),
      description: NX.I18n.get('SearchPyPi_Description'),
      readOnly: true,
      criterias: [
        { id: 'format', value: 'pypi', hidden: true },
        { id: 'assets.attributes.pypi.classifiers' },
        { id: 'assets.attributes.pypi.description' },
        { id: 'assets.attributes.pypi.keywords' },
        { id: 'assets.attributes.pypi.summary' }
      ]
    }, me);
  }

});
