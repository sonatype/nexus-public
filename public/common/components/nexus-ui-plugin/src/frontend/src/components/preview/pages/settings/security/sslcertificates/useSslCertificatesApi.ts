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

import { useState, useCallback } from 'react';
import Axios from 'axios';
import { ExtAPIUtils } from '../../../../../../interface/ExtAPIUtils';
import { APIConstants } from '../../../../../../constants/APIConstants';
import { SslCertificate, CertificateSource, CERTIFICATE_SOURCES } from './types';

const { EXT, REST } = APIConstants;

// The trust store itself is managed over REST v1 (list / create / delete).
const SSL_CERTIFICATES_URL = REST.PUBLIC.SSL_CERTIFICATES;

// Inspecting a certificate BEFORE adding it goes over ExtDirect, matching Classic UI.
// There is deliberately no REST equivalent: the only REST write path is POST
// /v1/security/ssl/truststore, which imports the certificate as a side effect, so it
// cannot be used to preview one. ssl_Certificate.details / .retrieveFromHost are
// read-only (@RequiresPermissions nexus:ssl-truststore:read) and additionally return
// `inTrustStore`, which the REST ApiCertificate DTO does not carry — that flag is what
// drives duplicate detection in the add form (NEXUS-54265).

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
   * Retrieve (without importing) the certificate presented by a remote host.
   * Read-only: ssl_Certificate.retrieveFromHost.
   */
  const fetchCertificateFromHost = useCallback(async (
    host: string,
    port: number | null = null,
    protocolHint: string | null = null
  ): Promise<SslCertificate> => {
    try {
      const response = await ExtAPIUtils.extAPIRequest(EXT.SSL.ACTION, EXT.SSL.METHODS.RETRIEVE_FROM_HOST, {
        data: [host, port, protocolHint],
      });
      ExtAPIUtils.checkForError(response);
      return ExtAPIUtils.extractResult(response);
    } catch (err: any) {
      console.error('Failed to fetch certificate from host:', err);
      throw new Error(err?.response?.data?.message || err?.message || 'Failed to retrieve certificate from host');
    }
  }, []);

  /**
   * Parse a PEM-encoded certificate so it can be previewed before being trusted.
   *
   * Read-only: ssl_Certificate.details. This MUST NOT go through
   * POST /v1/security/ssl/truststore — that endpoint imports the certificate, so using
   * it to "preview" silently added the certificate and made the subsequent add fail with
   * a 409, reporting failure for a certificate that had in fact been trusted
   * (NEXUS-54265).
   */
  const getCertificateDetails = useCallback(async (pemContent: string): Promise<SslCertificate> => {
    try {
      const response = await ExtAPIUtils.extAPIRequest(EXT.SSL.ACTION, EXT.SSL.METHODS.DETAILS, {
        data: [pemContent],
      });
      ExtAPIUtils.checkForError(response);
      return ExtAPIUtils.extractResult(response);
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
