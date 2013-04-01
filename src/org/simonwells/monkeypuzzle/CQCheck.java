package org.simonwells.monkeypuzzle;

/*
 * CQCheck.java
 *
 * Created on 20 July 2006, 21:13
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * Provides a list of critical questions and a boolean field to check if
 * they have been answered.
 * @author growe
 */
public class CQCheck
{
  private String cqText;
  private boolean cqAnswered;
  
  /** Creates a new instance of CQCheck */
  public CQCheck()
  {
  }
  
  public CQCheck(String t, boolean a)
  {
    cqText = t;
    cqAnswered = a;
  }

  public String getCqText()
  {
    return cqText;
  }

  public void setCqText(String cqText)
  {
    this.cqText = cqText;
  }

  public boolean isCqAnswered()
  {
    return cqAnswered;
  }

  public void setCqAnswered(boolean cqAnswered)
  {
    this.cqAnswered = cqAnswered;
  }
  
}
