package org.zanata.webtrans.client.presenter;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import net.customware.gwt.presenter.client.EventBus;

import org.easymock.Capture;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zanata.common.LocaleId;
import org.zanata.common.TransUnitCount;
import org.zanata.common.TransUnitWords;
import org.zanata.common.TranslationStats;
import org.zanata.webtrans.client.events.DocumentSelectionEvent;
import org.zanata.webtrans.client.events.DocumentStatsUpdatedEvent;
import org.zanata.webtrans.client.events.DocumentStatsUpdatedEventHandler;
import org.zanata.webtrans.client.events.NotificationEvent;
import org.zanata.webtrans.client.events.NotificationEvent.Severity;
import org.zanata.webtrans.client.events.NotificationEventHandler;
import org.zanata.webtrans.client.events.ProjectStatsUpdatedEvent;
import org.zanata.webtrans.client.events.ProjectStatsUpdatedEventHandler;
import org.zanata.webtrans.client.events.WorkspaceContextUpdateEvent;
import org.zanata.webtrans.client.events.WorkspaceContextUpdateEventHandler;
import org.zanata.webtrans.client.history.History;
import org.zanata.webtrans.client.history.HistoryToken;
import org.zanata.webtrans.client.history.Window;
import org.zanata.webtrans.client.history.Window.Location;
import org.zanata.webtrans.client.presenter.AppPresenter.Display;
import org.zanata.webtrans.client.resources.WebTransMessages;
import org.zanata.webtrans.shared.auth.Identity;
import org.zanata.webtrans.shared.model.DocumentId;
import org.zanata.webtrans.shared.model.DocumentInfo;
import org.zanata.webtrans.shared.model.Person;
import org.zanata.webtrans.shared.model.WorkspaceContext;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HasVisibility;

@Test(groups = { "unit-tests" })
public class AppPresenterTest
{

   private static final String TEST_PERSON_NAME = "Mister Ed";
   private static final String TEST_WORKSPACE_NAME = "Test Workspace Name";
   private static final String TEST_LOCALE_NAME = "Test Locale Name";
   private static final String TEST_WINDOW_TITLE = "Test Window Title";

   private static final String WORKSPACE_TITLE_QUERY_PARAMETER_KEY = "title";
   private static final String NO_DOCUMENTS_STRING = "No document selected";
   private static final String TEST_DOCUMENT_NAME = "test_document_name";
   private static final String TEST_DOCUMENT_PATH = "test/document/path/";
   private static final String TEST_WORKSPACE_TITLE = "test workspace title";


   private AppPresenter appPresenter;

   HasClickHandlers mockDismiss;
   HasVisibility mockDismissVisibility;
   Display mockDisplay;
   DocumentListPresenter mockDocumentListPresenter;
   HasClickHandlers mockDocumentsLink;
   EventBus mockEventBus;
   History mockHistory;
   Identity mockIdentity;
   HasClickHandlers mockLeaveWorkspaceLink;
   WebTransMessages mockMessages;
   Person mockPerson;
   HasClickHandlers mockSearchLink;
   SearchResultsPresenter mockSearchResultsPresenter;
   HasClickHandlers mockSignoutLink;
   TranslationPresenter mockTranslationPresenter;
   Window mockWindow;
   Location mockWindowLocation;
   WorkspaceContext mockWorkspaceContext;

   private Capture<ClickHandler> capturedDismissLinkClickHandler;
   private Capture<ClickHandler> capturedDocumentLinkClickHandler;
   private Capture<DocumentSelectionEvent> capturedDocumentSelectionEvent;
   private Capture<DocumentStatsUpdatedEventHandler> capturedDocumentStatsUpdatedEventHandler;
   private Capture<String> capturedHistoryTokenString;
   private Capture<ValueChangeHandler<String>> capturedHistoryValueChangeHandler;
   private Capture<ClickHandler> capturedLeaveWorkspaceLinkClickHandler;
   private Capture<NotificationEventHandler> capturedNotificationEventHandler;
   private Capture<ProjectStatsUpdatedEventHandler> capturedProjectStatsUpdatedEventHandler;
   private Capture<ClickHandler> capturedSearchLinkClickHandler;
   private Capture<ClickHandler> capturedSignoutLinkClickHandler;
   private Capture<WorkspaceContextUpdateEventHandler> capturedWorkspaceContextUpdatedEventHandler;

