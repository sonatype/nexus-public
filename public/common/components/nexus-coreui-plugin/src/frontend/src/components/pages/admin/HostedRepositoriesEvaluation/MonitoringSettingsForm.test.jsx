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

import MonitoringSettingsForm from './MonitoringSettingsForm';
import UIStrings from '../../../../constants/UIStrings';

const {HOSTED_REPOSITORIES_EVALUATION: STRINGS} = UIStrings.SONATYPE_LIFECYCLE;

describe('MonitoringSettingsForm', () => {
  const mockOnNext = jest.fn();
  const mockOnCancel = jest.fn();

  const defaultInitialData = {
    activityTimeFrame: '',
    artifactLatestVersions: '',
    policyEvaluationStage: '',
    applyToNewRepos: false
  };

  const renderComponent = (initialData = defaultInitialData) => {
    render(
      <MonitoringSettingsForm
        initialData={initialData}
        onNext={mockOnNext}
        onCancel={mockOnCancel}
      />
    );
  };

  beforeEach(() => {
    mockOnNext.mockClear();
    mockOnCancel.mockClear();
  });

  it('renders all form fields', () => {
    renderComponent();

    expect(screen.getByText(STRINGS.monitoringSettings.title)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.monitoringSettings.newHostedReposText)).toBeInTheDocument();
    expect(screen.getByRole('checkbox')).toBeInTheDocument();
  });

  it('renders Package File Patterns section', () => {
    renderComponent();

    expect(screen.getByText(STRINGS.packageFilePatterns.title)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.packageFilePatterns.description)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.packageFilePatterns.maven)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.packageFilePatterns.npm)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.packageFilePatterns.python)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.packageFilePatterns.docker)).toBeInTheDocument();
  });

  it('renders Cancel and Next buttons', () => {
    renderComponent();

    expect(screen.getByText(STRINGS.buttons.cancel)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.buttons.next)).toBeInTheDocument();
  });

  it('Next button is disabled when form is empty', () => {
    renderComponent();

    const nextButton = screen.getByText(STRINGS.buttons.next);
    expect(nextButton).toBeDisabled();
  });

  it('Next button is disabled when only one field is filled', async () => {
    renderComponent();

    const activityTimeFrameSelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    await userEvent.selectOptions(activityTimeFrameSelect, '30');

    const nextButton = screen.getByText(STRINGS.buttons.next);
    expect(nextButton).toBeDisabled();
  });

  it('Next button is disabled when only two fields are filled', async () => {
    renderComponent();

    const activityTimeFrameSelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const artifactLatestVersionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);

    await userEvent.selectOptions(activityTimeFrameSelect, '30');
    await userEvent.selectOptions(artifactLatestVersionsSelect, '5');

    const nextButton = screen.getByText(STRINGS.buttons.next);
    expect(nextButton).toBeDisabled();
  });

  it('Next button is enabled when all required fields are filled', async () => {
    renderComponent();

    const activityTimeFrameSelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const artifactLatestVersionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const policyEvaluationStageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);

    await userEvent.selectOptions(activityTimeFrameSelect, '30');
    await userEvent.selectOptions(artifactLatestVersionsSelect, '5');
    await userEvent.selectOptions(policyEvaluationStageSelect, 'release');

    const nextButton = screen.getByText(STRINGS.buttons.next);
    expect(nextButton).not.toBeDisabled();
  });

  it('Activity Time Frame dropdown has all options', () => {
    renderComponent();

    const activityTimeFrameSelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const options = Array.from(activityTimeFrameSelect.options).map(option => option.value);

    expect(options).toContain('');
    expect(options).toContain('30');
    expect(options).toContain('60');
    expect(options).toContain('90');
  });

  it('Artifact Latest Versions dropdown has all options', () => {
    renderComponent();

    const artifactLatestVersionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const options = Array.from(artifactLatestVersionsSelect.options).map(option => option.value);

    expect(options).toContain('');
    expect(options).toContain('1');
    expect(options).toContain('2');
    expect(options).toContain('3');
    expect(options).toContain('5');
  });

  it('Policy Evaluation Stage dropdown has all options', () => {
    renderComponent();

    const policyEvaluationStageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const options = Array.from(policyEvaluationStageSelect.options).map(option => option.value);

    expect(options).toContain('');
    expect(options).toContain('build');
    expect(options).toContain('stage-release');
    expect(options).toContain('release');
    expect(options).toContain('operate');
  });

  it('checkbox is unchecked by default', () => {
    renderComponent();

    const checkbox = screen.getByRole('checkbox');
    expect(checkbox).not.toBeChecked();
  });

  it('checkbox can be checked and unchecked', async () => {
    renderComponent();

    const checkbox = screen.getByRole('checkbox');

    await userEvent.click(checkbox);
    expect(checkbox).toBeChecked();

    await userEvent.click(checkbox);
    expect(checkbox).not.toBeChecked();
  });

  it('calls onNext with correct form data when Next is clicked', async () => {
    renderComponent();

    const activityTimeFrameSelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const artifactLatestVersionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const policyEvaluationStageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const checkbox = screen.getByRole('checkbox');

    await userEvent.selectOptions(activityTimeFrameSelect, '30');
    await userEvent.selectOptions(artifactLatestVersionsSelect, '5');
    await userEvent.selectOptions(policyEvaluationStageSelect, 'release');
    await userEvent.click(checkbox);

    const nextButton = screen.getByText(STRINGS.buttons.next);
    await userEvent.click(nextButton);

    expect(mockOnNext).toHaveBeenCalledTimes(1);
    expect(mockOnNext).toHaveBeenCalledWith({
      activityTimeFrame: '30',
      artifactLatestVersions: '5',
      policyEvaluationStage: 'release',
      applyToNewRepos: true
    });
  });

  it('calls onCancel when Cancel is clicked', async () => {
    renderComponent();

    const cancelButton = screen.getByText(STRINGS.buttons.cancel);
    await userEvent.click(cancelButton);

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it('populates form with initialData', () => {
    const initialData = {
      activityTimeFrame: '60',
      artifactLatestVersions: '5',
      policyEvaluationStage: 'operate',
      applyToNewRepos: true
    };

    renderComponent(initialData);

    const activityTimeFrameSelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const artifactLatestVersionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const policyEvaluationStageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const checkbox = screen.getByRole('checkbox');

    expect(activityTimeFrameSelect).toHaveValue('60');
    expect(artifactLatestVersionsSelect).toHaveValue('5');
    expect(policyEvaluationStageSelect).toHaveValue('operate');
    expect(checkbox).toBeChecked();
  });

  it('displays help text for all fields', () => {
    renderComponent();

    expect(screen.getByText(STRINGS.monitoringSettings.activityTimeFrameHelpText)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.monitoringSettings.artifactLatestVersionsHelpText)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.monitoringSettings.policyEvaluationStageHelpText)).toBeInTheDocument();
  });

  it('updates form state when fields are changed', async () => {
    renderComponent();

    const activityTimeFrameSelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const artifactLatestVersionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const policyEvaluationStageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);

    await userEvent.selectOptions(activityTimeFrameSelect, '30');
    expect(activityTimeFrameSelect.value).toBe('30');

    await userEvent.selectOptions(activityTimeFrameSelect, '60');
    expect(activityTimeFrameSelect.value).toBe('60');

    await userEvent.selectOptions(artifactLatestVersionsSelect, '1');
    expect(artifactLatestVersionsSelect.value).toBe('1');

    await userEvent.selectOptions(artifactLatestVersionsSelect, '2');
    expect(artifactLatestVersionsSelect.value).toBe('2');

    await userEvent.selectOptions(policyEvaluationStageSelect, 'build');
    expect(policyEvaluationStageSelect.value).toBe('build');

    await userEvent.selectOptions(policyEvaluationStageSelect, 'stage-release');
    expect(policyEvaluationStageSelect.value).toBe('stage-release');
  });
});
