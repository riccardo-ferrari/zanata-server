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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.NaturalIdentifier;
import org.hibernate.criterion.Restrictions;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.common.EntityStatus;
import org.zanata.dao.PersonDAO;
import org.zanata.dao.ProjectIterationDAO;
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
import org.zanata.util.ZanataMessages;
import org.zanata.webtrans.shared.model.ValidationAction;

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

    /* Outjected from ProjectRoleRestrictionAction */
    @In(required = false)
    private Set<HAccountRole> customizedProjectRoleRestrictions;

    /* Outjected from ProjectRoleRestrictionAction */
    @In(required = false)
    private Boolean restrictByRoles;

    /* Outjected from ValidationOptionsAction */
    @In(required = false)
    private Collection<ValidationAction> customizedValidations;

    @In
    private LocaleService localeServiceImpl;

    @In
    private SlugEntityService slugEntityServiceImpl;

    @In
    private PersonDAO personDAO;

    @In
    private FlashScopeBean flashScope;

    @In
    private EntityManager entityManager;

    @In
    private ZanataMessages zanataMessages;

    @Override
    protected HProject loadInstance() {
        Session session = (Session) getEntityManager().getDelegate();
        return (HProject) session.byNaturalId(HProject.class)
                .using("slug", getSlug()).load();
    }

    @Getter
    private final AbstractAutocomplete<HPerson> maintainerAutocomplete =
            new AbstractAutocomplete<HPerson>() {

                @Override
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
                    reset();

                    addMessage(StatusMessage.Severity.INFO,
                            zanataMessages.getMessage(
                                    "jsf.MaintainerAddedToProject",
                                    maintainer.getName()));
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
            updateOverrideLocales();
            updateRoleRestrictions();
            updateOverrideValidations();
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
        super.update();

        addMessage(StatusMessage.Severity.INFO, zanataMessages.getMessage(
                "jsf.MaintainerRemoveFromProject", maintainer.getName()));
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

        flashScope.setAttribute("message", statusMessage);
    }

    private void clearMessage() {
        flashScope.getAndClearAttribute("message");
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

    @Override
    public String update() {
        updateOverrideLocales();
        updateRoleRestrictions();
        updateOverrideValidations();
        String state = super.update();
        Events.instance().raiseEvent(PROJECT_UPDATE, getInstance());

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

        return state;
    }

    private void updateOverrideLocales() {
        if (overrideLocales != null) {
            getInstance().setOverrideLocales(overrideLocales);
            if (!overrideLocales) {
                getInstance().getCustomizedLocales().clear();
            } else if (customizedItems != null) {
                Set<HLocale> locale =
                        localeServiceImpl
                                .convertCustomizedLocale(customizedItems);
                getInstance().getCustomizedLocales().clear();
                getInstance().getCustomizedLocales().addAll(locale);
            }
        }
    }

    private void updateOverrideValidations() {
        // edit project page code won't have customized validations outjected
        if (customizedValidations != null) {
            getInstance().getCustomizedValidations().clear();
            for (ValidationAction action : customizedValidations) {
                getInstance().getCustomizedValidations().put(
                        action.getId().name(), action.getState().name());
            }
        }
    }

    private void updateRoleRestrictions() {
        if (restrictByRoles != null) {
            getInstance().setRestrictedByRoles(restrictByRoles);
            getInstance().getAllowedRoles().clear();

            if (restrictByRoles) {
                getInstance().getAllowedRoles().addAll(
                        customizedProjectRoleRestrictions);
            }
        }
    }

    public boolean isProjectActive() {
        return getInstance().getStatus() == EntityStatus.ACTIVE;
    }

    public boolean checkViewObsolete() {
        return identity != null
                && identity.hasPermission("HProject", "view-obsolete");
    }
}
