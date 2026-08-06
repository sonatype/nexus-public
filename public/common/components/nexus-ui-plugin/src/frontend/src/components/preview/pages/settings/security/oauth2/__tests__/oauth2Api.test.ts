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

import { fetchOAuth2Config, saveOAuth2Config } from '../oauth2Api';

const MOCK_OAUTH2_URL = 'service/rest/internal/ui/oauth2';

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

describe('oauth2Api', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fetches OAuth2 config and stringifies JSON fields', async () => {
    const apiConfig = {
      clientId: 'client-id',
      clientSecret: 'secret',
      idpAuthorizationUrl: 'https://auth.example.com/authorize',
      idpLogoutUrl: 'https://auth.example.com/logout',
      idpTokenUrl: 'https://auth.example.com/token',
      idpJwksUrl: 'https://auth.example.com/jwks',
      idpJwsAlgorithm: 'RS256',
      usernameClaim: 'sub',
      exactMatchClaims: { groups: ['admins'] },
      authorizationCustomParams: { prompt: 'login' },
      tokenRequestCustomParams: { audience: 'nexus' },
    };

    mockRestClient.get.mockResolvedValue(apiConfig);

    const config = await fetchOAuth2Config();

    expect(config).toEqual(
      expect.objectContaining({
        clientId: 'client-id',
        idpTokenUrl: 'https://auth.example.com/token',
        exactMatchClaims: JSON.stringify({ groups: ['admins'] }, null, 2),
        authorizationCustomParams: JSON.stringify({ prompt: 'login' }, null, 2),
        tokenRequestCustomParams: JSON.stringify({ audience: 'nexus' }, null, 2),
      })
    );
    expect(mockRestClient.get).toHaveBeenCalledWith(MOCK_OAUTH2_URL);
  });

  it('throws error with parsed message when fetch fails', async () => {
    const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    mockRestClient.get.mockRejectedValue({ response: { data: { message: 'Not authorized' } } });

    await expect(fetchOAuth2Config()).rejects.toThrow('Not authorized');
    errorSpy.mockRestore();
  });

  it('saves OAuth2 config with parsed JSON fields', async () => {
    const configToSave = {
      clientId: 'client-id',
      clientSecret: 'secret',
      idpAuthorizationUrl: 'https://auth.example.com/authorize',
      idpLogoutUrl: '',
      idpTokenUrl: 'https://auth.example.com/token',
      idpJwksUrl: 'https://auth.example.com/jwks',
      idpJwsAlgorithm: 'RS256',
      usernameClaim: 'sub',
      firstNameClaim: '',
      lastNameClaim: '',
      emailClaim: '',
      groupsClaim: '',
      exactMatchClaims: JSON.stringify({ groups: ['admins'] }),
      authorizationCustomParams: JSON.stringify({ prompt: 'login' }),
      tokenRequestCustomParams: JSON.stringify({ audience: 'nexus' }),
    };

    mockRestClient.put.mockResolvedValue({});

    await saveOAuth2Config(configToSave);

    expect(mockRestClient.put).toHaveBeenCalledWith(
      MOCK_OAUTH2_URL,
      expect.objectContaining({
        clientId: 'client-id',
        exactMatchClaims: { groups: ['admins'] },
        authorizationCustomParams: { prompt: 'login' },
        tokenRequestCustomParams: { audience: 'nexus' },
      })
    );
  });

  it('throws error with parsed message when save fails', async () => {
    mockRestClient.put.mockRejectedValue(new Error('Save failed'));

    await expect(
      saveOAuth2Config({
        clientId: '',
        clientSecret: '',
        idpAuthorizationUrl: '',
        idpLogoutUrl: '',
        idpTokenUrl: '',
        idpJwksUrl: '',
        idpJwsAlgorithm: '',
        usernameClaim: '',
        firstNameClaim: '',
        lastNameClaim: '',
        emailClaim: '',
        groupsClaim: '',
        exactMatchClaims: '',
        authorizationCustomParams: '',
        tokenRequestCustomParams: '',
      })
    ).rejects.toThrow('Save failed');
  });
});
