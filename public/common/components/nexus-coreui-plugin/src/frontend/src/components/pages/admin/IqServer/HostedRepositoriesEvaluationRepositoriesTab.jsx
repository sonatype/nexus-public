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
import React, {useState, useRef, useEffect, useCallback, useMemo} from 'react';
import {useMachine} from '@xstate/react';
import {SectionToolbar, HumanReadableUtils} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxCheckbox,
  NxFilterInput,
  NxFormSelect,
  NxLoadWrapper,
  NxModal,
  NxH3,
  NxP,
  NxPagination,
  NxTable,
  NxTableHead,
  NxTableRow,
  NxTableCell,
  NxTableBody,
  NxWarningAlert,
  NxCloseButton
} from '@sonatype/react-shared-components';

import HostedRepositoriesEvaluationMachine
  from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/HostedRepositoriesEvaluationMachine';
import UIStrings from '../../../../constants/UIStrings';

import './HostedRepositoriesEvaluationRepositoriesTab.scss';

const {HOSTED_REPOSITORIES_EVALUATION} = UIStrings.SONATYPE_LIFECYCLE;

export default function HostedRepositoriesEvaluationRepositoriesTab({onBack, settingsData, initialSelectedRepositories = [], onSelectionChange}) {
  const [current, send] = useMachine(HostedRepositoriesEvaluationMachine, {devTools: true});
  const [showIncompleteModal, setShowIncompleteModal] = useState(false);
  const [searchInput, setSearchInput] = useState('');
  const debounceRef = useRef(null);
  const lastSyncedSelection = useRef([]);
  const lastSyncedSelectionString = useRef('');
  const [selectedRepositories, setSelectedRepositories] = useState([]);

  const {data, repositories, loadError, formatFilter, offsetPage, sortField, sortDirection} = current.context;
  const isLoading = current.matches('loading');

  useEffect(() => {
    setSelectedRepositories(initialSelectedRepositories);
  }, [initialSelectedRepositories]);

  const selectionString = useMemo(() =>
    JSON.stringify([...selectedRepositories].sort()),
    [selectedRepositories]
  );

  useEffect(() => {
    if (onSelectionChange && selectionString !== lastSyncedSelectionString.current) {
      lastSyncedSelection.current = [...selectedRepositories];
      lastSyncedSelectionString.current = selectionString;
      onSelectionChange(selectedRepositories);
    }

    send({type: 'UPDATE', data: {selectedRepositories}});
  }, [selectionString, selectedRepositories, onSelectionChange, send]);

  useEffect(() => {
    return () => {
      if (debounceRef.current) {
        clearTimeout(debounceRef.current);
      }
    };
  }, []);

  const handleSubmit = useCallback(() => {
    if (!selectedRepositories || selectedRepositories.length === 0) {
      setShowIncompleteModal(true);
      return;
    }
    // TODO: CLM-38795 - Implement backend API for saving repository selection
    send({type: 'UPDATE', data: {selectedRepositories, settings: settingsData}});
    send('SAVE');
  }, [settingsData, selectedRepositories, send]);

  const handleCancelModal = useCallback(() => {
    setShowIncompleteModal(false);
  }, []);

  const handleContinueWithoutSelection = useCallback(() => {
    setShowIncompleteModal(false);
    if (onBack) {
      onBack();
    }
  }, [onBack]);

  const handleRepositorySelect = useCallback((repositoryId) => {
    setSelectedRepositories(currentSelection => {
      const isSelected = currentSelection.includes(repositoryId);
      const newSelection = isSelected
        ? currentSelection.filter(id => id !== repositoryId)
        : [...currentSelection, repositoryId];
      return newSelection;
    });
  }, []);

  const handleSelectAll = useCallback(() => {
    const currentPagedRepos = repositories || [];
    setSelectedRepositories(currentSelection => {
      const allSelected = currentPagedRepos.every(repo => currentSelection.includes(repo.id));
      const newSelection = allSelected
        ? currentSelection.filter(id => !currentPagedRepos.find(repo => repo.id === id))
        : [...new Set([...currentSelection, ...currentPagedRepos.map(repo => repo.id)])];
      return newSelection;
    });
  }, [repositories]);

  const handleFilterChange = useCallback((value) => {
    setSearchInput(value);

    if (debounceRef.current) {
      clearTimeout(debounceRef.current);
    }
    debounceRef.current = setTimeout(() => {
      send({type: 'FILTER', filter: value});
    }, 500);
  }, [send]);

  const handleFormatFilter = useCallback((value) => {
    send({type: 'FILTER_FORMAT', formatFilter: value});
  }, [send]);

  const handlePageChange = useCallback((page) => {
    send({type: 'CHANGE_PAGE', offsetPage: page});
  }, [send]);

  const handleSort = useCallback((field) => {
    const newDirection = sortField === field && sortDirection === 'asc' ? 'desc' : 'asc';
    send({type: 'SORT', sortField: field, sortDirection: newDirection});
  }, [sortField, sortDirection, send]);

  const handleRetry = useCallback(() => send('RETRY'), [send]);

  const handleSortName = useCallback(() => handleSort('name'), [handleSort]);
  const handleSortFormat = useCallback(() => handleSort('format'), [handleSort]);
  const handleSortSize = useCallback(() => handleSort('size'), [handleSort]);
  const handleSortComponents = useCallback(() => handleSort('number_of_components'), [handleSort]);

  // Backend handles filtering, pagination, sorting, and format options
  const pagedRepositories = repositories || [];
  const totalCount = current.context.totalCount || 0;
  const pages = current.context.totalPages || 1;
  const currentPage = offsetPage || 0;
  const formats = useMemo(() => current.context.formats || ['all'], [current.context.formats]);

  const allPagedSelected = pagedRepositories.length > 0 && pagedRepositories.every(repo => selectedRepositories.includes(repo.id));

  return (
    <>
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={handleRetry}>
        <div className="nx-form hosted-repository-evaluation-tab">
          <SectionToolbar>
            <NxFilterInput
              className="nx-text-input--long"
              placeholder="Search repositories..."
              value={searchInput}
              onChange={handleFilterChange}
              searchIcon={true}
            />
            <div className="nxrm-spacer" />
            <NxFormSelect
              value={formatFilter || 'all'}
              onChange={handleFormatFilter}
            >
              {formats.map(fmt => (
                <option key={fmt} value={fmt}>
                  {fmt === 'all' ? 'Format' : fmt}
                </option>
              ))}
            </NxFormSelect>
          </SectionToolbar>

          <NxP>
            Showing {pagedRepositories.length} of {totalCount} repositories
          </NxP>

          <NxTable>
            <NxTableHead>
              <NxTableRow>
                <NxTableCell className="nx-cell-select">
                  <NxCheckbox
                    checkboxId="select-all-repositories"
                    isChecked={allPagedSelected}
                    onChange={handleSelectAll}
                    aria-label="Select all repositories on this page"
                  />
                </NxTableCell>
                <NxTableCell
                  isSortable
                  sortDir={sortField === 'name' ? sortDirection : null}
                  onClick={handleSortName}
                >
                  Repository Name
                </NxTableCell>
                <NxTableCell
                  isSortable
                  sortDir={sortField === 'format' ? sortDirection : null}
                  onClick={handleSortFormat}
                >
                  Format
                </NxTableCell>
                <NxTableCell
                  isSortable
                  sortDir={sortField === 'size' ? sortDirection : null}
                  onClick={handleSortSize}
                >
                  Size
                </NxTableCell>
                <NxTableCell
                  isSortable
                  sortDir={sortField === 'number_of_components' ? sortDirection : null}
                  onClick={handleSortComponents}
                >
                  No. Components
                </NxTableCell>
              </NxTableRow>
            </NxTableHead>
            <NxTableBody emptyMessage="No hosted repositories available">
              {pagedRepositories.map(repo => (
                <NxTableRow key={repo.id}>
                  <NxTableCell className="nx-cell-select">
                    <NxCheckbox
                      checkboxId={`select-repo-${repo.id}`}
                      isChecked={selectedRepositories.includes(repo.id)}
                      onChange={() => handleRepositorySelect(repo.id)}
                      aria-label={`Select repository ${repo.name}`}
                    />
                  </NxTableCell>
                  <NxTableCell>{repo.name}</NxTableCell>
                  <NxTableCell>{repo.format}</NxTableCell>
                  <NxTableCell>{repo.size ? HumanReadableUtils.bytesToString(repo.size) : '-'}</NxTableCell>
                  <NxTableCell>{repo.artifactCount || '-'}</NxTableCell>
                </NxTableRow>
              ))}
            </NxTableBody>
          </NxTable>

          {pages > 1 && (
            <NxPagination
              onChange={handlePageChange}
              pageCount={pages}
              currentPage={currentPage}
            />
          )}

          <div className="nx-btn-bar">
            {onBack && (
              <NxButton onClick={onBack}>
                {UIStrings.SETTINGS.BACK_BUTTON_LABEL}
              </NxButton>
            )}
            <NxButton variant="primary" onClick={handleSubmit}>
              {UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
            </NxButton>
          </div>
        </div>
      </NxLoadWrapper>

      {showIncompleteModal && (
        <NxModal onCancel={handleCancelModal} variant="narrow">
          <header className="nx-modal-header">
            <NxH3>{HOSTED_REPOSITORIES_EVALUATION.INCOMPLETE_MODAL.TITLE}</NxH3>
            <NxCloseButton onClick={handleCancelModal} />
          </header>
          <div className="nx-modal-content">
            <NxWarningAlert>
              {HOSTED_REPOSITORIES_EVALUATION.INCOMPLETE_MODAL.MESSAGE}
            </NxWarningAlert>
          </div>
          <footer className="nx-footer">
            <div className="nx-btn-bar">
              <NxButton onClick={handleCancelModal}>
                {HOSTED_REPOSITORIES_EVALUATION.INCOMPLETE_MODAL.CANCEL}
              </NxButton>
              <NxButton variant="primary" onClick={handleContinueWithoutSelection}>
                {HOSTED_REPOSITORIES_EVALUATION.INCOMPLETE_MODAL.CONTINUE}
              </NxButton>
            </div>
          </footer>
        </NxModal>
      )}
    </>
  );
}
