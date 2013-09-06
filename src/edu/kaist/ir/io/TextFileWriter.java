package edu.kaist.ir.io;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import edu.kaist.ir.utils.StopWatch;

/**
 * 
 * @author Heung-Seon Oh
 * @version 1.0
 * @date 2009. 4. 14
 * 
 */

public class TextFileWriter {

	private int numWrites;

	private StopWatch stopWatch;

	private Writer writer;

	public TextFileWriter(File file) {
		this(file, IOUtils.UTF_8, false);
	}

	public TextFileWriter(File file, String encoding, boolean append) {
		try {
			writer = IOUtils.openBufferedWriter(file, encoding, append);
		} catch (Exception e) {
			e.printStackTrace();
		}
		stopWatch = new StopWatch();
		numWrites = 0;
	}

	public TextFileWriter(String path) {
		this(new File(path), IOUtils.UTF_8, false);
	}

	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	public void print(int amount) {
		if (stopWatch.startTime == 0) {
			stopWatch.start();
		}

		if (numWrites % amount == 0) {
			System.out.print(String.format("\r[%d writes, %s]", numWrites, stopWatch.stop()));
		}
	}

	public void printLast() {
		System.out.println(String.format("\r[%d writes, %s]", numWrites, stopWatch.stop()));
	}

	public void write(String text) {
		try {
			writer.write(text);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
