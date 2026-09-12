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

import React, { useState, useEffect, useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Plus, Loader2 } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsButton, SettingsAlert } from '../../../../shared/form';
import { HelpSection, useToast, PageHeader } from '../../../../shared';
import { LdapList } from './LdapList';
import { LdapForm } from './LdapForm';
import { useLdapApi } from './useLdapApi';
import { LdapServer, LdapPageProps, LdapViewMode } from './types';

import './LdapPage.scss';

// Base paths for LDAP URLs (must match UIRouter route definitions in previewAdminRoutes.js)

function navigateTo(path: string) {
  window.location.hash = path;
}

const BASE_PATH = 'preview/admin/security/ldap';
const CREATE_PATH = `${BASE_PATH}/create`;
const DETAIL_PATH_PREFIX = `${BASE_PATH}/`;

/**
 * Check if the given hash indicates the list view.
 */
function isListRoute(hash: string): boolean {
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  return cleanHash === BASE_PATH || cleanHash === `${BASE_PATH}/`;
}

/**
 * Check if the given hash indicates the create view.
 */
function isCreateRoute(hash: string): boolean {
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  return cleanHash === CREATE_PATH;
}

/**
 * Get server name from detail route hash.
 * Matches UIRouter pattern: preview/admin/security/ldap/:serverId
 */
function getDetailServerName(hash: string): string | null {
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  if (cleanHash.startsWith(DETAIL_PATH_PREFIX) && cleanHash !== BASE_PATH && cleanHash !== `${BASE_PATH}/` && !cleanHash.startsWith(CREATE_PATH)) {
    const segment = cleanHash.substring(DETAIL_PATH_PREFIX.length);
    if (segment && !segment.includes('/')) {
      return decodeURIComponent(segment);
    }
  }
  return null;
}

/**
 * Shared loading indicator for LdapPage's list-loading and edit-loading states.
 */
function LdapPageLoading({ text }: { text: string }) {
  return (
    <Flex align="center" justify="center" className="ldap-page__loading">
      <Loader2 size={24} className="ldap-page__spinner" />
      <Text size="2">{text}</Text>
    </Flex>
  );
}

/**
 * LdapPage - Main LDAP server management page for Preview UI
 */
