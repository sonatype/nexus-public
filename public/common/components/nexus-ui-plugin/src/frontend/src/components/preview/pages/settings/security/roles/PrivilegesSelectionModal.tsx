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

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { X, Loader2 } from 'lucide-react';

import { SettingsButton, SettingsTransferList } from '../../../../shared/form';
import { PrivilegesSelectionModalProps } from './types';

import './PrivilegesSelectionModal.scss';

/**
 * PrivilegesSelectionModal - Modal for selecting privileges to assign to a role
 */
export function PrivilegesSelectionModal({
  isOpen,
  onClose,
  availablePrivileges,
  selectedPrivileges,
  onSave,
  loading = false,
}: PrivilegesSelectionModalProps) {
  const [selection, setSelection] = useState<string[]>(selectedPrivileges);

  // Reset selection when modal opens
  useEffect(() => {
    if (isOpen) {
      setSelection(selectedPrivileges);
    }
  }, [isOpen, selectedPrivileges]);

  // Convert to transfer list format
  const availableItems = useMemo(() => {
    return availablePrivileges.map((p) => ({ id: p.id, name: p.name }));
  }, [availablePrivileges]);

  const selectedItems = useMemo(() => {
    return availablePrivileges
      .filter((p) => selection.includes(p.id))
      .map((p) => ({ id: p.id, name: p.name }));
  }, [availablePrivileges, selection]);

  const handleChange = useCallback((newSelected: Array<{ id: string; name: string }>) => {
    setSelection(newSelected.map((p) => p.id));
  }, []);

  const handleSave = useCallback(() => {
    onSave(selection);
    onClose();
  }, [selection, onSave, onClose]);

  const handleCancel = useCallback(() => {
    setSelection(selectedPrivileges);
    onClose();
  }, [selectedPrivileges, onClose]);

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => !open && handleCancel()}>
      <Dialog.Portal>
        <Dialog.Overlay className="privileges-modal__overlay" />
        <Dialog.Content className="privileges-modal__content">
          <Flex justify="between" align="center" className="privileges-modal__header">
            <Dialog.Title asChild>
              <Heading size="5">Select Privileges</Heading>
            </Dialog.Title>
            <Dialog.Close asChild>
              <button type="button" className="privileges-modal__close" aria-label="Close">
                <X size={18} />
              </button>
            </Dialog.Close>
          </Flex>

          <Dialog.Description className="privileges-modal__description">
            <Text size="2">
              Select privileges to grant to this role. Double-click or use arrows to move items.
            </Text>
          </Dialog.Description>

          <Box className="privileges-modal__body">
            {loading ? (
              <Flex align="center" justify="center" className="privileges-modal__loading">
                <Loader2 size={24} className="privileges-modal__spinner" />
                <Text size="2">Loading privileges...</Text>
              </Flex>
            ) : (
              <SettingsTransferList
                name="privileges-selection"
                availableItems={availableItems}
                selectedItems={selectedItems}
                onChange={handleChange}
                availableLabel="Available Privileges"
                selectedLabel="Selected Privileges"
                getItemId={(item) => item.id}
                getItemLabel={(item) => item.name}
              />
            )}
          </Box>

          <Flex justify="end" gap="3" className="privileges-modal__footer">
            <SettingsButton variant="secondary" onClick={handleCancel}>
              Cancel
            </SettingsButton>
            <SettingsButton variant="primary" onClick={handleSave} disabled={loading}>
              Apply Selection
            </SettingsButton>
          </Flex>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

export default PrivilegesSelectionModal;


