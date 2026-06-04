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

import React from 'react';

import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsCheckbox,
  SettingsSelect,
  SettingsTextArea,
} from '../../../../../shared/form';

import { RepositoryFormData, RepositoryFormErrors } from '../types';

interface DockerFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  repoType: 'hosted' | 'proxy' | 'group';
}

export const ROUTING_PATH_BASED = 'path-based';
export const ROUTING_CONNECTORS = 'connectors';

const ROUTING_MODE_OPTIONS = [
  { value: ROUTING_PATH_BASED, label: 'Use path-based routing' },
  { value: ROUTING_CONNECTORS, label: 'Use connectors (ports and/or subdomain)' },
];

const INDEX_TYPE_OPTIONS = [
  { value: 'REGISTRY', label: 'Use Docker Hub' },
  { value: 'HUB', label: 'Use proxy registry (specified in Remote URL)' },
  { value: 'CUSTOM', label: 'Custom index' },
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
 * - Routing Mode (path-based vs connectors)
 * - HTTP Connector Port (connectors mode only)
 * - HTTPS Connector Port (connectors mode only)
 * - Subdomain (connectors mode only)
 * - Force Basic Authentication
 * - Enable Docker V1 API
 * - Docker Proxy Index Type (proxy repos only)
 * - Foreign Layer Caching (proxy repos only)
 */
export function DockerFacet({
  formData,
  onNestedChange,
  errors,
  repoType,
}: DockerFacetProps) {
  const isProxy = repoType === 'proxy';
  const pathEnabled = formData.docker?.pathEnabled ?? false;

  const handleRoutingModeChange = createRoutingModeChangeHandler(onNestedChange);

  return (
    <>
      <SettingsFormSection
        title="Docker Registry API Support"
        description="Configure Docker registry connector and authentication settings"
      >
        <SettingsSelect
          name="docker-routingMode"
          label="Routing Mode"
          value={pathEnabled ? ROUTING_PATH_BASED : ROUTING_CONNECTORS}
          onChange={handleRoutingModeChange}
          options={ROUTING_MODE_OPTIONS}
          helpText="Path-based routing uses the repository name in the URL path. Connectors use dedicated ports or subdomains."
        />

        {!pathEnabled && (
          <>
            <SettingsTextInput
              name="docker-httpPort"
              label="HTTP Connector"
              value={formData.docker?.httpPort?.toString() || ''}
              onChange={(value) => onNestedChange('docker', {
                httpPort: value ? parseInt(value, 10) : null,
              })}
              helpText="Create an HTTP connector at the specified port. Normally used if the server is behind a reverse proxy."
              placeholder="e.g., 8082"
              type="number"
              error={errors?.['docker.httpPort'] || ''}
            />

            <SettingsTextInput
              name="docker-httpsPort"
              label="HTTPS Connector"
              value={formData.docker?.httpsPort?.toString() || ''}
              onChange={(value) => onNestedChange('docker', {
                httpsPort: value ? parseInt(value, 10) : null,
              })}
              helpText="Create an HTTPS connector at the specified port. Normally used if the server is not behind a reverse proxy."
              placeholder="e.g., 8083"
              type="number"
              error={errors?.['docker.httpsPort'] || ''}
            />

            <SettingsTextInput
              name="docker-subdomain"
              label="Subdomain"
              value={formData.docker?.subdomain || ''}
              onChange={(value) => onNestedChange('docker', { subdomain: value || null })}
              helpText="Use the specified subdomain to access this Docker repository. Only used when behind a reverse proxy with subdomain-based routing."
              placeholder="e.g., docker-hosted"
            />
          </>
        )}

        <SettingsCheckbox
          name="docker-forceBasicAuth"
          label="Force Basic Authentication"
          checked={formData.docker?.forceBasicAuth ?? false}
          onChange={(checked) => onNestedChange('docker', { forceBasicAuth: checked })}
          description="Require authentication even for anonymous access (docker login required for pull)"
        />

        <SettingsCheckbox
          name="docker-v1Enabled"
          label="Enable Docker V1 API"
          checked={formData.docker?.v1Enabled ?? false}
          onChange={(checked) => onNestedChange('docker', { v1Enabled: checked })}
          description="Allow clients to use the V1 API to interact with this repository"
        />
      </SettingsFormSection>

      {isProxy && (
        <SettingsFormSection
          title="Docker Index"
          description="Configure how this proxy repository connects to the Docker registry index"
        >
          <SettingsSelect
            name="docker-indexType"
            label="Docker Index"
            value={formData.dockerProxy?.indexType || 'REGISTRY'}
            onChange={(value) => onNestedChange('dockerProxy', { indexType: value })}
            options={INDEX_TYPE_OPTIONS}
            helpText="Type of Docker Index"
          />

          {formData.dockerProxy?.indexType === 'CUSTOM' && (
            <SettingsTextInput
              name="docker-indexUrl"
              label="Index URL"
              value={formData.dockerProxy?.indexUrl || ''}
              onChange={(value) => onNestedChange('dockerProxy', { indexUrl: value })}
              helpText="Location of Docker Index"
              placeholder="https://index.example.com"
              required
            />
          )}

          <SettingsCheckbox
            name="docker-cacheForeignLayers"
            label="Allow Nexus Repository Manager to download and cache foreign layers"
            checked={formData.dockerProxy?.cacheForeignLayers ?? false}
            onChange={(checked) => onNestedChange('dockerProxy', { cacheForeignLayers: checked })}
            description="Cache foreign layers (layers stored on a different server) in this proxy repository"
          />

          {formData.dockerProxy?.cacheForeignLayers && (
            <SettingsTextArea
              name="docker-foreignLayerUrlWhitelist"
              label="Foreign Layer URL Whitelist"
              value={(formData.dockerProxy?.foreignLayerUrlWhitelist || []).join('\n')}
              onChange={(value) => onNestedChange('dockerProxy', {
                foreignLayerUrlWhitelist: value.split('\n').filter((u) => u.trim()),
              })}
              helpText="Regular expressions of foreign layer URLs to allow (one per line). Leave empty to allow all."
              placeholder="https://example.com/.*"
            />
          )}
        </SettingsFormSection>
      )}
    </>
  );
}

export default DockerFacet;
