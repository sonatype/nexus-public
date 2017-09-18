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
 * Component/Asset tree model.
 *
 * @since 3.6
 */
Ext.define('NX.coreui.model.ComponentAssetTree', {
  extend: 'Ext.data.Model',
  fields: [
    {name: 'id', type: 'string', sortType: 'asUCText'},
    {name: 'type', type: 'string', sortType: 'asUCText'},
    {name: 'text', type: 'string', sortType: 'asUCText'},
    {name: 'iconCls', type: 'string', convert: function(value, record){
      switch (record.get('type')) {
        case 'folder':
          return 'nx-icon-tree-folder-x16';
        case 'component':
          return 'nx-icon-tree-component-x16';
        case 'asset':
          return 'nx-icon-tree-asset-x16';
        default:
          return null;
      }
    }},
    {name: 'leaf', type: 'boolean'},
    {name: 'componentId', type: 'string'},
    {name: 'assetId', type: 'string'}
  ]
});
