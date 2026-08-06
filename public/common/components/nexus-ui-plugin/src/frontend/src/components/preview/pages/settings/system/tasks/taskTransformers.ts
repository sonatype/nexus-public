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
 * Pure transformers between the REST `/v1/tasks/templates` shape and the UI's
 * `TaskType` shape. Lives in its own module so the form machine can reuse the
 * same enrichment as the API hook without `jest.mock('../useTasksApi')` in
 * TaskDetail tests stubbing out the transformer (the bug fixed under
 * NEXUS-53044 — bare-bones formFields in EDIT vs rich on CREATE).
 */

import { resolveTaskFieldMeta, TASK_SCOPE_DATES, ALL_BLOB_STORES } from './taskFieldMetadata';
import { FormField, TaskStatus, TaskType } from './types';

/**
 * Normalize the backend `TaskState` enum (TaskState.java) to the UI status —
 * the single shared source of truth used by BOTH `useTasksApi.fetchTask`
 * (initial load + polling) and the form machine's `fetchTask`. Keeping one
 * implementation guarantees a page load and a background poll can never disagree
 * about whether a task is running (NEXUS-53525).
 *
 * The raw value can carry a progress suffix while running (e.g.
 * "RUNNING: 42 of 100" — see TaskXO.java) and the running group has several
 * members (RUNNING_STARTING / RUNNING / RUNNING_BLOCKED / RUNNING_CANCELED). We
 * strip the suffix and collapse the whole running group to RUNNING so the badge
 * and Stop button reflect a live run for every task type. DONE-group outcomes
 * map to their terminal status; legacy "DONE" is kept as a fallback.
 */
export function mapRestStateToStatus(state: string): TaskStatus {
  const base = (state || '').split(':')[0].trim().toUpperCase();
  if (base.startsWith('RUNNING')) {
    return 'RUNNING';
  }
  switch (base) {
    case 'WAITING':
      return 'WAITING';
    case 'BLOCKED':
      return 'BLOCKED';
    case 'OK':
    case 'DONE':
      return 'OK';
    case 'FAILED':
      return 'FAILED';
    case 'CANCELED':
      return 'CANCELED';
    case 'INTERRUPTED':
      return 'INTERRUPTED';
    default:
      // Surface (don't silently swallow) a backend TaskState we don't recognize —
      // e.g. a newly added enum value — so the UI quietly mapping it to WAITING is
      // at least visible in the console rather than an invisible wrong status.
      if (base) {
        console.warn(`[tasks] Unknown TaskState "${base}" — treating as WAITING`);
      }
      return 'WAITING';
  }
}

/** Format a date as the backend's plan-date string `MM/DD/YYYY`, in UTC (matches calculateDateRange). */
function formatPlanDate(d: Date): string {
  const mm = String(d.getUTCMonth() + 1).padStart(2, '0');
  const dd = String(d.getUTCDate()).padStart(2, '0');
  return `${mm}/${dd}/${d.getUTCFullYear()}`;
}

/** Parse a duration field the backend stores as a string; non-numeric (e.g. "null") → 0. */
function parseDurationPart(value: string | undefined): number {
  return value && /^\d+$/.test(value) ? parseInt(value, 10) : 0;
}

/**
 * Derive the read-only display properties for the Execute Data Repair Plan task from the sibling
 * Data Repair Plan task's properties. The Execute task itself stores only `planIds`, so its blob
 * store / repository / scope / date-range are sourced from the Plan task — mirroring the backend
 * `TaskComponent.replaceDataRepairProperties` + `calculateDateRange` (which the Classic UI relies on,
 * but the REST `/v1/tasks` contract does not perform). `now` is injected for deterministic testing.
 */
export function deriveExecutePlanProperties(
  planProperties: Record<string, string> | null | undefined,
  now: Date
): Record<string, string> {
  if (!planProperties) {
    return {};
  }

  const derived: Record<string, string> = {
    repositoryName: planProperties.repositoryName ?? '',
    blobstoreName: planProperties.blobstoreName || ALL_BLOB_STORES,
    taskScope: TASK_SCOPE_DATES,
  };

  const start = planProperties.reconcileStartDate;
  if (!start) {
    // The plan used a duration → the displayed range is (now − duration) … now.
    const ms =
      ((parseDurationPart(planProperties.sinceDays) * 24 + parseDurationPart(planProperties.sinceHours)) * 60 +
        parseDurationPart(planProperties.sinceMinutes)) *
      60 *
      1000;
    derived.reconcileStartDate = formatPlanDate(new Date(now.getTime() - ms));
    derived.reconcileEndDate = formatPlanDate(now);
  }
  else {
    derived.reconcileStartDate = start;
    derived.reconcileEndDate = planProperties.reconcileEndDate ?? '';
  }

  // Drop null/undefined and empty-string values. Empty blobstoreName/repositoryName semantically
  // mean "no filter" (same as absent), so omitting them matches how Classic serialises these fields.
  // reconcileEndDate of '' (plan has no end date) is also omitted — the form renders the missing key
  // as '' via the `values[key] || ''` fallback, so the end result is the same either way.
  return Object.fromEntries(Object.entries(derived).filter(([, v]) => v != null && v !== ''));
}

