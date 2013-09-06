package edu.kaist.ir.poi.dis.tree;


public class Location {
	public String loc;
	public int cnt=0;
	public float p;
	public float s;
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
