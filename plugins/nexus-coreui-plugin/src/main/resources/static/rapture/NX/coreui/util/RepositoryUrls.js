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
 * URL related utils.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.util.RepositoryUrls', {
  singleton: true,
  requires: [
    'NX.util.Url',
    'NX.Assert'
  ],

  mixins: {
    logAware: 'NX.LogAware'
  },

  /**
   * Strategies for building urls to download assets.
   *
   * @private
   */
  repositoryUrlStrategies: {
    maven2: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    apt: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    cocoapods: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    conan: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    conda: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    npm: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    nuget: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    r: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    raw: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    rubygems: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    docker: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    pypi: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    yum: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    gitlfs: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    go: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    cargo: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    composer: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    helm: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    p2: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    },
    huggingface: function (me, assetModel) {
      var repositoryName = assetModel.get('repositoryName'),
          assetName = me.getAssetName(assetModel);
      return NX.util.Url.asLink(NX.util.Url.relativePath + '/repository/' + encodeURIComponent(repositoryName) + '/' + encodePath(assetName), assetName);
    }
  },

  /**
   * Get the asset name without beginning '/'.
   *
   * @private
   * @param {Object} assetModel the asset to fetch its name.
   * @return the asset name.
   */
  getAssetName: function(assetModel) {
    var assetName = assetModel.get('name');
    return assetName.startsWith('/') ? assetName.substring(1) : assetName;
  },

  /**
   * Add a strategy to build repository download links for a particular strategy.
   *
   * @public
   */
  addRepositoryUrlStrategy: function (format, strategy) {
    this.repositoryUrlStrategies[format] = strategy;
  },

  /**
   * Creates a link to an asset in a repository.
   *
   * @public
   * @param {Object} assetModel the asset to create a link for
   * @param {String} format the format of the repository storing this asset
   */
  asRepositoryLink: function (assetModel, format) {
    //<if assert>
    NX.Assert.assert(assetModel, 'Expected an assetModel with format: ' + format);
    //</if>
    //<if debug>
    this.logTrace('Creating link for format and asset:', format, assetModel.get('name'));
    //</if>

    var linkStrategy = this.repositoryUrlStrategies[format];
    return linkStrategy(this, assetModel);
  }
});

/**
 * Breaks down the component path into parts, encodes each part and adds back the slashes, returns the full component path with / not encoded
 *
 * @param uri: full component path
 * @returns component path encoded correctly without slashes included
 */
function encodePath(uri) {
  var parts = uri.split("/");
  for (var i = 0; i < parts.length; i++) {
    parts[i] = encodeURIComponent(parts[i]);
  }
  return parts.join("/");
}
