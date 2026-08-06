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
  HttpClientAuthenticationConfig,
} from '../types';
import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

import './HttpClientFacet.scss';

interface HttpClientFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  // errors may be either a nested RepositoryFormErrors object or a flat Record<string, string>
  // from the form machine's validationErrors (dot-notation keys).
  errors?: RepositoryFormErrors | Record<string, string | undefined>;
  showPreemptiveAuth?: boolean;
  originalRemoteUrl?: string;
  isEdit?: boolean;
  hadAuthOnLoad?: boolean;
  onOriginChangeWarning?: (warning: boolean) => void;
  format?: string;
}

/**
 * Resolve an error value from either a flat dot-notation errors map or a nested errors object.
 */
function resolveError(
  errors: RepositoryFormErrors | Record<string, string | undefined> | undefined,
  flatKey: string,
  nested: (e: RepositoryFormErrors) => string | undefined
): string | undefined {
  if (!errors) return undefined;
  // Try flat key first (form machine produces flat dot-notation keys)
  const flat = (errors as Record<string, string | undefined>)[flatKey];
  if (flat) return flat;
  // Fall back to nested access
  return nested(errors as RepositoryFormErrors);
}

// Matches ECR registry hostnames: 123456789012.dkr.ecr.us-east-1.amazonaws.com
const ECR_HOST_PATTERN = /^(\d{12})\.dkr\.ecr\.([\w-]+)\.amazonaws\.com$/i;

export function parseEcrUrl(remoteUrl: string): { registryId: string; awsRegion: string } | null {
  if (!remoteUrl) return null;
  const schemeEnd = remoteUrl.indexOf('://');
  let rest = schemeEnd >= 0 ? remoteUrl.substring(schemeEnd + 3) : remoteUrl;
  const slashIdx = rest.indexOf('/');
  if (slashIdx >= 0) rest = rest.substring(0, slashIdx);
  const colonIdx = rest.lastIndexOf(':');
  if (colonIdx >= 0) rest = rest.substring(0, colonIdx);
  const m = ECR_HOST_PATTERN.exec(rest);
  return m ? { registryId: m[1], awsRegion: m[2] } : null;
}

// Formats that support Bearer Token authentication (mirrors Classic UI BearerHttpClientFacet usage)
const BEARER_TOKEN_FORMATS = new Set(['npm', 'pub', 'composer', 'terraform', 'swift', 'cargo', 'huggingface']);

const BASE_AUTH_TYPE_OPTIONS = [
  { value: '', label: UIStrings.HTTP_CLIENT.AUTH_TYPE.NONE },
  { value: 'username', label: UIStrings.HTTP_CLIENT.AUTH_TYPE.USERNAME },
  { value: 'ntlm', label: UIStrings.HTTP_CLIENT.AUTH_TYPE.NTLM },
];

const BEARER_AUTH_TYPE_OPTION = { value: 'bearer', label: UIStrings.HTTP_CLIENT.AUTH_TYPE.BEARER };

/**
 * Creates the Authentication Type change handler. Exported as a pure factory for direct unit
 * testing because the Radix-backed SettingsSelect does not render its options in jsdom.
 *
 * - Selecting None clears HTTP auth.
 * - Selecting username/ntlm/bearer sets the auth type.
 */
