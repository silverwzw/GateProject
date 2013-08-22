package com.silverwzw.gate;

import gate.Gate;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;









import com.silverwzw.Debug;
import com.silverwzw.JSON.JSON;
import com.silverwzw.cmdapp.Executable;
import com.silverwzw.cmdapp.SimpleCommandlineApplication;
import com.silverwzw.cmdapp.SimpleCommandlineApplication.ActionHandler;
import com.silverwzw.cmdapp.SimpleCommandlineApplication.CommandlineArgumentParseException;
import com.silverwzw.gate.datastore.DatastoreRouter;
import com.silverwzw.gate.datastore.DatastoreRouterImpl;
import com.silverwzw.gate.manager.GateProjectManager;
import com.silverwzw.google.api.GQuery;
import com.silverwzw.thesis.mscs.DataHelper;

public class GateProject extends SimpleCommandlineApplication {

	public static void main(String[] args) throws Exception {
		(new GateProject()).execute(args);
	}

	public static DatastoreRouter datastoreRouter = null; 
	public static List<String> taskName = new LinkedList<String>();
	public static String dcolistJsonStr = null;
	public static String gateHome = null;
	public static String gatePluginHome = null;
	public static List<PostponeExecutable> pexec = new LinkedList<PostponeExecutable>();
	public static Collection<URL> gateCreoleDir = new LinkedList<URL>();
	
	GateProject() {
		
	}
	
	final protected String helpMessage() {
		return 
				"Usage: load   -e <file> [-d <file ..>] [-t <file ..>] [-v [level]]\n" +
				"  or : run    -e <file> -t <taskName> -l <file> [-g <path>] [-p <path>] [-c <url ..>] [-v [level]]\n" +
				"  or : reset  -e <file>\n" +
				"  or : deamon -e <file> [-g <path>] [-p <path>] [-c <url ..>] [-v [level]]" +
				"  or : --help, help\n" +
				"\n" +
				"Action 'load': Load config file to datastore\n" +
				"    -e <file>     Required. Specify the center datastore.\n" +
				"                            <file> is a datastore config file.\n" +
				"    -d <file ..>  Optional. Define new index datastore and save to center datastore.\n" +
				"                            <file ..> is a list of datastore config file.\n" +
				"    -t <file ..>  Optional. Define new task(s) and save to center datastore.\n" +
				"                            <file ..> is a list of task config file.\n" +
				"    -v [level]    Optional. Enable debug mode.\n" +
				"                            [level] is the debug level (1-3), default is 1.\n" +
				"\n" +
				"Action 'run': run a Task\n" +
				"    -e <file>     Required. Specify the center datastore.\n" +
				"                            <file> is a datastore config file.\n" +
				"    -t <task ..>  Required. Specify the task(s) to be executed.\n" +
				"                            <task ..> is the name of the task.\n" +
				"    -l <file>     Required. Specify the local/web document(s) to be processed.\n" +
				"                            <file> is a json file that contains a list of document path/url.\n" +
				"    -g <path>     Optional. Set the path to GATE home\n"+
				"    -p <path>     Optional. Set the path to GATE plugin home\n" +
				"    -c <path ..>  Optional. Set the path(s) of Creole plugin directory(ies)\n" +
				"    -v [level]    Optional. Enable debug mode.\n" +
				"                            [level] is the debug level (1-3), default is 1.\n" +
				"\n" +
				"Action 'reset': reset datastore\n" +
				"    -e <file>     Required. Specify the center datastore.\n" +
				"                            <file> is a datastore config file.\n" +
				"\n" +
				"Action 'deamon': run as deamon\n" + 
				"    -e <file>     Required. Specify the center datastore.\n" +
				"    -g <path>     Optional. Set the path to GATE home\n"+
				"    -p <path>     Optional. Set the path to GATE plugin home\n" +
				"    -c <path ..>  Optional. Set the path(s) of Creole plugin directory(ies)\n" +
				"    -v [level]    Optional. Enable debug mode.\n" +
				"\n" +
				"Action 'help'\n" +
				"  or '--help':\n" +
				"                  show this help message\n";
	}
	
	final protected void post(String[] s) throws Exception {
		if (datastoreRouter == null && !s[0].equals("help") && !s[0].equals("--help") && !s[0].equals("sandbox")) {
			throw new CommandlineArgumentParseException("option -e cannot be ommitted!");
		}
		for (PostponeExecutable pexe : pexec) {
			pexe.run();
		}
		pexec = null;
	}
	
	abstract static class PostponeExecutable implements Executable {
		String[] args;
		final public void execute(String[] args) throws Exception {
			this.args = args;
			prePostpone();
			pexec.add(this);
		}
		public void prePostpone() throws Exception {
			;
		}
		abstract public void run() throws Exception;
	}

