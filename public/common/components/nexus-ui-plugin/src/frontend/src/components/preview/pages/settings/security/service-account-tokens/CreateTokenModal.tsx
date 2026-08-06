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

import React, { useState, useCallback, useEffect, useRef } from 'react';
import { Box, Flex, Dialog, Callout, Text, VisuallyHidden } from '@radix-ui/themes';
import { KeyRound, AlertTriangle } from 'lucide-react';

import {
  SettingsTextInput,
  SettingsSelect,
  SettingsTextArea,
  SettingsButton,
} from '../../../../shared/form';
import { CreateTokenForm, RoleOption } from './types';
import { SERVICE_ACCOUNT_TOKENS_STRINGS } from './strings';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.CREATE_MODAL;
const EXPIRY_OPTIONS = LABELS.EXPIRY_OPTIONS;

interface CreateTokenModalProps {
  open: boolean;
  onClose: () => void;
  onCreate: (form: CreateTokenForm) => void;
  roles: RoleOption[];
  rolesError?: React.ReactNode | null;
  existingNames?: string[];
  loading?: boolean;
  /** Resolves the element to focus when the modal closes. Used to deterministically
   *  restore focus to the trigger that opened it, even if Radix's default
   *  snapshot of the previously-active element is empty. */
  getRestoreFocus?: () => HTMLElement | null;
}

// Must match ServiceAccountConstants.NAME_PATTERN on the server side
const NAME_PATTERN = /^[a-zA-Z0-9_-]+$/;

function validateName(name: string, existingNames: string[]): string | undefined {
  const trimmed = name.trim();
  if (name.length > 0 && !NAME_PATTERN.test(trimmed)) return LABELS.NAME_INVALID_CHARS_ERROR;
  if (trimmed.length > 0 && existingNames.includes(trimmed)) {
    return LABELS.NAME_DUPLICATE_ERROR(trimmed);
  }
  return undefined;
}

export function CreateTokenModal({ open, onClose, onCreate, roles, rolesError = null, existingNames = [], loading, getRestoreFocus }: CreateTokenModalProps) {
  const [name, setName] = useState('');
  const [roleId, setRoleId] = useState('');
  const [expirationDays, setExpirationDays] = useState(EXPIRY_OPTIONS[0].value);
  const [description, setDescription] = useState('');

  const nameError = validateName(name, existingNames);
  const isValid = name.trim().length > 0 && roleId.length > 0 && !nameError && !rolesError;
  const isNeverExpires = expirationDays === '-1';

  // Dialog content element — used as the portal target for nested Select
  // dropdowns so they stay inside the Dialog's focus trap.
  const [dialogContentEl, setDialogContentEl] = useState<HTMLDivElement | null>(null);

  const warningRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!isNeverExpires) return;
    // Radix Select schedules its own focus-return-to-trigger via setTimeout.
    // We queue our focus call after it (FIFO macrotask order) so VoiceOver
    // lands on the warning and announces it, rather than getting bounced
    // back to the expiration trigger.
    const timerId = setTimeout(() => {
      warningRef.current?.focus({preventScroll: true});
    }, 0);
    return () => clearTimeout(timerId);
  }, [isNeverExpires]);

  const handleSubmit = useCallback(() => {
    if (!isValid) return;
    const form: CreateTokenForm = {
      name: name.trim(),
      roleId,
      description: description.trim(),
    };
    if (!isNeverExpires) {
      form.expirationDays = parseInt(expirationDays, 10);
    }
    onCreate(form);
  }, [name, roleId, expirationDays, description, isValid, isNeverExpires, onCreate]);

  useEffect(() => {
    if (!open) {
      setName('');
      setRoleId('');
      setExpirationDays(EXPIRY_OPTIONS[0].value);
      setDescription('');
    }
  }, [open]);

  const roleOptions = roles.map((r) => ({ value: r.id, label: r.name }));

  return (
    <Dialog.Root open={open} onOpenChange={(o) => !o && !loading && onClose()}>
      <Dialog.Content
        maxWidth="560px"
        data-testid="sat-create-modal"
        ref={setDialogContentEl}
        onCloseAutoFocus={(e) => {
          const target = getRestoreFocus?.();
          if (target) {
            e.preventDefault();
            target.focus({preventScroll: true});
          }
        }}
      >
        <Dialog.Title size="4" weight="medium" mb="4">
          <Flex align="center" gap="3">
            <KeyRound size={24} />
            {LABELS.TITLE}
          </Flex>
        </Dialog.Title>
        <VisuallyHidden>
          <Dialog.Description>{LABELS.TITLE}</Dialog.Description>
        </VisuallyHidden>

        <Flex direction="column" gap="3" mb="4">
          <SettingsTextInput
            name="name"
            label={LABELS.NAME_LABEL}
            value={name}
            onChange={(v: string) => setName(v.slice(0, 128))}
            error={nameError}
            placeholder={LABELS.NAME_PLACEHOLDER}
            helpText={LABELS.NAME_HELP}
            maxLength={128}
            alwaysShowHelpText
            required
            data-testid="sat-create-name"
          />

          <SettingsSelect
            name="roleId"
            label={LABELS.ROLE_LABEL}
            value={roleId}
            onChange={setRoleId}
            options={roleOptions}
            placeholder={LABELS.ROLE_PLACEHOLDER}
            helpText={LABELS.ROLE_HELP}
            disabled={!!rolesError}
            required
            container={dialogContentEl}
            data-testid="sat-create-role"
          />

          {rolesError && (
            <Callout.Root color="red" role="alert" size="1" data-testid="sat-create-roles-error">
              <Callout.Icon>
                <AlertTriangle size={16} />
              </Callout.Icon>
              <Callout.Text>{rolesError}</Callout.Text>
            </Callout.Root>
          )}

          <SettingsSelect
            name="expirationDays"
            label={LABELS.EXPIRATION_LABEL}
            value={expirationDays}
            onChange={setExpirationDays}
            options={EXPIRY_OPTIONS}
            helpText={LABELS.EXPIRATION_HELP}
            required
            container={dialogContentEl}
            data-testid="sat-create-expiration"
          />

          <Box className="sat-description-field">
            <SettingsTextArea
              name="description"
              label={LABELS.DESCRIPTION_LABEL}
              helpText={LABELS.DESCRIPTION_HELP}
              value={description}
              onChange={(v: string) => setDescription(v.slice(0, 256))}
              maxLength={256}
              data-testid="sat-create-description"
            />
            <Flex justify="end" mt="1">
              <Text size="1" color="gray" data-testid="sat-description-counter">
                {description.length} / 256
              </Text>
            </Flex>
          </Box>

          {isNeverExpires && (
            <div ref={warningRef} tabIndex={-1}>
              <Callout.Root color="orange" size="1">
                <Callout.Icon>
                  <AlertTriangle size={16} />
                </Callout.Icon>
                <Callout.Text>{LABELS.NEVER_EXPIRES_WARNING}</Callout.Text>
              </Callout.Root>
            </div>
          )}
        </Flex>

        <Flex gap="3" justify="end" pt="4" style={{ borderTop: '1px solid var(--gray-6)' }}>
          <SettingsButton
            variant="secondary"
            onClick={onClose}
            disabled={loading}
            data-testid="sat-create-cancel"
          >
            {LABELS.CANCEL_BUTTON}
          </SettingsButton>
          <SettingsButton
            variant="primary"
            onClick={handleSubmit}
            disabled={!isValid}
            loading={loading}
            data-testid="sat-create-submit"
          >
            {LABELS.CREATE_BUTTON}
          </SettingsButton>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
