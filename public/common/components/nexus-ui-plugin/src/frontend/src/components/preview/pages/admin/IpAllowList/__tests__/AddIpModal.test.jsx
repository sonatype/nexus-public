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

  describe('URI-bracketed IPv6 leniency (NEXUS-54702)', () => {
    it('enables the submit button for bracketed IPv6 input', () => {
      renderModal();
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '[::1]' } });
      expect(screen.getByTestId('add-ip-submit-button')).not.toBeDisabled();
    });

    it('enables the submit button for bracketed IPv6 CIDR input', () => {
      renderModal();
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '[2001:db8::]/32' } });
      expect(screen.getByTestId('add-ip-submit-button')).not.toBeDisabled();
    });

    it('keeps the submit button disabled for half-bracketed input', () => {
      renderModal();
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '[::1' } });
      expect(screen.getByTestId('add-ip-submit-button')).toBeDisabled();
    });

    it('submits the raw bracketed input; canonicalization happens server-side', () => {
      // AddIpModal dispatches trimmedIp to onConfirm without normalizing. The
      // server-side IpAddressValidator normalizes on write, and validateAndNormalize
      // handles the read path. See AddIpModal.jsx:73-76.
      const onConfirm = jest.fn();
      renderModal({ onConfirm });
      fireEvent.change(screen.getByTestId('add-ip-input'), { target: { value: '[::1]' } });
      fireEvent.click(screen.getByTestId('add-ip-submit-button'));
      expect(onConfirm).toHaveBeenCalledWith([{ ipAddress: '[::1]', description: '' }]);
    });

    it('flags bracketed input as duplicate of an already-stored bare entry', () => {
      renderModal({ existingIps: [{ ipAddress: '::1' }] });
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '[::1]' } });
      fireEvent.blur(input);
      expect(
        screen.getByText('This IP address already exists in your allow list')
      ).toBeInTheDocument();
      expect(screen.getByTestId('add-ip-submit-button')).toBeDisabled();
    });

    it('flags bare input as duplicate of an already-stored bracketed entry', () => {
      renderModal({ existingIps: [{ ipAddress: '[::1]' }] });
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '::1' } });
      fireEvent.blur(input);
      expect(
        screen.getByText('This IP address already exists in your allow list')
      ).toBeInTheDocument();
    });
  });

  describe('handleSubmit uses live validation, not stale blur state', () => {
    it('does not call onConfirm when input becomes a duplicate after a prior valid entry (no blur in between)', () => {
      // Regression for PR review finding #1. Sequence:
      //   1. Type a valid, non-duplicate IP (validationError stays '').
      //   2. Type a duplicate WITHOUT blurring first (validationError stays '' — only blur sets it).
      //   3. handleSubmit fires. If the guard used the stale validationError,
      //      it would let a duplicate submit through; the fix runs
      //      validateIpInput(ipInput) live and blocks the submit.
      const onConfirm = jest.fn();
      renderModal({ onConfirm, existingIps: [{ ipAddress: '10.0.0.1' }] });
      const input = screen.getByTestId('add-ip-input');
      fireEvent.change(input, { target: { value: '192.168.1.1' } });
      // No blur — validationError is still ''.
      fireEvent.change(input, { target: { value: '10.0.0.1' } });
      // Attempt submit programmatically (bypasses the disabled-button gate,
      // simulates a future Enter-key handler or any indirect submit path).
      fireEvent.click(screen.getByTestId('add-ip-submit-button'));
      expect(onConfirm).not.toHaveBeenCalled();
    });
  });
});
