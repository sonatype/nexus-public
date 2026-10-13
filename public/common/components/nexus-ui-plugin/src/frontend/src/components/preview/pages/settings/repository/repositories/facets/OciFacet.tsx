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

import React, { useEffect } from 'react';
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
  OciCosignAttributes,
  OciProxyAttributes,
} from '../types';

import { type FacetErrors, resolveError } from './resolveError';

interface OciFacetProps {
  formData: RepositoryFormData;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  // Accepts the flat dot-notation map the form machine produces as well as the nested shape the
  // sibling facets still declare; see resolveError.ts.
  errors?: FacetErrors;
  repoType: 'hosted' | 'proxy' | 'group';
  isCloud?: boolean;
}

/**
 * Only NONE is offered. KEYLESS is rejected unconditionally by the server
 * (`OciCosignConfigurationConverters#rejectKeylessUntilVerifierShips`) on all three OCI
 * recipes, because the shipped verifier is a no-op stub that always allows pulls — accepting
 * the mode would advertise an enforcement that does not run. Offering it here made every such
 * save fail with HTTP 400 and persist nothing.
 *
 * Legacy rows that already hold KEYLESS are still surfaced by the server's read path and are
 * rendered read-only by {@link LegacyKeylessNotice}; see NEXUS-53140 for shipping the real
 * sigstore-java verifier, at which point KEYLESS becomes selectable again.
 */
const COSIGN_ENFORCEMENT_OPTIONS = [{ value: 'NONE', label: 'Off (no cosign enforcement)' }];

/**
 * OciFacet — OCI-specific repository configuration for the Preview UI.
 *
 * Branches by `repoType`:
 *   - hosted/group: connector + cosign sections only
 *   - proxy: additionally renders the OCI Registry Index radio (REGISTRY vs
 *     CUSTOM), the indexUrl field, and the foreign-layer caching toggle + URL
 *     allow-list rows. This mirrors OciProxyFacet.js so proxy admins get the
 *     same surface in Preview as in classic.
 *
 * Fields:
 * - HTTP / HTTPS Connector Port
 * - Subdomain (subdomain-based routing)
 * - Path-Based Routing toggle
 * - Force Basic Authentication
 * - Cosign enforcement mode (Off only; see COSIGN_ENFORCEMENT_OPTIONS)
 * - [proxy only] OCI Registry Index source / index URL
 * - [proxy only] Foreign Layer Caching + URL allow-list
 *
 * Note: in cloud (isCloud=true) the backend converter rejects httpPort, httpsPort,
 * a non-empty subdomain, and forceBasicAuth=false. Those fields are hidden and
 * path-based routing is forced so the UI never offers what the server refuses.
 */
export function OciFacet({
  formData,
  onNestedChange,
  errors,
  repoType,
  isCloud = false,
}: OciFacetProps) {
  const cosign: OciCosignAttributes = formData.cosign ?? { enforcement: 'NONE' };
  const enforcement = cosign.enforcement ?? 'NONE';
  // A persisted KEYLESS policy predates the server-side rejection. It cannot be selected or
  // re-saved, but it must still load, so the select falls back to NONE and the real stored
  // value is shown alongside it as read-only text.
  const hasLegacyKeyless = enforcement === 'KEYLESS';

  // In cloud, path-based routing is mandatory and anonymous access is not
  // supported. Force pathEnabled + forceBasicAuth on and clear the
  // connector-only fields the converter would reject.
  useEffect(() => {
    if (!isCloud) {
      return;
    }
    const oci = formData.oci;
    if (!oci?.pathEnabled || oci?.forceBasicAuth !== true
        || oci?.httpPort != null || oci?.httpsPort != null || oci?.subdomain) {
      onNestedChange('oci', {
        pathEnabled: true,
        forceBasicAuth: true,
        httpPort: null,
        httpsPort: null,
        subdomain: null,
      });
    }
  }, [isCloud, formData.oci, onNestedChange]);

  const pathEnabled = formData.oci?.pathEnabled ?? true;

  // Switching TO path-based routing clears the connector fields, mirroring DockerFacet's
  // createRoutingModeChangeHandler. Two reasons this cannot just flip the flag:
  //   1. A port left behind stays bound after save, so the connector keeps listening even
  //      though the operator selected path routing (the Classic UI clears it too).
  //   2. validateRepository skips the port-range check when pathEnabled is true (matching the
  //      Docker branch), which is only sound if a stale out-of-range port cannot survive the
  //      toggle. Clearing here is what makes that skip safe.
  const handlePathEnabledChange = (checked: boolean) => {
    if (checked) {
      onNestedChange('oci', {
        pathEnabled: true,
        httpPort: null,
        httpsPort: null,
        subdomain: null,
      });
    } else {
      onNestedChange('oci', { pathEnabled: false });
    }
  };

  const updateCosign = (patch: Partial<OciCosignAttributes>) => {
    const next: OciCosignAttributes = {
      enforcement,
      identityRegex: cosign.identityRegex,
      issuerRegex: cosign.issuerRegex,
      ...patch,
    };
    onNestedChange('cosign', next);
  };

  return (
    <>
      {/*
        Rendered ahead of every OCI section, not inside the Cosign one. Saving silently resets a
        stored KEYLESS policy to Off, so the disclosure has to be visible before the operator
        reaches Save — at the bottom of the form it can be missed entirely.
      */}
      {hasLegacyKeyless && <LegacyKeylessNotice cosign={cosign} />}

      <SettingsFormSection
        title="OCI Registry Connectors"
        description="Configure the v2 distribution spec connectors and authentication for this OCI registry"
      >
        {!isCloud && (
          <>
            <SettingsCheckbox
              name="oci-pathEnabled"
              label="Enable Path-Based Routing"
              checked={pathEnabled}
              onChange={handlePathEnabledChange}
              description="Expose this OCI repository under a path prefix (e.g. /repository/<name>). Disable to require a connector port."
            />

            {!pathEnabled && (
              <>
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
                  error={resolveError(errors, 'oci.httpPort', (e) => e.oci?.httpPort)}
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
                  error={resolveError(errors, 'oci.httpsPort', (e) => e.oci?.httpsPort)}
                />

                <SettingsTextInput
                  name="oci-subdomain"
                  label="Subdomain"
                  value={formData.oci?.subdomain || ''}
                  onChange={(value) => onNestedChange('oci', { subdomain: value || null })}
                  helpText="Use the specified subdomain to access this OCI repository. Only used when behind a reverse proxy with subdomain-based routing."
                  placeholder="e.g., oci-hosted"
                  error={resolveError(errors, 'oci.subdomain', (e) => e.oci?.subdomain)}
                />
              </>
            )}
          </>
        )}

        <SettingsCheckbox
          name="oci-forceBasicAuth"
          label="Force Basic Authentication"
          checked={formData.oci?.forceBasicAuth ?? true}
          onChange={(checked) => onNestedChange('oci', { forceBasicAuth: checked })}
          description="Require authentication even for anonymous access (login required for pull)"
          disabled={isCloud}
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
        description="Cosign keyless signature enforcement (Fulcio/Rekor) is not available in this release; the only supported setting is Off."
      >
        <SettingsSelect
          name="oci-cosign-enforcement"
          label="Enforcement mode"
          value={hasLegacyKeyless ? 'NONE' : enforcement}
          onChange={(value) => updateCosign({ enforcement: (value || 'NONE') as OciCosignAttributes['enforcement'] })}
          options={COSIGN_ENFORCEMENT_OPTIONS}
          helpText="Off disables enforcement. Keyless signature verification is not available in this release."
        />
      </SettingsFormSection>
    </>
  );
}

