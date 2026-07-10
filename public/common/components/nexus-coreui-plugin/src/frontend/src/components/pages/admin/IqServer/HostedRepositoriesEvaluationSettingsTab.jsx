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
import {
  NxH3,
  NxP,
  NxInfoAlert,
  NxFormGroup,
  NxFormSelect,
  NxCheckbox,
  NxButton
} from '@sonatype/react-shared-components';

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
export default function HostedRepositoriesEvaluationSettingsTab({initialData, onNext, onCancel, onFormChange, globalConfigAvailable = false}) {
  const isLatestDeployedVersions = initialData?.versionDepth != null && initialData.versionDepth > 0;
  const [evaluationDepthMethod, setEvaluationDepthMethod] = useState(
    isLatestDeployedVersions ? 'latestDeployedVersions' : 'activityTimeFrame'
  );
  const [activityTimeFrame, setActivityTimeFrame] = useState(initialData?.activityTimeFrame || '30');
  const [artifactLatestVersions, setArtifactLatestVersions] = useState(initialData?.artifactLatestVersions || '');
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
    if (initialData?.versionDepth != null) {
      setEvaluationDepthMethod(initialData.versionDepth > 0 ? 'latestDeployedVersions' : 'activityTimeFrame');
    }
    if (initialData?.policyEvaluationStage) {
      setPolicyEvaluationStage(normalizeStageForDisplay(initialData.policyEvaluationStage));
    }
    if (initialData?.applyToNewRepos !== undefined) {
      setApplyToNewRepos(initialData.applyToNewRepos);
    }
  }, [initialData]);

  const handleFieldChange = (setter, value) => {
    setter(value);
    if (onFormChange) {
      onFormChange();
    }
  };

  const handleNext = () => {
    const isVersionDepth = evaluationDepthMethod === 'latestDeployedVersions';
    // Normalize policyEvaluationStage to uppercase+underscore before sending to backend
    // (e.g. 'stage-release' → 'STAGE_RELEASE') to match backend @Pattern and DB CHECK constraint
    const normalizedStage = policyEvaluationStage
      ? policyEvaluationStage.toUpperCase().replace(/-/g, '_')
      : policyEvaluationStage;
    const formData = {
      activityTimeFrame,
      artifactLatestVersions,
      // versionDepth and artifactLatestVersions are intentionally the same value when in
      // latestDeployedVersions mode: versionDepth drives OR-logic expansion, artifactLatestVersions
      // is the final per-component cap — both set to the user's chosen depth.
      versionDepth: isVersionDepth ? artifactLatestVersions : '0',
      policyEvaluationStage: normalizedStage,
      applyToNewRepos
    };
    onNext(formData);
  };

  const isVersionDepthMethod = evaluationDepthMethod === 'latestDeployedVersions';
  const isValid = policyEvaluationStage !== '' &&
      (!isVersionDepthMethod ? activityTimeFrame !== '' : artifactLatestVersions !== '');

  const STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.monitoringSettings;
  const PACKAGE_STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.packageFilePatterns;
  const BUTTON_STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.buttons;

  return (
    <div className="nxrm-monitoring-settings-form">
      <NxH3>{STRINGS.title}</NxH3>

      <NxFormGroup label={STRINGS.evaluationDepthMethodLabel} sublabel={STRINGS.evaluationDepthMethodHelpText} isRequired>
        <NxFormSelect
          value={evaluationDepthMethod}
          onChange={(value) => handleFieldChange(setEvaluationDepthMethod, value)}
        >
          {STRINGS.evaluationDepthMethodOptions.map(option => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </NxFormSelect>
      </NxFormGroup>

      {!isVersionDepthMethod && (
        <NxFormGroup label={STRINGS.activityTimeFrameLabel} sublabel={STRINGS.activityTimeFrameHelpText} isRequired>
          <NxFormSelect
            value={activityTimeFrame}
            onChange={(value) => handleFieldChange(setActivityTimeFrame, value)}
          >
            <option value="">{STRINGS.activityTimeFramePlaceholder}</option>
            {STRINGS.activityTimeFrameOptions.map(option => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </NxFormSelect>
        </NxFormGroup>
      )}

      {isVersionDepthMethod && (
        <>
          <NxFormGroup label={STRINGS.artifactLatestVersionsLabel} sublabel={STRINGS.artifactLatestVersionsHelpText} isRequired>
            <NxFormSelect
              value={artifactLatestVersions}
              onChange={(value) => handleFieldChange(setArtifactLatestVersions, value)}
            >
              <option value="">{STRINGS.artifactLatestVersionsPlaceholder}</option>
              {STRINGS.artifactLatestVersionsOptions.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </NxFormSelect>
          </NxFormGroup>
          <NxP className="nxrm-version-depth-hint">{STRINGS.artifactLatestVersionsWarning}</NxP>
        </>
      )}

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
      </NxInfoAlert>
      <div className="package-patterns-list">
        <NxP>
          <strong>{PACKAGE_STRINGS.mavenLabel}</strong> {PACKAGE_STRINGS.maven}
        </NxP>
        <NxP>
          <strong>{PACKAGE_STRINGS.npmLabel}</strong> {PACKAGE_STRINGS.npm}
        </NxP>
        <NxP>
          <strong>{PACKAGE_STRINGS.pythonLabel}</strong> {PACKAGE_STRINGS.python}
        </NxP>
        <NxP>
          <strong>{PACKAGE_STRINGS.dockerLabel}</strong> {PACKAGE_STRINGS.docker}
        </NxP>
      </div>

      <div className="nx-btn-bar">
        <NxButton
          variant="tertiary"
          onClick={onCancel}
        >
          {BUTTON_STRINGS.cancel}
        </NxButton>
        <NxButton
          variant="primary"
          onClick={handleNext}
          disabled={!isValid}
        >
          {globalConfigAvailable ? BUTTON_STRINGS.update : BUTTON_STRINGS.next}
        </NxButton>
      </div>
    </div>
  );
}
