package com.freerdp.freerdpcore.services;

import java.util.HashMap;
import java.util.Map;

final class FreeRdpInstanceRegistry
{
	private enum State
	{
		CONNECTING,
		CONNECTED,
		DISCONNECTING
	}

	private final Map<Long, State> states = new HashMap<>();

	synchronized boolean beginConnect(long instance)
	{
		if (states.containsKey(instance))
			return false;

		states.put(instance, State.CONNECTING);
		notifyAll();
		return true;
	}

	synchronized boolean completeConnect(long instance)
	{
		final State state = states.get(instance);
		if (state == null)
			return false;
		if (state == State.DISCONNECTING)
			return false;

		states.put(instance, State.CONNECTED);
		notifyAll();
		return true;
	}

	synchronized boolean completeFailure(long instance)
	{
		final State state = states.remove(instance);
		notifyAll();
		return state != null && state != State.DISCONNECTING;
	}

	synchronized boolean completeDisconnect(long instance)
	{
		final State state = states.remove(instance);
		notifyAll();
		return state != null;
	}

	synchronized boolean requestDisconnect(long instance)
	{
		final State state = states.get(instance);
		if (state == null)
			return false;
		if (state == State.DISCONNECTING)
			return false;

		states.put(instance, State.DISCONNECTING);
		notifyAll();
		return true;
	}

	synchronized boolean isTracked(long instance)
	{
		return states.containsKey(instance);
	}

	synchronized void awaitInactive(long instance)
	{
		while (states.containsKey(instance))
		{
			try
			{
				wait();
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				throw new RuntimeException(
				    "Interrupted while waiting for FreeRDP instance shutdown", e);
			}
		}
	}
}
