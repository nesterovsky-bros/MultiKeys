package com.nesterovskyBros;

/**
 * A string pool sample.
 */
public class StringPool
{
  public static String get(String value)
  {
    return pool.get(value);
  }
  
  private static final WeakPool<String> pool = new WeakPool<>();
}
