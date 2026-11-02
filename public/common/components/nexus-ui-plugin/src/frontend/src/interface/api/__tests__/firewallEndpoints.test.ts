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

import { ENDPOINTS } from '../rest-client';

describe('Firewall security endpoints', () => {
  it('exposes the IQ status refresh endpoint', () => {
    expect(ENDPOINTS.FIREWALL_SECURITY_IQ_STATUS_REFRESH).toBe(
      '/service/rest/internal/firewall/security/iq-status/refresh'
    );
  });

  it('exposes the quarantine summary endpoint', () => {
    expect(ENDPOINTS.FIREWALL_QUARANTINE_SUMMARY).toBe('/service/rest/internal/ui/firewall/quarantine/summary');
  });
});
