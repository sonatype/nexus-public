/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { EditIpModal } from '../EditIpModal';

const SAMPLE_ENTRY = {
  id: 'abc-123',
  ipAddress: '192.168.1.1',
  description: 'Office network',
};

const renderModal = (props = {}) =>
  render(
    <Theme>
      <EditIpModal
        isOpen={true}
        onClose={jest.fn()}
        onConfirm={jest.fn()}
        ipEntry={SAMPLE_ENTRY}
        {...props}
      />
    </Theme>
  );

describe('EditIpModal', () => {
  describe('initial state', () => {
    it('renders dialog title', () => {
      renderModal();
      expect(screen.getByText('Edit IP Address')).toBeInTheDocument();
    });

    it('pre-populates IP address field from ipEntry prop', () => {
      renderModal();
      expect(screen.getByTestId('edit-ip-input')).toHaveValue('192.168.1.1');
    });

    it('pre-populates description field from ipEntry prop', () => {
      renderModal();
      expect(screen.getByTestId('edit-description-input')).toHaveValue('Office network');
    });

    it('is not rendered when ipEntry is null', () => {
      render(
        <Theme>
          <EditIpModal isOpen={true} onClose={jest.fn()} onConfirm={jest.fn()} ipEntry={null} />
        </Theme>
      );
      expect(screen.queryByTestId('edit-ip-modal')).not.toBeInTheDocument();
    });
  });

  describe('validation', () => {
    it('shows inline error when IP is changed to an invalid value', () => {
      renderModal();
      const input = screen.getByTestId('edit-ip-input');
      fireEvent.change(input, { target: { value: 'not-valid' } });
      expect(
        screen.getByText('Invalid IP address format. Use 192.168.1.1, 10.0.0.0/24, 2001:db8::1, or 2001:db8::/32')
      ).toBeInTheDocument();
    });

    it('submit button is disabled when IP is invalid', () => {
      renderModal();
      fireEvent.change(screen.getByTestId('edit-ip-input'), { target: { value: 'bad' } });
      expect(screen.getByTestId('edit-ip-submit-button')).toBeDisabled();
    });

    it('submit button is enabled when IP is valid', () => {
      renderModal();
      expect(screen.getByTestId('edit-ip-submit-button')).not.toBeDisabled();
    });
  });

  describe('submission', () => {
    it('calls onConfirm with updated ipAddress and description', () => {
      const onConfirm = jest.fn();
      renderModal({ onConfirm });

      const ipInput = screen.getByTestId('edit-ip-input');
      fireEvent.change(ipInput, { target: { value: '10.0.0.1' } });

      const descInput = screen.getByTestId('edit-description-input');
      fireEvent.change(descInput, { target: { value: 'Updated description' } });

      fireEvent.click(screen.getByTestId('edit-ip-submit-button'));

      expect(onConfirm).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'abc-123',
          ipAddress: '10.0.0.1',
          description: 'Updated description',
        })
      );
    });

    it('does not include lastUpdated in the onConfirm payload', () => {
      const onConfirm = jest.fn();
      renderModal({ onConfirm });

      fireEvent.click(screen.getByTestId('edit-ip-submit-button'));

      const payload = onConfirm.mock.calls[0][0];
      expect(payload).not.toHaveProperty('lastUpdated');
    });

    it('calls onClose when cancel button is clicked', () => {
      const onClose = jest.fn();
      renderModal({ onClose });
      fireEvent.click(screen.getByTestId('edit-ip-cancel-button'));
      expect(onClose).toHaveBeenCalled();
    });
  });
});
