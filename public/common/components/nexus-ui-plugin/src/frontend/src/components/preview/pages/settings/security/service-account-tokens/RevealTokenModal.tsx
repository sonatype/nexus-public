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

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Box, Flex, Text, Dialog, Callout, Button, VisuallyHidden } from '@radix-ui/themes';
import { Info, Copy, Check } from 'lucide-react';

import { SERVICE_ACCOUNT_TOKENS_STRINGS } from './strings';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.REVEAL_MODAL;
const AUTO_CLOSE_SECONDS = 60;

interface RevealTokenModalProps {
  open: boolean;
  token: string;
  onClose: () => void;
  /** Resolves the element to focus when the modal closes. Useful when this
   *  modal is opened after another (e.g. the Create modal) finishes — Radix's
   *  default focus-restore points at the previous modal's now-unmounted form,
   *  so we need to explicitly direct focus back to the original trigger. */
  getRestoreFocus?: () => HTMLElement | null;
}

export function RevealTokenModal({ open, token, onClose, getRestoreFocus }: RevealTokenModalProps) {
  const [copied, setCopied] = useState(false);
  const [copyFailed, setCopyFailed] = useState(false);
  const [secondsLeft, setSecondsLeft] = useState(AUTO_CLOSE_SECONDS);

  const onCloseRef = useRef(onClose);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  useEffect(() => {
    onCloseRef.current = onClose;
  });

  useEffect(() => {
    if (!open) {
      setSecondsLeft(AUTO_CLOSE_SECONDS);
      setCopied(false);
      setCopyFailed(false);
      return;
    }
    intervalRef.current = setInterval(() => {
      setSecondsLeft((s) => {
        if (s <= 1) {
          if (intervalRef.current) {
            clearInterval(intervalRef.current);
            intervalRef.current = null;
          }
          onCloseRef.current();
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [open]);

  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(token)
      .then(() => {
        setCopyFailed(false);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      })
      .catch(() => {
        setCopied(false);
        setCopyFailed(true);
      });
  }, [token]);

  return (
    <Dialog.Root open={open} onOpenChange={(o) => !o && onClose()}>
      <Dialog.Content
        maxWidth="520px"
        data-testid="sat-reveal-modal"
        onCloseAutoFocus={(e) => {
          const target = getRestoreFocus?.();
          if (target) {
            e.preventDefault();
            target.focus({preventScroll: true});
          }
        }}
      >
        <Dialog.Title size="4" weight="medium">
          {LABELS.TITLE}
        </Dialog.Title>
        <VisuallyHidden>
          <Dialog.Description>{LABELS.DIALOG_DESCRIPTION}</Dialog.Description>
        </VisuallyHidden>

        <Callout.Root color="blue" size="1" mb="4" mt="4" role="alert">
          <Callout.Icon>
            <Info size={16} />
          </Callout.Icon>
          <Callout.Text>
            <strong>{LABELS.WARNING}</strong>
          </Callout.Text>
        </Callout.Root>

        {/* Screen reader announcement for copy success */}
        <VisuallyHidden>
          <span role="status" aria-live="polite">
            {copied ? LABELS.COPY_ANNOUNCEMENT : ''}
          </span>
        </VisuallyHidden>

        <Box mb="4">
          <Text as="label" size="1" weight="medium" color="gray" mb="1">
            {LABELS.TOKEN_LABEL}
          </Text>
          <Flex
            align="center"
            gap="2"
            p="3"
            mt="1"
            style={{
              background: 'var(--gray-2)',
              border: '1px solid var(--gray-5)',
              borderRadius: '6px',
            }}
          >
            <code
              style={{
                flex: 1,
                fontSize: '13px',
                fontFamily: "'SF Mono', Monaco, 'Cascadia Code', monospace",
                wordBreak: 'break-all',
              }}
              data-testid="sat-token-value"
            >
              {token}
            </code>
            <Button
              variant="solid"
              size="2"
              onClick={handleCopy}
              data-testid="sat-token-copy"
              style={{ flexShrink: 0 }}
            >
              {copied ? <Check size={14} /> : <Copy size={14} />}
              {copied ? LABELS.COPIED_BUTTON : LABELS.COPY_BUTTON}
            </Button>
          </Flex>
          {copyFailed && (
            <Text
              as="p"
              size="1"
              color="red"
              role="alert"
              mt="2"
              data-testid="sat-copy-failed"
            >
              {LABELS.COPY_FAILED}
            </Text>
          )}
        </Box>

        <Flex justify="between" align="center">
          <Text size="1" color="gray" data-testid="sat-reveal-countdown">
            {LABELS.AUTO_CLOSE_NOTICE(secondsLeft)}
          </Text>
          <Button
            variant="soft"
            color="gray"
            size="2"
            onClick={onClose}
            data-testid="sat-reveal-done"
          >
            {LABELS.DONE_BUTTON}
          </Button>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
