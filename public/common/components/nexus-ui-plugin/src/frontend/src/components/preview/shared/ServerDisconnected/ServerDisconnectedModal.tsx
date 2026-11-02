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
import { Dialog, Flex, Text, Button, Box } from '@radix-ui/themes';
import { WifiOff, RefreshCw } from 'lucide-react';

export interface ServerDisconnectedModalProps {
  /** Whether the modal is open */
  isOpen: boolean;
  /** Callback to retry the server connection */
  onRetry: () => void;
  /** Callback to dismiss the modal (Escape or click outside) */
  onDismiss?: () => void;
  /** Whether a reconnect attempt is in progress */
  isRetrying?: boolean;
  /** Optional message to display */
  message?: string;
}

const DEFAULT_MESSAGE =
  'There is a problem communicating with the server. Retry to reconnect.';

/**
 * ServerDisconnectedModal - Preview UI modal shown when the server connection is lost.
 *
 * Replaces the legacy ExtJS NX.Dialogs.showError dialog for Preview UI users.
 * Disconnection is recoverable, so the modal is dismissable (Escape or a click
 * outside call onDismiss), matching the legacy dialog, which closed on Retry rather
 * than blocking until reconnect. It reopens if a later poll fails and auto-closes
 * when the connection recovers.
 */
export function ServerDisconnectedModal({
  isOpen,
  onRetry,
  onDismiss,
  isRetrying = false,
  message = DEFAULT_MESSAGE,
}: ServerDisconnectedModalProps): JSX.Element {
  return (
    <Dialog.Root
      open={isOpen}
      onOpenChange={(open) => {
        if (!open) {
          onDismiss?.();
        }
      }}
    >
      <Dialog.Content
        maxWidth="450px"
        data-testid="server-disconnected-modal"
      >
        <Flex direction="column" gap="4">
          {/* Header */}
          <Flex align="center" gap="3">
            <Box
              style={{
                background: 'var(--red-3)',
                borderRadius: 'var(--radius-3)',
                padding: 'var(--space-3)',
              }}
            >
              <WifiOff size={28} color="var(--red-9)" />
            </Box>
            <Box>
              <Dialog.Title size="4" mb="0">
                Server Disconnected
              </Dialog.Title>
              <Text size="2" color="gray">
                Connection Lost
              </Text>
            </Box>
          </Flex>

          {/* Message */}
          <Dialog.Description size="2" color="gray">
            {message}
          </Dialog.Description>

          {/* Actions */}
          <Flex gap="3" justify="end" mt="2">
            <Button
              variant="solid"
              color="red"
              onClick={onRetry}
              disabled={isRetrying}
              data-testid="server-disconnected-retry-button"
            >
              <RefreshCw size={16} />
              {isRetrying ? 'Reconnecting...' : 'Retry'}
            </Button>
          </Flex>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}

export default ServerDisconnectedModal;
