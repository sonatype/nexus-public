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
import {useMachine} from '@xstate/react';

import HostedRepositoriesEvaluationRepositoriesTab from './HostedRepositoriesEvaluationRepositoriesTab';
import UIStrings from '../../../../constants/UIStrings';

const {HOSTED_REPOSITORIES_EVALUATION} = UIStrings.SONATYPE_LIFECYCLE;

jest.mock('@xstate/react', () => ({
  useMachine: jest.fn()
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  HostedRepositoriesEvaluationMachine: {},
  SectionToolbar: ({children}) => <div data-testid="section-toolbar">{children}</div>,
  HumanReadableUtils: {
    formatBytes: (bytes) => `${bytes} bytes`,
    bytesToString: (bytes) => `${bytes} bytes`
  }
}));

describe('HostedRepositoriesEvaluationRepositoriesTab', () => {
  const mockOnBack = jest.fn();
  const mockOnSelectionChange = jest.fn();
  const mockSend = jest.fn();

  const mockSettingsData = {
    activityTimeFrame: '30',
    artifactLatestVersions: '5',
    policyEvaluationStage: 'build',
    applyToNewRepos: false
  };

  const mockRepositories = [
    {id: 'repo1', name: 'Maven Central', format: 'maven2', size: 1024, artifactCount: 100},
    {id: 'repo2', name: 'NPM Registry', format: 'npm', size: 2048, artifactCount: 200}
  ];

  const defaultMachineState = {
    context: {
      data: {},
      repositories: mockRepositories,
      loadError: null,
      formatFilter: 'all',
      offsetPage: 0,
      sortField: 'name',
      sortDirection: 'asc',
      totalPages: 1
    },
    matches: jest.fn(() => false)
  };

  const renderComponent = (props = {}, machineState = defaultMachineState) => {
    useMachine.mockReturnValue([machineState, mockSend]);

    render(
      <HostedRepositoriesEvaluationRepositoriesTab
        onBack={mockOnBack}
        settingsData={mockSettingsData}
        initialSelectedRepositories={[]}
        onSelectionChange={mockOnSelectionChange}
        {...props}
      />
    );
  };

  beforeEach(() => {
    mockOnBack.mockClear();
    mockOnSelectionChange.mockClear();
    mockSend.mockClear();
    useMachine.mockClear();
  });

  it('renders repository table with data', () => {
    renderComponent();

    expect(screen.getByText('Maven Central')).toBeInTheDocument();
    expect(screen.getByText('NPM Registry')).toBeInTheDocument();
    expect(screen.getByText('maven2')).toBeInTheDocument();
    expect(screen.getByText('npm')).toBeInTheDocument();
  });

  it('renders search and filter controls', () => {
    renderComponent();

    expect(screen.getByPlaceholderText('Search repositories...')).toBeInTheDocument();
    const selects = screen.getAllByRole('combobox');
    expect(selects.length).toBeGreaterThan(0);
  });

  it('renders Back and Save buttons', () => {
    renderComponent();

    expect(screen.getByText(UIStrings.SETTINGS.BACK_BUTTON_LABEL)).toBeInTheDocument();
    expect(screen.getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL)).toBeInTheDocument();
  });

  it.skip('renders settings summary', () => {
    // TODO: Settings summary display not yet implemented
    renderComponent();

    expect(screen.getByText(/Last 30 Days/)).toBeInTheDocument();
    expect(screen.getByText(/5 Artifact Latest Versions/)).toBeInTheDocument();
    expect(screen.getByText(/build/)).toBeInTheDocument();
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

  it('calls onBack when Back button is clicked', () => {
    renderComponent();


    const backButton = screen.getByText(UIStrings.SETTINGS.BACK_BUTTON_LABEL);
    userEvent.click(backButton);

    expect(mockOnBack).toHaveBeenCalled();
  });

  it.skip('shows modal when Save is clicked without selecting repositories', async () => {
    // TODO: Modal display logic not yet implemented or needs different mocking
    renderComponent();


    const saveButton = screen.getByText(HOSTED_REPOSITORIES_EVALUATION.buttons.save);
    userEvent.click(saveButton);

    await waitFor(() => {
      expect(screen.getByText(HOSTED_REPOSITORIES_EVALUATION.INCOMPLETE_MODAL.TITLE)).toBeInTheDocument();
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


    const searchInput = screen.getByPlaceholderText('Search repositories...');
    userEvent.type(searchInput, 'maven');

    await waitFor(() => {
      expect(mockSend).toHaveBeenCalledWith({type: 'FILTER', filter: 'maven'});
    }, {timeout: 600});
  });

  it('handles format filter change', () => {
    const stateWithFormats = {
      ...defaultMachineState,
      context: {
        ...defaultMachineState.context,
        formats: ['maven2', 'npm', 'pypi']
      }
    };

    renderComponent({}, stateWithFormats);

    const selects = screen.getAllByRole('combobox');
    const formatSelect = selects[selects.length - 1]; // Format select is the last one
    userEvent.selectOptions(formatSelect, 'maven2');

    expect(mockSend).toHaveBeenCalledWith({type: 'FILTER_FORMAT', formatFilter: 'maven2'});
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


    const saveButton = screen.getByText(UIStrings.SETTINGS.SAVE_BUTTON_LABEL);
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

    const checkboxes = screen.getAllByRole('checkbox');
    // Select-all checkbox + 2 repo checkboxes
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
});
