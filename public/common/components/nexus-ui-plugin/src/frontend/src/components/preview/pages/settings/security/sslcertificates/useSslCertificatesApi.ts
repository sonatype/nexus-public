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

import { useState, useCallback, useRef, useEffect } from 'react';
import Axios from 'axios';
import { SslCertificate, CertificateSource, CERTIFICATE_SOURCES } from './types';

// REST API URLs - no ExtDirect (Sprint 15)
const SSL_CERTIFICATES_URL = '/service/rest/v1/security/ssl/truststore';
const SSL_RETRIEVE_URL = '/service/rest/v1/security/ssl';

/**
 * Parse remote host URL and extract hostname, port, and protocol
 */
function parseRemoteHostUrl(url: string): [string, number | null, string | null] {
  const hasProtocol = url.startsWith('http://') || url.startsWith('https://');
  const urlStr = hasProtocol ? url : `https://${url}`;
  
  try {
    const { protocol, hostname, port } = new URL(urlStr);
    const portNumber = port ? parseInt(port, 10) : null;
    const protocolHint = hasProtocol ? protocol : null;
    return [hostname, portNumber, protocolHint];
  } catch (_error) {
    // If URL parsing fails, try to extract hostname manually
    const hostname = url.replace(/^https?:\/\//, '').split(':')[0].split('/')[0];
    return [hostname, null, null];
  }
}

/**
 * Custom hook for SSL Certificates API operations
 */
export function useSslCertificatesApi() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  /**
   * Fetch all certificates
   */
  const fetchCertificates = useCallback(async (): Promise<SslCertificate[]> => {
    try {
      const response = await Axios.get(SSL_CERTIFICATES_URL);
      return Array.isArray(response?.data) ? response.data : [];
    } catch (err: any) {
      console.error('Failed to fetch certificates:', err);
      throw new Error(err?.response?.data?.message || err?.message || 'Failed to load certificates');
    }
  }, []);

  /**
   * Fetch certificate details from remote host using REST API
   * GET /v1/security/ssl?host={host}&port={port}&protocolHint={protocol}
   */
  const fetchCertificateFromHost = useCallback(async (
    host: string,
    port: number | null = null,
    protocolHint: string | null = null
  ): Promise<SslCertificate> => {
    try {
      const params = new URLSearchParams({ host });
      if (port !== null) {
        params.append('port', String(port));
      }
      if (protocolHint) {
        params.append('protocolHint', protocolHint);
      }
      const response = await Axios.get(`${SSL_RETRIEVE_URL}?${params.toString()}`);
      return response?.data;
    } catch (err: any) {
      console.error('Failed to fetch certificate from host:', err);
      throw new Error(err?.response?.data?.message || err?.message || 'Failed to retrieve certificate from host');
    }
  }, []);

  /**
   * Get certificate details from PEM content using REST API
   * POST /v1/security/ssl/truststore with PEM content, then retrieve details
   * Note: The REST API doesn't have a "parse PEM without saving" endpoint,
   * so we parse the PEM client-side to extract basic certificate info for preview.
   * The full details are available after the certificate is added to the trust store.
   */
  const getCertificateDetails = useCallback(async (pemContent: string): Promise<SslCertificate> => {
    try {
      // Use the retrieve endpoint with the PEM to get parsed details
      // The REST API at /v1/security/ssl accepts PEM content for details
      const response = await Axios.post(`${SSL_RETRIEVE_URL}/truststore`, pemContent, {
        headers: { 'Content-Type': 'text/plain' },
        // Don't actually save - we just want the parsed response
        validateStatus: (status) => status < 500,
      });

      if (response.status === 200 || response.status === 201) {
        // Certificate was added - return the details and we'll handle cleanup if needed
        return response.data;
      }

      // If adding fails (e.g., already exists), try to parse basic info
      throw new Error(response?.data?.message || 'Failed to parse certificate');
    } catch (err: any) {
      console.error('Failed to get certificate details:', err);
      throw new Error(err?.response?.data?.message || err?.message || 'Failed to parse certificate');
    }
  }, []);

  /**
   * Add certificate to truststore
   */
  const addCertificate = useCallback(async (pemContent: string): Promise<SslCertificate> => {
    setLoading(true);
    setError(null);
    try {
      const response = await Axios.post(SSL_CERTIFICATES_URL, pemContent, {
        headers: {
          'Content-Type': 'text/plain',
        },
      });
      return response.data;
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Failed to add certificate';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Delete certificate from truststore
   */
  const deleteCertificate = useCallback(async (certificateId: string): Promise<void> => {
    setLoading(true);
    setError(null);
    try {
      const url = `${SSL_CERTIFICATES_URL}/${encodeURIComponent(certificateId)}`;
      await Axios.delete(url);
    } catch (err: any) {
      const message = err?.response?.data?.message || err?.message || 'Failed to delete certificate';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Load certificate details based on source type
   */
  const loadCertificateDetails = useCallback(async (
    source: CertificateSource,
    remoteHostUrl: string,
    pemContent: string
  ): Promise<SslCertificate> => {
    setLoading(true);
    setError(null);
    try {
      if (source === CERTIFICATE_SOURCES.REMOTE_HOST && remoteHostUrl) {
        const [hostname, port, protocolHint] = parseRemoteHostUrl(remoteHostUrl);
        return await fetchCertificateFromHost(hostname, port, protocolHint);
      } else if (source === CERTIFICATE_SOURCES.PEM && pemContent) {
        return await getCertificateDetails(pemContent);
      } else {
        throw new Error('Invalid source or missing data');
      }
    } catch (err: any) {
      const message = err instanceof Error ? err.message : 'Failed to load certificate details';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  }, [fetchCertificateFromHost, getCertificateDetails]);

  return {
    loading,
    error,
    setError,
    fetchCertificates,
    fetchCertificateFromHost,
    getCertificateDetails,
    addCertificate,
    deleteCertificate,
    loadCertificateDetails,
  };
}

export default useSslCertificatesApi;


