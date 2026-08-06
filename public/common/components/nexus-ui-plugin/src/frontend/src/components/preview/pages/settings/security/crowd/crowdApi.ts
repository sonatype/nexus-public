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

import { restClient, parseApiError } from '../../../../../../interface/api';
import { CrowdConfig, DEFAULT_CROWD_CONFIG } from './types';

const CROWD_API_URL = '/service/rest/v1/security/atlassian-crowd';

/**
 * Fetch the Crowd configuration, merged over defaults.
 *
 * Pure (no React) so it can be invoked from the Crowd settings machine.
 */
export async function fetchCrowdConfig(): Promise<CrowdConfig> {
  try {
    const data = await restClient.get<CrowdConfig>(CROWD_API_URL);
    return {
      ...DEFAULT_CROWD_CONFIG,
      ...data,
    };
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    throw new Error(apiError.message || 'Failed to load Crowd configuration');
  }
}

/** Save the Crowd configuration. */
export async function saveCrowdConfig(config: CrowdConfig): Promise<void> {
  try {
    await restClient.put(CROWD_API_URL, config);
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    throw new Error(apiError.message || 'Failed to save Crowd configuration');
  }
}

/** Verify connectivity to the configured Crowd server. */
export async function verifyCrowdConnection(config: CrowdConfig): Promise<void> {
  try {
    await restClient.post(`${CROWD_API_URL}/verify-connection`, config);
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    throw new Error(apiError.message || 'Failed to verify Crowd connection');
  }
}

/** Clear the Crowd authentication cache. */
export async function clearCrowdCache(): Promise<void> {
  try {
    await restClient.post(`${CROWD_API_URL}/clear-cache`);
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    throw new Error(apiError.message || 'Failed to clear Crowd cache');
  }
}
