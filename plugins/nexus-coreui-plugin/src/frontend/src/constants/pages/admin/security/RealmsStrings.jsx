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
import React from "react";
import {NxTextLink} from '@sonatype/react-shared-components';

export default {
  REALMS: {
    MENU: {
      text: 'Realms',
      description: 'Manage the active security realms and their order'
    },
    CONFIGURATION: {
      LABEL: 'Realms Configuration',
      SUB_LABEL: 'Active Realms',
      AVAILABLE_TITLE: 'Available',
      SELECTED_TITLE: 'Active',
      EMPTY_LIST: 'There are no configured realms for which you have view permissions.',
    },
    MESSAGES: {
      NO_REALMS_CONFIGURED: 'At least one realm must be selected and should include a user that is able to update the list of realms'
    },
    LOCAL_REALM_REMOVAL_MODAL: {
      HEADER: 'Confirm Removal of Local Realms',
      WARNING: <>
          Warning! Removing local realms will prevent local admin access.
          This can make system recovery more difficult should there be issues with
          other authentication realms. For more information, see the{' '}
          <NxTextLink 
            external 
            href="http://links.sonatype.com/products/nxrm3/docs/realms"
          >
            Realms help documentation
          </NxTextLink>
        </>,
      CONFIRM_BUTTON: 'Confirm',
      ACKNOWLEDGEMENT: {
        LABEL: 'Acknowledgement',
        STRING: 'I acknowledge',
        PLACEHOLDER: 'Provide acknowledgement',
        VALIDATION_ERROR: 'Invalid acknowledgement',
        get SUBLABEL () {return `Type "${this.STRING}" in order to proceed with this action`}
      }
    }
  }
}
