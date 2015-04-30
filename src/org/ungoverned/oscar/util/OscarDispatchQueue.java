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

import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.SynchronousBundleListener;
import org.ungoverned.oscar.Oscar;

/**
 * This is a subclass of <tt>DispatchQueue</tt> that specifically adds support
 * for <tt>SynchronousBundleListener</tt>s; the OSGi specification
 * says that synchronous bundle listeners should receive their events
 * immediately, i.e., they should not be delivered on a separate thread
 * like is the case with the <tt>DispatchQueue</tt>. To achieve this
 * functionality, this class overrides the dispatch method to automatically
 * fire any bundle events to synchronous bundle listeners using the
 * calling thread. In order to ensure that synchronous bundle listeners
 * do not receive an event twice, it wraps the passed in <tt>Dipatcher</tt>
 * instance so that it filters synchronous bundle listeners.
**/
public class OscarDispatchQueue extends DispatchQueue
{
    /**
     * Dispatches an event to a set of event listeners using a specified
     * dispatcher object. This overrides the definition of the super class
     * to deliver events to <tt>ServiceListener</tt>s and
     * <tt>SynchronousBundleListener</tt>s using
     * the calling thread instead of the event dispatching thread. All
     * other events are still delivered asynchronously.
     *
     * @param dispatcher the dispatcher used to actually dispatch the event; this
     *          varies according to the type of event listener.
     * @param clazz the class associated with the target event listener type;
     *              only event listeners of this type will receive the event.
     * @param eventObj the actual event object to dispatch.
    **/
    public void dispatch(Dispatcher dispatcher, Class clazz, EventObject eventObj)
    {
        Object[] listeners = getListeners();

        // If this is an event for service listeners, then dispatch it
        // immediately since service events are never asynchronous.
        if ((clazz == ServiceListener.class) && (listeners.length > 0))
        {
            // Notify appropriate listeners.
            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                // If the original listener is a synchronous bundle listener
                // or a service listener, then dispatch event immediately
                // per the specification.
                ListenerWrapper lw = (ListenerWrapper) listeners[i + 1];
                if (lw.getListenerClass() == ServiceListener.class)
                {
                    try {
                        dispatcher.dispatch(
                            (EventListener) lw, eventObj);
                    } catch (Throwable th) {
                        Oscar.error("OscarDispatchQueue: Error during dispatch.", th);
                    }
                }
            }
        }
        // Dispatch bundle events to synchronous bundle listeners immediately,
        // but deliver to standard bundle listeners asynchronously.
        else if ((clazz == BundleListener.class) && (listeners.length > 0))
        {
            // Notify appropriate listeners.
            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                // If the original listener is a synchronous bundle listener,
                // then dispatch event immediately per the specification.
                ListenerWrapper lw = (ListenerWrapper) listeners[i + 1];
                if (lw.getListenerClass() == SynchronousBundleListener.class)
                {
                    try {
                        dispatcher.dispatch(
                            (EventListener) lw, eventObj);
                    } catch (Throwable th) {
                        Oscar.error("OscarDispatchQueue: Error during dispatch.", th);
                    }
                }
            }

            // Wrap the dispatcher so that it ignores synchronous
            // bundle listeners since they have already been dispatched.
            IgnoreSynchronousDispatcher ignoreDispatcher = new IgnoreSynchronousDispatcher();
            ignoreDispatcher.setDispatcher(dispatcher);

            // Dispatch the bundle listener asynchronously.
            dispatch(listeners, ignoreDispatcher, clazz, eventObj);
        }
        // All other events are dispatched asynchronously.
        else
        {
            dispatch(listeners, dispatcher, clazz, eventObj);
        }
    }

    private static class IgnoreSynchronousDispatcher implements Dispatcher
    {
        private Dispatcher m_dispatcher = null;

        public void setDispatcher(Dispatcher dispatcher)
        {
            m_dispatcher = dispatcher;
        }

        public void dispatch(EventListener l, EventObject eventObj)
        {
            if (l instanceof ListenerWrapper)
            {
                ListenerWrapper lw = (ListenerWrapper) l;
                // Do not dispatch events to synchronous listeners,
                // since they are dispatched immediately above.
                if (!(lw.getListenerClass() == SynchronousBundleListener.class))
                {
                    m_dispatcher.dispatch(l, eventObj);
                }
            }
        }
    }
}