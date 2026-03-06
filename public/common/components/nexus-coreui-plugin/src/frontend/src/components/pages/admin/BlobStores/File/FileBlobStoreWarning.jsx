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
import {useActor} from '@xstate/react';
import {NxWarningAlert} from '@sonatype/react-shared-components';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../../constants/UIStrings';

const {BLOB_STORES: {FORM: {FILE_WARNING}}} = UIStrings;

function isRelativePath(path) {
  if (!path) {
    return false;
  }
  return !path.startsWith('/') && !path.match(/^[A-Za-z]:\\/);
}

function isUnderWorkDirectory(path, workDirectory) {
  if (!path || !workDirectory) {
    return false;
  }
  const normalizedPath = path.replace(/\\/g, '/');
  let normalizedWorkDir = workDirectory.replace(/\\/g, '/');

  // Ensure work directory ends with separator to avoid false positives
  // e.g., "/nexus-data/info" vs "/nexus-data/information"
  if (!normalizedWorkDir.endsWith('/')) {
    normalizedWorkDir += '/';
  }

  return normalizedPath.startsWith(normalizedWorkDir);
}

export default function FileBlobStoreWarning({service}) {
  const [state] = useActor(service);
  const {data} = state.context;
  const path = data?.path;

  const clusteringState = ExtJS.state()?.getValue('clustering');
  const isClustered = clusteringState?.isClustered || false;
  const workDirectory = clusteringState?.workDirectory || '';

  if (!isClustered) {
    return null;
  }

  const showWarning = isRelativePath(path) || isUnderWorkDirectory(path, workDirectory);

  if (!showWarning) {
    return null;
  }

  return (
    <NxWarningAlert>
      <strong>{FILE_WARNING.TITLE}</strong>
      <br />
      {FILE_WARNING.MESSAGE}
    </NxWarningAlert>
  );
}
