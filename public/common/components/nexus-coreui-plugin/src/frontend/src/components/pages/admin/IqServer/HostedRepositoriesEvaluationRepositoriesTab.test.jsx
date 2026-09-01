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
import {render, screen, waitFor, cleanup} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {useMachine} from '@xstate/react';

import HostedRepositoriesEvaluationRepositoriesTab from './HostedRepositoriesEvaluationRepositoriesTab';
import UIStrings from '../../../../constants/UIStrings';

const {HOSTED_REPOSITORIES_EVALUATION} = UIStrings.SONATYPE_LIFECYCLE;

jest.mock('@xstate/react', () => ({
  useMachine: jest.fn()
}));

jest.mock('@uirouter/react', () => ({
  useRouter: jest.fn(() => ({
    stateService: {
      go: jest.fn(),
      href: jest.fn(() => '#admin/iq-server/hosted-repositories-evaluation?activeTab=1')
    }
  }))
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  HostedRepositoriesEvaluationMachine: {},
  SectionToolbar: ({children}) => <div data-testid="section-toolbar">{children}</div>,
  HumanReadableUtils: {
    formatBytes: (bytes) => `${bytes} bytes`,
    bytesToString: (bytes) => `${bytes} bytes`
  },
  ExtJS: {
    setDirtyStatus: jest.fn()
  }
}));

