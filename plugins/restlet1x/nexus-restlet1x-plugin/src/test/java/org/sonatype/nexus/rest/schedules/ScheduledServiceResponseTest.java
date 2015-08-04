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
package org.sonatype.nexus.rest.schedules;

import org.sonatype.nexus.rest.AbstractRestTestCase;
import org.sonatype.nexus.rest.model.ScheduledServiceAdvancedResource;
import org.sonatype.nexus.rest.model.ScheduledServiceDailyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceMonthlyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceOnceResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.rest.model.ScheduledServiceResourceResponse;
import org.sonatype.nexus.rest.model.ScheduledServiceWeeklyResource;
import org.sonatype.plexus.rest.representation.XStreamRepresentation;

import org.junit.Test;
import org.restlet.data.MediaType;

public class ScheduledServiceResponseTest
    extends AbstractRestTestCase
{

  @Test
  public void testNoScheduledService()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"id\":null,\"name\":\"test\",\"typeId\":\"Synchronize Repositories\",\"schedule\":\"none\"}}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    ScheduledServiceResourceResponse response =
        (ScheduledServiceResourceResponse) representation.getPayload(new ScheduledServiceResourceResponse());

    assert response.getData().getId() == null;
    assert response.getData().getName().equals("test");
    assert response.getData().getTypeId().equals("Synchronize Repositories");
    assert response.getData().getSchedule().equals("none");
  }

  @Test
  public void testOnceScheduledService()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"id\":null,\"name\":\"test\",\"typeId\":\"Synchronize Repositories\",\"schedule\":\"once\",\"startDate\":\"1210651200000\",\"startTime\":\"12:30\"}}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    ScheduledServiceResourceResponse response =
        (ScheduledServiceResourceResponse) representation.getPayload(new ScheduledServiceResourceResponse());

    assert response.getData().getId() == null;
    assert response.getData().getName().equals("test");
    assert response.getData().getTypeId().equals("Synchronize Repositories");
    assert response.getData().getSchedule().equals("once");
    assert ((ScheduledServiceOnceResource) response.getData()).getStartDate().equals("1210651200000");
    assert ((ScheduledServiceOnceResource) response.getData()).getStartTime().equals("12:30");
  }

  @Test
  public void testDailyScheduledService()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"id\":null,\"name\":\"test\",\"typeId\":\"Synchronize Repositories\",\"schedule\":\"daily\",\"startDate\":\"1210651200000\",\"recurringTime\":\"12:30\"}}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    ScheduledServiceResourceResponse response =
        (ScheduledServiceResourceResponse) representation.getPayload(new ScheduledServiceResourceResponse());

    assert response.getData().getId() == null;
    assert response.getData().getName().equals("test");
    assert response.getData().getTypeId().equals("Synchronize Repositories");
    assert response.getData().getSchedule().equals("daily");
    assert ((ScheduledServiceDailyResource) response.getData()).getStartDate().equals("1210651200000");
    assert ((ScheduledServiceDailyResource) response.getData()).getRecurringTime().equals("12:30");
  }

  @Test
  public void testWeeklyScheduledService()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"id\":null,\"name\":\"test\",\"typeId\":\"Synchronize Repositories\",\"schedule\":\"weekly\",\"startDate\":\"1210651200000\",\"recurringTime\":\"12:30\",\"recurringDay\":[\"Monday\",\"Wednesday\"]}}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    ScheduledServiceResourceResponse response =
        (ScheduledServiceResourceResponse) representation.getPayload(new ScheduledServiceResourceResponse());

    assert response.getData().getId() == null;
    assert response.getData().getName().equals("test");
    assert response.getData().getTypeId().equals("Synchronize Repositories");
    assert response.getData().getSchedule().equals("weekly");
    assert ((ScheduledServiceWeeklyResource) response.getData()).getStartDate().equals("1210651200000");
    assert ((ScheduledServiceWeeklyResource) response.getData()).getRecurringTime().equals("12:30");
  }

  @Test
  public void testMonthlyScheduledService()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"id\":null,\"name\":\"test\",\"typeId\":\"Synchronize Repositories\",\"schedule\":\"monthly\",\"startDate\":\"1210651200000\",\"recurringTime\":\"12:30\",\"recurringDay\":[\"1\",\"2\"]}}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    ScheduledServiceResourceResponse response =
        (ScheduledServiceResourceResponse) representation.getPayload(new ScheduledServiceResourceResponse());

    assert response.getData().getId() == null;
    assert response.getData().getName().equals("test");
    assert response.getData().getTypeId().equals("Synchronize Repositories");
    assert response.getData().getSchedule().equals("monthly");
    assert ((ScheduledServiceMonthlyResource) response.getData()).getStartDate().equals("1210651200000");
    assert ((ScheduledServiceMonthlyResource) response.getData()).getRecurringTime().equals("12:30");
  }

  @Test
  public void testAdvancedScheduledService()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"id\":null,\"name\":\"test\",\"typeId\":\"Synchronize Repositories\",\"schedule\":\"advanced\",\"cronCommand\":\"somecroncommand\"}}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    ScheduledServiceResourceResponse response =
        (ScheduledServiceResourceResponse) representation.getPayload(new ScheduledServiceResourceResponse());

    assert response.getData().getId() == null;
    assert response.getData().getName().equals("test");
    assert response.getData().getTypeId().equals("Synchronize Repositories");
    assert response.getData().getSchedule().equals("advanced");
    assert ((ScheduledServiceAdvancedResource) response.getData()).getCronCommand().equals("somecroncommand");
  }

  @Test
  public void testAdvancedScheduledServiceWithProperties()
      throws Exception
  {
    String jsonString =
        "{\"data\":{\"id\":null,\"name\":\"test\",\"typeId\":\"Synchronize Repositories\",\"schedule\":\"advanced\",\"cronCommand\":\"somecroncommand\",\"properties\":[{\"key\":\"1\",\"value\":\"true\",\"@class\":\"org.sonatype.nexus.rest.model.ScheduledServicePropertyResource\"}]}}}";
    XStreamRepresentation representation =
        new XStreamRepresentation(xstream, jsonString, MediaType.APPLICATION_JSON);

    ScheduledServiceResourceResponse response =
        (ScheduledServiceResourceResponse) representation.getPayload(new ScheduledServiceResourceResponse());

    assert response.getData().getId() == null;
    assert response.getData().getName().equals("test");
    assert response.getData().getTypeId().equals("Synchronize Repositories");
    assert response.getData().getSchedule().equals("advanced");
    assert response.getData().getProperties().size() == 1;
    assert ((ScheduledServicePropertyResource) response.getData().getProperties().get(0)).getKey().equals("1");
    assert ((ScheduledServicePropertyResource) response.getData().getProperties().get(0)).getValue().equals(
        "true");
    assert ((ScheduledServiceAdvancedResource) response.getData()).getCronCommand().equals("somecroncommand");
  }
}
