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
import { AddIpModal } from '../AddIpModal';

const renderModal = (props = {}) =>
  render(
    <Theme>
      <AddIpModal isOpen={true} onClose={jest.fn()} onConfirm={jest.fn()} {...props} />
    </Theme>
  );

describe('AddIpModal', () => {
  describe('initial state', () => {
    it('renders dialog title heading', () => {
      renderModal();
      expect(screen.getByRole('heading', { name: 'Add IP Address' })).toBeInTheDocument();
    });

    it('submit button is disabled when input is empty', () => {
      renderModal();
      expect(screen.getByTestId('add-ip-submit-button')).toBeDisabled();
    });

    it('input field is initially empty', () => {
      renderModal();
      expect(screen.getByTestId('add-ip-input')).toHaveValue('');
    });
  });

  describe('validation', () => {
    it('submit button remains disabled for invalid IP format', () => {
      renderModal();
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: 'not-an-ip' } });
      expect(screen.getByTestId('add-ip-submit-button')).toBeDisabled();
    });

    it('shows inline validation error on blur with invalid IP', () => {
      renderModal();
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: 'not-an-ip' } });
      fireEvent.blur(input);
      expect(screen.getByText('Invalid IP address or CIDR notation')).toBeInTheDocument();
    });

    it('shows duplicate error when IP already exists', () => {
      renderModal({ existingIps: [{ ipAddress: '192.168.1.1' }] });
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '192.168.1.1' } });
      fireEvent.blur(input);
      expect(screen.getByText('This IP address already exists in your allow list')).toBeInTheDocument();
    });

    it('submit button is enabled for a valid IPv4 address', () => {
      renderModal();
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '192.168.1.1' } });
      expect(screen.getByTestId('add-ip-submit-button')).not.toBeDisabled();
    });

    it('submit button is enabled for a valid CIDR block', () => {
      renderModal();
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '10.0.0.0/24' } });
      expect(screen.getByTestId('add-ip-submit-button')).not.toBeDisabled();
    });

    it('submit button is enabled for a valid IPv6 address', () => {
      renderModal();
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '2001:db8::1' } });
      expect(screen.getByTestId('add-ip-submit-button')).not.toBeDisabled();
    });
  });

  describe('submission', () => {
    it('calls onConfirm with ipAddress and description on submit', () => {
      const onConfirm = jest.fn();
      renderModal({ onConfirm });

      fireEvent.change(screen.getByTestId('add-ip-input'), { target: { value: '192.168.1.1' } });
      fireEvent.change(screen.getByTestId('add-description-input'), {
        target: { value: 'Office' },
      });
      fireEvent.click(screen.getByTestId('add-ip-submit-button'));

      expect(onConfirm).toHaveBeenCalledWith([{ ipAddress: '192.168.1.1', description: 'Office' }]);
    });

    it('trims whitespace from IP address before calling onConfirm', () => {
      const onConfirm = jest.fn();
      renderModal({ onConfirm });

      fireEvent.change(screen.getByTestId('add-ip-input'), {
        target: { value: '  192.168.1.1  ' },
      });
      fireEvent.click(screen.getByTestId('add-ip-submit-button'));

      expect(onConfirm).toHaveBeenCalledWith([
        expect.objectContaining({ ipAddress: '192.168.1.1' }),
      ]);
    });

    it('calls onClose when cancel button is clicked', () => {
      const onClose = jest.fn();
      renderModal({ onClose });
      fireEvent.click(screen.getByTestId('add-ip-cancel-button'));
      expect(onClose).toHaveBeenCalled();
    });
  });
});
