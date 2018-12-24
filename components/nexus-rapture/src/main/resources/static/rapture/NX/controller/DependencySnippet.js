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
/*global Ext, NX, Image*/

/**
 * Dependency snippets controller.
 *
 * @since 3.next
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
   * Add a new dependency snippet
   *
   * @public
   */
  addDependencySnippet: function(format, snippet) {
    var store = this.getStore('DependencySnippet');

    if (Array.isArray(snippet)) {
      snippet.forEach(function(element) {
        store.add({format: format, displayName: element.displayName, snippetTemplate: element.snippetTemplate});
      })
    }
    else {
      store.add({format: format, displayName: snippet.displayName, snippetTemplate: snippet.snippetTemplate});
    }
  },

  /**
   * Retrieve dependency snippet for a given component and format
   *
   * @public
   */
  findDependencySnippets: function(format, componentModel) {
    var store = this.getStore('DependencySnippet');

    return store.queryRecordsBy(function(record) {
      return format === record.get('format');
    }).map(function(record) {
      var snippetTemplate = record.get('snippetTemplate');
      if (!(snippetTemplate instanceof Ext.XTemplate)) {
        record.set('snippetTemplate', Ext.create('Ext.XTemplate', snippetTemplate));
        record.commit();
      }
      return record;
    }).map(function(record) {
      return {
        displayName: record.get('displayName'),
        snippetText: record.get('snippetTemplate').apply(componentModel)
      }
    });
  }
});
