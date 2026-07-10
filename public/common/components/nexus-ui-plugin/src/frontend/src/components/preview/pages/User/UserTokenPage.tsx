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
import {
  Badge,
  Box,
  Button,
  Callout,
  Card,
  Code,
  Dialog,
  Flex,
  Heading,
  IconButton,
  Separator,
  Spinner,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import {
  Check,
  CheckCircle,
  Copy,
  Key,
  KeyRound,
} from 'lucide-react';
import Axios from 'axios';
import { APIConstants } from '../../../../constants/APIConstants';
import { ExtJS } from '../../../../interface/ExtJS';
import { useToast } from '../../shared';
import { ConfirmDialog } from '../../shared/form';

// ---------------------------------------------------------------------------
// API helpers
// ---------------------------------------------------------------------------

const USER_TOKEN_BASE = '/service/rest/internal/current-user/user-token';
const ATTRIBUTES_URL = `/${APIConstants.REST.USER_TOKEN_TIMESTAMP}`;
const USER_TOKENS_SETTINGS = '/service/rest/v1/security/user-tokens';

interface UserTokenAttributes {
  expirationTimeTimestamp?: string; // epoch ms as string
}

type AttributesResult =
  | { kind: 'present'; attributes: UserTokenAttributes }
  | { kind: 'absent' }
  | { kind: 'expired' };

async function fetchAttributes(): Promise<AttributesResult> {
  try {
    const res = await Axios.get<UserTokenAttributes>(ATTRIBUTES_URL);
    return { kind: 'present', attributes: res.data ?? {} };
  } catch (err: unknown) {
    if (Axios.isAxiosError(err)) {
      if (err.response?.status === 404) return { kind: 'absent' };
      if (err.response?.status === 410) return { kind: 'expired' };
    }
    throw err;
  }
}

async function fetchTokensEnabled(): Promise<boolean> {
  const res = await Axios.get(USER_TOKENS_SETTINGS);
  return res.data?.enabled ?? true;
}

function tokenUrl(authToken: string): string {
  return `${USER_TOKEN_BASE}?authToken=${btoa(authToken)}`;
}

// ---------------------------------------------------------------------------
// CopyField — shared copy button helper
// ---------------------------------------------------------------------------

function CopyField({ label, value }: { label: string; value: string }) {
  const [copied, setCopied] = useState(false);
  const handleCopy = () => {
    navigator.clipboard.writeText(value);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  return (
    <Box>
      <Text size="1" color="gray" style={{ display: 'block', marginBottom: '4px' }}>
        {label}
      </Text>
      <Flex align="center" gap="2">
        <Code
          variant="soft"
          size="2"
          style={{ flex: 1, wordBreak: 'break-all' }}
          data-testid={`copy-field-${label.toLowerCase().replace(/\s+/g, '-')}`}
        >
          {value}
        </Code>
        <Tooltip content={copied ? 'Copied!' : 'Copy to clipboard'}>
          <IconButton
            variant="ghost"
            size="2"
            onClick={handleCopy}
            aria-label={`Copy ${label}`}
            data-testid={`copy-btn-${label.toLowerCase().replace(/\s+/g, '-')}`}
          >
            {copied ? <Check size={14} color="var(--green-9)" /> : <Copy size={14} />}
          </IconButton>
        </Tooltip>
      </Flex>
    </Box>
  );
}

// ---------------------------------------------------------------------------
// TokenRevealDialog
// ---------------------------------------------------------------------------

const REVEAL_TIMEOUT_SECONDS = 60;

interface TokenRevealDialogProps {
  nameCode: string;
  passCode: string;
  onClose: () => void;
}

function TokenRevealDialog({ nameCode, passCode, onClose }: TokenRevealDialogProps) {
  const [seconds, setSeconds] = useState(REVEAL_TIMEOUT_SECONDS);

  useEffect(() => {
    const interval = setInterval(() => {
      setSeconds((s) => {
        if (s <= 1) {
          clearInterval(interval);
          onClose();
          return 0;
        }
        return s - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [onClose]);

  const mavenSnippet = `<server>\n  <id>\${server}</id>\n  <username>${nameCode}</username>\n  <password>${passCode}</password>\n</server>`;
  const base64Value = btoa(`${nameCode}:${passCode}`);

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open) onClose(); }}>
      <Dialog.Content maxWidth="680px" data-testid="token-reveal-dialog">
        <Dialog.Title>User Token</Dialog.Title>

        <Callout.Root color="amber" mb="4">
          <Callout.Text>
            Keep this token secret. Treat it like a password. It auto-hides in {seconds} seconds.
          </Callout.Text>
        </Callout.Root>

        <Flex direction="column" gap="4">
          <CopyField label="User Code (Username)" value={nameCode} />
          <CopyField label="Passcode (Password)" value={passCode} />

          <Separator size="4" />

          <Box>
            <Text size="1" color="gray" weight="medium" style={{ display: 'block', marginBottom: '6px' }}>
              Maven / Gradle settings.xml
            </Text>
            <Box
              style={{
                background: 'var(--gray-2)',
                borderRadius: 'var(--radius-2)',
                padding: 'var(--space-3)',
                fontFamily: 'var(--font-mono, monospace)',
                fontSize: '13px',
                whiteSpace: 'pre',
                overflowX: 'auto',
              }}
              data-testid="maven-snippet"
            >
              {mavenSnippet}
            </Box>
            <CopyField label="Copy Maven snippet" value={mavenSnippet} />
          </Box>

          <Separator size="4" />

          <CopyField label="Base64 encoded" value={base64Value} />

          <Text size="1" color="gray" data-testid="countdown-text">
            Auto-hides in {seconds} seconds
          </Text>
        </Flex>

        <Flex justify="end" mt="4">
          <Dialog.Close>
            <Button variant="soft" color="gray" onClick={onClose}>
              Close
            </Button>
          </Dialog.Close>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}

// ---------------------------------------------------------------------------
// UserTokenPage
// ---------------------------------------------------------------------------

type PageState = 'loading' | 'disabled' | 'no-token' | 'has-token' | 'expired-token' | 'error';

interface RevealedToken {
  nameCode: string;
  passCode: string;
}

export function UserTokenPage() {
  const toast = useToast();
  const toastRef = useRef(toast);
  toastRef.current = toast;

  const [pageState, setPageState] = useState<PageState>('loading');
  const [expirationTimeTimestamp, setExpirationTimeTimestamp] = useState<string | undefined>(undefined);
  const [revealedToken, setRevealedToken] = useState<RevealedToken | null>(null);
  const [showResetConfirm, setShowResetConfirm] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  // load has no deps so its identity is stable — prevents the toast→load→
  // toast→load re-render loop that caused infinite error spam (mrqu).
  const load = useCallback(async () => {
    setPageState('loading');
    try {
      const [enabled, result] = await Promise.all([fetchTokensEnabled(), fetchAttributes()]);
      if (!enabled) {
        setPageState('disabled');
        return;
      }
      if (result.kind === 'present') {
        setExpirationTimeTimestamp(result.attributes.expirationTimeTimestamp);
        setPageState('has-token');
      } else if (result.kind === 'expired') {
        setExpirationTimeTimestamp(undefined);
        setPageState('expired-token');
      } else {
        setExpirationTimeTimestamp(undefined);
        setPageState('no-token');
      }
    } catch {
      toastRef.current.error('Failed to load user token status.');
      setPageState('error');
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    load();
  }, [load]);

  const handleAccess = useCallback(async () => {
    setActionLoading(true);
    try {
      const authToken = await ExtJS.requestAuthenticationToken(
        'Please authenticate to access your user token.'
      );
      const res = await Axios.get(tokenUrl(authToken));
      setRevealedToken(res.data);
    } catch {
      toastRef.current.error('Failed to retrieve user token. Please check your credentials.');
    } finally {
      setActionLoading(false);
    }
  }, []);

  const handleGenerate = useCallback(async () => {
    setActionLoading(true);
    try {
      const authToken = await ExtJS.requestAuthenticationToken(
        'Please authenticate to generate a new user token.'
      );
      const res = await Axios.post(tokenUrl(authToken));
      setRevealedToken(res.data);
      await load();
    } catch {
      toastRef.current.error('Failed to generate user token. Please check your credentials.');
    } finally {
      setActionLoading(false);
    }
  }, [load]);

  const handleResetConfirm = useCallback(async () => {
    setShowResetConfirm(false);
    setActionLoading(true);
    try {
      const authToken = await ExtJS.requestAuthenticationToken(
        'Please authenticate to reset your user token.'
      );
      await Axios.delete(tokenUrl(authToken));
      toastRef.current.success('User token reset successfully.');
      await load();
    } catch {
      toastRef.current.error('Failed to reset user token. Please check your credentials.');
    } finally {
      setActionLoading(false);
    }
  }, [load]);

  const formatEpochMs = (epochMs: string) => {
    try {
      return new Date(Number(epochMs)).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return epochMs;
    }
  };

  const expiryBadge = (epochMs: string) => {
    if (!epochMs || isNaN(Number(epochMs))) return null;
    const diff = Number(epochMs) - Date.now();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    if (diff < 0) return <Badge color="red" variant="soft">Expired</Badge>;
    if (days < 7) return <Badge color="amber" variant="soft">Expires in {days}d</Badge>;
    return null;
  };

  // State 0: Loading
  if (pageState === 'loading') {
    return (
      <Flex direction="column" align="center" justify="center" gap="3" style={{ minHeight: 200 }} data-testid="user-token-page-loading">
        <Spinner size="3" />
        <Text size="2" color="gray">Loading your user token status...</Text>
      </Flex>
    );
  }

  return (
    <Box style={{ maxWidth: 640, margin: '0 auto', padding: 'var(--space-5)' }} data-testid="user-token-page">
      {/* Page header */}
      <Flex align="center" gap="3" mb="4">
        <Key size={24} color="var(--accent-9)" />
        <Box>
          <Heading size="5">User Token</Heading>
          <Text size="2" color="gray">Personal access token for API authentication</Text>
        </Box>
      </Flex>

      {/* State 1: Disabled */}
      {pageState === 'disabled' && (
        <Callout.Root color="blue" data-testid="tokens-disabled-callout">
          <Callout.Text>
            User tokens are not enabled for this instance. Contact your administrator to enable user tokens.
          </Callout.Text>
        </Callout.Root>
      )}

      {/* State: API error (non-404 failure — 401, 403, 500, network) */}
      {pageState === 'error' && (
        <Callout.Root color="red" data-testid="error-state">
          <Callout.Text>
            Failed to load your user token status. This may be a temporary issue.
          </Callout.Text>
          <Box mt="3">
            <Button variant="soft" color="red" onClick={load} data-testid="retry-btn">
              Retry
            </Button>
          </Box>
        </Callout.Root>
      )}

      {/* States 2, 3, 4: Token Status card */}
      {(pageState === 'no-token' || pageState === 'has-token' || pageState === 'expired-token') && (
        <>
          <Card mb="4" data-testid="token-status-card">
            <Heading size="3" mb="3">Token Status</Heading>

            {pageState === 'no-token' && (
              <Flex direction="column" align="center" gap="3" py="5" data-testid="no-token-state">
                <KeyRound size={48} color="var(--gray-8)" />
                <Heading size="3" color="gray">No token generated</Heading>
                <Text size="2" color="gray" align="center">
                  You don&apos;t have a user token yet. Generate one to use with Maven,
                  Gradle, npm, and other build tools.
                </Text>
                <Button
                  variant="solid"
                  color="blue"
                  highContrast
                  onClick={handleGenerate}
                  disabled={actionLoading}
                  data-testid="generate-token-btn"
                >
                  {actionLoading && <Spinner size="1" />}
                  Generate Token
                </Button>
              </Flex>
            )}

            {pageState === 'has-token' && (
              <Flex direction="column" gap="3" data-testid="has-token-state">
                <Flex align="center" gap="2">
                  <CheckCircle size={20} color="var(--green-9)" />
                  <Text weight="medium" color="green">Token active</Text>
                </Flex>
                {expirationTimeTimestamp && (
                  <Flex direction="column" gap="1">
                    <Flex gap="2" align="center" data-testid="expires-row">
                      <Text size="2" color="gray" style={{ minWidth: 70 }}>Expires</Text>
                      <Text size="2">{formatEpochMs(expirationTimeTimestamp)}</Text>
                      {expiryBadge(expirationTimeTimestamp)}
                    </Flex>
                  </Flex>
                )}
                <Flex gap="2" mt="1">
                  <Button
                    variant="soft"
                    color="green"
                    onClick={handleAccess}
                    disabled={actionLoading}
                    data-testid="access-token-btn"
                  >
                    {actionLoading && <Spinner size="1" />}
                    Access Token
                  </Button>
                  <Button
                    variant="soft"
                    color="red"
                    onClick={() => setShowResetConfirm(true)}
                    disabled={actionLoading}
                    data-testid="reset-token-btn"
                  >
                    Reset Token
                  </Button>
                </Flex>
              </Flex>
            )}

            {pageState === 'expired-token' && (
              <Flex direction="column" gap="3" data-testid="expired-token-state">
                <Callout.Root color="red">
                  <Callout.Text>
                    Your user token has expired. Generate a new one to restore API access.
                  </Callout.Text>
                </Callout.Root>
                <Flex gap="2">
                  <Button
                    variant="solid"
                    color="blue"
                    highContrast
                    onClick={handleGenerate}
                    disabled={actionLoading}
                    data-testid="generate-token-btn"
                  >
                    {actionLoading && <Spinner size="1" />}
                    Generate Token
                  </Button>
                  <Button
                    variant="soft"
                    color="red"
                    onClick={() => setShowResetConfirm(true)}
                    disabled={actionLoading}
                    data-testid="reset-token-btn"
                  >
                    Reset Token
                  </Button>
                </Flex>
              </Flex>
            )}
          </Card>

          <Card data-testid="what-is-token-card">
            <Heading size="3" mb="2">What is a User Token?</Heading>
            <Text size="2" color="gray">
              Your user token lets you authenticate to Nexus Repository without using your
              username and password. Use it with Maven, Gradle, npm, Docker, and other tools.
            </Text>
          </Card>
        </>
      )}

      {/* State 5: Token Revealed modal */}
      {revealedToken && (
        <TokenRevealDialog
          nameCode={revealedToken.nameCode}
          passCode={revealedToken.passCode}
          onClose={() => setRevealedToken(null)}
        />
      )}

      {/* State 6: Reset Confirmation */}
      <ConfirmDialog
        open={showResetConfirm}
        onOpenChange={setShowResetConfirm}
        title="Reset User Token"
        message="This will permanently delete your current token. Any build tools using the existing token will stop working immediately. You can generate a new token after resetting."
        confirmLabel="Reset Token"
        variant="danger"
        onConfirm={handleResetConfirm}
        testId="reset-token-dialog"
      />
    </Box>
  );
}

export default UserTokenPage;
