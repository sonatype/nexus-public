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
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Cloud } from 'lucide-react';
import { SettingsFormSection } from '../SettingsFormSection';

describe('SettingsFormSection', () => {
  const defaultProps = {
    children: <div data-testid="section-content">Content</div>,
  };

  describe('rendering', () => {
    it('renders children content', () => {
      render(<SettingsFormSection {...defaultProps} />);
      expect(screen.getByTestId('section-content')).toBeInTheDocument();
    });

    it('renders with title', () => {
      render(
        <SettingsFormSection {...defaultProps} title="Section Title" />
      );
      expect(screen.getByRole('heading', { name: 'Section Title' })).toBeInTheDocument();
    });

    it('renders without title', () => {
      render(<SettingsFormSection {...defaultProps} />);
      expect(screen.queryByRole('heading')).not.toBeInTheDocument();
    });

    it('renders with description', () => {
      render(
        <SettingsFormSection
          {...defaultProps}
          title="Title"
          description="This is a description"
        />
      );
      expect(screen.getByText('This is a description')).toBeInTheDocument();
    });

    it('renders with icon', () => {
      const { container } = render(
        <SettingsFormSection
          {...defaultProps}
          title="Title"
          icon={<Cloud data-testid="section-icon" />}
        />
      );
      expect(screen.getByTestId('section-icon')).toBeInTheDocument();
    });

    it('applies custom className', () => {
      const { container } = render(
        <SettingsFormSection {...defaultProps} className="custom-class" />
      );
      expect(container.querySelector('.custom-class')).toBeInTheDocument();
    });
  });

  describe('collapsible behavior', () => {
    it('is not collapsible by default', () => {
      render(
        <SettingsFormSection {...defaultProps} title="Title" />
      );
      // Content should always be visible
      expect(screen.getByTestId('section-content')).toBeInTheDocument();
      // Header should not have button role
      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('shows collapse chevron when collapsible', () => {
      render(
        <SettingsFormSection {...defaultProps} title="Title" collapsible />
      );
      // Should have the chevron indicator
      expect(screen.getByText('▾')).toBeInTheDocument();
    });

    it('header has button role when collapsible', () => {
      render(
        <SettingsFormSection {...defaultProps} title="Title" collapsible />
      );
      expect(screen.getByRole('button')).toBeInTheDocument();
    });

    it('starts expanded by default when collapsible', () => {
      render(
        <SettingsFormSection {...defaultProps} title="Title" collapsible />
      );
      expect(screen.getByTestId('section-content')).toBeInTheDocument();
    });

    it('starts collapsed when defaultCollapsed is true', () => {
      render(
        <SettingsFormSection
          {...defaultProps}
          title="Title"
          collapsible
          defaultCollapsed
        />
      );
      expect(screen.queryByTestId('section-content')).not.toBeInTheDocument();
    });

    it('toggles content visibility on click', () => {
      render(
        <SettingsFormSection {...defaultProps} title="Title" collapsible />
      );

      // Initially visible
      expect(screen.getByTestId('section-content')).toBeInTheDocument();

      // Click to collapse
      fireEvent.click(screen.getByRole('button'));
      expect(screen.queryByTestId('section-content')).not.toBeInTheDocument();

      // Click to expand
      fireEvent.click(screen.getByRole('button'));
      expect(screen.getByTestId('section-content')).toBeInTheDocument();
    });

    it('does not toggle when not collapsible', () => {
      const { container } = render(
        <SettingsFormSection {...defaultProps} title="Title" />
      );

      const header = container.querySelector('.settings-section__header');
      if (header) {
        fireEvent.click(header);
      }

      // Content should still be visible
      expect(screen.getByTestId('section-content')).toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('header is focusable when collapsible', () => {
      render(
        <SettingsFormSection {...defaultProps} title="Title" collapsible />
      );
      const header = screen.getByRole('button');
      expect(header).toHaveAttribute('tabindex', '0');
    });

    it('header is not focusable when not collapsible', () => {
      const { container } = render(
        <SettingsFormSection {...defaultProps} title="Title" />
      );
      const header = container.querySelector('.settings-section__header');
      expect(header).not.toHaveAttribute('tabindex');
    });

    it('can be toggled with Enter key', () => {
      render(
        <SettingsFormSection {...defaultProps} title="Title" collapsible />
      );

      const header = screen.getByRole('button');
      fireEvent.keyDown(header, { key: 'Enter' });

      // Should collapse
      expect(screen.queryByTestId('section-content')).not.toBeInTheDocument();
    });

    it('uses heading for title', () => {
      render(
        <SettingsFormSection {...defaultProps} title="Section Title" />
      );
      expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Section Title');
    });
  });

  describe('nested content', () => {
    it('renders form elements as children', () => {
      render(
        <SettingsFormSection title="Settings">
          <input type="text" data-testid="input-1" />
          <input type="text" data-testid="input-2" />
        </SettingsFormSection>
      );
      expect(screen.getByTestId('input-1')).toBeInTheDocument();
      expect(screen.getByTestId('input-2')).toBeInTheDocument();
    });

    it('renders nested sections', () => {
      render(
        <SettingsFormSection title="Parent">
          <SettingsFormSection title="Child">
            <div data-testid="nested-content">Nested</div>
          </SettingsFormSection>
        </SettingsFormSection>
      );
      expect(screen.getByRole('heading', { name: 'Parent' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Child' })).toBeInTheDocument();
      expect(screen.getByTestId('nested-content')).toBeInTheDocument();
    });
  });
});


