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

  it('Next button is enabled when all required fields are filled', () => {
    renderComponent();

    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const versionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(activitySelect, '30');
    userEvent.selectOptions(versionsSelect, '5');
    userEvent.selectOptions(stageSelect, 'build');

    expect(nextButton).not.toBeDisabled();
  });

  it('calls onFormChange when a field value changes', () => {
    renderComponent();

    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    userEvent.selectOptions(activitySelect, '30');

    expect(mockOnFormChange).toHaveBeenCalled();
  });

  it('calls onNext with form data when Next button is clicked', () => {
    renderComponent();

    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const versionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(activitySelect, '30');
    userEvent.selectOptions(versionsSelect, '5');
    userEvent.selectOptions(stageSelect, 'build');
    userEvent.click(nextButton);

    expect(mockOnNext).toHaveBeenCalledWith({
      activityTimeFrame: '30',
      artifactLatestVersions: '5',
      policyEvaluationStage: 'build',
      applyToNewRepos: false
    });
  });

  it('calls onCancel when Cancel button is clicked', () => {
    renderComponent();
    

    const cancelButton = screen.getByText(STRINGS.buttons.cancel);
    userEvent.click(cancelButton);

    expect(mockOnCancel).toHaveBeenCalled();
  });

  it('renders with pre-filled values from initialData', () => {
    const initialData = {
      activityTimeFrame: '60',
      artifactLatestVersions: '5',
      policyEvaluationStage: 'release',
      applyToNewRepos: true
    };

    renderComponent(initialData);

    expect(screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel)).toHaveValue('60');
    expect(screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel)).toHaveValue('5');
    expect(screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel)).toHaveValue('release');
    expect(screen.getByRole('checkbox')).toBeChecked();
  });

  it('includes applyToNewRepos checkbox state in form data', () => {
    renderComponent();
    

    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const versionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const checkbox = screen.getByRole('checkbox');
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(activitySelect, '30');
    userEvent.selectOptions(versionsSelect, '5');
    userEvent.selectOptions(stageSelect, 'build');
    userEvent.click(checkbox);
    userEvent.click(nextButton);

    expect(mockOnNext).toHaveBeenCalledWith({
      activityTimeFrame: '30',
      artifactLatestVersions: '5',
      policyEvaluationStage: 'build',
      applyToNewRepos: true
    });
  });

  it('Next button remains disabled if only some fields are filled', () => {
    renderComponent();
    

    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(activitySelect, '30');

    expect(nextButton).toBeDisabled();
  });
});