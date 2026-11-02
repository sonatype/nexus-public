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
import { RefreshCw } from 'lucide-react';

export interface ServerRestartModalProps {
  /** Whether the modal is open */
  isOpen: boolean;
  /** Callback to reload the application */
  onReload: () => void;
  /** Whether the reload is in progress */
  isReloading?: boolean;
  /** Optional message to display */
  message?: string;
}

const DEFAULT_MESSAGE =
  'The server has been restarted. Reload the application to continue.';

/**
 * ServerRestartModal - Preview UI modal shown when the server restarts.
 *
 * Replaces the legacy ExtJS NX.Dialogs.showInfo dialog for Preview UI users.
 * The reload is mandatory (the running app is stale), so the modal is not
 * dismissable: it stays open until the user reloads.
 */
export function ServerRestartModal({
  isOpen,
  onReload,
  isReloading = false,
  message = DEFAULT_MESSAGE,
}: ServerRestartModalProps): JSX.Element {
  return (
    <Dialog.Root open={isOpen}>
      <Dialog.Content
        maxWidth="450px"
        data-testid="server-restart-modal"
        onEscapeKeyDown={(event) => event.preventDefault()}
        onPointerDownOutside={(event) => event.preventDefault()}
        onInteractOutside={(event) => event.preventDefault()}
      >
        <Flex direction="column" gap="4">
          {/* Header */}
          <Flex align="center" gap="3">
            <Box
              style={{
                background: 'var(--blue-3)',
                borderRadius: 'var(--radius-3)',
                padding: 'var(--space-3)',
              }}
            >
              <RefreshCw size={28} color="var(--blue-9)" />
            </Box>
            <Box>
              <Dialog.Title size="4" mb="0">
                Server Restarted
              </Dialog.Title>
              <Text size="2" color="gray">
                Reload Required
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
              color="blue"
              onClick={onReload}
              disabled={isReloading}
              data-testid="server-restart-reload-button"
            >
              <RefreshCw size={16} />
              {isReloading ? 'Reloading...' : 'Reload now'}
            </Button>
          </Flex>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}

export default ServerRestartModal;
