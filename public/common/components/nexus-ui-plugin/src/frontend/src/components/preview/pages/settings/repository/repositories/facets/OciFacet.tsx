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
import { Box, Button, Flex, Heading, RadioGroup, Text } from '@radix-ui/themes';
import { Plus, Trash2 } from 'lucide-react';

import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsCheckbox,
  SettingsSelect,
} from '../../../../../shared/form';

import {
  RepositoryFormData,
  RepositoryFormErrors,
  OciCosignAttributes,
  OciProxyAttributes,
} from '../types';

interface OciFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  repoType: 'hosted' | 'proxy' | 'group';
}

const COSIGN_ENFORCEMENT_OPTIONS = [
  { value: 'NONE', label: 'Off (no cosign enforcement)' },
  { value: 'KEYLESS', label: 'Keyless (require cosign signature)' },
];

/**
 * OciFacet — OCI-specific repository configuration for the Preview UI.
 *
 * Branches by `repoType`:
 *   - hosted/group: connector + cosign sections only
 *   - proxy: additionally renders the OCI Registry Index radio (REGISTRY vs
 *     CUSTOM), the indexUrl field with truststore toggle, and the foreign-layer
 *     caching toggle + URL allow-list rows. This mirrors OciProxyFacet.js so
 *     proxy admins get the same surface in Preview as in classic.
 *
 * Fields:
 * - HTTP / HTTPS Connector Port
 * - Subdomain (subdomain-based routing)
 * - Path-Based Routing toggle
 * - Force Basic Authentication
 * - Cosign Keyless Policy (enforcement mode + identity/issuer regexes)
 * - [proxy only] OCI Registry Index source / index URL / truststore
 * - [proxy only] Foreign Layer Caching + URL allow-list
 */
export function OciFacet({
  formData,
  onNestedChange,
  errors,
  repoType,
}: OciFacetProps) {
  const cosign: OciCosignAttributes = formData.oci?.cosign ?? { enforcement: 'NONE' };
  const enforcement = cosign.enforcement ?? 'NONE';
  const keylessActive = enforcement === 'KEYLESS';

  const updateCosign = (patch: Partial<OciCosignAttributes>) => {
    const next: OciCosignAttributes = {
      enforcement,
      identityRegex: cosign.identityRegex,
      issuerRegex: cosign.issuerRegex,
      ...patch,
    };
    onNestedChange('oci', { cosign: next });
  };

  return (
    <>
      <SettingsFormSection
        title="OCI Registry Connectors"
        description="Configure the v2 distribution spec connectors and authentication for this OCI registry"
      >
        <SettingsCheckbox
          name="oci-pathEnabled"
          label="Enable Path-Based Routing"
          checked={formData.oci?.pathEnabled ?? true}
          onChange={(checked) => onNestedChange('oci', { pathEnabled: checked })}
          description="Expose this OCI repository under a path prefix (e.g. /repository/<name>). Disable to require a connector port."
        />

        <SettingsTextInput
          name="oci-httpPort"
          label="HTTP Connector"
          value={formData.oci?.httpPort?.toString() || ''}
          onChange={(value) => onNestedChange('oci', {
            httpPort: value ? parseInt(value, 10) : null,
          })}
          helpText="Create an HTTP connector at the specified port. Normally used if the server is behind a reverse proxy."
          placeholder="e.g., 8082"
          type="number"
        />

        <SettingsTextInput
          name="oci-httpsPort"
          label="HTTPS Connector"
          value={formData.oci?.httpsPort?.toString() || ''}
          onChange={(value) => onNestedChange('oci', {
            httpsPort: value ? parseInt(value, 10) : null,
          })}
          helpText="Create an HTTPS connector at the specified port. Normally used if the server is not behind a reverse proxy."
          placeholder="e.g., 8083"
          type="number"
        />

        <SettingsTextInput
          name="oci-subdomain"
          label="Subdomain"
          value={formData.oci?.subdomain || ''}
          onChange={(value) => onNestedChange('oci', { subdomain: value || null })}
          helpText="Use the specified subdomain to access this OCI repository. Only used when behind a reverse proxy with subdomain-based routing."
          placeholder="e.g., oci-hosted"
        />

        <SettingsCheckbox
          name="oci-forceBasicAuth"
          label="Force Basic Authentication"
          checked={formData.oci?.forceBasicAuth ?? false}
          onChange={(checked) => onNestedChange('oci', { forceBasicAuth: checked })}
          description="Require authentication even for anonymous access (login required for pull)"
        />
      </SettingsFormSection>

      {repoType === 'proxy' && (
        <OciProxySection
          formData={formData}
          onNestedChange={onNestedChange}
          errors={errors}
        />
      )}

      <SettingsFormSection
        title="Cosign Keyless Policy"
        description="Require cosign keyless signatures (Fulcio/Rekor) for image pulls. When enabled, pulls are denied unless a valid cosign referrer is attached to the image."
      >
        <SettingsSelect
          name="oci-cosign-enforcement"
          label="Enforcement mode"
          value={enforcement}
          onChange={(value) => updateCosign({ enforcement: (value || 'NONE') as OciCosignAttributes['enforcement'] })}
          options={COSIGN_ENFORCEMENT_OPTIONS}
          helpText="Off disables enforcement. Keyless requires every pull to be covered by a cosign signature attached as an OCI referrer."
        />

        {keylessActive && (
          <>
            <Text as="p" size="2" color="amber" className="nxrm-oci-cosign-keyless-warning">
              Keyless enforcement is configured but signature verification is not yet active in this
              release. Settings will be applied when the feature becomes available.
            </Text>

            <SettingsTextInput
              name="oci-cosign-identityRegex"
              label="Identity Regex"
              value={cosign.identityRegex || ''}
              onChange={(value) => updateCosign({ identityRegex: value || undefined })}
              helpText="Regex matched against the Fulcio certificate Subject Alternative Name. Required when Keyless enforcement is on."
              placeholder="e.g., ^https://github\.com/acme/.*$"
              required={keylessActive}
              error={
                keylessActive && !(cosign.identityRegex?.trim())
                  ? 'Identity regex is required when Keyless enforcement is enabled'
                  : errors?.oci?.cosign?.identityRegex
              }
            />

            <SettingsTextInput
              name="oci-cosign-issuerRegex"
              label="Issuer Regex"
              value={cosign.issuerRegex || ''}
              onChange={(value) => updateCosign({ issuerRegex: value || undefined })}
              helpText="Regex matched against the OIDC issuer extension on the Fulcio certificate. Required when Keyless enforcement is on."
              placeholder="e.g., ^https://token\.actions\.githubusercontent\.com$"
              required={keylessActive}
              error={
                keylessActive && !(cosign.issuerRegex?.trim())
                  ? 'Issuer regex is required when Keyless enforcement is enabled'
                  : errors?.oci?.cosign?.issuerRegex
              }
            />
          </>
        )}
      </SettingsFormSection>
    </>
  );
}

