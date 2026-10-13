/*
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */

import React, { useState, useRef, useEffect, useMemo } from 'react';
import { Box, Flex, Button, Dialog, Text, TextField, IconButton } from '@radix-ui/themes';
import { AlertCircle, X } from 'lucide-react';
import { isValidIP, stripUriBrackets } from './utils/ipValidation';


/**
 * Modal for adding a single IP address.
 */
export function AddIpModal({ isOpen, onClose, onConfirm, existingIps = [] }) {
  const [ipInput, setIpInput] = useState('');
  const [description, setDescription] = useState('');
  const [validationError, setValidationError] = useState('');
  const inputRef = useRef(null);

  // Focus input when modal opens
  useEffect(() => {
    if (isOpen) {
      inputRef.current?.focus();
    }
  }, [isOpen]);

  // Bracket-stripped set of already-stored entries. existingIps only changes
  // when the user adds or removes entries, not on every keystroke, so memoize
  // to avoid rebuilding on each render/input change.
  const existingBareSet = useMemo(
    () => new Set(existingIps.map(item => stripUriBrackets(item.ipAddress))),
    [existingIps]
  );

  const handleIpInputChange = (e) => {
    const value = e.target.value;
    setIpInput(value);
    // Clear validation error when user starts typing
    setValidationError('');
  };

  const validateIpInput = (value) => {
    const trimmed = value.trim();

    if (!trimmed) {
      return '';
    }

    // Check if valid IP format
    if (!isValidIP(trimmed)) {
      return 'Invalid IP address or CIDR notation';
    }

    if (existingBareSet.has(stripUriBrackets(trimmed))) {
      return 'This IP address already exists in your allow list';
    }

    return '';
  };

  const handleIpInputBlur = () => {
    const error = validateIpInput(ipInput);
    setValidationError(error);
  };

  // Check if we can submit: input must have value, be valid, and not be duplicate
  const trimmedIp = ipInput.trim();
  const currentError = validateIpInput(ipInput);
  const canSubmit = trimmedIp && !currentError;

  const handleSubmit = () => {
    const trimmedIp = ipInput.trim();

    // Use the live validation check, not the blur-updated validationError state.
    // The button's disabled flag uses this same live signal via canSubmit, so
    // handleSubmit stays consistent even if the user changes input without
    // blurring first, or if a keyboard/programmatic path bypasses the disabled
    // click gate.
    if (!trimmedIp || validateIpInput(ipInput)) return;

    onConfirm([{
      ipAddress: trimmedIp,
      description: description.trim() || '',
    }]);

    handleClose();
  };

  const handleClose = () => {
    setIpInput('');
    setDescription('');
    setValidationError('');
    onClose();
  };

  return (
    <Dialog.Root open={isOpen} onOpenChange={handleClose}>
      <Dialog.Content maxWidth="480px" data-testid="add-ip-modal">
        <Flex align="start" justify="between" mb="3">
          <Dialog.Title size="4">Add IP Address</Dialog.Title>
          <Dialog.Close>
            <IconButton variant="ghost" size="1">
              <X size={18} />
            </IconButton>
          </Dialog.Close>
        </Flex>

        <Box style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <Text size="2" color="gray">
            Add a single IP address to your allow list. Supports IPv4, IPv6, and CIDR notation (e.g., 192.168.1.1, 2001:db8::1, 10.0.0.0/24).
          </Text>

          {/* IP Address Input */}
          <Box>
            <Text size="2" weight="medium" style={{ display: 'block', marginBottom: '8px' }}>
              IP Address *
            </Text>
            <TextField.Root
              ref={inputRef}
              placeholder="192.168.1.1"
              value={ipInput}
              onChange={handleIpInputChange}
              onBlur={handleIpInputBlur}
              style={{ width: '100%' }}
              data-testid="add-ip-input"
            />
            {validationError && (
              <Flex gap="1" align="center" mt="2">
                <AlertCircle size={12} color="var(--red-11)" />
                <Text size="1" color="red">
                  {validationError}
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
              data-testid="add-description-input"
            />
          </Box>
        </Box>

        <Flex gap="3" mt="4" justify="end">
          <Dialog.Close>
            <Button variant="surface" onClick={handleClose} data-testid="add-ip-cancel-button">
              Cancel
            </Button>
          </Dialog.Close>
          <Button variant="solid" onClick={handleSubmit} disabled={!canSubmit} data-testid="add-ip-submit-button">
            Add IP Address
          </Button>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
