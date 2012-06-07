/*
 * Copyright 2012, Red Hat, Inc. and individual contributors as indicated by the
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

package org.zanata.webtrans.client.events;

import java.util.List;

import org.zanata.common.ContentState;
import com.google.common.collect.Lists;
import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Patrick Huang <a href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class TransUnitSaveEvent extends GwtEvent<TransUnitSaveEventHandler>
{
   public static Type<TransUnitSaveEventHandler> TYPE = new Type<TransUnitSaveEventHandler>();
   private List<String> targets = Lists.newArrayList();
   private ContentState status;

   public TransUnitSaveEvent(List<String> targets, ContentState status)
   {
      this.targets = targets;
      this.status = status;
   }

   public Type<TransUnitSaveEventHandler> getAssociatedType()
   {
      return TYPE;
   }

   protected void dispatch(TransUnitSaveEventHandler handler)
   {
      handler.onTransUnitSave(this);
   }

   public List<String> getTargets()
   {
      return targets;
   }

   public ContentState getStatus()
   {
      return status;
   }
}