package com.silverwzw;

import java.io.PrintStream;

public class Debug {
	
	static protected int level = 0;
	// 3: print stack, 2: trace calls, 1: some useful information
	
	private Debug(){};
	//Singleton
	
	final public static void set(int i) {
		int old;
		old = level;
		level = i > 0 ? i : 0;
		println(0, "debug level change from " + old + " to "+ level);
	}
	
	final public static boolean ge(int i) {
		return level >= i;
	}
	
	final public static boolean on() {
		return level > 0;
	}
	
	public static boolean rawPrint(int level, String msg, PrintStream o) {
		if (ge(level)) {
			o.print(msg);
			return true;
		}
		return false;
	}
	
	public static boolean println(int level, String msg) {
		return rawPrint(level, "[DEBUG]" + msg + '\n', System.out);
	}
	
	
	public static boolean into(Object o, String methodName) {
		return trace("INTO",o,methodName);
	}
	
	public static boolean out(Object o, String methodName) {
		return trace("OUT ",o,methodName);
	}
	
	final private static boolean trace(String action, Object o, String methodName) {
		String name;
		Class<?> c;
		
		c = o.getClass();
		
		if (c.equals(String.class)) {
			name = (String) o;
		} else if (c.equals(Class.class)){
			name = c.getName();
		} else {
			name = o.getClass().getName();
		}
		
		if (!ge(3)) {
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		
		return rawPrint(2, "[DEBUG]" + action + ": " + name + "." + methodName, System.out);
	}
	
	public static boolean info(String msg) {
		return rawPrint(1, "[DEBUG]" + msg + '\n', System.out);
	}
	
	public static boolean exception(Exception e) {
		if (ge(3)) {
			e.printStackTrace();
			return true;
		}
		return false;
	}
	
	final public static int level() {
		return level;
	}
}
