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
import { CapabilityDetail } from '../CapabilityDetail';
import { Capability, CapabilityType } from '../types';
import { useCapabilitiesApi } from '../useCapabilitiesApi';

jest.mock('../useCapabilitiesApi', () => ({
  useCapabilitiesApi: jest.fn(),
}));

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
}));

const renderWithTheme = (component: React.ReactElement) => {
  return render(<Theme>{component}</Theme>);
};

describe('CapabilityDetail', () => {
  const mockFetchCapabilityTypes = jest.fn();
  const mockOnSave = jest.fn().mockResolvedValue(undefined);
  const mockOnDelete = jest.fn();
  const mockOnEnable = jest.fn();
  const mockOnDisable = jest.fn();
  const mockOnBack = jest.fn();

  const baseCapability: Capability = {
    id: 'cap-1',
    typeId: 'outreach',
    typeName: 'Outreach: Management',
    enabled: true,
    active: true,
    error: false,
    state: 'active',
    stateDescription: 'Active',
    description: 'Enables outreach features',
    notes: 'Test capability notes',
    properties: {
      repository: 'maven-central',
      interval: '60',
    },
    tags: { repository: 'maven-central' },
  };

  const capabilityType: CapabilityType = {
    id: 'outreach',
    name: 'Outreach: Management',
    about: 'About outreach capability',
    formFields: [
      { id: 'repository', type: 'combobox', label: 'Repository', required: true },
      { id: 'interval', type: 'number', label: 'Interval', required: false },
    ],
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockFetchCapabilityTypes.mockResolvedValue([capabilityType]);
    (useCapabilitiesApi as jest.Mock).mockReturnValue({
      fetchCapabilityTypes: mockFetchCapabilityTypes,
    });
  });

  it('renders summary information', async () => {
    renderWithTheme(
      <CapabilityDetail
        capability={baseCapability}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onEnable={mockOnEnable}
        onDisable={mockOnDisable}
        onBack={mockOnBack}
      />
    );

    expect(await screen.findByText('Outreach: Management')).toBeInTheDocument();
    expect(screen.getByText('Enables outreach features')).toBeInTheDocument();
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('maven-central')).toBeInTheDocument();
  });

  it('displays notes section', () => {
    renderWithTheme(
      <CapabilityDetail
        capability={baseCapability}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onEnable={mockOnEnable}
        onDisable={mockOnDisable}
        onBack={mockOnBack}
      />
    );

    expect(screen.getByText('Test capability notes')).toBeInTheDocument();
  });

  it('shows Disable button for enabled capability', () => {
    renderWithTheme(
      <CapabilityDetail
        capability={baseCapability}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onEnable={mockOnEnable}
        onDisable={mockOnDisable}
        onBack={mockOnBack}
      />
    );

    expect(screen.getByRole('button', { name: /disable/i })).toBeInTheDocument();
  });

  it('shows Enable button for disabled capability', () => {
    renderWithTheme(
      <CapabilityDetail
        capability={{ ...baseCapability, enabled: false, active: false, state: 'disabled' }}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onEnable={mockOnEnable}
        onDisable={mockOnDisable}
        onBack={mockOnBack}
      />
    );

    expect(screen.getByRole('button', { name: /enable/i })).toBeInTheDocument();
  });

  it('calls onDisable when Disable button is clicked', async () => {
    renderWithTheme(
      <CapabilityDetail
        capability={baseCapability}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onEnable={mockOnEnable}
        onDisable={mockOnDisable}
        onBack={mockOnBack}
      />
    );

    await userEvent.click(screen.getByRole('button', { name: /disable/i }));
    expect(mockOnDisable).toHaveBeenCalled();
  });

  it('calls onEnable when Enable button is clicked', async () => {
    renderWithTheme(
      <CapabilityDetail
        capability={{ ...baseCapability, enabled: false, active: false, state: 'disabled' }}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onEnable={mockOnEnable}
        onDisable={mockOnDisable}
        onBack={mockOnBack}
      />
    );

    await userEvent.click(screen.getByRole('button', { name: /enable/i }));
    expect(mockOnEnable).toHaveBeenCalled();
  });

  it('shows loading state in settings tab while types load', async () => {
    mockFetchCapabilityTypes.mockReturnValue(new Promise(() => undefined));

    renderWithTheme(
      <CapabilityDetail
        capability={baseCapability}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onEnable={mockOnEnable}
        onDisable={mockOnDisable}
        onBack={mockOnBack}
      />
    );

    await userEvent.click(screen.getByRole('tab', { name: /settings/i }));
    expect(await screen.findByText('Loading settings...')).toBeInTheDocument();
  });

  it('shows error state when capability type is missing', async () => {
    mockFetchCapabilityTypes.mockResolvedValue([]);

    renderWithTheme(
      <CapabilityDetail
        capability={baseCapability}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onEnable={mockOnEnable}
        onDisable={mockOnDisable}
        onBack={mockOnBack}
      />
    );

    await userEvent.click(screen.getByRole('tab', { name: /settings/i }));
    expect(
      await screen.findByText('Unable to load capability type configuration')
    ).toBeInTheDocument();
  });

  it('renders about content when available', async () => {
    renderWithTheme(
      <CapabilityDetail
        capability={baseCapability}
        onSave={mockOnSave}
        onDelete={mockOnDelete}
        onEnable={mockOnEnable}
        onDisable={mockOnDisable}
        onBack={mockOnBack}
      />
    );

    await userEvent.click(screen.getByRole('tab', { name: /about/i }));
    expect(await screen.findByText('About outreach capability')).toBeInTheDocument();
  });
});


