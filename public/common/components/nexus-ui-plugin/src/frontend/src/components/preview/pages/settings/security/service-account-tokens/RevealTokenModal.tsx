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

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Box, Flex, Text, Dialog, Callout, Button, VisuallyHidden, Separator } from '@radix-ui/themes';
import { Info, Copy, Check, ExternalLink } from 'lucide-react';

import { SERVICE_ACCOUNT_TOKENS_STRINGS } from './strings';

const LABELS = SERVICE_ACCOUNT_TOKENS_STRINGS.REVEAL_MODAL;

// btoa() only accepts Latin-1, so a bare btoa(`${name}:${token}`) throws a
// DOMException when the account name contains non-ASCII characters (accents,
// CJK, emoji). Round-trip through UTF-8 first so those encode correctly.
// Mirrors safeBtoa in ../ldap/useLdapApi.ts.
function safeBtoa(str: string): string {
  return btoa(unescape(encodeURIComponent(str)));
}

interface RevealTokenModalProps {
  open: boolean;
  name: string;
  token: string;
  onClose: () => void;
  /** Resolves the element to focus when the modal closes. Useful when this
   *  modal is opened after another (e.g. the Create modal) finishes — Radix's
   *  default focus-restore points at the previous modal's now-unmounted form,
   *  so we need to explicitly direct focus back to the original trigger. */
  getRestoreFocus?: () => HTMLElement | null;
}