   private DocumentInfo testDocInfo;
   private DocumentId testDocId;
   private TranslationStats testDocStats;
   private TranslationStats emptyProjectStats;

   @BeforeClass
   public void createMocks()
   {
      mockDismiss = createMock(HasClickHandlers.class);
      mockDismissVisibility = createMock(HasVisibility.class);
      mockDisplay = createMock(AppPresenter.Display.class);
      mockDocumentListPresenter = createMock(DocumentListPresenter.class);
      mockDocumentsLink = createMock(HasClickHandlers.class);
      mockEventBus = createMock(EventBus.class);
      mockHistory = createMock(History.class);
      mockIdentity = createMock(Identity.class);
      mockLeaveWorkspaceLink = createMock(HasClickHandlers.class);
      mockMessages = createMock(WebTransMessages.class);
      mockPerson = createMock(Person.class);
      mockSearchLink = createMock(HasClickHandlers.class);
      mockSearchResultsPresenter = createMock(SearchResultsPresenter.class);
      mockSignoutLink = createMock(HasClickHandlers.class);
      mockTranslationPresenter = createMock(TranslationPresenter.class);
      mockWindow = createMock(Window.class);
      mockWindowLocation = createMock(Window.Location.class);
      mockWorkspaceContext = createMock(WorkspaceContext.class);

      capturedDismissLinkClickHandler = new Capture<ClickHandler>();
      capturedDocumentLinkClickHandler = new Capture<ClickHandler>();
      capturedDocumentSelectionEvent = new Capture<DocumentSelectionEvent>();
      capturedDocumentStatsUpdatedEventHandler = new Capture<DocumentStatsUpdatedEventHandler>();
      capturedHistoryTokenString = new Capture<String>();
      capturedHistoryValueChangeHandler = new Capture<ValueChangeHandler<String>>();
      capturedLeaveWorkspaceLinkClickHandler = new Capture<ClickHandler>();
      capturedNotificationEventHandler = new Capture<NotificationEventHandler>();
      capturedProjectStatsUpdatedEventHandler = new Capture<ProjectStatsUpdatedEventHandler>();
      capturedSearchLinkClickHandler = new Capture<ClickHandler>();
      capturedSignoutLinkClickHandler = new Capture<ClickHandler>();
      capturedWorkspaceContextUpdatedEventHandler = new Capture<WorkspaceContextUpdateEventHandler>();
   }

   @BeforeMethod
   void beforeMethod()
   {
      resetAllMocks();
      resetAllCaptures();

      setupDefaultMockExpectations();

      appPresenter = new AppPresenter(mockDisplay, mockEventBus, mockTranslationPresenter, mockDocumentListPresenter, mockSearchResultsPresenter, mockIdentity, mockWorkspaceContext, mockMessages, mockHistory, mockWindow, mockWindowLocation);
   }

   // Note: unable to test 'sign out' and 'close window' links as these have
   // static method calls to Application

   // TODO test that initial history state is handled properly

   public void testPerformsRequiredActionsOnBind()
   {

      // default mock expectations include:
      // - bind doclistpresenter
      // - bind translationpresenter
      // - show documents view initially
      // - set user label
      // - set workspace name + title
      // - set window title
      // - show 'No document selected'
      // - show (intitially empty) project stats

      replayAllMocks();

      appPresenter.bind();

      verifyAllMocks();
   }

   public void testShowsNotificationEvents()
   {
      String testMessage = "test notification message";
      mockDisplay.setNotificationMessage(testMessage, Severity.Warning);
      expectLastCall().once();
      mockDismissVisibility.setVisible(true);
      expectLastCall().once();

      replayAllMocks();

      appPresenter.bind();
      NotificationEvent notification = new NotificationEvent(Severity.Warning, testMessage);
      capturedNotificationEventHandler.getValue().onNotification(notification);

      verifyAllMocks();
   }

   public void testShowsUpdatedProjectStats()
   {
      Capture<TranslationStats> capturedTranslationStats = new Capture<TranslationStats>();
      mockDisplay.setStats(and(capture(capturedTranslationStats), isA(TranslationStats.class)));
      expectLastCall().once();

      replayAllMocks();

      appPresenter.bind();
      TranslationStats testStats = new TranslationStats(new TransUnitCount(6, 5, 4), new TransUnitWords(3, 2, 1));
      ProjectStatsUpdatedEvent event = new ProjectStatsUpdatedEvent(testStats);
      capturedProjectStatsUpdatedEventHandler.getValue().onProjectStatsRetrieved(event);

      verifyAllMocks();
      assertThat(capturedTranslationStats.getValue(), is(equalTo(testStats)));
   }

