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
 * @since 3.28
 */
Ext.define('NX.coreui.controller.SearchConan', {
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
        id: 'attributes.conan.baseVersion',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_BaseVersion_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.conan.channel',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_Channel_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.conan.revision',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_RecipeRevision_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'assets.attributes.conan.packageId',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_PackageId_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'assets.attributes.conan.packageRevision',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_PackageRevision_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.conan.baseVersion.strict',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_BaseVersionStrict_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.conan.revision.latest',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_RecipeRevisionLatest_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.conan.settings.arch',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_Arch_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.conan.settings.os',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_Os_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.conan.settings.compiler',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_Compiler_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.conan.settings.compiler.version',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_CompilerVersion_FieldLabel'),
          width: 250
        }
      },
      {
        id: 'attributes.conan.settings.compiler.runtime',
        group: NX.I18n.get('SearchConan_Group'),
        config: {
          format: 'conan',
          fieldLabel: NX.I18n.get('SearchConan_CompilerRuntime_FieldLabel'),
          width: 250
        }
      }
    ], me);

    search.registerFilter({
      id: 'conan',
      name: 'Conan',
      text: NX.I18n.get('SearchConan_Text'),
      description: NX.I18n.get('SearchConan_Description'),
      readOnly: true,
      criterias: [
        {id: 'format', value: 'conan', hidden: true},
        {id: 'name.raw'},
        {id: 'attributes.conan.baseVersion'},
        {id: 'attributes.conan.baseVersion.strict'},
        {id: 'attributes.conan.channel'},
        {id: 'attributes.conan.revision'},
        {id: 'assets.attributes.conan.packageId'},
        {id: 'assets.attributes.conan.packageRevision'}
      ]
    }, me);
  }
});
