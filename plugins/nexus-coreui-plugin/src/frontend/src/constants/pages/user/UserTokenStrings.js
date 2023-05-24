/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
export default {
  USER_TOKEN: {
    MENU: {
      text: 'User Token',
      description: 'Access Sonatype Nexus Repository without the use of passwords'
    },
    LABELS: {
      ACCESS_NOTE: 'A new user token will be created the first time it is accessed.',
      RESET_NOTE: 'Resetting your user token will invalidate the current token and force a new token to be created the next time it is accessed.',
      USER_TOKEN_NOTE: 'User tokens are a combination of a name and password codes.',
      KEEP_SECRET_NOTE: 'Keep these codes secret.',
      USER_TOKEN_NAME_CODE: 'Your user token name code is',
      USER_TOKEN_PASS_CODE: 'Your user token pass code is',
      MAVEN_USAGE: 'Use the following in your Maven settings.xml',
      BASE64_USER_TOKEN: 'Use the following for a base64 representation of "user:password"',
      AUTO_HIDE: 'This window will automatically close after one minute.',
    },
    MESSAGES: {
      ACCESS_AUTHENTICATION: 'Accessing user tokens requires validation of your credentials',
      RESET_AUTHENTICATION: 'Resetting user tokens requires validation of your credentials',
    },
    BUTTONS: {
      ACCESS: 'Access user token',
      RESET: 'Reset user token',
      CLOSE: 'Close'
    }
  }
};