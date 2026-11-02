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

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';
import {
  Badge,
  Box,
  Callout,
  Flex,
  Switch,
  Tabs,
  Text,
  Theme,
} from '@radix-ui/themes';
import { CircleAlert } from 'lucide-react';

import { PageHeader } from '../../../../shared';
import { ExtJS } from '../../../../../../interface/ExtJS';
import Permissions from '../../../../../../constants/Permissions';
import { IqServerOverviewTab } from './IqServerOverviewTab';
import { IqServerConnectionSettingsTab } from './IqServerConnectionSettingsTab';
import { DisconnectConfirmationModal } from './DisconnectConfirmationModal';
import { disableIqServer } from './iqServerFormMachine';
import { formatErrorMessage } from './iqServerUtils';
import type { IqServerConfiguration } from './types';

import './IqServerPage.scss';

const ROUTE_STATE = 'preview.admin.iq';

type IqTab = 'overview' | 'connection-settings';

const VALID_TABS: readonly IqTab[] = ['overview', 'connection-settings'];

function isValidTab(value: unknown): value is IqTab {
  return typeof value === 'string' && (VALID_TABS as readonly string[]).includes(value);
}

/**
 * Persisted IQ enabled flag from the rapture bootstrap state — read from data
 * already delivered with the page so tab defaulting doesn't have to wait on a
 * network round-trip. Same accessor RepositoriesList uses.
 */
function isIqServerEnabledLocally(): boolean {
  try {
    return ExtJS.state().getValue('clm')?.enabled ?? false;
  }
  catch {
    return false;
  }
}

function isIqServerConfiguredLocally(): boolean {
  try {
    const clm = ExtJS.state().getValue('clm');
    return Boolean(clm?.url) || Boolean(clm?.enabled);
  } catch {
    return false;
  }
}

/**
 * IqServerPage — Preview UI entry point for the IQ Server admin section.
 *
 * Renders a tabbed layout: Overview (Lifecycle + Firewall cards) and Connection
 * Settings (URL, credentials, trust-store, advanced settings). Replaces the
 * three-way IqServerOverviewPage / IqServerConnectedPage / IqServerConfigurationDialog
 * flow with a single page.
 *
 * Default active tab is derived from the persisted enabled state (matches the
 * signal IqServerOverviewPage previously used to redirect):
 *   - configured & enabled → Overview
 *   - not yet configured → Connection Settings (Overview disabled with tooltip)
 * A `?tab=` URL param takes precedence when present.
 */
