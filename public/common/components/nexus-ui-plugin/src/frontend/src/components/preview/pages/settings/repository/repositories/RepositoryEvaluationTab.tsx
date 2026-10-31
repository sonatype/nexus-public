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

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Box,
  Button,
  Card,
  Flex,
  Heading,
  RadioGroup,
  Select,
  Text,
  Theme,
} from '@radix-ui/themes';
import { ExternalLink } from 'lucide-react';
import { restClient } from '../../../../../../interface/api';
import { useUnsavedChangesWarning } from '../../../../shared/hooks/useUnsavedChangesWarning';
import { useToast } from '../../../../shared';

import {
  EvaluationMode,
  PolicyEvaluationStage,
  RepoEvaluationOverride,
  useRepoEvaluationOverride,
} from './useRepoEvaluationOverride';
import { EVALUATION_SETTINGS_API } from '../../system/iq-server/useHostedRepoEvaluation';

import './RepositoryEvaluationTab.scss';

const STAGES: { value: PolicyEvaluationStage; label: string }[] = [
  { value: 'BUILD', label: 'Build' },
  { value: 'STAGE_RELEASE', label: 'Stage Release' },
  { value: 'RELEASE', label: 'Release' },
  { value: 'OPERATE', label: 'Operate' },
];

const ACTIVITY_TIME_FRAMES = [30, 60, 90];
// Match Classic UI (HostedRepositoriesEvaluationStrings.artifactLatestVersionsOptions): 1..5.
const ARTIFACT_LATEST_VERSIONS = [1, 2, 3, 4, 5];

interface RepositoryEvaluationTabProps {
  repositoryName: string;
  /**
   * When rendered inside the Repository edit form, the parent form's top
   * "Save Changes" / "Cancel" buttons drive this tab. We hide the in-card
   * Cancel/Save and expose handlers + dirty state to the parent.
   */
  hideActions?: boolean;
  onDirtyChange?: (dirty: boolean) => void;
  onSavingChange?: (saving: boolean) => void;
  onSaveRef?: React.MutableRefObject<(() => Promise<void>) | null>;
  onCancelRef?: React.MutableRefObject<(() => void) | null>;
}

interface GlobalSettings {
  activityTimeFrame: number;
  artifactLatestVersions: number;
  policyEvaluationStage: PolicyEvaluationStage;
}

const DEFAULT_GLOBALS: GlobalSettings = {
  activityTimeFrame: 30,
  artifactLatestVersions: 5,
  policyEvaluationStage: 'RELEASE',
};

/**
 * Per-repo Evaluation override tab.
 * Three modes: INHERIT (global settings), OVERRIDE (per-repo), DISABLE (skip eval).
 * Preserves OVERRIDE values when toggling away and back (Classic UI pattern).
 */