/**
 * REST API per-form-field metadata shape (mirrors `FormFieldInfo.java`).
 * Present on `RestTaskTemplate.formFields` for descriptors that publish their
 * FormField list to the API (NEXUS-53357).
 */
export interface RestFormFieldInfo {
  id: string;
  type?: string;
  label?: string;
  helpText?: string;
  required?: boolean;
  initialValue?: string | number | boolean | null;
  regexValidation?: string | null;
  minValue?: string | null;
  maxValue?: string | null;
  storeApi?: string | null;
  storeFilters?: Record<string, string> | null;
}

/**
 * REST API Task Template shape (mirrors `TaskTemplateXO.java`).
 */
export interface RestTaskTemplate {
  type: string;
  name: string;
  enabled: boolean;
  alertEmail?: string | null;
  notificationCondition?: 'FAILURE' | 'SUCCESS_FAILURE';
  frequency?: unknown;
  properties: Record<string, string>;
  formFields?: RestFormFieldInfo[];
  concurrentRun?: boolean | null;
}

/**
 * Humanize a camelCase property key into a readable label.
 */
export function humanizePropertyKey(key: string): string {
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, (s) => s.toUpperCase())
    .trim();
}

/**
 * Map a REST task template to the UI's TaskType. Both CREATE (useTasksApi) and EDIT
 * (tasksFormMachine) call this so descriptor metadata fixes apply equally.
 *
 * Two paths:
 *  - Preferred (NEXUS-53357): when the backend ships `template.formFields` with per-field
 *    metadata (type/required/label/storeFilters/min/max), pass it through verbatim.
 *  - Legacy fallback: synthesise FormFields from `template.properties`, enriching known
 *    field ids via TASK_FIELD_UI and smart-detecting unknown ones (repo/blobstore/checkbox
 *    by id and value heuristics). Required for OSS / older Pro builds whose descriptors
 *    don't yet publish formFields.
 */
export function restTemplateToTaskType(rest: RestTaskTemplate): TaskType {
  let formFields: FormField[];

  if (Array.isArray(rest.formFields)) {
    formFields = rest.formFields
      // Skip fields that TASK_FIELD_UI marks as hidden (e.g. Data Repair Plan's `name`
      // TemplateFormField, which seeds the task name rather than a property). Matches
      // the legacy branch below so hidden fields never render or POST in either path.
      .filter((meta) => !resolveTaskFieldMeta(rest.type, meta.id)?.hidden)
      .map((meta) => ({
        id: meta.id,
        type: ((meta.type ?? 'string') as FormField['type']),
        label: meta.label ?? meta.id,
        helpText: meta.helpText,
        required: Boolean(meta.required),
        initialValue: meta.initialValue ?? (rest.properties?.[meta.id] ?? ''),
        regexValidation: meta.regexValidation ?? undefined,
        minValue: meta.minValue ?? undefined,
        maxValue: meta.maxValue ?? undefined,
        storeApi: meta.storeApi ?? undefined,
        storeFilters: meta.storeFilters ?? undefined,
      }));
  }
  else {
    formFields = Object.entries(rest.properties || {})
      .filter(([key]) => !resolveTaskFieldMeta(rest.type, key)?.hidden)
      .map(([key, value]) => {
        const fieldMeta = resolveTaskFieldMeta(rest.type, key);

        if (fieldMeta) {
          // Mirrors the inline logic that lived in useTasksApi on main: checkboxes
          // and hidden fields are never user-required; the explicit `required` flag
          // can opt any field out (e.g. ExternalMetadataTask's format field).
          const isRequired = fieldMeta.required !== false && fieldMeta.type !== 'checkbox' && !fieldMeta.hidden;
          return {
            id: key,
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            type: (fieldMeta.type || 'string') as any,
            // Append " *" only for required fields whose label doesn't already end with one.
            // (DynamicFormField also strips the trailing star at render time, so the
            // visible asterisk comes from <span className="dynamic-field__required">.)
            label: !isRequired || fieldMeta.label.trimEnd().endsWith('*') ? fieldMeta.label : `${fieldMeta.label} *`,
            helpText: fieldMeta.helpText || '',
            required: isRequired,
            // Use only what the backend sent — `fieldMeta.placeholder` is a UI hint (HTML
            // placeholder attribute) and must never leak in as the real value.
            // Otherwise a hint like "e.g. maven2" gets serialized back on save.
            initialValue: value ?? '',
            attributes:
              fieldMeta.min !== undefined || fieldMeta.max !== undefined
                ? { minValue: fieldMeta.min, maxValue: fieldMeta.max }
                : undefined,
          };
        }

        // Fallback: smart detection for unknown fields
        const keyLower = key.toLowerCase();
        const isRepo = keyLower.includes('repository');
        const isBlobStore =
          keyLower.includes('blobstore') || keyLower.includes('member') || keyLower.includes('group');
        const isBoolean = value === '' || value === 'true' || value === 'false';

        return {
          id: key,
          type: isRepo ? 'repo' : isBlobStore ? 'blobstore' : isBoolean ? 'checkbox' : 'string',
          label: humanizePropertyKey(key) + ' *',
          helpText: '',
          required: true,
          initialValue: value,
        };
      });
  }

  return {
    id: rest.type,
    name: rest.name,
    exposed: true,
    concurrentRun: rest.concurrentRun ?? undefined,
    formFields: formFields.length > 0 ? formFields : undefined,
  };
}
