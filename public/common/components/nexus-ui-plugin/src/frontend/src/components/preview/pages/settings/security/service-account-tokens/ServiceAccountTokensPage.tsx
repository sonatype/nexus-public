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

import React, { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { Box, Flex, Text, VisuallyHidden } from '@radix-ui/themes';
import { Plus, MoreHorizontal, KeyRound } from 'lucide-react';
import * as Tooltip from '@radix-ui/react-tooltip';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';

import { ExtJS } from '../../../../../../interface/ExtJS';
import Permissions from '../../../../../../constants/Permissions';
import { PageHeader, EmptyState, HelpSection, EntityTable, useToast } from '../../../../shared';
import type { TableColumn } from '../../../../shared';
import { SettingsButton, SettingsAlert } from '../../../../shared/form';
import { useServiceAccountTokens } from './useServiceAccountTokens';
import { CreateTokenModal } from './CreateTokenModal';
import { RevealTokenModal } from './RevealTokenModal';
import { RevokeTokenModal } from './RevokeTokenModal';
import { ServiceAccountToken } from './types';
import { ExpiresCell } from './ExpiresCell';
import { SERVICE_ACCOUNT_TOKENS_STRINGS } from './strings';

import './ServiceAccountTokensPage.scss';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.PAGE;
const MESSAGES = SERVICE_ACCOUNT_TOKENS_STRINGS.MESSAGES;

const navigateTo = (path: string) => {
  window.location.hash = path;
};

// Selector for the Create Token button. Returned to modals via `getRestoreFocus`
// so they can move focus here on close. Querying by testid (instead of a ref)
// avoids piping refs through SettingsButton.
const getCreateButton = (): HTMLElement | null =>
  document.querySelector('[data-testid="sat-create-button"]');

// Selector for a row's actions trigger by token id. Used to restore focus to
// the row that opened the revoke modal when the user cancels.
const getRevokeTrigger = (id: string): HTMLElement | null =>
  document.querySelector(`[data-testid="sat-actions-${id}"]`);

function formatDate(dateString: string | null): string {
  if (!dateString) return '—';
  return new Date(dateString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

type SortableColumn = 'name' | 'roleId' | 'createdBy' | 'expiresAt' | 'lastUsedAt';

function compareTokens(a: ServiceAccountToken, b: ServiceAccountToken, column: SortableColumn): number {
  if (column === 'expiresAt' || column === 'lastUsedAt') {
    const av = a[column];
    const bv = b[column];
    if (av === bv) return 0;
    if (av === null) return 1;
    if (bv === null) return -1;
    return new Date(av).getTime() - new Date(bv).getTime();
  }
  return (a[column] || '').toString().localeCompare((b[column] || '').toString());
}

export function ServiceAccountTokensPage() {
  const canCreate = ExtJS.checkPermission(Permissions.SERVICE_ACCOUNTS.CREATE);
  const canDelete = ExtJS.checkPermission(Permissions.SERVICE_ACCOUNTS.DELETE);
  const { tokens, roles, rolesError, loading, error, setError, loadAll, createToken, revokeToken } =
    useServiceAccountTokens({ canCreate });
  const toast = useToast();

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const [filter, setFilter] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [revealedToken, setRevealedToken] = useState<string | null>(null);
  const [revokeTarget, setRevokeTarget] = useState<ServiceAccountToken | null>(null);
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);
  const [sortBy, setSortBy] = useState<SortableColumn>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  const filteredTokens = useMemo(() => {
    const sorted = [...tokens].sort((a, b) => {
      const cmp = compareTokens(a, b, sortBy);
      return sortDirection === 'asc' ? cmp : -cmp;
    });
    if (!filter.trim()) return sorted;
    const lower = filter.toLowerCase();
    return sorted.filter(
      (t) =>
        t.name.toLowerCase().includes(lower) ||
        t.roleId.toLowerCase().includes(lower) ||
        t.createdBy?.toLowerCase().includes(lower)
    );
  }, [tokens, filter, sortBy, sortDirection]);

  const handleSort = useCallback(
    (columnId: string) => {
      const col = columnId as SortableColumn;
      if (sortBy === col) {
        setSortDirection((d) => (d === 'asc' ? 'desc' : 'asc'));
      } else {
        setSortBy(col);
        setSortDirection('asc');
      }
    },
    [sortBy]
  );

  const handleCreate = useCallback(
    async (form: Parameters<typeof createToken>[0]) => {
      try {
        const { rawToken } = await createToken(form);
        setShowCreateModal(false);
        setRevealedToken(rawToken);
        toast.success(MESSAGES.CREATE_SUCCESS(form.name));
      } catch (err: any) {
        toast.error(err?.message || MESSAGES.CREATE_ERROR_GENERIC);
      }
    },
    [createToken, toast]
  );

  const handleRevoke = useCallback(async () => {
    if (!revokeTarget) return;
    const { id, name } = revokeTarget;
    try {
      await revokeToken(id);
      setRevokeTarget(null);
      toast.success(MESSAGES.REVOKE_SUCCESS(name));
    } catch (err: any) {
      toast.error(err?.message || MESSAGES.REVOKE_ERROR_GENERIC);
    }
  }, [revokeTarget, revokeToken, toast]);

  // Snapshot the row id whenever the revoke modal opens, so the modal's
  // onCloseAutoFocus has a stable id to look up — by the time it fires, the
  // revokeTarget state is already null. On successful revoke the row is gone,
  // so the trigger query returns null and Radix's default focus-restore wins.
  const lastRevokeIdRef = useRef<string | null>(null);
  useEffect(() => {
    if (revokeTarget) lastRevokeIdRef.current = revokeTarget.id;
  }, [revokeTarget]);

  const columns: TableColumn<ServiceAccountToken>[] = [
    {
      id: 'name',
      header: LABELS.COLUMNS.NAME,
      sortable: true,
      accessor: (token) => (
        <Flex direction="column" gap="1">
          <Text size="2" weight="medium">{token.name}</Text>
          {token.description && (
            token.description.length > 50 ? (
              <Tooltip.Provider delayDuration={200}>
                <Tooltip.Root>
                  <Tooltip.Trigger asChild>
                    <Text
                      size="1"
                      color="gray"
                      className="sat-token-description sat-token-description--truncated"
                      data-testid="sat-description-tooltip-trigger"
                    >
                      {token.description}
                    </Text>
                  </Tooltip.Trigger>
                  <Tooltip.Portal>
                    <Tooltip.Content
                      className="sat-token-description-tooltip"
                      sideOffset={6}
                    >
                      {token.description}
                      <Tooltip.Arrow className="sat-token-description-tooltip-arrow" />
                    </Tooltip.Content>
                  </Tooltip.Portal>
                </Tooltip.Root>
              </Tooltip.Provider>
            ) : (
              <Text size="1" color="gray" className="sat-token-description">
                {token.description}
              </Text>
            )
          )}
        </Flex>
      ),
    },
    {
      id: 'roleId',
      header: LABELS.COLUMNS.ROLE,
      sortable: true,
      accessor: (token) => (
        <Text size="2">
          <code className="sat-page__role-badge">{token.roleId}</code>
        </Text>
      ),
    },
    {
      id: 'createdBy',
      header: LABELS.COLUMNS.CREATED_BY,
      sortable: true,
      accessor: (token) => <Text size="2">{token.createdBy}</Text>,
    },
    {
      id: 'expiresAt',
      header: LABELS.COLUMNS.EXPIRES,
      sortable: true,
      accessor: (token) => <ExpiresCell expiresAt={token.expiresAt} />,
    },
    {
      id: 'lastUsedAt',
      header: LABELS.COLUMNS.LAST_USED,
      sortable: true,
      accessor: (token) => (
        <Text size="2" color={token.lastUsedAt ? undefined : 'gray'}>
          {token.lastUsedAt ? formatDate(token.lastUsedAt) : LABELS.LAST_USED_NEVER}
        </Text>
      ),
    },
    {
      id: 'actions',
      header: '',
      width: '48px',
      align: 'right',
      accessor: (token) => !canDelete ? null : (
        <Box className="sat-page__actions-cell">
          <Tooltip.Provider delayDuration={200}>
            <Tooltip.Root open={openDropdown === token.id ? false : undefined}>
              <DropdownMenu.Root
                open={openDropdown === token.id}
                onOpenChange={(o) => setOpenDropdown(o ? token.id : null)}
              >
                <Tooltip.Trigger asChild>
                  <DropdownMenu.Trigger asChild>
                    <button
                      className="sat-page__action-trigger"
                      aria-label={LABELS.ACTIONS_BUTTON_LABEL}
                      data-testid={`sat-actions-${token.id}`}
                    >
                      <MoreHorizontal size={16} />
                    </button>
                  </DropdownMenu.Trigger>
                </Tooltip.Trigger>
                <Tooltip.Portal>
                  <Tooltip.Content className="sat-page__tooltip" sideOffset={4}>
                    {LABELS.ACTIONS_BUTTON_LABEL}
                    <Tooltip.Arrow className="sat-page__tooltip-arrow" />
                  </Tooltip.Content>
                </Tooltip.Portal>
                <DropdownMenu.Portal>
                  <DropdownMenu.Content
                    align="end"
                    sideOffset={4}
                    className="sat-page__dropdown"
                    data-testid={`sat-dropdown-${token.id}`}
                    onCloseAutoFocus={(e) => e.preventDefault()}
                  >
                    <DropdownMenu.Item
                      className="sat-page__dropdown-item sat-page__dropdown-item--danger"
                      onSelect={() => setRevokeTarget(token)}
                    >
                      {LABELS.REVOKE_ACTION}
                    </DropdownMenu.Item>
                  </DropdownMenu.Content>
                </DropdownMenu.Portal>
              </DropdownMenu.Root>
            </Tooltip.Root>
          </Tooltip.Provider>
        </Box>
      ),
    },
  ];

  const emptyStateNode = (
    <EmptyState
      icon={KeyRound}
      title={filter ? LABELS.EMPTY.TITLE_NO_MATCH : LABELS.EMPTY.TITLE_NO_TOKENS}
      description={
        filter ? LABELS.EMPTY.DESCRIPTION_NO_MATCH : LABELS.EMPTY.DESCRIPTION_NO_TOKENS
      }
      action={
        !filter && canCreate
          ? {
              label: LABELS.CREATE_BUTTON,
              onClick: () => setShowCreateModal(true),
            }
          : undefined
      }
    />
  );

  return (
    <Box
      className="sat-page"
      data-testid="service-account-tokens-page"
      px={{ initial: '4', md: '6', lg: '6' }}
      py={{ initial: '4', md: '5', lg: '6' }}
    >
      <Box mb="4">
        <PageHeader
          title={LABELS.TITLE}
          description={LABELS.DESCRIPTION}
          breadcrumbs={[
            { label: LABELS.BREADCRUMB_SETTINGS, onClick: () => navigateTo('#preview/admin/settings') },
            { label: LABELS.TITLE },
          ]}
        />
      </Box>

      {error && (
        <Box mb="4">
          <SettingsAlert type="error" onClose={() => setError(null)}>
            {error}
          </SettingsAlert>
        </Box>
      )}

      <Box mb="4">
        <HelpSection
          className="sat-help-section"
          title={LABELS.ABOUT.TITLE}
          content={canCreate ? LABELS.ABOUT.TEXT_ADMIN : LABELS.ABOUT.TEXT_VIEWER}
          docLink={{
            label: LABELS.ABOUT.LINK,
            href: LABELS.ABOUT.LINK_HREF,
          }}
        />
      </Box>

      <Flex
        justify="between"
        align="center"
        mb="4"
        className="sat-page__toolbar"
        role="search"
      >
        <Box className="sat-page__filter">
          <VisuallyHidden>
            <label htmlFor="sat-filter-input">{LABELS.FILTER_LABEL}</label>
          </VisuallyHidden>
          <input
            id="sat-filter-input"
            type="text"
            className="sat-page__filter-input"
            placeholder={LABELS.FILTER_PLACEHOLDER}
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            aria-label={LABELS.FILTER_LABEL}
            data-testid="sat-filter-input"
          />
        </Box>
        {canCreate && (
          <SettingsButton
            variant="primary"
            onClick={() => setShowCreateModal(true)}
            icon={Plus}
            data-testid="sat-create-button"
          >
            {LABELS.CREATE_BUTTON}
          </SettingsButton>
        )}
      </Flex>

      <Box data-testid="sat-table" role="region" aria-label={LABELS.TABLE_REGION_LABEL}>
        <EntityTable<ServiceAccountToken>
          data={filteredTokens}
          columns={columns}
          getRowKey={(token) => token.id}
          getRowTestId={(token) => `sat-row-${token.id}`}
          clickable={false}
          focusableRows
          getRowAriaLabel={(token) =>
            [
              token.name,
              token.roleId,
              token.createdBy,
              token.expiresAt
                ? `expires ${formatDate(token.expiresAt)}`
                : LABELS.COLUMNS.EXPIRES + ' never',
              token.lastUsedAt
                ? `last used ${formatDate(token.lastUsedAt)}`
                : LABELS.LAST_USED_NEVER,
            ].join(', ')
          }
          showRowArrow={false}
          ariaLabel={LABELS.TABLE_ARIA_LABEL}
          sortBy={sortBy}
          sortDirection={sortDirection}
          onSort={handleSort}
          loading={loading && tokens.length === 0}
          loadingMessage={LABELS.LOADING}
          emptyState={emptyStateNode}
        />
      </Box>

      <CreateTokenModal
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onCreate={handleCreate}
        roles={roles}
        rolesError={rolesError}
        existingNames={tokens.map((t) => t.name)}
        loading={loading}
        getRestoreFocus={getCreateButton}
      />

      <RevealTokenModal
        open={!!revealedToken}
        token={revealedToken || ''}
        onClose={() => setRevealedToken(null)}
        getRestoreFocus={getCreateButton}
      />

      <RevokeTokenModal
        open={!!revokeTarget}
        tokenName={revokeTarget?.name || ''}
        onConfirm={handleRevoke}
        onClose={() => setRevokeTarget(null)}
        loading={loading}
        getRestoreFocus={() => {
          const id = lastRevokeIdRef.current;
          return id ? getRevokeTrigger(id) : null;
        }}
      />
    </Box>
  );
}

export default ServiceAccountTokensPage;
