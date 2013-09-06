package edu.kaist.ir.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.kaist.ir.utils.Counter;
import edu.kaist.ir.utils.CounterMap;
import edu.kaist.ir.utils.Indexer;
import edu.kaist.ir.utils.StopWatch;

/**
 * @author Heung-Seon Oh
 * @version 1.2
 * @date 2009. 5. 10
 * 
 */
public class IOUtils {

	public static final String UTF_8 = "UTF-8";

	private static void addFilesUnder(File root, List<File> files, boolean recursive) {
		for (File child : root.listFiles()) {
			if (child.isFile()) {
				files.add(child);
			} else {
				if (recursive) {
					addFilesUnder(child, files, recursive);
				}
			}
		}
	}

	public static File appendFileNameSuffix(File file, String suffix) {
		String filePath = getCanonicalPath(file);
		if (!filePath.endsWith(suffix)) {
			filePath += suffix;
			file = new File(filePath);
		}
		return file;
	}

	public static void copy(File source, File destination) throws Exception {
		if (source.isDirectory()) {
			if (destination.isDirectory()) {
				for (File file : getFilesUnder(source)) {
					String srcPath = source.getPath();
					String tarPath = destination.getPath();
					String path = file.getPath();
					path = path.replace(srcPath, tarPath);
					File outputFile = new File(path);
					write(file, readText(outputFile));
				}
			} else {
				BufferedWriter writer = openBufferedWriter(destination, UTF_8, true);
				for (File file : getFilesUnder(source)) {
					String text = readText(file);

					writer.write(text + "\n\n");
					writer.flush();
				}
				writer.close();
			}
		} else {
			write(destination, readText(source));
		}
	}

