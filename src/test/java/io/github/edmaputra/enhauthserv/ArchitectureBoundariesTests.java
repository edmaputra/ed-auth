package io.github.edmaputra.enhauthserv;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureBoundariesTests {

  @Test
  void application_modules_should_verify() {
    ApplicationModules.of(Application.class).verify();
  }
}
