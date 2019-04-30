package com.nesterovskyBros;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A pool of values.
 * @param <T> an instance type to store in the pool.
 */
public class WeakPool<T>
{
  /**
   * Tests whether a value is contained in the pool.
   * @param value a value to test.
   * @return {@code true} if value is in the pool, and {@code false} otherwise.
   */
  public boolean contains(T value)
  {
    poll();
    
    return store.contains(new Ref<T>(value, queue));
  }

  /**
   * Gets an instance by value.
   * @param value a value to get from pool.
   * @return a pooled value.
   */
  public T get(T value)
  {
    poll();

    Ref<T> ref = new Ref<T>(value, queue);
    
    while(true)
    {
      Ref<T> oldRef = store.putIfAbsent(ref, ref);
      
      if (oldRef == null)
      {
        return value;
      }
      
      T oldValue = oldRef.get();
      
      if (oldValue != null)
      {
        return oldValue;
      }
      
      store.remove(oldRef);
    }
  }
  
  /**
   * <p>Polls this store and cleans reclaimed data.</p>
   * <p><b>Note:</b> this method is automatically called by 
   * {@link #contains(T)} and {@link #get(T)} methods.
   */
  public void poll()
  {
    while(true)
    {
      @SuppressWarnings("unchecked")
      Ref<T> ref = (Ref<T>)queue.poll();
      
      if (ref == null)
      {
        break;
      }
      
      store.remove(ref);
    }
  }
  
  private static class Ref<T> extends WeakReference<T>
  {
    public Ref(T value, ReferenceQueue<T> queue)
    {
      super(value, queue);
      this.hashCode = value.hashCode();
    }
    
    @Override
    public int hashCode()
    {
      return hashCode;
    }
    
    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }

      @SuppressWarnings("unchecked")
      Ref<T> that = (Ref<T>)obj;
      
      if (hashCode != that.hashCode)
      {
        return false;
      }
      
      T value = get();
      T thatValue = that.get();
      
      return (value != null) && (thatValue != null) && value.equals(thatValue);
    }

    private final int hashCode;
  }
  
  private final ReferenceQueue<T> queue = new ReferenceQueue<>();
  private final ConcurrentHashMap<Ref<T>, Ref<T>> store = 
    new ConcurrentHashMap<>();
}
