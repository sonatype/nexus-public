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
 * LdapPage - Main LDAP server management page for Preview UI
 */
export function LdapPage({ className }: LdapPageProps) {
  const {
    loading,
    error,
    setError,
    fetchServers,
    deleteServer,
    changeOrder,
    clearCache,
  } = useLdapApi();

  const [viewMode, setViewMode] = useState<LdapViewMode>('list');
  const [servers, setServers] = useState<LdapServer[]>([]);
  const [selectedServer, setSelectedServer] = useState<LdapServer | null>(null);
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);

  // Toast notifications (app-level provider)
  const toast = useToast();

  const canCreate = ExtJS.checkPermission('nexus:ldap:create');

  // Load servers on mount and when refreshKey changes
  const loadServers = useCallback(async () => {
    setLoadingInitial(true);
    try {
      const data = await fetchServers();
      setServers(data);
      return data;
    } catch (err) {
      // Error is set by the hook
      return [];
    } finally {
      setLoadingInitial(false);
    }
  }, [fetchServers]);

  useEffect(() => {
    loadServers();
  }, [loadServers, refreshKey]);

  // Handle hash changes for routing
  const syncViewWithHash = useCallback((hash: string) => {
    const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
    if (isListRoute(hash)) {
      setViewMode('list');
      setSelectedServer(null);
      setError(null);
    } else if (isCreateRoute(hash)) {
      setViewMode('create');
      setSelectedServer(null);
      setError(null);
    } else {
      const serverName = getDetailServerName(hash);
      if (serverName) {
        setViewMode('edit');
        setError(null);
      } else {
        setViewMode('list');
      }
    }
  }, [setError]);

  // Load server details when in edit mode
  useEffect(() => {
    if (viewMode === 'edit') {
      const serverName = getDetailServerName(window.location.hash);
      if (serverName) {
        fetchServers().then(freshServers => {
          const server = freshServers.find(s => s.name === serverName);
          if (server) {
            setSelectedServer(server);
            setServers(freshServers);
          } else {
            toast.error(`LDAP server "${serverName}" not found`);
            window.location.hash = BASE_PATH;
          }
        });
      }
    }
  }, [viewMode, fetchServers, toast]);

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
    } catch (err) {
      // Error is set by the hook
    }
  }, [deleteServer, toast, handleBack]);

  const handleReorder = useCallback(async (serverIds: string[]) => {
    try {
      await changeOrder(serverIds);
      toast.success('Server order updated');
    } catch (err) {
      // Error is set by the hook
    }
  }, [changeOrder, toast]);

  const handleClearCache = useCallback(async () => {
    try {
      await clearCache();
      toast.success('LDAP cache cleared successfully');
    } catch (err) {
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
            <SettingsButton variant="primary" onClick={handleCreate} icon={Plus}>
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
        <Flex align="center" justify="center" className="ldap-page__loading">
          <Loader2 size={24} className="ldap-page__spinner" />
          <Text size="2">Loading LDAP servers...</Text>
        </Flex>
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

        {(viewMode === 'create' || viewMode === 'edit') && (
          <LdapForm
            server={selectedServer}
            isCreate={viewMode === 'create'}
            onSave={handleSave}
            onCancel={handleBack}
            onDelete={selectedServer ? () => handleDelete(selectedServer, true) : undefined}
            loading={loading}
            error={error || undefined}
          />
        )}
      </Box>
    </Box>
  );
}

export default LdapPage;


