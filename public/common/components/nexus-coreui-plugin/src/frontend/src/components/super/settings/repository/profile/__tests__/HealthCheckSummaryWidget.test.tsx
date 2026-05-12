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
import '@testing-library/jest-dom';

import { HealthCheckSummaryWidget } from '../HealthCheckSummaryWidget';
import type { HealthCheckData, FirewallData } from '../hooks/useRepositoryProfile';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('HealthCheckSummaryWidget', () => {
  const onSelectTab = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders nothing when isGroup is true', () => {
    renderWithTheme(
      <HealthCheckSummaryWidget
        repositoryName="group-repo"
        healthCheck={null}
        firewall={null}
        onSelectTab={onSelectTab}
        isGroup={true}
      />
    );

    expect(screen.queryByText('Health Check')).not.toBeInTheDocument();
    expect(screen.queryByText('Firewall')).not.toBeInTheDocument();
  });

  it('renders Health Check and Firewall cards for non-group', () => {
    const healthCheck: HealthCheckData = {
      enabled: true,
      securityIssueCount: 5,
      licenseIssueCount: 2,
    };
    const firewall: FirewallData = {
      enabled: true,
      quarantinedComponentCount: 0,
    };

    renderWithTheme(
      <HealthCheckSummaryWidget
        repositoryName="maven-central"
        healthCheck={healthCheck}
        firewall={firewall}
        onSelectTab={onSelectTab}
        isGroup={false}
      />
    );

    expect(screen.getByText('Health Check')).toBeInTheDocument();
    expect(screen.getByText('Firewall')).toBeInTheDocument();
    expect(screen.getByText('5 vulns')).toBeInTheDocument();
    expect(screen.getByText('2 license')).toBeInTheDocument();
    expect(screen.getByText('Protected')).toBeInTheDocument();
  });

  it('calls onSelectTab with health-check when Health Check card is clicked', () => {
    renderWithTheme(
      <HealthCheckSummaryWidget
        repositoryName="maven-central"
        healthCheck={{ enabled: true, securityIssueCount: 0, licenseIssueCount: 0 }}
        firewall={null}
        onSelectTab={onSelectTab}
        isGroup={false}
      />
    );

    fireEvent.click(screen.getByText('Health Check'));
    expect(onSelectTab).toHaveBeenCalledWith('health-check');
  });

  it('calls onSelectTab with firewall-report when Firewall card is clicked', () => {
    renderWithTheme(
      <HealthCheckSummaryWidget
        repositoryName="maven-central"
        healthCheck={null}
        firewall={{ enabled: true }}
        onSelectTab={onSelectTab}
        isGroup={false}
      />
    );

    fireEvent.click(screen.getByText('Firewall'));
    expect(onSelectTab).toHaveBeenCalledWith('firewall-report');
  });

  it('shows No issues when health check has no counts', () => {
    renderWithTheme(
      <HealthCheckSummaryWidget
        repositoryName="maven-central"
        healthCheck={{ enabled: true }}
        firewall={null}
        onSelectTab={onSelectTab}
        isGroup={false}
      />
    );

    expect(screen.getByText('No issues')).toBeInTheDocument();
  });

  it('shows Unprotected when firewall is not enabled', () => {
    renderWithTheme(
      <HealthCheckSummaryWidget
        repositoryName="maven-central"
        healthCheck={null}
        firewall={{ enabled: false }}
        onSelectTab={onSelectTab}
        isGroup={false}
      />
    );

    expect(screen.getByText('Unprotected')).toBeInTheDocument();
  });
});
