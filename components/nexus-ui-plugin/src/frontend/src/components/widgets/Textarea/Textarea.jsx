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
import classNames from 'classnames';
import PropTypes from 'prop-types';
import React, {forwardRef} from 'react';

import './Textarea.scss';

import FieldErrorMessage from '../FieldErrorMessage/FieldErrorMessage';
import {hasValidationErrors, getFirstValidationError} from '@sonatype/react-shared-components/util/validationUtil';

/**
 * @since 3.22
 */
const Textarea = forwardRef(({value, className, name, id, validationErrors, ...attrs}, ref) => {

  const isInvalid = hasValidationErrors(validationErrors);
  const classes = classNames('nxrm-textarea', className, {
    'invalid': isInvalid
  });
  return <>
    <textarea
        id={id || name}
        name={name}
        value={value}
        className={classes}
        ref={ref}
        {...attrs}
    />
    {isInvalid ? <FieldErrorMessage message={getFirstValidationError(validationErrors)}/> : null}
  </>;
});

Textarea.propTypes = {
  validationErrors: PropTypes.oneOfType([PropTypes.string, PropTypes.array])
};

export default Textarea;
