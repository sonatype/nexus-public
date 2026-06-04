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
import '@testing-library/jest-dom';

const mockRestGet = jest.fn();

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({ stateService: { go: jest.fn() } }),
}));

jest.mock('../../../../../interface/api', () => ({
  restClient: { get: (...args: unknown[]) => mockRestGet(...args) },
  ENDPOINTS: {
    HEALTH_CHECK: '/service/rest/internal/ui/healthcheck',
    FIREWALL_STATUS: '/service/rest/internal/ui/firewall/status',
  },
}));

import { SecurityReportPage } from '../SecurityReportPage';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme>{ui}</Theme>);
}

describe('SecurityReportPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Health Check report type', () => {
    const healthCheckApiResponse = [
      {
        repositoryName: 'maven-central',
        enabled: true,
        detailUrl: 'https://iq.example.com/report',
        securityIssueCount: 5,
        licenseIssueCount: 3,
        results: { criticalCount: 5, severeCount: 2, totalCount: 10 },
      },
    ];

    it('calls HEALTH_CHECK endpoint when reportType is health-check', async () => {
      mockRestGet.mockResolvedValue(healthCheckApiResponse);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="health-check" />);

      await waitFor(() => {
        expect(mockRestGet).toHaveBeenCalledWith('/service/rest/internal/ui/healthcheck');
      });
    });

    it('shows loading state initially', () => {
      mockRestGet.mockImplementation(() => new Promise(() => {}));
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="health-check" />);

      expect(screen.getByText('Loading report...')).toBeInTheDocument();
    });

    it('renders summary cards and content when data loads', async () => {
      mockRestGet.mockResolvedValue(healthCheckApiResponse);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="health-check" />);

      await waitFor(() => {
        expect(screen.getByText('Health Check Report')).toBeInTheDocument();
      });

      expect(screen.getByText('Critical')).toBeInTheDocument();
      expect(screen.getByText('Severe')).toBeInTheDocument();
      expect(screen.getByText('Moderate')).toBeInTheDocument();
      expect(screen.getByText('Artifacts identified')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getByText('10')).toBeInTheDocument();
    });

    it('shows Back button when not embedded', async () => {
      mockRestGet.mockResolvedValue(healthCheckApiResponse);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="health-check" />);

      await waitFor(() => {
        expect(screen.getByText('Back')).toBeInTheDocument();
      });
    });

    it('hides Back button when embedded', async () => {
      mockRestGet.mockResolvedValue(healthCheckApiResponse);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="health-check" embedded />);

      await waitFor(() => {
        expect(screen.getByText('Health Check Report')).toBeInTheDocument();
      });
      expect(screen.queryByText('Back')).not.toBeInTheDocument();
    });

    it('shows error when repository not found in health check data', async () => {
      mockRestGet.mockResolvedValue([{ repositoryName: 'other-repo' }]);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="health-check" />);

      await waitFor(() => {
        expect(screen.getByText(/No health check data found for maven-central/)).toBeInTheDocument();
      });
    });

    it('shows error when API throws', async () => {
      mockRestGet.mockRejectedValue(new Error('Network error'));
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="health-check" />);

      await waitFor(() => {
        expect(screen.getByText('Network error')).toBeInTheDocument();
      });
    });

    it('shows Open in IQ Server link when reportUrl exists', async () => {
      mockRestGet.mockResolvedValue(healthCheckApiResponse);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="health-check" />);

      await waitFor(() => {
        const link = screen.getByText('Open in IQ Server').closest('a');
        expect(link).toHaveAttribute('href', 'https://iq.example.com/report');
      });
    });
  });

  describe('Firewall report type', () => {
    const firewallApiResponse = [
      {
        repositoryName: 'maven-central',
        affectedComponentCount: 50,
        criticalComponentCount: 10,
        severeComponentCount: 5,
        moderateComponentCount: 2,
        quarantinedComponentCount: 3,
        reportUrl: 'https://iq.example.com/firewall-report',
      },
    ];

    it('calls FIREWALL_STATUS endpoint when reportType is firewall', async () => {
      mockRestGet.mockResolvedValue(firewallApiResponse);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="firewall" />);

      await waitFor(() => {
        expect(mockRestGet).toHaveBeenCalledWith('/service/rest/internal/ui/firewall/status');
      });
    });

    it('renders firewall summary with quarantine badge when quarantinedComponentCount > 0', async () => {
      mockRestGet.mockResolvedValue(firewallApiResponse);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="firewall" />);

      await waitFor(() => {
        expect(screen.getByText('Firewall Report')).toBeInTheDocument();
      });

      expect(screen.getByText(/3 components are currently in quarantine/)).toBeInTheDocument();
      expect(screen.getByText('10')).toBeInTheDocument();
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('50')).toBeInTheDocument();
    });

    it('shows error when repository not found in firewall data', async () => {
      mockRestGet.mockResolvedValue([{ repositoryName: 'other-repo' }]);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="firewall" />);

      await waitFor(() => {
        expect(screen.getByText(/No firewall data found for maven-central/)).toBeInTheDocument();
      });
    });

    it('does not show quarantine badge when quarantinedComponentCount is 0', async () => {
      mockRestGet.mockResolvedValue([
        {
          ...firewallApiResponse[0],
          quarantinedComponentCount: 0,
        },
      ]);
      renderWithTheme(<SecurityReportPage repositoryName="maven-central" reportType="firewall" />);

      await waitFor(() => {
        expect(screen.getByText('Firewall Report')).toBeInTheDocument();
      });
      expect(screen.queryByText(/quarantine/)).not.toBeInTheDocument();
    });
  });
});
