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

import { TASK_FIELD_UI } from './taskFieldMetadata';
import { TaskType } from './types';

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
 * Map a REST task template to the UI's TaskType, applying TASK_FIELD_UI overrides
 * (label, type, helpText, placeholder, required, etc.). Both CREATE (useTasksApi)
 * and EDIT (tasksFormMachine) call this so descriptor metadata fixes apply equally.
 */
export function restTemplateToTaskType(rest: RestTaskTemplate): TaskType {
  const formFields = Object.entries(rest.properties || {})
    .filter(([key]) => !TASK_FIELD_UI[key]?.hidden)
    .map(([key, value]) => {
      const meta = TASK_FIELD_UI[key];

      if (meta) {
        // Mirrors the inline logic that lived in useTasksApi on main: checkboxes
        // and hidden fields are never user-required; the explicit `required` flag
        // can opt any field out (e.g. ExternalMetadataTask's format field).
        const isRequired = meta.required !== false && meta.type !== 'checkbox' && !meta.hidden;
        return {
          id: key,
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          type: (meta.type || 'string') as any,
          // Append " *" only for required fields whose label doesn't already end with one.
          // (DynamicFormField also strips the trailing star at render time, so the
          // visible asterisk comes from <span className="dynamic-field__required">.)
          label: !isRequired || meta.label.trimEnd().endsWith('*') ? meta.label : `${meta.label} *`,
          helpText: meta.helpText || '',
          required: isRequired,
          // Use only what the backend sent — `meta.placeholder` is a UI hint (HTML
          // placeholder attribute) and must never leak in as the real value.
          // Otherwise a hint like "e.g. maven2" gets serialized back on save.
          initialValue: value ?? '',
          attributes:
            meta.min !== undefined || meta.max !== undefined
              ? { minValue: meta.min, maxValue: meta.max }
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

  return {
    id: rest.type,
    name: rest.name,
    exposed: true,
    concurrentRun: rest.concurrentRun ?? undefined,
    formFields: formFields.length > 0 ? formFields : undefined,
  };
}
