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

jest.mock('@uirouter/react', () => ({
  useRouter: jest.fn(() => ({
    stateService: {
      go: jest.fn()
    }
  }))
}));

describe('HostedRepositoriesEvaluationSettingsTab', () => {
  const mockOnNext = jest.fn();
  const mockOnCancel = jest.fn();
  const mockOnFormChange = jest.fn();
  const mockSend = jest.fn();

  const defaultMachineState = {
    context: {
      saveError: null
    },
    matches: jest.fn(() => false)
  };

  const defaultInitialData = {
    activityTimeFrame: '',
    artifactLatestVersions: '',
    policyEvaluationStage: '',
    applyToNewRepos: false
  };

  const renderComponent = (initialData = defaultInitialData, machineState = defaultMachineState) => {
    render(
      <HostedRepositoriesEvaluationSettingsTab
        initialData={initialData}
        onNext={mockOnNext}
        onCancel={mockOnCancel}
        onFormChange={mockOnFormChange}
        current={machineState}
        send={mockSend}
      />
    );
  };

  beforeEach(() => {
    mockOnNext.mockClear();
    mockOnCancel.mockClear();
    mockOnFormChange.mockClear();
    mockSend.mockClear();
  });

  it('renders Activity Time Frame, Latest Deployed Versions, and Policy Evaluation Stage', () => {
    renderComponent();

    expect(screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.monitoringSettings.newHostedReposText)).toBeInTheDocument();
    expect(screen.queryByLabelText(STRINGS.monitoringSettings.evaluationDepthMethodLabel)).not.toBeInTheDocument();
  });

  it('does not render the per-component-cap warning text', () => {
    renderComponent();

    const versionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    userEvent.selectOptions(versionsSelect, '5');

    // Regression guard: the historical warning ("...can take longer for large repositories.")
    // was removed in CLM-41306 and must not reappear.
    expect(screen.queryByText(/can take longer for large repositories/i)).not.toBeInTheDocument();
  });

  it('defaults Latest Deployed Versions to 5 when initialData omits it', () => {
    renderComponent();

    expect(screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel)).toHaveValue('5');
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
        current={defaultMachineState}
        send={mockSend}
      />
    );

    expect(screen.getByText(STRINGS.buttons.update)).toBeInTheDocument();
    expect(screen.queryByText(STRINGS.buttons.next)).not.toBeInTheDocument();
  });

  it('Next button is enabled by default since all required fields have defaults', () => {
    renderComponent();

    const nextButton = screen.getByText(STRINGS.buttons.next);
    expect(nextButton).not.toBeDisabled();
  });

  it('does not offer an empty placeholder option in either depth select', () => {
    renderComponent();

    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const versionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);

    // Removing the empty <option value=""> guarantees the field-level validation
    // (artifactLatestVersions !== '' / activityTimeFrame !== '') can't be invalidated
    // through normal user interaction once defaults are populated.
    expect(activitySelect.querySelector('option[value=""]')).toBeNull();
    expect(versionsSelect.querySelector('option[value=""]')).toBeNull();
  });

  it('calls onFormChange when Activity Time Frame changes', () => {
    renderComponent();

    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    userEvent.selectOptions(activitySelect, '60');

    expect(mockOnFormChange).toHaveBeenCalled();
  });

  it('calls onNext with both Activity Time Frame and Latest Deployed Versions values', () => {
    renderComponent();

    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const versionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const stageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);
    const nextButton = screen.getByText(STRINGS.buttons.next);

    userEvent.selectOptions(activitySelect, '60');
    userEvent.selectOptions(versionsSelect, '5');
    userEvent.selectOptions(stageSelect, 'build');
    userEvent.click(nextButton);

    expect(mockOnNext).toHaveBeenCalledWith({
      activityTimeFrame: '60',
      artifactLatestVersions: '5',
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

  it('renders with pre-filled values from initialData', () => {
    const initialData = {
      activityTimeFrame: '60',
      artifactLatestVersions: '5',
      policyEvaluationStage: 'RELEASE',
      applyToNewRepos: true
    };

    renderComponent(initialData);

    expect(screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel)).toHaveValue('60');
    expect(screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel)).toHaveValue('5');
    expect(screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel)).toHaveValue('release');
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

  it('Update button is disabled when globalConfigAvailable and no fields changed', () => {
    const initialData = {
      activityTimeFrame: '60',
      artifactLatestVersions: '',
      policyEvaluationStage: 'RELEASE',
      applyToNewRepos: false
    };
    render(
      <HostedRepositoriesEvaluationSettingsTab
        initialData={initialData}
        onNext={mockOnNext}
        onCancel={mockOnCancel}
        onFormChange={mockOnFormChange}
        globalConfigAvailable={true}
        current={defaultMachineState}
        send={mockSend}
      />
    );

    expect(screen.getByText(STRINGS.buttons.update)).toBeDisabled();
  });

  it('Update button is enabled when globalConfigAvailable and a field is changed', () => {
    const initialData = {
      activityTimeFrame: '60',
      artifactLatestVersions: '',
      policyEvaluationStage: 'RELEASE',
      applyToNewRepos: false
    };
    render(
      <HostedRepositoriesEvaluationSettingsTab
        initialData={initialData}
        onNext={mockOnNext}
        onCancel={mockOnCancel}
        onFormChange={mockOnFormChange}
        globalConfigAvailable={true}
        current={defaultMachineState}
        send={mockSend}
      />
    );

    const activitySelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    userEvent.selectOptions(activitySelect, '30');

    expect(screen.getByText(STRINGS.buttons.update)).not.toBeDisabled();
  });

  it('Update button is enabled when globalConfigAvailable and checkbox is toggled', () => {
    const initialData = {
      activityTimeFrame: '60',
      artifactLatestVersions: '',
      policyEvaluationStage: 'RELEASE',
      applyToNewRepos: false
    };
    render(
      <HostedRepositoriesEvaluationSettingsTab
        initialData={initialData}
        onNext={mockOnNext}
        onCancel={mockOnCancel}
        onFormChange={mockOnFormChange}
        globalConfigAvailable={true}
        current={defaultMachineState}
        send={mockSend}
      />
    );

    userEvent.click(screen.getByRole('checkbox'));

    expect(screen.getByText(STRINGS.buttons.update)).not.toBeDisabled();
  });

  it('sends UPDATE and PATCH_SETTINGS when Update is clicked', () => {
    const initialData = {
      activityTimeFrame: '60',
      artifactLatestVersions: '5',
      policyEvaluationStage: 'RELEASE',
      applyToNewRepos: false
    };
    render(
      <HostedRepositoriesEvaluationSettingsTab
        initialData={initialData}
        onNext={mockOnNext}
        onCancel={mockOnCancel}
        onFormChange={mockOnFormChange}
        globalConfigAvailable={true}
        current={defaultMachineState}
        send={mockSend}
      />
    );

    // Toggle checkbox to make Update enabled
    userEvent.click(screen.getByRole('checkbox'));

    userEvent.click(screen.getByText(STRINGS.buttons.update));

    expect(mockSend).toHaveBeenCalledWith(expect.objectContaining({
      type: 'UPDATE',
      data: expect.objectContaining({
        settings: expect.objectContaining({artifactLatestVersions: '5'})
      })
    }));
    expect(mockSend).toHaveBeenCalledWith('PATCH_SETTINGS');
  });
});
