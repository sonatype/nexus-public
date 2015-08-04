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
package org.sonatype.nexus.notification;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.AbstractLastingConfigurable;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.model.CNotification;
import org.sonatype.nexus.configuration.model.CNotificationConfiguration;
import org.sonatype.nexus.configuration.model.CNotificationTarget;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named
public class DefaultNotificationManager
    extends AbstractLastingConfigurable<CNotification>
    implements NotificationManager
{

  private final Map<String, Carrier> carriers;

  // ==

  @Inject
  public DefaultNotificationManager(EventBus eventBus, ApplicationConfiguration applicationConfiguration,
      Map<String, Carrier> carriers)
  {
    super("Notification Manager", eventBus, applicationConfiguration);
    this.carriers = checkNotNull(carriers);
  }

  @Override
  protected void initializeConfiguration()
      throws ConfigurationException
  {
    if (getApplicationConfiguration().getConfigurationModel() != null) {
      configure(getApplicationConfiguration());
    }
  }

  @Override
  protected CoreConfiguration<CNotification> wrapConfiguration(Object configuration)
      throws ConfigurationException
  {
    if (configuration instanceof ApplicationConfiguration) {
      return new CNotificationConfiguration(getApplicationConfiguration());
    }
    else {
      throw new ConfigurationException("The passed configuration object is of class \""
          + configuration.getClass().getName() + "\" and not the required \""
          + ApplicationConfiguration.class.getName() + "\"!");
    }
  }

  // ==

  @Override
  public boolean isEnabled() {
    // TODO: this is needed, since events are happening even before configuration is loaded
    // And the NotificationEventInspector will start relaying even before we know our config!
    // This is actually chicken-egg problem: the EventInspector Hosts are wired in before we
    // load Nexus config.
    return isConfigured() && getCurrentConfiguration(false).isEnabled();
  }

  @Override
  public void setEnabled(boolean val) {
    getCurrentConfiguration(true).setEnabled(val);
  }

  @Override
  public NotificationTarget readNotificationTarget(String targetId) {
    if (targetId == null) {
      throw new NullPointerException("Notification target ID can't be null!");
    }

    // TODO: we always ask for this since we have this group only
    targetId = NotificationCheat.AUTO_BLOCK_NOTIFICATION_GROUP_ID;

    List<CNotificationTarget> targets = getCurrentConfiguration(false).getNotificationTargets();

    for (CNotificationTarget target : targets) {
      if (targetId.equals(target.getTargetId())) {
        NotificationTarget result = new NotificationTarget();

        result.setTargetId(target.getTargetId());

        result.getTargetRoles().addAll(target.getTargetRoles());

        result.getTargetUsers().addAll(target.getTargetUsers());

        result.getExternalTargets().addAll(target.getTargetExternals());

        return result;
      }
    }

    return null;
  }

  @Override
  public void updateNotificationTarget(NotificationTarget target) {
    if (target == null) {
      throw new NullPointerException("Notification target can't be null!");
    }

    // TODO: reimplement this to handle multiple targets!
    target.setTargetId(NotificationCheat.AUTO_BLOCK_NOTIFICATION_GROUP_ID);

    CNotificationTarget ctarget = new CNotificationTarget();

    ctarget.setTargetId(target.getTargetId());
    ctarget.getTargetRoles().addAll(target.getTargetRoles());
    ctarget.getTargetUsers().addAll(target.getTargetUsers());
    ctarget.getTargetExternals().addAll(target.getExternalTargets());

    List<CNotificationTarget> targets = getCurrentConfiguration(true).getNotificationTargets();

    targets.clear();

    targets.add(ctarget);
  }

  @Override
  public void notifyTargets(NotificationRequest request) {
    if (!isEnabled()) {
      return;
    }

    for (NotificationTarget target : request.getTargets()) {
      // TODO: should come from the group
      // for now, email is wired in
      String carrierKey = NotificationCheat.CARRIER_KEY;

      Carrier carrier = carriers.get(carrierKey);

      if (carrier != null) {
        try {
          carrier.notifyTarget(target, request.getMessage());
        }
        catch (NotificationException e) {
          log.warn("Could not send out notification over carrier \"{}\".", carrierKey, e);
        }
      }
      else {
        log.info("Notification carrier \"{}\" is unknown!", carrierKey);
      }
    }
  }

}
