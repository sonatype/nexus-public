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
import {DirectoryPage} from './DirectoryPage';
import {useCurrentStateAndParams} from '@uirouter/react';

// Mock dependencies
jest.mock('@uirouter/react', () => ({
  UIView: function MockUIView() {
    return <div data-testid="ui-view">UIView</div>;
  },
  useCurrentStateAndParams: jest.fn(),
  useRouter: jest.fn(() => ({
    stateService: {
      go: jest.fn()
    },
    stateRegistry: {
      get: jest.fn(() => [])
    }
  }))
}));

jest.mock('../../layout', () => ({
  Page: ({children, ...props}) => <div data-testid="page" {...props}>{children}</div>
}));

jest.mock('@sonatype/react-shared-components', () => {
  const Tile = ({children}) => <div data-testid="tile">{children}</div>;
  const TileContent = ({children}) => <div data-testid="tile-content">{children}</div>;
  TileContent.displayName = 'TileContent';
  Tile.Content = TileContent;

  return {
    NxH1: ({children}) => <h1>{children}</h1>,
    NxP: ({children}) => <p>{children}</p>,
    NxTile: Tile
  };
});

describe('DirectoryPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders page content when route matches', () => {
    useCurrentStateAndParams.mockReturnValue({
      state: {name: 'test.route'}
    });

    render(
      <DirectoryPage
        routeName="test.route"
        text="Test Page"
        description="Test description"
      >
        <div>Child content</div>
      </DirectoryPage>
    );

    expect(screen.getByText('Test Page')).toBeInTheDocument();
    expect(screen.getByText('Test description')).toBeInTheDocument();
    expect(screen.getByText('Child content')).toBeInTheDocument();
    expect(screen.getByTestId('page')).toBeInTheDocument();
    expect(screen.getByTestId('tile')).toBeInTheDocument();
  });

  it('renders UIView when route does not match', () => {
    useCurrentStateAndParams.mockReturnValue({
      state: {name: 'other.route'}
    });

    render(
      <DirectoryPage
        routeName="test.route"
        text="Test Page"
        description="Test description"
      >
        <div>Child content</div>
      </DirectoryPage>
    );

    expect(screen.getByTestId('ui-view')).toBeInTheDocument();
    expect(screen.queryByText('Test Page')).not.toBeInTheDocument();
    expect(screen.queryByText('Child content')).not.toBeInTheDocument();
  });

  it('passes additional attributes to Page component', () => {
    useCurrentStateAndParams.mockReturnValue({
      state: {name: 'test.route'}
    });

    render(
      <DirectoryPage
        routeName="test.route"
        text="Test Page"
        description="Test description"
        className="custom-class"
        data-test="custom-attr"
      >
        <div>Child content</div>
      </DirectoryPage>
    );

    const page = screen.getByTestId('page');
    expect(page).toHaveAttribute('class', 'custom-class');
    expect(page).toHaveAttribute('data-test', 'custom-attr');
  });

  it('renders without children', () => {
    useCurrentStateAndParams.mockReturnValue({
      state: {name: 'test.route'}
    });

    render(
      <DirectoryPage
        routeName="test.route"
        text="Test Page"
        description="Test description"
      />
    );

    expect(screen.getByText('Test Page')).toBeInTheDocument();
    expect(screen.getByText('Test description')).toBeInTheDocument();
    expect(screen.getByTestId('tile')).toBeInTheDocument();
  });

  it('renders with multiple children', () => {
    useCurrentStateAndParams.mockReturnValue({
      state: {name: 'test.route'}
    });

    render(
      <DirectoryPage
        routeName="test.route"
        text="Test Page"
        description="Test description"
      >
        <div>First child</div>
        <div>Second child</div>
      </DirectoryPage>
    );

    expect(screen.getByText('First child')).toBeInTheDocument();
    expect(screen.getByText('Second child')).toBeInTheDocument();
  });
});
