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
/*global Ext, NX, Image*/

/**
 * Dependency snippets controller.
 *
 * @since 3.15
 */
Ext.define('NX.controller.DependencySnippet', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.XTemplate'
  ],

  models: [
    'DependencySnippet'
  ],

  stores: [
    'DependencySnippet'
  ],

  /**
   * Add a new dependency snippet generator
   *
   * @public
   */
  addDependencySnippetGenerator: function(format, snippetGenerator) {
    this.getStore('DependencySnippet').add({
      format: format,
      snippetGenerator: snippetGenerator
    });
  },

  /**
   * Retrieve dependency snippets for a given format, component and asset.
   * Leave assetModel undefined if requesting snippets for a component.
   *
   * @public
   */
  getDependencySnippets: function(format, componentModel, assetModel) {
    var store = this.getStore('DependencySnippet');
    var dependencySnippets = [];

    store.queryRecordsBy(function(record) {
      return format === record.get('format');
    }).forEach(function(record) {
      var snippets = record.get('snippetGenerator')(componentModel, assetModel);
      Array.prototype.push.apply(dependencySnippets, snippets);
    });

    return dependencySnippets;
  }
});
