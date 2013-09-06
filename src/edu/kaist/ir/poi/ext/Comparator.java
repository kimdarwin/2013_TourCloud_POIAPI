package edu.kaist.ir.poi.ext;

import java.io.File;
import java.util.List;

import edu.kaist.ir.io.IOUtils;

public class Comparator {
	public static void main(String[] args) throws Exception {
		List<File> files = IOUtils.getFilesUnder(new File("D:/Test_Collection/8_testcollection_filter_2_filtered_out"));
		List<File> ref_files = IOUtils.getFilesUnder(new File("D:/Test_Collection/7_testcollection_filter_2_text"));
		for (int n = 0; n < files.size(); n++ ) {
			File inputFile = files.get(n);
			int Flag = 0;
				for (int i = 0; i < ref_files.size(); i++) {
					File ref_inputFile = ref_files.get(i);
					if(inputFile.getName().equals(ref_inputFile.getName())) {
						Flag = 1;
						break;
					}
				}
			if(Flag == 1)
				inputFile.delete();
		}
		System.out.println("process end.");
	}
}
