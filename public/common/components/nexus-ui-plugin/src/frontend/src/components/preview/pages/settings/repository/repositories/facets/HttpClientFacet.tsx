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

import React, { useMemo, useState, useEffect, useRef } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { ChevronDown, ChevronRight } from 'lucide-react';

import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsPasswordInput,
  SettingsCheckbox,
  SettingsSelect,
} from '../../../../../shared/form';

import {
  RepositoryFormData,
  RepositoryFormErrors,
} from '../types';

import './HttpClientFacet.scss';

interface HttpClientFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  showPreemptiveAuth?: boolean;
  originalRemoteUrl?: string;
  isEdit?: boolean;
  hadAuthOnLoad?: boolean;
  onOriginChangeWarning?: (warning: boolean) => void;
  format?: string;
}

// Formats that support Bearer Token authentication (mirrors Classic UI BearerHttpClientFacet usage)
const BEARER_TOKEN_FORMATS = new Set(['npm', 'pub', 'composer', 'terraform', 'swift', 'cargo', 'huggingface']);

const BASE_AUTH_TYPE_OPTIONS = [
  { value: '', label: 'No authentication' },
  { value: 'username', label: 'Username' },
  { value: 'ntlm', label: 'Windows NTLM' },
];

const BEARER_AUTH_TYPE_OPTION = { value: 'bearer', label: 'Bearer Token' };

/**
 * HttpClientFacet - HTTP client and authentication settings
 */
