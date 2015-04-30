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

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;

import org.ungoverned.oscar.Oscar;

/**
 * This class implements an event dispatching queue to simplify delivering
 * events to a list of event listener. To use this class, simply create an
 * instance and use it to keep track of your event listeners, much like
 * <tt>javax.swing.event.EventListenerList</tt>. To dispatch an event,
 * simply create an instance of a <tt>Dispatcher</tt> and pass the instance
 * to <tt>DispatchQueue.dispatch()</tt> method, for example:
 * <pre>
 *  Dispatcher d = new Dispatcher() {
 *      public void dispatch(EventListener l, Object eventObj)
 *      {
 *          ((FooListener) l).fooXXX((FooEvent) eventObj);
 *      }
 *  };
 *  FooEvent event = new FooEvent(this);
 *  dispatchQueue.dispatch(d, FooListener.class, event);
 * </pre>
 * In the above code substitute a specific listener and event for the
 * <tt>Foo</tt> listener and event. Since <tt>Dispatcher</tt>s are
 * reusable, it is probably a good idea to create one for each type of
 * event to be delivered and just reuse them everytime to avoid unnecessary
 * memory allocation.
 * <p>
 * Currently, the <tt>DispatchQueue</tt> creates an internal thread with
 * which all events are delivered; this means that events are never delivered
 * using the caller's thread.
**/
public class DispatchQueue
{
    // Representation of an empty listener list.
    private static final Object[] m_emptyList = new Object[0];

    // The event listeners for a particular queue instance.
    private Object[] m_listeners = m_emptyList;

    // A single thread is used to deliver events for all dispatchers.
    private static Thread m_thread = null;
    private static String m_threadLock = "thread lock";
    private static boolean m_stopping = false;
    private static boolean m_stopped = false;

    // List of dispatch requests.
    private static final ArrayList m_requestList = new ArrayList();
    // Cached dispatch requests to avoid memory allocation.
    private static final ArrayList m_requestCache = new ArrayList();

    /**
     * Constructs a dispatch queue and starts a dispather thread if
     * necessary.
    **/
    public DispatchQueue()
    {
        synchronized (m_threadLock)
        {
            // Start event dispatching thread if necessary.
            if (m_thread == null)
            {
                m_thread = new Thread(new Runnable() {
                    public void run()
                    {
                        DispatchQueue.run();
                    }
                }, "OscarDispatchQueue");
                m_thread.start();
            }
        }
    }

