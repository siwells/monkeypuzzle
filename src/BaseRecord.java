import java.util.*;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class BaseRecord
{

  public BaseRecord()
  {
  }

  /**
   * SQL strings cannot contain single quotes. Each '
   * must be replaced by a double quote ''
   */
  static public String escapeQuotes(String s)
  {
    if (s == null) {
      return null;
    }
    StringTokenizer tokens = new StringTokenizer(s, "'", true);

    StringBuffer esc = new StringBuffer();
    while (tokens.hasMoreTokens()) {
      String tok = tokens.nextToken();
      esc.append(tok);
      if (tok.equals("'")) esc.append("'");
    }
    return esc.toString();
  }
}