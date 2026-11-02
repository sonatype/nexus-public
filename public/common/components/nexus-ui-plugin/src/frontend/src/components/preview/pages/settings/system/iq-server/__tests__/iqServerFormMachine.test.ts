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
import { fetchIqSettings, toFormData, toUpdatePayload } from '../iqServerFormMachine';
import { DEFAULT_IQ_CONFIGURATION, PASSWORD_PLACEHOLDER } from '../types';

jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

const SETTINGS = { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'admin', password: PASSWORD_PLACEHOLDER };

describe('iqServerFormMachine', () => {
  beforeEach(() => jest.clearAllMocks());

  /**
   * The IQ REST API rejects a null authenticationType with a @NotNull violation
   * ("must not be null", localized), and GET /v1/iq returns an all-null XO while IQ has
   * never been configured. Spreading that response over the defaults replaces the ''
   * default with null, so the loaded form state must be normalized to a usable value.
   */
  it('normalizes a null authenticationType from an unconfigured server to USER', async () => {
    mockedAxios.get.mockResolvedValue({
      data: { enabled: false, url: null, authenticationType: null, username: null, password: null },
    });

    const data = await fetchIqSettings();

    expect(data.authenticationType).toBe('USER');
  });

  it('normalizes null string fields from an unconfigured server to the empty-string defaults', async () => {
    // GET /v1/iq returns an all-null XO on a first-time setup. These nulls must not survive
    // onto the form data, or a later url.trim() (isSecureUrl gate) throws (NEXUS-55151 crash).
    mockedAxios.get.mockResolvedValue({
      data: {
        enabled: false,
        url: null,
        authenticationType: null,
        username: null,
        password: null,
        properties: null,
      },
    });

    const data = await fetchIqSettings();

    expect(data.url).toBe('');
    expect(data.username).toBe('');
    expect(data.password).toBe('');
    expect(data.properties).toEqual([]);
  });

  it('normalizes a blank authenticationType with no username to USER', async () => {
    // The pre-existing coercion only fired when a username was present, which is never
    // true on a first-time setup.
    mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, authenticationType: '' } });

    const data = await fetchIqSettings();

    expect(data.authenticationType).toBe('USER');
  });

  it('does not overwrite an explicit PKI authenticationType', async () => {
    mockedAxios.get.mockResolvedValue({ data: { ...SETTINGS, authenticationType: 'PKI' } });

    const data = await fetchIqSettings();

    expect(data.authenticationType).toBe('PKI');
  });

  describe('properties parsing', () => {
    it('loads a properties string into a parsed row array', async () => {
      mockedAxios.get.mockResolvedValue({ data: { ...SETTINGS, properties: 'proxy.host=proxy.example.com\nproxy.port=8080' } });

      const data = await fetchIqSettings();

      expect(data.properties.map((p) => [p.name, p.value])).toEqual([
        ['proxy.host', 'proxy.example.com'],
        ['proxy.port', '8080'],
      ]);
    });

    it('counts unparseable lines as dropped on load', async () => {
      mockedAxios.get.mockResolvedValue({ data: { ...SETTINGS, properties: '# a comment\nproxy.host=x' } });

      const data = await fetchIqSettings();

      expect(data.propertiesDroppedLineCount).toBe(1);
    });
  });

  it('toFormData parses the wire properties string into rows', () => {
    const formData = toFormData({ ...SETTINGS, properties: 'a=1\nb=2' });
    expect(formData.properties.map((p) => [p.name, p.value])).toEqual([['a', '1'], ['b', '2']]);
    expect(formData.propertiesDroppedLineCount).toBe(0);
  });

  it('toUpdatePayload serializes the properties array back to a string and drops propertiesDroppedLineCount', () => {
    const formData = toFormData({ ...SETTINGS, properties: 'a=1\nb=2' });
    const payload = toUpdatePayload(formData);
    expect(payload.properties).toBe('a=1\nb=2');
    expect(payload).not.toHaveProperty('propertiesDroppedLineCount');
  });
});
