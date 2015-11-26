package org.javameta.pubsub;

import org.javameta.observer.ObserverHandler;
import org.javameta.observer.Observers;

/**
 * 
 */
public class SubscriptionHandler {

    private final ObserverHandler handler = new ObserverHandler();

    public void add(Class publisherClass, Class eventClass, Observers.Handler handler) {
        this.handler.add(publisherClass, eventClass, handler);
    }

    public void add(SubscriptionHandler other) {
        handler.add(other.handler);
    }

    public int unregister(Class eventClass, Class publisherClass) {
        return handler.unregister(eventClass, publisherClass);
    }

    public int unregister(Class eventClass) {
        return handler.unregister(eventClass);
    }

    public int unregisterAll(Class publisherClass) {
        return handler.unregisterAll(publisherClass);
    }

    public int unregisterAll() {
        return handler.unregisterAll();
    }
}