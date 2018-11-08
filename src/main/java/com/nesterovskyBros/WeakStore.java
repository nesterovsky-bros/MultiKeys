package com.nesterovskyBros;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * <p>A store to keep objects by multiple weak keys.
 * Value is kept only if all keys are alive, otherwise value is reclaimed.</p>
 * <p><b>Note:</b> class is thread safe</p>
 *
 * @param <T> an instance type to strore.
 */
public class WeakStore<T>
{
  /**
   * Gets an instance by keys.
   * @param keys an array of keys.
   * @return an instance, if avaliable.
   */
  public T get(Object ...keys)
  {
    poll();
    
    return store.get(new Key(queue, keys));
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
    
    return store.computeIfAbsent(new Key(queue, keys), key -> factory.get());
  }
  
  /**
   * Sests or remove an instance by keys.
   * @param value a value to set, or {@code null} to remove.
   * @param keys an array of keys.
   * @return a replaced instance.
   */
  public T set(T value, Object ...keys)
  {
    poll();
    
    Key key = new Key(queue, keys);
    
    return value == null ? store.remove(key) : store.put(key, value);
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
      Ref ref = (Ref)queue.poll();
      
      if (ref == null)
      {
        break;
      }
      
      store.remove(ref.key);
    }
  }
  
  private static class Key
  {
    public Key(ReferenceQueue<Object> queue, Object ...keys)
    {
      int hashCode = 0;
      
      refs = new Ref[keys.length];
      
      for(int i = 0; i < keys.length; ++i)
      {
        Object key = Objects.requireNonNull(keys[i]);

        hashCode ^= key.hashCode();
        refs[i] = new Ref(key, queue, this);
      }
      
      this.hashCode = hashCode;
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
      
      Key that = (Key)obj;
      
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
        Object key = refs[i].get();
        Object thatKey = that.refs[i].get();
        
        if ((key != thatKey) || (key == null))
        {
          return false;
        }
      }
      
      return false;
    }
    
    private final int hashCode;
    private final Ref[] refs;
  }
  
  private static class Ref extends WeakReference<Object>
  {
    public Ref(Object value, ReferenceQueue<Object> queue, Key key)
    {
      super(value, queue);
      this.key = key;
    }
    
    private final Key key;
  }
  
  private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
  private final ConcurrentHashMap<Key, T> store = new ConcurrentHashMap<>();
}
