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

/**
 * Email API Hook
 *
 * Migration Status (AgentDev3):
 * - fetchSettings: ✅ REST (GET /v1/email)
 * - saveSettings: ✅ REST (PUT /v1/email)
 * - sendVerificationEmail: ✅ REST (POST /v1/email/verify)
 *
 * All methods migrated to REST API.
 */

import { useState, useCallback } from 'react';
import { restClient, parseApiError, urlBuilder } from '../../../../../../interface/api';
import { EmailConfiguration, EmailVerificationResult } from './types';

// =============================================================================
// REST API RESPONSE TYPES
// =============================================================================

/**
 * REST API email configuration shape (from ApiEmailConfiguration.java)
 */
interface RestEmailConfiguration {
  enabled: boolean;
  host?: string;
  port?: number;
  username?: string;
  password?: string;
  fromAddress?: string;
  subjectPrefix?: string;
  startTlsEnabled?: boolean;
  startTlsRequired?: boolean;
  sslOnConnectEnabled?: boolean;
  sslServerIdentityCheckEnabled?: boolean;
  nexusTrustStoreEnabled?: boolean;
}

/**
 * REST API email verification response (from ApiEmailValidation.java)
 */
interface RestEmailValidation {
  success: boolean;
  reason?: string;
}

// =============================================================================
// TRANSFORMERS
// =============================================================================

/**
 * Transform REST email configuration to UI type
 */
function restToEmailConfig(rest: RestEmailConfiguration): EmailConfiguration {
  return {
    enabled: rest.enabled ?? false,
    host: rest.host,
    port: rest.port,
    username: rest.username,
    password: rest.password,
    fromAddress: rest.fromAddress,
    subjectPrefix: rest.subjectPrefix,
    startTlsEnabled: rest.startTlsEnabled,
    startTlsRequired: rest.startTlsRequired,
    sslOnConnectEnabled: rest.sslOnConnectEnabled,
    sslServerIdentityCheckEnabled: rest.sslServerIdentityCheckEnabled,
    nexusTrustStoreEnabled: rest.nexusTrustStoreEnabled,
  };
}

/**
 * Transform UI email configuration to REST format
 */
function emailConfigToRest(config: EmailConfiguration): RestEmailConfiguration {
  return {
    enabled: config.enabled ?? false,
    host: config.host,
    port: config.port,
    username: config.username,
    password: config.password,
    fromAddress: config.fromAddress,
    subjectPrefix: config.subjectPrefix,
    startTlsEnabled: config.startTlsEnabled,
    startTlsRequired: config.startTlsRequired,
    sslOnConnectEnabled: config.sslOnConnectEnabled,
    sslServerIdentityCheckEnabled: config.sslServerIdentityCheckEnabled,
    nexusTrustStoreEnabled: config.nexusTrustStoreEnabled,
  };
}

// =============================================================================
// HOOK
// =============================================================================

/**
 * Custom hook for Email server API operations using REST API
 */
export function useEmailApi() {
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Fetch email server configuration using REST API
   */
  const fetchSettings = useCallback(async (): Promise<EmailConfiguration> => {
    try {
      const url = urlBuilder.email.get();
      const response = await restClient.get<RestEmailConfiguration>(url);
      return restToEmailConfig(response);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      console.error('Failed to fetch email settings:', err);
      throw new Error(apiError.message || 'Failed to load email server settings');
    }
  }, []);

  /**
   * Save email server configuration using REST API
   */
  const saveSettings = useCallback(async (settings: EmailConfiguration): Promise<EmailConfiguration> => {
    setLoading(true);
    setError(null);
    try {
      const url = urlBuilder.email.update();
      const payload = emailConfigToRest(settings);
      await restClient.put(url, payload);
      // Fetch the updated settings to return
      const getUrl = urlBuilder.email.get();
      const response = await restClient.get<RestEmailConfiguration>(getUrl);
      return restToEmailConfig(response);
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      setError(apiError.message);
      throw new Error(apiError.message || 'Failed to save email server settings');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Send verification email to test configuration using REST API
   */
  const sendVerificationEmail = useCallback(async (email: string): Promise<EmailVerificationResult> => {
    setVerifying(true);
    setError(null);
    try {
      const url = urlBuilder.email.verify();
      // REST API expects plain text email address in body
      const response = await restClient.post<RestEmailValidation>(url, email, {
        headers: { 'Content-Type': 'text/plain' },
      });
      return {
        success: response.success,
        reason: response.reason,
      };
    } catch (err: unknown) {
      const apiError = parseApiError(err);
      // Verification failures are returned as success=false, not as errors
      return {
        success: false,
        reason: apiError.message || 'Failed to send verification email',
      };
    } finally {
      setVerifying(false);
    }
  }, []);

  return {
    loading,
    verifying,
    error,
    setError,
    fetchSettings,
    saveSettings,
    sendVerificationEmail,
  };
}

export default useEmailApi;
