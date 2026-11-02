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

import React, { useState, useRef, useEffect } from 'react';
import { AlertDialog, Box, Flex, Text, Button, TextField, IconButton } from '@radix-ui/themes';
import { X } from 'lucide-react';

// Default acknowledgement word required to confirm any destructive deletion. Using a
// single literal word - instead of per-entity names - keeps the friction predictable
// and short to type while still forcing a deliberate action. Displayed as-is; the
// match is case-insensitive so "delete", "Delete", "DELETE" all confirm. Callers that
// need to verify against a specific identifier (e.g. a username) override this via the
// requiredConfirmationText prop, in which case the match becomes case-sensitive.
const CONFIRMATION_TEXT = 'Delete';

export interface DeleteConfirmationModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  entityName?: string;
  entityType: string;
  loading?: boolean;
  /** When true, the entity is moved to Trash (recoverable) instead of being permanently
   *  deleted, so the copy reflects a recoverable trash. Callers pass this only when a
   *  trash will actually occur (e.g. trash enabled on a hosted repository). */
  trash?: boolean;
  /** Optional retention window (days) surfaced in the trash copy ("recoverable for N days").
   *  Only used when {@link trash} is true; omitted → a generic recoverable message is shown. */
  retentionDays?: number;
  /** Optional extra content (e.g. resource details) rendered above the
   *  acknowledgement input. Lets callers surface context like task type or
   *  schedule without forking the modal. */
  children?: React.ReactNode;
  /** Optional override for the acknowledgement token the user must type to confirm.
   *  Defaults to the shared {@link CONFIRMATION_TEXT} literal ("Delete"), keeping every
   *  other entity's modal unchanged. When supplied (e.g. a username), the match becomes
   *  case-sensitive so a lookalike-cased identifier does not confirm against the wrong
   *  target. The value is also what the prompt/placeholder/highlighted word render. */
  requiredConfirmationText?: string;
}

/**
 * Reusable delete confirmation modal with text verification.
 *
 * Follows nexusone design system standards:
 * - Modal width: 450px (destructive confirmation)
 * - Requires typing "Delete" (case-insensitive) by default for all deletions
 * - Callers may override the token via requiredConfirmationText (matched case-sensitively)
 * - Entity name is displayed in the description but not required for verification
 *
 * @param open - Controls modal visibility
 * @param onClose - Called when modal should close (cancel, X button)
 * @param onConfirm - Called when user confirms deletion with valid text
 * @param entityName - If provided, displayed in description for context
 * @param entityType - Display name for the entity type (e.g., "repository", "user", "blob store")
 * @param loading - Shows loading state during delete operation
 * @param requiredConfirmationText - Optional token to type instead of "Delete"; case-sensitive when set
 */
