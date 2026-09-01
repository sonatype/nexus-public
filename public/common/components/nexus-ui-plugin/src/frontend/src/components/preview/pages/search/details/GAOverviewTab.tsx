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

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Box, Button, Card, Code, Flex, Grid, Heading, Select, Table, Text, Tooltip } from '@radix-ui/themes';
import { Copy } from 'lucide-react';

import type { GADetail } from '../core';
import { parseGaCoordinates } from './detailHelpers';
import { getDependencySnippets } from './dependencySnippets/registry';
import { getFormatLabel } from './dependencySnippets/formatLabel';
import { trackSnippetCopy } from './dependencySnippets/trackSnippetCopy';
import type { SnippetComponentModel } from './dependencySnippets/types';

interface GAOverviewTabProps {
  detail: GADetail;
  selectedVersion: string | null;
  /**
   * Repository names holding the selected version.
   *
   * Passed in rather than read off `detail.repositories`, which is now always empty: it used to be
   * aggregated by walking every page of /v1/search, and that walk is gone (NEXUS-54201 /
   * NEXUS-54220). The caller sources this from the same per-version endpoint the Repositories tab
   * uses, so the "Repository" row and the snippet's registry URL agree with that tab.
   */
  repositories?: readonly string[];
  /** The selected version's most recent asset timestamp, or null if none carries one. */
  lastUpdated?: string | null;
}

function formatDate(dateString: string | null | undefined): string {
  if (!dateString) return '—';
  try {
    const date = new Date(dateString);
    if (Number.isNaN(date.getTime())) return '—';
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  } catch {
    return '—';
  }
}

/**
 * GAOverviewTab - Overview information for a GA.
 *
 * Renders per-format dependency snippets (ported from the Classic UI) and a Component Details
 * summary. The snippet set is chosen by repository format, so an npm component shows npm/Yarn
 * commands rather than a Maven pom.xml fragment. Formats with no generator show no snippet section.
 */
