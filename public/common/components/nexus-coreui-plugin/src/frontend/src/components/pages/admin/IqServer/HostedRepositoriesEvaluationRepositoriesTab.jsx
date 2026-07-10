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
import {useRouter} from '@uirouter/react';
import {SectionToolbar, HumanReadableUtils, ExtJS} from '@sonatype/nexus-ui-plugin';
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
  NxErrorAlert,
  NxCloseButton,
  NxSmallTag,
  NxTextLink
} from '@sonatype/react-shared-components';

import HostedRepositoriesEvaluationMachine
  from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/HostedRepositoriesEvaluationMachine';
import UIStrings from '../../../../constants/UIStrings';
import {ROUTE_NAMES} from '../../../../routerConfig/routeNames/routeNames';

import './HostedRepositoriesEvaluationRepositoriesTab.scss';

const {HOSTED_REPOSITORIES_EVALUATION} = UIStrings.SONATYPE_LIFECYCLE;

/**
 * Compare versionDepth values, normalizing both to numbers.
 * Returns true if both values are different (and both are valid numbers).
 * Used to detect if versionDepth has changed between settingsData and existingSettings.
 * @param {*} a - First value (may be string, number, null, or undefined)
 * @param {*} b - Second value (may be string, number, null, or undefined)
 * @returns {boolean} - true if both are valid numbers and NOT equal (i.e., changed)
 */
function versionDepthChanged(a, b) {
  const numA = Number(a);
  const numB = Number(b);
  // If either is NaN, consider them equal (no change) to avoid false positives
  if (isNaN(numA) || isNaN(numB)) {
    return false;
  }
  return numA !== numB;
}

/**
 * Repository selection tab for hosted repositories evaluation configuration.
 *
 * State Machine Contract:
 * This component expects HostedRepositoriesEvaluationMachine to provide:
 * - context.repositories: Array of {id, name, format, size, artifactCount}
 * - context.totalCount: Total number of repositories (for pagination)
 * - context.totalPages: Total number of pages
 * - context.formats: Array of available format filters
 * - context.saveError: Error object with optional response.data.message or message property
 * - context.loadError: Error string for data fetch failures
 * - States: 'loading', 'loaded', 'saving'
 * - Events: FILTER, FILTER_FORMAT, CHANGE_PAGE, SORT, UPDATE, SAVE, RETRY
 */
