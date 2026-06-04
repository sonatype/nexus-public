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
 * Email server configuration data model
 */
export interface EmailConfiguration {
  enabled: boolean;
  host: string;
  port: number;
  username: string;
  password: string;
  fromAddress: string;
  subjectPrefix: string;
  startTlsEnabled: boolean;
  startTlsRequired: boolean;
  sslOnConnectEnabled: boolean;
  sslCheckServerIdentityEnabled: boolean;
  nexusTrustStoreEnabled: boolean;
}

/**
 * Email verification result
 */
export interface EmailVerificationResult {
  success: boolean;
  reason?: string;
}

/**
 * Props for EmailPage component
 */
export interface EmailPageProps {
  className?: string;
}

/**
 * Default email configuration
 */
export const DEFAULT_EMAIL_CONFIGURATION: EmailConfiguration = {
  enabled: false,
  host: '',
  port: 25,
  username: '',
  password: '',
  fromAddress: '',
  subjectPrefix: '',
  startTlsEnabled: false,
  startTlsRequired: false,
  sslOnConnectEnabled: false,
  sslCheckServerIdentityEnabled: false,
  nexusTrustStoreEnabled: false,
};

/**
 * Validation errors for email configuration form
 */
export interface EmailValidationErrors {
  host?: string;
  port?: string;
  fromAddress?: string;
}


