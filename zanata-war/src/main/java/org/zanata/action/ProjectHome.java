/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.faces.event.ValueChangeEvent;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.NaturalIdentifier;
import org.hibernate.criterion.Restrictions;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.common.EntityStatus;
import org.zanata.common.ProjectType;
import org.zanata.dao.AccountRoleDAO;
import org.zanata.dao.PersonDAO;
import org.zanata.model.HAccount;
import org.zanata.model.HAccountRole;
import org.zanata.model.HLocale;
import org.zanata.model.HPerson;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.seam.scope.FlashScopeBean;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.LocaleService;
import org.zanata.service.SlugEntityService;
import org.zanata.service.ValidationService;
import org.zanata.service.impl.LocaleServiceImpl;
import org.zanata.util.ZanataMessages;
import org.zanata.webtrans.shared.model.ValidationAction;
import org.zanata.webtrans.shared.model.ValidationId;
import org.zanata.webtrans.shared.validation.ValidationFactory;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.Setter;

@Name("projectHome")
public class ProjectHome extends SlugHome<HProject> {
    private static final long serialVersionUID = 1L;

    public static final String PROJECT_UPDATE = "project.update";

    @Getter
    @Setter
    private String slug;

    @In
    ZanataIdentity identity;

    @In(required = false, value = JpaIdentityStore.AUTHENTICATED_USER)
    HAccount authenticatedAccount;

    /* Outjected from LocaleListAction */
    @In(required = false)
    Map<String, String> customizedItems;

    /* Outjected from LocaleListAction */
    @In(required = false)
    private Boolean overrideLocales;

    @In
    private LocaleService localeServiceImpl;

    @In
    private SlugEntityService slugEntityServiceImpl;

    @In
    private FlashScopeBean flashScope;

    @In
    private EntityManager entityManager;

    @In
    private ZanataMessages zanataMessages;

    @In
    private AccountRoleDAO accountRoleDAO;

    @In
    private ValidationService validationServiceImpl;

    @In
    private CopyTransOptionsModel copyTransOptionsModel;

    private Map<String, Boolean> roleRestrictions;

    private Map<ValidationId, ValidationAction> availableValidations = Maps
            .newHashMap();

    @Getter
    private AbstractAutocomplete<HLocale> localeAutocomplete =
            new AbstractAutocomplete<HLocale>() {

                private LocaleService localeServiceImpl =
                        (LocaleService) Component
                                .getInstance(LocaleServiceImpl.class);

                private ZanataMessages zanataMessages =
                        (ZanataMessages) Component
                                .getInstance(ZanataMessages.class);

                private boolean isIncludeLocale(HLocale input) {
                    return !getInstance().getCustomizedLocales()
                            .contains(input)
                            && (input.getLocaleId().getId()
                                    .startsWith(getQuery()) || input
                                    .retrieveDisplayName().toLowerCase()
                                    .contains(getQuery().toLowerCase()));
                }

                @Override
                public List<HLocale> suggest() {
                    if (StringUtils.isEmpty(getQuery())
                            || !getInstance().isOverrideLocales()) {
                        return Lists.newArrayList();
                    } else {
                        List<HLocale> localeList =
                                localeServiceImpl.getSupportedLocales();
                        Collection<HLocale> filtered =
                                Collections2.filter(localeList,
                                        new Predicate<HLocale>() {
                                            @Override
                                            public boolean apply(
                                                    @Nullable HLocale input) {
                                                return isIncludeLocale(input);
                                            }
                                        });
                        return Lists.newArrayList(filtered);
                    }
                }

                @Override
                @Restrict("#{s:hasPermission(projectHome.instance, 'update')}")
                public void onSelectItemAction() {
                    if (StringUtils.isEmpty(getSelectedItem())) {
                        return;
                    }
                    clearMessage();

                    HLocale locale =
                            localeServiceImpl.getByLocaleId(getSelectedItem());

                    if (!getInstance().isOverrideLocales()) {
                        getInstance().setOverrideLocales(true);
                        getInstance().getCustomizedLocales().clear();
                    }
                    getInstance().getCustomizedLocales().add(locale);

                    update();
                    reset();

                    addMessage(StatusMessage.Severity.INFO,
                            zanataMessages.getMessage(
                                    "jsf.LanguageAddedToGroup",
                                    locale.retrieveDisplayName()));
                }
            };