    /**
     * Terminates the dispatching thread for a graceful shutdown
     * of the dispatching queue; the caller will block until the
     * dispatching thread has completed all pending dispatches.
     * Since there is only one thread per all instances of
     * <tt>DispatchQueue</tt>, this method should only be called
     * prior to exiting the JVM.
    **/
    public static void shutdown()
    {
        synchronized (m_threadLock)
        {
            // Return if already stopped.
            if (m_stopped)
            {
                return;
            }

            // Signal dispatch thread.
            m_stopping = true;
            synchronized (m_requestList)
            {
                m_requestList.notify();
            }

            // Wait for dispatch thread to stop.
            while (!m_stopped)
            {
                try {
                    m_threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    /**
     * Returns a pointer to the array of event listeners. The array stores pairs
     * of associated <tt>Class</tt> and <tt>EventListener</tt> objects; a pair
     * corresponds to the arguments passed in to the <tt>addListener()</tt> method.
     * Even numbered array elements are the class object and odd numbered elements
     * are the corresponding event listener instance.
     *
     * @return guaranteed to return a non-null object array.
    **/
    public Object[] getListeners()
    {
        return m_listeners;
    }

    /**
     * Returns the listener if it is already in the dispatch queue.
     * @param clazz the class of the listener to find.
     * @param l the listener instance to find.
     * @return the listener instance or <tt>null</tt> if the listener was
     *         not found.
    **/
    public EventListener getListener(Class clazz, EventListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }
        else if (!clazz.isInstance(l))
        {
            throw new IllegalArgumentException(
                "Listener not of type " + clazz.getName());
        }

        // Lock the object.
        synchronized (this)
        {
            // Try to find the instance in our list.
            int idx = -1;
            for (int i = 0; i < m_listeners.length; i += 2)
            {
                if ((m_listeners[i] == clazz) &&
                    (m_listeners[i + 1].equals(l)))
                {
                    return (EventListener) m_listeners[i + 1];
                }
            }
        }
        
        return null;
    }

    /**
     * Adds a listener to the dispatch queue's listener list; the listener
     * is then able to receive events.
     *
     * @param clazz the class object associated with the event listener type.
     * @param l the instance of the event listener to add.
    **/
    public void addListener(Class clazz, EventListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }
        else if (!clazz.isInstance(l))
        {
            throw new IllegalArgumentException(
                "Listener not of type " + clazz.getName());
        }

        // Lock the object.
        synchronized (this)
        {
            // If we have no listeners, then just add the new listener.
            if (m_listeners == m_emptyList)
            {
                m_listeners = new Object[] { clazz, l };
            }
            // Otherwise, we need to do some array copying.
            // Notice, the old array is always valid, so if
            // the dispatch thread is in the middle of a dispatch,
            // then it has a reference to the old listener array
            // and is not affected by the new value.
            else
            {
                Object[] newList = new Object[m_listeners.length + 2];
                System.arraycopy(m_listeners, 0, newList, 0, m_listeners.length);
                newList[m_listeners.length] = clazz;
                newList[m_listeners.length + 1] = l;
                m_listeners = newList;
            }
        }
    }

    /**
     * Removes a listener from the dispatch queue's listener list; the listener
     * is no longer able to receive events.
     *
     * @param clazz the class object associated with the event listener type.
     * @param l the instance of the event listener to remove.
    **/
    public void removeListener(Class clazz, EventListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }
        else if (!clazz.isInstance(l))
        {
            throw new IllegalArgumentException(
                "Listener not of type " + clazz.getName());
        }

        // Lock the object.
        synchronized (this)
        {
            // Try to find the instance in our list.
            int idx = -1;
            for (int i = 0; i < m_listeners.length; i += 2)
            {
                if ((m_listeners[i] == clazz) &&
                    (m_listeners[i + 1].equals(l)))
                {
                    idx = i;
                    break;
                }
            }

            // If we have the instance, then remove it.
            if (idx >= 0)
            {
                // If this is the last listener, then point to empty list.
                if ((m_listeners.length - 2) == 0)
                {
                    m_listeners = m_emptyList;
                }
                // Otherwise, we need to do some array copying.
                // Notice, the old array is always valid, so if
                // the dispatch thread is in the middle of a dispatch,
                // then it has a reference to the old listener array
                // and is not affected by the new value.
                else
                {
                    Object[] newList  = new Object[m_listeners.length - 2];
                    System.arraycopy(m_listeners, 0, newList, 0, idx);
                    if (idx < newList.length)
                    {
                        System.arraycopy(m_listeners, idx + 2, newList, idx,
                            newList.length - idx);
                    }
                    m_listeners = newList;
                }
            }
        }
    }

    /**
     * Dispatches an event to a set of event listeners using a specified
     * dispatcher object.
     *
     * @param d the dispatcher used to actually dispatch the event; this
     *          varies according to the type of event listener.
     * @param clazz the class associated with the target event listener type;
     *              only event listeners of this type will receive the event.
     * @param eventObj the actual event object to dispatch.
    **/
    public void dispatch(Dispatcher d, Class clazz, EventObject eventObj)
    {
        dispatch(m_listeners, d, clazz, eventObj);
    }

    protected void dispatch(
        Object[] listeners, Dispatcher d, Class clazz, EventObject eventObj)
    {
        // If dispatch thread is stopped, then ignore dispatch request.
        if (m_stopped)
        {
            return;
        }

        // First get a dispatch request from the cache or
        // create one if necessary.
        DispatchRequest dr = null;
        synchronized (m_requestCache)
        {
            if (m_requestCache.size() > 0)
                dr = (DispatchRequest) m_requestCache.remove(0);
            else
                dr = new DispatchRequest();
        }

        // Initialize dispatch request.
        dr.m_listeners = listeners;
        dr.m_dispatcher = d;
        dr.m_clazz = clazz;
        dr.m_eventObj = eventObj;

        // Lock the request list.
        synchronized (m_requestList)
        {
            // Add our request to the list.
            m_requestList.add(dr);
            // Notify the dispatch thread that there is
            // work to do.
            m_requestList.notify();
        }
    }

    private static void run()
    {
        DispatchRequest dr = null;
        while (true)
        {
            // Lock the request list so we can try to get a
            // dispatch request from it.
            synchronized (m_requestList)
            {
                // Wait while there are no requests to dispatch. If the
                // dispatcher thread is supposed to stop, then let the
                // dispatcher thread exit the loop and stop.
                while ((m_requestList.size() == 0) && !m_stopping)
                {
                    // Wait until some signals us for work.
                    try {
                        m_requestList.wait();
                    } catch (InterruptedException ex) {
                        Oscar.error("Dispatch thread error.", ex);
                    }
                }

                // If there are no events to dispatch and shutdown
                // has been called then exit, otherwise dispatch event.
                if ((m_requestList.size() == 0) && (m_stopping))
                {
                    synchronized (m_threadLock)
                    {
                        m_stopped = true;
                        m_threadLock.notifyAll();
                    }
                    return;
                }

                // Get the dispatch request.
                dr = (DispatchRequest) m_requestList.remove(0);
            }

            // Deliver event outside of synchronized block
            // so that we don't block other requests from being
            // queued during event processing.

            // Try to dispatch to the listeners.
            if (dr.m_listeners.length > 0)
            {
                // Notify appropriate listeners.
                for (int i = dr.m_listeners.length - 2; i >= 0; i -= 2)
                {
                    if (dr.m_listeners[i] == dr.m_clazz)
                    {
                        try {
                            dr.m_dispatcher.dispatch(
                                (EventListener) dr.m_listeners[i + 1], dr.m_eventObj);
                        } catch (Throwable th) {
                            Oscar.error("DispatchQueue: Error during dispatch.", th);
                        }
                    }
                }
            }

            // Put dispatch request in cache.
            synchronized (m_requestCache)
            {
                m_requestCache.add(dr);
            }
        }
    }

    private static class DispatchRequest
    {
        public Object[] m_listeners = null;
        public Dispatcher m_dispatcher = null;
        public Class m_clazz = null;
        public EventObject m_eventObj = null;
    }
}