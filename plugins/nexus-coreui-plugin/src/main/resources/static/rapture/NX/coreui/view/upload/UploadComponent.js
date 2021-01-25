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
 * Upload Component View
 *
 * @since 3.7
 */
Ext.define('NX.coreui.view.upload.UploadComponent', {
    extend: 'NX.view.AddPanel',
    alias: 'widget.nx-coreui-upload-component',
    requires: [
      'NX.I18n',
      'Ext.util.Cookies',
      'NX.coreui.view.upload.facet.DefaultUploadFacet'
    ],
    uses: [
      'NX.coreui.view.upload.facet.Maven2UploadFacet'
    ],
  /**
   * Facet for describing upload page per format, if not specified DefaultUploadFacet will be used.
   * Also facet class should be added in uses section upper.
   * @alias nx-coreui-upload-facet-{format}
   * @type {DefaultUploadFacet}
   */
  uploadFacet: undefined,

    /**
     * @override
     */
    initComponent: function() {
      var me = this;
      me.store = 'UploadDefinition';

      me.callParent();
    },

    loadRecord: function(uploadDefinition, repository) {
      var me = this;
      var formatXtype = 'nx-coreui-upload-facet-' + repository.get('format');
      var facetNameByAlias = Ext.ClassManager.getNameByAlias('widget.' + formatXtype);
      var uploadFacetAlias = facetNameByAlias ? formatXtype : 'nx-coreui-upload-facet-default';

      me.uploadFacet = Ext.create({
        xtype: uploadFacetAlias,
        uploadDefinition: uploadDefinition,
        repository: repository,
        panel: me
      });
      me.uploadFacet.addWidget();
    },

    addAssetRow: function() {
      var me = this;
      return me.uploadFacet.addAssetRow();
    }
  });
