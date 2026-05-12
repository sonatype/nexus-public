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

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Box, Flex, Text, Heading, Dialog, TextField } from '@radix-ui/themes';
import {
  CheckCircle2,
  Loader2,
  Play,
  AlertCircle,
  User,
  Lock,
  KeyRound,
  Trash2,
} from 'lucide-react';

import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsCheckbox,
  SettingsSelect,
  SettingsButton,
  SettingsAlert,
  WizardForm,
} from '../../../shared/form';
import { ConfirmDialog } from '../shared/ConfirmDialog';
import { useLdapApi } from './useLdapApi';
import { useLdapForm } from './useLdapForm';
import {
  LdapServer,
  LdapFormData,
  LdapFormErrors,
  LdapFormStep,
  LdapSchemaTemplate,
  LdapUser,
  AUTH_SCHEMES,
} from './types';

import './LdapForm.scss';

interface LdapFormProps {
  server?: LdapServer | null;
  isCreate: boolean;
  onSave: (data: LdapFormData) => Promise<void>;
  onCancel: () => void;
  onDelete?: () => void;
  loading?: boolean;
  error?: string;
}

/**
 * Validate connection step.
 * In edit mode, password is NOT required here because the API doesn't return
 * stored passwords. Password re-entry is handled at save time via a modal.
 */
function validateConnection(data: LdapFormData, isCreate = true): LdapFormErrors {
  const errors: LdapFormErrors = {};

  if (!data.name?.trim()) {
    errors.name = 'Name is required';
  }
  if (!data.host?.trim()) {
    errors.host = 'Hostname is required';
  }
  if (!data.port || data.port < 1 || data.port > 65535) {
    errors.port = 'Port must be between 1 and 65535';
  }
  if (!data.searchBase?.trim()) {
    errors.searchBase = 'Search Base DN is required';
  }
  if (data.authScheme !== 'none') {
    if (!data.authUsername?.trim()) {
      errors.authUsername = 'Username is required for authenticated connection';
    }
    if (isCreate && !data.authPassword) {
      errors.authPassword = 'Password is required for authenticated connection';
    }
  }

  return errors;
}

/**
 * Validate user/group mapping step
 */
function validateUserGroup(data: LdapFormData): LdapFormErrors {
  const errors: LdapFormErrors = {};

  if (!data.userObjectClass?.trim()) {
    errors.userObjectClass = 'User object class is required';
  }
  if (!data.userIdAttribute?.trim()) {
    errors.userIdAttribute = 'User ID attribute is required';
  }
  if (!data.userRealNameAttribute?.trim()) {
    errors.userRealNameAttribute = 'Real name attribute is required';
  }
  if (!data.userEmailAddressAttribute?.trim()) {
    errors.userEmailAddressAttribute = 'Email attribute is required';
  }

  if (data.ldapGroupsAsRoles) {
    if (data.groupType === 'static') {
      if (!data.groupObjectClass?.trim()) {
        errors.groupObjectClass = 'Group object class is required';
      }
      if (!data.groupIdAttribute?.trim()) {
        errors.groupIdAttribute = 'Group ID attribute is required';
      }
      if (!data.groupMemberAttribute?.trim()) {
        errors.groupMemberAttribute = 'Group member attribute is required';
      }
      if (!data.groupMemberFormat?.trim()) {
        errors.groupMemberFormat = 'Group member format is required';
      }
    } else if (data.groupType === 'dynamic') {
      if (!data.userMemberOfAttribute?.trim()) {
        errors.userMemberOfAttribute = 'User member of attribute is required';
      }
    }
  }

  return errors;
}

const WIZARD_STEPS = [
  { id: 'connection', label: 'Connection' },
  { id: 'userGroup', label: 'User & Group' },
];

/**
 * LdapForm - Multi-step wizard form for creating/editing LDAP servers.
 * Uses shared WizardForm component with step indicator and navigation.
 */
