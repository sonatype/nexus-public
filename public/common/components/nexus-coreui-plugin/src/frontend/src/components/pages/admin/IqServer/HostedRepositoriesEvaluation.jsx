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
import React, {useState, useEffect} from 'react';
import {useRouter, useCurrentStateAndParams} from '@uirouter/react';
import {useMachine} from '@xstate/react';
import {Page, PageHeader, ContentBody, Section} from '@sonatype/nexus-ui-plugin';
import {
  NxH1,
  NxStatefulBreadcrumb,
  NxModal,
  NxButton,
  NxWarningAlert,
  NxCloseButton,
  NxTabs,
  NxTabList,
  NxTab,
  NxTabPanel,
  NxCounter
} from '@sonatype/react-shared-components';

import SettingsTab from './HostedRepositoriesEvaluationSettingsTab';
import RepositoriesTab from './HostedRepositoriesEvaluationRepositoriesTab';
import ProgressSteps from '../HostedRepositoriesEvaluation/ProgressSteps';
import HostedRepositoriesEvaluationMachine
  from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/HostedRepositoriesEvaluationMachine';
import UIStrings from '../../../../constants/UIStrings';
import {ROUTE_NAMES} from '../../../../routerConfig/routeNames/routeNames';

import './HostedRepositoriesEvaluation.scss';

