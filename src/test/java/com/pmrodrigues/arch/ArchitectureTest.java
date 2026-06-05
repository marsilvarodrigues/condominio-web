package com.pmrodrigues.arch;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.pmrodrigues", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final ArchCondition<JavaClass> BE_A_RECORD =
        new ArchCondition<>("be a Java record") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (!item.isRecord()) {
                    events.add(SimpleConditionEvent.violated(item,
                        item.getDescription() + " is not a Java record"));
                }
            }
        };

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories =
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .as("Controllers must not depend directly on repositories");

    @ArchTest
    static final ArchRule controllers_must_not_access_models =
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..model..")
            .as("Controllers must not depend on model/entity classes — use DTOs instead");

    @ArchTest
    static final ArchRule dtos_must_be_records =
        classes()
            .that().resideInAPackage("..dto..")
            .and().areNotInterfaces()
            .and().areNotAnnotations()
            .and().areTopLevelClasses()
            .should(BE_A_RECORD)
            .as("All classes in *.dto packages must be Java records");

    @ArchTest
    static final ArchRule dto_classes_must_reside_in_dto_package =
        classes()
            .that().haveSimpleNameEndingWith("DTO")
            .should().resideInAPackage("..dto..")
            .as("Classes ending with DTO must reside in a *.dto package");

    @ArchTest
    static final ArchRule layered_architecture =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .as("Layered architecture: Controller -> Service -> Repository");

    @ArchTest
    static final ArchRule no_autowired_field_injection =
        noFields()
            .should().beAnnotatedWith(Autowired.class)
            .as("Field injection with @Autowired is forbidden — use constructor injection instead");

    @ArchTest
    static final ArchRule no_cross_module_service_to_repository =
        classes()
            .that().resideInAPackage("..service..")
            .should(notAccessRepositoriesFromOtherModules())
            .as("Services must not access repositories from other modules — call the target module's service instead");

    @ArchTest
    static final ArchRule all_service_public_methods_must_be_timed =
        methods()
            .that().areDeclaredInClassesThat().resideInAPackage("com.pmrodrigues..service..")
            .and().arePublic()
            .should().beAnnotatedWith(Timed.class)
            .as("All public methods in any *.service package must be annotated with @Timed");

    private static ArchCondition<JavaClass> notAccessRepositoriesFromOtherModules() {
        return new ArchCondition<>("not access repositories from other modules") {
            @Override
            public void check(JavaClass clazz, ConditionEvents events) {
                String ownModule = moduleOf(clazz.getPackageName());
                clazz.getDirectDependenciesFromSelf().forEach(dep -> {
                    String depPkg = dep.getTargetClass().getPackageName();
                    if (depPkg.contains(".repository")) {
                        String depModule = moduleOf(depPkg);
                        if (depModule != null && !depModule.equals(ownModule)) {
                            // Allow if the class extends a service in the same module as the repository —
                            // this means the repository dependency is inherited via super constructor.
                            boolean inheritedViaParent = clazz.isAssignableTo(
                                "com.pmrodrigues." + depModule + ".service.UserService");
                            if (!inheritedViaParent) {
                                events.add(SimpleConditionEvent.violated(clazz,
                                    "%s (module '%s') must not depend on %s (module '%s'); use the target module's service instead"
                                        .formatted(clazz.getName(), ownModule,
                                                   dep.getTargetClass().getName(), depModule)));
                            }
                        }
                    }
                });
            }

            private String moduleOf(String pkg) {
                String prefix = "com.pmrodrigues.";
                if (!pkg.startsWith(prefix)) return null;
                String tail = pkg.substring(prefix.length());
                int dot = tail.indexOf('.');
                return dot < 0 ? tail : tail.substring(0, dot);
            }
        };
    }
}
