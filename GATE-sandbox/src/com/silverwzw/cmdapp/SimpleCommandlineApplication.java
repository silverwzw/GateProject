package com.silverwzw.cmdapp;

import java.util.HashMap;
import java.util.LinkedList;

abstract class Registry  {
	
	private HashMap<String, Executable> map;
	
	Registry() {
		map = new HashMap<String, Executable>();
		registry();
	}
	
	final protected void register(String registerName, Executable action) {
		if (!registerName.equals("")) {
			map.put(registerName, action);
		} else {
			throw new RuntimeException("Empty String cannot be registered");
		}
	}
	
	final protected Executable route2Executable(String executableName) {
		return map.get(executableName);
	}
	
	final protected void setDefault(Executable executable) {
		map.put("", executable);
	}
	
	final protected Executable getDefault() {
		return map.get("");
	}
	
	final protected void release() {
		map = null;
	}
	
	abstract protected void registry();
}

public abstract class SimpleCommandlineApplication extends Registry implements Executable {
	
	@SuppressWarnings("serial")
	final static public class CommandlineArgumentParseException extends Exception {
		public CommandlineArgumentParseException() {super();}
		public CommandlineArgumentParseException(String s) {super(s);}
		public CommandlineArgumentParseException(Exception e) {super(e);}
		public CommandlineArgumentParseException(String s, Exception e) {super(s,e);}
	}
	
	public static abstract class ActionHandler extends Registry implements Executable {
		
		//this is the "defualt" value of "defaultHandler"
		final private static class defaultDefaultHandler implements Executable {
			public void execute(String[] args) throws Exception {
				throw new CommandlineArgumentParseException("at least one option should be provided");
			}
		}
		
		protected ActionHandler() {
			setDefault(new defaultDefaultHandler());
		}
		
		protected ActionHandler(Executable deft) {
			setDefault(deft);
		}
		
		final protected void register(char c, Executable exe) {
			register(((Character) c).toString(), exe);
		}
		
		final protected Executable route2Executable(char c) {
			return route2Executable(((Character) c).toString());
		}
		
		final private void processOption(char option, LinkedList<String> argsList) throws Exception {
			Executable exe;
			exe = route2Executable(option);
			if (exe == null) {
				throw new CommandlineArgumentParseException("-" + option + " is not a vaild option here.");
			}
			String[] args1 = new String[argsList.size()];
			for (int j = 0; j < argsList.size(); j++) {
				args1[j] = argsList.get(j);
			}
			exe.execute(args1);
		}
		
		protected void post() {
			;
		}
		
		final public void execute(String[] args) throws Exception {
			
			//if no arguments or 1st argument is not an standard option, call default handler to handler it. 
			if (args.length == 0 || args[0].charAt(0) != '-' || args[0].length() != 2) {
				getDefault().execute(args);
			}
			
			char currentOption = args[0].charAt(1);
			LinkedList<String> sl = new LinkedList<String>(); 
			
			for (int i = 1; i < args.length; i++) {
				if (args[i].charAt(0) == '-' && args[i].length() == 2 ) {
					//process previous option
					processOption(currentOption, sl);
					//move on to next option
					currentOption = args[i].charAt(1);
					sl = new LinkedList<String>();
				} else { 
					sl.add(args[i]);
				}
			}
			//process the last option
			processOption(currentOption, sl);
			post();
		}
	}
	
	final private class help implements Executable {
		final public void execute(String [] s) {
			System.out.println(helpMessage());
			System.exit(0);
		}
	}
	
	private Executable helpInstance = new help(); 
	
	protected SimpleCommandlineApplication () {
		register("--help", helpInstance);
		register("help", helpInstance);
		setDefault(helpInstance);
	}
	
	//override by subclass
	protected abstract String helpMessage();
	//override by subclass
	protected void pre(String[] args) throws Exception {
		;
	}
	//override by subclass
	protected void post(String[] args) throws Exception  {
		;
	}
	
	protected void exceptionHandler(Exception e) throws Exception {
		throw e;
	}
	
	final public void execute(String[] args) throws Exception {
		try {
			// execute user defined pre-route action
			pre(args);
			
			String[] args1;
			
			// execute user defined pre-route action
			if (args.length == 0) {
				getDefault().execute(null);
			}
			
			//call action by name
			args1 = new String[args.length - 1];
			for (int i = 1; i < args.length ; i++) {
				args1[i - 1] = args[i];
			}
			Executable exe;
			exe = route2Executable(args[0]);
			//if no corresponding action
			if (exe == null) {
				throw new CommandlineArgumentParseException("'" + args[0] + "' is not a valid action.");
			}
			exe.execute(args1);
			
			release();
			System.gc();
			// execute user defined post-route action
			post(args);
		} catch (CommandlineArgumentParseException e) {
			String message = e.getMessage();
			if (message != null) {
				System.err.println(message);
			}
			(new help()).execute(null);
			System.exit(1);
		} catch (Exception e) {
			// execute user defined exception handler
			exceptionHandler(e);
		}
	}
}

 