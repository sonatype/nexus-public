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

import React, { useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsSelect,
  SettingsButton,
  SettingsAlert,
} from '../../../../shared/form';
import { useLoggerForm } from './useLoggerForm';
import { LOG_LEVELS } from './types';

import './LoggerForm.scss';

interface LoggerFormProps {
  loggerName?: string | null;
  isCreate?: boolean;
  onSave: () => void;
  onCancel: () => void;
  onDelete?: () => void;
}

/**
 * LoggerForm - Create or edit a logger configuration
 */
export function LoggerForm({ loggerName, isCreate = false, onSave, onCancel, onDelete }: LoggerFormProps) {
  const { name, level, isDirty, isLoading, isSaving, error, setName, setLevel, handleSubmit } = useLoggerForm({
    loggerName,
    isCreate,
    onSave,
    onCancel,
  });

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        handleSubmit();
      }
    },
    [handleSubmit]
  );

  if (isLoading) {
    return (
      <Box className="logger-form logger-form--loading">
        <Flex align="center" justify="center" gap="2" py="9">
          <Loader2 size={20} className="logger-form__spinner" />
          <Text size="2">Loading logger...</Text>
        </Flex>
      </Box>
    );
  }

  return (
    <Box className="logger-form">
      {/* Error alert */}
      {error && (
        <Box mb="4">
          <SettingsAlert type="error">{error}</SettingsAlert>
        </Box>
      )}

      <SettingsForm
        testId="logger-form"
        onSubmit={handleSubmit}
        onCancel={onCancel}
        dirty={isDirty}
        loading={isSaving}
        submitLabel={isCreate ? 'Create Logger' : 'Save'}
        footerExtra={
          !isCreate && onDelete ? (
            <SettingsButton type="button" variant="danger" onClick={onDelete} disabled={isSaving} testId="form-delete">
              Delete
            </SettingsButton>
          ) : undefined
        }
      >
        <SettingsFormSection>
          {/* Logger Name */}
          <Box mb="4">
            <SettingsTextInput
              name="loggerName"
              label="Logger Name"
              value={name}
              onChange={setName}
              placeholder="e.g., org.sonatype.nexus"
              helpText="Fully qualified logger name (e.g., org.sonatype.nexus.repository)"
              disabled={!isCreate}
              required
              onKeyDown={handleKeyDown}
            />
            {isCreate && (
              <Text size="1" color="gray" mt="1">
                Enter the fully qualified logger name (e.g., org.sonatype.nexus.repository)
              </Text>
            )}
          </Box>

          {/* Logger Level */}
          <Box mb="4">
            <SettingsSelect
              name="loggerLevel"
              label="Logger Level"
              value={level}
              onChange={setLevel}
              options={LOG_LEVELS.map((l) => ({ value: l, label: l }))}
              helpText="Log level: ERROR (least verbose) through TRACE (most verbose)"
            />
            <Text size="1" color="gray" mt="1">
              Setting a level here overrides the inherited level from the parent logger.
              {!isCreate && ' Use "Delete" to revert to the inherited level.'}
            </Text>
          </Box>
        </SettingsFormSection>
      </SettingsForm>
    </Box>
  );
}

export default LoggerForm;
