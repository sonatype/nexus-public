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

import React, { useCallback, useState } from 'react';
import {
  Badge,
  Box,
  Callout,
  Card,
  Flex,
  Heading,
  IconButton,
  Link,
  ScrollArea,
  Separator,
  Tabs,
  Text,
  TextField,
} from '@radix-ui/themes';
import {
  Search,
  Tags,
  X,
  HelpCircle,
  ExternalLink,
  Info,
  Lightbulb,
  Bookmark,
  GitBranch,
  Package,
  ChevronDown,
  ChevronUp,
  List,
  Clock,
  CheckCircle2,
  Sparkles,
} from 'lucide-react';
import { useCurrentStateAndParams, useRouter } from '@uirouter/react';

import { useTags } from './hooks/useTags';
import { TagsList } from './components/TagsList';
import { TagDetail } from './components/TagDetail';
import { Pagination } from './components/Pagination';

import './TagsPage.scss';

/**
 * UI Strings for the Tags page.
 */
const STRINGS = {
  pageTitle: 'Tags',
  pageDescription: 'Organize and track components with custom tags for CI builds, releases, and staging workflows.',
  listHeader: 'All Tags',
  filterPlaceholder: 'Search tags...',
  helpTitle: 'About Tags',
  helpText:
    'Tags let you mark and group components together. Use them for CI build tracking, release management, or any custom workflow.',
  useCaseTitle: 'Common Use Cases',
  useCases: [
    { icon: GitBranch, text: 'CI/CD build identifiers (e.g., build-142)' },
    { icon: Package, text: 'Release trains (e.g., release-2024.1)' },
    { icon: Bookmark, text: 'Staging and promotion workflows' },
  ],
  helpDocsLink: 'Learn more about tags',
  emptyState: 'No tags found',
  emptyStateFiltered: 'No tags match your search',
  infoCallout:
    'Tags allow you to mark a set of components so they can be logically associated. ' +
    'Common use cases include CI build IDs (e.g., project-abc-build-142) and release trains ' +
    '(e.g., release-2024.1). Tags are also used by the staging feature.',
  apiManagedNotice:
    'Tags are managed via the REST API. To create a tag, use the ' +
    'Tags REST API endpoint to tag components. There is no manual "Create" button by design.',
};

/**
 * Route state names for tags navigation.
 */
const ROUTE_STATES = {
  list: 'preview.browse.tags',
  detail: 'preview.browse.tags.detail',
};

/**
 * Collapsible section component
 */
interface CollapsibleSectionProps {
  title: string;
  icon: React.ReactNode;
  defaultOpen?: boolean;
  children: React.ReactNode;
}

function CollapsibleSection({
  title,
  icon,
  defaultOpen = true,
  children,
}: CollapsibleSectionProps): JSX.Element {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="collapsible-section">
      <button
        type="button"
        className="collapsible-section__header"
        onClick={() => setIsOpen(!isOpen)}
        aria-expanded={isOpen}
      >
        <Flex align="center" gap="2" className="collapsible-section__title">
          {icon}
          <Text size="2" weight="medium">{title}</Text>
        </Flex>
        {isOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
      </button>
      {isOpen && <div className="collapsible-section__content">{children}</div>}
    </div>
  );
}

/**
 * TagsPage - Main entry point for the Tags feature.
 * 
 * Layout inspired by Sonatype Guide with:
 * - Full-width hero header with gradient
 * - Tabbed navigation in main card
 * - Main content area with filter and table
 * - Sidebar with contextual help
 */
