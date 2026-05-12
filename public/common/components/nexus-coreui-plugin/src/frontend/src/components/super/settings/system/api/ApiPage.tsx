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

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Box, Flex, Heading, Text } from '@radix-ui/themes';
import { Code, ExternalLink, Info } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import { usePrivilegesApi } from '../../security/privileges/usePrivilegesApi';
import type { Privilege } from '../../security/privileges/types';
import { useRolesApi } from '../../security/roles/useRolesApi';
import type { Role } from '../../security/roles/types';
import { SettingsAlert } from '../../../shared/form';
import { ApiLayout } from './ApiLayout';
import { EndpointDetail } from './EndpointDetail';
import { EndpointList, endpointRowId } from './EndpointList';
import { useApiModuleDeepLink } from './hooks/useApiModuleDeepLink';
import { useEndpointPermissions } from './hooks/useEndpointPermissions';
import { useViewAsUserAccess } from './hooks/useViewAsUserAccess';
import type { ApiPageProps } from './types';
import {
  filterEndpointsByPermissionSubstring,
  findEndpointByDeepLink,
} from './utils/apiModuleDeepLinkParams';
import { readCurrentSessionUserId } from './utils/currentSessionUser';
import { computeEndpointAccess } from './utils/endpointAccess';
import { filterSwaggerSpecForCloud } from './utils/filterSwaggerForCloud';
import type { MergedApiEndpoint } from './utils/mergeSwaggerPermissions';
import { mergeSwaggerAndPermissions } from './utils/mergeSwaggerPermissions';
import { readNexusPermissionMap } from './utils/nexusPermissionMap';
import { computeRoleLensAccessById } from './utils/roleLensAccess';

import './ApiPage.scss';

function isAdminUser(): boolean {
  try {
    return ExtJS.checkPermission('nexus:*');
  } catch {
    return false;
  }
}

/**
 * ApiPage — API hub (master–detail): permission map + Swagger Try It per operation.
 */
