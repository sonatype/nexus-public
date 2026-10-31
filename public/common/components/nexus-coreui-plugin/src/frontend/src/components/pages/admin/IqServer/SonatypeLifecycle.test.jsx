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
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import Axios from 'axios';

import SonatypeLifecycle from './SonatypeLifecycle';
import UIStrings from '../../../../constants/UIStrings';

const {SONATYPE_LIFECYCLE, IQ_SERVER} = UIStrings;

const mockRouterGo = jest.fn();

jest.mock('axios');

jest.mock('@xstate/react', () => ({
  useMachine: jest.fn(() => [
    {
      matches: jest.fn(() => false),
      context: {
        data: null
      }
    },
    jest.fn()
  ])
}));

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      useUser: jest.fn(() => ({name: 'test-user'})),
    }
  }
});

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {
      go: mockRouterGo
    }
  })
}));

describe('SonatypeLifecycle', () => {
  beforeEach(() => {
    mockRouterGo.mockClear();
    Axios.get.mockResolvedValue({data: null});

    // Reset ExtJS.useUser to return a valid user
    ExtJS.useUser.mockReturnValue({name: 'test-user'});

    // Default machine state - loaded with no data
    useMachine.mockReturnValue([
      {
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          data: null
        }
      },
      jest.fn()
    ]);
  });

  it('renders the breadcrumb navigation', () => {
    render(<SonatypeLifecycle/>);

    expect(screen.getByText(IQ_SERVER.MENU.text)).toBeInTheDocument();
    expect(screen.getByText('/')).toBeInTheDocument();
    const lifecycleTexts = screen.getAllByText(SONATYPE_LIFECYCLE.MENU.text);
    expect(lifecycleTexts.length).toBeGreaterThan(0);
  });

  it('navigates to IQ Server Connected page when breadcrumb link is clicked', async () => {
    render(<SonatypeLifecycle/>);

    const iqServerLink = screen.getByText(IQ_SERVER.MENU.text);
    await userEvent.click(iqServerLink);

    expect(mockRouterGo).toHaveBeenCalledWith('admin.iqconnected');
  });

  it('renders the lifecycle icon', () => {
    render(<SonatypeLifecycle/>);

    const lifecycleIcon = screen.getByAltText('Lifecycle');
    expect(lifecycleIcon).toBeInTheDocument();
    expect(lifecycleIcon).toHaveAttribute('src');
  });

  it('renders the page title with lifecycle icon side by side', () => {
    render(<SonatypeLifecycle/>);

    const pageTitle = screen.getAllByText(SONATYPE_LIFECYCLE.MENU.text);
    expect(pageTitle.length).toBeGreaterThan(0);

    // Check that the title and icon are in the same container
    const lifecycleTitle = document.querySelector('.lifecycle-title');
    expect(lifecycleTitle).toBeInTheDocument();
  });

  it('renders the hosted repositories evaluation section with NxH2', () => {
    const {container} = render(<SonatypeLifecycle/>);

    const h2Element = container.querySelector('.nx-h2');
    expect(h2Element).toHaveTextContent(SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.title);
    expect(screen.getByText(SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.description)).toBeInTheDocument();
  });

  it('renders the global evaluation settings card using NxTile', async () => {
    const {container} = render(<SonatypeLifecycle/>);

    const h3Element = container.querySelector('.nx-h3');
    expect(h3Element).toHaveTextContent(SONATYPE_LIFECYCLE.GLOBAL_EVALUATION_SETTINGS.title);

    await waitFor(() => {
      expect(screen.getByText(SONATYPE_LIFECYCLE.GLOBAL_EVALUATION_SETTINGS.description)).toBeInTheDocument();
    });

    // Check that NxTile component is rendered
    const tileCard = document.querySelector('.lifecycle-card');
    expect(tileCard).toBeInTheDocument();
  });

  it('renders the card arrow element', () => {
    render(<SonatypeLifecycle/>);

    const cardArrow = document.querySelector('.card-arrow');
    expect(cardArrow).toBeInTheDocument();
  });

  it('uses NxH1 for page title', () => {
    const {container} = render(<SonatypeLifecycle/>);

    const h1Element = container.querySelector('.nx-h1');
    expect(h1Element).toBeInTheDocument();
    expect(h1Element).toHaveTextContent(SONATYPE_LIFECYCLE.MENU.text);
  });

  it('uses NxH2 for section title', () => {
    const {container} = render(<SonatypeLifecycle/>);

    const h2Element = container.querySelector('.nx-h2');
    expect(h2Element).toBeInTheDocument();
    expect(h2Element).toHaveTextContent(SONATYPE_LIFECYCLE.HOSTED_REPOSITORIES_EVALUATION.title);
  });

  it('uses NxH3 for card title', () => {
    const {container} = render(<SonatypeLifecycle/>);

    const h3Element = container.querySelector('.nx-h3');
    expect(h3Element).toBeInTheDocument();
    expect(h3Element).toHaveTextContent(SONATYPE_LIFECYCLE.GLOBAL_EVALUATION_SETTINGS.title);
  });

  it('uses NxP for descriptions', () => {
    const {container} = render(<SonatypeLifecycle/>);

    const pElements = container.querySelectorAll('.nx-p');
    expect(pElements.length).toBeGreaterThanOrEqual(2);
  });

  it('renders the lifecycle icon and title in a flex container', () => {
    const {container} = render(<SonatypeLifecycle/>);

    const lifecycleTitle = container.querySelector('.lifecycle-title');
    expect(lifecycleTitle).toBeInTheDocument();

    const icon = lifecycleTitle.querySelector('.lifecycle-icon');
    const title = lifecycleTitle.querySelector('.nx-h1');

    expect(icon).toBeInTheDocument();
    expect(title).toBeInTheDocument();
  });

  it('returns null when user is not authenticated', () => {
    ExtJS.useUser.mockReturnValue(null);
    Axios.get.mockResolvedValue({data: null});

    const {container} = render(<SonatypeLifecycle/>);

    expect(container.firstChild).toBeNull();
  });

  it('displays static text when no global settings are configured', async () => {
    Axios.get.mockResolvedValue({data: null});

    render(<SonatypeLifecycle/>);

    await waitFor(() => {
      expect(screen.getByText(SONATYPE_LIFECYCLE.GLOBAL_EVALUATION_SETTINGS.description)).toBeInTheDocument();
    });
  });

  it('displays global settings data when configured', async () => {
    const mockSettings = {
      activityTimeFrame: 60,
      artifactLatestVersions: 10,
      policyEvaluationStage: 'BUILD',
      autoEnrollNewRepos: true,
      monitoredRepoCount: 5,
      totalRepoCount: 10
    };

    useMachine.mockReturnValue([
      {
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          data: mockSettings
        }
      },
      jest.fn()
    ]);

    render(<SonatypeLifecycle/>);

    await waitFor(() => {
      expect(screen.getByText(/Last 60 Days \| 10 Latest Deployed Versions/)).toBeInTheDocument();
      expect(screen.getByText(/Build/)).toBeInTheDocument();
      expect(screen.getByText(/Global Evaluation: 5\/10/)).toBeInTheDocument();
      expect(screen.getByText(/Custom Evaluation: N\/A/)).toBeInTheDocument();
    });
  });

  it('displays both Activity Time Frame and Latest Deployed Versions when artifactLatestVersions is set', async () => {
    const mockSettings = {
      activityTimeFrame: 60,
      artifactLatestVersions: 10,
      policyEvaluationStage: 'BUILD',
      autoEnrollNewRepos: true,
      monitoredRepoCount: 5,
      totalRepoCount: 10
    };

    useMachine.mockReturnValue([
      {
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          data: mockSettings
        }
      },
      jest.fn()
    ]);

    render(<SonatypeLifecycle/>);

    await waitFor(() => {
      // Both depth values are saved together since CLM-41306 — the summary surfaces both.
      expect(screen.getByText(/Last 60 Days \| 10 Latest Deployed Versions/)).toBeInTheDocument();
    });
  });

  it('displays global count excluding custom repositories when numberOfCustomRepositories is provided', async () => {
    const mockSettings = {
      activityTimeFrame: 60,
      artifactLatestVersions: 10,
      policyEvaluationStage: 'BUILD',
      autoEnrollNewRepos: true,
      monitoredRepoCount: 5,
      numberOfCustomRepositories: 2,
      totalRepoCount: 10
    };

    useMachine.mockReturnValue([
      {
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          data: mockSettings
        }
      },
      jest.fn()
    ]);

    render(<SonatypeLifecycle/>);

    await waitFor(() => {
      expect(screen.getByText(/Global Evaluation: 3\/10/)).toBeInTheDocument();
      expect(screen.getByText(/Custom Evaluation: 2/)).toBeInTheDocument();
    });
  });

  it('displays 0 global count when monitoredRepoCount is less than numberOfCustomRepositories', async () => {
    const mockSettings = {
      activityTimeFrame: 30,
      artifactLatestVersions: 1,
      policyEvaluationStage: 'RELEASE',
      autoEnrollNewRepos: false,
      monitoredRepoCount: 0,
      numberOfCustomRepositories: 1,
      totalRepoCount: 12
    };

    useMachine.mockReturnValue([
      {
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          data: mockSettings
        }
      },
      jest.fn()
    ]);

    render(<SonatypeLifecycle/>);

    await waitFor(() => {
      expect(screen.getByText(/Global Evaluation: 0\/12/)).toBeInTheDocument();
      expect(screen.getByText(/Custom Evaluation: 1/)).toBeInTheDocument();
    });
  });

  it('omits the depth section entirely when neither activityTimeFrame nor artifactLatestVersions is set', async () => {
    // Corrupt/legacy data path: both depth values absent. The summary should
    // skip the depth section completely instead of emitting a stray leading " | ".
    const mockSettings = {
      policyEvaluationStage: 'RELEASE',
      autoEnrollNewRepos: false,
      monitoredRepoCount: 3,
      totalRepoCount: 9
    };

    useMachine.mockReturnValue([
      {
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          data: mockSettings
        }
      },
      jest.fn()
    ]);

    render(<SonatypeLifecycle/>);

    await waitFor(() => {
      const summary = screen.getByText(/Global Evaluation: 3\/9/);
      // No leading " | " before the stage label
      expect(summary.textContent).toMatch(/^Release \| Global Evaluation: 3\/9/);
      expect(summary.textContent).not.toMatch(/^\s*\|/);
    });
  });

  it('omits the Latest Deployed Versions bullet when artifactLatestVersions is 0', async () => {
    // 0 is the semantic equivalent of "no version-count filter configured" —
    // the tile summarises what filters are ACTIVE on scope, so an unset filter
    // must not appear as a bullet. This test locks in that intent so a future
    // change to `> 0` (e.g., swapping to `!= null`) is caught immediately.
    const mockSettings = {
      activityTimeFrame: 60,
      artifactLatestVersions: 0,
      policyEvaluationStage: 'RELEASE',
      autoEnrollNewRepos: false,
      monitoredRepoCount: 3,
      totalRepoCount: 9
    };

    useMachine.mockReturnValue([
      {
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          data: mockSettings
        }
      },
      jest.fn()
    ]);

    render(<SonatypeLifecycle/>);

    await waitFor(() => {
      // ATF bullet renders normally
      expect(screen.getByText(/Last 60 Days/)).toBeInTheDocument();
      // LDV bullet must be absent — no trailing "| 0 Latest Deployed Versions"
      expect(screen.queryByText(/Latest Deployed Versions/)).not.toBeInTheDocument();
      expect(screen.queryByText(/\| 0 /)).not.toBeInTheDocument();
    });
  });

  it('displays static text when API call fails', async () => {
    useMachine.mockReturnValue([
      {
        matches: jest.fn((state) => state === 'loaded'),
        context: {
          data: null
        }
      },
      jest.fn()
    ]);

    render(<SonatypeLifecycle/>);

    await waitFor(() => {
      expect(screen.getByText(SONATYPE_LIFECYCLE.GLOBAL_EVALUATION_SETTINGS.description)).toBeInTheDocument();
    });
  });

  it('displays loading text while fetching settings', () => {
    useMachine.mockReturnValue([
      {
        matches: jest.fn((state) => state === 'loading'),
        context: {
          data: null
        }
      },
      jest.fn()
    ]);

    render(<SonatypeLifecycle/>);

    expect(screen.getByText(UIStrings.LOADING)).toBeInTheDocument();
  });
});
