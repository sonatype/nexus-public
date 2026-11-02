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
import { Dialog, Flex, Text, Button, RadioGroup, Spinner, Badge } from '@radix-ui/themes';
import { useFirewallEnable, type FirewallMode } from './useFirewallEnable';
import { useFirewallModePicker } from './useFirewallModePicker';
import { useToast } from '../Toast';
import FirewallStrings from './FirewallStrings';

const { picker: STRINGS } = FirewallStrings.FIREWALL;

export interface FirewallModePickerDialogProps {
  repositoryName: string;
  repositoryFormat: string;
  isOpen: boolean;
  onClose: () => void;
  /** Called after the mode was written so the parent can refresh status */
  onModeChanged?: () => void;
}

/** Lets the user choose which firewall mode to apply, after loading the repository's current one. */
export function FirewallModePickerDialog({
  repositoryName,
  repositoryFormat,
  isOpen,
  onClose,
  onModeChanged,
}: FirewallModePickerDialogProps): JSX.Element {
  const toast = useToast();
  const picker = useFirewallModePicker(repositoryName, repositoryFormat);
  const { setMode, loading: saving } = useFirewallEnable(repositoryName);
  const { currentMode, selectedMode } = picker;

  const confirm = () => {
    if (!picker.canConfirm || !selectedMode) return;
    const wasEnabled = currentMode !== null && currentMode !== 'DISABLED';
    setMode(selectedMode, () => {
      toast.success(
        wasEnabled
          ? STRINGS.switchedToast(repositoryName, currentMode as string, selectedMode)
          : STRINGS.enabledToast(repositoryName, selectedMode),
      );
      onClose();
      onModeChanged?.();
    }).catch((err) => {
      toast.error(STRINGS.failedToast(err instanceof Error ? err.message : String(err)));
    });
  };

  const alreadyEnabled = currentMode !== null && currentMode !== 'DISABLED';

  return (
    <Dialog.Root open={isOpen} onOpenChange={(open) => { if (!open) onClose(); }}>
      <Dialog.Content
        maxWidth="480px"
        aria-describedby={undefined}
        onClick={(e) => e.stopPropagation()}
        onPointerDown={(e) => e.stopPropagation()}
      >
        <Dialog.Title>{STRINGS.title}</Dialog.Title>

        {picker.loadState === 'loading' && (
          <Flex align="center" gap="3" py="4">
            <Spinner size="2" />
            <Text size="2" color="gray">{STRINGS.loading}</Text>
          </Flex>
        )}

        {picker.loadState === 'failed' && (
          <Flex direction="column" gap="3" py="2">
            <Text size="2" color="red" role="alert">{STRINGS.loadFailed}</Text>
            <Flex justify="end">
              <Button variant="soft" size="2" onClick={picker.retry}>{STRINGS.retry}</Button>
            </Flex>
          </Flex>
        )}

        {picker.loadState === 'ready' && (
          <Flex direction="column" gap="3">
            <Flex align="center" gap="2">
              <Badge variant="soft" color={alreadyEnabled ? 'green' : 'gray'} size="1">
                {STRINGS.currentMode(currentMode ?? '')}
              </Badge>
            </Flex>
            <Text size="2" color="gray">
              {alreadyEnabled ? STRINGS.alreadyEnabled(currentMode as string) : STRINGS.chooseMode}
            </Text>
            <RadioGroup.Root
              value={selectedMode ?? ''}
              onValueChange={(value) => picker.select(value as FirewallMode)}
              aria-label={STRINGS.title}
            >
              <Flex direction="column" gap="2">
                {picker.availableModes.map((mode) => (
                  <RadioGroup.Item key={mode} value={mode} disabled={mode === currentMode}>
                    <Flex direction="column">
                      <Text size="2" weight="medium">{STRINGS.modes[mode as 'AUDIT' | 'QUARANTINE' | 'PCCS'].label}</Text>
                      <Text size="1" color="gray">{STRINGS.modes[mode as 'AUDIT' | 'QUARANTINE' | 'PCCS'].description}</Text>
                    </Flex>
                  </RadioGroup.Item>
                ))}
              </Flex>
            </RadioGroup.Root>
          </Flex>
        )}

        <Flex gap="3" mt="4" justify="end">
          <Button variant="soft" color="gray" size="2" onClick={onClose}>{STRINGS.cancel}</Button>
          <Button
            variant="solid"
            size="2"
            onClick={confirm}
            disabled={!picker.canConfirm}
            loading={saving}
          >
            {alreadyEnabled ? STRINGS.confirmSwitch : STRINGS.confirmEnable}
          </Button>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}

export default FirewallModePickerDialog;
