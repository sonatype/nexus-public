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

import type { SwaggerRequest, SwaggerResponse } from './types';

export function getCsrfToken(): string {
  const match = document.cookie.match('(^|; )NX-ANTI-CSRF-TOKEN=([^;]*)');
  return match ? match[2] : '';
}

export function swaggerRequestInterceptor(request: SwaggerRequest): SwaggerRequest {
  const csrfToken = getCsrfToken();
  if (csrfToken) {
    request.headers['NX-ANTI-CSRF-TOKEN'] = csrfToken;
  }
  request.headers['X-Nexus-UI'] = 'true';
  return request;
}

export function swaggerResponseInterceptor(response: SwaggerResponse): SwaggerResponse {
  let data = response.data;

  if (typeof response.data === 'string') {
    try {
      data = JSON.parse(response.data);
    } catch (e) {
      console.error('Failed to parse Swagger response:', e);
    }
  }

  if (data && typeof data === 'object' && 'tags' in data) {
    const dataObj = data as { tags?: Array<{ name: string }> };
    if (Array.isArray(dataObj.tags)) {
      const sortedTags = [...dataObj.tags].sort((a, b) => a.name.localeCompare(b.name));
      response.body.tags = sortedTags;
      response.data = { ...dataObj, tags: sortedTags };

      try {
        const text = JSON.parse(response.text);
        response.text = JSON.stringify({ ...text, tags: sortedTags });
      } catch {
        // ignore
      }
    }
  }

  return response;
}