	final protected void registry() {
		register("load", new ActionLoad());
		register("reset", new ActionReset());
		register("run", new ActionRun());
		register("sandbox", new ActionSandbox());
	}	
}

class ActionSandbox implements Executable  {
	public void execute(String[] args) throws Exception {
		System.out.print(DataHelper.getUrlMatrixCSV("edit+grep+sed", "edit+awk+sed", 100, 100));
	}
}

class ActionLoad extends ActionHandler {
	final protected void registry() {
		register('v', new ChangeDebugLvl());
		register('e', new SetCenterDatastore());
		register('d', new SaveDatastore());
		register('t', new SaveTask());
	}
}

class ActionReset extends ActionHandler {
	final protected void registry() {
		register('v', new ChangeDebugLvl());
		register('e', new SetCenterDatastore());
	}
	final protected void post() {
		GateProject.datastoreRouter.resetCenter();
	}
}

class ActionRun extends ActionHandler {
	final protected void registry() {
		register('e', new SetCenterDatastore());
		register('v', new ChangeDebugLvl());
		register('t', new SetTaskAndRun());
		register('l', new SetInitDocuments());
		register('g', new SetGateHome());
		register('p', new SetGatePluginHome());
		register('c', new SetCreoleDirectory());
	}
}

class ChangeDebugLvl implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			Debug.set(1);
			return;
		} else if (args.length == 1){
			try {
				int i;
				i = Integer.parseInt(args[0]);
				if (i < 1) {
					throw new NumberFormatException();
				}
				Debug.set(i);
			} catch (NumberFormatException e) {
				throw new CommandlineArgumentParseException("Debug level should be an positive integer");
			}
		} else {
			throw new CommandlineArgumentParseException("Too many arguments for option -v");
		}
	}
}

class SetCenterDatastore implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -e");
		}
		if (args.length > 1) {
			throw new CommandlineArgumentParseException("Too many argument for option -e");
		}
		if (GateProject.datastoreRouter != null) {
			throw new CommandlineArgumentParseException("-e option can only appear once. ");
		}
		GateProject.datastoreRouter = new DatastoreRouterImpl(JSON.parse(new File(args[0])).toString());
	}
}

class SaveTask extends GateProject.PostponeExecutable {
	public void run() throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -t");
		}
		for (int i = 0; i < args.length; i++) {
			JSON json;
			json = JSON.parse(new File(args[i]));

			GateProject.datastoreRouter.saveTask((String) json.get("name").toObject(), json.toString());
		}
	}
}

class SaveDatastore extends GateProject.PostponeExecutable {
	public void run() throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -d");
		}
		for (int i = 0; i < args.length; i++) {
			JSON json;
			json = JSON.parse(new File(args[i]));
			GateProject.datastoreRouter.saveDatastore((String) json.get("name").toObject(), json.toString());
		}
	}
}

class SetGateHome implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -g");
		}
		if (args.length > 1) {
			throw new CommandlineArgumentParseException("Too many argument for option -g");
		}
		GateProject.gateHome = args[0];
	}
}

class SetGatePluginHome implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -p");
		}
		if (args.length > 1) {
			throw new CommandlineArgumentParseException("Too many argument for option -p");
		}
		GateProject.gatePluginHome = args[0];
	}
}

class SetCreoleDirectory implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -c");
		}
		for (int i = 0; i < args.length; i++) {
			GateProject.gateCreoleDir.add((new File(args[i])).toURI().toURL());
		}
	}
}

class SetTaskAndRun extends GateProject.PostponeExecutable {
	public void prePostpone() throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -t");
		}
		for (int i = 0; i < args.length; i++) {
			GateProject.taskName.add(args[i]);
		}
	}
	public void run() throws Exception {
		if (GateProject.dcolistJsonStr == null) {
			throw new CommandlineArgumentParseException("Option -l is required!");
		}
		GateProjectManager gpm;
		gpm = new GateProjectManager();
		gpm.setGate(GateProject.gateHome, GateProject.gatePluginHome, GateProject.gateCreoleDir);
		gpm.setDebug(Debug.level());
		gpm.addJob(GateProject.datastoreRouter, GateProject.taskName, GateProject.dcolistJsonStr);
		(new Thread(gpm)).start();
	}
}

class SetInitDocuments implements Executable {
	public void execute(String[] args) throws Exception {
		if (args.length == 0) {
			throw new CommandlineArgumentParseException("Not enough argument for option -l");
		}
		if (args.length > 1) {
			throw new CommandlineArgumentParseException("Too many argument for option -l");
		}
		GateProject.dcolistJsonStr = JSON.parse(new File(args[0])).toString();
	}
}