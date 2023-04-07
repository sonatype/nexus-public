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
export const COMPONENTS = [
  {
    id: "folder1",
    text: "folder1",
    type: "folder",
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null
  },
  {
    id: "component1",
    text: "component1",
    type: "component",
    leaf: false,
    component: null,
    assetId: null,
    packageUrl: null
  },
  {
    id: "asset1",
    text: "asset1",
    type: "asset",
    leaf: true,
    componentId: null,
    assetId: null,
    packageUrl: null
  }
]

export const FOLDER1_CHILDREN = [
  {
    id: "folder1/component1",
    text: "component1",
    type: "component",
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
  {
    id: "folder1/component2",
    text: "component2",
    type: "component",
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  }
]

export const COMPONENT1_CHILDREN = [
  {
    id: "folder1/component1/asset1.txt",
    text: "asset1.txt",
    type: "asset",
    leaf: true,
    componentId: null,
    assetId: "asset1",
    packageUrl: null,
  },
  {
    id: "folder1/component1/asset1.txt.md5",
    text: "asset1.txt.md5",
    type: "asset",
    leaf: true,
    componentId: null,
    assetId: "asset1.txt.md5",
    packageUrl: null,
  },
  {
    id: "folder1/component1/asset1.txt.sha1",
    text: "asset1.txt.sha1",
    type: "asset",
    leaf: true,
    componentId: null,
    assetId: "asset1.txt.sha1",
    packageUrl: null,
  },
  {
    id: "folder1/component1/asset1.txt.sha256",
    text: "asset1.txt.sha256",
    type: "asset",
    leaf: true,
    componentId: null,
    assetId: "asset1.txt.sha256",
    packageUrl: null,
  },
  {
    id: "folder1/component1/asset1.txt.sha512",
    text: "asset1.txt.sha512",
    type: "asset",
    leaf: true,
    componentId: null,
    assetId: "asset1.txt.sha512",
    packageUrl: null,
  }
]
