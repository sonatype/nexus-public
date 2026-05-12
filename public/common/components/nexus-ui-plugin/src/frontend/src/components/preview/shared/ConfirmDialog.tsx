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
import * as AlertDialog from '@radix-ui/react-alert-dialog';
import { Flex, Text } from '@radix-ui/themes';
import { AlertTriangle } from 'lucide-react';

import './ConfirmDialog.scss';

export interface ConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'warning';
  onConfirm: () => void;
}

/**
 * ConfirmDialog - A reusable confirmation dialog using Radix AlertDialog
 *
 * Provides an accessible confirmation dialog with consistent styling across
 * the security settings modules.
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
}: ConfirmDialogProps) {
  const handleConfirm = () => {
    onConfirm();
    onOpenChange(false);
  };

  return (
    <AlertDialog.Root open={open} onOpenChange={onOpenChange}>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="confirm-dialog__overlay" />
        <AlertDialog.Content className="confirm-dialog__content">
          <Flex direction="column" gap="4">
            {/* Header with icon and title */}
            <Flex align="center" gap="3" className="confirm-dialog__header">
              <div className={`confirm-dialog__icon confirm-dialog__icon--${variant}`}>
                <AlertTriangle size={24} aria-hidden="true" />
              </div>
              <AlertDialog.Title className="confirm-dialog__title">
                {title}
              </AlertDialog.Title>
            </Flex>

            {/* Message */}
            <AlertDialog.Description asChild>
              <Text size="2" className="confirm-dialog__message">
                {message}
              </Text>
            </AlertDialog.Description>

            {/* Action buttons */}
            <Flex gap="3" justify="end" className="confirm-dialog__actions">
              <AlertDialog.Cancel asChild>
                <button
                  type="button"
                  className="confirm-dialog__button confirm-dialog__button--cancel"
                >
                  {cancelLabel}
                </button>
              </AlertDialog.Cancel>
              <AlertDialog.Action asChild>
                <button
                  type="button"
                  className={`confirm-dialog__button confirm-dialog__button--confirm confirm-dialog__button--${variant}`}
                  onClick={handleConfirm}
                >
                  {confirmLabel}
                </button>
              </AlertDialog.Action>
            </Flex>
          </Flex>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}

export default ConfirmDialog;