export function HttpClientFacet({
  formData,
  onChange,
  onNestedChange,
  errors,
  showPreemptiveAuth = false,
  originalRemoteUrl,
  isEdit = false,
  hadAuthOnLoad = false,
  onOriginChangeWarning,
  format,
}: HttpClientFacetProps) {
  const [showAdvanced, setShowAdvanced] = useState(false);
  const originResetDone = useRef(false);

  const httpClient = useMemo(() => formData.httpClient || {
    blocked: false,
    autoBlock: true,
    connection: null,
    authentication: null,
  }, [formData.httpClient]);

  useEffect(() => {
    if (!isEdit || !originalRemoteUrl) {
      return;
    }

    const currentRemoteUrl = formData.proxy?.remoteUrl;
    if (!currentRemoteUrl) {
      return;
    }

    if (!hadAuthOnLoad) {
      return;
    }

    const urlChanged = originalRemoteUrl !== currentRemoteUrl;

    if (urlChanged && !originResetDone.current) {
      onOriginChangeWarning?.(true);
      originResetDone.current = true;

      onNestedChange('httpClient', {
        authentication: {
          type: 'username',
          username: '',
          password: '',
          bearerToken: '',
          ntlmHost: '',
          ntlmDomain: '',
          preemptive: false,
        },
      });
    } else if (!urlChanged && originResetDone.current) {
      onOriginChangeWarning?.(false);
      originResetDone.current = false;
    }
  }, [formData.proxy?.remoteUrl, isEdit, originalRemoteUrl, hadAuthOnLoad, onOriginChangeWarning, onNestedChange]);

  const handleAuthTypeChange = (value: string) => {
    if (!value) {
      onNestedChange('httpClient', { authentication: null });
    } else {
      onNestedChange('httpClient', {
        authentication: {
          type: value as 'username' | 'ntlm' | 'bearer',
          username: httpClient.authentication?.username || '',
          password: httpClient.authentication?.password || '',
        },
      });
    }
  };

  const handleAuthFieldChange = (field: string, value: string | boolean) => {
    // spreads null safely (evaluates to {}), but caller must ensure `type` is set via handleAuthTypeChange first
    onNestedChange('httpClient', {
      authentication: {
        ...httpClient.authentication,
        [field]: value,
      },
    });
  };

  const handleConnectionFieldChange = (field: string, value: string | number | boolean) => {
    onNestedChange('httpClient', {
      connection: {
        ...httpClient.connection,
        [field]: value,
      },
    });
  };

  const authType = httpClient.authentication?.type || '';
  const authTypeOptions = format && BEARER_TOKEN_FORMATS.has(format)
    ? [...BASE_AUTH_TYPE_OPTIONS, BEARER_AUTH_TYPE_OPTION]
    : BASE_AUTH_TYPE_OPTIONS;

  return (
    <SettingsFormSection title="HTTP">
      {/* Authentication */}
      <SettingsSelect
        name="httpClient-authType"
        label="Authentication Type"
        value={authType}
        onChange={handleAuthTypeChange}
        options={authTypeOptions}
        helpText="Type of authentication used to connect to the remote repository"
      />

        {authType === 'username' && (
          <>
            <SettingsTextInput
              name="httpClient-username"
              label="Username"
              value={httpClient.authentication?.username || ''}
              onChange={(v) => handleAuthFieldChange('username', v)}
              error={errors?.httpClient?.authentication?.username}
            />
            <SettingsPasswordInput
              name="httpClient-password"
              label="Password"
              value={httpClient.authentication?.password || ''}
              onChange={(v) => handleAuthFieldChange('password', v)}
              error={errors?.httpClient?.authentication?.password}
            />
            {showPreemptiveAuth && (
              <SettingsCheckbox
                name="httpClient-preemptiveAuth"
                label="Use pre-emptive authentication"
                checked={httpClient.authentication?.preemptive ?? false}
                onChange={(v) => handleAuthFieldChange('preemptive', v)}
                description="Caution! Use this only when absolutely necessary. Enabling this option means configured authentication credentials will be sent to the remote URL regardless of whether the remote server has asked for them or not."
              />
            )}
          </>
        )}

        {authType === 'ntlm' && (
          <>
            <SettingsTextInput
              name="httpClient-username"
              label="Username"
              value={httpClient.authentication?.username || ''}
              onChange={(v) => handleAuthFieldChange('username', v)}
            />
            <SettingsPasswordInput
              name="httpClient-password"
              label="Password"
              value={httpClient.authentication?.password || ''}
              onChange={(v) => handleAuthFieldChange('password', v)}
            />
            <SettingsTextInput
              name="httpClient-ntlmHost"
              label="NTLM Host"
              value={httpClient.authentication?.ntlmHost || ''}
              onChange={(v) => handleAuthFieldChange('ntlmHost', v)}
            />
            <SettingsTextInput
              name="httpClient-ntlmDomain"
              label="NTLM Domain"
              value={httpClient.authentication?.ntlmDomain || ''}
              onChange={(v) => handleAuthFieldChange('ntlmDomain', v)}
            />
          </>
        )}

        {authType === 'bearer' && (
          <SettingsPasswordInput
            name="httpClient-bearerToken"
            label="Bearer Token"
            value={httpClient.authentication?.bearerToken || ''}
            onChange={(v) => handleAuthFieldChange('bearerToken', v)}
          />
        )}

      {/* Advanced Settings */}
      <Box className="http-client-facet__advanced">
        <button
          type="button"
          onClick={() => setShowAdvanced(!showAdvanced)}
          className="http-client-facet__advanced-toggle"
        >
          <Flex align="center" gap="2">
            {showAdvanced ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
            <Text size="2" weight="medium">HTTP Request Settings</Text>
          </Flex>
        </button>

        {showAdvanced && (
          <Box className="http-client-facet__advanced-content">
            <SettingsTextInput
              name="httpClient-userAgentSuffix"
              label="User-Agent Suffix"
              value={httpClient.connection?.userAgentSuffix || ''}
              onChange={(v) => handleConnectionFieldChange('userAgentSuffix', v)}
              helpText="Custom fragment to append to the User-Agent header"
            />

            <SettingsTextInput
              name="httpClient-retries"
              label="Connection Retries"
              value={String(httpClient.connection?.retries ?? '')}
              onChange={(v) => handleConnectionFieldChange('retries', parseInt(v, 10) || 0)}
              type="number"
              helpText="Total retries if the initial connection attempt suffers a timeout"
            />

            <SettingsTextInput
              name="httpClient-timeout"
              label="Connection/Socket Timeout"
              value={String(httpClient.connection?.timeout ?? '')}
              onChange={(v) => handleConnectionFieldChange('timeout', parseInt(v, 10) || 0)}
              type="number"
              helpText="Seconds to wait for activity before stopping and retrying the connection"
            />

            <SettingsCheckbox
              name="httpClient-enableCircularRedirects"
              label="Enable circular redirects"
              checked={httpClient.connection?.enableCircularRedirects ?? false}
              onChange={(v) => handleConnectionFieldChange('enableCircularRedirects', v)}
              description="Enable redirects to the same location"
            />

            <SettingsCheckbox
              name="httpClient-enableCookies"
              label="Enable cookies"
              checked={httpClient.connection?.enableCookies ?? false}
              onChange={(v) => handleConnectionFieldChange('enableCookies', v)}
              description="Allow cookies to be stored and used"
            />
          </Box>
        )}
      </Box>
    </SettingsFormSection>
  );
}

export default HttpClientFacet;
