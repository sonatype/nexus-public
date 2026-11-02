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

import { getGlobalDistributionTypeOptions } from '../globalDistributionTypeOptions';

describe('getGlobalDistributionTypeOptions', () => {
  it('lists None, S3 Transfer Acceleration and CloudFront in that order', () => {
    expect(getGlobalDistributionTypeOptions(true).map((o) => o.value)).toEqual(['', 'TRANSFER_ACCELERATION', 'CLOUDFRONT']);
  });

  it('disables and pills CloudFront and S3 Transfer Acceleration below Enterprise', () => {
    const [none, transferAcceleration, cloudFront] = getGlobalDistributionTypeOptions(false);

    expect(none.disabled).toBeFalsy();
    expect(none.badge).toBeUndefined();
    expect(transferAcceleration.disabled).toBe(true);
    expect(transferAcceleration.badge).toBeTruthy();
    expect(cloudFront.disabled).toBe(true);
    expect(cloudFront.badge).toBeTruthy();
  });

  it('enables every option without a badge on Enterprise', () => {
    const options = getGlobalDistributionTypeOptions(true);

    expect(options.every((o) => !o.disabled && o.badge === undefined)).toBe(true);
  });
});
