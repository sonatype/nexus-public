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

import React, { useCallback, useState } from 'react';
import {
  Box,
  Button,
  Callout,
  Card,
  Checkbox,
  Flex,
  Select,
  Text,
} from '@radix-ui/themes';
import { Info } from 'lucide-react';

import { useToast, RequiredMark } from '../../../../shared';
import {
  GlobalEvaluationSettings,
  useHostedRepoEvaluation,
} from './useHostedRepoEvaluation';
import {
  ACTIVITY_TIME_FRAME_OPTIONS,
  ARTIFACT_LATEST_VERSIONS_OPTIONS,
  POLICY_EVALUATION_STAGES,
  type ActivityTimeFrame,
  type ArtifactLatestVersions,
  type PolicyEvaluationStage,
} from './types';
import { humanizeStage } from './iqServerUtils';

import './HostedRepoEvaluationSetupPage.scss';

export interface SettingsTabProps {
  settingsForm: GlobalEvaluationSettings;
  setSettingsForm: React.Dispatch<React.SetStateAction<GlobalEvaluationSettings>>;
  pristineSettings: GlobalEvaluationSettings;
  setPristineSettings: React.Dispatch<React.SetStateAction<GlobalEvaluationSettings>>;
  hasExistingConfig: boolean;
  settingsDirty: boolean;
  setSettingsStaged: (v: boolean) => void;
  setTab: (t: 'repositories' | 'settings') => void;
}

