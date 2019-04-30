package com.nesterovskyBros;

/**
 * A sample of qname.
 */
public class QName
{
  public static QName of(String localname, String namespace)
  {
    return pool.get(new QName(localname, namespace));
  }
  
  public final String localname;
  public final String namespace;
  
  @Override
  public int hashCode()
  {
    return localname.hashCode() ^ namespace.hashCode();
  }
  
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    
    if (!(obj instanceof QName))
    {
      return false;
    }
    
    QName that = (QName)obj;
    
    return (localname == that.localname) && (namespace == that.namespace);
  }
  
  private QName(String localname, String namespace)
  {
    this.localname = localname;
    this.namespace = namespace;
  }
  
  private static final WeakPool<QName> pool = new WeakPool<>();
}
