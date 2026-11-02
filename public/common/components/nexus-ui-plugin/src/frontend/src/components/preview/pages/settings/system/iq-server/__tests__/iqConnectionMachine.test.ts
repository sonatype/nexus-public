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
import { verifyConnection } from '../iqConnectionMachine';
import { DEFAULT_IQ_CONFIGURATION } from '../types';

jest.mock('axios');

const mockGetValue = jest.fn();
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: { state: () => ({ getValue: (...args: any[]) => mockGetValue(...args) }) },
}));

const mockedAxios = Axios as jest.Mocked<typeof Axios>;
const SETTINGS = { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u', password: 'p' };

describe('verifyConnection', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockGetValue.mockReturnValue(false); // default to self-hosted
  });

  it('reports success and surfaces the reason from the response', async () => {
    mockedAxios.post.mockResolvedValueOnce({ data: { reason: 'Connected v1.2.3' } });

    const result = await verifyConnection(SETTINGS);

    expect(result).toEqual({ success: true, reason: 'Connected v1.2.3' });
  });

  it('posts to the self-hosted verify endpoint when not on cloud', async () => {
    mockedAxios.post.mockResolvedValueOnce({ data: {} });

    await verifyConnection(SETTINGS);

    expect(mockedAxios.post).toHaveBeenCalledWith(
      'service/rest/internal/ui/iq/verify-connection',
      SETTINGS,
    );
  });

  it('posts to the cloud verify endpoint when running on cloud', async () => {
    mockGetValue.mockReturnValue(true);
    mockedAxios.post.mockResolvedValueOnce({ data: {} });

    await verifyConnection(SETTINGS);

    expect(mockedAxios.post).toHaveBeenCalledWith(
      'service/rest/v1/iq/test-new-connection',
      SETTINGS,
    );
  });

  it('returns failure with the string body when the request rejects', async () => {
    mockedAxios.post.mockRejectedValueOnce({ response: { data: 'Authentication failed' } });

    const result = await verifyConnection(SETTINGS);

    expect(result).toEqual({ success: false, reason: 'Authentication failed' });
  });

  it('extracts just the message text from a ValidationErrorXO body, not the raw object', async () => {
    mockedAxios.post.mockRejectedValueOnce({
      response: { data: { id: '*', message: 'Error code 401: Invalid credentials. Please try again.' } },
    });

    const result = await verifyConnection(SETTINGS);

    expect(result).toEqual({
      success: false,
      reason: 'Error code 401: Invalid credentials. Please try again.',
    });
  });

  it('falls back to the error message when there is no response body', async () => {
    mockedAxios.post.mockRejectedValueOnce(new Error('Network error'));

    const result = await verifyConnection(SETTINGS);

    expect(result).toEqual({ success: false, reason: 'Network error' });
  });
});
