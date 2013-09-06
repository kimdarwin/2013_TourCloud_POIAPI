package edu.kaist.ir.pattern;

public enum Markup {
	MATCH("="), UNMATCH("!="), SIMILAR(":=");

	private String marker;

	private Markup(String marker) {
		this.marker = marker;
	}

	String getMarker() {
		return marker;
	}
}