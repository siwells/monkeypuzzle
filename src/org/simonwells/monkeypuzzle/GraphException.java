package org.simonwells.monkeypuzzle;

public class GraphException extends RuntimeException
{
  GraphException(String message)
  {
    super(message);
  }

  GraphException()
  {
    super();
  }
}
