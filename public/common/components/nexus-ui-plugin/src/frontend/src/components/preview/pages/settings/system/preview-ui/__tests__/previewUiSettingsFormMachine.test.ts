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
import { createPreviewUiSettingsFormMachine, LOCKOUT_ERROR_MESSAGE } from '../previewUiSettingsFormMachine';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: jest.fn(),
    put: jest.fn(),
  },
}));

jest.mock('../../../../../../../constants/APIConstants', () => ({
  APIConstants: {
    REST: {
      INTERNAL: {
        PREVIEW_UI_SETTINGS: '/service/rest/internal/ui/preview-ui-settings',
      },
    },
  },
}));

const { restClient } = jest.requireMock('../../../../../../../interface/api');

const MOCK_SETTINGS = {
  anonymousEnabled: false,
  loggedInEnabled: true,
  defaultToPreviewUi: false,
  disableLegacyUi: false,
  disableSwitchFeedback: false,
};

async function startAndLoad(overrides?: Partial<typeof MOCK_SETTINGS>) {
  restClient.get.mockResolvedValue({ ...MOCK_SETTINGS, ...overrides });
  const machine = createPreviewUiSettingsFormMachine();
  const service = interpret(machine).start();
  await waitFor(service, (state) => state.matches('editing'));
  return service;
}

