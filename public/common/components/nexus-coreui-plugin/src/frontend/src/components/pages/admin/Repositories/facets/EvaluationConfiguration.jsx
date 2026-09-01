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
import PropTypes from 'prop-types';
import Axios from 'axios';

import {
  NxFormGroup,
  NxRadio,
  NxFormSelect,
  NxFieldset
} from '@sonatype/react-shared-components';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../../constants/UIStrings';

const EVALUATION_MODE = {
  INHERIT: 'INHERIT',
  OVERRIDE: 'OVERRIDE',
  DISABLE: 'DISABLE'
};

export default function EvaluationConfiguration({parentMachine}) {
  const [parentState, sendParent] = parentMachine;
  const [globalSettings, setGlobalSettings] = useState(null);
  const savedOverrideValuesRef = useRef(null);
  const previousModeRef = useRef(null);

  const featureFlagValue = window.NX?.State?.getValue('hostedRepositoryEvaluationEnabled');
  const isFeatureEnabled = featureFlagValue === true;

  const evaluation = parentState.context.data.attributes?.evaluation;
  const mode = evaluation?.mode ?? EVALUATION_MODE.INHERIT;

  // Initialize previousModeRef on first render
  if (previousModeRef.current === null) {
    previousModeRef.current = mode;
  }

  const activityTimeFrame = mode === EVALUATION_MODE.INHERIT
    ? (globalSettings?.activityTimeFrame ?? '30')
    : (evaluation?.activityTimeFrame ?? (mode === EVALUATION_MODE.OVERRIDE ? '30' : ''));
  const artifactLatestVersions = mode === EVALUATION_MODE.INHERIT
    ? (globalSettings?.artifactLatestVersions ?? '1')
    : (evaluation?.artifactLatestVersions ?? (mode === EVALUATION_MODE.OVERRIDE ? '1' : ''));
  const policyEvaluationStage = mode === EVALUATION_MODE.INHERIT
    ? (globalSettings?.policyEvaluationStage ?? 'build')
    : (evaluation?.policyEvaluationStage ?? (mode === EVALUATION_MODE.OVERRIDE ? 'build' : ''));

  const STRINGS = UIStrings.SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.monitoringSettings;

  useEffect(() => {
    if (!isFeatureEnabled) {
      return;
    }

    const fetchSettings = async () => {
      try {
        const response = await Axios.get('service/rest/v1/evaluation/settings');
        if (response?.data) {
          const stage = response.data.policyEvaluationStage;
          const normalizedStage = stage ? stage.toLowerCase().replace(/_/g, '-') : 'build';
          setGlobalSettings({
            activityTimeFrame: String(response.data.activityTimeFrame ?? 30),
            artifactLatestVersions: String(response.data.artifactLatestVersions ?? 1),
            policyEvaluationStage: normalizedStage
          });
        } else {
          setGlobalSettings({
            activityTimeFrame: '30',
            artifactLatestVersions: '1',
            policyEvaluationStage: 'build'
          });
        }
      } catch (error) {
        setGlobalSettings({
          activityTimeFrame: '30',
          artifactLatestVersions: '1',
          policyEvaluationStage: 'build'
        });
      }
    };

    fetchSettings();
  }, [isFeatureEnabled]);

  // If feature is explicitly disabled, don't render the component
  if (!isFeatureEnabled) {
    return null;
  }

  const handleModeChange = (value) => {
    const previousMode = previousModeRef.current;

    // Save current OVERRIDE values before leaving OVERRIDE mode
    if (previousMode === EVALUATION_MODE.OVERRIDE && value !== EVALUATION_MODE.OVERRIDE) {
      savedOverrideValuesRef.current = {
        activityTimeFrame: evaluation?.activityTimeFrame,
        artifactLatestVersions: evaluation?.artifactLatestVersions,
        policyEvaluationStage: evaluation?.policyEvaluationStage
      };
    }

    previousModeRef.current = value;

    sendParent({
      type: 'UPDATE',
      name: 'attributes.evaluation',
      value: {
        mode: value,
        // For OVERRIDE mode, restore saved values if returning to OVERRIDE, otherwise use global settings
        // For INHERIT mode, set to global settings (these will be shown as read-only)
        // For DISABLE mode, clear all values
        ...(value === EVALUATION_MODE.OVERRIDE
          ? (savedOverrideValuesRef.current
              ? {
                  activityTimeFrame: savedOverrideValuesRef.current.activityTimeFrame ?? globalSettings?.activityTimeFrame ?? '30',
                  artifactLatestVersions: savedOverrideValuesRef.current.artifactLatestVersions ?? globalSettings?.artifactLatestVersions ?? '1',
                  policyEvaluationStage: savedOverrideValuesRef.current.policyEvaluationStage ?? globalSettings?.policyEvaluationStage ?? 'build'
                }
              : {
                  activityTimeFrame: globalSettings?.activityTimeFrame ?? '30',
                  artifactLatestVersions: globalSettings?.artifactLatestVersions ?? '1',
                  policyEvaluationStage: globalSettings?.policyEvaluationStage ?? 'build'
                })
          : value === EVALUATION_MODE.INHERIT
          ? {
              activityTimeFrame: globalSettings?.activityTimeFrame ?? '30',
              artifactLatestVersions: globalSettings?.artifactLatestVersions ?? '1',
              policyEvaluationStage: globalSettings?.policyEvaluationStage ?? 'build'
            }
          : {})
      }
    });
  };


  return (
    <div className="nxrm-evaluation-facet">
      <h2 className="nx-h2">Evaluation</h2>

      <NxFieldset label="Hosted Repository Evaluation" isRequired>
        <NxRadio
          name="evaluationMode"
          value={EVALUATION_MODE.INHERIT}
          isChecked={mode === EVALUATION_MODE.INHERIT}
          onChange={() => handleModeChange(EVALUATION_MODE.INHERIT)}
        >
          Inherit from global evaluation settings
        </NxRadio>
        <NxRadio
          name="evaluationMode"
          value={EVALUATION_MODE.OVERRIDE}
          isChecked={mode === EVALUATION_MODE.OVERRIDE}
          onChange={() => handleModeChange(EVALUATION_MODE.OVERRIDE)}
        >
          Override
        </NxRadio>
        <NxRadio
          name="evaluationMode"
          value={EVALUATION_MODE.DISABLE}
          isChecked={mode === EVALUATION_MODE.DISABLE}
          onChange={() => handleModeChange(EVALUATION_MODE.DISABLE)}
        >
          Disable Evaluation
        </NxRadio>
      </NxFieldset>

      <NxFormGroup
        label={STRINGS.activityTimeFrameLabel}
        sublabel={STRINGS.activityTimeFrameHelpText}
        isRequired={mode === EVALUATION_MODE.OVERRIDE}
      >
        <NxFormSelect
          {...FormUtils.selectProps('attributes.evaluation.activityTimeFrame', parentState, activityTimeFrame)}
          onChange={FormUtils.handleUpdate('attributes.evaluation.activityTimeFrame', sendParent)}
          disabled={mode !== EVALUATION_MODE.OVERRIDE}
        >
          {mode !== EVALUATION_MODE.OVERRIDE && <option value="">{STRINGS.activityTimeFramePlaceholder}</option>}
          {STRINGS.activityTimeFrameOptions.map(option => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </NxFormSelect>
      </NxFormGroup>

      <NxFormGroup
        label={STRINGS.artifactLatestVersionsLabel}
        sublabel={STRINGS.artifactLatestVersionsHelpText}
        isRequired={mode === EVALUATION_MODE.OVERRIDE}
      >
        <NxFormSelect
          {...FormUtils.selectProps('attributes.evaluation.artifactLatestVersions', parentState, artifactLatestVersions)}
          onChange={FormUtils.handleUpdate('attributes.evaluation.artifactLatestVersions', sendParent)}
          disabled={mode !== EVALUATION_MODE.OVERRIDE}
        >
          {mode !== EVALUATION_MODE.OVERRIDE && <option value="">{STRINGS.artifactLatestVersionsPlaceholder}</option>}
          {STRINGS.artifactLatestVersionsOptions.map(option => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </NxFormSelect>
      </NxFormGroup>

      <NxFormGroup
        label={STRINGS.policyEvaluationStageLabel}
        sublabel={STRINGS.policyEvaluationStageHelpText}
        isRequired={mode === EVALUATION_MODE.OVERRIDE}
      >
        <NxFormSelect
          {...FormUtils.selectProps('attributes.evaluation.policyEvaluationStage', parentState, policyEvaluationStage)}
          onChange={FormUtils.handleUpdate('attributes.evaluation.policyEvaluationStage', sendParent)}
          disabled={mode !== EVALUATION_MODE.OVERRIDE}
        >
          {mode !== EVALUATION_MODE.OVERRIDE && <option value="">{STRINGS.policyEvaluationStagePlaceholder}</option>}
          {STRINGS.policyEvaluationStageOptions.map(option => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </NxFormSelect>
      </NxFormGroup>
    </div>
  );
}

EvaluationConfiguration.propTypes = {
  parentMachine: PropTypes.arrayOf(PropTypes.any).isRequired
};
