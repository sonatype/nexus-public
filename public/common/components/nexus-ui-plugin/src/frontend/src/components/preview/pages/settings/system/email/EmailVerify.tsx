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

import React, { useState, useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Send, Loader2 } from 'lucide-react';

import { SettingsTextInput, SettingsButton, SettingsAlert } from '../../../../shared/form';
import { EmailVerificationResult } from './types';

import './EmailVerify.scss';

interface EmailVerifyProps {
  onSendTest: (email: string) => Promise<EmailVerificationResult>;
  loading?: boolean;
  disabled?: boolean;
}

/**
 * EmailVerify - Send test email component
 *
 * Allows users to verify their email configuration by sending a test email.
 */
export function EmailVerify({ onSendTest, loading = false, disabled = false }: EmailVerifyProps) {
  const [email, setEmail] = useState('');
  const [result, setResult] = useState<EmailVerificationResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const isValidEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  const isButtonDisabled = disabled || loading || !isValidEmail;

  const handleSendTest = useCallback(async () => {
    if (!isValidEmail) return;

    setResult(null);
    setError(null);

    try {
      const verificationResult = await onSendTest(email);
      setResult(verificationResult);
    } catch (err: any) {
      setError(err?.message || 'Failed to send test email');
    }
  }, [email, isValidEmail, onSendTest]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !isButtonDisabled) {
      e.preventDefault();
      handleSendTest();
    }
  }, [isButtonDisabled, handleSendTest]);

  const handleEmailChange = useCallback((value: string) => {
    setEmail(value);
    // Clear previous results when email changes
    setResult(null);
    setError(null);
  }, []);

  return (
    <Box className="email-verify">
      <Text size="2" className="email-verify__description">
        Send a test email to verify your SMTP configuration is working correctly.
        Make sure to save your settings before testing.
      </Text>

      <Flex gap="3" align="end" className="email-verify__form">
        <Box className="email-verify__input-wrapper">
          <SettingsTextInput
            name="testEmail"
            label="Test Email Address"
            type="email"
            value={email}
            onChange={handleEmailChange}
            onKeyDown={handleKeyDown}
            placeholder="test@example.com"
            helpText="Enter an email address to receive the test email"
            disabled={disabled}
          />
        </Box>

        <SettingsButton
          type="button"
          variant="secondary"
          onClick={handleSendTest}
          disabled={isButtonDisabled}
          loading={loading}
          className="email-verify__button"
          icon={Send}
          data-analytics-id="nxrm-email-test"
        >
          Send Test
        </SettingsButton>
      </Flex>

      {/* Results */}
      {result && (
        <Box className="email-verify__result">
          {result.success ? (
            <SettingsAlert type="success">
              <Text>Test email sent successfully! Check your inbox.</Text>
            </SettingsAlert>
          ) : (
            <SettingsAlert type="error">
              <Text>Failed to send test email: {result.reason || 'Unable to verify. Check email configuration and try again.'}</Text>
            </SettingsAlert>
          )}
        </Box>
      )}

      {error && (
        <Box className="email-verify__result">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}
    </Box>
  );
}

export default EmailVerify;
