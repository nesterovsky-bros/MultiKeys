package com.nesterovskyBros;

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
    
    Key<T> key = new Key<>(queue, keys);
    Key<T> value = store.get(key);
    
    key.clear();
    
    return value == null ? null : value.value;
  }
  
  /**
   * <p>Gets or creates an instance, if it was not in the store, by keys.</p>
   * <p><b>Note:</b> If factory is called then it runs within lock. 
   * Its computation should be short and simple, and must not attempt to 
   * update any other mappings of this store.</p>
   * @param factory a value factory.
   * @param keys an array of keys.
   * @return an instance.
   */
  public T getOrCreate(Supplier<T> factory, Object ...keys)
  {
    poll();
    
    Key<T> key = new Key<>(queue, keys);
    
    Key<T> value = store.computeIfAbsent(
      key,
      k -> 
      {
        k.value = Objects.requireNonNull(factory.get());
        
        return k;
      });
    
    if (key.value == null)
    {
      key.clear();
    }
    
    return value.value;
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
    
    Key<T> key = new Key<>(queue, keys);
    Key<T> prev;  
    
    if (value == null)
    {
      prev = store.remove(key);  
      key.clear();
    }
    else
    {
      key.value = value;
      prev = store.put(key, key);  
    }

    if (prev == null)
    {
      return null;
    }
    
    value = prev.value;
    prev.clear();
    
    return value;
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
      @SuppressWarnings("unchecked")
      Ref<T> ref = (Ref<T>)queue.poll();
      
      if (ref == null)
      {
        break;
      }
      
      Key<T> key = ref.key;
      
      if (key.value != null)
      {
        key.clear();
        store.remove(key);
      }
    }
  }
  
  private static class Key<T>
  {
    @SuppressWarnings("unchecked")
    public Key(ReferenceQueue<Object> queue, Object ...keys)
    {
      int hashCode = 0;
      
      refs = new Ref[keys.length];
      
      for(int i = 0; i < keys.length; ++i)
      {
        Object key = Objects.requireNonNull(keys[i]);

        hashCode ^= key.hashCode();
        refs[i] = new Ref<T>(key, queue, this);
      }
      
      this.hashCode = hashCode;
    }
    
    public void clear()
    {
      value = null;

      for(int i = 0; i < refs.length; ++i)
      {
        refs[i].clear();
      }
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

      if (!(obj instanceof Key))
      {
        return false;
      }
      
      @SuppressWarnings("unchecked")
      Key<T> that = (Key<T>)obj;
      
      if (hashCode != that.hashCode)
      {
        return false;
      }
      
      if (refs.length != that.refs.length)
      {
        return false;
      }
      
      for(int i = 0; i < refs.length; ++i)
      {
        Object value = refs[i].get();
        Object thatValue = that.refs[i].get();
        
        if ((value != thatValue) || (value == null))
        {
          return false;
        }
      }
      
      return true;
    }
    
    private final int hashCode;
    private final Ref<T>[] refs;
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
