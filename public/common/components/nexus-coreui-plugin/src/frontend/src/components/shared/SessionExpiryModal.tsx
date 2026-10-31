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
import { Dialog, Flex, Text, Button, Box, Heading } from '@radix-ui/themes';
import { AlertCircle, LogIn, RefreshCw } from 'lucide-react';
import { useRouter } from '@uirouter/react';

import './SessionExpiryModal.scss';

export interface SessionExpiryModalProps {
  /** Whether the modal is open */
  isOpen: boolean;
  /** Callback when modal is closed */
  onClose: () => void;
  /** Optional message to display */
  message?: string;
}

/**
 * SessionExpiryModal - User-friendly modal for session expiration handling.
 *
 * Features:
 * - Clear messaging about session expiration
 * - Options to re-authenticate or refresh
 * - Modern Radix UI design matching Preview UI
 */
export function SessionExpiryModal({
  isOpen,
  onClose,
  message = 'Your session has expired. Please sign in again to continue.',
}: SessionExpiryModalProps): JSX.Element {
  const router = useRouter();

  const handleSignIn = () => {
    // Get current URL
    const currentUrl = window.location.hash;

    // Don't return to test hub or other internal routes after login
    // These routes require debug mode and special localStorage flags
    const isInternalRoute = currentUrl?.includes('/test') ||
                            currentUrl?.includes('preview.test');

    // Store URL for redirect after login (but not for internal routes)
    if (currentUrl && !isInternalRoute) {
      sessionStorage.setItem('nexus:returnTo', currentUrl);
    } else {
      // Clear any previous return path for internal routes
      sessionStorage.removeItem('nexus:returnTo');
    }

    // Redirect to login page
    // For internal routes, return to welcome page after login
    const returnTo = isInternalRoute ? btoa('#browse/welcome') : btoa(currentUrl || '#/');
    router.stateService.go('login', { returnTo });
    onClose();
  };

  const handleRefresh = () => {
    // Attempt to refresh the page - this may redirect to login if session is truly expired
    window.location.reload();
  };

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <Dialog.Content
        maxWidth="450px"
        data-testid="session-expiry-modal"
      >
        <Flex direction="column" gap="4">
          {/* Header */}
          <Flex align="center" gap="3">
            <Box className="nxrm-session-expiry-modal__icon-tile">
              <AlertCircle size={28} color="var(--orange-9)" />
            </Box>
            <Box>
              <Heading size="4">Session Expired</Heading>
              <Text size="2" color="gray">
                Authentication Required
              </Text>
            </Box>
          </Flex>

          {/* Message */}
          <Text size="2" color="gray">
            {message}
          </Text>

          {/* Actions */}
          <Flex gap="3" justify="end" mt="2">
            <Button variant="soft" color="gray" onClick={handleRefresh}>
              <RefreshCw size={16} />
              Refresh Page
            </Button>
            <Button variant="solid" color="orange" onClick={handleSignIn}>
              <LogIn size={16} />
              Sign In
            </Button>
          </Flex>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}

export default SessionExpiryModal;
