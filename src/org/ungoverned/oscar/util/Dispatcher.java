/*
 * Oscar - An implementation of the OSGi framework.
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *  
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 * 
**/
package org.ungoverned.oscar.util;

import java.util.EventListener;
import java.util.EventObject;

/**
 * This interface is used by <tt>DispatchQueue</tt> to dispatch events.
 * Generally speaking, each type of event to dispatch will have an instance
 * of a <tt>Dispatcher</tt> so that the dispatch queue can dispatch to
 * the appropriate listener method for the specific listener type,
 * for example:
 * <pre>
 *  Dispatcher d = new Dispatcher() {
 *      public void dispatch(EventListener l, EventObject eventObj)
 *      {
 *          ((FooListener) l).fooXXX((FooEvent) eventObj);
 *      }
 *  };
 *  FooEvent event = new FooEvent(this);
 *  dispatchQueue.dispatch(d, FooListener.class, event);
 * </pre>
 * In the above code substitute a specific listener and event for the
 * <tt>Foo</tt> listener and event. <tt>Dispatcher</tt>s can be reused, so
 * it is a good idea to cache them to avoid unnecessary memory allocations.
**/
public interface Dispatcher
{
    /**
     * Dispatch an event to a specified event listener.
     *
     * @param l the event listener to receive the event.
     * @param eventObj the event to dispatch.
    **/
    public void dispatch(EventListener l, EventObject eventObj);
}
