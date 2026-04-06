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
import { render, screen, waitFor } from '@testing-library/react';
import { DirectoryPage } from './DirectoryPage';
import { RouteNames } from '../../../constants/RouteNames';
import * as NavigationUtils from '../../../interface/NavigationUtils';

const mockStateRegistry = {
  get: jest.fn()
};

const mockRouter = {
  stateService: {
    go: jest.fn()
  },
  stateRegistry: mockStateRegistry
};

const mockState = {
  name: 'admin.system'
};

jest.mock('@uirouter/react', () => ({
  ...jest.requireActual('@uirouter/react'),
  useRouter: () => mockRouter,
  useCurrentStateAndParams: () => ({ state: mockState }),
  UIView: () => <div>UIView</div>
}));

jest.spyOn(NavigationUtils, 'isVisible');

describe('DirectoryPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockState.name = 'admin.system';
    NavigationUtils.isVisible.mockReturnValue(true);
  });

  it('renders page with title and description when there are visible child routes', () => {
    mockStateRegistry.get.mockReturnValue([
      {
        name: 'admin.system.api',
        data: { visibilityRequirements: {} }
      },
      {
        name: 'admin.system.tasks',
        data: { visibilityRequirements: {} }
      }
    ]);

    render(
      <DirectoryPage
        routeName="admin.system"
        text="System"
        description="System configuration"
      >
        <div>Content</div>
      </DirectoryPage>
    );

    expect(screen.getByText('System')).toBeInTheDocument();
    expect(screen.getByText('System configuration')).toBeInTheDocument();
    expect(screen.getByText('Content')).toBeInTheDocument();
    expect(mockRouter.stateService.go).not.toHaveBeenCalled();
  });

  it('renders UIView when route does not match', () => {
    mockState.name = 'admin.security';
    mockStateRegistry.get.mockReturnValue([]);

    render(
      <DirectoryPage
        routeName="admin.system"
        text="System"
        description="System configuration"
      >
        <div>Content</div>
      </DirectoryPage>
    );

    expect(screen.getByText('UIView')).toBeInTheDocument();
    expect(screen.queryByText('System')).not.toBeInTheDocument();
  });

  it('redirects to 404 when no child routes are visible', async () => {
    mockStateRegistry.get.mockReturnValue([
      {
        name: 'admin.system.api',
        data: { visibilityRequirements: {} }
      }
    ]);
    NavigationUtils.isVisible.mockReturnValue(false);

    render(
      <DirectoryPage
        routeName="admin.system"
        text="System"
        description="System configuration"
      >
        <div>Content</div>
      </DirectoryPage>
    );

    await waitFor(() => {
      expect(mockRouter.stateService.go).toHaveBeenCalledWith(RouteNames.MISSING_ROUTE);
    });
  });

  it('does not redirect when at least one child route is visible', async () => {
    mockStateRegistry.get.mockReturnValue([
      {
        name: 'admin.system.api',
        data: { visibilityRequirements: { permissions: ['permission1'] } }
      },
      {
        name: 'admin.system.tasks',
        data: { visibilityRequirements: { permissions: ['permission2'] } }
      }
    ]);
    NavigationUtils.isVisible
      .mockReturnValueOnce(false)
      .mockReturnValueOnce(true);

    render(
      <DirectoryPage
        routeName="admin.system"
        text="System"
        description="System configuration"
      >
        <div>Content</div>
      </DirectoryPage>
    );

    await waitFor(() => {
      expect(mockRouter.stateService.go).not.toHaveBeenCalled();
    });
  });

  it('excludes routes with ignoreForMenuVisibilityCheck from visibility check', async () => {
    mockStateRegistry.get.mockReturnValue([
      {
        name: 'admin.system',
        data: {
          visibilityRequirements: {
            ignoreForMenuVisibilityCheck: true
          }
        }
      },
      {
        name: 'admin.system.api',
        data: { visibilityRequirements: {} }
      }
    ]);
    NavigationUtils.isVisible.mockReturnValue(false);

    render(
      <DirectoryPage
        routeName="admin.system"
        text="System"
        description="System configuration"
      >
        <div>Content</div>
      </DirectoryPage>
    );

    await waitFor(() => {
      expect(mockRouter.stateService.go).toHaveBeenCalledWith(RouteNames.MISSING_ROUTE);
    });

    expect(NavigationUtils.isVisible).toHaveBeenCalledTimes(1);
  });
});
