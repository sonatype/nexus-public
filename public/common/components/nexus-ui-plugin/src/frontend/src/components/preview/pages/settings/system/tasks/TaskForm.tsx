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
  Trash2, Loader2, Play, 
  Settings as SettingsIcon, Database, HeartPulse, Tag, MoreHorizontal, ListTodo,
  Wrench, Cloud,
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
  TaskType,
  TaskFormData,
  ScheduleData,
  TaskFormProps,
  ScheduleType,
  NOTIFICATION_CONDITIONS,
  DEFAULT_SCHEDULE_DATA,
} from './types';
import { isSingletonTaskType, isManualOnlyTaskType, filterCreatableTaskTypes } from './taskFieldMetadata';
import { CLOUD_BLOBSTORE_REMOVAL_TYPE_ID } from './tasksFormMachine';

import './TaskForm.scss';

const CATEGORY_ICONS: Record<string, React.ComponentType<{size?: number; className?: string}>> = {
  Admin: SettingsIcon,
  Repository: Database,
  Cleanup: Trash2,
  Repair: Wrench,
  Cloud: Cloud,
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

const WIZARD_STEPS_CREATE = [
  { id: 'type', label: 'Select Type' },
  { id: 'config', label: 'Configure' },
  { id: 'schedule', label: 'Schedule' },
];

const WIZARD_STEPS_EDIT = [
  { id: 'config', label: 'Configure' },
  { id: 'schedule', label: 'Schedule' },
];

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
  const { createTask, updateTask, deleteTask, fetchTasks } = useTasksApi();

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

  // Singleton enforcement (parity with Classic TaskSelectType.filterTasksIfCreated): when a
  // singleton task type already has an instance, it must not be offered in the create flow. Only
  // fetch the existing tasks when a singleton type is actually present in the list (so non-singleton
  // task lists incur no extra request and no state churn).
  const [existingTypeIds, setExistingTypeIds] = useState<Set<string>>(new Set());
  const hasSingletonType = useMemo(() => taskTypes.some((t) => isSingletonTaskType(t.id)), [taskTypes]);

  useEffect(() => {
    if (!isCreate || !hasSingletonType) return;
    let cancelled = false;
    (async () => {
      try {
        const tasks = await fetchTasks();
        if (!cancelled) setExistingTypeIds(new Set((tasks ?? []).map((t) => t.typeId)));
      } catch {
        // Non-fatal: fall back to the unfiltered list rather than blocking creation.
      }
    })();
    return () => { cancelled = true; };
  }, [isCreate, hasSingletonType, fetchTasks]);

  // The type list offered by the selector / URL auto-select, with already-created singletons removed.
  const creatableTaskTypes = useMemo(
    () => filterCreatableTaskTypes(taskTypes, existingTypeIds),
    [taskTypes, existingTypeIds]
  );

  // Wizard state
  const [internalStep, setInternalStep] = useState(0);
  const [selectedTaskTypeObj, setSelectedTaskTypeObj] = useState<TaskType | null>(null);

  // Get selected task type from machine or internal state
  const selectedTaskType = useMemo(() => {
    return selectedTaskTypeObj || machineSelectedType || taskTypes.find((t) => t.id === formData.typeId);
  }, [selectedTaskTypeObj, machineSelectedType, taskTypes, formData.typeId]);

  // Auto-select from URL — only for types still creatable (a singleton that already exists is not
  // in creatableTaskTypes, so a deep-link to re-create it leaves the user on the filtered selector).
  useEffect(() => {
    if (initialTypeId && isCreate && creatableTaskTypes.length > 0 && !formData.typeId) {
      const matchingType = creatableTaskTypes.find((t) => t.id === initialTypeId);
      if (matchingType) {
        form.send({ type: 'TASK_TYPE_CHANGE', value: initialTypeId } as any);
        setSelectedTaskTypeObj(matchingType);
        setInternalStep(1); // Jump to config if URL provides type
      }
    }
  }, [initialTypeId, isCreate, creatableTaskTypes, formData.typeId, form]);

  // Manual-only tasks (e.g. Data Repair Plan) omit the Schedule step entirely — parity with
  // Classic, which hides the schedule fieldset and pins the schedule to 'manual'. On the edit path
  // `task.typeId` is known immediately, so fall back to it to avoid a one-render flash of the
  // Schedule step before the machine resolves selectedTaskType.
  const isManualOnly = isManualOnlyTaskType(selectedTaskType?.id ?? task?.typeId);

  // Steps: Create = [Select Type, Configure, Schedule], Edit = [Configure, Schedule];
  // the Schedule step is dropped for manual-only tasks.
  const effectiveSteps = useMemo(() => {
    const base = isCreate ? WIZARD_STEPS_CREATE : WIZARD_STEPS_EDIT;
    return isManualOnly ? base.filter((s) => s.id !== 'schedule') : base;
  }, [isCreate, isManualOnly]);

  const handleWizardStepChange = useCallback((step: number) => {
    if (isCreate && step === 0) {
      setSelectedTaskTypeObj(null);
      form.send({ type: 'TASK_TYPE_CHANGE', value: '' } as any);
      window.history.replaceState(null, '', '#preview/admin/system/tasks/create');
    } else if (isCreate && step === 1) {
      if (selectedTaskTypeObj) {
        window.history.replaceState(null, '', `#preview/admin/system/tasks/create/${encodeURIComponent(selectedTaskTypeObj.id)}`);
      } else {
        setSelectedTaskTypeObj(null);
        form.send({ type: 'TASK_TYPE_CHANGE', value: '' } as any);
        window.history.replaceState(null, '', '#preview/admin/system/tasks/create');
      }
    }
    setInternalStep(step);
  }, [isCreate, form, selectedTaskTypeObj]);

  const handleTypeSelect = useCallback((type: TaskType) => {
    setSelectedTaskTypeObj(type);
    form.send({ type: 'TASK_TYPE_CHANGE', value: type.id } as any);
  }, [form]);

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

  const allowedSchedules: ScheduleType[] | undefined = useMemo(
    () => (selectedTaskType?.concurrentRun === false ? ['manual', 'once'] : undefined),
    [selectedTaskType?.concurrentRun],
  );

  useEffect(() => {
    if (allowedSchedules && !allowedSchedules.includes(formData.schedule as ScheduleType)) {
      handleScheduleChange({ ...DEFAULT_SCHEDULE_DATA, schedule: 'manual' });
    }
  }, [allowedSchedules, formData.schedule, handleScheduleChange]);

  // Manual-only tasks must persist schedule 'manual'. Only correct a non-manual value so this is a
  // no-op in the normal default-manual case (avoids marking the form dirty on load).
  useEffect(() => {
    if (isManualOnly && formData.schedule !== 'manual') {
      handleScheduleChange({ ...DEFAULT_SCHEDULE_DATA, schedule: 'manual' });
    }
  }, [isManualOnly, formData.schedule, handleScheduleChange]);

  // Indices within effectiveSteps; scheduleStep is -1 when the Schedule step is omitted, so the
  // schedule render guard (internalStep === scheduleStep) never matches.
  const configStep = useMemo(() => effectiveSteps.findIndex((s) => s.id === 'config'), [effectiveSteps]);
  const scheduleStep = useMemo(() => effectiveSteps.findIndex((s) => s.id === 'schedule'), [effectiveSteps]);

  const canAdvance = useMemo(() => {
    if (isCreate && internalStep === 0) return !!selectedTaskType;
    if (internalStep === configStep) {
      if (!formData.name?.trim()) return false;
      // Match the classic UI: disable advance until every required dynamic field
      // (e.g. repositoryName) has a non-empty value. The required flag comes from
      // the descriptor via TASK_FIELD_UI / restTemplateToTaskType.
      const fields = selectedTaskType?.formFields || [];
      // Cloud-only quirk: the auto-created blob-store cleanup task clears its
      // blobstoreName picker on load because the underlying blob store is gone.
      // Block Save if blobstoreName is empty to prevent corrupting the backend state.
      // The task can still be viewed and Run manually, but saving with an empty
      // blobstoreName would overwrite the original value in the backend.
      const isCloudBlobstoreRemoval = formData.typeId === CLOUD_BLOBSTORE_REMOVAL_TYPE_ID;
      return fields.every((f) => {
        if (isCloudBlobstoreRemoval && f.id === 'blobstoreName') {
          const blobstoreValue = (formData.properties?.[f.id] ?? '').toString().trim();
          return blobstoreValue !== ''; // Block Save if empty
        }
        return !f.required || (formData.properties?.[f.id] ?? '').toString().trim() !== '';
      });
    }
    return true;
  }, [isCreate, internalStep, selectedTaskType, formData.name, formData.properties, formData.typeId, configStep]);

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
      currentStep={internalStep}
      onStepChange={handleWizardStepChange}
      onComplete={() => form.send('SUBMIT')}
      onCancel={onCancel}
      completeLabel={isCreate ? 'Create Task' : 'Save Task'}
      submitAnalyticsId="nxrm-task-save"
      canAdvance={canAdvance}
      dirty={internalStep >= configStep}
      noDirtyTracking={internalStep < configStep}
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
                data-analytics-id="nxrm-task-run"
              >
                Run Now
              </SettingsButton>
            )}
            <SettingsButton
              variant="danger"
              onClick={() => form.send('DELETE')}
              disabled={form.isSaving || loading}
              icon={Trash2}
              data-analytics-id="nxrm-task-delete"
            >
              Delete
            </SettingsButton>
          </Flex>
        ) : undefined
      }
      className="task-form"
    >
      {/* Step 0 (Create only): Select Type - flat table */}
      {isCreate && internalStep === 0 && (
        <Box className="task-form__type-selector">
          <TaskTypeSelector
            taskTypes={creatableTaskTypes}
            onSelect={handleTypeSelect}
            loading={form.isLoading}
            selectedType={selectedTaskTypeObj}
          />
        </Box>
      )}

      {/* Configure step */}
      {internalStep === configStep && (
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
                  <Text size="2" color="gray" className="task-form__description-text">
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

      {/* Schedule step */}
      {internalStep === scheduleStep && (
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
            allowedSchedules={allowedSchedules}
          />
        </SettingsFormSection>
      )}
    </WizardForm>
  );
}

export default TaskForm;
