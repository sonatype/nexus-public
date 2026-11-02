/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { ServerDisconnectedModal } from '../ServerDisconnectedModal';

const RETRY_BUTTON_NAME = /retry|reconnecting/i;
const CUSTOM_MESSAGE = 'Custom disconnect message for testing.';
const DEFAULT_MESSAGE =
  'There is a problem communicating with the server. Retry to reconnect.';

describe('ServerDisconnectedModal', () => {
  describe('Rendering', () => {
    it('renders the modal when isOpen is true', () => {
      render(<ServerDisconnectedModal isOpen={true} onRetry={jest.fn()} />);
      expect(
        screen.getByTestId('server-disconnected-modal'),
      ).toBeInTheDocument();
    });

    it('does not render the modal when isOpen is false', () => {
      render(<ServerDisconnectedModal isOpen={false} onRetry={jest.fn()} />);
      expect(
        screen.queryByTestId('server-disconnected-modal'),
      ).not.toBeInTheDocument();
    });

    it('shows the title and connection-lost subheading', () => {
      render(<ServerDisconnectedModal isOpen={true} onRetry={jest.fn()} />);
      expect(screen.getByText('Server Disconnected')).toBeInTheDocument();
      expect(screen.getByText('Connection Lost')).toBeInTheDocument();
    });

    it('shows the default message when none is provided', () => {
      render(<ServerDisconnectedModal isOpen={true} onRetry={jest.fn()} />);
      expect(screen.getByText(DEFAULT_MESSAGE)).toBeInTheDocument();
    });

    it('shows a custom message when provided', () => {
      render(
        <ServerDisconnectedModal
          isOpen={true}
          onRetry={jest.fn()}
          message={CUSTOM_MESSAGE}
        />,
      );
      expect(screen.getByText(CUSTOM_MESSAGE)).toBeInTheDocument();
    });
  });

  describe('Actions', () => {
    it('calls onRetry when the retry button is clicked', () => {
      const onRetry = jest.fn();
      render(<ServerDisconnectedModal isOpen={true} onRetry={onRetry} />);

      fireEvent.click(screen.getByTestId('server-disconnected-retry-button'));

      expect(onRetry).toHaveBeenCalledTimes(1);
    });

    it('labels the button "Retry" when not retrying', () => {
      render(<ServerDisconnectedModal isOpen={true} onRetry={jest.fn()} />);
      expect(
        screen.getByRole('button', { name: RETRY_BUTTON_NAME }),
      ).toHaveTextContent('Retry');
    });
  });

  describe('Loading State', () => {
    it('labels the button "Reconnecting..." and disables it while retrying', () => {
      render(
        <ServerDisconnectedModal
          isOpen={true}
          onRetry={jest.fn()}
          isRetrying={true}
        />,
      );

      const button = screen.getByTestId('server-disconnected-retry-button');
      expect(button).toHaveTextContent('Reconnecting...');
      expect(button).toBeDisabled();
    });

    it('does not call onRetry when the disabled button is clicked', () => {
      const onRetry = jest.fn();
      render(
        <ServerDisconnectedModal
          isOpen={true}
          onRetry={onRetry}
          isRetrying={true}
        />,
      );

      fireEvent.click(screen.getByTestId('server-disconnected-retry-button'));

      expect(onRetry).not.toHaveBeenCalled();
    });
  });

  describe('Dismissable behaviour', () => {
    it('exposes an accessible dialog title so screen readers announce it', () => {
      render(<ServerDisconnectedModal isOpen={true} onRetry={jest.fn()} />);
      expect(
        screen.getByRole('dialog', { name: /server disconnected/i }),
      ).toBeInTheDocument();
    });

    it('calls onDismiss when Escape is pressed', () => {
      const onDismiss = jest.fn();
      render(
        <ServerDisconnectedModal
          isOpen={true}
          onRetry={jest.fn()}
          onDismiss={onDismiss}
        />,
      );

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(onDismiss).toHaveBeenCalledTimes(1);
    });

    it('does not throw on Escape when no onDismiss is provided', () => {
      render(<ServerDisconnectedModal isOpen={true} onRetry={jest.fn()} />);

      expect(() =>
        fireEvent.keyDown(document, { key: 'Escape' }),
      ).not.toThrow();
    });
  });
});
