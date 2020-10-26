package com.virtuslab.archunit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.Test;

public class ForbiddenClassesTestSuite extends BaseArchUnitTestSuite {

  @Test
  public void no_classes_should_depend_on_java_collections() {
    noClasses()
        .that().resideOutsideOfPackages(
            "com.virtuslab.binding",
            "com.virtuslab.gitmachete.frontend.actions..",
            "com.virtuslab.gitmachete.frontend.externalsystem..",
            "com.virtuslab.gitmachete.frontend.file..",
            "com.virtuslab..impl..")
        .should()
        .dependOnClassesThat().haveNameMatching("java\\.util\\.(Deque|HashMap|List|Map|Queue|Set|Stack|TreeMap|Vector)")
        .because("immutable Vavr counterparts should be used instead of mutable Java collections")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_depend_on_java_util_Date() {
    // GitCoreReflogEntry converts a Date coming from JGit to an Instant, hence the exception
    noClasses()
        .that().areNotAssignableTo(com.virtuslab.gitcore.impl.jgit.GitCoreReflogEntry.class)
        .should()
        .dependOnClassesThat().areAssignableTo(java.util.Date.class)
        .because("Date is unsafe and deprecated; ZonedDateTime or Instant should be used instead")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_depend_on_java_util_Optional() {
    noClasses()
        .should()
        .dependOnClassesThat().areAssignableTo(java.util.Optional.class)
        .because("io.vavr.control.Option should be used instead")
        .check(importedClasses);
  }

  @Test
  public void no_classes_should_depend_on_java_time_LocalDateTime() {
    noClasses()
        .should()
        .dependOnClassesThat().areAssignableTo(java.time.LocalDateTime.class)
        .because("local date-times are inherently unsafe " +
            "since they give an impression of referring to a specific instant " +
            "while in fact, they do not. Use ZonedDateTime or Instant instead")
        .check(importedClasses);
  }
}
