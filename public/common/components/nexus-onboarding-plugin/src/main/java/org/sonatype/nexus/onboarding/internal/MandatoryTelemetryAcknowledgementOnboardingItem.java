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
package org.sonatype.nexus.onboarding.internal;

import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistribution;
import org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.TelemetryEnforcement;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.onboarding.OnboardingItem;
import org.sonatype.nexus.onboarding.OnboardingItemPriority;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Raises a one-time onboarding card telling administrators that telemetry became mandatory in
 * 3.97.0, on self-hosted Pro and Community instances.
 * <p>
 * The card is informational. Telemetry transmission starts at upgrade whether or not the
 * acknowledgement is recorded, so this item never gates anything; it only tracks whether an
 * administrator has been told.
 * <p>
 * Gating happens in {@link #applies()} through public API rather than through
 * {@code @ConditionalOnEdition} / {@code @ConditionalOnDistribution}: those annotations live in a
 * private module unavailable here, and {@code @ConditionalOnEdition} additionally reads a property
 * published only between the two component-scan rounds.
 * <p>
 * Licenses carrying the telemetry opt-out entitlement never see the card. Only a commercial module
 * can read that entitlement, so the answer arrives through {@link TelemetryEnforcement}, injected
 * optionally. An absent bean means "telemetry required": it is absent on Community and Core, which
 * hold no opt-out entitlement, so inverting that polarity would hide the card on every Community
 * instance without failing a single Pro test. Withholding the card records nothing, so a license that
 * later loses the entitlement raises the card then.
 * <p>
 * The acknowledgement record is written by {@code MandatoryTelemetryAcknowledgementApiResource} in
 * {@code nexus-api-rest-selfhosted}. The key and field names below are duplicated there because the
 * two modules share no dependency edge — the same arrangement the Community EULA already uses.
 */
@Component
public class MandatoryTelemetryAcknowledgementOnboardingItem
    implements OnboardingItem
{
  /**
   * Key for the acknowledgement record in {@code nexus_key_value}.
   * <p>
   * <b>DUPLICATED in {@code MandatoryTelemetryAcknowledgementApiResource}</b>
   * (nexus-api-rest-selfhosted module). Changes here must be mirrored there.
   * This duplication exists because the two modules share no dependency edge —
   * the same arrangement the Community EULA uses.
   * <p>
   * Find the other location: {@code git grep -n 'ACKNOWLEDGEMENT_KEY.*telemetry'}
   */
  static final String ACKNOWLEDGEMENT_KEY = "nexus.telemetry.mandatory.acknowledgement";

  /**
   * Field name within the acknowledgement record.
   * <p>
   * <b>DUPLICATED in {@code MandatoryTelemetryAcknowledgementApiResource}</b>
   * (nexus-api-rest-selfhosted module). Changes here must be mirrored there.
   * This duplication exists because the two modules share no dependency edge —
   * the same arrangement the Community EULA uses.
   * <p>
   * Find the other location: {@code git grep -n 'ACKNOWLEDGEMENT_FIELD'}
   */
  static final String ACKNOWLEDGEMENT_FIELD = "acknowledgement";

  private final ApplicationVersion applicationVersion;

  private final GlobalKeyValueStore globalKeyValueStore;

  private final TelemetryEnforcement enforcementChecker;

  @Autowired
  public MandatoryTelemetryAcknowledgementOnboardingItem(
      final ApplicationVersion applicationVersion,
      final GlobalKeyValueStore globalKeyValueStore,
      @Autowired(required = false) final TelemetryEnforcement enforcementChecker)
  {
    this.applicationVersion = checkNotNull(applicationVersion);
    this.globalKeyValueStore = checkNotNull(globalKeyValueStore);
    // Deliberately unchecked: absent on every edition without the opt-out entitlement.
    this.enforcementChecker = enforcementChecker;
  }

  @Override
  public String getType() {
    return "MandatoryTelemetryAcknowledgement";
  }

  @Override
  public int getPriority() {
    return OnboardingItemPriority.MANDATORY_TELEMETRY_ACKNOWLEDGEMENT_ONBOARDING;
  }

  @Override
  public boolean applies() {
    return isSelfHosted() && isApplicableEdition() && isTelemetryRequired() && !isAcknowledged();
  }

  private boolean isTelemetryRequired() {
    // null for Community — no opt-out capability, so telemetry is required and the card applies
    return enforcementChecker == null || enforcementChecker.isTelemetryRequired();
  }

  /**
   * Distribution is decided by packaging, never by configuration: the cloud and self-hosted
   * distributions each contribute a classpath marker, and Core/OSS contributes none (so detection
   * returns {@code null} there and the card correctly stays hidden).
   */
  private boolean isSelfHosted() {
    return NexusDistributionType.SELF_HOSTED == NexusDistribution.detect(getClass().getClassLoader());
  }

  /**
   * {@code isAnyProEdition()} deliberately includes Cloud, which reports the Pro edition short name;
   * {@link #isSelfHosted()} is what excludes it. Community runs on the self-hosted distribution and
   * is in scope.
   */
  private boolean isApplicableEdition() {
    return applicationVersion.isAnyProEdition() || applicationVersion.isCommunityEdition();
  }

  /**
   * Guarded read. A row whose {@code acknowledgement} field is absent, null, or not a Boolean counts
   * as not acknowledged, so a partially-written row shows the card again instead of throwing.
   */
  private boolean isAcknowledged() {
    return globalKeyValueStore.getKey(ACKNOWLEDGEMENT_KEY)
        .map(NexusKeyValue::value)
        .map(value -> value.get(ACKNOWLEDGEMENT_FIELD) instanceof Boolean flag && flag)
        .orElse(false);
  }
}