export function GAOverviewTab({
  detail,
  selectedVersion,
  repositories = [],
  lastUpdated = null,
}: GAOverviewTabProps) {
  const { gaId, format } = detail;
  const [copied, setCopied] = useState(false);
  const [selectedName, setSelectedName] = useState<string | null>(null);
  const copiedTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Clear any pending copied-reset timer when the tab unmounts so it can't fire afterwards.
  useEffect(
    () => () => {
      if (copiedTimerRef.current) {
        clearTimeout(copiedTimerRef.current);
      }
    },
    [],
  );

  // Navigating to a component of a different format exposes a different snippet set, so drop any
  // prior selection and fall back to that format's default rather than a stale (or absent) type.
  // Adjusting state during render (React's recommended pattern) avoids the one-frame flicker an
  // effect would leave when the old and new format share a snippet displayName.
  const [prevFormat, setPrevFormat] = useState(format);
  if (prevFormat !== format) {
    setPrevFormat(format);
    setSelectedName(null);
  }

  const { group, name: parsedName } = parseGaCoordinates(gaId);
  const name = parsedName || detail.displayName;
  // `??`, not `||`: '' is the valid selected version of a versionless format (raw), and there is
  // no `detail.versions[0]` to fall back to any more regardless.
  const ver = selectedVersion ?? '';

  const formatLabel = getFormatLabel(format);
  const repositoryName = repositories[0];
  const repositoryNames = repositories.join(', ');

  const snippets = useMemo(() => {
    const component: SnippetComponentModel = { format, group, name, version: ver, repositoryName };
    // The Overview is component-level, so there is no specific asset — matching Classic, which
    // emits no extension here. Synthesizing a jar extension for Maven would wrongly append @jar
    // to Gradle/PURL/Leiningen coordinates and break transitive resolution.
    return getDependencySnippets(format, component, undefined);
  }, [format, group, name, ver, repositoryName]);

  // Require a resolvable version: several generators (npm, pypi, maven, …) don't guard on an empty
  // version, so without this gate a version-less component would emit a truncated coordinate.
  const hasSnippets = snippets.length > 0 && !!ver;

  // The selected snippet, defaulting to the first one (mirrors the Classic combo behavior).
  const selectedSnippet = snippets.find((s) => s.displayName === selectedName) ?? snippets[0];

  const handleSelect = useCallback((displayName: string) => {
    setSelectedName(displayName);
    setCopied(false);
  }, []);

  const handleCopy = useCallback(
    (displayName: string, snippetText: string) => {
      // writeText can reject (permissions, insecure context); swallow it so it isn't an
      // unhandled rejection — the copy is best-effort and the analytics event still fires.
      navigator.clipboard.writeText(snippetText).catch(() => {});
      trackSnippetCopy(format, displayName);
      setCopied(true);
      if (copiedTimerRef.current) {
        clearTimeout(copiedTimerRef.current);
      }
      copiedTimerRef.current = setTimeout(() => setCopied(false), 2000);
    },
    [format],
  );

  const detailRows: Array<{ label: string; value: string; mono?: boolean }> = [
    { label: 'Last Updated', value: formatDate(lastUpdated) },
    { label: 'Format', value: formatLabel },
    ...(group ? [{ label: 'Group', value: group }] : []),
    ...(name ? [{ label: 'Name', value: name }] : []),
    ...(ver ? [{ label: 'Version', value: ver }] : []),
    ...(repositoryNames ? [{ label: 'Repository', value: repositoryNames, mono: true }] : []),
  ];

  return (
    <Flex direction="column" gap="6">
      <Grid columns={{ initial: '1', md: hasSnippets ? '2fr 1fr' : '1' }} gap="4">
        {hasSnippets && selectedSnippet && (
          <Card size="1">
            <Box p="4">
              <Heading size="4" mb="4">
                Dependencies
              </Heading>
              <Flex align="center" gap="2" mb="3">
                {/* Deliberately shows one snippet at a time behind this picker rather than stacking
                    them all: some formats (e.g. Maven/Java) expose many snippets, and stacking would
                    force the user to scroll past a long wall of code to reach the details below.
                    Radix Select keeps this consistent with every other dropdown in the Preview UI. */}
                <Select.Root value={selectedSnippet.displayName} onValueChange={handleSelect} size="2">
                  <Select.Trigger aria-label="Select dependency snippet" style={{ flex: 1 }} />
                  <Select.Content>
                    {snippets.map((snippet) => (
                      <Select.Item key={snippet.displayName} value={snippet.displayName}>
                        {snippet.displayName}
                      </Select.Item>
                    ))}
                  </Select.Content>
                </Select.Root>
                <Tooltip content={copied ? 'Copied!' : 'Copy'}>
                  <Button
                    variant="soft"
                    color="blue"
                    size="2"
                    aria-label={`Copy ${selectedSnippet.displayName} snippet`}
                    onClick={() => handleCopy(selectedSnippet.displayName, selectedSnippet.snippetText)}
                  >
                    <Copy size={14} />
                  </Button>
                </Tooltip>
              </Flex>
              {selectedSnippet.description && (
                <Text as="div" size="1" color="gray" mb="2">
                  {selectedSnippet.description}
                </Text>
              )}
              <Box p="4" style={{ backgroundColor: 'var(--gray-2)', borderRadius: '6px' }}>
                <Code
                  size="2"
                  color="gray"
                  variant="ghost"
                  style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-all', display: 'block', overflowX: 'auto' }}
                >
                  {selectedSnippet.snippetText}
                </Code>
              </Box>
            </Box>
          </Card>
        )}

        <Card size="1">
          <Box p="4">
            <Heading size="4" mb="3">
              Component Details
            </Heading>
            <Table.Root variant="ghost" size="1">
              <Table.Body>
                {detailRows.map((row) => (
                  <Table.Row key={row.label}>
                    <Table.Cell>
                      <Text size="2" color="gray">
                        {row.label}
                      </Text>
                    </Table.Cell>
                    <Table.Cell>
                      <Text
                        size="2"
                        weight="medium"
                        style={row.mono ? { fontFamily: 'monospace', fontSize: '11px' } : undefined}
                      >
                        {row.value}
                      </Text>
                    </Table.Cell>
                  </Table.Row>
                ))}
              </Table.Body>
            </Table.Root>
          </Box>
        </Card>
      </Grid>
    </Flex>
  );
}

export default GAOverviewTab;
