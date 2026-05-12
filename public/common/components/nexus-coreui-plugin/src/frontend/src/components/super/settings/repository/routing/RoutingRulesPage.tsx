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
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { Route, Plus, ArrowLeft, Eye } from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import { useToast, LoadingState } from '../../../../shared';
import { SettingsButton, SettingsAlert, ConfirmDialog } from '../../../shared/form';
import { DeleteConfirmationModal } from '@/components/shared/modals/DeleteConfirmationModal';
import { RoutingRulesList } from './RoutingRulesList';
import { RoutingRuleForm } from './RoutingRuleForm';
import { RoutingRulePreview } from './RoutingRulePreview';
import { useRoutingRulesApi } from './useRoutingRulesApi';
import { RoutingRule, RoutingRuleFormData } from './types';

import './RoutingRulesPage.scss';

// Base path for routing rules URLs
const BASE_PATH = 'preview/admin/repository/routingrules';

/**
 * URL-based routing patterns:
 * - /routingrules           → List page
 * - /routingrules/create    → Create form
 * - /routingrules/preview   → Global preview
 * - /routingrules/{name}    → Edit form
 */
type ViewMode = 'list' | 'create' | 'detail' | 'preview';

interface RouteState {
  viewMode: ViewMode;
  ruleName: string | null;
}

/**
 * Parse the URL hash to determine the current route state
 */
