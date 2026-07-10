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

/**
 * Types for Tasks feature in Preview UI
 */

// =============================================================================
// SCHEDULE TYPES
// =============================================================================

/**
 * Schedule type identifier
 */
export type ScheduleType = 
  | 'manual'   // Run only manually
  | 'once'     // Run once at specific date/time
  | 'hourly'   // Run every hour starting at specific time
  | 'daily'    // Run daily at specific time
  | 'weekly'   // Run on specific days of the week
  | 'monthly'  // Run on specific days of the month
  | 'advanced'; // Custom cron expression

/**
 * Schedule configuration data
 */
export interface ScheduleData {
  /** Type of schedule */
  schedule: ScheduleType;
  /** Start date/time for the schedule */
  startDate?: Date | null;
  /** Start time (HH:mm format) */
  startTime?: string;
  /** For weekly: array of day numbers (1=Sunday, 2=Monday, ..., 7=Saturday) */
  /** For monthly: array of day numbers (1-31, 999=last day of month) */
  recurringDays?: number[];
  /** For advanced: cron expression string */
  cronExpression?: string;
  /** Client timezone offset (e.g., '+05:30', '-08:00') */
  timeZoneOffset?: string;
}

/**
 * Weekday definition for weekly schedule
 */
export interface WeekDay {
  value: number; // 1=Sunday, 2=Monday, ..., 7=Saturday
  label: string;
  shortLabel: string;
}

/**
 * Month day definition for monthly schedule
 */
export interface MonthDay {
  value: number; // 1-31, 999=last day
  label: string;
}

// =============================================================================
// TASK TYPES
// =============================================================================

/**
 * Task status
 */
export type TaskStatus = 
  | 'WAITING'
  | 'RUNNING' 
  | 'OK'
  | 'BLOCKED'
  | 'CANCELED'
  | 'FAILED'
  | 'INTERRUPTED';

/**
 * Task notification condition
 */
export type NotificationCondition = 
  | 'FAILURE'
  | 'SUCCESS_FAILURE';

/**
 * Form field type for dynamic task properties
 */
export type FormFieldType = 
  | 'string'
  | 'number'
  | 'text'
  | 'checkbox'
  | 'password'
  | 'date'
  | 'url'
  | 'repo'
  | 'repo-or-group'
  | 'blobstore'
  | 'combobox';

/**
 * Form field definition from TaskTypeXO
 */
export interface FormField {
  id: string;
  type: FormFieldType;
  label: string;
  helpText?: string;
  required: boolean;
  disabled?: boolean;
  readOnly?: boolean;
  initialValue?: string | number | boolean | null;
  regexValidation?: string;
  attributes?: Record<string, unknown>;
  storeApi?: string;
  storeFilters?: Array<{ property: string; value: string }>;
  allowAutocomplete?: boolean;
  idMapping?: string;
  nameMapping?: string;
}

/**
 * Task type definition from TaskTypeXO
 */
export interface TaskType {
  id: string;
  name: string;
  exposed: boolean;
  concurrentRun?: boolean;
  formFields?: FormField[];
}

/**
 * Task definition from TaskXO
 */
export interface Task {
  id: string;
  enabled: boolean;
  name: string;
  typeId: string;
  typeName: string;
  status: TaskStatus;
  statusDescription: string;
  nextRun: string | Date | null;
  lastRun: string | Date | null;
  lastRunResult: string | null;
  runnable: boolean;
  stoppable: boolean;
  alertEmail?: string;
  notificationCondition?: NotificationCondition;
  properties: Record<string, string>;
  schedule: ScheduleType;
  startDate?: Date | null;
  recurringDays?: number[];
  cronExpression?: string;
  timeZoneOffset?: string;
  isReadOnlyUi?: boolean;
  runPreviousPlan?: boolean;
}

/**
 * Task form data for create/update
 */
export interface TaskFormData {
  id?: string;
  enabled: boolean;
  name: string;
  typeId: string;
  alertEmail?: string;
  notificationCondition?: NotificationCondition;
  properties: Record<string, string>;
  schedule: ScheduleType;
  startDate?: Date | null;
  recurringDays?: number[];
  cronExpression?: string;
  timeZoneOffset?: string;
}

/**
 * Form validation errors
 */
export interface TaskFormErrors {
  name?: string;
  typeId?: string;
  alertEmail?: string;
  schedule?: string;
  startDate?: string;
  startTime?: string;
  recurringDays?: string;
  cronExpression?: string;
  properties?: Record<string, string>;
}

// =============================================================================
// COMPONENT PROPS
// =============================================================================

/**
 * Props for TasksPage component
 */
export interface TasksPageProps {
  className?: string;
}

/**
 * Props for TasksList component
 */
export interface TasksListProps {
  onSelect: (taskId: string) => void;
  onCreate: () => void;
}

/**
 * Props for TaskDetail component
 */
export interface TaskDetailProps {
  task: Task | null;
  /** Task ID from URL/route - used to fetch task when task prop is not yet loaded */
  taskId?: string | null;
  loading?: boolean;
  canEdit?: boolean;
  canDelete?: boolean;
  canRun?: boolean;
  canStop?: boolean;
  onSave: (data: TaskFormData) => Promise<void>;
  onDelete: () => void;
  onRun: () => void;
  onStop: () => void;
  onCancel: () => void;
  error?: string;
}

/**
 * Props for TaskForm component
 */
export interface TaskFormProps {
  task?: Task | null;
  taskTypes: TaskType[];
  isCreate: boolean;
  onSave: (data: TaskFormData) => Promise<void>;
  onCancel: () => void;
  onRun?: () => void;
  loading?: boolean;
  error?: string;
}

/**
 * Props for TaskScheduler component
 */
