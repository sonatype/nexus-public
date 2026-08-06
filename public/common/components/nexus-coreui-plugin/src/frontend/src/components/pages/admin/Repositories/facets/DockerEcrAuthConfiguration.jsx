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

import {NxH2, NxFormGroup, NxTextInput, NxWarningAlert} from '@sonatype/react-shared-components';
import {FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../../constants/UIStrings';

const {ECR} = UIStrings.REPOSITORIES.EDITOR.DOCKER;

// Matches an AWS ECR remote URL: <12-digit-accountId>.dkr.ecr.<region>.amazonaws.com
// (including GovCloud / ISO / China partitions). Kept in sync with the backend EcrUrlParser
// detection and with the gate in GenericHttpAuthConfiguration.
const ECR_URL_PATTERN = /^https?:\/\/\d{12}\.dkr\.ecr\.[a-z0-9-]+\.amazonaws\.com(\.cn)?\/?/i;

/**
 * Optional AWS ECR session-token field for Docker proxy repositories.
 *
 * Rendered inside the HTTP authentication section, directly below the Password field, so all
 * three AWS credential parts live together: Access Key ID (username), Secret Access Key
 * (password), and this optional session token.
 *
 * Only shown when every condition holds: the repository is a Docker proxy, the remote URL is
 * an AWS ECR registry, and the operator has selected the "username" authentication type.
 * Leaving the token blank uses long-lived (AKIA) credentials exactly as before.
 */
export default function DockerEcrAuthConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  const {data} = currentParent.context;
  const {format, type, proxy, httpClient} = data;
  const remoteUrl = proxy?.remoteUrl;
  const authType = httpClient?.authentication?.type;

  const isEcrDockerProxy =
    format === 'docker' &&
    type === 'proxy' &&
    authType === 'username' &&
    ECR_URL_PATTERN.test(remoteUrl || '');

  if (!isEcrDockerProxy) {
    return null;
  }

  const sessionToken = data.dockerProxy?.ecrAuth?.sessionToken;

  return (
    <>
      <NxH2>{ECR.CAPTION}</NxH2>
      <NxFormGroup
        label={ECR.SESSION_TOKEN.LABEL}
        sublabel={ECR.SESSION_TOKEN.SUBLABEL}
        className="nxrm-form-group-ecr-session-token"
      >
        <NxTextInput
          type="password"
          autoComplete="new-password"
          {...FormUtils.fieldProps('dockerProxy.ecrAuth.sessionToken', currentParent)}
          onChange={FormUtils.handleUpdate('dockerProxy.ecrAuth.sessionToken', sendParent)}
          placeholder={ECR.SESSION_TOKEN.PLACEHOLDER}
        />
      </NxFormGroup>
      {Boolean(sessionToken) && <NxWarningAlert>{ECR.EXPIRY_WARNING}</NxWarningAlert>}
    </>
  );
}
