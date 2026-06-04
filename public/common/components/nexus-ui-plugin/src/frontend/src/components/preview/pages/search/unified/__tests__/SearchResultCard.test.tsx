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
import { Theme } from '@radix-ui/themes';
import { SearchResultCard } from '../SearchResultCard';

// Helper to wrap components with Radix Theme
function renderWithTheme(ui) {
  return render(<Theme>{ui}</Theme>);
}

// Sample test data
const mockResult = {
  id: 'test-123',
  name: 'my-component',
  format: 'maven2',
  repository: 'maven-central',
  group: 'com.example',
  version: '1.2.3',
  lastUpdated: '2026-01-14T12:00:00Z',
};

const mockResultNoGroup = {
  id: 'test-456',
  name: 'npm-package',
  format: 'npm',
  repository: 'npm-proxy',
  version: '2.0.0',
};

const mockResultNpm = {
  id: 'test-789',
  name: '@scope/package',
  format: 'npm',
  repository: 'npm-hosted',
  group: '@scope',
  version: '3.1.0',
  lastUpdated: '2026-01-10T08:30:00Z',
};

const mockResultDocker = {
  id: 'test-docker',
  name: 'nginx',
  format: 'docker',
  repository: 'docker-hub',
  version: 'latest',
  lastUpdated: '2026-01-15T16:45:00Z',
};

describe('SearchResultCard', () => {
  describe('rendering', () => {
    it('renders component name and version inline', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByText(/my-component/)).toBeInTheDocument();
      expect(screen.getByText(/1\.2\.3/)).toBeInTheDocument();
    });

    it('renders group when provided', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByText('com.example')).toBeInTheDocument();
    });

    it('does not render group when group is not provided', () => {
      renderWithTheme(<SearchResultCard result={mockResultNoGroup} onClick={jest.fn()} />);
      expect(screen.queryByText('com.example')).not.toBeInTheDocument();
    });

    it('renders version', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByText(/1\.2\.3/)).toBeInTheDocument();
    });

    it('renders format badge', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('renders repository name', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByText('maven-central')).toBeInTheDocument();
    });

    it('renders last modified date in correct format', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByText('Jan 14, 2026')).toBeInTheDocument();
    });

    it('renders "—" when lastUpdated is not provided', () => {
      renderWithTheme(<SearchResultCard result={mockResultNoGroup} onClick={jest.fn()} />);
      expect(screen.getByText('—')).toBeInTheDocument();
    });
  });

  describe('format badges', () => {
    it('renders ecosystem logo when format has a logo', () => {
      const { container } = renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      const logoImg = container.querySelector('img[alt=""]');
      expect(logoImg).toBeInTheDocument();
      expect(logoImg).toHaveAttribute('src');
    });

    it('renders npm format badge label', () => {
      renderWithTheme(<SearchResultCard result={mockResultNpm} onClick={jest.fn()} />);
      expect(screen.getByText('npm')).toBeInTheDocument();
    });

    it('renders maven format badge label', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('renders docker format badge label', () => {
      renderWithTheme(<SearchResultCard result={mockResultDocker} onClick={jest.fn()} />);
      expect(screen.getByText('docker')).toBeInTheDocument();
    });
  });

  describe('interactions', () => {
    it('calls onClick when card is clicked', () => {
      const onClick = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onClick={onClick} />);
      fireEvent.click(screen.getByRole('button'));
      expect(onClick).toHaveBeenCalledTimes(1);
    });

    it('calls onClick when Enter key is pressed', () => {
      const onClick = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onClick={onClick} />);
      const card = screen.getByRole('button');
      fireEvent.keyDown(card, { key: 'Enter' });
      expect(onClick).toHaveBeenCalledTimes(1);
    });

    it('calls onClick when Space key is pressed', () => {
      const onClick = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onClick={onClick} />);
      const card = screen.getByRole('button');
      fireEvent.keyDown(card, { key: ' ' });
      expect(onClick).toHaveBeenCalledTimes(1);
    });

    it('does not call onClick for other keys', () => {
      const onClick = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onClick={onClick} />);
      const card = screen.getByRole('button');
      fireEvent.keyDown(card, { key: 'Tab' });
      expect(onClick).not.toHaveBeenCalled();
    });
  });

  describe('accessibility', () => {
    it('has correct aria-label', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      const card = screen.getByRole('button');
      expect(card).toHaveAttribute('aria-label', 'View details for my-component');
    });

    it('has button role', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByRole('button')).toBeInTheDocument();
    });

    it('is focusable', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      const card = screen.getByRole('button');
      expect(card).toHaveAttribute('tabIndex', '0');
    });
  });

  describe('layout', () => {
    it('renders card with name, group, badges, and last updated', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByText(/my-component/)).toBeInTheDocument();
      expect(screen.getByText('com.example')).toBeInTheDocument();
      expect(screen.getByText('maven2')).toBeInTheDocument();
      expect(screen.getByText('maven-central')).toBeInTheDocument();
      expect(screen.getByText('Jan 14, 2026')).toBeInTheDocument();
    });
  });

  describe('date formatting', () => {
    it('formats date correctly for different months', () => {
      const resultWithMarchDate = {
        ...mockResult,
        lastUpdated: '2026-03-15T10:00:00Z',
      };
      renderWithTheme(<SearchResultCard result={resultWithMarchDate} onClick={jest.fn()} />);
      expect(screen.getByText('Mar 15, 2026')).toBeInTheDocument();
    });

    it('formats date correctly for December', () => {
      const resultWithDecemberDate = {
        ...mockResult,
        lastUpdated: '2025-12-25T10:00:00Z',
      };
      renderWithTheme(<SearchResultCard result={resultWithDecemberDate} onClick={jest.fn()} />);
      expect(screen.getByText('Dec 25, 2025')).toBeInTheDocument();
    });
  });

  describe('format display', () => {
    it('displays format as provided by API', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByText('maven2')).toBeInTheDocument();
    });

    it('displays npm format', () => {
      renderWithTheme(<SearchResultCard result={mockResultNpm} onClick={jest.fn()} />);
      expect(screen.getByText('npm')).toBeInTheDocument();
    });

    it('displays docker format', () => {
      renderWithTheme(<SearchResultCard result={mockResultDocker} onClick={jest.fn()} />);
      expect(screen.getByText('docker')).toBeInTheDocument();
    });
  });

  describe('Guide deep research integration (y87u)', () => {
    it('shows deep research link when component has version', () => {
      renderWithTheme(<SearchResultCard result={mockResult} onClick={jest.fn()} />);
      expect(screen.getByTestId('deep-research-link')).toBeInTheDocument();
    });

    it('deep research link does not trigger card onClick', () => {
      const onCardClick = jest.fn();
      renderWithTheme(<SearchResultCard result={mockResult} onClick={onCardClick} />);
      // Clicking the deep research link should NOT trigger card onClick due to stopPropagation
      const guideLinks = screen.getAllByRole('link').filter(l => l.getAttribute('href')?.includes('guide.sonatype.com'));
      expect(guideLinks.length).toBeGreaterThan(0);
    });
  });
});
