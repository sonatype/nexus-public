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
  EMAIL_SERVER: {
    MENU: {
      text: 'Email Server',
      description: 'Manage email server configuration'
    },
    FORM: {
      SECTIONS: {
        SETUP: 'Email Server Configuration',
      },
      ENABLED: {
        LABEL: 'Enable Email Server',
        SUB_LABEL: 'Enabled',
      },
      HOST: {
        LABEL: 'Host',
      },
      PORT: {
        LABEL: 'Port',
      },
      USERNAME: {
        LABEL: 'Username',
      },
      PASSWORD: {
        LABEL: 'Password',
      },
      FROM_ADDRESS: {
        LABEL: 'From Address',
      },
      SUBJECT_PREFIX: {
        LABEL: 'Subject Prefix',
      },
      SSL_TLS_OPTIONS: {
        LABEL: 'SSL/TLS Options',
        OPTIONS: {
          ENABLE_STARTTLS: 'Enable STARTTLS support for insecure connections',
          REQUIRE_STARTTLS: 'Require STARTTLS support',
          ENABLE_SSL_TLS: 'Enable SSL/TLS encryption upon connection',
          IDENTITY_CHECK: 'Enable server identity check',
        },
      },
    },
    VERIFY: {
      LABEL: 'Verify Email Server',
      SUB_LABEL: 'Where do you want to send the test email?',
      TEST: 'Test',
      SUCCESS: 'Email server verification email sent successfully',
      ERROR: 'Email server verification email failed'
    },
    READ_ONLY: {
      ENABLE: {
        ENABLE_STARTTLS: 'STARTTLS support enabled for insecure connections',
        REQUIRE_STARTTLS: 'STARTTLS support required',
        ENABLE_SSL_TLS: 'SSL/TLS encryption enabled upon connection',
        IDENTITY_CHECK: 'Server identity check enabled',
      },
      NOT_ENABLE: {
        ENABLE_STARTTLS: 'STARTTLS support not enabled for insecure connections',
        REQUIRE_STARTTLS: 'STARTTLS support not required',
        ENABLE_SSL_TLS: 'SSL/TLS encryption not enabled upon connection',
        IDENTITY_CHECK: 'Server identity check not enabled',
      }
    }
  }
};