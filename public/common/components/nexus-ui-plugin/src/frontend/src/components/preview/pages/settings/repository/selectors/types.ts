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
 * Content selector data model
 */
export interface ContentSelector {
  name: string;
  type: string;
  description: string;
  expression: string;
}

/**
 * Repository option for preview
 */
export interface RepositoryOption {
  id: string;
  name: string;
}

/**
 * Content selector form data for create/edit
 */
export interface ContentSelectorFormData {
  name: string;
  type: string;
  description: string;
  expression: string;
}

/**
 * Form validation errors
 */
export interface ContentSelectorFormErrors {
  name?: string;
  description?: string;
  expression?: string;
}

/**
 * API URLs
 */
export const CONTENT_SELECTOR_API = {
  BASE_URL: 'service/rest/v1/security/content-selectors',
  PREVIEW_URL: 'service/rest/internal/ui/content-selectors/preview',
  // Must include withAll=true to get "All Repositories" option, withFormats=true for format info
  REPOSITORIES_URL: 'service/rest/internal/ui/repositories?withAll=true&withFormats=true',
};

/**
 * Get URL for a single content selector
 */
export const getContentSelectorUrl = (name: string): string =>
  `${CONTENT_SELECTOR_API.BASE_URL}/${encodeURIComponent(name)}`;

/**
 * Content selector type - only CSEL is supported
 */
export const CONTENT_SELECTOR_TYPE = 'csel';

/**
 * Empty content selector for create
 */
export const EMPTY_CONTENT_SELECTOR: ContentSelectorFormData = {
  name: '',
  type: CONTENT_SELECTOR_TYPE,
  description: '',
  expression: '',
};

/**
 * Maximum length for name field
 */
export const NAME_MAX_LENGTH = 512;

/**
 * CSEL Expression Language examples and help
 */
export const CSEL_HELP = {
  TITLE: 'Content Selector Expression Language (CSEL)',
  DESCRIPTION:
    'CSEL allows you to define expressions that select specific content from your repositories.',
  EXAMPLES: [
    {
      label: 'Select "raw" format content',
      expression: 'format == "raw"',
    },
    {
      label: 'Select "maven2" content along a path that starts with "/org/"',
      expression: 'format == "maven2" and path =^ "/org"',
    },
    {
      label: 'Select content by repository name',
      expression: 'coordinate.repositoryName == "maven-central"',
    },
    {
      label: 'Select content by path pattern',
      expression: 'path =~ ".*\\\\.jar$"',
    },
  ],
  OPERATORS: [
    { operator: '==', description: 'Equals' },
    { operator: '!=', description: 'Not equals' },
    { operator: '=~', description: 'Regular expression match' },
    { operator: '=^', description: 'Starts with' },
    { operator: 'and', description: 'Logical AND' },
    { operator: 'or', description: 'Logical OR' },
    { operator: 'not', description: 'Logical NOT' },
  ],
  ATTRIBUTES: [
    { attribute: 'format', description: 'Repository format (e.g., maven2, npm, docker)' },
    { attribute: 'path', description: 'Asset path within the repository' },
    { attribute: 'coordinate.repositoryName', description: 'Name of the repository' },
    { attribute: 'coordinate.groupId', description: 'Maven group ID (maven2 only)' },
    { attribute: 'coordinate.artifactId', description: 'Maven artifact ID (maven2 only)' },
    { attribute: 'coordinate.version', description: 'Component version' },
  ],
};

/**
 * Validate content selector name
 */
export const validateName = (name: string): string | undefined => {
  if (!name.trim()) {
    return 'Name is required';
  }
  if (name.length > NAME_MAX_LENGTH) {
    return `Name must be ${NAME_MAX_LENGTH} characters or less`;
  }
  if (!/^[a-zA-Z0-9\-_.]+$/.test(name)) {
    return 'Name must contain only letters, digits, underscores, hyphens, and periods';
  }
  return undefined;
};

/**
 * Validate content selector expression
 */
export const validateExpression = (expression: string): string | undefined => {
  if (!expression.trim()) {
    return 'Expression is required';
  }
  return undefined;
};

