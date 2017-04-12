package nl.knmi.adaguc.tools;

import lombok.Getter;

@Getter
public class StatusCode {
  private int statuscode=200;
  private String msg;
  
  public StatusCode(int statusCode, String msg) {
	  this.statuscode=statusCode;
	  this.msg=msg;
  }
  public StatusCode(){}
}
