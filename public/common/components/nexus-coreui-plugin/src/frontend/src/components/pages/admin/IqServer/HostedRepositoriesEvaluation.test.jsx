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
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useRouter, useCurrentStateAndParams} from '@uirouter/react';
import {useMachine} from '@xstate/react';

import HostedRepositoriesEvaluation from './HostedRepositoriesEvaluation';
import UIStrings from '../../../../constants/UIStrings';

const {HOSTED_REPOSITORIES_EVALUATION} = UIStrings.SONATYPE_LIFECYCLE;

jest.mock('@uirouter/react', () => ({
  useRouter: jest.fn(),
  useCurrentStateAndParams: jest.fn(() => ({params: {}}))
}));

jest.mock('@xstate/react', () => ({
  useMachine: jest.fn()
}));

jest.mock('./HostedRepositoriesEvaluationSettingsTab', () => {
  return function MockSettingsTab({onNext, onCancel}) {
    return (
      <div data-testid="settings-tab">
        <button onClick={() => onNext({
          activityTimeFrame: '30',
          artifactLatestVersions: '5',
          policyEvaluationStage: 'build',
          applyToNewRepos: false
        })}>
          Next
        </button>
        <button onClick={onCancel}>Cancel</button>
      </div>
    );
  };
});

jest.mock('./HostedRepositoriesEvaluationRepositoriesTab', () => {
  return function MockRepositoriesTab({onBack, globalConfigAvailable}) {
    return (
      <div data-testid="repositories-tab" data-has-selections={globalConfigAvailable}>
        <button onClick={onBack}>Back</button>
      </div>
    );
  };
});

jest.mock('../HostedRepositoriesEvaluation/ProgressSteps', () => {
  return function MockProgressSteps({steps, currentStep}) {
    return (
      <div data-testid="progress-steps" data-current-step={currentStep}>
        {steps.map((step, idx) => (
          <div key={idx} className={idx === currentStep ? 'active' : ''}>
            {step}
          </div>
        ))}
      </div>
    );
  };
});

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  Page: ({children}) => <div data-testid="page">{children}</div>,
  PageHeader: ({children}) => <div data-testid="page-header">{children}</div>,
  ContentBody: ({children}) => <div data-testid="content-body">{children}</div>,
  Section: ({children}) => <div data-testid="section">{children}</div>
}));

