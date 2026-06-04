/*
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */

import React from 'react';
import { Flex, Button, Dialog, Spinner, IconButton } from '@radix-ui/themes';
import { X } from 'lucide-react';

/**
 * Confirmation modal for IP filtering mode changes.
 * Shows different warnings based on the mode transition:
 * - Disabling: Warns that security feature is being turned off
 * - Enabling Enforce: Warns about potential blocking, checks for self-lockout
 */
export function IpFilteringModeChangeModal({
  isOpen,
  onClose,
  onConfirm,
  fromMode,
  toMode,
  currentUserIp,
  isCurrentUserIpAllowed = false,
  isLoading = false,
}) {
  const getModalContent = () => {
    // Disabling IP filtering (from Monitor or Enforce to Disabled)
    if (toMode === 'disabled' && (fromMode === 'monitor' || fromMode === 'enforce')) {
      return {
        title: 'Disable IP Filtering?',
        message:
          'Disabling IP filtering will remove all IP-based access restrictions. All requests will be allowed regardless of source IP address.',
        severity: 'warning',
        confirmLabel: 'Disable IP Filtering',
        confirmColor: 'orange',
        showWarningIcon: false,
      };
    }

    // Enabling Enforce mode
    if (toMode === 'enforce') {
      // Use the server-provided allowed status from /current-ip endpoint
      const isAdminIpInList = isCurrentUserIpAllowed;

      // Self-lockout warning if admin IP not in allow list
      if (!isAdminIpInList) {
        return {
          title: 'Enable Enforce Mode',
          message: `Your current IP address (${
            currentUserIp || 'unknown'
          }) is not in the allow list.

Enabling Enforce mode will deny requests from non-matching IP addresses, including your own. You may lose access to Nexus Repository.`,
          severity: 'error',
          confirmLabel: 'Enable Enforce Mode',
          confirmColor: 'red',
          showWarningIcon: true,
        };
      }

      // Standard enforce mode confirmation
      return {
        title: 'Enable Enforce Mode',
        message:
          'Enabling Enforce mode will immediately block all requests from IP addresses not in your allow list. Make sure your IP list is complete before proceeding.',
        severity: 'warning',
        confirmLabel: 'Enable Enforce Mode',
        confirmColor: 'orange',
        showWarningIcon: false,
      };
    }

    return null;
  };

  const content = getModalContent();
  if (!content) return null;

  return (
    <Dialog.Root open={isOpen} onOpenChange={onClose}>
      <Dialog.Content maxWidth="400px" data-testid="mode-change-modal">
        <Flex align="start" justify="between" mb="3">
          <Dialog.Title size="5">{content.title}</Dialog.Title>
          <Dialog.Close>
            <IconButton variant="ghost" size="1">
              <X size={18} />
            </IconButton>
          </Dialog.Close>
        </Flex>

        <Dialog.Description size="3" color="gray" style={{ marginBottom: '16px' }}>
          {content.message}
        </Dialog.Description>

        <Flex gap="3" justify="end">
          <Dialog.Close>
            <Button variant="surface" onClick={onClose} disabled={isLoading} data-testid="mode-change-cancel-button">
              Cancel
            </Button>
          </Dialog.Close>
          <Button variant="solid" color={content.confirmColor} onClick={onConfirm} disabled={isLoading} data-testid="mode-change-confirm-button">
            {isLoading && <Spinner size="1" style={{ marginRight: '8px' }} />}
            {isLoading ? 'Saving...' : content.confirmLabel}
          </Button>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