export function DeleteConfirmationModal({
  open,
  onClose,
  onConfirm,
  entityName,
  entityType,
  loading = false,
  trash = false,
  retentionDays,
  children,
  requiredConfirmationText,
}: DeleteConfirmationModalProps) {
  const [confirmationText, setConfirmationText] = useState('');
  const [showError, setShowError] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  // Token to type: defaults to CONFIRMATION_TEXT (case-insensitive). A non-blank caller override (e.g. a username) is matched case-sensitively; a blank override is ignored so an empty token can never auto-satisfy the gate.
  const override = requiredConfirmationText?.trim() ? requiredConfirmationText : undefined;
  const expected = override ?? CONFIRMATION_TEXT;
  const caseSensitive = override !== undefined;
  const normalize = (value: string) => (caseSensitive ? value : value.toLowerCase());
  const isValid = normalize(confirmationText) === normalize(expected);

  // Focus input when modal opens
  useEffect(() => {
    if (open && inputRef.current) {
      // Use requestAnimationFrame twice to ensure DOM has settled
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          inputRef.current?.focus();
        });
      });
    }
    // Reset state when modal closes
    if (!open) {
      setConfirmationText('');
      setShowError(false);
    }
  }, [open]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setConfirmationText(value);
    // Error only once typed text stops being a correct-so-far prefix of the expected token; casing rule follows the match above.
    const matchesPrefixSoFar = normalize(expected).startsWith(normalize(value));
    setShowError(value.length > 0 && !matchesPrefixSoFar);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && isValid && !loading) {
      onConfirm();
    }
  };

  const handleConfirm = () => {
    if (isValid && !loading) {
      onConfirm();
    }
  };

  return (
    <AlertDialog.Root open={open} onOpenChange={loading ? undefined : onClose}>
      <AlertDialog.Content maxWidth="450px">
        {/* Header with Close Button */}
        <Flex align="start" justify="between" mb="2">
          <AlertDialog.Title size="5">Delete {entityType}?</AlertDialog.Title>
          <IconButton
            variant="ghost"
            size="1"
            color="gray"
            aria-label="Close"
            disabled={loading}
            onClick={onClose}
          >
            <X size={16} />
          </IconButton>
        </Flex>

        {/* Warning Message — surface the entity name here so the user sees what they're
            deleting now that the acknowledgement is a fixed literal word. When the deletion is a
            recoverable trash (trash), the copy reflects a move to Trash instead of a
            permanent, irreversible removal. */}
        <AlertDialog.Description size="2" mb="4">
          {trash ? (
            <>
              {entityName ? (
                <>
                  <Text as="span" weight="bold">{entityName}</Text>{' '}
                  will be moved to Trash
                </>
              ) : (
                <>This {entityType} will be moved to Trash</>
              )}
              {typeof retentionDays === 'number'
                ? <> and can be restored within {retentionDays} days before it is permanently removed.</>
                : <> and can be restored before it is permanently removed.</>}
            </>
          ) : entityName ? (
            <>
              This action cannot be undone. All data associated with{' '}
              <Text as="span" weight="bold">{entityName}</Text>{' '}
              will be permanently deleted.
            </>
          ) : (
            <>This action cannot be undone. All data associated with this {entityType} will be permanently deleted.</>
          )}
        </AlertDialog.Description>

        {/* Optional caller-provided context (resource details, related counts, etc.) */}
        {children && <Box mb="4">{children}</Box>}

        {/* Highlighted Warning Box */}
        <Box
          p="3"
          mb="4"
          style={{
            backgroundColor: 'var(--red-2)',
            border: '1px solid var(--red-6)',
            borderRadius: '6px',
          }}
        >
          <Text size="2" weight="medium" style={{ display: 'block' }}>
            {expected}
          </Text>
          <Text size="1" color="gray">
            Type this to confirm deletion
          </Text>
        </Box>

        {/* Verification Input */}
        <Box mb="4">
          <Flex align="center" gap="1" mb="2">
            <Text size="2" weight="bold">
              Acknowledgement
            </Text>
            <Text size="2" style={{ color: 'var(--red-9)' }}>
              *
            </Text>
          </Flex>
          <TextField.Root
            ref={inputRef}
            size="2"
            value={confirmationText}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder={`Type "${expected}" to confirm`}
            disabled={loading}
            color={showError ? 'red' : undefined}
            style={
              showError
                ? {
                    outline: '2px solid var(--red-9)',
                    outlineOffset: '-2px',
                  }
                : undefined
            }
            aria-label="Acknowledgement"
            autoFocus
          />
          {showError && (
            <Text
              size="1"
              color="red"
              mt="1"
              style={{ display: 'block' }}
              role="alert"
            >
              The confirmation text provided is incorrect
            </Text>
          )}
        </Box>

        {/* Action Buttons */}
        <Flex gap="3" mt="4" justify="end">
          <AlertDialog.Cancel>
            <Button variant="surface" size="2" disabled={loading}>
              Cancel
            </Button>
          </AlertDialog.Cancel>
          <AlertDialog.Action>
            <Button
              variant="solid"
              color="red"
              size="2"
              onClick={handleConfirm}
              disabled={!isValid || loading}
            >
              {loading ? 'Deleting...' : 'Delete'}
            </Button>
          </AlertDialog.Action>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}

export default DeleteConfirmationModal;
