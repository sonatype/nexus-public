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
import {render, screen, fireEvent, act} from '@testing-library/react';
import '@testing-library/jest-dom';

import {RevealTokenModal} from '../RevealTokenModal';
import {SERVICE_ACCOUNT_TOKENS_STRINGS} from '../strings';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.REVEAL_MODAL;

describe('RevealTokenModal', () => {
  const defaultProps = {
    open: true,
    token: 'sat.test-token-abc123',
    name: 'jenkins-prod',
    onClose: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('rendering', () => {
    it('renders when open', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-reveal-modal')).toBeInTheDocument();
    });

    it('does not render when closed', () => {
      render(<RevealTokenModal {...defaultProps} open={false} />);
      expect(screen.queryByTestId('sat-reveal-modal')).not.toBeInTheDocument();
    });

    it('displays the token value', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-token-value')).toHaveTextContent('sat.test-token-abc123');
    });
  });

  describe('copy functionality', () => {
    it('has a copy button', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-token-copy')).toBeInTheDocument();
    });

    it('copies token to clipboard when copy button is clicked', async () => {
      const mockClipboard = {
        writeText: jest.fn().mockResolvedValue(undefined),
      };
      Object.assign(navigator, {clipboard: mockClipboard});

      render(<RevealTokenModal {...defaultProps} />);

      const copyButton = screen.getByTestId('sat-token-copy');
      fireEvent.click(copyButton);

      expect(mockClipboard.writeText).toHaveBeenCalledWith('sat.test-token-abc123');
    });
  });

  describe('copy announcement for screen readers', () => {
    it('has a visually hidden status region for copy announcement', () => {
      render(<RevealTokenModal {...defaultProps} />);

      // There should be a span with role="status" for the SR announcement
      const statusRegion = document.querySelector('[role="status"]');
      expect(statusRegion).toBeInTheDocument();
    });

    it('announces copy success text after copying', async () => {
      const mockClipboard = {
        writeText: jest.fn().mockResolvedValue(undefined),
      };
      Object.assign(navigator, {clipboard: mockClipboard});

      render(<RevealTokenModal {...defaultProps} />);

      // Initially, the status should be empty
      const statusRegion = document.querySelector('[role="status"]');
      expect(statusRegion).toHaveTextContent('');

      // Click copy
      const copyButton = screen.getByTestId('sat-token-copy');
      fireEvent.click(copyButton);

      // Wait for clipboard and state update
      await act(async () => {
        await Promise.resolve();
      });

      // Now it should announce the copy success
      expect(statusRegion).toHaveTextContent(LABELS.COPY_ANNOUNCEMENT);
    });
  });

  describe('warning callout', () => {
    it('has role="alert" on the warning callout', () => {
      render(<RevealTokenModal {...defaultProps} />);

      const callout = screen.getByRole('alert');
      expect(callout).toBeInTheDocument();
    });

    it('contains the warning message in the callout', () => {
      render(<RevealTokenModal {...defaultProps} />);

      const callout = screen.getByRole('alert');
      expect(callout).toHaveTextContent(LABELS.WARNING);
    });
  });

  describe('accessibility', () => {
    it('has Dialog.Title for screen readers', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByText(LABELS.TITLE)).toBeInTheDocument();
    });

    it('has Dialog.Description for screen readers', () => {
      render(<RevealTokenModal {...defaultProps} />);
      // The description carries the same sentence as the visible callout, so the text
      // matches twice. Resolve the dialog's aria-describedby to assert the hidden one.
      const describedBy = screen.getByRole('dialog').getAttribute('aria-describedby');
      expect(describedBy).toBeTruthy();
      expect(document.getElementById(describedBy as string)).toHaveTextContent(LABELS.WARNING);
    });
  });

  describe('Basic authentication credentials', () => {
    it('renders labeled Username field showing the account name', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-basic-username')).toHaveTextContent('jenkins-prod');
    });

    it('renders labeled Password field showing the token', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-basic-password')).toHaveTextContent('sat.test-token-abc123');
    });

    it('still renders the Bearer token field (sat-token-value)', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.getByTestId('sat-token-value')).toBeInTheDocument();
      expect(screen.getByTestId('sat-token-value')).toHaveTextContent('sat.test-token-abc123');
    });

    it('copies the account name when username copy button is clicked', () => {
      const mockClipboard = {writeText: jest.fn().mockResolvedValue(undefined)};
      Object.assign(navigator, {clipboard: mockClipboard});

      render(<RevealTokenModal {...defaultProps} />);
      fireEvent.click(screen.getByTestId('sat-basic-username-copy'));

      expect(mockClipboard.writeText).toHaveBeenCalledWith('jenkins-prod');
    });

    it('copies the token when password copy button is clicked', () => {
      const mockClipboard = {writeText: jest.fn().mockResolvedValue(undefined)};
      Object.assign(navigator, {clipboard: mockClipboard});

      render(<RevealTokenModal {...defaultProps} />);
      fireEvent.click(screen.getByTestId('sat-basic-password-copy'));

      expect(mockClipboard.writeText).toHaveBeenCalledWith('sat.test-token-abc123');
    });

    it('hides the encoded value initially and shows the reveal button', () => {
      render(<RevealTokenModal {...defaultProps} />);
      expect(screen.queryByTestId('sat-basic-encoded-value')).not.toBeInTheDocument();
      expect(screen.getByTestId('sat-basic-encoded-reveal')).toBeInTheDocument();
    });

    it('shows the encoded Authorization header after clicking reveal', () => {
      render(<RevealTokenModal {...defaultProps} />);
      fireEvent.click(screen.getByTestId('sat-basic-encoded-reveal'));

      const encoded = screen.getByTestId('sat-basic-encoded-value');
      expect(encoded).toBeInTheDocument();

      const expectedBase64 = btoa('jenkins-prod:sat.test-token-abc123');
      expect(encoded).toHaveTextContent(`Authorization: Basic ${expectedBase64}`);
      expect(encoded.textContent).toMatch(/^Authorization: Basic /);
    });

    it('AT-074: encoded value is base64 encoded exactly once (not double-encoded)', () => {
      render(<RevealTokenModal {...defaultProps} />);
      fireEvent.click(screen.getByTestId('sat-basic-encoded-reveal'));

      const encoded = screen.getByTestId('sat-basic-encoded-value');
      const text = encoded.textContent ?? '';
      const prefix = 'Authorization: Basic ';
      expect(text.startsWith(prefix)).toBe(true);

      const base64Part = text.slice(prefix.length).trim();
      expect(atob(base64Part)).toBe('jenkins-prod:sat.test-token-abc123');
    });

    it('encodes non-ASCII account names via UTF-8 (bare btoa would throw)', () => {
      const name = 'usér-café-名前';
      // The previous bare btoa(`${name}:${token}`) throws on these characters;
      // safeBtoa round-trips through UTF-8 so the encoded value is non-empty.
      expect(() => btoa(name)).toThrow();

      render(<RevealTokenModal {...defaultProps} name={name} />);
      fireEvent.click(screen.getByTestId('sat-basic-encoded-reveal'));

      const encoded = screen.getByTestId('sat-basic-encoded-value');
      const text = encoded.textContent ?? '';
      const prefix = 'Authorization: Basic ';
      expect(text.startsWith(prefix)).toBe(true);

      const base64Part = text.slice(prefix.length).trim();
      expect(base64Part.length).toBeGreaterThan(0);
      expect(base64Part).toBe(btoa(unescape(encodeURIComponent(`${name}:sat.test-token-abc123`))));
    });

    it('copies the full Authorization header when encoded copy button is clicked', () => {
      const mockClipboard = {writeText: jest.fn().mockResolvedValue(undefined)};
      Object.assign(navigator, {clipboard: mockClipboard});

      render(<RevealTokenModal {...defaultProps} />);
      fireEvent.click(screen.getByTestId('sat-basic-encoded-reveal'));
      fireEvent.click(screen.getByTestId('sat-basic-encoded-copy'));

      const expected = `Authorization: Basic ${btoa('jenkins-prod:sat.test-token-abc123')}`;
      expect(mockClipboard.writeText).toHaveBeenCalledWith(expected);
    });

    it('announces USERNAME_COPY_ANNOUNCEMENT in status region after copying username', async () => {
      const mockClipboard = {writeText: jest.fn().mockResolvedValue(undefined)};
      Object.assign(navigator, {clipboard: mockClipboard});

      render(<RevealTokenModal {...defaultProps} />);
      const statusRegion = document.querySelector('[role="status"]');

      fireEvent.click(screen.getByTestId('sat-basic-username-copy'));
      await act(async () => {
        await Promise.resolve();
      });

      expect(statusRegion).toHaveTextContent(LABELS.USERNAME_COPY_ANNOUNCEMENT);
    });

    it('announces PASSWORD_COPY_ANNOUNCEMENT in status region after copying password', async () => {
      const mockClipboard = {writeText: jest.fn().mockResolvedValue(undefined)};
      Object.assign(navigator, {clipboard: mockClipboard});

      render(<RevealTokenModal {...defaultProps} />);
      const statusRegion = document.querySelector('[role="status"]');

      fireEvent.click(screen.getByTestId('sat-basic-password-copy'));
      await act(async () => {
        await Promise.resolve();
      });

      expect(statusRegion).toHaveTextContent(LABELS.PASSWORD_COPY_ANNOUNCEMENT);
    });

    it('announces ENCODED_COPY_ANNOUNCEMENT in status region after copying encoded value', async () => {
      const mockClipboard = {writeText: jest.fn().mockResolvedValue(undefined)};
      Object.assign(navigator, {clipboard: mockClipboard});

      render(<RevealTokenModal {...defaultProps} />);
      fireEvent.click(screen.getByTestId('sat-basic-encoded-reveal'));

      const statusRegion = document.querySelector('[role="status"]');
      fireEvent.click(screen.getByTestId('sat-basic-encoded-copy'));
      await act(async () => {
        await Promise.resolve();
      });

      expect(statusRegion).toHaveTextContent(LABELS.ENCODED_COPY_ANNOUNCEMENT);
    });

    it('renders the docs link with correct href, text, target, and rel', () => {
      render(<RevealTokenModal {...defaultProps} />);
      const link = screen.getByTestId('sat-docs-link');

      expect(link).toBeInTheDocument();
      expect(link).toHaveAttribute('href', LABELS.DOCS_LINK_HREF);
      expect(link).toHaveTextContent(LABELS.DOCS_LINK);
      expect(link).toHaveAttribute('target', '_blank');
      expect(link.getAttribute('rel')).toContain('noopener');
    });

    it('username copy button has an accessible aria-label containing "username"', () => {
      render(<RevealTokenModal {...defaultProps} />);
      const btn = screen.getByTestId('sat-basic-username-copy');
      const ariaLabel = (btn.getAttribute('aria-label') ?? '').toLowerCase();
      expect(ariaLabel).toContain('username');
    });

    it('password copy button has an accessible aria-label containing "password"', () => {
      render(<RevealTokenModal {...defaultProps} />);
      const btn = screen.getByTestId('sat-basic-password-copy');
      const ariaLabel = (btn.getAttribute('aria-label') ?? '').toLowerCase();
      expect(ariaLabel).toContain('password');
    });
  });
});
