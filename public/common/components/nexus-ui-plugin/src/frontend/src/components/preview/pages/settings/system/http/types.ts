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
 * HTTP settings configuration data model
 */
export interface HttpConfiguration {
  userAgentSuffix: string;
  timeout: number | null;
  retries: number | null;
  httpEnabled: boolean;
  httpHost: string;
  httpPort: number | null;
  httpAuthEnabled: boolean;
  httpAuthUsername: string;
  httpAuthPassword: string;
  httpAuthNtlmHost: string;
  httpAuthNtlmDomain: string;
  httpsEnabled: boolean;
  httpsHost: string;
  httpsPort: number | null;
  httpsAuthEnabled: boolean;
  httpsAuthUsername: string;
  httpsAuthPassword: string;
  httpsAuthNtlmHost: string;
  httpsAuthNtlmDomain: string;
  nonProxyHosts: string[];
}

/**
 * Props for HttpPage component
 */
export interface HttpPageProps {
  className?: string;
}

/**
 * Default HTTP configuration
 */
export const DEFAULT_HTTP_CONFIGURATION: HttpConfiguration = {
  userAgentSuffix: '',
  timeout: null,
  retries: null,
  httpEnabled: false,
  httpHost: '',
  httpPort: null,
  httpAuthEnabled: false,
  httpAuthUsername: '',
  httpAuthPassword: '',
  httpAuthNtlmHost: '',
  httpAuthNtlmDomain: '',
  httpsEnabled: false,
  httpsHost: '',
  httpsPort: null,
  httpsAuthEnabled: false,
  httpsAuthUsername: '',
  httpsAuthPassword: '',
  httpsAuthNtlmHost: '',
  httpsAuthNtlmDomain: '',
  nonProxyHosts: [],
};

/**
 * Validation errors for HTTP configuration form
 */
export interface HttpValidationErrors {
  timeout?: string;
  retries?: string;
  httpHost?: string;
  httpPort?: string;
  httpAuthUsername?: string;
  httpsHost?: string;
  httpsPort?: string;
  httpsAuthUsername?: string;
}