describe('previewUiSettingsFormMachine', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('loading', () => {
    it('starts in loading state then transitions to editing', async () => {
      restClient.get.mockResolvedValue(MOCK_SETTINGS);
      const machine = createPreviewUiSettingsFormMachine();
      const service = interpret(machine).start();

      expect(service.getSnapshot().matches('loading')).toBe(true);

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().matches('editing')).toBe(true);

      service.stop();
    });

    it('loads settings from REST API', async () => {
      const service = await startAndLoad();
      const state = service.getSnapshot();

      expect(state.context.data.loggedInEnabled).toBe(true);
      expect(state.context.data.anonymousEnabled).toBe(false);
      expect(state.context.data.disableLegacyUi).toBe(false);
      expect(state.context.data.defaultToPreviewUi).toBe(false);
      expect(state.context.data.disableSwitchFeedback).toBe(false);

      service.stop();
    });

    it('transitions to loadError on API failure', async () => {
      restClient.get.mockRejectedValue(new Error('Network error'));
      const machine = createPreviewUiSettingsFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));
      expect(service.getSnapshot().context.loadError).toBeTruthy();

      service.stop();
    });

    it('retries loading on RETRY event', async () => {
      restClient.get.mockRejectedValueOnce(new Error('Network error'));
      restClient.get.mockResolvedValueOnce(MOCK_SETTINGS);

      const machine = createPreviewUiSettingsFormMachine();
      const service = interpret(machine).start();

      await waitFor(service, (state) => state.matches('loadError'));
      service.send({ type: 'RETRY' } as any);

      await waitFor(service, (state) => state.matches('editing'));
      expect(service.getSnapshot().context.data.loggedInEnabled).toBe(true);

      service.stop();
    });
  });

  describe('field updates', () => {
    it('updates boolean fields via UPDATE event', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'anonymousEnabled', value: true } as any);
      expect(service.getSnapshot().context.data.anonymousEnabled).toBe(true);

      service.send({ type: 'UPDATE', name: 'disableLegacyUi', value: true } as any);
      expect(service.getSnapshot().context.data.disableLegacyUi).toBe(true);

      service.send({ type: 'UPDATE', name: 'disableSwitchFeedback', value: true } as any);
      expect(service.getSnapshot().context.data.disableSwitchFeedback).toBe(true);

      service.stop();
    });

    it('tracks dirty state after field update', async () => {
      const service = await startAndLoad();

      expect(service.getSnapshot().context.isPristine).toBe(true);

      service.send({ type: 'UPDATE', name: 'loggedInEnabled', value: false } as any);

      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.stop();
    });

    it('resets to pristine after RESET event', async () => {
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'anonymousEnabled', value: true } as any);
      expect(service.getSnapshot().context.isPristine).toBe(false);

      service.send({ type: 'RESET' } as any);

      expect(service.getSnapshot().context.isPristine).toBe(true);
      expect(service.getSnapshot().context.data.anonymousEnabled).toBe(false);

      service.stop();
    });
  });

  describe('lockout validation', () => {
    it('shouldReportErrorWhenDisableLegacyAndBothAccessDisabled', async () => {
      const service = await startAndLoad({ disableLegacyUi: true, anonymousEnabled: false, loggedInEnabled: false });
      const state = service.getSnapshot();

      expect(state.context.validationErrors.disableLegacyUi).toBe(LOCKOUT_ERROR_MESSAGE);

      service.stop();
    });

    it('shouldReportErrorWhenUserDisablesBothAccessAfterLoad', async () => {
      const service = await startAndLoad({ disableLegacyUi: true, loggedInEnabled: true });

      service.send({ type: 'UPDATE', name: 'loggedInEnabled', value: false } as any);
      const state = service.getSnapshot();

      expect(state.context.validationErrors.disableLegacyUi).toBe(LOCKOUT_ERROR_MESSAGE);

      service.stop();
    });

    it('shouldNotReportErrorWhenAnonymousEnabled', async () => {
      const service = await startAndLoad({ disableLegacyUi: true, anonymousEnabled: true, loggedInEnabled: false });
      const state = service.getSnapshot();

      expect(state.context.validationErrors.disableLegacyUi).toBeUndefined();

      service.stop();
    });

    it('shouldNotReportErrorWhenLoggedInEnabled', async () => {
      const service = await startAndLoad({ disableLegacyUi: true, anonymousEnabled: false, loggedInEnabled: true });
      const state = service.getSnapshot();

      expect(state.context.validationErrors.disableLegacyUi).toBeUndefined();

      service.stop();
    });

    it('shouldNotReportErrorWhenLegacyNotDisabled', async () => {
      const service = await startAndLoad({ disableLegacyUi: false, anonymousEnabled: false, loggedInEnabled: false });
      const state = service.getSnapshot();

      expect(state.context.validationErrors.disableLegacyUi).toBeUndefined();

      service.stop();
    });

    it('shouldPreventSubmitWhenValidationErrorExists', async () => {
      restClient.put.mockResolvedValue(undefined);
      const service = await startAndLoad({ disableLegacyUi: true, anonymousEnabled: false, loggedInEnabled: true });

      service.send({ type: 'UPDATE', name: 'loggedInEnabled', value: false } as any);
      service.send({ type: 'SUBMIT' } as any);

      // Should remain in editing state and never call put
      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(restClient.put).not.toHaveBeenCalled();

      service.stop();
    });

    it('shouldClearErrorWhenUserFixesInvalidCombination', async () => {
      const service = await startAndLoad({ disableLegacyUi: true, anonymousEnabled: false, loggedInEnabled: false });

      expect(service.getSnapshot().context.validationErrors.disableLegacyUi).toBe(LOCKOUT_ERROR_MESSAGE);

      service.send({ type: 'UPDATE', name: 'loggedInEnabled', value: true } as any);

      expect(service.getSnapshot().context.validationErrors.disableLegacyUi).toBeUndefined();

      service.stop();
    });

    it('shouldClearErrorWhenUserDisablesLegacyToggle', async () => {
      const service = await startAndLoad({ disableLegacyUi: true, anonymousEnabled: false, loggedInEnabled: false });

      expect(service.getSnapshot().context.validationErrors.disableLegacyUi).toBe(LOCKOUT_ERROR_MESSAGE);

      service.send({ type: 'UPDATE', name: 'disableLegacyUi', value: false } as any);

      expect(service.getSnapshot().context.validationErrors.disableLegacyUi).toBeUndefined();

      service.stop();
    });
  });

  describe('save flow', () => {
    it('transitions through saving to saved on success', async () => {
      restClient.put.mockResolvedValue(undefined);
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'loggedInEnabled', value: false } as any);
      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) => state.matches('saved'));
      expect(service.getSnapshot().matches('saved')).toBe(true);
      expect(restClient.put).toHaveBeenCalledWith(
        '/service/rest/internal/ui/preview-ui-settings',
        expect.objectContaining({ loggedInEnabled: false })
      );

      service.stop();
    });

    it('returns to editing with saveError on save failure', async () => {
      restClient.put.mockRejectedValue(new Error('Save failed'));
      const service = await startAndLoad();

      service.send({ type: 'UPDATE', name: 'loggedInEnabled', value: false } as any);
      service.send({ type: 'SUBMIT' } as any);

      await waitFor(service, (state) =>
        state.matches('editing') && state.context.saveError !== null
      );

      const state = service.getSnapshot();
      expect(state.matches('editing')).toBe(true);
      expect(state.context.saveError).toBeTruthy();

      service.stop();
    });
  });
});