export default function HostedRepositoriesEvaluationRepositoriesTab({
  settingsData,
  initialSelectedRepositories = [],
  onSelectionChange,
  globalConfigAvailable = false,
  onBack
}) {
  const router = useRouter();
  // Child machine instance - independent from parent's machine instance.
  // Both parent and child share the same machine DEFINITION, but create separate
  // INSTANCES with independent state. This is standard XState + React pattern.
  // After successful PATCH, this component redirects user (line 124), so no need
  // to refetch data here. When user returns, this component remounts, creating a
  // fresh instance that fetches the latest repository list with correct sort order.
  const [current, send] = useMachine(HostedRepositoriesEvaluationMachine, {devTools: true});
  const [incompleteSelectionError, setIncompleteSelectionError] = useState(false);
  const [showErrorModal, setShowErrorModal] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const debounceRef = useRef(null);
  const lastSyncedSelectionString = useRef('');
  const hasInitializedSelection = useRef(false);
  const [selectedRepositories, setSelectedRepositories] = useState([]);
  const [pendingMonitoringChanges, setPendingMonitoringChanges] = useState({});
  // Tracks the baseline monitoring status (isSelected) for every repo we've seen,
  // across all filters/pages. Updated whenever repositories loads new data.
  const [repoStatusMap, setRepoStatusMap] = useState({});

  const {repositories, loadError, formatFilter, monitoringFilter, offsetPage, sortField, sortDirection, numberOfMonitoredRepositories, hasSelections: hasSelectionsContext, existingSettings} = current.context;
  const hasSelections = globalConfigAvailable || hasSelectionsContext || false;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving') || current.matches('patching');
  const isLoaded = current.matches('loaded');
  const wasSavingRef = useRef(false);

  useEffect(() => {
    if (hasInitializedSelection.current) {
      return;
    }
    if (hasSelections && repositories && repositories.length > 0) {
      const preSelectedIds = repositories.filter(repo => repo.isSelected).map(repo => repo.id);
      setSelectedRepositories(preSelectedIds);
      hasInitializedSelection.current = true;
    } else if (!hasSelections) {
      setSelectedRepositories(initialSelectedRepositories);
      hasInitializedSelection.current = true;
    }
  }, [hasSelections, repositories, initialSelectedRepositories]);

  useEffect(() => {
    if (repositories && repositories.length > 0) {
      setRepoStatusMap(prev => {
        const updated = {...prev};
        repositories.forEach(repo => {
          updated[repo.id] = repo.isSelected;
        });
        return updated;
      });
    }
  }, [repositories]);

  const selectionString = useMemo(() =>
    JSON.stringify([...selectedRepositories].sort()),
    [selectedRepositories]
  );

  useEffect(() => {
    if (onSelectionChange && selectionString !== lastSyncedSelectionString.current) {
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

  useEffect(() => {
    // Access error directly from context to avoid stale closure
    const currentSaveError = current.context.saveError;

    // After successful PATCH/PUT, redirect to Lifecycle page.
    // This means:
    // 1. User never sees stale badge count or repository list
    // 2. When user returns, components remount and fetch fresh data
    // 3. No need to refetch before redirect (avoid wasted API calls)
    // 4. Badge and repository list always show latest state when visible
    if (wasSavingRef.current && !isSaving && isLoaded && !currentSaveError) {
      ExtJS.setDirtyStatus('HostedRepositoriesEvaluationMachine', false);
      setPendingMonitoringChanges({});
      router.stateService.go(ROUTE_NAMES.ADMIN.IQ.SONATYPE_LIFECYCLE.ROOT);
    }
    wasSavingRef.current = isSaving;
  }, [isSaving, isLoaded, current.context.saveError, router]);

  useEffect(() => {
    const error = current.context.saveError;
    if (error) {
      const message = (typeof error?.response?.data?.message === 'string' && error.response.data.message) ||
                      (typeof error?.message === 'string' && error.message) ||
                      HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.MESSAGE;
      setErrorMessage(message);
      setShowErrorModal(true);
    }
  }, [current.context.saveError]);

  const handleSubmit = useCallback(async () => {
    if (hasSelections) {
      // Update button (returning users) → PATCH
      // Monitoring changes come only from explicit Enable/Disable Monitoring button clicks,
      // not from checkbox selection (checkboxes are used for batch selection only).
      // Filter out net-zero changes (e.g. disable then re-enable same repo = no actual change)
      const repositoriesToAdd = Object.entries(pendingMonitoringChanges)
        .filter(([id, enabled]) => enabled && !(repoStatusMap[id] || false))
        .map(([id]) => id);
      const repositoriesToRemove = Object.entries(pendingMonitoringChanges)
        .filter(([id, enabled]) => !enabled && (repoStatusMap[id] || false))
        .map(([id]) => id);

      const changedSettings = {};
      if (existingSettings) {
        if (settingsData.activityTimeFrame !== null && settingsData.activityTimeFrame !== undefined &&
            settingsData.activityTimeFrame !== existingSettings.activityTimeFrame) {
          changedSettings.activityTimeFrame = settingsData.activityTimeFrame;
        }
        if (settingsData.artifactLatestVersions !== null && settingsData.artifactLatestVersions !== undefined &&
            settingsData.artifactLatestVersions !== existingSettings.artifactLatestVersions) {
          changedSettings.artifactLatestVersions = settingsData.artifactLatestVersions;
        }
        // Both settingsData.policyEvaluationStage (normalized in SettingsTab) and
        // existingSettings.policyEvaluationStage (from backend) are uppercase+underscore (e.g. RELEASE)
        if (settingsData.policyEvaluationStage !== null && settingsData.policyEvaluationStage !== undefined &&
            settingsData.policyEvaluationStage !== existingSettings.policyEvaluationStage) {
          changedSettings.policyEvaluationStage = settingsData.policyEvaluationStage;
        }
        if (settingsData.applyToNewRepos !== existingSettings.autoEnrollNewRepos) {
          changedSettings.applyToNewRepos = settingsData.applyToNewRepos;
        }
        if (versionDepthChanged(settingsData.versionDepth, existingSettings.versionDepth)) {
          changedSettings.versionDepth = Number(settingsData.versionDepth);
        }
      }

      const hasChanges = Object.keys(changedSettings).length > 0 || repositoriesToAdd.length > 0 || repositoriesToRemove.length > 0;

      if (!hasChanges) {
        setShowErrorModal(true);
        setErrorMessage('No changes to save');
        return;
      }

      send({
        type: 'UPDATE',
        data: {
          settings: changedSettings,
          repositoriesToAdd,
          repositoriesToRemove
        }
      });
      send('PATCH');
    } else {
      // Save button (first-time users) → PUT
      // Require at least one repository for first-time setup
      if (!selectedRepositories || selectedRepositories.length === 0) {
        setIncompleteSelectionError(true);
        return;
      }

      send({type: 'UPDATE', data: {selectedRepositories, settings: settingsData}});
      send('SAVE');
    }
  }, [settingsData, selectedRepositories, send, hasSelections, repoStatusMap, existingSettings, pendingMonitoringChanges]);

  const handleCloseErrorModal = useCallback(() => {
    setShowErrorModal(false);
    setErrorMessage('');
  }, []);

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

  const handleMonitoringFilter = useCallback((value) => {
    send({type: 'FILTER_MONITORING', monitoringFilter: value});
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

  const handleRepositoryLinkClick = useCallback((e) => {
    e.preventDefault();

    const href = e.currentTarget.getAttribute('href');
    const targetHash = href.substring(1);

    // Clear dirty state to prevent "unsaved changes" warning during navigation
    ExtJS.setDirtyStatus('HostedRepositoriesEvaluationMachine', false);

    const baseRoute = 'admin/repository/repositories';
    const returnUrl = router.stateService.href(ROUTE_NAMES.ADMIN.IQ.HOSTED_REPOS_EVAL.ROOT, {activeTab: '1'});
    const returnHash = returnUrl.substring(1);

    if (window.Ext?.History) {
      window.Ext.History.add(returnHash);
      window.Ext.History.add(baseRoute);
    } else {
      window.location.hash = returnHash;
      window.location.hash = baseRoute;
    }

    // Two-step navigation required for React → ExtJS Drilldown boundary.
    // ExtJS Drilldown's navigateTo() returns early if feature view isn't mounted.
    // 100ms delay allows ExtJS History to initialize before navigating to detail page.
    // Timing determined through testing - shorter delays cause navigation failures.
    setTimeout(() => {
      const currentUrl = window.location.href.split('#')[0];
      window.location.replace(`${currentUrl}#${targetHash}`);
    }, 100);
  }, [router]);

  const handleEnableMonitoring = useCallback(() => {
    setPendingMonitoringChanges(prev => {
      const updated = {...prev};
      selectedRepositories.forEach(id => {
        const alreadyEnabled = prev[id] !== undefined ? prev[id] : (repoStatusMap[id] || false);
        if (!alreadyEnabled) {
          updated[id] = true;
        }
      });
      return updated;
    });
  }, [selectedRepositories, repoStatusMap]);

  const handleDisableMonitoring = useCallback(() => {
    setPendingMonitoringChanges(prev => {
      const updated = {...prev};
      selectedRepositories.forEach(id => {
        const alreadyDisabled = prev[id] !== undefined ? !prev[id] : !(repoStatusMap[id] || false);
        if (!alreadyDisabled) {
          updated[id] = false;
        }
      });
      return updated;
    });
  }, [selectedRepositories, repoStatusMap]);

  const handleClearSelection = useCallback(() => {
    setSelectedRepositories([]);
  }, []);

  // Check if any settings have changed from existing settings
  const hasSettingsChanges = useMemo(() => {
    if (!existingSettings || !settingsData) return false;

    if (settingsData.activityTimeFrame !== existingSettings.activityTimeFrame) return true;
    if (settingsData.artifactLatestVersions !== existingSettings.artifactLatestVersions) return true;
    if (settingsData.policyEvaluationStage !== existingSettings.policyEvaluationStage) return true;
    if (settingsData.applyToNewRepos !== existingSettings.autoEnrollNewRepos) return true;
    if (versionDepthChanged(settingsData.versionDepth, existingSettings.versionDepth)) return true;

    return false;
  }, [existingSettings, settingsData]);

  const getEffectiveMonitoringStatus = useCallback((repo) => {
    const pending = pendingMonitoringChanges[repo.id];
    const isEnabled = pending !== undefined ? pending : repo.isSelected;
    if (isEnabled && repo.hasCustomConfig && (pending === undefined || (pending === true && repo.isSelected))) {
      return 'Custom';
    }
    return isEnabled ? 'Enabled' : 'Disabled';
  }, [pendingMonitoringChanges]);

  const allSelectedEnabled = useMemo(() => {
    if (!selectedRepositories.length) return false;
    return selectedRepositories.every(id => {
      const pending = pendingMonitoringChanges[id];
      if (pending !== undefined) return pending;
      // Use repoStatusMap if populated; fall back to current repositories on first render
      if (id in repoStatusMap) return repoStatusMap[id];
      const repo = (repositories || []).find(r => r.id === id);
      return repo ? repo.isSelected : false;
    });
  }, [selectedRepositories, repoStatusMap, repositories, pendingMonitoringChanges]);

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
              placeholder={HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.searchPlaceholder}
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
                  {fmt === 'all' ? HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.formatFilterLabel : fmt}
                </option>
              ))}
            </NxFormSelect>
            {hasSelections && (
              <NxFormSelect
                value={monitoringFilter || 'all'}
                onChange={handleMonitoringFilter}
              >
                <option value="all">{HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.monitoringFilterOptions.all}</option>
                <option value="enabled">{HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.monitoringFilterOptions.enabled}</option>
                <option value="disabled">{HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.monitoringFilterOptions.disabled}</option>
                <option value="custom">{HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.monitoringFilterOptions.custom}</option>
              </NxFormSelect>
            )}
          </SectionToolbar>

          <div className="nxrm-repo-count-row">
            <NxP>
              {selectedRepositories.length > 0
                ? `${selectedRepositories.length} of ${totalCount} repositories selected`
                : `${pagedRepositories.length} of ${totalCount} repositories`}
            </NxP>
            {hasSelections && selectedRepositories.length > 0 && (
              <div className="nx-btn-bar">
                {allSelectedEnabled
                  ? <NxButton onClick={handleDisableMonitoring}>{HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring}</NxButton>
                  : <NxButton onClick={handleEnableMonitoring}>{HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring}</NxButton>
                }
                <NxButton onClick={handleClearSelection}>{HOSTED_REPOSITORIES_EVALUATION.buttons.clearSelection}</NxButton>
              </div>
            )}
          </div>

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
                {hasSelections && (
                  <NxTableCell>Monitoring</NxTableCell>
                )}
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
                  <NxTableCell>
                    {hasSelections ? (
                      <div className="repository-name-cell">
                        <NxTextLink
                          href={`#admin/repository/repositories:${encodeURIComponent(repo.name)}`}
                          onClick={handleRepositoryLinkClick}
                        >
                          {repo.name}
                        </NxTextLink>
                        {repo.hasCustomConfig && pendingMonitoringChanges[repo.id] !== false && (
                          <NxSmallTag className="custom-config-tag">
                            Custom
                          </NxSmallTag>
                        )}
                      </div>
                    ) : (
                      repo.name
                    )}
                  </NxTableCell>
                  <NxTableCell>{repo.format}</NxTableCell>
                  <NxTableCell>{repo.size ? HumanReadableUtils.bytesToString(repo.size) : '-'}</NxTableCell>
                  <NxTableCell>{repo.artifactCount || '-'}</NxTableCell>
                  {hasSelections && (
                    <NxTableCell>{getEffectiveMonitoringStatus(repo)}</NxTableCell>
                  )}
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

          {incompleteSelectionError && (
            <NxErrorAlert onClose={() => setIncompleteSelectionError(false)}>
              {HOSTED_REPOSITORIES_EVALUATION.INCOMPLETE_MODAL.MESSAGE}
            </NxErrorAlert>
          )}
          <div className="nx-btn-bar">
            {!globalConfigAvailable && onBack && (
              <NxButton onClick={onBack}>{HOSTED_REPOSITORIES_EVALUATION.buttons.back}</NxButton>
            )}
            <NxButton
              variant="primary"
              onClick={handleSubmit}
              disabled={hasSelections && Object.keys(pendingMonitoringChanges).length === 0 && !hasSettingsChanges}
            >
              {hasSelections ? HOSTED_REPOSITORIES_EVALUATION.buttons.update : HOSTED_REPOSITORIES_EVALUATION.buttons.save}
            </NxButton>
          </div>
        </div>
      </NxLoadWrapper>

      {showErrorModal && (
        <NxModal onCancel={handleCloseErrorModal} variant="narrow">
          <header className="nx-modal-header">
            <NxH3>{HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.TITLE}</NxH3>
            <NxCloseButton onClick={handleCloseErrorModal} />
          </header>
          <div className="nx-modal-content">
            <NxErrorAlert>
              {errorMessage}
            </NxErrorAlert>
          </div>
          <footer className="nx-footer">
            <div className="nx-btn-bar">
              <NxButton variant="primary" onClick={handleCloseErrorModal}>
                {HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.CLOSE}
              </NxButton>
            </div>
          </footer>
        </NxModal>
      )}
    </>
  );
}
