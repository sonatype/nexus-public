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

import { restClient } from '../../../../interface/api';

export interface RepositoryMetricsDetail {
  name: string;
  componentCount?: number;
  assetCount?: number;
  size?: number;
}

interface RepositoryDetailPage {
  pageData: RepositoryMetricsDetail[];
}

const REPOSITORIES_DETAILS_FILTERED_URL = '/service/rest/internal/ui/repositories/details/filtered';

/**
 * Fetches a single repository's metrics via the server-side-filtered /details/filtered endpoint
 * (nameFilter + pageSize=1), rather than /details, whose ?name= param is ignored server-side and
 * fetches every readable repository just to filter one row on the client.
 */
export async function fetchRepositoryMetricsDetail(
  repositoryName: string
): Promise<RepositoryMetricsDetail | undefined> {
  const page = await restClient.get<RepositoryDetailPage>(
    `${REPOSITORIES_DETAILS_FILTERED_URL}?nameFilter=${encodeURIComponent(repositoryName)}&pageSize=1`
  );
  const detail = page?.pageData?.[0];
  if (detail && detail.name !== repositoryName) {
    console.warn(
      `fetchRepositoryMetricsDetail: expected metrics for "${repositoryName}" but /details/filtered returned "${detail.name}" — nameFilter may not be an exact match server-side.`
    );
  }
  return detail;
}
