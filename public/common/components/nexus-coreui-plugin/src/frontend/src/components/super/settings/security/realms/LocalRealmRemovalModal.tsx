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
import * as Dialog from '@radix-ui/react-dialog';
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { AlertTriangle, X, ExternalLink } from 'lucide-react';
import { SettingsButton } from '../../../shared/form';

import './LocalRealmRemovalModal.scss';

const ACKNOWLEDGEMENT_STRING = 'I acknowledge';

export interface LocalRealmRemovalModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

/**
 * Modal dialog shown when user attempts to remove local authenticating or authorizing realms.
 * Requires user to type "I acknowledge" to confirm the action.
 */
export function LocalRealmRemovalModal({ open, onClose, onConfirm }: LocalRealmRemovalModalProps) {
  const [acknowledgement, setAcknowledgement] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  // Focus input when modal opens
  useEffect(() => {
    if (open && inputRef.current) {
      // Small delay to ensure DOM is ready
      setTimeout(() => inputRef.current?.focus(), 50);
    }
    // Reset acknowledgement when modal opens/closes
    if (!open) {
      setAcknowledgement('');
    }
  }, [open]);

  const isValid = acknowledgement === ACKNOWLEDGEMENT_STRING;

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && isValid) {
      onConfirm();
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setAcknowledgement(e.target.value);
  };

  return (
    <Dialog.Root open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="local-realm-removal-modal__overlay" />
        <Dialog.Content
          className="local-realm-removal-modal"
          aria-describedby="modal-description"
          data-testid="local-realm-removal-modal"
        >
          <Dialog.Title asChild>
            <Flex align="center" gap="2" className="local-realm-removal-modal__header">
              <AlertTriangle size={20} className="local-realm-removal-modal__icon" />
              <Heading as="h2" size="4" weight="medium">
                Confirm Removal of Local Realms
              </Heading>
            </Flex>
          </Dialog.Title>

          <Dialog.Close asChild>
            <button
              className="local-realm-removal-modal__close"
              aria-label="Close"
              type="button"
            >
              <X size={16} />
            </button>
          </Dialog.Close>

          <Box className="local-realm-removal-modal__content">
            <Box className="local-realm-removal-modal__warning" id="modal-description">
              <AlertTriangle size={16} />
              <Text size="2">
                Warning! Removing local realms will prevent local admin access.
                This can make system recovery more difficult should there be issues with
                other authentication realms. For more information, see the{' '}
                <a
                  href="http://links.sonatype.com/products/nxrm3/docs/realms"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="local-realm-removal-modal__link"
                >
                  Realms help documentation
                  <ExternalLink size={12} />
                </a>
              </Text>
            </Box>

            <Box className="local-realm-removal-modal__form-group">
              <label htmlFor="acknowledgement-input" className="local-realm-removal-modal__label">
                <Text size="2" weight="medium">Acknowledgement</Text>
                <Text size="1" className="local-realm-removal-modal__required">*</Text>
              </label>
              <Text size="1" className="local-realm-removal-modal__sublabel">
                Type "{ACKNOWLEDGEMENT_STRING}" in order to proceed with this action
              </Text>
              <input
                ref={inputRef}
                id="acknowledgement-input"
                type="text"
                value={acknowledgement}
                onChange={handleChange}
                onKeyDown={handleKeyDown}
                placeholder="Provide acknowledgement"
                className={`local-realm-removal-modal__input ${acknowledgement && !isValid ? 'local-realm-removal-modal__input--invalid' : ''}`}
                data-testid="acknowledgement-input"
              />
              {acknowledgement && !isValid && (
                <Text size="1" className="local-realm-removal-modal__error">
                  Invalid acknowledgement
                </Text>
              )}
            </Box>
          </Box>

          <Flex gap="2" justify="end" className="local-realm-removal-modal__footer">
            <SettingsButton variant="secondary" onClick={onClose} data-testid="cancel-button">
              Cancel
            </SettingsButton>
            <SettingsButton
              variant="danger"
              onClick={onConfirm}
              disabled={!isValid}
              data-testid="confirm-button"
            >
              Confirm
            </SettingsButton>
          </Flex>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

export default LocalRealmRemovalModal;