export function LdapForm({
  server,
  isCreate,
  onSave,
  onCancel,
  onDelete,
  loading = false,
  error,
}: LdapFormProps) {
  const { verifyConnection, verifyUserMapping, verifyLogin, fetchTemplates, createServer, updateServer } = useLdapApi();

  // Use XState form hook
  const { form, server: loadedServer, applyTemplate: hookApplyTemplate, changeProtocol } = useLdapForm({
    serverId: isCreate ? undefined : server?.id,
    server: server || null,
    onSave,
    onCancel,
    createServer,
    updateServer,
  });

  const formData = form.data as LdapFormData;

  const [currentStep, setCurrentStep] = useState(0);
  const [templates, setTemplates] = useState<LdapSchemaTemplate[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState<string>('');

  // Verification state
  const [verifying, setVerifying] = useState(false);
  const [connectionVerified, setConnectionVerified] = useState(false);
  const [verificationError, setVerificationError] = useState<string | null>(null);
  const [verifiedUsers, setVerifiedUsers] = useState<LdapUser[]>([]);
  const [showUserResults, setShowUserResults] = useState(false);

  // Verify Login modal state
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [loginUsername, setLoginUsername] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [loginResult, setLoginResult] = useState<{ success: boolean; message: string } | null>(null);
  const [loginVerifying, setLoginVerifying] = useState(false);

  // Password re-entry modal state (NEXUS-23184)
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [reenteredPassword, setReenteredPassword] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [pendingSave, setPendingSave] = useState(false);

  // Load templates
  useEffect(() => {
    fetchTemplates()
      .then(setTemplates)
      .catch(console.error);
  }, [fetchTemplates]);

  // Helper to update form field via machine
  const handleChange = useCallback(<K extends keyof LdapFormData>(
    field: K,
    value: LdapFormData[K]
  ) => {
    if (field === 'protocol') {
      changeProtocol(value as string);
    } else {
      form.send({ type: 'UPDATE', name: field as string, value });
    }

    if (field === 'protocol' || field === 'host' || field === 'port' || field === 'searchBase') {
      setConnectionVerified(false);
    }
  }, [form, changeProtocol]);

  // Handle delete with confirmation
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const handleDeleteConfirm = useCallback(() => {
    if (onDelete) {
      onDelete();
    }
    setShowDeleteConfirm(false);
  }, [onDelete]);

  // Check if connection step is valid
  const connectionValid = useMemo(() => {
    const stepErrors = validateConnection(formData, isCreate);
    return Object.keys(stepErrors).length === 0;
  }, [formData, isCreate]);

  // Check if user/group step is valid
  const userGroupValid = useMemo(() => {
    const stepErrors = validateUserGroup(formData);
    return Object.keys(stepErrors).length === 0;
  }, [formData]);

  // Apply template
  const handleApplyTemplate = useCallback((templateName: string) => {
    const template = templates.find((t) => t.name === templateName);
    if (template) {
      hookApplyTemplate(template);
      setSelectedTemplate(templateName);
    }
  }, [templates, hookApplyTemplate]);

  // Verify connection — sends full form data because the REST endpoint
  // validates CreateLdapServerXo (including user mapping fields)
  const handleVerifyConnection = useCallback(async () => {
    const stepErrors = validateConnection(formData, isCreate);
    if (Object.keys(stepErrors).length > 0) {
      return;
    }

    setVerifying(true);
    setVerificationError(null);
    try {
      await verifyConnection(formData, isCreate ? undefined : server?.name);
      setConnectionVerified(true);
    } catch (err: any) {
      setVerificationError(err.message || 'Connection verification failed');
    } finally {
      setVerifying(false);
    }
  }, [formData, verifyConnection]);

  // Verify user mapping
  const handleVerifyUserMapping = useCallback(async () => {
    const stepErrors = validateUserGroup(formData);
    if (Object.keys(stepErrors).length > 0) {
      return;
    }

    setVerifying(true);
    setVerificationError(null);
    try {
      const users = await verifyUserMapping(formData, isCreate ? undefined : server?.name);
      setVerifiedUsers(users);
      setShowUserResults(true);
    } catch (err: any) {
      setVerificationError(err.message || 'User mapping verification failed');
    } finally {
      setVerifying(false);
    }
  }, [formData, verifyUserMapping]);

  // Verify login
  const handleOpenLoginModal = useCallback(() => {
    setLoginUsername('');
    setLoginPassword('');
    setLoginResult(null);
    setShowLoginModal(true);
  }, []);

  const handleVerifyLogin = useCallback(async () => {
    if (!loginUsername || !loginPassword) return;

    setLoginVerifying(true);
    setLoginResult(null);
    try {
      await verifyLogin(formData, loginUsername, loginPassword);
      setLoginResult({ success: true, message: `User "${loginUsername}" authenticated successfully.` });
    } catch (err: any) {
      setLoginResult({ success: false, message: err.message || 'Login verification failed' });
    } finally {
      setLoginVerifying(false);
    }
  }, [formData, loginUsername, loginPassword, verifyLogin]);

  // Handle password re-entry for updates (NEXUS-23184)
  const handlePasswordReentry = useCallback(async () => {
    if (!reenteredPassword) {
      setPasswordError('Password is required');
      return;
    }

    setPasswordError(null);
    setPendingSave(true);
    try {
      form.send({ type: 'UPDATE', name: 'authPassword', value: reenteredPassword });
      // Small delay to let state update, then submit
      setTimeout(() => form.send('SUBMIT'), 50);
      setShowPasswordModal(false);
      setReenteredPassword('');
    } catch (err) {
      // Error handled by parent
    } finally {
      setPendingSave(false);
    }
  }, [form, reenteredPassword]);

  const handleStepChange = useCallback((step: number) => {
    if (step > currentStep) {
      const stepErrors = validateConnection(formData, isCreate);
      if (Object.keys(stepErrors).length > 0) {
        return;
      }
      setVerificationError(null);
      setConnectionVerified(false);
    } else {
      setVerificationError(null);
      setShowUserResults(false);
    }
    setCurrentStep(step);
  }, [currentStep, formData, isCreate]);

  const handleSave = useCallback(() => {
    const allErrors = {
      ...validateConnection(formData, isCreate),
      ...validateUserGroup(formData),
    };

    if (Object.keys(allErrors).length > 0) {
      return;
    }

    // For edit mode with auth scheme requiring password, show password re-entry modal
    if (!isCreate && formData.authScheme !== 'none' && !formData.authPassword) {
      setReenteredPassword('');
      setPasswordError(null);
      setShowPasswordModal(true);
      return;
    }

    form.send('SUBMIT');
  }, [formData, isCreate, form]);

  // Show loading state
  if (form.isLoading) {
    return (
      <Flex align="center" justify="center" gap="2" p="4">
        <Loader2 size={24} />
        <Text size="2">Loading form...</Text>
      </Flex>
    );
  }

  const isStep1 = currentStep === 0;

  return (
    <Box
      className="ldap-form"
      data-testid="ldap-form"
      data-loading={loading || form.isSaving ? 'true' : 'false'}
      data-dirty={!form.isPristine ? 'true' : 'false'}
      data-mode={isCreate ? 'create' : 'edit'}
      data-step={isStep1 ? 'connection' : 'userGroup'}
    >
      <WizardForm
        steps={WIZARD_STEPS}
        currentStep={currentStep}
        onStepChange={handleStepChange}
        onComplete={handleSave}
        onCancel={onCancel}
        completeLabel={isCreate ? 'Create' : 'Save'}
        canAdvance={isStep1 ? connectionValid : (connectionValid && userGroupValid)}
        dirty={!form.isPristine}
        loading={form.isSaving || loading}
        error={error}
        noDirtyTracking={form.isPristine}
        testId="ldap-wizard-form"
        footerExtra={
          !isCreate && onDelete ? (
            <SettingsButton 
              variant="danger" 
              icon={Trash2}
              onClick={() => setShowDeleteConfirm(true)} 
              disabled={loading || form.isSaving}
              testId="form-delete"
            >
              Delete
            </SettingsButton>
          ) : undefined
        }
      >
        {/* Step 1: Connection */}
        {isStep1 && (<>
          <SettingsFormSection title="Server Configuration" defaultOpen>
            <SettingsTextInput
              {...form.field('name')}
              label="Name"
              helpText="A unique name for this LDAP server"
              required
            />

            <Flex gap="3">
              <Box style={{ flex: 1 }}>
                <SettingsSelect
                  name="protocol"
                  label="Protocol"
                  value={(formData.protocol || 'ldap').toLowerCase()}
                  onChange={(value) => handleChange('protocol', value as 'ldap' | 'ldaps')}
                  helpText="Connection protocol. Use ldaps for encrypted connections."
                  required
                  options={[
                    { value: 'ldap', label: 'ldap' },
                    { value: 'ldaps', label: 'ldaps (SSL)' },
                  ]}
                />
              </Box>
              <Box style={{ flex: 2 }}>
                <SettingsTextInput
                  {...form.field('host')}
                  label="Hostname"
                  placeholder="ldap.example.com"
                  helpText="LDAP server hostname or IP address"
                  required
                />
              </Box>
              <Box style={{ width: 100 }}>
                <SettingsTextInput
                  name="port"
                  label="Port"
                  type="number"
                  value={formData.port || 389}
                  onChange={(value) => handleChange('port', parseInt(value as any, 10) || 389)}
                  helpText="Server port. Default: 389 (ldap) or 636 (ldaps)"
                  required
                />
              </Box>
            </Flex>

            {formData.protocol === 'ldaps' && (
              <SettingsCheckbox
                name="useTrustStore"
                label="Use Nexus SSL Trust Store"
                checked={formData.useTrustStore || false}
                onChange={(checked) => handleChange('useTrustStore', checked)}
                description="Use the Nexus trust store for SSL certificate validation"
              />
            )}

            <SettingsTextInput
              {...form.field('searchBase')}
              label="Search Base DN"
              placeholder="dc=example,dc=com"
              helpText="The base DN to search for users"
              required
            />
          </SettingsFormSection>

          <SettingsFormSection title="Authentication" defaultOpen>
            <SettingsSelect
              name="authScheme"
              label="Authentication Method"
              value={formData.authScheme || 'simple'}
              onChange={(value) => handleChange('authScheme', value)}
              required
              options={AUTH_SCHEMES.map((scheme) => ({
                value: scheme.value,
                label: scheme.label,
              }))}
            />

            {formData.authScheme !== 'none' && (
              <>
                <SettingsTextInput
                  {...form.field('authUsername')}
                  label="Username"
                  placeholder="cn=admin,dc=example,dc=com"
                  helpText="The DN of the user to bind with for searches"
                  required
                />

                <SettingsPasswordInput
                  {...form.field('authPassword')}
                  label="Password"
                  helpText="Password for the bind user"
                  required
                />

                {(formData.authScheme === 'DIGEST-MD5' || formData.authScheme === 'CRAM-MD5') && (
                  <SettingsTextInput
                    {...form.field('authRealm')}
                    label="SASL Realm"
                    helpText="The SASL realm for DIGEST-MD5/CRAM-MD5 authentication"
                  />
                )}
              </>
            )}
          </SettingsFormSection>

          <SettingsFormSection title="Connection Settings">
            <Flex gap="3">
              <SettingsTextInput
                name="connectionTimeout"
                label="Connection Timeout (seconds)"
                type="number"
                value={formData.connectionTimeout || 30}
                onChange={(value) => handleChange('connectionTimeout', parseInt(value as any, 10) || 30)}
                helpText="1-3600 seconds"
              />

              <SettingsTextInput
                name="connectionRetryDelay"
                label="Retry Delay (seconds)"
                type="number"
                value={formData.connectionRetryDelay || 300}
                onChange={(value) => handleChange('connectionRetryDelay', parseInt(value as any, 10) || 300)}
                helpText="Delay between connection retries"
              />

              <SettingsTextInput
                name="maxIncidentsCount"
                label="Max Incidents"
                type="number"
                value={formData.maxIncidentsCount || 3}
                onChange={(value) => handleChange('maxIncidentsCount', parseInt(value as any, 10) || 3)}
                helpText="Max failures before blacklisting"
              />
            </Flex>
          </SettingsFormSection>

          <Box className="ldap-form__verification">
            <Flex align="center" gap="3">
              <SettingsButton
                variant="secondary"
                onClick={handleVerifyConnection}
                disabled={verifying || !connectionValid}
                loading={verifying}
                icon={Play}
              >
                Verify Connection
              </SettingsButton>

              {connectionVerified && (
                <Flex align="center" gap="2" className="ldap-form__success">
                  <CheckCircle2 size={16} />
                  <Text size="2">Connection successful</Text>
                </Flex>
              )}
            </Flex>

            {verificationError && (
              <Box className="ldap-form__verification-error">
                <SettingsAlert type="error">{verificationError}</SettingsAlert>
              </Box>
            )}
          </Box>

        </>
        )}

        {/* Step 2: User & Group */}
        {!isStep1 && (
        <>
          {/* Template Selection */}
          {templates.length > 0 && (
            <Box className="ldap-form__template">
              <SettingsSelect
                name="template"
                label="Load Template"
                value={selectedTemplate}
                onChange={handleApplyTemplate}
                helpText="Select a template to pre-fill common LDAP configurations"
                placeholder="-- Select a template --"
                options={templates.map((template) => ({
                  value: template.name,
                  label: template.name,
                }))}
              />
            </Box>
          )}

          <SettingsFormSection title="User Mapping" defaultOpen>
            <SettingsTextInput
              {...form.field('userBaseDn')}
              label="Base DN"
              placeholder="ou=users"
              helpText="Relative DN for user searches (optional)"
            />

            <SettingsCheckbox
              name="userSubtree"
              label="Search Subtree"
              checked={formData.userSubtree || false}
              onChange={(checked) => handleChange('userSubtree', checked)}
              description="Search the entire subtree for users"
            />

            <SettingsTextInput
              {...form.field('userObjectClass')}
              label="Object Class"
              placeholder="inetOrgPerson"
              helpText="LDAP object class for users (e.g., inetOrgPerson, person)"
              required
            />

            <SettingsTextInput
              {...form.field('userLdapFilter')}
              label="LDAP Filter"
              placeholder="(memberOf=cn=users,dc=example,dc=com)"
              helpText="Optional additional filter for users"
            />

            <Flex gap="3">
              <SettingsTextInput
                {...form.field('userIdAttribute')}
                label="User ID Attribute"
                placeholder="uid"
                helpText="Attribute containing the username (e.g., uid, sAMAccountName)"
                required
              />

              <SettingsTextInput
                {...form.field('userRealNameAttribute')}
                label="Real Name Attribute"
                placeholder="cn"
                helpText="Attribute containing the display name (e.g., cn, displayName)"
                required
              />
            </Flex>

            <Flex gap="3">
              <SettingsTextInput
                {...form.field('userEmailAddressAttribute')}
                label="Email Attribute"
                placeholder="mail"
                required
              />

              <SettingsTextInput
                {...form.field('userPasswordAttribute')}
                label="Password Attribute"
                placeholder="userPassword"
                helpText="Optional"
              />
            </Flex>
          </SettingsFormSection>

          <SettingsFormSection title="Group Mapping" defaultOpen>
            <SettingsCheckbox
              name="ldapGroupsAsRoles"
              label="Map LDAP Groups as Roles"
              checked={formData.ldapGroupsAsRoles}
              onChange={(checked) => handleChange('ldapGroupsAsRoles', checked)}
              description="Enable to map LDAP groups to Nexus roles"
            />

            {formData.ldapGroupsAsRoles && (
              <>
                <SettingsSelect
                  name="groupType"
                  label="Group Type"
                  value={formData.groupType || 'static'}
                  onChange={(value) => handleChange('groupType', value as 'static' | 'dynamic')}
                  required
                  options={[
                    { value: 'static', label: 'Static Groups' },
                    { value: 'dynamic', label: 'Dynamic Groups' },
                  ]}
                />

                {formData.groupType === 'static' && (
                  <>
                    <SettingsTextInput
                      {...form.field('groupBaseDn')}
                      label="Group Base DN"
                      placeholder="ou=groups"
                      helpText="Relative DN for group searches"
                    />

                    <SettingsCheckbox
                      name="groupSubtree"
                      label="Search Group Subtree"
                      checked={formData.groupSubtree || false}
                      onChange={(checked) => handleChange('groupSubtree', checked)}
                      description="Search the entire subtree for groups"
                    />

                    <Flex gap="3">
                      <SettingsTextInput
                        {...form.field('groupObjectClass')}
                        label="Group Object Class"
                        placeholder="groupOfUniqueNames"
                        required
                      />

                      <SettingsTextInput
                        {...form.field('groupIdAttribute')}
                        label="Group ID Attribute"
                        placeholder="cn"
                        required
                      />
                    </Flex>

                    <SettingsTextInput
                      {...form.field('groupMemberAttribute')}
                      label="Group Member Attribute"
                      placeholder="uniqueMember"
                      required
                    />

                    <SettingsTextInput
                      {...form.field('groupMemberFormat')}
                      label="Group Member Format"
                      placeholder="uid=${username},ou=people,dc=example,dc=com"
                      helpText="Use ${username} as placeholder"
                      required
                    />
                  </>
                )}

                {formData.groupType === 'dynamic' && (
                  <SettingsTextInput
                    {...form.field('userMemberOfAttribute')}
                    label="User Member Of Attribute"
                    placeholder="memberOf"
                    required
                  />
                )}
              </>
            )}
          </SettingsFormSection>

          {/* User Mapping Verification */}
          <Box className="ldap-form__verification">
            <Flex align="center" gap="3" wrap="wrap">
              <SettingsButton
                variant="secondary"
                onClick={handleVerifyUserMapping}
                disabled={verifying || !userGroupValid}
                loading={verifying}
                data-testid="ldap-verify-user-mapping"
                icon={Play}
              >
                Verify User Mapping
              </SettingsButton>
              <SettingsButton
                variant="secondary"
                onClick={handleOpenLoginModal}
                disabled={verifying || !userGroupValid}
                data-testid="ldap-verify-login-button"
                icon={KeyRound}
              >
                Verify Login
              </SettingsButton>
            </Flex>

            {verificationError && currentStep === 1 && (
              <Box className="ldap-form__verification-error">
                <SettingsAlert type="error">{verificationError}</SettingsAlert>
              </Box>
            )}

            {showUserResults && verifiedUsers.length > 0 && (
              <Box className="ldap-form__user-results">
                <Text size="2" weight="medium" className="ldap-form__user-results-title">
                  Found {verifiedUsers.length} user(s):
                </Text>
                <Box className="ldap-form__user-list">
                  {verifiedUsers.slice(0, 10).map((user, idx) => (
                    <Box key={idx} className="ldap-form__user-item">
                      <Text size="2" weight="medium">{user.username}</Text>
                      {user.realName && <Text size="1" className="ldap-form__user-detail">{user.realName}</Text>}
                      {user.email && <Text size="1" className="ldap-form__user-detail">{user.email}</Text>}
                    </Box>
                  ))}
                  {verifiedUsers.length > 10 && (
                    <Text size="1" className="ldap-form__user-more">
                      ...and {verifiedUsers.length - 10} more
                    </Text>
                  )}
                </Box>
              </Box>
            )}
          </Box>

        </>
        )}
      </WizardForm>

      {/* Verify Login Modal */}
      <Dialog.Root open={showLoginModal} onOpenChange={setShowLoginModal}>
        <Dialog.Content maxWidth="450px" data-testid="ldap-login-modal">
          <Dialog.Title>
            <Flex align="center" gap="2">
              <KeyRound size={20} />
              Test LDAP Login
            </Flex>
          </Dialog.Title>
          <Dialog.Description size="2">
            Enter credentials to verify that a user can authenticate with this LDAP server.
          </Dialog.Description>

          <Flex direction="column" gap="3" mt="4">
            <Box>
              <Text as="label" size="2" weight="medium" mb="1" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <User size={14} />
                Username
              </Text>
              <TextField.Root
                value={loginUsername}
                onChange={(e) => setLoginUsername(e.target.value)}
                placeholder="Enter LDAP username"
                data-testid="ldap-login-username"
                autoComplete="off"
              />
            </Box>

            <Box>
              <Text as="label" size="2" weight="medium" mb="1" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Lock size={14} />
                Password
              </Text>
              <TextField.Root
                type="password"
                value={loginPassword}
                onChange={(e) => setLoginPassword(e.target.value)}
                placeholder="Enter password"
                data-testid="ldap-login-password"
                autoComplete="new-password"
              />
            </Box>

            {loginResult && (
              <Box className={`ldap-form__modal-result ldap-form__modal-result--${loginResult.success ? 'success' : 'error'}`}>
                {loginResult.success ? (
                  <CheckCircle2 size={16} />
                ) : (
                  <AlertCircle size={16} />
                )}
                <Text size="2">{loginResult.message}</Text>
              </Box>
            )}
          </Flex>

          <Flex gap="3" mt="4" justify="end">
            <Dialog.Close>
              <SettingsButton variant="secondary">
                Close
              </SettingsButton>
            </Dialog.Close>
            <SettingsButton
              variant="primary"
              onClick={handleVerifyLogin}
              disabled={!loginUsername || !loginPassword || loginVerifying}
              loading={loginVerifying}
              data-testid="ldap-login-submit"
            >
              Test Login
            </SettingsButton>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>

      {/* Password Re-entry Modal (NEXUS-23184) */}
      <Dialog.Root open={showPasswordModal} onOpenChange={setShowPasswordModal}>
        <Dialog.Content maxWidth="450px" data-testid="ldap-password-modal">
          <Dialog.Title>
            <Flex align="center" gap="2">
              <Lock size={20} />
              Enter LDAP Password
            </Flex>
          </Dialog.Title>
          <Dialog.Description size="2">
            For security reasons, the password must be re-entered when updating an LDAP server.
            Enter the password for the service account used to connect to LDAP.
          </Dialog.Description>

          <Flex direction="column" gap="3" mt="4">
            <Box>
              <Text as="label" size="2" weight="medium" mb="1" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Lock size={14} />
                Password
              </Text>
              <TextField.Root
                type="password"
                value={reenteredPassword}
                onChange={(e) => setReenteredPassword(e.target.value)}
                placeholder="Enter LDAP password"
                data-testid="ldap-password-input"
                autoComplete="new-password"
              />
              {passwordError && (
                <Text size="1" color="red" mt="1">{passwordError}</Text>
              )}
            </Box>
          </Flex>

          <Flex gap="3" mt="4" justify="end">
            <Dialog.Close>
              <SettingsButton variant="secondary">
                Cancel
              </SettingsButton>
            </Dialog.Close>
            <SettingsButton
              variant="primary"
              onClick={handlePasswordReentry}
              disabled={!reenteredPassword || pendingSave}
              loading={pendingSave}
              data-testid="ldap-password-submit"
            >
              Save
            </SettingsButton>
          </Flex>
        </Dialog.Content>
      </Dialog.Root>

      {/* Delete Confirmation Modal */}
      <ConfirmDialog
        open={showDeleteConfirm}
        testId="delete-ldap-server-dialog"
        onOpenChange={setShowDeleteConfirm}
        title="Delete LDAP Server"
        message={`Are you sure you want to delete the LDAP server "${formData.name}"? Users authenticated through this server will no longer be able to log in.`}
        confirmLabel="Delete Server"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDeleteConfirm}
      />
    </Box>
  );
}

export default LdapForm;
