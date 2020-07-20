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
import React from 'react';

import './Select.scss';
import FieldErrorMessage from "../FieldErrorMessage/FieldErrorMessage";
import PropTypes from "prop-types";
import Textfield from "../Textfield/Textfield";
import {getFirstValidationError, hasValidationErrors} from "@sonatype/react-shared-components/util/validationUtil";

/**
 * @since 3.21
 */
export default function Select({value, children, className, name, id, validationErrors, ...rest}) {
  const isInvalid = hasValidationErrors(validationErrors);
  const classes = classNames('nxrm-select', className, {
    'invalid': isInvalid
  });

  return <>
    <select id={id || name} name={name} className={classes} value={value} {...rest}>
      {children}
    </select>
    {isInvalid ? <FieldErrorMessage message={getFirstValidationError(validationErrors)}/> : null}
  </>;
}

Select.propTypes = {
  validationErrors: PropTypes.oneOfType([PropTypes.string, PropTypes.array])
};
