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

import type {
  ComponentVersionsPage,
  ComponentVersionsRequest,
  GAVersion,
} from './search.types';

const VERSIONS_ENDPOINT = '/service/rest/v1/search/versions';

interface RawComponentVersion {
  version: string;
  lastUpdated?: string | null;
  repositories?: string[] | null;
}

interface RawComponentVersionsPage {
  items?: RawComponentVersion[] | null;
  total?: number | null;
  page?: number | null;
  size?: number | null;
}

/**
 * Fetches one page of a component's distinct versions.
 *
 * `request.page` is the API's 0-based page index. Callers driving a 1-based pagination
 * control must convert before calling.
 */
export async function fetchComponentVersions(
  request: ComponentVersionsRequest,
): Promise<ComponentVersionsPage> {
  const params = new URLSearchParams({
    format: request.format,
    name: request.name,
    page: String(request.page),
    size: String(request.size),
    sort: request.sort,
    direction: request.direction,
  });
  if (request.group) {
    params.set('group', request.group);
  }
  if (request.versionFilter) {
    params.set('version', request.versionFilter);
  }

  const response = await Axios.get<RawComponentVersionsPage>(
    `${VERSIONS_ENDPOINT}?${params.toString()}`,
  );
  const data = response.data ?? {};

  const items: GAVersion[] = (data.items ?? []).map((item) => ({
    version: item.version,
    lastUpdated: item.lastUpdated ?? '',
    repositories: item.repositories ?? [],
  }));

  return {
    items,
    total: data.total ?? 0,
    page: data.page ?? request.page,
    size: data.size ?? request.size,
  };
}
