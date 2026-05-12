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

import { SystemInfoSection } from '../SystemInfoSection';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('SystemInfoSection', () => {
  const mockData = {
    version: '3.88.0-01',
    edition: 'PRO',
    status: 'Running',
    uptime: '5 days',
  };

  it('renders the section title', () => {
    render(
      <SystemInfoSection title="Nexus Status" data={mockData} />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Nexus Status')).toBeInTheDocument();
  });

  it('shows item count', () => {
    render(
      <SystemInfoSection title="Nexus Status" data={mockData} />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('(4 items)')).toBeInTheDocument();
  });

  it('is collapsed by default', () => {
    render(
      <SystemInfoSection title="Nexus Status" data={mockData} />,
      { wrapper: TestWrapper }
    );

    expect(screen.queryByText('version')).not.toBeInTheDocument();
  });

  it('expands when defaultOpen is true', () => {
    render(
      <SystemInfoSection title="Nexus Status" data={mockData} defaultOpen />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('version')).toBeInTheDocument();
    expect(screen.getByText('3.88.0-01')).toBeInTheDocument();
  });

  it('toggles open/closed on click', () => {
    render(
      <SystemInfoSection title="Nexus Status" data={mockData} />,
      { wrapper: TestWrapper }
    );

    // Initially closed
    expect(screen.queryByText('version')).not.toBeInTheDocument();

    // Click to open
    fireEvent.click(screen.getByText('Nexus Status'));
    expect(screen.getByText('version')).toBeInTheDocument();

    // Click to close
    fireEvent.click(screen.getByText('Nexus Status'));
    expect(screen.queryByText('version')).not.toBeInTheDocument();
  });

  it('toggles on keyboard Enter', () => {
    render(
      <SystemInfoSection title="Nexus Status" data={mockData} />,
      { wrapper: TestWrapper }
    );

    const header = screen.getByRole('button');
    fireEvent.keyDown(header, { key: 'Enter' });

    expect(screen.getByText('version')).toBeInTheDocument();
  });

  it('displays all properties when expanded', () => {
    render(
      <SystemInfoSection title="Nexus Status" data={mockData} defaultOpen />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('version')).toBeInTheDocument();
    expect(screen.getByText('edition')).toBeInTheDocument();
    expect(screen.getByText('status')).toBeInTheDocument();
    expect(screen.getByText('uptime')).toBeInTheDocument();
  });

  it('shows filter input for sections with more than 5 items', () => {
    const largeData = {
      prop1: 'value1',
      prop2: 'value2',
      prop3: 'value3',
      prop4: 'value4',
      prop5: 'value5',
      prop6: 'value6',
    };

    render(
      <SystemInfoSection title="Large Section" data={largeData} defaultOpen />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByPlaceholderText('Filter properties...')).toBeInTheDocument();
  });

  it('does not show filter input for sections with 5 or fewer items', () => {
    render(
      <SystemInfoSection title="Nexus Status" data={mockData} defaultOpen />,
      { wrapper: TestWrapper }
    );

    expect(screen.queryByPlaceholderText('Filter properties...')).not.toBeInTheDocument();
  });

  it('filters properties by key', () => {
    const largeData = {
      version: '3.88.0',
      edition: 'PRO',
      status: 'Running',
      uptime: '5 days',
      memory: '2048MB',
      diskSpace: '500GB',
    };

    render(
      <SystemInfoSection title="System" data={largeData} defaultOpen />,
      { wrapper: TestWrapper }
    );

    const filterInput = screen.getByPlaceholderText('Filter properties...');
    fireEvent.change(filterInput, { target: { value: 'version' } });

    expect(screen.getByText('version')).toBeInTheDocument();
    expect(screen.queryByText('edition')).not.toBeInTheDocument();
  });

  it('filters properties by value', () => {
    const largeData = {
      version: '3.88.0',
      edition: 'PRO',
      status: 'Running',
      uptime: '5 days',
      memory: '2048MB',
      diskSpace: '500GB',
    };

    render(
      <SystemInfoSection title="System" data={largeData} defaultOpen />,
      { wrapper: TestWrapper }
    );

    const filterInput = screen.getByPlaceholderText('Filter properties...');
    fireEvent.change(filterInput, { target: { value: 'PRO' } });

    expect(screen.getByText('edition')).toBeInTheDocument();
    expect(screen.queryByText('version')).not.toBeInTheDocument();
  });

  it('shows empty message when no matches', () => {
    const largeData = {
      version: '3.88.0',
      edition: 'PRO',
      status: 'Running',
      uptime: '5 days',
      memory: '2048MB',
      diskSpace: '500GB',
    };

    render(
      <SystemInfoSection title="System" data={largeData} defaultOpen />,
      { wrapper: TestWrapper }
    );

    const filterInput = screen.getByPlaceholderText('Filter properties...');
    fireEvent.change(filterInput, { target: { value: 'nonexistent' } });

    expect(screen.getByText('No matching properties found')).toBeInTheDocument();
  });

  it('shows empty message when data is empty', () => {
    render(
      <SystemInfoSection title="Empty Section" data={{}} defaultOpen />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('No properties available')).toBeInTheDocument();
  });

  it('formats boolean values correctly', () => {
    const dataWithBoolean = {
      enabled: true,
      disabled: false,
    };

    render(
      <SystemInfoSection title="Boolean Test" data={dataWithBoolean} defaultOpen />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Yes')).toBeInTheDocument();
    expect(screen.getByText('No')).toBeInTheDocument();
  });

  it('handles null and undefined values', () => {
    const dataWithNulls = {
      nullValue: null,
      undefinedValue: undefined,
      normalValue: 'test',
    };

    render(
      <SystemInfoSection title="Null Test" data={dataWithNulls} defaultOpen />,
      { wrapper: TestWrapper }
    );

    const dashes = screen.getAllByText('-');
    expect(dashes.length).toBeGreaterThanOrEqual(2);
  });

  it('handles single item singular text', () => {
    const singleItem = { version: '1.0.0' };

    render(
      <SystemInfoSection title="Single" data={singleItem} />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('(1 item)')).toBeInTheDocument();
  });
});


