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

import { ProtectAuditPage } from '../ProtectAuditPage';

// Helper to render with Radix Theme provider
function renderWithTheme(component: React.ReactElement) {
  return render(<Theme>{component}</Theme>);
}

describe('ProtectAuditPage', () => {
  describe('Header Section', () => {
    it('should render the main heading', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByRole('heading', { name: 'Protect Audit' })).toBeInTheDocument();
    });

    it('should render the description text', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(
        screen.getByText(
          /Track and query all administrative changes related to repository protection/
        )
      ).toBeInTheDocument();
    });
  });

  describe('Quick Actions Cards', () => {
    describe('Audit Log Viewer Card', () => {
      it('should display Audit Log Viewer heading', () => {
        renderWithTheme(<ProtectAuditPage />);

        expect(screen.getByRole('heading', { name: 'Audit Log Viewer' })).toBeInTheDocument();
      });

      it('should display description text', () => {
        renderWithTheme(<ProtectAuditPage />);

        expect(
          screen.getByText(/Browse and filter all audit events across Security, Repository/)
        ).toBeInTheDocument();
      });

      it('should have Open Audit Log link', () => {
        renderWithTheme(<ProtectAuditPage />);

        const auditLogLink = screen.getByRole('link', { name: /Open Audit Log/i });
        expect(auditLogLink).toBeInTheDocument();
        expect(auditLogLink).toHaveAttribute(
          'href',
          'http://localhost:8081/?debug#preview/admin/audit'
        );
      });
    });

    describe('API Documentation Card', () => {
      it('should display API Documentation heading', () => {
        renderWithTheme(<ProtectAuditPage />);

        expect(screen.getByRole('heading', { name: 'API Documentation' })).toBeInTheDocument();
      });

      it('should display description text', () => {
        renderWithTheme(<ProtectAuditPage />);

        expect(
          screen.getByText(/Interactive REST API documentation for programmatic access/)
        ).toBeInTheDocument();
      });

      it('should have View API Docs link', () => {
        renderWithTheme(<ProtectAuditPage />);

        const apiDocsLink = screen.getByRole('link', { name: /View API Docs/i });
        expect(apiDocsLink).toBeInTheDocument();
        expect(apiDocsLink).toHaveAttribute(
          'href',
          'http://localhost:8081/?debug#preview/browse/protect/audit/api'
        );
      });
    });
  });

  describe('What Gets Audited Section', () => {
    it('should display section heading', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByRole('heading', { name: 'What Gets Audited?' })).toBeInTheDocument();
    });

    it('should list Security Events', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText('Security Events', { exact: false })).toBeInTheDocument();
      expect(screen.getByText(/User creation\/updates, role changes/)).toBeInTheDocument();
    });

    it('should list Repository Events', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText('Repository Events', { exact: false })).toBeInTheDocument();
      expect(screen.getByText(/Repository CRUD, component operations/)).toBeInTheDocument();
    });

    it('should list Configuration Events', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText('Configuration Events', { exact: false })).toBeInTheDocument();
      expect(screen.getByText(/Task execution, capability changes/)).toBeInTheDocument();
    });

    it('should list Protection Events', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText('Protection Events', { exact: false })).toBeInTheDocument();
      expect(screen.getByText(/Repository Health Check configuration/)).toBeInTheDocument();
    });
  });

  describe('Event Information Section', () => {
    it('should display section heading', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByRole('heading', { name: 'Event Information' })).toBeInTheDocument();
    });

    it('should describe Domain field', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText(/Domain/)).toBeInTheDocument();
      expect(screen.getByText(/The functional area/)).toBeInTheDocument();
    });

    it('should describe Type field', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText(/The action performed/)).toBeInTheDocument();
    });

    it('should describe Context field', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText(/Context/)).toBeInTheDocument();
      expect(screen.getByText(/The specific entity affected/)).toBeInTheDocument();
    });

    it('should describe Timestamp field', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText(/Timestamp/)).toBeInTheDocument();
      expect(screen.getByText(/ISO 8601 timestamp/)).toBeInTheDocument();
    });

    it('should describe Initiator field', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText(/Initiator/)).toBeInTheDocument();
      expect(screen.getByText(/Username of the person\/system/)).toBeInTheDocument();
    });

    it('should describe Node ID field', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText(/Node ID/)).toBeInTheDocument();
      expect(screen.getByText(/Cluster node identifier/)).toBeInTheDocument();
    });

    it('should describe Attributes field', () => {
      renderWithTheme(<ProtectAuditPage />);

      expect(screen.getByText(/Attributes/)).toBeInTheDocument();
      expect(screen.getByText(/Full JSON object/)).toBeInTheDocument();
    });
  });

  describe('Accessibility', () => {
    it('should have proper heading hierarchy', () => {
      renderWithTheme(<ProtectAuditPage />);

      // Check main heading exists
      expect(screen.getByRole('heading', { name: 'Protect Audit' })).toBeInTheDocument();

      // Check sub-headings exist
      const headings = screen.getAllByRole('heading');
      expect(headings.length).toBeGreaterThan(3);
    });

    it('should have accessible links', () => {
      renderWithTheme(<ProtectAuditPage />);

      const links = screen.getAllByRole('link');
      expect(links.length).toBe(2);

      // All links should have text content
      links.forEach((link) => {
        expect(link).toHaveTextContent(/.+/);
      });
    });
  });

  describe('Layout', () => {
    it('should render within a reasonable max-width container', () => {
      const { container } = renderWithTheme(<ProtectAuditPage />);

      // Check for the root container with max-width style
      const rootBox = container.querySelector('[style*="max-width"]');
      expect(rootBox).toBeInTheDocument();
    });
  });
});
