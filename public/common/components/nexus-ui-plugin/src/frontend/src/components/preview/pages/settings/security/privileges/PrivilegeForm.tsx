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

import React, { useMemo, useEffect } from 'react';
import { Box, Flex, Text, Grid } from '@radix-ui/themes';
import { Trash2, Loader2 } from 'lucide-react';
import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsTextArea,
  SettingsSelect,
  SettingsButton,
  SettingsCheckboxGroup,
  SettingsCombobox,
} from '../../../../shared/form';
import { usePrivilegesApi } from './usePrivilegesApi';
import { usePrivilegeForm } from './usePrivilegeForm';
import {
  PrivilegeFormProps,
  PRIVILEGE_TYPES,
  getPrivilegeTypeLabel,
  isReadOnlyPrivilege,
  getActionsForPrivilegeType,
  APPLICATION_DOMAINS,
  ContentSelector,
} from './types';
import { SelectionInsights } from './SelectionInsights';

import './PrivilegeForm.scss';

/**
 * PrivilegeForm - Form for creating and editing privileges
 */
export function PrivilegeForm({
  privilege,
  isCreate,
  typeId,
  onSave,
  onCancel,
  onDelete,
  loading = false,
  error,
  hideActions = false,
  onSubmitRef,
  onValidationChange,
  wizardStep,
}: PrivilegeFormProps & { 
  typeId?: string; 
  hideActions?: boolean; 
  onSubmitRef?: React.MutableRefObject<(() => void) | null>;
  onValidationChange?: (isValid: boolean) => void;
  wizardStep?: number;
}) {
  const { createPrivilege, updatePrivilege } = usePrivilegesApi();

  const { form, privilege: formPrivilege } = usePrivilegeForm({
    privilegeId: isCreate ? undefined : privilege?.id,
    privilege: privilege || undefined,
    typeId,
    onSave,
    onCancel,
    createPrivilege,
    updatePrivilege,
  });

  const isLoading = form.state.matches('loading');
  const isSaving = form.state.matches('saving');
  const context = form.state.context as any;
  const formData = context.data;

  useEffect(() => {
    if (onValidationChange) {
      const isNameValid = !!formData?.name?.trim() && !form.validationErrors?.name;
      if (wizardStep === 1) {
        onValidationChange(isNameValid);
      } else {
        onValidationChange(true);
      }
    }
  }, [formData?.name, form.validationErrors?.name, onValidationChange, wizardStep]);

  if (onSubmitRef) {
    onSubmitRef.current = () => form.send('SUBMIT');
  }

  const currentPrivilege = privilege || formPrivilege;
  const isReadOnly = currentPrivilege ? isReadOnlyPrivilege(currentPrivilege) : false;

  const repositories = context.repositories || [];
  const formats = context.formats || [];
  const contentSelectors: ContentSelector[] = context.contentSelectors || [];
  const scripts = context.scripts || [];

  const selectedRepo = useMemo(() => {
    const repoName = formData?.properties?.repository;
    if (!repoName || repoName === '*') return null;
    return repositories.find((r: any) => r.name === repoName) || null;
  }, [repositories, formData?.properties?.repository]);

  const selectedSelector = useMemo(() => {
    const selectorName = formData?.properties?.contentSelector;
    if (!selectorName) return null;
    return contentSelectors.find((s) => s.name === selectorName) || null;
  }, [contentSelectors, formData?.properties?.contentSelector]);

  const selectedFormat = formData?.properties?.format;

  const currentPrivilegeType = typeId || formData?.type;

  // Repository options for repository-view and repository-admin privileges
  // These types only accept "*" or a specific repository name (not "*-format")
  const repositoryOptions = useMemo(() => {
    const options: Array<{ value: string; label: string }> = [];

    // Show "(All Repositories)" option with value "*"
    // When format is specified, "*" means "all repositories of that format"
    options.push({ value: '*', label: '(All Repositories)' });

    // Add individual repositories (filtered by selected format if applicable)
    const filteredRepos = selectedFormat && selectedFormat !== '*'
      ? repositories.filter((r: any) => r.format === selectedFormat)
      : repositories;
    filteredRepos.forEach((r: any) => {
      options.push({ value: r.name, label: r.name });
    });

    return options;
  }, [repositories, selectedFormat]);

  // Repository options for repository-content-selector privileges
  // This type accepts "*", "*-format", or a specific repository name
  const contentSelectorRepositoryOptions = useMemo(() => {
    const options: Array<{ value: string; label: string }> = [];

    // Only show "* (All Repositories)" if format is "*" or not selected
    if (!selectedFormat || selectedFormat === '*') {
      options.push({ value: '*', label: '(All Repositories)' });
    }

    // Add "All {format} Repositories" options - filtered by selected format
    // Uses "*-{format}" format (e.g., "*-maven2") which matches RepositorySelector.ALL_OF_FORMAT_PREFIX
    const uniqueFormats = Array.from(new Set(repositories.map((r: any) => r.format)));
    uniqueFormats.sort().forEach(format => {
      if (format) {
        // Only show this format option if it matches the selected format or no format is selected
        if (!selectedFormat || selectedFormat === '*' || selectedFormat === format) {
          options.push({
            value: `*-${format}`,
            label: `(All ${format} Repositories)`
          });
        }
      }
    });

    // Add individual repositories (filtered by selected format if applicable)
    const filteredRepos = selectedFormat && selectedFormat !== '*'
      ? repositories.filter((r: any) => r.format === selectedFormat)
      : repositories;
    filteredRepos.forEach((r: any) => {
      options.push({ value: r.name, label: r.name });
    });

    return options;
  }, [repositories, selectedFormat]);

  const formatOptions = useMemo(() => [
    { value: '*', label: '* (All Formats)' },
    ...formats.map((f: string) => ({ value: f, label: f })),
  ], [formats]);

  const contentSelectorOptions = useMemo(() => {
    const filteredSelectors = selectedFormat && selectedFormat !== '*'
      ? contentSelectors.filter((s) => {
          const lowerExpr = s.expression.toLowerCase();
          const regex = /format\s*(?:==|!=|=~|=\^|\^=)\s*["']([^"']+)["']/gi;
          let match;
          let hasRestriction = false;
          let matches = false;
          while ((match = regex.exec(lowerExpr)) !== null) {
            hasRestriction = true;
            if (match[1].toLowerCase() === selectedFormat.toLowerCase() || match[1] === '*') {
              matches = true;
            }
          }
          return !hasRestriction || matches;
        })
      : contentSelectors;
    return filteredSelectors.map((s: ContentSelector) => ({ 
      value: s.name, 
      label: s.name,
      description: s.expression,
    }));
  }, [contentSelectors, selectedFormat]);

  const scriptOptions = useMemo(() =>
    scripts.map((s: any) => ({ value: s.name, label: s.name })),
  [scripts]);

  const handleTypeChange = (value: string) => {
    form.send({ type: 'TYPE_CHANGE', value });
  };

  const renderTypeSpecificFields = () => {
    const type = typeId || formData?.type;
    switch (type) {
      case PRIVILEGE_TYPES.WILDCARD:
        return (
          <Box>
            <SettingsTextInput
              {...form.field('properties.pattern')}
              label="Privilege String"
              required
              disabled={isReadOnly}
            />
            <Text as="p" size="1" mt="2" style={{ color: 'var(--gray-11)' }}>
              The internal segment matching algorithm uses Apache Shiro wildcard permissions; see our{' '}
              <a
                href="https://help.sonatype.com/en/privileges.html"
                target="_blank"
                rel="noopener noreferrer"
                style={{ color: 'var(--accent-9)', textDecoration: 'underline' }}
                onMouseDown={(e) => {
                  // Open link before blur event fires to prevent validation from blocking navigation
                  e.preventDefault();
                  window.open('https://help.sonatype.com/en/privileges.html', '_blank', 'noopener,noreferrer');
                }}
              >
                documentation
              </a>
              {' '}for more details.
            </Text>
          </Box>
        );
      case PRIVILEGE_TYPES.APPLICATION:
        return (
          <Box>
            <SettingsCombobox
              {...form.field('properties.domain')}
              label="Domain"
              options={APPLICATION_DOMAINS}
              helpText="The application domain this privilege applies to. Select from suggestions or enter a custom domain (e.g., tasks)"
              placeholder="Select or type a domain..."
              required
              disabled={isReadOnly}
              allowCustom
            />
            <SettingsCheckboxGroup
              {...form.checkboxGroup('properties.actions')}
              label="Actions"
              options={getActionsForPrivilegeType(type)}
              helpText="The specific actions allowed within this domain. Format: comma-separated list (e.g., read,edit)"
              required
              disabled={isReadOnly}
              layout="horizontal"
            />
          </Box>
        );
      case PRIVILEGE_TYPES.REPOSITORY_VIEW:
      case PRIVILEGE_TYPES.REPOSITORY_ADMIN:
        return (
          <Box>
            <SettingsCombobox
              {...form.field('properties.format')}
              label="Repository Format"
              options={formatOptions}
              helpText="The repository format this privilege applies to. Use * for all formats (e.g., maven2)"
              required
              disabled={isReadOnly}
              allowCustom
              placeholder="Select format..."
            />
            <SettingsCombobox
              {...form.field('properties.repository')}
              label="Repository"
              options={repositoryOptions}
              helpText="The specific repository this privilege applies to. Use * for all repositories (e.g., maven-releases)"
              required
              disabled={isReadOnly}
              allowCustom
              placeholder="Select repository..."
            />
            <SettingsCheckboxGroup
              {...form.checkboxGroup('properties.actions')}
              label="Actions"
              options={getActionsForPrivilegeType(type)}
              helpText="The specific administrative or view actions allowed. Format: comma-separated list (e.g., browse,read)"
              required
              disabled={isReadOnly}
              layout="horizontal"
            />
          </Box>
        );
      case PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR:
        return null;
      case PRIVILEGE_TYPES.SCRIPT:
        return (
          <Box>
            <SettingsCombobox
              {...form.field('properties.name')}
              label="Script Name"
              options={scriptOptions}
              helpText="The name of the script this privilege applies to. Select from existing scripts or type a custom name (e.g., security-setup)"
              required
              disabled={isReadOnly}
              allowCustom
              placeholder="Select or type script name..."
            />
            <SettingsCheckboxGroup
              {...form.checkboxGroup('properties.actions')}
              label="Actions"
              options={getActionsForPrivilegeType(type)}
              helpText="The specific script actions allowed. Format: comma-separated list (e.g., add,edit)"
              required
              disabled={isReadOnly}
              layout="horizontal"
            />
          </Box>
        );
      default:
        return null;
    }
  };

  if (isLoading) {
    return (
      <Flex align="center" justify="center" className="privilege-form__loading">
        <Loader2 size={24} className="privilege-form__spinner" />
        <Text size="2">Loading form...</Text>
      </Flex>
    );
  }

  const showSetupSection = !hideActions || wizardStep === 1;
  const showScopeSection = !hideActions || wizardStep === 2;
  const showActionsSection = !hideActions || wizardStep === 2;
  const showConfigSection = !hideActions || wizardStep === 2;
  const showPreviewSection = hideActions && wizardStep === 3;
  const isContentSelector = (typeId || formData?.type) === PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR;
  const isAuditMode = isContentSelector && showPreviewSection;

  const renderFormFields = () => (
    <Box>
      {showSetupSection && (
        <SettingsFormSection title="Privilege Setup" defaultOpen>
          <SettingsTextInput
            {...form.field('name')}
            label="Name"
            helpText="Unique name used to identify this privilege. Use letters, numbers, and underscores (e.g., repository-view-maven)"
            required
            disabled={!isCreate || isReadOnly}
          />
          <SettingsTextArea
            {...form.field('description')}
            label="Description"
            helpText="A brief explanation of what this privilege allows (e.g., Allows viewing of all Maven repositories)"
            rows={2}
            disabled={isReadOnly}
          />
          {!typeId && (
            <SettingsSelect
              name="type"
              label="Type"
              value={formData?.type || ''}
              onChange={handleTypeChange}
              options={context.privilegeTypes?.map((t: any) => ({
                value: t.id,
                label: t.name || getPrivilegeTypeLabel(t.id),
              })) || []}
              helpText="The fundamental category of this privilege which determines its configuration options"
              required
              disabled={!isCreate || isReadOnly}
            />
          )}
        </SettingsFormSection>
      )}
      {showConfigSection && isContentSelector ? (
        <>
          {showScopeSection && (
            <SettingsFormSection title="Content" defaultOpen>
              <SettingsCombobox
                {...form.field('properties.contentSelector')}
                label="Content Selector"
                options={contentSelectorOptions}
                helpText="CSEL expression limits which paths this privilege covers."
                required
                disabled={isReadOnly}
                placeholder="Select content selector..."
              />
            </SettingsFormSection>
          )}
          {showScopeSection && (
            <SettingsFormSection title="Repository" defaultOpen>
              <SettingsCombobox
                {...form.field('properties.repository')}
                label="Repository"
                options={contentSelectorRepositoryOptions}
                helpText="The specific repository this privilege applies to. Use * for all repositories (e.g., npm-all)"
                required
                disabled={isReadOnly}
                allowCustom
                placeholder="Select repository..."
              />
            </SettingsFormSection>
          )}
          {showActionsSection && (
            <SettingsFormSection title="Actions" defaultOpen>
              <SettingsCheckboxGroup
                {...form.checkboxGroup('properties.actions')}
                label="Actions"
                options={getActionsForPrivilegeType(PRIVILEGE_TYPES.REPOSITORY_CONTENT_SELECTOR)}
                helpText="The specific content actions allowed. Format: comma-separated list (e.g., read,browse)"
                required
                disabled={isReadOnly}
                layout="horizontal"
              />
            </SettingsFormSection>
          )}
        </>
      ) : showConfigSection ? (
        <SettingsFormSection title="Configuration" defaultOpen>
          {renderTypeSpecificFields()}
        </SettingsFormSection>
      ) : null}
    </Box>
  );

  const renderFormContent = () => (
    <Box width="100%">
      {isAuditMode ? (
        // Preview step: SelectionInsights full-width, no form fields
        <Box width="100%">
          <SelectionInsights
            repository={selectedRepo}
            allRepositories={formData?.properties?.repository === '*'}
            selectedFormat={selectedFormat}
            contentSelector={selectedSelector}
          />
        </Box>
      ) : (
        <Box width="100%">{renderFormFields()}</Box>
      )}
    </Box>
  );

  if (hideActions) {
    return <Box className="privilege-form">{renderFormContent()}</Box>;
  }

  return (
    <Box className="privilege-form">
      <SettingsForm
        testId="privilege-form"
        onSubmit={() => form.send('SUBMIT')}
        onCancel={onCancel}
        loading={isSaving || loading}
        pristine={form.isPristine}
        error={error || (form.state.matches('error') ? form.state.context.error : undefined)}
        submitLabel={isCreate ? 'Create' : 'Save'}
        submitAnalyticsId={isCreate ? 'nxrm-privilege-create' : 'nxrm-privilege-save'}
        footerExtra={
          !isCreate && onDelete && !isReadOnly ? (
            <SettingsButton
              testId="form-delete"
              variant="danger"
              onClick={onDelete}
              disabled={isSaving || loading}
              icon={Trash2}
              data-analytics-id="nxrm-privilege-delete"
            >
              Delete
            </SettingsButton>
          ) : undefined
        }
      >
        {renderFormContent()}
      </SettingsForm>
    </Box>
  );
}

export default PrivilegeForm;
