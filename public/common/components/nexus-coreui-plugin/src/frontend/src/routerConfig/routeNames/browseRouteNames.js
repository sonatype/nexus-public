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

export default {
  // Preview-themed entries live in
  // nexus-ui-plugin/src/frontend/src/components/preview/constants/RouteNames.ts
  // under PREVIEW_* keys (PREVIEW_MALICIOUS_PACKAGES, PREVIEW_PROTECT,
  // PREVIEW_MALWARERISK) and are merged in by this plugin's routeNames.js
  // aggregator. Classic entries stay here.
  DIRECTORY: 'browse',
  BROWSE: {
    ROOT: 'browse.browse',
    TITLE: 'Browse',
  },
  WELCOME: {
    ROOT: 'browse.welcome',
    TITLE: 'Dashboard',
  },
  UPLOAD: {
    ROOT: 'browse.upload',
    TITLE: 'Upload',
    LIST: 'browse.upload.list',
    EDIT: 'browse.upload.edit',
  },
  TAGS: {
    ROOT: 'browse.tags',
    TITLE: 'Tags',
  },
  MALWARERISK: {
    ROOT: 'browse.malwarerisk',
    TITLE: 'Malicious Packages',
  },
  SEARCH: {
    ROOT: 'browse.search',
    TITLE: 'Search',
    UNIFIED: 'browse.search.unified',
    GENERIC: 'browse.search.generic',
    CUSTOM: 'browse.search.custom',
    ALPINE: 'browse.search.alpine',
    APT: 'browse.search.apt',
    CARGO: 'browse.search.cargo',
    COCOAPODS: 'browse.search.cocoapods',
    COMPOSER: 'browse.search.composer',
    CONAN: 'browse.search.conan',
    CONDA: 'browse.search.conda',
    DOCKER: 'browse.search.docker',
    GITLFS: 'browse.search.gitlfs',
    GOLANG: 'browse.search.golang',
    HELM: 'browse.search.helm',
    HUGGING_FACE: 'browse.search.hugging_face',
    MAVEN: 'browse.search.maven',
    NPM: 'browse.search.npm',
    NUGET: 'browse.search.nuget',
    P2: 'browse.search.p2',
    PYPI: 'browse.search.pypi',
    PUB: 'browse.search.pub',
    R: 'browse.search.r',
    RAW: 'browse.search.raw',
    RUBYGEMS: 'browse.search.rubygems',
    ANSIBLEGALAXY: 'browse.search.ansiblegalaxy',
    TERRAFORM: 'browse.search.terraform',
    YUM: 'browse.search.yum',
    SWIFT: 'browse.search.swift',
  },
};
