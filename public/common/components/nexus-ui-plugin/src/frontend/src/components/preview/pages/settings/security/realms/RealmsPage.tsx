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

import React, { useState, useCallback, useMemo } from 'react';
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import {
  Loader2,
  GripVertical,
  ChevronRight,
  ChevronLeft,
  ChevronsRight,
  ChevronsLeft,
  Search,
  ArrowUp,
  ArrowDown,
} from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import {
  SettingsForm,
  SettingsAlert,
} from '../../../../shared/form';
import { HelpSection, PageHeader } from '../../../../shared';
import { useRealmsForm } from './useRealmsForm';
import { Realm, RealmsPageProps } from './types';

import './RealmsPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

/**
 * RealmsPage - Security Realms configuration page for Preview UI
 *
 * Allows configuring which security realms are active and their order.
 * Order matters - realms are checked in sequence for authentication.
 * Uses XState form machine for state management.
 */
export function RealmsPage({ className }: RealmsPageProps) {
  // XState form hook handles load, save, dirty tracking, toast, validation
  const realmsForm = useRealmsForm();

  // Local UI state for selections and search
  const [availableSearch, setAvailableSearch] = useState('');
  const [activeSearch, setActiveSearch] = useState('');
  const [availableSelection, setAvailableSelection] = useState<string[]>([]);
  const [activeSelection, setActiveSelection] = useState<string[]>([]);
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);

  const canUpdate = ExtJS.checkPermission('nexus:settings:update');

  // Filtered lists
  const filteredInactive = useMemo(() => {
    if (!availableSearch) return realmsForm.inactiveRealms;
    const search = availableSearch.toLowerCase();
    return realmsForm.inactiveRealms.filter((r) => r.name.toLowerCase().includes(search));
  }, [realmsForm.inactiveRealms, availableSearch]);

  const filteredActive = useMemo(() => {
    if (!activeSearch) return realmsForm.activeRealms;
    const search = activeSearch.toLowerCase();
    return realmsForm.activeRealms.filter((r) => r.name.toLowerCase().includes(search));
  }, [realmsForm.activeRealms, activeSearch]);

  // Handle click on available item
  const handleAvailableClick = useCallback((realm: Realm, e: React.MouseEvent) => {
    const id = realm.id;
    if (e.ctrlKey || e.metaKey) {
      setAvailableSelection((prev) =>
        prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
      );
    } else {
      setAvailableSelection([id]);
    }
    setActiveSelection([]);
  }, []);

  // Handle click on active item
  const handleActiveClick = useCallback((realm: Realm, e: React.MouseEvent) => {
    const id = realm.id;
    if (e.ctrlKey || e.metaKey) {
      setActiveSelection((prev) =>
        prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
      );
    } else {
      setActiveSelection([id]);
    }
    setAvailableSelection([]);
  }, []);

  // Move selected to active
  const moveToActive = useCallback(() => {
    if (availableSelection.length === 0) return;
    const realmsToAdd = realmsForm.inactiveRealms.filter((r) => availableSelection.includes(r.id));
    realmsForm.reorder([...realmsForm.activeRealms, ...realmsToAdd]);
    setAvailableSelection([]);
  }, [availableSelection, realmsForm]);

  // Move selected to inactive
  const moveToInactive = useCallback(() => {
    if (activeSelection.length === 0) return;
    realmsForm.reorder(realmsForm.activeRealms.filter((r) => !activeSelection.includes(r.id)));
    setActiveSelection([]);
  }, [activeSelection, realmsForm]);

  // Move all to active
  const moveAllToActive = useCallback(() => {
    realmsForm.reorder(realmsForm.availableRealms);
    setAvailableSelection([]);
  }, [realmsForm]);

  // Move all to inactive
  const moveAllToInactive = useCallback(() => {
    realmsForm.reorder([]);
    setActiveSelection([]);
  }, [realmsForm]);

  // Double click to move single item
  const handleDoubleClickInactive = useCallback((realm: Realm) => {
    realmsForm.addRealm(realm);
    setAvailableSelection([]);
  }, [realmsForm]);

  const handleDoubleClickActive = useCallback((realm: Realm) => {
    realmsForm.removeRealm(realm.id);
    setActiveSelection([]);
  }, [realmsForm]);

  // Move selected item up/down in active list
  const handleMoveUp = useCallback(() => {
    if (activeSelection.length !== 1) return;
    realmsForm.moveUp(activeSelection[0]);
  }, [activeSelection, realmsForm]);

  const handleMoveDown = useCallback(() => {
    if (activeSelection.length !== 1) return;
    realmsForm.moveDown(activeSelection[0]);
  }, [activeSelection, realmsForm]);

  // Drag and drop handlers for reordering
  const handleDragStart = useCallback((e: React.DragEvent, index: number) => {
    setDraggedIndex(index);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', index.toString());
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent, _index: number) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  }, []);

  const handleDrop = useCallback((e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === dropIndex) {
      setDraggedIndex(null);
      return;
    }

    const newRealms = [...realmsForm.activeRealms];
    const [removed] = newRealms.splice(draggedIndex, 1);
    newRealms.splice(dropIndex, 0, removed);
    realmsForm.reorder(newRealms);
    setDraggedIndex(null);
  }, [draggedIndex, realmsForm]);

  const handleDragEnd = useCallback(() => {
    setDraggedIndex(null);
  }, []);

  // Loading state
  if (realmsForm.isLoading) {
    return (
      <Box
        className={`realms-page ${className || ''}`.trim()}
        data-testid="realms-page"
        data-view="edit"
      >
        <Flex align="center" justify="center" className="realms-page__loading">
          <Loader2 size={24} className="realms-page__spinner" />
          <Text size="2">Loading realm configuration...</Text>
        </Flex>
      </Box>
    );
  }

  return (
    <Box
      className={`realms-page ${className || ''}`.trim()}
      data-testid="realms-page"
      data-view="edit"
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      {/* Header */}
      <Box mb="4">
        <PageHeader
          title="Realms"
          description="Configure the active security realms and their order"
        
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'Realms' }
          ]}