export function LdapPage({ className }: LdapPageProps) {
  const {
    loading,
    error,
    setError,
    fetchServers,
    fetchTemplates,
    createServer,
    updateServer,
    deleteServer,
    changeOrder,
    clearCache,
    verifyConnection,
    verifyUserMapping,
    verifyLogin,
  } = useLdapApi();

  const [viewMode, setViewMode] = useState<LdapViewMode>('list');
  const [servers, setServers] = useState<LdapServer[]>([]);
  const [selectedServer, setSelectedServer] = useState<LdapServer | null>(null);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  // Toast notifications (app-level provider)
  const toast = useToast();

  const canCreate = ExtJS.checkPermission('nexus:ldap:create');
  const canDelete = ExtJS.checkPermission('nexus:ldap:delete');

  // Load servers on mount and when refreshKey changes
  const loadServers = useCallback(async () => {
    setLoadingInitial(true);
    try {
      const data = await fetchServers();
      setServers(data);
      return data;
    } catch (_err) {
      // Error is set by the hook
      return [];
    } finally {
      setLoadingInitial(false);
    }
  }, [fetchServers]);

  useEffect(() => {
    loadServers();
  }, [loadServers, refreshKey]);

  // Bounce back to the list view with an error toast. Used by the edit-mode
  // re-fetch effect below when the server can't be resolved, and by
  // syncViewWithHash below when a user without nexus:ldap:create lands
  // directly on the create URL (the Create button is already hidden via
  // `canCreate`, but a direct URL/back-button visit bypasses that). Sets
  // `viewMode` (and clears `selectedServer`/`error`) directly rather than
  // relying on the `window.location.hash` assignment to trigger a
  // `hashchange` event that then calls `syncViewWithHash` — that indirection
  // would otherwise leave `viewMode` stuck on the unauthorized view (with the
  // URL and view out of sync) for however long it takes the hashchange
  // listener to fire, and wouldn't update the view at all in contexts where
  // hash assignment doesn't dispatch that event (e.g. tests with a mocked
  // `window.location`).
  const redirectToListWithError = useCallback((message: string) => {
    toast.error(message);
    setViewMode('list');
    setSelectedServer(null);
    setError(null);
    if (!isListRoute(window.location.hash)) {
      window.location.hash = BASE_PATH;
    }
  }, [toast.error, setError]);

  // Handle hash changes for routing
  const syncViewWithHash = useCallback((hash: string) => {
    if (isListRoute(hash)) {
      setViewMode('list');
      setSelectedServer(null);
      setError(null);
    } else if (isCreateRoute(hash)) {
      if (!canCreate) {
        redirectToListWithError('You do not have permission to create LDAP servers');
        return;
      }
      setViewMode('create');
      setSelectedServer(null);
      setError(null);
    } else {
      const serverName = getDetailServerName(hash);
      if (serverName) {
        // selectedServer is populated by the edit-mode re-fetch effect below,
        // not here — it needs the freshly-fetched server object, not just the
        // name from the URL.
        setViewMode('edit');
        setError(null);
      } else {
        setViewMode('list');
      }
    }
  }, [setError, canCreate, redirectToListWithError]);

  // Load server details when in edit mode
  useEffect(() => {
    if (viewMode === 'edit') {
      const serverName = getDetailServerName(window.location.hash);
      if (serverName) {
        let cancelled = false;
        fetchServers().then(freshServers => {
          if (cancelled) return;
          const server = freshServers.find(s => s.name === serverName);
          if (server) {
            setSelectedServer(server);
            setServers(freshServers);
          } else {
            redirectToListWithError(`LDAP server "${serverName}" not found`);
          }
        }).catch(() => {
          if (cancelled) return;
          // fetchServers() already surfaces the error via the hook's `error`
          // state, but that banner is only rendered in list view (see the
          // `error && viewMode === 'list'` gate below). Without this catch, a
          // rejected re-fetch here would leave selectedServer unresolved
          // forever, and the render-gating below would show an infinite
          // "Loading LDAP server..." spinner with no way out except the
          // breadcrumb. Bounce back to the list, where the error is visible.
          redirectToListWithError(`Failed to load LDAP server "${serverName}"`);
        });
        // If the user navigates away (e.g. back to the list) while this
        // re-fetch is in flight, the promise still resolves/rejects after
        // viewMode has already changed. Without this guard, the .then would
        // still call setSelectedServer, and the .catch would still fire a
        // spurious error toast and redirect — both stale actions targeting a
        // view the user already left.
        return () => {
          cancelled = true;
        };
      }
    }
  }, [viewMode, fetchServers, redirectToListWithError]);

  useEffect(() => {
    const handleHashChange = () => {
      syncViewWithHash(window.location.hash);
    };

    // Sync on mount
    syncViewWithHash(window.location.hash);

    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [syncViewWithHash]);

  const handleCreate = useCallback(() => {
    window.location.hash = CREATE_PATH;
  }, []);

  const handleEdit = useCallback((server: LdapServer) => {
    setSelectedServer(server);
    window.location.hash = `${DETAIL_PATH_PREFIX}${encodeURIComponent(server.name)}`;
  }, []);

  // Navigate back to list and reset state.
  const handleBack = useCallback(() => {
    setViewMode('list');
    setSelectedServer(null);
    setError(null);
    setRefreshKey((k) => k + 1);

    // Also update URL hash to the list path
    if (!isListRoute(window.location.hash)) {
      window.location.hash = BASE_PATH;
    }
  }, [setError]);

  const handleSave = useCallback(async () => {
    // Refresh list data and navigate back
    setRefreshKey(k => k + 1);
    handleBack();
  }, [handleBack]);

  const handleDelete = useCallback(async (server: LdapServer, navigateAfter = false) => {
    try {
      await deleteServer(server.name);
      toast.success(`LDAP server "${server.name}" deleted successfully`);
      setRefreshKey((k) => k + 1);
      if (navigateAfter) {
        handleBack();
      }
    } catch (_err) {
      // Error is set by the hook
    }
  }, [deleteServer, toast, handleBack]);

  const handleReorder = useCallback(async (serverNames: string[]) => {
    // Let changeOrder reject so LdapList can roll back optimistic UI state.
    // Error is set on the hook's `error` state and shown in the page banner.
    await changeOrder(serverNames);
    toast.success('Server order updated');
    // Re-fetch directly (rather than bumping refreshKey) so the Order
    // column reflects the new positions without remounting LdapList and
    // flashing the full-page loading spinner — reorder fires far more
    // often (every drag-drop) than the create/edit/delete flows that use
    // refreshKey to also reset LdapList's local UI state.
    // A fetch failure here does not roll back the reorder — the order was
    // already saved; a stale Order column is preferable to reverting a
    // valid change.
    try {
      const data = await fetchServers();
      setServers(data);
    } catch {
      // Ignore; reorder succeeded
    }
  }, [changeOrder, toast, fetchServers]);

  const handleClearCache = useCallback(async () => {
    try {
      await clearCache();
      toast.success('LDAP cache cleared successfully');
    } catch (_err) {
      // Error is set by the hook
    }
  }, [clearCache, toast]);

  // Render header based on view mode
  const renderHeader = () => {
    if (viewMode === 'list') {
      return (
        <PageHeader
          title="LDAP"
          description="Manage LDAP server connections for user authentication"
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'LDAP' }
          ]}
          actions={canCreate && (
            <SettingsButton variant="primary" onClick={handleCreate} icon={Plus} data-analytics-id="nxrm-ldap-create">
              Create LDAP Server
            </SettingsButton>
          )}
        />
      );
    }

    const title = viewMode === 'create'
      ? 'Create LDAP Server'
      : selectedServer
        ? `Edit ${selectedServer.name}`
        : 'Edit LDAP Server';

    const lastBreadcrumb = viewMode === 'create'
      ? 'Create'
      : selectedServer?.name || 'Loading...';

    return (
      <PageHeader
        title={title}
        description={viewMode === 'create'
          ? 'Configure connection and user mapping settings'
          : 'Modify LDAP server configuration'
        }
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'LDAP', onClick: handleBack },
          { label: lastBreadcrumb }
        ]}
      />
    );
  };

  // Loading state
  if (loadingInitial && viewMode === 'list') {
    return (
      <Box 
        className={`ldap-page ${className || ''}`.trim()}
        data-testid="ldap-page"
        data-view="list"
        data-loading="true"
      >
        <LdapPageLoading text="Loading LDAP servers..." />
      </Box>
    );
  }

  return (
    <Box
      className={`ldap-page ${className || ''}`.trim()}
      data-testid="ldap-page"
      data-view={viewMode}
      data-loading={loading ? 'true' : 'false'}
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      <Box mb="4">
        {renderHeader()}
      </Box>

      {/* Alerts - Only show page-level error in list view (forms show errors inline) */}
      {error && viewMode === 'list' && (
        <Box className="ldap-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="ldap-page__content">
        {viewMode === 'list' && (
          <>
            <div className="ldap-page__main">
              <LdapList
                key={refreshKey}
                servers={servers}
                onSelect={handleEdit}
                onCreate={handleCreate}
                onReorder={handleReorder}
                onDelete={handleDelete}
                onClearCache={handleClearCache}
                loading={loading}
              />
            </div>
            <aside className="ldap-page__sidebar">
              <HelpSection
                title="About LDAP"
                content="LDAP (Lightweight Directory Access Protocol) allows you to integrate Nexus Repository with external user directories for authentication. You can configure multiple LDAP servers and they will be queried in order until a user is found."
                docLink={{
                  label: 'View Documentation',
                  href: 'https://help.sonatype.com/en/ldap.html',
                }}
              />
            </aside>
          </>
        )}

        {viewMode === 'create' && (
          <LdapForm
            server={null}
            isCreate={true}
            existingNames={servers.map((s) => s.name)} // snapshot of loaded servers; concurrent additions by other users will not be reflected until LdapPage re-fetches
            onSave={handleSave}
            onCancel={handleBack}
            loading={loading}
            error={error || undefined}
            fetchTemplates={fetchTemplates}
            createServer={createServer}
            updateServer={updateServer}
            verifyConnection={verifyConnection}
            verifyUserMapping={verifyUserMapping}
            verifyLogin={verifyLogin}
          />
        )}

        {/*
         * NEXUS-53672: Only mount LdapForm in edit mode once selectedServer
         * has actually resolved. `viewMode` can flip to 'edit' synchronously
         * (e.g. landing directly on an edit URL via page load/refresh) before
         * the async re-fetch effect above populates selectedServer. XState's
         * useMachine only binds to the machine on the form's first render and
         * ignores later machine changes, so mounting with a null server here
         * would leave the form permanently blank even after the real data
         * arrives. Show a loading state instead until selectedServer is set.
         */}
        {viewMode === 'edit' && (
          selectedServer ? (
            <LdapForm
              server={selectedServer}
              isCreate={false}
              existingNames={servers.filter((s) => s.id !== selectedServer.id).map((s) => s.name)}
              onSave={handleSave}
              onCancel={handleBack}
              onDelete={canDelete ? () => handleDelete(selectedServer, true) : undefined}
              loading={loading}
              error={error || undefined}
              fetchTemplates={fetchTemplates}
              createServer={createServer}
              updateServer={updateServer}
              verifyConnection={verifyConnection}
              verifyUserMapping={verifyUserMapping}
              verifyLogin={verifyLogin}
            />
          ) : (
            <LdapPageLoading text="Loading LDAP server..." />
          )
        )}
      </Box>
    </Box>
  );
}

export default LdapPage;
