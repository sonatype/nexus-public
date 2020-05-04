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
import {useMachine} from '@xstate/react';
import UserAccountSettings from './UserAccountSettings';
import PasswordChangeForm from "./PasswordChangeForm";
import {ContentBody} from 'nexus-ui-plugin';

import './UserAccount.scss';
import UserAccountMachine from "./UserAccountMachine";

export default function UserAccount() {
  const [current,, userAccountService] = useMachine(UserAccountMachine, {devTools: true});

  return <ContentBody className='nxrm-user-account'>
    <UserAccountSettings service={userAccountService}/>

    { !current.context.data.external &&  <PasswordChangeForm userId={current.context.data.userId}/> }
  </ContentBody>;
}