export function RevealTokenModal({ open, name, token, onClose, getRestoreFocus }: RevealTokenModalProps) {
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [copyFailed, setCopyFailed] = useState(false);
  const [encodedRevealed, setEncodedRevealed] = useState(false);

  const encodedCredential = useMemo(() => {
    try {
      return safeBtoa(`${name}:${token}`);
    } catch {
      return '';
    }
  }, [name, token]);

  // Reset the encoded reveal whenever the credential changes, so a second token
  // shown without a full close/open cycle doesn't inherit the prior reveal state.
  useEffect(() => {
    setEncodedRevealed(false);
  }, [name, token]);

  // Reset transient state whenever the modal closes, so reopening for a new
  // token starts clean.
  useEffect(() => {
    if (!open) {
      setCopiedField(null);
      setCopyFailed(false);
      setEncodedRevealed(false);
    }
  }, [open]);

  const handleCopy = useCallback((text: string, field: string) => {
    navigator.clipboard.writeText(text)
      .then(() => {
        setCopyFailed(false);
        setCopiedField(field);
        setTimeout(() => setCopiedField(null), 2000);
      })
      .catch(() => {
        setCopiedField(null);
        setCopyFailed(true);
      });
  }, []);

  const copyAnnouncement =
    copiedField === 'username'
      ? LABELS.USERNAME_COPY_ANNOUNCEMENT
      : copiedField === 'password'
      ? LABELS.PASSWORD_COPY_ANNOUNCEMENT
      : copiedField === 'token'
      ? LABELS.COPY_ANNOUNCEMENT
      : copiedField === 'encoded'
      ? LABELS.ENCODED_COPY_ANNOUNCEMENT
      : '';

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
          <Dialog.Description>{LABELS.WARNING}</Dialog.Description>
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
            {copyAnnouncement}
          </span>
        </VisuallyHidden>

        {/* Basic authentication section */}
        <Box mb="3">
          <Text as="p" size="2" weight="medium" mb="2">
            {LABELS.BASIC_SECTION_LABEL}
          </Text>

          {/* Username field */}
          <Box mb="2">
            <Text as="p" size="1" weight="medium" color="gray" className="sat-reveal__section-label">
              {LABELS.USERNAME_LABEL}
            </Text>
            <div className="sat-reveal__cred-box">
              <code className="sat-reveal__cred-code" data-testid="sat-basic-username">
                {name}
              </code>
              <Button
                variant="solid"
                size="2"
                onClick={() => handleCopy(name, 'username')}
                data-testid="sat-basic-username-copy"
                aria-label={LABELS.COPY_USERNAME_ARIA}
                className="sat-reveal__copy-btn"
              >
                {copiedField === 'username' ? <Check size={14} /> : <Copy size={14} />}
                {copiedField === 'username' ? LABELS.COPIED_BUTTON : LABELS.COPY_BUTTON}
              </Button>
            </div>
          </Box>

          {/* Password field */}
          <Box mb="2">
            <Text as="p" size="1" weight="medium" color="gray" className="sat-reveal__section-label">
              {LABELS.PASSWORD_LABEL}
            </Text>
            <div className="sat-reveal__cred-box">
              <code className="sat-reveal__cred-code" data-testid="sat-basic-password">
                {token}
              </code>
              <Button
                variant="solid"
                size="2"
                onClick={() => handleCopy(token, 'password')}
                data-testid="sat-basic-password-copy"
                aria-label={LABELS.COPY_PASSWORD_ARIA}
                className="sat-reveal__copy-btn"
              >
                {copiedField === 'password' ? <Check size={14} /> : <Copy size={14} />}
                {copiedField === 'password' ? LABELS.COPIED_BUTTON : LABELS.COPY_BUTTON}
              </Button>
            </div>
          </Box>
        </Box>

        <Separator size="4" mb="3" />

        {/* Bearer token section */}
        <Box mb="3">
          <Text as="p" size="2" weight="medium" mb="2">
            {LABELS.BEARER_SECTION_LABEL}
          </Text>
          <Box mb="2">
            <Text as="p" size="1" weight="medium" color="gray" className="sat-reveal__section-label">
              {LABELS.TOKEN_LABEL}
            </Text>
            <div className="sat-reveal__cred-box">
              <code className="sat-reveal__cred-code" data-testid="sat-token-value">
                {token}
              </code>
              <Button
                variant="solid"
                size="2"
                onClick={() => handleCopy(token, 'token')}
                data-testid="sat-token-copy"
                aria-label={LABELS.COPY_TOKEN_ARIA}
                className="sat-reveal__copy-btn"
              >
                {copiedField === 'token' ? <Check size={14} /> : <Copy size={14} />}
                {copiedField === 'token' ? LABELS.COPIED_BUTTON : LABELS.COPY_BUTTON}
              </Button>
            </div>
          </Box>
        </Box>

        {copyFailed && (
          <Text
            as="p"
            size="1"
            color="red"
            role="alert"
            mt="2"
            mb="3"
            data-testid="sat-copy-failed"
          >
            {LABELS.COPY_FAILED}
          </Text>
        )}

        <Separator size="4" mb="3" />

        {/* Encoded reveal section */}
        <Box mb="4">
          {!encodedRevealed ? (
            <Button
              variant="ghost"
              size="2"
              onClick={() => setEncodedRevealed(true)}
              data-testid="sat-basic-encoded-reveal"
            >
              {LABELS.ENCODED_REVEAL_BUTTON}
            </Button>
          ) : (
            <Box>
              <Text as="p" size="1" weight="medium" color="gray" className="sat-reveal__section-label">
                {LABELS.ENCODED_LABEL}
              </Text>
              <div className="sat-reveal__cred-box">
                <code className="sat-reveal__cred-code" data-testid="sat-basic-encoded-value">
                  {LABELS.ENCODED_VALUE(encodedCredential)}
                </code>
                <Button
                  variant="solid"
                  size="2"
                  onClick={() => handleCopy(`Authorization: Basic ${encodedCredential}`, 'encoded')}
                  data-testid="sat-basic-encoded-copy"
                  aria-label={LABELS.COPY_ENCODED_ARIA}
                  className="sat-reveal__copy-btn"
                >
                  {copiedField === 'encoded' ? <Check size={14} /> : <Copy size={14} />}
                  {copiedField === 'encoded' ? LABELS.COPIED_BUTTON : LABELS.COPY_BUTTON}
                </Button>
              </div>
              <Text as="p" size="1" color="gray" className="sat-reveal__note">
                {LABELS.ENCODED_NOTE}
              </Text>
            </Box>
          )}
        </Box>

        {/* Docs link */}
        <Flex justify="end" mb="4">
          <a
            href={LABELS.DOCS_LINK_HREF}
            target="_blank"
            rel="noopener noreferrer"
            className="sat-reveal__docs-link"
            data-testid="sat-docs-link"
          >
            {LABELS.DOCS_LINK}
            <ExternalLink size={12} aria-hidden="true" />
          </a>
        </Flex>

        <Flex justify="end" align="center">
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
