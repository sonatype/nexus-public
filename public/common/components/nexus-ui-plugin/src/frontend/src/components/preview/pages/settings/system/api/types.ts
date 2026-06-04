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
 * Props for ApiPage component
 */
export interface ApiPageProps {
  className?: string;
}

/**
 * Swagger UI configuration
 */
export interface SwaggerConfig {
  url: string;
  requestInterceptor: (request: SwaggerRequest) => SwaggerRequest;
  responseInterceptor: (response: SwaggerResponse) => SwaggerResponse;
  defaultModelsExpandDepth: number;
}

/**
 * Swagger request object passed to requestInterceptor
 */
export interface SwaggerRequest {
  headers: Record<string, string>;
  url: string;
  method: string;
  body?: unknown;
}

/**
 * Swagger response object passed to responseInterceptor
 */
export interface SwaggerResponse {
  data: unknown;
  body: {
    tags?: SwaggerTag[];
    [key: string]: unknown;
  };
  text: string;
}

/**
 * OpenAPI tag definition
 */
export interface SwaggerTag {
  name: string;
  description?: string;
}

/**
 * API Page loading states
 */
export type ApiPageState = 'loading' | 'ready' | 'error';

/** One entry in {@code permissions} array from GET /internal/ui/api/permissions */
export interface ApiPermissionRequirementDto {
  permission: string;
  logical: 'AND' | 'OR';
}

/** One endpoint row from GET /internal/ui/api/permissions */
export interface ApiEndpointPermissionDto {
  httpMethod: string;
  pathPattern: string;
  permissions: ApiPermissionRequirementDto[];
  description: string | null;
  tag: string | null;
  authenticated: boolean;
}

/** Response body for GET /internal/ui/api/permissions */
export interface ApiPermissionsResponseDto {
  endpoints: ApiEndpointPermissionDto[];
  generatedAt: string;
  totalEndpoints: number;
  unmappedEndpoints: number;
  error?: string | null;
}

/** POST /internal/ui/security/access-check */
export interface ApiAccessCheckRequestDto {
  userId?: string | null;
  roleId?: string | null;
  endpoint: string;
  method: string;
}

/** Response from POST /internal/ui/security/access-check */
export interface ApiAccessCheckResponseDto {
  hasAccess: boolean;
  requiredPermission?: string | null;
}
