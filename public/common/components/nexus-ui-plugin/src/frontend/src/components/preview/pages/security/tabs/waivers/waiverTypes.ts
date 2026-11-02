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

/** Mirrors WaiverXO on the merged backend. Field names are verbatim from the wire. */
export interface Waiver {
  policyWaiverId: string;
  comment: string | null;
  /** Epoch millis. Primitive on the wire — always present, 0 if upstream had none. */
  createTime: number;
  /** Epoch millis, or null meaning "never expires". */
  expiryTime: number | null;
  scopeOwnerType: string;
  scopeOwnerId: string;
  scopeOwnerName: string | null;
  policyId: string | null;
  policyName: string | null;
  threatLevel: number;
  displayName: string | null;
  ecosystem: string | null;
  groupId: string | null;
  artifactId: string | null;
  version: string | null;
  hash: string | null;
  matcherStrategy: string | null;
  reasonText: string | null;
  policyWaiverReasonId: string | null;
  /** null is distinct from false: null means no upgrade INFORMATION. */
  componentUpgradeAvailable: boolean | null;
  expireWhenRemediationAvailable: boolean;
  forContainerImage: boolean;
  lastRenewedAt: number | null;
  lastRenewedBy: string | null;
  lastRenewalComment: string | null;
}

export interface WaiverListResponse {
  total: number;
  results: Waiver[];
}

/** GET /waivers/reasons returns a BARE ARRAY of these, not a wrapper object. */
export interface WaiverReason {
  id: string;
  type: string | null;
  reasonText: string | null;
}

export interface RenewRequest {
  waiverIds: string[];
  /** Epoch millis, or null meaning "never expires". There is no "leave unchanged". */
  newExpiryTime: number | null;
  comment: string | null;
  reasonId: string | null;
}

/** Counts only — never which ids succeeded. Partial success cannot be patched client-side. */
export interface RenewResult {
  renewed: number;
  notFound: number;
  errors: string[];
}
