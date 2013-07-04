package com.silverwzw.gate;

import com.silvrewzw.gate.task.Task;
import com.silverwzw.gate.manager.AnnotationIndex;


public class GateProject1 {

	public static void main(String[] args) throws Exception {
	    
		CommandlineParser config;
		AnnotationIndex ai;
		
		config = new CommandlineParser(args);
		config.InitGate();
		
		for (Task t : config.getTaskList()) {
			ai = new AnnotationIndex();
			ai.buildIndex(t, config.getCorpus());
			ai.saveIndex(config.getDatastore(), t.getName());
		}
    
	}
}
