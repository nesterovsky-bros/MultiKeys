package com.nesterovskyBros;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;

import org.junit.jupiter.api.Test;

import com.nesterovskyBros.WeakStore;

class WeakStoreTest
{

  @Test
  void test()
  {
    String[] log = { null };

    WeakStore<Object> store = new WeakStore<Object>()
    {
      @Override
      protected void release(Object value)
      {
        log[0] = "data is released";
      }
    };
    
    Object key1 = new Object();
    Object key2 = new Object();
    Object data = new Object();
    
    store.set(data, key1, key2);
    
    assertTrue(store.get(key1, key2) == data);
    data = null;
    
    gc();
    
    assertNotNull(store.get(key1, key2));
    
    key2 = null;
    
    gc();
    
    store.poll();
    
    gc();
    
    assertNotNull(log[0]);

    key2 = new Object();
    
    assertTrue(store.get(key1, key2) == null);
  }
  
  private static void gc() 
  {
    for(int i = 0; i < 2; ++i)
    {
      WeakReference<Object> ref = new WeakReference<>(new Object());
    
      while(ref.get() != null) 
      {
        System.gc();
      }
      
      System.runFinalization();
    }
  }
}
