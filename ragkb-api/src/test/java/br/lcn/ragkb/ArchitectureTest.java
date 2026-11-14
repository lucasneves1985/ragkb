package br.lcn.ragkb;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {

    private static final String ASSISTANT = "br.lcn.ragkb.assistant..";
    private static final String SHARED = "br.lcn.ragkb.shared..";
    private static final JavaClasses CLASSES = new ClassFileImporter().importPackages("br.lcn.ragkb");

    @Test
    void assistantIsNeverADependencyOfAnotherModule() {
        ArchRule rule = noClasses().that().resideOutsideOfPackage(ASSISTANT)
                .should().dependOnClassesThat().resideInAPackage(ASSISTANT);
        rule.check(CLASSES);
    }

    @Test
    void sharedDoesNotDependOnDomainModules() {
        ArchRule rule = noClasses().that().resideInAPackage(SHARED)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "br.lcn.ragkb.assistant..", "br.lcn.ragkb.businessrule..", "br.lcn.ragkb.card..",
                        "br.lcn.ragkb.certificate..", "br.lcn.ragkb.conversation..", "br.lcn.ragkb.evaluation..",
                        "br.lcn.ragkb.identity..", "br.lcn.ragkb.integration..", "br.lcn.ragkb.knowledge..",
                        "br.lcn.ragkb.project..", "br.lcn.ragkb.reminder..", "br.lcn.ragkb.ticket..",
                        "br.lcn.ragkb.whatsapp..");
        rule.check(CLASSES);
    }

    @Test
    void conversationDoesNotDependOnAssistant() {
        noClasses().that().resideInAPackage("br.lcn.ragkb.conversation..")
                .should().dependOnClassesThat().resideInAPackage(ASSISTANT).check(CLASSES);
    }

    @Test
    void cardDoesNotDependOnForbiddenModules() {
        noClasses().that().resideInAPackage("br.lcn.ragkb.card..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "br.lcn.ragkb.knowledge..", "br.lcn.ragkb.conversation..", "br.lcn.ragkb.integration..",
                        "br.lcn.ragkb.reminder..", "br.lcn.ragkb.ticket..")
                .check(CLASSES);
    }

    @Test
    void modulesHaveNoCycles() {
        slices().matching("br.lcn.ragkb.(*)..").should().beFreeOfCycles().check(CLASSES);
    }
}
