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
import type { OAuth2Config } from '../types';

const MOCK_OAUTH2_URL = 'service/rest/internal/ui/oauth2';

/** Minimal valid config for tests that only assert on a single field. */
const BASE_CONFIG: OAuth2Config = {
  clientId: 'client-id',
  clientSecret: 'secret',
  idpAuthorizationUrl: 'https://auth.example.com/authorize',
  idpLogoutUrl: 'https://auth.example.com/logout',
  idpTokenUrl: 'https://auth.example.com/token',
  idpJwksUrl: 'https://auth.example.com/jwks',
  idpJwsAlgorithm: 'RS256',
  idpJwks: '',
  usernameClaim: 'sub',
  firstNameClaim: 'given_name',
  lastNameClaim: 'family_name',
  emailClaim: 'email',
  groupsClaim: 'groups',
  exactMatchClaims: '',
  authorizationCustomParams: '',
  tokenRequestCustomParams: '',
  useTrustStore: false,
};

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
      // Values must be strings: the API declares these as Map<String, String>
      // and answers 400 for array/nested values (NEXUS-54266).
      exactMatchClaims: JSON.stringify({ groups: 'admins' }),
      authorizationCustomParams: JSON.stringify({ prompt: 'login' }),
      tokenRequestCustomParams: JSON.stringify({ audience: 'nexus' }),
      useTrustStore: false,
    };

    mockRestClient.put.mockResolvedValue({});

    await saveOAuth2Config(configToSave);

    expect(mockRestClient.put).toHaveBeenCalledWith(
      MOCK_OAUTH2_URL,
      expect.objectContaining({
        clientId: 'client-id',
        exactMatchClaims: { groups: 'admins' },
        authorizationCustomParams: { prompt: 'login' },
        tokenRequestCustomParams: { audience: 'nexus' },
      })
    );
  });

  // NEXUS-54266: useTrustStore must survive a full load/save round-trip. The
  // backend XO field is a primitive boolean defaulting to false, so any payload
  // that omits it silently disables the admin's truststore choice.
  describe('useTrustStore round-trip', () => {
    it('preserves useTrustStore=true from the API on load', async () => {
      mockRestClient.get.mockResolvedValue({
        clientId: 'client-id',
        idpTokenUrl: 'https://auth.example.com/token',
        useTrustStore: true,
      });

      const result = await fetchOAuth2Config();

      expect(result.useTrustStore).toBe(true);
    });

    it('defaults useTrustStore to false when the API omits it', async () => {
      mockRestClient.get.mockResolvedValue({ clientId: 'client-id' });

      const result = await fetchOAuth2Config();

      expect(result.useTrustStore).toBe(false);
    });

    it('sends useTrustStore=true in the save payload', async () => {
      mockRestClient.put.mockResolvedValue({});

      await saveOAuth2Config({ ...BASE_CONFIG, useTrustStore: true });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        MOCK_OAUTH2_URL,
        expect.objectContaining({ useTrustStore: true })
      );
    });

    it('sends useTrustStore=false rather than omitting it', async () => {
      mockRestClient.put.mockResolvedValue({});

      await saveOAuth2Config({ ...BASE_CONFIG, useTrustStore: false });

      const payload = mockRestClient.put.mock.calls[0][1];
      expect(payload).toHaveProperty('useTrustStore', false);
    });
  });

  // NEXUS-54266: parseString used to swallow JSON.parse failures and return {},
  // so a malformed field produced a successful PUT that wiped the stored value.
  describe('malformed JSON is rejected, not coerced', () => {
    it.each([
      ['exactMatchClaims', 'Exact Match Claims'],
      ['authorizationCustomParams', 'Authorization Custom Parameters'],
      ['tokenRequestCustomParams', 'Token Request Custom Parameters'],
    ])('throws for malformed %s and sends no request', async (field, label) => {
      mockRestClient.put.mockResolvedValue({});

      await expect(
        saveOAuth2Config({ ...BASE_CONFIG, [field]: '{"dept": "engineering"' })
      ).rejects.toThrow(`${label} must be a valid JSON object with string values`);

      expect(mockRestClient.put).not.toHaveBeenCalled();
    });

    it('throws for a JSON array rather than sending it', async () => {
      mockRestClient.put.mockResolvedValue({});

      await expect(
        saveOAuth2Config({ ...BASE_CONFIG, exactMatchClaims: '["a"]' })
      ).rejects.toThrow('Exact Match Claims must be a valid JSON object with string values');

      expect(mockRestClient.put).not.toHaveBeenCalled();
    });

    // NEXUS-54266: the API declares these as Map<String, String>.
    it.each([
      ['array value', '{"role": ["admin"]}'],
      ['nested object value', '{"claims": {"role": "admin"}}'],
      ['number value', '{"max_age": 300}'],
      ['boolean value', '{"prompt": true}'],
    ])('throws for a non-string map value (%s) and sends no request', async (_label, value) => {
      mockRestClient.put.mockResolvedValue({});

      await expect(
        saveOAuth2Config({ ...BASE_CONFIG, exactMatchClaims: value })
      ).rejects.toThrow('Exact Match Claims must be a valid JSON object with string values');

      expect(mockRestClient.put).not.toHaveBeenCalled();
    });

    it('sends a flat string map unchanged', async () => {
      mockRestClient.put.mockResolvedValue({});

      await saveOAuth2Config({ ...BASE_CONFIG, exactMatchClaims: '{"role": "admin"}' });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        MOCK_OAUTH2_URL,
        expect.objectContaining({ exactMatchClaims: { role: 'admin' } })
      );
    });

    it('still sends {} for a blank field', async () => {
      mockRestClient.put.mockResolvedValue({});

      await saveOAuth2Config({ ...BASE_CONFIG, exactMatchClaims: '   ' });

      expect(mockRestClient.put).toHaveBeenCalledWith(
        MOCK_OAUTH2_URL,
        expect.objectContaining({ exactMatchClaims: {} })
      );
    });
  });

  it('throws error with parsed message when save fails', async () => {
    mockRestClient.put.mockRejectedValue(new Error('Save failed'));

    await expect(saveOAuth2Config({ ...BASE_CONFIG })).rejects.toThrow('Save failed');
  });
});
