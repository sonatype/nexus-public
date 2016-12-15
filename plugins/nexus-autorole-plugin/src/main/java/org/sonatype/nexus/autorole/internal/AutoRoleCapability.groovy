package org.sonatype.nexus.autorole.internal

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.goodies.i18n.I18N
import org.sonatype.goodies.i18n.MessageBundle
import org.sonatype.goodies.i18n.MessageBundle.DefaultMessage
import org.sonatype.nexus.capability.CapabilityConfigurationSupport
import org.sonatype.nexus.capability.CapabilityDescriptorSupport
import org.sonatype.nexus.capability.CapabilitySupport
import org.sonatype.nexus.capability.CapabilityType
import org.sonatype.nexus.capability.Capability
import org.sonatype.nexus.capability.Condition
import org.sonatype.nexus.formfields.ComboboxFormField
import org.sonatype.nexus.formfields.FormField
import org.sonatype.nexus.security.realm.RealmManager

import groovy.transform.PackageScope
import groovy.transform.ToString

import static org.sonatype.nexus.capability.CapabilityType.capabilityType

/**
 * Automatic role {@link Capability}.
 *
 * @since 3.2
 */
@Named(AutoRoleCapability.TYPE_ID)
class AutoRoleCapability
  extends CapabilitySupport<Configuration> {
  public static final String TYPE_ID = 'autorole'

  public static final CapabilityType TYPE = capabilityType(TYPE_ID)

  private static interface Messages
    extends MessageBundle
  {
    @DefaultMessage('Automatic Role')
    String name()

    @DefaultMessage('Role')
    String roleLabel()

    @DefaultMessage('The role which is automatically granted to authenticated users')
    String roleHelp()

    @DefaultMessage('%s')
    String description(String role)
  }

  @PackageScope
  static final Messages messages = I18N.create(Messages.class)

  @Inject
  RealmManager realmManager

  @Inject
  AutoRoleRealm realm

  @Override
  protected Configuration createConfig(final Map<String, String> properties) {
    return new Configuration(properties)
  }

  @Override
  protected String renderDescription() {
    return messages.description(config.role)
  }

  @Override
  Condition activationCondition() {
    return conditions().capabilities().passivateCapabilityDuringUpdate()
  }

  @Override
  protected void onActivate(final Configuration config) {
    realm.role = config.role

    // install realm if needed
    realmManager.enableRealm(AutoRoleRealm.NAME)
  }

  @Override
  protected void onPassivate(final Configuration config) {
    realm.role = null
  }

  //
  // Configuration
  //

  private static final String P_ROLE = 'role'

  @ToString(includePackage = false, includeNames = true)
  static class Configuration
    extends CapabilityConfigurationSupport
  {
    String role

    Configuration(final Map<String, String> properties) {
      role = parseUri(properties[P_ROLE])
    }
  }

  //
  // Descriptor
  //

  @Named(AutoRoleCapability.TYPE_ID)
  @Singleton
  static class Descriptor
    extends CapabilityDescriptorSupport
  {
    private final FormField role

    Descriptor() {
      this.exposed = true
      this.hidden = false

      this.role = new ComboboxFormField<String>(
        P_ROLE,
        messages.roleLabel(),
        messages.roleHelp(),
        FormField.MANDATORY
      ).withStoreApi('coreui_Role.read')
    }

    @Override
    CapabilityType type() {
      return TYPE
    }

    @Override
    String name() {
      return messages.name()
    }

    @Override
    List<FormField> formFields() {
      return [role]
    }

    @Override
    protected String renderAbout() {
      return render("$TYPE_ID-about.vm")
    }
  }
}
