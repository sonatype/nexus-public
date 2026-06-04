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

import React, { useState, useCallback, useMemo } from 'react';
import { Box, Flex, Text, Heading, Badge, Tabs, Spinner } from '@radix-ui/themes';
import {
  Trash2, Play, Square, FileText, Settings, Calendar, History, Edit3,
  CheckCircle, XCircle, AlertCircle, Clock, Mail, Info, Puzzle, RefreshCw,
} from 'lucide-react';

import {
  SettingsForm,
  SettingsFormSection,
  SettingsTextInput,
  SettingsToggle,
  SettingsSelect,
  SettingsButton,
} from '../../../../shared/form';
import { ConfirmDialog } from '../../../../shared';
import { DeleteConfirmationModal } from '../../../../shared/modals/DeleteConfirmationModal';
import { TaskScheduler } from './TaskScheduler';
import { DynamicFormFields } from './TaskTypeSelector';
import { TaskHistory } from './TaskHistory';
import { useTasksForm } from './useTasksForm';
import { useTasksApi } from './useTasksApi';
import {
  Task,
  TaskType,
  TaskFormData,
  TaskDetailProps,
  ScheduleData,
  formatDate,
  getStatusColor,
  NOTIFICATION_CONDITIONS,
} from './types';

import './TaskDetail.scss';

function getStatusBadgeColor(status: string): 'blue' | 'green' | 'gray' | 'yellow' | 'red' {
  switch (status) {
    case 'RUNNING': return 'blue';
    case 'OK': return 'green';
    case 'WAITING': return 'gray';
    case 'BLOCKED': return 'yellow';
    default: return 'red';
  }
}

function getStatusIcon(status: string) {
  switch (status) {
    case 'RUNNING': return <RefreshCw size={16} className="task-detail__status-icon task-detail__status-icon--running" />;
    case 'OK': return <CheckCircle size={16} className="task-detail__status-icon task-detail__status-icon--ok" />;
    case 'WAITING': return <Clock size={16} className="task-detail__status-icon task-detail__status-icon--waiting" />;
    case 'BLOCKED': return <AlertCircle size={16} className="task-detail__status-icon task-detail__status-icon--blocked" />;
    default: return <XCircle size={16} className="task-detail__status-icon task-detail__status-icon--error" />;
  }
}

// Summary item icon mapping
const SUMMARY_ICONS: Record<string, React.ComponentType<{ size?: number; className?: string }>> = {
  status: CheckCircle,
  lastResult: FileText,
  schedule: Calendar,
  nextRun: Clock,
  lastRun: History,
  alertEmail: Mail,
  description: Info,
};

