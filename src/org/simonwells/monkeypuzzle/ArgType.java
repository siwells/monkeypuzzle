package org.simonwells.monkeypuzzle;

import java.util.*;
import java.io.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      University of Dundee
 * @author
 * @version 1.0
 */

public class ArgType implements Serializable
{
  String name;
  String conclusion;
  Vector premises;
  Vector criticalQuestions;
  
  public ArgType()
  {
    name = "";
    conclusion = "";
    premises = new Vector();
    criticalQuestions = new Vector();
  }

  public Vector getCriticalQuestions()
  { return criticalQuestions; }

  public Vector getPremises()
  { return premises; }

  public String getConclusion()
  { return conclusion; }

  public void setConclusion(String conc)
  { conclusion = conc; }

  public String getName()
  { return name; }

  public void setName(String newName)
  { name = newName; }

  @Override
  public String toString()
  {
    String string = "Name: " + name + "\n";
    if (premises.size() > 0) {
      string += "Premises\n";
      for (int i = 0; i < premises.size(); i++)
      string += (String)premises.elementAt(i) + "\n";
    }
    string += "Conclusion: " + conclusion + "\n";
    if (criticalQuestions.size() > 0) {
      string += "Critical questions:\n";
      for (int i = 0; i < criticalQuestions.size(); i++)
        string += (String)criticalQuestions.elementAt(i) + "\n";
    }
    return string;
  }
}