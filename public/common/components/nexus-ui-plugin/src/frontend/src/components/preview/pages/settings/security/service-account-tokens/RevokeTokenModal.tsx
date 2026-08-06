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

import React, { useState, useEffect, useRef } from 'react';
import { Flex, Text, Dialog, Callout } from '@radix-ui/themes';
import { AlertTriangle } from 'lucide-react';

import { SettingsButton } from '../../../../shared/form';
import { SERVICE_ACCOUNT_TOKENS_STRINGS } from './strings';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.REVOKE_MODAL;

interface RevokeTokenModalProps {
  open: boolean;
  tokenName: string;
  onConfirm: () => void;
  onClose: () => void;
  loading?: boolean;
  /** Resolves the element to focus when the modal closes. Used to restore
   *  focus to the row's actions trigger on Cancel. Returns null after a
   *  successful revoke (row no longer exists). */
  getRestoreFocus?: () => HTMLElement | null;
}

export function RevokeTokenModal({ open, tokenName, onConfirm, onClose, loading, getRestoreFocus }: RevokeTokenModalProps) {
  const [confirmation, setConfirmation] = useState('');
  const matches = confirmation === tokenName;
  const warningRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) setConfirmation('');
  }, [open]);

  return (
    <Dialog.Root open={open} onOpenChange={(o) => !o && !loading && onClose()}>
      <Dialog.Content
        maxWidth="480px"
        data-testid="sat-revoke-modal"
        onCloseAutoFocus={(e) => {
          const target = getRestoreFocus?.();
          if (target) {
            e.preventDefault();
            target.focus({preventScroll: true});
          }
        }}
        onOpenAutoFocus={(e) => {
          // Move focus to the warning so screen readers announce it on open.
          e.preventDefault();
          warningRef.current?.focus({preventScroll: true});
        }}
      >
        <Dialog.Title size="4" weight="medium" mb="4">
          {LABELS.TITLE}
        </Dialog.Title>
        <Dialog.Description asChild>
          <div ref={warningRef} tabIndex={-1}>
            <Callout.Root color="orange" size="1" mb="4">
              <Callout.Icon>
                <AlertTriangle size={16} />
              </Callout.Icon>
              <Callout.Text>{LABELS.WARNING(tokenName)}</Callout.Text>
            </Callout.Root>
          </div>
        </Dialog.Description>

        <Flex direction="column" gap="1" mb="4">
          <Text as="label" size="1" weight="medium" htmlFor="sat-revoke-confirmation">
            {LABELS.INPUT_LABEL(tokenName)}
          </Text>
          <input
            id="sat-revoke-confirmation"
            className="sat-page__filter-input sat-revoke-input"
            type="text"
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
            data-testid="sat-revoke-input"
            autoComplete="off"
            dir="ltr"
          />
        </Flex>

        <Flex gap="3" justify="end">
          <SettingsButton
            variant="secondary"
            onClick={onClose}
            disabled={loading}
            data-testid="sat-revoke-cancel"
          >
            {LABELS.CANCEL_BUTTON}
          </SettingsButton>
          <SettingsButton
            variant="danger"
            onClick={onConfirm}
            disabled={!matches}
            loading={loading}
            data-testid="sat-revoke-confirm"
          >
            {LABELS.REVOKE_BUTTON}
          </SettingsButton>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
