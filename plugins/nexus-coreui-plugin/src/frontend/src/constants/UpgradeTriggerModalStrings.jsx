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
  TRIGGER_MODAL: {
    TITLE: 'Proceed with upgrade?',
    DESCRIPTION: <>After beginning the database schema migration process, you will not be able to start Nexus Repository nodes 
    until the process is complete. <strong>We highly recommend backing up your database before proceeding.</strong></>,
    CHECKBOX_LABEL: 'Checking this box indicates that you have backed up your database or are aware of the risks associated ' +
    'with proceeding without doing so.',
    CANCEL: 'Cancel',
    CONTINUE: 'Continue'
  }
};
