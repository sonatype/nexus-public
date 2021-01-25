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
 * Component/Asset tree result store.
 *
 * @since 3.6
 */
(function() {
  Ext.define('NX.coreui.store.ComponentAssetTree', {
      extend: 'Ext.data.TreeStore',
      model: 'NX.coreui.model.ComponentAssetTree',
      autoLoad: false,
      paramOrder: ['node', 'repositoryName', 'filter'],
      defaultRootId: '/',
      folderSort: true,
      root: {
          node: '/',
          expanded: false
      },
      proxy: {
          type: 'direct',
          api: {
              read: 'NX.direct.coreui_Browse.read'
          },

          reader: {
              type: 'json',
              rootProperty: 'data',
              successProperty: 'success'
          }
      },
      listeners: {
          beforeload: function (store) {
              if (store.isLoading()) {
                  return false;
              }
          },
          nodeexpand: function(node) {
              if (node.childNodes && node.childNodes.length === 1) {
                  node.childNodes[0].expand();
              }
          }
      }
  });
}());
