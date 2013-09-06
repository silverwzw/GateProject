package com.silverwzw.thesis.mscs;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.silverwzw.google.api.Search;

public class DataHelper {
	public static String getUrlMatrixCSV(String SearchTerm1,String SearchTerm2, int matrixDim1, int matrixDim2) {
		List<String> ulist1, ulist2;
		ulist1 = new Search(SearchTerm1).asUrlStringList(matrixDim1);
		ulist2 = new Search(SearchTerm2).asUrlStringList(matrixDim2);
		String csv = "";
		for (int i = 1; i < matrixDim1; i++) {
			for (int j = 1; j < matrixDim2; j++) {
				System.out.print(Helper.slice(ulist1, ulist2, i, j));
				System.out.print(',');
			}
			System.out.println();
		}
		return csv;
	}
	public static void getUrlSeries(List<String> keywords, int sampleSize) {
		List<String> ulist1,ulist2;
		ulist1 = new Search(Helper.getSearchTerm(keywords, 1)).asUrlStringList(sampleSize);
		for (int i = 2; i <= keywords.size(); i++) {
			ulist2 = new Search(Helper.getSearchTerm(keywords, i)).asUrlStringList(sampleSize);
			System.out.println(Helper.slice(ulist1, ulist1, sampleSize, sampleSize));
			ulist1 = ulist2;
		}
	}
	public static void testHelper() {
		List<String> s1,s2;
		s1 = new LinkedList<String>();
		s2 = new LinkedList<String>();
		s1.add("a");
		s1.add("b");
		s1.add("c");
		s1.add("d");
		s1.add("e");
		s2.addAll(s1);
		s1.add("e");
		s1.add("f");
		s1.add("j");
		s2.add("g");
		s2.add("h");
		s2.add("i");
		System.out.print(Helper.slice(s1, s2, s1.size(), s2.size()));
	}
}

class Helper {
	static float slice(List<String> lst1, List<String> lst2, int dim1, int dim2) {
		Set<String> scounter = new HashSet<String>();
		int i;
		for (i = 0; i < dim1; i++) {
			scounter.add(lst1.get(i));
		}
		for (i = 0; i < dim2; i++) {
			scounter.add(lst2.get(i));
		}
		
		return 1 - scounter.size()/(float)(dim1+dim2);
	}
	final static String getSearchTerm(List<String> keywords, int num) {
		String st = keywords.get(0);
		for (int i = 1; i < num; i++) {
			st += "%20" + keywords.get(i);
		}
		return st;
	}
}