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
  PROPRIETARY_REPOSITORIES: {
    MENU: {
      text: 'Proprietary Repositories',
      description: 'Manage the set of hosted repositories that contain proprietary components'
    },
    CONFIGURATION: {
      LABEL: 'Proprietary Repositories Configuration',
      AVAILABLE_TITLE: 'Generic Hosted Repositories',
      SELECTED_TITLE: 'Proprietary Hosted Repositories',
      EMPTY_LIST: 'There are no configured proprietary hosted repositories for which you have view permissions.',
    },
    HELP_TEXT: <>
      To help prevent dependency confusion attacks, identify your hosted repositories that contain
      proprietary components. Refer to the{' '}
      <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/proprietary-repositories">
        documentation
      </NxTextLink>
      {' '}for details on setting up appropriate IQ policies to quarantine public components with the same names
      as your proprietary components.
    </>,
  }
};