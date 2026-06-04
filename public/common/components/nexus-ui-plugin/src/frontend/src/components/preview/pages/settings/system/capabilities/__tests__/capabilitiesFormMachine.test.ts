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
import { createCapabilityFormMachine } from '../capabilitiesFormMachine';

// Mock the local API module used by capabilitiesFormMachine
jest.mock('../../../../../../../interface/api', () => ({
  ENDPOINTS: {
    CAPABILITIES: '/service/rest/v1/capabilities',
    CAPABILITIES_TYPES: '/service/rest/v1/capabilities/types',
  },
  restClient: {
    get: jest.fn().mockResolvedValue([]),
  },
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

// Mock capability types returned by the API
const MOCK_CAPABILITY_TYPES = [
  {
    id: 'healthcheck',
    name: 'Health Check',
    about: 'Analyzes repository components for known vulnerabilities',
    formFields: [
      {
        id: 'enabled',
        type: 'boolean',
        label: 'Enable Health Check',
        helpText: 'Enable health check analysis',
        required: false,
        initialValue: true,
      },
    ],
  },
  {
    id: 'outreach.management',
    name: 'Outreach Management',
    about: 'Manages outreach content',
    formFields: [
      {
        id: 'enabled',
        type: 'boolean',
        label: 'Show outreach content',
        required: false,
        initialValue: true,
      },
    ],
  },
  {
    id: 'firewall.audit',
    name: 'Firewall Audit',
    about: 'Audits repository components against IQ Server policies',
    formFields: [
      {
        id: 'iqUrl',
        type: 'url',
        label: 'IQ Server URL',
        helpText: 'URL of the IQ Server',
        required: true,
        initialValue: null,
      },
      {
        id: 'username',
        type: 'string',
        label: 'Username',
        helpText: 'IQ Server username',
        required: true,
        initialValue: null,
      },
      {
        id: 'password',
        type: 'password',
        label: 'Password',
        helpText: 'IQ Server password',
        required: true,
        initialValue: null,
      },
      {
        id: 'maxRetries',
        type: 'number',
        label: 'Max Retries',
        helpText: 'Maximum number of retries',
        required: false,
        initialValue: 3,
        minValue: 0,
        maxValue: 10,
      },
    ],
  },
];

/**
 * Helper: start a machine and wait for it to reach the editing state
 */
async function startAndLoad(
  machine: ReturnType<typeof createCapabilityFormMachine>,
) {
  restClient.get.mockImplementation((url: string) => {
    if (url.includes('types')) {
      return Promise.resolve(MOCK_CAPABILITY_TYPES);
    }
    return Promise.resolve([]);
  });

  const service = interpret(machine).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

describe('capabilitiesFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('create mode', () => {
    it('starts in loading state then transitions to editing', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      restClient.get.mockResolvedValue(MOCK_CAPABILITY_TYPES);
      await waitFor(service, (state) => state.matches('editing'));

      expect(service.getSnapshot().matches('editing')).toBe(true);
      service.stop();
    });

    it('defaults to empty typeId in create mode', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.typeId).toBe('');
      expect(state.context.data.enabled).toBe(true);

      service.stop();
    });

    it('loads capability types into context', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      const context = service.getSnapshot().context as any;
      expect(context.capabilityTypes).toHaveLength(3);
      expect(context.capabilityTypes[0].id).toBe('healthcheck');
      expect(context.capabilityTypes[1].id).toBe('outreach.management');
      expect(context.capabilityTypes[2].id).toBe('firewall.audit');

      service.stop();
    });
  });

  describe('capability type changes (CAPABILITY_TYPE_CHANGE)', () => {
    it('updates typeId and selectedCapabilityType', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'healthcheck' } as any);

      const context = service.getSnapshot().context as any;
      expect(context.data.typeId).toBe('healthcheck');
      expect(context.selectedCapabilityType).toBeDefined();
      expect(context.selectedCapabilityType.id).toBe('healthcheck');

      service.stop();
    });

    it('resets properties on type change', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Set type to healthcheck
      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'healthcheck' } as any);
      expect((service.getSnapshot().context as any).data.properties).toHaveProperty('enabled');

      // Switch to firewall.audit
      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'firewall.audit' } as any);

      const ctx = (service.getSnapshot().context as any);
      // Old healthcheck property behavior replaced by firewall.audit fields
      expect(ctx.data.properties).toHaveProperty('iqUrl');
      expect(ctx.data.properties).toHaveProperty('username');
      expect(ctx.data.properties).toHaveProperty('password');
      expect(ctx.data.properties).toHaveProperty('maxRetries');

      service.stop();
    });

    it('initializes properties with default values from type form fields', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'firewall.audit' } as any);

      const context = service.getSnapshot().context as any;
      // maxRetries has initialValue of 3
      expect(context.data.properties.maxRetries).toBe('3');
      // iqUrl has null initialValue, should be empty string
      expect(context.data.properties.iqUrl).toBe('');
      // username has null initialValue, should be empty string
      expect(context.data.properties.username).toBe('');

      service.stop();
    });

    it('initializes boolean properties to string "true" or "false"', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'healthcheck' } as any);

      const context = service.getSnapshot().context as any;
      expect(context.data.properties.enabled).toBe('true');

      service.stop();
    });

    it('sets selectedCapabilityType to null for unknown type', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'nonexistent.type' } as any);

      const context = service.getSnapshot().context as any;
      expect(context.selectedCapabilityType).toBeNull();

      service.stop();
    });
  });

  describe('validation', () => {
    it('validates typeId is required', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors.typeId).toBeTruthy();

      service.stop();
    });

    it('validates required form fields from capability type', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Select firewall.audit which has required fields
      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'firewall.audit' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.validationErrors['properties.iqUrl']).toBeTruthy();
      expect(state.context.validationErrors['properties.username']).toBeTruthy();
      expect(state.context.validationErrors['properties.password']).toBeTruthy();

      service.stop();
    });

    it('does not error on optional fields when empty', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Select healthcheck which has only optional boolean field
      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'healthcheck' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      // No validation errors for optional fields
      expect(state.context.validationErrors['properties.enabled']).toBeFalsy();

      service.stop();
    });

    it('validates number range for number fields', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      // Select firewall.audit and fill required fields
      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'firewall.audit' } as any);
      service.send({ type: 'UPDATE', name: 'properties.iqUrl', value: 'https://iq.example.com' } as any);
      service.send({ type: 'UPDATE', name: 'properties.username', value: 'admin' } as any);
      service.send({ type: 'UPDATE', name: 'properties.password', value: 'secret' } as any);
      // Set maxRetries above max (10)
      service.send({ type: 'UPDATE', name: 'properties.maxRetries', value: '15' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['properties.maxRetries']).toContain('at most 10');

      service.stop();
    });

    it('validates number field is actually a number', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'firewall.audit' } as any);
      service.send({ type: 'UPDATE', name: 'properties.maxRetries', value: 'not-a-number' } as any);
      service.send({ type: 'SUBMIT' } as any);

      const state = service.getSnapshot();
      expect(state.context.validationErrors['properties.maxRetries']).toContain('must be a number');

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates form fields via UPDATE event', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'notes', value: 'Test note' } as any);

      expect(service.getSnapshot().context.data.notes).toBe('Test note');

      service.stop();
    });

    it('updates nested property fields', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'CAPABILITY_TYPE_CHANGE', value: 'firewall.audit' } as any);
      service.send({ type: 'UPDATE', name: 'properties.iqUrl', value: 'https://iq.example.com' } as any);

      expect(service.getSnapshot().context.data.properties.iqUrl).toBe('https://iq.example.com');

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'notes', value: 'Modified' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'notes', value: 'Modified' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);
      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.stop();
    });

    it('updates enabled flag via checkbox', async () => {
      const machine = createCapabilityFormMachine(undefined);
      const service = await startAndLoad(machine);

      service.send({ type: 'UPDATE', name: 'enabled', value: false } as any);

      expect(service.getSnapshot().context.data.enabled).toBe(false);

      service.stop();
    });
  });

  describe('edit mode', () => {
    it('loads capability data in edit mode', async () => {
      const preloadedCapability = {
        id: 'cap-123',
        typeId: 'healthcheck',
        typeName: 'Health Check',
        enabled: true,
        active: true,
        error: false,
        state: 'active' as const,
        notes: 'Test capability',
        properties: { enabled: 'true' },
      };

      const machine = createCapabilityFormMachine('cap-123', preloadedCapability);
      const service = await startAndLoad(machine);

      const state = service.getSnapshot();
      expect(state.context.data.typeId).toBe('healthcheck');
      expect(state.context.data.enabled).toBe(true);
      expect(state.context.data.notes).toBe('Test capability');
      expect(state.context.data.properties.enabled).toBe('true');

      service.stop();
    });

    it('loads selectedCapabilityType in edit mode', async () => {
      const preloadedCapability = {
        id: 'cap-456',
        typeId: 'firewall.audit',
        typeName: 'Firewall Audit',
        enabled: false,
        active: false,
        error: false,
        state: 'disabled' as const,
        notes: '',
        properties: {
          iqUrl: 'https://iq.example.com',
          username: 'admin',
          password: 'secret',
          maxRetries: '5',
        },
      };

      const machine = createCapabilityFormMachine('cap-456', preloadedCapability);
      const service = await startAndLoad(machine);

      const context = service.getSnapshot().context as any;
      expect(context.selectedCapabilityType).toBeDefined();
      expect(context.selectedCapabilityType.id).toBe('firewall.audit');
      expect(context.selectedCapabilityType.formFields).toHaveLength(4);
      expect(context.data.properties.iqUrl).toBe('https://iq.example.com');

      service.stop();
    });

    it('preserves capability id in form data for edit mode', async () => {
      const preloadedCapability = {
        id: 'cap-789',
        typeId: 'outreach.management',
        typeName: 'Outreach Management',
        enabled: true,
        active: true,
        error: false,
        state: 'active' as const,
        properties: { enabled: 'true' },
      };

      const machine = createCapabilityFormMachine('cap-789', preloadedCapability);
      const service = await startAndLoad(machine);

      const context = service.getSnapshot().context as any;
      expect(context.data.id).toBe('cap-789');
      expect(context.capability.id).toBe('cap-789');

      service.stop();
    });
  });
});
