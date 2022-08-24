package com.nesterovskyBros;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;

import java.util.function.Supplier;

/**
 * <p>A store to keep objects by multiple weak keys.
 * Value is kept only if all keys are alive, otherwise value is reclaimed.</p>
 * <p><b>Note:</b> do not have strong references to all keys in an instance
 * of T kept in the store.</p>
 * <p><b>Note:</b> class is thread safe.</p>
 *
 * @param <T> an instance type to store.
 */
public class WeakStore<T>
{
  /**
   * Gets an instance by keys.
   * @param keys an array of keys.
   * @return an instance, if available.
   */
  public T get(Object ...keys)
  {
    poll();
    
    Key<T> value = store.get(new Key<>(keys));
    
    return value == null ? null : value.value;
  }

  /**
   * <p>Gets or creates an instance, if it was not in the store, by keys.</p>
   * <p><b>Note:</b> If create is called then it runs within lock. 
   * Its computation should be short and simple, and must not attempt to 
   * update any other mappings of this store.</p>
   * @param create a value factory.
   * @param keys an array of keys.
   * @return an instance.
   */
  public T getOrCreate(Supplier<T> create, Object ...keys)
  {
    poll();
    
    Key<T> next = store.computeIfAbsent(
      new Key<>(keys),
      k -> 
      {
        k.makeRefs(queue);
        k.value = create.get();
        
        return k;
      });
    
    return next.value;
  }
  
  /**
   * Sets or removes an instance by keys.
   * @param value a value to set, or {@code null} to remove.
   * @param keys an array of keys.
   * @return a replaced instance.
   */
  public T set(T value, Object ...keys)
  {
    poll();
    
    Key<T> key = new Key<>(keys);
    T prevValue = null;
    
    if (value == null)
    {
      Key<T> prev = store.remove(key);
      
      if (prev != null)
      {
        prevValue = prev.value;
        prev.clear();
      }
    }
    else
    {
      Key<T> next = store.compute(
        key, 
        (k, v) ->
        {
          if (v != null)
          {
            key.value = v.value;
            v.value = value;

            return v;
          }
          else
          {
            k.makeRefs(queue);
            k.value = value;
            
            return k;
          }
        });
      
      if (next != key)
      {
        prevValue = key.value;
      }
    }

    if ((prevValue != null) && (prevValue != value))
    {
      release(prevValue);
    }
    
    return prevValue;
  }

  /**
   * <p>Polls this store and cleans reclaimed data.</p>
   * <p><b>Note:</b> this method is automatically called by 
   * {@link #get(Object...)}, {@link #getOrCreate(Supplier, Object...)}, and 
   * {@link #set(Object, Object...)} methods.
   */
  public void poll()
  {
    while(true)
    {
      Reference<?> ref = queue.poll();
      
      if (ref == null)
      {
        break;
      }
      
      @SuppressWarnings("unchecked")
      Key<T> key = ((Ref<T>)ref).key;
      T value = key.value;
      
      if (value != null)
      {
        store.remove(key);
        key.clear();
        release(value);
      }
    }
  }

  /**
   * Called when value is released.
   * @param value a value to release.
   */
  protected void release(T value)
  {
  }
  
  private static class Key<T>
  {
    public Key(Object[] keys)
    {
      int hashCode = 0;
      
      for(int i = 0; i < keys.length; ++i)
      {
        hashCode ^= Objects.requireNonNull(keys[i]).hashCode();
      }
      
      this.hashCode = hashCode;
      this.keys = keys;
    }
    
    @SuppressWarnings("unchecked")
    public void clear()
    {
      value = null;

      for(int i = 0; i < keys.length; ++i)
      {
        Object key = keys[i];
        
        keys[i] = null;

        if (key instanceof Ref)
        {
          ((Ref<T>)key).clear();
        }
      }
    }
    
    @Override
    public int hashCode()
    {
      return hashCode;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }

      Key<T> that = (Key<T>)obj;
      
      if (keys.length != that.keys.length)
      {
        return false;
      }
      
      for(int i = 0; i < keys.length; ++i)
      {
        Object value = keys[i];
        
        if (value instanceof Ref)
        {
          value = ((Ref<T>)value).get();
        }
        
        Object thatValue = that.keys[i];
        
        if (thatValue instanceof Ref)
        {
          thatValue = ((Ref<T>)thatValue).get();
        }

        if ((value != thatValue) || (value == null))
        {
          return false;
        }
      }
      
      return true;
    }
    
    private void makeRefs(ReferenceQueue<Object> queue)
    {
      Object[] keys = new Object[this.keys.length];
      
      for(int i = 0; i < keys.length; ++i)
      {
        keys[i] = new Ref<T>(this.keys[i], queue, this);
      }
      
      this.keys = keys;
    }

    private final int hashCode;
    private Object[] keys;
    private T value;
  }
  
  private static class Ref<T> extends WeakReference<Object>
  {
    public Ref(Object value, ReferenceQueue<Object> queue, Key<T> key)
    {
      super(value, queue);
      this.key = key;
    }
    
    private final Key<T> key;
  }
  
  private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
  private final ConcurrentHashMap<Key<T>, Key<T>> store = 
    new ConcurrentHashMap<>();
}