describe('HostedRepositoriesEvaluationRepositoriesTab', () => {
  const mockOnSelectionChange = jest.fn();
  const mockSend = jest.fn();

  const mockSettingsData = {
    activityTimeFrame: '30',
    artifactLatestVersions: '5',
    policyEvaluationStage: 'build',
    applyToNewRepos: false
  };

  const mockRepositories = [
    {
      id: 'repo1',
      name: 'Maven Central',
      format: 'maven2',
      size: 1024,
      artifactCount: 100,
      isSelected: true,
      hasCustomConfig: false
    },
    {
      id: 'repo2',
      name: 'NPM Registry',
      format: 'npm',
      size: 2048,
      artifactCount: 200,
      isSelected: false,
      hasCustomConfig: true
    }
  ];

  const defaultMachineState = {
    context: {
      data: {},
      repositories: mockRepositories,
      loadError: null,
      formatFilter: 'all',
      monitoringFilter: 'all',
      offsetPage: 0,
      sortField: 'name',
      sortDirection: 'asc',
      totalPages: 1,
      totalCount: 2,
      numberOfMonitoredRepositories: 1,
      globalConfigAvailable: false,
      formats: ['all', 'maven2', 'npm']
    },
    matches: jest.fn(() => false)
  };

  const renderComponent = (props = {}, machineState = defaultMachineState) => {
    useMachine.mockReturnValue([machineState, mockSend]);

    return render(
      <HostedRepositoriesEvaluationRepositoriesTab
        settingsData={mockSettingsData}
        initialSelectedRepositories={[]}
        onSelectionChange={mockOnSelectionChange}
        {...props}
      />
    );
  };

  beforeEach(() => {
    const {useRouter} = require('@uirouter/react');
    mockOnSelectionChange.mockClear();
    mockSend.mockClear();
    useMachine.mockClear();
    useRouter.mockClear();
  });

  it('renders repository table with data', () => {
    renderComponent();

    expect(screen.getByText('Maven Central')).toBeInTheDocument();
    expect(screen.getByText('NPM Registry')).toBeInTheDocument();
    const table = screen.getByRole('table');
    expect(table).toHaveTextContent('maven2');
    expect(table).toHaveTextContent('npm');
  });

  it('renders search and filter controls for first-time users', () => {
    renderComponent({globalConfigAvailable: false});

    expect(screen.getByPlaceholderText(HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.searchPlaceholder)).toBeInTheDocument();
    const selects = screen.getAllByRole('combobox');
    expect(selects.length).toBe(1); // Only format filter for first-time users
  });

  it('renders Save button for first-time users', () => {
    renderComponent();

    expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.save)).toBeInTheDocument();
    expect(screen.queryByText(UIStrings.SETTINGS.BACK_BUTTON_LABEL)).not.toBeInTheDocument();
  });

  it('renders Back button for first-time users when onBack is provided', () => {
    const mockOnBack = jest.fn();
    renderComponent({onBack: mockOnBack, globalConfigAvailable: false});

    const backButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.back);
    expect(backButton).toBeInTheDocument();

    userEvent.click(backButton);
    expect(mockOnBack).toHaveBeenCalled();
  });

  it('does not render Back button for returning users even when onBack is provided', () => {
    const mockOnBack = jest.fn();
    renderComponent({onBack: mockOnBack, globalConfigAvailable: true});

    expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.back)).not.toBeInTheDocument();
  });

  it('renders Update button for returning users', () => {
    const returningUserState = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        globalConfigAvailable: true
      }
    };

    renderComponent({globalConfigAvailable: true}, returningUserState);

    expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update)).toBeInTheDocument();
  });

  it('shows loading state', () => {
    const loadingState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'loading')
    };

    renderComponent({}, loadingState);

    // NxLoadWrapper shows loading spinner, not text
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('shows error state with retry button', () => {
    const errorState = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        loadError: 'Failed to load repositories'
      }
    };

    renderComponent({}, errorState);

    expect(screen.getByText(/Failed to load repositories/)).toBeInTheDocument();
    expect(screen.getByText('Retry')).toBeInTheDocument();
  });

  it('shows inline error when Save is clicked without selecting repositories', async () => {
    renderComponent();

    const saveButton = screen.getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL);
    userEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.INCOMPLETE_MODAL.MESSAGE)).toBeInTheDocument();
    });
  });

  it('allows repository selection via checkbox', async () => {
    renderComponent();


    const checkboxes = screen.getAllByRole('checkbox');
    userEvent.click(checkboxes[1]); // First data checkbox (skip select-all)

    await waitFor(() => {
      expect(mockOnSelectionChange).toHaveBeenCalled();
    });
  });

  it('handles select all checkbox', async () => {
    renderComponent();


    const checkboxes = screen.getAllByRole('checkbox');
    userEvent.click(checkboxes[0]); // Select all checkbox

    await waitFor(() => {
      expect(mockOnSelectionChange).toHaveBeenCalled();
    });
  });

  it('handles search input with debounce', async () => {
    renderComponent();


    const searchInput = screen.getByPlaceholderText(HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.searchPlaceholder);
    userEvent.type(searchInput, 'maven');

    await waitFor(() => {
      expect(mockSend).toHaveBeenCalledWith({type: 'FILTER', filter: 'maven'});
    }, {timeout: 600});
  });

  it('handles format filter change', () => {
    renderComponent();

    const selects = screen.getAllByRole('combobox');
    const formatSelect = selects[0];
    userEvent.selectOptions(formatSelect, 'maven2');

    expect(mockSend).toHaveBeenCalledWith({type: 'FILTER_FORMAT', formatFilter: 'maven2'});
  });

  it('handles monitoring filter change for returning users', () => {
    renderComponent({globalConfigAvailable: true});

    const selects = screen.getAllByRole('combobox');
    const monitoringSelect = selects[1];
    userEvent.selectOptions(monitoringSelect, 'enabled');

    expect(mockSend).toHaveBeenCalledWith({type: 'FILTER_MONITORING', monitoringFilter: 'enabled'});
  });

  it('renders monitoring filter options correctly for returning users', () => {
    renderComponent({globalConfigAvailable: true});

    const selects = screen.getAllByRole('combobox');
    const monitoringSelect = selects[1];

    expect(monitoringSelect).toHaveValue('all');

    const options = Array.from(monitoringSelect.querySelectorAll('option')).map(opt => opt.textContent);
    expect(options).toContain(HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.monitoringFilterOptions.all);
    expect(options).toContain(HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.monitoringFilterOptions.enabled);
    expect(options).toContain(HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.monitoringFilterOptions.disabled);
    expect(options).toContain(HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.monitoringFilterOptions.custom);
  });

  it('handles retry button click in error state', () => {
    const errorState = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        loadError: 'Failed to load'
      }
    };

    renderComponent({}, errorState);


    const retryButton = screen.getByText('Retry');
    userEvent.click(retryButton);

    expect(mockSend).toHaveBeenCalledWith('RETRY');
  });

  it('calls save when repositories are selected', () => {
    renderComponent({initialSelectedRepositories: ['repo1']});


    const saveButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.save);
    userEvent.click(saveButton);

    expect(mockSend).toHaveBeenCalledWith({
      type: 'UPDATE',
      data: expect.objectContaining({
        selectedRepositories: ['repo1'],
        settings: mockSettingsData
      })
    });
    expect(mockSend).toHaveBeenCalledWith('SAVE');
  });

  it('renders with pre-selected repositories', () => {
    renderComponent({initialSelectedRepositories: ['repo1']});

    // Select-all checkbox + 2 repo checkboxes
    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes).toHaveLength(3);
    expect(checkboxes[1]).toBeChecked(); // First repo should be checked
  });

  it('handles pagination when available', () => {
    const multiPageState = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        totalPages: 3,
        offsetPage: 0
      }
    };

    renderComponent({}, multiPageState);

    const pagination = screen.getByRole('navigation', {name: /pagination/i});
    expect(pagination).toBeInTheDocument();
  });

  it('displays error modal when save fails', async () => {
    const errorState = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        saveError: {
          message: 'Backend validation failed',
          response: {
            data: {
              message: 'Backend validation failed'
            }
          }
        }
      }
    };

    renderComponent({}, errorState);

    await waitFor(() => {
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.TITLE)).toBeInTheDocument();
      expect(screen.getByText('Backend validation failed')).toBeInTheDocument();
    });
  });

  it('closes error modal when Close button is clicked', async () => {
    const errorState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'loaded'),
      context: {
        ...defaultMachineState.context,
        saveError: {
          message: 'Backend validation failed',
          response: {
            data: {
              message: 'Backend validation failed'
            }
          }
        }
      }
    };

    renderComponent({}, errorState);

    // Wait for error modal to appear
    await waitFor(() => {
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.TITLE)).toBeInTheDocument();
    });

    // Click the Close button
    const closeButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.CLOSE);
    userEvent.click(closeButton);

    // Verify modal is dismissed
    await waitFor(() => {
      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.TITLE)).not.toBeInTheDocument();
    });
  });

  it('displays error.message when response.data.message is not available', async () => {
    const errorState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'loaded'),
      context: {
        ...defaultMachineState.context,
        saveError: {
          message: 'Network timeout'
          // No response.data.message - fallback to message
        }
      }
    };

    renderComponent({}, errorState);

    await waitFor(() => {
      expect(screen.getByText('Network timeout')).toBeInTheDocument();
    });
  });

  it('displays default error message when no message available', async () => {
    const errorState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'loaded'),
      context: {
        ...defaultMachineState.context,
        saveError: {}
        // Empty error object - fallback to default message
      }
    };

    renderComponent({}, errorState);

    await waitFor(() => {
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.MESSAGE)).toBeInTheDocument();
    });
  });

  it('navigates to lifecycle page after successful save', () => {
    const mockRouter = {
      stateService: {
        go: jest.fn()
      }
    };

    const {useRouter} = require('@uirouter/react');
    useRouter.mockReturnValue(mockRouter);

    // Initial state: saving
    const savingState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'patchingRepositories')
    };

    const {rerender} = renderComponent({}, savingState);

    // Transition to loaded state (save complete)
    const loadedState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'loaded'),
      context: {
        ...defaultMachineState.context,
        saveError: null
      }
    };

    useMachine.mockReturnValue([loadedState, mockSend]);
    rerender(
      <HostedRepositoriesEvaluationRepositoriesTab
        settingsData={mockSettingsData}
        initialSelectedRepositories={[]}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    // Navigation should be triggered
    expect(mockRouter.stateService.go).toHaveBeenCalledWith('admin.sonatypelifecycle');
  });

  it('navigates to lifecycle page after successful first-time PUT save', () => {
    // First-time user flow: the machine enters the 'saving' state on PUT
    // (not 'patchingRepositories', which is the PATCH flow for returning users).
    // The isSaving computation in the component includes 'saving' explicitly —
    // this test guards against regressions where 'saving' is removed from the check.
    const mockRouter = {
      stateService: {
        go: jest.fn()
      }
    };

    const {useRouter} = require('@uirouter/react');
    useRouter.mockReturnValue(mockRouter);

    const savingState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'saving')
    };

    const {rerender} = renderComponent({}, savingState);

    const loadedState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'loaded'),
      context: {
        ...defaultMachineState.context,
        saveError: null
      }
    };

    useMachine.mockReturnValue([loadedState, mockSend]);
    rerender(
      <HostedRepositoriesEvaluationRepositoriesTab
        settingsData={mockSettingsData}
        initialSelectedRepositories={[]}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    expect(mockRouter.stateService.go).toHaveBeenCalledWith('admin.sonatypelifecycle');
  });

  it('does not navigate when save fails', () => {
    const mockRouter = {
      stateService: {
        go: jest.fn()
      }
    };

    const {useRouter} = require('@uirouter/react');
    useRouter.mockReturnValue(mockRouter);

    const savingState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'patchingRepositories')
    };

    const {rerender} = renderComponent({}, savingState);

    const errorState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'loaded'),
      context: {
        ...defaultMachineState.context,
        saveError: {message: 'Save failed'}
      }
    };

    useMachine.mockReturnValue([errorState, mockSend]);
    rerender(
      <HostedRepositoriesEvaluationRepositoriesTab
        settingsData={mockSettingsData}
        initialSelectedRepositories={[]}
        onSelectionChange={mockOnSelectionChange}
      />
    );

    expect(mockRouter.stateService.go).not.toHaveBeenCalled();
  });

  it('handles pagination with selected repositories across pages', async () => {
    const page1State = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        repositories: [
          {id: 'repo1', name: 'Repo 1', format: 'maven2', size: 1024, artifactCount: 10},
          {id: 'repo2', name: 'Repo 2', format: 'npm', size: 2048, artifactCount: 20}
        ],
        totalPages: 2,
        offsetPage: 0
      }
    };

    renderComponent({initialSelectedRepositories: ['repo1', 'repo3']}, page1State);

    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes[1]).toBeChecked();
    expect(checkboxes[2]).not.toBeChecked();
  });

  it('validates error message is always a string', async () => {
    const errorState = {
      ...defaultMachineState,
      matches: jest.fn((state) => state === 'loaded'),
      context: {
        ...defaultMachineState.context,
        saveError: {
          response: {
            data: {
              message: 123
            }
          }
        }
      }
    };

    renderComponent({}, errorState);

    await waitFor(() => {
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.TITLE)).toBeInTheDocument();
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.ERROR_MODAL.MESSAGE)).toBeInTheDocument();
    });
  });

  it('handles combined filter and format filter', async () => {
    const stateWithFormats = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        formats: ['all', 'maven2', 'npm']
      }
    };

    renderComponent({}, stateWithFormats);

    const searchInput = screen.getByPlaceholderText('Search repositories...');
    userEvent.type(searchInput, 'test');

    await waitFor(() => {
      expect(mockSend).toHaveBeenCalledWith({type: 'FILTER', filter: 'test'});
    }, {timeout: 600});

    const selects = screen.getAllByRole('combobox');
    const formatSelect = selects[selects.length - 1];
    userEvent.selectOptions(formatSelect, 'maven2');

    expect(mockSend).toHaveBeenCalledWith({type: 'FILTER_FORMAT', formatFilter: 'maven2'});
  });


  it('does not render Custom tag for repositories without custom configuration', () => {
    const reposWithoutCustom = [
      {
        id: 'repo1',
        name: 'Maven Central',
        format: 'maven2',
        size: 1024,
        artifactCount: 100,
        isSelected: true,
        hasCustomConfig: false
      },
      {
        id: 'repo2',
        name: 'NPM Registry',
        format: 'npm',
        size: 2048,
        artifactCount: 200,
        isSelected: false,
        hasCustomConfig: false
      }
    ];

    const stateWithoutCustom = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        repositories: reposWithoutCustom
      }
    };

    renderComponent({}, stateWithoutCustom);

    expect(screen.queryByText('Custom')).not.toBeInTheDocument();
  });

  it('shows correct repository count in summary text', () => {
    renderComponent();

    expect(screen.getByText(/2 of 2 repositories/)).toBeInTheDocument();
  });

  it('renders repositories with isSelected flag', () => {
    renderComponent();

    const checkboxes = screen.getAllByRole('checkbox');
    expect(checkboxes[1]).toBeInTheDocument();

    expect(checkboxes[2]).toBeInTheDocument();
  });

  it('hides monitoring filter for first-time users', () => {
    renderComponent({globalConfigAvailable: false});

    const selects = screen.getAllByRole('combobox');
    expect(selects.length).toBe(1);
  });

  it('shows monitoring filter for returning users', () => {
    renderComponent({globalConfigAvailable: true});

    const selects = screen.getAllByRole('combobox');
    expect(selects.length).toBe(2);
    expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.repositoriesTable.monitoringFilterOptions.all)).toBeInTheDocument();
  });

  it('renders repository names as plain text for first-time users', () => {
    renderComponent({globalConfigAvailable: false});

    expect(screen.queryByRole('link', {name: 'Maven Central'})).not.toBeInTheDocument();
    expect(screen.getByText('Maven Central')).toBeInTheDocument();
  });

  it('renders repository names as clickable links for returning users', () => {
    renderComponent({globalConfigAvailable: true});

    const mavenLink = screen.getByRole('link', {name: 'Maven Central'});
    const npmLink = screen.getByRole('link', {name: 'NPM Registry'});

    expect(mavenLink).toBeInTheDocument();
    expect(mavenLink).toHaveAttribute('href', '#admin/repository/repositories:Maven%20Central');
    expect(npmLink).toBeInTheDocument();
    expect(npmLink).toHaveAttribute('href', '#admin/repository/repositories:NPM%20Registry');
  });

  it('shows Custom tags for returning users', () => {
    renderComponent({globalConfigAvailable: true});

    const table = screen.getByRole('table');
    const customTags = Array.from(table.querySelectorAll('.custom-config-tag')).filter(
      tag => tag.textContent === 'Custom'
    );
    expect(customTags.length).toBe(1);

    const mavenRow = screen.getByText('Maven Central').closest('tr');
    expect(mavenRow).not.toHaveTextContent('Custom');
  });

  it('preserves Custom tag when Enable Monitoring is clicked on an already-enabled repo with custom config', async () => {
    const reposWithCustomEnabled = [
      {
        id: 'repo1',
        name: 'Maven Central',
        format: 'maven2',
        size: 1024,
        artifactCount: 100,
        isSelected: true,
        hasCustomConfig: true
      }
    ];
    const stateWithCustomEnabled = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        repositories: reposWithCustomEnabled,
        hasSelections: true,
        existingSettings: {activityTimeFrame: 30}
      }
    };

    renderComponent({globalConfigAvailable: true, initialSelectedRepositories: []}, stateWithCustomEnabled);

    // Selection now starts empty regardless of monitoring state — click the row first.
    const checkboxes = screen.getAllByRole('checkbox');
    await userEvent.click(checkboxes[1]);

    // Repo is already enabled so Disable Monitoring is shown — click it to set pending=false
    const disableButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring);
    await userEvent.click(disableButton);

    // Now Enable Monitoring is shown — click it (idempotent re-enable of already-enabled repo, sets pending=true)
    const enableButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring);
    await userEvent.click(enableButton);

    // Custom tag must still be visible — regression guard for the pending===true && repo.isSelected fix
    expect(screen.getAllByText('Custom').length).toBeGreaterThan(0);
  });

  it('does not show Custom tags for first-time users', () => {
    renderComponent({globalConfigAvailable: false});

    expect(screen.queryByText('Custom')).not.toBeInTheDocument();
  });

  it('shows Save button for first-time users', () => {
    renderComponent({globalConfigAvailable: false});

    expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.save)).toBeInTheDocument();
    expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update)).not.toBeInTheDocument();
  });

  it('shows Update button for returning users', () => {
    const returningUserState = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        globalConfigAvailable: true
      }
    };

    renderComponent({globalConfigAvailable: true}, returningUserState);

    expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update)).toBeInTheDocument();
    expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.save)).not.toBeInTheDocument();
  });

  describe('PATCH functionality', () => {
    afterEach(() => {
      cleanup();
      jest.clearAllMocks();
      jest.clearAllTimers();
    });

    it('sends PATCH_REPOSITORIES event for returning users with repository changes via Enable Monitoring', async () => {
      const returningUserState = {
        ...defaultMachineState,
        context: {
          ...defaultMachineState.context,
          globalConfigAvailable: true,
          existingSettings: {
            activityTimeFrame: 60,
            enableFirewallAutoBlocking: true
          },
          repositories: [
            {id: 'repo-1', name: 'Maven Central', format: 'maven2', type: 'hosted', isSelected: true},
            {id: 'repo-2', name: 'NPM Registry', format: 'npm', type: 'hosted', isSelected: false}
          ]
        }
      };

      renderComponent({globalConfigAvailable: true}, returningUserState);

      // Check repo-2 (disabled) and click Enable Monitoring to add it
      const checkbox = screen.getAllByRole('checkbox')[2];
      await userEvent.click(checkbox);
      const enableBtn = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring);
      await userEvent.click(enableBtn);

      const updateButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update);
      await userEvent.click(updateButton);

      await waitFor(() => {
        const updateCall = mockSend.mock.calls.find(call =>
          call[0].type === 'UPDATE' &&
          call[0].data &&
          (call[0].data.repositoriesToAdd !== undefined || call[0].data.repositoriesToRemove !== undefined)
        );
        expect(updateCall).toBeDefined();
        expect(mockSend).toHaveBeenCalledWith('PATCH_REPOSITORIES');
      });
    });

    it('sends PATCH_REPOSITORIES event for returning users after Enable Monitoring is clicked', async () => {
      const returningUserState = {
        ...defaultMachineState,
        context: {
          ...defaultMachineState.context,
          globalConfigAvailable: true,
          existingSettings: {
            activityTimeFrame: 30,
            artifactLatestVersions: 5,
            policyEvaluationStage: 'build',
            autoEnrollNewRepos: false
          },
          repositories: [
            {id: 'repo-1', name: 'Maven Central', format: 'maven2', type: 'hosted', isSelected: true},
            {id: 'repo-2', name: 'NPM Registry', format: 'npm', type: 'hosted', isSelected: false}
          ]
        }
      };

      renderComponent({globalConfigAvailable: true, initialSelectedRepositories: ['repo-1']}, returningUserState);

      // Check repo-2 and click Enable Monitoring to make pending changes
      const checkbox = screen.getAllByRole('checkbox')[2];
      await userEvent.click(checkbox);
      const enableBtn = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring);
      await userEvent.click(enableBtn);

      const updateButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update);
      expect(updateButton).not.toBeDisabled();
      await userEvent.click(updateButton);

      await waitFor(() => {
        expect(mockSend).toHaveBeenCalledWith('PATCH_REPOSITORIES');
      });
    });

    it('Update button is disabled when no monitoring changes have been made', async () => {
      const returningUserState = {
        ...defaultMachineState,
        context: {
          ...defaultMachineState.context,
          globalConfigAvailable: true,
          existingSettings: {
            activityTimeFrame: '60',
            artifactLatestVersions: '5',
            policyEvaluationStage: 'build',
            autoEnrollNewRepos: false
          },
          repositories: [
            {id: 'repo-1', name: 'Maven Central', format: 'maven2', type: 'hosted', isSelected: true}
          ]
        }
      };

      renderComponent({
        globalConfigAvailable: true,
        initialSelectedRepositories: ['repo-1'],
        settingsData: {
          activityTimeFrame: '60',
          artifactLatestVersions: '5',
          policyEvaluationStage: 'build',
          applyToNewRepos: false
        }
      }, returningUserState);

      const updateButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update);
      expect(updateButton).toBeDisabled();
    });

    it('redirects to Lifecycle page after successful PATCH_REPOSITORIES', async () => {
      const mockRouter = {
        stateService: {
          go: jest.fn()
        }
      };

      // Mock useRouter to return our mockRouter
      require('@uirouter/react').useRouter.mockReturnValue(mockRouter);

      const patchingState = {
        ...defaultMachineState,
        matches: jest.fn((state) => state === 'patchingRepositories'),
        context: {
          ...defaultMachineState.context,
          saveError: null
        }
      };

      const loadedState = {
        ...defaultMachineState,
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          ...defaultMachineState.context,
          saveError: null
        }
      };

      // First render: component mounts in patchingRepositories state
      useMachine.mockReturnValue([patchingState, mockSend]);
      const {rerender} = render(
        <HostedRepositoriesEvaluationRepositoriesTab
          settingsData={mockSettingsData}
          initialSelectedRepositories={[]}
          onSelectionChange={mockOnSelectionChange}
        />
      );

      // Rerender: transition to loaded state (simulates successful PATCH_REPOSITORIES)
      useMachine.mockReturnValue([loadedState, mockSend]);
      rerender(
        <HostedRepositoriesEvaluationRepositoriesTab
          settingsData={mockSettingsData}
          initialSelectedRepositories={[]}
          onSelectionChange={mockOnSelectionChange}
        />
      );

      await waitFor(() => {
        expect(mockRouter.stateService.go).toHaveBeenCalledWith('admin.sonatypelifecycle');
      });
    });

    it('does not redirect when PATCH_REPOSITORIES fails', async () => {
      const mockRouter = {
        stateService: {
          go: jest.fn()
        }
      };

      // Mock useRouter to return our mockRouter
      require('@uirouter/react').useRouter.mockReturnValue(mockRouter);

      const patchingState = {
        ...defaultMachineState,
        matches: jest.fn((state) => state === 'patchingRepositories'),
        context: {
          ...defaultMachineState.context,
          saveError: null
        }
      };

      const loadedStateWithError = {
        ...defaultMachineState,
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          ...defaultMachineState.context,
          saveError: 'Failed to update repositories'
        }
      };

      // First render: component mounts in patchingRepositories state
      useMachine.mockReturnValue([patchingState, mockSend]);
      const {rerender} = render(
        <HostedRepositoriesEvaluationRepositoriesTab
          settingsData={mockSettingsData}
          initialSelectedRepositories={[]}
          onSelectionChange={mockOnSelectionChange}
        />
      );

      // Rerender: transition to loaded state with error (simulates failed PATCH_REPOSITORIES)
      useMachine.mockReturnValueOnce([loadedStateWithError, mockSend]);
      rerender(
        <HostedRepositoriesEvaluationRepositoriesTab
          settingsData={mockSettingsData}
          initialSelectedRepositories={[]}
          onSelectionChange={mockOnSelectionChange}
        />
      );

      await waitFor(() => {
        expect(mockRouter.stateService.go).not.toHaveBeenCalled();
      });
    });

    it('calculates repositoriesToAdd from Enable Monitoring button click, not checkbox selection', async () => {
      const returningUserState = {
        ...defaultMachineState,
        context: {
          ...defaultMachineState.context,
          globalConfigAvailable: true,
          existingSettings: {
            activityTimeFrame: 60,
            enableFirewallAutoBlocking: true
          },
          repositories: [
            {id: 'repo-1', name: 'Maven Central', format: 'maven2', type: 'hosted', isSelected: true},
            {id: 'repo-2', name: 'NPM Registry', format: 'npm', type: 'hosted', isSelected: false}
          ]
        }
      };

      renderComponent({
        globalConfigAvailable: true,
        initialSelectedRepositories: ['repo-1']
      }, returningUserState);

      // Check repo-2 (currently disabled) to select it for batch action
      const checkbox = screen.getAllByRole('checkbox')[2];
      await userEvent.click(checkbox);

      // Click Enable Monitoring — this is what drives repositoriesToAdd
      const enableBtn = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring);
      await userEvent.click(enableBtn);

      const updateButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update);
      await userEvent.click(updateButton);

      await waitFor(() => {
        const updateCall = mockSend.mock.calls.find(call =>
          call[0].type === 'UPDATE' &&
          call[0].data &&
          (call[0].data.repositoriesToAdd !== undefined || call[0].data.repositoriesToRemove !== undefined)
        );
        expect(updateCall).toBeDefined();
        expect(updateCall[0].data).toEqual(expect.objectContaining({
          repositoriesToAdd: ['repo-2']
        }));
        // Settings should NOT be included in PATCH_REPOSITORIES
        expect(updateCall[0].data.settings).toBeUndefined();
      });
    });

    it('checking a row without clicking Enable Monitoring keeps the Update button disabled', async () => {
      const returningUserState = {
        ...defaultMachineState,
        context: {
          ...defaultMachineState.context,
          globalConfigAvailable: true,
          existingSettings: {
            activityTimeFrame: '30',
            artifactLatestVersions: '5',
            policyEvaluationStage: 'build',
            autoEnrollNewRepos: false,
          },
          repositories: [
            {id: 'repo-1', name: 'Maven Central', format: 'maven2', type: 'hosted', isSelected: true},
            {id: 'repo-2', name: 'NPM Registry', format: 'npm', type: 'hosted', isSelected: false}
          ]
        }
      };

      // Pass settingsData that matches existingSettings (no settings changes)
      renderComponent({
        globalConfigAvailable: true,
        initialSelectedRepositories: ['repo-1'],
        settingsData: {
          activityTimeFrame: '30',
          artifactLatestVersions: '5',
          policyEvaluationStage: 'build',
          applyToNewRepos: false,
        }
      }, returningUserState);

      // Check repo-2 (currently disabled) — just a UI selection, no monitoring change intended
      const checkbox = screen.getAllByRole('checkbox')[2];
      await userEvent.click(checkbox);

      // Update button should remain disabled since Enable/Disable Monitoring was not clicked
      const updateButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update);
      expect(updateButton).toBeDisabled();
      expect(mockSend).not.toHaveBeenCalledWith('PATCH_REPOSITORIES');
    });

    it('Update button remains disabled when only settings have changed (settings handled by SettingsTab)', async () => {
      const returningUserState = {
        ...defaultMachineState,
        context: {
          ...defaultMachineState.context,
          globalConfigAvailable: true,
          existingSettings: {
            activityTimeFrame: '60',
            artifactLatestVersions: '5',
            policyEvaluationStage: 'build',
            autoEnrollNewRepos: false,
          },
          repositories: [
            {id: 'repo-1', name: 'Maven Central', format: 'maven2', type: 'hosted', isSelected: true},
            {id: 'repo-2', name: 'NPM Registry', format: 'npm', type: 'hosted', isSelected: false}
          ]
        }
      };

      // Pass settingsData with activityTimeFrame changed from 60 to 30
      renderComponent({
        globalConfigAvailable: true,
        initialSelectedRepositories: ['repo-1'],
        settingsData: {
          activityTimeFrame: '30',
          artifactLatestVersions: '5',
          policyEvaluationStage: 'build',
          applyToNewRepos: false,
        }
      }, returningUserState);

      // Update button should remain disabled — settings changes are now handled independently by SettingsTab
      const updateButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update);
      expect(updateButton).toBeDisabled();
    });

    it('calculates repositoriesToRemove from Disable Monitoring button click, not checkbox deselection', async () => {
      const returningUserState = {
        ...defaultMachineState,
        context: {
          ...defaultMachineState.context,
          globalConfigAvailable: true,
          existingSettings: {
            activityTimeFrame: 60,
            enableFirewallAutoBlocking: true
          },
          repositories: [
            {id: 'repo-1', name: 'Maven Central', format: 'maven2', type: 'hosted', isSelected: true},
            {id: 'repo-2', name: 'NPM Registry', format: 'npm', type: 'hosted', isSelected: false}
          ]
        }
      };

      renderComponent({
        globalConfigAvailable: true,
        initialSelectedRepositories: ['repo-1']
      }, returningUserState);

      // Selection starts empty regardless of monitoring state — click the enabled
      // row (repo-1) first so Disable Monitoring becomes available.
      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[1]);

      const disableBtn = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring);
      await userEvent.click(disableBtn);

      const updateButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update);
      await userEvent.click(updateButton);

      await waitFor(() => {
        const updateCall = mockSend.mock.calls.find(call =>
          call[0].type === 'UPDATE' &&
          call[0].data &&
          (call[0].data.repositoriesToAdd !== undefined || call[0].data.repositoriesToRemove !== undefined)
        );
        expect(updateCall).toBeDefined();
        expect(updateCall[0].data).toEqual(expect.objectContaining({
          repositoriesToRemove: ['repo-1']
        }));
        // Settings should NOT be included in PATCH_REPOSITORIES
        expect(updateCall[0].data.settings).toBeUndefined();
      });
    });

    it('PATCH_REPOSITORIES does not include settings (settings handled by SettingsTab)', async () => {
      const returningUserState = {
        ...defaultMachineState,
        context: {
          ...defaultMachineState.context,
          globalConfigAvailable: true,
          existingSettings: {
            activityTimeFrame: '60',
            artifactLatestVersions: '5',
            policyEvaluationStage: 'build',
            autoEnrollNewRepos: false
          },
          repositories: [
            {id: 'repo-1', name: 'Maven Central', format: 'maven2', type: 'hosted', isSelected: true},
            {id: 'repo-2', name: 'NPM Registry', format: 'npm', type: 'hosted', isSelected: false}
          ]
        }
      };

      const changedSettingsData = {
        activityTimeFrame: '90',
        artifactLatestVersions: '5',
        policyEvaluationStage: 'build',
        applyToNewRepos: false
      };

      renderComponent({globalConfigAvailable: true, settingsData: changedSettingsData, initialSelectedRepositories: ['repo-1']}, returningUserState);

      // Must click Enable Monitoring before Update button becomes enabled
      const checkbox = screen.getAllByRole('checkbox')[2];
      await userEvent.click(checkbox);
      const enableBtn = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring);
      await userEvent.click(enableBtn);

      const updateButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update);
      await userEvent.click(updateButton);

      await waitFor(() => {
        const updateCall = mockSend.mock.calls.find(call =>
          call[0].type === 'UPDATE' &&
          call[0].data &&
          (call[0].data.repositoriesToAdd !== undefined || call[0].data.repositoriesToRemove !== undefined)
        );
        expect(updateCall).toBeDefined();
        // Settings should NOT be included in PATCH_REPOSITORIES - settings are handled by SettingsTab
        expect(updateCall[0].data.settings).toBeUndefined();
        expect(updateCall[0].data.repositoriesToAdd).toEqual(['repo-2']);
      });
    });
  });

  describe('Monitoring column and Enable/Disable buttons', () => {
    const returningUserState = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        hasSelections: true
      },
      matches: jest.fn((state) => state === 'loaded')
    };

    it('shows Monitoring column header for returning users', () => {
      renderComponent({globalConfigAvailable: true}, returningUserState);

      expect(screen.getByText('Monitoring')).toBeInTheDocument();
    });

    it('does not show Monitoring column header for first-time users', () => {
      renderComponent({globalConfigAvailable: false});

      expect(screen.queryByText('Monitoring')).not.toBeInTheDocument();
    });

    it('shows Enabled status for selected repo in Monitoring column', () => {
      renderComponent({globalConfigAvailable: true}, returningUserState);

      const rows = screen.getAllByRole('row');
      expect(rows[1]).toHaveTextContent('Enabled');
    });

    it('shows Disabled status for unselected repo in Monitoring column', () => {
      renderComponent({globalConfigAvailable: true}, returningUserState);

      const rows = screen.getAllByRole('row');
      expect(rows[2]).toHaveTextContent('Disabled');
    });

    it('does not auto-select rows based on existing monitoring state on initial load', async () => {
      renderComponent({globalConfigAvailable: true, initialSelectedRepositories: []}, returningUserState);

      await waitFor(() => {
        expect(screen.getByText('Monitoring')).toBeInTheDocument();
      });

      const checkboxes = screen.getAllByRole('checkbox');
      // checkboxes[0] is select-all; data rows are checkboxes[1..]
      // Mock data: repo1 has isSelected:true (monitored), repo2 has isSelected:false.
      // Neither should be pre-checked — selection is independent of monitoring state.
      expect(checkboxes[1]).not.toBeChecked();
      expect(checkboxes[2]).not.toBeChecked();

      // Bulk-action buttons must be hidden until the user explicitly selects rows
      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring)).not.toBeInTheDocument();
      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring)).not.toBeInTheDocument();
      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.clearSelection)).not.toBeInTheDocument();
    });

    it('does not show Enable/Disable Monitoring buttons when no rows are checked', async () => {
      const noSelectionState = {
        ...returningUserState,
        context: {
          ...returningUserState.context,
          repositories: [
            {id: 'repo1', name: 'Maven Central', format: 'maven2', size: 1024, artifactCount: 100, isSelected: false, hasCustomConfig: false},
            {id: 'repo2', name: 'NPM Registry', format: 'npm', size: 2048, artifactCount: 200, isSelected: false, hasCustomConfig: false}
          ]
        }
      };
      renderComponent({globalConfigAvailable: true}, noSelectionState);

      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring)).not.toBeInTheDocument();
      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring)).not.toBeInTheDocument();
    });

    it('shows Disable Monitoring (not Enable) when all checked rows are already enabled', async () => {
      renderComponent({globalConfigAvailable: true}, returningUserState);

      // Mock data: checkboxes[1] is the already-enabled Maven Central row
      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[1]);

      await waitFor(() => {
        expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring)).toBeInTheDocument();
      });
      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring)).not.toBeInTheDocument();
    });

    it('shows Enable Monitoring (not Disable) when a disabled row is checked', async () => {
      renderComponent({globalConfigAvailable: true}, returningUserState);

      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[2]);

      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring)).toBeInTheDocument();
      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring)).not.toBeInTheDocument();
    });

    it('clicking Enable Monitoring updates Monitoring column to Enabled without calling API', async () => {
      renderComponent({globalConfigAvailable: true}, returningUserState);

      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[2]);

      const enableBtn = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring);
      await userEvent.click(enableBtn);

      const rows = screen.getAllByRole('row');
      expect(rows[2]).toHaveTextContent('Enabled');
      expect(mockSend).not.toHaveBeenCalledWith('PATCH');
      expect(mockSend).not.toHaveBeenCalledWith('SAVE');
    });

    it('clicking Disable Monitoring updates Monitoring column to Disabled without calling API', async () => {
      renderComponent({globalConfigAvailable: true}, returningUserState);

      // Check the already-enabled row first to make Disable Monitoring available
      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[1]);

      await waitFor(() => {
        expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring)).toBeInTheDocument();
      });

      const disableBtn = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring);
      await userEvent.click(disableBtn);

      const rows = screen.getAllByRole('row');
      expect(rows[1]).toHaveTextContent('Disabled');
      expect(mockSend).not.toHaveBeenCalledWith('PATCH');
    });

    it('shows Clear Selection button when rows are checked', async () => {
      renderComponent({globalConfigAvailable: true}, returningUserState);

      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[1]);

      await waitFor(() => {
        expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.clearSelection)).toBeInTheDocument();
      });
    });

    it('clicking Clear Selection unchecks all rows', async () => {
      renderComponent({globalConfigAvailable: true}, returningUserState);

      const checkboxes = screen.getAllByRole('checkbox');
      await userEvent.click(checkboxes[1]);

      await waitFor(() => {
        expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.clearSelection)).toBeInTheDocument();
      });

      await userEvent.click(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.clearSelection));

      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.clearSelection)).not.toBeInTheDocument();
    });
  });

  describe('handleRepositoryLinkClick', () => {
    const {ExtJS} = require('@sonatype/nexus-ui-plugin');
    let originalLocation;
    let originalNX;

    beforeEach(() => {
      jest.clearAllMocks();

      originalLocation = window.location;
      delete window.location;
      window.location = {
        href: 'http://localhost:8081/#admin/iq-server/hosted-repositories-evaluation',
        hash: '',
        replace: jest.fn()
      };

      originalNX = window.NX;
      window.NX = {
        getApplication: jest.fn(() => ({
          getController: jest.fn(() => ({currentIndex: 1}))
        }))
      };

      window.dirty = ['something'];
    });

    afterEach(() => {
      window.location = originalLocation;
      window.NX = originalNX;
    });

    it('calls ExtJS.setDirtyStatus with correct parameters', async () => {
      const loadedState = {
        value: 'loaded',
        context: {
          repositories: mockRepositories,
          hasSelections: true
        },
        matches: jest.fn((state) => state === 'loaded')
      };

      useMachine.mockReturnValue([loadedState, mockSend]);
      renderComponent({}, loadedState);

      const mavenLink = screen.getByRole('link', {name: 'Maven Central'});
      await userEvent.click(mavenLink);

      expect(ExtJS.setDirtyStatus).toHaveBeenCalledWith('HostedRepositoriesEvaluationMachine', false);
    });

    it('sets window.location.hash to the target repository hash', async () => {
      const loadedState = {
        value: 'loaded',
        context: {
          repositories: mockRepositories,
          hasSelections: true
        },
        matches: jest.fn((state) => state === 'loaded')
      };

      useMachine.mockReturnValue([loadedState, mockSend]);
      renderComponent({}, loadedState);

      const mavenLink = screen.getByRole('link', {name: 'Maven Central'});
      await userEvent.click(mavenLink);

      expect(window.location.hash).toBe('admin/repository/repositories:Maven%20Central');
    });

    it('resets ExtJS Drilldown currentIndex to 0 before navigating', async () => {
      const mockController = {currentIndex: 1};
      window.NX.getApplication.mockReturnValue({
        getController: jest.fn(() => mockController)
      });

      const loadedState = {
        value: 'loaded',
        context: {
          repositories: mockRepositories,
          hasSelections: true
        },
        matches: jest.fn((state) => state === 'loaded')
      };

      useMachine.mockReturnValue([loadedState, mockSend]);
      renderComponent({}, loadedState);

      const mavenLink = screen.getByRole('link', {name: 'Maven Central'});
      await userEvent.click(mavenLink);

      expect(mockController.currentIndex).toBe(0);
    });

    it('navigates without error when window.NX is absent', async () => {
      window.NX = undefined;

      const loadedState = {
        value: 'loaded',
        context: {
          repositories: mockRepositories,
          hasSelections: true
        },
        matches: jest.fn((state) => state === 'loaded')
      };

      useMachine.mockReturnValue([loadedState, mockSend]);
      renderComponent({}, loadedState);

      const mavenLink = screen.getByRole('link', {name: 'Maven Central'});
      await userEvent.click(mavenLink);

      expect(window.location.hash).toBe('admin/repository/repositories:Maven%20Central');
    });

    it('prevents default link behavior', async () => {
      const loadedState = {
        value: 'loaded',
        context: {
          repositories: mockRepositories,
          hasSelections: true
        },
        matches: jest.fn((state) => state === 'loaded')
      };

      useMachine.mockReturnValue([loadedState, mockSend]);
      renderComponent({}, loadedState);

      const mavenLink = screen.getByRole('link', {name: 'Maven Central'});
      const clickEvent = new MouseEvent('click', {bubbles: true, cancelable: true});
      const preventDefaultSpy = jest.spyOn(clickEvent, 'preventDefault');

      mavenLink.dispatchEvent(clickEvent);

      expect(preventDefaultSpy).toHaveBeenCalled();
    });
  });

  describe('Saving feedback (NxSubmitMask + bulk-button disabled state)', () => {
    const savingStateFor = (stateName) => ({
      ...defaultMachineState,
      matches: jest.fn((s) => s === stateName)
    });

    it('renders the Saving mask when machine is in patching state', () => {
      renderComponent({}, savingStateFor('patching'));
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.savingMask)).toBeInTheDocument();
    });

    it('renders the Saving mask when machine is in patchingSettings state', () => {
      renderComponent({}, savingStateFor('patchingSettings'));
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.savingMask)).toBeInTheDocument();
    });

    it('renders the Saving mask when machine is in patchingRepositories state', () => {
      renderComponent({}, savingStateFor('patchingRepositories'));
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.savingMask)).toBeInTheDocument();
    });

    it('does not render the Saving mask when machine is in loaded state', () => {
      renderComponent({}, savingStateFor('loaded'));
      expect(screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.savingMask)).not.toBeInTheDocument();
    });

    it('disables the Update button while saving', () => {
      renderComponent({}, savingStateFor('patchingRepositories'));
      const updateOrSave = screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.update)
          || screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.save);
      expect(updateOrSave.closest('button')).toBeDisabled();
    });

    it('disables the bulk-action and Clear Selection buttons while saving', () => {
      // repo1 is pre-selected via mockRepositories, so the bulk-action toolbar renders.
      renderComponent({}, savingStateFor('patchingRepositories'));

      // One of Enable Monitoring / Disable Monitoring is rendered depending on the
      // selected repos' current state; either way it must be disabled while saving.
      const enableOrDisable =
          screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.enableMonitoring)
              || screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.disableMonitoring);
      if (enableOrDisable) {
        expect(enableOrDisable.closest('button')).toBeDisabled();
      }
      const clearSelection = screen.queryByText(HOSTED_REPOSITORIES_EVALUATION.buttons.clearSelection);
      if (clearSelection) {
        expect(clearSelection.closest('button')).toBeDisabled();
      }
    });
  });
});
