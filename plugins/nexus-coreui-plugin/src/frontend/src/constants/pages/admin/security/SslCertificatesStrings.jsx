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
import React from 'react';
import {NxTextLink} from '@sonatype/react-shared-components';

export default {
  SSL_CERTIFICATES: {
    MENU: {
      text: 'SSL Certificates',
      description: 'Manage Trusted SSL Certificates for use with the Nexus truststore'
    },
    LIST: {
      CREATE_BUTTON: 'Add Certificate',
      EMPTY_LIST: 'There are no SSL Certificates available',
      COLUMNS: {
        NAME: 'Name',
        ISSUED_TO: 'Issued to',
        ISSUED_BY: 'Issued by',
        FINGERPRINT: 'Fingerprint',
      },
      HELP: {
        TITLE: 'What is SSL?',
        TEXT: <>
          Using Secure Socket Layer (SSL) communication with the repository manager is an important security feature
          and a recommended best practice. Secure communication can be inbound or outbound. Outbound client
          communication may include integration with: proxy repository, email servers, LDAPS servers. Inbound client
          communication includes: web browser HTTPS access, tool access to repository content, usage of REST APIs.
          For more information check{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/ssl-certificate">
            the documentation
          </NxTextLink>.
        </>,
      },
    },
    ADD_FORM: {
      CAPTION: 'Add SSL Certificate',
      LOAD_BUTTON: 'Load Certificate',
      PEM: {
        RADIO_DESCRIPTION: 'Paste PEM',
        LABEL: 'Paste Certificate as PEM',
        PLACEHOLDER: 'Entry'
      },
      SERVER: {
        LABEL: 'Please enter a hostname, hostname:port or a URL to fetch a SSL certificate from',
        RADIO_DESCRIPTION: 'Load from server'
      },
      MODAL: {
        HEADER: 'Certificate Already Exists',
        CONTENT: 'This Certificate already exists and cannot be added again. Would you like to view the existing Certificate?',
        VIEW_BUTTON: 'View Certificate'
      }
    },
    FORM: {
      DETAILS_TITLE: (name) => `Certificate ${name}`,
      DETAILS_DESCRIPTION: 'Summary',
      WARNING: 'This certificate was retrieved over an untrusted connection. Always verify the details before adding it.',
      SECTIONS: {
        SETUP: 'Load SSL Certificates',
        SUBJECT: 'Subject',
        ISSUER: 'Issuer',
        CERTIFICATE: 'Certificate',
      },
      COMMON_NAME: {
        LABEL: 'Common Name',
      },
      ORGANIZATION: {
        LABEL: 'Organization',
      },
      UNIT: {
        LABEL: 'Unit',
      },
      ISSUED_ON: {
        LABEL: 'Issued On',
      },
      VALID_UNTIL: {
        LABEL: 'Valid Until',
      },
      FINGERPRINT: {
        LABEL: 'Fingerprint',
      },
      BUTTONS: {
        ADD: 'Add Certificate to Truststore',
        DELETE: 'Delete Certificate',
        LOAD: 'Load Certificate',
      },
    },
    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete SSL Certificate',
        MESSAGE: (name) => `Delete the SSL Certificate named ${name}?`,
        YES: 'Delete',
        NO: 'Cancel'
      },
      DELETE_SUCCESS: (name) => `SSL Certificate deleted: ${name}`,
    },
  }
};