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
import React, {useState} from 'react';
import {useRouter} from '@uirouter/react';
import {Page, PageHeader, ContentBody, Section} from '@sonatype/nexus-ui-plugin';
import {
  NxH1,
  NxStatefulBreadcrumb,
  NxModal,
  NxButton,
  NxWarningAlert,
  NxCloseButton
} from '@sonatype/react-shared-components';

import SettingsTab from './HostedRepositoriesEvaluationSettingsTab';
import RepositoriesTab from './HostedRepositoriesEvaluationRepositoriesTab';
import ProgressSteps from '../HostedRepositoriesEvaluation/ProgressSteps';
import UIStrings from '../../../../constants/UIStrings';
import {ROUTE_NAMES} from '../../../../routerConfig/routeNames/routeNames';

import './HostedRepositoriesEvaluation.scss';

export default function HostedRepositoriesEvaluation() {
  const router = useRouter();
  const [activeTabId, setActiveTabId] = useState(0);
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
    setActiveTabId(1);
    setIsFormDirty(false);
  }

  function handleBackToSettings() {
    setActiveTabId(0);
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
          <ProgressSteps
            steps={[
              UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.tabs.settings,
              UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.tabs.repositories
            ]}
            currentStep={activeTabId}
          />
          {activeTabId === 0 && (
            <SettingsTab
              initialData={settingsData}
              onNext={handleNext}
              onCancel={navigateBack}
              onFormChange={() => setIsFormDirty(true)}
            />
          )}
          {activeTabId === 1 && (
            <RepositoriesTab
              settingsData={settingsData}
              onBack={handleBackToSettings}
              initialSelectedRepositories={selectedRepositories}
              onSelectionChange={handleRepositorySelectionChange}
            />
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