export function SettingsTab({
  settingsForm,
  setSettingsForm,
  pristineSettings,
  setPristineSettings,
  hasExistingConfig,
  settingsDirty,
  setSettingsStaged,
  setTab,
}: SettingsTabProps) {
  const { saveSettings } = useHostedRepoEvaluation();
  const [savingSettings, setSavingSettings] = useState(false);
  const toast = useToast();

  const updateSettingsField = useCallback(<K extends keyof GlobalEvaluationSettings>(key: K, value: GlobalEvaluationSettings[K]) => {
    setSettingsForm(prev => ({ ...prev, [key]: value }));
  }, [setSettingsForm]);

  const handleSettingsCancel = useCallback(() => {
    setSettingsForm(pristineSettings);
  }, [pristineSettings, setSettingsForm]);

  const handleSettingsSave = useCallback(async () => {
    if (!hasExistingConfig) {
      // First-time onboarding: stage settings locally and defer persistence
      // until the first repo is enabled, which fires an atomic PUT to
      // /evaluation/settings-with-repos. Mirrors Classic UI's wizard flow.
      setPristineSettings(settingsForm);
      setSettingsStaged(true);
      setTab('repositories');
      return;
    }
    setSavingSettings(true);
    const result = await saveSettings(settingsForm);
    if (result.ok) {
      setPristineSettings(settingsForm);
      toast.success('Settings saved successfully');
    } else {
      toast.error(result.message || 'Failed to save settings');
    }
    setSavingSettings(false);
  }, [saveSettings, hasExistingConfig, settingsForm, toast, setPristineSettings, setSettingsStaged, setTab]);

  return (
    <Flex gap="4" mt="4" align="stretch" wrap="wrap">
      <Card style={{ flex: '1 1 480px', maxWidth: 640 }}>
        <Box p="4">
          {!hasExistingConfig && (
            <Box mb="3">
              <Callout.Root color="blue" variant="soft" size="1">
                <Callout.Icon>
                  <Info size={16} aria-hidden="true" />
                </Callout.Icon>
                <Callout.Text>
                  Components are evaluated when they match either setting: recent
                  activity within the selected time frame, or the latest deployed
                  versions.
                </Callout.Text>
              </Callout.Root>
            </Box>
          )}

          <Box mb="3">
            <Text as="label" size="2" weight="medium" id="hre-time-frame-label">
              Activity Time Frame<RequiredMark />
            </Text>
            <Text size="1" color="gray" as="div" mb="1">
              Set the time frame for evaluating components based on recent
              repository activity.
            </Text>
            <Select.Root
              value={String(settingsForm.activityTimeFrame)}
              onValueChange={v => updateSettingsField('activityTimeFrame', Number(v) as ActivityTimeFrame)}
            >
              <Select.Trigger className="hosted-repo-eval-setup-page__settings-select" aria-labelledby="hre-time-frame-label" />
              <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
                {ACTIVITY_TIME_FRAME_OPTIONS.map(o => (
                  <Select.Item key={o.value} value={String(o.value)}>{o.label}</Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Box>

          <Box mb="3">
            <Text as="label" size="2" weight="medium" id="hre-latest-versions-label">
              Latest Deployed Versions<RequiredMark />
            </Text>
            <Text size="1" color="gray" as="div" mb="1">
              Set the number of most recently deployed component versions to
              evaluate.
            </Text>
            <Select.Root
              value={String(settingsForm.artifactLatestVersions)}
              onValueChange={v => updateSettingsField('artifactLatestVersions', Number(v) as ArtifactLatestVersions)}
            >
              <Select.Trigger className="hosted-repo-eval-setup-page__settings-select" aria-labelledby="hre-latest-versions-label" />
              <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
                {ARTIFACT_LATEST_VERSIONS_OPTIONS.map(n => (
                  <Select.Item key={n} value={String(n)}>{n}</Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Box>

          <Box mb="3">
            <Text as="label" size="2" weight="medium" id="hre-policy-stage-label">
              Policy Evaluation Stage<RequiredMark />
            </Text>
            <Select.Root
              value={settingsForm.policyEvaluationStage}
              onValueChange={v => updateSettingsField('policyEvaluationStage', v as PolicyEvaluationStage)}
            >
              <Select.Trigger className="hosted-repo-eval-setup-page__settings-select" aria-labelledby="hre-policy-stage-label" />
              <Select.Content position="popper" side="bottom" align="start" sideOffset={4}>
                {POLICY_EVALUATION_STAGES.map(s => (
                  <Select.Item key={s} value={s}>{humanizeStage(s)}</Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Box>

          <Box mb="4">
            <Text size="2" weight="medium" as="div" mb="1">Apply To New Repositories</Text>
            <Flex as="label" align="center" gap="2">
              <Checkbox
                checked={settingsForm.autoEnrollNewRepos}
                onCheckedChange={v => updateSettingsField('autoEnrollNewRepos', v === true)}
              />
              <Text size="2">Evaluate new hosted repositories as they are created</Text>
            </Flex>
          </Box>

          <Flex justify="end" gap="2">
            <Button variant="soft" color="gray" onClick={handleSettingsCancel} disabled={!settingsDirty || savingSettings}>
              Cancel
            </Button>
            <Button
              onClick={handleSettingsSave}
              disabled={hasExistingConfig && (!settingsDirty || savingSettings)}
            >
              {!hasExistingConfig ? 'Next' : (savingSettings ? 'Saving…' : 'Save')}
            </Button>
          </Flex>
        </Box>
      </Card>

      {/* Package file Patterns — per-format file types IQ Server evaluates. */}
      <Box style={{ flex: '1 1 320px', display: 'flex' }}>
        <Callout.Root color="blue" variant="surface" style={{ width: '100%' }}>
          <Callout.Icon>
            <Info size={16} />
          </Callout.Icon>
          <Callout.Text>
            <Text weight="bold" size="2" as="div" mb="1">
              Package file Patterns
            </Text>
            <Text size="2" as="div" mb="2">
              We&apos;ll focus on the files that matter most, based on known
              file patterns for each package format. This helps reduce noise
              by avoiding unnecessary scans of metadata or source files.
            </Text>
            <Box asChild>
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                <li>
                  <Text size="2">
                    <Text weight="bold">Maven:</Text> Evaluations target
                    .jar, .war, and .ear files — the main deployable
                    outputs of Java builds.
                  </Text>
                </li>
                <li>
                  <Text size="2">
                    <Text weight="bold">npm:</Text> Evaluate .tgz
                    package files that follow versioned naming
                    conventions.
                  </Text>
                </li>
                <li>
                  <Text size="2">
                    <Text weight="bold">PyPI (Python):</Text> We match
                    .whl and .tar.gz files, which are the primary
                    distribution formats for Python packages.
                  </Text>
                </li>
              </ul>
            </Box>
          </Callout.Text>
        </Callout.Root>
      </Box>

    </Flex>
  );
}
