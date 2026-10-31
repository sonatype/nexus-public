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

/**
 * GADetailHeader — GA detail summary header rendered as a card: a component icon, the name over
 * its group coordinate, and a small labeled grid of Group / Repository selectors (repo types come
 * from useGARepositoriesForVersion, shown as a badge next to the repository) plus the current
 * version shown as read-only text — version switching lives in the Versions tab. The Download
 * control sits on the right. The Group row is hidden when no group-type repository serves the
 * selected version; a facet with a single option is shown but disabled, with a tooltip explaining
 * why.
 */

import React, { useMemo } from 'react';
import { Badge, Box, Button, Card, Flex, Grid, Heading, Select, Text, Tooltip } from '@radix-ui/themes';
import { FolderTree, Package } from 'lucide-react';

import { GADownloadMenu } from './GADownloadMenu';
import type { RepoRow } from './gaRepositoriesMachine';
import { DeepResearchLink } from '../../../shared/DeepResearchLink';
import { parseGaId } from './gaIdUtils';
import { guidePackageNameFromGaId } from '../../../../../utils/guideIntegration';

interface GADetailHeaderProps {
  gaId: string;
  displayName: string;
  coordinate: string;
  selectedVersion: string | null;
  repoRows: readonly RepoRow[];
  selectedRepository: string | null;
  onRepositoryChange: (repository: string) => void;
  selectedGroup: string | null;
  onGroupChange: (group: string) => void;
  /**
   * Deep-link this component/version into the new Preview/Nexus One Browse tree (NEXUS-54430).
   * Replaces the former copy-path button. When omitted (no repository resolved yet, or none serves
   * the version) the action is hidden — the caller decides availability via {@link buildBrowseTarget}.
   */
  onViewInBrowse?: () => void;
}

const TYPE_COLOR: Record<RepoRow['type'], 'iris' | 'grass' | 'amber'> = {
  group: 'iris',
  hosted: 'grass',
  proxy: 'amber',
};

/**
 * A single soft-accent dropdown. The visible field label lives in the header grid; the control
 * still carries an `aria-label` so it is reachable by name. When only one option serves the
 * version the control is shown (so the user sees the current value) but disabled, with a tooltip
 * spelling out why so the disabled state does not read as broken.
 */
function RepoDropdown({
  label,
  rows,
  value,
  onChange,
  disabled = false,
  disabledReason,
}: {
  label: string;
  rows: readonly RepoRow[];
  value: string | null;
  onChange: (v: string) => void;
  disabled?: boolean;
  disabledReason?: string;
}) {
  const trigger = <Select.Trigger aria-label={label} variant="soft" />;
  return (
    <Select.Root value={value ?? undefined} onValueChange={onChange} size="2" disabled={disabled}>
      {disabled && disabledReason ? (
        // A disabled trigger emits no pointer events, so the tooltip has nothing to hover; the
        // inline-flex span is a live element that receives the hover in the trigger's place.
        <Tooltip content={disabledReason}>
          <span style={{ display: 'inline-flex' }}>{trigger}</span>
        </Tooltip>
      ) : (
        trigger
      )}
      <Select.Content>
        {rows.map((r) => (
          <Select.Item key={r.repositoryName} value={r.repositoryName}>
            {r.repositoryName}
          </Select.Item>
        ))}
      </Select.Content>
    </Select.Root>
  );
}

export function GADetailHeader({
  gaId,
  displayName,
  coordinate,
  selectedVersion,
  repoRows,
  selectedRepository,
  onRepositoryChange,
  selectedGroup,
  onGroupChange,
  onViewInBrowse,
}: GADetailHeaderProps) {
  const groupRows = useMemo(() => repoRows.filter((r) => r.type === 'group'), [repoRows]);
  const memberRows = useMemo(() => repoRows.filter((r) => r.type !== 'group'), [repoRows]);

  const selectedType = repoRows.find((r) => r.repositoryName === selectedRepository)?.type;

  // Guide deep-research link derived from the GA id: ecosystem is the format segment and the
  // package path follows the same npm-scoped / maven group:name rules the search card uses.
  // parseGaId always returns an object; a single-segment id yields format '' (its "unusable"
  // case), and DeepResearchLink self-guards on that and on unsupported ecosystems, so only the
  // version needs checking here.
  const { format: guideEcosystem } = parseGaId(gaId);
  const guidePackageName = guidePackageNameFromGaId(gaId);

  return (
    <Card size="3">
      <Flex justify="between" align="stretch" gap="6" wrap="wrap">
        {/* Left: icon + name/coordinate + labeled selectors */}
        <Flex gap="4" align="start" minWidth="0" style={{ flex: '1 1 auto' }}>
          <Box
            aria-hidden
            style={{
              width: 64,
              height: 64,
              flexShrink: 0,
              borderRadius: 'var(--radius-4)',
              background: 'var(--accent-a3)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Package size={30} color="var(--accent-9)" />
          </Box>

          <Flex direction="column" gap="1" minWidth="0">
            <Heading size="6">{displayName}</Heading>
            {coordinate && (
              <Text size="2" color="gray">
                {coordinate}
              </Text>
            )}

            {/* Labeled facet grid: field name in the first column, control in the second. */}
            <Grid columns="max-content max-content" gapX="4" gapY="2" align="center" mt="2">
              {groupRows.length > 0 && (
                <>
                  <Text size="2" color="gray">
                    Group
                  </Text>
                  <Flex align="center" gap="2">
                    <RepoDropdown
                      label="Group"
                      rows={groupRows}
                      value={selectedGroup}
                      onChange={onGroupChange}
                      disabled={groupRows.length === 1}
                      disabledReason="Only one group repository serves this component."
                    />
                  </Flex>
                </>
              )}

              <Text size="2" color="gray">
                Repository
              </Text>
              <Flex align="center" gap="2">
                <RepoDropdown
                  label="Repository"
                  rows={memberRows}
                  value={selectedRepository}
                  onChange={onRepositoryChange}
                  disabled={memberRows.length <= 1}
                  disabledReason="Only one repository serves this component."
                />
                {selectedType && selectedType !== 'group' && (
                  <Badge color={TYPE_COLOR[selectedType]} variant="soft" size="1">
                    {selectedType}
                  </Badge>
                )}
              </Flex>

              <Text size="2" color="gray">
                Version
              </Text>
              <Text size="2" weight="medium">
                {selectedVersion ?? '—'}
              </Text>
            </Grid>
          </Flex>
        </Flex>

        {/* Right: component actions, kept top-aligned. No divider — the card edge and the gap
            already separate them. */}
        <Flex align="start" gap="2">
          {onViewInBrowse && (
            <Button
              variant="soft"
              size="2"
              color="blue"
              aria-label={`View ${displayName} in Browse`}
              onClick={onViewInBrowse}
            >
              <FolderTree size={14} />
              View in Browse
            </Button>
          )}
          {selectedVersion && guidePackageName && (
            <DeepResearchLink
              ecosystem={guideEcosystem}
              packageName={guidePackageName}
              version={selectedVersion}
              variant="soft"
              size="2"
              referrer="repo-componentdetail"
            />
          )}
          <GADownloadMenu gaId={gaId} version={selectedVersion} repository={selectedRepository} />
        </Flex>
      </Flex>
    </Card>
  );
}

export default GADetailHeader;
