package org.zanata.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.zanata.common.ContentType;
import org.zanata.common.LocaleId;
import org.zanata.model.Activity;
import org.zanata.model.HAccount;
import org.zanata.model.HAccountActivationKey;
import org.zanata.model.HAccountRole;
import org.zanata.model.HApplicationConfiguration;
import org.zanata.model.HDocument;
import org.zanata.model.HDocumentHistory;
import org.zanata.model.HGlossaryEntry;
import org.zanata.model.HGlossaryTerm;
import org.zanata.model.HIterationGroup;
import org.zanata.model.HLocale;
import org.zanata.model.HLocaleMember;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTermComment;
import org.zanata.model.HTextFlow;
import org.zanata.model.HTextFlowTarget;
import org.zanata.model.HTextFlowTargetHistory;
import org.zanata.model.po.HPoTargetHeader;
import org.zanata.model.security.HCredentials;
import org.zanata.model.tm.TransMemory;
import org.zanata.model.tm.TransMemoryUnit;
import org.zanata.model.tm.TransMemoryUnitVariant;
import com.github.huangp.entityunit.entity.Callbacks;
import com.github.huangp.entityunit.entity.EntityCleaner;
import com.github.huangp.entityunit.entity.EntityMaker;
import com.github.huangp.entityunit.entity.EntityMakerBuilder;
import com.github.huangp.entityunit.entity.FixIdCallback;
import com.github.huangp.entityunit.maker.FixedValueMaker;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Slf4j
@Name("sampleProjectProfile")
@Scope(ScopeType.APPLICATION)
@AutoCreate
@Transactional
public class SampleProjectProfile {

    @In
    private EntityManager entityManager;

    @In
    private EntityManagerFactory entityManagerFactory;

    private HLocale enUSLocale;

    private HPerson admin;

    public void deleteExceptEssentialData() {
        EntityCleaner.deleteAll(entityManager, Lists.<Class>newArrayList(
                TransMemoryUnitVariant.class, TransMemoryUnit.class,
                TransMemory.class,
                Activity.class,
                // glossary
                HTermComment.class, HGlossaryTerm.class, HGlossaryEntry.class,
                // tex flows and targets
                HPoTargetHeader.class, HTextFlowTargetHistory.class,
                HTextFlowTarget.class, HTextFlow.class,
                // documents
                HDocumentHistory.class, HDocument.class,
                // locales
                HLocaleMember.class, HLocale.class,
                // version group
                HIterationGroup.class,
                // project
                HProjectIteration.class, HProject.class,
                // account
                HAccountActivationKey.class, HCredentials.class, HPerson.class, HAccount.class));
        enUSLocale =
                forLocale(false, LocaleId.EN_US).makeAndPersist(entityManager,
                        HLocale.class);
        Query query = entityManager.createQuery(
                "update HApplicationConfiguration set value = '' where key = :key");
        query.setParameter("key", HApplicationConfiguration.KEY_HOME_CONTENT);
        query.executeUpdate();
        query.setParameter("key", HApplicationConfiguration.KEY_HELP_CONTENT);
        query.executeUpdate();

        purgeLuceneIndexes();
        // TODO probably should delete cache as well
    }

    private void purgeLuceneIndexes() {
        FullTextSession fullTextSession =
                Search.getFullTextSession((Session) entityManagerFactory
                        .createEntityManager().getDelegate());

        fullTextSession.purgeAll(HAccount.class);
        fullTextSession.purgeAll(HGlossaryEntry.class);
        fullTextSession.purgeAll(HGlossaryTerm.class);
        fullTextSession.purgeAll(HProject.class);
        fullTextSession.purgeAll(HProjectIteration.class);
        fullTextSession.purgeAll(TransMemoryUnit.class);
        fullTextSession.purgeAll(HTextFlowTarget.class);
    }

    public void makeSampleLanguages() {
        forLocale(true, LocaleId.FR).makeAndPersist(entityManager,
                HLocale.class);

        forLocale(true, new LocaleId("hi")).makeAndPersist(entityManager,
                HLocale.class);

        forLocale(true, new LocaleId("pl")).makeAndPersist(entityManager,
                HLocale.class);
    }

    private static EntityMaker forLocale(boolean enabledByDefault,
            LocaleId localeId) {
        return EntityMakerBuilder
                .builder()
                .addFieldOrPropertyMaker(HLocale.class, "active",
                        FixedValueMaker.ALWAYS_TRUE_MAKER)
                .addFieldOrPropertyMaker(HLocale.class, "enabledByDefault",
                        FixedValueMaker.fix(enabledByDefault))
                .addConstructorParameterMaker(HLocale.class, 0,
                        FixedValueMaker.fix(localeId)).build();
    }

