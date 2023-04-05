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

export default {
  LICENSING: {
    MENU: {
      text: 'Licensing',
      description: 'A valid license is required for PRO features; manage it here'
    },
    SECTIONS: {
      DETAILS: 'Licensing',
      INSTALL: 'Install License',
    },
    DETAILS: {
      COMPANY: {
        LABEL: 'Company',
      },
      NAME: {
        LABEL: 'Name',
      },
      EMAIL: {
        LABEL: 'Email',
      },
      EFFECTIVE_DATE: {
        LABEL: 'Effective Date',
      },
      LICENSE_TYPES: {
        LABEL: 'License Type(s)',
      },
      EXPIRATION_DATE: {
        LABEL: 'Expiration Date',
      },
      NUMBER_OF_USERS: {
        LABEL: 'Number of Licensed Users',
      },
      FINGERPRINT: {
        LABEL: 'Fingerprint',
      },
    },
    INSTALL: {
      LABEL: 'License',
      DESCRIPTION: 'Installing a new license requires restarting the server to take effect',
      MESSAGES: {
        ERROR: (error) => `Unable to update license for the reason identified below. Verify that you selected the correct file. If the problem persists, contact our support team. Reason: ${error}`,
        SUCCESS: 'License installed. Restart is only required if you are enabling new PRO features.',
      },
      BUTTONS: {
        UPLOAD: 'Upload License',
      }
    },
    AGREEMENT: {
      CAPTION: 'Nexus Repository Manager License Agreement',
      BUTTONS: {
        DECLINE: 'I Decline',
        ACCEPT: 'I Accept',
        DOWNLOAD: 'Download a copy of the agreement'
      }
    },
  }
};