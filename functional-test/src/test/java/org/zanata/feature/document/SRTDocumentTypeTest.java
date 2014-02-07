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
package org.zanata.feature.document;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zanata.feature.DetailedTest;
import org.zanata.page.projects.ProjectSourceDocumentsPage;
import org.zanata.page.webtrans.EditorPage;
import org.zanata.util.CleanDocumentStorageRule;
import org.zanata.util.SampleProjectRule;
import org.zanata.util.TestFileGenerator;
import org.zanata.workflow.BasicWorkFlow;
import org.zanata.workflow.LoginWorkFlow;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.zanata.util.FunctionalTestHelper.assumeFalse;

/**
 * @author Damian Jansen <a
 *         href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Category(DetailedTest.class)
public class SRTDocumentTypeTest {
    @Rule
    public SampleProjectRule sampleProjectRule = new SampleProjectRule();

    @Rule
    public CleanDocumentStorageRule documentStorageRule =
            new CleanDocumentStorageRule();

    private TestFileGenerator testFileGenerator = new TestFileGenerator();

    @Before
    public void before() {
        new BasicWorkFlow().goToHome().deleteCookiesAndRefresh();
        String documentStorageDirectory =
                CleanDocumentStorageRule.getDocumentStoragePath()
                        .concat(File.separator).concat("documents")
                        .concat(File.separator);
        assumeFalse("", new File(documentStorageDirectory).exists());
    }

    @Test
    public void uploadSrtFile() {
        String sep = System.getProperty("line.separator");
        String testFileContent ="1" + sep + "00:00:01,000 --> 00:00:02,000" +
                sep + "Test subtitle 1";
        File srtfile = testFileGenerator.generateTestFileWithContent(
                "testsrtfile", ".srt", testFileContent);
        String testFileName = srtfile.getName();
        String successfullyUploaded =
                "Document file " + testFileName + " uploaded.";

        ProjectSourceDocumentsPage projectSourceDocumentsPage =
                new LoginWorkFlow().signIn("admin", "admin").goToProjects()
                        .goToProject("about fedora").goToVersion("master")
                        .goToSourceDocuments().pressUploadFileButton()
                        .enterFilePath(srtfile.getAbsolutePath())
                        .submitUpload();

        assertThat("Document uploaded notification shows",
                projectSourceDocumentsPage.getNotificationMessage(),
                Matchers.equalTo(successfullyUploaded));
        assertThat("Document shows in table",
                projectSourceDocumentsPage.sourceDocumentsContains(srtfile
                        .getName()));

        EditorPage editorPage = projectSourceDocumentsPage.goToProjects()
                .goToProject("about fedora").goToVersion("master")
                .translate("pl").clickDocumentLink("", testFileName);

        assertThat("The first translation source is correct",
                editorPage.getTranslationSourceAtRowIndex(0),
                Matchers.equalTo("Test subtitle 1"));

    }
}
