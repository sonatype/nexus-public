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
package org.sonatype.nexus.rest.feeds.sources;

import java.util.Date;

import javax.inject.Inject;

import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import org.restlet.data.MediaType;

/**
 * @author Juven Xu
 */
abstract public class AbstractNexusItemEventEntryBuilder
    extends ComponentSupport
    implements SyndEntryBuilder<NexusArtifactEvent>
{
  private RepositoryRegistry repositoryRegistry;

  @Inject
  public void setRepositoryRegistry(final RepositoryRegistry repositoryRegistry) {
    this.repositoryRegistry = repositoryRegistry;
  }

  protected RepositoryRegistry getRepositoryRegistry() {
    return repositoryRegistry;
  }

  public SyndEntry buildEntry(NexusArtifactEvent event) {
    SyndEntry entry = new SyndEntryImpl();

    entry.setTitle(buildTitle(event));

    entry.setLink(buildLink(event));

    entry.setPublishedDate(buildPublishDate(event));

    entry.setAuthor(buildAuthor(event));

    entry.setDescription(buildDescription(event));

    return entry;
  }

  // better to override to provide more detailed information
  protected String buildTitle(NexusArtifactEvent event) {
    return event.getAction();
  }

  protected String buildLink(NexusArtifactEvent event) {
    return "content/repositories/" + event.getNexusItemInfo().getRepositoryId()
        + event.getNexusItemInfo().getPath();
  }

  protected Date buildPublishDate(NexusArtifactEvent event) {
    return event.getEventDate();
  }

  protected String buildAuthor(NexusArtifactEvent event) {
    if (event.getEventContext().containsKey(AccessManager.REQUEST_USER)) {
      return (String) event.getEventContext().get(AccessManager.REQUEST_USER);
    }
    else {
      return null;
    }
  }

  protected SyndContent buildDescription(NexusArtifactEvent event) {
    SyndContent content = new SyndContentImpl();

    content.setType(MediaType.TEXT_PLAIN.toString());

    StringBuilder msg = new StringBuilder();

    if (event.getMessage() != null) {
      msg.append(event.getMessage());
      msg.append(' ');
    }

    msg.append(buildDescriptionMsgItem(event));

    msg.append(buildDescriptionMsgAction(event));

    msg.append(buildDescriptionMsgAuthor(event));

    msg.append(buildDescriptionMsgAddress(event));

    content.setValue(msg.toString());

    return content;
  }

  protected String getRepositoryName(NexusArtifactEvent event) {
    String repoId = event.getNexusItemInfo().getRepositoryId();

    try {
      Repository repository = getRepositoryRegistry().getRepository(repoId);

      return repository.getName();
    }
    catch (NoSuchRepositoryException e) {
      // that's fine, no need to yell, old timeline entries might correspond to long-time removed reposes
      return repoId;
    }
  }

  abstract protected String buildDescriptionMsgItem(NexusArtifactEvent event);

  protected String buildDescriptionMsgAction(NexusArtifactEvent event) {
    StringBuilder msg = new StringBuilder(" was ");

    if (NexusArtifactEvent.ACTION_CACHED.equals(event.getAction())) {
      msg.append("cached from remote URL ").append(event.getNexusItemInfo().getRemoteUrl()).append(".");
    }
    else if (NexusArtifactEvent.ACTION_DEPLOYED.equals(event.getAction())) {
      msg.append("deployed.");

    }
    else if (NexusArtifactEvent.ACTION_DELETED.equals(event.getAction())) {
      msg.append("deleted.");
    }
    else if (NexusArtifactEvent.ACTION_RETRIEVED.equals(event.getAction())) {
      msg.append("served downstream.");
    }
    else if (NexusArtifactEvent.ACTION_BROKEN.equals(event.getAction())) {
      msg.append("broken.");

      if (event.getMessage() != null) {
        msg.append(" Details: \n");

        msg.append(event.getMessage());

        msg.append("\n");
      }
    }
    else if (NexusArtifactEvent.ACTION_BROKEN_WRONG_REMOTE_CHECKSUM.equals(event.getAction())) {
      msg.append("proxied, and the remote repository contains wrong checksum for it.");

      if (event.getMessage() != null) {
        msg.append(" Details: \n");

        msg.append(event.getMessage());

        msg.append("\n");
      }
    }

    return msg.toString();
  }

  protected String buildDescriptionMsgAuthor(NexusArtifactEvent event) {
    final String author = buildAuthor(event);

    if (author != null) {
      return "Action was initiated by user \"" + author + "\".\n";

    }
    return "";
  }

  protected String buildDescriptionMsgAddress(NexusArtifactEvent event) {
    if (event.getEventContext().containsKey(AccessManager.REQUEST_REMOTE_ADDRESS)) {
      return "Request originated from IP address "
          + (String) event.getEventContext().get(AccessManager.REQUEST_REMOTE_ADDRESS) + ".\n";
    }
    return "";
  }

  protected Gav buildGAV(final NexusArtifactEvent event) {
    if (event.getNexusItemInfo() == null) {
      return null;
    }
    try {
      final Repository repo = getRepositoryRegistry().getRepository(event.getNexusItemInfo().getRepositoryId());

      if (MavenRepository.class.isAssignableFrom(repo.getClass())) {
        return ((MavenRepository) repo).getGavCalculator().pathToGav(event.getNexusItemInfo().getPath());
      }

      return null;
    }
    catch (NoSuchRepositoryException e) {
      log.debug(
          "Feed entry contained invalid repository id " + event.getNexusItemInfo().getRepositoryId(),
          e);

      return null;
    }
  }

  public boolean shouldBuildEntry(NexusArtifactEvent event) {
    return true;
  }
}