export interface TaskSchedulerProps {
  value: ScheduleData;
  onChange: (data: ScheduleData) => void;
  errors?: Pick<TaskFormErrors, 'schedule' | 'startDate' | 'startTime' | 'recurringDays' | 'cronExpression'>;
  disabled?: boolean;
  allowedSchedules?: ScheduleType[];
}

/**
 * Props for TaskTypeSelector component (flat table version)
 */
export interface TaskTypeSelectorProps {
  taskTypes: TaskType[];
  onSelect: (type: TaskType) => void;
  loading?: boolean;
  error?: string | null;
  /** Currently selected type */
  selectedType?: TaskType | null;
}

/**
 * Props for TaskActions component
 */
export interface TaskActionsProps {
  task: Task;
  canEdit?: boolean;
  canDelete?: boolean;
  canRun?: boolean;
  canStop?: boolean;
  onRun?: (taskId: string) => void | Promise<void>;
  onStop?: (taskId: string) => void | Promise<void>;
  onDelete?: (taskId: string) => void | Promise<void>;
  loading?: boolean;
  /** Show text labels on buttons (default: true) */
  showLabels?: boolean;
  /** Render in compact mode (default: false) */
  compact?: boolean;
}

/**
 * Props for TaskHistory component
 */
export interface TaskHistoryProps {
  task: Task;
}

// =============================================================================
// CONSTANTS
// =============================================================================

/**
 * Schedule type options for dropdown
 */
export const SCHEDULE_OPTIONS: Array<{ value: ScheduleType; label: string }> = [
  { value: 'manual', label: 'Manual' },
  { value: 'once', label: 'Once' },
  { value: 'hourly', label: 'Hourly' },
  { value: 'daily', label: 'Daily' },
  { value: 'weekly', label: 'Weekly' },
  { value: 'monthly', label: 'Monthly' },
  { value: 'advanced', label: 'Advanced (Cron)' },
];

/**
 * Week days for weekly schedule
 */
export const WEEKDAYS: WeekDay[] = [
  { value: 1, label: 'Sunday', shortLabel: 'Sun' },
  { value: 2, label: 'Monday', shortLabel: 'Mon' },
  { value: 3, label: 'Tuesday', shortLabel: 'Tue' },
  { value: 4, label: 'Wednesday', shortLabel: 'Wed' },
  { value: 5, label: 'Thursday', shortLabel: 'Thu' },
  { value: 6, label: 'Friday', shortLabel: 'Fri' },
  { value: 7, label: 'Saturday', shortLabel: 'Sat' },
];

/**
 * Month days for monthly schedule (1-31 plus "Last")
 */
export const MONTH_DAYS: MonthDay[] = [
  ...Array.from({ length: 31 }, (_, i) => ({ 
    value: i + 1, 
    label: String(i + 1) 
  })),
  { value: 999, label: 'Last' },
];

/**
 * Notification condition options
 */
export const NOTIFICATION_CONDITIONS: Array<{ value: NotificationCondition; label: string }> = [
  { value: 'FAILURE', label: 'On failure only' },
  { value: 'SUCCESS_FAILURE', label: 'On success or failure' },
];

/**
 * Default task form data
 */
export const DEFAULT_TASK_FORM_DATA: TaskFormData = {
  enabled: true,
  name: '',
  typeId: '',
  alertEmail: '',
  notificationCondition: 'FAILURE',
  properties: {},
  schedule: 'manual',
  startDate: null,
  recurringDays: [],
  cronExpression: '',
  timeZoneOffset: '',
};

/**
 * Default schedule data
 */
export const DEFAULT_SCHEDULE_DATA: ScheduleData = {
  schedule: 'manual',
  startDate: null,
  startTime: '00:00',
  recurringDays: [],
  cronExpression: '',
  timeZoneOffset: '',
};

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

/**
 * Get timezone offset string (e.g., '+05:30', '-08:00')
 */
export const getTimezoneOffset = (): string => {
  const offset = new Date().getTimezoneOffset();
  const sign = offset <= 0 ? '+' : '-';
  const hours = Math.floor(Math.abs(offset) / 60);
  const minutes = Math.abs(offset) % 60;
  return `${sign}${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
};

/**
 * Format date for display
 */
export const formatDate = (date: Date | string | null | undefined): string => {
  if (!date) return '-';
  const d = typeof date === 'string' ? new Date(date) : date;
  return d.toLocaleString();
};

/**
 * Format task status for display with appropriate color
 */
export const getStatusColor = (status: TaskStatus): string => {
  switch (status) {
    case 'RUNNING':
      return 'blue';
    case 'OK':
      return 'green';
    case 'WAITING':
      return 'gray';
    case 'BLOCKED':
      return 'yellow';
    case 'CANCELED':
    case 'FAILED':
    case 'INTERRUPTED':
      return 'red';
    default:
      return 'gray';
  }
};

/**
 * Combine date and time into a single Date object
 */
export const combineDateAndTime = (date: Date | null, time: string): Date | null => {
  if (!date || !time) return null;
  const [hours, minutes] = time.split(':').map(Number);
  const combined = new Date(date);
  combined.setHours(hours, minutes, 0, 0);
  return combined;
};

/**
 * Extract time string from Date object (HH:mm format)
 */
export const extractTime = (date: Date | null): string => {
  if (!date) return '00:00';
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
};

/**
 * Validate cron expression - re-exported from cronValidation module
 * for backward compatibility.
 */
export { isValidCronExpression, validateCronExpression } from './cronValidation';
export type { CronValidationResult } from './cronValidation';

/**
 * Validate email address
 */
export const isValidEmail = (email: string): boolean => {
  if (!email) return true; // Optional field
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

