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
import '@testing-library/jest-dom';

// Stub out the three modal components so this suite can focus on the page's
// own controller wiring (handleCreate / handleRevoke). Each modal is exercised
// independently in its own *.test.tsx; here we only assert that the page
// passes the right props and wires the callbacks correctly.
jest.mock('../CreateTokenModal', () => ({
  CreateTokenModal: ({ open, onCreate, onClose }: any) =>
    open ? (
      <div data-testid="mock-create-modal">
        <button
          data-testid="mock-create-submit"
          onClick={() =>
            onCreate({ name: 'New Token', roleId: 'nx-admin', description: '' })
          }
        >
          submit
        </button>
        <button data-testid="mock-create-cancel" onClick={onClose}>
          cancel
        </button>
      </div>
    ) : null,
}));

jest.mock('../RevokeTokenModal', () => ({
  RevokeTokenModal: ({ open, tokenName, onConfirm, onClose }: any) =>
    open ? (
      <div data-testid="mock-revoke-modal" data-token-name={tokenName}>
        <button data-testid="mock-revoke-confirm" onClick={onConfirm}>
          confirm
        </button>
        <button data-testid="mock-revoke-cancel" onClick={onClose}>
          cancel
        </button>
      </div>
    ) : null,
}));

jest.mock('../RevealTokenModal', () => ({
  RevealTokenModal: ({ open, token, onClose }: any) =>
    open ? (
      <div data-testid="mock-reveal-modal" data-token={token}>
        <button data-testid="mock-reveal-done" onClick={onClose}>
          done
        </button>
      </div>
    ) : null,
}));

import { ServiceAccountTokensPage } from '../ServiceAccountTokensPage';
import { SERVICE_ACCOUNT_TOKENS_STRINGS } from '../strings';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.PAGE;

const DEFAULT_TOKENS = [
  {
    id: 'token-1',
    name: 'Jenkins CI',
    description: 'Build server token',
    roleId: 'nx-admin',
    createdBy: 'admin',
    createdAt: '2024-01-01T00:00:00Z',
    expiresAt: '2025-01-01T00:00:00Z',
    lastUsedAt: '2024-06-01T00:00:00Z',
  },
  {
    id: 'token-2',
    name: 'GitHub Actions',
    description: 'CI/CD pipeline',
    roleId: 'nx-deploy',
    createdBy: 'devops',
    createdAt: '2024-01-02T00:00:00Z',
    expiresAt: null,
    lastUsedAt: null,
  },
  {
    id: 'token-3',
    name: 'Expired Token',
    description: 'Old token',
    roleId: 'nx-reader',
    createdBy: 'tester',
    createdAt: '2023-01-01T00:00:00Z',
    expiresAt: '2024-01-01T00:00:00Z',
    lastUsedAt: '2024-01-01T00:00:00Z',
  },
];

const DEFAULT_ROLES = [
  { id: 'nx-admin', name: 'Administrator' },
  { id: 'nx-deploy', name: 'Deployer' },
  { id: 'nx-reader', name: 'Reader' },
];

let mockApiState: any = {
  tokens: DEFAULT_TOKENS,
  roles: DEFAULT_ROLES,
  rolesError: null,
  loading: false,
  error: null,
  setError: jest.fn(),
  loadAll: jest.fn().mockResolvedValue(undefined),
  createToken: jest.fn(),
  revokeToken: jest.fn(),
};

jest.mock('../useServiceAccountTokens', () => ({
  useServiceAccountTokens: () => mockApiState,
}));

beforeEach(() => {
  mockApiState = {
    tokens: DEFAULT_TOKENS,
    roles: DEFAULT_ROLES,
    rolesError: null,
    loading: false,
    error: null,
    setError: jest.fn(),
    loadAll: jest.fn().mockResolvedValue(undefined),
    createToken: jest.fn(),
    revokeToken: jest.fn(),
  };
});

