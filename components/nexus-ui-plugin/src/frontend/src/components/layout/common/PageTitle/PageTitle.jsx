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
import {NxFontAwesomeIcon} from "@sonatype/react-shared-components";

import './PageTitle.scss';

/**
 * @since 3.26
 */
export default function PageTitle({className, icon, text, description, ...rest}) {
  const classes = classNames('nx-page-title', className);

  return <div className={classes} {...rest}>
    <h1 className="nx-h1 nx-feature-name">
      {icon && <NxFontAwesomeIcon icon={icon} className="nx-page-title__page-icon"/>}
      <span>{text}</span>
    </h1>
    {description && <p className="nx-page-title__description nx-feature-description">{description}</p>}
  </div>;
}

PageTitle.propTypes = {
  text: PropTypes.string.isRequired,
  icon: PropTypes.oneOfType([
    PropTypes.object,
    PropTypes.array,
    PropTypes.string
  ]),
  description: PropTypes.string
};
