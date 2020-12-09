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
import classNames from 'classnames';
import {useService} from '@xstate/react';
import {NxErrorAlert, NxLoadWrapper} from "@sonatype/react-shared-components";

/**
 * @since 3.next
 */
export default function Form({
                               children,
                               className,
                               onKeyPress,
                               onSubmit,
                               isLoading,
                               loadError,
                               alert,
                               ...attrs}) {
  const classes = classNames('nxrm-form', className);

  function handleSubmit(event) {
    onSubmit(event);
  }

  function handleKeyPress(event) {
    if (event.key === 'Enter') {
      handleSubmit(event);
    }
    else {
      onKeyPress(event);
    }
  }

  return <div className={classes} onKeyPress={handleKeyPress} {...attrs}>
    <NxLoadWrapper loading={isLoading} error={loadError && String(loadError)}>
      {children}
    </NxLoadWrapper>
  </div>;
}