    public void setSelectedProjectType(String selectedProjectType) {
        if (!StringUtils.isEmpty(selectedProjectType)
                && !selectedProjectType.equals("null")) {
            ProjectType projectType = ProjectType.valueOf(selectedProjectType);
            getInstance().setDefaultProjectType(projectType);
        } else {
            getInstance().setDefaultProjectType(null);
        }
    }

    public List<HLocale> getInstanceActiveLocales() {
        List<HLocale> locales;
        if (getInstance().isOverrideLocales()) {
            return Lists.newArrayList(getInstance().getCustomizedLocales());
        } else {
            locales = localeServiceImpl.getSupportedLocales();
        }

        Collections.sort(locales, new Comparator<HLocale>() {
            @Override
            public int compare(HLocale hLocale, HLocale hLocale2) {
                return hLocale.retrieveDisplayName().compareTo(
                        hLocale2.retrieveDisplayName());
            }
        });
        return locales;
    }

    @Restrict("#{s:hasPermission(projectHome.instance, 'update')}")
    public void removeLanguage(HLocale locale) {
        clearMessage();
        if (getInstance().isOverrideLocales()) {
            getInstance().getCustomizedLocales().remove(locale);
        } else {
            getInstance().setOverrideLocales(true);
            getInstance().getCustomizedLocales().clear();
            for (HLocale activeLocale : getInstanceActiveLocales()) {
                if (!activeLocale.equals(locale)) {
                    getInstance().getCustomizedLocales().add(activeLocale);
                }
            }
        }
        update();
        addMessage(
                StatusMessage.Severity.INFO,
                zanataMessages.getMessage("jsf.project.LanguageRemoved",
                        locale.retrieveDisplayName()));
    }

    @Restrict("#{s:hasPermission(projectHome.instance, 'update')}")
    public void setRestrictedByRole(String key, boolean checked) {
        getInstance().setRestrictedByRoles(checked);
        update();
    }

    @Override
    protected HProject loadInstance() {
        Session session = (Session) getEntityManager().getDelegate();
        return (HProject) session.byNaturalId(HProject.class)
                .using("slug", getSlug()).load();
    }

    @Getter
    private final AbstractAutocomplete<HPerson> maintainerAutocomplete =
            new AbstractAutocomplete<HPerson>() {

                private PersonDAO personDAO = (PersonDAO) Component
                        .getInstance(PersonDAO.class);

                private ZanataMessages zanataMessages =
                        (ZanataMessages) Component
                                .getInstance(ZanataMessages.class);

                public List<HPerson> suggest() {
                    List<HPerson> personList =
                            personDAO.findAllContainingName(getQuery());

                    Collection<HPerson> filtered =
                            Collections2.filter(personList,
                                    new Predicate<HPerson>() {
                                        @Override
                                        public boolean apply(
                                                @Nullable HPerson input) {
                                            return !getInstance()
                                                    .getMaintainers().contains(
                                                            input);
                                        }
                                    });

                    return Lists.newArrayList(filtered);
                }

                @Override
                @Restrict("#{s:hasPermission(projectHome.instance, 'update')}")
                public void onSelectItemAction() {
                    if (StringUtils.isEmpty(getSelectedItem())) {
                        return;
                    }

                    HPerson maintainer =
                            personDAO.findByUsername(getSelectedItem());
                    getInstance().getMaintainers().add(maintainer);
                    update();
                    addMessage(StatusMessage.Severity.INFO,
                            zanataMessages.getMessage(
                                    "jsf.MaintainerAddedToProject",
                                    maintainer.getName()));
                    reset();
                }
            };

