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
  UPGRADE_MODAL: {
    HEADER: {
      TITLE: 'New In Nexus Repository 3.71.0',
    },
    ABOUT: {
      TITLE: 'Upgrade Without Downtime',
      DESCRIPTION: 'Upgrade all of your Sonatype Nexus Repository Pro instances in your highly available multi-node ' +
        'cluster without interrupting service availability'
    },
    BENEFITS: {
      TITLE: 'Benefits:',
      NOTES: <>See the <NxTextLink external href="https://links.sonatype.com/products/nexus/releasenotes">Nexus Repository 3.71.0 Release notes</NxTextLink> for full details.</>,
      LIST: {
        ITEM1: 'Fast access to bug fixes and new features when you upgrade to the newest Nexus Repository version',
        ITEM2: 'No downtime means your team can keep working while an upgrade takes place',
        ITEM3: 'Monitor upgrade process right from the user interface'
      }
    }
  }
};
