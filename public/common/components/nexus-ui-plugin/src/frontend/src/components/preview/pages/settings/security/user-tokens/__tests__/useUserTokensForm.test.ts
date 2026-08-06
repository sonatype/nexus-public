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

import { renderHook, act, waitFor } from '@testing-library/react';
import { useUserTokensForm } from '../useUserTokensForm';
import * as api from '../useUserTokensApi';

jest.mock('../useUserTokensApi');
jest.mock('../../../../../shared', () => ({ useToast: () => ({ success: jest.fn(), error: jest.fn() }) }));

const fetchSettings = jest.fn();
const saveSettings = jest.fn();
const resetAllTokens = jest.fn();

beforeEach(() => {
  jest.clearAllMocks();
  (api.useUserTokensApi as jest.Mock).mockReturnValue({
    loading: false,
    error: null,
    setError: jest.fn(),
    fetchSettings,
    saveSettings,
    resetAllTokens,
  });
  fetchSettings.mockResolvedValue({
    enabled: true,
    protectContent: true,
    expirationEnabled: false,
    expirationDays: 30,
  });
  saveSettings.mockResolvedValue({});
  resetAllTokens.mockResolvedValue(undefined);
});

it('loads settings and exposes them', async () => {
  const { result } = renderHook(() => useUserTokensForm());
  await waitFor(() => expect(result.current.isLoading).toBe(false));
  expect(result.current.data.enabled).toBe(true);
  expect(result.current.isPristine).toBe(true);
});

it('saves when expiration is unchanged', async () => {
  const { result } = renderHook(() => useUserTokensForm());
  await waitFor(() => expect(result.current.isLoading).toBe(false));
  act(() => result.current.handleChange('protectContent', false));
  act(() => result.current.handleSubmit());
  await waitFor(() => expect(saveSettings).toHaveBeenCalled());
});

it('routes through the expiration warning and saves on confirm', async () => {
  const { result } = renderHook(() => useUserTokensForm());
  await waitFor(() => expect(result.current.isLoading).toBe(false));
  act(() => result.current.handleChange('expirationEnabled', true));
  act(() => result.current.handleChange('expirationDays', 45));
  act(() => result.current.handleSubmit());
  await waitFor(() => expect(result.current.showExpirationWarning).toBe(true));
  expect(saveSettings).not.toHaveBeenCalled();

  act(() => result.current.confirmSave());
  await waitFor(() => expect(saveSettings).toHaveBeenCalled());
});

it('requests, confirms, and completes the reset-all-tokens flow', async () => {
  const { result } = renderHook(() => useUserTokensForm());
  await waitFor(() => expect(result.current.isLoading).toBe(false));

  act(() => result.current.requestReset());
  await waitFor(() => expect(result.current.showResetModal).toBe(true));

  act(() => result.current.setResetConfirmation('Reset all tokens'));
  act(() => result.current.confirmReset());

  await waitFor(() => expect(resetAllTokens).toHaveBeenCalled());
  await waitFor(() => expect(result.current.showResetModal).toBe(false));
});

it('surfaces the load error message', async () => {
  fetchSettings.mockRejectedValueOnce(new Error('Failed to load settings'));
  const { result } = renderHook(() => useUserTokensForm());
  await waitFor(() => expect(result.current.error).toBe('Failed to load settings'));
});
