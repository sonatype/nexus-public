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

import {
  NxReadOnly
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function GenericRepositoryConfiguration({parentMachine}) {
  const [state] = parentMachine;
  const {name, format, type, url} = state.context.data;

  return (
      <NxReadOnly>
        <NxReadOnly.Label>{EDITOR.NAME_LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{name}</NxReadOnly.Data>

        <NxReadOnly.Label>{EDITOR.FORMAT_LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{format}</NxReadOnly.Data>

        <NxReadOnly.Label>{EDITOR.TYPE_LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{type}</NxReadOnly.Data>

        <NxReadOnly.Label>{EDITOR.URL_LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{url}</NxReadOnly.Data>
      </NxReadOnly>
  );
}