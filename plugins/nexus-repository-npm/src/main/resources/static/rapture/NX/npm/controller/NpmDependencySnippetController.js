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
 * @since 3.16
 */
Ext.define('NX.npm.controller.NpmDependencySnippetController', {
  extend: 'NX.app.Controller',

  /**
   * @override
   */
  init: function() {
    NX.getApplication().getDependencySnippetController().addDependencySnippetGenerator('npm', this.snippetGenerator);
  },

  snippetGenerator: function(componentModel, assetModel) {
    var group = componentModel.get('group'),
        name = componentModel.get('name'),
        version = componentModel.get('version'),
        dependencyName = '';

    if (group) {
      dependencyName = '@' + group + '/';
    }

    dependencyName = dependencyName + name;

    return [
      {
        displayName: 'Npm',
        description: 'Install runtime dependency',
        snippetText: 'npm install ' + dependencyName + '@' + version
      },
      {
        displayName: 'Yarn',
        description: 'Install runtime dependency',
        snippetText: 'yarn add ' + dependencyName + '@' + version
      },
      {
        displayName: 'package.json',
        description: 'Install runtime dependency to the package.json\'s "dependencies" section',
        snippetText: '"' + dependencyName + '": "' + version + '"'
      }
    ]
  }
});
