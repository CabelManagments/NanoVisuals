package ru.nanovisuals.events;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventBus {

    private static final EventBus INSTANCE = new EventBus();

    public static EventBus getInstance() {
        return INSTANCE;
    }

    private final Map<Class<? extends Event>, List<Handler>> handlers = new HashMap<>();

    public synchronized void subscribe(Object listener) {
        Class<?> cls = listener.getClass();
        while (cls != null && cls != Object.class) {
            for (Method method : cls.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(EventHandler.class)) continue;
                if (method.getParameterCount() != 1) continue;

                Class<?> paramType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(paramType)) continue;

                method.setAccessible(true);
                EventHandler annotation = method.getAnnotation(EventHandler.class);

                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) paramType;

                Handler handler = new Handler(listener, method, annotation.priority());
                List<Handler> list = handlers.computeIfAbsent(eventClass, k -> new ArrayList<>());

                if (list.stream().noneMatch(h -> h.target == listener && h.method.equals(method))) {
                    list.add(handler);
                    list.sort(Comparator.comparingInt((Handler h) -> h.priority).reversed());
                }
            }
            cls = cls.getSuperclass();
        }
    }

    public synchronized void unsubscribe(Object listener) {
        for (List<Handler> list : handlers.values()) {
            list.removeIf(h -> h.target == listener);
        }
    }

    public void post(Event event) {
        List<Handler> snapshot;
        synchronized (this) {
            List<Handler> list = handlers.get(event.getClass());
            if (list == null || list.isEmpty()) return;
            snapshot = new ArrayList<>(list);
        }

        for (Handler handler : snapshot) {
            if (event.isCancelled()) break;
            try {
                handler.method.invoke(handler.target, event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    private static final class Handler {
        final Object target;
        final Method method;
        final int priority;

        Handler(Object target, Method method, int priority) {
            this.target = target;
            this.method = method;
            this.priority = priority;
        }
    }
}
