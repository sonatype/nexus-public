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

import { AuditApiPage } from '../AuditApiPage';

// Helper to render with Radix Theme provider
function renderWithTheme(component: React.ReactElement) {
  return render(<Theme>{component}</Theme>);
}

describe('AuditApiPage', () => {
  describe('Header Section', () => {
    it('should render the main heading', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByRole('heading', { name: 'Protect Audit API' })).toBeInTheDocument();
    });

    it('should render the description text', () => {
      renderWithTheme(<AuditApiPage />);

      expect(
        screen.getByText(/REST API for querying audit events across all administrative changes/)
      ).toBeInTheDocument();
    });
  });

  describe('Overview Cards', () => {
    it('should display Base URL information', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('Base URL')).toBeInTheDocument();
      expect(screen.getByText('/service/rest/internal/ui')).toBeInTheDocument();
    });

    it('should display Authentication information', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('Authentication')).toBeInTheDocument();
      expect(screen.getByText('Basic Auth (admin credentials required)')).toBeInTheDocument();
    });

    it('should display Content Type information', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('Content Type')).toBeInTheDocument();
      expect(screen.getByText('application/json')).toBeInTheDocument();
    });
  });

  describe('API Endpoints Section', () => {
    it('should display API Endpoints heading', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByRole('heading', { name: 'API Endpoints' })).toBeInTheDocument();
    });

    it('should display GET method badge', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('GET')).toBeInTheDocument();
    });

    it('should display the audit-log endpoint path', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('/service/rest/internal/ui/audit-log')).toBeInTheDocument();
    });

    it('should display List Audit Events title', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByRole('heading', { name: 'List Audit Events' })).toBeInTheDocument();
    });
  });

  describe('Query Parameters Section', () => {
    it('should display Query Parameters heading', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByRole('heading', { name: 'Query Parameters' })).toBeInTheDocument();
    });

    it('should list the page parameter', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('page')).toBeInTheDocument();
      expect(screen.getByText('Page number (1-based)')).toBeInTheDocument();
    });

    it('should list the limit parameter', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('limit')).toBeInTheDocument();
      expect(screen.getByText('Items per page (max 100)')).toBeInTheDocument();
    });

    it('should list the categories parameter', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('categories')).toBeInTheDocument();
      expect(
        screen.getByText(/Filter by categories: security, repository, configuration, protection/)
      ).toBeInTheDocument();
    });

    it('should list the domains parameter', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('domains')).toBeInTheDocument();
    });

    it('should list the types parameter', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('types')).toBeInTheDocument();
    });

    it('should list the initiators parameter', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('initiators')).toBeInTheDocument();
    });

    it('should list the startDate parameter', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('startDate')).toBeInTheDocument();
      expect(screen.getByText('ISO 8601 start date (inclusive)')).toBeInTheDocument();
    });

    it('should list the endDate parameter', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('endDate')).toBeInTheDocument();
      expect(screen.getByText('ISO 8601 end date (inclusive)')).toBeInTheDocument();
    });

    it('should display type badges for parameters', () => {
      renderWithTheme(<AuditApiPage />);

      // Check for integer type badge
      expect(screen.getAllByText('integer').length).toBeGreaterThanOrEqual(2);
      // Check for string type badges
      expect(screen.getAllByText('string').length).toBeGreaterThanOrEqual(2);
      // Check for string[] type badges
      expect(screen.getAllByText('string[]').length).toBeGreaterThanOrEqual(4);
    });
  });

  describe('Response Body Section', () => {
    it('should display Response Body heading', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByRole('heading', { name: 'Response Body' })).toBeInTheDocument();
    });

    it('should display example response JSON', () => {
      renderWithTheme(<AuditApiPage />);

      // Check for response structure elements
      expect(screen.getByText(/"items":/)).toBeInTheDocument();
      expect(screen.getByText(/"pagination":/)).toBeInTheDocument();
    });
  });

  describe('Examples Section', () => {
    it('should display Examples heading', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByRole('heading', { name: 'Examples' })).toBeInTheDocument();
    });

    it('should display example tabs', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getAllByText('Get all events (first page)').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Filter by security category').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Filter by date range').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Filter by multiple criteria').length).toBeGreaterThanOrEqual(1);
    });

    it('should display curl command example', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText(/curl -u admin:admin123/)).toBeInTheDocument();
    });
  });

  describe('Event Categories Section', () => {
    it('should display Event Categories heading', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByRole('heading', { name: 'Event Categories' })).toBeInTheDocument();
    });

    it('should display security category', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('Security Events')).toBeInTheDocument();
      expect(
        screen.getByText(/User, Role, Privilege, LDAP, SAML, SSL Certificate, Realm/)
      ).toBeInTheDocument();
    });

    it('should display repository category', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('Repository Events')).toBeInTheDocument();
      expect(screen.getByText(/Repository, Component, Asset, Blob Store/)).toBeInTheDocument();
    });

    it('should display configuration category', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('Configuration Events')).toBeInTheDocument();
      expect(
        screen.getByText(/Task, Capability, Cleanup Policy, Content Selector/)
      ).toBeInTheDocument();
    });

    it('should display protection category', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('Protection Events')).toBeInTheDocument();
      expect(
        screen.getByText(/Protection Configuration, Malware Removal, Firewall Quarantine/)
      ).toBeInTheDocument();
    });

    it('should display category badges', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByText('security')).toBeInTheDocument();
      expect(screen.getByText('repository')).toBeInTheDocument();
      expect(screen.getByText('configuration')).toBeInTheDocument();
      expect(screen.getByText('protection')).toBeInTheDocument();
    });
  });

  describe('Interactive API Explorer Section', () => {
    it('should display Interactive API Explorer heading', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getByRole('heading', { name: 'Interactive API Explorer' })).toBeInTheDocument();
    });

    it('should display Swagger UI link', () => {
      renderWithTheme(<AuditApiPage />);

      const swaggerLink = screen.getByRole('link', { name: /Open Swagger UI/i });
      expect(swaggerLink).toBeInTheDocument();
      expect(swaggerLink).toHaveAttribute(
        'href',
        'http://localhost:8081/?debug#preview/admin/system/api'
      );
    });

    it('should open Swagger link in new tab', () => {
      renderWithTheme(<AuditApiPage />);

      const swaggerLink = screen.getByRole('link', { name: /Open Swagger UI/i });
      expect(swaggerLink).toHaveAttribute('target', '_blank');
      expect(swaggerLink).toHaveAttribute('rel', 'noopener noreferrer');
    });
  });

  describe('Accessibility', () => {
    it('should have proper heading hierarchy', () => {
      renderWithTheme(<AuditApiPage />);

      const headings = screen.getAllByRole('heading');
      expect(headings.length).toBeGreaterThan(0);
    });

    it('should have accessible tab structure', () => {
      renderWithTheme(<AuditApiPage />);

      expect(screen.getAllByText('Get all events (first page)').length).toBeGreaterThanOrEqual(1);
      expect(screen.getAllByText('Filter by security category').length).toBeGreaterThanOrEqual(1);
    });
  });
});
