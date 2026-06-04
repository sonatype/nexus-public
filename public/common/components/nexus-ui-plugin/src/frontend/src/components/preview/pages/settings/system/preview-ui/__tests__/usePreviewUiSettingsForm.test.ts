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

import { renderHook } from '@testing-library/react';
import { clearDirtyState } from '../../../../../shared';
import { usePreviewUiSettingsForm } from '../usePreviewUiSettingsForm';
import { PREVIEW_UI_SETTINGS_FORM_ID } from '../previewUiSettingsFormMachine';

jest.mock('../../../../../../../interface/api', () => ({
  restClient: { put: jest.fn() },
}));

jest.mock('../../../../../../../interface/form', () => ({
  createFormMachine: jest.fn(() => ({})),
  useForm: jest.fn(),
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

jest.mock('../../../../../shared', () => ({
  clearDirtyState: jest.fn(),
  useToast: () => ({ error: jest.fn() }),
}));

const PREVIEW_UI_SETTINGS_URL = '/service/rest/internal/ui/preview-ui-settings';

describe('usePreviewUiSettingsForm', () => {
  const mockUseForm = jest.requireMock('../../../../../../../interface/form').useForm as jest.Mock;
  const mockPut = jest.requireMock('../../../../../../../interface/api').restClient.put as jest.Mock;
  const mockClearDirtyState = clearDirtyState as jest.Mock;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('clears dirty state before reloading after a successful save', async () => {
    const reload = jest.fn();
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: {
        ...window.location,
        reload,
      },
    });

    let saveService: ((ctx: { data: Record<string, boolean> }) => Promise<void>) | undefined;
    mockUseForm.mockImplementation((_machine, config) => {
      saveService = config.services.save;
      return {};
    });
    mockPut.mockResolvedValue(undefined);

    renderHook(() => usePreviewUiSettingsForm());

    await saveService?.({
      data: {
        anonymousEnabled: false,
        loggedInEnabled: true,
        defaultToPreviewUi: false,
        disableLegacyUi: false,
      },
    });

    expect(mockPut).toHaveBeenCalledWith(PREVIEW_UI_SETTINGS_URL, {
      anonymousEnabled: false,
      loggedInEnabled: true,
      defaultToPreviewUi: false,
      disableLegacyUi: false,
    });
    expect(mockClearDirtyState).toHaveBeenCalledWith(PREVIEW_UI_SETTINGS_FORM_ID);
    expect(reload).toHaveBeenCalled();
    expect(mockClearDirtyState.mock.invocationCallOrder[0]).toBeLessThan(reload.mock.invocationCallOrder[0]);
  });

  it('does not clear dirty state or reload when save fails', async () => {
    const reload = jest.fn();
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: {
        ...window.location,
        reload,
      },
    });

    let saveService: ((ctx: { data: Record<string, boolean> }) => Promise<void>) | undefined;
    mockUseForm.mockImplementation((_machine, config) => {
      saveService = config.services.save;
      return {};
    });
    mockPut.mockRejectedValue(new Error('save failed'));

    renderHook(() => usePreviewUiSettingsForm());

    await expect(saveService?.({
      data: {
        anonymousEnabled: false,
        loggedInEnabled: true,
        defaultToPreviewUi: false,
        disableLegacyUi: false,
      },
    })).rejects.toThrow('save failed');

    expect(mockClearDirtyState).not.toHaveBeenCalled();
    expect(reload).not.toHaveBeenCalled();
  });
});