export default function HostedRepositoriesEvaluation() {
  const router = useRouter();
  // Parent machine instance - creates fresh instance on mount.
  // When user returns from Lifecycle page after PATCH, this component remounts,
  // creating a new machine instance that fetches latest numberOfMonitoredRepositories.
  // This ensures the badge always shows the current count when visible.
  const {params} = useCurrentStateAndParams();
  const [current] = useMachine(HostedRepositoriesEvaluationMachine);

  const rawTab = parseInt(params?.activeTab, 10);
  const initialTabIndex = rawTab === 1 ? 1 : 0;

  const [activeTabIndex, setActiveTabIndex] = useState(initialTabIndex);
  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [settingsData, setSettingsData] = useState({
    activityTimeFrame: '',
    artifactLatestVersions: '',
    policyEvaluationStage: '',
    applyToNewRepos: false
  });
  const [selectedRepositories, setSelectedRepositories] = useState([]);
  const [isFormDirty, setIsFormDirty] = useState(false);
  const [showUnsavedModal, setShowUnsavedModal] = useState(false);
  const [pendingNavigationRoute, setPendingNavigationRoute] = useState(null);
  const [settingsLoaded, setSettingsLoaded] = useState(false);

  const hasSelections = current.context.hasSelections || false;
  const globalConfigAvailable = current.context.globalConfigAvailable || false;
  // Badge count is fetched fresh on component mount (when user navigates to this page)
  const numberOfMonitoredRepositories = current.context.numberOfMonitoredRepositories || 0;
  const existingSettings = current.context.existingSettings;

  useEffect(() => {
    if (globalConfigAvailable && existingSettings && !settingsLoaded) {
      const policyStage = existingSettings.policyEvaluationStage
        ? existingSettings.policyEvaluationStage.toLowerCase().replace(/_/g, '-')
        : '';
      setSettingsData({
        activityTimeFrame: existingSettings.activityTimeFrame || '',
        artifactLatestVersions: existingSettings.artifactLatestVersions || '',
        policyEvaluationStage: policyStage,
        applyToNewRepos: existingSettings.autoEnrollNewRepos || false
      });
      setSettingsLoaded(true);
    }
  }, [globalConfigAvailable, existingSettings, settingsLoaded]);

  function navigateBack() {
    if (isFormDirty || settingsData.activityTimeFrame || selectedRepositories.length > 0) {
      setPendingNavigationRoute(ROUTE_NAMES.ADMIN.IQ.SONATYPE_LIFECYCLE.ROOT);
      setShowUnsavedModal(true);
      return;
    }
    router.stateService.go(ROUTE_NAMES.ADMIN.IQ.SONATYPE_LIFECYCLE.ROOT);
  }

  function handleNext(formData) {
    setSettingsData(formData);
    if (!globalConfigAvailable) {
      setActiveStepIndex(1);
    } else {
      setActiveTabIndex(1);
    }
    setIsFormDirty(false);
  }

  function handleBackToSettings() {
    if (!globalConfigAvailable) {
      setActiveStepIndex(0);
    } else {
      setActiveTabIndex(0);
    }
  }

  function handleRepositorySelectionChange(newSelection) {
    setSelectedRepositories(newSelection);
  }

  function handleConfirmUnsavedChanges() {
    setShowUnsavedModal(false);
    setIsFormDirty(false);
    setSettingsData({
      activityTimeFrame: '',
      artifactLatestVersions: '',
      policyEvaluationStage: '',
      applyToNewRepos: false
    });
    setSelectedRepositories([]);
    if (pendingNavigationRoute) {
      router.stateService.go(pendingNavigationRoute);
    }
    setPendingNavigationRoute(null);
  }

  function handleCancelUnsavedChanges() {
    setShowUnsavedModal(false);
    setPendingNavigationRoute(null);
  }

  function handleBreadcrumbClick(e, targetRoute) {
    e.preventDefault();
    e.stopPropagation();
    if (isFormDirty || settingsData.activityTimeFrame || selectedRepositories.length > 0) {
      setPendingNavigationRoute(targetRoute);
      setShowUnsavedModal(true);
    } else {
      router.stateService.go(targetRoute);
    }
    return false;
  }

  const crumbs = [
    {
      name: UIStrings.IQ_SERVER.MENU.text,
      href: router.stateService.href(ROUTE_NAMES.ADMIN.IQ.CONNECTED),
      onClick: (e) => handleBreadcrumbClick(e, ROUTE_NAMES.ADMIN.IQ.CONNECTED)
    },
    {
      name: UIStrings.SONATYPE_LIFECYCLE.MENU.text,
      href: router.stateService.href(ROUTE_NAMES.ADMIN.IQ.SONATYPE_LIFECYCLE.ROOT),
      onClick: (e) => handleBreadcrumbClick(e, ROUTE_NAMES.ADMIN.IQ.SONATYPE_LIFECYCLE.ROOT)
    },
    {name: UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.breadcrumb, href: '#'}
  ];

  return (
    <Page>
      <PageHeader>
        <div className="sonatype-lifecycle-header">
          <NxStatefulBreadcrumb crumbs={crumbs} />
          <div className="lifecycle-title">
            <NxH1>{UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.title}</NxH1>
          </div>
        </div>
      </PageHeader>

      <ContentBody>
        <Section>
          {!globalConfigAvailable ? (
            <>
              <ProgressSteps
                steps={[
                  UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.tabs.settings,
                  UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.tabs.repositories
                ]}
                currentStep={activeStepIndex}
              />
              {activeStepIndex === 0 && (
                <SettingsTab
                  initialData={settingsData}
                  onNext={handleNext}
                  onCancel={navigateBack}
                  onFormChange={() => setIsFormDirty(true)}
                />
              )}
              {activeStepIndex === 1 && (
                <RepositoriesTab
                  settingsData={settingsData}
                  onBack={handleBackToSettings}
                  initialSelectedRepositories={selectedRepositories}
                  onSelectionChange={handleRepositorySelectionChange}
                  globalConfigAvailable={globalConfigAvailable}
                />
              )}
            </>
          ) : (
            <NxTabs activeTab={activeTabIndex} onTabSelect={setActiveTabIndex}>
              <NxTabList>
                <NxTab>
                  {UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.tabs.monitoringSettings}
                </NxTab>
                <NxTab>
                  {UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.tabs.monitoredRepositories}
                  {numberOfMonitoredRepositories > 0 && (
                    <NxCounter className={activeTabIndex === 1 ? 'nx-counter--active' : ''}>
                      {numberOfMonitoredRepositories}
                    </NxCounter>
                  )}
                </NxTab>
              </NxTabList>
              <NxTabPanel>
                <SettingsTab
                  initialData={settingsData}
                  onNext={handleNext}
                  onCancel={navigateBack}
                  onFormChange={() => setIsFormDirty(true)}
                />
              </NxTabPanel>
              <NxTabPanel>
                <RepositoriesTab
                  settingsData={settingsData}
                  onBack={handleBackToSettings}
                  initialSelectedRepositories={selectedRepositories}
                  onSelectionChange={handleRepositorySelectionChange}
                  globalConfigAvailable={globalConfigAvailable}
                />
              </NxTabPanel>
            </NxTabs>
          )}
        </Section>
      </ContentBody>

      {showUnsavedModal && (
        <NxModal onCancel={handleCancelUnsavedChanges} variant="narrow">
          <header className="nx-modal-header">
            <h2 className="nx-h2">{UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.UNSAVED_CHANGES_MODAL.TITLE}</h2>
            <NxCloseButton onClick={handleCancelUnsavedChanges} />
          </header>
          <div className="nx-modal-content">
            <NxWarningAlert>
              {UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.UNSAVED_CHANGES_MODAL.MESSAGE}
            </NxWarningAlert>
          </div>
          <footer className="nx-footer">
            <div className="nx-btn-bar">
              <NxButton onClick={handleCancelUnsavedChanges}>
                {UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.UNSAVED_CHANGES_MODAL.CANCEL}
              </NxButton>
              <NxButton variant="primary" onClick={handleConfirmUnsavedChanges}>
                {UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.UNSAVED_CHANGES_MODAL.CONTINUE}
              </NxButton>
            </div>
          </footer>
        </NxModal>
      )}
    </Page>
  );
}
