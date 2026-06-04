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
import { IpFilteringModeChangeModal } from '../IpFilteringModeChangeModal';

// The modal uses `fromMode` and `toMode` (not currentMode/targetMode).
const renderModal = (props = {}) =>
  render(
    <Theme>
      <IpFilteringModeChangeModal
        isOpen={true}
        onClose={jest.fn()}
        onConfirm={jest.fn()}
        fromMode="disabled"
        toMode="enforce"
        {...props}
      />
    </Theme>
  );

describe('IpFilteringModeChangeModal', () => {
  describe('rendering', () => {
    it('renders a heading for enforce-mode transition', () => {
      renderModal({ fromMode: 'disabled', toMode: 'enforce' });
      expect(screen.getByRole('heading', { name: 'Enable Enforce Mode' })).toBeInTheDocument();
    });

    it('renders a heading for disable transition', () => {
      renderModal({ fromMode: 'monitor', toMode: 'disabled' });
      expect(screen.getByRole('heading', { name: 'Disable IP Filtering?' })).toBeInTheDocument();
    });

    it('renders a heading when switching from enforce to disabled', () => {
      renderModal({ fromMode: 'enforce', toMode: 'disabled' });
      expect(screen.getByRole('heading', { name: 'Disable IP Filtering?' })).toBeInTheDocument();
    });

    it('renders nothing when mode combo has no defined content', () => {
      // e.g. disabled→monitor has no modal content defined
      renderModal({ fromMode: 'disabled', toMode: 'monitor' });
      expect(screen.queryByTestId('mode-change-modal')).not.toBeInTheDocument();
    });
  });

  describe('enforce mode warning', () => {
    it('shows self-lockout warning when current user IP is not allowed', () => {
      renderModal({
        fromMode: 'disabled',
        toMode: 'enforce',
        currentUserIp: '1.2.3.4',
        isCurrentUserIpAllowed: false,
      });
      expect(screen.getByText(/not in the allow list/i)).toBeInTheDocument();
    });

    it('shows standard enforce warning when current user IP is allowed', () => {
      renderModal({
        fromMode: 'disabled',
        toMode: 'enforce',
        currentUserIp: '1.2.3.4',
        isCurrentUserIpAllowed: true,
      });
      expect(screen.getByText(/block all requests/i)).toBeInTheDocument();
    });
  });

  describe('actions', () => {
    it('calls onConfirm when confirm button is clicked', () => {
      const onConfirm = jest.fn();
      renderModal({ onConfirm });
      fireEvent.click(screen.getByTestId('mode-change-confirm-button'));
      expect(onConfirm).toHaveBeenCalled();
    });

    it('calls onClose when cancel button is clicked', () => {
      const onClose = jest.fn();
      renderModal({ onClose });
      fireEvent.click(screen.getByTestId('mode-change-cancel-button'));
      expect(onClose).toHaveBeenCalled();
    });

    it('disables both buttons while loading', () => {
      renderModal({ isLoading: true });
      expect(screen.getByTestId('mode-change-confirm-button')).toBeDisabled();
      expect(screen.getByTestId('mode-change-cancel-button')).toBeDisabled();
    });
  });
});