export function IqServerPage() {
  const router = useRouter();
  const { params: routeParams } = useCurrentStateAndParams();

  // Snapshot the URL tab param at mount so per-render router objects don't
  // destabilize the default-tab logic below.
  const urlTabRef = useRef<IqTab | undefined>(
    isValidTab(routeParams?.tab) ? routeParams.tab : undefined,
  );

  const [enabled, setEnabled] = useState<boolean>(isIqServerEnabledLocally);
  const [iqUrl, setIqUrl] = useState<string>(() => {
    try {
      return ExtJS.state().getValue('clm')?.url ?? '';
    } catch {
      return '';
    }
  });
  const [configured, setConfigured] = useState<boolean>(isIqServerConfiguredLocally);

  const [tab, setTab] = useState<IqTab>(() => {
    if (urlTabRef.current) return urlTabRef.current;
    // Overview is the default landing tab even before IQ Server is configured: it renders the
    // Lifecycle + Firewall summary in a "Not connected" state, which is a clearer entry point than
    // dropping the user straight into the connection form.
    return 'overview';
  });

  // Bumped every time a save succeeds so the Overview tab knows to reload.
  const [saveRevision, setSaveRevision] = useState(0);

  const [togglingEnabled, setTogglingEnabled] = useState(false);
  const [toggleError, setToggleError] = useState<string | null>(null);
  // Disabling IQ Server is destructive (Repository Firewall stops blocking, policy enforcement
  // stops), so the header toggle-off is gated behind the same confirmation modal as the tab's
  // Disconnect button rather than firing on the switch flip.
  const [showDisconnectModal, setShowDisconnectModal] = useState(false);

  // The enable/disable control is a write affordance: visible to everyone who can
  // read the page, operable only with nexus:settings:update. Uses the ExtJS
  // singleton (not the context hook) because coreui never mounts a
  // <PermissionsProvider> — see NEXUS-54212.
  const hasUser = ExtJS.useUser() ?? false;
  const canEdit = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.SETTINGS.UPDATE),
    [hasUser],
  );

  const syncTabToRoute = useCallback((newTab: IqTab) => {
    try {
      router.stateService.go(ROUTE_STATE, { tab: newTab }, { location: 'replace' });
    } catch {
      // No-op in test environments without a UIRouter provider.
    }
  }, [router]);

  const handleTabChange = useCallback((value: string) => {
    if (!isValidTab(value)) return;
    setTab(value);
    syncTabToRoute(value);
  }, [syncTabToRoute]);

  useEffect(() => {
    // Reflect the initial tab into the URL so a direct load also picks up the
    // ?tab= that was chosen for it (helps bookmarks and back/forward flow).
    syncTabToRoute(tab);
    // Intentionally run once at mount — subsequent changes go through handleTabChange.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const applySavedConfig = useCallback((savedConfig: IqServerConfiguration | undefined | null) => {
    setEnabled(true);
    setConfigured(true);
    if (savedConfig?.url) setIqUrl(savedConfig.url);
    setSaveRevision(r => r + 1);
    setTab('overview');
    syncTabToRoute('overview');
    setToggleError(null);
  }, [syncTabToRoute]);

  const applyDisconnected = useCallback(() => {
    setEnabled(false);
    setToggleError(null);
    setTab('connection-settings');
    syncTabToRoute('connection-settings');
  }, [syncTabToRoute]);

  /**
   * Called when the connection settings tab finishes loading the config from the server.
   * Reconciles the bootstrap-derived local state with actual server state so the UI
   * reflects reality even if the bootstrap data was stale.
   */
  const handleConfigLoaded = useCallback((loadedConfig: IqServerConfiguration | undefined | null) => {
    if (loadedConfig) {
      setEnabled(Boolean(loadedConfig.enabled));
      setConfigured(Boolean(loadedConfig.url));
      if (loadedConfig.url) setIqUrl(loadedConfig.url);
    }
  }, []);

  const handleEnabledChange = useCallback((next: boolean) => {
    if (next) {
      // No live config yet — send the user to the form so they can fill it in.
      setTab('connection-settings');
      syncTabToRoute('connection-settings');
      return;
    }
    // Turning off is destructive — confirm before issuing the disable request. The switch
    // stays visually checked until the disconnect actually succeeds.
    setToggleError(null);
    setShowDisconnectModal(true);
  }, [syncTabToRoute]);

  const confirmDisconnect = useCallback(async () => {
    setToggleError(null);
    setTogglingEnabled(true);
    try {
      await disableIqServer();
      setShowDisconnectModal(false);
      applyDisconnected();
    } catch (err: any) {
      // Never report the integration as disabled when the request failed. Close the modal so
      // the error callout on the page is visible.
      setShowDisconnectModal(false);
      setToggleError(formatErrorMessage(err, 'Failed to disable IQ Server'));
    } finally {
      setTogglingEnabled(false);
    }
  }, [applyDisconnected]);

  return (
    <Theme accentColor="blue" hasBackground={false}>
      <Box className="iq-server-page" p="4">
        <PageHeader
          title="IQ Server"
          description="Manage Sonatype Repository Firewall and Lifecycle configuration"
          descriptionSuffix={
            iqUrl ? (
              <Badge
                variant="soft"
                color="gray"
                size="1"
                radius="full"
                data-testid="iq-server-url-chip"
              >
                {iqUrl}
              </Badge>
            ) : undefined
          }
          breadcrumbs={[
            { label: 'Settings', onClick: () => router.stateService.go('preview.settings') },
            { label: 'IQ Server' },
          ]}
          actions={
            configured ? (
              <Flex asChild align="center" gap="2">
                <label>
                  <Switch
                    data-testid="iq-enable-toggle"
                    aria-label="Enable IQ Server"
                    checked={enabled}
                    onCheckedChange={handleEnabledChange}
                    disabled={!canEdit || togglingEnabled}
                  />
                  <Text size="2">Enable IQ Server</Text>
                </label>
              </Flex>
            ) : undefined
          }
        />

        {toggleError && (
          <Box mt="4" data-testid="iq-enable-error">
            <Callout.Root color="red" variant="soft" role="alert">
              <Callout.Icon>
                <CircleAlert size={16} aria-hidden="true" />
              </Callout.Icon>
              <Callout.Text>{toggleError}</Callout.Text>
            </Callout.Root>
          </Box>
        )}

        <Tabs.Root value={tab} onValueChange={handleTabChange} mt="4">
          <Tabs.List>
            <Tabs.Trigger
              value="overview"
              data-testid="iq-tab-overview"
            >
              Overview
            </Tabs.Trigger>
            <Tabs.Trigger value="connection-settings" data-testid="iq-tab-connection-settings">
              Connection Settings
            </Tabs.Trigger>
          </Tabs.List>

          {/* Each panel stays mounted (forceMount) so the tab layout is stable, but its
              content is rendered only while that tab is active. The inactive panel is then
              left empty — Radix marks it hidden — instead of keeping a hidden-but-present
              subtree in the DOM. Overview renders even before IQ Server is configured: its GET
              endpoints return an empty/unconfigured payload (not an error), so the tab shows the
              Lifecycle + Firewall summary in a "Not connected" state. */}
          <Tabs.Content value="overview" forceMount>
            {tab === 'overview' ? (
              <IqServerOverviewTab isActive saveRevision={saveRevision} />
            ) : null}
          </Tabs.Content>

          <Tabs.Content value="connection-settings" forceMount>
            {tab === 'connection-settings' ? (
              <IqServerConnectionSettingsTab
                onSaved={applySavedConfig}
                onDisconnected={applyDisconnected}
                onConfigLoaded={handleConfigLoaded}
                initiallyConnected={enabled}
                canEdit={canEdit}
              />
            ) : null}
          </Tabs.Content>
        </Tabs.Root>

        <DisconnectConfirmationModal
          open={showDisconnectModal}
          loading={togglingEnabled}
          onClose={() => setShowDisconnectModal(false)}
          onConfirm={confirmDisconnect}
        />
      </Box>
    </Theme>
  );
}

export default IqServerPage;
