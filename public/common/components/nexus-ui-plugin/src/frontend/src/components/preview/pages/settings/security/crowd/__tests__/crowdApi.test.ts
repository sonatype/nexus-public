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

import { fetchCrowdConfig, saveCrowdConfig, verifyCrowdConnection, clearCrowdCache } from '../crowdApi';

const CROWD_URL = '/service/rest/v1/security/atlassian-crowd';

const mockRestClient = {
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
};

jest.mock('../../../../../../../interface/api', () => ({
  ...jest.requireActual('../../../../../../../interface/api'),
  restClient: {
    get: (...args: unknown[]) => mockRestClient.get(...args),
    post: (...args: unknown[]) => mockRestClient.post(...args),
    put: (...args: unknown[]) => mockRestClient.put(...args),
    delete: (...args: unknown[]) => mockRestClient.delete(...args),
  },
  parseApiError: jest.fn((err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred',
    status: err?.response?.status,
  })),
}));

const CONFIG = {
  enabled: true,
  realmActive: false,
  url: 'http://crowd.example.com',
  useTrustStoreForUrl: false,
  applicationName: 'nexus',
  applicationPassword: 'secret',
  timeout: 30,
};

describe('crowdApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches config merged over defaults', async () => {
    mockRestClient.get.mockResolvedValue({ url: 'http://crowd.example.com', applicationName: 'nexus' });
    const config = await fetchCrowdConfig();
    expect(config).toEqual(
      expect.objectContaining({ url: 'http://crowd.example.com', applicationName: 'nexus', enabled: false })
    );
    expect(mockRestClient.get).toHaveBeenCalledWith(CROWD_URL);
  });

  it('throws parsed message on fetch failure', async () => {
    const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    mockRestClient.get.mockRejectedValue({ response: { data: { message: 'Forbidden' } } });
    await expect(fetchCrowdConfig()).rejects.toThrow('Forbidden');
    errorSpy.mockRestore();
  });

  it('saves config via PUT', async () => {
    mockRestClient.put.mockResolvedValue({});
    await saveCrowdConfig(CONFIG);
    expect(mockRestClient.put).toHaveBeenCalledWith(CROWD_URL, CONFIG);
  });

  it('throws parsed message on save failure', async () => {
    mockRestClient.put.mockRejectedValue(new Error('Save failed'));
    await expect(saveCrowdConfig(CONFIG)).rejects.toThrow('Save failed');
  });

  it('verifies connection via POST', async () => {
    mockRestClient.post.mockResolvedValue({});
    await verifyCrowdConnection(CONFIG);
    expect(mockRestClient.post).toHaveBeenCalledWith(`${CROWD_URL}/verify-connection`, CONFIG);
  });

  it('clears cache via POST', async () => {
    mockRestClient.post.mockResolvedValue({});
    await clearCrowdCache();
    expect(mockRestClient.post).toHaveBeenCalledWith(`${CROWD_URL}/clear-cache`);
  });
});
