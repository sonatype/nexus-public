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
export const REPOS = [
  {
    "name": "maven-public",
    "url": "http://localhost:8081/repository/maven-public",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": false,
      "writePolicy": "ALLOW_ONCE"
    },
    "cleanup": null,
    "maven": {
      "versionPolicy": "RELEASE",
      "layoutPolicy": "STRICT",
      "contentDisposition": "INLINE"
    },
    "component": {"proprietaryComponents": false},
    "format": "raw",
    "type": "hosted"
  },
  {
    "name": "maven-releases",
    "url": "http://localhost:8081/repository/maven-releases",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": false,
      "writePolicy": "ALLOW_ONCE"
    },
    "cleanup": null,
    "maven": {
      "versionPolicy": "RELEASE",
      "layoutPolicy": "STRICT",
      "contentDisposition": "INLINE"
    },
    "component": {"proprietaryComponents": false},
    "format": "maven2",
    "type": "hosted"
  },
  {
    "name": "nuget-hosted",
    "format": "nuget",
    "url": "http://localhost:8081/repository/nuget-hosted",
    "online": true,
    "storage": {
      "blobStoreName": "default",
      "strictContentTypeValidation": true,
      "writePolicy": "ALLOW"
    },
    "cleanup": null,
    "component": {"proprietaryComponents": false},
    "type": "hosted"
  },
];

export const FORMATS = [
  {uiUpload: true, format: 'helm'},
  {uiUpload: true, format: 'r'},
  {uiUpload: true, format: 'pypi'},
  {uiUpload: true, format: 'yum'},
  {uiUpload: true, format: 'rubygems'},
  {uiUpload: true, format: 'npm'},
  {uiUpload: true, format: 'raw'},
  {uiUpload: true, format: 'apt'},
  {uiUpload: true, format: 'nuget'},
  {uiUpload: true, format: 'maven2'},
];

export const FIELDS = {
  NAME: 'name',
  FORMAT: 'format',
};