export function TaskDetail({
  task: initialTask,
  taskId: taskIdFromRoute,
  loading: initialLoading = false,
  canEdit = false,
  canDelete = false,
  canRun = false,
  canStop = false,
  onSave: onSaveProp,
  onDelete: onDeleteProp,
  onRun,
  onStop,
  onCancel,
  error: externalError,
}: TaskDetailProps) {
  const { createTask, updateTask, deleteTask } = useTasksApi();
  const [activeTab, setActiveTab] = useState('summary');
  const [confirmAction, setConfirmAction] = useState<'delete' | 'run' | 'stop' | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Use the unified form hook - pass taskId from route when task not yet loaded
  const {
    form,
    task,
    selectedTaskType,
  } = useTasksForm({
    taskId: initialTask?.id ?? taskIdFromRoute ?? undefined,
    task: initialTask || undefined,
    onCancel,
    createTask: async (data, startTime) => {
      const result = await createTask(data, startTime);
      if (onSaveProp) await onSaveProp(data);
      return result;
    },
    updateTask: async (id, data, startTime) => {
      const result = await updateTask(id, data, startTime);
      if (onSaveProp) await onSaveProp(data);
      return result;
    },
    deleteTask: async (id) => {
      await deleteTask(id);
      if (onDeleteProp) await onDeleteProp();
    },
  });

  const formData = form.data as TaskFormData & { startTime?: string };

  // Use task from form machine when loaded; fall back to initialTask (e.g. when TasksPage
  // fetched it before machine load, or in tests where useForm is mocked)
  const displayTask = task ?? initialTask ?? null;

  const handlePropertyChange = useCallback((fieldId: string, value: string) => {
    const currentProps = formData.properties || {};
    form.send({ type: 'UPDATE', name: 'properties', value: { ...currentProps, [fieldId]: value } });
  }, [form, formData.properties]);

  const handleScheduleChange = useCallback((data: ScheduleData) => {
    form.send({ type: 'SCHEDULE_CHANGE', value: data.schedule, data } as any);
  }, [form]);

  // Build schedule data from form data
  const scheduleData: ScheduleData = useMemo(() => ({
    schedule: formData.schedule || 'manual',
    startDate: formData.startDate ? new Date(formData.startDate as any) : null,
    startTime: formData.startTime || '00:00',
    recurringDays: formData.recurringDays || [],
    cronExpression: formData.cronExpression || '',
    timeZoneOffset: formData.timeZoneOffset || '',
  }), [formData]);

  if (initialLoading || !displayTask) {
    return (
      <Box className="task-detail task-detail--loading">
        <Flex align="center" justify="center" gap="3" p="6">
          <Spinner size="3" />
          <Text>Loading task details...</Text>
        </Flex>
      </Box>
    );
  }

  const footerActions = (
    <Flex gap="2" align="center">
      {activeTab === 'summary' && canEdit && !form.isDirty && displayTask && (
        <SettingsButton
          variant="secondary"
          icon={Edit3}
          onClick={() => setActiveTab('settings')}
          testId="task-edit-button"
        >
          Edit
        </SettingsButton>
      )}
      {canDelete && (
        <SettingsButton
          variant="danger"
          icon={Trash2}
          onClick={() => {
            setConfirmAction('delete');
          }}
          disabled={form.isSaving}
          testId="form-delete"
        >
          Delete
        </SettingsButton>
      )}
      {displayTask.status === 'RUNNING' && canStop ? (
        <SettingsButton
          variant="secondary"
          icon={Square}
          onClick={() => setConfirmAction('stop')}
          testId="task-stop"
        >
          Stop
        </SettingsButton>
      ) : displayTask.enabled && canRun && displayTask.status !== 'RUNNING' ? (
        <SettingsButton
          variant="secondary"
          icon={Play}
          onClick={() => setConfirmAction('run')}
          testId="task-run"
        >
          Run Now
        </SettingsButton>
      ) : null}
    </Flex>
  );

  return (
    <Box className="task-detail" data-testid="task-detail">
      <SettingsForm
        onSubmit={() => form.send('SUBMIT')}
        onCancel={onCancel}
        loading={form.isSaving}
        pristine={form.isPristine}
        error={externalError || form.saveError || undefined}
        submitLabel="Save"
        cancelLabel="Back"
        showActions={canEdit}
        footerExtra={footerActions}
        testId="task-detail-form"
      >
        {/* Task Header */}
        <Box className="task-detail__header">
          <Flex justify="between" align="start">
            <Box>
              <Heading size="5" weight="bold">{displayTask.name}</Heading>
              <Text size="2" color="gray">{displayTask.typeName}</Text>
            </Box>
            <Flex gap="2" align="center">
              <Flex align="center" gap="2" className={`task-detail__status-badge ${displayTask.status === 'RUNNING' ? 'task-detail__status-badge--running' : ''}`}>
                {getStatusIcon(displayTask.status)}
                <Badge
                  color={getStatusBadgeColor(displayTask.status)}
                  variant="soft"
                  data-testid="task-status-badge"
                >
                  {displayTask.status}
                </Badge>
              </Flex>
              <Badge
                color={displayTask.enabled ? 'green' : 'gray'}
                variant="outline"
                data-testid="task-enabled-badge"
              >
                {displayTask.enabled ? 'Enabled' : 'Disabled'}
              </Badge>
            </Flex>
          </Flex>
        </Box>

        {/* 4 Tabs */}
        <Tabs.Root value={activeTab} onValueChange={setActiveTab} className="task-detail__tabs">
          <Tabs.List>
            <Tabs.Trigger value="summary">
              <FileText size={16} /> Summary
            </Tabs.Trigger>
            <Tabs.Trigger value="settings">
              <Settings size={16} /> Settings
            </Tabs.Trigger>
            <Tabs.Trigger value="schedule">
              <Calendar size={16} /> Schedule
            </Tabs.Trigger>
            <Tabs.Trigger value="history">
              <History size={16} /> History
            </Tabs.Trigger>
          </Tabs.List>

          <Box className="task-detail__tab-content">
            {/* Summary Tab */}
            <Tabs.Content value="summary">
              <Box className="task-detail__summary-grid">
                <SummaryItem
                  icon={<CheckCircle size={16} />}
                  label="Status"
                  value={displayTask.status}
                />
                <SummaryItem
                  icon={<FileText size={16} />}
                  label="Last Result"
                  value={displayTask.lastRunResult || '-'}
                />
                <SummaryItem
                  icon={<Calendar size={16} />}
                  label="Schedule"
                  value={displayTask.schedule || '-'}
                  onClick={canEdit ? () => setActiveTab('schedule') : undefined}
                />
                <SummaryItem
                  icon={<Clock size={16} />}
                  label="Next Run"
                  value={displayTask.nextRun ? formatDate(displayTask.nextRun) : '-'}
                />
                <SummaryItem
                  icon={<History size={16} />}
                  label="Last Run"
                  value={displayTask.lastRun ? formatDate(displayTask.lastRun) : 'Never'}
                />
                <SummaryItem
                  icon={<Mail size={16} />}
                  label="Alert Email"
                  value={displayTask.alertEmail || 'Not configured'}
                  onClick={canEdit ? () => setActiveTab('settings') : undefined}
                />
                {displayTask.statusDescription && (
                  <SummaryItem
                    icon={<Info size={16} />}
                    label="Description"
                    value={displayTask.statusDescription}
                  />
                )}
                {Object.entries(displayTask.properties || {}).filter(([k]) => !k.startsWith('.')).map(([k, v]) => (
                  <SummaryItem
                    key={k}
                    icon={<Puzzle size={16} />}
                    label={k}
                    value={v || '-'}
                    onClick={canEdit ? () => setActiveTab('settings') : undefined}
                  />
                ))}
              </Box>
            </Tabs.Content>

            {/* Settings Tab (editable via form hook) */}
            <Tabs.Content value="settings">
              <SettingsFormSection title="Basic Settings">
                <SettingsToggle
                  name="enabled"
                  label="Task Status"
                  checked={formData.enabled}
                  onChange={(checked) => form.send({ type: 'UPDATE', name: 'enabled', value: checked })}
                  disabled={!canEdit || form.isSaving}
                  helpText="Enable or disable this task"
                  checkedText="Enabled"
                  uncheckedText="Disabled"
                />
                <SettingsTextInput
                  name="name"
                  label="Name"
                  value={displayTask.name}
                  onChange={() => {}}
                  disabled
                  helpText="Task name (cannot be changed)"
                />
                <SettingsTextInput
                  name="typeId"
                  label="Task Type"
                  value={displayTask.typeName}
                  onChange={() => {}}
                  disabled
                  helpText="Task type (cannot be changed after creation)"
                />
                <SettingsTextInput
                  {...form.field('alertEmail')}
                  label="Notification Email"
                  disabled={!canEdit || form.isSaving}
                  helpText="Leave empty to disable notifications. Enter an email to receive alerts."
                  placeholder="admin@example.com"
                />
                {formData.alertEmail && (
                  <SettingsSelect
                    name="notificationCondition"
                    label="Send Notification"
                    value={formData.notificationCondition || 'FAILURE'}
                    onChange={(v) => form.send({ type: 'UPDATE', name: 'notificationCondition', value: v })}
                    disabled={!canEdit || form.isSaving}
                    options={NOTIFICATION_CONDITIONS}
                    helpText="When to send email notifications"
                  />
                )}
              </SettingsFormSection>

              {selectedTaskType?.formFields && selectedTaskType.formFields.length > 0 && (
                <SettingsFormSection title="Task Configuration">
                  <DynamicFormFields
                    taskType={selectedTaskType}
                    values={formData.properties}
                    onChange={handlePropertyChange}
                    errors={form.validationErrors?.properties as any}
                    disabled={!canEdit || form.isSaving}
                  />
                </SettingsFormSection>
              )}
            </Tabs.Content>

            {/* Schedule Tab */}
            <Tabs.Content value="schedule">
              <SettingsFormSection title="Schedule Configuration">
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
                  disabled={!canEdit || form.isSaving}
                />
              </SettingsFormSection>
            </Tabs.Content>

            {/* History Tab */}
            <Tabs.Content value="history">
              <TaskHistory task={displayTask} />
            </Tabs.Content>
          </Box>
        </Tabs.Root>
      </SettingsForm>

      <DeleteConfirmationModal
        open={confirmAction === 'delete'}
        onClose={() => setConfirmAction(null)}
        onConfirm={async () => {
          setIsDeleting(true);
          try {
            setConfirmAction(null);
            form.send('DELETE');
            await form.confirmDelete();
          } finally {
            setIsDeleting(false);
          }
        }}
        entityName={displayTask.name}
        entityType="task"
        loading={isDeleting}
      />
      <ConfirmDialog
        open={confirmAction === 'run'}
        testId="task-run-dialog"
        onOpenChange={(open) => { if (!open) setConfirmAction(null); }}
        title="Run Task"
        message={`Run "${displayTask.name}" now?`}
        confirmLabel="Run"
        variant="warning"
        onConfirm={() => { setConfirmAction(null); onRun(); }}
      />
      <ConfirmDialog
        open={confirmAction === 'stop'}
        testId="task-stop-dialog"
        onOpenChange={(open) => { if (!open) setConfirmAction(null); }}
        title="Stop Task"
        message={`Stop "${displayTask.name}"?`}
        confirmLabel="Stop"
        variant="warning"
        onConfirm={() => { setConfirmAction(null); onStop(); }}
      />
    </Box>
  );
}

function SummaryItem({
  icon,
  label,
  value,
  onClick,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  onClick?: () => void;
}) {
  return (
    <Box
      className={`task-detail__summary-item ${onClick ? 'task-detail__summary-item--clickable' : ''}`}
      onClick={onClick}
    >
      <Flex align="center" gap="2">
        <Box className="task-detail__summary-icon">{icon}</Box>
        <Text size="1" color="gray" className="task-detail__summary-label">{label}:</Text>
      </Flex>
      <Text size="2" className="task-detail__summary-value">{value}</Text>
    </Box>
  );
}

export default TaskDetail;
