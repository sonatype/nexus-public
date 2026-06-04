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

import React, { useMemo, useCallback, useEffect, useState } from 'react';
import { Box, Flex, Text, Badge } from '@radix-ui/themes';
import {
  Trash2, Loader2, Play, ArrowLeft,
  Settings as SettingsIcon, Database, HeartPulse, Tag, MoreHorizontal, ListTodo,
} from 'lucide-react';

import {
  WizardForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsCheckbox,
  SettingsSelect,
  SettingsButton,
} from '../../../../shared/form';

import { TaskTypeSelector, DynamicFormFields } from './TaskTypeSelector';
import { FormatIcon } from '../../repository/repositories/components/FormatIcon';
import { TaskScheduler } from './TaskScheduler';
import { useTasksApi } from './useTasksApi';
import { useTasksForm } from './useTasksForm';
import { getTaskTypeDescription, getTaskTypeCategory } from './taskTypeDescriptions';

import {
  Task,
  TaskType,
  TaskFormData,
  ScheduleData,
  TaskFormProps,
  NOTIFICATION_CONDITIONS,
} from './types';

import './TaskForm.scss';

const CATEGORY_ICONS: Record<string, React.ComponentType<{size?: number; className?: string}>> = {
  Admin: SettingsIcon,
  Repository: Database,
  Cleanup: Trash2,
  'Health Check': HeartPulse,
  Tags: Tag,
  Other: MoreHorizontal,
};

const CATEGORY_FORMAT_MAP: Record<string, string> = {
  Maven: 'maven2',
  Docker: 'docker',
  npm: 'npm',
  Yum: 'yum',
  APT: 'apt',
  Helm: 'helm',
  R: 'r',
  PyPI: 'pypi',
  Conda: 'conda',
  RubyGems: 'rubygems',
  Go: 'go',
  P2: 'p2',
  Conan: 'conan',
  NuGet: 'nuget',
  CocoaPods: 'cocoapods',
  Raw: 'raw',
};

/**
 * TaskForm - Create/Edit form for tasks using a multi-step Wizard pattern.
 */
