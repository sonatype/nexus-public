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
import React, {useCallback, useEffect, useRef, useState} from 'react';
import {useMachine} from '@xstate/react';
import {
  NxButton,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxIconDropdown,
  NxInfoAlert,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTag,
  NxTextLink,
  NxTooltip,
} from '@sonatype/react-shared-components';
import {faExclamationTriangle} from '@fortawesome/free-solid-svg-icons';
import {
  ContentBody,
  ListMachineUtils,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Section,
  SectionToolbar,
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import ServiceAccountTokensListMachine from './ServiceAccountTokensListMachine';
import ServiceAccountTokensCreateModal from './ServiceAccountTokensCreateModal';
import ServiceAccountTokensTokenModal from './ServiceAccountTokensTokenModal';
import ServiceAccountTokensRevokeModal from './ServiceAccountTokensRevokeModal';
import {canCreateToken, canRevokeToken} from './ServiceAccountTokensHelper';

import './ServiceAccountTokens.scss';

const LABELS = UIStrings.SERVICE_ACCOUNT_TOKENS;

// Selector for a row's actions trigger by token id. Used to restore focus to
// the row that opened the revoke modal when the user cancels.
const getRevokeTrigger = (id) =>
  document.querySelector(`[data-sat-row-id="${id}"] .nx-icon-dropdown__toggle`);

function formatDate(dateString) {
  return new Date(dateString).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

function ExpiresCell({expiresAt, labels}) {
  if (!expiresAt) {
    return <span className="nx-text-color--subtle">{labels.NEVER_EXPIRES}</span>;
  }
  const isExpired = new Date(expiresAt) <= new Date();
  if (isExpired) {
    return (
      <NxTooltip title={`Expired on ${formatDate(expiresAt)}`}>
        <NxTag className="nxrm-sa-expired-badge">
          <NxFontAwesomeIcon icon={faExclamationTriangle} aria-hidden="true" />
          {labels.EXPIRED_BADGE}
        </NxTag>
      </NxTooltip>
    );
  }
  return formatDate(expiresAt);
}

function LastUsedCell({lastUsedAt, neverUsedLabel}) {
  if (!lastUsedAt) {
    return <span className="nx-text-color--subtle">{neverUsedLabel}</span>;
  }
  return formatDate(lastUsedAt);
}

export default function ServiceAccountTokens() {
  const [state, send] = useMachine(ServiceAccountTokensListMachine, {devTools: true});
  const isLoading = state.matches('loading');
  const {data, error, filter: filterText, createdToken, roles, rolesError} = state.context;
  const hasFilter = (filterText ?? '').length > 0;
  const emptyMessage = hasFilter
    ? LABELS.LIST.EMPTY_LIST_FILTERED
    : LABELS.LIST.EMPTY_LIST;

  const nameSortDir = ListMachineUtils.getSortDirection('name', state.context);
  const roleIdSortDir = ListMachineUtils.getSortDirection('roleId', state.context);
  const createdBySortDir = ListMachineUtils.getSortDirection('createdBy', state.context);
  const expiresAtSortDir = ListMachineUtils.getSortDirection('expiresAt', state.context);
  const lastUsedAtSortDir = ListMachineUtils.getSortDirection('lastUsedAt', state.context);

  const sortByName = () => send({type: 'SORT_BY_NAME'});
  const sortByRoleId = () => send({type: 'SORT_BY_ROLE_ID'});
  const sortByCreatedBy = () => send({type: 'SORT_BY_CREATED_BY'});
  const sortByExpiresAt = () => send({type: 'SORT_BY_EXPIRES_AT'});
  const sortByLastUsedAt = () => send({type: 'SORT_BY_LAST_USED_AT'});

  const filter = (value) => send({type: 'FILTER', filter: value});

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [revokeTarget, setRevokeTarget] = useState(null);
  const [openDropdown, setOpenDropdown] = useState(null);

  const canCreate = canCreateToken();
  const canDelete = canRevokeToken();

  const isCreating = state.matches('creating');
  const isRevoking = state.matches('revoking');

  const handleCreate = useCallback((formData) => {
    const payload = {
      name: formData.name,
      roleId: formData.roleId,
    };
    if (formData.description) {
      payload.description = formData.description;
    }
    if (formData.expirationDays != null) {
      payload.expirationDays = formData.expirationDays;
    }
    send({type: 'CREATE_TOKEN', payload});
  }, [send]);

  const wasCreating = useRef(false);
  useEffect(() => {
    if (wasCreating.current && !isCreating) {
      setShowCreateModal(false);
    }
    wasCreating.current = isCreating;
  }, [isCreating]);

  const handleRevoke = useCallback(() => {
    if (!revokeTarget) return;
    send({type: 'REVOKE_TOKEN', tokenId: revokeTarget.id, tokenName: revokeTarget.name});
  }, [revokeTarget, send]);

  const wasRevoking = useRef(false);
  useEffect(() => {
    if (wasRevoking.current && !isRevoking) {
      setRevokeTarget(null);
    }
    wasRevoking.current = isRevoking;
  }, [isRevoking]);

  // Restore focus to the row's actions trigger when the revoke modal closes.
  // After a successful revoke the row is unmounted, so the trigger query
  // returns null and the call no-ops — only cancel restores focus.
  const lastRevokeIdRef = useRef(null);
  useEffect(() => {
    if (revokeTarget) {
      lastRevokeIdRef.current = revokeTarget.id;
    } else if (lastRevokeIdRef.current) {
      getRevokeTrigger(lastRevokeIdRef.current)?.focus();
      lastRevokeIdRef.current = null;
    }
  }, [revokeTarget]);

  const clearCreatedToken = useCallback(() => {
    send({type: 'CLEAR_CREATED_TOKEN'});
  }, [send]);

  const {COLUMNS} = LABELS.LIST;

  return (
    <Page className="nxrm-service-account-tokens">
      <PageHeader>
        <PageTitle
          icon={LABELS.MENU.icon}
          text={LABELS.MENU.text}
          description={LABELS.MENU.description}
        />
        {canCreate && (
          <PageActions>
            <NxButton
              type="button"
              variant="primary"
              onClick={() => setShowCreateModal(true)}
            >
              {LABELS.LIST.CREATE_BUTTON}
            </NxButton>
          </PageActions>
        )}
      </PageHeader>
      <ContentBody className="nxrm-service-account-tokens-list">
        <NxInfoAlert className="nxrm-sa-about-alert">
          <p>{canCreate ? LABELS.LIST.ABOUT.TEXT_ADMIN : LABELS.LIST.ABOUT.TEXT_VIEWER}</p>
          <p>
            <NxTextLink href="https://links.sonatype.com/products/nxrm3/docs/service-account-tokens" external truncate>
              {LABELS.LIST.ABOUT.LINK}
            </NxTextLink>
          </p>
        </NxInfoAlert>
        <Section>
          <SectionToolbar>
            <div className="nxrm-spacer" />
            <NxFilterInput
              id="filter"
              className="nxrm-sa-filter-input"
              onChange={filter}
              value={filterText}
              placeholder={LABELS.LIST.FILTER_PLACEHOLDER}
            />
          </SectionToolbar>
          <NxTable>
            <NxTableHead>
              <NxTableRow>
                <NxTableCell isSortable sortDir={nameSortDir} onClick={sortByName}>{COLUMNS.NAME}</NxTableCell>
                <NxTableCell isSortable sortDir={roleIdSortDir} onClick={sortByRoleId}>{COLUMNS.ROLE}</NxTableCell>
                <NxTableCell isSortable sortDir={createdBySortDir} onClick={sortByCreatedBy}>{COLUMNS.CREATED_BY}</NxTableCell>
                <NxTableCell isSortable sortDir={expiresAtSortDir} onClick={sortByExpiresAt}>{COLUMNS.EXPIRES}</NxTableCell>
                <NxTableCell isSortable sortDir={lastUsedAtSortDir} onClick={sortByLastUsedAt}>{COLUMNS.LAST_USED}</NxTableCell>
                <NxTableCell>{COLUMNS.ACTIONS}</NxTableCell>
              </NxTableRow>
            </NxTableHead>
            <NxTableBody
              isLoading={isLoading}
              error={error}
              emptyMessage={emptyMessage}
              retryHandler={() => send({type: 'RETRY'})}
            >
              {data.map((token) => {
                return (
                  <NxTableRow key={token.id} isClickable tabIndex={0}>
                    <NxTableCell>
                      <span className="nxrm-sa-token-name">{token.name}</span>
                      {token.description && (
                        token.description.length > 50 ? (
                          <NxTooltip title={token.description} className="nxrm-sa-token-description-tooltip">
                            <span className="nxrm-sa-token-description nxrm-sa-token-description--truncated">
                              {token.description}
                            </span>
                          </NxTooltip>
                        ) : (
                          <span className="nxrm-sa-token-description">{token.description}</span>
                        )
                      )}
                    </NxTableCell>
                    <NxTableCell>
                      <code className="nxrm-sa-role-badge">{token.roleId}</code>
                    </NxTableCell>
                    <NxTableCell>{token.createdBy}</NxTableCell>
                    <NxTableCell><ExpiresCell expiresAt={token.expiresAt} labels={COLUMNS} /></NxTableCell>
                    <NxTableCell><LastUsedCell lastUsedAt={token.lastUsedAt} neverUsedLabel={COLUMNS.LAST_USED_NEVER} /></NxTableCell>
                    <NxTableCell data-sat-row-id={token.id}>
                      {canDelete && (
                        <NxIconDropdown
                          title={openDropdown === token.id ? undefined : 'Actions'}
                          isOpen={openDropdown === token.id}
                          onToggleCollapse={() =>
                            setOpenDropdown(openDropdown === token.id ? null : token.id)
                          }
                          onCloseClick={() => setOpenDropdown(null)}
                          onCloseKeyDown={() => setOpenDropdown(null)}
                        >
                          <button
                            className="nx-dropdown-button"
                            onClick={() => {
                              setOpenDropdown(null);
                              setRevokeTarget(token);
                            }}
                          >
                            {LABELS.LIST.ACTIONS.REVOKE}
                          </button>
                        </NxIconDropdown>
                      )}
                    </NxTableCell>
                  </NxTableRow>
                );
              })}
            </NxTableBody>
          </NxTable>
        </Section>
      </ContentBody>

      {showCreateModal && (
        <ServiceAccountTokensCreateModal
          onClose={() => setShowCreateModal(false)}
          onCreate={handleCreate}
          roles={roles}
          rolesError={rolesError}
          existingNames={data.map((t) => t.name)}
          isCreating={isCreating}
        />
      )}

      {createdToken && (
        <ServiceAccountTokensTokenModal
          token={createdToken.rawToken}
          onClose={clearCreatedToken}
        />
      )}

      {revokeTarget && (
        <ServiceAccountTokensRevokeModal
          tokenName={revokeTarget.name}
          onConfirm={handleRevoke}
          onClose={() => setRevokeTarget(null)}
          isRevoking={isRevoking}
        />
      )}
    </Page>
  );
}
