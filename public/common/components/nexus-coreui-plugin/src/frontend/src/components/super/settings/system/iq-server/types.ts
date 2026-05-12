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
 * IQ Server configuration data model
 */
export interface IqServerConfiguration {
  enabled: boolean;
  url: string;
  authenticationType: 'USER' | 'PKI' | '';
  username: string;
  password: string;
  useTrustStoreForUrl: boolean;
  timeoutSeconds: number | null;
  properties: string;
  showLink: boolean;
}

/**
 * IQ Server connection verification result
 */
export interface IqVerificationResult {
  success: boolean;
  reason?: string;
}

/**
 * IQ Server capabilities data model
 */
export interface IqCapabilities {
  hasFirewall: boolean;
  hasLifecycle: boolean;
  connected: boolean;
  url: string | null;
}

/**
 * Default IQ Server capabilities
 */
export const DEFAULT_IQ_CAPABILITIES: IqCapabilities = {
  hasFirewall: false,
  hasLifecycle: false,
  connected: false,
  url: null,
};

/**
 * Props for IqServerPage component
 */
export interface IqServerPageProps {
  className?: string;
}

/**
 * Default IQ Server configuration
 */
export const DEFAULT_IQ_CONFIGURATION: IqServerConfiguration = {
  enabled: false,
  url: '',
  authenticationType: '',
  username: '',
  password: '',
  useTrustStoreForUrl: false,
  timeoutSeconds: null,
  properties: '',
  showLink: true,
};

/**
 * Validation errors for IQ Server configuration form
 */
export interface IqValidationErrors {
  url?: string;
  authenticationType?: string;
  username?: string;
  password?: string;
  timeoutSeconds?: string;
}

/**
 * Password placeholder constant
 */
export const PASSWORD_PLACEHOLDER = '#~NXRM~PLACEHOLDER~PASSWORD~#';


