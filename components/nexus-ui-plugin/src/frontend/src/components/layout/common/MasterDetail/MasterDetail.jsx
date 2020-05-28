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
import React, {Children, cloneElement, useState} from 'react';
import PropTypes from 'prop-types';

import Master from './Master';
import Detail from './Detail';

import ExtJS from '../../../../interface/ExtJS';

/**
 * @since 3.24
 */
export default function MasterDetail({className, children, path, ...attrs}) {
  const {location: {pathname}} = ExtJS.useHistory({basePath: path});
  const [isCreate, setCreate] = useState(false);

  const classes = classNames('nxrm-master-detail', className);
  const isMasterRoute = pathname === '';
  const itemId = isMasterRoute ? '' : pathname.substring(1);

  function onCreate() {
    setCreate(true);
  }

  function onEdit(itemId) {
    window.location.hash = `${path}:${itemId}`;
  }

  function onDone() {
    setCreate(false);
    window.location.hash = path;
  }

  const childrenArray = Children.toArray(children);
  const master = cloneElement(childrenArray.filter(child => child.type === Master)[0], {onCreate, onEdit});
  const detail = cloneElement(childrenArray.filter(child => child.type === Detail)[0], {itemId, onDone});

  return (
      <div className={classes} {...attrs}>
        {isMasterRoute && !isCreate ? master : null}
        {!isMasterRoute || isCreate ? detail : null}
      </div>
  );
}

MasterDetail.propTypes = {
  path: PropTypes.string.isRequired
};
