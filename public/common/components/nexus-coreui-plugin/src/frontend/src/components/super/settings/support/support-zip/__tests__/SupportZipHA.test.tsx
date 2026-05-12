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
import { Theme } from '@radix-ui/themes';

import { SupportZipHA } from '../SupportZipHA';
import { SupportZipParams } from '../types';

// Wrapper component for Radix Theme
function TestWrapper({ children }: { children: React.ReactNode }) {
  return <Theme>{children}</Theme>;
}

describe('SupportZipHA', () => {
  const defaultParams: SupportZipParams = {
    systemInformation: true,
    threadDump: true,
    configuration: true,
    security: true,
    log: true,
    taskLog: true,
    replication: false,
    auditLog: false,
    metrics: true,
    jmx: false,
    archivedLog: 0,
    limitFileSizes: true,
    limitZipSize: true,
  };

  const mockOnParamChange = jest.fn();
  const mockOnSubmit = jest.fn();
  const mockOnSubmitAll = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the HA header and description', () => {
    render(
      <SupportZipHA
        params={defaultParams}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        onSubmitAll={mockOnSubmitAll}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('High Availability Mode')).toBeInTheDocument();
    expect(screen.getByText(/clustered environment/i)).toBeInTheDocument();
  });

  it('renders the HA actions', () => {
    render(
      <SupportZipHA
        params={defaultParams}
        onParamChange={mockOnParamChange}
        onSubmit={mockOnSubmit}
        onSubmitAll={mockOnSubmitAll}
      />,
      { wrapper: TestWrapper }
    );

    expect(screen.getByText('Create support ZIP')).toBeInTheDocument();
    expect(screen.getByText('Create support ZIP (all nodes)')).toBeInTheDocument();
  });
});

