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
import { Box } from '@radix-ui/themes';
import { Layers, Plus } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { PageHeader, LoadingState } from '../../../../shared';
import { SettingsButton, SettingsAlert } from '../../../../shared/form';
import { ContentSelectorsList } from './ContentSelectorsList';
import { ContentSelectorForm } from './ContentSelectorForm';
import { useContentSelectorsApi } from './useContentSelectorsApi';
import { ContentSelector } from './types';

import './ContentSelectorsPage.scss';

// Base path for content selectors URLs
const BASE_PATH = 'preview/admin/repository/selectors';

/**
 * URL-based routing patterns:
 * - /selectors           → List page
 * - /selectors/create    → Create form
 * - /selectors/{name}    → Edit form
 */
type ViewMode = 'list' | 'create' | 'detail';

interface RouteState {
  viewMode: ViewMode;
  selectorName: string | null;
}

/**
 * Parse the URL hash to determine the current route state
 */
function parseRoute(hash: string): RouteState {
  // Remove leading # and any query params (like ?debug)
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  const parts = cleanHash.split('/');

  // Find the selectors segment
  const selectorsIndex = parts.indexOf('selectors');
  if (selectorsIndex === -1) {
    return { viewMode: 'list', selectorName: null };
  }

  const pathAfterSelectors = parts.slice(selectorsIndex + 1);

  // /selectors → list
  if (pathAfterSelectors.length === 0 || pathAfterSelectors[0] === '') {
    return { viewMode: 'list', selectorName: null };
  }

  // /selectors/create → create form
  if (pathAfterSelectors[0] === 'create') {
    return { viewMode: 'create', selectorName: null };
  }

  // /selectors/{name} → detail/edit
  return {
    viewMode: 'detail',
    selectorName: decodeURIComponent(pathAfterSelectors[0]),
  };
}

/**
 * Navigate to a new route by updating the URL hash
 */
function navigateTo(path: string) {
  window.location.hash = path;
}

/**
 * ContentSelectorsPage - Main Content Selectors management page for Preview UI
 *
 * Uses URL-based routing for testability:
 * - Direct URL access to any view
 * - Browser back/forward support
 * - Bookmarkable URLs
 *
 * URL patterns:
 * - #preview/admin/repository/selectors           → List
 * - #preview/admin/repository/selectors/create    → Create form
 * - #preview/admin/repository/selectors/{name}    → Edit form
 */
export function ContentSelectorsPage() {
  const [routeState, setRouteState] = useState<RouteState>(() => parseRoute(window.location.hash));
  const [selector, setSelector] = useState<ContentSelector | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const {
    loading,
    error,
    setError,
    fetchContentSelector,
  } = useContentSelectorsApi();

  const canCreate = ExtJS.checkPermission('nexus:selectors:create');
  const canDelete = ExtJS.checkPermission('nexus:selectors:delete');

  // Listen for hash changes (browser back/forward, direct URL navigation)
  useEffect(() => {
    const handleHashChange = () => {
      const newState = parseRoute(window.location.hash);
      setRouteState(newState);
      setError(null);
    };

    window.addEventListener('hashchange', handleHashChange);
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, [setError]);

  // Load selector details when editing
  useEffect(() => {
    if (routeState.viewMode === 'detail' && routeState.selectorName) {
      fetchContentSelector(routeState.selectorName)
        .then((selectorData) => {
          if (selectorData) {
            setSelector(selectorData);
          } else {
            setError('Content selector not found');
            navigateTo(BASE_PATH);
          }
        })
        .catch((err) => {
          setError(err.message);
          navigateTo(BASE_PATH);
        });
    } else {
      setSelector(null);
    }
  }, [routeState.viewMode, routeState.selectorName, fetchContentSelector, setError]);

  // Navigation handlers - update URL instead of local state
  const handleSelectSelector = useCallback((name: string) => {
    navigateTo(`${BASE_PATH}/${encodeURIComponent(name)}`);
  }, []);

  const handleCreate = useCallback(() => {
    navigateTo(`${BASE_PATH}/create`);
  }, []);

  const handleBack = useCallback(() => {
    navigateTo(BASE_PATH);
  }, []);

  // Called by the form after successful save/delete to refresh the list
  const handleComplete = useCallback(() => {
    setRefreshKey((k) => k + 1);
  }, []);

  // Determine page title based on view mode
  const getPageConfig = () => {
    if (routeState.viewMode === 'list') {
      return {
        title: 'Content Selectors',
        description: 'Define the content that users can access using Content Selector Expression Language (CSEL)',
      };
    }

    if (routeState.viewMode === 'create') {
      return {
        title: 'Create Content Selector',
        description: 'Define a new content selector using CSEL expressions',
      };
    }

    // detail mode
    return {
      title: selector ? `Edit: ${selector.name}` : 'Content Selector Details',
      description: selector ? `Type: ${selector.type?.toUpperCase()}` : undefined,
    };
  };

  const getBreadcrumbs = () => {
    const settingsItem = { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') };
    if (routeState.viewMode === 'list') {
      return [settingsItem, { label: 'Content Selectors' }];
    }
    const listItem = { label: 'Content Selectors', onClick: handleBack };
    if (routeState.viewMode === 'create') {
      return [settingsItem, listItem, { label: 'Create' }];
    }
    return [settingsItem, listItem, { label: selector?.name || 'Loading...' }];
  };

  const pageConfig = getPageConfig();

  // Show loading state when in detail mode but selector not yet loaded
  // (fetchContentSelector doesn't set API loading - it's async from useEffect)
  if (routeState.viewMode === 'detail' && routeState.selectorName && !selector) {
    return (
      <Box className="content-selectors-page" data-testid="content-selectors-page" data-view="loading">
        <PageHeader
          icon={Layers}
          title="Loading..."
          breadcrumbs={getBreadcrumbs()}
        />
        <LoadingState message="Loading content selector details..." />
      </Box>
    );
  }

  return (
    <Box
      className="content-selectors-page"
      data-testid="content-selectors-page"
      data-view={routeState.viewMode}
    >
      <PageHeader
        icon={Layers}
        title={pageConfig.title}
        description={pageConfig.description}
        breadcrumbs={getBreadcrumbs()}
        actions={
          routeState.viewMode === 'list' && canCreate ? (
            <SettingsButton
              variant="primary"
              onClick={handleCreate}
              icon={Plus}
              testId="create-selector-button"
              data-analytics-id="nxrm-content-selector-create"
            >
              Create Selector
            </SettingsButton>
          ) : undefined
        }
      />

      {/* Error Alert */}
      {error && (
        <Box className="content-selectors-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="content-selectors-page__content">
        {routeState.viewMode === 'list' && (
          <ContentSelectorsList
            key={refreshKey}
            onSelect={handleSelectSelector}
            onCreate={handleCreate}
          />
        )}

        {routeState.viewMode === 'create' && (
          <ContentSelectorForm
            isCreate={true}
            onCancel={handleBack}
            onComplete={handleComplete}
            loading={loading}
          />
        )}

        {routeState.viewMode === 'detail' && selector && (
          <ContentSelectorForm
            selector={selector}
            isCreate={false}
            canDelete={canDelete}
            onCancel={handleBack}
            onComplete={handleComplete}
            loading={loading}
          />
        )}
      </Box>
    </Box>
  );
}

export default ContentSelectorsPage;
