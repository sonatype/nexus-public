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
import React, {useState, useEffect, useRef} from 'react';
import {
  NxH3,
  NxP,
  NxInfoAlert,
  NxFormGroup,
  NxFormSelect,
  NxCheckbox,
  NxButton,
  NxModal,
  NxErrorAlert,
  NxCloseButton
} from '@sonatype/react-shared-components';
import {useRouter} from '@uirouter/react';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import {ROUTE_NAMES} from '../../../../routerConfig/routeNames/routeNames';

import UIStrings from '../../../../constants/UIStrings';
import './HostedRepositoriesEvaluationSettingsTab.scss';

// Normalize API value (e.g. 'RELEASE', 'STAGE_RELEASE') to dropdown option value (e.g. 'release', 'stage-release')
const normalizeStageForDisplay = v => v ? v.toLowerCase().replace(/_/g, '-') : '';

/**
 * Monitoring Settings Form
 *
 * Form with Activity Time Frame, Artifact Latest Versions, Policy Evaluation Stage,
 * and New Hosted Repositories checkbox
 */
export default function HostedRepositoriesEvaluationSettingsTab({
  initialData,
  onNext,
  onCancel,
  onFormChange,
  globalConfigAvailable = false,
  onUpdateSuccess,
  onCancelEdit,
  current,
  send
}) {
  const router = useRouter();
  const [showErrorModal, setShowErrorModal] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const wasSavingRef = useRef(false);
  const [activityTimeFrame, setActivityTimeFrame] = useState(initialData?.activityTimeFrame || '30');
  const [artifactLatestVersions, setArtifactLatestVersions] = useState(initialData?.artifactLatestVersions || '5');
  const [policyEvaluationStage, setPolicyEvaluationStage] = useState(
    normalizeStageForDisplay(initialData?.policyEvaluationStage) || 'release'
  );
  const [applyToNewRepos, setApplyToNewRepos] = useState(initialData?.applyToNewRepos || false);

  useEffect(() => {
    if (initialData?.activityTimeFrame) {
      setActivityTimeFrame(initialData.activityTimeFrame);
    }
    if (initialData?.artifactLatestVersions) {
      setArtifactLatestVersions(initialData.artifactLatestVersions);
    }
    if (initialData?.policyEvaluationStage) {
      setPolicyEvaluationStage(normalizeStageForDisplay(initialData.policyEvaluationStage));
    }
    if (initialData?.applyToNewRepos !== undefined) {
      setApplyToNewRepos(initialData.applyToNewRepos);
    }
  }, [initialData]);

  const isSaving = current.matches('patchingSettings');
  const isLoaded = current.matches('loaded');

  useEffect(() => {
    const currentSaveError = current.context.saveError;

    if (wasSavingRef.current && !isSaving && isLoaded && !currentSaveError) {
      ExtJS.setDirtyStatus('HostedRepositoriesEvaluationMachine', false);
      if (onUpdateSuccess) {
        onUpdateSuccess();
      } else {
        router.stateService.go(ROUTE_NAMES.ADMIN.IQ.SONATYPE_LIFECYCLE.ROOT);
      }
    }
    wasSavingRef.current = isSaving;
  }, [isSaving, isLoaded, current.context.saveError, router, onUpdateSuccess]);

  useEffect(() => {
    const error = current.context.saveError;
    if (error) {
      const message = (typeof error?.response?.data?.message === 'string' && error.response.data.message) ||
                      (typeof error?.message === 'string' && error.message) ||
                      'Failed to update settings';
      setErrorMessage(message);
      setShowErrorModal(true);
    }
  }, [current.context.saveError]);

  const handleCloseErrorModal = () => {
    setShowErrorModal(false);
    setErrorMessage('');
  };

  const handleCancelEdit = () => {
    // Reset all form fields back to saved initialData
    setActivityTimeFrame(initialData?.activityTimeFrame || '30');
    setArtifactLatestVersions(initialData?.artifactLatestVersions || '5');
    setPolicyEvaluationStage(normalizeStageForDisplay(initialData?.policyEvaluationStage) || 'release');
    setApplyToNewRepos(initialData?.applyToNewRepos || false);
    if (onCancelEdit) onCancelEdit();
  };

  const handleUpdate = () => {
    const normalizedStage = policyEvaluationStage
      ? policyEvaluationStage.toUpperCase().replace(/-/g, '_')
      : policyEvaluationStage;
    const settingsData = {
      activityTimeFrame,
      artifactLatestVersions,
      policyEvaluationStage: normalizedStage,
      applyToNewRepos
    };
    send({type: 'UPDATE', data: {settings: settingsData}});
    send('PATCH_SETTINGS');
  };

  const handleFieldChange = (setter, value) => {
    setter(value);
    if (onFormChange) {
      onFormChange();
    }
  };

  const handleNext = () => {
    // Normalize policyEvaluationStage to uppercase+underscore before sending to backend
    // (e.g. 'stage-release' → 'STAGE_RELEASE') to match backend @Pattern and DB CHECK constraint
    const normalizedStage = policyEvaluationStage
      ? policyEvaluationStage.toUpperCase().replace(/-/g, '_')
      : policyEvaluationStage;
    const formData = {
      activityTimeFrame,
      artifactLatestVersions,
      policyEvaluationStage: normalizedStage,
      applyToNewRepos
    };
    onNext(formData);
  };

  const isValid = policyEvaluationStage !== '' && activityTimeFrame !== '' && artifactLatestVersions !== '';

  const isDirty = globalConfigAvailable && (
    String(activityTimeFrame) !== String(initialData?.activityTimeFrame || '30') ||
    String(artifactLatestVersions) !== String(initialData?.artifactLatestVersions || '5') ||
    policyEvaluationStage !== (normalizeStageForDisplay(initialData?.policyEvaluationStage) || 'release') ||
    applyToNewRepos !== (initialData?.applyToNewRepos || false)
  );

  const STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.monitoringSettings;
  const ERROR_MODAL_STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.SETTINGS_ERROR_MODAL;
  const PACKAGE_STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.packageFilePatterns;
  const BUTTON_STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.buttons;

  return (
    <div className="nxrm-monitoring-settings-form">
      {!globalConfigAvailable && (
        <NxInfoAlert>
          <NxP>{STRINGS.evaluationContextDescription}</NxP>
        </NxInfoAlert>
      )}

      <NxFormGroup label={STRINGS.activityTimeFrameLabel} sublabel={STRINGS.activityTimeFrameHelpText} isRequired>
        <NxFormSelect
          value={activityTimeFrame}
          onChange={(value) => handleFieldChange(setActivityTimeFrame, value)}
        >
          {STRINGS.activityTimeFrameOptions.map(option => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </NxFormSelect>
      </NxFormGroup>

      <NxFormGroup label={STRINGS.artifactLatestVersionsLabel} sublabel={STRINGS.artifactLatestVersionsHelpText} isRequired>
        <NxFormSelect
          value={artifactLatestVersions}
          onChange={(value) => handleFieldChange(setArtifactLatestVersions, value)}
        >
          {STRINGS.artifactLatestVersionsOptions.map(option => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </NxFormSelect>
      </NxFormGroup>

      <NxFormGroup label={STRINGS.policyEvaluationStageLabel} sublabel={STRINGS.policyEvaluationStageHelpText} isRequired>
        <NxFormSelect
          value={policyEvaluationStage}
          onChange={(value) => handleFieldChange(setPolicyEvaluationStage, value)}
        >
          <option value="">{STRINGS.policyEvaluationStagePlaceholder}</option>
          {STRINGS.policyEvaluationStageOptions.map(option => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </NxFormSelect>
      </NxFormGroup>

      <div className="new-hosted-repos-container">
        <strong>{STRINGS.newHostedReposLabel}</strong>
        <div className="new-hosted-repos-checkbox">
          <NxCheckbox
            checkboxId="apply-to-new-repos"
            isChecked={applyToNewRepos}
            onChange={(value) => handleFieldChange(setApplyToNewRepos, value)}
          >
            {STRINGS.newHostedReposText}
          </NxCheckbox>
        </div>
      </div>

      <NxH3>{PACKAGE_STRINGS.title}</NxH3>
      <NxInfoAlert>
        <NxP>{PACKAGE_STRINGS.description}</NxP>
        <ul className="package-patterns-list">
          <li>
            <strong>{PACKAGE_STRINGS.mavenLabel}</strong> {PACKAGE_STRINGS.maven}
          </li>
          <li>
            <strong>{PACKAGE_STRINGS.npmLabel}</strong> {PACKAGE_STRINGS.npm}
          </li>
          <li>
            <strong>{PACKAGE_STRINGS.pythonLabel}</strong> {PACKAGE_STRINGS.python}
          </li>
        </ul>
      </NxInfoAlert>

      <div className="nx-btn-bar">
        <NxButton
          variant="tertiary"
          onClick={globalConfigAvailable ? handleCancelEdit : onCancel}
        >
          {BUTTON_STRINGS.cancel}
        </NxButton>
        <NxButton
          variant="primary"
          onClick={globalConfigAvailable ? handleUpdate : handleNext}
          disabled={!isValid || (globalConfigAvailable && (!isDirty || isSaving))}
        >
          {globalConfigAvailable ? BUTTON_STRINGS.update : BUTTON_STRINGS.next}
        </NxButton>
      </div>

      {showErrorModal && (
        <NxModal onCancel={handleCloseErrorModal} variant="narrow">
          <header className="nx-modal-header">
            <NxH3>{ERROR_MODAL_STRINGS.TITLE}</NxH3>
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
                {ERROR_MODAL_STRINGS.CLOSE}
              </NxButton>
            </div>
          </footer>
        </NxModal>
      )}
    </div>
  );
}
