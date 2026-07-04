package com.tenpo.challenge;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private static final String BASE = "com.tenpo.challenge";
  private static JavaClasses importedClasses;

  @BeforeAll
  static void importClasses() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);
  }

  @Test
  void domainMustNotDependOnFrameworks() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..",
            "java.sql..")
        .check(importedClasses);
  }

  @Test
  void applicationMustNotDependOnWebOrPersistence() {
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework.web..",
            "jakarta.persistence..",
            "org.hibernate..",
            "jakarta.servlet..")
        .check(importedClasses);
  }

  @Test
  void apiMustNotDependOnPersistence() {
    noClasses()
        .that()
        .resideInAPackage("..api..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..persistence..")
        .check(importedClasses);
  }

  @Test
  void portsMustBeInterfaces() {
    classes()
        .that()
        .resideInAPackage("..port.in..")
        .or()
        .resideInAPackage("..port.out..")
        .should()
        .beInterfaces()
        .check(importedClasses);
  }

  @Test
  void useCaseImplementationsMustResideInServicePackage() {
    classes()
        .that()
        .haveSimpleNameEndingWith("Service")
        .and()
        .resideInAPackage("..application..")
        .should()
        .resideInAPackage("..application.service..")
        .check(importedClasses);
  }

  @Test
  void packageDependenciesMustNotFormCycles() {
    slices()
        .matching("com.tenpo.challenge.(*)..")
        .should()
        .beFreeOfCycles()
        .check(importedClasses);
  }
}
