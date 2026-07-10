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
import Axios from 'axios';

import EvaluationConfiguration from './EvaluationConfiguration';

jest.mock('axios');

describe('EvaluationConfiguration', () => {
  const mockSend = jest.fn();
  const mockParentMachine = [
    {
      context: {
        data: {
          attributes: {
            evaluation: {
              mode: 'INHERIT'
            }
          }
        }
      }
    },
    mockSend
  ];

  const mockGlobalSettings = {
    activityTimeFrame: 30,
    artifactLatestVersions: 3,
    policyEvaluationStage: 'stage-release'
  };

  beforeEach(() => {
    mockSend.mockClear();

    // Mock window.NX.State.getValue for feature flag (preserve other NX properties from setup.js)
    if (!global.window.NX) {
      global.window.NX = {};
    }
    if (!global.window.NX.State) {
      global.window.NX.State = {};
    }
    global.window.NX.State.getValue = jest.fn().mockReturnValue(true);

    // Mock Axios GET for global settings
    Axios.get.mockImplementation((url) => {
      if (url === '/service/rest/v1/evaluation/settings') {
        return Promise.resolve({ data: mockGlobalSettings });
      }
      return Promise.resolve({ data: {} });
    });
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders with inherit mode by default', () => {
    render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    expect(screen.getByText('Evaluation')).toBeInTheDocument();
    expect(screen.getByText('Inherit from global evaluation settings')).toBeInTheDocument();
    expect(screen.getByText('Override')).toBeInTheDocument();
    expect(screen.getByText('Disable Evaluation')).toBeInTheDocument();
  });

  it('shows override fields when override mode is selected', () => {
    const overrideStateMachine = [
      {
        context: {
          data: {
            attributes: {
              evaluation: {
                mode: 'OVERRIDE',
                activityTimeFrame: '60',
                artifactLatestVersions: '3',
                policyEvaluationStage: 'build'
              }
            }
          }
        }
      },
      mockSend
    ];

    render(<EvaluationConfiguration parentMachine={overrideStateMachine} />);

    expect(screen.getByText('Activity Time Frame')).toBeInTheDocument();
    expect(screen.getByText('Latest Deployed Versions')).toBeInTheDocument();
    expect(screen.getByText('Policy Evaluation Stage')).toBeInTheDocument();
  });

  it('disables override fields when inherit mode is selected', () => {
    render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    const activityTimeFrameSelect = screen.getByLabelText('Activity Time Frame');
    const artifactLatestVersionsSelect = screen.getByLabelText('Latest Deployed Versions');
    const policyEvaluationStageSelect = screen.getByLabelText('Policy Evaluation Stage');

    expect(activityTimeFrameSelect).toBeDisabled();
    expect(artifactLatestVersionsSelect).toBeDisabled();
    expect(policyEvaluationStageSelect).toBeDisabled();
  });

  it('shows global settings values in inherit mode', async () => {
    render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    // Wait for global settings to be fetched
    await waitFor(() => {
      expect(Axios.get).toHaveBeenCalledWith('/service/rest/v1/evaluation/settings');
    });

    const activityTimeFrameSelect = screen.getByLabelText('Activity Time Frame');
    const artifactLatestVersionsSelect = screen.getByLabelText('Latest Deployed Versions');
    const policyEvaluationStageSelect = screen.getByLabelText('Policy Evaluation Stage');

    await waitFor(() => {
      expect(activityTimeFrameSelect).toHaveValue('30');
      expect(artifactLatestVersionsSelect).toHaveValue('3');
      expect(policyEvaluationStageSelect).toHaveValue('stage-release');
    });
  });

  it('calls sendParent when mode is changed', () => {
    render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    const overrideRadio = screen.getByLabelText('Override');
    userEvent.click(overrideRadio);

    expect(mockSend).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'attributes.evaluation',
      value: expect.objectContaining({
        mode: 'OVERRIDE'
      })
    });
  });

  it('initializes override fields with default values when switching to override mode', () => {
    render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    const overrideRadio = screen.getByLabelText('Override');
    userEvent.click(overrideRadio);

    expect(mockSend).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'attributes.evaluation',
      value: {
        mode: 'OVERRIDE',
        activityTimeFrame: '30',
        artifactLatestVersions: '1',
        policyEvaluationStage: 'build'
      }
    });
  });

  it('clears override fields when switching to disable mode', () => {
    const overrideStateMachine = [
      {
        context: {
          data: {
            attributes: {
              evaluation: {
                mode: 'OVERRIDE',
                activityTimeFrame: '60',
                artifactLatestVersions: '3',
                policyEvaluationStage: 'build'
              }
            }
          }
        }
      },
      mockSend
    ];

    render(<EvaluationConfiguration parentMachine={overrideStateMachine} />);

    const disableRadio = screen.getByLabelText('Disable Evaluation');
    userEvent.click(disableRadio);

    expect(mockSend).toHaveBeenCalledWith({
      type: 'UPDATE',
      name: 'attributes.evaluation',
      value: {
        mode: 'DISABLE'
      }
    });
  });

  it('renders nothing when feature flag is explicitly disabled', () => {
    window.NX.State.getValue.mockReturnValue(false);

    const {container} = render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    expect(container).toBeEmptyDOMElement();
  });

  it('loads persisted override values correctly', () => {
    const overrideWithValuesStateMachine = [
      {
        matches: jest.fn().mockReturnValue(false),
        context: {
          data: {
            attributes: {
              evaluation: {
                mode: 'OVERRIDE',
                activityTimeFrame: '60',
                artifactLatestVersions: '3',
                policyEvaluationStage: 'release'
              }
            }
          }
        }
      },
      mockSend
    ];

    render(<EvaluationConfiguration parentMachine={overrideWithValuesStateMachine} />);

    const activityTimeFrameSelect = screen.getByLabelText('Activity Time Frame');
    const artifactLatestVersionsSelect = screen.getByLabelText('Latest Deployed Versions');
    const policyEvaluationStageSelect = screen.getByLabelText('Policy Evaluation Stage');

    expect(activityTimeFrameSelect).toHaveValue('60');
    expect(artifactLatestVersionsSelect).toHaveValue('3');
    expect(policyEvaluationStageSelect).toHaveValue('release');
  });

  it('fetches global settings on mount', async () => {
    render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    await waitFor(() => {
      expect(Axios.get).toHaveBeenCalledWith('/service/rest/v1/evaluation/settings');
    });
  });

  it('normalizes policyEvaluationStage format from API (STAGE_RELEASE to stage-release)', async () => {
    Axios.get.mockResolvedValue({
      data: {
        activityTimeFrame: 30,
        artifactLatestVersions: 3,
        policyEvaluationStage: 'STAGE_RELEASE'
      }
    });

    render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    await waitFor(() => {
      const policyEvaluationStageSelect = screen.getByLabelText('Policy Evaluation Stage');
      expect(policyEvaluationStageSelect).toHaveValue('stage-release');
    });
  });

  it('uses default values when global settings API fails', async () => {
    Axios.get.mockRejectedValue(new Error('API Error'));

    render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    await waitFor(() => {
      const activityTimeFrameSelect = screen.getByLabelText('Activity Time Frame');
      const artifactLatestVersionsSelect = screen.getByLabelText('Latest Deployed Versions');
      const policyEvaluationStageSelect = screen.getByLabelText('Policy Evaluation Stage');

      expect(activityTimeFrameSelect).toHaveValue('30');
      expect(artifactLatestVersionsSelect).toHaveValue('1');
      expect(policyEvaluationStageSelect).toHaveValue('build');
    });
  });

  it('uses global settings when switching from OVERRIDE to INHERIT', async () => {
    const overrideStateMachine = [
      {
        context: {
          data: {
            attributes: {
              evaluation: {
                mode: 'OVERRIDE',
                activityTimeFrame: '60',
                artifactLatestVersions: '5',
                policyEvaluationStage: 'release'
              }
            }
          }
        }
      },
      mockSend
    ];

    render(<EvaluationConfiguration parentMachine={overrideStateMachine} />);

    await waitFor(() => {
      expect(Axios.get).toHaveBeenCalled();
    });

    const inheritRadio = screen.getByLabelText('Inherit from global evaluation settings');
    userEvent.click(inheritRadio);

    await waitFor(() => {
      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'attributes.evaluation',
        value: {
          mode: 'INHERIT',
          activityTimeFrame: '30',
          artifactLatestVersions: '3',
          policyEvaluationStage: 'stage-release'
        }
      });
    });
  });

  it('renders nothing when feature flag is undefined (strict check)', () => {
    window.NX.State.getValue.mockReturnValue(undefined);

    const {container} = render(<EvaluationConfiguration parentMachine={mockParentMachine} />);

    expect(container).toBeEmptyDOMElement();
  });
});
