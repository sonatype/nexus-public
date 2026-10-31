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

import React, { useEffect, useState, useCallback } from 'react';
import { useRouter } from '@uirouter/react';
import { Box, Button, Card, Flex, Heading, Text, Theme } from '@radix-ui/themes';

import { PageHeader, ErrorState } from '../../../../shared';
import { useIqConnectedApi } from './useIqConnectedApi';
import { pendingDisconnect, clearPendingDisconnect } from './iqServerStateCache';
import { ExtJS } from '../../../../../../interface/ExtJS';
import Permissions from '../../../../../../constants/Permissions';

/**
 * IqServerOverviewPage — entry point for the IQ Server admin section.
 *
 * Mounted by preview.admin.iq (#preview/admin/iq-overview).
 * On load it fetches the current IQ config:
 *   - enabled  → immediately redirects to preview.admin.iqConnected
 *   - disabled → shows the "Connect to IQ Server to get started" card
 *
 * The Connect button navigates to preview.admin.iqConnection, which opens
 * the configuration dialog on top of IqServerConnectedPage.
 */
export function IqServerOverviewPage() {
  const router = useRouter();
  const { fetchIq } = useIqConnectedApi();

  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Connecting IQ Server writes settings (nexus:settings:update), so hide the Connect action
  // for users without that permission — matching Classic, where IqServer.jsx renders the
  // read-only view (no Connect/Save) unless checkPermission('nexus:settings:update') passes
  // (NEXUS-54212). Use the provider-independent ExtJS.usePermission: the context-based
  // usePermission returns false without a <PermissionsProvider> ancestor, which coreui never
  // mounts, so the action would be hidden even from admins. Depend on hasUser so the check
  // re-evaluates once the user and their permissions load asynchronously after mount.
  const hasUser = ExtJS.useUser() ?? false;
  const canEdit = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.SETTINGS.UPDATE),
    [hasUser],
  );

  useEffect(() => {
    // If we just disconnected, skip the GET entirely — the server processes
    // disconnects asynchronously, so an immediate GET may still return enabled:true
    // and bounce the user back to the connected page.
    if (pendingDisconnect) {
      clearPendingDisconnect();
      setLoading(false);
      return;
    }

    let cancelled = false;
    fetchIq()
      .then(iq => {
        if (cancelled) return;
        if (iq.enabled) {
          router.stateService.go('preview.admin.iqConnected');
        } else {
          setLoading(false);
        }
      })
      .catch(err => {
        if (cancelled) return;
        setLoadError(err?.message || 'Failed to load IQ Server configuration');
        setLoading(false);
      });
    return () => { cancelled = true; };
  }, [fetchIq, router]);

  const handleConnect = useCallback(() => {
    router.stateService.go('preview.admin.iqConnection');
  }, [router]);

  // Return null while loading so the redirect to iqConnected is invisible —
  // showing a spinner here causes a double-load flash (overview spinner then
  // connected spinner) every time a connected tenant navigates to IQ Server.
  if (loading) {
    return null;
  }

  if (loadError) {
    return (
      <ErrorState
        title="Failed to load IQ Server"
        message={loadError}
        onRetry={() => router.stateService.reload()}
      />
    );
  }

  return (
    <Theme accentColor="blue" hasBackground={false}>
      <Box p="4">
        <PageHeader
          title="IQ Server"
          description="Manage Sonatype Repository Firewall and Lifecycle configuration"
          breadcrumbs={[
            { label: 'Settings', onClick: () => router.stateService.go('preview.settings') },
            { label: 'IQ Server' },
          ]}
        />
        <Card data-testid="iq-disconnected-card">
          <Flex direction="column" align="center" justify="center" gap="3" py="6" px="5">
            <Box style={{ maxWidth: 460, textAlign: 'center' }}>
              <Heading as="h1" size="4" weight="bold" mb="2">
                {canEdit
                  ? 'Connect to IQ Server to get started'
                  : 'IQ Server is not connected'}
              </Heading>
              <Text as="p" size="2" color="gray" style={{ margin: 0 }}>
                {canEdit
                  ? 'Sonatype Lifecycle and Repository Firewall require a connection to IQ Server to evaluate policies across your repositories.'
                  : 'You are viewing a read-only version of this page. You do not have permission to edit. Contact your Administrator if you need additional permissions.'}
              </Text>
            </Box>
            {canEdit && (
              <Button size="2" onClick={handleConnect} data-testid="iq-disconnected-connect">
                Connect
              </Button>
            )}
          </Flex>
        </Card>
      </Box>
    </Theme>
  );
}

export default IqServerOverviewPage;
