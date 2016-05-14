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
 * Rubygems repository search contribution.
 *
 * @since 3.0
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
        id: 'name',
        group: NX.I18n.get('SearchRubygems_Group'),
        config: {
          fieldLabel: NX.I18n.get('SearchRubygems_Name_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'version',
        group: NX.I18n.get('SearchRubygems_Group'),
        config: {
          fieldLabel: NX.I18n.get('SearchRubygems_Version_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'assets.attributes.rubygems.platform',
        group: NX.I18n.get('SearchRubygems_Group'),
        config: {
          fieldLabel: NX.I18n.get('SearchRubygems_Platform_FieldLabel'),
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
        { id: 'name' },
        { id: 'version' },
        { id: 'assets.attributes.rubygems.platform' }
      ]
    }, me);
  }

});
