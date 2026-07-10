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

import React, { useCallback, useEffect } from 'react';
import { Text } from '@radix-ui/themes';

import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsCheckbox,
  SettingsSelect,
  SettingsTextArea,
} from '../../../../../shared/form';
import { restClient, ENDPOINTS } from '../../../../../../../interface/api';

import {
  RepositoryFormData,
  RepositoryFormErrors,
} from '../types';
import { DOCKER_FOREIGN_LAYER_WHITELIST_ERROR_KEY } from '../repositoryFormMachine';

import UIStrings from '../../../../../../../constants/pages/admin/repository/RepositoriesStrings';

import './DockerFacet.scss';

interface DockerFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  repoType: 'hosted' | 'proxy' | 'group';
  isCloud?: boolean;
}

export const ROUTING_PATH_BASED = 'path-based';
export const ROUTING_CONNECTORS = 'connectors';

const ROUTING_MODE_OPTIONS = [
  { value: ROUTING_PATH_BASED, label: UIStrings.DOCKER.ROUTING_MODE.PATH_BASED },
  { value: ROUTING_CONNECTORS, label: UIStrings.DOCKER.ROUTING_MODE.CONNECTORS },
];

const INDEX_TYPE_OPTIONS = [
  { value: 'REGISTRY', label: UIStrings.DOCKER.INDEX_TYPE.USE_HUB },
  { value: 'HUB', label: UIStrings.DOCKER.INDEX_TYPE.USE_PROXY },
  { value: 'CUSTOM', label: UIStrings.DOCKER.INDEX_TYPE.CUSTOM },
];

/**
 * Creates a routing mode change handler that updates docker config
 * based on whether path-based or connector-based routing is selected.
 * Exported for direct unit testing since Radix Select is difficult to interact with in jsdom.
 */
export function createRoutingModeChangeHandler(
  onNestedChange: DockerFacetProps['onNestedChange']
) {
  return (value: string) => {
    const isPathBased = value === ROUTING_PATH_BASED;
    if (isPathBased) {
      onNestedChange('docker', {
        pathEnabled: true,
        httpPort: null,
        httpsPort: null,
        subdomain: null,
      });
    } else {
      onNestedChange('docker', { pathEnabled: false });
    }
  };
}

/**
 * DockerFacet - Docker-specific repository configuration
 *
 * Fields:
 * - Routing Mode (path-based vs connectors) — self-hosted only
 * - HTTP Connector Port (connectors mode only) — self-hosted only
 * - HTTPS Connector Port (connectors mode only) — self-hosted only
 * - Subdomain (connectors mode only) — self-hosted only
 * - Force Basic Authentication
 * - Enable Docker V1 API
 * - Docker Proxy Index Type (proxy repos only)
 * - Foreign Layer Caching (proxy repos only)
 *
 * Note: In cloud (isCloud=true), routing is locked to path-based and connector fields are hidden.
 */
