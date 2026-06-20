package io.github.edmaputra.enhauthserv;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "io.github.edmaputra.enhauthserv",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureBoundariesTests {

  @ArchTest
  static final ArchRule domain_must_not_depend_on_frameworks =
      noClasses()
          .that().resideInAnyPackage("..domain..")
          .should().dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.servlet..",
              "jakarta.persistence..",
              "org.hibernate..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule application_must_not_depend_on_legacy_adapters_or_frameworks =
      noClasses()
          .that().resideInAnyPackage("..application..")
          .should().dependOnClassesThat()
          .resideInAnyPackage(
              "..adapter..",
              "..controller..",
              "..repository..",
              "..entity..",
              "org.springframework..",
              "jakarta.servlet..",
              "jakarta.persistence..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule domain_must_not_depend_on_application_or_adapter =
      noClasses()
          .that().resideInAnyPackage("..domain..")
          .should().dependOnClassesThat()
          .resideInAnyPackage("..application..", "..adapter..")
          .allowEmptyShould(true);
}