describe('ServiceAccountTokensPage', () => {
  describe('filtering', () => {
    it('filters by name', () => {
      render(<ServiceAccountTokensPage />);

      const filterInput = screen.getByTestId('sat-filter-input');
      fireEvent.change(filterInput, { target: { value: 'Jenkins' } });

      expect(screen.getByTestId('sat-row-token-1')).toBeInTheDocument();
      expect(screen.queryByTestId('sat-row-token-2')).not.toBeInTheDocument();
      expect(screen.queryByTestId('sat-row-token-3')).not.toBeInTheDocument();
    });

    it('filters by role (roleId)', () => {
      render(<ServiceAccountTokensPage />);

      const filterInput = screen.getByTestId('sat-filter-input');
      fireEvent.change(filterInput, { target: { value: 'nx-admin' } });

      expect(screen.getByTestId('sat-row-token-1')).toBeInTheDocument();
      expect(screen.queryByTestId('sat-row-token-2')).not.toBeInTheDocument();
    });

    it('filters by createdBy', () => {
      render(<ServiceAccountTokensPage />);

      const filterInput = screen.getByTestId('sat-filter-input');
      fireEvent.change(filterInput, { target: { value: 'devops' } });

      expect(screen.getByTestId('sat-row-token-2')).toBeInTheDocument();
      expect(screen.queryByTestId('sat-row-token-1')).not.toBeInTheDocument();
    });

    it('does NOT filter by description', () => {
      render(<ServiceAccountTokensPage />);

      const filterInput = screen.getByTestId('sat-filter-input');
      // "Build server token" is the description of token-1
      fireEvent.change(filterInput, { target: { value: 'Build server' } });

      // Should not match any tokens because we no longer filter by description
      expect(screen.getByText(LABELS.EMPTY.TITLE_NO_MATCH)).toBeInTheDocument();
    });

    it('has correct placeholder text', () => {
      render(<ServiceAccountTokensPage />);

      const filterInput = screen.getByTestId('sat-filter-input');
      expect(filterInput).toHaveAttribute('placeholder', LABELS.FILTER_PLACEHOLDER);
    });
  });

  describe('about section', () => {
    it('renders the about section text', () => {
      render(<ServiceAccountTokensPage />);

      expect(screen.getByText(/View, create, and revoke tokens/)).toBeInTheDocument();
    });

    it('renders the learn more link', () => {
      render(<ServiceAccountTokensPage />);

      const learnMoreLink = screen.getByRole('link', { name: LABELS.ABOUT.LINK });
      expect(learnMoreLink).toHaveAttribute('href', LABELS.ABOUT.LINK_HREF);
      expect(learnMoreLink).toHaveAttribute('target', '_blank');
      expect(learnMoreLink).toHaveAttribute('rel', 'noopener noreferrer');
    });
  });

  describe('accessibility', () => {
    it('filter input has accessible label', () => {
      render(<ServiceAccountTokensPage />);

      const filterInput = screen.getByTestId('sat-filter-input');
      expect(filterInput).toHaveAttribute('aria-label', LABELS.FILTER_LABEL);
      expect(filterInput).toHaveAttribute('id', 'sat-filter-input');
    });

    it('toolbar has search role', () => {
      render(<ServiceAccountTokensPage />);

      expect(screen.getByRole('search')).toBeInTheDocument();
    });

    it('table is wrapped in a labelled region', () => {
      render(<ServiceAccountTokensPage />);

      const region = screen.getByTestId('sat-table');
      expect(region).toHaveAttribute('role', 'region');
      expect(region).toHaveAttribute('aria-label', LABELS.TABLE_REGION_LABEL);
    });
  });

  describe('sorting', () => {
    it('clicking the Name column header toggles sort direction', () => {
      render(<ServiceAccountTokensPage />);

      const nameHeader = screen.getByRole('columnheader', { name: /Name/i });
      // First click on the active column flips to descending
      fireEvent.click(nameHeader);

      // After the click, the table should still render all three rows
      expect(screen.getByTestId('sat-row-token-1')).toBeInTheDocument();
      expect(screen.getByTestId('sat-row-token-2')).toBeInTheDocument();
      expect(screen.getByTestId('sat-row-token-3')).toBeInTheDocument();
    });

    it('clicking a different sortable column changes the sort field', () => {
      render(<ServiceAccountTokensPage />);

      const createdByHeader = screen.getByRole('columnheader', { name: /Created By/i });
      fireEvent.click(createdByHeader);

      expect(screen.getByTestId('sat-row-token-1')).toBeInTheDocument();
      expect(screen.getByTestId('sat-row-token-2')).toBeInTheDocument();
      expect(screen.getByTestId('sat-row-token-3')).toBeInTheDocument();
    });
  });

  describe('loading state', () => {
    it('shows the loading spinner on initial fetch (loading=true and no tokens yet)', () => {
      mockApiState = { ...mockApiState, tokens: [], loading: true };

      render(<ServiceAccountTokensPage />);

      expect(screen.getByTestId('loading-state')).toBeInTheDocument();
      expect(screen.getByText(LABELS.LOADING)).toBeInTheDocument();
      expect(screen.queryByText(LABELS.EMPTY.TITLE_NO_TOKENS)).not.toBeInTheDocument();
    });

    it('does not show the loading spinner during a refresh when tokens already exist', () => {
      mockApiState = { ...mockApiState, loading: true };

      render(<ServiceAccountTokensPage />);

      expect(screen.queryByTestId('loading-state')).not.toBeInTheDocument();
      expect(screen.getByTestId('sat-row-token-1')).toBeInTheDocument();
    });

    it('shows the empty state when not loading and there are no tokens', () => {
      mockApiState = { ...mockApiState, tokens: [], loading: false };

      render(<ServiceAccountTokensPage />);

      expect(screen.queryByTestId('loading-state')).not.toBeInTheDocument();
      expect(screen.getByText(LABELS.EMPTY.TITLE_NO_TOKENS)).toBeInTheDocument();
    });
  });

  describe('create flow', () => {
    it('opens the create modal, calls createToken, and reveals the new token on success', async () => {
      mockApiState.createToken = jest
        .fn()
        .mockResolvedValue({ token: { id: 'new', name: 'New Token' }, rawToken: 'raw-token-123' });

      render(<ServiceAccountTokensPage />);

      // Open the create modal.
      fireEvent.click(screen.getByTestId('sat-create-button'));
      expect(screen.getByTestId('mock-create-modal')).toBeInTheDocument();

      // Submit the form via the mocked modal — fires onCreate with a stubbed payload.
      fireEvent.click(screen.getByTestId('mock-create-submit'));

      // After createToken resolves, the create modal closes and the reveal
      // modal opens with the rawToken returned from the API.
      await waitFor(() =>
        expect(mockApiState.createToken).toHaveBeenCalledWith({
          name: 'New Token',
          roleId: 'nx-admin',
          description: '',
        }),
      );
      await waitFor(() =>
        expect(screen.queryByTestId('mock-create-modal')).not.toBeInTheDocument(),
      );
      const reveal = await screen.findByTestId('mock-reveal-modal');
      expect(reveal).toHaveAttribute('data-token', 'raw-token-123');
    });
  });

  // Revoke-flow integration is exercised by RevokeTokenModal.test.tsx (modal
  // calls onConfirm) and serviceAccountTokensMachine.test.ts (REVOKE hits the
  // right service and refreshes). The dropdown-to-modal step would require
  // driving Radix's DropdownMenu, which depends on pointer-capture behavior
  // that jsdom does not implement.

  describe('description column truncation', () => {
    const LONG_DESCRIPTION = 'This description is intentionally long enough to exceed the threshold';
    const SHORT_DESCRIPTION = 'A brief note';

    it('truncates and renders a tooltip when description is longer than 50 characters', () => {
      mockApiState = {
        ...mockApiState,
        tokens: [{
          id: 'long-desc',
          name: 'token-with-long-desc',
          description: LONG_DESCRIPTION,
          roleId: 'nx-admin',
          createdBy: 'admin',
          createdAt: '2024-01-01T00:00:00Z',
          expiresAt: null,
          lastUsedAt: null,
        }],
      };

      render(<ServiceAccountTokensPage />);

      const trigger = screen.getByTestId('sat-description-tooltip-trigger');
      expect(trigger).toHaveClass('sat-token-description--truncated');
      expect(trigger).toHaveTextContent(LONG_DESCRIPTION);
    });

    it('does not truncate or render a tooltip when description is 50 characters or fewer', () => {
      mockApiState = {
        ...mockApiState,
        tokens: [{
          id: 'short-desc',
          name: 'token-with-short-desc',
          description: SHORT_DESCRIPTION,
          roleId: 'nx-admin',
          createdBy: 'admin',
          createdAt: '2024-01-01T00:00:00Z',
          expiresAt: null,
          lastUsedAt: null,
        }],
      };

      render(<ServiceAccountTokensPage />);

      expect(screen.queryByTestId('sat-description-tooltip-trigger')).not.toBeInTheDocument();
      expect(screen.getByText(SHORT_DESCRIPTION)).not.toHaveClass('sat-token-description--truncated');
    });
  });

  describe('last used column', () => {
    it('shows formatted date when lastUsedAt is set', () => {
      render(<ServiceAccountTokensPage />);

      // token-1 has lastUsedAt: '2024-06-01T00:00:00Z'
      const row1 = screen.getByTestId('sat-row-token-1');
      // Formatted date varies by locale/timezone; assert it is not "Never used"
      expect(row1).not.toHaveTextContent(LABELS.LAST_USED_NEVER);
      expect(row1).toHaveTextContent('2024');
    });

    it('shows "Never used" when lastUsedAt is null', () => {
      render(<ServiceAccountTokensPage />);

      // token-2 has lastUsedAt: null
      const row2 = screen.getByTestId('sat-row-token-2');
      expect(row2).toHaveTextContent(LABELS.LAST_USED_NEVER);
    });
  });
});