    public void validateSuppliedId() {
        HProject ip = getInstance(); // this will raise an EntityNotFound
                                     // exception
        // when id is invalid and conversation will not
        // start

        if (ip.getStatus().equals(EntityStatus.OBSOLETE)
                && !checkViewObsolete()) {
            throw new EntityNotFoundException();
        }
    }

    @Transactional
    @Restrict("#{s:hasPermission(projectHome.instance, 'update')}")
    public void updateCopyTrans(String action, String value) {
        copyTransOptionsModel.setInstance(getInstance()
                .getDefaultCopyTransOpts());
        copyTransOptionsModel.update(action, value);
        copyTransOptionsModel.save();
        getInstance().setDefaultCopyTransOpts(
                copyTransOptionsModel.getInstance());

        update();
        addMessage(StatusMessage.Severity.INFO,
                zanataMessages.getMessage("jsf.project.CopyTransOpts.saved"));
    }

    public void initialize() {
        validateSuppliedId();
        if (getInstance().getDefaultCopyTransOpts() != null) {
            copyTransOptionsModel.setInstance(getInstance()
                    .getDefaultCopyTransOpts());
        }
    }

    public void verifySlugAvailable(ValueChangeEvent e) {
        String slug = (String) e.getNewValue();
        validateSlug(slug, e.getComponent().getId());
    }

    public boolean validateSlug(String slug, String componentId) {
        if (!isSlugAvailable(slug)) {
            FacesMessages.instance().addToControl(componentId,
                    "This Project ID is not available");
            return false;
        }
        return true;
    }

    public boolean isSlugAvailable(String slug) {
        return slugEntityServiceImpl.isSlugAvailable(slug, HProject.class);
    }

    @Override
    @Transactional
    public String persist() {
        String retValue = "";
        if (!validateSlug(getInstance().getSlug(), "slug"))
            return null;

        if (authenticatedAccount != null) {
            getInstance().addMaintainer(authenticatedAccount.getPerson());
            retValue = super.persist();
            Events.instance().raiseEvent("projectAdded");
        }
        return retValue;
    }

    public final static Comparator<HPerson> PERSON_COMPARATOR =
            new Comparator<HPerson>() {
                @Override
                public int compare(HPerson hPerson, HPerson hPerson2) {
                    return hPerson.getName().compareTo(hPerson2.getName());
                }
            };

    public List<HPerson> getInstanceMaintainers() {
        List<HPerson> list = Lists.newArrayList(getInstance().getMaintainers());

        Collections.sort(list, PERSON_COMPARATOR);

        return list;
    }

    @Restrict("#{s:hasPermission(projectHome.instance, 'update')}")
    public void removeMaintainer(HPerson maintainer) {
        clearMessage();
        getInstance().getMaintainers().remove(maintainer);
        update();

        addMessage(StatusMessage.Severity.INFO, zanataMessages.getMessage(
                "jsf.MaintainerRemoveFromProject", maintainer.getName()));
    }

    @Restrict("#{s:hasPermission(projectHome.instance, 'update')}")
    public void updateRoles(String roleName, boolean isRestricted) {
        getInstance().getAllowedRoles().clear();
        if (getInstance().isRestrictedByRoles()) {
            getRoleRestrictions().put(roleName, isRestricted);

            for (Map.Entry<String, Boolean> entry : getRoleRestrictions()
                    .entrySet()) {
                if (entry.getValue()) {
                    getInstance().getAllowedRoles().add(
                            accountRoleDAO.findByName(entry.getKey()));
                }
            }
        }
        update();
        addMessage(StatusMessage.Severity.INFO,
                zanataMessages.getMessage("jsf.RolesUpdated"));
    }

