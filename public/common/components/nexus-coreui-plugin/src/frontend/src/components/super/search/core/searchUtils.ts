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

import Axios from 'axios';

/**
 * Shared search utilities for all format searches.
 * Use these utilities to call the /service/rest/v1/search API consistently.
 */

/**
 * Base search API endpoint.
 */
const SEARCH_API_BASE = '/service/rest/v1/search';

/**
 * Raw item from the search API response.
 */
export interface RawSearchItem {
  id: string;
  repository: string;
  format: string;
  group: string | null;
  name: string;
  version: string;
  assets: RawAsset[];
}

/**
 * Raw asset from the search API response.
 */
export interface RawAsset {
  id: string;
  path: string;
  downloadUrl: string;
  checksum?: {
    sha1?: string;
    sha256?: string;
    md5?: string;
  };
  contentType?: string;
  lastModified?: string;
}

/**
 * Raw search API response.
 */
export interface RawSearchResponse {
  items: RawSearchItem[];
  continuationToken?: string;
}

/**
 * Common search parameters supported by all formats.
 */
export interface BaseSearchParams {
  /** Free text query */
  q?: string;
  /** Repository name */
  repository?: string;
  /** Continuation token for pagination */
  continuationToken?: string;
}

/**
 * npm-specific search parameters.
 */
export interface NpmSearchParams extends BaseSearchParams {
  /** npm scope (e.g., @angular) */
  scope?: string;
  /** Package name */
  name?: string;
}

/**
 * NuGet-specific search parameters.
 */
export interface NuGetSearchParams extends BaseSearchParams {
  /** Package ID */
  packageId?: string;
  /** Package version */
  version?: string;
}

/**
 * Docker-specific search parameters.
 */
export interface DockerSearchParams extends BaseSearchParams {
  /** Image name (e.g., nginx) */
  imageName?: string;
  /** Image tag (e.g., latest) */
  imageTag?: string;
}

/**
 * Build query string from params, filtering out undefined values.
 */
function buildQueryString(params: Record<string, string | undefined>): string {
  const queryParams = new URLSearchParams();
  
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      queryParams.set(key, value);
    }
  }
  
  return queryParams.toString();
}

/**
 * Generic search function that calls the API.
 */
async function fetchSearch(queryString: string): Promise<RawSearchResponse> {
  const url = queryString ? `${SEARCH_API_BASE}?${queryString}` : SEARCH_API_BASE;
  const response = await Axios.get<RawSearchResponse>(url);
  return response.data;
}

/**
 * Search for npm packages.
 * 
 * @example
 * const results = await searchNpm({ q: 'react', scope: '@types' });
 */
export async function searchNpm(params: NpmSearchParams): Promise<RawSearchResponse> {
  const queryString = buildQueryString({
    format: 'npm',
    q: params.q,
    'npm.scope': params.scope,
    name: params.name,
    repository: params.repository,
    continuationToken: params.continuationToken,
  });
  
  return fetchSearch(queryString);
}

/**
 * Search for NuGet packages.
 * 
 * @example
 * const results = await searchNuGet({ q: 'Newtonsoft', packageId: 'Newtonsoft.Json' });
 */
export async function searchNuGet(params: NuGetSearchParams): Promise<RawSearchResponse> {
  const queryString = buildQueryString({
    format: 'nuget',
    q: params.q,
    'nuget.id': params.packageId,
    version: params.version,
    repository: params.repository,
    continuationToken: params.continuationToken,
  });
  
  return fetchSearch(queryString);
}

/**
 * Search for Docker images.
 * 
 * @example
 * const results = await searchDocker({ imageName: 'nginx', imageTag: 'latest' });
 */
export async function searchDocker(params: DockerSearchParams): Promise<RawSearchResponse> {
  const queryString = buildQueryString({
    format: 'docker',
    q: params.q,
    'docker.imageName': params.imageName,
    'docker.imageTag': params.imageTag,
    repository: params.repository,
    continuationToken: params.continuationToken,
  });
  
  return fetchSearch(queryString);
}

/**
 * Search for any format (generic search).
 * 
 * @example
 * const results = await searchGeneric({ q: 'commons', format: 'maven2' });
 */
export async function searchGeneric(
  params: BaseSearchParams & { format?: string; group?: string; name?: string; version?: string }
): Promise<RawSearchResponse> {
  const queryString = buildQueryString({
    format: params.format,
    q: params.q,
    group: params.group,
    name: params.name,
    version: params.version,
    repository: params.repository,
    continuationToken: params.continuationToken,
  });
  
  return fetchSearch(queryString);
}

/**
 * Format display configuration.
 */
export const FORMAT_DISPLAY: Record<string, { label: string; color: string }> = {
  maven2: { label: 'Maven', color: 'orange' },
  npm: { label: 'npm', color: 'red' },
  nuget: { label: 'NuGet', color: 'blue' },
  docker: { label: 'Docker', color: 'cyan' },
  pypi: { label: 'PyPI', color: 'yellow' },
  raw: { label: 'Raw', color: 'gray' },
  helm: { label: 'Helm', color: 'indigo' },
  go: { label: 'Go', color: 'teal' },
  rubygems: { label: 'RubyGems', color: 'ruby' },
  apt: { label: 'APT', color: 'grass' },
  yum: { label: 'Yum', color: 'bronze' },
  conda: { label: 'Conda', color: 'green' },
  conan: { label: 'Conan', color: 'sky' },
  r: { label: 'R', color: 'plum' },
  gitlfs: { label: 'Git LFS', color: 'crimson' },
  cocoapods: { label: 'CocoaPods', color: 'tomato' },
};

/**
 * Get display label for a format.
 */
export function getFormatLabel(format: string): string {
  return FORMAT_DISPLAY[format]?.label ?? format;
}

/**
 * Get display color for a format (for badges).
 */
export function getFormatColor(format: string): string {
  return FORMAT_DISPLAY[format]?.color ?? 'gray';
}

/**
 * Build a display name from group and name.
 */
export function buildDisplayName(group: string | null, name: string): string {
  return group ? `${group}:${name}` : name;
}

/**
 * Build a component ID from format, group, name, and version.
 */
export function buildComponentId(
  format: string,
  group: string | null,
  name: string,
  version: string
): string {
  const parts = [format];
  if (group) parts.push(group);
  parts.push(name, version);
  return parts.join(':');
}


