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

// Fixed acknowledgement word required to confirm any destructive deletion. Using a
// single literal word — instead of per-entity names — keeps the friction predictable
// and short to type while still forcing a deliberate action. Displayed as-is; the
// match is case-insensitive so "delete", "Delete", "DELETE" all confirm.
const CONFIRMATION_TEXT = 'Delete';

export interface DeleteConfirmationModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  entityName?: string;
  entityType: string;
  loading?: boolean;
  /** Optional extra content (e.g. resource details) rendered above the
   *  acknowledgement input. Lets callers surface context like task type or
   *  schedule without forking the modal. */
  children?: React.ReactNode;
}

/**
 * Reusable delete confirmation modal with text verification.
 *
 * Follows nexusone design system standards:
 * - Modal width: 450px (destructive confirmation)
 * - Requires typing "Delete" (case-insensitive) for all deletions
 * - Entity name is displayed in the description but not required for verification
 *
 * @param open - Controls modal visibility
 * @param onClose - Called when modal should close (cancel, X button)
 * @param onConfirm - Called when user confirms deletion with valid text
 * @param entityName - If provided, displayed in description for context
 * @param entityType - Display name for the entity type (e.g., "repository", "user", "blob store")
 * @param loading - Shows loading state during delete operation
 */
export function DeleteConfirmationModal({
  open,
  onClose,
  onConfirm,
  entityName,
  entityType,
  loading = false,
  children,
}: DeleteConfirmationModalProps) {
  const [confirmationText, setConfirmationText] = useState('');
  const [showError, setShowError] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  // Acknowledgement is a case-insensitive match against the literal CONFIRMATION_TEXT
  // in every case. The entity name still flows through to the description so the user
  // can see what they're about to delete, but they no longer have to type it.
  const expectedLower = CONFIRMATION_TEXT.toLowerCase();
  const isValid = confirmationText.toLowerCase() === expectedLower;

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
    // Show the inline error only when the user has typed something and it doesn't
    // match (or is not on track to match) the expected literal CONFIRMATION_TEXT.
    // A correct-so-far prefix (e.g. "del", "Del") is not an error — we just leave
    // the submit button disabled until the full word lands. Comparison is
    // case-insensitive so any casing of the word confirms.
    const matchesPrefixSoFar = expectedLower.startsWith(value.toLowerCase());
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
            deleting now that the acknowledgement is a fixed literal word. */}
        <AlertDialog.Description size="2" mb="4">
          {entityName ? (
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
            {CONFIRMATION_TEXT}
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
            placeholder={`Type "${CONFIRMATION_TEXT}" to confirm`}
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
