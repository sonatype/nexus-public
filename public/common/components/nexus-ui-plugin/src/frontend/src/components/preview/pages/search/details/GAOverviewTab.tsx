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
import { Badge, Box, Button, Card, Code, Flex, Grid, Heading, Select, Table, Text, Tooltip } from '@radix-ui/themes';
import { Check, Copy } from 'lucide-react';

import type { GADetail } from '../core';
import { formatDateTime } from '../../../shared';
import { parseGaCoordinates } from './detailHelpers';
import type { RepoRow } from './gaRepositoriesMachine';
import { getDependencySnippets } from './dependencySnippets/registry';
import { getFormatLabel } from './dependencySnippets/formatLabel';
import { trackSnippetCopy } from './dependencySnippets/trackSnippetCopy';
import type { SnippetComponentModel } from './dependencySnippets/types';
import { useComponentIntelligence } from './useComponentIntelligence';
import {
  buildIntelligenceRows,
  renderIntelligenceValue,
  type IntelligenceDetailRow,
} from './intelligenceRows';

interface GAOverviewTabProps {
  detail: GADetail;
  selectedVersion: string | null;
  /**
   * The repository currently selected in the header. The Overview never triggers the aggregate
   * repository walk, so detail.repositories is usually empty here; this is the reliable source for
   * the Component Details "Repository" row.
   */
  selectedRepository?: string | null;
  /**
   * Every repository serving the selected version, as resolved for the header's Repository facet.
   * When present, the Component Details "Repository" row lists all member repositories (the current
   * one first and flagged) instead of a single value. The list is informational only — the header
   * dropdown remains the sole control that switches the active repository.
   */
  repoRows?: readonly RepoRow[];
  /**
   * The selected version's most recent asset timestamp, from the per-version detail source. The
   * aggregate walk that once populated detail.versions was removed, so that array is always empty
   * here and cannot supply "Last Updated" — this prop is its only reliable source.
   */
  versionLastUpdated?: string | null;
}

/**
 * GAOverviewTab - Overview information for a GA.
 *
 * Renders per-format dependency snippets (ported from the Classic UI) and a Component Details
 * summary. The snippet set is chosen by repository format, so an npm component shows npm/Yarn
 * commands rather than a Maven pom.xml fragment. Formats with no generator show no snippet section.
 */
export function GAOverviewTab({ detail, selectedVersion, selectedRepository, repoRows, versionLastUpdated }: GAOverviewTabProps) {
  const { gaId, format, repositories } = detail;
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
  // `??`, not `||`: '' is the valid selected version of a versionless format (raw) and is falsy,
  // so `||` would fall through to detail.versions[0] and render the Version row / snippets for the
  // wrong version. Matches the guarded pattern documented in GADetailPage.tsx.
  const ver = selectedVersion ?? detail.versions[0]?.version ?? '';

  // Prefer the per-version timestamp threaded from useGADetail: detail.versions is always empty on
  // this page (its aggregate source was removed), so the object lookup below only ever supplies a
  // value on the other format detail pages that still populate it.
  const selectedVersionObj = detail.versions.find((v) => v.version === ver);
  const lastUpdated =
    versionLastUpdated ?? selectedVersionObj?.lastUpdated ?? detail.versions[0]?.lastUpdated;
  const formatLabel = getFormatLabel(format);
  // Feeds the dependency snippets (e.g. the npm/PyPI registry hint). detail.repositories is empty
  // here, so derive from the header's selected repository, then the first non-group member repo,
  // before falling back to the (usually empty) aggregate list.
  const repositoryName =
    selectedRepository ??
    repoRows?.find((r) => r.type !== 'group')?.repositoryName ??
    repositories?.[0]?.name;
  const repositoryNames = repositories?.map((r) => r.name).join(', ') || '';
  // Always show a Repository row. Prefer the header's selected repository (the only value reliably
  // available on Overview), fall back to the aggregate list if it happens to be loaded, then to an
  // em dash so the row is never silently dropped.
  const repositoryValue = selectedRepository || repositoryNames || '—';

  // When the header has resolved the repositories serving this version, list every member repo in
  // the Repository row (the selected one first and flagged) instead of the single string value.
  // Group repositories are surfaced by the header's Group facet, so exclude them here. Read-only:
  // the header dropdown remains the control that switches the active repository.
  const memberRepoRows = useMemo(() => {
    const members = (repoRows ?? []).filter((r) => r.type !== 'group');
    return [...members].sort((a, b) => {
      if (a.repositoryName === selectedRepository) return -1;
      if (b.repositoryName === selectedRepository) return 1;
      return 0;
    });
  }, [repoRows, selectedRepository]);

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

  // Repository Health Check intelligence, from the same Ext.Direct source the Classic component
  // details panel uses. See useComponentIntelligence for the transport and entitlement notes.
  const {
    data: intelligence,
    status: intelligenceStatus,
    alertsStatus,
  } = useComponentIntelligence({ gaId, format, version: ver });

  const detailRows: IntelligenceDetailRow[] = [
    { label: 'Last Updated', value: formatDateTime(lastUpdated) },
    { label: 'Format', value: formatLabel },
    ...(group ? [{ label: 'Group', value: group }] : []),
    ...(name ? [{ label: 'Name', value: name }] : []),
    ...(ver ? [{ label: 'Version', value: ver }] : []),
    { label: 'Repository', value: repositoryValue, mono: true },
    ...buildIntelligenceRows(intelligenceStatus, intelligence, alertsStatus),
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
                      {row.label === 'Repository' && memberRepoRows.length > 0 ? (
                        <Flex asChild gap="2" wrap="wrap" align="center" role="list" aria-label="Repositories serving this version">
                          <ul style={{ margin: 0, padding: 0, listStyle: 'none' }}>
                            {memberRepoRows.map((repo) => {
                              const isCurrent = repo.repositoryName === selectedRepository;
                              return (
                                // Selected repo: subdued gray (soft), not solid accent blue — the
                                // blue-on-black label was hard to read, and gray reads clearly and
                                // stays visually distinct from the colored type badges. The Check
                                // icon carries the "current" cue; unselected repos sit back as
                                // outline so the selected one still stands out.
                                <Badge
                                  key={repo.repositoryName}
                                  asChild
                                  variant={isCurrent ? 'soft' : 'outline'}
                                  color="gray"
                                  highContrast={isCurrent}
                                >
                                  <li aria-current={isCurrent ? 'true' : undefined}>
                                    {isCurrent && <Check size={12} aria-hidden="true" />}
                                    <Text as="span" size="1">
                                      {repo.repositoryName}
                                    </Text>
                                    <Text as="span" size="1" style={{ opacity: 0.75 }}>
                                      · {repo.type}
                                    </Text>
                                  </li>
                                </Badge>
                              );
                            })}
                          </ul>
                        </Flex>
                      ) : (
                        renderIntelligenceValue(row)
                      )}
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