function parseRoute(hash: string): RouteState {
  // Remove leading # and any query params (like ?debug)
  const cleanHash = hash.replace(/^#/, '').replace(/\?.*$/, '');
  const parts = cleanHash.split('/');

  // Find the routingrules segment
  const rulesIndex = parts.indexOf('routingrules');
  if (rulesIndex === -1) {
    return { viewMode: 'list', ruleName: null };
  }

  const pathAfterRules = parts.slice(rulesIndex + 1);

  // /routingrules → list
  if (pathAfterRules.length === 0 || pathAfterRules[0] === '') {
    return { viewMode: 'list', ruleName: null };
  }

  // /routingrules/create → create form
  if (pathAfterRules[0] === 'create') {
    return { viewMode: 'create', ruleName: null };
  }

  // /routingrules/preview → global preview
  if (pathAfterRules[0] === 'preview') {
    return { viewMode: 'preview', ruleName: null };
  }

  // /routingrules/{name} → detail/edit
  return {
    viewMode: 'detail',
    ruleName: decodeURIComponent(pathAfterRules[0]),
  };
}

/**
 * Navigate to a new route by updating the URL hash
 */
function navigateTo(path: string) {
  window.location.hash = path;
}

/**
 * RoutingRulesPage - Main Routing Rules management page for Preview UI
 *
 * Uses URL-based routing for testability:
 * - Direct URL access to any view
 * - Browser back/forward support
 * - Bookmarkable URLs
 *
 * URL patterns:
 * - #preview/admin/repository/routingrules           → List
 * - #preview/admin/repository/routingrules/create    → Create form
 * - #preview/admin/repository/routingrules/preview   → Global preview
 * - #preview/admin/repository/routingrules/{name}    → Edit form
 */
export function RoutingRulesPage() {
  const [routeState, setRouteState] = useState<RouteState>(() => parseRoute(window.location.hash));
  const [rule, setRule] = useState<RoutingRule | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  // Toast notifications (app-level provider)
  const toast = useToast();

  const {
    loading,
    error,
    setError,
    fetchRoutingRule,
    deleteRoutingRule,
  } = useRoutingRulesApi();

  const canCreate = ExtJS.checkPermission('nexus:*');
  const canDelete = ExtJS.checkPermission('nexus:*');

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

  // Load rule details when editing
  useEffect(() => {
    if (routeState.viewMode === 'detail' && routeState.ruleName) {
      setRule(null); // Clear stale rule before fetching to avoid showing wrong data
      fetchRoutingRule(routeState.ruleName)
        .then((ruleData) => {
          if (ruleData) {
            setRule(ruleData);
          } else {
            setError('Routing rule not found');
            navigateTo(BASE_PATH);
          }
        })
        .catch((err) => {
          setError(err.message);
          navigateTo(BASE_PATH);
        });
    } else {
      setRule(null);
    }
  }, [routeState.viewMode, routeState.ruleName, fetchRoutingRule, setError]);

  // Navigation handlers - update URL instead of local state
  const handleSelectRule = useCallback((name: string) => {
    navigateTo(`${BASE_PATH}/${encodeURIComponent(name)}`);
  }, []);

  const handleCreate = useCallback(() => {
    navigateTo(`${BASE_PATH}/create`);
  }, []);

  const handlePreview = useCallback(() => {
    navigateTo(`${BASE_PATH}/preview`);
  }, []);

  const handleBack = useCallback(() => {
    navigateTo(BASE_PATH);
  }, []);

  // Called by form after successful save - form's useRoutingRulesForm handles API call + toast
  const handleSave = useCallback((_data: RoutingRuleFormData) => {
    setRefreshKey((k) => k + 1);
  }, []);

  const handleDeleteClick = useCallback(() => {
    setDeleteDialogOpen(true);
  }, []);

  const handleDeleteConfirm = useCallback(async () => {
    if (!rule) return;

    setIsDeleting(true);
    try {
      await deleteRoutingRule(rule.name);
      toast.success(`Routing rule "${rule.name}" deleted successfully`);
      setRefreshKey((k) => k + 1);
      setDeleteDialogOpen(false);
      navigateTo(BASE_PATH);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to delete routing rule';
      setError(message);
      toast.error(message);
    } finally {
      setIsDeleting(false);
    }
  }, [rule, deleteRoutingRule, toast, setError]);

  // Render header based on view mode
  const renderHeader = () => {
    if (routeState.viewMode === 'list') {
      return (
        <Flex justify="between" align="center" className="routing-rules-page__header">
          <Flex align="center" gap="3">
            <Route size={24} className="routing-rules-page__icon" />
            <Box>
              <Heading as="h1" size="6" weight="medium">Routing Rules</Heading>
              <Text size="2" className="routing-rules-page__description">
                Control which requests are allowed or blocked for repositories
              </Text>
            </Box>
          </Flex>
          <Flex gap="2">
            <SettingsButton
              variant="secondary"
              onClick={handlePreview}
              icon={Eye}
              testId="preview-button"
            >
              Preview
            </SettingsButton>
            {canCreate && (
              <SettingsButton
                variant="primary"
                onClick={handleCreate}
                icon={Plus}
                testId="create-rule-button"
              >
                Create Rule
              </SettingsButton>
            )}
          </Flex>
        </Flex>
      );
    }

    if (routeState.viewMode === 'preview') {
      return (
        <Flex align="center" gap="3" className="routing-rules-page__header">
          <SettingsButton
            variant="ghost"
            onClick={handleBack}
            className="routing-rules-page__back"
            icon={ArrowLeft}
            testId="back-button"
            aria-label="Back to Routing Rules"
          />
          <Box>
            <Heading as="h1" size="6" weight="medium">Global Routing Preview</Heading>
            <Text size="2" className="routing-rules-page__description">
              Test how routing rules affect requests across all repositories
            </Text>
          </Box>
        </Flex>
      );
    }

    const title = routeState.viewMode === 'create'
      ? 'Create Routing Rule'
      : rule
        ? `Edit ${rule.name}`
        : 'Routing Rule Details';

    return (
      <Flex align="center" gap="3" className="routing-rules-page__header">
        <SettingsButton
          variant="ghost"
          onClick={handleBack}
          className="routing-rules-page__back"
          icon={ArrowLeft}
          testId="back-button"
          aria-label="Back to Routing Rules"
        />
        <Box>
          <Heading as="h1" size="6" weight="medium">{title}</Heading>
          {rule && routeState.viewMode === 'detail' && (
            <Text size="2" className="routing-rules-page__description">
              Mode: {rule.mode} • {rule.matchers.length} matcher(s)
              {(rule.assignedRepositoryCount ?? 0) > 0 && ` • Assigned to ${rule.assignedRepositoryCount} repositories`}
            </Text>
          )}
        </Box>
      </Flex>
    );
  };

  // Show loading when in detail mode but rule not yet loaded
  if (routeState.viewMode === 'detail' && routeState.ruleName && !rule) {
    return (
      <Box
        className="routing-rules-page"
        data-testid="routing-rules-page"
        data-view="loading"
      >
        {renderHeader()}
        <Box p="4" className="routing-rules-page__content">
          <LoadingState message="Loading routing rule details..." />
        </Box>
      </Box>
    );
  }

  return (
    <Box
      className="routing-rules-page"
      data-testid="routing-rules-page"
      data-view={routeState.viewMode}
    >
      {renderHeader()}

      {/* Error Alert */}
      {error && (
        <Box className="routing-rules-page__alerts">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      {/* Content */}
      <Box className="routing-rules-page__content">
        {routeState.viewMode === 'list' && (
          <RoutingRulesList
            key={refreshKey}
            onSelect={handleSelectRule}
            onCreate={handleCreate}
            onPreview={handlePreview}
          />
        )}

        {routeState.viewMode === 'create' && (
          <RoutingRuleForm
            isCreate={true}
            onSave={handleSave}
            onCancel={handleBack}
            loading={loading}
          />
        )}

        {routeState.viewMode === 'detail' && rule && (
          <RoutingRuleForm
            rule={rule}
            isCreate={false}
            onSave={handleSave}
            onCancel={handleBack}
            onDelete={canDelete ? handleDeleteClick : undefined}
            loading={loading}
          />
        )}

        {routeState.viewMode === 'preview' && (
          <RoutingRulePreview onClose={handleBack} />
        )}
      </Box>

      {/* Delete Confirmation Modal */}
      <DeleteConfirmationModal
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
        onConfirm={handleDeleteConfirm}
        entityName={rule?.name || ''}
        entityType="routing rule"
        loading={isDeleting}
      />
    </Box>
  );
}

export default RoutingRulesPage;
