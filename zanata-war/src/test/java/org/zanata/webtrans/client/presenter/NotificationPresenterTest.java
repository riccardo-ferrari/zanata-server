package org.zanata.webtrans.client.presenter;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import net.customware.gwt.presenter.client.EventBus;

import org.easymock.Capture;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zanata.webtrans.client.events.NotificationEvent;
import org.zanata.webtrans.client.events.NotificationEvent.Severity;
import org.zanata.webtrans.client.events.NotificationEventHandler;
import org.zanata.webtrans.client.presenter.NotificationPresenter.Display;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent.Type;
import com.google.gwt.event.shared.HandlerRegistration;

@Test(groups = { "unit-tests" })
public class NotificationPresenterTest
{
   private NotificationPresenter notificationPresenter;

   HasClickHandlers mockDismiss;
   HasClickHandlers mockClear;

   Display mockDisplay;
   EventBus mockEventBus;

   private Capture<ClickHandler> capturedDismissClickHandler;
   private Capture<ClickHandler> capturedClearClickHandler;
   private Capture<NotificationEventHandler> capturedNotificationEventHandler;
   
   private final static int MSG_TO_KEEP = 5;

   @BeforeClass
   public void createMocks()
   {
      mockDismiss = createMock(HasClickHandlers.class);
      mockClear = createMock(HasClickHandlers.class);
      mockDisplay = createMock(NotificationPresenter.Display.class);
      mockEventBus = createMock(EventBus.class);
      // mockWindow = createMock(Window.class);

      capturedDismissClickHandler = new Capture<ClickHandler>();
      capturedClearClickHandler = new Capture<ClickHandler>();
      capturedNotificationEventHandler = new Capture<NotificationEventHandler>();
   }

   @BeforeMethod
   void beforeMethod()
   {
      resetAllMocks();
      resetAllCaptures();

      setupDefaultMockExpectations();

      notificationPresenter = new NotificationPresenter(mockDisplay, mockEventBus);
   }

   public void testOnBind()
   {
      replayAllMocks();
      notificationPresenter.bind();
      verifyAllMocks();
   }

   public void testErrorNotificationShows()
   {
      String testMessage = "error testing";

      mockDisplay.appendMessage(Severity.Error, testMessage);
      expectLastCall().once();

      mockDisplay.setPopupTopRightCorner();
      expectLastCall().once();

      mockDisplay.show();
      expectLastCall().once();

      replayAllMocks();
      notificationPresenter.bind();
      NotificationEvent notification = new NotificationEvent(Severity.Error, testMessage);
      capturedNotificationEventHandler.getValue().onNotification(notification);

      verifyAllMocks();
   }

   public void testErrorMessageCount()
   {
      String[] testMessages = { "test1", "test2", "test3", "test4", "test5" };

      for (String msg : testMessages)
      {
         mockDisplay.appendMessage(Severity.Error, msg);
         expectLastCall().once();
      }

      mockDisplay.setPopupTopRightCorner();
      expectLastCall().times(testMessages.length);

      mockDisplay.show();
      expectLastCall().times(testMessages.length);

      expect(mockDisplay.getMessageCount()).andReturn(testMessages.length);

      replayAllMocks();
      notificationPresenter.bind();
      for (String msg : testMessages)
      {
         NotificationEvent notification = new NotificationEvent(Severity.Error, msg);
         capturedNotificationEventHandler.getValue().onNotification(notification);
      }
      assertThat(notificationPresenter.getMessageCount(), is(testMessages.length));

      verifyAllMocks();
   }

   public void testErrorMessageCountExceedMax()
   {
      String[] testMessages = { "test1", "test2", "test3", "test4", "test5", "test6", "test7" };

      for (String msg : testMessages)
      {
         mockDisplay.appendMessage(Severity.Error, msg);
         expectLastCall().once();
      }

      mockDisplay.setPopupTopRightCorner();
      expectLastCall().times(testMessages.length);

      mockDisplay.show();
      expectLastCall().times(testMessages.length);

      expect(mockDisplay.getMessageCount()).andReturn(MSG_TO_KEEP);

      replayAllMocks();
      notificationPresenter.bind();
      for (String msg : testMessages)
      {
         NotificationEvent notification = new NotificationEvent(Severity.Error, msg);
         capturedNotificationEventHandler.getValue().onNotification(notification);
      }
      assertThat(notificationPresenter.getMessageCount(), is(MSG_TO_KEEP));

      verifyAllMocks();
   }

   private void setupDefaultMockExpectations()
   {
      expectHandlerRegistrations();
      expectPresenterSetupActions();
      setupMockGetterReturnValues();
   }

   @SuppressWarnings("unchecked")
   private void expectHandlerRegistrations()
   {
      expectClickHandlerRegistration(mockDismiss, capturedDismissClickHandler);
      expectClickHandlerRegistration(mockClear, capturedClearClickHandler);
      expectEventHandlerRegistration(NotificationEvent.getType(), NotificationEventHandler.class, capturedNotificationEventHandler);
   }

   /**
    * Expect a single handler registration on a mock object, and capture the
    * click handler in the given {@link Capture}
    * 
    * @param mockObjectToClick
    * @param captureForHandler
    */
   private void expectClickHandlerRegistration(HasClickHandlers mockObjectToClick, Capture<ClickHandler> captureForHandler)
   {
      expect(mockObjectToClick.addClickHandler(and(capture(captureForHandler), isA(ClickHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();
   }

   private <H extends EventHandler> void expectEventHandlerRegistration(Type<H> expectedType, Class<H> expectedClass, Capture<H> handlerCapture)
   {
      expect(mockEventBus.addHandler(eq(expectedType), and(capture(handlerCapture), isA(expectedClass)))).andReturn(createMock(HandlerRegistration.class)).once();
   }

   private void expectPresenterSetupActions()
   {
      mockDisplay.setModal(true);
      expectLastCall().once();
      mockDisplay.setAutoHideEnabled(true);
      expectLastCall().once();
      mockDisplay.setAnimationEnabled(true);
      expectLastCall().once();
      mockDisplay.hide(true);
      expectLastCall().once();
      mockDisplay.setMessagesToKeep(MSG_TO_KEEP);
      expectLastCall().once();
   }

   private void setupMockGetterReturnValues()
   {
      expect(mockDisplay.getDismissButton()).andReturn(mockDismiss).anyTimes();
      expect(mockDisplay.getClearButton()).andReturn(mockClear).anyTimes();
   }


   private void resetAllCaptures()
   {
      capturedNotificationEventHandler.reset();
      capturedDismissClickHandler.reset();
      capturedClearClickHandler.reset();
   }

   private void resetAllMocks()
   {
      reset(mockDisplay, mockEventBus, mockDismiss, mockClear);
   }

   private void replayAllMocks()
   {
      replay(mockDisplay, mockEventBus, mockDismiss, mockClear);
   }

   private void verifyAllMocks()
   {
      verify(mockDisplay, mockEventBus, mockDismiss, mockClear);
   }
}