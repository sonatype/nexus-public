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
 * Maven2 Component Details Provider.
 *
 * @since 3.14
 */
Ext.define('NX.coreui.util.Maven2ComponentActionHandler', {
  alias: 'nx-coreui-maven2-component-action-handler',
  singleton: true,
  requires: [
      'NX.Bookmarks',
      'NX.I18n'
  ],

  isSnapshot: function(componentModel) {
    var id;

    if (!componentModel) {
      return false;
    }

    id = componentModel.get('id');
    return (componentModel.get('format') === 'maven2') &&
        id.slice(-'-SNAPSHOT'.length) === '-SNAPSHOT';
  },

  updateDeleteButtonVisibility: function(button, componentModel) {
    var visibilityUpdated;

    if (this.isSnapshot(componentModel)) {
      button.hide();
      visibilityUpdated = true;
    } else {
      visibilityUpdated = false;
    }

    return visibilityUpdated;
  },

  updateBrowseButtonVisibility: function(button, componentModel) {
    if (this.isSnapshot(componentModel)) {
      button.show();
    } else {
      button.hide();
    }

    return true;
  },

  /**
   * Browse to the selected component.
   *
   * @private
   */
  browseComponent: function(componentModel, assetModel) {
    var repositoryName, componentGroup, componentName,
        attributes, version, baseVersion, path;

    if (componentModel && assetModel) {
      repositoryName = componentModel.get('repositoryName');
      componentGroup = componentModel.get('group').replace(/\./g, '/');
      componentName = componentModel.get('name');

      attributes = assetModel.get('attributes');
      version = attributes.maven2 && attributes.maven2.version;
      baseVersion = attributes.maven2 && attributes.maven2.baseVersion;

      path = 'browse/browse:' + encodeURIComponent(repositoryName) + ':' +
          encodeURIComponent(componentGroup + '/' + componentName + '/' + baseVersion + '/' + version);

      NX.Bookmarks.navigateTo(NX.Bookmarks.fromToken(path));
    }
  }


});