   public void testUpdateProjectStatsFromEditorView()
   {
      TranslationStats newProjectStats = new TranslationStats(new TransUnitCount(9, 9, 9), new TransUnitWords(9, 9, 9));

      expectLoadDocAndViewEditor();
      expectReturnToDocListView(newProjectStats);

      replayAllMocks();

      appPresenter.bind();
      HistoryToken token = simulateLoadDocAndViewEditor();
      // set stats to allow differentiation from doc stats
      capturedProjectStatsUpdatedEventHandler.getValue().onProjectStatsRetrieved(new ProjectStatsUpdatedEvent(newProjectStats));
      simulateReturnToDocListView(token);

      verifyAllMocks();
   }

   public void testHistoryTriggersDocumentSelectionEvent()
   {
      replayAllMocks();
      appPresenter.bind();
      verifyAllMocks();

      reset(mockDisplay, mockDocumentListPresenter);

      // not testing for specific values for the following in this test
      mockDisplay.setDocumentLabel(notNull(String.class), notNull(String.class));
      expectLastCall().anyTimes();
      mockDisplay.setStats(notNull(TranslationStats.class));
      expectLastCall().anyTimes();

      buildTestDocumentInfo();
      expect(mockDocumentListPresenter.getDocumentId(TEST_DOCUMENT_PATH + TEST_DOCUMENT_NAME)).andReturn(testDocId).anyTimes();
      expect(mockDocumentListPresenter.getDocumentInfo(testDocId)).andReturn(testDocInfo).anyTimes();

      replay(mockDisplay, mockDocumentListPresenter);

      HistoryToken docSelectionToken = new HistoryToken();
      docSelectionToken.setDocumentPath(TEST_DOCUMENT_PATH + TEST_DOCUMENT_NAME);
      capturedHistoryValueChangeHandler.getValue().onValueChange(new ValueChangeEvent<String>(docSelectionToken.toTokenString())
      {
      });

      assertThat("a new document path in history should trigger a document selection event with the correct id", capturedDocumentSelectionEvent.getValue().getDocumentId(), is(testDocId));
   }

   public void testHistoryTriggersViewChange()
   {
      replayAllMocks();
      appPresenter.bind();
      verifyAllMocks();

      reset(mockDisplay, mockDocumentListPresenter);

      buildTestDocumentInfo();
      expect(mockDocumentListPresenter.getDocumentId(TEST_DOCUMENT_PATH + TEST_DOCUMENT_NAME)).andReturn(testDocId).anyTimes();
      expect(mockDocumentListPresenter.getDocumentInfo(testDocId)).andReturn(testDocInfo).anyTimes();
      mockDisplay.showInMainView(MainView.Editor);
      expectLastCall().once();
      // avoid checking name or stats for this test
      mockDisplay.setDocumentLabel(notNull(String.class), notNull(String.class));
      expectLastCall().anyTimes();
      mockDisplay.setStats(notNull(TranslationStats.class));
      expectLastCall().anyTimes();

      replay(mockDisplay, mockDocumentListPresenter);

      HistoryToken docInEditorToken = buildDocInEditorToken();
      capturedHistoryValueChangeHandler.getValue().onValueChange(new ValueChangeEvent<String>(docInEditorToken.toTokenString())
      {
      });
      verify(mockDisplay);
   }

   public void testNoEditorWithoutValidDocument()
   {
      replayAllMocks();
      appPresenter.bind();
      verifyAllMocks();

      reset(mockDisplay, mockDocumentListPresenter);
      // return invalid document
      expect(mockDocumentListPresenter.getDocumentId(notNull(String.class))).andReturn(null).anyTimes();
      replay(mockDisplay, mockDocumentListPresenter);

      HistoryToken editorWithoutDocToken = new HistoryToken();
      simulateReturnToEditorView(editorWithoutDocToken);

      // not expecting show view editor
      verify(mockDisplay);
   }


