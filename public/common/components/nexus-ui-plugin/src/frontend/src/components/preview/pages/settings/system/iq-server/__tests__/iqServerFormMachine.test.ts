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

import { interpret } from 'xstate';
import { waitFor } from 'xstate/lib/waitFor';
import Axios from 'axios';
import { createIqServerFormMachine, toFormData, toUpdatePayload } from '../iqServerFormMachine';
import { DEFAULT_IQ_CONFIGURATION, PASSWORD_PLACEHOLDER } from '../types';

jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

const SETTINGS = { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'admin', password: PASSWORD_PLACEHOLDER };

describe('iqServerFormMachine', () => {
  beforeEach(() => jest.clearAllMocks());

  it('loads settings into editing', async () => {
    mockedAxios.get.mockResolvedValue({ data: SETTINGS });
    const service = interpret(createIqServerFormMachine()).start();
    await waitFor(service, (s) => s.matches('editing'));
    expect(service.getSnapshot().context.data.url).toBe('https://iq');
    service.stop();
  });

  it('validates required url when enabled and empty', async () => {
    mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true } });
    const service = interpret(createIqServerFormMachine()).start();
    await waitFor(service, (s) => s.matches('editing'));
    expect(service.getSnapshot().context.validationErrors.url).toMatch(/required/i);
    service.stop();
  });

  it('save PUTs then re-GETs and merges server result into pristine data', async () => {
    mockedAxios.get
      .mockResolvedValueOnce({ data: SETTINGS })                    // initial load
      .mockResolvedValueOnce({ data: { ...SETTINGS, url: 'https://iq-normalized' } }); // post-PUT re-GET
    mockedAxios.put.mockResolvedValue({ data: {} });
    const service = interpret(createIqServerFormMachine()).start();
    await waitFor(service, (s) => s.matches('editing'));
    service.send({ type: 'UPDATE', name: 'username', value: 'newadmin' });
    service.send({ type: 'SUBMIT' });
    await waitFor(service, (s) => s.matches('editing') && s.context.isPristine);
    expect(service.getSnapshot().context.data.url).toBe('https://iq-normalized');
    service.stop();
  });

  describe('properties parsing', () => {
    it('loads a properties string into a parsed row array', async () => {
      mockedAxios.get.mockResolvedValue({ data: { ...SETTINGS, properties: 'proxy.host=proxy.example.com\nproxy.port=8080' } });
      const service = interpret(createIqServerFormMachine()).start();
      await waitFor(service, (s) => s.matches('editing'));
      const { properties } = service.getSnapshot().context.data;
      expect(properties.map((p: { name: string; value: string }) => [p.name, p.value])).toEqual([
        ['proxy.host', 'proxy.example.com'],
        ['proxy.port', '8080'],
      ]);
      service.stop();
    });

    it('counts unparseable lines as dropped on load', async () => {
      mockedAxios.get.mockResolvedValue({ data: { ...SETTINGS, properties: '# a comment\nproxy.host=x' } });
      const service = interpret(createIqServerFormMachine()).start();
      await waitFor(service, (s) => s.matches('editing'));
      expect(service.getSnapshot().context.data.propertiesDroppedLineCount).toBe(1);
      service.stop();
    });

    it('preserves the edited properties array (stable ids) after save, and resets the dropped-line count', async () => {
      const { properties: _omitted, ...settingsWithoutProperties } = SETTINGS;
      mockedAxios.get
        .mockResolvedValueOnce({ data: { ...SETTINGS, properties: '# a comment\nproxy.host=x' } }) // initial load: 1 dropped line
        .mockResolvedValueOnce({ data: settingsWithoutProperties });                                // post-PUT re-GET never echoes properties
      mockedAxios.put.mockResolvedValue({ data: {} });
      const service = interpret(createIqServerFormMachine()).start();
      await waitFor(service, (s) => s.matches('editing'));
      const beforeSave = service.getSnapshot().context.data.properties;
      expect(beforeSave).toHaveLength(1);
      const idBeforeSave = beforeSave[0].id;

      service.send({ type: 'SUBMIT' });
      await waitFor(service, (s) => s.matches('editing') && s.context.isPristine);

      const afterSave = service.getSnapshot().context.data;
      expect(afterSave.properties).toHaveLength(1);
      expect(afterSave.properties[0].id).toBe(idBeforeSave);
      expect(afterSave.properties[0]).toMatchObject({ name: 'proxy.host', value: 'x' });
      expect(afterSave.propertiesDroppedLineCount).toBe(0);
      service.stop();
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
