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

/**
 * Preview UI string dictionary.
 *
 * Layers preview-specific strings over the base nexus-ui-plugin UIStrings.
 * Preview components should import from here rather than from
 * nexus-coreui-plugin's constants/UIStrings so they remain portable within
 * nexus-ui-plugin.
 *
 * Preview-specific keys sourced from usage:
 *   UIStrings.MALICIOUS_RISK  (MaliciousRiskStrings — all MALICIOUS_RISK.* keys)
 */
import NxrmUIStrings from '../../../constants/UIStrings';
import MaliciousRiskStrings from './pages/maliciousrisk/MaliciousRiskStrings';
import PreviewUiStrings from './pages/admin/system/PreviewUiStrings';

const PreviewUIStrings = {
  ...NxrmUIStrings,
  // Preview-specific keys referenced by components/super/:
  ...MaliciousRiskStrings,
  ...PreviewUiStrings,
};

export default PreviewUIStrings;