	public static int countLines(File file) throws Exception {
		int numLines = 0;

		BufferedReader reader = openBufferedReader(file, UTF_8);
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			} else {
				numLines++;
			}
		}
		reader.close();

		return numLines;
	}

	public static boolean create(File file) {
		if (file.exists()) {
			deleteFilesUnder(file);
		}
		return file.mkdirs();
	}

	private static int deleteFiles(File root) {
		int numFiles = 0;
		if (root.exists()) {
			if (root.isDirectory()) {
				for (File child : root.listFiles()) {
					numFiles += deleteFiles(child);
				}
				root.delete();
			} else if (root.isFile()) {
				root.delete();
				numFiles++;
			}
		}
		return numFiles;
	}

	public static void deleteFilesUnder(File dir) {
		StopWatch watch = new StopWatch();
		watch.start();
		int numFiles = deleteFiles(dir);
		System.out.println(String.format("delete [%d] files under [%s]:\t%s", numFiles, dir.getPath(), watch.stop()));
	}

	public static String getCanonicalPath(File file) {
		String ret = null;
		try {
			ret = file.getCanonicalPath();
			ret = ret.replace("\\", "/");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static String getExtension(String fileName) {
		int idx = fileName.lastIndexOf(".");
		if (idx > 0) {
			fileName = fileName.substring(idx + 1);
		}
		return fileName;
	}

	public static String getFileName(String fileName) {
		int end = fileName.lastIndexOf(".");

		if (end > 0) {
			fileName = fileName.substring(0, end);
		}
		return fileName;
	}

	public static List<File> getFilesUnder(File dir) {
		return getFilesUnder(dir, true);
	}

	public static List<File> getFilesUnder(File dir, boolean recursive) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		List<File> files = new ArrayList<File>();
		addFilesUnder(dir, files, recursive);

		System.out.println(String.format("read [%d] files from [%s]:\t%s", files.size(), dir.getPath(), stopWatch.stop()));
		Collections.sort(files);
		return files;
	}

	public static BufferedReader openBufferedReader(File file, String encoding) throws Exception {
		return new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
	}

	public static BufferedWriter openBufferedWriter(File file, String encoding, boolean append) throws Exception {
		String fileSeparator = System.getProperty("file.separator");

		if (fileSeparator.equals("\\")) {
			fileSeparator = "\\\\";
		}

		if (file.getPath().split(fileSeparator).length > 1) {
			File parentFile = new File(file.getParent());
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
		}

		OutputStreamWriter osw = null;

		if (file.exists()) {
			osw = new OutputStreamWriter(new FileOutputStream(file, append), encoding);
		} else {
			osw = new OutputStreamWriter(new FileOutputStream(file), encoding);
		}
		return new BufferedWriter(osw);
	}

	public static ObjectInputStream openObjectInputStream(File file) throws Exception {
		ObjectInputStream ret = null;
		if (file.getName().endsWith(".gz")) {
			ret = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
		} else {
			ret = new ObjectInputStream(new FileInputStream(file));
		}
		return ret;
	}

	public static ObjectOutputStream openObjectOutputStream(File file) throws Exception {
		ObjectOutputStream ret = null;
		File parent = file.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}
		if (file.getName().endsWith(".gz")) {
			ret = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
		} else {
			ret = new ObjectOutputStream(new FileOutputStream(file));
		}

		return ret;
	}

	public static InputStreamReader openUrl(URL url) throws IOException {
		URLConnection urlConn = url.openConnection();
		urlConn.setRequestProperty("User-agent", "Mozilla/4.0");

		HttpURLConnection httpUrlConn = (HttpURLConnection) urlConn;
		httpUrlConn.setConnectTimeout(2000);
		int responseCode = httpUrlConn.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			return new InputStreamReader(urlConn.getInputStream(), "UTF-8");
		} else {
			throw new IOException();
		}
	}

	public static Counter<String> readCounter(File inputFile) throws Exception {
		Counter<String> ret = new Counter<String>();
		if (inputFile.getName().endsWith(".ser")) {
			ObjectInputStream ois = openObjectInputStream(inputFile);
			ret = readCounter(ois);
			ois.close();
		} else {
			TextFileReader reader = new TextFileReader(inputFile);
			while (reader.hasNext()) {
				String[] parts = reader.next().split("\t");
				ret.incrementCount(parts[0], Double.parseDouble(parts[1]));
			}
		}
		return ret;
	}

	public static Counter<String> readCounter(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		Counter<String> ret = new Counter<String>();
		for (int i = 0; i < size; i++) {
			String key = readText(ois);
			double value = ois.readDouble();
			ret.setCount(key, value);
		}
		return ret;
	}

	public static CounterMap<String, String> readCounterMap(File inputFile) throws Exception {
		ObjectInputStream ois = openObjectInputStream(inputFile);
		CounterMap<String, String> ret = readCounterMap(ois);
		ois.close();
		return ret;
	}

	public static CounterMap<String, String> readCounterMap(ObjectInputStream ois) throws Exception {
		CounterMap<String, String> ret = new CounterMap<String, String>();
		int keySize = ois.readInt();
		for (int i = 0; i < keySize; i++) {
			String key = readText(ois);
			Counter<String> counter = readCounter(ois);
			ret.setCounter(key, counter);
		}
		return ret;
	}

	public static List<Counter<String>> readCounters(File inputDir) throws Exception {
		List<Counter<String>> ret = new ArrayList<Counter<String>>();
		for (File file : getFilesUnder(inputDir)) {
			ret.add(readCounter(file));
		}
		return ret;
	}

	public static double[] readDoubleArray(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		double[] ret = new double[size];
		for (int i = 0; i < size; i++) {
			ret[i] = ois.readInt();
		}
		return ret;
	}

	public static double[][] readDoubleMatrix(ObjectInputStream ois) throws Exception {
		int rowSize = ois.readInt();
		double[][] ret = new double[rowSize][];
		for (int i = 0; i < rowSize; i++) {
			ret[i] = readDoubleArray(ois);
		}
		return ret;
	}

	public static Indexer<String> readIndexer(File inputFile) throws Exception {
		System.out.printf("read [%s].\n", inputFile.getPath());
		ObjectInputStream ois = openObjectInputStream(inputFile);
		Indexer<String> ret = readIndexer(ois);
		ois.close();
		return ret;
	}

	public static Indexer<String> readIndexer(ObjectInputStream ois) throws Exception {
		Indexer<String> ret = new Indexer<String>();
		int size = ois.readInt();
		for (int i = 0; i < size; i++) {
			String item = readText(ois);
			ret.add(item);
		}
		return ret;
	}

	public static int[] readIntegerArray(File inputFile) throws Exception {
		ObjectInputStream ois = openObjectInputStream(inputFile);
		int[] ret = readIntegerArray(ois);
		ois.close();
		return ret;
	}

	public static int[] readIntegerArray(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		int[] ret = new int[size];
		for (int i = 0; i < size; i++) {
			ret[i] = ois.readInt();
		}
		return ret;
	}

	public static int[][] readIntegerMatrix(File inputFile) throws Exception {
		ObjectInputStream ois = openObjectInputStream(inputFile);
		int[][] ret = readIntegerMatrix(ois);
		ois.close();
		return ret;
	}

	public static int[][] readIntegerMatrix(ObjectInputStream ois) throws Exception {
		int rowSize = ois.readInt();
		int[][] ret = new int[rowSize][];
		for (int i = 0; i < rowSize; i++) {
			ret[i] = readIntegerArray(ois);
		}
		return ret;
	}

	public static List<String> readLines(BufferedReader reader, int numLines) throws Exception {
		List<String> ret = new ArrayList<String>();
		while (true) {
			String line = reader.readLine();
			if (line == null || ret.size() == numLines) {
				break;
			} else {
				ret.add(line);
			}
		}
		return ret;
	}

	public static List<String> readLines(File file) throws Exception {
		return readLines(file, UTF_8, Integer.MAX_VALUE);
	}

	public static List<String> readLines(File file, int maxLines) throws Exception {
		return readLines(file, UTF_8, maxLines);
	}

	public static List<String> readLines(File file, String encoding, int maxLines) throws Exception {
		BufferedReader reader = openBufferedReader(file, encoding);
		List<String> ret = readLines(reader, maxLines);
		reader.close();
		return ret;
	}

	public static HashSet<String> readSet(File file) throws Exception {
		return new HashSet<String>(readLines(file));
	}

	public static String readText(File file) throws Exception {
		return readText(file, UTF_8);
	}

	public static String readText(File file, String encoding) throws Exception {
		StringBuffer ret = new StringBuffer();
		BufferedReader reader = openBufferedReader(file, encoding);
		ret.append(readText(reader));
		reader.close();
		return ret.toString();
	}

	public static String readText(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		StringBuffer sb = new StringBuffer(size);
		for (int j = 0; j < size; j++) {
			sb.append((char) ois.readByte());
		}
		return sb.toString();
	}

	public static String readText(Reader reader) throws Exception {
		StringBuffer sb = new StringBuffer();
		while (true) {
			int i = reader.read();
			if (i == -1) {
				break;
			} else {
				sb.append((char) i);
			}
		}
		return sb.toString();
	}

	public static Indexer<String> readTextIndexer(File inputFile) {
		System.out.printf("read [%s].\n", inputFile.getName());

		Indexer<String> ret = new Indexer<String>();
		TextFileReader reader = new TextFileReader(inputFile);
		while (reader.hasNext()) {
			String[] parts = reader.next().split("\t");
			if (parts[0].startsWith("##")) {
				continue;
			}
			ret.add(parts[0]);
		}
		reader.close();

		System.out.printf("[%s, %d]\n", inputFile.getName(), ret.size());
		return ret;
	}

	public static void write(File file, boolean append, String text) throws Exception {
		write(file, UTF_8, append, text);
	}

	public static void write(File outputFile, Counter<String> counter) throws Exception {
		ObjectOutputStream oos = openObjectOutputStream(outputFile);
		write(oos, counter);
		oos.close();
	}

	public static void write(File outputFile, CounterMap<String, String> counterMap) throws Exception {
		ObjectOutputStream ois = openObjectOutputStream(outputFile);
		write(ois, counterMap);
		ois.close();
	}

	public static void write(File outputFile, Indexer<String> indexer) throws Exception {
		ObjectOutputStream oos = openObjectOutputStream(outputFile);
		write(oos, indexer);
		oos.close();
	}

	public static void write(File outputFile, int[] array) throws Exception {
		ObjectOutputStream oos = openObjectOutputStream(outputFile);
		write(oos, array);
		oos.close();
	}

	public static void write(File file, int[][] mat) throws Exception {
		ObjectOutputStream oos = openObjectOutputStream(file);
		write(oos, mat);
		oos.close();
	}

	public static void write(File file, String text) throws Exception {
		write(file, UTF_8, false, text);
	}

	public static void write(File file, String encoding, boolean append, String text) throws Exception {
		Writer writer = openBufferedWriter(file, encoding, append);
		writer.write(text);
		writer.flush();
		writer.close();
	}

	public static void write(ObjectOutputStream oos, Counter<String> counter) throws Exception {
		oos.writeInt(counter.size());
		for (String key : counter.keySet()) {
			double value = counter.getCount(key);
			write(oos, key);
			oos.writeDouble(value);
		}
	}

	public static void write(ObjectOutputStream ois, CounterMap<String, String> counterMap) throws Exception {
		List<String> keys = new ArrayList<String>(counterMap.keySet());
		ois.writeInt(keys.size());

		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			Counter<String> counter = counterMap.getCounter(key);
			write(ois, key);
			write(ois, counter);
		}
	}

	public static void write(ObjectOutputStream oos, double[] array) throws Exception {
		oos.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			oos.writeDouble(array[i]);
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, double[][] mat) throws Exception {
		oos.writeInt(mat.length);
		for (int i = 0; i < mat.length; i++) {
			write(oos, mat[i]);
		}
	}

	public static void write(ObjectOutputStream oos, Indexer<String> indexer) throws Exception {
		oos.writeInt(indexer.size());
		for (int i = 0; i < indexer.size(); i++) {
			write(oos, indexer.getObject(i));
		}
	}

	public static void write(ObjectOutputStream oos, int[] array) throws Exception {
		oos.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			oos.writeInt(array[i]);
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, int[][] mat) throws Exception {
		oos.writeInt(mat.length);
		for (int i = 0; i < mat.length; i++) {
			write(oos, mat[i]);
		}

	}

	public static void write(ObjectOutputStream oos, Integer[] array) throws Exception {
		oos.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			oos.writeInt(array[i].intValue());
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, String text) throws Exception {
		oos.writeInt(text.length());
		for (int i = 0; i < text.length(); i++) {
			oos.writeByte(text.charAt(i));
		}
		oos.flush();
	}

	public static void writeText(File outputFile, Indexer<String> indexer) throws Exception {
		System.out.printf("write to %s.\n", outputFile.getPath());
		BufferedWriter writer = openBufferedWriter(outputFile, UTF_8, false);
		for (String str : indexer.getObjects()) {
			writer.write(str + "\n");
			writer.flush();
		}
		writer.close();
	}

}
