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
import { useCurrentStateAndParams } from '@uirouter/react';

import { UploadRepositoryListPage } from './UploadRepositoryListPage';
import { UploadFormContainer } from './UploadFormContainer';

/**
 * UploadPage is the main entry point for the Upload module.
 * It handles routing between the repository list and the upload form.
 *
 * @returns {JSX.Element} The rendered component.
 */
export function UploadPage(): JSX.Element {
  const { params } = useCurrentStateAndParams();
  const isFormView = Boolean(params?.repoName);

  return isFormView ? <UploadFormContainer /> : <UploadRepositoryListPage />;
}

export default UploadPage;
