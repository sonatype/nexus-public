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
import PropTypes from 'prop-types';
import classNames from 'classnames';

export default function FieldWrapper({labelText, descriptionText, id, isOptional, children}) {
  const firstChildProps = React.Children.toArray(children)[0].props;
  const fieldName = firstChildProps.id || firstChildProps.name;
  const classes = classNames('nx-label', {
    'nx-label--optional': isOptional
  });

  return <div className='nx-form-group' id={id}>
    {labelText ? <label htmlFor={fieldName} className={classes}><span className="nx-label__text">{labelText}</span></label> : null}
    {descriptionText ? <p className='nx-p'>{descriptionText}</p> : null}
    {children}
  </div>;
};

FieldWrapper.propTypes = {
  descriptionText: PropTypes.string,
  isOptional: PropTypes.bool,
  labelText: PropTypes.string.isRequired
};
