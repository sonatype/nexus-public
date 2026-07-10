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

import React, { useState, useMemo, useEffect } from 'react';
import {
  Badge,
  Box,
  Callout,
  Container,
  Flex,
  Text,
  TextField,
  IconButton,
  ScrollArea,
} from '@radix-ui/themes';
import { Search, X, Info } from 'lucide-react';

import { ExtJS } from '../../../../interface/ExtJS';
import { isVisible } from '../../../../interface/NavigationUtils';
import { PageHeader } from '../../shared';
import { SETTINGS_SECTIONS } from './settingsConfig';
import SettingsCard from './SettingsCard';
import SettingsSection from './SettingsSection';

/**
 * SettingsHubPage - Searchable hub for all admin settings
 *
 * Features:
 * - 36 settings cards organized in 4 sections
 * - Client-side search filtering by label, description, and search terms
 * - Empty sections automatically hidden when searching
 * - Cards navigate to individual settings pages
 * - Radix UI components only (no custom CSS)
 *
 * URL: #preview/admin/settings
 */
export function SettingsHubPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const isCloud = ExtJS.state?.().getValue?.('isCloud', false) ?? false;
  const isProEdition = ExtJS.isProEdition?.() ?? false;
  const user = ExtJS.useUser();
  const isAdmin = !!user?.administrator;

  // Reactive trigger: increments when ExtJS permissions load or change,
  // forcing the useMemo to re-evaluate isVisible() with real permission data.
  const [permissionsVersion, setPermissionsVersion] = useState(0);
  useEffect(() => {
    let unmounted = false;
    let interval: ReturnType<typeof setInterval> | undefined;
    const cleanupRef: { fn?: () => void } = {};

    function subscribe() {
      const app = (window as any).Ext?.getApplication?.();
      if (!app) return false;

      const pc = app.getController?.('Permissions');
      const sc = app.getController?.('State');
      if (!pc) return false;

      if (unmounted) return true;

      const handler = () => { if (!unmounted) setPermissionsVersion((v) => v + 1); };
      pc.on('changed', handler);
      sc?.on('userchanged', handler);
      handler();
      cleanupRef.fn = () => {
        pc.un('changed', handler);
        sc?.un('userchanged', handler);
      };
      return true;
    }

    if (!subscribe()) {
      let retries = 0;
      interval = setInterval(() => {
        if (subscribe() || ++retries >= 100) clearInterval(interval!);
      }, 100);
    }

    return () => {
      unmounted = true;
      if (interval) clearInterval(interval);
      cleanupRef.fn?.();
    };
  }, []);

  // ExtJS.state() is a singleton store that React cannot observe — edition values
  // are read at startup and do not change during a session,
  // so omitting them from the dependency array is intentional.
  const filteredSections = useMemo(() => {
    let sections = SETTINGS_SECTIONS;

    if (isCloud) {
      sections = sections
        .filter((section) => !section.cloudExcluded)
        .map((section) => ({
          ...section,
          cards: section.cards.filter((card) => !card.cloudExcluded),
        }))
        .filter((section) => section.cards.length > 0);
    } else if (!isProEdition) {
      sections = sections
        .map((section) => ({
          ...section,
          cards: section.cards.filter((card) => !card.proOnly),
        }))
        .filter((section) => section.cards.length > 0);
    }

    if (!isAdmin) {
      sections = sections
        .map((section) => ({
          ...section,
          cards: section.cards.filter((card) => !card.adminOnly),
        }))
        .filter((section) => section.cards.length > 0);
    }

    sections = sections
      .map((section) => ({
        ...section,
        cards: section.cards.filter((card) => isVisible(card.visibilityRequirements)),
      }))
      .filter((section) => section.cards.length > 0);

    if (!searchQuery.trim()) {
      return sections;
    }

    const query = searchQuery.toLowerCase().trim();

    return sections.map((section) => ({
      ...section,
      cards: section.cards.filter((card) =>
        card.label.toLowerCase().includes(query) ||
        card.description.toLowerCase().includes(query) ||
        card.searchTerms?.some((term) => term.toLowerCase().includes(query))
      ),
    })).filter((section) => section.cards.length > 0);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchQuery, isCloud, isProEdition, isAdmin, permissionsVersion]);

  const hasResults = filteredSections.length > 0;

  return (
    <ScrollArea>
      <Container size="3" py="4">
        <PageHeader
          title="Settings"
          description="Configure security, repositories, system, and support"
        >
          {/* Search bar */}
          <Box mt="4">
            <TextField.Root
              size="3"
              placeholder="Search settings..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            >
              <TextField.Slot side="left">
                <Search size={16} />
              </TextField.Slot>
              {searchQuery && (
                <TextField.Slot side="right">
                  <IconButton
                    variant="ghost"
                    size="1"
                    onClick={() => setSearchQuery('')}
                    aria-label="Clear search"
                  >
                    <X size={14} />
                  </IconButton>
                </TextField.Slot>
              )}
            </TextField.Root>
          </Box>
        </PageHeader>

        {/* Preview mode callout */}
        <Callout.Root color="blue" mb="4" mt="4">
          <Callout.Icon>
            <Info size={16} />
          </Callout.Icon>
          <Callout.Text>
            Some settings pages are available in this preview. Pages marked{' '}
            <Badge color="blue" size="1" variant="soft">Coming Soon</Badge>
            {' '}are still being prepared for the Nexus One UI. Access them through
            the classic UI administration panel.
          </Callout.Text>
        </Callout.Root>

        {/* Settings sections */}
        <Box mt="6">
          {hasResults ? (
            <Flex direction="column" gap="6">
              {filteredSections.map((section) => (
                <SettingsSection key={section.id} section={section} />
              ))}
            </Flex>
          ) : (
            <EmptyState searchQuery={searchQuery} />
          )}
        </Box>
      </Container>
    </ScrollArea>
  );
}

/**
 * EmptyState - Shown when search returns no results
 */
interface EmptyStateProps {
  searchQuery: string;
}

function EmptyState({ searchQuery }: EmptyStateProps) {
  return (
    <Box py="8" style={{ textAlign: 'center' }}>
      <Flex direction="column" align="center" gap="2">
        <Text size="3" weight="medium" color="gray">
          No settings found matching "{searchQuery}"
        </Text>
        <Text size="2" color="gray">
          Try a different search term or browse all settings
        </Text>
      </Flex>
    </Box>
  );
}

export default SettingsHubPage;
