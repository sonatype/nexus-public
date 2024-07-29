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
  HTTP: {
    MENU: {
      text: 'HTTP',
      description: 'Manage outbound HTTP/HTTPS configuration',
    },
    CONFIGURATION: {
      USER_AGENT: {
        LABEL: 'User-Agent Customization',
        SUB_LABEL: 'Custom fragment to append to “User-Agent” header in HTTP requests'
      },
      TIMEOUT: {
        LABEL: 'Connection/Socket Timeout',
        SUB_LABEL: 'Time (seconds) to wait for activity before stopping and retrying the connection'
      },
      ATTEMPTS: {
        LABEL: 'Connection/Socket Retry Attempts',
        SUB_LABEL: 'Maximum number of retry attempts if the initial connection attempt suffers a timeout'
      },
      PROXY: {
        LABEL: 'Proxy Settings',
        SUB_LABEL: 'Provide an IP address or DNS name (e.g., proxy-host or 192.168.1.101), not a URL',
        HTTP_HOST: 'HTTP Proxy Host',
        HTTP_PORT: 'HTTP Proxy Port',
        HTTP_CHECKBOX: 'Enable HTTP proxy',
        HTTPS_HOST: 'HTTPS Proxy Host',
        HTTPS_PORT: 'HTTPS Proxy Port',
        HTTPS_CHECKBOX: 'Enable HTTPS proxy',
        HTTP_AUTHENTICATION: 'HTTP Authentication',
        HTTPS_AUTHENTICATION: 'HTTPS Authentication',
        HTTP_AUTH_CHECKBOX: 'Enable HTTP Authentication',
        HTTPS_AUTH_CHECKBOX: 'Enable HTTPS Authentication',
        USERNAME: 'Username',
        PASSWORD: 'Password',
        HOST_NAME: 'Windows NTLM Hostname',
        
        DOMAIN: 'Windows NTLM Domain'
      },
      EXCLUDE: {
        LABEL: 'Hosts to exclude from HTTP/HTTPS Proxy',
        SUB_LABEL: 'Accepts Java “http.nonProxyHosts” wildcard patterns (one per line, no “l” hostname delimiters)',
        ADD: 'Add',
        REMOVE: 'Remove',
        ALREADY_ADDED: 'The pattern has already been added to the list'
      },
      READ_ONLY: {
        LABEL: 'HTTP Configuration',
        WARNING: 'You are viewing a read-only version of this page. Some fields will not appear if they have not changed from their default values or if HTTP/HTTPS authentication is not enabled. Contact your Administrator if you require edit permissions.'
      }
    }
  }
};