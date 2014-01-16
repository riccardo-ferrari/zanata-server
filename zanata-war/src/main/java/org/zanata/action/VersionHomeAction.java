/*
 *
 *  * Copyright 2014, Red Hat, Inc. and individual contributors as indicated by the
 *  * @author tags. See the copyright.txt file in the distribution for a full
 *  * listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it under the
 *  * terms of the GNU Lesser General Public License as published by the Free
 *  * Software Foundation; either version 2.1 of the License, or (at your option)
 *  * any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *  * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 *  * details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public License
 *  * along with this software; if not, write to the Free Software Foundation,
 *  * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 *  * site: http://www.fsf.org.
 */
package org.zanata.action;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.framework.EntityNotFoundException;
import org.zanata.annotation.CachedMethods;
import org.zanata.common.EntityStatus;
import org.zanata.common.LocaleId;
import org.zanata.dao.DocumentDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.model.HDocument;
import org.zanata.model.HIterationGroup;
import org.zanata.model.HLocale;
import org.zanata.model.HProjectIteration;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.LocaleService;
import org.zanata.service.TranslationStateCache;
import org.zanata.service.VersionStateCache;
import org.zanata.ui.model.statistic.WordStatistic;
import org.zanata.util.StatisticsUtil;
import org.zanata.util.ZanataMessages;
import org.zanata.webtrans.shared.model.DocumentStatus;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Name("versionHomeAction")
@Scope(ScopeType.PAGE)
@CachedMethods
public class VersionHomeAction extends AbstractSortAction implements
        Serializable {
    private static final long serialVersionUID = 1L;

    @In
    private ProjectIterationDAO projectIterationDAO;

    @In
    private DocumentDAO documentDAO;

    @In
    private LocaleService localeServiceImpl;

    @In
    private VersionStateCache versionStateCacheImpl;

    @In
    private TranslationStateCache translationStateCacheImpl;

    @In
    private ZanataMessages zanataMessages;

    @In
    private ZanataIdentity identity;

    @Setter
    @Getter
    private String versionSlug;

    @Setter
    @Getter
    private String projectSlug;

    @Getter
    private boolean pageRendered = false;

    @Getter
    private WordStatistic overallStatistic;

    @Getter
    private HLocale selectedLocale;

    @Getter
    @Setter
    private HDocument selectedDocument;

    private List<HLocale> supportedLocale;

    private List<HDocument> documents;

    private Map<LocaleId, WordStatistic> statisticMap;

    private Map<DocumentLocaleKey, WordStatistic> documentStatisticMap;

    @Getter
    private SortingType documentSortingList = new SortingType(
            Lists.newArrayList(SortingType.SortOption.ALPHABETICAL,
                    SortingType.SortOption.HOURS,
                    SortingType.SortOption.PERCENTAGE,
                    SortingType.SortOption.WORDS,
                    SortingType.SortOption.LAST_UPDATED,
                    SortingType.SortOption.LAST_TRANSLATED));

    private final LanguageComparator languageComparator =
            new LanguageComparator(getLanguageSortingList());

    private final DocumentComparator documentComparator =
            new DocumentComparator(getDocumentSortingList());

    /**
     * Sort language list based on locale statistic
     */
    public void sortLanguageList() {
        languageComparator.setSelectedDocumentId(null);
        Collections.sort(getSupportedLocale(), languageComparator);
    }

    /**
     * Sort document list based on selected locale
     *
     * @param localeId
     */
    public void sortDocumentList(LocaleId localeId) {
        documentComparator.setSelectedLocaleId(localeId);
        Collections.sort(getDocuments(), documentComparator);
        languageTabDocumentFilter.resetDocumentPage();
    }

    public void sortDocumentList() {
        documentComparator.setSelectedLocaleId(null);
        Collections.sort(getDocuments(), documentComparator);
        languageTabDocumentFilter.resetDocumentPage();
    }

    @Override
    public void resetPageData() {
        languageTabDocumentFilter.resetDocumentPage();
        loadStatistic();
    }

    @Override
    protected void loadStatistic() {
        statisticMap = Maps.newHashMap();
        for (HLocale locale : getSupportedLocale()) {
            WordStatistic wordStatistic =
                    versionStateCacheImpl.getVersionStatistics(getVersion()
                            .getId(), locale.getLocaleId());
            wordStatistic.setRemainingHours(StatisticsUtil
                    .getRemainingHours(wordStatistic));
            statisticMap.put(locale.getLocaleId(), wordStatistic);
        }

        overallStatistic = new WordStatistic();
        for (Map.Entry<LocaleId, WordStatistic> entry : statisticMap.entrySet()) {
            overallStatistic.add(entry.getValue());
        }
        overallStatistic.setRemainingHours(StatisticsUtil
                .getRemainingHours(overallStatistic));

        documentStatisticMap = Maps.newHashMap();
        for (HDocument document : getVersion().getDocuments().values()) {
            for (HLocale locale : getSupportedLocale()) {
                WordStatistic wordStatistic =
                        documentDAO.getWordStatistics(document.getId(),
                                locale.getLocaleId());
                wordStatistic.setRemainingHours(StatisticsUtil
                        .getRemainingHours(wordStatistic));
                documentStatisticMap.put(new DocumentLocaleKey(
                        document.getId(), locale.getLocaleId()), wordStatistic);
            }
        }
    }

    @Override
    String getMessage(String key, Object... args) {
        return zanataMessages.getMessage(key, args);
    }

    public List<HLocale> getSupportedLocale() {
        if (supportedLocale == null) {
            supportedLocale =
                    localeServiceImpl.getSupportedLanguageByProjectIteration(
                            projectSlug, versionSlug);
            Collections.sort(supportedLocale, languageComparator);
        }
        return supportedLocale;
    }

    public List<HDocument> getDocuments() {
        if (documents == null) {
            HProjectIteration version = getVersion();
            if (version != null) {
                documents = Lists.newArrayList(version.getDocuments().values());
            }
            Collections.sort(documents, documentComparator);
        }
        return documents;
    }

    private List<HIterationGroup> groups;

    public List<HIterationGroup> getGroups() {
        if (groups == null) {
            HProjectIteration version = getVersion();
            if (version != null) {
                groups = Lists.newArrayList(version.getGroups());
            }
        }
        return groups;
    }

    public HProjectIteration getVersion() {
        return projectIterationDAO.getBySlug(projectSlug, versionSlug);
    }

    public void setPageRendered(boolean pageRendered) {
        if (pageRendered) {
            loadStatistic();
        }
        this.pageRendered = pageRendered;
    }

    public void validateIteration() {
        if (getVersion() == null) {
            throw new EntityNotFoundException(versionSlug,
                    HProjectIteration.class);
        }
    }

    public void setSelectedLocale(HLocale hLocale) {
        this.selectedLocale = hLocale;
        resetPageData();
    }

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    public class DocumentLocaleKey {
        private Long documentId;
        private LocaleId localeId;
    }

    public WordStatistic getStatisticsForLocale(LocaleId localeId) {
        return statisticMap.get(localeId);
    }

    public WordStatistic getStatisticForDocument(Long documentId,
            LocaleId localeId) {
        return documentStatisticMap.get(new DocumentLocaleKey(documentId,
                localeId));
    }

    public WordStatistic getDocumentStatistic(Long documentId) {
        WordStatistic wordStatistic = new WordStatistic();
        for (Map.Entry<DocumentLocaleKey, WordStatistic> entry : documentStatisticMap
                .entrySet()) {
            if (entry.getKey().getDocumentId().equals(documentId)) {
                wordStatistic.add(entry.getValue());
            }
        }
        wordStatistic.setRemainingHours(StatisticsUtil
                .getRemainingHours(wordStatistic));
        return wordStatistic;
    }

    public DisplayUnit getStatisticFigureForDocumentWithLocale(
            SortingType.SortOption sortOption, LocaleId localeId,
            Long documentId) {
        WordStatistic statistic = getStatisticForDocument(documentId, localeId);
        return getDisplayUnit(sortOption, statistic);
    }

    public DisplayUnit getStatisticFigureForLocale(
            SortingType.SortOption sortOption, LocaleId localeId) {
        WordStatistic statistic = getStatisticsForLocale(localeId);
        return getDisplayUnit(sortOption, statistic);
    }

    public boolean isUserAllowedToTranslateOrReview(HLocale hLocale) {
        return isVersionActive()
                && identity != null
                && (identity.hasPermission("add-translation", getVersion()
                        .getProject(), hLocale) || identity.hasPermission(
                        "translation-review", getVersion().getProject(),
                        hLocale));
    }

    private boolean isVersionActive() {
        return getVersion().getProject().getStatus() == EntityStatus.ACTIVE
                || getVersion().getStatus() == EntityStatus.ACTIVE;
    }

    @Getter
    private final AbstractDocumentsFilter languageTabDocumentFilter =
            new AbstractDocumentsFilter() {

                @Override
                public int getFilteredDocumentSize() {
                    if (getSelectedLocale() == null) {
                        return 0;
                    } else {
                        return getFilteredDocuments().size();
                    }
                }

                @Override
                public List<HDocument> getPagedFilteredDocuments() {
                    List<List<HDocument>> partition =
                            Lists.partition(getFilteredDocuments(),
                                    getDocumentCountPerPage());
                    if (!partition.isEmpty()
                            && getCurrentDocumentPage() <= partition.size()) {
                        return partition.get(getCurrentDocumentPage());
                    }
                    return Lists.newArrayList();
                }

                @Override
                List<HDocument> getFilteredDocuments() {
                    List<HDocument> list = getDocuments();
                    if (StringUtils.isEmpty(getDocumentQuery())) {
                        return list;
                    }
                    final String lowerCaseQuery =
                            getDocumentQuery().toLowerCase();
                    Collection<HDocument> filtered =
                            Collections2.filter(list,
                                    new Predicate<HDocument>() {
                                        @Override
                                        public boolean apply(
                                                @Nullable HDocument input) {
                                            return input.getName()
                                                    .toLowerCase()
                                                    .contains(lowerCaseQuery)
                                                    || input.getPath()
                                                            .toLowerCase()
                                                            .contains(
                                                                    lowerCaseQuery);
                                        }
                                    });
                    return Lists.newArrayList(filtered);
                }
            };

    @Getter
    private final AbstractDocumentsFilter documentTabDocumentFilter =
            new AbstractDocumentsFilter() {

                @Override
                public int getFilteredDocumentSize() {
                    if (getSelectedLocale() == null) {
                        return 0;
                    } else {
                        return getFilteredDocuments().size();
                    }
                }

                @Override
                public List<HDocument> getPagedFilteredDocuments() {
                    List<List<HDocument>> partition =
                            Lists.partition(getFilteredDocuments(),
                                    getDocumentCountPerPage());
                    if (!partition.isEmpty()
                            && getCurrentDocumentPage() <= partition.size()) {
                        return partition.get(getCurrentDocumentPage());
                    }
                    return Lists.newArrayList();
                }

                @Override
                public List<HDocument> getFilteredDocuments() {
                    List<HDocument> list = getDocuments();
                    if (StringUtils.isEmpty(getDocumentQuery())) {
                        return list;
                    }
                    final String lowerCaseQuery =
                            getDocumentQuery().toLowerCase();
                    Collection<HDocument> filtered =
                            Collections2.filter(list,
                                    new Predicate<HDocument>() {
                                        @Override
                                        public boolean apply(
                                                @Nullable HDocument input) {
                                            return input.getName()
                                                    .toLowerCase()
                                                    .contains(lowerCaseQuery)
                                                    || input.getPath()
                                                            .toLowerCase()
                                                            .contains(
                                                                lowerCaseQuery);
                                        }
                                    });
                    return Lists.newArrayList(filtered);
                }
            };

    private class DocumentComparator implements Comparator<HDocument> {
        private SortingType sortingType;

        @Setter
        private LocaleId selectedLocaleId;

        public DocumentComparator(SortingType sortingType) {
            this.sortingType = sortingType;
        }

        @Override
        public int compare(HDocument o1, HDocument o2) {
            final HDocument item1, item2;

            if (sortingType.isDescending()) {
                item1 = o1;
                item2 = o2;
            } else {
                item1 = o2;
                item2 = o1;
            }

            SortingType.SortOption selectedSortOption =
                    sortingType.getSelectedSortOption();

            if (selectedSortOption.equals(SortingType.SortOption.ALPHABETICAL)) {
                return item1.getName().compareTo(item2.getName());
            } else if (selectedSortOption
                    .equals(SortingType.SortOption.LAST_UPDATED)) {
                return item1.getLastChanged().compareTo(item2.getLastChanged());
            } else if (selectedSortOption
                    .equals(SortingType.SortOption.LAST_TRANSLATED)) {
                if (selectedLocaleId != null) {
                    DocumentStatus docStat1 =
                            translationStateCacheImpl.getDocumentStatus(
                                    o1.getId(), selectedLocaleId);
                    DocumentStatus docStat2 =
                            translationStateCacheImpl.getDocumentStatus(
                                    o2.getId(), selectedLocaleId);
                    return docStat1.getLastTranslatedDate().compareTo(
                            docStat2.getLastTranslatedDate());
                }
            } else {
                WordStatistic wordStatistic1;
                WordStatistic wordStatistic2;
                if (selectedLocaleId != null) {
                    wordStatistic1 =
                            documentStatisticMap.get(new DocumentLocaleKey(
                                    item1.getId(), selectedLocaleId));
                    wordStatistic2 =
                            documentStatisticMap.get(new DocumentLocaleKey(
                                    item2.getId(), selectedLocaleId));

                } else {
                    wordStatistic1 = getDocumentStatistic(item1.getId());
                    wordStatistic2 = getDocumentStatistic(item2.getId());
                }
                return compareWordStatistic(wordStatistic1, wordStatistic2,
                        selectedSortOption);
            }
            return 0;
        }
    }

    private class LanguageComparator implements Comparator<HLocale> {
        private SortingType sortingType;

        @Setter
        private Long selectedDocumentId;

        public LanguageComparator(SortingType sortingType) {
            this.sortingType = sortingType;
        }

        @Override
        public int compare(HLocale compareFrom, HLocale compareTo) {
            final HLocale item1, item2;

            if (sortingType.isDescending()) {
                item1 = compareFrom;
                item2 = compareTo;
            } else {
                item1 = compareTo;
                item2 = compareFrom;
            }

            SortingType.SortOption selectedSortOption =
                    sortingType.getSelectedSortOption();

            // Need to get statistic for comparison
            if (!selectedSortOption.equals(SortingType.SortOption.ALPHABETICAL)) {
                WordStatistic wordStatistic1;
                WordStatistic wordStatistic2;

                if (selectedDocumentId == null) {
                    wordStatistic1 =
                            getStatisticsForLocale(item1.getLocaleId());
                    wordStatistic2 =
                            getStatisticsForLocale(item2.getLocaleId());
                } else {
                    wordStatistic1 =
                            getStatisticForDocument(selectedDocumentId,
                                    item1.getLocaleId());
                    wordStatistic2 =
                            getStatisticForDocument(selectedDocumentId,
                                    item2.getLocaleId());
                }
                return compareWordStatistic(wordStatistic1, wordStatistic2,
                        selectedSortOption);
            } else {
                return item1.retrieveDisplayName().compareTo(
                        item2.retrieveDisplayName());
            }
        }
    }

}
