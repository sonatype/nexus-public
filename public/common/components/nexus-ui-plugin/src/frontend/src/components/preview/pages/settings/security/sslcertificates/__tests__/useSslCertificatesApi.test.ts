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

// Axios is mocked at the transport level rather than mocking ExtAPIUtils, so these tests also
// cover the ExtDirect request body the hook produces.
jest.mock('axios');
const mockedAxios = Axios as jest.Mocked<typeof Axios>;

// The trust store is managed over REST; certificate inspection goes over ExtDirect.
const TRUSTSTORE_URL = 'service/rest/v1/security/ssl/truststore';
const EXTDIRECT_URL = 'service/extdirect';

const extDirectOk = (data: unknown) => ({ data: { result: { success: true, data } } });

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
      expect(mockedAxios.get).toHaveBeenCalledWith(TRUSTSTORE_URL);
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
    it('retrieves a certificate from a remote host over ExtDirect', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
        inTrustStore: false,
      };

      mockedAxios.post.mockResolvedValue(extDirectOk(mockCertificate));

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate;
      await act(async () => {
        certificate = await result.current.fetchCertificateFromHost('example.com', 443, 'https');
      });

      expect(certificate).toEqual(mockCertificate);
      expect(mockedAxios.post).toHaveBeenCalledWith(
        EXTDIRECT_URL,
        expect.objectContaining({
          action: 'ssl_Certificate',
          method: 'retrieveFromHost',
          data: ['example.com', 443, 'https'],
        })
      );
    });

    it('surfaces an ExtDirect failure message', async () => {
      mockedAxios.post.mockResolvedValue({
        data: { result: { success: false, message: 'Host unreachable' } },
      });

      const { result } = renderHook(() => useSslCertificatesApi());

      await expect(result.current.fetchCertificateFromHost('invalid.host', 443, 'https'))
        .rejects.toThrow('Host unreachable');
    });

    it('surfaces a transport error', async () => {
      mockedAxios.post.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useSslCertificatesApi());

      await expect(result.current.fetchCertificateFromHost('example.com', 443, 'https'))
        .rejects.toThrow('Network error');
    });
  });

  describe('getCertificateDetails', () => {
    it('parses PEM content over ExtDirect', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
        inTrustStore: false,
      };

      mockedAxios.post.mockResolvedValue(extDirectOk(mockCertificate));

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate;
      await act(async () => {
        certificate = await result.current.getCertificateDetails('-----BEGIN CERTIFICATE-----');
      });

      expect(certificate).toEqual(mockCertificate);
      expect(mockedAxios.post).toHaveBeenCalledWith(
        EXTDIRECT_URL,
        expect.objectContaining({
          action: 'ssl_Certificate',
          method: 'details',
          data: ['-----BEGIN CERTIFICATE-----'],
        })
      );
    });

    // Regression guard (NEXUS-54265): previewing a PEM used to POST to the trust store, which
    // imports the certificate. Every PEM-based add then failed with a 409 even though the
    // certificate had in fact been added. Inspection must stay read-only.
    it('never posts to the trust store while inspecting a certificate', async () => {
      mockedAxios.post.mockResolvedValue(extDirectOk({ id: 'cert1' }));

      const { result } = renderHook(() => useSslCertificatesApi());

      await act(async () => {
        await result.current.getCertificateDetails('-----BEGIN CERTIFICATE-----');
      });

      expect(mockedAxios.post).toHaveBeenCalledTimes(1);
      expect(mockedAxios.post).not.toHaveBeenCalledWith(
        TRUSTSTORE_URL,
        expect.anything(),
        expect.anything()
      );
    });

    // The REST ApiCertificate DTO has no inTrustStore field, so duplicate detection in the add
    // form only works because inspection goes over ExtDirect (CertificateXO).
    it('returns inTrustStore so duplicates can be detected', async () => {
      mockedAxios.post.mockResolvedValue(extDirectOk({ id: 'cert1', inTrustStore: true }));

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate: SslCertificate | undefined;
      await act(async () => {
        certificate = await result.current.getCertificateDetails('-----BEGIN CERTIFICATE-----');
      });

      expect(certificate?.inTrustStore).toBe(true);
    });

    it('surfaces an ExtDirect failure message', async () => {
      mockedAxios.post.mockResolvedValue({
        data: { result: { success: false, message: 'Invalid certificate format' } },
      });

      const { result } = renderHook(() => useSslCertificatesApi());

      await expect(result.current.getCertificateDetails('invalid-pem-content'))
        .rejects.toThrow('Invalid certificate format');
    });

    it('surfaces a transport error', async () => {
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
        TRUSTSTORE_URL,
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
        `${TRUSTSTORE_URL}/cert1`
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
    it('loads details from a remote host', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
      };

      mockedAxios.post.mockResolvedValue(extDirectOk(mockCertificate));

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate;
      await act(async () => {
        certificate = await result.current.loadCertificateDetails('remoteHost', 'example.com', '');
      });

      expect(certificate).toEqual(mockCertificate);
      expect(mockedAxios.post).toHaveBeenCalledWith(
        EXTDIRECT_URL,
        expect.objectContaining({ method: 'retrieveFromHost', data: ['example.com', null, null] })
      );
    });

    it('loads details from PEM content', async () => {
      const mockCertificate: SslCertificate = {
        id: 'cert1',
        subjectCommonName: 'example.com',
        fingerprint: 'AA:BB:CC:DD:EE:FF',
      };

      mockedAxios.post.mockResolvedValue(extDirectOk(mockCertificate));

      const { result } = renderHook(() => useSslCertificatesApi());

      let certificate;
      await act(async () => {
        certificate = await result.current.loadCertificateDetails('PEM', '', '-----BEGIN CERTIFICATE-----');
      });

      expect(certificate).toEqual(mockCertificate);
      expect(mockedAxios.post).toHaveBeenCalledWith(
        EXTDIRECT_URL,
        expect.objectContaining({ method: 'details' })
      );
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
        resolvePromise!(extDirectOk(mockCertificate));
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

