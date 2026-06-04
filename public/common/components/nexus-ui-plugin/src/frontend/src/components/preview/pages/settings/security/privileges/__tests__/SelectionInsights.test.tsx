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
import { Theme } from '@radix-ui/themes';
import { SelectionInsights } from '../SelectionInsights';
// @ts-ignore
import { useContentSelectorsApi } from '../../../repository/selectors/useContentSelectorsApi';
// @ts-ignore
import { useRepositoryTree } from '../../../repository/repositories/useRepositoryTree';

// Mock dependencies
jest.mock('../../../repository/selectors/useContentSelectorsApi');
jest.mock('../../../repository/repositories/useRepositoryTree');
jest.mock('../../../repository/repositories/components/FormatIcon', () => ({
  FormatIcon: () => <div data-testid="format-icon" />
}));
jest.mock('../../../repository/selectors/ContentSelectorPreview', () => ({
  ContentSelectorPreview: () => <div data-testid="content-selector-preview" />
}));
jest.mock('../../../../browse/browse.api', () => ({
  fetchBrowseNodes: jest.fn().mockResolvedValue([]),
}));
jest.mock('../../../repository/selectors/cselValidator', () => ({
  interpretExpression: jest.fn().mockReturnValue({ success: true, text: 'Matches format maven2' }),
}));

const mockRepository = {
  name: 'maven-releases',
  format: 'maven2',
  type: 'hosted',
  status: { online: true }
};

const mockContentSelector = {
  name: 'test-selector',
  expression: 'format == "maven2"',
  description: 'Test selector description'
};

const renderWithTheme = (component: React.ReactNode) => {
  return render(<Theme>{component}</Theme>);
};

