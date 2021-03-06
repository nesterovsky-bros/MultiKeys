package com.nesterovskyBros;

/**
 * A sample of qname with prefix.
 */
public class EQName
{
  public static EQName of(String prefix, QName qname)
  {
    return pool.get(new EQName(prefix, qname));
  }
  
  public final String prefix;
  public final QName qname;
  
  @Override
  public int hashCode()
  {
    return prefix.hashCode() ^ qname.hashCode();
  }
  
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    
    if (!(obj instanceof EQName))
    {
      return false;
    }
    
    EQName that = (EQName)obj;
    
    return (prefix == that.prefix) && (qname == that.qname);
  }
  
  private EQName(String prefix, QName qname)
  {
    this.prefix = prefix;
    this.qname = qname;
  }
  
  private static final WeakPool<EQName> pool = new WeakPool<>();
}
