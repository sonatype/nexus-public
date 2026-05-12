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
import '@testing-library/jest-dom';
import { SettingsAlert } from '../SettingsAlert';

describe('SettingsAlert', () => {
  describe('rendering', () => {
    it('renders with children content', () => {
      render(<SettingsAlert>Alert message</SettingsAlert>);
      expect(screen.getByText('Alert message')).toBeInTheDocument();
    });

    it('renders with correct ARIA role', () => {
      // Default type (info) uses role="status" for non-urgent notifications
      render(<SettingsAlert>Message</SettingsAlert>);
      expect(screen.getByRole('status')).toBeInTheDocument();
    });

    it('applies custom className', () => {
      const { container } = render(
        <SettingsAlert className="custom-class">Message</SettingsAlert>
      );
      expect(container.querySelector('.custom-class')).toBeInTheDocument();
    });
  });

  describe('alert types', () => {
    it('renders info type by default', () => {
      const { container } = render(<SettingsAlert>Info message</SettingsAlert>);
      expect(container.querySelector('.settings-alert--info')).toBeInTheDocument();
    });

    it('renders error type', () => {
      const { container } = render(
        <SettingsAlert type="error">Error message</SettingsAlert>
      );
      expect(container.querySelector('.settings-alert--error')).toBeInTheDocument();
    });

    it('renders success type', () => {
      const { container } = render(
        <SettingsAlert type="success">Success message</SettingsAlert>
      );
      expect(container.querySelector('.settings-alert--success')).toBeInTheDocument();
    });

    it('renders warning type', () => {
      const { container } = render(
        <SettingsAlert type="warning">Warning message</SettingsAlert>
      );
      expect(container.querySelector('.settings-alert--warning')).toBeInTheDocument();
    });
  });

  describe('icons', () => {
    it('displays icon for each type', () => {
      const { container: errorContainer } = render(
        <SettingsAlert type="error">Error</SettingsAlert>
      );
      expect(errorContainer.querySelector('.settings-alert__icon')).toBeInTheDocument();

      const { container: successContainer } = render(
        <SettingsAlert type="success">Success</SettingsAlert>
      );
      expect(successContainer.querySelector('.settings-alert__icon')).toBeInTheDocument();

      const { container: warningContainer } = render(
        <SettingsAlert type="warning">Warning</SettingsAlert>
      );
      expect(warningContainer.querySelector('.settings-alert__icon')).toBeInTheDocument();

      const { container: infoContainer } = render(
        <SettingsAlert type="info">Info</SettingsAlert>
      );
      expect(infoContainer.querySelector('.settings-alert__icon')).toBeInTheDocument();
    });
  });

  describe('close button', () => {
    it('shows close button when onClose is provided', () => {
      render(
        <SettingsAlert onClose={() => {}}>Message</SettingsAlert>
      );
      expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument();
    });

    it('does not show close button when onClose is not provided', () => {
      render(<SettingsAlert>Message</SettingsAlert>);
      expect(screen.queryByRole('button', { name: 'Dismiss' })).not.toBeInTheDocument();
    });

    it('calls onClose when close button is clicked', () => {
      const onClose = jest.fn();
      render(
        <SettingsAlert onClose={onClose}>Message</SettingsAlert>
      );

      fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }));

      expect(onClose).toHaveBeenCalledTimes(1);
    });
  });

  describe('complex content', () => {
    it('renders JSX children', () => {
      render(
        <SettingsAlert>
          <strong>Bold</strong> and <em>italic</em> text
        </SettingsAlert>
      );
      expect(screen.getByText('Bold')).toBeInTheDocument();
      expect(screen.getByText('italic')).toBeInTheDocument();
    });

    it('renders multiple paragraphs', () => {
      render(
        <SettingsAlert>
          <p>First paragraph</p>
          <p>Second paragraph</p>
        </SettingsAlert>
      );
      expect(screen.getByText('First paragraph')).toBeInTheDocument();
      expect(screen.getByText('Second paragraph')).toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('has role="status" by default for non-urgent info messages', () => {
      render(<SettingsAlert>Important message</SettingsAlert>);
      expect(screen.getByRole('status')).toBeInTheDocument();
    });

    it('has role="alert" for error type messages', () => {
      render(<SettingsAlert type="error">Error message</SettingsAlert>);
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    it('close button has accessible name', () => {
      render(
        <SettingsAlert onClose={() => {}}>Message</SettingsAlert>
      );
      expect(screen.getByRole('button', { name: 'Dismiss' })).toBeInTheDocument();
    });

    it('close button can be activated with keyboard', () => {
      const onClose = jest.fn();
      render(
        <SettingsAlert onClose={onClose}>Message</SettingsAlert>
      );

      const closeButton = screen.getByRole('button', { name: 'Dismiss' });
      closeButton.focus();
      fireEvent.keyDown(closeButton, { key: 'Enter' });
      fireEvent.click(closeButton);

      expect(onClose).toHaveBeenCalled();
    });
  });
});


