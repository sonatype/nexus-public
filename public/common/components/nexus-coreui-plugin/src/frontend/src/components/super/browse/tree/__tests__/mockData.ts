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

import type { BrowseNode } from '../browse-tree.types';

/**
 * Mock data for BrowseTree tests.
 */

/**
 * Root level nodes.
 */
export const mockRootNodes: BrowseNode[] = [
  {
    id: 'folder1',
    text: 'folder1',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
  {
    id: 'component1',
    text: 'component1',
    type: 'component',
    leaf: false,
    componentId: 'comp-1',
    assetId: null,
    packageUrl: null,
  },
  {
    id: 'asset1.jar',
    text: 'asset1.jar',
    type: 'asset',
    leaf: true,
    componentId: null,
    assetId: 'asset-1',
    packageUrl: null,
  },
];

/**
 * Children of folder1.
 */
export const mockFolder1Children: BrowseNode[] = [
  {
    id: 'folder1/subfolder1',
    text: 'subfolder1',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
  {
    id: 'folder1/component2',
    text: 'component2',
    type: 'component',
    leaf: false,
    componentId: 'comp-2',
    assetId: null,
    packageUrl: null,
  },
];

/**
 * Children of folder1/subfolder1.
 */
export const mockSubfolder1Children: BrowseNode[] = [
  {
    id: 'folder1/subfolder1/file1.txt',
    text: 'file1.txt',
    type: 'asset',
    leaf: true,
    componentId: null,
    assetId: 'asset-2',
    packageUrl: null,
  },
];

/**
 * Children of component1.
 */
export const mockComponent1Children: BrowseNode[] = [
  {
    id: 'component1/asset1.txt',
    text: 'asset1.txt',
    type: 'asset',
    leaf: true,
    componentId: 'comp-1',
    assetId: 'asset-3',
    packageUrl: null,
  },
  {
    id: 'component1/asset1.txt.md5',
    text: 'asset1.txt.md5',
    type: 'asset',
    leaf: true,
    componentId: 'comp-1',
    assetId: 'asset-4',
    packageUrl: null,
  },
  {
    id: 'component1/asset1.txt.sha1',
    text: 'asset1.txt.sha1',
    type: 'asset',
    leaf: true,
    componentId: 'comp-1',
    assetId: 'asset-5',
    packageUrl: null,
  },
];

/**
 * Single child folder (for auto-expand testing).
 */
export const mockSingleChildFolder: BrowseNode[] = [
  {
    id: 'single-child-folder',
    text: 'single-child-folder',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
];

/**
 * Children of single-child-folder (single child).
 */
export const mockSingleChildFolderChildren: BrowseNode[] = [
  {
    id: 'single-child-folder/nested',
    text: 'nested',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
];

/**
 * Children of single-child-folder/nested.
 */
export const mockNestedChildren: BrowseNode[] = [
  {
    id: 'single-child-folder/nested/file.txt',
    text: 'file.txt',
    type: 'asset',
    leaf: true,
    componentId: null,
    assetId: 'asset-6',
    packageUrl: null,
  },
];

/**
 * Error nodes returned by the backend when a repository has null references (bug s0h6).
 */
export const mockErrorNodes: BrowseNode[] = [
  {
    id: 'error-node-1',
    text: 'Cannot invoke org.sonatype.nexus.repository.Repository.getName() because repository is null',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
  {
    id: 'error-node-2',
    text: 'java.lang.NullPointerException: Cannot read field "name"',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
];

/**
 * Mix of valid and error nodes — the tree should only show valid ones.
 */
export const mockMixedWithErrorNodes: BrowseNode[] = [
  mockRootNodes[0], // folder1 — valid
  mockErrorNodes[0], // error — should be filtered
  mockRootNodes[2], // asset1.jar — valid
  mockErrorNodes[1], // error — should be filtered
];

/**
 * Go module root nodes — IDs use Java URLEncoder encoding (%21 for !, %2B for +).
 * This simulates paths like `github.com/!data!dog/gostackparse/@v/v0.7.0.mod`
 * where Java's BrowseComponent encodes `!` → `%21`, `@` → `%40`, `+` → `%2B`.
 */
export const mockGoRootNodes: BrowseNode[] = [
  {
    id: 'github.com',
    text: 'github.com',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
];

export const mockGoGithubChildren: BrowseNode[] = [
  {
    id: 'github.com/%21data%21dog',
    text: '!data!dog',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
];

export const mockGoDatadogChildren: BrowseNode[] = [
  {
    id: 'github.com/%21data%21dog/gostackparse',
    text: 'gostackparse',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
];

export const mockGoGostackparseChildren: BrowseNode[] = [
  {
    id: 'github.com/%21data%21dog/gostackparse/%40v',
    text: '@v',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
];

export const mockGoAtVChildren: BrowseNode[] = [
  {
    id: 'github.com/%21data%21dog/gostackparse/%40v/v0.7.0.mod',
    text: 'v0.7.0.mod',
    type: 'asset',
    leaf: true,
    componentId: 'go-comp-1',
    assetId: 'go-asset-1',
    packageUrl: 'pkg:golang/github.com/!data!dog/gostackparse@v0.7.0',
  },
];

/**
 * Nodes with `+` in path — Java encodes `+` to `%2B`.
 */
export const mockPlusRootNodes: BrowseNode[] = [
  {
    id: 'lib%2B%2B',
    text: 'lib++',
    type: 'folder',
    leaf: false,
    componentId: null,
    assetId: null,
    packageUrl: null,
  },
];

export const mockPlusChildren: BrowseNode[] = [
  {
    id: 'lib%2B%2B/file%2B1.txt',
    text: 'file+1.txt',
    type: 'asset',
    leaf: true,
    componentId: null,
    assetId: 'plus-asset-1',
    packageUrl: null,
  },
];

/**
 * Mock API response helper.
 */
export function createMockApiResponse(data: BrowseNode[]): { result: { success: boolean; data: BrowseNode[] } } {
  return {
    result: {
      success: true,
      data,
    },
  };
}

/**
 * Map of node IDs to their children for mocking.
 */
export const mockChildrenMap: Record<string, BrowseNode[]> = {
  '/': mockRootNodes,
  'folder1': mockFolder1Children,
  'folder1/subfolder1': mockSubfolder1Children,
  'component1': mockComponent1Children,
  'single-child-folder': mockSingleChildFolderChildren,
  'single-child-folder/nested': mockNestedChildren,
  'github.com': mockGoGithubChildren,
  'github.com/%21data%21dog': mockGoDatadogChildren,
  'github.com/%21data%21dog/gostackparse': mockGoGostackparseChildren,
  'github.com/%21data%21dog/gostackparse/%40v': mockGoAtVChildren,
  'lib%2B%2B': mockPlusChildren,
};

/**
 * Get mock children for a node ID.
 */
export function getMockChildren(nodeId: string): BrowseNode[] {
  return mockChildrenMap[nodeId] || [];
}