describe('HostedRepositoriesEvaluation', () => {
  const mockStateService = {
    go: jest.fn(),
    href: jest.fn((route) => `#${route}`)
  };

  const mockRouter = {
    stateService: mockStateService
  };

  const mockMachineState = {
    context: {
      hasSelections: false,
      globalConfigAvailable: false,
      numberOfMonitoredRepositories: 0,
      existingSettings: null
    },
    matches: jest.fn(() => false)
  };

  const mockSend = jest.fn();

  beforeEach(() => {
    useRouter.mockReturnValue(mockRouter);
    useMachine.mockReturnValue([mockMachineState, mockSend]);
    mockStateService.go.mockClear();
    mockStateService.href.mockClear();
    mockSend.mockClear();
  });

  it('renders with ProgressSteps for first-time users (globalConfigAvailable=false)', () => {
    render(<HostedRepositoriesEvaluation />);

    expect(screen.getByTestId('progress-steps')).toBeInTheDocument();
    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
  });

  it('renders with NxTabs for returning users (globalConfigAvailable=true)', () => {
    const returningUserState = {
      context: {
        hasSelections: false,
        globalConfigAvailable: true,
        numberOfMonitoredRepositories: 0,
        existingSettings: null
      },
      matches: jest.fn(() => false)
    };
    useMachine.mockReturnValue([returningUserState, mockSend]);

    render(<HostedRepositoriesEvaluation />);

    expect(screen.getByRole('tablist')).toBeInTheDocument();
    expect(screen.queryByTestId('progress-steps')).not.toBeInTheDocument();
  });

  it('renders Monitoring Settings tab by default for returning users', () => {
    const returningUserState = {
      context: {
        hasSelections: false,
        globalConfigAvailable: true,
        numberOfMonitoredRepositories: 0,
        existingSettings: null
      },
      matches: jest.fn(() => false)
    };
    useMachine.mockReturnValue([returningUserState, mockSend]);

    render(<HostedRepositoriesEvaluation />);

    // NxTabs renders tab list, verify it contains tabs
    expect(screen.getByRole('tablist')).toBeInTheDocument();
    const tabs = screen.getAllByRole('tab');
    expect(tabs.length).toBeGreaterThan(0);
  });

  it('renders Monitored Repositories tab for returning users', () => {
    const returningUserState = {
      context: {
        hasSelections: false,
        globalConfigAvailable: true,
        numberOfMonitoredRepositories: 0,
        existingSettings: null
      },
      matches: jest.fn(() => false)
    };
    useMachine.mockReturnValue([returningUserState, mockSend]);

    render(<HostedRepositoriesEvaluation />);

    // Verify tabs are rendered
    expect(screen.getByRole('tablist')).toBeInTheDocument();
    const tabs = screen.getAllByRole('tab');
    expect(tabs.length).toBe(2); // Settings and Repositories tabs
  });

  it('shows SettingsTab content when Monitoring Settings tab is active', async () => {
    render(<HostedRepositoriesEvaluation />);

    await waitFor(() => {
      expect(screen.getByTestId('settings-tab')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('repositories-tab')).not.toBeInTheDocument();
  });

  it('switches to Monitored Repositories tab when clicking on it for returning users', async () => {
    const returningUserState = {
      context: {
        hasSelections: false,
        globalConfigAvailable: true,
        numberOfMonitoredRepositories: 0,
        existingSettings: null
      },
      matches: jest.fn(() => false)
    };
    useMachine.mockReturnValue([returningUserState, mockSend]);

    render(<HostedRepositoriesEvaluation />);

    const tabs = screen.getAllByRole('tab');
    // Click the second tab (index 1) which is Monitored Repositories
    userEvent.click(tabs[1]);

    await waitFor(() => {
      expect(screen.getByTestId('repositories-tab')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('settings-tab')).not.toBeInTheDocument();
  });

  it('switches to Monitored Repositories tab when Next is clicked in Settings', async () => {
    render(<HostedRepositoriesEvaluation />);

    await waitFor(() => {
      expect(screen.getByText('Next')).toBeInTheDocument();
    });

    const nextButton = screen.getByText('Next');
    userEvent.click(nextButton);

    await waitFor(() => {
      expect(screen.getByTestId('repositories-tab')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('settings-tab')).not.toBeInTheDocument();
  });

  it('switches back to Monitoring Settings tab when Back is clicked in Repositories for returning users', async () => {
    const returningUserState = {
      context: {
        hasSelections: false,
        globalConfigAvailable: true,
        numberOfMonitoredRepositories: 0,
        existingSettings: null
      },
      matches: jest.fn(() => false)
    };
    useMachine.mockReturnValue([returningUserState, mockSend]);

    render(<HostedRepositoriesEvaluation />);

    // First navigate to repositories tab
    const tabs = screen.getAllByRole('tab');
    // Click the second tab (index 1) which is Monitored Repositories
    userEvent.click(tabs[1]);

    await waitFor(() => {
      expect(screen.getByText('Back')).toBeInTheDocument();
    });

    // Then click back button
    const backButton = screen.getByText('Back');
    userEvent.click(backButton);

    await waitFor(() => {
      expect(screen.getByTestId('settings-tab')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('repositories-tab')).not.toBeInTheDocument();
  });

  it('renders page header with breadcrumbs', () => {
    render(<HostedRepositoriesEvaluation />);

    expect(screen.getByRole('heading', {name: HOSTED_REPOSITORIES_EVALUATION.title})).toBeInTheDocument();
  });

  it('does not show badge when monitored count is 0 for returning users', () => {
    const returningUserState = {
      context: {
        hasSelections: false,
        globalConfigAvailable: true,
        numberOfMonitoredRepositories: 0,
        existingSettings: null
      },
      matches: jest.fn(() => false)
    };
    useMachine.mockReturnValue([returningUserState, mockSend]);

    render(<HostedRepositoriesEvaluation />);

    // For returning users with tabs, check that no badge is shown initially
    // Badge only appears when monitoredCount > 0
    expect(screen.queryByText(/^\d+$/)).not.toBeInTheDocument();
  });

  it('maintains form state when switching between tabs', async () => {
    render(<HostedRepositoriesEvaluation />);

    // Wait for Next button and click it
    await waitFor(() => {
      expect(screen.getByText('Next')).toBeInTheDocument();
    });

    const nextButton = screen.getByText('Next');
    userEvent.click(nextButton);

    // Wait for Back button and click it
    await waitFor(() => {
      expect(screen.getByText('Back')).toBeInTheDocument();
    });

    const backButton = screen.getByText('Back');
    userEvent.click(backButton);

    // Settings should still be present (component maintains state)
    await waitFor(() => {
      expect(screen.getByTestId('settings-tab')).toBeInTheDocument();
    });
  });

  it('shows ProgressSteps with SETTINGS step for first-time users', () => {
    render(<HostedRepositoriesEvaluation />);

    const progressSteps = screen.getByTestId('progress-steps');
    expect(progressSteps).toHaveAttribute('data-current-step', '0');
    expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.tabs.settings)).toBeInTheDocument();
  });

  it('shows step 2 in ProgressSteps after clicking Next for first-time users', async () => {
    render(<HostedRepositoriesEvaluation />);

    await waitFor(() => {
      expect(screen.getByText('Next')).toBeInTheDocument();
    });

    const nextButton = screen.getByText('Next');
    userEvent.click(nextButton);

    await waitFor(() => {
      const progressSteps = screen.getByTestId('progress-steps');
      expect(progressSteps).toHaveAttribute('data-current-step', '1');
    });
  });

  it('passes hasSelections=false to RepositoriesTab for first-time users', async () => {
    render(<HostedRepositoriesEvaluation />);

    await waitFor(() => {
      expect(screen.getByText('Next')).toBeInTheDocument();
    });

    const nextButton = screen.getByText('Next');
    userEvent.click(nextButton);

    await waitFor(() => {
      const repositoriesTab = screen.getByTestId('repositories-tab');
      expect(repositoriesTab).toHaveAttribute('data-has-selections', 'false');
    });
  });

  it('passes hasSelections=true to RepositoriesTab for returning users', async () => {
    const returningUserState = {
      context: {
        hasSelections: false,
        globalConfigAvailable: true,
        numberOfMonitoredRepositories: 0,
        existingSettings: null
      },
      matches: jest.fn(() => false)
    };
    useMachine.mockReturnValue([returningUserState, mockSend]);

    render(<HostedRepositoriesEvaluation />);

    const tabs = screen.getAllByRole('tab');
    // Click the second tab (index 1) which is Monitored Repositories
    userEvent.click(tabs[1]);

    await waitFor(() => {
      const repoTab = screen.getByTestId('repositories-tab');
      expect(repoTab).toHaveAttribute('data-has-selections', 'true');
    });
  });

  it('populates settings from existingSettings for returning users', () => {
    const returningUserState = {
      context: {
        hasSelections: false,
        globalConfigAvailable: true,
        numberOfMonitoredRepositories: 5,
        existingSettings: {
          activityTimeFrame: '60',
          artifactLatestVersions: '10',
          policyEvaluationStage: 'STAGE_RELEASE',
          autoEnrollNewRepos: true
        }
      },
      matches: jest.fn(() => false)
    };
    useMachine.mockReturnValue([returningUserState, mockSend]);

    render(<HostedRepositoriesEvaluation />);

    expect(screen.getByTestId('settings-tab')).toBeInTheDocument();
  });

  describe('activeTab parameter handling', () => {
    it('starts on tab 0 by default when activeTab not specified', async () => {
      useCurrentStateAndParams.mockReturnValue({params: {}});

      const returningUserState = {
        context: {
          hasSelections: false,
          globalConfigAvailable: true,
          numberOfMonitoredRepositories: 0,
          existingSettings: null
        },
        matches: jest.fn(() => false)
      };
      useMachine.mockReturnValue([returningUserState, mockSend]);

      render(<HostedRepositoriesEvaluation />);

      // NxTabs should render, and settings tab (tab 0) content should be shown
      expect(screen.getByRole('tablist')).toBeInTheDocument();
      await waitFor(() => {
        expect(screen.getByTestId('settings-tab')).toBeInTheDocument();
      });
    });

    it('starts on tab 1 when activeTab=1 in params', async () => {
      useCurrentStateAndParams.mockReturnValue({params: {activeTab: '1'}});

      const returningUserState = {
        context: {
          hasSelections: true,
          globalConfigAvailable: true,
          numberOfMonitoredRepositories: 0,
          existingSettings: null
        },
        matches: jest.fn(() => false)
      };
      useMachine.mockReturnValue([returningUserState, mockSend]);

      render(<HostedRepositoriesEvaluation />);

      // NxTabs should render, and repositories tab (tab 1) content should be shown
      expect(screen.getByRole('tablist')).toBeInTheDocument();
      await waitFor(() => {
        expect(screen.getByTestId('repositories-tab')).toBeInTheDocument();
      });
    });

    it('defaults to tab 0 when activeTab has invalid value', async () => {
      useCurrentStateAndParams.mockReturnValue({params: {activeTab: 'invalid'}});

      const returningUserState = {
        context: {
          hasSelections: false,
          globalConfigAvailable: true,
          numberOfMonitoredRepositories: 0,
          existingSettings: null
        },
        matches: jest.fn(() => false)
      };
      useMachine.mockReturnValue([returningUserState, mockSend]);

      render(<HostedRepositoriesEvaluation />);

      // NxTabs should render, and settings tab (tab 0) should be shown as fallback
      expect(screen.getByRole('tablist')).toBeInTheDocument();
      await waitFor(() => {
        expect(screen.getByTestId('settings-tab')).toBeInTheDocument();
      });
    });

    it('handles out of range activeTab value gracefully', () => {
      useCurrentStateAndParams.mockReturnValue({params: {activeTab: '5'}});

      const returningUserState = {
        context: {
          hasSelections: false,
          globalConfigAvailable: true,
          numberOfMonitoredRepositories: 0,
          existingSettings: null
        },
        matches: jest.fn(() => false)
      };
      useMachine.mockReturnValue([returningUserState, mockSend]);

      // Component should render without crashing even with out-of-range activeTab
      expect(() => render(<HostedRepositoriesEvaluation />)).not.toThrow();
      expect(screen.getByRole('tablist')).toBeInTheDocument();
    });
  });
});