export function RepositoryEvaluationTab({
  repositoryName,
  hideActions = false,
  onDirtyChange,
  onSavingChange,
  onSaveRef,
  onCancelRef,
}: RepositoryEvaluationTabProps): JSX.Element {
  const { loading: apiLoading, error: apiError, fetchOverride, saveOverride } = useRepoEvaluationOverride();
  // Surface API errors as toasts too (previously an inline banner block).

  // Initial-load state
  const [loadingInitial, setLoadingInitial] = useState(true);
  const [globals, setGlobals] = useState<GlobalSettings>(DEFAULT_GLOBALS);

  // Working form state
  const [mode, setMode] = useState<EvaluationMode>('INHERIT');
  const [activityTimeFrame, setActivityTimeFrame] = useState<number>(DEFAULT_GLOBALS.activityTimeFrame);
  const [artifactLatestVersions, setArtifactLatestVersions] = useState<number>(DEFAULT_GLOBALS.artifactLatestVersions);
  const [policyEvaluationStage, setPolicyEvaluationStage] = useState<PolicyEvaluationStage>(DEFAULT_GLOBALS.policyEvaluationStage);

  // Pristine values for dirty-check + Cancel
  const [pristine, setPristine] = useState<RepoEvaluationOverride>({ mode: 'INHERIT' });

  // Saved OVERRIDE values so user can flip away and back without losing values.
  const [savedOverride, setSavedOverride] = useState<{
    activityTimeFrame: number;
    artifactLatestVersions: number;
    policyEvaluationStage: PolicyEvaluationStage;
  } | null>(null);

  const [saving, setSaving] = useState(false);
  const toast = useToast();

  useEffect(() => {
    if (apiError) toast.error(apiError);
  }, [apiError, toast]);

  // Load globals + existing override on mount.
  useEffect(() => {
    let cancelled = false;
    const run = async () => {
      setLoadingInitial(true);
      try {
        const [globalData, override] = await Promise.all([
          restClient.get<{
            activityTimeFrame?: number;
            artifactLatestVersions?: number;
            policyEvaluationStage?: string;
          } | null>(EVALUATION_SETTINGS_API).catch((err: any) => {
            const msg = err?.response?.data?.message || err?.message || 'Failed to load global evaluation settings';
            toast.error(msg);
            return null;
          }),
          fetchOverride(repositoryName).catch(() => null),
        ]);
        if (cancelled) return;
        const g: GlobalSettings = {
          activityTimeFrame: globalData?.activityTimeFrame ?? DEFAULT_GLOBALS.activityTimeFrame,
          artifactLatestVersions: globalData?.artifactLatestVersions ?? DEFAULT_GLOBALS.artifactLatestVersions,
          policyEvaluationStage: (STAGES.find(s => s.value === globalData?.policyEvaluationStage)?.value) ?? DEFAULT_GLOBALS.policyEvaluationStage,
        };
        setGlobals(g);
        const initialMode: EvaluationMode = override?.mode ?? 'INHERIT';
        const initial: RepoEvaluationOverride = {
          mode: initialMode,
          activityTimeFrame: override?.activityTimeFrame ?? g.activityTimeFrame,
          artifactLatestVersions: override?.artifactLatestVersions ?? g.artifactLatestVersions,
          policyEvaluationStage: override?.policyEvaluationStage ?? g.policyEvaluationStage,
        };
        setMode(initial.mode);
        setActivityTimeFrame(initial.activityTimeFrame!);
        setArtifactLatestVersions(initial.artifactLatestVersions!);
        setPolicyEvaluationStage(initial.policyEvaluationStage!);
        setPristine(initial);
        if (initialMode === 'OVERRIDE') {
          setSavedOverride({
            activityTimeFrame: initial.activityTimeFrame!,
            artifactLatestVersions: initial.artifactLatestVersions!,
            policyEvaluationStage: initial.policyEvaluationStage!,
          });
        }
      } finally {
        if (!cancelled) setLoadingInitial(false);
      }
    };
    run();
    return () => {
      cancelled = true;
    };
  }, [repositoryName, fetchOverride]);

  // Dirty-check for Save / Cancel button enablement.
  const isDirty = useMemo(() => {
    if (mode !== pristine.mode) return true;
    if (mode === 'OVERRIDE') {
      return (
        activityTimeFrame !== pristine.activityTimeFrame ||
        artifactLatestVersions !== pristine.artifactLatestVersions ||
        policyEvaluationStage !== pristine.policyEvaluationStage
      );
    }
    return false;
  }, [mode, pristine, activityTimeFrame, artifactLatestVersions, policyEvaluationStage]);

  useUnsavedChangesWarning(isDirty, `repo-evaluation-${repositoryName}`);

  // When the parent edit form hosts this tab, surface dirty/saving state and
  // expose Save/Cancel handlers so the page-level toolbar drives this form
  // instead of duplicating Cancel/Save buttons inside the card.
  useEffect(() => {
    onDirtyChange?.(isDirty);
  }, [isDirty, onDirtyChange]);
  useEffect(() => {
    onSavingChange?.(saving);
  }, [saving, onSavingChange]);

  const handleModeChange = useCallback(
    (nextMode: EvaluationMode) => {
      // Leaving OVERRIDE -> remember the values so they're restored later.
      if (mode === 'OVERRIDE' && nextMode !== 'OVERRIDE') {
        setSavedOverride({
          activityTimeFrame,
          artifactLatestVersions,
          policyEvaluationStage,
        });
      }
      // Entering OVERRIDE -> seed from previously saved OVERRIDE values if any,
      // else from globals.
      if (nextMode === 'OVERRIDE') {
        const src = savedOverride ?? globals;
        setActivityTimeFrame(src.activityTimeFrame);
        setArtifactLatestVersions(src.artifactLatestVersions);
        setPolicyEvaluationStage(src.policyEvaluationStage);
      }
      setMode(nextMode);
    },
    [mode, savedOverride, globals, activityTimeFrame, artifactLatestVersions, policyEvaluationStage],
  );

  const handleCancel = useCallback(() => {
    setMode(pristine.mode);
    setActivityTimeFrame(pristine.activityTimeFrame ?? globals.activityTimeFrame);
    setArtifactLatestVersions(pristine.artifactLatestVersions ?? globals.artifactLatestVersions);
    setPolicyEvaluationStage(pristine.policyEvaluationStage ?? globals.policyEvaluationStage);
  }, [pristine, globals]);

  const handleSave = useCallback(async () => {
    setSaving(true);
    const payload: RepoEvaluationOverride = { mode };
    if (mode === 'OVERRIDE') {
      payload.activityTimeFrame = activityTimeFrame;
      payload.artifactLatestVersions = artifactLatestVersions;
      payload.policyEvaluationStage = policyEvaluationStage;
    }
    const result = await saveOverride(repositoryName, payload);
    if (result.ok) {
      setPristine(payload);
      if (mode === 'OVERRIDE') {
        setSavedOverride({ activityTimeFrame, artifactLatestVersions, policyEvaluationStage });
      }
      toast.success(result.message || 'Override saved');
    } else {
      toast.error(result.message || 'Failed to save override');
    }
    setSaving(false);
  }, [mode, activityTimeFrame, artifactLatestVersions, policyEvaluationStage, saveOverride, repositoryName, toast]);

  // Keep refs in sync so the parent toolbar's Save Changes / Cancel can invoke
  // the latest closures (which capture the current form values).
  useEffect(() => {
    if (onSaveRef) onSaveRef.current = handleSave;
    return () => {
      if (onSaveRef && onSaveRef.current === handleSave) onSaveRef.current = null;
    };
  }, [handleSave, onSaveRef]);
  useEffect(() => {
    if (onCancelRef) onCancelRef.current = handleCancel;
    return () => {
      if (onCancelRef && onCancelRef.current === handleCancel) onCancelRef.current = null;
    };
  }, [handleCancel, onCancelRef]);

  if (loadingInitial) {
    return (
      <Theme accentColor="blue" hasBackground={false}>
        <Box p="4" className="repository-evaluation-tab">
          <Text size="2" color="gray">Loading evaluation configuration…</Text>
        </Box>
      </Theme>
    );
  }

  return (
    <Theme accentColor="blue" hasBackground={false}>
      <Box p="4" className="repository-evaluation-tab">
        <Heading size="4" as="h2" mb="2">Evaluation</Heading>
        <Text size="2" color="gray" as="div" mb="4">
          Configure how evaluation is applied to this hosted repository.
        </Text>

        <Card size="2">
          <Box p="4">
            <Heading size="3" as="h3" mb="2">Hosted Repository Evaluation</Heading>
            <Text size="2" color="gray" as="div" mb="4">
              Choose how evaluation settings should be applied to this repository.
            </Text>

            <RadioGroup.Root value={mode} onValueChange={(v) => handleModeChange(v as EvaluationMode)}>
              <Flex direction="column" gap="4">
                {/* INHERIT — links to global settings page */}
                <Box>
                  <Flex gap="2" align="start" className="repository-evaluation-tab__radio-row">
                    <RadioGroup.Item value="INHERIT" />
                    <Box>
                      <Text as="label" size="2" weight="medium">Use global settings</Text>
                      <Text size="2" color="gray" as="div">
                        Inherit the settings configured in{' '}
                        <Button asChild variant="ghost" size="1" color="blue" className="repository-evaluation-tab__inline-link">
                          <a href="#preview/admin/iq/hosted-repos-eval">
                            Hosted Repository Evaluation
                            <ExternalLink size={12} aria-hidden="true" />
                          </a>
                        </Button>
                        .
                      </Text>
                    </Box>
                  </Flex>
                </Box>

                {/* OVERRIDE */}
                <Box>
                  <Flex gap="2" align="start" className="repository-evaluation-tab__radio-row">
                    <RadioGroup.Item value="OVERRIDE" />
                    <Box>
                      <Text as="label" size="2" weight="medium">Customize for this repository</Text>
                      <Text size="2" color="gray" as="div">Override the global configuration with repository-specific settings.</Text>
                    </Box>
                  </Flex>
                  {mode === 'OVERRIDE' && (
                    <Box ml="6" mt="2">
                      <Card size="1">
                        <Box p="3">
                          <FieldRow
                            label="Activity Time Frame"
                            description="Evaluate components based on recent repository activity."
                            control={
                              <Select.Root
                                value={String(activityTimeFrame)}
                                onValueChange={v => setActivityTimeFrame(parseInt(v, 10))}
                              >
                                <Select.Trigger style={{ minWidth: 160 }} />
                                <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
                                  {ACTIVITY_TIME_FRAMES.map(n => (
                                    <Select.Item key={n} value={String(n)}>{`Last ${n} Days`}</Select.Item>
                                  ))}
                                </Select.Content>
                              </Select.Root>
                            }
                          />
                          <FieldRow
                            label="Artifact Latest Versions"
                            description="Set the number of versions of the artifact for evaluation."
                            control={
                              <Select.Root
                                value={String(artifactLatestVersions)}
                                onValueChange={v => setArtifactLatestVersions(parseInt(v, 10))}
                              >
                                <Select.Trigger style={{ minWidth: 100 }} />
                                <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
                                  {ARTIFACT_LATEST_VERSIONS.map(n => (
                                    <Select.Item key={n} value={String(n)}>{n}</Select.Item>
                                  ))}
                                </Select.Content>
                              </Select.Root>
                            }
                          />
                          <FieldRow
                            label="Policy Evaluation Stage"
                            description="Select the lifecycle stage for continuous policy evaluation."
                            control={
                              <Select.Root
                                value={policyEvaluationStage}
                                onValueChange={v => setPolicyEvaluationStage(v as PolicyEvaluationStage)}
                              >
                                <Select.Trigger style={{ minWidth: 160 }} />
                                <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
                                  {STAGES.map(s => (
                                    <Select.Item key={s.value} value={s.value}>{s.label}</Select.Item>
                                  ))}
                                </Select.Content>
                              </Select.Root>
                            }
                            isLast
                          />
                        </Box>
                      </Card>
                    </Box>
                  )}
                </Box>

                {/* DISABLE */}
                <Box>
                  <Flex gap="2" align="start" className="repository-evaluation-tab__radio-row">
                    <RadioGroup.Item value="DISABLE" />
                    <Box>
                      <Text as="label" size="2" weight="medium">Disable evaluation</Text>
                      <Text size="2" color="gray" as="div">Don't evaluate components in this repository.</Text>
                    </Box>
                  </Flex>
                </Box>
              </Flex>
            </RadioGroup.Root>

            {!hideActions && (
              <Flex justify="end" gap="2" mt="4">
                <Button
                  variant="soft"
                  color="gray"
                  onClick={handleCancel}
                  disabled={!isDirty || saving || apiLoading}
                >
                  Cancel
                </Button>
                <Button onClick={handleSave} disabled={!isDirty || saving || apiLoading}>
                  {saving ? 'Saving…' : 'Save'}
                </Button>
              </Flex>
            )}
          </Box>
        </Card>
      </Box>
    </Theme>
  );
}

// =============================================================================
// Local helpers
// =============================================================================

interface FieldRowProps {
  label: string;
  description: string;
  control: React.ReactNode;
  isLast?: boolean;
}

function FieldRow({ label, description, control, isLast }: FieldRowProps): JSX.Element {
  return (
    <Flex
      align="center"
      justify="between"
      gap="4"
      py="3"
      className={`repository-evaluation-tab__field-row${isLast ? ' repository-evaluation-tab__field-row--last' : ''}`}
    >
      <Box>
        <Text size="2" weight="medium" as="div">{label}</Text>
        <Text size="2" color="gray" as="div">{description}</Text>
      </Box>
      <Box>{control}</Box>
    </Flex>
  );
}

export default RepositoryEvaluationTab;
