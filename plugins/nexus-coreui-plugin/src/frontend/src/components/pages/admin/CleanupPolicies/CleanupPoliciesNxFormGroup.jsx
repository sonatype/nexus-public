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
import React, {forwardRef} from 'react';
import classnames from 'classnames';
import {useUniqueId} from '@sonatype/react-shared-components';

const CleanupPoliciesNxFormGroup = forwardRef(function NxFormGroup(
  {className, label, children, isRequired, prefix, suffix, ...attrs},
  ref
) {
  const classNames = classnames('nx-form-group', className),
    labelClassnames = classnames('nx-label', {
      'nx-label--optional': !isRequired,
    }),
    childId = useUniqueId('nx-form-group-child', children.props.id),
    childDescribedBy = classnames(children.props['aria-describedby']),
    childRequired = children.props['aria-required'] ?? isRequired,
    childNeedsAugmentation =
      !children.props.id || (isRequired && !children.props['aria-required']),
    childEl = childNeedsAugmentation
      ? React.cloneElement(children, {
          id: childId,
          'aria-describedby': childDescribedBy,
          'aria-required': childRequired,
        })
      : children;

  return (
    <div ref={ref} className={classNames} {...attrs}>
      {label && (
        <label htmlFor={childId} className={labelClassnames}>
          <span className="nx-label__text">{label}</span>
        </label>
      )}
      <span className="prefix">{prefix}</span>
      {childEl}
      <span className="suffix">{suffix}</span>
    </div>
  );
});

export default CleanupPoliciesNxFormGroup;
