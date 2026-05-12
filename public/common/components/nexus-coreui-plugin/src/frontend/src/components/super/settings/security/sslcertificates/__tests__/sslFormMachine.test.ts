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
import { createSslFormMachine } from '../sslFormMachine';
import { CERTIFICATE_SOURCES } from '../types';

// Mock the nexus-ui-plugin module
jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    createFormMachine: actual.createFormMachine,
    restClient: {
      get: jest.fn().mockResolvedValue([]),
      post: jest.fn().mockResolvedValue({}),
    },
  };
});

describe('sslFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('initial state', () => {
    it('starts in editing state (no load service)', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      // No load service, so starts directly in editing
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });

    it('defaults to remoteHost source', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      const state = service.getSnapshot();
      expect(state.context.data.source).toBe(CERTIFICATE_SOURCES.REMOTE_HOST);
      expect(state.matches({ editing: CERTIFICATE_SOURCES.REMOTE_HOST })).toBe(true);

      service.stop();
    });

    it('starts with empty form fields', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      const state = service.getSnapshot();
      expect(state.context.data.remoteHostUrl).toBe('');
      expect(state.context.data.pemContent).toBe('');
      expect(state.context.certificateDetails).toBeNull();

      service.stop();
    });
  });

  describe('source variant sub-states', () => {
    it('transitions to remoteHost sub-state on SOURCE_CHANGE', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      // First switch to PEM, then back to remoteHost
      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);
      expect(service.getSnapshot().matches({ editing: CERTIFICATE_SOURCES.PEM })).toBe(true);

      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.REMOTE_HOST } as any);
      expect(service.getSnapshot().matches({ editing: CERTIFICATE_SOURCES.REMOTE_HOST })).toBe(true);

      service.stop();
    });

    it('transitions to PEM sub-state on SOURCE_CHANGE', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);

      const state = service.getSnapshot();
      expect(state.matches({ editing: CERTIFICATE_SOURCES.PEM })).toBe(true);
      expect(state.context.data.source).toBe(CERTIFICATE_SOURCES.PEM);

      service.stop();
    });

    it('clears remoteHostUrl when switching to PEM', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      // Enter a hostname
      service.send({ type: 'UPDATE', name: 'remoteHostUrl', value: 'example.com' } as any);
      expect(service.getSnapshot().context.data.remoteHostUrl).toBe('example.com');

      // Switch to PEM
      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);

      const state = service.getSnapshot();
      expect(state.context.data.remoteHostUrl).toBe('');
      expect(state.context.data.source).toBe(CERTIFICATE_SOURCES.PEM);

      service.stop();
    });

    it('clears pemContent when switching to remoteHost', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      // Switch to PEM and enter content
      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);
      service.send({ type: 'UPDATE', name: 'pemContent', value: '-----BEGIN CERTIFICATE-----' } as any);
      expect(service.getSnapshot().context.data.pemContent).toBe('-----BEGIN CERTIFICATE-----');

      // Switch back to remoteHost
      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.REMOTE_HOST } as any);

      const state = service.getSnapshot();
      expect(state.context.data.pemContent).toBe('');
      expect(state.context.data.source).toBe(CERTIFICATE_SOURCES.REMOTE_HOST);

      service.stop();
    });

    it('resets touched state on source change', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      // Touch a field
      service.send({ type: 'BLUR', name: 'remoteHostUrl' } as any);
      expect(service.getSnapshot().context.touched.remoteHostUrl).toBe(true);

      // Switch source
      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);

      // Touched should be reset
      expect(service.getSnapshot().context.touched).toEqual({});

      service.stop();
    });
  });

  describe('sub-state metadata', () => {
    it('remoteHost sub-state has correct field metadata', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      const state = service.getSnapshot();
      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Load from Server');
      expect(typeMeta.fields).toEqual(['remoteHostUrl']);
      expect(typeMeta.requiredFields).toEqual(['remoteHostUrl']);

      service.stop();
    });

    it('PEM sub-state has correct field metadata', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);

      const state = service.getSnapshot();
      const metaValues = Object.values(state.meta);
      const typeMeta = metaValues.find((m: any) => m?.fields) as any;

      expect(typeMeta).toBeDefined();
      expect(typeMeta.typeLabel).toBe('Paste PEM');
      expect(typeMeta.fields).toEqual(['pemContent']);
      expect(typeMeta.requiredFields).toEqual(['pemContent']);

      service.stop();
    });
  });

  describe('validation per source', () => {
    it('validates remoteHostUrl is required in remoteHost mode', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      // Try to submit with empty remoteHostUrl
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.remoteHostUrl).toBe('Hostname or URL is required');

      service.stop();
    });

    it('validates pemContent is required in PEM mode', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.pemContent).toBe('PEM content is required');

      service.stop();
    });

    it('no remoteHostUrl error in PEM mode', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);
      service.send({ type: 'UPDATE', name: 'pemContent', value: '-----BEGIN CERTIFICATE-----\nfoo\n-----END CERTIFICATE-----' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.remoteHostUrl).toBeUndefined();

      service.stop();
    });

    it('no pemContent error in remoteHost mode', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'UPDATE', name: 'remoteHostUrl', value: 'example.com' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors.pemContent).toBeUndefined();

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates remoteHostUrl field', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'UPDATE', name: 'remoteHostUrl', value: 'example.com:443' } as any);

      expect(service.getSnapshot().context.data.remoteHostUrl).toBe('example.com:443');

      service.stop();
    });

    it('updates pemContent field', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);
      service.send({ type: 'UPDATE', name: 'pemContent', value: 'pem-data' } as any);

      expect(service.getSnapshot().context.data.pemContent).toBe('pem-data');

      service.stop();
    });

    it('tracks dirty state after field update', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'remoteHostUrl', value: 'example.com' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'UPDATE', name: 'remoteHostUrl', value: 'example.com' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.remoteHostUrl).toBe('');

      service.stop();
    });
  });

  describe('save flow', () => {
    it('transitions to saving on valid SUBMIT with remoteHostUrl', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'UPDATE', name: 'remoteHostUrl', value: 'example.com' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      // Should be in validating or saving (depending on timing)
      expect(
        state.matches('saving') || state.matches('validating') || state.matches('saved')
      ).toBe(true);

      service.stop();
    });

    it('transitions to saving on valid SUBMIT with PEM', () => {
      const machine = createSslFormMachine();
      const service = interpret(machine).start();

      service.send({ type: 'SOURCE_CHANGE', value: CERTIFICATE_SOURCES.PEM } as any);
      service.send({ type: 'UPDATE', name: 'pemContent', value: '-----BEGIN CERTIFICATE-----\ndata\n-----END CERTIFICATE-----' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(
        state.matches('saving') || state.matches('validating') || state.matches('saved')
      ).toBe(true);

      service.stop();
    });
  });
});