interface OciProxySectionProps {
  formData: RepositoryFormData;
  onNestedChange: OciFacetProps['onNestedChange'];
  errors?: RepositoryFormErrors;
}

/**
 * Proxy-only OCI subform. Mirrors OciProxyFacet.js: index source radio, custom
 * index URL with truststore toggle, foreign-layer caching toggle, and an editable
 * regex allow-list for foreign-layer URLs.
 *
 * Whitelist rows are stored as a flat string[] under
 * formData.oci.ociProxy.foreignLayerUrlWhitelist. Empty array (or absent) means
 * "deny all foreign-layer fetches" — when caching is first enabled we seed a
 * single ".*" row to match the classic ExtJS UX.
 */
function OciProxySection({ formData, onNestedChange, errors }: OciProxySectionProps) {
  const ociProxy: OciProxyAttributes = formData.oci?.ociProxy ?? {};
  const indexType = ociProxy.indexType ?? 'REGISTRY';
  const cacheForeignLayers = ociProxy.cacheForeignLayers ?? false;
  const whitelist = ociProxy.foreignLayerUrlWhitelist ?? [];

  const patchProxy = (patch: Partial<OciProxyAttributes>) => {
    onNestedChange('oci', {
      ociProxy: { ...ociProxy, ...patch },
    });
  };

  const updateRow = (index: number, value: string) => {
    const next = whitelist.slice();
    next[index] = value;
    patchProxy({ foreignLayerUrlWhitelist: next });
  };
  const removeRow = (index: number) => {
    const next = whitelist.slice();
    next.splice(index, 1);
    patchProxy({ foreignLayerUrlWhitelist: next });
  };
  const addRow = () => {
    const next = whitelist.concat([whitelist.length === 0 ? '.*' : '']);
    patchProxy({ foreignLayerUrlWhitelist: next });
  };

  const onIndexTypeChange = (next: 'REGISTRY' | 'CUSTOM') => {
    // Match the classic UI: switching back to REGISTRY clears the custom URL
    // so a stale value can't sneak through; switching to CUSTOM preserves
    // whatever's already stored so the field is editable.
    patchProxy(
      next === 'REGISTRY'
        ? { indexType: next, indexUrl: null, useTrustStoreForIndexAccess: false }
        : { indexType: next }
    );
  };

  // Auto-seed a starter ".*" row the first time caching is turned on, mirroring
  // the classic UI where toggling Foreign Layer Caching adds a default row.
  const onCacheToggle = (checked: boolean) => {
    if (checked && whitelist.length === 0) {
      patchProxy({ cacheForeignLayers: true, foreignLayerUrlWhitelist: ['.*'] });
    } else {
      patchProxy({ cacheForeignLayers: checked });
    }
  };

  return (
    <SettingsFormSection
      title="OCI Proxy"
      description="Index source and foreign-layer caching policy for this proxy registry"
    >
      <Box mb="3">
        <Text as="div" weight="medium" size="2" mb="1">
          OCI Registry Index
        </Text>
        <RadioGroup.Root
          value={indexType}
          onValueChange={(v) => onIndexTypeChange(v as 'REGISTRY' | 'CUSTOM')}
          name="oci-proxy-indexType"
        >
          <Flex gap="3" direction="column">
            <Text as="label" size="2">
              <Flex gap="2" align="center">
                <RadioGroup.Item value="REGISTRY" />
                Use proxy registry (specified above)
              </Flex>
            </Text>
            <Text as="label" size="2">
              <Flex gap="2" align="center">
                <RadioGroup.Item value="CUSTOM" />
                Custom index
              </Flex>
            </Text>
          </Flex>
        </RadioGroup.Root>
      </Box>

      {indexType === 'CUSTOM' && (
        <>
          <SettingsTextInput
            name="oci-proxy-indexUrl"
            label="OCI Registry Index URL"
            value={ociProxy.indexUrl || ''}
            onChange={(value) => patchProxy({ indexUrl: value || null })}
            helpText="Location of OCI registry index"
            placeholder="https://index.example.com"
            required
            error={
              !(ociProxy.indexUrl?.trim())
                ? 'Index URL is required when Custom index is selected'
                : errors?.oci?.ociProxy?.indexUrl
            }
          />
          <SettingsCheckbox
            name="oci-proxy-useTrustStoreForIndexAccess"
            label="Use the Nexus truststore for HTTPS index access"
            checked={ociProxy.useTrustStoreForIndexAccess ?? false}
            onChange={(checked) => patchProxy({ useTrustStoreForIndexAccess: checked })}
            description="Validate the index server certificate against Nexus's trust store rather than the default JVM CA bundle."
          />
        </>
      )}

      <SettingsCheckbox
        name="oci-proxy-cacheForeignLayers"
        label="Foreign Layer Caching"
        checked={cacheForeignLayers}
        onChange={onCacheToggle}
        description="Allow Nexus Repository Manager to download and cache foreign layers"
      />

      {cacheForeignLayers && (
        <Box mt="2">
          <Heading as="h4" size="2" mb="1">
            Foreign Layer Allowed URLs
          </Heading>
          <Text as="div" size="1" color="gray" mb="2">
            Regular expressions used to identify URLs that are allowed for foreign layer requests
          </Text>
          {whitelist.map((value, idx) => (
            <Flex key={`oci-proxy-whitelist-row-${idx}`} gap="2" align="center" mb="2">
              <Box flexGrow="1">
                <SettingsTextInput
                  name={`oci-proxy-whitelist-${idx}`}
                  label={idx === 0 ? 'Allowed URL pattern' : ''}
                  value={value}
                  onChange={(v) => updateRow(idx, v)}
                  placeholder=".*"
                />
              </Box>
              <Button
                type="button"
                variant="soft"
                color="red"
                size="1"
                onClick={() => removeRow(idx)}
                aria-label={`Remove URL pattern ${idx + 1}`}
              >
                <Trash2 size={14} />
              </Button>
            </Flex>
          ))}
          <Button
            type="button"
            variant="soft"
            size="2"
            onClick={addRow}
            aria-label="Add URL Pattern"
          >
            <Plus size={14} />
            Add URL Pattern
          </Button>
        </Box>
      )}
    </SettingsFormSection>
  );
}

export default OciFacet;
