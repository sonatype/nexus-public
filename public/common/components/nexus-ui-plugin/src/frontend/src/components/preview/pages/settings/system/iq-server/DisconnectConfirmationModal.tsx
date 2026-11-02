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

import React, { useRef } from 'react';
import { AlertDialog, Button, Callout, Flex, IconButton } from '@radix-ui/themes';
import { Loader2, TriangleAlert, X } from 'lucide-react';

export interface DisconnectConfirmationModalProps {
  /** Controls modal visibility. */
  open: boolean;
  /** Called when the modal is dismissed without disconnecting (Cancel, X, or Esc). */
  onClose: () => void;
  /** Called when the user confirms the disconnect. */
  onConfirm: () => void;
  /** Shows the pending state on the Disconnect button and locks dismissal while true. */
  loading?: boolean;
}

/**
 * DisconnectConfirmationModal — destructive confirmation shown before disconnecting from
 * IQ Server. Disconnecting stops Repository Firewall from blocking components and halts
 * policy enforcement, so the action is gated behind an explicit confirm rather than firing
 * on the first click.
 *
 * Built on Radix Themes AlertDialog to match the repo's destructive-confirm convention
 * (see form/ConfirmDialog and modals/DeleteConfirmationModal): role="alertdialog", an orange
 * warning callout, and a red solid confirm button.
 */
export function DisconnectConfirmationModal({
  open,
  onClose,
  onConfirm,
  loading = false,
}: DisconnectConfirmationModalProps) {
  const contentRef = useRef<HTMLDivElement>(null);

  return (
    // While the disconnect is in flight, ignore outside dismissal so the request can't be
    // orphaned mid-flight — the buttons and X are disabled to match.
    <AlertDialog.Root open={open} onOpenChange={(next) => { if (!next && !loading) onClose(); }}>
      <AlertDialog.Content
        ref={contentRef}
        maxWidth="480px"
        data-testid="iq-disconnect-modal"
        onOpenAutoFocus={(event) => {
          // Radix moves initial focus to the first action, which makes VoiceOver announce that
          // button and skip the dialog's title and description. Focus the dialog container
          // instead (role="alertdialog" + aria-labelledby=title + aria-describedby=description)
          // so the screen reader announces the alert text on open (NEXUS-53625).
          event.preventDefault();
          contentRef.current?.focus();
        }}
      >
        <Flex align="start" justify="between" gap="3" mb="3">
          <AlertDialog.Title size="5" mb="0">Disconnect from IQ Server?</AlertDialog.Title>
          <IconButton
            variant="ghost"
            color="gray"
            size="1"
            aria-label="Close"
            disabled={loading}
            onClick={onClose}
          >
            <X size={18} aria-hidden="true" />
          </IconButton>
        </Flex>

        <Callout.Root color="orange" variant="soft" size="2" mb="4">
          <Callout.Icon>
            <TriangleAlert size={16} aria-hidden="true" />
          </Callout.Icon>
          <Callout.Text>
            Repository Firewall will stop blocking components, and policy enforcement will stop
            across your repositories.
          </Callout.Text>
        </Callout.Root>

        <AlertDialog.Description size="2" color="gray">
          Nothing is enforced again until IQ Server is reconnected.
        </AlertDialog.Description>

        <Flex align="center" justify="end" gap="3" mt="5">
          <AlertDialog.Cancel>
            <Button
              variant="surface"
              color="gray"
              size="2"
              disabled={loading}
              data-testid="iq-disconnect-modal-cancel"
            >
              Cancel
            </Button>
          </AlertDialog.Cancel>
          <AlertDialog.Action>
            <Button
              variant="solid"
              color="red"
              size="2"
              onClick={onConfirm}
              disabled={loading}
              data-testid="iq-disconnect-modal-confirm"
            >
              {loading && (
                <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} aria-hidden="true" />
              )}
              {loading ? 'Disconnecting…' : 'Disconnect'}
            </Button>
          </AlertDialog.Action>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}

export default DisconnectConfirmationModal;
