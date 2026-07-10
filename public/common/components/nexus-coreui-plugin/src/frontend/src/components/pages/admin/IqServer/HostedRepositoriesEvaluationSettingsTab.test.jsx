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
import React from 'react';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import HostedRepositoriesEvaluationSettingsTab from './HostedRepositoriesEvaluationSettingsTab';
import UIStrings from '../../../../constants/UIStrings';

const {HOSTED_REPOSITORIES_EVALUATION: STRINGS} = UIStrings.SONATYPE_LIFECYCLE;

describe('HostedRepositoriesEvaluationSettingsTab', () => {
  const mockOnNext = jest.fn();
  const mockOnCancel = jest.fn();
  const mockOnFormChange = jest.fn();

  const defaultInitialData = {
    activityTimeFrame: '',
    artifactLatestVersions: '',
    policyEvaluationStage: '',
    applyToNewRepos: false
  };

  const renderComponent = (initialData = defaultInitialData) => {
    render(
      <HostedRepositoriesEvaluationSettingsTab
        initialData={initialData}
        onNext={mockOnNext}
        onCancel={mockOnCancel}
        onFormChange={mockOnFormChange}
      />
    );
  };

  beforeEach(() => {
    mockOnNext.mockClear();
    mockOnCancel.mockClear();
    mockOnFormChange.mockClear();
  });

  it('renders Evaluation Depth Method dropdown and Policy Evaluation Stage by default', () => {
    renderComponent();

    expect(screen.getByText(STRINGS.monitoringSettings.title)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.monitoringSettings.newHostedReposText)).toBeInTheDocument();
  });

  it('shows Activity Time Frame sub-dropdown by default', () => {
    renderComponent();

    expect(screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel)).toBeInTheDocument();
    expect(screen.queryByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel)).not.toBeInTheDocument();
  });

  it('shows Latest Deployed Versions sub-dropdown and warning when Latest Deployed Versions method is selected', () => {
    renderComponent();

    const methodSelect = screen.getByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel);
    userEvent.selectOptions(methodSelect, 'latestDeployedVersions');

    expect(screen.queryByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel)).not.toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.monitoringSettings.artifactLatestVersionsWarning)).toBeInTheDocument();
  });

  it('hides warning when Activity Time Frame method is selected', () => {
    renderComponent();

    const methodSelect = screen.getByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel);
    userEvent.selectOptions(methodSelect, 'latestDeployedVersions');
    userEvent.selectOptions(methodSelect, 'activityTimeFrame');

    expect(screen.queryByText(STRINGS.monitoringSettings.artifactLatestVersionsWarning)).not.toBeInTheDocument();
  });

  it('renders Package File Patterns section', () => {
    renderComponent();

    expect(screen.getByText(STRINGS.packageFilePatterns.title)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.packageFilePatterns.description)).toBeInTheDocument();
  });

  it('renders Cancel and Next buttons', () => {
    renderComponent();

    expect(screen.getByText(STRINGS.buttons.cancel)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.buttons.next)).toBeInTheDocument();
  });

  it('renders Update button instead of Next when globalConfigAvailable is true', () => {
    render(
      <HostedRepositoriesEvaluationSettingsTab
        initialData={defaultInitialData}
        onNext={mockOnNext}
        onCancel={mockOnCancel}
        onFormChange={mockOnFormChange}
        globalConfigAvailable={true}
      />
    );

    expect(screen.getByText(STRINGS.buttons.update)).toBeInTheDocument();
    expect(screen.queryByText(STRINGS.buttons.next)).not.toBeInTheDocument();
  });

  it('Next button is enabled by default since Activity Time Frame and Policy Evaluation Stage have defaults', () => {
    renderComponent();

    const nextButton = screen.getByText(STRINGS.buttons.next);
    expect(nextButton).not.toBeDisabled();
  });

  it('Next button is enabled when Activity Time Frame method has timeframe and stage filled', () => {
    renderComponent();

    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(stageSelect, 'build');

    expect(nextButton).not.toBeDisabled();
  });

  it('Next button is disabled when Latest Deployed Versions method has no version count selected', () => {
    renderComponent();

    const methodSelect = screen.getByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel);
    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(methodSelect, 'latestDeployedVersions');
    userEvent.selectOptions(stageSelect, 'build');

    expect(nextButton).toBeDisabled();
  });

  it('Next button is enabled when Latest Deployed Versions method has version count and stage filled', () => {
    renderComponent();

    const methodSelect = screen.getByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel);
    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(methodSelect, 'latestDeployedVersions');
    const versionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    userEvent.selectOptions(versionsSelect, '5');
    userEvent.selectOptions(stageSelect, 'build');

    expect(nextButton).not.toBeDisabled();
  });

  it('calls onFormChange when Evaluation Depth Method changes', () => {
    renderComponent();

    const methodSelect = screen.getByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel);
    userEvent.selectOptions(methodSelect, 'latestDeployedVersions');

    expect(mockOnFormChange).toHaveBeenCalled();
  });

  it('calls onNext with Activity Time Frame data when that method is selected', () => {
    renderComponent();

    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(activitySelect, '60');
    userEvent.selectOptions(stageSelect, 'build');
    userEvent.click(nextButton);

    expect(mockOnNext).toHaveBeenCalledWith({
      activityTimeFrame: '60',
      artifactLatestVersions: '',
      versionDepth: '0',
      policyEvaluationStage: 'BUILD',
      applyToNewRepos: false
    });
  });

  it('calls onNext with Latest Deployed Versions data when that method is selected', () => {
    renderComponent();

    const methodSelect = screen.getByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel);
    userEvent.selectOptions(methodSelect, 'latestDeployedVersions');

    const versionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(versionsSelect, '5');
    userEvent.selectOptions(stageSelect, 'build');
    userEvent.click(nextButton);

    expect(mockOnNext).toHaveBeenCalledWith({
      activityTimeFrame: '30',
      artifactLatestVersions: '5',
      versionDepth: '5',
      policyEvaluationStage: 'BUILD',
      applyToNewRepos: false
    });
  });

  it('calls onCancel when Cancel button is clicked', () => {
    renderComponent();

    const cancelButton = screen.getByText(STRINGS.buttons.cancel);
    userEvent.click(cancelButton);

    expect(mockOnCancel).toHaveBeenCalled();
  });

  it('renders with pre-filled Activity Time Frame values from initialData', () => {
    const initialData = {
      activityTimeFrame: '60',
      artifactLatestVersions: '5',
      versionDepth: 0,
      policyEvaluationStage: 'RELEASE',
      applyToNewRepos: true
    };

    renderComponent(initialData);

    expect(screen.getByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel)).toHaveValue('activityTimeFrame');
    expect(screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel)).toHaveValue('60');
    expect(screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel)).toHaveValue('release');
  });

  it('renders with pre-filled Latest Deployed Versions values from initialData', () => {
    const initialData = {
      activityTimeFrame: '30',
      artifactLatestVersions: '5',
      versionDepth: 5,
      policyEvaluationStage: 'STAGE_RELEASE',
      applyToNewRepos: false
    };

    renderComponent(initialData);

    expect(screen.getByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel)).toHaveValue('latestDeployedVersions');
    expect(screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel)).toHaveValue('5');
  });

  it('normalizes uppercase API policyEvaluationStage to lowercase-hyphen for dropdown display', () => {
    renderComponent({...defaultInitialData, policyEvaluationStage: 'STAGE_RELEASE'});

    expect(screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel)).toHaveValue('stage-release');
  });

  it('includes applyToNewRepos checkbox state in form data', () => {
    renderComponent();

    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const applyToNewReposCheckbox = screen.getByRole('checkbox');
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(stageSelect, 'build');
    userEvent.click(applyToNewReposCheckbox);
    userEvent.click(nextButton);

    expect(mockOnNext).toHaveBeenCalledWith(expect.objectContaining({
      applyToNewRepos: true
    }));
  });
});
