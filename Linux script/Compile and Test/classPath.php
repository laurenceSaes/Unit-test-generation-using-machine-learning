<?php 

	$folders = array_values(array_filter(glob('tech-projects/*'), 'is_dir'));
	$scriptInstance = $argv[1];
	$totalScriptInstances = $argv[2];
	
	for($i = 0; $i < count($folders); $i++) {
		if($i % $totalScriptInstances != $scriptInstance)
			continue;

 		$folder = $folders[$i];
		$doneFile = $folder . "/" . "cpDone.txt";
		if(file_exists($doneFile) || ! file_exists($folder . "/" . "pom.xml"))
			continue;

		echo "($i) $folder\n";
		execute($folder, "mvn dependency:build-classpath -Dmdep.outputFile=cp.txt");

		file_put_contents($doneFile,"yes");

	}
	
	function execute($folder, $command) {
		return shell_exec("cd $folder && $command");
	}	
	
	
	

