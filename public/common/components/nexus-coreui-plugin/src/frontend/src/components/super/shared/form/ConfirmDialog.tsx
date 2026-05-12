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
import { AlertDialog, Button, Flex, Text, Box, Callout } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';

export interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  message: React.ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'warning';
  onConfirm: () => void;
  loading?: boolean;
  children?: React.ReactNode;
  /** Optional entity name to display in a callout (for mid-risk deletions). Can be a string or React.ReactNode for rich content. */
  entityName?: React.ReactNode;
  /** Base testId for the dialog; used to generate cancelTestId/confirmTestId if not provided */
  testId?: string;
  /** testId for the Cancel button */
  cancelTestId?: string;
  /** testId for the Confirm button */
  confirmTestId?: string;
}

/**
 * ConfirmDialog - Reusable confirmation dialog using Radix Themes AlertDialog.
 *
 * Supports simple confirm/cancel flows with optional loading state
 * and extra content below the description.
 */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'danger',
  onConfirm,
  loading = false,
  children,
  entityName,
  testId,
  cancelTestId,
  confirmTestId,
}: ConfirmDialogProps) {
  const handleConfirm = () => {
    onConfirm();
    if (!loading) {
      onOpenChange(false);
    }
  };

  const confirmColor = variant === 'danger' ? 'red' : 'orange';

  return (
    <AlertDialog.Root open={open} onOpenChange={loading ? undefined : onOpenChange}>
      <AlertDialog.Content maxWidth="450px">
        <AlertDialog.Title>{title}</AlertDialog.Title>

        <AlertDialog.Description size="2">
          {message}
        </AlertDialog.Description>

        {entityName && (
          <Box mt="3">
            <Callout.Root color={confirmColor}>
              <Callout.Text>
                {typeof entityName === 'string' ? (
                  <Text size="2" weight="medium">
                    {entityName}
                  </Text>
                ) : (
                  entityName
                )}
              </Callout.Text>
            </Callout.Root>
          </Box>
        )}

        {children}

        <Flex gap="3" mt="4" justify="end">
          <AlertDialog.Cancel>
            <Button
              variant="surface"
              color="gray"
              size="2"
              disabled={loading}
              data-testid={cancelTestId ?? (testId ? `${testId}-cancel` : undefined)}
            >
              {cancelLabel}
            </Button>
          </AlertDialog.Cancel>
          <AlertDialog.Action>
            <Button
              variant="solid"
              color={confirmColor}
              size="2"
              onClick={handleConfirm}
              disabled={loading}
              data-testid={confirmTestId ?? (testId ? `${testId}-confirm` : undefined)}
            >
              {loading && <Loader2 size={16} style={{ animation: 'spin 1s linear infinite' }} />}
              {confirmLabel}
            </Button>
          </AlertDialog.Action>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}

export default ConfirmDialog;
