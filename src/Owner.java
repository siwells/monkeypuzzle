
public class Owner
{
	private String fullName;
	private String shortName;	
	
	public Owner()
	{
		fullName = "";
		shortName = "";
	}
	
	public Owner(String full, String shortN)
	{
		fullName = full;
		shortName = shortN;
	}

	public void setFullName(String name)
	{
		fullName = name;
	}

	public void setShortName(String name)
	{
		shortName = name;
	}

	public String getShortName()
	{
		return shortName;
	}

	public String getFullName()
	{
		return fullName;
	}	
  
  public boolean equals(Object other)
  {
    if (!(other instanceof Owner))
      return false;
    Owner o = (Owner)other;
    return o.fullName.equals(this.fullName);
  }
}
