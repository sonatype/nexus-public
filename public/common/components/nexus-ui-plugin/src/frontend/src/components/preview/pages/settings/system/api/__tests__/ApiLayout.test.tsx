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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { ApiLayout } from '../ApiLayout';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('ApiLayout', () => {
  const leftContent = <div data-testid="left-content">Left Panel</div>;
  const rightContent = <div data-testid="right-content">Right Panel</div>;

  describe('Expanded State', () => {
    it('renders both panels when not collapsed', () => {
      renderWithTheme(
        <ApiLayout
          leftPanel={leftContent}
          rightPanel={rightContent}
          leftCollapsed={false}
          onToggleLeft={jest.fn()}
        />
      );
      expect(screen.getByTestId('api-layout-left')).toBeInTheDocument();
      expect(screen.getByTestId('api-layout-right')).toBeInTheDocument();
    });

    it('renders left panel content', () => {
      renderWithTheme(
        <ApiLayout
          leftPanel={leftContent}
          rightPanel={rightContent}
          leftCollapsed={false}
          onToggleLeft={jest.fn()}
        />
      );
      expect(screen.getByTestId('left-content')).toBeInTheDocument();
    });

    it('renders right panel content', () => {
      renderWithTheme(
        <ApiLayout
          leftPanel={leftContent}
          rightPanel={rightContent}
          leftCollapsed={false}
          onToggleLeft={jest.fn()}
        />
      );
      expect(screen.getByTestId('right-content')).toBeInTheDocument();
    });

    it('shows collapse button with correct label', () => {
      renderWithTheme(
        <ApiLayout
          leftPanel={leftContent}
          rightPanel={rightContent}
          leftCollapsed={false}
          onToggleLeft={jest.fn()}
        />
      );
      expect(screen.getByRole('button', { name: 'Collapse endpoint list' })).toBeInTheDocument();
    });
  });

  describe('Collapsed State', () => {
    it('hides the left panel when collapsed', () => {
      renderWithTheme(
        <ApiLayout
          leftPanel={leftContent}
          rightPanel={rightContent}
          leftCollapsed={true}
          onToggleLeft={jest.fn()}
        />
      );
      expect(screen.queryByTestId('api-layout-left')).not.toBeInTheDocument();
    });

    it('still renders the right panel when collapsed', () => {
      renderWithTheme(
        <ApiLayout
          leftPanel={leftContent}
          rightPanel={rightContent}
          leftCollapsed={true}
          onToggleLeft={jest.fn()}
        />
      );
      expect(screen.getByTestId('api-layout-right')).toBeInTheDocument();
    });

    it('shows expand button with correct label', () => {
      renderWithTheme(
        <ApiLayout
          leftPanel={leftContent}
          rightPanel={rightContent}
          leftCollapsed={true}
          onToggleLeft={jest.fn()}
        />
      );
      expect(screen.getByRole('button', { name: 'Expand endpoint list' })).toBeInTheDocument();
    });
  });

  describe('Toggle Interaction', () => {
    it('calls onToggleLeft when toggle button is clicked', async () => {
      const onToggle = jest.fn();
      renderWithTheme(
        <ApiLayout
          leftPanel={leftContent}
          rightPanel={rightContent}
          leftCollapsed={false}
          onToggleLeft={onToggle}
        />
      );
      await userEvent.click(screen.getByRole('button', { name: 'Collapse endpoint list' }));
      expect(onToggle).toHaveBeenCalledTimes(1);
    });

    it('calls onToggleLeft when expand button is clicked in collapsed state', async () => {
      const onToggle = jest.fn();
      renderWithTheme(
        <ApiLayout
          leftPanel={leftContent}
          rightPanel={rightContent}
          leftCollapsed={true}
          onToggleLeft={onToggle}
        />
      );
      await userEvent.click(screen.getByRole('button', { name: 'Expand endpoint list' }));
      expect(onToggle).toHaveBeenCalledTimes(1);
    });
  });
});
