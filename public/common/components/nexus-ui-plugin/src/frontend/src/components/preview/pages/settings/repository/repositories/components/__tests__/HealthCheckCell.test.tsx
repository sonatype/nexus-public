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

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: jest.fn() } }),
}));

import { HealthCheckCell } from '../../../../../../shared/security/HealthCheckCell';
import { Repository, HealthCheckStatus } from '../../types';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

const proxyRepo: Repository = {
  name: 'maven-central',
  format: 'maven2',
  type: 'proxy',
  url: 'http://localhost:8081/repository/maven-central',
  online: true,
  status: { online: true },
  attributes: {},
};

const hostedRepo: Repository = {
  ...proxyRepo,
  name: 'maven-releases',
  type: 'hosted',
};

const unsupportedFormatRepo: Repository = {
  ...proxyRepo,
  name: 'docker-proxy',
  format: 'docker',
};

const enabledHealth: HealthCheckStatus = {
  repositoryName: 'maven-central',
  enabled: true,
  analyzing: false,
  securityIssueCount: 12,
  licenseIssueCount: 3,
};

describe('HealthCheckCell', () => {
  it('shows disabled icon for non-proxy repositories', () => {
    renderWithTheme(<HealthCheckCell repository={hostedRepo} />);
    expect(document.querySelector('.health-check-cell--na')).toBeInTheDocument();
  });

  it('shows disabled icon for unsupported formats', () => {
    renderWithTheme(<HealthCheckCell repository={unsupportedFormatRepo} />);
    expect(document.querySelector('.health-check-cell--unsupported')).toBeInTheDocument();
  });

  it('shows Analyze button for proxy repos without health status', () => {
    const onAnalyze = jest.fn();
    renderWithTheme(<HealthCheckCell repository={proxyRepo} onAnalyze={onAnalyze} />);
    expect(screen.getByText('Analyze')).toBeInTheDocument();
  });

  it('calls onAnalyze with stopPropagation when Analyze is clicked', () => {
    const onAnalyze = jest.fn();
    renderWithTheme(<HealthCheckCell repository={proxyRepo} onAnalyze={onAnalyze} />);
    fireEvent.click(screen.getByText('Analyze'));
    expect(onAnalyze).toHaveBeenCalledWith('maven-central');
  });

  it('shows analyzing spinner', () => {
    const analyzingStatus: HealthCheckStatus = { ...enabledHealth, analyzing: true };
    renderWithTheme(<HealthCheckCell repository={proxyRepo} healthStatus={analyzingStatus} />);
    expect(screen.getByText('Analyzing...')).toBeInTheDocument();
  });

  it('shows vulnerability counts when enabled', () => {
    renderWithTheme(<HealthCheckCell repository={proxyRepo} healthStatus={enabledHealth} />);
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
  });

  it('does not show malware badge when malwareCount is null or undefined', () => {
    const { container, rerender } = renderWithTheme(
      <HealthCheckCell repository={proxyRepo} healthStatus={{ ...enabledHealth, malwareCount: null }} />
    );
    expect(container.querySelectorAll('.health-check-cell__malware-badge-inner')).toHaveLength(0);

    rerender(
      <Theme>
        <HealthCheckCell repository={proxyRepo} healthStatus={{ ...enabledHealth, malwareCount: undefined }} />
      </Theme>
    );
    expect(container.querySelectorAll('.health-check-cell__malware-badge-inner')).toHaveLength(0);
  });

  it('shows green malware badge with skull when malwareCount is 0', () => {
    renderWithTheme(
      <HealthCheckCell repository={proxyRepo} healthStatus={{ ...enabledHealth, malwareCount: 0 }} />
    );
    expect(document.querySelector('.health-check-cell__malware-badge-inner')).toBeInTheDocument();
    expect(screen.getByText('0')).toBeInTheDocument();
  });

  it('shows red malware badge with count when malwareCount is positive', () => {
    renderWithTheme(
      <HealthCheckCell repository={proxyRepo} healthStatus={{ ...enabledHealth, malwareCount: 7 }} />
    );
    expect(screen.getByText('7')).toBeInTheDocument();
    expect(document.querySelector('.health-check-cell__malware-badge-inner')).toBeInTheDocument();
  });

  it('stops event propagation when clicking health check counts', () => {
    const parentClick = jest.fn();
    render(
      <Theme>
        <div onClick={parentClick}>
          <HealthCheckCell repository={proxyRepo} healthStatus={enabledHealth} />
        </div>
      </Theme>
    );

    const trigger = screen.getByRole('button', { name: /Health check:.*Click for details/i });
    fireEvent.click(trigger);
    expect(parentClick).not.toHaveBeenCalled();
  });

  it('has button role on health check counts for accessibility', () => {
    renderWithTheme(<HealthCheckCell repository={proxyRepo} healthStatus={enabledHealth} />);
    const trigger = screen.getByRole('button', { name: /Health check:.*Click for details/i });
    expect(trigger).toBeInTheDocument();
  });

  it('renders clickable cell with hover style', () => {
    renderWithTheme(<HealthCheckCell repository={proxyRepo} healthStatus={enabledHealth} />);
    expect(document.querySelector('.health-check-cell--clickable')).toBeInTheDocument();
  });

  it('shows modal with summary when counts are clicked', async () => {
    renderWithTheme(<HealthCheckCell repository={proxyRepo} healthStatus={enabledHealth} />);

    const trigger = screen.getByRole('button', { name: /Health check:.*Click for details/i });
    fireEvent.click(trigger);

    const modalTitle = await screen.findByText(/Health Check/);
    expect(modalTitle).toBeInTheDocument();
  });

  it('shows View Full Report button in modal', async () => {
    const healthWithReport: HealthCheckStatus = { ...enabledHealth, detailedReport: 'https://example.com/report' };
    renderWithTheme(<HealthCheckCell repository={proxyRepo} healthStatus={healthWithReport} />);

    const trigger = screen.getByRole('button', { name: /Health check:.*Click for details/i });
    fireEvent.click(trigger);

    const reportButton = await screen.findByText(/View Full Report/);
    expect(reportButton).toBeInTheDocument();
  });
});
