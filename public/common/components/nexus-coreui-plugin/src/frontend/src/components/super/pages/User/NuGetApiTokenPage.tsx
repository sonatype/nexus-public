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

import React, { useState, useCallback } from 'react';
import {
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
import { Check, Copy, Package } from 'lucide-react';
import Axios from 'axios';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import { useToast } from '../../../shared';
import { ConfirmDialog } from '../../shared/form';

// ---------------------------------------------------------------------------
// API helpers
// ---------------------------------------------------------------------------

const NUGET_API_KEY_BASE = '/service/rest/internal/nuget-api-key';

function nugetApiKeyUrl(authToken: string): string {
  return `${NUGET_API_KEY_BASE}?authToken=${btoa(authToken)}`;
}

// ---------------------------------------------------------------------------
// CopyField — inline copy helper (matches UserTokenPage.tsx)
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
      {label && (
        <Text size="1" color="gray" style={{ display: 'block', marginBottom: '4px' }}>
          {label}
        </Text>
      )}
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
            aria-label={`Copy ${label || 'value'}`}
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
// NuGetRevealDialog
// ---------------------------------------------------------------------------

interface NuGetRevealDialogProps {
  apiKey: string;
  userId: string;
  onClose: () => void;
}

function NuGetRevealDialog({ apiKey, userId, onClose }: NuGetRevealDialogProps) {
  const repoUrl = `${window.location.origin}/repository/nuget-hosted/index.json`;
  const dotnetCmd = `dotnet nuget add source "Nexus" \\\n  --source ${repoUrl} \\\n  --username ${userId} \\\n  --password ${apiKey}`;

  return (
    <Dialog.Root open onOpenChange={(open) => { if (!open) onClose(); }}>
      <Dialog.Content maxWidth="520px" data-testid="nuget-reveal-dialog">
        <Dialog.Title>NuGet API Key</Dialog.Title>

        <Callout.Root color="amber" mb="4">
          <Callout.Text>
            Keep this key secret. Store it in your NuGet configuration, not in source code.
          </Callout.Text>
        </Callout.Root>

        <Flex direction="column" gap="4">
          <CopyField label="Your API Key" value={apiKey} />

          <Separator size="4" />

          <Box>
            <Text size="2" weight="medium" mb="1" style={{ display: 'block' }}>
              dotnet nuget add source command
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
              data-testid="dotnet-command"
            >
              {dotnetCmd}
            </Box>
            <CopyField label="Copy command" value={dotnetCmd} />
          </Box>
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
// NuGetApiTokenPage
// ---------------------------------------------------------------------------

const API_KEY_PLACEHOLDER = '{API_KEY}';

export function NuGetApiTokenPage() {
  const toast = useToast();
  const user = ExtJS.useUser();
  const userId = user?.id ?? user?.userId ?? '{userId}';

  const [apiKey, setApiKey] = useState<string | null>(null);
  const [showReveal, setShowReveal] = useState(false);
  const [showResetConfirm, setShowResetConfirm] = useState(false);
  const [accessLoading, setAccessLoading] = useState(false);
  const [resetLoading, setResetLoading] = useState(false);

  const repoUrl = `${typeof window !== 'undefined' ? window.location.origin : ''}/repository/nuget-hosted/index.json`;
  const displayKey = apiKey ?? API_KEY_PLACEHOLDER;

  const dotnetCmdPreview = `dotnet nuget add source "Nexus" \\\n  --source ${repoUrl} \\\n  --username ${userId} \\\n  --password ${displayKey}`;
  const nugetCmdPreview = `nuget.exe setapikey ${displayKey} \\\n  -Source ${repoUrl}`;

  const handleAccess = useCallback(async () => {
    setAccessLoading(true);
    try {
      const authToken = await ExtJS.requestAuthenticationToken(
        'Please authenticate to access your NuGet API key.'
      );
      const res = await Axios.get(nugetApiKeyUrl(authToken));
      setApiKey(res.data?.apiKey ?? res.data);
      setShowReveal(true);
    } catch {
      toast.error('Failed to retrieve NuGet API key. Please check your credentials.');
    } finally {
      setAccessLoading(false);
    }
  }, [toast]);

  const handleResetConfirm = useCallback(async () => {
    setShowResetConfirm(false);
    setResetLoading(true);
    try {
      const authToken = await ExtJS.requestAuthenticationToken(
        'Please authenticate to reset your NuGet API key.'
      );
      await Axios.delete(nugetApiKeyUrl(authToken));
      setApiKey(null);
      toast.success('API key reset. Generate a new one.');
    } catch {
      toast.error('Failed to reset NuGet API key. Please check your credentials.');
    } finally {
      setResetLoading(false);
    }
  }, [toast]);

  return (
    <Box style={{ maxWidth: 640, margin: '0 auto', padding: 'var(--space-5)' }} data-testid="nuget-api-token-page">
      {/* Page header */}
      <Flex align="center" gap="3" mb="4">
        <Package size={24} color="var(--accent-9)" />
        <Box>
          <Heading size="5">NuGet API Key</Heading>
          <Text size="2" color="gray">Authentication key for NuGet package feeds</Text>
        </Box>
      </Flex>

      {/* Your API Key card */}
      <Card mb="4" data-testid="nuget-key-card">
        <Heading size="3" mb="2">Your API Key</Heading>
        <Text size="2" color="gray" mb="3" style={{ display: 'block' }}>
          Access your NuGet API key to authenticate NuGet package manager clients against this
          Nexus Repository instance.
        </Text>
        <Flex gap="2">
          <Button
            variant="solid"
            color="blue"
            onClick={handleAccess}
            disabled={accessLoading || resetLoading}
            data-testid="access-api-key-btn"
          >
            {accessLoading && <Spinner size="1" />}
            Access API Key
          </Button>
          <Button
            variant="soft"
            color="red"
            onClick={() => setShowResetConfirm(true)}
            disabled={accessLoading || resetLoading}
            data-testid="reset-api-key-btn"
          >
            {resetLoading && <Spinner size="1" />}
            Reset API Key
          </Button>
        </Flex>
      </Card>

      {/* Usage Instructions card */}
      <Card data-testid="usage-instructions-card">
        <Heading size="3" mb="3">Usage Instructions</Heading>

        <Flex direction="column" gap="4">
          <Box>
            <Text size="2" weight="medium" mb="1" style={{ display: 'block' }}>
              dotnet nuget CLI
            </Text>
            <Separator size="4" mb="2" />
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
              data-testid="dotnet-cmd-preview"
            >
              {dotnetCmdPreview}
            </Box>
            <Flex justify="end" mt="1">
              <CopyField label="dotnet command" value={dotnetCmdPreview} />
            </Flex>
          </Box>

          <Box>
            <Text size="2" weight="medium" mb="1" style={{ display: 'block' }}>
              nuget.exe CLI
            </Text>
            <Separator size="4" mb="2" />
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
              data-testid="nuget-cmd-preview"
            >
              {nugetCmdPreview}
            </Box>
            <Flex justify="end" mt="1">
              <CopyField label="nuget.exe command" value={nugetCmdPreview} />
            </Flex>
          </Box>
        </Flex>
      </Card>

      {/* Reveal modal */}
      {showReveal && apiKey && (
        <NuGetRevealDialog
          apiKey={apiKey}
          userId={userId}
          onClose={() => {
            setShowReveal(false);
            setApiKey(null);
          }}
        />
      )}

      {/* Reset Confirmation */}
      <ConfirmDialog
        open={showResetConfirm}
        onOpenChange={setShowResetConfirm}
        title="Reset NuGet API Key"
        message="This will permanently invalidate your current NuGet API key. Any NuGet clients using the existing key will fail to authenticate. You can generate a new key after resetting."
        confirmLabel="Reset Key"
        variant="danger"
        onConfirm={handleResetConfirm}
        testId="reset-nuget-key-dialog"
      />
    </Box>
  );
}

export default NuGetApiTokenPage;
