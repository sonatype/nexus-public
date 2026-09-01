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
import Axios from 'axios';
import { useIqServerForm } from '../useIqServerForm';
import { DEFAULT_IQ_CONFIGURATION, PASSWORD_PLACEHOLDER } from '../types';

jest.mock('axios');
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: { checkPermission: () => true, state: () => ({ getValue: () => false }) },
}));
jest.mock('../../../../../shared', () => ({ useToast: () => ({ success: jest.fn(), error: jest.fn() }) }));
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('useIqServerForm', () => {
  beforeEach(() => jest.clearAllMocks());

  it('auto-tests on load when enabled with a url, reaching connected', async () => {
    mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u' } });
    mockedAxios.post.mockResolvedValueOnce({ data: { reason: 'Connected v1.0' } }).mockResolvedValueOnce({ data: { hasFirewall: true } });
    const { result } = renderHook(() => useIqServerForm());
    await waitFor(() => expect(result.current.connectionStatus).toBe('connected'));
    expect(result.current.capabilities.hasFirewall).toBe(true);
  });

  it('a field change resets the connection to idle', async () => {
    mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u' } });
    mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
    const { result } = renderHook(() => useIqServerForm());
    await waitFor(() => expect(result.current.connectionStatus).toBe('connected'));
    act(() => result.current.handleFieldChange('username', 'changed'));
    await waitFor(() => expect(result.current.connectionStatus).toBe('idle'));
  });

  describe('save-completion detection', () => {
    it('sends SAVED to connection machine after successful save WITHOUT test/confirm flow', async () => {
      const loadedConfig = {
        ...DEFAULT_IQ_CONFIGURATION,
        enabled: false,
        url: 'https://iq',
        authenticationType: 'PKI',
        showLink: false,
      };
      mockedAxios.get
        .mockResolvedValueOnce({ data: loadedConfig })
        .mockResolvedValueOnce({ data: { ...loadedConfig, showLink: true } });
      mockedAxios.put.mockResolvedValue({});
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.connectionStatus).toBe('idle');
      act(() => result.current.handleFieldChange('showLink', true));
      await waitFor(() => expect(result.current.isPristine).toBe(false));
      expect(result.current.connectionStatus).toBe('idle');
      act(() => result.current.submit());
      await waitFor(() => expect(result.current.isSaving).toBe(true));
      await waitFor(() => expect(result.current.isSaving).toBe(false));
      await waitFor(() => expect(result.current.isPristine).toBe(true));
      expect(result.current.connectionStatus).toBe('idle');
      expect(result.current.connectionMessage).toBe('Saved. Click "Test Connection" to verify.');
    });

    it('sends SAVED to connection machine after a successful save following verify', async () => {
      const loadedConfig = {
        ...DEFAULT_IQ_CONFIGURATION,
        enabled: true,
        url: 'https://iq',
        authenticationType: 'USER',
        username: 'u',
        password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      };
      mockedAxios.get
        .mockResolvedValueOnce({ data: loadedConfig })
        .mockResolvedValueOnce({ data: { ...loadedConfig, username: 'newuser' } });
      mockedAxios.put.mockResolvedValue({});
      mockedAxios.post.mockResolvedValueOnce({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.connectionStatus).toBe('connected'));
      act(() => result.current.handleFieldChange('username', 'newuser'));
      act(() => result.current.handleFieldChange('password', 'newpass'));
      await waitFor(() => expect(result.current.isPristine).toBe(false));
      act(() => result.current.verify());
      await waitFor(() => expect(result.current.connectionStatus).toBe('connected'));
      act(() => result.current.submit());
      await waitFor(() => expect(result.current.isSaving).toBe(true));
      await waitFor(() => expect(result.current.isSaving).toBe(false));
      await waitFor(() => expect(result.current.isPristine).toBe(true));
      expect(result.current.connectionStatus).toBe('idle');
      expect(result.current.connectionMessage).toBe('Saved. Click "Test Connection" to verify.');
    });

    it('does NOT send SAVED when save fails, preserving prior connection status', async () => {
      const loadedConfig = {
        ...DEFAULT_IQ_CONFIGURATION,
        enabled: true,
        url: 'https://iq',
        authenticationType: 'USER',
        username: 'u',
        password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      };
      mockedAxios.get.mockResolvedValue({ data: loadedConfig });
      mockedAxios.put.mockRejectedValue(new Error('Save failed'));
      mockedAxios.post.mockResolvedValueOnce({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.connectionStatus).toBe('connected'));
      act(() => result.current.handleFieldChange('username', 'newuser'));
      act(() => result.current.handleFieldChange('password', 'newpass'));
      await waitFor(() => expect(result.current.isPristine).toBe(false));
      act(() => result.current.verify());
      await waitFor(() => expect(result.current.connectionStatus).toBe('connected'));
      act(() => result.current.submit());
      await waitFor(() => expect(result.current.isSaving).toBe(true));
      await waitFor(() => expect(result.current.isSaving).toBe(false));
      await waitFor(() => expect(result.current.saveError).toBeTruthy());
      expect(result.current.isPristine).toBe(false);
      expect(result.current.connectionStatus).toBe('connected');
      expect(result.current.connectionMessage).toBe('Connected to IQ Server');
    });
  });

  describe('canOpenDashboard / dashboardUrl', () => {
    it('stay derived from the persisted config while an unsaved URL edit is pending', async () => {
      mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u' } });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.canOpenDashboard).toBe(true);
      expect(result.current.dashboardUrl).toBe('https://iq');

      act(() => result.current.handleUrlChange('https://unverified-iq'));
      await waitFor(() => expect(result.current.data.url).toBe('https://unverified-iq'));

      expect(result.current.canOpenDashboard).toBe(true);
      expect(result.current.dashboardUrl).toBe('https://iq');
    });
  });

  describe('clearSaveError', () => {
    it('clears saveError without touching pending edits', async () => {
      const loadedConfig = {
        ...DEFAULT_IQ_CONFIGURATION,
        enabled: true,
        url: 'https://iq',
        authenticationType: 'USER',
        username: 'u',
        password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
      };
      mockedAxios.get.mockResolvedValue({ data: loadedConfig });
      mockedAxios.put.mockRejectedValue(new Error('Save failed'));
      mockedAxios.post.mockResolvedValueOnce({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.connectionStatus).toBe('connected'));
      act(() => result.current.handleFieldChange('username', 'newuser'));
      act(() => result.current.submit());
      await waitFor(() => expect(result.current.saveError).toBeTruthy());

      act(() => result.current.clearSaveError());

      expect(result.current.saveError).toBeNull();
      expect(result.current.data.username).toBe('newuser');
      expect(result.current.isPristine).toBe(false);
    });
  });

  describe('handleUrlChange side effects', () => {
    it('clears placeholder password when URL changes from loaded value', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          ...DEFAULT_IQ_CONFIGURATION,
          enabled: true,
          url: 'https://iq',
          authenticationType: 'USER',
          username: 'u',
          password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
        },
      });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.data.password).toBe('#~NXRM~PLACEHOLDER~PASSWORD~#');
      act(() => result.current.handleUrlChange('https://different-iq'));
      await waitFor(() => expect(result.current.data.url).toBe('https://different-iq'));
      expect(result.current.data.password).toBe('');
    });

    it('does NOT clear placeholder password when URL unchanged', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          ...DEFAULT_IQ_CONFIGURATION,
          enabled: true,
          url: 'https://iq',
          authenticationType: 'USER',
          username: 'u',
          password: '#~NXRM~PLACEHOLDER~PASSWORD~#',
        },
      });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      act(() => result.current.handleUrlChange('https://iq'));
      await waitFor(() => expect(result.current.data.url).toBe('https://iq'));
      expect(result.current.data.password).toBe('#~NXRM~PLACEHOLDER~PASSWORD~#');
    });

    it('disables useTrustStoreForUrl when URL changes to non-https', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          ...DEFAULT_IQ_CONFIGURATION,
          enabled: true,
          url: 'https://iq',
          authenticationType: 'USER',
          username: 'u',
          useTrustStoreForUrl: true,
        },
      });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.data.useTrustStoreForUrl).toBe(true);
      act(() => result.current.handleUrlChange('http://insecure-iq'));
      await waitFor(() => expect(result.current.data.url).toBe('http://insecure-iq'));
      expect(result.current.data.useTrustStoreForUrl).toBe(false);
    });

    it('resets connection status on URL change', async () => {
      mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u' } });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.connectionStatus).toBe('connected'));
      act(() => result.current.handleUrlChange('https://new-iq'));
      await waitFor(() => expect(result.current.connectionStatus).toBe('idle'));
    });
  });

  describe('properties editor', () => {
    it('setProperties updates data and marks the form dirty', async () => {
      mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u' } });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));

      act(() => result.current.setProperties([{ id: '1', name: 'proxy.host', value: 'x' }]));

      expect(result.current.data.properties).toEqual([{ id: '1', name: 'proxy.host', value: 'x' }]);
      expect(result.current.isPristine).toBe(false);
    });

    it('blocks submit and shows row errors when a property is invalid', async () => {
      mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u' } });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));

      act(() => result.current.setProperties([{ id: '1', name: '', value: 'x' }]));
      expect(result.current.hasPropertyErrors).toBe(true);
      expect(result.current.showAllValidation).toBe(false);

      act(() => result.current.submit());

      expect(result.current.showAllValidation).toBe(true);
      expect(mockedAxios.put).not.toHaveBeenCalled();
    });

    describe('clear all confirm flow', () => {
      it('request shows the confirm banner, confirm clears properties and hides it', async () => {
        mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u', properties: 'a=1' } });
        mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
        const { result } = renderHook(() => useIqServerForm());
        await waitFor(() => expect(result.current.isLoading).toBe(false));
        expect(result.current.data.properties).toHaveLength(1);

        act(() => result.current.requestClearAllProperties());
        expect(result.current.showClearAllConfirm).toBe(true);

        act(() => result.current.confirmClearAllProperties());
        expect(result.current.showClearAllConfirm).toBe(false);
        expect(result.current.data.properties).toHaveLength(0);
      });

      it('cancel hides the confirm banner without clearing properties', async () => {
        mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u', properties: 'a=1' } });
        mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
        const { result } = renderHook(() => useIqServerForm());
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        act(() => result.current.requestClearAllProperties());
        act(() => result.current.cancelClearAllProperties());

        expect(result.current.showClearAllConfirm).toBe(false);
        expect(result.current.data.properties).toHaveLength(1);
      });
    });

    describe('dropped-line warning', () => {
      it('exposes the dropped-line count and shows the warning after a load with unparseable lines', async () => {
        mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u', properties: '# comment\na=1' } });
        mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
        const { result } = renderHook(() => useIqServerForm());
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        expect(result.current.propertiesDroppedLineCount).toBe(1);
        expect(result.current.showPropertiesDroppedWarning).toBe(true);
      });

      it('dismissing hides the warning without changing the count', async () => {
        mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u', properties: '# comment\na=1' } });
        mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
        const { result } = renderHook(() => useIqServerForm());
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        act(() => result.current.dismissPropertiesDroppedWarning());

        expect(result.current.showPropertiesDroppedWarning).toBe(false);
        expect(result.current.propertiesDroppedLineCount).toBe(1);
      });

      it('does not show the warning when there is nothing to drop', async () => {
        mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u', properties: 'a=1' } });
        mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
        const { result } = renderHook(() => useIqServerForm());
        await waitFor(() => expect(result.current.isLoading).toBe(false));

        expect(result.current.showPropertiesDroppedWarning).toBe(false);
      });
    });

    it('reset clears showAllValidation and showClearAllConfirm local state', async () => {
      mockedAxios.get.mockResolvedValue({ data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u', properties: 'a=1' } });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));

      act(() => result.current.setProperties([{ id: '1', name: '', value: 'x' }]));
      act(() => result.current.submit());
      expect(result.current.showAllValidation).toBe(true);
      act(() => result.current.requestClearAllProperties());
      expect(result.current.showClearAllConfirm).toBe(true);

      act(() => result.current.reset());

      expect(result.current.showAllValidation).toBe(false);
      expect(result.current.showClearAllConfirm).toBe(false);
    });

    it('preserves stable ids through load → edit → save round-trip at the hook layer', async () => {
      const loadedConfig = { ...DEFAULT_IQ_CONFIGURATION, enabled: false, url: 'https://iq', authenticationType: 'USER', username: 'u', password: PASSWORD_PLACEHOLDER, properties: 'a=1' };
      const { properties: _omitted, ...configWithoutProperties } = loadedConfig;
      mockedAxios.get
        .mockResolvedValueOnce({ data: loadedConfig })
        .mockResolvedValueOnce({ data: configWithoutProperties });
      mockedAxios.put.mockResolvedValue({});
      const { result } = renderHook(() => useIqServerForm());

      await waitFor(() => expect(result.current.isLoading).toBe(false));
      expect(result.current.data.properties).toHaveLength(1);
      const idBeforeSave = result.current.data.properties[0].id;

      act(() => result.current.setProperties([{ ...result.current.data.properties[0], value: 'newvalue' }]));
      await waitFor(() => expect(result.current.isPristine).toBe(false));
      expect(result.current.hasPropertyErrors).toBe(false);

      act(() => result.current.submit());

      await waitFor(() => expect(result.current.isSaving).toBe(true));
      await waitFor(() => expect(result.current.isSaving).toBe(false));
      await waitFor(() => expect(result.current.isPristine).toBe(true));

      expect(mockedAxios.put).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({ properties: 'a=newvalue' })
      );
      expect(result.current.data.properties[0].id).toBe(idBeforeSave);
      expect(result.current.data.properties[0].value).toBe('newvalue');
    });
  });

  describe('connection test payload', () => {
    it('sends properties as a serialized wire string (not an array) when auto-testing on load', async () => {
      mockedAxios.get.mockResolvedValue({
        data: {
          ...DEFAULT_IQ_CONFIGURATION,
          enabled: true,
          url: 'https://iq',
          authenticationType: 'USER',
          username: 'u',
          properties: 'ApplicationName=MyApp\nreadOnly=false',
        },
      });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });

      renderHook(() => useIqServerForm());

      await waitFor(() => expect(mockedAxios.post).toHaveBeenCalled());
      const [, body] = mockedAxios.post.mock.calls[0];
      expect(typeof body.properties).toBe('string');
      expect(body.properties).toBe('ApplicationName=MyApp\nreadOnly=false');
    });

    it('sends properties as a serialized wire string (not an array) when manually testing the connection', async () => {
      mockedAxios.get.mockResolvedValue({
        data: { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://iq', authenticationType: 'USER', username: 'u', properties: 'a=1' },
      });
      mockedAxios.post.mockResolvedValue({ data: { reason: 'ok' } });
      const { result } = renderHook(() => useIqServerForm());
      await waitFor(() => expect(result.current.isLoading).toBe(false));
      mockedAxios.post.mockClear();

      act(() => result.current.verify());

      await waitFor(() => expect(mockedAxios.post).toHaveBeenCalled());
      const [, body] = mockedAxios.post.mock.calls[0];
      expect(typeof body.properties).toBe('string');
      expect(body.properties).toBe('a=1');
    });
  });

});
