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
package org.sonatype.nexus.api.rest.selfhosted.telemetry;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.sonatype.nexus.api.rest.selfhosted.telemetry.model.MandatoryTelemetryAcknowledgementStatus;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.UserIdHelper;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records and reports the one-time acknowledgement that telemetry became mandatory in 3.97.0.
 * <p>
 * The acknowledgement changes nothing: transmission begins at upgrade whether or not this record
 * exists, and the grace period an upgraded instance is owed is restored by the upgrade itself (see
 * {@code MandatoryTelemetryGracePeriodMigrationStep_2_174} in the private analytics module), not by
 * this endpoint. It is purely a record that the notice was displayed and dismissed, read back by the
 * onboarding item to decide whether to raise the card again.
 * <p>
 * It used to carry the grace period reset as a side effect. That made a second chance every upgraded
 * instance is owed depend on an administrator opening the wizard, and it forced this one-shot record
 * to double as run-once bookkeeping so a repeated POST could not stack another reset. The upgrade
 * step reaches every upgraded instance and Flyway already guarantees once-per-instance, so both
 * problems went away with the move.
 * <p>
 * The key and field names are duplicated in
 * {@code MandatoryTelemetryAcknowledgementOnboardingItem} (module {@code nexus-onboarding-plugin}),
 * which reads them to decide whether to raise the onboarding card. The two modules share no
 * dependency edge, so they cannot share a constant — the same arrangement the Community EULA uses.
 * Change one and you must change the other.
 */
public class MandatoryTelemetryAcknowledgementApiResource
    implements Resource, MandatoryTelemetryAcknowledgementApiResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Key for the acknowledgement record in {@code nexus_key_value}.
   * <p>
   * <b>DUPLICATED in {@code MandatoryTelemetryAcknowledgementOnboardingItem}</b>
   * (nexus-onboarding-plugin module). Changes here must be mirrored there.
   * This duplication exists because the two modules share no dependency edge —
   * the same arrangement the Community EULA uses.
   * <p>
   * Find the other location: {@code git grep -n 'ACKNOWLEDGEMENT_KEY.*telemetry'}
   */
  static final String ACKNOWLEDGEMENT_KEY = "nexus.telemetry.mandatory.acknowledgement";

  /**
   * Field name within the acknowledgement record.
   * <p>
   * <b>DUPLICATED in {@code MandatoryTelemetryAcknowledgementOnboardingItem}</b>
   * (nexus-onboarding-plugin module). Changes here must be mirrored there.
   * This duplication exists because the two modules share no dependency edge —
   * the same arrangement the Community EULA uses.
   * <p>
   * Find the other location: {@code git grep -n 'ACKNOWLEDGEMENT_FIELD'}
   */
  static final String ACKNOWLEDGEMENT_FIELD = "acknowledgement";

  private final GlobalKeyValueStore globalKeyValueStore;

  public MandatoryTelemetryAcknowledgementApiResource(final GlobalKeyValueStore globalKeyValueStore) {
    this.globalKeyValueStore = globalKeyValueStore;
  }

  @Override
  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Produces(MediaType.APPLICATION_JSON)
  public MandatoryTelemetryAcknowledgementStatus getMandatoryTelemetryAcknowledgement() {
    MandatoryTelemetryAcknowledgementStatus status = new MandatoryTelemetryAcknowledgementStatus();
    status.setAcknowledgement(isAcknowledged());
    return status;
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Consumes(MediaType.APPLICATION_JSON)
  public void acknowledgeMandatoryTelemetry(final MandatoryTelemetryAcknowledgementStatus status) {
    if (status == null || !status.isAcknowledgement()) {
      throw new ValidationErrorsException(ACKNOWLEDGEMENT_FIELD, "Acknowledgement must be true");
    }

    if (isAcknowledged()) {
      log.debug("Mandatory telemetry was already acknowledged, ignoring request");
      return;
    }

    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey(ACKNOWLEDGEMENT_KEY);
    kv.setType(ValueType.OBJECT);
    kv.setValue(Map.of(ACKNOWLEDGEMENT_FIELD, status.isAcknowledgement()));
    globalKeyValueStore.setKey(kv);

    log.info("Mandatory telemetry acknowledged by {}", UserIdHelper.get());
  }

  private boolean isAcknowledged() {
    return globalKeyValueStore.getKey(ACKNOWLEDGEMENT_KEY)
        .map(NexusKeyValue::value)
        .map(this::readAcknowledgementFlag)
        .orElse(false);
  }

  private boolean readAcknowledgementFlag(final Map<String, Object> value) {
    Object raw = value.get(ACKNOWLEDGEMENT_FIELD);
    return raw instanceof Boolean && (Boolean) raw;
  }
}
