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

import React, { useState, useEffect, useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Loader2 } from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsSelect,
  SettingsButton,
  SettingsAlert,
  ConfirmDialog,
} from '../../../shared/form';
import { useToast } from '../../../../shared';
import { useLoggingConfigApi } from './useLoggingConfigApi';
import { Logger, LogLevel, LOG_LEVELS } from './types';

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
export function LoggerForm({
  loggerName,
  isCreate = false,
  onSave,
  onCancel,
  onDelete,
}: LoggerFormProps) {
  const toast = useToast();

  const [name, setName] = useState('');
  const [level, setLevel] = useState<LogLevel>('INFO');
  const [originalLevel, setOriginalLevel] = useState<LogLevel>('INFO');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(!isCreate);
  const [formError, setFormError] = useState<string | null>(null);
  
  // Track if the form has been modified
  const isDirty = isCreate ? name.trim().length > 0 : level !== originalLevel;

  const { fetchLogger, updateLogger, loading, error, setError } = useLoggingConfigApi();

  // Load logger data when editing
  useEffect(() => {
    if (!isCreate && loggerName) {
      const loadLogger = async () => {
        setIsLoading(true);
        try {
          const logger = await fetchLogger(loggerName);
          if (logger) {
            setName(logger.name);
            setLevel(logger.level);
            setOriginalLevel(logger.level);
          }
        } catch (err: any) {
          setFormError(err.message);
        } finally {
          setIsLoading(false);
        }
      };
      loadLogger();
    }
  }, [isCreate, loggerName, fetchLogger]);

  // Validate form
  const validateForm = useCallback((): string | null => {
    if (!name.trim()) {
      return 'Logger name is required';
    }
    return null;
  }, [name]);

  // Handle form submission
  const handleSubmit = useCallback(async (e?: React.FormEvent) => {
    if (e) {
      e.preventDefault();
    }

    const validationError = validateForm();
    if (validationError) {
      setFormError(validationError);
      return;
    }

    setIsSubmitting(true);
    setFormError(null);
    setError(null);

    try {
      await updateLogger(name, level);
      toast.success(isCreate ? `Logger "${name}" created successfully` : `Logger "${name}" updated to ${level}`);
      onSave();
    } catch (err) {
      throw err;
    } finally {
      setIsSubmitting(false);
    }
  }, [name, level, isCreate, validateForm, updateLogger, setError, onSave]);

  // Handle Enter key
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
      {(error || formError) && (
        <Box mb="4">
          <SettingsAlert
            type="error"
            onClose={() => {
              setError(null);
              setFormError(null);
            }}
          >
            {error || formError}
          </SettingsAlert>
        </Box>
      )}

      <SettingsForm
        testId="logger-form"
        onSubmit={handleSubmit}
        onCancel={onCancel}
        dirty={isDirty}
        loading={isSubmitting}
        submitLabel={isCreate ? 'Create Logger' : 'Save'}
        footerExtra={!isCreate && onDelete ? (
          <SettingsButton
            type="button"
            variant="danger"
            onClick={onDelete}
            disabled={isSubmitting || loading}
            testId="form-delete"
          >
            Delete
          </SettingsButton>
        ) : undefined}
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
              onChange={(value) => setLevel(value as LogLevel)}
              options={LOG_LEVELS.map((l) => ({ value: l, label: l }))}
              helpText="Log level: ERROR (least verbose) through TRACE (most verbose)"
            />
            <Text size="1" color="gray" mt="1">
              Setting a level here overrides the inherited level from the parent logger.
              {!isCreate && ' Use "Delete" to revert to the inherited level.'}
            </Text>
          </Box>
        </SettingsFormSection>

        {/* Actions are in the sticky header bar via SettingsForm */}
      </SettingsForm>
    </Box>
  );
}

export default LoggerForm;


