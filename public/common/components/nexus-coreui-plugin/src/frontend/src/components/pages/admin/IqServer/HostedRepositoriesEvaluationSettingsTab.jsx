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

/**
 * Monitoring Settings Form
 *
 * Form with Activity Time Frame, Artifact Latest Versions, Policy Evaluation Stage,
 * and New Hosted Repositories checkbox
 */
export default function HostedRepositoriesEvaluationSettingsTab({initialData, onNext, onCancel, onFormChange}) {
  const [activityTimeFrame, setActivityTimeFrame] = useState(initialData?.activityTimeFrame || '');
  const [artifactLatestVersions, setArtifactLatestVersions] = useState(initialData?.artifactLatestVersions || '');
  const [policyEvaluationStage, setPolicyEvaluationStage] = useState(initialData?.policyEvaluationStage || '');
  const [applyToNewRepos, setApplyToNewRepos] = useState(initialData?.applyToNewRepos || false);

  useEffect(() => {
    if (initialData?.activityTimeFrame) {
      setActivityTimeFrame(initialData.activityTimeFrame);
    }
    if (initialData?.artifactLatestVersions) {
      setArtifactLatestVersions(initialData.artifactLatestVersions);
    }
    if (initialData?.policyEvaluationStage) {
      setPolicyEvaluationStage(initialData.policyEvaluationStage);
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
    const formData = {
      activityTimeFrame,
      artifactLatestVersions,
      policyEvaluationStage,
      applyToNewRepos
    };
    onNext(formData);
  };

  const isValid = activityTimeFrame !== '' && artifactLatestVersions !== '' && policyEvaluationStage !== '';

  const STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.monitoringSettings;
  const PACKAGE_STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.packageFilePatterns;
  const BUTTON_STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.buttons;

  return (
    <div className="nxrm-monitoring-settings-form">
      <NxH3>{STRINGS.title}</NxH3>

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
          {BUTTON_STRINGS.next}
        </NxButton>
      </div>
    </div>
  );
}
