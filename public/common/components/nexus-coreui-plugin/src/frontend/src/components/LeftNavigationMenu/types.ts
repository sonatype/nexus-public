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
import { FORMATS } from '@sonatype/nexus-ui-plugin';

/**
 * Definition of a Classic UI search format entry.
 *
 * - `apiFormat` is matched against the set of formats returned by the repositories
 *   REST API to decide whether the entry should be shown.
 * - `routeKey` indexes into `ROUTE_NAMES.BROWSE.SEARCH` to resolve the target route.
 */
export interface ClassicSearchFormat {
  /** Stable React key / identifier for the entry. */
  id: string;
  /** Format identifier as reported by the repositories REST API (e.g. `maven2`). */
  apiFormat: string;
  /** Human-readable label shown in the submenu. */
  label: string;
  /** Key into `ROUTE_NAMES.BROWSE.SEARCH` for the format-specific search route. */
  routeKey: string;
}

/**
 * A search format that resolved to a concrete route and is visible in the submenu.
 * This is the shape consumed by the presentation layer after filtering.
 */
export interface VisibleFormat {
  /** Stable React key / identifier for the entry. */
  id: string;
  /** Human-readable label shown in the submenu. */
  label: string;
  /** Resolved route name for the format-specific search route. */
  routeName: string;
}

/**
 * Classic UI search submenu formats.
 *
 * The full catalogue of formats that can appear under the Classic UI "Search"
 * entry. The hook layer filters this list down to the formats the user actually
 * has repositories for (see `useSearchCollapsibleNav`).
 */
export const CLASSIC_SEARCH_FORMATS: ClassicSearchFormat[] = [
  { id: 'alpine', apiFormat: 'alpine', label: FORMATS.alpine.label, routeKey: 'ALPINE' },
  { id: 'apt', apiFormat: 'apt', label: FORMATS.apt.label, routeKey: 'APT' },
  { id: 'cargo', apiFormat: 'cargo', label: FORMATS.cargo.label, routeKey: 'CARGO' },
  { id: 'cocoapods', apiFormat: 'cocoapods', label: FORMATS.cocoapods.label, routeKey: 'COCOAPODS' },
  { id: 'composer', apiFormat: 'composer', label: FORMATS.composer.label, routeKey: 'COMPOSER' },
  { id: 'conan', apiFormat: 'conan', label: FORMATS.conan.label, routeKey: 'CONAN' },
  { id: 'conda', apiFormat: 'conda', label: FORMATS.conda.label, routeKey: 'CONDA' },
  { id: 'docker', apiFormat: 'docker', label: FORMATS.docker.label, routeKey: 'DOCKER' },
  { id: 'gitlfs', apiFormat: 'gitlfs', label: FORMATS.gitlfs.label, routeKey: 'GITLFS' },
  { id: 'go', apiFormat: 'go', label: FORMATS.go.label, routeKey: 'GOLANG' },
  { id: 'helm', apiFormat: 'helm', label: FORMATS.helm.label, routeKey: 'HELM' },
  { id: 'huggingface', apiFormat: 'huggingface', label: FORMATS.huggingface.label, routeKey: 'HUGGING_FACE' },
  { id: 'maven', apiFormat: 'maven2', label: FORMATS.maven.label, routeKey: 'MAVEN' },
  { id: 'npm', apiFormat: 'npm', label: FORMATS.npm.label, routeKey: 'NPM' },
  { id: 'nuget', apiFormat: 'nuget', label: FORMATS.nuget.label, routeKey: 'NUGET' },
  { id: 'p2', apiFormat: 'p2', label: FORMATS.p2.label, routeKey: 'P2' },
  { id: 'pypi', apiFormat: 'pypi', label: FORMATS.pypi.label, routeKey: 'PYPI' },
  { id: 'pub', apiFormat: 'pub', label: FORMATS.pub.label, routeKey: 'PUB' },
  { id: 'r', apiFormat: 'r', label: FORMATS.r.label, routeKey: 'R' },
  { id: 'raw', apiFormat: 'raw', label: FORMATS.raw.label, routeKey: 'RAW' },
  { id: 'rubygems', apiFormat: 'rubygems', label: FORMATS.rubygems.label, routeKey: 'RUBYGEMS' },
  { id: 'swift', apiFormat: 'swift', label: FORMATS.swift.label, routeKey: 'SWIFT' },
  { id: 'terraform', apiFormat: 'terraform', label: FORMATS.terraform.label, routeKey: 'TERRAFORM' },
  { id: 'yum', apiFormat: 'yum', label: FORMATS.yum.label, routeKey: 'YUM' },
];