export function DockerFacet({
  formData,
  onNestedChange,
  errors,
  repoType,
  isCloud = false,
}: DockerFacetProps) {
  const isProxy = repoType === 'proxy';
  const pathEnabled = formData.docker?.pathEnabled ?? false;

  // In cloud environments, path-based routing is required. Automatically set
  // pathEnabled=true and clear connector-only fields when isCloud changes.
  useEffect(() => {
    if (isCloud && !formData.docker?.pathEnabled) {
      onNestedChange('docker', {
        pathEnabled: true,
        httpPort: null,
        httpsPort: null,
        subdomain: null,
      });
    }
  }, [isCloud]); // eslint-disable-line react-hooks/exhaustive-deps -- intentional: only respond to isCloud changes, not every formData keystroke

  const handleRoutingModeChange = createRoutingModeChangeHandler(onNestedChange);

  const handleSuggestPort = useCallback(async (field: 'httpPort' | 'httpsPort') => {
    try {
      const otherPort = field === 'httpPort'
        ? formData.docker?.httpsPort
        : formData.docker?.httpPort;
      const url = otherPort
        ? `${ENDPOINTS.DOCKER_SUGGEST_PORT}?exclude=${otherPort}`
        : ENDPOINTS.DOCKER_SUGGEST_PORT;
      const suggested = await restClient.get<number>(url);
      onNestedChange('docker', { [field]: suggested });
    } catch (e: unknown) {
      // Non-blocking: suggest-port failure should never prevent repo creation.
      // User can enter a port manually.
      const status = (e as {response?: {status?: number}})?.response?.status;
      const message = (e as {message?: string})?.message;
      console.warn('Could not suggest Docker connector port:', status ?? message);
    }
  }, [formData.docker, onNestedChange]);

  return (
    <>
      <SettingsFormSection
        title={UIStrings.DOCKER.REGISTRY.title}
        description={UIStrings.DOCKER.REGISTRY.description}
      >
        {!isCloud && (
          <>
            <SettingsSelect
              name="docker-routingMode"
              label={UIStrings.DOCKER.ROUTING_MODE.label}
              value={pathEnabled ? ROUTING_PATH_BASED : ROUTING_CONNECTORS}
              onChange={handleRoutingModeChange}
              options={ROUTING_MODE_OPTIONS}
              helpText={UIStrings.DOCKER.ROUTING_MODE.helpText}
            />

            {!pathEnabled && (
              <>
                <SettingsTextInput
                  name="docker-httpPort"
                  label={UIStrings.DOCKER.HTTP_CONNECTOR.label}
                  value={formData.docker?.httpPort?.toString() || ''}
                  onChange={(value) => onNestedChange('docker', {
                    httpPort: value ? parseInt(value, 10) : null,
                  })}
                  helpText={UIStrings.DOCKER.HTTP_CONNECTOR.helpText}
                  placeholder={UIStrings.DOCKER.HTTP_CONNECTOR.placeholder}
                  type="number"
                  error={errors?.['docker.httpPort'] || ''}
                  alwaysShowHelpText
                />
                <Text as="p" size="1" className="settings-text-input__help">
                  <button
                    type="button"
                    className="docker-facet-suggest-link"
                    onClick={() => handleSuggestPort('httpPort')}
                  >
                    {UIStrings.DOCKER.HTTP_CONNECTOR.suggestPort}
                  </button>
                </Text>

                <SettingsTextInput
                  name="docker-httpsPort"
                  label={UIStrings.DOCKER.HTTPS_CONNECTOR.label}
                  value={formData.docker?.httpsPort?.toString() || ''}
                  onChange={(value) => onNestedChange('docker', {
                    httpsPort: value ? parseInt(value, 10) : null,
                  })}
                  helpText={UIStrings.DOCKER.HTTPS_CONNECTOR.helpText}
                  placeholder={UIStrings.DOCKER.HTTPS_CONNECTOR.placeholder}
                  type="number"
                  error={errors?.['docker.httpsPort'] || ''}
                  alwaysShowHelpText
                />
                <Text as="p" size="1" className="settings-text-input__help">
                  <button
                    type="button"
                    className="docker-facet-suggest-link"
                    onClick={() => handleSuggestPort('httpsPort')}
                  >
                    {UIStrings.DOCKER.HTTPS_CONNECTOR.suggestPort}
                  </button>
                </Text>

                <SettingsTextInput
                  name="docker-subdomain"
                  label={UIStrings.DOCKER.SUBDOMAIN.label}
                  value={formData.docker?.subdomain || ''}
                  onChange={(value) => onNestedChange('docker', { subdomain: value || null })}
                  helpText={UIStrings.DOCKER.SUBDOMAIN.helpText}
                  placeholder={UIStrings.DOCKER.SUBDOMAIN.placeholder}
                />
              </>
            )}
          </>
        )}

        <SettingsCheckbox
          name="docker-forceBasicAuth"
          label={UIStrings.DOCKER.FORCE_BASIC_AUTH.label}
          checked={formData.docker?.forceBasicAuth ?? false}
          onChange={(checked) => onNestedChange('docker', { forceBasicAuth: checked })}
          description={UIStrings.DOCKER.FORCE_BASIC_AUTH.description}
        />

        <SettingsCheckbox
          name="docker-v1Enabled"
          label={UIStrings.DOCKER.V1_ENABLED.label}
          checked={formData.docker?.v1Enabled ?? false}
          onChange={(checked) => onNestedChange('docker', { v1Enabled: checked })}
          description={UIStrings.DOCKER.V1_ENABLED.description}
        />
      </SettingsFormSection>

      {isProxy && (
        <SettingsFormSection
          title={UIStrings.DOCKER.INDEX.title}
          description={UIStrings.DOCKER.INDEX.description}
        >
          <SettingsSelect
            name="docker-indexType"
            label={UIStrings.DOCKER.INDEX_TYPE.label}
            value={formData.dockerProxy?.indexType || 'REGISTRY'}
            onChange={(value) => onNestedChange('dockerProxy', { indexType: value })}
            options={INDEX_TYPE_OPTIONS}
            helpText={UIStrings.DOCKER.INDEX_TYPE.helpText}
          />

          {formData.dockerProxy?.indexType === 'CUSTOM' && (
            <SettingsTextInput
              name="docker-indexUrl"
              label={UIStrings.DOCKER.INDEX_URL.label}
              value={formData.dockerProxy?.indexUrl || ''}
              onChange={(value) => onNestedChange('dockerProxy', { indexUrl: value })}
              helpText={UIStrings.DOCKER.INDEX_URL.helpText}
              placeholder={UIStrings.DOCKER.INDEX_URL.placeholder}
              error={errors?.['dockerProxy.indexUrl'] || ''}
              required
            />
          )}

          <SettingsCheckbox
            name="docker-cacheForeignLayers"
            label={UIStrings.DOCKER.CACHE_FOREIGN_LAYERS.label}
            checked={formData.dockerProxy?.cacheForeignLayers ?? false}
            onChange={(checked) => onNestedChange('dockerProxy', { cacheForeignLayers: checked })}
            description={UIStrings.DOCKER.CACHE_FOREIGN_LAYERS.description}
          />

          {formData.dockerProxy?.cacheForeignLayers && (
            <SettingsTextArea
              name="docker-foreignLayerUrlWhitelist"
              label={UIStrings.DOCKER.FOREIGN_LAYER_WHITELIST.label}
              value={(formData.dockerProxy?.foreignLayerUrlWhitelist || []).join('\n')}
              onChange={(value) => onNestedChange('dockerProxy', {
                foreignLayerUrlWhitelist: value.split('\n').filter((u) => u.trim()),
              })}
              helpText={UIStrings.DOCKER.FOREIGN_LAYER_WHITELIST.helpText}
              placeholder={UIStrings.DOCKER.FOREIGN_LAYER_WHITELIST.placeholder}
              error={errors?.[DOCKER_FOREIGN_LAYER_WHITELIST_ERROR_KEY] || ''}
            />
          )}
        </SettingsFormSection>
      )}
    </>
  );
}

export default DockerFacet;
