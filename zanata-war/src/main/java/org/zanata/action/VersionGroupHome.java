/*
 * Copyright 2013, Red Hat, Inc. and individual contributors as indicated by the
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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.NaturalIdentifier;
import org.hibernate.criterion.Restrictions;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.zanata.common.EntityStatus;
import org.zanata.dao.PersonDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.model.HAccount;
import org.zanata.model.HIterationGroup;
import org.zanata.model.HLocale;
import org.zanata.model.HPerson;
import org.zanata.model.HProjectIteration;
import org.zanata.seam.scope.FlashScopeBean;
import org.zanata.service.LocaleService;
import org.zanata.service.SlugEntityService;
import org.zanata.service.VersionGroupService;
import org.zanata.service.impl.LocaleServiceImpl;
import org.zanata.service.impl.VersionGroupServiceImpl;
import org.zanata.util.ZanataMessages;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */

@Name("versionGroupHome")
public class VersionGroupHome extends SlugHome<HIterationGroup> {
    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private String slug;

    @In(required = false, value = JpaIdentityStore.AUTHENTICATED_USER)
    private HAccount authenticatedAccount;

    @In
    private SlugEntityService slugEntityServiceImpl;

    @In
    private ZanataMessages zanataMessages;

    @In
    private FlashScopeBean flashScope;

    private List<SelectItem> statusList;

