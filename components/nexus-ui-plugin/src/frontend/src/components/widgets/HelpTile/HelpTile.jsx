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
import classNames from "classnames";

import {NxFontAwesomeIcon, NxP, NxH3} from "@sonatype/react-shared-components";
import {faInfoCircle} from '@fortawesome/free-solid-svg-icons';

import './HelpTile.scss';
/**
 * @since 3.29
 */
export default function HelpTile({className, header, body, ...attrs}) {
  return <div className={classNames('nxrm-help-tile', 'nx-tile', className)} {...attrs}>
    <div className="nx-tile-content">
      <NxH3><NxFontAwesomeIcon icon={faInfoCircle}/><span>{header}</span></NxH3>
      <NxP>{body}</NxP>
    </div>
  </div>;
}

HelpTile.propTypes = {
  className: PropTypes.string,
  header: PropTypes.string,
  body: PropTypes.oneOfType([PropTypes.string, PropTypes.element]),
};
