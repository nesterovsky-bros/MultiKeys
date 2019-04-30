package com.nesterovskyBros;

/**
 * A string pool sample.
 */
public class PooledString
{
  public static String of(String value)
  {
    return pool.get(value);
  }
  
  private static final WeakPool<String> pool = new WeakPool<>();
}
