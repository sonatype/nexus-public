package org.sonatype.nexus.groovyremote

// here for IDE purposes only

// script must return a single closure
return { name ->
  println "${Thread.currentThread().name} -> sup $name"
  return 12345
}