export function ApiPage({ className }: ApiPageProps) {
  const isCloud = ExtJS.state?.().getValue?.('isCloud', false) ?? false;
  const deepLink = useApiModuleDeepLink();
  const currentUserId = readCurrentSessionUserId();
  const { data: permData, loading: permLoading, error: permError } = useEndpointPermissions();
  const { fetchRoles, findRole } = useRolesApi();
  const { fetchPrivileges } = usePrivilegesApi();

  const [swaggerJson, setSwaggerJson] = useState<Record<string, unknown> | null>(null);
  const [swaggerError, setSwaggerError] = useState(false);
  const [dismissPermBanner, setDismissPermBanner] = useState(false);
  const [dismissSwaggerBanner, setDismissSwaggerBanner] = useState(false);
  const [dismissDeepLinkBanner, setDismissDeepLinkBanner] = useState(false);
  const [leftCollapsed, setLeftCollapsed] = useState(false);
  const [selected, setSelected] = useState<MergedApiEndpoint | null>(null);

  const [roleLens, setRoleLens] = useState<{
    allRoles: Role[];
    privileges: Privilege[];
    target: Role;
  } | null>(null);
  const [roleLensError, setRoleLensError] = useState<string | null>(null);

  const swaggerUrl = useMemo(() => ExtJS.urlOf('/service/rest/swagger.json'), []);

  useEffect(() => {
    let cancelled = false;
    setSwaggerError(false);
    fetch(swaggerUrl, { credentials: 'same-origin' })
      .then((r) => {
        if (!r.ok) {
          throw new Error(String(r.status));
        }
        return r.json();
      })
      .then((json) => {
        if (!cancelled && json && typeof json === 'object') {
          setSwaggerJson(json as Record<string, unknown>);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setSwaggerJson(null);
          setSwaggerError(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [swaggerUrl]);

  const swaggerSpec = useMemo(() => {
    if (!swaggerJson) {
      return null;
    }
    return isCloud ? filterSwaggerSpecForCloud(swaggerJson) : swaggerJson;
  }, [swaggerJson, isCloud]);

  useEffect(() => {
    if (!deepLink.roleLensId) {
      setRoleLens(null);
      setRoleLensError(null);
      return undefined;
    }
    let cancelled = false;
    setRoleLens(null);
    setRoleLensError(null);
    (async () => {
      try {
        const [allRoles, privRes, target] = await Promise.all([
          fetchRoles(),
          fetchPrivileges(undefined, undefined, undefined, 0, undefined),
          findRole(deepLink.roleLensId!),
        ]);
        if (cancelled) {
          return;
        }
        if (!target) {
          setRoleLensError(`Role "${deepLink.roleLensId}" was not found.`);
          return;
        }
        setRoleLens({
          allRoles,
          privileges: privRes.data,
          target,
        });
      } catch (e: unknown) {
        if (!cancelled) {
          setRoleLensError(e instanceof Error ? e.message : 'Failed to load role lens data');
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [deepLink.roleLensId, fetchRoles, findRole, fetchPrivileges]);

  const merged = useMemo(
    () => mergeSwaggerAndPermissions(swaggerSpec, permData?.endpoints ?? []),
    [swaggerSpec, permData?.endpoints]
  );

  const permissionFiltered = useMemo(
    () => filterEndpointsByPermissionSubstring(merged, deepLink.permissionFilter),
    [merged, deepLink.permissionFilter]
  );

  const viewAs = useViewAsUserAccess(
    permissionFiltered,
    deepLink.viewAsUserId,
    currentUserId,
    true
  );

  const roleLensAccessById = useMemo(() => {
    if (!deepLink.roleLensId || !roleLens) {
      return null;
    }
    return computeRoleLensAccessById(
      permissionFiltered,
      deepLink.roleLensId,
      roleLens.allRoles,
      roleLens.privileges
    );
  }, [deepLink.roleLensId, roleLens, permissionFiltered]);

  const accessById = useMemo(() => {
    const permMap = readNexusPermissionMap();
    const m: Record<string, ReturnType<typeof computeEndpointAccess>> = {};
    for (const row of permissionFiltered) {
      const id = endpointRowId(row);
      if (viewAs.accessById && viewAs.accessById[id] !== undefined) {
        m[id] = viewAs.accessById[id]!;
      } else if (roleLensAccessById) {
        m[id] = roleLensAccessById[id] ?? 'unknown';
      } else {
        m[id] = computeEndpointAccess(row.permission, permMap);
      }
    }
    return m;
  }, [permissionFiltered, viewAs.accessById, roleLensAccessById]);

  const deepLinkWarnings = useMemo(() => {
    const w = [...deepLink.warnings];
    if (roleLensError) {
      w.push(roleLensError);
    }
    if (viewAs.error) {
      w.push(viewAs.error);
    }
    if (
      deepLink.viewAsUserId &&
      currentUserId &&
      deepLink.viewAsUserId !== currentUserId &&
      !isAdminUser()
    ) {
      w.push(
        "Checking another user's access requires administrator privileges. Access dots may stay unknown until permissions allow the access-check API to succeed."
      );
    }
    if (deepLink.endpointParam && permissionFiltered.length > 0) {
      const f = findEndpointByDeepLink(permissionFiltered, deepLink.endpointParam);
      if (!f) {
        w.push(
          'No endpoint matched the endpoint= deep link for the current filtered list (verify method, path, and permission filter).'
        );
      }
    }
    return w;
  }, [deepLink, roleLensError, viewAs.error, currentUserId, permissionFiltered]);

  const deepLinkWarningKey = deepLinkWarnings.join('\n');
  useEffect(() => {
    setDismissDeepLinkBanner(false);
  }, [deepLinkWarningKey]);

  useEffect(() => {
    if (!deepLink.endpointParam || permissionFiltered.length === 0) {
      return;
    }
    const f = findEndpointByDeepLink(permissionFiltered, deepLink.endpointParam);
    if (f) {
      setSelected(f);
    }
  }, [deepLink.endpointParam, permissionFiltered]);

  const selectedAccess = selected ? accessById[endpointRowId(selected)] ?? 'unknown' : 'unknown';

  const handleSelect = useCallback((row: MergedApiEndpoint) => {
    setSelected(row);
  }, []);

  const initialLoading = permLoading && !permData;
  const roleLensLoading = !!deepLink.roleLensId && !roleLens && !roleLensError;
  const listLoading = initialLoading || viewAs.loading || roleLensLoading;

  const accessDotPalette = deepLink.roleLensId ? 'roleLens' : 'session';

  return (
    <Box
      className={`api-page ${className || ''}`.trim()}
      data-testid="api-page"
      data-loading={initialLoading ? 'true' : 'false'}
    >
      <Flex align="center" gap="3" className="api-page__header">
        <Code size={24} className="api-page__icon" />
        <Box>
          <Heading as="h1" size="6" weight="medium">
            API
          </Heading>
          <Text size="2" className="api-page__description">
            Documentation, permissions, and access tools
          </Text>
          {deepLink.viewAsUserId && (
            <Text size="2" color="gray" mt="1" as="div">
              Viewing access for user: <Text weight="bold">{deepLink.viewAsUserId}</Text>
              {currentUserId && deepLink.viewAsUserId === currentUserId ? ' (you)' : null}
            </Text>
          )}
          {deepLink.roleLensId && (
            <Text size="2" color="gray" mt="1" as="div">
              Role lens:{' '}
              <Text weight="bold">
                {roleLens?.target?.name ?? deepLink.roleLensId}
              </Text>{' '}
              (blue dot = role satisfies required permissions)
            </Text>
          )}
          {deepLink.permissionFilter && (
            <Text size="2" color="gray" mt="1" as="div">
              Filtered to permission containing: <Text weight="bold">{deepLink.permissionFilter}</Text>
            </Text>
          )}
        </Box>
      </Flex>

      {deepLinkWarnings.length > 0 && !dismissDeepLinkBanner && (
        <Box className="api-page__alerts" mb="3">
          <SettingsAlert type="warning" onClose={() => setDismissDeepLinkBanner(true)}>
            <Flex direction="column" gap="1">
              {deepLinkWarnings.map((msg, i) => (
                <Text key={`${i}-${msg}`} size="2">
                  {msg}
                </Text>
              ))}
            </Flex>
          </SettingsAlert>
        </Box>
      )}

      {permError && !dismissPermBanner && (
        <Box className="api-page__alerts" mb="3">
          <SettingsAlert type="warning" onClose={() => setDismissPermBanner(true)}>
            <Text size="2" as="div" weight="medium">
              Could not load the API permission map (Try It and the endpoint list from Swagger may still work).
            </Text>
            <Text size="2" as="div" mt="2" style={{ opacity: 0.95 }}>
              Who Has Access and Grant Access need mapped permissions; they may be limited until this succeeds. Typical
              causes: API not deployed in this build, missing <Text style={{ fontFamily: 'var(--font-mono)' }}>nexus:settings:read</Text>, or network/VPN issues.
            </Text>
            <Text size="2" as="div" mt="2" style={{ fontFamily: 'var(--font-mono)', wordBreak: 'break-word' }}>
              {permError}
            </Text>
          </SettingsAlert>
        </Box>
      )}

      {swaggerError && !dismissSwaggerBanner && (
        <Box className="api-page__alerts" mb="3">
          <SettingsAlert type="warning" onClose={() => setDismissSwaggerBanner(true)}>
            API documentation failed to load. Permission metadata from the registry is still shown where available.
          </SettingsAlert>
        </Box>
      )}

      <ApiLayout
        leftCollapsed={leftCollapsed}
        onToggleLeft={() => setLeftCollapsed((c) => !c)}
        leftPanel={
          <EndpointList
            endpoints={permissionFiltered}
            accessById={accessById}
            selectedId={selected ? endpointRowId(selected) : null}
            onSelect={handleSelect}
            loading={listLoading}
            accessDotPalette={accessDotPalette}
          />
        }
        rightPanel={
          <EndpointDetail row={selected} fullSwagger={swaggerSpec} access={selectedAccess} />
        }
      />

      <Box className="api-page__help">
        <Flex align="center" gap="2" className="api-page__help-header">
          <Info size={16} />
          <Text size="2" weight="medium">
            About API Documentation
          </Text>
        </Flex>
        <Text size="2" className="api-page__help-text">
          Explore the Nexus Repository REST API, inspect required permissions, and use Try It to call operations from
          your browser.
        </Text>
        <Text size="2" className="api-page__help-text">
          See our{' '}
          <a
            href="https://help.sonatype.com/en/rest-and-integration-api.html"
            target="_blank"
            rel="noopener noreferrer"
            className="api-page__help-link"
          >
            documentation
            <ExternalLink size={12} />
          </a>{' '}
          for more information.
        </Text>
      </Box>
    </Box>
  );
}

export default ApiPage;
