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

import React, { useMemo } from 'react';
import { Box, Text } from '@radix-ui/themes';
import SwaggerUI from 'swagger-ui-react';

import { sliceSwaggerSpec, type MergedApiEndpoint } from '../utils/mergeSwaggerPermissions';
import { swaggerRequestInterceptor, swaggerResponseInterceptor } from '../swaggerInterceptors';

import 'swagger-ui-react/swagger-ui.css';

export interface TryItTabProps {
  fullSwagger: Record<string, unknown> | null;
  row: MergedApiEndpoint;
  accessDenied: boolean;
}

export function TryItTab({ fullSwagger, row, accessDenied }: TryItTabProps) {
  const methodLower = row.httpMethod.toLowerCase() as 'get' | 'post' | 'put' | 'delete' | 'patch' | 'head' | 'options';

  const slim = useMemo(() => {
    if (!fullSwagger) {
      return null;
    }
    return sliceSwaggerSpec(fullSwagger, row.swaggerPathKey, methodLower);
  }, [fullSwagger, row.swaggerPathKey, methodLower]);

  if (!(fullSwagger && slim)) {
    return (
      <Text size="2" color="gray">
        API documentation is not available. Load swagger.json to use Try It.
      </Text>
    );
  }

  return (
    <Box className="api-try-it-tab">
      {accessDenied && (
        <Box mb="3" p="2" className="api-try-it-tab__banner">
          <Text size="2">
            You may not have permission to execute this endpoint. Requests may return 403.
          </Text>
        </Box>
      )}
      <Box className="api-try-it-tab__swagger" data-testid="api-try-it-swagger">
        <SwaggerUI
          key={`${row.httpMethod}-${row.fullPath}`}
          spec={slim}
          requestInterceptor={swaggerRequestInterceptor}
          responseInterceptor={swaggerResponseInterceptor}
          defaultModelsExpandDepth={-1}
          docExpansion="full"
        />
      </Box>
    </Box>
  );
}
