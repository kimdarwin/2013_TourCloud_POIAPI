package edu.kaist.ir.api;


public class Location {
	String loc;
	int cnt=0;
	float p;
	float s;
	public Location(String loc) {
		this.loc=loc;
	}
	public Location(String loc, int cnt) {
		this.loc=loc;
		this.cnt=cnt;
	}
	public Location(String loc, int cnt, float p) {
		this.loc=loc;
		this.cnt=cnt;
		this.p=p;
	}
	public Location(String loc, float s) {
		this.loc=loc;
		this.s=s;
	}
	public String toString() {
		return loc;
		
	}

}
