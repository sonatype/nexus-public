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
package org.sonatype.nexus.testsuite.capabilities;

import java.util.Collection;

import org.sonatype.nexus.capabilities.client.Capability;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.testsuite.capabilities.client.CapabilityA;
import org.sonatype.nexus.testsuite.capabilities.client.CapabilityB;
import org.sonatype.sisu.siesta.common.validation.ValidationErrorsException;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.capabilities.client.Filter.capabilitiesThat;

public class CapabilitiesIT
    extends CapabilitiesITSupport
{

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public CapabilitiesIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void crudTypedA() {
    // create
    final CapabilityA created = capabilities().create(CapabilityA.class)
        .withNotes("Some notes")
        .withPropertyA1("foo")
        .save();

    assertThat(created.id(), is(notNullValue()));
    assertThat(created.notes(), is("Some notes"));
    assertThat(created.property("a1"), is("foo"));
    assertThat(created.propertyA1(), is("foo"));

    // read
    final CapabilityA read = capabilities().get(CapabilityA.class, created.id());

    assertThat(read.id(), is(created.id()));
    assertThat(read.notes(), is(created.notes()));
    assertThat(read.type(), is(created.type()));
    assertThat(read.properties(), is(created.properties()));
    assertThat(read.propertyA1(), is(created.propertyA1()));

    // update
    read.withNotes("Some other notes").save();

    final CapabilityA updated = capabilities().get(CapabilityA.class, created.id());

    assertThat(updated.notes(), is("Some other notes"));
    assertThat(created.refresh().notes(), is("Some other notes"));

    // delete
    read.remove();

    thrown.expect(UniformInterfaceException.class);
    thrown.expectMessage(String.format("Capability with id '%s' was not found", created.id()));
    capabilities().get(CapabilityA.class, created.id());
  }

  @Test
  public void crudTypedB() {
    // create
    final CapabilityB created = capabilities().create(CapabilityB.class)
        .withNotes("Some notes")
        .withPropertyB1("foo")
        .save();

    assertThat(created.id(), is(notNullValue()));
    assertThat(created.notes(), is("Some notes"));
    assertThat(created.property("b1"), is("foo"));
    assertThat(created.propertyB1(), is("foo"));

    // read
    final CapabilityB read = capabilities().get(CapabilityB.class, created.id());

    assertThat(read.id(), is(created.id()));
    assertThat(read.notes(), is(created.notes()));
    assertThat(read.type(), is(created.type()));
    assertThat(read.properties(), is(created.properties()));
    assertThat(read.propertyB1(), is(created.propertyB1()));

    // update
    read.withNotes("Some other notes").save();

    final CapabilityB updated = capabilities().get(CapabilityB.class, created.id());

    assertThat(updated.notes(), is("Some other notes"));
    assertThat(created.refresh().notes(), is("Some other notes"));

    // delete
    read.remove();

    thrown.expect(UniformInterfaceException.class);
    thrown.expectMessage(String.format("Capability with id '%s' was not found", created.id()));
    capabilities().get(CapabilityB.class, created.id());
  }

  @Test
  public void enableAndDisableTypedA() {
    final CapabilityA created = capabilities().create(CapabilityA.class)
        .withNotes("Some notes")
        .withPropertyA1("foo")
        .save();

    final CapabilityA read = capabilities().get(CapabilityA.class, created.id());

    assertThat(read.isEnabled(), is(true));

    created.disable();
    read.refresh();

    assertThat(read.isEnabled(), is(false));

    created.enable();
    read.refresh();

    assertThat(read.isEnabled(), is(true));
  }

  @Test
  public void enableAndDisableTypedB() {
    final CapabilityB created = capabilities().create(CapabilityB.class)
        .withNotes("Some notes")
        .withPropertyB1("foo")
        .save();

    final CapabilityB read = capabilities().get(CapabilityB.class, created.id());

    assertThat(read.isEnabled(), is(true));

    created.disable();
    read.refresh();

    assertThat(read.isEnabled(), is(false));

    created.enable();
    read.refresh();

    assertThat(read.isEnabled(), is(true));
  }

  @Test
  public void getInexistentTypedA() {
    thrown.expect(UniformInterfaceException.class);
    thrown.expectMessage("Capability with id 'getInexistent' was not found");
    capabilities().get(CapabilityA.class, "getInexistent");
  }

  @Test
  public void getInexistentTypedB() {
    thrown.expect(UniformInterfaceException.class);
    thrown.expectMessage("Capability with id 'getInexistent' was not found");
    capabilities().get(CapabilityB.class, "getInexistent");
  }

  @Test
  public void updateInexistentTypedA() {
    final CapabilityA created = capabilities().create(CapabilityA.class)
        .withNotes("Some notes")
        .withPropertyA1("foo")
        .save();

    final CapabilityA read = capabilities().get(CapabilityA.class, created.id());
    created.remove();

    thrown.expect(UniformInterfaceException.class);
    thrown.expectMessage(String.format("Capability with id '%s' was not found", created.id()));
    read.save();
  }

  @Test
  public void updateInexistentTypedB() {
    final CapabilityB created = capabilities().create(CapabilityB.class)
        .withNotes("Some notes")
        .withPropertyB1("foo")
        .save();

    final CapabilityB read = capabilities().get(CapabilityB.class, created.id());
    created.remove();

    thrown.expect(UniformInterfaceException.class);
    thrown.expectMessage(String.format("Capability with id '%s' was not found", created.id()));
    read.save();
  }

  @Test
  public void deleteInexistentTypedA() {
    final CapabilityA created = capabilities().create(CapabilityA.class)
        .withNotes("Some notes")
        .withPropertyA1("foo")
        .save();

    final CapabilityA read = capabilities().get(CapabilityA.class, created.id());
    created.remove();

    thrown.expect(UniformInterfaceException.class);
    thrown.expectMessage(String.format("Capability with id '%s' was not found", created.id()));
    read.remove();
  }

  @Test
  public void deleteInexistentTypedB() {
    final CapabilityB created = capabilities().create(CapabilityB.class)
        .withNotes("Some notes")
        .withPropertyB1("foo")
        .save();

    final CapabilityB read = capabilities().get(CapabilityB.class, created.id());
    created.remove();

    thrown.expect(UniformInterfaceException.class);
    thrown.expectMessage(String.format("Capability with id '%s' was not found", created.id()));
    read.remove();
  }

  @Test
  public void getThemAll() {
    capabilities().create(CapabilityA.class)
        .withNotes("Some notes")
        .withPropertyA1("foo")
        .save();

    capabilities().create(CapabilityB.class)
        .withNotes("Some notes")
        .withPropertyB1("foo")
        .save();

    final Collection<Capability> capabilities = capabilities().get();

    assertThat(capabilities, is(notNullValue()));
    assertThat(capabilities, hasSize(greaterThan(0)));
  }

  @Test
  public void filterByType() {
    capabilities().create(CapabilityA.class)
        .withNotes("Some notes")
        .withPropertyA1("foo")
        .save();

    capabilities().create(CapabilityB.class)
        .withNotes("Some notes")
        .withPropertyB1("foo")
        .save();

    final Collection<Capability> capabilities = capabilities().get(
        capabilitiesThat().haveType("[a]")
    );

    assertThat(capabilities, is(notNullValue()));
    assertThat(capabilities, hasSize(greaterThan(0)));
    for (Capability capability : capabilities) {
      MatcherAssert.assertThat(capability, instanceOf(CapabilityA.class));
    }
  }

  @Test
  public void filterByTypeTyped() {
    capabilities().create(CapabilityA.class)
        .withNotes("Some notes")
        .withPropertyA1("foo")
        .save();

    capabilities().create(CapabilityB.class)
        .withNotes("Some notes")
        .withPropertyB1("foo")
        .save();

    final Collection<CapabilityA> capabilities = capabilities().get(
        CapabilityA.class,
        capabilitiesThat().haveType("[a]")
    );

    assertThat(capabilities, is(notNullValue()));
    assertThat(capabilities, hasSize(greaterThan(0)));
  }

  @Test
  public void filterByTypeAndProperty() {
    capabilities().create(CapabilityA.class)
        .withNotes("Some notes")
        .withPropertyA1("foo")
        .save();
    capabilities().create(CapabilityA.class)
        .withNotes("Some notes")
        .withPropertyA1("bar")
        .save();

    final Collection<Capability> capabilities = capabilities().get(
        capabilitiesThat().haveType("[a]").haveProperty("a1", "bar")
    );

    assertThat(capabilities, is(notNullValue()));
    assertThat(capabilities, hasSize(greaterThan(0)));
    for (Capability capability : capabilities) {
      MatcherAssert.assertThat(capability, instanceOf(CapabilityA.class));
      MatcherAssert.assertThat(capability.property("a1"), is("bar"));
    }
  }

  /**
   * Verify that create will fail if a mandatory property is not set.
   */
  @Test
  public void failIfMandatoryPropertyNotPresent() {
    thrown.expect(ValidationErrorsException.class);
    thrown.expectMessage("Repository/Group is required");
    capabilities().create("[message]")
        .save();
  }

  /**
   * Verify that create will fail if a property validated via regexp (in this case XYZ.*) does not match.
   */
  @Test
  public void failIfNotMatchingRegexp() {
    thrown.expect(ValidationErrorsException.class);
    thrown.expectMessage("Message does not match 'XYZ.*'");
    capabilities().create("[message]")
        .withProperty("repository", "releases")
        .withProperty("message", "Blah")
        .save();
  }

  /**
   * Verify that create will fail if a property validated via regexp (in this case XYZ.*) does not match.
   */
  @Test
  public void doNotFailIfMatchingRegexp() {
    capabilities().create("[message]")
        .withProperty("repository", "releases")
        .withProperty("message", "XYZ Blah")
        .save();
  }

  /**
   * Verify that in case of an activation failure the flag is set and description contains details.
   *
   * @since 2.4
   */
  @Test
  public void checkErrorOnActivation() {
    final Capability capability = capabilities().create("[withActivationError]").save();
    MatcherAssert.assertThat(capability.hasErrors(), is(true));
    MatcherAssert.assertThat(capability.isActive(), is(false));
    MatcherAssert.assertThat(capability.stateDescription(),
        containsString("This capability always fails on activate"));
  }

  /**
   * Verify that capability is initially active when created for a repository that is in service
   * Verify that capability becomes inactive when repository is put out of service
   * Verify that capability becomes active when repository is put back in service
   */
  @Test
  public void repositoryInService() {
    final String rId = repositoryIdForTest();

    final MavenHostedRepository repository = repositories().create(MavenHostedRepository.class, rId)
        .excludeFromSearchResults()
        .save();

    Capability capability = capabilities().create("[repositoryIsInService]")
        .withProperty("repository", rId)
        .save();
    MatcherAssert.assertThat(capability.isActive(), is(true));

    logRemote("Put repository '{}' out of service", rId);
    repository.putOutOfService();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(false));

    logRemote("Put repository '{}' back in service", rId);
    repository.putInService();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(true));
  }

  /**
   * Verify that capability is initially inactive when created for a repository that is out of service
   * Verify that capability becomes active when repository is put back in service
   */
  @Test
  public void repositoryOutOfService() {
    final String rId = repositoryIdForTest();

    final MavenHostedRepository repository = repositories().create(MavenHostedRepository.class, rId)
        .excludeFromSearchResults()
        .save()
        .putOutOfService();

    Capability capability = capabilities().create("[repositoryIsInService]")
        .withProperty("repository", rId)
        .save();
    MatcherAssert.assertThat(capability.isActive(), is(false));

    logRemote("Put repository '{}' back in service", rId);
    repository.putInService();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(true));
  }

  /**
   * Verify that capability becomes inactive when repository is changed and the new repository is out of service
   */
  @Test
  public void changeRepositoryToAnOutOfServiceOne() {
    final String rIdActive = repositoryIdForTest("active");
    final String rIdInactive = repositoryIdForTest("inactive");

    repositories().create(MavenHostedRepository.class, rIdActive)
        .excludeFromSearchResults()
        .save();
    repositories().create(MavenHostedRepository.class, rIdInactive)
        .excludeFromSearchResults()
        .save().putOutOfService();

    Capability capability = capabilities().create("[repositoryIsInService]")
        .withProperty("repository", rIdActive)
        .save();
    MatcherAssert.assertThat(capability.isActive(), is(true));

    logRemote("Change capability to use repository '{}'", rIdInactive);
    capability.withProperty("repository", rIdInactive).save();
    MatcherAssert.assertThat(capability.isActive(), is(false));
  }

  /**
   * Verify that capability is initially active when created for a repository that is not blocked
   * Verify that capability becomes inactive when repository is manually blocked
   * Verify that capability becomes active when repository is unblocked
   */
  @Test
  public void repositoryNotBlocked() {
    final String rId = repositoryIdForTest();

    final MavenProxyRepository repository = repositories().create(MavenProxyRepository.class, rId)
        .asProxyOf(repositories().get("releases").contentUri())
        .save();

    Capability capability = capabilities().create("[repositoryIsNotBlocked]")
        .withProperty("repository", rId)
        .save();
    MatcherAssert.assertThat(capability.isActive(), is(true));

    logRemote("Block repository '{}'", rId);
    repository.block();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(false));

    logRemote("Unblock repository '{}'", rId);
    repository.unblock();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(true));
  }

  /**
   * Verify that capability is initially inactive when created for a repository that is blocked
   * Verify that capability becomes active when repository is unblocked
   */
  @Test
  public void repositoryBlocked() {
    final String rId = repositoryIdForTest();

    final MavenProxyRepository repository = repositories().create(MavenProxyRepository.class, rId)
        .asProxyOf(repositories().get("releases").contentUri())
        .save()
        .block();

    Capability capability = capabilities().create("[repositoryIsNotBlocked]")
        .withProperty("repository", rId)
        .save();
    MatcherAssert.assertThat(capability.isActive(), is(false));

    logRemote("Unblock repository '{}'", rId);
    repository.unblock();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(true));
  }

  /**
   * Verify that capability becomes inactive when repository is changed and the new repository is blocked
   */
  @Test
  public void changeRepositoryToABlockedOne() {
    final String rIdNotBlocked = repositoryIdForTest("notBlocked");
    final String rIdBlocked = repositoryIdForTest("blocked");

    repositories().create(MavenProxyRepository.class, rIdNotBlocked)
        .asProxyOf(repositories().get("releases").contentUri())
        .save();
    repositories().create(MavenProxyRepository.class, rIdBlocked)
        .asProxyOf(repositories().get("releases").contentUri())
        .save()
        .block();

    Capability capability = capabilities().create("[repositoryIsNotBlocked]")
        .withProperty("repository", rIdNotBlocked)
        .save();
    MatcherAssert.assertThat(capability.isActive(), is(true));

    logRemote("Change capability to use repository '{}'", rIdBlocked);
    capability.withProperty("repository", rIdBlocked).save();
    MatcherAssert.assertThat(capability.isActive(), is(false));
  }

  /**
   * Verify that a capability is automatically removed when configured repository is removed.
   */
  @Test
  public void capabilityRemovedWhenRepositoryRemoved() {
    final String rId = repositoryIdForTest();

    final MavenHostedRepository repository = repositories().create(MavenHostedRepository.class, rId)
        .excludeFromSearchResults()
        .save()
        .putOutOfService();

    Capability capability = capabilities().create("[repositoryIsInService]")
        .withProperty("repository", rId)
        .save();
    MatcherAssert.assertThat(capability.isActive(), is(false));

    logRemote("Remove repository '{}'", rId);
    repository.remove();

    thrown.expect(UniformInterfaceException.class);
    thrown.expectMessage(String.format("Capability with id '%s' was not found", capability.id()));
    capability.refresh();
  }

  /**
   * Verify that a capability, that has an activation condition that another capability of a specific type exists,
   * becomes active/inactive depending on existence of that another capability.
   */
  @Test
  public void capabilityOfTypeExists() {
    removeAllMessageCapabilities();

    Capability capability = capabilities().create("[capabilityOfTypeExists]")
        .save();
    MatcherAssert.assertThat(capability.isActive(), is(false));

    logRemote("Create a capability of type [message]");
    final Capability messageCapability = capabilities().create("[message]")
        .withProperty("repository", "releases")
        .save();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(true));

    logRemote("Remove capability of type [message]");
    messageCapability.remove();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(false));
  }

  @Test
  public void capabilityOfTypeIsActive() {
    removeAllMessageCapabilities();

    Capability capability = capabilities().create("[capabilityOfTypeActive]")
        .save();
    MatcherAssert.assertThat(capability.isActive(), is(false));

    logRemote("Create a capability of type [message]");
    final Capability messageCapability = capabilities().create("[message]")
        .withProperty("repository", "releases")
        .save();

    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(true));

    logRemote("Disable capability of type [message]");
    messageCapability.disable();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(false));

    logRemote("Enable capability of type [message]");
    messageCapability.enable();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(true));

    logRemote("Remove capability of type [message]");
    messageCapability.remove();
    capability.refresh();
    MatcherAssert.assertThat(capability.isActive(), is(false));
  }

  /**
   * Verify uri validator accepts a valid URI.
   *
   * @since 2.7
   */
  @Test
  public void uriIsValid() {
    final Capability capability = capabilities().create("[values]")
        .withProperty("uri", "http://localhost")
        .save();
    assertThat(capability.isActive(), is(true));
  }

  /**
   * Verify uri validator rejects and invalid URI.
   *
   * @since 2.7
   */
  @Test
  public void uriIsInvalid() {
    thrown.expect(ValidationErrorsException.class);
    thrown.expectMessage("Some URI is not a valid URI");
    capabilities().create("[values]")
        .withProperty("uri", "foo is not valid")
        .save();
  }

  /**
   * Verify url validator accepts a valid URL.
   *
   * @since 2.7
   */
  @Test
  public void urlIsValid() {
    final Capability capability = capabilities().create("[values]")
        .withProperty("url", "http://localhost")
        .save();
    assertThat(capability.isActive(), is(true));
  }

  /**
   * Verify url validator rejects and invalid URL.
   *
   * @since 2.7
   */
  @Test
  public void urlIsInvalid() {
    thrown.expect(ValidationErrorsException.class);
    thrown.expectMessage("Some URL is not a valid URL");
    capabilities().create("[values]")
        .withProperty("url", "foo is not valid")
        .save();
  }

}
