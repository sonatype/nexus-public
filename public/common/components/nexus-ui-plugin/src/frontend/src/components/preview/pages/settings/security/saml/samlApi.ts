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

import { APIConstants } from '../../../../../../constants/APIConstants';
import { restClient, parseApiError } from '../../../../../../interface/api';
import { SamlConfiguration } from './types';

const SAML_URL = APIConstants.REST.PUBLIC.SAML;

/**
 * Fetch the SAML configuration.
 *
 * Returns `null` when the server responds 404 — meaning no configuration
 * exists yet (not an error). Pure (no React) so it can back the form machine's
 * load service.
 */
export async function fetchSamlConfiguration(): Promise<SamlConfiguration | null> {
  try {
    return await restClient.get<SamlConfiguration>(SAML_URL);
  } catch (err: unknown) {
    if ((err as { response?: { status?: number } })?.response?.status === 404) {
      return null;
    }
    const apiError = parseApiError(err);
    throw new Error(apiError.message || 'Failed to load SAML configuration');
  }
}

/** Save the SAML configuration. */
export async function saveSamlConfiguration(config: SamlConfiguration): Promise<void> {
  try {
    await restClient.put(SAML_URL, config);
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    throw new Error(apiError.message || 'Failed to save SAML configuration');
  }
}

/** Delete the SAML configuration. */
export async function deleteSamlConfiguration(): Promise<void> {
  try {
    await restClient.delete(SAML_URL);
  } catch (err: unknown) {
    const apiError = parseApiError(err);
    throw new Error(apiError.message || 'Failed to delete SAML configuration');
  }
}

/** Relative path to the SAML Service Provider metadata endpoint. */
export function getSamlMetadataUrl(): string {
  return `${SAML_URL}/metadata`;
}
