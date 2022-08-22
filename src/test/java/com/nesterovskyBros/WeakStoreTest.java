package com.nesterovskyBros;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;

import org.junit.jupiter.api.Test;

class WeakStoreTest
{
  @Test
  void test()
  {
    String[] log = { null };

    // For unit test we override release() to capture the call.
    WeakStore<Object> store = new WeakStore<Object>()
    {
      @Override
      protected void release(Object value)
      {
        log[0] = "data is released";
      }
    };
    
    // Create keys
    Object key1 = new Object();
    Object key2 = new Object();
    
    // and data.
    Object data = new Object();
    
    // Set data by (key1, key2).
    store.set(data, key1, key2);
    
    // Verify it's in the store.
    assertTrue(store.get(key1, key2) == data);
    data = null;
    
    gc();
    
    // Verify data is in the store even after we don't have its firm reference.
    assertNotNull(store.get(key1, key2));
    
    // Let key2 to go.
    key2 = null;
    
    gc();
    
    // Some operation over store. It can be any.
    store.poll();
    
    // Verify that data is released, so entry for (key1, key2) is not there.
    assertNotNull(log[0]);

    key2 = new Object();

    // There is no data by different keys.
    assertNull(store.get(key1, key2));
  }
  
  private static void gc() 
  {
    WeakReference<Object> ref = new WeakReference<>(new Object());
  
    while(ref.get() != null) 
    {
      System.gc();
    }
  }
}
