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
 * User token settings data model
 */
export interface UserTokenSettings {
  enabled: boolean;
  protectContent: boolean;
  expirationEnabled: boolean;
  expirationDays: number;
}

/**
 * Default user token settings
 */
export const DEFAULT_USER_TOKEN_SETTINGS: UserTokenSettings = {
  enabled: false,
  protectContent: false,
  expirationEnabled: false,
  expirationDays: 30,
};

/**
 * Confirmation string required for reset all tokens
 * Must be typed exactly to confirm the reset action
 */
export const RESET_CONFIRMATION_STRING = 'Reset all tokens';
