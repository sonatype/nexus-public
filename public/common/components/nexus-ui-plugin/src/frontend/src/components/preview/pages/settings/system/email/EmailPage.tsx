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

import React, { useState } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2, Info, ExternalLink, ShieldCheck } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { PageHeader } from '../../../../shared';
import {
  SettingsForm,
  SettingsFormSection,
  SettingsCheckbox,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsButton,
} from '../../../../shared/form';
import SslCertificateDetailsModal from '../../../../../widgets/SslCertificateDetailsModal/SslCertificateDetailsModal';
import { EmailVerify } from './EmailVerify';
import { useEmailForm } from './useEmailForm';
import { useEmailApi } from './useEmailApi';
import { EmailConfiguration, EmailPageProps } from './types';

import './EmailPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

/**
 * EmailPage - Email Server configuration page for Preview UI
 *
 * Configures SMTP settings for outgoing email notifications.
 * Uses XState form machine for state management.
 */
export function EmailPage({ className }: EmailPageProps) {
  // XState form hook handles load, save, dirty tracking, toast, validation
  const form = useEmailForm();

  // Email API for verification only
  const { verifying, sendVerificationEmail } = useEmailApi();

  const isCloud = ExtJS.state?.().getValue?.('isCloud', false) ?? false;
  const canUpdate = ExtJS.checkPermission('nexus:settings:update');
  const formData = form.data as EmailConfiguration;

  const [showCertModal, setShowCertModal] = useState(false);

  const canReadTruststore = ExtJS.checkPermission('nexus:ssl-truststore:read');
  const remoteUrl = formData.host && formData.port ? `https://${formData.host}:${formData.port}` : '';

  if (isCloud) {
    return null;
  }

  // Loading state
  if (form.isLoading) {
    return (
      <Box className={`email-page ${className || ''}`.trim()}>
        <Flex align="center" justify="center" className="email-page__loading">
          <Loader2 size={24} className="email-page__spinner" />
          <Text size="2">Loading email server settings...</Text>
        </Flex>
      </Box>
    );
  }

  // Read-only view for users without update permission
  if (!canUpdate) {
    return (
      <Box className={`email-page ${className || ''}`.trim()}>
        <PageHeader
          title="Email Server"
          description="Configure outgoing email server settings"
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'Email Server' },
          ]}
        />

        <SettingsFormSection title="Current Settings">
          <Box className="email-page__readonly">
            <Flex className="email-page__row">
              <Text size="2" weight="medium" className="email-page__label">Enabled</Text>
              <Text size="2">{formData.enabled ? 'Yes' : 'No'}</Text>
            </Flex>
            <Flex className="email-page__row">
              <Text size="2" weight="medium" className="email-page__label">SMTP Host</Text>
              <Text size="2">{formData.host || 'Not configured'}</Text>
            </Flex>
            <Flex className="email-page__row">
              <Text size="2" weight="medium" className="email-page__label">SMTP Port</Text>
              <Text size="2">{formData.port}</Text>
            </Flex>
            <Flex className="email-page__row">
              <Text size="2" weight="medium" className="email-page__label">From Address</Text>
              <Text size="2">{formData.fromAddress || 'Not configured'}</Text>
            </Flex>
          </Box>
        </SettingsFormSection>
      </Box>
    );
  }

  return (
    <Box
      className={`email-page ${className || ''}`.trim()}
      data-testid="email-page"
      data-loading={form.isSaving ? 'true' : 'false'}
    >
      <PageHeader
        title="Email Server"
        description="Configure outgoing email server settings"
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'Email Server' },
        ]}
      />

      {/* Scrollable content area */}
      <Box className="email-page__content">
        <SettingsForm
          testId="email-form"
          onSubmit={() => form.submit()}
          onCancel={() => form.reset()}
          loading={form.isSaving}
          pristine={form.isPristine}
          cancelDisabled={form.isPristine}
          error={form.saveError || undefined}
          submitAnalyticsId="nxrm-email-save"
        >
          <SettingsFormSection title="SMTP Configuration">
            <SettingsCheckbox
              {...form.checkbox('enabled')}
              label="Enable email server"
              description="Allow the system to send outgoing email notifications"
            />

            <SettingsTextInput
              {...form.field('host')}
              label="SMTP Host"
              helpText="The hostname of your SMTP server"
              placeholder="smtp.example.com"
              required
            />

            <SettingsTextInput
              name="port"
              label="SMTP Port"
              type="number"
              value={formData.port != null ? String(formData.port) : ''}
              // Port needs a numeric value in context but UPDATE event types expect string;
              // a numberField() helper on useForm would remove these casts (NEXUS-52591 follow-up).
              // 0 when cleared so validateEmailConfig's `port === 0` guard marks it as required.
              onChange={(value: string) => form.send({ type: 'UPDATE', name: 'port', value: value ? parseInt(value, 10) : 0 } as any)}
              onBlur={() => form.send({ type: 'BLUR', name: 'port' } as any)}
              error={form.touched?.port ? form.validationErrors?.port : undefined}
              helpText="The port number of your SMTP server (typically 25, 465, or 587)"
              min={1}
              max={65535}
              required
            />

            <SettingsCheckbox
              {...form.checkbox('nexusTrustStoreEnabled')}
              label="Use Nexus Trust Store"
              description="Use certificates from the Nexus Repository trust store for SSL/TLS connections"
            />

            <Box>
              <SettingsButton
                type="button"
                variant="secondary"
                onClick={() => setShowCertModal(true)}
                disabled={!remoteUrl || !canReadTruststore}
                icon={ShieldCheck}
              >
                View Certificate
              </SettingsButton>
            </Box>

            {showCertModal && remoteUrl && (
              <SslCertificateDetailsModal
                remoteUrl={remoteUrl}
                onCancel={() => setShowCertModal(false)}
              />
            )}
          </SettingsFormSection>

          <SettingsFormSection title="Authentication">
            <SettingsCheckbox
              {...form.checkbox('useAuthentication')}
              label="Enable authentication"
              description="Provide credentials for SMTP server authentication"
            />

            {formData.useAuthentication && (
              <>
                <SettingsTextInput
                  {...form.field('username')}
                  label="Username"
                  helpText="Username for SMTP authentication"
                  autoComplete="username"
                  placeholder="nexus-smtp-user"
                />
                <SettingsPasswordInput
                  {...form.field('password')}
                  label="Password"
                  helpText="Password for SMTP authentication"
                  autoComplete="new-password"
                  placeholder={form.isPristine && !formData.password ? 'Saved — enter to change' : undefined}
                />
              </>
            )}
          </SettingsFormSection>

          <SettingsFormSection title="Email Settings">
            <SettingsTextInput
              {...form.field('fromAddress')}
              label="From Address"
              type="email"
              helpText="The email address used as the sender for outgoing emails"
              placeholder="nexus@example.com"
              required
            />

            <SettingsTextInput
              {...form.field('subjectPrefix')}
              label="Subject Prefix"
              helpText="Optional prefix added to all email subject lines"
              placeholder="[Nexus]"
            />
          </SettingsFormSection>

          <SettingsFormSection
            title="SSL/TLS Options"
            collapsible
            defaultCollapsed={!(formData.startTlsEnabled || formData.startTlsRequired || formData.sslOnConnectEnabled || formData.sslCheckServerIdentityEnabled)}
          >
            <SettingsCheckbox
              {...form.checkbox('startTlsEnabled')}
              label="Enable STARTTLS"
              description="Enable STARTTLS support for secure connections"
            />

            <SettingsCheckbox
              {...form.checkbox('startTlsRequired')}
              label="Require STARTTLS"
              description="Require STARTTLS for connections (connection will fail if not supported)"
            />

            <SettingsCheckbox
              {...form.checkbox('sslOnConnectEnabled')}
              label="Enable SSL/TLS on Connect"
              description="Use SSL/TLS encryption immediately on connection (implicit TLS)"
            />

            <SettingsCheckbox
              {...form.checkbox('sslCheckServerIdentityEnabled')}
              label="Server Identity Check"
              description="Verify that the server certificate matches the hostname"
            />
          </SettingsFormSection>

        </SettingsForm>

        {/* Test Email — outside form to prevent Enter-key from triggering SMTP save */}
        <Box className="email-page__form-aligned" data-testid="email-verify-section">
          <SettingsFormSection title="Test Email">
            <EmailVerify
              onSendTest={sendVerificationEmail}
              loading={verifying}
              disabled={!formData.enabled}
            />
          </SettingsFormSection>
        </Box>

        {/* Help Section */}
        <Box className="email-page__form-aligned">
          <Box className="email-page__help">
            <Flex align="center" gap="2" className="email-page__help-header">
              <Info size={16} />
              <Text size="2" weight="medium">About Email Server</Text>
            </Flex>
            <Text size="2" className="email-page__help-text">
              The email server is used to send notifications about system events,
              password reset emails, and other communications.{' '}
              See our{' '}
              <a
                href="https://help.sonatype.com/en/email-server-configuration.html"
                target="_blank"
                rel="noopener noreferrer"
                className="email-page__help-link"
              >
                documentation
                <ExternalLink size={12} />
              </a>
              {' '}for more information.
            </Text>
          </Box>
        </Box>

      </Box>
    </Box>
  );
}

export default EmailPage;
