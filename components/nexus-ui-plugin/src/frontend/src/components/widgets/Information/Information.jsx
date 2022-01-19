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

import './Information.scss';

/**
 * @since 3.22
 */
export default function Information({information}) {
  return <table className="nxrm-information">
    <tbody>
    {Object.entries(information).map(([name, value]) =>
        <InformationRow key={name}>
          <InformationName>{name}</InformationName>
          <InformationValue>{String(value)}</InformationValue>
        </InformationRow>
    )}
    </tbody>
  </table>;
}

Information.propTypes = {
  information: PropTypes.object.isRequired
}

function InformationRow({children}) {
  return <tr className="nxrm-information--row">{children}</tr>;
}

function InformationName({children}) {
  return <td className="nxrm-information--name">{children}</td>;
}

function InformationValue({children}) {
  return <td className="nxrm-information--value">{children}</td>;
}
