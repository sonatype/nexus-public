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
  REPLICATION: {
    MENU: {
      text: 'Replication',
      description: 'Hosted repositories can be replicated for faster artifact availability across various zones or regions'
    },
    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete Replication Connection',
        MESSAGE: (name) => `Delete the replication connection named ${name}?`,
        YES: 'Delete',
        NO: 'Cancel'
      }
    },
    LIST: {
      HEADING: 'Current Connections',
      CREATE_BUTTON: 'New Replication',
      NAME_LABEL: 'Name',
      SOURCE_REPO_LABEL: 'Source Repo',
      TARGET_INSTANCE_LABEL: 'Target Instance',
      DESTINATION_REPO_LABEL: 'Destination Repo',
      EMPTY_LIST: 'There are no replications created yet',
      HELP_TITLE: 'What is replication?',
      HELP_TEXT: <>
        Repository Replication allows for you to copy full or partial content from one hosted source repository
        to a target repository across a succesful connection. For more information,{' '}
        <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/replication">
          check the documentation
        </NxTextLink>.
      </>,
    },
    FORM: {
      CREATE_TITLE: 'New Replication',
      EDIT_TITLE: 'Edit Replication',
      DELETE_BUTTON_LABEL: 'Delete',
      TITLE: 'Replication Information',
      TARGET_INFORMATION: 'Target Information',
      NAME_LABEL: 'Name',
      NAME_DESCRIPTION: 'Assign a unique name for this replication.',
      SOURCE_CONTENT_OPTIONS: 'Source Content Options',
      SOURCE_CONTENT_OPTION_ALL: 'Replicate all content files in the Source Repository',
      SOURCE_CONTENT_OPTION_REGEX: 'Replicate specific content files in the Source Repository',
      CONTENT_FILTER: 'Content Filter',
      CONTENT_FILTER_DESCRIPTION: 'Include assets that have a name matching the following regular expression patterns.',
      INCLUDE_EXISTING_CONTENT: 'Include existing content files from the Source Repository in replication',
      SOURCE_REPO_LABEL: 'Source Repository Name',
      SOURCE_REPO_DESCRIPTION: 'Select the repository from which content will be replicated.',
      INSTANCE_URL_LABEL: 'Instance URL',
      INSTANCE_URL_DESCRIPTION: 'Enter the target instance URL to which content will be replicated.',
      USER_AUTH_LABEL: 'User Authentication',
      USER_AUTH_TEXT: {
        __html: 'Enter the login credentials for your target instance. To learn more, visit our <a href="http://links.sonatype.com/products/nxrm3/docs/replication"> help documentation</a>.'
      },
      USER_LABEL: 'Username',
      PASSWORD_LABEL: 'Password',
      USE_TRUST_STORE: 'Use certificate connected to the Nexus Repository Truststore',
      VIEW_CERTIFICATE: (url) => `View`,
      CERTIFICATE_DETAILS: 'Certificate Details',
      TEST_BUTTON: 'Test Repository Connection',
      TARGET_REPO_LABEL: 'Target Repository Name',
      CREATE_BUTTON: 'Create Replication Connection',

      TEST_STATUS_MESSAGE: {
        200: 'Test was successful. A list of repositories has been retrieved, please select one as a target below.',
        400: {
          titleMessage: 'HTTP status code: 400.',
          error: message => `A problem was detected when testing the connection to the target instance. Message from the target instance was "${message}". ` +
              'Please resolve the error and try again.'
        },
        401: {
          titleMessage: 'HTTP status code: 401.',
          error: () => 'The tested target Instance URL and User Authentication credentials are not valid and cannot retrieve a list of ' +
              'available repositories. Please make sure the User Authentication credentials are correct and try again.'
        },
        403: {
          titleMessage: 'HTTP status code: 403.',
          error: () => 'The tested target Instance URL and User Authentication credentials are not acknowledged and cannot retrieve a list ' +
              'of available repositories. Please make sure the target Instance URL and User Authentication credentials are correct and try again.'
        },
        404: {
          titleMessage: 'HTTP status code: 404.',
          error: () => 'The test connection cannot retrieve a list of available repositories because Replication is not yet supported on ' +
              'this target instance URL. Please enter a new target Instance URL and User Authentication credentials and try again.'
        },
        500: {
          titleMessage: 'HTTP status code: 500.',
          error: () => 'The test connection cannot retrieve a list of available repositories because of a target server error. Please try again.'
        },
        501: {
          titleMessage: 'HTTP status code: 501.',
          error: () => 'The HTTPS link to the target Nexus Repository instance is using a certificate that is not in the Nexus Repository Truststore. ' +
              'Ensure you are using a certificate connected to the Nexus Repository Truststore and try again.'
        },
        502: {
          titleMessage: 'HTTP status code: 502.',
          error: () => 'The test connection cannot retrieve a list of available repositories because the connection cannot be made. ' +
              'Please make sure the target Instance URL is correct and try again.'
        },
        UNKNOWN: {
          titleMessage: 'An unknown error occurred while testing the connection.',
          error: 'The test connection cannot retrieve a list of available repositories because of a target server error. Please try again.'
        }
      },
      CONNECTION_STATUS_MESSAGE: {
        400: {
          titleMessage: 'HTTP status code: 400.',
          error: remoteMessage => `Replication could not be enabled on the target instance. Message from the target instance was "${remoteMessage}". ` +
              'Please resolve the error and try again.'
        },
        401: {
          titleMessage: 'HTTP status code: 401.',
          error: remoteMessage => 'The target Instance URL and User Authentication credentials are not valid and cannot enable replication on the target. ' +
              'Please make sure the User Authentication credentials are correct and try again.'
        },
        403: {
          titleMessage: 'HTTP status code: 403.',
          error: remoteMessage => 'The target Instance URL and User Authentication credentials are not acknowledged and cannot enable replication on ' +
              'the target. Please make sure the target Instance URL and User Authentication credentials are correct and try again.'
        },
        404: {
          titleMessage: 'HTTP status code: 404.',
          error: remoteMessage => 'The target Instance URL cannot be used to enable replication because Replication is not yet supported on ' +
              'this target instance URL. Please enter a new target Instance URL and User Authentication credentials and try again.'
        },
        500: {
          titleMessage: 'HTTP status code: 500.',
          error: remoteMessage => 'The target Instance URL cannot be used to enable replication because of a server error. Please try again.'
        },
        502: {
          titleMessage: 'HTTP status code: 502.',
          error: remoteMessage => 'The target Instance URL cannot be used to enable replication because the connection could not be made.  ' +
              'Please make sure the target Instance URL is correct and try again.'
        },
        UNKNOWN: {
          titleMessage: 'An unknown error occurred while enabling replication on target server.',
          error: remoteMessage => `The target Instance URL cannot be used to enable replication because of an unknown error. Message from the target instance was "${remoteMessage}". Please try again.`
        }
      }
    }
  }
};