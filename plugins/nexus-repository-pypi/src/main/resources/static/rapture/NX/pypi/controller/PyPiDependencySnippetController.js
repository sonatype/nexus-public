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
 * PyPI controller.
 *
 * @since 3.15
 */
Ext.define('NX.pypi.controller.PyPiDependencySnippetController', {
  extend: 'NX.app.Controller',

  /**
   * @override
   */
  init: function() {
    NX.getApplication().getDependencySnippetController()
        .addDependencySnippetGenerator('pypi', this.snippetGenerator);
  },

  snippetGenerator: function(componentModel, assetModel) {
    var name = componentModel.get('name'),
        version = componentModel.get('version');

    return [
      {
        displayName: 'pip',
        snippetText: 'pip install ' + name + '==' + version
      }, {
        displayName: 'easy_install',
        snippetText: 'easy_install ' + name + '==' + version
      }, {
        displayName: 'pipenv',
        snippetText: 'pipenv install ' + name + '==' + version
      }, {
        displayName: 'requirements.txt',
        snippetText: name + ' == ' + version
      }
    ];
  }
});
