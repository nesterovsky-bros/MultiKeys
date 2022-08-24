package com.nesterovskyBros;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;

import org.junit.jupiter.api.Test;

class WeakStoreTest
{
  @Test
  void test()
  {
    int[] changes = { 0 };

    // For unit test we override release() to capture the call.
    WeakStore<Object> store = new WeakStore<Object>()
    {
      @Override
      protected void release(Object value)
      {
        ++changes[0];
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

    gc();
    
    // Set data by (key1, key2) again.
    store.set(data, key1, key2);
    
    // Verify nothing has changed.
    assertTrue(store.get(key1, key2) == data);
    assertEquals(changes[0], 0);

    data = null;
    
    gc();
    
    // Verify data is in the store even after we don't have its firm reference.
    assertNotNull(store.get(key1, key2));
    
    Object data2 = new Object();
    
    // Set data by (key1, key2).
    store.set(data2, key1, key2);
    
    gc();

    // Verify it's in the store.
    assertTrue(store.get(key1, key2) == data2);
    assertEquals(changes[0], 1);

    // Let key2 to go.
    key2 = null;
    
    gc();
    
    // Some operation over store. It can be any.
    store.poll();
    
    // Verify that data is released, so entry for (key1, key2) is not there.
    assertEquals(changes[0], 2);

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
