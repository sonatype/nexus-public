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

import type { DangerousFieldInfo } from './dangerousFields';

export interface DangerousEditConfirmDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  blobStoreName: string;
  changedFields: DangerousFieldInfo[];
}

export function DangerousEditConfirmDialog({
  open,
  onClose,
  onConfirm,
  blobStoreName,
  changedFields,
}: DangerousEditConfirmDialogProps) {
  const [confirmationText, setConfirmationText] = useState('');
  const [showError, setShowError] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const isValid = confirmationText === blobStoreName;

  useEffect(() => {
    if (open && inputRef.current) {
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          inputRef.current?.focus();
        });
      });
    }
    if (!open) {
      setConfirmationText('');
      setShowError(false);
    }
  }, [open]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setConfirmationText(value);
    setShowError(value.length > 0 && value !== blobStoreName);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && isValid) {
      onConfirm();
    }
  };

  const handleConfirm = () => {
    if (isValid) {
      onConfirm();
    }
  };

  return (
    <AlertDialog.Root open={open} onOpenChange={onClose}>
      <AlertDialog.Content maxWidth="450px">
        <Flex align="start" justify="between" mb="2">
          <AlertDialog.Title size="5">Update Blob Store?</AlertDialog.Title>
          <IconButton
            variant="ghost"
            size="1"
            color="gray"
            aria-label="Close"
            onClick={onClose}
          >
            <X size={16} />
          </IconButton>
        </Flex>

        <AlertDialog.Description size="2" mb="4">
          You are about to change configuration fields that may cause data loss or make this blob store inaccessible.
        </AlertDialog.Description>

        <Box
          p="3"
          mb="4"
          style={{
            backgroundColor: 'var(--amber-2)',
            border: '1px solid var(--amber-6)',
            borderRadius: '6px',
          }}
        >
          <Text size="2" weight="medium" style={{ display: 'block' }} mb="2">
            The following fields have been modified:
          </Text>
          <ul style={{ margin: 0, paddingLeft: '20px' }}>
            {changedFields.map(({ field, label }) => (
              <li key={field}>
                <Text size="2">{label}</Text>
              </li>
            ))}
          </ul>
        </Box>

        <Box
          p="3"
          mb="4"
          style={{
            backgroundColor: 'var(--amber-2)',
            border: '1px solid var(--amber-6)',
            borderRadius: '6px',
          }}
        >
          <Text size="2" weight="medium" style={{ display: 'block' }}>
            {blobStoreName}
          </Text>
          <Text size="1" color="gray">
            Type this name to confirm
          </Text>
        </Box>

        <Box mb="4">
          <Flex align="center" gap="1" mb="2">
            <Text size="2" weight="bold">
              Acknowledgement
            </Text>
            <Text size="2" style={{ color: 'var(--amber-9)' }}>
              *
            </Text>
          </Flex>
          <TextField.Root
            ref={inputRef}
            size="2"
            value={confirmationText}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            placeholder={`Type "${blobStoreName}" to confirm`}
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

        <Flex gap="3" mt="4" justify="end">
          <AlertDialog.Cancel>
            <Button variant="surface" size="2" onClick={onClose}>
              Cancel
            </Button>
          </AlertDialog.Cancel>
          <AlertDialog.Action>
            <Button
              variant="solid"
              color="amber"
              size="2"
              onClick={handleConfirm}
              disabled={!isValid}
            >
              Confirm Update
            </Button>
          </AlertDialog.Action>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}

export default DangerousEditConfirmDialog;
