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
 * Crowd settings data
 */
export interface CrowdConfig {
  enabled: boolean;
  realmActive: boolean;
  url: string;
  useTrustStoreForUrl: boolean;
  applicationName: string;
  applicationPassword: string;
  timeout?: number;
}

/**
 * Default Crowd configuration values
 */
export const DEFAULT_CROWD_CONFIG: CrowdConfig = {
  enabled: false,
  realmActive: false,
  url: '',
  useTrustStoreForUrl: false,
  applicationName: '',
  applicationPassword: '',
  timeout: undefined,
};

/**
 * Page props
 */
export interface CrowdPageProps {
  className?: string;
}


