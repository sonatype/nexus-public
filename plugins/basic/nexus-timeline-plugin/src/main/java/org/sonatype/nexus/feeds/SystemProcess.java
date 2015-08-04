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
package org.sonatype.nexus.feeds;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/**
 * A system process event, a system event that has duration and holds possible error cause if any.
 *
 * @author cstamas
 */
public class SystemProcess
    extends SystemEvent
{
  enum Status
  {
    STARTED, FINISHED, BROKEN, CANCELED
  }

  /**
   * When was the process started?
   */
  private final Date started;

  /**
   * The process status.
   */
  private Status status;

  /**
   * When has finised the process?
   */
  private Date finished;

  /**
   * Human message/descritpion.
   */
  private String finishedMessage;

  /**
   * The error cause while running, if any.
   */
  private Throwable errorCause;

  public SystemProcess(final Date eventDate, final String action, final String message, final Date started) {
    super(eventDate, action, message);

    this.started = started;

    this.status = Status.STARTED;
  }

  public void finished(String message) {
    this.finished = new Date();

    this.status = Status.FINISHED;

    this.finishedMessage = message;
  }

  public void canceled(String message) {
    this.finished = new Date();

    this.status = Status.CANCELED;

    this.finishedMessage = message;
  }

  public void broken(Throwable e) {
    this.errorCause = e;

    this.finished = new Date();

    this.status = Status.BROKEN;
  }

  public Date getEventDate() {
    if (finished == null) {
      return super.getEventDate();
    }
    else {
      return getFinished();
    }
  }

  public Date getFinished() {
    return finished;
  }

  public String getMessage() {
    StringBuilder sb = new StringBuilder(super.getMessage());

    if (started != null) {
      sb.append(" : Process started on ");

      sb.append(started.toString());

      if (finished != null) {
        if (Status.BROKEN.equals(status)) {
          sb.append(", finished on ").append(finished.toString()).append(" with error.");

          if (errorCause != null) {
            sb.append(" Error message is: ").append(errorCause.getClass().getName());

            if (errorCause.getMessage() != null) {
              sb.append(", ").append(errorCause.getMessage());
            }

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            errorCause.printStackTrace(pw);
            sb.append(" Strack trace: ").append(sw.toString());
          }
        }
        else if (Status.FINISHED.equals(status)) {
          sb.append(", finished successfully on ").append(finished.toString());
        }
        else if (Status.CANCELED.equals(status)) {
          sb.append(", canceled on ").append(finished.toString());
        }
      }
      else {
        sb.append(", not yet finished.");
      }
    }

    return sb.toString();
  }

  public String toString() {
    return getMessage();
  }

}