   public void testHistoryTriggersDocumentNameStatsUpdate()
   {
      replayAllMocks();
      appPresenter.bind();
      verifyAllMocks();

      reset(mockDisplay, mockDocumentListPresenter);

      buildTestDocumentInfo();
      expect(mockDocumentListPresenter.getDocumentId(TEST_DOCUMENT_PATH + TEST_DOCUMENT_NAME)).andReturn(testDocId).anyTimes();
      expect(mockDocumentListPresenter.getDocumentInfo(testDocId)).andReturn(testDocInfo).anyTimes();
      // avoid checking for view change, tested elsewhere
      mockDisplay.showInMainView(isA(MainView.class));
      expectLastCall().anyTimes();
      mockDisplay.setDocumentLabel(TEST_DOCUMENT_PATH, TEST_DOCUMENT_NAME);
      expectLastCall().once();
      mockDisplay.setStats(eq(testDocStats));
      expectLastCall().once();

      replay(mockDisplay, mockDocumentListPresenter);

      HistoryToken docInEditorToken = new HistoryToken();
      docInEditorToken.setView(MainView.Editor);
      docInEditorToken.setDocumentPath(TEST_DOCUMENT_PATH + TEST_DOCUMENT_NAME);
      capturedHistoryValueChangeHandler.getValue().onValueChange(new ValueChangeEvent<String>(docInEditorToken.toTokenString())
      {
      });

      verify(mockDisplay);
   }


   /**
    * Note: this also verifies that editor pending change is saved when changing
    * from editor to document list
    */
   public void testStatsAndNameChangeWithView()
   {
      expectLoadDocAndViewEditor();
      expectReturnToDocListView(emptyProjectStats);
      expectReturnToEditorView(testDocStats);

      replayAllMocks();
      appPresenter.bind();
      HistoryToken token = simulateLoadDocAndViewEditor();
      simulateReturnToDocListView(token);
      simulateReturnToEditorView(token);

      verifyAllMocks();
   }


   public void testShowsUpdatedDocumentStats()
   {
      expectLoadDocAndViewEditor();
      replayAllMocks();
      appPresenter.bind();

      // must be in editor to see document stats
      simulateLoadDocAndViewEditor();
      verifyAllMocks();

      TranslationStats updatedStats = new TranslationStats(new TransUnitCount(9, 9, 9), new TransUnitWords(9, 9, 9));

      reset(mockDisplay);
      mockDisplay.setStats(eq(updatedStats));
      expectLastCall().once();
      replay(mockDisplay);
      capturedDocumentStatsUpdatedEventHandler.getValue().onDocumentStatsUpdated(new DocumentStatsUpdatedEvent(testDocId, updatedStats));

      verify(mockDisplay);
   }

   public void testDoesNotShowWrongDocumentStats()
   {
      expectLoadDocAndViewEditor();
      replayAllMocks();
      appPresenter.bind();
      simulateLoadDocAndViewEditor();
      verifyAllMocks();

      TranslationStats updatedStats = new TranslationStats(new TransUnitCount(9, 9, 9), new TransUnitWords(9, 9, 9));

      reset(mockDisplay);
      // not expecting stats change
      replay(mockDisplay);

      DocumentId notSelectedDocId = new DocumentId(7777L);
      capturedDocumentStatsUpdatedEventHandler.getValue().onDocumentStatsUpdated(new DocumentStatsUpdatedEvent(notSelectedDocId, updatedStats));

      verify(mockDisplay);
   }

   public void testUpdateDocumentStatsFromDoclistView()
   {
      TranslationStats updatedStats = new TranslationStats(new TransUnitCount(9, 9, 9), new TransUnitWords(9, 9, 9));

      expectLoadDocAndViewEditor();
      expectReturnToDocListView(emptyProjectStats);
      expectReturnToEditorView(updatedStats);
      replayAllMocks();

      appPresenter.bind();
      HistoryToken token = simulateLoadDocAndViewEditor();
      simulateReturnToDocListView(token);
      //update document stats
      capturedDocumentStatsUpdatedEventHandler.getValue().onDocumentStatsUpdated(new DocumentStatsUpdatedEvent(testDocId, updatedStats));
      simulateReturnToEditorView(token);

      verifyAllMocks();
   }

