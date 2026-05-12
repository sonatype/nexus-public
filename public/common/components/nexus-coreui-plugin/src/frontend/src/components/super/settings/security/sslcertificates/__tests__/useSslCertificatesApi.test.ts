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
import { useSslCertificatesApi } from '../useSslCertificatesApi';
import { SslCertificate } from '../types';

// Mock Axios - Sprint 15: All SSL operations now use REST via Axios
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

describe('useSslCertificatesApi', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('fetchCertificates', () => {
    it('fetches certificates successfully', async () => {
      const mockCertificates: SslCertificate[] = [
        {
          id: 'cert1',
          subjectCommonName: 'example.com',
          subjectOrganization: 'Example Inc',
          issuerOrganization: 'CA Inc',
          fingerprint: 'AA:BB:CC:DD:EE:FF',
        },
      ];

      mockedAxios.get.mockResolvedValue({ data: mockCertificates });

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificates;
      await act(async () => {
        certificates = await result.current.fetchCertificates();
      });

      expect(certificates).toEqual(mockCertificates);
      expect(mockedAxios.get).toHaveBeenCalledWith('/service/rest/v1/security/ssl/truststore');
    });

    it('handles error when fetching certificates fails', async () => {
      mockedAxios.get.mockRejectedValue({
        response: { data: { message: 'Access denied' } },
      });

      const { result } = renderHook(() => useSslCertificatesApi());

      await expect(result.current.fetchCertificates()).rejects.toThrow('Access denied');
    });
  });

  describe('fetchCertificateFromHost', () => {
    it('fetches certificate from remote host successfully via REST', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
      };

      mockedAxios.get.mockResolvedValue({ data: mockCertificate });

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate;
      await act(async () => {
        certificate = await result.current.fetchCertificateFromHost('example.com', 443, 'https');
      });

      expect(certificate).toEqual(mockCertificate);
      expect(mockedAxios.get).toHaveBeenCalledWith(
        expect.stringContaining('/service/rest/v1/security/ssl?host=example.com&port=443&protocolHint=https')
      );
    });

    it('handles error when fetching certificate from host fails', async () => {
      mockedAxios.get.mockRejectedValue({
        response: { data: { message: 'Host unreachable' } },
      });

      const { result } = renderHook(() => useSslCertificatesApi());

      await expect(result.current.fetchCertificateFromHost('invalid.host', 443, 'https'))
        .rejects.toThrow('Host unreachable');
    });

    it('uses generic error message when no specific message provided', async () => {
      mockedAxios.get.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useSslCertificatesApi());

      await expect(result.current.fetchCertificateFromHost('example.com', 443, 'https'))
        .rejects.toThrow('Network error');
    });
  });

  describe('getCertificateDetails', () => {
    it('gets certificate details from PEM content via REST', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
      };

      mockedAxios.post.mockResolvedValue({ data: mockCertificate, status: 201 });

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate;
      await act(async () => {
        certificate = await result.current.getCertificateDetails('-----BEGIN CERTIFICATE-----');
      });

      expect(certificate).toEqual(mockCertificate);
      expect(mockedAxios.post).toHaveBeenCalledWith(
        '/service/rest/v1/security/ssl/truststore',
        '-----BEGIN CERTIFICATE-----',
        expect.objectContaining({
          headers: { 'Content-Type': 'text/plain' },
        })
      );
    });

    it('handles error when parsing certificate fails', async () => {
      mockedAxios.post.mockRejectedValue({
        response: { data: { message: 'Invalid certificate format' } },
      });

      const { result } = renderHook(() => useSslCertificatesApi());

      await expect(result.current.getCertificateDetails('invalid-pem-content'))
        .rejects.toThrow('Invalid certificate format');
    });

    it('uses generic error message when no specific message provided', async () => {
      mockedAxios.post.mockRejectedValue(new Error('Parse error'));

      const { result } = renderHook(() => useSslCertificatesApi());

      await expect(result.current.getCertificateDetails('bad-pem'))
        .rejects.toThrow('Parse error');
    });
  });

  describe('addCertificate', () => {
    it('adds certificate successfully', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
      };

      mockedAxios.post.mockResolvedValue({ data: mockCertificate });

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate;
      await act(async () => {
        certificate = await result.current.addCertificate('-----BEGIN CERTIFICATE-----');
      });

      expect(certificate).toEqual(mockCertificate);
      expect(mockedAxios.post).toHaveBeenCalledWith(
        '/service/rest/v1/security/ssl/truststore',
        '-----BEGIN CERTIFICATE-----',
        { headers: { 'Content-Type': 'text/plain' } }
      );
    });

    it('sets loading and error states', async () => {
      mockedAxios.post.mockRejectedValue({
        response: { data: { message: 'Certificate already exists' } },
      });

      const { result } = renderHook(() => useSslCertificatesApi());

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();

      await act(async () => {
        await expect(
          result.current.addCertificate('-----BEGIN CERTIFICATE-----')
        ).rejects.toThrow('Certificate already exists');
      });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
        expect(result.current.error).toBe('Certificate already exists');
      });
    });
  });

  describe('deleteCertificate', () => {
    it('deletes certificate successfully', async () => {
      mockedAxios.delete.mockResolvedValue({});

      const { result } = renderHook(() => useSslCertificatesApi());

      await act(async () => {
        await result.current.deleteCertificate('cert1');
      });

      expect(mockedAxios.delete).toHaveBeenCalledWith(
        '/service/rest/v1/security/ssl/truststore/cert1'
      );
    });

    it('handles delete error', async () => {
      mockedAxios.delete.mockRejectedValue({
        response: { data: { message: 'Certificate not found' } },
      });

      const { result } = renderHook(() => useSslCertificatesApi());

      await act(async () => {
        await expect(result.current.deleteCertificate('cert1')).rejects.toThrow('Certificate not found');
      });

      await waitFor(() => {
        expect(result.current.error).toBe('Certificate not found');
      });
    });
  });

  describe('loadCertificateDetails', () => {
    it('loads details from remote host via REST', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
      };

      mockedAxios.get.mockResolvedValue({ data: mockCertificate });

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate;
      await act(async () => {
        certificate = await result.current.loadCertificateDetails('remoteHost', 'example.com', '');
      });

      expect(certificate).toEqual(mockCertificate);
    });

    it('loads details from PEM content via REST', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
      };

      mockedAxios.post.mockResolvedValue({ data: mockCertificate, status: 201 });

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate;
      await act(async () => {
        certificate = await result.current.loadCertificateDetails('PEM', '', '-----BEGIN CERTIFICATE-----');
      });

      expect(certificate).toEqual(mockCertificate);
    });

    it('throws error for invalid source type', async () => {
      const { result } = renderHook(() => useSslCertificatesApi());

      await act(async () => {
        await expect(
          result.current.loadCertificateDetails('invalidSource' as any, '', '')
        ).rejects.toThrow('Invalid source or missing data');
      });

      await waitFor(() => {
        expect(result.current.error).toBe('Invalid source or missing data');
      });
    });

    it('sets loading state during load', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
      };

      let resolvePromise: (value: any) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });

      mockedAxios.post.mockReturnValueOnce(promise as any);

      const { result } = renderHook(() => useSslCertificatesApi());

      act(() => {
        result.current.loadCertificateDetails('PEM', '', '-----BEGIN CERTIFICATE-----');
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePromise!({ data: mockCertificate, status: 201 });
      });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });
    });

    it('sets error state on load failure', async () => {
      mockedAxios.post.mockRejectedValue({
        response: { data: { message: 'Certificate expired' } },
      });

      const { result } = renderHook(() => useSslCertificatesApi());

      await act(async () => {
        await expect(
          result.current.loadCertificateDetails('PEM', '', '-----BEGIN CERTIFICATE-----')
        ).rejects.toThrow();
      });

      await waitFor(() => {
        expect(result.current.error).toBeTruthy();
      });
    });
  });

  describe('setError', () => {
    it('allows manual error setting', () => {
      const { result } = renderHook(() => useSslCertificatesApi());

      expect(result.current.error).toBeNull();

      act(() => {
        result.current.setError('Manual error');
      });

      expect(result.current.error).toBe('Manual error');
    });

    it('allows clearing error', async () => {
      mockedAxios.get.mockRejectedValue({
        response: { data: { message: 'API error' } },
      });

      const { result } = renderHook(() => useSslCertificatesApi());

      await expect(result.current.fetchCertificates()).rejects.toThrow();

      act(() => {
        result.current.setError(null);
      });

      expect(result.current.error).toBeNull();
    });
  });

  describe('initial state', () => {
    it('starts with loading false and no error', () => {
      const { result } = renderHook(() => useSslCertificatesApi());

      expect(result.current.loading).toBe(false);
      expect(result.current.error).toBeNull();
    });
  });
});

