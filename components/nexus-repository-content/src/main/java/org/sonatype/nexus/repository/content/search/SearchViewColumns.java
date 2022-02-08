package org.sonatype.nexus.repository.content.search;

/**
 * Enum representing column names for {@link SearchDAO}
 *
 * @since 3.next
 */
public enum SearchViewColumns
{
  COMPONENT_ID("componentId"),
  NAMESPACE("namespace"),
  COMPONENT_NAME("componentName"),
  VERSION("version"),
  REPOSITORY_NAME("repositoryName");

  private final String name;

  SearchViewColumns(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