   public void testDismiss()
   {
      String testMessage = "testing";

      mockDisplay.setNotificationMessage(testMessage, Severity.Error);
      mockDismissVisibility.setVisible(true); // visible upon message
      mockDisplay.setNotificationMessage("", Severity.Info);
      mockDismissVisibility.setVisible(false); // invisible upon clear

      replayAllMocks();
      appPresenter.bind();
      NotificationEvent notification = new NotificationEvent(Severity.Error, testMessage);
      capturedNotificationEventHandler.getValue().onNotification(notification);
      ClickEvent event = new ClickEvent()
      {
      };
      capturedDismissLinkClickHandler.getValue().onClick(event);

      verify(mockDismissVisibility, mockDisplay);
   }

   public void testDocumentsLinkGeneratesHistoryToken()
   {
      ClickEvent docLinkClickEvent = createMock(ClickEvent.class);

      // 1 - click doc link from default state
      expect(mockHistory.getToken()).andReturn("").once();

      // 2 - load a document in the editor
      expectLoadDocAndViewEditor();

      // 3 - click doc link to return to doclist
      HistoryToken expectedDocInEditorToken = buildDocInEditorToken();
      expect(mockHistory.getToken()).andReturn(expectedDocInEditorToken.toTokenString()).once();
      expectReturnToDocListView(emptyProjectStats);

      // 4 - click doc link to return to editor
      HistoryToken expectedDocListWithLoadedDocToken = new HistoryToken();
      expectedDocListWithLoadedDocToken.setDocumentPath(TEST_DOCUMENT_PATH + TEST_DOCUMENT_NAME);
      expect(mockHistory.getToken()).andReturn(expectedDocListWithLoadedDocToken.toTokenString()).once();

      // NOTE not expecting return to editor view as this test does not simulate
      // the event for the new history item

      replayAllMocks();


      appPresenter.bind();
      //discard captured tokens from bind to allow easy check for new token
      capturedHistoryTokenString.reset();

      // 1 - no doc loaded, don't generate MainView.Editor token
      capturedDocumentLinkClickHandler.getValue().onClick(docLinkClickEvent);
      assertThat(capturedHistoryTokenString.hasCaptured(), is(false));

      // 2 - load doc in editor
      HistoryToken token = simulateLoadDocAndViewEditor();

      // 3 - doc loaded in editor, return to doclist
      capturedDocumentLinkClickHandler.getValue().onClick(docLinkClickEvent);
      HistoryToken returnToDoclistToken = HistoryToken.fromTokenString(capturedHistoryTokenString.getValue());
      assertThat("clicking documents link should always show doclist when editor is visible", returnToDoclistToken.getView(), is(MainView.Documents));
      assertThat("document path should be maintained when clicking documents link", returnToDoclistToken.getDocumentPath(), is(token.getDocumentPath()));

      // simulate history token event for new token
      capturedHistoryValueChangeHandler.getValue().onValueChange(new ValueChangeEvent<String>(returnToDoclistToken.toTokenString())
      {
      });

      // 4 - doc loaded, return to editor
      capturedDocumentLinkClickHandler.getValue().onClick(docLinkClickEvent);
      HistoryToken returnToEditorToken = HistoryToken.fromTokenString(capturedHistoryTokenString.getValue());
      assertThat("clicking documents link should show editor when doclist is visible and a valid document is selected", returnToEditorToken.getView(), is(MainView.Editor));
      assertThat("document path should be maintained when clicking documents link", returnToEditorToken.getDocumentPath(), is(token.getDocumentPath()));

      // NOTE not simulating history change event for newest history token

      // TODO could check that filter parameters haven't changed as well

      verifyAllMocks();
   }

   public void testSearchLinkGeneratesHistoryToken()
   {
      expect(mockHistory.getToken()).andReturn("").once();
      replayAllMocks();
      appPresenter.bind();
      //simulate click
      ClickEvent searchLinkClickEvent = createMock(ClickEvent.class);
      capturedSearchLinkClickHandler.getValue().onClick(searchLinkClickEvent);
      HistoryToken capturedToken = HistoryToken.fromTokenString(capturedHistoryTokenString.getValue());
      assertThat("clicking search link should set view in history token to search", capturedToken.getView(), is(MainView.Search));
      //TODO could check that nothing else has changed in token
      verifyAllMocks();
   }

