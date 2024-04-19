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
import RoutingRuleMatcherRow from './RoutingRuleMatcherRow';
import {
  screen,
  render
} from "@testing-library/react";
import userEvent from '@testing-library/user-event';

const selectors = {
  removeButton: () => screen.queryByRole('button')
};

describe('RoutingRuleMatcherRow', function() {
  it('should render without remove button', function() {
    const onRemove = jest.fn();
    render(<RoutingRuleMatcherRow
        label="label"
        isPristine={true}
        value=""
        showRemoveButton={false}
        onRemove={onRemove}
    />);

    expect(selectors.removeButton()).not.toBeInTheDocument();
  });

  it('should render with remove button', function() {
    const onRemove = jest.fn();
    render(<RoutingRuleMatcherRow
        label="label"
        isPristine={true}
        value=""
        showRemoveButton={true}
        onRemove={onRemove}
    />);
  
    expect(selectors.removeButton()).toBeInTheDocument();
  });
  
  it('should remove the row when the button is clicked', function() {
    const onRemove = jest.fn();
    render(<RoutingRuleMatcherRow
        label="label"
        isPristine={true}
        value=""
        showRemoveButton={true}
        onRemove={onRemove}
    />);
  
    userEvent.click(selectors.removeButton());
  
    expect(onRemove).toHaveBeenCalled();
  });
});