export function createAuthTypeChangeHandler(
  onNestedChange: HttpClientFacetProps['onNestedChange'],
  context: {
    existingAuth: HttpClientAuthenticationConfig | null | undefined;
  }
) {
  const { existingAuth } = context;
  return (value: string) => {
    if (!value) {
      onNestedChange('httpClient', { authentication: null });
    } else {
      onNestedChange('httpClient', {
        authentication: {
          type: value as 'username' | 'ntlm' | 'bearer',
          username: existingAuth?.username || '',
          password: existingAuth?.password || '',
        },
      });
    }
  };
}

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
  const [showAdvanced, setShowAdvanced] = useState(true);
  const originResetDone = useRef(false);

  const httpClient = useMemo(() => formData.httpClient || {
    blocked: false,
    autoBlock: true,
    connection: null,
    authentication: null,
  }, [formData.httpClient]);

  useEffect(() => {
    if (!(isEdit && originalRemoteUrl)) {
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

  const handleAuthTypeChange = createAuthTypeChangeHandler(onNestedChange, {
    existingAuth: httpClient.authentication,
  });

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
    // Spread from existing connection (or empty object when connection is null/undefined)
    const existingConnection = httpClient.connection ?? {};
    onNestedChange('httpClient', {
      connection: {
        ...existingConnection,
        [field]: value,
      },
    });
  };

  const authType = httpClient.authentication?.type || '';
  const authTypeOptions = [
    ...BASE_AUTH_TYPE_OPTIONS,
    ...(format && BEARER_TOKEN_FORMATS.has(format) ? [BEARER_AUTH_TYPE_OPTION] : []),
  ];

  // Pre-emptive auth requires HTTPS - check remote URL
  const remoteUrl = formData.proxy?.remoteUrl || '';
  const isSecureRemoteUrl = remoteUrl.startsWith('https://');
  const isPreemptiveAuthDisabled = !isSecureRemoteUrl;

  const isDocker = format === 'docker';
  const ecrEnabled = isDocker && Boolean(parseEcrUrl(remoteUrl));

  return (
    <SettingsFormSection title={UIStrings.HTTP_CLIENT.SECTION.title}>
      {/* Authentication Type dropdown is hidden when ECR auth is auto-active (URL is ECR) */}
      {!ecrEnabled && (
        <SettingsSelect
          name="httpClient-authType"
          label={UIStrings.HTTP_CLIENT.AUTH_TYPE.label}
          value={authType}
          onChange={handleAuthTypeChange}
          options={authTypeOptions}
          helpText={UIStrings.HTTP_CLIENT.AUTH_TYPE.helpText}
        />
      )}

        {authType === 'username' && (
          <>
            <SettingsTextInput
              name="httpClient-username"
              label={UIStrings.HTTP_CLIENT.USERNAME.label}
              value={httpClient.authentication?.username || ''}
              onChange={(v) => handleAuthFieldChange('username', v)}
              error={resolveError(errors, 'httpClient.authentication.username', (e) => e.httpClient?.authentication?.username)}
              required
            />
            <SettingsPasswordInput
              name="httpClient-password"
              label={UIStrings.HTTP_CLIENT.PASSWORD.label}
              value={httpClient.authentication?.password || ''}
              onChange={(v) => handleAuthFieldChange('password', v)}
              error={resolveError(errors, 'httpClient.authentication.password', (e) => e.httpClient?.authentication?.password)}
              required
            />
            {showPreemptiveAuth && (
              <SettingsCheckbox
                name="httpClient-preemptiveAuth"
                label={UIStrings.HTTP_CLIENT.PREEMPTIVE_AUTH.label}
                checked={httpClient.authentication?.preemptive ?? false}
                onChange={(v) => handleAuthFieldChange('preemptive', v)}
                disabled={isPreemptiveAuthDisabled}
                description={
                  isPreemptiveAuthDisabled
                    ? UIStrings.HTTP_CLIENT.PREEMPTIVE_AUTH.disabledDescription
                    : UIStrings.HTTP_CLIENT.PREEMPTIVE_AUTH.description
                }
              />
            )}
          </>
        )}

        {authType === 'ntlm' && (
          <>
            <SettingsTextInput
              name="httpClient-username"
              label={UIStrings.HTTP_CLIENT.USERNAME.label}
              value={httpClient.authentication?.username || ''}
              onChange={(v) => handleAuthFieldChange('username', v)}
              error={resolveError(errors, 'httpClient.authentication.username', (e) => e.httpClient?.authentication?.username)}
              required
            />
            <SettingsPasswordInput
              name="httpClient-password"
              label={UIStrings.HTTP_CLIENT.PASSWORD.label}
              value={httpClient.authentication?.password || ''}
              onChange={(v) => handleAuthFieldChange('password', v)}
              error={resolveError(errors, 'httpClient.authentication.password', (e) => e.httpClient?.authentication?.password)}
              required
            />
            <SettingsTextInput
              name="httpClient-ntlmHost"
              label={UIStrings.HTTP_CLIENT.NTLM_HOST.label}
              value={httpClient.authentication?.ntlmHost || ''}
              onChange={(v) => handleAuthFieldChange('ntlmHost', v)}
              error={resolveError(errors, 'httpClient.authentication.ntlmHost', (e) => (e.httpClient?.authentication as Record<string, string | undefined>)?.ntlmHost)}
              required
            />
            <SettingsTextInput
              name="httpClient-ntlmDomain"
              label={UIStrings.HTTP_CLIENT.NTLM_DOMAIN.label}
              value={httpClient.authentication?.ntlmDomain || ''}
              onChange={(v) => handleAuthFieldChange('ntlmDomain', v)}
              error={resolveError(errors, 'httpClient.authentication.ntlmDomain', (e) => (e.httpClient?.authentication as Record<string, string | undefined>)?.ntlmDomain)}
              required
            />
          </>
        )}

        {authType === 'bearer' && (
          <SettingsPasswordInput
            name="httpClient-bearerToken"
            label={UIStrings.HTTP_CLIENT.BEARER_TOKEN.label}
            value={httpClient.authentication?.bearerToken || ''}
            onChange={(v) => handleAuthFieldChange('bearerToken', v)}
            error={resolveError(errors, 'httpClient.authentication.bearerToken', (e) => (e.httpClient?.authentication as Record<string, string | undefined>)?.bearerToken)}
            required
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
            <Text size="2" weight="medium">{UIStrings.HTTP_CLIENT.ADVANCED.toggleLabel}</Text>
          </Flex>
        </button>

        {showAdvanced && (
          <Box className="http-client-facet__advanced-content">
            <SettingsTextInput
              name="httpClient-userAgentSuffix"
              label={UIStrings.HTTP_CLIENT.USER_AGENT_SUFFIX.label}
              value={httpClient.connection?.userAgentSuffix || ''}
              onChange={(v) => handleConnectionFieldChange('userAgentSuffix', v)}
              helpText={UIStrings.HTTP_CLIENT.USER_AGENT_SUFFIX.helpText}
            />

            <SettingsTextInput
              name="httpClient-retries"
              label={UIStrings.HTTP_CLIENT.RETRIES.label}
              value={httpClient.connection?.retries != null ? String(httpClient.connection.retries) : ''}
              onChange={(v) => {
                if (v === '') {
                  handleConnectionFieldChange('retries', 0);
                } else {
                  const n = parseInt(v, 10);
                  if (!Number.isNaN(n)) handleConnectionFieldChange('retries', n);
                }
              }}
              type="number"
              helpText={UIStrings.HTTP_CLIENT.RETRIES.helpText}
            />

            <SettingsTextInput
              name="httpClient-timeout"
              label={UIStrings.HTTP_CLIENT.TIMEOUT.label}
              value={httpClient.connection?.timeout != null ? String(httpClient.connection.timeout) : ''}
              onChange={(v) => {
                if (v === '') {
                  handleConnectionFieldChange('timeout', 0);
                } else {
                  const n = parseInt(v, 10);
                  if (!Number.isNaN(n)) handleConnectionFieldChange('timeout', n);
                }
              }}
              type="number"
              helpText={UIStrings.HTTP_CLIENT.TIMEOUT.helpText}
            />

            <SettingsCheckbox
              name="httpClient-enableCircularRedirects"
              label={UIStrings.HTTP_CLIENT.CIRCULAR_REDIRECTS.label}
              checked={httpClient.connection?.enableCircularRedirects ?? false}
              onChange={(v) => handleConnectionFieldChange('enableCircularRedirects', v)}
              description={UIStrings.HTTP_CLIENT.CIRCULAR_REDIRECTS.description}
            />

            <SettingsCheckbox
              name="httpClient-enableCookies"
              label={UIStrings.HTTP_CLIENT.COOKIES.label}
              checked={httpClient.connection?.enableCookies ?? false}
              onChange={(v) => handleConnectionFieldChange('enableCookies', v)}
              description={UIStrings.HTTP_CLIENT.COOKIES.description}
            />
          </Box>
        )}
      </Box>
    </SettingsFormSection>
  );
}

export default HttpClientFacet;