   public void testShowsHidesReadonlyLabel()
   {
      replayAllMocks();
      appPresenter.bind();
      verifyAllMocks();

      // display expect show readonly
      reset(mockDisplay);
      mockDisplay.setReadOnlyVisible(true);
      expectLastCall().once();
      // simulate workspace readonly event
      WorkspaceContextUpdateEvent mockEvent = createMock(WorkspaceContextUpdateEvent.class);
      expect(mockEvent.isReadOnly()).andReturn(true).anyTimes();
      replay(mockDisplay, mockEvent);
      capturedWorkspaceContextUpdatedEventHandler.getValue().onWorkspaceContextUpdated(mockEvent);
      verify(mockDisplay);

      // display expect hide readonly
      reset(mockDisplay, mockEvent);
      mockDisplay.setReadOnlyVisible(false);
      expectLastCall().once();
      // simulate workspace editable event
      expect(mockEvent.isReadOnly()).andReturn(false).anyTimes();
      replay(mockDisplay, mockEvent);
      capturedWorkspaceContextUpdatedEventHandler.getValue().onWorkspaceContextUpdated(mockEvent);
      verify(mockDisplay);
   }

   /**
    * generates new test doc id and doc info ready for use in tests
    */
   private void buildTestDocumentInfo()
   {
      testDocId = new DocumentId(2222L);
      TransUnitCount unitCount = new TransUnitCount(1, 2, 3);
      TransUnitWords wordCount = new TransUnitWords(4, 5, 6);
      testDocStats = new TranslationStats(unitCount, wordCount);
      testDocInfo = new DocumentInfo(testDocId, TEST_DOCUMENT_NAME, TEST_DOCUMENT_PATH, LocaleId.EN_US, testDocStats);
   }

   /**
    * @see #simulateLoadDocAndViewEditor()
    */
   private void expectLoadDocAndViewEditor()
   {
      buildTestDocumentInfo();
      expect(mockDocumentListPresenter.getDocumentId(TEST_DOCUMENT_PATH + TEST_DOCUMENT_NAME)).andReturn(testDocId).anyTimes();
      expect(mockDocumentListPresenter.getDocumentInfo(testDocId)).andReturn(testDocInfo).anyTimes();

      // test document name and stats should be shown
      mockDisplay.setDocumentLabel(TEST_DOCUMENT_PATH, TEST_DOCUMENT_NAME);
      expectLastCall().once();
      mockDisplay.setStats(eq(testDocStats));
      expectLastCall().once();

      mockDisplay.showInMainView(MainView.Editor);
      expectLastCall().once();
   }

   /**
    * @see #expectLoadDocAndViewEditor()
    * @return history token representing the editor view and loaded document
    */
   private HistoryToken simulateLoadDocAndViewEditor()
   {
      HistoryToken docInEditorToken = buildDocInEditorToken();
      capturedHistoryValueChangeHandler.getValue().onValueChange(new ValueChangeEvent<String>(docInEditorToken.toTokenString())
      {
      });
      return docInEditorToken;
   }

   /**
    * Generate a token representing the default test document being viewed in
    * the editor
    * 
    * @return the newly generated token
    */
   private HistoryToken buildDocInEditorToken()
   {
      HistoryToken docInEditorToken = new HistoryToken();
      docInEditorToken.setDocumentPath(TEST_DOCUMENT_PATH + TEST_DOCUMENT_NAME);
      docInEditorToken.setView(MainView.Editor);
      return docInEditorToken;
   }


   /**
    * Sets expectations to show the documents view, update the document label,
    * show the given project stats, and save pending editor changes
    * 
    * @see #simulateUpdateProjectStatsThenReturnToDocListView(TranslationStats, HistoryToken)
    * @param projectStats the current project stats that should be displayed
    */
   private void expectReturnToDocListView(TranslationStats projectStats)
   {
      //expect return to document view
      mockDisplay.showInMainView(MainView.Documents);
      expectLastCall().once();
      mockDisplay.setDocumentLabel("", NO_DOCUMENTS_STRING);
      expectLastCall().once();
      mockDisplay.setStats(eq(projectStats));
      expectLastCall().once();
      mockTranslationPresenter.saveEditorPendingChange();
      expectLastCall().once();
   }


   /**
    * @see {@link #expectUpdateProjectStatsThenReturnToDocListView(TranslationStats)}
    */
   private void simulateReturnToDocListView(HistoryToken previousToken)
   {
      previousToken.setView(MainView.Documents);
      //simulate return to document list view
      capturedHistoryValueChangeHandler.getValue().onValueChange(new ValueChangeEvent<String>(previousToken.toTokenString())
      {
      });
   }

