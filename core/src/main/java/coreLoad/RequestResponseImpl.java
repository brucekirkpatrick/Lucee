package coreLoad;

import lucee.cli.cli2.RequestResponse;

public class RequestResponseImpl implements RequestResponse {
	public String host="localhost";
	public void write(String s){
		System.out.println(s);
	}
	public void write(char s){
		System.out.println(s);
	}
}
