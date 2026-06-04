/*
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */

import { Box, Flex, Button, Dialog, Text, TextField, IconButton } from '@radix-ui/themes';
import { AlertCircle, X } from 'lucide-react';
import { isValidIP } from './utils/ipValidation';
import React, { useState, useEffect } from 'react';


/**
 * Modal for editing an existing IP address entry.
 * Allows modification of IP address and description.
 */
export function EditIpModal({ isOpen, onClose, onConfirm, ipEntry }) {
  const [ipAddress, setIpAddress] = useState('');
  const [description, setDescription] = useState('');
  const [ipError, setIpError] = useState('');

  // Initialize form when modal opens or ipEntry changes
  useEffect(() => {
    if (isOpen && ipEntry) {
      setIpAddress(ipEntry.ipAddress || '');
      setDescription(ipEntry.description || '');
      setIpError('');
    }
  }, [isOpen, ipEntry]);

  // Validate IP address on change
  const handleIpChange = (e) => {
    const value = e.target.value;
    setIpAddress(value);

    if (value.trim() && !isValidIP(value.trim())) {
      setIpError('Invalid IP address format. Use 192.168.1.1, 10.0.0.0/24, 2001:db8::1, or 2001:db8::/32');
    } else {
      setIpError('');
    }
  };

  const canSubmit = ipAddress.trim() && !ipError;

  const handleSubmit = () => {
    if (!canSubmit) return;

    onConfirm({
      ...ipEntry,
      ipAddress: ipAddress.trim(),
      description: description.trim(),
    });

    handleClose();
  };

  const handleClose = () => {
    setIpAddress('');
    setDescription('');
    setIpError('');
    onClose();
  };

  if (!ipEntry) return null;

  return (
    <Dialog.Root open={isOpen} onOpenChange={handleClose}>
      <Dialog.Content maxWidth="480px" data-testid="edit-ip-modal">
        <Flex align="start" justify="between" mb="3">
          <Dialog.Title size="4">Edit IP Address</Dialog.Title>
          <Dialog.Close>
            <IconButton variant="ghost" size="1">
              <X size={18} />
            </IconButton>
          </Dialog.Close>
        </Flex>

        <Box style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* IP Address Input */}
          <Box>
            <Text size="2" weight="medium" style={{ display: 'block', marginBottom: '8px' }}>
              IP Address *
            </Text>
            <TextField.Root
              placeholder="192.168.1.1 or 10.0.0.0/24"
              value={ipAddress}
              onChange={handleIpChange}
              style={{ width: '100%' }}
              data-testid="edit-ip-input"
            />
            {ipError && (
              <Flex gap="2" align="start" mt="2">
                <AlertCircle size={14} style={{ color: 'var(--red-9)', marginTop: '2px', flexShrink: 0 }} />
                <Text size="1" style={{ color: 'var(--red-11)' }}>
                  {ipError}
                </Text>
              </Flex>
            )}
          </Box>

          {/* Description Input */}
          <Box>
            <Flex justify="between" align="center" style={{ marginBottom: '8px' }}>
              <Text size="2" weight="medium">
                Description <Text size="1" color="gray" weight="normal">(Optional)</Text>
              </Text>
              <Text size="1" color="gray">
                {description.length}/255
              </Text>
            </Flex>
            <TextField.Root
              placeholder="e.g., Office network, VPN gateway"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={255}
              style={{ width: '100%' }}
              data-testid="edit-description-input"
            />
          </Box>
        </Box>

        <Flex gap="3" mt="4" justify="end">
          <Dialog.Close>
            <Button variant="surface" onClick={handleClose} data-testid="edit-ip-cancel-button">
              Cancel
            </Button>
          </Dialog.Close>
          <Button variant="solid" onClick={handleSubmit} disabled={!canSubmit} data-testid="edit-ip-submit-button">
            Save Changes
          </Button>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