   /**
    * Return to the editor view (requires test document already loaded)
    * 
    * @param previousToken a token representing the state of the application
    *           before returning to the editor view
    * @see #expectReturnToEditorView(TranslationStats)
    */
   private void simulateReturnToEditorView(HistoryToken previousToken)
   {
      previousToken.setView(MainView.Editor);
      capturedHistoryValueChangeHandler.getValue().onValueChange(new ValueChangeEvent<String>(previousToken.toTokenString())
      {
      });
   }

   /**
    * @see #simulateReturnToEditorView(HistoryToken)
    * @param documentStats the stats object that has been set for the given document
    */
   private void expectReturnToEditorView(TranslationStats documentStats)
   {
      mockDisplay.showInMainView(MainView.Editor);
      expectLastCall().once();
      mockDisplay.setDocumentLabel(TEST_DOCUMENT_PATH, TEST_DOCUMENT_NAME);
      expectLastCall().once();
      mockDisplay.setStats(eq(documentStats));
      expectLastCall().once();
   }

   private void setupDefaultMockExpectations()
   {
      expect(mockDisplay.getSignOutLink()).andReturn(mockSignoutLink).anyTimes();
      expect(mockDisplay.getLeaveWorkspaceLink()).andReturn(mockLeaveWorkspaceLink).anyTimes();
      expect(mockDisplay.getDocumentsLink()).andReturn(mockDocumentsLink).anyTimes();
      expect(mockDisplay.getSearchLink()).andReturn(mockSearchLink).anyTimes();
      expect(mockDisplay.getDismiss()).andReturn(mockDismiss).anyTimes();
      expect(mockDisplay.getDismissVisibility()).andReturn(mockDismissVisibility).anyTimes();

      mockDismissVisibility.setVisible(false); // starts invisible
      expectLastCall().once();

      mockDisplay.showInMainView(MainView.Documents);
      expectLastCall().once(); //starts on document list view

      mockDisplay.setDocumentLabel("", NO_DOCUMENTS_STRING);
      expectLastCall().once();
      mockDisplay.setUserLabel(TEST_PERSON_NAME);
      expectLastCall().anyTimes();
      mockDisplay.setWorkspaceNameLabel(TEST_WORKSPACE_NAME, TEST_WORKSPACE_TITLE);
      expectLastCall().anyTimes();
      mockDisplay.setReadOnlyVisible(false);
      expectLastCall().once();

      // initially empty project stats
      emptyProjectStats = new TranslationStats();
      mockDisplay.setStats(eq(emptyProjectStats));
      expectLastCall().once();

      mockDocumentListPresenter.bind();
      expectLastCall().once();

      expectClickHandlerRegistrationOnce(mockDocumentsLink, capturedDocumentLinkClickHandler);
      expectClickHandlerRegistrationOnce(mockSearchLink, capturedSearchLinkClickHandler);
      expectClickHandlerRegistrationOnce(mockDismiss, capturedDismissLinkClickHandler);
      expectClickHandlerRegistrationOnce(mockLeaveWorkspaceLink, capturedLeaveWorkspaceLinkClickHandler);
      expectClickHandlerRegistrationOnce(mockSignoutLink, capturedSignoutLinkClickHandler);

      expect(mockEventBus.addHandler(eq(NotificationEvent.getType()), and(capture(capturedNotificationEventHandler), isA(NotificationEventHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();
      expect(mockEventBus.addHandler(eq(DocumentStatsUpdatedEvent.getType()), and(capture(capturedDocumentStatsUpdatedEventHandler), isA(DocumentStatsUpdatedEventHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();
      expect(mockEventBus.addHandler(eq(ProjectStatsUpdatedEvent.getType()), and(capture(capturedProjectStatsUpdatedEventHandler), isA(ProjectStatsUpdatedEventHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();

      expect(mockEventBus.addHandler(eq(WorkspaceContextUpdateEvent.getType()), and(capture(capturedWorkspaceContextUpdatedEventHandler), isA(WorkspaceContextUpdateEventHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();

      mockEventBus.fireEvent(and(capture(capturedDocumentSelectionEvent), isA(DocumentSelectionEvent.class)));
      expectLastCall().anyTimes();

      setupMockHistory();

      expect(mockIdentity.getPerson()).andReturn(mockPerson).anyTimes();


      expect(mockMessages.windowTitle(TEST_WORKSPACE_NAME, TEST_LOCALE_NAME)).andReturn(TEST_WINDOW_TITLE).anyTimes();
      expect(mockMessages.noDocumentSelected()).andReturn(NO_DOCUMENTS_STRING).anyTimes();

      expect(mockPerson.getName()).andReturn(TEST_PERSON_NAME).anyTimes();


      mockSearchResultsPresenter.bind();
      expectLastCall().once();

      mockTranslationPresenter.bind();
      expectLastCall().once();

      mockWindow.setTitle(TEST_WINDOW_TITLE);
      expectLastCall().once();

      expect(mockWindowLocation.getParameter(WORKSPACE_TITLE_QUERY_PARAMETER_KEY)).andReturn(TEST_WORKSPACE_TITLE).anyTimes();

      expect(mockWorkspaceContext.getWorkspaceName()).andReturn(TEST_WORKSPACE_NAME).anyTimes();
      expect(mockWorkspaceContext.getLocaleName()).andReturn(TEST_LOCALE_NAME).anyTimes();
      expect(mockWorkspaceContext.isReadOnly()).andReturn(false).anyTimes();
   }

   /**
    * Expect a single handler registration on a mock object, and capture the
    * click handler in the given {@link Capture}
    * 
    * @param mockObjectToClick
    * @param captureForHandler
    */
   private void expectClickHandlerRegistrationOnce(HasClickHandlers mockObjectToClick, Capture<ClickHandler> captureForHandler)
   {
      expect(mockObjectToClick.addClickHandler(and(capture(captureForHandler), isA(ClickHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();
   }

   @SuppressWarnings("unchecked")
   private void setupMockHistory()
   {
      expect(mockHistory.addValueChangeHandler(and(capture(capturedHistoryValueChangeHandler), isA(ValueChangeHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();
      mockHistory.fireCurrentHistoryState();
      expectLastCall().anyTimes();

      mockHistory.newItem(capture(capturedHistoryTokenString));
      expectLastCall().anyTimes();
   }

   private void resetAllMocks()
   {
      reset(mockDisplay, mockDocumentListPresenter, mockDocumentsLink);
      reset(mockEventBus, mockHistory, mockIdentity, mockLeaveWorkspaceLink);
      reset(mockMessages, mockPerson, mockSearchResultsPresenter, mockSignoutLink);
      reset(mockTranslationPresenter, mockWindow, mockWindowLocation, mockWorkspaceContext);
      reset(mockDismiss, mockDismissVisibility, mockSearchLink);
   }

   private void resetAllCaptures()
   {
      capturedDismissLinkClickHandler.reset();
      capturedDocumentLinkClickHandler.reset();
      capturedDocumentSelectionEvent.reset();
      capturedDocumentStatsUpdatedEventHandler.reset();
      capturedHistoryTokenString.reset();
      capturedHistoryValueChangeHandler.reset();
      capturedLeaveWorkspaceLinkClickHandler.reset();
      capturedNotificationEventHandler.reset();
      capturedProjectStatsUpdatedEventHandler.reset();
      capturedSearchLinkClickHandler.reset();
      capturedSignoutLinkClickHandler.reset();
      capturedWorkspaceContextUpdatedEventHandler.reset();
   }

   private void replayAllMocks()
   {
      replay(mockDisplay, mockDocumentListPresenter, mockDocumentsLink);
      replay(mockEventBus, mockHistory, mockIdentity, mockLeaveWorkspaceLink);
      replay(mockMessages, mockPerson, mockSearchResultsPresenter, mockSignoutLink);
      replay(mockTranslationPresenter, mockWindow, mockWindowLocation, mockWorkspaceContext);
      replay(mockDismiss, mockDismissVisibility, mockSearchLink);
   }

   private void verifyAllMocks()
   {
      verify(mockDisplay, mockDocumentListPresenter, mockDocumentsLink);
      verify(mockEventBus, mockHistory, mockIdentity, mockLeaveWorkspaceLink);
      verify(mockMessages, mockPerson, mockSearchResultsPresenter, mockSignoutLink);
      verify(mockTranslationPresenter, mockWindow, mockWindowLocation, mockWorkspaceContext);
      verify(mockDismiss, mockDismissVisibility, mockSearchLink);
   }
}