export function TagsPage(): JSX.Element {
  const router = useRouter();
  const { params } = useCurrentStateAndParams();

  // Check if we're viewing a specific tag (detail view)
  const selectedTagName = params.tagName || null;

  // Get tags state and actions
  const { state, actions } = useTags();
  const [activeTab, setActiveTab] = useState<string>('list');

  // Calculate page count
  const pageCount = Math.ceil(state.totalItems / state.pageSize);

  /**
   * Handle tag selection from the list.
   */
  const handleSelectTag = useCallback(
    (tagName: string) => {
      router.stateService.go(ROUTE_STATES.detail, { tagName });
    },
    [router]
  );

  /**
   * Handle sort toggle from the list.
   */
  const handleSort = useCallback(
    (field: typeof state.sortField) => {
      actions.toggleSort(field);
    },
    [actions]
  );

  /**
   * Clear the filter input.
   */
  const handleClearFilter = useCallback(() => {
    actions.setFilter('');
  }, [actions]);

  /**
   * Handle back navigation from tag detail.
   */
  const handleBackFromDetail = useCallback(() => {
    router.stateService.go(ROUTE_STATES.list);
  }, [router]);

  // If tag detail is selected, render TagDetail component
  if (selectedTagName) {
    return <TagDetail tagName={selectedTagName} onBack={handleBackFromDetail} />;
  }

  return (
    <div className="tags-page" data-testid="tags-page">
      {/* Hero Header - Full Width */}
      <div className="tags-page__hero">
        <div className="tags-page__hero-container">
          <Flex align="center" gap="4">
            <div className="tags-page__hero-icon">
              <Tags size={28} />
            </div>
            <Box>
              <Heading size="7" className="tags-page__title">
                {STRINGS.pageTitle}
              </Heading>
              <Text size="3" className="tags-page__subtitle">
                {STRINGS.pageDescription}
              </Text>
            </Box>
          </Flex>

          <Flex align="center" gap="3" mt="4" className="tags-page__badges">
            <Badge size="2" variant="soft" color="violet" className="tags-page__format-badge">
              <Flex align="center" gap="1">
                <Sparkles size={12} />
                Pro Feature
              </Flex>
            </Badge>
            <Badge size="2" variant="soft" color="gray">
              <Flex align="center" gap="1">
                <List size={12} />
                {state.totalItems} tag{state.totalItems !== 1 ? 's' : ''}
              </Flex>
            </Badge>
          </Flex>
        </div>
      </div>

      {/* Main Content */}
      <div className="tags-page__container">
        <div className="tags-page__layout">
          
          {/* Main Column - Tags List */}
          <div className="tags-page__main">
            <Card className="tags-page__card tags-page__main-card">
              <Callout.Root color="blue" size="1" mb="3" data-testid="tags-info-callout">
                <Callout.Icon>
                  <Info size={16} />
                </Callout.Icon>
                <Callout.Text size="2">
                  {STRINGS.infoCallout}{' '}
                  <Link
                    href="https://help.sonatype.com/en/tagging.html"
                    target="_blank"
                    rel="noopener noreferrer"
                    size="2"
                  >
                    Learn more <ExternalLink size={12} style={{ display: 'inline', verticalAlign: 'middle' }} />
                  </Link>
                </Callout.Text>
              </Callout.Root>

              <Callout.Root color="gray" size="1" mb="3" variant="surface" data-testid="tags-api-notice">
                <Callout.Icon>
                  <HelpCircle size={16} />
                </Callout.Icon>
                <Callout.Text size="2">
                  {STRINGS.apiManagedNotice}
                </Callout.Text>
              </Callout.Root>

              <Tabs.Root value={activeTab} onValueChange={setActiveTab}>
                <Flex justify="between" align="center" className="tags-page__card-header">
                  <Tabs.List className="tags-page__tabs">
                    <Tabs.Trigger value="list" className="tags-page__tab">
                      <List size={16} />
                      All Tags
                    </Tabs.Trigger>
                    <Tabs.Trigger value="recent" className="tags-page__tab">
                      <Clock size={16} />
                      Recent
                    </Tabs.Trigger>
                  </Tabs.List>

                  <Box className="tags-page__filter">
                    <TextField.Root
                      placeholder={STRINGS.filterPlaceholder}
                      value={state.filter}
                      onChange={(e) => actions.setFilter(e.target.value)}
                      data-testid="tags-filter"
                      size="2"
                    >
                      <TextField.Slot>
                        <Search size={16} />
                      </TextField.Slot>
                      {state.filter && (
                        <TextField.Slot>
                          <IconButton
                            variant="ghost"
                            size="1"
                            onClick={handleClearFilter}
                            aria-label="Clear filter"
                          >
                            <X size={14} />
                          </IconButton>
                        </TextField.Slot>
                      )}
                    </TextField.Root>
                  </Box>
                </Flex>

                <Box className="tags-page__tab-content">
                  <Tabs.Content value="list">
                    {/* Tags Table */}
                    <ScrollArea scrollbars="vertical" className="tags-page__scroll-area">
                      <TagsList
                        tags={state.tags}
                        loading={state.loading}
                        error={state.error}
                        sortField={state.sortField}
                        sortDirection={state.sortDirection}
                        onSort={handleSort}
                        onSelect={handleSelectTag}
                        onRetry={actions.retry}
                      />
                    </ScrollArea>

                    {/* Pagination */}
                    {!state.loading && !state.error && pageCount > 1 && (
                      <Box className="tags-page__pagination">
                        <Pagination
                          currentPage={state.currentPage}
                          pageCount={pageCount}
                          onChange={actions.setPage}
                        />
                      </Box>
                    )}

                    {/* Results count */}
                    {!state.loading && !state.error && (
                      <Flex justify="between" align="center" className="tags-page__footer">
                        <Text size="2" color="gray">
                          Showing {Math.min(state.pageSize, state.tags.length)} of {state.totalItems} tag{state.totalItems !== 1 ? 's' : ''}
                          {state.filter && ` matching "${state.filter}"`}
                        </Text>
                      </Flex>
                    )}
                  </Tabs.Content>

                  <Tabs.Content value="recent">
                    {/* Recent Tags - Could be a filtered view */}
                    <Box py="6" style={{ textAlign: 'center' }}>
                      <Clock size={32} style={{ color: 'var(--gray-8)', marginBottom: 'var(--space-3)' }} />
                      <Text size="2" color="gray" as="p">
                        Recent tags will show tags created or updated in the last 7 days
                      </Text>
                    </Box>
                  </Tabs.Content>
                </Box>
              </Tabs.Root>
            </Card>
          </div>

          {/* Sidebar - Help & Info */}
          <div className="tags-page__sidebar">
            {/* About Tags */}
            <Card className="tags-page__card tags-page__sidebar-card">
              <Flex align="center" gap="2" mb="4" className="tags-page__sidebar-header">
                <HelpCircle size={18} />
                <Heading size="3">{STRINGS.helpTitle}</Heading>
              </Flex>
              
              <Text size="2" as="p" className="tags-page__help-text">
                {STRINGS.helpText}
              </Text>

              <Separator my="4" size="4" />

              <CollapsibleSection
                title={STRINGS.useCaseTitle}
                icon={<Lightbulb size={16} className="tags-page__tip-icon" />}
                defaultOpen={true}
              >
                <Flex direction="column" gap="3" mt="3">
                  {STRINGS.useCases.map((useCase, index) => (
                    <Flex key={index} gap="3" align="start" className="tags-page__usecase">
                      <useCase.icon size={16} className="tags-page__usecase-icon" />
                      <Text size="2">
                        {useCase.text}
                      </Text>
                    </Flex>
                  ))}
                </Flex>
              </CollapsibleSection>

              <Separator my="4" size="4" />

              <Link
                href="https://help.sonatype.com/en/tagging.html"
                target="_blank"
                rel="noopener noreferrer"
                size="2"
                className="tags-page__docs-link"
              >
                <Flex align="center" gap="1">
                  {STRINGS.helpDocsLink}
                  <ExternalLink size={12} />
                </Flex>
              </Link>
            </Card>

            {/* Quick Actions */}
            <Card className="tags-page__card tags-page__sidebar-card tags-page__actions-card">
              <CollapsibleSection
                title="Tag Management"
                icon={<Tags size={16} />}
                defaultOpen={false}
              >
                <Flex direction="column" gap="3" mt="3">
                  <Flex gap="3" align="start">
                    <CheckCircle2 size={16} className="tags-page__action-icon" />
                    <Text size="2">
                      Tags are created when you tag components
                    </Text>
                  </Flex>
                  <Flex gap="3" align="start">
                    <CheckCircle2 size={16} className="tags-page__action-icon" />
                    <Text size="2">
                      Delete tags by removing all tagged components
                    </Text>
                  </Flex>
                  <Flex gap="3" align="start">
                    <CheckCircle2 size={16} className="tags-page__action-icon" />
                    <Text size="2">
                      Use REST API for bulk tag operations
                    </Text>
                  </Flex>
                </Flex>
              </CollapsibleSection>
            </Card>

            {/* Pro Feature Badge */}
            <Card className="tags-page__card tags-page__pro-card">
              <Flex align="center" gap="3">
                <div className="tags-page__pro-badge">
                  <Sparkles size={14} />
                </div>
                <Box>
                  <Text size="2" weight="medium">Pro Feature</Text>
                  <Text size="1" color="gray" as="p" mt="1">
                    Tags are available in Nexus Repository Pro and higher editions.
                  </Text>
                </Box>
              </Flex>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}

export default TagsPage;
