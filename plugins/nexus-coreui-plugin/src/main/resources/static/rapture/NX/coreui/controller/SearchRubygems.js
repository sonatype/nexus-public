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
 * Rubygems repository search contribution.
 *
 * @since 3.1
 */
Ext.define('NX.coreui.controller.SearchRubygems', {
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
        id: 'assets.attributes.rubygems.platform',
        group: NX.I18n.get('SearchRubygems_Group'),
        config: {
          format: 'rubygems',
          fieldLabel: NX.I18n.get('SearchRubygems_Platform_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'assets.attributes.rubygems.summary',
        group: NX.I18n.get('SearchRubygems_Group'),
        config: {
          format: 'rubygems',
          fieldLabel: NX.I18n.get('SearchRubygems_Summary_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'assets.attributes.rubygems.description',
        group: NX.I18n.get('SearchRubygems_Group'),
        config: {
          format: 'rubygems',
          fieldLabel: NX.I18n.get('SearchRubygems_Description_FieldLabel'),
          width: 250
        }
      }
    ], me);

    search.registerFilter({
      id: 'rubygems',
      name: 'Rubygems',
      text: NX.I18n.get('SearchRubygems_Text'),
      description: NX.I18n.get('SearchRubygems_Description'),
      readOnly: true,
      criterias: [
        { id: 'format', value: 'rubygems', hidden: true },
        { id: 'name.raw' },
        { id: 'version' },
        { id: 'assets.attributes.rubygems.platform' },
        { id: 'assets.attributes.rubygems.summary' },
        { id: 'assets.attributes.rubygems.description' }
      ]
    }, me);
  }

});
