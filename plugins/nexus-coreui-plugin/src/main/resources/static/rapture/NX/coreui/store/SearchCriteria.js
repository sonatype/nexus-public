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
 * Search Criteria store.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.store.SearchCriteria', {
  extend: 'Ext.data.Store',
  model: 'NX.coreui.model.SearchCriteria',
  requires: [
    'NX.I18n',
  ],

  autoLoad: true,

  proxy: {
    type: 'memory',
    reader: {
      type: 'json'
    }
  },

  data: [
    {
      id: 'format',
      config: {
        fieldLabel: NX.I18n.get('SearchCriteria_Format_FieldLabel')
      }
    },
    {
      id: 'keyword',
      config: {
        fieldLabel: NX.I18n.get('SearchCriteria_Keyword_FieldLabel'),
        width: 250
      }
    },
    {
      id: 'version',
      config: {
        fieldLabel: NX.I18n.get('SearchCriteria_Version_FieldLabel')
      }
    },
    {
      id: 'group.raw',
      config: {
        fieldLabel: NX.I18n.get('SearchCriteria_Group_FieldLabel'),
        width: 250
      }
    },
    {
      id: 'name.raw',
      config: {
        fieldLabel: NX.I18n.get('SearchCriteria_Name_FieldLabel'),
        width: 200
      }
    },
    {
      id: 'assets.attributes.checksum.sha1',
      group: NX.I18n.get('SearchCriteria_Checksum_Group'),
      config: {
        fieldLabel: NX.I18n.get('SearchCriteria_SHA1_FieldLabel'),
        width: 250
      }
    },
    {
      id: 'assets.attributes.checksum.sha512',
      group: NX.I18n.get('SearchCriteria_Checksum_Group'),
      config: {
        fieldLabel: NX.I18n.get('SearchCriteria_SHA2_FieldLabel'),
        width: 250
      }
    },
    {
      id: 'assets.attributes.checksum.md5',
      group: NX.I18n.get('SearchCriteria_Checksum_Group'),
      config: {
        fieldLabel: NX.I18n.get('SearchCriteria_MD5_FieldLabel'),
        width: 250
      }
    }
  ],

  sorters: { property: 'id', direction: 'ASC' }

});