/**
 * Read-only disclosure for a repository whose stored cosign enforcement is KEYLESS.
 *
 * Such a policy predates the server-side rejection. Two facts have to be stated plainly,
 * because both are counter-intuitive and silence on either would mislead an operator about a
 * security control:
 *
 * 1. KEYLESS is **not** being enforced. The shipped verifier is a no-op stub that allows every
 *    pull, which is exactly why the server refuses to accept the mode on save.
 * 2. Saving this form **resets** enforcement to Off. The server rejects a KEYLESS payload with
 *    HTTP 400, so the form cannot round-trip the stored value; re-emitting it would make every
 *    save of an unrelated field fail. Downgrading to NONE is the only way to keep the
 *    repository editable, and the operator is told before they save rather than after.
 */
function LegacyKeylessNotice({ cosign }: { cosign: OciCosignAttributes }) {
  return (
    <Box className="nxrm-oci-cosign-legacy-keyless" data-testid="oci-cosign-legacy-keyless">
      <Text as="p" size="2" color="amber">
        This repository has cosign enforcement stored as <strong>Keyless (not available in this
        release)</strong>. Signature verification is <strong>not</strong> active: the verifier
        shipped with this release always allows pulls. Saving this form will reset enforcement to
        Off.
      </Text>
      {(cosign.identityRegex || cosign.issuerRegex) && (
        <Text as="p" size="1" color="gray">
          Stored identity regex: <code>{cosign.identityRegex || '—'}</code>. Stored issuer regex:{' '}
          <code>{cosign.issuerRegex || '—'}</code>.
        </Text>
      )}
    </Box>
  );
}

interface OciProxySectionProps {
  formData: RepositoryFormData;
  onNestedChange: OciFacetProps['onNestedChange'];
  errors?: FacetErrors;
}

/**
 * Proxy-only OCI subform. Mirrors OciProxyFacet.js: index source radio, custom
 * index URL with truststore toggle, foreign-layer caching toggle, and an editable
 * regex allow-list for foreign-layer URLs.
 *
 * Whitelist rows are stored as a flat string[] under
 * formData.ociProxy.foreignLayerUrlWhitelist. Empty array (or absent) means
 * "deny all foreign-layer fetches" — when caching is first enabled we seed a
 * single ".*" row to match the classic ExtJS UX.
 */
function OciProxySection({ formData, onNestedChange, errors }: OciProxySectionProps) {
  const ociProxy: OciProxyAttributes = formData.ociProxy ?? {};
  const indexType = ociProxy.indexType ?? 'REGISTRY';
  const cacheForeignLayers = ociProxy.cacheForeignLayers ?? false;
  const whitelist = ociProxy.foreignLayerUrlWhitelist ?? [];

  const patchProxy = (patch: Partial<OciProxyAttributes>) => {
    onNestedChange('ociProxy', patch);
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
    patchProxy(next === 'REGISTRY' ? { indexType: next, indexUrl: null } : { indexType: next });
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
        <SettingsTextInput
          name="oci-proxy-indexUrl"
          label="OCI Registry Index URL"
          value={ociProxy.indexUrl || ''}
          onChange={(value) => patchProxy({ indexUrl: value || null })}
          helpText="Location of OCI registry index"
          placeholder="https://index.example.com"
          required
          error={resolveError(errors, 'ociProxy.indexUrl', (e) => e.ociProxy?.indexUrl)}
        />
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
