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

import React from 'react';
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { Archive, Server } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsCheckbox,
  SettingsSelect,
  SettingsButton,
} from '../../../shared/form';
import { SupportZipParams, ARCHIVED_LOG_OPTIONS } from './types';

import './SupportZipForm.scss';

interface SupportZipFormProps {
  params: SupportZipParams;
  onParamChange: (name: keyof SupportZipParams, value: boolean | number) => void;
  onSubmit: () => void;
  onSubmitAll?: () => void;
  isHa?: boolean;
  disabled?: boolean;
}

/**
 * SupportZipForm - Form for selecting support ZIP contents
 */
export function SupportZipForm({
  params,
  onParamChange,
  onSubmit,
  onSubmitAll,
  isHa = false,
  disabled = false,
}: SupportZipFormProps) {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    <Box className="support-zip-form">
      {/* Description */}
      <Box className="support-zip-form__description" mb="4">
        <Text size="2" color="gray">
          No information will be sent to Sonatype when creating the support ZIP file.
        </Text>
        <Text size="2" color="gray" mt="2">
          Support ZIP creation may take a few minutes to complete.
        </Text>
      </Box>

      <SettingsForm testId="support-zip-form" onSubmit={handleSubmit} showActions={false} pristine={false} noDirtyTracking>
        {/* Contents Section */}
        <SettingsFormSection title="Contents">
          <Box className="support-zip-form__checkboxes">
            <SettingsCheckbox
              name="systemInformation"
              label="System information report"
              checked={params.systemInformation}
              onChange={(checked) => onParamChange('systemInformation', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-systemInformation"
            />
            <SettingsCheckbox
              name="threadDump"
              label="JVM thread-dump"
              checked={params.threadDump}
              onChange={(checked) => onParamChange('threadDump', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-threadDump"
            />
            <SettingsCheckbox
              name="configuration"
              label="Configuration files"
              checked={params.configuration}
              onChange={(checked) => onParamChange('configuration', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-configuration"
            />
            <SettingsCheckbox
              name="security"
              label="Security configuration files"
              checked={params.security}
              onChange={(checked) => onParamChange('security', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-security"
            />
            <SettingsCheckbox
              name="log"
              label="Log files"
              checked={params.log}
              onChange={(checked) => onParamChange('log', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-log"
            />
            <SettingsCheckbox
              name="taskLog"
              label="Task log files"
              checked={params.taskLog}
              onChange={(checked) => onParamChange('taskLog', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-taskLog"
            />
            <SettingsCheckbox
              name="replication"
              label="Replication log files"
              checked={params.replication}
              onChange={(checked) => onParamChange('replication', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-replication"
            />
            <SettingsCheckbox
              name="auditLog"
              label="Audit log files"
              checked={params.auditLog}
              onChange={(checked) => onParamChange('auditLog', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-auditLog"
            />
            <SettingsCheckbox
              name="metrics"
              label="System and component metrics"
              checked={params.metrics}
              onChange={(checked) => onParamChange('metrics', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-metrics"
            />
            <SettingsCheckbox
              name="jmx"
              label="JMX information"
              checked={params.jmx}
              onChange={(checked) => onParamChange('jmx', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-jmx"
            />

            {/* Archived logs dropdown */}
            <Box mt="3">
              <SettingsSelect
                label="Include logs from previous days"
                value={String(params.archivedLog)}
                onChange={(value) => onParamChange('archivedLog', Number(value))}
                options={ARCHIVED_LOG_OPTIONS.map((opt) => ({
                  value: String(opt.value),
                  label: opt.label,
                }))}
                disabled={disabled}
                data-testid="support-zip-select-archivedLog"
              />
            </Box>
          </Box>
        </SettingsFormSection>

        {/* Options Section */}
        <SettingsFormSection title="Options">
          <Box className="support-zip-form__checkboxes">
            <SettingsCheckbox
              name="limitFileSizes"
              label="Limit files in the ZIP archive to 30 MB apiece"
              checked={params.limitFileSizes}
              onChange={(checked) => onParamChange('limitFileSizes', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-limitFileSizes"
            />
            <SettingsCheckbox
              name="limitZipSize"
              label="Limit the ZIP archive to 50 MB"
              checked={params.limitZipSize}
              onChange={(checked) => onParamChange('limitZipSize', checked)}
              disabled={disabled}
              data-testid="support-zip-checkbox-limitZipSize"
            />
          </Box>
        </SettingsFormSection>

        {/* Actions */}
        <Flex gap="3" mt="4">
          <SettingsButton
            type="submit"
            variant="primary"
            disabled={disabled}
            icon={Archive}
            data-testid="support-zip-create-button"
          >
            Create support ZIP
          </SettingsButton>
          {isHa && onSubmitAll && (
            <SettingsButton
              type="button"
              variant="primary"
              onClick={onSubmitAll}
              disabled={disabled}
              icon={Server}
              data-testid="support-zip-create-all-button"
            >
              Create support ZIP (all nodes)
            </SettingsButton>
          )}
        </Flex>
      </SettingsForm>
    </Box>
  );
}

export default SupportZipForm;