    @Restrict("#{s:hasPermission(projectHome.instance, 'update')}")
    public void updateStatus(char initial) {
        getInstance().setStatus(EntityStatus.valueOf(initial));
        if (getInstance().getStatus() == EntityStatus.READONLY) {
            for (HProjectIteration version : getInstance()
                    .getProjectIterations()) {
                if (version.getStatus() == EntityStatus.ACTIVE) {
                    version.setStatus(EntityStatus.READONLY);
                    entityManager.merge(version);
                    Events.instance().raiseEvent(
                            VersionHome.PROJECT_ITERATION_UPDATE, version);
                }
            }
        } else if (getInstance().getStatus() == EntityStatus.OBSOLETE) {
            for (HProjectIteration version : getInstance()
                    .getProjectIterations()) {
                if (version.getStatus() != EntityStatus.OBSOLETE) {
                    version.setStatus(EntityStatus.OBSOLETE);
                    entityManager.merge(version);
                    Events.instance().raiseEvent(
                            VersionHome.PROJECT_ITERATION_UPDATE, version);
                }
            }
        }
        update();
        addMessage(StatusMessage.Severity.INFO, zanataMessages.getMessage(
                "jsf.project.status.updated", EntityStatus.valueOf(initial)));
    }

    public Map<String, Boolean> getRoleRestrictions() {
        if (roleRestrictions == null) {
            roleRestrictions = Maps.newHashMap();

            for (HAccountRole role : getInstance().getAllowedRoles()) {
                roleRestrictions.put(role.getName(), true);
            }
        }
        return roleRestrictions;
    }

    public boolean isRoleRestrictionEnabled(String roleName) {
        if (getRoleRestrictions().containsKey(roleName)) {
            return getRoleRestrictions().get(roleName);
        }
        return false;
    }

