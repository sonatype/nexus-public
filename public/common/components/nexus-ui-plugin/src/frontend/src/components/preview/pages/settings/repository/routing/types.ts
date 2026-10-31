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
 * Routing mode - determines whether matching paths are allowed or blocked
 */
export type RoutingMode = 'ALLOW' | 'BLOCK';

/**
 * Routing Rule data model matching the backend API (RoutingRuleXO)
 */
export interface RoutingRule {
  id: string;
  name: string;
  description: string;
  mode: RoutingMode;
  matchers: string[];
  assignedRepositoryCount?: number;
  assignedRepositoryNames?: string[];
}

/**
 * Create/update routing rule form data
 */
export interface RoutingRuleFormData {
  name: string;
  description: string;
  mode: RoutingMode;
  matchers: string[];
}

/**
 * Form validation errors
 */
export interface RoutingRuleFormErrors {
  name?: string;
  description?: string;
  mode?: string;
  matchers?: string;
}

/**
 * Routing rule test request
 */
export interface RoutingRuleTestRequest {
  mode: RoutingMode;
  matchers: string[];
  path: string;
}

/**
 * Repository preview item in the routing rules preview tree
 */
export interface RepositoryPreviewItem {
  repository: string;
  type: string;
  format: string;
  rule: string | null;
  allowed: boolean;
  expanded: boolean;
  expandable: boolean;
  children: RepositoryPreviewItem[] | null;
}

/**
 * Routing rules preview response
 */
export interface RoutingRulesPreview {
  children: RepositoryPreviewItem[];
  expanded: boolean;
  expandable: boolean;
}

/**
 * Preview filter options
 */
export type PreviewFilter = 'all' | 'groups' | 'proxies';

/**
 * Sort direction type
 */
export type SortDirection = 'asc' | 'desc' | null;

/**
 * Sortable fields for routing rules list
 */
export type RoutingRuleSortField = 'name' | 'description' | 'mode' | 'assignedRepositoryCount';

/**
 * Props for RoutingRulesPage component
 */
export interface RoutingRulesPageProps {
  className?: string;
}

/**
 * Props for RoutingRulesList component
 */
export interface RoutingRulesListProps {
  onSelect: (name: string) => void;
  onCreate: () => void;
  onPreview: () => void;
}

/**
 * Props for RoutingRuleForm component
 */
export interface RoutingRuleFormProps {
  rule?: RoutingRule | null;
  isCreate: boolean;
  onSave: (data: RoutingRuleFormData) => Promise<void>;
  onCancel: () => void;
  onDelete?: () => void;
  loading?: boolean;
  error?: string;
}

/**
 * Props for RoutingRuleMatcher component
 */
export interface RoutingRuleMatcherProps {
  matchers: string[];
  onChange: (matchers: string[]) => void;
  error?: string;
  disabled?: boolean;
  testPath?: string;
  testMode?: RoutingMode;
  onTest?: (path: string) => void;
}

/**
 * Props for RoutingRulePreview component
 */
export interface RoutingRulePreviewProps {
  onClose: () => void;
}

/**
 * Initial form data for creating a new routing rule
 */
export const INITIAL_ROUTING_RULE_FORM: RoutingRuleFormData = {
  name: '',
  description: '',
  mode: 'BLOCK',
  matchers: [''],
};

/**
 * Routing mode display labels
 */
export const ROUTING_MODE_LABELS: Record<RoutingMode, string> = {
  ALLOW: 'Allow',
  BLOCK: 'Block',
};

/**
 * Routing mode help text (updated per UX guidance)
 */
export const ROUTING_MODE_HELP: Record<RoutingMode, string> = {
  ALLOW: 'If any matcher matches the request path, the request will be allowed.',
  BLOCK: 'If any matcher matches the request path, the request will be blocked.',
};

/**
 * Check if any matchers have invalid regex patterns
 */
export function hasInvalidMatchers(matchers: string[]): boolean {
  return matchers.some(m => {
    if (!m.trim()) return false;
    try {
      new RegExp(m);
      return false;
    } catch {
      return true;
    }
  });
}

/**
 * Validate routing rule name pattern
 * Must start with a letter, can contain letters, digits, underscores, or hyphens
 */
export const NAME_PATTERN = /^[a-zA-Z][a-zA-Z0-9_-]*$/;
export const NAME_PATTERN_MESSAGE = 'Name must start with a letter and can only contain letters, digits, underscores, and hyphens';

/**
 * Validate that a routing rule form has required fields
 */
export function validateRoutingRuleForm(data: RoutingRuleFormData): RoutingRuleFormErrors {
  const errors: RoutingRuleFormErrors = {};

  if (!data.name?.trim()) {
    errors.name = 'Name is required';
  } else if (!NAME_PATTERN.test(data.name)) {
    errors.name = NAME_PATTERN_MESSAGE;
  }

  if (!data.mode) {
    errors.mode = 'Mode is required';
  }

  const validMatchers = data.matchers.filter(m => m.trim());
  if (validMatchers.length === 0) {
    errors.matchers = 'At least one matcher is required';
  } else {
    // Validate regex patterns
    for (const matcher of validMatchers) {
      try {
        new RegExp(matcher);
      } catch (_e) {
        errors.matchers = `Invalid regex pattern: ${matcher}`;
        break;
      }
    }
  }

  return errors;
}

/**
 * Check if form has validation errors
 */
export function hasFormErrors(errors: RoutingRuleFormErrors): boolean {
  return Object.keys(errors).length > 0;
}

