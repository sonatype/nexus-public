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

import React, { useMemo } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Trash2, Info, ExternalLink, Loader2 } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsTextArea,
  SettingsSelect,
  SettingsButton,
  SettingsAlert,
} from '../../../shared/form';
import { useRoutingRulesForm } from './useRoutingRulesForm';
import { RoutingRuleMatcher } from './RoutingRuleMatcher';
import {
  RoutingRuleFormProps,
  RoutingRuleFormData,
  RoutingMode,
  ROUTING_MODE_LABELS,
  ROUTING_MODE_HELP,
  hasInvalidMatchers,
} from './types';

import './RoutingRuleForm.scss';

/**
 * RoutingRuleForm - Create/Edit form for routing rules
 * Now uses XState for state management via useRoutingRulesForm hook
 */
export function RoutingRuleForm({
  rule,
  isCreate,
  onSave,
  onCancel,
  onDelete,
  loading = false,
  error,
}: RoutingRuleFormProps) {
  const [testPath, setTestPath] = React.useState('');

  // Use XState form hook
  const {
    form,
    routingRule,
    canDelete,
    matchers,
    handleMatchersChange,
    handleModeChange,
  } = useRoutingRulesForm({
    ruleName: isCreate ? undefined : rule?.name,
    rule: rule || undefined,
    onSave,
    onCancel,
  });

  const currentRule = rule || routingRule;
  const formData = form.data as RoutingRuleFormData;

  // Mode options for select
  const modeOptions = [
    { value: 'BLOCK', label: ROUTING_MODE_LABELS.BLOCK },
    { value: 'ALLOW', label: ROUTING_MODE_LABELS.ALLOW },
  ];

  // Check if save should be disabled due to invalid matchers
  const hasBlockingErrors = hasInvalidMatchers(matchers || []);

  // Show loading state
  if (form.isLoading) {
    return (
      <Flex align="center" justify="center" className="routing-rule-form__loading">
        <Loader2 size={24} className="routing-rule-form__spinner" />
        <Text size="2">Loading form...</Text>
      </Flex>
    );
  }

  return (
    <Box className="routing-rule-form">
      <SettingsForm
        onSubmit={() => form.send('SUBMIT')}
        onCancel={onCancel}
        loading={form.isSaving || loading}
        pristine={form.isPristine}
        error={error || form.saveError || undefined}
        submitLabel={isCreate ? 'Create' : 'Save'}
        cancelLabel="Cancel"
        submitDisabled={hasBlockingErrors}
        confirmDiscard
        testId="routing-rule-form"
        footerExtra={
          !isCreate && onDelete ? (
            <SettingsButton
              testId="form-delete"
              variant="danger"
              onClick={onDelete}
              disabled={form.isSaving || loading || !canDelete}
              icon={Trash2}
              title={
                !canDelete
                  ? `Cannot delete: Assigned to ${currentRule?.assignedRepositoryCount ?? 0} repositories`
                  : 'Delete routing rule'
              }
            >
              Delete
            </SettingsButton>
          ) : undefined
        }
      >
        <SettingsFormSection title="Basic Information">
          <SettingsTextInput
            {...form.field('name')}
            label="Name"
            placeholder="e.g., block-sources"
            helpText="A unique name for this routing rule"
            required
            disabled={!isCreate || form.isSaving || loading}
            readOnly={!isCreate}
          />

          <SettingsTextArea
            {...form.field('description')}
            label="Description"
            placeholder="Describe what this routing rule does"
            helpText="Optional description for this routing rule"
            disabled={form.isSaving || loading}
            rows={3}
          />

          <SettingsSelect
            name="mode"
            label="Mode"
            value={formData.mode || 'BLOCK'}
            onChange={(value) => handleModeChange(value as RoutingMode)}
            options={modeOptions}
            helpText={ROUTING_MODE_HELP[formData.mode || 'BLOCK']}
            required
            disabled={form.isSaving || loading}
          />
        </SettingsFormSection>

        <SettingsFormSection title="Matchers">
          <RoutingRuleMatcher
            matchers={matchers || ['']}
            onChange={handleMatchersChange}
            error={form.touched?.matchers ? form.validationErrors?.matchers : undefined}
            disabled={form.isSaving || loading}
            testPath={testPath}
            testMode={formData.mode || 'BLOCK'}
            onTest={setTestPath}
          />
        </SettingsFormSection>

        {/* Help Section */}
        <Box className="routing-rule-form__help">
          <Flex align="center" gap="2" className="routing-rule-form__help-header">
            <Info size={16} />
            <Text size="2" weight="medium">About Routing Rules</Text>
          </Flex>
          <Text size="2" className="routing-rule-form__help-text">
            Routing rules allow you to control which requests are allowed or blocked based on
            request path patterns. Rules can be assigned to proxy repositories to restrict which
            components can be retrieved from upstream.
            {' '}
            <a
              href="http://links.sonatype.com/products/nxrm3/docs/routing-rule"
              target="_blank"
              rel="noopener noreferrer"
              className="routing-rule-form__help-link"
            >
              Learn more
              <ExternalLink size={12} />
            </a>
          </Text>
        </Box>

        {/* Warning for assigned repositories */}
        {!isCreate && (currentRule?.assignedRepositoryCount ?? 0) > 0 && (
          <SettingsAlert type="warning" className="routing-rule-form__warning">
            This routing rule is assigned to {currentRule?.assignedRepositoryCount} repositories: {' '}
            {currentRule?.assignedRepositoryNames?.join(', ')}.
            You must unassign it from all repositories before deleting.
          </SettingsAlert>
        )}
      </SettingsForm>
    </Box>
  );
}

export default RoutingRuleForm;