describe('SelectionInsights', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useContentSelectorsApi as jest.Mock).mockReturnValue({
      previewContentSelector: jest.fn().mockResolvedValue(['path/to/asset1', 'path/to/asset2'])
    });
    (useRepositoryTree as jest.Mock).mockReturnValue({
      tree: [{ id: 'maven-releases', name: 'maven-releases', type: 'hosted', format: 'maven2', status: 'online', online: true }],
      loading: false,
      expandedIds: new Set(),
      toggleExpand: jest.fn()
    });
  });

  it('should render placeholder when no format/repo selected', () => {
    renderWithTheme(<SelectionInsights />);
    expect(screen.getByText(/Select Format and Repository to preview content/i)).toBeInTheDocument();
  });

  it('should render repository context when repository is selected', () => {
    renderWithTheme(<SelectionInsights repository={mockRepository} />);
    expect(screen.getByText('LIVE CONTENT BROWSER')).toBeInTheDocument();
    // Use getAllByText since it appears in both the header card and the tree preview
    expect(screen.getAllByText('maven-releases').length).toBeGreaterThan(0);
    expect(screen.getByTestId('format-icon')).toBeInTheDocument();
  });

  it('should render selector logic when content selector is selected', () => {
    renderWithTheme(<SelectionInsights contentSelector={mockContentSelector} />);
    expect(screen.getByText('Selector Logic')).toBeInTheDocument();
    expect(screen.getByText('test-selector')).toBeInTheDocument();
    expect(screen.getByText('format == "maven2"')).toBeInTheDocument();
  });

  it('should call preview API and show match count', async () => {
    renderWithTheme(
      <SelectionInsights 
        repository={mockRepository} 
        contentSelector={mockContentSelector} 
      />
    );

    await waitFor(() => {
      expect(screen.getByText('2 matches')).toBeInTheDocument();
    });
  });

  it('should show "All Repositories" view', () => {
    renderWithTheme(
      <SelectionInsights 
        allRepositories={true} 
        selectedFormat="npm" 
      />
    );
    expect(screen.getByText('All Repositories')).toBeInTheDocument();
    expect(screen.getByText('NPM Format')).toBeInTheDocument();
  });

  it('Full Match List button has type=button to prevent form submit (03xd)', async () => {
    (useContentSelectorsApi as jest.Mock).mockReturnValue({
      previewContentSelector: jest.fn().mockResolvedValue(['/a.jar', '/b.jar'])
    });
    renderWithTheme(
      <SelectionInsights
        repository={mockRepository}
        contentSelector={mockContentSelector}
      />
    );
    await waitFor(() => expect(screen.getByText('2 matches')).toBeInTheDocument());
    const btn = screen.getByRole('button', { name: /full match list/i });
    // Radix Button type prop maps to native button type attribute
    expect(btn).toHaveAttribute('type', 'button');
  });

  it('renders match count badge in header when flyout is open', async () => {
    (useContentSelectorsApi as jest.Mock).mockReturnValue({
      previewContentSelector: jest.fn().mockResolvedValue([
        '/org/apache/a-1.pom',
        '/org/apache/b-2.jar',
        '/org/apache/c-3.jar',
      ])
    });
    renderWithTheme(
      <SelectionInsights
        repository={mockRepository}
        contentSelector={mockContentSelector}
      />
    );
    await waitFor(() => expect(screen.getByText('3 matches')).toBeInTheDocument());
    screen.getByRole('button', { name: /full match list/i }).click();
    await waitFor(() => {
      expect(screen.getByTestId('match-count-badge')).toHaveTextContent('3 items');
    });
  });

  it('renders paths as sorted table rows in flyout', async () => {
    (useContentSelectorsApi as jest.Mock).mockReturnValue({
      previewContentSelector: jest.fn().mockResolvedValue([
        '/z/last.jar',
        '/a/first.jar',
        '/m/middle.jar',
      ])
    });
    renderWithTheme(
      <SelectionInsights
        repository={mockRepository}
        contentSelector={mockContentSelector}
      />
    );
    await waitFor(() => expect(screen.getByText('3 matches')).toBeInTheDocument());
    screen.getByRole('button', { name: /full match list/i }).click();
    await waitFor(() => {
      const rows = screen.getAllByTestId('match-path-row');
      const texts = rows.map((r) => r.textContent ?? '');
      expect(texts[0]).toContain('/a/first.jar');
      expect(texts[2]).toContain('/z/last.jar');
    });
  });

  it('flyout filter input filters path rows', async () => {
    (useContentSelectorsApi as jest.Mock).mockReturnValue({
      previewContentSelector: jest.fn().mockResolvedValue([
        '/org/alpha/v1.jar',
        '/org/beta/v2.jar',
        '/org/alpha/v3.pom',
      ])
    });
    const { getByLabelText } = renderWithTheme(
      <SelectionInsights
        repository={mockRepository}
        contentSelector={mockContentSelector}
      />
    );
    await waitFor(() => expect(screen.getByText('3 matches')).toBeInTheDocument());
    screen.getByRole('button', { name: /full match list/i }).click();
    await waitFor(() => expect(screen.getByLabelText('Filter matching paths')).toBeInTheDocument());
    const { fireEvent } = await import('@testing-library/react');
    fireEvent.change(screen.getByLabelText('Filter matching paths'), { target: { value: 'alpha' } });
    await waitFor(() => {
      const rows = screen.getAllByTestId('match-path-row');
      expect(rows).toHaveLength(2);
    });
  });

  it('flyout shows empty state when filter matches nothing', async () => {
    (useContentSelectorsApi as jest.Mock).mockReturnValue({
      previewContentSelector: jest.fn().mockResolvedValue(['/org/x/artifact.jar'])
    });
    renderWithTheme(
      <SelectionInsights
        repository={mockRepository}
        contentSelector={mockContentSelector}
      />
    );
    await waitFor(() => expect(screen.getByText('1 matches')).toBeInTheDocument());
    screen.getByRole('button', { name: /full match list/i }).click();
    await waitFor(() => expect(screen.getByLabelText('Filter matching paths')).toBeInTheDocument());
    const { fireEvent } = await import('@testing-library/react');
    fireEvent.change(screen.getByLabelText('Filter matching paths'), { target: { value: 'zzznomatch' } });
    await waitFor(() => {
      expect(screen.getByTestId('match-empty-state')).toBeInTheDocument();
    });
  });
});
