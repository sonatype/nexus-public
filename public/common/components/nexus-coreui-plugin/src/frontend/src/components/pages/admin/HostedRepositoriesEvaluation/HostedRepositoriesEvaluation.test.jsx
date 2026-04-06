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
import {render, screen, waitFor, fireEvent} from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import HostedRepositoriesEvaluation from './HostedRepositoriesEvaluation';
import UIStrings from '../../../../constants/UIStrings';

const {HOSTED_REPOSITORIES_EVALUATION: STRINGS} = UIStrings.SONATYPE_LIFECYCLE;

// Mock the useRouter hook from @uirouter/react
jest.mock('@uirouter/react', () => ({
  useRouter: jest.fn(() => ({
    stateService: {
      go: jest.fn(),
      href: jest.fn((stateName) => `#${stateName}`)
    }
  }))
}));

describe('HostedRepositoriesEvaluation', () => {
  const renderComponent = () => {
    render(<HostedRepositoriesEvaluation/>);
  };

  it('renders the page with title', () => {
    renderComponent();

    expect(screen.getByRole('heading', {name: STRINGS.title})).toBeInTheDocument();
  });

  it('renders both progress steps', () => {
    renderComponent();

    expect(screen.getByText(STRINGS.tabs.settings)).toBeInTheDocument();
    expect(screen.getByText(STRINGS.tabs.repositories)).toBeInTheDocument();
  });

  it('renders SETTINGS tab content by default', () => {
    renderComponent();

    expect(screen.getByText(STRINGS.monitoringSettings.title)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel)).toBeInTheDocument();
    expect(screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel)).toBeInTheDocument();
  });

  it('switches to REPOSITORIES step when clicked', async () => {
    renderComponent();

    const repositoriesStep = screen.getByText(STRINGS.tabs.repositories);

    await userEvent.click(repositoriesStep);

    await waitFor(() => {
      // TODO (CLM-38702): Update test when repository selection UI is implemented
      expect(screen.getByText('Repository selection will be implemented in the next milestone (CLM-38702)')).toBeInTheDocument();
    });
  });

  it('switches back to SETTINGS step when clicked', async () => {
    renderComponent();

    const repositoriesStep = screen.getByText(STRINGS.tabs.repositories);
    const settingsStep = screen.getByText(STRINGS.tabs.settings);

    await userEvent.click(repositoriesStep);
    await waitFor(() => {
      // TODO (CLM-38702): Update test when repository selection UI is implemented
      expect(screen.getByText('Repository selection will be implemented in the next milestone (CLM-38702)')).toBeInTheDocument();
    });

    await userEvent.click(settingsStep);
    await waitFor(() => {
      expect(screen.getByText(STRINGS.monitoringSettings.title)).toBeInTheDocument();
    });
  });

  it('logs form data when Next is clicked', async () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();

    renderComponent();

    const activityTimeFrameSelect = screen.getByLabelText(STRINGS.monitoringSettings.activityTimeFrameLabel);
    const artifactLatestVersionsSelect = screen.getByLabelText(STRINGS.monitoringSettings.artifactLatestVersionsLabel);
    const policyEvaluationStageSelect = screen.getByLabelText(STRINGS.monitoringSettings.policyEvaluationStageLabel);

    fireEvent.change(activityTimeFrameSelect, {target: {value: '30'}});
    fireEvent.change(artifactLatestVersionsSelect, {target: {value: '5'}});
    fireEvent.change(policyEvaluationStageSelect, {target: {value: 'release'}});

    await waitFor(() => {
      const nextButton = screen.getByText(STRINGS.buttons.next);
      expect(nextButton).not.toBeDisabled();
    });

    const nextButton = screen.getByText(STRINGS.buttons.next);
    await userEvent.click(nextButton);

    expect(consoleSpy).toHaveBeenCalledWith('Form data submitted:', expect.objectContaining({
      activityTimeFrame: '30',
      artifactLatestVersions: '5',
      policyEvaluationStage: 'release',
      applyToNewRepos: false
    }));

    consoleSpy.mockRestore();
  });

  it('logs cancel message when Cancel is clicked', async () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();

    renderComponent();

    const cancelButton = screen.getByText(STRINGS.buttons.cancel);
    await userEvent.click(cancelButton);

    expect(consoleSpy).toHaveBeenCalledWith('Form cancelled');

    consoleSpy.mockRestore();
  });
});
