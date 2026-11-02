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
import { render, screen, fireEvent, cleanup } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { DisconnectConfirmationModal } from '../DisconnectConfirmationModal';

function renderModal(props: Record<string, any> = {}) {
  return render(
    <Theme>
      <DisconnectConfirmationModal
        open
        onClose={jest.fn()}
        onConfirm={jest.fn()}
        {...props}
      />
    </Theme>
  );
}

afterEach(() => {
  cleanup();
  Array.from(document.body.children).forEach(el => el.remove());
});

describe('DisconnectConfirmationModal', () => {
  it('does not render when closed', () => {
    renderModal({ open: false });
    expect(screen.queryByTestId('iq-disconnect-modal')).not.toBeInTheDocument();
  });

  it('renders the title, warning callout, and follow-up copy when open', () => {
    renderModal();
    expect(screen.getByTestId('iq-disconnect-modal')).toBeInTheDocument();
    expect(screen.getByText('Disconnect from IQ Server?')).toBeInTheDocument();
    expect(
      screen.getByText(/Repository Firewall will stop blocking components/)
    ).toBeInTheDocument();
    expect(
      screen.getByText('Nothing is enforced again until IQ Server is reconnected.')
    ).toBeInTheDocument();
  });

  it('calls onConfirm when the Disconnect button is clicked', () => {
    const onConfirm = jest.fn();
    renderModal({ onConfirm });
    fireEvent.click(screen.getByTestId('iq-disconnect-modal-confirm'));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when Cancel is clicked', () => {
    const onClose = jest.fn();
    renderModal({ onClose });
    fireEvent.click(screen.getByTestId('iq-disconnect-modal-cancel'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when the X button is clicked', () => {
    const onClose = jest.fn();
    renderModal({ onClose });
    fireEvent.click(screen.getByLabelText('Close'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('shows the pending state and disables the actions while loading', () => {
    const onClose = jest.fn();
    renderModal({ loading: true, onClose });

    expect(screen.getByText('Disconnecting…')).toBeInTheDocument();
    expect(screen.getByTestId('iq-disconnect-modal-confirm')).toBeDisabled();
    expect(screen.getByTestId('iq-disconnect-modal-cancel')).toBeDisabled();

    // The X is disabled too, so a mid-flight disconnect can't be orphaned.
    fireEvent.click(screen.getByLabelText('Close'));
    expect(onClose).not.toHaveBeenCalled();
  });
});
