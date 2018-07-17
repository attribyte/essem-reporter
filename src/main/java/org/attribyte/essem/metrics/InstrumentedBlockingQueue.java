/*
 * Copyright 2018 Attribyte, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.attribyte.essem.metrics;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A blocking queue with hooks for instrumentation.
 * @param <E> The class.
 */
public class InstrumentedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

   /**
    * Creates an instrumented blocking queue.
    * @param delegate The underlying queue.
    * @param mutationHandler A handler for queue events.
    */
	public InstrumentedBlockingQueue(final BlockingQueue<E> delegate,
                                    final BlockingQueueEventHandler mutationHandler) {
	   this.delegate = delegate;
	   this.eventHandler = mutationHandler;
	}

	@Override
	public E poll() {
		final E polled = delegate.poll();
		if(polled != null) eventHandler.removed(1);
		return polled;
	}

	@Override
	public E peek() {
	   return delegate.peek();
	}

	@Override
	public boolean offer(E e) {
		final boolean enqueued = delegate.offer(e);
		if(enqueued) {
		   eventHandler.added(1);
		} else {
		   eventHandler.failedOffer();
		}
		return enqueued;
	}

	@Override
	public void put(E e) throws InterruptedException {
		delegate.put(e);
		eventHandler.added(1);
	}

	@Override
	public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
		final boolean enqueued = delegate.offer(e, timeout, unit);
		if(enqueued) {
		   eventHandler.added(1);
		} else {
		   eventHandler.failedOffer();
		}
		return enqueued;
	}

	@Override
	public E take() throws InterruptedException {
		final E taken = delegate.take();
		eventHandler.removed(1);
		return taken;
	}

	@Override
	public E poll(long timeout, TimeUnit unit) throws InterruptedException {
		final E polled = delegate.poll(timeout, unit);
		if(polled != null) {
         eventHandler.removed(1);
      }
		return polled;
	}

	@Override
	public int remainingCapacity() {
		return delegate.remainingCapacity();
	}

	@Override
	@SuppressWarnings("unchecked")
	public int drainTo(Collection<? super E> c) {
		int removed = delegate.drainTo(c);
		if(removed > 0) {
		   eventHandler.removed(removed);
      }
      return removed;
	}

	@Override
	@SuppressWarnings("unchecked")
	public int drainTo(Collection<? super E> c, int maxElements) {
		int removed = delegate.drainTo(c, maxElements);
		if(removed > 0) {
		   eventHandler.removed(removed);
      }
      return removed;
	}

   @Override
   public int size() {
      return delegate.size();
   }

   @Override
   public Iterator<E> iterator() {
      return new InstrumentedIterator<E>(delegate.iterator(), eventHandler);
   }

	@Override
  	public boolean equals(Object o) {
		return delegate.equals(o);
	}

	@Override
  	public int hashCode() {
		return delegate.hashCode();
	}

	/**
    * The underlying queue.
	 */
	private final BlockingQueue<E> delegate;

   /**
    * The handler for mutation events.
    */
   private final BlockingQueueEventHandler eventHandler;
}