    public List<HAccountRole> getAvailableRoles() {
        List<HAccountRole> allRoles = accountRoleDAO.findAll();

        Collections.sort(allRoles, new Comparator<HAccountRole>() {
            @Override
            public int compare(HAccountRole o1, HAccountRole o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return allRoles;
    }

    /**
     * Use FlashScopeBean to store message in page. Multiple ajax requests for
     * re-rendering statistics after updating will clear FacesMessages.
     *
     * @param severity
     * @param message
     */
    private void addMessage(StatusMessage.Severity severity, String message) {
        StatusMessage statusMessage =
                new StatusMessage(severity, null, null, message, null);
        statusMessage.interpolate();

        if (flashScope == null) {
            initFlashScope();
        }
        flashScope.setAttribute("message", statusMessage);
    }

    private void clearMessage() {
        if (flashScope == null) {
            initFlashScope();
        }
        flashScope.getAndClearAttribute("message");
    }

    private void initFlashScope() {
        flashScope =
                (FlashScopeBean) Component.getInstance(FlashScopeBean.class);
    }

    public List<HProjectIteration> getVersions() {
        List<HProjectIteration> results = new ArrayList<HProjectIteration>();

        for (HProjectIteration iteration : getInstance().getProjectIterations()) {
            if (iteration.getStatus() == EntityStatus.OBSOLETE
                    && checkViewObsolete()) {
                results.add(iteration);
            } else if (iteration.getStatus() != EntityStatus.OBSOLETE) {
                results.add(iteration);
            }
        }
        Collections.sort(results, new Comparator<HProjectIteration>() {
            @Override
            public int compare(HProjectIteration o1, HProjectIteration o2) {
                EntityStatus fromStatus = o1.getStatus();
                EntityStatus toStatus = o2.getStatus();

                if (fromStatus.equals(toStatus)) {
                    return 0;
                }

                if (fromStatus.equals(EntityStatus.ACTIVE)) {
                    return -1;
                }

                if (fromStatus.equals(EntityStatus.READONLY)) {
                    if (toStatus.equals(EntityStatus.ACTIVE)) {
                        return 1;
                    }
                    return -1;
                }

                if (fromStatus.equals(EntityStatus.OBSOLETE)) {
                    return 1;
                }

                return 0;
            }
        });
        return results;
    }

    public EntityStatus getEffectiveVersionStatus(HProjectIteration version) {
        /**
         * Null pointer exception checking caused by unknown issues where
         * getEffectiveIterationStatus gets invoke before getIterations
         */
        if (version == null) {
            return null;
        }
        if (getInstance().getStatus() == EntityStatus.READONLY) {
            if (version.getStatus() == EntityStatus.ACTIVE) {
                return EntityStatus.READONLY;
            }
        } else if (getInstance().getStatus() == EntityStatus.OBSOLETE) {
            if (version.getStatus() == EntityStatus.ACTIVE
                    || version.getStatus() == EntityStatus.READONLY) {
                return EntityStatus.OBSOLETE;
            }
        }
        return version.getStatus();
    }

    public String cancel() {
        return "cancel";
    }

    @Override
    public boolean isIdDefined() {
        return slug != null;
    }

    @Override
    public NaturalIdentifier getNaturalId() {
        return Restrictions.naturalId().set("slug", slug);
    }

    @Override
    public Object getId() {
        return slug;
    }

    private Map<ValidationId, ValidationAction> getValidations() {
        if (availableValidations.isEmpty()) {
            Collection<ValidationAction> validationList =
                    validationServiceImpl.getValidationActions(slug);

            for (ValidationAction validationAction : validationList) {
                availableValidations.put(validationAction.getId(),
                        validationAction);
            }
        }

        return availableValidations;
    }

    @Restrict("#{s:hasPermission(projectHome.instance, 'update')}")
    public void updateValidationOption(String name, String state) {
        ValidationId validatationId = ValidationId.valueOf(name);

        for (Map.Entry<ValidationId, ValidationAction> entry : getValidations()
                .entrySet()) {
            if (entry.getKey().name().equals(name)) {
                getValidations().get(validatationId).setState(
                        ValidationAction.State.valueOf(state));
                ensureMutualExclusivity(getValidations().get(validatationId));
                break;
            }
            getInstance().getCustomizedValidations().put(entry.getKey().name(),
                    entry.getValue().getState().name());
        }

        update();
        addMessage(StatusMessage.Severity.INFO, zanataMessages.getMessage(
                "jsf.project.validation.updated",
                validatationId.getDisplayName(), state));
    }

    public List<ValidationAction> getValidationList() {
        List<ValidationAction> sortedList =
                Lists.newArrayList(getValidations().values());
        Collections.sort(sortedList,
                ValidationFactory.ValidationActionComparator);
        return sortedList;
    }

    /**
     * If this action is enabled(Warning or Error), then it's exclusive
     * validation will be turn off
     *
     * @param selectedValidationAction
     */
    private void ensureMutualExclusivity(
            ValidationAction selectedValidationAction) {
        if (selectedValidationAction.getState() != ValidationAction.State.Off) {
            for (ValidationAction exclusiveValAction : selectedValidationAction
                    .getExclusiveValidations()) {
                getInstance().getCustomizedValidations().put(
                        exclusiveValAction.getId().name(),
                        ValidationAction.State.Off.name());
                getValidations().get(exclusiveValAction.getId()).setState(
                        ValidationAction.State.Off);
            }
        }
    }

    public List<ValidationAction.State> getValidationStates() {
        return Arrays.asList(ValidationAction.State.values());
    }

    @Override
    public String update() {
        clearMessage();
        String state = super.update();
        Events.instance().raiseEvent(PROJECT_UPDATE, getInstance());
        return state;
    }

    public boolean isProjectActive() {
        return getInstance().getStatus() == EntityStatus.ACTIVE;
    }

    public boolean checkViewObsolete() {
        return identity != null
                && identity.hasPermission("HProject", "view-obsolete");
    }
}
