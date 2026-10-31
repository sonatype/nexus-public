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

/**
 * Dependency snippet types for the Preview UI Overview tab.
 *
 * These types mirror the Classic UI's dependency-snippet contract
 * (NX/model/DependencySnippet.js and the per-format snippet generators) so the two
 * UIs can produce identical snippet text. See details/dependencySnippets/registry.ts.
 */

/**
 * A single copyable dependency snippet, matching the Classic generator return shape.
 */
export interface DependencySnippet {
  /** Short label for the tool/snippet (e.g. "Apache Maven", "npm", "package.json"). */
  readonly displayName: string;
  /** Optional human-readable instruction shown above the snippet text. */
  readonly description?: string;
  /** The copyable snippet body. */
  readonly snippetText: string;
}

/**
 * Component-level coordinates a generator reads from.
 *
 * Mirrors the fields the Classic `componentModel` exposes via `.get(...)`:
 * `format`, `group`, `name`, `version`, and (for NuGet) `repositoryName`.
 */
export interface SnippetComponentModel {
  /** Raw repository format (e.g. "maven2", "npm", "pypi"). */
  readonly format: string;
  /** Namespace/group. For scoped npm this is the scope without the leading "@". */
  readonly group: string;
  /** Component name. */
  readonly name: string;
  /** Component version. May be empty for versionless formats. */
  readonly version: string;
  /** Repository name; used by the NuGet Chocolatey snippet. */
  readonly repositoryName?: string;
}

/**
 * Asset-level attributes a generator may read from.
 *
 * The Overview tab is component-level, so generators are invoked with `undefined`
 * here (matching Classic passing `assetModel` as undefined). The shape is kept so the
 * asset-aware generators (Maven, Docker, OCI) can be reused for per-asset snippets later.
 */
export interface SnippetAssetModel {
  readonly attributes?: {
    readonly maven2?: {
      readonly classifier?: string;
      readonly extension?: string;
    };
  };
  /** Docker top-level registry URL (NEXUS-51972). */
  readonly registryUrl?: string;
  /** OCI image digest. */
  readonly digest?: string;
}

/**
 * A per-format snippet generator, mirroring a Classic `snippetGenerator(componentModel, assetModel)`.
 */
export type SnippetGenerator = (
  component: SnippetComponentModel,
  asset?: SnippetAssetModel,
) => DependencySnippet[];
