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
import DirectoryListItem from './DirectoryListItem';
import {useSref, useRouter} from '@uirouter/react';
import {useIsVisible} from '../../../interface/NavigationUtils';

// Mock dependencies
jest.mock('@uirouter/react', () => ({
  useSref: jest.fn(),
  useRouter: jest.fn()
}));

jest.mock('@sonatype/react-shared-components', () => ({
  NxList: {
    LinkItem: ({children, href, className, ...props}) => (
      <a href={href} className={className} data-testid="link-item" {...props}>{children}</a>
    ),
    Text: ({children}) => <span data-testid="text">{children}</span>,
    Subtext: ({children}) => <span data-testid="subtext">{children}</span>
  }
}));

jest.mock('../../../interface/NavigationUtils', () => ({
  useIsVisible: jest.fn()
}));

describe('DirectoryListItem', () => {
  beforeEach(() => {
    jest.clearAllMocks();

    // Default mocks
    useSref.mockReturnValue({href: '/test/path'});
    useRouter.mockReturnValue({
      stateRegistry: {
        get: jest.fn().mockReturnValue({
          data: {
            visibilityRequirements: {}
          }
        })
      }
    });
    useIsVisible.mockReturnValue(true);
  });

  it('renders link item when visible', () => {
    render(
      <DirectoryListItem
        text="Test Item"
        description="Test description"
        routeName="test.route"
        params={{id: '123'}}
      />
    );

    expect(screen.getByTestId('link-item')).toBeInTheDocument();
    expect(screen.getByTestId('text')).toHaveTextContent('Test Item');
    expect(screen.getByTestId('subtext')).toHaveTextContent('Test description');
    expect(screen.getByTestId('link-item')).toHaveAttribute('href', '/test/path');
  });

  it('does not render when not visible', () => {
    useIsVisible.mockReturnValue(false);

    const {container} = render(
      <DirectoryListItem
        text="Test Item"
        description="Test description"
        routeName="test.route"
      />
    );

    expect(container.firstChild).toBeNull();
  });

  it('applies custom className', () => {
    render(
      <DirectoryListItem
        text="Test Item"
        description="Test description"
        routeName="test.route"
        className="custom-class"
      />
    );

    const linkItem = screen.getByTestId('link-item');
    expect(linkItem).toHaveClass('nxrm-directory-list-item');
    expect(linkItem).toHaveClass('custom-class');
  });

  it('applies default className without custom className', () => {
    render(
      <DirectoryListItem
        text="Test Item"
        description="Test description"
        routeName="test.route"
      />
    );

    const linkItem = screen.getByTestId('link-item');
    expect(linkItem).toHaveClass('nxrm-directory-list-item');
  });

  it('passes additional props to LinkItem', () => {
    render(
      <DirectoryListItem
        text="Test Item"
        description="Test description"
        routeName="test.route"
        data-test="custom-prop"
        id="custom-id"
      />
    );

    const linkItem = screen.getByTestId('link-item');
    expect(linkItem).toHaveAttribute('data-test', 'custom-prop');
    expect(linkItem).toHaveAttribute('id', 'custom-id');
  });

  it('uses useSref with correct parameters', () => {
    const params = {id: '123', tab: 'settings'};

    render(
      <DirectoryListItem
        text="Test Item"
        description="Test description"
        routeName="test.route"
        params={params}
      />
    );

    expect(useSref).toHaveBeenCalledWith('test.route', params);
  });

  it('retrieves route state from router', () => {
    const mockGetState = jest.fn().mockReturnValue({
      data: {
        visibilityRequirements: {permission: 'admin'}
      }
    });

    useRouter.mockReturnValue({
      stateRegistry: {
        get: mockGetState
      }
    });

    render(
      <DirectoryListItem
        text="Test Item"
        description="Test description"
        routeName="test.route"
      />
    );

    expect(mockGetState).toHaveBeenCalledWith('test.route');
  });

  it('checks visibility with route data requirements', () => {
    const visibilityRequirements = {permission: 'admin'};

    useRouter.mockReturnValue({
      stateRegistry: {
        get: jest.fn().mockReturnValue({
          data: {visibilityRequirements}
        })
      }
    });

    render(
      <DirectoryListItem
        text="Test Item"
        description="Test description"
        routeName="test.route"
      />
    );

    expect(useIsVisible).toHaveBeenCalledWith(visibilityRequirements);
  });
});