export function TaskForm({
  task,
  taskTypes: taskTypesFromProps,
  isCreate,
  initialTypeId,
  onSave,
  onCancel,
  onRun,
  onTypeChange,
  loading = false,
  error,
}: TaskFormProps & { initialTypeId?: string; onTypeChange?: (typeId: string) => void }) {
  const { createTask, updateTask, deleteTask } = useTasksApi();

  const {
    form,
    taskTypes: machineTaskTypes,
    selectedTaskType: machineSelectedType,
  } = useTasksForm({
    taskId: isCreate ? undefined : task?.id,
    task: task || undefined,
    onCancel,
    createTask,
    updateTask,
    deleteTask,
  });

  const formData = form.data as TaskFormData & { startTime?: string };
  const taskTypes = taskTypesFromProps || machineTaskTypes || [];

  // Wizard state
  const [internalStep, setInternalStep] = useState(0);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedTaskTypeObj, setSelectedTaskTypeObj] = useState<TaskType | null>(null);

  // Get selected task type from machine or internal state
  const selectedTaskType = useMemo(() => {
    return selectedTaskTypeObj || machineSelectedType || taskTypes.find((t) => t.id === formData.typeId);
  }, [selectedTaskTypeObj, machineSelectedType, taskTypes, formData.typeId]);

  // Sync category if editing
  useEffect(() => {
    if (!isCreate && selectedTaskType && !selectedCategory) {
      setSelectedCategory(getTaskTypeCategory(selectedTaskType.id));
    }
  }, [isCreate, selectedTaskType, selectedCategory]);

  // Auto-select from URL
  useEffect(() => {
    if (initialTypeId && isCreate && taskTypes.length > 0 && !formData.typeId) {
      const matchingType = taskTypes.find((t) => t.id === initialTypeId);
      if (matchingType) {
        form.send({ type: 'TASK_TYPE_CHANGE', value: initialTypeId } as any);
        setSelectedCategory(getTaskTypeCategory(initialTypeId));
        setSelectedTaskTypeObj(matchingType);
        setInternalStep(2); // Jump to config if URL provides type
      }
    }
  }, [initialTypeId, isCreate, taskTypes, formData.typeId, form]);

  const steps = [
    { id: 'category', label: 'Select Category' },
    { id: 'type', label: 'Select Type' },
    { id: 'config', label: 'Configure' },
    { id: 'schedule', label: 'Schedule' },
  ];

  // Adjust steps for Edit mode (no type selection)
  const effectiveSteps = isCreate ? steps : steps.slice(2);
  const effectiveCurrentStep = isCreate ? internalStep : internalStep;

  const handleWizardStepChange = useCallback((step: number) => {
    if (step === 0) {
      setSelectedCategory(null);
      setSelectedTaskTypeObj(null);
      form.send({ type: 'TASK_TYPE_CHANGE', value: '' } as any);
      window.history.replaceState(null, '', '#preview/admin/system/tasks/create');
    } else if (step === 1) {
      setSelectedTaskTypeObj(null);
      form.send({ type: 'TASK_TYPE_CHANGE', value: '' } as any);
      window.history.replaceState(null, '', '#preview/admin/system/tasks/create');
    } else if (step === 2 && selectedTaskTypeObj) {
      window.history.replaceState(null, '', `#preview/admin/system/tasks/create/${encodeURIComponent(selectedTaskTypeObj.id)}`);
    }
    setInternalStep(step);
  }, [form, selectedTaskTypeObj]);

  const handleSelectionChange = useCallback((canAdvance: boolean, selection: any) => {
    if (internalStep === 0) {
      // Category selection change
    } else if (internalStep === 1) {
      if (selection) {
        setSelectedTaskTypeObj(selection);
        form.send({ type: 'TASK_TYPE_CHANGE', value: selection.id } as any);
        // DO NOT call onTypeChange here - it triggers a re-mount via route change
      }
    }
  }, [internalStep, form]);

  const handlePropertyChange = useCallback((fieldId: string, value: string) => {
    const currentProps = formData.properties || {};
    form.send({ type: 'UPDATE', name: 'properties', value: { ...currentProps, [fieldId]: value } });
  }, [form, formData.properties]);

  const handleScheduleChange = useCallback((data: ScheduleData) => {
    form.send({ type: 'SCHEDULE_CHANGE', value: data.schedule, data } as any);
  }, [form]);

  const scheduleData: ScheduleData = useMemo(() => ({
    schedule: formData.schedule || 'manual',
    startDate: formData.startDate ? new Date(formData.startDate as any) : null,
    startTime: formData.startTime || '00:00',
    recurringDays: formData.recurringDays || [],
    cronExpression: formData.cronExpression || '',
    timeZoneOffset: formData.timeZoneOffset || '',
  }), [formData]);

  const canAdvance = useMemo(() => {
    if (internalStep === 0) return !!selectedCategory;
    if (internalStep === 1) return !!selectedTaskType;
    if (internalStep === 2) return !!formData.name?.trim();
    return true;
  }, [internalStep, selectedCategory, selectedTaskType, formData.name]);

  if (form.isLoading) {
    return (
      <Flex align="center" justify="center" gap="2" p="4">
        <Loader2 size={24} className="animate-spin" />
        <Text size="2">Loading form...</Text>
      </Flex>
    );
  }

  return (
    <WizardForm
      steps={effectiveSteps}
      currentStep={effectiveCurrentStep}
      onStepChange={handleWizardStepChange}
      onComplete={() => form.send('SUBMIT')}
      onCancel={onCancel}
      completeLabel={isCreate ? 'Create Task' : 'Save Task'}
      canAdvance={canAdvance}
      dirty={internalStep >= 2}
      noDirtyTracking={internalStep < 2}
      loading={form.isSaving || loading}
      error={error || form.saveError || undefined}
      testId="task-wizard"
      footerExtra={
        !isCreate ? (
          <Flex gap="2">
            {onRun && task?.runnable && (
              <SettingsButton
                variant="secondary"
                onClick={onRun}
                disabled={form.isSaving || loading}
                icon={Play}
                testId="task-run-now"
              >
                Run Now
              </SettingsButton>
            )}
            <SettingsButton
              variant="danger"
              onClick={() => form.send('DELETE')}
              disabled={form.isSaving || loading}
              icon={Trash2}
            >
              Delete
            </SettingsButton>
          </Flex>
        ) : undefined
      }
      className="task-form"
    >
      {/* Step 0: Category Selection */}
      {isCreate && internalStep === 0 && (
        <Box className="task-form__type-selector">
          <TaskTypeSelector
            taskTypes={taskTypes}
            onSelect={() => {}}
            loading={form.isLoading}
            mode="category"
            selectedCategory={selectedCategory}
            onCategorySelect={setSelectedCategory}
            onSelectionChange={handleSelectionChange}
          />
        </Box>
      )}

      {/* Step 1: Type Selection */}
      {isCreate && internalStep === 1 && (
        <Box className="task-form__type-selector">
          <TaskTypeSelector
            taskTypes={taskTypes}
            onSelect={() => {}}
            loading={form.isLoading}
            mode="type"
            selectedCategory={selectedCategory}
            onCategorySelect={setSelectedCategory}
            onSelectionChange={handleSelectionChange}
          />
        </Box>
      )}

      {/* Step 2: Configure */}
      {internalStep === 2 && (
        <>
          {selectedTaskType && (
            <Box className="task-form__selected-type">
              <Flex align="center" gap="3">
                {CATEGORY_FORMAT_MAP[getTaskTypeCategory(selectedTaskType.id)] ? (
                  <FormatIcon format={CATEGORY_FORMAT_MAP[getTaskTypeCategory(selectedTaskType.id)]} size={48} />
                ) : (
                  <Box className="repository-type-selector__type-icon repository-type-selector__type-icon--hosted">
                    {React.createElement(CATEGORY_ICONS[getTaskTypeCategory(selectedTaskType.id)] || ListTodo, { size: 24 })}
                  </Box>
                )}
                <Box>
                  <Flex align="center" gap="2">
                    <Text weight="bold" size="4">{selectedTaskType.name}</Text>
                    {selectedTaskType.exposed === false && (
                      <Badge size="1" color="purple" variant="soft">PRO</Badge>
                    )}
                  </Flex>
                  <Text size="2" color="gray" style={{ display: 'block' }}>
                    {getTaskTypeCategory(selectedTaskType.id)} • {getTaskTypeDescription(selectedTaskType.id)}
                  </Text>
                </Box>
              </Flex>
            </Box>
          )}

          <SettingsFormSection title="Settings">
            <SettingsCheckbox
              name="enabled"
              label="Enabled"
              checked={formData.enabled}
              onChange={(checked) => form.send({ type: 'UPDATE', name: 'enabled', value: checked })}
              helpText="Enable or disable task execution"
              disabled={form.isSaving || loading}
            />

            <SettingsTextInput
              {...form.field('name')}
              label="Name"
              placeholder="e.g. Daily Cleanup"
              helpText="A unique name for this task"
              required
              disabled={form.isSaving || loading}
              autoComplete="off"
            />

            <SettingsTextInput
              {...form.field('alertEmail')}
              label="Email"
              placeholder="e.g. admin@example.com"
              helpText="Email address to receive task notifications"
              disabled={form.isSaving || loading}
              autoComplete="off"
            />

            <SettingsSelect
              name="notificationCondition"
              label="Send Notification"
              value={formData.notificationCondition || 'FAILURE'}
              onChange={(value) => form.send({ type: 'UPDATE', name: 'notificationCondition', value })}
              options={NOTIFICATION_CONDITIONS}
              helpText="When to send email: on failure, success, or both"
              disabled={form.isSaving || loading}
            />
          </SettingsFormSection>

          {selectedTaskType?.formFields && selectedTaskType.formFields.length > 0 && (
            <SettingsFormSection title="Task Configuration">
              <DynamicFormFields
                taskType={selectedTaskType}
                values={formData.properties}
                onChange={handlePropertyChange}
                errors={form.validationErrors?.properties as any}
                disabled={form.isSaving || loading}
              />
            </SettingsFormSection>
          )}
        </>
      )}

      {/* Step 3: Schedule */}
      {internalStep === 3 && (
        <SettingsFormSection title="Schedule">
          <TaskScheduler
            value={scheduleData}
            onChange={handleScheduleChange}
            errors={{
              schedule: form.touched?.schedule ? form.validationErrors?.schedule : undefined,
              startDate: form.touched?.startDate ? form.validationErrors?.startDate : undefined,
              startTime: form.touched?.startTime ? form.validationErrors?.startTime : undefined,
              recurringDays: form.touched?.recurringDays ? form.validationErrors?.recurringDays : undefined,
              cronExpression: form.touched?.cronExpression ? form.validationErrors?.cronExpression : undefined,
            }}
            disabled={form.isSaving || loading}
          />
        </SettingsFormSection>
      )}
    </WizardForm>
  );
}

export default TaskForm;
