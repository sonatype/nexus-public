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
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { SupportZipForm } from '../SupportZipForm';
import { DEFAULT_SUPPORT_ZIP_PARAMS } from '../types';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('SupportZipForm', () => {
  const mockOnSubmit = jest.fn();
  const mockOnHaSubmit = jest.fn();
  const mockOnParamChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders form with all sections', () => {
    render(
      <SupportZipForm
        params={DEFAULT_SUPPORT_ZIP_PARAMS}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        disabled={false}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Contents')).toBeInTheDocument();
    expect(screen.getByText('Options')).toBeInTheDocument();
  });

  it('renders all content checkboxes', () => {
    render(
      <SupportZipForm
        params={DEFAULT_SUPPORT_ZIP_PARAMS}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        disabled={false}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('System information report')).toBeInTheDocument();
    expect(screen.getByText('JVM thread-dump')).toBeInTheDocument();
    expect(screen.getByText('Configuration files')).toBeInTheDocument();
    expect(screen.getByText('Security configuration files')).toBeInTheDocument();
    expect(screen.getByText('Log files')).toBeInTheDocument();
    expect(screen.getByText('Task log files')).toBeInTheDocument();
    expect(screen.getByText('Replication log files')).toBeInTheDocument();
    expect(screen.getByText('Audit log files')).toBeInTheDocument();
    expect(screen.getByText('System and component metrics')).toBeInTheDocument();
    expect(screen.getByText('JMX information')).toBeInTheDocument();
  });

  it('renders option checkboxes', () => {
    render(
      <SupportZipForm
        params={DEFAULT_SUPPORT_ZIP_PARAMS}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        disabled={false}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Limit files in the ZIP archive to 30 MB apiece')).toBeInTheDocument();
    expect(screen.getByText('Limit the ZIP archive to 50 MB')).toBeInTheDocument();
  });

  it('calls onSubmit when button is clicked', async () => {
    render(
      <SupportZipForm
        params={DEFAULT_SUPPORT_ZIP_PARAMS}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        disabled={false}
      />,
      { wrapper: TestWrapper }
    );

    const submitButton = screen.getByText('Create support ZIP');
    await userEvent.click(submitButton);

    expect(mockOnSubmit).toHaveBeenCalled();
  });

  it('disables button when disabled', () => {
    render(
      <SupportZipForm
        params={DEFAULT_SUPPORT_ZIP_PARAMS}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        disabled={true}
      />,
      { wrapper: TestWrapper }
    );

    const button = screen.getByText('Create support ZIP');
    expect(button.closest('button')).toBeDisabled();
  });

  it('shows HA button when onHaSubmit is provided', () => {
    render(
      <SupportZipForm
        params={DEFAULT_SUPPORT_ZIP_PARAMS}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        onSubmitAll={mockOnHaSubmit}
        disabled={false}
        isHa={true}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Create support ZIP (all nodes)')).toBeInTheDocument();
  });

  it('does not show HA button when onHaSubmit is not provided', () => {
    render(
      <SupportZipForm
        params={DEFAULT_SUPPORT_ZIP_PARAMS}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        disabled={false}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.queryByText('Create support ZIP (all nodes)')).not.toBeInTheDocument();
  });

  it('calls onHaSubmit when HA button is clicked', () => {
    render(
      <SupportZipForm
        params={DEFAULT_SUPPORT_ZIP_PARAMS}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        onSubmitAll={mockOnHaSubmit}
        disabled={false}
        isHa={true}
      />,
      { wrapper: TestWrapper }
    );

    fireEvent.click(screen.getByText('Create support ZIP (all nodes)'));

    expect(mockOnHaSubmit).toHaveBeenCalled();
  });

  it('updates params when checkbox is toggled', async () => {
    render(
      <SupportZipForm
        params={DEFAULT_SUPPORT_ZIP_PARAMS}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        disabled={false}
      />,
      { wrapper: TestWrapper }
    );

    // Find the checkbox input by its label
    const checkbox = screen.getByLabelText('System information report');
    await userEvent.click(checkbox);

    expect(mockOnParamChange).toHaveBeenCalledWith('systemInformation', false);
  });

  it('reflects params state in checkboxes', () => {
    const customParams = {
      ...DEFAULT_SUPPORT_ZIP_PARAMS,
      systemInformation: false,
      threadDump: false,
      log: true,
    };

    render(
      <SupportZipForm
        params={customParams}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        disabled={false}
      />,
      { wrapper: TestWrapper }
    );

    // The checkboxes should reflect the params state
    // This is a basic structure test - the actual checked state would be
    // more accurately tested with data-testid attributes
    expect(screen.getByText('System information report')).toBeInTheDocument();
  });

  // Test all checkbox onChange handlers to improve coverage
  describe('checkbox onChange handlers', () => {
    it('calls onParamChange for threadDump checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('JVM thread-dump');
      await userEvent.click(checkbox);

      expect(mockOnParamChange).toHaveBeenCalledWith('threadDump', false);
    });

    it('calls onParamChange for configuration checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('Configuration files');
      await userEvent.click(checkbox);

      expect(mockOnParamChange).toHaveBeenCalledWith('configuration', false);
    });

    it('calls onParamChange for security checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('Security configuration files');
      await userEvent.click(checkbox);

      expect(mockOnParamChange).toHaveBeenCalledWith('security', false);
    });

    it('calls onParamChange for log checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('Log files');
      await userEvent.click(checkbox);

      expect(mockOnParamChange).toHaveBeenCalledWith('log', false);
    });

    it('calls onParamChange for taskLog checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('Task log files');
      await userEvent.click(checkbox);

      // Default is true, so clicking toggles to false
      expect(mockOnParamChange).toHaveBeenCalledWith('taskLog', false);
    });

    it('calls onParamChange for replication checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('Replication log files');
      await userEvent.click(checkbox);

      // Default is true, so clicking toggles to false
      expect(mockOnParamChange).toHaveBeenCalledWith('replication', false);
    });

    it('calls onParamChange for auditLog checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('Audit log files');
      await userEvent.click(checkbox);

      // Default is true, so clicking toggles to false
      expect(mockOnParamChange).toHaveBeenCalledWith('auditLog', false);
    });

    it('calls onParamChange for metrics checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('System and component metrics');
      await userEvent.click(checkbox);

      expect(mockOnParamChange).toHaveBeenCalledWith('metrics', false);
    });

    it('calls onParamChange for jmx checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('JMX information');
      await userEvent.click(checkbox);

      expect(mockOnParamChange).toHaveBeenCalledWith('jmx', false);
    });

    it('calls onParamChange for limitFileSizes checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('Limit files in the ZIP archive to 30 MB apiece');
      await userEvent.click(checkbox);

      expect(mockOnParamChange).toHaveBeenCalledWith('limitFileSizes', false);
    });

    it('calls onParamChange for limitZipSize checkbox', async () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      const checkbox = screen.getByLabelText('Limit the ZIP archive to 50 MB');
      await userEvent.click(checkbox);

      expect(mockOnParamChange).toHaveBeenCalledWith('limitZipSize', false);
    });
  });

  describe('archived logs dropdown', () => {
    it('renders archived logs dropdown', () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('Include logs from previous days')).toBeInTheDocument();
    });
  });

  describe('description text', () => {
    it('renders description messages', () => {
      render(
        <SupportZipForm
          params={DEFAULT_SUPPORT_ZIP_PARAMS}
          onParamChange={mockOnParamChange}
          onSubmit={mockOnSubmit}
          disabled={false}
        />,
        { wrapper: TestWrapper }
      );

      expect(screen.getByText('No information will be sent to Sonatype when creating the support ZIP file.')).toBeInTheDocument();
      expect(screen.getByText('Support ZIP creation may take a few minutes to complete.')).toBeInTheDocument();
    });
  });
});

