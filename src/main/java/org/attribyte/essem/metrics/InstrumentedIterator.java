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

import java.util.Iterator;

/**
 * An iterator that notifies a mutation handler when items are removed through {@code remove}.
 * @param <T> The type.
 */
public class InstrumentedIterator<T> implements Iterator<T> {

	/**
	 * Creates an instrumented iterator.
	 * @param delegate The iterator to which operations are delegated.
	 * @param mutationHandler A handler that accepts mutation events.
	 */
	public InstrumentedIterator(final Iterator<T> delegate, final CollectionMutationHandler mutationHandler) {
		this.delegate = delegate;
		this.mutationHandler = mutationHandler;
	}

	@Override
	public T next() {
		return delegate.next();
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public void remove() {
		this.delegate.remove();
		this.mutationHandler.removed(1);
	}

	/**
	 * The iterator to which operations are delegated.
	 */
	private final Iterator<T> delegate;

	/**
	 * The handler for mutation events.
	 */
	private final CollectionMutationHandler mutationHandler;
}