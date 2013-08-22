package com.silverwzw.thesis.mscs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.silverwzw.google.api.GQuery;

public class DataHelper {
	public static String getUrlMatrixCSV(String SearchTerm1,String SearchTerm2, int matrixDim1, int matrixDim2) {
		List<String> ulist1, ulist2;
		ulist1 = new GQuery(SearchTerm1).asUrlStringList(matrixDim1);
		ulist2 = new GQuery(SearchTerm2).asUrlStringList(matrixDim2);
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
}