/>
      </Box>

      {/* Alerts */}
      {(realmsForm.saveError || realmsForm.loadError) && (
        <Box className="realms-page__alerts">
          <SettingsAlert type="error">
            {realmsForm.saveError || realmsForm.loadError}
          </SettingsAlert>
        </Box>
      )}

      {/* Permission Warning */}
      {!canUpdate && (
        <Box className="realms-page__alerts">
          <SettingsAlert type="warning">
            You don't have permission to edit this page. Contact your administrator to request access.
          </SettingsAlert>
        </Box>
      )}

      {/* Form */}
      <SettingsForm
        onSubmit={() => realmsForm.submit()}
        onCancel={() => realmsForm.discard()}
        loading={realmsForm.isSaving}
        pristine={realmsForm.isPristine}
        showActions={canUpdate}
        testId="realms-form"
        submitAnalyticsId="nxrm-realms-save"
        data-loading={realmsForm.isLoading || realmsForm.isSaving ? 'true' : 'false'}
        data-dirty={!realmsForm.isPristine ? 'true' : 'false'}
        data-valid="true"
      >
        {/* Section heading */}
        <Heading as="h2" size="3" mb="3">Active Realms</Heading>

        {/* Transfer List */}
        <Box className="realms-page__transfer-container">
          {/* Available (Inactive) Panel */}
          <Box className="realms-page__panel">
            <Box className="realms-page__panel-header">
              <Text size="2" weight="medium">Available</Text>
              <Text size="1" className="realms-page__panel-count">
                {filteredInactive.length} realms
              </Text>
            </Box>
            <Box className="realms-page__search">
              <Search size={14} className="realms-page__search-icon" />
              <input
                type="text"
                placeholder="Filter..."
                value={availableSearch}
                onChange={(e) => setAvailableSearch(e.target.value)}
                disabled={!canUpdate}
                className="realms-page__search-input"
              />
            </Box>
            <Box className="realms-page__items" role="listbox" aria-label="Available realms">
              {filteredInactive.map((realm) => (
                <Box
                  key={realm.id}
                  role="option"
                  aria-selected={availableSelection.includes(realm.id)}
                  tabIndex={0}
                  onClick={(e) => canUpdate && handleAvailableClick(realm, e)}
                  onDoubleClick={() => canUpdate && handleDoubleClickInactive(realm)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && canUpdate) handleDoubleClickInactive(realm);
                  }}
                  className={`realms-page__item ${availableSelection.includes(realm.id) ? 'realms-page__item--selected' : ''} ${!canUpdate ? 'realms-page__item--disabled' : ''}`}
                >
                  <Text size="2">{realm.name}</Text>
                </Box>
              ))}
              {filteredInactive.length === 0 && (
                <Box className="realms-page__empty">
                  {availableSearch ? 'No matches' : 'No available realms'}
                </Box>
              )}
            </Box>
          </Box>

          {/* Transfer Controls */}
          <Box className="realms-page__controls">
            <button
              type="button"
              onClick={moveAllToActive}
              disabled={!canUpdate || realmsForm.inactiveRealms.length === 0}
              className="realms-page__control-button"
              aria-label="Add all realms"
              title="Add all"
            >
              <ChevronsRight size={16} />
            </button>
            <button
              type="button"
              onClick={moveToActive}
              disabled={!canUpdate || availableSelection.length === 0}
              className="realms-page__control-button"
              aria-label="Add selected realms"
              title="Add selected"
            >
              <ChevronRight size={16} />
            </button>
            <button
              type="button"
              onClick={moveToInactive}
              disabled={!canUpdate || activeSelection.length === 0}
              className="realms-page__control-button"
              aria-label="Remove selected realms"
              title="Remove selected"
            >
              <ChevronLeft size={16} />
            </button>
            <button
              type="button"
              onClick={moveAllToInactive}
              disabled={!canUpdate || realmsForm.activeRealms.length === 0}
              className="realms-page__control-button"
              aria-label="Remove all realms"
              title="Remove all"
            >
              <ChevronsLeft size={16} />
            </button>
          </Box>

          {/* Active Panel */}
          <Box className="realms-page__panel realms-page__panel--active">
            <Box className="realms-page__panel-header">
              <Text size="2" weight="medium">Active</Text>
              <Text size="1" className="realms-page__panel-count">
                {realmsForm.activeRealms.length} realms
              </Text>
            </Box>
            <Box className="realms-page__search">
              <Search size={14} className="realms-page__search-icon" />
              <input
                type="text"
                placeholder="Filter..."
                value={activeSearch}
                onChange={(e) => setActiveSearch(e.target.value)}
                disabled={!canUpdate}
                className="realms-page__search-input"
              />
            </Box>
            <Box className="realms-page__items realms-page__items--sortable" role="listbox" aria-label="Active realms">
              {filteredActive.map((realm) => {
                const actualIndex = realmsForm.activeRealms.findIndex((r) => r.id === realm.id);
                return (
                  <Box
                    key={realm.id}
                    role="option"
                    aria-selected={activeSelection.includes(realm.id)}
                    tabIndex={0}
                    draggable={canUpdate}
                    onDragStart={(e) => handleDragStart(e, actualIndex)}
                    onDragOver={(e) => handleDragOver(e, actualIndex)}
                    onDrop={(e) => handleDrop(e, actualIndex)}
                    onDragEnd={handleDragEnd}
                    onClick={(e) => canUpdate && handleActiveClick(realm, e)}
                    onDoubleClick={() => canUpdate && handleDoubleClickActive(realm)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && canUpdate) handleDoubleClickActive(realm);
                    }}
                    className={`realms-page__item realms-page__item--draggable ${activeSelection.includes(realm.id) ? 'realms-page__item--selected' : ''} ${draggedIndex === actualIndex ? 'realms-page__item--dragging' : ''} ${!canUpdate ? 'realms-page__item--disabled' : ''}`}
                  >
                    {canUpdate && (
                      <GripVertical size={14} className="realms-page__drag-handle" />
                    )}
                    <Text size="2">{realm.name}</Text>
                    <Text size="1" className="realms-page__item-order">
                      {actualIndex + 1}
                    </Text>
                  </Box>
                );
              })}
              {filteredActive.length === 0 && (
                <Box className="realms-page__empty">
                  {activeSearch ? 'No matches' : 'No active realms'}
                </Box>
              )}
            </Box>
          </Box>

          {/* Reorder Controls */}
          {canUpdate && (
            <Box className="realms-page__reorder-controls">
              <button
                type="button"
                onClick={handleMoveUp}
                disabled={activeSelection.length !== 1 || realmsForm.activeRealms.findIndex((r) => r.id === activeSelection[0]) <= 0}
                className="realms-page__control-button"
                aria-label="Move up"
                title="Move up"
              >
                <ArrowUp size={16} />
              </button>
              <button
                type="button"
                onClick={handleMoveDown}
                disabled={activeSelection.length !== 1 || realmsForm.activeRealms.findIndex((r) => r.id === activeSelection[0]) >= realmsForm.activeRealms.length - 1}
                className="realms-page__control-button"
                aria-label="Move down"
                title="Move down"
              >
                <ArrowDown size={16} />
              </button>
            </Box>
          )}
        </Box>

        {/* Help Section */}
        <HelpSection
          title="About Realms"
          content="Security realms are used to authenticate users. The order of active realms determines the authentication sequence. Move realms between Available and Active lists, and drag to reorder active realms."
          docLink={{
            label: 'View Documentation',
            href: 'https://help.sonatype.com/en/realms.html',
          }}
        />
      </SettingsForm>
    </Box>
  );
}

export default RealmsPage;
