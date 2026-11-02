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
import { ServerRestartModal } from '../ServerRestartModal';

const RELOAD_BUTTON_NAME = /reload/i;
const CUSTOM_MESSAGE = 'Custom restart message for testing.';
const DEFAULT_MESSAGE =
  'The server has been restarted. Reload the application to continue.';

describe('ServerRestartModal', () => {
  describe('Rendering', () => {
    it('renders the modal when isOpen is true', () => {
      render(<ServerRestartModal isOpen={true} onReload={jest.fn()} />);
      expect(screen.getByTestId('server-restart-modal')).toBeInTheDocument();
    });

    it('does not render the modal when isOpen is false', () => {
      render(<ServerRestartModal isOpen={false} onReload={jest.fn()} />);
      expect(screen.queryByTestId('server-restart-modal')).not.toBeInTheDocument();
    });

    it('shows the title and reload-required subheading', () => {
      render(<ServerRestartModal isOpen={true} onReload={jest.fn()} />);
      expect(screen.getByText('Server Restarted')).toBeInTheDocument();
      expect(screen.getByText('Reload Required')).toBeInTheDocument();
    });

    it('shows the default message when none is provided', () => {
      render(<ServerRestartModal isOpen={true} onReload={jest.fn()} />);
      expect(screen.getByText(DEFAULT_MESSAGE)).toBeInTheDocument();
    });

    it('shows a custom message when provided', () => {
      render(
        <ServerRestartModal
          isOpen={true}
          onReload={jest.fn()}
          message={CUSTOM_MESSAGE}
        />,
      );
      expect(screen.getByText(CUSTOM_MESSAGE)).toBeInTheDocument();
    });
  });

  describe('Actions', () => {
    it('calls onReload when the reload button is clicked', () => {
      const onReload = jest.fn();
      render(<ServerRestartModal isOpen={true} onReload={onReload} />);

      fireEvent.click(screen.getByTestId('server-restart-reload-button'));

      expect(onReload).toHaveBeenCalledTimes(1);
    });

    it('labels the button "Reload now" when not reloading', () => {
      render(<ServerRestartModal isOpen={true} onReload={jest.fn()} />);
      expect(
        screen.getByRole('button', { name: RELOAD_BUTTON_NAME }),
      ).toHaveTextContent('Reload now');
    });
  });

  describe('Loading State', () => {
    it('labels the button "Reloading..." and disables it while reloading', () => {
      render(
        <ServerRestartModal isOpen={true} onReload={jest.fn()} isReloading={true} />,
      );

      const button = screen.getByTestId('server-restart-reload-button');
      expect(button).toHaveTextContent('Reloading...');
      expect(button).toBeDisabled();
    });

    it('does not call onReload when the disabled button is clicked', () => {
      const onReload = jest.fn();
      render(
        <ServerRestartModal isOpen={true} onReload={onReload} isReloading={true} />,
      );

      fireEvent.click(screen.getByTestId('server-restart-reload-button'));

      expect(onReload).not.toHaveBeenCalled();
    });
  });

  describe('Non-dismissable behaviour', () => {
    it('exposes an accessible dialog title so screen readers announce it', () => {
      render(<ServerRestartModal isOpen={true} onReload={jest.fn()} />);
      expect(
        screen.getByRole('dialog', { name: /server restarted/i }),
      ).toBeInTheDocument();
    });

    it('stays open when Escape is pressed', () => {
      render(<ServerRestartModal isOpen={true} onReload={jest.fn()} />);

      fireEvent.keyDown(document, { key: 'Escape' });

      expect(screen.getByTestId('server-restart-modal')).toBeInTheDocument();
    });

    it('stays open when a pointer press lands outside the modal', () => {
      render(<ServerRestartModal isOpen={true} onReload={jest.fn()} />);

      fireEvent.pointerDown(document.body);

      expect(screen.getByTestId('server-restart-modal')).toBeInTheDocument();
    });
  });
});