    @Getter
    private AbstractAutocomplete<HPerson> maintainerAutocomplete =
            new AbstractAutocomplete<HPerson>() {

                private PersonDAO personDAO = (PersonDAO) Component
                        .getInstance(PersonDAO.class);

                private ZanataMessages zanataMessages =
                        (ZanataMessages) Component
                                .getInstance(ZanataMessages.class);

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
                @Restrict("#{s:hasPermission(versionGroupHome.instance, 'update')}")
                public
                        void onSelectItemAction() {
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
                                    "jsf.MaintainerAddedToGroup",
                                    maintainer.getName()));
                }
            };

    @Getter
    private AbstractAutocomplete<HProjectIteration> versionAutocomplete =
            new AbstractAutocomplete<HProjectIteration>() {
                private ProjectIterationDAO projectIterationDAO =
                        (ProjectIterationDAO) Component
                                .getInstance(ProjectIterationDAO.class);

                private VersionGroupService versionGroupServiceImpl =
                        (VersionGroupService) Component
                                .getInstance(VersionGroupServiceImpl.class);

                private ZanataMessages zanataMessages =
                        (ZanataMessages) Component
                                .getInstance(ZanataMessages.class);

                @Override
                public List<HProjectIteration> suggest() {
                    List<HProjectIteration> versionList =
                            versionGroupServiceImpl
                                    .searchLikeSlugOrProjectSlug(getQuery());

                    Collection<HProjectIteration> filtered =
                            Collections2.filter(versionList,
                                    new Predicate<HProjectIteration>() {
                                        @Override
                                        public
                                                boolean
                                                apply(@Nullable HProjectIteration input) {
                                            return !input.getGroups().contains(
                                                    getInstance());
                                        }
                                    });

                    return Lists.newArrayList(filtered);
                }

                @Override
                @Restrict("#{s:hasPermission(versionGroupHome.instance, 'update')}")
                public
                        void onSelectItemAction() {
                    if (StringUtils.isEmpty(getSelectedItem())) {
                        return;
                    }

                    HProjectIteration version =
                            projectIterationDAO.findById(new Long(
                                    getSelectedItem()));
                    getInstance().getProjectIterations().add(version);
                    update();
                    reset();

                    addMessage(StatusMessage.Severity.INFO,
                            zanataMessages.getMessage(
                                    "jsf.VersionAddedToGroup", version
                                            .getSlug(), version.getProject()
                                            .getSlug()));
                }
            };

    @Getter
    private AbstractAutocomplete<HLocale> localeAutocomplete =
            new AbstractAutocomplete<HLocale>() {

                private LocaleService localeServiceImpl =
                        (LocaleService) Component
                                .getInstance(LocaleServiceImpl.class);

                private ZanataMessages zanataMessages =
                        (ZanataMessages) Component
                                .getInstance(ZanataMessages.class);

                @Override
                public List<HLocale> suggest() {
                    if (StringUtils.isEmpty(getQuery())) {
                        return Lists.newArrayList();
                    }

                    List<HLocale> localeList =
                            localeServiceImpl.getSupportedLocales();

                    Collection<HLocale> filtered =
                            Collections2.filter(localeList,
                                    new Predicate<HLocale>() {
                                        @Override
                                        public boolean apply(
                                                @Nullable HLocale input) {
                                            if (StringUtils.isEmpty(getQuery())) {
                                                return !getInstance()
                                                        .getActiveLocales()
                                                        .contains(input);
                                            }

                                            return !getInstance()
                                                    .getActiveLocales()
                                                    .contains(input)
                                                    && (input
                                                            .getLocaleId()
                                                            .getId()
                                                            .startsWith(
                                                                    getQuery()) || input
                                                            .retrieveDisplayName()
                                                            .toLowerCase()
                                                            .contains(
                                                                    getQuery()
                                                                            .toLowerCase()));
                                        }
                                    });

                    return Lists.newArrayList(filtered);
                }

                @Override
                @Restrict("#{s:hasPermission(versionGroupHome.instance, 'update')}")
                public
                        void onSelectItemAction() {
                    if (StringUtils.isEmpty(getSelectedItem())) {
                        return;
                    }

                    HLocale locale =
                            localeServiceImpl.getByLocaleId(getSelectedItem());

                    getInstance().getActiveLocales().add(locale);
                    update();
                    reset();

                    addMessage(StatusMessage.Severity.INFO,
                            zanataMessages.getMessage(
                                    "jsf.LanguageAddedToGroup",
                                    locale.retrieveDisplayName()));
                }
            };

    public void verifySlugAvailable(ValueChangeEvent e) {
        String slug = (String) e.getNewValue();
        validateSlug(slug, e.getComponent().getId());
    }

    public boolean validateSlug(String slug, String componentId) {
        if (!isSlugAvailable(slug)) {
            FacesMessages.instance().addToControl(componentId,
                    "This Group ID is not available");
            return false;
        }
        return true;
    }

    public boolean isSlugAvailable(String slug) {
        return slugEntityServiceImpl.isSlugAvailable(slug,
                HIterationGroup.class);
    }

    @Override
    @Restrict("#{s:hasPermission(versionGroupHome.instance, 'update')}")
    public String persist() {
        if (!validateSlug(getInstance().getSlug(), "slug"))
            return null;

        if (authenticatedAccount != null) {
            getInstance().addMaintainer(authenticatedAccount.getPerson());
        }
        clearMessage();
        return super.persist();
    }

    @Override
    @Restrict("#{s:hasPermission(versionGroupHome.instance, 'update')}")
    public String update() {
        clearMessage();
        return super.update();
    }

    @Override
    public List<SelectItem> getStatusList() {
        return getAvailableStatus();
    }

    public void setStatus(char initial) {
        getInstance().setStatus(EntityStatus.valueOf(initial));
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

    private void initFlashScope() {
        flashScope =
                (FlashScopeBean) Component.getInstance(FlashScopeBean.class);
    }

    private void clearMessage() {
        if (flashScope == null) {
            initFlashScope();
        }
        flashScope.getAndClearAttribute("message");
    }

    @Restrict("#{s:hasPermission(versionGroupHome.instance, 'update')}")
    public void removeLanguage(HLocale locale) {
        getInstance().getActiveLocales().remove(locale);
        super.update();
        addMessage(
                StatusMessage.Severity.INFO,
                zanataMessages.getMessage("jsf.LanguageRemoveFromGroup",
                        locale.retrieveDisplayName()));
    }

    @Restrict("#{s:hasPermission(versionGroupHome.instance, 'update')}")
    public void removeVersion(HProjectIteration version) {
        getInstance().getProjectIterations().remove(version);
        super.update();

        addMessage(
                StatusMessage.Severity.INFO,
                zanataMessages.getMessage("jsf.VersionRemoveFromGroup",
                        version.getSlug(), version.getProject().getSlug()));
    }

    @Restrict("#{s:hasPermission(versionGroupHome.instance, 'update')}")
    public void removeMaintainer(HPerson maintainer) {
        clearMessage();
        getInstance().getMaintainers().remove(maintainer);
        super.update();

        addMessage(StatusMessage.Severity.INFO, zanataMessages.getMessage(
                "jsf.MaintainerRemoveFromGroup", maintainer.getName()));
    }

    public List<HLocale> getInstanceActiveLocales() {
        List<HLocale> activeLocales =
                Lists.newArrayList(getInstance().getActiveLocales());

        Collections.sort(activeLocales, new Comparator<HLocale>() {
            @Override
            public int compare(HLocale hLocale, HLocale hLocale2) {
                return hLocale.retrieveDisplayName().compareTo(
                        hLocale2.retrieveDisplayName());
            }
        });
        return activeLocales;
    }

    private List<SelectItem> getAvailableStatus() {
        if (statusList == null) {
            statusList =
                    ImmutableList.copyOf(Iterables.filter(
                            super.getStatusList(), new Predicate<SelectItem>() {
                                @Override
                                public boolean apply(SelectItem input) {
                                    return !input.getValue().equals(
                                            EntityStatus.READONLY);
                                }
                            }));
        }
        return statusList;
    }

    @Override
    protected HIterationGroup loadInstance() {
        Session session = (Session) getEntityManager().getDelegate();
        return (HIterationGroup) session.byNaturalId(HIterationGroup.class)
                .using("slug", getSlug()).load();
    }

    // sort by slug
    public List<HProjectIteration> getSortedInstanceProjectIterations() {
        List<HProjectIteration> list =
                Lists.newArrayList(getInstance().getProjectIterations());

        Collections.sort(list, new Comparator<HProjectIteration>() {
            @Override
            public int compare(HProjectIteration documentWithIds,
                    HProjectIteration documentWithIds2) {
                return documentWithIds
                        .getProject()
                        .getName()
                        .toLowerCase()
                        .compareTo(
                                documentWithIds2.getProject().getName()
                                        .toLowerCase());
            }
        });

        return list;
    }

    public List<HPerson> getInstanceMaintainers() {
        List<HPerson> list = Lists.newArrayList(getInstance().getMaintainers());

        Collections.sort(list, PERSON_COMPARATOR);

        return list;
    }

    public final static Comparator<HPerson> PERSON_COMPARATOR =
            new Comparator<HPerson>() {
                @Override
                public int compare(HPerson hPerson, HPerson hPerson2) {
                    return hPerson.getName().compareTo(hPerson2.getName());
                }
            };

    @Override
    public NaturalIdentifier getNaturalId() {
        return Restrictions.naturalId().set("slug", slug);
    }

    @Override
    public boolean isIdDefined() {
        return slug != null;
    }

    @Override
    public Object getId() {
        return slug;
    }

    public void validateSuppliedId() {
        getInstance(); // this will raise an EntityNotFound exception
        // when id is invalid and conversation will not
        // start
    }

    public String cancel() {
        return "cancel";
    }
}
