<?php 

	$folders = array_values(array_filter(glob('tech-projects/*'), 'is_dir'));
	$scriptInstance = $argv[1];
	$totalScriptInstances = $argv[2];
	
	for($i = 0; $i < count($folders); $i++) {
		if($i % $totalScriptInstances != $scriptInstance)
			continue;

 		$folder = $folders[$i];
		$doneFile = $folder . "/" . "cpDone.txt";
		if(file_exists($doneFile) || ! file_exists($folder . "/" . "build.gradle"))
			continue;

		echo "($i) $folder\n";

                $extract = "task writeClasspath << {
                                                buildDir.mkdirs()
                                                new File(file('.').absolutePath, \"cp.txt\").text = configurations.runtime.asPath + \"\\n\"
                                        }";

                $gradleFile = $folder . "/build.gradle";
                $gradleContent = file_get_contents($gradleFile);
                if( strpos($gradleContent, "task writeClasspath") === false) {
                        file_put_contents($gradleFile, "\n" . $extract, FILE_APPEND);
                }
                execute($folder, "gradle writeClasspath");

		file_put_contents($doneFile,"yes");

	}
	
	function execute($folder, $command) {
		return shell_exec("cd $folder && $command");
	}	
	
	
	