    public void makeSampleUsers() {
        HAccount adminAccount =
                makeAccount("admin", "Eyox7xbNQ09MkIfRyH+rjg==",
                        "b6d7044e9ee3b2447c28fb7c50d86d98", 1L);
        assignAccountRole(1L, "admin");

        admin = makePerson(1L, "Administrator", "admin");
        assignAccountToPerson(adminAccount, admin);

        HAccount translatorAccount =
                makeAccount("translator", "Fr5JHlcaEqKLSHjnBm4gXg==",
                        "d83882201764f7d339e97c4b087f0806", 2L);
        assignAccountRole(2L, "user");
        HPerson translator = makePerson(2L, "translator", "translator");
        assignAccountToPerson(translatorAccount, translator);

        HAccount glossaristAccount =
                makeAccount("glossarist", "fRIeiPDPlSMtHbBNoqDjNQ==",
                        "b1e3daa18e41c0ce79829e87ce66b201", 3L);
        assignAccountRole(3L, "glossarist");
        HPerson glossarist = makePerson(3L, "glossarist", "glossarist");
        assignAccountToPerson(glossaristAccount, glossarist);

        HAccount gAdminAccount =
                makeAccount("glossaryadmin", "/W0YpteXk+WtymQ7H84kPQ==",
                        "5a6a34d28d39ff90ea47402311f339d4", 4L);
        assignAccountRole(4L, "glossary-admin");
        HPerson glossaryAdmin =
                makePerson(4L, "glossary-admin", "glossary-admin");
        assignAccountToPerson(gAdminAccount, glossaryAdmin);
    }

    private HPerson makePerson(Long wantedIdValue, String nameValue,
            String username) {
        return EntityMakerBuilder
                .builder()
                .addFieldOrPropertyMaker(HPerson.class, "name",
                        FixedValueMaker.fix(nameValue))
                .addFieldOrPropertyMaker(HPerson.class, "email",
                        FixedValueMaker.fix(username + "@example.com"))
                .build()
                .makeAndPersist(entityManager, HPerson.class,
                        new FixIdCallback(HPerson.class, wantedIdValue));
    }

    private void assignAccountToPerson(HAccount account, HPerson hPerson) {
        hPerson.setAccount(account);
    }

    private void assignAccountRole(Long accountId, String roleName) {
        entityManager.find(HAccount.class, accountId).getRoles()
                .add(getAccountRole(roleName));
    }

    private HAccountRole getAccountRole(String roleName) {
        log.debug("get account role with name {}", roleName);
        return entityManager.createQuery(
                String.format("from HAccountRole where name = '%s' ", roleName),
                HAccountRole.class)
                .getSingleResult();
    }

    public void addUsersToLanguage(HPerson person, Iterable<HLocale> locales) {
        for (HLocale locale : locales) {
            entityManager.persist(asLocaleMember(person, locale));
        }
    }

    private static HLocaleMember asLocaleMember(HPerson person, HLocale locale) {
        return new HLocaleMember(person, locale, true, false, false);
    }

    private HAccount makeAccount(String username, String passwordHash,
            String apiKey, Long accountId) {
        EntityMaker maker = EntityMakerBuilder
                        .builder()
                        // .includeOptionalOneToOne()
                        .addFieldOrPropertyMaker(HAccount.class, "enabled",
                                FixedValueMaker.ALWAYS_TRUE_MAKER)
                        .addFieldOrPropertyMaker(HAccount.class, "username",
                                FixedValueMaker.fix(username))
                        .addFieldOrPropertyMaker(HAccount.class,
                                "passwordHash",
                                FixedValueMaker.fix(passwordHash))
                        .addFieldOrPropertyMaker(HAccount.class, "apiKey",
                                FixedValueMaker.fix(apiKey)).build();
        return maker.makeAndPersist(entityManager, HAccount.class,
                new FixIdCallback(HAccount.class, accountId));
    }

    public void makeSampleProject() {
        EntityMaker maker =
                EntityMakerBuilder
                        .builder()
                        // project
                        .addFieldOrPropertyMaker(HProject.class, "slug",
                                FixedValueMaker.fix("about-fedora"))
                        .addFieldOrPropertyMaker(HProject.class, "name",
                                FixedValueMaker.fix("about fedora"))
                        // iteration
                        .addFieldOrPropertyMaker(HProjectIteration.class,
                                "slug", FixedValueMaker.fix("master"))
                        // document
                        // public HDocument(String docId, String name, String
                        // path,
                        // ContentType contentType, HLocale locale)
                        .addConstructorParameterMaker(HDocument.class, 0,
                                FixedValueMaker.fix("About_Fedora"))
                        .addConstructorParameterMaker(HDocument.class, 1,
                                FixedValueMaker.fix("About_Fedora"))
                        .addConstructorParameterMaker(HDocument.class, 2,
                                FixedValueMaker.EMPTY_STRING_MAKER)
                        .addConstructorParameterMaker(HDocument.class, 3,
                                FixedValueMaker.fix(ContentType.PO))
                        .reuseEntity(enUSLocale).build();

        maker.makeAndPersist(entityManager, HTextFlowTarget.class,
                Callbacks.wireManyToMany(HProject.class, admin));

    }

}
