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

import { createContext, useContext } from 'react';

/**
 * Whether the preview views may prompt the user to upgrade. True only in the 'preview-pro' tier: a Pro+ or Enterprise
 * user without IQ already owns the edition and needs to connect IQ Server, not upgrade. Defaults to false so a view
 * rendered outside FirewallSecurityTabPanel never shows an upgrade prompt.
 */
export const ProUpsellContext = createContext(false);

export const useProUpsell = (): boolean => useContext(ProUpsellContext);
